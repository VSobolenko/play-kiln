/**
 * Play Store market scan — the factory's repeatable niche screen.
 *
 * Why this exists
 * ---------------
 * Picking an idea is the factory's highest-leverage decision and its most expensive mistake: a
 * wrong idea costs the same days as a right one, plus policy exposure for a dead app in the
 * catalog. The criteria in docs/niche-selection.md were derived by hand; this module makes that
 * procedure repeatable, so an idea is screened in seconds instead of by opening pages one by one.
 *
 * The one signal that matters
 * ---------------------------
 * A rating on an incumbent is proof the niche supports an app at all, because Play does not show
 * a rating until an app has enough installs to compute one. So:
 *
 * - mostly unrated competitors, no leader  -> DEAD ZONE. People tried; nobody got traction.
 * - rated and mostly strong                -> FORTRESS. Nothing to displace.
 * - rated, and most of the rated are weak  -> CANDIDATE. Proven demand, inadequate supply.
 * - otherwise                              -> MIXED. Some pocket exists, strong players dominate.
 *
 * The naive "few competitors = opportunity" reading is exactly backwards, which is why this is
 * computed rather than eyeballed. `bjj training log` is the trap this screen exists to catch.
 *
 * A known limitation, stated rather than hidden
 * ---------------------------------------------
 * This sees ratings, not reputation. It cannot tell that `first aid guide` is held by the American
 * Red Cross, the IFRC and the UN, so it scores that field as merely `mixed` when a human reads it
 * as a fortress. `organisationLikeDevelopers()` surfaces the hint instead of pretending to judge
 * it: institutional strength stays a human call.
 *
 * A second limitation: fuzzy match makes a verdict about the wrong apps
 * ---------------------------------------------------------------------
 * Play search is a fuzzy TEXT match, so a query can come back answering a fragment of itself:
 * `late blight` returned `Lands of Blight`, `BE LIGHT: Calm & Focus Boost`, `Belight Community` and
 * `L&S LITE BT`, and this screen scored that field `candidate` — a "build this" label computed from
 * lighting and calm-focus apps. The verdict arithmetic was right; its input was not about the
 * query. `analyzeRelevance()` therefore measures how much of the visible result set actually echoes
 * the query's distinctive tokens and attaches the result to every scan as `relevance`.
 *
 * That guard is an ANNOTATION, never a rewrite: `verdictFor()` is unchanged, so a polluted query
 * still reports the verdict its (polluted) numbers produce, with a warning beside it. Suppressing a
 * verdict when relevance is low is a change to the verdict contract, and that is a human decision,
 * not something this module may do silently.
 *
 * Scope
 * -----
 * - Signed-out, geo-parameterised search: a spot check of one storefront, not a market study.
 * - The visible result set is display-capped (~30). That is a page limit, NOT a competitor count.
 * - Ratings only. Install and review counts need a per-app page fetch; review sentiment is not read.
 */

const SEARCH_ENDPOINT = 'https://play.google.com/store/search';

const BROWSER_HEADERS = {
  'user-agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36',
  'accept-language': 'en-US,en;q=0.9',
};

/** An incumbent below this is part of the reachable pocket rather than a wall. */
export const WEAK_RATING = 4.2;

/** At or above this an incumbent is treated as entrenched. */
export const STRONG_RATING = 4.5;

/** Below this share of rated results, demand is not treated as proven. */
export const DEMAND_PROVEN_SHARE = 0.35;

/** At or above this share of weak ratings among rated results, the field is inadequate. */
export const INADEQUATE_SHARE = 0.5;

/** At or above this share of strong ratings, incumbents are entrenched. */
export const FORTRESS_SHARE = 0.6;

/** Below this share of weak ratings, there is no pocket left to enter through. */
export const NO_POCKET_SHARE = 0.3;

/**
 * Below this share of visible titles echoing the query, its result set is treated as polluted.
 *
 * Chosen to be conservative on purpose: token overlap UNDER-counts relevance (see `tokensOf`), so a
 * low share is evidence of pollution, and the flag only fires when most visible cards do not carry
 * a single distinctive token of the query.
 */
export const POLLUTED_SHARE = 0.35;

/**
 * At or above this share — with every distinctive query token echoed somewhere — a result set is
 * treated as on-topic. Where relevance cannot be measured the level is `unmeasurable`, never a
 * fabricated pass.
 */
export const ON_TOPIC_SHARE = 0.65;

/**
 * Phrases that mark a publisher as an organisation rather than an individual developer.
 *
 * Matched on word boundaries, and short ambiguous tokens are deliberately absent. Substring
 * matching made a mess of this: `'who'` flagged `WHOOP`, `'clinic'` flagged `eClinicalWorks`, and
 * neither could ever match the actual `World Health Organization` — the heuristic was producing
 * false positives while missing the case it existed for. Phrases like `'world health'` cover the
 * real publishers, and boundaries keep `eClinicalWorks` from tripping `'clinic'`.
 */
const ORGANISATION_PHRASES = [
  'red cross', 'red crescent', 'united nations', 'world health', 'safety council',
  'government', 'ministry', 'council', 'association', 'foundation', 'university',
  'institute', 'hospital', 'society', 'ifrc', 'unicef'
];

/** Pre-built boundary-anchored matchers, so each developer name is tested without rebuilding regexes. */
const ORGANISATION_MATCHERS = ORGANISATION_PHRASES.map(
  (phrase) => new RegExp(`\\b${phrase.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'i')
);

/**
 * Parses a Play search results page into one entry per app.
 *
 * Each card renders an app-details link, so splitting on those links yields non-overlapping
 * segments and anything found in a segment belongs to that segment's app. Within a card, the
 * collapsed visible text reads `[title, developer, rating, "star"]` when rated and
 * `[title, developer]` when not. The `Rated X stars` aria-label is used for the number because
 * it is unambiguous, and a missing label is a genuinely unrated app — the signal itself, which
 * must never be defaulted to a value.
 */
export function parseSearchHtml(html) {
  const linkPattern = /\/store\/apps\/details\?id=([A-Za-z0-9_.]+)/g;
  const matches = [...html.matchAll(linkPattern)];

  // A match sits INSIDE the anchor's `href` attribute, so a segment starting at `match.index`
  // begins mid-attribute. Its tail then survives tag-stripping and can become the card's "text" —
  // whether it does depended only on the 80-char filter, which is how a card title once came out
  // as `/store/apps/details?id=com.whoop.android" aria-label="WHOOP" class="Qfxief">`.
  // Card boundaries are therefore structural: each segment runs from just after the tag containing
  // its link to just before the tag containing the next link.
  const segmentStarts = matches.map((match) => {
    const tagEnd = html.indexOf('>', match.index);
    return tagEnd === -1 ? match.index : tagEnd + 1;
  });
  const tagStarts = matches.map((match) => {
    const tagStart = html.lastIndexOf('<', match.index);
    return tagStart === -1 ? match.index : tagStart;
  });

  const results = [];
  const seen = new Set();

  matches.forEach((match, index) => {
    const packageId = match[1];
    if (seen.has(packageId)) return;
    seen.add(packageId);

    const start = segmentStarts[index];
    const end = index + 1 < matches.length ? tagStarts[index + 1] : html.length;
    if (end <= start) return;
    const segment = html.slice(start, end);

    const ratingMatch = segment.match(/aria-label="Rated\s+([\d.]+)\s+stars?/i);
    const rating = ratingMatch ? Number.parseFloat(ratingMatch[1]) : null;

    const text = segment
      .replace(/<script[\s\S]*?<\/script>/g, ' ')
      .replace(/<[^>]+>/g, '\n')
      .split('\n')
      .map((line) => line.trim())
      // Visible text on a Play card never contains an attribute assignment or a store path, so
      // either marker means a markup fragment rather than content. Filtering on that keeps the
      // parser correct even if a segmentation edge case appears on a future page layout.
      .filter((line) => line.length > 0 && line.length < 80 && !line.includes('="') && !line.startsWith('/store/'))

    const playLabel = segment.match(/aria-label="Play ([^"]+)"/);

    results.push({
      packageId,
      title: playLabel?.[1] ?? text[0] ?? packageId,
      // text[1] is the developer whether or not the Play label was present: the label duplicates
      // the title rather than replacing it, so the offsets are stable.
      developer: text[1] ?? '',
      rating,
      rated: rating !== null,
    });
  });

  return results;
}

/**
 * Turns parsed results into the decision signals for one query.
 *
 * `ratedShare` is computed over all visible results, because an unrated app is the absence of
 * evidence. The rating distribution is computed over rated results only, and every share is
 * derived from those two counts so a reader can recompute them.
 */
export function analyzeResults(results) {
  const total = results.length;
  const rated = results.filter((r) => r.rated);
  const unrated = total - rated.length;

  const ratings = rated.map((r) => r.rating).sort((a, b) => a - b);
  const weak = ratings.filter((r) => r < WEAK_RATING);
  const strong = ratings.filter((r) => r >= STRONG_RATING);

  const median =
    ratings.length === 0
      ? null
      : ratings.length % 2 === 1
        ? ratings[(ratings.length - 1) / 2]
        : (ratings[ratings.length / 2 - 1] + ratings[ratings.length / 2]) / 2;

  const share = (numerator, denominator) =>
    denominator === 0 ? 0 : Number((numerator / denominator).toFixed(2));

  return {
    total,
    ratedCount: rated.length,
    unratedCount: unrated,
    ratedShare: share(rated.length, total),
    weakCount: weak.length,
    weakShare: share(weak.length, rated.length),
    strongCount: strong.length,
    strongShare: share(strong.length, rated.length),
    medianRating: median === null ? null : Number(median.toFixed(2)),
    minRating: ratings.length ? ratings[0] : null,
    worst: rated.length ? rated.reduce((a, b) => (b.rating < a.rating ? b : a)) : null,
  };
}

/**
 * Words that name no niche on their own, so they are removed from a query before its relevance is
 * measured. A store search for `plant guide app` describes a shelf, not a job; keeping those tokens
 * would mark every gardening app on that shelf as "on-topic" and blunt the guard precisely where it
 * is needed. The lists are conservative: a word is only here if it appears in titles across
 * unrelated niches, and over-removing inflates the flag's false-alarm rate.
 */
const STOPWORDS = new Set([
  'and', 'the', 'for', 'with', 'from', 'your', 'that', 'this', 'you', 'are', 'was', 'were', 'has',
  'have', 'how', 'what', 'which', 'who', 'why', 'when', 'where', 'not', 'but', 'all', 'any', 'its',
  'our', 'out', 'off', 'per', 'via', 'use', 'uses', 'using', 'get', 'got', 'can', 'will', 'may',
]);

const GENERIC_QUERY_TOKENS = new Set([
  'app', 'apps', 'application', 'android', 'play', 'store', 'free', 'best', 'top', 'new', 'online',
  'guide', 'guides', 'helper', 'tool', 'tools', 'service', 'services', 'pro', 'plus', 'lite',
  'treatment', 'treatments', 'tracker', 'trackers', 'track', 'log', 'logs', 'diary', 'journal',
  'daily', 'plan', 'planner', 'planning', 'plant', 'plants', 'garden', 'gardens',
]);

/**
 * Splits text into comparable tokens.
 *
 * Whole tokens, never substrings. That distinction is the entire point: the query `late blight`
 * contains the substring `light`, which is how `BE LIGHT: Calm & Focus Boost`, `Belight Community`,
 * `L&S LITE BT` and `Dustlight` looked like matches, and how the screen produced a `candidate`
 * verdict from a lighting shelf. Here `light` and `blight` are different tokens, so none of those
 * cards matches the query, while a card actually titled `Late Blight Alert` would.
 *
 * A trailing `s` is dropped, so `plants`/`plant` and `diseases`/`disease` compare equal. That is the
 * ONLY stemming: morphological variants (`identification` vs `identifier`, `leaves` vs `leaf`)
 * compare unequal and count as misses. Misses push the share DOWN, so `on-topic` is trustworthy
 * while a low share is evidence of pollution rather than proof of it — the flag is deliberately
 * one-directional rather than pretending to a precision it does not have.
 *
 * Tokens shorter than 3 characters are dropped, but not for safety: whole-token equality already
 * makes a short token harmless (`bjj` can only match the token `bjj`). They are dropped because
 * one- and two-letter tokens in a title are as likely to be noise as signal. `bjj` — the most
 * distinctive token of `bjj training log` — is 3 characters and therefore kept.
 */
function tokensOf(text) {
  return String(text ?? '')
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter((token) => token.length >= 3 && !STOPWORDS.has(token))
    .map((token) => (token.length > 3 && token.endsWith('s') && !token.endsWith('ss') ? token.slice(0, -1) : token));
}

/**
 * The part of a query that can be evidence of topical relevance.
 *
 * Stopwords and generic tokens are removed, because a title containing `plant`, `guide`, `app` or
 * `treatment` is not evidence that it answers `plant guide app`: those words are how everything on
 * that shelf is titled. What is left is the part of the query that names a job, and only that part
 * is tested. A query made of nothing else has no distinctive tokens, which is reported as
 * `unmeasurable` rather than as a pass or a failure.
 */
function distinctiveQueryTokens(query) {
  return [...new Set(tokensOf(query).filter((token) => !GENERIC_QUERY_TOKENS.has(token)))];
}

/**
 * How much of a result set actually echoes its own query — the guard against a fuzzy-match verdict.
 *
 * Every consumer gets this on each scan result as `relevance`, because a verdict computed over
 * off-topic apps is not a weaker signal, it is a signal about a different market. It is attached,
 * never applied: see the module header for why suppressing a verdict stays a human decision.
 */
export function analyzeRelevance(query, results = []) {
  const tokens = distinctiveQueryTokens(query);
  const total = results.length;
  const titleTokenSets = results.map((result) => new Set(tokensOf(result.title)));

  const base = { tokens, total, onTopicCount: 0, share: null, unsupportedTokens: [], level: 'unmeasurable', suspect: false };

  if (total === 0) {
    return { ...base, warning: 'the query returned nothing visible, so there is nothing to measure relevance over' };
  }

  if (tokens.length === 0) {
    return {
      ...base,
      warning: `"${query}" has no distinctive tokens left once stopwords and generic words are removed, so token overlap cannot judge this result set`,
    };
  }

  const onTopicCount = titleTokenSets.filter((set) => tokens.some((token) => set.has(token))).length;
  const unsupportedTokens = tokens.filter((token) => !titleTokenSets.some((set) => set.has(token)));
  const share = Number((onTopicCount / total).toFixed(2));
  const level =
    share < POLLUTED_SHARE ? 'polluted' : share < ON_TOPIC_SHARE || unsupportedTokens.length > 0 ? 'partial' : 'on-topic';

  const quoted = unsupportedTokens.map((token) => `"${token}"`).join(', ');
  const missing = unsupportedTokens.length > 0 ? `, and ${quoted} appear${unsupportedTokens.length === 1 ? 's' : ''} in no title` : '';

  const warning =
    level === 'polluted'
      ? `only ${onTopicCount}/${total} visible titles contain a distinctive token from "${query}"${missing} — a verdict computed from these results describes whatever Play fuzzy-matched, not the query`
      : level === 'partial'
        ? `${onTopicCount}/${total} visible titles contain a distinctive token from "${query}"${missing} — part of this result set is not about the query`
        : null;

  return {
    tokens,
    total,
    onTopicCount,
    share,
    unsupportedTokens,
    level,
    suspect: level === 'polluted' || level === 'partial',
    warning,
  };
}

/**
 * Publishers whose names read like institutions rather than individuals.
 *
 * This is a hint for a human decision, not a verdict: the `first aid guide` field is held by the
 * Red Cross, the IFRC and the UN, which ratings alone cannot see. It is intentionally conservative
 * — a name it misses costs a reviewer a glance, whereas a name it invents costs them trust.
 */
export function organisationLikeDevelopers(results) {
  return results
    .filter((r) => {
      const name = r.developer || '';
      return ORGANISATION_MATCHERS.some((matcher) => matcher.test(name));
    })
    .map((r) => `${r.developer} (${r.title})`);
}

/**
 * The verdict, with its reasoning attached so a reader can disagree with the rule rather than
 * only with the label.
 *
 * Order matters. Unproven demand is rejected first, because in a dead zone a low rating means
 * "this tried and failed", not "this is beatable".
 */
export function verdictFor(analysis) {
  if (analysis.total === 0) {
    return { verdict: 'no-results', reason: 'the query returned nothing visible' };
  }

  if (analysis.ratedShare < DEMAND_PROVEN_SHARE) {
    return {
      verdict: 'dead-zone',
      reason: `only ${analysis.ratedCount}/${analysis.total} incumbents are rated (${analysis.ratedShare} < ${DEMAND_PROVEN_SHARE}), so demand is not proven — people tried and did not get traction`,
    };
  }

  if (analysis.weakShare >= INADEQUATE_SHARE) {
    return {
      verdict: 'candidate',
      reason: `${analysis.weakCount}/${analysis.ratedCount} rated incumbents are below ${WEAK_RATING} — demand proven, supply inadequate`,
    };
  }

  if (analysis.strongShare >= FORTRESS_SHARE && analysis.weakShare < NO_POCKET_SHARE) {
    // The message is built from the same numbers the condition tested. The previous wording
    // claimed "no weak pocket" unconditionally, so all eight fortress verdicts in a live sample
    // asserted something false while their own weakCount sat between 1 and 6.
    const pocket = `${analysis.weakCount}/${analysis.ratedCount} rated incumbents are below ${WEAK_RATING} (${analysis.weakShare} < ${NO_POCKET_SHARE})`;
    return {
      verdict: 'fortress',
      reason: `${analysis.strongCount}/${analysis.ratedCount} rated incumbents are >= ${STRONG_RATING}, and only ${pocket} — entrenched`,
    };
  }

  return {
    verdict: 'mixed',
    reason: `demand proven, ${analysis.weakCount} weak incumbent(s), but ${analysis.strongShare} of rated are strong — enter only with a specific angle`,
  };
}

/** Fetches and screens one query. Returns raw results plus the computed signals. */
export async function scanQuery(query, { gl = 'US', hl = 'en', fetchImpl = fetch } = {}) {
  const url = `${SEARCH_ENDPOINT}?q=${encodeURIComponent(query)}&c=apps&hl=${hl}&gl=${gl}`;

  const response = await fetchImpl(url, { headers: BROWSER_HEADERS });
  if (!response.ok) {
    // `relevance: null` rather than a computed value: an error is not a relevance finding, and a
    // consumer must be able to tell "not measured" from "measured and clean".
    return { query, url, error: `HTTP ${response.status}`, results: [], relevance: null };
  }

  const html = await response.text();
  const results = parseSearchHtml(html);
  const analysis = analyzeResults(results);
  const { verdict, reason } = verdictFor(analysis);

  return {
    query,
    url,
    results,
    analysis,
    verdict,
    reason,
    relevance: analyzeRelevance(query, results),
    organisations: organisationLikeDevelopers(results),
    worst: analysis.worst,
  };
}

/**
 * Screens many queries with bounded concurrency.
 *
 * Concurrency is bounded rather than unlimited on purpose: this hits a public endpoint, and a
 * burst of parallel requests is how a scan turns into a block.
 */
export async function scanQueries(queries, { concurrency = 4, ...options } = {}) {
  const out = [];
  const queue = [...queries];

  const workers = Array.from({ length: Math.min(concurrency, queue.length) }, async () => {
    while (queue.length > 0) {
      const query = queue.shift();
      try {
        out.push(await scanQuery(query, options));
      } catch (error) {
        out.push({ query, error: String(error), results: [], relevance: null });
      }
    }
  });

  await Promise.all(workers);
  // Restore input order so a report reads the same regardless of completion order.
  return queries.map((query) => out.find((entry) => entry.query === query)).filter(Boolean);
}

/**
 * The relevance guard as a table cell.
 *
 * Short by design: the table is already seven columns wide, and the load-bearing part is whether a
 * reader can see a `POLLUTED` verdict at a glance. The measured share and the warning live in
 * `scan.relevance` and in the reports, not here. `-` means not measured (an errored scan);
 * `n/a` means measurable in principle but this query had nothing to measure.
 */
function relevanceCell(scan) {
  if (!scan.relevance) return '-';
  if (scan.relevance.level === 'unmeasurable') return 'n/a';
  if (scan.relevance.level === 'on-topic') return 'ok';
  return scan.relevance.level === 'polluted' ? 'POLLUTED' : 'partial';
}

/** Renders the scan as a fixed-width table for a terminal or a markdown code block. */
export function formatTable(scans) {
  const rows = scans.map((scan) => {
    if (scan.error) {
      return [scan.query, 'ERROR', '-', '-', '-', '-', '-', scan.error.slice(0, 44)];
    }
    const a = scan.analysis;
    return [
      scan.query,
      scan.verdict,
      relevanceCell(scan),
      String(a.total),
      `${a.ratedCount}/${a.total}`,
      `${a.weakCount}`,
      a.medianRating === null ? '-' : a.medianRating.toFixed(2),
      scan.reason.slice(0, 56),
    ];
  });

  const header = ['query', 'verdict', 'relev', 'seen', 'rated', 'weak', 'median', 'why'];
  const all = [header, ...rows];
  const widths = header.map((_, i) => Math.max(...all.map((row) => row[i].length)));

  return all
    .map((row) => row.map((cell, i) => cell.padEnd(widths[i])).join('  '))
    .join('\n')
    .replace('\n', `\n${widths.map((w) => '-'.repeat(w)).join('  ')}\n`);
}

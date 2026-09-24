/**
 * Play review complaints — what users actually say, read from the server-rendered review section.
 *
 * Why this exists
 * ---------------
 * `play-scan.mjs` reads ratings, and `play-app-detail.mjs` reads install counts and monetisation
 * badges. None of them reads a single word a user wrote, so the factory could say "11/14 rated
 * incumbents are below 4.2" and never say *why* those incumbents are below 4.2. The distinction
 * matters when choosing what to build: a pocket of apps rated 3.8★ for a missing feature is a
 * different opportunity from a pocket rated 3.8★ because the apps crash.
 *
 * This module reads the review text Play server-renders on an app-detail page and sorts what it
 * says into named complaint classes.
 *
 * Scope and honesty
 * -----------------
 * - **Server-rendered HTML only**, the same discipline as `play-app-detail.mjs`. Anything Play
 *   ships in a deferred JSON blob is deliberately not parsed: an undocumented internal payload
 *   changes without warning. (The page does carry one — `AF_initDataCallback` — and it is not
 *   used.)
 * - **A reviewer's name is never extracted, on purpose.** Review text is user-generated public
 *   content and is needed for the signal; a reviewer's identity is not, and collecting it would
 *   put personal data into build artefacts and logs for no benefit. The parser has no field for
 *   it, so it cannot leak one.
 * - **Absence is information.** A page with no `data-g-id="reviews"` section is a genuinely
 *   review-less app, reported as `sectionFound: false` with an empty list — never defaulted to a
 *   fabricated review and never silently treated as "no complaints".
 * - **The sample is Play's, not ours.** Play server-renders a fixed handful of reviews per page
 *   (measured: 3) chosen by its own relevance ranking. This is a *sample*, and the classifier
 *   below therefore reports counts and the verbatim texts they came from, so a human can audit a
 *   number instead of trusting it.
 * - **The classifier is a lexical heuristic, not comprehension.** It matches word patterns in
 *   lowercase text. It can fire on a compliment ("no ads, love it" matches the ads class) and it
 *   can miss a complaint phrased unusually. That is why `classifyComplaint` returns class ids
 *   *and* the caller is expected to print the raw texts next to the counts.
 *
 * Markers, as measured on real pages (2026-09-24, verified against `com.mushroomhunters`,
 * `com.spotify.music`, `it.Ettore.calcolielettrici`):
 *
 * | Element | Server-rendered anchor | Note |
 * |---|---|---|
 * | reviews section | `data-g-id="reviews"` | runs to the end of the document; no marker follows it |
 * | one review card | `class="Jx4nYe"` | measured 3 per page; occurred 0 times outside the section |
 * | the review's own stars | `aria-label="Rated N stars out of five stars"` **inside a card** | must be scoped to the card — the same label appears 12+ times elsewhere in the section (the star-distribution histogram), so counting labels globally is wrong |
 * | review body | `class="h3YV2d"` | one per card when the review has text |
 * | review date | `class="bp9Aid"` | one per card |
 * | helpful count | `N people found this review helpful` | optional |
 *
 * The card-scoped reading is the whole point of the split. An earlier probe counted
 * `aria-label="Rated…"` occurrences across the reviews section and found **15** on Spotify's page
 * for **3** reviews; pairing ratings to bodies by "nearest text after the rating" would have
 * attached a histogram bucket's rating to a review about something else entirely.
 */

/** Fetches an app page; reviews are inside it, so the endpoint is the same as the detail page. */
const DETAIL_ENDPOINT = 'https://play.google.com/store/apps/details';

/** Same browser-ish headers as the other Play readers in this repo. */
const BROWSER_HEADERS = {
  'user-agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36',
  'accept-language': 'en-US,en;q=0.9',
};

/**
 * At or below this, a review is treated as a complaint.
 *
 * Play's own scale: 1★ is unusable, 2★ is bad, 3★ is "works but I am unhappy", 4★ is good, 5★ is
 * enthusiastic. The factory's niche screen already treats a *product* rating below `WEAK_RATING`
 * (4.2, `play-scan.mjs`) as part of the reachable pocket; a *review* is a single person's verdict
 * rather than an average, so the bar is stated separately and is deliberately the three lowest
 * values rather than a float. Exported so callers print the threshold they actually applied.
 */
export const CRITICAL_RATING = 3;

const NAMED_ENTITIES = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ' };

function decodeEntities(text) {
  return text
    .replace(/&#x([0-9a-f]+);/gi, (whole, hex) => {
      const code = Number.parseInt(hex, 16);
      return Number.isFinite(code) ? String.fromCodePoint(code) : whole;
    })
    .replace(/&#(\d+);/g, (whole, dec) => {
      const code = Number.parseInt(dec, 10);
      return Number.isFinite(code) ? String.fromCodePoint(code) : whole;
    })
    .replace(/&(amp|lt|gt|quot|apos|nbsp);/g, (_, name) => NAMED_ENTITIES[name]);
}

/** Thrown for anything the caller must not mistake for a successful read. */
export class PlayReviewsError extends Error {
  constructor(code, message, details = {}) {
    super(message);
    this.name = 'PlayReviewsError';
    this.code = code;
    Object.assign(this, details);
  }
}

/**
 * The complaint taxonomy — the single source of truth for what this sensor can name.
 *
 * Every count the CLI prints is derived from this table via `classifyComplaint`, so a class that
 * is renamed here is renamed in the report, and a class that is added here is counted everywhere
 * without another edit. Patterns are matched against lowercased text and use `\b` word boundaries
 * so `\bad\b` cannot fire inside `had`, `bad`, or `download`.
 *
 * The list is deliberately about *actionable* complaints — the kind that name something a different
 * app could do better. "Why is my app bad" is not actionable; "everything is behind a paywall" is.
 *
 * Revision record (kept here, not in a document that can drift away from the code)
 * -------------------------------------------------------------------------------
 * **2026-09-24 — revised against the first real corpus.** Seeded from intuition, then run over 59
 * critical reviews read from four pockets (reloading, reptile husbandry, plumber pipe sizing,
 * property maintenance inspection). Measured: **37% of critical reviews were unclassified**, which
 * makes the histogram useless. Three classes were missing entirely, and the review texts that
 * exposed them were:
 *
 * | added / extended | text that exposed it |
 * |---|---|
 * | `units` (new) | "Results are in metric" · "set my prefs to us units … it still displays all metric" |
 * | `regression` (new) | "Old version was better!" · "New version it is different?" · "worse than the app it replaced" |
 * | `language` (new) | "It's in a different language. And currency. Can't understand much." |
 * | `account` extended | "I cannot log into this app" (the old `\blog ?in\b` could not match "log into") |
 * | `paywall` extended | "I would have to buy 38 rounds on the new version" · "no way to cancel" · "so much money" |
 * | `broken` extended | "would not return a value for a long length high demand run" · "doesn't show results" |
 * | `missing-feature` extended | "there's no way to save more than one recipe" · "only supports technicians" |
 * | `value` extended | "Deceptive pics. None of the screens … even exist in the app." |
 *
 * The revision is measured, not asserted: `verify-reviews-sensor.mjs` prints the unclassified
 * share, and the re-run after this change is recorded in
 * `docs/research/2026-09-24-market-decision.md`.
 */
export const COMPLAINT_CLASSES = [
  {
    id: 'crash',
    label: 'Crashes or will not open',
    patterns: [/\bcrash\w*/i, /\bfreez\w*/i, /\bblack screen\b/i, /\bwon'?t open\b/i, /\bwill not open\b/i, /\bkeeps closing\b/i, /\bforce clos\w*/i, /\bstops? working\b/i, /\bshuts? down\b/i, /\bcloses? out\b/i],
  },
  {
    id: 'broken',
    label: 'Broken feature or bug',
    patterns: [/\bbug\w*/i, /\bbroken\b/i, /\bdoes ?n[o']?t work\b/i, /\bnot working\b/i, /\bstopped working\b/i, /\bglitch\w*/i, /\berror\b/i, /\bwould ?n[o']?t (return|load|save|open|work)\b/i, /\bdoes ?n[o']?t (show|display|return|load|save)\b/i, /\bnothing happens\b/i, /\bdoes nothing\b/i],
  },
  {
    id: 'ads',
    label: 'Ads',
    patterns: [/\bads?\b/i, /\badvert\w*/i, /\bcommercials?\b/i, /\bunskippable\b/i],
  },
  {
    id: 'paywall',
    label: 'Paywall, price or subscription',
    patterns: [/\bpaywall\b/i, /\bsubscri\w*/i, /\bpremium\b/i, /\blocked behind\b/i, /\bpay to\b/i, /\bpaid\b/i, /\bhave to pay\b/i, /\bwould have to (pay|buy)\b/i, /\bpay again\b/i, /\bbuy (it|them|this) again\b/i, /\brestore purchase\b/i, /\bexpensive\b/i, /\boverpriced\b/i, /\bso much money\b/i, /\bcash ?grab\b/i, /\bgreedy\b/i, /\brefund\w*/i, /\bcharged\b/i, /\bfree trial\b/i, /\btrial\b/i, /\bcan'?t cancel\b/i, /\bcan ?not cancel\b/i, /\bno way to cancel\b/i, /\block(ed|ing) (you )?in\b/i],
  },
  {
    id: 'account',
    label: 'Forced account or login',
    patterns: [/\blog ?in\b/i, /\blog ?into\b/i, /\blogin\b/i, /\bsign ?in\b/i, /\bsign ?up\b/i, /\bregister\w*/i, /\bencouraged to register\b/i, /\baccount (is )?required\b/i, /\bcreate an account\b/i, /\bcreate a login\b/i, /\bwithout an account\b/i, /\bcan ?not (log|sign)\b/i, /\bmust (log|sign|register)\b/i],
  },
  {
    id: 'privacy',
    label: 'Permissions, privacy or tracking',
    patterns: [/\bpermission\w*/i, /\bprivacy\b/i, /\btracking\b/i, /\bspy\w*/i, /\bpersonal (info|data)\b/i, /\bsell(s|ing)? my data\b/i],
  },
  {
    id: 'accuracy',
    label: 'Wrong or outdated information',
    patterns: [/\bwrong\b/i, /\bincorrect\w*/i, /\binaccurate\w*/i, /\bnot accurate\b/i, /\bmisleading\b/i, /\bfalse\b/i, /\bout ?of ?date\b/i, /\boutdated\b/i, /\bwrong (answer|result|number|info)/i, /\bcompletely wrong\b/i, /\bdifferent answers\b/i],
  },
  {
    id: 'units',
    label: 'Wrong units or measurement system',
    patterns: [/\bmetric\b/i, /\bimperial\b/i, /\bwrong units?\b/i, /\bunits? (are|is|were)\b/i, /\bus units\b/i, /\bno way to (change|switch) units?\b/i, /\bonly (in )?(mm|metres|meters|inches|feet)\b/i],
  },
  {
    id: 'regression',
    label: 'Update made it worse',
    patterns: [/\bworse than\b/i, /\bold version was better\b/i, /\bused to (work|be)\b/i, /\bsince (the )?(last |latest )?update\b/i, /\bafter (the )?(last |latest )?update\b/i, /\bnew version\b[^.]{0,24}\b(different|worse|confusing|broken|buggy|stupid)\b/i, /\brevamp\w*/i, /\bbroke (it|everything)\b/i, /\bit worked (fine )?before\b/i],
  },
  {
    id: 'language',
    label: 'Wrong or missing language',
    patterns: [/\bdifferent language\b/i, /\bwrong language\b/i, /\bnot (in )?(my|your) language\b/i, /\bcan'?t understand\b/i, /\bcannot understand\b/i, /\btranslation\b/i, /\bnot translated\b/i, /\bbad (english|translation)\b/i],
  },
  {
    id: 'missing-feature',
    label: 'Missing feature or capability',
    patterns: [/\bmissing\b/i, /\bno way to\b/i, /\bthere ?(is|'s) no\b/i, /\bwish (it|there|you)\b/i, /\bdoes ?n[o']?t have\b/i, /\bneeds? (a |an |to )/i, /\bshould (have|be able)\b/i, /\badd (a |an |the )\w+/i, /\bwould be nice\b/i, /\bi want to be able\b/i, /\bonly supports?\b/i, /\bcan'?t (save|add|edit|export|import)\b/i, /\bno option to\b/i, /\bit would be (way )?better\b/i],
  },
  {
    id: 'confusing-ui',
    label: 'Confusing or clunky interface',
    patterns: [/\bconfus\w*/i, /\bcomplicated\b/i, /\bhard to (use|navigate|find)\b/i, /\bdifficult to (use|navigate)\b/i, /\bnot intuitive\b/i, /\bunclear\b/i, /\bclunky\b/i, /\bmessy\b/i, /\bpoorly designed\b/i, /\bbad (ui|ux|interface)\b/i, /\binterface\b/i, /\bhard to figure out\b/i],
  },
  {
    id: 'performance',
    label: 'Slow, heavy or battery drain',
    patterns: [/\bslow\w*/i, /\blag\w*/i, /\bbattery\b/i, /\bdrain\w*/i, /\btakes forever\b/i, /\bso slow\b/i, /\bmemory\b/i, /\bnot responsive\b/i],
  },
  {
    id: 'data-loss',
    label: 'Sync, backup or lost data',
    patterns: [/\bsync\w*/i, /\blost (my|all|the)\b/i, /\bdata loss\b/i, /\bbackup\b/i, /\bdid ?n[o']?t save\b/i, /\bnot saving\b/i, /\bdeleted my\b/i, /\bwere not saved\b/i],
  },
  {
    id: 'offline',
    label: 'Needs connection or offline failure',
    patterns: [/\boffline\b/i, /\bno internet\b/i, /\brequires (an )?internet\b/i, /\bwithout internet\b/i, /\bno connection\b/i, /\bserver\b/i, /\bstored online\b/i, /\bbaked into the app\b/i, /\bdata should be (local|offline|in the app)\b/i],
  },
  {
    id: 'abandoned',
    label: 'Abandoned or unsupported',
    patterns: [/\babandon\w*/i, /\bno updates?\b/i, /\bnever update\w*/i, /\blast update\b/i, /\bno support\b/i, /\bunmaintained\b/i, /\bdeveloper (does ?n[o']?t|never)\b/i, /\bdead app\b/i],
  },
  {
    id: 'notifications',
    label: 'Notifications or email spam',
    patterns: [/\bnotification\w*/i, /\bspam\w*/i, /\bconstant reminders?\b/i, /\bemail\w*/i],
  },
  {
    id: 'value',
    label: 'No value, useless or scam',
    patterns: [/\buseless\b/i, /\bworthless\b/i, /\bwaste of (time|money)\b/i, /\bscam\w*/i, /\bfake\b/i, /\bno value\b/i, /\bnot worth\b/i, /\brip ?off\b/i, /\bdeceptive\b/i, /\bnot exist in the app\b/i, /\bwasted (time|money)\b/i, /\bwhat a (shame|joke)\b/i],
  },
];

/** Class ids in taxonomy order, exported so a report can show every class including zero counts. */
export const COMPLAINT_CLASS_IDS = COMPLAINT_CLASSES.map((entry) => entry.id);

/**
 * Names the complaint classes a single review text matches.
 *
 * Returns an array (possibly empty, possibly with several ids): one review can legitimately
 * complain about ads *and* about a crash, and collapsing that to one bucket would hide which is
 * more common. An empty array means "the classifier did not recognise this text", which is
 * reported as an unclassified count rather than folded into a class.
 */
export function classifyComplaint(text) {
  if (typeof text !== 'string' || text.length === 0) return [];
  return COMPLAINT_CLASSES.filter((entry) => entry.patterns.some((re) => re.test(text))).map(
    (entry) => entry.id,
  );
}

/**
 * Counts complaint classes over a set of reviews, and says how many reviews each count rests on.
 *
 * `counts` is derived from `classifyComplaint` on every call rather than stored on a review, so
 * the histogram and the per-review labels can never disagree. Reviews whose text is `null` (a
 * rating-only review) are counted in `withoutText`, and reviews the classifier did not recognise
 * are counted in `unclassified` — neither is silently dropped, because a report that hides its
 * blind spots is the failure mode this module exists to avoid.
 */
export function summariseComplaints(reviews, { maxRating = CRITICAL_RATING } = {}) {
  const critical = reviews.filter((r) => typeof r.rating === 'number' && r.rating <= maxRating);
  const counts = Object.fromEntries(COMPLAINT_CLASS_IDS.map((id) => [id, 0]));
  let withoutText = 0;
  let unclassified = 0;

  for (const review of critical) {
    if (typeof review.text !== 'string' || review.text.length === 0) {
      withoutText += 1;
      continue;
    }
    const ids = classifyComplaint(review.text);
    if (ids.length === 0) unclassified += 1;
    for (const id of ids) counts[id] += 1;
  }

  const ranked = COMPLAINT_CLASS_IDS.filter((id) => counts[id] > 0)
    .map((id) => ({
      id,
      label: COMPLAINT_CLASSES.find((entry) => entry.id === id).label,
      count: counts[id],
    }))
    .sort((a, b) => b.count - a.count || a.id.localeCompare(b.id));

  return {
    reviewsRead: reviews.length,
    criticalRead: critical.length,
    maxRating,
    counts,
    ranked,
    withoutText,
    unclassified,
  };
}

/** Reads one review card's facts. Returns `null` when the card has no rating anchor at all. */
function readCard(card) {
  const ratingMatch = card.match(/aria-label="Rated ([0-9.]+) stars? out of five stars"/i);
  if (!ratingMatch) return null;

  const bodyMatch = card.match(/class="h3YV2d"[^>]*>([\s\S]*?)<\/div>/);
  const dateMatch = card.match(/class="bp9Aid">([^<]*)<\/span>/);
  const helpfulMatch = card.match(/(\d+)\s*(?:people|person)[^<]{0,40}helpful/i);

  // A body may contain nested markup; strip tags and decode, then keep the text.
  const rawText = bodyMatch ? bodyMatch[1].replace(/<[^>]*>/g, ' ') : null;
  const text = rawText === null ? null : decodeEntities(rawText).replace(/\s+/g, ' ').trim();

  return {
    rating: Number.parseFloat(ratingMatch[1]),
    text: text === null || text.length === 0 ? null : text,
    date: dateMatch ? decodeEntities(dateMatch[1]).trim() : null,
    helpfulCount: helpfulMatch ? Number.parseInt(helpfulMatch[1], 10) : null,
  };
}

/**
 * Extracts the reviews from an app-detail HTML page.
 *
 * Pure and network-free so the markers above can be re-verified against captured HTML whenever
 * Play changes its markup. Throws `PlayReviewsError` when the document is not an app-detail page
 * at all — returning a plausible empty review list there would make "this app has no reviews"
 * and "the page changed" indistinguishable, which is the exact failure this module must not have.
 *
 * A page that *is* an app detail page but carries no review section is a successful read of zero
 * reviews: `sectionFound: false`, `reviews: []`.
 */
export function parseReviewsHtml(html, packageId) {
  if (typeof html !== 'string' || html.length === 0) {
    throw new PlayReviewsError('EMPTY_HTML', 'no HTML to parse');
  }
  if (typeof packageId !== 'string' || packageId.trim().length === 0) {
    throw new PlayReviewsError('INVALID_PACKAGE_ID', 'packageId must be a non-empty string');
  }

  // Same "is this even an app page" gate as play-app-detail.mjs, kept independent so this module
  // can be used without importing that one.
  const titleMatch = html.match(
    /<h1[^>]*>[\s\S]{0,200}?<span[^>]*itemprop="name"[^>]*>([^<]*)<\/span>/,
  );
  if (!titleMatch) {
    throw new PlayReviewsError(
      'PARSE_FAILED',
      `no app title (itemprop="name" inside an h1) — this is not a Play app-detail page for ${packageId}`,
    );
  }

  const unparsed = [];
  const sectionAt = html.indexOf('data-g-id="reviews"');
  if (sectionAt === -1) {
    return {
      packageId,
      title: decodeEntities(titleMatch[1]).trim(),
      sectionFound: false,
      reviews: [],
      unparsed: ['reviews'],
      url: null,
    };
  }

  // No `data-g-id` marker follows `reviews` on any page measured, so the section runs to the end
  // of the document. Every `Jx4nYe` card was inside it (0 occurrences before it), so that is safe.
  const section = html.slice(sectionAt);
  const cards = section.split('class="Jx4nYe"').slice(1);

  const reviews = [];
  let cardsWithoutRating = 0;
  for (const card of cards) {
    const review = readCard(card);
    if (review === null) {
      cardsWithoutRating += 1;
      continue;
    }
    reviews.push(review);
  }

  if (cards.length > 0 && cardsWithoutRating > 0) {
    unparsed.push('reviewRating');
  }

  return {
    packageId,
    title: decodeEntities(titleMatch[1]).trim(),
    sectionFound: true,
    reviews,
    /** Cards seen vs. reviews read: a gap means some card had no rating anchor. */
    cardsSeen: cards.length,
    unparsed,
    url: null,
  };
}

/**
 * Fetches one app's page and returns its reviews.
 *
 * Throws `PlayReviewsError` on a non-2xx response or an unparseable page instead of returning an
 * empty list: a caller deciding what users complain about must be able to tell "nobody wrote
 * anything" from "the fetch failed".
 */
export async function fetchReviews(packageId, { gl = 'US', hl = 'en', fetchImpl = fetch } = {}) {
  if (typeof packageId !== 'string' || packageId.trim().length === 0) {
    throw new PlayReviewsError('INVALID_PACKAGE_ID', 'packageId must be a non-empty string');
  }

  const url = `${DETAIL_ENDPOINT}?id=${encodeURIComponent(packageId)}&hl=${hl}&gl=${gl}`;
  const response = await fetchImpl(url, { headers: BROWSER_HEADERS });

  if (!response.ok) {
    throw new PlayReviewsError(
      'HTTP_ERROR',
      `Play returned HTTP ${response.status} for ${packageId}`,
      { status: response.status, url },
    );
  }

  const html = await response.text();
  return { ...parseReviewsHtml(html, packageId), url };
}

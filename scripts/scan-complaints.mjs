/**
 * PlayKiln complaint scan — what the incumbents' own users say is wrong with them.
 *
 * Why this exists
 * ---------------
 * The niche screen (`docs/niche-selection.md`) proves demand from *ratings* and size from *install
 * buckets*, and both are silent about cause. This CLI adds the missing fact: the review text. Its
 * question is "what is wrong with the apps already in this pocket", because that is the only kind
 * of answer a new app can act on.
 *
 * Usage
 * -----
 *   node scripts/scan-complaints.mjs --query "mushroom foraging guide" --top 5
 *   node scripts/scan-complaints.mjs --package com.mushroomhunters --package com.shroomid
 *   node scripts/scan-complaints.mjs --query "visual schedule autism" --top 6 --json
 *   node scripts/scan-complaints.mjs --query "bolt torque calculator" --top 5 --out docs/report.md
 *
 * With `--query`, the weakest rated incumbents are chosen on purpose: the screen's whole premise
 * is that a weak mid-tier is the reachable pocket, so reading *its* complaints is more useful than
 * reading the leader's.
 *
 * What this cannot tell you
 * -------------------------
 * - **The sample is Play's, not ours.** Play server-renders a fixed handful of reviews per page
 *   (measured: 3) chosen by its own ranking. Three reviews are a smell, not a survey.
 * - **`--top N` apps means at most `3 x N` reviews.** A histogram over 15 reviews shows the
 *   vocabulary of a pocket; it does not rank complaint frequency in the population.
 * - **The classifier is lexical** — see `scripts/lib/play-reviews.mjs`. Every report prints the
 *   verbatim texts, so a human can see what the counts came from, and prints the unclassified
 *   count so the blind spot is visible too.
 * - **Ratings-only screens still apply.** A complaint field over unrated apps is still a dead zone.
 */
import { parseArgs } from 'node:util';
import { scanQuery } from './lib/play-scan.mjs';
import { fetchAppDetail } from './lib/play-app-detail.mjs';
import {
  COMPLAINT_CLASSES,
  COMPLAINT_CLASS_IDS,
  CRITICAL_RATING,
  fetchReviews,
  summariseComplaints,
} from './lib/play-reviews.mjs';

const { values } = parseArgs({
  options: {
    query: { type: 'string', multiple: true, default: [] },
    package: { type: 'string', multiple: true, default: [] },
    top: { type: 'string', default: '5' },
    concurrency: { type: 'string', default: '2' },
    json: { type: 'boolean', default: false },
    out: { type: 'string' },
    help: { type: 'boolean', short: 'h', default: false },
  },
  allowPositionals: false,
});

const HELP = `PlayKiln complaint scan — what the incumbents' own users say is wrong with them.

Usage:
  node scripts/scan-complaints.mjs --query "<play search query>" [--top <n>] [options]
  node scripts/scan-complaints.mjs --package <id> [--package <id> ...] [options]

Options:
  --query <text>      Screen this Play search query and read the weakest rated incumbents
                      (repeatable). Weakest-first on purpose: the weak mid-tier is the
                      reachable pocket the niche screen looks for.
  --package <id>      Read these packages directly (repeatable). Skips the search.
  --top <n>           How many incumbents to read per query (default 5).
  --concurrency <n>   Parallel fetches (default 2). Bounded on purpose.
  --json              Print machine-readable JSON instead of the readable report.
  --out <path>        Also write a markdown report (path relative to the repo root).
  --help, -h          Show this help.

Reading the output:
  critical   reviews rated ${CRITICAL_RATING} or lower — the complaints this report is about.
  no text    critical reviews that are rating-only, so they carry no complaint vocabulary.
  unclassified  critical reviews the lexical classifier did not recognise. Printed, never
                folded into a class, because a hidden blind spot is the failure mode.

Limitations:
  - Play server-renders a SAMPLE of its own choosing, measured at 3 reviews per page. A
    histogram over 15 reviews shows a pocket's vocabulary, not its complaint frequency.
  - The classifier is lexical; every text it read is printed so the counts can be audited.
  - This does not replace the ratings screen. Complaints over unrated apps are still a dead zone.
`;

if (values.help || (values.query.length === 0 && values.package.length === 0)) {
  console.log(HELP);
  process.exit(values.help ? 0 : 2);
}

const TOP = Number.parseInt(values.top, 10);
const CONCURRENCY = Number.parseInt(values.concurrency, 10);
if (!Number.isFinite(TOP) || TOP < 1) {
  console.error(`--top must be a positive integer, got ${JSON.stringify(values.top)}`);
  process.exit(2);
}

/** Runs `worker` over `items` with a bounded number of parallel fetches. */
async function mapBounded(items, limit, worker) {
  const out = new Array(items.length);
  let next = 0;
  const runners = Array.from({ length: Math.max(1, Math.min(limit, items.length)) }, async () => {
    while (next < items.length) {
      const index = next;
      next += 1;
      out[index] = await worker(items[index], index);
    }
  });
  await Promise.all(runners);
  return out;
}

/**
 * Picks the apps to read reviews from.
 *
 * For a query: rated cards only, weakest first. An unrated card is skipped rather than included,
 * because `play-scan.mjs` treats an unrated incumbent as a failed attempt, not a weak one — reading
 * the complaints of an app nobody installed answers a different question.
 */
async function selectFromQuery(query, top) {
  const scan = await scanQuery(query);
  if (scan.error) throw new Error(`search failed for ${JSON.stringify(query)}: ${scan.error}`);

  const rated = scan.results.filter((r) => r.rated);
  const weakest = [...rated].sort((a, b) => a.rating - b.rating).slice(0, top);

  return {
    query,
    verdict: scan.verdict,
    reason: scan.reason,
    relevance: scan.relevance,
    seen: scan.results.length,
    ratedSeen: rated.length,
    selected: weakest.map((r) => ({ packageId: r.packageId, cardTitle: r.title, cardRating: r.rating })),
  };
}

/** Reads facts and reviews for one package. Returns a record the report can print as-is. */
async function readApp(entry) {
  const [detail, reviews] = await Promise.all([
    fetchAppDetail(entry.packageId).catch((err) => ({ error: err.message, code: err.code })),
    fetchReviews(entry.packageId).catch((err) => ({ error: err.message, code: err.code })),
  ]);

  if (detail.error || reviews.error) {
    return {
      ...entry,
      error: detail.error ?? reviews.error,
      errorCode: detail.code ?? reviews.code,
    };
  }

  return {
    ...entry,
    title: detail.title,
    developer: detail.developer,
    rating: detail.rating,
    ratingCountExact: detail.ratingCountExact,
    installs: detail.installs,
    containsAds: detail.containsAds,
    inAppPurchases: detail.inAppPurchases,
    reviewsRead: reviews.reviews.length,
    sectionFound: reviews.sectionFound,
    reviews: reviews.reviews,
    summary: summariseComplaints(reviews.reviews),
  };
}

/** Sums complaint counts across apps, so the pocket has one profile rather than N. */
function aggregate(apps) {
  const totals = Object.fromEntries(COMPLAINT_CLASS_IDS.map((id) => [id, 0]));
  let reviewsRead = 0;
  let criticalRead = 0;
  let withoutText = 0;
  let unclassified = 0;

  for (const app of apps) {
    if (!app.summary) continue;
    reviewsRead += app.summary.reviewsRead;
    criticalRead += app.summary.criticalRead;
    withoutText += app.summary.withoutText;
    unclassified += app.summary.unclassified;
    for (const id of COMPLAINT_CLASS_IDS) totals[id] += app.summary.counts[id];
  }

  const ranked = COMPLAINT_CLASS_IDS.filter((id) => totals[id] > 0)
    .map((id) => ({
      id,
      label: COMPLAINT_CLASSES.find((e) => e.id === id).label,
      count: totals[id],
      shareOfCritical: criticalRead > 0 ? totals[id] / criticalRead : 0,
    }))
    .sort((a, b) => b.count - a.count || a.id.localeCompare(b.id));

  return { totals, ranked, reviewsRead, criticalRead, withoutText, unclassified };
}

function renderMarkdown(report) {
  const lines = [];
  lines.push(`# PlayKiln complaint scan — ${report.generatedAt}`);
  lines.push('');
  lines.push(
    `Reads the review text Play server-renders on each incumbent's page, and sorts it into named ` +
      `complaint classes. Critical means a review rated ${CRITICAL_RATING}★ or lower.`,
  );
  lines.push('');
  lines.push('## Limitation, stated before the numbers');
  lines.push('');
  lines.push(
    `- Play server-renders a **sample** of its own choosing, measured at **3 reviews per page**. ` +
      `These counts describe the vocabulary of the pocket, not the frequency of complaints in it.`,
  );
  lines.push(
    `- The classifier is **lexical** (see \`scripts/lib/play-reviews.mjs\`). Every text it read is ` +
      `printed below so the counts can be audited, and the unclassified count is printed so its ` +
      `blind spot is visible.`,
  );
  lines.push('');

  for (const group of report.groups) {
    lines.push(`## ${group.source}`);
    lines.push('');
    if (group.verdict) {
      lines.push(`Ratings screen: **${group.verdict}** — ${group.reason}`);
      lines.push('');
      lines.push(`Visible cards: ${group.seen}; rated: ${group.ratedSeen}. Selected weakest ${group.apps.length}.`);
      lines.push('');
    }

    lines.push('| package | title | rating | reviews read | critical | top complaint |');
    lines.push('| --- | --- | --- | --- | --- | --- |');
    for (const app of group.apps) {
      if (app.error) {
        lines.push(`| \`${app.packageId}\` | _failed: ${app.errorCode ?? 'error'}_ | — | — | — | — |`);
        continue;
      }
      const top = app.summary.ranked[0];
      lines.push(
        `| \`${app.packageId}\` | ${app.title} | ${app.rating ?? 'unrated'} | ${app.reviewsRead} | ` +
          `${app.summary.criticalRead} | ${top ? `${top.id} (${top.count})` : '—'} |`,
      );
    }
    lines.push('');

    const agg = group.aggregate;
    lines.push('### Pocket complaint profile');
    lines.push('');
    lines.push(
      `${agg.reviewsRead} reviews read, ${agg.criticalRead} critical (<= ${CRITICAL_RATING}★), ` +
        `${agg.withoutText} critical without text, ${agg.unclassified} critical not classified.`,
    );
    lines.push('');
    if (agg.ranked.length === 0) {
      lines.push('_No complaint class fired in this sample._');
    } else {
      lines.push('| complaint class | reviews | share of critical |');
      lines.push('| --- | --- | --- |');
      for (const row of agg.ranked) {
        lines.push(`| ${row.label} (\`${row.id}\`) | ${row.count} | ${(row.shareOfCritical * 100).toFixed(0)}% |`);
      }
    }
    lines.push('');

    lines.push('### Verbatim critical reviews');
    lines.push('');
    for (const app of group.apps) {
      if (app.error || !app.reviews) continue;
      const critical = app.reviews.filter(
        (r) => typeof r.rating === 'number' && r.rating <= CRITICAL_RATING && typeof r.text === 'string',
      );
      if (critical.length === 0) continue;
      lines.push(`**${app.title}** (\`${app.packageId}\`, ${app.rating ?? 'unrated'}★)`);
      lines.push('');
      for (const review of critical) {
        lines.push(`- ${review.rating}★ (${review.date ?? 'date unknown'}): ${review.text}`);
      }
      lines.push('');
    }
  }

  lines.push('## Classes defined');
  lines.push('');
  for (const entry of COMPLAINT_CLASSES) {
    lines.push(`- \`${entry.id}\` — ${entry.label}`);
  }
  lines.push('');

  return lines.join('\n');
}

const groups = [];

for (const query of values.query) {
  const selection = await selectFromQuery(query, TOP);
  console.log(`\n[${query}] ${selection.verdict} — selecting ${selection.selected.length} weakest rated incumbent(s)`);
  const apps = await mapBounded(selection.selected, CONCURRENCY, readApp);
  groups.push({
    source: `query: ${query}`,
    verdict: selection.verdict,
    reason: selection.reason,
    relevance: selection.relevance?.level ?? null,
    seen: selection.seen,
    ratedSeen: selection.ratedSeen,
    apps,
    aggregate: aggregate(apps),
  });
}

if (values.package.length > 0) {
  const entries = values.package.map((packageId) => ({ packageId, cardTitle: null, cardRating: null }));
  console.log(`\n[explicit packages] reading ${entries.length} app(s)`);
  const apps = await mapBounded(entries, CONCURRENCY, readApp);
  groups.push({
    source: 'explicit packages',
    verdict: null,
    reason: null,
    relevance: null,
    seen: entries.length,
    ratedSeen: apps.filter((a) => a.rating !== undefined && a.rating !== null).length,
    apps,
    aggregate: aggregate(apps),
  });
}

const report = { generatedAt: new Date().toISOString(), criticalRating: CRITICAL_RATING, groups };

if (values.json) {
  console.log(JSON.stringify(report, null, 2));
} else {
  for (const group of groups) {
    console.log(`\n=== ${group.source} ===`);
    if (group.verdict) console.log(`screen: ${group.verdict} — ${group.reason}`);
    for (const app of group.apps) {
      if (app.error) {
        console.log(`  ${app.packageId}: FETCH FAILED (${app.errorCode ?? 'error'}) ${app.error}`);
        continue;
      }
      const top = app.summary.ranked[0];
      console.log(
        `  ${app.packageId}  ${app.rating ?? 'unrated'}★  ${app.installs ?? '?'}  ` +
          `reviews=${app.reviewsRead} critical=${app.summary.criticalRead} ` +
          `top=${top ? `${top.id}(${top.count})` : 'none'} unclassified=${app.summary.unclassified}`,
      );
      for (const row of app.summary.ranked.slice(0, 4)) {
        console.log(`      ${String(row.count).padStart(2)}  ${row.id} — ${row.label}`);
      }
      const critical = app.reviews.filter(
        (r) => typeof r.rating === 'number' && r.rating <= CRITICAL_RATING && typeof r.text === 'string',
      );
      for (const review of critical.slice(0, 3)) {
        console.log(`      ${review.rating}★ ${JSON.stringify(review.text.slice(0, 150))}`);
      }
    }
    const agg = group.aggregate;
    console.log(
      `  -- pocket: ${agg.reviewsRead} reviews, ${agg.criticalRead} critical, ` +
        `${agg.withoutText} no text, ${agg.unclassified} unclassified`,
    );
    for (const row of agg.ranked) {
      console.log(`     ${String(row.count).padStart(3)}  ${row.id} (${(row.shareOfCritical * 100).toFixed(0)}% of critical)`);
    }
  }
}

if (values.out) {
  const { writeFileSync, mkdirSync } = await import('node:fs');
  const { dirname, resolve } = await import('node:path');
  const target = resolve(process.cwd(), values.out);
  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, renderMarkdown(report), 'utf8');
  console.log(`\nmarkdown report written to ${values.out}`);
}

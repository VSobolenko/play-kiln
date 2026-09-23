#!/usr/bin/env node
/**
 * scan-plant-vertical.mjs — screen one *vertical* (edible-garden crop health) and enrich its top
 * cards, so the "the vertical is unowned" hypothesis is tested with store data instead of assumed.
 *
 * Why this exists
 * ---------------
 * `docs/market-enrichment-2026-09-23.md` measured the GENERIC niche (`plant disease identifier`)
 * and found it owned by PictureThis at 50M+/800,000 ratings, with 8/8 enriched apps declaring
 * in-app purchases. The factory's next bet is a *different job* inside the same domain — "my tomato
 * has spots, tell me what to do" — on the theory that the crop-specific question is not already
 * owned. That theory is a hypothesis, so this CLI answers the two questions a ratings-only screen
 * cannot:
 *
 *   1. How big is anything in the vertical? (install buckets — `play-app-detail.mjs`)
 *   2. Does the vertical monetise at all, and how? (ads / IAP badges)
 *
 * Pipeline, reusing the validated libraries unmodified:
 *
 *   scripts/lib/play-scan.mjs    -> screen the vertical query set (ratings-only verdict)
 *   scripts/lib/play-app-detail.mjs -> per-app install bucket, ads, IAP, price, review count
 *
 * Honesty rules, inherited from the two libraries and not relaxed here
 * --------------------------------------------------------------------
 * - `installs` is Play's rounded bucket (`5M+`), so every size statement is a floor, never a count.
 * - A `null` badge means the header badge region was absent: the fact was NOT observable. It is
 *   printed as `unknown` and counted in its own column — never folded into `false` or `0`.
 * - A `null` rating means genuinely unrated, which is the `dead-zone` signal itself.
 * - Nothing is invented: every number in the report is the output of this run.
 *
 * Politeness
 * ----------
 * Every enriched app is a separate page fetch, so concurrency defaults to 3 (matching
 * `enrich-market.mjs`, and lower than the search scanner's 4) and every request carries a timeout.
 * A failed detail fetch is retried once, then reported as a failure that drives a non-zero exit.
 *
 * Exit codes:
 *   0  every query returned visible results and every selected app detail was fetched and parsed
 *   1  at least one query returned nothing, or at least one app detail failed (named in FAILURES)
 *   2  every query failed, or the invocation itself was invalid (bad flag, no queries)
 *
 * Usage:
 *   node scripts/scan-plant-vertical.mjs
 *   node scripts/scan-plant-vertical.mjs --query "tomato disease" --top 8 --concurrency 2
 *   node scripts/scan-plant-vertical.mjs --json
 *   node scripts/scan-plant-vertical.mjs --out docs/research/foo.md
 *   node scripts/scan-plant-vertical.mjs --help
 *
 * Scope: signed-out Play search (`?c=apps`) + app-detail pages, `hl=en`, `gl=US`. One storefront,
 * one session — a spot check, not a market study.
 */

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, isAbsolute, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { PlayAppDetailError, fetchAppDetail } from './lib/play-app-detail.mjs';
import { WEAK_RATING, STRONG_RATING, formatTable, scanQueries } from './lib/play-scan.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');

/**
 * The vertical's built-in query set.
 *
 * The first eight are the queries this screen was commissioned with; the rest are more of the same
 * kind (an edible-crop symptom, a treatment, or a pest), added so the vertical is not judged on a
 * single phrasing. All of them describe the grower's job — "my crop is sick, what do I do" — rather
 * than plant identification in general, which is the whole point of the comparison.
 */
const VERTICAL_QUERIES = [
  'tomato disease',
  'tomato blight',
  'cucumber plant disease',
  'pepper plant disease',
  'vegetable garden disease',
  'powdery mildew treatment',
  'plant fungus treatment',
  'garden pest identifier',
  'tomato leaf curl',
  'crop disease identification',
  'late blight',
  'aphid treatment plants',
  'whitefly treatment',
  'plant fungicide guide',
];

/** Default cards enriched per query. Kept modest: each one is a full page fetch. */
const DEFAULT_TOP = 8;

/** Default parallel fetches for BOTH stages. Deliberately low: one page fetch per app. */
const DEFAULT_CONCURRENCY = 3;

/** Play occasionally serves a slow or interstitial page; a hung socket is not a parse failure. */
const REQUEST_TIMEOUT_MS = 30_000;

/** One retry, so a transient bot-check page does not become a hole in the evidence. */
const RETRY_DELAY_MS = 1_500;

const UNKNOWN = 'unknown';

const USAGE = `PlayKiln plant-vertical screen — is the edible-crop-health vertical owned or fragmented?

Usage:
  node scripts/scan-plant-vertical.mjs [options]

Options:
  --query <text>      Screen this query instead of the built-in vertical set (repeatable; values
                      replace the built-in set, they are not added to it).
  --top <n>           Enrich the first n result cards per query (default ${DEFAULT_TOP}).
  --concurrency <n>   Parallel fetches for search AND app-detail stages (default ${DEFAULT_CONCURRENCY}).
                      Each enriched app is a separate page fetch, so this stays low on purpose.
  --json              Print machine-readable JSON to stdout instead of the tables.
  --out <path>        Also write a markdown report (path is relative to the repo root).
  --help, -h          Show this help.

Built-in vertical query set (${VERTICAL_QUERIES.length} queries):
${VERTICAL_QUERIES.map((query) => `  - ${query}`).join('\n')}

Columns:
  rating    the app's star rating, or "unrated" — "unrated" is the signal, not a missing value.
  reviews   Play's displayed count (e.g. "49K"); the unrounded schema.org twin in parentheses when
            the page server-renders it.
  installs  Play's rounded bucket (e.g. "5M+") plus the lower bound it implies (>= 5000000).
  ads / iap true = Play declares the badge, false = the badge region was present and did not
            declare it, unknown = the badge region was absent, so the fact was not observable.

Exit codes: 0 all fetches succeeded; 1 a query returned nothing or an app detail failed; 2 all
queries failed or the invocation was invalid.
`;

function fail(message) {
  process.stderr.write(`scan-plant-vertical: ${message}\n\n${USAGE}`);
  process.exit(2);
}

function parseArgs(argv) {
  const options = {
    queries: [],
    top: DEFAULT_TOP,
    concurrency: DEFAULT_CONCURRENCY,
    json: false,
    out: null,
    help: false,
  };

  const value = (flag, index) => {
    const inline = argv[index].includes('=') ? argv[index].slice(argv[index].indexOf('=') + 1) : null;
    if (inline !== null) return inline;
    const next = argv[index + 1];
    if (next === undefined || next.startsWith('--')) fail(`${flag} needs a value`);
    return next;
  };

  const positiveInteger = (flag, raw) => {
    const parsed = Number(raw);
    if (!Number.isInteger(parsed) || parsed < 1) {
      fail(`${flag} must be a positive integer (got "${raw}")`);
    }
    return parsed;
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--help' || arg === '-h') options.help = true;
    else if (arg === '--json') options.json = true;
    else if (arg === '--query' || arg.startsWith('--query=')) {
      options.queries.push(value('--query', index));
      if (!arg.includes('=')) index += 1;
    } else if (arg === '--top' || arg.startsWith('--top=')) {
      options.top = positiveInteger('--top', value('--top', index));
      if (!arg.includes('=')) index += 1;
    } else if (arg === '--concurrency' || arg.startsWith('--concurrency=')) {
      options.concurrency = positiveInteger('--concurrency', value('--concurrency', index));
      if (!arg.includes('=')) index += 1;
    } else if (arg === '--out' || arg.startsWith('--out=')) {
      options.out = value('--out', index);
      if (!arg.includes('=')) index += 1;
    } else {
      fail(`unknown argument "${arg}"`);
    }
  }

  return options;
}

/**
 * Every request gets a timeout, so a stalled socket surfaces as a reported failure instead of
 * hanging the run. Node's `fetch` has no default timeout.
 */
function timedFetch(url, init = {}) {
  return fetch(url, { ...init, signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS) });
}

const sleep = (ms) => new Promise((done) => setTimeout(done, ms));

/** Fetches one app detail, retrying once. Never throws: the outcome is part of the record. */
async function detailWithRetry(packageId, options) {
  let lastError = null;
  for (let attempt = 1; attempt <= 2; attempt += 1) {
    try {
      const detail = await fetchAppDetail(packageId, options);
      return { ok: true, packageId, attempts: attempt, detail };
    } catch (error) {
      lastError = error;
      if (attempt < 2) await sleep(RETRY_DELAY_MS);
    }
  }

  return {
    ok: false,
    packageId,
    attempts: 2,
    error: {
      code: lastError instanceof PlayAppDetailError ? lastError.code : 'UNKNOWN',
      message: String(lastError?.message ?? lastError),
    },
  };
}

/** Runs `worker` over `items` with bounded concurrency, preserving input order in the result. */
async function mapWithConcurrency(items, concurrency, worker) {
  const results = new Array(items.length);
  const queue = items.map((item, index) => ({ item, index }));

  const runners = Array.from({ length: Math.max(1, Math.min(concurrency, queue.length)) }, async () => {
    while (queue.length > 0) {
      const { item, index } = queue.shift();
      results[index] = await worker(item, index);
    }
  });

  await Promise.all(runners);
  return results;
}

function displayCount(value) {
  if (value === null || value === undefined) return UNKNOWN;
  return value.toLocaleString('en-US');
}

function displayFree(detail) {
  if (detail.free === true) return 'free';
  if (detail.free === false) return `paid (${detail.price ?? UNKNOWN})`;
  return UNKNOWN;
}

/** `true` / `false` / `unknown` — a null badge is never flattened into "no". */
function displayDeclared(value) {
  if (value === true) return 'true';
  if (value === false) return 'false';
  return UNKNOWN;
}

/**
 * Three-way tally for a badge field.
 *
 * Kept three-way on purpose: `declared` (true), `declaredAbsent` (false) and `unknown` (null).
 * Collapsing the last two would claim a fact this run never observed, which is the failure mode
 * `play-app-detail.mjs` documents at length.
 */
function tallyBadge(details, field) {
  return {
    declared: details.filter((detail) => detail[field] === true).length,
    declaredAbsent: details.filter((detail) => detail[field] === false).length,
    unknown: details.filter((detail) => detail[field] === null).length,
  };
}

function enrichedDetails(apps) {
  return apps.filter((app) => app.ok).map((app) => app.detail);
}

/** Largest install bucket among the enriched apps, or null when no bucket was readable. */
function largestBucket(details) {
  const known = details.filter((detail) => detail.installsMin !== null);
  if (known.length === 0) return null;
  const max = Math.max(...known.map((detail) => detail.installsMin));
  const atMax = known.filter((detail) => detail.installsMin === max);
  return {
    installsMin: max,
    labels: [...new Set(atMax.map((detail) => detail.installs))],
    apps: atMax.map((detail) => ({
      packageId: detail.packageId,
      title: detail.title,
      developer: detail.developer,
      rating: detail.rating,
      ratingCount: detail.ratingCount,
      installs: detail.installs,
    })),
  };
}

function summarise(apps) {
  const details = enrichedDetails(apps);
  return {
    enriched: details.length,
    ads: tallyBadge(details, 'containsAds'),
    iap: tallyBadge(details, 'inAppPurchases'),
    largest: largestBucket(details),
    installsUnknown: details.filter((detail) => detail.installsMin === null).length,
    rated: details.filter((detail) => detail.rating !== null).length,
    unrated: details.filter((detail) => detail.rating === null).length,
  };
}

function clip(text, width) {
  const value = String(text ?? '');
  return value.length <= width ? value : `${value.slice(0, width - 3)}...`;
}

function renderTextTable(apps) {
  const rows = apps.map((app, index) => {
    const position = String(index + 1);
    if (!app.ok) {
      return [position, app.packageId, 'FETCH FAILED', '-', '-', '-', '-', '-', '-', '-', app.error.code];
    }
    const d = app.detail;
    const reviews =
      d.ratingCount === null
        ? UNKNOWN
        : d.ratingCountExact !== null
          ? `${displayCount(d.ratingCount)} (${displayCount(d.ratingCountExact)})`
          : displayCount(d.ratingCount);
    return [
      position,
      d.packageId,
      clip(d.title, 32),
      clip(d.developer ?? UNKNOWN, 22),
      d.rating === null ? 'unrated' : d.rating.toFixed(1),
      reviews,
      d.installs ?? UNKNOWN,
      d.installsMin === null ? UNKNOWN : `>= ${displayCount(d.installsMin)}`,
      displayDeclared(d.containsAds),
      displayDeclared(d.inAppPurchases),
      displayFree(d),
    ];
  });

  const header = ['#', 'package', 'title', 'developer', 'rating', 'reviews (exact)', 'installs', 'bound', 'ads', 'iap', 'price'];
  const all = [header, ...rows];
  const widths = header.map((_, column) => Math.max(...all.map((row) => row[column].length)));

  return all
    .map((row) => row.map((cell, column) => cell.padEnd(widths[column])).join('  ').trimEnd())
    .join('\n')
    .replace('\n', `\n${widths.map((width) => '-'.repeat(width)).join('  ')}\n`);
}

function escapeCell(text) {
  return String(text).replace(/\|/g, '\\|').replace(/\r?\n/g, ' ');
}

function markdownTable(header, rows) {
  const lines = [`| ${header.join(' | ')} |`, `| ${header.map(() => '---').join(' | ')} |`];
  for (const row of rows) lines.push(`| ${row.map(escapeCell).join(' | ')} |`);
  return lines.join('\n');
}

const APP_TABLE_HEADER = [
  '#',
  'package id',
  'title',
  'developer',
  'rating',
  'reviews',
  'reviews (exact)',
  'installs',
  'install bound',
  'containsAds',
  'inAppPurchases',
  'price',
];

function markdownAppRows(apps) {
  return apps.map((app, index) => {
    const position = String(index + 1);
    if (!app.ok) {
      return [position, app.packageId, '**FETCH FAILED**', '-', '-', '-', '-', '-', '-', '-', '-', app.error.code];
    }
    const d = app.detail;
    return [
      position,
      `\`${d.packageId}\``,
      d.title,
      d.developer ?? UNKNOWN,
      d.rating === null ? 'unrated' : d.rating.toFixed(1),
      displayCount(d.ratingCount),
      d.ratingCountExact === null ? UNKNOWN : displayCount(d.ratingCountExact),
      d.installs ?? UNKNOWN,
      d.installsMin === null ? UNKNOWN : displayCount(d.installsMin),
      displayDeclared(d.containsAds),
      displayDeclared(d.inAppPurchases),
      displayFree(d),
    ];
  });
}

function summaryLines(summary) {
  const largest = summary.largest
    ? `${summary.largest.labels.join(' / ')} (>= ${displayCount(summary.largest.installsMin)})`
    : UNKNOWN;
  return [
    `enriched apps: ${summary.enriched}`,
    `ads — declared: ${summary.ads.declared}, declared absent: ${summary.ads.declaredAbsent}, unknown: ${summary.ads.unknown}`,
    `in-app purchases — declared: ${summary.iap.declared}, declared absent: ${summary.iap.declaredAbsent}, unknown: ${summary.iap.unknown}`,
    `largest install bucket seen: ${largest}`,
    `installs unknown: ${summary.installsUnknown}`,
    `rated / unrated among enriched: ${summary.rated} / ${summary.unrated}`,
  ];
}

/**
 * Apps that appear in more than one query's enriched set.
 *
 * This is the objective form of "the same generalists answer every crop question": if one package
 * id answers `tomato disease`, `pepper plant disease` and `garden pest identifier`, it is a
 * general-purpose app, not a tomato tool. Computed from package ids only — no title guessing.
 */
function crossQueryRepeats(records) {
  const byPackage = new Map();
  for (const record of records) {
    if (record.searchError) continue;
    for (const app of record.apps) {
      if (!app.ok) continue;
      const entry = byPackage.get(app.packageId) ?? {
        packageId: app.packageId,
        title: app.detail.title,
        developer: app.detail.developer,
        installs: app.detail.installs,
        installsMin: app.detail.installsMin,
        queries: [],
      };
      entry.queries.push(record.query);
      byPackage.set(app.packageId, entry);
    }
  }
  return [...byPackage.values()]
    .filter((entry) => entry.queries.length > 1)
    .sort((a, b) => b.queries.length - a.queries.length || a.packageId.localeCompare(b.packageId));
}

function resolveOutputPath(target) {
  return isAbsolute(target) ? target : resolve(ROOT, target);
}

function renderReport({ generatedAt, command, options, queryRecords, failures, repeats, totals, exitCode, outPath }) {
  const lines = [];
  lines.push(`# Plant vertical (edible-crop health) screen — ${generatedAt.slice(0, 10)}`);
  lines.push('');
  lines.push(`Generated by \`node ${command}\` at ${generatedAt}.`);
  lines.push('');
  lines.push(
    `Top ${options.top} card(s) per query enriched, fetch concurrency ${options.concurrency} (search and detail). Signed-out Play (\`hl=en\`, \`gl=US\`).`,
  );
  lines.push('');
  lines.push(
    'Search cards from `scripts/lib/play-scan.mjs`; per-app facts from `scripts/lib/play-app-detail.mjs` (both unmodified). Every number below was fetched in this run; `installs` is Play\'s rounded bucket, so the bound column is a lower bound, not a count, and `unknown` is never rendered as `false` or `0`.',
  );
  lines.push('');
  lines.push(`Exit code ${exitCode}.`);
  lines.push('');
  lines.push('## Query set');
  lines.push('');
  for (const record of queryRecords) lines.push(`- \`${record.query}\``);
  lines.push('');

  lines.push('## Ratings-only screen (per query)');
  lines.push('');
  lines.push('```');
  lines.push(formatTable(queryRecords.map((record) => record.scan)).trimEnd());
  lines.push('```');
  lines.push('');

  for (const record of queryRecords) {
    lines.push(`## \`${record.query}\``);
    lines.push('');
    if (record.searchError) {
      lines.push(`**Search failed:** ${record.searchError}`);
      lines.push('');
      continue;
    }
    lines.push(
      `Search returned ${record.resultCount} visible card(s); ${record.apps.length} enriched (top ${options.top}). Verdict: **${record.verdict}** — ${record.reason}`,
    );
    lines.push('');
    // The verdict is reported exactly as the library returns it; this states whether the apps it
    // was computed from answer the query, so a polluted `candidate` cannot be read as a GO.
    if (record.relevance?.suspect) {
      lines.push(`**Relevance warning (${record.relevance.level}):** ${record.relevance.warning}`);
      lines.push('');
    }
    lines.push(markdownTable(APP_TABLE_HEADER, markdownAppRows(record.apps)));
    lines.push('');
    lines.push('Summary (derived from the fetched apps only):');
    lines.push('');
    for (const line of summaryLines(record.summary)) lines.push(`- ${line}`);
    if (record.summary.largest) {
      const owners = record.summary.largest.apps
        .map((app) => `\`${app.packageId}\` — ${app.title} (${app.developer ?? UNKNOWN})`)
        .join('; ');
      lines.push(`- app(s) at the largest bucket: ${owners}`);
    }
    lines.push('');
  }

  lines.push('## Monetisation across the whole vertical');
  lines.push('');
  lines.push(
    `Across ${totals.enriched} enriched app(s) (every query combined, fetched in this run). Counts are three-way: \`declared\` = Play showed the badge, \`declared absent\` = the badge region was present and did not show it, \`unknown\` = the badge region was absent so the fact was not observable.`,
  );
  lines.push('');
  lines.push(
    markdownTable(
      ['signal', 'declared', 'declared absent', 'unknown'],
      [
        ['contains ads', String(totals.ads.declared), String(totals.ads.declaredAbsent), String(totals.ads.unknown)],
        ['in-app purchases', String(totals.iap.declared), String(totals.iap.declaredAbsent), String(totals.iap.unknown)],
      ],
    ),
  );
  lines.push('');
  lines.push(`- apps declaring ads and no IAP: ${totals.adsOnly}`);
  lines.push(`- apps declaring IAP and no ads: ${totals.iapOnly}`);
  lines.push(`- apps declaring both: ${totals.both}`);
  lines.push(`- apps declaring neither (badge region present): ${totals.neither}`);
  lines.push(`- apps free / paid / unknown price: ${totals.free} / ${totals.paid} / ${totals.priceUnknown}`);
  lines.push('');

  lines.push('## Apps answering more than one query');
  lines.push('');
  if (repeats.length === 0) {
    lines.push(
      'No package id appeared in the enriched set of two different queries in this run, so no app here demonstrably answers several crop questions at once.',
    );
  } else {
    lines.push(
      'A package id that appears under several crop queries is answering them with one general app. This is computed from package ids only, not from titles.',
    );
    lines.push('');
    lines.push(
      markdownTable(
        ['package id', 'title', 'developer', 'installs', 'queries answered'],
        repeats.map((entry) => [
          `\`${entry.packageId}\``,
          entry.title,
          entry.developer ?? UNKNOWN,
          entry.installs ?? UNKNOWN,
          String(entry.queries.length),
        ]),
      ),
    );
    lines.push('');
    for (const entry of repeats) {
      lines.push(`- \`${entry.packageId}\` — ${entry.queries.map((query) => `\`${query}\``).join(', ')}`);
    }
  }
  lines.push('');

  lines.push('## Failures');
  lines.push('');
  if (failures.length === 0) {
    lines.push('None: every query returned visible results and every selected app detail was fetched and parsed.');
  } else {
    for (const failure of failures) {
      lines.push(`- \`${failure.query}\` -> ${failure.packageId ?? '(search)'} — ${failure.message}`);
    }
  }
  lines.push('');

  lines.push('## What this cannot tell us');
  lines.push('');
  lines.push(
    '- **Install buckets are floors, not counts.** `5M+` means "at least 5,000,000"; two apps in the same bucket are indistinguishable, and `500K+` spans 500,000–999,999.',
  );
  lines.push(
    `- **Only the top ${options.top} card(s) of each query were enriched.** Play's relevance order is not size order, so a larger incumbent can sit below the cut, and the ~30-card display cap hides everything past it.`,
  );
  lines.push(
    '- **Revenue, retention and conversion are invisible.** A declared badge proves a model was implemented, not that it earns; the page shows the app price, not the in-app price points.',
  );
  lines.push(
    '- **Whether growers would pay for THIS app is not a store-visible fact.** Declared IAP on incumbents shows the shelf supports it; it says nothing about a new entrant\'s conversion.',
  );
  lines.push(
    '- **One storefront, one signed-out session** (`hl=en`, `gl=US`) at the timestamp above. Play search is personalised and geo-dependent, so this is a spot check, not a market study.',
  );
  lines.push('');

  return `${lines.join('\n')}\n`;
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    process.stdout.write(USAGE);
    process.exit(0);
  }

  const providedQueries = [...new Set(options.queries.map((query) => query.trim()).filter((query) => query.length > 0))];
  const queries = providedQueries.length > 0 ? providedQueries : VERTICAL_QUERIES;
  if (queries.length === 0) fail('nothing to screen: the built-in set is empty and no --query was given');

  const generatedAt = new Date().toISOString();
  const command = `scripts/scan-plant-vertical.mjs ${process.argv.slice(2).join(' ')}`.trim();
  // In --json mode stdout must stay pure JSON, so human-facing lines go to stderr.
  const human = options.json ? (text) => process.stderr.write(`${text}\n`) : (text) => console.log(text);

  human(`plant-vertical screen: ${queries.length} query(ies), top ${options.top}, concurrency ${options.concurrency}`);
  if (providedQueries.length > 0) {
    human('using --query values instead of the built-in vertical set');
  }

  const startedAt = Date.now();
  const failures = [];
  const queryRecords = [];

  // Stage 1: screen every query. Order is restored by scanQueries.
  const scans = await scanQueries(queries, { concurrency: options.concurrency, fetchImpl: timedFetch });

  // Stage 2: enrich each query's top cards. Sequential across queries, bounded within each, so the
  // politeness ceiling holds for the whole run rather than per query.
  for (const scan of scans) {
    if (scan.error) {
      failures.push({ query: scan.query, packageId: null, stage: 'search', message: `search failed: ${scan.error}` });
      queryRecords.push({ query: scan.query, scan, searchError: `search failed: ${scan.error}`, apps: [], resultCount: 0 });
      human(`\n## ${scan.query}\nsearch failed: ${scan.error}`);
      continue;
    }

    if (scan.results.length === 0) {
      failures.push({
        query: scan.query,
        packageId: null,
        stage: 'search',
        message: 'search returned no visible results — that is a failure, not an empty market',
      });
      queryRecords.push({ query: scan.query, scan, searchError: 'search returned no visible results', apps: [], resultCount: 0 });
      human(`\n## ${scan.query}\nsearch returned no visible results`);
      continue;
    }

    const selected = scan.results.slice(0, options.top);
    const apps = await mapWithConcurrency(selected, options.concurrency, (card) =>
      detailWithRetry(card.packageId, { fetchImpl: timedFetch }),
    );

    for (const app of apps) {
      if (app.ok) continue;
      failures.push({
        query: scan.query,
        packageId: app.packageId,
        stage: 'detail',
        message: `${app.error.code}: ${app.error.message}${app.attempts > 1 ? ` (failed ${app.attempts} attempts)` : ''}`,
      });
    }

    const summary = summarise(apps);
    queryRecords.push({
      query: scan.query,
      scan,
      searchError: null,
      resultCount: scan.results.length,
      verdict: scan.verdict,
      reason: scan.reason,
      relevance: scan.relevance,
      url: scan.url,
      apps,
      summary,
    });

    human(`\n## ${scan.query}`);
    human(
      `search returned ${scan.results.length} card(s); enriching top ${selected.length} (of ${options.top} requested) — verdict ${scan.verdict}`,
    );
    // The verdict above is unchanged by the guard; this line is what tells a reader whether the
    // verdict was computed from apps the query actually describes.
    if (scan.relevance?.suspect) {
      human(`relevance guard: ${scan.relevance.level} — ${scan.relevance.warning}`);
    }
    human('');
    human(renderTextTable(apps));
    human('');
    human('summary:');
    for (const line of summaryLines(summary)) human(`  ${line}`);
  }

  // Aggregate tallies over every enriched app across every query.
  const allDetails = queryRecords.flatMap((record) => enrichedDetails(record.apps ?? []));
  const ads = tallyBadge(allDetails, 'containsAds');
  const iap = tallyBadge(allDetails, 'inAppPurchases');
  const both = allDetails.filter((d) => d.containsAds === true && d.inAppPurchases === true).length;
  const adsOnly = allDetails.filter((d) => d.containsAds === true && d.inAppPurchases === false).length;
  const iapOnly = allDetails.filter((d) => d.containsAds === false && d.inAppPurchases === true).length;
  const neither = allDetails.filter((d) => d.containsAds === false && d.inAppPurchases === false).length;
  const totals = {
    enriched: allDetails.length,
    ads,
    iap,
    both,
    adsOnly,
    iapOnly,
    neither,
    free: allDetails.filter((d) => d.free === true).length,
    paid: allDetails.filter((d) => d.free === false).length,
    priceUnknown: allDetails.filter((d) => d.free === null).length,
  };
  const repeats = crossQueryRepeats(queryRecords);

  const elapsedMs = Date.now() - startedAt;
  const succeededQueries = queryRecords.filter((record) => !record.searchError && record.apps.some((app) => app.ok));

  let exitCode = 0;
  if (succeededQueries.length === 0) exitCode = 2;
  else if (failures.length > 0) exitCode = 1;

  human('');
  human(
    `enriched ${totals.enriched} app page(s) across ${queries.length} query(ies) in ${elapsedMs} ms (concurrency ${options.concurrency}); ${failures.length} failure(s); exit ${exitCode}`,
  );
  human(
    `vertical monetisation (all ${totals.enriched} enriched): ads declared ${ads.declared}, iap declared ${iap.declared}; both ${both}, ads-only ${adsOnly}, iap-only ${iapOnly}, neither ${neither}; unknown ads ${ads.unknown}, unknown iap ${iap.unknown}`,
  );
  if (failures.length > 0) {
    for (const failure of failures) {
      human(`  FAILED ${failure.query} -> ${failure.packageId ?? '(search)'}: ${failure.message}`);
    }
  }

  if (options.json) {
    const payload = {
      generatedAt,
      storefront: { hl: 'en', gl: 'US', surface: 'signed-out /store/search?c=apps + /store/apps/details' },
      options: { queries, top: options.top, concurrency: options.concurrency },
      elapsedMs,
      exitCode,
      totals: {
        queries: queries.length,
        queriesWithResults: queryRecords.filter((record) => !record.searchError).length,
        appsEnriched: totals.enriched,
        failures: failures.length,
        ads,
        iap,
        both,
        adsOnly,
        iapOnly,
        neither,
        free: totals.free,
        paid: totals.paid,
        priceUnknown: totals.priceUnknown,
      },
      repeats: repeats.map((entry) => ({ ...entry })),
      failures,
      results: queryRecords.map((record) => ({
        query: record.query,
        searchError: record.searchError,
        resultCount: record.resultCount,
        verdict: record.verdict ?? null,
        reason: record.reason ?? null,
        url: record.url ?? null,
        analysis: record.scan?.analysis ?? null,
        relevance: record.relevance ?? null,
        organisations: record.scan?.organisations ?? [],
        summary: record.summary ?? null,
        apps: (record.apps ?? []).map((app) =>
          app.ok
            ? { ok: true, attempts: app.attempts, ...app.detail }
            : { ok: false, attempts: app.attempts, packageId: app.packageId, error: app.error },
        ),
      })),
    };
    process.stdout.write(`${JSON.stringify(payload, null, 2)}\n`);
  }

  if (options.out) {
    const outPath = resolveOutputPath(options.out);
    const report = renderReport({
      generatedAt,
      command,
      options,
      queryRecords,
      failures,
      repeats,
      totals,
      exitCode,
      outPath: options.out,
    });
    mkdirSync(dirname(outPath), { recursive: true });
    writeFileSync(outPath, report, 'utf8');
    human(`report written: ${outPath}`);
  }

  process.exit(exitCode);
}

main().catch((error) => {
  console.error(`scan-plant-vertical: unexpected failure: ${error?.stack ?? error}`);
  process.exit(2);
});

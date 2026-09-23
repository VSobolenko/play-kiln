#!/usr/bin/env node
/**
 * enrich-market.mjs — turn the ratings-only market screen into a competitive picture.
 *
 * Why this exists
 * ---------------
 * `scripts/lib/play-scan.mjs` reads a search results page, which carries a rating and nothing
 * else. A 4.4★ incumbent with 50K installs and a 4.4★ incumbent with 5M installs look identical
 * in a result card, and neither card says whether anyone pays. `docs/niche-selection.md` §3 step 4
 * resolves that by opening the listing by hand; this CLI does that reading at scale:
 *
 *   search scan (play-scan.mjs) -> top N cards -> per-app detail page (play-app-detail.mjs)
 *
 * What it prints
 * --------------
 * For every enriched app: package id, title, developer, rating, rating count (displayed and the
 * unrounded schema.org twin when Play ships it), the install bucket plus the numeric lower bound
 * the bucket implies, `containsAds`, `inAppPurchases`, and free / price.
 *
 * Then a per-query summary derived only from the fetched data: how many enriched apps declare ads,
 * how many declare in-app purchases, how many are unknown, the largest install bucket seen with
 * its lower bound, and how many installs were unknown. Nothing is invented and no `null` is
 * silently rendered as `false` or `0`: unknown is printed as `unknown`, because "Play did not
 * declare a badge" and "Play declared no badge" are different facts and only one of them is
 * evidence about monetisation.
 *
 * Politeness
 * ----------
 * Each enriched app is a separate page fetch, so concurrency defaults to 3 (lower than the search
 * scanner's 4) and every request carries a timeout. Requests are retried once on failure; an app
 * that fails both attempts is reported as a failure and drives a non-zero exit code.
 *
 * Exit codes:
 *   0  every query returned results and every selected app detail was fetched and parsed
 *   1  at least one search returned nothing, or at least one app detail failed (which ones is
 *      printed in the FAILURES section)
 *   2  every query failed, or the invocation itself was invalid (bad flag, no --query)
 *
 * Usage:
 *   node scripts/enrich-market.mjs --query "plant disease identifier"
 *   node scripts/enrich-market.mjs --query "migraine tracker" --top 8 --concurrency 2
 *   node scripts/enrich-market.mjs --query "bolt torque calculator" --json
 *   node scripts/enrich-market.mjs --query "mushroom foraging guide" --out docs/foo.md
 *   node scripts/enrich-market.mjs --help
 */

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, isAbsolute, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { PlayAppDetailError, fetchAppDetail } from './lib/play-app-detail.mjs';
import { scanQuery } from './lib/play-scan.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');

/** Default cards enriched per query. Kept modest: each one is a full page fetch. */
const DEFAULT_TOP = 8;

/**
 * Default parallel app fetches. Deliberately lower than the search scan's 4: the search scan
 * fetches one page per query, this fetches one page per app.
 */
const DEFAULT_CONCURRENCY = 3;

/** Play occasionally serves a slow or interstitial page; a hung socket is not a parse failure. */
const REQUEST_TIMEOUT_MS = 30_000;

/** One retry, so a transient bot-check page does not become a hole in the evidence. */
const RETRY_DELAY_MS = 1_500;

const USAGE = `PlayKiln market enrichment — per-app facts behind the ratings-only screen.

Usage:
  node scripts/enrich-market.mjs --query <text> [options]

Options:
  --query <text>      Query to screen and enrich (repeatable). At least one is required.
  --top <n>           Enrich the first n result cards per query (default ${DEFAULT_TOP}).
  --concurrency <n>   Parallel app-detail fetches (default ${DEFAULT_CONCURRENCY}). Each app is a
                      separate page fetch, so this stays low on purpose.
  --json              Print machine-readable JSON to stdout instead of the tables.
  --out <path>        Also write a markdown report (path relative to the repo root).
  --help, -h          Show this help.

Columns:
  reviews   Play's displayed count (e.g. "49K"); the unrounded schema.org twin is shown in
            parentheses when the page server-renders it.
  installs  Play's rounded bucket (e.g. "5M+") plus the lower bound it implies (>= 5000000).
  ads / iap true = Play declares the badge, false = the badge region was present and did not
            declare it, unknown = the badge region was absent, so the fact was not observable.

Exit codes: 0 all fetches succeeded; 1 a query returned nothing or an app failed; 2 all queries
failed or the invocation was invalid.

Examples:
  node scripts/enrich-market.mjs --query "plant disease identifier" --top 8
  node scripts/enrich-market.mjs --query "migraine tracker" --query "bolt torque calculator" --json
`;

function fail(message) {
  process.stderr.write(`enrich-market: ${message}\n\n${USAGE}`);
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

const UNKNOWN = 'unknown';

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
 * Per-query summary, computed only from the enriched records.
 *
 * The ads/IAP counts are split three ways on purpose: `declared` (true), `declaredAbsent` (false)
 * and `unknown` (null). Collapsing the last two would claim a fact the scan never observed.
 */
function summarise(apps) {
  const enriched = apps.filter((app) => app.ok).map((app) => app.detail);

  const tally = (field) => ({
    declared: enriched.filter((detail) => detail[field] === true).length,
    declaredAbsent: enriched.filter((detail) => detail[field] === false).length,
    unknown: enriched.filter((detail) => detail[field] === null).length,
  });

  const known = enriched.filter((detail) => detail.installsMin !== null);
  let largest = null;
  if (known.length > 0) {
    const max = Math.max(...known.map((detail) => detail.installsMin));
    const atMax = known.filter((detail) => detail.installsMin === max);
    largest = {
      installsMin: max,
      labels: [...new Set(atMax.map((detail) => detail.installs))],
      packageIds: atMax.map((detail) => detail.packageId),
    };
  }

  return {
    enriched: enriched.length,
    ads: tally('containsAds'),
    iap: tally('inAppPurchases'),
    largest,
    installsUnknown: enriched.length - known.length,
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
      clip(d.title, 30),
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

function markdownAppRows(apps) {
  return apps.map((app, index) => {
    const position = String(index + 1);
    if (!app.ok) {
      return [position, app.packageId, '**FETCH FAILED**', '-', '-', '-', '-', '-', '-', '-', app.error.code];
    }
    const d = app.detail;
    return [
      position,
      `\`${d.packageId}\``,
      d.title,
      d.developer ?? UNKNOWN,
      d.rating === null ? 'unrated' : d.rating.toFixed(1),
      displayCount(d.ratingCount),
      displayCount(d.ratingCountExact),
      d.installs ?? UNKNOWN,
      d.installsMin === null ? UNKNOWN : displayCount(d.installsMin),
      displayDeclared(d.containsAds),
      displayDeclared(d.inAppPurchases),
      displayFree(d),
    ];
  });
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
  ];
}

function renderReport({ generatedAt, options, queryRecords, failures, exitCode, outPath }) {
  const lines = [];
  lines.push(`# Play Store market enrichment — ${generatedAt.slice(0, 10)}`);
  lines.push('');
  lines.push(
    `Generated by \`node scripts/enrich-market.mjs --out ${outPath}\` at ${generatedAt}, top ${options.top} per query, app-fetch concurrency ${options.concurrency}.`,
  );
  lines.push('');
  lines.push(
    'Search cards come from `scripts/lib/play-scan.mjs`; per-app facts from `scripts/lib/play-app-detail.mjs` (both unmodified). Signed-out Play (`hl=en`, `gl=US`).',
  );
  lines.push('');
  lines.push(
    'Every number below was fetched in this run. `installs` is Play\'s rounded bucket, so the bound column is a lower bound, not a count. `unknown` means the page did not expose the fact — it is never rendered as `false` or `0`.',
  );
  lines.push('');
  lines.push(`Exit code ${exitCode}.`);
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
      `Search returned ${record.resultCount} visible card(s); ${record.apps.length} enriched (top ${options.top}). Ratings-only screen verdict: **${record.verdict}** — ${record.reason}`,
    );
    lines.push('');
    // The verdict stays exactly as the library returns it; this says whether the apps it was
    // computed from answer the query, so a polluted `candidate` is not read as a GO.
    if (record.relevance?.suspect) {
      lines.push(`**Relevance warning (${record.relevance.level}):** ${record.relevance.warning}`);
      lines.push('');
    }
    lines.push(markdownTable(APP_TABLE_HEADER, markdownAppRows(record.apps)));
    lines.push('');
    lines.push('Summary (derived from the fetched apps only):');
    lines.push('');
    for (const line of summaryLines(record.summary)) lines.push(`- ${line}`);
    lines.push('');
  }

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
  lines.push('- Install counts are Play\'s rounded buckets (`5M+`), not exact counts, so "largest install bucket" is a floor comparison and two apps in the same bucket are indistinguishable.');
  lines.push('- Revenue, retention and willingness to pay are not observable on the store page. An app with ads and IAP declared proves a model exists, not that it earns.');
  lines.push('- A weak rating count does not mean a small niche: a strong incumbent can absorb demand while showing a modest count, and Play withholds counts below a display threshold.');

  return `${lines.join('\n')}\n`;
}

function resolveOutputPath(target) {
  return isAbsolute(target) ? target : resolve(ROOT, target);
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    process.stdout.write(USAGE);
    process.exit(0);
  }

  const queries = [...new Set(options.queries.map((query) => query.trim()).filter((query) => query.length > 0))];
  if (queries.length === 0) fail('at least one --query is required');

  const generatedAt = new Date().toISOString();
  // In --json mode stdout stays pure JSON, so human-facing lines go to stderr.
  const human = options.json ? (text) => process.stderr.write(`${text}\n`) : (text) => console.log(text);

  const startedAt = Date.now();
  const failures = [];
  const queryRecords = [];

  // Queries run one at a time; politeness matters more than wall-clock here, and each query's
  // detail fetches are already bounded by --concurrency.
  for (const query of queries) {
    const scan = await scanQuery(query, { fetchImpl: timedFetch });

    if (scan.error) {
      failures.push({ query, packageId: null, stage: 'search', message: `search failed: ${scan.error}` });
      queryRecords.push({ query, searchError: `search failed: ${scan.error}`, apps: [], resultCount: 0 });
      human(`\n## ${query}\nsearch failed: ${scan.error}`);
      continue;
    }

    if (scan.results.length === 0) {
      failures.push({
        query,
        packageId: null,
        stage: 'search',
        message: 'search returned no visible results — that is a failure, not an empty market',
      });
      queryRecords.push({ query, searchError: 'search returned no visible results', apps: [], resultCount: 0 });
      human(`\n## ${query}\nsearch returned no visible results`);
      continue;
    }

    const selected = scan.results.slice(0, options.top);
    const apps = await mapWithConcurrency(selected, options.concurrency, (card) =>
      detailWithRetry(card.packageId, { fetchImpl: timedFetch }),
    );

    for (const app of apps) {
      if (app.ok) continue;
      failures.push({
        query,
        packageId: app.packageId,
        stage: 'detail',
        message: `${app.error.code}: ${app.error.message}${app.attempts > 1 ? ` (failed ${app.attempts} attempts)` : ''}`,
      });
    }

    const summary = summarise(apps);
    queryRecords.push({
      query,
      searchError: null,
      resultCount: scan.results.length,
      verdict: scan.verdict,
      reason: scan.reason,
      relevance: scan.relevance,
      url: scan.url,
      apps,
      summary,
    });

    human(`\n## ${query}`);
    human(
      `search returned ${scan.results.length} card(s); enriching top ${selected.length} (of ${options.top} requested) — verdict ${scan.verdict}`,
    );
    // A verdict computed from apps that do not echo the query is arithmetic over the wrong shelf.
    if (scan.relevance?.suspect) {
      human(`relevance guard: ${scan.relevance.level} — ${scan.relevance.warning}`);
    }
    human('');
    human(renderTextTable(apps));
    human('');
    human('summary:');
    for (const line of summaryLines(summary)) human(`  ${line}`);
  }

  const elapsedMs = Date.now() - startedAt;
  const succeededQueries = queryRecords.filter(
    (record) => !record.searchError && record.apps.some((app) => app.ok),
  );

  let exitCode = 0;
  if (succeededQueries.length === 0) exitCode = 2;
  else if (failures.length > 0) exitCode = 1;

  human('');
  human(
    `enriched ${queryRecords.reduce((total, record) => total + record.apps.length, 0)} app(s) across ${queries.length} query(s) in ${elapsedMs} ms (app concurrency ${options.concurrency}); ${failures.length} failure(s); exit ${exitCode}`,
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
        appsEnriched: queryRecords.reduce((total, record) => total + record.apps.filter((app) => app.ok).length, 0),
        failures: failures.length,
      },
      failures,
      results: queryRecords.map((record) => ({
        query: record.query,
        searchError: record.searchError,
        resultCount: record.resultCount,
        verdict: record.verdict ?? null,
        reason: record.reason ?? null,
        relevance: record.relevance ?? null,
        url: record.url ?? null,
        summary: record.summary ?? null,
        apps: record.apps.map((app) =>
          app.ok
            ? { ok: true, attempts: app.attempts, ...app.detail }
            : { ok: false, attempts: app.attempts, packageId: app.packageId, error: app.error },
        ),
      })),
    };
    process.stdout.write(`${JSON.stringify(payload, null, 2)}\n`);
  }

  if (options.out) {
    const target = resolveOutputPath(options.out);
    const report = renderReport({
      generatedAt,
      options,
      queryRecords,
      failures,
      exitCode,
      outPath: options.out,
    });
    mkdirSync(dirname(target), { recursive: true });
    writeFileSync(target, report, 'utf8');
    human(`report written: ${target}`);
  }

  process.exit(exitCode);
}

main().catch((error) => {
  console.error(`enrich-market: unexpected failure: ${error?.stack ?? error}`);
  process.exit(2);
});

#!/usr/bin/env node
/**
 * scan-market.mjs — run the market screen (scripts/lib/play-scan.mjs) over a query set and
 * report the reachable pockets.
 *
 * Usage:
 *   node scripts/scan-market.mjs
 *   node scripts/scan-market.mjs --json --out docs/market-scan-YYYY-MM-DD.md
 *   node scripts/scan-market.mjs --archetype hobby-tools --query "yoyo trick log"
 *   node scripts/scan-market.mjs --help
 *
 * What it does
 * ------------
 * Reads scripts/market-queries.json (the archetype-spread query set), scans every query through
 * the validated library with bounded concurrency, and prints one table per archetype. With
 * `--out` it also writes a markdown report: the full table, a ranked candidate list, the
 * rejected fortresses and dead zones, and the stated limitations.
 *
 * Exit codes:
 *   0  every query scanned, and at least one query returned visible results
 *   1  at least one query errored or returned nothing while others succeeded (partial evidence)
 *   2  every query errored, every query returned zero visible results, or the invocation itself
 *      was invalid (bad flag, unknown archetype, unreadable query set)
 *
 * A ratings-only screen is not a revenue forecast — see the limitations section of the report
 * and docs/niche-selection.md.
 */

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, isAbsolute, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  DEMAND_PROVEN_SHARE,
  FORTRESS_SHARE,
  INADEQUATE_SHARE,
  NO_POCKET_SHARE,
  STRONG_RATING,
  WEAK_RATING,
  formatTable,
  scanQueries,
} from './lib/play-scan.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const DEFAULT_QUERY_SET = join(ROOT, 'scripts', 'market-queries.json');
const DEFAULT_CONCURRENCY = 4;

const USAGE = `PlayKiln market scan — screen Play search queries for reachable pockets.

Usage:
  node scripts/scan-market.mjs [options]

Options:
  --archetype <id>    Scan only this archetype (repeatable). See --help query set for ids.
  --query <text>      Add an ad-hoc query (repeatable). Ad-hoc queries are scanned in addition
                      to the archetype set and reported under the "ad-hoc" group.
  --json              Print machine-readable JSON to stdout instead of the tables.
  --concurrency <n>   Parallel fetches (default ${DEFAULT_CONCURRENCY}). Bounded on purpose.
  --out <path>        Also write the markdown report (path is relative to the repo root).
  --help, -h          Show this help.

Verdicts (definitions applied to ratings, not revenue forecasts):
  candidate   demand proven and >= ${INADEQUATE_SHARE} of rated incumbents are below ${WEAK_RATING}
  fortress    >= ${FORTRESS_SHARE} of rated incumbents are >= ${STRONG_RATING} with no weak pocket
  dead-zone   fewer than ${DEMAND_PROVEN_SHARE} of visible incumbents are rated at all
  mixed       demand proven, a weak pocket exists, but strong incumbents dominate
  no-results  the query returned nothing visible

Examples:
  node scripts/scan-market.mjs
  node scripts/scan-market.mjs --archetype hobby-tools --concurrency 2
  node scripts/scan-market.mjs --json --out docs/market-scan-2026-09-23.md
`;

function fail(message) {
  console.error(`scan-market: ${message}`);
  process.exit(2);
}

function parseArgs(argv) {
  const options = {
    archetypes: [],
    queries: [],
    json: false,
    concurrency: DEFAULT_CONCURRENCY,
    out: null,
    help: false,
  };

  const value = (flag, index) => {
    const inline = argv[index].includes('=') ? argv[index].slice(argv[index].indexOf('=') + 1) : null;
    if (inline !== null) return inline;
    const next = argv[index + 1];
    if (next === undefined) fail(`${flag} needs a value`);
    return next;
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--help' || arg === '-h') options.help = true;
    else if (arg === '--json') options.json = true;
    else if (arg === '--archetype' || arg.startsWith('--archetype=')) {
      options.archetypes.push(value('--archetype', index));
      if (!arg.includes('=')) index += 1;
    } else if (arg === '--query' || arg.startsWith('--query=')) {
      options.queries.push(value('--query', index));
      if (!arg.includes('=')) index += 1;
    } else if (arg === '--out' || arg.startsWith('--out=')) {
      options.out = value('--out', index);
      if (!arg.includes('=')) index += 1;
    } else if (arg === '--concurrency' || arg.startsWith('--concurrency=')) {
      const raw = value('--concurrency', index);
      if (!arg.includes('=')) index += 1;
      const parsed = Number(raw);
      if (!Number.isInteger(parsed) || parsed < 1) fail(`--concurrency must be a positive integer (got "${raw}")`);
      options.concurrency = parsed;
    } else {
      fail(`unknown argument "${arg}"\n\n${USAGE}`);
    }
  }

  return options;
}

function readQuerySet(path) {
  if (!existsSync(path)) fail(`query set not found: ${path}`);
  try {
    const parsed = JSON.parse(readFileSync(path, 'utf8').replace(/^\uFEFF/, ''));
    if (!Array.isArray(parsed.archetypes) || parsed.archetypes.length === 0) {
      fail(`${path} has no "archetypes" array`);
    }
    return parsed;
  } catch (error) {
    fail(`could not read ${path}: ${error.message}`);
    return null;
  }
}

function resolveOutputPath(target) {
  return isAbsolute(target) ? target : resolve(ROOT, target);
}

function escapeCell(text) {
  return String(text).replace(/\|/g, '\\|').replace(/\r?\n/g, ' ');
}

function markdownTable(header, rows) {
  const lines = [
    `| ${header.join(' | ')} |`,
    `| ${header.map(() => '---').join(' | ')} |`,
  ];
  for (const row of rows) lines.push(`| ${row.map(escapeCell).join(' | ')} |`);
  return lines.join('\n');
}

/**
 * Ranking key for the candidate list.
 *
 * Candidates sort above mixed because the verdict is the primary key; inside a verdict the score
 * orders by how much pocket is actually reachable: more weak incumbents and fewer strong ones is
 * the opening, and a higher rated share means the demand is better proven. It ranks a fixed
 * definition over the same signals the verdicts use — it is NOT a revenue estimate, and it cannot
 * see install counts or reputation.
 */
function opportunityScore(scan) {
  const a = scan.analysis;
  const score = a.weakShare * 100 - a.strongShare * 60 + a.ratedShare * 30 + Math.min(a.weakCount, 12) * 2;
  return Number(score.toFixed(1));
}

const VERDICT_RANK = { candidate: 2, mixed: 1, 'no-results': 0, 'dead-zone': 0, fortress: 0 };

function rankCandidates(scans) {
  return scans
    .filter((scan) => !scan.error && (scan.verdict === 'candidate' || scan.verdict === 'mixed'))
    .map((scan) => ({ ...scan, opportunityScore: opportunityScore(scan) }))
    .sort(
      (a, b) =>
        VERDICT_RANK[b.verdict] - VERDICT_RANK[a.verdict] ||
        b.opportunityScore - a.opportunityScore ||
        b.analysis.weakShare - a.analysis.weakShare ||
        a.query.localeCompare(b.query),
    );
}

function worstLabel(scan) {
  const worst = scan.worst;
  if (!worst) return 'none — no rated incumbent in the visible set';
  const developer = worst.developer ? `, ${worst.developer}` : '';
  return `\`${worst.packageId}\` — ${worst.rating}★ (${worst.title}${developer})`;
}

function signalsLine(scan) {
  const a = scan.analysis;
  return `${a.ratedCount}/${a.total} rated, ${a.weakCount} weak (< ${WEAK_RATING}), ${a.strongCount} strong (>= ${STRONG_RATING}), weak share ${a.weakShare}, strong share ${a.strongShare}, median ${a.medianRating === null ? 'n/a' : a.medianRating}`;
}

function renderReport({ groups, groupScans, allScans, generatedAt, concurrency, exitCode, outPath }) {
  const reportDate = generatedAt.slice(0, 10);
  const ranked = rankCandidates(allScans);
  const rejected = allScans.filter((scan) => !scan.error && (scan.verdict === 'fortress' || scan.verdict === 'dead-zone'));
  const noResults = allScans.filter((scan) => !scan.error && scan.verdict === 'no-results');
  const errored = allScans.filter((scan) => scan.error);
  const archetypeOf = new Map();
  for (const group of groups) for (const query of group.queries) archetypeOf.set(query, group.id);

  const lines = [];

  lines.push(`# Play Store market scan — ${reportDate}`);
  lines.push('');
  lines.push(
    `Generated by \`node scripts/scan-market.mjs --json --out ${outPath}\` at ${generatedAt}.`,
  );
  lines.push('');
  lines.push(
    `Signed-out Google Play search (\`?c=apps\`, \`hl=en\`, \`gl=US\`), ${allScans.length} queries, bounded concurrency ${concurrency}. Query set: \`scripts/market-queries.json\`. Screening library: \`scripts/lib/play-scan.mjs\` (unmodified).`,
  );
  lines.push('');
  lines.push(
    `Nothing below is hand-written: every number is the library's output for this run. Exit code ${exitCode}.`,
  );
  lines.push('');

  lines.push('## What the verdicts mean');
  lines.push('');
  lines.push(
    `The screen assumes a rating is proof the category supports an app at all (Play withholds ratings until enough installs exist), so unrated incumbents are read as failed attempts rather than vacancy.`,
  );
  lines.push('');
  lines.push('| Verdict | Definition |');
  lines.push('| --- | --- |');
  lines.push(
    `| \`candidate\` | Demand proven, and >= ${INADEQUATE_SHARE} of rated incumbents sit below ${WEAK_RATING} — demand proven, supply inadequate. |`,
  );
  lines.push(
    `| \`fortress\` | >= ${FORTRESS_SHARE} of rated incumbents are >= ${STRONG_RATING} and fewer than ${NO_POCKET_SHARE} are weak — entrenched. |`,
  );
  lines.push(
    `| \`dead-zone\` | Fewer than ${DEMAND_PROVEN_SHARE} of visible incumbents carry a rating at all — demand is not proven. |`,
  );
  lines.push(
    `| \`mixed\` | Demand proven and a weak pocket exists, but strong incumbents dominate; entry needs a specific angle. |`,
  );
  lines.push(`| \`no-results\` | The query returned nothing visible. |`);
  lines.push('');

  lines.push('## Queries scanned');
  lines.push('');
  for (const group of groups) {
    lines.push(`- **${group.id}** — ${group.label}. ${group.why ?? ''} (${group.queries.length} queries)`);
  }
  lines.push('');

  lines.push('## Full result table');
  lines.push('');
  for (const group of groups) {
    lines.push(`### ${group.id} — ${group.label}`);
    lines.push('');
    const scans = groupScans.get(group.id) ?? [];
    lines.push(
      markdownTable(
        ['query', 'verdict', 'seen', 'rated/total', 'weak', 'median', 'why'],
        scans.map((scan) => {
          if (scan.error) return [scan.query, 'ERROR', '-', '-', '-', '-', scan.error];
          const a = scan.analysis;
          return [
            scan.query,
            scan.verdict,
            String(a.total),
            `${a.ratedCount}/${a.total}`,
            String(a.weakCount),
            a.medianRating === null ? '-' : a.medianRating.toFixed(2),
            scan.reason,
          ];
        }),
      ),
    );
    lines.push('');
  }

  lines.push('## Ranked candidates');
  lines.push('');
  if (ranked.length === 0) {
    lines.push('No query produced a `candidate` or `mixed` verdict in this run.');
  } else {
    lines.push(
      `Only \`candidate\` and \`mixed\` results appear here. Ordering key: verdict (\`candidate\` above \`mixed\`), then \`opportunityScore = weakShare x 100 - strongShare x 60 + ratedShare x 30 + min(weakCount, 12) x 2\`. The score ranks the same signals the verdict uses; it is not a revenue estimate.`,
    );
    lines.push('');
    ranked.forEach((scan, index) => {
      lines.push(`### ${index + 1}. \`${scan.query}\` — ${scan.verdict} (score ${scan.opportunityScore})`);
      lines.push('');
      lines.push(`- Archetype: \`${archetypeOf.get(scan.query) ?? 'ad-hoc'}\``);
      lines.push(`- Why it passed: ${scan.reason}`);
      lines.push(`- Worst incumbent: ${worstLabel(scan)}`);
      lines.push(`- Signals: ${signalsLine(scan)}`);
      if (scan.relevance?.suspect) {
        lines.push(
          `- Relevance warning: ${scan.relevance.warning}. The verdict above is reported unmodified; this says what it was computed from.`,
        );
      }
      if (scan.organisations.length > 0) {
        lines.push(
          `- Organisation-like publishers surfaced: ${scan.organisations.join('; ')} — this is a human-decision hint, not a verdict; a ratings-only screen cannot see reputation.`,
        );
      }
      lines.push('');
    });
  }

  lines.push('## Rejected: fortresses and dead zones');
  lines.push('');
  if (rejected.length === 0) {
    lines.push('No query produced a `fortress` or `dead-zone` verdict in this run.');
  } else {
    for (const scan of rejected) {
      const orgs = scan.organisations.length
        ? ` — organisation-like publishers: ${scan.organisations.join('; ')} (human-decision hint, not a verdict)`
        : '';
      lines.push(
        `- \`${scan.query}\` (${archetypeOf.get(scan.query) ?? 'ad-hoc'}) — **${scan.verdict}** — ${scan.reason}${orgs}`,
      );
    }
  }
  lines.push('');

  if (noResults.length > 0) {
    lines.push('## Queries with no visible results');
    lines.push('');
    for (const scan of noResults) {
      lines.push(`- \`${scan.query}\` (${archetypeOf.get(scan.query) ?? 'ad-hoc'}) — ${scan.reason}`);
    }
    lines.push('');
  }

  lines.push('## Errors');
  lines.push('');
  if (errored.length === 0) {
    lines.push('No query errored in this run: every fetch returned a parseable page.');
  } else {
    for (const scan of errored) {
      lines.push(`- \`${scan.query}\` (${archetypeOf.get(scan.query) ?? 'ad-hoc'}) — **ERROR** — ${scan.error}`);
    }
  }
  lines.push('');

  lines.push('## Limitations — read before acting on this');
  lines.push('');
  lines.push(
    '- **Ratings only.** The scan reads the rating a card shows. It does not read install counts, review counts, or review sentiment, so "demand proven" means "enough installs exist to compute a rating", not "this makes money".',
  );
  lines.push(
    '- **The result set is display-capped (~30 cards).** That is a page limit, not a competitor count, and not a market size. A query with 30 visible results does not have 30 competitors.',
  );
  lines.push(
    '- **One storefront, one session.** Signed-out `hl=en`, `gl=US` only. Play search is personalised and geo-dependent, so this is a spot check of a single storefront, not a market study.',
  );
  lines.push(
    '- **Verdicts are definitions applied to ratings, not revenue forecasts.** `candidate` means demand is proven and the visible supply is inadequate by the stated thresholds; it does not predict installs, revenue, or conversion.',
  );
  lines.push(
    '- **Reputation is invisible.** The screen cannot tell that an incumbent is a Red Cross or a government body, which is why organisation-like publisher names are surfaced as a hint for a human instead of being scored.',
  );
  lines.push(
    '- **A rating is a lagging signal.** A weak incumbent may be weak because the market will not pay, and a fortress may have an unserved sub-niche that this query text does not reach.',
  );
  const thin = allScans.filter((scan) => !scan.error && (scan.analysis?.total ?? 0) > 0 && scan.analysis.total < 15);
  lines.push(
    thin.length === 0
      ? '- **Equal sample sizes.** Every query returned at least 15 visible cards, so the shares are comparable across queries.'
      : `- **Thin result sets.** These queries returned fewer than 15 visible cards, so their shares rest on a small sample and their verdict is weaker evidence than one computed over ~30: ${thin
          .map((scan) => `\`${scan.query}\` (${scan.analysis.total})`)
          .join(', ')}.`,
  );
  const polluted = allScans.filter((scan) => !scan.error && scan.relevance?.suspect);
  lines.push(
    polluted.length === 0
      ? '- **Every result set echoes its query.** Each query\'s visible titles carry at least one of its distinctive tokens, so no verdict here is computed from a shelf the query never described.'
      : `- **Token-polluted queries.** Play search is a fuzzy text match, so these queries returned apps that do not answer them — and a verdict is arithmetic over the apps it was given, so these labels describe the wrong shelf: ${polluted
          .map((scan) => `\`${scan.query}\` (${scan.relevance.level}, ${scan.relevance.onTopicCount}/${scan.relevance.total} titles echo the query)`)
          .join(', ')}.`,
  );
  lines.push(
    `- **Ceiling on inference.** ${allScans.length} queries screened; ${ranked.length} reported a pocket. The manual baseline in \`docs/niche-selection.md\` is roughly one candidate in four, so a hit rate far above that is a reason to widen the query set, not to trust the label.`,
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

  const querySetPath = DEFAULT_QUERY_SET;
  const querySet = readQuerySet(querySetPath);
  const archetypes = querySet.archetypes;

  const knownIds = archetypes.map((a) => a.id);
  const unknown = options.archetypes.filter((id) => !knownIds.includes(id));
  if (unknown.length > 0) {
    fail(`unknown archetype(s): ${unknown.join(', ')}. Known: ${knownIds.join(', ')}`);
  }

  const selected = options.archetypes.length
    ? archetypes.filter((a) => options.archetypes.includes(a.id))
    : archetypes;

  const groups = selected.map((a) => ({
    id: a.id,
    label: a.label,
    why: a.why,
    queries: [...new Set(a.queries.map((q) => String(q)))],
  }));

  if (options.queries.length > 0) {
    const existing = new Set(groups.flatMap((g) => g.queries));
    const adHoc = [...new Set(options.queries.map((q) => q.trim()))].filter((q) => q.length > 0 && !existing.has(q));
    if (adHoc.length > 0) {
      groups.push({
        id: 'ad-hoc',
        label: 'Ad-hoc queries',
        why: 'Queries supplied with --query on this invocation.',
        queries: adHoc,
      });
    }
  }

  const allQueries = [...new Set(groups.flatMap((g) => g.queries))];
  if (allQueries.length === 0) fail('nothing to scan: the selected archetypes and --query list are empty');

  const startedAt = Date.now();
  const scans = await scanQueries(allQueries, { concurrency: options.concurrency });
  const elapsedMs = Date.now() - startedAt;
  const byQuery = new Map(scans.map((scan) => [scan.query, scan]));
  const groupScans = new Map(groups.map((g) => [g.id, g.queries.map((q) => byQuery.get(q)).filter(Boolean)]));

  const errored = scans.filter((scan) => scan.error);
  const empty = scans.filter((scan) => !scan.error && (scan.analysis?.total ?? 0) === 0);

  let exitCode = 0;
  if (errored.length === scans.length) exitCode = 2;
  else if (errored.length > 0) exitCode = 1;
  else if (empty.length === scans.length) exitCode = 2;

  const generatedAt = new Date().toISOString();
  // In --json mode stdout must stay pure JSON, so human-facing lines go to stderr.
  const human = options.json ? (text) => process.stderr.write(`${text}\n`) : (text) => console.log(text);

  if (options.json) {
    const payload = {
      generatedAt,
      storefront: { hl: 'en', gl: 'US', surface: 'signed-out /store/search?c=apps' },
      querySet: 'scripts/market-queries.json',
      concurrency: options.concurrency,
      elapsedMs,
      exitCode,
      totals: {
        queries: scans.length,
        errored: errored.length,
        zeroVisibleResults: empty.length,
        candidates: scans.filter((s) => s.verdict === 'candidate').length,
        mixed: scans.filter((s) => s.verdict === 'mixed').length,
        fortresses: scans.filter((s) => s.verdict === 'fortress').length,
        deadZones: scans.filter((s) => s.verdict === 'dead-zone').length,
      },
      archetypes: groups.map((g) => ({ id: g.id, label: g.label, queries: g.queries })),
      results: scans.map((scan) => {
        const archetype = groups.find((g) => g.queries.includes(scan.query))?.id ?? null;
        if (scan.error) {
          return { query: scan.query, archetype, error: scan.error, verdict: null, analysis: null, relevance: null, organisations: [], worst: null, incumbents: [] };
        }
        return {
          query: scan.query,
          archetype,
          error: null,
          url: scan.url,
          verdict: scan.verdict,
          reason: scan.reason,
          analysis: scan.analysis,
          relevance: scan.relevance,
          organisations: scan.organisations,
          worst: scan.worst,
          opportunityScore:
            scan.verdict === 'candidate' || scan.verdict === 'mixed' ? opportunityScore(scan) : null,
          incumbents: scan.results.map((r) => ({
            packageId: r.packageId,
            title: r.title,
            developer: r.developer,
            rating: r.rating,
            rated: r.rated,
          })),
        };
      }),
    };
    process.stdout.write(`${JSON.stringify(payload, null, 2)}\n`);
  } else {
    for (const group of groups) {
      human(`\n## ${group.id} — ${group.label}`);
      if (group.why) human(group.why);
      human('');
      human(formatTable(groupScans.get(group.id) ?? []));
      const withOrgs = (groupScans.get(group.id) ?? []).filter((scan) => scan.organisations?.length);
      if (withOrgs.length > 0) {
        human('\nOrganisation-like publishers surfaced (human-decision hint, not a verdict):');
        for (const scan of withOrgs) human(`  ${scan.query}: ${scan.organisations.join('; ')}`);
      }
    }
  }

  human(
    `\nscanned ${scans.length} queries in ${elapsedMs} ms (concurrency ${options.concurrency}): ` +
      `${scans.filter((s) => s.verdict === 'candidate').length} candidate, ` +
      `${scans.filter((s) => s.verdict === 'mixed').length} mixed, ` +
      `${scans.filter((s) => s.verdict === 'fortress').length} fortress, ` +
      `${scans.filter((s) => s.verdict === 'dead-zone').length} dead-zone, ` +
      `${scans.filter((s) => s.verdict === 'no-results').length} no-results, ` +
      `${errored.length} errored`,
  );
  if (errored.length > 0) {
    for (const scan of errored) human(`  ERROR ${scan.query}: ${scan.error}`);
  }

  if (options.out) {
    const target = resolveOutputPath(options.out);
    const report = renderReport({
      groups,
      groupScans,
      allScans: scans,
      generatedAt,
      concurrency: options.concurrency,
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
  console.error(`scan-market: unexpected failure: ${error?.stack ?? error}`);
  process.exit(2);
});

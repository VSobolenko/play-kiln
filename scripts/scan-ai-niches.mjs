/**
 * AI-utility niche screen — the AI-specific companion to `scan-market.mjs`.
 *
 * Why this file exists
 * --------------------
 * `scripts/lib/play-scan.mjs` is the factory's validated screen, but it answers one question:
 * does this niche support an app at all? For an *AI-utility* direction there is a second
 * question the generic CLI cannot answer — whether the visible incumbents are AI-first products
 * (a model doing the work) or traditional tooling that happens to sit next to the query.
 *
 * That second question is judged from the incumbents' titles, and it is a human call: this script
 * therefore prints the rated titles verbatim instead of inventing an "AI-ness" score. The titles
 * are the evidence; the classification is in
 * `docs/research/2026-09-23-ai-utility-store-evidence.md`.
 *
 * Scope and non-goals
 * -------------------
 * - Reuses `scanQueries`/`formatTable` from the validated library. `play-scan.mjs` is NOT modified.
 * - This is not a second market scanner: `scripts/scan-market.mjs` owns the general CLI. This file
 *   is a fixed, AI-utility-shaped query set layered on the same sensor.
 * - Concurrency is explicitly 4, matching the library default; the endpoint is a public page and
 *   a burst of parallel requests is how a scan turns into a block.
 *
 * Run: node scripts/scan-ai-niches.mjs
 * Exit: 0 when every query returned; 1 when any query's fetch failed (an ERROR row is not a
 *       verdict and must never be read as one).
 */
import { scanQueries, formatTable } from './lib/play-scan.mjs';

/**
 * The query set. The first eight are the task's seed list; the rest are the same kind of job —
 * a phone sensor plus a model doing one tedious, bounded task for a non-developer audience.
 * Feature/use-case phrasing is preferred over broad categories, per docs/niche-selection.md §3.1.
 */
const QUERIES = [
  // Seed queries.
  'plant disease identifier',
  'language pronunciation practice',
  'receipt scanner expense',
  'handwriting notes digitize',
  'label reader food additives',
  'bird song identifier',
  'math word problem solver',
  'recipe from ingredients',
  // Additional queries of the same kind.
  'mushroom identifier',
  'voice memo transcribe',
  'business card scanner contacts',
  'skincare ingredient scanner',
  'screenshot text extractor',
  'document scanner summarize',
];

/** How many rated titles to print per query — enough to judge AI-first vs traditional from titles. */
const TITLE_SAMPLE = 8;

const scans = await scanQueries(QUERIES, { concurrency: 4 });

console.log('=== AI-utility screen: verdicts ===\n');
console.log(formatTable(scans));
console.log('');

console.log('=== Per-query detail (numbers + incumbent titles) ===\n');
for (const scan of scans) {
  if (scan.error) {
    console.log(`--- ${scan.query} ---`);
    console.log(`ERROR: ${scan.error} (no verdict; this is a failed fetch, not a market read)\n`);
    continue;
  }

  const a = scan.analysis;
  console.log(`--- ${scan.query} ---`);
  console.log(`verdict   : ${scan.verdict}`);
  console.log(`reason    : ${scan.reason}`);
  console.log(
    `numbers   : seen=${a.total} rated=${a.ratedCount}/${a.total} (${a.ratedShare}) ` +
      `weak=${a.weakCount}/${a.ratedCount} (${a.weakShare}) ` +
      `strong=${a.strongCount}/${a.ratedCount} (${a.strongShare}) ` +
      `median=${a.medianRating ?? '-'} min=${a.minRating ?? '-'}`,
  );
  console.log(
    `worst     : ${scan.worst ? `${scan.worst.packageId} ${scan.worst.rating}★` : 'none (no rated incumbent)'}`,
  );
  console.log(`url       : ${scan.url}`);

  // Titles are the evidence for the AI-first vs traditional-tooling call, so print them verbatim
  // rather than summarising. Rating is shown as a number or `unrated` (never defaulted).
  const sample = scan.results.slice(0, TITLE_SAMPLE);
  console.log(`titles    : (top ${sample.length} of ${a.total}, as shown by Play search)`);
  for (const r of sample) {
    console.log(`  - ${r.rating === null ? 'unrated' : `${r.rating}★`}  ${r.title}  [${r.packageId}]`);
  }
  if (scan.organisations.length > 0) {
    console.log(`orgs      : ${scan.organisations.join('; ')}`);
  }
  console.log('');
}

console.log('=== JSON dump ===\n');
console.log(JSON.stringify(scans, null, 2));

const failures = scans.filter((s) => s.error).length;
console.log('');
console.log(
  failures === 0
    ? `SCAN COMPLETE: ${scans.length}/${scans.length} queries returned`
    : `SCAN INCOMPLETE: ${failures} of ${scans.length} queries failed to fetch`,
);
process.exit(failures === 0 ? 0 : 1);

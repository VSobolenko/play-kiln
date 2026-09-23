/**
 * Verifies the specific defects reported against scripts/lib/play-scan.mjs by the independent
 * audit. The auditor could not execute the module (read-only sandbox), so its findings were
 * derived from a transcription of the functions. A transcription is corroboration, not proof:
 * before changing anything, each claim is reproduced here against the real module and real pages.
 *
 * Run: node scripts/verify-play-scan-defects.mjs
 */
import { scanQueries, scanQuery, parseSearchHtml, formatTable } from './lib/play-scan.mjs';
import { organisationLikeDevelopers } from './lib/play-scan.mjs';

const QUERIES = [
  'morse code trainer', 'whoop strap', 'beekeeping log', 'child allowance tracker',
  'world health organization', 'barcode scanner', 'step counter', 'water reminder',
  'budget planner', 'flashcard app', 'meditation timer', 'tire pressure calculator',
];

console.log('=== fetching %d queries for defect reproduction ===', QUERIES.length);
const scans = await scanQueries(QUERIES, { concurrency: 4 });

let confirmed = 0;

// ---------------------------------------------------------------- D1: markup leaking into title
console.log('\n=== D1: does a card title contain raw markup? ===');
let d1Hits = 0;
for (const scan of scans) {
  if (scan.error) continue;
  for (const r of scan.results) {
    // Anything resembling an attribute fragment means the segment split landed inside a tag.
    if (/[<>]|href|class=|aria-label=/.test(r.title) || r.title.includes('/store/apps/')) {
      d1Hits += 1;
      console.log(`  HIT  [${scan.query}] pkg=${r.packageId}`);
      console.log(`       title=${JSON.stringify(r.title)}`);
    }
  }
}
if (d1Hits > 0) {
  confirmed += 1;
  console.log(`  CONFIRMED: ${d1Hits} card(s) have markup in the title field`);
} else {
  console.log('  not reproduced in this sample (the trigger may need a specific card width)');
}

// ------------------------------------------- D3/D4: reason string contradicts the measured data
console.log('\n=== D3/D4: does a fortress reason contradict its own weakCount? ===');
let d3Hits = 0;
for (const scan of scans) {
  if (scan.error || scan.verdict !== 'fortress') continue;
  const a = scan.analysis;
  const claimsNoPocket = /no weak pocket/.test(scan.reason);
  if (claimsNoPocket && a.weakCount > 0) {
    d3Hits += 1;
    console.log(`  CONTRADICTION [${scan.query}] weakCount=${a.weakCount} weakShare=${a.weakShare} reason="${scan.reason}"`);
  } else {
    console.log(`  ok            [${scan.query}] weakCount=${a.weakCount}`);
  }
}
if (d3Hits > 0) {
  confirmed += 1;
  console.log(`  CONFIRMED: ${d3Hits} fortress verdict(s) claim "no weak pocket" while weakCount > 0`);
} else {
  console.log('  not reproduced in this sample');
}

// ------------------------------------------------- D2: the organisation token heuristic
console.log('\n=== D2: organisation-like developer heuristic ===');
for (const scan of scans) {
  if (scan.error) continue;
  const orgs = organisationLikeDevelopers(scan.results);
  if (orgs.length > 0) {
    console.log(`  [${scan.query}] flagged: ${orgs.join(' | ')}`);
  }
}
// The known false-negative: an organisation whose name has 'organization', not a listed token.
const whoScan = scans.find((s) => s.query === 'world health organization');
if (whoScan && !whoScan.error) {
  const names = [...new Set(whoScan.results.map((r) => r.developer))].filter(Boolean);
  console.log(`\n  developers seen for "world health organization":`);
  for (const n of names) console.log(`    - ${n}`);
  const flagged = organisationLikeDevelopers(whoScan.results);
  console.log(`  flagged by heuristic: ${flagged.length === 0 ? '(none)' : flagged.join(' | ')}`);
}

console.log('\n=== summary table ===');
console.log(formatTable(scans));
console.log('');
console.log(`confirmed defect classes reproduced: ${confirmed}`);

/**
 * PlayKiln title scan — print the visible result set so a human can classify it by JOB.
 *
 * Why this exists
 * ---------------
 * `scripts/lib/play-scan.mjs` attaches a `relevance` annotation that measures how much of a result
 * set *lexically* echoes its query: a title counts as on-topic when it contains a whole-token match
 * for a distinctive query token. That is not the same as answering the query's *job*, and the gap
 * has now been measured three times:
 *
 *   1. `docs/research/2026-09-23-backup-niche-candidates.md` §2 — the guard returned its most-trusted
 *      label `ok` on a 30% job-share shelf (`fence post spacing calculator`, via the generic shelf
 *      token `calculator`) and a 45% one (`seating chart maker`, via `maker`).
 *   2. `docs/research/2026-09-23-visual-schedule-evidence.md` — the same under-counting for
 *      `visual`/`schedule`; the conclusion there was that the guard "is not sufficient here".
 *   3. `docs/research/2026-09-24-market-decision.md` §2.1 — a **homonym** rather than a generic
 *      token: `gas pipe sizing` scored `ok` over a shelf of gas-utility *billing* apps (Gas South,
 *      Georgia Natural Gas, Southwest Gas), because `gas` really is in those titles.
 *
 * The response adopted after the first two was to read and classify every visible card by hand, and
 * that discipline is what this CLI automates the boring half of: it fetches the query and prints
 * every visible title with its package id, developer and rating, so the classification is a read
 * rather than a re-implementation. `--on-job` / `--off-job` take regexes and bucket the cards for
 * you, but the regexes are *yours* — this tool never invents the job definition, because that
 * judgement is the whole point.
 *
 * Usage
 * -----
 *   node scripts/scan-titles.mjs --query "gas pipe sizing"
 *   node scripts/scan-titles.mjs --query "nfpa 54 gas pipe sizing" --query "black iron pipe sizing" \
 *     --on-job "\b(pipe|piping|plumb|sizing)\b" \
 *     --off-job "\b(gasbuddy|gas south|southwest gas|utility|bill|fuel|price)\b"
 *
 * Where the guard and the hand read disagree, the hand read decides; report both, because the
 * disagreement is itself the finding (see the third case above).
 *
 * What this cannot tell you
 * -------------------------
 * - **~30 visible cards is a display cap**, not a competitor count. Relevance order is not size
 *   order, so a larger app can sit below the cut.
 * - **A title is not a capability.** A card proves what an app *advertises* in its name, never what
 *   it does; absence of a claim is not absence of a feature.
 * - **No install data.** Search cards do not carry it; a browser read is still a spot check of one
 *   storefront in one signed-out session.
 */
import { parseArgs } from 'node:util';
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { scanQueries } from './lib/play-scan.mjs';

const { values } = parseArgs({
  options: {
    query: { type: 'string', multiple: true, default: [] },
    'on-job': { type: 'string' },
    'off-job': { type: 'string' },
    concurrency: { type: 'string', default: '3' },
    out: { type: 'string' },
    help: { type: 'boolean', short: 'h', default: false },
  },
  allowPositionals: false,
});

const HELP = `PlayKiln title scan — print the visible result set so a human can classify it by job.

Usage:
  node scripts/scan-titles.mjs --query "<play search query>" [--query ...] [options]

Options:
  --query <text>      Print the visible result set for this query (repeatable, at least one).
  --on-job <regex>    Case-insensitive regex. Cards matching are bucketed "on the job".
  --off-job <regex>   Case-insensitive regex. Checked FIRST, so a card naming both an off-job
                      signal and an on-job word lands in "not the job" — which is the order that
                      catches a homonym ("Gas South" names a utility, not a pipe).
  --concurrency <n>   Parallel fetches (default 3). Bounded on purpose.
  --out <path>        Also write a markdown report (path relative to the repo root).
  --help, -h          Show this help.

Without --on-job/--off-job every visible card is printed in one bucket, which is the right default
when you do not yet know the job definition you are looking for.

Why the guard is not enough (read this before trusting a verdict):
  scripts/lib/play-scan.mjs scores lexical overlap with the query, not job relevance. Measured
  three times: 'ok' on a 30% job-share shelf ('fence post spacing calculator'), 'ok' on a 45% one
  ('seating chart maker'), and 'ok' over a shelf of gas-utility billing apps for 'gas pipe sizing'.
  The hand read decides; where it disagrees with the guard, report both.

Limitations:
  - ~30 cards is a display cap, not a competitor count, and relevance order is not size order.
  - A title is what an app advertises, never what it does.
  - Search cards carry no install data. One storefront, one signed-out session.`;

if (values.help) {
  console.log(HELP);
  process.exit(0);
}

if (values.query.length === 0) {
  console.error('FAIL  at least one --query is required\n');
  console.log(HELP);
  process.exit(1);
}

/** Compiles a user regex into a test function, and fails loudly rather than silently ignoring it. */
function compile(label, pattern) {
  if (pattern === undefined) return null;
  try {
    return new RegExp(pattern, 'i');
  } catch (error) {
    console.error(`FAIL  --${label} is not a valid regex: ${error.message}`);
    process.exit(1);
  }
}

const onJob = compile('on-job', values['on-job']);
const offJob = compile('off-job', values['off-job']);
const concurrency = Number.parseInt(values.concurrency, 10);

if (!Number.isInteger(concurrency) || concurrency < 1) {
  console.error(`FAIL  --concurrency must be a positive integer, got "${values.concurrency}"`);
  process.exit(1);
}

const lines = [];
const say = (line = '') => {
  console.log(line);
  lines.push(line);
};

const cards = (list) => {
  if (list.length === 0) {
    say('    (none)');
    return;
  }
  for (const card of list) {
    const rating = card.rating === null ? 'unrated' : `${card.rating}`;
    const developer = card.developer ? `  ${card.developer}` : '';
    say(`    ${card.title}  [${card.packageId}]  ${rating}${developer}`);
  }
};

const scans = await scanQueries(values.query, { concurrency });

let disagreementCount = 0;

for (const scan of scans) {
  if (scan.error) {
    say(`\n### ${scan.query} — ERROR ${scan.error}`);
    continue;
  }

  const relevance = scan.relevance;
  const share = relevance?.share === null || relevance?.share === undefined ? 'n/a' : relevance.share.toFixed(2);
  say(`\n### ${scan.query}`);
  say(
    `verdict=${scan.verdict}  guard=${relevance?.level ?? 'n/a'} (${relevance?.onTopicCount ?? 0}/${relevance?.total ?? 0}, share ${share})  visible=${scan.results.length}`
  );

  if (!onJob && !offJob) {
    say(`  -- ALL VISIBLE (${scan.results.length}) --`)
    cards(scan.results);
    continue;
  }

  const on = [];
  const off = [];
  const unmatched = [];
  for (const result of scan.results) {
    // off-job is tested first on purpose: a homonym names the off-job thing using the query's own
    // word, so letting an on-job word win would reproduce the very error this tool exists to catch.
    if (offJob?.test(result.title)) off.push(result);
    else if (onJob?.test(result.title)) on.push(result);
    else unmatched.push(result);
  }

  say(`  -- ON THE JOB (${on.length}) --`);
  cards(on);
  say(`  -- NOT THE JOB (${off.length}) --`);
  cards(off);
  say(`  -- UNMATCHED, classify by eye (${unmatched.length}) --`);
  cards(unmatched);

  // The comparison worth reporting: the guard's lexical share against the hand bucket. A guard
  // `ok` over a low job share is the failure mode, and it is only visible side by side.
  const handShare = scan.results.length === 0 ? null : on.length / scan.results.length;
  if (handShare !== null) {
    const guardOk = relevance?.level === 'on-topic';
    const flag = guardOk && handShare < 0.65 ? '  <-- GUARD SAID on-topic, JOB SHARE IS LOW' : '';
    if (flag) disagreementCount += 1;
    say(
      `  hand job-share: ${on.length}/${scan.results.length} (${handShare.toFixed(2)}) vs guard ${relevance?.level ?? 'n/a'}${flag}`
    );
  }
}

say('');
say(
  `scanned ${scans.length} queries: ${scans.filter((s) => !s.error).length} ok, ${scans.filter((s) => s.error).length} errored` +
    (disagreementCount > 0 ? `; ${disagreementCount} query/queries where the guard said on-topic but the hand job-share is below 0.65` : '')
);

if (values.out) {
  writeFileSync(join(process.cwd(), values.out), `${lines.join('\n')}\n`, 'utf8');
  console.log(`\nmarkdown report written to ${values.out}`);
}

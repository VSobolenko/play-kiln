#!/usr/bin/env node
/**
 * Freshness watchdog for the measured-facts ledger.
 *
 *   node scripts/check-freshness.mjs
 *   node scripts/check-freshness.mjs --json
 *   node scripts/check-freshness.mjs --warn-only
 *   node scripts/check-freshness.mjs --ledger <path>        (tests and dry runs)
 *
 * Why this exists: this factory's rules are full of facts measured at a moment in time - Play's
 * field limits, the console's inline sentences, a vendor's guidance, a pinned toolchain version.
 * They live as prose in `AGENTS.md`, `docs/research/` and `scripts/lib/console-form.json`, with no
 * record of when they were true and no mechanism that ever asks whether they still are. A limit
 * Google changes therefore cannot be noticed from inside the repository, and the next slot silently
 * ships against a stale rule. `docs/research/measured-facts.json` is the record: one entry per fact,
 * with the source to re-open, the date it was last confirmed, the window it may be trusted for, the
 * repository paths that depend on it, and `enforcedBy`, the artifact that turns the fact into a
 * machine check.
 *
 * The contract, the loader and the per-fact audit all live in `scripts/lib/freshness.mjs`; this file
 * is the CLI and the exit-code policy. It asks three questions per fact: is it fresh, is the script
 * it names still on disk, and does the probe it names still appear in that script's source.
 *
 * Exit codes follow `scripts/verify-slot.mjs` exactly, because `2` already means UNVERIFIED in this
 * repository and a fact measured once is unverified now:
 *
 *   0  every entry is inside its window and every entry names an enforcing program
 *   2  at least one entry is stale, or at least one entry names no enforcing program. Both are
 *      "not a pass, not a code defect": the first needs a human to re-measure it against its source,
 *      the second needs somebody to write the check the rule is still only prose about. A stale
 *      ledger and a prose-only ledger can never be read as green.
 *   1  the ledger (or the enforcement it names) is broken: a missing, unparseable or contract-failing
 *      record, a fact whose `enforcedBy.script` is not on disk, or a fact whose `probe` is not a
 *      literal in the named artifact. An unreadable record cannot testify to anything.
 *
 * The stale block prints the claim, the source and the `whereUsed` paths, so the output is the work
 * order rather than a verdict: "re-open this page, then update these files". The unenforced block
 * prints the fact and its `gap`, so the missing check is a work order too.
 *
 * An empty ledger is broken (exit 1) on purpose. "Every entry is fresh" is vacuously true for zero
 * entries, so deleting the facts would otherwise be the cheapest way to pass - the same failure mode
 * `rm store/studio.json store/provenance.json` would be for the store gate.
 */
import process from 'node:process'
import { auditLedger, DEFAULT_LEDGER } from './lib/freshness.mjs'

const ARGV = process.argv.slice(2)
const ledgerIndex = ARGV.indexOf('--ledger')
const LEDGER = ledgerIndex !== -1 ? ARGV[ledgerIndex + 1] : DEFAULT_LEDGER
const AS_JSON = ARGV.includes('--json')
const WARN_ONLY = ARGV.includes('--warn-only')

if (ARGV.includes('--help') || ARGV.includes('-h')) {
  console.log(`Usage: node scripts/check-freshness.mjs [options]

  --ledger <path>   read another ledger (default: ${DEFAULT_LEDGER})
  --json            print machine-readable output
  --warn-only       a stale or unenforced fact prints but does not fail the run (for a loop that
                    must not block). It does not excuse a broken record.
  --help            print this message

Exit codes: 0 fresh and every fact enforced, 2 stale or unenforced (a human must re-measure or write
the check), 1 the ledger is broken - an unreadable record, a missing enforcing script, or a probe
that is no longer in the script it names.`)
  process.exit(0)
}

const audit = auditLedger(LEDGER)
const { rows, stale, unenforced, partial, problems, enforcementProblems } = audit
const recordsBroken = problems.length > 0 || enforcementProblems.length > 0
// One source of truth for the code (the library), and one local policy on top of it: `--warn-only`
// relaxes the two "unverified" states so a loop that must not block can keep moving, and never
// relaxes a broken record.
const exitCode = audit.exitCode === 2 && WARN_ONLY ? 0 : audit.exitCode

if (AS_JSON) {
  console.log(
    JSON.stringify(
      {
        ledger: LEDGER,
        checkedAt: new Date().toISOString(),
        today: audit.today,
        entryCount: rows.length,
        staleCount: stale.length,
        unenforcedCount: unenforced.length,
        gapIds: unenforced.map((row) => row.id),
        warnOnly: WARN_ONLY,
        verdict: recordsBroken ? 'BROKEN' : audit.verdict,
        exitCode,
        problems,
        enforcementProblems,
        entries: rows
      },
      null,
      2
    )
  )
} else if (recordsBroken) {
  console.log(`=== measured-facts freshness — ledger ${LEDGER} ===`)
  if (problems.length > 0) {
    for (const problem of problems) console.log(`  FAIL        ${problem}`)
  }
  for (const problem of enforcementProblems) console.log(`  FAIL        ${problem}`)
  console.log('\n---')
  console.log(
    `VERDICT: BROKEN — ${problems.length + enforcementProblems.length} problem(s); ` +
      'the record must be readable, and the program it names must exist, before any fact can be trusted.'
  )
} else {
  console.log(`=== measured-facts freshness — ${rows.length} entries in ${LEDGER} (today ${audit.today}) ===`)
  const idWidth = Math.max(...rows.map((row) => row.id.length), 2)
  const programWidth = Math.max(
    ...rows.map((row) => (row.enforcement.script ? row.enforcement.script.length : 'NONE'.length)),
    7
  )
  console.log(`  ${'id'.padEnd(idWidth)}  verified    age  window  ${'enforced'.padEnd(programWidth)}  verdict`)
  for (const row of rows) {
    const program = row.enforcement.script || 'NONE'
    console.log(
      `  ${row.id.padEnd(idWidth)}  ${row.verifiedAt}  ${String(row.ageDays).padStart(3)}  ` +
        `${String(row.expiresAfterDays).padStart(6)}  ${program.padEnd(programWidth)}  ` +
        `${row.verdict}${row.enforcement.state === 'unenforced' ? '/unenforced' : ''}`
    )
  }

  if (stale.length > 0) {
    console.log(`\n--- ${stale.length} entry(ies) past their window: re-measure, then update verifiedAt ---`)
    for (const row of stale) {
      console.log(`  STALE       ${row.id}`)
      console.log(`              claim:  ${row.claim}`)
      console.log(`              source: ${row.source}`)
      console.log(
        `              verifiedAt ${row.verifiedAt} — ${row.ageDays} days ago, window ` +
          `${row.expiresAfterDays} → stale for ${row.staleForDays} day(s)`
      )
      console.log(`              re-check with: ${row.verifiedBy}`)
      console.log(`              whereUsed (update these after re-measuring):`)
      for (const used of row.whereUsed) console.log(`                - ${used}`)
    }
  }

  if (unenforced.length > 0) {
    console.log(
      `\n--- ${unenforced.length} fact(s) name no enforcing program: the rule is followed on prose ` +
        'alone, so nothing here can fail when it drifts ---'
    )
    for (const row of unenforced) {
      console.log(`  UNENFORCED  ${row.id}`)
      console.log(`              claim: ${row.claim}`)
      console.log(`              gap:   ${row.enforcement.gap}`)
      console.log(`              write the check the rule is still only prose about, then set enforcedBy`)
    }
  }

  for (const row of partial) {
    console.log(`  NOTE        ${row.id} is enforced by ${row.enforcement.script} only in part: ${row.enforcement.gap}`)
  }

  console.log('\n---')
  if (audit.verdict === 'FRESH') {
    console.log(`VERDICT: FRESH — ${rows.length} entries, none past its window, every fact names an enforcing program.`)
  } else if (audit.verdict === 'STALE') {
    console.log(
      `VERDICT: STALE — ${stale.length} of ${rows.length} entries must be re-measured` +
        (unenforced.length > 0 ? `, and ${unenforced.length} name no enforcing program` : '') +
        (WARN_ONLY ? ' (exit 0 because --warn-only).' : '.')
    )
  } else {
    console.log(
      `VERDICT: UNENFORCED — ${unenforced.length} of ${rows.length} facts name no enforcing program, ` +
        'so the factory follows them without a check' +
        (WARN_ONLY ? ' (exit 0 because --warn-only).' : '.')
    )
  }
}

// One assignment, one policy: BROKEN is 1 and never 2 (there is nothing to re-measure until the
// record is readable), STALE and UNENFORCED are 2 ("measured once, unverified now") unless
// --warn-only relaxed the run, and a fresh, fully enforced ledger is 0.
process.exitCode = exitCode

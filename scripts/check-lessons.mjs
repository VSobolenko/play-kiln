#!/usr/bin/env node
/**
 * Does every lesson leave a sensor behind, or say out loud that it cannot?
 *
 * `harness/lessons.md` is where a defect stops being an anecdote. The rule for what an entry owes the
 * factory lives in `.cursor/rules/verification-discipline.mdc`, and until now nothing checked it: an
 * entry could describe a fix, name no check, and read exactly like an entry that closed the defect
 * for good. This CLI is what makes "every repeated defect earns a regression check" a claim the
 * repository can test rather than a sentence in a rule file.
 *
 * The contract, the three accepted shapes and the `.kiln/` caveat are documented in
 * `scripts/lib/lessons.mjs`.
 *
 * Usage:
 *   node scripts/check-lessons.mjs                 # bind entries dated 2026-09-29 or later
 *   node scripts/check-lessons.mjs --since <date>  # move the cut-off (older entries are counted only)
 *   node scripts/check-lessons.mjs --require-path  # retained for compatibility; naming a file is now required
 *   node scripts/check-lessons.mjs --json
 *
 * A bound entry must name a sensor that exists: a backticked repository path that resolves on disk, or
 * an explicit `- Sensor: none - <reason>`. A sensor bullet that names no path at all fails, because a
 * check that cannot be opened cannot fail when the check behind it is deleted or renamed.
 *
 * Exit codes, matching this repository's convention: 0 PASS, 1 FAIL, 2 UNVERIFIED (the journal is
 * missing, so there is nothing to read). `2` is never a pass.
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { evaluateLessons, SENSOR_CONTRACT_FROM } from './lib/lessons.mjs'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const argv = process.argv.slice(2)
const sinceIndex = argv.indexOf('--since')
const since = sinceIndex !== -1 ? argv[sinceIndex + 1] : SENSOR_CONTRACT_FROM
const asJson = argv.includes('--json')
const requirePath = argv.includes('--require-path')

if (sinceIndex !== -1 && !/^\d{4}-\d{2}-\d{2}$/.test(since || '')) {
  console.error(`check-lessons: --since needs a YYYY-MM-DD date (got ${since || 'nothing'})`)
  process.exit(2)
}

const journalPath = path.join(root, 'harness', 'lessons.md')
if (!fs.existsSync(journalPath)) {
  console.log('UNVERIFIED  harness/lessons.md is missing, so no lesson can be read')
  process.exit(2)
}

const exists = (relative) => fs.existsSync(path.join(root, relative.split('/').join(path.sep)))
const { verdict, rows, notes, stats } = evaluateLessons(fs.readFileSync(journalPath, 'utf8'), exists, {
  from: since,
  requirePath
})

if (asJson) {
  console.log(JSON.stringify({ verdict, since, requirePath, stats, rows, notes }, null, 2))
  process.exit(verdict === 'PASS' ? 0 : 1)
}

for (const row of rows) console.log(`  ${row.status.padEnd(11)} ${row.message}`)
for (const note of notes) console.log(`  ${'NOTE'.padEnd(11)} ${note}`)

console.log(
  `\n${stats.bound} entr${stats.bound === 1 ? 'y' : 'ies'} bound by the sensor contract (from ${since}); ` +
    `${stats.covered} declare one (${stats.none} as \`none\`, ${stats.prose} in prose without a file name).`
)
// The history is counted, never rewritten. Printing the gap is the point: it is visible, and it is not
// held against entries written before the rule existed.
console.log(
  `${stats.grandfathered} earlier entr${stats.grandfathered === 1 ? 'y' : 'ies'} grandfathered, ` +
    `${stats.grandfatheredWithoutSensor} of them without a sensor line.`
)
console.log(`\nVERDICT: ${verdict}`)
process.exit(verdict === 'PASS' ? 0 : 1)

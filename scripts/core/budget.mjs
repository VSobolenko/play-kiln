// The size budget's one maintenance command.
//
// `core.size` in `scripts/core/project.mjs` compares the harness's measured lines against the
// **recorded baselines** in `scripts/core/budget.json`, allowing a band (`tolerancePercent`) over
// each. This is the command that re-records a baseline when the band is exhausted, and it exists
// because the alternative was measured and rejected: the row used to compare against an exact
// ceiling, so every edit under `scripts/` turned it red and the remedy was a hand-written
// justification in the budget file. Nine such justifications are recorded there, none of them bought
// a deletion, and on 2026-10-07 a correct change ended red because a **parallel session** had
// written 172 lines minutes earlier. A gate whose remedy is prose an author must invent is a gate
// that gets worked around; this one's remedy is a command.
//
// What it does, in order: measure the tree the same way the check measures it, compare against the
// recorded baselines, and — only when a row is past its band — set that baseline to the measurement
// and append a revision entry naming every directory that moved, measured against `_measured`. The
// entry is machine-written and says so (`_auto: true`); a human adds prose to it when the move was a
// decision rather than drift.
//
// The honest limit, stated rather than implied: this command cannot tell a shared library from a new
// gate. `scripts/core/budget.json`'s rule is that a legacy ceiling must never absorb a new legacy
// *gate*, and that judgement stays a reader's — which is why the entry it writes records where the
// lines went, in directories, rather than asserting a reason.
//
// It writes one file, inside the repository, and no temporary file anywhere.
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import { BUDGET_PATH, budgets, measureSources, sizeLimit, sizeProblems, tolerancePercent } from './project.mjs'
import { ROOT, expectEqual, expectTrue, guard, runFixtures } from './lib.mjs'

/** The directories the recorded snapshot names, in report order. */
const AREAS = [
  { key: 'scripts/core', dir: ['core'] },
  { key: 'scripts/store', dir: ['store'] },
  { key: 'scripts/lib', dir: ['lib'] },
  { key: 'scripts (root)', dir: [] }
]

/** Measures the tree exactly as `checkSize` does. */
export function measureHarness(scriptsDir) {
  const scripts = scriptsDir ?? path.join(ROOT, 'scripts')
  const total = measureSources(scripts).totals.lines
  const surface = ['core', 'store']
    .map((name) => {
      const dir = path.join(scripts, name)
      return existsSync(dir) ? measureSources(dir).totals.lines : 0
    })
    .reduce((a, b) => a + b, 0)

  const areas = {}
  for (const area of AREAS) {
    const dir = area.dir.length === 0 ? scripts : path.join(scripts, ...area.dir)
    if (!existsSync(dir)) continue
    const lines = measureSources(dir).totals.lines
    areas[area.key] = area.dir.length === 0 ? lines - surface : lines
  }
  return { total, surface, legacy: total - surface, areas }
}

const signed = (delta) => `${delta > 0 ? '+' : ''}${delta}`

/**
 * The next budget file and what changed, given a measurement. Pure, so the fixtures drive it.
 *
 * A row inside its band is not recorded — that is the whole point of a band, and recording anyway
 * would make the trend invisible. `problems` is non-empty only when nothing could be decided.
 */
export function recordBudget(budget, measurement, now = new Date()) {
  const percent = tolerancePercent(budget)
  if (percent === null) {
    return { wrote: false, budget, moves: [], message: `no \`tolerancePercent\` in ${BUDGET_PATH}, so nothing can be recorded` }
  }
  const rows = [
    { field: 'coreBaseline', what: 'core', measured: measurement.surface },
    { field: 'legacyBaseline', what: 'legacy', measured: measurement.legacy }
  ]
  const over = rows.filter((row) => row.measured > sizeLimit(budget[row.field] ?? 0, percent))
  if (over.length === 0) {
    return {
      wrote: false,
      budget,
      moves: [],
      message:
        `nothing to record: core ${measurement.surface} and legacy ${measurement.legacy} are both ` +
        `inside the ${percent}% band over their recorded baselines`
    }
  }

  const previous = budget._measured ?? {}
  const moves = []
  for (const [key, lines] of Object.entries(measurement.areas)) {
    const before = previous[key]
    if (typeof before === 'number' && before !== lines) moves.push({ key, delta: lines - before })
  }
  moves.sort((a, b) => Math.abs(b.delta) - Math.abs(a.delta))

  const next = JSON.parse(JSON.stringify(budget))
  for (const row of over) next[row.field] = row.measured
  next._measured = { ...measurement.areas, 'scripts total': measurement.total }
  next._revisions = Array.isArray(next._revisions) ? next._revisions : []
  next._revisions.push({
    ...(over.some((row) => row.field === 'coreBaseline')
      ? { coreBaseline: `${budget.coreBaseline} -> ${measurement.surface}` }
      : {}),
    ...(over.some((row) => row.field === 'legacyBaseline')
      ? { legacyBaseline: `${budget.legacyBaseline} -> ${measurement.legacy}` }
      : {}),
    _when: now.toISOString().slice(0, 10),
    _auto: true,
    _moved: Object.fromEntries(moves.map((move) => [move.key, signed(move.delta)])),
    why:
      'Re-baselined by `node scripts/core/budget.mjs --record`, because a row had moved past its ' +
      'band. The per-directory deltas in `_moved` are measured against `_measured`, not written by ' +
      'hand, and `_auto` records that no reason was authored: this command measures the tree and ' +
      'cannot tell a shared library from a new gate, so a raise that adds a gate would need a ' +
      'reader to catch it. Per-file lines are `git diff --numstat` against the recorded commit.'
  })
  return { wrote: true, budget: next, moves, message: '' }
}

/**
 * The write path's own controls.
 *
 * `--record` is the one command in this file that **mutates** a committed budget, so it is the one
 * that most needs a fixture that can fail: a recorder that wrote on every run would erase the band's
 * trend, and one that wrote nothing when it should would leave `core.size` red with no remedy. Each
 * fixture below asserts one of those halves against a synthetic measurement, and the recorder is
 * **pure** — it returns the next file — so the tests never touch the real `budget.json`.
 */
export function recordFixtures() {
  const measurement = { total: 5000, surface: 2000, legacy: 3000, areas: { 'scripts/core': 1200, 'scripts/lib': 800, 'scripts (root)': 3000 } }
  return guard('budget.record', () =>
    runFixtures('budget.record', [
      [
        'a measurement inside the band writes nothing',
        () => {
          const budget = { tolerancePercent: 25, coreBaseline: 2000, legacyBaseline: 3000, _measured: measurement.areas }
          const result = recordBudget(budget, measurement, new Date('2026-10-07T00:00:00Z'))
          expectEqual(result.wrote, false, 'wrote')
          expectTrue(result.message.includes('nothing to record'), 'the message names the band')
          expectEqual(result.budget, budget, 'the budget is returned untouched')
        }
      ],
      [
        'a measurement past the band records the measurement and says the entry has no reason',
        () => {
          const budget = { tolerancePercent: 25, coreBaseline: 1000, legacyBaseline: 2000, _measured: measurement.areas }
          const result = recordBudget(budget, measurement, new Date('2026-10-07T00:00:00Z'))
          expectEqual(result.wrote, true, 'wrote')
          expectEqual(result.budget.coreBaseline, 2000, 'the core baseline')
          expectEqual(result.budget.legacyBaseline, 3000, 'the legacy baseline')
          const entry = result.budget._revisions[result.budget._revisions.length - 1]
          expectEqual(entry.coreBaseline, '1000 -> 2000', 'the core move')
          expectEqual(entry.legacyBaseline, '2000 -> 3000', 'the legacy move')
          expectEqual(entry._auto, true, 'the entry declares itself machine-written')
          expectEqual(entry._when, '2026-10-07', 'the date')
          expectTrue(entry.why.includes('cannot tell a shared library from a new gate'), 'the honest limit is in the entry')
        }
      ],
      [
        'the per-directory deltas are measured against `_measured`, and a directory that did not move is absent',
        () => {
          const budget = {
            tolerancePercent: 25,
            coreBaseline: 1000,
            legacyBaseline: 1000,
            _measured: { 'scripts/core': 1200, 'scripts/lib': 700, 'scripts (root)': 3000 }
          }
          const result = recordBudget(budget, measurement, new Date('2026-10-07T00:00:00Z'))
          const entry = result.budget._revisions[result.budget._revisions.length - 1]
          expectEqual(entry._moved, { 'scripts/lib': '+100' }, 'the measured moves')
        }
      ],
      [
        'a budget with no band records nothing rather than dividing by infinity',
        () => {
          const result = recordBudget({ coreBaseline: 1, legacyBaseline: 1 }, measurement)
          expectEqual(result.wrote, false, 'wrote')
          expectTrue(result.message.includes('tolerancePercent'), 'the message names the missing field')
        }
      ],
      [
        'a first recording works on a budget that has no revision history',
        () => {
          const result = recordBudget({ tolerancePercent: 1, coreBaseline: 1, legacyBaseline: 1 }, measurement)
          expectEqual(result.budget._revisions.length, 1, 'one entry created')
          expectEqual(result.budget._measured['scripts total'], 5000, 'the total is recorded')
        }
      ],
      [
        'the recorded file is a copy, so the budget handed in is never mutated in place',
        () => {
          const budget = { tolerancePercent: 1, coreBaseline: 1, legacyBaseline: 1, nested: { keep: true } }
          recordBudget(budget, measurement)
          expectEqual(budget.coreBaseline, 1, 'the input baseline')
          expectEqual(budget._revisions, undefined, 'the input is untouched')
        }
      ]
    ])
  )
}

function parseArgs(argv) {
  const options = { record: false, check: false, budget: null }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--record') options.record = true
    else if (arg === '--check') options.check = true
    else if (arg === '--budget') options.budget = argv[(index += 1)]
    else if (arg === '--help' || arg === '-h') options.help = true
    else throw new Error(`unknown argument '${arg}'`)
  }
  return options
}

export function budgetMain(argv = process.argv.slice(2)) {
  const options = parseArgs(argv)
  if (options.help || (!options.record && !options.check)) {
    console.log(
      'usage: node scripts/core/budget.mjs --check [--budget <path>]   report the measurement, write nothing\n' +
        '       node scripts/core/budget.mjs --record [--budget <path>]  re-record a baseline past its band\n' +
        '\n' +
        `The budget lives at ${BUDGET_PATH}; --budget exists so the recorder can be audited against a\n` +
        'copy before it is applied to the committed file.'
    )
    return options.help ? 0 : 2
  }

  const target = options.budget ? path.resolve(options.budget) : path.join(ROOT, BUDGET_PATH)
  const file = budgets(JSON.parse(readFileSync(target, 'utf8')))
  const measurement = measureHarness()
  const percent = tolerancePercent(file)
  console.log(
    `core ${measurement.surface}/${sizeLimit(file.coreBaseline ?? 0, percent ?? 0)} (recorded ${file.coreBaseline}); ` +
      `legacy ${measurement.legacy}/${sizeLimit(file.legacyBaseline ?? 0, percent ?? 0)} (recorded ${file.legacyBaseline}); ` +
      `scripts total ${measurement.total}`
  )
  console.log(`areas: ${Object.entries(measurement.areas).map(([k, v]) => `${k} ${v}`).join(', ')}`)
  const problems = sizeProblems({ surface: measurement.surface, legacy: measurement.legacy, budget: file })
  for (const problem of problems) console.log(`PROBLEM: ${problem}`)

  if (options.check) {
    console.log(problems.length === 0 ? 'within the band: nothing to record' : 'past the band: --record would set the baselines above')
    return 0
  }

  const result = recordBudget(file, measurement)
  if (!result.wrote) {
    console.log(result.message)
    return 0
  }
  writeFileSync(target, `${JSON.stringify(result.budget, null, 2)}\n`, 'utf8')
  console.log(`recorded: ${result.moves.map((move) => `${move.key} ${signed(move.delta)}`).join(', ') || 'no measured directory moved'}`)
  console.log(`updated ${path.relative(ROOT, target) || target}`)
  return 0
}

function runCli() {
  try {
    process.exitCode = budgetMain()
  } catch (error) {
    console.error(`FAIL: ${error.message}`)
    process.exitCode = 1
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) runCli()

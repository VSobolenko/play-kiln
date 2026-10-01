#!/usr/bin/env node
// The factory's verification entry point: every check, one process, one report.
//
// Usage
//   node scripts/verify.mjs                      project-wide invariants only
//   node scripts/verify.mjs --slot <slug>        the slot's pack, icon, client and lifecycle
//   node scripts/verify.mjs --slot <slug> --build
//                                                the above, plus assembleDebug
//   node scripts/verify.mjs --only invariants,assets
//   node scripts/verify.mjs --json
//
// Exit codes
//   0  every check that ran PASSed (SKIP is not a failure, and is never printed as PASS)
//   1  at least one FAIL
//   2  no FAIL, but at least one UNVERIFIED or SKIP that this run cannot call a pass
//
// Why the codes are ordered this way: `UNVERIFIED` means the check could not decide, and a run
// that cannot decide must not be quoted as release evidence. `.cursor/rules/verification-discipline`
// records the cost of the alternative — six suites cited as evidence and never executed.
import path from 'node:path'
import { CHECKS, CHECK_IDS } from './core/index.mjs'
import {
  ROOT,
  VERDICT,
  fail,
  noteSpawn,
  pass,
  resolveSlotDir,
  skip,
  spawnedProcesses,
  unverified
} from './core/lib.mjs'

const USAGE = `node scripts/verify.mjs [--slot <slug>] [--build] [--only <ids>] [--json]

  --slot <slug>     verify one slot under apps/<slug>
  --build           also compile the slot's debug variant (the only subprocess this runs)
  --only <ids>      comma-separated subset of: ${CHECK_IDS.join(', ')}
  --json            emit the report as JSON
  --help            this text`

function parseArgs(argv) {
  const options = { slot: null, build: false, only: null, json: false, help: false }
  const takesValue = new Set(['--slot', '--only'])
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--help' || arg === '-h') {
      options.help = true
      continue
    }
    if (arg === '--build') {
      options.build = true
      continue
    }
    if (arg === '--json') {
      options.json = true
      continue
    }
    if (takesValue.has(arg)) {
      const value = argv[index + 1]
      if (value === undefined || value.startsWith('--')) {
        throw new Error(`${arg} needs a value`)
      }
      index += 1
      if (arg === '--slot') options.slot = value
      else options.only = value.split(',').map((part) => part.trim()).filter(Boolean)
      continue
    }
    throw new Error(`unknown option: ${arg}`)
  }
  if (options.only) {
    const unknown = options.only.filter((id) => !CHECK_IDS.includes(id))
    if (unknown.length > 0) {
      throw new Error(`unknown check id(s): ${unknown.join(', ')} — known: ${CHECK_IDS.join(', ')}`)
    }
  }
  return options
}

async function buildVerdict(slotDir, tasks) {
  // Imported lazily so a run without --build never touches the JDK resolver: a project-wide
  // invariant check must not fail because a slot's toolchain is missing.
  const { runGradleTasks } = await import('./lib/gradle.mjs')
  try {
    const outcome = runGradleTasks(slotDir, tasks)
    // The one subprocess this entry point is allowed to spawn must appear in the ledger, or the
    // `subprocesses:` line is a false statement: it printed 0 while a Gradle build was running,
    // which is exactly the class of claim this repository refuses to make.
    noteSpawn([outcome.command, ...outcome.args].join(' '))
    const status = outcome.result ? outcome.result.status : null
    const detail = `${tasks.join(' ')} in ${outcome.elapsedMs} ms` +
      (outcome.javaMajor ? ` under JDK ${outcome.javaMajor} (${outcome.javaSource})` : '')
    if (status !== 0) {
      const stderr = (outcome.result && outcome.result.stderr) || ''
      return fail('build', `${detail}; exit ${status}\n${stderr.split('\n').slice(-15).join('\n')}`)
    }
    return pass('build', detail)
  } catch (error) {
    return unverified('build', `could not run Gradle: ${error.message}`)
  }
}

function print(report, options) {
  if (options.json) {
    process.stdout.write(`${JSON.stringify(report, null, 2)}\n`)
    return
  }
  const width = report.rows.reduce((max, row) => Math.max(max, row.id.length), 0)
  process.stdout.write(`\ncore verification${report.slot ? ` — ${report.slot}` : ' — project-wide'}\n\n`)
  for (const row of report.rows) {
    const detail = (row.detail || '').split('\n').join('\n' + ' '.repeat(width + 14))
    process.stdout.write(`  ${row.verdict.padEnd(11)}${row.id.padEnd(width)}  ${detail}\n`)
  }
  const counts = report.rows.reduce((acc, row) => {
    acc[row.verdict] = (acc[row.verdict] || 0) + 1
    return acc
  }, {})
  const summary = Object.entries(counts).map(([verdict, count]) => `${count} ${verdict}`).join(', ')
  process.stdout.write(`\n${summary || 'no checks ran'}\n`)
  process.stdout.write(`verdict: ${report.verdict}\n`)
  process.stdout.write(`elapsed: ${report.elapsedMs} ms\n`)
  process.stdout.write(
    `subprocesses: ${report.subprocesses.length}` +
      (report.subprocesses.length ? ` (${report.subprocesses.join('; ')})` : '') +
      '\n'
  )
  if (report.verdict === VERDICT.SKIP) {
    process.stdout.write(`\nnot a full pass: ${report.reason}\n`)
  }
  if (report.exitCode !== 0) {
    process.stdout.write(`\n(node scripts/verify.mjs exited ${report.exitCode}: ${report.reason})\n`)
  }
}

/**
 * The exit code answers "may this run be quoted as evidence?", which is a narrower question than
 * "did anything at all happen". SKIP never changes the code, because a check that was not requested
 * is not a finding — but it does change the verdict, so a run that skipped work is never printed as
 * a clean PASS. UNVERIFIED does change the code, because a check that could not decide is the one
 * state that must never be quoted as a pass.
 */
function verdictFor(rows) {
  if (rows.some((row) => row.verdict === VERDICT.FAIL)) {
    return { verdict: VERDICT.FAIL, exitCode: 1, reason: 'a check failed; read the FAIL rows above' }
  }
  if (rows.some((row) => row.verdict === VERDICT.UNVERIFIED)) {
    return {
      verdict: VERDICT.UNVERIFIED,
      exitCode: 2,
      reason: 'a check could not decide; an UNVERIFIED row is not a pass'
    }
  }
  if (rows.some((row) => row.verdict === VERDICT.SKIP)) {
    const skipped = rows.filter((row) => row.verdict === VERDICT.SKIP).map((row) => row.id)
    return {
      verdict: VERDICT.SKIP,
      exitCode: 0,
      reason: `not evaluated: ${skipped.join(', ')}`
    }
  }
  return { verdict: VERDICT.PASS, exitCode: 0, reason: '' }
}

async function main() {
  let options
  try {
    options = parseArgs(process.argv.slice(2))
  } catch (error) {
    process.stderr.write(`${error.message}\n\n${USAGE}\n`)
    process.exit(2)
  }
  if (options.help) {
    process.stdout.write(`${USAGE}\n`)
    process.exit(0)
  }

  const started = Date.now()
  const rows = []
  let slotDir = null
  let slotLabel = null

  if (options.slot) {
    try {
      slotDir = resolveSlotDir(options.slot)
      slotLabel = path.relative(ROOT, slotDir).split(path.sep).join('/')
    } catch (error) {
      process.stderr.write(`verify: ${error.message}\n`)
      process.exit(2)
    }
  }

  for (const check of CHECKS) {
    if (options.only && !options.only.includes(check.id)) continue
    if (check.needsSlot && !slotDir) {
      rows.push(skip(check.id, 'no --slot given, so this check did not run'))
      continue
    }
    try {
      rows.push(...check.run(slotDir))
    } catch (error) {
      // A check that throws is UNVERIFIED, never a pass: the machinery failed, not the artifact.
      rows.push(unverified(check.id, `the check threw: ${error.message}`))
    }
  }

  if (options.build) {
    if (!slotDir) {
      rows.push(unverified('build', '--build needs --slot'))
    } else {
      // No wrapper-existence pre-check here on purpose. Naming `gradlew` in this file would make it
      // a second place that knows how a Gradle build is started, and it would duplicate the check
      // `runGradleTasks` already does — it throws `gradlew.bat not found`, which the catch below
      // turns into UNVERIFIED.
      rows.push(await buildVerdict(slotDir, ['assembleDebug']))
    }
  } else if (slotDir && (!options.only || options.only.includes('build'))) {
    // Ledger row 12.11: a build-less run is UNVERIFIED, never a pass. `SKIP` would have exited 0,
    // which a caller reading only the exit code cannot tell apart from a full pass — the defect the
    // row exists to prevent. Source-only evidence cannot prove the app builds.
    rows.push(
      unverified(
        'build',
        'assembleDebug was not compiled because --build was not passed; source-only evidence cannot ' +
          'prove the app builds, so this run must not be quoted as release evidence'
      )
    )
  }

  const outcome = verdictFor(rows)
  const report = {
    slot: slotLabel,
    rows,
    ...outcome,
    elapsedMs: Date.now() - started,
    subprocesses: spawnedProcesses()
  }
  print(report, options)
  process.exit(outcome.exitCode)
}

main()

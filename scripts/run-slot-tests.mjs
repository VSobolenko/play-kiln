#!/usr/bin/env node
/**
 * Sanctioned unit-test entry point for one PlayKiln slot.
 *
 *   node scripts/run-slot-tests.mjs --slot pipe-sizing
 *   node scripts/run-slot-tests.mjs --slot pipe-sizing --rerun
 *
 * A green Gradle run is not the same as an executed test suite. Gradle prints
 * `> Task :app:testDebugUnitTest UP-TO-DATE` when it believes nothing the task
 * depends on changed, and the XML under `build/test-results/` is then the
 * *previous* run's output. A test that reads a file off disk — `HomeViewModelTest`
 * reads `src/main/res/values/strings.xml` to check the shipped copy — is an input
 * Gradle does not know about, so a copy-only change leaves the task UP-TO-DATE and
 * the suite silently does not run. Reporting those stale results as PASS is a check
 * that cannot fail. This runner reports STALE and exits 2 instead, and `--rerun`
 * forces execution.
 */

import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'
import { runGradleTasks } from './lib/gradle.mjs'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
export const UNIT_TEST_TASKS = Object.freeze(['testDebugUnitTest'])

/** Forces Gradle to execute a task instead of reporting it up to date. */
export const RERUN_FLAG = '--rerun'

/** Gradle task outcomes that mean the task did not execute in this invocation. */
export const NON_EXECUTED_OUTCOMES = Object.freeze(['UP-TO-DATE', 'FROM-CACHE', 'NO-SOURCE'])

/** Filesystem timestamps can be truncated, so allow this much slack against the run start. */
const MTIME_TOLERANCE_MS = 2000

function listDirectories(directory) {
  if (!existsSync(directory)) return []
  return readdirSync(directory)
    .map((name) => path.join(directory, name))
    .filter((candidate) => statSync(candidate).isDirectory())
}

export function resolveSlotDir(slug, appsDir = path.join(ROOT, 'apps')) {
  const matches = listDirectories(appsDir).filter((candidate) => {
    const name = path.basename(candidate)
    return name === slug || name.endsWith(`-${slug}`)
  })
  return matches.length === 1 ? matches[0] : null
}

function xmlFiles(directory, found = []) {
  if (!existsSync(directory)) return found
  for (const name of readdirSync(directory)) {
    const candidate = path.join(directory, name)
    if (statSync(candidate).isDirectory()) xmlFiles(candidate, found)
    else if (name.endsWith('.xml')) found.push(candidate)
  }
  return found
}

function resultDir(slotDir, task) {
  return path.join(slotDir, 'app', 'build', 'test-results', task)
}

/**
 * Newest mtime among a task's XML reports, or null when the task has no reports.
 * Derived from the artifact on disk, so it cannot be faked by a status code.
 */
export function newestResultMtime(slotDir, task) {
  const files = xmlFiles(resultDir(slotDir, task))
  if (files.length === 0) return null
  return Math.max(...files.map((file) => statSync(file).mtimeMs))
}

/**
 * Gradle's own outcome for one task, read from its console line.
 *
 * Returns `'EXECUTED'` when the task ran with no suffix, the recorded outcome when
 * it did not (`'UP-TO-DATE'`, `'FROM-CACHE'`, …), and `null` when Gradle never named
 * the task — which is itself a signal that the output does not describe this task.
 */
export function taskOutcome(output, task) {
  for (const raw of String(output || '').split(/\r?\n/)) {
    const match = /^>\s*Task\s+(\S+)(?:\s+(\S+))?$/.exec(raw.trim())
    if (!match) continue
    const taskPath = match[1]
    if (taskPath !== task && !taskPath.endsWith(`:${task}`)) continue
    return match[2] || 'EXECUTED'
  }
  return null
}

export function testCounts(slotDir, task) {
  const counts = { tests: 0, failures: 0, errors: 0, skipped: 0, files: 0 }
  for (const file of xmlFiles(resultDir(slotDir, task))) {
    const suite = readFileSync(file, 'utf8').match(/<testsuite\b[^>]*>/)?.[0]
    if (!suite) continue
    counts.files += 1
    for (const key of ['tests', 'failures', 'errors', 'skipped']) {
      const value = suite.match(new RegExp(`\\b${key}="(\\d+)"`))?.[1]
      counts[key] += Number(value || 0)
    }
  }
  return counts
}

function gradleOutput(result) {
  return `${result?.stdout || ''}${result?.stderr || ''}`.trim()
}

/**
 * Run the slot test task through the shared, pinned-JDK Gradle service.
 *
 * `force` adds `--rerun`, which makes Gradle execute the task instead of reporting
 * it up to date — use it whenever a PASS is going to be cited as evidence.
 */
export function runSlotUnitTests(
  slug,
  { appsDir = path.join(ROOT, 'apps'), runGradle = runGradleTasks, report = console.log, force = false } = {}
) {
  const slotDir = resolveSlotDir(slug, appsDir)
  if (!slotDir) {
    report(`FAIL slot directory not found or ambiguous under apps/: ${slug}; run node scripts/kiln-status.mjs to list slots.`)
    return { ok: false, exitCode: 1, results: [] }
  }

  const results = []
  for (const task of UNIT_TEST_TASKS) {
    const startedAt = Date.now()
    const beforeMtime = newestResultMtime(slotDir, task)
    let invocation
    try {
      invocation = runGradle(slotDir, force ? [task, RERUN_FLAG] : [task])
    } catch (error) {
      report(`FAIL ${task} — ${error?.message || String(error)}`)
      return { ok: false, exitCode: 1, results }
    }
    const result = invocation.result
    const output = gradleOutput(result)
    if (result?.error || result?.status !== 0) {
      report(`FAIL ${task} — Gradle exited ${result?.status ?? 'with an error'}`)
      if (output) console.log(output.split(/\r?\n/).map((line) => `        ${line}`).join('\n'))
      return { ok: false, exitCode: 1, results }
    }

    const counts = testCounts(slotDir, task)
    const outcome = taskOutcome(output, task)
    const afterMtime = newestResultMtime(slotDir, task)
    const resultsWrittenThisRun = afterMtime !== null && afterMtime >= startedAt - MTIME_TOLERANCE_MS

    // A named, non-executing outcome is definitive. Otherwise, if reports already
    // existed and none was rewritten, the numbers we are about to print describe an
    // earlier invocation. Either way the suite did not run, so this is not a PASS.
    const didNotExecute = NON_EXECUTED_OUTCOMES.includes(outcome)
    const reportsPreexisted = beforeMtime !== null
    const stale = didNotExecute || (outcome === null && reportsPreexisted && !resultsWrittenThisRun)

    if (stale) {
      const why = didNotExecute
        ? `Gradle reported the task ${outcome}`
        : 'Gradle never named the task and no report was rewritten'
      report(
        `STALE ${task} — ${why}; the ${counts.tests} test(s) on disk are from an earlier run and did not execute now. Re-run with ${RERUN_FLAG} to force the suite.`
      )
      results.push({ task, counts, elapsedMs: invocation.elapsedMs, stale: true, outcome })
      return { ok: false, exitCode: 2, results }
    }

    results.push({ task, counts, elapsedMs: invocation.elapsedMs, stale: false, outcome })
    if (counts.tests === 0) {
      report(`NO TESTS ${task} — no tests found (task completed in ${invocation.elapsedMs}ms).`)
    } else {
      report(
        `PASS ${task} — ${counts.tests} tests, ${counts.failures} failures, ${counts.errors} errors, ${counts.skipped} skipped (${invocation.elapsedMs}ms).`
      )
    }
  }
  return { ok: true, exitCode: 0, results }
}

function printHelp() {
  console.log(`Usage: node scripts/run-slot-tests.mjs --slot <slug> [--rerun]

  --slot <slug>     run the slot's debug unit tests through the factory Gradle service
  --rerun           add Gradle's --rerun so the suite executes instead of reporting UP-TO-DATE
  --help            print this message

The runner pins the factory JDK and makes one Gradle invocation. A completed task
with no XML test results is reported as "no tests found", not as a test PASS.

A task Gradle did not execute in this invocation is reported as STALE and exits 2,
never PASS: the XML on disk would otherwise be a previous run's output. Use --rerun
when the result is going to be cited as evidence.`)
}

function main(argv) {
  if (argv.includes('--help') || argv.includes('-h')) {
    printHelp()
    return 0
  }
  const index = argv.indexOf('--slot')
  const slot = index === -1 ? null : argv[index + 1]
  if (!slot || slot.startsWith('-')) {
    console.error('run-slot-tests: --slot <slug> is required; run with --help for usage.')
    return 1
  }
  return runSlotUnitTests(slot, { force: argv.includes(RERUN_FLAG) }).exitCode
}

if (process.argv[1] === fileURLToPath(import.meta.url)) process.exitCode = main(process.argv.slice(2))

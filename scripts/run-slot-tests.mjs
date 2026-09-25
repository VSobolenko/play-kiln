#!/usr/bin/env node
/**
 * Sanctioned unit-test entry point for one PlayKiln slot.
 *
 *   node scripts/run-slot-tests.mjs --slot pipe-sizing
 */

import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'
import { runGradleTasks } from './lib/gradle.mjs'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
export const UNIT_TEST_TASKS = Object.freeze(['testDebugUnitTest'])

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

export function testCounts(slotDir, task) {
  const resultDir = path.join(slotDir, 'app', 'build', 'test-results', task)
  const counts = { tests: 0, failures: 0, errors: 0, skipped: 0, files: 0 }
  for (const file of xmlFiles(resultDir)) {
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

/** Run the slot test task through the shared, pinned-JDK Gradle service. */
export function runSlotUnitTests(
  slug,
  { appsDir = path.join(ROOT, 'apps'), runGradle = runGradleTasks, report = console.log } = {}
) {
  const slotDir = resolveSlotDir(slug, appsDir)
  if (!slotDir) {
    report(`FAIL slot directory not found or ambiguous under apps/: ${slug}; run node scripts/kiln-status.mjs to list slots.`)
    return { ok: false, exitCode: 1, results: [] }
  }

  const results = []
  for (const task of UNIT_TEST_TASKS) {
    let invocation
    try {
      invocation = runGradle(slotDir, task)
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
    results.push({ task, counts, elapsedMs: invocation.elapsedMs })
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
  console.log(`Usage: node scripts/run-slot-tests.mjs --slot <slug>

  --slot <slug>     run the slot's debug unit tests through the factory Gradle service
  --help            print this message

The runner pins the factory JDK and makes one Gradle invocation. A completed task
with no XML test results is reported as "no tests found", not as a test PASS.`)
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
  return runSlotUnitTests(slot).exitCode
}

if (process.argv[1] === fileURLToPath(import.meta.url)) process.exitCode = main(process.argv.slice(2))

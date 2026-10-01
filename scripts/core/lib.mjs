// Shared primitives for the factory core.
//
// The core's contract (see openspec/changes/reduce-factory-to-core/specs/factory-core/spec.md):
//   - one entry point runs every check, in ONE process (scripts/verify.mjs);
//   - a check is a function, never a script that shells out to another script;
//   - the only permitted subprocess is the build tool, and only when a build is requested;
//   - `UNVERIFIED` is a first-class verdict and is never printed as `PASS`.
//
// Every Play limit lives in scripts/lib/play-assets.json, which both Node and Python read.
// Nothing in the core restates a value from there; `assets.mjs` goes further and asserts that the
// legacy Node constants still agree with the JSON, so the two sources cannot drift apart.
import { execFileSync } from 'node:child_process'
import { readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')

/** Verdict vocabulary. `UNVERIFIED` means "could not be evaluated", never "fine". */
export const VERDICT = Object.freeze({
  PASS: 'PASS',
  FAIL: 'FAIL',
  SKIP: 'SKIP',
  UNVERIFIED: 'UNVERIFIED'
})

// The subprocess ledger. A check that spawns a process must say so through here, so the entry
// point prints the real count rather than a promise that spawning does not happen.
const spawns = []
export function noteSpawn(label) {
  spawns.push(label)
}
export function spawnedProcesses() {
  return spawns.slice()
}

export const pass = (id, detail = '') => ({ id, verdict: VERDICT.PASS, detail })
export const fail = (id, detail) => ({ id, verdict: VERDICT.FAIL, detail })
export const skip = (id, detail) => ({ id, verdict: VERDICT.SKIP, detail })
export const unverified = (id, detail) => ({ id, verdict: VERDICT.UNVERIFIED, detail })

/** A check that fails only when it cannot decide, which is a distinct outcome from failing. */
export function guard(id, body) {
  try {
    return body()
  } catch (error) {
    return unverified(id, `could not evaluate: ${error.message}`)
  }
}

/** Reads the single source of truth for Play limits. Throws rather than falling back. */
export function playAssets() {
  return JSON.parse(readFileSync(path.join(ROOT, 'scripts', 'lib', 'play-assets.json'), 'utf8'))
}

/** Resolves a slot slug to its directory under apps/, refusing an ambiguous match. */
export function resolveSlotDir(slug) {
  const apps = path.join(ROOT, 'apps')
  let names
  try {
    names = readdirSync(apps, { withFileTypes: true })
      .filter((entry) => entry.isDirectory())
      .map((entry) => entry.name)
  } catch {
    throw new Error(`no apps/ directory at ${apps}`)
  }
  const matches = names.filter((name) => name === slug || name.endsWith(`-${slug}`))
  if (matches.length === 0) throw new Error(`no slot under apps/ matches '${slug}'`)
  if (matches.length > 1) throw new Error(`slot '${slug}' is ambiguous: ${matches.join(', ')}`)
  return path.join(apps, matches[0])
}

/** Runs a command and records the spawn. Only the build route may call this. */
export function runRecorded(command, args, options = {}) {
  noteSpawn([command, ...args].join(' '))
  return execFileSync(command, args, { encoding: 'utf8', ...options })
}

/**
 * Runs a set of inline fixtures and reports which ones failed.
 *
 * A fixture set exists to make a sensor falsifiable: every entry asserts the behaviour the check
 * depends on, and at least one entry in each set must be capable of failing against a wrong
 * implementation (`.cursor/rules/verification-discipline.mdc`).
 */
export function runFixtures(id, cases) {
  const failures = []
  for (const [label, body] of cases) {
    try {
      body()
    } catch (error) {
      failures.push(`${label}: ${error.message}`)
    }
  }
  return failures.length === 0
    ? pass(id, `${cases.length} fixtures`)
    : fail(id, failures.join(' | '))
}

export function expectEqual(actual, expected, label = 'value') {
  const a = JSON.stringify(actual)
  const e = JSON.stringify(expected)
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`)
}

export function expectTrue(value, label) {
  if (!value) throw new Error(`${label}: expected true`)
}

export function expectThrows(body, label) {
  let threw = false
  try {
    body()
  } catch {
    threw = true
  }
  if (!threw) throw new Error(`${label}: expected a thrown error`)
}

#!/usr/bin/env node
/**
 * Hard gate: a slot must state a monetization plan its capabilities can actually
 * deliver, backed by research, with numeric kill criteria.
 *
 * Why this exists: apps/2026-09-25-pipe-sizing shipped with
 * `capabilities.json = {"schemaVersion":1,"enabled":[]}` — no billing, no ads, no
 * revenue mechanism — and every existing gate returned PASS. The archetype rule in
 * `docs/monetization-strategy.md` §3.6 and the kill criteria in §5 item 5 were prose
 * no program read, and prose that describes a check that does not exist is this
 * repo's most-recorded failure mode. This script is the check.
 *
 * Usage:
 *   node scripts/check-economics.mjs --slot <slug> [--validate-only]
 *   node scripts/check-economics.mjs --economics <path> [--validate-only]
 *
 * `--economics <path>` validates a plan that is not in a slot yet (how
 * `new-slot.mjs` uses it). In that mode the capability pairing cannot be evaluated,
 * because the capabilities are not enabled until the slot exists; it is skipped and
 * enforced later against the real slot.
 */

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { basename, dirname, join, relative, resolve, sep } from 'node:path'
import process from 'node:process'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { parseEconomics, validateEconomicsAgainstCapabilities } from './lib/slot-economics.mjs'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const usage =
  'node scripts/check-economics.mjs --slot <slug> [--validate-only] | --economics <path> [--validate-only]'

function line(status, message) {
  console.log(`${status.padEnd(8)} ${message}`)
}

function parseArgs(argv) {
  const values = new Set(['--slot', '--economics'])
  const flags = new Set(['--validate-only'])
  const parsed = { validateOnly: false }

  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index]
    if (flags.has(option)) {
      parsed.validateOnly = true
      continue
    }
    if (!values.has(option)) throw new Error(`Unknown option: ${option}\nUsage: ${usage}`)
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) throw new Error(`Missing value for ${option}\nUsage: ${usage}`)
    parsed[option.slice(2)] = value
    index += 1
  }
  return parsed
}

function safeIsFileType(candidate, type) {
  try {
    const stats = statSync(candidate)
    return type === 'directory' ? stats.isDirectory() : stats.isFile()
  } catch {
    return false
  }
}

function listAppDirs() {
  const appsDir = join(ROOT, 'apps')
  if (!existsSync(appsDir)) return []
  return readdirSync(appsDir)
    .map((name) => join(appsDir, name))
    .filter((candidate) => safeIsFileType(candidate, 'directory'))
}

/**
 * Resolve a slot the way `scripts/verify.mjs` does: an exact directory name or
 * a `<date>-<slug>` suffix, and only when exactly one candidate matches, so an
 * ambiguous slug fails instead of silently picking one.
 */
function resolveSlotDir(slug) {
  const matches = listAppDirs().filter((candidate) => {
    const name = basename(candidate)
    return name === slug || name.endsWith(`-${slug}`)
  })
  return matches.length === 1 ? matches[0] : null
}

function readJsonFile(file, label) {
  let raw
  try {
    raw = readFileSync(file, 'utf8')
  } catch (error) {
    throw new Error(`${label} could not be read: ${file} (${error.message})`)
  }
  try {
    return JSON.parse(raw.replace(/^\uFEFF/, ''))
  } catch (error) {
    throw new Error(`${label} is not valid JSON: ${file} (${error.message})`)
  }
}

function finish(blocked) {
  console.log(`\nVERDICT: ${blocked ? 'BLOCKED' : 'PASS'}`)
  process.exitCode = blocked ? 1 : 0
}

function main() {
  const options = parseArgs(process.argv.slice(2))
  if (!options.slot && !options.economics) {
    throw new Error(`Missing --slot <slug> or --economics <path>\nUsage: ${usage}`)
  }

  let slotDir = null
  if (options.slot) {
    slotDir = resolveSlotDir(options.slot)
    if (!slotDir) throw new Error(`slot not found or ambiguous under apps/: ${options.slot}`)
    line('INFO', `slot: ${relative(ROOT, slotDir).split(sep).join('/')}`)
  }

  const economicsFile = options.economics
    ? resolve(ROOT, options.economics)
    : join(slotDir, 'economics.json')
  line('INFO', `plan: ${economicsFile}`)

  const parsed = readJsonFile(economicsFile, 'economics plan')

  // A schema failure stops here: pairing an invalid archetype or checking a missing
  // evidenceRef would report derived noise on top of the real problem.
  const plan = parseEconomics(parsed)
  if (!plan.ok) {
    for (const problem of plan.errors) line('BLOCKED', problem)
    return finish(true)
  }
  line('PASS', `economics schema — archetype ${plan.value.archetype}, priceUsd ${plan.value.priceUsd}`)

  let blocked = false

  if (slotDir) {
    const capabilitiesFile = join(slotDir, 'capabilities.json')
    if (!existsSync(capabilitiesFile)) {
      line(
        'BLOCKED',
        `capabilities.json is missing: ${relative(ROOT, capabilitiesFile)} — a slot at verify time must declare its capabilities`
      )
      blocked = true
    } else {
      const capabilities = readJsonFile(capabilitiesFile, 'capabilities.json')
      const pairing = validateEconomicsAgainstCapabilities(plan.value, capabilities)
      if (pairing.ok) {
        line('PASS', `capabilities satisfy archetype ${plan.value.archetype}`)
      } else {
        for (const problem of pairing.errors) line('BLOCKED', problem)
        blocked = true
      }
    }
  } else {
    // At scaffold time the capabilities are not enabled yet, so the pairing has no
    // evidence to read. Skipping (rather than failing) is why this mode exists; the
    // teeth are applied later, against the created slot.
    line('SKIP', 'capability pairing is enforced later by `node scripts/check-economics.mjs --slot <slug>`')
  }

  if (options.validateOnly) {
    line('SKIP', 'evidenceRef file check skipped by --validate-only')
    return finish(blocked)
  }

  const reference = String(plan.value.evidenceRef).split('#')[0]
  const referenceFile = join(ROOT, reference)
  if (safeIsFileType(referenceFile, 'file')) {
    line('PASS', `evidenceRef resolves on disk: ${reference}`)
  } else {
    line('BLOCKED', `evidenceRef does not resolve to a file: ${reference} (relative to the repo root)`)
    blocked = true
  }

  return finish(blocked)
}

function runCli() {
  try {
    main()
  } catch (error) {
    line('BLOCKED', error instanceof Error ? error.message : String(error))
    console.log('\nVERDICT: BLOCKED')
    process.exitCode = 1
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) runCli()

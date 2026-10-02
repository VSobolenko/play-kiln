#!/usr/bin/env node
/**
 * Hard gate: a slot must state the name it ships, derived from its job, audience and market, with
 * its candidates and its rejected alternatives — and the name must satisfy Google Play's title rules.
 *
 * Why this exists. `scripts/new-slot.mjs --name "<Title>"` accepted free text and nothing validated
 * it, so an agent building a utility could call it `Calculator` — the bare category noun the owner
 * names as the defect — and spend Play's strongest ranking field on the head term the incumbent
 * already owns. `docs/research/2026-10-01-play-title-policy-and-aso.md` measured the rules and
 * `docs/research/2026-10-01-app-naming-methodology.md` the procedure; `scripts/lib/slot-naming.mjs`
 * is the contract this script applies, and every message below is built from it rather than
 * restated, so the wording cannot drift away from the logic
 * (`.cursor/rules/verification-discipline.mdc`).
 *
 * Usage:
 *   node scripts/check-naming.mjs --slot <slug> [--validate-only]
 *   node scripts/check-naming.mjs --naming <path> [--validate-only]
 *
 * `--naming <path>` validates a contract that is not in a slot yet, which is how
 * `scripts/new-slot.mjs` calls it. A missing file is `UNVERIFIED` (exit 2), never a pass: nothing
 * scanned must not read as nothing found.
 */

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { basename, dirname, join, relative, resolve, sep } from 'node:path'
import process from 'node:process'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { parseNaming, validateNamingAgainstListing } from './lib/slot-naming.mjs'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const usage =
  'node scripts/check-naming.mjs --slot <slug> [--validate-only] | --naming <path> [--validate-only]'

function line(status, message) {
  console.log(`${status.padEnd(10)} ${message}`)
}

function parseArgs(argv) {
  const values = new Set(['--slot', '--naming'])
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
 * Resolve a slot the way `scripts/verify.mjs` does: an exact directory name or a `<date>-<slug>`
 * suffix, and only when exactly one candidate matches, so an ambiguous slug fails instead of
 * silently picking one.
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

/** Exit 0 on PASS, 1 on FAIL/BLOCKED, 2 on UNVERIFIED — the vocabulary `scripts/core/lib.mjs` uses. */
function finish(status) {
  console.log(`\nVERDICT: ${status}`)
  process.exitCode = status === 'PASS' ? 0 : status === 'UNVERIFIED' ? 2 : 1
}

function main() {
  const options = parseArgs(process.argv.slice(2))
  if (!options.slot && !options.naming) {
    throw new Error(`Missing --slot <slug> or --naming <path>\nUsage: ${usage}`)
  }

  let slotDir = null
  if (options.slot) {
    slotDir = resolveSlotDir(options.slot)
    if (!slotDir) throw new Error(`slot not found or ambiguous under apps/: ${options.slot}`)
    line('INFO', `slot: ${relative(ROOT, slotDir).split(sep).join('/')}`)
  }

  const namingFile = options.naming ? resolve(ROOT, options.naming) : join(slotDir, 'naming.json')
  line('INFO', `naming: ${namingFile}`)

  if (!safeIsFileType(namingFile, 'file')) {
    line(
      'UNVERIFIED',
      `no naming contract at ${namingFile} — a missing decision is not a pass, and deleting the file ` +
        'must not switch this gate off; record one with the slot\'s name, candidates and rejected alternatives'
    )
    return finish('UNVERIFIED')
  }

  const parsed = readJsonFile(namingFile, 'naming contract')

  // A schema failure stops here: cross-checking the listing against an invalid contract would report
  // derived noise on top of the real problem.
  const naming = parseNaming(parsed)
  if (!naming.ok) {
    for (const problem of naming.errors) line('BLOCKED', problem)
    return finish('BLOCKED')
  }
  line(
    'PASS',
    `naming schema — title "${naming.value.title}", archetype ${naming.value.archetype}, ` +
      `${naming.value.candidates.length} candidate(s), ${naming.value.rejected.length} rejected`
  )

  let blocked = false

  if (slotDir) {
    const listingFile = join(slotDir, 'store', 'listing.json')
    if (!existsSync(listingFile)) {
      line('SKIP', `no ${relative(ROOT, listingFile).split(sep).join('/')} yet — a slot in development has no pack`)
    } else {
      const listing = readJsonFile(listingFile, 'store/listing.json')
      const mismatches = validateNamingAgainstListing(naming.value, listing)
      if (mismatches.length === 0) {
        line('PASS', 'the pack titles agree with the approved name')
      } else {
        for (const problem of mismatches) line('BLOCKED', problem)
        blocked = true
      }
    }
  } else {
    // At scaffold time there is no slot and no pack, so the listing cross-check has nothing to read.
    line('SKIP', 'the pack cross-check is enforced later by `node scripts/check-naming.mjs --slot <slug>`')
  }

  if (options.validateOnly) {
    line('SKIP', 'evidence file check skipped by --validate-only')
    return finish(blocked ? 'BLOCKED' : 'PASS')
  }

  const reference = String(naming.value.evidence).split('#')[0]
  const referenceFile = join(ROOT, reference)
  if (safeIsFileType(referenceFile, 'file')) {
    line('PASS', `evidence resolves on disk: ${reference}`)
  } else {
    line('BLOCKED', `evidence does not resolve to a file: ${reference} (relative to the repo root)`)
    blocked = true
  }

  return finish(blocked ? 'BLOCKED' : 'PASS')
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

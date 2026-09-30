#!/usr/bin/env node
/**
 * CLI for the client-side secret / paid-API sensor. The logic lives in
 * `scripts/lib/client-secrets.mjs` so `scripts/test-client-secrets.mjs` can drive it against
 * throwaway sandboxes: one implementation, two callers, and a CLI failure and a test failure cannot
 * drift apart.
 *
 * Why this exists: the factory's `secret-guard` hook and `.cursor/rules/security-secrets.mdc` protect
 * the **repository**, and nothing protected the shipped **APK**. A slot that compiled a provider key
 * into `app/src/main/` would pass every other gate and ship the credential. The measured exposure is
 * in `docs/research/2026-09-29-app-factory-prior-art.md` §3 row 8; the rule is
 * `.cursor/rules/client-side-secrets.mdc`; this script is the enforcement that rule names.
 *
 * Usage:
 *   node scripts/check-client-secrets.mjs --slot <slug> [--json]
 *
 * Exit codes, matching this repository's convention: 0 PASS, 1 FAIL, 2 UNVERIFIED (the slot is
 * missing, ambiguous, unreadable, or has nothing to scan). `2` is never a pass — a slot with no
 * `app/src/main/` must not be readable as "no secrets found".
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { scanSlot } from './lib/client-secrets.mjs'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const argv = process.argv.slice(2)

function usage(error) {
  if (error) console.error(`check-client-secrets: ${error}`)
  console.error('usage: node scripts/check-client-secrets.mjs --slot <slug> [--json]')
  process.exit(2)
}

const slotIndex = argv.indexOf('--slot')
const slot = slotIndex !== -1 ? argv[slotIndex + 1] : null
const json = argv.includes('--json')

// Every argument that is not one of the two accepted options, and not the value that follows --slot,
// is a typo worth refusing: silently ignoring `--slots` would answer a question nobody asked.
const slotIndices = new Set(slotIndex === -1 ? [] : [slotIndex, slotIndex + 1])
const unknown = argv.filter((option, index) => !slotIndices.has(index) && option !== '--json')
if (unknown.length > 0) usage(`unknown argument(s): ${unknown.join(', ')}`)
if (!slot) usage('--slot is required')
if (slot.startsWith('--')) usage(`--slot needs a slot slug, got ${slot}`)

/**
 * Resolves a slot the same way `verify-slot.mjs` does: an exact directory name, or a unique `-<slug>`
 * suffix. Two matches is ambiguous and is refused rather than guessed.
 */
function resolveSlotDir(slug) {
  const appsDir = path.join(root, 'apps')
  if (!fs.existsSync(appsDir)) return null
  const matches = fs
    .readdirSync(appsDir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .filter((name) => name === slug || name.endsWith(`-${slug}`))
  return matches.length === 1 ? path.join(appsDir, matches[0]) : null
}

const slotDir = resolveSlotDir(slot)
if (!slotDir) usage(`slot not found or ambiguous under apps/: ${slot}`)

let result
try {
  result = scanSlot(slotDir)
} catch (error) {
  // An exception here means the slot could not be read at all. That is `UNVERIFIED` (2), never a pass
  // and never a verdict about the app's code.
  console.error(`check-client-secrets: ${error instanceof Error ? error.message : String(error)}`)
  console.log('VERDICT: UNVERIFIED')
  process.exit(2)
}

const exitCode = result.verdict === 'PASS' ? 0 : result.verdict === 'FAIL' ? 1 : 2
const relativeSlot = path.relative(root, slotDir).split(path.sep).join('/')

if (json) {
  console.log(
    JSON.stringify(
      {
        tool: 'check-client-secrets',
        slot,
        slotDir: relativeSlot,
        verdict: result.verdict,
        exitCode,
        scannedFiles: result.scannedFiles,
        rows: result.rows,
        findings: result.findings,
        exemptions: result.exemptions,
        notes: result.notes
      },
      null,
      2
    )
  )
} else {
  console.log(`client-secrets: ${relativeSlot}`)
  for (const row of result.rows) console.log(`  ${row.status.padEnd(11)} ${row.message}`)
  console.log(`\nVERDICT: ${result.verdict}`)
}
process.exit(exitCode)

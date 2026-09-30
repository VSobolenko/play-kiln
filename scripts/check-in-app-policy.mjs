#!/usr/bin/env node
/**
 * CLI for the in-app privacy-policy sensor. The logic lives in `scripts/lib/in-app-policy.mjs` so
 * `scripts/test-in-app-policy.mjs` can drive it against throwaway sandboxes: one implementation, two
 * callers, and a CLI failure and a test failure cannot drift apart.
 *
 * Usage:
 *   node scripts/check-in-app-policy.mjs --slot <slug>
 *
 * Exit codes, matching this repository's convention: 0 PASS, 1 FAIL, 2 UNVERIFIED (the slot is
 * missing, ambiguous, or the pack is unreadable). `2` is never a pass.
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { evaluateInAppPolicy } from './lib/in-app-policy.mjs'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const argv = process.argv.slice(2)
const slotIndex = argv.indexOf('--slot')
const slot = slotIndex !== -1 ? argv[slotIndex + 1] : null

function usage(error) {
  if (error) console.error(`check-in-app-policy: ${error}`)
  console.error('usage: node scripts/check-in-app-policy.mjs --slot <slug>')
  process.exit(2)
}

/** Resolves a slot the same way `verify-slot.mjs` does: an exact directory name, or a unique `-<slug>`
 *  suffix. Two matches is ambiguous and is refused rather than guessed. */
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

if (!slot) usage('--slot is required')
const slotDir = resolveSlotDir(slot)
if (!slotDir) usage(`slot not found or ambiguous under apps/: ${slot}`)

const { verdict, rows, notes } = evaluateInAppPolicy(slotDir, { root })
for (const row of rows) console.log(`  ${row.status.padEnd(11)} ${row.message}`)
for (const note of notes) console.log(`  ${'NOTE'.padEnd(11)} ${note}`)

process.exit(verdict === 'PASS' ? 0 : verdict === 'FAIL' ? 1 : 2)

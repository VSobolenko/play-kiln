#!/usr/bin/env node
/**
 * Record (or inspect) the owner's approval to release one slot.
 *
 *   node scripts/approve-release.mjs --slot <slug> --note "<the owner's own words>"
 *   node scripts/approve-release.mjs --check --slot <slug>
 *   node scripts/approve-release.mjs --revoke --slot <slug>
 *
 * `--check` is what every release-side script calls: it exits 0 only when a current
 * approval exists, and 2 with an actionable reason otherwise. The reason names which half of the
 * approval is stale — the app whose build inputs moved, or the authored store pack whose listing,
 * journey or identity changed — because the two are different documents and the reader has to
 * know which one to review again.
 *
 * The write path is a **human decision, not a build step**. Run it only when the owner has
 * explicitly approved the app in the current conversation, and pass their own words as
 * `--note`; the record exists so a later reader can see that a person decided, not that a
 * flag was set. Signing, uploading and publishing keep their own separate authorizations
 * (`.cursor/rules/authorization.mdc`).
 */

import { existsSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import {
  ROOT,
  ReleaseApprovalError,
  approvalPath,
  checkReleaseApproval,
  revokeReleaseApproval,
  slotSlug,
  writeReleaseApproval
} from './lib/release-approval.mjs'

const USAGE = `Usage:
  node scripts/approve-release.mjs --slot <slug> --note "<owner's words>"   record approval
  node scripts/approve-release.mjs --check --slot <slug>                    verify approval
  node scripts/approve-release.mjs --revoke --slot <slug>                   drop approval

Options:
  --slot <slug>      slot resolved beneath apps/
  --slot-dir <path>  explicit slot directory (used by the factory self-test)
  --note "<words>"   the owner's own approval words; required to record
  --check            verify only; never writes
  --revoke           delete the recorded approval
  --help             show this message

Recording approval is a human decision: run it only after the owner has explicitly
approved the app in the current conversation. It authorises the release pipeline
(build, signing, store assets, screenshots) and nothing else — uploading to Play and
publishing stay with the owner.`

function argument(name) {
  const index = process.argv.indexOf(name)
  return index === -1 ? null : process.argv[index + 1] || null
}

function line(status, message) {
  console.log(`  ${status.padEnd(11)} ${message}`)
}

function parseArgs() {
  const argv = process.argv.slice(2)
  if (argv.includes('--help') || argv.includes('-h')) return { help: true }
  const allowed = new Set(['--slot', '--slot-dir', '--note'])
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index]
    if (token === '--check' || token === '--revoke') continue
    if (!allowed.has(token)) throw new Error(`unknown argument ${token}`)
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) throw new Error(`${token} requires a value`)
    index += 1
  }
  return {
    help: false,
    check: argv.includes('--check'),
    revoke: argv.includes('--revoke'),
    note: argument('--note'),
    slot: argument('--slot'),
    slotDir: argument('--slot-dir')
  }
}

function listAppDirs() {
  const appsDir = path.join(ROOT, 'apps')
  if (!existsSync(appsDir)) return []
  return readdirSync(appsDir)
    .map((name) => path.join(appsDir, name))
    .filter((candidate) => {
      try {
        return statSync(candidate).isDirectory()
      } catch {
        return false
      }
    })
}

function resolveSlotDir({ slot, slotDir }) {
  if (slotDir) {
    const resolved = path.resolve(slotDir)
    if (!existsSync(resolved) || !statSync(resolved).isDirectory()) {
      throw new Error(`--slot-dir is not a directory: ${resolved}`)
    }
    return resolved
  }
  if (!slot) throw new Error('--slot <slug> or --slot-dir <path> is required')
  const matches = listAppDirs().filter((candidate) => {
    const name = path.basename(candidate)
    return name === slot || name.endsWith(`-${slot}`)
  })
  if (matches.length === 0) throw new Error(`slot not found under apps/: ${slot}`)
  if (matches.length > 1) {
    throw new Error(`slot is ambiguous: ${matches.map((item) => path.basename(item)).join(', ')}`)
  }
  return matches[0]
}

function runCheck(slotDir) {
  const result = checkReleaseApproval(slotDir)
  console.log('## PlayKiln release approval\n')
  if (!result.ok) {
    line('BLOCKED', result.reason)
    console.log('\nVERDICT: NOT APPROVED')
    process.exitCode = 2
    return
  }
  line('PASS', `${path.basename(slotDir)} is approved for release`)
  line('PASS', `owner's words: ${result.record.note}`)
  line('PASS', `approved at ${result.record.approvedAt}`)
  // The two halves are reported separately because they are separate documents the owner reviewed:
  // the app the build reads, and the authored pack Play is told about. A refusal names the half that
  // moved, so a reader can tell an edit to a Kotlin file from an edit to the listing.
  line('PASS', `app digest ${String(result.digest).slice(0, 12)}… over ${result.files} build input(s)`)
  line('PASS', `pack digest ${String(result.packDigest).slice(0, 12)}… over ${result.packFiles} authored input(s)`)
  console.log('\nVERDICT: APPROVED')
}

function runWrite(slotDir, note) {
  console.log('## PlayKiln release approval\n')
  const record = writeReleaseApproval(slotDir, { note })
  line('PASS', `recorded ${path.relative(ROOT, approvalPath(slotDir)).split(path.sep).join('/')}`)
  line('PASS', `owner's words: ${record.note}`)
  line('PASS', `bound to ${record.sourceFiles} build input(s), app digest ${record.sourceDigest.slice(0, 12)}…`)
  line(
    'PASS',
    `bound to ${record.packFiles} authored pack input(s) (${record.packInputs.join(', ') || 'none'}), ` +
      `pack digest ${String(record.packDigest).slice(0, 12)}…`
  )
  line('INFO', 'any edit to the app or to an authored store input voids this approval until the owner approves the new revision again')
  line('INFO', `release may now run: node scripts/prepare-release.mjs --slot ${slotSlug(slotDir)}`)
  line('INFO', 'signing, uploading and publishing keep their own authorizations')
  console.log('\nVERDICT: APPROVED — the release phase may proceed')
}

function runRevoke(slotDir) {
  console.log('## PlayKiln release approval\n')
  const removed = revokeReleaseApproval(slotDir)
  line(removed ? 'PASS' : 'INFO', removed ? 'approval removed' : 'no approval was recorded')
  console.log('\nVERDICT: NOT APPROVED')
}

try {
  const options = parseArgs()
  if (options.help) {
    console.log(USAGE)
    process.exit(0)
  }
  const slotDir = resolveSlotDir(options)
  if (options.revoke) {
    runRevoke(slotDir)
  } else if (options.check) {
    runCheck(slotDir)
  } else {
    runWrite(slotDir, options.note)
  }
} catch (error) {
  if (error instanceof ReleaseApprovalError) {
    line('BLOCKED', error.message)
  } else {
    line('BLOCKED', error.message)
  }
  console.log(`\n${USAGE}`)
  process.exitCode = 1
}

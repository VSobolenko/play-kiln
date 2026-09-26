/**
 * The release gate: development ends before the app is built, signed, or photographed.
 *
 * Why this exists
 * ---------------
 * The factory used to run one undivided pipeline — research, implement, build the signed
 * APK/AAB, compose every store asset, capture screenshots, verify — with no point at which
 * the owner could say "the app is right, ship it". The owner had to inspect the UI after
 * the expensive release work was already paid for, and a rejected UI meant the whole
 * release cost was spent on a revision nobody wanted. The split is therefore not a
 * documentation preference: release work is the slow, irreversible half (a signing key,
 * a permanent applicationId, a store pack), and it must not run until the owner has
 * personally approved the revision.
 *
 * The mechanics
 * -------------
 * Approval is a file in the slot (`release-approval.json`) that records the owner's own
 * words and a digest of every file that can change what the app *is*. The digest is
 * content-based, not commit-based, because the factory must work on a dirty tree and
 * because `git commit` needs its own authorization (`authorization.mdc`). Any edit to a
 * build input changes the digest, so a stale approval cannot authorise a revision the
 * owner never saw; restoring the approved bytes re-opens the gate, because the content is
 * identical to what was approved.
 *
 * Which paths are build inputs is decided here, once, and imported by every release-side
 * script. `store/`, `dist/`, `captures/`, `build/` and machine-local state are excluded by
 * design: the release pipeline itself writes them *after* approval, so including them
 * would make the pipeline invalidate the very approval it is acting on.
 */

import { createHash } from 'node:crypto'
import { existsSync, readFileSync, readdirSync, unlinkSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

/** Repository root, resolved from this file so the module works from any cwd. */
export const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')

export const APPROVAL_FILENAME = 'release-approval.json'
export const APPROVAL_SCHEMA_VERSION = 1

/**
 * Shared factory code a slot can pull into its build. A change here can change the built
 * bytes while the slot's own tree is untouched, so it is part of the digest. The path is
 * deliberately always included rather than conditionally on the slot's settings file: a
 * conditional would make the digest depend on a file it is also hashing.
 */
export const SHARED_BUILD_TREE = 'capabilities'

/** Directories that are outputs or machine state, never reviewed source — at any depth. */
const EXCLUDED_DIRECTORY_NAMES = new Set([
  '.git',
  'build',
  '.gradle',
  '.kotlin',
  '.idea',
  '.cxx',
  'node_modules'
])

/** Directories that are outputs only at the slot root; a source package may be named `store`. */
const EXCLUDED_ROOT_DIRECTORY_NAMES = new Set(['store', 'dist', 'captures', '.kiln'])

/**
 * Files that do not describe the app the owner reviews: machine-local state, secrets, and
 * generated tooling metadata. Listed by name because none of them are build inputs at any
 * depth, and `.env` variants are matched by prefix below.
 */
const EXCLUDED_FILE_NAMES = new Set([
  APPROVAL_FILENAME,
  '.kiln-state.json',
  'economics.json',
  'capabilities.json',
  'README.md',
  '.gitignore',
  '.gitattributes',
  'keystore.properties',
  'local.properties',
  '.env'
])

export class ReleaseApprovalError extends Error {}

/** The approval record path for a slot. */
export function approvalPath(slotDir) {
  return path.join(path.resolve(slotDir), APPROVAL_FILENAME)
}

/** The kebab slug of a slot, derived from its directory name (`apps/2026-09-26-thing` -> `thing`). */
export function slotSlug(slotDir) {
  return path.basename(path.resolve(slotDir)).replace(/^\d{4}-\d{2}-\d{2}-/, '')
}

function isExcludedFile(name) {
  return EXCLUDED_FILE_NAMES.has(name) || name.startsWith('.env.')
}

/**
 * Every file that is a build input under `root`, as forward-slash relative paths.
 *
 * Symbolic links are skipped, matching `verify-slot.mjs`: a link can point outside the tree,
 * and hashing through one would make the digest depend on something the approval does not
 * name.
 */
function collectBuildInputs(root) {
  const found = []
  const walk = (current, relative, depth) => {
    let entries
    try {
      entries = readdirSync(current, { withFileTypes: true })
    } catch {
      return
    }
    for (const entry of entries) {
      const entryRelative = relative ? `${relative}/${entry.name}` : entry.name
      if (entry.isSymbolicLink()) continue
      if (entry.isDirectory()) {
        if (EXCLUDED_DIRECTORY_NAMES.has(entry.name)) continue
        if (depth === 0 && EXCLUDED_ROOT_DIRECTORY_NAMES.has(entry.name)) continue
        walk(path.join(current, entry.name), entryRelative, depth + 1)
      } else if (entry.isFile()) {
        if (isExcludedFile(entry.name)) continue
        found.push(entryRelative)
      }
    }
  }
  walk(path.resolve(root), '', 0)
  return found.sort()
}

/**
 * The digest an approval is bound to: every build input in the slot, plus the shared
 * capability tree. Path and bytes are both hashed, so a rename is a change, and the sort
 * makes the value independent of directory-read order.
 */
export function releaseSourceDigest(slotDir, { root = ROOT } = {}) {
  const absoluteSlot = path.resolve(slotDir)
  const hash = createHash('sha256')
  let files = 0
  // `key` is what the digest names the file; it is separate from the path it is read from,
  // because the shared tree is keyed with its repo-relative prefix.
  const scope = (label, absoluteFile, key) => {
    hash.update(`${label}:${key}\0`).update(readFileSync(absoluteFile)).update('\0')
    files += 1
  }
  for (const relative of collectBuildInputs(absoluteSlot)) {
    scope('slot', path.join(absoluteSlot, relative), relative)
  }
  const shared = path.join(root, SHARED_BUILD_TREE)
  if (existsSync(shared)) {
    for (const relative of collectBuildInputs(shared)) {
      scope('shared', path.join(shared, relative), `${SHARED_BUILD_TREE}/${relative}`)
    }
  }
  return { digest: hash.digest('hex'), files }
}

function blocked(reason) {
  return { ok: false, reason }
}

/**
 * Whether `slotDir` may enter the release phase.
 *
 * Returns `{ ok: true, record, digest }` or `{ ok: false, reason }`. The reason is a full
 * sentence that names the exact command which records approval, because a refusal a reader
 * cannot act on is how the previous gate this file replaces became prose nobody followed.
 */
export function checkReleaseApproval(slotDir, { root = ROOT } = {}) {
  const absoluteSlot = path.resolve(slotDir)
  const slug = slotSlug(absoluteSlot)
  const recordCommand = `node scripts/approve-release.mjs --slot ${slug} --note "<the owner's own words>"`
  const file = approvalPath(absoluteSlot)

  if (!existsSync(file)) {
    return blocked(
      `${path.basename(absoluteSlot)} has no ${APPROVAL_FILENAME}, so the owner has not approved ` +
        'this app for release. The development phase ends with the app reviewable; building, ' +
        `signing, screenshots and the store pack all wait for an explicit approval: ${recordCommand}`
    )
  }

  let record
  try {
    record = JSON.parse(readFileSync(file, 'utf8'))
  } catch (error) {
    return blocked(`${APPROVAL_FILENAME} could not be read (${error.message}); re-record the approval with: ${recordCommand}`)
  }
  if (record.schemaVersion !== APPROVAL_SCHEMA_VERSION) {
    return blocked(
      `${APPROVAL_FILENAME} declares schemaVersion ${record.schemaVersion}, expected ` +
        `${APPROVAL_SCHEMA_VERSION}; re-record the approval with: ${recordCommand}`
    )
  }
  if (!String(record.note || '').trim() || !String(record.approvedAt || '').trim()) {
    return blocked(
      `${APPROVAL_FILENAME} records no approval words or timestamp, so it is an empty gesture rather ` +
        `than a decision; re-record it with: ${recordCommand}`
    )
  }

  const current = releaseSourceDigest(absoluteSlot, { root })
  if (record.sourceDigest !== current.digest) {
    return blocked(
      `the app changed after the owner approved it (${record.sourceFiles} source file(s) approved, ` +
        `${current.files} now), so the approved revision no longer exists. Re-approve the revision you ` +
        `actually reviewed: ${recordCommand}`
    )
  }

  return { ok: true, digest: current.digest, record }
}

/**
 * Record the owner's approval. The caller is responsible for having actually asked; the
 * required `note` is what makes the record evidence of a decision instead of a boolean an
 * agent could set on its own initiative.
 */
export function writeReleaseApproval(slotDir, { note, root = ROOT, now = new Date() } = {}) {
  const absoluteSlot = path.resolve(slotDir)
  const words = String(note ?? '').trim()
  if (words.length < 4) {
    throw new ReleaseApprovalError(
      'recording a release approval requires --note with the owner\'s own approval words; an ' +
        'approval with no words is not a decision anyone can audit'
    )
  }
  const source = releaseSourceDigest(absoluteSlot, { root })
  const record = {
    schemaVersion: APPROVAL_SCHEMA_VERSION,
    slot: slotSlug(absoluteSlot),
    approvedBy: 'owner',
    approvedAt: now.toISOString(),
    note: words,
    sourceDigest: source.digest,
    sourceFiles: source.files
  }
  writeFileSync(approvalPath(absoluteSlot), `${JSON.stringify(record, null, 2)}\n`, 'utf8')
  return record
}

/** Remove an approval. Always safe: it can only ever narrow what may run. */
export function revokeReleaseApproval(slotDir) {
  const file = approvalPath(slotDir)
  if (!existsSync(file)) return false
  unlinkSync(file)
  return true
}

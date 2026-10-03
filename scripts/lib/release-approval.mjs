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
 *
 * That exclusion left a hole of the same shape it was avoiding. `store/` is not all pipeline
 * output: `store/listing.json`, `store/journey.json` and `store/studio.json` are files a human
 * authors, and every public claim the pack makes is rendered from them, so an approval that did
 * not cover them approved an app while leaving the description of the app free to change. On
 * 2026-09-29 a review demonstrated it: approve a slot, rewrite the listing to describe a feature
 * the build does not have, rewrite the journey so the screenshots assert different readings, then
 * run `scripts/store/render.py` and `media-capture.mjs` — every gate still returned ok. The authored half of
 * the pack is therefore bound by a second digest over the authored files only, in
 * `scripts/lib/pack-approval.mjs`, and an approval records both halves; a record that carries only
 * the build half is a record from before this contract and is refused rather than read as complete.
 */

import { createHash } from 'node:crypto'
import { existsSync, readFileSync, readdirSync, unlinkSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { checkPackApproval, packDigest } from './pack-approval.mjs'

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
 * A store-side renderer is not automatically outside the approval: run it before a capture and it
 * will move this slot out from under a recorded approval.
 *
 * `store/` is excluded above because the release pipeline writes a pack *after* approval — but
 * `scripts/store-studio.py` renders the launcher mark into `store/assets/` (excluded, no effect on
 * the digest) **and** the adaptive-icon vectors into `app/src/main/res/**` (not excluded, compiled
 * into the APK). On 2026-09-29 that file was 172 insertions / 300 deletions away from the version
 * that rendered the committed assets, so those bytes are not reproducible from the working tree; the
 * next `python scripts/store-studio.py --slot <slug>` may replace them, and what a reader then sees is
 * not a renderer complaint but a gate refusing:
 *
 *   - `python scripts/store/validate.py --slot <slug>` -> `FAIL provenance.json - <file>
 *     does not match its recorded sha256 (<12 hex>…)`, one row per composed asset, because the images
 *     on disk were replaced and `store/provenance.json` still records the previous run's hashes;
 *   - `node scripts/approve-release.mjs --check --slot <slug>` -> `the app changed after the owner
 *     approved it (<n> source file(s) approved, <n> now)` if the vectors moved, because they are build
 *     inputs like any other resource. The two counts are equal there, and that is not a contradiction:
 *     the gate reports how many files it hashed, and a replacement changes one of them.
 *
 * Neither is a bug in the module: it is the approval doing its job. The order that avoids it is the
 * pipeline's own — approval, then `prepare-release.mjs`, then `media-capture.mjs` (which re-records
 * `store/provenance.json` for the build it photographed), and only then a studio re-render. A slot
 * that needs its vectors regenerated needs the owner's approval again, because the app changed.
 */

/**
 * The criterion an entry has to satisfy, the reasons it may be satisfied for, and the list — one
 * decision in three views, so the sentence a reader applies cannot contradict the names in force.
 *
 * It could, until 2026-09-29. The criterion was prose sitting above the list:
 *
 *   an entry may be excluded only if it cannot alter the built artifact or its signature.
 *
 * and two of the entries it sat above, `keystore.properties` and `local.properties`, are exactly
 * the files that choose the key the APK is signed with and the SDK it is compiled against. The
 * list was right and the sentence was wrong: what an approval is *about* is the revision the owner
 * reviewed, and the machine's own configuration is not part of it. Two ways to make them agree were
 * refused on the way: hashing the two files would have given one revision one digest per machine
 * and voided every recorded approval the moment it was checked on another one — a guarantee bought
 * with a gate that cannot be relied on — and deleting the two entries would have kept a sentence
 * true that was never the rule. So the criterion is no longer prose to be re-derived: it *is* the
 * closed set `EXCLUSION_REASONS`, `EXCLUDED_FILE_REASONS` maps every excluded name to one of them,
 * and `EXCLUSION_CRITERION` is built from the set, so the sentence and the reasons the list uses
 * cannot drift apart. `scripts/selftest.mjs` used to fail a name whose reason is not in the set, a
 * reason no name uses, and a criterion that does not name every reason — the checks that would have
 * caught the contradiction above. That suite went with the meta-runner chain on 2026-09-30 and those
 * three assertions have **no successor**: they are recorded as a finding in
 * `openspec/changes/reduce-factory-to-core/deletions.md` §7, because the set is now unguarded and a
 * reader must not take this paragraph for a check that runs.
 *
 * A name list is still the mechanism: a rule keyed on `.gitignore` would exempt whatever a slot
 * happens to ignore next, and this list is the one place the exemption is decided. What changed is
 * that an entry now has to name *which reason* it is exempt for, and the reasons are about different
 * things — so a future reader adding a name argues against the reason, not against the list.
 */
export const EXCLUSION_REASONS = {
  'machine-local':
    "it is the machine's own configuration rather than the app's source, so its bytes differ between two machines that build the same revision (the SDK root, the path to the upload key, the slot's secrets), and hashing it would give one revision one digest per machine and leave a recorded approval unusable on the machine that did not record it. What it can change is not what the owner approved: the app's source is, and both `sign-release.mjs` and `prepare-release.mjs` read this configuration after approval — `prepare-release.mjs` verifies the built APK's certificate against the configured upload certificate and records `signing.fingerprint` in `dist/release-manifest.json`, so the signature is asserted against the artifact rather than against a file in the digest",
  'not-read-by-the-build':
    'no task of the build opens it, so it cannot change the compiled bytes or what signs them; the factory scripts that do read it decide a plan or render a document, and those run outside Gradle',
  'the-record-itself':
    'it is a record that is written and then checked — `release-approval.json` for the approval, `device-verification.json` for the on-device run — and hashing it would make writing the record change the digest that record states'
}

export const EXCLUSION_CRITERION =
  'an entry may be excluded only for one of ' +
  `${Object.keys(EXCLUSION_REASONS).map((reason) => `\`${reason}\``).join(', ')}; each is stated in ` +
  '`EXCLUSION_REASONS` (`scripts/lib/release-approval.mjs`) and every excluded name carries one'

/** Which reason each excluded name is excluded for: the list, keyed by name. */
export const EXCLUDED_FILE_REASONS = new Map([
  [APPROVAL_FILENAME, 'the-record-itself'],
  // The on-device run record is excluded for the same reason as the approval: `slot.device` checks it,
  // and a record that stores the revision's `buildDigest` must not be part of the digest it stores.
  // It is named `RECORD_NAME` in `scripts/core/device.mjs`; there is no cycle to import it from here,
  // because `device.mjs` imports this module for `releaseSourceDigest`.
  ['device-verification.json', 'the-record-itself'],
  ['.kiln-state.json', 'machine-local'],
  // The SDK root and the upload key's path and password: this machine's, not the app's.
  ['local.properties', 'machine-local'],
  ['keystore.properties', 'machine-local'],
  ['.env', 'machine-local'],
  // The slot's plan and its capability registry are read by the economics gate and by the store
  // pack, and by no Gradle task: the build's own capability list is compiled into the slot's
  // Gradle files when the slot is scaffolded, not re-read from this file at build time.
  ['economics.json', 'not-read-by-the-build'],
  ['capabilities.json', 'not-read-by-the-build'],
  // Documents and VCS configuration: an APK does not depend on any of them.
  ['README.md', 'not-read-by-the-build'],
  ['.gitignore', 'not-read-by-the-build'],
  ['.gitattributes', 'not-read-by-the-build'],
  // The batch of Play Console promo codes (`Monetize with Play -> Promotions -> Download codes`)
  // minted for a reviewer, written beside the slot by the factory's promotions step and gitignored
  // so a free unlock of what the slot sells never enters a commit — the capability never reads it,
  // Play Billing takes its product IDs from the slot's Kotlin and the console, and no build task
  // opens it. Counting it as a build input made the factory's own console step void the approval for
  // the revision the owner had just reviewed: `--check` reported `236 now` against `235 approved`
  // while the two trees differed by this one file. Both spellings are listed because the console
  // exports `promotion_codes.csv` while the factory's notes call it `promo-codes.csv`, and the
  // scaffold and root `.gitignore` cover the same two names.
  ['promotion_codes.csv', 'not-read-by-the-build'],
  ['promo-codes.csv', 'not-read-by-the-build']
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

/**
 * The one sentence that tells a reader how to record an approval.
 *
 * It exists as a function rather than as a string built at each refusal so the command an operator
 * is told to run is identical in `approve-release.mjs`, `prepare-release.mjs`, `media-capture.mjs`,
 * `scripts/store/render.py` and `play-upload.mjs`. The scripts that only print `checkReleaseApproval`'s reason
 * already share it — this is the same sentence, exported so the uploader does not restate it.
 */
export function approvalRemediation(slotDir) {
  return `node scripts/approve-release.mjs --slot ${slotSlug(slotDir)} --note "<the owner's own words>"`
}

/**
 * Why a file is excluded from the digest, or `undefined` when it is a build input.
 *
 * The prefix rule for `.env` variants is stated here rather than in a second list so the reason a
 * variant is excluded is the same object as the reason `.env` itself is: `exclusionReason('.env.local')`
 * returns `machine-local` because `.env` does, and a reader asking why gets the same answer.
 */
export function exclusionReason(name) {
  if (EXCLUDED_FILE_REASONS.has(name)) return EXCLUDED_FILE_REASONS.get(name)
  if (name.startsWith('.env.')) return EXCLUDED_FILE_REASONS.get('.env')
  return undefined
}

function isExcludedFile(name) {
  return exclusionReason(name) !== undefined
}

/**
 * Every file that is a build input under `root`, as forward-slash relative paths.
 *
 * Symbolic links are skipped, matching `verify.mjs`: a link can point outside the tree,
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
 * A fresh SHA-256 for a file-set digest, and the one line that folds a file into it.
 *
 * Both are exported because a second digest has to exist: the approval now covers the *authored*
 * store pack as well as the build inputs (`scripts/lib/pack-approval.mjs`), and two digests over
 * two path sets must still hash a byte the same way. A copy of `hash.update(...)` at the second
 * call site is exactly how the two would drift, and the drift would be invisible — both keep
 * returning a digest, and only a re-approval ever shows that one of them stopped describing its
 * own inputs. `key` is what the digest names the file by, deliberately separate from the path it
 * is read from, because the shared build tree is keyed with its repo-relative prefix.
 */
export function newFileDigest() {
  return createHash('sha256')
}

export function absorbFile(hash, label, key, absoluteFile) {
  hash.update(`${label}:${key}\0`).update(readFileSync(absoluteFile)).update('\0')
}

/** The forward-slash relative path a digest names a file by, so a key can never carry a `\`. */
export function digestKey(root, absoluteFile) {
  return path.relative(path.resolve(root), path.resolve(absoluteFile)).split(path.sep).join('/')
}

/**
 * The digest an approval is bound to: every build input in the slot, plus the shared
 * capability tree. Path and bytes are both hashed, so a rename is a change, and the sort
 * makes the value independent of directory-read order.
 */
export function releaseSourceDigest(slotDir, { root = ROOT } = {}) {
  const absoluteSlot = path.resolve(slotDir)
  const hash = newFileDigest()
  let files = 0
  const scope = (label, absoluteFile, key) => {
    absorbFile(hash, label, key, absoluteFile)
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
 * Returns `{ ok: true, record, digest, files, packDigest, packFiles }` or `{ ok: false, reason }`.
 * The reason is a full sentence that names which half of the approval is stale and the exact
 * command that records approval, because a refusal a reader cannot act on is how the previous gate
 * this file replaces became prose nobody followed.
 */
export function checkReleaseApproval(slotDir, { root = ROOT } = {}) {
  const absoluteSlot = path.resolve(slotDir)
  const recordCommand = approvalRemediation(absoluteSlot)
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
  // Two halves, two reasons. The approval is about the revision the owner reviewed, and that
  // revision is two documents: the app (what the build reads) and the authored store pack (what
  // Play is told). They move independently — a Kotlin edit does not touch the listing, and a
  // listing edit does not touch a build input — so a refusal names the half that moved instead of
  // the word "changed", and the reader knows which document to bring back for review.
  const staleHalves = []
  if (record.sourceDigest !== current.digest) {
    staleHalves.push(
      `the app changed after the owner approved it (${record.sourceFiles} source file(s) approved, ` +
        `${current.files} now)`
    )
  }
  const pack = checkPackApproval(absoluteSlot, { record })
  if (!pack.ok) staleHalves.push(pack.reason)
  if (staleHalves.length > 0) {
    return blocked(
      `${staleHalves.join('; ')}. Re-approve the revision you actually reviewed: ${recordCommand}`
    )
  }

  return {
    ok: true,
    digest: current.digest,
    files: current.files,
    packDigest: pack.digest,
    packFiles: pack.files,
    record
  }
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
  const pack = packDigest(absoluteSlot)
  const record = {
    schemaVersion: APPROVAL_SCHEMA_VERSION,
    slot: slotSlug(absoluteSlot),
    approvedBy: 'owner',
    approvedAt: now.toISOString(),
    note: words,
    sourceDigest: source.digest,
    sourceFiles: source.files,
    packDigest: pack.digest,
    packFiles: pack.files,
    packInputs: pack.inputs
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

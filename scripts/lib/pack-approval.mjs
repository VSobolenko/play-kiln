/**
 * The pack half of the release approval: the store files a human *authors*.
 *
 * Why a second digest exists
 * --------------------------
 * `release-approval.mjs` binds the owner's approval to every file a build reads, and it excludes
 * `store/` on purpose: the release pipeline writes a pack *after* approval, so a digest that
 * included the pipeline's own output would void the very approval it is acting on. The exclusion is
 * right for that output and wrong for the files beside it. `store/listing.json` is the public prose,
 * `store/journey.json` is the claim every screenshot asserts, `store/studio.json` is the mark,
 * palette and headlines the assets are rendered from, and `store/review-notes.md` is what a reviewer
 * is told about access and unfinished work. None of those is pipeline output; all of them are inputs
 * a human authors, and the pack that reaches Play is rendered from them.
 *
 * The hole that made this file necessary was demonstrated on 2026-09-29: approve a slot, rewrite
 * `store/listing.json` to describe a feature the build does not have and `store/journey.json` to
 * photograph different readings, then run `scripts/store/render.py` and `media-capture.mjs`. Every gate
 * returned ok — the one artifact nobody re-approved being the one that carries every public claim.
 *
 * Which files are in it
 * ---------------------
 * The closed list is `AUTHORED_PACK_INPUTS`, and the digest is over nothing else. That boundary is
 * the whole difficulty of this module, because the *other* half of `store/` is written by the
 * pipeline: `store/assets/**`, `store/provenance.json`, `store/checklist.md`, `store/graphics.md`
 * and `store/release-notes.md` are produced by
 * `scripts/store/render.py` and `media-capture.mjs` during the release run itself. A digest that covered them
 * would refuse every release for a pack nobody changed — the trap `release-approval.mjs` documents
 * for the build half, repeated here — so `scripts/test-pack-approval.mjs` asserts the boundary from
 * both sides: editing an authored input voids the approval, and rewriting every pipeline-owned file
 * does not move the digest at all.
 */
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'

import {
  APPROVAL_FILENAME,
  absorbFile,
  approvalPath,
  digestKey,
  newFileDigest
} from './release-approval.mjs'

/**
 * The authored store inputs, and the only files this digest covers.
 *
 * Each entry is here because a human decision lives in it and the pack that reaches Play is
 * rendered from it. `store/review-notes.md` is conditional on the file existing, because a pack is
 * developed before every document is present and a missing authored input is represented by its
 * absence in the digest rather than by an invented empty one — the list of what was covered is
 * part of the digest's return value so a reader can see which of the four it hashed.
 */
export const AUTHORED_PACK_INPUTS = Object.freeze([
  'store/listing.json',
  'store/journey.json',
  'store/studio.json',
  'store/review-notes.md'
])

/** The record field that carries the pack digest, named once so a message cannot drift from it. */
export const PACK_DIGEST_FIELD = 'packDigest'
/** The record field that carries how many authored inputs the pack digest covered. */
export const PACK_FILES_FIELD = 'packFiles'

/**
 * The digest over a slot's authored pack inputs, plus the list it covered.
 *
 * Content-based for the same reason the build digest is: the factory works on a dirty tree, so the
 * value has to describe the bytes on disk rather than a commit, and restoring the approved bytes
 * has to re-open the gate. Hashed through the helpers in `release-approval.mjs` so a byte folds
 * into both digests the same way.
 */
export function packDigest(slotDir) {
  const absoluteSlot = path.resolve(slotDir)
  const hash = newFileDigest()
  const inputs = []
  for (const relative of AUTHORED_PACK_INPUTS) {
    const file = path.join(absoluteSlot, ...relative.split('/'))
    if (!existsSync(file)) continue
    absorbFile(hash, 'pack', digestKey(absoluteSlot, file), file)
    inputs.push(relative)
  }
  return { digest: hash.digest('hex'), files: inputs.length, inputs }
}

/**
 * Whether the recorded approval still covers the slot's authored pack.
 *
 * Returns `{ ok: true, digest, files, inputs }` or `{ ok: false, reason }`. The reason is built
 * from the check rather than written as a sentence about it, so it cannot describe a stale pack
 * while the code compares something else. `record` may be passed by a caller that already read the
 * approval file — `checkReleaseApproval` has — and is otherwise read from the slot, so this function
 * answers for a slot directory on its own.
 *
 * A record with no pack digest is refused, not accepted as "no pack to check". Such a record was
 * written before the authored pack was part of the contract, so the owner approved a revision whose
 * listing, journey and identity were still free to change; reading that as complete would be the
 * silent downgrade this module exists to prevent. Re-recording costs the owner one approval.
 */
export function checkPackApproval(slotDir, { record } = {}) {
  const absoluteSlot = path.resolve(slotDir)
  let approved = record
  if (!approved) {
    const file = approvalPath(absoluteSlot)
    if (!existsSync(file)) {
      return {
        ok: false,
        reason: `${path.basename(absoluteSlot)} has no ${APPROVAL_FILENAME}, so no ${PACK_DIGEST_FIELD} was recorded`
      }
    }
    try {
      approved = JSON.parse(readFileSync(file, 'utf8'))
    } catch (error) {
      return { ok: false, reason: `${APPROVAL_FILENAME} could not be read (${error.message})` }
    }
  }

  const recorded = approved?.[PACK_DIGEST_FIELD]
  if (typeof recorded !== 'string' || recorded.trim() === '') {
    return {
      ok: false,
      reason:
        `${APPROVAL_FILENAME} records no ${PACK_DIGEST_FIELD}, so it was written before the authored ` +
        'store pack was part of the approval and never bound the listing, the journey or the identity'
    }
  }

  const current = packDigest(absoluteSlot)
  if (recorded !== current.digest) {
    return {
      ok: false,
      reason:
        `the store pack changed after the owner approved it (${approved?.[PACK_FILES_FIELD]} pack ` +
        `input(s) approved, ${current.files} now)`
    }
  }
  return { ok: true, digest: current.digest, files: current.files, inputs: current.inputs }
}

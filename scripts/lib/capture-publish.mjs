/**
 * Transactional publication of a verified capture set.
 *
 * Lives apart from `media-capture.mjs` so the failure paths can be driven directly by
 * `scripts/test-capture-publish.mjs`. The interesting behaviour here is what the filesystem looks like
 * after an interruption, which cannot be tested by running the capture tool: that needs a device, and
 * it cannot be made to die between two renames on purpose.
 *
 * The guarantee: `raw/` always holds either the previous complete set or the new complete set. It is
 * never a mixture, never empty because of a failure, and stale files from an earlier run never survive.
 *
 * Two distinct failure modes need two distinct mechanisms, and using only one of them was the bug:
 *
 *  - a *thrown* error is handled by the rollback in `publishCaptures`;
 *  - a *killed* process runs no code at all, so the repair has to happen on the next start, in
 *    `recoverInterruptedPublish`.
 */
import { existsSync, readdirSync, renameSync, rmSync } from 'node:fs'

/**
 * Repair a publish that was killed between its two renames.
 *
 * Beware the ordering, which is load-bearing in two places:
 *
 *  - the backup must not be deleted before checking whether `raw/` is missing, or the only surviving
 *    copy of a good capture set is destroyed by the run that was meant to recover it;
 *  - this must run before anything recreates `raw/`, because an empty `raw/` is indistinguishable from
 *    a set that survived a completed publish, and the backup would be discarded as a leftover.
 *
 * Returns true when a previous run's capture set had to be restored.
 */
export function recoverInterruptedPublish(rawDir) {
  const backupDir = `${rawDir}.previous`
  if (!existsSync(backupDir)) return false
  if (existsSync(rawDir)) {
    // `raw/` survived, so the backup is a leftover from a publish that completed but died before its
    // cleanup. Only now is discarding it safe.
    rmSync(backupDir, { recursive: true, force: true })
    return false
  }
  renameSync(backupDir, rawDir)
  return true
}

/**
 * Replace `rawDir` with `stageDir` as a whole directory.
 *
 * Renaming file by file into an existing `captures/raw/` means an interruption leaves a mixture of two
 * runs, and files a previous run left behind (an old `07.png`, an old dump) survive into the new set.
 * Replacing the directory makes both impossible: nothing is carried over, so nothing stale can be.
 *
 * Returns `{ published, recovered }`.
 */
export function publishCaptures(stageDir, rawDir) {
  const names = readdirSync(stageDir).filter((name) => name.endsWith('.png'))
  const backupDir = `${rawDir}.previous`
  const recovered = recoverInterruptedPublish(rawDir)
  const hadPrevious = existsSync(rawDir)
  if (hadPrevious) renameSync(rawDir, backupDir)
  try {
    renameSync(stageDir, rawDir)
  } catch (error) {
    // A failed swap must leave the previous good set, not an absent or empty directory.
    if (hadPrevious && existsSync(backupDir) && !existsSync(rawDir)) renameSync(backupDir, rawDir)
    throw error
  }
  rmSync(backupDir, { recursive: true, force: true })
  return { published: names.length, recovered }
}

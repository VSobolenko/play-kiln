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
import { existsSync, mkdirSync, readFileSync, readdirSync, renameSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'

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

/**
 * How long a lock whose owner is dead is left alone before it may be reclaimed.
 *
 * Generous on purpose: a capture run can spend minutes on the device, and reclaiming a lock that a
 * live run is still using would let two runs publish at once - the exact outcome this lock exists to
 * prevent. Waiting an extra half hour after a genuine crash costs an operator nothing; stealing a
 * live lock corrupts a store pack.
 */
export const DEFAULT_LOCK_STALE_MS = 30 * 60 * 1000

/**
 * Whether a pid names a process this machine still has. Signal 0 tests existence, not ownership.
 *
 * Deliberately local rather than imported from `codex-ledger.mjs`, whose same-named helper answers the
 * same question for worker liveness: the two share the "only `ESRCH` proves death" rule and nothing
 * else, and a lock's staleness policy must not move when the worker ledger's does. If a third caller
 * appears they belong in one small `scripts/lib/` module; the duplication is flagged, not hidden.
 */
function isProcessAlive(pid) {
  try {
    process.kill(pid, 0)
    return true
  } catch (error) {
    // `ESRCH` is the only answer that means "no such process". `EPERM` means it exists but belongs to
    // another user, and any other failure is unknown; both count as alive so a live holder is never
    // displaced. Preferring a false "alive" over a false "dead" is the whole staleness policy.
    return error.code !== 'ESRCH'
  }
}

/** Read the owner record, or null when the file is missing, empty, or not a valid record. */
function readLockOwner(lockPath) {
  try {
    const owner = JSON.parse(readFileSync(lockPath, 'utf8'))
    if (!owner || !Number.isInteger(owner.pid) || owner.pid <= 0) return null
    return owner
  } catch {
    return null
  }
}

/**
 * Whether a lock may be reclaimed. Deliberately conservative: an unreadable owner record, a pid that
 * is still alive, or a lock younger than the threshold are all "not stale". The cost of that choice is
 * a manual cleanup after a crash that happened moments ago; the cost of the opposite is two runs
 * publishing at once, which is silent and unrecoverable in the committed pack.
 */
function isLockStale(owner, staleAfterMs) {
  if (!owner) return false
  if (isProcessAlive(owner.pid)) return false
  const startedAt = Number(owner.startedAt)
  if (!Number.isFinite(startedAt)) return false
  return Date.now() - startedAt > staleAfterMs
}

function lockHeldError(lockPath, slot, owner) {
  const holder = owner?.pid ? `pid ${owner.pid}` : 'an unreadable owner record'
  const since = owner?.startedAt ? ` since ${new Date(owner.startedAt).toISOString()}` : ''
  const error = new Error(
    `capture lock for slot '${slot ?? '?'}' is held by ${holder}${since}; another media-capture run ` +
    `is publishing to this slot. Wait for it to finish, or delete ${lockPath} once you are certain ` +
    'that process is gone.'
  )
  error.code = 'CAPTURE_LOCK_HELD'
  error.holder = owner ?? null
  return error
}

/** A handle whose idempotent `release` removes the lock file only while this process still owns it. */
function captureLock(lockPath, record) {
  let released = false
  return {
    path: lockPath,
    pid: record.pid,
    release() {
      if (released) return
      released = true
      const owner = readLockOwner(lockPath)
      // Another run may have reclaimed this lock after a threshold-length pause, and deleting *its*
      // lock would hand the slot to two writers. Only an owner record naming this process - or a file
      // this process cannot parse, which a live owner is never allowed to leave behind - is removed.
      // A file already gone is fine, which is what makes `block()`'s exit path safe to double-handle.
      if (owner && owner.pid !== record.pid) return
      rmSync(lockPath, { force: true })
    }
  }
}

/**
 * Take the slot's capture lock, or throw if another live run holds it.
 *
 * WHY this exists: two concurrent `media-capture.mjs` runs on one slot share one `staging/` and one
 * `raw/`. Process B's `recoverInterruptedPublish` can consume A's `raw.previous/` while A is between
 * the two renames of `publishCaptures`, and both wipe and rewrite the single `staging/`, so a
 * published `raw/` can become a mixture of two journeys - the capture-time form of the mixed-pack
 * failure the studio already refuses at composition. It also closes the operator window after a
 * mid-swap kill: while `raw/` is absent, running `scripts/store/render.py` directly would re-render synthetic
 * screens over the committed assets, and a lock makes re-running the capture the only action that can
 * proceed.
 *
 * The primitive is `writeFileSync` with the `wx` flag (O_CREAT|O_EXCL): creation fails with `EEXIST`
 * when the file already exists, and the filesystem guarantees exactly one creator, which is the whole
 * mutual exclusion. `mkdirSync` is the other portable atomic primitive and would be equally correct,
 * but a file carries the owner record without a second path, so `wx` is the smaller of the two. No
 * `flock`, no external dependency, and it behaves the same on Windows and Linux.
 *
 * Returns a handle; the caller must `release()` it on every exit path.
 */
export function acquireCaptureLock(lockPath, options = {}) {
  const slot = options.slot ?? null
  const staleAfterMs = Number.isFinite(options.staleAfterMs) ? options.staleAfterMs : DEFAULT_LOCK_STALE_MS
  mkdirSync(path.dirname(lockPath), { recursive: true })
  const record = { pid: process.pid, startedAt: Date.now(), slot }
  for (let attempt = 0; attempt < 3; attempt += 1) {
    try {
      writeFileSync(lockPath, `${JSON.stringify(record)}\n`, { encoding: 'utf8', flag: 'wx' })
      return captureLock(lockPath, record)
    } catch (error) {
      if (error.code !== 'EEXIST') throw error
    }
    const owner = readLockOwner(lockPath)
    if (!isLockStale(owner, staleAfterMs)) throw lockHeldError(lockPath, slot, owner)
    // Reclaim by renaming the stale file aside, never by deleting it in place: two reclaimers that both
    // judged it stale would otherwise both delete, and the second delete could remove the first
    // reclaimer's fresh lock. `renameSync` moves the one file exactly once, so only one reclaimer wins;
    // the loser loops, sees the winner's live lock, and refuses rather than racing it.
    const graveyard = `${lockPath}.reclaimed-${process.pid}-${Date.now()}`
    try {
      renameSync(lockPath, graveyard)
    } catch {
      // Another reclaimer won the rename. Loop and let the `wx` create decide the single winner.
    }
    rmSync(graveyard, { force: true })
  }
  // Bounded attempts exhausted without acquiring: report the current holder instead of looping forever.
  throw lockHeldError(lockPath, slot, readLockOwner(lockPath))
}

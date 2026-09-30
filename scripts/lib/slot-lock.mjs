#!/usr/bin/env node
/**
 * A per-slot lease: one consumer per slot directory, whichever consumer it is.
 *
 * WHY this exists — the recorded failure, not a theory
 * ----------------------------------------------------
 * The factory had locks for exactly one thing: media capture (`capture-publish.mjs`). Gradle, the
 * verification build, release preparation and a Codex worker mutating a slot had none. The cost is
 * already on record (`harness/lessons.md`, 2026-09-26, and `.cursor/rules/build-speed.mdc` §"Multiple
 * Gradle processes must not share one slot"): four Codex workers in one session died with
 * `java.io.FileNotFoundException: ...gradle-9.7.1-bin.zip.lck (Access denied)` while the orchestrator
 * also had a Gradle run live against the same slot. The wrapper's distribution lock lives in
 * `GRADLE_USER_HOME`, outside both the slot and the workspace, so it is not shareable at all: the
 * second process is denied rather than queued, and it dies **mid-write**. One worker left a Kotlin
 * file with three return-type errors against its own interface; another left three files that never
 * existed while its ledger label read `BLOCKED(RATE_LIMITED)`. The tree, not the label, was what told
 * a human what had happened.
 *
 * The contention is a *same-slot* contention, so the fix is a *slot-scoped* lease: the second
 * consumer waits for the first, or is refused with an explicit error naming the holder — never a
 * wrapper `.lck` crash that destroys a half-written tree. Two different slots are different lock
 * files by construction, so a build on one slot can never block a build on another.
 *
 * The primitive is `openSync(path, 'wx')` (O_CREAT|O_EXCL): creation fails with `EEXIST` when the
 * file exists, and the filesystem guarantees exactly one creator, which is the whole mutual
 * exclusion. No `flock`, no dependency, and it behaves the same on Windows and Linux. The lock file
 * lives under the slot's ignored state directory (`<slot>/.kiln/slot.lock`) because the repository's
 * `.gitignore` ignores `.kiln/` at any depth, and because the slot's `store/` is a validated artifact
 * tree that must not gain a file nothing validates.
 *
 * The lease is deliberately *advisory*: it protects only consumers that ask for it. That is why the
 * single Gradle route `runGradleTasks()` takes it — one place covers every Gradle consumer, and a
 * consumer that bypasses that route bypasses the lock too.
 */
import { closeSync, mkdirSync, openSync, readFileSync, rmSync, writeSync } from 'node:fs'
import { hostname } from 'node:os'
import path from 'node:path'
import process from 'node:process'

/** How long a second consumer waits for the holder before refusing, by default (two minutes). */
export const DEFAULT_SLOT_LOCK_TIMEOUT_MS = 120_000

/** How long a waiting consumer sleeps between attempts. A build lasts minutes; this is noise. */
export const DEFAULT_SLOT_LOCK_POLL_MS = 200

/** The exit hatch's variable name. `off` is the only value that skips the lock. */
export const SLOT_LOCK_ENV = 'PLAYKILN_SLOT_LOCK'

/** The one value of `SLOT_LOCK_ENV` that skips acquisition. */
export const SLOT_LOCK_ENV_OFF = 'off'

/** Stable `code` on the refusal, so a caller can catch it specifically instead of matching text. */
export const SLOT_LOCK_HELD_CODE = 'SLOT_LOCK_HELD'

/** Ignored state directory the lock lives in, relative to the slot root. */
export const SLOT_LOCK_DIR = '.kiln'

/** Lock file name inside that directory. */
export const SLOT_LOCK_FILE = 'slot.lock'

/**
 * The refusal a second consumer of a held slot receives.
 *
 * Exported as a class *and* carrying `code = 'SLOT_LOCK_HELD'`, because a caller that has to
 * distinguish "someone else is building" from "the build failed" must not do it by matching prose.
 * `holder` is the owner record on disk (or null when it could not be parsed), so a caller can print
 * the pid, purpose and age without re-reading the file.
 */
export class SlotLockHeldError extends Error {
  constructor(message, { lockPath, holder, waitedMs, timeoutMs } = {}) {
    super(message)
    this.name = 'SlotLockHeldError'
    this.code = SLOT_LOCK_HELD_CODE
    this.lockPath = lockPath ?? null
    this.holder = holder ?? null
    this.waitedMs = waitedMs ?? null
    this.timeoutMs = timeoutMs ?? null
  }
}

/**
 * Whether the process at `pid` still exists — a heuristic, and here is exactly how it lies.
 *
 * `process.kill(pid, 0)` sends no signal; it asks the OS whether the pid can be signalled. It throws
 * `ESRCH` only when the process is gone, and `EPERM` when the process exists but belongs to another
 * user. **Only `ESRCH` proves death**; every other answer counts as alive, so a live holder is never
 * displaced. Pids are recycled, so a dead holder's pid can later belong to an unrelated live
 * process — which reads as alive and therefore *conservative*: the lock waits for a human instead of
 * being stolen. `scripts/lib/codex-ledger.mjs` records the same rule for worker liveness; the
 * duplication is deliberate (a lock's staleness policy must not move when the ledger's does) and is
 * the same call `capture-publish.mjs` makes for its own lock.
 *
 * @param {number} pid
 * @param {(pid: number, signal: number) => void} [kill] Injectable for tests; defaults to `process.kill`.
 * @returns {boolean}
 */
export function isProcessAlive(pid, kill = process.kill) {
  const numeric = Number(pid)
  if (!Number.isInteger(numeric) || numeric <= 0) return false
  try {
    kill(numeric, 0)
    return true
  } catch (error) {
    return error?.code !== 'ESRCH'
  }
}

/** Path of a slot's lock file. Derived from the slot directory, so two slots are never one lock. */
export function slotLockPath(slotDir) {
  if (!slotDir) throw new Error('slotLockPath needs a slot directory')
  return path.join(slotDir, SLOT_LOCK_DIR, SLOT_LOCK_FILE)
}

/**
 * Whether the lock is switched off by the escape hatch.
 *
 * @param {Record<string, string|undefined>} [env] Injectable so a fixture need not mutate `process.env`.
 * @returns {boolean}
 */
export function slotLockDisabled(env = process.env) {
  return String(env?.[SLOT_LOCK_ENV] ?? '').trim().toLowerCase() === SLOT_LOCK_ENV_OFF
}

/**
 * Read the current holder without touching the file — the read half of "a second consumer must be
 * able to name the holder". Returns null when the file is absent, empty, or not a valid record.
 *
 * @param {string} slotDir
 * @param {{ lockPath?: string }} [options]
 * @returns {object|null}
 */
export function slotLockHolder(slotDir, options = {}) {
  return readLockRecord(options.lockPath ?? slotLockPath(slotDir))
}

/**
 * Take the slot's lease, waiting up to `timeoutMs` for a live holder, or throw `SlotLockHeldError`.
 *
 * The wait is bounded on purpose. The recorded alternative is not a queue but a crash, so the second
 * consumer's worst case must be a refusal it can read and act on. On expiry the error names the
 * holder (pid, age, purpose, command, host) and states the remedy, which is what turns "Access
 * denied" into a diagnosis.
 *
 * Stale-lock reclaim never creates two owners: a lock is reclaimed only when its recorded pid is
 * **definitely not alive** (see `isProcessAlive`), and the reclaim itself is serialised by an
 * exclusive-create takeover file, so two consumers that both judge the same lock stale cannot both
 * delete it and both create their own. A record written on another host is never reclaimed, because
 * a foreign pid says nothing about a process on this machine.
 *
 * @param {string} slotDir                The slot root; the lock path is derived from it.
 * @param {object} [options]
 * @param {number} [options.timeoutMs]    How long to wait before refusing. Default 120000.
 * @param {number} [options.pollMs]       Sleep between attempts. Default 200.
 * @param {string} [options.purpose]      Human-readable reason, recorded for the next consumer.
 * @param {string} [options.command]      The command that took the lease; defaults to this argv.
 * @param {string} [options.lockPath]     Override the derived path (fixtures only).
 * @param {Record<string,string|undefined>} [options.env] Env used for the escape hatch check.
 * @param {(ms: number) => void} [options.sleep] Injectable wait, for tests.
 * @param {() => number} [options.now]    Injectable clock, for tests.
 * @param {(pid: number, signal: number) => void} [options.kill] Injectable liveness probe, for tests.
 * @returns {{ path: string, pid: number|null, purpose: string, acquired: boolean, release: () => void }}
 *
 * @remarks
 * `PLAYKILN_SLOT_LOCK=off` skips acquisition entirely and returns an unheld handle. That is a
 * **hazard**, not a feature: it exists so a fixture can exercise a caller without a lease, it is
 * never a way for a caller to dodge contention, and a workaround built on it silently restores the
 * four-dead-workers failure this module exists to prevent. Nothing else skips the lease — not a
 * short timeout, not a missing purpose, not the absence of a `.kiln/` directory.
 */
export function acquireSlotLock(slotDir, options = {}) {
  if (!slotDir) throw new Error('acquireSlotLock needs a slot directory')
  const lockPath = options.lockPath ?? slotLockPath(slotDir)
  const env = options.env ?? process.env
  const purpose = options.purpose ? String(options.purpose) : 'unspecified'
  const timeoutMs = Number.isFinite(options.timeoutMs)
    ? Math.max(0, Number(options.timeoutMs))
    : DEFAULT_SLOT_LOCK_TIMEOUT_MS
  const pollMs = Number.isFinite(options.pollMs) && options.pollMs > 0 ? Number(options.pollMs) : DEFAULT_SLOT_LOCK_POLL_MS
  const kill = options.kill ?? process.kill
  const sleep = options.sleep ?? waitSynchronously
  const now = options.now ?? Date.now

  if (slotLockDisabled(env)) return skippedSlotLock(lockPath, purpose)

  mkdirSync(path.dirname(lockPath), { recursive: true })

  const record = {
    pid: process.pid,
    hostname: hostname(),
    command: options.command ? String(options.command) : describeCommand(),
    startedAt: new Date(now()).toISOString(),
    purpose,
    // Unique per acquisition, so `release()` can tell "my lock" from "someone else's lock that
    // replaced mine after I went stale". Without it, a released-after-reclaim handle would delete a
    // live consumer's lock and hand the slot to two writers.
    token: `${process.pid}-${now()}-${Math.random().toString(36).slice(2, 10)}`
  }

  const waitedFrom = now()
  const deadline = waitedFrom + timeoutMs

  for (;;) {
    try {
      writeLockRecord(lockPath, record)
      return slotLockHandle(lockPath, record)
    } catch (error) {
      if (error.code !== 'EEXIST') throw error
    }

    const holder = readLockRecord(lockPath)
    if (isReclaimable(holder, { kill })) {
      // The reclaim may or may not win the takeover; either way the loop retries the exclusive
      // create, which is the only thing that decides who owns the slot.
      if (tryReclaimStaleLock(lockPath, record, { kill })) continue
    }

    const remaining = deadline - now()
    if (remaining <= 0) {
      throw slotLockHeldError(lockPath, readLockRecord(lockPath), {
        waitedMs: now() - waitedFrom,
        timeoutMs
      })
    }
    sleep(Math.min(pollMs, remaining))
  }
}

/**
 * Take the lease, run `fn`, and always release — including when `fn` throws.
 *
 * WHY: the failure this module exists for left trees half-written, and a lock that survives a crash
 * would convert a crash into a permanently blocked slot. `release()` in a `finally` is what makes a
 * throwing build leave the slot acquirable, so the next consumer waits for a build rather than for a
 * human with `rm`.
 *
 * @template T
 * @param {string} slotDir
 * @param {object|((lock: object) => T)} options  Same options as `acquireSlotLock`, or the callback.
 * @param {(lock: object) => T} [fn]
 * @returns {T}
 */
export function withSlotLock(slotDir, options, fn) {
  const resolvedOptions = typeof options === 'function' ? {} : options ?? {}
  const callback = typeof options === 'function' ? options : fn
  if (typeof callback !== 'function') throw new Error('withSlotLock needs a callback')
  const lock = acquireSlotLock(slotDir, resolvedOptions)
  try {
    return callback(lock)
  } finally {
    lock.release()
  }
}

/** Exclusive create — the entire mutual exclusion — then write the record. */
function writeLockRecord(file, record) {
  const handle = openSync(file, 'wx')
  try {
    writeSync(handle, `${JSON.stringify(record)}\n`)
  } catch (error) {
    // We created it, so removing the half-written record is ours to do: an empty file parses as
    // "unreadable owner" and would otherwise block the slot for every consumer that is polite
    // enough not to steal a lock it cannot read.
    closeSync(handle)
    rmSync(file, { force: true })
    throw error
  }
  closeSync(handle)
}

function readLockRecord(file) {
  try {
    const record = JSON.parse(readFileSync(file, 'utf8'))
    if (!record || typeof record !== 'object') return null
    if (!Number.isInteger(record.pid) || record.pid <= 0) return null
    return record
  } catch {
    return null
  }
}

/**
 * Whether a lock may be reclaimed.
 *
 * Deliberately conservative in one direction only: an unreadable record, a pid this machine can
 * still signal, and a record written on another host are all "not reclaimable". The cost of that
 * choice is a manual `rm` after a crash that happened moments ago; the cost of the opposite is two
 * writers in one slot, which is silent and corrupts a tree.
 */
function isReclaimable(holder, { kill = process.kill, localHost = hostname() } = {}) {
  if (!holder) return false
  if (holder.hostname && holder.hostname !== localHost) return false
  return !isProcessAlive(holder.pid, kill)
}

/**
 * Reclaim a stale lock, with exactly one reclaimer.
 *
 * WHY the takeover file exists: the obvious implementation — delete the stale lock, then create your
 * own — lets two consumers that both judged it stale both delete, and the second delete removes the
 * first one's fresh lock, which is two owners. The takeover file is created exclusively (`wx`), so
 * the filesystem elects one reclaimer; a consumer that does not hold it must not touch the lock at
 * all. A takeover record whose own owner is gone is cleared, and the exclusive create elects a new
 * winner, so a reclaimer that dies leaves the slot reclaimable.
 *
 * Returns true when the stale lock was removed and the caller should retry its exclusive create.
 */
function tryReclaimStaleLock(lockPath, record, { kill }) {
  const takeoverPath = `${lockPath}.takeover`
  let won = false
  for (let attempt = 0; attempt < 4 && !won; attempt += 1) {
    try {
      writeLockRecord(takeoverPath, record)
      won = true
    } catch (error) {
      if (error.code !== 'EEXIST') throw error
      const rival = readLockRecord(takeoverPath)
      if (rival && rival.hostname === record.hostname && isProcessAlive(rival.pid, kill)) return false
      // The rival reclaimer is gone or its marker is unreadable. Clear it and let the exclusive
      // create elect one winner; removing a *takeover marker* is safe precisely because it is not
      // the lock.
      rmSync(takeoverPath, { force: true })
    }
  }
  if (!won) return false
  try {
    const holder = readLockRecord(lockPath)
    // Re-check under the takeover: the holder may have released between the judgement and here, in
    // which case there is nothing to reclaim and the create must decide the winner on its own.
    if (!isReclaimable(holder, { kill })) return false
    rmSync(lockPath, { force: true })
    return true
  } finally {
    rmSync(takeoverPath, { force: true })
  }
}

/** A handle whose `release` removes the file only while this process still owns the record. */
function slotLockHandle(lockPath, record) {
  let released = false
  return {
    path: lockPath,
    pid: record.pid,
    purpose: record.purpose,
    acquired: true,
    release() {
      if (released) return
      released = true
      const owner = readLockRecord(lockPath)
      // Safe to call when this handle does not hold the lock: a missing file, an unreadable one, or
      // a file recording a different token all leave the real holder's lock alone. Deleting it would
      // hand the slot to two writers, which is the failure this module exists to prevent.
      if (!owner || owner.token !== record.token) return
      rmSync(lockPath, { force: true })
    }
  }
}

/** The handle returned when the escape hatch skipped acquisition. It holds nothing, so it releases nothing. */
function skippedSlotLock(lockPath, purpose) {
  return {
    path: lockPath,
    pid: null,
    purpose,
    acquired: false,
    release() {}
  }
}

function slotLockHeldError(lockPath, holder, { waitedMs, timeoutMs }) {
  const now = Date.now()
  let who = 'an unreadable owner record (a partial write, or a file this script does not own)'
  if (holder) {
    const startedAt = Date.parse(holder.startedAt ?? '')
    const age = Number.isFinite(startedAt) ? `${Math.max(0, now - startedAt)}ms` : 'an unknown time'
    who =
      `pid ${holder.pid} on ${holder.hostname ?? 'an unrecorded host'}, held for ${age} ` +
      `for "${holder.purpose ?? 'an unspecified purpose'}"` +
      (holder.command ? ` (command: ${holder.command})` : '')
  }
  const error = new SlotLockHeldError(
    `slot lock '${lockPath}' is held by ${who}, and waited ${waitedMs}ms of the allowed ${timeoutMs}ms. ` +
      'Another Gradle build, verification, release or Codex worker is using this slot; Gradle\'s own ' +
      'wrapper lock lives in GRADLE_USER_HOME and kills the second process mid-write instead of ' +
      'queueing it, which is why this lease refuses instead. Wait for the holder to finish, or — only ' +
      `once you are certain that process is gone — delete ${lockPath}.`,
    { lockPath, holder, waitedMs, timeoutMs }
  )
  return error
}

/** The command that took the lease, recorded so the next consumer can name the holder. */
function describeCommand() {
  const script = process.argv[1] ? path.basename(process.argv[1]) : 'node'
  const rest = process.argv.slice(2).join(' ')
  return rest ? `${script} ${rest}` : script
}

/**
 * Block the current thread without burning CPU. `Atomics.wait` on a scratch buffer is the portable
 * way to sleep synchronously in Node, and synchronous is what a Gradle caller needs: it must not
 * hand back a promise and let a build start before the lease is held.
 */
function waitSynchronously(ms) {
  const scratch = new Int32Array(new SharedArrayBuffer(4))
  Atomics.wait(scratch, 0, 0, Math.max(1, Math.ceil(ms)))
}

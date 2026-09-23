/**
 * Codex worker ledger state — the single source of truth for "what happened to this worker".
 *
 * Why this is its own module
 * --------------------------
 * The ledger used to answer that question from `meta.status` alone unless an exit record existed.
 * So a worker that died abruptly — `capability-integration2`, whose wrapper pid 10188 was no longer
 * running, with no `.exit.json` and no `.last.md` — was still reported `running`, forever. The
 * orchestrator nearly waited on a process that did not exist, and the ledger's final verdict on
 * that worker stayed wrong on disk.
 *
 * The decision is a pure function of three inputs — the recorded status, the exit record, and
 * whether the recorded pid is alive — so it can be unit-tested directly. `scripts/codex-run.mjs` is
 * a CLI with a top-level dispatch, so nothing defined there can be imported without running it;
 * keeping the classifier here makes it testable. See the same shape in `scripts/lib/codex-signals.mjs`
 * and its test `scripts/test-codex-signals.mjs`.
 *
 * Precedence is unchanged from the CLI it replaces
 * ------------------------------------------------
 *   exit record with a signal        -> BLOCKED(signal)   (signal beats a zero exit code:
 *   exit record, exit code 0         -> DONE               a worker that stopped on a limit
 *   exit record, non-zero exit code  -> FAILED(exitCode)   has not delivered)
 *   no exit record, not alive        -> LOST  (new)
 *   no exit record, otherwise        -> the recorded status, or `unknown`
 *
 * The only new outcome is LOST. No existing label changes, and none of the existing precedence moves.
 */

import { readFileSync } from 'node:fs'

/**
 * The label for a worker that recorded itself as `running`, never wrote an exit record, and whose
 * pid is not alive.
 *
 * Deliberately neither `DONE` nor `FAILED`: the process vanished, so nobody knows whether the work
 * landed. It is also not `running`, because a reader must not wait on it. Callers that print it
 * should also append `LOST_NOTE`, so the label is never read as a clean outcome.
 */
export const LOST_STATE = 'LOST'

/** Human-facing suffix for a LOST worker: it did not report, so its deliverable is unverified. */
export const LOST_NOTE = '(died without exit record — work unverified)'

/**
 * Decides the final label for one worker.
 *
 * @param {object} input
 * @param {string|null|undefined} input.status   `meta.status` as recorded on disk.
 * @param {object|null|undefined} input.exitRecord Parsed `.exit.json`, or null/undefined if absent.
 * @param {boolean|undefined} input.alive        Whether the recorded pid is alive. `undefined` is
 *                                               treated as "not determined", which must NOT produce
 *                                               LOST: a false LOST on a healthy worker is worse than
 *                                               the ghost it replaces.
 * @returns {string} one of `BLOCKED(<signal>)`, `DONE`, `FAILED(<code>)`, `LOST`, or the recorded status.
 */
export function classifyWorkerState({ status, exitRecord, alive } = {}) {
  const record = exitRecord ?? null
  if (record) {
    // Signal first, for the same reason `finish()` writes it first: exit 0 does not mean the work
    // happened. A worker that stopped on a usage limit has not delivered.
    if (record.signal) return `BLOCKED(${record.signal})`
    if (record.exitCode === 0) return 'DONE'
    return `FAILED(${record.exitCode})`
  }

  // No exit record. This is where the old ledger stopped thinking and simply echoed `running`.
  // Only the exact `running` status is downgraded, and only on positive evidence of death; every
  // other recorded status keeps its current meaning.
  if (status === 'running' && alive === false) return LOST_STATE
  return status || 'unknown'
}

/**
 * Whether a recorded pid is still alive — a heuristic, on purpose, and here is exactly how it lies.
 *
 * `process.kill(pid, 0)` sends no signal; it only asks the OS whether the pid can be signalled. It
 * throws `ESRCH` when the process is gone, and `EPERM` when the process exists but this user may not
 * signal it — **EPERM counts as alive**, because "exists but not mine" is not "dead", and calling it
 * dead would raise a false LOST.
 *
 * Known limitation — pid reuse: pids are recycled. A dead worker's pid can later belong to an
 * unrelated live process, in which case this returns `true` and the worker is still reported
 * `running`. Nothing available to a user-space script can distinguish a recycled pid from the
 * original process, so this remains a heuristic. It is still strictly better than never checking:
 * it converts the common abnormal-death case from "silently wrong forever" into "reported as LOST",
 * and its failure mode is conservative — a recycled pid looks alive, it does not invent a LOST.
 *
 * The same conservatism governs unexpected errors: only `ESRCH` proves death. Any other error
 * (including Windows `ERR_INVALID_ARG_TYPE` for a pid outside the signallable range) is treated as
 * alive, so the check never fires on evidence it cannot interpret.
 *
 * @param {number|string|null|undefined} pid
 * @param {(pid: number, signal: number) => void} [kill] Injectable for tests; defaults to `process.kill`.
 * @returns {boolean} true when the pid exists and is signallable (or is EPERM-protected).
 */
export function isProcessAlive(pid, kill = process.kill) {
  const numeric = Number(pid)
  // A missing, non-numeric, zero, negative or fractional pid cannot identify a live process.
  // This is a definite "not alive", not a guess: the ledger recorded nothing usable to wait on.
  if (!Number.isInteger(numeric) || numeric <= 0) return false

  try {
    kill(numeric, 0)
    return true
  } catch (error) {
    // ESRCH is the only code that positively means "no such process". Everything else — EPERM in
    // particular — means we could not prove it is gone, so we must not claim it is.
    return error?.code !== 'ESRCH'
  }
}

/**
 * Derives the label from what is on disk, which is what `codex-run.mjs` calls.
 *
 * @param {object|null|undefined} meta        Parsed `.meta.json` (its `status` and `pid` are used).
 * @param {object|null|undefined} exitRecord  Parsed `.exit.json`, or null if the file is absent.
 * @param {object} [options]
 * @param {(pid: number, signal: number) => void} [options.kill] Injectable for tests.
 * @returns {string}
 */
export function classifyWorkerFromDisk(meta, exitRecord, { kill = process.kill } = {}) {
  return classifyWorkerState({
    status: meta?.status,
    exitRecord,
    alive: isProcessAlive(meta?.pid, kill)
  })
}

/**
 * Reads and parses a JSON state file without throwing.
 *
 * State files are machine-written, but a worker killed mid-`writeFileSync` leaves a **truncated**
 * file, and a hand-edited one can gain a **BOM**. Both make `JSON.parse` throw, and because the
 * ledger views iterate every worker, one throw aborted the entire listing — a single truncated file
 * hid every worker after it. That is the worst possible failure for the instrument the orchestrator
 * uses to decide whether anything is still running, and it was measured: `list` printed four rows
 * and exited non-zero with the real ghost worker present.
 *
 * The failure is surfaced rather than swallowed, so the unreadable file appears as
 * `UNREADABLE <id> (reason)` instead of silently vanishing from the ledger.
 *
 * @param {string} path
 * @param {(path: string, encoding: string) => string} [readFile] Injectable for tests.
 * @returns {{ok: true, value: unknown} | {ok: false, error: string}}
 */
export function readJsonSafe(path, readFile = readFileSync) {
  try {
    return { ok: true, value: JSON.parse(readFile(path, 'utf8')) }
  } catch (error) {
    return { ok: false, error: error?.message ?? String(error) }
  }
}

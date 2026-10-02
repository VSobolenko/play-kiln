/**
 * Subagent liveness — whether a dispatched worker is still working, has finished, failed, or has
 * stopped producing anything at all.
 *
 * Why this exists
 * ---------------
 * The factory's model is that the main chat orchestrates and the work runs in subagents
 * (`.cursor/rules/parallel-execution.mdc`). A `Task` dispatch is a *blocking* call: the orchestrator
 * does not get control back until the agent returns. When an agent stops producing output and never
 * returns, nothing notices. The orchestrator waits, the turn never ends, and the owner watches a
 * frozen chat — which is exactly what he reported on 2026-10-01: *"they stop on their own, and our
 * orchestrator just stands and waits, maybe for an hour."*
 *
 * The same defect was already fixed for Codex workers, and the shape of that fix is the shape of
 * this one: `scripts/lib/codex-ledger.mjs` downgrades a worker that claimed `running`, never wrote
 * an exit record and whose pid is gone to `LOST`, and `scripts/codex-run.mjs list` prints it. Its
 * header records that a dead worker *"was still reported `running`, forever"* before that. Cursor
 * subagents had no equivalent, so this module is it.
 *
 * What can and cannot be observed from outside
 * -------------------------------------------
 * A Cursor subagent writes a JSONL transcript, one record per event. Two facts are readable from
 * it, and they are enough:
 *
 *   - the **terminal record** — a last line of `{"type":"turn_ended","status":"success"|"error"}`.
 *     Measured over this project's 67 finished transcripts: 66 `success`, 1 `error`. Its presence
 *     means the agent stopped *on purpose* and said how it went.
 *   - the **last write time**, which is the only liveness signal there is. `mtime` moves when the
 *     agent emits a record, and stops when it does not.
 *
 * What this module therefore does **not** claim: that a silent agent is *dead*. A silent agent and a
 * dead agent are indistinguishable from outside — both have stopped writing. So the word used here is
 * `STALLED`, not `DEAD`, and it means exactly "no output for longer than the threshold". The action
 * is the same either way (re-dispatch its brief, which is why the brief is kept), and a label that
 * claimed knowledge it does not have would be the defect `AGENTS.md` calls the factory's
 * most-recorded failure mode.
 *
 * Precedence, and why it is this order
 * ------------------------------------
 *   terminal record, status success  -> DONE
 *   terminal record, any other status-> FAILED      (it stopped and reported a failure)
 *   no terminal record, recently active -> RUNNING
 *   no terminal record, idle past threshold -> STALLED
 *
 * A terminal record outranks the clock in both directions: a `DONE` agent whose file happens to be
 * old is still `DONE`, and it must never be reported as stalled. That is the fixture the test suite
 * carries as its negative control, because getting it backwards would re-dispatch work that already
 * landed — the expensive direction of this mistake.
 */

/** No output for longer than the threshold: the agent may still recover, so this is not `DEAD`. */
export const STALLED = 'STALLED'

/** The agent's own terminal record reported something other than `success`. Re-dispatch it. */
export const FAILED = 'FAILED'

/** A terminal record with `status: success` exists. The work landed; do not re-dispatch. */
export const DONE = 'DONE'

/** No terminal record and the transcript is still moving. Leave it alone. */
export const RUNNING = 'RUNNING'

/** The two states that want the orchestrator's attention, and the ones `--wait` stops on. */
export const UNHEALTHY_STATES = Object.freeze([FAILED, STALLED])

/** Every state a subagent can be in, in the order they are reported. */
export const STATES = Object.freeze([FAILED, STALLED, RUNNING, DONE])

/** Default idle threshold: the longest measured healthy gap, several times over. */
export const DEFAULT_STALL_SECONDS = 180

/**
 * How much of a transcript is read to find the terminal record.
 *
 * The record is the file's last line and is tiny, so the tail is where it lives. Tailing also keeps
 * a poll loop cheap: the finished transcripts in this project run up to 400 KB, and a `--wait` loop
 * re-reads every candidate every interval.
 */
export const TAIL_BYTES = 64 * 1024

/**
 * The last JSON record in a transcript, or `null` when none parses.
 *
 * A tail slice starts mid-line when the preceding record is larger than the window, so unparseable
 * fragments are skipped rather than treated as corruption, and the last *readable* record wins. The
 * terminator is searched for by string before parsing, so a large transcript that does not contain
 * one costs a scan rather than a `JSON.parse` of every line.
 */
export function readLastRecord(text) {
  const lines = text.split('\n')
  for (let index = lines.length - 1; index >= 0; index -= 1) {
    const line = lines[index].trim()
    if (!line) continue
    try {
      return JSON.parse(line)
    } catch {
      // A partial first line, or a record still being written. Keep looking backwards.
    }
  }
  return null
}

/**
 * The terminal record of a transcript, or `null` when the agent has not finished.
 *
 * `type` is the discriminator and `status` is carried through uninterpreted, because the vocabulary
 * on the other side is Cursor's and guessing at it here would be a second source of truth.
 */
export function readTerminal(text) {
  const record = readLastRecord(text)
  if (record && record.type === 'turn_ended') {
    return { status: typeof record.status === 'string' ? record.status : 'unknown' }
  }
  return null
}

/**
 * The state of one subagent from four observations, as a pure function so it can be unit-tested
 * without a filesystem or a clock — the same reason `classifyWorkerState` is separate from its CLI.
 *
 * @param {object} input
 * @param {boolean} input.hasTerminal        Whether a `turn_ended` record was found.
 * @param {string|null} [input.terminalStatus] That record's `status` when one was found.
 * @param {number} input.idleMs              Milliseconds since the transcript was last written.
 * @param {number} [input.stallSeconds]      Idle threshold for `STALLED`.
 * @returns {string} one of `DONE`, `FAILED`, `RUNNING`, `STALLED`.
 */
export function classifySubagent({ hasTerminal, terminalStatus, idleMs, stallSeconds = DEFAULT_STALL_SECONDS } = {}) {
  // The terminal record is the agent's own conclusion and outranks the clock. Checking it first is
  // what keeps a finished agent from being re-dispatched just because its file went cold.
  if (hasTerminal) return terminalStatus === 'success' ? DONE : FAILED
  return idleMs >= stallSeconds * 1000 ? STALLED : RUNNING
}

/**
 * Lists the subagent transcript directories this machine could be using, newest activity first.
 *
 * Cursor keeps transcripts outside the repository, under the home directory, so this **reads** a path
 * outside it. That is deliberate and allowed: `.cursor/rules/workspace-hygiene.mdc` forbids factory
 * processes from *writing* outside the repository, and this module never writes. It is also the only
 * way to observe an agent the orchestrator dispatched, so refusing to look would mean keeping the
 * defect in exchange for a tidier boundary.
 *
 * Resolution order is deliberate: an explicit override, then the directory derived from this
 * repository's own path, then a scan. The derived form is tried first because it is exact, and the
 * scan exists because the derivation encodes a Cursor-internal naming convention that may change.
 *
 * @param {object} input
 * @param {string} input.root      The repository root, used to derive the project directory name.
 * @param {string} input.homedir   The user's home directory.
 * @param {(dir: string) => string[]} input.readdir  Injectable for tests.
 * @param {(path: string) => boolean} input.exists   Injectable for tests.
 * @param {(path: string) => {mtimeMs:number}} input.stat Injectable for tests.
 * @param {string|null} [input.override] `PLAYKILN_SUBAGENT_DIR` when set.
 * @returns {string[]} Candidate directories, most recently active first.
 */
export function candidateSubagentDirs({ root, homedir, readdir, exists, stat, override = null }) {
  // The name Cursor gives a project directory: drive or leading slash folded to a dash, separators
  // and the volume colon folded to dashes. `D:\Python\PlayKiln` becomes `d-Python-PlayKiln`.
  const projectName = String(root)
    .replace(/[:/\\]+/g, '-')
    .replace(/^-/, '')
    .toLowerCase()

  const parents = []
  if (override) parents.push(override)
  parents.push(`${homedir}/.cursor/projects/${projectName}/agent-transcripts`)

  const dirs = []
  const push = (parent) => {
    let chats = []
    try {
      chats = readdir(parent)
    } catch {
      return
    }
    for (const chat of chats) dirs.push(`${parent}/${chat}/subagents`)
  }
  for (const parent of parents) push(parent)

  // The scan fallback: any project, any chat. It is what makes this work when the derived name is
  // wrong, which is a Cursor-internal detail and not a contract this factory controls.
  const projectsRoot = `${homedir}/.cursor/projects`
  if (exists(projectsRoot)) {
    let projects = []
    try {
      projects = readdir(projectsRoot)
    } catch {
      projects = []
    }
    for (const project of projects) push(`${projectsRoot}/${project}/agent-transcripts`)
  }

  const existing = dirs.filter((dir) => exists(dir))
  // Most recent activity first, so the live chat wins over a stale one and `--dir` can be omitted.
  // A directory whose transcripts cannot be read sorts last rather than being dropped: it is a real
  // candidate, and silently removing it would make "no subagents" and "unreadable directory" look
  // the same, which is the distinction this whole module exists to preserve.
  const newest = (dir) => {
    let names = []
    try {
      names = readdir(dir)
    } catch {
      return -1
    }
    let best = -1
    for (const name of names) {
      if (!name.endsWith('.jsonl')) continue
      try {
        const info = stat(`${dir}/${name}`)
        if (info.mtimeMs > best) best = info.mtimeMs
      } catch {
        // An unreadable transcript does not disqualify its directory.
      }
    }
    return best
  }
  return existing.slice().sort((a, b) => newest(b) - newest(a))
}

/**
 * Reads every candidate subagent in a directory and classifies it.
 *
 * `createdWithinSeconds` is what keeps this from reporting last week's finished agents: a transcript
 * older than the window is not part of the batch being watched, however cold its file is. Creation
 * time is used rather than modification time because a *stalled* agent's mtime is old by definition,
 * and filtering on it would hide exactly the rows this exists to find. Where a platform does not
 * record a birth time, the fallback is deliberately to **include** the file: a false `RUNNING` costs
 * a row the reader can see, and a false exclusion costs the defect.
 *
 * @param {object} input
 * @param {string} input.dir
 * @param {number} [input.now]
 * @param {number} [input.stallSeconds]
 * @param {number} [input.createdWithinSeconds] 0 or absent means every transcript in the directory.
 * @param {(path: string) => string} input.readFile
 * @param {(path: string) => {size:number, mtimeMs:number, birthtimeMs:number}} input.stat
 * @param {(dir: string) => string[]} input.readdir
 * @returns {Array<{id: string, file: string, state: string, status: string|null, idleSeconds: number, sizeBytes: number}>}
 */
export function scanSubagents({
  dir,
  now = Date.now(),
  stallSeconds = DEFAULT_STALL_SECONDS,
  createdWithinSeconds = 0,
  readFile,
  stat,
  readdir
}) {
  let names = []
  try {
    names = readdir(dir)
  } catch {
    return []
  }

  const rows = []
  for (const name of names) {
    if (!name.endsWith('.jsonl')) continue
    const file = `${dir}/${name}`
    let info
    try {
      info = stat(file)
    } catch {
      continue
    }

    const createdMs = Number.isFinite(info.birthtimeMs) && info.birthtimeMs > 0 ? info.birthtimeMs : info.mtimeMs
    if (createdWithinSeconds > 0 && now - createdMs > createdWithinSeconds * 1000) continue

    // Only the tail is read; see TAIL_BYTES.
    let text = ''
    try {
      const full = readFile(file)
      text = full.length > TAIL_BYTES ? full.slice(full.length - TAIL_BYTES) : full
    } catch {
      continue
    }

    const terminal = readTerminal(text)
    const idleMs = Math.max(0, now - info.mtimeMs)
    const state = classifySubagent({
      hasTerminal: Boolean(terminal),
      terminalStatus: terminal?.status ?? null,
      idleMs,
      stallSeconds
    })

    rows.push({
      id: name.replace(/\.jsonl$/, '').slice(0, 8),
      file,
      state,
      status: terminal?.status ?? null,
      idleSeconds: Math.round(idleMs / 1000),
      sizeBytes: info.size
    })
  }

  // Problems first, then the working agents by id; a reader should not have to scan for red rows.
  const rank = (state) => {
    const index = [FAILED, STALLED, RUNNING, DONE].indexOf(state)
    return index === -1 ? STATES.length : index
  }
  return rows.sort((a, b) => rank(a.state) - rank(b.state) || a.id.localeCompare(b.id))
}

/**
 * The re-dispatch registry: which brief belongs to which dispatched agent.
 *
 * The owner's instruction was that a stopped worker is not thrown away but replaced, and the only
 * thing needed to replace it is the brief it was given. The filesystem is the bus
 * (`.cursor/rules/parallel-execution.mdc`), so the orchestrator writes `.kiln/subagents/<slug>.md`
 * before dispatching and appends one `id<TAB>slug` row to `.kiln/subagents/index.tsv` once the
 * dispatch returns an id. This resolves the second half, and an unresolved id is reported as such
 * rather than guessed at.
 *
 * @param {string} text  The contents of `index.tsv`.
 * @param {string} id    A transcript id, with or without its `.jsonl` suffix.
 * @returns {string|null} The slug recorded for that id, or null.
 */
export function resolveBriefSlug(text, id) {
  const wanted = String(id).replace(/\.jsonl$/, '')
  for (const row of String(text).split('\n')) {
    if (!row.trim()) continue
    const [recorded, slug] = row.split('\t')
    if ((recorded || '').trim() === wanted) return (slug || '').trim() || null
  }
  return null
}

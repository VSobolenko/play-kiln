#!/usr/bin/env node
/**
 * stop — one evidence follow-up, but only when this session actually changed something.
 *
 * Why the condition is real code and not a sentence: this hook used to emit its reminder
 * unconditionally, while the reminder's own text said "if this session changed apps/ or harness/".
 * The wording promised a check the implementation never performed, so it fired after every single
 * turn — including turns that only answered a question and edited nothing. Two costs, and the
 * second is the serious one: the owner read it as spam, and a reminder that fires when nothing
 * changed is indistinguishable from one that fires when something did, which is the opposite of
 * what a gate is for. A message that restates a rule has to be *built from* that rule; otherwise
 * the two drift apart silently, which is exactly what happened here.
 *
 * What "this session changed something" means here, and why:
 *
 * The natural signal would be the file-edit ledger that `.cursor/hooks/after-edit-ledger.cjs`
 * writes, since it records one line per edit. It is unusable for this, and that was checked rather
 * than assumed: in this Cursor build the `afterFileEdit` payload carries neither `file_path` nor
 * `conversation_id`, so every ledger line is `timestamp \t - \t ` — the path is empty and no edit
 * can be attributed to any chat. A decision cannot be built on that.
 *
 * So the session boundary is a timestamp, taken at `sessionStart` and advanced after every check.
 * Changes are then whatever under `apps/` or `harness/` has an `mtime` newer than that watermark.
 * Modification time is the right sensor for the question actually being asked — "did anything in
 * these trees change?" — because it is independent of how the change was made: a Write tool, a
 * patch script, or a shell redirect all move it, so none of them can slip past.
 *
 * Two consequences worth stating:
 *
 * - **The watermark advances on every stop, including quiet ones.** Announcing a batch of changes
 *   once is the point; a watermark fixed at session start would re-announce the same edits after
 *   every following turn, which is the spam this file exists to remove.
 * - **Generated output is pruned** (`build/`, `.gradle/`, `dist/`, `captures/` and friends). A
 *   Gradle task or a capture run writes thousands of files under `apps/`, and counting those would
 *   fire the reminder for a build rather than for a change anyone should answer for.
 *
 * Missing or unstamped baseline: the hook writes one and stays quiet. It cannot attribute the
 * changes it did not observe, and guessing "yes" is what produced the original spam. The real
 * evidence gate is `scripts/verify.mjs`, which is unaffected either way — this is a nudge.
 *
 * Disable with env PLAYKILN_STOP_FOLLOWUP=0.
 */
const fs = require('fs')
const path = require('path')

/** Directories that are outputs or machine state, never a change the owner must answer for. */
const SKIP_DIRS = new Set([
  'build',
  '.gradle',
  '.kotlin',
  '.idea',
  '.cxx',
  'node_modules',
  'dist',
  'captures',
  '.kiln',
  '.superpowers'
])

/** The trees whose changes the stop gate is about — the slot sources and the factory harness. */
const WATCHED = ['apps', 'harness']

/** Enough to recognise which work is being asked about, without turning the reminder into a dump. */
const MAX_NAMED = 3

function readInput() {
  try {
    return JSON.parse(fs.readFileSync(0, 'utf8') || '{}')
  } catch {
    return {}
  }
}

function emit(payload) {
  process.stdout.write(JSON.stringify(payload || {}))
}

function baselineFile(root) {
  return path.join(root, '.superpowers', 'stop-gate', 'session.json')
}

function readWatermark(root) {
  try {
    const parsed = JSON.parse(fs.readFileSync(baselineFile(root), 'utf8'))
    const since = Date.parse(parsed.since || '')
    if (!Number.isFinite(since)) return null
    // A baseline written for a different workspace says nothing about this one.
    if (parsed.root && parsed.root !== root) return null
    return since
  } catch {
    return null
  }
}

function writeWatermark(root, since) {
  const file = baselineFile(root)
  try {
    fs.mkdirSync(path.dirname(file), { recursive: true })
    fs.writeFileSync(file, `${JSON.stringify({ since: new Date(since).toISOString(), root }, null, 2)}\n`)
  } catch {
    // A read-only workspace must not turn a nudge into a failure.
  }
}

function collectChanges(dir, root, since, found) {
  let entries
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true })
  } catch {
    return found
  }
  for (const entry of entries) {
    // A link can point outside the tree, and following one would report a change nobody made here.
    if (entry.isSymbolicLink()) continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (SKIP_DIRS.has(entry.name)) continue
      collectChanges(full, root, since, found)
      continue
    }
    if (!entry.isFile()) continue
    let stat
    try {
      stat = fs.statSync(full)
    } catch {
      continue
    }
    if (stat.mtimeMs > since) found.push(path.relative(root, full).split(path.sep).join('/'))
  }
  return found
}

function describe(paths) {
  const named = paths.slice(0, MAX_NAMED).join(', ')
  const rest = paths.length - MAX_NAMED
  return rest > 0 ? `${named} (+${rest} more)` : named
}

function main() {
  const input = readInput()

  if (process.env.PLAYKILN_STOP_FOLLOWUP === '0') return emit(null)
  if ((input.status || 'completed') !== 'completed') return emit(null)
  if (Number(input.loop_count || 0) !== 0) return emit(null)

  const root = (input.workspace_roots && input.workspace_roots[0]) || process.cwd()
  const since = readWatermark(root)
  if (since === null) {
    writeWatermark(root, Date.now())
    return emit(null)
  }

  const changed = []
  for (const relative of WATCHED) collectChanges(path.join(root, relative), root, since, changed)

  // Advance before deciding, so the same batch is announced once rather than after every turn.
  writeWatermark(root, Date.now())

  if (changed.length === 0) return emit(null)

  // The sentence is assembled from this run's own observation, so it cannot claim more or less
  // than what was measured.
  emit({
    followup_message:
      `PlayKiln stop gate: this session changed ${changed.length} file(s) under apps/ or harness/ — ` +
      `${describe(changed)}. Confirm evidence (\`node scripts/verify.mjs\` and/or \`/eval\`). ` +
      'If already reported with stdout, reply DONE.'
  })
}

main()

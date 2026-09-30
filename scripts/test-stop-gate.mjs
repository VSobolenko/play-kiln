#!/usr/bin/env node
/**
 * Regression suite for the conditional stop gate (`.cursor/hooks/stop-gate.cjs`).
 *
 * The defect this exists against: the hook emitted its evidence reminder unconditionally, while
 * its own text said "if this session changed apps/ or harness/". So it fired after every turn —
 * including turns that edited nothing — and the owner read it as spam. Every leg below is a
 * scenario where the old implementation fired and the new one must stay quiet, plus the one
 * scenario where it must still fire.
 *
 * The legs are only meaningful if they can fail, so `--hook <path>` runs them against any
 * implementation. Against the preserved pre-fix hook the quiet legs must FAIL and the exit code must
 * be non-zero — a test that passes against the broken code proves nothing, which is the whole point
 * of running it both ways.
 *
 * Fixtures live in a temp directory and are given explicit `mtime`s rather than relying on how
 * fast files are written, so every leg is deterministic on any filesystem.
 *
 * Usage:
 *   node scripts/test-stop-gate.mjs
 *   node scripts/test-stop-gate.mjs --hook .kiln/old-stop-gate.cjs   # must report failures
 */

import { spawnSync } from 'node:child_process'
import { mkdirSync, mkdtempSync, rmSync, utimesSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '..')
const hookArgIndex = process.argv.indexOf('--hook')
const HOOK = hookArgIndex === -1 ? path.join(ROOT, '.cursor', 'hooks', 'stop-gate.cjs') : process.argv[hookArgIndex + 1]

const NOW = Date.now()
/** Anything older than this is pre-session work and must not be reported. */
const BEFORE_SESSION = NOW - 60_000
/** Anything newer than this is a change made during the session. */
const DURING_SESSION = NOW - 1_000
/** The watermark a session start would have stamped: after the old files, before the new ones. */
const WATERMARK = NOW - 30_000
const BASELINE_BACKDATE = NOW - 10_000

const results = []
function check(ok, label) {
  results.push({ ok, label })
  console.log(`${ok ? 'PASS' : 'FAIL'}\t${label}`)
}

/** A fresh fixture workspace, so no leg can inherit another leg's watermark. */
function workspace() {
  return mkdtempSync(path.join(tmpdir(), 'stop-gate-'))
}

/** Write a file and give it an explicit age, so mtime comparisons never depend on timing. */
function writeAged(file, ms) {
  mkdirSync(path.dirname(file), { recursive: true })
  writeFileSync(file, 'fixture\n')
  utimesSync(file, new Date(ms), new Date(ms))
}

function stampBaseline(root, since) {
  const file = path.join(root, '.superpowers', 'stop-gate', 'session.json')
  mkdirSync(path.dirname(file), { recursive: true })
  writeFileSync(file, `${JSON.stringify({ since: new Date(since).toISOString(), root }, null, 2)}\n`)
}

/** Run the hook as Cursor would: JSON on stdin, JSON on stdout. */
function runHook(root, { status = 'completed', loop = 0, env = {} } = {}) {
  const result = spawnSync(process.execPath, [HOOK], {
    input: JSON.stringify({ workspace_roots: [root], status, loop_count: loop }),
    encoding: 'utf8',
    env: { ...process.env, ...env },
    windowsHide: true
  })
  const out = (result.stdout || '').trim()
  let parsed = {}
  try {
    parsed = JSON.parse(out || '{}')
  } catch {
    parsed = { unparseable: out }
  }
  return { message: parsed.followup_message || '', status: result.status }
}

console.log(`## stop gate — ${path.relative(ROOT, HOOK) || HOOK}\n`)

// 1. Nothing changed since the watermark: the reminder must not fire. This is the leg that
//    describes the owner's experience — quiet turns were the ones being spammed.
{
  const root = workspace()
  writeAged(path.join(root, 'apps', 'slot', 'app', 'Old.kt'), BEFORE_SESSION)
  stampBaseline(root, WATERMARK)
  check(runHook(root).message === '', 'no change since the watermark stays quiet')
  rmSync(root, { recursive: true, force: true })
}

// 2. A slot source changed: it fires, and it names what changed so the reader knows what is meant.
{
  const root = workspace()
  stampBaseline(root, WATERMARK)
  writeAged(path.join(root, 'apps', 'slot', 'app', 'New.kt'), DURING_SESSION)
  const { message } = runHook(root)
  check(message !== '', 'a change under apps/ fires the reminder')
  check(message.includes('apps/slot/app/New.kt'), 'the reminder names the changed path')
  rmSync(root, { recursive: true, force: true })
}

// 3. The harness is the other watched tree, and it must be watched for the same reason.
{
  const root = workspace()
  stampBaseline(root, WATERMARK)
  writeAged(path.join(root, 'harness', 'lessons.md'), DURING_SESSION)
  check(runHook(root).message !== '', 'a change under harness/ fires the reminder')
  rmSync(root, { recursive: true, force: true })
}

// 4. Changes outside the two trees are not this hook's business. Meetings that only touch docs
//    must be quiet, or the reminder means nothing.
{
  const root = workspace()
  stampBaseline(root, WATERMARK)
  writeAged(path.join(root, 'docs', 'note.md'), DURING_SESSION)
  check(runHook(root).message === '', 'a change outside apps/ and harness/ stays quiet')
  rmSync(root, { recursive: true, force: true })
}

// 5. Build output is not a change anyone answers for. A Gradle run writes thousands of files
//    under apps/, and counting them would fire the reminder for a build.
{
  const root = workspace()
  stampBaseline(root, WATERMARK)
  writeAged(path.join(root, 'apps', 'slot', 'build', 'outputs', 'app.apk'), DURING_SESSION)
  writeAged(path.join(root, 'apps', 'slot', '.gradle', 'cache.bin'), DURING_SESSION)
  writeAged(path.join(root, 'apps', 'slot', 'dist', 'release.aab'), DURING_SESSION)
  writeAged(path.join(root, 'apps', 'slot', 'captures', 'raw.png'), DURING_SESSION)
  check(runHook(root).message === '', 'generated output under build/, .gradle/, dist/, captures/ is pruned')
  rmSync(root, { recursive: true, force: true })
}

// 6. The watermark advances. Without this, one edit would be re-announced after every following
//    turn — the same spam in a slower form.
{
  const root = workspace()
  stampBaseline(root, WATERMARK)
  writeAged(path.join(root, 'apps', 'slot', 'app', 'New.kt'), DURING_SESSION)
  const first = runHook(root).message
  const second = runHook(root).message
  check(first !== '' && second === '', 'a batch of changes is announced once, not after every turn')
  rmSync(root, { recursive: true, force: true })
}

// 7. The documented kill switch keeps working.
{
  const root = workspace()
  stampBaseline(root, WATERMARK)
  writeAged(path.join(root, 'apps', 'slot', 'app', 'New.kt'), DURING_SESSION)
  check(
    runHook(root, { env: { PLAYKILN_STOP_FOLLOWUP: '0' } }).message === '',
    'PLAYKILN_STOP_FOLLOWUP=0 silences it even when something changed'
  )
  rmSync(root, { recursive: true, force: true })
}

// 8. The followup loop and non-completed turns must not fire, or the hook would answer itself.
{
  const root = workspace()
  stampBaseline(root, WATERMARK)
  writeAged(path.join(root, 'apps', 'slot', 'app', 'New.kt'), DURING_SESSION)
  check(runHook(root, { loop: 1 }).message === '', 'loop_count 1 stays quiet (the followup is not re-answered)')
  check(runHook(root, { status: 'aborted' }).message === '', 'a non-completed turn stays quiet')
  rmSync(root, { recursive: true, force: true })
}

// 9. No baseline: the hook cannot attribute what it did not observe, so it stays quiet and stamps
//    one. Guessing "yes" here is precisely what produced the spam.
{
  const root = workspace()
  writeAged(path.join(root, 'apps', 'slot', 'app', 'New.kt'), DURING_SESSION)
  const { message } = runHook(root)
  check(message === '', 'a missing baseline stays quiet rather than guessing')
  const baseline = path.join(root, '.superpowers', 'stop-gate', 'session.json')
  check(
    spawnSync(process.execPath, ['-e', `process.exit(require('fs').existsSync(${JSON.stringify(baseline)}) ? 0 : 1)`]).status === 0,
    'a missing baseline is written so the next check can decide'
  )
  rmSync(root, { recursive: true, force: true })
}

// 10. A baseline stamped for another workspace must not be used as this one's watermark, or two
//     open workspaces would silence each other's reminders.
{
  const root = workspace()
  const other = workspace()
  stampBaseline(root, WATERMARK)
  writeAged(path.join(root, 'apps', 'slot', 'app', 'New.kt'), DURING_SESSION)
  // Rewrite this root's baseline so it claims a different workspace, backdated so the file it
  // names is legitimate: the hook must ignore it and re-stamp rather than trust it.
  const file = path.join(root, '.superpowers', 'stop-gate', 'session.json')
  writeFileSync(file, `${JSON.stringify({ since: new Date(BASELINE_BACKDATE).toISOString(), root: other }, null, 2)}\n`)
  check(runHook(root).message === '', 'a baseline belonging to another workspace is not trusted')
  rmSync(root, { recursive: true, force: true })
  rmSync(other, { recursive: true, force: true })
}

const failed = results.filter((row) => !row.ok).length
console.log(`\n${results.length - failed}/${results.length} legs passed`)
console.log(`VERDICT: ${failed === 0 ? 'PASS' : `FAIL (${failed})`}`)
// A test exits non-zero when its legs fail; that is what an exit code is for, and it is what every
// other suite here does (`test-release-notes.py`, `test-locale-dedup.mjs`, `test-visual-qa-discovery.py`).
//
// An earlier revision of this file forced exit 0 unconditionally and justified it by analogy with
// `visual-qa.py`, which deliberately reports rather than gates. The analogy was wrong: `visual-qa.py`
// is a *report* over pixels that needs a human's judgement, while this is a regression test whose
// legs are objective PASS/FAIL. Forcing 0 made `selftest.mjs`'s "stop-gate test exits 0" check
// unable to see a failing leg — it could only detect a crash — and made a standalone run of this
// suite report success through its exit code while printing FAIL rows. The rows were the only
// truthful signal, which is one signal too few.
process.exitCode = failed === 0 ? 0 : 1

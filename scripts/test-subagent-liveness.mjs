/**
 * Regression tests for the subagent liveness sensor.
 *
 * The defect that motivated this file, measured on 2026-10-01: three subagents this chat had
 * dispatched wrote **zero bytes for over 100 seconds** while the orchestrator waited on them, and
 * nothing in the factory could see it. Two of them later resumed, so this is a *stall* sensor, not a
 * death certificate — and the distinction matters, because a sensor that fired on a merely slow
 * agent would re-dispatch work that is still running.
 *
 * So there are three things to prove, and the third is the one that protects finished work:
 *   1. a stalled agent MUST be reported `STALLED`   (recall — this is the reported defect);
 *   2. a failed agent MUST be reported `FAILED`, not `DONE`   (a real terminal failure is not a pass);
 *   3. a FINISHED agent MUST NEVER be reported `STALLED`, however cold its file   (precision — this
 *      is the expensive direction: it would discard a delivered result and re-run it).
 *
 * Run: node scripts/test-subagent-liveness.mjs
 */
import { execFileSync } from 'node:child_process'
import { mkdirSync, rmSync, utimesSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { SCRATCH_DIR } from './lib/scratch.mjs'
import {
  DONE,
  FAILED,
  RUNNING,
  STALLED,
  candidateSubagentDirs,
  classifySubagent,
  readTerminal,
  resolveBriefSlug,
  scanSubagents
} from './lib/subagent-liveness.mjs'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const SANDBOX = path.join(SCRATCH_DIR, `test-watch-subagents-${process.pid}`)

let failures = 0

function check(name, actual, expected) {
  const ok = actual === expected
  if (!ok) failures += 1
  console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name}`)
  if (!ok) console.log(`        expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`)
}

function checkTrue(name, actual) {
  check(name, Boolean(actual), true)
}

/** The line Cursor writes when an agent stops. `status` is Cursor's word and is carried through. */
function terminalLine(status) {
  return `${JSON.stringify({ type: 'turn_ended', status })}\n`
}

// ---------------------------------------------------------------------------------------------
// 1. The classifier, as a pure function — no filesystem, no clock.
// ---------------------------------------------------------------------------------------------
console.log('classifier')

check('no terminal record, still active -> RUNNING', classifySubagent({ hasTerminal: false, idleMs: 5_000, stallSeconds: 180 }), RUNNING)
check('no terminal record, idle past threshold -> STALLED', classifySubagent({ hasTerminal: false, idleMs: 200_000, stallSeconds: 180 }), STALLED)
check('idle exactly at the threshold counts as stalled', classifySubagent({ hasTerminal: false, idleMs: 180_000, stallSeconds: 180 }), STALLED)
check('terminal success -> DONE', classifySubagent({ hasTerminal: true, terminalStatus: 'success', idleMs: 1_000 }), DONE)
check('terminal error -> FAILED', classifySubagent({ hasTerminal: true, terminalStatus: 'error', idleMs: 1_000 }), FAILED)

// The precision controls. A finished result must survive its own file going cold, or the sensor
// would re-dispatch delivered work — worse than the stall it was built to catch.
check('NEGATIVE CONTROL: a finished agent is DONE however long its file has been cold', classifySubagent({ hasTerminal: true, terminalStatus: 'success', idleMs: 86_400_000 }), DONE)
check('NEGATIVE CONTROL: a failed agent is FAILED, never DONE', classifySubagent({ hasTerminal: true, terminalStatus: 'error', idleMs: 86_400_000 }), FAILED)

// ---------------------------------------------------------------------------------------------
// 2. Reading the terminal record out of a transcript tail.
// ---------------------------------------------------------------------------------------------
console.log('terminal record')

check('success is read', readTerminal(terminalLine('success'))?.status, 'success')
check('error is read', readTerminal(terminalLine('error'))?.status, 'error')
check('no terminal record reads as null', readTerminal('{"type":"assistant","text":"working"}\n'), null)
check('empty transcript reads as null', readTerminal(''), null)
check('a tail cut mid-line still finds the terminal record', readTerminal(`{"type":"tool","payload":"\n${terminalLine('success')}`)?.status, 'success')
check('an unknown status is carried through, not guessed', readTerminal(terminalLine('whatever'))?.status, 'whatever')
check('a large tool result before the terminal record does not hide it', readTerminal(`${JSON.stringify({ type: 'tool', payload: 'x'.repeat(5000) })}\n${terminalLine('success')}`)?.status, 'success')

// ---------------------------------------------------------------------------------------------
// 3. Scanning a directory. mtime and birthtime are injected so the control rows differ in nothing
//    but the one field under test, rather than racing a real clock.
// ---------------------------------------------------------------------------------------------
console.log('directory scan')

const NOW = 1_800_000_000_000
const SCAN_DIR = '/synthetic/subagents'

/** The module joins dir and name with `/`, so the fixtures use the same form on every platform. */
const fixtures = {
  'aaaaaaaa.jsonl': { body: terminalLine('success'), mtimeMs: NOW - 60_000 },
  'bbbbbbbb.jsonl': { body: terminalLine('error'), mtimeMs: NOW - 60_000 },
  'cccccccc.jsonl': { body: '{"type":"assistant"}\n', mtimeMs: NOW - 5_000 },
  'dddddddd.jsonl': { body: '{"type":"assistant"}\n', mtimeMs: NOW - 900_000 },
  'notes.txt': { body: 'not a transcript', mtimeMs: NOW - 60_000 }
}

const scanInput = (extra = {}) => ({
  dir: SCAN_DIR,
  now: NOW,
  stallSeconds: 180,
  readFile: (file) => fixtures[file.replace(`${SCAN_DIR}/`, '')]?.body ?? '',
  stat: (file) => {
    const fixture = fixtures[file.replace(`${SCAN_DIR}/`, '')]
    if (!fixture) throw new Error('ENOENT')
    return { size: fixture.body.length, mtimeMs: fixture.mtimeMs, birthtimeMs: NOW }
  },
  readdir: () => Object.keys(fixtures),
  ...extra
})

const rows = scanSubagents(scanInput())
check('every .jsonl is scanned and nothing else is', rows.length, 4)
check('states are classified per row', rows.map((row) => `${row.id}:${row.state}`).join(' '), 'bbbbbbbb:FAILED dddddddd:STALLED cccccccc:RUNNING aaaaaaaa:DONE')
check('problems sort before healthy rows', rows[0].state, FAILED)
check('the finished row is DONE despite a cold file', rows.find((row) => row.id === 'aaaaaaaa')?.state, DONE)
check('idle seconds are reported for a human', rows.find((row) => row.id === 'dddddddd')?.idleSeconds, 900)
check('the terminal status is carried into the row', rows.find((row) => row.id === 'bbbbbbbb')?.status, 'error')

// The window keeps last week's finished agents out of the report. Creation time is the filter, not
// modification time, because a stalled agent's mtime is old by definition and filtering on it would
// hide exactly the rows this exists to find.
const windowed = scanSubagents(scanInput({
  createdWithinSeconds: 600,
  stat: (file) => {
    const key = file.replace(`${SCAN_DIR}/`, '')
    const fixture = fixtures[key]
    if (!fixture) throw new Error('ENOENT')
    return {
      size: fixture.body.length,
      mtimeMs: fixture.mtimeMs,
      birthtimeMs: key === 'aaaaaaaa.jsonl' ? NOW - 3_600_000 : NOW
    }
  }
}))
check('a transcript created outside the window is excluded', windowed.some((row) => row.id === 'aaaaaaaa'), false)
check('a transcript inside the window is kept', windowed.some((row) => row.id === 'dddddddd'), true)

check('an unreadable directory yields no rows rather than throwing', scanSubagents({
  dir: '/does/not/exist',
  readFile: () => '',
  stat: () => ({ size: 0, mtimeMs: 0, birthtimeMs: 0 }),
  readdir: () => {
    throw new Error('ENOENT')
  }
}).length, 0)

// ---------------------------------------------------------------------------------------------
// 4. The brief index — what makes a stopped agent replaceable rather than lost.
// ---------------------------------------------------------------------------------------------
console.log('brief index')

const INDEX = '36bb4b12\tsurvey-testing-programs\n52347619\tmoney-review\n'
check('an id resolves to its slug', resolveBriefSlug(INDEX, '36bb4b12'), 'survey-testing-programs')
check('a .jsonl suffix is tolerated', resolveBriefSlug(INDEX, '52347619.jsonl'), 'money-review')
check('an unknown id resolves to nothing', resolveBriefSlug(INDEX, 'ffffffff'), null)
check('a row with no tab does not throw', resolveBriefSlug('garbage with no tab\n', 'garbage'), null)

// ---------------------------------------------------------------------------------------------
// 5. Discovery of the transcript directory, without which nothing else can run.
// ---------------------------------------------------------------------------------------------
console.log('directory discovery')

const CHAT_A = 'C:/Users/x/.cursor/projects/d-Python-PlayKiln/agent-transcripts/chat-a/subagents'
const CHAT_B = 'C:/Users/x/.cursor/projects/d-Python-PlayKiln/agent-transcripts/chat-b/subagents'
const CHAT_C = 'C:/Users/x/.cursor/projects/other-project/agent-transcripts/chat-c/subagents'
const PROJECTS = 'C:/Users/x/.cursor/projects'
const DERIVED = 'C:/Users/x/.cursor/projects/d-Python-PlayKiln/agent-transcripts'

const found = candidateSubagentDirs({
  root: 'D:\\Python\\PlayKiln',
  homedir: 'C:/Users/x',
  exists: (target) => [CHAT_A, CHAT_B, CHAT_C, PROJECTS, DERIVED].includes(target),
  readdir: (dir) => {
    if (dir === DERIVED) return ['chat-a', 'chat-b']
    if (dir === PROJECTS) return ['d-Python-PlayKiln', 'other-project']
    if (dir === 'C:/Users/x/.cursor/projects/other-project/agent-transcripts') return ['chat-c']
    // Each chat directory holds one transcript, so the freshness sort has something to compare.
    if (dir === CHAT_A) return ['aaaa1111.jsonl']
    if (dir === CHAT_B) return ['bbbb2222.jsonl']
    return []
  },
  stat: (file) => ({ mtimeMs: file.includes('chat-b') ? 900 : 100 })
})
check('the repository path resolves to its project directory', found.includes(CHAT_A), true)
check('the scan fallback reaches other projects', found.includes(CHAT_C), true)
check('the most recently active directory is first', found[0], CHAT_B)

// ---------------------------------------------------------------------------------------------
// 6. End to end, through the real CLI, on real files in the scratch directory.
// ---------------------------------------------------------------------------------------------
console.log('cli')

rmSync(SANDBOX, { recursive: true, force: true })
mkdirSync(SANDBOX, { recursive: true })

function sandbox(name) {
  const dir = path.join(SANDBOX, name)
  mkdirSync(dir, { recursive: true })
  return dir
}

function writeTranscript(dir, id, body) {
  const file = path.join(dir, `${id}.jsonl`)
  writeFileSync(file, body)
  return file
}

/** Runs the real script and normalises a non-zero exit into a value rather than a thrown error. */
function runWatch(args) {
  try {
    return { code: 0, stdout: execFileSync(process.execPath, [path.join(ROOT, 'scripts', 'watch-subagents.mjs'), ...args], { encoding: 'utf8' }) }
  } catch (error) {
    return { code: error.status ?? -1, stdout: `${error.stdout ?? ''}${error.stderr ?? ''}` }
  }
}

const doneDir = sandbox('done')
writeTranscript(doneDir, 'aaaaaaaa', terminalLine('success'))
const doneRun = runWatch(['--dir', doneDir, '--json'])
check('a finished agent exits 0', doneRun.code, 0)
checkTrue('the json report carries its rows', doneRun.stdout.includes('"state": "DONE"'))

const failedDir = sandbox('failed')
writeTranscript(failedDir, 'bbbbbbbb', terminalLine('error'))
const failedRun = runWatch(['--dir', failedDir])
check('a failed agent exits 1', failedRun.code, 1)
checkTrue('the failure names its status', failedRun.stdout.includes('turn_ended status=error'))

const runningDir = sandbox('running')
writeTranscript(runningDir, 'cccccccc', '{"type":"assistant"}\n')
check('a working agent is not a problem on a single read', runWatch(['--dir', runningDir]).code, 0)

// The reported defect, end to end: an agent that has written nothing for a long time. Its mtime is
// pushed into the past rather than waited for, so this asserts the sensor and not the test's patience.
const stalledDir = sandbox('stalled')
const stalledFile = writeTranscript(stalledDir, 'dddddddd', '{"type":"assistant"}\n')
const old = new Date(Date.now() - 900_000)
utimesSync(stalledFile, old, old)
const stalledRun = runWatch(['--dir', stalledDir, '--stall', '180'])
check('a silent agent exits 1', stalledRun.code, 1)
checkTrue('it is named STALLED', stalledRun.stdout.includes('STALLED'))
checkTrue('the absent brief is reported rather than invented', stalledRun.stdout.includes('no brief recorded'))

// `--wait` is the whole point: control comes back whether or not the agents do.
const waitDir = sandbox('wait-running')
writeTranscript(waitDir, 'eeeeeeee', '{"type":"assistant"}\n')
const waitRun = runWatch(['--dir', waitDir, '--wait', '--timeout', '1', '--interval', '1'])
check('a bounded wait on an active agent gives control back and exits 1', waitRun.code, 1)
checkTrue('and says so with a marker a caller can block on', waitRun.stdout.includes('PLAYKILN SUBAGENTS TIMEOUT'))

const settledRun = runWatch(['--dir', doneDir, '--wait', '--timeout', '5', '--interval', '1'])
check('a bounded wait on finished agents settles and exits 0', settledRun.code, 0)
checkTrue('with the settled marker', settledRun.stdout.includes('PLAYKILN SUBAGENTS SETTLED'))

check('a missing directory is UNVERIFIED (2), never a pass', runWatch(['--dir', path.join(SANDBOX, 'nowhere')]).code, 2)
check('an unrecorded brief exits 1 rather than printing a path', runWatch(['--brief', 'ffffffff']).code, 1)

rmSync(SANDBOX, { recursive: true, force: true })

console.log('')
console.log(failures === 0 ? 'SUBAGENT LIVENESS TESTS PASSED' : `SUBAGENT LIVENESS TESTS FAILED (${failures})`)
process.exitCode = failures === 0 ? 0 : 1

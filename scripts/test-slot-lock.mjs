#!/usr/bin/env node
/**
 * Regression tests for the per-slot lease (`scripts/lib/slot-lock.mjs`) and for the two defects it
 * was written against in `scripts/lib/gradle.mjs`.
 *
 * Two things this suite must not do, because the recorded failure is a *concurrency* failure and a
 * test that proves it by inspection proves nothing:
 *
 *   - it must produce an actual second consumer of the same slot, which is why the "waits then
 *     succeeds" case runs a real child process holding the real lock file rather than two handles
 *     in one context; and
 *   - it must never run Gradle. The JDK case drives `runGradleTasks` with an overridden resolver and
 *     throws before any spawn, so the assertion holds without a toolchain, a device, or a build.
 *
 * Every claim below is asserted against an observation — a wall-clock measurement, the bytes on
 * disk, or the error that came back — never against the shape of the code.
 */
import { spawn, spawnSync } from 'node:child_process'
import { existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { hostname, tmpdir } from 'node:os'
import path from 'node:path'
import process from 'node:process'
import { pathToFileURL } from 'node:url'

import { runGradleTasks, wrapperFor } from './lib/gradle.mjs'
import {
  acquireSlotLock,
  SLOT_LOCK_ENV,
  SlotLockHeldError,
  slotLockDisabled,
  slotLockHolder,
  slotLockPath,
  withSlotLock
} from './lib/slot-lock.mjs'

let failures = 0
let checks = 0

function check(label, condition, detail) {
  checks += 1
  if (condition) {
    console.log(`PASS ${label}`)
  } else {
    failures += 1
    console.log(`FAIL ${label}${detail ? ` — ${detail}` : ''}`)
  }
}

function sandbox() {
  return mkdtempSync(path.join(tmpdir(), 'kiln-slot-lock-'))
}

function cleanup(dir) {
  rmSync(dir, { recursive: true, force: true })
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function waitFor(predicate, timeoutMs) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (predicate()) return true
    await sleep(10)
  }
  return Boolean(predicate())
}

function childExit(child) {
  if (child.exitCode !== null) return Promise.resolve(child.exitCode)
  return new Promise((resolve) => child.once('exit', (code) => resolve(code)))
}

/** A pid that is not alive: a child that has already exited and been reaped by `spawnSync`. */
function deadPid() {
  return spawnSync(process.execPath, ['-e', '0']).pid
}

/** Mirrors the lock's liveness rule so a test can assert its own precondition. */
function pidAlive(pid) {
  try {
    process.kill(pid, 0)
    return true
  } catch (error) {
    return error.code !== 'ESRCH'
  }
}

function record(overrides = {}) {
  return {
    pid: process.pid,
    hostname: hostname(),
    command: 'fixture',
    startedAt: new Date().toISOString(),
    purpose: 'fixture-holder',
    token: 'fixture-token',
    ...overrides
  }
}

function writeRecord(lockPath, value) {
  mkdirSync(path.dirname(lockPath), { recursive: true })
  writeFileSync(lockPath, `${JSON.stringify(value)}\n`, 'utf8')
}

/**
 * A real second consumer: a child process that takes the lease, reports it, holds it for
 * `SLOT_LOCK_HOLD_MS`, then releases it. The module URL, the slot and the hold are passed through
 * the environment so nothing has to be quoted into a command line.
 */
const HOLDER_SOURCE = `
const { acquireSlotLock } = await import(process.env.SLOT_LOCK_MODULE)
const lock = acquireSlotLock(process.env.SLOT_LOCK_SLOT, { purpose: 'child-holder', timeoutMs: 10000 })
console.log('HELD')
Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, Number(process.env.SLOT_LOCK_HOLD_MS))
lock.release()
console.log('RELEASED')
`
const HOLDER_HOLD_MS = 1500
const SLOT_LOCK_MODULE_URL = pathToFileURL(path.join(import.meta.dirname, 'lib', 'slot-lock.mjs')).href

/* 1. Two acquisitions of the SAME slot: the second waits for the first, and the wait is visible. */
{
  const slotDir = sandbox()
  const holder = spawn(process.execPath, ['--input-type=module', '-e', HOLDER_SOURCE], {
    env: {
      ...process.env,
      SLOT_LOCK_MODULE: SLOT_LOCK_MODULE_URL,
      SLOT_LOCK_SLOT: slotDir,
      SLOT_LOCK_HOLD_MS: String(HOLDER_HOLD_MS)
    },
    stdio: ['ignore', 'pipe', 'pipe']
  })
  let holderOut = ''
  holder.stdout.on('data', (chunk) => {
    holderOut += String(chunk)
  })

  const sawHolder = await waitFor(() => existsSync(slotLockPath(slotDir)), 8000)
  const observed = slotLockHolder(slotDir)
  check('precondition: a real second process holds the same slot', sawHolder, 'the child never took the lock')
  check(
    'the holder can be read without touching its lock',
    observed?.purpose === 'child-holder' && observed?.pid !== process.pid,
    JSON.stringify(observed)
  )

  const contendedFrom = Date.now()
  let contended = null
  let contendedError = null
  try {
    contended = acquireSlotLock(slotDir, { purpose: 'parent-waiter', timeoutMs: 10000 })
  } catch (error) {
    contendedError = error
  }
  const contendedMs = Date.now() - contendedFrom

  check('the second consumer acquires the slot once the holder releases', contended !== null, contendedError?.message)
  check('the wait is observable rather than immediate', contendedMs >= 500, `waited only ${contendedMs}ms`)
  contended?.release()

  const exitCode = await childExit(holder)
  await sleep(50)
  check('the child holder released before exiting', exitCode === 0 && holderOut.includes('RELEASED'), `exit ${exitCode}, output ${JSON.stringify(holderOut)}`)

  const freshDir = sandbox()
  const quickFrom = Date.now()
  const quick = acquireSlotLock(freshDir, { purpose: 'uncontended' })
  const quickMs = Date.now() - quickFrom
  quick.release()
  check(
    'an uncontended acquire is measurably faster than a contended one',
    quickMs < 300 && quickMs < contendedMs,
    `uncontended ${quickMs}ms vs contended ${contendedMs}ms`
  )

  cleanup(slotDir)
  cleanup(freshDir)
}

/* 2. A short timeout on a held slot refuses with SLOT_LOCK_HELD, naming the holder's pid and purpose. */
{
  const slotDir = sandbox()
  const held = acquireSlotLock(slotDir, { purpose: 'gradle-proof' })
  let refusal = null
  try {
    acquireSlotLock(slotDir, { purpose: 'second-consumer', timeoutMs: 0 })
  } catch (error) {
    refusal = error
  }
  check('a held slot with an expired deadline refuses instead of waiting', refusal !== null, 'the second acquire succeeded')
  check('the refusal is a SlotLockHeldError', refusal instanceof SlotLockHeldError, refusal?.name)
  check('the refusal carries the stable SLOT_LOCK_HELD code', refusal?.code === 'SLOT_LOCK_HELD', refusal?.code)
  check('the refusal names the holder pid', Boolean(refusal?.message.includes(`pid ${process.pid}`)), refusal?.message)
  check('the refusal names the holder purpose', Boolean(refusal?.message.includes('gradle-proof')), refusal?.message)
  check('the refusal names the remedy (the lock path)', Boolean(refusal?.message.includes(held.path)), refusal?.message)
  check('the refusal exposes the holder record', refusal?.holder?.purpose === 'gradle-proof', JSON.stringify(refusal?.holder))
  held.release()
  cleanup(slotDir)
}

/* 3. Two DIFFERENT slots proceed concurrently: proven by timing, not by inspection. */
{
  const slotA = sandbox()
  const slotB = sandbox()
  const lockA = acquireSlotLock(slotA, { purpose: 'slot-a' })
  const from = Date.now()
  let lockB = null
  let error = null
  try {
    lockB = acquireSlotLock(slotB, { purpose: 'slot-b', timeoutMs: 0 })
  } catch (caught) {
    error = caught
  }
  const elapsed = Date.now() - from
  check('two different slots derive different lock files', lockA.path !== lockB?.path, `${lockA.path} vs ${lockB?.path}`)
  check('a second slot is acquired while the first is held', lockB !== null, error?.message)
  check('the second slot waited for nothing', elapsed < 300, `took ${elapsed}ms`)
  lockA.release()
  lockB?.release()
  cleanup(slotA)
  cleanup(slotB)
}

/* 4. A lock recording a dead pid is reclaimed — and leaves exactly one holder. */
{
  const slotDir = sandbox()
  const lockPath = slotLockPath(slotDir)
  const pid = deadPid()
  check('precondition: the fixture pid is not alive', !pidAlive(pid), `pid ${pid} is alive`)
  writeRecord(lockPath, record({ pid, purpose: 'crashed-holder', token: 'dead-token' }))

  const reclaimer = acquireSlotLock(slotDir, { purpose: 'reclaimer' })
  const after = slotLockHolder(slotDir)
  check('a lock held by a dead pid is reclaimed, not deadlocked', after?.purpose === 'reclaimer', JSON.stringify(after))
  check('the reclaimed lock records this process', after?.pid === process.pid, JSON.stringify(after))
  check('the reclaim leaves no takeover marker behind', !existsSync(`${lockPath}.takeover`))

  let second = null
  try {
    acquireSlotLock(slotDir, { timeoutMs: 0 })
  } catch (error) {
    second = error
  }
  check(
    'after the reclaim exactly one holder exists',
    second?.code === 'SLOT_LOCK_HELD' && second?.holder?.token === after?.token,
    JSON.stringify(second?.holder)
  )
  reclaimer.release()
  cleanup(slotDir)
}

/* 5. A lock whose pid is ALIVE is never reclaimed, however old it is. */
{
  const slotDir = sandbox()
  const lockPath = slotLockPath(slotDir)
  writeRecord(lockPath, record({ startedAt: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(), purpose: 'live-holder', token: 'live-token' }))

  let refusal = null
  try {
    acquireSlotLock(slotDir, { timeoutMs: 0 })
  } catch (error) {
    refusal = error
  }
  const after = slotLockHolder(slotDir)
  check('a lock whose pid is alive is not reclaimed, however old', refusal?.code === 'SLOT_LOCK_HELD', refusal?.message)
  check('the live holder keeps its own lock file', after?.token === 'live-token' && after?.purpose === 'live-holder', JSON.stringify(after))
  cleanup(slotDir)
}

/* 5b. A record written on another host is never reclaimed: a foreign pid proves nothing here. */
{
  const slotDir = sandbox()
  const lockPath = slotLockPath(slotDir)
  const pid = deadPid()
  writeRecord(lockPath, record({ pid, hostname: 'some-other-machine', purpose: 'foreign-holder', token: 'foreign-token' }))

  let refusal = null
  try {
    acquireSlotLock(slotDir, { timeoutMs: 0 })
  } catch (error) {
    refusal = error
  }
  check('a dead pid from another host is not treated as a stale lock', refusal?.code === 'SLOT_LOCK_HELD', refusal?.message)
  check('the foreign holder keeps its own lock file', slotLockHolder(slotDir)?.token === 'foreign-token')
  cleanup(slotDir)
}

/* 6. `withSlotLock` releases on a throwing callback, so a crash does not lock the slot forever. */
{
  const slotDir = sandbox()
  let propagated = null
  try {
    withSlotLock(slotDir, { purpose: 'throwing-build' }, () => {
      throw new Error('boom')
    })
  } catch (error) {
    propagated = error
  }
  check('a throwing callback propagates its own error', propagated?.message === 'boom', propagated?.message)
  check('a throwing callback leaves no lock file', !existsSync(slotLockPath(slotDir)))
  const after = acquireSlotLock(slotDir, { purpose: 'after-crash', timeoutMs: 0 })
  check('the slot is acquirable after a throwing callback', after.acquired === true)
  after.release()

  const returned = withSlotLock(slotDir, { purpose: 'returning' }, (lock) => lock.acquired)
  check('withSlotLock returns the callback result and releases', returned === true && !existsSync(slotLockPath(slotDir)))
  check('withSlotLock accepts the callback as its second argument', withSlotLock(slotDir, (lock) => lock.purpose === 'unspecified') === true)
  cleanup(slotDir)
}

/* 7. Releasing a lock this handle does not hold is a no-op, never a deletion of the real holder. */
{
  const slotDir = sandbox()
  const ours = acquireSlotLock(slotDir, { purpose: 'ours' })
  // Exactly the shape of a reclaim: the file now records a different holder, with a different token.
  writeRecord(ours.path, record({ purpose: 'other-holder', token: 'other-token', command: 'other' }))

  ours.release()
  check('releasing a lock another holder owns is a no-op', slotLockHolder(slotDir)?.token === 'other-token', JSON.stringify(slotLockHolder(slotDir)))
  check('the other holder\'s lock file survives the stale release', existsSync(ours.path))
  ours.release()
  check('a double release still leaves the other holder alone', slotLockHolder(slotDir)?.token === 'other-token')
  cleanup(slotDir)
}

/* 8. `PLAYKILN_SLOT_LOCK=off` skips acquisition — and it is the only thing that does. */
{
  const slotDir = sandbox()
  const held = acquireSlotLock(slotDir, { purpose: 'real-holder' })
  const heldToken = slotLockHolder(slotDir)?.token
  const previous = process.env[SLOT_LOCK_ENV]

  try {
    process.env[SLOT_LOCK_ENV] = 'on'
    let refused = null
    try {
      acquireSlotLock(slotDir, { timeoutMs: 0 })
    } catch (error) {
      refused = error
    }
    check('a value other than off does not skip the lock', refused?.code === 'SLOT_LOCK_HELD', refused?.message)

    process.env[SLOT_LOCK_ENV] = '  OFF  '
    const skipped = acquireSlotLock(slotDir, { purpose: 'skipped' })
    check('PLAYKILN_SLOT_LOCK=off skips acquisition', skipped.acquired === false, JSON.stringify(skipped))
    check('the skipped handle holds nothing to release', skipped.pid === null)
    check('the skip leaves the real holder\'s lock untouched', slotLockHolder(slotDir)?.token === heldToken)
    skipped.release()
    check('releasing a skipped handle touches nothing', existsSync(slotLockPath(slotDir)))
  } finally {
    if (previous === undefined) delete process.env[SLOT_LOCK_ENV]
    else process.env[SLOT_LOCK_ENV] = previous
  }

  let refusedWithout = null
  try {
    acquireSlotLock(slotDir, { timeoutMs: 0 })
  } catch (error) {
    refusedWithout = error
  }
  check('the escape hatch is the only way to skip acquisition', refusedWithout?.code === 'SLOT_LOCK_HELD', refusedWithout?.message)

  check(
    'slotLockDisabled recognises off and nothing else',
    slotLockDisabled({}) === false &&
      slotLockDisabled({ [SLOT_LOCK_ENV]: '1' }) === false &&
      slotLockDisabled({ [SLOT_LOCK_ENV]: 'off' }) === true
  )
  held.release()
  cleanup(slotDir)
}

/* 9. `runGradleTasks` refuses to spawn Gradle when no supported JDK resolves. */
{
  const slotDir = sandbox()
  // The wrapper must exist for the call to reach the JDK check; the name is derived, never written
  // as a literal, so the factory's own "wrappers are invoked only through lib/gradle.mjs" tripwire
  // does not have to be weakened for this test to exist.
  writeFileSync(wrapperFor(slotDir, 'linux'), '#!/bin/sh\nexit 0\n', 'utf8')

  let thrown = null
  try {
    runGradleTasks(slotDir, ['assembleDebug'], { platform: 'linux', javaResolver: () => null })
  } catch (error) {
    thrown = error
  }
  check('Gradle is not spawned when no supported JDK resolves', thrown !== null, 'runGradleTasks returned instead of refusing')
  check('the refusal is the JDK blocker, not a wrapper error', thrown?.code === 'JAVA_UNRESOLVED', thrown?.message)
  check('the refusal names what to set (PLAYKILN_JAVA_HOME)', Boolean(thrown?.message.includes('PLAYKILN_JAVA_HOME')), thrown?.message)
  check('the refusal names what was searched', Boolean(thrown?.message.includes('candidate home')), thrown?.message)
  check('the refusal is thrown before the slot lease is taken', !existsSync(slotLockPath(slotDir)))

  // The harness seam, exercised deliberately: a caller that replaced the spawner replaced the build,
  // so there is no Gradle process for a JDK to pin. This is the shape `scripts/selftest.mjs` uses,
  // and it is why its existing one-invocation assertions stay satisfiable.
  const harnessCalls = []
  const harnessRun = runGradleTasks(slotDir, ['assembleDebug'], {
    platform: 'linux',
    javaResolver: () => null,
    spawn: (command, args, options) => {
      harnessCalls.push({ command, args, options })
      return { status: 0, stdout: 'ok', stderr: '' }
    }
  })
  check('an injected spawner is the harness path and still runs one invocation', harnessCalls.length === 1, `got ${harnessCalls.length}`)
  check('the harness path pins no JDK and inherits nothing it did not receive', harnessRun.javaHome === null && harnessRun.javaMajor === null)

  // And when the resolver does answer, the child process receives the pinned JAVA_HOME.
  const pinnedCalls = []
  const pinnedRun = runGradleTasks(slotDir, ['assembleDebug'], {
    platform: 'linux',
    javaResolver: () => ({ home: '/opt/jdk-17', major: 17, source: 'stub' }),
    spawn: (command, args, options) => {
      pinnedCalls.push(options)
      return { status: 0, stdout: '', stderr: '' }
    }
  })
  check(
    'a resolved JDK is passed through to the spawned process',
    pinnedCalls[0]?.env?.JAVA_HOME === '/opt/jdk-17' && pinnedRun.javaMajor === 17,
    JSON.stringify(pinnedCalls[0]?.env || {})
  )
  check('the Gradle run released the slot lease on the way out', !existsSync(slotLockPath(slotDir)))
  cleanup(slotDir)
}

console.log(`\n${checks - failures}/${checks} checks passed`)
process.exit(failures === 0 ? 0 : 1)

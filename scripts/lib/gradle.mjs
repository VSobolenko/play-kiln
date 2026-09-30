#!/usr/bin/env node
/**
 * Single source of truth for invoking a Gradle wrapper.
 *
 * Why this module exists
 * ----------------------
 * `prepare-release.mjs` used to launch `assembleDebug`, `assembleRelease`, and
 * `bundleRelease` as three separate Gradle processes. Each one started a JVM,
 * restored the configuration cache, and rebuilt a task graph for work that is
 * largely shared. Measured on a warm cache, collapsing them into one invocation
 * went from 6.3s to 2.3s; the gap is wider on a real build because dependency
 * resolution and configuration then happen once instead of three times.
 *
 * Rules encoded here so they cannot drift back in:
 *
 *   1. All tasks for one build go to a **single** invocation.
 *   2. `--no-daemon` is never added. It forces a single-use daemon and throws away
 *      the JVM, configuration, and task-graph reuse that makes the second build of
 *      a session cheap.
 *   3. The JDK is pinned via `scripts/lib/java.mjs`. A changing JDK forces a new
 *      Gradle daemon and a cold rebuild (measured: 2.4s pinned, 70.1s unpinned).
 *      When nothing supported resolves, this module **throws before spawning**;
 *      falling back to the ambient PATH JDK is exactly the 70-second defect, and
 *      `scripts/lib/java.mjs` says callers must surface that as a blocker.
 *   4. The slot's lease (`scripts/lib/slot-lock.mjs`) is held around the process.
 *      This is the repo's single Gradle route, so taking the lease here covers
 *      every Gradle consumer at once: a second consumer waits, or is refused with
 *      `SLOT_LOCK_HELD` naming the holder, instead of dying on the Gradle
 *      wrapper's own `GRADLE_USER_HOME` lock mid-write (recorded in
 *      `harness/lessons.md`, 2026-09-26, and `.cursor/rules/build-speed.mdc`).
 *
 * `spawn` is injectable so `scripts/selftest.mjs` can assert the one-invocation
 * contract with a fake process instead of trusting this comment.
 */

import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { candidateJavaHomes, MIN_MAJOR_SUPPORTED, resolveJavaHome } from './java.mjs'
import { withSlotLock } from './slot-lock.mjs'

/** Tasks that produce every artifact `prepare-release.mjs` stages into `dist/`. */
export const RELEASE_TASKS = Object.freeze(['assembleDebug', 'assembleRelease', 'bundleRelease'])

/** Flags that must never appear on a factory Gradle invocation. */
export const FORBIDDEN_GRADLE_FLAGS = Object.freeze(['--no-daemon'])

/** Argument vector for one Gradle run. */
export function gradleArgs(tasks, { console: consoleMode = 'plain' } = {}) {
  const list = normalizeTasks(tasks)
  for (const flag of FORBIDDEN_GRADLE_FLAGS) {
    if (list.includes(flag)) throw new Error(`gradleArgs must not receive ${flag}`)
  }
  return [...list, `--console=${consoleMode}`]
}

/** Path to the wrapper script for a slot on a given platform. */
export function wrapperFor(slotDir, platform = process.platform) {
  return path.join(slotDir, platform === 'win32' ? 'gradlew.bat' : 'gradlew')
}

function normalizeTasks(tasks) {
  const list = (Array.isArray(tasks) ? tasks : [tasks]).filter(Boolean)
  if (list.length === 0) throw new Error('at least one Gradle task is required')
  return list
}

/**
 * Run every task in ONE Gradle process, under one deterministic JDK, holding the
 * slot's lease.
 *
 * `javaHome` controls JDK selection: omit it to resolve automatically **and to
 * fail closed** when nothing supported resolves, pass an explicit path to force
 * one, or pass `null` to inherit the ambient environment unchanged — the explicit
 * test-only skip that `scripts/selftest.mjs` exercises.
 *
 * Two seams exist for the regression tests, and neither is a production dodge:
 * `spawn` replaces the process this module would launch (`scripts/selftest.mjs`
 * drives the one-invocation contract that way), and when a caller has replaced
 * the spawner there is no Gradle process for a JDK to pin, so the JDK
 * precondition is not enforced on that path; `javaResolver` replaces the JDK
 * resolver, so the fail-closed throw can be driven offline. Every production
 * caller — `verify-slot.mjs`, `prepare-release.mjs`, `run-slot-tests.mjs` — uses
 * the default spawner and the real resolver, so neither seam can be reached from
 * a factory route.
 *
 * Returns the command, the exact argument vector, the spawn result, the resolved
 * JDK, and the wall-clock time so callers can report build cost without
 * duplicating timing. The caller owns interpretation: a non-zero `result.status`
 * or `result.error` means the build failed.
 */
export function runGradleTasks(
  slotDir,
  tasks,
  { spawn = spawnSync, platform = process.platform, env = process.env, javaHome, javaResolver = resolveJavaHome } = {}
) {
  const list = normalizeTasks(tasks)
  const wrapper = wrapperFor(slotDir, platform)
  if (!existsSync(wrapper)) throw new Error(`${path.basename(wrapper)} not found`)
  const args = gradleArgs(list)

  // A caller that replaced the spawner replaced the build itself, so there is no
  // process to pin a JDK for. See the JSDoc above: this is the harness seam, not
  // an option any factory route passes.
  const harnessProcess = spawn !== spawnSync

  // Resolve the JDK so Gradle reuses one daemon across runs. Letting the JDK vary
  // costs a full cold build: measured 2.4s under JDK 17 versus 70.1s when an
  // ambient JDK 24 took over the build. When nothing supported resolves, the
  // ambient environment is *not* a fallback — it is that defect — so refuse.
  const resolved =
    javaHome === undefined ? javaResolver({ env, platform, spawn }) : javaHome === null ? null : { home: javaHome }

  if (javaHome === undefined && !resolved?.home && !harnessProcess) {
    throw unresolvedJavaError({ env, platform })
  }

  const spawnEnv = resolved?.home ? { ...env, JAVA_HOME: resolved.home } : { ...env }

  const started = Date.now()
  // One lease per slot, around the process only. The second consumer of a slot
  // waits here for up to two minutes, then gets `SLOT_LOCK_HELD` naming the
  // holder and the remedy — never the wrapper's own `.lck` crash, which kills a
  // writer mid-file and leaves a tree that does not compile.
  const result = withSlotLock(slotDir, { purpose: `Gradle ${list.join(' ')}`, env }, () =>
    spawn(wrapper, args, {
      cwd: slotDir,
      encoding: 'utf8',
      shell: platform === 'win32',
      windowsHide: true,
      env: spawnEnv
    })
  )
  return {
    command: wrapper,
    args,
    tasks: list,
    result,
    elapsedMs: Date.now() - started,
    javaHome: resolved?.home || null,
    javaSource: resolved?.source || null,
    javaMajor: resolved?.major ?? null
  }
}

/**
 * The blocker thrown when no supported JDK resolves.
 *
 * It names what was searched and what to set, because the alternative — quietly
 * inheriting the ambient JDK — is silent: the build succeeds and simply takes
 * 70 seconds instead of 2.4, which no gate reports. `PLAYKILN_JAVA_HOME` is the
 * override `scripts/lib/java.mjs` documents.
 */
function unresolvedJavaError({ env, platform }) {
  const searched = candidateJavaHomes({ env, platform })
  const error = new Error(
    `no supported JDK (major ${MIN_MAJOR_SUPPORTED} or newer) was resolved, so Gradle would inherit ` +
      'whatever `java` happens to be on PATH — the measured 70-second cold-daemon build ' +
      '(.cursor/rules/build-speed.mdc). Refusing to spawn. Searched PLAYKILN_JAVA_HOME, JAVA_HOME, ' +
      `and ${searched.length} candidate home(s): ${searched.join(', ') || '(none found)'}. ` +
      `Set PLAYKILN_JAVA_HOME to a JDK ${MIN_MAJOR_SUPPORTED}+ home and re-run.`
  )
  error.code = 'JAVA_UNRESOLVED'
  error.candidates = searched
  return error
}

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
 * Two rules are encoded here so they cannot drift back in:
 *
 *   1. All tasks for one build go to a **single** invocation.
 *   2. `--no-daemon` is never added. It forces a single-use daemon and throws away
 *      the JVM, configuration, and task-graph reuse that makes the second build of
 *      a session cheap.
 *   3. The JDK is pinned via `scripts/lib/java.mjs`. A changing JDK forces a new
 *      Gradle daemon and a cold rebuild (measured: 2.4s pinned, 70.1s unpinned).
 *
 * `spawn` is injectable so `scripts/selftest.mjs` can assert the one-invocation
 * contract with a fake process instead of trusting this comment.
 */

import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { resolveJavaHome } from './java.mjs'

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
 * Run every task in ONE Gradle process, under one deterministic JDK.
 *
 * `javaHome` controls JDK selection: omit it to resolve automatically, pass an
 * explicit path to force one, or pass `null` to inherit the ambient environment
 * unchanged (used by the regression test so it never probes the real filesystem).
 *
 * Returns the command, the exact argument vector, the spawn result, the resolved
 * JDK, and the wall-clock time so callers can report build cost without
 * duplicating timing. The caller owns interpretation: a non-zero `result.status`
 * or `result.error` means the build failed.
 */
export function runGradleTasks(
  slotDir,
  tasks,
  { spawn = spawnSync, platform = process.platform, env = process.env, javaHome } = {}
) {
  const list = normalizeTasks(tasks)
  const wrapper = wrapperFor(slotDir, platform)
  if (!existsSync(wrapper)) throw new Error(`${path.basename(wrapper)} not found`)
  const args = gradleArgs(list)

  // Pin the JDK so Gradle reuses one daemon across runs. Letting the JDK vary
  // costs a full cold build: measured 2.4s under JDK 17 versus 70.1s when an
  // ambient JDK 24 took over the build.
  const resolved = javaHome === undefined ? resolveJavaHome({ platform, spawn }) : javaHome === null ? null : { home: javaHome }
  const spawnEnv = resolved?.home ? { ...env, JAVA_HOME: resolved.home } : { ...env }

  const started = Date.now()
  const result = spawn(wrapper, args, {
    cwd: slotDir,
    encoding: 'utf8',
    shell: platform === 'win32',
    windowsHide: true,
    env: spawnEnv
  })
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

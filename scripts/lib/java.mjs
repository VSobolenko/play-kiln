#!/usr/bin/env node
/**
 * Deterministic JDK selection for factory Gradle runs.
 *
 * Why this module exists
 * ----------------------
 * Gradle keeps one daemon per JDK. When the JDK changes between runs, the old
 * daemon is not reused: the project is re-configured, incremental state is
 * invalidated, and the build restarts from cold. Measured on one slot:
 *
 *   JAVA_HOME = JDK 17              2.4 s
 *   JAVA_HOME = JDK 17 (repeat)     2.9 s
 *   JAVA_HOME unset (PATH JDK 24)  70.1 s
 *
 * The factory therefore resolves one JDK once and passes it to every Gradle
 * invocation, instead of inheriting whatever happens to be on PATH.
 *
 * Preference order, so the choice is stable across runs:
 *   1. `PLAYKILN_JAVA_HOME` вЂ” explicit override for this factory
 *   2. `JAVA_HOME`          вЂ” the operator's own choice, if it is supported
 *   3. a discovered JDK, preferring exactly 17 (AGP 9's documented target)
 *
 * A JDK is only accepted if `java -version` reports major >= MIN_MAJOR, so a
 * stray JRE or an unreadable install is skipped rather than selected.
 */

import { spawnSync } from 'node:child_process'
import { existsSync, readdirSync } from 'node:fs'
import path from 'node:path'

/** Minimum JDK major version AGP 9 supports. */
export const MIN_MAJOR_SUPPORTED = 17

/** Directory names searched on Windows, in order. */
const WINDOWS_VENDOR_DIRS = [
  'C:\\Program Files\\Java',
  'C:\\Program Files\\Eclipse Adoptium',
  'C:\\Program Files\\Microsoft',
  'C:\\Program Files\\Amazon Corretto',
  'C:\\Program Files\\Zulu',
  'C:\\Program Files\\BellSoft'
]

/** Directory names searched on Unix-like systems, in order. */
const UNIX_VENDOR_DIRS = ['/usr/lib/jvm', '/Library/Java/JavaVirtualMachines']

function stripQuotes(value) {
  return typeof value === 'string' ? value.replace(/^"|"$/g, '') : value
}

function javaExecutable(home, platform) {
  return path.join(home, 'bin', platform === 'win32' ? 'java.exe' : 'java')
}

/**
 * Major version reported by the JDK at `home`, or null when it is not a usable
 * JDK. Handles the legacy `1.8.0_392` shape by reading the second component.
 */
export function javaMajorOf(home, { spawn = spawnSync, platform = process.platform } = {}) {
  if (!home) return null
  const executable = javaExecutable(stripQuotes(home), platform)
  if (!existsSync(executable)) return null
  let result
  try {
    result = spawn(executable, ['-version'], { encoding: 'utf8', windowsHide: true })
  } catch {
    return null
  }
  const text = `${result?.stdout || ''}${result?.stderr || ''}`
  const match = text.match(/version "(\d+)(?:\.(\d+))?/)
  if (!match) return null
  const major = Number(match[1]) === 1 ? Number(match[2]) : Number(match[1])
  return Number.isFinite(major) ? major : null
}

/** Candidate JDK homes to probe, in preference order, without duplicates. */
export function candidateJavaHomes({ env = process.env, platform = process.platform } = {}) {
  const homes = []
  const push = (value) => {
    const clean = stripQuotes(value)
    if (clean && !homes.includes(clean)) homes.push(clean)
  }

  push(env.PLAYKILN_JAVA_HOME)
  push(env.JAVA_HOME)

  const vendorDirs = platform === 'win32' ? WINDOWS_VENDOR_DIRS : UNIX_VENDOR_DIRS
  for (const base of vendorDirs) {
    if (!existsSync(base)) continue
    let entries = []
    try {
      entries = readdirSync(base)
    } catch {
      continue
    }
    for (const name of entries.sort()) push(path.join(base, name))
  }

  return homes
}

/**
 * Resolve the JDK the factory should use for Gradle.
 *
 * Returns `{ home, major, source }`, or null when no supported JDK was found вЂ”
 * callers must surface that as a blocking error rather than falling back to
 * whatever `java` is on PATH.
 */
export function resolveJavaHome({ env = process.env, platform = process.platform, spawn = spawnSync } = {}) {
  const homes = candidateJavaHomes({ env, platform })

  const explicit = [stripQuotes(env.PLAYKILN_JAVA_HOME), stripQuotes(env.JAVA_HOME)]
  for (let index = 0; index < explicit.length; index += 1) {
    const home = explicit[index]
    if (!home) continue
    const major = javaMajorOf(home, { spawn, platform })
    if (major !== null && major >= MIN_MAJOR_SUPPORTED) {
      return { home, major, source: index === 0 ? 'PLAYKILN_JAVA_HOME' : 'JAVA_HOME' }
    }
  }

  const discovered = []
  for (const home of homes) {
    const major = javaMajorOf(home, { spawn, platform })
    if (major !== null && major >= MIN_MAJOR_SUPPORTED) discovered.push({ home, major, source: 'discovered' })
  }
  if (discovered.length === 0) return null

  // Prefer exactly 17, then the lowest supported major; break ties by path so the
  // choice is identical on every run and the daemon survives between builds.
  discovered.sort((a, b) => {
    const distance = Math.abs(a.major - MIN_MAJOR_SUPPORTED) - Math.abs(b.major - MIN_MAJOR_SUPPORTED)
    if (distance !== 0) return distance
    if (a.major !== b.major) return a.major - b.major
    return a.home.localeCompare(b.home)
  })
  return discovered[0]
}

#!/usr/bin/env node
/**
 * Where a factory-spawned process is allowed to write temporary files.
 *
 * Why this module exists
 * ----------------------
 * A Gradle build writes a lot of scratch: Kotlin compiler daemon sessions, R8
 * working sets, `workerMain` jars, incremental caches. By default every one of
 * them lands in the machine's system temp directory (`%TEMP%` on Windows,
 * `$TMPDIR` elsewhere), outside the repository. The owner's instruction on
 * 2026-10-01 is unambiguous: a factory process may make as much mess as it likes
 * **inside the project**, and must not write into the system at all — the
 * previous behaviour filled the user's temp directory and slowed the machine.
 *
 * The rule this encodes:
 *
 *   1. Every temporary file a spawned build process writes lives under
 *      `.kiln/scratch/` in this repository, which is gitignored and disposable.
 *   2. The system temp directory is never a target. Java derives
 *      `java.io.tmpdir` from `TEMP`/`TMP` at JVM start, so redirecting those two
 *      variables redirects the Gradle launcher JVM, the Kotlin compiler daemon it
 *      forks and every `workerMain` process beneath it.
 *   3. Nothing here is a source of product state: deleting `.kiln/scratch/` at
 *      any moment, including mid-build, is always safe.
 *
 * What this module cannot claim, stated because the limit is real: a **reused**
 * Gradle daemon keeps the environment it was first started with, so a daemon
 * that began before this module existed continues writing to the old location
 * until it is stopped. `--no-daemon` is not an acceptable fix (it costs the
 * measured 6.3s → 2.3s regression `build-speed.mdc` records), so the honest
 * remedy is to stop a stale daemon, not to fork a fresh one per build.
 *
 * `GRADLE_USER_HOME` is deliberately **not** redirected: it is the shared
 * dependency cache, not scratch, and moving it would re-download every
 * dependency for every repository.
 */

import { mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

/** Repository root, derived from this file's own location (`scripts/lib/scratch.mjs`). */
export const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')

/** The one directory a factory process may treat as scratch. Gitignored with all of `.kiln/`. */
export const SCRATCH_DIR = path.join(ROOT, '.kiln', 'scratch')

// Created at import time, so `SCRATCH_DIR` is always a usable parent for `mkdtempSync` and every
// caller can name it without repeating a `mkdirSync`. This is the module's only side effect, and it
// is deliberate: a scratch directory that might not exist is a scratch directory a caller skips.
mkdirSync(SCRATCH_DIR, { recursive: true })

/**
 * The scratch directory for one purpose, created if needed.
 *
 * A named subdirectory per consumer keeps two concurrent builds from sharing a
 * `java.io.tmpdir`, which is the same contention `scripts/lib/slot-lock.mjs`
 * prevents one level up.
 */
export function scratchDirFor(name) {
  const dir = path.join(SCRATCH_DIR, name)
  mkdirSync(dir, { recursive: true })
  return dir
}

/**
 * Environment for a spawned build process, with its temporary directory redirected
 * into the project.
 *
 * `TEMP`/`TMP` are the Windows lever; `TMPDIR` covers macOS and Linux so the same
 * contract holds on every platform a slot may be built on. All three are set
 * unconditionally rather than per-platform, because a stray inherited `TEMP` on
 * one platform is exactly the leak this prevents.
 */
export function scratchEnv(env = process.env, name = 'gradle') {
  const dir = scratchDirFor(name)
  return { ...env, TEMP: dir, TMP: dir, TMPDIR: dir }
}

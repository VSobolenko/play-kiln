#!/usr/bin/env node
/**
 * Regression self-test for PlayKiln's own factory scripts.
 *
 *   node scripts/selftest.mjs          # run every check, delete the sandbox
 *   node scripts/selftest.mjs --keep   # keep the sandbox for inspection
 *
 * Why this exists
 * ---------------
 * `new-slot.mjs` once copied the scaffold with an unfiltered `cpSync`, dragging
 * ~145 MB of Gradle build output into every new slot and then running text
 * substitution over binary `.so`/`.aab` files. Nothing caught it until a slot was
 * created by hand. Reading the code was not enough; the defect was only visible by
 * *running* it.
 *
 * This suite therefore runs the real scripts against a throwaway sandbox copy of
 * the scaffold and asserts the slot contract. It imports the exclusion predicate
 * and the namespace rule from the factory modules, so the test cannot drift away
 * from the implementation it guards.
 *
 * The sandbox is a temp directory containing `scripts/` + `scaffold/`. Because
 * `new-slot.mjs` resolves its root from its own file location, everything it
 * writes stays inside the temp directory and the real repo is untouched.
 */

import { spawnSync } from 'node:child_process'
import {
  copyFileSync,
  cpSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { isExcludedScaffoldPath, walkFiles } from './new-slot.mjs'
import { applicationIdForSlug, applicationIdPattern, packagePrefix, scaffoldPackage } from './lib/namespaces.mjs'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const SCAFFOLD = join(ROOT, 'scaffold')
const SLOT_SCAFFOLD = join(SCAFFOLD, 'android-compose')
const KEEP = process.argv.includes('--keep')

let failed = false

function line(status, message) {
  if (status === 'FAIL') failed = true
  console.log(`  ${status.padEnd(9)} ${message}`)
}

function check(ok, message) {
  line(ok ? 'PASS' : 'FAIL', message)
  return ok
}

function section(title) {
  console.log(`\n### ${title}`)
}

/** Format a short list of offending relative paths for a failure message. */
function detail(names) {
  if (names.length === 0) return ''
  const shown = names.slice(0, 3).join(', ')
  return ` -> ${shown}${names.length > 3 ? ` (+${names.length - 3} more)` : ''}`
}

/** Read every file under a directory as text, skipping anything with NUL bytes. */
function textFiles(directory) {
  const found = []
  for (const file of walkFiles(directory)) {
    const bytes = readFileSync(file)
    if (bytes.includes(0)) continue
    found.push([relative(directory, file), bytes.toString('utf8')])
  }
  return found
}

function createSandbox() {
  const sandbox = mkdtempSync(join(tmpdir(), 'playkiln-selftest-'))
  mkdirSync(join(sandbox, 'scripts', 'lib'), { recursive: true })
  copyFileSync(join(ROOT, 'scripts', 'new-slot.mjs'), join(sandbox, 'scripts', 'new-slot.mjs'))
  copyFileSync(
    join(ROOT, 'scripts', 'lib', 'namespaces.mjs'),
    join(sandbox, 'scripts', 'lib', 'namespaces.mjs')
  )
  cpSync(SCAFFOLD, join(sandbox, 'scaffold'), { recursive: true })
  return sandbox
}

function runNewSlot(sandbox, args) {
  const result = spawnSync(
    process.execPath,
    [join(sandbox, 'scripts', 'new-slot.mjs'), ...args],
    { cwd: sandbox, encoding: 'utf8', windowsHide: true }
  )
  return {
    status: result.status,
    output: `${result.stdout || ''}${result.stderr || ''}`
  }
}

function applicationIdIn(gradleText) {
  return gradleText.match(/\bapplicationId\s*=\s*["']([^"']+)["']/)?.[1]
}

// --------------------------------------------------------------------------
// 1. The scaffold must ship sources only.
// --------------------------------------------------------------------------

section('scaffold purity')

const scaffoldFiles = existsSync(SCAFFOLD) ? walkFiles(SCAFFOLD) : []
check(scaffoldFiles.length > 0, `scaffold contains files (${scaffoldFiles.length})`)

const scaffoldLeaks = scaffoldFiles
  .map((file) => relative(SCAFFOLD, file))
  .filter(isExcludedScaffoldPath)
check(scaffoldLeaks.length === 0, `scaffold carries no build state${detail(scaffoldLeaks)}`)

check(
  existsSync(join(SLOT_SCAFFOLD, 'gradle', 'wrapper', 'gradle-wrapper.jar')),
  'scaffold ships gradle-wrapper.jar (a slot cannot build without it)'
)
check(
  existsSync(join(SLOT_SCAFFOLD, 'gradle', 'wrapper', 'gradle-wrapper.properties')),
  'scaffold ships gradle-wrapper.properties'
)
check(
  !existsSync(join(SLOT_SCAFFOLD, 'keystore.properties')),
  'scaffold ships no keystore.properties'
)
check(
  existsSync(join(SLOT_SCAFFOLD, 'keystore.properties.example')),
  'scaffold ships keystore.properties.example'
)

const scaffoldIgnore = existsSync(join(SLOT_SCAFFOLD, '.gitignore'))
  ? readFileSync(join(SLOT_SCAFFOLD, '.gitignore'), 'utf8').split(/\r?\n/).map((item) => item.trim())
  : []
for (const entry of ['keystore.properties', '*.jks', '*.keystore', '.env', 'local.properties']) {
  check(scaffoldIgnore.includes(entry), `scaffold .gitignore covers ${entry}`)
}

// --------------------------------------------------------------------------
// 2. The namespace rule and the scaffold must agree.
// --------------------------------------------------------------------------

section('namespace rule')

check(
  applicationIdPattern.test(applicationIdForSlug('smoke')),
  `accepts ${applicationIdForSlug('smoke')}`
)
check(
  applicationIdPattern.test(applicationIdForSlug('my-app')),
  `drops dashes: my-app -> ${applicationIdForSlug('my-app')}`
)
check(!applicationIdPattern.test('com.playkiln.smoke'), 'rejects a foreign prefix')
check(!applicationIdPattern.test('com.gexetr.Smoke'), 'rejects uppercase')
check(!applicationIdPattern.test('com.gexetr.smoke.dev'), 'rejects an extra segment')

const scaffoldGradleFile = join(SLOT_SCAFFOLD, 'app', 'build.gradle.kts')
const scaffoldApplicationId = existsSync(scaffoldGradleFile)
  ? applicationIdIn(readFileSync(scaffoldGradleFile, 'utf8'))
  : null
check(
  scaffoldApplicationId === scaffoldPackage,
  `scaffold applicationId is ${scaffoldPackage} (found ${scaffoldApplicationId || '?'})`
)
check(
  Boolean(scaffoldApplicationId) && applicationIdPattern.test(scaffoldApplicationId),
  'scaffold applicationId satisfies the verify-slot gate pattern'
)
check(
  existsSync(join(SLOT_SCAFFOLD, 'app', 'src', 'main', 'java', ...scaffoldPackage.split('.'))),
  'scaffold main package directory matches its namespace'
)

// --------------------------------------------------------------------------
// 3. Creating a slot in a sandbox must produce a clean, buildable slot.
// --------------------------------------------------------------------------

section('new-slot contract (sandbox)')

const sandbox = createSandbox()
const slotDate = '1970-01-01'
const slotDir = join(sandbox, 'apps', `${slotDate}-selftest`)
const expectedApplicationId = applicationIdForSlug('selftest')

try {
  const created = runNewSlot(sandbox, [
    '--slug', 'selftest',
    '--name', 'Selftest App',
    '--idea', 'sandbox slot for the factory self-test',
    '--date', slotDate
  ])
  const createdOk = check(created.status === 0, `exits 0 (got ${created.status})`)
  if (!createdOk) console.log(created.output.trim().split(/\r?\n/).map((l) => `      ${l}`).join('\n'))

  check(existsSync(slotDir), `creates apps/${slotDate}-selftest`)

  if (existsSync(slotDir)) {
    const slotFiles = walkFiles(slotDir)
    const slotLeaks = slotFiles
      .map((file) => relative(slotDir, file))
      .filter(isExcludedScaffoldPath)
    check(slotLeaks.length === 0, `slot copies no build state${detail(slotLeaks)}`)

    check(
      existsSync(join(slotDir, 'gradle', 'wrapper', 'gradle-wrapper.jar')),
      'slot has gradle-wrapper.jar'
    )
    check(
      existsSync(join(slotDir, 'app', 'src', 'main', 'java', ...expectedApplicationId.split('.'), 'MainActivity.kt')),
      'slot moved sources into the applicationId package'
    )
    check(
      !existsSync(join(slotDir, 'app', 'src', 'main', 'java', ...scaffoldPackage.split('.'))),
      'slot left no scaffold package directory behind'
    )

    const slotText = textFiles(slotDir)
    const tokenHits = slotText
      .filter(([, text]) => /__[A-Z_]+__/.test(text))
      .map(([name]) => name)
    check(tokenHits.length === 0, `no leftover template tokens${detail(tokenHits)}`)

    const packageHits = slotText
      .filter(([, text]) => text.includes(scaffoldPackage))
      .map(([name]) => name)
    check(packageHits.length === 0, `no leftover ${scaffoldPackage} reference${detail(packageHits)}`)

    const gradleFile = join(slotDir, 'app', 'build.gradle.kts')
    const slotApplicationId = existsSync(gradleFile)
      ? applicationIdIn(readFileSync(gradleFile, 'utf8'))
      : null
    check(
      slotApplicationId === expectedApplicationId,
      `applicationId rewritten to ${expectedApplicationId} (found ${slotApplicationId || '?'})`
    )
    check(existsSync(join(slotDir, '.kiln-state.json')), 'writes .kiln-state.json')
    check(existsSync(join(slotDir, 'dist')), 'creates dist/')
  }

  const registryFile = join(sandbox, 'registry', 'apps.json')
  let registered = null
  if (existsSync(registryFile)) {
    try {
      registered = JSON.parse(readFileSync(registryFile, 'utf8'))?.apps?.find((app) => app?.id === 'selftest')
    } catch {
      registered = null
    }
  }
  check(
    registered?.applicationId === expectedApplicationId,
    `registry records applicationId ${expectedApplicationId}`
  )

  // ------------------------------------------------------------------------
  // 4. Bad input must be refused, and the message must state the real rule.
  // ------------------------------------------------------------------------

  section('new-slot refuses bad input')

  const invalidSlug = runNewSlot(sandbox, [
    '--slug', 'Bad Slug', '--name', 'X', '--idea', 'y', '--date', slotDate
  ])
  check(invalidSlug.status !== 0, 'rejects an uppercase / spaced slug')

  const missingIdea = runNewSlot(sandbox, ['--slug', 'ok', '--name', 'X', '--date', slotDate])
  check(missingIdea.status !== 0, 'rejects a missing --idea')

  const foreignPackage = runNewSlot(sandbox, [
    '--slug', 'ok', '--name', 'X', '--idea', 'y', '--date', slotDate,
    '--package', 'com.other.ok'
  ])
  check(foreignPackage.status !== 0, 'rejects a foreign package prefix')

  // Regression guard for the drift defect: the message used to hardcode the old
  // namespace and told the user to pass a value the check would then reject.
  const expectedInMessage = packagePrefix.replaceAll('.', '\\.')
  check(
    foreignPackage.output.includes(expectedInMessage),
    `the package error message names the real prefix (${expectedInMessage})`
  )

  check(
    !existsSync(join(sandbox, 'apps', `${slotDate}-ok`)),
    'refused runs create no slot directory'
  )
} finally {
  if (KEEP) {
    console.log(`\n  INFO      sandbox kept at ${sandbox}`)
  } else {
    rmSync(sandbox, { recursive: true, force: true })
  }
}

console.log(`\nVERDICT: ${failed ? 'FAIL' : 'PASS'}`)
process.exitCode = failed ? 1 : 0

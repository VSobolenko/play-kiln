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
  rmSync,
  writeFileSync
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
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

/** Read every file under a directory as text, skipping binaries and unreadable files. */
function textFiles(directory, { skip = () => false } = {}) {
  const found = []
  for (const file of walkFiles(directory)) {
    const name = relative(directory, file)
    if (skip(name)) continue
    let bytes
    try {
      bytes = readFileSync(file)
    } catch {
      // A concurrent Gradle build legitimately holds locks on build/ and .gradle/. A file we
      // cannot read cannot be checked; crashing the whole suite over it would make this
      // sensor fail for a reason unrelated to the defect it looks for.
      continue
    }
    if (bytes.includes(0)) continue
    found.push([name, bytes.toString('utf8')])
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

const scaffoldSettingsFile = join(SLOT_SCAFFOLD, 'settings.gradle.kts')
const scaffoldSettings = existsSync(scaffoldSettingsFile) ? readFileSync(scaffoldSettingsFile, 'utf8') : ''
const dependencyRepositories = scaffoldSettings.match(/dependencyResolutionManagement\s*\{[\s\S]*?repositories\s*\{([\s\S]*?)\n\s*}\s*\n}/)?.[1] ?? ''
const pluginRepositories = scaffoldSettings.match(/pluginManagement\s*\{[\s\S]*?repositories\s*\{([\s\S]*?)\n\s*}\s*\n}/)?.[1] ?? ''
check(
  dependencyRepositories.includes('mavenLocal()') && !pluginRepositories.includes('mavenLocal()'),
  'scaffold resolves published capabilities through dependencyResolutionManagement only'
)

// --------------------------------------------------------------------------
// 2b. One rule, one place: the capability artifact group must not be restated.
// --------------------------------------------------------------------------

section('capability group single source')

const capabilitiesDir = join(ROOT, 'capabilities')
const capabilityGroupProperty = 'playkiln.capabilityGroup'
const capabilityGroup = readFileSync(join(capabilitiesDir, 'gradle.properties'), 'utf8')
  .split(/\r?\n/)
  .find((row) => row.startsWith(`${capabilityGroupProperty}=`))
  ?.slice(capabilityGroupProperty.length + 1)
  .trim()

check(
  Boolean(capabilityGroup),
  `${capabilityGroupProperty} is declared in capabilities/gradle.properties`
)

if (capabilityGroup) {
  const capabilityModules = ['core', 'ads', 'billing', 'telemetry']
  const escapedGroup = capabilityGroup.replaceAll('.', '\\.')

  // The defect this guards against: when the namespace moved to com.gexetr, every module and
  // every capability.json was updated but the JSON schema kept its escaped
  // `com\.playkiln\.capability` pattern. Both looked correct in isolation, and nothing
  // compared them, so the schema silently disagreed with the artifacts it validates.
  //
  // The same prefix hides in several textual forms (.cursor/rules/verification-discipline.mdc):
  // plain, regex-escaped, JSON string-escaped, and path form. Stripping backslashes before
  // matching catches all of them, because a single- or double-escaped copy flattens to plain.
  const staleNames = ['com.playkiln.capability', 'com/playkiln/capability']
  const staleHits = textFiles(capabilitiesDir, {
    skip: (name) => /(^|[\\/])(build|\.gradle|\.kotlin|\.idea)[\\/]/.test(name)
  })
    .filter(([, text]) => {
      const flattened = text.replaceAll('\\', '')
      return staleNames.some((literal) => flattened.includes(literal))
    })
    .map(([name]) => name)
  check(staleHits.length === 0, `no leftover pre-rename capability group${detail(staleHits)}`)

  const groupLiterals = capabilityModules.filter((module) => {
    const file = join(capabilitiesDir, module, 'build.gradle.kts')
    if (!existsSync(file)) return true
    return !readFileSync(file, 'utf8').includes(`gradleProperty("${capabilityGroupProperty}")`)
  })
  check(
    groupLiterals.length === 0,
    `every capability module derives its group from ${capabilityGroupProperty}${detail(groupLiterals)}`
  )

  const artifactMismatches = capabilityModules.filter((module) => {
    const file = join(capabilitiesDir, module, 'capability.json')
    if (!existsSync(file)) return true
    return JSON.parse(readFileSync(file, 'utf8')).artifact !== `${capabilityGroup}:${module}`
  })
  check(
    artifactMismatches.length === 0,
    `every capability.json artifact is ${capabilityGroup}:<module>${detail(artifactMismatches)}`
  )

  const schemaPattern =
    JSON.parse(readFileSync(join(capabilitiesDir, 'schema', 'capability.schema.json'), 'utf8'))
      ?.properties?.artifact?.pattern ?? ''
  check(
    schemaPattern.includes(`^${escapedGroup}:`),
    `schema artifact pattern agrees with ${capabilityGroupProperty}=${capabilityGroup}`
  )
}

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

  // ------------------------------------------------------------------------
  // 5. applicationId uniqueness.
  //
  // `applicationIdForSlug` drops dashes, so `my-app` and `myapp` both derive
  // `com.gexetr.myapp`. An applicationId is permanent after the first Play upload,
  // so the second slot must be refused rather than scaffolded against an ID that
  // can never be published.
  // ------------------------------------------------------------------------

  section('applicationId uniqueness')

  const firstDashed = runNewSlot(sandbox, [
    '--slug', 'my-app', '--name', 'Dashed', '--idea', 'dash case', '--date', slotDate
  ])
  check(firstDashed.status === 0, 'creates a slot whose slug contains a dash')

  const colliding = runNewSlot(sandbox, [
    '--slug', 'myapp', '--name', 'Fused', '--idea', 'no dash', '--date', slotDate
  ])
  check(colliding.status !== 0, 'refuses a slug whose applicationId another slot already owns')
  check(
    colliding.output.includes('com.gexetr.myapp'),
    'the collision message names the shared applicationId'
  )
  check(
    colliding.output.includes('my-app'),
    'the collision message names the slot that already owns the applicationId'
  )
  check(
    !existsSync(join(sandbox, 'apps', `${slotDate}-myapp`)),
    'a refused collision creates no slot directory'
  )

  const explicitCollision = runNewSlot(sandbox, [
    '--slug', 'other', '--name', 'Other', '--idea', 'explicit', '--date', slotDate,
    '--package', 'com.gexetr.myapp'
  ])
  check(
    explicitCollision.status !== 0,
    'refuses an explicit --package that another slot already owns'
  )

  // ------------------------------------------------------------------------
  // 5. play-upload must refuse unsafe Play calls and name the real reason.
  //
  // These paths are pure validation and argument handling, so they run without
  // Play credentials or network access. The Data safety guard matters most: it is
  // what stops the factory from silently sending a legal declaration.
  // ------------------------------------------------------------------------

  section('play-upload guards')

  copyFileSync(join(ROOT, 'scripts', 'play-upload.mjs'), join(sandbox, 'scripts', 'play-upload.mjs'))

  const uploadSlot = join(sandbox, 'apps', `${slotDate}-upload`)
  mkdirSync(join(uploadSlot, 'store'), { recursive: true })
  mkdirSync(join(uploadSlot, 'dist'), { recursive: true })
  writeFileSync(
    join(uploadSlot, 'store', 'listing.json'),
    JSON.stringify({
      schemaVersion: 1,
      applicationId: 'com.gexetr.upload',
      defaultLocale: 'en-US',
      contactEmail: 'support@example.com',
      locales: { 'en-US': { title: 'X', shortDescription: 'Y', fullDescription: 'Z' } }
    }, null, 2),
    'utf8'
  )

  function runUpload(args) {
    const result = spawnSync(
      process.execPath,
      [join(sandbox, 'scripts', 'play-upload.mjs'), ...args],
      { cwd: sandbox, encoding: 'utf8', windowsHide: true }
    )
    return { status: result.status, output: `${result.stdout || ''}${result.stderr || ''}` }
  }

  const uploadHelp = runUpload(['--help'])
  check(uploadHelp.status === 0, '--help exits 0')
  for (const target of ['release', 'details', 'mapping', 'data-safety', 'internal-sharing']) {
    check(uploadHelp.output.includes(target), `--help documents target ${target}`)
  }

  const uploadUnknown = runUpload(['--slot', 'upload', '--only', 'bogus', '--dry-run'])
  check(uploadUnknown.status !== 0, 'rejects an unknown upload target')

  const uploadUnapproved = runUpload(['--slot', 'upload', '--only', 'data-safety', '--dry-run'])
  check(uploadUnapproved.status !== 0, 'refuses data-safety without --approve-declarations')
  check(
    uploadUnapproved.output.includes('--approve-declarations'),
    'the data-safety refusal names the approval flag it needs'
  )

  const uploadNoCsv = runUpload([
    '--slot', 'upload', '--only', 'data-safety', '--approve-declarations', '--dry-run'
  ])
  check(uploadNoCsv.status !== 0, 'refuses data-safety when the CSV is absent')
  check(
    uploadNoCsv.output.includes('data-safety.csv'),
    'the missing-CSV message names store/data-safety.csv'
  )

  const csvPath = join(uploadSlot, 'store', 'data-safety.csv')
  writeFileSync(csvPath, 'foo,bar\n1,2\n', 'utf8')
  const uploadBadCsv = runUpload([
    '--slot', 'upload', '--only', 'data-safety', '--approve-declarations', '--dry-run'
  ])
  check(uploadBadCsv.status !== 0, 'refuses a CSV that is not the official template')

  writeFileSync(csvPath, 'Question ID,Response value,Answer requirement\nDATA_COLLECTION,TRUE,REQUIRED\n', 'utf8')
  const uploadOkCsv = runUpload([
    '--slot', 'upload', '--only', 'data-safety', '--approve-declarations', '--dry-run'
  ])
  check(uploadOkCsv.status === 0, 'accepts a template-shaped CSV in dry-run')
  check(
    uploadOkCsv.output.includes('dataSafety'),
    'the dry-run plan targets the dataSafety endpoint'
  )

  const uploadReleaseNoAab = runUpload(['--slot', 'upload', '--only', 'release', '--dry-run'])
  check(uploadReleaseNoAab.status !== 0, 'refuses a release with no store assets or staged AAB')

  // Isolate the AAB check: internal-sharing needs no store pack, so the reported
  // problem is unambiguously the missing artifact under dist/.
  const uploadNoAab = runUpload(['--slot', 'upload', '--only', 'internal-sharing', '--dry-run'])
  check(uploadNoAab.status !== 0, 'refuses internal-sharing with no AAB staged under dist/')
  check(
    uploadNoAab.output.includes('dist'),
    'the missing-AAB message names dist/'
  )

  const uploadDetails = runUpload(['--slot', 'upload', '--only', 'details', '--dry-run'])
  check(uploadDetails.status === 0, 'dry-runs edits.details from listing.json')
  check(
    uploadDetails.output.includes('updateMask=defaultLanguage,contactEmail'),
    'details updateMask is derived from the fields that are present'
  )

  const uploadBadTrack = runUpload(['--slot', 'upload', '--track', 'bad track', '--dry-run'])
  check(uploadBadTrack.status !== 0, 'rejects an invalid track name')

  // ------------------------------------------------------------------------
  // 7. The readiness gate must not claim PASS without compiling.
  //
  // `verify-slot.mjs` used to print `VERDICT: PASS - available checks green` while
  // skipping assembleDebug, so uncompilable code could look like release evidence.
  // A skipped build is now an UNVERIFIED reason, and `--help` must not run the
  // whole suite before printing usage.
  // ------------------------------------------------------------------------

  section('readiness gate honesty')

  // The sandbox only ships the scripts each section needs, so copy the readiness
  // gate and its imports in before driving it.
  for (const relative of [
    'verify-slot.mjs',
    'lib/play-assets.mjs',
    'lib/capabilities.mjs',
    'lib/gradle.mjs',
    'lib/java.mjs'
  ]) {
    copyFileSync(join(ROOT, 'scripts', relative), join(sandbox, 'scripts', relative))
  }

  function runVerify(args, cwd = sandbox) {
    const started = Date.now()
    const result = spawnSync(
      process.execPath,
      [join(cwd, 'scripts', 'verify-slot.mjs'), ...args],
      { cwd, encoding: 'utf8', windowsHide: true }
    )
    return { status: result.status, output: `${result.stdout || ''}${result.stderr || ''}`, elapsedMs: Date.now() - started }
  }

  const helpRun = runVerify(['--help'])
  check(helpRun.status === 0, '--help exits 0')
  check(helpRun.output.includes('--build'), '--help documents the --build flag')
  check(helpRun.elapsedMs < 5000, `--help short-circuits instead of running the suite (${helpRun.elapsedMs}ms)`)

  const noBuildRun = runVerify(['--slot', 'selftest', '--no-selftest'])
  check(
    !/VERDICT: PASS/.test(noBuildRun.output),
    'a run that skips the build never reports PASS'
  )
  check(
    noBuildRun.output.includes('VERDICT: UNVERIFIED'),
    'a run that skips the build reports UNVERIFIED'
  )
  check(noBuildRun.status === 2, `an UNVERIFIED verdict exits 2 (got ${noBuildRun.status})`)

  // ------------------------------------------------------------------------
  // 8. Capability declarations must drive the slot compliance gate.
  //
  // These checks use the real aggregator and verify-slot process. Each defect is
  // introduced into the sandbox slot, observed as a FAIL, then removed and
  // observed as a PASS line. This guards against a gate that merely prints a
  // reassuring message without comparing the declarations to shipped evidence.
  // ------------------------------------------------------------------------

  section('capability compliance derivation')

  const capabilityHelper = join(ROOT, 'scripts', 'lib', 'capabilities.mjs')
  const helperExists = check(
    existsSync(capabilityHelper),
    'shared capability aggregator exists'
  )

  if (helperExists) {
    copyFileSync(capabilityHelper, join(sandbox, 'scripts', 'lib', 'capabilities.mjs'))
    for (const id of ['ads', 'billing', 'core', 'telemetry']) {
      mkdirSync(join(sandbox, 'capabilities', id), { recursive: true })
      copyFileSync(
        join(ROOT, 'capabilities', id, 'capability.json'),
        join(sandbox, 'capabilities', id, 'capability.json')
      )
    }

    const capabilityConfigPath = join(slotDir, 'capabilities.json')
    const bootstrap = spawnSync(
      'python',
      [join(ROOT, 'scripts', 'store-kit.py'), '--slot-dir', slotDir, '--render-only'],
      { cwd: ROOT, encoding: 'utf8', windowsHide: true }
    )
    check(bootstrap.status === 0, 'normal store-kit generation bootstraps a slot capability registry')
    const bootstrappedCapabilities = existsSync(capabilityConfigPath)
      ? JSON.parse(readFileSync(capabilityConfigPath, 'utf8'))
      : null
    check(
      bootstrappedCapabilities?.schemaVersion === 1 &&
        Array.isArray(bootstrappedCapabilities.enabled) &&
        bootstrappedCapabilities.enabled.length === 0,
      'bootstrapped capability registry explicitly enables none'
    )

    const { aggregateSlotCapabilities } = await import(pathToFileURL(capabilityHelper).href)
    const validCapabilityConfig = {
      schemaVersion: 1,
      enabled: ['ads', 'billing'],
      billing: {
        consumables: [{ id: 'generations-10', fulfillment: 'SERVER_COST' }],
        purchaseAuthority: 'PlayKiln API verifies purchase tokens and owns the generation balance.'
      }
    }
    writeFileSync(capabilityConfigPath, JSON.stringify(validCapabilityConfig, null, 2), 'utf8')

    const aggregate = aggregateSlotCapabilities({ rootDir: ROOT, slotDir })
    check(
      aggregate.permissions.map((item) => item.name).join(',') === 'com.google.android.gms.permission.AD_ID',
      'aggregates the union of capability permissions'
    )
    check(
      aggregate.dataCollected.map((item) => item.type).join(',') ===
        'App activity,App info and performance,Device or other IDs,Financial info',
      'aggregates and de-duplicates capability data types'
    )
    check(aggregate.adsPresent === true, 'derives adsPresent from enabled capabilities')
    check(aggregate.requiresConsent === true, 'derives requiresConsent from enabled capabilities')
    check(
      aggregate.requiresPurchaseAuthority === true && Boolean(aggregate.purchaseAuthority),
      'SERVER_COST consumables derive and document a purchase authority requirement'
    )

    writeFileSync(
      capabilityConfigPath,
      JSON.stringify({ schemaVersion: 1, enabled: ['not-real'] }, null, 2),
      'utf8'
    )
    let unknownMessage = ''
    try {
      aggregateSlotCapabilities({ rootDir: ROOT, slotDir })
    } catch (error) {
      unknownMessage = error.message
    }
    check(
      unknownMessage.includes('not-real') &&
        ['ads', 'billing', 'core', 'telemetry'].every((id) => unknownMessage.includes(id)),
      'unknown capability fails loudly and names every valid id'
    )

    writeFileSync(capabilityConfigPath, JSON.stringify(validCapabilityConfig, null, 2), 'utf8')
    const mergedManifest = join(
      slotDir,
      'app', 'build', 'intermediates', 'merged_manifests', 'debug', 'processDebugManifest',
      'AndroidManifest.xml'
    )
    mkdirSync(dirname(mergedManifest), { recursive: true })
    const manifestWithAdId = `<?xml version="1.0" encoding="utf-8"?>\n<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="com.google.android.gms.permission.AD_ID"/><application/></manifest>\n`
    const manifestWithoutAdId = `<?xml version="1.0" encoding="utf-8"?>\n<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application/></manifest>\n`
    const storeDir = join(slotDir, 'store')
    mkdirSync(storeDir, { recursive: true })
    const listingPath = join(storeDir, 'listing.json')
    const complianceListing = JSON.parse(readFileSync(listingPath, 'utf8'))
    complianceListing.ads = { containsAds: true, reason: 'Enabled capabilities contain ads.' }
    writeFileSync(listingPath, JSON.stringify(complianceListing, null, 2), 'utf8')
    const allDataTypes = `| Data category | Collected | Shared | Purpose |
|---|---:|---:|---|
| App activity | Yes | Yes | Advertising or marketing |
| App info and performance | Yes | Yes | Analytics |
| Device or other IDs | Yes | Yes | Advertising or marketing |
| Financial info | Yes | No | App functionality |
`
    writeFileSync(join(storeDir, 'data-safety.md'), allDataTypes, 'utf8')
    writeFileSync(mergedManifest, manifestWithoutAdId, 'utf8')

    const missingPermission = runVerify(['--slot', 'selftest', '--strict-store', '--no-selftest'])
    check(
      missingPermission.output.includes('FAIL        capability permission missing from merged manifest: com.google.android.gms.permission.AD_ID'),
      'missing capability permission fails the strict store gate'
    )

    writeFileSync(mergedManifest, manifestWithAdId, 'utf8')
    const authorityMissingFromPack = runVerify(['--slot', 'selftest', '--strict-store', '--no-selftest'])
    check(
      authorityMissingFromPack.output.includes('FAIL        purchase authority missing from store/review-notes.md'),
      'purchase authority omitted from the store pack fails the strict store gate'
    )
    writeFileSync(
      join(storeDir, 'review-notes.md'),
      `# Review notes\n\nPurchase authority: ${validCapabilityConfig.billing.purchaseAuthority}\n`,
      'utf8'
    )
    const capabilityAgreement = runVerify(['--slot', 'selftest', '--strict-store', '--no-selftest'])
    check(
      capabilityAgreement.output.includes('PASS        capability permissions match merged manifest'),
      'matching capability permissions pass the strict store gate'
    )
    check(
      capabilityAgreement.output.includes('PASS        store ads declaration matches capabilities: ads present'),
      'matching ads declaration passes the strict store gate'
    )
    check(
      capabilityAgreement.output.includes('PASS        capability data types disclosed in store/data-safety.md'),
      'complete capability data disclosure passes the strict store gate'
    )
    check(
      capabilityAgreement.output.includes('PASS        SERVER_COST purchase authority documented'),
      'documented SERVER_COST purchase authority passes the strict store gate'
    )

    const billingOnly = {
      schemaVersion: 1,
      enabled: ['billing'],
      billing: validCapabilityConfig.billing
    }
    writeFileSync(capabilityConfigPath, JSON.stringify(billingOnly, null, 2), 'utf8')
    const undeclaredPermission = runVerify(['--slot', 'selftest', '--strict-store', '--no-selftest'])
    check(
      undeclaredPermission.output.includes('FAIL        ads/billing permission has no enabled capability: com.google.android.gms.permission.AD_ID'),
      'ads permission without an enabled ads capability fails the strict store gate'
    )
    check(
      undeclaredPermission.output.includes('FAIL        store/listing.json claims ads but no enabled capability declares ads'),
      'ads store claim without an enabled ads capability fails the strict store gate'
    )

    writeFileSync(capabilityConfigPath, JSON.stringify(validCapabilityConfig, null, 2), 'utf8')
    const listingFixture = JSON.parse(readFileSync(listingPath, 'utf8'))
    listingFixture.ads.containsAds = false
    writeFileSync(listingPath, JSON.stringify(listingFixture, null, 2), 'utf8')
    const falseAdsClaim = runVerify(['--slot', 'selftest', '--strict-store', '--no-selftest'])
    check(
      falseAdsClaim.output.includes('FAIL        enabled capabilities contain ads but store/listing.json claims no ads'),
      'false no-ads store claim fails the strict store gate'
    )
    listingFixture.ads.containsAds = true
    writeFileSync(listingPath, JSON.stringify(listingFixture, null, 2), 'utf8')

    writeFileSync(
      join(storeDir, 'data-safety.md'),
      allDataTypes.replace('| Device or other IDs | Yes | Yes | Advertising or marketing |\n', ''),
      'utf8'
    )
    const omittedDataType = runVerify(['--slot', 'selftest', '--strict-store', '--no-selftest'])
    check(
      omittedDataType.output.includes('FAIL        capability data type missing from store/data-safety.md: Device or other IDs'),
      'omitted capability data type fails the strict store gate'
    )
    writeFileSync(join(storeDir, 'data-safety.md'), allDataTypes, 'utf8')

    writeFileSync(
      join(storeDir, 'data-safety.md'),
      allDataTypes.replace('| Financial info | Yes |', '| Financial info | No |'),
      'utf8'
    )
    const deniedDataType = runVerify(['--slot', 'selftest', '--strict-store', '--no-selftest'])
    check(
      deniedDataType.output.includes('FAIL        capability data type missing from store/data-safety.md: Financial info'),
      'a negative Data safety row cannot satisfy a collected capability data type'
    )
    writeFileSync(join(storeDir, 'data-safety.md'), allDataTypes, 'utf8')

    const undocumentedAuthorityConfig = structuredClone(validCapabilityConfig)
    delete undocumentedAuthorityConfig.billing.purchaseAuthority
    writeFileSync(capabilityConfigPath, JSON.stringify(undocumentedAuthorityConfig, null, 2), 'utf8')
    const undocumentedAuthority = runVerify(['--slot', 'selftest', '--strict-store', '--no-selftest'])
    check(
      undocumentedAuthority.output.includes('FAIL        SERVER_COST consumable requires billing.purchaseAuthority documentation'),
      'undocumented SERVER_COST purchase authority fails the strict store gate'
    )
    writeFileSync(capabilityConfigPath, JSON.stringify(validCapabilityConfig, null, 2), 'utf8')

    const storeInit = spawnSync(
      'python',
      [join(ROOT, 'scripts', 'store-kit.py'), '--slot-dir', slotDir, '--init'],
      { cwd: ROOT, encoding: 'utf8', windowsHide: true }
    )
    check(storeInit.status === 0, 'store-kit initializes a capability-derived listing')
    const derivedListing = JSON.parse(readFileSync(listingPath, 'utf8'))
    check(
      derivedListing.ads?.containsAds === true &&
        derivedListing.permissions?.some((item) => item.name === 'com.google.android.gms.permission.AD_ID'),
      'store-kit derives ads and permissions from enabled capabilities'
    )
    check(
      derivedListing.dataSafety?.dataTypes?.some((item) => item.type === 'Financial info') &&
        derivedListing.dataSafety?.dataTypes?.some((item) => item.type === 'Device or other IDs'),
      'store-kit derives Data safety types from enabled capabilities'
    )
    check(
      derivedListing.capabilityCompliance?.requiresConsent === true &&
        derivedListing.capabilityCompliance?.requiresPurchaseAuthority === true &&
        Boolean(derivedListing.capabilityCompliance?.purchaseAuthority),
      'store-kit records consent and purchase-authority handoff facts'
    )

    writeFileSync(
      capabilityConfigPath,
      JSON.stringify({ schemaVersion: 1, enabled: ['billing'] }, null, 2),
      'utf8'
    )
    const dataOnlyInit = spawnSync(
      'python',
      [join(ROOT, 'scripts', 'store-kit.py'), '--slot-dir', slotDir, '--init'],
      { cwd: ROOT, encoding: 'utf8', windowsHide: true }
    )
    const dataOnlyListing = JSON.parse(readFileSync(listingPath, 'utf8'))
    check(
      dataOnlyInit.status === 0 &&
        !dataOnlyListing.locales['en-US'].fullDescription.includes('with advertising'),
      'data-only capabilities do not make the generated listing claim advertising'
    )
  }

  // ------------------------------------------------------------------------
  // 10. Store-pack contracts: per-kind screenshot limits and idea provenance.
  //
  // Two store-kit behaviours regressed silently before this section existed:
  //
  //  * `SCREENSHOT_LIMITS` split the screenshot gate per asset kind. The tablet
  //    rule wants a 1080px short side and a 3840px long side with NO 2x cap,
  //    because Play's large-screen section conflicts with its general
  //    requirements; the old blanket rule applied the phone 2x cap to tablets and
  //    wrongly rejected a 1080x3000 tablet asset.
  //  * `refresh_idea_from_record` rewrites only the copy that
  //    `is_idea_derived_output` proves is still generator output for the listing's
  //    previous idea, so a hand-written `shortDescription` survives a refresh.
  //
  // These checks drive the real CLI (`--init`, `--render-only`, `--validate-only`).
  // ------------------------------------------------------------------------

  section('store pack contracts')

  const storeKitScript = join(ROOT, 'scripts', 'store-kit.py')
  const contractStoreDir = join(slotDir, 'store')
  const contractListingPath = join(contractStoreDir, 'listing.json')
  const contractAssets = join(contractStoreDir, 'assets', 'en-US')
  const slotStatePath = join(slotDir, '.kiln-state.json')

  function runStoreKit(args) {
    const result = spawnSync('python', [storeKitScript, '--slot-dir', slotDir, ...args], {
      cwd: ROOT,
      encoding: 'utf8',
      windowsHide: true
    })
    return { status: result.status, output: `${result.stdout || ''}${result.stderr || ''}` }
  }

  function writePngFill(path, width, height) {
    const script =
      'from PIL import Image\n' +
      `Image.new("RGB", (${width}, ${height}), (30, 90, 160)).save(${JSON.stringify(path)})`
    const result = spawnSync('python', ['-c', script], {
      cwd: ROOT,
      encoding: 'utf8',
      windowsHide: true
    })
    return result.status === 0
  }

  /** The FAIL row the validator prints for one store asset, or '' when it passed. */
  function assetFailure(output, fileName) {
    return (
      output.split(/\r?\n/).find((row) => row.startsWith('FAIL') && row.includes(fileName)) || ''
    )
  }

  function setRecordedIdea(idea) {
    const state = JSON.parse(readFileSync(slotStatePath, 'utf8'))
    state.idea = idea
    writeFileSync(slotStatePath, `${JSON.stringify(state, null, 2)}\n`, 'utf8')
  }

  // A neutral capability set keeps capability-derived copy out of these checks.
  writeFileSync(
    join(slotDir, 'capabilities.json'),
    JSON.stringify({ schemaVersion: 1, enabled: [] }, null, 2),
    'utf8'
  )

  // The scaffold's own boilerplate stands in for a pack generated before the slot
  // recorded an idea; that is exactly the pack the refresh has to repair.
  const scaffoldBoilerplate =
    'PlayKiln slot — isolated Android attempt. Factory harness lives at the repo root.'
  setRecordedIdea(scaffoldBoilerplate)

  const contractInit = runStoreKit(['--init'])
  check(contractInit.status === 0, 'store-kit --init writes a draft listing for the contract checks')
  const contractRender = runStoreKit(['--render-only'])
  check(
    contractRender.status === 0,
    'store-kit --render-only renders a complete pack for the contract checks'
  )

  // (1) Baseline: a generated pack is valid.
  const contractBaseline = runStoreKit(['--validate-only'])
  check(contractBaseline.status === 0, 'a generated store pack passes --validate-only with exit 0')

  // (2)-(6) Per-kind screenshot rule, through the CLI.
  //
  // `validate_store` also enforces the exact canonical size from SCREEN_TARGETS
  // (`--render-only` writes 1200x1920 tablets and 1080x1920 phones), so a size-mutated
  // asset always exits non-zero for that unrelated reason. Without weakening that
  // guard there is no CLI route to exit 0 for a mutated asset, so these checks assert
  // *which* rule the failure names: the tablet 1080x3000 case — the regression — must
  // name no rule at all, while a phone 1080x3000 must still name the phone 2x rule.
  const tabletAsset = join(contractAssets, 'tablet-7-01.png')
  const phoneAsset = join(contractAssets, 'phone-01.png')
  const tabletOriginal = readFileSync(tabletAsset)
  const phoneOriginal = readFileSync(phoneAsset)

  const tabletTallWritten = writePngFill(tabletAsset, 1080, 3000)
  const tabletTall = runStoreKit(['--validate-only'])
  const tabletTallFailure = assetFailure(tabletTall.output, 'tablet-7-01.png')
  check(
    tabletTallWritten &&
      tabletTallFailure.includes('1080x3000') &&
      !tabletTallFailure.includes('more than 2x') &&
      !tabletTallFailure.includes('below the 1080px minimum'),
    'tablet 1080x3000 satisfies the per-kind tablet rule (short >= 1080, long <= 3840, no 2x rule) ' +
      `-> ${tabletTallFailure.trim()}`
  )
  writeFileSync(tabletAsset, tabletOriginal)

  const tabletShortWritten = writePngFill(tabletAsset, 720, 1280)
  const tabletShort = runStoreKit(['--validate-only'])
  const tabletShortFailure = assetFailure(tabletShort.output, 'tablet-7-01.png')
  check(
    tabletShortWritten &&
      tabletShort.status !== 0 &&
      tabletShortFailure.includes('short side 720px is below the 1080px minimum') &&
      !tabletShortFailure.includes('more than 2x'),
    'tablet 720x1280 fails the tablet 1080px short-side minimum, not the phone 2x rule'
  )
  writeFileSync(tabletAsset, tabletOriginal)

  const phoneTallWritten = writePngFill(phoneAsset, 1080, 3000)
  const phoneTall = runStoreKit(['--validate-only'])
  const phoneTallFailure = assetFailure(phoneTall.output, 'phone-01.png')
  check(
    phoneTallWritten &&
      phoneTall.status !== 0 &&
      phoneTallFailure.includes('long side 3000px is more than 2x the short side 1080px'),
    'phone 1080x3000 fails the phone 2x rule, so the phone rule is not the tablet rule'
  )
  writeFileSync(phoneAsset, phoneOriginal)

  const phoneShortWritten = writePngFill(phoneAsset, 320, 800)
  const phoneShort = runStoreKit(['--validate-only'])
  const phoneShortFailure = assetFailure(phoneShort.output, 'phone-01.png')
  check(
    phoneShortWritten &&
      phoneShort.status !== 0 &&
      phoneShortFailure.includes('long side 800px is more than 2x the short side 320px'),
    'phone 320x800 fails the phone 2x rule even though 320-3840 alone would allow it'
  )
  writeFileSync(phoneAsset, phoneOriginal)

  const tabletOverlongWritten = writePngFill(tabletAsset, 1200, 4000)
  const tabletOverlong = runStoreKit(['--validate-only'])
  const tabletOverlongFailure = assetFailure(tabletOverlong.output, 'tablet-7-01.png')
  check(
    tabletOverlongWritten &&
      tabletOverlong.status !== 0 &&
      tabletOverlongFailure.includes('long side 4000px exceeds the 3840px maximum'),
    'tablet 1200x4000 is rejected for exceeding the tablet 3840px long-side cap'
  )
  writeFileSync(tabletAsset, tabletOriginal)

  // (2) again, as a pure rule verdict: `screenshot_rule_failures` is the function
  // `validate_store` calls for every screenshot, so importing it directly shows that
  // a 1080x3000 tablet asset produces no rule failure at all while the same size on a
  // phone does. This is the core regression the blanket rule caused.
  const ruleScript = [
    'import importlib.util, json, sys',
    'sys.dont_write_bytecode = True',
    "spec = importlib.util.spec_from_file_location('store_kit_contract', sys.argv[1])",
    'module = importlib.util.module_from_spec(spec)',
    'spec.loader.exec_module(module)',
    'cases = json.loads(sys.argv[2])',
    'print(json.dumps([module.screenshot_rule_failures(k, (w, h)) for k, w, h in cases]))'
  ].join('\n')
  const ruleCases = [
    ['tablet-7', 1080, 3000],
    ['tablet-7', 720, 1280],
    ['phone', 1080, 3000],
    ['phone', 320, 800],
    ['tablet-7', 1200, 4000]
  ]
  // `-B` plus `sys.dont_write_bytecode` keeps this import from creating a
  // `scripts/__pycache__/store-kit.*.pyc` side effect inside the real repo.
  const ruleRun = spawnSync(
    'python',
    ['-B', '-c', ruleScript, storeKitScript, JSON.stringify(ruleCases)],
    { cwd: ROOT, encoding: 'utf8', windowsHide: true }
  )
  let ruleVerdicts = null
  try {
    ruleVerdicts = JSON.parse(ruleRun.stdout || '')
  } catch {
    ruleVerdicts = null
  }
  check(
    Array.isArray(ruleVerdicts) && ruleVerdicts.length === ruleCases.length,
    'the per-kind screenshot rule answers for every case in the contract table'
  )
  if (Array.isArray(ruleVerdicts)) {
    check(
      ruleVerdicts[0].length === 0,
      'the per-kind rule accepts a 1080x3000 tablet asset (no 2x rule, short >= 1080, long <= 3840)'
    )
    check(
      ruleVerdicts[1].some((failure) => failure.includes('below the 1080px minimum')),
      'the per-kind rule rejects a 720x1280 tablet for the 1080px short side'
    )
    check(
      ruleVerdicts[2].some((failure) => failure.includes('more than 2x')),
      'the per-kind rule rejects a 1080x3000 phone for the 2x cap that tablets are exempt from'
    )
    check(
      ruleVerdicts[3].some((failure) => failure.includes('more than 2x')),
      'the per-kind rule rejects a 320x800 phone for the 2x cap'
    )
    check(
      ruleVerdicts[4].some((failure) => failure.includes('exceeds the 3840px maximum')),
      'the per-kind rule rejects a 1200x4000 tablet for the 3840px long-side cap'
    )
  }

  // (7) Idea refresh replaces scaffold boilerplate with the recorded idea.
  const recordedIdea = 'A tide-chart notebook that records harbour water levels every morning.'
  setRecordedIdea(recordedIdea)
  const firstRefresh = runStoreKit(['--render-only'])
  check(
    firstRefresh.status === 0,
    'store-kit --render-only exits 0 while refreshing the listing from the slot record'
  )
  const firstRefreshed = JSON.parse(readFileSync(contractListingPath, 'utf8'))
  check(
    firstRefreshed.locales['en-US'].fullDescription.includes(recordedIdea) &&
      !firstRefreshed.locales['en-US'].fullDescription.includes(scaffoldBoilerplate),
    'refresh rewrites fullDescription to the recorded idea and drops the scaffold boilerplate'
  )

  // (8)-(9) Provenance: human copy survives, non-derived fields are never touched.
  const humanShortDescription = 'Hand-written short description that must survive the refresh.'
  const humanContactEmail = 'human-authored@example.org'
  const handEdited = JSON.parse(readFileSync(contractListingPath, 'utf8'))
  handEdited.locales['en-US'].shortDescription = humanShortDescription
  handEdited.contactEmail = humanContactEmail
  writeFileSync(contractListingPath, `${JSON.stringify(handEdited, null, 2)}\n`, 'utf8')

  const nextRecordedIdea = 'A hand-authored harbour log that outlives a generator refresh.'
  setRecordedIdea(nextRecordedIdea)
  const secondRefresh = runStoreKit(['--render-only'])
  check(
    secondRefresh.status === 0,
    'store-kit --render-only exits 0 while a human edit is present'
  )
  const secondRefreshed = JSON.parse(readFileSync(contractListingPath, 'utf8'))
  check(
    secondRefreshed.locales['en-US'].shortDescription === humanShortDescription &&
      secondRefreshed.locales['en-US'].fullDescription.includes(nextRecordedIdea),
    'a hand-edited shortDescription survives verbatim while fullDescription refreshes to the new idea'
  )
  check(
    secondRefreshed.contactEmail === humanContactEmail,
    'the refresh never rewrites a non-idea-derived field (contactEmail)'
  )

  // ------------------------------------------------------------------------
  // 6. Gradle invocation contract.
  //
  // The factory once launched assembleDebug/assembleRelease/bundleRelease as
  // three separate Gradle processes, paying three JVM startups and three project
  // configurations for shared work (warm cache: 6.3s -> 2.3s once merged). The
  // test drives runGradleTasks with a fake spawn, so the contract is asserted by
  // behaviour rather than by reading the implementation.
  // ------------------------------------------------------------------------

  section('gradle invocation contract')

  // Only the shared Gradle helper may launch a wrapper. Other scripts must use
  // runGradleTasks so wrapper selection, JDK pinning, and process options cannot
  // drift.
  //
  // Strategy: detect the wrapper *name*, not the launch idiom. Any way to launch
  // the wrapper has to name it somewhere the process can see, so after comments
  // are stripped this check fails a non-allow-listed script that puts the wrapper
  // token in a string literal or concatenates adjacent string literals that
  // reassemble to it (`'grad' + 'lew'`, `"gradlew" + ".bat"`). The previous,
  // call-shaped version missed `spawnSync(wrapperFor(dir))`, an aliased launcher
  // (`const go = spawnSync; go(wrapper, ...)`), and a local `launch()` helper; a
  // name-shaped check does not, because each of those still spells the name.
  // Reassembly only follows literals joined by `+`; a name split by any other
  // expression is the residual below. The scan models string and regex literals,
  // so a quote inside a regex cannot desync it into hiding a real launch.
  //
  // This is a tripwire, not a proof. A name assembled at runtime — from character
  // codes, from data read off disk, or by importing a path builder that never
  // spells `gradlew` in this file — leaves no literal behind and will not be seen.
  // A regex literal that merely *matches* Gradle's own "not found" error text is
  // deliberately not a violation, because a pattern is not a launch.
  //
  // The allow-list exempts only files that cannot launch; every entry states why.
  const directGradleWrapperAllowlist = new Map([
    ['lib/gradle.mjs', 'the single shared launcher: the one module allowed to name a wrapper'],
    ['selftest.mjs', "this check: its own pattern text and the fake 'gradlew' fixture for the JDK-pinning test"],
    ['kiln-status.mjs', 'inventories scaffold wrapper paths with existsSync; it never launches a process']
  ])
  const wrapperPattern = /\bgradlew\b/i
  const regexPrecedingWords = new Set([
    'return', 'typeof', 'instanceof', 'in', 'of', 'new', 'delete', 'void', 'case', 'do', 'else', 'yield', 'await'
  ])
  const regexPrecedingChars = new Set([
    '(', ',', '=', ':', '[', '!', '&', '|', '?', '{', '}', ';', '+', '-', '*', '/', '%', '^', '~', '<', '>'
  ])

  /** True when the `/` at the end of `chars` opens a regex literal rather than dividing. */
  const startsRegexLiteral = (chars) => {
    let index = chars.length - 1
    while (index >= 0 && /\s/.test(chars[index])) index -= 1
    if (index < 0) return true
    const previous = chars[index]
    if (regexPrecedingChars.has(previous)) return true
    if (/[\w$]/.test(previous)) {
      let word = ''
      while (index >= 0 && /[\w$]/.test(chars[index])) {
        word = chars[index] + word
        index -= 1
      }
      return regexPrecedingWords.has(word)
    }
    return false
  }

  const unescapeLiteral = (value) => value.replace(/\\(.)/g, '$1')

  /**
   * Remove comments while collecting the string literals the code contains. String and
   * regex literals are modelled explicitly, so a quote inside a regex cannot desync the
   * scan, and a comment that merely mentions the wrapper is not mistaken for a launch.
   */
  const scanCode = (source) => {
    const chars = []
    const literals = []
    let lineComment = false
    let blockComment = false
    let quote = null
    let regex = false
    let regexClass = false
    let literalStart = 0
    let literalText = ''
    for (let index = 0; index < source.length; index += 1) {
      const char = source[index]
      const next = source[index + 1]
      if (lineComment) {
        if (char === '\n') {
          lineComment = false
          chars.push('\n')
        }
        continue
      }
      if (blockComment) {
        if (char === '*' && next === '/') {
          blockComment = false
          index += 1
          chars.push(' ')
        }
        continue
      }
      if (quote) {
        chars.push(char)
        if (char === '\\') {
          chars.push(next ?? '')
          literalText += char + (next ?? '')
          index += 1
        } else if (char === quote) {
          quote = null
          literals.push({ start: literalStart, end: chars.length, content: literalText })
          literalText = ''
        } else {
          literalText += char
        }
        continue
      }
      if (regex) {
        chars.push(char)
        if (char === '\\') {
          chars.push(next ?? '')
          index += 1
        } else if (char === '[') {
          regexClass = true
        } else if (char === ']') {
          regexClass = false
        } else if (char === '/' && !regexClass) {
          regex = false
        }
        continue
      }
      if (char === '/' && next === '/') {
        lineComment = true
        index += 1
        continue
      }
      if (char === '/' && next === '*') {
        blockComment = true
        index += 1
        continue
      }
      if (char === '"' || char === "'" || char === '`') {
        quote = char
        literalStart = chars.length
        literalText = ''
        chars.push(char)
        continue
      }
      if (char === '/' && startsRegexLiteral(chars)) {
        regex = true
        regexClass = false
        chars.push(char)
        continue
      }
      chars.push(char)
    }
    return { code: chars.join(''), literals }
  }

  const directWrapperNames = textFiles(join(ROOT, 'scripts'))
    .filter(
      ([name]) =>
        /\.(?:cjs|js|mjs)$/.test(name) &&
        !directGradleWrapperAllowlist.has(name.replaceAll('\\', '/'))
    )
    .filter(([, text]) => {
      const { code, literals } = scanCode(text)
      let run = ''
      for (let index = 0; index < literals.length; index += 1) {
        const literal = literals[index]
        const previous = literals[index - 1]
        // A `+` (and only whitespace) between two literals makes them one reassembled value.
        const chained = Boolean(previous) && /^\s*\+\s*$/.test(code.slice(previous.end, literal.start))
        run = chained ? run + unescapeLiteral(literal.content) : unescapeLiteral(literal.content)
        if (wrapperPattern.test(run)) return true
      }
      return false
    })
    .map(([name]) => name.replaceAll('\\', '/'))
  check(
    directWrapperNames.length === 0,
    `Gradle wrappers are invoked only through scripts/lib/gradle.mjs${detail(directWrapperNames)}`
  )

  const testRunnerFile = join(ROOT, 'scripts', 'run-slot-tests.mjs')
  check(existsSync(testRunnerFile), 'a sanctioned slot unit-test runner is available')
  if (existsSync(testRunnerFile)) {
    const { runSlotUnitTests } = await import(pathToFileURL(testRunnerFile).href)
    const fakeApps = join(sandbox, 'test-runner-apps')
    const fakeSlot = join(fakeApps, '1970-01-01-runner-fixture')
    mkdirSync(fakeSlot, { recursive: true })

    const noTestsLines = []
    const noTestsCalls = []
    const noTests = runSlotUnitTests('runner-fixture', {
      appsDir: fakeApps,
      report: (message) => noTestsLines.push(message),
      runGradle: (directory, tasks) => {
        noTestsCalls.push({ directory, tasks })
        return { result: { status: 0, stdout: '', stderr: '' }, elapsedMs: 1 }
      }
    })
    check(
      noTests.ok && noTestsCalls.length === 1 && noTestsCalls[0].tasks.includes('testDebugUnitTest'),
      'the slot test runner delegates testDebugUnitTest to runGradleTasks once'
    )
    check(
      noTestsLines.some((message) => message.includes('no tests found')) &&
        !noTestsLines.some((message) => /^PASS\s+testDebugUnitTest/.test(message)),
      'the slot test runner reports no tests found distinctly from PASS'
    )

    const failedLines = []
    const failed = runSlotUnitTests('runner-fixture', {
      appsDir: fakeApps,
      report: (message) => failedLines.push(message),
      runGradle: () => ({ result: { status: 1, stdout: '', stderr: '' }, elapsedMs: 1 })
    })
    check(
      !failed.ok && failed.exitCode !== 0 && failedLines.some((message) => message.includes('FAIL testDebugUnitTest')),
      'a failed slot unit-test task reports FAIL and returns a non-zero exit'
    )
  } else {
    check(false, 'the runner can report no tests found distinctly from PASS')
    check(false, 'a failed slot unit-test task returns a non-zero exit')
  }

  const { RELEASE_TASKS, gradleArgs, runGradleTasks, FORBIDDEN_GRADLE_FLAGS } = await import(
    pathToFileURL(join(ROOT, 'scripts', 'lib', 'gradle.mjs')).href
  )

  check(
    RELEASE_TASKS.length === 3 && RELEASE_TASKS.includes('bundleRelease'),
    'RELEASE_TASKS covers debug, release, and bundle'
  )

  const fakeProject = join(sandbox, 'fake-project')
  mkdirSync(fakeProject, { recursive: true })
  // A non-win32 platform keeps the wrapper path simple and shell-free.
  writeFileSync(join(fakeProject, 'gradlew'), '#!/bin/sh\nexit 0\n', 'utf8')

  const spawnCalls = []
  const fakeSpawn = (command, args, options) => {
    spawnCalls.push({ command, args, options })
    return { status: 0, stdout: 'ok', stderr: '', error: undefined }
  }

  const run = runGradleTasks(fakeProject, RELEASE_TASKS, { spawn: fakeSpawn, platform: 'linux' })

  check(spawnCalls.length === 1, `all release tasks go to ONE Gradle process (got ${spawnCalls.length})`)
  check(
    RELEASE_TASKS.every((task) => spawnCalls[0]?.args?.includes(task)),
    'the single invocation carries every release task'
  )
  check(
    !spawnCalls[0]?.args?.some((arg) => FORBIDDEN_GRADLE_FLAGS.includes(arg)),
    'the invocation adds no --no-daemon flag'
  )
  check(typeof run.elapsedMs === 'number', 'runGradleTasks reports elapsed time for build-cost reporting')

  let rejectedEmpty = false
  try {
    gradleArgs([])
  } catch {
    rejectedEmpty = true
  }
  check(rejectedEmpty, 'an empty task list is refused instead of silently doing nothing')

  let rejectedDaemonFlag = false
  try {
    gradleArgs(['--no-daemon'])
  } catch {
    rejectedDaemonFlag = true
  }
  check(rejectedDaemonFlag, '--no-daemon is refused by the argument builder')

  let missingWrapper = false
  try {
    runGradleTasks(join(sandbox, 'no-such-project'), RELEASE_TASKS, { spawn: fakeSpawn, platform: 'linux' })
  } catch {
    missingWrapper = true
  }
  check(missingWrapper, 'a missing Gradle wrapper fails loudly instead of reporting success')

  // ------------------------------------------------------------------------
  // 7. JDK pinning.
  //
  // Gradle keeps one daemon per JDK, so letting the JDK vary between runs turns
  // an incremental build into a cold one: measured 2.4s under a pinned JDK 17
  // versus 70.1s when an ambient JDK 24 took over. These checks use fake JDK
  // directories and a fake `java -version`, so they are deterministic and do not
  // depend on what is installed on the machine.
  // ------------------------------------------------------------------------

  section('jdk pinning contract')

  const { javaMajorOf, resolveJavaHome, MIN_MAJOR_SUPPORTED } = await import(
    pathToFileURL(join(ROOT, 'scripts', 'lib', 'java.mjs')).href
  )

  const jdk17 = join(sandbox, 'fake-jdk-17')
  const jdk21 = join(sandbox, 'fake-jdk-21')
  const jdk8 = join(sandbox, 'fake-jdk-8')
  for (const home of [jdk17, jdk21, jdk8]) {
    mkdirSync(join(home, 'bin'), { recursive: true })
    writeFileSync(join(home, 'bin', 'java'), '#!/bin/sh\nexit 0\n', 'utf8')
  }

  const versionSpawn = (executable) => {
    if (executable.includes('fake-jdk-8')) return { stdout: '', stderr: 'openjdk version "1.8.0_392"\n' }
    if (executable.includes('fake-jdk-21')) return { stdout: 'openjdk version "21.0.5" 2024-10-15\n', stderr: '' }
    if (executable.includes('fake-jdk-17')) return { stdout: 'openjdk version "17.0.11" 2024-04-16\n', stderr: '' }
    return { stdout: '', stderr: '' }
  }

  check(javaMajorOf(jdk17, { spawn: versionSpawn, platform: 'linux' }) === 17, 'reads the major version of a modern JDK')
  check(javaMajorOf(jdk8, { spawn: versionSpawn, platform: 'linux' }) === 8, 'reads the legacy 1.8 version shape')
  check(
    javaMajorOf(join(sandbox, 'fake-jdk-absent'), { spawn: versionSpawn, platform: 'linux' }) === null,
    'a path that is not a JDK resolves to null'
  )

  const override = resolveJavaHome({
    env: { PLAYKILN_JAVA_HOME: jdk17, JAVA_HOME: jdk21 },
    platform: 'linux',
    spawn: versionSpawn
  })
  check(
    override?.home === jdk17 && override.source === 'PLAYKILN_JAVA_HOME',
    'PLAYKILN_JAVA_HOME wins over JAVA_HOME'
  )

  const honored = resolveJavaHome({ env: { JAVA_HOME: jdk21 }, platform: 'linux', spawn: versionSpawn })
  check(honored?.home === jdk21 && honored.source === 'JAVA_HOME', 'a supported JAVA_HOME is honored')

  check(
    resolveJavaHome({ env: { JAVA_HOME: jdk8 }, platform: 'linux', spawn: versionSpawn }) === null,
    `a JDK below major ${MIN_MAJOR_SUPPORTED} is refused instead of used`
  )

  const pinCalls = []
  const pinSpawn = (command, args, options) => {
    pinCalls.push({ command, args, options })
    return { status: 0, stdout: '', stderr: '' }
  }
  const pinnedRun = runGradleTasks(fakeProject, RELEASE_TASKS, {
    spawn: pinSpawn,
    platform: 'linux',
    javaHome: jdk17,
    env: { PLAYKILN_MARKER: 'yes' }
  })
  check(pinCalls[0]?.options?.env?.JAVA_HOME === jdk17, 'the Gradle process receives the pinned JAVA_HOME')
  check(
    pinCalls[0]?.options?.env?.PLAYKILN_MARKER === 'yes',
    'pinning the JDK preserves the rest of the environment'
  )
  check(pinnedRun.javaHome === jdk17, 'the run reports which JDK it used')

  const inheritCalls = []
  const inheritSpawn = (command, args, options) => {
    inheritCalls.push({ options })
    return { status: 0, stdout: '', stderr: '' }
  }
  runGradleTasks(fakeProject, RELEASE_TASKS, {
    spawn: inheritSpawn,
    platform: 'linux',
    javaHome: null,
    env: { PLAYKILN_MARKER: 'yes' }
  })
  check(
    !('JAVA_HOME' in (inheritCalls[0]?.options?.env || {})),
    'an explicitly skipped JDK leaves the ambient environment untouched'
  )
} finally {
  if (KEEP) {
    console.log(`\n  INFO      sandbox kept at ${sandbox}`)
  } else {
    rmSync(sandbox, { recursive: true, force: true })
  }
}

// --------------------------------------------------------------------------
// 9. The Codex worker ledger must notice a worker that died without reporting.
//
// `capability-integration2` recorded `status: running` and pid 10188, then its wrapper
// died without writing an exit record. The ledger reported `running` forever, so the
// orchestrator could have waited on a process that no longer existed. These checks
// drive the pure classifier; the CLI is wired to the same module.
//
// The liveness check is deliberately injectable so its ESRCH/EPERM handling is tested
// deterministically instead of depending on which pids happen to be alive.
// --------------------------------------------------------------------------

section('codex worker ledger')

const { LOST_STATE, LOST_NOTE, classifyWorkerState, classifyWorkerFromDisk, isProcessAlive, readJsonSafe } =
  await import(pathToFileURL(join(ROOT, 'scripts', 'lib', 'codex-ledger.mjs')).href)

// Precedence must not move: a diagnosed signal outranks even a zero exit code.
check(
  classifyWorkerState({
    status: 'running',
    exitRecord: { exitCode: 0, signal: 'RATE_LIMITED' },
    alive: true
  }) === 'BLOCKED(RATE_LIMITED)',
  'a signal in the exit record wins over a zero exit code'
)
check(
  classifyWorkerState({
    status: 'running',
    exitRecord: { exitCode: 7, signal: 'AUTH_ERROR' },
    alive: true
  }) === 'BLOCKED(AUTH_ERROR)',
  'a signal in the exit record wins over a non-zero exit code too'
)
check(
  classifyWorkerState({ status: 'running', exitRecord: { exitCode: 1 }, alive: true }) === 'FAILED(1)',
  'a non-zero exit code with no signal is FAILED'
)
check(
  classifyWorkerState({ status: 'running', exitRecord: { exitCode: 0 }, alive: false }) === 'DONE',
  'a zero exit code is DONE even if the pid is already gone'
)

// The defect itself: running, no exit record, and the recorded pid is dead.
check(
  classifyWorkerState({ status: 'running', exitRecord: null, alive: false }) === LOST_STATE,
  `running + no exit record + dead pid is ${LOST_STATE} (was: running)`
)
check(
  !['DONE', 'running'].includes(
    classifyWorkerState({ status: 'running', exitRecord: null, alive: false })
  ),
  `${LOST_STATE} cannot be mistaken for success or for a healthy run`
)
check(
  LOST_NOTE.includes('unverified'),
  'the lost-state note tells the reader the work is unverified'
)
check(
  classifyWorkerState({ status: 'running', exitRecord: null, alive: true }) === 'running',
  'running + no exit record + live pid still reports running (the check must discriminate)'
)
check(
  classifyWorkerState({ status: 'running', exitRecord: null, alive: undefined }) === 'running',
  'an undetermined liveness never fabricates LOST'
)
check(
  classifyWorkerState({ status: 'blocked', exitRecord: null, alive: false }) === 'blocked',
  'a non-running recorded status keeps its existing label'
)
check(
  classifyWorkerState({ status: 'done', exitRecord: undefined, alive: false }) === 'done',
  'an absent (not just null) exit record preserves the recorded status'
)
check(
  classifyWorkerState({}) === 'unknown',
  'a meta with no status still resolves to unknown'
)

// Liveness: only ESRCH proves death; EPERM means alive.
check(
  isProcessAlive(process.pid) === true,
  'the current process reports alive'
)
check(
  isProcessAlive(1234, () => undefined) === true,
  'a pid the OS accepts reports alive'
)
check(
  isProcessAlive(1234, () => {
    throw Object.assign(new Error('gone'), { code: 'ESRCH' })
  }) === false,
  'ESRCH reports not alive'
)
check(
  isProcessAlive(1234, () => {
    throw Object.assign(new Error('not mine'), { code: 'EPERM' })
  }) === true,
  'EPERM counts as alive, because "exists but not mine" is not "dead"'
)
let killCalledForGarbage = false
check(
  isProcessAlive(null, () => {
    killCalledForGarbage = true
  }) === false && killCalledForGarbage === false,
  'a missing pid is not alive and is never handed to the OS'
)
check(isProcessAlive('not-a-pid') === false, 'a non-numeric pid reports not alive')
check(isProcessAlive(0) === false, 'pid 0 reports not alive')

// The disk-derived wrapper is what the CLI actually calls.
check(
  classifyWorkerFromDisk(
    { status: 'running', pid: 4321 },
    null,
    { kill: () => {
      throw Object.assign(new Error('gone'), { code: 'ESRCH' })
    } }
  ) === LOST_STATE,
  'classifyWorkerFromDisk derives LOST from a dead meta pid'
)
check(
  classifyWorkerFromDisk({ status: 'running', pid: process.pid }, null) === 'running',
  'classifyWorkerFromDisk leaves a live worker reporting running'
)

// A worker killed mid-`writeFileSync` leaves a truncated meta file, and a hand-edited one can gain
// a BOM; both make `JSON.parse` throw. Measured on this repo: with one such file present, `list`
// printed four rows and exited non-zero, hiding every worker after the broken one — the ledger is
// the orchestrator's only view of what is running, so one unreadable file must not blind it. The
// reader has to surface the file instead of throwing, and the listing has to keep every worker.
//
// The reader is injected so the check does not shell out and does not depend on a real file; and
// so that a reverted `readJsonSafe` (one that rethrows) makes these checks FAIL rather than crash
// the suite: the helper catches the throw and reports it as a missing `ok`, which no check accepts.
function readOutcome(text) {
  try {
    return readJsonSafe('worker.meta.json', () => text)
  } catch (error) {
    return { threw: error?.message ?? String(error) }
  }
}

const truncatedRead = readOutcome('{"id":"capability-integration2","status":"run')
check(
  truncatedRead.ok === false && truncatedRead.threw === undefined && truncatedRead.error.length > 0,
  'a truncated meta file is reported unreadable rather than thrown'
)

const bomRead = readOutcome('\uFEFF{"id":"worker","status":"done"}')
check(
  bomRead.ok === false && bomRead.threw === undefined,
  'a BOM-prefixed meta file is reported unreadable rather than thrown'
)

const validRead = readOutcome('{"id":"worker","status":"done"}')
check(
  validRead.ok === true && validRead.value?.status === 'done',
  'a valid meta file still parses (the check must discriminate)'
)

// The listing shape itself: one malformed meta among good ones must not abort the iteration.
const listingWorkers = [
  { id: 'first', text: '{"status":"running"}' },
  { id: 'broken', text: '{"status":"run' },
  { id: 'bom', text: '\uFEFF{"status":"done"}' },
  { id: 'last', text: '{"status":"done"}' }
]
const listingRows = listingWorkers.map((w) => readOutcome(w.text))
check(
  listingRows.length === listingWorkers.length &&
    listingRows.every((r) => r.threw === undefined) &&
    listingRows.filter((r) => r.ok === true).length === 2 &&
    listingRows.filter((r) => r.ok === false).length === 2,
  'a listing containing a malformed meta does not abort and keeps every worker'
)

console.log(`\nVERDICT: ${failed ? 'FAIL' : 'PASS'}`)
process.exitCode = failed ? 1 : 0

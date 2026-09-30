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
  readdirSync,
  rmSync,
  utimesSync,
  writeFileSync
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { isExcludedScaffoldPath, walkFiles } from './new-slot.mjs'
import { PLACEHOLDER_HOSTS } from './lib/in-app-policy.mjs'
import { applicationIdForSlug, applicationIdPattern, packagePrefix, scaffoldPackage } from './lib/namespaces.mjs'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const SCAFFOLD = join(ROOT, 'scaffold')
const SLOT_SCAFFOLD = join(SCAFFOLD, 'android-compose')
const KEEP = process.argv.includes('--keep')

// Python probes below read Russian and English console text, so on a Windows console whose
// codepage cannot represent every character they die with UnicodeEncodeError and the gate goes
// red for a reason that has nothing to do with what it grades. Measured 2026-09-29: the `→` in a
// Russian page path inside `scripts/lib/console-form.json` killed `test-console-answers.py` under
// cp1251, and `scripts/selftest.mjs` reported `console-answers test exits 0 -> FAIL` on a machine
// where the suite passed all 113 of its own content legs. A verdict must not depend on the
// terminal's glyph set. Setting it on the parent environment rather than on each `spawnSync`
// options object is deliberate: children inherit it, so a probe added later cannot forget it.
process.env.PYTHONIOENCODING = 'utf-8'
process.env.PYTHONUTF8 = '1'

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
  // The whole `scripts/` tree, copied once, here, through the factory's own exclusion predicate.
  //
  // This replaced a hand-maintained list of individual files, and that list is a defect generator: it
  // fails by omission, and it fails as a *crash* rather than a readable FAIL line. It was caught twice
  // in one session — `lib/gradle.mjs` grew an import of `lib/slot-lock.mjs` and every sandboxed
  // `verify-slot.mjs` died with `ERR_MODULE_NOT_FOUND`, taking twelve capability assertions red for a
  // reason unrelated to what they grade; and `play-upload.mjs` grew an import of
  // `lib/release-approval.mjs` while the section that copies it was added without its module, so
  // `play-upload.mjs --help` inside the sandbox printed a stack trace instead of the flags it documents
  // (the assertion for `--help documents target release` passed on the word "release" inside the error
  // text, which is how a crash reads as a partial success).
  //
  // Copying the tree makes every future addition automatic, and the predicate is shared with
  // `new-slot.mjs`, so "what must not be copied" has one answer: build outputs and machine-local files
  // (`__pycache__`, `*.pyc`) are filtered out, and the scaffold-integrity section proves the filter works.
  cpSync(join(ROOT, 'scripts'), join(sandbox, 'scripts'), {
    recursive: true,
    dereference: false,
    filter: (source) => {
      const rel = relative(join(ROOT, 'scripts'), source)
      return rel === '' || !isExcludedScaffoldPath(rel)
    }
  })
  // The two recon documents exist because a promise's evidence and the contract's `reconRef` are
  // resolved on disk: a citation to a file that is not there is the dangling reference the gate is
  // meant to catch.
  mkdirSync(join(sandbox, 'docs', 'research'), { recursive: true })
  mkdirSync(join(sandbox, 'docs', 'ideation'), { recursive: true })
  writeFileSync(
    join(sandbox, 'docs', 'research', 'selftest-evidence.md'),
    '# Self-test recon evidence\n\nMeasured for the factory self-test only.\n',
    'utf8'
  )
  writeFileSync(
    join(sandbox, 'docs', 'ideation', 'selftest-recon.md'),
    '# Self-test recon\n\nWritten for the factory self-test only.\n',
    'utf8'
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
for (const entry of ['keystore.properties', '*.jks', '*.keystore', '.env', 'local.properties', 'release-approval.json', 'promo-codes.csv', 'promotion_codes.csv']) {
  check(scaffoldIgnore.includes(entry), `scaffold .gitignore covers ${entry}`)
}
// The console exports promo codes under its own filename, so a rule covering only the
// factory's `promo-codes.csv` left the real download untracked-but-committable. The root
// `.gitignore` is the backstop for a file that lands at the repository root instead of in a
// slot, and it has to name the same two files or the protection depends on the folder.
const rootIgnore = readFileSync(join(ROOT, '.gitignore'), 'utf8').split(/\r?\n/).map((item) => item.trim())
for (const entry of ['promo-codes.csv', 'promotion_codes.csv']) {
  check(rootIgnore.includes(entry), `root .gitignore covers ${entry}`)
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

/**
 * A complete, valid plan. `new-slot.mjs` must be handed one for every run that is
 * expected to reach the filesystem. Overrides let a check break exactly one field,
 * so the failure it observes is attributable to that field and not to the fixture.
 */
const ECONOMICS_EVIDENCE_REF = 'docs/research/2026-09-23-plant-vertical-evidence.md'

function validEconomics(overrides = {}) {
  return {
    schemaVersion: 1,
    archetype: 'purchase_led_single_unlock',
    payingUser: 'A gas fitter who sizes a pipe run on site and needs the result recorded.',
    paidUnit: 'One permanent unlock of the export and multi-run comparison features.',
    priceUsd: 4.99,
    momentOfPayment: 'After the first saved sizing run, when the fitter taps export.',
    distinctnessSentence: 'It records the code edition and table each reading came from so a result can be re-checked.',
    evidenceRef: `${ECONOMICS_EVIDENCE_REF}#demand`,
    day1RevenuePath: 'The first paid unlock arrives from a fitter exporting a run for a permit packet.',
    killCriteria: { windowDays: 30, minInstalls: 500, minRevenueUsd: 50 },
    assumptionsUnverified: ['Fitters may prefer a paper form to a phone for permit paperwork.'],
    ...overrides
  }
}

function writeEconomicsFixture(name, overrides) {
  const file = join(sandbox, name)
  writeFileSync(file, `${JSON.stringify(validEconomics(overrides), null, 2)}\n`, 'utf8')
  return file
}

const validEconomicsFile = writeEconomicsFixture('economics-valid.json')

/**
 * A valid contract in the state a fresh plan is in: every promise `deferred`. The gate
 * must accept that at scaffold time, because nothing has been built. That half is asserted
 * by the dash-slug slot further down; the refusal half is asserted by the `diffdeferred`
 * leg in the differentiator section, against its own slot.
 */
const SELFTEST_EVIDENCE = 'docs/research/selftest-evidence.md'

function deferredClaim(id, weight, promise) {
  return {
    id,
    weight,
    promise,
    evidence: {
      strength: 'MEASURED',
      source: `${SELFTEST_EVIDENCE}#demand`,
      finding: '3 of the 5 incumbents refuse this case in their own review replies.'
    },
    status: 'deferred',
    deferredReason: 'Not built yet in the sandbox fixture.'
  }
}

function validDifferentiators(overrides = {}) {
  return {
    schemaVersion: 1,
    slot: 'selftest',
    reconRef: SELFTEST_EVIDENCE,
    claims: [
      deferredClaim('refuses-no-case', 'core', 'Returns a result for the cases the incumbents refuse.'),
      deferredClaim('cites-its-source', 'core', 'Cites the table every printed number came from.'),
      deferredClaim('no-ad-over-input', 'supporting', 'Never covers an input with an advertisement.')
    ],
    ...overrides
  }
}

function writeDifferentiatorsFixture(name, overrides) {
  const file = join(sandbox, name)
  writeFileSync(file, `${JSON.stringify(validDifferentiators(overrides), null, 2)}\n`, 'utf8')
  return file
}

const validDifferentiatorsFile = writeDifferentiatorsFixture('differentiators-valid.json')

/**
 * The same three promises, fully accounted for, for the slot every later section reads.
 *
 * That slot is the fixture the readiness section depends on: it is the slot that has to pass every
 * check *except* the build, which is what makes "a run without `--build` reports UNVERIFIED and exits
 * 2" testable at all. Its contract therefore cannot be the deferred one - `verify-slot` fails a slot
 * whose reason list is unresolved, so the promise gate would answer first and mask the missing-build
 * verdict the section exists to check. The first version of the gate left this fixture deferred and
 * those two legs went red for a reason that had nothing to do with what they measure.
 *
 * Nothing is lost by resolving it: the deferred fixture is still scaffolded into the dash-slug slot
 * below, and the refusal half of the rule is asserted by the `diffdeferred` leg in the differentiator
 * section, against a slot of its own.
 */
function resolvedClaim(id, weight, promise, proof) {
  return {
    id,
    weight,
    promise,
    evidence: {
      strength: 'MEASURED',
      source: `${SELFTEST_EVIDENCE}#demand`,
      finding: '3 of the 5 incumbents refuse this case in their own review replies.'
    },
    status: 'shipped',
    proof
  }
}

function resolvedDifferentiators() {
  const code = 'app/src/main/java/com/gexetr/selftest'
  return {
    schemaVersion: 1,
    slot: 'selftest',
    reconRef: SELFTEST_EVIDENCE,
    claims: [
      resolvedClaim(
        'refuses-no-case',
        'core',
        'Returns a result for the cases the incumbents refuse.',
        {
          type: 'code',
          ref: `${code}/CaseEditor.kt`,
          symbol: 'refuseNoCase',
          assertion: 'The editor rejects the cases the incumbents refuse instead of returning a blank result.'
        }
      ),
      resolvedClaim('cites-its-source', 'core', 'Cites the table every printed number came from.', {
        type: 'code',
        ref: `${code}/CaseReport.kt`,
        symbol: 'citationLine',
        assertion: 'The report prints the line naming the table each number was read from.'
      }),
      resolvedClaim('no-ad-over-input', 'supporting', 'Never covers an input with an advertisement.', {
        type: 'test',
        ref: 'app/src/test/java/com/gexetr/selftest/NoAdOverInputTest.kt',
        symbol: 'holdsThePromise',
        assertion: 'The test asserts no ad surface is composed above a numeric input.'
      })
    ]
  }
}

const resolvedDifferentiatorsFile = join(sandbox, 'differentiators-resolved.json')
writeFileSync(
  resolvedDifferentiatorsFile,
  `${JSON.stringify(resolvedDifferentiators(), null, 2)}\n`,
  'utf8'
)

/**
 * The artifacts the resolved contract above points at, written into the scaffolded slot. The gate
 * reads their text and requires the named symbol to be in the file it names, so these are ordinary
 * sources rather than stand-ins: removing a symbol is what the regression legs below do.
 */
function writeSandboxProofs(dir) {
  const codeDir = join(dir, 'app', 'src', 'main', 'java', 'com', 'gexetr', 'selftest')
  const testDir = join(dir, 'app', 'src', 'test', 'java', 'com', 'gexetr', 'selftest')
  mkdirSync(testDir, { recursive: true })
  writeFileSync(
    join(codeDir, 'CaseEditor.kt'),
    'package com.gexetr.selftest\n\nfun refuseNoCase(): Boolean = true\n',
    'utf8'
  )
  writeFileSync(
    join(codeDir, 'CaseReport.kt'),
    'package com.gexetr.selftest\n\nfun citationLine(): String = "from the table"\n',
    'utf8'
  )
  writeFileSync(
    join(testDir, 'NoAdOverInputTest.kt'),
    'package com.gexetr.selftest\n\nclass NoAdOverInputTest {\n  fun holdsThePromise() {}\n}\n',
    'utf8'
  )
}

try {
  const created = runNewSlot(sandbox, [
    '--slug', 'selftest',
    '--name', 'Selftest App',
    '--idea', 'sandbox slot for the factory self-test',
    '--date', slotDate,
    '--economics', validEconomicsFile,
    '--differentiators', resolvedDifferentiatorsFile
  ])
  const createdOk = check(created.status === 0, `exits 0 (got ${created.status})`)
  if (!createdOk) console.log(created.output.trim().split(/\r?\n/).map((l) => `      ${l}`).join('\n'))
  // The contract this slot was scaffolded with names artifacts, so they have to exist for the slot
  // to be the "everything passes except the build" fixture the readiness section reads.
  if (created.status === 0) writeSandboxProofs(slotDir)

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

    // The whole point of the gate: the validated plan must land in the slot, and the
    // recorded state must carry it forward, not merely accept it and discard it.
    const slotEconomicsFile = join(slotDir, 'economics.json')
    check(existsSync(slotEconomicsFile), 'writes economics.json into the slot root')
    let slotEconomics = null
    try {
      slotEconomics = JSON.parse(readFileSync(slotEconomicsFile, 'utf8'))
    } catch {
      slotEconomics = null
    }
    check(
      slotEconomics?.archetype === 'purchase_led_single_unlock',
      'economics.json keeps the validated archetype'
    )

    const slotStateFile = join(slotDir, '.kiln-state.json')
    let slotState = null
    try {
      slotState = JSON.parse(readFileSync(slotStateFile, 'utf8'))
    } catch {
      slotState = null
    }
    check(
      slotState?.economics?.archetype === 'purchase_led_single_unlock' &&
        slotState?.economics?.priceUsd === 4.99,
      '.kiln-state.json carries the economics archetype and priceUsd'
    )

    // The promise contract must land in the slot before the UI exists, and its `slot`
    // field is forced to this slot so a plan copied from another slot cannot claim this
    // slot's recon.
    const slotContractFile = join(slotDir, 'differentiators.json')
    check(existsSync(slotContractFile), 'writes differentiators.json into the slot root')
    let slotContract = null
    try {
      slotContract = JSON.parse(readFileSync(slotContractFile, 'utf8'))
    } catch {
      slotContract = null
    }
    check(slotContract?.slot === 'selftest', 'differentiators.json records this slot rather than the plan author\'s guess')
    check(slotContract?.claims?.length === 3, 'differentiators.json keeps every promise from the plan')
    check(
      slotState?.differentiators?.promises === 3 && slotState?.differentiators?.core === 2,
      '.kiln-state.json carries the promise and core counts'
    )
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
  check(
    registered?.archetype === 'purchase_led_single_unlock',
    'registry records the validated archetype'
  )
  check(registered?.promises === 3, 'registry records the promise count')

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

  // An economics plan alone is no longer enough: recon's reason list is what the app
  // promises to be, and a slot created without one has nothing to be held to.
  const planless = runNewSlot(sandbox, [
    '--slug', 'planless', '--name', 'X', '--idea', 'y', '--date', slotDate,
    '--economics', validEconomicsFile
  ])
  check(
    planless.status !== 0 && !existsSync(join(sandbox, 'apps', `${slotDate}-planless`)),
    'refuses a slot with an economics plan but no promise contract, and writes nothing'
  )
  check(
    /--differentiators/.test(planless.output),
    'the refusal names the missing --differentiators'
  )

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
  // 4b. A slot cannot exist without a validated economics plan.
  //
  // The factory's first real slot shipped with capabilities.json enabled: [] —
  // no revenue mechanism — and every gate returned PASS. These checks prove a
  // planless or placeholder plan now creates nothing on disk, instead of being
  // accepted and discarded.
  // ------------------------------------------------------------------------

  section('economics plan gate')

  const noPlan = runNewSlot(sandbox, [
    '--slug', 'no-plan', '--name', 'No Plan', '--idea', 'a slot with no economics plan', '--date', slotDate
  ])
  check(noPlan.status !== 0, 'refuses a slot with no --economics plan')
  check(noPlan.output.includes('--economics'), 'the missing-plan refusal names --economics')
  check(
    !existsSync(join(sandbox, 'apps', `${slotDate}-no-plan`)),
    'a run with no plan creates no slot directory'
  )

  const zeroPriceFile = writeEconomicsFixture('economics-zero-price.json', { priceUsd: 0 })
  const zeroPrice = runNewSlot(sandbox, [
    '--slug', 'zero-price', '--name', 'Zero', '--idea', 'a paid plan priced at zero', '--date', slotDate,
    '--economics', zeroPriceFile
  ])
  check(zeroPrice.status !== 0, 'refuses a paid archetype priced at zero')
  // The expected text is written out literally, not derived from the validator, so
  // a message that drifts into vagueness fails this check.
  check(
    zeroPrice.output.includes(
      'priceUsd must be greater than 0 for archetype "purchase_led_single_unlock"; a paid archetype needs a price, because 0 cannot earn'
    ),
    'the zero-price refusal states that a paid archetype needs a price'
  )
  check(
    !existsSync(join(sandbox, 'apps', `${slotDate}-zero-price`)),
    'a zero-price plan creates no slot directory'
  )

  const placeholderFile = writeEconomicsFixture('economics-placeholder.json', { paidUnit: 'TBD' })
  const placeholder = runNewSlot(sandbox, [
    '--slug', 'placeholder', '--name', 'Placeholder', '--idea', 'a plan that was never filled in',
    '--date', slotDate, '--economics', placeholderFile
  ])
  check(placeholder.status !== 0, 'refuses a placeholder paidUnit')
  check(
    placeholder.output.includes('paidUnit must be a real plan, not a placeholder'),
    'the placeholder refusal names the field and the reason'
  )
  check(
    !existsSync(join(sandbox, 'apps', `${slotDate}-placeholder`)),
    'a placeholder plan creates no slot directory'
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
    '--slug', 'my-app', '--name', 'Dashed', '--idea', 'dash case', '--date', slotDate,
    '--economics', validEconomicsFile,
    '--differentiators', validDifferentiatorsFile
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

  // The sandbox's `scripts/` tree is populated in full by `createSandbox()`, so the upload guards need
  // no copies of their own — that per-section copying is what missed `play-upload.mjs`'s new import of
  // `lib/release-approval.mjs` and turned a help-text assertion into a stack trace.

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

  // `play-upload.mjs` is the only script that talks to Google Play, and it did not ask whether the
  // revision was approved: an independent review drove it to a complete `--dry-run` plan for a slot no
  // owner had approved. It now refuses before the pack is read, before the credentials file is opened and
  // before any plan is printed, so this section needs a fixture approval to exercise the guards behind it
  // — and the refusal is asserted *before* it is granted, because that assertion is the regression test
  // for the finding. The third check is the one that matters most: a refusal that still prints the plan
  // would leak exactly what the gate exists to withhold.
  const uploadUnapprovedSlot = runUpload(['--slot', 'upload', '--only', 'details', '--dry-run'])
  check(uploadUnapprovedSlot.status !== 0, 'an unapproved slot is refused before any Play call')
  check(
    uploadUnapprovedSlot.output.includes('approve-release.mjs'),
    'the uploader refusal names the command that records approval'
  )
  check(
    !uploadUnapprovedSlot.output.includes('updateMask'),
    'the refusal precedes the dry-run plan, so an unapproved revision is not planned'
  )

  const uploadApproval = spawnSync(
    process.execPath,
    [
      join(sandbox, 'scripts', 'approve-release.mjs'),
      '--slot-dir', uploadSlot,
      '--note', 'selftest fixture: this approval exists only inside a temporary sandbox directory'
    ],
    { cwd: sandbox, encoding: 'utf8', windowsHide: true }
  )
  check(
    uploadApproval.status === 0,
    `the fixture approval is recorded (${`${uploadApproval.stdout || ''}${uploadApproval.stderr || ''}`.trim().split(/\r?\n/).pop()})`
  )

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
  //
  // Every script `verify-slot.mjs` invokes belongs in this list. Wiring a new gate
  // into the readiness gate without adding its helper here does not weaken the
  // sandbox politely: the run reports FAIL for the missing helper, so a build-less
  // run stops reporting UNVERIFIED and this section's own honesty checks break. That
  // is how `check-ui-coverage.mjs` was missed when the claimed-feature gate was added.
  //
  // `lib/console-form.json` is on the list for the same reason: the store-pack check
  // judges each document's absence against the inventory that file carries, so a
  // sandbox without it cannot read the pack's contract at all.
  // `verify-slot.mjs` drives the money gate, which resolves the plan's evidenceRef against the repo root
  // and pairs the archetype with capabilities.json, so both must exist here — the scripts tree itself
  // comes from `createSandbox()`.
  //
  // The measured-facts ledger is here for the same reason and was missed the same way: `verify-slot.mjs`
  // runs `check-freshness.mjs` on every slot, and that gate reads `docs/research/measured-facts.json`
  // from the root it is run in. Without the file the sandbox run reported a FAIL, so this section's
  // UNVERIFIED-exit assertion failed with `got 1` instead of 2 — again for a reason unrelated to what it
  // grades.
  mkdirSync(join(sandbox, 'docs', 'research'), { recursive: true })
  copyFileSync(
    join(ROOT, ...ECONOMICS_EVIDENCE_REF.split('/')),
    join(sandbox, ...ECONOMICS_EVIDENCE_REF.split('/'))
  )
  copyFileSync(
    join(ROOT, 'docs', 'research', 'measured-facts.json'),
    join(sandbox, 'docs', 'research', 'measured-facts.json')
  )

  // `check-freshness.mjs` verifies that every path the ledger names in `whereUsed` exists in the
  // repository, which is what makes a renamed rule file fail instead of silently orphaning the fact it
  // enforced. The sandbox is a partial copy, so the gate was right and the sandbox was incomplete:
  // `verify-slot.mjs` inside it reported `LEDGER-WHERE-USED-MISSING: play-listing-text-caps.whereUsed
  // names AGENTS.md`, which turned this section's "an UNVERIFIED verdict exits 2" assertion into
  // `got 1` — a red self-test for a reason that had nothing to do with what it grades.
  //
  // The files are therefore derived from the ledger rather than listed here: a hand-kept list of the
  // files a ledger points at is the same drift in a new place, and the ledger is the thing that already
  // knows the answer.
  const measuredLedger = JSON.parse(
    readFileSync(join(ROOT, 'docs', 'research', 'measured-facts.json'), 'utf8')
  )
  const ledgerFacts = Array.isArray(measuredLedger) ? measuredLedger : measuredLedger.facts ?? []
  const ledgerReferences = new Set()
  for (const fact of ledgerFacts) {
    for (const where of fact.whereUsed ?? []) {
      if (typeof where === 'string' && !/^[a-z]+:\/\//i.test(where)) ledgerReferences.add(where)
    }
  }
  let ledgerReferencesCopied = 0
  for (const relativePath of ledgerReferences) {
    const source = join(ROOT, relativePath)
    if (!existsSync(source)) continue
    const target = join(sandbox, relativePath)
    mkdirSync(dirname(target), { recursive: true })
    cpSync(source, target, { recursive: true, dereference: false })
    ledgerReferencesCopied += 1
  }
  check(
    ledgerReferencesCopied > 0,
    `the measured-facts ledger's referenced files are present in the sandbox (${ledgerReferencesCopied} copied, ${ledgerReferences.size} named)`
  )
  const readinessCapabilities = join(slotDir, 'capabilities.json')
  writeFileSync(
    readinessCapabilities,
    JSON.stringify({ schemaVersion: 1, enabled: ['billing'] }, null, 2),
    'utf8'
  )

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
  // When this section is red, the reason is inside the nested run and the nested output is otherwise
  // discarded — which cost three self-test runs to diagnose once: the sandbox's `verify-slot.mjs` was
  // reporting `LEDGER-WHERE-USED-MISSING: play-listing-text-caps.whereUsed names AGENTS.md`, and nothing
  // in this log said so. An assertion that fails without showing its evidence is the defect the assertion
  // was written to catch.
  const noBuildVerdict = /VERDICT: (\S+)/.exec(noBuildRun.output)?.[1] ?? 'none'
  if (noBuildVerdict !== 'UNVERIFIED' || noBuildRun.status !== 2) {
    console.log(`         nested verdict: ${noBuildVerdict}, exit ${noBuildRun.status}`)
    for (const row of noBuildRun.output
      .split(/\r?\n/)
      .filter((line) => /^\s*(FAIL|UNVERIFIED)\b/.test(line))
      .slice(0, 10)) {
      console.log(`         nested: ${row.trim()}`)
    }
  }
  check(
    !/VERDICT: PASS/.test(noBuildRun.output),
    'a run that skips the build never reports PASS'
  )
  check(
    noBuildRun.output.includes('VERDICT: UNVERIFIED'),
    'a run that skips the build reports UNVERIFIED'
  )
  check(noBuildRun.status === 2, `an UNVERIFIED verdict exits 2 (got ${noBuildRun.status})`)

  // Leave no capabilities.json behind: the capability-compliance section asserts that
  // store-kit bootstraps a registry that explicitly enables none.
  rmSync(readinessCapabilities, { force: true })

  // ------------------------------------------------------------------------
  // 7b. Release runs only after the owner's approval, and only for the revision approved.
  //
  // The factory used to build, sign, photograph and pack an app in one undivided pass, so
  // the owner first saw the UI after all of that had been paid for. These checks drive the
  // real CLI: an unapproved slot is refused, the record must carry the owner's own words,
  // any edit to a build input voids it, and release *outputs* (store/, dist/) must not —
  // the release pipeline writes those after approval, so a digest that included them would
  // void its own approval mid-run.
  // ------------------------------------------------------------------------

  section('release approval gate')

  const approveScript = join(ROOT, 'scripts', 'approve-release.mjs')
  const approvalFile = join(slotDir, 'release-approval.json')

  function runApprove(args) {
    const result = spawnSync(process.execPath, [approveScript, ...args], {
      cwd: ROOT, encoding: 'utf8', windowsHide: true
    })
    return { status: result.status, output: `${result.stdout || ''}${result.stderr || ''}` }
  }

  function runStoreKitCli(args) {
    const result = spawnSync(
      'python',
      [join(ROOT, 'scripts', 'store-kit.py'), '--slot-dir', slotDir, ...args],
      { cwd: ROOT, encoding: 'utf8', windowsHide: true }
    )
    return { status: result.status, output: `${result.stdout || ''}${result.stderr || ''}` }
  }

  const { checkReleaseApproval } = await import(
    pathToFileURL(join(ROOT, 'scripts', 'lib', 'release-approval.mjs')).href
  )

  const unapproved = runApprove(['--check', '--slot-dir', slotDir])
  check(unapproved.status === 2, `an unapproved slot does not pass the gate (got ${unapproved.status})`)
  check(
    unapproved.output.includes('approve-release.mjs'),
    'the refusal names the command that records approval'
  )
  check(!existsSync(approvalFile), 'checking the gate never writes an approval')

  const noNote = runApprove(['--slot-dir', slotDir])
  check(noNote.status !== 0, "recording an approval without the owner's words is refused")
  check(!existsSync(approvalFile), 'a refused approval writes nothing')

  const gatedRender = runStoreKitCli(['--render-only'])
  check(gatedRender.status !== 0, 'store-kit refuses to render assets before approval')
  check(
    gatedRender.output.includes('approve-release.mjs'),
    'the store-kit refusal comes from the shared check, not a second copy of the rule'
  )

  const recorded = runApprove([
    '--slot-dir', slotDir, '--note', 'Owner approved the release after reviewing the UI.'
  ])
  check(recorded.status === 0, `recording the owner's approval exits 0 (got ${recorded.status})`)
  check(existsSync(approvalFile), 'the approval is written into the slot')
  check(
    JSON.parse(readFileSync(approvalFile, 'utf8')).note?.includes('Owner approved'),
    "the record keeps the owner's own words"
  )

  const granted = checkReleaseApproval(slotDir)
  check(granted.ok === true, 'a recorded approval opens the gate')

  // The record is bound to the revision, not to the slot name.
  const contractGradle = join(slotDir, 'app', 'build.gradle.kts')
  const approvedGradle = readFileSync(contractGradle, 'utf8')
  writeFileSync(contractGradle, `${approvedGradle}\n// a post-approval edit\n`, 'utf8')
  const tampered = checkReleaseApproval(slotDir)
  check(tampered.ok === false, 'an edit to the app voids the approval')
  check(
    String(tampered.reason).includes('changed after the owner approved'),
    'the refusal explains that the approved revision no longer exists'
  )

  mkdirSync(join(slotDir, 'store'), { recursive: true })
  writeFileSync(join(slotDir, 'store', 'gate-probe.txt'), 'release output\n', 'utf8')
  writeFileSync(join(slotDir, 'dist', 'gate-probe.txt'), 'release output\n', 'utf8')
  check(
    checkReleaseApproval(slotDir).ok === false,
    'release outputs do not re-open an approval that a source edit voided'
  )

  writeFileSync(contractGradle, approvedGradle, 'utf8')
  const restored = checkReleaseApproval(slotDir)
  check(
    restored.ok === true,
    'restoring the approved bytes re-opens the gate while store/ and dist/ outputs exist'
  )
  check(
    existsSync(join(slotDir, 'store', 'gate-probe.txt')) &&
      existsSync(join(slotDir, 'dist', 'gate-probe.txt')),
    'the probe outputs are still on disk, so the previous check is discriminating'
  )
  rmSync(join(slotDir, 'store', 'gate-probe.txt'), { force: true })
  rmSync(join(slotDir, 'dist', 'gate-probe.txt'), { force: true })

  // The factory's own promotions step writes the reviewer's promo-code batch beside the slot,
  // and that batch must not count as a build input. Counting it made the release pipeline void
  // the approval for the revision the owner had just reviewed while the app was byte-identical:
  // `--check` reported `236 now` against `235 approved`, and the whole difference was this one
  // gitignored CSV of Play promo codes sitting in the slot root (`harness/lessons.md`, 2026-09-29).
  // The second half is what makes the first half discriminating: a file the curated list does not
  // name still voids the approval, so this cannot pass by exempting everything beside the slot.
  for (const promoName of ['promotion_codes.csv', 'promo-codes.csv']) {
    const batch = join(slotDir, promoName)
    writeFileSync(batch, 'code,redeemed\nABCD-EFGH-1234,false\n', 'utf8')
    check(
      checkReleaseApproval(slotDir).ok === true,
      `a promo-code batch (${promoName}) does not void the approval it was minted for`
    )
    rmSync(batch, { force: true })
  }
  const unrelatedProbe = join(slotDir, 'PROBE-not-an-excluded-name.md')
  writeFileSync(unrelatedProbe, 'a slot-root file the curated exclusion list does not name\n', 'utf8')
  check(
    checkReleaseApproval(slotDir).ok === false,
    'a slot-root file the curated list does not name still voids the approval'
  )
  rmSync(unrelatedProbe, { force: true })
  check(
    checkReleaseApproval(slotDir).ok === true,
    'removing that probe restores the approval, so the previous check compared the file, not the run'
  )

  // The criterion and the exclusion list are one decision in two views, and until 2026-09-29 they were
  // two: an absolute sentence — "an entry may be excluded only if it cannot alter the built artifact or
  // its signature" — sat above `keystore.properties` and `local.properties`, which choose the key the
  // APK is signed with and the SDK it is compiled against. A prose criterion next to a list that
  // violates it is worse than no criterion, so the criterion is now the closed set
  // `EXCLUSION_REASONS`, every excluded name carries one of them, and this section fails on the three
  // ways they can come apart again: a name whose reason is not in the set, a reason no name uses, and a
  // criterion that does not name every reason. The two machine-config entries are asserted by name,
  // because they are the ones the old sentence contradicted.
  const {
    EXCLUSION_CRITERION,
    EXCLUSION_REASONS,
    EXCLUDED_FILE_REASONS,
    exclusionReason
  } = await import(pathToFileURL(join(ROOT, 'scripts', 'lib', 'release-approval.mjs')).href)

  const allowedReasons = new Set(Object.keys(EXCLUSION_REASONS))
  const unlistedReasons = [...EXCLUDED_FILE_REASONS]
    .filter(([, reason]) => !allowedReasons.has(reason))
    .map(([name, reason]) => `${name}: ${reason}`)
  check(
    unlistedReasons.length === 0,
    `every excluded name is excluded for a reason the criterion states (unlisted: ${unlistedReasons.join(', ') || 'none'})`
  )

  const usedReasons = new Set(EXCLUDED_FILE_REASONS.values())
  const unusedReasons = [...allowedReasons].filter((reason) => !usedReasons.has(reason))
  check(
    unusedReasons.length === 0,
    `every reason the criterion states is used by a name in the list, so the criterion carries no clause nothing satisfies (unused: ${unusedReasons.join(', ') || 'none'})`
  )

  const unnamedReasons = [...allowedReasons].filter((reason) => !EXCLUSION_CRITERION.includes(reason))
  check(
    unnamedReasons.length === 0,
    `the criterion names every reason the list may use, so the sentence a reader applies is not narrower than the list (unnamed: ${unnamedReasons.join(', ') || 'none'})`
  )

  for (const machineConfig of ['keystore.properties', 'local.properties', '.env']) {
    check(
      EXCLUDED_FILE_REASONS.get(machineConfig) === 'machine-local' &&
        exclusionReason(machineConfig) === 'machine-local',
      `${machineConfig} is excluded as the machine's configuration, which is a reason the criterion admits — the two the old sentence contradicted`
    )
  }
  check(
    exclusionReason('.env.local') === exclusionReason('.env') &&
      exclusionReason('.env.production') === exclusionReason('.env'),
    'an .env variant is excluded for the same reason as .env, so the prefix rule is not a second list with its own criterion'
  )
  check(
    exclusionReason('app/build.gradle.kts') === undefined,
    'an ordinary build input has no exclusion reason, so the tables above are not a blanket exemption'
  )

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
  // 10. Store-pack contracts: per-kind screenshot limits, the console's aspect rule, and idea
  // provenance.
  //
  // Three store-kit behaviours regressed silently before this section existed:
  //
  //  * `SCREENSHOT_LIMITS` split the screenshot gate per asset kind, and the two tablet fields do
  //    NOT share a window: the console's own 7-inch field accepts 320..3840 while its 10-inch
  //    field starts at 1080. A single blanket rule got one of the two wrong, and the old tablet
  //    rule used the 10-inch floor for both.
  //  * `SCREENSHOT_ASPECT_RATIOS` is the console's own sentence - "Соотношение сторон изображения
  //    должно составлять 16:9 или 9:16" - stated in every screenshot field. Without it, a
  //    `max_ratio: None` kind was checked for size only and the retired 5:8 tablet canvases
  //    (1200x1920, 1600x2560) passed a gate Play's own prose refuses.
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

  /**
   * Re-record the sandbox slot's fixture approval before a render, because rendering is now gated.
   *
   * `store-kit.py --render-only` refuses without an approval record — the boundary working, and the
   * refusal is asserted in the approval section above. These contract checks then rewrite
   * `store/listing.json`, `capabilities.json` and the generated documents between renders, and both
   * digests cover those files, so an approval granted once at the top of the section lapses by design
   * on the first edit and every later render is refused. Approving before each render keeps the
   * boundary visible inside these tests instead of bypassed: a render with no approval call above it is
   * a red test, which is why the failure this fixed was worth chasing rather than silencing.
   */
  function approveFixture() {
    const result = spawnSync(
      process.execPath,
      [
        join(ROOT, 'scripts', 'approve-release.mjs'),
        '--slot-dir', slotDir,
        '--note', 'selftest fixture: approved inside a temporary directory to exercise the render paths'
      ],
      { cwd: ROOT, encoding: 'utf8', windowsHide: true }
    )
    if (result.status !== 0) {
      throw new Error(
        `selftest fixture approval failed: ${`${result.stdout || ''}${result.stderr || ''}`.trim()}`
      )
    }
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
  approveFixture()
  const contractRender = runStoreKit(['--render-only'])
  check(
    contractRender.status === 0,
    'store-kit --render-only renders a complete pack for the contract checks'
  )

  // (1) Baseline: a generated pack is valid.
  const contractBaseline = runStoreKit(['--validate-only'])
  check(contractBaseline.status === 0, 'a generated store pack passes --validate-only with exit 0')

  // (2)-(7) Per-kind screenshot rule, through the CLI.
  //
  // `validate_store` also enforces the exact canonical size from SCREEN_TARGETS (1080x1920 for
  // phone and 7-inch, 1440x2560 for 10-inch), so a size-mutated asset always exits non-zero for
  // that unrelated reason. Without weakening that guard there is no CLI route to exit 0 for a
  // mutated asset, so these checks assert *which* rule the failure names.
  const tabletAsset = join(contractAssets, 'tablet-7-01.png')
  const tenInchAsset = join(contractAssets, 'tablet-10-01.png')
  const phoneAsset = join(contractAssets, 'phone-01.png')
  const tabletOriginal = readFileSync(tabletAsset)
  const tenInchOriginal = readFileSync(tenInchAsset)
  const phoneOriginal = readFileSync(phoneAsset)

  // The retired factory canvas. 1200x1920 is 5:8 - neither 16:9 nor 9:16 - and every console
  // screenshot field states the aspect rule, so it must fail on the ratio and not on the
  // phone-only 2x cap.
  const tabletFiveEightWritten = writePngFill(tabletAsset, 1200, 1920)
  const tabletFiveEight = runStoreKit(['--validate-only'])
  const tabletFiveEightFailure = assetFailure(tabletFiveEight.output, 'tablet-7-01.png')
  check(
    tabletFiveEightWritten &&
      tabletFiveEight.status !== 0 &&
      tabletFiveEightFailure.includes('is not 16:9 or 9:16') &&
      !tabletFiveEightFailure.includes('more than 2x'),
    'the retired 1200x1920 tablet canvas (5:8) fails the console aspect rule, not the phone 2x cap ' +
      `-> ${tabletFiveEightFailure.trim()}`
  )
  writeFileSync(tabletAsset, tabletOriginal)

  // The two tablet windows differ, exactly as the console states them: the 7-inch field accepts
  // 320..3840 while the 10-inch field starts at 1080. A 720x1280 canvas is exactly 9:16, so the
  // only rule it can fail is the 10-inch short-side floor.
  const tenInchShortWritten = writePngFill(tenInchAsset, 720, 1280)
  const tenInchShort = runStoreKit(['--validate-only'])
  const tenInchShortFailure = assetFailure(tenInchShort.output, 'tablet-10-01.png')
  check(
    tenInchShortWritten &&
      tenInchShort.status !== 0 &&
      tenInchShortFailure.includes('short side 720px is below the 1080px minimum') &&
      !tenInchShortFailure.includes('is not 16:9 or 9:16') &&
      !tenInchShortFailure.includes('more than 2x'),
    'a 9:16 720x1280 asset fails only the 10-inch 1080px floor, which the 7-inch field does not have'
  )
  writeFileSync(tenInchAsset, tenInchOriginal)

  const tabletLongWritten = writePngFill(tabletAsset, 1200, 4000)
  const tabletLong = runStoreKit(['--validate-only'])
  const tabletLongFailure = assetFailure(tabletLong.output, 'tablet-7-01.png')
  check(
    tabletLongWritten &&
      tabletLong.status !== 0 &&
      tabletLongFailure.includes('long side 4000px exceeds the 3840px maximum'),
    'tablet 1200x4000 is rejected for exceeding the 7-inch 3840px long-side cap'
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

  // (8) The declared inventory, not the pixels: every canvas the factory renders must satisfy the
  // console's aspect sentence and its own field window, and the per-kind window must be the
  // console's. This is the check that fails when `SCREEN_TARGETS` is changed back to the old 5:8
  // tablets, because it reads the constants a render would use.
  const targetScript = [
    'import importlib.util, json, sys',
    'sys.dont_write_bytecode = True',
    "spec = importlib.util.spec_from_file_location('store_kit_contract', sys.argv[1])",
    'module = importlib.util.module_from_spec(spec)',
    'spec.loader.exec_module(module)',
    'print(json.dumps({',
    '    "targets": {k: {"count": c, "size": list(s)} for k, (c, s) in module.SCREEN_TARGETS.items()},',
    '    "limits": module.SCREENSHOT_LIMITS,',
    '    "aspects": [list(pair) for pair in module.SCREENSHOT_ASPECT_RATIOS],',
    '}))'
  ].join('\n')
  const consoleWindows = { phone: [320, 3840], 'tablet-7': [320, 3840], 'tablet-10': [1080, 7680] }
  const targetRun = spawnSync('python', ['-B', '-c', targetScript, storeKitScript], {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  })
  let declared = null
  try {
    declared = JSON.parse(targetRun.stdout || '')
  } catch {
    declared = null
  }
  check(
    declared !== null && Object.keys(declared.targets).length === 3,
    'the screenshot inventory is readable from store-kit.py'
  )
  if (declared) {
    for (const [kind, window] of Object.entries(consoleWindows)) {
      const target = declared.targets[kind]
      check(Boolean(target), `SCREEN_TARGETS declares the ${kind} kind`)
      if (!target) continue
      const [width, height] = target.size
      const ratioOk = declared.aspects.some(
        ([across, down]) => width * down === height * across || width * across === height * down
      )
      check(
        ratioOk,
        `${kind} target ${width}x${height} matches the console's 16:9/9:16 screenshot aspect rule`
      )
      check(
        Math.min(width, height) >= window[0] && Math.max(width, height) <= window[1],
        `${kind} target ${width}x${height} sits inside the console's own ${window[0]}..${window[1]} field window`
      )
      check(
        declared.limits[kind].min_short === window[0] && declared.limits[kind].max_long === window[1],
        `${kind} SCREENSHOT_LIMITS window is the console's ${window[0]}..${window[1]}, not another field's`
      )
    }
  }

  // (8b) One source of truth, imported - never restated. `store-studio.py` carried a *second* copy
  // of `SCREEN_TARGETS` until 2026-09-29, which is the defect `.cursor/rules/
  // verification-discipline.mdc` names: the two copies were free to drift, and a studio render would
  // have produced the 5:8 canvas `store-kit --validate-only` now refuses. The check reads the
  // factory's own modules, so re-adding the literal fails here rather than at capture time.
  const contractLiteral = /^\s*(SCREEN_TARGETS|SCREENSHOT_LIMITS)\s*(?::[^=\n]*)?=\s*[{[]/m
  const contractOwners = walkFiles(join(ROOT, 'scripts'))
    .filter((file) => /\.(py|mjs|cjs|js)$/.test(file))
    .filter((file) => contractLiteral.test(readFileSync(file, 'utf8')))
    .map((file) => relative(ROOT, file).split('\\').join('/'))
  check(
    contractOwners.length === 1 && contractOwners[0] === 'scripts/store-kit.py',
    'the screenshot inventory is defined in exactly one module, and it is store-kit.py ' +
      `(found: ${contractOwners.join(', ') || 'none'})`
  )
  check(
    readFileSync(join(ROOT, 'scripts', 'store-studio.py'), 'utf8').includes(
      '_STORE_KIT.SCREEN_TARGETS'
    ),
    'store-studio.py imports SCREEN_TARGETS from store-kit.py instead of restating it'
  )

  // (8c) The aspect rule exists twice in the factory out of necessity - once in the Python module
  // that renders and validates the pack, once in the JS library the store tools share - and a JS
  // module cannot import a Python constant. Two copies that must agree is exactly what a check is
  // for: this one fails the moment `scripts/lib/play-assets.mjs` and `scripts/store-kit.py`
  // disagree about the sentence the console prints in every screenshot field.
  if (declared && declared.aspects) {
    const { SCREENSHOT_ASPECT_RATIOS: jsAspects } = await import(
      pathToFileURL(join(ROOT, 'scripts', 'lib', 'play-assets.mjs')).href
    )
    const jsPairs = jsAspects.map((pair) => [...pair])
    check(
      JSON.stringify(jsPairs) === JSON.stringify(declared.aspects),
      'scripts/lib/play-assets.mjs restates the aspect rule and agrees with store-kit.py ' +
        `(got ${JSON.stringify(jsPairs)} against ${JSON.stringify(declared.aspects)})`
    )
  }

  // (9) The rule as a pure verdict: `screenshot_rule_failures` is the function `validate_store`
  // calls for every screenshot, so importing it directly shows which sentence each size trips.
  // Both tablet kinds have `max_ratio: None`, and every case below proves the aspect rule still
  // decides them.
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
    ['tablet-7', 1200, 1920],  // the retired 5:8 factory canvas
    ['tablet-7', 320, 800],    // inside 320..3840 and under the 2x cap, still not 16:9 or 9:16
    ['tablet-10', 720, 1280],  // exactly 9:16, below the 10-inch 1080px floor
    ['tablet-7', 720, 1280],   // exactly 9:16 and inside the 7-inch window -> accepted
    ['phone', 1080, 3000],     // the phone-only 2x cap
    ['phone', 320, 800],       // the phone-only 2x cap
    ['tablet-7', 1200, 4000],  // the 7-inch long-side cap
    ['tablet-10', 1440, 2560]  // the shipped 10-inch canvas -> accepted
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
      ruleVerdicts[0].some((failure) => failure.includes('is not 16:9 or 9:16')) &&
        !ruleVerdicts[0].some((failure) => failure.includes('more than 2x')),
      'the rule rejects the retired 1200x1920 tablet canvas for its 5:8 ratio, not for a 2x cap'
    )
    check(
      ruleVerdicts[1].some((failure) => failure.includes('is not 16:9 or 9:16')),
      'the aspect rule decides a tablet size that the 320..3840 window and the 2x cap both allow'
    )
    check(
      ruleVerdicts[2].some((failure) => failure.includes('below the 1080px minimum')) &&
        !ruleVerdicts[2].some((failure) => failure.includes('is not 16:9 or 9:16')),
      'the 10-inch rule rejects a 9:16 720x1280 for the 1080px short side only'
    )
    check(
      ruleVerdicts[3].length === 0,
      'the 7-inch rule accepts the same 9:16 720x1280 that the 10-inch rule rejects'
    )
    check(
      ruleVerdicts[4].some((failure) => failure.includes('more than 2x')),
      'the per-kind rule rejects a 1080x3000 phone for the 2x cap that tablets are exempt from'
    )
    check(
      ruleVerdicts[5].some((failure) => failure.includes('more than 2x')),
      'the per-kind rule rejects a 320x800 phone for the 2x cap'
    )
    check(
      ruleVerdicts[6].some((failure) => failure.includes('exceeds the 3840px maximum')),
      'the per-kind rule rejects a 1200x4000 tablet for the 3840px long-side cap'
    )
    check(
      ruleVerdicts[7].length === 0,
      'the per-kind rule accepts the shipped 1440x2560 10-inch canvas'
    )
  }

  // (7) Idea refresh replaces scaffold boilerplate with the recorded idea.
  const recordedIdea = 'A tide-chart notebook that records harbour water levels every morning.'
  setRecordedIdea(recordedIdea)
  approveFixture()
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
  approveFixture()
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

  // (10) Hand-written policy documents survive a regeneration, and marked ones do not.
  //
  // `privacy.md` and `data-safety.md` are the two store documents a human must write
  // app-specifically: the generator cannot read the merged manifest, so it cannot know which SDK
  // contributed a permission, and it does not. It used to overwrite them unconditionally while
  // `verify-slot` passed the resulting stub, so a hand-written policy was silently destroyed and
  // the pack still reported OK. Generated content now carries a marker comment; content without it
  // is a human edit and must be kept. The second half of the check matters as much as the first:
  // if a marked file were also preserved, "delete this line to keep your own wording" would be a
  // one-way instruction and the generator could never update a policy again.
  const handWrittenPolicy = '# Privacy policy — hand-written\n\nNothing is collected.\n'
  const handWrittenSafety = '# Data safety — hand-written\n\n- Does the app collect data? **No**\n'
  writeFileSync(join(contractStoreDir, 'privacy.md'), handWrittenPolicy, 'utf8')
  writeFileSync(join(contractStoreDir, 'data-safety.md'), handWrittenSafety, 'utf8')
  approveFixture()
  const policyRefresh = runStoreKit(['--render-only'])
  check(
    policyRefresh.status === 0 &&
      policyRefresh.output.includes('PRESERVE privacy.md') &&
      policyRefresh.output.includes('PRESERVE data-safety.md'),
    'a regeneration reports that it preserved hand-written privacy.md and data-safety.md'
  )
  check(
    readFileSync(join(contractStoreDir, 'privacy.md'), 'utf8') === handWrittenPolicy &&
      readFileSync(join(contractStoreDir, 'data-safety.md'), 'utf8') === handWrittenSafety,
    'hand-written privacy.md and data-safety.md survive a regeneration byte for byte'
  )

  const markedPolicy = '# Privacy policy — regenerated\n\n<!-- kiln:generated policy - delete this line to keep your own wording -->\n'
  writeFileSync(join(contractStoreDir, 'privacy.md'), markedPolicy, 'utf8')
  approveFixture()
  const markedRefresh = runStoreKit(['--render-only'])
  check(
    markedRefresh.status === 0 &&
      readFileSync(join(contractStoreDir, 'privacy.md'), 'utf8') !== markedPolicy,
    'a privacy.md that still carries the generator marker is regenerated, not preserved'
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
    ['kiln-status.mjs', 'inventories scaffold wrapper paths with existsSync; it never launches a process'],
    [
      'lib/lessons.mjs',
      'parses lesson text, so `gradlew` is one of the program names it must recognise when a lesson cites its sensor; it imports no child_process and launches nothing'
    ]
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

    // A green Gradle exit is not an executed suite. `strings.xml` is read off disk by
    // `HomeViewModelTest`, so a copy-only change leaves the task UP-TO-DATE and the XML
    // under build/test-results/ is the *previous* run's output. Reporting that as PASS is
    // a check that cannot fail; the runner must refuse to call it a PASS.
    const reportXml = (tests) =>
      `<?xml version="1.0" encoding="UTF-8"?>\n<testsuite name="fixture" tests="${tests}" failures="0" errors="0" skipped="0"></testsuite>\n`
    const staleDir = join(fakeSlot, 'app', 'build', 'test-results', 'testDebugUnitTest')
    mkdirSync(staleDir, { recursive: true })
    const staleReport = join(staleDir, 'TEST-fixture.xml')
    writeFileSync(staleReport, reportXml(125), 'utf8')
    const hourAgo = new Date(Date.now() - 3600_000)
    utimesSync(staleReport, hourAgo, hourAgo)

    const upToDateLines = []
    const upToDate = runSlotUnitTests('runner-fixture', {
      appsDir: fakeApps,
      report: (message) => upToDateLines.push(message),
      runGradle: () => ({
        result: { status: 0, stdout: '> Task :app:testDebugUnitTest UP-TO-DATE', stderr: '' },
        elapsedMs: 1
      })
    })
    check(
      !upToDate.ok &&
        upToDate.exitCode === 2 &&
        upToDateLines.some((message) => message.startsWith('STALE testDebugUnitTest')) &&
        upToDateLines.some((message) => message.includes('--rerun')) &&
        !upToDateLines.some((message) => /^PASS\s+testDebugUnitTest/.test(message)),
      'an UP-TO-DATE test task is reported STALE with exit 2, never PASS on a previous run\u2019s report'
    )

    // The backstop, for when Gradle's console does not name the task at all: reports that
    // already existed and were not rewritten in this run cannot describe this run.
    const silentLines = []
    const silent = runSlotUnitTests('runner-fixture', {
      appsDir: fakeApps,
      report: (message) => silentLines.push(message),
      runGradle: () => ({ result: { status: 0, stdout: '', stderr: '' }, elapsedMs: 1 })
    })
    check(
      !silent.ok &&
        silent.exitCode === 2 &&
        silentLines.some((message) => message.startsWith('STALE testDebugUnitTest')),
      'an unnamed test task over untouched pre-existing reports is reported STALE'
    )

    // A sensor that can only fail is not a sensor: a task that really ran and rewrote its
    // report must still be a PASS, or the staleness guard would reject every honest run.
    const freshLines = []
    const fresh = runSlotUnitTests('runner-fixture', {
      appsDir: fakeApps,
      report: (message) => freshLines.push(message),
      runGradle: () => {
        writeFileSync(staleReport, reportXml(126), 'utf8')
        return {
          result: { status: 0, stdout: '> Task :app:testDebugUnitTest', stderr: '' },
          elapsedMs: 1
        }
      }
    })
    check(
      fresh.ok &&
        fresh.exitCode === 0 &&
        freshLines.some((message) => /^PASS testDebugUnitTest — 126 tests/.test(message)),
      'a test task that executed and rewrote its report is still reported PASS'
    )

    const rerunCalls = []
    runSlotUnitTests('runner-fixture', {
      appsDir: fakeApps,
      force: true,
      report: () => {},
      runGradle: (directory, tasks) => {
        rerunCalls.push({ directory, tasks })
        return {
          result: { status: 0, stdout: '> Task :app:testDebugUnitTest', stderr: '' },
          elapsedMs: 1
        }
      }
    })
    check(
      rerunCalls.length === 1 &&
        rerunCalls[0].tasks.includes('testDebugUnitTest') &&
        rerunCalls[0].tasks.includes('--rerun'),
      'forcing the runner adds Gradle --rerun to the one invocation'
    )
  } else {
    check(false, 'the runner can report no tests found distinctly from PASS')
    check(false, 'a failed slot unit-test task returns a non-zero exit')
    check(false, 'an UP-TO-DATE test task is reported STALE with exit 2')
    check(false, 'an unnamed test task over untouched reports is reported STALE')
    check(false, 'a test task that executed and rewrote its report is still reported PASS')
    check(false, 'forcing the runner adds Gradle --rerun to the one invocation')
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
// 4c. The capability pairing is the teeth: an archetype that names a way to earn is
// not credible while the capability that collects the money is disabled.
//
// This is a pure check of the shared module, so it needs no sandbox. The expected
// sentence is written out literally rather than computed by the function under test,
// because a check that derives its expectation from the code it checks can never
// disagree with it.
// --------------------------------------------------------------------------

section('economics capability pairing')

const { parseEconomics, validateEconomicsAgainstCapabilities } = await import(
  pathToFileURL(join(ROOT, 'scripts', 'lib', 'slot-economics.mjs')).href
)

const adsLedPlan = { ...validEconomics(), archetype: 'ads_led_utility', priceUsd: 0 }
const missingAds = validateEconomicsAgainstCapabilities(adsLedPlan, { schemaVersion: 1, enabled: [] })
check(missingAds.ok === false, 'ads_led_utility with no enabled capability reports an error')
// Optional chaining keeps a regression (ok: true, no errors) a clean FAIL instead of
// throwing and aborting every check after it.
check(
  missingAds.errors?.includes(
    'archetype "ads_led_utility" requires the "ads" capability; capabilities.json enables []; add "ads" to capabilities.json or choose another archetype'
  ),
  'the pairing error names the archetype, the missing capability, and capabilities.json'
)
const withAds = validateEconomicsAgainstCapabilities(adsLedPlan, { schemaVersion: 1, enabled: ['ads'] })
check(withAds.ok === true, 'ads_led_utility with the ads capability passes')
const withExtras = validateEconomicsAgainstCapabilities(adsLedPlan, {
  schemaVersion: 1,
  enabled: ['ads', 'billing']
})
check(withExtras.ok === true, 'extra enabled capabilities do not fail the pairing')

// --------------------------------------------------------------------------
// 4d. The evidenceRef may point at measured research or at the ideation proposal a
// plan came from, but not at an arbitrary directory.
//
// All five plans in `docs/ideation/2026-09-26-five-recipes.md` originally cited only
// the *method* record `docs/research/2026-09-24-market-decision.md`, because the
// pattern accepted `docs/research/` alone. A plan therefore pointed at a file
// describing how the market was measured rather than what was measured for its own
// niche. Both roots are legitimate; any other root is not. Deleting the cited file is
// caught separately: `check-economics.mjs` resolves the reference on disk and BLOCKS
// when it is gone, so the two checks have to stay in different places on purpose.
// --------------------------------------------------------------------------

section('economics evidenceRef shape')

check(parseEconomics(validEconomics()).ok === true, 'a docs/research evidenceRef still validates')

check(
  parseEconomics(validEconomics({ evidenceRef: 'docs/ideation/2026-09-26-five-recipes.md#31-recipe-1' })).ok ===
    true,
  'a docs/ideation evidenceRef validates'
)

// The rejection cases are the reason this test exists: widening the accepted roots
// must not have widened them to everything, and a bare directory is not a file.
for (const rejected of ['docs/anything/else.md#1', 'docs/ideation#1', 'docs/ideation/', 'notes.md']) {
  check(
    parseEconomics(validEconomics({ evidenceRef: rejected })).ok === false,
    `evidenceRef "${rejected}" is refused`
  )
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

// `scripts/visual-qa.py` measures the shipped pixels for `.cursor/rules/visual-identity.mdc`, and
// both of its reads of a slot were written against the first slot: a literal
// `app/src/main/java/com/gexetr/pipesizing/ui/theme` path, and a palette regex that only matched
// `internal val`. The failure was silent in the worst way - the *palette* section of a later slot
// died with a traceback before it printed a single measurement, so the verification the rule names
// could not be run for any slot but the one it was written for, and nothing said so. The check
// below drives the regression test in `scripts/test-visual-qa-discovery.py`, which builds a
// synthetic slot whose package and colour declarations are deliberately unlike the reference
// slot's and is expected to FAIL against the previous implementation.
section('visual-qa measures the slot it is given')
const visualQaProbe = spawnSync('python', [join(ROOT, 'scripts', 'test-visual-qa-discovery.py')], {
  cwd: ROOT,
  encoding: 'utf8',
  windowsHide: true
})
const visualQaRows = `${visualQaProbe.stdout || ''}${visualQaProbe.stderr || ''}`
  .split(/\r?\n/)
  .filter((row) => /^(PASS|FAIL)\s/.test(row))
if (visualQaRows.length === 0) {
  check(
    false,
    `visual-qa discovery test reported no verdict (exit ${visualQaProbe.status ?? 'unknown'})`
  )
} else {
  for (const row of visualQaRows) {
    const [status, ...rest] = row.split(/\s+/)
    line(status === 'PASS' ? 'PASS' : 'FAIL', `visual-qa: ${rest.join(' ').trim()}`)
  }
}
check(visualQaProbe.status === 0, 'visual-qa discovery test exits 0')

// `.cursor/rules/visual-identity.mdc` requires a drawn icon that reads at 48dp, and the icon the
// owner rejected satisfied every gate the factory had, because none of them measured the mark:
// `store-studio.py` composited a resampled gradient, two radial blooms, a 32px drafting grid at
// alpha 12 and a glyph placed by hand-tuned pixel constants, and nothing could say so. The check
// below drives `scripts/test-icon-design.py`, which measures the rendered pixels — stroke, counter,
// optical offset, field texture, mask survival, 48dp contrast, determinism — and drives every one of
// those sensors to failure with a mark built to break exactly it, so a sensor that stops working
// fails here rather than passing quietly.
//
// No `--legacy-rev`: that flag loads `scripts/store-studio.py` from a git revision, which is the
// pre-change renderer only until this rewrite is committed, and a check pinned to `HEAD` would invert
// itself the moment it landed. The suite's in-process reproduction of the old field composition runs
// unconditionally instead, which is why it was written to not depend on git history.
section('an icon is measured, not eyeballed')
const iconDesignProbe = spawnSync('python', [join(ROOT, 'scripts', 'test-icon-design.py')], {
  cwd: ROOT,
  encoding: 'utf8',
  windowsHide: true
})
const iconDesignRows = `${iconDesignProbe.stdout || ''}${iconDesignProbe.stderr || ''}`
  .split(/\r?\n/)
  .filter((row) => /^\s*(PASS|FAIL)\s/.test(row))
  .map((row) => row.trim())
if (iconDesignRows.length === 0) {
  check(
    false,
    `icon design test reported no verdict (exit ${iconDesignProbe.status ?? 'unknown'})`
  )
} else {
  for (const row of iconDesignRows) {
    const [status, ...rest] = row.split(/\s+/)
    line(status === 'PASS' ? 'PASS' : 'FAIL', `icon design: ${rest.join(' ').trim()}`)
  }
}
check(iconDesignProbe.status === 0, 'icon design test exits 0')

// Play's release screen asks for a release name and release notes, and the pack carried neither, so
// the first real upload reached that screen with no prepared text and no way to count it — and Play
// enforces its 500-character cap by truncating the paste, not by refusing it. The check below drives
// `scripts/test-release-notes.py`, which generates a real pack and then mutates it to prove every
// rule can fail; the half that matters is the generator refusing to overwrite a written note, since a
// regenerated pack replacing real release text with a prompt is worse than having no file at all.
section('the release screen has its two fields prepared')
const releaseNotesProbe = spawnSync('python', [join(ROOT, 'scripts', 'test-release-notes.py')], {
  cwd: ROOT,
  encoding: 'utf8',
  windowsHide: true
})
const releaseNotesRows = `${releaseNotesProbe.stdout || ''}${releaseNotesProbe.stderr || ''}`
  .split(/\r?\n/)
  .filter((row) => /^(PASS|FAIL)\s/.test(row))
if (releaseNotesRows.length === 0) {
  check(false, `release-notes test reported no verdict (exit ${releaseNotesProbe.status ?? 'unknown'})`)
} else {
  for (const row of releaseNotesRows) {
    const [status, ...rest] = row.split(/\s+/)
    line(status === 'PASS' ? 'PASS' : 'FAIL', `release-notes: ${rest.join(' ').trim()}`)
  }
}
check(releaseNotesProbe.status === 0, 'release-notes test exits 0')

// A billing app's checklist named screenshots, declarations, signing and the first AAB upload, and
// never named the three things that actually block a first launch: the one-time product, its
// purchase option, and the tester's license-testing entry. All three are Play *console* state, so no
// pack document described them and no gate could see them - the owner found them by uploading a
// bundle, rolling out nothing, getting "not found" from the opt-in link, and nearly testing a
// purchase against a product that did not exist. The check below drives
// `scripts/test-checklist-products.py`, which is expected to FAIL against the old behaviour (it
// does: 8 of its 19 legs fail with the billing gate removed), and whose second half matters just as
// much - a non-billing slot must be told nothing about products it does not sell.
section('a billing app is told to create and activate what it sells')
const checklistProductsProbe = spawnSync(
  'python',
  [join(ROOT, 'scripts', 'test-checklist-products.py')],
  {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  }
)
const checklistProductsRows = `${checklistProductsProbe.stdout || ''}${checklistProductsProbe.stderr || ''}`
  .split(/\r?\n/)
  .filter((row) => /^(PASS|FAIL)\s/.test(row))
if (checklistProductsRows.length === 0) {
  check(
    false,
    `checklist-products test reported no verdict (exit ${checklistProductsProbe.status ?? 'unknown'})`
  )
} else {
  for (const row of checklistProductsRows) {
    const [status, ...rest] = row.split(/\s+/)
    line(status === 'PASS' ? 'PASS' : 'FAIL', `checklist-products: ${rest.join(' ').trim()}`)
  }
}
check(checklistProductsProbe.status === 0, 'checklist-products test exits 0')

// The owner performed the first Play Console upload of a real slot **by hand**, answering
// page-by-page questions in chat: what to put in the privacy-policy URL field, yes/no for each of
// the ten App-content declarations, what the credentials dialog wants, why «Create release» asked
// for the bundle a second time. Every question was a stall, and none of the answers lived in the
// generated pack, so the next slot would stall in exactly the same places. The check below drives
// `scripts/test-console-answers.py`, which generates a real pack and then mutates it to prove every
// rule can fail. The follow-up facts the owner walked out of the product wizard are covered by the
// same suite: with the product-form literals not emitted at all, 27 of its legs fail; with the price
// rendered the way `economics.json` stores it - dot-decimal, which the console's price field rejects -
// 5 legs fail; and disabling the validation hookup still fails the seven drift legs. All four
// observations are recorded in `harness/lessons.md` for 2026-09-29. The completion statement is
// measured the same way, because it exists to stop one specific misreading - the owner read the
// wizard's `Шаг N из M` strip as a completion counter and took a saved declaration for an unfinished
// one: with the warning worded as "a guide, not a progress indicator" instead of "not a completion
// counter" 1 of 250 legs fails; with a branch's `skippedSteps` emptied 4 fail; with the note not
// emitted for the branch 1 fails; and with the note's `{steps}` placeholder removed the generator
// refuses outright rather than printing a sentence that names no step.
section('the first console upload has its answers prepared')
const consoleAnswersProbe = spawnSync('python', [join(ROOT, 'scripts', 'test-console-answers.py')], {
  cwd: ROOT,
  encoding: 'utf8',
  windowsHide: true
})
const consoleAnswersRows = `${consoleAnswersProbe.stdout || ''}${consoleAnswersProbe.stderr || ''}`
  .split(/\r?\n/)
  .filter((row) => /^(PASS|FAIL)\s/.test(row))
if (consoleAnswersRows.length === 0) {
  check(
    false,
    `console-answers test reported no verdict (exit ${consoleAnswersProbe.status ?? 'unknown'})`
  )
} else {
  for (const row of consoleAnswersRows) {
    const [status, ...rest] = row.split(/\s+/)
    line(status === 'PASS' ? 'PASS' : 'FAIL', `console-answers: ${rest.join(' ').trim()}`)
  }
}
check(consoleAnswersProbe.status === 0, 'console-answers test exits 0')

// The walkthrough and the sheet answer the same console form to two different readers - the owner
// in Russian, the paste sheet in English - so both are rendered from `scripts/lib/console-form.json`
// and neither may be hand-edited. A fact stated in two places is the drift this pair exists to
// prevent: the check below regenerates the document from the model and compares it byte for byte, so
// editing the document instead of the model fails. The commands it is expected to answer with are
// recorded in `harness/lessons.md` for 2026-09-29: a single inserted line fails the check with the
// first differing line named, and re-rendering restores it. The same check is what catches a
// *renderer* that stops printing part of the document - with `completionSection()` no longer pushed -
// it named `line 13: model="## 1. Состояние приложения ..." file="## Как читать, что декларация
// заполнена"` and exited 1, while the sheet suite stayed green because the document on disk was
// untouched. The pair is deliberate: the check owns the model-to-document direction, the suite's own
// legs own a document that was edited by hand.
section('the console walkthrough is the model, not a copy of it')
const walkthroughProbe = spawnSync(
  process.execPath,
  [join(ROOT, 'scripts', 'console-walkthrough.mjs'), '--check'],
  { cwd: ROOT, encoding: 'utf8', windowsHide: true }
)
const walkthroughRows = `${walkthroughProbe.stdout || ''}${walkthroughProbe.stderr || ''}`
  .split(/\r?\n/)
  .filter((row) => /^(PASS|FAIL)\s/.test(row))
if (walkthroughRows.length === 0) {
  check(false, `walkthrough check reported no verdict (exit ${walkthroughProbe.status ?? 'unknown'})`)
} else {
  for (const row of walkthroughRows) {
    const [status, ...rest] = row.split(/\s+/)
    line(status === 'PASS' ? 'PASS' : 'FAIL', `walkthrough: ${rest.join(' ').trim()}`)
  }
}
check(walkthroughProbe.status === 0, 'walkthrough check exits 0')

// The audience is the slot's decision and the walkthrough has no slot, so the guide prints the model's
// own sentence for a slot that has declared none — filled in, never a branch. The check above only
// proves the document agrees with the model it was rendered *from*, so a renderer that stopped printing
// the sentence would still agree with a document regenerated without it. This leg reads the rendered
// document itself, which is what makes it fail in that case; the sheet's half of the same sentence is
// asserted in `scripts/test-console-answers.py`.
const walkthroughDoc = readFileSync(join(ROOT, 'docs', 'play-console-walkthrough.md'), 'utf8')
const consoleForm = JSON.parse(readFileSync(join(ROOT, 'scripts', 'lib', 'console-form.json'), 'utf8'))
const refusalTemplate = String(consoleForm.targetAudience?.emptyChoiceRefusalRu || '')
const audienceField = String(
  (consoleForm.screens || []).find((screen) => screen.id === 'declaration-target-audience')?.slotKey || ''
)
const audienceBranches = Object.keys(consoleForm.targetAudience?.audiences || {}).join(', ')
const filledRefusal = refusalTemplate
  .replaceAll('{branches}', audienceBranches)
  .replaceAll('{field}', audienceField)
check(
  refusalTemplate.includes('{field}') && refusalTemplate.includes('{branches}') && audienceField.length > 0,
  'the model states the no-audience sentence with the field and the branches as placeholders, and the screen names the field they are filled from'
)
check(
  walkthroughDoc.split(/\r?\n/).includes(`> ${filledRefusal}`),
  'the guide prints that sentence filled in, so a slot that declared no audience is told so here as well as in its sheet'
)
const quotedTodos = walkthroughDoc
  .split(/\r?\n/)
  .filter((line) => line.startsWith('> TODO-HUMAN'))
check(
  quotedTodos.length > 0 && quotedTodos.every((line) => !/\{[a-zA-Z]+\}/.test(line)),
  'every TODO-HUMAN line the guide quotes is filled in, so the renderer cannot push the model template at a reader'
)

// `about_support_email` shipped as `support@example.com` in the default resources and in eight
// locale directories at once, while `store/listing.json` advertised the developer's real address -
// and `about_app_name` and `common_date_placeholder` were duplicated the same way. `check-locales`
// skipped non-translatable keys, so no gate could see any copy of them; the duplication was found
// only by hand, twice. The check below drives `scripts/test-locale-dedup.mjs`, which is expected to
// FAIL against the previous implementation: two of its four legs are the defect, and the other two
// prove the guard can pass and that it keys off the attribute rather than off "the value is the same
// in two files", which would fire on every legally untranslated string.
section('a non-translatable key is declared once')
const localeDedupProbe = spawnSync(process.execPath, [join(ROOT, 'scripts', 'test-locale-dedup.mjs')], {
  cwd: ROOT,
  encoding: 'utf8',
  windowsHide: true
})
const localeDedupRows = `${localeDedupProbe.stdout || ''}${localeDedupProbe.stderr || ''}`
  .split(/\r?\n/)
  .filter((row) => /^(PASS|FAIL)\s/.test(row))
if (localeDedupRows.length === 0) {
  check(
    false,
    `locale-dedup test reported no verdict (exit ${localeDedupProbe.status ?? 'unknown'})`
  )
} else {
  for (const row of localeDedupRows) {
    const [status, ...rest] = row.split(/\s+/)
    line(status === 'PASS' ? 'PASS' : 'FAIL', `locale-dedup: ${rest.join(' ').trim()}`)
  }
}
check(localeDedupProbe.status === 0, 'locale-dedup test exits 0')

// Play requires the privacy policy in **two** places - the listing's public URL and inside the app -
// and `AGENTS.md` stated that rule while nothing measured it, so `teacher-gradebook` shipped with
// privacy prose and a support e-mail and no way to open the policy at all. The prose was right; the
// sensor did not exist. The suite below drives `scripts/test-in-app-policy.mjs`, whose legs are the
// defects that actually recurred plus the ones the naive implementation gets wrong (a hardcoded URL,
// a URL that no Kotlin references, an opener living in a different file, a URL declared only under
// `values-<locale>/`). Having the sensor is not enough on its own: `check-adaptive-icon.py` also
// existed with nothing invoking it, so this probe is what keeps the sensor's own suite honest, and
// `verify-slot.mjs` runs the sensor itself on every slot.
section('the privacy policy is reachable from inside the app, not only from the listing')
const inAppPolicyProbe = spawnSync(process.execPath, [join(ROOT, 'scripts', 'test-in-app-policy.mjs')], {
  cwd: ROOT,
  encoding: 'utf8',
  windowsHide: true
})
const inAppPolicyRows = `${inAppPolicyProbe.stdout || ''}${inAppPolicyProbe.stderr || ''}`
  .split(/\r?\n/)
  .map((row) => row.trim())
  .filter(Boolean)
const inAppPolicySummary = inAppPolicyRows.find((row) => /^=== \d+ checks/.test(row))
if (!inAppPolicySummary) {
  check(false, `in-app policy suite reported no verdict (exit ${inAppPolicyProbe.status ?? 'unknown'})`)
} else {
  for (const row of inAppPolicyRows.filter((entry) => /^FAIL\s/.test(entry))) {
    line('FAIL', `in-app policy: ${row.replace(/^FAIL\s+/, '')}`)
  }
  check(
    inAppPolicyProbe.status === 0,
    `in-app policy sensor: ${inAppPolicySummary.replace(/^===\s*/, '').replace(/\s*===$/, '')}`
  )
}

// Two holders of one list is the shape that has bitten this factory before (`SCREEN_TARGETS` was
// written once in `store-kit.py` and once in `store-studio.py`, and the copies disagreed while
// nothing failed). The in-app policy sensor needs the reserved-host list in JavaScript, and the
// Python generator needs it to say a pack is unfinished, so neither can import the other: the
// duplication is checked instead of trusted. Moving a host in either file without the other fails
// here rather than silently changing what "a real policy" means.
section('the reserved documentation hosts are one list in two languages')
const storeKitText = readFileSync(join(ROOT, 'scripts', 'store-kit.py'), 'utf8')
const storeKitHosts = storeKitText.match(/^PLACEHOLDER_HOSTS\s*=\s*\(([\s\S]*?)\)/m)?.[1] ?? ''
const storeKitHostList = [...storeKitHosts.matchAll(/"([^"]+)"/g)].map((match) => match[1]).sort()
const sensorHostList = [...PLACEHOLDER_HOSTS].sort()
check(
  storeKitHostList.length > 0,
  `scripts/store-kit.py still declares PLACEHOLDER_HOSTS in the shape this check reads (found ${storeKitHostList.length})`
)
check(
  storeKitHostList.join(',') === sensorHostList.join(','),
  `store-kit.py and in-app-policy.mjs agree on the reserved hosts (store-kit [${storeKitHostList.join(', ')}] vs sensor [${sensorHostList.join(', ')}])`
)

// A watchdog nobody runs is the defect it was built to catch, so `verify-slot.mjs` calls
// `scripts/check-freshness.mjs` on every verification — slot or not. Its own suite is driven here for
// the same reason: the mechanism is only real if an expired window makes the verdict UNVERIFIED and a
// broken ledger cannot read as fresh. The ledger itself is about the factory, not about a slot, and
// it is the one place where "this rule was true on this date" is recorded at all.
section('a measured fact past its re-check window is stale, never fresh')
const freshnessProbe = spawnSync(process.execPath, [join(ROOT, 'scripts', 'test-freshness.mjs')], {
  cwd: ROOT,
  encoding: 'utf8',
  windowsHide: true
})
const freshnessRows = `${freshnessProbe.stdout || ''}${freshnessProbe.stderr || ''}`
  .split(/\r?\n/)
  .map((row) => row.trim())
  .filter(Boolean)
const freshnessSummary = freshnessRows.find((row) => /^\d+\/\d+ checks/.test(row))
if (!freshnessSummary) {
  check(false, `freshness suite reported no verdict (exit ${freshnessProbe.status ?? 'unknown'})`)
} else {
  for (const row of freshnessRows.filter((entry) => /^FAIL\s/.test(entry))) {
    line('FAIL', `freshness: ${row.replace(/^FAIL\s+/, '')}`)
  }
  check(freshnessProbe.status === 0, `freshness watchdog: ${freshnessSummary}`)
}

// A journey asserts the literal text of the nodes it photographs, and those strings come from the
// app's resources, which Android resolves against the *device's* language - not the listing's and
// not the journey's. That dependency was invisible until a slot gained `values-<locale>/`
// directories: the journey that had been green while the app was English-only started reading
// Russian on the Russian A40, and every reading in every shot failed with "not in the field they
// name", which describes a broken journey rather than a device setting. Diagnosing it cost three
// device attempts and about twenty minutes. The check below drives `scripts/test-capture-locale.mjs`,
// which is expected to FAIL against the previous implementation, where no check existed at all, so
// the defect could only ever surface as a screen-reading error.
section('the capture journey runs on a device in the language it reads')
const captureLocaleProbe = spawnSync(process.execPath, [join(ROOT, 'scripts', 'test-capture-locale.mjs')], {
  cwd: ROOT,
  encoding: 'utf8',
  windowsHide: true
})
const captureLocaleRows = `${captureLocaleProbe.stdout || ''}${captureLocaleProbe.stderr || ''}`
  .split(/\r?\n/)
  .filter((row) => /^(PASS|FAIL)\s/.test(row))
if (captureLocaleRows.length === 0) {
  check(
    false,
    `capture-locale test reported no verdict (exit ${captureLocaleProbe.status ?? 'unknown'})`
  )
} else {
  for (const row of captureLocaleRows) {
    const [status, ...rest] = row.split(/\s+/)
    line(status === 'PASS' ? 'PASS' : 'FAIL', `capture-locale: ${rest.join(' ').trim()}`)
  }
}
check(captureLocaleProbe.status === 0, 'capture-locale test exits 0')

// Recon states why an app can win; implementation loses part of that list, and every other
// check in the suite measures form — the APK builds, the pack is complete, the money plan
// parses — so none of them can see a promised feature that quietly did not ship. The legs
// below drive the real gate: one slot whose promises are all accounted for must pass, and
// the rest must fail for the reason they exist. The failing legs matter more than the
// passing one: a gate that cannot fail is not a gate.
section('differentiator contract (sandbox)')

// This section is sandbox-dependent, and the shared sandbox is deleted in a `finally` far above
// this point in the file, so it owns one. Sharing the peer's sandbox would have tied these legs
// to the ordering of a block they do not control.
const diffSandbox = createSandbox()

function promiseBase(id, weight) {
  return {
    id,
    weight,
    promise: 'Returns a result for the cases the incumbents refuse, with the working shown.',
    evidence: {
      strength: 'MEASURED',
      source: `${SELFTEST_EVIDENCE}#demand`,
      finding: '3 of the 5 incumbents refuse this case in their own review replies.'
    }
  }
}

function shippedTest(slug, id, weight, overrides = {}) {
  return {
    ...promiseBase(id, weight),
    status: 'shipped',
    proof: {
      type: 'test',
      ref: `app/src/test/java/com/gexetr/${slug}/ThingTest.kt`,
      symbol: 'holdsThePromise',
      assertion: 'The unit test asserts the promise and fails when the behaviour is removed.',
      ...overrides
    }
  }
}

function shippedCode(slug, id, weight) {
  return {
    ...promiseBase(id, weight),
    status: 'shipped',
    proof: {
      type: 'code',
      ref: `app/src/main/java/com/gexetr/${slug}/Thing.kt`,
      symbol: 'printsTheWorking',
      assertion: 'The report shows the multipliers rather than a single opined number.'
    }
  }
}

/**
 * Write a slot whose contract and artifacts can pass or fail on request. The artifacts are
 * deliberately trivial files: the gate reads their text, so a file that contains the named
 * symbol is a valid proof and removing the symbol is a valid regression.
 */
function writePromiseSlot(slug, claims) {
  const dir = join(diffSandbox, 'apps', `${slotDate}-${slug}`)
  const testDir = join(dir, 'app', 'src', 'test', 'java', 'com', 'gexetr', slug)
  const codeDir = join(dir, 'app', 'src', 'main', 'java', 'com', 'gexetr', slug)
  mkdirSync(testDir, { recursive: true })
  mkdirSync(codeDir, { recursive: true })
  mkdirSync(join(dir, 'app', 'src', 'main', 'res', 'values'), { recursive: true })
  mkdirSync(join(dir, 'store', 'assets', 'en-US'), { recursive: true })
  writeFileSync(
    join(dir, 'differentiators.json'),
    `${JSON.stringify({ schemaVersion: 1, slot: slug, reconRef: SELFTEST_EVIDENCE, claims }, null, 2)}\n`,
    'utf8'
  )
  writeFileSync(
    join(testDir, 'ThingTest.kt'),
    `package com.gexetr.${slug}\n\nclass ThingTest {\n  fun holdsThePromise() {}\n}\n`,
    'utf8'
  )
  writeFileSync(
    join(codeDir, 'Thing.kt'),
    `package com.gexetr.${slug}\n\nfun printsTheWorking(): String = "shown"\n`,
    'utf8'
  )
  writeFileSync(
    join(dir, 'app', 'src', 'main', 'res', 'values', 'strings.xml'),
    '<resources>\n  <string name="result_citation">from the table</string>\n</resources>\n',
    'utf8'
  )
  writeFileSync(join(dir, 'store', 'assets', 'en-US', 'phone-04.png'), 'not a real png', 'utf8')
  return dir
}

function runDiffCheck(slug, extraArgs = []) {
  const result = spawnSync(
    process.execPath,
    [join(diffSandbox, 'scripts', 'check-differentiators.mjs'), '--slot', slug, ...extraArgs],
    { cwd: diffSandbox, encoding: 'utf8', windowsHide: true }
  )
  return { status: result.status, output: `${result.stdout || ''}${result.stderr || ''}` }
}

/** Every promise accounted for, one of each proof type, and one core promise narrowed. */
function resolvedClaims(slug) {
  return [
    shippedTest(slug, 'refuses-no-case', 'core'),
    {
      ...promiseBase('cites-its-source', 'core'),
      status: 'narrowed',
      narrowedTo: 'Cites the table name in the case report, but not the row number.',
      lostCoverage: 'The exact row within the table is not printed.',
      ownerDecision: {
        words: 'Ладно, пока пусть будет без номера строки.',
        recordedAt: '2026-09-26T19:00:00.000Z'
      },
      proof: {
        type: 'resource',
        ref: 'app/src/main/res/values/strings.xml',
        symbol: 'result_citation',
        assertion: 'The case report renders the citation line.'
      }
    },
    {
      ...promiseBase('keeps-working-visible', 'supporting'),
      status: 'shipped',
      proof: {
        type: 'code',
        ref: `app/src/main/java/com/gexetr/${slug}/Thing.kt`,
        symbol: 'printsTheWorking',
        assertion: 'The report shows the multipliers rather than a single opined number.'
      }
    },
    {
      ...promiseBase('no-ad-over-input', 'supporting'),
      status: 'shipped',
      proof: {
        type: 'capture',
        ref: 'store/assets/en-US/phone-04.png',
        assertion: 'The frame shows the number pad with no advertisement over it.'
      }
    },
    {
      ...promiseBase('row-level-citation', 'supporting'),
      status: 'dropped',
      droppedReason: 'The table row data is not licensed for redistribution in the app.'
    }
  ]
}

// Leg 1 — a fully accounted-for contract passes, all four proof types resolve, and the
// narrowed/dropped rows are printed rather than hidden inside the JSON.
const passSlug = 'diffpass'
writePromiseSlot(passSlug, resolvedClaims(passSlug))
const passRun = runDiffCheck(passSlug)
check(passRun.status === 0, `a resolved contract passes (got exit ${passRun.status})`)
check(/PASS\s+contract schema/.test(passRun.output), 'the passing run reports the promise summary')
check(
  /NARROWED \[core\] cites-its-source/.test(passRun.output) &&
    /DROPPED \[supporting\] row-level-citation/.test(passRun.output) &&
    /owner's words/.test(passRun.output),
  'the passing run reports every narrowed and dropped promise and whose decision it was'
)

// Leg 2 — a promised feature that never shipped must not pass. This is the defect the whole
// contract exists for: ten promises delivered, ten quietly abandoned, and every other gate
// still green.
const deferredSlug = 'diffdeferred'
writePromiseSlot(deferredSlug, [
  shippedTest(deferredSlug, 'refuses-no-case', 'core'),
  shippedCode(deferredSlug, 'keeps-working-visible', 'core'),
  {
    ...promiseBase('no-ad-over-input', 'supporting'),
    status: 'deferred',
    deferredReason: 'The ad SDK is not wired up yet.'
  }
])
const deferredRun = runDiffCheck(deferredSlug)
check(deferredRun.status !== 0, `a deferred promise fails the slot gate (got exit ${deferredRun.status})`)
check(/still deferred/.test(deferredRun.output), 'the failure says which promise is unfinished')

// Leg 3 — the same contract is readable by a builder mid-flight, which is the only thing
// --allow-deferred is for.
const progressRun = runDiffCheck(deferredSlug, ['--allow-deferred'])
check(
  progressRun.status === 0 && /still deferred/.test(progressRun.output),
  '--allow-deferred reports progress without passing the slot'
)

// Leg 4 — the sensor's key leg. The promise status still says "shipped"; only the artifact
// changed. A gate that reads the status field would pass, and this one must not.
const artifactDir = writePromiseSlot('diffgone', resolvedClaims('diffgone'))
writeFileSync(
  join(artifactDir, 'app', 'src', 'test', 'java', 'com', 'gexetr', 'diffgone', 'ThingTest.kt'),
  'package com.gexetr.diffgone\n\nclass ThingTest {\n}\n',
  'utf8'
)
const goneRun = runDiffCheck('diffgone')
check(goneRun.status !== 0, `a proof whose symbol was deleted fails (got exit ${goneRun.status})`)
check(
  /holdsThePromise/.test(goneRun.output) && /no longer appears/.test(goneRun.output),
  'the failure names the symbol that left the artifact'
)

// Leg 5 — breaking a core promise is the owner's decision, not the builder's.
const coreDroppedSlug = 'diffcore'
writePromiseSlot(coreDroppedSlug, [
  shippedTest(coreDroppedSlug, 'refuses-no-case', 'core'),
  shippedCode(coreDroppedSlug, 'keeps-working-visible', 'core'),
  {
    ...promiseBase('cites-its-source', 'core'),
    status: 'dropped',
    droppedReason: 'The citation needs a data file the build cannot ship.'
  }
])
const coreDroppedRun = runDiffCheck(coreDroppedSlug)
check(coreDroppedRun.status !== 0, `a core promise dropped without owner words fails (got exit ${coreDroppedRun.status})`)
check(/ownerDecision/.test(coreDroppedRun.output), 'the failure asks for the owner\'s own words')

// Leg 6 — a proof must live where its type belongs, or it is a checkbox pointing anywhere.
const wrongPlaceSlug = 'diffplace'
writePromiseSlot(wrongPlaceSlug, [
  shippedTest(wrongPlaceSlug, 'refuses-no-case', 'core', {
    type: 'test',
    ref: 'app/src/main/res/values/strings.xml',
    symbol: 'result_citation'
  }),
  shippedCode(wrongPlaceSlug, 'keeps-working-visible', 'core'),
  {
    ...promiseBase('cites-its-source', 'supporting'),
    status: 'dropped',
    droppedReason: 'The citation needs a data file the build cannot ship.'
  }
])
const wrongPlaceRun = runDiffCheck(wrongPlaceSlug)
check(wrongPlaceRun.status !== 0, `a proof in the wrong location fails (got exit ${wrongPlaceRun.status})`)
check(/is not in a location/.test(wrongPlaceRun.output), 'the failure names where a proof of that type may live')

// Leg 7 — a contract copied from another slot claims the wrong recon.
const copiedSlug = 'diffcopy'
const copiedDir = writePromiseSlot(copiedSlug, resolvedClaims(copiedSlug))
const copiedFile = join(copiedDir, 'differentiators.json')
const copiedContract = JSON.parse(readFileSync(copiedFile, 'utf8'))
copiedContract.slot = 'somewhere-else'
writeFileSync(copiedFile, `${JSON.stringify(copiedContract, null, 2)}\n`, 'utf8')
const copiedRun = runDiffCheck(copiedSlug)
check(copiedRun.status !== 0, `a contract naming another slot fails (got exit ${copiedRun.status})`)
check(/copied reason list/.test(copiedRun.output), 'the failure says the contract is not this slot\'s recon')

// Leg 8 — a citation to recon that does not exist is a dangling reference, not evidence.
const danglingSlug = 'diffdangling'
const danglingClaims = resolvedClaims(danglingSlug)
danglingClaims[0].evidence = {
  strength: 'MEASURED',
  source: 'docs/research/does-not-exist.md#demand',
  finding: '3 of the 5 incumbents refuse this case in their own review replies.'
}
writePromiseSlot(danglingSlug, danglingClaims)
const danglingRun = runDiffCheck(danglingSlug)
check(danglingRun.status !== 0, `evidence citing a missing document fails (got exit ${danglingRun.status})`)
check(/does not resolve to a file/.test(danglingRun.output), 'the failure names the unresolvable citation')

// Leg 9 — the floors. A contract with one promise, or with no core promise, is not a reason
// list an app can win on, and the gate must refuse it before it becomes a slot.
const floorRun = runDiffCheck('diffmissing', [])
check(floorRun.status !== 0, 'a contract for a slot that does not exist fails')
const thinContract = join(diffSandbox, 'thin.json')
writeFileSync(
  thinContract,
  `${JSON.stringify(validDifferentiators({
    claims: [deferredClaim('only-one', 'supporting', 'Does the one thing the app does.')]
  }), null, 2)}\n`,
  'utf8'
)
const thinRun = spawnSync(
  process.execPath,
  [join(diffSandbox, 'scripts', 'check-differentiators.mjs'), '--differentiators', thinContract],
  { cwd: diffSandbox, encoding: 'utf8', windowsHide: true }
)
const thinOutput = `${thinRun.stdout || ''}${thinRun.stderr || ''}`
check(thinRun.status !== 0, 'a one-promise contract is refused')
check(
  /at least 3 promises/.test(thinOutput) && /"core"/.test(thinOutput),
  'the refusal states the promise and core floors'
)

if (KEEP) {
  console.log(`\n  INFO      promise-gate sandbox kept at ${diffSandbox}`)
} else {
  rmSync(diffSandbox, { recursive: true, force: true })
}

// The stop hook reminds the agent to show evidence after it changes `apps/` or `harness/`. Its own
// text said "if this session changed apps/ or harness/" while the implementation emitted it
// unconditionally, so it fired after every turn — including turns that answered a question and
// edited nothing — and the owner read it as spam. A reminder that fires when nothing changed
// cannot be told apart from one that fires when something did, which is the opposite of a gate.
// The section below drives `scripts/test-stop-gate.mjs`, which is expected to FAIL against the
// previous implementation: quiet turns were the defect, and nothing measured them.
section('the stop gate fires on change, not on turns')
const stopGateProbe = spawnSync(process.execPath, [join(ROOT, 'scripts', 'test-stop-gate.mjs')], {
  cwd: ROOT,
  encoding: 'utf8',
  windowsHide: true
})
const stopGateRows = `${stopGateProbe.stdout || ''}${stopGateProbe.stderr || ''}`
  .split(/\r?\n/)
  .filter((row) => /^(PASS|FAIL)\s/.test(row))
if (stopGateRows.length === 0) {
  check(false, `stop-gate test reported no verdict (exit ${stopGateProbe.status ?? 'unknown'})`)
} else {
  for (const row of stopGateRows) {
    const [status, ...rest] = row.split(/\s+/)
    line(status === 'PASS' ? 'PASS' : 'FAIL', `stop-gate: ${rest.join(' ').trim()}`)
  }
}
check(stopGateProbe.status === 0, 'stop-gate test exits 0')

// ---------------------------------------------------------------------------------------------
// A coverage check that cannot fail is not a check.
//
// `check-ui-coverage.mjs` printed a PASS for every slot except the one it was written against: a slot
// with no ViewModel under `ui/` had nothing to compare and reported success, and a slot whose
// ViewModels share none of the required table's names reported "0 reachable, 0 reported as notes" -
// also success. Both are green lines produced by a check that structurally cannot go red, which an
// independent review reproduced on `apps/2026-09-25-pipe-sizing` (a released slot whose operations the
// table does not name). The two legs below are the two ways of checking nothing, and `verify-slot.mjs`
// maps exit 2 to UNVERIFIED so neither can back a release claim. Without these legs the sensor could
// regress to always-PASS and only a real slot would notice.
section('a coverage check that cannot fail is not a check')
// This section needs its own sandbox: the tree the earlier sections use is removed in their `finally`
// (line-level detail that cost one debugging round: `mkdirSync` recreates the directories inside the
// deleted path without complaining, and the sensor then fails to load, which reads as a red leg for
// the wrong reason).
const uiCoverageSandbox = createSandbox()
try {
  const uiCoverageSlot = `${slotDate}-uicoverage`
  const uiCoverageDir = join(
    uiCoverageSandbox,
    'apps',
    uiCoverageSlot,
    'app',
    'src',
    'main',
    'java',
    'com',
    'gexetr',
    'uicoverage',
    'ui'
  )
  mkdirSync(uiCoverageDir, { recursive: true })
  writeFileSync(
    join(uiCoverageDir, 'HomeScreen.kt'),
    'package com.gexetr.uicoverage.ui\n\n@Composable\nfun HomeScreen() {}\n',
    'utf8'
  )
  const runUiCoverage = () => {
    const probe = spawnSync(
      process.execPath,
      [join(uiCoverageSandbox, 'scripts', 'check-ui-coverage.mjs'), '--slot', uiCoverageSlot],
      { cwd: uiCoverageSandbox, encoding: 'utf8', windowsHide: true }
    )
    return `${probe.stdout || ''}${probe.stderr || ''}`
  }
  const noViewModelOutput = runUiCoverage()
  check(
    /UNVERIFIED/.test(noViewModelOutput),
    'a slot with no ViewModel under ui/ is UNVERIFIED rather than PASS'
  )
  // The operations table names the shipped flows it knows about; a ViewModel whose operations it does
  // not name measured nothing, so it is the same refusal rather than a clean bill of health.
  writeFileSync(
    join(uiCoverageDir, 'LedgerViewModel.kt'),
    'package com.gexetr.uicoverage.ui\n\nclass LedgerViewModel {\n    fun startLedgerRun() {}\n}\n',
    'utf8'
  )
  const unmatchedOutput = runUiCoverage()
  check(
    /UNVERIFIED/.test(unmatchedOutput),
    'a ViewModel whose operations the table does not name is UNVERIFIED rather than PASS'
  )
} finally {
  if (KEEP) {
    console.log(`\n  INFO      ui-coverage sandbox kept at ${uiCoverageSandbox}`)
  } else {
    rmSync(uiCoverageSandbox, { recursive: true, force: true })
  }
}

// ---------------------------------------------------------------------------------------------
// Every regression suite the harness owns is executed, and a suite that is not says why.
//
// These files used to be invoked one at a time by hand, and six of them - `test-apk-provenance`,
// `test-capture-publish`, `test-capture-assertions`, `test-codex-signals`, `test-play-scan`,
// `test-play-reviews` - had been written, cited in `AGENTS.md` as evidence a release rests on, and
// never run by the one command a session actually executes. That is this factory's most repeated
// defect in another coat: a check that exists and is never run cannot be told apart from a check
// that does not exist, and both read as green.
//
// The loop below therefore runs every `scripts/test-*` file it finds, except the ones with their own
// section above (`DEDICATED_SUITES`) and the ones that name a reason (`SUITE_EXCLUSIONS`). A suite
// added to `scripts/` tomorrow is picked up with no edit here, which is the safe direction: the
// failure mode of a hand-maintained runner list is a new sensor that nothing ever runs.
const DEDICATED_SUITES = new Set([
  'test-capture-locale.mjs',
  'test-checklist-products.py',
  'test-console-answers.py',
  'test-freshness.mjs',
  'test-icon-design.py',
  'test-in-app-policy.mjs',
  'test-locale-dedup.mjs',
  'test-release-notes.py',
  'test-stop-gate.mjs',
  'test-visual-qa-discovery.py'
])

// A suite excluded here must say why in the same breath, and a name that no longer exists on disk
// fails the coverage check below - a stale exclusion is how an exclusion list stops meaning
// anything. The bar for adding one is that the suite cannot run offline or without a device or local
// captures - never that it is slow or inconvenient.
const SUITE_EXCLUSIONS = new Map([
  [
    'test-store-studio-completeness.py',
    'composes the full pack eleven times and reads store/captures/raw/, which is gitignored, so it cannot run on a clean checkout; it is a release-procedure step, listed in AGENTS.md'
  ]
])

section('every regression suite the harness owns is executed')
const suiteDirectory = join(ROOT, 'scripts')
const diskSuites = readdirSync(suiteDirectory)
  .filter((name) => /^test-.*\.(mjs|py)$/.test(name))
  .sort()

const staleExclusions = [...SUITE_EXCLUSIONS.keys()].filter((name) => !diskSuites.includes(name))
check(
  staleExclusions.length === 0,
  `every suite named in SUITE_EXCLUSIONS exists on disk (stale: ${staleExclusions.join(', ') || 'none'})`
)
const mislabelled = [...DEDICATED_SUITES].filter((name) => SUITE_EXCLUSIONS.has(name))
check(
  mislabelled.length === 0,
  `no suite is both dedicated and excluded (${mislabelled.join(', ') || 'none'})`
)
const dedicatedGone = [...DEDICATED_SUITES].filter((name) => !diskSuites.includes(name))
check(
  dedicatedGone.length === 0,
  `every suite claimed to have its own section still exists (missing: ${dedicatedGone.join(', ') || 'none'})`
)

const genericSuites = diskSuites.filter(
  (name) => !DEDICATED_SUITES.has(name) && !SUITE_EXCLUSIONS.has(name)
)
check(
  genericSuites.length > 0,
  `the runner found suites to execute (${genericSuites.length} of ${diskSuites.length} on disk)`
)

for (const name of diskSuites) {
  if (SUITE_EXCLUSIONS.has(name)) {
    line('SKIP', `${name} — ${SUITE_EXCLUSIONS.get(name)}`)
    continue
  }
  if (DEDICATED_SUITES.has(name)) continue
  const interpreter = name.endsWith('.py') ? 'python' : process.execPath
  const probe = spawnSync(interpreter, [join(suiteDirectory, name)], {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  })
  const rows = `${probe.stdout || ''}${probe.stderr || ''}`
    .split(/\r?\n/)
    .map((row) => row.trim())
    .filter(Boolean)
  // The summary is the line the suite offers as one, not simply the last line it printed: a suite whose
  // final output is a diagnostic (a dump excerpt, a device reading) would otherwise be reported as
  // `test-capture-assertions.mjs: nearby: text="Material or system" ...`, which says the suite passed
  // while showing something that looks like a failure.
  const summaryRow =
    rows.find((row) => /^\d+\/\d+ checks passed$/.test(row)) ||
    rows.find((row) => /VERDICT:/.test(row)) ||
    rows.find((row) => /PASSED|passed/.test(row)) ||
    rows[rows.length - 1]
  const summary = summaryRow || `exit ${probe.status ?? 'unknown'}`
  if (probe.status === 0) {
    line('PASS', `${name}: ${summary}`)
  } else {
    line('FAIL', `${name}: ${summary}`)
    for (const row of rows.filter((entry) => /^(FAIL|ERROR)/i.test(entry)).slice(0, 5)) {
      console.log(`            ${row}`)
    }
    failed += 1
  }
}

console.log(`\nVERDICT: ${failed ? 'FAIL' : 'PASS'}`)
process.exitCode = failed ? 1 : 0

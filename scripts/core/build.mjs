// Build, toolchain, namespace and copy-filter facts.
//
// Ledger rows: requirements-preservation.md §1.1-1.4 (identity, namespace, the four textual forms),
// §2.1-2.7 (toolchain pins and suppressed defaults), §15.6-15.8 (the money plan's machine-checkable
// half), §16.2-16.3 (recursive copies filter; a text pass must never touch a binary).
//
// Why these checks exist: the toolchain table was fixed after `org.jetbrains.kotlin.android`,
// `android.kotlinOptions`, `kapt` and a Compose-compiler version that had drifted from Kotlin each
// cost a build; the theme-parent rule broke once, because a Compose-only app that parents from a
// Material Components View style does not link; and `buildToolsVersion` was pinned in AGENTS.md
// while no artifact declared it. A rule that lives only in prose is enforced by nothing, which is
// this repository's most-recorded failure mode.
import { existsSync, readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { fail, guard, pass, ROOT, runFixtures, skip, unverified, expectEqual, expectTrue, VERDICT } from './lib.mjs'
import { applicationIdPattern, packagePrefix } from '../lib/namespaces.mjs'
import { parseEconomics, validateEconomicsAgainstCapabilities } from '../lib/slot-economics.mjs'
import { IDENTITY_FROZEN_STATES, readRegistry } from './project.mjs'
// Imported, never re-implemented: a second answer to "what must not be copied" is the defect the
// shared predicate was consolidated to remove.
import { isExcludedScaffoldPath } from '../new-slot.mjs'

// The toolchain pins (AGENTS.md "Required toolchain", ledger §2.1-2.4) live here rather than being
// parsed out of the scaffold, because a check that compares an artifact against itself can never
// fail. The scaffold is the artifact; this table is the rule.
export const PINS = Object.freeze({
  gradle: '9.7.1', agp: '9.4.1', kotlin: '2.3.21', composeBom: '2026.09.00',
  baselineProfilePlugin: '1.5.0', profileInstaller: '1.4.1', benchmarkMacro: '1.4.1',
  compileSdk: '37', targetSdk: '36', minSdk: '26', buildTools: '36.0.0', java: '17'
})
const CATALOG_PIN_KEYS = ['agp', 'kotlin', 'composeBom', 'baselineProfilePlugin', 'profileInstaller', 'benchmarkMacro']

/** The "Defaults to suppress" table, each naming its replacement rather than only a ban. */
export const FORBIDDEN_PATTERNS = Object.freeze([
  ['org.jetbrains.kotlin.android', /org\.jetbrains\.kotlin\.android/, 'AGP 9 has built-in Kotlin; the standalone plugin is suppressed'],
  ['android.kotlinOptions', /android\.kotlinOptions/, 'use android.compileOptions.targetCompatibility; built-in Kotlin derives jvmTarget'],
  ['kapt(', /\bkapt\s*\(/, 'use KSP when annotation processing is required'],
  ['applicationVariants', /\bapplicationVariants\b/, 'the API is removed; use androidComponents.onVariants'],
  ['android.enableR8.fullMode', /android\.enableR8\.fullMode/, 'obsolete flag; full mode is the default'],
  ['android.builtInKotlin', /android\.builtInKotlin/, 'obsolete flag'],
  ['android.newDsl', /android\.newDsl/, 'obsolete flag'],
  ['io.flutter', /\bio\.flutter\b/, 'native Kotlin + Compose only'],
  ['react-native', /react-native/i, 'native Kotlin + Compose only'],
  ['expo', /\bexpo\b/i, 'native Kotlin + Compose only'],
  ['next.js', /next\.js/i, 'native Kotlin + Compose only']
])

// Third-party package roots that may legally appear. The applicationId's own root is absent on
// purpose: it is compared against the imported `packagePrefix`, so the namespace rule keeps one home.
export const ALLOWED_PACKAGE_ROOTS = Object.freeze([
  'androidx', 'android', 'kotlin', 'kotlinx', 'java', 'javax',
  'com.google', 'com.android', // AGP's plugin id, devtools.ksp, the AndroidX test runner
  'org.jetbrains', // JetBrains annotations and the Compose compiler plugin marker
  // The JDK's own namespace, which a build script names when it excludes a JDK service file the
  // merge would otherwise carry into the APK. Measured 2026-10-09 on the Lumintra slot: the exclude
  // `"META-INF/services/com.sun.source.util.Plugin"` is preceded by a slash and the sweep never saw
  // it, but the *comment* documenting that exclude read as a package declaration and failed two
  // rows — `build.namespaceSweep` and the repo-wide leg of `build.fixtures`. `com.sun` is not a
  // namespace this factory ever shipped under, so the finding was the sweep's false positive rather
  // than a leftover; a JDK root is the same kind of occurrence the roots above already are.
  'com.sun',
  'com.squareup', // OkHttp is a third-party library root, not a leftover.
  'com.burhanrashid52', // PhotoEditor (stories canvas) — third-party library root, not a leftover.
])

// Build outputs, IDE state, machine-local caches and worker ledgers. A sweep that descends into
// `build/` reads a stale generated copy of a source file and reports it as source.
const SKIP_SEGMENTS = new Set([
  'node_modules', '.git', '__pycache__', 'build', '.gradle', '.kotlin', '.idea', '.cxx', 'dist',
  'captures', '.kiln', '.superpowers', '.venv', 'venv'
])
const BUILD_SOURCE_EXTENSIONS = new Set(['.kt', '.java', '.kts', '.gradle', '.xml', '.toml', '.properties', '.pro'])
const SWEEP_EXTENSIONS = new Set([...BUILD_SOURCE_EXTENSIONS, '.md', '.mjs', '.js', '.cjs', '.py'])
const relPath = (file) => path.relative(ROOT, file).split(path.sep).join('/')

function walkSourceFiles(dir, extensions, acc = []) {
  let entries
  try { entries = readdirSync(dir, { withFileTypes: true }) } catch { return acc }
  for (const entry of entries) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) { if (!SKIP_SEGMENTS.has(entry.name)) walkSourceFiles(full, extensions, acc) }
    else if (entry.isFile() && extensions.has(path.extname(entry.name).toLowerCase())) acc.push(full)
  }
  return acc
}

/**
 * Decodes a file as text, or returns null when it holds a NUL byte.
 *
 * The core sweeps the tree itself, so it obeys the rule it checks elsewhere (§16.3): a byte pattern
 * matched inside a binary is invisible until something far away breaks. Null rather than a lossy
 * decode is deliberate — a "mostly text" file is one this sweep must not speak about.
 */
export function decodeSource(bytes) {
  return bytes.includes(0) ? null : bytes.toString('utf8')
}
const readSource = (file) => decodeSource(readFileSync(file))

function slotDirs() {
  const apps = path.join(ROOT, 'apps')
  if (!existsSync(apps)) return []
  return readdirSync(apps, { withFileTypes: true })
    .filter((entry) => entry.isDirectory()).map((entry) => path.join(apps, entry.name)).sort()
}

// ---------------------------------------------------------------------------------------------
// Toolchain pins
// ---------------------------------------------------------------------------------------------

/** The Gradle version named by a wrapper's `distributionUrl`. */
export function wrapperPinProblems(text) {
  const match = /distributionUrl=.*?gradle-([0-9][A-Za-z0-9.\-_]*?)-(?:bin|all)\.zip/.exec(text)
  if (!match) return ['distributionUrl does not name a gradle-<version>-bin|all.zip distribution']
  return match[1] === PINS.gradle ? [] : [`Gradle ${match[1]}, expected ${PINS.gradle}`]
}

/**
 * Differences between a catalog's declared pins and the table above.
 *
 * Parsed rather than grepped, because the Compose compiler plugin's version is not a literal: it is
 * a `version.ref` pointing at a key. A grep for the plugin line finds the plugin and never notices
 * that it points somewhere else, which is exactly how the two versions drifted apart. Only keys the
 * catalog declares are compared, so a catalog that stays silent inherits the scaffold's value.
 */
export function catalogPinProblems(text) {
  const versions = {}
  const composeCompilerRefs = []
  let section = null
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim()
    if (!line || line.startsWith('#')) continue
    const heading = /^\[(.+)\]$/.exec(line)
    if (heading) { section = heading[1]; continue }
    const entry = section === 'versions' ? /^([A-Za-z0-9_]+)\s*=\s*"([^"]*)"/.exec(line) : null
    if (entry) { versions[entry[1]] = entry[2]; continue }
    if (section !== 'plugins') continue
    const plugin = /^([A-Za-z0-9_.-]+)\s*=\s*\{\s*id\s*=\s*"[^"]*"\s*,\s*version\.ref\s*=\s*"([^"]*)"/.exec(line)
    if (plugin && plugin[1] === 'compose-compiler') composeCompilerRefs.push(plugin[2])
  }
  const problems = CATALOG_PIN_KEYS.filter((key) => versions[key] !== undefined && versions[key] !== PINS[key]).map(
    (key) => `${key} is "${versions[key]}", expected "${PINS[key]}"`
  )
  for (const ref of composeCompilerRefs) {
    if (ref !== 'kotlin') {
      // The Compose compiler plugin ships with Kotlin, so a separate key is how the two versions drift.
      problems.push(`the compose-compiler plugin uses version.ref "${ref}"; it must be "kotlin", because the Compose compiler plugin ships with Kotlin and the two versions must match`)
    }
  }
  return problems
}

/**
 * Differences in an `android { }` build script.
 *
 * `requireAll` splits the two callers: the scaffold is the artifact that must *declare* every pin,
 * while a slot scaffolded before a pin existed inherits it, so only what the slot restates is
 * compared. A contradiction — legacy JNI packaging, the non-optimising ProGuard default, a release
 * that is not minified — is a finding either way.
 */
export function androidConfigProblems(text, { requireAll = true } = {}) {
  const problems = []
  const declared = (label, re, expected) => {
    const match = re.exec(text)
    if (!match) { if (requireAll) problems.push(`${label} is not declared (expected ${expected})`) }
    else if (match[1] !== expected) problems.push(`${label} is ${match[1]}, expected ${expected}`)
  }
  declared('compileSdk', /\bcompileSdk\s*=\s*(\d+)/, PINS.compileSdk)
  declared('targetSdk', /\btargetSdk\s*=\s*(\d+)/, PINS.targetSdk)
  declared('minSdk', /\bminSdk\s*=\s*(\d+)/, PINS.minSdk)
  declared('buildToolsVersion', /\bbuildToolsVersion\s*=\s*"([^"]+)"/, PINS.buildTools)
  declared('sourceCompatibility', /\bsourceCompatibility\s*=\s*JavaVersion\.VERSION_(\d+)/, PINS.java)
  declared('targetCompatibility', /\btargetCompatibility\s*=\s*JavaVersion\.VERSION_(\d+)/, PINS.java)
  for (const [flag, why] of [['isMinifyEnabled', 'a release must be minified'], ['isShrinkResources', 'a release must shrink resources']]) {
    if (new RegExp(`${flag}\\s*=\\s*false`).test(text)) problems.push(`${flag} = false; ${why}`)
    else if (requireAll && !new RegExp(`${flag}\\s*=\\s*true`).test(text)) problems.push(`${flag} = true is not declared`)
  }
  if (/"proguard-android\.txt"/.test(text)) {
    problems.push('getDefaultProguardFile("proguard-android.txt") is the non-optimising default; use proguard-android-optimize.txt')
  } else if (requireAll && !/getDefaultProguardFile\("proguard-android-optimize\.txt"\)/.test(text)) {
    problems.push('getDefaultProguardFile("proguard-android-optimize.txt") is not declared')
  }
  // 16 KB pages: legacy JNI packaging stores .so entries uncompressed and unaligned.
  if (/useLegacyPackaging\s*=\s*true/.test(text)) problems.push('useLegacyPackaging = true breaks 16 KB page-size compatibility')
  else if (requireAll && !/useLegacyPackaging\s*=\s*false/.test(text)) problems.push('jniLibs { useLegacyPackaging = false } is not declared')
  return problems
}

/** Finds the suppressed defaults, each with the file and line that carries it. */
export function forbiddenFindings(dir) {
  const findings = []
  for (const file of walkSourceFiles(dir, BUILD_SOURCE_EXTENSIONS)) {
    const text = readSource(file)
    if (text === null) continue
    text.split(/\r?\n/).forEach((line, index) => {
      for (const [name, re, why] of FORBIDDEN_PATTERNS) {
        if (re.test(line)) findings.push(`${relPath(file)}:${index + 1}: ${name} — ${why}`)
      }
    })
  }
  return findings
}

/**
 * A theme parent of `Theme.Material3.*` or `Theme.MaterialComponents.*`.
 *
 * Both are View-library styles, and a Compose-only app does not bundle Material Components View
 * libraries, so such a parent does not resolve. It broke once; the fix is an
 * `android:Theme.Material.*` parent or a Compose `MaterialTheme`.
 */
export function themeParentProblems(text) {
  return [...text.matchAll(/parent\s*=\s*"(Theme\.(?:Material3|MaterialComponents)[^"]*)"/g)].map(
    (match) => `parent="${match[1]}" is a View-library style; a Compose-only app must use android:Theme.Material.* or a Compose MaterialTheme`
  )
}

export function themeParentFindings(dir) {
  const findings = []
  for (const file of walkSourceFiles(dir, new Set(['.xml']))) {
    if (!/[\\/]res[\\/]values(-[A-Za-z0-9-]+)?[\\/]themes\.xml$/.test(file)) continue
    const text = readSource(file)
    if (text !== null) themeParentProblems(text).forEach((p) => findings.push(`${relPath(file)}: ${p}`))
  }
  return findings
}

export function checkToolchainPins() {
  return guard('build.toolchain', () => {
    const scaffold = path.join(ROOT, 'scaffold', 'android-compose')
    if (!existsSync(scaffold)) return unverified('build.toolchain', 'no scaffold/android-compose to measure the pins against')
    const problems = []
    const read = (relative, label) => {
      const full = path.join(scaffold, relative)
      if (!existsSync(full)) { problems.push(`scaffold/${label} is missing, so its pins cannot be read`); return null }
      return readFileSync(full, 'utf8')
    }
    const wrapper = read(path.join('gradle', 'wrapper', 'gradle-wrapper.properties'), 'gradle/wrapper/gradle-wrapper.properties')
    const catalog = read(path.join('gradle', 'libs.versions.toml'), 'gradle/libs.versions.toml')
    const gradle = read(path.join('app', 'build.gradle.kts'), 'app/build.gradle.kts')
    if (wrapper !== null) wrapperPinProblems(wrapper).forEach((p) => problems.push(`scaffold gradle-wrapper.properties: ${p}`))
    if (catalog !== null) catalogPinProblems(catalog).forEach((p) => problems.push(`scaffold gradle/libs.versions.toml: ${p}`))
    if (gradle !== null) androidConfigProblems(gradle).forEach((p) => problems.push(`scaffold app/build.gradle.kts: ${p}`))

    // The suppressed defaults and the theme rule apply to a slot as well as to the scaffold, so they
    // are swept with file and line across both; and a slot that restates a catalog key must agree.
    const slots = slotDirs()
    for (const dir of [scaffold, ...slots]) problems.push(...forbiddenFindings(dir), ...themeParentFindings(dir))
    for (const slot of slots) {
      const file = path.join(slot, 'gradle', 'libs.versions.toml')
      if (!existsSync(file)) continue
      catalogPinProblems(readFileSync(file, 'utf8')).forEach((p) => problems.push(`${relPath(file)}: ${p}`))
    }
    if (problems.length > 0) return fail('build.toolchain', problems.join('; '))
    return pass(
      'build.toolchain',
      `scaffold pins Gradle ${PINS.gradle}, AGP ${PINS.agp}, Kotlin ${PINS.kotlin}, Compose BOM ${PINS.composeBom}, ` +
        `SDK ${PINS.compileSdk}/${PINS.targetSdk}/${PINS.minSdk}, build-tools ${PINS.buildTools}; scaffold and ` +
        `${slots.length} slot(s) carry no suppressed default and no View-library theme parent`
    )
  })
}

// ---------------------------------------------------------------------------------------------
// The namespace sweep
// ---------------------------------------------------------------------------------------------

// One prefix hides in four textual forms, and a rename that searches one of them reports zero
// leftovers while the others sit untouched in a schema or a rule file (§1.4). Three of the four
// differ only in how the dot between the segments is escaped — plain, one backslash, two — so the
// sweep strips the backslashes that escape a *dot* and reads the flattened result rather than
// carrying four patterns that can drift apart. It deliberately does not strip every backslash:
// measured here, a line that held a token followed by two newline escapes flattened into a package
// that appears nowhere, the token fused with the word after it, which is a finding no author can act
// on. The plain form counts as an identity under `apps/` and `scaffold/`; under `scripts/` only an
// escaped or path spelling does, because there a plain token is a sensor's own data.
//
// `apps/<slot>/docs/` is excluded: those files are the slot's cut-out archive (proposals, domain
// memos) and cite competitor `com.*` packages by design. They lived under root `docs/` before the
// 2026-10-04 cut-out rule moved them, and root `docs/` is outside `SWEEP_ROOTS` — sweeping them
// after the move would punish the archive for the same prose the factory already accepted.
const SWEEP_ROOTS = [
  { root: 'apps', plainEscapedOnly: false },
  { root: 'scaffold', plainEscapedOnly: false },
  { root: 'scripts', plainEscapedOnly: true }
]

/** Slot docs are product archives, not shipping identity — see the comment on `SWEEP_ROOTS`. */
const isSlotDocsPath = (file) => /^apps\/[^/]+\/docs\//.test(relPath(file))

// Files whose package-shaped tokens are sensor data rather than a declaration. A suite's synthetic
// path (the reserved example domain, reverse-DNS'd) and its banned-vendor coordinate name no package
// this factory ships: they are the fixture a sensor is calibrated against, and the client-secrets
// rule downgrades a documented placeholder for the same reason. The factory's own *former* prefix is
// the same kind of occurrence — it survives only inside a suite's leftover-reference probe,
// which is the sensor for the rename this sweep exists to catch, so deleting the mention would
// delete the check. Findings here are notes, never dropped: the same token in a shipping artifact or
// a library module is still a failure.
const SENSOR_DATA = /^(?:test-.*\.(?:mjs|py)|.*\.test\.mjs|selftest\.mjs)$/

/**
 * Foreign package tokens in a text, as `"<token> (line N)"`.
 *
 * A token is any package root that is neither the imported `packagePrefix` nor an allowlisted
 * third-party root. The root rather than the full package keeps the finding short; the line number
 * is what makes it actionable.
 */
export function namespaceTokenProblems(text, { plainEscapedOnly = false } = {}) {
  const problems = []
  const lines = text.split(/\r?\n/)
  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]
    const candidates = []
    for (const match of line.replace(/\\+\./g, '.').matchAll(/(?:^|[^\w.$/])com\.([a-z][a-z0-9_]*)/g)) {
      // A plain spelling is one the raw line already contains; anything else is here because it lost
      // an escape, which is what makes it an identity rather than prose.
      if (!plainEscapedOnly || !line.includes(`com.${match[1]}`)) candidates.push(`com.${match[1]}`)
    }
    for (const match of line.matchAll(/(?:^|[^\w.$])com\/([a-z][a-z0-9_]*)/g)) candidates.push(`com.${match[1]}`)
    for (const root of candidates) {
      if (root === packagePrefix) continue
      if (ALLOWED_PACKAGE_ROOTS.some((allowed) => root === allowed || root.startsWith(`${allowed}.`))) continue
      const finding = `${root} (line ${index + 1})`
      if (!problems.includes(finding)) problems.push(finding)
    }
  }
  return problems
}

export function checkNamespaceSweep() {
  return guard('build.namespaceSweep', () => {
    const files = SWEEP_ROOTS.flatMap(({ root, plainEscapedOnly }) =>
      walkSourceFiles(path.join(ROOT, root), SWEEP_EXTENSIONS)
        .filter((file) => !isSlotDocsPath(file))
        .map((file) => ({ file, plainEscapedOnly }))
    )
    if (files.length === 0) {
      return unverified('build.namespaceSweep', 'no source file was found to sweep for a foreign package token')
    }
    const problems = []
    const notes = []
    let binaries = 0
    for (const { file, plainEscapedOnly } of files) {
      const text = readSource(file)
      if (text === null) { binaries += 1; continue }
      const bucket = SENSOR_DATA.test(path.basename(file)) ? notes : problems
      namespaceTokenProblems(text, { plainEscapedOnly }).forEach((p) => bucket.push(`${relPath(file)}: ${p}`))
    }
    const binaryNote = binaries ? `; ${binaries} NUL-bearing file(s) skipped rather than decoded` : ''
    if (problems.length > 0) return fail('build.namespaceSweep', problems.join('; '))
    const noteText = notes.length ? `; sensor data (not declarations): ${notes.join('; ')}` : ''
    return pass(
      'build.namespaceSweep',
      `${files.length} file(s) swept in all four textual forms; no package token outside ${packagePrefix}${noteText}${binaryNote}`
    )
  })
}

// ---------------------------------------------------------------------------------------------
// The copy filter and the text pass
// ---------------------------------------------------------------------------------------------

/** The names ledger §16.2 requires a recursive copy to exclude, plus the factory's own additions. */
export const EXCLUSION_PROBES = Object.freeze([
  'app/build/outputs/logs/manifest-merger-report.txt', 'gradle/.gradle/8.0/checksums', '.kotlin/sessions/x',
  '.idea/workspace.xml', 'app/.cxx/Debug/x', 'dist/app-release.apk', 'captures/raw/phone-01.png',
  'scripts/__pycache__/x.pyc', 'local.properties', 'keystore.properties', '.kiln-abc.log'
])

/** Paths the copy must keep: excluding one of these is what makes a fresh slot unable to build. */
export const EXCLUSION_SURVIVORS = Object.freeze([
  'gradle/wrapper/gradle-wrapper.jar', // excluded once, and no slot could build
  'gradle/wrapper/gradle-wrapper.properties',
  'gradle/libs.versions.toml',
  'build.gradle.kts', // a file whose *name* starts with "build" is not a build directory
  'app/src/main/java/com/gexetr/scaffold/MainActivity.kt',
  'app/src/main/res/values/strings.xml'
])

// The sites that decode a tree's files as text. Each must skip a NUL-bearing file before decoding it
// (§16.3), and the shape below is the one those sites use: read the bytes, test them.
//
// After `reduce-factory-to-core` the core has **one** decoder — `decodeSource()` in this file — and
// every sweep imports it rather than re-implementing the test (`scripts/core/manifest.mjs` reads a
// slot's files and a slot's manifest through it). So the observable guard is the definition itself,
// and the site below is that definition rather than the three monoliths that used to each carry a
// copy. Listing a caller would measure nothing: a caller that imported the predicate cannot skip it.
const NUL_GUARD_SITES = Object.freeze([
  { file: 'scripts/core/build.mjs', why: 'decodeSource() is the one place the core turns a file\'s bytes into text, and every sweep imports it' },
  { file: 'scripts/store/render_documents.py', why: 'slot_kotlin_sources() reads Kotlin sources looking for declared product ids' }
])

/** True when a text applies a NUL-byte test before turning bytes into a string. */
export function hasNulGuard(text) {
  return (
    /bytes\.includes\(\s*0\s*\)/.test(text) || // Node: read as a Buffer, test the byte
    /b"\\x00"\s+in\s+\w+/.test(text) || // Python: read as bytes, test the byte
    /includes\(\s*0x0*\s*\)/.test(text)
  )
}

/**
 * Every recursive copy in `scripts/`, and whether it decides what to take.
 *
 * A `filter:` is what makes a copy decide what to take, and that is where a second exclusion
 * vocabulary would live. A copy of named files takes what it names, so it has no filter to duplicate
 * and is exempt with a reason rather than by omission.
 */
export function copySites() {
  const sites = []
  for (const file of walkSourceFiles(path.join(ROOT, 'scripts'), new Set(['.mjs', '.py']))) {
    const text = readSource(file)
    if (text === null || !/cpSync\(|copytree\(/.test(text)) continue
    sites.push({ rel: relPath(file), decides: /\bfilter\s*:/.test(text), namesPredicate: text.includes('isExcludedScaffoldPath') })
  }
  return sites
}

// The names that are unambiguously build output or IDE state. A list carrying four or more of them
// answers "what must not be read as source" a second time — visible, but a note rather than a failure
// when that list is not a copy filter, because §16.2 governs recursive copies and the copy sites
// themselves are asserted above.
const DISTINCTIVE_EXCLUSIONS = ['.gradle', '.kotlin', '.idea', '.cxx', 'captures', 'dist']

function overlappingExclusionLists() {
  const findings = []
  for (const file of walkSourceFiles(path.join(ROOT, 'scripts'), new Set(['.mjs', '.py']))) {
    const rel = relPath(file)
    if (rel === 'scripts/new-slot.mjs' || rel === 'scripts/core/build.mjs') continue
    const text = readSource(file)
    if (text === null) continue
    const restated = DISTINCTIVE_EXCLUSIONS.filter((name) => text.includes(`'${name}'`) || text.includes(`"${name}"`))
    if (restated.length >= 4) findings.push(`${rel} [${restated.join(', ')}]`)
  }
  return findings
}

export function checkCopyFilter() {
  return guard('build.copyFilter', () => {
    const problems = []
    const notes = []

    // (a) The exclusion predicate has exactly one home, is importable, and still answers correctly.
    const predicateFile = path.join(ROOT, 'scripts', 'new-slot.mjs')
    if (!existsSync(predicateFile)) {
      problems.push('scripts/new-slot.mjs is missing, so the recursive-copy exclusion predicate cannot be read')
    } else {
      if (!/export\s*\{[^}]*isExcludedScaffoldPath/.test(readFileSync(predicateFile, 'utf8'))) {
        problems.push('scripts/new-slot.mjs does not export isExcludedScaffoldPath, so no caller can reuse the filter')
      }
      for (const probe of EXCLUSION_PROBES) {
        if (!isExcludedScaffoldPath(probe)) problems.push(`the copy filter would copy ${probe}`)
      }
      for (const survivor of EXCLUSION_SURVIVORS) {
        if (isExcludedScaffoldPath(survivor)) problems.push(`the copy filter would drop ${survivor}`)
      }
      // §16.2 names `*.so`; the predicate does not, and no slot ships a native library today. A note
      // so the gap is visible rather than closed by silence.
      if (!isExcludedScaffoldPath('jni/x.so')) {
        notes.push('so is named by the ledger but not by the predicate; no slot ships a native library today')
      }
    }

    // A copy that filters must filter through the predicate; re-deciding it locally is the defect the
    // shared predicate was consolidated to remove.
    const sites = copySites()
    const deciding = sites.filter((site) => site.decides)
    for (const site of deciding) {
      if (!site.namesPredicate) {
        problems.push(`${site.rel} filters a recursive copy without isExcludedScaffoldPath, so it decides for itself what must not be copied`)
      }
    }
    const exempt = sites.filter((site) => !site.decides).map((site) => site.rel)
    if (exempt.length) notes.push(`${exempt.join(', ')} copy named files, so they carry no filter to share`)
    const overlapping = overlappingExclusionLists()
    if (overlapping.length) notes.push(`other exclusion lists answer a read or a digest, not a copy: ${overlapping.join('; ')}`)

    // (b) The text passes skip NUL-bearing files before decoding them.
    let guardSites = 0
    for (const { file, why } of NUL_GUARD_SITES) {
      const absolute = path.join(ROOT, file)
      if (!existsSync(absolute)) { notes.push(`${file} is gone, so its NUL guard cannot be read (${why})`); continue }
      guardSites += 1
      if (!hasNulGuard(readFileSync(absolute, 'utf8'))) {
        problems.push(`${file}: no NUL-byte guard before decoding bytes as text (${why})`)
      }
    }
    if (guardSites === 0) {
      return unverified('build.copyFilter', 'no text-decoding site exists to measure, so §16.3 could not be evaluated')
    }
    const noteText = notes.length ? `; notes: ${notes.join('; ')}` : ''
    if (problems.length > 0) return fail('build.copyFilter', problems.join('; ') + noteText)
    return pass(
      'build.copyFilter',
      `one exclusion predicate (${EXCLUSION_PROBES.length} excluded, ${EXCLUSION_SURVIVORS.length} kept), ` +
        `${deciding.length} copy filter(s) route through it, ${guardSites} text pass(es) skip NUL-bearing files${noteText}`
    )
  })
}

// ---------------------------------------------------------------------------------------------
// Slot-level facts
// ---------------------------------------------------------------------------------------------

const IDENTITY_DIR_PATTERN = /^(\d{4}-\d{2}-\d{2})-([a-z0-9][a-z0-9-]*)$/

/** Paths a slot must never carry: the factory's own configuration, which stays at the root. */
const EMBEDDED_FACTORY_CONFIG = Object.freeze([
  ['.cursor', 'the Cursor rules, commands and hooks belong to the factory at the repository root'],
  ['.agents', 'shared agent skills belong to the factory at the repository root'],
  ['openspec', 'a slot records its own change under the root openspec/changes/, not inside itself'],
  ['harness', 'the harness stays at the repository root so slots cannot fork it']
])

export function checkIdentityFrozen(slotDir) {
  return guard('slot.identity', () => {
    const name = path.basename(slotDir)
    const problems = []
    const match = IDENTITY_DIR_PATTERN.exec(name)
    if (!match) problems.push(`the directory name '${name}' is not apps/<YYYY-MM-DD>-<slug>/`)
    const slug = match ? match[2] : name.replace(/^\d{4}-\d{2}-\d{2}-/, '')
    const gradle = path.join(slotDir, 'app', 'build.gradle.kts')
    const buildText = existsSync(gradle) ? readFileSync(gradle, 'utf8') : null
    if (buildText === null) problems.push('app/build.gradle.kts is missing, so no applicationId can be read')
    const declared = buildText === null ? null : /applicationId\s*=\s*"([^"]+)"/.exec(buildText)
    const namespace = buildText === null ? null : /namespace\s*=\s*"([^"]+)"/.exec(buildText)
    if (buildText !== null && !declared) problems.push('app/build.gradle.kts declares no applicationId')
    if (declared && !applicationIdPattern.test(declared[1])) {
      // Built from the imported pattern, never hand-written: a hardcoded prefix once told the user to
      // pass a namespace the regex had already stopped accepting.
      problems.push(`applicationId "${declared[1]}" does not match ${applicationIdPattern.source} from scripts/lib/namespaces.mjs`)
    }
    if (declared && namespace && namespace[1] !== declared[1]) {
      problems.push(`namespace "${namespace[1]}" differs from applicationId "${declared[1]}"`)
    }
    if (!existsSync(path.join(ROOT, 'registry', 'apps.json'))) {
      problems.push('registry/apps.json is missing, so a frozen identity cannot be confirmed')
    } else {
      const entry = readRegistry().apps.find((app) => app.id === slug)
      if (!entry) problems.push(`registry/apps.json records no slot with the id '${slug}'`)
      // The applicationId is permanent after the first Play upload: the listing, the signing key and
      // every install are bound to it, so a rename ships a second app nobody owns.
      else if (declared && IDENTITY_FROZEN_STATES.includes(entry.status) && declared[1] !== entry.applicationId) {
        problems.push(`the slot is '${entry.status}', so its applicationId is frozen: registry/apps.json says "${entry.applicationId}", app/build.gradle.kts says "${declared[1]}"`)
      }
    }
    for (const [embedded, why] of EMBEDDED_FACTORY_CONFIG) {
      if (existsSync(path.join(slotDir, embedded))) problems.push(`the slot embeds ${embedded}/ — ${why}`)
    }
    if (problems.length > 0) return fail('slot.identity', problems.join('; '))
    return pass('slot.identity', `${name} declares ${declared ? declared[1] : '<none>'} and carries no factory configuration`)
  })
}

export function checkEconomicsPlan(slotDir) {
  return guard('slot.economics', () => {
    const file = path.join(slotDir, 'economics.json')
    // SKIP, not PASS: the slot predates the money gate, and a plan that was never written cannot be
    // read. `scripts/check-economics.mjs` is the same gate as a script.
    if (!existsSync(file)) return skip('slot.economics', 'no economics.json; this slot predates the money gate, so no plan can be read')
    const parsed = parseEconomics(readFileSync(file, 'utf8'))
    if (!parsed.ok) return fail('slot.economics', parsed.errors.join('; '))
    const capabilitiesFile = path.join(slotDir, 'capabilities.json')
    if (!existsSync(capabilitiesFile)) {
      return fail('slot.economics', `the plan declares archetype "${parsed.value.archetype}" but capabilities.json is missing, so the pairing cannot be evaluated`)
    }
    const pairing = validateEconomicsAgainstCapabilities(
      parsed.value,
      JSON.parse(readFileSync(capabilitiesFile, 'utf8').replace(/^\uFEFF/, ''))
    )
    if (!pairing.ok) return fail('slot.economics', pairing.errors.join('; '))
    const reference = String(parsed.value.evidenceRef).split('#')[0]
    if (!existsSync(path.join(ROOT, reference))) {
      return fail('slot.economics', `evidenceRef does not resolve to a file: ${reference}`)
    }
    const { archetype, killCriteria, priceUsd } = parsed.value
    return pass(
      'slot.economics',
      `archetype ${archetype} at $${priceUsd} with the capabilities that can collect it; kill criteria ` +
        `${killCriteria.windowDays}d / ${killCriteria.minInstalls} installs / $${killCriteria.minRevenueUsd}`
    )
  })
}

export function checkBuildConfig(slotDir) {
  return guard('slot.buildConfig', () => {
    const gradle = path.join(slotDir, 'app', 'build.gradle.kts')
    if (!existsSync(gradle)) return unverified('slot.buildConfig', `${relPath(slotDir)} has no app/build.gradle.kts to read`)
    const problems = [...forbiddenFindings(slotDir), ...themeParentFindings(slotDir)]
    // `requireAll: false`: the slot inherits every pin it does not restate, so only the values it
    // declares are compared. Failing it for silence would make this a complaint about the past.
    androidConfigProblems(readFileSync(gradle, 'utf8'), { requireAll: false }).forEach((p) =>
      problems.push(`app/build.gradle.kts: ${p}`)
    )
    const catalog = path.join(slotDir, 'gradle', 'libs.versions.toml')
    if (existsSync(catalog)) {
      catalogPinProblems(readFileSync(catalog, 'utf8')).forEach((p) => problems.push(`gradle/libs.versions.toml: ${p}`))
    }
    if (problems.length > 0) return fail('slot.buildConfig', problems.join('; '))
    return pass('slot.buildConfig', 'no suppressed default, no View-library theme parent, no pin contradicted')
  })
}

/** The build facts that need a slot. The project-wide ones are the three checks above. */
// ---------------------------------------------------------------------------------------------
// The owner's machine is not the factory's scratch space
// ---------------------------------------------------------------------------------------------

/**
 * The one module allowed to decide where a temporary file goes, and the one consumer that has to
 * route through it.
 *
 * The rule and its incident are `.cursor/rules/workspace-hygiene.mdc`: a Gradle build filled the
 * owner's system temp directory on 2026-10-01 and slowed his machine. Every fixture sandbox in this
 * repository did the same thing, and four of them ran on **every** `verify.mjs` invocation. The fix
 * is `SCRATCH_DIR`; these sensors are what stop it from drifting back.
 */
const SCRATCH_MODULE = 'scripts/lib/scratch.mjs'
const SCRATCH_CONSUMER = 'scripts/lib/gradle.mjs'

/**
 * A fixture spelling of a temp-directory call, assembled from two segments.
 *
 * The literal would make this module trip its own sweep in `systemTempFindings` — the same reason
 * `buildFixtures` assembles its foreign package token instead of writing it (see `SENSOR_DATA`).
 */
const TEMP_CALL = `${'tmp'}${'dir'}(`

/** Why a source file must not name the machine's system temp directory. */
export function systemTempProblems(text) {
  return new RegExp(`\\b${'tmp'}${'dir'}\\s*\\(`).test(text)
    ? [`names \`${TEMP_CALL})\`, which resolves the machine's system temp directory outside this repository`]
    : []
}

/** Every file under `dir` that still names the system temp, as `file: reason` strings. */
export function systemTempFindings(dir) {
  if (!existsSync(dir)) return []
  const findings = []
  for (const file of walkSourceFiles(dir, new Set(['.mjs', '.cjs', '.js']))) {
    for (const problem of systemTempProblems(readSource(file))) findings.push(`${relPath(file)}: ${problem}`)
  }
  for (const file of walkSourceFiles(dir, new Set(['.py']))) {
    for (const problem of pythonTempProblems(readSource(file))) findings.push(`${relPath(file)}: ${problem}`)
  }
  return findings
}

/**
 * Why a Python source file must not take the default temp directory.
 *
 * The Python API spells its scratch differently and needs its own sensor: `tempfile.mkdtemp()` and
 * `tempfile.TemporaryDirectory()` accept a `dir`, and *not* passing it is the whole defect. The
 * library name is assembled the same way `TEMP_CALL` is, because this module is itself swept.
 */
export function pythonTempProblems(text) {
  const problems = []
  const api = `${'temp'}${'file'}\\.(?:mkdtemp|TemporaryDirectory|gettempdir)\\(`
  text.split('\n').forEach((line, index) => {
    if (!new RegExp(api).test(line)) return
    if (/dir\s*=\s*SCRATCH_DIR/.test(line)) return
    problems.push(`line ${index + 1}: the ${'temp'}${'file'} API takes the machine's system temp directory; pass \`dir=SCRATCH_DIR\` from scripts/lib/scratch.py`)
  })
  return problems
}

/** What `scripts/lib/scratch.mjs` must keep exporting and enforcing. */
export function scratchModuleProblems(text) {
  const problems = []
  for (const name of ['SCRATCH_DIR', 'scratchDirFor', 'scratchEnv']) {
    if (!new RegExp(`export (?:const|function) ${name}\\b`).test(text)) problems.push(`no longer exports ${name}`)
  }
  for (const key of ['TEMP', 'TMP', 'TMPDIR']) {
    if (!new RegExp(`\\b${key}:`).test(text)) problems.push(`no longer redirects ${key}`)
  }
  if (!/import\.meta\.url/.test(text)) {
    problems.push('no longer derives its root from its own file location, so it could point outside the repository')
  }
  return problems
}

/** What `scripts/lib/gradle.mjs` must keep doing, or the rule is prose again. */
export function scratchConsumerProblems(text) {
  const problems = []
  if (!/from '\.\/scratch\.mjs'/.test(text)) problems.push(`no longer imports ${SCRATCH_MODULE}`)
  if (!/scratchEnv\s*\(/.test(text)) problems.push('no longer builds its spawn environment from scratchEnv(), so a Gradle build writes to the system temp again')
  return problems
}

/** Whether `.gitignore` ignores the scratch directory's home, `.kiln/`. */
export function gitignoreCoversKiln(text) {
  return /^\.kiln\/?\s*$/m.test(text)
}

export function checkScratchHygiene() {
  return guard('build.scratch', () => {
    const problems = []
    const scratch = path.join(ROOT, '.kiln', 'scratch')

    const rel = path.relative(ROOT, scratch)
    if (rel.startsWith('..') || path.isAbsolute(rel)) problems.push(`the scratch directory resolves outside the repository (${rel})`)
    if (!existsSync(scratch)) problems.push('.kiln/scratch/ does not exist, so a spawner that redirects its temp there has nowhere to write')

    const gitignore = path.join(ROOT, '.gitignore')
    if (!existsSync(gitignore)) problems.push('.gitignore is missing')
    else if (!gitignoreCoversKiln(readSource(gitignore))) problems.push('.kiln/ is not gitignored, so scratch could be committed')

    const module = path.join(ROOT, SCRATCH_MODULE)
    if (existsSync(module)) scratchModuleProblems(readSource(module)).forEach((p) => problems.push(`${SCRATCH_MODULE}: ${p}`))
    else problems.push(`${SCRATCH_MODULE} is missing`)

    // The Python half of the same rule. Both are required: the JavaScript half covers the build and
    // every fixture sandbox the checks run, the Python half covers the store pack and its suites.
    const pythonModule = path.join(ROOT, 'scripts', 'lib', 'scratch.py')
    if (!existsSync(pythonModule)) problems.push('scripts/lib/scratch.py is missing, so a Python script has no SCRATCH_DIR to pass')
    else if (!/^SCRATCH_DIR\s*=/m.test(readSource(pythonModule))) {
      problems.push('scripts/lib/scratch.py: no longer defines SCRATCH_DIR')
    }

    const consumer = path.join(ROOT, SCRATCH_CONSUMER)
    if (existsSync(consumer)) scratchConsumerProblems(readSource(consumer)).forEach((p) => problems.push(`${SCRATCH_CONSUMER}: ${p}`))
    else problems.push(`${SCRATCH_CONSUMER} is missing`)

    for (const finding of systemTempFindings(path.join(ROOT, 'scripts'))) {
      problems.push(`${finding} — use SCRATCH_DIR from ${SCRATCH_MODULE}`)
    }

    if (problems.length > 0) return fail('build.scratch', problems.join('; '))
    return pass(
      'build.scratch',
      `every spawner writes under ${SCRATCH_MODULE}'s SCRATCH_DIR, which is gitignored as .kiln/; ` +
        'no module under scripts/ names the system temp directory, and no Python tempfile call omits dir=SCRATCH_DIR'
    )
  })
}

export function buildChecks(slotDir) {
  return [checkIdentityFrozen(slotDir), checkEconomicsPlan(slotDir), checkBuildConfig(slotDir)]
}

// ---------------------------------------------------------------------------------------------
// Fixtures: every sensor above, driven with the value that must make it fire.
// ---------------------------------------------------------------------------------------------

/** A build script that differs from the scaffold only in the line a fixture is testing. */
const SCAFFOLD_LIKE = [
  'android {',
  '    namespace = "com.gexetr.scaffold"',
  '    compileSdk = 37',
  '    buildToolsVersion = "36.0.0"',
  '    defaultConfig { applicationId = "com.gexetr.scaffold"',
  '        minSdk = 26',
  '        targetSdk = 36 }',
  '    buildTypes { release {',
  '        isMinifyEnabled = true',
  '        isShrinkResources = true',
  '        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")',
  '    } }',
  '    compileOptions {',
  '        sourceCompatibility = JavaVersion.VERSION_17',
  '        targetCompatibility = JavaVersion.VERSION_17',
  '    }',
  '    packaging { jniLibs { useLegacyPackaging = false } }',
  '}'
]

/**
 * The sensors this module depends on, made falsifiable.
 *
 * Each case drives a sensor with the value that must make it fire, so deleting the comparison it
 * depends on turns the fixture red (`.cursor/rules/verification-discipline.mdc`: a check that cannot
 * fail is not a check). The foreign package tokens are assembled from two segments, because a
 * literal would make this module trip its own sweep.
 */
export function buildFixtures() {
  return guard('build.fixtures', () => {
    const cases = []
    const foreign = `${['com'].join('')}.${['acme'].join('')}`
    const script = (mutate) => SCAFFOLD_LIKE.map(mutate).join('\n')
    const catalog = (versions, ...plugins) => ['[versions]', 'kotlin = "2.3.21"', ...versions, '[plugins]', ...plugins].join('\n')
    const composePlugin = (ref) => `compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "${ref}" }`

    cases.push([
      'a wrapper that is not the pinned Gradle is refused, the pinned one is accepted, and the wrapper survives a copy',
      () => {
        const wrapper = (version) => `distributionUrl=https\\://services.gradle.org/distributions/gradle-${version}-bin.zip`
        expectTrue(wrapperPinProblems(wrapper('8.0')).length > 0, 'Gradle 8.0 was accepted')
        expectEqual(wrapperPinProblems(wrapper(PINS.gradle)), [], 'the pinned wrapper')
        expectTrue(wrapperPinProblems('distributionUrl=https\\://example.invalid/gradle.zip').length > 0, 'a wrapper with no versioned distribution was accepted')
        // The 4 KB wrapper incident: the wrapper was excluded from a copy and the slot could not build.
        expectTrue(!isExcludedScaffoldPath('gradle/wrapper/gradle-wrapper.jar'), 'the wrapper jar would be dropped')
      }
    ])

    cases.push([
      'the Compose compiler plugin must resolve its version from the kotlin key',
      () => {
        const drifted = catalog(['composeCompiler = "2.0.0"'], composePlugin('composeCompiler'))
        expectTrue(catalogPinProblems(drifted).some((p) => p.includes('compose-compiler')), `a separate Compose compiler version was accepted: ${JSON.stringify(catalogPinProblems(drifted))}`)
        expectTrue(catalogPinProblems(catalog(['kotlin = "2.0.0"'], composePlugin('kotlin'))).some((p) => p.includes('kotlin')), 'a drifted kotlin pin was accepted')
        expectEqual(catalogPinProblems(catalog([], composePlugin('kotlin'))), [], 'the aligned catalog')
      }
    ])

    cases.push([
      'a catalog key that contradicts a pin is refused, per key',
      () => {
        const problems = catalogPinProblems(catalog(['agp = "8.7.0"', 'composeBom = "2026.09.00"']))
        expectEqual(problems.length, 1, 'exactly the agp pin')
        expectTrue(problems[0].includes('8.7.0'), problems[0])
      }
    ])

    cases.push([
      'proguard-android.txt is refused and proguard-android-optimize.txt is not',
      () => {
        const plain = script((line) => line.replace('proguard-android-optimize.txt', 'proguard-android.txt'))
        expectTrue(androidConfigProblems(plain).some((p) => p.includes('proguard-android.txt')), 'the non-optimising default was accepted')
        expectEqual(androidConfigProblems(script((line) => line)), [], 'the scaffold-shaped script')
      }
    ])

    cases.push([
      'legacy JNI packaging is refused',
      () => expectTrue(
        androidConfigProblems(script((line) => line.replace('useLegacyPackaging = false', 'useLegacyPackaging = true'))).some((p) => p.includes('useLegacyPackaging')),
        'useLegacyPackaging = true was accepted'
      )
    ])

    cases.push([
      'a missing or wrong buildToolsVersion pin is refused',
      () => {
        const without = SCAFFOLD_LIKE.filter((line) => !line.includes('buildToolsVersion')).join('\n')
        expectTrue(androidConfigProblems(without).some((p) => p.includes('buildToolsVersion')), 'the absent build-tools pin was accepted')
        const wrong = without.replace('compileSdk = 37', 'compileSdk = 37\n    buildToolsVersion = "35.0.0"')
        expectTrue(androidConfigProblems(wrong).some((p) => p.includes('35.0.0')), 'buildToolsVersion 35.0.0 was accepted')
      }
    ])

    cases.push([
      'a Material Components theme parent is refused and an android:Theme.Material one is not',
      () => {
        expectTrue(themeParentProblems('<style name="Theme.Slot" parent="Theme.Material3.DayNight.NoActionBar">').length > 0, 'Theme.Material3 was accepted')
        expectTrue(themeParentProblems('<style parent="Theme.MaterialComponents.Light">').length > 0, 'Theme.MaterialComponents was accepted')
        expectEqual(themeParentProblems('<style parent="android:Theme.Material.Light.NoActionBar">'), [], 'the android:Theme.Material parent')
      }
    ])

    cases.push([
      'a foreign package token is found in each of the four textual forms',
      () => {
        const forms = [
          `package = "${foreign}.widget"`, // plain
          `pattern = "${foreign.replaceAll('.', '\\.')}\\.widget"`, // regex-escaped
          `schema = "${foreign.replaceAll('.', '\\\\.')}\\.widget"`, // string-escaped
          `path = "app/src/main/java/${foreign.replaceAll('.', '/')}/widget/Main.kt"` // path form
        ]
        for (const form of forms) {
          expectTrue(namespaceTokenProblems(form).some((p) => p.startsWith(foreign)), `the token was not found in: ${form}`)
        }
      }
    ])

    cases.push([
      'apps/<slot>/docs/ is outside the namespace sweep — competitor packages in a cut-out archive are not leftovers',
      () => {
        expectTrue(isSlotDocsPath(path.join(ROOT, 'apps', '2026-10-01-lawn-round-log', 'docs', 'x.md')), 'slot docs path')
        expectTrue(!isSlotDocsPath(path.join(ROOT, 'apps', '2026-10-01-lawn-round-log', 'app', 'x.kt')), 'app source path')
        const verdict = checkNamespaceSweep()
        expectEqual(verdict.verdict, VERDICT.PASS, `verdict (${verdict.detail})`)
        expectTrue(!String(verdict.detail).includes('/docs/'), `detail must not cite slot docs: ${verdict.detail}`)
      }
    ])

    cases.push([
      'a newline escape is not flattened into the token beside it, and the allowlist stays silent',
      () => {
        // The measured incident: stripping every backslash fused the token with the word after two
        // newline escapes and invented a package that appears nowhere.
        expectEqual(namespaceTokenProblems(`package = "${foreign}\\n\\nval grade = 4"`), [`${foreign} (line 1)`], 'findings')
        const clean = [
          `package ${packagePrefix}.widget`,
          `import ${packagePrefix.replaceAll('.', '\\.')}.Widget`,
          `path ${packagePrefix.replaceAll('.', '/')}/Widget.kt`,
          'implementation("com.google.devtools.ksp")',
          'plugins { id("com.android.application") }',
          'import androidx.compose.runtime.Composable',
          // The measured false positive (2026-10-09, the Lumintra slot): a JDK service path named in
          // a comment that documents a packaging exclude is prose, not a package declaration, and the
          // sweep failed two rows over it. The line below is the one that fired; the one under it is
          // the real exclude, which the sweep never matched because a slash precedes `com`.
          '// com.sun.source.util.Plugin is a javac plugin service Orekit never uses at run time',
          'excludes += "META-INF/services/com.sun.source.util.Plugin"'
        ]
        for (const line of clean) expectEqual(namespaceTokenProblems(line), [], `findings for: ${line}`)
      }
    ])

    cases.push([
      'a NUL-bearing file is skipped rather than decoded, and a test-free decode is what the guard detects',
      () => {
        expectEqual(decodeSource(Buffer.from('val grade = 4\n', 'utf8')), 'val grade = 4\n', 'plain text')
        expectEqual(decodeSource(Buffer.from([0x76, 0x61, 0x6c, 0x00, 0x31])), null, 'a NUL-bearing file')
        // The NUL-byte file skipped by a text pass: recognized, and not recognized in a plain decode.
        expectTrue(hasNulGuard('if (bytes.includes(0)) continue'), 'the Node NUL guard')
        expectTrue(hasNulGuard('if b"\\x00" in raw:'), 'the Python NUL guard')
        expectTrue(!hasNulGuard('const text = bytes.toString("utf8")'), 'a decode with no NUL test')
      }
    ])

    cases.push([
      'the copy filter takes the build outputs and keeps the files a slot cannot build without',
      () => {
        for (const probe of EXCLUSION_PROBES) expectTrue(isExcludedScaffoldPath(probe), `${probe} would be copied`)
        for (const survivor of EXCLUSION_SURVIVORS) expectTrue(!isExcludedScaffoldPath(survivor), `${survivor} would be dropped`)
        for (const site of copySites().filter((s) => s.decides)) {
          expectTrue(site.namesPredicate, `${site.rel} decides what to copy without the shared predicate`)
        }
      }
    ])

    cases.push([
      'a fixture sandbox is refused when it resolves the machine system temp, and accepted when it names the project scratch',
      () => {
        const bad = `const sandbox = mkdtempSync(path.join(${TEMP_CALL}), 'kiln-x-')`
        const good = "const sandbox = mkdtempSync(path.join(SCRATCH_DIR, 'kiln-x-'))"
        expectTrue(systemTempProblems(bad).length > 0, `the system-temp form was accepted: ${bad}`)
        expectEqual(systemTempProblems(good), [], 'the SCRATCH_DIR form')
        // The os-preserving variant: `homedir`/`hostname` are not findings, only the temp call is.
        expectEqual(systemTempProblems("import { hostname } from 'node:os'"), [], 'an unrelated node:os import')
      }
    ])

    cases.push([
      'the scratch module keeps exporting a directory, a per-purpose dir, and an environment that redirects all three temp variables',
      () => {
        const complete = [
          'export const SCRATCH_DIR = path.join(ROOT, ".kiln", "scratch")',
          'export function scratchDirFor(name) {}',
          'export function scratchEnv(env = process.env) { return { ...env, TEMP: dir, TMP: dir, TMPDIR: dir } }',
          'const here = fileURLToPath(import.meta.url)'
        ].join('\n')
        expectEqual(scratchModuleProblems(complete), [], 'the complete module')
        for (const missing of ['SCRATCH_DIR', 'scratchDirFor', 'scratchEnv']) {
          const without = complete.replace(new RegExp(`export (const|function) ${missing}\\b`), 'function removed()')
          expectTrue(scratchModuleProblems(without).some((p) => p.includes(missing)), `${missing} could be deleted unnoticed`)
        }
        // Dropping one temp variable is the leak: Java reads TEMP on Windows, TMPDIR elsewhere.
        // Each variable is removed through a pattern that matches the last one too — `TMPDIR: dir }`
        // has no trailing comma, so a `", "`-suffixed replacement would silently no-op and the
        // fixture would pass for the wrong reason (`.cursor/rules/verification-discipline.mdc`).
        for (const key of ['TEMP', 'TMP', 'TMPDIR']) {
          const without = complete.replace(new RegExp(`${key}: dir,? `), '')
          expectTrue(without.includes('scratchEnv(') && !new RegExp(`\\b${key}:`).test(without), `the fixture failed to remove ${key}`)
          expectTrue(scratchModuleProblems(without).some((p) => p.includes(key)), `${key} could stop being redirected`)
        }
        // A root derived from anything but the file's own location can point outside the repository.
        expectTrue(
          scratchModuleProblems(complete.replace('import.meta.url', 'process.cwd()')).some((p) => p.includes('own file location')),
          'a cwd-derived root was accepted'
        )
      }
    ])

    cases.push([
      'gradle stops routing through scratchEnv and the sensor says so',
      () => {
        const wired = "import { scratchEnv } from './scratch.mjs'\nconst spawnEnv = scratchEnv(env)"
        expectEqual(scratchConsumerProblems(wired), [], 'the wired consumer')
        expectTrue(
          scratchConsumerProblems("const spawnEnv = { ...env, TEMP: process.env.TEMP }").some((p) => p.includes('scratchEnv')),
          'a build that rebuilds its own temp environment was accepted'
        )
        expectTrue(
          scratchConsumerProblems("import { scratchEnv } from './elsewhere.mjs'").some((p) => p.includes('scratch.mjs')),
          'an import from another module was accepted'
        )
      }
    ])

    cases.push([
      '.kiln/ has to be ignored by git, or scratch becomes committable',
      () => {
        expectTrue(gitignoreCoversKiln('node_modules/\n.kiln/\n'), 'the scaffold .gitignore')
        expectTrue(gitignoreCoversKiln('.kiln\n'), 'the bare directory form')
        expectTrue(!gitignoreCoversKiln('node_modules/\n*.apk\n'), 'an .gitignore with no .kiln entry')
        expectTrue(!gitignoreCoversKiln('# .kiln/ is mentioned only in a comment\n'), 'a commented-out entry')
      }
    ])

    cases.push([
      'a Python scratch call is refused when it omits dir=SCRATCH_DIR, and accepted when it passes it',
      () => {
        const library = `${'temp'}${'file'}`
        const bad = `    tmp = Path(${library}.mkdtemp(prefix="x"))`
        const good = `    tmp = Path(${library}.mkdtemp(prefix="x", dir=SCRATCH_DIR))`
        const context = `    with ${library}.TemporaryDirectory(prefix="x") as tmp:`
        const contextGood = `    with ${library}.TemporaryDirectory(prefix="x", dir=SCRATCH_DIR) as tmp:`
        expectTrue(pythonTempProblems(bad).length > 0, `the default-parent form was accepted: ${bad}`)
        expectEqual(pythonTempProblems(good), [], 'the SCRATCH_DIR form')
        expectTrue(pythonTempProblems(context).length > 0, 'the context-manager form was accepted')
        expectEqual(pythonTempProblems(contextGood), [], 'the redirected context manager')
        // The finding names the line, because a Python file has no other way to locate it.
        expectTrue(pythonTempProblems(bad)[0].includes('line 1'), pythonTempProblems(bad)[0])
        expectEqual(pythonTempProblems('# a comment about the default temp parent'), [], 'prose with no call')
      }
    ])

    return runFixtures('build.fixtures', cases)
  })
}

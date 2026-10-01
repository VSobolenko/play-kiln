// A settings surface is part of the baseline, not decoration added at release.
//
// The measured defect (owner's review of the first live app, 2026-09-30): `apps/2026-09-26-teacher-gradebook`
// shipped with a working app, a complete store pack and an About screen, and **no place to change
// anything** — no theme, no language, and no preference of any kind. The owner's verdict was that the
// settings screen "should have been added"; his instruction was not to repair that app but to make the
// next ones unable to repeat it. Nothing in the factory read for a settings surface, so every gate
// stayed green: the APK built, the pack validated, the icon measured inside its masks. This module is
// the program that was missing.
//
// What it measures, and what it refuses to claim:
//
//   * A *surface* (`.` a `@Composable` `*Screen`/`*Route` under a settings/preferences package).
//   * A *route* (`.` navigation code references it, and something in the app opens it).
//   * A *language entry point* (`.` a route to the **system** per-app language picker, or an in-app
//     picker built on the published APIs). `android:localeConfig` is already shipped by the scaffold,
//     so the honest requirement is a route to the picker the system already offers, not a second
//     picker that can disagree with it.
//   * A *theme preference* wired into the theme host, an *app-behaviour preference* read by app logic,
//     and *persistence* through the DataStore dependency the scaffold already declares but never used.
//   * The strings the surface names actually exist.
//
// **Boundary, so this module is not a second sensor for a rule someone else owns.** Hardcoded copy is
// `locale.hardcoded` (`scripts/core/localization.mjs`, `.cursor/rules/localization.mdc`); touch
// targets, accessible names and icon labelling are `uiux.a11y` (`scripts/core/uiux.mjs`,
// `.cursor/rules/ui-ux-baseline.mdc`). Nothing here re-checks either — a duplicate is a second thing
// to drift, not extra safety. The one string fact only this module can see is whether a
// `R.string.<name>` a settings screen *names* is declared at all, which is neither a literal nor a
// locale-tree question, so it lives here as `settings.strings`.
//
// It is a **source-level** sensor. It never spawns a process (the core's contract), so it cannot prove
// a preference reaches the pixels, that a switch is reachable by a screen reader, or that the branch
// reading a preference is ever taken. Those are named as gaps in `.cursor/rules/app-settings.mdc`
// rather than implied to be covered.
//
// Applicability is derived, never hand-maintained: the four slots that already exist predate the
// baseline and report their findings as notes, while every slot created from 2026-09-30 is gated.
// `baselineVerdict` is this module's only route to that decision — importing `baselineApplies` as well
// would be a second code path to one answer, which is the duplication this factory removes.
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { SCRATCH_DIR } from '../lib/scratch.mjs'
import path from 'node:path'
import { baselineVerdict, FACTORY_BASELINE } from './baseline.mjs'
// `decodeSource` is the one place the core decides a file is binary; `walkSlotFiles`, `applicationTag`
// and `attributeValue` are the one place it walks a slot and reads its manifest; `stripKotlinComments`
// is the one comment-stripping reader already in the core. All are imported rather than restated,
// because a second copy of a rule is a second rule
// (`.cursor/rules/verification-discipline.mdc`, "One source of truth, imported — never restated").
import { decodeSource } from './build.mjs'
import {
  expectTrue,
  fail,
  guard,
  pass,
  resolveSlotDir,
  ROOT,
  runFixtures,
  unverified
} from './lib.mjs'
import { stripKotlinComments } from './localization.mjs'
import { applicationTag, attributeValue, MANIFEST_REL, walkSlotFiles } from './manifest.mjs'

/** The slot-scoped ids. Each is also run against the scaffold by `settingsProjectChecks()`. */
export const SETTINGS_SLOT_CHECK_IDS = Object.freeze([
  'settings.surface',
  'settings.reachable',
  'settings.language',
  'settings.theme',
  'settings.behaviour',
  'settings.strings',
  'settings.persistence'
])

/** The factory-scoped ids. `settings.documented` exists so the rule cannot drift from the sensor. */
export const SETTINGS_PROJECT_CHECK_IDS = Object.freeze([...SETTINGS_SLOT_CHECK_IDS, 'settings.documented'])

/** The template every slot is copied from. A defect here is a defect in every future slot. */
export const SCAFFOLD_DIR = path.join(ROOT, 'scaffold', 'android-compose')

/** The rule the builder reads. `settings.documented` requires it to name this module and its ids. */
export const SETTINGS_RULE_REL = '.cursor/rules/app-settings.mdc'

const BUILD_RELS = Object.freeze(['app/build.gradle.kts', 'gradle/libs.versions.toml', 'build.gradle.kts'])
const KOTLIN_FILE = /\.kt$/i

// What a settings preference is allowed to be about. The three categories are the floor the rule
// states: a language route, an appearance choice, and something that changes what the app does.
const LANGUAGE_TOKENS = /lang|locale/i
const THEME_TOKENS = /theme|dark|light|appearance|night|contrast/i

// The published entry points for the *system* per-app language picker. `ACTION_APP_LOCALE_SETTINGS`
// was added in API 33 with the feature (verified against the API-33 diff of `android.provider.Settings`);
// `ACTION_APPLICATION_DETAILS_SETTINGS` is older and lands on the app page that hosts the language row.
const SYSTEM_LANGUAGE_ROUTE = /(?:\bSettings\s*\.\s*)?ACTION_(?:APP_LOCALE_SETTINGS|APPLICATION_DETAILS_SETTINGS|LOCALE_SETTINGS)\b/
// The framework picker, API 33+ only: `LocaleManager.applicationLocales = ...`.
const FRAMEWORK_LOCALE_SETTER = /\bapplicationLocales\s*=/
// The AndroidX picker: `AppCompatDelegate.setApplicationLocales(...)`, which the published caution
// says only works when the hosting Activity extends `AppCompatActivity`.
const APPCOMPAT_LOCALE_SETTER = /\bAppCompatDelegate\s*\.\s*setApplicationLocales\b/
// Reimplementing the locale plumbing instead of using the published APIs.
const CUSTOM_LOCALE_PLUMBING = /\bcreateConfigurationContext\s*\(|\bapplyOverrideConfiguration\s*\(/

const NAV_MARKERS = /\bNavDisplay\s*\(|\bNavHost\s*\(|\bNavEntry\s*\(/
// An affordance that opens settings, distinct from the settings screen's own name: a callback, a
// route object, or a destination constant. A `NavEntry` with nothing pointing at it is a route the
// user cannot take.
const SETTINGS_AFFORDANCE =
  /\bon[A-Za-z0-9_]*Settings[A-Za-z0-9_]*\b|\b[A-Za-z0-9_]*Settings(?:Destination|Route)\b|\bDestination\s*\.\s*Settings\b/

const relOf = (dir, file) => path.relative(dir, file).split(path.sep).join('/')
const lineAt = (text, index) => text.slice(0, index).split('\n').length
const isDataPath = (rel) => /(^|\/)data\//i.test(rel)

/**
 * Resolves a slot slug, or a directory relative to the repository root.
 *
 * The slot resolver is imported rather than re-implemented, so an ambiguous slug is refused by the one
 * place that knows how `apps/` is named.
 */
export function resolveSettingsDir(input) {
  if (typeof input !== 'string' || input.trim() === '') {
    throw new Error('a slot directory or slug is required')
  }
  const candidate = path.isAbsolute(input) ? input : path.join(ROOT, input)
  if (existsSync(candidate) && statSync(candidate).isDirectory()) return candidate
  return resolveSlotDir(input)
}

// ---------------------------------------------------------------------------------------------
// Reading Kotlin without being fooled by comments
// ---------------------------------------------------------------------------------------------

/**
 * The comment-stripping reader, imported from the core rather than written a third time.
 *
 * It is the one reader in `scripts/core/` that replaces a comment with spaces, so a line number
 * survives and `file:line` stays a reference a fix can be made from. A comment naming
 * `preferencesDataStore` or `ACTION_APP_LOCALE_SETTINGS` must not count as a use: these factory files
 * discuss the APIs they are about, and a sensor that reads prose as evidence reports its own
 * documentation.
 *
 * `scripts/core/uiux.mjs` (`blankSpans`) and `scripts/core/localization.mjs` carry the same reader.
 * This module imports the export instead of copying it; the honest long-term home is `lib.mjs`, which
 * is outside this change's file allowance — see the note in `.cursor/rules/app-settings.mdc`.
 */
export { stripKotlinComments }

/**
 * Every Kotlin source of a module or slot, comment-stripped, with NUL-bearing files skipped.
 *
 * The walk starts at `app/src/main/` and filters by extension rather than assuming `java/` and
 * `kotlin/`, so a source tree laid out differently is still read. `walkSlotFiles` is the factory's
 * walker: it is the one that knows `build/` must not be descended into, because a stale generated copy
 * of a source file reads exactly like a source file.
 */
export function kotlinSources(dir) {
  const sources = []
  for (const file of walkSlotFiles(path.join(dir, 'app', 'src', 'main'))) {
    if (!KOTLIN_FILE.test(file)) continue
    let text = null
    try {
      text = decodeSource(readFileSync(file))
    } catch {
      continue
    }
    if (text === null) continue
    sources.push({ rel: relOf(dir, file), code: stripKotlinComments(text) })
  }
  return sources.sort((a, b) => a.rel.localeCompare(b.rel))
}

export function hasKotlinSources(dir) {
  return kotlinSources(dir).length > 0
}

/** `{ rel, line, text }` for every match of `pattern` across the sources. Regenerated with `g`. */
function matchSites(sources, pattern) {
  const flags = pattern.flags.includes('g') ? pattern.flags : `${pattern.flags}g`
  const regex = new RegExp(pattern.source, flags)
  const sites = []
  for (const file of sources) {
    for (const match of file.code.matchAll(regex)) {
      sites.push({ rel: file.rel, line: lineAt(file.code, match.index), text: match[0] })
    }
  }
  return sites
}

// ---------------------------------------------------------------------------------------------
// The shapes a settings surface is recognised by
// ---------------------------------------------------------------------------------------------

const SETTINGS_DIR = /(^|\/)(settings|preferences)\//i
const SETTINGS_NAMED_FILE = /(^|\/)[A-Za-z0-9_]*(?:Settings|Preferences)[A-Za-z0-9_]*\.kt$/i

/**
 * The files that make up the settings *surface* (the UI), as opposed to the settings *data*.
 *
 * A `settings/` or `preferences/` directory is a surface; a file merely named `AppSettings.kt` is not,
 * so the data model and the persistence layer are never judged as UI. A file outside such a directory
 * has to declare a composable to count, which keeps `data/SettingsStore.kt` out.
 */
function settingsSurfaceFiles(sources) {
  return sources.filter((file) => {
    if (SETTINGS_DIR.test(file.rel)) return true
    return SETTINGS_NAMED_FILE.test(file.rel) && file.code.includes('@Composable')
  })
}

const FUN_DECL = /\bfun\s+([A-Za-z0-9_]+)\s*\(/g

/** The settings screen composable, or null. `*Screen` and `*Route` are the names a route can host. */
export function settingsScreen(sources) {
  for (const file of settingsSurfaceFiles(sources)) {
    if (!file.code.includes('@Composable')) continue
    for (const match of file.code.matchAll(new RegExp(FUN_DECL.source, 'g'))) {
      const name = match[1]
      if (!/(Settings|Preferences)/i.test(name)) continue
      if (!/(Screen|Route)$/.test(name)) continue
      return { rel: file.rel, name, line: lineAt(file.code, match.index), file }
    }
  }
  return null
}

/**
 * The typed settings values, from a `data class` whose name names Settings or Preferences.
 *
 * The rule is that a toggle has to do something, and 'something' is decided against these values: a
 * value declared here and read nowhere outside the surface is a control that changes nothing. The
 * constructor body is scanned with paren balance rather than a regex, because a default value can
 * contain parentheses (`ThemeMode.System`) and a non-greedy regex would then read the wrong region.
 *
 * A declaration inside a `settings/` or `preferences/` package is a UI-state mirror of the model, not
 * the model: it is used only when nothing outside such a package declares a settings value at all, so
 * a screen holding its own copy of `themeMode` cannot stand in for a persisted setting.
 */
export function declaredSettingsProperties(sources) {
  const properties = []
  const seen = new Set()
  for (const file of sources) {
    const code = file.code
    for (const match of code.matchAll(/\bdata\s+class\s+([A-Za-z0-9_]+)\s*\(/g)) {
      if (!/(Settings|Preferences)/i.test(match[1])) continue
      const start = match.index + match[0].length
      let depth = 1
      let cursor = start
      while (cursor < code.length && depth > 0) {
        if (code[cursor] === '(') depth += 1
        else if (code[cursor] === ')') depth -= 1
        cursor += 1
      }
      const body = code.slice(start, cursor - 1)
      const firstLine = lineAt(code, start)
      body.split(/\r?\n/).forEach((line, offset) => {
        const property = /^\s*(?:val|var)\s+([A-Za-z_][A-Za-z0-9_]*)\s*:/.exec(line)
        if (!property) return
        if (seen.has(property[1])) return
        seen.add(property[1])
        properties.push({ name: property[1], rel: file.rel, line: firstLine + offset })
      })
    }
  }
  const model = properties.filter((property) => !SETTINGS_DIR.test(property.rel))
  return model.length > 0 ? model : properties
}

/** Files outside the settings surface and the data layer that name `identifier` in code. */
function consumersOutside(sources, identifier) {
  const regex = new RegExp(`(?<![A-Za-z0-9_])${identifier}(?![A-Za-z0-9_])`)
  return sources
    .filter((file) => !settingsSurfaceFiles([file]).length && !isDataPath(file.rel))
    .filter((file) => regex.test(file.code))
    .map((file) => file.rel)
}

const THEME_HOST = (file) =>
  /(^|\/)ui\/theme\//.test(file.rel) || /[A-Za-z0-9_]Theme\s*\(/.test(file.code) || /\bisSystemInDarkTheme\s*\(/.test(file.code)

// ---------------------------------------------------------------------------------------------
// The checks. Each returns `{ findings, summary }`; the summary is only read when findings is empty,
// so a passing row never claims more than the check measured.
// ---------------------------------------------------------------------------------------------

function surfaceCheck(sources) {
  const screen = settingsScreen(sources)
  if (screen) {
    return {
      findings: [],
      summary: `a settings surface is declared: ${screen.name}(…) at ${screen.rel}:${screen.line}`
    }
  }
  const shaped = settingsSurfaceFiles(sources).map((file) => file.rel)
  return {
    findings: [
      'no settings screen: nothing under app/src/main/ declares a @Composable settings surface ' +
        '(a *Screen/*Route function in a settings/ or preferences/ file)' +
        (shaped.length > 0 ? `; settings-shaped file(s) present but none declares one: ${shaped.join(', ')}` : '') +
        '. A user with no place to change language, theme or behaviour has an app that was decided for them'
    ],
    summary: ''
  }
}

function reachableCheck(sources) {
  const findings = []
  const screen = settingsScreen(sources)
  const surface = settingsSurfaceFiles(sources)
  const navFiles = sources.filter((file) => !surface.includes(file) && NAV_MARKERS.test(file.code))
  if (screen === null) {
    findings.push(
      'no settings destination is wired into navigation, because no settings screen exists to wire ' +
        `(looked for NavDisplay/NavHost/NavEntry in ${sources.length} source file(s))`
    )
  } else {
    const referenced = navFiles.filter((file) => new RegExp(`\\b${screen.name}\\b`).test(file.code))
    if (referenced.length === 0) {
      findings.push(
        `${screen.rel}:${screen.line}: ${screen.name}(…) is declared, but no navigation entry point ` +
          `references it (${navFiles.length} file(s) declare NavDisplay/NavHost/NavEntry) — the surface ` +
          'exists on disk and nowhere in the app'
      )
    }
    const affordance = sources.filter(
      (file) => !surface.includes(file) && !isDataPath(file.rel) && SETTINGS_AFFORDANCE.test(file.code)
    )
    if (affordance.length === 0) {
      findings.push(
        'nothing in the app opens the settings surface: no settings-named route or callback ' +
          '(on…Settings, …SettingsDestination, Destination.Settings, …SettingsRoute) appears outside the ' +
          'settings files, so a route with no affordance is a screen the user cannot reach'
      )
    }
  }
  return {
    findings,
    summary: screen ? `${screen.name}(…) is a navigation destination opened from ${sources.length} source file(s)` : ''
  }
}

function languageCheck(dir, sources) {
  const findings = []
  const localeConfig = localeConfigSite(dir)
  const systemRoute = matchSites(sources, SYSTEM_LANGUAGE_ROUTE)
  const framework = matchSites(sources, FRAMEWORK_LOCALE_SETTER)
  const appcompat = matchSites(sources, APPCOMPAT_LOCALE_SETTER)

  if (systemRoute.length === 0 && framework.length === 0 && appcompat.length === 0) {
    findings.push(
      localeConfig
        ? `${localeConfig.rel}:${localeConfig.line}: declares android:localeConfig="${localeConfig.value}" ` +
          '(so Android 13+ already offers a per-app language picker for this app), but nothing in the app ' +
          'routes to it: expected Settings.ACTION_APP_LOCALE_SETTINGS (API 33+), or ' +
          'Settings.ACTION_APPLICATION_DETAILS_SETTINGS below it. Declaring the languages and hiding the ' +
          'picker is the defect'
        : 'no language entry point at all: neither a route to the system per-app language settings ' +
          '(Settings.ACTION_APP_LOCALE_SETTINGS / ACTION_APPLICATION_DETAILS_SETTINGS) nor an in-app ' +
          'picker built on the published APIs (AppCompatDelegate.setApplicationLocales / ' +
          'LocaleManager.applicationLocales) exists anywhere in the app'
    )
  }

  // The published caution: on Compose, `setApplicationLocales` only works when the hosting Activity
  // extends AppCompatActivity, and the scaffold's MainActivity extends ComponentActivity.
  if (appcompat.length > 0) {
    const appCompatActivityHosts = matchSites(sources, /\bclass\s+[A-Za-z0-9_]+\s*:\s*AppCompatActivity\s*\(/)
    if (appCompatActivityHosts.length === 0) {
      findings.push(
        `${appcompat[0].rel}:${appcompat[0].line}: AppCompatDelegate.setApplicationLocales is used, and ` +
          'the published caution is that a Compose app must host it in an AppCompatActivity (the ' +
          'startApplicationLocales call otherwise does not apply); no activity in this app extends ' +
          'AppCompatActivity, and androidx.appcompat is not a declared dependency'
      )
    }
  }

  // The framework picker exists only on API 33+. minSdk 26 means an unguarded write silently does
  // nothing on API 26-32 — no crash, no effect, which is the worst shape of failure.
  for (const site of framework) {
    const file = sources.find((candidate) => candidate.rel === site.rel)
    if (file && !/TIRAMISU|SDK_INT\s*>=\s*33/.test(file.code)) {
      findings.push(
        `${site.rel}:${site.line}: LocaleManager.applicationLocales is written without a ` +
          'Build.VERSION.SDK_INT >= TIRAMISU guard; the API exists only on Android 13+, so on API 26-32 ' +
          'this is a silent no-op rather than an error'
      )
    }
  }

  // Two sources of truth for the app's language is the failure the public APIs exist to prevent.
  if (localeConfig) {
    for (const file of sources) {
      if (!/locale/i.test(file.code)) continue
      const plumbing = matchSites([file], CUSTOM_LOCALE_PLUMBING)
      if (plumbing.length > 0) {
        findings.push(
          `${plumbing[0].rel}:${plumbing[0].line}: reimplements the locale plumbing ` +
            `(createConfigurationContext/applyOverrideConfiguration) while android:localeConfig declares ` +
            'per-app language support, so the system picker and the app can disagree about the language'
        )
      }
    }
  }

  const routes = systemRoute.length + framework.length + appcompat.length
  return {
    findings,
    summary:
      routes > 0
        ? `the language entry point exists (${systemRoute.length} system-picker route(s), ` +
          `${framework.length} framework call(s), ${appcompat.length} AndroidX call(s)); ` +
          `android:localeConfig ${localeConfig ? 'is' : 'is not'} declared`
        : ''
  }
}

function themeCheck(sources) {
  const findings = []
  const properties = declaredSettingsProperties(sources)
  if (properties.length === 0) {
    findings.push(
      'no settings model: nothing declares a typed settings value (a data class whose name names ' +
        'Settings or Preferences), so no preference — theme included — can be read back'
    )
    return { findings, summary: '' }
  }
  const themed = properties.filter((property) => THEME_TOKENS.test(property.name))
  if (themed.length === 0) {
    findings.push(
      'no theme preference: the settings surface declares no light/dark/system choice, so the app ' +
        'ships the hand-tuned light and dark palettes it owns with no way for the user to reach either'
    )
    return { findings, summary: '' }
  }
  for (const property of themed) {
    const consumers = consumersOutside(sources, property.name)
    const hosts = sources.filter((file) => THEME_HOST(file) && consumers.includes(file.rel))
    if (hosts.length === 0) {
      findings.push(
        `${property.rel}:${property.line}: the theme preference '${property.name}' is not read by any ` +
          'theme host, so choosing it would change nothing. Expected it to reach a composable that ' +
          'declares or calls a *Theme(...) (or is under ui/theme/), or an activity that passes ' +
          'darkTheme = to one'
      )
    }
  }
  return {
    findings,
    summary: `${themed.map((property) => property.name).join(', ')} wired into the theme host`
  }
}

function behaviourCheck(sources) {
  const findings = []
  const properties = declaredSettingsProperties(sources)
  if (properties.length === 0) {
    findings.push('no settings model: nothing declares a typed settings value, so no behaviour preference can exist')
    return { findings, summary: '' }
  }
  const behaviours = properties.filter(
    (property) => !LANGUAGE_TOKENS.test(property.name) && !THEME_TOKENS.test(property.name)
  )
  if (behaviours.length === 0) {
    findings.push(
      'no app-behaviour preference: every value the settings model declares is about language or ' +
        'appearance, so the surface changes how the app looks and never what it does'
    )
    return { findings, summary: '' }
  }
  for (const property of behaviours) {
    const consumers = consumersOutside(sources, property.name)
    if (consumers.length === 0) {
      findings.push(
        `${property.rel}:${property.line}: '${property.name}' is declared and persisted but never read ` +
          'outside the settings surface and the data layer, so the toggle would do nothing. A control ' +
          'that changes no behaviour is theatre'
      )
    }
  }
  return {
    findings,
    summary: `${behaviours.map((property) => property.name).join(', ')} read by app logic outside the settings surface`
  }
}

function persistenceCheck(dir, sources) {
  const findings = []
  const stores = matchSites(sources, /\bpreferencesDataStore\s*\(/)
  const surface = settingsSurfaceFiles(sources)
  const screen = settingsScreen(sources)
  const writes = matchSites(sources, /\.(?:edit|updateData)\s*\{/)
  const buildText = BUILD_RELS.map((rel) => {
    const file = path.join(dir, ...rel.split('/'))
    if (!existsSync(file)) return null
    try {
      return readFileSync(file, 'utf8')
    } catch {
      return null
    }
  })
    .filter((text) => text !== null)
    .join('\n')
  const declaresDependency = /datastore/i.test(buildText)

  if (stores.length === 0) {
    findings.push(
      declaresDependency
        ? 'the build declares androidx.datastore:datastore-preferences and no preferencesDataStore(...) ' +
          'exists in the app, so the dependency has no consumer and nothing the user chooses survives ' +
          'the process. A declared dependency with zero consumers is how this defect was recorded the ' +
          'first time'
        : 'no persisted settings store: no preferencesDataStore(...) exists, so every choice the ' +
          'settings surface offers lives in memory and is lost when the process dies'
    )
  } else {
    for (const store of stores) {
      if (surface.some((file) => file.rel === store.rel)) {
        findings.push(
          `${store.rel}:${store.line}: preferencesDataStore(...) is declared inside the settings ` +
            'surface; the store belongs to the data layer, exposed through a repository and a ViewModel'
        )
      }
    }
    if (writes.length === 0) {
      findings.push(
        'a preferencesDataStore(...) is declared and nothing writes to it (no edit { } / updateData { }), ' +
          'so the store can only ever return its defaults'
      )
    }
  }

  if (screen) {
    const direct = matchSites([screen.file], /preferencesDataStore\s*\(|\.edit\s*\{|\bDataStore\s*</)
    if (direct.length > 0) {
      findings.push(
        `${direct[0].rel}:${direct[0].line}: the settings screen reads or writes DataStore directly; ` +
          'keep DataStore in the data layer and reach it through a ViewModel'
      )
    }
  }

  if (stores.length > 0) {
    const viewModels = sources.filter((file) =>
      /\bclass\s+[A-Za-z0-9_]+[\s\S]{0,200}?:\s*ViewModel\s*\(/.test(file.code)
    )
    const consumed = viewModels.some((file) => /SettingsStore|DataStore/.test(file.code))
    if (!consumed) {
      findings.push(
        'no ViewModel consumes the settings store, so the composable would have to talk to DataStore ' +
          'itself; the published pattern is repository → ViewModel → collectAsStateWithLifecycle'
      )
    }
  }

  return {
    findings,
    summary: stores.length
      ? `${stores.length} DataStore declaration(s), ${writes.length} write site(s), reached through a ViewModel`
      : ''
  }
}

/**
 * `R.string.<name>` names the settings surface declares that no *default* resource file declares.
 *
 * Only the default configuration counts. A name declared solely under `values-ru/` is Android's
 * `MissingDefaultResource` error, and a screen whose default is missing cannot be translated into any
 * language at all. This is the one string fact no other module owns: `locale.hardcoded` looks for
 * literals and `localeProblems()` compares locale trees, so neither can see a reference to a string
 * that does not exist.
 */
export function stringsCheck(dir, sources) {
  const findings = []
  const surface = settingsSurfaceFiles(sources)
  if (surface.length === 0) {
    findings.push('no settings surface exists, so no string it names was measured')
    return { findings, summary: '' }
  }

  const declared = declaredStringResources(dir)
  const missing = new Map()
  let references = 0
  for (const file of surface) {
    for (const match of file.code.matchAll(/\bR\s*\.\s*string\s*\.\s*([A-Za-z0-9_]+)/g)) {
      references += 1
      const name = match[1]
      if (declared.has(name)) continue
      const entry = missing.get(file.rel) || { line: lineAt(file.code, match.index), names: new Set() }
      entry.names.add(name)
      missing.set(file.rel, entry)
    }
  }
  for (const [rel, entry] of missing) {
    findings.push(
      `${rel}:${entry.line}: the settings surface names string resource(s) that no default resource ` +
        `file declares: ${[...entry.names].sort().join(', ')}. Declare them under ` +
        'app/src/main/res/values/ — a reference to a string nobody declares is as untranslatable as a ' +
        'literal, and only a build would fail on it, which no check here is allowed to run'
    )
  }
  return {
    findings,
    summary: `${references} R.string reference(s) in ${surface.length} settings file(s), every one declared in res/values/`
  }
}

/**
 * The string names the *default* configuration declares (`res/values/*.xml`).
 *
 * Only the default counts. A name declared solely under `values-ru/` is Android's
 * `MissingDefaultResource` error, and a screen whose default is missing cannot be translated into any
 * language at all.
 */
export function declaredStringResources(dir) {
  const names = new Set()
  for (const file of walkSlotFiles(path.join(dir, 'app', 'src', 'main', 'res'))) {
    if (!/\/values\/[^/]*\.xml$/i.test(relOf(dir, file))) continue
    let text = null
    try {
      text = readFileSync(file, 'utf8')
    } catch {
      continue
    }
    for (const match of text.matchAll(/<string\s+name\s*=\s*"([^"]+)"/g)) names.add(match[1])
  }
  return names
}

// ---------------------------------------------------------------------------------------------
// Reading the manifest and the settings model
// ---------------------------------------------------------------------------------------------

/** The `android:localeConfig` declaration's site, or null when the app does not promise one. */
function localeConfigSite(dir) {
  const file = path.join(dir, ...MANIFEST_REL.split('/'))
  if (!existsSync(file)) return null
  let text = null
  try {
    text = readFileSync(file, 'utf8')
  } catch {
    return null
  }
  const application = applicationTag(text)
  if (!application) return null
  const value = attributeValue(application.attributes, 'android:localeConfig')
  if (!value || !/^@xml\//.test(value)) return null
  const attribute = /android:localeConfig\s*=\s*"/.exec(text)
  return {
    rel: MANIFEST_REL,
    line: attribute ? lineAt(text, attribute.index) : application.line,
    value
  }
}

/** Whether the app promises the system a per-app language picker (Android 13+). */
export function declaresLocaleConfig(dir) {
  return localeConfigSite(dir) !== null
}

// ---------------------------------------------------------------------------------------------
// The public surface
// ---------------------------------------------------------------------------------------------

/** The pure sensor: `{ [id]: { findings, summary } }` for one module or slot directory. */
export function settingsAudit(input) {
  const dir = resolveSettingsDir(input)
  const sources = kotlinSources(dir)
  return {
    'settings.surface': surfaceCheck(sources),
    'settings.reachable': reachableCheck(sources),
    'settings.language': languageCheck(dir, sources),
    'settings.theme': themeCheck(sources),
    'settings.behaviour': behaviourCheck(sources),
    'settings.strings': stringsCheck(dir, sources),
    'settings.persistence': persistenceCheck(dir, sources)
  }
}

/** Every finding for one module or slot, as `string[]`. Empty means the sensor found nothing. */
export function settingsFindings(input) {
  const audit = settingsAudit(input)
  return SETTINGS_SLOT_CHECK_IDS.flatMap((id) => audit[id].findings)
}

/**
 * The settings facts about one slot.
 *
 * A slot with no Kotlin source is `UNVERIFIED`, never a pass: nothing scanned must not read as nothing
 * found. Everything else goes through `baselineVerdict`, so a slot that predates `FACTORY_BASELINE`
 * carries its findings as a note instead of a permanent red row.
 */
export function settingsChecks(slotDir) {
  const dir = resolveSettingsDir(slotDir)
  if (!hasKotlinSources(dir)) {
    return SETTINGS_SLOT_CHECK_IDS.map((id) =>
      unverified(id, 'no Kotlin source under app/src/main/ to measure, so the settings surface was not read')
    )
  }
  const audit = settingsAudit(dir)
  return SETTINGS_SLOT_CHECK_IDS.map((id) => {
    const { findings, summary } = audit[id]
    // `baselineVerdict` prints the summary on a non-gating slot too, so an empty one would leave the
    // note reading as a stray semicolon.
    return baselineVerdict(id, dir, findings, summary || `${id} has findings for this slot`)
  })
}

/**
 * The settings facts about the FACTORY: the scaffold every new slot is copied from.
 *
 * The scaffold has no grandfathering — a defect here is copied into every future slot — so a finding is
 * `FAIL`, including the expected red rows while the scaffold's own settings screen is not yet wired into
 * its navigation. The check is not weakened to make it pass; the wiring is the fix.
 */
export function settingsProjectChecks() {
  const checks = []
  if (!existsSync(SCAFFOLD_DIR)) {
    return SETTINGS_PROJECT_CHECK_IDS.map((id) =>
      unverified(id, 'no scaffold/android-compose to measure the factory baseline against')
    )
  }
  const audit = settingsAudit(SCAFFOLD_DIR)
  for (const id of SETTINGS_SLOT_CHECK_IDS) {
    const { findings, summary } = audit[id]
    checks.push(
      findings.length === 0
        ? pass(id, `template: ${summary}`)
        : fail(id, `template: ${findings.join('; ')}`)
    )
  }
  const documented = documentationCheck()
  checks.push(
    documented.findings.length === 0
      ? pass('settings.documented', documented.summary)
      : fail('settings.documented', documented.findings.join('; '))
  )
  return checks
}

/** Why the rule cannot stand on prose alone: it must name the program that enforces it and its ids. */
export function ruleDocProblems(text) {
  const problems = []
  if (!text.includes('scripts/core/settings.mjs')) {
    problems.push(
      'the rule does not name scripts/core/settings.mjs, so a reader cannot find the program that ' +
        'enforces it (a rule only a document asserts is this repository\'s most-recorded failure mode)'
    )
  }
  const missing = SETTINGS_SLOT_CHECK_IDS.filter((id) => !text.includes(id))
  if (missing.length > 0) {
    problems.push(`the rule does not name the check id(s) it is enforced by: ${missing.join(', ')}`)
  }
  return problems
}

function documentationCheck() {
  const file = path.join(ROOT, ...SETTINGS_RULE_REL.split('/'))
  if (!existsSync(file)) {
    return { findings: [`${SETTINGS_RULE_REL} does not exist, so the builder has no rule — only a sensor`], summary: '' }
  }
  let text = null
  try {
    text = readFileSync(file, 'utf8')
  } catch (error) {
    return { findings: [`could not read ${SETTINGS_RULE_REL}: ${error.message}`], summary: '' }
  }
  return {
    findings: ruleDocProblems(text),
    summary:
      `${SETTINGS_RULE_REL} names ${SETTINGS_SLOT_CHECK_IDS.length} check id(s) and the sensor that ` +
      `enforces them (the ${FACTORY_BASELINE} baseline)`
  }
}

// ---------------------------------------------------------------------------------------------
// Fixtures: the sensor driven with the implementation that must make it fire
// ---------------------------------------------------------------------------------------------

const STORE_REL = 'app/src/main/java/com/gexetr/demo/data/SettingsStore.kt'
const SCREEN_REL = 'app/src/main/java/com/gexetr/demo/ui/settings/SettingsScreen.kt'
const VIEWMODEL_REL = 'app/src/main/java/com/gexetr/demo/ui/settings/SettingsViewModel.kt'
const NAV_REL = 'app/src/main/java/com/gexetr/demo/ui/PlayKilnApp.kt'
const ACTIVITY_REL = 'app/src/main/java/com/gexetr/demo/MainActivity.kt'
const HOME_REL = 'app/src/main/java/com/gexetr/demo/ui/home/HomeScreen.kt'

const GOOD_BUILD = 'dependencies {\n    implementation(libs.androidx.datastore.preferences)\n}\n'

const GOOD_MANIFEST = [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<manifest xmlns:android="http://schemas.android.com/apk/res/android">',
  '    <application',
  '        android:allowBackup="true"',
  '        android:localeConfig="@xml/locales_config"',
  '        android:label="@string/app_name">',
  '        <activity android:name=".MainActivity" android:exported="true" />',
  '    </application>',
  '</manifest>',
  ''
].join('\n')

const GOOD_STORE = [
  'package com.gexetr.demo.data',
  '',
  'import androidx.datastore.preferences.core.booleanPreferencesKey',
  'import androidx.datastore.preferences.core.edit',
  'import androidx.datastore.preferences.core.stringPreferencesKey',
  'import androidx.datastore.preferences.preferencesDataStore',
  'import kotlinx.coroutines.flow.Flow',
  'import kotlinx.coroutines.flow.map',
  '',
  'private val Context.settingsStore by preferencesDataStore(name = "settings")',
  '',
  'enum class ThemeMode { System, Light, Dark }',
  '',
  'data class AppSettings(',
  '    val themeMode: ThemeMode = ThemeMode.System,',
  '    val hapticsEnabled: Boolean = true,',
  ')',
  '',
  'private val themeModeKey = stringPreferencesKey("theme_mode")',
  'private val hapticsEnabledKey = booleanPreferencesKey("haptics_enabled")',
  '',
  'class DataStoreSettingsStore(private val context: Context) : SettingsStore {',
  '    override val settings: Flow<AppSettings> = context.settingsStore.data.map { preferences ->',
  '        AppSettings(',
  '            themeMode = ThemeMode.entries.firstOrNull { it.name == preferences[themeModeKey] } ?: ThemeMode.System,',
  '            hapticsEnabled = preferences[hapticsEnabledKey] ?: true,',
  '        )',
  '    }',
  '',
  '    override suspend fun setThemeMode(mode: ThemeMode) {',
  '        context.settingsStore.edit { preferences ->',
  '            preferences[themeModeKey] = mode.name',
  '        }',
  '    }',
  '',
  '    override suspend fun setHapticsEnabled(enabled: Boolean) {',
  '        context.settingsStore.edit { preferences ->',
  '            preferences[hapticsEnabledKey] = enabled',
  '        }',
  '    }',
  '}',
  ''
].join('\n')

const GOOD_SCREEN = [
  'package com.gexetr.demo.ui.settings',
  '',
  'import androidx.compose.foundation.clickable',
  'import androidx.compose.material3.ListItem',
  'import androidx.compose.material3.Switch',
  'import androidx.compose.material3.Text',
  'import androidx.compose.runtime.Composable',
  'import androidx.compose.ui.Modifier',
  'import androidx.compose.ui.res.stringResource',
  'import com.gexetr.demo.R',
  'import com.gexetr.demo.data.ThemeMode',
  '',
  '@Composable',
  'fun SettingsScreen(',
  '    state: SettingsUiState,',
  '    onBack: () -> Unit,',
  '    onThemeModeChange: (ThemeMode) -> Unit,',
  '    onHapticsChange: (Boolean) -> Unit,',
  '    onOpenLanguageSettings: () -> Unit,',
  ') {',
  '    ListItem(',
  '        headlineContent = { Text(stringResource(R.string.settings_language_title)) },',
  '        modifier = Modifier.clickable(onClick = onOpenLanguageSettings),',
  '    )',
  '    ListItem(',
  '        headlineContent = { Text(stringResource(R.string.settings_theme_title)) },',
  '        trailingContent = {',
  '            Switch(checked = state.themeMode == ThemeMode.Dark, onCheckedChange = { dark ->',
  '                onThemeModeChange(if (dark) ThemeMode.Dark else ThemeMode.Light)',
  '            })',
  '        },',
  '    )',
  '    ListItem(',
  '        headlineContent = { Text(stringResource(R.string.settings_haptics_title)) },',
  '        trailingContent = { Switch(checked = state.hapticsEnabled, onCheckedChange = onHapticsChange) },',
  '    )',
  '}',
  '',
  'internal fun openAppLanguageSettings(context: Context) {',
  '    val packageUri = Uri.fromParts("package", context.packageName, null)',
  '    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {',
  '        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(packageUri)',
  '    } else {',
  '        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri)',
  '    }',
  '    runCatching { context.startActivity(intent) }',
  '}',
  ''
].join('\n')

const GOOD_VIEWMODEL = [
  'package com.gexetr.demo.ui.settings',
  '',
  'import androidx.lifecycle.ViewModel',
  'import androidx.lifecycle.ViewModelProvider',
  'import androidx.lifecycle.viewModelScope',
  'import com.gexetr.demo.data.AppSettings',
  'import com.gexetr.demo.data.SettingsStore',
  'import com.gexetr.demo.data.ThemeMode',
  'import kotlinx.coroutines.flow.SharingStarted',
  'import kotlinx.coroutines.flow.StateFlow',
  'import kotlinx.coroutines.flow.stateIn',
  'import kotlinx.coroutines.launch',
  '',
  'data class SettingsUiState(',
  '    val themeMode: ThemeMode = ThemeMode.System,',
  '    val hapticsEnabled: Boolean = true,',
  ')',
  '',
  'class SettingsViewModel(private val store: SettingsStore) : ViewModel() {',
  '    val settings: StateFlow<AppSettings> = store.settings.stateIn(',
  '        scope = viewModelScope,',
  '        started = SharingStarted.WhileSubscribed(5_000),',
  '        initialValue = AppSettings(),',
  '    )',
  '',
  '    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }',
  '',
  '    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { store.setHapticsEnabled(enabled) }',
  '',
  '    companion object {',
  '        fun factory(store: SettingsStore): ViewModelProvider.Factory =',
  '            object : ViewModelProvider.Factory {',
  '                @Suppress("UNCHECKED_CAST")',
  '                override fun <T : ViewModel> create(modelClass: Class<T>): T =',
  '                    SettingsViewModel(store) as T',
  '            }',
  '    }',
  '}',
  ''
].join('\n')

const GOOD_NAV = [
  'package com.gexetr.demo.ui',
  '',
  'import androidx.compose.runtime.Composable',
  'import androidx.navigation3.runtime.NavEntry',
  'import androidx.navigation3.ui.NavDisplay',
  'import com.gexetr.demo.data.AppSettings',
  'import com.gexetr.demo.ui.home.HomeScreen',
  'import com.gexetr.demo.ui.settings.SettingsScreen',
  '',
  'private sealed interface Destination {',
  '    data object Home : Destination',
  '    data object SettingsDestination : Destination',
  '}',
  '',
  '@Composable',
  'fun PlayKilnApp(settings: AppSettings) {',
  '    NavDisplay(',
  '        backStack = backStack,',
  '        onBack = { pop() },',
  '        entryProvider = { key ->',
  '            when (key) {',
  '                Destination.Home -> NavEntry(key) {',
  '                    HomeScreen(settings = settings, onNavigateToSettings = { push(Destination.SettingsDestination) })',
  '                }',
  '                Destination.SettingsDestination -> NavEntry(key) {',
  '                    SettingsScreen(onBack = { pop() })',
  '                }',
  '            }',
  '        },',
  '    )',
  '}',
  ''
].join('\n')

const GOOD_ACTIVITY = [
  'package com.gexetr.demo',
  '',
  'import androidx.activity.ComponentActivity',
  'import androidx.compose.foundation.isSystemInDarkTheme',
  'import com.gexetr.demo.data.AppSettings',
  'import com.gexetr.demo.ui.PlayKilnApp',
  'import com.gexetr.demo.ui.theme.PlayKilnTheme',
  '',
  'class MainActivity : ComponentActivity() {',
  '    private fun render(settings: AppSettings) {',
  '        setContent {',
  '            PlayKilnTheme(darkTheme = settings.themeMode.resolve(isSystemInDarkTheme())) {',
  '                PlayKilnApp(settings = settings)',
  '            }',
  '        }',
  '    }',
  '}',
  ''
].join('\n')

const GOOD_STRINGS = [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<resources>',
  '    <string name="settings_language_title" translatable="true">App language</string>',
  '    <string name="settings_theme_title" translatable="true">Theme</string>',
  '    <string name="settings_haptics_title" translatable="true">Haptic feedback</string>',
  '</resources>',
  ''
].join('\n')

const GOOD_HOME = [
  'package com.gexetr.demo.ui.home',
  '',
  'import androidx.compose.material3.Text',
  'import androidx.compose.runtime.Composable',
  'import com.gexetr.demo.data.AppSettings',
  '',
  '@Composable',
  'fun HomeScreen(settings: AppSettings, onNavigateToSettings: () -> Unit) {',
  '    if (settings.hapticsEnabled) {',
  '        onNavigateToSettings()',
  '    }',
  '    Text(text = "home")',
  '}',
  ''
].join('\n')

/** A complete, correct settings surface. Every fixture below is this tree with one thing broken. */
const GOOD_FILES = Object.freeze({
  'app/build.gradle.kts': GOOD_BUILD,
  'gradle/libs.versions.toml': 'datastore = "1.2.1"\n',
  [MANIFEST_REL]: GOOD_MANIFEST,
  'app/src/main/res/values/strings.xml': GOOD_STRINGS,
  [STORE_REL]: GOOD_STORE,
  [SCREEN_REL]: GOOD_SCREEN,
  [VIEWMODEL_REL]: GOOD_VIEWMODEL,
  [NAV_REL]: GOOD_NAV,
  [ACTIVITY_REL]: GOOD_ACTIVITY,
  [HOME_REL]: GOOD_HOME
})

function withFixtureDir(files, body) {
  // `manifest.mjs` owns a private copy of this helper and exports no predicate for it; a fixture tree
  // is not a rule, so writing it here rather than editing a frozen module is the honest trade.
  const dir = mkdtempSync(path.join(SCRATCH_DIR, 'kiln-settings-'))
  try {
    for (const [rel, contents] of Object.entries(files)) {
      if (contents === null) continue
      const file = path.join(dir, ...rel.split('/'))
      mkdirSync(path.dirname(file), { recursive: true })
      writeFileSync(file, contents, 'utf8')
    }
    return body(dir)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
}

const withGood = (overrides, body) => withFixtureDir({ ...GOOD_FILES, ...overrides }, body)
const findingsFor = (dir, id) => settingsAudit(dir)[id].findings

function expectFinding(id, overrides, phrase) {
  const findings = withGood(overrides, (dir) => findingsFor(dir, id))
  expectTrue(findings.length > 0, `${id}: expected a finding for ${phrase || 'the wrong implementation'}`)
  if (phrase) {
    expectTrue(
      findings.join(' ').includes(phrase),
      `${id}: findings did not name ${phrase}: ${findings.join(' | ')}`
    )
  }
}

/**
 * The sensors this module depends on, made falsifiable.
 *
 * Every case breaks exactly one thing in an otherwise correct settings surface and asserts the check
 * fires with an actionable message. A case that cannot fail is not a check
 * (`.cursor/rules/verification-discipline.mdc`), which is why the good tree is asserted clean first:
 * a sensor that fires on everything is as useless as one that fires on nothing.
 */
export function settingsFixtures() {
  return [
    guard('settings.fixtures', () => {
      const cases = []

      cases.push([
        'the correct settings surface produces no finding at all',
        () => {
          const findings = withGood({}, (dir) => settingsFindings(dir))
          expectTrue(findings.length === 0, `the good tree produced findings: ${findings.join(' | ')}`)
        }
      ])

      // ---- settings.surface ----
      cases.push([
        'a module with no settings screen fails, and so does one whose screen is not a *Screen/*Route',
        () => {
          expectFinding('settings.surface', { [SCREEN_REL]: null }, 'no settings screen')
          expectFinding(
            'settings.surface',
            { [SCREEN_REL]: GOOD_SCREEN.replace('fun SettingsScreen(', 'fun SettingsPanel(') },
            'no settings screen'
          )
          expectTrue(
            withGood({}, (dir) => findingsFor(dir, 'settings.surface')).length === 0,
            'the good tree reported a missing settings screen'
          )
        }
      ])

      // ---- settings.reachable ----
      cases.push([
        'a settings screen no navigation entry references fails',
        () => {
          expectFinding('settings.reachable', { [NAV_REL]: null }, 'no navigation entry point references it')
        }
      ])

      cases.push([
        'a settings screen composed with no settings route or callback fails, even when navigation names it',
        () => {
          // The route still resolves to the screen through an inline `NavEntry`, and `HomeScreen` no
          // longer offers a way in: the surface exists and nothing can reach it.
          const orphaned = [
            'package com.gexetr.demo.ui',
            '',
            'import androidx.compose.runtime.Composable',
            'import androidx.navigation3.runtime.NavEntry',
            'import androidx.navigation3.ui.NavDisplay',
            'import com.gexetr.demo.data.AppSettings',
            'import com.gexetr.demo.ui.home.HomeScreen',
            'import com.gexetr.demo.ui.settings.SettingsScreen',
            '',
            'private sealed interface Destination {',
            '    data object Home : Destination',
            '}',
            '',
            '@Composable',
            'fun PlayKilnApp(settings: AppSettings) {',
            '    NavDisplay(',
            '        backStack = backStack,',
            '        onBack = { pop() },',
            '        entryProvider = { key ->',
            '            when (key) {',
            '                Destination.Home -> NavEntry(key) { HomeScreen(settings = settings, onOpenPanel = {}) }',
            '            }',
            '        },',
            '    )',
            '}',
            '',
            '@Composable',
            'fun SettingsPanel() = SettingsScreen(onBack = {})',
            ''
          ].join('\n')
          const broken = { [NAV_REL]: orphaned, [HOME_REL]: GOOD_HOME.replaceAll('onNavigateToSettings', 'onOpenMenu') }
          expectTrue(
            withGood(broken, (dir) => findingsFor(dir, 'settings.surface').length === 0),
            'this fixture must isolate the affordance: the settings screen is still declared'
          )
          expectTrue(
            withGood(broken, (dir) => !findingsFor(dir, 'settings.reachable').some((finding) => finding.includes('no navigation entry point'))),
            'this fixture must isolate the affordance: the navigation entry still references the screen'
          )
          expectFinding('settings.reachable', broken, 'nothing in the app opens the settings surface')
        }
      ])

      // ---- settings.language ----
      cases.push([
        'an app that declares android:localeConfig with no route to the system picker fails',
        () => {
          expectFinding(
            'settings.language',
            { [SCREEN_REL]: GOOD_SCREEN.replace(/openAppLanguageSettings[\s\S]*$/, '') },
            'android:localeConfig'
          )
        }
      ])

      cases.push([
        'an unguarded LocaleManager write fails, because it silently no-ops below API 33',
        () => {
          const unguarded = GOOD_ACTIVITY.replace(
            '    }',
            '    }\n\n    private fun pick(context: Context) {\n        context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.getDefault()\n    }'
          )
          expectFinding('settings.language', { [ACTIVITY_REL]: unguarded }, 'TIRAMISU guard')
        }
      ])

      cases.push([
        'AppCompatDelegate.setApplicationLocales in a non-AppCompatActivity host fails',
        () => {
          const appcompat = GOOD_ACTIVITY.replace(
            '    }',
            '    }\n\n    private fun pick() {\n        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))\n    }'
          )
          expectFinding('settings.language', { [ACTIVITY_REL]: appcompat }, 'AppCompatActivity')
        }
      ])

      cases.push([
        'reimplementing the locale plumbing beside android:localeConfig fails',
        () => {
          const plumbing = GOOD_ACTIVITY.replace(
            '    }',
            '    }\n\n    private fun locale(base: Context) = base.createConfigurationContext(Configuration())'
          )
          expectFinding('settings.language', { [ACTIVITY_REL]: plumbing }, 'reimplements the locale plumbing')
        }
      ])

      // ---- settings.theme ----
      cases.push([
        'no theme preference fails, and a theme preference nothing consumes fails',
        () => {
          expectFinding('settings.theme', { [STORE_REL]: GOOD_STORE.replace('    val themeMode: ThemeMode = ThemeMode.System,\n', '') }, 'no theme preference')
          expectFinding('settings.theme', { [ACTIVITY_REL]: null }, "not read by any theme host")
        }
      ])

      // ---- settings.behaviour ----
      cases.push([
        'no app-behaviour preference fails, and one nothing reads fails',
        () => {
          expectFinding(
            'settings.behaviour',
            { [STORE_REL]: GOOD_STORE.replace('    val hapticsEnabled: Boolean = true,\n', '') },
            'no app-behaviour preference'
          )
          expectFinding('settings.behaviour', { [HOME_REL]: null }, 'never read outside the settings surface')
        }
      ])

      // ---- settings.persistence ----
      cases.push([
        'the declared DataStore dependency with no engine fails, and so does a store nothing writes to',
        () => {
          expectFinding('settings.persistence', { [STORE_REL]: null }, 'has no consumer')
          expectFinding(
            'settings.persistence',
            { [STORE_REL]: GOOD_STORE.replace(/    override suspend fun set[A-Za-z]+\([^)]*\) \{[\s\S]*?\n    \}\n/g, '') },
            'nothing writes to it'
          )
        }
      ])

      cases.push([
        'a DataStore declared inside the settings surface fails, and so does a store no ViewModel reaches',
        () => {
          expectFinding(
            'settings.persistence',
            { [STORE_REL]: null, [SCREEN_REL]: `${GOOD_SCREEN}\nprivate val Context.inline by preferencesDataStore(name = "settings")\n` },
            'declared inside the settings surface'
          )
          expectFinding('settings.persistence', { [VIEWMODEL_REL]: null }, 'no ViewModel consumes the settings store')
        }
      ])

      // ---- settings.strings ----
      cases.push([
        'a reference to a string nobody declares fails, and a locale-only declaration does not satisfy it',
        () => {
          expectTrue(
            withGood({}, (dir) => findingsFor(dir, 'settings.strings').length === 0),
            'the good tree produced a string finding'
          )
          expectFinding(
            'settings.strings',
            { [SCREEN_REL]: GOOD_SCREEN.replaceAll('R.string.settings_theme_title', 'R.string.settings_theme_typo') },
            'no default resource file declares'
          )
          // A name declared only in a locale variant is Android's missing-default-resource error, so
          // it must not satisfy the check.
          expectFinding(
            'settings.strings',
            {
              'app/src/main/res/values/strings.xml': GOOD_STRINGS.replace(
                '    <string name="settings_theme_title" translatable="true">Theme</string>\n',
                ''
              ),
              'app/src/main/res/values-ru/strings.xml':
                '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="settings_theme_title" translatable="true">Тема</string>\n</resources>\n'
            },
            'no default resource file declares'
          )
        }
      ])

      // ---- settings.documented, and the comment-stripping reader it and every check depend on ----
      cases.push([
        'a rule that names neither the sensor nor its ids fails, and the real rule passes',
        () => {
          expectTrue(
            ruleDocProblems('A settings screen should exist.') .some((problem) => problem.includes('scripts/core/settings.mjs')),
            'a rule naming no sensor was accepted'
          )
          expectTrue(
            ruleDocProblems('settings.surface').some((problem) => problem.includes('scripts/core/settings.mjs')),
            'a rule naming no sensor was accepted'
          )
          const declared = `Enforced by scripts/core/settings.mjs: ${SETTINGS_SLOT_CHECK_IDS.join(', ')}`
          expectTrue(ruleDocProblems(declared).length === 0, `the naming rule was refused: ${ruleDocProblems(declared).join(' | ')}`)
        }
      ])

      cases.push([
        'a commented-out API is not a use, and a string containing // is not a comment',
        () => {
          expectTrue(
            !stripKotlinComments('// preferencesDataStore(name = "x")\n').includes('preferencesDataStore'),
            'an API named only in a comment survived the stripper'
          )
          expectTrue(
            stripKotlinComments('val url = "https://example.invalid/a" // tail\n').includes('https://example.invalid/a'),
            'a URL inside a string literal was treated as a comment'
          )
          const blockStripped = stripKotlinComments('/* AppSettings(themeMode) */\nval url = 1\n')
          expectTrue(
            !blockStripped.includes('AppSettings') && blockStripped.split('\n').length === 3,
            'a block comment was not replaced by whitespace on the same lines'
          )
          // The end-to-end consequence: a preference that only exists in a comment is not a
          // preference, so a surface that merely documents a theme is not scored as offering one.
          expectTrue(
            withGood(
              {
                [STORE_REL]: GOOD_STORE.replace(
                  '    val themeMode: ThemeMode = ThemeMode.System,\n',
                  '    // val themeMode: ThemeMode = ThemeMode.System,\n'
                )
              },
              (dir) => findingsFor(dir, 'settings.theme').some((finding) => finding.includes('no theme preference'))
            ),
            'a theme preference declared only as a comment was counted as the surface offering a theme'
          )
        }
      ])

      return runFixtures('settings.fixtures', cases)
    })
  ]
}

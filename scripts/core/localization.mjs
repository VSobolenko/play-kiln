// Localization as a design input: the shape a slot is born in, and the two things
// `slot.locales` deliberately does not judge.
//
// Measured this session (2026-09-30), before a line of this module was written, because the
// obvious hypothesis was already covered and the open gap had to be found rather than assumed:
//
//   1. "locales_config.xml and res/values-<locale>/ agree in both directions" — COVERED.
//      `scripts/core/policy.mjs` `localeProblems()` reports both `locales_config.xml offers
//      '<lang>' but res/values-<lang>/ does not exist` and `res/<dir>/ exists but
//      locales_config.xml does not declare '<lang>'`, and `check-locales.mjs`'s own lines
//      247-268 say the same. Restating either here would be a duplicate check, which is this
//      repository's most-recorded failure mode.
//   2. "a translation is complete, not merely present" — COVERED. `localeProblems()` fails on
//      `is missing '<key>' (from values/<source>), so it falls back to English` and on a locale
//      key `which values/ no longer has`, per key, plus format-argument and plural-category
//      parity. `node scripts/check-locales.mjs --slot teacher-gradebook` answers
//      `PASS locales - 11 translated language(s) x 518 key(s) complete`.
//   3. "a translation is not a machine placeholder" — OPEN. A search of `scripts/` for
//      `en-XA|ar-XB|pseudo|machine.translat|placeholder` found no locale-shaped sensor; the one
//      plant-marker test (`HomeViewModelTest.shippedCopyCarriesNoScaffoldMarkers`) is a slot's
//      own unit test, not a factory check.
//   4. "the app is born multilingual" — OPEN, and this is the gap the owner's sentence names.
//      `localeProblems()` is explicitly honest about zero additional languages: its own fixture
//      is `a slot that declares no additional language is reported honestly, not as a defect`.
//      So the scaffold ships `values/` alone, `locales_config.xml` declares `en` alone, and a
//      brand-new slot is monolingual by construction — localization then arrives as a catch-up
//      task, which is the ordering the owner objects to.
//
// Rule 4 is the defect; rule 3 is the trap it leads to (paste a machine translation to make the
// tree exist). Both live here, and neither restates rule 1 or 2.
//
// Two scoping rules this module obeys:
//   - Applicability is derived, never hand-maintained. `baselineVerdict` from `./baseline.mjs`
//     already answers "not a product slot / killed slot / created before the 2026-09-30
//     baseline", so `apps/2026-09-22-smoke` and `apps/2026-09-23-pipe-proof` (pipeline proofs
//     with no `economics.json`) carry a note instead of a permanent red row.
//   - Everything already owned elsewhere is imported, not re-implemented: the language-tag
//     vocabulary and the XML key reader come from `./policy.mjs`, the file walker and the
//     NUL-byte guard come from `./manifest.mjs` and `./build.mjs`.
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { SCRATCH_DIR } from '../lib/scratch.mjs'
import path from 'node:path'
import { baselineVerdict } from './baseline.mjs'
import { decodeSource } from './build.mjs'
import { expectEqual, expectTrue, fail, guard, pass, ROOT, runFixtures, unverified } from './lib.mjs'
import { walkSlotFiles } from './manifest.mjs'
import { declaredLanguages, listingLanguage, localeDirNames, resourceEntries } from './policy.mjs'

/** The check ids this module owns, in report order. Each is deliberately narrower than `slot.locales`. */
export const LOCALIZATION_IDS = Object.freeze([
  'locale.multilingual', // the shape: a slot declares and ships more than its default language
  'locale.placeholder', // the trap: the tree exists but the text is not a translation
  'locale.hardcoded', // the prerequisite: user-facing copy reaches a resource at all
  'locale.pseudolocale' // the hygiene: a pseudolocale is a device option, never a shipped language
])

const RES_REL = 'app/src/main/res'
const SCRATCH_PREFIX = 'kiln-localization-'

/** Below this many shared values a clone is a coincidence rather than a copied file. */
const CLONE_MIN_KEYS = 3
/** Above this share of verbatim-identical values a translation is worth a human look, not a failure. */
const PLACEHOLDER_RATIO = 0.8
const PLACEHOLDER_MIN_SHARED = 5

const relFrom = (base, file) => path.relative(base, file).split(path.sep).join('/')
const dedupe = (values) => [...new Set(values)]

// ---------------------------------------------------------------------------------------------
// locale.multilingual — the shape a slot is born in
// ---------------------------------------------------------------------------------------------

/**
 * The languages a slot declares beyond the one `values/` already speaks.
 *
 * `values/` is the fallback for every locale, so the default language has no `values-<lang>/` of
 * its own; demanding one would demand a duplicate of the default. That is the same skip
 * `localeProblems()` makes, quoted rather than re-derived from a second rule.
 */
export function additionalLanguages(declared, fallback) {
  return dedupe(declared).filter((language) => language !== fallback)
}

/**
 * The shape findings: does this tree exist in more than one language at all?
 *
 * A slot with one language is not a slot with a translation bug; it is a slot that has not started
 * localizing and can only localize later. That is the state `localeProblems()` calls honest and
 * this check calls a defect, and the difference is not a contradiction: the older check answers
 * "is what is declared true", this one answers "is more than one language declared at all".
 *
 * Key-level coverage is deliberately *not* measured here — `slot.locales` owns it, and the fixture
 * `a translation missing half its keys is slot.locales' business, not this module's` pins that
 * boundary so this check cannot grow into a duplicate.
 */
export function multilingualProblems({ declared = [], dirs = {}, localeFiles = {}, fallback = 'en' }) {
  const additional = additionalLanguages(declared, fallback)
  if (additional.length === 0) {
    return [
      `locales_config.xml declares only the default language '${fallback}', so the slot is monolingual; ` +
        'a slot is born multilingual — at least one additional language, declared and shipped with a ' +
        'res/values-<locale>/ tree, exists from the first commit, because translation delivered later is ' +
        'the ordering this rule exists to prevent'
    ]
  }
  const problems = []
  for (const language of additional) {
    const directory = dirs[language]
    if (!directory) {
      problems.push(
        `locales_config.xml declares '${language}' but the scaffold carries no res/values-${language}/ ` +
          'directory, so the shipped shape advertises a language the tree cannot answer in'
      )
      continue
    }
    if (resourceEntries(localeFiles[language] || []).resources.size === 0) {
      problems.push(`res/${directory}/ carries no strings, so '${language}' is declared but not shipped`)
    }
  }
  return problems
}

// ---------------------------------------------------------------------------------------------
// locale.placeholder — the tree exists, but is it a translation?
// ---------------------------------------------------------------------------------------------

/**
 * A value that is exactly a translation sentinel, or the whole value bracketed around one.
 *
 * Deliberately anchored at both ends. A substring rule would fire on a real string such as
 * "Machine translation is off" in a settings screen, and a detector that fires on correct copy is
 * a detector the first author it annoys switches off — the same reasoning
 * `scripts/lib/client-secrets.mjs` uses to downgrade a documented placeholder to a note.
 */
const EXACT_SENTINEL = /^(?:[<([{]\s*)?(?:todo|fixme|tbd|translate|translated|translation|placeholder|untranslated)\s*(?:[>)\]}]|!*)$/i

/** Markers that cannot appear in a genuine translation of anything. */
const SUBSTRING_MARKERS = [
  [/\uFFFD/, 'U+FFFD (an unencodeable character, so the value was mangled before it was stored)'],
  [/lorem ipsum/i, 'Lorem ipsum (filler text)'],
  [/(?:^|\s)X{3,}(?:\s|$)/, 'XXX (a placeholder marker)']
]

/** The placeholder marker in one value, or null when the value looks like a real translation. */
export function translationPlaceholder(body) {
  const text = String(body ?? '').trim()
  if (EXACT_SENTINEL.test(text)) return `the sentinel '${text}'`
  for (const [pattern, label] of SUBSTRING_MARKERS) if (pattern.test(text)) return label
  return null
}

const normalize = (text) => String(text ?? '').replace(/\s+/g, ' ').trim()

/**
 * How much of a locale is a verbatim copy of the default.
 *
 * Only keys present in both are compared, so a half-finished translation is measured on what it
 * actually claims. A full clone is the thing Play's automatic string translation cannot produce
 * (it produces *different* text) and the thing a hurried author does by copying `values/` and
 * intending to come back — which is why it is a finding, while a partial overlap is only a note.
 */
export function cloneAnalysis(base, local) {
  const shared = []
  for (const [key, source] of base) {
    const target = local.get(key)
    if (target && source.kind === target.kind) shared.push([key, source, target])
  }
  if (shared.length === 0) return { shared: 0, identical: 0, ratio: 0, clone: false, keys: [] }
  const identical = shared.filter(([, source, target]) => normalize(source.body) === normalize(target.body))
  return {
    shared: shared.length,
    identical: identical.length,
    ratio: identical.length / shared.length,
    clone: shared.length >= CLONE_MIN_KEYS && identical.length === shared.length,
    keys: identical.slice(0, 3).map(([key]) => key)
  }
}

export function placeholderProblems({ baseFiles = [], localeFiles = {}, dirs = {}, declared = [], fallback = 'en' }) {
  const problems = []
  const notes = []
  const base = resourceEntries(baseFiles).resources
  const translated = Object.entries(dirs).filter(([language]) => additionalLanguages(declared, fallback).includes(language))

  for (const [language, directory] of translated) {
    const local = resourceEntries(localeFiles[language] || []).resources
    // An empty directory is `slot.locales`'s finding ("contains no strings"); a second message for
    // it would be noise, so this check stays silent and lets the owner of that rule report it.
    if (local.size === 0) continue

    for (const [key, entry] of local) {
      const marker = translationPlaceholder(entry.body)
      if (marker) {
        problems.push(
          `res/${directory}/ '${key}' contains ${marker}, so the value is a placeholder rather than a ` +
            'translation — a user in this language reads the placeholder'
        )
      }
    }

    const clone = cloneAnalysis(base, local)
    if (clone.clone) {
      problems.push(
        `res/${directory}/ is byte-identical to values/ for all ${clone.shared} shared value(s) ` +
          `(including '${clone.keys.join("', '")}'), so these are copies of the English rather than translations`
      )
    } else if (clone.ratio >= PLACEHOLDER_RATIO && clone.shared >= PLACEHOLDER_MIN_SHARED) {
      notes.push(
        `res/${directory}/ shares ${Math.round(clone.ratio * 100)}% of its ${clone.shared} shared value(s) ` +
          'verbatim with the default — confirm this is a translation and not a paste'
      )
    }
  }
  return { problems, notes }
}

// ---------------------------------------------------------------------------------------------
// locale.hardcoded — the prerequisite the pseudolocale test depends on
// ---------------------------------------------------------------------------------------------

/**
 * A Kotlin source with its comments blanked out, at identical length and identical line numbers.
 *
 * Offsets are preserved by replacing comment characters with spaces rather than deleting them, so
 * a finding can still report `<file>:<line>`. A comment that mentions `Text("...")` in prose must
 * not become a finding, which is the defect this reader exists for; the alternative, a regex over
 * the raw file, reads a doc comment as code.
 */
export function stripKotlinComments(source) {
  const out = source.split('')
  const blank = (from, to) => {
    for (let index = from; index < to; index += 1) if (out[index] !== '\n') out[index] = ' '
  }
  let index = 0
  let mode = 'code'
  while (index < source.length) {
    const char = source[index]
    if (mode === 'code') {
      if (char === '/' && source[index + 1] === '/') {
        const start = index
        index += 2
        while (index < source.length && source[index] !== '\n') index += 1
        blank(start, index)
        continue
      }
      if (char === '/' && source[index + 1] === '*') {
        const start = index
        index += 2
        while (index < source.length && !(source[index] === '*' && source[index + 1] === '/')) index += 1
        index = Math.min(index + 2, source.length)
        blank(start, index)
        continue
      }
      if (source.startsWith('"""', index)) {
        mode = 'raw'
        index += 3
        continue
      }
      if (char === '"') mode = 'string'
      index += 1
      continue
    }
    if (mode === 'raw') {
      if (source.startsWith('"""', index)) {
        mode = 'code'
        index += 3
      } else index += 1
      continue
    }
    if (char === '\\') {
      index += 2
      continue
    }
    if (char === '"') mode = 'code'
    index += 1
  }
  return out.join('')
}

/** A literal passed straight to Compose, where a resource is required. */
const LITERAL_SITES = [
  { pattern: /\bText\s*\(\s*"((?:[^"\\]|\\.)*)"/g, what: 'Text(...)' },
  { pattern: /\b(?:contentDescription|cd)\s*=\s*"((?:[^"\\]|\\.)*)"/g, what: 'a content description' }
]

/**
 * Whether a literal is user-facing prose rather than a token.
 *
 * Precision over recall on purpose. A rule that flags `Text("OK")` or `Text("PDF")` is a rule the
 * builder learns to bypass with a suppression comment, and the defects that matter — a whole
 * sentence compiled into the UI — are two words or more. Single-word prose is therefore missed and
 * that is stated rather than hidden.
 */
export function isTranslatableProse(value) {
  const text = String(value).replace(/\\[nrt"'\\]/g, ' ').trim()
  if (text.length < 6) return false
  if (!/[a-z]/.test(text)) return false // an all-caps token like CSV or OK is not prose
  if (/^(?:https?:|www\.|mailto:|android\.)/i.test(text)) return false
  if (/%\d*\$?[a-zA-Z]/.test(text)) return false // a format string is not copy
  const words = text.split(/\s+/).filter((word) => /[a-z]/i.test(word))
  return words.length >= 2
}

/** Every user-facing literal compiled into one Kotlin file, as `<file>:<line>: ...`. */
export function hardcodedFindings(file, source) {
  const stripped = stripKotlinComments(source)
  const findings = []
  for (const { pattern, what } of LITERAL_SITES) {
    for (const match of stripped.matchAll(pattern)) {
      if (!isTranslatableProse(match[1])) continue
      const line = stripped.slice(0, match.index).split(/\r?\n/).length
      findings.push(
        `${file}:${line}: ${what} is given the literal "${match[1]}", so it is compiled into the APK and ` +
          'never reaches a resource — it cannot be translated, and the pseudolocale test cannot see it'
      )
    }
  }
  return findings
}

// ---------------------------------------------------------------------------------------------
// locale.pseudolocale — a test language is a device option, not a shipped one
// ---------------------------------------------------------------------------------------------

/** Android's two built-in pseudolocales, as they are written in a locale config. */
export const PSEUDOLOCALE_TAGS = Object.freeze(['en-XA', 'ar-XB'])

export const isPseudolocaleTag = (tag) => PSEUDOLOCALE_TAGS.some((known) => known.toLowerCase() === String(tag).toLowerCase())

/** A `values-en-rXA/` or `values-ar-rXB/` resource directory. */
export const isPseudolocaleDir = (name) => /^values-(?:en|ar)-r?(?:XA|XB)(?:-|$)/i.test(String(name))

// ---------------------------------------------------------------------------------------------
// The pure sensor
// ---------------------------------------------------------------------------------------------

/** The pure rules over an in-memory tree, so a fixture can drive every one without a slot on disk. */
export function localizationProblems(input = {}) {
  const {
    baseFiles = [],
    localeFiles = {},
    dirs = {},
    declared = [],
    declaredTags = [],
    resDirNames = [],
    fallback = 'en',
    kotlin = {}
  } = input

  const notes = []
  const byId = Object.fromEntries(LOCALIZATION_IDS.map((id) => [id, []]))

  byId['locale.multilingual'] = multilingualProblems({ declared, dirs, localeFiles, fallback })

  const placeholders = placeholderProblems({ baseFiles, localeFiles, dirs, declared, fallback })
  byId['locale.placeholder'] = placeholders.problems
  notes.push(...placeholders.notes)

  for (const [file, source] of Object.entries(kotlin)) {
    byId['locale.hardcoded'].push(...hardcodedFindings(file, source))
  }

  for (const tag of declaredTags) {
    if (isPseudolocaleTag(tag)) {
      byId['locale.pseudolocale'].push(
        `res/xml/locales_config.xml declares the pseudolocale '${tag}', so the per-app language picker ` +
          'offers a debug test language to real users'
      )
    }
  }
  for (const name of resDirNames) {
    if (isPseudolocaleDir(name)) {
      byId['locale.pseudolocale'].push(
        `res/${name}/ ships a pseudolocale resource tree; a pseudolocale is enabled from the device's ` +
          'developer options to test layout, and is never a released translation'
      )
    }
  }
  // A locale config whose tags could not be read makes the declared-pseudolocale half blind. Said
  // out loud, because a sensor that has stopped being able to fire must not read as one that found
  // nothing (`scripts/lib/client-secrets.mjs` states the same limit the same way).
  if (declared.length > 0 && declaredTags.length === 0) {
    notes.push(
      'locales_config.xml declares language(s) but no <locale android:name="…"> tag could be read, so the ' +
        'declared-pseudolocale sensor did not run against this file'
    )
  }
  if (resourceEntries(baseFiles).resources.size === 0) {
    notes.push('res/values/ holds no translatable string, so only the declared-language shape was measured')
  }

  const findings = LOCALIZATION_IDS.flatMap((id) => byId[id].map((finding) => `${id}: ${finding}`))
  return { byId, notes: dedupe(notes), findings }
}

// ---------------------------------------------------------------------------------------------
// Reading a slot or the scaffold off disk
// ---------------------------------------------------------------------------------------------

/** Every `.xml` under a resource directory, as `{ name, text }`, sorted so a report is stable. */
function xmlFiles(dir) {
  if (!existsSync(dir)) return []
  return readdirSync(dir)
    .filter((name) => name.endsWith('.xml'))
    .sort()
    .map((name) => ({ name, text: readFileSync(path.join(dir, name), 'utf8') }))
}

/**
 * The raw `<locale android:name="…">` tags, kept beside the reduced language list rather than
 * replacing it.
 *
 * `declaredLanguages()` is the one source of truth for the *language* a slot offers (it is what
 * `slot.locales` uses), and it collapses `zh-CN` and `en-XA` to `zh` and `en`. A pseudolocale is
 * only visible in the raw tag, which is the single reason this projection exists; a fixture pins
 * the two against the same config so they cannot drift apart silently.
 */
export function localeConfigTags(text) {
  return [...String(text).matchAll(/<locale\s+android:name="([^"]+)"/g)].map((match) => match[1])
}

/** Every Kotlin source under `app/src/main/java`, as `{ relPath: text }`, NUL-bearing files skipped. */
function kotlinSources(slotDir) {
  const javaDir = path.join(slotDir, 'app', 'src', 'main', 'java')
  if (!existsSync(javaDir)) return {}
  const sources = {}
  for (const file of walkSlotFiles(javaDir)) {
    if (!file.endsWith('.kt')) continue
    let text = null
    try {
      text = decodeSource(readFileSync(file))
    } catch {
      continue
    }
    if (text === null) continue
    sources[relFrom(slotDir, file)] = text
  }
  return sources
}

/**
 * A slot's or the scaffold's localization inputs, or the reason they could not be read.
 *
 * `ok: false` is a distinct outcome from "read and clean", so the caller can report `UNVERIFIED`
 * rather than a green line — measuring nothing must never read as a pass.
 */
export function readSlotLocalization(slotDir) {
  const resDir = path.join(slotDir, ...RES_REL.split('/'))
  if (!existsSync(resDir)) return { ok: false, reason: `no ${RES_REL}, so there is no resource tree to localize` }
  let entries
  try {
    entries = readdirSync(resDir, { withFileTypes: true })
  } catch (error) {
    return { ok: false, reason: `could not read ${RES_REL}: ${error.message}` }
  }
  const resDirNames = entries.filter((entry) => entry.isDirectory()).map((entry) => entry.name)
  const baseFiles = xmlFiles(path.join(resDir, 'values'))
  const dirs = localeDirNames(resDirNames)
  const localeFiles = {}
  for (const [language, directory] of Object.entries(dirs)) {
    localeFiles[language] = xmlFiles(path.join(resDir, directory))
  }

  const config = path.join(resDir, 'xml', 'locales_config.xml')
  const configText = existsSync(config) ? readFileSync(config, 'utf8') : null
  const fallback = listingLanguage(path.join(slotDir, 'store', 'listing.json'))

  return {
    ok: true,
    input: {
      baseFiles,
      localeFiles,
      dirs,
      declared: configText === null ? [] : declaredLanguages(config),
      declaredTags: configText === null ? [] : localeConfigTags(configText),
      resDirNames,
      fallback,
      kotlin: kotlinSources(slotDir)
    }
  }
}

/** The flat finding list, used to demonstrate the sensor in red and in green. */
export function localizationFindings(slotDir) {
  const read = readSlotLocalization(slotDir)
  if (!read.ok) return [`unreadable: ${read.reason}`]
  return localizationProblems(read.input).findings
}

// ---------------------------------------------------------------------------------------------
// The verdicts
// ---------------------------------------------------------------------------------------------

/** What a passing check may claim, per id, so the sentence cannot outrun the measurement. */
function summaryFor(id, input) {
  const { declared = [], fallback = 'en', dirs = {}, kotlin = {} } = input
  if (id === 'locale.multilingual') {
    const additional = additionalLanguages(declared, fallback)
    return `declares ${additional.length} language(s) besides the default '${fallback}' (${additional.join(', ') || 'none'})`
  }
  if (id === 'locale.placeholder') {
    const translated = additionalLanguages(declared, fallback).filter((language) => dirs[language])
    return `${translated.length} translated language(s) (${translated.join(', ') || 'none'}) carry no translation placeholder`
  }
  if (id === 'locale.hardcoded') {
    return `no user-facing prose compiled into ${Object.keys(kotlin).length} Kotlin source file(s) under app/src/main/java`
  }
  return 'neither locales_config.xml nor res/ declares a pseudolocale (en-XA / ar-XB)'
}

const withNotes = (summary, notes) => (notes.length === 0 ? summary : `${summary}; ${notes.join('; ')}`)

/** The four slot facts, as baseline-scoped verdicts. Registered in the `ui` group by the entry point. */
export function localizationChecks(slotDir) {
  return LOCALIZATION_IDS.map((id) =>
    guard(id, () => {
      const read = readSlotLocalization(slotDir)
      if (!read.ok) return unverified(id, read.reason)
      const { byId, notes } = localizationProblems(read.input)
      return baselineVerdict(id, slotDir, byId[id], withNotes(summaryFor(id, read.input), notes))
    })
  )
}

/** The scaffold a new slot is copied from. Named once so the project check and its fixtures cannot drift. */
export const SCAFFOLD_DIR = path.join(ROOT, 'scaffold', 'android-compose')

function scaffoldVerdict(id, slotDir, projectId) {
  const read = readSlotLocalization(slotDir)
  if (!read.ok) return unverified(projectId, read.reason)
  const { byId, notes } = localizationProblems(read.input)
  const findings = byId[id]
  const detail = withNotes(summaryFor(id, read.input), notes)
  if (findings.length > 0) {
    return fail(
      projectId,
      `the scaffold a new slot is copied from does not carry the localization baseline, so every slot ` +
        `created from it starts monolingual: ${findings.join('; ')}`
    )
  }
  return pass(projectId, detail)
}

/**
 * The factory's own half: does the scaffold a new slot is copied from carry the baseline?
 *
 * Expected RED until `scaffold/android-compose/` ships a second language, and deliberately not
 * weakened to be green — `localizationProjectChecks()` is what makes "localization is a design
 * input from day one" a fact about the factory rather than a sentence in a rule, and a check whose
 * own false green was its first observation would be the shape this repository has already retired.
 */
export function localizationProjectChecks() {
  return [
    guard('locale.scaffold.multilingual', () => scaffoldVerdict('locale.multilingual', SCAFFOLD_DIR, 'locale.scaffold.multilingual')),
    guard('locale.scaffold.hardcoded', () => scaffoldVerdict('locale.hardcoded', SCAFFOLD_DIR, 'locale.scaffold.hardcoded'))
  ]
}

// ---------------------------------------------------------------------------------------------
// Inline fixtures — the sensors above, made falsifiable
// ---------------------------------------------------------------------------------------------

const xml = (name, text) => ({ name, text })
const stringsXml = (pairs) =>
  `<resources>${pairs.map(([name, value]) => `<string name="${name}">${value}</string>`).join('')}</resources>`
const configXml = (tags) =>
  `<locale-config xmlns:android="http://schemas.android.com/apk/res/android">${tags.map((tag) => `<locale android:name="${tag}" />`).join('')}</locale-config>`

const BASE_PAIRS = [
  ['home_intro', 'A focused starting point'],
  ['home_action', 'Run the starter action'],
  ['about_body', 'This app keeps your record on the device']
]
const RU_PAIRS = [
  ['home_intro', 'Сфокусированная отправная точка'],
  ['home_action', 'Запустить стартовое действие'],
  ['about_body', 'Это приложение хранит записи на устройстве']
]

/** A slot tree written to a temp directory, so the disk driver itself is exercised, not only the pure rules. */
function withTempSlot(files, body) {
  const dir = mkdtempSync(path.join(SCRATCH_DIR, SCRATCH_PREFIX))
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

export function localizationFixtures() {
  return guard('localization.fixtures', () =>
    runFixtures('localization.fixtures', [
      // ---- locale.multilingual --------------------------------------------------------------
      [
        'a tree that declares only its default language is monolingual; one complete extra language is not',
        () => {
          const baseFiles = [xml('strings.xml', stringsXml(BASE_PAIRS))]
          const monolingual = localizationProblems({ baseFiles, declared: ['en'], fallback: 'en' })
          expectEqual(monolingual.byId['locale.multilingual'].length, 1, 'monolingual findings')
          expectTrue(monolingual.byId['locale.multilingual'][0].includes('monolingual'), 'the finding names the shape')

          const bilingual = localizationProblems({
            baseFiles,
            declared: ['en', 'ru'],
            dirs: { ru: 'values-ru' },
            localeFiles: { ru: [xml('strings.xml', stringsXml(RU_PAIRS))] }
          })
          expectEqual(bilingual.byId['locale.multilingual'], [], 'bilingual findings')
        }
      ],
      [
        'a declaration with no resource tree, and a declaration with an empty tree, are both shapes the build cannot honour',
        () => {
          const baseFiles = [xml('strings.xml', stringsXml(BASE_PAIRS))]
          const absent = localizationProblems({ baseFiles, declared: ['en', 'ru'], fallback: 'en' })
          expectTrue(absent.byId['locale.multilingual'][0].includes('no res/values-ru/'), absent.byId['locale.multilingual'][0])

          const empty = localizationProblems({
            baseFiles,
            declared: ['en', 'ru'],
            dirs: { ru: 'values-ru' },
            localeFiles: { ru: [] }
          })
          expectTrue(empty.byId['locale.multilingual'][0].includes('carries no strings'), empty.byId['locale.multilingual'][0])
        }
      ],
      // ---- locale.placeholder ----------------------------------------------------------------
      [
        'a sentinel, a mangled character and Lorem ipsum are placeholders; a real translation is not',
        () => {
          const baseFiles = [xml('strings.xml', stringsXml(BASE_PAIRS))]
          expectEqual(translationPlaceholder('TODO'), "the sentinel 'TODO'", 'a bare sentinel')
          expectEqual(translationPlaceholder('[TRANSLATE]'), "the sentinel '[TRANSLATE]'", 'a bracketed sentinel')
          expectTrue(translationPlaceholder('Broken \uFFFD value') !== null, 'U+FFFD is a placeholder')
          expectTrue(translationPlaceholder('Lorem ipsum dolor') !== null, 'Lorem ipsum is a placeholder')
          expectEqual(translationPlaceholder('Сфокусированная отправная точка'), null, 'a real translation')
          // The negative control that keeps the detector from being switched off: correct copy that
          // merely mentions translation must stay silent.
          expectEqual(translationPlaceholder('Machine translation is off'), null, 'prose mentioning translation')

          const flagged = localizationProblems({
            baseFiles,
            declared: ['en', 'ru'],
            dirs: { ru: 'values-ru' },
            localeFiles: { ru: [xml('strings.xml', stringsXml([...RU_PAIRS.slice(0, 2), ['about_body', 'TODO']]))] }
          })
          expectEqual(flagged.byId['locale.placeholder'].length, 1, 'one placeholder finding')
          expectTrue(flagged.byId['locale.placeholder'][0].includes("'about_body'"), flagged.byId['locale.placeholder'][0])
        }
      ],
      [
        'a locale copied verbatim from the default is a clone; the same tree translated is not, and a partial overlap is only a note',
        () => {
          const baseFiles = [xml('strings.xml', stringsXml(BASE_PAIRS))]
          const clone = localizationProblems({
            baseFiles,
            declared: ['en', 'ru'],
            dirs: { ru: 'values-ru' },
            localeFiles: { ru: [xml('strings.xml', stringsXml(BASE_PAIRS))] }
          })
          expectEqual(clone.byId['locale.placeholder'].length, 1, 'clone findings')
          expectTrue(clone.byId['locale.placeholder'][0].includes('byte-identical'), clone.byId['locale.placeholder'][0])

          const translated = localizationProblems({
            baseFiles,
            declared: ['en', 'ru'],
            dirs: { ru: 'values-ru' },
            localeFiles: { ru: [xml('strings.xml', stringsXml(RU_PAIRS))] }
          })
          expectEqual(translated.byId['locale.placeholder'], [], 'translated findings')

          // Four of five values verbatim is 0.8: over the note threshold but short of a clone, so it
          // is reported for a human to judge rather than failed — and the note is not a finding.
          const wide = [
            ['k1', 'Alpha one'],
            ['k2', 'Beta two'],
            ['k3', 'Gamma three'],
            ['k4', 'Delta four'],
            ['k5', 'Epsilon five']
          ]
          const mostlyShared = localizationProblems({
            baseFiles: [xml('strings.xml', stringsXml(wide))],
            declared: ['en', 'ru'],
            dirs: { ru: 'values-ru' },
            localeFiles: {
              ru: [xml('strings.xml', stringsXml([...wide.slice(0, 4), ['k5', 'Пятая буква']]))]
            }
          })
          expectEqual(mostlyShared.byId['locale.placeholder'], [], 'no failure at a high overlap')
          expectTrue(mostlyShared.notes.some((note) => note.includes('verbatim')), mostlyShared.notes.join('; '))
        }
      ],
      [
        'a translation missing half its keys is slot.locales\' business, not this module\'s — the boundary is a fixture',
        () => {
          const baseFiles = [xml('strings.xml', stringsXml(BASE_PAIRS))]
          const partial = localizationProblems({
            baseFiles,
            declared: ['en', 'ru'],
            dirs: { ru: 'values-ru' },
            localeFiles: { ru: [xml('strings.xml', stringsXml([RU_PAIRS[0]]))] }
          })
          expectEqual(partial.findings, [], 'a missing-key translation is not this module\'s finding')
        }
      ],
      // ---- locale.hardcoded ------------------------------------------------------------------
      [
        'a sentence compiled into Text(...) is a finding; stringResource, a token and a comment are not',
        () => {
          expectTrue(isTranslatableProse('Starter action complete'), 'a sentence is prose')
          expectTrue(!isTranslatableProse('OK'), 'a token is not prose')
          expectTrue(!isTranslatableProse('CSV'), 'an acronym is not prose')
          expectTrue(!isTranslatableProse('%1$s of %2$s'), 'a format string is not prose')
          expectEqual(hardcodedFindings('Home.kt', 'Text("Starter action complete")').length, 1, 'a literal')

          const clean = [
            'Text(stringResource(R.string.home_intro))',
            'Text("OK")',
            'contentDescription = name',
            '// Text("Starter action complete") is what this check exists for',
            '/* Text("Also in a block comment") */'
          ].join('\n')
          expectEqual(hardcodedFindings('Clean.kt', clean), [], 'a resource-backed screen')
        }
      ],
      [
        'comment blanking preserves offsets, so a finding still names the line it is on',
        () => {
          const source = ['// Text("Not this one")', 'val x = 1', 'Text("This one is real")'].join('\n')
          const stripped = stripKotlinComments(source)
          expectEqual(stripped.split('\n').length, source.split('\n').length, 'line count preserved')
          expectEqual(stripped.length, source.length, 'length preserved')
          const found = hardcodedFindings('Mixed.kt', source)
          expectEqual(found.length, 1, 'one finding')
          expectTrue(found[0].startsWith('Mixed.kt:3:'), found[0])
        }
      ],
      // ---- locale.pseudolocale ---------------------------------------------------------------
      [
        'a declared pseudolocale tag and a shipped pseudolocale tree are both findings; a normal config is not',
        () => {
          const baseFiles = [xml('strings.xml', stringsXml(BASE_PAIRS))]
          expectTrue(isPseudolocaleTag('en-XA'), 'en-XA is a pseudolocale tag')
          expectTrue(isPseudolocaleTag('ar-XB'), 'ar-XB is a pseudolocale tag')
          expectTrue(!isPseudolocaleTag('ru'), 'ru is a language')
          expectTrue(isPseudolocaleDir('values-en-rXA'), 'the en-XA directory')
          expectTrue(isPseudolocaleDir('values-ar-rXB'), 'the ar-XB directory')
          expectTrue(!isPseudolocaleDir('values-night'), 'a qualifier is not a pseudolocale')

          const declared = localizationProblems({ baseFiles, declared: ['en'], declaredTags: ['en', 'en-XA'], fallback: 'en' })
          expectEqual(declared.byId['locale.pseudolocale'].length, 1, 'a declared pseudolocale')

          const shipped = localizationProblems({ baseFiles, declared: ['en'], declaredTags: ['en'], resDirNames: ['values', 'values-en-rXA'], fallback: 'en' })
          expectEqual(shipped.byId['locale.pseudolocale'].length, 1, 'a shipped pseudolocale tree')

          const clean = localizationProblems({ baseFiles, declared: ['en'], declaredTags: ['en'], resDirNames: ['values', 'values-night'], fallback: 'en' })
          expectEqual(clean.byId['locale.pseudolocale'], [], 'a clean config')
        }
      ],
      [
        'the raw tag reader and the declared-language reader agree on the same config',
        () => {
          const text = configXml(['en', 'ru', 'zh-CN', 'en-XA'])
          expectEqual(localeConfigTags(text), ['en', 'ru', 'zh-CN', 'en-XA'], 'raw tags')
          expectEqual(
            localeConfigTags(text).map((tag) => tag.split('-')[0].toLowerCase()),
            ['en', 'ru', 'zh', 'en'],
            'the reduction slot.locales applies to the same tags'
          )
        }
      ],
      // ---- the disk driver -------------------------------------------------------------------
      [
        'the disk driver reads a real tree: a correct bilingual slot is clean, and deleting the translation turns it red',
        () => {
          const files = {
            'app/src/main/res/xml/locales_config.xml': configXml(['en', 'ru']),
            'app/src/main/res/values/strings.xml': stringsXml(BASE_PAIRS),
            'app/src/main/res/values-ru/strings.xml': stringsXml(RU_PAIRS),
            'app/src/main/java/com/gexetr/x/ui/Home.kt': 'Text(stringResource(R.string.home_intro))\n'
          }
          withTempSlot(files, (dir) => {
            expectEqual(localizationFindings(dir), [], 'a bilingual slot is clean')
            expectTrue(localizationFindings(dir).length === 0, 'and it is a real read, not a refusal')
          })

          const monolingual = { ...files, 'app/src/main/res/xml/locales_config.xml': configXml(['en']) }
          delete monolingual['app/src/main/res/values-ru/strings.xml']
          withTempSlot(monolingual, (dir) => {
            const findings = localizationFindings(dir)
            expectEqual(findings.length, 1, 'one finding')
            expectTrue(findings[0].startsWith('locale.multilingual:'), findings[0])
          })

          withTempSlot({}, (dir) => {
            expectEqual(localizationFindings(dir), ['unreadable: no app/src/main/res, so there is no resource tree to localize'], 'an unreadable slot is not empty')
          })
        }
      ]
    ])
  )
}

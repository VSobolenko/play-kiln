// The slot-level promises the core still owns: the languages behind the language picker, the
// reason list the app was chosen for, and the controls the store listing claims.
//
// These three facts had no core check id and one caller — `scripts/verify.mjs` — so the legacy
// gate could not be retired until they lived here. Ported, not paraphrased:
//
//   `slot.locales`          <- `verify.mjs` `verifyLocales()` and `scripts/check-locales.mjs`
//   `slot.differentiators`  <- `verify.mjs` `verifyDifferentiators()` (the module is imported)
//   `slot.uiCoverage`       <- `verify.mjs` `verifyUiCoverage()` and `scripts/check-ui-coverage.mjs`
//
// Why the locale rule set is restated here instead of imported, while the UI-coverage tables are not.
// `scripts/check-locales.mjs` is a CLI script whose last statement calls `main()` unconditionally,
// so importing it would execute it against the core's own `process.argv` (and spawn nothing); the
// locale table therefore says below which file and line it was copied from. The UI-coverage tables
// are the opposite case, and this is the direction the rule allows: they live here, and
// `scripts/check-ui-coverage.mjs` imports them, which is safe because this module has no top-level
// side effect. The core's contract is that a check is a function the entry point calls in-process —
// `specs/factory-core/spec.md`: "It SHALL import the checks it runs. The only subprocess it may
// spawn is the build tool" — and the fixtures in `policyFixtures()` pin the behaviour each table
// produces. `scripts/lib/slot-differentiators.mjs` *is* a module and is imported rather than
// restated, per `.cursor/rules/verification-discipline.mdc` ("One source of truth, imported —
// never restated").
//
// Two rules every check here obeys. A missing input is never a pass: an absent `store/listing.json`,
// an absent `differentiators.json`, an absent `res/xml/locales_config.xml` or an absent ViewModel
// reports `UNVERIFIED` naming the path or the count, because measuring nothing must not read as a
// green line — `harness/lessons.md` (the false `PASS` that `check-ui-coverage.mjs` printed for every
// slot but the one its table was authored for) and `docs/reviews/2026-09-29-factory-review-decisions.md`
// D6 are the recorded incidents. And no window is narrowed: the locale engine below applies the whole
// `check-locales.mjs` rule set, not only the declared-language half.
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import {
  CONNECTIVE_FAMILIES,
  crudReport,
  crudVerdict,
  DEPTH_FAMILIES,
  depthFindings,
  JOB_STAGES,
  parseDifferentiators,
  summarizeDifferentiators,
  validateDifferentiatorRefs
} from '../lib/slot-differentiators.mjs'
import { baselineVerdict } from './baseline.mjs'
import { expectEqual, expectTrue, fail, guard, pass, ROOT, unverified, VERDICT } from './lib.mjs'

/** Slot-relative, POSIX-shaped path, for a message a human reads. */
const rel = (file) => path.relative(ROOT, file).split(path.sep).join('/')

const readText = (file) => readFileSync(file, 'utf8')

/** The slot's bare slug: the directory name without its `YYYY-MM-DD-` scaffold prefix. */
const slotSlug = (slotDir) => path.basename(slotDir).replace(/^\d{4}-\d{2}-\d{2}-/, '')

/** Every `.xml` under a resource directory, as `{ name, text }`, sorted so messages are stable. */
function xmlFiles(dir) {
  if (!existsSync(dir)) return []
  return readdirSync(dir)
    .filter((name) => name.endsWith('.xml'))
    .sort()
    .map((name) => ({ name, text: readText(path.join(dir, name)) }))
}

/** Every `.kt` under a source root, recursively. */
function kotlinFiles(dir) {
  const out = []
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) out.push(...kotlinFiles(full))
    else if (entry.name.endsWith('.kt')) out.push(full)
  }
  return out
}

// ---------------------------------------------------------------------------------------------
// slot.locales — the language engine
//
// Ported from `scripts/check-locales.mjs` (read 2026-09-30). Its four rules are all here, because a
// core check that only counted directories would quietly accept the state the script exists to
// refuse: a language offered in the picker with no resources, a translation missing keys, a
// dropped or renumbered format argument, a missing plural category, and a `translatable="false"`
// key copied into a locale where it can silently diverge from the default.
// ---------------------------------------------------------------------------------------------

/**
 * The plural categories CLDR can select for an integer count in 0..1000, per language.
 *
 * Copied verbatim from `scripts/check-locales.mjs` (its `PLURAL_CATEGORIES`). A count of 1,000,000 or
 * more adds a `many` category in most European languages and no roster, mark count or day count
 * reaches that, so `many` is optional for those languages instead of demanded; Russian and Polish
 * need `few` and `many` for ordinary counts. A declared language missing from this table is refused
 * by the check rather than silently under-checked.
 */
export const PLURAL_CATEGORIES = Object.freeze({
  en: ['one', 'other'],
  de: ['one', 'other'],
  nl: ['one', 'other'],
  es: ['one', 'other'],
  it: ['one', 'other'],
  pt: ['one', 'other'],
  tr: ['one', 'other'],
  fr: ['one', 'other'],
  ru: ['one', 'few', 'many', 'other'],
  pl: ['one', 'few', 'many', 'other'],
  ja: ['other'],
  zh: ['other'],
  zz: ['other']
})

/** Categories CLDR defines for a language but only for counts this app cannot produce. */
export const PLURAL_OPTIONAL = Object.freeze({
  fr: ['many'],
  es: ['many'],
  it: ['many'],
  pt: ['many'],
  en: ['many'],
  de: ['many'],
  nl: ['many']
})

/** Resource qualifiers that look like a language but are not one. Copied from `check-locales.mjs`. */
export const NOT_A_LOCALE = Object.freeze([
  'night', 'notnight', 'land', 'port', 'car', 'desk', 'television', 'watch', 'vrheadset',
  'square', 'long', 'notlong', 'round', 'notround', 'highdr', 'lowdr', 'nowidecg', 'widecg',
  'ldrtl', 'ldltr', 'anydpi'
])

/** The positional format arguments in a value, sorted. `%%` is a literal, not an argument. */
export function formatArgs(text) {
  const args = []
  for (const match of text.replace(/%%/g, '').matchAll(/%(\d+)\$([a-zA-Z])/g)) {
    args.push(`%${match[1]}$${match[2]}`)
  }
  return args.sort()
}

/** A `%` that is neither `%%` nor a positional argument: only a defect where the string is formatted. */
export function nonPositionalSpecifiers(text) {
  return [...text.replace(/%%/g, '').matchAll(/%([^0-9]|\d+[^$])/g)].map((match) => match[0])
}

/**
 * The language-qualified resource directories, as `language -> directory name`.
 *
 * `values-night` and friends are qualifiers, not languages, and `values-b+sr+Latn` has no
 * two-or-three-letter language tag, so both are skipped rather than reported as a language Android
 * never offered.
 */
export function localeDirNames(names, notALocale = NOT_A_LOCALE) {
  const found = {}
  for (const name of names) {
    if (!name.startsWith('values-')) continue
    const language = name.slice('values-'.length).split('-')[0].toLowerCase()
    if (notALocale.includes(language) || !/^[a-z]{2,3}$/.test(language)) continue
    if (!(language in found)) found[language] = name
  }
  return found
}

/**
 * Every string and plurals entry in a set of resource files.
 *
 * A deliberately small parser rather than an XML library, as in the source: these files are
 * machine-shaped, and the only things that matter are the element kind, the name, and the format
 * arguments — per item, for a plural. A `<string translatable="false">` is skipped, because it is
 * not offered for translation. A key declared twice is recorded rather than thrown, so the check
 * can report it beside everything else instead of stopping at the first one.
 */
export function resourceEntries(files) {
  const resources = new Map()
  const duplicates = []
  for (const { name, text } of files) {
    for (const match of text.matchAll(/<(string|plurals)([^>]*)>([\s\S]*?)<\/\1>/g)) {
      if (/translatable\s*=\s*"false"/.test(match[2])) continue
      const nameMatch = match[2].match(/name\s*=\s*"([^"]+)"/)
      if (!nameMatch) continue
      const key = nameMatch[1]
      if (resources.has(key)) {
        duplicates.push(`'${key}' is declared more than once (in ${name})`)
        continue
      }
      const body = match[3]
      if (match[1] === 'plurals') {
        const items = new Map()
        for (const item of body.matchAll(/<item\s+quantity\s*=\s*"([^"]+)"\s*>([\s\S]*?)<\/item>/g)) {
          items.set(item[1], formatArgs(item[2]))
        }
        resources.set(key, { kind: 'plurals', items, body, source: name })
      } else {
        resources.set(key, { kind: 'string', args: formatArgs(body), body, source: name })
      }
    }
  }
  return { resources, duplicates }
}

/**
 * Keys declared `translatable="false"` in a set of resource files, with their source file.
 *
 * A copy of one of these in a locale directory cannot change what the user sees, because Android
 * already falls back to the default — so the copy can only drift away from the original. That is
 * how the generator's `support@example.com` placeholder shipped into the About screen of twelve
 * files at once while `store/listing.json` advertised the developer's real address.
 */
export function nonTranslatableEntries(files) {
  const keys = new Map()
  const repeated = []
  for (const { name, text } of files) {
    for (const match of text.matchAll(/<(string|plurals)([^>]*)>([\s\S]*?)<\/\1>/g)) {
      if (!/translatable\s*=\s*"false"/.test(match[2])) continue
      const nameMatch = match[2].match(/name\s*=\s*"([^"]+)"/)
      if (!nameMatch) continue
      const key = nameMatch[1]
      if (keys.has(key)) repeated.push(`'${key}' in both ${keys.get(key)} and ${name}`)
      keys.set(key, name)
    }
  }
  return { keys, repeated }
}

/**
 * The locale rules, over an in-memory resource tree, so a fixture can drive them without a slot.
 *
 * A language the slot declares and does not translate is the failure the whole check exists for:
 * `res/xml/locales_config.xml` drives Android 13's per-app language picker, so a locale listed there
 * with no `values-<lang>/` offers the user a language that changes nothing. The `fallback` language
 * is the one served by `values/` itself, so demanding `values-en/` would demand a duplicate of the
 * default and would fail on every correct app.
 */
export function localeProblems({ baseFiles, localeFiles = {}, dirs = {}, declared = [], fallback = 'en' }) {
  const failures = []
  const notes = []
  const base = resourceEntries(baseFiles)
  const nonTranslatable = nonTranslatableEntries(baseFiles)

  for (const duplicate of base.duplicates) failures.push(`res/values/ declares a key twice: ${duplicate}`)
  for (const repeated of nonTranslatable.repeated) {
    failures.push(`res/values/ declares a non-translatable key twice: ${repeated}`)
  }
  if (base.resources.size === 0) failures.push('res/values/ holds no strings to translate')

  for (const language of [...declared].sort()) {
    if (language === fallback) continue
    if (!PLURAL_CATEGORIES[language]) {
      failures.push(
        `locales_config.xml offers '${language}' but PLURAL_CATEGORIES in scripts/core/policy.mjs has ` +
          'no entry for it, so its plural categories would be guessed rather than checked. Add the ' +
          'categories CLDR defines for that language before declaring it.'
      )
    }
    if (!dirs[language]) {
      failures.push(
        `locales_config.xml offers '${language}' but res/values-${language}/ does not exist, so the ` +
          'per-app language picker would offer a language that changes nothing'
      )
    }
  }
  for (const [language, directory] of Object.entries(dirs)) {
    if (!declared.includes(language)) {
      failures.push(
        `res/${directory}/ exists but locales_config.xml does not declare '${language}', so Android ` +
          'resolves those resources without offering the language'
      )
    }
  }

  let translated = 0
  let complete = 0
  const translatedDirs = Object.entries(dirs)
    .filter(([language]) => declared.includes(language))
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
  for (const [language, directory] of translatedDirs) {
    complete += 1
    const files = localeFiles[language] || []
    const local = resourceEntries(files)
    const localNonTranslatable = nonTranslatableEntries(files)
    for (const duplicate of local.duplicates) {
      failures.push(`res/${directory}/ declares a key twice: ${duplicate}`)
    }
    if (local.resources.size === 0) {
      failures.push(`res/${directory}/ contains no strings`)
      continue
    }

    for (const [key, source] of localNonTranslatable.keys) {
      if (!nonTranslatable.keys.has(key)) {
        notes.push(
          `res/${directory}/ declares '${key}' (${source}) translatable="false", which values/ does not declare`
        )
        continue
      }
      failures.push(
        `res/${directory}/ redeclares '${key}' (${source}), which values/ marks ` +
          'translatable="false" - the default is the only source of truth for it, so the two copies ' +
          'can silently diverge and translate a value that was declared untranslatable'
      )
    }

    for (const [key, source] of base.resources) {
      const target = local.resources.get(key)
      if (!target) {
        failures.push(
          `res/${directory}/ is missing '${key}' (from values/${source.source}), so it falls back to English`
        )
        continue
      }
      translated += 1
      if (source.kind !== target.kind) {
        failures.push(
          `res/${directory}/ declares '${key}' as <${target.kind}> but the default is <${source.kind}>`
        )
        continue
      }

      if (source.kind === 'string') {
        if (source.args.join(',') !== target.args.join(',')) {
          failures.push(
            `res/${directory}/ '${key}' has format arguments [${target.args.join(', ')}] where the ` +
              `default has [${source.args.join(', ')}]; a mismatch crashes or leaves a hole in the sentence`
          )
        }
      } else {
        // Each plural item is its own format string, so each must carry a signature the default uses.
        const signatures = new Set([...source.items.values()].map((args) => args.join(',')))
        const required = PLURAL_CATEGORIES[language] || ['other']
        for (const category of required) {
          if (!target.items.has(category)) {
            failures.push(
              `res/${directory}/ <plurals name="${key}"> has no '${category}' category, which ${language} ` +
                'selects for ordinary counts - a missing category throws at runtime rather than falling back'
            )
          }
        }
        for (const [category, args] of target.items) {
          if (!signatures.has(args.join(','))) {
            failures.push(
              `res/${directory}/ <plurals name="${key}"> category '${category}' has format arguments ` +
                `[${args.join(', ')}], which is not a signature the default uses ([${[...signatures].join('] [')}])`
            )
          }
          if (!required.includes(category) && !(PLURAL_OPTIONAL[language] || []).includes(category)) {
            notes.push(`res/${directory}/ <plurals name="${key}"> has an unused '${category}' category`)
          }
        }
      }

      const stray = nonPositionalSpecifiers(target.body)
      if (stray.length > 0) {
        const formatted = source.kind === 'string' ? source.args.length > 0 : true
        const message =
          `res/${directory}/ '${key}' contains ${stray.map((s) => `'${s}'`).join(', ')}, a ` +
          'format specifier with no position'
        if (formatted) failures.push(`${message}; the string is formatted, so this throws or mangles the value`)
        else notes.push(`${message} - harmless here, because this string is never formatted`)
      }
    }

    for (const key of local.resources.keys()) {
      if (!base.resources.has(key)) {
        failures.push(
          `res/${directory}/ declares '${key}', which values/ no longer has - a translation of copy that no longer ships`
        )
      }
    }
  }

  return { failures, notes, translated, complete, baseKeys: base.resources.size }
}

/**
 * The language each `values/` fallback is written in, read from the store listing.
 *
 * `values/` is the fallback for every locale and it has no `values-<lang>/` of its own, so this is
 * the language an untranslated string actually looks like. The listing is where the slot declares
 * it; a listing that is malformed or silent falls back to `en`, which is the factory's default
 * (`scripts/lib/play-assets.json` `locales.default` is `en-US`) rather than a guess.
 */
export function listingLanguage(file) {
  try {
    const tag = String(JSON.parse(readText(file)).defaultLocale || '')
      .split('-')[0]
      .toLowerCase()
    if (/^[a-z]{2,3}$/.test(tag)) return tag
  } catch {
    // A malformed listing is the pack renderer's business, not this check's.
  }
  return 'en'
}

/** The languages `<locale-config>` declares, as language tags. */
export function declaredLanguages(file) {
  const declared = []
  for (const match of readText(file).matchAll(/<locale\s+android:name="([^"]+)"/g)) {
    const language = match[1].split('-')[0].toLowerCase()
    if (!declared.includes(language)) declared.push(language)
  }
  return declared
}

export function checkLocales(slotDir) {
  return guard('slot.locales', () => {
    const resDir = path.join(slotDir, 'app', 'src', 'main', 'res')
    const config = path.join(resDir, 'xml', 'locales_config.xml')
    const valuesDir = path.join(resDir, 'values')
    const listing = path.join(slotDir, 'store', 'listing.json')

    if (!existsSync(listing)) {
      return unverified('slot.locales', `no ${rel(listing)} to read the default language from`)
    }
    if (!existsSync(config)) {
      return unverified('slot.locales', `no ${rel(config)}, so there is no language declaration to check`)
    }
    const baseFiles = xmlFiles(valuesDir)
    if (baseFiles.length === 0) {
      return unverified('slot.locales', `no XML resource file under ${rel(valuesDir)}, so there is nothing to translate`)
    }

    const dirs = localeDirNames(
      readdirSync(resDir, { withFileTypes: true })
        .filter((entry) => entry.isDirectory())
        .map((entry) => entry.name)
    )
    const localeFiles = {}
    for (const [language, directory] of Object.entries(dirs)) {
      localeFiles[language] = xmlFiles(path.join(resDir, directory))
    }

    const fallback = listingLanguage(listing)
    const { failures, notes, translated, complete, baseKeys } = localeProblems({
      baseFiles,
      localeFiles,
      dirs,
      declared: declaredLanguages(config),
      fallback
    })

    if (failures.length > 0) {
      return fail(
        'slot.locales',
        `${failures.length} problem(s) across ${complete} language(s) and ${baseKeys} key(s): ` +
          failures.join('; ')
      )
    }
    // The honest sentence when a slot declares no additional language: `0 translated language(s)`,
    // not a claim about languages nobody translated. That is the legacy gate's own wording.
    const noteText = notes.length > 0 ? `; ${[...new Set(notes)].length} note(s)` : ''
    return pass(
      'slot.locales',
      `${complete} translated language(s) x ${baseKeys} key(s) complete, plus the ${fallback} default ` +
        `in values/: ${translated} entries, every format argument and plural category matching the default${noteText}`
    )
  })
}

// ---------------------------------------------------------------------------------------------
// slot.differentiators — the promise contract
//
// Ported from `verify.mjs` `verifyDifferentiators()`. The library
// (`scripts/lib/slot-differentiators.mjs`) is imported: it parses the contract, resolves every
// proof against the slot on disk, and — the teeth — fails while any promise is still `deferred`,
// which is what stops a slot being called done with ten of twenty reasons delivered.
//
// Since 2026-10-02 the same check carries the **depth** half (`.cursor/rules/app-depth.mdc`): a
// promise list that names only the record-keeping middle of the job produces exactly that app while
// every gate reports PASS, so `depthFindings` asks whether `jobCoverage` covers `before`, `during`
// and `after`, and whether at least one shipped claim is a *connective* handoff whose proof artifact
// actually contains the family's named token. The measured defect is `apps/2026-10-01-lawn-round-log`:
// it builds `domain/Csv.kt` and has no `ACTION_SEND`, no `ACTION_CREATE_DOCUMENT` and no
// `FileProvider`, so the CSV is constructed and the user cannot get the file off the phone. Those
// findings are baseline-scoped through `baselineVerdict`; the CRUD report beside them is a note that
// never gates, because the predicate measures vocabulary and depth is semantics plus market.
//
// One half does not survive the port, and it is stated rather than implied: the legacy wrapper also
// looked for `differentiators.json` in git, so deleting the file from the working tree was a `FAIL`
// ("committed but missing from the working tree") rather than an `UNVERIFIED`. Reading git means
// spawning a subprocess, which the core's contract does not allow outside the build route, so the
// deleted-file case now reports `UNVERIFIED` naming the path. The protection that matters is kept:
// an absent contract can never read as a pass.
// ---------------------------------------------------------------------------------------------

/** The parse-and-resolve result for one contract, with the file system injected so a fixture can drive it. */
export function contractProblems(json, options) {
  const parsed = parseDifferentiators(json)
  if (!parsed.ok) return { stage: 'schema', errors: parsed.errors, summary: null }
  const summary = summarizeDifferentiators(parsed.value)
  const refs = validateDifferentiatorRefs(parsed.value, options)
  return {
    stage: refs.ok ? 'ok' : 'refs',
    errors: refs.ok ? [] : refs.errors,
    summary,
    value: parsed.value
  }
}

export function checkDifferentiators(slotDir) {
  return guard('slot.differentiators', () => {
    const file = path.join(slotDir, 'differentiators.json')
    if (!existsSync(file)) {
      return unverified(
        'slot.differentiators',
        `no ${rel(file)}; this slot declares no reason the app should win, so every promise it was ` +
          'chosen for is unaccounted for'
      )
    }

    let json
    try {
      json = readText(file)
    } catch (error) {
      return fail('slot.differentiators', `${rel(file)} could not be read: ${error.message}`)
    }

    const result = contractProblems(json, {
      slotDir,
      repoRoot: ROOT,
      slug: slotSlug(slotDir),
      allowDeferred: false,
      isFile: (candidate) => {
        try {
          return statSync(candidate).isFile()
        } catch {
          return false
        }
      },
      readFile: (candidate) => readText(candidate)
    })

    if (result.stage !== 'ok') {
      return fail(
        'slot.differentiators',
        result.errors.length > 0
          ? result.errors.join('; ')
          : 'the promise contract could not be resolved against the slot'
      )
    }

    const { total, core, shipped, narrowed, dropped, deferred } = result.summary
    const summary =
      `contract schema — ${total} promise(s), ${core} core, ${shipped} shipped, ${narrowed} narrowed, ` +
      `${dropped} dropped, ${deferred} deferred`

    // The CRUD report is a note and never a finding. `docs/research/2026-10-02-depth-encoding-and-
    // detectability.md` §1.3 measured the repository's own `edit-every-record` row escaping the
    // lexical predicate because it carries a contrast clause, which may be a true and sourced
    // difference; a FAIL here would fire on correct prose. It prints so the owner can read it at
    // checkpoint 1 (`.cursor/rules/app-depth.mdc`, "Human-audited, reported and never auto-passed").
    const crud = crudReport(result.value, { file: rel(file), raw: json })
    const withCrud = crud.length
      ? `${summary}; CRUD report (human-read, never gating): ${crud.join('; ')}`
      : summary

    // The depth floor — does the contract cover the user's whole job, and does at least one shipped
    // handoff carry its family's token in the artifact that proves it — is baseline-scoped through
    // `baselineVerdict`, exactly as `uiux.*`, `settings.*` and `locale.*` are: a slot created before
    // the rule reports the finding as a note, a slot created from it is gated.
    const depth = depthFindings(result.value, {
      slotDir,
      isFile: (candidate) => {
        try {
          return statSync(candidate).isFile()
        } catch {
          return false
        }
      },
      readFile: (candidate) => readText(candidate)
    })
    return baselineVerdict('slot.differentiators', slotDir, depth, withCrud)
  })
}

// ---------------------------------------------------------------------------------------------
// slot.uiCoverage — a claimed operation with no control
//
// Ported from `scripts/check-ui-coverage.mjs` and `verify.mjs` `verifyUiCoverage()`.
//
// The signal is a dangling ViewModel operation, cross-checked against the promises in
// `store/listing.json` via `REQUIRED` below. Five screens were restyled in one pass and one dropped
// the add/edit controls for notes and contacts while keeping the read-only list; every other check
// reads form, so nothing could see it — the restyle compiled, and the domain tests exercise the edit
// logic directly, so they stayed green.
//
// The defect the table then caused is recorded 2026-09-29 (`harness/lessons.md`, factory-review D6):
// because `REQUIRED` is authored for one slot, it reported a *pass* for every other slot while
// checking nothing. Both ways of checking nothing are `UNVERIFIED` here, and the refusals are part
// of the pure function below so a fixture can prove they are refusals and not green lines.
// ---------------------------------------------------------------------------------------------

/**
 * Operations the store listing promises, with the promise each one keeps.
 *
 * This is the single home of the table: `scripts/check-ui-coverage.mjs` imports it rather than
 * restating it, so that CLI and `slot.uiCoverage` cannot drift apart. The `claim` text is a
 * quotation or a close paraphrase of `store/listing.json`'s `fullDescription`, so this table can be
 * checked against the listing it guards rather than trusted — and a reviewer adding a feature is the
 * person who should add its line.
 */
export const REQUIRED = Object.freeze({
  addClass: 'the listing sells "create, rename, edit and delete them" for classes',
  renameClass: 'the listing sells "create, rename, edit and delete them" for classes',
  updateClassDetails: 'the listing sells "edit" for classes (subject, period, term)',
  deleteClass: 'the listing sells deleting a class, with its roster and marks',
  addStudent: 'the listing sells "add a roster by hand"',
  updateStudent: 'the listing sells "edit anyone, mark someone inactive when they leave"',
  deleteStudent: 'the listing sells "or delete them"',
  importRoster: 'the listing sells "or import a CSV ... never adds the same student twice"',
  addCategory: 'the listing sells "weigh coursework, exams and homework however your department does"',
  updateCategory: 'the listing sells changing a category weight',
  deleteCategory: 'a category that can be added and weighed must be removable',
  moveCategory: 'the listing sells "reorder them"',
  addAssignment: 'the listing sells recording a score per assignment',
  updateAssignment: 'the listing sells weighted categories, which requires editing an assignment',
  deleteAssignment: 'an assignment that can be added must be removable',
  setGrade: 'the listing sells "record a score, mark work missing, or exempt a student"',
  clearGrade: 'the listing sells that "an empty cell is never written as a zero"',
  setAttendance: 'the listing sells "mark a register per day"',
  clearAttendance: 'a register that can be marked must be correctable',
  markAllPresent: 'the listing sells "with one tap for everyone present"',
  addNote: 'the listing sells "participation notes"',
  updateNote: 'the listing sells notes with a kind and a date',
  deleteNote: 'a note that can be recorded must be removable',
  addContact: 'the listing sells "a log of who you contacted, when, and what about"',
  updateContact: 'the listing sells "follow-up dates that come back to you"',
  deleteContact: 'a contact that can be logged must be removable',
  resizeSeating: 'the listing sells "set a grid of desks"',
  setSeat: 'the listing sells "and put students in them"',
  moveToNextFreeDesk: 'the listing sells seating plans a teacher does not redraw',
  requestExport: 'the listing sells CSV and PDF export',
  requestBackup: 'the listing sells writing "your whole record to one file you keep"',
  restoreBackup: 'the listing sells restoring that file later',
  // --- laneset (apps/2026-10-02-laneset) ---
  // The swim set builder's claimed operations. The listing is `store/listing.json`'s
  // `fullDescription`; each operation is the control its sentence depends on, and every one is
  // reachable from a screen today, so this table checks the slot instead of refusing to speak.
  onEvent: 'the listing sells configuring "the minutes you have, the length of your pool, the day\'s focus, the strokes you want to avoid and the gear that is on the deck", then computing the set from them',
  onPasteFromClipboard: 'the listing sells "Paste in the set your coach wrote"',
  onRead: 'the listing sells that Laneset "parses it into the same structured, paced session it generates itself"',
  onAccept: 'the listing sells the pasted set becoming "the same structured, paced session it generates itself"',
  buy: 'the listing sells "One purchase, everything included" for the one-time unlock',
  // --- lawn-round-log (apps/2026-10-01-lawn-round-log) ---
  // The round book's claimed operations. The listing is `store/listing.json`'s `fullDescription`;
  // each operation is the control its sentence depends on. Measured before this row was written:
  // with it `uiCoverageFrom` returns 0 problems over 33 reachable operations (2 notes: `clearStatus`,
  // `shutdown`, both lifecycle calls), and the extended table run over every slot in `apps/` leaves
  // every other slot at 0 problems. `restore` is shared with laneset, whose own unlock screen
  // restores a purchase, so its claim is worded for both rather than for one.
  upsertCustomer: 'the listing sells "Add each customer once" and "Edit or delete a customer"',
  move: 'the listing sells "move the list into the order you actually drive"',
  mark: 'the listing sells "Mark a visit mowed, skipped or rained off with a single tap"',
  pushByOneDay: 'the listing sells "One action moves every customer due today forward exactly one day"',
  recordPayment: 'the listing sells "Record a payment in one tap"',
  removePayment: 'the listing sells "and remove it if you mistyped"',
  restatePayment: 'the listing sells "Edit or delete a customer, a visit or a payment" (payment)',
  deleteCustomer: 'the listing sells "Edit or delete a customer, a visit or a payment" (customer)',
  deleteVisit: 'the listing sells "Edit or delete a customer, a visit or a payment" (visit)',
  restateVisit: 'the listing sells "Edit or delete a customer, a visit or a payment" (visit status)',
  exportCsv: 'the listing sells "Export the stored record to a CSV file through the system file picker"',
  shareCsv: 'the listing sells keeping "your own copy"; the share control hands the CSV off the device',
  purchase: 'the listing sells "One purchase adds two things and nothing else" — the unlock control',
  restore: 'the listing sells that the one-time purchase can be restored after a reinstall',
  setConfirmBeforeDelete: 'the listing sells a delete confirmation "on by default and can be turned off in Settings"',
  startNew: 'the listing sells "Add each customer once" (the editor entry point)',
  startEdit: 'the listing sells "Edit or delete a customer" (the editor entry point)',
  setFilter: 'the listing sells every visit "on the record" (the history filter)',
  clearExportStatus: 'the listing sells the app telling "you how many rows it wrote"',
  // --- plinth-product-photos (apps/2026-10-15-plinth-product-photos) ---
  // The product-photo operations kept by this listing: importing a photo, cutting it out,
  // composing a card, choosing a marketplace and exporting the result. Each claim quotes or
  // paraphrases `store/listing.json`'s `fullDescription`.
  importUris: 'the listing sells "Use the camera or choose photos from your device"',
  select: 'the listing sells choosing "photos from your device" for a product cut-out',
  startCutout: 'the listing sells "Cut out a product"',
  setBrushMode: 'the listing sells identifying images that are "manually adjusted"',
  applyBrushStroke: 'the listing sells identifying images that are "manually adjusted"',
  undoBrushStroke: 'the listing sells identifying images that are "manually adjusted"',
  makeCard: 'the listing sells composing a product "on a clean ... background"',
  updateSettings: 'the listing sells "compose it on a clean or generated background"',
  setShadow: 'the listing sells composing a polished listing image on a clean background',
  undo: 'the listing sells editing a composition while keeping "the product\'s original pixels"',
  redo: 'the listing sells editing a composition while keeping "the product\'s original pixels"',
  refreshCostAndBalance: 'the listing sells reviewing "the credit cost and price shown in the app before confirming a purchase"',
  start: 'the listing sells a "five-item batch limit" that the one-time unlock removes',
  saveCategory: 'the listing sells checking "marketplace image requirements" before export',
  setMarketplaceTarget: 'the listing sells "per-marketplace listing images"',
  changeMarketplaceForSelected: 'the listing sells "per-marketplace listing images"',
  reExportSelected: 'the listing sells "export listing-ready images"'
})

/** Operations that are deliberately not called from a screen, with the reason they are not. */
export const NOT_A_CONTROL = Object.freeze({
  load: 'called by the shell on entry, not by a button',
  announce: 'how a screen posts a message; called from other ViewModel code',
  next: 'the id generator, used by the ViewModel itself',
  isUnlocked: 'the entitlement question, asked by the ViewModel and by tests',
  classroom: 'a state accessor a screen reads through `state`, not an action',
  averageOf: 'a read, reached through the average surface or the shell',
  csvFor: 'produces the export payload; `requestExport` is the control',
  backupDocument: 'produces the backup payload; `requestBackup` is the control',
  createServerSession: 'bootstraps the device session from an integrity token; it is app setup, not a user control'
})

/**
 * Public functions declared by a class in this file.
 *
 * `^    fun` is the four-space body of a class in this codebase and is deliberately narrow: a nested
 * `private fun` is indented further and an interface's `fun` has no body, so neither is collected.
 * `private` and `internal` are excluded because they cannot be the missing control.
 */
export function declaredOperations(source) {
  const found = new Set()
  for (const match of source.matchAll(/^ {4}fun ([A-Za-z_][A-Za-z0-9_]*)\s*\(/gm)) {
    found.add(match[1])
  }
  return found
}

/**
 * The whole UI-coverage rule over an in-memory file list, including both refusal shapes.
 *
 * `files` are `{ path, source }` with `path` relative to `app/src/main/java`, so a fixture can build
 * a slot in memory. A screen's own file is not a caller of its own ViewModel: the operation is
 * dangling unless some *other* file reaches it, so the ViewModel files are removed from the corpus.
 */
export function uiCoverageFrom(files, { slug = '<slot>', required = REQUIRED, notAControl = NOT_A_CONTROL } = {}) {
  const uiDirs = files.filter((file) => file.path.split('\\').join('/').includes('/ui/'))
  const viewModels = uiDirs.filter((file) => file.path.split('\\').join('/').endsWith('ViewModel.kt'))
  if (viewModels.length === 0) {
    return {
      refusal: {
        reason: 'no-viewmodel',
        count: 0,
        detail:
          `${slug} declares no ViewModel under ui/ (0 of ${uiDirs.length} ui/*.kt file(s) matches ` +
          '*ViewModel.kt), so nothing about the listing\'s claims could be checked. Point REQUIRED at ' +
          "this slot's own operations before treating this as evidence."
      },
      problems: [],
      notes: [],
      checked: 0,
      viewModels: 0,
      declaredCount: 0
    }
  }

  const corpus = uiDirs.filter((file) => !viewModels.includes(file))
  const declared = new Set()
  for (const vm of viewModels) {
    for (const operation of declaredOperations(vm.source)) declared.add(operation)
  }

  if (Object.keys(required).filter((operation) => declared.has(operation)).length === 0) {
    return {
      refusal: {
        reason: 'not-applicable',
        count: declared.size,
        detail:
          `${slug} declares ${declared.size} ViewModel operation(s) and none of them is named by ` +
          'REQUIRED, so this table cannot check this slot. Add its claimed operations to REQUIRED, ' +
          'with the listing sentence each one keeps.'
      },
      problems: [],
      notes: [],
      checked: 0,
      viewModels: viewModels.length,
      declaredCount: declared.size
    }
  }

  const problems = []
  const notes = []
  let checked = 0
  for (const vm of viewModels) {
    for (const operation of declaredOperations(vm.source)) {
      const reachable = corpus.some(({ source }) =>
        new RegExp(`\\b${operation}\\s*\\(|::${operation}\\b`).test(source)
      )
      if (reachable) {
        checked += 1
        continue
      }
      if (notAControl[operation]) continue
      if (required[operation]) problems.push({ operation, claim: required[operation] })
      else notes.push(operation)
    }
  }
  return {
    refusal: null,
    problems,
    notes,
    checked,
    viewModels: viewModels.length,
    declaredCount: declared.size
  }
}

export function checkUiCoverage(slotDir) {
  return guard('slot.uiCoverage', () => {
    const slug = slotSlug(slotDir)
    const listing = path.join(slotDir, 'store', 'listing.json')
    if (!existsSync(listing)) {
      return unverified(
        'slot.uiCoverage',
        `no ${rel(listing)}: the claims this check compares controls against have no source`
      )
    }
    const main = path.join(slotDir, 'app', 'src', 'main', 'java')
    if (!existsSync(main)) {
      return unverified('slot.uiCoverage', `no ${rel(main)}, so no screen or ViewModel could be read`)
    }

    const files = kotlinFiles(main).map((file) => ({
      path: path.relative(main, file).split(path.sep).join('/'),
      source: readText(file)
    }))
    const result = uiCoverageFrom(files, { slug })

    if (result.refusal) return unverified('slot.uiCoverage', result.refusal.detail)
    if (result.problems.length > 0) {
      const detail = result.problems
        .map(
          ({ operation, claim }) =>
            `ViewModel operation '${operation}' has no caller in any screen, but ${claim}. Either ` +
            'restore the control or change store/listing.json, so the listing and the build cannot ' +
            'disagree about what the app does.'
        )
        .join('; ')
      return fail(
        'slot.uiCoverage',
        `${detail}; ${result.problems.length} claimed operation(s) unreachable, ${result.checked} reachable`
      )
    }
    return pass(
      'slot.uiCoverage',
      `every operation the listing claims is reachable from a screen (${result.checked} reachable, ` +
        `${result.notes.length} reported as notes)`
    )
  })
}

// ---------------------------------------------------------------------------------------------
// The inline fixtures — the sensors above, made falsifiable
//
// Every fixture asserts the behaviour its check depends on, so inverting the comparison it makes
// turns it red. Two of them are the refusals of `slot.uiCoverage`: if the empty-ViewModel branch
// returned a pass, the fixture would fail, which is exactly how that defect shipped before.
// ---------------------------------------------------------------------------------------------

/** `<resources>` with one `<string>` per pair, for a fixture's resource tree. */
const stringsXml = (pairs) =>
  `<resources>${pairs.map(([name, value]) => `<string name="${name}">${value}</string>`).join('')}</resources>`

const xml = (name, text) => ({ name, text })

/** A promise contract that parses: three claims, two of them core, all resolved. */
function contract(overrides = {}) {
  const evidence = {
    strength: 'MEASURED',
    source: 'docs/research/2026-09-30-policy-fixture.md#1',
    finding: 'A 1-star review of the leader names the gap: 41 % of the sampled reviews repeat it.'
  }
  const claim = (id, weight, extra = {}) => ({
    id,
    weight,
    promise: `The app does the thing that claim ${id} describes, rather than the incumbent's version of it.`,
    evidence,
    status: 'shipped',
    proof: { type: 'code', ref: 'app/src/main/java/com/gexetr/x/Thing.kt', symbol: 'thing', assertion: 'Thing.kt declares thing, and deleting it fails this gate.' },
    ...extra
  })
  return JSON.stringify({
    schemaVersion: 1,
    slot: 'fixture',
    reconRef: 'docs/research/2026-09-30-policy-fixture.md',
    claims: [claim('a-thing', 'core'), claim('b-thing', 'core'), claim('c-thing', 'supporting')],
    ...overrides
  })
}

const FIXTURE_RECON = 'docs/research/2026-09-30-policy-fixture.md'
const FIXTURE_PROOF = 'app/src/main/java/com/gexetr/x/Thing.kt'

/** The file system `contractProblems` asks about, so a fixture needs no slot on disk. */
const fixtureFs = (present = [FIXTURE_RECON, FIXTURE_PROOF]) => ({
  slug: 'fixture',
  repoRoot: ROOT,
  slotDir: ROOT,
  allowDeferred: false,
  isFile: (candidate) => present.some((entry) => candidate.split('\\').join('/').endsWith(entry)),
  readFile: () => 'object Thing { fun thing() {} }'
})

export function policyFixtures() {
  return guard('policy.fixtures', () => {
    const cases = []

    // ---- slot.locales ----------------------------------------------------------------------
    const base = [xml('strings.xml', stringsXml([['app_name', 'Kiln']]))]
    cases.push([
      'a declared language with no values-<lang>/ directory fails',
      () => {
        const { failures } = localeProblems({ baseFiles: base, declared: ['en', 'ru'], fallback: 'en' })
        expectEqual(failures.length, 1, 'failures')
        expectTrue(failures[0].includes('values-ru'), failures[0])
      }
    ])
    cases.push([
      'a slot that declares no additional language is reported honestly, not as a defect',
      () => {
        // The contract for the only-language case: zero translated languages, no failure. Inverting
        // the `language === fallback` skip would make this a failure naming values-en.
        const result = localeProblems({ baseFiles: base, declared: ['en'], fallback: 'en' })
        expectEqual(result.failures, [], 'failures')
        expectEqual(result.translated, 0, 'translated')
        expectEqual(result.complete, 0, 'complete')
        expectEqual(result.baseKeys, 1, 'baseKeys')
      }
    ])
    cases.push([
      'resources no declaration names fail',
      () => {
        const { failures } = localeProblems({
          baseFiles: base,
          dirs: { ru: 'values-ru' },
          declared: ['en'],
          fallback: 'en',
          localeFiles: { ru: base }
        })
        expectTrue(failures.some((entry) => entry.includes('does not declare')), failures.join('; '))
      }
    ])
    cases.push([
      'a declared language missing from the plural table is refused rather than guessed',
      () => {
        const { failures } = localeProblems({
          baseFiles: base,
          dirs: { xx: 'values-xx' },
          declared: ['en', 'xx'],
          fallback: 'en',
          localeFiles: { xx: base }
        })
        expectTrue(failures.some((entry) => entry.includes('PLURAL_CATEGORIES')), failures.join('; '))
      }
    ])
    cases.push([
      'a non-translatable key declared twice in values/ fails',
      () => {
        const files = [
          xml('strings.xml', '<resources><string name="contact_email" translatable="false">a@b</string></resources>'),
          xml('strings_misc.xml', '<resources><string name="contact_email" translatable="false">c@d</string></resources>')
        ]
        const { failures } = localeProblems({ baseFiles: files, declared: ['en'], fallback: 'en' })
        expectTrue(failures.some((entry) => entry.includes('non-translatable key twice')), failures.join('; '))
      }
    ])
    cases.push([
      'a non-translatable key copied into a locale fails; one only the locale declares is a note',
      () => {
        const baseFiles = [
          xml('strings.xml', stringsXml([['app_name', 'Kiln']]) + '<string name="contact_email" translatable="false">a@b</string>')
        ]
        const localeFiles = {
          ru: [
            xml(
              'strings.xml',
              stringsXml([['app_name', 'Печь']]) +
                '<string name="contact_email" translatable="false">c@d</string>' +
                '<string name="build_flavor" translatable="false">debug</string>'
            )
          ]
        }
        const { failures, notes } = localeProblems({
          baseFiles,
          localeFiles,
          dirs: { ru: 'values-ru' },
          declared: ['en', 'ru'],
          fallback: 'en'
        })
        expectTrue(failures.some((entry) => entry.includes("redeclares 'contact_email'")), failures.join('; '))
        expectTrue(notes.some((entry) => entry.includes("'build_flavor'")), notes.join('; '))
      }
    ])
    cases.push([
      'a missing key, a stale key and a format-argument mismatch all fail',
      () => {
        const baseFiles = [xml('strings.xml', stringsXml([['app_name', 'Kiln'], ['hello', 'Hi %1$s'], ['gone', 'old']]))]
        const localeFiles = {
          ru: [xml('strings.xml', stringsXml([['app_name', 'Печь'], ['hello', 'Привет %1$s %2$s'], ['stale', 'лишний']]))]
        }
        const { failures } = localeProblems({
          baseFiles,
          localeFiles,
          dirs: { ru: 'values-ru' },
          declared: ['en', 'ru'],
          fallback: 'en'
        })
        expectTrue(failures.some((entry) => entry.includes("is missing 'gone'")), failures.join('; '))
        expectTrue(failures.some((entry) => entry.includes("declares 'stale'")), failures.join('; '))
        expectTrue(failures.some((entry) => entry.includes('format arguments')), failures.join('; '))
      }
    ])
    cases.push([
      '%% is a literal and %1$s is an argument, so arguments are compared by position',
      () => {
        expectEqual(formatArgs('50%% off'), [], 'no arguments')
        expectEqual(formatArgs('%1$s of %1$s'), ['%1$s', '%1$s'], 'every occurrence counts, as in the source')
        expectEqual(formatArgs('%2$s %1$s'), ['%1$s', '%2$s'], 'sorted')
        expectTrue(formatArgs('%1$s').join(',') !== formatArgs('%2$s').join(','), 'positions differ')
        expectTrue(
          formatArgs('%1$s of %1$s').join(',') !== formatArgs('%1$s').join(','),
          'a repeated argument is a signature the default does not have'
        )
      }
    ])
    cases.push([
      'a plural category the language selects is required, and a legitimate extra one is a note',
      () => {
        const plurals = (items) =>
          xml('strings.xml', `<resources><plurals name="students">${items}</plurals></resources>`)
        const baseFiles = [plurals('<item quantity="one">%1$d student</item><item quantity="other">%1$d students</item>')]
        const { failures, notes } = localeProblems({
          baseFiles,
          localeFiles: { ru: [plurals('<item quantity="one">%1$d ученик</item><item quantity="other">%1$d учеников</item>')] },
          dirs: { ru: 'values-ru' },
          declared: ['en', 'ru'],
          fallback: 'en'
        })
        expectTrue(failures.some((entry) => entry.includes("no 'few' category")), failures.join('; '))
        expectTrue(failures.some((entry) => entry.includes("no 'many' category")), failures.join('; '))

        const many = localeProblems({
          baseFiles,
          localeFiles: {
            de: [plurals('<item quantity="one">%1$d Schüler</item><item quantity="other">%1$d Schüler</item><item quantity="many">%1$d Schüler</item>')]
          },
          dirs: { de: 'values-de' },
          declared: ['en', 'de'],
          fallback: 'en'
        })
        expectEqual(many.failures, [], 'a plural category CLDR only defines above a million is not a failure')
        expectEqual(many.notes, [], 'nor is it an unused category, so it is not even a note')

        const unused = localeProblems({
          baseFiles,
          localeFiles: {
            de: [plurals('<item quantity="one">%1$d Schüler</item><item quantity="few">%1$d Schüler</item><item quantity="other">%1$d Schüler</item>')]
          },
          dirs: { de: 'values-de' },
          declared: ['en', 'de'],
          fallback: 'en'
        })
        expectEqual(unused.failures, [], 'an extra category is not a failure')
        expectTrue(unused.notes.some((entry) => entry.includes("unused 'few'")), unused.notes.join('; '))
      }
    ])
    cases.push([
      'a plural category whose arguments the default does not use fails',
      () => {
        const plurals = (items) =>
          xml('strings.xml', `<resources><plurals name="students">${items}</plurals></resources>`)
        const { failures } = localeProblems({
          baseFiles: [plurals('<item quantity="one">%1$d</item><item quantity="other">%1$d</item>')],
          localeFiles: { ru: [plurals('<item quantity="one">%1$d</item><item quantity="few">%1$d %2$s</item><item quantity="many">%1$d</item><item quantity="other">%1$d</item>')] },
          dirs: { ru: 'values-ru' },
          declared: ['en', 'ru'],
          fallback: 'en'
        })
        expectTrue(failures.some((entry) => entry.includes('not a signature the default uses')), failures.join('; '))
      }
    ])
    cases.push([
      'a stray % fails only where the string is formatted, and is a note otherwise',
      () => {
        const baseFiles = [
          xml('strings.xml', stringsXml([['weight_label', 'Weight (%)'], ['weight', '%1$s kg  (%)']]))
        ]
        const { failures, notes } = localeProblems({
          baseFiles,
          localeFiles: { ru: [xml('strings.xml', stringsXml([['weight_label', 'Вес (%)'], ['weight', '%1$s кг  (%)']]))] },
          dirs: { ru: 'values-ru' },
          declared: ['en', 'ru'],
          fallback: 'en'
        })
        expectTrue(failures.some((entry) => entry.includes("'weight'")), failures.join('; '))
        expectTrue(failures.some((entry) => entry.includes('throws or mangles')), failures.join('; '))
        expectTrue(notes.some((entry) => entry.includes("'weight_label'")), notes.join('; '))
        expectTrue(notes.some((entry) => entry.includes('never formatted')), notes.join('; '))
      }
    ])
    cases.push([
      'a resource qualifier that is not a language is not a locale directory',
      () => {
        expectEqual(localeDirNames(['values-night', 'values-land', 'values-b+sr+Latn']), {}, 'qualifiers skipped')
        expectEqual(localeDirNames(['values-ru', 'values-zh']), { ru: 'values-ru', zh: 'values-zh' }, 'languages')
      }
    ])

    // ---- slot.differentiators ---------------------------------------------------------------
    cases.push([
      'a completed contract resolves',
      () => {
        const result = contractProblems(contract(), fixtureFs())
        expectEqual(result.errors, [], 'errors')
        expectEqual(result.summary.shipped, 3, 'shipped')
        expectEqual(result.summary.deferred, 0, 'deferred')
      }
    ])
    cases.push([
      'a deferred promise fails, which is what stops ten of twenty reasons being called done',
      () => {
        const value = JSON.parse(contract())
        value.claims[1] = { ...value.claims[1], status: 'deferred', deferredReason: 'not built yet' }
        delete value.claims[1].proof
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.stage, 'refs', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('still deferred')), result.errors.join('; '))
        expectEqual(result.summary.deferred, 1, 'deferred')
      }
    ])
    cases.push([
      'a core promise narrowed without the owner\'s words fails; a supporting one is only explained',
      () => {
        const value = JSON.parse(contract())
        value.claims[0] = {
          ...value.claims[0],
          status: 'narrowed',
          narrowedTo: 'only the class list',
          lostCoverage: 'the student list'
        }
        const missing = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(missing.stage, 'schema', 'stage')
        expectTrue(missing.errors.some((entry) => entry.includes('ownerDecision')), missing.errors.join('; '))

        const words = JSON.parse(JSON.stringify(value))
        words.claims[0].ownerDecision = { words: 'we ship the class list first', recordedAt: '2026-09-30' }
        expectEqual(contractProblems(JSON.stringify(words), fixtureFs()).errors, [], 'owner decision accepted')

        const supporting = JSON.parse(contract())
        supporting.claims[2] = {
          ...supporting.claims[2],
          status: 'narrowed',
          narrowedTo: 'the CSV export only, not the PDF',
          lostCoverage: 'the PDF export entirely'
        }
        expectEqual(
          contractProblems(JSON.stringify(supporting), fixtureFs()).errors,
          [],
          'a supporting promise is explained in writing, not decided by the owner'
        )
      }
    ])
    cases.push([
      'a proof whose symbol no longer appears in its artifact fails',
      () => {
        const options = { ...fixtureFs([FIXTURE_RECON, FIXTURE_PROOF]), readFile: () => 'object Thing {}' }
        const result = contractProblems(contract(), options)
        expectTrue(result.errors.some((entry) => entry.includes('no longer appears')), result.errors.join('; '))
      }
    ])
    cases.push([
      'a proof outside its own type\'s location fails',
      () => {
        const value = JSON.parse(contract())
        value.claims[0].proof = {
          type: 'test',
          ref: 'app/src/main/java/com/gexetr/x/Thing.kt',
          symbol: 'thing',
          assertion: 'Thing.kt declares thing, and deleting it fails this gate.'
        }
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectTrue(result.errors.some((entry) => entry.includes('proof.ref')), result.errors.join('; '))
      }
    ])

    // ---- slot.differentiators: the depth contract (jobCoverage + the handoff floor) -----------
    //
    // The findings this half produces are baseline-scoped, so the two applicability fixtures call
    // `baselineVerdict` directly on the two real slots — a pre-baseline product slot, which must
    // only ever carry a note, and a slot created at the baseline, which must be gated. Both dates
    // come from registry/apps.json through `baseline.mjs`, never from a literal in this file.

    const depthContract = (overrides = {}) => {
      const value = JSON.parse(contract())
      value.jobCoverage = {
        before: { claimId: 'c-thing' },
        during: { claimId: 'b-thing' },
        after: { claimId: 'a-thing' }
      }
      value.claims[0] = { ...value.claims[0], jobStage: 'after', depthFamily: 'data_out' }
      return { ...value, ...overrides }
    }
    const depthFs = (present, readFile = () => 'object Thing { fun thing() {} }') => ({
      ...fixtureFs(present),
      readFile
    })
    // The same contract with its connective claim moved from the exit door to the entry one, so the
    // `data_in` token half of the floor can be driven on its own. `data_in` is the family the camera
    // tokens were added to (`.cursor/rules/app-depth.mdc`), so the legs below are the only fixtures
    // that exercise them.
    const dataInContract = () => {
      const value = depthContract()
      value.claims[0] = { ...value.claims[0], depthFamily: 'data_in' }
      return value
    }

    cases.push([
      'RULE jobCoverage is authored — a contract with no jobCoverage reports the missing-coverage finding',
      () => {
        const findings = depthFindings(JSON.parse(contract()), depthFs())
        expectTrue(findings.some((entry) => entry.includes('no jobCoverage')), findings.join('; '))
      }
    ])

    cases.push([
      'RULE the handoff floor — a contract whose shipped claims are all core_operation reports the floor finding',
      () => {
        expectTrue(!CONNECTIVE_FAMILIES.includes('core_operation'), 'core_operation must never count as connective')
        const value = depthContract()
        value.claims.forEach((claim) => {
          claim.depthFamily = 'core_operation'
        })
        const findings = depthFindings(value, depthFs())
        expectTrue(
          findings.some((entry) => entry.includes('no shipped claim names a connective depth family')),
          findings.join('; ')
        )
      }
    ])

    cases.push([
      'RULE the handoff floor — a data_out claim whose artifact builds a CSV but carries no share intent reports the token finding',
      () => {
        // The measured lawn-round-log shape: `domain/Csv.kt` builds a file, and no ACTION_SEND,
        // ACTION_CREATE_DOCUMENT or FileProvider carries it off the phone.
        const findings = depthFindings(depthContract(), depthFs(undefined, () => 'object Csv { fun csvFor() = "a,b" }'))
        expectTrue(findings.some((entry) => entry.includes('data_out token')), findings.join('; '))
        expectTrue(findings.some((entry) => entry.includes('lawn-round-log shape')), findings.join('; '))
      }
    ])

    cases.push([
      'RULE the handoff floor — the same claim with ACTION_SEND in its artifact clears the floor (no findings)',
      () => {
        const findings = depthFindings(
          depthContract(),
          depthFs(undefined, () => 'Intent(Intent.ACTION_SEND).setType("text/csv")')
        )
        expectEqual(findings, [], 'findings')
      }
    ])

    cases.push([
      'RULE the handoff floor — a data_in claim whose artifact builds a CSV but reaches for no picker or camera reports the token finding',
      () => {
        // The `data_in` mirror of the lawn-round-log shape, written as the same kind of test so the
        // two read alike: the artifact exists and the handoff does not. `data_in` is the door the
        // user's own content comes in through — the picker, the clipboard, or the camera — so a class
        // that only parses a string has none of them, and the floor says so rather than reading the
        // claim's prose. `parseCsv` is deliberately a name the token list cannot match by accident.
        const findings = depthFindings(dataInContract(), depthFs(undefined, () => 'object Importer { fun parseCsv() = "a,b" }'))
        expectTrue(findings.some((entry) => entry.includes('data_in token')), findings.join('; '))
        expectTrue(findings.some((entry) => entry.includes('lawn-round-log shape')), findings.join('; '))
      }
    ])

    cases.push([
      'RULE the handoff floor — each camera token satisfies data_in, because the camera is the same handoff through a different door',
      () => {
        // The picker (`PickVisualMedia`) was already a `data_in` token; the owner's 2026-10-02
        // decision made the phone's free, local camera one too (`.cursor/rules/app-depth.mdc`). This
        // leg drives all three camera tokens the rule names — `ACTION_IMAGE_CAPTURE` (the system
        // camera intent), `TakePicture` (`ActivityResultContracts.TakePicture`) and `ImageCapture`
        // (the CameraX use case) — because a token listed but never matched is the silent no-op this
        // suite exists to catch: the floor would pass on a claim it never actually checked. What it
        // cannot prove, and does not claim, is that the call site runs or that the captured image is
        // used — the token match is textual, the limit the module header states.
        const artifacts = [
          ['ACTION_IMAGE_CAPTURE', 'startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))'],
          ['TakePicture', 'val takePicture = ActivityResultContracts.TakePicture()'],
          ['ImageCapture', 'ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()']
        ]
        for (const [token, source] of artifacts) {
          const findings = depthFindings(dataInContract(), depthFs(undefined, () => source))
          expectEqual(findings, [], `the ${token} artifact was refused`)
        }
      }
    ])

    cases.push([
      'RULE jobCoverage — "during" is the act the app is named for and can never be refused',
      () => {
        const value = depthContract()
        value.jobCoverage.during = { refusal: { reason: 'the visit is the job', decidedBy: 'scout', recordedAt: '2026-10-02' } }
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.stage, 'schema', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('can never be refused')), result.errors.join('; '))
      }
    ])

    cases.push([
      'RULE a refusal is a sentence with a reason, a decider and a date — one missing fails',
      () => {
        const value = depthContract()
        value.jobCoverage.after = { refusal: { reason: 'the job truly ends at the visit', recordedAt: '2026-10-02' } }
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.stage, 'schema', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('refusal.decidedBy')), result.errors.join('; '))
      }
    ])

    // ---- the `after` refusal is the owner's decision, not the scout's (2026-10-02) -------------
    // Refusing `after` discards the handoff, so it needs the owner's own words exactly as weakening
    // a `core` promise does. One fixture per leg: a scout refusing `after` fails, the owner's words
    // pass, an empty owner record fails naming the field, and `before` stays the scout's to refuse.

    cases.push([
      'RULE after refusal — a scout may not refuse the after stage, which is the owner\'s decision',
      () => {
        const value = depthContract()
        value.jobCoverage.after = {
          refusal: {
            reason: 'The round record is finished on the phone and nothing is sent anywhere.',
            decidedBy: 'scout',
            recordedAt: '2026-10-02'
          }
        }
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.stage, 'schema', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('refusal.decidedBy')), result.errors.join('; '))
        expectTrue(result.errors.some((entry) => entry.includes('"owner"')), result.errors.join('; '))
        expectTrue(result.errors.some((entry) => entry.includes('ownerDecision')), result.errors.join('; '))
      }
    ])

    cases.push([
      'RULE after refusal — the owner refusing the after stage in his own words parses',
      () => {
        const value = depthContract()
        value.jobCoverage.after = {
          refusal: {
            reason: 'The round record is finished on the phone and nothing is sent anywhere.',
            decidedBy: 'owner',
            recordedAt: '2026-10-02',
            ownerDecision: {
              words: 'the round record stays on the phone, we ship it without an export',
              recordedAt: '2026-10-02T19:00:00.000Z'
            }
          }
        }
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.errors, [], 'errors')
        expectEqual(result.stage, 'ok', 'stage')
      }
    ])

    cases.push([
      'RULE after refusal — an owner refusal whose owner record is empty fails, naming the field',
      () => {
        const value = depthContract()
        value.jobCoverage.after = {
          refusal: {
            reason: 'The round record is finished on the phone and nothing is sent anywhere.',
            decidedBy: 'owner',
            recordedAt: '2026-10-02',
            ownerDecision: { words: '   ', recordedAt: '2026-10-02' }
          }
        }
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.stage, 'schema', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('ownerDecision.words')), result.errors.join('; '))
      }
    ])

    cases.push([
      'RULE after refusal — a missing owner record fails, while the same refusal on before is the scout\'s',
      () => {
        const value = depthContract()
        value.jobCoverage.after = {
          refusal: {
            reason: 'The round record is finished on the phone and nothing is sent anywhere.',
            decidedBy: 'owner',
            recordedAt: '2026-10-02'
          }
        }
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.stage, 'schema', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('ownerDecision')), result.errors.join('; '))

        const scout = depthContract()
        scout.jobCoverage.before = {
          refusal: {
            reason: 'There is nothing to prepare: the log starts empty on the first drive.',
            decidedBy: 'scout',
            recordedAt: '2026-10-02'
          }
        }
        const before = contractProblems(JSON.stringify(scout), fixtureFs())
        expectEqual(before.errors, [], 'errors')
        expectEqual(before.stage, 'ok', 'stage')
      }
    ])

    cases.push([
      'RULE a refusal reason that is a placeholder token fails; the token check cannot read sincerity',
      () => {
        const value = depthContract()
        value.jobCoverage.before = { refusal: { reason: 'none', decidedBy: 'scout', recordedAt: '2026-10-02' } }
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.stage, 'schema', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('refusal.reason')), result.errors.join('; '))
      }
    ])

    cases.push([
      'RULE coverage resolves — a stage pointing at a claim id nobody declared fails',
      () => {
        const value = depthContract()
        value.jobCoverage.before = { claimId: 'no-such-claim' }
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.stage, 'schema', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('does not name a claim')), result.errors.join('; '))
      }
    ])

    cases.push([
      'RULE the family set is closed — an unknown depthFamily fails, naming the set',
      () => {
        const value = depthContract()
        value.claims[0].depthFamily = 'export'
        const result = contractProblems(JSON.stringify(value), fixtureFs())
        expectEqual(result.stage, 'schema', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('depthFamily')), result.errors.join('; '))
      }
    ])

    cases.push([
      'RULE comparison — a comparison missing its limitation fails, and a complete one parses',
      () => {
        const broken = depthContract()
        broken.claims[0].comparison = { incumbent: 'com.yardbook.app', source: 'docs/research/2026-09-30-policy-fixture.md#1' }
        const result = contractProblems(JSON.stringify(broken), fixtureFs())
        expectEqual(result.stage, 'schema', 'stage')
        expectTrue(result.errors.some((entry) => entry.includes('comparison.limitation')), result.errors.join('; '))

        const complete = depthContract()
        complete.claims[0].comparison = {
          incumbent: 'com.yardbook.app',
          limitation: 'its export is a screenshot rather than a file an accountant can read',
          source: 'docs/research/2026-09-30-policy-fixture.md#1'
        }
        expectEqual(contractProblems(JSON.stringify(complete), fixtureFs()).errors, [], 'a complete comparison was refused')
      }
    ])

    cases.push([
      'RULE baseline — a pre-baseline slot carries a depth finding as a note, never a FAIL',
      () => {
        const dir = path.join(ROOT, 'apps', '2026-09-26-teacher-gradebook')
        const verdict = baselineVerdict('slot.differentiators', dir, ['a depth finding'], 'summary')
        expectEqual(verdict.verdict, VERDICT.PASS, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('note:'), `the finding was not reported as a note: ${verdict.detail}`)
        expectTrue(verdict.detail.includes('2026-09-30 baseline'), `the derivation was not named: ${verdict.detail}`)
      }
    ])

    cases.push([
      'RULE baseline — a slot created at the baseline turns the same depth finding into a FAIL',
      () => {
        const dir = path.join(ROOT, 'apps', '2026-10-01-lawn-round-log')
        const verdict = baselineVerdict('slot.differentiators', dir, ['a depth finding'], 'summary')
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('a depth finding'), `the gated finding was not reported: ${verdict.detail}`)
      }
    ])

    cases.push([
      'RULE the CRUD report is a note, never a finding — a bare record operation prints and does not gate',
      () => {
        const value = JSON.parse(contract())
        value.claims[0].promise = 'Adds a student and deletes a row.'
        const raw = JSON.stringify(value, null, 2)
        expectEqual(crudVerdict(value.claims[0].promise), 'CRUD-shaped', 'crud verdict')
        const report = crudReport(value, { file: 'differentiators.json', raw })
        expectTrue(report.some((entry) => entry.startsWith('differentiators.json:')), report.join('; '))
        expectTrue(report.some((entry) => entry.includes('a-thing')), report.join('; '))

        const gated = depthContract()
        gated.claims[1].promise = 'Adds a student and deletes a row.'
        expectEqual(
          depthFindings(gated, depthFs(undefined, () => 'Intent.ACTION_SEND')),
          [],
          'a CRUD-shaped promise produced a gating finding'
        )
      }
    ])

    cases.push([
      'RULE the CRUD report — the measured edit-every-record row is AUDIT because of its contrast clause, not CRUD-shaped',
      () => {
        const promise =
          'Every class, student, category, assignment, mark, attendance record, note, contact and seat can ' +
          "be created, edited and deleted, instead of the incumbent's name-only editing."
        expectEqual(crudVerdict(promise), 'AUDIT', 'crud verdict')
        const value = JSON.parse(contract())
        value.claims[0].promise = promise
        const report = crudReport(value, { file: 'differentiators.json', raw: JSON.stringify(value) })
        expectEqual(report.length, 1, 'the row was not reported exactly once')
      }
    ])

    cases.push([
      'RULE the CRUD report NEGATIVE CONTROL — a promise that names a handoff is not reported',
      () => {
        expectEqual(crudVerdict('Imports the class list from a CSV the teacher already has.'), null, 'crud verdict')
      }
    ])

    cases.push([
      'the real lawn-round-log contract still parses and its five original claims survive unchanged',
      () => {
        const file = path.join(ROOT, 'apps', '2026-10-01-lawn-round-log', 'differentiators.json')
        const parsed = parseDifferentiators(readFileSync(file, 'utf8'))
        expectTrue(parsed.ok, `the live contract was refused: ${parsed.ok ? '' : parsed.errors.join(' | ')}`)
        // The claim COUNT is deliberately not pinned, and the reason is recorded rather than left to
        // a reader's trust. This fixture proves one direction — the back-compatible read of a real,
        // pre-existing contract — and `.cursor/rules/app-depth.mdc` now REQUIRES that contract to
        // grow a connective claim. A pinned total therefore failed on the contract working as
        // intended (the slot's authorised handoff claim `csv-off-the-phone`, `data_out`, proved by
        // `ACTION_SEND`), which is a drift trap rather than a protection. What IS pinned is the half
        // that carries the regression's value: the five original ids are still present, in their
        // original relative order, so a promise that was reworded away, reordered or dropped is
        // still caught.
        const ids = parsed.value.claims.map((claim) => claim.id)
        const originals = [
          'offline-round-no-account',
          'round-not-a-calendar',
          'owed-per-customer',
          'bought-once',
          'one-handed-in-sunlight'
        ]
        let matched = 0
        for (const id of ids) if (id === originals[matched]) matched += 1
        expectEqual(matched, originals.length, 'the five original claim ids, still present in order')
      }
    ])

    // ---- slot.uiCoverage --------------------------------------------------------------------
    const vm = (dir, body) => ({ path: `${dir}/NotesViewModel.kt`, source: `class NotesViewModel {\n${body}\n}` })
    const screen = (dir, body = '') => ({ path: `${dir}/NotesScreen.kt`, source: `@Composable fun NotesScreen(vm: NotesViewModel) {\n${body}\n}` })

    cases.push([
      'a slot with no ViewModel under ui/ refuses instead of passing',
      () => {
        // The defect this pins: a slot with no ViewModel printed a green line from a check that
        // structurally could not go red (`harness/lessons.md`, factory-review D6).
        const result = uiCoverageFrom([screen('com/gexetr/x/ui/notes')], { slug: 'empty-slot' })
        expectEqual(result.refusal.reason, 'no-viewmodel', 'refusal reason')
        expectTrue(result.refusal.detail.includes('empty-slot'), result.refusal.detail)
        expectTrue(result.refusal.detail.includes('0 of 1'), result.refusal.detail)
        expectTrue(result.refusal.detail.startsWith('empty-slot declares no ViewModel under ui/'), result.refusal.detail)
      }
    ])
    cases.push([
      'a ViewModel whose operations REQUIRED does not name refuses, naming the count',
      () => {
        const result = uiCoverageFrom([vm('com/gexetr/x/ui/notes', '    fun frobnicate() {}')], { slug: 'other-slot' })
        expectEqual(result.refusal.reason, 'not-applicable', 'refusal reason')
        expectTrue(result.refusal.detail.includes('declares 1 ViewModel operation(s)'), result.refusal.detail)
        expectTrue(result.refusal.detail.includes('none of them is named by REQUIRED'), result.refusal.detail)
      }
    ])
    cases.push([
      'a claimed operation no screen reaches is a problem, and a reached one is not',
      () => {
        const dangling = uiCoverageFrom([vm('com/gexetr/x/ui/notes', '    fun addNote(n: String) {}')])
        expectEqual(dangling.problems.length, 1, 'problems')
        expectEqual(dangling.problems[0].operation, 'addNote', 'operation')
        expectTrue(dangling.problems[0].claim.includes('participation notes'), dangling.problems[0].claim)

        const reached = uiCoverageFrom([
          vm('com/gexetr/x/ui/notes', '    fun addNote(n: String) {}'),
          screen('com/gexetr/x/ui/notes', 'vm.addNote("x")')
        ])
        expectEqual(reached.problems, [], 'no problem')
        expectEqual(reached.checked, 1, 'checked')
      }
    ])
    cases.push([
      'a ViewModel\'s own file is not a caller of its own operation',
      () => {
        // Inverting the corpus filter (keeping the ViewModel files in the search corpus) turns this
        // red: the operation would look reachable because it reaches itself.
        const result = uiCoverageFrom([vm('com/gexetr/x/ui/notes', '    fun addNote(n: String) { addNote(n) }')])
        expectEqual(result.problems.length, 1, 'problems')
        expectEqual(result.checked, 0, 'checked')
      }
    ])
    cases.push([
      'an orphan nothing claims and nothing excuses is a note, and NOT_A_CONTROL is not even that',
      () => {
        const result = uiCoverageFrom([
          vm('com/gexetr/x/ui/notes', '    fun addNote(n: String) {}\n    fun frobnicate() {}\n    fun load() {}'),
          screen('com/gexetr/x/ui/notes', 'vm.addNote("x")')
        ])
        expectEqual(result.problems, [], 'no problem')
        expectEqual(result.notes, ['frobnicate'], 'notes')
        expectEqual(result.checked, 1, 'checked')
      }
    ])
    cases.push([
      'a private function is not an operation and cannot satisfy a claim',
      () => {
        expectEqual([...declaredOperations('class V {\n    private fun helper() {}\n    fun run() {}\n}')], ['run'], 'declared')
        const result = uiCoverageFrom([vm('com/gexetr/x/ui/notes', '    private fun addNote(n: String) {}')])
        expectEqual(result.refusal.reason, 'not-applicable', 'a private claim-satisfying control is not one')
        expectEqual(result.refusal.count, 0, 'nothing declared')
      }
    ])
    cases.push([
      'a ui/ file that is not a ViewModel reaches an operation; a file outside ui/ does not',
      () => {
        const inside = uiCoverageFrom([
          vm('com/gexetr/x/ui/notes', '    fun addNote(n: String) {}'),
          screen('com/gexetr/x/ui/notes', 'btn.onClick { vm.addNote("x") }')
        ])
        expectEqual(inside.checked, 1, 'reached from ui/')

        const outside = uiCoverageFrom([
          vm('com/gexetr/x/ui/notes', '    fun addNote(n: String) {}'),
          { path: 'com/gexetr/x/App.kt', source: 'vm.addNote("x")' }
        ])
        expectEqual(outside.checked, 0, 'a caller outside ui/ is not a screen')
        expectEqual(outside.problems.length, 1, 'problems')
      }
    ])

    const failures = []
    for (const [label, body] of cases) {
      try {
        body()
      } catch (error) {
        failures.push(`${label}: ${error.message}`)
      }
    }
    return failures.length === 0
      ? pass('policy.fixtures', `${cases.length} fixtures`)
      : fail('policy.fixtures', failures.join(' | '))
  })
}

/** The three facts the core must still report once the legacy gate is retired. */
export function policyChecks(slotDir) {
  return [checkLocales(slotDir), checkDifferentiators(slotDir), checkUiCoverage(slotDir)]
}

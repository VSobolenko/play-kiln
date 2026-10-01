#!/usr/bin/env node
/**
 * Is every offered language actually complete?
 *
 * `res/xml/locales_config.xml` tells Android - and the per-app language picker - which languages this
 * app speaks. Android itself does not check that claim: a locale with no `values-<locale>/` directory
 * simply falls back to English, so the app appears in the picker, switches to nothing, and looks
 * broken. The same is true string by string, because a missing translation falls back to English rather
 * than failing, so one forgotten key is one English sentence in an otherwise translated screen - the
 * half-translated state a user reads as "unfinished" and a reviewer reads as a mismatch.
 *
 * This checks four things, all mechanically:
 *
 *   1. `locales_config.xml` and the `values-<locale>/` directories name exactly the same languages,
 *      in both directions. A declared language with no resources offers a language that changes
 *      nothing; resources with no declaration are dead weight that also alters locale resolution.
 *   2. Every `<name>` in `values/` exists in every locale, with no extra names - a stale key is a
 *      translation of copy that no longer ships.
 *   3. Format arguments match. A dropped, added, retyped or renumbered `%1$s` is a runtime crash or a
 *      sentence with a hole in it, and neither is visible in a diff review.
 *   4. `<plurals>` provide every CLDR category the language can select for a realistic count. A missing
 *      category is `Resources$NotFoundException` at runtime, not a fallback.
 *
 * Two deliberate design decisions, because both were got wrong on the first attempt:
 *
 *   - **Plurals are compared item by item, not as a union.** A language with four plural categories has
 *     four format strings where English has two, so counting occurrences across the whole `<plurals>`
 *     element reports a mismatch for a correct translation. What must hold is that each item's
 *     argument signature equals the signature of the default's items - which also catches a translator
 *     who drops an argument from one category but keeps it in another.
 *   - **A bare `%` is not by itself a failure.** `Weight (%)` is a label Android never formats, because
 *     `getString(id)` does not process format specifiers, and writing `%%` there would render two
 *     literal percents. A stray `%` only becomes a defect when the string is passed to `String.format`
 *     with arguments, where it throws or mangles the output. So it fails only for a string that has
 *     positional arguments, and is reported as a note otherwise.
 *
 * Usage:
 *   node scripts/check-locales.mjs --slot teacher-gradebook
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

/**
 * The plural categories CLDR can select for an integer count in 0..1000, per language.
 *
 * Source: CLDR plural rules as Android implements them through ICU, reduced to the integer counts this
 * app can produce. A count of 1,000,000 or more adds a `many` category in French, Spanish, Italian,
 * Portuguese, German, Dutch and English, and no roster, mark count or day count reaches that - so
 * `many` is reported as an optional extra for those languages instead of demanded. Russian and Polish
 * need `few` and `many` for ordinary counts (2 students, 5 students), so those are required.
 */
const PLURAL_CATEGORIES = {
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
  // Anything else: correct for languages with no plural distinction. A declared language missing from
  // this table is refused rather than silently under-checked - see `main`.
  zz: ['other'],
}

/** Categories CLDR defines for a language but only for counts this app cannot produce. */
const PLURAL_OPTIONAL = {
  fr: ['many'], es: ['many'], it: ['many'], pt: ['many'],
  en: ['many'], de: ['many'], nl: ['many'],
}

/** Resource qualifiers that look like a language but are not one. */
const NOT_A_LOCALE = new Set([
  'night', 'notnight', 'land', 'port', 'car', 'desk', 'television', 'watch', 'vrheadset',
  'square', 'long', 'notlong', 'round', 'notround', 'highdr', 'lowdr', 'nowidecg', 'widecg',
  'ldrtl', 'ldltr', 'anydpi',
])

function findSlot(slug) {
  const apps = path.join(root, 'apps')
  const matches = fs
    .readdirSync(apps)
    .filter((name) => name === slug || name.endsWith(`-${slug}`))
    .filter((name) => fs.statSync(path.join(apps, name)).isDirectory())
  if (matches.length === 0) throw new Error(`no slot under apps/ matches '${slug}'`)
  if (matches.length > 1) throw new Error(`'${slug}' is ambiguous: ${matches.join(', ')}`)
  return path.join(apps, matches[0])
}

const valuesDir = (slot, qualifier) =>
  path.join(slot, 'app', 'src', 'main', 'res', qualifier ? `values-${qualifier}` : 'values')

/** The language-qualified resource directories, as language tags. */
function localeDirs(slot) {
  const res = path.join(slot, 'app', 'src', 'main', 'res')
  const found = new Map()
  for (const entry of fs.readdirSync(res, { withFileTypes: true })) {
    if (!entry.isDirectory() || !entry.name.startsWith('values-')) continue
    const language = entry.name.slice('values-'.length).split('-')[0].toLowerCase()
    if (NOT_A_LOCALE.has(language) || !/^[a-z]{2,3}$/.test(language)) continue
    if (!found.has(language)) found.set(language, entry.name)
  }
  return found
}

/** The languages `<locale-config>` declares, as language tags. */
function declaredLocales(slot) {
  const file = path.join(slot, 'app', 'src', 'main', 'res', 'xml', 'locales_config.xml')
  if (!fs.existsSync(file)) throw new Error('res/xml/locales_config.xml is missing')
  const declared = new Set()
  for (const match of fs.readFileSync(file, 'utf8').matchAll(/<locale\s+android:name="([^"]+)"/g)) {
    declared.add(match[1].split('-')[0].toLowerCase())
  }
  return declared
}

/**
 * The language the default `values/` is written in.
 *
 * Read from the store listing rather than assumed, because `values/` is the fallback for every locale
 * and its language is what an untranslated string actually looks like. It has no `values-<lang>/` of
 * its own - `en` is served by `values/`, so demanding `values-en/` would be demanding a duplicate of
 * the default and would fail on every correct app.
 */
function defaultLanguage(slot) {
  const listing = path.join(slot, 'store', 'listing.json')
  if (fs.existsSync(listing)) {
    try {
      const tag = String(JSON.parse(fs.readFileSync(listing, 'utf8')).defaultLocale || '')
        .split('-')[0]
        .toLowerCase()
      if (/^[a-z]{2,3}$/.test(tag)) return tag
    } catch {
      // A malformed listing is scripts/store/render.py's business, not this check's; fall through to the default.
    }
  }
  return 'en'
}

/** The positional format arguments in a value, as a sorted list. `%%` is a literal, not an argument. */
function formatArgs(text) {
  const args = []
  for (const match of text.replace(/%%/g, '').matchAll(/%(\d+)\$([a-zA-Z])/g)) {
    args.push(`%${match[1]}$${match[2]}`)
  }
  return args.sort()
}

/** A `%` that is neither `%%` nor a positional argument: only a defect where the string is formatted. */
function nonPositionalSpecifiers(text) {
  return [...text.replace(/%%/g, '').matchAll(/%([^0-9]|\d+[^$])/g)].map((match) => match[0])
}

/**
 * Every string and plurals entry in a resource directory.
 *
 * A deliberately small parser rather than an XML library: these files are machine-shaped, and the only
 * things that matter are the element kind, the name, and the format arguments - per item, for a plural.
 * A `<string translatable="false">` is skipped, because it is not offered for translation.
 */
function readResources(dir) {
  const resources = new Map()
  if (!fs.existsSync(dir)) return resources
  for (const name of fs.readdirSync(dir).filter((entry) => entry.endsWith('.xml')).sort()) {
    const text = fs.readFileSync(path.join(dir, name), 'utf8')
    for (const match of text.matchAll(/<(string|plurals)([^>]*)>([\s\S]*?)<\/\1>/g)) {
      if (/translatable\s*=\s*"false"/.test(match[2])) continue
      const nameMatch = match[2].match(/name\s*=\s*"([^"]+)"/)
      if (!nameMatch) continue
      const key = nameMatch[1]
      if (resources.has(key)) throw new Error(`${dir}: '${key}' is declared more than once (in ${name})`)
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
  return resources
}

/**
 * Keys declared `translatable="false"` in a resource directory, with their source file.
 *
 * These are deliberately not offered for translation: Android resolves them from the default
 * resources by fallback, so a copy in a locale directory cannot take effect and can only drift away
 * from the original. `readResources` skips them, which is why nothing reported the duplication that
 * shipped the generator's `support@example.com` placeholder into the About screen of all twelve
 * files at once while `store/listing.json` advertised the developer's real address. A key that no
 * gate can see is a key that cannot be kept correct.
 */
function scanNonTranslatable(dir) {
  const keys = new Map()
  const repeated = []
  if (!fs.existsSync(dir)) return { keys, repeated }
  for (const name of fs.readdirSync(dir).filter((entry) => entry.endsWith('.xml')).sort()) {
    const text = fs.readFileSync(path.join(dir, name), 'utf8')
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

function main() {
  const slotArgIndex = process.argv.indexOf('--slot')
  if (slotArgIndex === -1 || !process.argv[slotArgIndex + 1]) {
    console.error('usage: node scripts/check-locales.mjs --slot <slug>')
    process.exit(2)
  }
  const slot = findSlot(process.argv[slotArgIndex + 1])
  const base = readResources(valuesDir(slot, null))
  if (base.size === 0) throw new Error('res/values/ holds no strings to translate')
  const nonTranslatable = scanNonTranslatable(valuesDir(slot, null))

  const dirs = localeDirs(slot)
  const declared = declaredLocales(slot)
  const fallback = defaultLanguage(slot)
  const failures = []
  const notes = []

  // A non-translatable key declared twice in the default resources has two candidate values, and
  // `readResources` cannot see either of them, so nothing else here would report it.
  for (const repeated of nonTranslatable.repeated) {
    failures.push(`res/values/ declares a non-translatable key twice: ${repeated}`)
  }

  for (const language of declared) {
    if (language === fallback) continue
    if (!PLURAL_CATEGORIES[language]) {
      failures.push(
        `locales_config.xml offers '${language}' but PLURAL_CATEGORIES in this script has no entry for ` +
          'it, so its plural categories would be guessed rather than checked. Add the categories CLDR ' +
          'defines for that language before declaring it.',
      )
    }
    if (!dirs.has(language)) {
      failures.push(
        `locales_config.xml offers '${language}' but res/values-${language}/ does not exist, so the ` +
          'per-app language picker would offer a language that changes nothing',
      )
    }
  }
  for (const [language, directory] of dirs) {
    if (!declared.has(language)) {
      failures.push(
        `res/${directory}/ exists but locales_config.xml does not declare '${language}', so Android ` +
          'resolves those resources without offering the language',
      )
    }
  }

  let translated = 0
  let complete = 0
  for (const [language, directory] of [...dirs].sort()) {
    if (!declared.has(language)) continue
    complete += 1
    const resources = readResources(valuesDir(slot, directory.slice('values-'.length)))
    if (resources.size === 0) {
      failures.push(`res/${directory}/ contains no strings`)
      continue
    }

    // A non-translatable key copied into a locale is invisible to every other check here, and it
    // cannot change what the user sees, because Android already falls back to the default for it.
    const localeNonTranslatable = scanNonTranslatable(
      valuesDir(slot, directory.slice('values-'.length))
    )
    for (const [key, source] of localeNonTranslatable.keys) {
      if (!nonTranslatable.keys.has(key)) {
        notes.push(
          `res/${directory}/ declares '${key}' (${source}) translatable="false", which values/ does not declare`,
        )
        continue
      }
      failures.push(
        `res/${directory}/ redeclares '${key}' (${source}), which values/ marks ` +
          'translatable="false" - the default is the only source of truth for it, so the two copies ' +
          'can silently diverge and translate a value that was declared untranslatable',
      )
    }

    for (const [key, source] of base) {
      const target = resources.get(key)
      if (!target) {
        failures.push(
          `res/${directory}/ is missing '${key}' (from values/${source.source}), so it falls back to English`,
        )
        continue
      }
      translated += 1
      if (source.kind !== target.kind) {
        failures.push(`res/${directory}/ declares '${key}' as <${target.kind}> but the default is <${source.kind}>`)
        continue
      }

      if (source.kind === 'string') {
        if (source.args.join(',') !== target.args.join(',')) {
          failures.push(
            `res/${directory}/ '${key}' has format arguments [${target.args.join(', ')}] where the default ` +
              `has [${source.args.join(', ')}]; a mismatch crashes or leaves a hole in the sentence`,
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
                'selects for ordinary counts - a missing category throws at runtime rather than falling back',
            )
          }
        }
        for (const [category, args] of target.items) {
          if (!signatures.has(args.join(','))) {
            failures.push(
              `res/${directory}/ <plurals name="${key}"> category '${category}' has format arguments ` +
                `[${args.join(', ')}], which is not a signature the default uses ([${[...signatures].join('] [')}])`,
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
        if (formatted) {
          failures.push(`${message}; the string is formatted, so this throws or mangles the value`)
        } else {
          notes.push(`${message} - harmless here, because this string is never formatted`)
        }
      }
    }

    for (const key of resources.keys()) {
      if (!base.has(key)) {
        failures.push(
          `res/${directory}/ declares '${key}', which values/ no longer has - a translation of copy that no longer ships`,
        )
      }
    }
  }

  for (const note of [...new Set(notes)]) console.log(`NOTE locales - ${note}`)

  if (failures.length > 0) {
    for (const failure of failures) console.error(`FAIL locales - ${failure}`)
    console.error(
      `\nlocales - ${failures.length} problem(s) across ${complete} language(s) and ${base.size} key(s)`,
    )
    process.exit(1)
  }
  console.log(
    `PASS locales - ${complete} translated language(s) x ${base.size} key(s) complete, plus the ` +
      `${fallback} default in values/: ${translated} entries, every format argument and plural ` +
      'category matching the default',
  )
}

try {
  main()
} catch (error) {
  console.error(`BLOCKED locales - ${error.message}`)
  process.exit(2)
}

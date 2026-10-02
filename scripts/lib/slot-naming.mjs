/**
 * The slot naming contract: the name a slot ships, derived rather than invented, with its
 * candidates and its rejected alternatives on the record before any code exists.
 *
 * Why this exists: `scripts/new-slot.mjs --name "<Title>"` accepts free text and nothing validated
 * it, so an agent building a utility could name it `Calculator` — a bare category noun that spends
 * Google Play's strongest ranking field on the head term the incumbent already owns, and gives a
 * user no reason to prefer this app over the leader. `scripts/lib/slot-economics.mjs` and
 * `scripts/lib/slot-differentiators.mjs` already force the money decision and the promise list to be
 * recorded, validated and gated before the UI exists; the name had no such contract. This module is
 * that contract, and it is the single source of truth for it: the CLI gate
 * (`scripts/check-naming.mjs`) and the core check (`scripts/core/naming.mjs`) both import it rather
 * than restating a rule.
 *
 * The measured Play rules and the naming methodology this encodes are
 * `docs/research/2026-10-01-play-title-policy-and-aso.md` and
 * `docs/research/2026-10-01-app-naming-methodology.md`. Every numeric limit below is one of theirs.
 */

import { applicationIdForSlug } from './namespaces.mjs'
// The placeholder vocabulary is the economics contract's, imported rather than copied: "the plan is
// not written yet" must mean one thing in this repository. `verification-discipline.mdc` — one source
// of truth, imported, never restated.
import { PLACEHOLDER_PATTERNS } from './slot-economics.mjs'

export const SCHEMA_VERSION = 1

/** Play's measured cap for the App name field, full-width characters counted the same as half-width. */
export const TITLE_MAX = 30

/** A name that arrives without alternatives was invented, not derived. */
export const MIN_CANDIDATES = 5

/** The candidates must span archetypes, so the field was generated and eliminated rather than defended. */
export const MIN_ARCHETYPES = 3

/** Minimum length of a free-text field; a one-word answer is not a reason. */
export const MIN_TEXT_LENGTH = 12

/** The score a chosen candidate must reach, out of 100 (`app-naming-methodology.md` §3 Step 5). */
export const MIN_CHOSEN_SCORE = 70

/**
 * The nine shapes of `docs/research/2026-10-01-app-naming-methodology.md` §2, in its order.
 *
 * `carrier_job_noun` is the shape the methodology recommends as the default for a small utility: an
 * ownable carrier plus the real two-to-three-word job noun a user types, after a separator. The list
 * is closed because a candidate declaring a shape nobody defined cannot be scored against the field.
 */
export const ARCHETYPES = Object.freeze([
  'descriptive',
  'evocative',
  'coined',
  'compound',
  'metaphorical',
  'eponymous',
  'acronym',
  'borrowed_foreign_word',
  'carrier_job_noun'
])

/**
 * The bare category nouns a name may not be. Hand-maintained, because the list is a judgement about
 * vocabulary rather than something derivable: each entry is a word for a *category* (`methodology.md`
 * §6.1: "a pure generic word or bare category noun") rather than a job, and a title that is exactly
 * one of them competes for the head term the leader owns. `Calculator` is the owner's own example.
 */
export const GENERIC_NOUNS = Object.freeze([
  'calculator', 'calculators',
  'notes', 'note', 'notepad',
  'scanner', 'scan',
  'flashlight', 'torch',
  'converter', 'converters',
  'timer', 'timers', 'stopwatch',
  'clock', 'clocks', 'alarm', 'alarms',
  'calendar', 'calendars', 'planner', 'planners',
  'todo', 'todos', 'tasks', 'task',
  'editor', 'viewer',
  'player', 'recorder', 'recorders',
  'camera', 'cameras', 'gallery', 'galleries',
  'music', 'podcast', 'podcasts', 'radio',
  'browser', 'browsers', 'files', 'file', 'filemanager',
  'wallet', 'budget', 'budgets', 'expenses', 'expense',
  'diary', 'journal', 'journals', 'diaries',
  'tracker', 'trackers', 'tracking',
  'translator', 'dictionary', 'thesaurus',
  'compass', 'ruler', 'level', 'protractor',
  'map', 'maps', 'navigation', 'navigator',
  'contacts', 'phone', 'dialer', 'messenger', 'chat', 'mail', 'email',
  'photos', 'photo', 'video', 'videos', 'images', 'image',
  'pdf', 'reader', 'readers', 'downloader', 'cleaner', 'cleaners',
  'keyboard', 'launcher', 'wallpaper', 'wallpapers',
  'fitness', 'workout', 'workouts', 'steps', 'stepcounter',
  'recipes', 'recipe', 'shopping', 'shoplists', 'shoppinglist',
  'weather', 'news', 'search', 'speedtest', 'vpn', 'antivirus'
])

/** Empty modifiers: they name nothing a user can prefer (`methodology.md` §6.2). */
export const BANNED_AFFIXES = Object.freeze(['my', 'simple', 'easy', 'pro', 'free', 'best'])

/**
 * Store-performance, price and promotional text Play forbids in a title
 * (`docs/research/2026-10-01-play-title-policy-and-aso.md` §1, the best-practices page).
 */
export const PROMO_TERMS = Object.freeze(['#1', 'no.1', 'top', 'best', 'free', 'cheap', 'sale', 'discount'])

/** The free-text fields, each required to read as a sentence rather than a label. */
export const REQUIRED_STRING_FIELDS = Object.freeze([
  'audience',
  'rationale',
  'collisionCheck',
  'localisationCheck'
])

/**
 * Where a name may point for its evidence, mirroring `slot-economics.mjs`'s `EVIDENCE_REF_PATTERN`:
 * the methodology in `docs/research/` or the proposal it came from in `docs/ideation/`. Kept in the
 * same shape so a reviewer reads one pattern, not two.
 */
const EVIDENCE_REF_PATTERN = /^docs\/(?:research|ideation)\/[a-z0-9._-]+\.md(#.+)?$/

const SLUG_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

/** Characters that carry no letter and no digit, used to normalise a title for the word rules. */
const NON_WORD = /[^\p{L}\p{N}\s]+/gu

/** A run of two or more identical non-letter, non-digit characters. */
const REPEATED_SYMBOLS = /([^\p{L}\p{N}\s])\1+/u

/** Pictographic characters and the two symbol blocks a title is most likely to reach for. */
const EMOJI = /[\u{1F000}-\u{1FAFF}\u{2190}-\u{21FF}\u{2300}-\u{27BF}\u{2B00}-\u{2BFF}\u{FE0F}\u{200D}]/u

function isPlainObject(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function describe(value) {
  if (value === undefined) return 'missing'
  if (typeof value === 'string') return `"${value}"`
  return JSON.stringify(value)
}

function matchesPlaceholder(text) {
  return PLACEHOLDER_PATTERNS.some((pattern) => pattern.test(text))
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/** The words of a title, lowercased, punctuation treated as a separator. */
function titleWords(title) {
  return title
    .toLowerCase()
    .replace(NON_WORD, ' ')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
}

/** The promotional terms the title contains, matched as whole words. */
function promoTermsIn(title) {
  const lower = title.toLowerCase()
  return PROMO_TERMS.filter((term) => {
    if (/^[a-z0-9]+$/.test(term)) {
      return new RegExp(`(?<![\\p{L}\\p{N}])${escapeRegExp(term)}(?![\\p{L}\\p{N}])`, 'u').test(lower)
    }
    // `#1` and `no.1` are not words: match the literal, and keep `#1` out of `#12`.
    const tail = /\d$/.test(term) ? '(?!\\d)' : ''
    return new RegExp(`${escapeRegExp(term)}${tail}`, 'u').test(lower)
  })
}

/**
 * The bare-category-noun rule: the title, lowercased and stripped of punctuation, is a single word
 * that appears in `GENERIC_NOUNS`.
 *
 * This is the rule the owner asked for. `Calculator` fails; `Round Log: Lawn Care Rounds` passes,
 * because the second is a phrase about a job and the first is the name of a category.
 */
export function genericNounIn(title) {
  const words = titleWords(title)
  if (words.length !== 1) return null
  return GENERIC_NOUNS.includes(words[0]) ? words[0] : null
}

function validateTitle(value, errors) {
  const raw = value.title
  if (typeof raw !== 'string') {
    errors.push(`title is required and must be a string (found ${describe(raw)})`)
    return
  }
  if (raw !== raw.trim()) {
    errors.push(`title must not have leading or trailing whitespace (found ${describe(raw)})`)
  }
  if (/[\r\n]/.test(raw)) {
    errors.push(`title must be a single line, with no newline (found ${describe(raw)})`)
  }
  const trimmed = raw.trim()
  if (trimmed.length === 0) {
    errors.push('title must not be empty')
    return
  }
  if (trimmed.length > TITLE_MAX) {
    errors.push(
      `title must be at most ${TITLE_MAX} characters, Play's measured App name limit including spaces ` +
        `and any localised name (found ${trimmed.length}: ${describe(trimmed)})`
    )
  }

  const generic = genericNounIn(trimmed)
  if (generic) {
    errors.push(
      `title must not be a bare category noun: "${trimmed}" is the single word "${generic}", which ` +
        'spends Play\'s strongest ranking field on the head term the category leader already owns. ' +
        'Name the job, not the category (docs/research/2026-10-01-app-naming-methodology.md §1)'
    )
  }
  if (/[\p{L}]/u.test(trimmed) && trimmed === trimmed.toUpperCase()) {
    errors.push(`title must not be ALL CAPS (found ${describe(trimmed)})`)
  }
  if (EMOJI.test(trimmed)) {
    errors.push(
      `title must not carry an emoji or pictographic character (found ${describe(trimmed)}); Play's ` +
        'Metadata policy forbids emojis in the app title'
    )
  }
  const repeated = REPEATED_SYMBOLS.exec(trimmed)
  if (repeated) {
    errors.push(
      `title must not carry a run of repeated special characters (found ${describe(repeated[0])} in ` +
        `${describe(trimmed)}); Play's Metadata policy forbids repeated special characters`
    )
  }
  const words = titleWords(trimmed)
  const affixSites = []
  if (BANNED_AFFIXES.includes(words[0])) affixSites.push(`starts with "${words[0]}"`)
  if (BANNED_AFFIXES.includes(words[words.length - 1])) affixSites.push(`ends with "${words[words.length - 1]}"`)
  if (affixSites.length > 0) {
    errors.push(
      `title must not carry an empty modifier: it ${affixSites.join(' and ')} — "My"/"Simple"/"Easy"/` +
        '"Pro"/"Free"/"Best" name nothing a user can prefer ' +
        '(docs/research/2026-10-01-app-naming-methodology.md §6.2)'
    )
  }
  const promos = promoTermsIn(trimmed)
  if (promos.length > 0) {
    errors.push(
      `title must not contain promotional or ranking text (found ${promos.map((term) => `"${term}"`).join(', ')} ` +
        `in ${describe(trimmed)}); Play's Metadata policy forbids price, ranking and promotional text in a title`
    )
  }
}

function validateSlug(value, errors) {
  const raw = value.slug
  if (typeof raw !== 'string') {
    errors.push(`slug is required and must be a string (found ${describe(raw)})`)
    return
  }
  if (!SLUG_PATTERN.test(raw)) {
    errors.push(`slug must be lowercase kebab case matching ${SLUG_PATTERN.source} (found ${describe(raw)})`)
  }
  if (raw.length < 2 || raw.length > 40) {
    errors.push(`slug must be between 2 and 40 characters (found ${raw.length}: ${describe(raw)})`)
  }
}

function validateApplicationId(value, errors) {
  const raw = value.applicationId
  if (typeof raw !== 'string') {
    errors.push(`applicationId is required and must be a string (found ${describe(raw)})`)
    return
  }
  if (typeof value.slug !== 'string') return
  const expected = applicationIdForSlug(value.slug)
  if (raw !== expected) {
    errors.push(
      `applicationId must equal ${describe(expected)} for slug ${describe(value.slug)}, built by the ` +
        `shared helper in scripts/lib/namespaces.mjs (found ${describe(raw)})`
    )
  }
}

function validateArchetype(value, errors) {
  if (!ARCHETYPES.includes(value.archetype)) {
    errors.push(`archetype must be one of: ${ARCHETYPES.join(', ')} (found ${describe(value.archetype)})`)
  }
}

function validateCandidates(value, errors) {
  const candidates = value.candidates
  if (!Array.isArray(candidates)) {
    errors.push(`candidates must be an array of at least ${MIN_CANDIDATES} entries (found ${describe(candidates)})`)
    return
  }
  if (candidates.length < MIN_CANDIDATES) {
    errors.push(
      `candidates must hold at least ${MIN_CANDIDATES} entries: a name that arrives without ` +
        `alternatives was invented, not derived (found ${candidates.length})`
    )
  }
  const archetypesSeen = new Set()
  let chosen = null
  candidates.forEach((candidate, index) => {
    if (!isPlainObject(candidate)) {
      errors.push(`candidates[${index}] must be an object with name, archetype, score and why`)
      return
    }
    if (typeof candidate.name !== 'string' || candidate.name.trim() === '') {
      errors.push(`candidates[${index}].name must be a non-empty string (found ${describe(candidate.name)})`)
    }
    if (!ARCHETYPES.includes(candidate.archetype)) {
      errors.push(
        `candidates[${index}].archetype must be one of: ${ARCHETYPES.join(', ')} (found ${describe(candidate.archetype)})`
      )
    } else {
      archetypesSeen.add(candidate.archetype)
    }
    if (typeof candidate.score !== 'number' || !Number.isFinite(candidate.score) || candidate.score < 0 || candidate.score > 100) {
      errors.push(`candidates[${index}].score must be a number between 0 and 100 (found ${describe(candidate.score)})`)
    }
    if (typeof candidate.why !== 'string' || candidate.why.trim().length < MIN_TEXT_LENGTH) {
      errors.push(
        `candidates[${index}].why must be at least ${MIN_TEXT_LENGTH} characters explaining the score ` +
          `(found ${typeof candidate.why === 'string' ? candidate.why.trim().length : describe(candidate.why)})`
      )
    }
    if (typeof candidate.name === 'string' && candidate.name === value.title) chosen = candidate
  })

  if (archetypesSeen.size < MIN_ARCHETYPES) {
    errors.push(
      `candidates must span at least ${MIN_ARCHETYPES} archetypes, so the field was generated and ` +
        `eliminated rather than defended (found ${archetypesSeen.size}: ${[...archetypesSeen].join(', ') || 'none'})`
    )
  }
  if (typeof value.title === 'string' && chosen === null) {
    errors.push(
      `candidates must include the chosen name: no candidate is named "${value.title}", so the title ` +
        'was not derived from them (candidates: ' +
        candidates
          .filter((candidate) => isPlainObject(candidate) && typeof candidate.name === 'string')
          .map((candidate) => `"${candidate.name}"`)
          .join(', ') +
        ')'
    )
  } else if (chosen && typeof chosen.score === 'number' && chosen.score < MIN_CHOSEN_SCORE) {
    errors.push(
      `the chosen candidate "${chosen.name}" scores ${chosen.score}, below the required ${MIN_CHOSEN_SCORE} ` +
        'out of 100 (docs/research/2026-10-01-app-naming-methodology.md §3 Step 5)'
    )
  }
}

function validateRejected(value, errors) {
  const rejected = value.rejected
  if (!Array.isArray(rejected)) {
    errors.push(`rejected must be an array of at least 1 { name, reason } entry (found ${describe(rejected)})`)
    return
  }
  if (rejected.length === 0) {
    errors.push('rejected must hold at least one alternative with the reason it lost; an empty list is not a decision')
  }
  rejected.forEach((entry, index) => {
    if (!isPlainObject(entry)) {
      errors.push(`rejected[${index}] must be an object with name and reason`)
      return
    }
    if (typeof entry.name !== 'string' || entry.name.trim() === '') {
      errors.push(`rejected[${index}].name must be a non-empty string (found ${describe(entry.name)})`)
    }
    if (typeof entry.reason !== 'string' || entry.reason.trim().length < MIN_TEXT_LENGTH) {
      errors.push(
        `rejected[${index}].reason must be at least ${MIN_TEXT_LENGTH} characters naming why it lost ` +
          `(found ${typeof entry.reason === 'string' ? entry.reason.trim().length : describe(entry.reason)})`
      )
    }
  })
}

function validateSearchPhrases(value, errors) {
  const phrases = value.searchPhrases
  if (!Array.isArray(phrases)) {
    errors.push(`searchPhrases must be an array of at least 2 non-empty strings (found ${describe(phrases)})`)
    return
  }
  if (phrases.length < 2) {
    errors.push(
      `searchPhrases must hold at least 2 phrases real users type, so the name is anchored to demand ` +
        `rather than taste (found ${phrases.length})`
    )
  }
  phrases.forEach((phrase, index) => {
    if (typeof phrase !== 'string' || phrase.trim() === '') {
      errors.push(`searchPhrases[${index}] must be a non-empty string (found ${describe(phrase)})`)
    }
  })
}

function validateRequiredText(value, errors) {
  for (const field of REQUIRED_STRING_FIELDS) {
    const raw = value[field]
    if (raw === undefined || raw === null) {
      errors.push(`${field} is required and must be a string`)
      continue
    }
    if (typeof raw !== 'string') {
      errors.push(`${field} must be a string (found ${typeof raw})`)
      continue
    }
    const trimmed = raw.trim()
    if (!trimmed) {
      errors.push(`${field} must not be empty`)
      continue
    }
    if (matchesPlaceholder(trimmed)) {
      errors.push(`${field} must be a real answer, not a placeholder (found "${trimmed}")`)
    }
    if (trimmed.length < MIN_TEXT_LENGTH) {
      errors.push(
        `${field} must be at least ${MIN_TEXT_LENGTH} characters so it states a reason, not a label ` +
          `(found ${trimmed.length})`
      )
    }
  }
}

function validateEvidence(value, errors) {
  const raw = value.evidence
  if (typeof raw !== 'string') {
    errors.push(
      'evidence is required and must be a string like docs/research/<file>.md#<section> or docs/ideation/<file>.md#<section>'
    )
    return
  }
  const trimmed = raw.trim()
  if (!EVIDENCE_REF_PATTERN.test(trimmed)) {
    errors.push(
      `evidence must be docs/research/<file>.md or docs/ideation/<file>.md with an optional #section (found "${trimmed}")`
    )
    return
  }
  if (matchesPlaceholder(trimmed)) {
    errors.push(`evidence must point at real research, not a placeholder (found "${trimmed}")`)
  }
}

function validateLocalizedTitles(value, errors) {
  const titles = value.localizedTitles
  if (titles === undefined) return
  if (!isPlainObject(titles)) {
    errors.push(`localizedTitles must be an object of locale -> title when present (found ${describe(titles)})`)
    return
  }
  for (const [locale, title] of Object.entries(titles)) {
    if (typeof title !== 'string') {
      errors.push(`localizedTitles["${locale}"] must be a string (found ${describe(title)})`)
      continue
    }
    if (title.length < 1 || title.length > TITLE_MAX) {
      errors.push(
        `localizedTitles["${locale}"] must be between 1 and ${TITLE_MAX} characters, the same Play cap ` +
          `the default title obeys (found ${title.length}: ${describe(title)})`
      )
    }
  }
}

/**
 * Validate a parsed `naming.json`. Returns `{ ok: true, value }` or `{ ok: false, errors }` and never
 * throws for invalid input, so a caller can print every problem at once instead of the first.
 *
 * A string argument is accepted and JSON-parsed (with a leading BOM stripped, exactly as
 * `parseEconomics` does); a parse failure is an error rather than an exception.
 */
export function parseNaming(json) {
  let value = json
  if (typeof value === 'string') {
    try {
      value = JSON.parse(value.replace(/^\uFEFF/, ''))
    } catch (error) {
      return { ok: false, errors: [`naming is not valid JSON: ${error.message}`] }
    }
  }
  if (!isPlainObject(value)) {
    return { ok: false, errors: ['naming must be a JSON object'] }
  }

  const errors = []
  if (value.schemaVersion !== SCHEMA_VERSION) {
    errors.push(`schemaVersion must be exactly ${SCHEMA_VERSION} (found ${describe(value.schemaVersion)})`)
  }
  validateTitle(value, errors)
  validateSlug(value, errors)
  validateApplicationId(value, errors)
  validateArchetype(value, errors)
  validateCandidates(value, errors)
  validateRejected(value, errors)
  validateSearchPhrases(value, errors)
  validateRequiredText(value, errors)
  validateEvidence(value, errors)
  validateLocalizedTitles(value, errors)

  return errors.length === 0 ? { ok: true, value } : { ok: false, errors }
}

/**
 * Cross-check a parsed naming contract against a parsed `store/listing.json`.
 *
 * The subject under test is that a pack cannot ship a different name than the one that was approved:
 * `naming.json` is the decision, `store/listing.json` is what a user reads, and the two are authored
 * weeks apart by different stages.
 *
 * Returns an array of error strings (empty when the pack agrees). A missing locales map or a missing
 * default title is an error rather than a silent pass, because a comparison that cannot be made must
 * not read as one that succeeded.
 */
export function validateNamingAgainstListing(naming, listing) {
  const errors = []
  if (!isPlainObject(listing)) {
    errors.push('listing must be a parsed store/listing.json object for the titles to be compared')
    return errors
  }
  const locales = isPlainObject(listing.locales) ? listing.locales : null
  if (!locales) {
    errors.push('store/listing.json must contain a "locales" object before its titles can be compared with the approved name')
    return errors
  }
  const title = naming && typeof naming.title === 'string' ? naming.title : null
  if (title === null) {
    errors.push('naming.title must be a string before the approved name and the listing title can be compared')
    return errors
  }

  const defaultLocale = typeof listing.defaultLocale === 'string' ? listing.defaultLocale : null
  if (defaultLocale === null) {
    errors.push('store/listing.json must declare defaultLocale, so the pack\'s default title can be compared with the approved name')
  } else {
    const entry = locales[defaultLocale]
    const listingTitle = isPlainObject(entry) && typeof entry.title === 'string' ? entry.title : null
    if (listingTitle === null) {
      errors.push(
        `store/listing.json locales["${defaultLocale}"].title is missing, so the default title cannot ` +
          `be compared with the approved name ${describe(title)}`
      )
    } else if (listingTitle !== title) {
      errors.push(
        `store/listing.json locales["${defaultLocale}"].title is ${describe(listingTitle)} but the ` +
          `approved name is ${describe(title)}; a pack that ships a different name than the one that ` +
          'was approved is the defect this check exists to catch'
      )
    }
  }

  const localized = isPlainObject(naming.localizedTitles) ? naming.localizedTitles : {}
  for (const [locale, value] of Object.entries(localized)) {
    const entry = locales[locale]
    if (!isPlainObject(entry)) continue
    const listingTitle = typeof entry.title === 'string' ? entry.title : null
    if (listingTitle !== value) {
      errors.push(
        `store/listing.json locales["${locale}"].title is ${describe(listingTitle)} but ` +
          `naming.localizedTitles["${locale}"] is ${describe(value)}; a pack that ships a different ` +
          'name than the one that was approved is the defect this check exists to catch'
      )
    }
  }
  return errors
}

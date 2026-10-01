/**
 * The outcomes contract — what a shipped slot actually earned, one observation at a time.
 *
 * Why this exists: `economics.json` has carried numeric `killCriteria` since the money gate was
 * written, and nothing ever evaluated them. A repo-wide search for a reader of `minInstalls` /
 * `minRevenueUsd` found the schema, the documents and one test fixture — so the one stage
 * `AGENTS.md` calls the factory's product ("revenue, not green gates") had no program behind it.
 * The prior-art review reached the same conclusion from the other side
 * (`docs/research/2026-09-29-app-factory-prior-art.md` §2.6, change B4): every portfolio source is
 * unanimous that a portfolio is only a system when the attempts that miss are killed on a clock.
 *
 * This module is the contract for the reading that clock is measured against. It parses
 * `apps/<slot>/store/outcomes.json` and turns those readings plus the slot's own `killCriteria`
 * into one verdict. Play Console is the source of truth for every number here and the factory has
 * no revenue API yet, so a reading is hand-recorded today. That is exactly why a reading must carry
 * `observedAt` and `source`: without them an unmeasured claim would read as an optimistic one, and
 * the repository's rule is that an unmeasured claim is `UNVERIFIED`, never optimistic.
 *
 * The module is deliberately pure — no filesystem, no console, no exit — so
 * `scripts/test-outcomes.mjs` drives every case through the same functions the gate uses and a CLI
 * failure and a test failure cannot drift apart.
 */

/** The only `outcomes.json` schema this module accepts; a bump is a deliberate migration. */
export const SCHEMA_VERSION = 1

/**
 * The source a reading is expected to come from today.
 *
 * It is a default only for the error message and the documents, not an enforced value: a source is
 * required to be non-empty but is not restricted to a list, because a reading may legitimately come
 * from a payout statement or a review page, and refusing those would force an operator to mislabel
 * the record rather than describe it.
 */
export const DEFAULT_SOURCE = 'play-console'

/** The fields every reading must carry. `observedAt` and `source` are what make it evidence. */
export const REQUIRED_READING_FIELDS = Object.freeze([
  'observedAt',
  'source',
  'installCount',
  'activeDeviceCount',
  'revenueUsd'
])

/** Optional integer counters a reading may carry; each is validated only when present. */
export const OPTIONAL_COUNT_FIELDS = Object.freeze(['refundCount', 'ratingCount'])

/**
 * The four verdicts `scripts/check-outcomes.mjs` can print.
 *
 * `NO-DATA` and `TOO-EARLY` are honest refusals rather than passes: a slot nobody has measured and
 * a slot whose window is still open are both unjudgeable, and the fact ledger
 * (`scripts/core/facts.mjs`) already established the convention in this repository that
 * "measured once, unverified now" is reported as `UNVERIFIED` and can never be read as green.
 */
export const VERDICTS = Object.freeze({
  NO_DATA: 'NO-DATA',
  TOO_EARLY: 'TOO-EARLY',
  ON_TRACK: 'ON-TRACK',
  MISSED: 'MISSED'
})

/**
 * A guard verdict for the case where a caller skipped `parseEconomics` and handed this module
 * `killCriteria` that are not integers. It is not a gate verdict: the gate validates the plan before
 * it calls here, so this exists only so a programming error surfaces as "cannot judge" instead of
 * silently comparing a number against `undefined` and reporting `ON-TRACK`.
 */
export const VERDICT_UNVERIFIED = 'UNVERIFIED'

/** The archive path change B4 specifies. Kept in one place so the instruction cannot drift. */
export const KILL_COMMAND = 'node scripts/kiln-status.mjs --kill'

const DAY_MS = 24 * 60 * 60 * 1000
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

function isPlainObject(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function describe(value) {
  if (value === undefined) return 'missing'
  if (typeof value === 'string') return `"${value}"`
  return JSON.stringify(value)
}

/** Today as a UTC calendar date, so a verdict never depends on the machine's locale or timezone. */
export function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

/**
 * UTC midnight of an ISO calendar date.
 *
 * Why: an age computed from local `Date` arithmetic shifts by an hour across a DST boundary and can
 * turn a 60-day window into a 59- or 61-day one, which is the difference between `TOO-EARLY` and a
 * kill decision.
 */
function utcMidnight(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number)
  return Date.UTC(year, month - 1, day)
}

/** A date that exists on the calendar, not merely one that matches the pattern (`2026-02-31`). */
function isCalendarDate(text) {
  if (typeof text !== 'string' || !DATE_PATTERN.test(text)) return false
  const [year, month, day] = text.split('-').map(Number)
  const parsed = new Date(Date.UTC(year, month - 1, day))
  return (
    parsed.getUTCFullYear() === year && parsed.getUTCMonth() === month - 1 && parsed.getUTCDate() === day
  )
}

/**
 * Reduce an `observedAt` or a registry `createdAt` to an ISO calendar date.
 *
 * Why: `registry/apps.json` stores `createdAt` as a full timestamp while a reading is a calendar
 * date, and the window arithmetic compares calendar days. One normaliser keeps the two comparable
 * instead of leaving each caller to slice a string slightly differently.
 */
export function toIsoDate(value) {
  if (typeof value !== 'string') return null
  const candidate = value.trim().slice(0, 10)
  return isCalendarDate(candidate) ? candidate : null
}

/** Whole days between two ISO dates, counted in UTC midnights. */
export function daysBetweenIso(fromIso, toIso) {
  return Math.round((utcMidnight(toIso) - utcMidnight(fromIso)) / DAY_MS)
}

/** The most recent reading by `observedAt`; a caller passes a non-empty array. */
export function newestReading(readings) {
  return [...readings].sort((left, right) => (left.observedAt < right.observedAt ? 1 : -1))[0] || null
}

/** The earliest reading by `observedAt` — the epoch of the observation window once there are two. */
export function firstReading(readings) {
  return [...readings].sort((left, right) => (left.observedAt < right.observedAt ? -1 : 1))[0] || null
}

function validateObservedAt(reading, at, seen, index, today, errors) {
  const raw = reading.observedAt
  if (typeof raw !== 'string' || !isCalendarDate(raw.trim())) {
    errors.push(`${at}.observedAt must be an ISO calendar date (YYYY-MM-DD) (found ${describe(raw)})`)
    return
  }
  const observedAt = raw.trim()
  if (utcMidnight(observedAt) > utcMidnight(today)) {
    errors.push(
      `${at}.observedAt ${observedAt} is after today (${today}); a reading cannot be observed in the future, and a future date is how a stale record hides`
    )
  }
  if (seen.has(observedAt)) {
    errors.push(
      `${at}.observedAt ${observedAt} duplicates readings[${seen.get(observedAt)}].observedAt; one observation per date, so a corrected re-read replaces the row rather than being added beside it`
    )
  } else {
    seen.set(observedAt, index)
  }
}

function validateSource(reading, at, errors) {
  const raw = reading.source
  if (typeof raw !== 'string' || raw.trim() === '') {
    errors.push(
      `${at}.source must be a non-empty string naming where the number came from, for example "${DEFAULT_SOURCE}" (found ${describe(raw)})`
    )
  }
}

function validateCount(reading, at, field, errors) {
  const raw = reading[field]
  if (raw === undefined) {
    errors.push(`${at}.${field} is required and must be an integer >= 0 (found missing)`)
    return
  }
  if (!Number.isInteger(raw)) {
    errors.push(`${at}.${field} must be an integer >= 0 (found ${describe(raw)})`)
    return
  }
  if (raw < 0) {
    errors.push(`${at}.${field} must be >= 0 (found ${raw}); a negative count means the row was mis-entered, not that the app uninstalled itself`)
  }
}

function validateOptionalCount(reading, at, field, errors) {
  const raw = reading[field]
  if (raw === undefined) return
  if (!Number.isInteger(raw) || raw < 0) {
    errors.push(`${at}.${field} must be an integer >= 0 when present (found ${describe(raw)})`)
  }
}

function validateRevenue(reading, at, errors) {
  const raw = reading.revenueUsd
  if (raw === undefined) {
    errors.push(`${at}.revenueUsd is required and must be a finite number >= 0 (found missing)`)
    return
  }
  if (typeof raw !== 'number' || !Number.isFinite(raw)) {
    errors.push(`${at}.revenueUsd must be a finite number >= 0 (found ${describe(raw)})`)
    return
  }
  if (raw < 0) {
    errors.push(`${at}.revenueUsd must be >= 0 (found ${raw}); a refund is recorded as refundCount, not as negative revenue`)
  }
}

function validateConversionRate(reading, at, errors) {
  const raw = reading.conversionRate
  if (raw === undefined) return
  if (typeof raw !== 'number' || !Number.isFinite(raw) || raw < 0 || raw > 1) {
    errors.push(
      `${at}.conversionRate must be a fraction between 0 and 1 when present (found ${describe(raw)}); Play Console shows a percentage, so record 1.9% as 0.019`
    )
  }
}

function validateNotes(reading, at, errors) {
  const raw = reading.notes
  if (raw === undefined) return
  if (typeof raw !== 'string' || raw.trim() === '') {
    errors.push(`${at}.notes must be a non-empty string when present (found ${describe(raw)})`)
  }
}

/**
 * Validate a parsed `store/outcomes.json`.
 *
 * Returns `{ ok: true, value }` or `{ ok: false, errors }`, and never throws for invalid input, so a
 * caller prints every problem at once instead of failing on the first — the same contract
 * `parseEconomics` follows. A string argument is JSON-parsed for convenience.
 *
 * An empty `readings` array is **valid**: a slot that has just shipped has no data yet, and the
 * honest report of that state is the `NO-DATA` verdict rather than a broken file. Rejecting it here
 * would make "not measured yet" indistinguishable from "hand-edited wrongly".
 */
export function parseOutcomes(json) {
  let value = json
  if (typeof value === 'string') {
    try {
      value = JSON.parse(value.replace(/^\uFEFF/, ''))
    } catch (error) {
      return { ok: false, errors: [`outcomes is not valid JSON: ${error.message}`] }
    }
  }
  if (!isPlainObject(value)) {
    return { ok: false, errors: ['outcomes must be a JSON object with a schemaVersion and a readings array'] }
  }

  const errors = []
  if (value.schemaVersion !== SCHEMA_VERSION) {
    errors.push(`schemaVersion must be exactly ${SCHEMA_VERSION} (found ${describe(value.schemaVersion)})`)
  }
  if (!Array.isArray(value.readings)) {
    errors.push(
      'readings must be an array; an empty array is valid (a slot with no data yet is NO-DATA, not a broken file)'
    )
    return errors.length === 0 ? { ok: true, value } : { ok: false, errors }
  }

  const seen = new Map()
  const today = todayIso()
  value.readings.forEach((reading, index) => {
    const at = `readings[${index}]`
    if (!isPlainObject(reading)) {
      errors.push(`${at} must be an object with ${REQUIRED_READING_FIELDS.join(', ')} (found ${describe(reading)})`)
      return
    }
    validateObservedAt(reading, at, seen, index, today, errors)
    validateSource(reading, at, errors)
    validateCount(reading, at, 'installCount', errors)
    validateCount(reading, at, 'activeDeviceCount', errors)
    validateRevenue(reading, at, errors)
    validateConversionRate(reading, at, errors)
    for (const field of OPTIONAL_COUNT_FIELDS) validateOptionalCount(reading, at, field, errors)
    validateNotes(reading, at, errors)
  })

  return errors.length === 0 ? { ok: true, value } : { ok: false, errors }
}

/**
 * The `createdAt` of the slot that produced a reading, read from `registry/apps.json`.
 *
 * Why it is needed: with a single reading there is no observation series to date the window from, so
 * the only honest epoch is when the slot was created. Without this the first reading would make its
 * own window start, and a slot could sit unmeasured forever reporting `TOO-EARLY` while its kill
 * window had in fact long expired.
 */
export function findSlotCreatedAt(registryJson, { slug = null, slotDirRelative = null } = {}) {
  const apps = Array.isArray(registryJson?.apps) ? registryJson.apps : []
  const normalise = (value) => (typeof value === 'string' ? value.replace(/\\/g, '/') : null)
  const wanted = normalise(slotDirRelative)
  const match = apps.find(
    (app) =>
      (wanted && normalise(app?.dir) === wanted) || (slug && typeof app?.id === 'string' && app.id === slug)
  )
  return toIsoDate(match?.createdAt)
}

/**
 * Turn readings plus a slot's own kill criteria into one verdict.
 *
 * Why the order of the checks is what it is: hitting both thresholds early is success, so `ON-TRACK`
 * is tested before the window — a slot that reaches its numbers on day 5 must not be reported
 * `TOO-EARLY` merely because its window is still open. `TOO-EARLY` and `MISSED` are separated
 * strictly by the elapsed window, and an empty reading list is always `NO-DATA`, never `MISSED`: the
 * factory has no evidence either way, and a portfolio rule that kills on missing data would kill
 * every new slot on the day it shipped.
 *
 * The window epoch is the **first reading** once there are two or more, and the registry
 * `createdAt` for a single reading (falling back to that reading's own date when the registry has no
 * entry, which reports `TOO-EARLY` rather than inventing an epoch).
 */
export function evaluateOutcomes({ readings, killCriteria, slug = null, createdAt = null, today = todayIso() } = {}) {
  const windowDays = killCriteria?.windowDays
  const minInstalls = killCriteria?.minInstalls
  const minRevenueUsd = killCriteria?.minRevenueUsd
  if (![windowDays, minInstalls, minRevenueUsd].every((value) => Number.isInteger(value))) {
    return {
      verdict: VERDICT_UNVERIFIED,
      exitCode: 2,
      reason: `killCriteria must carry integer windowDays, minInstalls and minRevenueUsd (found ${describe(killCriteria)})`
    }
  }

  const list = Array.isArray(readings) ? readings.filter((reading) => isPlainObject(reading)) : []
  if (list.length === 0) {
    return {
      verdict: VERDICTS.NO_DATA,
      exitCode: 2,
      readingCount: 0,
      windowDays,
      reason: 'no reading has been recorded, so the kill criteria cannot be judged'
    }
  }

  const newest = newestReading(list)
  const misses = []
  if (newest.installCount < minInstalls) {
    misses.push(`installCount ${newest.installCount} of ${minInstalls} required`)
  }
  if (newest.revenueUsd < minRevenueUsd) {
    misses.push(`revenueUsd ${newest.revenueUsd} of ${minRevenueUsd} required`)
  }

  if (misses.length === 0) {
    return {
      verdict: VERDICTS.ON_TRACK,
      exitCode: 0,
      readingCount: list.length,
      windowDays,
      newest,
      misses,
      reason: `the newest reading ${newest.observedAt} meets both thresholds: installCount ${newest.installCount} >= ${minInstalls}, revenueUsd ${newest.revenueUsd} >= ${minRevenueUsd}`
    }
  }

  const seriesEpoch = list.length >= 2
  const epoch = seriesEpoch ? firstReading(list).observedAt : toIsoDate(createdAt) || newest.observedAt
  const epochSource = seriesEpoch
    ? 'the first reading'
    : toIsoDate(createdAt)
      ? 'the slot\'s registry createdAt'
      : 'the single reading (no registry entry, so the window cannot have started earlier)'
  const elapsedDays = Math.max(0, daysBetweenIso(epoch, today))

  if (elapsedDays < windowDays) {
    return {
      verdict: VERDICTS.TOO_EARLY,
      exitCode: 0,
      readingCount: list.length,
      windowDays,
      elapsedDays,
      epoch,
      epochSource,
      newest,
      misses,
      reason: `${elapsedDays} of ${windowDays} day(s) have elapsed since ${epochSource} ${epoch}, so the window is still open`
    }
  }

  const reason = `missed killCriteria after ${elapsedDays} of ${windowDays} day(s) since ${epochSource} ${epoch} (newest reading ${newest.observedAt}): ${misses.join('; ')}`
  return {
    verdict: VERDICTS.MISSED,
    exitCode: 1,
    readingCount: list.length,
    windowDays,
    elapsedDays,
    epoch,
    epochSource,
    newest,
    misses,
    reason,
    killInstruction: killInstruction(slug, reason)
  }
}

/**
 * The exact command an operator runs when a slot is `MISSED`.
 *
 * Why it is a function and not a printed string: the instruction is the only part of the verdict an
 * operator copies, so a hand-written sentence in the CLI and a hand-written sentence in the document
 * would be two copies of one rule — the drift this repository has recorded more than once.
 */
export function killInstruction(slug, reason) {
  const target = slug || '<slug>'
  return `${KILL_COMMAND} ${target} --reason "${reason}"`
}

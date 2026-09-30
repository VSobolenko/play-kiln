/**
 * The measured-facts ledger: load it, validate it, and answer three questions per fact.
 *
 * This factory's rules are full of facts measured at a moment in time - Play's field limits, the
 * console's inline sentences, a vendor's guidance, a pinned toolchain version - and
 * `docs/research/measured-facts.json` is the record: one entry per fact, with the source to re-open,
 * the date it was last confirmed, the window it may be trusted for, and the repository paths that
 * depend on it. `scripts/check-freshness.mjs` is the CLI over this module and
 * `scripts/test-freshness.mjs` is its offline suite; both read this file rather than restating the
 * contract, so there is exactly one definition of what an entry is.
 *
 * `scripts/check-freshness.mjs` used to hold all of this inline. It moved here for two reasons: the
 * suite can drive a pure module against synthetic ledgers, and the entry contract grew a field that
 * only makes sense beside the loader.
 *
 * ## The three questions
 *
 * `auditFact` answers, for one entry and one `today`:
 *
 *   1. **Is it fresh?** - `stale`, computed from `verifiedAt` and `expiresAfterDays`. "Expires after
 *      N days" means the entry is still inside its window ON day N, and stale on day N+1.
 *   2. **Is the named enforcing program present on disk?** - `enforcedBy.script`, checked against the
 *      repository root. A script that is not there is a broken record (exit 1): the fact claims a
 *      machine check that no longer exists, which is exactly the silent drift this field exists to
 *      catch.
 *   3. **Does the named probe exist in that script's source?** - `enforcedBy.probe` is the exact CLI
 *      argument (or `null` when the fact is enforced by a whole-file rule rather than a probe), and
 *      it must appear as a literal string in the named artifact. A probe that was renamed or removed
 *      is a broken record too.
 *
 * ## `enforcedBy` is required, and `null` is an honest answer
 *
 * Every entry must carry `enforcedBy`. It is either
 *
 *   - `{ "script": "scripts/check-16k.py", "probe": "--selftest" }` - a real artifact that turns the
 *     fact into a machine check, where `script` is a repository-relative path and `probe` is either
 *     a literal in that file or `null`; or
 *   - `null`, which says **no program enforces this fact** - the rule is followed on prose alone. A
 *     `null` needs a `gap` string in the same entry naming what is unenforced, because a record that
 *     cannot explain its own hole is not a record. An unenforced fact is reported as `UNENFORCED`
 *     (exit 2, "measured once, unverified now"), never as a pass: the whole point is that nothing in
 *     the repository can fail when the rule drifts.
 *
 * A `gap` string may also sit beside a real `enforcedBy` when the enforcement covers only part of
 * the claim. That is a note, not a verdict - the fact still has a sensor - but it is printed, because
 * a claim larger than its check is how a partial sensor becomes a false one.
 *
 * The module is parameterised (`root`, `exists`, `readFile`) so the suite can hand it a sandbox
 * filesystem and a synthetic ledger; the defaults are the real repository and the real FS.
 */
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// Resolved from this module rather than from the cwd: the ledger's `whereUsed` and `enforcedBy.script`
// paths are repository-relative, so the root has to be known even when the CLI is invoked from a
// sandbox directory (the self-test does exactly that).
const MODULE_DIR = path.dirname(fileURLToPath(import.meta.url))
export const ROOT = path.resolve(MODULE_DIR, '..', '..')
export const DEFAULT_LEDGER = 'docs/research/measured-facts.json'

/** The entry contract, in one place: an unknown token is how a typo becomes a silent gap. */
export const SOURCE_KINDS = ['play-console', 'vendor-docs', 'eu-legislation', 'repo-measurement']
export const VERIFIED_BY = ['live-console', 'vendor-page', 'primary-legislation', 'measured-in-repo']
export const REQUIRED_KEYS = [
  'id',
  'claim',
  'source',
  'sourceKind',
  'verifiedAt',
  'verifiedBy',
  'expiresAfterDays',
  'whereUsed',
  'enforcedBy'
]

/** kebab-case: the id is a stable handle a work order and a lesson entry can both name. */
export const ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/
export const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

/** The only keys an `enforcedBy` object may carry, so a typo (`probeArg`) is reported not ignored. */
export const ENFORCED_BY_KEYS = ['script', 'probe']

const DAY_MS = 24 * 60 * 60 * 1000

/** UTC midnight of an ISO date, so an age never depends on the machine's timezone or on DST. */
export function utcMidnight(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number)
  return Date.UTC(year, month - 1, day)
}

export function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

export function isPlainObject(value) {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function defaultExists(root) {
  return (relative) =>
    existsSync(path.isAbsolute(relative) ? relative : path.join(root, relative))
}

function defaultReadFile(root) {
  return (relative) => {
    const full = path.isAbsolute(relative) ? relative : path.join(root, relative)
    try {
      return readFileSync(full, 'utf8')
    } catch {
      return null
    }
  }
}

/**
 * Validate the ledger's structure and return either its entries or the list of problems found.
 *
 * Every problem is reported rather than the first one: a ledger edited by hand typically breaks in
 * more than one place, and a watchdog that stops at the first typo turns one repair into several
 * runs. Each problem is a code plus the entry it belongs to, so it can be grepped.
 */
export function loadLedger(relativePath, { root = ROOT, exists } = {}) {
  const isThere = exists || defaultExists(root)
  const full = path.isAbsolute(relativePath) ? relativePath : path.join(root, relativePath)
  if (!isThere(relativePath)) {
    return { problems: [`LEDGER-MISSING: ${relativePath} does not exist at ${full}`], entries: [] }
  }
  let parsed
  try {
    parsed = JSON.parse(readFileSync(full, 'utf8'))
  } catch (error) {
    return {
      problems: [`LEDGER-UNPARSEABLE: ${relativePath} is not valid JSON — ${error.message}`],
      entries: []
    }
  }
  if (!Array.isArray(parsed)) {
    return { problems: [`LEDGER-NOT-ARRAY: ${relativePath} must hold a JSON array of facts`], entries: [] }
  }
  if (parsed.length === 0) {
    return {
      problems: [
        `LEDGER-EMPTY: ${relativePath} holds no facts, and "every entry is fresh" is vacuously true ` +
          'for zero entries, so an empty ledger cannot be a pass'
      ],
      entries: []
    }
  }

  const problems = []
  const seen = new Map()
  const today = todayIso()

  parsed.forEach((entry, index) => {
    const at = `entry[${index}]`
    if (!isPlainObject(entry)) {
      problems.push(`LEDGER-ENTRY-NOT-OBJECT: ${at} is ${Array.isArray(entry) ? 'an array' : typeof entry}`)
      return
    }
    const id = typeof entry.id === 'string' ? entry.id : null
    const where = id ? `${id}` : at
    for (const key of REQUIRED_KEYS) {
      if (!(key in entry)) problems.push(`LEDGER-MISSING-KEY: ${where} has no \`${key}\``)
    }
    if (id === null) {
      problems.push(`LEDGER-MISSING-KEY: ${at} has no usable \`id\``)
    } else if (!ID_PATTERN.test(id)) {
      problems.push(`LEDGER-ID-NOT-KEBAB: ${id} must be lower-case kebab-case`)
    } else if (seen.has(id)) {
      problems.push(`LEDGER-DUPLICATE-ID: ${id} appears at entries ${seen.get(id)} and ${index}`)
    } else {
      seen.set(id, index)
    }
    for (const key of ['claim', 'source']) {
      if (typeof entry[key] !== 'string' || entry[key].trim() === '') {
        problems.push(`LEDGER-FIELD-INVALID: ${where}.${key} must be a non-empty string`)
      }
    }
    if (!SOURCE_KINDS.includes(entry.sourceKind)) {
      problems.push(
        `LEDGER-UNKNOWN-SOURCE-KIND: ${where}.sourceKind is ${JSON.stringify(entry.sourceKind)}; ` +
          `known: ${SOURCE_KINDS.join(', ')}`
      )
    }
    if (!VERIFIED_BY.includes(entry.verifiedBy)) {
      problems.push(
        `LEDGER-UNKNOWN-VERIFIED-BY: ${where}.verifiedBy is ${JSON.stringify(entry.verifiedBy)}; ` +
          `known: ${VERIFIED_BY.join(', ')}`
      )
    }
    if (typeof entry.verifiedAt !== 'string' || !DATE_PATTERN.test(entry.verifiedAt)) {
      problems.push(`LEDGER-VERIFIED-AT-INVALID: ${where}.verifiedAt must be an ISO date (YYYY-MM-DD)`)
    } else if (utcMidnight(entry.verifiedAt) > utcMidnight(today)) {
      // A fact cannot have been confirmed tomorrow; a future date is how a stale entry is hidden.
      problems.push(`LEDGER-VERIFIED-AT-FUTURE: ${where}.verifiedAt ${entry.verifiedAt} is after today (${today})`)
    }
    if (!Number.isInteger(entry.expiresAfterDays) || entry.expiresAfterDays <= 0) {
      problems.push(`LEDGER-EXPIRES-INVALID: ${where}.expiresAfterDays must be a positive integer`)
    }
    if (!Array.isArray(entry.whereUsed) || entry.whereUsed.length === 0) {
      problems.push(`LEDGER-WHERE-USED-INVALID: ${where}.whereUsed must be a non-empty array of repository paths`)
    } else {
      entry.whereUsed.forEach((used) => {
        if (typeof used !== 'string' || used.trim() === '') {
          problems.push(`LEDGER-WHERE-USED-INVALID: ${where}.whereUsed holds a non-string entry`)
          return
        }
        // A fact whose blast radius no longer exists is a record that has stopped being true about
        // the repository, so it is a broken entry to fix rather than a stale fact to re-measure.
        if (!isThere(used)) {
          problems.push(
            `LEDGER-WHERE-USED-MISSING: ${where}.whereUsed names ${used}, which does not exist in the repository`
          )
        }
      })
    }
    problems.push(...enforcedByProblems(entry, where))
  })

  return { problems, entries: parsed }
}

/**
 * The structural half of the `enforcedBy` contract. The on-disk half is `checkEnforcement`, because
 * the two answer different questions: "is the field written correctly?" is a record defect, and
 * "does the file it names exist?" is a sensor that has gone missing.
 */
function enforcedByProblems(entry, where) {
  const problems = []
  if (!('enforcedBy' in entry)) return problems // already reported by REQUIRED_KEYS
  const declared = entry.enforcedBy

  if ('gap' in entry && (typeof entry.gap !== 'string' || entry.gap.trim() === '')) {
    problems.push(`LEDGER-GAP-INVALID: ${where}.gap must be a non-empty string when present`)
  }

  if (declared === null) {
    // `null` is the honest answer, and it must explain itself: a hole nobody named is the state this
    // field exists to end.
    if (typeof entry.gap !== 'string' || entry.gap.trim() === '') {
      problems.push(
        `LEDGER-GAP-MISSING: ${where}.enforcedBy is null and it carries no \`gap\` string, so the record ` +
          'does not say what is unenforced'
      )
    }
    return problems
  }

  if (!isPlainObject(declared)) {
    problems.push(
      `LEDGER-ENFORCED-BY-INVALID: ${where}.enforcedBy must be an object {script, probe} or null, ` +
        `not ${JSON.stringify(declared)}`
    )
    return problems
  }

  for (const key of Object.keys(declared)) {
    if (!ENFORCED_BY_KEYS.includes(key)) {
      problems.push(
        `LEDGER-ENFORCED-BY-UNKNOWN-KEY: ${where}.enforcedBy carries ${JSON.stringify(key)}; ` +
          `known keys: ${ENFORCED_BY_KEYS.join(', ')}`
      )
    }
  }
  if (typeof declared.script !== 'string' || declared.script.trim() === '') {
    problems.push(`LEDGER-ENFORCED-BY-SCRIPT-MISSING: ${where}.enforcedBy.script must be a non-empty path`)
  }
  if (!('probe' in declared)) {
    // Required even when it is `null`: an absent key is indistinguishable from an author who never
    // considered how the fact is probed, which is the ambiguity this field removes.
    problems.push(`LEDGER-ENFORCED-BY-PROBE-MISSING: ${where}.enforcedBy has no \`probe\` (use null for a whole-file rule)`)
  } else if (declared.probe !== null && (typeof declared.probe !== 'string' || declared.probe.trim() === '')) {
    problems.push(`LEDGER-ENFORCED-BY-PROBE-INVALID: ${where}.enforcedBy.probe must be a non-empty string or null`)
  }
  return problems
}

/**
 * The on-disk half: is the named enforcing program present, and does its named probe exist?
 *
 * Returns `{ state, script, probe, gap, problems }` where `state` is `enforced`, `unenforced` (the
 * fact says `null`, so nothing can fail when the rule drifts) or `broken` (a named program or probe
 * is gone). `problems` are the FAIL rows; `unenforced` carries no problems of its own because it is
 * a verdict rather than a defect - the entry did exactly what the contract asks.
 */
export function checkEnforcement(entry, { root = ROOT, exists, readFile } = {}) {
  const isThere = exists || defaultExists(root)
  const read = readFile || defaultReadFile(root)
  const gap = typeof entry.gap === 'string' ? entry.gap : null
  const declared = entry && entry.enforcedBy

  if (declared === null) {
    return { state: 'unenforced', script: null, probe: null, gap, problems: [] }
  }
  if (!isPlainObject(declared)) {
    // The structural problem was reported by `loadLedger`; this function only judges disk.
    return { state: 'broken', script: null, probe: null, gap, problems: [] }
  }

  const script = typeof declared.script === 'string' ? declared.script : null
  const probe = declared.probe === undefined ? null : declared.probe
  const problems = []

  if (script !== null && script.trim() !== '' && !isThere(script)) {
    problems.push(
      `ENFORCE-SCRIPT-MISSING: ${entry.id} names enforcing script ${script}, which does not exist — ` +
        'the fact claims a machine check that is gone'
    )
  }

  if (problems.length === 0 && typeof probe === 'string') {
    const source = read(script)
    if (source === null) {
      problems.push(`ENFORCE-SCRIPT-UNREADABLE: ${entry.id} names ${script}, which exists but could not be read`)
    } else if (!source.includes(probe)) {
      problems.push(
        `ENFORCE-PROBE-ABSENT: ${entry.id} names probe ${JSON.stringify(probe)} in ${script}, and that ` +
          'literal is not in the file — the probe was renamed or removed'
      )
    }
  }

  return {
    state: problems.length > 0 ? 'broken' : 'enforced',
    script,
    probe,
    gap,
    problems
  }
}

/** The timing verdict for one entry: its age, its window, and whether it is still inside it. */
export function describe(entry, today) {
  const ageDays = Math.round((utcMidnight(today) - utcMidnight(entry.verifiedAt)) / DAY_MS)
  const daysRemaining = entry.expiresAfterDays - ageDays
  // "Expires after N days": the entry is still inside its window on day N, and stale on day N+1.
  const stale = ageDays > entry.expiresAfterDays
  return {
    id: entry.id,
    claim: entry.claim,
    source: entry.source,
    sourceKind: entry.sourceKind,
    verifiedAt: entry.verifiedAt,
    verifiedBy: entry.verifiedBy,
    whereUsed: entry.whereUsed,
    expiresAfterDays: entry.expiresAfterDays,
    ageDays,
    daysRemaining,
    staleForDays: stale ? ageDays - entry.expiresAfterDays : 0,
    stale,
    verdict: stale ? 'stale' : 'fresh'
  }
}

/** One fact's three answers, in one row: is it fresh, is its sensor present, does its probe exist? */
export function auditFact(entry, today, options = {}) {
  return { ...describe(entry, today), enforcement: checkEnforcement(entry, options) }
}

/**
 * The whole ledger, judged once.
 *
 * `verdict` precedence is BROKEN > STALE > UNENFORCED > FRESH: a record defect is reported even when
 * a fact is also stale, because there is nothing to re-measure until the record is readable.
 * `exitCode` is 1 for BROKEN, 2 for STALE or UNENFORCED (the two "not a pass, not a code failure"
 * states), 0 otherwise; `--warn-only` handling belongs to the CLI, which is the only caller that has
 * a run to keep unblocked.
 */
export function auditLedger(relativePath, options = {}) {
  const loaded = loadLedger(relativePath, options)
  const today = options.today || todayIso()
  // Only well-formed entries are audited: a non-object entry is reported as a contract problem by
  // `loadLedger`, and asking it for a `verifiedAt` would crash the watchdog instead of reporting it.
  const rows = loaded.entries.filter(isPlainObject).map((entry) => auditFact(entry, today, options))
  const enforcementProblems = rows.flatMap((row) => row.enforcement.problems)
  const stale = rows.filter((row) => row.stale)
  const unenforced = rows.filter((row) => row.enforcement.state === 'unenforced')
  const partial = rows.filter((row) => row.enforcement.state === 'enforced' && row.enforcement.gap)
  const broken = loaded.problems.length > 0 || enforcementProblems.length > 0
  return {
    ledger: relativePath,
    today,
    problems: loaded.problems,
    enforcementProblems,
    rows,
    stale,
    unenforced,
    partial,
    broken,
    verdict: broken ? 'BROKEN' : stale.length > 0 ? 'STALE' : unenforced.length > 0 ? 'UNENFORCED' : 'FRESH',
    exitCode: broken ? 1 : stale.length > 0 || unenforced.length > 0 ? 2 : 0
  }
}

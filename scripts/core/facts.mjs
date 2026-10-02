// The measured-fact ledger: is every fact still enforced by something that exists, and inside its
// re-check window?
//
// This is the successor of the two deleted ledger gates (tasks 5.4 and 17.1/17.4 of
// `openspec/changes/reduce-factory-to-core`):
//
//   deleted   `scripts/check-freshness.mjs` + `scripts/lib/freshness.mjs` — the freshness/coverage gate
//   deleted   `scripts/check-lessons.mjs` + `scripts/lib/lessons.mjs` — the lesson-sensor gate
//   kept      `docs/research/measured-facts.json` — the DATA the core reads (ledger row 17.1)
//   kept      `harness/lessons.md` — a readable record for a reader and a role card (row 17.2). Its
//             gate checked that a filename exists, which is not that a check works, so it is gone;
//             the lessons file is not, and nothing here re-checks it.
//
// What is kept from the deleted gate, and what is deliberately not:
//
//   kept      a fact naming an enforcing program that does not exist is a FAIL naming that fact;
//   kept      an empty ledger is broken, because "every entry is enforced" is vacuously true for zero;
//   kept      a fact whose window has elapsed is reported as `UNVERIFIED` — never a FAIL. A date only
//             a human can re-measure is not a code defect, and blocking a slot on it teaches people
//             to silence the check, which is the failure mode the ledger exists to prevent;
//   kept      a fact that names no enforcing program is reported as `UNENFORCED` and must carry a
//             `gap`, so an honest hole is visible instead of reading as a green row;
//   NOT kept  the exit-code vocabulary (0/1/2) and the standalone CLI. This is one function returning
//             one verdict, and `scripts/verify.mjs` already carries PASS / FAIL / UNVERIFIED. The
//             stale state survives as the `UNVERIFIED` token in the detail, which is the string the
//             old gate's readers actually read.
//
// `enforcedBy` accepts two forms, and the detail says which one each fact used:
//
//   a core check id   e.g. `"enforcedBy": "assets"` — must be one of the ids declared by the registry
//                     in `scripts/core/index.mjs`. This is the form design D7 describes: a fact names
//                     a function the one verify command runs, so a fact cannot claim an enforcement
//                     that nobody performs.
//   a legacy pair     `{ "script": "scripts/check-16k.py", "probe": "--selftest" }` — the script must
//                     exist on disk and, when `probe` is a string, that literal must appear in it.
//                     This is the contract the deleted gate carried, and facts whose enforcement is a
//                     surviving script rather than a core check still use it. The detail counts them
//                     separately, so the migration to the first form is visible in every run rather
//                     than implied by a comment.
//
// Why the registry is read by PARSING `scripts/core/index.mjs` and not by importing `CHECK_IDS` from
// it — the decision the task asked to be justified:
//
//   The day `index.mjs` imports this module in order to run `checkFactLedger` (it does not today;
//   `index.mjs` is owned by another writer), `import { CHECK_IDS } from './index.mjs'` here would close
//   the cycle `index.mjs` → `facts.mjs` → `index.mjs`. ESM does not raise on that cycle: it hands back
//   a partially initialised module, so the failure would surface as `Cannot access 'CHECK_IDS' before
//   initialization` from inside a check — a landmine rather than a guard, and one that appears only
//   in the configuration that matters. Parsing has no phase problem at all, and it states the
//   dependency in one place: the admissible ids are whatever the registry declares.
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fail, guard, pass, ROOT, runFixtures } from './lib.mjs'

/** The record of measured facts. Kept as data; `whereUsed` is what makes its blast radius readable. */
const LEDGER = 'docs/research/measured-facts.json'

/** The check registry whose ids a fact may name. */
const REGISTRY = 'scripts/core/index.mjs'

/**
 * `id: '<name>'`, which is how the registry declares one check.
 *
 * The leading `[\s{,}]` is required rather than optional: the registry writes one entry per line, but
 * only the first is at the start of a line — the other three follow `{ ` on the same line as the
 * object's own spacing. An anchored expression therefore reads one id out of four and would report
 * `assets`, `icon` and `project` as undeclared, turning the three real checks into phantom failures.
 * Requiring a delimiter instead of anchoring also keeps the reader from matching a longer key such as
 * `valid:` or `needsSlot:`.
 */
const REGISTRY_ID = /(?:^|[\s{,])id:[ \t]*(['"])([^'"]+)\1/gm

const DAY_MS = 86_400_000

/** The check ids declared in the registry's source. Exported so a fixture can assert the reader. */
export function checkIdsFromRegistry(source) {
  return [...source.matchAll(REGISTRY_ID)].map((match) => match[2])
}

/** Whole days since the epoch. Windows are compared by day, so "on day N" is still inside it. */
function dayIndex(milliseconds) {
  return Math.floor(milliseconds / DAY_MS)
}

/**
 * The expiry of a fact's re-check window, or null when the entry declares no readable window.
 *
 * "Expires after N days" means the entry is still inside its window **on** day N and stale on day
 * N+1, which is why the comparison downstream is on day indices rather than on instants.
 */
export function windowExpiry(fact) {
  const verified = Date.parse(String(fact?.verifiedAt ?? ''))
  const days = Number(fact?.expiresAfterDays)
  if (!Number.isFinite(verified) || !Number.isInteger(days) || days <= 0) return null
  return { dayIndex: dayIndex(verified) + days, verifiedAt: fact.verifiedAt, days }
}

/**
 * Resolves one fact's `enforcedBy` against the registry and the tree.
 *
 * Returns `{ kind }` — `core`, `script`, or `unenforced` — or `{ problem }` naming what is wrong.
 * A problem is a broken record, not a judgement: the fact claims an enforcement nobody can perform.
 */
export function resolveEnforcement(value, { idSet, ids, exists, readText }) {
  if (value === null || value === undefined) return { kind: 'unenforced' }
  if (typeof value === 'string') {
    const name = value.trim()
    if (name === '') return { kind: 'unenforced' }
    if (idSet.has(name)) return { kind: 'core', label: name }
    return {
      problem:
        `names the check id '${name}', which is not declared by ${REGISTRY} ` +
        `(declared: ${ids.join(', ') || 'none'})`
    }
  }
  if (typeof value === 'object' && !Array.isArray(value)) {
    const problems = []
    let kind = null
    const namedId = value.check ?? value.core
    if (namedId !== undefined) {
      if (typeof namedId !== 'string' || !idSet.has(namedId)) {
        problems.push(
          `names the check id '${String(namedId)}', which is not declared by ${REGISTRY} ` +
            `(declared: ${ids.join(', ') || 'none'})`
        )
      } else {
        kind = 'core'
      }
    }
    const namedScript = value.script
    if (namedScript !== undefined) {
      if (typeof namedScript !== 'string' || namedScript.trim() === '') {
        problems.push('names an empty script path')
      } else if (!exists(namedScript.trim())) {
        problems.push(
          `names the enforcing script ${namedScript.trim()}, which is not on disk — ` +
            'the fact claims a machine check that is gone'
        )
      } else if (value.probe !== null && value.probe !== undefined) {
        if (typeof value.probe !== 'string' || value.probe.trim() === '') {
          problems.push(`names an unusable probe for ${namedScript.trim()}`)
        } else if (!readText(namedScript.trim()).includes(value.probe)) {
          problems.push(
            `names the probe '${value.probe}' in ${namedScript.trim()}, and that literal is not in ` +
              'the file — the probe was renamed or removed'
          )
        } else {
          kind = kind ?? 'script'
        }
      } else {
        kind = kind ?? 'script'
      }
    }
    if (problems.length > 0) return { problem: problems.join('; ') }
    if (kind === null) {
      return {
        problem: 'names no check id and no script, so it asserts an enforcement that cannot be resolved'
      }
    }
    return { kind }
  }
  return { problem: `enforcedBy is a ${typeof value}, which is not a check id or a script record` }
}

/**
 * Judges a parsed ledger. Pure: everything it reads from the world is injected, which is what makes
 * the fixture set below able to exercise the real implementation instead of a copy of it.
 *
 * @param {unknown} facts   the parsed ledger, expected to be an array of entries
 * @param {object} options
 * @param {string[]} options.ids        check ids the registry declares
 * @param {(p: string) => boolean} [options.exists]
 * @param {(p: string) => string} [options.readText]
 * @param {number} [options.now]
 */
export function auditFactLedger(facts, options = {}) {
  const id = 'facts.ledger'
  const ids = Array.isArray(options.ids) ? options.ids : []
  const idSet = new Set(ids)
  const exists = options.exists ?? ((relative) => existsSync(path.join(ROOT, relative)))
  const readText =
    options.readText ?? ((relative) => readFileSync(path.join(ROOT, relative), 'utf8').replace(/^\uFEFF/, ''))
  const now = Number.isFinite(options.now) ? options.now : Date.now()

  if (!Array.isArray(facts)) {
    return fail(id, `the ledger is a ${typeof facts}, not a list of facts — the record is unreadable`)
  }
  if (facts.length === 0) {
    return fail(
      id,
      'the ledger carries no fact, and "every fact is enforced" is vacuously true for zero entries, ' +
        'so an empty ledger is a broken record rather than a clean one'
    )
  }

  const problems = []
  const seen = new Map()
  const unenforced = []
  const coreEnforced = []
  const scriptEnforced = []
  const stale = []
  const unlistedWhereUsed = []

  facts.forEach((fact, index) => {
    const named = typeof fact?.id === 'string' && fact.id.trim() !== ''
    const name = named ? fact.id.trim() : `<entry ${index}>`
    if (!named) problems.push(`${name}: declares no id, so nothing can name this fact`)
    else if (seen.has(fact.id)) {
      problems.push(`${name}: the id is declared twice (entries ${seen.get(fact.id)} and ${index})`)
    } else seen.set(fact.id, index)

    const resolved = resolveEnforcement(fact?.enforcedBy, { idSet, ids, exists, readText })
    if (resolved.problem) problems.push(`${name}: ${resolved.problem}`)
    else if (resolved.kind === 'unenforced') {
      const gap = typeof fact?.gap === 'string' ? fact.gap.trim() : ''
      if (gap === '') {
        problems.push(
          `${name}: names no enforcing program and carries no gap — UNENFORCED is the honest answer ` +
            'only when it says in one sentence what is unenforced'
        )
      } else {
        unenforced.push(`${name} (${gap})`)
      }
    } else if (resolved.kind === 'core') coreEnforced.push(name)
    else scriptEnforced.push(name)

    const expiry = windowExpiry(fact)
    if (expiry === null) {
      problems.push(
        `${name}: verifiedAt '${String(fact?.verifiedAt)}' with expiresAfterDays ` +
          `${String(fact?.expiresAfterDays)} does not describe a window, so this fact can never come due`
      )
    } else if (dayIndex(now) > expiry.dayIndex) {
      stale.push(`${name} (re-checked ${expiry.verifiedAt}, ${expiry.days}-day window)`)
    }

    const whereUsed = Array.isArray(fact?.whereUsed) ? fact.whereUsed : []
    for (const used of whereUsed) {
      if (typeof used === 'string' && used.trim() !== '' && !exists(used)) unlistedWhereUsed.push(used)
    }
  })

  if (problems.length > 0) return fail(id, problems.join('; '))

  const enforced = coreEnforced.length + scriptEnforced.length
  if (enforced === 0) {
    return fail(
      id,
      `not one of the ${facts.length} fact(s) names an enforcement that exists, so the ledger asserts ` +
        'nothing: every entry is UNENFORCED'
    )
  }

  const parts = [
    `${enforced} of ${facts.length} fact(s) name an enforcement that exists — ` +
      `${coreEnforced.length} by a core check id, ${scriptEnforced.length} by a surviving script`
  ]
  if (unenforced.length > 0) {
    parts.push(`${unenforced.length} UNENFORCED, never a pass: ${unenforced.join('; ')}`)
  }
  if (stale.length > 0) {
    parts.push(
      `${stale.length} past the re-check window today, so UNVERIFIED rather than FAIL — ` +
        `re-open the source and re-date the entry: ${stale.join('; ')}`
    )
  }
  if (unlistedWhereUsed.length > 0) {
    parts.push(
      `note: ${unlistedWhereUsed.length} whereUsed path(s) are no longer on disk: ` +
        unlistedWhereUsed.join(', ')
    )
  }
  return pass(id, parts.join('; '))
}

/**
 * Reads the real ledger and the real registry and judges the pair.
 *
 * A missing or unparseable ledger is a FAIL: "nothing was scanned" must not read as "nothing is
 * wrong". `ledgerPath` / `registryPath` / `now` are injectable so a demonstration or a drill can run
 * against a copy without touching the repository's own record.
 */
export function checkFactLedger(options = {}) {
  return guard('facts.ledger', () => {
    const absolute = (candidate) => (path.isAbsolute(candidate) ? candidate : path.join(ROOT, candidate))
    const ledgerRel = options.ledgerPath ?? LEDGER
    const ledgerFile = absolute(ledgerRel)
    if (!existsSync(ledgerFile)) {
      return fail('facts.ledger', `no ledger at ${ledgerRel} — the record is missing, not empty`)
    }
    let facts
    try {
      facts = JSON.parse(readFileSync(ledgerFile, 'utf8').replace(/^\uFEFF/, ''))
    } catch (error) {
      return fail('facts.ledger', `${ledgerRel} does not parse: ${error.message}`)
    }

    const registryRel = options.registryPath ?? REGISTRY
    const registryFile = absolute(registryRel)
    if (!existsSync(registryFile)) {
      return fail('facts.ledger', `no check registry at ${registryRel}, so no id can be resolved`)
    }
    const ids = options.ids ?? checkIdsFromRegistry(readFileSync(registryFile, 'utf8'))
    if (ids.length === 0) {
      return fail(
        'facts.ledger',
        `${registryRel} declares no check id, so every fact naming one would be reported as broken — ` +
          'that is a defect in the reader or the registry, not in the ledger'
      )
    }
    return auditFactLedger(facts, { ids, now: options.now })
  })
}

// ---------------------------------------------------------------------------------------------
// Inline fixtures — the sensor, shown red and then green in both directions
// ---------------------------------------------------------------------------------------------
//
// The deleted gate's suite (`scripts/test-freshness.mjs`) is gone; these are its successor. Every
// case below asserts a broken ledger is caught **and** that the repaired ledger is accepted, because
// a sensor that only ever sees a good record cannot be told apart from one that reads nothing.

const FIXTURE_IDS = ['invariants', 'assets', 'icon', 'project']
const FIXTURE_NOW = Date.parse('2026-09-30T12:00:00Z')
const ALL_PRESENT = () => true
const PROBE_IN_FILE = () => 'usage: --probe --validate-only'

const fact = (over = {}) => ({
  id: 'fixture-fact',
  verifiedAt: '2026-09-29',
  expiresAfterDays: 180,
  whereUsed: [],
  ...over
})

const audit = (facts, over = {}) =>
  auditFactLedger(facts, {
    ids: FIXTURE_IDS,
    exists: ALL_PRESENT,
    readText: PROBE_IN_FILE,
    now: FIXTURE_NOW,
    ...over
  })

function expectVerdict(result, expected, label) {
  if (result.verdict !== expected) {
    throw new Error(`${label}: expected ${expected}, got ${result.verdict} — ${result.detail}`)
  }
}

function expectDetail(result, needle, label) {
  if (!String(result.detail).includes(needle)) {
    throw new Error(`${label}: the detail does not mention ${JSON.stringify(needle)} — ${result.detail}`)
  }
}

function expectNoDetail(result, needle, label) {
  if (String(result.detail).includes(needle)) {
    throw new Error(`${label}: the detail wrongly mentions ${JSON.stringify(needle)} — ${result.detail}`)
  }
}

export function factFixtureCases() {
  const cases = []
  const broken = fact({ id: 'play-something-measured', enforcedBy: 'no-such-check' })
  const repaired = fact({ id: 'play-something-measured', enforcedBy: 'assets' })

  cases.push([
    'a fact naming a check id that does not exist fails, and the FAIL names the fact',
    () => {
      const result = audit([broken])
      expectVerdict(result, 'FAIL', 'broken id')
      expectDetail(result, 'play-something-measured', 'broken id')
      expectDetail(result, 'no-such-check', 'broken id')
    }
  ])
  cases.push([
    'the same fact is accepted once it names a declared id',
    () => {
      const result = audit([repaired])
      expectVerdict(result, 'PASS', 'repaired id')
      expectDetail(result, '1 of 1', 'repaired id')
      expectNoDetail(result, 'no-such-check', 'repaired id')
    }
  ])
  cases.push([
    'the id that exists is resolved from the registry, not whitelisted: adding it makes the same entry pass',
    () => {
      expectVerdict(audit([broken]), 'FAIL', 'before the id is declared')
      const result = audit([broken], { ids: [...FIXTURE_IDS, 'no-such-check'] })
      expectVerdict(result, 'PASS', 'after the id is declared')
      expectDetail(result, '1 by a core check id', 'registry-resolved id')
    }
  ])
  cases.push([
    'a fact naming no enforcing program is reported as unenforced and does not fail while another fact is enforced',
    () => {
      const result = audit([
        fact({ id: 'a-rule-on-prose', enforcedBy: null, gap: 'nothing can read a console toggle' }),
        repaired
      ])
      expectVerdict(result, 'PASS', 'one unenforced fact')
      expectDetail(result, 'a-rule-on-prose', 'unenforced fact')
      expectDetail(result, 'UNENFORCED', 'unenforced fact')
      expectDetail(result, '1 of 2', 'unenforced fact')
    }
  ])
  cases.push([
    'a null enforcedBy with no gap is a broken record rather than a quiet hole',
    () => {
      expectVerdict(audit([fact({ id: 'hollow', enforcedBy: null, gap: '' })]), 'FAIL', 'null without a gap')
      expectDetail(audit([fact({ id: 'hollow', enforcedBy: null, gap: 'why it cannot be checked' })]), 'UNENFORCED', 'null with a gap')
    }
  ])
  cases.push([
    'an all-unenforced ledger is refused, because a ledger that asserts nothing is not a pass',
    () => {
      const holes = [
        fact({ id: 'one', enforcedBy: null, gap: 'a console state nothing reads' }),
        fact({ id: 'two', enforcedBy: null, gap: 'a judgement, not a fact' })
      ]
      const result = audit(holes)
      expectVerdict(result, 'FAIL', 'all unenforced')
      expectDetail(result, 'every entry is UNENFORCED', 'all unenforced')
      expectVerdict(audit([...holes, repaired]), 'PASS', 'one enforceable fact added back')
    }
  ])
  cases.push([
    'a fact past its window is UNVERIFIED in the detail and never a FAIL',
    () => {
      const result = audit([fact({ id: 'measured-long-ago', enforcedBy: 'project', verifiedAt: '2020-01-01' })])
      expectVerdict(result, 'PASS', 'stale fact')
      expectDetail(result, 'UNVERIFIED', 'stale fact')
      expectDetail(result, 'measured-long-ago', 'stale fact')
      const fresh = audit([fact({ id: 'measured-long-ago', enforcedBy: 'project' })])
      expectVerdict(fresh, 'PASS', 'fresh fact')
      expectNoDetail(fresh, 'UNVERIFIED', 'fresh fact')
    }
  ])
  cases.push([
    'the window boundary: inside on day N, stale on day N+1',
    () => {
      const oneDay = [fact({ id: 'window', enforcedBy: 'icon', verifiedAt: '2026-09-29', expiresAfterDays: 1 })]
      expectNoDetail(audit(oneDay), 'UNVERIFIED', 'last day of the window')
      const theDayAfter = audit(oneDay, { now: Date.parse('2026-10-01T12:00:00Z') })
      expectDetail(theDayAfter, 'UNVERIFIED', 'the day after the window')
      expectDetail(theDayAfter, 'window', 'the day after the window')
    }
  ])
  cases.push([
    'a window that does not parse is a broken record, not a fact nobody can re-measure',
    () => {
      expectVerdict(audit([fact({ id: 'timeless', enforcedBy: 'assets', expiresAfterDays: 0 })]), 'FAIL', 'zero window')
      expectVerdict(audit([fact({ id: 'timeless', enforcedBy: 'assets', verifiedAt: 'someday' })]), 'FAIL', 'unparseable date')
    }
  ])
  cases.push([
    'a legacy enforcement is still honoured, and a script that is gone is a FAIL naming it',
    () => {
      const legacy = fact({ id: 'sixteen-kb', enforcedBy: { script: 'scripts/check-16k.py', probe: '--validate-only' } })
      const partial = audit([legacy], { exists: (p) => p !== 'scripts/check-16k.py' })
      expectVerdict(partial, 'FAIL', 'script not on disk')
      expectDetail(partial, 'sixteen-kb', 'script not on disk')
      const present = audit([legacy])
      expectVerdict(present, 'PASS', 'script on disk')
      expectDetail(present, '1 by a surviving script', 'legacy shape counted separately')
    }
  ])
  cases.push([
    'a legacy probe that is no longer in its script is caught, and is accepted once it is there',
    () => {
      const legacy = fact({ id: 'probe', enforcedBy: { script: 'scripts/check-16k.py', probe: '--selftest' } })
      expectVerdict(audit([legacy], { readText: () => 'def main(): pass' }), 'FAIL', 'probe removed')
      expectVerdict(audit([legacy], { readText: () => '# --selftest' }), 'PASS', 'probe present')
    }
  ])
  cases.push([
    'an empty ledger is broken, and so is a ledger that is not a list',
    () => {
      expectVerdict(audit([]), 'FAIL', 'empty ledger')
      expectVerdict(audit({ facts: [] }), 'FAIL', 'non-list ledger')
    }
  ])
  cases.push([
    'a duplicated fact id is refused, because two records of one fact cannot both be true',
    () => {
      const result = audit([repaired, fact({ id: 'play-something-measured', enforcedBy: 'assets' })])
      expectVerdict(result, 'FAIL', 'duplicate id')
      expectDetail(result, 'declared twice', 'duplicate id')
    }
  ])
  cases.push([
    'a missing id is refused, because the FAIL could not name the fact',
    () => {
      const result = audit([fact({ id: undefined, enforcedBy: 'assets' })])
      expectVerdict(result, 'FAIL', 'no id')
      expectDetail(result, '<entry 0>', 'no id')
    }
  ])
  cases.push([
    'the registry reader finds every declared id, including the three that are not at the start of a line',
    () => {
      const source = [
        'export const CHECKS = Object.freeze([',
        '  {',
        "    id: 'invariants',",
        '    needsSlot: false',
        '  },',
        "  { id: 'assets', needsSlot: true },",
        "  { id: 'icon', needsSlot: true },",
        "  { id: 'project', needsSlot: true }",
        '])'
      ].join('\n')
      const ids = checkIdsFromRegistry(source)
      for (const expected of FIXTURE_IDS) {
        if (!ids.includes(expected)) {
          throw new Error(`the reader missed '${expected}' — it read ${JSON.stringify(ids)}`)
        }
      }
      expectVerdict(audit([fact({ enforcedBy: 'project' })]), 'PASS', 'id read from an inline entry')
      expectVerdict(audit([fact({ enforcedBy: 'misread' })]), 'FAIL', 'id absent from the registry')
    }
  ])
  cases.push([
    'a check id reported from inside a group entry is declared too, so a fact can name the specific sensor',
    () => {
      // `reports` is how a group declares the sub-ids its rows carry (`ui` reports `uiux.insets` and
      // `uiux.platform`). The reader already sees them, because they are `id:` fields like any other, and
      // this case makes that a stated contract rather than an accident: a fact naming one is enforced,
      // because the group that reports it is what `verify.mjs` runs.
      const source = "  { id: 'ui', needsSlot: true, reports: [{ id: 'uiux.insets' }, { id: 'uiux.platform' }], run: () => [] }"
      const ids = checkIdsFromRegistry(source)
      for (const expected of ['ui', 'uiux.insets', 'uiux.platform']) {
        if (!ids.includes(expected)) throw new Error(`the reader missed '${expected}': ${JSON.stringify(ids)}`)
      }
      const result = audit([fact({ id: 'edge-to-edge', enforcedBy: 'uiux.insets' })], { ids })
      expectVerdict(result, 'PASS', 'a fact naming a reported sensor id')
      expectDetail(result, 'by a core check id', 'a fact naming a reported sensor id')
    }
  ])
  cases.push([
    'the registry reader does not mistake a longer key for an id declaration',
    () => {
      const ids = checkIdsFromRegistry("  valid: 'yes',\n  needsSlot: true,\n  id: 'assets'")
      if (ids.join(',') !== 'assets') {
        throw new Error(`a longer key was read as an id: ${JSON.stringify(ids)}`)
      }
    }
  ])
  cases.push([
    'the real registry declares one id for each check the core runs, so the ledger has something to name',
    () => {
      const ids = checkIdsFromRegistry(readFileSync(path.join(ROOT, REGISTRY), 'utf8'))
      if (ids.length < 4) {
        throw new Error(`${REGISTRY} declares ${ids.length} id(s): ${JSON.stringify(ids)}`)
      }
      for (const expected of FIXTURE_IDS) {
        if (!ids.includes(expected)) throw new Error(`${REGISTRY} no longer declares '${expected}'`)
      }
    }
  ])
  cases.push([
    'a whereUsed path that is gone is a note, not a failure, because a moved file is not a defect in the fact',
    () => {
      const result = audit([fact({ id: 'used', enforcedBy: 'assets', whereUsed: ['docs/gone.md'] })], {
        exists: (p) => p !== 'docs/gone.md'
      })
      expectVerdict(result, 'PASS', 'stale whereUsed')
      expectDetail(result, 'docs/gone.md', 'stale whereUsed')
      expectNoDetail(audit([fact({ id: 'used', enforcedBy: 'assets', whereUsed: ['docs/gone.md'] })]), 'docs/gone.md', 'path present')
    }
  ])
  return cases
}

/** The fact-ledger sensor as a verdict, so the one verify command runs it like any other check. */
export function factFixtures() {
  return guard('facts.fixtures', () => runFixtures('facts.fixtures', factFixtureCases()))
}

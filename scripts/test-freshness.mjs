#!/usr/bin/env node
/**
 * Offline regression suite for `scripts/check-freshness.mjs`, its library and the ledger it reads.
 *
 *   node scripts/test-freshness.mjs            # the suite
 *   node scripts/test-freshness.mjs --prove    # the suite, plus the sensor proof
 *
 * Why it is built this way: the watchdog's whole value is its verdict on a *stale* fact and on a
 * fact whose enforcing program has gone missing, and a fixture that is fresh and enforced by
 * accident would hide a watchdog that never says either. So every case carries its own defect - a
 * fresh ledger, one aged entry, one unenforced entry, one broken record per malformation this
 * repository has to expect - and the expectations are the exit codes the repository already uses
 * (0 fresh and enforced, 2 stale or unenforced, 1 broken, see `scripts/verify-slot.mjs`).
 *
 * The suites run against ledgers written into a temporary sandbox, so no case can touch
 * `docs/research/measured-facts.json`. `whereUsed` and `enforcedBy.script` are checked for existence
 * against the repository root, which is why the sandbox carries one placeholder path of its own: a
 * mutant copy of the watchdog resolves its root to its own directory, and the same fixture has to be
 * valid there.
 *
 * `--prove` answers the repository's rule that *a check that cannot fail is not a check*: it mutates
 * a copy of the watchdog (or of its library) until one of its guarantees is gone (staleness ignored,
 * exit code pinned to 0, duplicate ids allowed, a missing enforcing script ignored) and reports
 * whether the suite's own expectation would notice. A mutant that survives is a weakened sensor and
 * fails this run.
 */
import { spawnSync } from 'node:child_process'
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(SCRIPT_DIR, '..')
const CHECKER = path.join(SCRIPT_DIR, 'check-freshness.mjs')
const LIB_FILE = path.join(SCRIPT_DIR, 'lib', 'freshness.mjs')
const REAL_LEDGER = path.join(ROOT, 'docs', 'research', 'measured-facts.json')
const PROVE = process.argv.includes('--prove')

let checks = 0
let failures = 0

function check(label, condition, detail) {
  checks += 1
  if (condition) {
    console.log(`  PASS  ${label}`)
  } else {
    failures += 1
    console.log(`  FAIL  ${label}`)
    if (detail) console.log(`        ${detail}`)
  }
}

const SANDBOX = mkdtempSync(path.join(tmpdir(), 'kiln-freshness-'))
/** A `whereUsed` path has to exist; a mutant's root is its own directory, so one lives there too. */
function seedRoot(dir) {
  mkdirSync(dir, { recursive: true })
  writeFileSync(path.join(dir, 'AGENTS.md'), 'placeholder for whereUsed resolution\n', 'utf8')
  return dir
}
seedRoot(SANDBOX)

const DAY_MS = 24 * 60 * 60 * 1000

function isoDaysAgo(days) {
  return new Date(Date.now() - days * DAY_MS).toISOString().slice(0, 10)
}

/** A valid, enforced entry, so each case can break exactly one thing. */
function entry(overrides = {}) {
  return {
    id: 'fixture-fact',
    claim: 'A fixture fact the watchdog is asked to judge.',
    source: 'https://example.invalid/fixture',
    sourceKind: 'vendor-docs',
    verifiedAt: isoDaysAgo(0),
    verifiedBy: 'vendor-page',
    expiresAfterDays: 180,
    whereUsed: ['AGENTS.md'],
    enforcedBy: { script: 'scripts/check-freshness.mjs', probe: null },
    ...overrides
  }
}

/** Write a ledger into the sandbox (raw text, so malformed JSON can be a case too). */
function writeLedger(name, contents, root = SANDBOX) {
  const file = path.join(root, `ledger-${name}.json`)
  writeFileSync(file, typeof contents === 'string' ? contents : `${JSON.stringify(contents, null, 2)}\n`, 'utf8')
  return file
}

function runChecker(ledgerPath, extraArgs = [], { cwd = ROOT, script = CHECKER } = {}) {
  const args = [script, ...(ledgerPath ? ['--ledger', ledgerPath] : []), ...extraArgs]
  const result = spawnSync(process.execPath, args, { cwd, encoding: 'utf8', windowsHide: true })
  return { status: result.status, out: `${result.stdout || ''}${result.stderr || ''}` }
}

console.log('=== T1: a fresh, enforced ledger is a pass ===')
const freshFile = writeLedger('fresh', [entry(), entry({ id: 'fixture-second', expiresAfterDays: 365 })])
const freshRun = runChecker(freshFile)
check('a fresh ledger exits 0', freshRun.status === 0, `exit ${freshRun.status}`)
check('the verdict is FRESH', /VERDICT: FRESH/.test(freshRun.out), freshRun.out.split('\n').slice(-3).join(' | '))
check('both entries are listed', freshRun.out.includes('fixture-fact') && freshRun.out.includes('fixture-second'))
check('a fresh run prints no stale block', !/--- 1 entry\(ies\)/.test(freshRun.out))
// The counterfactual that makes the check above meaningful: the same ledger, aged, must not be a
// pass. If this pair ever agrees, the suite is not measuring freshness at all.
const agedPair = runChecker(writeLedger('fresh-aged', [entry({ verifiedAt: isoDaysAgo(181) })]))
check(
  'the same fixture aged past its window is NOT a pass (the check can fail)',
  agedPair.status === 2,
  `exit ${agedPair.status}`
)

console.log('\n=== T2: one stale entry is exit 2, and the output is the work order ===')
const staleFile = writeLedger('stale', [
  entry({ id: 'fixture-aging', claim: 'A rule Google moves often.', expiresAfterDays: 180, verifiedAt: isoDaysAgo(200) }),
  entry({ id: 'fixture-healthy', source: 'https://example.invalid/healthy' })
])
// 200 days ago with a 180-day window: stale for 20 days, and the boundary is asserted separately.
const staleRun = runChecker(staleFile)
check('one stale entry exits 2', staleRun.status === 2, `exit ${staleRun.status}`)
check('the verdict is STALE', /VERDICT: STALE — 1 of 2 entries/.test(staleRun.out), staleRun.out.split('\n').slice(-2).join(' | '))
check('the stale entry is named', /STALE\s+fixture-aging/.test(staleRun.out))
check('the stale claim is printed', staleRun.out.includes('A rule Google moves often.'))
check('the source to re-open is printed', staleRun.out.includes('https://example.invalid/fixture'))
check('the whereUsed path is printed as part of the work order', /whereUsed[\s\S]*- AGENTS\.md/.test(staleRun.out))
check('the staleness arithmetic is printed', /stale for 20 day\(s\)/.test(staleRun.out), staleRun.out.match(/verifiedAt .*/)?.[0])
check('the fresh entry is not reported stale', !/STALE\s+fixture-healthy/.test(staleRun.out))

console.log('\n=== T2b: the expiry boundary is a window, not a guess ===')
// "Expires after N days" means the entry is still inside its window ON day N.
const onBoundary = runChecker(writeLedger('boundary-in', [entry({ expiresAfterDays: 180, verifiedAt: isoDaysAgo(180) })]))
const pastBoundary = runChecker(writeLedger('boundary-out', [entry({ expiresAfterDays: 180, verifiedAt: isoDaysAgo(181) })]))
check('day N of an N-day window is still fresh', onBoundary.status === 0, `exit ${onBoundary.status}`)
check('day N+1 is stale', pastBoundary.status === 2, `exit ${pastBoundary.status}`)

console.log('\n=== T3: a broken ledger is exit 1, per malformation ===')
const missing = runChecker(path.join(SANDBOX, 'not-here.json'))
check('a missing ledger exits 1', missing.status === 1, `exit ${missing.status}`)
check('a missing ledger is named', missing.out.includes('LEDGER-MISSING'), missing.out.trim().split('\n')[1])

const unparseable = runChecker(writeLedger('unparseable', '{"id": "fixture-fact",}'))
check('unparseable JSON exits 1', unparseable.status === 1, `exit ${unparseable.status}`)
check('unparseable JSON is named', unparseable.out.includes('LEDGER-UNPARSEABLE'))

const notArray = runChecker(writeLedger('not-array', { facts: [entry()] }))
check('a top-level object exits 1', notArray.status === 1, `exit ${notArray.status}`)
check('a top-level object is named', notArray.out.includes('LEDGER-NOT-ARRAY'))

const empty = runChecker(writeLedger('empty', []))
check('an empty ledger exits 1 rather than passing vacuously', empty.status === 1, `exit ${empty.status}`)
check('the empty-ledger reason says why', empty.out.includes('LEDGER-EMPTY'))

const notObject = runChecker(writeLedger('entry-not-object', ['fixture-fact']))
check('a non-object entry exits 1', notObject.status === 1, `exit ${notObject.status}`)
check('a non-object entry is named', notObject.out.includes('LEDGER-ENTRY-NOT-OBJECT'))

const duplicateLedger = writeLedger('duplicate-id', [entry(), entry({ source: 'https://example.invalid/other' })])
const duplicated = runChecker(duplicateLedger)
check('a duplicate id exits 1', duplicated.status === 1, `exit ${duplicated.status}`)
check('the duplicate id is named', duplicated.out.includes('LEDGER-DUPLICATE-ID: fixture-fact'))

const badKind = runChecker(writeLedger('bad-kind', [entry({ sourceKind: 'play-site' })]))
check('an unknown sourceKind exits 1', badKind.status === 1, `exit ${badKind.status}`)
check('the unknown sourceKind is named', badKind.out.includes('LEDGER-UNKNOWN-SOURCE-KIND'), badKind.out.split('\n')[1])
check(
  'the refusal lists the known source kinds, so the fix is not a guess',
  /known: play-console, vendor-docs, eu-legislation, repo-measurement/.test(badKind.out)
)

const badBy = runChecker(writeLedger('bad-by', [entry({ verifiedBy: 'i-read-it' })]))
check('an unknown verifiedBy exits 1', badBy.status === 1, `exit ${badBy.status}`)
check('the unknown verifiedBy is named', badBy.out.includes('LEDGER-UNKNOWN-VERIFIED-BY'))

const badId = runChecker(writeLedger('bad-id', [entry({ id: 'Fixture_Fact' })]))
check('a non-kebab id exits 1', badId.status === 1, `exit ${badId.status}`)
check('the id form is named', badId.out.includes('LEDGER-ID-NOT-KEBAB'))

const noSource = JSON.parse(JSON.stringify([entry()]))
delete noSource[0].source
const missingSource = runChecker(writeLedger('missing-source', noSource))
check('a missing `source` exits 1', missingSource.status === 1, `exit ${missingSource.status}`)
check('the missing key is named', missingSource.out.includes('LEDGER-MISSING-KEY: fixture-fact has no `source`'))

const noWindow = JSON.parse(JSON.stringify([entry()]))
delete noWindow[0].expiresAfterDays
const missingWindow = runChecker(writeLedger('missing-window', noWindow))
check('a missing `expiresAfterDays` exits 1', missingWindow.status === 1, `exit ${missingWindow.status}`)
check(
  'a missing window is never defaulted in code',
  missingWindow.out.includes('LEDGER-MISSING-KEY') && missingWindow.out.includes('expiresAfterDays'),
  missingWindow.out.split('\n')[1]
)

const noEnforcedBy = JSON.parse(JSON.stringify([entry()]))
delete noEnforcedBy[0].enforcedBy
const missingEnforcedBy = runChecker(writeLedger('missing-enforced-by', noEnforcedBy))
check('a missing `enforcedBy` exits 1', missingEnforcedBy.status === 1, `exit ${missingEnforcedBy.status}`)
check(
  'the enforcement field is required, so a fact cannot dodge the question by omitting it',
  missingEnforcedBy.out.includes('LEDGER-MISSING-KEY') && missingEnforcedBy.out.includes('enforcedBy'),
  missingEnforcedBy.out.split('\n')[1]
)

const badDate = runChecker(writeLedger('bad-date', [entry({ verifiedAt: '29-09-2026' })]))
check('a non-ISO verifiedAt exits 1', badDate.status === 1, `exit ${badDate.status}`)
check('the date form is named', badDate.out.includes('LEDGER-VERIFIED-AT-INVALID'))

const futureDate = runChecker(writeLedger('future-date', [entry({ verifiedAt: isoDaysAgo(-30) })]))
check('a future verifiedAt exits 1', futureDate.status === 1, `exit ${futureDate.status}`)
check('a future date is named as the way stale entries hide', futureDate.out.includes('LEDGER-VERIFIED-AT-FUTURE'))

const zeroWindow = runChecker(writeLedger('zero-window', [entry({ expiresAfterDays: 0 })]))
check('a zero window exits 1', zeroWindow.status === 1, `exit ${zeroWindow.status}`)
check('the window form is named', zeroWindow.out.includes('LEDGER-EXPIRES-INVALID'))

const noWhereUsed = runChecker(writeLedger('no-where-used', [entry({ whereUsed: [] })]))
check('an empty whereUsed exits 1', noWhereUsed.status === 1, `exit ${noWhereUsed.status}`)
check('the blast radius is required', noWhereUsed.out.includes('LEDGER-WHERE-USED-INVALID'))

const ghostLedger = writeLedger('ghost-path', [entry({ whereUsed: ['scripts/does-not-exist.mjs'] })])
const ghostPath = runChecker(ghostLedger)
check('a whereUsed path that no longer exists exits 1', ghostPath.status === 1, `exit ${ghostPath.status}`)
check('the missing blast-radius path is named', ghostPath.out.includes('LEDGER-WHERE-USED-MISSING'))

console.log('\n=== T4: --warn-only unblocks a loop without hiding the entry ===')
const warnRun = runChecker(staleFile, ['--warn-only'])
check('--warn-only exits 0 on a stale ledger', warnRun.status === 0, `exit ${warnRun.status}`)
check('--warn-only still names the entry', /STALE\s+fixture-aging/.test(warnRun.out))
check('--warn-only says the exit code was relaxed', /exit 0 because --warn-only/.test(warnRun.out))
check(
  '--warn-only does not excuse a broken ledger',
  runChecker(writeLedger('warn-broken', '{,'), ['--warn-only']).status === 1
)

console.log('\n=== T5: --json is machine-readable, and carries the same verdict ===')
const jsonRun = runChecker(staleFile, ['--json'])
let jsonPayload = null
try {
  jsonPayload = JSON.parse(jsonRun.out)
} catch (error) {
  check('--json emits parseable JSON', false, error.message)
}
if (jsonPayload) {
  check('--json emits parseable JSON', true)
  check('--json reports the entry and stale counts', jsonPayload.entryCount === 2 && jsonPayload.staleCount === 1, JSON.stringify({ entryCount: jsonPayload.entryCount, staleCount: jsonPayload.staleCount }))
  check('--json reports the verdict', jsonPayload.verdict === 'STALE', jsonPayload.verdict)
  check('--json carries the exit code it will use', jsonPayload.exitCode === 2, String(jsonPayload.exitCode))
  const aging = jsonPayload.entries.find((row) => row.id === 'fixture-aging')
  check('--json carries the age, the window and the remainder', aging?.ageDays === 200 && aging?.expiresAfterDays === 180 && aging?.daysRemaining === -20, JSON.stringify(aging))
  check('--json carries the source and the blast radius per entry', aging?.whereUsed?.includes('AGENTS.md') && aging?.source === 'https://example.invalid/fixture', JSON.stringify(aging?.whereUsed))
  check('--json carries the enforcement of each entry', aging?.enforcement?.state === 'enforced' && aging?.enforcement?.script === 'scripts/check-freshness.mjs', JSON.stringify(aging?.enforcement))
} else {
  check('--json reports the entry and stale counts', false, 'no payload')
  check('--json reports the verdict', false, 'no payload')
  check('--json carries the exit code it will use', false, 'no payload')
  check('--json carries the age, the window and the remainder', false, 'no payload')
  check('--json carries the source and the blast radius per entry', false, 'no payload')
  check('--json carries the enforcement of each entry', false, 'no payload')
}
const brokenJson = runChecker(writeLedger('json-broken', '{,'), ['--json'])
let brokenPayload = null
try {
  brokenPayload = JSON.parse(brokenJson.out)
} catch (error) {
  check('--json on a broken ledger also emits parseable JSON', false, error.message)
}
if (brokenPayload) {
  check('--json on a broken ledger also emits parseable JSON', brokenPayload.verdict === 'BROKEN' && brokenPayload.problems.length > 0, JSON.stringify(brokenPayload).slice(0, 200))
} else {
  check('--json on a broken ledger also emits parseable JSON', false, 'no payload')
}

console.log('\n=== T6: --help documents the exit-code contract ===')
const help = runChecker(null, ['--help'])
check('--help exits 0', help.status === 0, `exit ${help.status}`)
check(
  '--help states the three exit codes',
  /0 fresh/.test(help.out) && /2 stale or unenforced/.test(help.out) && /1 the ledger is broken/.test(help.out),
  help.out.trim().split('\n').slice(-1)[0]
)

console.log('\n=== T7: the repository ledger parses, is fresh today, and is canonical ===')
let realRaw = null
let real = null
try {
  realRaw = readFileSync(REAL_LEDGER, 'utf8')
  real = JSON.parse(realRaw)
} catch (error) {
  check('docs/research/measured-facts.json parses', false, error.message)
}
if (real) {
  check('docs/research/measured-facts.json parses', true)
  check('it holds an array of facts', Array.isArray(real) && real.length >= 12, `entries ${real?.length}`)
  // Canonical form, so the file stays diffable and a hand edit that mangles the indent or lets
  // `json.dumps` fall back to `ensure_ascii=True` shows up here rather than as an encoding scar.
  // Line endings are normalised first: `core.autocrlf=true` in this repository may materialise CRLF
  // in a working tree, which is a checkout policy and not a property of the ledger's content.
  const canonical = `${JSON.stringify(real, null, 2)}\n`
  check(
    'the ledger is byte-identical to its canonical JSON form',
    realRaw.replace(/\r\n/g, '\n') === canonical,
    `line endings: ${realRaw.includes('\r\n') ? 'CRLF (normalised)' : 'LF'}`
  )
  check('the ledger stores non-ASCII literally, not as \\u escapes', !/\\u[0-9a-fA-F]{4}/.test(realRaw))
  const windows = [...new Set(real.map((row) => row.expiresAfterDays))].sort((a, b) => a - b)
  check(
    'every window is one of the two documented defaults (180 moving, 365 long-lived)',
    windows.every((value) => value === 180 || value === 365),
    `windows ${windows.join(', ')}`
  )
  // The facts the factory's own documents rely on. Naming them keeps a ledger from being trimmed
  // down to whatever happens to pass today.
  const required = [
    'play-screenshot-canvas-targets',
    'play-screenshot-count-and-size-windows',
    'play-screenshot-byte-cap',
    'play-release-notes-multilang-block',
    'play-app-content-declarations',
    'play-first-publication-declarations',
    'play-release-country-list-required',
    'eu-geo-blocking-regulation-2018-302',
    'android-16kb-page-size-readiness',
    'factory-toolchain-pin',
    'play-screenshot-desktop-xr-optional'
  ]
  const ids = new Set(real.map((row) => row.id))
  const absent = required.filter((id) => !ids.has(id))
  check('the ledger still carries the facts the factory relies on', absent.length === 0, `absent: ${absent.join(', ')}`)
}
const realJson = runChecker(null, ['--json'])
let realPayload = null
try {
  realPayload = JSON.parse(realJson.out)
} catch (error) {
  check('the repository ledger is machine-readable', false, error.message)
}
if (realPayload) {
  check('the repository ledger is machine-readable', true)
  check('the repository ledger is fresh today (no stale entry)', realPayload.staleCount === 0, `stale ${realPayload.staleCount}`)
  check(
    'every unenforced fact carries a gap that names what is unenforced',
    realPayload.entries.every(
      (row) => row.enforcement.state !== 'unenforced' || (typeof row.enforcement.gap === 'string' && row.enforcement.gap.length > 0)
    ),
    JSON.stringify(realPayload.entries.filter((row) => row.enforcement.state === 'unenforced').map((row) => row.id))
  )
  check(
    'a fact with no enforcing program is UNVERIFIED, never a pass',
    realJson.status === (realPayload.unenforcedCount > 0 ? 2 : 0),
    `exit ${realJson.status}, unenforced ${realPayload.unenforcedCount}`
  )
  check(
    'no fact in the repository ledger names a missing program or probe',
    realPayload.enforcementProblems.length === 0,
    JSON.stringify(realPayload.enforcementProblems)
  )
}
const realRun = runChecker(null)
check(
  'the repository ledger is FRESH or UNENFORCED, never STALE or BROKEN',
  /VERDICT: (FRESH|UNENFORCED)/.test(realRun.out),
  realRun.out.split('\n').slice(-2).join(' | ')
)
const fromElsewhere = runChecker(null, [], { cwd: SANDBOX })
check(
  'the ledger and its whereUsed paths resolve from any cwd',
  fromElsewhere.status === realRun.status,
  `exit ${fromElsewhere.status} — ${fromElsewhere.out.split('\n').filter(Boolean).slice(-3).join(' | ')}`
)

console.log('\n=== T8: a fact must name the program that turns it into a check ===')
// A probe that is a literal in the named program is a pass...
const probeOk = runChecker(
  writeLedger('probe-ok', [entry({ enforcedBy: { script: 'scripts/check-freshness.mjs', probe: '--warn-only' } })])
)
check('a probe that exists in the named program is a pass', probeOk.status === 0, `exit ${probeOk.status}`)
// ...and the counterfactual is what makes the check a check: the same fact with a probe that is not
// in the file must fail, and name it.
const probeAbsentFile = writeLedger(
  'probe-absent',
  [entry({ enforcedBy: { script: 'scripts/check-freshness.mjs', probe: '--this-flag-does-not-exist' } })]
)
const probeAbsent = runChecker(probeAbsentFile)
check('a probe absent from the named program exits 1', probeAbsent.status === 1, `exit ${probeAbsent.status}`)
check('the missing probe is named', probeAbsent.out.includes('ENFORCE-PROBE-ABSENT'), probeAbsent.out.trim().split('\n')[0])
check('the probe refusal quotes the literal it looked for', probeAbsent.out.includes('--this-flag-does-not-exist'))

// A named enforcing program that no longer exists is the silent drift this field exists to catch.
const scriptMissingFile = writeLedger(
  'enforce-script-missing',
  [entry({ enforcedBy: { script: 'scripts/definitely-not-a-script.mjs', probe: null } })]
)
const scriptMissing = runChecker(scriptMissingFile)
check('a named enforcing program that is gone exits 1', scriptMissing.status === 1, `exit ${scriptMissing.status}`)
check('the missing enforcing program is named', scriptMissing.out.includes('ENFORCE-SCRIPT-MISSING'))
check('the missing path is quoted, so the fix is not a guess', scriptMissing.out.includes('scripts/definitely-not-a-script.mjs'))

// A fact with no enforcing program is UNVERIFIED, never a pass - a rule followed on prose alone.
const unenforcedFile = writeLedger(
  'unenforced',
  [entry({ enforcedBy: null, gap: 'no program asserts this fixture rule.' })]
)
const unenforcedRun = runChecker(unenforcedFile)
check('a fact with no enforcing program exits 2, not 0', unenforcedRun.status === 2, `exit ${unenforcedRun.status}`)
check('the verdict is UNENFORCED', /VERDICT: UNENFORCED/.test(unenforcedRun.out), unenforcedRun.out.split('\n').slice(-2).join(' | '))
check('the unenforced fact is named', /UNENFORCED\s+fixture-fact/.test(unenforcedRun.out))
check('the gap is printed as the work order', unenforcedRun.out.includes('no program asserts this fixture rule.'))

// `null` must explain itself: a hole nobody named is the state the field exists to end.
const noGap = runChecker(writeLedger('unenforced-no-gap', [entry({ enforcedBy: null })]))
check('enforcedBy null without a gap exits 1', noGap.status === 1, `exit ${noGap.status}`)
check('the missing gap is named', noGap.out.includes('LEDGER-GAP-MISSING'))

const noProbeKey = runChecker(
  writeLedger('enforced-by-no-probe', [entry({ enforcedBy: { script: 'scripts/check-freshness.mjs' } })])
)
check('enforcedBy without a probe key exits 1', noProbeKey.status === 1, `exit ${noProbeKey.status}`)
check('the missing probe key is named', noProbeKey.out.includes('LEDGER-ENFORCED-BY-PROBE-MISSING'))

const badEnforcedBy = runChecker(writeLedger('enforced-by-string', [entry({ enforcedBy: 'scripts/x.mjs' })]))
check('enforcedBy that is not an object or null exits 1', badEnforcedBy.status === 1, `exit ${badEnforcedBy.status}`)
check('the invalid enforcedBy is named', badEnforcedBy.out.includes('LEDGER-ENFORCED-BY-INVALID'))

const unknownEnforcedKey = runChecker(
  writeLedger('enforced-by-unknown-key', [entry({ enforcedBy: { script: 'scripts/check-freshness.mjs', probe: null, probeArg: 'x' } })])
)
check('an unknown enforcedBy key exits 1', unknownEnforcedKey.status === 1, `exit ${unknownEnforcedKey.status}`)
check('the unknown key is named', unknownEnforcedKey.out.includes('LEDGER-ENFORCED-BY-UNKNOWN-KEY'))

// The case the two rules must both speak to: stale *and* unenforced must be reported twice over,
// never collapsed into one verdict that hides the other.
const staleUnenforced = runChecker(
  writeLedger('stale-unenforced', [
    entry({ verifiedAt: isoDaysAgo(200), expiresAfterDays: 180, enforcedBy: null, gap: 'no program asserts this fixture rule.' })
  ])
)
check('a stale and unenforced fact is exit 2', staleUnenforced.status === 2, `exit ${staleUnenforced.status}`)
check('it is reported stale', /STALE\s+fixture-fact/.test(staleUnenforced.out))
check('it is also reported unenforced, not silently passed', /UNENFORCED\s+fixture-fact/.test(staleUnenforced.out))

// A gap beside a real sensor is a partial-coverage note, not a failure.
const partialRun = runChecker(writeLedger('partial-gap', [entry({ gap: 'only the wrapper version is probed.' })]))
check('a partial gap is a NOTE, not a failure', partialRun.status === 0, `exit ${partialRun.status}`)
check('the partial gap is printed', /NOTE\s+fixture-fact/.test(partialRun.out) && partialRun.out.includes('only the wrapper version is probed.'))

// --warn-only relaxes staleness and unenforced-ness, but never a missing program.
check(
  '--warn-only does not excuse a missing enforcing program',
  runChecker(scriptMissingFile, ['--warn-only']).status === 1,
  `exit ${runChecker(scriptMissingFile, ['--warn-only']).status}`
)

if (PROVE) {
  console.log('\n=== T9: the sensor proof — can these checks fail? ===')
  // Each mutation removes one guarantee of the watchdog. The suite's expectation for the fixture is
  // asserted against the real script first, then against the mutant: if the mutant still produces the
  // expected observation, the expectation was not measuring that guarantee at all. `file` says which
  // of the two modules the anchor lives in, because the logic moved into `scripts/lib/freshness.mjs`.
  const mutants = [
    {
      name: 'stale-is-ignored',
      file: 'lib',
      from: '  const stale = ageDays > entry.expiresAfterDays',
      to: '  const stale = false',
      fixture: staleFile,
      expectedReal: 2,
      expectedMutant: 0,
      guards: 'T2 "one stale entry exits 2"'
    },
    {
      name: 'exit-code-pinned-to-zero',
      file: 'cli',
      from: 'process.exitCode = exitCode',
      to: 'process.exitCode = 0',
      fixture: staleFile,
      expectedReal: 2,
      expectedMutant: 0,
      guards: 'T2 "one stale entry exits 2" (the exit code it returns)'
    },
    {
      name: 'duplicate-ids-allowed',
      file: 'lib',
      from: '    } else if (seen.has(id)) {',
      to: '    } else if (false) {',
      fixture: duplicateLedger,
      expectedReal: 1,
      expectedMutant: 0,
      guards: 'T3 "a duplicate id exits 1"'
    },
    {
      name: 'blast-radius-not-checked',
      file: 'lib',
      from: '        if (!isThere(used)) {',
      to: '        if (false) {',
      fixture: ghostLedger,
      expectedReal: 1,
      expectedMutant: 0,
      guards: 'T3 "a whereUsed path that no longer exists exits 1"'
    },
    {
      name: 'missing-enforcing-script-allowed',
      file: 'lib',
      from: "  if (script !== null && script.trim() !== '' && !isThere(script)) {",
      to: '  if (false) {',
      fixture: scriptMissingFile,
      expectedReal: 1,
      expectedMutant: 0,
      guards: 'T8 "a named enforcing program that is gone exits 1"'
    },
    {
      name: 'probe-not-checked',
      file: 'lib',
      from: '    } else if (!source.includes(probe)) {',
      to: '    } else if (false) {',
      fixture: probeAbsentFile,
      expectedReal: 1,
      expectedMutant: 0,
      guards: 'T8 "a probe absent from the named program exits 1"'
    },
    {
      name: 'unenforced-counted-as-a-pass',
      file: 'lib',
      from: "    exitCode: broken ? 1 : stale.length > 0 || unenforced.length > 0 ? 2 : 0",
      to: "    exitCode: broken ? 1 : stale.length > 0 ? 2 : 0",
      fixture: unenforcedFile,
      expectedReal: 2,
      expectedMutant: 0,
      guards: 'T8 "a fact with no enforcing program exits 2, not 0"'
    }
  ]
  const checkerSource = readFileSync(CHECKER, 'utf8')
  const libSource = readFileSync(LIB_FILE, 'utf8')
  for (const mutant of mutants) {
    const dir = seedRoot(path.join(SANDBOX, `mutant-${mutant.name}`))
    if (typeof mutant.fixture !== 'string') {
      check(`mutation fixture is a ledger path: ${mutant.name}`, false, `got ${JSON.stringify(mutant.fixture)}`)
      continue
    }
    // The mutant is a real copy of the CLI *and* its library, because an ESM import resolves beside
    // the file: a copy of the CLI alone cannot see a library it does not have.
    const source = mutant.file === 'lib' ? libSource : checkerSource
    if (!source.includes(mutant.from)) {
      check(`mutation anchor is present: ${mutant.name}`, false, `anchor not found — the proof needs updating with the source`)
      continue
    }
    mkdirSync(path.join(dir, 'scripts', 'lib'), { recursive: true })
    writeFileSync(
      path.join(dir, 'scripts', 'check-freshness.mjs'),
      mutant.file === 'cli' ? checkerSource.replace(mutant.from, mutant.to) : checkerSource,
      'utf8'
    )
    writeFileSync(
      path.join(dir, 'scripts', 'lib', 'freshness.mjs'),
      mutant.file === 'lib' ? libSource.replace(mutant.from, mutant.to) : libSource,
      'utf8'
    )
    const mutated = path.join(dir, 'scripts', 'check-freshness.mjs')
    const real = runChecker(mutant.fixture, [], { script: CHECKER })
    const patched = runChecker(mutant.fixture, [], { cwd: dir, script: mutated })
    check(
      `${mutant.name}: the real script produces the expected observation (${mutant.expectedReal})`,
      real.status === mutant.expectedReal,
      `observed ${real.status}`
    )
    check(
      `${mutant.name}: the mutant produces the wrong observation (${mutant.expectedMutant}), so ${mutant.guards} can fail`,
      patched.status === mutant.expectedMutant,
      `observed ${patched.status} — the guarantee survived the mutation`
    )
  }
}

rmSync(SANDBOX, { recursive: true, force: true })

console.log(`\n${checks - failures}/${checks} checks passed`)
console.log(failures === 0 ? 'FRESHNESS WATCHDOG TESTS PASSED' : 'FRESHNESS WATCHDOG TESTS FAILED')
process.exit(failures === 0 ? 0 : 1)

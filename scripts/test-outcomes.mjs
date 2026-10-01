#!/usr/bin/env node
/**
 * Offline regression suite for the outcomes contract (`scripts/lib/outcomes.mjs`) and for the gate
 * that reads it (`scripts/check-outcomes.mjs`).
 *
 *   node scripts/test-outcomes.mjs
 *
 * Why it is built this way: the gate exists because the factory shipped a money plan with numeric
 * kill criteria that no program ever evaluated, so the sensor's whole value is its verdict on a slot
 * that *missed*. A fixture that is on-track by accident would hide a gate that never says `MISSED`,
 * and a fixture whose window has not elapsed would hide a gate that kills too early. Every case
 * below therefore carries its own defect — a reading dated in the future, a negative count, a window
 * that is still open, a window that has elapsed with no data at all — and the expectations are the
 * exit codes this repository already uses (0 pass, 1 MISSED or broken, 2 NO-DATA / UNVERIFIED).
 *
 * The CLI is driven through `--slot-dir` and `--registry` against throwaway sandboxes under the
 * system temp dir, so no case can touch `apps/` or `registry/apps.json`. The library functions are
 * driven directly for the validation cases, so a failure names the exact rule that broke.
 */
import { spawnSync } from 'node:child_process'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { SCRATCH_DIR } from './lib/scratch.mjs'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'
import { VERDICTS, evaluateOutcomes, parseOutcomes, todayIso } from './lib/outcomes.mjs'

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(SCRIPT_DIR, '..')
const CHECKER = path.join(SCRIPT_DIR, 'check-outcomes.mjs')
const SANDBOX = mkdtempSync(path.join(SCRATCH_DIR, 'kiln-outcomes-'))

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

const DAY_MS = 24 * 60 * 60 * 1000

/** A UTC calendar date N days in the past (or the future when N is negative). */
function isoDaysAgo(days) {
  return new Date(Date.now() - days * DAY_MS).toISOString().slice(0, 10)
}

/** A plan that passes `parseEconomics`, so a case can break exactly one thing. */
function economics(overrides = {}) {
  return {
    schemaVersion: 1,
    archetype: 'purchase_led_single_unlock',
    payingUser: 'A teacher who funds classroom tools from their own pocket.',
    paidUnit: 'A one-time unlock of unlimited classes and export, with no account and no ads.',
    priceUsd: 6.99,
    momentOfPayment: 'When the teacher creates a second class or exports the first report.',
    distinctnessSentence: 'Everything is editable and owned locally, and it is bought once instead of rented.',
    evidenceRef: 'docs/research/2026-09-29-app-factory-prior-art.md',
    day1RevenuePath: 'The free single-class gradebook is used for real marking, then the unlock appears.',
    killCriteria: { windowDays: 60, minInstalls: 1500, minRevenueUsd: 100 },
    assumptionsUnverified: ['That a new listing can rank in a category with a 4,000-review leader.'],
    ...overrides
  }
}

/** A reading that passes validation, so a case can break exactly one field. */
function reading(overrides = {}) {
  return {
    observedAt: isoDaysAgo(1),
    source: 'play-console',
    installCount: 10,
    activeDeviceCount: 8,
    revenueUsd: 0,
    ...overrides
  }
}

/**
 * Build a throwaway slot. `outcomes: null` leaves the file absent on purpose (the NO-DATA case), and
 * raw strings are written as-is so malformed JSON can be a case too.
 */
function writeSlot(name, { readings = [], outcomes = undefined, plan = undefined } = {}) {
  const dir = path.join(SANDBOX, `2026-09-20-fixture-${name}`)
  mkdirSync(path.join(dir, 'store'), { recursive: true })
  writeFileSync(path.join(dir, 'economics.json'), JSON.stringify(plan ?? economics(), null, 2), 'utf8')
  const outcomesValue = outcomes !== undefined ? outcomes : { schemaVersion: 1, readings }
  if (outcomesValue !== null) {
    writeFileSync(
      path.join(dir, 'store', 'outcomes.json'),
      typeof outcomesValue === 'string' ? outcomesValue : JSON.stringify(outcomesValue, null, 2),
      'utf8'
    )
  }
  return dir
}

function writeRegistry(name, apps) {
  const file = path.join(SANDBOX, `registry-${name}.json`)
  writeFileSync(file, JSON.stringify({ version: 1, apps }, null, 2), 'utf8')
  return file
}

/** Run the real gate against a sandbox slot. `--json` keeps every case's assertion shape the same. */
function runGate(slotDir, { registry = null } = {}) {
  const args = [CHECKER, '--slot-dir', slotDir, '--json']
  if (registry) args.push('--registry', registry)
  const result = spawnSync(process.execPath, args, { cwd: ROOT, encoding: 'utf8', windowsHide: true })
  const out = `${result.stdout || ''}${result.stderr || ''}`
  let payload = null
  try {
    payload = JSON.parse(out)
  } catch {
    payload = null
  }
  return { status: result.status, out, payload }
}

console.log('=== T1: a well-formed reading parses, and an empty file is a valid state ===')
{
  const ok = parseOutcomes({ schemaVersion: 1, readings: [reading()] })
  check('a valid reading parses', ok.ok === true, JSON.stringify(ok.errors))

  // The counterfactual that makes the case above meaningful: the same shape with one field wrong is
  // rejected, so a parser that accepts anything cannot pass both.
  const broken = parseOutcomes({ schemaVersion: 1, readings: [reading({ installCount: 'ten' })] })
  check('a non-integer count is rejected', broken.ok === false, JSON.stringify(broken.errors))

  const empty = parseOutcomes({ schemaVersion: 1, readings: [] })
  check('an empty readings array is valid, not broken', empty.ok === true, JSON.stringify(empty.errors))
}

console.log('\n=== T2: the validation refusals a naive parser would allow ===')
{
  const future = parseOutcomes({ schemaVersion: 1, readings: [reading({ observedAt: isoDaysAgo(-3) })] })
  check('a reading observed in the future is rejected', future.ok === false, JSON.stringify(future.errors))
  check(
    'and the refusal says a future date is how a stale record hides',
    future.errors?.some((problem) => /after today/.test(problem)) === true,
    JSON.stringify(future.errors)
  )

  const negative = parseOutcomes({ schemaVersion: 1, readings: [reading({ installCount: -4 })] })
  check('a negative install count is rejected', negative.ok === false, JSON.stringify(negative.errors))

  const negativeDevices = parseOutcomes({ schemaVersion: 1, readings: [reading({ activeDeviceCount: -1 })] })
  check('a negative active-device count is rejected', negativeDevices.ok === false, JSON.stringify(negativeDevices.errors))

  const noSource = parseOutcomes({ schemaVersion: 1, readings: [reading({ source: '   ' })] })
  check('an empty source is rejected', noSource.ok === false, JSON.stringify(noSource.errors))

  const missingSource = parseOutcomes({ schemaVersion: 1, readings: [{ ...reading(), source: undefined }] })
  check('a missing source is rejected', missingSource.ok === false, JSON.stringify(missingSource.errors))

  const badSchema = parseOutcomes({ schemaVersion: 2, readings: [] })
  check('an unknown schemaVersion is rejected', badSchema.ok === false, JSON.stringify(badSchema.errors))

  const notJson = parseOutcomes('{"schemaVersion": 1,}')
  check('unparseable JSON is rejected without throwing', notJson.ok === false, JSON.stringify(notJson.errors))

  const negativeRevenue = parseOutcomes({ schemaVersion: 1, readings: [reading({ revenueUsd: -2 })] })
  check('negative revenue is rejected', negativeRevenue.ok === false, JSON.stringify(negativeRevenue.errors))
}

console.log('\n=== T3: one observation per date ===')
{
  const sameDay = reading({ observedAt: isoDaysAgo(2), source: 'play-console' })
  const duplicate = parseOutcomes({
    schemaVersion: 1,
    readings: [sameDay, { ...sameDay, source: 'payout-statement' }]
  })
  check('a duplicate observedAt is rejected', duplicate.ok === false, JSON.stringify(duplicate.errors))
  check(
    'and the refusal names both rows, so the fix is not a search',
    duplicate.errors?.some((problem) => /duplicates readings\[0\]/.test(problem)) === true,
    JSON.stringify(duplicate.errors)
  )

  const distinct = parseOutcomes({ schemaVersion: 1, readings: [sameDay, { ...sameDay, observedAt: isoDaysAgo(3) }] })
  check('two different dates are accepted', distinct.ok === true, JSON.stringify(distinct.errors))
}

console.log('\n=== T4: the window boundary decides TOO-EARLY from MISSED ===')
{
  const criteria = { windowDays: 60, minInstalls: 1500, minRevenueUsd: 100 }
  const low = { installCount: 20, activeDeviceCount: 15, revenueUsd: 0 }
  const soon = evaluateOutcomes({
    readings: [reading({ observedAt: isoDaysAgo(10), ...low }), reading({ observedAt: isoDaysAgo(1), ...low })],
    killCriteria: criteria,
    slug: 'fixture-soon'
  })
  check('a window that has not elapsed is TOO-EARLY', soon.verdict === VERDICTS.TOO_EARLY, soon.verdict)
  check('TOO-EARLY is not MISSED', soon.verdict !== VERDICTS.MISSED, soon.verdict)
  check('TOO-EARLY exits 0', soon.exitCode === 0, String(soon.exitCode))
  check('TOO-EARLY carries no kill instruction', soon.killInstruction === undefined, String(soon.killInstruction))

  const late = evaluateOutcomes({
    readings: [reading({ observedAt: isoDaysAgo(70), ...low }), reading({ observedAt: isoDaysAgo(2), ...low })],
    killCriteria: criteria,
    slug: 'fixture-missed'
  })
  check('the same readings with an elapsed window are MISSED', late.verdict === VERDICTS.MISSED, late.verdict)
  check('MISSED exits 1', late.exitCode === 1, String(late.exitCode))
  check(
    'MISSED names the exact kill instruction an operator runs',
    typeof late.killInstruction === 'string' && late.killInstruction.includes('--kill') && late.killInstruction.includes('fixture'),
    String(late.killInstruction)
  )
}

console.log('\n=== T5: an empty record is NO-DATA, never MISSED ===')
{
  // The trap: with no reading there is no newest reading, and a naive implementation would read an
  // undefined count as "below threshold" and kill a slot on the day it shipped.
  const empty = evaluateOutcomes({
    readings: [],
    killCriteria: { windowDays: 60, minInstalls: 1500, minRevenueUsd: 100 },
    createdAt: isoDaysAgo(400)
  })
  check('no readings at all is NO-DATA', empty.verdict === VERDICTS.NO_DATA, empty.verdict)
  check('NO-DATA is not MISSED', empty.verdict !== VERDICTS.MISSED, empty.verdict)
  check('NO-DATA exits 2, never 0', empty.exitCode === 2, String(empty.exitCode))

  const absent = evaluateOutcomes({ readings: null, killCriteria: { windowDays: 60, minInstalls: 1500, minRevenueUsd: 100 } })
  check('a null reading list is NO-DATA too', absent.verdict === VERDICTS.NO_DATA, absent.verdict)
}

console.log('\n=== T6: hitting the numbers early is ON-TRACK, not TOO-EARLY ===')
{
  const criteria = { windowDays: 60, minInstalls: 1500, minRevenueUsd: 100 }
  const hit = evaluateOutcomes({
    readings: [reading({ observedAt: isoDaysAgo(5), installCount: 1600, activeDeviceCount: 1400, revenueUsd: 120 })],
    killCriteria: criteria,
    createdAt: isoDaysAgo(6)
  })
  check('thresholds met inside the window are ON-TRACK', hit.verdict === VERDICTS.ON_TRACK, hit.verdict)
  check('ON-TRACK exits 0', hit.exitCode === 0, String(hit.exitCode))

  const short = evaluateOutcomes({
    readings: [reading({ observedAt: isoDaysAgo(5), installCount: 1600, activeDeviceCount: 1400, revenueUsd: 99 })],
    killCriteria: criteria,
    createdAt: isoDaysAgo(6)
  })
  check('one dollar under the floor is not ON-TRACK', short.verdict !== VERDICTS.ON_TRACK, short.verdict)
}

console.log('\n=== T7: a single reading dates its window from the registry createdAt ===')
{
  const criteria = { windowDays: 60, minInstalls: 1500, minRevenueUsd: 100 }
  const stale = evaluateOutcomes({
    readings: [reading({ observedAt: isoDaysAgo(3), installCount: 5, activeDeviceCount: 4, revenueUsd: 0 })],
    killCriteria: criteria,
    createdAt: isoDaysAgo(90)
  })
  check('a single old-slot reading past the window is MISSED', stale.verdict === VERDICTS.MISSED, stale.verdict)

  const fresh = evaluateOutcomes({
    readings: [reading({ observedAt: isoDaysAgo(3), installCount: 5, activeDeviceCount: 4, revenueUsd: 0 })],
    killCriteria: criteria,
    createdAt: isoDaysAgo(4)
  })
  check('the same reading on a young slot is TOO-EARLY', fresh.verdict === VERDICTS.TOO_EARLY, fresh.verdict)

  const noRegistry = evaluateOutcomes({
    readings: [reading({ observedAt: isoDaysAgo(3), installCount: 5, activeDeviceCount: 4, revenueUsd: 0 })],
    killCriteria: criteria,
    createdAt: null
  })
  check('no registry entry reports TOO-EARLY rather than inventing an epoch', noRegistry.verdict === VERDICTS.TOO_EARLY, noRegistry.verdict)
}

console.log('\n=== T8: the gate — NO-DATA names the file that is missing ===')
{
  const slot = writeSlot('nodata', { outcomes: null })
  const run = runGate(slot)
  check('a missing outcomes file exits 2', run.status === 2, `exit ${run.status}`)
  check('the verdict is NO-DATA', run.payload?.verdict === 'NO-DATA', JSON.stringify(run.payload))
  check('NO-DATA is not MISSED', run.payload?.verdict !== 'MISSED', JSON.stringify(run.payload))
  check(
    'and it names the missing file, so the operator knows what to create',
    /store\/outcomes\.json is missing/.test(run.out),
    run.out.slice(0, 240)
  )

  const emptySlot = writeSlot('nodata-empty', { readings: [] })
  const emptyRun = runGate(emptySlot)
  check('an outcomes file with no readings exits 2 as NO-DATA', emptyRun.status === 2 && emptyRun.payload?.verdict === 'NO-DATA', JSON.stringify(emptyRun.payload))
}

console.log('\n=== T9: the gate — an elapsed window with no data still cannot be MISSED ===')
{
  // The registry says the slot is 200 days old and its window is 60, so a gate that judged the
  // window instead of the readings would return MISSED here. It must not.
  const slot = writeSlot('aged-nodata', { readings: [] })
  const registry = writeRegistry('aged', [
    { id: 'fixture-aged-nodata', dir: 'apps/2026-09-20-fixture-aged-nodata', createdAt: `${isoDaysAgo(200)}T00:00:00.000Z` }
  ])
  const run = runGate(slot, { registry })
  check('a long-elapsed window with no reading is NO-DATA, not MISSED', run.payload?.verdict === 'NO-DATA', JSON.stringify(run.payload))
  check('and it exits 2 rather than 1', run.status === 2, `exit ${run.status}`)
}

console.log('\n=== T10: the gate — TOO-EARLY is exit 0 and prints no kill instruction ===')
{
  const slot = writeSlot('early', {
    readings: [
      reading({ observedAt: isoDaysAgo(9), installCount: 30, activeDeviceCount: 20, revenueUsd: 0 }),
      reading({ observedAt: isoDaysAgo(1), installCount: 40, activeDeviceCount: 28, revenueUsd: 0 })
    ]
  })
  const run = runGate(slot)
  check('an open window exits 0', run.status === 0, `exit ${run.status}`)
  check('the verdict is TOO-EARLY', run.payload?.verdict === 'TOO-EARLY', JSON.stringify(run.payload))
  check('no kill instruction is offered while the window is open', run.payload?.killInstruction === null, String(run.payload?.killInstruction))
}

console.log('\n=== T11: the gate — ON-TRACK is exit 0, and the sensor can also say MISSED ===')
{
  const strong = writeSlot('ontrack', {
    readings: [
      reading({ observedAt: isoDaysAgo(9), installCount: 900, activeDeviceCount: 700, revenueUsd: 60 }),
      reading({ observedAt: isoDaysAgo(1), installCount: 1700, activeDeviceCount: 1500, revenueUsd: 140 })
    ]
  })
  const onTrack = runGate(strong)
  check('thresholds met exits 0', onTrack.status === 0, `exit ${onTrack.status}`)
  check('the verdict is ON-TRACK', onTrack.payload?.verdict === 'ON-TRACK', JSON.stringify(onTrack.payload))

  // The counterfactual: the same fixture with the newest reading under the revenue floor must not be
  // ON-TRACK, or the check above is not measuring the thresholds at all.
  const weak = writeSlot('ontrack-under', {
    readings: [
      reading({ observedAt: isoDaysAgo(9), installCount: 900, activeDeviceCount: 700, revenueUsd: 60 }),
      reading({ observedAt: isoDaysAgo(1), installCount: 1700, activeDeviceCount: 1500, revenueUsd: 40 })
    ]
  })
  const under = runGate(weak)
  check('one threshold missed is not ON-TRACK', under.payload?.verdict !== 'ON-TRACK', JSON.stringify(under.payload))
  check('and it is TOO-EARLY while the window is open', under.payload?.verdict === 'TOO-EARLY', JSON.stringify(under.payload))
}

console.log('\n=== T12: the gate — MISSED exits 1 and prints the kill instruction ===')
{
  const slot = writeSlot('missed', {
    readings: [
      reading({ observedAt: isoDaysAgo(70), installCount: 200, activeDeviceCount: 150, revenueUsd: 4 }),
      reading({ observedAt: isoDaysAgo(2), installCount: 420, activeDeviceCount: 300, revenueUsd: 12 })
    ]
  })
  const run = runGate(slot)
  check('an elapsed window with missed thresholds exits 1', run.status === 1, `exit ${run.status}`)
  check('the verdict is MISSED', run.payload?.verdict === 'MISSED', JSON.stringify(run.payload))
  check('the miss names the install shortfall', /installCount 420 of 1500 required/.test(run.out), run.out.slice(0, 300))
  check('the miss names the revenue shortfall', /revenueUsd 12 of 100 required/.test(run.out), run.out.slice(0, 300))
  check(
    'and it prints the exact --kill instruction an operator would run',
    typeof run.payload?.killInstruction === 'string' && run.payload.killInstruction.includes('--kill') && run.payload.killInstruction.includes('fixture-missed'),
    String(run.payload?.killInstruction)
  )
}

console.log('\n=== T13: the gate — a broken reading is BLOCKED (exit 1), not NO-DATA ===')
{
  const future = writeSlot('future', { readings: [reading({ observedAt: isoDaysAgo(-5) })] })
  const futureRun = runGate(future)
  check('a future reading exits 1', futureRun.status === 1, `exit ${futureRun.status}`)
  check('and it is BLOCKED, not NO-DATA', futureRun.payload?.verdict === 'BLOCKED', JSON.stringify(futureRun.payload))

  const negative = writeSlot('negative', { readings: [reading({ installCount: -1 })] })
  const negativeRun = runGate(negative)
  check('a negative count exits 1', negativeRun.status === 1, `exit ${negativeRun.status}`)
  check('and the field is named', negativeRun.out.includes('installCount'), negativeRun.out.slice(0, 240))

  const malformed = writeSlot('malformed', { outcomes: '{ "schemaVersion": 1, ' })
  const malformedRun = runGate(malformed)
  check('unparseable outcomes exits 1', malformedRun.status === 1, `exit ${malformedRun.status}`)
  check('and it is BLOCKED', malformedRun.payload?.verdict === 'BLOCKED', JSON.stringify(malformedRun.payload))
}

console.log('\n=== T14: a slot with no plan is UNVERIFIED (exit 2), never a verdict ===')
{
  const dir = path.join(SANDBOX, '2026-09-20-fixture-noplan')
  mkdirSync(path.join(dir, 'store'), { recursive: true })
  writeFileSync(path.join(dir, 'store', 'outcomes.json'), JSON.stringify({ schemaVersion: 1, readings: [reading()] }), 'utf8')
  const run = runGate(dir)
  check('a missing economics plan exits 2', run.status === 2, `exit ${run.status}`)
  check('and it is BLOCKED, not PASS', run.payload?.verdict === 'BLOCKED', JSON.stringify(run.payload))
  check('and it names the missing plan', /economics plan is missing/.test(run.out), run.out.slice(0, 240))
}

console.log('\n=== T15: the human output is a single verdict block, and --json is machine-readable ===')
{
  const slot = writeSlot('missed-human', {
    readings: [reading({ observedAt: isoDaysAgo(70), installCount: 10, activeDeviceCount: 8, revenueUsd: 0 })]
  })
  const registry = writeRegistry('human', [
    { id: 'fixture-missed-human', dir: 'apps/2026-09-20-fixture-missed-human', createdAt: `${isoDaysAgo(200)}T00:00:00.000Z` }
  ])
  const human = spawnSync(process.execPath, [CHECKER, '--slot-dir', slot, '--registry', registry], {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  })
  const humanOut = `${human.stdout || ''}${human.stderr || ''}`
  check('the human output prints exactly one VERDICT line', (humanOut.match(/VERDICT: /g) || []).length === 1, humanOut.slice(0, 300))
  check('and the verdict is MISSED', /VERDICT: MISSED/.test(humanOut), humanOut.slice(-200))
  check('and the kill instruction is in the block', /KILL\s+node scripts\/kiln-status\.mjs --kill/.test(humanOut), humanOut.slice(-400))

  const json = runGate(slot, { registry })
  check('--json emits parseable JSON', json.payload !== null, json.out.slice(0, 200))
  check('--json carries the verdict and its exit code', json.payload?.verdict === 'MISSED' && json.payload?.exitCode === 1, JSON.stringify(json.payload))
  check('--json carries the newest reading and the window', json.payload?.newest?.observedAt === isoDaysAgo(70) && json.payload?.windowDays === 60, JSON.stringify(json.payload?.newest))
  check('--json carries the elapsed days and the epoch', json.payload?.elapsedDays === 200 && json.payload?.epoch === isoDaysAgo(200), `${json.payload?.elapsedDays} / ${json.payload?.epoch}`)
}

console.log('\n=== T16: the window boundary is a boundary, not a guess ===')
{
  // "TOO-EARLY while the newest reading is younger than windowDays" means exactly that: N-1 days
  // elapsed is still inside the window, N days elapsed is not. The pair is asserted together,
  // because a gate that always says TOO-EARLY would pass the first half alone.
  const criteria = { windowDays: 60, minInstalls: 1500, minRevenueUsd: 100 }
  const low = { installCount: 5, activeDeviceCount: 4, revenueUsd: 0 }
  const dayBefore = evaluateOutcomes({
    readings: [reading({ observedAt: isoDaysAgo(59), ...low }), reading({ observedAt: isoDaysAgo(1), ...low })],
    killCriteria: criteria
  })
  const onDayN = evaluateOutcomes({
    readings: [reading({ observedAt: isoDaysAgo(60), ...low }), reading({ observedAt: isoDaysAgo(1), ...low })],
    killCriteria: criteria
  })
  check('one day before the window closes is still TOO-EARLY', dayBefore.verdict === VERDICTS.TOO_EARLY, dayBefore.verdict)
  check('the window is elapsed on day N itself', onDayN.verdict === VERDICTS.MISSED, onDayN.verdict)
  check('and the gate reports today as a UTC date', /^\d{4}-\d{2}-\d{2}$/.test(todayIso()), todayIso())
}

rmSync(SANDBOX, { recursive: true, force: true })

console.log(`\n${checks - failures}/${checks} checks passed`)
console.log(failures === 0 ? 'OUTCOMES GATE TESTS PASSED' : 'OUTCOMES GATE TESTS FAILED')
process.exit(failures === 0 ? 0 : 1)

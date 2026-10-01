#!/usr/bin/env node
/**
 * Hard gate: a shipped slot's measured outcome against the numeric kill criteria it declared.
 *
 * Why this exists: `apps/<slot>/economics.json` has required `killCriteria` since the money gate was
 * written, and nothing ever evaluated them. The schema, the documents and one test fixture were the
 * only readers of `minInstalls` / `minRevenueUsd`, so a slot could miss every number it promised and
 * still pass every gate — because all of them measure form. The mission puts the answer elsewhere
 * (`AGENTS.md`: "The factory's product is revenue, not green gates"), and
 * `docs/research/2026-09-29-app-factory-prior-art.md` §2.6 records the same rule from the outside:
 * a portfolio only works when the attempts that miss are killed on a clock. This script is that
 * clock. It reads the plan and the readings, and prints exactly one verdict per slot.
 *
 * Usage:
 *   node scripts/check-outcomes.mjs --slot <slug> [--json]
 *   node scripts/check-outcomes.mjs --slot-dir <path> [--registry <path>] [--json]
 *
 * `--slot-dir` and `--registry` exist so `scripts/test-outcomes.mjs` can drive this real gate against
 * throwaway slots, the same way the fact-ledger check takes a `ledgerPath` so its own fixtures never
 * touch the repository's ledger. A slot run uses `--slot` alone.
 *
 * Exit codes, matching this repository's convention (0 pass, 1 a verdict the caller must act on,
 * 2 UNVERIFIED / no evidence — never a pass):
 *   0  ON-TRACK or TOO-EARLY — the thresholds are met, or the window is still open
 *   1  MISSED — the window has elapsed and a threshold is missed; or a file is broken
 *   2  NO-DATA — nothing has been measured, so nothing can be judged
 */

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { basename, dirname, join, relative, resolve, sep } from 'node:path'
import process from 'node:process'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { parseEconomics } from './lib/slot-economics.mjs'
import {
  VERDICTS,
  evaluateOutcomes,
  findSlotCreatedAt,
  parseOutcomes,
  todayIso
} from './lib/outcomes.mjs'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const usage =
  'node scripts/check-outcomes.mjs --slot <slug> [--json] | --slot-dir <path> [--registry <path>] [--json]'

let asJson = false

function line(status, message) {
  console.log(`${status.padEnd(8)} ${message}`)
}

function rel(target) {
  const display = relative(ROOT, target).split(sep).join('/')
  return display.startsWith('..') ? target.split(sep).join('/') : display
}

function parseArgs(argv) {
  const values = new Set(['--slot', '--slot-dir', '--registry'])
  const flags = new Set(['--json'])
  const parsed = {}

  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index]
    if (flags.has(option)) {
      parsed.json = true
      continue
    }
    if (!values.has(option)) throw new Error(`Unknown option: ${option}\nUsage: ${usage}`)
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) throw new Error(`Missing value for ${option}\nUsage: ${usage}`)
    parsed[option.slice(2)] = value
    index += 1
  }
  return parsed
}

function isFile(candidate) {
  try {
    return statSync(candidate).isFile()
  } catch {
    return false
  }
}

function listAppDirs() {
  const appsDir = join(ROOT, 'apps')
  if (!existsSync(appsDir)) return []
  return readdirSync(appsDir)
    .map((name) => join(appsDir, name))
    .filter((candidate) => {
      try {
        return statSync(candidate).isDirectory()
      } catch {
        return false
      }
    })
}

/**
 * Resolve a slot the way `scripts/verify.mjs` does: an exact directory name or a `<date>-<slug>`
 * suffix, and only when exactly one candidate matches, so an ambiguous slug fails instead of
 * silently picking one.
 */
function resolveSlotDir(slug) {
  const matches = listAppDirs().filter((candidate) => {
    const name = basename(candidate)
    return name === slug || name.endsWith(`-${slug}`)
  })
  return matches.length === 1 ? matches[0] : null
}

function readJsonFile(file, label) {
  let raw
  try {
    raw = readFileSync(file, 'utf8')
  } catch (error) {
    throw new Error(`${label} could not be read: ${rel(file)} (${error.message})`)
  }
  try {
    return JSON.parse(raw.replace(/^\uFEFF/, ''))
  } catch (error) {
    throw new Error(`${label} is not valid JSON: ${rel(file)} (${error.message})`)
  }
}

/** Print one verdict and set the process exit code; the only place either is decided. */
function emit(verdict, exitCode, humanLines, payload) {
  // `messages` carries the human block into `--json` as well. Without it a machine consumer sees a
  // verdict with no reason, and the missing-file path an operator needs exists only in the human
  // branch — two outputs of one decision that do not agree.
  const record = {
    ...payload,
    messages: humanLines.map(([status, message]) => ({ status, message })),
    verdict,
    exitCode,
    checkedAt: new Date().toISOString()
  }
  if (asJson) {
    console.log(JSON.stringify(record, null, 2))
  } else {
    console.log(`=== outcomes — ${payload.slotLabel} ===`)
    for (const [status, message] of humanLines) line(status, message)
    console.log(`\nVERDICT: ${verdict}`)
  }
  process.exitCode = exitCode
}

function main() {
  const options = parseArgs(process.argv.slice(2))
  asJson = options.json === true

  if (!options.slot && !options['slot-dir']) {
    throw new Error(`Missing --slot <slug> or --slot-dir <path>\nUsage: ${usage}`)
  }
  if (options.slot && options['slot-dir']) {
    throw new Error(`--slot and --slot-dir are mutually exclusive\nUsage: ${usage}`)
  }

  let slotDir = null
  let slug = null
  if (options.slot) {
    slotDir = resolveSlotDir(options.slot)
    if (!slotDir) {
      return emit(
        'BLOCKED',
        2,
        [['BLOCKED', `slot not found or ambiguous under apps/: ${options.slot}`]],
        { slot: options.slot, slotLabel: options.slot, problems: [`slot not found or ambiguous: ${options.slot}`] }
      )
    }
    slug = basename(slotDir).replace(/^\d{4}-\d{2}-\d{2}-/, '')
  } else {
    slotDir = resolve(ROOT, options['slot-dir'])
    if (!existsSync(slotDir)) {
      return emit('BLOCKED', 2, [['BLOCKED', `slot directory does not exist: ${rel(slotDir)}`]], {
        slot: null,
        slotLabel: rel(slotDir),
        problems: [`slot directory does not exist: ${rel(slotDir)}`]
      })
    }
    slug = basename(slotDir).replace(/^\d{4}-\d{2}-\d{2}-/, '')
  }

  const economicsFile = join(slotDir, 'economics.json')
  const outcomesFile = join(slotDir, 'store', 'outcomes.json')
  const registryFile = options.registry ? resolve(ROOT, options.registry) : join(ROOT, 'registry', 'apps.json')
  const slotLabel = rel(slotDir)

  // The plan is the yardstick. Without it there is no threshold to judge against, which is
  // UNVERIFIED (2) rather than a verdict: the gate has no evidence, and no evidence is never a pass.
  if (!isFile(economicsFile)) {
    return emit('BLOCKED', 2, [['BLOCKED', `economics plan is missing, so there are no kill criteria to judge: ${rel(economicsFile)}`]], {
      slot: slug,
      slotLabel,
      planFile: rel(economicsFile),
      problems: [`economics plan is missing: ${rel(economicsFile)}`]
    })
  }

  const plan = parseEconomics(readJsonFile(economicsFile, 'economics plan'))
  if (!plan.ok) {
    return emit('BLOCKED', 1, plan.errors.map((problem) => ['BLOCKED', problem]), {
      slot: slug,
      slotLabel,
      planFile: rel(economicsFile),
      problems: plan.errors
    })
  }
  const { killCriteria } = plan.value

  // No readings file at all is a legitimate state, not a broken one: the slot simply has not been
  // measured. It is reported NO-DATA (2) and never MISSED, so a portfolio rule cannot kill a slot on
  // the day it shipped.
  if (!isFile(outcomesFile)) {
    return emit(
      VERDICTS.NO_DATA,
      2,
      [
        ['INFO', `plan: ${rel(economicsFile)} — kill window ${killCriteria.windowDays} day(s), installs >= ${killCriteria.minInstalls}, revenue >= $${killCriteria.minRevenueUsd}`],
        ['NO-DATA', `no outcomes have been recorded: ${rel(outcomesFile)} is missing`],
        ['INFO', 'record the first Play Console reading into that file with an observedAt and a source']
      ],
      {
        slot: slug,
        slotLabel,
        planFile: rel(economicsFile),
        readingsFile: rel(outcomesFile),
        windowDays: killCriteria.windowDays,
        readingCount: 0,
        reason: 'no outcomes file has been recorded'
      }
    )
  }

  const parsed = parseOutcomes(readJsonFile(outcomesFile, 'outcomes'))
  if (!parsed.ok) {
    // Broken data is not absent data: the file exists and contradicts its own contract, so the
    // caller must fix it rather than read "no data". Exit 1, like every other broken artifact here.
    return emit('BLOCKED', 1, parsed.errors.map((problem) => ['BLOCKED', problem]), {
      slot: slug,
      slotLabel,
      planFile: rel(economicsFile),
      readingsFile: rel(outcomesFile),
      problems: parsed.errors
    })
  }

  const readings = parsed.value.readings
  if (readings.length === 0) {
    return emit(
      VERDICTS.NO_DATA,
      2,
      [
        ['INFO', `plan: ${rel(economicsFile)} — kill window ${killCriteria.windowDays} day(s), installs >= ${killCriteria.minInstalls}, revenue >= $${killCriteria.minRevenueUsd}`],
        ['NO-DATA', `${rel(outcomesFile)} holds no readings yet (a valid state: a slot with no data is NO-DATA, not a broken file)`]
      ],
      {
        slot: slug,
        slotLabel,
        planFile: rel(economicsFile),
        readingsFile: rel(outcomesFile),
        windowDays: killCriteria.windowDays,
        readingCount: 0,
        reason: 'the outcomes file holds no readings'
      }
    )
  }

  const createdAt = findSlotCreatedAt(readJsonFile(registryFile, 'registry'), {
    slug,
    slotDirRelative: rel(slotDir)
  })
  const result = evaluateOutcomes({ readings, killCriteria, slug, createdAt, today: todayIso() })
  const newest = result.newest

  const header = [
    ['INFO', `plan: ${rel(economicsFile)} — kill window ${killCriteria.windowDays} day(s), installs >= ${killCriteria.minInstalls}, revenue >= $${killCriteria.minRevenueUsd}`],
    ['INFO', `readings: ${rel(outcomesFile)} — ${readings.length} reading(s), newest ${newest.observedAt} from ${newest.source}`]
  ]

  let humanLines
  if (result.verdict === VERDICTS.MISSED) {
    humanLines = [
      ...header,
      ...result.misses.map((miss) => ['MISSED', miss]),
      ['MISSED', `window elapsed: ${result.elapsedDays} of ${result.windowDays} day(s) since ${result.epochSource} ${result.epoch}`],
      ['KILL', result.killInstruction]
    ]
  } else if (result.verdict === VERDICTS.TOO_EARLY) {
    humanLines = [
      ...header,
      ['INFO', `too early to judge: ${result.elapsedDays} of ${result.windowDays} day(s) since ${result.epochSource} ${result.epoch}`],
      ['INFO', `still short of the thresholds: ${result.misses.join('; ')}`]
    ]
  } else if (result.verdict === VERDICTS.ON_TRACK) {
    humanLines = [...header, ['PASS', result.reason]]
  } else {
    humanLines = [...header, ['BLOCKED', result.reason]]
  }

  return emit(result.verdict, result.exitCode, humanLines, {
    slot: slug,
    slotLabel,
    planFile: rel(economicsFile),
    readingsFile: rel(outcomesFile),
    windowDays: killCriteria.windowDays,
    minInstalls: killCriteria.minInstalls,
    minRevenueUsd: killCriteria.minRevenueUsd,
    readingCount: readings.length,
    elapsedDays: result.elapsedDays ?? null,
    epoch: result.epoch ?? null,
    epochSource: result.epochSource ?? null,
    newest,
    misses: result.misses,
    reason: result.reason,
    killInstruction: result.killInstruction ?? null
  })
}

function runCli() {
  try {
    main()
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    if (asJson) {
      console.log(JSON.stringify({ verdict: 'BLOCKED', exitCode: 1, problems: [message] }, null, 2))
    } else {
      line('BLOCKED', message)
      console.log('\nVERDICT: BLOCKED')
    }
    process.exitCode = 1
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) runCli()

#!/usr/bin/env node
/**
 * Hard gate: every promise this app was chosen for is either delivered and provable, or
 * dropped in writing — with the owner's own words when the promise was load-bearing.
 *
 * Why this exists: recon produces the reason list an app can win on, and implementation
 * silently loses part of it. The slot then ships ten of twenty promises and every
 * existing gate returns PASS, because they all measure form — the APK builds, the pack
 * is complete, the money plan parses — and none of them knew what the app had promised
 * to be. A promised feature that quietly did not ship is the cheapest possible way to
 * ship a worse app than the one the evidence selected.
 *
 * Usage:
 *   node scripts/check-differentiators.mjs --slot <slug> [--allow-deferred]
 *   node scripts/check-differentiators.mjs --differentiators <path> [--allow-deferred]
 *
 * The slot mode is the gate: it resolves the file, checks the promises against the
 * slot's own artifacts and fails while anything is still deferred, which is how the
 * slot's own `deferred` rows stop being a progress report and start being a blocker.
 *
 * The `--differentiators <path>` mode validates a plan that has no slot yet. Every
 * promise in a fresh plan is legitimately deferred — nothing has been built — so the
 * `deferred` rule and the artifact checks are skipped with printed SKIP lines. The
 * shape checks are not skipped.
 */

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { basename, dirname, join, relative, resolve, sep } from 'node:path'
import process from 'node:process'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { crudReport, depthFindings, parseDifferentiators, summarizeDifferentiators, validateDifferentiatorRefs } from './lib/slot-differentiators.mjs'
import { baselineApplies } from './core/baseline.mjs'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const usage =
  'node scripts/check-differentiators.mjs --slot <slug> [--allow-deferred] | --differentiators <path> [--allow-deferred]'

function line(status, message) {
  console.log(`${status.padEnd(8)} ${message}`)
}

function parseArgs(argv) {
  const values = new Set(['--slot', '--differentiators'])
  const flags = new Set(['--allow-deferred'])
  const parsed = { allowDeferred: false }

  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index]
    if (flags.has(option)) {
      parsed.allowDeferred = true
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
 * Resolve a slot the way `scripts/verify.mjs` does: an exact directory name or a
 * `<date>-<slug>` suffix, and only when exactly one candidate matches, so an ambiguous
 * slug fails instead of silently picking one.
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
    throw new Error(`${label} could not be read: ${file} (${error.message})`)
  }
  try {
    return JSON.parse(raw.replace(/^\uFEFF/, ''))
  } catch (error) {
    throw new Error(`${label} is not valid JSON: ${file} (${error.message})`)
  }
}

function finish(blocked) {
  console.log(`\nVERDICT: ${blocked ? 'BLOCKED' : 'PASS'}`)
  process.exitCode = blocked ? 1 : 0
}

/**
 * Print what the owner needs to read before calling the app done: which promises were
 * weakened or lost, and whose decision that was. A drop that only exists inside a JSON
 * file the owner never opens is a silent scope cut, so the gate states it out loud on
 * every run.
 */
function reportWeakened(value) {
  const claims = Array.isArray(value?.claims) ? value.claims : []
  const weakened = claims.filter((claim) => claim?.status === 'narrowed' || claim?.status === 'dropped')
  if (weakened.length === 0) return
  console.log('')
  for (const claim of weakened) {
    const detail =
      claim.status === 'narrowed'
        ? `narrowed to: ${claim.narrowedTo} | no longer covered: ${claim.lostCoverage}`
        : `dropped: ${claim.droppedReason}`
    line('INFO', `${claim.status.toUpperCase()} [${claim.weight}] ${claim.id} — ${detail}`)
    if (claim.ownerDecision?.words) {
      line('INFO', `  owner's words: ${claim.ownerDecision.words} (${claim.ownerDecision.recordedAt})`)
    }
  }
}

function main() {
  const options = parseArgs(process.argv.slice(2))
  if (!options.slot && !options.differentiators) {
    throw new Error(`Missing --slot <slug> or --differentiators <path>\nUsage: ${usage}`)
  }

  let slotDir = null
  let slug = null
  if (options.slot) {
    slotDir = resolveSlotDir(options.slot)
    if (!slotDir) throw new Error(`slot not found or ambiguous under apps/: ${options.slot}`)
    slug = basename(slotDir).replace(/^\d{4}-\d{2}-\d{2}-/, '')
    line('INFO', `slot: ${relative(ROOT, slotDir).split(sep).join('/')}`)
  }

  const file = options.differentiators
    ? resolve(ROOT, options.differentiators)
    : join(slotDir, 'differentiators.json')
  line('INFO', `contract: ${relative(ROOT, file).split(sep).join('/')}`)
  if (!existsSync(file)) {
    line('BLOCKED', `differentiators contract is missing: ${relative(ROOT, file).split(sep).join('/')}`)
    return finish(true)
  }

  const parsed = readJsonFile(file, 'differentiators contract')

  // A schema failure stops here: resolving the artifacts of promises whose fields are
  // wrong would report derived noise on top of the real problem.
  const result = parseDifferentiators(parsed)
  if (!result.ok) {
    for (const problem of result.errors) line('BLOCKED', problem)
    return finish(true)
  }

  const summary = summarizeDifferentiators(result.value)
  line(
    'PASS',
    `contract schema — ${summary.total} promise(s), ${summary.core} core, ${summary.shipped} shipped, ${summary.narrowed} narrowed, ${summary.dropped} dropped, ${summary.deferred} deferred`
  )

  // The CRUD report is a note and never a gate. The predicate measures vocabulary, and the
  // repository's own `edit-every-record` row escapes it because it carries a contrast clause that
  // may be a sourced difference (docs/research/2026-10-02-depth-encoding-and-detectability.md §1.3),
  // so a FAIL here would fire on correct prose. It prints so the owner can read it at checkpoint 1.
  const crud = crudReport(result.value, { file: relative(ROOT, file).split(sep).join('/'), raw: readFileSync(file, 'utf8') })
  if (crud.length > 0) {
    line('INFO', `CRUD report (human-read, never gating): ${crud.length} promise(s) read as basic record operations`)
    for (const entry of crud) line('INFO', `  ${entry}`)
  }

  let blocked = false

  if (!slotDir) {
    // No slot yet, so there is nothing to resolve the recon or the proofs against. The
    // shape checks above still ran, which is what `new-slot.mjs` needs.
    line('SKIP', `artifacts are enforced later by \`node scripts/check-differentiators.mjs --slot <slug>\``)
    if (summary.deferred > 0) {
      line('INFO', `${summary.deferred} promise(s) start deferred; the slot gate fails until each one is resolved`)
    }
    reportWeakened(result.value)
    return finish(blocked)
  }

  const refs = validateDifferentiatorRefs(result.value, {
    slotDir,
    repoRoot: ROOT,
    slug,
    allowDeferred: options.allowDeferred,
    isFile,
    readFile: (candidate) => readFileSync(candidate, 'utf8')
  })
  if (refs.ok) {
    line('PASS', 'every promise is delivered and proved, or dropped in writing')
  } else {
    for (const problem of refs.errors) line('BLOCKED', problem)
    blocked = true
  }

  // The depth half: does the contract cover the user's before/during/after, and does at least one
  // shipped handoff carry its family's token in the artifact that proves it
  // (`.cursor/rules/app-depth.mdc`). Baseline-scoped exactly as the core is, so a slot that predates
  // the rule prints the finding as a note and a slot created from it is blocked. `baselineApplies`
  // is the derivation itself, imported rather than restated.
  const depth = depthFindings(result.value, {
    slotDir,
    isFile,
    readFile: (candidate) => readFileSync(candidate, 'utf8')
  })
  if (depth.length > 0) {
    const state = baselineApplies(slotDir)
    if (state.applies) {
      for (const finding of depth) line('BLOCKED', finding)
      blocked = true
    } else {
      line('NOTE', `the depth contract carries ${depth.length} finding(s) that do not gate here: ${state.reason}`)
      for (const finding of depth) line('NOTE', `  ${finding}`)
    }
  } else {
    line('PASS', 'job coverage covers before, during and after, and a shipped handoff carries its family token')
  }

  reportWeakened(result.value)

  if (summary.deferred > 0) {
    if (options.allowDeferred) {
      line('INFO', `${summary.deferred} promise(s) still deferred — allowed only while the work is in flight`)
    } else {
      line(
        'BLOCKED',
        `${summary.deferred} promise(s) still deferred: the app is not done until every promise is shipped, narrowed or dropped`
      )
      blocked = true
    }
  }

  return finish(blocked)
}

function runCli() {
  try {
    main()
  } catch (error) {
    line('BLOCKED', error instanceof Error ? error.message : String(error))
    console.log('\nVERDICT: BLOCKED')
    process.exitCode = 1
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) runCli()

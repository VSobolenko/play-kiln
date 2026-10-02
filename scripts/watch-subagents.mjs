#!/usr/bin/env node
/**
 * watch-subagents — report whether the subagents this chat dispatched are still working.
 *
 * The defect this exists for
 * --------------------------
 * A `Task` dispatch blocks the orchestrator until the agent returns. When an agent stops producing
 * output and never returns, nothing notices: the chat freezes, the turn never ends, and the owner
 * waits. He reported it on 2026-10-01 — *"they stop on their own, and our orchestrator just stands
 * and waits, maybe for an hour"* — while three of this chat's own subagents had written zero bytes
 * for over 100 seconds and were still being waited on.
 *
 * The fix is not a longer timeout. It is to stop treating a blocking dispatch as the only way to
 * wait: dispatch in the background, then ask this script — bounded, once or in a polling loop — what
 * is actually happening. `scripts/lib/subagent-liveness.mjs` holds the classification and the honest
 * limits; this file is the command line and the exit code around it.
 *
 * Exit codes
 * ----------
 *   0  every watched agent is `DONE` (or the wait settled with none unhealthy)
 *   1  at least one is `FAILED` or `STALLED`, or a `--wait` expired with agents still `RUNNING`
 *   2  nothing could be observed — the transcript directory was not found or held no candidates
 *
 * `2` is deliberately not `0`. A watcher that reports "all clear" when it could not look is the
 * failure mode `AGENTS.md` names for every sensor in this repository, and it would be worst here:
 * the orchestrator would read a blind watcher as permission to keep waiting.
 *
 * Usage
 * -----
 *   node scripts/watch-subagents.mjs                        # one report, the default
 *   node scripts/watch-subagents.mjs --wait --timeout 300   # poll until all settle, or 5 minutes
 *   node scripts/watch-subagents.mjs --json                 # the same rows, machine-readable
 *   node scripts/watch-subagents.mjs --brief 36bb4b12        # where to find that agent's brief
 *
 * `--wait` prints a progress line per interval and a final `PLAYKILN SUBAGENTS SETTLED` or
 * `PLAYKILN SUBAGENTS TIMEOUT`, so a caller can block on the marker rather than on a guessed delay.
 */

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { homedir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  DEFAULT_STALL_SECONDS,
  DONE,
  RUNNING,
  UNHEALTHY_STATES,
  candidateSubagentDirs,
  resolveBriefSlug,
  scanSubagents
} from './lib/subagent-liveness.mjs'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

/** Where dispatch briefs live, so a stopped agent can be replaced instead of reconstructed. */
const BRIEF_INDEX = path.join(ROOT, '.kiln', 'subagents', 'index.tsv')

function parseArgs(argv) {
  const options = {
    wait: false,
    timeoutSeconds: 300,
    intervalSeconds: 15,
    stallSeconds: DEFAULT_STALL_SECONDS,
    windowMinutes: 120,
    json: false,
    dir: null,
    brief: null,
    help: false
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    const next = () => argv[index + 1]
    if (arg === '--wait') options.wait = true
    else if (arg === '--json') options.json = true
    else if (arg === '--help' || arg === '-h') options.help = true
    else if (arg === '--timeout') options.timeoutSeconds = Number(next()) || options.timeoutSeconds
    else if (arg === '--interval') options.intervalSeconds = Number(next()) || options.intervalSeconds
    else if (arg === '--stall') options.stallSeconds = Number(next()) || options.stallSeconds
    else if (arg === '--window') options.windowMinutes = Number(next()) || options.windowMinutes
    else if (arg === '--dir') options.dir = next()
    else if (arg === '--brief') options.brief = next()
  }
  return options
}

/** The first candidate directory, or null. Prints nothing; the caller explains what was chosen. */
function pickDir(options) {
  if (options.dir) return existsSync(options.dir) ? options.dir : null
  const dirs = candidateSubagentDirs({
    root: ROOT,
    homedir: homedir(),
    readdir: (dir) => readdirSync(dir),
    exists: (target) => existsSync(target),
    stat: (target) => statSync(target),
    override: process.env.PLAYKILN_SUBAGENT_DIR || null
  })
  return dirs[0] ?? null
}

function readRows(options, dir) {
  return scanSubagents({
    dir,
    stallSeconds: options.stallSeconds,
    createdWithinSeconds: options.windowMinutes > 0 ? options.windowMinutes * 60 : 0,
    readFile: (file) => readFileSync(file, 'utf8'),
    stat: (file) => statSync(file),
    readdir: (target) => readdirSync(target)
  })
}

/** The brief recorded for an agent id, or null. Read here so the mapping has one home. */
function briefFor(id) {
  try {
    return resolveBriefSlug(readFileSync(BRIEF_INDEX, 'utf8'), id)
  } catch {
    return null
  }
}

function reason(row) {
  if (row.state === DONE) return `success`
  if (row.state === RUNNING) return 'working'
  if (row.state === 'FAILED') return `turn_ended status=${row.status}`
  return 'no terminal record'
}

function report(rows, options) {
  const unhealthy = rows.filter((row) => UNHEALTHY_STATES.includes(row.state))
  const running = rows.filter((row) => row.state === RUNNING)

  if (options.json) {
    console.log(JSON.stringify({ rows, unhealthy: unhealthy.length, running: running.length }, null, 2))
    return
  }

  console.log(
    `PlayKiln subagent watch — ${rows.length} candidate(s), window ${options.windowMinutes}m, ` +
      `stall ${options.stallSeconds}s`
  )
  console.log('')
  for (const row of rows) {
    const kb = `${Math.round(row.sizeBytes / 1024)}KB`.padStart(6)
    const idle = `idle ${String(row.idleSeconds).padStart(5)}s`
    console.log(`  ${row.state.padEnd(8)} ${row.id}  ${kb}  ${idle}  ${reason(row)}`)
  }
  console.log('')

  if (unhealthy.length === 0 && running.length === 0) {
    console.log('all watched subagents finished')
    return
  }

  if (unhealthy.length > 0) {
    console.log(`${unhealthy.length} want attention:`)
    for (const row of unhealthy) {
      const slug = briefFor(row.id)
      // The owner's instruction was that a stopped worker is replaced, not discarded. Naming the
      // brief is what makes the replacement cheap; its absence is reported rather than papered over.
      console.log(
        slug
          ? `  ${row.id}  re-dispatch brief: .kiln/subagents/${slug}.md`
          : `  ${row.id}  no brief recorded in .kiln/subagents/index.tsv — write one, then re-dispatch`
      )
    }
  }
  if (running.length > 0) console.log(`${running.length} still working`)
}

function main() {
  const options = parseArgs(process.argv.slice(2))

  if (options.help) {
    console.log('usage: node scripts/watch-subagents.mjs [--wait] [--timeout s] [--json] [--dir path]')
    console.log('       node scripts/watch-subagents.mjs --brief <agentId>')
    return 0
  }

  if (options.brief) {
    const slug = briefFor(options.brief)
    if (!slug) {
      console.error(`no brief recorded for ${options.brief} in ${path.relative(ROOT, BRIEF_INDEX)}`)
      return 1
    }
    const file = path.join(ROOT, '.kiln', 'subagents', `${slug}.md`)
    console.log(existsSync(file) ? file : `${file} (recorded, but the file is missing)`)
    return existsSync(file) ? 0 : 1
  }

  const dir = pickDir(options)
  if (!dir) {
    console.error('could not find a subagent transcript directory; set PLAYKILN_SUBAGENT_DIR or --dir')
    return 2
  }

  if (!options.wait) {
    const rows = readRows(options, dir)
    if (rows.length === 0) {
      console.error(`no subagent transcripts in the last ${options.windowMinutes}m under ${dir}`)
      return 2
    }
    report(rows, options)
    return rows.some((row) => UNHEALTHY_STATES.includes(row.state)) ? 1 : 0
  }

  // A bounded wait: the whole point is that this loop ends whether or not the agents do, so control
  // returns to the orchestrator either way. A blocking dispatch cannot promise that.
  const deadline = Date.now() + options.timeoutSeconds * 1000
  for (;;) {
    const rows = readRows(options, dir)
    const running = rows.filter((row) => row.state === RUNNING).length
    const unhealthy = rows.filter((row) => UNHEALTHY_STATES.includes(row.state)).length

    if (rows.length === 0) {
      console.error(`no subagent transcripts in the last ${options.windowMinutes}m under ${dir}`)
      return 2
    }
    if (running === 0 || Date.now() >= deadline) {
      report(rows, options)
      const timedOut = running > 0
      console.log('')
      console.log(timedOut ? 'PLAYKILN SUBAGENTS TIMEOUT' : 'PLAYKILN SUBAGENTS SETTLED')
      // A timeout that left agents working is not a pass: the caller asked for a settled batch and
      // did not get one, and returning 0 would read as "nothing to do here".
      return unhealthy > 0 || timedOut ? 1 : 0
    }

    console.log(
      `waiting — ${running} working, ${unhealthy} need attention, ` +
        `${Math.max(0, Math.round((deadline - Date.now()) / 1000))}s left`
    )
    const until = Math.min(deadline, Date.now() + options.intervalSeconds * 1000)
    while (Date.now() < until) {
      // Sleep without a dependency: a poll loop this short does not justify a helper, and the
      // workspace-hygiene rule forbids writing anywhere but .kiln/scratch for anything heavier.
      Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, until - Date.now())
    }
  }
}

process.exitCode = main()

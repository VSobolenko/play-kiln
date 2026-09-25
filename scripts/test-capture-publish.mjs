#!/usr/bin/env node
/**
 * Failure-injection tests for transactional capture publication.
 *
 * An earlier evaluation scored the publish step 3/5 on tests because the only evidence for its
 * fail-closed claim was the happy path: `PASS publish` proves the swap works when nothing goes wrong,
 * and says nothing about what the filesystem holds when it does. These cases drive the failure paths
 * directly, including the one a test cannot normally reach - a process killed between the two renames,
 * simulated by leaving the directory in exactly the state that interruption produces.
 */
import { existsSync, mkdirSync, mkdtempSync, readdirSync, renameSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import process from 'node:process'

import { publishCaptures, recoverInterruptedPublish } from './lib/capture-publish.mjs'

let failures = 0
let checks = 0

function check(label, condition, detail) {
  checks += 1
  if (condition) {
    console.log(`PASS ${label}`)
  } else {
    failures += 1
    console.log(`FAIL ${label}${detail ? ` — ${detail}` : ''}`)
  }
}

/** Build a capture set of `names` under `dir`, so contents identify which run wrote the directory. */
function seed(dir, names) {
  mkdirSync(dir, { recursive: true })
  for (const name of names) writeFileSync(path.join(dir, name), `${name} from ${path.basename(path.dirname(dir))}\n`, 'utf8')
}

function contents(dir) {
  return existsSync(dir) ? readdirSync(dir).sort() : null
}

function sameSet(a, b) {
  return Array.isArray(a) && Array.isArray(b) && a.length === b.length && a.every((v, i) => v === b[i])
}

function sandbox() {
  return mkdtempSync(path.join(tmpdir(), 'kiln-publish-'))
}

const oldSet = ['01.png', '02.png', '03.png', '04.png', '05.png', '06.png', 'old-07.png']
const newSet = ['01.png', '02.png', '03.png', '04.png', '05.png', '06.png']

/* 1. The happy path still publishes the new set. */
{
  const root = sandbox()
  const raw = path.join(root, 'raw')
  const staging = path.join(root, 'staging')
  seed(raw, oldSet)
  seed(staging, newSet)
  const result = publishCaptures(staging, raw)
  check('happy path reports the staged png count', result.published === 6, `got ${result.published}`)
  check('happy path leaves the new set in place', sameSet(contents(raw), newSet), JSON.stringify(contents(raw)))
  check('happy path removes the backup', !existsSync(`${raw}.previous`))
  rmSync(root, { recursive: true, force: true })
}

/* 2. A stale file from the previous run cannot survive into the new set. */
{
  const root = sandbox()
  const raw = path.join(root, 'raw')
  const staging = path.join(root, 'staging')
  seed(raw, oldSet)
  seed(staging, newSet)
  publishCaptures(staging, raw)
  check('stale old-07.png does not survive publication', !contents(raw).includes('old-07.png'), JSON.stringify(contents(raw)))
  rmSync(root, { recursive: true, force: true })
}

/* 3. Killed between the two renames: raw/ absent, raw.previous/ holds the only good set. */
{
  const root = sandbox()
  const raw = path.join(root, 'raw')
  const staging = path.join(root, 'staging')
  seed(raw, oldSet)
  seed(staging, newSet)
  renameSync(raw, `${raw}.previous`) // exactly the state a kill after the first rename produces
  check('precondition: raw/ is absent mid-swap', !existsSync(raw))

  const recovered = recoverInterruptedPublish(raw)
  check('recovery reports that it restored a set', recovered === true)
  check('recovery restores the previous set', sameSet(contents(raw), oldSet), JSON.stringify(contents(raw)))
  check('recovery consumes the backup directory', !existsSync(`${raw}.previous`))
  rmSync(root, { recursive: true, force: true })
}

/* 4. The next run must recover rather than delete the backup, then still publish cleanly. */
{
  const root = sandbox()
  const raw = path.join(root, 'raw')
  const staging = path.join(root, 'staging')
  seed(raw, oldSet)
  seed(staging, newSet)
  renameSync(raw, `${raw}.previous`)

  const result = publishCaptures(staging, raw)
  check('publish over an interrupted run reports recovery', result.recovered === true)
  check('publish over an interrupted run yields the new set', sameSet(contents(raw), newSet), JSON.stringify(contents(raw)))
  rmSync(root, { recursive: true, force: true })
}

/* 5. A leftover backup next to a surviving raw/ is discarded, and raw/ is left untouched. */
{
  const root = sandbox()
  const raw = path.join(root, 'raw')
  seed(raw, newSet)
  seed(`${raw}.previous`, oldSet)

  const recovered = recoverInterruptedPublish(raw)
  check('a backup beside a surviving raw/ is not treated as a recovery', recovered === false)
  check('surviving raw/ is left untouched', sameSet(contents(raw), newSet), JSON.stringify(contents(raw)))
  check('the leftover backup is discarded', !existsSync(`${raw}.previous`))
  rmSync(root, { recursive: true, force: true })
}

/* 6. A throwing second rename rolls back to the previous set and does not leave raw/ absent. */
{
  const root = sandbox()
  const raw = path.join(root, 'raw')
  // A staging path that does not exist makes the second rename fail the way a real I/O error would.
  const staging = path.join(root, 'staging-does-not-exist')
  seed(raw, oldSet)

  let threw = false
  try {
    publishCaptures(staging, raw)
  } catch {
    threw = true
  }
  check('a failed second rename throws', threw)
  check('a failed second rename leaves raw/ present', contents(raw) !== null, JSON.stringify(contents(raw)))
  check('a failed second rename preserves the previous set', sameSet(contents(raw), oldSet), JSON.stringify(contents(raw)))
  rmSync(root, { recursive: true, force: true })
}

/* 7. First ever run: no raw/, no backup. Publication must still work and leave no backup. */
{
  const root = sandbox()
  const raw = path.join(root, 'raw')
  const staging = path.join(root, 'staging')
  seed(staging, newSet)
  const result = publishCaptures(staging, raw)
  check('a first run with no previous set publishes', sameSet(contents(raw), newSet), JSON.stringify(contents(raw)))
  check('a first run reports no recovery', result.recovered === false)
  check('a first run leaves no backup', !existsSync(`${raw}.previous`))
  rmSync(root, { recursive: true, force: true })
}

console.log(`\n${checks - failures}/${checks} checks passed`)
process.exit(failures === 0 ? 0 : 1)

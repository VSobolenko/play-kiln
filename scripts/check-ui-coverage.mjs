#!/usr/bin/env node
/**
 * Does every operation the product claims actually have a control?
 *
 * Five UI screens were restyled in parallel, and one of them dropped the add/edit controls for notes
 * and parent contacts while leaving the read-only list in place. Nothing caught it: the removed code
 * was inside files the compiler still accepted, the domain tests exercise `RecordBookEditor` directly
 * so every edit operation still passes, and `verify.mjs` checks form - it builds, it finds the
 * store pack complete, it reads the manifest - none of which can see a button that no longer exists.
 *
 * The signal this script uses is a dangling ViewModel operation. A ViewModel method that no screen
 * calls is not proof of a missing control (a method can be legitimately internal, or reached only from
 * a test), so the script does not simply fail on orphans. It separates them:
 *
 *   - **REQUIRED** operations are the ones `store/listing.json` tells a buyer the app does. If the
 *     listing says a teacher can record a note, and no screen can record a note, the listing is a
 *     false claim and Play review or a buyer will find it. That is a failure.
 *   - **optional** orphans are reported as notes, because the author may know a reason.
 *
 * Why the two tables are written out rather than derived from the listing text: deriving them would
 * mean matching prose to identifiers, and a heuristic that guesses wrong either misses a dropped
 * control or fails on a legitimate rename. An explicit list with the claim it enforces beside it is
 * reviewable, and a reviewer adding a feature is the person who should add its line. The tables live
 * in `scripts/core/policy.mjs`, the single home for them, and are imported here so this CLI and the
 * core's `slot.uiCoverage` check cannot drift apart.
 *
 * **The defect this table then caused, recorded 2026-09-29.** Because the table is written for the one
 * slot it was authored against, it reported a *pass* for every other slot while checking nothing: a
 * slot with no ViewModel under `ui/` printed "nothing to check", and a slot whose ViewModels share none
 * of these names printed "0 reachable, 0 reported as notes". An independent review reproduced it on
 * `apps/2026-09-25-pipe-sizing` — a green line for a check that structurally cannot go red, printed by
 * `verify.mjs` for every slot unconditionally. That is precisely what
 * `.cursor/rules/verification-discipline.mdc` forbids ("a check that cannot fail is not a check"), so
 * the two ways of checking nothing are now `UNVERIFIED` (exit 2) rather than `PASS`: the honest
 * statement is "this check cannot speak for this slot", and it stops a release from being called green
 * until someone writes the slot's own mapping.
 *
 * Usage:
 *   node scripts/check-ui-coverage.mjs --slot teacher-gradebook
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { NOT_A_CONTROL, REQUIRED } from './core/policy.mjs'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

function findSlot(slug) {
  const apps = path.join(root, 'apps')
  const matches = fs
    .readdirSync(apps)
    .filter((name) => name === slug || name.endsWith(`-${slug}`))
    .filter((name) => fs.statSync(path.join(apps, name)).isDirectory())
  if (matches.length === 0) throw new Error(`no slot under apps/ matches '${slug}'`)
  if (matches.length > 1) throw new Error(`'${slug}' is ambiguous: ${matches.join(', ')}`)
  return path.join(apps, matches[0])
}

function kotlinFiles(dir) {
  const out = []
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) out.push(...kotlinFiles(full))
    else if (entry.name.endsWith('.kt')) out.push(full)
  }
  return out
}

/**
 * Public functions declared by a class in this file.
 *
 * `^    fun` is the four-space body of a class in this codebase and is deliberately narrow: a nested
 * `private fun` is indented further and an interface's `fun` has no body, so neither is collected.
 * `private` and `internal` are excluded because they cannot be the missing control.
 */
function declaredOperations(source) {
  const found = new Set()
  for (const match of source.matchAll(/^ {4}fun ([A-Za-z_][A-Za-z0-9_]*)\s*\(/gm)) {
    found.add(match[1])
  }
  return found
}

function main() {
  const slotArgIndex = process.argv.indexOf('--slot')
  if (slotArgIndex === -1 || !process.argv[slotArgIndex + 1]) {
    console.error('usage: node scripts/check-ui-coverage.mjs --slot <slug>')
    process.exit(2)
  }
  const slot = findSlot(process.argv[slotArgIndex + 1])
  const main = path.join(slot, 'app', 'src', 'main', 'java')
  const uiDirs = kotlinFiles(main).filter((file) => file.includes(`${path.sep}ui${path.sep}`))

  const viewModels = uiDirs.filter((file) => path.basename(file).endsWith('ViewModel.kt'))
  if (viewModels.length === 0) {
    // This used to be a PASS. It is not one: with no ViewModel under `ui/` there is no operation to
    // compare against any claim, so the check has measured nothing and must not be read as a clean
    // result. Exit 2 is the repository's convention for exactly this ("measured once, unverified
    // now"), and `verify.mjs` turns it into UNVERIFIED rather than a green line.
    console.log(
      `UNVERIFIED ui-coverage - ${path.basename(slot)} declares no ViewModel under ui/, so nothing ` +
        'about the listing\'s claims could be checked. Point REQUIRED at this slot\'s own operations ' +
        'before treating this as evidence.',
    )
    process.exit(2)
  }

  // A screen's own file is not a caller of its own ViewModel: the operation is dangling unless some
  // *other* file reaches it, so the ViewModel files are removed from the search corpus.
  const corpus = uiDirs
    .filter((file) => !viewModels.includes(file))
    .map((file) => ({ file, source: fs.readFileSync(file, 'utf8') }))

  const declared = new Set()
  for (const vm of viewModels) {
    for (const operation of declaredOperations(fs.readFileSync(vm, 'utf8'))) declared.add(operation)
  }

  // The required table is authored for one slot. When none of its names exists in this slot's
  // ViewModels, the table describes a different app, so a pass here would be a claim about a check
  // that never ran — the exact false green an independent review found on `pipe-sizing`.
  const applicable = Object.keys(REQUIRED).filter((operation) => declared.has(operation))
  if (applicable.length === 0) {
    console.log(
      `UNVERIFIED ui-coverage - ${path.basename(slot)} declares ${declared.size} ViewModel ` +
        `operation(s) and none of them is named by REQUIRED, so this table cannot check this slot. ` +
        'Add its claimed operations to REQUIRED, with the listing sentence each one keeps.',
    )
    process.exit(2)
  }

  const missing = []
  const notes = []
  let checked = 0

  for (const vm of viewModels) {
    for (const operation of declaredOperations(fs.readFileSync(vm, 'utf8'))) {
      const reachable = corpus.some(
        ({ source }) => new RegExp(`\\b${operation}\\s*\\(|::${operation}\\b`).test(source),
      )
      if (reachable) {
        checked += 1
        continue
      }
      if (NOT_A_CONTROL[operation]) continue
      if (REQUIRED[operation]) missing.push({ operation, claim: REQUIRED[operation] })
      else notes.push(operation)
    }
  }

  for (const operation of notes) {
    console.log(`NOTE ui-coverage - ViewModel '${operation}' has no screen caller, and no claim depends on it`)
  }

  if (missing.length > 0) {
    for (const { operation, claim } of missing) {
      console.error(
        `FAIL ui-coverage - ViewModel operation '${operation}' has no caller in any screen, but ` +
          `${claim}. Either restore the control or change store/listing.json, so the listing and the ` +
          'build cannot disagree about what the app does.',
      )
    }
    console.error(
      `\nui-coverage - ${missing.length} claimed operation(s) unreachable, ${checked} reachable. ` +
        'This is what a restyle removes silently: the code compiles and the domain tests still pass, ' +
        'because the edit logic is exercised directly and no screen ever calls it.',
    )
    process.exit(1)
  }

  console.log(
    `PASS ui-coverage - every operation the listing claims is reachable from a screen ` +
      `(${checked} reachable, ${notes.length} reported as notes)`,
  )
}

try {
  main()
} catch (error) {
  console.error(`BLOCKED ui-coverage - ${error.message}`)
  process.exit(2)
}

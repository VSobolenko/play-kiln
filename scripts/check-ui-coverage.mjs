#!/usr/bin/env node
/**
 * Does every operation the product claims actually have a control?
 *
 * Five UI screens were restyled in parallel, and one of them dropped the add/edit controls for notes
 * and parent contacts while leaving the read-only list in place. Nothing caught it: the removed code
 * was inside files the compiler still accepted, the domain tests exercise `RecordBookEditor` directly
 * so every edit operation still passes, and `verify-slot.mjs` checks form - it builds, it finds the
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
 * Why the list is written out here rather than derived from the listing text: deriving it would mean
 * matching prose to identifiers, and a heuristic that guesses wrong either misses a dropped control or
 * fails on a legitimate rename. An explicit list with the claim it enforces beside it is reviewable,
 * and a reviewer adding a feature is the person who should add its line.
 *
 * Usage:
 *   node scripts/check-ui-coverage.mjs --slot teacher-gradebook
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

/**
 * Operations the store listing promises, with the promise each one keeps.
 *
 * The `claim` text is a quotation or a close paraphrase of `store/listing.json`'s `fullDescription`,
 * so this table can be checked against the listing it guards rather than trusted.
 */
const REQUIRED = {
  addClass: 'the listing sells "create, rename, edit and delete them" for classes',
  renameClass: 'the listing sells "create, rename, edit and delete them" for classes',
  updateClassDetails: 'the listing sells "edit" for classes (subject, period, term)',
  deleteClass: 'the listing sells deleting a class, with its roster and marks',
  addStudent: 'the listing sells "add a roster by hand"',
  updateStudent: 'the listing sells "edit anyone, mark someone inactive when they leave"',
  deleteStudent: 'the listing sells "or delete them"',
  importRoster: 'the listing sells "or import a CSV ... never adds the same student twice"',
  addCategory: 'the listing sells "weigh coursework, exams and homework however your department does"',
  updateCategory: 'the listing sells changing a category weight',
  deleteCategory: 'a category that can be added and weighed must be removable',
  moveCategory: 'the listing sells "reorder them"',
  addAssignment: 'the listing sells recording a score per assignment',
  updateAssignment: 'the listing sells weighted categories, which requires editing an assignment',
  deleteAssignment: 'an assignment that can be added must be removable',
  setGrade: 'the listing sells "record a score, mark work missing, or exempt a student"',
  clearGrade: 'the listing sells that "an empty cell is never written as a zero"',
  setAttendance: 'the listing sells "mark a register per day"',
  clearAttendance: 'a register that can be marked must be correctable',
  markAllPresent: 'the listing sells "with one tap for everyone present"',
  addNote: 'the listing sells "participation notes"',
  updateNote: 'the listing sells notes with a kind and a date',
  deleteNote: 'a note that can be recorded must be removable',
  addContact: 'the listing sells "a log of who you contacted, when, and what about"',
  updateContact: 'the listing sells "follow-up dates that come back to you"',
  deleteContact: 'a contact that can be logged must be removable',
  resizeSeating: 'the listing sells "set a grid of desks"',
  setSeat: 'the listing sells "and put students in them"',
  moveToNextFreeDesk: 'the listing sells seating plans a teacher does not redraw',
  requestExport: 'the listing sells CSV and PDF export',
  requestBackup: 'the listing sells writing "your whole record to one file you keep"',
  restoreBackup: 'the listing sells restoring that file later',
}

/** Operations that are deliberately not called from a screen, with the reason they are not. */
const NOT_A_CONTROL = {
  load: 'called by the shell on entry, not by a button',
  announce: 'how a screen posts a message; called from other ViewModel code',
  next: 'the id generator, used by the ViewModel itself',
  isUnlocked: 'the entitlement question, asked by the ViewModel and by tests',
  classroom: 'a state accessor a screen reads through `state`, not an action',
  averageOf: 'a read, reached through the average surface or the shell',
  csvFor: 'produces the export payload; `requestExport` is the control',
  backupDocument: 'produces the backup payload; `requestBackup` is the control',
}

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
    console.log(`PASS ui-coverage - ${path.basename(slot)} has no ViewModel under ui/, nothing to check`)
    return
  }

  // A screen's own file is not a caller of its own ViewModel: the operation is dangling unless some
  // *other* file reaches it, so the ViewModel files are removed from the search corpus.
  const corpus = uiDirs
    .filter((file) => !viewModels.includes(file))
    .map((file) => ({ file, source: fs.readFileSync(file, 'utf8') }))

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

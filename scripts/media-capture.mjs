#!/usr/bin/env node
/** Capture a six-step store journey with ADB, then delegate framing to store-kit.py. */

import { spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { existsSync, mkdirSync, readFileSync, readdirSync, renameSync, rmSync, statSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { pathToFileURL } from 'node:url'

import { acquireCaptureLock, publishCaptures, recoverInterruptedPublish } from './lib/capture-publish.mjs'
import { buildInputsUnchangedBetween, verifyArtifactChain } from './lib/apk-provenance.mjs'
import { checkReleaseApproval } from './lib/release-approval.mjs'

const ROOT = process.cwd()

function usage() {
  console.log('Usage: node scripts/media-capture.mjs --slot <slug> [--serial <device>] [--attempts N]')
  console.log('Captures store/journey.json with ADB, verifies each screen, saves store/captures/raw/, then frames assets.')
  console.log('A run that cannot prove what it photographed is retried, then fails closed without touching the assets.')
}

function parseArgs(argv) {
  if (argv.includes('--help') || argv.includes('-h')) {
    usage()
    process.exit(0)
  }
  const allowed = new Set(['--slot', '--serial', '--attempts'])
  const result = { slot: null, serial: null, attempts: 3 }
  for (let i = 0; i < argv.length; i += 2) {
    const flag = argv[i]
    const value = argv[i + 1]
    if (!allowed.has(flag) || !value || value.startsWith('--')) {
      throw new Error(`invalid arguments near ${flag || '(end)'}; run with --help`)
    }
    if (flag === '--slot') result.slot = value
    if (flag === '--serial') result.serial = value
    if (flag === '--attempts') {
      const attempts = Number(value)
      if (!Number.isInteger(attempts) || attempts < 1) throw new Error('--attempts must be a positive whole number')
      result.attempts = attempts
    }
  }
  if (!result.slot) throw new Error('--slot <slug> is required')
  return result
}

function resolveSlot(slug) {
  const apps = path.join(ROOT, 'apps')
  if (!existsSync(apps)) throw new Error(`apps directory does not exist: ${apps}`)
  const matches = readdirSync(apps)
    .map((name) => path.join(apps, name))
    .filter((candidate) => statSync(candidate).isDirectory())
    .filter((candidate) => path.basename(candidate) === slug || path.basename(candidate).endsWith(`-${slug}`))
    .sort()
  if (matches.length === 0) throw new Error(`slot '${slug}' was not found under apps/`)
  if (matches.length > 1) throw new Error(`slot '${slug}' is ambiguous: ${matches.map(path.basename).join(', ')}`)
  return matches[0]
}

function executableWorks(executable) {
  const result = spawnSync(executable, ['version'], { encoding: 'utf8' })
  return !result.error && result.status === 0
}

function findAdb() {
  if (executableWorks('adb')) return 'adb'
  const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT
  if (sdk) {
    const candidate = path.join(sdk, 'platform-tools', process.platform === 'win32' ? 'adb.exe' : 'adb')
    if (existsSync(candidate) && executableWorks(candidate)) return candidate
  }
  return null
}

/**
 * Stop without touching the assets.
 *
 * `hint` exists because the emulator instructions were previously printed for every failure, including
 * a release-hash mismatch and a missing `listing.json`, where they send the operator looking for a
 * device that is already attached and working. Advice attached to the wrong cause costs more than no
 * advice.
 */
function block(reason, hint) {
  console.error(`BLOCKED: ${reason}`)
  if (hint) for (const line of hint) console.error(line)
  process.exit(2)
}

function emulatorHint() {
  return [
    'BLOCKED: start an emulator with:',
    '  $ANDROID_HOME/emulator/emulator -list-avds',
    '  $ANDROID_HOME/emulator/emulator -avd <AVD_NAME>',
    '  adb wait-for-device'
  ]
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: ROOT,
    encoding: options.binary ? null : 'utf8',
    maxBuffer: 32 * 1024 * 1024
  })
  if (result.error || result.status !== 0) {
    const detail = result.error?.message || String(result.stderr || result.stdout || '').trim()
    throw new Error(`${command} ${args.join(' ')} failed: ${detail}`)
  }
  return result.stdout
}

function sleep(milliseconds) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, Math.max(0, milliseconds))
}

function defaultJourney() {
  const captions = [
    'Start with a clear overview',
    'Choose what matters now',
    'Take the next focused step',
    'See your progress clearly',
    'Build a steady daily rhythm',
    'Finish with confidence'
  ]
  const stepSets = [
    [],
    [{ type: 'tap', x: 540, y: 1600 }],
    [{ type: 'wait', ms: 800 }, { type: 'tap', x: 540, y: 1450 }],
    [{ type: 'swipe', x1: 540, y1: 1500, x2: 540, y2: 600, durationMs: 350 }],
    [{ type: 'back' }],
    [{ type: 'tap', x: 950, y: 120 }]
  ]
  return {
    schemaVersion: 1,
    locale: 'en-US',
    _comment: 'Edit coordinates/text for the real app. Each shot runs its steps, then captures one raw PNG.',
    shots: captions.map((title, index) => ({
      name: `shot-${String(index + 1).padStart(2, '0')}`,
      title,
      body: `Journey step ${index + 1}`,
      steps: stepSets[index]
    })),
    supportedStepTypes: {
      tap: { x: 540, y: 1200 },
      swipe: { x1: 540, y1: 1500, x2: 540, y2: 500, durationMs: 350 },
      wait: { ms: 750 },
      text: { value: 'example text' },
      clear: { count: 40 },
      back: {}
    }
  }
}

/**
 * The device's primary language, read from `am get-config`'s own locale list.
 *
 * The output is `config: <locale list>-<qualifier>...`, e.g. `en-rUS,ru-rRU,be-rBY-ldltr-sw360dp-...`.
 * Only the locale list may carry `-rXX` region subtags, and it ends at the first `-` that is not one,
 * so the grammar below separates the two rather than guessing a token index. `getprop
 * persist.sys.locale` is deliberately not used: it read `en-US` on a device whose live framework
 * config was `ru-rRU`, so it is the last value written, not the locale in force.
 *
 * Returns the lower-cased primary subtag (`en`, `ru`) or null when the output does not match.
 */
export function devicePrimaryLanguage(amGetConfigOutput) {
  const match = String(amGetConfigOutput).match(
    /^config:\s*((?:[a-z]{2,3}(?:-r[A-Z]{2})?)(?:,(?:[a-z]{2,3}(?:-r[A-Z]{2})?))*(?:-|$))/m
  )
  if (!match) return null
  return match[1].split(',')[0].split('-')[0].toLowerCase()
}

/** The language a journey's expectations are written in; `en` when the journey does not declare one. */
export function journeyLanguage(journey) {
  const declared = typeof journey?.locale === 'string' ? journey.locale.trim() : ''
  return (declared ? declared.split(/[-_]/)[0] : 'en').toLowerCase()
}

function loadJson(file, description) {
  try {
    return JSON.parse(readFileSync(file, 'utf8'))
  } catch (error) {
    throw new Error(`cannot read ${description} ${file}: ${error.message}`)
  }
}

function adb(adbPath, serial, args, options = {}) {
  return run(adbPath, ['-s', serial, ...args], options)
}

function executeStep(adbPath, serial, step) {
  switch (step.type) {
    case 'tap':
      adb(adbPath, serial, ['shell', 'input', 'tap', String(step.x), String(step.y)])
      break
    case 'swipe':
      adb(adbPath, serial, [
        'shell', 'input', 'swipe', String(step.x1), String(step.y1),
        String(step.x2), String(step.y2), String(step.durationMs ?? 350)
      ])
      break
    case 'wait':
      sleep(Number(step.ms ?? 0))
      break
    case 'text':
      adb(adbPath, serial, ['shell', 'input', 'text', String(step.value ?? '').replaceAll(' ', '%s')])
      break
    case 'clear':
      // Move to the end, then delete. Without this, typing into a field that already holds the
      // persisted case *appends* to it: the app deliberately restores the entered case across
      // process death, so a run that starts from a non-empty field silently corrupts the capture.
      adb(adbPath, serial, ['shell', 'input', 'keyevent', '123'])
      for (let index = 0; index < Number(step.count ?? 40); index += 1) {
        adb(adbPath, serial, ['shell', 'input', 'keyevent', '67'])
      }
      break
    case 'back':
      adb(adbPath, serial, ['shell', 'input', 'keyevent', '4'])
      break
    default:
      throw new Error(`unsupported journey step type: ${JSON.stringify(step.type)}`)
  }
}

/**
 * Disable every enabled IME, retrying until the enabled list is actually empty.
 * `ime disable` races the IME service restarting, so a single call is a no-op often enough
 * to matter, and a live keyboard both autocorrects typed values and rescrolls the form.
 */
function disableImes(adbPath, serial) {
  for (let attempt = 0; attempt < 6; attempt += 1) {
    const enabled = String(adb(adbPath, serial, ['shell', 'ime', 'list', '-s']))
      .split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
    if (enabled.length === 0) return true
    for (const id of enabled) {
      try { adb(adbPath, serial, ['shell', 'ime', 'disable', id]) } catch { /* retried below */ }
    }
    sleep(500)
  }
  return String(adb(adbPath, serial, ['shell', 'ime', 'list', '-s'])).trim() === ''
}

/** Decode the XML entities `uiautomator` escapes attribute values with. */
function decodeXml(value) {
  return String(value)
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
    .replaceAll('&quot;', '"')
    .replaceAll('&apos;', "'")
    .replaceAll('&#10;', '\n')
    .replaceAll('&amp;', '&')
}

/**
 * Parse a `uiautomator dump` into the nodes it describes.
 *
 * Every attribute the field check needs is kept, not just `text`: `class` separates an editable
 * input from a paragraph, `content-desc` and `hint` are where Compose puts a text field's label,
 * `resource-id` is the stable identity when a slot sets one, and `bounds` is what lets a value be
 * tied to the field its label names. The old reader kept only `text` and returned a flat list, which
 * is the defect this reader exists to undo: a flat list cannot say WHICH field a reading is in.
 */
export function parseDump(xml) {
  const nodes = []
  for (const match of String(xml).matchAll(/<node\b([^>]*)>/g)) {
    const attributes = {}
    for (const attribute of match[1].matchAll(/([\w-]+)="([^"]*)"/g)) {
      attributes[attribute[1]] = decodeXml(attribute[2])
    }
    const bounds = (attributes.bounds || '').match(/\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]/)
    nodes.push({
      text: attributes.text || '',
      resourceId: attributes['resource-id'] || '',
      contentDesc: attributes['content-desc'] || '',
      hint: attributes.hint || '',
      className: attributes.class || '',
      bounds: bounds
        ? { left: Number(bounds[1]), top: Number(bounds[2]), right: Number(bounds[3]), bottom: Number(bounds[4]) }
        : null
    })
  }
  return nodes
}

/** Dump the live view hierarchy, keep it beside the capture it describes, return its parsed nodes. */
function dumpScreen(adbPath, serial, rawDir, name) {
  const xml = String(adb(adbPath, serial, ['exec-out', 'uiautomator', 'dump', '/dev/tty']))
  writeFileSync(path.join(rawDir, `${name}.xml`), xml, 'utf8')
  return parseDump(xml)
}

/** The classes `uiautomator` gives an editable input; a Compose text field surfaces as EditText. */
const EDITABLE_CLASS = /EditText|AutoCompleteTextView|TextField/i

function isEditable(node) {
  return EDITABLE_CLASS.test(node.className)
}

/**
 * Normalize one `expect` entry to `{ value, field }`.
 *
 * A plain string stays backwards compatible as the value only, but it is the WEAKEST form: with no
 * field named the value is accepted in any editable input, so it cannot tell a reading in the wrong
 * field from one in the right field. The shipped journey names a field for every entry.
 */
function normalizeExpectation(entry) {
  if (typeof entry === 'string') return { value: entry, field: null }
  if (entry && typeof entry === 'object' && !Array.isArray(entry)) {
    if (typeof entry.value !== 'string') {
      throw new Error(`an 'expect' object must have a string 'value'; got ${JSON.stringify(entry)}`)
    }
    let field = null
    if (typeof entry.field === 'string') field = { label: entry.field }
    else if (entry.field && typeof entry.field === 'object') field = { ...entry.field }
    else if (entry.field !== undefined && entry.field !== null) {
      throw new Error(`the 'field' of ${JSON.stringify(entry.value)} must be a string or an object`)
    }
    return { value: entry.value, field }
  }
  throw new Error(`an 'expect' entry must be a string or a { value, field } object; got ${JSON.stringify(entry)}`)
}

/** The expectations a shot (or any source object) declares, normalized. */
export function expectedReadings(source) {
  const declared = Array.isArray(source?.expect) ? source.expect : []
  return declared.map((entry) => normalizeExpectation(entry))
}

/**
 * Reject expectations that cannot be contradicted, before a device is touched.
 *
 * A one-character reading (`"4"`, `"A"`, `"2"`) is satisfied by virtually any screen - another field,
 * a unit, a page number, a coincidental glyph - so accepting it inflates the check's count while
 * removing its ability to fail. Refusing it at load time tells the author the claim is unfalsifiable
 * while editing the journey, not after a device run that reported PASS.
 */
export function validateExpectations(entries, label) {
  for (const entry of entries) {
    if (entry.value.trim().length < 2) {
      throw new Error(
        `${label}: the expected reading ${JSON.stringify(entry.value)} is shorter than 2 characters, ` +
        'so any screen could satisfy it. Give it a value the screen cannot show by accident (a ' +
        'reading with its unit or context), or drop it. Bare single characters and digits ' +
        '("4", "A", "2") are refused rather than accepted.'
      )
    }
  }
  return entries
}

function matchesPattern(value, pattern) {
  if (!pattern) return false
  try {
    return new RegExp(pattern).test(value)
  } catch (error) {
    throw new Error(`invalid field pattern ${JSON.stringify(pattern)}: ${error.message}`)
  }
}

/**
 * True when two nodes share a region of the screen: one contains the other, or their boxes overlap.
 *
 * Only containment and overlap count. A looser "nearby in the same row or column" rule was rejected
 * because it would re-introduce the defect it is meant to fix: the label of the NEXT field down can
 * sit within a few dozen pixels of the previous field's value, so a gap-tolerant rule would let a
 * reading in the wrong field satisfy the right field's label. Failing closed on an unusual layout is
 * the intended trade - the capture retries, then blocks, rather than shipping a wrong screenshot.
 */
function boundsRelated(a, b) {
  if (!a || !b) return false
  const contains = (outer, inner) =>
    inner.left >= outer.left - 2 && inner.top >= outer.top - 2 &&
    inner.right <= outer.right + 2 && inner.bottom <= outer.bottom + 2
  if (contains(a, b) || contains(b, a)) return true
  const overlapX = Math.min(a.right, b.right) - Math.max(a.left, b.left)
  const overlapY = Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top)
  return overlapX > 0 && overlapY > 0
}

/** Whether a node's own identity (not its value) satisfies a field pattern. */
function identityMatches(node, pattern) {
  return matchesPattern(node.resourceId, pattern) ||
    matchesPattern(node.contentDesc, pattern) ||
    matchesPattern(node.hint, pattern) ||
    matchesPattern(node.text, pattern)
}

/**
 * Whether `node` is the field `field` names.
 *
 * `label`/`resourceId` first accept a node that identifies itself - Compose puts a text field's
 * label in the node's `content-desc` or `hint` on some builds, and a result line names itself in its
 * own text - and otherwise require an identity node with the matching label to contain, be contained
 * by, or overlap the value node. That geometric link is what makes "the value is in the field its
 * label names" a claim the dump can contradict; a flat list of texts had no notion of a field.
 */
function nodeMatchesField(nodes, node, field) {
  if (!field) return isEditable(node)
  if (field.editable === true) return isEditable(node)
  const pattern = field.resourceId || field.label
  if (!pattern) throw new Error(`a field must name a 'label' or 'resourceId'; got ${JSON.stringify(field)}`)
  if (field.resourceId && matchesPattern(node.resourceId, pattern)) return true
  if (field.label && identityMatches(node, pattern)) return true
  const identities = nodes.filter((candidate) => field.resourceId
    ? matchesPattern(candidate.resourceId, pattern)
    : identityMatches(candidate, pattern))
  return identities.some((identity) => identity !== node && boundsRelated(identity.bounds, node.bounds))
}

function describeField(field) {
  if (!field || field.editable === true) return 'as the text of an editable input'
  if (field.resourceId) return `in a node whose resource-id matches ${JSON.stringify(field.resourceId)}`
  return `as the text of the field labelled ${JSON.stringify(field.label)}`
}

function describeNode(node) {
  const parts = []
  if (node.text) parts.push(`text=${JSON.stringify(node.text)}`)
  if (node.contentDesc) parts.push(`content-desc=${JSON.stringify(node.contentDesc)}`)
  if (node.hint) parts.push(`hint=${JSON.stringify(node.hint)}`)
  if (node.resourceId) parts.push(`resource-id=${JSON.stringify(node.resourceId)}`)
  if (node.className) parts.push(`class=${node.className}`)
  return parts.length > 0 ? parts.join(' ') : '(empty node)'
}

function nearbyIdentities(nodes, node) {
  if (!node?.bounds) return []
  return nodes
    .filter((candidate) => candidate !== node && candidate.bounds && boundsRelated(candidate.bounds, node.bounds))
    .map(describeNode)
    .slice(0, 6)
}

function matchExpectation(nodes, entry) {
  const valueNodes = nodes.filter((node) => node.text === entry.value)
  return { valueNodes, matched: valueNodes.find((node) => nodeMatchesField(nodes, node, entry.field)) || null }
}

/**
 * Assert that a screen really shows what the shot claims to show, IN THE FIELD IT NAMES.
 *
 * A screenshot is a claim about the product, and this is the only thing in the pipeline that reads
 * one back: every other check proves a PNG exists at the right size, which is how six captures of
 * IME autocorrect gibberish ('0.5 Paige 100y40v4 5 to the ...') once passed as this app's entire
 * store presence. Reading it back as a FLAT list of texts fixed that case but was still field-blind,
 * and three failure modes proved it: a value typed into the wrong input passed because the text
 * appeared somewhere; the four readings with no form on screen at all passed because they appeared
 * in labels or result paragraphs; and one-character entries like "4" or "A" were satisfied by
 * almost any screen. Each expectation now names the field (or node) it belongs to, a plain string
 * must at least be the text of an editable input, and an entry that no screen could contradict is
 * refused before a device is touched. Every failure throws; nothing here degrades to a warning.
 */
export function assertReadings(nodes, expected, label) {
  const entries = validateExpectations(expectedReadings({ expect: expected }), label)
  if (entries.length === 0) {
    console.log(`WARN verify - ${label} declares no 'expect' entries; screen read:`)
    console.log(`  ${nodes.map((node) => node.text).filter(Boolean).join(' \u00b7 ')}`)
    return
  }
  const failures = []
  for (const entry of entries) {
    const { valueNodes, matched } = matchExpectation(nodes, entry)
    if (!matched) failures.push({ entry, valueNodes })
  }
  if (failures.length > 0) {
    console.error(`  screen read: ${nodes.map((node) => node.text).filter(Boolean).join(' \u00b7 ')}`)
    for (const { entry, valueNodes } of failures) {
      console.error(`  expected ${JSON.stringify(entry.value)} ${describeField(entry.field)}`)
      if (valueNodes.length === 0) {
        const editable = nodes.filter(isEditable).map(describeNode)
        console.error(`    no node has that text; editable nodes read: ${editable.length > 0 ? editable.join(' | ') : '(none)'}`)
      } else {
        console.error('    the text exists but not in that field; candidates read:')
        for (const candidate of valueNodes.slice(0, 4)) {
          console.error(`      ${describeNode(candidate)}`)
          const nearby = nearbyIdentities(nodes, candidate)
          if (nearby.length > 0) console.error(`        nearby: ${nearby.join(' | ')}`)
        }
      }
    }
    const summary = failures
      .map(({ entry }) => `${JSON.stringify(entry.value)} ${describeField(entry.field)}`)
      .join('; ')
    throw new Error(
      `${label} does not show what it claims to show; ${failures.length} of ${entries.length} ` +
      `expected readings are not in the field they name: ${summary}`
    )
  }
  console.log(`PASS verify - ${label} shows all ${entries.length} expected readings in their named fields`)
}

/**
 * Replay the journey from a wiped, keyboard-free state and verify every screen before it is
 * photographed. Throws on the first screen that does not show what its shot claims to show.
 */
function replayJourney({ adbPath, serial, journey, stageDir, slotDir, listing }) {
  // Enforce the preconditions the journey documents but the tool used to leave to the operator.
  adb(adbPath, serial, ['shell', 'pm', 'clear', listing.applicationId])
  if (!disableImes(adbPath, serial)) {
    throw new Error('a soft keyboard is still enabled; it autocorrects typed values and rescrolls the form')
  }
  verifyInstalledBuild(adbPath, serial, listing.applicationId, stageDir, slotDir)
  adb(adbPath, serial, ['shell', 'monkey', '-p', listing.applicationId, '-c', 'android.intent.category.LAUNCHER', '1'])
  sleep(1000)
  journey.shots.slice(0, 6).forEach((shot, index) => {
    if (!Array.isArray(shot.steps)) throw new Error(`shot ${index + 1} steps must be an array`)
    const expectations = expectedReadings(shot)
    const label = `${shot.name} (${shot.title})`
    shot.steps.forEach((step) => executeStep(adbPath, serial, step))
    const ordinal = String(index + 1).padStart(2, '0')
    // Read the screen on BOTH sides of the shutter. A dump taken only before it is seconds stale by
    // the time `screencap` runs, so it describes a screen that may no longer be the photographed one;
    // a dump taken only after it leaves the shutter-to-dump window unverified. Two reads that agree
    // bracket the frame, which is the cheapest form of proof that the assertion describes the pixels.
    assertReadings(dumpScreen(adbPath, serial, stageDir, `shot-${ordinal}-before`), expectations, `${label} before the shutter`)
    const png = adb(adbPath, serial, ['exec-out', 'screencap', '-p'], { binary: true })
    const target = path.join(stageDir, `${ordinal}.png`)
    writeFileSync(target, png)
    assertReadings(dumpScreen(adbPath, serial, stageDir, `shot-${ordinal}-after`), expectations, `${label} after the shutter`)
    console.log(`PASS capture \u2014 ${path.basename(target)}`)
  })
  // The whole-journey 'checksum' that used to live here read the same final frame shot-06 had just
  // verified and asserted a subset of the same readings, so it added no independent evidence: one
  // screen cannot be two screens. The readings only it carried (the total run and the citation echo)
  // moved into shot-06's own `expect`, and the metric pair is asserted where it is on screen, at
  // shot-04. A check that re-reads a frame another shot already verifies is redundant; the shot that
  // owns the frame now carries the claim.
}

/**
 * Confirm which build is installed, and record what was observed.
 *
 * `dumpsys package` rules out the `.debug` variant (which carries a `.debug` applicationId suffix,
 * so the bare id already targets release) and rejects a debuggable build of any kind. That alone
 * does not identify *which* release build is installed, which `verifyArtifactIdentity` then
 * establishes by hashing the installed APK against the signed artifact.
 */
function verifyInstalledBuild(adbPath, serial, applicationId, stageDir, slotDir) {
  const dump = String(adb(adbPath, serial, ['shell', 'dumpsys', 'package', applicationId]))
  if (!dump.trim() || /Unable to find package/i.test(dump)) {
    throw new Error(
      `package ${applicationId} is not installed; install the signed release APK first ` +
      '(adb install -r app/build/outputs/apk/release/app-release.apk)'
    )
  }
  const flags = (dump.match(/pkgFlags=\[([^\]]*)\]/) || [])[1] || ''
  const versionName = (dump.match(/\bversionName=(\S+)/) || [])[1] || 'unknown'
  const versionCode = (dump.match(/\bversionCode=(\S+)/) || [])[1] || 'unknown'
  if (/DEBUGGABLE/i.test(flags)) {
    throw new Error(
      `${applicationId} is installed as a DEBUGGABLE build (pkgFlags=${flags}); a capture of a ` +
      'debug build would photograph a package that never ships. Install the release APK.'
    )
  }
  const observed = verifyArtifactIdentity(adbPath, serial, applicationId, slotDir, stageDir, {
    applicationId, versionName, versionCode, pkgFlags: flags
  })
  writeFileSync(
    path.join(stageDir, 'installed-build.json'),
    `${JSON.stringify(observed, null, 2)}\n`,
    'utf8'
  )
  console.log(`PASS installed build - ${applicationId} ${versionName} (${versionCode}), not debuggable`)
}

/**
 * Repair a publish that was killed between its two renames.
 *
 * The swap in `publishCaptures` is two operations: `raw/` moves aside to `raw.previous/`, then the
 * staged set moves into `raw/`. Between them `raw/` does not exist, and a process death there cannot
 * run the rollback, so the *next* run has to repair it. Two consequences, both load-bearing:
 *
 *  - the backup must not be deleted before checking whether `raw/` is missing, or the only surviving
 *    copy of a good capture set is destroyed by the run that was supposed to recover it;
 *  - this must run before anything recreates `raw/`, because an empty `raw/` looks exactly like a set
 *    that survived a completed publish, and the backup would be discarded as a leftover.
 *
 * Returns true when a previous run's capture set had to be restored.
 */
function sha256(bytes) {
  return createHash('sha256').update(bytes).digest('hex')
}

/** Hash every file of the given extension under `dir`, keyed by slot-relative path. */
function hashTree(dir, slotDir, extension) {
  if (!existsSync(dir)) return []
  const found = []
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      found.push(...hashTree(full, slotDir, extension))
    } else if (entry.name.endsWith(extension)) {
      found.push({
        file: path.relative(slotDir, full).split(path.sep).join('/'),
        bytes: statSync(full).size,
        sha256: sha256(readFileSync(full))
      })
    }
  }
  return found.sort((a, b) => a.file.localeCompare(b.file))
}

/**
 * Record, in a tracked file, which build the committed screenshots came from.
 *
 * The per-run evidence and the release manifest both live under ignored paths (`store/captures/`,
 * `dist/`), so they vanish with a clean checkout. What survives is the committed images plus the
 * documentation calling them captures of the release build - a claim with nothing left to check it
 * against. This writes the binding into `store/`, which is committed, and hashes both the raw captures
 * the composition was built from and the composed assets that are actually committed, so the images in
 * git can be shown to be the ones this run verified against this artifact.
 */
function writeProvenance(store, slotDir, slot, listing, observed) {
  const record = {
    slot,
    applicationId: listing.applicationId,
    versionName: observed.versionName,
    versionCode: observed.versionCode,
    gitCommit: observed.gitCommit,
    manifestPath: observed.manifestPath,
    artifactPath: observed.artifactPath,
    artifactSha256: observed.artifactSha256,
    installedSha256: observed.installedSha256,
    installedApkPath: observed.installedApkPath,
    artifactMatch: observed.artifactMatch,
    recordedAt: new Date().toISOString(),
    note: 'Raw captures live under the ignored store/captures/. Re-run scripts/media-capture.mjs to reproduce them; the composed asset hashes below are the committed evidence.',
    rawCaptures: hashTree(path.join(store, 'captures', 'raw'), slotDir, '.png'),
    composedAssets: hashTree(path.join(store, 'assets'), slotDir, '.png')
  }
  const target = path.join(store, 'provenance.json')
  writeFileSync(target, `${JSON.stringify(record, null, 2)}\n`, 'utf8')
  console.log(
    `PASS provenance - ${path.relative(ROOT, target)} binds ${record.composedAssets.length} committed ` +
    `asset(s) to commit ${String(observed.gitCommit).slice(0, 12)} (sha256 ${String(observed.artifactSha256).slice(0, 12)}…)`
  )
  return target
}

/**
 * Prove the installed package is the signed artifact, not just a non-debuggable build of the right
 * package id.
 *
 * `dumpsys package` confirms the id and that it is not debuggable, which rules out the `.debug`
 * variant but says nothing about *which* build is installed: a stale release APK from an earlier
 * commit passes both. So the check walks the whole chain and requires every link to hold:
 *
 *   HEAD -> manifest.gitCommit -> manifest sha256 -> the APK file on disk -> the installed base.apk
 *
 * Each link catches a distinct staleness. The commit link rejects a manifest built from other sources.
 * The file link rejects a manifest whose recorded hash no longer describes the artifact it names, which
 * is the case a hash-string-only comparison passes: a manifest and an installed APK can agree with each
 * other about a build that neither of them is. The installed link rejects an artifact that was rebuilt
 * since the last install, which is the staleness that produced screenshots of a build nobody shipped.
 *
 * Every failure throws. An unproven identity must not reach publication, because the committed
 * screenshots and `graphics.md` say they show the release build, and a warning leaves that claim
 * standing while the evidence for it is missing.
 */
function verifyArtifactIdentity(adbPath, serial, applicationId, slotDir, stageDir, observed) {
  const manifestPath = path.join(slotDir, 'dist', 'release-manifest.json')
  // Forward slashes so the committed record reads the same on every platform.
  const manifestLabel = path.relative(ROOT, manifestPath).split(path.sep).join('/')
  // The chain itself lives in `scripts/lib/apk-provenance.mjs` so its failure paths can be driven with
  // fakes; this function only supplies the I/O it reads.
  const manifest = existsSync(manifestPath) ? JSON.parse(readFileSync(manifestPath, 'utf8')) : null
  const head = String(run('git', ['rev-parse', 'HEAD'])).trim()
  // A commit mismatch is only fatal if the app sources moved. Committing the captures and the
  // provenance record necessarily advances HEAD past the commit the APK was built from, and treating
  // that as staleness would make the claim unverifiable the moment it was recorded. What matters is
  // whether HEAD would still build this same APK, so compare the app sources between the two commits.
  let appSourcesUnchanged = false
  if (manifest?.gitCommit && String(manifest.gitCommit) !== head) {
    appSourcesUnchanged = buildInputsUnchangedBetween(String(manifest.gitCommit), head, slotDir, ROOT)
  }
  let artifactPath = null
  let artifactHash = null
  if (manifest) {
    const entry = (manifest.artifacts || []).find((item) => String(item.path || '').endsWith('release.apk'))
    // Manifest artifact paths are slot-relative, as `prepare-release.mjs` writes them.
    if (entry?.path) {
      const resolved = path.join(slotDir, entry.path)
      if (existsSync(resolved)) {
        artifactPath = resolved
        artifactHash = sha256(readFileSync(resolved))
      }
    }
  }
  let installedHash = null
  let installedApkPath = null
  if (manifest) {
    const pmPath = String(adb(adbPath, serial, ['shell', 'pm', 'path', applicationId]))
    const apk = (pmPath.match(/package:(\S+base\.apk)/) || [])[1]
    if (apk) {
      installedApkPath = apk
      installedHash = sha256(adb(adbPath, serial, ['exec-out', 'cat', apk], { binary: true }))
    }
  }
  const record = verifyArtifactChain({
    manifestLabel, manifest, head, artifactPath, artifactHash, installedHash, installedApkPath,
    appSourcesUnchanged
  })
  console.log(
    `PASS installed build - HEAD ${record.gitCommit.slice(0, 12)} -> ${manifestLabel} -> ` +
    `${path.basename(record.artifactPath)} sha256 ${record.artifactSha256.slice(0, 12)}… -> ` +
    'installed base.apk is byte-identical'
  )
  return { ...observed, ...record }
}

function findPython() {
  const candidates = process.env.PYTHON ? [process.env.PYTHON] : (process.platform === 'win32' ? ['python', 'py'] : ['python3', 'python'])
  for (const candidate of candidates) {
    const result = spawnSync(candidate, ['--version'], { encoding: 'utf8' })
    if (!result.error && result.status === 0) return candidate
  }
  return null
}

function main() {
  let options
  try {
    options = parseArgs(process.argv.slice(2))
  } catch (error) {
    console.error(`BLOCKED: ${error.message}`)
    process.exit(2)
  }

  let slotDir
  try {
    slotDir = resolveSlot(options.slot)
  } catch (error) {
    block(error.message)
  }
  // Screenshots are release media: they are composed after approval and then committed as a
  // claim about the shipped build. Refuse before touching a device rather than after a
  // journey has run, so an unapproved revision costs a device run nothing. The check covers both
  // halves of the approval and its reason names the half that moved: the app (the build inputs
  // the capture is proven against, further down this file) and the authored store pack — this
  // run photographs `store/journey.json` and renders from `store/studio.json`, so a pack edited
  // after approval would otherwise be captured as if the owner had reviewed it.
  const approval = checkReleaseApproval(slotDir)
  if (!approval.ok) block(approval.reason)
  const store = path.join(slotDir, 'store')
  const listingPath = path.join(store, 'listing.json')
  if (!existsSync(listingPath)) block(`missing ${listingPath}; run python scripts/store-kit.py --slot ${options.slot} --init`)
  const listing = loadJson(listingPath, 'listing')
  if (!listing.applicationId) block(`${listingPath} has no applicationId`)

  const journeyPath = path.join(store, 'journey.json')
  if (!existsSync(journeyPath)) {
    mkdirSync(store, { recursive: true })
    writeFileSync(journeyPath, `${JSON.stringify(defaultJourney(), null, 2)}\n`, 'utf8')
    console.log(`PASS journey — wrote documented example ${journeyPath}`)
  }
  const journey = loadJson(journeyPath, 'journey')
  if (!Array.isArray(journey.shots) || journey.shots.length < 6) {
    block(`${journeyPath} must contain at least 6 shots`)
  }
  // Refuse unfalsifiable expectations before a device is touched, so the author is told while editing
  // the journey rather than after a device run that reported PASS for a claim nothing could contradict.
  try {
    journey.shots.slice(0, 6).forEach((shot, index) => {
      const name = `shot ${index + 1}${shot.name ? ` (${shot.name})` : ''}`
      validateExpectations(expectedReadings(shot), name)
    })
  } catch (error) {
    block(`${journeyPath} declares an unfalsifiable expectation: ${error.message}`)
  }

  const adbPath = findAdb()
  if (!adbPath) block('adb was not found on PATH or under ANDROID_HOME/platform-tools', emulatorHint())
  const deviceOutput = String(run(adbPath, ['devices']))
  const devices = deviceOutput.split(/\r?\n/).slice(1)
    .map((line) => line.trim().split(/\s+/))
    .filter((parts) => parts.length >= 2 && parts[1] === 'device')
    .map((parts) => parts[0])
  const serial = options.serial || devices[0]
  if (!serial) block("adb found no attached device in 'device' state", emulatorHint())
  if (!devices.includes(serial)) block(`requested device '${serial}' is not attached in 'device' state`)

  // A journey asserts the literal text of the nodes it photographs, and those strings come from the
  // app's resources, which Android resolves against the *device's* language - not the listing's and
  // not the journey's. Adding `values-<locale>/` to a slot therefore silently changes what a capture
  // photographs: a journey that was green while the app was English-only starts reading the device's
  // language, and every reading in every shot fails with "not in the field they name", which reads
  // like a broken journey rather than a device setting. That cost three attempts and about twenty
  // minutes the first time it happened. The device is checked before the lock is taken, so a
  // mismatch costs no device run at all.
  const declaredLocale = typeof journey.locale === 'string' && journey.locale.trim() ? journey.locale.trim() : 'en-US'
  const deviceLanguage = devicePrimaryLanguage(adb(adbPath, serial, ['shell', 'am', 'get-config']))
  const expectedLanguage = journeyLanguage(journey)
  if (deviceLanguage === null) {
    // A guard must not block on its own uncertainty: an unreadable config is a note, never a refusal.
    console.log(`NOTE device language - could not read it from 'am get-config'; the journey's readings are written in ${declaredLocale}`)
  } else if (deviceLanguage !== expectedLanguage) {
    block(
      `the device's language is '${deviceLanguage}' but the journey's readings are written in '${expectedLanguage}' - every expectation would fail as "not in the field they name"`,
      [
        `${journeyPath} declares "locale": ${JSON.stringify(declaredLocale)}, and each expectation there is the literal`,
        'text of a node, which Android resolves against the device language rather than the listing language.',
        'Set the device language to match and capture again:',
        `  adb shell cmd locale set-app-locales ${listing.applicationId} --locales ${declaredLocale}`,
        'That per-app override needs Android 13 or newer and leaves the rest of the phone alone. On Android 11 and',
        'older there is none: change the system language, capture, then change it back.',
        'If these readings are deliberately written in another language, declare that in the journey instead.'
      ]
    )
  } else {
    console.log(`PASS device language - ${deviceLanguage}, the language the journey's readings are written in`)
  }

  const rawDir = path.join(store, 'captures', 'raw')
  const stageDir = path.join(store, 'captures', 'staging')
  // The lock lives beside staging/ under the ignored captures/ tree, so it is never committed and can
  // never be published as part of a pack. It must be taken before the first mutation - before the
  // recovery below, before raw/ is recreated, before staging/ is wiped - because recovery consumes
  // another run's `raw.previous/`, and two runs sharing staging/ would publish a mixture of journeys.
  const lockPath = path.join(store, 'captures', 'capture.lock')
  let lock
  try {
    lock = acquireCaptureLock(lockPath, { slot: options.slot })
  } catch (error) {
    block(error.message)
  }
  // `block()` calls `process.exit`, which terminates without unwinding the stack, so the `finally`
  // below cannot run on those paths. The `exit` listener does, for `process.exit` and a normal return
  // alike, and `release()` is idempotent, so the two paths cannot release twice or hand away a lock
  // another run has since reclaimed.
  process.on('exit', () => lock.release())
  try {
    // Repair an interrupted publish before `mkdirSync` can create the empty `raw/` that would make the
    // recovery check read the backup as a stale leftover and delete it.
    if (recoverInterruptedPublish(rawDir)) {
      console.log('RECOVER publish - restored the capture set a previous run left mid-swap')
    }
    mkdirSync(rawDir, { recursive: true })
    let lastError = null
    for (let attempt = 1; attempt <= options.attempts; attempt += 1) {
      if (attempt > 1) console.log(`RETRY - replaying the journey (attempt ${attempt} of ${options.attempts})`)
      // Each attempt starts from an empty staging area, so a retry cannot inherit the previous
      // attempt's files and publish a mixture of the two.
      rmSync(stageDir, { recursive: true, force: true })
      mkdirSync(stageDir, { recursive: true })
      try {
        replayJourney({ adbPath, serial, journey, stageDir, slotDir, listing })
        lastError = null
        break
      } catch (error) {
        lastError = error
        console.error(`FAIL verify - ${error.message}`)
      }
    }
    if (lastError) {
      // Fail closed: nothing is published to captures/raw/ and the assets are not re-rendered, so the
      // last good pack survives a failed run untouched.
      block(`the journey did not produce a verifiable result in ${options.attempts} attempt(s): ${lastError.message}`)
    }
    const published = publishCaptures(stageDir, rawDir)
    rmSync(stageDir, { recursive: true, force: true })
    if (published.recovered) {
      console.log('RECOVER publish - a previous run died mid-swap; its capture set was restored first')
    }
    console.log(`PASS publish - ${published.published} verified capture(s) moved into ${path.relative(ROOT, rawDir)}`)

    const python = findPython()
    if (!python) block('Python was not found; set PYTHON or add python to PATH')
    // `--skip-provenance` because the record on disk during this render describes the *previous* run's
    // captures by construction: this run replaces them, and the record naming the new ones is written
    // below. store-kit answers that flag with UNVERIFIED and exit 2 rather than `STORE-KIT: OK`, so this
    // deferred state cannot be mistaken for a pass - including here, where it is the only accepted
    // non-zero status and every other one still blocks.
    const render = spawnSync(python, ['scripts/store-kit.py', '--slot', options.slot, '--skip-provenance'], {
      cwd: ROOT,
      encoding: 'utf8',
      stdio: 'inherit'
    })
    if (render.error) block(`store-kit framing failed to run: ${render.error.message}`)
    if (render.status !== 0 && render.status !== 2) {
      block(`store-kit framing failed with exit code ${render.status ?? 'unknown'}`)
    }
    const observed = loadJson(path.join(rawDir, 'installed-build.json'), 'installed build')
    writeProvenance(store, slotDir, options.slot, listing, observed)
    // Confirm the pack now describes itself: this is the pass that checks the hashes written above
    // against the assets on disk, so a render that did not produce what the record claims stops here.
    // No deferral is accepted at this point, and the record must be present, not merely tolerated.
    const confirm = spawnSync(python, ['scripts/store-kit.py', '--slot', options.slot, '--validate-only'], {
      cwd: ROOT,
      encoding: 'utf8',
      stdio: 'inherit'
    })
    if (confirm.error || confirm.status !== 0) {
      block(`the pack does not validate against its own provenance record (exit code ${confirm.status ?? 'unknown'})`)
    }
    console.log(`PASS media-capture — ${published.published} raw captures from ${serial} framed for ${options.slot}`)
  } finally {
    lock.release()
  }
}

// Run the CLI only when this file is the entry point, so `scripts/test-capture-assertions.mjs` can
// import the assertion functions and drive them against synthetic dumps without touching a device.
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main()

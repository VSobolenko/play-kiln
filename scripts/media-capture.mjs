#!/usr/bin/env node
/** Capture a six-step store journey with ADB, then delegate framing to store-kit.py. */

import { spawnSync } from 'node:child_process'
import { existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'

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

function block(reason) {
  console.error(`BLOCKED: ${reason}`)
  console.error('BLOCKED: start an emulator with:')
  console.error('  $ANDROID_HOME/emulator/emulator -list-avds')
  console.error('  $ANDROID_HOME/emulator/emulator -avd <AVD_NAME>')
  console.error('  adb wait-for-device')
  process.exit(2)
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

/** Dump the live view hierarchy, keep it beside the capture it describes, return its text nodes. */
function dumpScreen(adbPath, serial, rawDir, name) {
  const xml = String(adb(adbPath, serial, ['exec-out', 'uiautomator', 'dump', '/dev/tty']))
  writeFileSync(path.join(rawDir, `${name}.xml`), xml, 'utf8')
  return [...xml.matchAll(/\btext="([^"]*)"/g)]
    .map((match) => match[1])
    .filter((value) => value && value !== 'null')
}

function expectedReadings(source) {
  return Array.isArray(source?.expect)
    ? source.expect.filter((entry) => typeof entry === 'string' && entry.trim())
    : []
}

/**
 * Assert that a screen really shows what the shot claims to show. A screenshot is a claim about
 * the product, and this is the only thing in the pipeline that reads one back: every other check
 * proves a PNG exists at the right size, which is how six captures of IME autocorrect gibberish
 * ('0.5 Paige 100y40v4 5 to the ...') once passed as this app's entire store presence.
 */
function assertReadings(texts, expected, label) {
  if (expected.length === 0) {
    console.log(`WARN verify - ${label} declares no 'expect' entries; screen read:`)
    console.log(`  ${texts.join(' \u00b7 ')}`)
    return
  }
  const missing = expected.filter((entry) => !texts.includes(entry))
  if (missing.length > 0) {
    console.error(`  screen read: ${texts.join(' \u00b7 ')}`)
    throw new Error(
      `${label} does not show what it claims to show; ` +
      `${missing.length} of ${expected.length} expected readings are absent: ` +
      `${missing.map((entry) => JSON.stringify(entry)).join(', ')}`
    )
  }
  console.log(`PASS verify - ${label} shows all ${expected.length} expected readings`)
}

/**
 * Replay the journey from a wiped, keyboard-free state and verify every screen before it is
 * photographed. Throws on the first screen that does not show what its shot claims to show.
 */
function replayJourney({ adbPath, serial, journey, rawDir, listing }) {
  // Enforce the preconditions the journey documents but the tool used to leave to the operator.
  adb(adbPath, serial, ['shell', 'pm', 'clear', listing.applicationId])
  if (!disableImes(adbPath, serial)) {
    throw new Error('a soft keyboard is still enabled; it autocorrects typed values and rescrolls the form')
  }
  adb(adbPath, serial, ['shell', 'monkey', '-p', listing.applicationId, '-c', 'android.intent.category.LAUNCHER', '1'])
  sleep(1000)
  journey.shots.slice(0, 6).forEach((shot, index) => {
    if (!Array.isArray(shot.steps)) throw new Error(`shot ${index + 1} steps must be an array`)
    shot.steps.forEach((step) => executeStep(adbPath, serial, step))
    const ordinal = String(index + 1).padStart(2, '0')
    // Read the screen back before photographing it: the capture on its own proves nothing.
    const texts = dumpScreen(adbPath, serial, rawDir, `shot-${ordinal}-window`)
    assertReadings(texts, expectedReadings(shot), `${shot.name} (${shot.title})`)
    const png = adb(adbPath, serial, ['exec-out', 'screencap', '-p'], { binary: true })
    const target = path.join(rawDir, `${ordinal}.png`)
    writeFileSync(target, png)
    console.log(`PASS capture — ${target}`)
  })
  // Whole-journey checksum: the last screen echoes every value entered along the way, so this
  // fails if any single step was dropped or rewritten even though each tap reported success.
  assertReadings(
    dumpScreen(adbPath, serial, rawDir, 'final-window'),
    expectedReadings(journey),
    'the completed journey'
  )
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

  const adbPath = findAdb()
  if (!adbPath) block('adb was not found on PATH or under ANDROID_HOME/platform-tools')
  const deviceOutput = String(run(adbPath, ['devices']))
  const devices = deviceOutput.split(/\r?\n/).slice(1)
    .map((line) => line.trim().split(/\s+/))
    .filter((parts) => parts.length >= 2 && parts[1] === 'device')
    .map((parts) => parts[0])
  const serial = options.serial || devices[0]
  if (!serial) block("adb found no attached device in 'device' state")
  if (!devices.includes(serial)) block(`requested device '${serial}' is not attached in 'device' state`)

  const rawDir = path.join(store, 'captures', 'raw')
  mkdirSync(rawDir, { recursive: true })
  let lastError = null
  for (let attempt = 1; attempt <= options.attempts; attempt += 1) {
    if (attempt > 1) console.log(`RETRY - replaying the journey (attempt ${attempt} of ${options.attempts})`)
    try {
      replayJourney({ adbPath, serial, journey, rawDir, listing })
      lastError = null
      break
    } catch (error) {
      lastError = error
      console.error(`FAIL verify - ${error.message}`)
    }
  }
  if (lastError) {
    // Fail closed: the framed assets are only rendered after every screen has been verified, so a
    // run that cannot prove what it photographed leaves the last good pack untouched.
    block(`the journey did not produce a verifiable result in ${options.attempts} attempt(s): ${lastError.message}`)
  }

  const python = findPython()
  if (!python) block('Python was not found; set PYTHON or add python to PATH')
  const render = spawnSync(python, ['scripts/store-kit.py', '--slot', options.slot], {
    cwd: ROOT,
    encoding: 'utf8',
    stdio: 'inherit'
  })
  if (render.error || render.status !== 0) block(`store-kit framing failed with exit code ${render.status ?? 'unknown'}`)
  console.log(`PASS media-capture — 6 raw captures from ${serial} framed for ${options.slot}`)
}

main()

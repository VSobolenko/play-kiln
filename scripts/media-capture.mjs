#!/usr/bin/env node
/** Capture a six-step store journey with ADB, then delegate framing to store-kit.py. */

import { spawnSync } from 'node:child_process'
import { existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'

const ROOT = process.cwd()

function usage() {
  console.log('Usage: node scripts/media-capture.mjs --slot <slug> [--serial <device>]')
  console.log('Captures store/journey.json with ADB, saves store/captures/raw/, then frames assets.')
}

function parseArgs(argv) {
  if (argv.includes('--help') || argv.includes('-h')) {
    usage()
    process.exit(0)
  }
  const allowed = new Set(['--slot', '--serial'])
  const result = { slot: null, serial: null }
  for (let i = 0; i < argv.length; i += 2) {
    const flag = argv[i]
    const value = argv[i + 1]
    if (!allowed.has(flag) || !value || value.startsWith('--')) {
      throw new Error(`invalid arguments near ${flag || '(end)'}; run with --help`)
    }
    if (flag === '--slot') result.slot = value
    if (flag === '--serial') result.serial = value
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
    case 'back':
      adb(adbPath, serial, ['shell', 'input', 'keyevent', '4'])
      break
    default:
      throw new Error(`unsupported journey step type: ${JSON.stringify(step.type)}`)
  }
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
  try {
    adb(adbPath, serial, ['shell', 'monkey', '-p', listing.applicationId, '-c', 'android.intent.category.LAUNCHER', '1'])
    sleep(1000)
    journey.shots.slice(0, 6).forEach((shot, index) => {
      if (!Array.isArray(shot.steps)) throw new Error(`shot ${index + 1} steps must be an array`)
      shot.steps.forEach((step) => executeStep(adbPath, serial, step))
      const png = adb(adbPath, serial, ['exec-out', 'screencap', '-p'], { binary: true })
      const target = path.join(rawDir, `${String(index + 1).padStart(2, '0')}.png`)
      writeFileSync(target, png)
      console.log(`PASS capture — ${target}`)
    })
  } catch (error) {
    block(error.message)
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

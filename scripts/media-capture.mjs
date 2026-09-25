#!/usr/bin/env node
/** Capture a six-step store journey with ADB, then delegate framing to store-kit.py. */

import { spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { existsSync, mkdirSync, readFileSync, readdirSync, renameSync, rmSync, statSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'

import { publishCaptures, recoverInterruptedPublish } from './lib/capture-publish.mjs'
import { verifyArtifactChain } from './lib/apk-provenance.mjs'

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
    shot.steps.forEach((step) => executeStep(adbPath, serial, step))
    const ordinal = String(index + 1).padStart(2, '0')
    // Read the screen back before photographing it: the capture on its own proves nothing.
    const texts = dumpScreen(adbPath, serial, stageDir, `shot-${ordinal}-window`)
    assertReadings(texts, expectedReadings(shot), `${shot.name} (${shot.title})`)
    const png = adb(adbPath, serial, ['exec-out', 'screencap', '-p'], { binary: true })
    const target = path.join(stageDir, `${ordinal}.png`)
    writeFileSync(target, png)
    console.log(`PASS capture — ${path.basename(target)}`)
  })
  // Whole-journey checksum: the last screen echoes every value entered along the way, so this
  // fails if any single step was dropped or rewritten even though each tap reported success.
  assertReadings(
    dumpScreen(adbPath, serial, stageDir, 'final-window'),
    expectedReadings(journey),
    'the completed journey'
  )
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
    const diff = spawnSync('git', ['diff', '--quiet', String(manifest.gitCommit), head, '--', path.join(slotDir, 'app')], {
      cwd: ROOT, encoding: 'utf8'
    })
    appSourcesUnchanged = diff.status === 0
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
  if (!adbPath) block('adb was not found on PATH or under ANDROID_HOME/platform-tools', emulatorHint())
  const deviceOutput = String(run(adbPath, ['devices']))
  const devices = deviceOutput.split(/\r?\n/).slice(1)
    .map((line) => line.trim().split(/\s+/))
    .filter((parts) => parts.length >= 2 && parts[1] === 'device')
    .map((parts) => parts[0])
  const serial = options.serial || devices[0]
  if (!serial) block("adb found no attached device in 'device' state", emulatorHint())
  if (!devices.includes(serial)) block(`requested device '${serial}' is not attached in 'device' state`)

  const rawDir = path.join(store, 'captures', 'raw')
  const stageDir = path.join(store, 'captures', 'staging')
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
  const render = spawnSync(python, ['scripts/store-kit.py', '--slot', options.slot], {
    cwd: ROOT,
    encoding: 'utf8',
    stdio: 'inherit'
  })
  if (render.error || render.status !== 0) block(`store-kit framing failed with exit code ${render.status ?? 'unknown'}`)
  const observed = loadJson(path.join(rawDir, 'installed-build.json'), 'installed build')
  writeProvenance(store, slotDir, options.slot, listing, observed)
  console.log(`PASS media-capture — ${published.published} raw captures from ${serial} framed for ${options.slot}`)
}

main()

#!/usr/bin/env node
/**
 * Objective verification gate for PlayKiln.
 *
 *   node scripts/verify-slot.mjs
 *   node scripts/verify-slot.mjs --slot <slug>
 *   node scripts/verify-slot.mjs --base <ref>
 *   node scripts/verify-slot.mjs --build
 *   node scripts/verify-slot.mjs --strict-store
 */

import { spawnSync } from 'node:child_process'
import { existsSync, lstatSync, readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import {
  APP_ICON,
  FEATURE_GRAPHIC,
  LOCALES,
  NON_DEGENERATE,
  PHONE_SCREENSHOTS,
  TABLET_10,
  TABLET_7,
  bytesToHuman,
  pngContentStats,
  pngSize,
  readListing,
  sha256,
  validateListingText
} from './lib/play-assets.mjs'
import { applicationIdPattern } from './lib/namespaces.mjs'
import {
  aggregateSlotCapabilities,
  discoverCapabilities
} from './lib/capabilities.mjs'
import { runGradleTasks } from './lib/gradle.mjs'

const ROOT = process.cwd()
const ARGV = process.argv.slice(2)
const baseIndex = ARGV.indexOf('--base')
const BASE_REF = baseIndex !== -1 ? ARGV[baseIndex + 1] : null
const slotIndex = ARGV.indexOf('--slot')
const SLOT = slotIndex !== -1 ? ARGV[slotIndex + 1] : null
const RUN_BUILD = ARGV.includes('--build')
const STRICT_STORE = ARGV.includes('--strict-store')

if (ARGV.includes('--help') || ARGV.includes('-h')) {
  console.log(`Usage: node scripts/verify-slot.mjs [options]

  --slot <slug>     verify one slot under apps/
  --base <ref>      verify the diff against a base ref
  --strict-store    require the full store-pack contract
  --build           compile the slot (assembleDebug)
  --no-selftest     skip the factory self-test (rare)
  --help            print this message

Without --build the slot is not compiled, so the verdict is UNVERIFIED rather
than PASS: source-only evidence cannot prove the app builds.`)
  process.exit(0)
}

let failed = false
let blocked = false
const unverified = []

function line(status, message) {
  console.log(`  ${status.padEnd(11)} ${message}`)
  if (status === 'FAIL') failed = true
  if (status === 'BLOCKED') blocked = true
}

function run(command, args, cwd, { useShell = false } = {}) {
  const result = spawnSync(command, args, { cwd, encoding: 'utf8', shell: useShell, windowsHide: true })
  const output = `${result.stdout || ''}${result.stderr || ''}`.trim()
  if (result.error) return { ok: false, missing: result.error.code === 'ENOENT', out: result.error.message }
  return { ok: result.status === 0, status: result.status, out: output }
}

function gitLines(args, { trim = true } = {}) {
  const result = spawnSync('git', args, { cwd: ROOT, encoding: 'utf8', shell: false, windowsHide: true })
  if (result.status !== 0) {
    console.error(`verify-slot: git failed:\n${result.stderr || ''}`)
    process.exit(1)
  }
  const lines = result.stdout.split('\n').filter(Boolean)
  return trim ? lines.map((value) => value.trim()) : lines
}

function changedFiles() {
  if (BASE_REF) return gitLines(['diff', '--name-only', '--diff-filter=ACMRD', `${BASE_REF}...HEAD`])
  return gitLines(['status', '--porcelain', '--untracked-files=all'], { trim: false })
    .map((entry) => {
      let file = entry.slice(3)
      const arrow = file.indexOf(' -> ')
      if (arrow !== -1) file = file.slice(arrow + 4)
      return file.replace(/^"|"$/g, '').trim()
    })
    .filter(Boolean)
}

function listAppDirs() {
  const appsDir = path.join(ROOT, 'apps')
  if (!existsSync(appsDir)) return []
  return readdirSync(appsDir)
    .map((name) => path.join(appsDir, name))
    .filter((candidate) => safeIsFileType(candidate, 'directory'))
}

function resolveSlotDir(slug) {
  const matches = listAppDirs().filter((candidate) => {
    const name = path.basename(candidate)
    return name === slug || name.endsWith(`-${slug}`)
  })
  return matches.length === 1 ? matches[0] : null
}

function checkNodeSyntax(files) {
  for (const file of files) {
    const absolute = path.join(ROOT, file)
    if (!existsSync(absolute)) continue
    const result = run(process.execPath, ['--check', absolute], ROOT)
    if (result.missing) {
      line('BLOCKED', 'node --check unavailable')
      break
    }
    if (result.ok) line('PASS', `syntax — ${file}`)
    else {
      line('FAIL', `syntax — ${file}`)
      console.log(indent(result.out))
    }
  }
}

function gradleAssemble(slotDir) {
  let gradleRun
  try {
    gradleRun = runGradleTasks(slotDir, 'assembleDebug')
  } catch (error) {
    const out = error?.message || String(error)
    return { ok: false, missing: /^gradlew(?:\.bat)? not found$/.test(out), out }
  }
  const { result } = gradleRun
  const out = `${result.stdout || ''}${result.stderr || ''}`.trim()
  if (result.error) {
    return { ok: false, missing: result.error.code === 'ENOENT', out: result.error.message }
  }
  return { ok: result.status === 0, status: result.status, out }
}

function indent(value, padding = '        ') {
  return !value ? `${padding}(no output)` : value.split(/\r?\n/).map((entry) => padding + entry).join('\n')
}

function safeIsFileType(candidate, type) {
  try {
    const stats = statSync(candidate)
    return type === 'directory' ? stats.isDirectory() : stats.isFile()
  } catch {
    return false
  }
}

function walkFiles(dir, output = []) {
  if (!existsSync(dir)) return output
  for (const name of readdirSync(dir)) {
    if (name === '.git') continue
    const candidate = path.join(dir, name)
    try {
      if (lstatSync(candidate).isSymbolicLink()) continue
    } catch {
      continue
    }
    if (safeIsFileType(candidate, 'directory')) walkFiles(candidate, output)
    else if (safeIsFileType(candidate, 'file')) output.push(candidate)
  }
  return output
}

function storeLine(ok, message) {
  line(ok ? 'PASS' : STRICT_STORE ? 'FAIL' : 'WARN', message)
}

function pngDetails(file) {
  try {
    const bytes = readFileSync(file)
    const size = pngSize(file)
    if (!size || bytes.length < 26) return null
    return {
      ...size,
      bitDepth: bytes[24],
      colorType: bytes[25],
      hasTransparencyChunk: hasPngChunk(bytes, 'tRNS')
    }
  } catch {
    return null
  }
}

function hasPngChunk(bytes, chunkName) {
  let offset = 8
  while (offset + 12 <= bytes.length) {
    const length = bytes.readUInt32BE(offset)
    if (offset + 12 + length > bytes.length) return false
    if (bytes.toString('ascii', offset + 4, offset + 8) === chunkName) return true
    offset += 12 + length
  }
  return false
}

// Play's per-kind count window, the rule `play-assets.mjs` states and the gate
// must apply. `PHONE_SCREENSHOTS.min` is the reconciled value; see the comment
// above that constant for why it is 4 and not Play's weaker 2.
const SCREEN_KINDS = Object.freeze({
  phone: PHONE_SCREENSHOTS,
  'tablet-7': TABLET_7,
  'tablet-10': TABLET_10
})

/**
 * The composed-asset inventory a store pack must contain, read out of
 * `scripts/store-kit.py`.
 *
 * That file is what WRITES these assets, so its `SCREEN_TARGETS` (exact count and
 * size per kind) and `SCREENSHOT_LIMITS` (Play's per-kind window) are
 * authoritative. Hand-copying those numbers into JavaScript is how the gate
 * drifted before: it globbed only `phone-*`, so all eight tablet assets went
 * unverified while `store/graphics.md` and `store-kit.py` both claimed they
 * existed. Reading the Python source keeps one source of truth with no build
 * step, the same way `scripts/visual-qa.py` reads `play-assets.mjs`.
 *
 * Throws when the parse is incomplete, so a renamed constant fails the gate
 * loudly instead of silently verifying nothing.
 */
function loadStoreKitInventory() {
  const source = readFileSync(path.join(ROOT, 'scripts', 'store-kit.py'), 'utf8')

  const targetBlock = source.match(/SCREEN_TARGETS\s*=\s*\{([\s\S]*?)\n\}/)
  if (!targetBlock) throw new Error('SCREEN_TARGETS not found in scripts/store-kit.py')
  const targets = new Map()
  for (const match of targetBlock[1].matchAll(
    /"([a-z0-9-]+)"\s*:\s*\(\s*(\d+)\s*,\s*\(\s*(\d+)\s*,\s*(\d+)\s*\)\s*\)/g
  )) {
    targets.set(match[1], { count: Number(match[2]), width: Number(match[3]), height: Number(match[4]) })
  }

  const limits = new Map()
  for (const match of source.matchAll(
    /"([a-z0-9-]+)"\s*:\s*\{\s*"min_short":\s*(\d+),\s*"max_long":\s*(\d+),\s*"max_ratio":\s*(None|\d+)\s*\}/g
  )) {
    limits.set(match[1], {
      minShort: Number(match[2]),
      maxLong: Number(match[3]),
      maxRatio: match[4] === 'None' ? null : Number(match[4])
    })
  }

  const maxBytesMatch = source.match(/^MAX_PNG_BYTES\s*=\s*([\d\s*]+)$/m)
  if (!maxBytesMatch) throw new Error('MAX_PNG_BYTES not found in scripts/store-kit.py')
  const maxBytes = maxBytesMatch[1].split('*').reduce((total, part) => total * Number(part.trim()), 1)

  const inventory = []
  for (const [kind, target] of targets) {
    const limit = limits.get(kind)
    if (!limit) throw new Error(`scripts/store-kit.py has no SCREENSHOT_LIMITS entry for ${kind}`)
    if (!SCREEN_KINDS[kind]) throw new Error(`verify-slot has no Play count rule for screenshot kind ${kind}`)
    inventory.push({ kind, ...target, ...limit, maxBytes })
  }
  for (const kind of Object.keys(SCREEN_KINDS)) {
    if (!targets.has(kind)) throw new Error(`scripts/store-kit.py SCREEN_TARGETS has no ${kind} entry`)
  }
  return inventory
}

/** The Play rule a screenshot size violates, or `null` when it satisfies every limit. */
function screenshotRuleProblem(limit, { width, height }) {
  const shortSide = Math.min(width, height)
  const longSide = Math.max(width, height)
  if (shortSide < limit.minShort) return `short side ${shortSide}px is below the ${limit.minShort}px minimum`
  if (longSide > limit.maxLong) return `long side ${longSide}px exceeds the ${limit.maxLong}px maximum`
  if (limit.maxRatio !== null && longSide > shortSide * limit.maxRatio) {
    return `long side ${longSide}px is more than ${limit.maxRatio}x the short side ${shortSide}px`
  }
  return null
}

/**
 * A PNG that is one flat colour passes every size/mode/format check and is still
 * not a screenshot. Measure the pixels and report the numbers so a human can see
 * the margin between a real frame and the threshold.
 *
 * `screenshot` adds the colour-count floor: marketing frames carry hundreds of
 * quantised colours, but a legitimate flat app icon can be two-tone, so only
 * screenshots are gated on colour count.
 */
function pngContentProblem(file, { screenshot = false } = {}) {
  const stats = pngContentStats(file)
  if (!stats) return { note: 'content unreadable', problem: 'PNG content could not be decoded' }
  const note = `luminance sd=${stats.luminanceStdDev.toFixed(2)}, colours=${stats.distinctColours}`
  if (stats.luminanceStdDev < NON_DEGENERATE.minLuminanceStdDev) {
    return {
      note,
      problem: `near-uniform image: luminance sd ${stats.luminanceStdDev.toFixed(2)} < ${NON_DEGENERATE.minLuminanceStdDev}`
    }
  }
  if (screenshot && stats.distinctColours < NON_DEGENERATE.minScreenshotColours) {
    return {
      note,
      problem: `near-uniform image: ${stats.distinctColours} colours < ${NON_DEGENERATE.minScreenshotColours}`
    }
  }
  return { note, problem: null }
}

/**
 * Verify every composed asset the pack ships for one locale: presence, exact
 * size, PNG mode, the Play size rule, non-degenerate content, and that no two
 * assets are byte-identical (a copy-paste between kinds is otherwise invisible).
 *
 * `--strict-store` turns each failed `storeLine` into a FAIL, which is what makes
 * a corrupt or missing tablet asset block the gate instead of passing silently.
 */
function verifyComposedAssets(locale, localeDir, inventory) {
  const names = readdirSync(localeDir)
  const seenHashes = new Map()
  const counts = []
  let allOk = true

  for (const target of inventory) {
    const play = SCREEN_KINDS[target.kind]
    const pattern = new RegExp(`^${target.kind.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}-(\\d+)\\.png$`, 'i')
    const present = new Map()
    for (const name of names) {
      const match = name.match(pattern)
      if (match) present.set(Number(match[1]), name)
    }

    let invalid = 0
    for (const [index, name] of [...present].sort((left, right) => left[0] - right[0])) {
      const file = path.join(localeDir, name)
      const bytes = statSync(file).size
      const dimensions = pngSize(file)
      const png = pngDetails(file)
      const formatValid = png?.bitDepth === 8 && png?.colorType === 2 && !png.hasTransparencyChunk
      const sizeValid =
        dimensions?.width === target.width && dimensions?.height === target.height
      const ruleProblem = dimensions ? screenshotRuleProblem(target, dimensions) : 'unreadable PNG header'
      const content = pngContentProblem(file, { screenshot: true })
      const hash = sha256(file)
      const duplicate = seenHashes.get(hash)
      if (!duplicate) seenHashes.set(hash, `${locale}/${name}`)

      const problems = []
      if (!sizeValid) problems.push(`expected ${target.width}x${target.height} from store-kit.py SCREEN_TARGETS`)
      if (!formatValid) problems.push('PNG must be 24-bit RGB without alpha')
      if (ruleProblem) problems.push(ruleProblem)
      if (content.problem) problems.push(content.problem)
      if (duplicate) problems.push(`byte-identical to ${duplicate}`)
      if (bytes > target.maxBytes) problems.push(`over store-kit.py MAX_PNG_BYTES (${bytesToHuman(target.maxBytes)})`)

      const ok = Boolean(dimensions) && sizeValid && formatValid && !ruleProblem && !content.problem && !duplicate && bytes <= target.maxBytes
      if (!ok) invalid += 1
      storeLine(
        ok,
        `${locale}/${name} — ${dimensions ? `${dimensions.width}x${dimensions.height}, ` : ''}${bytesToHuman(bytes)}, ${content.note}${ok ? '' : ` (${problems.join('; ')})`}`
      )
      if (!ok) allOk = false
    }

    let missing = 0
    for (let index = 1; index <= target.count; index += 1) {
      if (!present.has(index)) {
        missing += 1
        storeLine(
          false,
          `${locale}/${target.kind}-${String(index).padStart(2, '0')}.png missing — store-kit.py SCREEN_TARGETS declares ${target.count}`
        )
      }
    }

    const withinPlay = present.size >= play.min && present.size <= play.max
    const countOk = invalid === 0 && missing === 0 && withinPlay
    storeLine(
      countOk,
      `${locale} ${target.kind}: ${present.size} present, ${target.count} required (store-kit.py SCREEN_TARGETS), ${present.size - invalid} valid, Play allows ${play.min}-${play.max}`
    )
    if (!countOk) allOk = false
    counts.push(`${target.kind.replace('-', '')}=${present.size}`)
  }

  storeLine(allOk, `${locale} composed assets: ${counts.join(' ')}`)
}

function verifyStorePack(slotDir) {
  console.log('\n#### store pack')
  const storeDir = path.join(slotDir, 'store')
  const listingMarkdown = path.join(storeDir, 'listing.md')
  const listingJson = path.join(storeDir, 'listing.json')
  storeLine(existsSync(listingMarkdown), 'store/listing.md')
  storeLine(existsSync(listingJson), 'store/listing.json')

  let listing = null
  if (existsSync(listingJson)) {
    try {
      listing = readListing(slotDir)
      const localeEntries = Object.entries(listing.locales)
      if (localeEntries.length === 0) storeLine(false, 'listing.json contains no locale listings')
      storeLine(LOCALES.includes(listing.defaultLocale), `listing default locale is supported: ${listing.defaultLocale}`)
      storeLine(Boolean(listing.locales[listing.defaultLocale]), `listing contains its default locale: ${listing.defaultLocale}`)
      for (const [locale, text] of localeEntries) {
        const problems = validateListingText(text)
        storeLine(problems.length === 0, `listing ${locale}${problems.length ? ` — ${problems.join('; ')}` : ''}`)
      }
    } catch (error) {
      storeLine(false, `listing.json could not be parsed — ${error.message}`)
    }
  }

  for (const file of ['privacy.md', 'data-safety.md', 'review-notes.md', 'checklist.md']) {
    storeLine(existsSync(path.join(storeDir, file)), `store/${file}`)
  }

  const assetsDir = path.join(storeDir, 'assets')
  const iconFile = path.join(assetsDir, 'icon-512.png')
  if (!existsSync(iconFile)) {
    storeLine(false, 'store/assets/icon-512.png')
  } else {
    const details = pngDetails(iconFile)
    const bytes = statSync(iconFile).size
    const content = pngContentProblem(iconFile)
    const valid =
      details?.width === APP_ICON.width &&
      details?.height === APP_ICON.height &&
      details.bitDepth === 8 &&
      details.colorType === 6 &&
      !content.problem &&
      bytes <= APP_ICON.maxBytes
    storeLine(
      valid,
      `icon-512.png — ${details ? `${details.width}x${details.height}, ${bytesToHuman(bytes)}` : 'invalid PNG'}, ${content.note}${valid ? '' : ' (requires 512x512 32-bit RGBA PNG, <= 1024 KB, non-uniform content)'}`
    )
  }

  const featureFile = path.join(assetsDir, 'feature-graphic-1024x500.png')
  if (!existsSync(featureFile)) {
    storeLine(false, 'store/assets/feature-graphic-1024x500.png')
  } else {
    const details = pngDetails(featureFile)
    const content = pngContentProblem(featureFile)
    const valid =
      details?.width === FEATURE_GRAPHIC.width &&
      details?.height === FEATURE_GRAPHIC.height &&
      details.bitDepth === 8 &&
      details.colorType === 2 &&
      !details.hasTransparencyChunk &&
      !content.problem
    storeLine(
      valid,
      `feature-graphic-1024x500.png — ${details ? `${details.width}x${details.height}` : 'invalid PNG'}, ${content.note}${valid ? '' : ' (requires 1024x500 24-bit RGB PNG without alpha, non-uniform content)'}`
    )
  }

  let inventory = null
  try {
    inventory = loadStoreKitInventory()
  } catch (error) {
    storeLine(false, `composed-asset inventory could not be read — ${error.message}`)
  }

  if (inventory) {
    const requestedDefaultLocale = listing?.defaultLocale || LOCALES[0]
    const defaultLocale = LOCALES.includes(requestedDefaultLocale) ? requestedDefaultLocale : LOCALES[0]
    // The default locale is mandatory; the additional locale is verified whenever
    // it is present, because store-kit.py composes every screenshot kind for
    // every locale it renders.
    const locales = [defaultLocale, ...LOCALES.filter((locale) => locale !== defaultLocale)]
    for (const locale of locales) {
      const localeDir = path.join(assetsDir, locale)
      if (!existsSync(localeDir)) {
        if (locale === defaultLocale) storeLine(false, `store/assets/${locale} directory`)
        continue
      }
      verifyComposedAssets(locale, localeDir, inventory)
    }
  }

  verifyProvenance(slotDir)
}

/**
 * Locate the Python interpreter the same way `scripts/media-capture.mjs` does, so
 * the whole factory honours one resolution order: `PYTHON` first, then the
 * platform's usual names (`py` is the Windows launcher that exists even when no
 * `python.exe` is on PATH). Hardcoding `python` would silently skip the check on
 * machines where only `py` resolves.
 */
function findPython() {
  const candidates = process.env.PYTHON
    ? [process.env.PYTHON]
    : process.platform === 'win32'
      ? ['python', 'py']
      : ['python3', 'python']
  for (const candidate of candidates) {
    const result = spawnSync(candidate, ['--version'], { encoding: 'utf8', windowsHide: true })
    if (!result.error && result.status === 0) return candidate
  }
  return null
}

/**
 * Verify the store pack against `store/provenance.json`, the sha256 record of every
 * committed asset, by delegating to `scripts/store-kit.py --validate-only` — the
 * single source of truth for that record. The per-asset checks above read only the
 * PNG IHDR header (dimensions, mode, size), so an asset whose bytes were edited can
 * keep its dimensions and still pass; this is how a composed pack once cleared the
 * gate while `store-kit.py` correctly failed it. Re-implementing the sha256 sweep
 * in JavaScript would let the two drift, so the authoritative implementation is
 * invoked instead.
 *
 * Composed slots are exactly those `store-kit.py` treats as provenance-required
 * (`store/studio.json`), plus any pack that already ships a record. For those, an
 * absent record is a FAIL under `--strict-store`, not a skip: an unattested pack
 * must not clear the release gate. A pack that claims no captures is left alone.
 *
 * Every failure mode — non-zero exit, a missing `store-kit.py`, a missing Python,
 * or a spawn error — routes through `storeLine`, which fails closed under
 * `--strict-store` and only warns on the non-strict path.
 */
/**
 * True when a path is committed, even if it is missing from the working tree.
 *
 * `store/studio.json` and `store/provenance.json` are both tracked; keying the provenance
 * requirement on their mere presence let a deletion remove the check instead of failing it.
 */
function gitTracks(file) {
  const relative = path.relative(ROOT, file).split(path.sep).join('/')
  const result = spawnSync('git', ['ls-files', '--error-unmatch', relative], {
    cwd: ROOT, encoding: 'utf8', windowsHide: true
  })
  return !result.error && result.status === 0
}

function verifyProvenance(slotDir) {
  const storeDir = path.join(slotDir, 'store')
  const studioFile = path.join(storeDir, 'studio.json')
  const provenanceFile = path.join(storeDir, 'provenance.json')
  // Composed is a property of the slot, not of which files survive on disk. Both files are
  // committed, so deleting them is a working-tree change; treating their absence as "this slot is
  // not composed" let `rm store/studio.json store/provenance.json` skip the whole check. Git
  // remembers what the pack is supposed to contain, so a tracked-but-absent file still counts.
  const declared =
    existsSync(studioFile) || existsSync(provenanceFile) ||
    gitTracks(studioFile) || gitTracks(provenanceFile)
  if (!declared) return
  if (!existsSync(studioFile) && gitTracks(studioFile)) {
    storeLine(false, 'store/studio.json is committed but missing from the working tree')
  }

  const script = path.join(ROOT, 'scripts', 'store-kit.py')
  if (!existsSync(script)) {
    storeLine(false, `provenance unverified — ${path.relative(ROOT, script)} is missing`)
    return
  }

  const python = findPython()
  if (!python) {
    storeLine(false, 'provenance unverified — Python not found; set PYTHON or add python to PATH')
    return
  }

  // Pass the slot directory's own name, which `store-kit.py` resolves beneath
  // apps/; `--slot` is its documented entry point.
  const slug = path.basename(slotDir)
  const result = spawnSync(python, [script, '--slot', slug, '--validate-only'], {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  })
  const out = `${result.stdout || ''}${result.stderr || ''}`.trim()
  if (result.error) {
    storeLine(false, `provenance unverified — ${result.error.message}`)
    return
  }
  if (result.status !== 0) {
    storeLine(false, `store-kit.py --validate-only failed (exit ${result.status})`)
    const failures = out.split(/\r?\n/).filter((entry) => entry.trim().startsWith('FAIL'))
    for (const failure of failures) console.log(`            ${failure.trim()}`)
    if (failures.length === 0) console.log(indent(out))
    return
  }
  const pass = out.split(/\r?\n/).map((entry) => entry.trim()).find((entry) => entry.startsWith('PASS provenance.json'))
  storeLine(true, pass || `store-kit.py --validate-only passed for ${slug}`)
}

/**
 * The money gate. `apps/2026-09-25-pipe-sizing` passed every other check in this file while its
 * `capabilities.json` was `{"schemaVersion":1,"enabled":[]}` - no billing, no ads, no way to earn -
 * which is why this exists. `scripts/check-economics.mjs` decides whether the slot's plan names a
 * revenue mechanism its capabilities can actually deliver.
 *
 * Enforcement turns on a declared plan, and a git-tracked-but-absent plan still counts, so deleting
 * `economics.json` in the working tree downgrades nothing. A slot with no plan at all predates the
 * gate: it is reported UNVERIFIED rather than FAIL because the pre-gate slots are documented
 * throwaway proofs, but it is pushed into `unverified` so the run cannot end in PASS. A slot that
 * has not stated how it earns is not release evidence.
 */
function verifyEconomics(slotDir) {
  const planFile = path.join(slotDir, 'economics.json')
  const declared = existsSync(planFile) || gitTracks(planFile)
  const slug = path.basename(slotDir)

  if (!declared) {
    line(
      'UNVERIFIED',
      `economics — ${slug} declares no economics.json; it predates the money gate and states no revenue mechanism`
    )
    unverified.push(`economics:${slug}`)
    return
  }
  if (!existsSync(planFile) && gitTracks(planFile)) {
    line('FAIL', 'economics.json is committed but missing from the working tree')
    return
  }

  const script = path.join(ROOT, 'scripts', 'check-economics.mjs')
  if (!existsSync(script)) {
    line('FAIL', 'economics unverified — scripts/check-economics.mjs is missing')
    return
  }

  const result = spawnSync(process.execPath, [script, '--slot', slug], {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  })
  const out = `${result.stdout || ''}${result.stderr || ''}`.trim()
  if (result.error) {
    line('FAIL', `economics unverified — ${result.error.message}`)
    return
  }
  if (result.status !== 0) {
    line('FAIL', 'economics plan does not name a revenue mechanism this slot can deliver')
    const problems = out
      .split(/\r?\n/)
      .map((entry) => entry.trim())
      .filter((entry) => entry.startsWith('BLOCKED'))
    for (const problem of problems) console.log(`            ${problem}`)
    if (problems.length === 0) console.log(indent(out))
    return
  }
  const pass = out
    .split(/\r?\n/)
    .map((entry) => entry.trim())
    .find((entry) => entry.startsWith('PASS economics schema'))
  line('PASS', pass || `economics plan validated for ${slug}`)
}

/**
 * The promise gate. Recon states why the app can win; implementation loses part of that list,
 * and every other check in this file measures form — the APK builds, the pack is complete, the
 * money plan parses — so none of them could see a promised feature that quietly did not ship.
 * `scripts/check-differentiators.mjs` decides whether each promise is delivered with an artifact
 * that still exists, or has been given up in writing with the owner's words when it was core.
 *
 * Enforcement turns on a declaration rather than on the file being present: it is looked for on
 * disk *and* in git, so deleting `differentiators.json` in the working tree downgrades nothing. A
 * slot with no contract at all predates the gate — reported UNVERIFIED rather than FAIL, because
 * the pre-gate slots are documented throwaway proofs, and pushed into `unverified` so the run
 * cannot end in PASS. A slot whose reason list is unresolved is not release evidence.
 *
 * Unlike economics, this wrapper does not pass `--allow-deferred`. That flag exists so a builder
 * can read progress; the completion sensor must not accept an unfinished promise list, which is
 * the entire defect the contract was written to expose.
 */
function verifyDifferentiators(slotDir) {
  const contractFile = path.join(slotDir, 'differentiators.json')
  const declared = existsSync(contractFile) || gitTracks(contractFile)
  const slug = path.basename(slotDir)

  if (!declared) {
    line(
      'UNVERIFIED',
      `differentiators — ${slug} declares no differentiators.json; it predates the promise gate and states no reason the app should win`
    )
    unverified.push(`differentiators:${slug}`)
    return
  }
  if (!existsSync(contractFile) && gitTracks(contractFile)) {
    line('FAIL', 'differentiators.json is committed but missing from the working tree')
    return
  }

  const script = path.join(ROOT, 'scripts', 'check-differentiators.mjs')
  if (!existsSync(script)) {
    line('FAIL', 'differentiators unverified — scripts/check-differentiators.mjs is missing')
    return
  }

  const result = spawnSync(process.execPath, [script, '--slot', slug], {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  })
  const out = `${result.stdout || ''}${result.stderr || ''}`.trim()
  if (result.error) {
    line('FAIL', `differentiators unverified — ${result.error.message}`)
    return
  }

  if (result.status !== 0) {
    line('FAIL', 'a promised reason this app should win is neither delivered nor accounted for')
    for (const entry of out.split(/\r?\n/).map((row) => row.trim())) {
      if (/^(BLOCKED|INFO)/.test(entry)) console.log(`            ${entry}`)
    }
    return
  }

  const summary = out
    .split(/\r?\n/)
    .map((entry) => entry.trim())
    .find((entry) => entry.startsWith('PASS contract schema'))
  line('PASS', summary || `every promise accounted for in ${slug}`)
  // A narrowed or dropped promise is a scope cut the owner is entitled to see, even when the
  // gate passes. The checker reports them as INFO rows; surfacing them here is the difference
  // between a recorded decision and a silent one.
  for (const entry of out.split(/\r?\n/).map((row) => row.trim())) {
    if (entry.startsWith('INFO')) console.log(`            ${entry}`)
  }
}

/**
 * Every operation the listing claims has a control somewhere.
 *
 * Five screens were restyled in one pass and one of them dropped the add/edit controls for notes and
 * parent contacts while keeping the read-only list. Nothing else here could see it: the restyle
 * compiles, the domain tests exercise the edit logic directly so they stay green, and every other
 * check in this file reads form. The signal is a ViewModel operation no screen calls, cross-checked
 * against the promises in `store/listing.json`, so a listing that sells a feature the build cannot
 * reach is a failure rather than a footnote. `scripts/check-ui-coverage.mjs` holds the table and the
 * reasoning; this function is only the wiring.
 */
function verifyUiCoverage(slotDir) {
  const slug = path.basename(slotDir)
  const script = path.join(ROOT, 'scripts', 'check-ui-coverage.mjs')
  if (!existsSync(script)) {
    line('FAIL', 'UI coverage unverified — scripts/check-ui-coverage.mjs is missing')
    return
  }
  const result = spawnSync(process.execPath, [script, '--slot', slug], {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  })
  const out = `${result.stdout || ''}${result.stderr || ''}`.trim()
  if (result.error) {
    line('FAIL', `UI coverage unverified — ${result.error.message}`)
    return
  }
  if (result.status === 0) {
    line('PASS', out.split(/\r?\n/).find((entry) => entry.startsWith('PASS')) || 'UI coverage')
    return
  }
  line('FAIL', 'a feature the store listing claims has no control in any screen')
  for (const problem of out.split(/\r?\n/).map((entry) => entry.trim())) {
    if (problem.startsWith('FAIL') || problem.startsWith('NOTE') || problem.startsWith('BLOCKED')) {
      console.log(`            ${problem}`)
    }
  }
}

/**
 * A declared app language must have resources behind it.
 *
 * `res/xml/locales_config.xml` drives Android 13's per-app language picker, so a locale listed
 * there with no `res/values-<lang>/` offers the user a language that changes nothing. The check
 * is `scripts/check-locales.mjs`, and until this wiring nothing invoked it: `apps/2026-09-25-pipe-sizing`
 * declares `ru` in its picker with no `res/values-ru/` directory and no gate had noticed. It is
 * wired into the per-slot run rather than into `--strict-store`, because a missing translation is a
 * phase-1 defect the *owner* used to find by hand on his own device - the review is the wrong place
 * to discover that a language the app ships is still English.
 */
function verifyLocales(slotDir) {
  const slug = path.basename(slotDir)
  const config = path.join(slotDir, 'app', 'src', 'main', 'res', 'xml', 'locales_config.xml')
  if (!existsSync(config)) {
    line('SKIP', 'locales - no res/xml/locales_config.xml, so no per-app language picker')
    return
  }
  const script = path.join(ROOT, 'scripts', 'check-locales.mjs')
  if (!existsSync(script)) {
    line('FAIL', 'locale coverage unverified — scripts/check-locales.mjs is missing')
    return
  }
  const result = spawnSync(process.execPath, [script, '--slot', slug], {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  })
  const out = `${result.stdout || ''}${result.stderr || ''}`.trim()
  if (result.error) {
    line('FAIL', `locale coverage unverified — ${result.error.message}`)
    return
  }
  if (result.status === 0) {
    const summary = out
      .split(/\r?\n/)
      .map((entry) => entry.trim())
      .find((entry) => entry.startsWith('PASS'))
    line('PASS', summary || 'locale coverage')
    return
  }
  line('FAIL', 'a declared app language has no resources behind it')
  for (const problem of out.split(/\r?\n/).map((entry) => entry.trim())) {
    if (/^(FAIL|BLOCKED)/.test(problem)) console.log(`            ${problem}`)
  }
}

/**
 * The launcher mark must survive the adaptive-icon masks.
 *
 * `scripts/check-adaptive-icon.py` samples the mark against the 72dp launcher mask and the 66dp
 * safe zone. It also existed with nothing invoking it, and nothing else in this file can see the
 * defect it looks for: the store icon is checked as a 512x512 PNG of a *different* layer, so a mark
 * that a circular launcher crops still measures fine.
 *
 * This wrapper deliberately does not name the foreground file. It did once: it looked for
 * `res/drawable/ic_launcher_foreground.xml` and reported SKIP when that exact name was absent, so a
 * slot referencing a differently named foreground was reported as a slot with no adaptive icon, and
 * the gate silently stopped measuring instead of failing. The launcher XML is the entry point the
 * checker itself follows - it reads the launcher XML under a `mipmap-anydpi*` resource directory,
 * resolves the `foreground` reference it declares
 * and refuses zero or ambiguous matches - so this only decides whether an adaptive icon exists at
 * all, and the checker owns everything after that.
 */
function verifyAdaptiveIcon(slotDir) {
  const slug = path.basename(slotDir)
  const resDir = path.join(slotDir, 'app', 'src', 'main', 'res')
  // Any XML under a `mipmap-anydpi*` directory is a candidate adaptive-icon definition. Naming the
  // file here (`ic_launcher.xml`) is what made this skip instead of fail: a slot whose launcher XML
  // is named differently was reported as a slot with no adaptive icon, so the gate quietly stopped
  // measuring exactly the case it exists for. An app at minSdk 26 must ship an adaptive icon, so
  // absence is a failure, not a note - and the checker owns everything after this point, resolving
  // the `foreground` reference it finds and refusing zero or ambiguous matches.
  const launcherXml = existsSync(resDir)
    ? readdirSync(resDir, { withFileTypes: true })
        .filter((entry) => entry.isDirectory() && entry.name.startsWith('mipmap-anydpi'))
        .flatMap((entry) =>
          readdirSync(path.join(resDir, entry.name))
            .filter((name) => name.endsWith('.xml'))
            .map((name) => `${entry.name}/${name}`)
        )
    : []
  if (launcherXml.length === 0) {
    line('FAIL', 'adaptive icon - minSdk 26 requires one, but res/ has no mipmap-anydpi*/ launcher XML')
    return
  }
  const script = path.join(ROOT, 'scripts', 'check-adaptive-icon.py')
  if (!existsSync(script)) {
    line('FAIL', 'adaptive icon unverified — scripts/check-adaptive-icon.py is missing')
    return
  }
  const python = findPython()
  if (!python) {
    line('FAIL', 'adaptive icon unverified — Python not found; set PYTHON or add python to PATH')
    return
  }
  // The checker resolves the slot itself now and prints the path it measured, so this delegates
  // with the slug rather than reaching into the checker's module globals. The previous version
  // injected a `FOREGROUND` constant through `runpy` because the checker had no slot argument; that
  // workaround is gone with the argument, and two mechanisms for one rule would drift apart.
  const result = spawnSync(python, [script, '--slot', slug], {
    cwd: ROOT,
    encoding: 'utf8',
    windowsHide: true
  })
  const out = `${result.stdout || ''}${result.stderr || ''}`.trim()
  if (result.error) {
    line('FAIL', `adaptive icon unverified — ${result.error.message}`)
    return
  }
  if (result.status === 0) {
    const summary = out
      .split(/\r?\n/)
      .map((entry) => entry.trim())
      .find((entry) => entry.startsWith('PASS'))
    line('PASS', summary || 'adaptive icon fits the launcher masks')
    return
  }
  // The checker fails for two different reasons - an unresolvable foreground and a mark that does
  // not fit - so the summary is the checker's own first FAIL/BLOCKED line rather than a sentence
  // written here. A hardcoded "does not fit" would have told the reader the mark is too large when
  // the real problem was that the launcher XML references something that cannot be found.
  const problems = out
    .split(/\r?\n/)
    .map((entry) => entry.trim())
    .filter((entry) => /^(FAIL|BLOCKED)/.test(entry))
  line('FAIL', problems[0] || 'adaptive icon unverified - the checker reported no reason')
  for (const problem of problems.slice(1)) console.log(`            ${problem}`)
}

function validateBackup(slotDir, manifestText) {
  const allowBackup = manifestText.match(/android:allowBackup\s*=\s*["']([^"']+)["']/)
  if (allowBackup) line('PASS', `android:allowBackup=${allowBackup[1]}`)
  else line('FAIL', 'missing android:allowBackup in AndroidManifest')

  const reference = manifestText.match(/android:dataExtractionRules\s*=\s*["']@xml\/([^"']+)["']/)
  if (!reference) {
    line('FAIL', 'missing or invalid android:dataExtractionRules in AndroidManifest')
    return
  }
  line('PASS', `dataExtractionRules declared: @xml/${reference[1]}`)
  if (reference[1] !== 'data_extraction_rules') {
    line('FAIL', 'android:dataExtractionRules must reference @xml/data_extraction_rules')
  }
  const xmlFile = path.join(slotDir, 'app', 'src', 'main', 'res', 'xml', `${reference[1]}.xml`)
  if (!existsSync(xmlFile)) {
    line('FAIL', `referenced backup rules missing: ${path.relative(slotDir, xmlFile)}`)
    return
  }
  line('PASS', `${path.relative(slotDir, xmlFile)} present`)
  const xml = readFileSync(xmlFile, 'utf8').replace(/<!--[\s\S]*?-->/g, '')
  for (const section of ['cloud-backup', 'device-transfer']) {
    const match = xml.match(new RegExp(`<${section}\\b[^>]*>([\\s\\S]*?)<\\/${section}>`))
    if (!match) line('FAIL', `data extraction rules missing <${section}>`)
    else if (!/<(?:include|exclude)\b/.test(match[1])) line('FAIL', `<${section}> contains no include/exclude rules`)
    else line('PASS', `<${section}> rules present`)
  }
}

function checkDangerousPermissions(slotDir, manifestText) {
  const dangerous = [
    'CAMERA',
    'RECORD_AUDIO',
    'ACCESS_FINE_LOCATION',
    'ACCESS_COARSE_LOCATION',
    'READ_CONTACTS',
    'READ_SMS',
    'READ_CALL_LOG',
    'MANAGE_EXTERNAL_STORAGE',
    'QUERY_ALL_PACKAGES',
    'POST_NOTIFICATIONS'
  ]
  const declared = dangerous.filter((permission) =>
    new RegExp(`android\\.permission\\.${permission}(?:["']|\\s)`).test(manifestText)
  )
  if (declared.length === 0) {
    line('PASS', 'no gated dangerous permissions declared')
    return
  }

  const disclosureFile = path.join(slotDir, 'store', 'data-safety.md')
  const disclosure = existsSync(disclosureFile) ? readFileSync(disclosureFile, 'utf8').toUpperCase() : ''
  for (const permission of declared) {
    if (disclosure.includes(permission)) line('PASS', `${permission} disclosed in store/data-safety.md`)
    else line('FAIL', `${permission} is not mentioned in store/data-safety.md`)
  }
}

function mergedManifestFor(slotDir, variant = null) {
  const root = path.join(slotDir, 'app', 'build', 'intermediates', 'merged_manifests')
  const manifests = walkFiles(root).filter((file) => {
    if (path.basename(file) !== 'AndroidManifest.xml') return false
    if (!variant) return true
    return file.split(path.sep).some((segment) => segment.toLowerCase() === variant.toLowerCase())
  })
  manifests.sort((left, right) => statSync(right).mtimeMs - statSync(left).mtimeMs)
  return manifests[0] ?? null
}

function manifestPermissions(manifestText) {
  return new Set(
    [...manifestText.matchAll(/<uses-permission(?:-sdk-23)?\b[^>]*\bandroid:name\s*=\s*["']([^"']+)["']/gi)]
      .map((match) => match[1])
  )
}

function verifyCapabilityCompliance(slotDir) {
  console.log('\n#### capability compliance')
  let aggregate
  try {
    aggregate = aggregateSlotCapabilities({ rootDir: ROOT, slotDir })
    line('PASS', `capability registry: ${aggregate.enabled.length ? aggregate.enabled.join(', ') : 'none enabled'}`)
  } catch (error) {
    line('FAIL', error.message)
    return
  }

  const mergedManifest = mergedManifestFor(slotDir, RUN_BUILD ? 'debug' : null)
  if (!mergedManifest) {
    line('FAIL', 'merged AndroidManifest.xml is missing; run the slot build before strict verification')
  } else {
    const requested = manifestPermissions(readFileSync(mergedManifest, 'utf8'))
    const declared = new Set(aggregate.permissions.map((permission) => permission.name))
    let permissionsMatch = true
    for (const permission of declared) {
      if (!requested.has(permission)) {
        line('FAIL', `capability permission missing from merged manifest: ${permission}`)
        permissionsMatch = false
      }
    }

    const allCapabilities = discoverCapabilities(ROOT)
    const relatedPermissions = new Set(
      ['ads', 'billing'].flatMap((id) =>
        (allCapabilities.get(id)?.permissions ?? []).map((permission) => permission.name)
      )
    )
    for (const permission of requested) {
      if (relatedPermissions.has(permission) && !declared.has(permission)) {
        line('FAIL', `ads/billing permission has no enabled capability: ${permission}`)
        permissionsMatch = false
      }
    }
    if (permissionsMatch) line('PASS', 'capability permissions match merged manifest')
  }

  const listingFile = path.join(slotDir, 'store', 'listing.json')
  let listing = null
  if (existsSync(listingFile)) {
    try {
      listing = JSON.parse(readFileSync(listingFile, 'utf8'))
    } catch (error) {
      line('FAIL', `store/listing.json could not be parsed for capability compliance: ${error.message}`)
    }
  }
  if (aggregate.adsPresent) {
    if (listing?.ads?.containsAds === true) {
      line('PASS', 'store ads declaration matches capabilities: ads present')
    } else {
      line('FAIL', 'enabled capabilities contain ads but store/listing.json claims no ads')
    }
  } else {
    if (listing?.ads?.containsAds === true) {
      line('FAIL', 'store/listing.json claims ads but no enabled capability declares ads')
    } else {
      line('PASS', 'enabled capabilities declare no ads')
    }
  }

  const dataSafetyFile = path.join(slotDir, 'store', 'data-safety.md')
  const dataSafety = existsSync(dataSafetyFile) ? readFileSync(dataSafetyFile, 'utf8') : ''
  const collectedRows = new Set()
  for (const lineText of dataSafety.split(/\r?\n/)) {
    if (!lineText.trim().startsWith('|')) continue
    const cells = lineText
      .split('|')
      .slice(1, -1)
      .map((cell) => cell.replace(/[`*_]/g, '').trim())
    if (cells.length >= 2 && cells[1].toLocaleLowerCase('en-US') === 'yes') {
      collectedRows.add(cells[0].toLocaleLowerCase('en-US'))
    }
  }
  let dataTypesMatch = true
  const dataTypes = [...new Set(aggregate.dataCollected.map((item) => item.type))].sort()
  for (const dataType of dataTypes) {
    if (!collectedRows.has(dataType.toLocaleLowerCase('en-US'))) {
      // The message names the shape the parser needs, because the parser reads a markdown table with
      // exact cells and a hand-written row that merely *describes* the type reads as missing. The
      // store pack keeps hand-written safety answers by default (store-kit's write_preserving), so a
      // confusing failure here is the common case rather than an exotic one.
      line(
        'FAIL',
        `capability data type missing from store/data-safety.md: ${dataType}` +
          ` (needs a table row "| ${dataType} | Yes | <shared> | <purpose> |" — the Collected cell must read exactly "Yes")`
      )
      dataTypesMatch = false
    }
  }
  if (dataTypesMatch) line('PASS', 'capability data types disclosed in store/data-safety.md')

  if (aggregate.requiresPurchaseAuthority) {
    if (!aggregate.purchaseAuthority) {
      line('FAIL', 'SERVER_COST consumable requires billing.purchaseAuthority documentation')
    } else {
      const reviewNotesFile = path.join(slotDir, 'store', 'review-notes.md')
      const reviewNotes = existsSync(reviewNotesFile) ? readFileSync(reviewNotesFile, 'utf8') : ''
      if (reviewNotes.includes(aggregate.purchaseAuthority)) {
        line('PASS', 'SERVER_COST purchase authority documented')
      } else {
        line('FAIL', 'purchase authority missing from store/review-notes.md')
      }
    }
  } else {
    line('PASS', 'no SERVER_COST purchase authority required')
  }
}

function checkReleaseHardening(slotDir, gradleText) {
  const proguardFile = path.join(slotDir, 'app', 'proguard-rules.pro')
  const proguard = existsSync(proguardFile) ? readFileSync(proguardFile, 'utf8') : ''
  for (const directive of ['-dontoptimize', '-dontobfuscate']) {
    if (new RegExp(`^\\s*${directive}\\b`, 'm').test(proguard)) line('FAIL', `app/proguard-rules.pro contains ${directive}`)
    else line('PASS', `app/proguard-rules.pro does not contain ${directive}`)
  }
  const uncommentedGradle = stripKotlinComments(gradleText)
  if (/\brelease\s*\{[\s\S]*?\bisMinifyEnabled\s*=\s*false\b/.test(uncommentedGradle)) {
    line('FAIL', 'release build type sets isMinifyEnabled = false')
  } else {
    line('PASS', 'release build does not disable minification')
  }
  if (
    /\borg\.jetbrains\.kotlin\.android\b/.test(uncommentedGradle) ||
    /alias\s*\(\s*libs\.plugins\.kotlin\.android\s*\)/.test(uncommentedGradle)
  ) {
    line('FAIL', 'app/build.gradle.kts applies org.jetbrains.kotlin.android; AGP 9 uses built-in Kotlin')
  } else {
    line('PASS', 'AGP 9 built-in Kotlin plugin configuration')
  }
}

function stripKotlinComments(text) {
  return text.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '')
}

function checkUnresolvedTokens(slotDir) {
  const matches = []
  for (const file of walkFiles(slotDir)) {
    const bytes = readFileSync(file)
    if (bytes.includes(0)) continue
    if (/__[A-Z_]+__/.test(bytes.toString('utf8'))) matches.push(path.relative(slotDir, file))
  }
  if (matches.length === 0) line('PASS', 'no unresolved __TOKEN__ placeholders')
  else {
    line('FAIL', `unresolved template tokens in ${matches.length} file(s)`)
    console.log(indent(matches.slice(0, 20).join('\n')))
  }
}

function checkSecrets(slotDir) {
  const ignoreFile = path.join(slotDir, '.gitignore')
  const patterns = existsSync(ignoreFile) ? readFileSync(ignoreFile, 'utf8').split(/\r?\n/) : []
  const sensitive = walkFiles(slotDir).filter((file) => {
    const name = path.basename(file).toLowerCase()
    return (
      name.endsWith('.jks') ||
      name.endsWith('.keystore') ||
      ['keystore.properties', '.env', 'google-services.json', 'play-service-account.json'].includes(name)
    )
  })
  if (sensitive.length === 0) {
    line('PASS', 'no in-slot secret files present')
    return
  }
  for (const file of sensitive) {
    const relative = path.relative(slotDir, file).split(path.sep).join('/')
    if (isGitIgnored(relative, patterns)) line('PASS', `secret path ignored: ${relative}`)
    else line('FAIL', `secret path is not ignored by slot .gitignore: ${relative}`)
  }
}

function isGitIgnored(relativePath, patterns) {
  let ignored = false
  for (const raw of patterns) {
    const trimmed = raw.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const negated = trimmed.startsWith('!')
    const pattern = (negated ? trimmed.slice(1) : trimmed).replaceAll('\\', '/')
    if (matchesIgnorePattern(relativePath, pattern)) ignored = !negated
  }
  return ignored
}

function matchesIgnorePattern(relativePath, rawPattern) {
  const anchored = rawPattern.startsWith('/')
  const directory = rawPattern.endsWith('/')
  let pattern = rawPattern.replace(/^\//, '').replace(/\/$/, '')
  if (!pattern) return false
  const globstarDirectory = '\u0001'
  const globstar = '\u0000'
  pattern = pattern.replaceAll('**/', globstarDirectory).replaceAll('**', globstar)
  const escaped = pattern.replace(/[.+^${}()|[\]\\]/g, '\\$&').replaceAll('*', '[^/]*').replaceAll('?', '[^/]')
  const glob = escaped.replaceAll(globstarDirectory, '(?:.*/)?').replaceAll(globstar, '.*')
  const prefix = anchored || rawPattern.includes('/') ? '^' : '(?:^|/)'
  const suffix = directory ? '(?:/.*)?$' : '$'
  return new RegExp(prefix + glob + suffix).test(relativePath)
}

const tracked = SLOT
  ? (() => {
      const dir = resolveSlotDir(SLOT)
      return dir ? [`apps/${path.basename(dir)}/`] : []
    })()
  : [...new Set(changedFiles())]

console.log('## PlayKiln verification\n')
console.log(
  `Scope: ${SLOT ? `slot ${SLOT}` : BASE_REF ? `vs ${BASE_REF}` : 'uncommitted'} — ${tracked.length} path(s)${STRICT_STORE ? ' — strict store' : ''}`
)

if (!SLOT && tracked.length === 0) {
  console.log('\nNothing changed. Nothing to verify.')
  process.exit(1)
}

const harnessJs = tracked.filter(
  (file) =>
    (file.startsWith('scripts/') || file.startsWith('.cursor/hooks/') || file.startsWith('.cursor/mcp/')) &&
    /\.(?:js|mjs|cjs)$/.test(file)
)
if (harnessJs.length > 0) {
  console.log(`\n### harness scripts (${harnessJs.length})`)
  checkNodeSyntax(harnessJs)
}

/**
 * Factory self-test: proves the scripts still honour their contract by running the
 * real `new-slot.mjs` against a throwaway scaffold sandbox. Added after an
 * unfiltered `cpSync` shipped 145 MB of build output into every new slot and no
 * gate noticed. Skip with `--no-selftest`.
 */
if (!ARGV.includes('--no-selftest')) {
  console.log('\n### factory self-test')
  const selftestPath = path.join(ROOT, 'scripts', 'selftest.mjs')
  if (!existsSync(selftestPath)) {
    line('FAIL', 'scripts/selftest.mjs is missing')
  } else {
    const selftest = spawnSync(process.execPath, [selftestPath], {
      cwd: ROOT,
      encoding: 'utf8',
      windowsHide: true
    })
    if (selftest.status === 0) {
      const passes = (selftest.stdout || '').split(/\r?\n/).filter((l) => l.trim().startsWith('PASS')).length
      line('PASS', `factory scripts honour their contract (${passes} checks)`)
    } else {
      line('FAIL', 'factory self-test failed — run `node scripts/selftest.mjs` for detail')
      const failures = (selftest.stdout || '')
        .split(/\r?\n/)
        .filter((entry) => entry.trim().startsWith('FAIL'))
        .slice(0, 6)
      for (const failure of failures) console.log(`         ${failure.trim()}`)
    }
  }
}

const docs = tracked.filter((file) => file.startsWith('openspec/') || file.startsWith('docs/') || file.endsWith('.md'))
if (docs.length > 0) {
  console.log(`\n### docs/openspec (${docs.length})`)
  line('UNVERIFIED', 'markdown/spec — read by hand')
  unverified.push('docs/openspec')
}

const slotDirs = new Set()
for (const file of tracked) {
  const match = file.match(/^apps\/([^/]+)\//)
  if (match) slotDirs.add(match[1])
}
if (SLOT) {
  const dir = resolveSlotDir(SLOT)
  if (dir) slotDirs.add(path.basename(dir))
  else line('FAIL', `slot directory not found or ambiguous under apps/: ${SLOT}`)
}

for (const name of slotDirs) {
  const dir = path.join(ROOT, 'apps', name)
  console.log(`\n### apps/${name}`)
  if (!existsSync(dir)) {
    line('FAIL', 'missing directory')
    continue
  }

  const manifest = path.join(dir, 'app', 'src', 'main', 'AndroidManifest.xml')
  const gradle = path.join(dir, 'app', 'build.gradle.kts')
  if (!existsSync(manifest) || !existsSync(gradle)) {
    line('FAIL', 'not a Compose slot (missing app module files)')
    continue
  }

  const gradleText = readFileSync(gradle, 'utf8')
  const applicationId = gradleText.match(/\bapplicationId\s*=\s*["']([^"']+)["']/)?.[1]
  if (!applicationId || !applicationIdPattern.test(applicationId)) {
    line('FAIL', `applicationId must match ${applicationIdPattern} (found ${applicationId || '?'})`)
  } else {
    line('PASS', `applicationId ${applicationId}`)
  }

  const targetSdk = Number(gradleText.match(/\btargetSdk\s*=\s*(\d+)/)?.[1] || 0)
  const compileSdk = Number(gradleText.match(/\bcompileSdk\s*=\s*(\d+)/)?.[1] || 0)
  if (targetSdk < 36 || compileSdk < 36) {
    line('FAIL', `targetSdk and compileSdk must be >= 36 (found target=${targetSdk || '?'}, compile=${compileSdk || '?'})`)
  } else {
    line('PASS', `targetSdk=${targetSdk} compileSdk=${compileSdk}`)
  }
  if (compileSdk < 37) line('WARN', `compileSdk=${compileSdk || '?'}; Compose 1.12+ needs compileSdk 37`)

  const manifestText = readFileSync(manifest, 'utf8')
  validateBackup(dir, manifestText)
  if (/android:usesCleartextTraffic\s*=\s*["']true["']/i.test(manifestText)) {
    line('FAIL', 'AndroidManifest sets android:usesCleartextTraffic="true"')
  } else {
    line('PASS', 'cleartext traffic is not explicitly enabled')
  }
  checkDangerousPermissions(dir, manifestText)
  checkReleaseHardening(dir, gradleText)
  checkUnresolvedTokens(dir)
  checkSecrets(dir)
  verifyEconomics(dir)
  verifyDifferentiators(dir)
  verifyUiCoverage(dir)
  verifyLocales(dir)
  verifyAdaptiveIcon(dir)
  verifyStorePack(dir)

  const kotlinChanged = tracked.filter((file) => file.startsWith(`apps/${name}/`) && file.endsWith('.kt'))
  if (kotlinChanged.length > 0) {
    line('UNVERIFIED', `kotlin style (${kotlinChanged.length} file(s)) — read diff by hand`)
    unverified.push(name)
  }

  if (RUN_BUILD) {
    const result = gradleAssemble(dir)
    if (result.missing) {
      line('BLOCKED', 'Gradle wrapper / SDK not available')
      console.log(indent(result.out))
    } else if (result.ok) {
      line('PASS', 'assembleDebug')
    } else {
      line('FAIL', 'assembleDebug')
      console.log(indent(result.out.split(/\r?\n/).slice(-40).join('\n')))
    }
  } else {
    line('SKIP', 'assembleDebug - pass --build to compile')
    // Skipping the build means nothing proved the slot compiles, so the run cannot
    // end in PASS. Uncompilable code must never look like release evidence.
    unverified.push('build')
  }

  if (STRICT_STORE) verifyCapabilityCompliance(dir)
}

console.log('\n---')
if (unverified.length > 0) console.log(`UNVERIFIED: ${[...new Set(unverified)].join(', ')}`)
if (failed) {
  console.log('\nVERDICT: FAIL')
  process.exit(1)
}
if (blocked) {
  console.log('\nVERDICT: BLOCKED')
  process.exit(1)
}
if (unverified.length > 0) {
  console.log('\nVERDICT: UNVERIFIED')
  process.exit(2)
}
console.log('\nVERDICT: PASS — available checks green.')

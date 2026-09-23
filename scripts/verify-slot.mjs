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
  PHONE_SCREENSHOTS,
  bytesToHuman,
  isValidScreenshot,
  jpegSize,
  pngSize,
  readListing,
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
    const valid =
      details?.width === APP_ICON.width &&
      details?.height === APP_ICON.height &&
      details.bitDepth === 8 &&
      details.colorType === 6 &&
      bytes <= APP_ICON.maxBytes
    storeLine(
      valid,
      `icon-512.png — ${details ? `${details.width}x${details.height}, ${bytesToHuman(bytes)}` : 'invalid PNG'}${valid ? '' : ' (requires 512x512 32-bit RGBA PNG, <= 1024 KB)'}`
    )
  }

  const featureFile = path.join(assetsDir, 'feature-graphic-1024x500.png')
  if (!existsSync(featureFile)) {
    storeLine(false, 'store/assets/feature-graphic-1024x500.png')
  } else {
    const details = pngDetails(featureFile)
    const valid =
      details?.width === FEATURE_GRAPHIC.width &&
      details?.height === FEATURE_GRAPHIC.height &&
      details.bitDepth === 8 &&
      details.colorType === 2 &&
      !details.hasTransparencyChunk
    storeLine(
      valid,
      `feature-graphic-1024x500.png — ${details ? `${details.width}x${details.height}` : 'invalid PNG'}${valid ? '' : ' (requires 1024x500 24-bit RGB PNG without alpha)'}`
    )
  }

  const requestedDefaultLocale = listing?.defaultLocale || LOCALES[0]
  const defaultLocale = LOCALES.includes(requestedDefaultLocale) ? requestedDefaultLocale : LOCALES[0]
  const localeDir = path.join(assetsDir, defaultLocale)
  const screenshotFiles = existsSync(localeDir)
    ? readdirSync(localeDir)
        .filter((name) => /^phone-.*\.(?:png|jpe?g)$/i.test(name))
        .sort()
    : []
  let validScreenshots = 0
  for (const name of screenshotFiles) {
    const file = path.join(localeDir, name)
    const extension = path.extname(name).toLowerCase()
    const bytes = statSync(file).size
    const dimensions = extension === '.png' ? pngSize(file) : jpegSize(file)
    const png = extension === '.png' ? pngDetails(file) : null
    const formatValid = extension !== '.png' || (png?.bitDepth === 8 && png?.colorType === 2 && !png.hasTransparencyChunk)
    const valid =
      Boolean(dimensions) &&
      formatValid &&
      bytes <= PHONE_SCREENSHOTS.maxBytes &&
      isValidScreenshot(dimensions.width, dimensions.height)
    if (valid) validScreenshots += 1
    storeLine(
      valid,
      `${defaultLocale}/${name} — ${dimensions ? `${dimensions.width}x${dimensions.height}, ${bytesToHuman(bytes)}` : 'invalid image'}${formatValid ? '' : ', PNG must be 24-bit RGB without alpha'}`
    )
  }
  storeLine(
    validScreenshots >= PHONE_SCREENSHOTS.largeFormat.minCount && validScreenshots <= PHONE_SCREENSHOTS.max,
    `${defaultLocale} valid phone screenshots: ${validScreenshots} (required 4-${PHONE_SCREENSHOTS.max})`
  )
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
      line('FAIL', `capability data type missing from store/data-safety.md: ${dataType}`)
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

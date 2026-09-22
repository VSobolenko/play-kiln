#!/usr/bin/env node
/**
 * Validate and upload a PlayKiln release through Google Play Developer API v3.
 *
 * Credential contract: PLAY_SERVICE_ACCOUNT_JSON is an absolute or relative
 * path to a local service-account JSON file. Never commit that file.
 *
 * Important: this API cannot create a brand-new Play application. A human must
 * create the package, upload its first AAB, and complete all legal declarations
 * once in Play Console before this script can update it.
 */

import { createSign } from 'node:crypto'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'

const ROOT = process.cwd()
const API = 'https://androidpublisher.googleapis.com/androidpublisher/v3'
const UPLOAD_API = 'https://androidpublisher.googleapis.com/upload/androidpublisher/v3'
const SCOPE = 'https://www.googleapis.com/auth/androidpublisher'
const MAX_IMAGE_BYTES = 8 * 1024 * 1024
const TARGETS = {
  phoneScreenshots: { prefix: 'phone', count: 6, width: 1080, height: 1920 },
  sevenInchScreenshots: { prefix: 'tablet-7', count: 4, width: 1200, height: 1920 },
  tenInchScreenshots: { prefix: 'tablet-10', count: 4, width: 1600, height: 2560 }
}

class BlockedError extends Error {}

function usage() {
  console.log('Usage: node scripts/play-upload.mjs --slot <slug> [--track internal] [--dry-run]')
  console.log('Requires PLAY_SERVICE_ACCOUNT_JSON and one release AAB under apps/<slot>/dist/.')
}

function parseArgs(argv) {
  if (argv.includes('--help') || argv.includes('-h')) {
    usage()
    process.exit(0)
  }
  const result = { slot: null, track: 'internal', dryRun: false }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--dry-run') {
      result.dryRun = true
    } else if (arg === '--slot' || arg === '--track') {
      const value = argv[++i]
      if (!value || value.startsWith('--')) throw new BlockedError(`${arg} requires a value`)
      if (arg === '--slot') result.slot = value
      else result.track = value
    } else {
      throw new BlockedError(`unknown argument '${arg}'; run with --help`)
    }
  }
  if (!result.slot) throw new BlockedError('--slot <slug> is required')
  if (!/^[A-Za-z0-9._-]+$/.test(result.track)) throw new BlockedError(`invalid track name '${result.track}'`)
  return result
}

function resolveSlot(slug) {
  const apps = path.join(ROOT, 'apps')
  if (!existsSync(apps)) throw new BlockedError(`apps directory does not exist: ${apps}`)
  const matches = readdirSync(apps)
    .map((name) => path.join(apps, name))
    .filter((candidate) => statSync(candidate).isDirectory())
    .filter((candidate) => path.basename(candidate) === slug || path.basename(candidate).endsWith(`-${slug}`))
    .sort()
  if (matches.length === 0) throw new BlockedError(`slot '${slug}' was not found under apps/`)
  if (matches.length > 1) throw new BlockedError(`slot '${slug}' is ambiguous: ${matches.map(path.basename).join(', ')}`)
  return matches[0]
}

function readJson(file, description) {
  if (!existsSync(file)) throw new BlockedError(`missing ${description}: ${file}`)
  try {
    return JSON.parse(readFileSync(file, 'utf8'))
  } catch (error) {
    throw new BlockedError(`invalid ${description} ${file}: ${error.message}`)
  }
}

function listFilesRecursively(directory) {
  if (!existsSync(directory)) return []
  const result = []
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const absolute = path.join(directory, entry.name)
    if (entry.isDirectory()) result.push(...listFilesRecursively(absolute))
    else if (entry.isFile()) result.push(absolute)
  }
  return result
}

function findAab(slotDir) {
  const files = listFilesRecursively(path.join(slotDir, 'dist')).filter((file) => file.toLowerCase().endsWith('.aab')).sort()
  if (files.length === 0) throw new BlockedError(`no AAB found under ${path.join(slotDir, 'dist')}`)
  if (files.length > 1) throw new BlockedError(`multiple AAB files found under dist/; leave exactly one release artifact: ${files.join(', ')}`)
  if (statSync(files[0]).size === 0) throw new BlockedError(`AAB is empty: ${files[0]}`)
  return files[0]
}

function parsePng(file) {
  if (!existsSync(file)) throw new BlockedError(`missing image asset: ${file}`)
  const bytes = readFileSync(file)
  if (bytes.length > MAX_IMAGE_BYTES) throw new BlockedError(`${file} is ${bytes.length} bytes; maximum is ${MAX_IMAGE_BYTES}`)
  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])
  if (bytes.length < 33 || !bytes.subarray(0, 8).equals(signature) || bytes.toString('ascii', 12, 16) !== 'IHDR') {
    throw new BlockedError(`${file} is not a valid PNG with an IHDR header`)
  }
  return {
    file,
    bytes: bytes.length,
    width: bytes.readUInt32BE(16),
    height: bytes.readUInt32BE(20),
    bitDepth: bytes[24],
    colorType: bytes[25]
  }
}

function requirePng(file, width, height, colorType, maximum = MAX_IMAGE_BYTES) {
  const png = parsePng(file)
  if (png.bytes > maximum) throw new BlockedError(`${file} is ${png.bytes} bytes; maximum is ${maximum}`)
  if (png.width !== width || png.height !== height) {
    throw new BlockedError(`${file} is ${png.width}x${png.height}; expected ${width}x${height}`)
  }
  if (png.bitDepth !== 8 || png.colorType !== colorType) {
    const expected = colorType === 6 ? '8-bit RGBA' : '8-bit RGB without alpha'
    throw new BlockedError(`${file} has PNG bitDepth=${png.bitDepth} colorType=${png.colorType}; expected ${expected}`)
  }
  return png
}

function validateListing(listing) {
  if (listing.schemaVersion !== 1) throw new BlockedError('listing.json schemaVersion must be 1')
  if (!listing.applicationId || typeof listing.applicationId !== 'string') throw new BlockedError('listing.json applicationId is missing')
  if (!listing.locales || typeof listing.locales !== 'object' || Array.isArray(listing.locales)) {
    throw new BlockedError('listing.json locales must be an object')
  }
  const locales = Object.keys(listing.locales)
  if (locales.length === 0) throw new BlockedError('listing.json contains no locales')
  for (const locale of locales) {
    const entry = listing.locales[locale]
    for (const [field, limit] of [['title', 30], ['shortDescription', 80], ['fullDescription', 4000]]) {
      if (typeof entry?.[field] !== 'string' || entry[field].trim() === '') {
        throw new BlockedError(`listing.locales.${locale}.${field} must be a non-empty string`)
      }
      if (entry[field].length > limit) {
        throw new BlockedError(`listing.locales.${locale}.${field} is ${entry[field].length} chars; Play limit is ${limit}`)
      }
    }
  }
  return locales
}

function validateStore(slotDir, listing) {
  const store = path.join(slotDir, 'store')
  for (const document of ['listing.md', 'privacy.md', 'data-safety.md', 'review-notes.md', 'graphics.md', 'checklist.md']) {
    const file = path.join(store, document)
    if (!existsSync(file) || statSync(file).size === 0) throw new BlockedError(`missing or empty store document: ${file}`)
  }
  const locales = validateListing(listing)
  const uploadImages = []
  for (const locale of locales) {
    const base = path.join(store, 'assets', locale)
    const icon = path.join(base, 'icon-512.png')
    const feature = path.join(base, 'feature-graphic-1024x500.png')
    requirePng(icon, 512, 512, 6, 1024 * 1024)
    requirePng(feature, 1024, 500, 2)
    uploadImages.push({ locale, imageType: 'icon', file: icon })
    uploadImages.push({ locale, imageType: 'featureGraphic', file: feature })
    for (const [imageType, target] of Object.entries(TARGETS)) {
      for (let index = 1; index <= target.count; index += 1) {
        const file = path.join(base, `${target.prefix}-${String(index).padStart(2, '0')}.png`)
        requirePng(file, target.width, target.height, 2)
        uploadImages.push({ locale, imageType, file })
      }
    }
  }
  requirePng(path.join(store, 'assets', 'icon-512.png'), 512, 512, 6, 1024 * 1024)
  requirePng(path.join(store, 'assets', 'feature-graphic-1024x500.png'), 1024, 500, 2)
  return { store, locales, uploadImages }
}

function readVersionCode(slotDir) {
  const gradle = path.join(slotDir, 'app', 'build.gradle.kts')
  if (!existsSync(gradle)) throw new BlockedError(`missing ${gradle}`)
  const match = readFileSync(gradle, 'utf8').match(/\bversionCode\s*=\s*(\d+)/)
  if (!match) throw new BlockedError(`${gradle} has no numeric versionCode`)
  return match[1]
}

function credentials() {
  const credentialPath = process.env.PLAY_SERVICE_ACCOUNT_JSON
  if (!credentialPath) throw new BlockedError('PLAY_SERVICE_ACCOUNT_JSON is not set')
  const absolute = path.resolve(ROOT, credentialPath)
  const value = readJson(absolute, 'service-account JSON')
  if (!value.client_email || !value.private_key) {
    throw new BlockedError(`${absolute} must contain client_email and private_key`)
  }
  return { ...value, file: absolute }
}

function base64url(value) {
  return Buffer.from(value).toString('base64url')
}

async function accessToken(serviceAccount) {
  const now = Math.floor(Date.now() / 1000)
  const header = base64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }))
  const claims = base64url(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: SCOPE,
    aud: serviceAccount.token_uri || 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600
  }))
  const unsigned = `${header}.${claims}`
  let signature
  try {
    const signer = createSign('RSA-SHA256')
    signer.update(unsigned)
    signer.end()
    signature = signer.sign(serviceAccount.private_key).toString('base64url')
  } catch (error) {
    throw new BlockedError(`cannot sign JWT with service-account private key: ${error.message}`)
  }
  const response = await fetch(serviceAccount.token_uri || 'https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: `${unsigned}.${signature}`
    })
  })
  const body = await response.text()
  if (!response.ok) throw new Error(`OAuth token request failed (${response.status}): ${body}`)
  const parsed = JSON.parse(body)
  if (!parsed.access_token) throw new Error('OAuth response did not contain access_token')
  return parsed.access_token
}

async function apiRequest(token, method, url, body = null, contentType = 'application/json') {
  const headers = { authorization: `Bearer ${token}` }
  if (body !== null) headers['content-type'] = contentType
  const response = await fetch(url, {
    method,
    headers,
    body: body === null ? undefined : (contentType === 'application/json' ? JSON.stringify(body) : body)
  })
  const text = await response.text()
  if (!response.ok) throw new Error(`${method} ${url} failed (${response.status}): ${text}`)
  if (!text) return {}
  try {
    return JSON.parse(text)
  } catch {
    return { text }
  }
}

function encoded(value) {
  return encodeURIComponent(value)
}

function dryRun(plan) {
  const app = encoded(plan.listing.applicationId)
  const editRoot = `${API}/applications/${app}/edits/{editId}`
  const uploadRoot = `${UPLOAD_API}/applications/${app}/edits/{editId}`
  console.log(`DRY-RUN POST ${API}/applications/${app}/edits`)
  console.log(`DRY-RUN POST ${uploadRoot}/bundles?uploadType=media <- ${plan.aab}`)
  for (const locale of plan.locales) {
    console.log(`DRY-RUN PATCH ${editRoot}/listings/${encoded(locale)} <- listing.json locales.${locale}`)
    for (const imageType of ['icon', 'featureGraphic', ...Object.keys(TARGETS)]) {
      console.log(`DRY-RUN DELETE ${editRoot}/listings/${encoded(locale)}/${imageType}`)
      for (const image of plan.uploadImages.filter((item) => item.locale === locale && item.imageType === imageType)) {
        console.log(`DRY-RUN POST ${uploadRoot}/listings/${encoded(locale)}/${imageType}?uploadType=media <- ${image.file}`)
      }
    }
  }
  console.log(`DRY-RUN PUT ${editRoot}/tracks/${encoded(plan.track)} <- versionCode ${plan.versionCode}`)
  console.log(`DRY-RUN POST ${editRoot}:commit`)
  console.log(`PASS dry-run — ${plan.locales.length} locale(s), ${plan.uploadImages.length} image upload(s), no network requests`)
}

async function upload(plan, serviceAccount) {
  const token = await accessToken(serviceAccount)
  const app = encoded(plan.listing.applicationId)
  const created = await apiRequest(token, 'POST', `${API}/applications/${app}/edits`, {})
  if (!created.id) throw new Error('create edit response did not contain an edit id')
  const edit = encoded(created.id)
  const editRoot = `${API}/applications/${app}/edits/${edit}`
  const uploadRoot = `${UPLOAD_API}/applications/${app}/edits/${edit}`

  const bundle = await apiRequest(
    token,
    'POST',
    `${uploadRoot}/bundles?uploadType=media`,
    readFileSync(plan.aab),
    'application/octet-stream'
  )
  const versionCode = String(bundle.versionCode || plan.versionCode)
  console.log(`PASS bundle — uploaded versionCode ${versionCode}`)

  for (const locale of plan.locales) {
    const entry = plan.listing.locales[locale]
    const fields = new URLSearchParams({ updateMask: 'title,shortDescription,fullDescription' })
    await apiRequest(token, 'PATCH', `${editRoot}/listings/${encoded(locale)}?${fields}`, {
      language: locale,
      title: entry.title,
      shortDescription: entry.shortDescription,
      fullDescription: entry.fullDescription
    })
    console.log(`PASS listing — ${locale}`)
    for (const imageType of ['icon', 'featureGraphic', ...Object.keys(TARGETS)]) {
      const imageRoot = `${editRoot}/listings/${encoded(locale)}/${imageType}`
      await apiRequest(token, 'DELETE', imageRoot)
      const selected = plan.uploadImages.filter((item) => item.locale === locale && item.imageType === imageType)
      for (const image of selected) {
        await apiRequest(
          token,
          'POST',
          `${uploadRoot}/listings/${encoded(locale)}/${imageType}?uploadType=media`,
          readFileSync(image.file),
          'image/png'
        )
      }
      console.log(`PASS images — ${locale}/${imageType} (${selected.length})`)
    }
  }

  await apiRequest(token, 'PUT', `${editRoot}/tracks/${encoded(plan.track)}`, {
    track: plan.track,
    releases: [{
      name: `PlayKiln ${versionCode}`,
      versionCodes: [versionCode],
      status: 'completed'
    }]
  })
  console.log(`PASS track — ${plan.track} versionCode ${versionCode}`)
  await apiRequest(token, 'POST', `${editRoot}:commit`, {})
  console.log(`PASS commit — edit ${created.id}`)
}

async function main() {
  const options = parseArgs(process.argv.slice(2))
  const slotDir = resolveSlot(options.slot)
  const listing = readJson(path.join(slotDir, 'store', 'listing.json'), 'listing.json')
  const validated = validateStore(slotDir, listing)
  const aab = findAab(slotDir)
  const versionCode = readVersionCode(slotDir)
  const serviceAccount = credentials()
  const plan = {
    ...validated,
    listing,
    aab,
    versionCode,
    track: options.track
  }
  console.log(`PASS preflight — package=${listing.applicationId} AAB=${aab} credentials=${serviceAccount.file}`)
  if (options.dryRun) {
    dryRun(plan)
    return
  }
  await upload(plan, serviceAccount)
}

try {
  await main()
} catch (error) {
  if (error instanceof BlockedError) {
    console.error(`BLOCKED: ${error.message}`)
    process.exit(2)
  }
  console.error(`FAIL: ${error.stack || error.message}`)
  process.exit(1)
}

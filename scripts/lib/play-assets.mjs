import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'
import path from 'node:path'

// Google Play app icon rule: 512x512 32-bit PNG with alpha, at most 1024 KB.
export const APP_ICON = Object.freeze({ width: 512, height: 512, maxBytes: 1024 * 1024, format: 'png', alpha: true })

// Google Play feature graphic rule: exactly 1024x500, JPEG or 24-bit PNG without alpha.
export const FEATURE_GRAPHIC = Object.freeze({ width: 1024, height: 500, formats: Object.freeze(['jpeg', 'png']), alpha: false })

// Google Play phone screenshot rule: 2-8 images, 320-3840px sides, long side no more than twice the short side.
export const PHONE_SCREENSHOTS = Object.freeze({
  min: 2,
  max: 8,
  target: 6,
  minSide: 320,
  maxSide: 3840,
  maxAspectRatio: 2,
  maxBytes: 8 * 1024 * 1024,
  recommended: Object.freeze({ width: 1080, height: 1920 }),
  largeFormat: Object.freeze({ minCount: 4, minShortSide: 1080 })
})

// Google Play 7-inch tablet screenshot rule: 4-8 images, short edge at least 1080px, long edge at most 7680px.
export const TABLET_7 = Object.freeze({ min: 4, max: 8, minShortSide: 1080, maxLongSide: 7680, maxBytes: 8 * 1024 * 1024 })

// Google Play 10-inch tablet screenshot rule: 4-8 images, short edge at least 1080px, long edge at most 7680px.
export const TABLET_10 = Object.freeze({ min: 4, max: 8, minShortSide: 1080, maxLongSide: 7680, maxBytes: 8 * 1024 * 1024 })

// Google Play main-store-listing character limits: title 30, short description 80, full description 4000.
export const LISTING_LIMITS = Object.freeze({ title: 30, shortDescription: 80, fullDescription: 4000 })

// PlayKiln store-pack locale rule: en-US is the first/default locale and ru-RU is the additional locale.
export const LOCALES = Object.freeze(['en-US', 'ru-RU'])

// Google Play Developer API imageTypes keyed by PlayKiln asset folder/prefix names.
export const IMAGE_TYPES = Object.freeze({
  phone: 'phoneScreenshots',
  'tablet-7': 'sevenInchScreenshots',
  'tablet-10': 'tenInchScreenshots',
  'feature-graphic': 'featureGraphic',
  icon: 'icon'
})

export function pngSize(file) {
  try {
    const bytes = readFileSync(file)
    if (bytes.length < 24 || !bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) return null
    if (bytes.toString('ascii', 12, 16) !== 'IHDR') return null
    const width = bytes.readUInt32BE(16)
    const height = bytes.readUInt32BE(20)
    return width > 0 && height > 0 ? { width, height } : null
  } catch {
    return null
  }
}

export function jpegSize(file) {
  try {
    const bytes = readFileSync(file)
    if (bytes.length < 4 || bytes[0] !== 0xff || bytes[1] !== 0xd8) return null

    let offset = 2
    while (offset + 3 < bytes.length) {
      while (offset < bytes.length && bytes[offset] !== 0xff) offset += 1
      while (offset < bytes.length && bytes[offset] === 0xff) offset += 1
      if (offset >= bytes.length) break

      const marker = bytes[offset]
      offset += 1
      if (marker === 0xd8 || marker === 0xd9 || marker === 0x01 || (marker >= 0xd0 && marker <= 0xd7)) continue
      if (offset + 1 >= bytes.length) return null

      const length = bytes.readUInt16BE(offset)
      if (length < 2 || offset + length > bytes.length) return null
      if (isStartOfFrame(marker)) {
        if (length < 7) return null
        const height = bytes.readUInt16BE(offset + 3)
        const width = bytes.readUInt16BE(offset + 5)
        return width > 0 && height > 0 ? { width, height } : null
      }
      offset += length
    }
    return null
  } catch {
    return null
  }
}

function isStartOfFrame(marker) {
  return marker >= 0xc0 && marker <= 0xcf && ![0xc4, 0xc8, 0xcc].includes(marker)
}

export function isValidScreenshot(width, height) {
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) return false
  const shortSide = Math.min(width, height)
  const longSide = Math.max(width, height)
  return (
    shortSide >= PHONE_SCREENSHOTS.minSide &&
    longSide <= PHONE_SCREENSHOTS.maxSide &&
    longSide <= shortSide * PHONE_SCREENSHOTS.maxAspectRatio
  )
}

export function validateListingText({ title, shortDescription, fullDescription } = {}) {
  const values = { title, shortDescription, fullDescription }
  const labels = {
    title: 'Title',
    shortDescription: 'Short description',
    fullDescription: 'Full description'
  }
  const problems = []

  for (const [key, limit] of Object.entries(LISTING_LIMITS)) {
    if (typeof values[key] !== 'string' || values[key].trim().length === 0) {
      problems.push(`${labels[key]} is required`)
      continue
    }
    const length = [...values[key]].length
    if (length > limit) problems.push(`${labels[key]} is ${length} characters; maximum is ${limit}`)
  }
  return problems
}

export function bytesToHuman(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) return 'unknown'
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB', 'TB']
  let value = bytes
  let unit = -1
  do {
    value /= 1024
    unit += 1
  } while (value >= 1024 && unit < units.length - 1)
  const precision = value >= 100 ? 0 : value >= 10 ? 1 : 2
  return `${value.toFixed(precision)} ${units[unit]}`
}

export function sha256(file) {
  return createHash('sha256').update(readFileSync(file)).digest('hex')
}

export function readListing(slotDir) {
  const file = path.join(slotDir, 'store', 'listing.json')
  const raw = JSON.parse(readFileSync(file, 'utf8').replace(/^\uFEFF/, ''))
  const defaultLocale = raw.defaultLocale || raw.defaultLanguage || LOCALES[0]
  const source = raw.listings || raw.locales || raw.translations || raw
  const localeKeys = Object.keys(source).filter((key) => /^[a-z]{2}(?:-[A-Z]{2})?$/.test(key) && isObject(source[key]))
  const locales = {}

  if (localeKeys.length > 0) {
    for (const locale of localeKeys) locales[locale] = normalizeListing(source[locale])
  } else {
    locales[defaultLocale] = normalizeListing(raw)
  }

  const primary = locales[defaultLocale] || { title: '', shortDescription: '', fullDescription: '' }
  return { defaultLocale, locales, ...primary }
}

function normalizeListing(value) {
  const listing = isObject(value) ? value : {}
  return {
    title: stringValue(listing.title || listing.name),
    shortDescription: stringValue(listing.shortDescription || listing.short_description || listing.short),
    fullDescription: stringValue(listing.fullDescription || listing.full_description || listing.description)
  }
}

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function stringValue(value) {
  return typeof value === 'string' ? value.trim() : ''
}

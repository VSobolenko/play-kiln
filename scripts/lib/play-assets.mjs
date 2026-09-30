import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { inflateSync } from 'node:zlib'

// Google Play app icon rule: 512x512 32-bit PNG with alpha, at most 1024 KB.
export const APP_ICON = Object.freeze({ width: 512, height: 512, maxBytes: 1024 * 1024, format: 'png', alpha: true })

// Google Play feature graphic rule: exactly 1024x500, JPEG or 24-bit PNG without alpha, at most 15 MB.
export const FEATURE_GRAPHIC = Object.freeze({ width: 1024, height: 500, formats: Object.freeze(['jpeg', 'png']), alpha: false, maxBytes: 15 * 1024 * 1024 })

// The aspect rule the Play Console states in EVERY screenshot field, in the console's own inline
// words (measured live, RU console, 2026-09-29, on the app's `Стандартная страница приложения`
// page): "Соотношение сторон изображения должно составлять 16:9 или 9:16". Phone, 7-inch and
// 10-inch share the sentence, so it is one constant here rather than a per-kind key.
//
// `store-kit.py` -> `SCREENSHOT_ASPECT_RATIOS` remains the authoritative copy (it is what renders
// and validates the pack, and what `scripts/verify-slot.mjs` reads); this export exists so a
// caller of this library applies the same rule rather than trusting `maxAspectRatio: 2`, which is
// Play's weaker general-Requirements cap and never the console's aspect requirement.
export const SCREENSHOT_ASPECT_RATIOS = Object.freeze([Object.freeze([16, 9]), Object.freeze([9, 16])])

// Google Play phone screenshot rule: 2-8 images, 320-3840px sides, long side no more than twice the short side.
//
// `min` is the LOWEST count the factory accepts, not Play's absolute floor of 2.
// Play's general Requirements section allows as few as 2 phone screenshots, but
// its large-screen section requires "a minimum of 4 screenshots", and the factory
// ships 6 (`scripts/store-kit.py` -> `SCREEN_TARGETS["phone"]`). Keeping the
// weaker reading here while `scripts/verify-slot.mjs` enforced 4 meant the library
// and the gate disagreed; `scripts/store-kit.py` is authoritative for this rule.
// Source: https://support.google.com/googleplay/android-developer/answer/9866151?hl=en
export const PHONE_SCREENSHOTS = Object.freeze({
  min: 4,
  max: 8,
  target: 6,
  minSide: 320,
  maxSide: 3840,
  maxAspectRatio: 2,
  maxBytes: 8 * 1024 * 1024,
  recommended: Object.freeze({ width: 1080, height: 1920 }),
  largeFormat: Object.freeze({ minCount: 4, minShortSide: 1080 })
})

// `min` and `largeFormat.minCount` state the same rule twice because
// `scripts/visual-qa.py` reads `largeFormat.minCount` by name. This assertion
// makes the duplication impossible to drift: the contradiction it prevents is
// exactly the defect that let the library say 2 while the gate enforced 4.
if (PHONE_SCREENSHOTS.largeFormat.minCount !== PHONE_SCREENSHOTS.min) {
  throw new Error(
    'play-assets: PHONE_SCREENSHOTS.min and largeFormat.minCount must be equal, ' +
      `got ${PHONE_SCREENSHOTS.min} and ${PHONE_SCREENSHOTS.largeFormat.minCount}`
  )
}

// A real composed marketing frame always has structure — a background gradient,
// a caption, a device mock-up — so its luminance varies and it carries many
// colours. A solid fill, a one-colour placeholder, or a frame whose render failed
// has neither, yet every dimension/mode/format check calls it valid. Both
// `store-kit --validate-only` and `verify-slot --strict-store` accepted a solid
// red 1080x1920 PNG as a screenshot because nothing ever looked at a pixel; these
// floors close that hole.
//
// Calibrated on 2026-09-25 against all 86 committed assets under
// `apps/*/store/assets/*/`: the least-structured real screenshot measured
// luminance sd 45.15, the fewest-coloured real screenshot measured 114 quantised
// colours, and the flattest asset of any kind — a real app icon — measured sd
// 52.00 (its 15 colours are why the colour floor is applied to screenshots only).
// A solid colour measures sd 0.00 and one colour, so floors of 6 (every asset)
// and 12 (screenshots) sit ~7x and ~9x clear of real art while a blank frame
// fails hard.
export const NON_DEGENERATE = Object.freeze({
  // Applies to every composed PNG the pack ships, icon and feature graphic
  // included: a uniform image is a failed render at any size.
  minLuminanceStdDev: 6,
  // Applies to screenshots only. A composed marketing frame carries hundreds of
  // quantised colours, but a legitimate flat app icon can be two-tone, so gating
  // the icon on colour count would reject valid art rather than a defect.
  minScreenshotColours: 12
})

// Google Play 7-inch tablet screenshot rule: 4-8 images, sides 320-3840px, at most 8 MB each.
//
// The console's 7-inch field accepts 320..3840 ("каждая сторона 320..3840"), the same window as
// the phone field, so this is the laxer of the two tablet windows: the 1080px floor belongs to
// the 10-inch field, not this one. `SCREENSHOT_ASPECT_RATIOS` above is the ratio rule both
// tablet fields and the phone field share.
export const TABLET_7 = Object.freeze({ min: 4, max: 8, minShortSide: 320, maxLongSide: 3840, maxBytes: 8 * 1024 * 1024 })

// Google Play 10-inch tablet screenshot rule: 4-8 images, sides 1080-7680px, at most 8 MB each.
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

const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])

export function pngSize(file) {
  try {
    return pngSizeFromBytes(readFileSync(file))
  } catch {
    return null
  }
}

function pngSizeFromBytes(bytes) {
  if (bytes.length < 24 || !bytes.subarray(0, 8).equals(PNG_SIGNATURE)) return null
  if (bytes.toString('ascii', 12, 16) !== 'IHDR') return null
  const width = bytes.readUInt32BE(16)
  const height = bytes.readUInt32BE(20)
  return width > 0 && height > 0 ? { width, height } : null
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

/**
 * The console's aspect rule for one screenshot size, or `null` when it is 16:9 or 9:16.
 *
 * The comparison cross-multiplies so an exactly 9:16 canvas is decided by integers, never by a
 * float rounding a pixel-perfect 1080x1920 into a near miss.
 */
export function aspectRatioProblem(kind, width, height) {
  for (const [across, down] of SCREENSHOT_ASPECT_RATIOS) {
    if (width * down === height * across || width * across === height * down) return null
  }
  const allowed = SCREENSHOT_ASPECT_RATIOS.map(([across, down]) => `${across}:${down}`).join(' or ')
  return `${kind}: ${width}x${height} is not ${allowed}`
}

export function isValidScreenshot(width, height) {
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) return false
  const shortSide = Math.min(width, height)
  const longSide = Math.max(width, height)
  return (
    shortSide >= PHONE_SCREENSHOTS.minSide &&
    longSide <= PHONE_SCREENSHOTS.maxSide &&
    longSide <= shortSide * PHONE_SCREENSHOTS.maxAspectRatio &&
    aspectRatioProblem('screenshot', width, height) === null
  )
}

/**
 * Measure whether a PNG carries real content, cheaply and without a dependency.
 *
 * Returns `{ width, height, luminanceStdDev, distinctColours }`, or `null` when
 * the bytes are not a non-interlaced 8-bit PNG this can decode. `null` means
 * "could not check", which the caller must treat as a failure rather than a pass:
 * a truncated file decodes to nothing and must not slip through looking valid.
 *
 * Only `node:zlib` is used on purpose. Shelling out to the pack's Python/Pillow
 * would make the gate fail wherever Pillow is absent, even though the check
 * itself needs no Pillow; the factory's own gate must stay dependency-free.
 */
export function pngContentStats(file) {
  try {
    const bytes = readFileSync(file)
    const size = pngSizeFromBytes(bytes)
    if (!size) return null
    const bitDepth = bytes[24]
    const colorType = bytes[25]
    const interlace = bytes[28]
    const channels = { 0: 1, 2: 3, 4: 2, 6: 4 }[colorType]
    if (bitDepth !== 8 || interlace !== 0 || !channels) return null

    const idat = []
    let offset = 8
    while (offset + 12 <= bytes.length) {
      const length = bytes.readUInt32BE(offset)
      const type = bytes.toString('ascii', offset + 4, offset + 8)
      if (offset + 12 + length > bytes.length) return null
      if (type === 'IDAT') idat.push(bytes.subarray(offset + 8, offset + 8 + length))
      offset += 12 + length
      if (type === 'IEND') break
    }
    if (idat.length === 0) return null

    const raw = inflateSync(Buffer.concat(idat))
    const { width, height } = size
    const stride = width * channels
    if (raw.length < height * (stride + 1)) return null

    const previous = Buffer.alloc(stride)
    const row = Buffer.alloc(stride)
    // 4 bits per channel keeps the colour histogram at 16^3 slots, small enough
    // to allocate once and large enough that a photo-like frame fills hundreds.
    const seen = new Uint8Array(4096)
    let distinctColours = 0
    let sum = 0
    let sumOfSquares = 0
    let index = 0

    for (let y = 0; y < height; y += 1) {
      const filter = raw[index]
      index += 1
      raw.copy(row, 0, index, index + stride)
      index += stride
      unfilterPngRow(filter, row, previous, channels)
      for (let x = 0; x < stride; x += channels) {
        const red = row[x]
        const green = channels >= 3 ? row[x + 1] : red
        const blue = channels >= 3 ? row[x + 2] : red
        const luminance = (red * 77 + green * 150 + blue * 29) >> 8
        sum += luminance
        sumOfSquares += luminance * luminance
        const quantised = ((red >> 4) << 8) | ((green >> 4) << 4) | (blue >> 4)
        if (seen[quantised] === 0) {
          seen[quantised] = 1
          distinctColours += 1
        }
      }
      previous.set(row)
    }

    const pixels = width * height
    const mean = sum / pixels
    const variance = Math.max(0, sumOfSquares / pixels - mean * mean)
    return { width, height, luminanceStdDev: Math.sqrt(variance), distinctColours }
  } catch {
    return null
  }
}

function unfilterPngRow(filter, row, previous, bytesPerPixel) {
  const length = row.length
  if (filter === 0) return
  if (filter === 1) {
    for (let i = bytesPerPixel; i < length; i += 1) row[i] = (row[i] + row[i - bytesPerPixel]) & 0xff
    return
  }
  if (filter === 2) {
    for (let i = 0; i < length; i += 1) row[i] = (row[i] + previous[i]) & 0xff
    return
  }
  if (filter === 3) {
    for (let i = 0; i < length; i += 1) {
      const left = i >= bytesPerPixel ? row[i - bytesPerPixel] : 0
      row[i] = (row[i] + ((left + previous[i]) >> 1)) & 0xff
    }
    return
  }
  if (filter === 4) {
    for (let i = 0; i < length; i += 1) {
      const left = i >= bytesPerPixel ? row[i - bytesPerPixel] : 0
      const above = previous[i]
      const upperLeft = i >= bytesPerPixel ? previous[i - bytesPerPixel] : 0
      row[i] = (row[i] + paethPredictor(left, above, upperLeft)) & 0xff
    }
    return
  }
  throw new Error(`unsupported PNG filter type ${filter}`)
}

function paethPredictor(left, above, upperLeft) {
  const estimate = left + above - upperLeft
  const leftDistance = Math.abs(estimate - left)
  const aboveDistance = Math.abs(estimate - above)
  const upperLeftDistance = Math.abs(estimate - upperLeft)
  if (leftDistance <= aboveDistance && leftDistance <= upperLeftDistance) return left
  if (aboveDistance <= upperLeftDistance) return above
  return upperLeft
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

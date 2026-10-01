// The Play asset contract, enforced from scripts/lib/play-assets.json.
//
// Every window here was read from the console's own inline strings rather than from the
// documentation; the provenance for each value is openspec/changes/reduce-factory-to-core/
// requirements-preservation.md §4. Two rules matter enough to restate in code:
//
//   1. The two tablet windows genuinely differ (320..3840 against 1080..7680), so the sizes are
//      checked per kind. A single blanket predicate previously rejected a legal tablet asset.
//   2. The 16:9-or-9:16 sentence the console states in EVERY screenshot field is decided by
//      integer cross-multiplication, so an exactly-9:16 canvas is never a float near-miss.
import { readFileSync, existsSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import {
  expectEqual,
  expectTrue,
  fail,
  guard,
  pass,
  playAssets,
  ROOT,
  skip,
  unverified
} from './lib.mjs'
import {
  APP_ICON,
  FEATURE_GRAPHIC,
  LISTING_LIMITS,
  LOCALES,
  NON_DEGENERATE,
  PHONE_SCREENSHOTS,
  SCREENSHOT_ASPECT_RATIOS,
  TABLET_10,
  TABLET_7,
  pngContentStats,
  pngSize,
  readListing,
  sha256
} from '../lib/play-assets.mjs'

const KINDS = ['phone', 'tablet-7', 'tablet-10']

const perLocaleKinds = (assets) =>
  Object.entries(assets.screens).map(([kind, spec]) => ({
    kind,
    ship: spec.ship,
    min: spec.min,
    max: spec.max,
    minShortSide: spec.minShortSide,
    maxLongSide: spec.maxLongSide,
    maxRatio: spec.maxRatio,
    maxBytes: spec.maxBytes,
    width: spec.targetWidth,
    height: spec.targetHeight
  }))

// ---------------------------------------------------------------------------------------------
// One source of truth: the JSON is authoritative, and the legacy Node constants must still agree
// with it. Without this, "import a single source of truth" is an intention rather than a fact.
// ---------------------------------------------------------------------------------------------

export function checkLimitsAgreement() {
  return guard('limits.agreement', () => {
    const assets = playAssets()
    const problems = []
    const same = (label, actual, expected) =>
      actual === expected || problems.push(`${label}: constant ${actual} != json ${expected}`)

    same('icon.width', APP_ICON.width, assets.icon.width)
    same('icon.height', APP_ICON.height, assets.icon.height)
    same('icon.maxBytes', APP_ICON.maxBytes, assets.icon.maxBytes)
    same('feature.width', FEATURE_GRAPHIC.width, assets.featureGraphic.width)
    same('feature.height', FEATURE_GRAPHIC.height, assets.featureGraphic.height)
    same('feature.maxBytes', FEATURE_GRAPHIC.maxBytes, assets.featureGraphic.maxBytes)
    same('phone.min', PHONE_SCREENSHOTS.min, assets.screens.phone.min)
    same('phone.max', PHONE_SCREENSHOTS.max, assets.screens.phone.max)
    same('phone.minSide', PHONE_SCREENSHOTS.minSide, assets.screens.phone.minShortSide)
    same('phone.maxSide', PHONE_SCREENSHOTS.maxSide, assets.screens.phone.maxLongSide)
    same('phone.maxBytes', PHONE_SCREENSHOTS.maxBytes, assets.screens.phone.maxBytes)
    same('tablet7.min', TABLET_7.min, assets.screens['tablet-7'].min)
    same('tablet7.minShortSide', TABLET_7.minShortSide, assets.screens['tablet-7'].minShortSide)
    same('tablet7.maxLongSide', TABLET_7.maxLongSide, assets.screens['tablet-7'].maxLongSide)
    same('tablet10.minShortSide', TABLET_10.minShortSide, assets.screens['tablet-10'].minShortSide)
    same('tablet10.maxLongSide', TABLET_10.maxLongSide, assets.screens['tablet-10'].maxLongSide)
    same('listing.title', LISTING_LIMITS.title, assets.listing.title)
    same('listing.short', LISTING_LIMITS.shortDescription, assets.listing.shortDescription)
    same('listing.full', LISTING_LIMITS.fullDescription, assets.listing.fullDescription)
    same('nonDegenerate.minLuminanceStdDev', NON_DEGENERATE.minLuminanceStdDev, assets.nonDegenerate.minLuminanceStdDev)
    same('nonDegenerate.minScreenshotColours', NON_DEGENERATE.minScreenshotColours, assets.nonDegenerate.minScreenshotColours)
    same('locales.default', LOCALES[0], assets.locales.default)
    same('locales.count', LOCALES.length, 1 + assets.locales.additional.length)
    same('aspect.count', SCREENSHOT_ASPECT_RATIOS.length, assets.aspectRatios.length)
    for (const [index, [across, down]] of SCREENSHOT_ASPECT_RATIOS.entries()) {
      const json = assets.aspectRatios[index]
      same(`aspect[${index}][0]`, across, json[0])
      same(`aspect[${index}][1]`, down, json[1])
    }
    // The one contradiction the legacy library asserted in code, kept because it is the defect
    // class: the library once said `min: 2` while the gate enforced 4.
    same('phone.largeFormat.minCount', PHONE_SCREENSHOTS.largeFormat.minCount, assets.screens.phone.min)

    return problems.length === 0
      ? pass('limits.agreement', 'play-assets.json and the Node constants agree')
      : fail('limits.agreement', problems.join('; '))
  })
}

// ---------------------------------------------------------------------------------------------
// Inline fixtures — the sensors this module depends on, made falsifiable.
// ---------------------------------------------------------------------------------------------

export function checkAssetFixtures() {
  return guard('assets.fixtures', () => {
    const fixtures = []
    const assets = playAssets()

    fixtures.push(['1280x720 is 16:9', () => expectTrue(isAspect(1280, 720, assets), '16:9')])
    fixtures.push(['1080x1920 is 9:16', () => expectTrue(isAspect(1080, 1920, assets), '9:16')])
    fixtures.push(['1920x1080 is 16:9', () => expectTrue(isAspect(1920, 1080, assets), 'rotated 16:9')])
    // The defect this guard exists for: a 5:8 canvas the factory once shipped.
    fixtures.push(['1200x1920 is neither', () => expectTrue(!isAspect(1200, 1920, assets), '5:8 rejected')])
    fixtures.push(['1600x2560 is neither', () => expectTrue(!isAspect(1600, 2560, assets), '5:8 rejected')])
    // A float implementation rounds 1080/1920 to 0.5625 and passes; the integer form is exact, so
    // a canvas one pixel off 9:16 must still be refused.
    fixtures.push(['1081x1920 is neither', () => expectTrue(!isAspect(1081, 1920, assets), 'near-miss rejected')])

    // Per-kind windows: the 1080 px floor belongs to tablet-10 and NOT to tablet-7. A 720x1280
    // canvas is inside tablet-7's window (short side >= 320) and outside tablet-10's (>= 1080),
    // which is the exact pair a single blanket predicate got wrong.
    fixtures.push([
      'tablet-7 accepts a 720x1280 canvas',
      () => expectTrue(sizeProblem('tablet-7', 720, 1280, 1000, assets) === null, 'accepted')
    ])
    fixtures.push([
      'tablet-10 refuses a 720x1280 canvas',
      () => expectTrue(sizeProblem('tablet-10', 720, 1280, 1000, assets) !== null, 'refused')
    ])
    fixtures.push([
      'tablet-10 accepts its own 1440x2560 target',
      () => expectTrue(sizeProblem('tablet-10', 1440, 2560, 1000, assets) === null, 'accepted')
    ])
    fixtures.push([
      'phone refuses a 1200-wide long edge of 4000',
      () => expectTrue(sizeProblem('phone', 1200, 4000, 1000, assets) !== null, 'refused')
    ])
    fixtures.push([
      'byte cap is honoured',
      () => expectTrue(sizeProblem('phone', 1080, 1920, assets.screens.phone.maxBytes + 1, assets) !== null, 'refused')
    ])
    fixtures.push([
      'a release-note block with two tags counts two languages',
      () => expectEqual(countNoteLanguages('<en-US>\nhello\n</en-US>\n<ru-RU>\nпривет\n</ru-RU>'), 2, 'tag count')
    ])

    const failures = []
    for (const [label, body] of fixtures) {
      try {
        body()
      } catch (error) {
        failures.push(`${label}: ${error.message}`)
      }
    }
    return failures.length === 0
      ? pass('assets.fixtures', `${fixtures.length} fixtures`)
      : fail('assets.fixtures', failures.join(' | '))
  })
}

// ---------------------------------------------------------------------------------------------
// Geometry primitives, exported so the fixtures above exercise the real implementations.
// ---------------------------------------------------------------------------------------------

/** True when the canvas matches one of the ratios the console states, by integer arithmetic. */
export function isAspect(width, height, assets = playAssets()) {
  for (const [across, down] of assets.aspectRatios) {
    if (width * down === height * across || width * across === height * down) return true
  }
  return false
}

/** Returns a problem string, or null when the canvas is inside its own kind's window. */
export function sizeProblem(kind, width, height, bytes, assets = playAssets()) {
  const spec = assets.screens[kind]
  if (!spec) return `${kind}: unknown screenshot kind`
  const shortSide = Math.min(width, height)
  const longSide = Math.max(width, height)
  if (shortSide < spec.minShortSide) {
    return `${kind}: short side ${shortSide} below ${spec.minShortSide}`
  }
  if (longSide > spec.maxLongSide) {
    return `${kind}: long side ${longSide} above ${spec.maxLongSide}`
  }
  if (spec.maxRatio !== null && spec.maxRatio !== undefined && longSide > shortSide * spec.maxRatio) {
    return `${kind}: long side ${longSide} exceeds ${spec.maxRatio}x the short side ${shortSide}`
  }
  if (bytes > spec.maxBytes) {
    return `${kind}: ${bytes} bytes over ${spec.maxBytes}`
  }
  if (!isAspect(width, height, assets)) {
    const allowed = assets.aspectRatios.map(([a, d]) => `${a}:${d}`).join(' or ')
    return `${kind}: ${width}x${height} is not ${allowed}`
  }
  return null
}

/** Counts how many language tags a release-notes paste block carries, for the fixtures. */
export function countNoteLanguages(block) {
  return [...block.matchAll(/^<([A-Za-z]{2}(?:-[A-Za-z]{2})?)>\s*$/gm)].length
}

// ---------------------------------------------------------------------------------------------
// The pack walk
// ---------------------------------------------------------------------------------------------

const localeDirs = (assetsDir) => {
  if (!existsSync(assetsDir)) return []
  return readdirSync(assetsDir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .filter((name) => /^[a-z]{2}(?:-[A-Z]{2})?$/.test(name))
    .sort()
}

export function checkPack(slotDir) {
  return guard('pack.inventory', () => {
    const assets = playAssets()
    const storeAssets = path.join(slotDir, 'store', 'assets')
    if (!existsSync(storeAssets)) {
      return unverified('pack.inventory', `no store/assets at ${path.relative(ROOT, storeAssets)}`)
    }
    const problems = []
    const locales = localeDirs(storeAssets)
    if (locales.length === 0) problems.push('no <locale> directory under store/assets')
    if (!locales.includes(assets.locales.default)) {
      problems.push(`the default locale ${assets.locales.default} has no asset directory`)
    }

    // Root-level assets: the pack's own icon and feature graphic.
    for (const [file, label] of [
      ['icon-512.png', 'icon'],
      ['feature-graphic-1024x500.png', 'feature graphic']
    ]) {
      if (!existsSync(path.join(storeAssets, file))) problems.push(`missing ${label}: assets/${file}`)
    }

    for (const locale of locales) {
      const dir = path.join(storeAssets, locale)
      for (const [file] of [['icon-512.png'], ['feature-graphic-1024x500.png']]) {
        if (!existsSync(path.join(dir, file))) problems.push(`missing ${locale}/${file}`)
      }
      for (const { kind, ship, min, max } of perLocaleKinds(assets)) {
        const present = readdirSync(dir).filter((name) => name.startsWith(`${kind}-`) && name.endsWith('.png'))
        if (present.length !== ship) {
          problems.push(`${locale} ${kind}: ${present.length} present, ${ship} required (Play allows ${min}-${max})`)
        }
      }
    }
    return problems.length === 0
      ? pass('pack.inventory', `${locales.length} locale(s), counts match the declared inventory`)
      : fail('pack.inventory', problems.join('; '))
  })
}

/**
 * Measures every composed asset and returns both the problems and the rows measured.
 *
 * The rows are returned rather than only printed so an equivalence test can compare this
 * implementation against another one asset by asset. A check whose findings exist only as a
 * formatted string cannot be cross-checked, only trusted.
 */
export function inspectPackMedia(slotDir) {
  const assets = playAssets()
  const storeAssets = path.join(slotDir, 'store', 'assets')
  if (!existsSync(storeAssets)) {
    return { absent: true, problems: [], rows: [] }
  }
  const problems = []
  const rows = []

  const inspect = (file, kind) => {
    const bytes = statSync(file).size
    const size = pngSize(file)
    const rel = path.relative(storeAssets, file).split(path.sep).join('/')
    if (!size) {
      problems.push({ rel, problem: 'not a readable PNG' })
      return
    }
    let problem = null
    if (kind === 'icon') {
      if (size.width !== assets.icon.width || size.height !== assets.icon.height) {
        problem = `icon: ${size.width}x${size.height}, expected ${assets.icon.width}x${assets.icon.height}`
      } else if (bytes > assets.icon.maxBytes) {
        problem = `icon: ${bytes} bytes over ${assets.icon.maxBytes}`
      }
    } else if (kind === 'feature-graphic') {
      if (size.width !== assets.featureGraphic.width || size.height !== assets.featureGraphic.height) {
        problem = `feature graphic: ${size.width}x${size.height}, expected ${assets.featureGraphic.width}x${assets.featureGraphic.height}`
      } else if (bytes > assets.featureGraphic.maxBytes) {
        problem = `feature graphic: ${bytes} bytes over ${assets.featureGraphic.maxBytes}`
      }
    } else {
      problem = sizeProblem(kind, size.width, size.height, bytes, assets)
      const expected = assets.screens[kind]
      if (problem === null && (size.width !== expected.targetWidth || size.height !== expected.targetHeight)) {
        problem = `${kind}: ${size.width}x${size.height}, expected ${expected.targetWidth}x${expected.targetHeight} from the declared target`
      }
    }
    if (problem) problems.push({ rel, problem })

    // Pixel content: a uniform fill or a failed render satisfies every geometry check.
    const stats = pngContentStats(file)
    if (!stats) {
      problems.push({ rel, problem: 'could not decode pixels, which is a failure and not a pass' })
      return
    }
    const floor = assets.nonDegenerate.minLuminanceStdDev
    if (stats.luminanceStdDev < floor) {
      problems.push({ rel, problem: `luminance sd ${stats.luminanceStdDev.toFixed(2)} below ${floor}` })
    }
    if (kind !== 'icon' && kind !== 'feature-graphic') {
      const colourFloor = assets.nonDegenerate.minScreenshotColours
      if (stats.distinctColours < colourFloor) {
        problems.push({ rel, problem: `${stats.distinctColours} colours below ${colourFloor}` })
      }
    }
    rows.push({
      rel,
      kind,
      width: size.width,
      height: size.height,
      bytes,
      luminanceStdDev: stats.luminanceStdDev,
      distinctColours: stats.distinctColours,
      problem
    })
  }

  for (const locale of localeDirs(storeAssets)) {
    const dir = path.join(storeAssets, locale)
    const icon = path.join(dir, 'icon-512.png')
    const feature = path.join(dir, 'feature-graphic-1024x500.png')
    if (existsSync(icon)) inspect(icon, 'icon')
    if (existsSync(feature)) inspect(feature, 'feature-graphic')
    for (const kind of KINDS) {
      for (const name of readdirSync(dir).filter((n) => n.startsWith(`${kind}-`) && n.endsWith('.png')).sort()) {
        inspect(path.join(dir, name), kind)
      }
    }
  }
  // Root-level assets too: the pack ships its own icon and feature graphic there.
  for (const name of ['icon-512.png', 'feature-graphic-1024x500.png']) {
    const file = path.join(storeAssets, name)
    if (existsSync(file)) inspect(file, name.startsWith('icon') ? 'icon' : 'feature-graphic')
  }
  return { absent: false, problems, rows }
}

export function checkPackMedia(slotDir) {
  return guard('pack.media', () => {
    const { absent, problems, rows } = inspectPackMedia(slotDir)
    if (absent) return unverified('pack.media', 'no store/assets')
    return problems.length === 0
      ? pass('pack.media', `${rows.length} assets inside their own kind's window with real pixels`)
      : fail('pack.media', problems.map(({ rel, problem }) => `${rel}: ${problem}`).join('; '))
  })
}

const LISTING_LIMIT_KEYS = ['title', 'shortDescription', 'fullDescription']

export function checkListingText(slotDir) {
  return guard('pack.listing', () => {
    const assets = playAssets()
    const file = path.join(slotDir, 'store', 'listing.json')
    if (!existsSync(file)) return unverified('pack.listing', 'no store/listing.json')
    const listing = readListing(slotDir)
    const problems = []
    const limits = assets.listing
    for (const [locale, text] of Object.entries(listing.locales)) {
      // Only the three measured fields are checked. `listing.json` carries descriptive keys of its
      // own (`_countedAs` notes, comments) and treating one of those as a required field produced a
      // FAIL that named a field Play has never heard of.
      for (const key of LISTING_LIMIT_KEYS) {
        const limit = limits[key]
        const value = (text[key] || '').trim()
        if (!value) {
          problems.push(`${locale} ${key}: required and empty`)
          continue
        }
        // Code points, matching the console's own count.
        const length = [...value].length
        if (length > limit) problems.push(`${locale} ${key}: ${length} characters over ${limit}`)
      }
    }
    if (Object.keys(listing.locales).length === 0) problems.push('listing.json declares no locale')
    return problems.length === 0
      ? pass(
          'pack.listing',
          `${Object.keys(listing.locales).length} locale(s) within ` +
            `${limits.title}/${limits.shortDescription}/${limits.fullDescription}`
        )
      : fail('pack.listing', problems.join('; '))
  })
}

/** Extracts the paste block from release-notes.md: a fenced block carrying <locale> tags. */
export function releaseNoteBlock(markdown) {
  const fences = [...markdown.matchAll(/```([a-zA-Z]*)\r?\n([\s\S]*?)```/g)]
  const carrying = fences.filter((match) => countNoteLanguages(match[2]) > 0)
  if (carrying.length === 0) return null
  return carrying[carrying.length - 1][2]
}

export function parseReleaseNotes(block) {
  const notes = {}
  const lines = block.split(/\r?\n/)
  let current = null
  let buffer = []
  for (const line of lines) {
    const open = /^<([A-Za-z]{2}(?:-[A-Za-z]{2})?)>\s*$/.exec(line)
    const close = /^<\/([A-Za-z]{2}(?:-[A-Za-z]{2})?)>\s*$/.exec(line)
    if (open) {
      current = open[1]
      buffer = []
      continue
    }
    if (close) {
      if (current) notes[current] = buffer.join('\n').trim()
      current = null
      buffer = []
      continue
    }
    if (current) buffer.push(line)
  }
  return notes
}

/**
 * The measurable problems in a release-note paste block.
 *
 * Extracted from `checkReleaseNotes` so the fixture set below exercises the rule the check actually
 * applies instead of a copy of it that can drift away. A note for a language the listing does not
 * declare is measured too: text in the paste block is text a human may paste, Play **does** show
 * release notes on the store page, and it **truncates silently** at its cap rather than refusing the
 * upload — so a note left over the cap is one the author never sees shortened.
 */
export function releaseNoteProblems(notes, declaredLocales, limit) {
  const problems = []
  for (const [locale, note] of Object.entries(notes)) {
    const length = [...note].length
    const undeclared = declaredLocales.includes(locale) ? '' : ' (not a declared listing locale)'
    if (length > limit) problems.push(`${locale}: ${length} characters over ${limit}${undeclared}`)
  }
  if (Object.keys(notes).length === 0) problems.push('the paste block carries no measurable note')
  return problems
}

/**
 * The release-note length rule, run against the function the check calls.
 *
 * The cap is counted in **code points**, and the boundary is the interesting part: exactly at the cap
 * passes, one character over fails. An emoji counts as one code point and two UTF-16 units, so a
 * `.length` implementation would refuse a note that is legally at the cap — that near-miss is the
 * fixture that keeps this from being a tautology.
 */
export function checkReleaseNoteFixtures() {
  return guard('releaseNotes.fixtures', () => {
    const limit = playAssets().releaseNote.maxChars
    const cases = []
    cases.push([
      'a note exactly at the cap passes',
      () => expectEqual(releaseNoteProblems({ 'en-US': 'a'.repeat(limit) }, ['en-US'], limit), [], 'problems')
    ])
    cases.push([
      'a note one character over the cap fails, and says by how much',
      () => {
        const problems = releaseNoteProblems({ 'en-US': 'a'.repeat(limit + 1) }, ['en-US'], limit)
        expectEqual(problems.length, 1, 'one problem')
        expectTrue(problems[0].includes(String(limit + 1)), problems[0])
      }
    ])
    cases.push([
      'a note for an undeclared language is still measured',
      () => {
        const problems = releaseNoteProblems({ 'de-DE': 'a'.repeat(limit + 1) }, ['en-US'], limit)
        expectEqual(problems.length, 1, 'one problem')
        expectTrue(problems[0].includes('not a declared listing locale'), problems[0])
      }
    ])
    cases.push([
      'the cap is counted in code points, not UTF-16 units',
      () => {
        const note = '🙂'.repeat(limit)
        expectEqual([...note].length, limit, 'code points')
        expectTrue(note.length > limit, 'utf-16 units exceed the cap, so a .length reading would refuse a legal note')
        expectEqual(releaseNoteProblems({ 'en-US': note }, ['en-US'], limit), [], 'problems')
      }
    ])
    cases.push([
      'a block with no measurable note is a problem rather than a pass',
      () => expectEqual(releaseNoteProblems({}, ['en-US'], limit).length, 1, 'one problem')
    ])

    const failures = []
    for (const [label, body] of cases) {
      try {
        body()
      } catch (error) {
        failures.push(`${label}: ${error.message}`)
      }
    }
    return failures.length === 0
      ? pass('releaseNotes.fixtures', `${cases.length} fixtures`)
      : fail('releaseNotes.fixtures', failures.join(' | '))
  })
}

export function checkReleaseNotes(slotDir) {
  return guard('pack.releaseNotes', () => {
    const assets = playAssets()
    const file = path.join(slotDir, 'store', 'release-notes.md')
    if (!existsSync(file)) return unverified('pack.releaseNotes', 'no store/release-notes.md')
    const block = releaseNoteBlock(readFileSync(file, 'utf8'))
    if (block === null) return unverified('pack.releaseNotes', 'no paste block carrying <locale> tags')
    const notes = parseReleaseNotes(block)
    const limit = assets.releaseNote.maxChars
    const problems = releaseNoteProblems(notes, Object.keys(readListing(slotDir).locales), limit)
    return problems.length === 0
      ? pass('pack.releaseNotes', `${Object.keys(notes).length} language(s) within ${limit} characters`)
      : fail('pack.releaseNotes', problems.join('; '))
  })
}

export function checkPlaceholderHosts(slotDir) {
  return guard('pack.placeholders', () => {
    const assets = playAssets()
    const file = path.join(slotDir, 'store', 'listing.json')
    if (!existsSync(file)) return unverified('pack.placeholders', 'no store/listing.json')
    const raw = JSON.parse(readFileSync(file, 'utf8').replace(/^\uFEFF/, ''))
    const problems = []
    for (const field of ['privacyPolicyUrl', 'contactEmail', 'websiteUrl', 'supportUrl']) {
      const value = raw[field]
      if (typeof value !== 'string' || value.trim() === '') {
        if (field === 'privacyPolicyUrl') problems.push('privacyPolicyUrl is empty; it has no default')
        continue
      }
      if (field === 'contactEmail') continue
      const host = hostOf(value)
      if (!host) {
        problems.push(`${field}: ${value} has no usable host`)
        continue
      }
      if (isPlaceholderHost(host, assets.placeholderHosts)) {
        problems.push(`${field}: ${value} points at the reserved host ${host}`)
      }
    }
    if (typeof raw.contactEmail !== 'string' || raw.contactEmail.trim() === '') {
      problems.push(`contactEmail is empty; the default is ${assets.contactEmail}`)
    }
    return problems.length === 0
      ? pass('pack.placeholders', 'no reserved documentation host, no insecure scheme')
      : fail('pack.placeholders', problems.join('; '))
  })
}

export function hostOf(value) {
  const withScheme = value.includes('//') ? value : `//${value}`
  try {
    return new URL(withScheme).hostname || null
  } catch {
    return null
  }
}

export function isPlaceholderHost(host, hosts) {
  return hosts.some((entry) => host === entry || host.endsWith(`.${entry}`))
}

/**
 * Verifies the committed images against the build they claim to come from.
 *
 * The record has two lists and they are not the same kind of evidence. `composedAssets` are the
 * committed store assets, so each one must hash to what the record says. `rawCaptures` live under
 * the gitignored `store/captures/`, so their absence is expected and is reported as a note rather
 * than a failure — treating them as evidence would make every clean checkout fail.
 */
export function checkProvenance(slotDir) {
  return guard('pack.provenance', () => {
    const store = path.join(slotDir, 'store')
    const record = path.join(store, 'provenance.json')
    const composesMedia = existsSync(path.join(store, 'studio.json'))
    if (!existsSync(record)) {
      return composesMedia
        ? fail(
            'pack.provenance',
            'the pack declares itself composed (store/studio.json) but ships no provenance record'
          )
        : skip('pack.provenance', 'not a composed pack')
    }
    const data = JSON.parse(readFileSync(record, 'utf8').replace(/^\uFEFF/, ''))
    const committed = data.composedAssets || data.assets || data.files
    if (!Array.isArray(committed)) {
      return unverified('pack.provenance', 'the record carries no composedAssets list to verify')
    }

    const problems = []
    let checked = 0
    for (const entry of committed) {
      const rel = entry.file || entry.path
      const expected = entry.sha256 || entry.hash
      if (!rel || !expected) {
        problems.push(`an entry names ${rel ? 'no hash' : 'no file'}: ${JSON.stringify(entry).slice(0, 80)}`)
        continue
      }
      const file = path.join(slotDir, rel)
      if (!existsSync(file)) {
        problems.push(`${rel}: recorded as committed evidence but absent from the tree`)
        continue
      }
      checked += 1
      const actual = sha256(file)
      if (actual !== expected) {
        problems.push(`${rel}: sha256 ${actual.slice(0, 12)}… does not match the record`)
      }
    }

    const rawCount = Array.isArray(data.rawCaptures) ? data.rawCaptures.length : 0
    const rawNote = rawCount ? `; ${rawCount} raw capture(s) under the gitignored captures/ not checked` : ''
    if (problems.length > 0) return fail('pack.provenance', problems.join('; ') + rawNote)
    if (checked === 0) {
      return fail('pack.provenance', `the record lists no composed asset that exists${rawNote}`)
    }
    return pass('pack.provenance', `${checked} committed asset(s) match the recorded build${rawNote}`)
  })
}

/**
 * The checks that need a slot: the pack on disk.
 *
 * `checkLimitsAgreement` and `checkAssetFixtures` are deliberately NOT here. They are project-wide
 * invariants that hold with or without a slot, so a run without `--slot` still evaluates them — an
 * invariant that only runs when someone remembers a flag is the failure this refactor removes.
 */
export function packChecks(slotDir) {
  return [
    checkPack(slotDir),
    checkPackMedia(slotDir),
    checkListingText(slotDir),
    checkReleaseNotes(slotDir),
    checkPlaceholderHosts(slotDir),
    checkProvenance(slotDir)
  ]
}

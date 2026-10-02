// Palette: the derivation contract as a sensor — is a slot's colour system derived rather than
// guessed, and is the one in `store/studio.json` the one in the build.
//
// The contract this enforces is `docs/research/2026-10-02-color-systems-and-palette-derivation.md`,
// written 2026-10-02 after the owner installed a built app and said its palette was unaesthetic: a
// high-chroma lime accent (`#C9F04A`, HCT H123.4 C68.0) on a dark green field (`#12301C`, H151 C22.8)
// reads as neon. Section 0 of that document shows the defect in numbers — the accent sits 31.9° from
// the field's own hue at a chroma ratio of 1.9–3.0, so the two are the same green and one has simply
// been made loud. Sections 2 and 4 and the `## Encodable rules` list are the specification; this
// module is the first program that reads it.
//
// The rules, and what this module does with each:
//
//   gated (a FAIL on a slot the baseline applies to):
//     rules 1 and 4's `on-X` leg   contrast of every declared `on-X`/`X` pair, WCAG 1.4.3 AA 4.5:1
//     rule 29                      both schemes are declared, and every pair holds in both
//     rule 8                       the accent's hue against each chromatic field role (the defect)
//     rule 11/25                   at most three chromatic hue families, plus one error family
//     rule 13                      a field/surface/background is at or below M3's neutral chroma
//     rule 15                      the accent's chroma against the chroma Material 3 would derive
//                                  at the same hue and tone — the one vendor algorithm in the list
//     rule 16                      no role lands in M3's `disliked` volume (H90–111, C>16, T<65)
//     rule 21                      no two declared roles collapse into one colour
//   reported, never gated (the document says so, and this module says so too):
//     rules 12, 20, 28                    a number is measured and printed in one row, and gating it
//                                         would fail the design system this factory follows
//     rules 2,3,4's `container` leg,5,6,7  not encoded — floors above AA, and M3 defines `outline`
//                                         and `*Container` at low contrast by construction
//     rules 14,17,18,19,22,26,27           not encoded — M3 constants and conventions with no defect
//                                         boundary the document asserts
//     rules 23/24 (60-30-10 shares)        not encoded — no slot declares an area and the sources do
//                                         not validate the ratio
//   not encoded:
//     rules 30-34 (distinctiveness, recognition, consistency, accessibility discipline,
//     instrumentation) — subjective by the document's own statement
//
// `contrastRatio` from `scripts/lib/hct.mjs` is the only WCAG implementation in this file. Do NOT use
// `relativeLuminance` for a WCAG decision, here or anywhere: Python's similarly-named function in
// `scripts/lib/colours.py` is a legacy raw-sample luminance whose callers threshold 0..255 pixels at
// 140/150, so the same name means two different things across the two languages and the two answers
// only agree by accident.
//
// `paletteFindings(slotDir)` is the run-time entry point and must never throw; `paletteChecks()`
// returns one baseline-scoped verdict per id, and each leg degrades on its own — a slot with no
// `store/studio.json` still gets its theme contrast measured, and a `Color.kt` that cannot be parsed
// reports UNVERIFIED rather than a finding it cannot support.
//
// A rule that cannot fail is not a rule: `paletteFixtures()` drives every leg in red and in green.
// Registered by `scripts/core/index.mjs` (not by this file — do not edit that registry from here).

import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { chromaHeadroom, contrastRatio, deltaHue, hctFrom, hctOf, isDislikedHct } from '../lib/hct.mjs'
import { SCRATCH_DIR } from '../lib/scratch.mjs'
import { baselineVerdict } from './baseline.mjs'
import { paletteRoleSet, stripComments } from './icons.mjs'
import { expectEqual, expectTrue, guard, ROOT, runFixtures, unverified } from './lib.mjs'
import { walkSlotFiles } from './manifest.mjs'

/** The ids this module reports. The registry in `scripts/core/index.mjs` holds the same list. */
export const PALETTE_IDS = Object.freeze([
  'palette.declared',
  'palette.agreement',
  'palette.contrast',
  'palette.field-chroma',
  'palette.hue-structure',
  'palette.accent-chroma',
  'palette.families',
  'palette.duplicate',
  'palette.dislike'
])

const ID = Object.freeze(Object.fromEntries(PALETTE_IDS.map((id) => [id, id])))

const STUDIO_REL = 'store/studio.json'
const THEME_REL_HINT = 'app/src/main/java/<package>/ui/theme'

// ---------------------------------------------------------------------------------------------
// Thresholds, each with the source it came from — never a number without a reason
// ---------------------------------------------------------------------------------------------

/** WCAG 1.4.3 AA for normal text: rules 1, 4's `on-X` leg and 29 (docs/…/…-derivation.md §5.2). */
const MIN_TEXT_CONTRAST = 4.5
/** A declared palette with fewer roles than this is not the "full palette" visual-identity.mdc asks
 * for; it is this factory's completeness floor, not a number from §5. */
const MIN_ROLES = 4
/** Rule 8 (doc §5.3): the accent's hue offset from a field role, labelled INFERRED there because M3
 * states the analogous step at 30° for a *family* and the document reasons the accent needs more. */
const ACCENT_HUE_OFFSET = 40
/** Rule 8's same-hue escape: if the accent is the field's own hue, its tone must be this far away… */
const ACCENT_TONE_OFFSET = 40
/** …and its chroma this many times the field's. */
const ACCENT_CHROMA_RATIO = 2
/** Rule 13 (doc §5.6): M3's own neutral roles top out around C8.5–11.3, so 12 is the bound. */
const MAX_FIELD_CHROMA = 12
/** Rule 14 (doc §5.6): M3's highest applied chroma, the `disliked` volume's ceiling. Reported. */
const MAX_ROLE_CHROMA = 48
/** Rules 11/20 (doc §6.1): `HCT_FAMILY_TOLERANCE`, the documented constant agents get wrong. */
const FAMILY_TOLERANCE = 30
/** Rule 25: three chromatic families, plus at most one error family. */
const MAX_FAMILIES = 3
/** Rule 11's grey floor: an HCT chroma under 24 is neutral, whatever the hex looks like. */
const FAMILY_CHROMA_FLOOR = 24
/** Rule 15 (doc §5.6): Material 3's TonalSpot derives primary at chroma 36 and its primaryContainer
 * at tone 90, so that tone-90 container is the vendor reference the accent is measured against. */
const M3_ACCENT_CHROMA = 36
const M3_CONTAINER_TONE = 90
/** Rule 15's bound: `C(accent) <= 1.5 * C(SchemeTonalSpot(accent).primaryContainer)`. */
const M3_CONTAINER_FACTOR = 1.5
/** Rule 21 (doc §9): two roles are indistinguishable below this contrast while this close in hue and
 * chroma. The document's example is `paper`/`amber_pale` at 1.006:1. */
const DUPLICATE_CONTRAST = 1.2
const DUPLICATE_HUE = 15
const DUPLICATE_CHROMA = 20

/**
 * What this module does *not* turn into a verdict, named rule by rule.
 *
 * Two notes rather than one, because "measured but not gated" and "not encoded at all" are different
 * claims and a single sentence would have to overstate one of them — the defect
 * `.cursor/rules/grounded-claims.mdc` exists to stop.
 */
const MEASURED_NOT_GATED =
  'measured and printed, never gated: rule 12 (the accent\'s gamut headroom, in `palette.accent-chroma`), ' +
  'rule 20 (a chromatic family wider than 90°, in `palette.families`) and rule 28 (the dark field tone ' +
  'range, in `palette.field-chroma`); the document\'s own text says enforcing rule 12 would fail two ' +
  'real design systems, and rule 17 is satisfied by the offending palette'

const NOT_ENCODED =
  'not encoded at all, and why: rules 2/3/5/6/7 and rule 4\'s `container` leg (floors above AA, and M3 ' +
  'defines `outline` and `*Container` at low contrast by construction — doc §5.2); rules 14/17/18/19/' +
  '22/26/27 (M3 constants and conventions with no defect boundary the document asserts); rules 23/24 ' +
  '(60-30-10 shares — no slot declares an area and the sources do not validate the ratio); rules 30-34 ' +
  '(subjective by the document\'s own statement)'

const PALETTE_NOTES = Object.freeze([
  MEASURED_NOT_GATED,
  NOT_ENCODED,
  'this module reads `store/studio.json` and `app/src/main/java/**/ui/theme/{Color,Theme}.kt`; it ' +
    'cannot see a colour chosen at run time by Material You dynamic colour, a colour inside a ' +
    'drawable, or a colour picked in a `remember {}` block, so a green run is evidence about the ' +
    'declared system and never a certificate about every pixel'
])

/** `on-X` roles and the role each must be legible against (rules 1, 4, 29). */
export const CONTRAST_PAIRS = Object.freeze([
  ['onBackground', 'background'],
  ['onSurface', 'surface'],
  ['onSurfaceVariant', 'surfaceVariant'],
  ['onPrimary', 'primary'],
  ['onPrimaryContainer', 'primaryContainer'],
  ['onSecondary', 'secondary'],
  ['onSecondaryContainer', 'secondaryContainer'],
  ['onTertiary', 'tertiary'],
  ['onTertiaryContainer', 'tertiaryContainer'],
  ['onError', 'error'],
  ['onErrorContainer', 'errorContainer'],
  ['inverseOnSurface', 'inverseSurface']
])

/** The roles rule 13 calls a field: a surface a large area of the screen is painted with. */
export const FIELD_SLOTS = Object.freeze([
  'background',
  'surface',
  'surfaceBright',
  'surfaceDim',
  'surfaceVariant',
  'surfaceContainer',
  'surfaceContainerLow',
  'surfaceContainerLowest',
  'surfaceContainerHigh',
  'surfaceContainerHighest'
])

/** Roles that are never *the* accent: an error, a boundary, a shadow, a surface. */
const NON_ACCENT = /error|scrim|outline|frame|bezel|paper|divider|border|shadow|surface|background/i

/** A colour the studio or Compose can emit: `#RGB`, `#RRGGBB` or `#AARRGGBB`. */
const HEX_VALUE = /^#(?:[0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/

/**
 * A `#RGB`, `#RRGGBB` or eight-digit literal as `#RRGGBB`.
 *
 * The eight-digit form is genuinely ambiguous, and both readings are read correctly when the value is
 * opaque: `#FFRRGGBB` (Android/Compose — the form the studio emits, where a palette `#FFFFFF` reaches
 * the pack as `#FFFFFFFF`) and `#RRGGBBFF` (CSS). A colour that is neither is translucent, and then
 * the Android reading wins, because that is how the slot's own Kotlin declares colours. Getting this
 * wrong is not cosmetic: `parseHex` in `scripts/lib/hct.mjs` accepts `#RGB`/`#RRGGBB` only, so an
 * eight-digit value handed to `hctOf` unnormalised would throw and silently turn four legs UNVERIFIED.
 */
export function toHex6(value) {
  const s = String(value ?? '').trim().replace(/^#/, '').toUpperCase()
  if (s.length === 3) return `#${s[0]}${s[0]}${s[1]}${s[1]}${s[2]}${s[2]}`
  if (s.length === 8) {
    if (s.slice(0, 2) === 'FF') return `#${s.slice(2)}`
    if (s.slice(6) === 'FF') return `#${s.slice(0, 6)}`
    return `#${s.slice(2)}`
  }
  return `#${s}`
}

// ---------------------------------------------------------------------------------------------
// Small shared helpers
// ---------------------------------------------------------------------------------------------

const relIn = (base, file) => path.relative(base, file).split(path.sep).join('/')
const relRepo = (file) => path.relative(ROOT, file).split(path.sep).join('/')
const lineOf = (text, index) => String(text).slice(0, index).split(/\r?\n/).length
const dedupe = (values) => [...new Set(values)]
const round2 = (value) => Math.round(value * 100) / 100
const round1 = (value) => Math.round(value * 10) / 10

/** The first line a pattern matches, or 1 when it matches nothing — never a `slice(0, -1)` surprise. */
function lineOfFirst(text, pattern) {
  const index = String(text).search(pattern)
  return index < 0 ? 1 : lineOf(text, index)
}

/** One `file:line` finding, in the shape every core finding uses. */
const finding = (rel, line, id, message) => `${rel}:${line} ${id}: ${message}`

// ---------------------------------------------------------------------------------------------
// Reading the slot
// ---------------------------------------------------------------------------------------------

/**
 * The slot's studio record. The three states stay distinct — absent, unreadable, read — because the
 * checks answer differently for each and collapsing them would report a missing file as a broken one.
 */
function readStudio(slotDir) {
  const file = path.join(slotDir, STUDIO_REL)
  if (!existsSync(file)) return { file, present: false }
  let text
  try {
    text = readFileSync(file, 'utf8')
  } catch (error) {
    return { file, present: true, error }
  }
  let record
  try {
    record = JSON.parse(text)
  } catch (error) {
    return { file, present: true, error, text }
  }
  return { file, present: true, record, text }
}

/**
 * The palette as `{ name, value }` rows.
 *
 * Both shapes a studio record can carry are accepted — the documented array of `{ role, value }` and
 * an object map of role to value — because refusing the second would report a working pack as
 * undeclared, which is the false positive this sensor exists to avoid.
 */
export function declaredPalette(record) {
  const raw = record && typeof record === 'object' && !Array.isArray(record) ? record.palette : null
  if (Array.isArray(raw)) {
    return raw.map((entry) => {
      if (typeof entry === 'string') return { name: null, value: entry }
      const name = entry?.role ?? entry?.name ?? null
      const value = entry?.value ?? entry?.hex ?? entry?.colour ?? entry?.color ?? null
      return { name: typeof name === 'string' ? name : null, value }
    })
  }
  if (raw && typeof raw === 'object') {
    return Object.entries(raw).map(([name, value]) => ({ name, value }))
  }
  return []
}

const COLOR_LITERAL =
  /\b(?:internal\s+|private\s+|public\s+)?val\s+(\w+)\s*(?::\s*[\w.<>?, ]+)?\s*=\s*Color\(\s*0x([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})\s*\)/g

/** Every `val X = Color(0xAARRGGBB)` the theme file declares, with the line it is on. */
export function colourConstants(text) {
  const clean = stripComments(String(text), 'c')
  const rows = []
  for (const match of clean.matchAll(COLOR_LITERAL)) {
    const digits = match[2].toUpperCase()
    const hex = `#${digits.length === 8 ? digits.slice(2) : digits}`
    rows.push({ name: match[1], hex, line: lineOf(clean, match.index) })
  }
  return rows
}

const SCHEME_BLOCK = {
  light: /\blightColorScheme\s*\(([\s\S]*?)\n\s*\)/,
  dark: /\bdarkColorScheme\s*\(([\s\S]*?)\n\s*\)/
}

const SCHEME_ASSIGNMENT = /(\w+)\s*=\s*([^,\n]+?)\s*,/g

/**
 * The `lightColorScheme`/`darkColorScheme` blocks, each resolved to `slot -> colour row`.
 *
 * A value is resolved by its last dot-segment, so `Chalk.Indigo40` and `Indigo40` are the same
 * constant — which is how the reference slot declares them. A value that resolves to nothing is kept
 * as `unresolved`, not dropped: a reader must be able to tell "this role is not declared" from "this
 * role's constant is not in Color.kt", and only the second is drift.
 */
export function themeSchemes(themeText, constants) {
  const clean = stripComments(String(themeText), 'c')
  const byName = new Map(constants.map((row) => [row.name, row]))
  const schemes = {}
  for (const [name, pattern] of Object.entries(SCHEME_BLOCK)) {
    const block = clean.match(pattern)
    if (!block) continue
    const offset = block.index + block[0].indexOf(block[1])
    const slots = {}
    const unresolved = []
    for (const match of block[1].matchAll(SCHEME_ASSIGNMENT)) {
      const slot = match[1]
      const expression = match[2].trim()
      const tail = expression.split('.').pop().replace(/\(.*$/, '').trim()
      const constant = byName.get(tail)
      const line = lineOf(clean, offset + match.index)
      if (constant) slots[slot] = { ...constant, slot, line, scheme: name }
      else unresolved.push({ slot, expression, line })
    }
    schemes[name] = { slots, unresolved, line: lineOf(clean, block.index) }
  }
  return schemes
}

/** The slot's theme files, or the empty readings that make every dependent leg UNVERIFIED. */
function readTheme(slotDir) {
  const javaRoot = path.join(slotDir, 'app', 'src', 'main', 'java')
  const files = walkSlotFiles(javaRoot)
  const colorFile = files.find((file) => /ui[\\/]theme[\\/]Color\.kt$/i.test(file)) || null
  const themeFile = files.find((file) => /ui[\\/]theme[\\/]Theme\.kt$/i.test(file)) || null
  const read = (file) => {
    if (!file) return ''
    try {
      return readFileSync(file, 'utf8')
    } catch {
      return ''
    }
  }
  const colorText = read(colorFile)
  const themeText = read(themeFile)
  const constants = colourConstants(colorText)
  return {
    javaRoot,
    colorFile,
    themeFile,
    colorText,
    themeText,
    constants,
    constantsByName: new Map(constants.map((row) => [row.name, row])),
    schemes: themeSchemes(themeText, constants)
  }
}

/** The accent candidate: the most chromatic declared role that is not an error, boundary or field. */
export function accentOf(palette) {
  let best = null
  for (const role of palette) {
    const value = String(role.value ?? '').trim()
    if (!HEX_VALUE.test(value)) continue
    const hex = toHex6(value)
    const hct = hctOf(hex)
    if (hct.c < MAX_FIELD_CHROMA) continue
    if (role.name && NON_ACCENT.test(role.name)) continue
    if (!best || hct.c > best.hct.c) best = { role, hex, hct }
  }
  return best
}

/**
 * The distinct field roles the theme declares, each with the schemes and slots it appears under.
 *
 * Deduplicated by hex because the same surface role is usually declared in both schemes, and a
 * reader wants one row per colour rather than one per scheme.
 */
export function fieldRoles(theme) {
  const byHex = new Map()
  for (const [scheme, block] of Object.entries(theme.schemes)) {
    for (const slot of FIELD_SLOTS) {
      const resolved = block.slots[slot]
      if (!resolved) continue
      const row = byHex.get(resolved.hex) || {
        hex: resolved.hex,
        slots: [],
        schemes: [],
        line: resolved.line,
        hct: hctOf(resolved.hex)
      }
      row.slots.push(slot)
      row.schemes.push(scheme)
      byHex.set(resolved.hex, row)
    }
  }
  for (const row of byHex.values()) {
    row.slots = dedupe(row.slots)
    row.schemes = dedupe(row.schemes)
  }
  return [...byHex.values()]
}

/** The line a role is named on inside the studio record, or 1. */
function studioLine(studio, role) {
  if (!role || !role.name || !studio.text) return 1
  return lineOfFirst(studio.text, new RegExp(`"${String(role.name).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}"`))
}

/** Two role names in a `name`/`name_variant` relation: one family's own light/dark steps. */
export const isVariantOf = (a, b) => {
  if (!a || !b) return false
  const [short, long] = a.length <= b.length ? [a, b] : [b, a]
  return long.startsWith(short) && long.length > short.length
}

/** Chromatic roles grouped into hue families at `FAMILY_TOLERANCE`, in the shape rule 11 describes. */
export function hueFamilies(roles) {
  const families = []
  for (const role of [...roles].sort((left, right) => left.hct.h - right.hct.h)) {
    const family = families.find((candidate) => deltaHue(candidate.hue, role.hct.h) <= FAMILY_TOLERANCE)
    if (family) {
      family.members.push(role)
      family.hue = family.members.reduce((sum, member) => sum + member.hct.h, 0) / family.members.length
    } else {
      families.push({ hue: role.hct.h, members: [role] })
    }
  }
  return families
}

const isErrorRole = (role) => Boolean(role.name && /error/i.test(role.name))

/** `{ h, c, t }` plus the canonical hex, for a studio role whose value is a usable colour. */
const roleColour = (role) => {
  const value = String(role.value ?? '').trim()
  if (!HEX_VALUE.test(value)) return null
  const hex = toHex6(value)
  return { name: role.name, hex, hct: hctOf(hex) }
}

// ---------------------------------------------------------------------------------------------
// The checks — each returns `{ findings, summary }` or `{ unverified }`
// ---------------------------------------------------------------------------------------------

function checkDeclared(ctx) {
  const { studio, palette } = ctx
  if (!studio.present) return { unverified: `there is no ${STUDIO_REL}, so the slot declares no palette to judge` }
  if (studio.error) return { unverified: `${STUDIO_REL} could not be read as a studio record: ${studio.error.message}` }
  const findings = []
  if (palette.length === 0) {
    findings.push(
      finding(STUDIO_REL, 1, ID['palette.declared'],
        'the record declares no `palette`; a slot\'s identity names every role the renderer draws with (visual-identity.mdc)')
    )
  }
  for (const role of palette) {
    const value = String(role.value ?? '').trim()
    if (HEX_VALUE.test(value)) continue
    findings.push(
      finding(STUDIO_REL, studioLine(studio, role), ID['palette.declared'],
        `role '${role.name ?? '(unnamed)'}' has value ${JSON.stringify(role.value ?? null)}, which is not a #RGB, #RRGGBB or #AARRGGBB colour`)
    )
  }
  if (palette.length > 0 && palette.length < MIN_ROLES) {
    findings.push(
      finding(STUDIO_REL, 1, ID['palette.declared'],
        `only ${palette.length} role(s) are declared; a palette is not complete under ${MIN_ROLES} (visual-identity.mdc)`)
    )
  }
  const accent = accentOf(palette)
  const accentPart = accent
    ? `; accent candidate '${accent.role.name}' ${accent.hex} (H${round1(accent.hct.h)} C${round1(accent.hct.c)} T${round1(accent.hct.t)})`
    : '; no chromatic role stands out as the accent'
  return { findings, summary: `${palette.length} role(s) declared${accentPart}` }
}

function checkAgreement(ctx) {
  const { studio, palette, theme } = ctx
  if (!studio.present) return { unverified: `there is no ${STUDIO_REL} to compare against the app theme` }
  if (studio.error) return { unverified: `${STUDIO_REL} could not be read: ${studio.error.message}` }
  if (!theme.colorFile) {
    return { unverified: `no ${THEME_REL_HINT}/Color.kt exists, so the declared palette has no counterpart to agree with` }
  }
  if (theme.constants.length === 0) {
    return { unverified: `${relIn(ctx.dir, theme.colorFile)} declares no \`val X = Color(0x…)\` constant this sensor can parse` }
  }
  const colorSet = new Set(theme.constants.map((row) => row.hex.toUpperCase()))
  const findings = []
  let checked = 0
  for (const role of palette) {
    const value = String(role.value ?? '').trim()
    if (!HEX_VALUE.test(value)) continue
    checked += 1
    // A three-digit or eight-digit palette value is the same colour as its six-digit form, so the
    // canonical reading is compared first; the raw readings stay for the eight-digit ambiguity the
    // studio's own `#FFFFFFFF` form creates.
    const forms = [toHex6(value), ...paletteRoleSet([value.toUpperCase()])]
    if (forms.some((form) => colorSet.has(form.toUpperCase()))) continue
    findings.push(
      finding(STUDIO_REL, studioLine(studio, role), ID['palette.agreement'],
        `role '${role.name ?? '(unnamed)'}' is ${toHex6(value)} in the pack but no constant of ${relIn(ctx.dir, theme.colorFile)} carries it — the pack and the app have drifted (visual-identity.mdc: one source of truth)`)
    )
  }
  return {
    findings,
    summary: `${checked} declared role(s) matched against ${theme.constants.length} Color.kt constant(s)`
  }
}

function checkContrast(ctx) {
  const { dir, theme } = ctx
  if (!theme.themeFile) return { unverified: `no ${THEME_REL_HINT}/Theme.kt exists, so no declared scheme can be measured` }
  if (!theme.colorFile) return { unverified: `no ${THEME_REL_HINT}/Color.kt exists, so scheme roles cannot be resolved` }
  const schemeNames = Object.keys(theme.schemes)
  if (schemeNames.length === 0) {
    return { unverified: `${relIn(dir, theme.themeFile)} declares no \`lightColorScheme\`/\`darkColorScheme\` block this sensor can read` }
  }
  const themeRel = relIn(dir, theme.themeFile)
  const findings = []
  let measured = 0
  let weakest = null
  for (const missing of ['light', 'dark'].filter((name) => !theme.schemes[name])) {
    findings.push(
      finding(themeRel, 1, ID['palette.contrast'],
        `no \`${missing}ColorScheme\` is declared (rule 29): a palette is two schemes, and only one is present`)
    )
  }
  for (const [scheme, block] of Object.entries(theme.schemes)) {
    for (const [upper, lower] of CONTRAST_PAIRS) {
      const fore = block.slots[upper]
      const back = block.slots[lower]
      if (!fore || !back) continue
      const ratio = round2(contrastRatio(fore.hex, back.hex))
      measured += 1
      if (!weakest || ratio < weakest.ratio) weakest = { ratio, scheme, upper, lower }
      if (ratio >= MIN_TEXT_CONTRAST) continue
      findings.push(
        finding(themeRel, fore.line, ID['palette.contrast'],
          `${scheme} \`${upper}\` ${fore.hex} on \`${lower}\` ${back.hex} is ${ratio.toFixed(2)}:1, under the ${MIN_TEXT_CONTRAST}:1 text floor (WCAG 1.4.3 AA; rules 1, 4, 29)`)
      )
    }
  }
  const weakestPart = weakest
    ? `; weakest ${weakest.scheme} \`${weakest.upper}\`/\`${weakest.lower}\` ${weakest.ratio.toFixed(2)}:1`
    : ''
  return { findings, summary: `${measured} on-X/X pair(s) across ${schemeNames.join('+')}${weakestPart}` }
}

function checkFieldChroma(ctx) {
  const { dir, theme } = ctx
  if (!theme.themeFile) return { unverified: `no ${THEME_REL_HINT}/Theme.kt exists, so no field role can be measured` }
  if (!theme.colorFile) return { unverified: `no ${THEME_REL_HINT}/Color.kt exists, so field roles cannot be resolved` }
  const fields = fieldRoles(theme)
  if (fields.length === 0) {
    return { unverified: `${relIn(dir, theme.themeFile)} resolves no field role (${FIELD_SLOTS.join(', ')})` }
  }
  const themeRel = relIn(dir, theme.themeFile)
  const findings = []
  const worst = fields.reduce((max, row) => (row.hct.c > max.hct.c ? row : max), fields[0])
  for (const row of fields) {
    if (row.hct.c <= MAX_FIELD_CHROMA) continue
    findings.push(
      finding(themeRel, row.line, ID['palette.field-chroma'],
        `${row.schemes.join('+')} \`${row.slots.join('`/`')}\` ${row.hex} is H${round1(row.hct.h)} C${round1(row.hct.c)} T${round1(row.hct.t)}, over the C${MAX_FIELD_CHROMA} neutral bound of rule 13 — a highly chromatic field is what an accent has to compete with`)
    )
  }
  const darkFields = fields.filter((row) => row.schemes.includes('dark'))
  const darkPart = darkFields.length > 0
    ? `; dark field tones T${round1(Math.min(...darkFields.map((row) => row.hct.t)))}–T${round1(Math.max(...darkFields.map((row) => row.hct.t)))} (rule 28 wants >= 4)`
    : ''
  return {
    findings,
    summary: `highest field chroma C${round1(worst.hct.c)} at ${worst.hex}${darkPart}; bound C${MAX_FIELD_CHROMA} (rule 13, M3's neutral constant)`
  }
}

function checkHueStructure(ctx) {
  const { studio, palette, theme } = ctx
  if (!studio.present) return { unverified: `there is no ${STUDIO_REL}, so no accent is declared to place against the fields` }
  if (studio.error) return { unverified: `${STUDIO_REL} could not be read: ${studio.error.message}` }
  const accent = accentOf(palette)
  if (!accent) {
    return { unverified: `the palette declares no chromatic role (HCT chroma >= ${MAX_FIELD_CHROMA}) to treat as the accent` }
  }
  if (!theme.themeFile) return { unverified: `no ${THEME_REL_HINT}/Theme.kt exists, so the accent has no field to be placed against` }
  const fields = fieldRoles(theme)
  if (fields.length === 0) return { unverified: `${relIn(ctx.dir, theme.themeFile)} resolves no field role` }
  const chromatic = fields.filter((row) => row.hct.c >= MAX_FIELD_CHROMA)
  const accentName = accent.role.name ?? '(unnamed)'
  if (chromatic.length === 0) {
    return {
      findings: [],
      summary: `accent '${accentName}' H${round1(accent.hct.h)} faces ${fields.length} field role(s), all neutral (C < ${MAX_FIELD_CHROMA}), so rule 8 has no field hue to be too close to`
    }
  }
  const themeRel = relIn(ctx.dir, theme.themeFile)
  const findings = []
  let closest = null
  for (const row of chromatic) {
    const hueGap = round1(deltaHue(accent.hct.h, row.hct.h))
    const toneGap = round1(Math.abs(accent.hct.t - row.hct.t))
    const chromaRatio = row.hct.c <= 0 ? Infinity : accent.hct.c / row.hct.c
    if (!closest || hueGap < closest.hueGap) closest = { hueGap, row }
    if (hueGap >= ACCENT_HUE_OFFSET) continue
    if (toneGap >= ACCENT_TONE_OFFSET && chromaRatio >= ACCENT_CHROMA_RATIO) continue
    const ratioText = Number.isFinite(chromaRatio) ? round2(chromaRatio).toFixed(2) : 'n/a'
    findings.push(
      finding(themeRel, row.line, ID['palette.hue-structure'],
        `${row.schemes.join('+')} field \`${row.slots.join('`/`')}\` ${row.hex} sits ${hueGap}° from accent '${accentName}' ${accent.hex} (H${round1(accent.hct.h)}) at a tone gap of ${toneGap} and a chroma ratio of ${ratioText} — under the ${ACCENT_HUE_OFFSET}° offset and outside the same-hue escape of >= ${ACCENT_TONE_OFFSET} tone and >= ${ACCENT_CHROMA_RATIO}x chroma (rule 8, INFERRED: the document reads M3's 30° family step as needing more for an accent)`)
    )
  }
  return {
    findings,
    summary: `accent '${accentName}' H${round1(accent.hct.h)} vs ${chromatic.length} chromatic field role(s); closest ${closest.hueGap}° at ${closest.row.hex}`
  }
}

function checkAccentChroma(ctx) {
  const { studio, palette, theme } = ctx
  if (!studio.present) return { unverified: `there is no ${STUDIO_REL}, so no accent is declared to measure` }
  if (studio.error) return { unverified: `${STUDIO_REL} could not be read: ${studio.error.message}` }
  const accent = accentOf(palette)
  if (!accent) {
    return { unverified: `the palette declares no chromatic role (HCT chroma >= ${MAX_FIELD_CHROMA}) to treat as the accent` }
  }
  if (!theme.themeFile || !theme.colorFile) {
    return { unverified: `the app theme could not be read (${THEME_REL_HINT}/{Color,Theme}.kt), so it is unknown whether the palette has a chromatic field` }
  }
  const fields = fieldRoles(theme)
  if (fields.length === 0) return { unverified: `${relIn(ctx.dir, theme.themeFile)} resolves no field role` }
  const chromatic = fields.filter((row) => row.hct.c >= MAX_FIELD_CHROMA)

  // Rule 15's vendor reference: the chroma Material 3's own TonalSpot derives for a
  // `primaryContainer` at this hue and tone 90.
  const reference = hctOf(hctFrom(accent.hct.h, M3_ACCENT_CHROMA, M3_CONTAINER_TONE))
  const ratio = reference.c <= 0 ? Infinity : accent.hct.c / reference.c
  const bound = round1(M3_CONTAINER_FACTOR * reference.c)
  const headroom = round1(chromaHeadroom(accent.hex))
  const accentName = accent.role.name ?? '(unnamed)'
  const ratioText = Number.isFinite(ratio) ? round2(ratio).toFixed(2) : 'n/a'
  const measured =
    `accent '${accentName}' ${accent.hex} is C${round1(accent.hct.c)} = ${ratioText}x ` +
    `the C${round1(reference.c)} Material 3 derives at H${round1(accent.hct.h)} tone ${M3_CONTAINER_TONE} (bound ${round1(M3_CONTAINER_FACTOR)}x = C${bound}); ` +
    `headroom ${headroom}% of its sRGB maximum chroma (rule 12, reported)`

  if (chromatic.length === 0) {
    return {
      findings: [],
      summary: `${measured}; not gated, because every field role is neutral (C < ${MAX_FIELD_CHROMA}) — the document's own section 2.3 scopes the bound to a chromatic field, where the accent has a chroma budget to compete for`
    }
  }
  const findings = []
  if (ratio > M3_CONTAINER_FACTOR) {
    findings.push(
      finding(STUDIO_REL, studioLine(studio, accent.role), ID['palette.accent-chroma'],
        `${measured} — over the ${round1(M3_CONTAINER_FACTOR)}x bound of rule 15, the one vendor algorithm in the specification disagreeing with the palette`)
    )
  }
  return { findings, summary: `${measured}; gated against a chromatic field (${chromatic.map((row) => row.hex).join(', ')})` }
}

function checkFamilies(ctx) {
  const { studio, palette } = ctx
  if (!studio.present) return { unverified: `there is no ${STUDIO_REL}, so no hue families are declared` }
  if (studio.error) return { unverified: `${STUDIO_REL} could not be read: ${studio.error.message}` }
  const rows = palette.map(roleColour).filter(Boolean)
  if (rows.length === 0) return { unverified: 'the studio record declares no usable palette role (a #RGB, #RRGGBB or #AARRGGBB value)' }
  const chromatic = rows.filter((row) => row.hct.c >= FAMILY_CHROMA_FLOOR)
  const families = hueFamilies(chromatic.filter((row) => !isErrorRole(row)))
  const errors = chromatic.filter(isErrorRole)
  const findings = []
  if (families.length > MAX_FAMILIES) {
    findings.push(
      finding(STUDIO_REL, 1, ID['palette.families'],
        `${families.length} chromatic hue families (${families.map((family) => `H${round1(family.hue)}: ${family.members.map((member) => member.name ?? member.hex).join('+')}`).join(' | ')}) exceed the ${MAX_FAMILIES} of rule 25 (HCT chroma >= ${FAMILY_CHROMA_FLOOR} is chromatic; families merge within ${FAMILY_TOLERANCE}°)`)
    )
  }
  const spread = families.filter((family) => {
    if (family.members.length < 2) return false
    const hues = family.members.map((member) => member.hct.h)
    return Math.max(...hues) - Math.min(...hues) > 90
  })
  const spreadPart = spread.length > 0
    ? `; rule 20 (a family wider than 90° is not a family) flags ${spread.map((family) => family.members.map((member) => member.name ?? member.hex).join('+')).join(', ')} — reported, not gated`
    : ''
  const familyPart = families.map((family) => `H${round1(family.hue)}`).join('+') || 'none'
  return {
    findings,
    summary: `${families.length} chromatic family(ies) ${familyPart} plus ${errors.length} error role(s)${spreadPart}; bound ${MAX_FAMILIES} (rules 11, 25)`
  }
}

function checkDuplicate(ctx) {
  const { studio, palette } = ctx
  if (!studio.present) return { unverified: `there is no ${STUDIO_REL}, so no roles can be compared with each other` }
  if (studio.error) return { unverified: `${STUDIO_REL} could not be read: ${studio.error.message}` }
  const rows = palette.map(roleColour).filter(Boolean)
  if (rows.length < 2) {
    return { unverified: 'the studio record declares fewer than two usable roles, so nothing can be indistinguishable' }
  }
  const findings = []
  let skipped = 0
  let compared = 0
  for (let i = 0; i < rows.length; i += 1) {
    for (let j = i + 1; j < rows.length; j += 1) {
      const left = rows[i]
      const right = rows[j]
      // A role and its own named variants are one another's steps by construction (`ink`/`ink_soft`/
      // `ink_lift` is a declared three-stop gradient), so rule 21's "two roles a user must tell apart"
      // does not apply inside a named family. Excluding them by name is a scope, not a threshold
      // change: the 1.2:1 / 15° / C20 bounds below are the document's, unmodified.
      if (isVariantOf(left.name, right.name)) {
        skipped += 1
        continue
      }
      compared += 1
      const ratio = round2(contrastRatio(left.hex, right.hex))
      const hueGap = round1(deltaHue(left.hct.h, right.hct.h))
      const chromaGap = round1(Math.abs(left.hct.c - right.hct.c))
      if (ratio >= DUPLICATE_CONTRAST || hueGap >= DUPLICATE_HUE || chromaGap >= DUPLICATE_CHROMA) continue
      const lower = left.hct.c <= right.hct.c ? left : right
      findings.push(
        finding(STUDIO_REL, studioLine(studio, lower), ID['palette.duplicate'],
          `roles '${left.name ?? left.hex}' ${left.hex} and '${right.name ?? right.hex}' ${right.hex} differ by ${ratio.toFixed(3)}:1 contrast, ${hueGap}° hue and C${chromaGap}, under all three of rule 21's bounds (${DUPLICATE_CONTRAST}:1, ${DUPLICATE_HUE}°, C${DUPLICATE_CHROMA}) — a user cannot tell them apart, and the document's own example is \`paper\`/\`amber_pale\` at 1.006:1`)
      )
    }
  }
  return {
    findings,
    summary: `${compared} cross-family pair(s) compared, ${skipped} skipped as named variants; bounds ${DUPLICATE_CONTRAST}:1 / ${DUPLICATE_HUE}° / C${DUPLICATE_CHROMA} (rule 21)`
  }
}

function checkDislike(ctx) {
  const { studio, palette } = ctx
  if (!studio.present) return { unverified: `there is no ${STUDIO_REL}, so no role can be tested against Material 3's dislike volume` }
  if (studio.error) return { unverified: `${STUDIO_REL} could not be read: ${studio.error.message}` }
  const rows = palette.map(roleColour).filter(Boolean)
  if (rows.length === 0) return { unverified: 'the studio record declares no usable palette role (a #RGB, #RRGGBB or #AARRGGBB value)' }
  const findings = []
  for (const row of rows) {
    if (!isDislikedHct(row.hct)) continue
    findings.push(
      finding(STUDIO_REL, studioLine(studio, row), ID['palette.dislike'],
        `role '${row.name ?? '(unnamed)'}' ${row.hex} is H${round1(row.hct.h)} C${round1(row.hct.c)} T${round1(row.hct.t)}, inside Material 3's \`disliked\` volume (H90–111, C>16, T<65) — the vendor's own model of a colour nobody likes (rule 16)`)
    )
  }
  return {
    findings,
    summary: `${rows.length} role(s) tested against the H90–111 / C>16 / T<65 dislike volume (rule 16)`
  }
}

const CHECKS = Object.freeze([
  [ID['palette.declared'], checkDeclared],
  [ID['palette.agreement'], checkAgreement],
  [ID['palette.contrast'], checkContrast],
  [ID['palette.field-chroma'], checkFieldChroma],
  [ID['palette.hue-structure'], checkHueStructure],
  [ID['palette.accent-chroma'], checkAccentChroma],
  [ID['palette.families'], checkFamilies],
  [ID['palette.duplicate'], checkDuplicate],
  [ID['palette.dislike'], checkDislike]
])

// ---------------------------------------------------------------------------------------------
// The run-time entry point
// ---------------------------------------------------------------------------------------------

/**
 * Every leg's raw answer for one slot.
 *
 * A leg that throws is reported as UNVERIFIED for that leg alone, so one unreadable file cannot take
 * the other eight measurements down with it — the defect `paletteFindings` must never reproduce.
 */
export function paletteProblems(slotDir) {
  const dir = typeof slotDir === 'string' && slotDir.length > 0 ? slotDir : String(slotDir ?? '')
  const studio = readStudio(dir)
  const palette = declaredPalette(studio.record)
  const theme = readTheme(dir)
  const ctx = { dir, studio, palette, theme }
  const byId = {}
  const summaries = {}
  const unverifiedById = {}
  const findings = []
  for (const [id, run] of CHECKS) {
    try {
      const result = run(ctx) || {}
      if (result.unverified) {
        unverifiedById[id] = result.unverified
        continue
      }
      byId[id] = Array.isArray(result.findings) ? result.findings : []
      summaries[id] = typeof result.summary === 'string' && result.summary.length > 0 ? result.summary : 'evaluated'
      findings.push(...byId[id])
    } catch (error) {
      unverifiedById[id] = `the check could not evaluate ${dir || 'the slot'}: ${error.message}`
    }
  }
  return { byId, summaries, unverified: unverifiedById, notes: [...PALETTE_NOTES], findings }
}

/**
 * The clean `file:line` findings, and it never throws — a malformed palette, a missing studio record
 * and an unparsable `Color.kt` all degrade to a finding or to an UNVERIFIED verdict, never to an
 * exception, because a sensor that throws tells the operator nothing about the slot.
 */
export function paletteFindings(slotDir) {
  try {
    return paletteProblems(slotDir).findings
  } catch (error) {
    return [`palette.findings: UNVERIFIED: the palette sensor could not evaluate ${relRepo(String(slotDir))}: ${error.message}`]
  }
}

/**
 * Which rules each row actually gates, so a row cannot be read as covering more than it does.
 *
 * The full rationale for every rule this module does *not* turn into a verdict lives in
 * `PALETTE_NOTES` and is printed once, in the `palette.declared` row; repeating it on all nine rows
 * made the report unreadable, and an unreadable report is how "the gate is green" starts meaning
 * less than it says.
 */
const ROW_RULES = Object.freeze({
  [ID['palette.declared']]: 'gates rule 22 (the record exists and names a role) and reports rule 2',
  [ID['palette.agreement']]: 'gates rule 26 (the pack and the app are one source of truth)',
  [ID['palette.contrast']]: 'gates rules 1/4/29 (WCAG 1.4.3 AA, 4.5:1 text floor)',
  [ID['palette.field-chroma']]: 'gates rule 13 (field chroma <= C12); reports rule 28',
  [ID['palette.hue-structure']]: 'gates rule 8 (accent hue offset >= 40 degrees), INFERRED threshold',
  [ID['palette.accent-chroma']]: 'gates rule 15 (accent chroma <= 1.5x the M3 ton-90 reference); reports rule 12',
  [ID['palette.families']]: 'gates rules 11/25 (<= 3 chromatic hue families); reports rule 20',
  [ID['palette.duplicate']]: 'gates rule 21 (1.2:1 / 15 degrees / C20)',
  [ID['palette.dislike']]: 'gates rule 16 (the H90-111 / C>16 / T<65 dislike volume)'
})

const withNotes = (id, summary, notes) => {
  const scope = ROW_RULES[id] || 'scope not stated'
  if (id === ID['palette.declared'] && notes.length > 0) return `${summary}; ${scope}; ${notes.join('; ')}`
  return `${summary}; ${scope}; every rule this row does not gate is named in the palette.declared row`
}

function verdictFor(id, slotDir, problems) {
  const reason = problems.unverified[id]
  if (reason) return unverified(id, reason)
  return baselineVerdict(id, slotDir, problems.byId[id] || [], withNotes(id, problems.summaries[id] || 'evaluated', problems.notes))
}

/** The nine slot facts, as baseline-scoped verdicts, registered in `scripts/core/index.mjs`. */
export function paletteChecks(slotDir) {
  let problems
  try {
    problems = paletteProblems(slotDir)
  } catch (error) {
    return PALETTE_IDS.map((id) => unverified(id, `could not evaluate: ${error.message}`))
  }
  return PALETTE_IDS.map((id) => guard(id, () => verdictFor(id, slotDir, problems)))
}

// ---------------------------------------------------------------------------------------------
// Inline fixtures — every leg, driven in red and in green
// ---------------------------------------------------------------------------------------------

function withTempDir(body) {
  const dir = mkdtempSync(path.join(SCRATCH_DIR, 'kiln-palette-'))
  try {
    return body(dir)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
}

/**
 * The marker that makes a synthetic tree a product slot.
 *
 * `baselineApplies()` decides a directory is a gateable app by the presence of `economics.json`, so a
 * temp slot without it would print every finding as a grandfathered note inside a PASS and no fixture
 * could ever fail. Writing it points the fixtures at the production path instead of a parallel,
 * weaker one; this module never parses its contents.
 */
const PRODUCT_MARKER = '{"schemaVersion": 1, "archetype": "purchase_led_single_unlock"}'

/** A slot tree written to a scratch temp directory, so the disk driver itself is exercised. */
function withTempSlot(files, body) {
  return withTempDir((dir) => {
    for (const [rel, contents] of Object.entries({ 'economics.json': PRODUCT_MARKER, ...files })) {
      if (contents === null) continue
      const file = path.join(dir, ...rel.split('/'))
      mkdirSync(path.dirname(file), { recursive: true })
      writeFileSync(file, contents, 'utf8')
    }
    return body(dir)
  })
}

/** One id's verdict from a synthetic slot. */
function slotVerdict(id, files) {
  return withTempSlot(files, (dir) => paletteChecks(dir).find((row) => row.id === id))
}

function expectSlotVerdict(id, files, verdict, phrase) {
  const result = slotVerdict(id, files)
  expectTrue(result !== undefined, `${id} is absent from the report`)
  expectEqual(result.verdict, verdict, `verdict of ${id}`)
  if (phrase) {
    expectTrue(String(result.detail).includes(phrase), `detail "${result.detail}" does not name ${phrase}`)
  }
  return result
}

const COLOR_KT_REL = 'app/src/main/java/com/gexetr/demo/ui/theme/Color.kt'
const THEME_KT_REL = 'app/src/main/java/com/gexetr/demo/ui/theme/Theme.kt'

/** A `Color.kt` from `name -> 0xAARRGGBB` digits; the same file `colourConstants` parses back. */
function colorKt(constants) {
  const body = Object.entries(constants).map(([name, digits]) => `internal val ${name} = Color(0x${digits})`)
  return ['package com.gexetr.demo.ui.theme', '', 'import androidx.compose.ui.graphics.Color', '', ...body, ''].join('\n')
}

/** A `Theme.kt` from two scheme blocks of `slot -> constant name`. */
function themeKt(light, dark) {
  const block = (fn, label, entries) =>
    [`private val ${label} = ${fn}(`, ...Object.entries(entries).map(([slot, name]) => `    ${slot} = ${name},`), ')'].join('\n')
  return [
    'package com.gexetr.demo.ui.theme',
    '',
    'import androidx.compose.material3.darkColorScheme',
    'import androidx.compose.material3.lightColorScheme',
    '',
    block('lightColorScheme', 'Light', light),
    '',
    block('darkColorScheme', 'Dark', dark),
    ''
  ].join('\n')
}

/**
 * A palette and theme whose verdict is known clean: two hue families, neutral fields, every text pair
 * above 4.5:1, no role in the dislike volume and no pair indistinguishable.
 */
const CLEAN_CONSTANTS = {
  Ink: 'FF1B1B22',
  InkSoft: 'FF2A2A33',
  Paper: 'FFF7F7FB',
  Accent: 'FF4F46E5',
  AccentDeep: 'FF3730A3',
  Amber: 'FFD97706',
  AmberDeep: 'FF92400E',
  White: 'FFFFFFFF',
  AccentPale: 'FFE0E7FF',
  IndigoDeep: 'FF1E1B4B',
  IndigoPale: 'FFE0E7FF',
  TealDeep: 'FF115E59',
  TealPale: 'FFCCFBF1',
  TealInk: 'FF042F2E',
  AmberPale: 'FFFEF3C7',
  AmberInk: 'FF451A03',
  ErrorDeep: 'FFB91C1C',
  ErrorPale: 'FFFEE2E2',
  ErrorInk: 'FF450A0A',
  SurfaceVariant: 'FFF2F2F8',
  SurfaceHigh: 'FFECECF2',
  MidSurface: 'FF34343F',
  Outline: 'FFB9B9C6',
  OutlineDark: 'FF8A8A99',
  TealInline: 'FF5EEAD4',
  TealContainerDark: 'FF115E59',
  AmberInline: 'FFFCD34D',
  AmberContainerDark: 'FF92400E',
  ErrorInline: 'FFFCA5A5',
  ErrorContainerDark: 'FF7F1D1D',
  AccentInline: 'FFA5B4FC'
}

const CLEAN_STUDIO = {
  schemaVersion: 1,
  mark: 'demo',
  palette: [
    { role: 'ink', value: '#1B1B22' },
    { role: 'ink_soft', value: '#2A2A33' },
    { role: 'paper', value: '#F7F7FB' },
    { role: 'accent', value: '#4F46E5' },
    { role: 'accent_deep', value: '#3730A3' },
    { role: 'amber', value: '#D97706' },
    { role: 'amber_deep', value: '#92400E' }
  ]
}

const CLEAN_LIGHT = {
  primary: 'Accent', onPrimary: 'White', primaryContainer: 'AccentPale', onPrimaryContainer: 'IndigoDeep',
  secondary: 'TealDeep', onSecondary: 'White', secondaryContainer: 'TealPale', onSecondaryContainer: 'TealInk',
  tertiary: 'AmberDeep', onTertiary: 'White', tertiaryContainer: 'AmberPale', onTertiaryContainer: 'AmberInk',
  error: 'ErrorDeep', onError: 'White', errorContainer: 'ErrorPale', onErrorContainer: 'ErrorInk',
  background: 'Paper', onBackground: 'Ink', surface: 'White', onSurface: 'Ink',
  surfaceVariant: 'SurfaceVariant', onSurfaceVariant: 'Ink', surfaceContainer: 'SurfaceVariant',
  surfaceContainerHigh: 'SurfaceHigh', outline: 'Outline',
  inverseSurface: 'InkSoft', inverseOnSurface: 'SurfaceVariant'
}

const CLEAN_DARK = {
  primary: 'AccentInline', onPrimary: 'IndigoDeep', primaryContainer: 'AccentDeep', onPrimaryContainer: 'IndigoPale',
  secondary: 'TealInline', onSecondary: 'TealInk', secondaryContainer: 'TealContainerDark', onSecondaryContainer: 'TealPale',
  tertiary: 'AmberInline', onTertiary: 'AmberInk', tertiaryContainer: 'AmberContainerDark', onTertiaryContainer: 'AmberPale',
  error: 'ErrorInline', onError: 'ErrorInk', errorContainer: 'ErrorContainerDark', onErrorContainer: 'ErrorPale',
  background: 'Ink', onBackground: 'Paper', surface: 'InkSoft', onSurface: 'SurfaceVariant',
  surfaceVariant: 'MidSurface', onSurfaceVariant: 'Outline', outline: 'OutlineDark',
  inverseSurface: 'Paper', inverseOnSurface: 'Ink'
}

/** The clean tree, with any part overridden — the shape every fixture below is written against. */
function cleanFiles({ studio = CLEAN_STUDIO, constants = CLEAN_CONSTANTS, light = CLEAN_LIGHT, dark = CLEAN_DARK, themeText = null } = {}) {
  return {
    [STUDIO_REL]: typeof studio === 'string' ? studio : JSON.stringify(studio, null, 2),
    [COLOR_KT_REL]: constants ? colorKt(constants) : null,
    [THEME_KT_REL]: themeText !== null ? themeText : (light || dark ? themeKt(light || {}, dark || {}) : null)
  }
}

/**
 * The fixtures: a red and a green for every leg, plus the degradation cases and the baseline.
 *
 * Every `FAIL` row below is the assertion that the leg can fail — `verification-discipline.mdc`'s "a
 * check that cannot fail is not a check" — and the mutation proof in the handoff neuters one
 * predicate and shows the matching row here going red.
 */
export function paletteFixtures() {
  return runFixtures('palette.fixtures', [
    // ---- palette.declared: the palette is declared and well formed ------------------------------
    ['declared: a clean palette passes', () => {
      expectSlotVerdict(ID['palette.declared'], cleanFiles(), 'PASS', '7 role(s) declared')
    }],
    ['declared: no studio.json is UNVERIFIED', () => {
      expectSlotVerdict(ID['palette.declared'], { ...cleanFiles(), [STUDIO_REL]: null }, 'UNVERIFIED', 'no store/studio.json')
    }],
    ['declared: an unparsable studio.json is UNVERIFIED, not a throw', () => {
      expectSlotVerdict(ID['palette.declared'], cleanFiles({ studio: '{ "schemaVersion": 1,' }), 'UNVERIFIED', 'could not be read')
    }],
    ['declared: a value that is not #RRGGBB fails', () => {
      const studio = { ...CLEAN_STUDIO, palette: [...CLEAN_STUDIO.palette, { role: 'glow', value: '#12' }] }
      expectSlotVerdict(ID['palette.declared'], cleanFiles({ studio }), 'FAIL', "role 'glow'")
    }],
    ['declared: a non-string value fails without throwing', () => {
      const studio = { ...CLEAN_STUDIO, palette: [...CLEAN_STUDIO.palette, { role: 'glow', value: 42 }] }
      expectSlotVerdict(ID['palette.declared'], cleanFiles({ studio }), 'FAIL', 'not a #RGB')
    }],
    ['declared: a #AARRGGBB value is accepted, as the studio emits it', () => {
      const studio = { ...CLEAN_STUDIO, palette: [...CLEAN_STUDIO.palette, { role: 'ink_deep', value: '#FF2A2568' }] }
      expectSlotVerdict(ID['palette.declared'], cleanFiles({ studio }), 'PASS', '8 role(s) declared')
    }],
    ['declared: a palette with too few roles fails', () => {
      const studio = { ...CLEAN_STUDIO, palette: [{ role: 'ink', value: '#1B1B22' }] }
      expectSlotVerdict(ID['palette.declared'], cleanFiles({ studio }), 'FAIL', 'only 1 role(s)')
    }],
    ['declared: a record with no palette array fails', () => {
      expectSlotVerdict(ID['palette.declared'], cleanFiles({ studio: { schemaVersion: 1, mark: 'demo' } }), 'FAIL', 'declares no')
    }],
    ['declared: an object-map palette is read, not reported as undeclared', () => {
      const studio = { schemaVersion: 1, palette: { ink: '#1B1B22', ink_soft: '#2A2A33', paper: '#F7F7FB', accent: '#4F46E5' } }
      expectSlotVerdict(ID['palette.declared'], cleanFiles({ studio }), 'PASS', '4 role(s) declared')
    }],

    // ---- palette.agreement: the pack and the app are one source of truth ------------------------
    ['agreement: a palette that is present in Color.kt passes', () => {
      expectSlotVerdict(ID['palette.agreement'], cleanFiles(), 'PASS', 'matched against')
    }],
    ['agreement: a declared role missing from Color.kt fails (the reference slot\'s drift)', () => {
      const studio = { ...CLEAN_STUDIO, palette: [...CLEAN_STUDIO.palette, { role: 'drifted', value: '#2A2568' }] }
      expectSlotVerdict(ID['palette.agreement'], cleanFiles({ studio }), 'FAIL', "role 'drifted'")
    }],
    ['agreement: an #AARRGGBB palette value matches its six-digit constant', () => {
      const studio = { ...CLEAN_STUDIO, palette: [...CLEAN_STUDIO.palette, { role: 'ink_deep', value: '#FF1B1B22' }] }
      expectSlotVerdict(ID['palette.agreement'], cleanFiles({ studio }), 'PASS', 'matched against')
    }],
    ['agreement: no Color.kt is UNVERIFIED', () => {
      expectSlotVerdict(ID['palette.agreement'], cleanFiles({ constants: null }), 'UNVERIFIED', 'no app/src/main/java')
    }],
    ['agreement: a Color.kt with no parseable constant is UNVERIFIED, not a false drift', () => {
      const files = { ...cleanFiles(), [COLOR_KT_REL]: 'package com.gexetr.demo.ui.theme\n\n// nothing here\n' }
      expectSlotVerdict(ID['palette.agreement'], files, 'UNVERIFIED', 'declares no')
    }],
    ['agreement: a Color.kt with garbage text does not throw', () => {
      const files = { ...cleanFiles(), [COLOR_KT_REL]: ' binary\u0000noise\n' }
      const row = slotVerdict(ID['palette.agreement'], files)
      expectTrue(row !== undefined, 'agreement is absent from the report')
      expectTrue(['UNVERIFIED', 'FAIL', 'PASS'].includes(row.verdict), `unexpected verdict ${row.verdict}`)
    }],

    // ---- palette.contrast: WCAG 1.4.3 on every declared on-X/X pair -----------------------------
    ['contrast: a clean pair set passes', () => {
      expectSlotVerdict(ID['palette.contrast'], cleanFiles(), 'PASS', 'on-X/X pair(s)')
    }],
    ['contrast: white on a mid teal fails (the reference slot\'s onSecondary)', () => {
      const light = { ...CLEAN_LIGHT, secondary: 'TealMid', onSecondary: 'White' }
      const constants = { ...CLEAN_CONSTANTS, TealMid: 'FF0D9488' }
      expectSlotVerdict(ID['palette.contrast'], cleanFiles({ light, constants }), 'FAIL', 'under the 4.5:1 text floor')
    }],
    ['contrast: a single scheme fails rule 29', () => {
      const onlyLight = [
        'package com.gexetr.demo.ui.theme',
        '',
        'import androidx.compose.material3.lightColorScheme',
        '',
        'private val Light = lightColorScheme(',
        ...Object.entries(CLEAN_LIGHT).map(([slot, name]) => `    ${slot} = ${name},`),
        ')',
        ''
      ].join('\n')
      expectSlotVerdict(ID['palette.contrast'], cleanFiles({ themeText: onlyLight }), 'FAIL', 'no `darkColorScheme` is declared')
    }],
    ['contrast: no Theme.kt is UNVERIFIED', () => {
      expectSlotVerdict(ID['palette.contrast'], cleanFiles({ light: null, dark: null }), 'UNVERIFIED', 'no app/src/main/java')
    }],
    ['contrast: a Theme.kt with no scheme block is UNVERIFIED', () => {
      const files = { ...cleanFiles(), [THEME_KT_REL]: 'package com.gexetr.demo.ui.theme\n\nval x = 1\n' }
      expectSlotVerdict(ID['palette.contrast'], files, 'UNVERIFIED', 'declares no')
    }],

    // ---- palette.field-chroma: rule 13, the field's own chroma ----------------------------------
    ['field-chroma: neutral fields pass', () => {
      expectSlotVerdict(ID['palette.field-chroma'], cleanFiles(), 'PASS', 'bound C12')
    }],
    ['field-chroma: a green field over the neutral bound fails (the offending slot)', () => {
      const constants = { ...CLEAN_CONSTANTS, TurfDeep: 'FF12301C' }
      expectSlotVerdict(ID['palette.field-chroma'], cleanFiles({ dark: { ...CLEAN_DARK, background: 'TurfDeep' }, constants }), 'FAIL', 'over the C12 neutral bound')
    }],
    ['field-chroma: no Theme.kt is UNVERIFIED', () => {
      expectSlotVerdict(ID['palette.field-chroma'], cleanFiles({ light: null, dark: null }), 'UNVERIFIED', 'no app/src/main/java')
    }],

    // ---- palette.hue-structure: rule 8, the accent against each chromatic field ------------------
    ['hue-structure: neutral fields leave rule 8 nothing to measure, and pass', () => {
      expectSlotVerdict(ID['palette.hue-structure'], cleanFiles(), 'PASS', 'all neutral')
    }],
    ['hue-structure: an accent 20° from a chromatic field fails (the offending palette)', () => {
      const studio = { ...CLEAN_STUDIO, palette: [{ role: 'turf', value: '#12301C' }, { role: 'lime', value: '#C9F04A' }] }
      const constants = { ...CLEAN_CONSTANTS, TurfWash: 'FFDCEBD3' }
      expectSlotVerdict(ID['palette.hue-structure'], cleanFiles({ studio, constants, light: { ...CLEAN_LIGHT, surfaceVariant: 'TurfWash' } }), 'FAIL', 'rule 8')
    }],
    ['hue-structure: an accent 136° from a chromatic field passes', () => {
      const constants = { ...CLEAN_CONSTANTS, TurfWash: 'FFDCEBD3' }
      expectSlotVerdict(ID['palette.hue-structure'], cleanFiles({ constants, light: { ...CLEAN_LIGHT, surfaceVariant: 'TurfWash' } }), 'PASS', 'accent')
    }],
    ['hue-structure: no studio.json is UNVERIFIED', () => {
      expectSlotVerdict(ID['palette.hue-structure'], { ...cleanFiles(), [STUDIO_REL]: null }, 'UNVERIFIED', 'no store/studio.json')
    }],

    // ---- palette.accent-chroma: rule 15, the vendor algorithm disagreeing ------------------------
    ['accent-chroma: a chromatic accent over neutral fields is measured, not gated', () => {
      expectSlotVerdict(ID['palette.accent-chroma'], cleanFiles(), 'PASS', 'not gated')
    }],
    ['accent-chroma: the offending lime accent fails against a chromatic field', () => {
      const studio = { ...CLEAN_STUDIO, palette: [{ role: 'turf', value: '#12301C' }, { role: 'lime', value: '#C9F04A' }] }
      const constants = { ...CLEAN_CONSTANTS, TurfWash: 'FFDCEBD3' }
      expectSlotVerdict(ID['palette.accent-chroma'], cleanFiles({ studio, constants, light: { ...CLEAN_LIGHT, surfaceVariant: 'TurfWash' } }), 'FAIL', 'rule 15')
    }],
    ['accent-chroma: no accent role is UNVERIFIED', () => {
      const studio = { ...CLEAN_STUDIO, palette: [{ role: 'ink', value: '#1B1B22' }, { role: 'paper', value: '#F7F7FB' }] }
      expectSlotVerdict(ID['palette.accent-chroma'], cleanFiles({ studio }), 'UNVERIFIED', 'no chromatic role')
    }],

    // ---- palette.families: rules 11 and 25, chromatic hue families -------------------------------
    ['families: a two-family palette passes', () => {
      expectSlotVerdict(ID['palette.families'], cleanFiles(), 'PASS', 'bound 3')
    }],
    ['families: four chromatic families fail', () => {
      const studio = { ...CLEAN_STUDIO, palette: [
        { role: 'red', value: '#B91C1C' },
        { role: 'amber', value: '#D97706' },
        { role: 'green', value: '#047857' },
        { role: 'indigo', value: '#4F46E5' }
      ] }
      expectSlotVerdict(ID['palette.families'], cleanFiles({ studio }), 'FAIL', 'exceed the 3')
    }],
    ['families: an error role is not counted as a family', () => {
      const studio = { ...CLEAN_STUDIO, palette: [
        ...CLEAN_STUDIO.palette,
        { role: 'error', value: '#B91C1C' },
        { role: 'error_deep', value: '#7F1D1D' }
      ] }
      expectSlotVerdict(ID['palette.families'], cleanFiles({ studio }), 'PASS', 'error role(s)')
    }],
    ['families: no studio.json is UNVERIFIED', () => {
      expectSlotVerdict(ID['palette.families'], { ...cleanFiles(), [STUDIO_REL]: null }, 'UNVERIFIED', 'no store/studio.json')
    }],

    // ---- palette.duplicate: rule 21, two roles a user cannot tell apart -------------------------
    ['duplicate: a clean palette passes', () => {
      expectSlotVerdict(ID['palette.duplicate'], cleanFiles(), 'PASS', 'rule 21')
    }],
    ['duplicate: paper and amber_pale at 1.006:1 fail (the document\'s own example)', () => {
      const studio = { ...CLEAN_STUDIO, palette: [
        ...CLEAN_STUDIO.palette,
        { role: 'paper', value: '#F6F4E4' },
        { role: 'amber_pale', value: '#EAF9C6' }
      ] }
      expectSlotVerdict(ID['palette.duplicate'], cleanFiles({ studio }), 'FAIL', 'differ by 1.010:1 contrast')
    }],
    ['duplicate: a role and its own named variant are one family, not a collision', () => {
      const studio = { ...CLEAN_STUDIO, palette: [
        { role: 'ink', value: '#1B1B22' },
        { role: 'ink_soft', value: '#1F1F26' },
        { role: 'paper', value: '#F7F7FB' },
        { role: 'accent', value: '#4F46E5' }
      ] }
      expectSlotVerdict(ID['palette.duplicate'], cleanFiles({ studio }), 'PASS', 'skipped as named variants')
    }],
    ['duplicate: one usable role is UNVERIFIED', () => {
      const studio = { ...CLEAN_STUDIO, palette: [{ role: 'ink', value: '#1B1B22' }] }
      expectSlotVerdict(ID['palette.duplicate'], cleanFiles({ studio }), 'UNVERIFIED', 'fewer than two')
    }],

    // ---- palette.dislike: rule 16, Material 3's own dislike volume -------------------------------
    ['dislike: a clean palette passes', () => {
      expectSlotVerdict(ID['palette.dislike'], cleanFiles(), 'PASS', 'dislike volume')
    }],
    ['dislike: an olive at H100 C40 T45 fails', () => {
      const studio = { ...CLEAN_STUDIO, palette: [...CLEAN_STUDIO.palette, { role: 'olive', value: '#7A6A0D' }] }
      expectSlotVerdict(ID['palette.dislike'], cleanFiles({ studio }), 'FAIL', "role 'olive' #7A6A0D")
    }],
    ['dislike: no studio.json is UNVERIFIED', () => {
      expectSlotVerdict(ID['palette.dislike'], { ...cleanFiles(), [STUDIO_REL]: null }, 'UNVERIFIED', 'no store/studio.json')
    }],
    ['dislike: an #AARRGGBB value is read as its colour, not degraded to UNVERIFIED', () => {
      const studio = { ...CLEAN_STUDIO, palette: [...CLEAN_STUDIO.palette, { role: 'olive', value: '#FF7A6A0D' }] }
      expectSlotVerdict(ID['palette.dislike'], cleanFiles({ studio }), 'FAIL', "role 'olive' #7A6A0D")
    }],
    ['dislike: a three-digit value is read as its colour, not rejected as malformed', () => {
      const studio = { ...CLEAN_STUDIO, palette: [...CLEAN_STUDIO.palette, { role: 'chalk', value: '#FFF' }] }
      expectSlotVerdict(ID['palette.dislike'], cleanFiles({ studio }), 'PASS', '8 role(s) tested')
    }],

    // ---- the baseline, and the never-throw guarantee ---------------------------------------------
    ['baseline: a temp slot without economics.json is noted, not gated', () => {
      const studio = { ...CLEAN_STUDIO, palette: [...CLEAN_STUDIO.palette, { role: 'drifted', value: '#2A2568' }] }
      const row = withTempSlot({ ...cleanFiles({ studio }), 'economics.json': null }, (dir) =>
        paletteChecks(dir).find((entry) => entry.id === ID['palette.agreement']))
      expectEqual(row.verdict, 'PASS', 'verdict of palette.agreement without economics.json')
      expectTrue(String(row.detail).includes('not a product slot'), `detail "${row.detail}" does not explain the grandfathering`)
    }],
    ['never throws: paletteFindings on a directory that does not exist', () => {
      const rows = paletteFindings(path.join(SCRATCH_DIR, 'kiln-palette-absent-directory'))
      expectTrue(Array.isArray(rows), 'paletteFindings did not return an array')
    }],
    ['never throws: paletteFindings on a malformed tree still returns rows', () => {
      withTempSlot({ [STUDIO_REL]: '{', [COLOR_KT_REL]: '\u0000', [THEME_KT_REL]: 'colorScheme(' }, (dir) => {
        expectTrue(Array.isArray(paletteFindings(dir)), 'paletteFindings did not return an array')
        const rows = paletteChecks(dir)
        expectEqual(rows.length, PALETTE_IDS.length, 'verdict count')
        for (const row of rows) {
          expectTrue(['PASS', 'FAIL', 'UNVERIFIED', 'SKIP'].includes(row.verdict), `unexpected verdict ${row.verdict} for ${row.id}`)
        }
      })
    }],
    ['never throws: every id is reported for a slot with nothing in it', () => {
      withTempSlot({}, (dir) => {
        const rows = paletteChecks(dir)
        expectEqual(rows.map((row) => row.id), [...PALETTE_IDS], 'reported ids')
        expectTrue(rows.every((row) => row.verdict === 'UNVERIFIED'), 'an empty slot must be UNVERIFIED, never PASS')
      })
    }]
  ])
}

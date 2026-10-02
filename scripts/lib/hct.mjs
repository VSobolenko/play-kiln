#!/usr/bin/env node
/**
 * The factory's ONE implementation of the colour maths a palette gate needs.
 *
 * Why this module exists
 * ----------------------
 * Every PlayKiln slot declares a palette twice and keeps the two copies identical
 * (`store/studio.json` and `ui/theme/Color.kt`), but nothing measured *how the palette was
 * derived*. The motivating defect is recorded in numbers in
 * `docs/research/2026-10-02-color-systems-and-palette-derivation.md` §0: a high-chroma lime
 * (`#C9F04A`, HCT C 68.0) sitting on a chromatic dark-green field (`#12301C`, HCT C 22.8) at the
 * same hue, one Material 3 analogous step away. The palette's contrast story passed every floor,
 * which is exactly why a contrast-only gate could not see it. This module supplies the hue, chroma
 * and headroom measurements that document's "Encodable rules" are stated in, so a gate can compute
 * them instead of an agent judging them.
 *
 * No dependency is added. `package.json` declares none and
 * `@material/material-color-utilities` is not installed; the HCT half below is a faithful port of
 * Google's own reference implementation so the numbers agree with the ones the research document
 * measured with the real library.
 *
 * The WCAG half is deliberately redundantly implemented: `scripts/lib/colours.py` owns the same
 * maths for the Python report (`scripts/visual-qa.py`), and the two are cross-checked on a shared
 * fixture table rather than assumed equal — the "one source of truth, imported" rule of
 * `.cursor/rules/verification-discipline.mdc`, applied across the language boundary where an
 * `import` is not possible. `selfTest()` carries that cross-check's known values so the sensor can
 * be falsified.
 *
 * What this module does NOT claim: it cannot say a palette is beautiful. It measures. The
 * research document's §9 is explicit that chroma magnitude is a design-system choice, not a
 * perceptual constant, and that rules built on it are conventions wearing numbers.
 *
 * Algorithm sources, cited per rule below:
 *  - WCAG 2.2 relative luminance / contrast: <https://www.w3.org/TR/WCAG22/> (SC 1.4.3, 1.4.11).
 *  - OKLab / OKLCH: B. Ottosson, "A perceptual color space for image processing" (2020),
 *    <https://bottosson.github.io/posts/oklab/>; gamut mapping by holding hue and reducing chroma
 *    is CSS Color 4 §14 (MINDE), <https://www.w3.org/TR/css-color-4/>.
 *  - HCT (CAM16 hue/chroma + CIELAB L* tone): Google's `material-color-utilities` reference
 *    implementation, <https://github.com/material-foundation/material-color-utilities> — files
 *    ported: `hct/hct.js`, `hct/cam16.js`, `hct/viewing_conditions.js`, `hct/hct_solver.js`,
 *    `utils/color_utils.js`, `utils/math_utils.js` (v0.4.0).
 */

// ============================================================================
// Hex <-> RGB
// ============================================================================

const HEX_LONG = /^#?([0-9a-fA-F]{6})$/
const HEX_SHORT = /^#?([0-9a-fA-F]{3})$/

/**
 * Parse `#RGB` or `#RRGGBB` (with or without `#`, case-insensitive) into 0..255 integers.
 *
 * The three-digit form is CSS shorthand: each nibble is doubled (`#abc` -> `#aabbcc`). A malformed
 * value throws rather than silently becoming black — the same choice `scripts/lib/colours.py`
 * makes, because a parse failure read as black is a palette gate that passes.
 */
export function parseHex(hex) {
  if (typeof hex !== 'string') {
    throw new Error(`parseHex: expected a string colour, got ${typeof hex} (${String(hex)})`)
  }
  const s = hex.trim()
  const long = HEX_LONG.exec(s)
  if (long) {
    const v = long[1]
    return {
      r: parseInt(v.slice(0, 2), 16),
      g: parseInt(v.slice(2, 4), 16),
      b: parseInt(v.slice(4, 6), 16),
    }
  }
  const short = HEX_SHORT.exec(s)
  if (short) {
    const [r, g, b] = short[1].split('')
    return {
      r: parseInt(r + r, 16),
      g: parseInt(g + g, 16),
      b: parseInt(b + b, 16),
    }
  }
  throw new Error(`parseHex: expected "#RGB" or "#RRGGBB", got ${JSON.stringify(hex)}`)
}

/**
 * `{ r, g, b }` (or any object with those channels) as an uppercase `#RRGGBB` string.
 * Channels are rounded and clamped to 0..255, matching `scripts/lib/colours.py`'s `hex_of`.
 */
export function hexOf(rgb) {
  if (!rgb || typeof rgb !== 'object') {
    throw new Error(`hexOf: expected { r, g, b }, got ${JSON.stringify(rgb)}`)
  }
  const channel = (value, name) => {
    const n = Number(value)
    if (!Number.isFinite(n)) throw new Error(`hexOf: channel ${name} is not a finite number (${String(value)})`)
    return Math.max(0, Math.min(255, Math.round(n)))
  }
  const toHex = (v) => v.toString(16).toUpperCase().padStart(2, '0')
  return `#${toHex(channel(rgb.r, 'r'))}${toHex(channel(rgb.g, 'g'))}${toHex(channel(rgb.b, 'b'))}`
}

// ============================================================================
// sRGB transfer, WCAG relative luminance, WCAG contrast
// ============================================================================

/**
 * The real IEC 61966-2-1 sRGB transfer function on a 0..255 sample, returning 0..1.
 *
 * This is the piecewise curve — `c / 12.92` below the knee and `((c + 0.055) / 1.055)^2.4` above
 * it — NOT `x / 255`, which is the linear approximation that makes a contrast report disagree with
 * every other tool. The 0.04045 knee and this exact shape are what `scripts/lib/colours.py` uses,
 * so the JS and Python halves agree bit-for-bit on the fixtures `selfTest()` carries.
 * Source: <https://www.w3.org/TR/WCAG22/#dfn-relative-luminance>.
 */
export function srgbToLinear(channel0to255) {
  const c = Number(channel0to255) / 255
  return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
}

/**
 * WCAG 2.2 relative luminance of an `#RRGGBB` colour: the 0.2126/0.7152/0.0722 weighted sum of the
 * linearised channels. Returns 0..1 (`#000000` -> 0, `#FFFFFF` -> 1).
 *
 * Rule 1/2 (contrast floors) and §4.1 of the research document rest on this quantity.
 */
export function relativeLuminance(hex) {
  const { r, g, b } = parseHex(hex)
  return 0.2126 * srgbToLinear(r) + 0.7152 * srgbToLinear(g) + 0.0722 * srgbToLinear(b)
}

/**
 * WCAG 2.2 contrast ratio `(L1 + 0.05) / (L2 + 0.05)` with `L1` the lighter relative luminance.
 * Returns 1..21. Hue- and chroma-blind by construction — the research document §4.1 is explicit
 * that a pass here is not a legibility claim, and §0.4 is explicit that the motivating palette
 * passes it while failing hierarchy.
 *
 * Rules 1, 2, 3, 4, 5, 6 and 21 all read this function.
 */
export function contrastRatio(hexA, hexB) {
  const la = relativeLuminance(hexA)
  const lb = relativeLuminance(hexB)
  const hi = Math.max(la, lb)
  const lo = Math.min(la, lb)
  return (hi + 0.05) / (lo + 0.05)
}

// ============================================================================
// OKLab / OKLCH  (Ottosson 2020, CSS Color 4)
// ============================================================================

/**
 * OKLab coordinates of an `#RRGGBB` colour. Matrices are Ottosson's `M1` (linear sRGB -> LMS) and
 * `M2` (cube-root LMS -> Lab), as reproduced in the research document §1.3.
 */
export function oklabOf(hex) {
  const { r, g, b } = parseHex(hex)
  const lr = srgbToLinear(r)
  const lg = srgbToLinear(g)
  const lb = srgbToLinear(b)
  const l = Math.cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb)
  const m = Math.cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb)
  const s = Math.cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)
  return {
    L: 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
    a: 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
    b: 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
  }
}

/**
 * OKLCH polar coordinates: `{ L, C, h }` with `h` in degrees 0..359.99.
 *
 * Rule 12 (gamut headroom report) and §1.3 of the research document measure in this space. Note the
 * warning it gives: OKLCH hue angles are NOT HCT hue angles — the same lime is HCT 123.4° and OKLCH
 * 121.9° — so a rule stated in degrees must say which degrees it means.
 */
export function oklchOf(hex) {
  const { L, a, b } = oklabOf(hex)
  let h = Math.atan2(b, a) * 180 / Math.PI
  if (h < 0) h += 360
  return { L, C: Math.hypot(a, b), h }
}

/** Linear-sRGB -> OKLab inverse (Ottosson), used only by the gamut bisection below. */
function _rgbFromOklab(L, a, b) {
  const l_ = L + 0.3963377774 * a + 0.2158037573 * b
  const m_ = L - 0.1055613458 * a - 0.0638541728 * b
  const s_ = L - 0.0894841775 * a - 1.2914855480 * b
  const l = l_ ** 3
  const m = m_ ** 3
  const s = s_ ** 3
  return [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
  ]
}

const GAMUT_EPSILON = 1e-5

function _oklabInGamut(L, a, b) {
  const [r, g, bl] = _rgbFromOklab(L, a, b)
  return (
    r >= -GAMUT_EPSILON && r <= 1 + GAMUT_EPSILON &&
    g >= -GAMUT_EPSILON && g <= 1 + GAMUT_EPSILON &&
    bl >= -GAMUT_EPSILON && bl <= 1 + GAMUT_EPSILON
  )
}

/**
 * The largest OKLCH chroma that stays inside sRGB at the given OKLCH lightness and hue, found by
 * bisection over the OKLab -> sRGB inverse. Returns 0 when even chroma 0 is out of gamut.
 *
 * This is CSS Color 4 §14's MINDE definition — hold hue and lightness, reduce chroma until the
 * colour is in gamut — and it is the metric `chromaHeadroom` divides by. The research document
 * §2.1 explains why its HCT-based proxy (`Hct.from(hue, 200, tone).chroma`) is NOT used here: that
 * proxy reports pure green at 189% of its own ceiling.
 *
 * The self-check the document relies on is that pure red, pure green and pure magenta come back at
 * exactly 100.0% headroom; `selfTest()` asserts it.
 */
export function maxChromaFor(L, hueDegrees) {
  if (!Number.isFinite(L) || !Number.isFinite(hueDegrees)) {
    throw new Error(`maxChromaFor: L and hue must be finite numbers, got L=${L} hue=${hueDegrees}`)
  }
  const hr = (hueDegrees * Math.PI) / 180
  if (!_oklabInGamut(L, 0, 0)) return 0
  let lo = 0
  let hi = 0.5
  if (_oklabInGamut(L, hi * Math.cos(hr), hi * Math.sin(hr))) return hi
  for (let i = 0; i < 60; i++) {
    const mid = (lo + hi) / 2
    if (_oklabInGamut(L, mid * Math.cos(hr), mid * Math.sin(hr))) lo = mid
    else hi = mid
  }
  return lo
}

/**
 * `C(role) / C_max(role) × 100`, where `C_max` is the largest in-gamut OKLCH chroma at the role's
 * OWN OKLCH lightness and hue (found by `maxChromaFor`).
 *
 * Rule 12 reads this and is explicitly a REPORT, not a pass/fail rule: Material 3's own light
 * `primaryContainer` sits at 100.0% because the algorithm clamps it there, and Tailwind's
 * `lime-500` at 97.6% by choice. A gate that failed either would be failing a real design system.
 */
export function chromaHeadroom(hex) {
  const { L, C, h } = oklchOf(hex)
  const max = maxChromaFor(L, h)
  return max <= 0 ? 0 : (C / max) * 100
}

// ============================================================================
// HCT — CAM16 hue/chroma + CIELAB L* tone (Material 3)
// ============================================================================

// -- math helpers ported from material-color-utilities/utils/math_utils.js -----

function _signum(x) {
  return x < 0 ? -1 : x === 0 ? 0 : 1
}

function _lerp(start, stop, amount) {
  return (1 - amount) * start + amount * stop
}

function _sanitizeDegreesDouble(degrees) {
  let d = degrees % 360
  if (d < 0) d += 360
  return d
}

/** matrixMultiply: result[j] = row . matrix[j], as in the reference implementation. */
function _matrixMultiply(row, matrix) {
  return [
    row[0] * matrix[0][0] + row[1] * matrix[0][1] + row[2] * matrix[0][2],
    row[0] * matrix[1][0] + row[1] * matrix[1][1] + row[2] * matrix[1][2],
    row[0] * matrix[2][0] + row[1] * matrix[2][1] + row[2] * matrix[2][2],
  ]
}

// -- sRGB <-> linear in the reference implementation's 0..100 scale ------------

/**
 * The reference implementation's `linearized()`: 0..255 -> 0..100. Its knee is 0.040449936, not the
 * WCAG 0.04045, and it scales to 100 rather than 1 — both matter only to the HCT/CAM16 pipeline,
 * never to `relativeLuminance`. Keeping this separate is deliberate: mixing the two conventions is
 * how a faithful HCT port silently stops reproducing the vendor's numbers.
 */
function _linearized100(channel) {
  const n = channel / 255
  return n <= 0.040449936 ? (n / 12.92) * 100 : Math.pow((n + 0.055) / 1.055, 2.4) * 100
}

/** The reference implementation's `delinearized()`: 0..100 -> rounded, clamped 0..255. */
function _delinearized(linear0to100) {
  const n = linear0to100 / 100
  const d = n <= 0.0031308 ? n * 12.92 : 1.055 * Math.pow(n, 1 / 2.4) - 0.055
  return Math.max(0, Math.min(255, Math.round(d * 255)))
}

// -- CIELAB L* (tone), tied to WCAG luminance by construction ------------------

const _LAB_E = 216 / 24389
const _LAB_KAPPA = 24389 / 27

function _labF(t) {
  return t > _LAB_E ? Math.cbrt(t) : (_LAB_KAPPA * t + 16) / 116
}

function _labInvf(ft) {
  const ft3 = ft * ft * ft
  return ft3 > _LAB_E ? ft3 : (116 * ft - 16) / _LAB_KAPPA
}

/** L* (tone) from XYZ Y (0..100). Tone 0 = black, tone 100 = white. */
function _lstarFromY(y) {
  return _labF(y / 100) * 116 - 16
}

/** Y (0..100) from L* (tone). The exact inverse of `_lstarFromY`. */
function _yFromLstar(lstar) {
  return 100 * _labInvf((lstar + 16) / 116)
}

// -- CAM16 viewing conditions (white point D65, 200 lux, average surround) -----

const _WHITE_POINT_D65 = [95.047, 100.0, 108.883]

/**
 * The reference implementation's `ViewingConditions.make()` with its documented defaults: D65 white
 * point, adapting luminance from 200 lux (~11.72), background L* 50, average surround, illuminant
 * not discounted. Ported verbatim so `hctOf` reproduces the vendor's numbers.
 */
function _makeViewingConditions(
  whitePoint = _WHITE_POINT_D65,
  adaptingLuminance = ((200.0 / Math.PI) * _yFromLstar(50.0)) / 100.0,
  backgroundLstar = 50.0,
  surround = 2.0,
  discountingIlluminant = false,
) {
  const [X, Y, Z] = whitePoint
  const rW = X * 0.401288 + Y * 0.650173 + Z * -0.051461
  const gW = X * -0.250268 + Y * 1.204414 + Z * 0.045854
  const bW = X * -0.002079 + Y * 0.048952 + Z * 0.953127
  const f = 0.8 + surround / 10.0
  const c = f >= 0.9 ? _lerp(0.59, 0.69, (f - 0.9) * 10.0) : _lerp(0.525, 0.59, (f - 0.8) * 10.0)
  let d = discountingIlluminant ? 1.0 : f * (1.0 - (1.0 / 3.6) * Math.exp((-adaptingLuminance - 42.0) / 92.0))
  d = d > 1.0 ? 1.0 : d < 0.0 ? 0.0 : d
  const nc = f
  const rgbD = [
    d * (100.0 / rW) + 1.0 - d,
    d * (100.0 / gW) + 1.0 - d,
    d * (100.0 / bW) + 1.0 - d,
  ]
  const k = 1.0 / (5.0 * adaptingLuminance + 1.0)
  const k4 = k * k * k * k
  const k4F = 1.0 - k4
  const fl = k4 * adaptingLuminance + 0.1 * k4F * k4F * Math.cbrt(5.0 * adaptingLuminance)
  const n = _yFromLstar(backgroundLstar) / whitePoint[1]
  const z = 1.48 + Math.sqrt(n)
  const nbb = 0.725 / Math.pow(n, 0.2)
  const ncb = nbb
  const rgbAFactors = [
    Math.pow((fl * rgbD[0] * rW) / 100.0, 0.42),
    Math.pow((fl * rgbD[1] * gW) / 100.0, 0.42),
    Math.pow((fl * rgbD[2] * bW) / 100.0, 0.42),
  ]
  const rgbA = [
    (400.0 * rgbAFactors[0]) / (rgbAFactors[0] + 27.13),
    (400.0 * rgbAFactors[1]) / (rgbAFactors[1] + 27.13),
    (400.0 * rgbAFactors[2]) / (rgbAFactors[2] + 27.13),
  ]
  const aw = (2.0 * rgbA[0] + rgbA[1] + 0.05 * rgbA[2]) * nbb
  return { n, aw, nbb, ncb, c, nc, rgbD, fl, fLRoot: Math.pow(fl, 0.25), z }
}

const VIEWING_CONDITIONS = _makeViewingConditions()

/** `Cam16.fromInt(argb)` — returns the CAM16 coordinates under default viewing conditions. */
function _cam16FromRgb(r, g, b) {
  const vc = VIEWING_CONDITIONS
  const redL = _linearized100(r)
  const greenL = _linearized100(g)
  const blueL = _linearized100(b)
  // linear sRGB -> XYZ (the reference implementation's SRGB_TO_XYZ matrix)
  const x = 0.41233895 * redL + 0.35762064 * greenL + 0.18051042 * blueL
  const y = 0.2126 * redL + 0.7152 * greenL + 0.0722 * blueL
  const z = 0.01932141 * redL + 0.11916382 * greenL + 0.95034478 * blueL
  // XYZ -> cone responses, then discount the illuminant
  const rC = 0.401288 * x + 0.650173 * y - 0.051461 * z
  const gC = -0.250268 * x + 1.204414 * y + 0.045854 * z
  const bC = -0.002079 * x + 0.048952 * y + 0.953127 * z
  const rD = vc.rgbD[0] * rC
  const gD = vc.rgbD[1] * gC
  const bD = vc.rgbD[2] * bC
  const rAF = Math.pow((vc.fl * Math.abs(rD)) / 100.0, 0.42)
  const gAF = Math.pow((vc.fl * Math.abs(gD)) / 100.0, 0.42)
  const bAF = Math.pow((vc.fl * Math.abs(bD)) / 100.0, 0.42)
  const rA = (_signum(rD) * 400.0 * rAF) / (rAF + 27.13)
  const gA = (_signum(gD) * 400.0 * gAF) / (gAF + 27.13)
  const bA = (_signum(bD) * 400.0 * bAF) / (bAF + 27.13)
  const a = (11.0 * rA + -12.0 * gA + bA) / 11.0
  const bb = (rA + gA - 2.0 * bA) / 9.0
  const u = (20.0 * rA + 20.0 * gA + 21.0 * bA) / 20.0
  const p2 = (40.0 * rA + 20.0 * gA + bA) / 20.0
  const atanDegrees = (Math.atan2(bb, a) * 180.0) / Math.PI
  const hue = _sanitizeDegreesDouble(atanDegrees)
  const hueRadians = (hue * Math.PI) / 180.0
  const ac = p2 * vc.nbb
  const j = 100.0 * Math.pow(ac / vc.aw, vc.c * vc.z)
  const huePrime = hue < 20.14 ? hue + 360 : hue
  const eHue = 0.25 * (Math.cos((huePrime * Math.PI) / 180.0 + 2.0) + 3.8)
  const p1 = (50000.0 / 13.0) * eHue * vc.nc * vc.ncb
  const t = (p1 * Math.sqrt(a * a + bb * bb)) / (u + 0.305)
  const alpha = Math.pow(t, 0.9) * Math.pow(1.64 - Math.pow(0.29, vc.n), 0.73)
  const chroma = alpha * Math.sqrt(j / 100.0)
  return { hue, chroma, j, hueRadians }
}

/** CIELAB L* of an `#RRGGBB` colour, via the reference implementation's XYZ path. */
function _lstarFromRgb(r, g, b) {
  const redL = _linearized100(r)
  const greenL = _linearized100(g)
  const blueL = _linearized100(b)
  const y = 0.2126 * redL + 0.7152 * greenL + 0.0722 * blueL
  return _lstarFromY(y)
}

/**
 * HCT of an `#RRGGBB` colour: `{ h, c, t }`.
 *
 *  - `h` (hue) and `c` (chroma) come from CAM16 (a colour-*appearance* model, i.e. the colour as a
 *    human sees it under the default D65/200-lux/background-L*-50 viewing conditions), exactly as
 *    Material 3's `Hct.fromInt()` computes them.
 *  - `t` (tone) is CIELAB L*, which is tied to WCAG relative luminance by construction: tone 0 is
 *    black, tone 100 is white, and `Contrast.ratioOfTones` is the WCAG ratio over that axis.
 *
 * This is the hard half. It reproduces the research document §0.3 table: `#C9F04A` -> H123.4,
 * C68.0, T89.5, and `#12301C` -> H155.3, C22.8, T17.1. Rules 8, 10, 11, 13, 14, 15, 16, 17, 18,
 * 20, 21, 25 and 26-28 read this function's three numbers.
 */
export function hctOf(hex) {
  const { r, g, b } = parseHex(hex)
  const cam = _cam16FromRgb(r, g, b)
  return { h: cam.hue, c: cam.chroma, t: _lstarFromRgb(r, g, b) }
}

// -- HctSolver: the inverse, `Hct.from(h, c, t)` -> hex ------------------------
// Rule 15 is the reason this inverse is here: it needs Material 3's primaryContainer chroma at the
// accent's own hue, which is `Hct.from(accentHue, 36, 90).chroma` for the tonal-spot scheme's
// constant primary chroma 36 and container tone 90. Ported from `hct/hct_solver.js`.

const SCALED_DISCOUNT_FROM_LINRGB = [
  [0.001200833568784504, 0.002389694492170889, 0.0002795742885861124],
  [0.0005891086651375999, 0.0029785502573438758, 0.0003270666104008398],
  [0.00010146692491640572, 0.0005364214359186694, 0.0032979401770712076],
]

const LINRGB_FROM_SCALED_DISCOUNT = [
  [1373.2198709594231, -1100.4251190754821, -7.278681089101213],
  [-271.815969077903, 559.6580465940733, -32.46047482791194],
  [1.9622899599665666, -57.173814538844006, 308.7233197812385],
]

const Y_FROM_LINRGB = [0.2126, 0.7152, 0.0722]

/**
 * The 255 critical planes of the sRGB cube, in linear-RGB coordinates. `CRITICAL_PLANES[i]` is the
 * linear value at sRGB sample `i + 0.5`, i.e. the boundary between integer samples `i` and `i + 1`.
 * The reference implementation hardcodes these; they are the exact inverse of `_linearized100` at
 * half-integer samples, so they are derived here rather than transcribed — 255 magic numbers is a
 * transcription defect waiting to happen. `selfTest()` checks the derived array against the
 * reference implementation's first and last constants.
 */
const CRITICAL_PLANES = (() => {
  const planes = new Array(255)
  for (let i = 0; i < 255; i++) {
    const n = (i + 0.5) / 255
    planes[i] = n <= 0.040449936 ? (n / 12.92) * 100 : Math.pow((n + 0.055) / 1.055, 2.4) * 100
  }
  return planes
})()

function _sanitizeRadians(angle) {
  return (angle + Math.PI * 8) % (Math.PI * 2)
}

function _trueDelinearized(linear0to100) {
  const n = linear0to100 / 100
  const d = n <= 0.0031308 ? n * 12.92 : 1.055 * Math.pow(n, 1 / 2.4) - 0.055
  return d * 255
}

function _chromaticAdaptation(component) {
  const af = Math.pow(Math.abs(component), 0.42)
  return (_signum(component) * 400.0 * af) / (af + 27.13)
}

function _inverseChromaticAdaptation(adapted) {
  const adaptedAbs = Math.abs(adapted)
  const base = Math.max(0, (27.13 * adaptedAbs) / (400.0 - adaptedAbs))
  return _signum(adapted) * Math.pow(base, 1 / 0.42)
}

function _hueOf(linrgb) {
  const scaledDiscount = _matrixMultiply(linrgb, SCALED_DISCOUNT_FROM_LINRGB)
  const rA = _chromaticAdaptation(scaledDiscount[0])
  const gA = _chromaticAdaptation(scaledDiscount[1])
  const bA = _chromaticAdaptation(scaledDiscount[2])
  const a = (11.0 * rA + -12.0 * gA + bA) / 11.0
  const b = (rA + gA - 2.0 * bA) / 9.0
  return Math.atan2(b, a)
}

function _areInCyclicOrder(a, b, c) {
  const deltaAB = _sanitizeRadians(b - a)
  const deltaAC = _sanitizeRadians(c - a)
  return deltaAB < deltaAC
}

function _lerpPoint(source, t, target) {
  return [
    source[0] + (target[0] - source[0]) * t,
    source[1] + (target[1] - source[1]) * t,
    source[2] + (target[2] - source[2]) * t,
  ]
}

function _setCoordinate(source, coordinate, target, axis) {
  const t = (coordinate - source[axis]) / (target[axis] - source[axis])
  return _lerpPoint(source, t, target)
}

function _isBounded(x) {
  return x >= 0.0 && x <= 100.0
}

function _nthVertex(y, n) {
  const kR = Y_FROM_LINRGB[0]
  const kG = Y_FROM_LINRGB[1]
  const kB = Y_FROM_LINRGB[2]
  const coordA = n % 4 <= 1 ? 0.0 : 100.0
  const coordB = n % 2 === 0 ? 0.0 : 100.0
  if (n < 4) {
    const g = coordA
    const b = coordB
    const r = (y - g * kG - b * kB) / kR
    return _isBounded(r) ? [r, g, b] : [-1, -1, -1]
  }
  if (n < 8) {
    const b = coordA
    const r = coordB
    const g = (y - r * kR - b * kB) / kG
    return _isBounded(g) ? [r, g, b] : [-1, -1, -1]
  }
  const r = coordA
  const g = coordB
  const b = (y - r * kR - g * kG) / kB
  return _isBounded(b) ? [r, g, b] : [-1, -1, -1]
}

function _bisectToSegment(y, targetHue) {
  let left = [-1, -1, -1]
  let right = left
  let leftHue = 0.0
  let rightHue = 0.0
  let initialized = false
  let uncut = true
  for (let n = 0; n < 12; n++) {
    const mid = _nthVertex(y, n)
    if (mid[0] < 0) continue
    const midHue = _hueOf(mid)
    if (!initialized) {
      left = mid
      right = mid
      leftHue = midHue
      rightHue = midHue
      initialized = true
      continue
    }
    if (uncut || _areInCyclicOrder(leftHue, midHue, rightHue)) {
      uncut = false
      if (_areInCyclicOrder(leftHue, targetHue, midHue)) {
        right = mid
        rightHue = midHue
      } else {
        left = mid
        leftHue = midHue
      }
    }
  }
  return [left, right]
}

function _midpoint(a, b) {
  return [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2]
}

function _criticalPlaneBelow(x) {
  return Math.floor(x - 0.5)
}

function _criticalPlaneAbove(x) {
  return Math.ceil(x - 0.5)
}

function _bisectToLimit(y, targetHue) {
  const segment = _bisectToSegment(y, targetHue)
  let left = segment[0]
  let leftHue = _hueOf(left)
  let right = segment[1]
  for (let axis = 0; axis < 3; axis++) {
    if (left[axis] !== right[axis]) {
      let lPlane = -1
      let rPlane = 255
      if (left[axis] < right[axis]) {
        lPlane = _criticalPlaneBelow(_trueDelinearized(left[axis]))
        rPlane = _criticalPlaneAbove(_trueDelinearized(right[axis]))
      } else {
        lPlane = _criticalPlaneAbove(_trueDelinearized(left[axis]))
        rPlane = _criticalPlaneBelow(_trueDelinearized(right[axis]))
      }
      for (let i = 0; i < 8; i++) {
        if (Math.abs(rPlane - lPlane) <= 1) break
        const mPlane = Math.floor((lPlane + rPlane) / 2.0)
        const midPlaneCoordinate = CRITICAL_PLANES[mPlane]
        const mid = _setCoordinate(left, midPlaneCoordinate, right, axis)
        const midHue = _hueOf(mid)
        if (_areInCyclicOrder(leftHue, targetHue, midHue)) {
          right = mid
          rPlane = mPlane
        } else {
          left = mid
          leftHue = midHue
          lPlane = mPlane
        }
      }
    }
  }
  return _midpoint(left, right)
}

function _findResultByJ(hueRadians, chroma, y) {
  let j = Math.sqrt(y) * 11.0
  const vc = VIEWING_CONDITIONS
  const tInnerCoeff = 1 / Math.pow(1.64 - Math.pow(0.29, vc.n), 0.73)
  const eHue = 0.25 * (Math.cos(hueRadians + 2.0) + 3.8)
  const p1 = eHue * (50000.0 / 13.0) * vc.nc * vc.ncb
  const hSin = Math.sin(hueRadians)
  const hCos = Math.cos(hueRadians)
  for (let iterationRound = 0; iterationRound < 5; iterationRound++) {
    const jNormalized = j / 100.0
    const alpha = chroma === 0.0 || j === 0.0 ? 0.0 : chroma / Math.sqrt(jNormalized)
    const t = Math.pow(alpha * tInnerCoeff, 1.0 / 0.9)
    const ac = vc.aw * Math.pow(jNormalized, 1.0 / vc.c / vc.z)
    const p2 = ac / vc.nbb
    const gamma = (23.0 * (p2 + 0.305) * t) / (23.0 * p1 + 11 * t * hCos + 108.0 * t * hSin)
    const a = gamma * hCos
    const b = gamma * hSin
    const rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0
    const gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0
    const bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0
    const rCScaled = _inverseChromaticAdaptation(rA)
    const gCScaled = _inverseChromaticAdaptation(gA)
    const bCScaled = _inverseChromaticAdaptation(bA)
    const linrgb = _matrixMultiply([rCScaled, gCScaled, bCScaled], LINRGB_FROM_SCALED_DISCOUNT)
    if (linrgb[0] < 0 || linrgb[1] < 0 || linrgb[2] < 0) return 0
    const fnj = Y_FROM_LINRGB[0] * linrgb[0] + Y_FROM_LINRGB[1] * linrgb[1] + Y_FROM_LINRGB[2] * linrgb[2]
    if (fnj <= 0) return 0
    if (iterationRound === 4 || Math.abs(fnj - y) < 0.002) {
      if (linrgb[0] > 100.01 || linrgb[1] > 100.01 || linrgb[2] > 100.01) return 0
      return _argbFromLinrgb(linrgb)
    }
    j = j - ((fnj - y) * j) / (2 * fnj)
  }
  return 0
}

function _argbFromRgb(r, g, b) {
  return (((255 << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255)) >>> 0)
}

function _argbFromLinrgb(linrgb) {
  return _argbFromRgb(_delinearized(linrgb[0]), _delinearized(linrgb[1]), _delinearized(linrgb[2]))
}

function _argbFromLstar(lstar) {
  const y = _yFromLstar(lstar)
  const component = _delinearized(y)
  return _argbFromRgb(component, component, component)
}

function _solveToInt(hueDegrees, chroma, lstar) {
  if (chroma < 0.0001 || lstar < 0.0001 || lstar > 99.9999) return _argbFromLstar(lstar)
  const sanitized = _sanitizeDegreesDouble(hueDegrees)
  const hueRadians = (sanitized / 180) * Math.PI
  const y = _yFromLstar(lstar)
  const exactAnswer = _findResultByJ(hueRadians, chroma, y)
  if (exactAnswer !== 0) return exactAnswer
  return _argbFromLinrgb(_bisectToLimit(y, hueRadians))
}

/**
 * The inverse of `hctOf`: the `#RRGGBB` colour for a requested HCT triple, Material 3's
 * `Hct.from(hue, chroma, tone)`. When the requested chroma is out of gamut at that hue and tone it
 * is maximised instead (the vendor's documented behaviour).
 *
 * Rule 15 needs it: the tonal-spot scheme's `primaryContainer` is `Hct.from(seedHue, 36, 90)`, so
 * `hctFrom(accentHue, 36, 90)` gives the chroma Material 3 would have used, and the rule's 1.5×
 * bound is a comparison against it. Rule 22 (deviations from a declared derivation) needs it too.
 */
export function hctFrom(hueDegrees, chroma, lstar) {
  const argb = _solveToInt(hueDegrees, chroma, lstar)
  return hexOf({ r: (argb >>> 16) & 255, g: (argb >>> 8) & 255, b: argb & 255 })
}

// ============================================================================
// Small rule helpers
// ============================================================================

/**
 * Circular hue distance in degrees, `min(|a - b|, 360 - |a - b|)`, in the range 0..180.
 * Rules 8, 9, 10, 20 and 21 are all stated as hue deltas and must use the circular distance, or a
 * pair straddling 0° reads as 350° apart.
 */
export function deltaHue(a, b) {
  const d = Math.abs(((a % 360) + 360) % 360 - (((b % 360) + 360) % 360))
  return d > 180 ? 360 - d : d
}

/**
 * Material 3's dislike predicate, ported from
 * `@material/material-color-utilities/dislike/dislike_analyzer.js`: a colour is "universally
 * disliked" when `90 <= round(hue) <= 111 AND round(chroma) > 16 AND round(tone) < 65`, and the
 * algorithm forces it to tone 70.
 *
 * Rule 16. It is carried because it is the only vendor-encoded aesthetic bound that exists, NOT
 * because it catches the motivating defect — the lawn accent is H123.4 and escapes the 90-111 band
 * by 12°. A gate built only on this would pass the palette §0 is about.
 */
export function isDislikedHct(hct) {
  if (!hct || typeof hct !== 'object') {
    throw new Error(`isDislikedHct: expected { h, c, t }, got ${JSON.stringify(hct)}`)
  }
  const huePasses = Math.round(hct.h) >= 90.0 && Math.round(hct.h) <= 111.0
  const chromaPasses = Math.round(hct.c) > 16.0
  const tonePasses = Math.round(hct.t) < 65.0
  return huePasses && chromaPasses && tonePasses
}

// ============================================================================
// selfTest — the sensor's own falsifiability
// ============================================================================

/** Absolute tolerance helper for the self-test rows. */
function _close(actual, expected, tol) {
  return Math.abs(actual - expected) <= tol
}

/**
 * Assert the values this module is trusted for, as `{ name, ok, detail }` rows.
 *
 * The fixture values are the ones the research document measured with Google's own
 * `@material/material-color-utilities` 0.4.0 (§0.3 and §2.4), so a row that fails is a finding
 * about the port, not a matter of taste. `selfTest()` never throws: a malformed-input row is
 * checked by catching the expected error, so a caller reading the rows sees a FAIL rather than an
 * exception.
 *
 * A sensor that cannot fail is not a sensor: mutating `srgbToLinear`'s knee (or inverting the
 * transfer) must turn rows here red. That is the demonstration the task asks for.
 */
export function selfTest() {
  const rows = []
  const add = (name, fn) => {
    try {
      const { ok, detail } = fn()
      rows.push({ name, ok: Boolean(ok), detail })
    } catch (err) {
      rows.push({ name, ok: false, detail: `threw: ${err.message}` })
    }
  }

  // --- hex <-> rgb --------------------------------------------------------
  add('parseHex("#C9F04A") -> 201,240,74', () => {
    const { r, g, b } = parseHex('#C9F04A')
    const ok = r === 201 && g === 240 && b === 74
    return { ok, detail: `got ${r},${g},${b}` }
  })

  add('parseHex("#abc") shorthand -> 170,187,204', () => {
    const { r, g, b } = parseHex('#abc')
    const ok = r === 170 && g === 187 && b === 204
    return { ok, detail: `got ${r},${g},${b}` }
  })

  add('parseHex("12301c") without # -> 18,48,28', () => {
    const { r, g, b } = parseHex('12301c')
    const ok = r === 18 && g === 48 && b === 28
    return { ok, detail: `got ${r},${g},${b}` }
  })

  add('parseHex("nope") throws', () => {
    let threw = false
    try {
      parseHex('nope')
    } catch {
      threw = true
    }
    return { ok: threw, detail: threw ? 'threw as required' : 'did NOT throw' }
  })

  add('hexOf(parseHex(x)) round-trips', () => {
    const ok = hexOf(parseHex('#12301C')) === '#12301C' && hexOf(parseHex('abc')) === '#AABBCC'
    return { ok, detail: `${hexOf(parseHex('#12301C'))}, ${hexOf(parseHex('abc'))}` }
  })

  // --- sRGB transfer / WCAG ----------------------------------------------
  add('srgbToLinear(0)=0, (255)=1, (128)=0.2159', () => {
    const a = srgbToLinear(0)
    const b = srgbToLinear(255)
    const c = srgbToLinear(128)
    const ok = a === 0 && _close(b, 1, 1e-12) && _close(c, 0.2158605, 1e-6)
    return { ok, detail: `${a}, ${b}, ${c.toFixed(7)}` }
  })

  add('relativeLuminance: black 0, white 1', () => {
    const a = relativeLuminance('#000000')
    const b = relativeLuminance('#FFFFFF')
    const ok = a === 0 && _close(b, 1, 1e-12)
    return { ok, detail: `${a}, ${b}` }
  })

  add('contrastRatio(black, white) = 21', () => {
    const v = contrastRatio('#000000', '#FFFFFF')
    const ok = _close(v, 21, 1e-9)
    return { ok, detail: v.toFixed(6) }
  })

  add('contrastRatio("#C9F04A","#12301C") ~ 10.95 (doc §0.2)', () => {
    const v = contrastRatio('#C9F04A', '#12301C')
    const ok = _close(v, 10.95, 0.01)
    return { ok, detail: v.toFixed(4) }
  })

  // --- OKLCH --------------------------------------------------------------
  add('oklchOf("#C9F04A") = L0.898 C0.190 H121.9 (doc §0.3)', () => {
    const { L, C, h } = oklchOf('#C9F04A')
    const ok = _close(L, 0.898, 0.002) && _close(C, 0.190, 0.002) && _close(h, 121.9, 0.5)
    return { ok, detail: `L${L.toFixed(3)} C${C.toFixed(3)} H${h.toFixed(1)}` }
  })

  add('maxChromaFor: pure red/green/magenta at 100.0% headroom', () => {
    const parts = ['#FF0000', '#00FF00', '#FF00FF'].map((hex) => chromaHeadroom(hex))
    const ok = parts.every((p) => _close(p, 100.0, 0.05))
    return { ok, detail: parts.map((p) => p.toFixed(1)).join(', ') }
  })

  add('chromaHeadroom("#C9F04A") ~ 87.5 (doc §0.3)', () => {
    const v = chromaHeadroom('#C9F04A')
    const ok = _close(v, 87.5, 0.5)
    return { ok, detail: v.toFixed(1) }
  })

  // --- HCT ----------------------------------------------------------------
  add('hctOf("#C9F04A") = H123.4 C68.0 T89.5 (doc §0.3)', () => {
    const { h, c, t } = hctOf('#C9F04A')
    const ok = _close(h, 123.4, 0.15) && _close(c, 68.0, 0.15) && _close(t, 89.5, 0.15)
    return { ok, detail: `H${h.toFixed(1)} C${c.toFixed(1)} T${t.toFixed(1)}` }
  })

  add('hctOf("#12301C") = H155.3 C22.8 T17.1 (doc §0.3)', () => {
    const { h, c, t } = hctOf('#12301C')
    const ok = _close(h, 155.3, 0.15) && _close(c, 22.8, 0.15) && _close(t, 17.1, 0.15)
    return { ok, detail: `H${h.toFixed(1)} C${c.toFixed(1)} T${t.toFixed(1)}` }
  })

  add('deltaHue("#C9F04A","#12301C") ~ 31.9 (doc §0.3 fact 2)', () => {
    const v = deltaHue(hctOf('#C9F04A').h, hctOf('#12301C').h)
    const ok = _close(v, 31.9, 0.2)
    return { ok, detail: v.toFixed(1) }
  })

  // --- HCT inverse / rule 15 ---------------------------------------------
  add('hctFrom(123.4, 36, 90) ~ M3 primaryContainer C36.1; accent/accent = 1.88', () => {
    const pc = hctOf(hctFrom(123.4, 36, 90))
    const accentC = hctOf('#C9F04A').c
    const ratio = accentC / pc.c
    const ok = _close(pc.c, 36.1, 0.2) && _close(ratio, 1.88, 0.05)
    return { ok, detail: `pcC${pc.c.toFixed(1)} ratio ${ratio.toFixed(3)}` }
  })

  add('hctFrom round-trips the accent through HCT', () => {
    const { h, c, t } = hctOf('#C9F04A')
    const hex = hctFrom(h, c, t)
    const back = hctOf(hex)
    const ok = _close(back.h, h, 0.2) && _close(back.c, c, 0.2) && _close(back.t, t, 0.2)
    return { ok, detail: `${hex} H${back.h.toFixed(1)} C${back.c.toFixed(1)} T${back.t.toFixed(1)}` }
  })

  // --- derived CRITICAL_PLANES sanity ------------------------------------
  add('derived CRITICAL_PLANES match the reference implementation', () => {
    const ok =
      _close(CRITICAL_PLANES[0], 0.015176349177441876, 1e-15) &&
      _close(CRITICAL_PLANES[254], 99.55452497210776, 1e-12)
    return { ok, detail: `[0]=${CRITICAL_PLANES[0]} [254]=${CRITICAL_PLANES[254]}` }
  })

  // --- rule 16 dislike predicate -----------------------------------------
  add('isDislikedHct: H100 C30 T40 disliked; lawn accent not', () => {
    const a = isDislikedHct({ h: 100, c: 30, t: 40 })
    const b = isDislikedHct(hctOf('#C9F04A'))
    const ok = a === true && b === false
    return { ok, detail: `mid-olive ${a}, accent ${b}` }
  })

  return rows
}

export default {
  parseHex,
  hexOf,
  srgbToLinear,
  relativeLuminance,
  contrastRatio,
  oklabOf,
  oklchOf,
  maxChromaFor,
  chromaHeadroom,
  hctOf,
  hctFrom,
  deltaHue,
  isDislikedHct,
  selfTest,
}

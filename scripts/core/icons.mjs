// Icons: the provenance contract as a sensor — where a slot's UI iconography came from, and that
// its launcher mark is still the slot's own.
//
// The contract this enforces is `docs/icon-provenance.md`, written 2026-10-02 after the owner
// objected that UI glyphs had been hand-drawn instead of taken from a professional, licence-cleared
// set. That document admitted, in its own words, that no program read it: "the UI-iconography half
// is unenforced … a builder's obligation read back from the diff". A rule only a document asserts is
// a rule nobody follows — this repository's most-recorded failure mode — so this module is that
// check, and this file is the first program to read the contract.
//
// Legs, each named after what it measures (docs/icon-provenance.md section in brackets):
//
//   icons.mark       the declared mark resolves in `scripts/lib/icon_design.py`'s MARK_REGISTRY (§9)
//   icons.launcher   the shipped launcher vectors are the slot's own, not the scaffold's placeholder,
//                    and their fills are roles of the slot's declared palette (§9, rule 13)
//   icons.provenance `apps/<slot>/icons/provenance.json` resolves: schema, §2 allowlist, §3 denylist,
//                    a regular committed file per record and per sha256, and a `licenceFile` that is a
//                    regular non-empty file named for its own spdx. A record that cannot be read
//                    degrades locally: only this leg and `icons.licence` lose their verdicts
//   icons.licence    a non-empty `icons/LICENSES/<SPDX>.txt` exists for every distinct spdx (§5)
//   icons.offline    no icon artifact is fetched at build or run time (§4, rule 6); comments are
//                    documentation and are blanked before matching, and the shipped scan is at least
//                    as strict as the build scan
//   icons.deprecated no `material-icons-core` / `material-icons-extended` coordinate in a real
//                    dependency line (§4.1, rule 8); a comment naming one is not a coordinate
//   icons.tint       UI iconography carries no literal `fillColor`; the mark is excluded by design, and
//                    a palette-role literal on a slot-owned drawable is a note (§6)
//
// How the Python-owned registry is read, and why that way. `scripts/lib/icon_design.py` holds
// `MARK_REGISTRY` as a Python dict, and this core's contract (`scripts/core/lib.mjs`) is explicit
// that "a check is a function, never a script that shells out to another script" and that "the only
// permitted subprocess is the build tool". So the registry cannot be imported — it is read from the
// module's own source text, by brace-matching the `MARK_REGISTRY = { … }` literal and collecting its
// top-level string keys. That is the same route `scripts/core/build.mjs` already takes to
// Python-owned data (it regexes `scripts/lib/scratch.py`'s source for `SCRATCH_DIR` and scans
// `scripts/*.py` for `tempfile` calls) and it keeps one source of truth: the registry literal on
// disk, never a second copy in JavaScript. Its honest limit: a registry built by code rather than
// written as a literal would not be read, and this check would then report `UNVERIFIED` rather than
// a wrong answer. It never claims to have executed Python.
//
// The run-time entry point is `iconsFindings(slotDir)` and the verdicts are `iconsChecks(slotDir)`;
// `iconsFixtures()` carries one synthetic tree per leg whose verdict is known, including the case
// that must fail, because a check that cannot fail is not a check.
//
// Registered by `scripts/core/index.mjs` in the `icon` group beside the adaptive-icon check, so
// `node scripts/verify.mjs --slot <slug>` runs it. The entry point holds a literal `reports` list
// rather than deriving it from `ICON_IDS`, because `checkIdsFromRegistry` reads that registry as
// text and a fact can only name an id written there as a literal `id:`; a load-time assertion fails
// if the two lists drift.
//
// Declared ambiguities in the contract, enforced as stated and reported rather than resolved:
//
//   1. §1.2 writes `icons[].file` as `app/src/main/res/drawable/ic_add.xml` (slot-relative) while its
//      field table calls the value "repo-relative" and its `licenceFile` example is repo-relative
//      (`apps/<slot>/icons/LICENSES/Apache-2.0.txt`). This check resolves a recorded path relative to
//      the slot first and the repository root second, and reports the ambiguity as a note the first
//      time the repository-root reading is the one that resolves.
//   2. §7 requires "a written justification field" for a second library but §1.2's schema does not
//      name one. A set-level `justification` string is accepted, and the finding says so.
//   3. §6 exempts a literal colour "other than a mask". VectorDrawable has no `<mask>` element — the
//      nearest thing is `<clip-path>` — so the carve-out is not implemented and its absence is
//      reported rather than guessed at.
//   4. Rule 2's orphan rule ("a committed `res/drawable/ic_*.xml` that is not in some
//      `sets[].icons[].file`") collides with rule 13, which requires the launcher mark to be a
//      slot-owned `ic_launcher_*.xml` that must never be listed as a library icon. Orphan detection
//      is therefore deliberately NOT implemented and this module says so instead of inventing a
//      carve-out.
//
// Honest limits, stated because a sensor that overclaims is worse than none:
//
//   - Nothing here proves the bytes came from the URL the record names. There is no network access
//     in a core check, so `sourceUrl` is checked for the HTTPS scheme it must have, never followed.
//   - There is deliberately no "hand-drawn glyph" heuristic. It would be noisy, and the research
//     that motivated this contract warns that a noisy gate is read past. "Is this glyph beautiful,
//     and is it identity or vocabulary" stays the owner's judgement at the phase-1 review; this
//     module measures the record, the placeholder, the licence text, the deprecated coordinate and
//     the literal colour, each of which is objective.
//   - `icons.tint` reads literal colours in drawable XML only. A literal `tint = Color(0xFF…)` in a
//     Compose `Icon(...)` call is not read; that is a Kotlin scan, and `locale.hardcoded` is the
//     module that owns comment-aware Kotlin reading.
//   - `icons.launcher` compares normalised `android:pathData` against the scaffold's, not raw bytes.
//     A copy whose geometry was edited to evade the comparison is not detected; the geometry is what
//     this leg measures, and the reason is stated at `launcherProblems()`.
import { createHash } from 'node:crypto'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { SCRATCH_DIR } from '../lib/scratch.mjs'
import path from 'node:path'
import { baselineVerdict } from './baseline.mjs'
import { decodeSource } from './build.mjs'
import { expectEqual, expectTrue, guard, ROOT, runFixtures, unverified } from './lib.mjs'
import { walkSlotFiles } from './manifest.mjs'

/** The check ids this module owns, in report order. */
export const ICON_IDS = Object.freeze([
  'icons.mark', // the declared mark is registered in the Python-owned MARK_REGISTRY
  'icons.launcher', // the launcher vectors are the slot's own, not the scaffold's placeholder art
  'icons.provenance', // icons/provenance.json resolves: schema, allowlist, files, sha256
  'icons.licence', // every declared spdx has its committed, non-empty licence text
  'icons.offline', // no icon artifact is fetched at build or run time
  'icons.deprecated', // no deprecated androidx material-icons coordinate
  'icons.tint' // UI iconography carries no literal colour (the mark is excluded by design)
])

/** The scaffold a new slot is copied from. Named once, so this module and its fixtures cannot drift. */
export const SCAFFOLD_DIR = path.join(ROOT, 'scaffold', 'android-compose')

const ICON_DESIGN_REL = 'scripts/lib/icon_design.py'
const ICON_DESIGN = path.join(ROOT, 'scripts', 'lib', 'icon_design.py')
const STUDIO_REL = 'store/studio.json'
const PROVENANCE_REL = 'icons/provenance.json'
const LICENCES_REL = 'icons/LICENSES'
const DRAWABLE_REL = 'app/src/main/res/drawable'
const BUILD_FILES = ['app/build.gradle.kts', 'gradle/libs.versions.toml', 'build.gradle.kts']
const BUILD_SCAN_FILES = [...BUILD_FILES, 'settings.gradle.kts', 'gradle.properties']

/** The three vectors the slot's mark is delivered through. Their role is stated at `tintProblems()`. */
export const MARK_VECTOR_NAMES = Object.freeze([
  'ic_launcher_foreground.xml',
  'ic_launcher_background.xml',
  'ic_launcher_monochrome.xml'
])
const DRAWABLE_EXT = /\.xml$/i
const MARK_ARTIFACT = /^ic_launcher/i

/** The §2 allowlist, verbatim. `LicenseRef-Remix-Icon-1.0` is §2.1's one conditional entry. */
export const SPDX_ALLOWLIST = Object.freeze([
  'MIT',
  'ISC',
  'Apache-2.0',
  'OFL-1.1',
  'CC-BY-4.0',
  'MIT-0',
  'BSD-2-Clause',
  'BSD-3-Clause',
  'LicenseRef-Remix-Icon-1.0'
])

/** The §3 denylist, as patterns, each carrying the reason the contract gives. Order is significant. */
export const SPDX_DENY = Object.freeze([
  {
    pattern: /CC-BY-NC/i,
    why: 'NonCommercial — the material may not be used for commercial purposes, and every slot is sold or ad-supported'
  },
  { pattern: /CC-BY-ND/i, why: 'NoDerivatives — a converted VectorDrawable is a derivative' },
  { pattern: /CC-BY-SA/i, why: 'ShareAlike — incompatible with a paid closed-source app' },
  {
    pattern: /\b(?:A?L?GPL)-/i,
    why: 'copyleft — distribution can oblige release of the covered work under the same terms'
  },
  { pattern: /^NOASSERTION$/i, why: 'no licence was asserted, and an unknown licence is not a permissive one' }
])

const relIn = (base, file) => path.relative(base, file).split(path.sep).join('/')
const relRepo = (file) => path.relative(ROOT, file).split(path.sep).join('/')
const lineOf = (text, index) => text.slice(0, index).split(/\r?\n/).length
const dedupe = (values) => [...new Set(values)]

/** The first line a pattern matches, or 1 when it matches nothing — never a slice(0, -1) surprise. */
function lineOfFirst(text, pattern) {
  const index = String(text).search(pattern)
  return index < 0 ? 1 : lineOf(text, index)
}

/** The comment syntax of a scanned file, chosen by extension so `.toml`'s `#` is read correctly. */
export function commentKind(rel) {
  const ext = path.extname(String(rel)).toLowerCase()
  if (ext === '.xml') return 'xml'
  if (ext === '.toml') return 'toml'
  if (ext === '.properties') return 'properties'
  return 'c'
}

/**
 * Text with comments blanked out, keeping every byte offset and every line so `lineOf` agrees with
 * the file, and string literals left intact so a real URL is still read as code.
 *
 * The distinction this exists to make (audit 2026-10-02, F6/F7): a URL or an artifact name inside a
 * comment is documentation, and the contract *asks* slots to record where their icons came from, so
 * punishing that prose as though the build fetched it is a false positive. The naive fixes both
 * fail: a regex over raw text cannot tell `// source: https://…` from code, and stripping `//` to
 * end of line would truncate every `https://` inside a string literal and hide the real fetch this
 * leg must still catch. So the scanner walks the text, skips string literals (with escapes, and
 * Kotlin's `"""` raw strings), and blanks only line and block comments, `#` and XML/`.properties`
 * comments. It never rewrites the text — comments become spaces — so no match index or line number
 * moves.
 */
export function stripComments(text, kind = 'c') {
  const src = String(text)
  const out = src.split('')
  const blank = (from, to) => {
    for (let i = from; i < to && i < src.length; i += 1) if (src[i] !== '\n') out[i] = ' '
  }
  if (kind === 'xml') {
    for (const match of src.matchAll(/<!--[\s\S]*?-->/g)) blank(match.index, match.index + match[0].length)
    return out.join('')
  }
  if (kind === 'properties') {
    for (const match of src.matchAll(/^[ \t]*[#!].*$/gm)) blank(match.index, match.index + match[0].length)
    return out.join('')
  }
  const hash = kind === 'toml'
  let index = 0
  while (index < src.length) {
    const char = src[index]
    if (char === '"' || char === "'") {
      const triple = `${char}${char}${char}`
      if (src.startsWith(triple, index)) {
        const end = src.indexOf(triple, index + 3)
        index = end < 0 ? src.length : end + 3
        continue
      }
      index += 1
      while (index < src.length) {
        if (src[index] === '\\') {
          index += 2
          continue
        }
        if (src[index] === char) {
          index += 1
          break
        }
        if (src[index] === '\n') break
        index += 1
      }
      continue
    }
    if (hash && char === '#') {
      let end = index
      while (end < src.length && src[end] !== '\n') end += 1
      blank(index, end)
      index = end
      continue
    }
    if (char === '/' && src[index + 1] === '/') {
      let end = index
      while (end < src.length && src[end] !== '\n') end += 1
      blank(index, end)
      index = end
      continue
    }
    if (char === '/' && src[index + 1] === '*') {
      const close = src.indexOf('*/', index + 2)
      const end = close < 0 ? src.length : close + 2
      blank(index, end)
      index = end
      continue
    }
    index += 1
  }
  return out.join('')
}

/**
 * The slot's declared palette as a comparable role set, upper-cased, with both readings of an
 * eight-digit literal (`#RRGGBBAA` and `#AARRGGBB`) reduced to their six-digit core.
 *
 * The two forms are genuinely ambiguous in the wild and the studio emits `#FFFFFFFF` where its
 * palette says `#FFFFFF`; without this a correct slot would be reported as off-palette, which is the
 * false positive F9's warning is about.
 */
export function paletteRoleSet(palette = []) {
  const roles = new Set()
  for (const value of palette) {
    if (typeof value !== 'string') continue
    const colour = value.trim().toUpperCase()
    if (!/^#[0-9A-F]{3,8}$/.test(colour)) continue
    roles.add(colour)
    if (colour.length === 9) {
      roles.add(`#${colour.slice(1, 7)}`)
      roles.add(`#${colour.slice(3, 9)}`)
    }
  }
  return roles
}

/** Whether a literal colour is a role of the declared palette, honouring the eight-digit readings. */
export function paletteHasRole(colour, roles) {
  if (roles.size === 0) return false
  const value = String(colour).trim().toUpperCase()
  if (roles.has(value)) return true
  if (value.length === 9) return roles.has(`#${value.slice(1, 7)}`) || roles.has(`#${value.slice(3, 9)}`)
  return false
}

// ---------------------------------------------------------------------------------------------
// Leg A — the declared mark is registered (docs/icon-provenance.md §9)
// ---------------------------------------------------------------------------------------------

/**
 * The top-level string keys of one Python dict literal, by brace matching from the declaration.
 *
 * A key is collected only where the closing `:` sits at the literal's own depth, so keys of nested
 * dicts (a registry entry's `primitives`, `keyline`, `note`) are not read as marks. Comments are
 * skipped, because a commented-out entry is not a registered one.
 */
export function pythonDictKeys(source, name) {
  const text = String(source)
  const declaration = new RegExp(`^${name}\\b`, 'm').exec(text)
  if (!declaration) return null
  const open = text.indexOf('{', declaration.index)
  if (open < 0) return null

  const keys = []
  let depth = 0
  let pending = null
  let index = open
  while (index < text.length) {
    const char = text[index]
    if (char === '#') {
      while (index < text.length && text[index] !== '\n') index += 1
      continue
    }
    if (char === '"' || char === "'") {
      const quote = char
      let body = ''
      index += 1
      while (index < text.length && text[index] !== quote) {
        if (text[index] === '\\') {
          body += text[index + 1] ?? ''
          index += 2
          continue
        }
        body += text[index]
        index += 1
      }
      index += 1
      pending = body
      continue
    }
    if (char === '{' || char === '[' || char === '(') {
      depth += 1
      index += 1
      continue
    }
    if (char === '}' || char === ']' || char === ')') {
      depth -= 1
      if (depth === 0) break
      index += 1
      continue
    }
    if (char === ':' && depth === 1 && pending !== null) {
      keys.push(pending)
      pending = null
      index += 1
      continue
    }
    if (!/\s/.test(char)) pending = null
    index += 1
  }
  return keys
}

/** `MARK_REGISTRY`'s marks and `DEFAULT_MARK`, or null when the module could not be read. */
export function readMarkRegistry(source) {
  const marks = pythonDictKeys(source, 'MARK_REGISTRY')
  if (!marks || marks.length === 0) return null
  const fallback = /^DEFAULT_MARK\s*=\s*["']([^"']+)["']/m.exec(String(source))
  return { marks, defaultMark: fallback ? fallback[1] : null }
}

/** Why a `store/studio.json` mark does not resolve, or an empty list when it does. */
export function markProblems({ declaredMark = null, hasMarkKey = true, registry }) {
  const problems = []
  if (!hasMarkKey || typeof declaredMark !== 'string' || declaredMark.trim() === '') {
    problems.push(
      `${STUDIO_REL} declares no \`mark\`, so the launcher art is not derived from a declared ` +
        `identity; the studio would fall back to the registry's default ${registry.defaultMark === null ? '(unread)' : `'${registry.defaultMark}'`}`
    )
    return { problems }
  }
  const mark = declaredMark.trim()
  if (registry.marks.includes(mark)) return { problems }
  problems.push(
    `${STUDIO_REL} declares mark '${mark}', which is absent from MARK_REGISTRY in ${ICON_DESIGN_REL} ` +
      `(registered: ${registry.marks.join(', ')}); mark_primitives('${mark}') would raise ` +
      `SystemExit("BLOCKED: unknown studio mark"), so the studio cannot render this slot's launcher icon`
  )
  return { problems }
}

// ---------------------------------------------------------------------------------------------
// Leg B — the launcher vectors are the slot's own (rule 13)
// ---------------------------------------------------------------------------------------------

/**
 * Every `pathData` attribute in a vector, whatever namespace prefix it carries and whichever quote
 * style it uses, whitespace-normalised and order-preserved.
 *
 * The prefix and the quote are not geometry, so they must not be the evasion. XML allows any prefix
 * for `http://schemas.android.com/apk/res/android` and either quote, and aapt reads all four
 * combinations identically; a reader that matched only the literal `android:` + double quote reported
 * a byte-identical scaffold copy as `PASS` (audit 2026-10-02, F2/F3).
 * Comparison is on geometry rather than on raw bytes, and the choice matters:
 *
 *   - A byte hash is brittle for the thing it is meant to catch. The same art reformatted, reindented
 *     or checked out with different line endings changes every byte and stops matching, so a slot that
 *     still ships the placeholder would read clean. The mark's *identity* is its geometry, not its
 *     whitespace.
 *   - Colour is the wrong identity here. A slot's palette may legitimately recolour a shape, and the
 *     Android 13+ monochrome layer is required to be a single tinted colour at all, so a colour-set
 *     comparison would both flag legitimate work and be evaded by recolouring the placeholder.
 *   - Geometry is the identity the factory already trusts elsewhere: `scripts/check-adaptive-icon.py`
 *     and `scripts/core/icon.mjs` read `android:pathData` as the mark's reach, so this leg uses the
 *     same signal rather than inventing a second one.
 *
 * The under-approximation is stated in the module header: a copy whose geometry was edited is not
 * detected, and the colours are printed in the finding as corroborating evidence, not as the test.
 */
export function vectorPaths(text) {
  const stripped = stripComments(String(text), 'xml')
  return [...stripped.matchAll(/(?:[\w.-]+):pathData\s*=\s*(?:"([^"]*)"|'([^']*)')/g)].map((match) =>
    (match[1] ?? match[2] ?? '').replace(/\s+/g, '')
  )
}

/** Every literal colour a vector declares, upper-cased, for the finding's evidence line. */
export function vectorColours(text) {
  const stripped = stripComments(String(text), 'xml')
  const colours = [
    ...stripped.matchAll(
      /(?:[\w.-]+):(?:fillColor|strokeColor|startColor|endColor|centerColor)\s*=\s*(?:"(#[0-9A-Fa-f]{3,8})"|'(#[0-9A-Fa-f]{3,8})')/g
    )
  ].map((match) => (match[1] ?? match[2]).toUpperCase())
  return [...new Set(colours)]
}

/** The sorted geometry set, so reordering the paths of a copy does not evade the comparison. */
export const vectorGeometry = (text) => [...vectorPaths(text)].sort().join('|')

/** Any `pathData` attribute, whatever prefix it carries, for the finding's line number. */
const PATH_DATA_ATTR = /(?:[\w.-]+):pathData\s*=/

/**
 * A slot whose launcher art is the scaffold's placeholder, or whose own art has left the palette.
 *
 * Two criteria, both of them the contract's (rule 13 and §6):
 *
 *   - the geometry must not be the scaffold's, and
 *   - every literal fill of a slot's own vector must be a role of the slot's declared palette, once
 *     the eight-digit alpha forms are reduced to their six-digit colour.
 *
 * The second is the half the doc claimed and the code did not do (audit 2026-10-02, F9): the palette
 * only ever appeared in a message, so a vector with new geometry and a wholly foreign fill reported
 * `PASS`. It is skipped when the slot declares no palette at all — a killed slot with only headlines
 * (the historic `pipe-sizing` studio) has no identity to be off, and inventing a finding there is the
 * false positive F9 warns about.
 */
export function launcherProblems({ slotVectors = {}, scaffoldVectors = {}, palette = [] }) {
  const problems = []
  const paletteRoles = paletteRoleSet(palette)
  for (const name of MARK_VECTOR_NAMES) {
    const slot = slotVectors[name]
    const scaffold = scaffoldVectors[name]
    if (!slot || !scaffold) continue
    const line = lineOfFirst(slot.text, PATH_DATA_ATTR)
    const colours = vectorColours(slot.text)
    if (vectorGeometry(slot.text) === vectorGeometry(scaffold.text)) {
      const offPalette = colours.filter((colour) => !paletteHasRole(colour, paletteRoles))
      const paletteNote =
        paletteRoles.size > 0 && offPalette.length > 0
          ? `, and ${offPalette.join(', ')} appear(s) in no role of ${STUDIO_REL}'s palette`
          : ''
      problems.push(
        `${slot.rel}:${line}: the same geometry as ${scaffold.rel} ` +
          `(${vectorPaths(slot.text).length} identical path(s), fill colour(s) ${colours.join(', ') || 'none'}${paletteNote})` +
          `, so this slot still ships the scaffold's placeholder launcher art rather than its own mark`
      )
      continue
    }
    if (paletteRoles.size === 0) continue
    const offPalette = colours.filter((colour) => !paletteHasRole(colour, paletteRoles))
    if (offPalette.length > 0) {
      problems.push(
        `${slot.rel}:${line}: the launcher vector has its own geometry, but fill colour(s) ` +
          `${offPalette.join(', ')} appear(s) in no role of ${STUDIO_REL}'s palette, so the slot's mark has drifted ` +
          'off the identity it declares (rule 13): every colour of the mark is a palette role, and an ' +
          'eight-digit literal is read as its six-digit colour with alpha'
      )
    }
  }
  return problems
}

/** Every platform vector the slot (or the scaffold) ships, keyed by file name. */
export function readLauncherVectors(dir, describe = (abs) => relIn(dir, abs)) {
  const out = {}
  for (const name of MARK_VECTOR_NAMES) {
    const abs = path.join(dir, ...DRAWABLE_REL.split('/'), name)
    if (!existsSync(abs)) continue
    out[name] = { abs, rel: describe(abs), text: readFileSync(abs, 'utf8') }
  }
  return out
}

/** SHA-256 of a committed file, lowercase hex — the value the record must carry. */
export function sha256Of(file) {
  return createHash('sha256').update(readFileSync(file)).digest('hex')
}

const sha256Text = (text) => createHash('sha256').update(Buffer.from(text, 'utf8')).digest('hex')

/**
 * A recorded path, resolved the two ways the contract states, or `null` when neither exists.
 *
 * §1.2's example uses the slot-relative form and its own field table plus its `licenceFile` example
 * use the repo-relative one; both are accepted rather than one being invented as the correct reading.
 */
export function resolveRecordedPath(slotDir, recorded, rootDir = ROOT) {
  const normalised = String(recorded).replace(/\\/g, '/').replace(/^\.\//, '')
  if (normalised === '') return null
  const slotCandidate = path.join(slotDir, ...normalised.split('/'))
  if (existsSync(slotCandidate)) return { file: slotCandidate, how: 'slot-relative' }
  const repoCandidate = path.join(rootDir, ...normalised.split('/'))
  if (existsSync(repoCandidate)) return { file: repoCandidate, how: 'repo-relative' }
  return null
}

/**
 * Every problem in one `icons/provenance.json`, as findings a reader can act on.
 *
 * `rootDir` is a parameter so a fixture can exercise the repo-relative reading without touching
 * `apps/`; the run-time caller leaves it at the repository root.
 */
export function provenanceProblems(slotDir, record, options = {}) {
  const rootDir = options.rootDir || ROOT
  const relFile = options.relFile || PROVENANCE_REL
  const problems = []
  const notes = []
  let sawRepoRelative = false

  if (record === null || typeof record !== 'object' || Array.isArray(record)) {
    return { problems: [`${relFile}: the record is not a JSON object`], notes }
  }
  if (record.schemaVersion !== 1) {
    problems.push(
      `${relFile}: schemaVersion is ${JSON.stringify(record.schemaVersion)}, expected 1 — a shape this ` +
        'check does not know must not read as a record it read'
    )
  }
  if (!Array.isArray(record.sets) || record.sets.length === 0) {
    problems.push(
      `${relFile}: \`sets\` is ${Array.isArray(record.sets) ? 'empty' : 'not an array'}, so no vendored icon set is declared`
    )
    return { problems, notes }
  }

  const libraries = new Set()
  record.sets.forEach((set, setIndex) => {
    const where = `${relFile}: sets[${setIndex}]`
    if (set === null || typeof set !== 'object' || Array.isArray(set)) {
      problems.push(`${where} is not an object`)
      return
    }
    if (typeof set.library !== 'string' || set.library.trim() === '') problems.push(`${where}: \`library\` is missing or empty`)
    else libraries.add(set.library.trim())

    if (typeof set.version !== 'string' || set.version.trim() === '') {
      problems.push(`${where}: \`version\` is missing or empty; the pinned release, tag or dated snapshot is required`)
    } else if (/^(?:latest|main|master|head|trunk|develop|dev)$/i.test(set.version.trim())) {
      problems.push(`${where}: \`version\` is '${set.version}', which is a moving reference; pin a release, tag or dated snapshot`)
    }

    if (typeof set.spdx !== 'string' || set.spdx.trim() === '') {
      problems.push(`${where}: \`spdx\` is missing or empty; an absent identifier is a failure, not a note`)
    } else {
      const spdx = set.spdx.trim()
      const denied = SPDX_DENY.find((entry) => entry.pattern.test(spdx))
      if (denied) problems.push(`${where}: spdx '${spdx}' is a denied licence (${denied.why}) and must never be bundled`)
      else if (!SPDX_ALLOWLIST.includes(spdx)) {
        problems.push(
          `${where}: spdx '${spdx}' is not on the §2 allowlist (${SPDX_ALLOWLIST.join(', ')}); a custom ` +
            '`LicenseRef-` identifier is accepted only for Remix Icon (§2.1)'
        )
      }
    }

    if (typeof set.sourceUrl !== 'string' || !/^https:\/\//i.test(set.sourceUrl.trim())) {
      problems.push(`${where}: \`sourceUrl\` is ${JSON.stringify(set.sourceUrl)}; an HTTPS URL of the canonical source is required`)
    }

    if (typeof set.licenceFile !== 'string' || set.licenceFile.trim() === '') {
      problems.push(`${where}: \`licenceFile\` is missing or empty; the committed licence text is the obligation for every permissive set`)
    } else {
      const resolved = resolveRecordedPath(slotDir, set.licenceFile.trim(), rootDir)
      if (!resolved) {
        problems.push(
          `${where}: \`licenceFile\` '${set.licenceFile}' resolves to no committed file (tried it relative to the ` +
            `slot and to the repository root), so the licence text does not travel with the set`
        )
      } else {
        if (resolved.how === 'repo-relative') sawRepoRelative = true
        const spdx = typeof set.spdx === 'string' ? set.spdx.trim() : ''
        let regular = false
        try {
          const stat = statSync(resolved.file)
          if (stat.isFile()) regular = true
          else {
            problems.push(
              `${where}: \`licenceFile\` '${set.licenceFile}' is a directory, not the committed licence text, so the ` +
                'existence check alone would read as an obligation that does not exist'
            )
          }
        } catch (error) {
          problems.push(`${where}: \`licenceFile\` '${set.licenceFile}' could not be inspected: ${error.message}`)
        }
        if (regular) {
          if (spdx !== '' && path.basename(resolved.file) !== `${spdx}.txt`) {
            problems.push(
              `${where}: \`licenceFile\` '${set.licenceFile}' is named ${path.basename(resolved.file)}, not ` +
                `'${spdx}.txt', so the committed text is not tied to the declared spdx (§5): the file is the ` +
                'obligation for the licence it names'
            )
          }
          let text = null
          try {
            text = readFileSync(resolved.file, 'utf8')
          } catch (error) {
            problems.push(`${where}: \`licenceFile\` '${set.licenceFile}' could not be read: ${error.message}`)
          }
          if (text !== null && text.trim() === '') {
            problems.push(`${where}: \`licenceFile\` '${set.licenceFile}' is empty, so the licence text of '${spdx}' was not committed`)
          }
        }
      }
    }

    if (!Object.prototype.hasOwnProperty.call(set, 'attribution')) {
      problems.push(`${where}: \`attribution\` is absent; it is required and may be \`null\``)
    } else if (set.attribution !== null) {
      const attribution = set.attribution
      if (typeof attribution !== 'object' || Array.isArray(attribution)) {
        problems.push(`${where}: \`attribution\` must be \`null\` or an object \`{ text, url, inApp }\``)
      } else {
        if (typeof attribution.text !== 'string' || attribution.text.trim() === '') problems.push(`${where}: attribution.text is missing or empty`)
        if (typeof attribution.url !== 'string' || attribution.url.trim() === '') problems.push(`${where}: attribution.url is missing or empty`)
        if (typeof attribution.inApp !== 'boolean') problems.push(`${where}: attribution.inApp must be a boolean`)
      }
    }
    if (typeof set.spdx === 'string' && set.spdx.trim() === 'CC-BY-4.0') {
      const reachable =
        set.attribution &&
        typeof set.attribution === 'object' &&
        set.attribution.inApp === true &&
        typeof set.attribution.text === 'string' &&
        set.attribution.text.trim() !== ''
      if (!reachable) {
        problems.push(
          `${where}: CC-BY-4.0 requires attribution ("give appropriate credit, provide a link to the ` +
            'license, and indicate if changes were made"), so `attribution.inApp` must be `true` with its text ' +
            'and be reachable from inside the app'
        )
      }
    }

    if (typeof set.fetchedAt !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(set.fetchedAt.trim())) {
      problems.push(`${where}: \`fetchedAt\` is ${JSON.stringify(set.fetchedAt)}; an ISO date (YYYY-MM-DD) is required`)
    }
    if (typeof set.fetchedBy !== 'string' || set.fetchedBy.trim() === '') problems.push(`${where}: \`fetchedBy\` is missing or empty`)

    if (!Array.isArray(set.icons) || set.icons.length === 0) {
      problems.push(`${where}: \`icons\` is ${Array.isArray(set.icons) ? 'empty' : 'not an array'}; one entry per committed icon is required`)
      return
    }
    set.icons.forEach((icon, iconIndex) => {
      const at = `${where}.icons[${iconIndex}]`
      if (icon === null || typeof icon !== 'object' || Array.isArray(icon)) {
        problems.push(`${at} is not an object`)
        return
      }
      if (typeof icon.name !== 'string' || icon.name.trim() === '') problems.push(`${at}: \`name\` is missing or empty`)
      if (typeof icon.sourcePath !== 'string' || icon.sourcePath.trim() === '') {
        problems.push(`${at}: \`sourcePath\` is missing or empty; the path inside the upstream repository is required`)
      }
      const hashWellFormed = typeof icon.sha256 === 'string' && /^[0-9a-f]{64}$/.test(icon.sha256)
      if (!hashWellFormed) {
        problems.push(`${at}: \`sha256\` is ${JSON.stringify(icon.sha256)}; 64 lowercase hex characters are required`)
      }
      if (typeof icon.file !== 'string' || icon.file.trim() === '') {
        problems.push(`${at}: \`file\` is missing or empty`)
        return
      }
      const resolved = resolveRecordedPath(slotDir, icon.file.trim(), rootDir)
      if (!resolved) {
        problems.push(
          `${at}: \`file\` '${icon.file}' is not committed (tried it relative to the slot and to the ` +
            'repository root), so a listed icon that is not there cannot be delivered'
        )
        return
      }
      if (resolved.how === 'repo-relative') sawRepoRelative = true
      let regular = false
      try {
        const stat = statSync(resolved.file)
        if (stat.isFile()) regular = true
        else {
          problems.push(
            `${at}: \`file\` '${icon.file}' resolves to a directory, not a committed file, so the listed icon ` +
              'cannot be delivered'
          )
        }
      } catch (error) {
        problems.push(`${at}: \`file\` '${icon.file}' could not be inspected: ${error.message}`)
      }
      if (regular && hashWellFormed) {
        let actual = null
        try {
          actual = sha256Of(resolved.file)
        } catch (error) {
          problems.push(`${at}: ${icon.file} could not be read for its sha256: ${error.message}`)
        }
        if (actual !== null && actual !== icon.sha256) {
          problems.push(
            `${at}: ${icon.file} hashes to ${actual}, not the recorded ${icon.sha256}, so the committed ` +
              'bytes are not the bytes the record describes'
          )
        }
      }
    })
  })

  if (libraries.size > 1) {
    const unjustified = record.sets
      .slice(1)
      .filter((set) => !set || typeof set.justification !== 'string' || set.justification.trim() === '')
    if (unjustified.length > 0) {
      problems.push(
        `${relFile}: ${libraries.size} distinct libraries are declared (${[...libraries].join(', ')}) and ` +
          `${unjustified.length} set(s) after the first carry no \`justification\`; §7 allows a second library ` +
          'only with a written justification. §1.2 does not name this field, so a set-level `justification` ' +
          'string is what this check accepts — the field name is a contract ambiguity, reported and not resolved.'
      )
    }
  }
  if (sawRepoRelative) {
    notes.push(
      'a recorded path resolved relative to the repository root, which §1.2 states in its field table while ' +
        'its own example writes the slot-relative form — both are accepted because the contract states two'
    )
  }
  return { problems, notes }
}

/** The §5 half: every distinct `spdx` has a non-empty committed `icons/LICENSES/<SPDX>.txt`. */
export function licenceProblems(slotDir, record) {
  const problems = []
  const notes = []
  if (record === null || typeof record !== 'object' || !Array.isArray(record.sets)) return { problems, notes, declared: [] }
  const declared = dedupe(record.sets.map((set) => (set && typeof set.spdx === 'string' ? set.spdx.trim() : '')).filter((spdx) => spdx !== ''))
  const dir = path.join(slotDir, ...LICENCES_REL.split('/'))
  for (const spdx of declared) {
    const file = path.join(dir, `${spdx}.txt`)
    if (!existsSync(file)) {
      problems.push(
        `${LICENCES_REL}/${spdx}.txt does not exist, so the licence text of '${spdx}' does not travel with the ` +
          'vendored set; the committed text is the whole obligation for a permissive licence (§5)'
      )
      continue
    }
    let text = null
    try {
      text = readFileSync(file, 'utf8')
    } catch (error) {
      problems.push(`${LICENCES_REL}/${spdx}.txt could not be read: ${error.message}`)
      continue
    }
    if (text.trim() === '') problems.push(`${LICENCES_REL}/${spdx}.txt is empty, so the licence text of '${spdx}' was not committed`)
  }
  const declaredFiles = new Set(declared.map((spdx) => `${spdx}.txt`))
  if (existsSync(dir)) {
    const extra = readdirSync(dir).filter((name) => !declaredFiles.has(name))
    if (extra.length > 0) notes.push(`${LICENCES_REL}/ carries ${extra.join(', ')}, which no set's spdx references — remove or reference it`)
  }
  return { problems, notes, declared }
}

// ---------------------------------------------------------------------------------------------
// Leg D — no icon artifact is fetched at build or run time (rule 6)
// ---------------------------------------------------------------------------------------------

/** The hosts the build and shipped scans share, stated once so the two lists cannot drift apart. */
const FONT_CDN_HOSTS = 'fonts\\.googleapis\\.com|fonts\\.gstatic\\.com'
const ICON_CDN_HOSTS = 'iconify|iconfont\\.cn|fontsource|flaticon|icons8|svgrepo|lucide\\.dev'

const REMOTE_ASSET_SHAPE = [/https?:\/\/[^\s"'<>)]+\.(?:svg|woff2?|ttf|otf|eot)\b/gi, 'a remote font/icon asset']
const CDN_SHAPE = [
  new RegExp(`https?://[^\\s"'<>)]*(?:${FONT_CDN_HOSTS}|${ICON_CDN_HOSTS})`, 'gi'),
  'an icon/font CDN URL'
]
const DOWNLOAD_PLUGIN_SHAPE = [
  // The dots in the coordinates sit in a character class on purpose: `build.namespaceSweep` reads a
  // plain `com.<word>` in `scripts/` as a foreign package *identity*, and these are detector data —
  // third-party download plugins this leg refuses — not a namespace the factory declares.
  /\b(?:de\.undercouch\.download|download\.run|gradle-download-task|com[.]github[.]michel-kraemer|com[.]novoda[.]download)\b/gi,
  'a Gradle download plugin'
]
const NPM_ICON_PACKAGE_SHAPE = [
  /\b(?:fontsource|@iconify|iconify|svg-to-compose|compose-icons)\b/gi,
  'an npm-style icon package consumed by a build step'
]
const DOWNLOADABLE_FONT_SHAPE = [
  /\b(?:androidx\.compose\.ui\.text\.googlefonts|googlefonts\.GoogleFont|com\.google\.android\.gms\.fonts)\b/gi,
  'a downloadable-font provider fetch at run time'
]

/**
 * The shapes a build file must not carry, and the shapes a shipped source must not carry.
 *
 * The shipped list is a **superset** of the build list (audit 2026-10-02, F4): before this the
 * shipped half named only Google Fonts, so `https://api.iconify.design/mdi.json?icons=plus` — a real
 * runtime icon fetch with no `.svg` extension — produced a row in a build file and no row in shipped
 * Kotlin. Both halves now read the same three shared shapes plus the run-time downloadable-font
 * provider, so the rule cannot be escaped by moving the URL one file over.
 */
const BUILD_FETCH_SHAPES = [REMOTE_ASSET_SHAPE, CDN_SHAPE, DOWNLOAD_PLUGIN_SHAPE, NPM_ICON_PACKAGE_SHAPE]
const SHIPPED_FETCH_SHAPES = [
  REMOTE_ASSET_SHAPE,
  CDN_SHAPE,
  DOWNLOAD_PLUGIN_SHAPE,
  NPM_ICON_PACKAGE_SHAPE,
  DOWNLOADABLE_FONT_SHAPE
]

const clip = (value) => (value.length > 72 ? `${value.slice(0, 69)}…` : value)

/**
 * A remote icon artifact declared in a build file, or an icon/font URL reached from shipped sources.
 *
 * Offline is a repository rule (`AGENTS.md` "a build must not depend on the network"), and the
 * contract's failure list is the shapes below. Vendored bytes under `res/` are the pass shape; this
 * leg cannot see a download done by hand during development, and does not claim to. A URL that sits
 * in a comment is documentation — the contract *asks* slots to record where their icons came from —
 * so comments are blanked before matching (`stripComments`), never treated as a declared fetch.
 */
export function offlineProblems({ buildTexts = [], shipped = [] }) {
  const findings = new Set()
  for (const { rel, text } of buildTexts) {
    const clean = stripComments(text, commentKind(rel))
    for (const [pattern, what] of BUILD_FETCH_SHAPES) {
      for (const match of clean.matchAll(pattern)) {
        findings.add(
          `${rel}:${lineOf(clean, match.index)}: ${what} (${clip(match[0])}) is declared in a build file, so ` +
            'the build would depend on the network; an icon is fetched once during development and committed (§4, rule 6)'
        )
      }
    }
  }
  for (const { rel, text } of shipped) {
    const clean = stripComments(text, commentKind(rel))
    for (const [pattern, what] of SHIPPED_FETCH_SHAPES) {
      for (const match of clean.matchAll(pattern)) {
        findings.add(
          `${rel}:${lineOf(clean, match.index)}: ${what} (${clip(match[0])}) is reached from a shipped source, so ` +
            'the icon is fetched at run time; vendored bytes under res/ are the pass shape (§4, rule 6)'
        )
      }
    }
  }
  return [...findings]
}

// ---------------------------------------------------------------------------------------------
// Leg E — no deprecated Material icon artifact (rule 8)
// ---------------------------------------------------------------------------------------------

const DEPRECATED_ICON_ARTIFACT = /\bmaterial-icons(?:-(?:core|extended))?\b/g

/**
 * A deprecated `material-icons-*` coordinate in a real dependency line.
 *
 * A comment that mentions the artifact the slot removed is not a coordinate, so comments are blanked
 * first (audit 2026-10-02, F7); the coordinate in an `implementation(...)` line is preserved and
 * still fails.
 */
export function deprecatedProblems({ buildTexts = [] }) {
  const findings = new Set()
  for (const { rel, text } of buildTexts) {
    const clean = stripComments(text, commentKind(rel))
    for (const match of clean.matchAll(DEPRECATED_ICON_ARTIFACT)) {
      findings.add(
        `${rel}:${lineOf(clean, match.index)}: '${match[0]}' is a deprecated Material icon artifact, removed from ` +
          `current Material 3 and pinned at 1.7.3, so it cannot resolve from this factory's Compose BOM; download ` +
          'the VectorDrawable XML from the Material Symbols "Android" tab instead (§4.1, rule 8)'
      )
    }
  }
  return [...findings]
}

// ---------------------------------------------------------------------------------------------
// Leg F — UI iconography takes its tint from the theme (rule 10)
// ---------------------------------------------------------------------------------------------

/**
 * `android:fillColor` only, any namespace prefix and either quote style.
 *
 * Rule 10's failure shape is written about `fillColor` specifically, and reading the bare
 * `android:color` too failed a `<ripple android:color="#…">` that is not a path fill at all (audit
 * 2026-10-02, F8). Gradient stops and `strokeColor` are deliberately not read by this leg; the
 * module's honest-limit note says so.
 */
const LITERAL_FILL = /(?:[\w.-]+):fillColor\s*=\s*(?:"(#[0-9A-Fa-f]{3,8})"|'(#[0-9A-Fa-f]{3,8})')/g

/**
 * A literal `fillColor` on a UI icon drawable, with the slot's own identity excluded — named out loud.
 *
 * The distinction this leg must not blur, and the one it used to: the slot's **mark** may be the
 * launcher vectors (`ic_launcher_foreground.xml` / `ic_launcher_background.xml` /
 * `ic_launcher_monochrome.xml`) **or an in-app brand glyph**, and both are slot-owned identity that
 * legitimately carries the palette (§9, rule 13 — the launcher half is also the subject of
 * `icons.launcher`). Everything else is UI iconography — the vocabulary a library set also provides —
 * and taking its colour from the theme is what keeps a stock SVG with a baked fill from entering the
 * app.
 *
 * The line between the two is the palette, not the file name (F8): a literal fill that **is a role of
 * the slot's own declared palette** is reported as a note naming the file, because a slot-owned mark
 * drawn in the slot's own colours is identity doing its job; a literal fill that is **not** a palette
 * role is a failure, whether it is a baked stock colour or an off-identity one. When the slot declares
 * no palette there is no identity to appeal to, so every literal fill fails — the conservative
 * direction.
 */
export function tintProblems({ drawables = [], palette = [] }) {
  const problems = []
  const notes = []
  const markFiles = []
  const paletteRoles = paletteRoleSet(palette)
  for (const { rel, text } of drawables) {
    if (MARK_ARTIFACT.test(path.basename(rel))) {
      markFiles.push(rel)
      continue
    }
    const clean = stripComments(String(text), 'xml')
    for (const match of clean.matchAll(LITERAL_FILL)) {
      const colour = (match[1] ?? match[2]).toUpperCase()
      if (paletteHasRole(colour, paletteRoles)) {
        notes.push(
          `${rel}: fillColor="${colour}" is a role of the slot's own palette, so it reads as slot-owned identity ` +
            'rather than a baked-in UI colour; reported as a note, not failed, because §9 makes a slot-owned brand mark ' +
            'palette-coloured art'
        )
        continue
      }
      problems.push(
        `${rel}:${lineOf(clean, match.index)}: fillColor="${colour}" is a literal colour on UI iconography; a UI ` +
          'icon draws in the theme colour (LocalContentColor.current or MaterialTheme.colorScheme.<role>) and carries ' +
          'no fill of its own, and a stock SVG with a baked fill breaks that (§6, rule 10)'
      )
    }
  }
  if (markFiles.length > 0) {
    notes.push(
      `the slot's own mark is delivered through ${markFiles.join(', ')}; those files are the slot's declared identity, ` +
        'not library UI iconography, and legitimately carry the palette colours — they are excluded from icons.tint by ' +
        'design and are the subject of icons.launcher. Every other drawable is UI iconography, and a literal colour ' +
        'there is identity only when it is a palette role (reported as a note) and a failure when it is not'
    )
  }
  notes.push(
    '§6 exempts a literal colour "other than a mask"; VectorDrawable has no <mask> element (the nearest is <clip-path>), ' +
      'so that carve-out is not implemented and no path is exempted'
  )
  notes.push(
    'this leg reads `android:fillColor` only, which is rule 10\'s named failure shape; a literal `strokeColor` or a ' +
      '`<gradient>` stop is not read here, and a `Color(0xFF…)` literal in a Compose `Icon(...)` call is a Kotlin scan ' +
      'this module documents as out of scope'
  )
  return { problems, notes }
}

// ---------------------------------------------------------------------------------------------
// Reading a slot off disk
// ---------------------------------------------------------------------------------------------

function readStudio(slotDir) {
  const file = path.join(slotDir, STUDIO_REL)
  if (!existsSync(file)) return { present: false, error: null, record: null }
  try {
    return { present: true, error: null, record: JSON.parse(readFileSync(file, 'utf8').replace(/^\uFEFF/, '')) }
  } catch (error) {
    return { present: true, error: error.message, record: null }
  }
}

function readBuildTexts(slotDir, rels) {
  const out = []
  for (const rel of rels) {
    const file = path.join(slotDir, ...rel.split('/'))
    if (!existsSync(file)) continue
    let text = null
    try {
      text = readFileSync(file, 'utf8')
    } catch {
      continue
    }
    if (text !== null) out.push({ rel, text })
  }
  return out
}

/** Every `.xml` under `res/drawable*`, as `{ rel, text }`. `null` when there is no `res/` at all. */
export function readDrawables(slotDir) {
  const res = path.join(slotDir, 'app', 'src', 'main', 'res')
  if (!existsSync(res)) return null
  const out = []
  for (const entry of readdirSync(res, { withFileTypes: true })) {
    if (!entry.isDirectory() || !entry.name.startsWith('drawable')) continue
    const dir = path.join(res, entry.name)
    for (const name of readdirSync(dir).sort()) {
      if (!DRAWABLE_EXT.test(name)) continue
      const abs = path.join(dir, name)
      out.push({ rel: relIn(slotDir, abs), text: readFileSync(abs, 'utf8') })
    }
  }
  return out
}

/** Every `.kt` / `.java` / `.xml` under `app/src/main/`, as `{ rel, text }`, NUL-bearing files skipped. */
function readShippedSources(slotDir) {
  const main = path.join(slotDir, 'app', 'src', 'main')
  if (!existsSync(main)) return []
  const out = []
  for (const file of walkSlotFiles(main)) {
    if (!/\.(?:kt|java|xml)$/i.test(file)) continue
    let text = null
    try {
      text = decodeSource(readFileSync(file))
    } catch {
      continue
    }
    if (text === null) continue
    out.push({ rel: relIn(slotDir, file), text })
  }
  return out
}

// ---------------------------------------------------------------------------------------------
// The disk driver
// ---------------------------------------------------------------------------------------------

/**
 * Every leg's raw findings, the ids that could not be evaluated, and the summary each pass may claim.
 *
 * One read of the tree, so `iconsFindings` and `iconsChecks` cannot disagree about what was on disk.
 */
export function iconProblems(slotDir) {
  const byId = Object.fromEntries(ICON_IDS.map((id) => [id, []]))
  const unverifiedById = {}
  const summaries = {}
  const notes = []

  const leg = (id, body) => {
    try {
      body()
    } catch (error) {
      unverifiedById[id] = `could not evaluate: ${error.message}`
    }
  }

  // ---- icons.mark -----------------------------------------------------------------------------
  leg('icons.mark', () => {
    const source = existsSync(ICON_DESIGN) ? readFileSync(ICON_DESIGN, 'utf8') : null
    const registry = source === null ? null : readMarkRegistry(source)
    if (registry === null) {
      unverifiedById['icons.mark'] = `${ICON_DESIGN_REL} could not be read or declares no MARK_REGISTRY, so the registry a studio mark must resolve in is unknown`
      summaries['icons.mark'] = `MARK_REGISTRY in ${ICON_DESIGN_REL} could not be read`
      return
    }
    const studio = readStudio(slotDir)
    if (!studio.present) {
      unverifiedById['icons.mark'] = `no ${STUDIO_REL}, so this slot declares no mark to resolve`
      summaries['icons.mark'] = `no ${STUDIO_REL}`
      return
    }
    if (studio.record === null) {
      unverifiedById['icons.mark'] = `${STUDIO_REL} could not be parsed: ${studio.error}`
      summaries['icons.mark'] = `${STUDIO_REL} could not be parsed`
      return
    }
    const hasMarkKey = Object.prototype.hasOwnProperty.call(studio.record, 'mark')
    const declared = typeof studio.record.mark === 'string' ? studio.record.mark : null
    byId['icons.mark'].push(...markProblems({ declaredMark: declared, hasMarkKey, registry }).problems)
    summaries['icons.mark'] =
      declared && hasMarkKey
        ? `${STUDIO_REL} declares mark '${declared}', which resolves in MARK_REGISTRY (${registry.marks.length} registered: ${registry.marks.join(', ')})`
        : `${STUDIO_REL} declares no \`mark\`, so the studio would fall back to '${registry.defaultMark}'`
  })

  const studio = readStudio(slotDir)
  const palette =
    studio.record && studio.record.palette && typeof studio.record.palette === 'object'
      ? Object.values(studio.record.palette).filter((value) => typeof value === 'string')
      : []

  // ---- icons.launcher -------------------------------------------------------------------------
  leg('icons.launcher', () => {
    const slotVectors = readLauncherVectors(slotDir)
    const scaffoldVectors = readLauncherVectors(SCAFFOLD_DIR, relRepo)
    const slotNames = Object.keys(slotVectors)
    if (slotNames.length === 0) {
      unverifiedById['icons.launcher'] = `no ${DRAWABLE_REL}/ic_launcher_*.xml in this slot, so there is no launcher mark to compare against the scaffold`
      summaries['icons.launcher'] = 'no launcher vector present'
      return
    }
    if (Object.keys(scaffoldVectors).length === 0) {
      unverifiedById['icons.launcher'] = `the scaffold at ${relRepo(SCAFFOLD_DIR)} ships no ic_launcher_*.xml, so there is nothing to compare the slot's launcher art against`
      summaries['icons.launcher'] = 'the scaffold ships no launcher vector'
      return
    }
    byId['icons.launcher'].push(...launcherProblems({ slotVectors, scaffoldVectors, palette }))
    const compared = MARK_VECTOR_NAMES.filter((name) => slotVectors[name] && scaffoldVectors[name]).length
    summaries['icons.launcher'] = `${compared} launcher vector(s) present in both the slot and the scaffold, and none carries the scaffold's geometry`
  })

  // ---- icons.provenance and icons.licence -----------------------------------------------------
  const provenanceFile = path.join(slotDir, ...PROVENANCE_REL.split('/'))
  let record = null
  let recordError = null
  if (!existsSync(provenanceFile)) {
    const reason =
      `no ${PROVENANCE_REL}, so no vendored icon set is recorded — a slot with no record is not a clean slot, ` +
      'it is an unmeasured one (§1, §"How it is verified")'
    unverifiedById['icons.provenance'] = reason
    unverifiedById['icons.licence'] = `no ${PROVENANCE_REL}, so no spdx is declared and no licence file could be required`
    summaries['icons.provenance'] = `no ${PROVENANCE_REL}`
    summaries['icons.licence'] = `no ${PROVENANCE_REL}`
  } else {
    try {
      record = JSON.parse(readFileSync(provenanceFile, 'utf8').replace(/^\uFEFF/, ''))
    } catch (error) {
      recordError = error.message
      byId['icons.provenance'].push(`${PROVENANCE_REL}: could not be parsed as JSON: ${error.message}`)
      unverifiedById['icons.licence'] = `${PROVENANCE_REL} could not be parsed, so no spdx could be read`
    }
    if (record !== null && recordError === null) {
      // The record legs degrade locally (audit 2026-10-02, F1). `provenanceProblems` reads the files
      // the record names, so an unreadable path — a directory, a permission error — used to throw out
      // of `iconProblems` and take every other leg's verdict with it; `iconsChecks` then reported all
      // seven UNVERIFIED and `iconsFindings` threw. A malformed record may only cost its own leg.
      const tryRecordLeg = (id, body) => {
        try {
          return body()
        } catch (error) {
          unverifiedById[id] = `${PROVENANCE_REL} could not be evaluated: ${error.message}`
          return null
        }
      }
      const provenance = tryRecordLeg('icons.provenance', () => provenanceProblems(slotDir, record))
      if (provenance) {
        byId['icons.provenance'].push(...provenance.problems)
        notes.push(...provenance.notes)
      }
      const licence = tryRecordLeg('icons.licence', () => licenceProblems(slotDir, record))
      if (licence) {
        byId['icons.licence'].push(...licence.problems)
        notes.push(...licence.notes)
      }
      const sets = Array.isArray(record.sets) ? record.sets.length : 0
      const icons = Array.isArray(record.sets)
        ? record.sets.reduce((total, set) => total + (set && Array.isArray(set.icons) ? set.icons.length : 0), 0)
        : 0
      if (provenance) {
        summaries['icons.provenance'] =
          `${PROVENANCE_REL}: ${sets} set(s) and ${icons} icon(s); every schema field present, every spdx on the §2 ` +
          'allowlist and none denied, every referenced file committed with a matching sha256'
      }
      if (licence) {
        summaries['icons.licence'] =
          licence.declared.length === 0
            ? 'no spdx was declared, so there was no licence text to require'
            : `a non-empty ${LICENCES_REL}/<SPDX>.txt exists for each of the ${licence.declared.length} declared spdx id(s): ${licence.declared.join(', ')}`
      }
    } else if (recordError !== null) {
      summaries['icons.provenance'] = `${PROVENANCE_REL} could not be parsed`
    }
  }

  // ---- icons.offline --------------------------------------------------------------------------
  leg('icons.offline', () => {
    const buildTexts = readBuildTexts(slotDir, BUILD_SCAN_FILES)
    const shipped = readShippedSources(slotDir)
    if (buildTexts.length === 0 && shipped.length === 0) {
      unverifiedById['icons.offline'] = `no build file and no source under app/src/main/ to read, so a build-time icon fetch cannot be judged`
      summaries['icons.offline'] = 'nothing to scan'
      return
    }
    byId['icons.offline'].push(...offlineProblems({ buildTexts, shipped }))
    summaries['icons.offline'] =
      `${buildTexts.length} build file(s) and ${shipped.length} shipped source(s) scanned; no remote icon/font artifact is declared or reached`
  })

  // ---- icons.deprecated -----------------------------------------------------------------------
  leg('icons.deprecated', () => {
    const buildTexts = readBuildTexts(slotDir, BUILD_FILES)
    if (buildTexts.length === 0) {
      unverifiedById['icons.deprecated'] = `no ${BUILD_FILES.join(' / ')} to read, so a deprecated icon coordinate cannot be judged`
      summaries['icons.deprecated'] = 'nothing to scan'
      return
    }
    byId['icons.deprecated'].push(...deprecatedProblems({ buildTexts }))
    summaries['icons.deprecated'] = `${buildTexts.length} build file(s) read; no material-icons-core / material-icons-extended coordinate`
  })

  // ---- icons.tint -----------------------------------------------------------------------------
  leg('icons.tint', () => {
    const drawables = readDrawables(slotDir)
    if (drawables === null) {
      unverifiedById['icons.tint'] = `no app/src/main/res, so there is no drawable tree to read a UI icon from`
      summaries['icons.tint'] = 'no drawable tree'
      return
    }
    const tint = tintProblems({ drawables, palette })
    byId['icons.tint'].push(...tint.problems)
    notes.push(...tint.notes)
    const markCount = drawables.filter((entry) => MARK_ARTIFACT.test(path.basename(entry.rel))).length
    const uiCount = drawables.length - markCount
    summaries['icons.tint'] =
      uiCount === 0
        ? `no UI icon drawable beyond the launcher mark, so there is nothing to tint; the ${markCount} launcher ` +
          'mark vector(s) are excluded by design (they are the subject of icons.launcher, and legitimately carry the palette)'
        : `${uiCount} UI icon drawable(s) carry no literal fill that is off the slot's declared palette ` +
          `(the ${markCount} launcher mark vector(s) are excluded by design and are the subject of icons.launcher; a ` +
          'palette-role literal on another drawable is reported as a note, not a failure)'
  })

  const cleanNotes = dedupe(notes)
  byId._notes = cleanNotes
  const findings = ICON_IDS.flatMap((id) =>
    unverifiedById[id]
      ? [`${id}: UNVERIFIED: ${unverifiedById[id]}`]
      : byId[id].map((finding) => `${id}: ${finding}`)
  )
  return { byId, unverified: unverifiedById, summaries, notes: cleanNotes, findings }
}

/**
 * The flat finding list, used to demonstrate the sensor in red and in green.
 *
 * It never throws (audit 2026-10-02, F1): the record legs inside `iconProblems` degrade locally, so
 * the remaining `try` is a last resort for an unforeseen read failure rather than the mechanism. An
 * unreadable input is a finding that says it is unverified, never an exception into the caller.
 */
export function iconsFindings(slotDir) {
  try {
    return iconProblems(slotDir).findings
  } catch (error) {
    return [`icons.findings: UNVERIFIED: the icon sensor could not evaluate ${relRepo(slotDir)}: ${error.message}`]
  }
}

// ---------------------------------------------------------------------------------------------
// The verdicts
// ---------------------------------------------------------------------------------------------

const withNotes = (summary, notes) => (notes.length === 0 ? summary : `${summary}; ${notes.join('; ')}`)

function verdictFor(id, slotDir, problems) {
  const reason = problems.unverified[id]
  if (reason) return unverified(id, reason)
  return baselineVerdict(id, slotDir, problems.byId[id] || [], withNotes(problems.summaries[id] || 'evaluated', problems.notes))
}

/** The seven slot facts, as baseline-scoped verdicts, registered in `scripts/core/index.mjs`. */
export function iconsChecks(slotDir) {
  let problems
  try {
    problems = iconProblems(slotDir)
  } catch (error) {
    return ICON_IDS.map((id) => unverified(id, `could not evaluate: ${error.message}`))
  }
  return ICON_IDS.map((id) => guard(id, () => verdictFor(id, slotDir, problems)))
}

// ---------------------------------------------------------------------------------------------
// Inline fixtures — every leg, driven in red and in green
// ---------------------------------------------------------------------------------------------

function withTempDir(body) {
  const dir = mkdtempSync(path.join(SCRATCH_DIR, 'kiln-icons-'))
  try {
    return body(dir)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
}

/**
 * The marker that makes a synthetic tree a product slot.
 *
 * It is not decoration. `baselineApplies()` decides a directory is a gateable app by the presence of
 * `economics.json` (`isProductSlot`), so a temp slot without it would print every finding as a
 * grandfathered note inside a PASS — and no fixture could ever fail. Writing it points the fixtures
 * at the production path instead of at a parallel, weaker one; `icons.mjs` never parses its contents.
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
  return withTempSlot(files, (dir) => iconsChecks(dir).find((row) => row.id === id))
}

function expectSlotVerdict(id, files, verdict, phrase) {
  const result = slotVerdict(id, files)
  expectTrue(result !== undefined, `${id} is absent from the report`)
  expectEqual(result.verdict, verdict, `verdict of ${id}`)
  if (phrase) expectTrue(String(result.detail).includes(phrase), `detail "${result.detail}" does not name ${phrase}`)
  return result
}

const ICON_FILE = 'app/src/main/res/drawable/ic_add.xml'
const ICON_TEXT = [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
  '    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">',
  '    <path android:fillColor="?attr/colorControlNormal" android:pathData="M11,5h2v14h-2z" />',
  '</vector>',
  ''
].join('\n')
const ICON_SHA = sha256Text(ICON_TEXT)

const LICENCE_REL = `${LICENCES_REL}/Apache-2.0.txt`
const LICENCE_TEXT = 'Apache License\nVersion 2.0, January 2004\nhttp://www.apache.org/licenses/\n'

/** A provenance record whose verdict is known to be clean, with one named field overridden. */
function validRecord(overrides = {}) {
  return {
    schemaVersion: 1,
    sets: [
      {
        library: 'Material Symbols',
        version: '2.966',
        spdx: 'Apache-2.0',
        sourceUrl: 'https://github.com/google/material-design-icons',
        licenceFile: LICENCE_REL,
        attribution: null,
        fetchedAt: '2026-10-02',
        fetchedBy: 'agent',
        icons: [
          {
            name: 'add',
            file: ICON_FILE,
            sha256: ICON_SHA,
            sourcePath: 'symbols/android/material_symbols/add/materialsymbolsoutlined/add_24px.svg'
          }
        ],
        ...overrides
      }
    ]
  }
}

/** The files a clean record needs on disk: the icon it lists and the licence text it declares. */
const PROVENANCE_FILES = {
  [ICON_FILE]: ICON_TEXT,
  [LICENCE_REL]: LICENCE_TEXT,
  [PROVENANCE_REL]: null // filled per fixture
}

const provenanceSlot = (record, extra = {}) => ({
  ...PROVENANCE_FILES,
  [PROVENANCE_REL]: JSON.stringify(record, null, 2),
  ...extra
})

/** The scaffold's own launcher foreground, read lazily so a missing scaffold fails the fixture, loudly. */
function scaffoldVector(name) {
  const file = path.join(SCAFFOLD_DIR, ...DRAWABLE_REL.split('/'), name)
  if (!existsSync(file)) throw new Error(`this fixture needs the scaffold's ${name}, absent at ${relRepo(file)}`)
  return readFileSync(file, 'utf8')
}

const OWN_FOREGROUND = [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
  '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">',
  '    <path android:fillColor="#3D2E7C" android:pathData="M28,34L80,34L54,80Z" />',
  '</vector>',
  ''
].join('\n')

const OWN_BACKGROUND = [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
  '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">',
  '    <path android:fillColor="#1B1440" android:pathData="M0,0h108v108h-108z" />',
  '</vector>',
  ''
].join('\n')

export function iconsFixtures() {
  return guard('icons.fixtures', () =>
    runFixtures('icons.fixtures', [
      // ---- icons.mark -------------------------------------------------------------------------
      [
        'a registry key nested inside an entry is not a mark, and a commented-out entry is not registered',
        () => {
          const source = [
            '# MARK_REGISTRY is declared below',
            'MARK_REGISTRY: dict[str, dict] = {',
            '    "pipe": {"primitives": PIPE, "keyline": "circle",',
            '             "note": "a pipe cross-section"},',
            '    # "retired": {"primitives": OLD, "keyline": "circle"},',
            '    "mark-book": {"primitives": BOOK, "keyline": "portrait"},',
            '}',
            'DEFAULT_MARK = "pipe"',
            ''
          ].join('\n')
          expectEqual(pythonDictKeys(source, 'MARK_REGISTRY'), ['pipe', 'mark-book'], 'top-level keys only')
          expectTrue(!pythonDictKeys(source, 'MARK_REGISTRY').includes('primitives'), 'a nested key is not a mark')
          expectTrue(!pythonDictKeys(source, 'MARK_REGISTRY').includes('retired'), 'a commented entry is not registered')
          expectEqual(readMarkRegistry(source), { marks: ['pipe', 'mark-book'], defaultMark: 'pipe' }, 'the registry')
        }
      ],
      [
        'a mark absent from MARK_REGISTRY fails naming the registry, a registered one passes, and no declared mark fails',
        () => {
          const registry = { marks: ['pipe', 'mark-book'], defaultMark: 'pipe' }
          const missing = markProblems({ declaredMark: 'mark-stripes', hasMarkKey: true, registry })
          expectEqual(missing.problems.length, 1, 'one finding for an unregistered mark')
          expectTrue(missing.problems[0].includes("mark-stripes"), missing.problems[0])
          expectTrue(missing.problems[0].includes('BLOCKED: unknown studio mark'), missing.problems[0])
          expectEqual(markProblems({ declaredMark: 'mark-book', hasMarkKey: true, registry }).problems, [], 'a registered mark')
          const absent = markProblems({ declaredMark: null, hasMarkKey: false, registry })
          expectTrue(absent.problems[0].includes('declares no `mark`'), absent.problems[0])
        }
      ],
      [
        "the live slot's declared mark resolves against the real MARK_REGISTRY: an unregistered one fails, a registered one passes",
        () => {
          expectSlotVerdict('icons.mark', { [`${STUDIO_REL}`]: '{"mark":"pipe"}\n' }, 'PASS', 'resolves in MARK_REGISTRY')
          expectSlotVerdict('icons.mark', { [`${STUDIO_REL}`]: '{"mark":"not-a-mark"}\n' }, 'FAIL', 'absent from MARK_REGISTRY')
          expectSlotVerdict('icons.mark', {}, 'UNVERIFIED', `no ${STUDIO_REL}`)
        }
      ],
      // ---- icons.launcher ---------------------------------------------------------------------
      [
        'a slot that still ships the scaffold art fails; its own mark passes; and recolouring the copy does not evade it',
        () => {
          const scaffold = scaffoldVector('ic_launcher_foreground.xml')
          const recoloured = scaffold.replace(/#FFB86B/g, '#123456')
          const scaffoldVectors = { 'ic_launcher_foreground.xml': { rel: 'scaffold/…/ic_launcher_foreground.xml', text: scaffold } }
          const asSlot = (text) => ({ 'ic_launcher_foreground.xml': { rel: DRAWABLE_REL + '/ic_launcher_foreground.xml', text } })

          const identical = launcherProblems({ slotVectors: asSlot(scaffold), scaffoldVectors })
          expectEqual(identical.length, 1, 'the scaffold copy is a finding')
          expectTrue(identical[0].includes('placeholder launcher art'), identical[0])
          expectEqual(launcherProblems({ slotVectors: asSlot(recoloured), scaffoldVectors }).length, 1, 'a recoloured copy is still the placeholder')
          expectEqual(launcherProblems({ slotVectors: asSlot(OWN_FOREGROUND), scaffoldVectors }), [], 'its own geometry is not')

          // The positive control for the comparison: reordered paths in an otherwise identical file
          // are still the scaffold's art, so sorting the geometry set is what makes the leg stable.
          const reordered = scaffold.replace(/(<path[\s\S]*?\/>)(\s*<path[\s\S]*?\/>)/, '$2$1')
          expectTrue(reordered !== scaffold, 'the fixture actually reordered the paths')
          expectEqual(launcherProblems({ slotVectors: asSlot(reordered), scaffoldVectors }).length, 1, 'reordering does not evade')
        }
      ],
      [
        'a scaffold copy read through a renamed namespace prefix or single quotes is still the placeholder, and the byte-identical control still fails (F2/F3)',
        () => {
          const scaffold = scaffoldVector('ic_launcher_foreground.xml')
          const scaffoldVectors = { 'ic_launcher_foreground.xml': { rel: 'scaffold/…/ic_launcher_foreground.xml', text: scaffold } }
          const asSlot = (text) => ({ 'ic_launcher_foreground.xml': { rel: DRAWABLE_REL + '/ic_launcher_foreground.xml', text } })
          const renamed = scaffold
            .replace(/xmlns:android="[^"]*"/, 'xmlns:a="http://schemas.android.com/apk/res/android"')
            .replace(/android:/g, 'a:')
          const singleQuoted = scaffold.replace(/android:pathData="([^"]*)"/g, "android:pathData='$1'")
          expectTrue(renamed !== scaffold && !renamed.includes('android:pathData'), 'the fixture actually renamed the prefix')
          expectTrue(singleQuoted !== scaffold, 'the fixture actually quoted the attributes')
          expectEqual(vectorPaths(renamed), vectorPaths(scaffold), 'the renamed copy reads as the same geometry')
          expectEqual(vectorPaths(singleQuoted), vectorPaths(scaffold), 'the single-quoted copy reads as the same geometry')
          expectEqual(launcherProblems({ slotVectors: asSlot(renamed), scaffoldVectors }).length, 1, 'a renamed prefix does not evade')
          expectEqual(launcherProblems({ slotVectors: asSlot(singleQuoted), scaffoldVectors }).length, 1, 'single quotes do not evade')
          expectSlotVerdict('icons.launcher', { [`${DRAWABLE_REL}/ic_launcher_foreground.xml`]: renamed }, 'FAIL', 'placeholder launcher art')
        }
      ],
      [
        "the launcher palette is a criterion, not a note (F9): the slot's own geometry with a fill in no palette role fails; a role of the palette passes; an undeclared palette judges nothing",
        () => {
          const scaffoldVectors = { 'ic_launcher_foreground.xml': { rel: 'scaffold/…', text: scaffoldVector('ic_launcher_foreground.xml') } }
          const asSlot = (text) => ({ 'ic_launcher_foreground.xml': { rel: DRAWABLE_REL + '/ic_launcher_foreground.xml', text } })
          expectEqual(
            launcherProblems({ slotVectors: asSlot(OWN_FOREGROUND), scaffoldVectors, palette: ['#123456'] }).length,
            1,
            "an off-palette fill on the slot's own geometry is a finding"
          )
          expectEqual(
            launcherProblems({ slotVectors: asSlot(OWN_FOREGROUND), scaffoldVectors, palette: ['#3D2E7C'] }),
            [],
            'a fill that is a palette role is the slot identity'
          )
          expectEqual(
            launcherProblems({ slotVectors: asSlot(OWN_FOREGROUND), scaffoldVectors, palette: [] }),
            [],
            'with no declared palette there is no identity to be off'
          )
          expectTrue(paletteHasRole('#FFFFFFFF', paletteRoleSet(['#FFFFFF'])), 'an eight-digit white matches a six-digit palette white')
          expectSlotVerdict(
            'icons.launcher',
            {
              [`${STUDIO_REL}`]: JSON.stringify({ mark: 'pipe', palette: { ink: '#123456' } }),
              [`${DRAWABLE_REL}/ic_launcher_foreground.xml`]: OWN_FOREGROUND
            },
            'FAIL',
            `in no role of ${STUDIO_REL}`
          )
          expectSlotVerdict(
            'icons.launcher',
            {
              [`${STUDIO_REL}`]: JSON.stringify({ mark: 'pipe', palette: { ink: '#3D2E7C' } }),
              [`${DRAWABLE_REL}/ic_launcher_foreground.xml`]: OWN_FOREGROUND
            },
            'PASS',
            'none carries the scaffold'
          )
        }
      ],
      [
        'a slot still shipping the scaffold foreground fails on disk, and a slot with its own vectors passes',
        () => {
          expectSlotVerdict(
            'icons.launcher',
            { [`${DRAWABLE_REL}/ic_launcher_foreground.xml`]: scaffoldVector('ic_launcher_foreground.xml') },
            'FAIL',
            'placeholder launcher art'
          )
          expectSlotVerdict(
            'icons.launcher',
            {
              [`${DRAWABLE_REL}/ic_launcher_foreground.xml`]: OWN_FOREGROUND,
              [`${DRAWABLE_REL}/ic_launcher_background.xml`]: OWN_BACKGROUND
            },
            'PASS',
            'none carries the scaffold'
          )
          expectSlotVerdict('icons.launcher', {}, 'UNVERIFIED', 'no launcher mark to compare')
        }
      ],
      // ---- icons.provenance and icons.licence --------------------------------------------------
      [
        'a complete record passes; a wrong sha256, a missing file, schemaVersion 2, a denied spdx, an off-allowlist spdx and a moving version each fail',
        () => {
          expectSlotVerdict('icons.provenance', provenanceSlot(validRecord()), 'PASS', 'every referenced file committed')
          expectSlotVerdict(
            'icons.provenance',
            provenanceSlot(validRecord({ icons: [{ name: 'add', file: ICON_FILE, sha256: 'a'.repeat(64), sourcePath: 'x.svg' }] })),
            'FAIL',
            'not the recorded'
          )
          expectSlotVerdict(
            'icons.provenance',
            provenanceSlot(validRecord({ icons: [{ name: 'add', file: 'app/src/main/res/drawable/ic_gone.xml', sha256: ICON_SHA, sourcePath: 'x.svg' }] })),
            'FAIL',
            'is not committed'
          )
          expectSlotVerdict('icons.provenance', provenanceSlot({ ...validRecord(), schemaVersion: 2 }), 'FAIL', 'expected 1')
          expectSlotVerdict('icons.provenance', provenanceSlot(validRecord({ spdx: 'CC-BY-NC-4.0' })), 'FAIL', 'NonCommercial')
          expectSlotVerdict('icons.provenance', provenanceSlot(validRecord({ spdx: 'LicenseRef-Other-1.0' })), 'FAIL', 'not on the §2 allowlist')
          expectSlotVerdict('icons.provenance', provenanceSlot(validRecord({ version: 'latest' })), 'FAIL', 'moving reference')
          expectSlotVerdict('icons.provenance', provenanceSlot(validRecord({ attribution: { text: 'Google', url: 'https://example.com', inApp: false }, spdx: 'CC-BY-4.0' })), 'FAIL', 'inApp')
        }
      ],
      [
        'a declared spdx with no LICENSES/<SPDX>.txt fails; committing a non-empty one passes; an empty one fails',
        () => {
          const noLicence = provenanceSlot(validRecord(), { [LICENCE_REL]: null })
          expectSlotVerdict('icons.licence', noLicence, 'FAIL', 'does not travel')
          expectSlotVerdict('icons.licence', provenanceSlot(validRecord()), 'PASS', 'Apache-2.0')
          expectSlotVerdict('icons.licence', provenanceSlot(validRecord(), { [LICENCE_REL]: '   \n' }), 'FAIL', 'is empty')
        }
      ],
      [
        'a record that is absent or unparseable is UNVERIFIED for both record legs, never a pass',
        () => {
          expectSlotVerdict('icons.provenance', {}, 'UNVERIFIED', 'unmeasured one')
          expectSlotVerdict('icons.licence', {}, 'UNVERIFIED', 'no spdx is declared')
          expectSlotVerdict('icons.provenance', { [PROVENANCE_REL]: '{ not json\n' }, 'FAIL', 'could not be parsed')
          expectSlotVerdict('icons.licence', { [PROVENANCE_REL]: '{ not json\n' }, 'UNVERIFIED', 'no spdx could be read')
        }
      ],
      [
        'a record whose icon file resolves to a directory degrades locally (F1): the launcher leg keeps its real FAIL, provenance fails and neither iconsChecks nor iconsFindings throws',
        () => {
          const files = {
            ...provenanceSlot(validRecord({ icons: [{ name: 'add', file: DRAWABLE_REL, sha256: ICON_SHA, sourcePath: 'x.svg' }] })),
            [`${DRAWABLE_REL}/ic_launcher_foreground.xml`]: scaffoldVector('ic_launcher_foreground.xml')
          }
          withTempSlot(files, (dir) => {
            const rows = iconsChecks(dir)
            const verdict = (id) => rows.find((row) => row.id === id)?.verdict
            expectEqual(verdict('icons.launcher'), 'FAIL', 'the launcher leg keeps its real verdict')
            expectEqual(verdict('icons.provenance'), 'FAIL', 'a directory is not a committed icon file')
            expectEqual(verdict('icons.licence'), 'PASS', 'the licence leg keeps its real verdict')
            const findings = iconsFindings(dir)
            expectTrue(findings.some((finding) => finding.startsWith('icons.launcher:')), 'the launcher finding survives the malformed record')
            expectTrue(findings.some((finding) => finding.startsWith('icons.provenance:')), 'the provenance finding is reported')
          })
        }
      ],
      [
        'a licenceFile that is a directory or that is not named for its spdx fails (F5)',
        () => {
          expectSlotVerdict('icons.provenance', provenanceSlot(validRecord({ licenceFile: LICENCES_REL })), 'FAIL', 'is a directory')
          expectSlotVerdict(
            'icons.provenance',
            provenanceSlot(validRecord({ spdx: 'ISC', licenceFile: LICENCE_REL }), { [`${LICENCES_REL}/ISC.txt`]: 'ISC License\n' }),
            'FAIL',
            'not tied to the declared spdx'
          )
          expectSlotVerdict(
            'icons.provenance',
            provenanceSlot(validRecord({ licenceFile: `${LICENCES_REL}/Apache-2.0.txt` })),
            'PASS',
            'every referenced file committed'
          )
        }
      ],
      [
        'two libraries without a written justification fail; the set-level justification §7 asks for passes',
        () => {
          const second = {
            library: 'Lucide',
            version: '0.475.0',
            spdx: 'ISC',
            sourceUrl: 'https://github.com/lucide-icons/lucide',
            licenceFile: `${LICENCES_REL}/ISC.txt`,
            attribution: null,
            fetchedAt: '2026-10-02',
            fetchedBy: 'agent',
            icons: [{ name: 'plus', file: ICON_FILE, sha256: ICON_SHA, sourcePath: 'icons/plus.svg' }]
          }
          const twoSets = (extra) => ({
            ...validRecord(),
            sets: [validRecord().sets[0], { ...second, ...extra }]
          })
          const files = { [ICON_FILE]: ICON_TEXT, [LICENCE_REL]: LICENCE_TEXT, [`${LICENCES_REL}/ISC.txt`]: 'ISC License\n' }
          expectSlotVerdict('icons.provenance', { ...files, [PROVENANCE_REL]: JSON.stringify(twoSets({})) }, 'FAIL', 'justification')
          expectSlotVerdict(
            'icons.provenance',
            { ...files, [PROVENANCE_REL]: JSON.stringify(twoSets({ justification: 'the domain glyph has no Material Symbols match' })) },
            'PASS',
            'every referenced file committed'
          )
        }
      ],
      [
        'the contract\'s two recorded-path forms both resolve, and only the repo-relative one is reported as the ambiguity',
        () => {
          withTempDir((base) => {
            const slot = path.join(base, 'slot')
            const root = path.join(base, 'repo-root')
            mkdirSync(path.join(slot, 'icons', 'LICENSES'), { recursive: true })
            mkdirSync(path.join(root, 'apps', 'demo', 'icons', 'LICENSES'), { recursive: true })
            writeFileSync(path.join(slot, 'icons', 'LICENSES', 'MIT.txt'), 'MIT\n', 'utf8')
            writeFileSync(path.join(root, 'apps', 'demo', 'icons', 'LICENSES', 'MIT.txt'), 'MIT\n', 'utf8')

            expectEqual(resolveRecordedPath(slot, 'icons/LICENSES/MIT.txt', root).how, 'slot-relative', 'the slot-relative form')
            const repoForm = resolveRecordedPath(slot, 'apps/demo/icons/LICENSES/MIT.txt', root)
            expectEqual(repoForm.how, 'repo-relative', 'the repo-relative form')
            expectEqual(resolveRecordedPath(slot, 'icons/LICENSES/Apache-2.0.txt', root), null, 'an absent file resolves to nothing')

            const record = validRecord({ licenceFile: 'apps/demo/icons/LICENSES/MIT.txt', icons: [] })
            const result = provenanceProblems(slot, record, { rootDir: root })
            expectTrue(result.notes.some((note) => note.includes('repository root')), result.notes.join('; '))
          })
        }
      ],
      // ---- icons.offline ----------------------------------------------------------------------
      [
        'a remote SVG URL, a download plugin and a Google Fonts URL each fail; vendored bytes pass',
        () => {
          expectEqual(
            offlineProblems({ buildTexts: [{ rel: 'app/build.gradle.kts', text: 'val icons = "https://cdn.example.com/icons/add.svg"\n' }] }).length,
            1,
            'a remote asset'
          )
          expectEqual(
            offlineProblems({ buildTexts: [{ rel: 'build.gradle.kts', text: 'plugins { id("de.undercouch.download") }\n' }] }).length,
            1,
            'a download plugin'
          )
          expectEqual(
            offlineProblems({ shipped: [{ rel: 'app/src/main/java/com/gexetr/demo/Type.kt', text: 'val f = "https://fonts.googleapis.com/css2?family=Material+Symbols"\n' }] }).length,
            1,
            'a Google Fonts URL'
          )
          expectEqual(
            offlineProblems({
              buildTexts: [{ rel: 'app/build.gradle.kts', text: 'implementation(libs.androidx.compose.material3)\n' }],
              shipped: [{ rel: 'app/src/main/res/drawable/ic_add.xml', text: ICON_TEXT }]
            }),
            [],
            'vendored bytes'
          )
        }
      ],
      [
        'a build file that downloads icons fails on disk, and one that vendors them passes',
        () => {
          const clean = 'dependencies {\n    implementation(libs.androidx.compose.material3)\n}\n'
          expectSlotVerdict(
            'icons.offline',
            { 'app/build.gradle.kts': 'plugins { id("de.undercouch.download") }\n' },
            'FAIL',
            'Gradle download plugin'
          )
          expectSlotVerdict('icons.offline', { 'app/build.gradle.kts': clean }, 'PASS', 'no remote icon/font artifact')
          expectSlotVerdict('icons.offline', {}, 'UNVERIFIED', 'cannot be judged')
        }
      ],
      [
        'a URL inside a comment is documentation, not a fetch; the same URL in code still fails, including an icon CDN with no .svg (F4/F6)',
        () => {
          expectEqual(offlineProblems({ buildTexts: [{ rel: 'app/build.gradle.kts', text: '// source: https://example.com/assets/plus.svg\n' }] }), [], 'a commented .svg URL')
          expectEqual(offlineProblems({ buildTexts: [{ rel: 'app/build.gradle.kts', text: '// do NOT use https://fonts.googleapis.com here\n' }] }), [], 'a commented CDN host')
          expectEqual(offlineProblems({ shipped: [{ rel: 'app/src/main/java/x/Icon.kt', text: '// vendored from https://lucide.dev/icons/plus\nval x = 1\n' }] }), [], 'a Kotlin provenance comment')
          expectEqual(offlineProblems({ shipped: [{ rel: 'app/src/main/java/x/Icon.kt', text: '/* vendored from https://lucide.dev */\nval x = 1\n' }] }), [], 'a block comment')
          expectEqual(offlineProblems({ buildTexts: [{ rel: 'gradle/libs.versions.toml', text: '# fetched https://api.iconify.design/x.svg\n' }] }), [], 'a TOML comment')
          expectTrue(
            offlineProblems({ buildTexts: [{ rel: 'app/build.gradle.kts', text: '// see https://example.com/a.svg\nval u = "https://cdn.example.com/icons/add.svg"\n' }] }).some((why) => why.includes('add.svg')),
            'the real URL on a later line still fails'
          )
          expectTrue(
            offlineProblems({ shipped: [{ rel: 'app/src/main/java/x/Icon.kt', text: 'val u = "https://api.iconify.design/mdi.json?icons=plus"\n' }] }).some((why) => why.includes('iconify')),
            'a shipped icon-CDN URL with no .svg extension fails'
          )
          expectTrue(
            offlineProblems({ shipped: [{ rel: 'app/src/main/java/x/Icon.kt', text: 'val u = "https://www.svgrepo.com/show/1234/plus"' }] }).some((why) => why.includes('svgrepo')),
            'a shipped svgrepo URL fails'
          )
        }
      ],
      // ---- icons.deprecated -------------------------------------------------------------------
      [
        'material-icons-extended fails; a Material Symbols drawable passes',
        () => {
          expectEqual(
            deprecatedProblems({ buildTexts: [{ rel: 'gradle/libs.versions.toml', text: 'androidx-compose-material-icons-extended = "1.7.3"\n' }] }).length,
            1,
            'the extended coordinate'
          )
          expectEqual(
            deprecatedProblems({ buildTexts: [{ rel: 'app/build.gradle.kts', text: 'implementation("androidx.compose.material:material-icons-core:1.7.3")\n' }] }).length,
            1,
            'the core coordinate'
          )
          expectEqual(
            deprecatedProblems({ buildTexts: [{ rel: 'app/build.gradle.kts', text: 'implementation(libs.androidx.compose.material3)\n' }] }),
            [],
            'no deprecated coordinate'
          )
        }
      ],
      [
        'a deprecated coordinate fails on disk, and a clean catalog passes',
        () => {
          expectSlotVerdict(
            'icons.deprecated',
            { 'gradle/libs.versions.toml': 'androidx-compose-material-icons-core = "1.7.3"\n' },
            'FAIL',
            'deprecated Material icon artifact'
          )
          expectSlotVerdict('icons.deprecated', { 'gradle/libs.versions.toml': 'composeBom = "2026.09.00"\n' }, 'PASS', 'no material-icons-core')
          expectSlotVerdict('icons.deprecated', {}, 'UNVERIFIED', 'cannot be judged')
        }
      ],
      [
        'a comment naming a deprecated artifact is not a coordinate; a real dependency still fails, trailing comment or not (F7)',
        () => {
          expectEqual(deprecatedProblems({ buildTexts: [{ rel: 'app/build.gradle.kts', text: '// we removed material-icons-core in favour of vendored vectors\n' }] }), [], 'a Kotlin comment')
          expectEqual(deprecatedProblems({ buildTexts: [{ rel: 'gradle/libs.versions.toml', text: '# material-icons-extended was dropped\n' }] }), [], 'a TOML comment')
          expectEqual(deprecatedProblems({ buildTexts: [{ rel: 'x', text: '// material-icons-extended\n' }] }), [], 'a comment in a file with no extension')
          expectEqual(deprecatedProblems({ buildTexts: [{ rel: 'app/build.gradle.kts', text: '// pinned\nimplementation("androidx.compose.material:material-icons-core:1.7.3")\n' }] }).length, 1, 'the real coordinate survives an earlier comment')
          expectEqual(deprecatedProblems({ buildTexts: [{ rel: 'app/build.gradle.kts', text: 'implementation("androidx.compose.material:material-icons-core:1.7.3") // pinned\n' }] }).length, 1, 'the real coordinate survives a trailing comment')
        }
      ],
      // ---- icons.tint -------------------------------------------------------------------------
      [
        'a literal fillColor on a UI drawable fails; a theme reference passes; the slot\'s own mark is excluded by design',
        () => {
          const literal = { rel: `${DRAWABLE_REL}/ic_delete.xml`, text: '<vector><path android:fillColor="#FF0000" android:pathData="M0,0L1,1Z" /></vector>\n' }
          const themed = { rel: `${DRAWABLE_REL}/ic_delete.xml`, text: '<vector><path android:fillColor="?attr/colorControlNormal" android:pathData="M0,0L1,1Z" /></vector>\n' }
          const mark = { rel: `${DRAWABLE_REL}/ic_launcher_foreground.xml`, text: '<vector><path android:fillColor="#3D2E7C" android:pathData="M0,0L1,1Z" /></vector>\n' }

          const flagged = tintProblems({ drawables: [literal] })
          expectEqual(flagged.problems.length, 1, 'a literal colour')
          expectTrue(flagged.problems[0].includes('fillColor="#FF0000"'), flagged.problems[0])
          expectEqual(tintProblems({ drawables: [themed] }).problems, [], 'a theme reference')

          const excluded = tintProblems({ drawables: [mark] })
          expectEqual(excluded.problems, [], 'the mark is excluded')
          expectTrue(excluded.notes.some((note) => note.includes('excluded from icons.tint by design')), excluded.notes.join('; '))
          expectTrue(excluded.notes.some((note) => note.includes('icons.launcher')), 'the distinction names the mark leg')
        }
      ],
      [
        'a literal colour on a UI drawable fails on disk; the same file as the launcher mark passes',
        () => {
          const body = '<vector><path android:fillColor="#FF0000" android:pathData="M0,0L1,1Z" /></vector>\n'
          expectSlotVerdict('icons.tint', { [`${DRAWABLE_REL}/ic_delete.xml`]: body }, 'FAIL', 'literal colour on UI iconography')
          expectSlotVerdict('icons.tint', { [`${DRAWABLE_REL}/ic_launcher_monochrome.xml`]: body }, 'PASS', 'excluded by design')
          expectSlotVerdict('icons.tint', {}, 'UNVERIFIED', 'no drawable tree')
        }
      ],
      [
        "a palette-role literal on a slot-owned drawable is identity (a NOTE), a ripple colour is not a fill, and an off-palette literal still fails (F8)",
        () => {
          const brand = { rel: `${DRAWABLE_REL}/ic_brand_mark.xml`, text: '<vector><path android:fillColor="#C9F04A" android:pathData="M0,0L1,1Z" /></vector>\n' }
          const inPalette = tintProblems({ drawables: [brand], palette: ['#C9F04A'] })
          expectEqual(inPalette.problems, [], 'a role of the palette is identity, not a baked colour')
          expectTrue(inPalette.notes.some((note) => note.includes('ic_brand_mark.xml')), 'the note names the file')
          expectEqual(tintProblems({ drawables: [brand], palette: ['#123456'] }).problems.length, 1, 'an off-palette literal still fails')
          const ripple = { rel: `${DRAWABLE_REL}/ic_ripple.xml`, text: '<ripple android:color="#FF0000"><item android:drawable="@android:drawable/white"/></ripple>\n' }
          expectEqual(tintProblems({ drawables: [ripple] }).problems, [], 'a ripple colour is not a path fill')
          const renamedFill = { rel: `${DRAWABLE_REL}/ic_delete.xml`, text: '<vector xmlns:a="http://schemas.android.com/apk/res/android"><path a:fillColor="#FF0000" a:pathData="M0,0L1,1Z" /></vector>\n' }
          expectEqual(tintProblems({ drawables: [renamedFill] }).problems.length, 1, 'a renamed prefix does not evade the tint leg')
          expectSlotVerdict(
            'icons.tint',
            {
              [`${STUDIO_REL}`]: JSON.stringify({ mark: 'pipe', palette: { amber: '#C9F04A' } }),
              [`${DRAWABLE_REL}/ic_brand_mark.xml`]: brand.text
            },
            'PASS',
            'palette'
          )
        }
      ]
    ])
  )
}

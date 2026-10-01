// UI/UX baseline: Navigation 3, adaptive layout, text-layout resilience, motion, accessibility
// semantics and IME insets — the six facts behind the owner's 2026-09-30 review of the first live
// slot, `apps/2026-09-26-teacher-gradebook`.
//
// Why this module exists. The owner read that app's interface and said it was crude, that it did not
// use modern technologies, and that "strings wrap badly, like two words spread over seven lines". He
// then said not to repair that app but to make sure the next apps cannot repeat it. Until now nothing
// in the core read a line of Kotlin: the registered groups are
// `invariants, assets, icon, apk, build, manifest, policy, project`, `.cursor/rules/compose-quality.mdc`
// was eleven lines of unspecified reminders, and `WindowSizeClass` appeared nowhere in the repository.
// A defect the factory cannot see is a defect the factory ships again.
//
// What this module measures, and what it refuses to claim. It reads source files, not pixels, so it
// cannot see a wrapped string. What it can do is name the *causes* of forced wrapping that are
// statically visible, and it says which read it is making: a fixed extent on an element that holds
// text, a clamp with no ellipsis, a hand-rolled back stack, a one-column layout that has a detail
// destination to show, a clickable element with no accessible name and no 48 dp target, a text field
// the keyboard can cover. Every finding names the file and the line, so it is actionable without a
// build. Proxies it cannot defend are deliberately absent and are listed in "What this module does
// not check" in `docs/ui-ux-standards.md`: a `Text` with no `maxLines` is not a finding (a `Text` in
// a weighted or filling container wrapping is correct behaviour), and `contentDescription = null` is
// not a finding (deciding an icon is decorative needs the screen, and a static scan must not guess).
// A false-positive sensor is worse than a missing one here: the owner's decision was not to repair
// the app that already exists, so a noisy rule would be read past by the time it matters.
//
// The standards are published, not invented, per the owner's instruction ("there are professionals;
// use ready standards"): Material 3 type scale and touch targets, WCAG 2.2 SC 1.4.4 and 1.4.12, the
// AndroidX Compose adaptive / insets / accessibility guides, and the three officially vendored skills
// under `.agents/skills/android-official/`. The full list is `UIUX_STANDARD_SOURCES`, it lives in
// `docs/ui-ux-standards.md`, and `checkRuleAgreement` fails when a citation disappears — so the doc
// cannot drift away from what the sensor enforces.
//
// Baseline scoping is imported, never restated: `baselineVerdict` reports a finding as a FAIL only
// where the rule is in force, and prints it as a note on the slots that predate the baseline. The
// four existing slots are therefore not re-judged for a defect the owner has already decided not to
// repair, while every slot scaffolded from today is gated.
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { SCRATCH_DIR } from '../lib/scratch.mjs'
import path from 'node:path'
import { baselineVerdict } from './baseline.mjs'
// Imported, never re-implemented: `decodeSource` is the one place that decides a file is binary
// (`bytes.includes(0)`), which every text sweep in the core obeys.
import { decodeSource } from './build.mjs'
import { expectEqual, expectTrue, fail, guard, pass, ROOT, runFixtures, unverified } from './lib.mjs'
// The manifest reader and the tree walker are shared with `manifest.mjs` for the same reason: a
// second answer to "which files does a slot have" is a second answer that will drift.
import { MANIFEST_REL, readManifestState, walkSlotFiles } from './manifest.mjs'

/** The slot-scoped checks this module registers. The rule text must name every one of them. */
export const UIUX_IDS = Object.freeze([
  'uiux.navigation',
  'uiux.adaptive',
  'uiux.text-layout',
  'uiux.motion',
  'uiux.a11y',
  'uiux.insets'
])

/** The factory-scoped checks. `uiux.scaffold` is expected red until the scaffold carries the baseline. */
export const UIUX_PROJECT_IDS = Object.freeze(['uiux.scaffold', 'uiux.rule'])

export const UIUX_RULE_REL = '.cursor/rules/ui-ux-baseline.mdc'
export const UIUX_STANDARDS_REL = 'docs/ui-ux-standards.md'

/**
 * Every external standard the rules below rest on, with the claim it supports.
 *
 * `checkRuleAgreement` asserts each string is present in `docs/ui-ux-standards.md`, so this table is
 * the rule's citation list rather than a suggestion about one. The three `.agents/skills/...` entries
 * are also asserted to exist on disk, because a vendored skill that has been deleted is a citation
 * that cannot be read.
 */
export const UIUX_STANDARD_SOURCES = Object.freeze([
  ['https://m3.material.io/styles/typography/type-scale-tokens', 'Material 3 type scale — text is sized by role, never shrunk to fit a box'],
  ['https://m3.material.io/foundations/accessible-design/overview', 'Material 3 accessible design — targets, contrast and labelled controls'],
  ['https://m3.material.io/styles/motion/overview', 'Material 3 motion — a transition says where a screen came from'],
  ['https://www.w3.org/WAI/WCAG22/Understanding/resize-text.html', 'WCAG 2.2 SC 1.4.4 Resize Text — up to 200 % with no loss of content, and the failure it names is "only one word of a sentence may fit on each line, causing the sentence to be displayed as a vertical column of text that is difficult to read"'],
  ['https://www.w3.org/WAI/WCAG22/Understanding/text-spacing.html', 'WCAG 2.2 SC 1.4.12 Text Spacing — a text container must be able to grow with its text'],
  ['https://developer.android.com/develop/ui/compose/layouts/adaptive', 'Compose adaptive apps — window size classes, `currentWindowAdaptiveInfo()`, panes, adaptive grids'],
  ['https://developer.android.com/develop/ui/compose/layouts/adaptive/list-detail', 'Compose list-detail — a detail destination needs a two-pane policy'],
  ['https://developer.android.com/develop/ui/compose/accessibility', 'Compose accessibility — semantics, contentDescription, user-scalable content'],
  ['https://developer.android.com/develop/ui/compose/accessibility/semantics', 'Compose semantics — role, state description, merged and cleared trees'],
  ['https://developer.android.com/guide/topics/ui/accessibility/apps', 'Android accessibility — "a touch target size, of at least 48dpx48dp"'],
  ['https://developer.android.com/develop/ui/compose/system/insets', 'Compose window insets — the app draws under the system bars at targetSdk 35+, and the IME is its own inset'],
  ['https://developer.android.com/reference/kotlin/androidx/compose/ui/text/style/TextOverflow', 'TextOverflow — `Clip` is the default, and a clamped line shows a cut glyph with no indication that there is more'],
  ['https://developer.android.com/guide/navigation/navigation-3', 'Navigation 3 — a back stack the host owns, with animations, scenes and deep links'],
  ['https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture', 'Predictive back — the gesture has nothing to preview unless the host owns the back stack'],
  ['https://developer.android.com/develop/ui/compose/tooling/previews', 'Compose previews — `@PreviewFontScales` and `fontScale` render the layout at more than one text size'],
  ['https://developer.android.com/jetpack/androidx/releases/navigation', 'AndroidX navigation releases — which `navigation3-*` artifact a slot depends on'],
  ['.agents/skills/android-official/jetpack-compose-adaptive/SKILL.md', 'vendored: Compose adaptive layouts'],
  ['.agents/skills/android-official/navigation-navigation-3/SKILL.md', 'vendored: Navigation 3'],
  ['.agents/skills/android-official/system-edge-to-edge/SKILL.md', 'vendored: edge-to-edge, imePadding and adjustResize']
])

/**
 * The adaptive APIs any one of which answers "how wide is the window".
 *
 * A slot needs one of these, a scene strategy, or an adaptive grid before it can be said to react to
 * form factor at all. Listed by name so a finding can print the set it looked for, which is what the
 * builder needs to fix it.
 */
export const ADAPTIVE_APIS = Object.freeze([
  'WindowSizeClass',
  'calculateWindowSizeClass',
  'currentWindowAdaptiveInfo',
  'ListDetailSceneStrategy',
  'SupportingPaneSceneStrategy',
  'rememberListDetailSceneStrategy',
  'rememberSupportingPaneSceneStrategy',
  'NavigationSuiteScaffold',
  'GridCells.Adaptive',
  'StaggeredGridCells.Adaptive'
])

/**
 * Calls that can own a text label.
 *
 * `Text` is a host because a fixed `Modifier.width` on the `Text` itself is the same defect, and the
 * layout containers are hosts because a fixed extent on one of them clips every label inside it. The
 * list is closed on purpose: a name that is not here is not judged, which is why an unlisted wrapper
 * composing a `Row` still gets its `Row` judged.
 */
export const TEXT_HOSTS = Object.freeze([
  'Text', 'Button', 'OutlinedButton', 'TextButton', 'FilledTonalButton', 'ElevatedButton',
  'IconButton', 'FilledIconButton', 'OutlinedIconButton',
  'Surface', 'Card', 'ElevatedCard', 'OutlinedCard',
  'FilterChip', 'AssistChip', 'InputChip', 'SuggestionChip',
  'Box', 'Row', 'Column', 'MenuItem', 'DropdownMenuItem'
])

/** Calls that take a click, for the accessible-name and touch-target rules. */
export const CLICKABLE_HOSTS = Object.freeze([
  'Button', 'OutlinedButton', 'TextButton', 'FilledTonalButton', 'ElevatedButton',
  'IconButton', 'FilledIconButton', 'OutlinedIconButton',
  'Surface', 'Card', 'ElevatedCard', 'OutlinedCard',
  'FilterChip', 'AssistChip', 'InputChip', 'SuggestionChip',
  'Box', 'Row', 'Column', 'MenuItem', 'DropdownMenuItem', 'ListItem'
])

/** Material 3's floor, and the one number in this module that is not derived: it is the published rule. */
export const MIN_TOUCH_TARGET_DP = 48

/**
 * A fixed extent declared on a modifier chain.
 *
 * Deliberately narrow, and each exclusion is a false positive that was considered and rejected:
 *   - `widthIn` / `heightIn` are not listed at all. `heightIn(min = 48.dp)` *is* the correct
 *     idiom for a touch target, and a rule that fired on it would punish the fix for the a11y rule;
 *     a single-argument `widthIn(200.dp)` is rare enough that catching the popular `width` and `size`
 *     forms is worth more than the coverage.
 *   - a `sp` value is never matched, only `dp`: `fontSize` is not an extent.
 *   - a bare number with no `.dp` (a fraction, as in `fillMaxWidth(0.6f)`) is not matched.
 */
const EXTENT_DECL = /\.(width|height|size|requiredWidth|requiredHeight|requiredSize)\s*\(\s*(?:(width|height)\s*=\s*)?([0-9]+(?:\.[0-9]+)?)\.dp/g

/**
 * Every fixed extent in a chunk of source, from a *fresh* regular expression.
 *
 * A module-level `/g` regex carries `lastIndex` between uses, and `matchAll` starts from it rather than
 * from zero — the state left by the previous call site would silently skip the first extent of the next
 * one. Building the expression per call removes the state instead of remembering to reset it.
 */
function allExtents(text) {
  return [...text.matchAll(new RegExp(EXTENT_DECL.source, 'g'))]
}

/** The first fixed extent, or null. Same reason as `allExtents`: no shared state. */
function firstExtent(text) {
  return new RegExp(EXTENT_DECL.source, 'g').exec(text)
}

/** A clamp that cannot indicate its own truncation. `(?!\d)` keeps `maxLines = 10` out. */
const CLAMP_ONLY = /maxLines\s*=\s*1(?!\d)/

/** Every API that can animate something the user sees. */
const ANIMATION_PATTERNS = Object.freeze([
  /\bNavDisplay\s*\(/,
  /\bAnimatedContent\s*\(/,
  /\bAnimatedVisibility\s*\(/,
  /\bCrossfade\s*\(/,
  /\banimateContentSize\s*\(/,
  /\bSharedTransitionLayout\s*\(/,
  /\bAnimatedPane\s*\(/,
  /\bupdateTransition\s*\(/,
  /\bAnimatable\s*\(/,
  /\banimateItem\s*\(/,
  /\banimate[A-Za-z]*AsState\s*\(/,
  /\bAnimatedNavHost\s*\(/
])

/** The animation APIs, by name, for the message a finding prints. Kept beside the patterns below. */
const ANIMATION_API_NAMES = Object.freeze([
  'NavDisplay', 'AnimatedContent', 'AnimatedVisibility', 'Crossfade', 'animateContentSize',
  'SharedTransitionLayout', 'AnimatedPane', 'updateTransition', 'Animatable', 'animateItem',
  'animate*AsState', 'AnimatedNavHost'
])

const SOURCE_CODE = /\.(?:kt|java)$/i
const slotSourceDir = (slotDir) => path.join(slotDir, 'app', 'src', 'main')
const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/**
 * The path a finding prints: relative to the repository when the file is inside it, and relative to
 * the slot otherwise, so a synthetic fixture tree reads as `app/src/main/java/...` instead of as a
 * temporary directory's absolute path.
 *
 * `path.isAbsolute` is part of the test, not decoration: on Windows a file on another drive than the
 * repository makes `path.relative` return an absolute path rather than one starting with `..`, so the
 * status-code-only check printed a fixture's full temporary path as if it were a factory path.
 */
const displayPath = (slotDir, file) => {
  const fromRoot = path.relative(ROOT, file)
  if (fromRoot && !fromRoot.startsWith('..') && !path.isAbsolute(fromRoot)) return fromRoot.split(path.sep).join('/')
  return path.relative(slotDir, file).split(path.sep).join('/')
}

const lineOf = (text, index) => text.slice(0, index).split(/\r?\n/).length

// ---------------------------------------------------------------------------------------------
// Primitives: reading Kotlin as code rather than as text
// ---------------------------------------------------------------------------------------------

/**
 * Blanks comments and string literals, preserving length, newlines and therefore line numbers.
 *
 * The ordering inside this loop is the whole point: `//` must be tested before `/*`, and both before
 * the single-character cases, or a `//` comment opening with an apostrophe would be read as a char
 * literal and swallow the rest of the file. `decodeSource` guards the other direction — a file with a
 * NUL byte is not text and is never read at all.
 *
 * Blanking a literal is what makes the sweep honest in both directions: an API named in a comment or
 * in a `TODO("use NavDisplay here")` is not a use, and a fixed extent written inside a string is not
 * a modifier. `split('')` rather than `Array.from` is deliberate: it iterates UTF-16 code units, which
 * is what `text[i]` indexes, so the result stays the same length as the input.
 */
export function blankSpans(text) {
  const out = text.split('')
  const blank = (index) => {
    if (out[index] !== '\n' && out[index] !== '\r') out[index] = ' '
  }
  let i = 0
  while (i < text.length) {
    if (text.startsWith('//', i)) {
      while (i < text.length && text[i] !== '\n') {
        blank(i)
        i += 1
      }
      continue
    }
    if (text.startsWith('/*', i)) {
      const end = text.indexOf('*/', i + 2)
      const stop = end === -1 ? text.length : end + 2
      while (i < stop) {
        blank(i)
        i += 1
      }
      continue
    }
    if (text.startsWith('"""', i)) {
      const end = text.indexOf('"""', i + 3)
      const stop = end === -1 ? text.length : end + 3
      while (i < stop) {
        blank(i)
        i += 1
      }
      continue
    }
    const quote = text[i]
    if (quote === '"' || quote === "'" || quote === '`') {
      blank(i)
      i += 1
      while (i < text.length && text[i] !== quote) {
        // An unterminated literal must not swallow the rest of the file: a newline ends it.
        if (text[i] === '\n') break
        if (text[i] === '\\') {
          blank(i)
          i += 1
          if (i < text.length) {
            blank(i)
            i += 1
          }
          continue
        }
        blank(i)
        i += 1
      }
      if (i < text.length && text[i] === quote) {
        blank(i)
        i += 1
      }
      continue
    }
    i += 1
  }
  return out.join('')
}

/** The index of the `)` closing the `(` at `open`, or -1. */
function matchParen(text, open) {
  let depth = 0
  for (let i = open; i < text.length; i += 1) {
    if (text[i] === '(') depth += 1
    else if (text[i] === ')') {
      depth -= 1
      if (depth === 0) return i
    }
  }
  return -1
}

/** The index of the `}` closing the `{` at `open`, or -1. */
function matchBrace(text, open) {
  let depth = 0
  for (let i = open; i < text.length; i += 1) {
    if (text[i] === '{') depth += 1
    else if (text[i] === '}') {
      depth -= 1
      if (depth === 0) return i
    }
  }
  return -1
}

/** The index of the `>` closing the `<` at `open`, or -1. */
function matchAngle(text, open) {
  let depth = 0
  for (let i = open; i < text.length; i += 1) {
    if (text[i] === '<') depth += 1
    else if (text[i] === '>') {
      depth -= 1
      if (depth === 0) return i
    }
  }
  return -1
}

const skipSpace = (text, from) => {
  let i = from
  while (i < text.length && /\s/.test(text[i])) i += 1
  return i
}

/**
 * Every `Name(…)` call site, with the balanced span of its arguments *and* its trailing lambda.
 *
 * Explicit type arguments are part of the call, and missing them is not a small error: the scaffold's
 * `mutableStateListOf<PlayKilnDestination>(HomeDestination)` is why a `\s*\(` pattern reported the
 * *hand-rolled back stack in the factory's own template* as clean. Kotlin allows `<…>` between the name
 * and the `(`, so the scanner skips it with depth counting rather than a regex that stops at the first
 * `>` of a nested type.
 *
 * The trailing lambda is likewise not optional detail: `Button(…) { Text(…) }` is one subject, and a
 * span that stopped at the closing paren would report every labelled button as "clickable with no
 * accessible name" and would miss every fixed extent on a container. An identifier character
 * immediately before the match is how a longer name ending in the same word is rejected — `BasicText(`
 * is not a `Text(`, and `OutlinedTextField(` does not contain a `TextField(` call site.
 *
 * `argsSpan` keeps the call name and the argument list, offset-aligned with the whole site, which is
 * what a rule about the call's *own* modifiers must read: a fixed width written inside the trailing
 * lambda belongs to whatever call is written there, not to the container that holds it.
 */
export function callSites(masked, name) {
  const sites = []
  let from = 0
  for (;;) {
    const at = masked.indexOf(name, from)
    if (at === -1) break
    from = at + name.length
    if (at > 0 && /[A-Za-z0-9_$]/.test(masked[at - 1])) continue
    let j = skipSpace(masked, at + name.length)
    if (masked[j] === '<') {
      const angle = matchAngle(masked, j)
      if (angle === -1) continue
      j = skipSpace(masked, angle + 1)
    }
    if (masked[j] !== '(') continue
    const open = j
    const close = matchParen(masked, open)
    if (close === -1) continue
    let end = close
    const after = skipSpace(masked, close + 1)
    if (masked[after] === '{') {
      const brace = matchBrace(masked, after)
      if (brace !== -1) end = brace
    }
    sites.push({
      name,
      index: at,
      line: lineOf(masked, at),
      args: masked.slice(open + 1, close),
      argsSpan: masked.slice(at, close + 1),
      span: masked.slice(at, end + 1)
    })
  }
  return sites
}

/**
 * Every text-field call site.
 *
 * `callSites` already rejects a match whose preceding character is an identifier character, so
 * `OutlinedTextField(` never arrives here as a `TextField(` site and is not counted twice.
 */
function fieldSites(masked) {
  const sites = []
  for (const name of ['OutlinedTextField', 'BasicTextField', 'TextField']) {
    for (const site of callSites(masked, name)) sites.push({ ...site, name })
  }
  return sites.sort((a, b) => a.index - b.index)
}

/**
 * Every call site of the hosts that take a click.
 *
 * The click is looked for in the call's **own arguments**, never in its trailing lambda, because a
 * container is not clickable just because a descendant is: `Row(…) { sections.forEach { SectionPill(
 * onClick = …) } }` matched `onClick` inside the lambda and was reported as a nameless clickable `Row`,
 * which is a finding about the wrong element and therefore noise.
 */
function clickableSites(source) {
  const sites = []
  for (const name of CLICKABLE_HOSTS) {
    for (const site of callSites(source.masked, name)) {
      if (!/\bonClick\s*=|\.clickable\s*\(|\bonLongClick\s*=/.test(site.argsSpan)) continue
      // A component that takes its content as a slot is labelled by its caller, and this scan cannot
      // see the call site's text: `ClickablePanel(modifier, onClick, content)` is not a nameless
      // element, it is an element whose name is written where the label is passed.
      if (/\b(?:content|children|slot|label)\s*\(\s*\)/.test(site.span)) continue
      sites.push(site)
    }
  }
  return sites
}

/**
 * The Kotlin/Java UI sources of one slot-like tree, decoded and masked.
 *
 * `build/` is skipped by the shared walker, which is the rule that keeps a stale generated copy of a
 * source file from producing a finding.
 */
export function readKotlin(slotDir) {
  const sources = []
  for (const file of walkSlotFiles(slotSourceDir(slotDir))) {
    if (!SOURCE_CODE.test(file)) continue
    let text = null
    try {
      text = decodeSource(readFileSync(file))
    } catch {
      continue
    }
    if (text === null) continue
    sources.push({ file, rel: displayPath(slotDir, file), text, masked: blankSpans(text) })
  }
  return sources
}

// ---------------------------------------------------------------------------------------------
// The sensors. Each returns `string[]`; none of them decides a verdict.
// ---------------------------------------------------------------------------------------------

/**
 * Navigation 3 is the host, and the back stack is the one Navigation 3 owns.
 *
 * The measured defect: the scaffold already hosts the UI in `NavDisplay`, and both it and every shipped
 * slot replaced `rememberNavBackStack` with a hand-rolled `SnapshotStateList` plus a `listSaver`. What
 * that costs is not style — it is that `NavDisplay`'s transitions, predictive-back continuity, scene
 * strategies and deep-link handling all key off the back stack it is given, so a hand-rolled list
 * disables all of them at once.
 *
 * The call test goes through `callSites` rather than a regex, and that is the fix for a real blindness:
 * `mutableStateListOf<PlayKilnDestination>(…)` carries explicit type arguments, so
 * `/mutableStateListOf\s*\(/` did not match the hand-rolled back stack in the factory's own template and
 * reported it clean.
 */
export function navigationFindings(sources) {
  const findings = []
  const uses = (source, name) => callSites(source.masked, name).length > 0
  const displays = sources.flatMap((source) => callSites(source.masked, 'NavDisplay').map((site) => ({ source, site })))
  if (displays.length === 0) {
    findings.push(
      'no NavDisplay call anywhere under app/src/main, so the UI is navigated by hand: Navigation 3 ' +
        'provides the host, its transitions, predictive back and the scene-strategy hook, and none of ' +
        'them can engage without it (see docs/ui-ux-standards.md, "Navigation")'
    )
  }
  for (const source of sources) {
    if (!uses(source, 'mutableStateListOf') && !uses(source, 'listSaver')) continue
    if (uses(source, 'rememberNavBackStack')) continue
    if (callSites(source.masked, 'NavDisplay').length === 0) continue
    const site = [...callSites(source.masked, 'mutableStateListOf'), ...callSites(source.masked, 'listSaver')].sort(
      (a, b) => a.index - b.index
    )[0]
    findings.push(
      `${source.rel}:${site.line}: the back stack is a hand-rolled saved list (` +
        '`mutableStateListOf` / `listSaver`) instead of `rememberNavBackStack`, so NavDisplay is driven ' +
        'by a list it does not own and its transitions, predictive back and scene strategy are inert ' +
        '(see docs/ui-ux-standards.md, "Navigation")'
    )
  }
  return findings
}

/** Spans where navigation keys are handled, across the whole slot: `NavDisplay(…)` and `NavEntry(…)`. */
function navigationSpans(sources) {
  const spans = []
  for (const source of sources) {
    for (const name of ['NavDisplay', 'NavEntry']) {
      for (const site of callSites(source.masked, name)) spans.push(site.span)
    }
  }
  return spans.join('\n')
}

/**
 * The declaration of one navigation key, or null when the name is not a declared destination.
 *
 * `owner` is the qualifier the host branched on (`Destination` in `is Destination.Classroom`), and when
 * it is known the file must also declare `sealed interface`/`sealed class <owner>` *before* the member
 * for the match to count. That test is not decoration: without it a `data class Classroom` in
 * `domain/model/RecordBook.kt` was reported as the slot's navigation destination, because the name
 * appears both in the domain model and in `Destination.Classroom`. A destination is a member of the
 * sealed hierarchy the host branches on, and the owner prefix is what says which declaration it is.
 */
function declarationOf(sources, key) {
  const dot = key.lastIndexOf('.')
  const owner = dot === -1 ? null : key.slice(0, dot)
  const short = dot === -1 ? key : key.slice(dot + 1)
  for (const source of sources) {
    const declared = new RegExp(`\\bdata\\s+(?:class|object)\\s+${escapeRegExp(short)}\\b`).exec(source.masked)
    if (!declared) continue
    if (owner !== null) {
      const sealed = new RegExp(`\\bsealed\\s+(?:interface|class)\\s+${escapeRegExp(owner)}\\b`).exec(source.masked)
      if (!sealed || sealed.index > declared.index) continue
    }
    return {
      rel: source.rel,
      line: lineOf(source.masked, declared.index),
      payload: declaresPayload(source.masked.slice(declared.index))
    }
  }
  return null
}

/**
 * Whether a declaration carries a payload — `val` members, which is what makes it a detail view rather
 * than a tab.
 *
 * The window is the declaration's own parameter list, not a fixed number of following characters: a
 * character window from `data object Classes : Destination` reached into the `data class Classroom(val
 * classId: String, …)` declared two lines later, so a payload-free tab was reported as the slot's detail
 * destination and the finding pointed at the wrong line.
 */
function declaresPayload(text) {
  const open = text.indexOf('(')
  const lineEnd = text.indexOf('\n')
  if (open === -1 || (lineEnd !== -1 && lineEnd < open)) return false
  const close = matchParen(text, open)
  return close !== -1 && /\bval\s+[A-Za-z0-9_]/.test(text.slice(open + 1, close))
}

/**
 * The destinations the navigation host branches on, each with the line that declares it.
 *
 * A key is a name the host's own `when` compares — `is Destination.Classroom ->` or, for a `data object`,
 * `HomeDestination ->` — *and* a name some `data class`/`data object` in the slot declares. Anchoring
 * both halves is what keeps a lambda parameter out of the list: an uppercase lambda binder has no
 * declaration, and a domain model has no branch.
 */
export function navigationKeys(sources) {
  const nav = navigationSpans(sources)
  if (!nav) return []
  const names = new Set()
  for (const match of nav.matchAll(/\bis\s+([A-Za-z0-9_]+(?:\.[A-Z][A-Za-z0-9_]*)?)\s*(?:->|=>|\{|\()/g)) names.add(match[1])
  for (const match of nav.matchAll(/(?:^|[\s{;(=,>])([A-Z][A-Za-z0-9_]*(?:\.[A-Z][A-Za-z0-9_]*)?)\s*->/g)) names.add(match[1])
  const keys = []
  for (const name of names) {
    const declared = declarationOf(sources, name)
    if (declared) keys.push({ key: name, ...declared })
  }
  return keys.sort((a, b) => a.line - b.line)
}

/** Which of `ADAPTIVE_APIS` the slot *uses*, in the table's order. */
export function usedAdaptiveApis(sources) {
  return ADAPTIVE_APIS.filter((api) => sources.some((source) => new RegExp(`\\b${escapeRegExp(api)}\\b`).test(withoutImports(source.masked))))
}

/**
 * The same source with every `import` line blanked, length and newlines preserved.
 *
 * Only `usedAdaptiveApis` reads masked text with a bare regex, and without this an *unused* import of
 * `ListDetailSceneStrategy` satisfied the adaptive rule without a single adaptive layout existing —
 * a false negative in the one place a slot could silence the rule by adding a line. Blanking the
 * import line keeps the rule reading the file's code, not its dependency list, which is the same
 * reason `blankSpans` blanks comments: a name that is not a use must not be counted as one.
 */
export function withoutImports(masked) {
  return masked
    .split('\n')
    .map((line) => (/^\s*import\b/.test(line) ? ' '.repeat(line.length) : line))
    .join('\n')
}

/**
 * A detail destination in a slot that never asks how wide the window is.
 *
 * The measured defect: `WindowSizeClass` and every other adaptive API appeared nowhere in this
 * repository, so an app with a list and a detail screen rendered the detail screen *instead of* the
 * list on a tablet or an unfolded phone, with a column stretched to a width no text is set for. The
 * rule fires only on a slot that actually has a detail destination, because a single-screen app has
 * no two-pane policy to state and failing it would be noise.
 */
export function adaptiveFindings(sources) {
  const used = usedAdaptiveApis(sources)
  if (used.length > 0) return []
  const keys = navigationKeys(sources)
  if (keys.length < 2) return []
  const detail = keys.find((entry) => entry.payload) || keys[0]
  const suffix = detail.payload
    ? ' — the list and the destination it opens are shown one after the other instead of side by side'
    : ''
  return [
    `${detail.rel}:${detail.line}: the navigation host branches on ${keys.length} destinations ` +
      `(${keys.map((entry) => entry.key).join(', ')}) but the slot names none of ${ADAPTIVE_APIS.join(', ')}, ` +
      `so the window size class is never read and every device renders the same single column${suffix} ` +
      '(see docs/ui-ux-standards.md, "Adaptive layout")'
  ]
}

/**
 * The text-wrapping sensor: the causes of a string that cannot do anything but wrap.
 *
 * Two rules, both of which the reference slot provably violates and neither of which fires on the
 * scaffold (the measurements are in the report and in `docs/ui-ux-standards.md`):
 *
 *   1. a fixed extent on an element that holds text. `Modifier.size(width = 96.dp, height = 48.dp)`
 *      around a label is a box that cannot grow; at a font scale of 2.0 — the maximum Android offers,
 *      and the class of defect the owner saw — the text has no option but to wrap into the narrowest
 *      column it can, and no typography setting can rescue it. This is WCAG SC 1.4.4/1.4.12 read as a
 *      static property of the container.
 *   2. a `maxLines = 1` clamp with no `overflow`, which is `TextOverflow.Clip`: the user sees a glyph
 *      sliced in half with nothing to say the text continues.
 *
 * `isSelf` distinguishes the subject from its container, and together with `argsSpan` it is what keeps
 * one defect from being reported twice. A `Text` is the text, so a fixed extent in its own argument
 * list is the finding. A `Row`/`Button`/`Surface` is only a finding when the extent is in *its* argument
 * list and a `Text` call appears anywhere in the call — because a fixed-width container with no text in
 * it cannot wrap anything, and because a fixed width written inside the lambda belongs to whatever call
 * is written there. `Row { Text(label, Modifier.width(80.dp)) }` is therefore one finding, on the
 * `Text`, not two. A span that already uses `weight(` is skipped: a weighted child is a flexible width
 * by construction, and firing there would be a false positive. That exemption is an under-approximation,
 * and it is named as one in `docs/ui-ux-standards.md`.
 */
function textLayoutRules(source) {
  const findings = []
  const subjects = [
    ...callSites(source.masked, 'Text').map((site) => ({ ...site, isSelf: true })),
    ...fieldSites(source.masked).map((site) => ({ ...site, isSelf: true })),
    ...TEXT_HOSTS.filter((name) => name !== 'Text').flatMap((name) =>
      callSites(source.masked, name).map((site) => ({ ...site, isSelf: false }))
    )
  ]

  for (const site of subjects) {
    if (/\.weight\s*\(/.test(site.span)) continue
    const match = firstExtent(site.argsSpan)
    if (!match) continue
    if (!site.isSelf && !/\bText\s*\(/.test(site.span)) continue
    const dimension = match[2] ? `${match[2]} = ` : ''
    findings.push(
      `${source.rel}:${lineOf(source.masked, site.index + match.index)}: ${site.name} sets a fixed ` +
        `extent (${match[1]}(${dimension}${match[3]}.dp)) on an element that holds text, so at a large ` +
        'font scale the text can only wrap inside it — use a flexible extent and let the typography ' +
        'scale (see docs/ui-ux-standards.md, "Text must be able to wrap and to grow")'
    )
  }

  for (const site of subjects) {
    const clamp = CLAMP_ONLY.exec(site.argsSpan)
    if (!clamp) continue
    if (/\boverflow\s*=|TextOverflow/.test(site.argsSpan)) continue
    findings.push(
      `${source.rel}:${lineOf(source.masked, site.index + clamp.index)}: ${site.name} clamps to ` +
        '`maxLines = 1` with no `overflow`, which is `TextOverflow.Clip`, so a truncated label shows a ' +
        'cut glyph with no indication that there is more (see docs/ui-ux-standards.md, "Text must be ' +
        'able to wrap and to grow")'
    )
  }
  return findings
}

/** `textLayoutFindings` over already-read sources, so a check does not read the tree twice. */
export function textLayoutFrom(sources) {
  return sources.flatMap((source) => textLayoutRules(source))
}

/**
 * A multi-screen slot that animates nothing.
 *
 * Motion is what tells the user where a screen came from, and predictive back only has something to
 * show when the transition is continuous. The rule is deliberately weak — one animation API anywhere
 * is enough, and `NavDisplay`'s own default transitions count — because the honest, measurable claim
 * is "nothing in this slot animates", not "this slot's motion is not to taste". A single-screen slot
 * is not judged: it has no navigation to make continuous.
 */
export function animationSites(sources) {
  const sites = []
  for (const source of sources) {
    for (const pattern of ANIMATION_PATTERNS) {
      const match = pattern.exec(source.masked)
      if (match) sites.push({ rel: source.rel, line: lineOf(source.masked, match.index), api: match[0].replace(/[\s(]+$/, '') })
    }
  }
  return sites
}

/** The distinct `*Screen` composables and classes a slot declares. */
export function screenNames(sources) {
  const names = new Set()
  for (const source of sources) {
    for (const match of source.masked.matchAll(/\bfun\s+([A-Z][A-Za-z0-9_]*(?:Screen|Route))\s*\(/g)) names.add(match[1])
    for (const match of source.masked.matchAll(/\bclass\s+([A-Z][A-Za-z0-9_]*Screen)\b/g)) names.add(match[1])
  }
  return [...names]
}

export function motionFindings(sources) {
  const screens = screenNames(sources)
  if (screens.length < 2) return []
  const sites = animationSites(sources)
  if (sites.length > 0) return []
  return [
    `the slot declares ${screens.length} screens (${screens.join(', ')}) and no animation API anywhere ` +
      `(${ANIMATION_API_NAMES.slice(0, 6).join(', ')}, …), so every navigation is an instant swap with ` +
      'no continuity for predictive back to follow (see docs/ui-ux-standards.md, "Motion")'
  ]
}

/** The accessible name of an interactive element, as far as source can tell. */
const hasAccessibleName = (span) => /\bText\s*\(/.test(span) || /contentDescription\s*=\s*(?!null\b)/.test(span) || /\bonClickLabel\s*=/.test(span)

/**
 * Accessibility semantics and touch targets.
 *
 * Three claims, each of which names the file and the line:
 *
 *   1. an `Icon`/`Image` with no `contentDescription` argument at all, so TalkBack announces nothing;
 *   2. a clickable element with neither a `Text` child nor a non-null `contentDescription`, so the
 *      control has no name to announce;
 *   3. a clickable element declaring an explicit extent below Material 3's 48 dp minimum.
 *
 * `contentDescription = null` is accepted rather than flagged, and that is a deliberate
 * under-approximation: whether an icon is decorative depends on the screen around it, and a scan that
 * guesses would produce the false positives that make an operator read past the gate. Rule 3 cannot
 * see a target that is too small because it was never declared — it only catches a declared number
 * below the floor, which is the case that is provable and which P-K measured on the reference slot.
 */
export function a11yFindings(sources) {
  const findings = []
  for (const source of sources) {
    for (const name of ['Icon', 'Image']) {
      for (const site of callSites(source.masked, name)) {
        if (/\bcontentDescription\s*=/.test(site.args)) continue
        findings.push(
          `${source.rel}:${site.line}: ${name}(…) declares no contentDescription argument, so TalkBack ` +
            'announces nothing for it — pass a label, or an explicit `null` where the icon is decorative ' +
            '(see docs/ui-ux-standards.md, "Accessibility")'
        )
      }
    }
    for (const site of clickableSites(source)) {
      if (!hasAccessibleName(site.span)) {
        findings.push(
          `${source.rel}:${site.line}: ${site.name} is clickable and carries neither a Text nor a ` +
            'non-null contentDescription, so it has no accessible name (see docs/ui-ux-standards.md, ' +
            '"Accessibility")'
        )
      }
      for (const match of allExtents(site.argsSpan)) {
        const value = Number(match[3])
        if (value >= MIN_TOUCH_TARGET_DP) continue
        findings.push(
          `${source.rel}:${lineOf(source.masked, site.index + match.index)}: ${site.name} declares a ` +
            `${value}.dp ${match[1]}, below Material 3's ${MIN_TOUCH_TARGET_DP} dp minimum touch target, ` +
            'so the control is hard to hit (see docs/ui-ux-standards.md, "Accessibility")'
        )
      }
    }
  }
  return findings
}

/**
 * Edge-to-edge and the IME.
 *
 * `enableEdgeToEdge()` is mandatory at `targetSdk` 36, and the keyboard is the inset that is *not* a
 * system bar: `Scaffold`'s own `contentWindowInsets` does not carry it, the platform only resizes the
 * window for it when the activity says `adjustResize`, and a slot that declares neither can leave the
 * field the user is typing into underneath the keyboard. The rule is only read when the slot shows a
 * text field, because a slot with no input has no IME to place.
 */
export function insetFindings(slotDir, sources) {
  const findings = []
  if (!sources.some((source) => /\benableEdgeToEdge\s*\(/.test(source.masked))) {
    findings.push(
      'no enableEdgeToEdge() in any app/src/main source: edge-to-edge is mandatory at targetSdk 36, ' +
        'and an app that does not opt in gets a scrimmed letterbox instead of the drawn-to-the-edges ' +
        'layout the slot was designed for (see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
    )
  }
  const fields = sources.flatMap((source) => fieldSites(source.masked).map((site) => ({ source, site })))
  if (fields.length === 0) return findings
  const where = `${fields[0].source.rel}:${fields[0].site.line}`

  const manifest = readManifestState(slotDir)
  if (!manifest.text) {
    findings.push(
      `no ${displayPath(slotDir, path.join(slotDir, MANIFEST_REL))} to read, so the soft-input declaration ` +
        `for the text field at ${where} could not be decided`
    )
  } else if (!/windowSoftInputMode/.test(manifest.text)) {
    findings.push(
      `${displayPath(slotDir, manifest.file)}: the slot shows a text field (${where}) but no <activity> ` +
        'declares android:windowSoftInputMode="adjustResize", so the platform pans the window instead of ' +
        'resizing it and the field can end up under the keyboard (see docs/ui-ux-standards.md, ' +
        '"Edge-to-edge and the IME")'
    )
  }
  if (!/imePadding\s*\(|fitInside\s*\(|WindowInsets\.ime|WindowInsetsRulers\.Ime/.test(sources.map((s) => s.masked).join('\n'))) {
    findings.push(
      `the slot shows a text field (${where}) but no imePadding()/fitInside(WindowInsetsRulers.Ime…) and no ` +
        'WindowInsets.ime reference: the IME inset is not applied by Scaffold default, so nothing in the ' +
        'layout reacts to the keyboard (see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
    )
  }
  return findings
}

// ---------------------------------------------------------------------------------------------
// The pure sensors, exported for measurement and for the red-before-green demonstration
// ---------------------------------------------------------------------------------------------

/** Every UI/UX finding for one slot, as file-and-line strings. No verdicts, no baseline scoping. */
export function uiuxFindings(slotDir) {
  const sources = readKotlin(slotDir)
  return [
    ...navigationFindings(sources),
    ...adaptiveFindings(sources),
    ...textLayoutFrom(sources),
    ...motionFindings(sources),
    ...a11yFindings(sources),
    ...insetFindings(slotDir, sources)
  ]
}

/** The text-wrapping / typography sensor on its own, which is the rule the owner's review was about. */
export function textLayoutFindings(slotDir) {
  return textLayoutFrom(readKotlin(slotDir))
}

// ---------------------------------------------------------------------------------------------
// The verdicts
// ---------------------------------------------------------------------------------------------

const SENSORS = Object.freeze({
  'uiux.navigation': (slotDir) => navigationFindings(readKotlin(slotDir)),
  'uiux.adaptive': (slotDir) => adaptiveFindings(readKotlin(slotDir)),
  'uiux.text-layout': (slotDir) => textLayoutFindings(slotDir),
  'uiux.motion': (slotDir) => motionFindings(readKotlin(slotDir)),
  'uiux.a11y': (slotDir) => a11yFindings(readKotlin(slotDir)),
  'uiux.insets': (slotDir) => insetFindings(slotDir, readKotlin(slotDir))
})

/** The summaries a passing check prints. Each is built from what was measured, never asserted. */
function summaries(slotDir, sources) {
  const displays = sources.reduce((total, source) => total + callSites(source.masked, 'NavDisplay').length, 0)
  const nav3Stack = sources.some((source) => /rememberNavBackStack\s*\(/.test(source.masked))
  const adaptive = usedAdaptiveApis(sources)
  const icons = sources.reduce((total, source) => total + callSites(source.masked, 'Icon').length + callSites(source.masked, 'Image').length, 0)
  const clickables = sources.reduce((total, source) => total + clickableSites(source).length, 0)
  const fields = sources.reduce((total, source) => total + fieldSites(source.masked).length, 0)
  const animations = animationSites(sources).length
  const screens = screenNames(sources)
  const manifest = readManifestState(slotDir)
  return {
    'uiux.navigation': `${displays} NavDisplay call site(s); back stack ${nav3Stack ? 'from rememberNavBackStack' : 'not via rememberNavBackStack'}`,
    'uiux.adaptive':
      adaptive.length > 0
        ? `adaptive APIs in use: ${adaptive.join(', ')}`
        : 'no detail destination in the slot, so there is no two-pane policy to state',
    'uiux.text-layout': `no fixed extent on a text-bearing element and no maxLines clamp without an overflow in ${sources.length} Kotlin file(s)`,
    'uiux.motion':
      screens.length < 2
        ? `${screens.length} screen composable(s), so no navigation motion to judge`
        : `${animations} animation API site(s) across ${screens.length} screen(s)`,
    'uiux.a11y': `${icons} Icon/Image call(s) each with a contentDescription argument; ${clickables} clickable element(s) each with an accessible name and no declared extent below ${MIN_TOUCH_TARGET_DP} dp`,
    'uiux.insets':
      fields === 0
        ? 'enableEdgeToEdge() present; the slot shows no text field, so there is no IME placement to judge'
        : `enableEdgeToEdge() present; ${fields} text field(s) with ${manifest.text && /windowSoftInputMode/.test(manifest.text) ? 'a soft-input declaration' : 'no soft-input declaration'} and IME inset handling`
  }
}

/**
 * Every UI/UX verdict for one slot.
 *
 * The whole set is built in one pass and a thrown error turns *every* id `UNVERIFIED` rather than a
 * single umbrella row, because a check that could not run must not read as a check that passed, and an
 * id that disappears from a report cannot be told apart from an id that does not exist. That is
 * `guard`'s contract applied to a list of ids, which is the one place the shape here differs from the
 * other modules' `*Checks`.
 */
export function uiuxChecks(slotDir) {
  try {
    const sources = readKotlin(slotDir)
    if (sources.length === 0) {
      const detail = `no Kotlin source under ${displayPath(slotDir, slotSourceDir(slotDir))} to read; nothing scanned must not read as nothing found`
      return UIUX_IDS.map((id) => unverified(id, detail))
    }
    const text = summaries(slotDir, sources)
    return UIUX_IDS.map((id) => baselineVerdict(id, slotDir, SENSORS[id](slotDir), text[id]))
  } catch (error) {
    return UIUX_IDS.map((id) => unverified(id, `could not evaluate: ${error.message}`))
  }
}

// ---------------------------------------------------------------------------------------------
// The factory's own half: the scaffold carries the baseline, and the rule text agrees with it
// ---------------------------------------------------------------------------------------------

/** The ids the rule text must name: built from the constants, so the message cannot drift. */
export const UIUX_RULE_IDS = Object.freeze([...UIUX_IDS, ...UIUX_PROJECT_IDS])

/** The ids the rule text does not name. A rule the builder cannot read is a rule nobody follows. */
export function ruleTextProblems(ruleText) {
  return UIUX_RULE_IDS.filter((id) => !ruleText.includes(id))
}

/** The citations `docs/ui-ux-standards.md` does not carry. */
export function citationProblems(docText) {
  return UIUX_STANDARD_SOURCES.filter(([source]) => !docText.includes(source)).map(([source, claim]) => `${source} (${claim})`)
}

/** The vendored skills a citation points at that are not on disk. */
export function missingStandardFiles() {
  return UIUX_STANDARD_SOURCES.map(([source]) => source)
    .filter((source) => source.endsWith('.md'))
    .filter((source) => !existsSync(path.join(ROOT, ...source.split('/'))))
}

/** The rule a builder reads and the standard it cites, read back. */
function checkRuleAgreement() {
  return guard('uiux.rule', () => {
    const problems = []
    const ruleFile = path.join(ROOT, ...UIUX_RULE_REL.split('/'))
    const docFile = path.join(ROOT, ...UIUX_STANDARDS_REL.split('/'))
    if (!existsSync(ruleFile)) problems.push(`${UIUX_RULE_REL} does not exist, so no builder can read the baseline`)
    else {
      ruleTextProblems(readFileSync(ruleFile, 'utf8')).forEach((id) => problems.push(`${UIUX_RULE_REL} does not name the check id ${id}`))
    }
    if (!existsSync(docFile)) problems.push(`${UIUX_STANDARDS_REL} does not exist, so the citations behind the rules cannot be read`)
    else citationProblems(readFileSync(docFile, 'utf8')).forEach((citation) => problems.push(`${UIUX_STANDARDS_REL} does not cite ${citation}`))
    missingStandardFiles().forEach((file) => problems.push(`the cited skill ${file} is not on disk`))
    if (problems.length > 0) return fail('uiux.rule', problems.join('; '))
    return pass(
      'uiux.rule',
      `${UIUX_RULE_IDS.length} check id(s) named in ${UIUX_RULE_REL}; ${UIUX_STANDARD_SOURCES.length} external standard(s) cited in ${UIUX_STANDARDS_REL} and every cited skill present`
    )
  })
}

/**
 * The scaffold carries the baseline, or the factory is about to ship the defect again.
 *
 * This is the check that is expected to be RED until `scaffold/android-compose/` is wired, and it must
 * not be weakened to make it pass: the scaffold is what every new slot is copied from, so a scaffold
 * finding is the count of slots that would be born with the defect. The message says that, and lists
 * the findings in the same file-and-line form a slot reports, so the fix is a diff and not a reading.
 */
function checkScaffoldBaseline() {
  return guard('uiux.scaffold', () => {
    const scaffold = path.join(ROOT, 'scaffold', 'android-compose')
    if (!existsSync(scaffold)) {
      return unverified('uiux.scaffold', 'no scaffold/android-compose to measure the baseline against')
    }
    const sources = readKotlin(scaffold)
    if (sources.length === 0) {
      return unverified('uiux.scaffold', 'the scaffold carries no Kotlin source under app/src/main, so nothing was measured')
    }
    const findings = [
      ...navigationFindings(sources),
      ...adaptiveFindings(sources),
      ...textLayoutFrom(sources),
      ...motionFindings(sources),
      ...a11yFindings(sources),
      ...insetFindings(scaffold, sources)
    ]
    if (findings.length === 0) {
      return pass('uiux.scaffold', `the scaffold carries every UI/UX rule for the ${sources.length} Kotlin file(s) it holds`)
    }
    return fail(
      'uiux.scaffold',
      `the scaffold does not yet carry the baseline, so every slot copied from it is born with these ` +
        `${findings.length} finding(s): ${findings.join('; ')}`
    )
  })
}

/**
 * The factory's own UI/UX facts. Registered in the `invariants` group, so they run with no `--slot`.
 *
 * `uiux.scaffold` is allowed to be red here and is not a slot's fault; `uiux.rule` is green by
 * construction once the rule and this module agree, and it is the check that fails if someone deletes
 * the rule, drops a check id from it, or removes a citation the rules rest on.
 */
export function uiuxProjectChecks() {
  return [checkScaffoldBaseline(), checkRuleAgreement()]
}

// ---------------------------------------------------------------------------------------------
// Inline fixtures — each sensor driven with the value that must make it fire
// ---------------------------------------------------------------------------------------------

/**
 * A synthetic slot tree, written from inline strings and removed afterwards.
 *
 * A fixture must drive the *sensor* rather than the verdict: `baselineVerdict` would report a finding
 * on a temporary directory as a non-gating note (it is not a product slot in `registry/apps.json`),
 * so a fixture that went through `uiuxChecks` could not fail. `expectSensor` calls the finding
 * function directly for exactly that reason.
 */
function withFixtureSlot(files, body) {
  const dir = mkdtempSync(path.join(SCRATCH_DIR, 'kiln-uiux-'))
  try {
    for (const [rel, contents] of Object.entries(files)) {
      if (contents === null) continue
      const file = path.join(dir, ...rel.split('/'))
      mkdirSync(path.dirname(file), { recursive: true })
      writeFileSync(file, contents, 'utf8')
    }
    return body(dir)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
}

const UI_REL = 'app/src/main/java/com/gexetr/demo/ui'

const MANIFEST_OK = [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<manifest xmlns:android="http://schemas.android.com/apk/res/android">',
  '    <application android:label="@string/app_name">',
  '        <activity',
  '            android:name=".MainActivity"',
  '            android:exported="true"',
  '            android:windowSoftInputMode="adjustResize">',
  '            <intent-filter>',
  '                <action android:name="android.intent.action.MAIN" />',
  '                <category android:name="android.intent.category.LAUNCHER" />',
  '            </intent-filter>',
  '        </activity>',
  '    </application>',
  '</manifest>',
  ''
].join('\n')

/** The same manifest with the one attribute the soft-input rule reads removed. */
const MANIFEST_NO_SOFT_INPUT = MANIFEST_OK.replace(/\n\s*android:windowSoftInputMode="adjustResize"/, '')

/** A Navigation 3 app: `rememberNavBackStack`, an adaptive scene strategy, edge-to-edge, no fixed extent. */
const NAV3_APP = [
  'package com.gexetr.demo.ui',
  '',
  'import androidx.activity.compose.setContent',
  'import androidx.activity.enableEdgeToEdge',
  'import androidx.compose.foundation.layout.Column',
  'import androidx.compose.foundation.layout.imePadding',
  'import androidx.compose.material3.Text',
  'import androidx.compose.runtime.Composable',
  'import androidx.compose.ui.Modifier',
  'import androidx.compose.ui.text.style.TextOverflow',
  'import androidx.navigation3.runtime.NavEntry',
  'import androidx.navigation3.runtime.rememberNavBackStack',
  'import androidx.navigation3.ui.NavDisplay',
  '',
  'fun MainActivity.onCreate() {',
  '    enableEdgeToEdge()',
  '}',
  '',
  '@Composable',
  'fun HomeScreen() {',
  '    Column(modifier = Modifier.imePadding()) {',
  '        Text(stringResource(R.string.home_title))',
  '    }',
  '}',
  '',
  '@Composable',
  'fun DetailScreen(id: String) {',
  '    Text(stringResource(R.string.detail_title), maxLines = 1, overflow = TextOverflow.Ellipsis)',
  '}',
  '',
  '@Composable',
  'fun DemoApp() {',
  '    val backStack = rememberNavBackStack(Destination.Home)',
  '    NavDisplay(',
  '        backStack = backStack,',
  '        onBack = { backStack.removeLastOrNull() },',
  '        sceneStrategy = rememberListDetailSceneStrategy(),',
  '        entryProvider = { key ->',
  '            when (key) {',
  '                is Destination.Home -> NavEntry(key) { HomeScreen() }',
  '                is Destination.Detail -> NavEntry(key) { DetailScreen(key.id) }',
  '            }',
  '        }',
  '    )',
  '}',
  ''
].join('\n')

/** A detail destination with a payload, which is what makes the adaptive rule applicable. */
const NAV3_DESTINATIONS = [
  'package com.gexetr.demo.ui',
  '',
  'sealed interface Destination {',
  '    data object Home : Destination',
  '    data class Detail(val id: String) : Destination',
  '}',
  ''
].join('\n')

const demoFiles = (overrides = {}) => ({
  [MANIFEST_REL]: MANIFEST_OK,
  [`${UI_REL}/PlayKilnApp.kt`]: NAV3_APP,
  [`${UI_REL}/Destinations.kt`]: NAV3_DESTINATIONS,
  ...overrides
})

function findingsIn(files, sensor) {
  return withFixtureSlot(files, (dir) => sensor(dir))
}

/** Drives one sensor with a synthetic tree and asserts whether it fired, and on what. */
function expectSensor(id, files, fired, phrase) {
  const findings = findingsIn(files, SENSORS[id])
  expectEqual(findings.length > 0, fired, `${id} fired=${findings.length > 0} on ${JSON.stringify(Object.keys(files))}`)
  if (phrase && fired) {
    expectTrue(findings.join(' | ').includes(phrase), `${id} detail "${findings.join(' | ')}" does not name ${phrase}`)
  }
  return findings
}

export function uiuxFixtures() {
  return guard('uiux.fixtures', () =>
    runFixtures('uiux.fixtures', [
      // ---- uiux.navigation ----------------------------------------------------------------
      [
        'a hand-rolled saved back stack is a finding; rememberNavBackStack is not',
        () => {
          const handRolled = [
            'package com.gexetr.demo.ui',
            'import androidx.compose.runtime.mutableStateListOf',
            'import androidx.compose.runtime.saveable.listSaver',
            'import androidx.navigation3.ui.NavDisplay',
            '@Composable',
            'fun DemoApp() {',
            '    val backStack = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { mutableStateListOf() })) {',
            '        mutableStateListOf<Destination>(Destination.Home)',
            '    }',
            '    NavDisplay(backStack = backStack, onBack = { backStack.removeAt(backStack.lastIndex) }, entryProvider = { })',
            '}',
            ''
          ].join('\n')
          expectSensor('uiux.navigation', demoFiles({ [`${UI_REL}/PlayKilnApp.kt`]: handRolled }), true, 'rememberNavBackStack')
          expectSensor('uiux.navigation', demoFiles(), false)
        }
      ],
      [
        'a UI with no NavDisplay at all is a finding',
        () => {
          const manualSwitch = [
            'package com.gexetr.demo.ui',
            'import androidx.compose.runtime.getValue',
            '@Composable',
            'fun DemoApp() {',
            '    var screen by remember { mutableStateOf(0) }',
            '    when (screen) { 0 -> HomeScreen(); 1 -> DetailScreen() }',
            '}',
            ''
          ].join('\n')
          expectSensor('uiux.navigation', demoFiles({ [`${UI_REL}/PlayKilnApp.kt`]: manualSwitch }), true, 'NavDisplay')
        }
      ],
      [
        'an API named only inside a comment is not a use',
        () => {
          const anchor = 'val backStack = rememberNavBackStack(Destination.Home)'
          const commented = NAV3_APP.replace(
            anchor,
            '// TODO: rememberNavBackStack(Destination.Home) once the schema lands\n    val backStack = mutableStateListOf(Destination.Home)'
          )
          expectTrue(commented !== NAV3_APP, 'the fixture did not modify the app it drives')
          expectSensor('uiux.navigation', demoFiles({ [`${UI_REL}/PlayKilnApp.kt`]: commented }), true, 'rememberNavBackStack')
        }
      ],
      // ---- uiux.adaptive ------------------------------------------------------------------
      [
        'a detail destination with no adaptive API is a finding; naming one is not',
        () => {
          expectSensor('uiux.adaptive', demoFiles(), false) // NAV3_APP names rememberListDetailSceneStrategy
          expectSensor('uiux.adaptive', demoFiles({ [`${UI_REL}/Destinations.kt`]: NAV3_DESTINATIONS, [`${UI_REL}/PlayKilnApp.kt`]: NAV3_APP.replace('        sceneStrategy = rememberListDetailSceneStrategy(),\n', '') }), true, 'Detail')
        }
      ],
      [
        'a single-screen slot with no destination is not judged',
        () => {
          expectSensor('uiux.adaptive', demoFiles({ [`${UI_REL}/Destinations.kt`]: 'package com.gexetr.demo.ui\n\nsealed interface Destination {\n    data object Home : Destination\n}\n' }), false)
        }
      ],
      [
        'an unused adaptive import does not satisfy the rule; a call in the code does',
        () => {
          const importOnly = NAV3_APP.replace('        sceneStrategy = rememberListDetailSceneStrategy(),\n', '').replace(
            'import androidx.navigation3.ui.NavDisplay\n',
            'import androidx.navigation3.ui.NavDisplay\nimport androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy\n'
          )
          expectTrue(importOnly.includes('import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy'), 'the fixture did not add the unused import')
          expectTrue(!importOnly.includes('sceneStrategy ='), 'the fixture did not remove the scene strategy call')
          expectSensor('uiux.adaptive', demoFiles({ [`${UI_REL}/PlayKilnApp.kt`]: importOnly }), true, 'Detail')
        }
      ],
      // ---- uiux.text-layout ---------------------------------------------------------------
      [
        'a fixed width on a Text is a finding; a flexible one is not',
        () => {
          const fixed = `${UI_REL}/Fixed.kt`
          expectSensor(
            'uiux.text-layout',
            demoFiles({ [fixed]: 'package com.gexetr.demo.ui\nimport androidx.compose.material3.Text\nimport androidx.compose.foundation.layout.width\nimport androidx.compose.ui.Modifier\n\nfun Label() { Text(stringResource(R.string.average), modifier = Modifier.width(28.dp)) }\n' }),
            true,
            '28.dp'
          )
          expectSensor(
            'uiux.text-layout',
            demoFiles({ [fixed]: 'package com.gexetr.demo.ui\nimport androidx.compose.material3.Text\nimport androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.ui.Modifier\n\nfun Label() { Text(stringResource(R.string.average), modifier = Modifier.fillMaxWidth()) }\n' }),
            false
          )
        }
      ],
      [
        'a fixed size on a container that holds text is a finding, and one with no text is not',
        () => {
          const button = 'package com.gexetr.demo.ui\nimport androidx.compose.material3.Button\nimport androidx.compose.material3.Text\nimport androidx.compose.foundation.layout.size\nimport androidx.compose.ui.Modifier\n\nfun Pager() { Button(onClick = {}, modifier = Modifier.size(width = 96.dp, height = 48.dp)) { Text(stringResource(R.string.prev)) } }\n'
          expectSensor('uiux.text-layout', demoFiles({ [`${UI_REL}/Pager.kt`]: button }), true, '96.dp')
          const spacer = 'package com.gexetr.demo.ui\nimport androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.size\nimport androidx.compose.ui.Modifier\n\nfun Rule2() { Box(modifier = Modifier.size(width = 96.dp, height = 1.dp)) }\n'
          expectSensor('uiux.text-layout', demoFiles({ [`${UI_REL}/Rule.kt`]: spacer }), false)
        }
      ],
      [
        'a heightIn(min = 48.dp) floor is not a fixed extent, and a fixed height is',
        () => {
          const floor = 'package com.gexetr.demo.ui\nimport androidx.compose.material3.Text\nimport androidx.compose.foundation.layout.heightIn\nimport androidx.compose.ui.Modifier\n\nfun Target() { Text(stringResource(R.string.name), modifier = Modifier.heightIn(min = 48.dp)) }\n'
          expectSensor('uiux.text-layout', demoFiles({ [`${UI_REL}/Target.kt`]: floor }), false)
          const fixed = 'package com.gexetr.demo.ui\nimport androidx.compose.material3.Text\nimport androidx.compose.foundation.layout.height\nimport androidx.compose.ui.Modifier\n\nfun Squashed() { Text(stringResource(R.string.name), modifier = Modifier.height(20.dp)) }\n'
          expectSensor('uiux.text-layout', demoFiles({ [`${UI_REL}/Squashed.kt`]: fixed }), true, '20.dp')
        }
      ],
      [
        'a maxLines = 1 clamp without an overflow is a finding; with one it is not, and maxLines = 10 is neither',
        () => {
          const header = (tail) => `package com.gexetr.demo.ui\nimport androidx.compose.material3.Text\n\nfun Header() { Text(stringResource(R.string.headline), ${tail}) }\n`
          expectSensor('uiux.text-layout', demoFiles({ [`${UI_REL}/Header.kt`]: header('maxLines = 1') }), true, 'overflow')
          expectSensor('uiux.text-layout', demoFiles({ [`${UI_REL}/Header.kt`]: header('maxLines = 1, overflow = TextOverflow.Ellipsis') }), false)
          expectSensor('uiux.text-layout', demoFiles({ [`${UI_REL}/Header.kt`]: header('maxLines = 10') }), false)
        }
      ],
      // ---- uiux.motion --------------------------------------------------------------------
      [
        'two screens with nothing animated is a finding; one animation API is not',
        () => {
          const still = 'package com.gexetr.demo.ui\nimport androidx.compose.material3.Text\n\n@Composable\nfun HomeScreen() { Text(stringResource(R.string.home_title)) }\n\n@Composable\nfun DetailScreen() { Text(stringResource(R.string.detail_title)) }\n\n@Composable\nfun DemoApp() { when (route) { 0 -> HomeScreen(); else -> DetailScreen() } }\n'
          expectSensor('uiux.motion', demoFiles({ [`${UI_REL}/PlayKilnApp.kt`]: still }), true, '2 screens')
          expectSensor('uiux.motion', demoFiles(), false)
        }
      ],
      [
        'a single-screen slot is not judged for motion',
        () => {
          const one = 'package com.gexetr.demo.ui\n\n@Composable\nfun HomeScreen() { }\n'
          expectSensor('uiux.motion', demoFiles({ [`${UI_REL}/PlayKilnApp.kt`]: one }), false)
        }
      ],
      // ---- uiux.a11y ----------------------------------------------------------------------
      [
        'an Icon with no contentDescription argument is a finding; one with a label is not',
        () => {
          const bare = 'package com.gexetr.demo.ui\nimport androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.Add\nimport androidx.compose.material3.Icon\n\nfun Mark() { Icon(Icons.Filled.Add, contentDescription = label) }\nfun Bare() { Icon(Icons.Filled.Add, tint = LocalContentColor.current) }\n'
          expectSensor('uiux.a11y', demoFiles({ [`${UI_REL}/Mark.kt`]: bare }), true, 'contentDescription')
          const labelled = 'package com.gexetr.demo.ui\nimport androidx.compose.material3.Icon\n\nfun Mark() { Icon(Icons.Filled.Add, contentDescription = label) }\n'
          expectSensor('uiux.a11y', demoFiles({ [`${UI_REL}/Mark.kt`]: labelled }), false)
        }
      ],
      [
        'an icon-only clickable with no name is a finding, and a clickable whose target is below 48 dp is another',
        () => {
          const nameless = 'package com.gexetr.demo.ui\nimport androidx.compose.foundation.clickable\nimport androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.size\nimport androidx.compose.ui.Modifier\n\nfun Pager() { Box(modifier = Modifier.size(40.dp).clickable(onClick = onClick)) { Mark() } }\n'
          const findings = expectSensor('uiux.a11y', demoFiles({ [`${UI_REL}/Pager.kt`]: nameless }), true, '48 dp')
          expectTrue(findings.some((finding) => finding.includes('no accessible name')), `the nameless clickable was not reported: ${findings.join(' | ')}`)
          const named = 'package com.gexetr.demo.ui\nimport androidx.compose.foundation.clickable\nimport androidx.compose.foundation.layout.size\nimport androidx.compose.material3.Surface\nimport androidx.compose.material3.Text\nimport androidx.compose.ui.Modifier\n\nfun Pager() { Surface(modifier = Modifier.size(64.dp).clickable(onClick = onClick)) { Text(label) } }\n'
          expectSensor('uiux.a11y', demoFiles({ [`${UI_REL}/Pager.kt`]: named }), false)
        }
      ],
      // ---- uiux.insets --------------------------------------------------------------------
      [
        'no enableEdgeToEdge() is a finding; with it the rule is silent',
        () => {
          const noEdge = NAV3_APP.replace('import androidx.activity.enableEdgeToEdge\n', '').replace('    enableEdgeToEdge()\n', '')
          expectSensor('uiux.insets', demoFiles({ [`${UI_REL}/PlayKilnApp.kt`]: noEdge }), true, 'enableEdgeToEdge')
          expectSensor('uiux.insets', demoFiles(), false)
        }
      ],
      [
        'a text field with no adjustResize and no IME inset is two findings; declaring both is none',
        () => {
          const field = 'package com.gexetr.demo.ui\nimport androidx.compose.material3.OutlinedTextField\n\nfun Note(value, onValue) { OutlinedTextField(value, onValue, label = { Text(stringResource(R.string.note)) }) }\n'
          const noIme = NAV3_APP.replace('Column(modifier = Modifier.imePadding())', 'Column(modifier = Modifier)')
          const findings = expectSensor(
            'uiux.insets',
            demoFiles({
              [`${UI_REL}/Note.kt`]: field,
              [`${UI_REL}/PlayKilnApp.kt`]: noIme,
              [MANIFEST_REL]: MANIFEST_NO_SOFT_INPUT
            }),
            true,
            'windowSoftInputMode'
          )
          expectTrue(findings.some((finding) => finding.includes('imePadding')), `the missing IME inset was not reported: ${findings.join(' | ')}`)
          expectSensor('uiux.insets', demoFiles({ [`${UI_REL}/Note.kt`]: field }), false)
        }
      ],
      // ---- uiux.rule ----------------------------------------------------------------------
      [
        'a rule text that omits a check id is a finding, and the complete list is not',
        () => {
          const complete = UIUX_RULE_IDS.join('\n')
          expectEqual(ruleTextProblems(complete), [], 'the complete rule text')
          expectEqual(ruleTextProblems(complete.replace('uiux.text-layout', 'uiux.typography')), ['uiux.text-layout'], 'the renamed id')
        }
      ],
      [
        'a standards document missing a citation is a finding, and the complete list is not',
        () => {
          const complete = UIUX_STANDARD_SOURCES.map(([source]) => source).join('\n')
          expectEqual(citationProblems(complete), [], 'the complete citation list')
          const dropped = UIUX_STANDARD_SOURCES[0][0]
          expectEqual(citationProblems(complete.replace(dropped, 'https://example.invalid/')).length, 1, 'the dropped citation')
        }
      ]
    ])
  )
}

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
//
// On 2026-10-02 the owner installed a slot on a current phone and reported a second class of defect:
// "text crawls onto the notification panel, text crawls onto the camera, and it hides under the
// bottom buttons", written "as if for old phones" — and that the app was "just ordinary buttons and
// ordinary texts". The first sentence is `uiux.insets` reading the causes of a collision the platform
// enforces at `targetSdk` 35+; the second is the new `uiux.platform` reading. Both rest on the two
// research documents measured that day, `docs/research/2026-10-02-android-window-insets.md` and
// `docs/research/2026-10-02-modern-android-platform-surface.md`, and the ranked predicate tables
// there decide what is a FAIL and what is a note: a predicate that fires on correct code is worse
// than one that does not exist, so the fill-radius and cutout legs print as notes and never gate.
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
  'uiux.insets',
  'uiux.platform'
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
  ['https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture', 'Predictive back — the system animation is opted into with a one-line `android:enableOnBackInvokedCallback` declaration, and the gesture has nothing to preview unless the host owns the back stack'],
  ['https://developer.android.com/develop/ui/compose/tooling/previews', 'Compose previews — `@PreviewFontScales` and `fontScale` render the layout at more than one text size'],
  ['https://developer.android.com/jetpack/androidx/releases/navigation', 'AndroidX navigation releases — which `navigation3-*` artifact a slot depends on'],
  ['.agents/skills/android-official/jetpack-compose-adaptive/SKILL.md', 'vendored: Compose adaptive layouts'],
  ['.agents/skills/android-official/navigation-navigation-3/SKILL.md', 'vendored: Navigation 3'],
  ['.agents/skills/android-official/system-edge-to-edge/SKILL.md', 'vendored: edge-to-edge, imePadding and adjustResize'],
  // Added 2026-10-02 with the strengthened `uiux.insets` and the new `uiux.platform`, each taken from
  // a URL `docs/research/2026-10-02-android-window-insets.md` or
  // `docs/research/2026-10-02-modern-android-platform-surface.md` actually fetched. The claims say
  // what each source is load-bearing for, so the doc's treatment can be read against the sensor.
  ['https://developer.android.com/about/versions/15/behavior-changes-15', 'Android 15 behavior changes — edge-to-edge is enforced for apps targeting API 35, so drawing under the system bars is no longer a choice'],
  ['https://developer.android.com/about/versions/16/behavior-changes-16', 'Android 16 behavior changes — `windowOptOutEdgeToEdgeEnforcement` is deprecated and disabled at target 36, so there is no opt-out left to rely on'],
  ['https://developer.android.com/develop/ui/compose/system/material-insets', 'Material 3 component inset defaults — a `Scaffold` does not apply its `contentWindowInsets` itself, it reports them as `innerPadding`, and a bar resets its own by passing `WindowInsets(0, 0, 0, 0)`'],
  ['https://developer.android.com/develop/ui/compose/system/insets-ui', 'Compose inset modifiers — `windowInsetsPadding`/`safeDrawingPadding` consume what they apply, and `innerPadding.asPaddingValues()` is how a Scaffold content lambda converts its padding for a list'],
  ['https://developer.android.com/develop/ui/compose/system/cutouts', 'Display cutout in Compose — the cutout is a side inset in landscape, so `statusBars` alone does not cover it'],
  ['https://developer.android.com/develop/ui/compose/system/setup-e2e', 'Set up edge-to-edge — `enableEdgeToEdge()` and the `android:windowSoftInputMode="adjustResize"` requirement, without which the app never receives IME insets at all'],
  ['https://developer.android.com/develop/ui/compose/system/evaluate-rulers', 'Window insets rulers — `WindowInsetsRulers` and `Modifier.fitInside(WindowInsetsRulers.Ime.current)`, which the official edge-to-edge skill prefers over `imePadding()` because it consumes what it applies'],
  ['https://developer.android.com/training/data-storage/shared/photopicker', 'Photo Picker — the preferred media-picking surface, and the reason a slot should not ask for `READ_MEDIA_*`'],
  ['https://developer.android.com/develop/ui/views/launch/splash-screen', 'Splash screen — the launch surface on Android 12+, and `setKeepOnScreenCondition` is how a starting screen holds it'],
  ['https://developer.android.com/develop/ui/compose/animation/shared-elements', 'Compose shared elements — `SharedTransitionScope` and `Modifier.animateItem()` are the shipping ways to say where a list row went'],
  ['https://developer.android.com/develop/ui/compose/text/autofill', 'Compose autofill — a text field opts into autofill with `Modifier.contentType(...)`, and does nothing without it'],
  ['https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedback', 'Compose haptics — `LocalHapticFeedback` plus a `HapticFeedbackType` is the one-line platform confirmation a tap can carry']
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
 *
 * `allowBareLambda` is off by default and is the one thing here that is not a general improvement.
 * Kotlin lets a call whose parameters are all defaulted be written with no argument list at all —
 * `Scaffold { padding -> … }` is the idiom, and every real slot writes it that way — so a scanner
 * that insists on a `(` never sees the Scaffold. Turning the option on for the two inset predicates
 * that need it keeps the change from reaching the other nine rules, which have never been asked to
 * judge a paren-less call and would only gain sites whose `args` are empty.
 */
export function callSites(masked, name, { allowBareLambda = false } = {}) {
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
    if (masked[j] !== '(') {
      // `Scaffold { … }` — no argument list, one trailing lambda. The empty `args`/`argsSpan` are
      // deliberate: a rule asking about the call's own arguments must read nothing rather than read
      // the lambda as though it were the argument list.
      if (!allowBareLambda) continue
      if (masked[j] !== '{') continue
      const bareBrace = matchBrace(masked, j)
      if (bareBrace === -1) continue
      sites.push({
        name,
        index: at,
        line: lineOf(masked, at),
        args: '',
        argsSpan: masked.slice(at, at + name.length),
        span: masked.slice(at, bareBrace + 1)
      })
      continue
    }
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

// ---------------------------------------------------------------------------------------------
// Bottom chrome: a bar pinned below a list still needs the system navigation bar's inset
// ---------------------------------------------------------------------------------------------

/**
 * The ink a layout uses to make room for the system navigation bar.
 *
 * Any one of these says the bar was considered, because they are the published ways to the same
 * geometry: `navigationBarsPadding()` is the shorthand for
 * `windowInsetsPadding(WindowInsets.navigationBars)`, `safeDrawing` covers the bar together with the
 * cutout, and `contentWindowInsets` is `Scaffold`'s own slot for it. The list is deliberately short
 * and specific, and `imePadding()` is *not* in it: the keyboard's inset does not move a control clear
 * of the navigation bar, which is the distinction this leg was written from.
 */
const NAVIGATION_BAR_INSET = /\bnavigationBarsPadding\s*\(|\bwindowInsetsPadding\s*\(|\bsafeDrawing\b|WindowInsetsSides\s*\.\s*Bottom|\bsystemBars\b|\bcontentWindowInsets\b/

/**
 * The calls that make a child flexible along the axis its parent fills.
 *
 * A weighted child or a scrolling container is what pushes a *following* sibling to the parent's
 * bottom edge, which is what makes that sibling a pinned bar rather than another row in the flow.
 */
const FLEXIBLE_CHILD = /\bLazyColumn\s*\(|\bLazyRow\s*\(|\.weight\s*\(|\bverticalScroll\s*\(|\bhorizontalScroll\s*\(/

/** A child that draws bottom chrome: a `Surface`/`Column` call, or a composable that roots at one. */
const LITERAL_BAR = /^\s*(Surface|Column)\s*\(/

/**
 * The top-level children of a call's trailing lambda body, as offset ranges into `masked`.
 *
 * A child ends at a newline seen at brace/paren depth zero, which keeps a multi-line call and the
 * whole of a nested lambda inside one child instead of splitting `Surface(\n …\n)` into four. The
 * scan cannot separate the branches of an `if`/`else` into children, so a bar written inside a branch
 * is one child carrying the branch — an under-approximation, named rather than hidden.
 */
function topLevelChildren(masked, start, end) {
  const children = []
  let depth = 0
  let from = start
  for (let i = start; i < end; i += 1) {
    const ch = masked[i]
    if (ch === '{' || ch === '(' || ch === '[') depth += 1
    else if (ch === '}' || ch === ')' || ch === ']') depth = depth > 0 ? depth - 1 : 0
    else if (ch === '\n' && depth === 0) {
      if (masked.slice(from, i).trim() !== '') children.push({ start: from, end: i })
      from = i + 1
    }
  }
  if (masked.slice(from, end).trim() !== '') children.push({ start: from, end })
  return children
}

/**
 * The root call of a composable declared in this file, when that root is a `Surface` or a `Column`.
 *
 * A screen often pins its bar by calling a private composable — `RoundActions(…)` — rather than by
 * writing the `Surface` at the call site, so the sibling test cannot stop at the call's name: it has
 * to read the declaration, which is in the same file because the bottom bar a slot owns is slot code.
 */
function declaredBarRoot(masked, name) {
  const declaration = new RegExp(`\\bfun\\s+${escapeRegExp(name)}\\s*\\(`).exec(masked)
  if (!declaration) return null
  const open = masked.indexOf('(', declaration.index + declaration[0].length - 1)
  if (open === -1) return null
  const close = matchParen(masked, open)
  if (close === -1) return null
  const brace = skipSpace(masked, close + 1)
  if (masked[brace] !== '{') return null
  const root = LITERAL_BAR.exec(masked.slice(skipSpace(masked, brace + 1)))
  return root ? root[1] : null
}

/** The bar a top-level child is, or null: `Surface`, `Column`, or a same-file composable rooting at one. */
function barChild(masked, text) {
  const literal = LITERAL_BAR.exec(text)
  if (literal) return literal[1]
  const call = /^\s*([A-Z][A-Za-z0-9_]*)\s*\(/.exec(text)
  if (!call) return null
  return declaredBarRoot(masked, call[1])
}

/**
 * The bottom bar one source pins with no navigation-bar inset, or null.
 *
 * All three conditions must hold, and each one excludes a false positive the others allow:
 *   - the container is a window-filling `Column` (or one holding a weighted child), so it is a screen
 *     root rather than a card whose bar travels with its content;
 *   - a `Surface`/`Column` child *follows* a weighted or scrolling child, which is what pins it to the
 *     bottom edge — a bar written before the list is a header and is not judged;
 *   - the file names no navigation-bar inset at all, so the finding is about a screen that never
 *     considered the bar rather than one that applied the inset somewhere it does not help.
 */
function pinnedBottomBar(source) {
  const masked = source.masked
  for (const column of callSites(masked, 'Column')) {
    const open = skipSpace(masked, column.index + column.argsSpan.length)
    if (masked[open] !== '{') continue
    const close = column.index + column.span.length - 1
    if (close <= open) continue
    const children = topLevelChildren(masked, open + 1, close)
    const weighted = children.some((child) => FLEXIBLE_CHILD.test(masked.slice(child.start, child.end)))
    if (!/fillMaxSize/.test(column.argsSpan) && !weighted) continue
    let flexible = false
    for (const child of children) {
      const text = masked.slice(child.start, child.end)
      const bar = barChild(masked, text)
      if (bar) {
        if (flexible) return { line: lineOf(masked, child.start), name: bar }
        continue
      }
      if (FLEXIBLE_CHILD.test(text)) flexible = true
    }
  }
  return null
}

/** Every bottom bar pinned with no navigation-bar inset, as file-and-line findings. */
function bottomChromeFindings(sources) {
  const findings = []
  for (const source of sources) {
    // Imports are blanked for the same reason `usedAdaptiveApis` blanks them: a dependency line is
    // not a use, and `import …safeDrawing` must not stand in for a layout that applied it.
    if (NAVIGATION_BAR_INSET.test(withoutImports(source.masked))) continue
    const bar = pinnedBottomBar(source)
    if (!bar) continue
    findings.push(
      `${source.rel}:${bar.line}: a bottom-pinned ${bar.name} action bar with no navigation-bar ` +
        'inset — its controls render under the 3-button system navigation bar, where a user cannot ' +
        'read or tap them (add `navigationBarsPadding()` to the bar, or ' +
        '`windowInsetsPadding(WindowInsets.safeDrawing)` to the screen) ' +
        '(see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
    )
  }
  return findings
}

// ---------------------------------------------------------------------------------------------
// The inset vocabulary, and the causes the owner photographed on a current phone
// ---------------------------------------------------------------------------------------------

/**
 * Every modifier call that applies — and therefore consumes — a window inset.
 *
 * One list, because three predicates ask three different questions of the same vocabulary, and a fix
 * honoured in one place and not the others is how a sensor starts disagreeing with itself: the
 * `Scaffold` predicate asks whether the lambda used what it was handed, the screen-root note asks
 * whether the root applies *any* inset at all, and the cutout note asks whether anything in the module
 * is cutout-aware. A bare `WindowInsets(` is deliberately absent: constructing one is how a
 * component's own handling is switched *off*, which is the opposite of applying one.
 */
const INSET_PADDING_TOKENS = Object.freeze([
  'windowInsetsPadding', 'safeDrawingPadding', 'safeContentPadding', 'safeGesturesPadding',
  'systemBarsPadding', 'statusBarsPadding', 'navigationBarsPadding', 'displayCutoutPadding',
  'imePadding', 'captionBarPadding', 'waterfallPadding', 'systemGesturesPadding',
  'mandatorySystemGesturesPadding', 'fitInside', 'fitOutside',
  'windowInsetsTopHeight', 'windowInsetsBottomHeight', 'windowInsetsStartWidth', 'windowInsetsEndWidth'
])

/**
 * The tokens that cover the display cutout, which `systemBars` and `statusBars` do not.
 *
 * MEASURED (`docs/research/2026-10-02-android-window-insets.md` §2): `displayCutout` is a distinct
 * inset type, absent from both `systemBars` and `safeGestures`. In portrait a top cutout usually sits
 * inside the status bar, so a screen padded with `statusBarsPadding()` looks correct; in landscape the
 * cutout moves to a vertical edge where `statusBars` contributes nothing, which is the owner's "text
 * crawls onto the camera".
 */
const CUTOUT_AWARE_TOKENS = Object.freeze([
  'displayCutoutPadding', 'safeDrawingPadding', 'safeContentPadding',
  'WindowInsets.displayCutout', 'WindowInsets.safeDrawing', 'WindowInsets.safeContent',
  'WindowInsetsRulers.SafeDrawing', 'WindowInsetsRulers.SafeContent',
  'WindowInsets.systemBarsForVisualComponents'
])

/**
 * The M3 bars whose own default `windowInsets` unions the display cutout in.
 *
 * MEASURED (AOSP `SystemBarsDefaultInsets.android.kt`, reached from `…insets.md` §4):
 * `systemBarsForVisualComponents = systemBars.union(displayCutout)`, and it is what
 * `TopAppBarDefaults.windowInsets` and `NavigationBarDefaults.windowInsets` are built from — so a bar
 * whose `windowInsets` is explicitly zeroed gives up the cutout protection it had by default. Each
 * centred/medium/large variant is listed separately because `callSites` rejects a match preceded by an
 * identifier character, which is exactly what keeps `CenterAlignedTopAppBar(` from reading as a
 * `TopAppBar(` site.
 */
const M3_INSET_BAR_NAMES = Object.freeze([
  'TopAppBar', 'CenterAlignedTopAppBar', 'MediumTopAppBar', 'LargeTopAppBar',
  'NavigationBar', 'BottomAppBar'
])

/**
 * `WindowInsets(0, 0, 0, 0)` — the documented way to switch a component's own inset handling off.
 *
 * MEASURED (`…insets.md` §3, *Material 3 insets*). Read from a call's own arguments, never from its
 * trailing lambda, so an unrelated `WindowInsets(0, 0, 0, 0)` inside the content slot is not a finding
 * about the bar that contains it.
 */
const ZEROED_INSETS = /WindowInsets\s*\(\s*0\s*,\s*0\s*,\s*0\s*,\s*0\s*\)/

/**
 * The edge-to-edge opt-in, in both forms the platform documents.
 *
 * `enableEdgeToEdge()` is the modern one, and the current check knew only it — so a slot that disables
 * decor fitting by hand was reported as not edge-to-edge at all, which is a finding about the wrong
 * thing. `WindowCompat.setDecorFitsSystemWindows(window, false)` is the identical act one layer down,
 * and the AOSP Compose comment reached from `…insets.md` §1 names it as the pairing for `adjustResize`.
 *
 * The two are kept as separate patterns because the measured summary has to name *which* one was
 * found: a row that says `enableEdgeToEdge()` present when the slot never called it is the same defect
 * as the finding it replaced, one layer up.
 */
const EDGE_TO_EDGE_CALL = /\benableEdgeToEdge\s*\(/
const DECOR_FITS_OPT_OUT = /WindowCompat\s*\.\s*setDecorFitsSystemWindows\s*\([^)]*\bfalse\b/
const EDGE_TO_EDGE_OPT_IN = new RegExp(`${EDGE_TO_EDGE_CALL.source}|${DECOR_FITS_OPT_OUT.source}`)

/** Which opt-in the sources actually carry, or null. Read by the summary, never asserted. */
function edgeToEdgeForm(sources) {
  if (sources.some((source) => EDGE_TO_EDGE_CALL.test(source.masked))) return 'enableEdgeToEdge()'
  if (sources.some((source) => DECOR_FITS_OPT_OUT.test(source.masked))) return 'WindowCompat.setDecorFitsSystemWindows(…, false)'
  return null
}

/** A screen entry point: a composable named for a destination rather than for a widget. */
const SCREEN_ENTRY = /(?:Screen|Route|Page|Pane)$/

/**
 * A call's trailing lambda, split from the call's own arguments.
 *
 * `callSites` returns a whole call *including* its trailing lambda, which is right for every other rule
 * in this module — `Button(…) { Text(…) }` is one subject. It is exactly wrong for the `Scaffold`
 * predicate, which must read the lambda's *parameter* and then ask whether the lambda uses it, so the
 * two spans are separated here instead of by a second regex.
 *
 * The `{` only counts as a trailing lambda when it follows the call's closing `)`: a lambda passed as a
 * named argument (`content = { … }`) is not the content slot, and reading it as one would read the
 * wrong parameter list.
 */
function trailingLambda(span) {
  const braceAt = span.indexOf('{')
  const open = span.indexOf('(')
  // The call's own argument list, when it has one, comes *before* the trailing lambda's brace. That
  // ordering is the whole test: `Scaffold { (innerPadding, fab) -> … }` has a `(` too — the lambda's
  // parameter list — and splitting on the first `(` alone reads those parentheses as the call's
  // arguments and then finds an arrow where it expected a brace. A lambda passed as a named argument
  // (`content = { … }`) still returns `null`, because its own brace precedes no top-level brace after
  // the closing paren, which is the case this must keep refusing.
  if (open !== -1 && (braceAt === -1 || open < braceAt)) {
    const close = matchParen(span, open)
    if (close === -1) return null
    const after = skipSpace(span, close + 1)
    if (span[after] !== '{') return null
    const brace = matchBrace(span, after)
    if (brace === -1) return null
    return { args: span.slice(open + 1, close), body: span.slice(after + 1, brace) }
  }
  // A call written with no argument list at all — `Scaffold { padding -> … }`, which is how the idiom
  // is actually written. There is no `args` to split from the lambda, and returning null here would
  // silently skip exactly the call the predicate exists to read.
  if (braceAt === -1) return null
  const bareBrace = matchBrace(span, braceAt)
  if (bareBrace === -1) return null
  return { args: '', body: span.slice(braceAt + 1, bareBrace) }
}

/**
 * The name a lambda binds its first parameter to, and the body scoped after that binding.
 *
 * A `Scaffold` content lambda is `(PaddingValues) -> Unit`, so the binding is written three ways and a
 * rule that understood one of them would report the other two as "not consumed" — a finding about the
 * wrong thing. All three are read here: `{ padding -> … }`, destructured as
 * `{ (innerPadding, fabPosition) -> … }` (the padding is the *first* name), and omitted, in which case
 * Kotlin names it `it`.
 *
 * `scoped` is the body *after* the arrow, and that separation is the whole reason this returns an object
 * rather than a name: the parameter declaration is itself part of the lambda's text, so testing
 * "does the body mention `innerPadding`" against the whole body is satisfied by the binding alone and
 * the predicate could never fire.
 *
 * The under-approximation is deliberate and directional: a body whose first `->` belongs to a nested
 * `when` branch or a nested lambda is unreadable, and an unreadable lambda returns `null` so the call is
 * skipped rather than guessed at.
 */
function lambdaBinding(body) {
  const arrow = body.indexOf('->')
  if (arrow === -1) return { name: 'it', scoped: body }
  const params = body.slice(0, arrow)
  if (/[{}]/.test(params) || params.length > 120) return null
  const names = [...params.matchAll(/[A-Za-z_][A-Za-z0-9_]*/g)].map((match) => match[0])
  if (names.length === 0) return null
  return { name: names[0], scoped: body.slice(arrow + 2) }
}

/**
 * Every `@Composable` function body in one source, with the index its body starts at.
 *
 * The body is read with `matchBrace` rather than a regex so a nested lambda or a `when` block inside a
 * screen cannot truncate it: both notes below have to look at the *whole* function to decide whether a
 * `Scaffold`, an M3 bar or an inset modifier exists anywhere in it. `start` is kept so a finding can
 * still print the line of the call it names, which is the whole point of a `file:line` finding.
 */
function composableBodies(masked) {
  const bodies = []
  for (const match of masked.matchAll(/@Composable\b/g)) {
    const funAt = masked.indexOf('fun ', match.index)
    if (funAt === -1) continue
    const name = /^fun\s+([A-Za-z_][A-Za-z0-9_]*)/.exec(masked.slice(funAt))
    if (!name) continue
    const brace = masked.indexOf('{', funAt)
    if (brace === -1) continue
    const end = matchBrace(masked, brace)
    if (end === -1) continue
    bodies.push({ name: name[1], line: lineOf(masked, match.index), start: brace + 1, body: masked.slice(brace + 1, end) })
  }
  return bodies
}

/**
 * A `Scaffold` that hands out `innerPadding` and never receives it back.
 *
 * MEASURED (`…insets.md` §4): *"Scaffold does not apply the insets to content; this responsibility is
 * yours."* So a `Scaffold` whose content lambda drops its `PaddingValues` is byte-for-byte as exposed
 * as no `Scaffold` at all — the first line lands under the notification panel and the last row under
 * the gesture bar — which is the exact mechanism of the owner's report, and the reason this is a FAIL
 * rather than a note: it is a binding-unused test, not a guess about pixels.
 *
 * The parameter is looked for **anywhere in the lambda body**, not only in a `padding(…)` call,
 * because `padding(horizontal = innerPadding.calculateTopPadding())` and a value handed on to a child
 * component are both uses. That is the direction this predicate has to err in: it is a hard FAIL, so a
 * missed defect is acceptable and a finding about a screen that did consume the padding is not.
 */
function scaffoldPaddingFindings(sources) {
  const findings = []
  for (const source of sources) {
    for (const site of callSites(source.masked, 'Scaffold', { allowBareLambda: true })) {
      const lambda = trailingLambda(site.span)
      if (!lambda) continue
      const binding = lambdaBinding(lambda.body)
      if (!binding) continue
      if (binding.name === '_') {
        findings.push(
          `${source.rel}:${site.line}: this Scaffold's content lambda discards its PaddingValues ` +
            'parameter, and a Scaffold does not apply its insets to content — it hands them to the lambda, ' +
            'so the screen is as exposed as if there were no Scaffold at all (see docs/ui-ux-standards.md, ' +
            '"Edge-to-edge and the IME")'
        )
        continue
      }
      if (new RegExp(`\\b${escapeRegExp(binding.name)}\\b`).test(binding.scoped)) continue
      findings.push(
        `${source.rel}:${site.line}: this Scaffold's content lambda never uses the innerPadding it is ` +
          `handed (${binding.name}), and a Scaffold does not apply its insets to content — it hands them to ` +
          'the lambda, so the first line renders under the status bar and the last under the navigation ' +
          'bar (see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
      )
    }
  }
  return findings
}

/**
 * An M3 bar whose own inset handling was explicitly switched off.
 *
 * MEASURED (`…insets.md` §3, §4): passing `WindowInsets(0, 0, 0, 0)` is the documented way to disable a
 * component's default handling, and the default is the one thing that unions the display cutout in. A
 * `Scaffold`'s `contentWindowInsets` is deliberately **not** read here: zeroing the inner Scaffold is
 * the documented shape when a host Scaffold already applied the insets, so firing on it would be the
 * false positive this module's header warns about.
 */
function zeroedBarInsetFindings(sources) {
  const findings = []
  for (const source of sources) {
    for (const name of M3_INSET_BAR_NAMES) {
      for (const site of callSites(source.masked, name, { allowBareLambda: true })) {
        if (!ZEROED_INSETS.test(site.argsSpan)) continue
        findings.push(
          `${source.rel}:${site.line}: ${name}(windowInsets = WindowInsets(0, 0, 0, 0)) switches off the ` +
            "component's own inset handling — the default unions the display cutout in, so the bar's " +
            'content can sit under the status bar, the camera cutout or the navigation bar ' +
            '(see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
        )
      }
    }
  }
  return findings
}

/** Whether anything in the module applies an inset at all, ignoring import lines. */
const moduleAppliesAnInset = (sources) =>
  sources.some((source) => INSET_PADDING_TOKENS.some((token) => source.masked.includes(token)))

/** Whether anything in the module is cutout-aware. */
const moduleIsCutoutAware = (sources) =>
  sources.some((source) => CUTOUT_AWARE_TOKENS.some((token) => source.masked.includes(token)))

/**
 * Whether the module contains a component whose own default insets union the cutout in.
 *
 * This is the ancestor half of the two notes below, and it is read with `callSites` rather than with a
 * `Scaffold\s*\(` regex for the reason the `Scaffold` predicate exists at all: the idiom is
 * `Scaffold { padding -> … }`, written with **no** argument list, so a paren-only test sees no
 * Scaffold on the most common shape in the codebase and then reports a note on a screen that is
 * perfectly correct. A note that fires on correct code is worse than no note, because it teaches the
 * reader to skip the row.
 */
const moduleDeclaresInsetHost = (sources) =>
  sources.some((source) =>
    ['Scaffold', ...M3_INSET_BAR_NAMES].some((name) => callSites(source.masked, name, { allowBareLambda: true }).length > 0)
  )

/**
 * A screen whose root fills the window and applies no inset, in a module that applies none anywhere.
 *
 * Ranked 6 of 9 by `…insets.md` §7.4, and rated **medium–high** false-positive risk, because the hard
 * half is the ancestor: the same screen is correct under an app-level `Scaffold` and wrong without one,
 * and that is a whole-subtree fact a file-local scan cannot see. So it is a **note inside a PASS** and
 * never a FAIL, and it is only printed when the module applies no inset *anywhere* and declares no
 * `Scaffold` or M3 bar — the one shape in which "no ancestor could have supplied one" is provable from
 * source.
 */
function screenRootNotes(sources) {
  if (moduleAppliesAnInset(sources)) return []
  if (moduleDeclaresInsetHost(sources)) return []
  const notes = []
  for (const source of sources) {
    for (const body of composableBodies(source.masked)) {
      if (!SCREEN_ENTRY.test(body.name)) continue
      const fill = /fillMaxSize\s*\(/.exec(body.body)
      if (!fill) continue
      notes.push(
        `${source.rel}:${lineOf(source.masked, body.start + fill.index)}: ${body.name} fills the window ` +
          'and applies no inset modifier, and nothing else in the module applies one or declares a ' +
          'Scaffold — at targetSdk 36 the top offset is disabled, so its first line can render under the ' +
          'notification panel (a note, not a failure: an ancestor this scan cannot see may already have ' +
          'applied the insets) (see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
      )
    }
  }
  return notes
}

/**
 * A screen that pads with a system-bars inset and nothing cutout-aware.
 *
 * Ranked 7 of 9, rated **medium**: `displayCutout` is not in `systemBars`, but the collision only
 * happens in landscape on a device that has a cutout, and a device without one never shows it — so a
 * hard FAIL would need the "no Scaffold or bar anywhere above it" guard, and a note is what the
 * research defends. The guard used here is the provable half of it: no cutout-aware token anywhere in
 * the module, and no `Scaffold` or M3 bar anywhere in it either, so nothing above the screen can have
 * unioned the cutout in.
 *
 * `navigationBarsPadding()` is in the bars-only set as well, even though `…insets.md` §7.2's own line
 * names only `statusBars`/`systemBars`: the same document's footgun list (§3) records that
 * `navigationBarsPadding()` alone "misses the top entirely and misses the side cutout", which is the
 * same defect with the other bar, and a note that fires on it costs a reader nothing.
 */
function cutoutOnlyNotes(sources) {
  if (moduleIsCutoutAware(sources)) return []
  if (moduleDeclaresInsetHost(sources)) return []
  const notes = []
  for (const source of sources) {
    for (const body of composableBodies(source.masked)) {
      if (!SCREEN_ENTRY.test(body.name)) continue
      const barsOnly =
        /statusBarsPadding\s*\(|systemBarsPadding\s*\(|navigationBarsPadding\s*\(|windowInsetsPadding\s*\(\s*WindowInsets\.(?:statusBars|systemBars|navigationBars)\b/.exec(
          body.body
        )
      if (!barsOnly) continue
      notes.push(
        `${source.rel}:${lineOf(source.masked, body.start + barsOnly.index)}: ${body.name} pads with a ` +
          'system-bars inset only, and neither the screen nor the module is cutout-aware — in landscape ' +
          'the cutout moves to a vertical edge, where systemBars contributes nothing and the first ' +
          'character of a title can sit behind the camera (a note, not a failure: the collision is ' +
          'device- and orientation-dependent) (see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
      )
    }
  }
  return notes
}

/**
 * A scrollable whose parent carries an inset instead of the list carrying it in `contentPadding`.
 *
 * Ranked 9 of 9 and rated **medium–high**: the official skill is explicit that inset padding belongs in
 * the scrollable's `contentPadding` and *"DO NOT"* on the list's parent container, because the parent
 * version clips the content and stops it scrolling behind the bars — but whether a given list actually
 * occludes anything is a runtime property of item count, row height and font scale. Hence a note, and it
 * fires only on the provable shape: a full-bleed lazy list, the inset on the same composable, and no
 * `contentPadding` or `consumeWindowInsets` anywhere to absorb it.
 */
const LAZY_LIST_NAMES = Object.freeze([
  'LazyColumn', 'LazyRow', 'LazyVerticalGrid', 'LazyHorizontalGrid',
  'LazyVerticalStaggeredGrid', 'LazyHorizontalStaggeredGrid'
])

function scrollParentPaddingNotes(sources) {
  const notes = []
  for (const source of sources) {
    for (const body of composableBodies(source.masked)) {
      const parentInset = /(?:safeDrawingPadding|safeContentPadding|safeGesturesPadding|systemBarsPadding|statusBarsPadding|navigationBarsPadding|imePadding)\s*\(|windowInsetsPadding\s*\(/.exec(
        body.body
      )
      if (!parentInset) continue
      for (const name of LAZY_LIST_NAMES) {
        for (const site of callSites(body.body, name)) {
          if (!/fillMaxSize\s*\(|fillMaxWidth\s*\(/.test(site.argsSpan)) continue
          if (/contentPadding\s*=/.test(site.argsSpan)) continue
          if (/consumeWindowInsets\s*\(/.test(site.span)) continue
          notes.push(
            `${source.rel}:${lineOf(source.masked, body.start + site.index)}: this ${name} is full-bleed ` +
              'with no contentPadding, and the inset is applied on this composable instead — the list is ' +
              'clipped rather than scrolling behind the bars, so the inset belongs in its contentPadding ' +
              '(a note, not a failure: whether it occludes anything depends on the item count and the ' +
              'font scale) (see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
          )
        }
      }
    }
  }
  return notes
}

/**
 * `imePadding()` beside an IME-carrying `contentWindowInsets` in one file.
 *
 * Ranked 8 of 9 and rated **medium**: it is a MEASURED WRONG pattern from the official skill, because
 * `safeDrawing` already contains `ime`, so the keyboard is padded twice. Scope is the file rather than
 * the Scaffold, and the modifier may belong to a different Scaffold, so it is a note.
 */
function doubleImeNotes(sources) {
  const notes = []
  for (const source of sources) {
    const carrying = /contentWindowInsets\s*=\s*WindowInsets\.(?:safeDrawing|safeContent|safeGestures|ime)\b/.exec(
      source.masked
    )
    if (!carrying) continue
    const ime = /imePadding\s*\(/.exec(source.masked)
    if (!ime) continue
    notes.push(
      `${source.rel}:${lineOf(source.masked, ime.index)}: contentWindowInsets already carries the IME ` +
        '(safeDrawing/safeContent/safeGestures contains it) and imePadding() is applied in the same file, ' +
        'so the keyboard is padded twice (a note, not a failure: the modifier may belong to a different ' +
        'Scaffold than the one declaring the insets) (see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
    )
  }
  return notes
}

/** Every inset note for one slot: the causes a static scan can see but cannot call a defect. */
export function insetNotes(sources) {
  return [...screenRootNotes(sources), ...cutoutOnlyNotes(sources), ...doubleImeNotes(sources), ...scrollParentPaddingNotes(sources)]
}

/**
 * Edge-to-edge and the IME.
 *
 * `enableEdgeToEdge()` is mandatory at `targetSdk` 36, and the keyboard is the inset that is *not* a
 * system bar: `Scaffold`'s own `contentWindowInsets` does not carry it, the platform only resizes the
 * window for it when the activity says `adjustResize`, and a slot that declares neither can leave the
 * field the user is typing into underneath the keyboard. The rule is only read when the slot shows a
 * text field, because a slot with no input has no IME to place.
 *
 * The opt-in is satisfied by either form the platform documents, and the two failures the owner
 * photographed are read here as causes rather than symptoms: an unused `Scaffold.innerPadding` (the
 * content slot is the one thing a Scaffold does not inset for you) and a bar whose own insets were
 * zeroed. What this cannot see is stated rather than implied — whether a `Text` actually touches the
 * camera is a runtime fact of the device, the orientation and the content, so a green row here means
 * the cause is absent, never that the pixels are right.
 */
export function insetFindings(slotDir, sources) {
  const findings = [
    ...bottomChromeFindings(sources),
    ...scaffoldPaddingFindings(sources),
    ...zeroedBarInsetFindings(sources)
  ]
  if (!sources.some((source) => EDGE_TO_EDGE_OPT_IN.test(source.masked))) {
    findings.push(
      'no enableEdgeToEdge() and no WindowCompat.setDecorFitsSystemWindows(…, false) in any ' +
        'app/src/main source: edge-to-edge is mandatory at targetSdk 36, and an app that does not opt ' +
        'in gets a scrimmed letterbox instead of the drawn-to-the-edges layout the slot was designed ' +
        'for (see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
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
  const imeHandling =
    /imePadding\s*\(|fitInside\s*\(|safeDrawingPadding\s*\(|safeContentPadding\s*\(|safeGesturesPadding\s*\(|WindowInsets\.(?:ime|safeDrawing|safeContent|safeGestures)\b|WindowInsetsRulers\.(?:Ime|SafeDrawing|SafeContent|SafeGestures)\b|ModalBottomSheet\s*\(/
  if (!imeHandling.test(sources.map((source) => source.masked).join('\n'))) {
    findings.push(
      `the slot shows a text field (${where}) but no imePadding()/fitInside(...), no safe-drawing inset ` +
        '(safeDrawing contains the IME where systemBars does not) and no ModalBottomSheet that would ' +
        'carry it: the IME inset is not applied by the Scaffold default, so nothing in the layout reacts ' +
        'to the keyboard (see docs/ui-ux-standards.md, "Edge-to-edge and the IME")'
    )
  }
  return findings
}

// ---------------------------------------------------------------------------------------------
// `uiux.platform` — the modern Android surface, read the same way
// ---------------------------------------------------------------------------------------------

/**
 * The build files a dependency can be declared in.
 *
 * A closed list rather than "every `*.gradle*` on disk", because `walkSlotFiles` would also reach the
 * generated copies Gradle leaves under `app/build/`, and a stale generated build file is not a
 * declaration the slot made. The version catalog is first because it is where this factory's slots put
 * versions; the rest cover a direct dependency and a settings-level one.
 */
const BUILD_FILE_PATHS = Object.freeze([
  'gradle/libs.versions.toml',
  'build.gradle.kts',
  'build.gradle',
  'settings.gradle.kts',
  'settings.gradle',
  'app/build.gradle.kts',
  'app/build.gradle',
  'baselineprofile/build.gradle.kts'
])

/**
 * A Material 3 Expressive / `MotionScheme` artifact on the alpha track.
 *
 * MEASURED (`docs/research/2026-10-02-modern-android-platform-surface.md` §6): the pinned Compose BOM
 * `2026.09.00` carries stable `material3 1.4.0`, and every Expressive component and the `MotionScheme`
 * physics live on `1.5.0-alpha*` behind `@ExperimentalMaterial3ExpressiveApi`. So an
 * `androidx.compose.material3:material3:1.5.0-alpha28` line is not a bug in the app — it is an alpha
 * dependency on the library every screen is built from, which is why the research says to gate it as
 * *forbidden* rather than required: taking it silently breaks the pin every other gate reads, and
 * forbidding it costs a slot nothing it can already have.
 */
const EXPRESSIVE_ALPHA_DEP = /androidx\.compose\.material3:[A-Za-z0-9.-]+:[0-9][0-9.]*-?[A-Za-z0-9.-]*alpha[0-9.]*/i

/**
 * The `androidx.compose.material3:*` declarations in one build file, with the version they resolve to.
 *
 * Two shapes, because this factory's slots use both: a direct coordinate that carries its own version
 * (`implementation("androidx.compose.material3:material3:1.5.0-alpha28")`), and a version catalog whose
 * `[libraries]` entry names the module while its `[versions]` table holds the version. Reading only the
 * first would have reported a catalogued alpha as clean — the line that *is* a dependency does not
 * contain the word `alpha`, and the line that does is not a dependency.
 */
function material3Declarations(text) {
  const alphaAliases = new Set()
  for (const match of text.matchAll(/^\s*([A-Za-z0-9_.-]+)\s*=\s*"([^"]*alpha[^"]*)"/gim)) alphaAliases.add(match[1])
  const found = []
  for (const line of text.split(/\r?\n/)) {
    if (!/androidx\.compose\.material3:/.test(line)) continue
    const ref = /version\.ref\s*=\s*"([^"]+)"/.exec(line)
    if (!EXPRESSIVE_ALPHA_DEP.test(line) && !(ref && alphaAliases.has(ref[1]))) continue
    found.push(line.trim())
  }
  return found
}

/**
 * A Material 3 Expressive artifact the pinned BOM does not carry.
 *
 * `slotDir` is passed because this is the one predicate in the module that reads a Gradle file rather
 * than Kotlin: a dependency is declared where the toolchain is, and a scan of `app/src/main` cannot see
 * it. The line is searched for inside the file's own text rather than with a line number from a
 * separate walk, so the finding names the declaration the builder has to change.
 */
export function expressiveAlphaSites(slotDir) {
  const sites = []
  for (const rel of BUILD_FILE_PATHS) {
    const file = path.join(slotDir, ...rel.split('/'))
    if (!existsSync(file)) continue
    let text = null
    try {
      text = decodeSource(readFileSync(file))
    } catch {
      continue
    }
    if (text === null) continue
    for (const declaration of material3Declarations(text)) sites.push({ rel, declaration })
  }
  return sites
}

/**
 * The product-conditional surfaces `uiux.platform` reports and never gates.
 *
 * Each entry carries the token that would mean "this slot does this", the surface it names, and the
 * reason the research rates it presence-only. They are printed in the PASS detail so the builder sees
 * what the platform offers and an operator can read past it: a lawn-round log has nothing to share, a
 * slot with no identity field has nothing to autofill, and a rule that failed on either would be turned
 * off within a week — which is the failure mode this module's header calls worse than a missing sensor.
 */
const ENCOURAGED_SURFACES = Object.freeze([
  {
    token: /installSplashScreen\s*\(/,
    surface: 'the splash-screen API (core-splashscreen + installSplashScreen())',
    why: 'it is a perceived-start-up improvement, and the research rates splash continuity as needing a device and a stopwatch'
  },
  {
    token: /ACTION_PROCESS_TEXT|ACTION_SEND\b/,
    surface: 'a share or text-processing target (ACTION_SEND / ACTION_PROCESS_TEXT)',
    why: 'it is only meaningful when the slot produces something another app can consume'
  },
  {
    token: /PickVisualMedia|PickMultipleVisualMedia/,
    surface: 'the Photo Picker (PickVisualMedia)',
    why: 'it is the recommended path only for a slot that reads the user media, and a slot that never does needs neither the picker nor the permission'
  },
  {
    token: /LocalHapticFeedback|performHapticFeedback\s*\(/,
    surface: 'haptic feedback (LocalHapticFeedback / performHapticFeedback)',
    why: 'presence is a token while the semantic HapticFeedbackType is the actual point, and a scan cannot judge it'
  },
  {
    token: /animateItem\s*\(|SharedTransitionLayout\s*\(|sharedElement\s*\(|sharedBounds\s*\(/,
    surface: 'list-item and shared-element motion (animateItem / SharedTransitionLayout)',
    why: 'it is load-bearing on a list whose order changes and decorative on a static one, which depends on the data'
  },
  {
    token: /SelectionContainer\s*\(/,
    surface: 'text selection (SelectionContainer)',
    why: 'it helps only where the app output is text a user would want to copy'
  },
  {
    token: /\bcontentType\s*=/,
    surface: 'autofill content types (Modifier.contentType(...))',
    why: 'it applies only where the slot collects identity fields, and the wrong hint is worse than none'
  }
])

/**
 * The modern-platform surface: predictive back, and the toolchain the slot is allowed to stand on.
 *
 * Two FAILs and a list of notes, exactly the split `…platform-surface.md`'s ranked gate candidates
 * draw. Both FAILs are one-line, universal and provable by token: a manifest that never opts into
 * predictive back, and a build file that reaches for an alpha Expressive artifact the pinned BOM does
 * not carry. Everything else the document rates DETECTABLE it also rates product-conditional, and a
 * conditional requirement is a note here rather than a gate.
 */
export function platformFindings(slotDir, sources) {
  const findings = []
  const manifest = readManifestState(slotDir)
  if (!manifest.text) {
    findings.push(
      `no ${displayPath(slotDir, path.join(slotDir, MANIFEST_REL))} to read, so the predictive-back ` +
        'opt-in could not be decided'
    )
  } else if (!/enableOnBackInvokedCallback/.test(manifest.text)) {
    findings.push(
      `${displayPath(slotDir, manifest.file)}: no android:enableOnBackInvokedCallback="true" on the ` +
        '<application> or the <activity>, so the predictive-back animation has nothing to preview and ' +
        'back is an instant swap on a current device (see docs/ui-ux-standards.md, ' +
        '"The modern Android platform surface")'
    )
  }
  for (const site of expressiveAlphaSites(slotDir)) {
    findings.push(
      `${site.rel}: ${site.declaration} — a Material 3 Expressive / MotionScheme artifact on the alpha ` +
        'track, which the pinned Compose BOM does not carry: stable material3 1.4.0 is what the BOM maps ' +
        'to, so taking the alpha breaks the pin every other gate reads (see docs/ui-ux-standards.md, ' +
        '"The modern Android platform surface")'
    )
  }
  return findings
}

/**
 * What the platform offers and the slot does not use, as notes.
 *
 * The research's encourage-only list, read as absence rather than as a requirement. A note is printed
 * for each surface the slot does not reach for, with the reason it cannot be a gate; nothing here can
 * turn a PASS into a FAIL, which is the whole point of keeping the two lists apart.
 */
export function platformNotes(slotDir, sources) {
  const module = sources.map((source) => source.masked).join('\n')
  const notes = []
  for (const entry of ENCOURAGED_SURFACES) {
    if (entry.token.test(module)) continue
    notes.push(
      `${entry.surface} is absent from app/src/main — worth considering, and a note rather than a ` +
        `finding because ${entry.why} (see docs/ui-ux-standards.md, "The modern Android platform surface")`
    )
  }
  const manifest = readManifestState(slotDir)
  if (manifest.text && /READ_MEDIA_IMAGES|READ_MEDIA_VIDEO/.test(manifest.text) && !/PickVisualMedia/.test(module)) {
    notes.push(
      `${displayPath(slotDir, manifest.file)}: the manifest requests READ_MEDIA_IMAGES/READ_MEDIA_VIDEO ` +
        'and no PickVisualMedia call exists — the Photo Picker needs no storage permission, and Play ' +
        'policy prefers it (see docs/ui-ux-standards.md, "The modern Android platform surface")'
    )
  }
  return notes
}

/** What `uiux.platform` measured, as one line, so a PASS row is evidence rather than an assertion. */
export function platformSummary(slotDir, sources) {
  const manifest = readManifestState(slotDir)
  const back = manifest.text ? /enableOnBackInvokedCallback/.test(manifest.text) : false
  const alpha = expressiveAlphaSites(slotDir).length
  const module = sources.map((source) => source.masked).join('\n')
  const present = ENCOURAGED_SURFACES.filter((entry) => entry.token.test(module)).length
  return `predictive back ${back ? 'declared' : 'not declared'}; ${alpha} M3 Expressive alpha artifact(s); ${present} of ${ENCOURAGED_SURFACES.length} product-conditional surface(s) in use`
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
    ...insetFindings(slotDir, sources),
    ...insetNotes(sources),
    ...platformFindings(slotDir, sources),
    ...platformNotes(slotDir, sources)
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
  'uiux.insets': (slotDir) => insetFindings(slotDir, readKotlin(slotDir)),
  'uiux.platform': (slotDir) => platformFindings(slotDir, readKotlin(slotDir))
})

/**
 * The note halves of the two checks that print them, kept beside `SENSORS` and deliberately separate.
 *
 * A note is not a finding: `baselineVerdict` gates on the array `SENSORS` returns, so a note that
 * leaked into that array would turn a PASS into a FAIL — which is the one thing the research forbids
 * for a predicate that fires on correct code. The two tables are read by different callers for exactly
 * that reason, and the fixtures drive each one separately.
 */
const NOTE_SENSORS = Object.freeze({
  'uiux.insets': (slotDir) => insetNotes(readKotlin(slotDir)),
  'uiux.platform': (slotDir) => platformNotes(slotDir, readKotlin(slotDir))
})

/** The summaries a passing check prints. Each is built from what was measured, never asserted. */
/**
 * What the insets half measured, in one line, with nothing asserted that was not counted.
 *
 * The previous text claimed `enableEdgeToEdge()` present unconditionally, which is exactly the defect
 * this change is about — a row that says a thing is there without having looked. Every clause here is a
 * count or a name the pass above actually observed, and the count of `Scaffold`s and bars is included
 * because a green row on a slot that has neither has measured the easy half and should say so.
 */
function insetSummary(slotDir, sources, fields, manifest) {
  const form = edgeToEdgeForm(sources)
  const scaffolds = sources.reduce((total, source) => total + callSites(source.masked, 'Scaffold', { allowBareLambda: true }).length, 0)
  const bars = sources.reduce(
    (total, source) => total + M3_INSET_BAR_NAMES.reduce((n, name) => n + callSites(source.masked, name).length, 0),
    0
  )
  const optIn = form ? `${form} present` : 'no edge-to-edge opt-in in any source'
  const chrome = `${scaffolds} Scaffold(s) and ${bars} inset-handling bar(s) scanned`
  // The note count is in the summary because a note that never reaches the report is a note nobody
  // reads: these are the risks the pass above refused to call defects, and the count is how a reader
  // knows it looked.
  const noteCount = insetNotes(sources).length
  const pitfalls = noteCount === 0 ? 'no inset pitfall noted' : `${noteCount} inset pitfall note(s)`
  if (fields === 0) return `${optIn}; ${chrome}; ${pitfalls}; the slot shows no text field, so there is no IME placement to judge`
  const softInput = manifest.text && /windowSoftInputMode/.test(manifest.text) ? 'a soft-input declaration' : 'no soft-input declaration'
  return `${optIn}; ${chrome}; ${pitfalls}; ${fields} text field(s) with ${softInput} and IME inset handling`
}

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
    'uiux.insets': insetSummary(slotDir, sources, fields, manifest),
    'uiux.platform': platformSummary(slotDir, sources)
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
      ...insetFindings(scaffold, sources),
      // `uiux.platform` is read here too, and for the same reason: the scaffold is what every new slot
      // is copied from, so a template that never opts into predictive back would ship the omission to
      // every slot after it, with the gate reporting it one slot later instead of here.
      ...platformFindings(scaffold, sources)
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
  '    <application',
  '        android:label="@string/app_name"',
  '        android:enableOnBackInvokedCallback="true">',
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

/** The same manifest with the predictive-back opt-in removed, which is `uiux.platform`'s first FAIL. */
const MANIFEST_NO_BACK_CALLBACK = MANIFEST_OK.replace(/\n\s*android:enableOnBackInvokedCallback="true"/, '')

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

/**
 * Drives one **note** half with a synthetic tree, and never through the verdict.
 *
 * The two tables are deliberately separate, so this asserts on `NOTE_SENSORS[ id ]` and the fixture pair
 * below asserts both directions: the note fires here, and the same tree produces **no** finding above.
 * A note that leaked into the gating array would be a FAIL on correct code, which is the failure mode
 * the research's false-positive column exists to prevent.
 */
function expectNote(id, files, fired, phrase) {
  const notes = findingsIn(files, NOTE_SENSORS[id])
  expectEqual(notes.length > 0, fired, `${id} note fired=${notes.length > 0} on ${JSON.stringify(Object.keys(files))}`)
  if (phrase && fired) {
    expectTrue(notes.join(' | ').includes(phrase), `${id} note "${notes.join(' | ')}" does not name ${phrase}`)
  }
  return notes
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
      [
        'a bar pinned below a weighted list with no navigation-bar inset is a finding; an inset is not, and neither is a header before the list nor a lone list',
        () => {
          const body = (lines) =>
            [
              'package com.gexetr.demo.ui',
              'import androidx.compose.foundation.layout.Column',
              'import androidx.compose.foundation.layout.fillMaxSize',
              'import androidx.compose.foundation.layout.fillMaxWidth',
              'import androidx.compose.foundation.lazy.LazyColumn',
              'import androidx.compose.material3.Surface',
              'import androidx.compose.material3.Text',
              'import androidx.compose.ui.Modifier',
              '',
              '@Composable',
              'fun RoundScreen() {',
              ...lines,
              '}',
              ''
            ].join('\n')
          const barAfterList = (bar) =>
            body([
              '    Column(modifier = Modifier.fillMaxSize()) {',
              '        LazyColumn(modifier = Modifier.weight(1f)) {',
              '            item { Text(label) }',
              '        }',
              `        ${bar}`,
              '    }'
            ])
          // No inset anywhere: the SystemBar's scrim paints over the bar, so it is unreadable and untappable.
          expectSensor(
            'uiux.insets',
            demoFiles({ [`${UI_REL}/Round.kt`]: barAfterList('Surface(modifier = Modifier.fillMaxWidth()) { Text(label) }') }),
            true,
            'navigation-bar inset'
          )
          // The same tree with the inset on the bar is clean.
          expectSensor(
            'uiux.insets',
            demoFiles({
              [`${UI_REL}/Round.kt`]: barAfterList(
                'Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) { Text(label) }'
              )
            }),
            false
          )
          // A Surface *before* the weighted list is a header, not bottom chrome.
          expectSensor(
            'uiux.insets',
            demoFiles({
              [`${UI_REL}/Round.kt`]: body([
                '    Column(modifier = Modifier.fillMaxSize()) {',
                '        Surface(modifier = Modifier.fillMaxWidth()) { Text(label) }',
                '        LazyColumn(modifier = Modifier.weight(1f)) {',
                '            item { Text(label) }',
                '        }',
                '    }'
              ])
            }),
            false
          )
          // A screen that is one list with the safe-drawing inset has no bar to judge.
          expectSensor(
            'uiux.insets',
            demoFiles({
              [`${UI_REL}/Round.kt`]: [
                'package com.gexetr.demo.ui',
                'import androidx.compose.foundation.layout.WindowInsets',
                'import androidx.compose.foundation.layout.fillMaxSize',
                'import androidx.compose.foundation.layout.safeDrawing',
                'import androidx.compose.foundation.layout.windowInsetsPadding',
                'import androidx.compose.foundation.lazy.LazyColumn',
                'import androidx.compose.material3.Text',
                'import androidx.compose.ui.Modifier',
                '',
                '@Composable',
                'fun ListScreen() {',
                '    LazyColumn(',
                '        modifier = Modifier',
                '            .fillMaxSize()',
                '            .windowInsetsPadding(WindowInsets.safeDrawing),',
                '    ) {',
                '        item { Text(label) }',
                '    }',
                '}',
                ''
              ].join('\n')
            }),
            false
          )
        }
      ],
      // ---- uiux.insets: the causes the owner photographed on a current phone -----------------
      [
        'a Scaffold whose content lambda never uses innerPadding is a finding; one that applies it is not',
        () => {
          const scaffold = (body) =>
            [
              'package com.gexetr.demo.ui',
              'import androidx.compose.foundation.layout.Column',
              'import androidx.compose.foundation.layout.fillMaxSize',
              'import androidx.compose.foundation.layout.padding',
              'import androidx.compose.material3.Scaffold',
              'import androidx.compose.material3.Text',
              'import androidx.compose.ui.Modifier',
              '',
              '@Composable',
              'fun HomeScreen() {',
              '    Scaffold(topBar = { Text(label) }) { innerPadding ->',
              `        ${body}`,
              '    }',
              '}',
              ''
            ].join('\n')
          expectSensor(
            'uiux.insets',
            demoFiles({ [`${UI_REL}/Home.kt`]: scaffold('Column(modifier = Modifier.fillMaxSize()) { Text(label) }') }),
            true,
            'innerPadding'
          )
          expectSensor(
            'uiux.insets',
            demoFiles({
              [`${UI_REL}/Home.kt`]: scaffold('Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) { Text(label) }')
            }),
            false
          )
        }
      ],
      [
        'a destructured first binding that is used is not a finding, and one bound to `_` is',
        () => {
          const scaffold = (params, body) =>
            [
              'package com.gexetr.demo.ui',
              'import androidx.compose.foundation.layout.Column',
              'import androidx.compose.foundation.layout.padding',
              'import androidx.compose.material3.Scaffold',
              'import androidx.compose.material3.Text',
              'import androidx.compose.ui.Modifier',
              '',
              '@Composable',
              'fun HomeScreen() {',
              `    Scaffold { ${params} ->`,
              `        ${body}`,
              '    }',
              '}',
              ''
            ].join('\n')
          // The scanner reads the *first* binding, which is what destructuring means: the PaddingValues is
          // the first parameter of the content lambda's signature, so either it is used or it is dropped.
          expectSensor(
            'uiux.insets',
            demoFiles({
              [`${UI_REL}/Home.kt`]: scaffold('(innerPadding, fabPosition)', 'Column(modifier = Modifier.padding(innerPadding)) { Text(label) }')
            }),
            false
          )
          expectSensor(
            'uiux.insets',
            demoFiles({ [`${UI_REL}/Home.kt`]: scaffold('(_, fabPosition)', 'Column { Text(label) }') }),
            true,
            'discards its PaddingValues'
          )
        }
      ],
      [
        'an implicit `it` that is used is not a finding, and one that is ignored is',
        () => {
          const scaffold = (body) =>
            [
              'package com.gexetr.demo.ui',
              'import androidx.compose.foundation.layout.Box',
              'import androidx.compose.foundation.layout.fillMaxSize',
              'import androidx.compose.foundation.layout.padding',
              'import androidx.compose.material3.Scaffold',
              'import androidx.compose.ui.Modifier',
              '',
              '@Composable',
              'fun HomeScreen() {',
              `    Scaffold { ${body} }`,
              '}',
              ''
            ].join('\n')
          expectSensor('uiux.insets', demoFiles({ [`${UI_REL}/Home.kt`]: scaffold('Box(modifier = Modifier.padding(it))') }), false)
          expectSensor('uiux.insets', demoFiles({ [`${UI_REL}/Home.kt`]: scaffold('Box(modifier = Modifier.fillMaxSize())') }), true, 'innerPadding')
        }
      ],
      [
        'a zeroed windowInsets on an M3 bar is a finding; the component default is not',
        () => {
          const bar = (insets) =>
            [
              'package com.gexetr.demo.ui',
              'import androidx.compose.foundation.layout.WindowInsets',
              'import androidx.compose.material3.NavigationBar',
              'import androidx.compose.material3.NavigationBarDefaults',
              'import androidx.compose.material3.Text',
              '',
              '@Composable',
              'fun HomeScreen() {',
              `    NavigationBar(windowInsets = ${insets}) { Text(label) }`,
              '}',
              ''
            ].join('\n')
          expectSensor('uiux.insets', demoFiles({ [`${UI_REL}/Bar.kt`]: bar('WindowInsets(0, 0, 0, 0)') }), true, 'switches off')
          expectSensor('uiux.insets', demoFiles({ [`${UI_REL}/Bar.kt`]: bar('NavigationBarDefaults.windowInsets') }), false)
        }
      ],
      [
        'WindowCompat.setDecorFitsSystemWindows(window, false) satisfies the edge-to-edge rule on its own',
        () => {
          const compat = (call) =>
            [
              'package com.gexetr.demo.ui',
              'import androidx.compose.foundation.layout.Column',
              'import androidx.compose.foundation.layout.fillMaxSize',
              'import androidx.compose.material3.Text',
              'import androidx.compose.ui.Modifier',
              'import androidx.core.view.WindowCompat',
              '',
              '@Composable',
              'fun HomeScreen() {',
              '    Column(modifier = Modifier.fillMaxSize()) { Text(label) }',
              '}',
              '',
              'fun MainActivity.onCreate() {',
              `    ${call}`,
              '}',
              ''
            ].join('\n')
          // The other file is replaced as well, so the assertion cannot pass because NAV3_APP's own
          // `enableEdgeToEdge()` happens to be in the tree — this fixture has to prove the *alternative*
          // form satisfies the rule, not that some other file does.
          const stub = 'package com.gexetr.demo.ui\n'
          expectSensor(
            'uiux.insets',
            demoFiles({
              [`${UI_REL}/MainActivity.kt`]: compat('WindowCompat.setDecorFitsSystemWindows(window, false)'),
              [`${UI_REL}/PlayKilnApp.kt`]: stub
            }),
            false
          )
          expectSensor(
            'uiux.insets',
            demoFiles({ [`${UI_REL}/MainActivity.kt`]: compat(''), [`${UI_REL}/PlayKilnApp.kt`]: stub }),
            true,
            'setDecorFitsSystemWindows'
          )
        }
      ],
      [
        'a screen that fills the window with no inset anywhere is a note inside a PASS, never a finding',
        () => {
          const bare = [
            'package com.gexetr.demo.ui',
            'import androidx.activity.enableEdgeToEdge',
            'import androidx.compose.foundation.layout.Column',
            'import androidx.compose.foundation.layout.fillMaxSize',
            'import androidx.compose.material3.Text',
            'import androidx.compose.ui.Modifier',
            '',
            'fun MainActivity.onCreate() {',
            '    enableEdgeToEdge()',
            '}',
            '',
            '@Composable',
            'fun HomeScreen() {',
            '    Column(modifier = Modifier.fillMaxSize()) { Text(label) }',
            '}',
            ''
          ].join('\n')
          const files = demoFiles({ [`${UI_REL}/PlayKilnApp.kt`]: bare })
          expectNote('uiux.insets', files, true, 'fills the window')
          // The other half of the pair: the note must not reach the array the verdict gates on. A note
          // that leaked would turn a correct screen into a FAIL, which is the one thing the research's
          // false-positive column forbids for this predicate.
          expectSensor('uiux.insets', files, false)
        }
      ],
      [
        'a statusBars-only screen with nothing cutout-aware is a note inside a PASS, never a finding',
        () => {
          const barsOnly = [
            'package com.gexetr.demo.ui',
            'import androidx.activity.enableEdgeToEdge',
            'import androidx.compose.foundation.layout.Column',
            'import androidx.compose.foundation.layout.fillMaxSize',
            'import androidx.compose.foundation.layout.statusBarsPadding',
            'import androidx.compose.material3.Text',
            'import androidx.compose.ui.Modifier',
            '',
            'fun MainActivity.onCreate() {',
            '    enableEdgeToEdge()',
            '}',
            '',
            '@Composable',
            'fun HomeScreen() {',
            '    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) { Text(label) }',
            '}',
            ''
          ].join('\n')
          const files = demoFiles({ [`${UI_REL}/PlayKilnApp.kt`]: barsOnly })
          expectNote('uiux.insets', files, true, 'cutout')
          expectSensor('uiux.insets', files, false)
        }
      ],
      [
        'a paren-less `Scaffold { … }` is still an inset-host ancestor; navigationBars-only fires the cutout note',
        () => {
          const app = (root) =>
            [
              'package com.gexetr.demo.ui',
              'import androidx.activity.enableEdgeToEdge',
              'import androidx.compose.foundation.layout.Column',
              'import androidx.compose.foundation.layout.fillMaxSize',
              'import androidx.compose.foundation.layout.padding',
              'import androidx.compose.material3.Scaffold',
              'import androidx.compose.material3.Text',
              'import androidx.compose.ui.Modifier',
              '',
              'fun MainActivity.onCreate() {',
              '    enableEdgeToEdge()',
              '}',
              '',
              '@Composable',
              'fun AppRoot() {',
              `    ${root}`,
              '}',
              ''
            ].join('\n')
          const details = (body) =>
            [
              'package com.gexetr.demo.ui',
              'import androidx.compose.foundation.layout.Column',
              'import androidx.compose.foundation.layout.fillMaxSize',
              'import androidx.compose.foundation.layout.navigationBarsPadding',
              'import androidx.compose.foundation.layout.statusBarsPadding',
              'import androidx.compose.material3.Text',
              'import androidx.compose.ui.Modifier',
              '',
              '@Composable',
              'fun DetailsScreen() {',
              `    ${body}`,
              '}',
              ''
            ].join('\n')
          // `Scaffold { padding -> … }` — the argument list is absent, which is how the idiom is written.
          // Its default insets contain the display cutout, so a screen below it must produce neither note.
          // The guard used to be `Scaffold\s*\(`, which matched neither this form nor the bar names, and
          // it therefore reported both notes on correct code; these two legs fail against that version.
          const withScaffold = app('Scaffold { padding -> Column(modifier = Modifier.padding(padding)) { Text(label) } }')
          const barsOnly = demoFiles({
            [`${UI_REL}/PlayKilnApp.kt`]: withScaffold,
            [`${UI_REL}/Details.kt`]: details('Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) { Text(label) }')
          })
          expectNote('uiux.insets', barsOnly, false)
          expectSensor('uiux.insets', barsOnly, false)
          // No inset token anywhere in this leg, so the Scaffold ancestor is the *only* thing that can
          // suppress the fillMaxSize note — which is exactly the clause the old guard got wrong.
          const bareRoot = demoFiles({
            [`${UI_REL}/PlayKilnApp.kt`]: withScaffold,
            [`${UI_REL}/Details.kt`]: details('Column(modifier = Modifier.fillMaxSize()) { Text(label) }')
          })
          expectNote('uiux.insets', bareRoot, false)
          expectSensor('uiux.insets', bareRoot, false)
          // The other system bar, with nothing cutout-aware and no host above the screen: the note fires.
          const navOnly = demoFiles({
            [`${UI_REL}/PlayKilnApp.kt`]: app('Column(modifier = Modifier.fillMaxSize()) { Text(label) }'),
            [`${UI_REL}/Details.kt`]: details('Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) { Text(label) }')
          })
          expectNote('uiux.insets', navOnly, true, 'cutout')
          expectSensor('uiux.insets', navOnly, false)
        }
      ],
      // ---- uiux.platform ------------------------------------------------------------------
      [
        'a manifest with no enableOnBackInvokedCallback is a finding; the declaration is not',
        () => {
          expectSensor('uiux.platform', demoFiles({ [MANIFEST_REL]: MANIFEST_NO_BACK_CALLBACK }), true, 'enableOnBackInvokedCallback')
          expectSensor('uiux.platform', demoFiles(), false)
        }
      ],
      [
        'an M3 Expressive alpha artifact in a version catalog is a finding; the pinned stable line is not',
        () => {
          const catalog = (version) =>
            [
              '[versions]',
              `material3 = "${version}"`,
              '',
              '[libraries]',
              'material3 = { module = "androidx.compose.material3:material3", version.ref = "material3" }',
              ''
            ].join('\n')
          expectSensor('uiux.platform', demoFiles({ 'gradle/libs.versions.toml': catalog('1.5.0-alpha28') }), true, 'alpha')
          expectSensor('uiux.platform', demoFiles({ 'gradle/libs.versions.toml': catalog('1.4.0') }), false)
        }
      ],
      [
        'the product-conditional surfaces are notes inside a PASS, never findings',
        () => {
          const notes = expectNote('uiux.platform', demoFiles(), true, 'splash-screen API')
          expectTrue(notes.length >= 5, `the encourage-only list produced only ${notes.length} note(s)`)
          expectSensor('uiux.platform', demoFiles(), false)
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

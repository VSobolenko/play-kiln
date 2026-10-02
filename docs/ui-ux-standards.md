# UI/UX standards for a PlayKiln slot

Every slot this factory ships gets a pronounced visual identity from
[`DESIGN.md`](../DESIGN.md) and `.cursor/rules/visual-identity.mdc`. This document is the other half:
the **interaction and layout** baseline every slot is born with. The rule the builder reads is
[`.cursor/rules/ui-ux-baseline.mdc`](../.cursor/rules/ui-ux-baseline.mdc); the sensor that measures it
is [`scripts/core/uiux.mjs`](../scripts/core/uiux.mjs).

Nothing here is invented. The owner's instruction on 2026-09-30 was explicit: *"you don't need to
invent UI/UX — there are professionals; use ready standards."* Every rule below names the published
source it comes from, and `uiux.rule` fails if a citation disappears from this page.

## Why this page exists

The owner reviewed the factory's first live app, `apps/2026-09-26-teacher-gradebook`, and said
(translated) that the interface was crude, that it did not use modern technologies, and that
**"strings wrap badly, like two words spread over seven lines."** He then said not to repair that app
but to make sure the next apps cannot repeat it.

Re-measured in this tree on 2026-09-30, the causes behind that sentence:

| Where | What | Which rule catches it |
|---|---|---|
| `ui/PlayKilnApp.kt:87` | a `mutableStateListOf` / `listSaver` back stack behind `NavDisplay`, instead of `rememberNavBackStack` | `uiux.navigation` |
| `ui/PlayKilnApp.kt:55` | a five-destination host that names no adaptive API, so the list/detail pair renders as one column on a tablet | `uiux.adaptive` |
| `ui/classroom/ClassroomScreen.kt:145` | `maxLines = 1` with no `overflow`, i.e. `TextOverflow.Clip` | `uiux.text-layout` |
| `ui/components/Average.kt:266` | a `TextButton` holding a label inside `Modifier.size(width = 96.dp, height = 48.dp)` | `uiux.text-layout` |
| `ui/components/Brand.kt:440` | `maxLines = 1` with no `overflow` | `uiux.text-layout` |
| `AndroidManifest.xml` | no `android:windowSoftInputMode="adjustResize"`, while `ClassroomTabs.kt:445` shows a text field | `uiux.insets` |
| every Kotlin file | no IME inset anywhere, so nothing in the layout reacts to the keyboard | `uiux.insets` |

## 1. Navigation — a host that owns its back stack

**Rule `uiux.navigation`.** The UI is hosted by Jetpack Navigation 3: `NavDisplay` over
`rememberNavBackStack(…)`, with one `NavEntry` per destination. A `rememberSaveable` +
`mutableStateListOf` + `listSaver` list is the defect — not because it is unusual, but because
`NavDisplay`'s transitions, its scene strategies, its predictive-back continuity and its deep-link
handling all key off the back stack it is given, so a hand-rolled list turns all four off at once.

- Navigation 3 — a back stack you own, a UI that updates with it including animations, and an
  adaptive layout system that can show more than one destination at a time:
  <https://developer.android.com/guide/navigation/navigation-3>
- Predictive back — the back gesture needs a back stack the platform can preview; intercepting
  `KeyEvent.KEYCODE_BACK` is no longer supported:
  <https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture>
- Which `navigation3-*` artifact a slot depends on:
  <https://developer.android.com/jetpack/androidx/releases/navigation>
- Vendored, in this repository: `.agents/skills/android-official/navigation-navigation-3/SKILL.md`.

## 2. Adaptive layout — a window, not a phone

**Rule `uiux.adaptive`.** A slot with more than one destination reads the window size class, directly
or through a scene strategy or an adaptive grid. A list that opens a detail destination must state its
two-pane policy: on the expanded width class the two panes are side by side, and on compact and medium
the detail replaces the list.

- Getting started with adaptive apps — the compact/medium/expanded classes,
  `currentWindowAdaptiveInfo().windowSizeClass`, panes, and Google Play ranking large-screen-optimised
  apps higher: <https://developer.android.com/develop/ui/compose/layouts/adaptive>
- Build a list-detail layout —
  <https://developer.android.com/develop/ui/compose/layouts/adaptive/list-detail>
- Vendored, in this repository: `.agents/skills/android-official/jetpack-compose-adaptive/SKILL.md`.

The APIs the sensor looks for are `currentWindowAdaptiveInfo`, `WindowSizeClass`,
`calculateWindowSizeClass`, `ListDetailSceneStrategy`, `SupportingPaneSceneStrategy`,
`rememberListDetailSceneStrategy`, `rememberSupportingPaneSceneStrategy`, `NavigationSuiteScaffold`,
`GridCells.Adaptive` and `StaggeredGridCells.Adaptive`. Naming one is the floor; the rule does not
claim the layout is good, only that the slot asked how wide the window is.

## 3. Text must be able to wrap and to grow

**Rule `uiux.text-layout`.** This is the rule the owner's review was about. Static analysis cannot see
pixels, so it does not claim to: it names the statically visible **causes** of a string that has no
option but to wrap into the narrowest column available.

Two causes, each provable in the reference slot:

1. **A fixed extent on an element that holds text.** `Modifier.width(28.dp)`,
   `Modifier.size(width = 96.dp, height = 48.dp)`, `Modifier.height(20.dp)` — a box that cannot grow.
   At a font scale above 1.0 the text is laid out into a width that was chosen for the text at 1.0,
   and no typography setting rescues it.
2. **A clamp with no indication of truncation.** `maxLines = 1` with no `overflow` argument is
   `TextOverflow.Clip`: the user sees a glyph sliced in half with nothing to say the text continues.

Text is sized by role from the Material 3 type scale and is never shrunk to fit a container. A
container grows around its label; a `Row` that must share width gives its text child
`Modifier.weight(…)` so the width is flexible by construction.

- WCAG 2.2 SC 1.4.4 **Resize Text** — text resizable to 200 % with no loss of content or
  functionality. The Understanding page states the exact failure the owner described: *"only one word
  of a sentence may fit on each line, causing the sentence to be displayed as a vertical column of
  text that is difficult to read"*, and names failure F69 — resizing causes text to be *"clipped,
  truncated or obscured"*: <https://www.w3.org/WAI/WCAG22/Understanding/resize-text.html>
- WCAG 2.2 SC 1.4.12 **Text Spacing** — text containers must be able to grow with their content:
  <https://www.w3.org/WAI/WCAG22/Understanding/text-spacing.html>
- `TextOverflow` — `Clip` is the default and shows a cut glyph; `Ellipsis` is what says "there is
  more": <https://developer.android.com/reference/kotlin/androidx/compose/ui/text/style/TextOverflow>
- Material 3 type scale — the roles text is sized by:
  <https://m3.material.io/styles/typography/type-scale-tokens>
- Compose previews — `@PreviewFontScales` (and a custom multipreview with `fontScale`) renders the
  same composable at more than one text size in one annotation:
  <https://developer.android.com/develop/ui/compose/tooling/previews>

Android's own maximum font scale is worth stating plainly because it is what makes the box the
defect: a user can set **2.0**. A layout that only works at 1.0 is a layout that works for the person
who wrote it.

## 4. Motion — a transition says where a screen came from

**Rule `uiux.motion`.** A slot that declares more than one screen animates at least one thing. The
rule is deliberately weak — `NavDisplay`'s own default transitions count — because the honest,
measurable claim is *"nothing in this slot animates"*, not *"this slot's motion is not to taste."*
The owner's complaint was not about taste; it was that a hard cut has no continuity for predictive
back to follow.

- Material 3 motion: <https://m3.material.io/styles/motion/overview>
- Predictive back, again, because continuity is what it previews:
  <https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture>

## 5. Accessibility — a name and a 48 dp target

**Rule `uiux.a11y`.** Every interactive element has an accessible name, and every touch target is at
least 48 dp. An `Icon`/`Image` passes a label or an explicit `null`; a clickable element carries a
`Text`, a non-null `contentDescription`, or an `onClickLabel`; nothing declares an extent below
`48.dp`.

- Android, "Make apps more accessible" — *"we recommend that each interactive UI element have a
  focusable area, or touch target size, of at least 48dpx48dp"*, and the `contentDescription`
  guidance, including passing `null` for a purely decorative icon:
  <https://developer.android.com/guide/topics/ui/accessibility/apps>
- Accessibility in Jetpack Compose — semantics, and *"Support user-scalable content"*:
  <https://developer.android.com/develop/ui/compose/accessibility>
- Semantics — role, state description, merged and cleared trees:
  <https://developer.android.com/develop/ui/compose/accessibility/semantics>
- Material 3 accessible design: <https://m3.material.io/foundations/accessible-design/overview>

## 6. Edge-to-edge and the IME

**Rule `uiux.insets`.** The owner's second live report was a screen that drew *"under the notification
panel, under the camera, and under the bottom buttons"* — written, he said, *"as if for old phones."*
On a current phone that is not a style complaint; it is the platform's own behaviour, and it is
measurable in source.

### The platform requirement

Edge-to-edge is **enforced**, not chosen: from `targetSdk 35` the window spans the whole display and
the app draws behind the system bars — *"Apps are edge-to-edge by default on devices running Android
15 if the app is targeting Android 15 (API level 35)"*, and *"This is a breaking change that might
negatively impact your app's UI."* The opt-out (`R.attr#windowOptOutEdgeToEdgeEnforcement`) is
deprecated and **disabled** at `targetSdk 36`, and this factory targets `36`, so there is nothing left
to switch off. The sensor accepts either form of the opt-in —
`enableEdgeToEdge()` or `WindowCompat.setDecorFitsSystemWindows(window, false)` — and fails on the
absence of both.

<https://developer.android.com/about/versions/15/behavior-changes-15> ·
<https://developer.android.com/about/versions/16/behavior-changes-16>

### What subsumes what

`displayCutout` is a **distinct** inset type. It is not in `systemBars`, and it is not in
`statusBars`. `safeDrawing` is the union that contains all three of the ones that matter:

```text
safeDrawing = systemBars ∪ ime ∪ displayCutout
systemBarsForVisualComponents = systemBars ∪ displayCutout   (what M3's bars use by default)
```

In portrait a top cutout usually sits inside the status bar, so a screen padded with
`statusBarsPadding()` *looks* correct. Rotate to landscape and the cutout moves to a vertical edge,
where `systemBars` contributes nothing on that side — the first character of a title, or a toolbar
icon, can end up behind the camera. That is the "text crawls onto the camera" report, and the sensor
names it: a screen that uses only `statusBars`/`systemBars`/`navigationBars` with nothing
cutout-aware anywhere is **reported as a note inside a PASS**, because the collision is real but
device- and orientation-dependent, and a hard failure there would fire on correct code.

<https://developer.android.com/develop/ui/compose/system/cutouts> ·
<https://developer.android.com/develop/ui/compose/system/insets> ·
<https://developer.android.com/develop/ui/compose/system/setup-e2e>

### `Scaffold` hands you the insets; it does not apply them

This is the mechanism behind the owner's top and bottom symptoms, and it is measured, not inferred:
*"By default, `Scaffold` provides insets as parameter `PaddingValues` for you to consume and use.
`Scaffold` does not apply the insets to content; this responsibility is yours."* So a `Scaffold` whose
content lambda drops its `innerPadding` is byte-for-byte as exposed as no `Scaffold` at all — the
first line under the notification panel and the last row under the gesture bar. The sensor reads the
lambda's first binding (named, destructured, or Kotlin's implicit `it`) and **fails** when the body
never uses it. A `Scaffold` is a fix only when its padding is consumed.

The same reference measures the other half: passing `WindowInsets(0, 0, 0, 0)` is the documented way
to disable a component's own handling, and an M3 bar's default is the set that unions the cutout in —
so a bar that zeroes its `windowInsets` gives up protection it had for free, and that is a failure
rather than a note. Zeroing an *inner* `Scaffold`'s `contentWindowInsets` is deliberately **not**
flagged: doing that is the documented shape when a host Scaffold already applied the insets.

<https://developer.android.com/develop/ui/compose/system/material-insets> ·
<https://developer.android.com/develop/ui/compose/system/insets-ui>

### The IME is a fourth, separate mechanism

The keyboard is not a system bar, `Scaffold`'s default `contentWindowInsets` does not carry it, and
the platform only resizes the window for it when the activity declares
`android:windowSoftInputMode="adjustResize"` — *"This setting allows your app to receive IME
insets"*. So whenever a slot shows a text field the sensor requires that declaration **and** an IME
inset read: `imePadding()`, `fitInside(…)`, a `WindowInsets` safe set, or a `ModalBottomSheet` that
carries it by default. The official edge-to-edge skill prefers
`Modifier.fitInside(WindowInsetsRulers.Ime.current)` over `imePadding()` *"because it reduces jank
and extra padding caused by forgetting to consume insets upstream"*, and the sensor accepts both.

<https://developer.android.com/develop/ui/compose/system/setup-e2e> ·
<https://developer.android.com/develop/ui/compose/system/evaluate-rulers>

### What the sensor cannot say

It names the **cause** of a collision, never the collision. Whether a `Text` actually reaches the
camera depends on the device, the orientation, the content and the font scale. The manual half is
named in `docs/research/2026-10-02-android-window-insets.md` §6 — turn on **Simulate a display with a
cutout**, rotate to landscape, and repeat in both **Gesture** and **3-button** navigation.

- Vendored, in this repository: `.agents/skills/android-official/system-edge-to-edge/SKILL.md`.

## 7. The modern Android platform surface

**Rule `uiux.platform`.** The owner's third sentence was about the app looking like *"just ordinary
buttons and ordinary texts"*, so this rule reads the surfaces a current Android app can stand on. It
holds two failures and a list of notes, and the split is the point: a **presence-only** measurement is
a note, because the token satisfies the check while the quality is a judgement.

**The two failures.**

- **Predictive back is unopted.** `android:enableOnBackInvokedCallback="true"` on `<application>` or
  `<activity>` is a one-line, universal declaration, and without it the system animation has nothing
  to preview. What the sensor does *not* also require is that the root does not intercept back —
  that half is not statically decidable.
  <https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture>
- **A Material 3 Expressive artifact on the alpha track.** The pinned Compose BOM `2026.09.00` maps to
  stable `material3 1.4.0`; every Expressive component and the `MotionScheme` physics live on
  `1.5.0-alpha*` behind `@ExperimentalMaterial3ExpressiveApi`. Requiring them would break the pin, so
  they are **forbidden rather than required** — measured per `…platform-surface.md` §6.
  <https://developer.android.com/jetpack/androidx/releases/compose-material3>

**The notes.** Each of these is worth doing and is reported in the PASS detail with the reason it
cannot be a gate, because every one of them is conditional on the slot's own job:

| Surface | Why it is a note |
|---|---|
| Splash screen (`core-splashscreen`, `installSplashScreen()`) | `<https://developer.android.com/develop/ui/views/launch/splash-screen>` — a perceived start-up improvement, and continuity needs a device and a stopwatch. |
| Share / text-processing target (`ACTION_SEND`, `ACTION_PROCESS_TEXT`) | Only meaningful when the slot produces something another app can consume. |
| Photo Picker (`PickVisualMedia`) | `<https://developer.android.com/training/data-storage/shared/photopicker>` — the recommended path when a slot reads media, and a slot that never does needs neither the picker nor the `READ_MEDIA_*` permission. |
| Haptics (`LocalHapticFeedback`) | `<https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedback>` — presence is a token while the semantic `HapticFeedbackType` is the actual point. |
| `animateItem` / shared elements | `<https://developer.android.com/develop/ui/compose/animation/shared-elements>` — load-bearing on a list whose order changes, decorative on a static one. |
| `SelectionContainer` | It helps only where the app's output is text a user would want to copy. |
| Autofill `contentType` | `<https://developer.android.com/develop/ui/compose/text/autofill>` — it applies only where the slot collects identity fields, and the wrong hint is worse than none. |

## 8. The factory's own half

**Rules `uiux.scaffold` and `uiux.rule`.** `scaffold/android-compose/` is the template every new slot
is copied from, so a finding there is the count of slots that would be born with the defect — it is
measured with the same sensors, and it is expected to be **red** until the orchestrator wires it. The
check is never weakened to make a wiring task look finished.

`uiux.rule` asserts that this page still cites every source the rules rest on, that
`.cursor/rules/ui-ux-baseline.mdc` still names every check id, and that each cited skill exists on
disk. A rule text that has drifted from its sensor is a rule nobody follows.

## What this standard does not check

A named gap is a result; a silent one is a defect. The sensor reads source, not pixels, and it refuses
to guess:

| Not checked | Why |
|---|---|
| A wrapped string, an ugly line break, a ragged column | Requires rendering. The manual half is a `@PreviewFontScales` render and a real device at a large font scale. |
| A `Text` with no `maxLines` | An unclamped `Text` in a filling or weighted container wrapping onto three lines is correct behaviour. |
| `contentDescription = null` on an icon | Whether an icon is decorative depends on the screen around it. The sensor fires only on an icon with **no** `contentDescription` argument. |
| An undeclared touch target that is too small | Not visible statically. Only a declared extent below `48.dp` is caught, which is the provable case. |
| Whether a clickable's name is a *good* name | A clickable whose subtree contains a `Text` is treated as named, because `clickable` merges its descendants' semantics — so a child `Text` **is** the announced label. Whether that wording is useful is a judgement, not a measurement. |
| A fixed extent inside a weighted child | The sensor skips any span that already uses `weight(`, which is the idiom the rule recommends. This under-approximates. |
| `widthIn` / `heightIn` | `heightIn(min = 48.dp)` is the correct touch-target idiom, and firing on it would punish the fix for `uiux.a11y`. |
| Whether the palette, hierarchy and depth are good | That is `DESIGN.md`, `.cursor/rules/visual-identity.mdc`, `scripts/visual-qa.py` and the owner's judgement. |
| Whether the app is usable at a 200 % font scale | Only a render can say. The sensor removes the *structural* causes; it cannot certify the result. |
| A landscape camera collision | Real but device- and orientation-dependent: the sensor reports a screen that is not cutout-aware as a **note inside a PASS**, never a failure. |
| A consumed `innerPadding` used for the wrong thing | The sensor proves the padding is read, not that it is applied where it matters. |
| A splash screen, a share target, a picker, a haptic | Presence is measurable; whether the slot's own job benefits is a judgement, so each is a note in the detail. |
| Whether an Expressive dependency is *wanted* | The sensor only fails the **alpha** track, which the pinned BOM does not carry. A stable future BOM is a toolchain decision. |

## How it is verified

```bash
node scripts/verify.mjs                        # `uiux.project` runs in the `invariants` group
node scripts/verify.mjs --slot <slug>          # the seven `uiux.*` slot checks, in the `ui` group
python scripts/visual-qa.py --slot <slug>      # the pixel half: palette, contrast, icon masks
```

`scripts/core/uiux.mjs` reads Kotlin and `AndroidManifest.xml` with comments and string literals
blanked, so an API named in a `TODO` is not a use and a fixed extent written inside a string is not a
modifier. `import` lines are blanked too, so an unused import of `ListDetailSceneStrategy` does not
satisfy `uiux.adaptive` — the rule reads the slot's code, not its dependency list. It spawns no
process and runs no Gradle. Applicability is derived by
`scripts/core/baseline.mjs`: a slot created before `2026-09-30`, a pipeline proof, and a killed slot
carry a finding as a note inside a PASS rather than a permanent red row, while every slot created from
today is gated.

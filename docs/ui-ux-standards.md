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

**Rule `uiux.insets`.** `enableEdgeToEdge()` in the activity — mandatory once the app targets
`targetSdk 35`, and this factory targets `36` — plus, whenever the slot shows a text field,
`android:windowSoftInputMode="adjustResize"` in the manifest **and** an IME inset read
(`imePadding()`, `fitInside(WindowInsetsRulers.Ime…)`, or a `WindowInsets.ime` reference).

The IME is the inset that catches people out: it is not a system bar, `Scaffold`'s own
`contentWindowInsets` does not carry it, and the platform only resizes the window for it when the
activity says `adjustResize`. A slot that declares neither can leave the field the user is typing into
underneath the keyboard.

- Compose window insets — `WindowInsets.ime`, and *"On Android 15 (API level 35) and higher, your app
  draws underneath the system bars and display cutouts once your app targets SDK 35"*:
  <https://developer.android.com/develop/ui/compose/system/insets>
- Vendored, in this repository: `.agents/skills/android-official/system-edge-to-edge/SKILL.md`.

## 7. The factory's own half

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

## How it is verified

```bash
node scripts/verify.mjs                        # `uiux.project` runs in the `invariants` group
node scripts/verify.mjs --slot <slug>          # the six `uiux.*` slot checks, in the `ui` group
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

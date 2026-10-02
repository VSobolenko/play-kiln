# The modern Android platform surface a well-built app uses

**Measured 2026-10-02.** Every external claim carries one of four labels: **MEASURED** (read on the
cited source), **ON RECORD** (a named source states it, not independently reproduced here),
**INFERRED** (this document's reasoning), **UNVERIFIED** (could not confirm). Availability against
this factory's pinned toolchain (Compose BOM `2026.09.00`, Material 3 `1.4.0`, Kotlin `2.3.21`,
AGP `9.4.1`, compileSdk 37 / targetSdk 36 / minSdk 26) is stated where it differs from "latest".

## What this measures

The owner's complaint is that agents "do not use Android's capabilities to the full", and that
shipped apps are "just ordinary buttons and ordinary texts". This document is a **decision list**,
not a catalogue: for each capability it records what the user actually gets, when it is worth it,
when it is wrong, what it costs (permission, manifest entry, dependency), and — the load-bearing
column for a factory whose gates are static source scans — **whether it is statically checkable from
Kotlin/XML under `app/src/main/`**. It measures external facts only; it states no factory rule and
resolves no conflict for the factory.

**The three checkability verdicts.**

- **DETECTABLE** — a source scan can require *or forbid* the API by name, reliably, without guessing.
- **WEAK** — only "mentions the API" is visible; a token call satisfies it, so it must **not** become
  a gate. Useful in a report, never a row that turns red.
- **NOT-DETECTABLE** — needs a run, a device, a pixel, or a judgement.

## Verdicts up front

- **The pinned BOM has *no* Material 3 Expressive.** Stable `material3` is `1.4.0` (September 09,
  2026); every Expressive component and the physics `MotionScheme` live in `1.5.0-alpha*` behind
  `@ExperimentalMaterial3ExpressiveApi` (**MEASURED**, release notes). A slot on the pinned BOM that
  wants Expressive must accept an alpha dependency — a toolchain decision, not a style one.
- **The factory's own `uiux.motion` already requires an animation API** and its pattern list already
  contains `animateItem`, `SharedTransitionLayout`, `AnimatedContent` and `NavDisplay`
  (**ON RECORD**, `scripts/core/uiux.mjs`). What is missing is not detection of motion but the
  *system integration* layer: predictive back's manifest flag, splash, share/text-processing targets,
  haptics, autofill content types, selection, and the glanceable surfaces.
- **Most of the "cool things" are DETECTABLE as presence and NOT-DETECTABLE as quality.** The honest
  split below is therefore short: presence-based gates that can be written today, and a longer list
  that must stay out of the gate because a token call would satisfy it.

---

## 1. Motion and animation

| API | Buys the user | Worth it when | Wrong when | Needs | Verdict |
|---|---|---|---|---|---|
| `NavDisplay` transitions / `AnimatedContent` / `AnimatedVisibility` / `Crossfade` | continuity — the screen tells you where it came from | any multi-screen app; screen-to-screen or state-to-state swap | a hard cut is deliberate (camera, security) | none | **DETECTABLE** (already `uiux.motion`) |
| `Modifier.animateItem` (replaces deprecated `animateItemPlacement`, Compose ≥ 1.7) | reorder/insert/remove in a list reads as movement, not teleport | any `LazyColumn`/`LazyRow` whose order can change | static list that never reorders | a stable `key` on the item | **DETECTABLE** by name |
| `SharedTransitionLayout` + `Modifier.sharedElement`/`sharedBounds` | a list thumbnail *becomes* the detail hero | a list→detail flow with a real hero object | flat list with no distinguishing object | `AnimatedContent`/`NavHost` scope | **DETECTABLE** by name |
| `animateContentSize`, `animate*AsState`, `updateTransition`, `Animatable` | small state changes feel physical | expanding rows, counters, progress | every tap animated | none | **DETECTABLE** by name |
| Material 3 `MotionScheme` / `MaterialTheme.motionScheme` (standard vs expressive physics) | one coherent physics scale app-wide; 21 components use it by default | when the app's motion should feel systematic | — | **alpha `1.5.0-alpha*` only** in the pinned BOM | **DETECTABLE presence** but *not available at the pin* — treat as an alpha-adoption decision |
| Decorative motion (ambient gradients, particles, animated rules) | little | a branded hero surface, sparingly | almost everywhere; it costs frames and battery | none | **NOT-DETECTABLE** |

**Meaningful vs decorative, in one sentence:** motion that connects two states the user can name
(navigation, list reorder, expand/collapse, shared hero) is meaningful; motion that exists only to
be noticed is decoration. The first category is presence-checkable; the second cannot be scanned.

## 2. Predictive back

- **The opt-in is `android:enableOnBackInvokedCallback="true"` on `<application>`** (overridable per
  `<activity>`) — **MEASURED**, *Add support for the predictive back gesture*. On Android 15+ the
  system animations are enabled by default; the flag is what makes the app's own back handling
  participate on 13–14 and what M3 sheets/search bars hook into (**MEASURED**, *Set up Predictive
  back*). No permission, no dependency beyond `material3` ≥ `1.3.0` for the components.
- **Intercepting back at the root activity disables the back-to-home animation** — **MEASURED**.
  That is the failure mode: a `BackHandler` at `MainActivity` to log or guard business logic kills
  the feature app-wide.
- **With Navigation 3** the precondition is the back stack the host owns (`rememberNavBackStack` +
  `NavDisplay`) — the factory's `uiux.navigation` check already enforces that — and NavDisplay's own
  transitions are what the gesture previews (**INFERRED** from the predictive-back guide plus the
  Navigation 3 model; the Navigation 3-specific doc was not fetched).
- **Verdict:** the manifest flag is **DETECTABLE**. Whether the gesture actually previews, and
  whether the app wrongly intercepts at the root, is **NOT-DETECTABLE** statically (a `BackHandler`
  at the root is visible as a token but "wrong root" needs the call graph and a device).

## 3. Adaptive and large screens

| API | Buys the user | Worth it when | Wrong when | Needs | Verdict |
|---|---|---|---|---|---|
| `currentWindowAdaptiveInfo()` / `currentWindowAdaptiveInfoV2()` | the layout reacts to the window, not the device | any slot with a list+detail or a wide layout | single-screen compact-only app | `material3-adaptive` (BOM-managed) | **DETECTABLE** by name (the factory's set already lists `currentWindowAdaptiveInfo`) |
| `NavigationSuiteScaffold` / `NavigationSuiteType` | bar on a phone, rail on a tablet/share | ≥ 3 top-level destinations | 1–2 destinations | `material3-adaptive-navigation-suite` | **DETECTABLE** by name (already in the factory's adaptive set) |
| `ListDetailPaneScaffold` / `ListDetailSceneStrategy` / `SupportingPaneSceneStrategy` | list and detail side by side instead of one stretched column | a list that opens a detail | single-pane app | `material3-adaptive` + Navigation 3 glue | **DETECTABLE** by name (`ListDetailSceneStrategy` is in the set; `ListDetailPaneScaffold` is **missing** from it) |
| Foldables: `WindowLayoutInfo`, `FoldingFeature`, `collectFoldingFeaturesAsState()`, `windowPosture`, `isTabletop` | content avoids the hinge; tabletop becomes a natural split | a foldable is a target audience | the app is phone-only and the cost is real | `androidx.window` / `material3-adaptive` | **DETECTABLE** by name; **correctness NOT-DETECTABLE** |
| Resizable / multi-window (`onConfigurationChanged`, dynamic size class) | the app survives split-screen without a restart | always on modern Android | — | none beyond reading the size class | **NOT-DETECTABLE** (the reactive read is inside `currentWindowAdaptiveInfo`) |

**What is missing from the factory's current adaptive vocabulary:** `currentWindowAdaptiveInfoV2`,
`ListDetailPaneScaffold`, `SupportingPaneScaffold`, `NavigationSuiteType`,
`collectFoldingFeaturesAsState`, `FoldingFeature` and `windowPosture` are named nowhere in
`ADAPTIVE_APIS` (**ON RECORD**, `scripts/core/uiux.mjs`). Only the *presence* of one adaptive API per
slot is measured today.

## 4. Haptics

- **API:** `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.<X>)` in Compose —
  **MEASURED**, `HapticFeedback` / `HapticFeedbackType`. The semantic constants are `Confirm`,
  `Reject`, `ToggleOn`, `ToggleOff`, `SegmentTick`, `SegmentFrequentTick`, `LongPress`,
  `ContextClick`, `GestureEnd`, `GestureThresholdActivate`, `KeyboardTap`, `TextHandleMove`,
  `VirtualKey` (**MEASURED**).
- **Cost:** none — no `VIBRATE` permission for the view/Compose haptic APIs (**MEASURED**, *Add
  haptic feedback to events*: the `HapticFeedbackConstants` route "doesn't require the `VIBRATE`
  permission").
- **Worth it when:** a state change has no other confirmation (a toggle, a threshold crossing in a
  drag, a destructive confirm), or tactile feedback reinforces a physical gesture.
- **Wrong when:** every tap, every scroll tick, or as a substitute for visual feedback. Default
  preference is "system decides" — a raw `Vibrator`/`VibrationEffect` wave bypasses the user's
  haptics setting and is the annoyance path (**MEASURED**: predefined `VibrationEffect`s need the
  `Vibrator` service; `Composition` primitives have no system fallback).
- **Verdict:** **DETECTABLE presence** (`LocalHapticFeedback` / `performHapticFeedback` /
  `HapticFeedbackType`). **WEAK as a gate** — any call satisfies it and the *right type at the right
  moment* is **NOT-DETECTABLE**.

## 5. Native system integration

| Surface | API / artifact | Buys the user | Worth it when | Wrong when | Needs | Verdict |
|---|---|---|---|---|---|---|
| Splash | `androidx.core:core-splashscreen` (docs state `1.0.0`), `Theme.SplashScreen`, `installSplashScreen()` before `super.onCreate()` | a cold start looks like the app, not a white flash | every app | a splash that lingers to hide a slow init | dependency + theme XML + call | **DETECTABLE** (dependency + `installSplashScreen` + theme parent) |
| Share out | `Intent.ACTION_SEND` filter on a receiver + `Intent.createChooser` / `ShareCompat` | text/CSV goes to any other app | the app produces something shareable | a utility with nothing to export | `<intent-filter>` | **DETECTABLE** (manifest filter + call) |
| Text-processing target | `ACTION_PROCESS_TEXT` exported activity, `EXTRA_PROCESS_TEXT`, `EXTRA_PROCESS_TEXT_READONLY` | the app appears in *any* app's selection toolbar ("Translate", "Make a note") | the app's job is to transform selected text | the app does not process text | exported activity + filter; one activity per action | **DETECTABLE** (manifest) |
| App shortcuts | `ShortcutManagerCompat` / `ShortcutInfoCompat`, `res/xml/shortcuts.xml` | long-press a key action without opening the app | 1–4 genuine top actions | a shortcut that just opens the app | manifest + XML/dynamic publish | **DETECTABLE** by name |
| Direct Share | `ShortcutManagerCompat` sharing shortcuts, `EXTRA_SHORTCUT_ID`, `ChooserTargetServiceCompat` | a person/target appears at the top of the share sheet | sharing is frequent and target is a person | no personal targets | metadata + filter | **DETECTABLE presence**; **quality NOT-DETECTABLE** |
| Widget | `androidx.glance:glance-appwidget`, `GlanceAppWidget`, `GlanceAppWidgetReceiver`, appwidget-provider XML | the app's one number is visible without opening it | a single glanceable datum | a widget that only launches the app | dependency + receiver + XML + update policy | **DETECTABLE by name**; **glanceability NOT-DETECTABLE** |
| Quick Settings tile | `TileService` + `android.permission.BIND_QUICK_SETTINGS_TILE` + `ACTION_QS_TILE` | a one-tap toggle from the shade | a real on/off or one-shot action | a tile that opens the app | manifest service + permission | **DETECTABLE by name** |
| Notifications | `NotificationChannel` + `NotificationManagerCompat.notify`; `POST_NOTIFICATIONS` (API 33+) | timely, user-controlled updates | the app has something time-sensitive | a channel the app never posts to, or a channel + permission it does not need | permission is a **cost**; channel id stable | **DETECTABLE presence**; **justification NOT-DETECTABLE** |
| Media pick | `ActivityResultContracts.PickVisualMedia` / `PickMultipleVisualMedia` | user picks one item, **no storage permission** | any image/video selection | never — the picker is the recommended path | none (falls back to `ACTION_OPEN_DOCUMENT`) | **DETECTABLE**; Play policy prefers it over `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` (**MEASURED**, Play Help) |

**Photo-picker policy note (MEASURED, Play Console Help):** apps targeting API 33+ may request
`READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` only where system pickers are not sufficient, and custom
pickers must still submit a declaration. So "reads media" + "no `PickVisualMedia`" + "requests broad
media permission" is a detectable, policy-relevant combination.

## 6. Material 3 Expressive

- **MEASURED**, Compose Material 3 release notes (2026-09-09): stable = **`1.4.0`**, alpha =
  **`1.5.0-alpha28`**. The Expressive set was developed on the `1.5.0-alpha` track; the December '25
  release blog states plainly that *"Material 3 Expressive APIs continue to be developed in the alpha
  releases"* (**MEASURED**).
- **The component set** (all in `1.5.0-alpha*`, most behind `ExperimentalMaterial3ExpressiveApi`):
  `ButtonGroup` (with `Modifier.animateWidth` compression/overflow menu; promoted in `alpha22`),
  `FloatingActionButtonMenu` + `ToggleFloatingActionButton` (graduated `alpha20`), `LoadingIndicator`
  / `ContainedLoadingIndicator` (morphing shapes, still experimental), the flexible `TopAppBar` /
  `BottomAppBar` family (graduated `alpha23`), expressive `ListItem`, `MaterialExpressiveTheme`,
  `MotionScheme` (introduced on the alpha track; `MaterialTheme.motionScheme` is the non-deprecated
  form as of `alpha27`), `SplitButtonLayout`, expressive dropdown menu items (**MEASURED**, release
  notes + API reference).
- **Availability at this factory's pin:** **no** — Material 3 `1.4.0` does not contain these APIs.
  A slot must either accept `material3:1.5.0-alpha*` (alpha dependency, opt-in annotation) or ship
  on stable `1.4.0` with its own components. **Be explicit: I could not confirm any Expressive
  component in `1.4.0`, and the release notes place their promotion in `1.5.0-alpha*`.**
- **Verdict:** presence of the alpha artifact/annotation is **DETECTABLE**; taste is not.

## 7. Dynamic color versus a declared brand identity

- **MEASURED:** on API 31+ `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)`
  derive the whole `ColorScheme` from the user's wallpaper, and the canonical `Theme.kt` snippet
  chooses them in place of the static scheme. `ColorScheme` also exposes fixed roles
  (`primaryFixed`, …) and `MaterialColors.harmonize` for brand colours that should survive
  (**ON RECORD**: Android *Material Design 3 in Compose*, *Enable users to personalize*).
- **The exact conflict:** dynamic color **replaces the role values**, so an app that declares its own
  palette but routes `MaterialTheme(colorScheme = dynamic…ColorScheme(context))` does not ship its
  identity — it ships the user's wallpaper recoloured. The factory's `visual-identity.mdc` requires
  the slot's **own** palette and its own components; a `dynamic*ColorScheme` branch at the theme host
  is therefore a direct contradiction of the declared identity, not a personalization feature layered
  on top of it.
- **This document does not resolve it.** The correct policy is the factory's decision. What is
  **MEASURED** is only the mechanism: `dynamicLightColorScheme`/`dynamicDarkColorScheme` presence is
  **DETECTABLE** (so a scan can require *or forbid* it), while "is this the right brand trade-off" is
  **NOT-DETECTABLE**.

## 8. Performance and correctness surface

| Item | Buys the user | Worth it when | Wrong when | Needs | Verdict |
|---|---|---|---|---|---|
| Baseline Profiles (`androidx.baselineprofile` **1.5.0** plugin, `profileinstaller` **1.4.1**, Macrobenchmark module) | faster cold start and first scroll on real devices | every shipped app; the factory already pins it and the scaffold ships a `baselineprofile/` module | — | a device/emulator to generate | **DETECTABLE** (plugin + module + benchmarks present) |
| Strong Skipping (Compose compiler) | fewer recompositions with no code change | always — **default** with Kotlin 2.0.20+ (factory pins 2.3.21) | — | none | **NOT-DETECTABLE** (compiler behaviour, not a source token) — visible only in compiler reports / a run |
| `@Stable` / `@Immutable` | lets the compiler skip an unstable class | a data class handed to a lazy list | sprinkled on mutable classes — an `@Immutable` lie hides bugs | none | **WEAK** as a gate (presence is a token; the claim's truth is **NOT-DETECTABLE**) |
| `derivedStateOf` | de-duplicates hot state reads (scroll, derived flags) | scroll-derived booleans, expensive derivations | wrapping every value "for performance" | none | **WEAK** (token-satisfiable) |
| `key` on `items(...)`/`item(...)` | correct identity on reorder; enables `animateItem` | lists whose contents/order change | a short static list needs none | none | **WEAK** (a key is always addable; *when it is needed* is not static) |
| `LazyColumn` over `Column` + `verticalScroll` | virtualization for long/unknown lists | more items than fit; dynamic data | a fixed 3-row settings column does not need a lazy list | `foundation` | **WEAK/NOT-DETECTABLE** (the right choice depends on the expected count) |

## 9. The non-visual "cool things" the owner would mean

| Item | API | Buys the user | Worth it when | Wrong when | Needs | Verdict |
|---|---|---|---|---|---|---|
| Autofill | `Modifier.contentType(ContentType.Username)` / `Modifier.semantics { contentType = … }` | one-tap fill from the password manager / keyboard | the app has identity fields (login, signup) | a field that is not identity — wrong hint hurts | none | **DETECTABLE** by name; the *right* content type is **NOT-DETECTABLE** |
| Text selection | `SelectionContainer`, `SelectionState`, `DisableSelection`, `LinkAnnotation` | users can copy the app's output | an app whose output is text | a form where selection fights editing; lazy lists have undefined selection behaviour off-screen | `foundation` | **DETECTABLE** by name |
| Drag and drop | `Modifier.dragAndDropSource` / `dragAndDropTarget`, `ClipDescription` | move an item to another app or pane | genuine reordering or cross-app transfer | decorative "shaking" with no drop target | `foundation` | **DETECTABLE presence** |
| Keyboard behaviour | `KeyboardOptions` (`keyboardType`, `imeAction`), `ImeAction` | the keyboard's action key does the expected thing ("Done", "Next", "Search") | every text field | — | none | **DETECTABLE presence**; the factory's `uiux.insets` already measures the IME inset half |
| Accessibility service compatibility | semantics (`contentDescription`, role, state description), 48 dp targets | TalkBack/Switch Access can use the app | always | — | none | Already `uiux.a11y`; deeper compatibility is **NOT-DETECTABLE** |
| Word suggestions / IME lifecycle | platform default via `TextField` state + `ImeAction` | correct suggestions, no lost input | any input | — | none | **NOT-DETECTABLE** beyond field presence |

---

## The ranked gate candidates — DETECTABLE, and genuinely worth requiring

Ordered by value per line of check. Each is presence-based and cannot be satisfied by a comment
(the scanners must blank comments/strings, as `scripts/core/uiux.mjs` already does).

1. **Predictive back opt-in** — `<application android:enableOnBackInvokedCallback="true">`. Cheap,
   decisive, and its absence is invisible until a device is used. *Do not also require that the root
   does not intercept; that half is not statically decidable.*
2. **Splash Screen API** — `core-splashscreen` dependency + `installSplashScreen()` + a
   `Theme.SplashScreen` parent theme. Perceived start-up cost with no runtime risk.
3. **Share / text-processing targets, only when the app has shareable output** — an `ACTION_SEND`
   or `ACTION_PROCESS_TEXT` filter. Conditional on the slot's job; not a blanket row.
4. **Photo Picker instead of broad media permissions** — `PickVisualMedia` present, and fail if the
   manifest requests `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` without it. Directly tied to a Play
   policy (**MEASURED**).
5. **Adaptive vocabulary widened** — extend the existing presence set with
   `ListDetailPaneScaffold`, `currentWindowAdaptiveInfoV2`, `NavigationSuiteType`,
   `collectFoldingFeaturesAsState`; keep the existing "one of these when a detail destination
   exists" shape.
6. **Haptics presence, reported not gated** — detectable, but the semantic type is the point and a
   scan cannot judge it. (Listed here for completeness; see the WEAK list for why it must not turn
   red.)
7. **Autofill content types on identity fields** — detectable presence; conditional on the app
   collecting identity.
8. **Content-selection / copy affordance where the app produces text** — `SelectionContainer`
   presence, conditional on the app's output being text.
9. **Glance widget / QS tile / notification channel** — each detectable by name, and each a product
   decision: require only where the slot's job implies the surface (a glanceable value, a toggle, a
   time-sensitive event). **Do not require all three.**
10. **Baseline Profile module** — the factory already pins the plugin and the scaffold already ships
    the module; a presence check keeps it from silently disappearing.

**Two things to gate as *forbidden* rather than required:** the Expressive alpha artifacts (a
toolchain decision, not an accident — see §6) and `dynamic*ColorScheme` in a slot whose declared
identity is its own palette (§7), unless the factory decides otherwise.

## The encourage-only list — worth doing, must never be a gate

A scanner would either pass any token or fire wrongly, and a noisy gate is read past.

- **`MotionScheme` / Expressive components** — alpha-only at the pin; requiring them or forbidding
  them is a toolchain decision, and `@ExperimentalMaterial3ExpressiveApi` presence proves nothing
  about use.
- **Motion quality** — any of the ~12 animation APIs satisfies a presence check; whether the motion
  is meaningful is a human judgement.
- **Haptic moments and types** — one `performHapticFeedback` call satisfies it; the semantic
  constant can be wrong and still pass.
- **`@Stable` / `@Immutable`, `derivedStateOf`, `key`, lazy-vs-scroll** — all token-satisfiable, and
  the *right* choice depends on the data, not the source text.
- **Strong skipping** — a compiler default; no source token exists.
- **Adaptive/foldable *correctness*** — reacting to a hinge is not the same as reacting *well*.
- **Widget glanceability, share round-trips, notification usefulness, ordering of content
  descriptions for screen readers, IME suggestion quality, drag-and-drop payload correctness** — each
  needs a run or a device; presence is not behaviour.
- **Splash continuity and startup feel** — needs a device and a stopwatch.

## What this document cannot say

It is a reading of published documentation, not a device measurement: no claim here was reproduced
on hardware, and the `m3.material.io` motion page returned only its title to the fetch tool
(**UNVERIFIED** for any figure taken from that page alone). BOM-to-library mapping for
`2026.09.00` was taken from the factory's own record plus the Android BOM-mapping page
(**ON RECORD**); `mvnrepository.com` blocked automated fetch. Material 3 Expressive availability at
`1.4.0` is **MEASURED as absent** from the release notes, but the mapping of the pinned BOM to
`material3:1.4.0` rests on the factory's pin, not an independent read of that exact BOM.

## Sources

Every URL the fetch/search tool returned content from for this document (2026-10-02), grouped by
topic. Where a page was returned by search as a content snippet rather than a full fetch, it is in
this list because that content was read here.

- **Compose Material 3 / Expressive:** <https://developer.android.com/jetpack/androidx/releases/compose-material3> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/LoadingIndicator.composable> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/ButtonGroup.composable> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialExpressiveTheme.composable> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialTheme.composable> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme> · <https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-motion-scheme/> · <https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html> · <https://mvnrepository.com/artifact/androidx.compose.material3/material3/1.4.0> · <https://zoewave.medium.com/compose-material-3-expressive-89f4147df5b8>
- **BOM mapping:** <https://developer.android.com/develop/ui/compose/bom/bom-mapping> · <https://mvnrepository.com/artifact/androidx.compose/compose-bom/2026.01.00/dependencies> · <https://mvnrepository.com/artifact/androidx.compose/compose-bom/2026.09.00/dependencies> (Cloudflare-blocked)
- **Animation / shared elements:** <https://developer.android.com/develop/ui/compose/animation/shared-elements> · <https://developer.android.com/develop/ui/compose/animation/shared-elements/customize> · <https://developer.android.com/reference/kotlin/androidx/compose/animation/SharedTransitionScope> · <https://developer.android.com/develop/ui/compose/animation/composables-modifiers> · <https://developer.android.com/reference/kotlin/androidx/compose/animation/ContentTransform> · <https://developer.android.com/jetpack/androidx/releases/compose-animation> · <https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyItemScope>
- **Predictive back:** <https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture> · <https://developer.android.com/develop/ui/compose/system/predictive-back-setup> · <https://developer.android.com/codelabs/predictive-back> · <https://developer.android.google.cn/codelabs/predictive-back> · <https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back>
- **Haptics:** <https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedback> · <https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedbackType> · <https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/package-summary> · <https://developer.android.com/develop/ui/views/haptics/haptic-feedback> · <https://developer.android.com/develop/ui/compose/compositionlocal>
- **Adaptive / foldables:** <https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps> · <https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes> · <https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation> · <https://developer.android.com/develop/adaptive-apps/guides/foldables/make-your-app-fold-aware> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/navigationsuite/NavigationSuiteScaffold.composable>
- **Splash / sharing / shortcuts:** <https://developer.android.com/develop/ui/views/launch/splash-screen> · <https://developer.android.com/develop/ui/views/launch/splash-screen/migrate> · <https://developer.android.com/reference/androidx/core/splashscreen/SplashScreen> · <https://developer.android.com/reference/androidx/core/content/pm/ShortcutManagerCompat> · <https://developer.android.com/training/sharing/direct-share-targets> · <https://medium.com/androiddevelopers/custom-text-selection-actions-with-action-process-text-191f792d2999> · <https://stackoverflow.com/questions/34559992/android-marshmallow-text-selection-option-menu-actions>
- **Widgets / tiles / notifications:** <https://developer.android.com/develop/ui/compose/glance/create-app-widget> · <https://developer.android.com/develop/ui/compose/glance/glance-app-widget> · <https://developer.android.com/reference/kotlin/androidx/glance/appwidget/GlanceAppWidgetReceiver> · <https://developer.android.google.cn/reference/kotlin/android/service/quicksettings/TileService> · <https://medium.com/androiddevelopers/quick-settings-tiles-e3c22daf93a8>
- **Dynamic color:** <https://developer.android.google.cn/develop/ui/compose/designsystems/material3> · <https://developer.android.com/develop/ui/views/theming/dynamic-colors> · <https://source.android.com/docs/core/display/dynamic-color> · <https://github.com/Drjacky/claude-android-ninja/blob/master/references/android-theming.md>
- **Performance:** <https://developer.android.com/develop/ui/compose/performance/stability/strongskipping> · <https://developer.android.com/develop/ui/compose/performance/stability/fix> · <https://developer.android.google.cn/develop/ui/compose/performance/bestpractices> · <https://github.com/rcosteira79/android-skills/blob/main/plugins/android-skills/skills/compose/references/performance.md> · <https://www.davideagostini.com/android/2026-02-15-lazycolumn-skip-recomposition-user-list>
- **Photo picker / media policy:** <https://developer.android.com/training/data-storage/shared/photo-picker> · <https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.PickVisualMedia> · <https://support.google.com/googleplay/android-developer/answer/16935362> · <https://developer.android.com/about/versions/14/changes/partial-photo-video-access> · <https://medium.com/androiddevelopers/permissionless-is-the-future-of-storage-on-android-3fbceeb3d70a>
- **Autofill / selection:** <https://developer.android.com/develop/ui/compose/text/autofill> · <https://developer.android.com/reference/kotlin/androidx/compose/ui/autofill/contentType.modifier> · <https://developer.android.com/develop/ui/compose/text/user-interactions> · <https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/selection/SelectionContainer.composable>
- **Motion spec:** <https://m3.material.io/styles/motion/overview/how-it-works> (title only returned)

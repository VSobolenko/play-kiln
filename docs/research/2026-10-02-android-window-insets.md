# Android window insets — platform rules, the API surface, and what a static sensor can see

**Measured 2026-10-02.** Every external claim carries one of four labels: **MEASURED** (read on the cited source, URL given), **ON RECORD** (asserted by a named secondary source), **INFERRED** (this document's reasoning), **UNVERIFIED** (could not confirm). Quotations are verbatim; the label says so when a page is paraphrased.

## What this measures

The platform rule that forces edge-to-edge (from which `targetSdk`, and what changes), the exact API that opts into it, the complete `WindowInsets` vocabulary and its subsumption relationships, every Compose modifier that consumes insets, which Material 3 components handle insets themselves and where that stops, the mechanism behind three real on-device symptoms (content under the status bar, under a punch-hole cutout, and under the bottom navigation bar), how to reproduce each on a phone, and — the point of the document — **which of those failures a static scan of Kotlin under `app/src/main/` can reliably detect and which it cannot**. It records external facts and detection predicates only.

---

## 1. The platform requirement

### Enforcement, and from which `targetSdk`

- **MEASURED**, *Behavior changes: Apps targeting Android 15 or higher*, section *Edge-to-edge enforcement*: **"Apps are edge-to-edge by default on devices running Android 15 if the app is targeting Android 15 (API level 35)."** The same page: **"This is a breaking change that might negatively impact your app's UI."** — <https://developer.android.com/about/versions/15/behavior-changes-15>
- **MEASURED**, *Display content edge-to-edge in views*: **"Once you target SDK 35 or higher on a device running Android 15 or higher, your app is displayed edge-to-edge. The window spans the entire width and height of the display by drawing behind the system bars. System bars include the status bar, caption bar, and navigation bar."** and **"Important: Edge-to-edge is enforced on Android 15 (API level 35) and higher once your app targets SDK 35. If your app is not already edge-to-edge, portions of your app may be obscured and you must handle insets."** — <https://developer.android.com/develop/ui/views/layout/edge-to-edge>
- **MEASURED**, *Compatibility framework changes (Android 15)*, the change record: `ENFORCE_EDGE_TO_EDGE`, **Change ID `309578419`**, **Default State: "Enabled for apps that target Android 15 (API level 35) or higher."** — <https://developer.android.com/about/versions/15/reference/compat-framework-changes>

### What exactly changes when enforcement is on — MEASURED, quoted

From *Behavior changes: Apps targeting Android 15 or higher* (same URL):

- **Gesture handle navigation bar** — *"Transparent by default."* · *"Bottom offset is disabled so content draws behind the system navigation bar unless insets are applied."* · *"`setNavigationBarColor` and `R.attr#navigationBarColor` are deprecated and don't affect gesture navigation."*
- **3-button navigation** — *"Opacity set to 80% by default, with color possibly matching the window background."* · *"Bottom offset disabled so content draws behind the system navigation bar unless insets are applied."* · *"`setNavigationBarContrastEnforced` and `R.attr#navigationBarContrastEnforced` is true by default, which adds an 80% opaque background across 3-button navigation."*
- **Status bar** — *"Transparent by default."* · *"The top offset is disabled so content draws behind the status bar unless insets are applied."* · *"`setStatusBarColor` and `R.attr#statusBarColor` are deprecated and have no effect on Android 15."*
- **Display cutout** — *"`layoutInDisplayCutoutMode` of non-floating windows must be `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`. `SHORT_EDGES`, `NEVER`, and `DEFAULT` are interpreted as `ALWAYS` so that users don't see a black bar caused by the display cutout and appear edge-to-edge."*

*Deprecated and disabled* on that page include `Window#setDecorFitsSystemWindows`, `Window#setStatusBarColor`, `Window#setNavigationBarColor` (gesture navigation). *Deprecated but not disabled* include `Window#isStatusBarContrastEnforced` and `setNavigationBarColor` for 3-button. **MEASURED**.

**Content that is now hidden** is named concretely on that page: *"Hidden UI includes the Material 2 top app bar, floating action buttons, and list items."* **MEASURED**.

### The opt-out, and its removal

- **MEASURED**, *Behavior changes: Apps targeting Android 16 or higher*, *Edge to edge opt-out going away*: **"Android 15 enforced edge-to-edge for apps targeting Android 15 (API level 35), but your app could opt-out by setting `R.attr#windowOptOutEdgeToEdgeEnforcement` to `true`. For apps targeting Android 16 (API level 36), `R.attr#windowOptOutEdgeToEdgeEnforcement` is deprecated and disabled, and your app can't opt-out of going edge-to-edge."** The same section states the device split: a targetSdk-36 app on an **Android 15 device** keeps the opt-out; on an **Android 16 device** it is disabled. — <https://developer.android.com/about/versions/16/behavior-changes-16>
- **ON RECORD** (Android Police, 2025): *"Android 16 removes this opt-out, mandating full-screen display for apps targeting the new OS."* — <https://www.androidpolice.com/android-16-full-screen-edge-to-edge-no-opt-out/>

`targetSdk` 36 is therefore the first version at which there is no escape hatch at all; the platform guarantee from 35 onward is only that the app may opt out, and only on Android 15.

### `enableEdgeToEdge()` — exact API

- **MEASURED**, *androidx.activity package summary*: `fun ComponentActivity.enableEdgeToEdge(statusBarStyle: SystemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT), navigationBarStyle: SystemBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim)): Unit`, described as *"Enables the edge-to-edge display for this `ComponentActivity`."* Kotlin extension on `ComponentActivity`; package **`androidx.activity`**; artifact **`androidx.activity:activity`** (needs 1.8.1 or newer; `activity-compose` 1.9.0 or newer for Compose hosts). — <https://developer.android.com/reference/kotlin/androidx/activity/package-summary> · <https://developer.android.com/reference/androidx/activity/EdgeToEdge>
- **MEASURED**, same page: *"The default style configures the system bars with a transparent background when contrast can be enforced by the system (API 29 or above). On older platforms (which only have 3-button/2-button navigation modes), an equivalent scrim is applied to ensure contrast with the system bars."*
- **MEASURED**, *Display content edge-to-edge in views*: *"By default, `enableEdgeToEdge()` makes the system bars transparent, except on 3-button navigation mode where the navigation bar gets a translucent scrim. The colors of the system icons and the scrim are adjusted based on the system light or dark theme."*
- **INFERRED** (mechanism): the opt-in is `WindowCompat.setDecorFitsSystemWindows(window, false)` plus system-bar styles. The AOSP Compose source states the equivalent for the IME half — *"Developers should set `android:windowSoftInputMode=\"adjustResize\"` in their `AndroidManifest.xml` file and call `WindowCompat.setDecorFitsSystemWindows(window, false)` in their `android.app.Activity.onCreate`."* **MEASURED** as a source comment — <https://android.googlesource.com/platform/frameworks/support/+/4a82be8eaba0cb03d9ea4fe58bc99da7853a4237/compose/foundation/foundation-layout/src/androidMain/kotlin/androidx/compose/foundation/layout/WindowInsets.android.kt>

### What happens WITHOUT it

| App `targetSdk` | Device | Behaviour | Label |
|---|---|---|---|
| `< 35` | any | Not enforced. The window does **not** draw behind the bars by default; the platform insets content for you. Calling `enableEdgeToEdge()` is what makes it edge-to-edge. | MEASURED (*Views: edge-to-edge*, *Compose: about window insets*) |
| `35` | Android 15/16 | Enforced regardless of `enableEdgeToEdge()`. Content draws behind all bars and the cutout; insets must be applied or UI is hidden. | MEASURED |
| `35` | Android 14 or lower | Not enforced by the platform. Without an explicit call the app keeps the pre-15 look. | MEASURED (*Compose: about window insets* — "On Android 14 (API level 34) and lower, your app's UI does not draw underneath the system bars and display cutouts by default.") |
| `36` | Android 16 | Enforced, no opt-out, `windowOptOutEdgeToEdgeEnforcement` disabled. | MEASURED |
| `36` | Android 15 | Enforced; the opt-out attribute still works here. | MEASURED |
| any | any | `enableEdgeToEdge()` gives one consistent behaviour across all of the above, which is why the official guidance is to call it unconditionally. | MEASURED (*Views: edge-to-edge*) |

**MEASURED**, *Compose: about window insets*: *"On Android 15 (API level 35) and higher, your app draws underneath the system bars and display cutouts once your app targets SDK 35."* — <https://developer.android.com/develop/ui/compose/system/insets>

---

## 2. The insets vocabulary, and what subsumes what

All types are `@Composable` accessors on `WindowInsets.Companion` in package `androidx.compose.foundation.layout`, backed by `WindowInsetsCompat.Type.*` in `androidx.core.view`. **MEASURED** — <https://developer.android.com/develop/ui/compose/system/insets> · <https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type>

| Accessor (`WindowInsets.…`) | What it is, quoted where possible | Real screen feature it protects |
|---|---|---|
| `statusBars` | *"The insets describing the status bars. These are the top system UI bars containing notification icons and other indicators."* | Status bar / notification panel (top). |
| `navigationBars` | *"The system UI bars on the left, right, or bottom side of the device, describing the taskbar or navigation icons."* | Gesture pill (~24 dp, transparent) or 3-button bar (~48 dp, 80% scrim). |
| `captionBar` | *"The inset describing the system UI window decoration if in a freeform window, like top title bar."* | Freeform/desktop window title bar. Empty on a phone. |
| `systemBars` | *"The union of the system bar insets, which include the status bars, navigation bars, and caption bar."* | All three above, together. |
| `ime` | *"The insets describing the amount of space on the bottom that the software keyboard occupies."* | On-screen keyboard. **Not a system bar.** |
| `displayCutout` | *"The insets representing the amount of spacing needed to avoid overlapping with a display cutout (notch or pinhole)."* | Punch-hole/notch camera. **Separate from `systemBars`.** |
| `systemGestures` | *"The insets representing the amount of insets where the system will intercept gestures for navigation. Apps can manually specify handling a limited amount of these gestures using `Modifier.systemGestureExclusion`."* | Back/home edge-swipe zones. |
| `mandatorySystemGestures` | *"A subset of the system gestures that will always be handled by the system, and which can't be opted out using `Modifier.systemGestureExclusion`."* | Non-excludable gesture zone. |
| `tappableElement` | *"the amount of space where 'taps' will be handled by the system, and not the app."* | The tappable part of a transparent nav bar. |
| `waterfall` | *"The insets representing the curved areas of a waterfall display … where the screen begins to wrap along the sides of the device."* | Curved screen edges. |
| `safeDrawing` | *"Use `WindowInsets.safeDrawing` to protect content that shouldn't be drawn underneath any system UI. This is the most common usage of insets."* | Visual-overlap safety. |
| `safeGestures` | *"Use `WindowInsets.safeGestures` to protect content with gestures. This avoids system gestures clashing with app gestures."* | Gesture-conflict safety. |
| `safeContent` | *"a combination of `WindowInsets.safeDrawing` and `WindowInsets.safeGestures` to ensure content has no visual overlap and no gesture overlap."* | Both. |

### The subsumption relationships — MEASURED (AOSP source)

**MEASURED**, `WindowInsetsHolder`, `WindowInsets.android.kt` (URL above):

```text
safeDrawing  = systemBars ∪ ime ∪ displayCutout
safeGestures = tappableElement ∪ mandatorySystemGestures ∪ systemGestures ∪ waterfall
safeContent  = safeDrawing ∪ safeGestures
```

**MEASURED**, the same file's KDoc: `safeDrawing` *"includes all system bars, display cutout, and soft keyboard"*; `safeGestures` *"includes system gestures, mandatory system gestures, rounded display areas, and tappable areas"*; `safeContent` *"including everything in safeDrawing and safeGestures"*.

**The crux.** `systemBars` does **not** contain `displayCutout`, and it does not contain `ime`.

- `safeDrawing ⊃ systemBars`, `safeDrawing ⊃ displayCutout`, `safeDrawing ⊃ ime`.
- In **portrait** a top punch-hole is usually inside the status bar, so `statusBars` happens to cover it. **MEASURED**, *Views: edge-to-edge*: *"Typically, the cutout is at the top of the screen and included in the status bar. When the device screen is in landscape mode, the cutout may be on the vertical edge."*
- In **landscape** the cutout is on a *side*; `statusBars` has no left/right extent there and `navigationBars` has no top/left-in-landscape extent for it. Only `displayCutout` (or a set that unions it) protects it. An agent that pads with `statusBars` alone collides with the camera in landscape. **INFERRED** from the two MEASURED facts above.

- `WindowInsets` supports `union(insets)` (per-side maximum), `exclude(insets)` (per-side subtraction), `add(insets)` (per-side sum), and `only(sides)` taking `WindowInsetsSides.{Start, End, Left, Right, Top, Bottom, Horizontal, Vertical}` (`Horizontal = Left + Right`, `Vertical = Top + Bottom`; `Start`/`End` are layout-direction aware). **MEASURED** — <https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/WindowInsets> · <https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/WindowInsetsSides>
- `*IgnoringVisibility` variants (`statusBarsIgnoringVisibility`, `systemBarsIgnoringVisibility`, `navigationBarsIgnoringVisibility`, `captionBarIgnoringVisibility`, `tappableElementIgnoringVisibility`) return the inset **when the bar is shown**, so immersive mode does not zero them. **MEASURED** (same insets page).

---

## 3. The Compose consumption API

**MEASURED**, *Compose: set up window insets*: *"There are three ways to handle insets to adjust your Composable layouts: rulers, padding modifiers, inset size modifiers."* — <https://developer.android.com/develop/ui/compose/system/insets-ui>

### Padding modifiers (each consumes what it applies)

- `Modifier.windowInsetsPadding(insets: WindowInsets)` and the sided overload `windowInsetsPadding(insets, sides: WindowInsetsSides)`.
- Typed helpers, all `actual public fun` in `androidx.compose.foundation.layout` and each the exact equivalent of `windowInsetsPadding` for its type — **MEASURED**, `WindowInsetsPadding.android.kt`: `safeDrawingPadding()`, `safeGesturesPadding()`, `safeContentPadding()`, `systemBarsPadding()`, `displayCutoutPadding()`, `statusBarsPadding()`, `imePadding()`, `navigationBarsPadding()`, `captionBarPadding()`, `waterfallPadding()`, `systemGesturesPadding()`, `mandatorySystemGesturesPadding()`.
- `Modifier.consumeWindowInsets(insets)` / `consumeWindowInsets(paddingValues: PaddingValues)` consumes without applying padding.
- **MEASURED**, *set up window insets*: *"The inset padding modifiers … automatically consume the portion of the insets that are applied as padding. While going deeper into the composition tree, nested inset padding modifiers … avoid using the same portion of the insets more than once."*

### Inset size modifiers (do not consume)

**MEASURED**, same page: `Modifier.windowInsetsStartWidth(...)`, `Modifier.windowInsetsEndWidth(...)`, `Modifier.windowInsetsTopHeight(...)`, `Modifier.windowInsetsBottomHeight(...)` — each sets the component's size to that inset side, *"especially useful for sizing a `Spacer`"*.

### Raw values

`WindowInsets.asPaddingValues()` / `asPaddingValues(density)` returns a `PaddingValues` **"of the insets that are unaffected by consumption"** — i.e. it does **not** consume, so a child `imePadding()` will pad again. **MEASURED**, *set up window insets*.

### Rulers (Compose UI 1.9+)

- `WindowInsetsRulers` (`androidx.compose.ui.layout`) with `…current` and `…maximum` values (`SafeDrawing`, `SafeContent`, `SafeGestures`, `StatusBars`, `NavigationBars`, `Ime`, `CaptionBar`, `SystemBars`, `DisplayCutout`, `Waterfall`), consumed by `Modifier.fitInside(rulers: RectRulers)` and `Modifier.fitOutside(rulers)` (both in `androidx.compose.foundation.layout`), and `RectRulers.innermostOf(...)` / `outermostOf(...)`. **MEASURED** — <https://developer.android.com/develop/ui/compose/system/evaluate-rulers> · <https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/fitInside.modifier> · <https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/RectRulers.Companion>
- **MEASURED**, same page: `fitInside`/`fitOutside` *"only works when `Constraints` have fixed width and fixed height"* — `Modifier.fillMaxSize()` or a size modifier must precede it.

### Recommended default vs footguns

- **RECOMMENDED default (MEASURED, official skill):** what is above the fold is a `Scaffold` whose `innerPadding` is applied to the content (`contentPadding` of a list), and for components outside a Scaffold *"use padding modifiers, such as `Modifier.safeDrawingPadding()` or `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`."* For the IME the skill now **prefers** `Modifier.fitInside(WindowInsetsRulers.Ime.current)` over `imePadding()` "because it reduces jank and extra padding caused by forgetting to consume insets upstream". — <https://developer.android.com/agents/skills/system/edge-to-edge/skill>
- **Footguns** (each produces a real symptom, all **INFERRED** from the subsumption above):
  - `statusBarsPadding()` / `systemBarsPadding()` alone → covers bars but **not** the landscape side cutout. `systemBarsPadding()` also misses the IME.
  - `navigationBarsPadding()` alone → misses the top entirely and misses the side cutout.
  - Hardcoding `Modifier.padding(top = 24.dp)` for a bar height. **MEASURED**, *cutouts*: *"don't hardcode the status bar height, as this can lead to overlapping or cut-off content."* — <https://developer.android.com/develop/ui/compose/system/cutouts>
  - `WindowInsets(0, 0, 0, 0)` passed as a component's `windowInsets`/`contentWindowInsets` → explicitly disables that component's handling. **MEASURED** (documented as the way to disable it) — <https://developer.android.com/develop/ui/compose/system/material-insets>
  - `padding(WindowInsets.safeDrawing.asPaddingValues())` + a child `imePadding()` → IME padded twice, because `asPaddingValues()` does not consume. **MEASURED** as a labelled WRONG example in the official skill.
  - `safeDrawingPadding()` (or similar) on the `NavigationSuiteScaffold` parent → *"This clips and prevents an edge-to-edge screen."* **MEASURED**, official skill.

---

## 4. Material 3 components: what they forgive, and where it stops

**MEASURED**, *Use Material 3 insets* — <https://developer.android.com/develop/ui/compose/system/material-insets>:

| Component | Default inset handling | What that does **not** cover |
|---|---|---|
| `TopAppBar` family | *"Applies the top and horizontal sides of the system bars as padding."* `TopAppBarDefaults.windowInsets`. | IME; the content below it. |
| `BottomAppBar` | *"Applies the bottom and horizontal sides of the system bars as padding."* `BottomAppBarDefaults.windowInsets`. | IME; content above it. |
| `NavigationBar` | *"Applies the bottom and horizontal insets."* `NavigationBarDefaults.windowInsets = systemBarsForVisualComponents.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)`. | IME. |
| `ModalDrawerSheet` / `DismissibleDrawerSheet` / `PermanentDrawerSheet` | *"Applies vertical and start insets to content."* | — |
| `ModalBottomSheet` | *"Applies the bottom insets."* `BottomSheetDefaults.modalWindowInsets`; *"By default, IME padding is handled through this parameter."* | Only the sheet's own content. |
| `NavigationRail` | *"Applies the vertical and start insets."* | — |
| `NavigationSuiteScaffold` (adaptive) | *"manages safe areas for its own components, like the `NavigationRail` or `NavigationBar`."* | **Does not propagate `PaddingValues` to inner content** — *"You MUST apply insets to individual screens or components … DO NOT apply `safeDrawingPadding` or similar modifiers to the `NavigationSuiteScaffold` parent."* |

### What `systemBarsForVisualComponents` actually is — MEASURED (AOSP)

`ScaffoldDefaults.contentWindowInsets`, `TopAppBarDefaults.windowInsets`, `NavigationBarDefaults.windowInsets` and `SearchBarDefaults.windowInsets` are all built from the internal `WindowInsets.systemBarsForVisualComponents`. Its Android definition is:

```kotlin
internal actual val WindowInsets.Companion.systemBarsForVisualComponents: WindowInsets
    @Composable get() = systemBars.union(displayCutout)
```

**MEASURED** — `…/material3/src/androidMain/kotlin/androidx/compose/material3/internal/SystemBarsDefaultInsets.android.kt` — <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/androidMain/kotlin/androidx/compose/material3/internal/SystemBarsDefaultInsets.android.kt>

Two consequences, and they are the whole mechanism of the reported symptoms:

1. **The M3 default contains the display cutout** (`displayCutout` is unioned in), so a `Scaffold`'s default `innerPadding` **does** protect a landscape side cutout, and a `NavigationBar`'s default insets do too. It also **contains no IME** — `safeDrawing` = `systemBarsForVisualComponents ∪ ime`, so `safeDrawingPadding()` is exactly the Scaffold default *plus the keyboard*.
2. **A `Scaffold` applies nothing by itself.** **MEASURED**, *Scaffold* reference: *"window insets to be passed to `content` slot via `PaddingValues` params. Scaffold will take the insets into account from the top/bottom only if the `topBar`/`bottomBar` are not present, as the scaffold expect `topBar`/`bottomBar` to handle insets instead."* and *"`Modifier.consumeWindowInsets(innerPadding)` … to properly offset top and bottom bars. If using `Modifier.verticalScroll`, apply this modifier to the child of the scroll, and not on the scroll itself."* — <https://developer.android.com/reference/kotlin/androidx/compose/material3/Scaffold.composable>

   So a `Scaffold` whose lambda ignores `innerPadding`, or whose content root never receives it, is byte-for-byte as exposed as no `Scaffold` at all. **MEASURED** (same reference): *"By default, `Scaffold` provides insets as parameter `PaddingValues` for you to consume and use. `Scaffold` does not apply the insets to content; this responsibility is yours."*

**INFERRED:** this is why the mistake is invisible in code review. `Scaffold` looks like a fix, `TopAppBar` and `NavigationBar` genuinely are fixes for their own bars, and the one thing that is *not* fixed — the content slot, and the IME — is the exact thing the owner saw.

---

## 5. The three reported symptoms, mechanically

### (a) Content under the status bar / notification panel

**Cause.** At `targetSdk` 35+ the top offset is disabled (**MEASURED**, §1: *"The top offset is disabled so content draws behind the status bar unless insets are applied."*). A screen root that is a `Box`/`Column`/`LazyColumn` with `fillMaxSize()` and **no** `Scaffold` above it and **no** inset modifier on that root draws its first line at y = 0, i.e. under the status bar.

**Exact mistake.** `@Composable fun DetailsScreen() { Column(Modifier.fillMaxSize()) { Text(title) … } }`, or a `Scaffold { … }` whose `innerPadding` is dropped.

**Exact fix.** `Modifier.safeDrawingPadding()` (or `windowInsetsPadding(WindowInsets.safeDrawing)`) on the root, **or** wrap in `Scaffold { inner -> Column(Modifier.padding(inner).consumeWindowInsets(inner)) }`. A `Scaffold` with an unused `innerPadding` is not a fix.

### (b) Content under a notch / punch-hole camera

**Cause.** `displayCutout` is a distinct inset type and is **not** in `systemBars` (§2, MEASURED from the AOSP union). In portrait the top cutout usually sits inside the status bar, so a screen padded with `statusBarsPadding()` *looks* correct. Rotate to landscape and the cutout moves to a vertical edge (**MEASURED**, §2) where `statusBars` contributes nothing on that side — the first character of a title, or a toolbar icon, is behind the camera. This is the failure the factory's owner describes as "text crawls onto the camera".

**Exact mistake.** Padding with `statusBarsPadding()` or `systemBarsPadding()` and treating the cutout as covered — or a full-bleed custom header that draws to the physical edge with no cutout-aware inset at all.

**Exact fix.** `Modifier.safeDrawingPadding()` on the root (safeDrawing contains `displayCutout`), `Modifier.displayCutoutPadding()`, or `windowInsetsPadding(WindowInsets.displayCutout)` on whatever element actually reaches the edge. Do **not** hardcode a status-bar height (**MEASURED**, *cutouts*).

### (c) Content under the bottom gesture bar / 3-button navigation bar

**Cause.** The bottom offset is disabled for both navigation modes (**MEASURED**, §1: *"Bottom offset disabled so content draws behind the system navigation bar unless insets are applied."*). Anything anchored to the bottom of the window — a FAB, a save button, the last row of a list — lands under the pill or under the 3-button bar.

**The list variant, specifically.** A `LazyColumn` with `Modifier.fillMaxSize()` and **no `contentPadding`** can scroll its last item to the very bottom, where the navigation bar covers it; the item exists but cannot be tapped. This is the *"last item is unreachable"* case.

**Exact mistake.** `LazyColumn(Modifier.fillMaxSize()) { items(…){ … } }` — no `contentPadding`.

**Exact fix.** Put the inset into the list's `contentPadding` (so it scrolls *behind* the bar but stops above it), not into a parent `Modifier.padding(...)`. **MEASURED**, official skill: *"Apply inset padding (like `Scaffold`'s `innerPadding`) to the `contentPadding` parameter of scrollable components (e.g., `LazyColumn`, `LazyRow`). DO NOT apply it as a `Modifier.padding()` to the list's parent container, as this clips the content and prevents it from scrolling behind the system bars."* The idiomatic form is `Scaffold { inner -> LazyColumn(Modifier.consumeWindowInsets(inner), contentPadding = inner) { … } }`. A trailing `Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))` inside the scroll is the alternative the Compose guide shows. **MEASURED**.

**Related, MEASURED**, official skill: a bottom bar such as `NavigationBar` handles its own bottom inset, but *"because your bottom bar colors extend to the bottom of the screen"* the app should set `window.isNavigationBarContrastEnforced = false` on SDK 29+ so no translucent scrim is drawn over it.

### The IME is a fourth, separate mechanism

`Scaffold`'s default `contentWindowInsets` **does not contain IME insets** (**MEASURED** — `systemBarsForVisualComponents = systemBars ∪ displayCutout`). Three ways to handle it, all **MEASURED** from the official skill: `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)` (then do not also call `imePadding()`), `Modifier.imePadding()` before `verticalScroll()`, or the now-preferred `Modifier.fitInside(WindowInsetsRulers.Ime.current)`. The manifest must carry `android:windowSoftInputMode="adjustResize"` — **MEASURED**, *Set up Edge-to-edge*: *"Set `android:windowSoftInputMode="adjustResize"` in your Activity's `AndroidManifest.xml` entry. This setting allows your app to receive IME insets."* — <https://developer.android.com/develop/ui/compose/system/setup-e2e>

---

## 6. Testing on a device

- **Simulate a cutout.** **MEASURED**, *Support display cutouts* and *Test how your content renders with cutouts*: *"Enable Developer options. In the Developer options screen, scroll down to the Drawing section and select **Simulate a display with a cutout**. Select the cutout type."* This is available on any device running Android 9 (API 28) or higher and needs no cutout hardware. **Rotate to landscape** — that is where the cutout becomes a side inset. — <https://developer.android.com/develop/ui/views/layout/display-cutout> · <https://developer.android.com/develop/ui/compose/system/test-cutouts>
- **Switch navigation mode.** **MEASURED**, *Get around on your Android device*: **Settings → System → Gesture navigation / Navigation mode** (Pixel: *System → Navigation mode*; others: *System → Gestures → Navigation mode*), then choose **Gesture navigation** or **3-button navigation**. The Settings implementation keys are `system_nav_gestural`, `system_nav_2buttons`, `system_nav_3buttons`. The two modes give different `navigationBars` heights (a ~24 dp pill against a ~48 dp scrimmed bar), so a defect can appear in one and not the other. — <https://support.google.com/android/answer/9079644?hl=en> · <https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/src/com/android/settings/gestures/SystemNavigationGestureSettings.java>
- **Display size / smallest width.** **Settings → Display → Display size** changes the effective density and therefore `dp` insets. **MEASURED** as a warning, not an instruction: extreme **Developer options → Smallest width** values *"can cause a System UI crash loop"* on at least one device (`docs/research/2026-09-23-device-capture-readiness.md` measured this class of device-level breakage). Reach for **Display size** first; a smaller window also exercises the compact width class and any adaptive layout. — ON RECORD (Android Developers blog, *Preview and test your app's edge-to-edge UI*) · <https://medium.com/androiddevelopers/preview-and-test-your-apps-edge-to-edge-ui-da645c905d78>
- **Force edge-to-edge on an older SDK or a non-15 device**, three options — **MEASURED**, same blog: (1) ship a targetSdk-35 build to an Android 15 device/emulator; (2) *"enable the `ENFORCE_EDGE_TO_EDGE` flag in the App Compatibility Change Developer Option on an Android 15 device without having to target SDK 35"*; (3) *"call `enableEdgeToEdge` on each Activity to simulate the Android 15 platform enforcement without having to target SDK 35 and without needing an Android 15 device."*

### Compose preview and test overrides — MEASURED

`DeviceConfigurationOverride` lives in `androidx.compose.ui.test` and is *"only suitable for tests"*. The relevant overrides — <https://developer.android.com/reference/kotlin/androidx/compose/ui/test/DeviceConfigurationOverride.Companion>:

- `DeviceConfigurationOverride.WindowInsets(windowInsets: WindowInsetsCompat)` — set any inset, including a synthetic `displayCutout` or an `ime`, via `WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.…, DpRect(…).toAndroidXInsets()).build()`. This is the only override that fabricates a cutout; there is **no** separate `DisplayCutout` override.
- `DeviceConfigurationOverride.WindowSize(DpSize)` / `ForcedSize(DpSize)` — a forced window size that also overrides `LocalWindowInfo` and `LocalConfiguration` (the two differ exactly in that).
- `DeviceConfigurationOverride.Navigation(navigationType: Int, isHidden: Boolean)` — uses `Configuration.NAVIGATION_*` (e.g. `NAVIGATION_DPAD`), not a gesture/3-button enum.
- Also `FontScale`, `DarkMode`, `LayoutDirection`, `Locales`, `RoundScreen`, `Keyboard`, `Touchscreen`, `UiMode`, `FontWeightAdjustment`, combinable with `then`.
- **ON RECORD** (Android Developers blog): the combined example sets `statusBars` to a 64 dp top inset and `navigationBars` to 24/48/24 dp, and notes the `WindowInsets` override landed in *"Compose 1.8.0-alpha01 ui-test"*.

---

## 7. The statically detectable subset

Scope: only Kotlin text under `app/src/main/`. Nothing below can see pixels; every finding is a **cause**, and a cause without an effect is a false positive the gate must not fire on.

### 7.1 RELIABLY detectable (a missing token; a syntactic absence)

1. **No edge-to-edge opt-in at all.** Predicate: no `enableEdgeToEdge(` **and** no `WindowCompat.setDecorFitsSystemWindows(` with `false` anywhere under `app/src/main/**/*.kt`. False positives: an app that is edge-to-edge only because it targets 35+ — but then a call is still recommended, and the absence of both tokens is the thing the owner complained about. **FP risk: very low.**
2. **`windowSoftInputMode` missing on the Activity while the app has a text field.** Predicate: manifest has an Activity (the launcher one at least) and none declares `android:windowSoftInputMode="adjustResize"`, **and** a `TextField`/`OutlinedTextField`/`BasicTextField` exists. FP: a slot whose only input is inside a Dialog/`ModalBottomSheet`; a slot that sets `SOFT_INPUT_ADJUST_RESIZE` in code. **FP risk: low** (already the mechanism the existing check uses).
3. **No IME handling anywhere while a text field exists.** Predicate: a text field exists and no `imePadding(`, `fitInside(`, `WindowInsets.ime`, `WindowInsetsRulers.Ime`, `safeDrawing`/`safeContent`/`safeGestures`, and no `contentWindowInsets = …safeDrawing|safeContent|safeGestures|WindowInsets.ime`, and no `ModalBottomSheet` that would carry it. FP: a screen whose field is inside a `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)` in another file — so the predicate is **module-wide, not file-local**. **FP risk: low–medium.**
4. **`Scaffold`'s `innerPadding` is never consumed.** Predicate: a `Scaffold { padding -> … }` whose lambda body never references its `PaddingValues` parameter (no `padding(inner`, `consumeWindowInsets(inner`, `contentPadding = inner`, and the identifier is unused). FP: the param renamed and consumed; a deliberately full-bleed screen that insets explicitly. **FP risk: low–medium** — it is a syntactic "binding is unused" test, which parsers do well.
5. **Padding applied to a list's parent instead of its `contentPadding`.** Predicate: a `LazyColumn`/`LazyRow`/`LazyVerticalGrid`/`LazyVerticalStaggeredGrid` with no `contentPadding =` argument, whose direct parent carries `Modifier.padding(`/`windowInsetsPadding(`. **FP risk: medium** — a short list never occludes anything; that is a runtime property a static scan cannot see, so this should be **a note**, not a FAIL.
6. **A component's own inset handling was deliberately switched off.** Predicate: `windowInsets = WindowInsets(0, 0, 0, 0)` or `contentWindowInsets = WindowInsets(0, 0, 0, 0)`. It is a deliberate act, so report it (as a note) rather than fail. **FP risk: low.**

### 7.2 Detectable with caveats (needs whole-subtree / whole-module reasoning)

7. **A screen root with no inset-consumer and no ancestor that supplies one.** Predicate: a `@Composable fun …Screen(…)` / a navigation destination whose root layout modifier chain contains `fillMaxSize()`/`fillMaxWidth()` but **no** `windowInsetsPadding`/`safeDrawingPadding`/`safeContentPadding`/`safeGesturesPadding`/`systemBarsPadding`/`statusBarsPadding`/`navigationBarsPadding`/`imePadding`/`fitInside`, **and** no `Scaffold` in its body, **and** no parent composable in the module (e.g. a `NavDisplay` host, an app-level `Scaffold`) that applies insets to it. **The ancestor half is the hard part** and is why a purely file-local version of this rule fires on correct code. **FP risk: medium–high** unless the rule is scoped to the screen entry points and the `NavDisplay`/`NavHost` host is inspected too.
8. **`statusBars`/`systemBars`/`navigationBars` used as the *only* visible-area inset.** Predicate: one of `statusBarsPadding(`/`systemBarsPadding(`/`windowInsetsPadding(WindowInsets.statusBars|systemBars)` appears in a screen, and the screen (or the module) contains no `displayCutoutPadding(`, `safeDrawingPadding(`, `safeContentPadding(`, `WindowInsets.displayCutout`, `WindowInsets.safeDrawing`, `WindowInsets.safeContent`, `WindowInsetsRulers.SafeDrawing`/`SafeContent`, and no `Scaffold`/`TopAppBar`/`NavigationBar` (whose defaults union the cutout) above it. **This is the landscape-cutout sensor.** **FP risk: medium** — the cutout only bites in landscape, and a device without a cutout never shows it; the code is still wrong on a device that has one. A **note** is defensible; a hard FAIL needs the "no Scaffold/AppBar anywhere" guard.
9. **`imePadding()` combined with an IME-carrying `contentWindowInsets`.** Predicate: same scope has both `contentWindowInsets = WindowInsets.safeDrawing|safeContent|safeGestures|ime` and `imePadding(`. **MEASURED** as a documented WRONG pattern. FP: the modifier is inside a *different* Scaffold. **FP risk: medium** — scope-limited parsing is required.

### 7.3 Cannot be detected statically (state the limit, do not fake it)

- Whether content **actually** collides: a `Text` two lines shorter never reaches the status bar. Insets are runtime, device-, orientation- and content-dependent values.
- Whether the chosen inset set is **sufficient for a specific screen**: `safeDrawingPadding()` at the root is correct and looks identical to `statusBarsPadding()` in a diff until you count the missing type.
- Whether a **cutout exists** on the target device, or the app is ever used in landscape.
- Whether a list actually **overflows** the window (the last-item-unreachable case) — a function of item count, heights and font scale.
- Whether a bottom-anchored element is a FAB that M3 would have handled inside a Scaffold, or a hand-placed `Box(Modifier.align(BottomCenter))` outside it — the same modifier text in two different trees.
- Whether a `Dialog` is full-screen (`usePlatformDefaultWidth = false` **and** `fillMaxSize()`) and therefore needs `DialogProperties(decorFitsSystemWindows = false)` — detectable as tokens, not as a verdict.

### 7.4 Ranked predicates by reliability (highest first)

| # | Predicate (Kotlin tokens under `app/src/main/`) | Detectability | False-positive risk |
|---|---|---|---|
| 1 | no `enableEdgeToEdge(` **and** no `WindowCompat.setDecorFitsSystemWindows(…, false)` | certain | very low |
| 2 | text field present **and** manifest has no `windowSoftInputMode="adjustResize"` | certain | low |
| 3 | `Scaffold` lambda never consumes its `innerPadding` | certain (binding-unused) | low–medium |
| 4 | text field present **and** no IME token anywhere in the module | high | low–medium |
| 5 | `windowInsets = WindowInsets(0, 0, 0, 0)` on an M3 bar | certain | low (report, not fail) |
| 6 | screen root has `fillMaxSize()` and no inset modifier, **and** no `Scaffold` in its body **and** no inset-applying ancestor in the module | medium–high | medium–high |
| 7 | only `statusBars`/`systemBars`/`navigationBars` used, no cutout-aware token and no M3 bar/Scaffold above | medium | medium (landscape-only, device-dependent) |
| 8 | `imePadding()` + an IME-carrying `contentWindowInsets` in one scope | medium | medium |
| 9 | scrollable with no `contentPadding` whose parent carries the padding | medium | medium–high (short lists) |

A predicate that fires on correct code is worse than one that does not exist: a noisy gate is read past, and the defect it names survives anyway. Predicates 6–9 should be **notes with `file:line`**, and only 1–5 are candidates for a hard FAIL.

---

## Sources

Every URL fetched or returned with content for this document:

- <https://developer.android.com/about/versions/15/behavior-changes-15> — edge-to-edge enforcement, the bar-by-bar change list, the cutout-mode change, deprecated/disabled APIs, `Configuration` change.
- <https://developer.android.com/about/versions/16/behavior-changes-16> — the opt-out deprecation and its device split.
- <https://developer.android.com/about/versions/15/reference/compat-framework-changes> — `ENFORCE_EDGE_TO_EDGE`, change ID 309578419.
- <https://developer.android.com/develop/ui/views/layout/edge-to-edge> — the platform sentence, `enableEdgeToEdge()` defaults, landscape cutout sentence.
- <https://developer.android.com/develop/ui/views/layout/display-cutout> — cutout modes, "Simulate a display with a cutout", no hardcoded heights.
- <https://developer.android.com/develop/ui/compose/system/insets> — the full inset vocabulary, the three safe sets, targetSdk 35 drawing rule.
- <https://developer.android.com/develop/ui/compose/system/insets-ui> — padding/size modifiers, consumption, `asPaddingValues`, the three approaches.
- <https://developer.android.com/develop/ui/compose/system/material-insets> — per-component inset handling, Scaffold does not apply insets, `WindowInsets(0,0,0,0)`.
- <https://developer.android.com/develop/ui/compose/system/setup-e2e> — `adjustResize`, `enableEdgeToEdge`, contrast enforcement.
- <https://developer.android.com/develop/ui/compose/system/cutouts> — recommended cutout handling, no hardcoded status bar height.
- <https://developer.android.com/develop/ui/compose/system/test-cutouts> — the Developer Options path.
- <https://developer.android.com/develop/ui/compose/system/evaluate-rulers> — `fitInside`/`fitOutside`, `WindowInsetsRulers`, fixed-constraint requirement.
- <https://developer.android.com/agents/skills/system/edge-to-edge/skill> — the official migration skill: preferred/forbidden combinations, adaptive scaffolds, lists, dialogs, `isNavigationBarContrastEnforced`.
- <https://developer.android.com/reference/kotlin/androidx/activity/package-summary> · <https://developer.android.com/reference/androidx/activity/EdgeToEdge> · <https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity> — `enableEdgeToEdge` signature, package, artifact.
- <https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/WindowInsets> — `union`/`exclude`/`add`/`only`, `asPaddingValues`.
- <https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/WindowInsetsSides> — the side constants.
- <https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/fitInside.modifier> · <https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/WindowInsetsRulers> · <https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/RectRulers.Companion> — ruler API.
- <https://developer.android.com/reference/kotlin/androidx/compose/material3/Scaffold.composable> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/ScaffoldDefaults> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/TopAppBarDefaults> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/BottomAppBarDefaults> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/BottomSheetDefaults> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/ModalBottomSheet.composable> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/navigationsuite/NavigationSuiteScaffoldDefaults> — component defaults.
- <https://developer.android.com/reference/kotlin/androidx/compose/ui/test/DeviceConfigurationOverride.Companion> · <https://developer.android.com/reference/kotlin/androidx/compose/ui/test/DeviceConfigurationOverride> — preview/test overrides.
- <https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type> · <https://developer.android.com/reference/android/view/WindowInsets> — platform type definitions for `tappableElement`, `mandatorySystemGestures`, `captionBar`.
- <https://android.googlesource.com/platform/frameworks/support/+/4a82be8eaba0cb03d9ea4fe58bc99da7853a4237/compose/foundation/foundation-layout/src/androidMain/kotlin/androidx/compose/foundation/layout/WindowInsets.android.kt> — the `safeDrawing`/`safeGestures`/`safeContent` union definitions and the `adjustResize` + `setDecorFitsSystemWindows` comment.
- <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/foundation/foundation-layout/src/androidMain/kotlin/androidx/compose/foundation/layout/WindowInsetsPadding.android.kt> — every typed padding modifier name.
- <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/androidMain/kotlin/androidx/compose/material3/internal/SystemBarsDefaultInsets.android.kt> — `systemBarsForVisualComponents = systemBars.union(displayCutout)`.
- <https://android.googlesource.com/platform/frameworks/support/+/83040cf151db157d7cc1e50588ae10b0b5a12369/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/NavigationBar.kt> — `NavigationBarDefaults.windowInsets`.
- <https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/src/com/android/settings/gestures/SystemNavigationGestureSettings.java> — navigation-mode keys.
- <https://support.google.com/android/answer/9079644?hl=en> — the Settings path for navigation mode.
- <https://medium.com/androiddevelopers/preview-and-test-your-apps-edge-to-edge-ui-da645c905d78> — **ON RECORD**: the three ways to force enforcement, the `WindowInsets` override, the synthetic-inset example.
- <https://www.androidpolice.com/android-16-full-screen-edge-to-edge-no-opt-out/> — **ON RECORD**: the Android 16 opt-out removal.

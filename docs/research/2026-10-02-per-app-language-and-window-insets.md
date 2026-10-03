# Per-app language and window insets — the platform truth behind two owner-reported defects

**Measured 2026-10-02.** Labels: **MEASURED** (read at the URL given), **ON RECORD** (a named
secondary source, not reproduced here), **INFERRED** (this document's reasoning), **UNVERIFIED**
(could not confirm). Quotes are verbatim; where a page is paraphrased the label says so.

Target: Kotlin + Compose, `minSdk 26`, `targetSdk 36`, Compose BOM `2026.09.00` (Material 3 `1.4.0`),
AGP 9 with built-in Kotlin. External facts only; the factory statements found to be wrong are named
at the end, as required.

## What this measures

Two owner-reported defects, each traced to platform behaviour rather than taste:

- **Part A** — the Settings Language row opens the phone's system Settings app; the owner's point is
  that in-app language settings must change *this app's* language inside the app. What
  `AppCompatDelegate.setApplicationLocales`, framework `LocaleManager` and `android:localeConfig`
  actually do, and whether they conflict.
- **Part B** — the bottom bar ignores the system navigation buttons and the top bar ignores the
  status bar / camera cutout, while a factory sensor PASSES those screens. What each Material 3
  default covers, and where coverage stops.

# PART A — per-app language

## A1. What `AppCompatDelegate.setApplicationLocales` changes

- **MEASURED**, `AppCompatDelegate` reference: **"Sets the current locales for the calling app."**
  — <https://developer.android.com/reference/kotlin/androidx/appcompat/app/AppCompatDelegate>
- **MEASURED**, *Per-app language preferences*: the public APIs *"let apps set a different language
  from the system language at runtime."* — <https://developer.android.com/guide/topics/resources/app-languages>
- Scope is the app, not the device. No page states verbatim "this never changes the system locale";
  *"never the system locale"* is **INFERRED** from *"for the calling app"* + *"different language
  from the system language at runtime"* — an app cannot change the phone-wide locale through an
  app-scoped API. **MEASURED**, same reference: *"Pass a `getEmptyLocaleList` to reset to the system
  locale."*, and *"This API should always be called after Activity.onCreate()."*

## A2. `AppCompatActivity` requirement, and the manifest `<service>`

- **MEASURED**, *Per-app language preferences*: *"For backward compatibility with Android 12 (API
  level 32) and lower, we strongly recommend using the AndroidX support library (`AppCompatDelegate`)
  when implementing an in-app language picker. If you use this approach, the activity hosting your
  Compose UI must extend `AppCompatActivity`."*
- **MEASURED**, same page: *"Caution: If you're using Compose with `setApplicationLocales`, you must
  extend your activity from `AppCompatActivity`. Otherwise, setting the app locale won't work."*
- **MEASURED**, same page: backward-compatible APIs need **"Appcompat 1.6.0 or higher."**, and they
  *"work with the `AppCompatActivity` context, not the application context, for Android 12 (API level
  32) and earlier."*
- **MEASURED**, AppCompatDelegate reference — the exact explicit entry for storage on API < 33:

```xml
<service
    android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
    android:enabled="false"
    android:exported="false">
    <meta-data
        android:name="autoStoreLocales"
        android:value="true" />
</service>
```

  Same reference: *"this will cause a blocking diskRead and diskWrite strictMode violation"*; the
  alternative is custom storage supplied **before** `onCreate`, *"gate this to API versions <33"*.

## A3. Does an in-app picker CONFLICT with `android:localeConfig`? — **FALSE, it SYNCs**

- **MEASURED**, *Per-app language preferences*: **"These APIs automatically sync with system
  settings; therefore, apps that use these APIs to create custom in-app language pickers will ensure
  their users have a consistent user experience regardless of where they select their language
  preferences."**
- **MEASURED**, same page: **"If you use the public APIs for your in-app language picker, the
  device's system settings are automatically updated to match whichever language the user selects
  through your in-app experience."**
- **MEASURED**, AppCompatDelegate reference: **"On API level 33 and above, this API will handle
  storage automatically."** The guide adds that an app upgrading from < 33 with `autoStoreLocales`
  opted in has its locales **"automatically synced to the framework."**
- On API 33+ the framework store is `LocaleManager`, so an in-app picker and the system picker read
  and write one value; below 33 AndroidX owns the storage and the system has no per-app picker at
  all. AppCompat *delegating* to `LocaleManager` is not spelled out verbatim — the **sync** is
  MEASURED, the delegation is **INFERRED** from the two quotes above.
- **Conclusion (INFERRED from the two quoted sentences):** an in-app picker built on these APIs is
  **not** a second source of truth; it is the same source reached from a second place. The factory's
  prohibition is **FALSE**. One real caveat, **MEASURED**: `createConfigurationContext` /
  `applyOverrideConfiguration` is a *different* mechanism that bypasses the shared store; the
  prohibition is right for that plumbing and wrong for the published APIs.

## A4. `LocaleManager` / `applicationLocales` (framework API, API 33+)

- **MEASURED**, *Per-app language preferences*: *"Android 13 introduces the following features…"* and
  *"you can also use the `setApplicationLocales` and `getApplicationLocales` methods in the Android
  framework for devices running Android 13."* The guide's own example is annotated
  `@RequiresApi(Build.VERSION_CODES.TIRAMISU)`:

```kotlin
val localeManager = context.getSystemService(LocaleManager::class.java)
localeManager.applicationLocales = LocaleList(Locale.forLanguageTag(languageTag))   // set (API 33+)
val currentLocales = localeManager.applicationLocales                               // get (API 33+)
```

- Scope: the same per-app store (*"for the calling app"* on the AndroidX facade; shared identity
  with AppCompat is **INFERRED** from A3).
- **Below API 33 (minSdk 26):** `LocaleManager` does not exist, there is no platform per-app language
  concept, and — **MEASURED** (A2) — the AndroidX replacement *"work[s] with the `AppCompatActivity`
  context, not the application context, for Android 12 (API level 32) and earlier."* An unguarded
  `LocaleManager` write is a no-op / crash risk, which is why the guide's example carries that
  annotation.

## A5. How an app enumerates the languages it can offer

| Need | Exact call | Label |
|---|---|---|
| Current selection (all APIs) | `AppCompatDelegate.getApplicationLocales()` → `LocaleListCompat` (*"Returns a `getEmptyLocaleList` if no app-specific locales are set."*) | MEASURED (reference) |
| Current selection (API 33+) | `getSystemService(LocaleManager::class.java).getApplicationLocales(packageName)` → `LocaleList` | MEASURED (guide) |
| Declared set (source of truth) | `android:localeConfig="@xml/locale_config"` or AGP-generated `LocaleConfig`; per-locale `values-<locale>/` dirs | MEASURED (guide) |
| Override offered set (API 33+) | `localeManager.overrideLocaleConfig = LocaleConfig(...)`; read back `overrideLocaleConfig` → `supportedLocales()` | MEASURED (guide) |
| Resolved runtime config | `resources.configuration.locales` | UNVERIFIED — not stated as the recommended path |
| Read the *declared* `localeConfig` at runtime | none found | **UNVERIFIED** — no documented API returns the manifest's declared list; the app keeps its own (its `values-*` set) |

**MEASURED**: with `androidResources { generateLocaleConfig = true }` and `compileSdkVersion` 33+,
*"AGP generates the `LocaleConfig` file and adds a reference to it in the final manifest file"*, and
*"the build fails if you do manually create a `LocaleConfig` file."* Omitting `android:localeConfig`
*"signals that users shouldn't be able to set your app's language independent of their system
language within their system settings."*

## A6. Published guidance on providing an in-app picker

- **MEASURED**, *Per-app language preferences*, the use-case table: *"Your app doesn't have an in-app
  language picker — Enable automatic per-app language support… **Optionally, if you want to add an
  in-app language picker: implement one in Compose or use the AndroidX library and opt in to our API
  implementation to support backward compatibility through `autoStoreLocales`.**"*
- **MEASURED**, same page: *"Your app already has an in-app language picker… **Migrate your app's
  custom logic to use Compose or the public APIs to ensure users see a consistent experience.**"* and
  *"For apps that already have an in-app language picker or want to use one, use the public APIs
  instead of custom app logic."*
- The guidance **permits and describes** an in-app picker and does **not require** one (enabling
  `localeConfig` for the system-settings list is the only baseline step). It does **not** call the
  system picker preferred, and does not say an in-app picker should be avoided. **INFERRED**.

## A7. Recommended recipe for a Compose-only app on minSdk 26

1. **Dependency:** `androidx.appcompat:appcompat` **1.6.0+** (for `AppCompatDelegate`). **MEASURED**.
2. **Activity base class:** `AppCompatActivity`, not `ComponentActivity` — *"you must extend your
   activity from `AppCompatActivity`. Otherwise, setting the app locale won't work."* (A real cost for
   a pure-Compose app: the scaffold's `ComponentActivity` must change, or the picker must be
   framework-only on API 33+.) **MEASURED**.
3. **Read / write:** `AppCompatDelegate.getApplicationLocales()` and
   `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))` on the main
   thread, after `onCreate`; reset with `LocaleListCompat.getEmptyLocaleList()`. **MEASURED**.
4. **Manifest, for API < 33 storage:** `AppLocalesMetadataHolderService` + `autoStoreLocales=true`
   (A2 XML), or custom storage before `onCreate`. **MEASURED**.
5. **Declared set:** `android:localeConfig`, or AGP `generateLocaleConfig` + `resources.properties`.
   **MEASURED**.
6. **API 26–32 vs 33+:** on 26–32 AndroidX owns the storage and there is no system per-app picker; on
   33+ the platform owns it, `setApplicationLocales` *"will handle storage automatically"*, and the
   in-app and system pickers stay in sync. **MEASURED**.

# PART B — window insets

## B1. `WindowInsets.systemBars` constituents — and `displayCutout`

- **MEASURED**, *About window insets*: **"`WindowInsets.systemBars` — The union of the system bar
  insets, which include the status bars, navigation bars, and caption bar."** Members: `statusBars`,
  `navigationBars`, `captionBar` (and their `…IgnoringVisibility` variants).
  — <https://developer.android.com/develop/ui/compose/system/insets>
- **`displayCutout` is NOT in that list.** **MEASURED**, same page: **"`WindowInsets.displayCutout`
  — The insets representing the amount of spacing needed to avoid overlapping with a display cutout
  (notch or pinhole)."** So **`systemBars` does NOT include `displayCutout`.**
- **MEASURED**, AOSP `WindowInsets.android.kt`: `val safeDrawing = systemBars.union(ime).union(displayCutout)`.
  So **`safeDrawing = systemBars ∪ ime ∪ displayCutout`**. —
  <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/foundation/foundation-layout/src/androidMain/kotlin/androidx/compose/foundation/layout/WindowInsets.android.kt>

## B2. `TopAppBarDefaults.windowInsets` — the exact default

- **MEASURED**, AOSP `AppBar.kt`:

```kotlin
public val windowInsets: WindowInsets
    @Composable get() = WindowInsets.systemBarsForVisualComponents.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
    )
```

  — <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt>
- **MEASURED**, AOSP `SystemBarsDefaultInsets.android.kt`:

```kotlin
internal actual val WindowInsets.Companion.systemBarsForVisualComponents: WindowInsets
    @Composable get() = systemBars.union(displayCutout)
```

  — <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/androidMain/kotlin/androidx/compose/material3/internal/SystemBarsDefaultInsets.android.kt>
- **Handles the status bar? — YES.** `systemBarsForVisualComponents` includes `statusBars` and
  `.only(Horizontal + Top)` keeps its top side. **MEASURED** (the two expressions).
- **Handles a landscape left-edge cutout? — YES.** `displayCutout` is unioned in and
  `.only(Horizontal + Top)` keeps its Horizontal (Left + Right) sides, where a landscape side cutout
  lives. **INFERRED** from the two MEASURED expressions; prose agrees — **MEASURED**, *Use Material 3
  insets*: *"`TopAppBar` …: Applies the top and horizontal sides of the system bars as padding…"* —
  <https://developer.android.com/develop/ui/compose/system/material-insets>

## B3. `ScaffoldDefaults.contentWindowInsets` — the exact default

- **MEASURED**, AOSP `Scaffold.kt`:
  `public val contentWindowInsets: WindowInsets @Composable get() = WindowInsets.systemBarsForVisualComponents`.
  — <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Scaffold.kt>
- **Includes the IME? — NO**: `systemBarsForVisualComponents = systemBars ∪ displayCutout`, no `ime`
  (B1/B2). **Includes the cutout? — YES**, via the same union. **MEASURED**. The reference page gives
  only the type — *"Default insets to be used and consumed by the scaffold content slot"* —
  <https://developer.android.com/reference/kotlin/androidx/compose/material3/ScaffoldDefaults>.

## B4. `NavigationBarDefaults.windowInsets`

- **MEASURED**, AOSP `NavigationBar.kt`:

```kotlin
public val windowInsets: WindowInsets
    @Composable get() = WindowInsets.systemBarsForVisualComponents.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    )
```

  — <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/NavigationBar.kt>
- **Clears the system navigation buttons? — YES**: `systemBarsForVisualComponents` contains
  `navigationBars` and `.only(Horizontal + Bottom)` keeps its bottom side (and the cutout's horizontal
  sides). **MEASURED**; corroborated **MEASURED**, *Use Material 3 insets*: *"`NavigationBar`: Applies
  the bottom and horizontal insets."* That page does **not** give `NavigationRailDefaults`' expression,
  so a rail's exact default is **UNVERIFIED** (the page only says *"`NavigationRail`: Applies the
  vertical and start insets."*). `BottomAppBarDefaults.windowInsets` has the **same** expression as
  `NavigationBarDefaults`, **MEASURED**, `AppBar.kt`.

## B5. The correct modern pattern, per surface

- **Top app bar:** a Material `TopAppBar` (or variant) in `Scaffold(topBar = { … })` applies its own
  `Horizontal + Top` insets (B2). **Do not** add `statusBarsPadding()` / `safeDrawingPadding()` —
  that double-pads (**MEASURED**, *Use Material 3 insets*: *"Avoid using additional inset handling
  approaches like rulers, padding modifiers, or inset size modifiers if using `Scaffold`"*).
- **Bottom navigation bar / tab row:** `NavigationBar` in `Scaffold(bottomBar = { … })` applies its
  own `Horizontal + Bottom` insets (B4); do not add `navigationBarsPadding()` on top. A hand-written
  bottom row that is **not** a Material bar must consume the inset itself:
  `Modifier.navigationBarsPadding()` (i.e. `windowInsetsPadding(WindowInsets.navigationBars)`) or
  `windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))`.
- **Content that must clear the IME:** the Scaffold default carries no `ime` (B3). Options:
  `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)` (then do **not** also `imePadding()`),
  `Modifier.imePadding()`, or `Modifier.fitInside(WindowInsetsRulers.Ime.current)`; plus the manifest
  must declare `android:windowSoftInputMode="adjustResize"` — **MEASURED**, *Set up Edge-to-edge*:
  *"Set `android:windowSoftInputMode=\"adjustResize\"` in your Activity's `AndroidManifest.xml`
  entry. This setting allows your app to receive IME insets…"* —
  <https://developer.android.com/develop/ui/compose/system/setup-e2e>.
- **`safeDrawing` vs `systemBars`:** `safeDrawing` is the union that adds **`ime` and `displayCutout`**
  to `systemBars` (B1). Use it when the element must clear the keyboard or a cutout `systemBars` does
  not carry; use bare `systemBars`/`statusBars`/`navigationBars` only where cutout and keyboard are
  genuinely irrelevant. The identity **`safeDrawing = systemBars ∪ ime ∪ displayCutout`** is
  **MEASURED** from the AOSP union; the public page only says `safeDrawing` *"protect[s] content that
  shouldn't be drawn underneath any system UI"* — **MEASURED**.

## B6. `enableEdgeToEdge()` — on which API levels

- **MEASURED**, *Set up Edge-to-edge*: *"To enable edge-to-edge on **previous Android versions**,
  call `enableEdgeToEdge()` in your `Activity.onCreate()` method."* On Android 15+ (targetSdk 35+) the
  platform enforces it regardless — **MEASURED**, *About window insets*: *"On Android 15 (API level
  35) and higher, your app draws underneath the system bars and display cutouts once your app targets
  SDK 35."*
- **Older platforms:** **MEASURED**, `EdgeToEdge` reference: *"The default style configures the system
  bars with a transparent background when contrast can be enforced by the system (API 29 or above). On
  older platforms (which only have 3-button/2-button navigation modes), an equivalent scrim is applied
  to ensure contrast with the system bars."* **MEASURED**, same reference: `EdgeToEdge.enable(...)` is
  **deprecated** — *"Use `WindowCompat#enableEdgeToEdge(Window)`"; which form the pinned Activity
  version exposes as non-deprecated is **UNVERIFIED** (release notes not fetched).

## B7. Predictive back / Navigation 3 at targetSdk 36

- **MEASURED**, *Add support for the predictive back gesture*: `android:enableOnBackInvokedCallback`
  on `<application>` (overridable per `<activity>`) is **the switch**, and `false` is the documented
  opt-out: *"To opt out, in `AndroidManifest.xml`, in the `<application>` tag, set the
  `android:enableOnBackInvokedCallback` flag to `false`."* `true` is the explicit opt-in. —
  <https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture>
- **MEASURED**, same page: *"Starting with Android 15, the developer option for predictive back
  animations is no longer available. System animations such as back-to-home, cross-task, and
  cross-activity now appear for apps that have **opted in** to the predictive back gesture either
  entirely or at an activity level."* and, under *"Update an app that uses default back navigation"*,
  *"Predictive back is enabled by default."*
- **MEASURED**, same page: predictive back *"requires updating your app, using the backward compatible
  `OnBackPressedCallback` in AndroidX Activity 1.6.0 or higher API, or using the new
  `OnBackInvokedCallback` platform API."*; a `PRIORITY_DEFAULT`/`PRIORITY_OVERLAY` callback
  **suppresses the animation** — *"the predictive back animations don't run… Don't create these
  callbacks to run business logic or to log."* On **Android 16+** the non-consuming observer is
  `OnBackInvokedCallback` with `PRIORITY_SYSTEM_NAVIGATION_OBSERVER`.
- **Is `enableOnBackInvokedCallback` still the right switch? — YES**, per the page; `false` disables
  the animation and no replacement flag is listed. A Navigation-3-specific predictive-back page was not
  fetched, so `NavDisplay`'s own transition behaviour is **UNVERIFIED**.

## B8. The Scaffold trap — does Scaffold inset its `topBar`/`bottomBar`?

- **MEASURED**, AOSP `Scaffold.kt` KDoc: *"`contentWindowInsets` window insets to be passed to
  `content` slot via `PaddingValues` params. **Scaffold will take the insets into account from the
  top/bottom only if the `topBar`/`bottomBar` are not present, as the scaffold expect
  `topBar`/`bottomBar` to handle insets instead.**"* — same `Scaffold.kt` URL as B3.
- **MEASURED**, *Use Material 3 insets*: *"By default, `Scaffold` provides insets as parameter
  `PaddingValues` for you to consume and use. **`Scaffold` does not apply the insets to content; this
  responsibility is yours.**"*
- **Answer (under- vs over-padded):** with a `topBar`/`bottomBar` present, `Scaffold` contributes that
  bar's **measured height** to `innerPadding` instead of the raw inset, so the bar must consume its
  own inset (M3 bars do by default — B2/B4). **Not applying `innerPadding` to the content root is
  under-padding** (*"this responsibility is yours"*); **applying `innerPadding` *and* extra inset
  modifiers is over-padding** — **MEASURED**, *Use Material 3 insets*: *"…Avoid using additional inset
  handling approaches like rulers, padding modifiers, or inset size modifiers if using `Scaffold` to
  avoid applying too much padding to your UI."*

# What the factory's current rule gets wrong

Quoted from `.cursor/rules/app-settings.mdc` (mirrored in `docs/ui-ux-standards.md`, read by
`scripts/core/settings.mjs`). Only the incorrect statements are named.

1. **"An in-app picker is accepted only when it is built on the published APIs and there is no
   declared `localeConfig`."** — **WRONG.** An in-app picker built on the published APIs uses the
   same storage as `localeConfig` and **"automatically sync[s] with system settings"** (A3).
   `localeConfig` is not a reason to refuse an in-app picker.
2. **"A second in-app language picker while `android:localeConfig` is declared. It would be a second
   source of truth for one value…"** — **WRONG.** There is one source of truth (the per-app locale
   store); the two pickers are two views of it, and the system settings *"are automatically updated to
   match whichever language the user selects"* in-app (A3).
3. **The general rejection of an in-app picker, leaving `Settings.ACTION_APP_LOCALE_SETTINGS` /
   `ACTION_APPLICATION_DETAILS_SETTINGS` as the required entry point.** — This is the owner's
   complaint's mechanism. The guide presents an in-app picker as a supported option (*"Optionally, if
   you want to add an in-app language picker…"*, A6) and gives the recipe (A7). Opening system
   Settings is a valid *additional* route, not the only one.
4. **"Never reimplement the locale plumbing (`createConfigurationContext` /
   `applyOverrideConfiguration`) beside a declared `localeConfig`: two writers for one language…"** —
   **correct for hand-rolled plumbing**, but stated as a blanket ban it also covers the published
   APIs, which is the error in (1)/(2). **MEASURED** (A3).

The rule is **right** that an `AppCompatActivity` host is required (A2) and that an **unguarded**
`LocaleManager` write is a no-op / crash risk below API 33 (A4). On insets, `uiux.insets` rests on the
same facts measured here (a `Scaffold` does not apply its insets, B8; an M3 bar's default unions the
cutout in, B2/B4); the "PASS" on a still-colliding bar is a sensor-coverage question this document
does not judge.

# Sources

Every URL fetched for this document (2026-10-02):

- <https://developer.android.com/guide/topics/resources/app-languages> — API↔system-settings sync, `AppCompatActivity` requirement, `autoStoreLocales`, `LocaleManager` example, `localeConfig`/AGP generation, use-case table.
- <https://developer.android.com/reference/kotlin/androidx/appcompat/app/AppCompatDelegate> — `set`/`getApplicationLocales` scope, storage, exact `<service>` XML.
- <https://developer.android.com/develop/ui/compose/system/insets> — `systemBars`/`displayCutout` definitions, the three "safe" sets, targetSdk 35 rule.
- <https://developer.android.com/develop/ui/compose/system/material-insets> — per-component handling, "Scaffold does not apply the insets to content", the double-padding warning.
- <https://developer.android.com/develop/ui/compose/system/setup-e2e> — `enableEdgeToEdge()`, `windowSoftInputMode="adjustResize"`.
- <https://developer.android.com/reference/kotlin/androidx/compose/material3/ScaffoldDefaults> — `contentWindowInsets` description.
- <https://developer.android.com/reference/androidx/activity/EdgeToEdge> — `EdgeToEdge.enable` deprecation and the default-style/scrim sentence.
- <https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture> — opt-in/opt-out, `enableOnBackInvokedCallback`, priority guidance, Android 16 observer.
- <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt> — `TopAppBarDefaults.windowInsets`, `BottomAppBarDefaults.windowInsets`.
- <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/NavigationBar.kt> — `NavigationBarDefaults.windowInsets`.
- <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Scaffold.kt> — `ScaffoldDefaults.contentWindowInsets`, the `contentWindowInsets` KDoc.
- <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/androidMain/kotlin/androidx/compose/material3/internal/SystemBarsDefaultInsets.android.kt> — `systemBarsForVisualComponents = systemBars.union(displayCutout)`.
- <https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/foundation/foundation-layout/src/androidMain/kotlin/androidx/compose/foundation/layout/WindowInsets.android.kt> — `safeDrawing = systemBars ∪ ime ∪ displayCutout`.

**UNVERIFIED, stated rather than filled:** `NavigationRailDefaults.windowInsets`'s exact expression;
whether the pinned Activity version's non-deprecated entry point is `enableEdgeToEdge()` or
`WindowCompat.enableEdgeToEdge(window)`; a documented API returning the *declared* `localeConfig` at
runtime; "`setApplicationLocales` never changes the system locale" as a verbatim sentence;
Navigation-3-specific predictive-back behaviour. **ON RECORD:** none — every claim above is MEASURED
or INFERRED.

# Design: app-laneset

**Status of this document.** It describes the intended design of the slot and, separately and
explicitly, **what exists on disk today**. Those two things are not the same and are never blended:
everything not yet built is marked **not-yet-built** and written as a requirement, never in the
present tense. Every claim about the repository is **MEASURED** with a `path:line`; every claim about
the market or the contracts is **ON RECORD** from the file named; storefront readings are **ON RECORD**
via `docs/research/2026-10-02-planners-and-optimisers-evidence.md`. Nothing here claims the app builds
or that any test passes — that is **UNVERIFIED** and is decided by
`node scripts/run-slot-tests.mjs --slot laneset --rerun` and
`node scripts/verify.mjs --slot laneset --build`.

## Product boundary

`laneset` is an offline swim set **builder**: an app whose product is a computation it performs from
the swimmer's own constraints, and whose output leaves the phone as a set list. It is not a session
log, not a stopwatch, not a stroke-lesson course, not a video library and not a challenge board
(`docs/ideation/2026-10-02-planners-and-optimisers.md` §3; the non-goals in `proposal.md`).

The recorded product decision that shapes this design: the measured complaints against the incumbents
are about **repetitive generated sets** and an **unguided pacing test** plus a **recurring paywall**
(**ON RECORD**, `docs/research/2026-10-02-planners-and-optimisers-evidence.md` §3.4). So the three
`core` promises of `differentiators.json` are exactly those three things, and this design is traceable
to them one clause at a time:

| `differentiators.json` promise | weight | `jobStage` | `depthFamily` | this design's section |
|---|---|---|---|---|
| `computes-the-session` | core | during | `core_operation` | *The generator* |
| `pace-test-drives-every-target` | core | before | `fit` | *Pace arithmetic*, *Pace test flow* |
| `takes-it-to-the-pool` | core | after | `data_out` | *Export / share / print handoff* |
| `no-subscription-one-unlock` | supporting | after | `trust` | *Play Billing* |
| `open-a-coach-workout` | supporting | before | `data_in` | *Coach-workout parser* |
| `offline-by-construction` | supporting | during | `trust` | *Privacy and offline* |
| `session-survives-interruption` | supporting | during | `durability` | *Persistence* |

No design decision below may contradict a `core` promise; where one would, the design gives way, not
the promise (`.cursor/rules/differentiator-contract.mdc`).

## What exists today, and what this design is for

MEASURED, by reading the slot tree in this session:

- **The contracts and the scaffold exist.** `economics.json`, `differentiators.json` and `naming.json`
  are present; the slot is registered as `scaffolded` with 7 promises, 3 core
  (`registry/apps.json:63-72`).
- **The domain layer exists, fully.** `domain/Session.kt`, `domain/PaceModel.kt`,
  `domain/SetGenerator.kt` and `domain/CoachSetParser.kt` are written, with JVM unit tests under
  `app/src/test/java/com/gexetr/laneset/domain/` (`SetGeneratorTest.kt`, `PaceModelTest.kt`,
  `CoachSetParserTest.kt`).
- **The data layer exists.** `data/SettingsStore.kt`, `data/PaceStore.kt` and `data/SessionStore.kt`
  are written, including the versioned in-progress-session codec.
- **The icon set is vendored.** 13 Material Symbols `VectorDrawable`s with a provenance record and a
  committed licence (`icons/provenance.json`, `icons/LICENSES/Apache-2.0.txt`).
- **The UI is still the scaffold.** `ui/home/HomeScreen.kt` renders the starter ("Try the starter
  action", `home_primary_action` at `res/values/strings.xml:9`) and every user-facing string in
  `res/values/strings.xml` is a scaffold string ("A focused starting point, ready to become your app.",
  `res/values/strings.xml:6`). **There is no builder screen, no pace-test screen, no session screen,
  no import screen and no unlock screen.**
- **There is no billing code.** The build declares the billing libraries and the `BILLING` permission
  but no `BillingClient` call, entitlement store or paywall exists yet
  (`app/build.gradle.kts:140-146`, `app/src/main/AndroidManifest.xml:9`).
- **There is no share or print code.** No `ACTION_SEND`, `ACTION_CREATE_DOCUMENT`, `PdfDocument` or
  `FileProvider` anywhere in `app/src/main/` (the core promise `takes-it-to-the-pool` is unbuilt).
- **There is no `store/studio.json` and no `capabilities.json`.** Both are required by
  `.cursor/rules/visual-identity.mdc` and the economics contract respectively; neither is on disk yet
  (MEASURED — `Test-Path store/studio.json` and `Test-Path capabilities.json` both returned `False`).
- **No build has been run.** There is no `app/build/` output and no `device-verification.json`
  (MEASURED — neither is on disk).

So this document is a **specification of the slot in flight**, written to be the consolidated
specification an implementation worker can be held to. The domain and data layers are real; the
screens, the billing surface, the handoff and the identity are requirements.

## Architecture and layering

- **One Gradle module, `:app`, plus the `:baselineprofile` test module** (`settings.gradle.kts`). No
  feature modules exist and none are introduced (`.cursor/rules/android-architecture.mdc` — single
  `:app` until OpenSpec requires modules).
- **Three layers.** `domain/` is pure Kotlin with no Android type on the classpath; `data/` owns
  persistence and is the only layer that names `Context`, `DataStore` or (later) `BillingClient`;
  `ui/` owns Compose, a `ViewModel` and an immutable `UiState`, and reaches the app through interfaces.
- **Unidirectional data flow.** Each screen is a `Route` (state wiring) over a pure `Screen` (a
  function of its arguments). The pattern already exists: `ui/home/HomeRoute` / `HomeScreen`
  (`ui/home/HomeScreen.kt:44`, `:64`) and `ui/settings/SettingsRoute` / `SettingsScreen`
  (`ui/settings/SettingsScreen.kt:53`, `:87`).
- **ViewModels expose `StateFlow` and own no UI state of their own.**
  `ui/settings/SettingsViewModel.kt:29` forwards the store's flow with
  `SharingStarted.WhileSubscribed(5_000)` (`:31`), because a mirror of persisted truth can drift
  (the class comment says so at `:20`). `ui/home/HomeViewModel.kt` is the scaffold's local-state
  example and will be replaced by the builder's ViewModel. **[not-yet-built]** — the builder,
  pace-test, session, import and unlock ViewModels do not exist.
- **No DI framework.** Dependencies are constructed by a `ViewModelProvider.Factory`
  (`ui/settings/SettingsViewModel.kt:55`), and the factory reads `context.applicationContext`
  (`:56`) so a ViewModel never holds an Activity context and the screen never names `DataStore`. The
  build declares no Hilt/Koin (`gradle/libs.versions.toml`).
- **Stores are interfaces.** `SettingsStore` (`data/SettingsStore.kt:72`), `PaceStore`
  (`data/PaceStore.kt:49`) and `SessionStore` (`data/SessionStore.kt:265`) are interfaces with
  `DataStore`-backed implementations, so a screen is previewable and a test can supply an in-memory
  store. The new billing surface follows the same shape: a `BillingStore` interface over the
  `billing` capability, so no screen names a Billing SDK type (**[not-yet-built]**).
- **Serialization.** `kotlinx.serialization` is a declared dependency and is used for Navigation 3's
  `@Serializable` back-stack keys (`ui/PlayKilnApp.kt:41-47`). The in-progress session uses a
  **separate, hand-written** codec — see *Persistence* — because that format is the durability
  contract and is deliberately Android-free.

## Domain model

The model is closed and small, so nothing the UI could invent is unrepresentable, and every value the
generator emits is a named type. MEASURED, `domain/Session.kt`:

- `enum class Stroke { FREESTYLE, BACKSTROKE, BREASTSTROKE, BUTTERFLY, INDIVIDUAL_MEDLEY }` (`:11`). A
  kick, a drill or a pull is **not** a stroke; it is a gear or an intent variation, which is why
  `Stroke` stays at five.
- `enum class Focus { SPEED, THRESHOLD, ENDURANCE, TECHNIQUE, RECOVERY }` (`:25`).
- `enum class Gear { KICKBOARD, PULL_BUOY, FINS, PADDLES, SNORKEL }` (`:40`).
- `data class SessionConstraints(poolLengthMeters: Int, minutesAvailable: Int, focus: Focus,
  strokesToAvoid: Set<Stroke>, gearOnDeck: Set<Gear>)` (`:55`).
- `data class SetBlock(distanceMeters, repetitions, stroke, targetTimeSeconds, restSeconds,
  gear: Set<Gear> = emptySet())` (`:73`), with derived `totalSeconds = repetitions *
  (targetTimeSeconds + restSeconds)` (`:82`) and `totalDistanceMeters` (`:85`). Keeping the target and
  the rest apart is what lets the UI print a time on every line (`:77-78`).
- `typealias Interval = SetBlock` (`:94`) — the swimmer's word for one line of a set list.
- `enum class SectionKind { WARM_UP, MAIN_SET, COOL_DOWN }` (`:97`) and
  `data class SessionSection(kind, blocks)` (`:104`) with `totalSeconds` (`:108`) /
  `totalDistanceMeters` (`:110`).
- `data class Session(constraints, pacePer100Seconds, sections, estimatedTotalSeconds,
  totalDistanceMeters)` (`:120`) with `blocks` (`:128`), `section(kind)` (`:131`),
  `mainSetBlocks` (`:139`) and `estimatedTotalMinutes` rounded up (`:142`).

The model states its own totals rather than leaving the caller to re-add, so "does this fit the
minutes I have?" is answerable without walking the sections (`domain/Session.kt:113-119`).

## Pace arithmetic

One number drives every interval: the swimmer's **threshold pace per 100 m**, derived from a 400 m and
a 200 m time trial by the Critical Swim Speed relationship (Ginn, 1993), which is the standard field
test a coach uses without lactate data (**ON RECORD** as the model's own citation,
`domain/PaceModel.kt:9`):

```
CSS (m/s)                    = (400 - 200) / (t400 - t200)
threshold pace per 100 m (s) = 100 / CSS = (t400 - t200) / 2
```

MEASURED, `domain/PaceModel.kt`:

- `thresholdPacePer100(timeTrial400Seconds: Double, timeTrial200Seconds: Double): Double` (`:37`)
  returns `(t400 - t200) / 2` (`:45`). It `require`s the 200 m time positive (`:38`) and the 400 m
  time strictly slower than the 200 m (`:41`), and **throws rather than returning a negative pace**,
  because a silently wrong target would print on every interval (`:36`).
- `intervalTargetSeconds(pacePer100Seconds: Double, distanceMeters: Int): Int` (`:55`) returns
  `(pace * distance / 100).roundToInt().coerceAtLeast(1)` (`:62`). It is **whole seconds on purpose**:
  a pool pace clock is read in whole seconds, and a target a swimmer cannot see on the clock is not a
  target (`:51-53`).

The generator applies **explicit per-focus factors** on top of the threshold pace — `0.90–1.25` — and
those factors are the generator's decision, not this model's (`domain/PaceModel.kt:21-22`;
`domain/SetGenerator.kt:337-339`, `profileFor` at `:366`).

**Why the difference of the two times carries the pace.** The two distances slope the swimmer's
speed-versus-duration line, so the 400 m time alone does not determine the threshold
(`domain/PaceModel.kt:19-22`). This is the clause that makes the guided trials the wedge: the
incumbent's own reviewer reports that the test workouts that set up the dynamic pacing have no
guidance (**ON RECORD**, evidence §3.4), so the app must walk the swimmer through the two swims rather
than hand them a time field.

## The generator: determinism and constraint honouring

MEASURED, `domain/SetGenerator.kt`. The generator is **not a rotation over a fixed library** — cutting a
set out of a fixed list is exactly the reviewer complaint it answers (`:5-10`) — and it is
**deterministic**: the same constraints and the same pace always produce the same session (`:12-14`),
which is what makes the core promise falsifiable.

`generateSession(constraints: SessionConstraints, pacePer100Seconds: Double): Session` (`:33`) —
MEASURED structure:

1. **Refuses self-contradictory input.** It `require`s a positive pace (`:34`), a positive pool
   (`:37`), positive minutes (`:39`) and at least one allowed stroke (`:43-45`), because a silently
   empty main set would be worse than a refusal the caller can act on (`:29-31`).
2. **Builds a seed from every constraint** via `seedFor` (`:316`): a canonical, field-separated string
   of pool length, minutes, focus, sorted avoided strokes, sorted gear and the pace (`:317-324`),
   hashed with **FNV-1a** (`:325-330`). Two constraint sets that differ in any field get different
   seeds — that is how "the main set changes when the constraints change" is made true rather than
   hoped for (`:313`).
3. **Writes the bookends first** — `buildWarmUp` (`:82`) and `buildCoolDown` (`:122`) — then shrinks
   them with `fitBookends` (`:144`) until they take no more than half the budget, and hands the rest to
   the main set. The main set is the reason the swimmer came, so it is never the part that gives way
   first (`:140-143`).
4. **Builds the main set** in `buildMainSet` (`:174`): one block per declared gear item, in sorted
   order, then stroke sets whose **count falls out of the minute budget**
   (`targetBlockCount = (minutesAvailable / 12).coerceIn(2, 7)`, `:191`) and whose distance, stroke,
   repetitions and rest all come from the seeded sequence. `fitBlock` (`:248`) trims **repetitions** —
   never the rest or the target — so the pace the swimmer is meant to hold stays intact (`:245-247`).
5. **Has a last safety net.** `trimToBudget` (`:273`) shrinks the cool-down first and then the warm-up
   if the fitted sections still overflow; the main set is never touched (`:269-272`).
6. **Snaps distances to whole pool lengths** with `snapToStep` (`:257`), never below one length.
7. **Uses an explicit xorshift32** `DeterministicRandom` (`:422`) rather than `kotlin.random.Random`,
   because the contract is "identical input, identical output" across every Kotlin version and
   platform without relying on a library's seeding guarantees (`:416`). A zero seed is remapped
   (`:423`).

**Invariants it claims** (`domain/SetGenerator.kt:17-22`), each of which the existing test file drives:
every emitted block carries a target derived from the pace; a stroke in `strokesToAvoid` is never
emitted; only gear in `gearOnDeck` is used; and the estimated total is fitted inside
`minutesAvailable` and stated on the returned `Session`. The **negative control** the contract names
(`differentiators.json` `computes-the-session`) is `Session.mainSetBlocks` changing when the
constraints change (`domain/Session.kt:133-139`) — the tests assert exactly that for a changed pool
length, minute budget, gear set and avoided-stroke set
(`app/src/test/java/com/gexetr/laneset/domain/SetGeneratorTest.kt`).

**The programming rules are authored data, in code.** Per-focus profiles (`profileFor`, `:366`) fix the
distance ladder, repetition range, rest range and pace factor per focus; per-gear recipes
(`gearRecipe`, `:407`) fix a kickboard/pull/fins/paddles/snorkel set. Bookend factors are named
constants (`:337-339`): warm-up `1.15`, warm-up kick `1.25`, cool-down `1.25`. These are the factory's
judgement, not calibration data, and the design states that plainly rather than implying a
physiological source.

## Coach-workout parser

MEASURED, `domain/CoachSetParser.kt`. A swimmer who already has a club set should not retype it into a
different shape, so a pasted practice becomes **exactly the value a generated session is** — sections,
blocks and a target on every interval, derived from the swimmer's own pace (`:5-8`).

`parse(text: String, pacePer100Seconds: Double, constraints: SessionConstraints = …): Session`
(`:60`). What it understands, because that is how practices are written (`:11-19`):

- section headers (`Warm up`, `Main set`, `Cool down`) set the section for the lines after them, and a
  header may carry its own set on the same line (`Warm up 300 free`) — `sectionFor` (`:122`);
- a set as `repetitions x distance` (`8x100`) or a bare distance (`300`) — `REPETITIONS_AND_DISTANCE`
  (`:26`), `SINGLE_DISTANCE` (`:27`);
- a stroke word after the distance (`free`, `back`, `breast`, `fly`, `IM`), defaulting to freestyle —
  `STROKE_ALIASES` (`:32`);
- a send-off (`on 1:45`, `@1:10`) from which the rest is derived as send-off minus target — `SEND_OFF`
  (`:28`), and the rest computation at `:168-169`;
- an explicit rest (`rest 20`) — `REST` (`:29`).

Blank lines and `#`/`//` comments are ignored, a line with no readable distance is **skipped rather
than guessed at** (`:19-20`), and when the text names no section headers every set is the main set
(`:20`). The parser owns no display text — it returns the `Session` and the UI formats it (`:9`).
`detectFocus` (`:181`) takes the focus from the text when it names one (sprint/speed, threshold/css,
technique/drill, recovery/easy, endurance/aerobic).

The promise `open-a-coach-workout` is `supporting` / `before` / `data_in`, and its evidence strength is
**INFERRED** — no incumbent reviewer was recorded asking to import a coach's workout
(`differentiators.json` `open-a-coach-workout`). The design honours that label: the parser is cheap to
build and closes the `before` stage, but it is not sold as a measured demand claim.

## Persistence

Three DataStore files, deliberately separate, plus one interface per concern. MEASURED:
`data/SettingsStore.kt`, `data/PaceStore.kt`, `data/SessionStore.kt`.

**Settings** — `settingsDataStore` (`data/SettingsStore.kt:105`), a `Flow<AppSettings>` with
`AppSettings(themeMode, dynamicColor, hapticsEnabled)` (`:60`). The read `catch`es `IOException` to
`emptyPreferences()` (`:122-125`); the theme is stored by enum `name` and an unrecognised value falls
back to `FOLLOW_SYSTEM` (`:128-129`), so a downgrade cannot brick the screen. The store's own comment
records the rule that **a field is added together with its reader**, because `settings.behaviour`
fails a toggle nothing reads (`:44-58`).

**Pace** — `paceDataStore` (`data/PaceStore.kt:66`), a separate file on purpose because the two have
different lifetimes and one corrupt file should not take the other down (`:62-64`). `PaceProfile`
(`:25`) keeps **all three numbers** — the two trials and the derived pace — because the trials are the
swimmer's own measurement and the pace is derived, so the pace can be recomputed if the model changes
and Settings can show the two times actually swum (`:17-20`). `hasTrials` (`:30-34`) is true only when
all three are present, so a half-written record reads as "no test". `setTimeTrials` (`:97`) delegates
the arithmetic to `PaceModel` rather than duplicating `(t400-t200)/2`, and writes all three keys in one
`edit { }` so a reader never sees a 400 without its 200 (`:96-104`).

**In-progress session** — `sessionDataStore` (`data/SessionStore.kt:280`). This is the storage half of
`session-survives-interruption`: a generated session comes back with the same set, the same targets and
the same position rather than being regenerated (`:254-259`).

- `SessionPosition(sectionIndex, blockIndex, repetitionIndex)` (`:32`) is **indices, not a wall-clock
  stopwatch** — a pool visit is interrupted and what must survive is the position in the set, not an
  elapsed timer that would have kept running (`:22-27`). `clampedTo(session)` (`:47`) guarantees the
  restored position points at a real block/repetition, so a position stored against a different
  arrangement cannot point past the end of the one on screen (`:29-30`).
- `PersistedSession(session, position)` (`:67`) is the whole of what survives.
- `SessionSerializer` (`:86`) is a **stable, versioned, framework-free** line-oriented codec.
  `SCHEMA_VERSION = 1` (`:89`); `v=` is the version and a payload whose version is not current is
  **refused rather than guessed at** (`:202`), so an old or future payload degrades to "no session"
  (`:81-84`). Every field is an enum name, an integer or a double, so the format needs no string
  escaping (`:77-79`). `decode` **never throws** — a malformed, truncated or unreadable payload
  returns `null`, which the store surfaces as "nothing to resume" (`:83-84`, `:125`), because the
  caller is a cold-start restore and a corrupt file must cost the session, never the app.
- `SessionStore` (`:265`) is deliberately dumb: it saves what it is given and never decides how a
  session advances; advancing is the runner's job, and a store that "helpfully" moved the position
  would be a second place progress is decided (`:261-263`). `save(session, position)` (`:270`),
  `savePosition(position)` (`:273`, a no-op when nothing is saved) and `clear()` (`:276`).
- `DataStoreSessionStore` (`:289`) stores the whole payload under one string key, so it is read whole
  or not at all and a partially-updated record is impossible (`:285-287`); `savePosition` decodes,
  re-clamps and re-encodes (`:302-308`).

**Not-yet-built:** the in-progress session has no writer yet — no screen calls `SessionStore.save`. The
promise stays `deferred` until the session screen exists and a test recreates the holder from a saved
state.

## Play Billing: the single unlock and the free/paid surface

**Contract.** `purchase_led_single_unlock`, `billing` required, `ads` deliberately not enabled, no
server (`economics.json`). One **non-consumable** Play product. The build already declares both the
factory's `billing` capability and the raw SDK, resolving the same version so one library compiles in
(`app/build.gradle.kts:140-146`; `gradle/libs.versions.toml:21` `billing = "9.1.0"`,
`:54` `capability-billing`, `:55` `billing-ktx`). The `BILLING` permission is the app's only
`uses-permission` (`app/src/main/AndroidManifest.xml:9`).

**The free/paid surface — a decided trade-off, with its tension named.** The recon fixes two moments
and they pull in different directions: `paidUnit` lists **the guided pace test** inside what is bought
(`economics.json`), while `momentOfPayment` says the free **Endurance** session has been "generated and
swum once" before payment, and a session cannot carry the target time the whole app is about without a
pace. The design resolves this as follows, and marks the resolution as a **product decision the owner
may adjust**, not a measurement:

| Surface | Free | Behind the unlock |
|---|---|---|
| Build a session | the **Endurance** focus only | every other focus (Sprint, Threshold, Technique, Recovery) |
| The pace behind the targets | a conservative **default** threshold shipped in the programming rules, labelled in the UI as a default | the **guided 400/200 trials**, which replace the default with the swimmer's own pace for every future session |
| Coach-workout import | locked | unlocked |
| Session runner and position resume | **free** (durability is a `trust` promise, not a purchase reason) | — |
| Share / print handoff | locked | unlocked |
| Settings | free | — |

This keeps `paidUnit` literally true (the guided trials are part of the unlock), keeps
`momentOfPayment` literally true (a working Endurance session is swum first), and does not hollow the
free session (it still carries a target time). The **exact default value must be authored from the
programming rules, not invented here** — this document deliberately states no number. **[not-yet-built]**
— no billing code, entitlement store or paywall exists.

**Entitlement state must survive process death.** The unlock is stored through the `billing`
capability's entitlement store (DataStore-backed, in `data/`, never in the UI layer), and the release
acknowledges pending purchases on cold start, because an unacknowledged purchase is reversed by Play
and reads as a lost sale. **UNVERIFIED** — whether the current `billing` capability exposes a
product-details/price surface is not established in this session; the render must be checked against
the capability as it stands.

**No price of its own.** The paywall states that the unlock is a one-time purchase and that Google Play
shows the exact localized price on the confirmation sheet it opens. A hard-coded `$9.99` is wrong for
every user outside the dollar zone, and a wrong price on a paywall is worse than no price.

**Restore and reinstall.** The unlock is **non-consumable** and restorable after a reinstall with the
same Play account. `no-subscription-one-unlock` requires: no renewal, no trial clock, no expiry, no
subscription wording anywhere, and no advertising SDK. MEASURED, the build declares no ads dependency
(`gradle/libs.versions.toml`, `app/build.gradle.kts:140-146`).

## Screen map and navigation

**The host already exists and is correct.** `ui/PlayKilnApp.kt` hosts Navigation 3 with
`rememberNavBackStack(HomeDestination)` (`:64`) — the back stack Navigation 3 owns, not a hand-rolled
`listSaver` — a `rememberListDetailSceneStrategy<NavKey>()` (`:65`), and destinations declared as
`@Serializable data object … : NavKey` (`:41-47`) typed by the `entryProvider` DSL (`:75-98`).
`MainActivity` opts in to edge-to-edge with `enableEdgeToEdge()` (`MainActivity.kt:27`) and the shell
wraps the host in a root `Surface` (`ui/PlayKilnApp.kt:71`) so `LocalContentColor` yields
`onBackground` rather than Compose's default black. These are the fixes `.cursor/rules/ui-ux-baseline.mdc`
requires and they are MEASURED present.

**Existing destinations:** Home (list pane), About (detail pane), Settings (detail pane)
(`ui/PlayKilnApp.kt:80-96`). **New destinations required [not-yet-built]:** Builder (the constraints
form), Session (the computed set list and the runner), Import (paste a coach's workout), PaceTest (the
guided 400/200 trials), Unlock (the single-price sheet). Each is a `@Serializable` `NavKey` pushed on
the same host-owned stack, so predictive back pops it like any other destination. The app is already
declared back-opted: `android:enableOnBackInvokedCallback="true"`
(`app/src/main/AndroidManifest.xml:16`).

**First-run flow.** On a cold start with no saved pace and no entitlement the app opens on Home, which
presents **one** action: build the free Endurance session. The Builder collects pool length (default
`SettingsStore.DEFAULT_POOL_LENGTH_METERS = 25`, `data/SettingsStore.kt:94`), minutes, focus (only
Endurance selectable; the others show a lock that opens the Unlock sheet), avoided strokes and gear on
deck. Submitting pushes Session, which renders the generated set with a target on every interval and a
resume position. The two conversion moments are the first locked focus and the first tap on the
handoff — exactly the recon's `momentOfPayment`.

**[not-yet-built]** none of Builder, Session, Import, PaceTest or Unlock exists; Home still renders the
scaffold starter.

**Adaptive layout.** The existing `ListDetailSceneStrategy` keeps a wide window as two panes
(`ui/PlayKilnApp.kt:53-54`, `:81-95`), which satisfies `uiux.adaptive`. The new screens must not
regress it: the constraints form and the session list are a list-and-detail pair, so a wide window
shows the set beside the session rather than replacing it.

## Export / share / print handoff

This is the core promise `takes-it-to-the-pool` (`data_out`), and `.cursor/rules/app-depth.mdc` makes
the handoff floor non-negotiable: the app must not construct a set list the user cannot get off the
phone (the defect measured on `lawn-round-log`, whose `Csv.kt` built a file with no `ACTION_SEND`, no
`ACTION_CREATE_DOCUMENT` and no `FileProvider` — `.cursor/rules/app-depth.mdc` "Why the factory ships
partial flow").

**Exact intent shapes required [not-yet-built]:**

- **Share as text.** `Intent(Intent.ACTION_SEND)` with `type = "text/plain"` and `EXTRA_TEXT` carrying
  the **whole** set list — every section, every block, and on every interval its distance,
  repetitions, stroke, target time and rest, plus the pace the targets derive from. Launched through
  `Intent.createChooser`, so the swimmer picks the app. No storage permission and no `FileProvider`
  are needed for a text payload.
- **Print a one-page card.** A one-page card built with the framework's
  `android.graphics.pdf.PdfDocument` and written to a file the swimmer chooses through
  `Intent.ACTION_CREATE_DOCUMENT` (the Storage Access Framework), which needs **no storage
  permission**. The card carries the same content as a compact monospaced table sized for one page.
- **The alternative considered and rejected:** the platform print framework
  (`PrintManager` + a `PrintDocumentAdapter`). It opens the system print sheet directly, which is
  friendlier, but it requires a print adapter and leaves **no file the swimmer keeps**. Choose the
  SAF/`PdfDocument` route; if a later revision wants the print sheet too, it is an addition, not a
  replacement.

**Proof requirement.** The proof artifact for this promise must contain the family's tokens
(`ACTION_SEND`, and an `ACTION_CREATE_DOCUMENT`/`PdfDocument` pair), and a test must assert the shared
payload carries the whole set list with its interval targets (`differentiators.json`
`takes-it-to-the-pool`).

## Settings surface and the language row

The settings rule `.cursor/rules/app-settings.mdc` is already satisfied by the scaffold's surface and
must stay satisfied. MEASURED:

- A real Settings destination exists (`ui/settings/SettingsScreen.kt:87`) and is reachable from Home's
  top bar (`ui/home/HomeScreen.kt:105`, `stringResource(R.string.settings_title)`) and declared as a
  destination (`ui/PlayKilnApp.kt:94`).
- Three groups: **Appearance** (theme mode over `FOLLOW_SYSTEM`/`LIGHT`/`DARK`, plus a dynamic-colour
  switch gated below API 31 — `ui/settings/SettingsScreen.kt:142-155`), **Language**, and **Behaviour**
  (haptic feedback, `:184-190`).
- The **behaviour preference is read outside the surface**: `settings.hapticsEnabled` is collected in
  `MainActivity.kt:31` and handed to `PlayKilnApp` (`:38`), which passes it to `HomeScreen`, where the
  action reads it before firing `HapticFeedbackType.LongPress` (`ui/home/HomeScreen.kt:117`). That is
  the preference `settings.behaviour` needs.
- The **language row opens the platform's own per-app picker**, not a second in-app picker.
  `Context.openAppLanguageSettings()` (`ui/settings/SettingsScreen.kt:290`) sends
  `Settings.ACTION_APP_LOCALE_SETTINGS` with a `package:` data URI on API 33+ (`:292-293`) and
  `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` below it (`:295`), returning `false` from the
  `ActivityNotFoundException` catch (`:301-303`) so the screen can say so (`:176-178`). This is exactly
  the shape `.cursor/rules/app-settings.mdc` requires while `android:localeConfig` is declared
  (`app/src/main/AndroidManifest.xml:20`), and there is deliberately **no** stored language field
  (`data/SettingsStore.kt:55-58`).
- Every preference is persisted through the store and read back as a `StateFlow`
  (`ui/settings/SettingsViewModel.kt:29`), and the screen names no `DataStore` type.

**Extensions [not-yet-built], each added with its reader.** When the Builder ships it will want a
default pool length and a default session-minutes preference. Both are **behaviour** preferences and
may be added — but only together with the code that reads them, because `settings.behaviour` fails a
stored value read by nothing (`data/SettingsStore.kt:44-58`). Until then `DEFAULT_POOL_LENGTH_METERS`
stays a constant, which is why the store publishes it as one (`data/SettingsStore.kt:82-93`).

## Localization

The slot is **born multilingual** (`res/xml/locales_config.xml`: `en` and `ru`), and every user-facing
string is a resource (`.cursor/rules/localization.mdc`). MEASURED:

- `app/src/main/res/values/strings.xml` is the default locale; `app/src/main/res/values-ru/strings.xml`
  ships alongside it.
- Every string in the existing screens is reached through `stringResource` — e.g.
  `ui/settings/SettingsScreen.kt` names no prose literal, and `ui/home/HomeScreen.kt:75` routes the
  snackbar through `R.string.…`.
- `android:localeConfig="@xml/locales_config"` is declared (`app/src/main/AndroidManifest.xml:20`), so
  Android 13+ owns the per-app picker and the language row routes to it (above).

**Required and not-yet-built:** every new string for the Builder, Session, Import, PaceTest and Unlock
screens must be added to **both** `values/` and `values-ru/` in the same change, because `slot.locales`
fails a declared locale whose tree falls back to English or carries a placeholder. The store listing
already carries the two localized titles (`store/listing.json` `locales`, `en-US` "Laneset: Swim Set
Builder" / `ru-RU` "Laneset: план тренировок", from `naming.json`).

**The About copy is currently wrong.** `res/values/strings.xml:18` declares `about_privacy_note` =
"This starter requests no permissions and sends no data." That contradicts the shipped configuration:
the manifest declares `com.android.vending.BILLING` (`app/src/main/AndroidManifest.xml:9`) and the pack
declares that Play Billing transmits purchase data (`store/listing.json:59-60`, `collectsData: true`).
The scaffold strings ("began as a PlayKiln starter", `res/values/strings.xml:17`) must be replaced
with true copy in both locales before release. **[not-yet-built]**.

## Visual identity and iconography

**The identity is not declared yet.** `.cursor/rules/visual-identity.mdc` requires the identity to be a
build input declared in `store/studio.json` **before** the UI is styled, and **MEASURED:
`store/studio.json` does not exist** (`Test-Path store/studio.json` returned `False`). The recon
declared the intended direction — *a lane line and a turning cross rendered as two stacked strokes*,
palette *pool water at depth: deep teal-blue fields, one high-chroma accent reserved for the target
time on an interval* (**ON RECORD**, `docs/ideation/2026-10-02-planners-and-optimisers.md` §7) — and it
is a **requirement, not a fact**:

- **[required, not-yet-built]** `store/studio.json` with `mark`, the full `palette`, per-shot
  `headlines`/`kickers`, `featureTagline` and `featurePill`.
- **[required, not-yet-built]** the launcher icon, the adaptive-icon foreground, the monochrome/themed
  layer and any in-app brand mark derived from `studio.json`'s `mark`, never from a library icon
  (`docs/icon-provenance.md`). MEASURED today: the launcher vectors exist
  (`res/drawable/ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`,
  `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`), but there is no declared palette
  for `icons.launcher` to check the fills against. The theme's current palette (`ui/theme/Color.kt:5-24`:
  green/teal/amber roles) is not yet reconciled with the recon's teal-blue direction or with
  `store/listing.json:92-93`'s `brandColor` `#369b35` / `accentColor` `#977b47`. Making these one
  decision is required before release.
- `store/provenance.json` is required once the pack has composed assets
  (`.cursor/rules/release-gate.mdc`, phase 2).

**The UI icon set is vendored and licensed.** MEASURED, `icons/provenance.json`: one set,
**Material Symbols**, pinned at commit `737e3324305806514d7909874fa1818ae1808232`, **Apache-2.0**,
source `https://github.com/google/material-design-icons`, licence text committed at
`apps/2026-10-02-laneset/icons/LICENSES/Apache-2.0.txt`, 13 icons — `arrow_back`, `add`, `close`,
`settings`, `share`, `print`, `timer`, `delete`, `check`, `edit`, `info`, `pool`, `water_drop` — each
with a `sha256` and an upstream `sourcePath`. This satisfies `docs/icon-provenance.md`; the `share`,
`print`, `timer` and `pool` glyphs are exactly the vocabulary the new screens need. **[not-yet-built]**
the six new screens do not yet reference them, and `icons.mark` / `icons.launcher` cannot pass until
`store/studio.json` declares the palette the launcher fills are checked against.

## Privacy, offline operation and Data safety

- **No `INTERNET` permission** in the app's own manifest. MEASURED, the only `uses-permission` is
  `com.android.vending.BILLING` (`app/src/main/AndroidManifest.xml:9`), which belongs to the Play
  Billing flow and is a normal permission granted by the Play Store. No location, camera, storage,
  contacts, microphone or background work (`economics.json`).
- **Cleartext is forbidden** by `res/xml/network_security_config.xml`, deliberately strict so a future
  revision that adds a network path inherits HTTPS-only.
- **Backup rules separate cloud backup from device transfer**: `res/xml/data_extraction_rules.xml` and
  the legacy `res/xml/backup_rules.xml`.
- **Data safety is not settled.** `store/data-safety.md` is still the unfilled scaffold (MEASURED — its
  checkboxes are unchecked), and `store/privacy.md` is still a TODO scaffold (MEASURED — "Last
  updated: TODO"). `store/listing.json:59-60` already declares `collectsData: true` with a **Financial
  info** row for Play Billing. The pack and the form must be reconciled to one truth before submission.
- **The honest boundary, and the precedent.** The promise `offline-by-construction` is *no account and
  no network in the daily path*, **not** "the app is incapable of using the network"
  (`differentiators.json` `offline-by-construction`). The **merged** manifest — not the app's own — is
  what ships, and reading it is the only honest check; a transitive dependency of Play Billing
  (`com.google.android.datatransport`) contributed `INTERNET`/`ACCESS_NETWORK_STATE` to the reference
  teacher-gradebook's merged manifest despite its own manifest declaring none. **UNVERIFIED for
  laneset**: no build has been run and the merged manifest has not been read. The deciding action is to
  read the merged release manifest after the first build and record the observed list in
  `store/checklist.md`, correcting the privacy and Data-safety wording if it disagrees. The app's own
  Kotlin has no HTTP client and no URL, and the cleartext policy forbids cleartext regardless.
- **No client-side secrets.** The app talks to no provider, so `scripts/check-client-secrets.mjs`
  should be trivially clean; the app bundles no key (`economics.json`).
- **In-app privacy policy.** When `listing.json` gains a `privacyPolicyUrl`, the policy must be
  reachable from **inside** the app (`AGENTS.md` Play compliance floor;
  `scripts/check-in-app-policy.mjs`). Today `listing.json` declares no `privacyPolicyUrl` and the About
  screen has no such route, so this is a **[not-yet-built]** requirement, not a satisfied one. The
  scaffold's `about_privacy_note` is not the policy and is currently false.

## Deliberately decided trade-offs

Each is a decision with a cost that is named, not hidden.

1. **Determinism over variety.** The generator is a seeded function of the constraints, so identical
   constraints give the identical session. That is what makes the core promise testable, and the fix
   for "samey" is that the seed depends on **every** constraint — but a swimmer who changes nothing
   gets the same set twice. Accepted; the alternative (a random generator) cannot be tested against the
   negative control the contract requires.
2. **A text-and-print handoff, not a watch ecosystem.** The shelf's native handoff is a Garmin/Apple
   watch; this app is phone-only. For a swimmer who already owns a supported watch this is strictly
   worse. Accepted and recorded in the recon and the promise.
3. **No history or tracking.** The planner lane forbids the notebook shape and every leader already
   charts times. The app computes and hands over; it keeps no record of what was swum. A real cost,
   accepted.
4. **The free tier uses a default pace.** Explained under *Play Billing*: the free session must carry
   a target time, and the guided trials are part of the unlock. The tension with `paidUnit` is named
   rather than smoothed.
5. **Three DataStore files, not one.** Settings, pace and in-progress session have different
   lifetimes, and one corrupt file should not take another down (`data/PaceStore.kt:62-64`).
6. **A hand-written, versioned codec for the session, not a serialization framework.** The session
   format is the durability contract, so it must be readable and writable with no Android type on the
   classpath and directly unit-testable (`data/SessionStore.kt:72-79`); a framework would put the
   format beyond the JVM tests that prove the promise.
7. **A dumb store.** `SessionStore` never advances the position; the runner does, so progress has one
   decider (`data/SessionStore.kt:261-263`).
8. **No DI framework, single `:app` module.** Factory-constructed `ViewModel`s and interface stores are
   enough for this surface; Hilt/Koin would add build cost and indirection without adding a capability.
9. **A default pool length is a constant, not a setting, until its reader exists.** Adding it to
   `AppSettings` early would fail `settings.behaviour` (`data/SettingsStore.kt:82-93`).
10. **The paywall states no numeric price.** A hard-coded price is wrong outside the dollar zone; the
    Play confirmation sheet carries the localized price.

## What is not yet built

Stated once, plainly, so no reader mistakes this design for a description of a finished app:

- the constraint form, the session screen and the runner; the coach-import screen; the guided pace-test
  flow; the unlock sheet — **none exists**;
- **no billing code, no entitlement store, no paywall**, though the libraries and the `BILLING`
  permission are declared;
- **no share, print, `ACTION_SEND`, `ACTION_CREATE_DOCUMENT`, `PdfDocument` or `FileProvider` path**;
- **no `store/studio.json`, no declared palette, no slot-owned mark**, and no `capabilities.json`;
- **no pack**: `store/privacy.md`, `store/data-safety.md`, `store/review-notes.md` and
  `store/graphics.md` are scaffolds or TODOs;
- **no build has been run, no test result is claimed, and the merged manifest has not been read.**

The domain and data layers, the contracts, the scaffold UI shell and the vendored icon set are the
parts that are real.

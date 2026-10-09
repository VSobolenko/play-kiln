# Design: app-lumintra-light-planner

**Status of this document.** It describes the intended architecture and the mathematics of the slot
and, separately, **what exists on disk today**. The two are never blended: every part that is not
built is written as a requirement, in the future tense, and the closing
`## What is not yet built` section states plainly that this change has no application code yet. The
normative requirements live in the eleven capability specifications (`night-scene`, `session-brief`,
`target-catalogue`, `satellite-passes`, `cloud-forecast`, `scout-spots`, `share-brief`, `night-glance`,
`settings-and-credits`, `welcome`, `visual-identity`), written in parallel; this document states **how
the thing is built**, not what it must do, and cross-references those capabilities rather than
restating them.

**Labels.** Every factual sentence carries the factory's four labels
(`.cursor/rules/grounded-claims.mdc`): **MEASURED** (read or run in this session, with the `path` or
the command), **ON RECORD** (a named source states it, and this session read that source),
**INFERRED** (it follows from labelled facts, and the step is shown), **UNVERIFIED** (not checked; the
exact check that would settle it is named). The numbers in this document are inherited from
`annex/N-data-algorithms.md` (§ cited per clause) and
`annex/O-sky-scene-rendering.md` (§ cited per clause), both read in full this
session.

**The authoring annexes are committed beside this document.** The six inputs this design cites —
`CONTEXT.md`, `K-competitor-features.md`, `L-depth-retention-edges.md`,
`M-play-integrations-compliance.md`, `N-data-algorithms.md` and `O-sky-scene-rendering.md` — live under
`annex/` inside this change (`openspec/changes/app-lumintra-light-planner/annex/`), and `annex/README.md`
is the letter-to-file index. Every citation of the form `<letter> §x` into one of those six files — for
example `N §4.1` — resolves to `annex/<that file> §x` (`annex/N-data-algorithms.md §4.1`), and every
citation of the form `(E<n>)` — and the bare `E<n>` it abbreviates, for example `E36` — resolves to a
row of the edge-case register in `annex/L-depth-retention-edges.md`. A citation naming a file that is
**not** in `annex/` —
`F-engine-gap-map.md`, `G-android-build-proof.md` and `I-assets-and-data.md` under
`.kiln/scratch/positioning-2026-10-08/`, and that directory's `run-passvariants.txt` and
`run-passcrosscheck.txt` probe transcripts — is marked **not copied into this change**, so a reader who
never had the scratch directory knows it may be absent.

**A standing honesty limit that governs the whole document.** The test device is the Samsung Galaxy
**A40** (`SM-A405FM`, serial `R58MB3SGZVL`), **Android 11 / API 30** — it is **below the AGSL floor of
API 33**, so it never executes the shader path, and **no frame-time, thermal or battery measurement
exists for this scene on this phone**. Per `.cursor/rules/device-testing.mdc`, that phone is the
factory's test target. Both the A40 performance claim and the two-path colour-parity claim are
**UNVERIFIED** and are written as such throughout; §16 names the exact commands that would settle each.

---

## Product boundary

`lumintra-light-planner` is a **paid session planner** for outdoor and night photographers: it takes a
place the user saved, a night, and a lens, and answers — when is it actually dark, where is the moon,
which of the ~250 famous deep-sky objects will be up and for how long, will a satellite streak the
frame, what will the clouds do, and will it all fit inside the field of view — on **one scrubbable
scene** the user drags through the night, with their own drawn horizon silhouette in the foreground
and the real sky above it (annex/CONTEXT.md §1, §2). The buyer is the outdoor / night / landscape
photographer planning a shoot (annex/CONTEXT.md §2). The business shape is `purchase_only_no_ads`, one-time
**$9.99** set in Play Console, with **no in-app product, no billing library, no ads and no
subscription, ever** (annex/CONTEXT.md §2).

The distinctness, in one sentence: *it plans one night at one place around the darkness itself —
the computed moonless astronomical-dark window, the moon, the named deep-sky targets, the passes and
the cloud, all on a single scrubbable sky the user draws their own horizon onto — where the incumbents
plan an object's position and leave the user to infer the darkness.* The wedge is a difference in
**functionality and content**, not in naming or colour
(`docs/monetization-strategy.md` §1.2, cited by `.cursor/rules/ideation-gate.mdc`).

**What this app is not, stated so the boundary is a decision rather than an omission:**

- **Not a star map and not an AR camera sky view.** It plans a session; it does not identify what is
  overhead in real time, and it asks for no camera (`ideation-gate.mdc` § "Games"/depth; annex/L-depth-retention-edges.md §1.3.B7).
- **Not a satellite tracker as its primary job.** Passes are one condition inside the brief, never the
  headline (annex/L-depth-retention-edges.md §1.3 / `F-engine-gap-map.md` §4.6, not copied into this change).
- **Not a light-pollution / Bortle / SQM map.** No raster ships in v1: the widely used Falchi World
  Atlas is **CC BY-NC** and cannot go in a paid app (annex/N-data-algorithms.md §6.2, MEASURED). Darkness is described from
  computed quantities plus the user's own SQM reading or Bortle estimate, labelled "user estimate".
- **Not a notebook, log or history.** It computes and hands over; it keeps no record of what was shot.
- **Not a social, streak, challenge or coaching product.**
- **No account, no server the factory runs, no cloud sync.** The two network calls it makes (element
  sets, cloud forecast) are keyless and user-triggered (see §9, §11).

The four market gaps this slot is built on are consolidated in
`docs/research/2026-10-08-light-and-sky-session-planner-market.md` §3–§5, and the binding promise
contract is `docs/ideation/plans/lumintra-light-planner.differentiators.json` (7 existing claims;
MEASURED, read this session). This design is traceable to those promises clause by clause; where a
design decision would weaken one, the design gives way, not the promise
(`.cursor/rules/differentiator-contract.mdc`).

## Architecture and layering

**One Gradle module.** The slot is a single `:app` module plus the shared baseline-profile test module
(the factory default per `.cursor/rules/android-architecture.mdc` and the `android-compose-slot`
skill). No feature modules are introduced; the surface does not need them and modules would add build
cost with no capability.

**The package map.** Root package `com.gexetr.lumintralightplanner`, at
`app/src/main/java/com/gexetr/lumintralightplanner/` (annex/CONTEXT.md §3.1):

```text
domain/                 pure Kotlin, NO Android type on the classpath
  Time.kt               NightSpan, EventInstant
  Twilight.kt           TwilightKind, TwilightWindow, DarkWindowKind, DarkWindowReport
  Moon.kt               MoonState, MoonlessWindow
  SkyPosition.kt        HorizontalCoords, EquatorialCoords
  SkyObject.kt          SkyObject, ObjectType, ObjectKind, ObjectVisibility
  TargetWindow.kt       TargetWindow
  PassFinder.kt         SatellitePass, PassVisibility, ElementSetAge
  CloudForecast.kt      CloudHour, CloudReport, ForecastAge
  HorizonProfile.kt     HorizonProfile, HorizonSample
  Optics.kt             LensPreset, FieldOfView, TargetFit
  SceneState.kt         SceneFrame, SceneScrub
  Scout.kt              Scout, EquipmentProfile
  Brief.kt              SessionBrief, BriefCondition, ConditionKind
  SkyGrade.kt           SkyGrade

data/                   the only layer that names Context / DataStore / java.net / the clock
  ScoutStore.kt  SettingsStore.kt  CatalogueRepository.kt  CloudRepository.kt
  ElementsRepository.kt  SkyAssetSource.kt  SpotTransfer.kt

ui/
  theme/                Color.kt Type.kt Shape.kt Theme.kt  NightVisionPalette.kt
  components/           LumintraMark.kt SkyCard.kt StatTile.kt ConditionChip.kt
                        SectionStrip.kt EmptyState.kt PhaseDial.kt
  night/                NightSceneScreen.kt
  brief/                BriefScreen.kt
  targets/              TargetsScreen.kt
  spots/                SpotsScreen.kt SpotEditScreen.kt
  share/                ShareScreen.kt
  settings/             SettingsScreen.kt CreditsScreen.kt
  welcome/              WelcomeScreen.kt
widget/               NightGlanceWidget.kt  NightGlanceWidgetReceiver.kt  NightGlanceState.kt
reminder/             DarkWindowReminder.kt
```

The file paths and package names above are fixed by annex/CONTEXT.md §3.3–§3.7 and are used verbatim.

**Unidirectional data flow, per screen.** Each destination is a `Route` (state wiring over a
`ViewModel` exposing `StateFlow`) around a pure `Screen` that is a function of its arguments and an
immutable `UiState`, the pattern the accepted precedent `app-laneset` already uses. A `ViewModel` owns
no UI state of its own: it mirrors persisted truth, because a mirror can drift. Dependencies are
constructed by a `ViewModelProvider.Factory` over interface stores — no DI framework — so a screen is
previewable and a test can supply an in-memory store.

**Where the domain boundary sits.** `domain/` is **pure Kotlin with no Android import**. It owns the
mathematics: time, twilight, the dark window, the moon, target altitude/azimuth, optics, the satellite
pass model (as value types), the forecast model (as value types), the horizon profile, and the brief's
assembly. It may not name `Context`, `android.*`, a `CoroutineScope` tied to a lifecycle, or the
system clock — an instant is always passed in. This is what makes the ephemeris unit-testable without
a device or a GPU, and it is the rule the two engines are confined behind: `astronomy.kt` and Orekit
are **implementation details of `data/`** (and of thin adapters), never types the UI can see.

**Why the scene is a rendering concern driven by `SceneFrame`, not the source of truth.** The sky
scene is expensive — a full-screen composited Canvas rebuilt on every scrub frame (§6). If it were the
source of truth, every truth-dependent surface (the brief, the widget, the share text, the reminder)
would depend on a render, and a still frame would have to be produced to answer a question. Instead
the source of truth is a **pure function** `SkySnapshot = Sky.ephemeris(key, scrub)`, and the scene
consumes one `SceneFrame` per instant (annex/CONTEXT.md §3.4: `domain/SceneState.kt` owns `SceneFrame` and
`SceneScrub`). `SceneFrame` carries everything the canvas needs for **one instant** and nothing else;
`SceneScrub` is the scrub position `0…1`. The consequence that matters: **the scene is one consumer of
`SceneFrame`, not its definition** — the brief, the share composer, the widget payload and the reminder
body are other consumers of the same ephemeris, and none of them requires a pixel to have been drawn.

**The capability boundary.** Rendering requirements are owned by `night-scene`; the assembled answer by
`session-brief`; the object browser by `target-catalogue`; pass timing by `satellite-passes`; the
forecast by `cloud-forecast`; saved places and the horizon editor by `scout-spots`; the handoff by
`share-brief`; the widget and reminder by `night-glance`; preferences, Credits and Rate us by
`settings-and-credits`; the first-run screen by `welcome`; and the app's declared identity by
`visual-identity`. This design states the architecture those capabilities share; it states no
requirement that belongs to one of them.

**One asymmetry is on the record rather than implicit: `settings-and-credits` delivers no claim of its
own.** It is supporting surface — a settings list, the Credits screen and Rate us — and it carries no
row in the differentiator contract (§ "What is not yet built"), so its existence is not a reason anyone
installs the app. Every other capability maps to at least one promise.

## Domain model

The types below are the closed vocabulary of `domain/` (annex/CONTEXT.md §3.4). Every value the UI can show
is a named type; nothing the UI could invent is unrepresentable. Field units are stated because a
silently-wrong unit is the defect this factory pays for most often.

- **`domain/Time.kt`** — `NightSpan`: an **absolute UTC** half-open interval `[startUtc, endUtc)`
  carrying the spot it belongs to. `EventInstant`: an absolute UTC instant with the event that
  produced it (rise, set, transit, twilight boundary, pass AOS/LOS). No local clock time is ever a
  stored value; local time exists only as a formatted label (§4, §13).

- **`domain/Twilight.kt`** — `TwilightKind` = `CIVIL | NAUTICAL | ASTRONOMICAL` (the `−6° / −12° /
  −18°` boundaries). `TwilightWindow`: a `TwilightKind` plus its `NightSpan`. `DarkWindowKind` =
  `DARK | POLAR_DAY | POLAR_NIGHT | NO_ASTRO_DARK | NO_DARK` — the five typed outcomes of §4, never a
  bare empty list. `DarkWindowReport`: the `kind`, a `darkWindows: List<NightSpan>`, the measured
  `sunAltMinDeg` / `sunAltMaxDeg`, and the optional `middayTwilightDip` and `bestTwilight` fields the
  polar cases need.

- **`domain/Moon.kt`** — `MoonState`: `illuminatedFraction` (`0.0…1.0`), `phaseAngleDeg`,
  `phaseName` (one of the eight 45°-binned names), `altitudeDeg`, `azimuthDeg`, `magnitude`.
  `MoonlessWindow`: the moonless sub-interval of the dark window, or a typed "none" outcome — never a
  zero-length interval.

- **`domain/SkyPosition.kt`** — `HorizontalCoords(altitudeDeg, azimuthDeg)` with `azimuthDeg` measured
  `0 = North, 90 = East` clockwise; `EquatorialCoords(raDeg, decDeg)`, epoch **J2000.0 / ICRS**.
  **`raDeg` is degrees, never hours** — the one boundary where hours appear is the engine call (§4).

- **`domain/SkyObject.kt`** — `SkyObject` (the catalogue row, §7), `ObjectType` (the closed enum of
  §7), `ObjectKind` = `FIXED | COMPUTED` (an authored catalogue row vs a solar-system body computed at
  the instant), `ObjectVisibility` (above the mask / below / never-up / circumpolar).

- **`domain/TargetWindow.kt`** — `TargetWindow`: a sealed result of **rise/set plus transit**, or
  `Circumpolar(minimumAltitudeDeg, atInstant)`, or `NeverRises` — each a distinct case, because the
  natural implementation returns an empty window and that is the bug §16 guards against
  (`specs/target-catalogue/spec.md`).

- **`domain/PassFinder.kt`** — `SatellitePass` (AOS/LOS, `maxElevationDeg`, the geodetic sub-point
  track, and the `occurred` vs `observable` split), `PassVisibility` (`OBSERVABLE` / `IN_EARTH_SHADOW`
  / `TOO_BRIGHT`), `ElementSetAge` (an age in hours with the threshold band it falls in, §8).

- **`domain/CloudForecast.kt`** — `CloudHour(hourUtc, cloudCoverPercent 0…100)`; `CloudReport` (the
  hours, a `ForecastAge`, and the source label "MET Norway"); `ForecastAge` (age with its fetch instant).
  **Total cloud only** — there is no low/mid/high field, because the source has none (annex/N-data-algorithms.md §4.3).

- **`domain/HorizonProfile.kt`** — `HorizonProfile`: `id`, `name`, the place it was drawn at
  (`latitudeDeg`, `longitudeDeg`), and **`altitudeDeg: FloatArray` of exactly 360 samples** — one per
  degree of azimuth `0…359°` (geographic north, clockwise), elevation in degrees `−10.0…+90.0`
  (annex/CONTEXT.md §3.4, "the 360-sample array"); this in-memory array is serialised as the
  **720-byte** signed-16-bit buffer described under **Persistence**. `HorizonSample(azimuthDeg, altitudeDeg)` is the element
  type. Occlusion is an `O(1)` array lookup (§6).

- **`domain/Optics.kt`** — `LensPreset` (sensor width/height in mm, focal length in mm, name),
  `FieldOfView(fovH, fovV, fovDiag)` in degrees, `TargetFit(objectAngularSizeDeg, frameFovShortSideDeg,
  frameFrac, pixelsOnSensor)`. Focal length and sensor dimensions are **millimetres everywhere and are
  never unit-converted** (§15, and the E71 edge).

- **`domain/SceneState.kt`** — `SceneFrame`: the immutable per-instant payload the canvas consumes
  (sun/moon altitude and azimuth, ε, air mass, the star/sky rotation, the moon phase and libration, the
  target and pass positions, the active twilight band). `SceneScrub`: the scrub value `0…1` and the
  instant it resolves to — a **read-only state** inside the draw lambda (§6).

- **`domain/Scout.kt`** — `Scout` (a saved spot: `id`, `name`, `latDeg`, `lonDeg`, `elevationM`, IANA
  `tzId`, optional `HorizonProfile` id), `EquipmentProfile` (focal length mm, sensor mm, a chosen lens
  preset). A spot's zone is an **IANA zone id, never a numeric offset** (§4).

- **`domain/Brief.kt`** — `SessionBrief`: the assembled answer for one (spot, night) — the light and
  dark windows, the twilight table, the moon, the target list with their windows, the passes, the
  forecast, and the `SkyGrade`. `BriefCondition(conditionKind, severity, text)`, `ConditionKind` = the
  closed set of things the brief can say (dark present / no astronomical dark / polar day / …).

- **`domain/SkyGrade.kt`** — `SkyGrade`: the computed darkness description (darkest Sun altitude, the
  dark-window kind and duration, the moon's fraction × altitude). **Never a Bortle class and never
  mag/arcsec²** — Bortle is only ever the user's own labelled estimate (§1, §15).

**Relationships.** A `Scout` **has** an optional `HorizonProfile` and an `EquipmentProfile`; a
`SessionBrief` is a **function of** (Scout, night date) and **references** `TargetWindow`s by target
id, `SatellitePass`es by NORAD catalog id, and a `CloudReport`; a `SceneFrame` is a **projection of**
the same ephemeris the `SessionBrief` is assembled from. Nothing in `domain/` holds a cache, a
`Context`, or a clock — those are `data/` concerns.

## Time and geometry

This section is the mathematics the app must get exactly right. Every constant carries its unit and
its source; the two "same word, different test" traps and the RA-unit trap are the reason this section
exists.

**Time model.** All computation is in **UTC**. A spot stores `{id, name, latDeg, lonDeg, elevationM,
tzId}`, with `tzId` an **IANA zone id** (`"Europe/Oslo"`), never a numeric offset, so DST is resolved
by the platform tz database (`java.time.ZoneId`, available at `minSdk = 26`, so no desugaring). A
session's night is the spot's **local-noon-to-local-noon** window: a sunset on local day *N* and the
sunrise on *N+1* are **one** session. The one place a UTC instant becomes a local date is the brief's
`dateLabel`, computed with the spot's `ZoneId` (annex/N-data-algorithms.md §2.1, §2.6).

**Twilight angles and the sunrise test are two different tests — do not interchange them**
(annex/N-data-algorithms.md §2.2, MEASURED from USNO <https://aa.usno.navy.mil/faq/RST_defs>):

| Boundary | Sun-centre **geometric** altitude | Test the app must call |
|---|---:|---|
| Civil twilight | **−6.0°** | centre-of-body crossing, **no refraction, no semidiameter** |
| Nautical twilight | **−12.0°** | centre-of-body crossing, **no refraction, no semidiameter** |
| Astronomical twilight | **−18.0°** | centre-of-body crossing, **no refraction, no semidiameter** |
| Sunrise / sunset | **−0.8333°** (zenith distance **90.8333°** = **50′** below the horizon = **16′** semidiameter + **34′** refraction) | rise/set with semidiameter + horizon dip + near-horizon refraction |

The engine distinction is measured: `searchAltitude(...)` passes `bodyRadiusAu = 0.0` with no dip and
no refraction (`astronomy.kt:6911-6918`), and `searchRiseSet(...)` includes semidiameter, dip and
`REFRACTION_NEAR_HORIZON` scaled by density (`astronomy.kt:6818-6845`). **The app must not use −18°
for sunset, and must not use `searchRiseSet` for the dark window** — using the wrong one shifts the
dark window by minutes.

**The dark-window algorithm** (the `night-scene` and `session-brief` capabilities own the requirements;
this is the algorithm):

1. `sunset = searchRiseSet(Sun, obs, Set, localNoon, 1.0)`;
   `sunrise = searchRiseSet(Sun, obs, Rise, sunset, 1.0)`.
2. Sample the Sun's altitude over `[localNoon, localNoon + 24 h]` at **10-minute** steps; record
   `sunAltMinDeg`, `sunAltMaxDeg` and the times of the extrema.
3. Scan for **every crossing of −18.0°** with a **1-hour re-armed probe loop** (§5) — a single long
   call can step over a short window. Each crossing pair becomes one `[start, end)` interval.
4. Emit a **`darkWindows` array plus a `kind` discriminator** (this is the design point: at 48–55°
   latitude near the solstice a session can have **two** dark windows separated by a twilight dip, and
   a single-interval output cannot express it).

**The typed outcomes — one per polar and high-latitude case** (annex/N-data-algorithms.md §2.3; the enum values are
`DarkWindowKind` from annex/CONTEXT.md §3.4):

| `DarkWindowKind` | Condition | Required output |
|---|---|---|
| **`DARK`** | one or more −18.0° crossing pairs found | `darkWindows: [{startUtc, endUtc, durationMin}]`, `sunAltMinDeg` |
| **`NO_ASTRO_DARK`** | no −18.0° crossing, and `sunAltMinDeg < −6.0°` | `bestTwilight = NAUTICAL` and the −12.0° intervals; if `−12.0° ≤ sunAltMinDeg < −6.0°` then `bestTwilight = CIVIL` and the −6.0° intervals |
| **`NO_DARK`** | `sunAltMinDeg ≥ −6.0°` | `{kind:"no-dark", sunAltMinDeg}` — no window of any kind |
| **`POLAR_DAY`** | `searchRiseSet(Sun, Set, …)` returns `null` **and** `sunAltMinDeg > −0.8333°` | `{kind:"polar-day", sunAltMinDeg, sunAltMaxDeg}` — **no window at all**, and the UI says so in words, never an empty list |
| **`POLAR_NIGHT`** | `searchRiseSet(Sun, Rise, …)` returns `null` **and** `sunAltMaxDeg < −0.8333°` | if `sunAltMaxDeg ≤ −18.0°`: `{kind:"dark", darkWindows:[{startUtc = localNoon, endUtc = localNoon + 24 h}], wholeNightDark:true}`; else `{kind:"polar-night", darkWindows:[…], middayTwilightDip:{startUtc, endUtc}}` |

Three universal rules the `night-scene` capability must state: the window is `[start, end)`; durations
are computed in **UTC** and **never** from two formatted date strings; and a `null` from an engine call
is a **case**, not an error — it maps to a `kind`, never to an empty list shown as "no darkness
tonight". The polar cases are not exotic: astronomical darkness is absent at ~52.5° N for roughly two
months of summer, so `NO_ASTRO_DARK` / `NO_DARK` is the ordinary June case for most of Europe
(INFERRED — standard geometry, annex/N-data-algorithms.md §2.3).

**The moon** (annex/N-data-algorithms.md §2.4): phase is the **elongation**, Moon's ecliptic longitude minus the Sun's, in
degrees `[0,360)` — `moonPhase(time)`, `astronomy.kt:6173`; 0° = new, 180° = full. Illuminated fraction
is `illumination(Body.Moon, time).phaseFraction` in `[0.0, 1.0]`; phase angle is `.phaseAngle` in
degrees. The implementation may assert `k = (1 + cos(phaseAngle)) / 2` in a unit test. The eight phase
names are mapped from the elongation in **45° sectors offset by 22.5°**, so a waxing crescent near 359°
does not fall into the wrong bucket. **"Moonless" is two booleans and one stated threshold**:
`moonUp = (moonAltDeg > 0.0)`; `darkMoon = (illuminatedFraction ≤ 0.10)` — **a product threshold,
stated as such**; `session.moonless = (!moonUp) || darkMoon`. The UI must show **both** numbers
(fraction and altitude), because a 5 % crescent 10° above the horizon is not moonless in practice and a
single boolean would hide that.

**Target altitude, azimuth and refraction** (annex/N-data-algorithms.md §2.5): every quantity comes from
`horizon(time, observer, ra, dec, refraction) → Topocentric{azimuth, altitude, ra, dec}`; `azimuth` is
**0 = North, 90 = East**, `altitude` is degrees above (+) or below (−) the horizon, and the returned
`ra` is in **sidereal hours** (a second hours/degrees boundary the implementer must not miss). Use
**`Refraction.Normal`** (Saemundsson) for a target's displayed altitude and for the user's altitude
mask, and **no refraction** for the −6/−12/−18° twilight tests; the toggle is **per use, never
global**. `Refraction.Normal` is Saemundsson's — `angle_deg = (1.02 / tan(h + 10.3/(h + 5.11))) / 60.0`
with `h = max(altitude, −1.0°)` and a taper `(altitude + 90.0) / 89.0` below −1° so refraction reaches
zero at the nadir (`astronomy.kt:4015-4031`; `REFRACTION_NEAR_HORIZON = 34.0/60.0` degrees at `:196`,
matching USNO's 34′). **"Up" for a target** means `altitudeDeg(Normal) ≥ maskDeg`, default mask
**10.0°**, user-adjustable.

**Rise / set / transit / circumpolar** (annex/N-data-algorithms.md §2.5): classify by sampling `altitude(t)` at **10-minute**
steps across the session window and computing `min` and `max` — `min > maskDeg` →
`Circumpolar(minimumAltitudeDeg, atInstant)` (the target is up all night, no rise/set); `max < maskDeg`
→ `NeverRises` (the UI must say so, not render an empty window); otherwise find boundaries with
`searchRiseSet(...)` and **refine the crossing to the mask**
with `searchAltitude(..., maskDeg)`, because `searchRiseSet` crosses the *horizon*, not the user's
mask. Transit is the time of maximum altitude, found by the 10-minute scan refined to **1 minute**
(whether `astronomy.kt` exposes `searchHourAngle` was **not** checked — §16).

**Reference frames and sign conventions** (annex/N-data-algorithms.md §1.3, §2.5):

- **Epoch J2000.0, ICRS-aligned.** The catalogue declares `epoch="J2000.0"`; no precession is applied
  to the stored numbers; the engine's `horizon()` performs the time-dependent transformation.
- **`Observer(latitude, longitude, height)`** — latitude degrees **N+ / S−**, longitude degrees
  **E+ / W−**, `height` in **metres** above sea level (`astronomy.kt:1412-1431`). A spot stored
  west-positive silently mirrors the sky.
- **The RA-degrees-versus-hours trap.** The catalogue stores right ascension in **degrees,
  `[0, 360)`**. The engine's `horizon()` and `defineStar()` take RA in **hours** — `horizon()`
  converts internally with `sinra = dsin(ra * 15.0)` (`astronomy.kt:5400-5413`), and
  `defineStar(body, ra, dec, distanceLightYears)` documents `ra` in **hours `[0,24)`**
  (`astronomy.kt:452`). **The conversion `ra × 15.0` happens at exactly one boundary** — the
  `data/`↔engine adapter — and nowhere else; hours are never stored.

## The event-search rule

**The measured trap, stated once and applied everywhere.** A single `propagate()` call over a long
window, with an `ElevationDetector` attached, finds a fraction of the events that exist: over **3
days** at a 10° mask it reported **1** AOS where a 60-second brute-force elevation scan found **15**,
and tightening `maxCheck` from 600 s to 10 s changed nothing. Re-segmenting the same propagation into
1-hour windows (with `maxCheck = 60 s`) found all **15** AOS, agreeing exactly with the brute-force
scan. This was reproduced three times and is byte-identical across runs (MEASURED — annex/N-data-algorithms.md §3.5, and
`F-engine-gap-map.md` §4.2 (not copied into this change) citing `run-passvariants.txt` and
`run-passcrosscheck.txt` in the same not-copied `.kiln/scratch/positioning-2026-10-08/` directory).

**The rule, in one sentence: no event search is ever a single call over a long window — it is
segmented, or stepped and refined, and it returns a list of crossings, never a single crossing.**

The discipline, precise enough to implement:

- **Applies to every event search, not only satellites.** Twilight crossings, moonrise/moonset, and
  target rise/set over a multi-day or polar window have the same shape and the same failure; the
  satellite case is only the one that was reproduced. The dark window's −18° scan (§4, step 3) is a
  **1-hour re-armed probe loop** for exactly this reason (annex/N-data-algorithms.md §2.3).
- **Satellite propagation:** the session's dark window, capped at **24 h** in UTC, walked in
  **3600 s** segments. For **each** segment a **fresh** `ElevationDetector` is built with
  **`maxCheck = 60 s`** and the default threshold (`1e-6`), attached to a propagator **started at the
  segment's `t`**, and propagated to `tNext`. **The re-arm is the point**: a detector that has already
  fired is not re-armed inside one call.
- **The interval length is 3600 s** and **`maxCheck` is 60 s**; a one-hour segment is short enough
  that no pass is missed and long enough that a whole night costs ≤ 24 propagations. A 60-second brute
  scan at runtime is **not** the algorithm — it is the *check*.
- **Both directions are collected** (acquisition with `g` increasing, loss with `g` decreasing) and
  paired into passes; a pass is kept only if `maxElevationDeg ≥ maskDeg`.
- **A test must assert the segmented finder returns the same AOS count as a brute-force 60-second
  scan** (the 3-day / 10° case returns 15), because one probe row labelled `segmented-10m` actually
  passed `segmented=false` — so the label alone is not evidence (annex/N-data-algorithms.md §3.5).

## The sky scene pipeline

The rendering contract is `annex/O-sky-scene-rendering.md`; the `night-scene`
capability owns its normative requirements. This section states the pipeline the implementation uses.

**The 13-layer stack.** One Compose `Canvas` inside a `Box`, painted bottom-first (annex/O-sky-scene-rendering.md §1.1):

| # | Layer | Renderer |
|---|---|---|
| L1 | Sky field (twilight radiance) | fallback: one `1 × Hpx` `ImageBitmap` strip drawn 1:1 · API 33+: an AGSL `RuntimeShader` over the scene rect |
| L2 | Night floor (airglow + zodiacal + integrated starlight) | additive vertical gradient / an addend in the shader |
| L3 | Milky Way composite | `drawVertices` over a 48×24 quad mesh with an `ImageShader` |
| L4 | Star field | ≤ 8 batched `drawPoints` calls, `BlendMode.Plus`, pre-rendered sprites per (magnitude bin × colour bucket) |
| L5 | Deep-sky targets + constellation figures | a `Path` per object; IAU figure polylines |
| L6 | Moon | `drawImage` clipped to the disc, a phase `Path`, a radial-gradient glow |
| L7 | Satellite trail | one fading `Path`, split at horizon crossings |
| L8 | Earth-shadow wedge | **not an overlay** — a modulation applied inside L1's evaluation |
| L9 | Alpenglow band | one additive `Brush.radialGradient`, clipped above the ridge |
| L10 | Horizon silhouette | one opaque `Path` (the ridge) + optional tree glyphs |
| L11 | Lens FOV frame | four corner ticks + a hairline rect |
| L12 | Target marker + label | marker (occluded by L10) + a label in the overlay plane |
| L13 | UI chrome | Compose `MaterialTheme` surfaces |

Three load-bearing consequences: **L8 is inside L1, not above it** (the Earth's shadow dims scattered
sunlight, not starlight — a wedge drawn over L4 would extinguish the dark antisolar stars); **L10
occludes by draw order and nothing else**; and **L12 splits** so the ridge occludes the marker while
the label is drawn after and clamped.

**The LUT axes and colour-space discipline.** Two committed binary tables, `assets/sky/sky_lut_a.bin`
and `sky_lut_b.bin` (`sky_lut.bin` in O), produced by **`apps/<slot>/tools/bake-sky-lut.py`**, a baker
the builder runs **once**, writing both files into `app/src/main/assets/sky/` — **the app never computes
the table at runtime and never on first launch** (annex/O-sky-scene-rendering.md §1.2, §2.3). Axes:

| Axis | Quantity | Range | Step | Samples |
|---|---|---|---|---|
| A1 | solar altitude ε | **+10.0° … −20.0°** | **0.5°** | **61** |
| A2 | view elevation h | **0.0° … 90.0°** | **1.0°** | **91** |

**The on-disk format is specified, so an implementer guesses nothing.** Both files share one layout,
written **little-endian**:

| Offset | Length | Content |
|---:|---:|---|
| `0` | 8 B | magic ASCII **`LUMSKY01`** |
| `8` | 4 B | `formatVersion`, `uint32` = **1** |
| `12` | 2 B | `a1Count`, `uint16` = **61** (solar altitudes) |
| `14` | 2 B | `a2Count`, `uint16` = **91** (elevations) |
| `16` | 4 B | `a1StartDeg`, `float32` = **+10.0** |
| `20` | 4 B | `a1StepDeg`, `float32` = **0.5** (descending towards −20.0°) |
| `24` | 4 B | `a2StartDeg`, `float32` = **0.0** |
| `28` | 4 B | `a2StepDeg`, `float32` = **1.0** |
| `32` | 1 B | `channelCount`, `uint8` = **3** |
| `33` | 31 B | zero padding, to the fixed **64-byte** header |
| `64` | 244 B | `E(ε)` — **61 × `float32`**, solar altitude +10.0° → −20.0° in 0.5° steps |
| `308` | 244 B | `α_s(ε)` — **61 × `float32`**, the Earth-shadow edge at the same altitudes |
| `552` | 33,306 B | the cells — **61 × 91 × 3 × `float16`**, channel order **R, G, B** |

Loading validates the header **before any cell is read**: a file whose magic, `formatVersion`, axis
counts or axis bounds disagree with the expected shape is **rejected with a typed error**, never read
past. One file is therefore **33,858 B** (64 + 244 + 244 + 33,306) and the pair is **67,716 B**
(≈ 66.1 KiB) — this specification's number, computed from the axes above.

**`LUT_A` is the antisolar meridian (Δaz = 180°), the fallback path's entire sky; `LUT_B` is the solar
meridian (Δaz = 0°), used only on the API 33+ path to blend** by `w(Δaz) = (1 + cos Δaz)/2`, in linear
light. Each cell stores **linear-light radiance relative to the zenith exposure `E(ε)`, as three
`float16` channels** (`L_abs = L_rel × E(ε)`). **Colour-space discipline, one stage per line, no stage
skipped:** the baker integrates in **linear spectral → linear CIE XYZ (D65)**; cells are **linear sRGB
float16 relative to `E(ε)`**; interpolation at render (bilinear in ε/h, then the azimuth `lerp`) is
**linear** and is the **only** interpolation over sky colour; white balance is one fixed **linear** von
Kries matrix D65 → display white; the tonemap is the Narkowicz (2015) ACES approximation
`L' = L(2.51L + 0.03) / (L(2.43L + 0.59) + 0.14)`; the **sRGB piecewise transfer (IEC 61966-2-1)**,
not a 2.2 power; ±0.5/255 tiled **blue-noise dither added in the encoded domain immediately before
quantisation**; and the 8-bit quantisation is **the only 8-bit step in the entire pipeline**.

**Star rendering parameters.** Stars are viewpoint-aligned billboards carrying a Point Spread Function,
batched by magnitude and colour — never `drawCircle` per star and never a shader loop (annex/O-sky-scene-rendering.md §3.1). The
parameters:

- Flux ratio to the control magnitude: `Δm(m) = 2.512^(m_a − m)`, **`m_a = 4.0`**.
- Screen intensity: `I = c_q · Δm(m) · O(h) · S(h,t) · a_dark(ε)`, with `c_q` calibrated so a
  `V = 6.5` star at `h = 60°` at `ε = −15°` renders at **1/255** above the local sky (ours, INFERRED).
- Core radius: `r_core = max(√2 px, 0.5 dp)` — **a constant for every star**; on the A40 (480 dpi =
  3 device px per dp) it resolves to **1.5 device px**. Brightness, never size, carries the magnitude.
- Glare halo (the only place a radius grows): `I_G = max(0, Δm − 1)`; `r_glare = max(r_core,
  c_k · √I_G)` with **`c_k = 0.50 px`**.
- PSF: `P(l) = A_core·T(l) + A_glare·G(l)`, with `T(l) = 2l³ − 3l² + 1` (the paper's intensity PSF)
  and `G(l) = (1 − l²)²` (**ours** — a band-limited substitute, because the paper's glare profile is
  ambiguous in the OCR and inventing a constant is banned).
- Extinction: `O_c(h) = exp(−β_r[c] · (Φ(h) − 1))`, `β_r = (0.16, 0.37, 0.91)` for sea level,
  `Φ` the Kasten–Young (1989) air mass, `Φ(h) = 1 / (cos z + 0.50572·(96.07995° − z)^−1.6364)`,
  `z = 90° − h`.
- Scintillation: `σ(h) = s_0 · (Φ(h) − 1)^1.5`, **`s_0 = 0.12`**, one-pole low-pass with τ = 60 ms,
  plus a ±0.15 px positional jitter.
- Count: **0.5-mag bins from `V = −1.5` to `6.5` → 16 bins**; **8 magnitude bins × 6 colour buckets =
  48 sprites**; the default 73° × 53° field renders **≈ 850 stars** (range 700–1,100 asserted); the
  faint 2MASS field beyond `V = 6.5` appears only on zoom.

**Where the optional AGSL path sits, and how divergence is prevented.** AGSL is Android 13+ (**API
33+**), so the A40 (API 30) **never executes the shader** and the fallback is the product on the
factory's own test phone (annex/O-sky-scene-rendering.md §0, §8). The two paths are selected by one `when` over `SDK_INT`, with no
reflection, no `Class.forName`, and no try/catch used as a version check. **AGSL is additive, never
load-bearing**: deleting the shader source must leave the module compiling and the screen structurally
unchanged. Divergence between the two is prevented by four mechanisms, in increasing strength:

1. **One source of truth** — both paths sample the `sky_lut_a.bin` / `sky_lut_b.bin` tables; neither
   has its own palette, stops or tonemap curve. (The mockup's hand-authored stop list is a mockup
   device and is **not** shipped.)
2. **One implementation of the last stages** — `tonemapACES`, `whiteBalance`, `srgbEncode`, `dither`
   are declared once in Kotlin and once in AGSL, and a unit test transcribes **both** from the shipped
   text and compares.
3. **A debug golden-image test** renders the same input through the CPU path and through the shader
   into two offscreen bitmaps and **fails if any channel differs by more than 2/255 over 256 elevations
   × 61 solar altitudes** — runnable only on an API 33+ device or emulator.
4. **`python scripts/visual-qa.py --slot lumintra-light-planner`'s palette/contrast sections** read the
   shipped pixels, so a drifted path is visible in the pack as well as in the debug diff.

**The shader-path parity claim is UNVERIFIED on the test device**, because the A40 cannot run the
shader; §16 names the two-device check.

## Sky assets and attribution

Four decisions, settled rather than left conditional, because each is a licence term as much as a
build input. Every shipped image carries its credit **where the user sees it**, not only in the credits
list, and the licence text of every third-party asset is committed with the slot.

- **The Moon** ships the **NASA SVS** Moon Kit image (`assets/sky/moon_svs.webp`), which is **public
  domain**. Its obligation is an acknowledgement and **no agency insignia**, and it is **never**
  credited to JPL-Caltech. Source: NASA Scientific Visualization Studio (annex/CONTEXT.md §2; annex/N-data-algorithms.md §1.4).
- **Planets ship no surface textures.** A planet is drawn as a **magnitude-scaled disc with a label**
  (L6), which is also the astronomically honest rendering at naked-eye scale. The Solar System Scope
  texture set and its credit row are **removed** from this design, and no
  `assets/sky/planets/<name>.webp` ships.
- **The Milky Way** is **self-baked** from the ESO panorama `eso0932a` (S. Brunier), **CC BY 4.0**. The
  credit **`ESO/S. Brunier`** appears **beside the image** — on the scene and on the shared postcard,
  not only in the credits list — with the licence link `https://www.eso.org/public/images/eso0932a/`,
  unaltered and with no implied ESO endorsement. Target asset:
  `assets/sky/milky_way_eso0932a.webp`, an **equirectangular** panorama of at most **4096 × 2048**,
  **WebP, under 4 MB**; if the bake is **AI-modified**, the modification is declared with the image.
- **Constellation figures** ship from the **IAU / Sky & Telescope CC BY 4.0** set, carrying the exact
  credit `Constellation figures: IAU and Sky & Telescope (Roger Sinnott, Rick Fienberg; patterns by
  Alan MacRobert) — CC BY 4.0`, reproduced in full, with no IAU logo. The
  `dcf21/constellation_lines_iau.dat` pseudo-source is **rejected**: its description says CC BY 4.0
  while its repository footer says GPL v3, and a measured conflict is not a licence (§7).

**The FK6 star catalogue count is live, not fixed.** The shipped FK6 copy holds **4,151 stars** (the
"~4,150" a source report carried is a rounding) — and because FK6 **omits Sirius, Arcturus, Capella and
Betelgeuse** (its brightest row is `alpha Car` at `−0.62`), a **hand-authored supplement of the
brightest stars is mandatory**, each row verified against two independent sources by the same `annex/N-data-algorithms.md` §1.4
procedure. The supplement's presence and row count are asserted by a unit test, because an app whose
brightest star is missing is the exact defect this clause exists to prevent.

## The catalogue

The catalogue ships as **three authored or baked data shapes plus one computed appendix**, all under
`app/src/main/assets/catalog/` (annex/CONTEXT.md §3.7; annex/N-data-algorithms.md §1). The `target-catalogue` capability owns the
normative requirements; this is the data architecture.

**Three data shapes.**

- **`assets/catalog/objects.json`** — the authored fixed-coordinate deep-sky rows (250). Format
  `{"schemaVersion": 1, "objects": [...]}`. Each row: `id` (ASCII lowercase slug), `messier`/`ngc`/`ic`
  (string or null), `name`, `nameRu` (hand-authored; `ru` is a declared locale), `type` (the closed
  enum, below), **`raDeg` in degrees `[0,360)` ICRS**, **`decDeg` in degrees `[−90,+90]`**, `epoch`
  (constant `"J2000.0"`), `magV`/`magB` (or null), `magSystem` (`"V" | "B" | null` — which of the two
  is authoritative), `magKind` (`"integrated" | "peak" | "unknown"`), `sizeMajArcmin`/`sizeMinArcmin`,
  `positionAngleDeg` `[0,180)`, `aliases`, `commonNames`, `sourceIds` (`{wikidata, heasarc, simbad}`),
  `verification` (`{srcA, srcB, deltaArcmin}`), `magDisputed` (boolean), `notes`. Derived at load:
  `raHours = raDeg / 15`.
- **`assets/catalog/constellations.json`** — the **88** IAU constellations, a **different record
  shape** (a region, not a point): `{id, nameEn, nameRu, nameLatin, abbrev, raCenterDeg, decCenterDeg,
  areaRank}`.
- **`assets/catalog/stars.bin`** — the FK6 ⊕ supplement star table, baked: per star **RA, Dec
  (2 × float32 = 8 B)**, **V (float16, 2 B)**, **colour-bucket index (uint8, 1 B)** = **11 B/star**.
  Its row count is **FK6's 4,151 rows plus the mandatory hand-authored brightest-star supplement**
  (§ "The FK6 supplement is mandatory"), so the table's size is **computed, not measured**, from
  that count: **4,151 × 11 B = 45,661 B ≈ 44.6 KiB**, rising by exactly **11 B for every
  supplement row** the supplement adds (at least the four FK6 lacks). Proper motion is deliberately
  **not** carried (far below one pixel in one night). The **faint 2MASS field beyond `V = 6.5`** named in
  §6 is a **separate** layer, **not** part of this FK6 ⊕ supplement table: it appears only on
  zoom, and its own numbers stay with it there — it does not set `stars.bin`'s size.

**The computed appendix — the solar-system section.** Sun, Moon, Mercury…Neptune, and (optional)
Pluto are **computed, never authored** — a planet has no fixed RA/Dec — and are *presented* with the
same view fields as a catalogue row. At the observation time the app builds a computed record with
`id` (`"planet-mars"`, `"moon"`, `"sun"`), `type` (`planet | moon | sun`), geocentric apparent RA/Dec,
`azimuthDeg`/`altitudeDeg` from `horizon(...)`, `magnitude` from `illumination(body, t).mag`,
`phaseAngleDeg`, `illuminatedFraction` `[0,1]`, and `ephemeris = "computed"` so the UI can distinguish
it from an authored row. **Engine assignment:** Sun/Moon/planets come from `astronomy.kt`
(VSOP87/ELP built in, **zero data files**), **not** Orekit — Orekit's planetary ephemeris needs JPL DE
kernels of tens of MB and is used **only for satellites** (one 3,321-byte file). The two engines are
not interchangeable for a given body.

**The `type` enum is a closed set:** `galaxy, galaxy_group, globular_cluster, open_cluster,
emission_nebula, reflection_nebula, planetary_nebula, supernova_remnant, dark_nebula, asterism,
double_star, planet, moon, sun` — the last three used only by the computed appendix, never authored
into `objects.json`.

**Selection rule (arithmetic, not taste).** **All `M1–M110` → 110 Messier rows** (M102 is the documented
duplicate/`notes` case), **plus the 140 brightest non-Messier NGC or IC objects whose sourced `magV ≤
10.0` → 140 rows**, so the authored deep-sky table is **110 + 140 = 250 rows**; the magnitude limit is
raised to **10.5 only if fewer than 140 non-Messier objects qualify at 10.0**, is never raised further,
and is capped to the 140 brightest when more qualify. The 250 authored rows are presented together with
the **10 computed Solar-System records**. The manifest records the shipped row count, the non-Messier
cap applied and the magnitude limit actually used, because "about 250" is not checkable
(`specs/target-catalogue/spec.md`).

**How the rows are authored and independently verified** (annex/N-data-algorithms.md §1.3, §1.4) — this is the lawfulness
mechanism:

1. Designation and name from the **historical catalogues themselves** (Messier 1781; Dreyer's NGC 1888
   and IC 1895–1908), public domain by age; no modern compilation is copied.
2. **Coordinates** from **Wikidata** `P6257` (RA degrees) and `P6258` (Dec degrees) — **CC0 1.0** —
   as the primary source; coverage is measured to be **not uniform**, so rows where Wikidata is missing
   or coarse are sourced by hand and recorded as such.
3. **A second, independent source** for coordinates — **NASA HEASARC** (freely usable, US government)
   and/or the discovery paper.
4. **The discrepancy is computed and recorded**: `Δ = (180/π)·acos(sin δ₁ sin δ₂ + cos δ₁ cos δ₂
   cos(α₁ − α₂))`, α,δ in radians. **Acceptance: `Δ ≤ 1.0 arcmin` for objects with
   `sizeMajArcmin ≥ 5`, `Δ ≤ 0.2 arcmin` for unresolved objects.** A row outside tolerance is
   **re-sourced or dropped, never averaged.**
5. **Magnitudes**: take the published V (or B), cross-check a second reference; **when two sources
   disagree by more than 0.50 mag, do not average** — set `magDisputed = true`, store both in `notes`,
   prefer the source the entry names as primary. `magSystem`/`magKind` are filled from the same source,
   never inferred. **No comparison ever mixes V with B, or integrated with peak** — the engine's own
   `magnitude()` / `illumination()` apply **only to solar-system bodies** and must not be asked of a
   deep-sky object.
6. Size and position angle from the same references, stating which optical extent is used.

**Why no third-party catalogue ships** (annex/N-data-algorithms.md §1.4, MEASURED this session). No permissively-licensed
complete deep-sky catalogue exists: **OpenNGC and HYG are CC BY-SA 4.0**; **Hipparcos/Tycho and Gaia
DR3 are CC BY-NC**; **Stellarium's is GPL**; **NGC 2000.0 is non-commercial**; **Yale BSC5 carries no
SPDX licence**; and Wikidata, though CC0, was **measured not to be enumerable as a catalogue**
(instance-of-Messier returned 0; the global NGC/IC counts returned HTTP 504/429). Designations,
coordinates and magnitudes are **facts**, so the lawful route is to author a small table of facts and
verify each row against two independent references. The denylist is explicit: `dcf21/constellation_lines_iau.dat`
is **not** shipped (its file description says CC BY 4.0 while the repository footer says GPL v3 — a
measured conflict).

**The FK6 supplement is mandatory** (annex/N-data-algorithms.md §8 rows 7–8). **FK6 is CC0 1.0, 4,151 rows** (the live TAP
`SELECT COUNT(*)` governs over the recon's 4,150), V range **−0.62 … 9.71** — and it **lacks Sirius,
Arcturus, Capella and Betelgeuse**, its brightest entry being `alpha Car` at `−0.62`. FK6 is a
*fundamental* astrometric catalogue, not a naked-eye list, so the app ships a **hand-authored
supplement of the brightest stars**, verified against two independent sources by the same `annex/N-data-algorithms.md` §1.4
procedure, or the scene omits the sky's most recognisable stars. The supplements and FK6 are merged
and baked into `stars.bin` at build time.

## Engines and dependency facts

Two engines, recorded with the coordinates, versions and licence texts that were actually proven —
read from the jars' own metadata and from a working smoke project, never restated from memory.

**Orekit 13.1.9 and Hipparchus 4.0.3 — satellites and topocentric geometry.** `org.orekit:orekit:13.1.9`
and eight `org.hipparchus` 4.0.3 artifacts are the set; a slot declares the same coordinates
through its version catalog, while the scratch smoke project that proved them vendored the jars
directly. **The pin is the latest stable release, measured 2026-10-08:**
`https://repo1.maven.org/maven2/org/orekit/orekit/maven-metadata.xml` returns `<latest>13.1.9</latest>`,
`<release>13.1.9</release>`, `<lastUpdated>20261003193228</lastUpdated>` — so the smoke's `13.1.5`
(published 2026-05-03) is four patch releases behind and is **not** the coordinate a slot declares.
Hipparchus is unaffected: both POMs carry `<orekit.hipparchus.version>4.0.3</orekit.hipparchus.version>`.
The exact jars, with measured byte sizes (MEASURED this session,
`.kiln/scratch/android-orekit-smoke/app/libs/` for the smoke set, and
`.kiln/scratch/lumintra-spec-close/orekit-13.1.9.jar` for the pin):

| Coordinate | Jar | Bytes |
|---|---|---:|
| `org.orekit:orekit:13.1.9` | `orekit-13.1.9.jar` | 8,538,135 |
| `org.hipparchus:hipparchus-core:4.0.3` | `hipparchus-core-4.0.3.jar` | 1,959,795 |
| `org.hipparchus:hipparchus-filtering:4.0.3` | `hipparchus-filtering-4.0.3.jar` | 31,650 |
| `org.hipparchus:hipparchus-fitting:4.0.3` | `hipparchus-fitting-4.0.3.jar` | 31,076 |
| `org.hipparchus:hipparchus-geometry:4.0.3` | `hipparchus-geometry-4.0.3.jar` | 276,264 |
| `org.hipparchus:hipparchus-ode:4.0.3` | `hipparchus-ode-4.0.3.jar` | 282,008 |
| `org.hipparchus:hipparchus-optim:4.0.3` | `hipparchus-optim-4.0.3.jar` | 228,802 |
| `org.hipparchus:hipparchus-stat:4.0.3` | `hipparchus-stat-4.0.3.jar` | 222,980 |

The Hipparchus versions are read from each jar's `META-INF/maven/**/pom.properties`
(`version=4.0.3`; MEASURED). The pinned Orekit jar is identified by its own manifest,
`Bundle-Version: 13.1.9`, and by **sha256 `A1DBEC16…0FBE`** (8,538,135 B, MEASURED 2026-10-08);
the smoke's vendored copy is 13.1.5 — 8,527,112 B, sha256 `71F995BA…373E`,
`Implementation-Build: 7b39f84999bca5b51b11f216befd6555116f8f52; 2026-05-02T13:14:03Z` — and
**every reading in the two paragraphs below was taken on it**. A slot on 13.1.9 re-takes them; that
is a task in `tasks.md`, never a claim carried over. **Licence: Apache-2.0.** The Orekit POM declares
`<name>The Apache Software License, Version 2.0</name>` and the Hipparchus jar's own POM carries the
Apache-2.0 header (`... under the Apache License, Version 2.0`); both jars ship `META-INF/LICENSE.txt`
and `META-INF/NOTICE.txt`, and the **LICENSE.txt / NOTICE.txt text is committed with the slot**
(the redistribution obligation travels with the APK), not merely referenced.

**Proven on 13.1.5 to build and to shrink under R8 full mode with zero keep rules.** A `minifyEnabled = true` +
`shrinkResources = true` release build passed with **no keep rules**, **220/249 `org.orekit.*` and
49/59 `org.hipparchus.*` classes surviving**, and **one non-fatal R8 warning about
`META-INF/services/com.sun.source.util.Plugin`**. The measured APK is **4,379,153 B = 4.176 MiB**,
sha256 **`39FAE799…E25A`** (beginning `39FAE799`), and **75.7 % of that payload is Orekit/Hipparchus
*assets*, not code** (MEASURED: `app-release.apk` in the smoke project; §7.7).

These readings describe the **13.1.5** smoke artifact, not the pinned 13.1.9 jar, which is 10,833 B
larger (8,538,135 against 8,527,112). The APK size, the class-survival counts and the R8 warning are
therefore **re-taken at implementation** on the declared coordinate; what is being claimed is the
shape of the result — a release build that shrinks under R8 full mode with no keep rules — not the
byte count.

**Required runtime data.** Orekit refuses to run without an IERS UTC-TAI provider, so the slot bundles
**`assets/orekit-data/tai-utc.dat`, 3,321 B** (MEASURED, byte-identical copies under
`.kiln/scratch/android-orekit-smoke/app/src/main/assets/`), and constructs its frames with
**`simpleEOP = true`**, which removes the need for `finals.all` (§8).

**Its source, named rather than assumed.** The file is the IERS Rapid Service/Prediction Center
leap-second table published by USNO at **`https://maia.usno.navy.mil/ser7/tai-utc.dat`** — a keyless
`GET` that answered **HTTP 200, `Content-Length: 3321`, `Last-Modified: Tue, 06 Oct 2026 20:23:20 GMT`**
and whose bytes are **identical** to the shipped copy (same 3,321 B, same sha256
**`3524E1AE34D67E858873A89E59983BBC5BD100221DA898E796C1B36036A310C3`**; MEASURED 2026-10-08). Orekit's
own data repository is **`gitlab.orekit.org/orekit/orekit-data`** — **not** GitHub, where
`github.com/orekit/orekit-data` is a 404 — and its `update.sh` fetches the same file from the same host
(`usno_ser7_url=https://maia.usno.navy.mil/ser7`), which is what makes this host the upstream and not a
mirror. **Terms:** the USNO page states **"Distribution Statement A. Approved for public release:
distribution unlimited."** The `orekit-data` repository ships **no** `LICENSE` file, so **Apache-2.0
covers Orekit's code and not this data file**; the credits row names the two libraries, and the data
file's provenance is this paragraph. The table is append-only and has recorded no leap second since
2017-01-01, so the vendored copy cannot decay the way an ephemeris can.

**`astronomy.kt` — the Astronomy Engine for Kotlin (MIT).** Sun, Moon, planets, twilight, target
altitude/azimuth and rise/set/transit come from the vendored `astronomy.kt`. Its provenance is
`https://github.com/cosinekitty/astronomy` and its licence header is at **`astronomy.kt:2-7`**
(MEASURED — the lines read `Astronomy Engine for Kotlin / JVM.`, the repository URL, `MIT License` and
`Copyright (c) 2019-2025 Don Cross`); the **MIT notice text is committed with the slot** because the
vendored file is redistributed. It carries no runtime data file (VSOP87/ELP are built in) and no
catalogue, magnitude table or darkness model.

**The revision is pinned by digest, not by a line number.** Every citation in this design and in
`annex/` is of one revision of the file: **`.kiln/scratch/nv-core-proof/data/astronomy.kt`, 432,946 B,
sha256 `D4213F7E2AFC4553DFDF1D78A9C453A5C420CB77B280A2A092BB607618ACD97B`** (MEASURED 2026-10-08). The
`D1` task vendors exactly this revision, because the citations are line numbers — `:196`
(`REFRACTION_NEAR_HORIZON = 34.0 / 60.0`), `:452` (`defineStar`), `:1412` (`data class Observer`),
`:6173` (`moonPhase`), `:6363` (`searchHourAngle`), `:6818` (`searchRiseSet`), `:6911`
(`searchAltitude`) — and a different revision moves them. **All of them were re-verified to resolve to
the named symbol in this revision** (MEASURED 2026-10-08). That is why the digest and not the line
number is the pin: an upstream fetch of `master` may be a later file, so the slot either vendors the
digest above or re-verifies every citation against whatever it does vendor.

## Satellites

The `satellite-passes` capability owns the requirements; this is the source, format and accuracy
architecture.

**Source and terms.** `GET https://celestrak.org/NORAD/elements/gp.php?GROUP=<GROUP>&FORMAT=JSON` —
keyless, HTTP 200, 9,270 B for `GROUP=STATIONS` with 17 OMM records (MEASURED, annex/N-data-algorithms.md §3.1). Groups
shipped: a curated small set — `STATIONS` and `VISUAL` (the 100 brightest) — plus a
`CATNR=<noradId>` fetch for a user-named satellite; fetching the full `ACTIVE` list is the traffic the
policy asks clients to avoid. Politeness, enforced in code: store `fetchedAtUtc` **per group** and do
**not re-fetch a group within 2 hours**; on **any non-200** (301/403/404/429/50x), **stop the fetch
loop and log — never retry** (CelesTrak's own rule, annex/N-data-algorithms.md §3.1, §3.6). **Licence status is honest:** the
usage policy is a **bandwidth policy**, never a licence; it states the data are "freely available to all
users" and **neither forbids nor grants commercial use**, and **no SPDX licence text was found** —
`UNVERIFIED` whether a **paid** app is affirmatively licensed (§16, item 1).

**The OMM format change.** CelesTrak ran out of 5-digit catalog numbers on **2026-07-11**; newly
catalogued objects are **6-digit (100000+)** and **GP data is not available for them in TLE format**
(MEASURED, annex/N-data-algorithms.md §3.1). The curated legacy set (ISS 25544, Tiangong 48274, Hubble 20580) still has TLE
text, so a TLE-only parser would work today and break the first time the list grows. **The parser
therefore reads OMM/JSON**, using the fields `OBJECT_NAME`, `OBJECT_ID`, `EPOCH`, `MEAN_MOTION`,
`ECCENTRICITY`, `INCLINATION`, `RA_OF_ASC_NODE`, `ARG_OF_PERICENTER`, `MEAN_ANOMALY`,
`EPHEMERIS_TYPE`, `NORAD_CAT_ID`, `ELEMENT_SET_NO`, `REV_AT_EPOCH`, `BSTAR`, `MEAN_MOTION_DOT`,
`MEAN_MOTION_DDOT`.

**Orekit parses OMM itself — measured, not assumed.** `javap -public` over `orekit-13.1.9.jar`
(2026-10-08) shows the package `org.orekit.files.ccsds.ndm.odm.omm` carrying `OmmParser`, `Omm`,
`OmmData`, `OmmMetadata` and `OmmTle`; `OmmParser` has **`public Omm build()`**, and `Omm` exposes
**`generateTLE()`**, `generateKeplerianOrbit()` and `generateSpacecraftState()`. `OmmTle` carries the
SGP4 element set directly — `getEphemerisType()` with the constants `EPHEMERIS_TYPE_SGP`,
`EPHEMERIS_TYPE_SGP4`, `EPHEMERIS_TYPE_PPT3`, `EPHEMERIS_TYPE_SGP4_XP` and
`EPHEMERIS_TYPE_SPECIAL_PERTURBATIONS`, plus `getBStar()`, `getMeanMotionDot()`,
`getMeanMotionDotDot()`, `getElementSetNo()`, `getRevAtEpoch()`, `getNoradID()` and
`getClassificationType()`. The OMM → propagator path is therefore **direct and needs no hand-mapping**:
the app parses with `OmmParser` and asks `Omm.generateTLE()`. The
`TLE(int, char, int, int, String, int, int, AbsoluteDate, double×8, int, double)` field constructor is
confirmed present on the same revision and remains the **fallback** for a record the parser refuses.
What the measurement does **not** establish is whether `generateTLE()` tolerates a 6-digit
`NORAD_CAT_ID`, so that is a **required unit test** in `satellite-passes`, not an assumption — the
`E32` scenario exists for exactly this reason.

**The OMM→Orekit unit contract — the fallback's contract** (MEASURED against the Orekit javadoc,
annex/N-data-algorithms.md §3.2; the primary path is `OmmParser` → `Omm.generateTLE()` above, where
Orekit owns the conversion). It governs the hand-mapping used when the parser refuses a record, and it
is the invariant the unit test asserts either way, because Orekit takes angles in **radians** and mean
motion in **rad/s**, while OMM gives angles in degrees and mean motion in **rev/day**:

```text
i_rad    = INCLINATION       × π / 180
raan_rad = RA_OF_ASC_NODE     × π / 180
pa_rad   = ARG_OF_PERICENTER  × π / 180
ma_rad   = MEAN_ANOMALY       × π / 180
n_radps  = MEAN_MOTION        × 2π / 86400        # rev/day → rad/s
nDot     = MEAN_MOTION_DOT    × 2π / 86400²       # rev/day² → rad/s²
nDDot    = MEAN_MOTION_DDOT   × 2π / 86400³       # rev/day³ → rad/s³
epoch    = AbsoluteDate(EPOCH, TimeScalesFactory.getUTC())
```

`meanMotion` **is rad/s, not rev/day** — passing `15.48769982` straight through yields a satellite
that falls out of the sky; this is the most likely silent defect in the satellite path. Orekit
**normalises** negative `meanAnomaly`/`raan`/`pa` into `[0, 2π)`, so the converter must **wrap, not
clamp**. The app bundles **`assets/orekit-data/tai-utc.dat` (3,321 B)** — Orekit refuses to run without
an IERS UTC-TAI provider (`no IERS UTC-TAI history data loaded`, MEASURED) — with `simpleEOP = true` and
**plain prefixes** in `itrf-versions.conf` (a regex prefix breaks ITRF, measured).

**A pass is reported only when all of these hold** (annex/N-data-algorithms.md §3.4): `maxElevationDeg ≥ maskDeg` (default
**10.0°**, adjustable 0–45°); the satellite is **sunlit** (not in the Earth's umbra — `occurs` vs
`observable` is a two-state output, never a silently-dropped pass); the observer's Sun **geometric**
altitude at the pass instant is **≤ −6.0°** (civil twilight; the app states which threshold it used and
exposes the choice); and the AOS/LOS pair comes from the elevation detector at the observer's
`TopocentricFrame`, with the user's drawn `HorizonProfile` serving as the elevation mask. **No
brightness is claimed**: CelesTrak's OMM carries **no standard magnitude field** (measured key list
has `BSTAR`, `MEAN_MOTION_DOT`, `MEAN_MOTION_DDOT` and no magnitude), so `BSTAR` is **never** converted
to a magnitude, and a bright/faint label requires a curated per-satellite table recorded as facts.

**Staleness thresholds.** The element-set `EPOCH` is compared with the session instant, and the age is
**shown prominently in the 24–72 h band and at refusal**, and may be omitted only below 24 h:

| Age | Behaviour |
|---|---|
| `age ≤ 24 h` | compute, no age badge |
| `24 h < age ≤ 72 h` | compute, and **show the element set's age prominently beside every prediction**, with "element set is N days old — pass times may shift by minutes" |
| `age > 72 h` | **refuse to compute any pass time** and offer a refresh: "element set expired — connect once to refresh" |

**These are product choices, not measured constants** (annex/N-data-algorithms.md §3.6). They are grounded on two measured facts:
a normal age is **hours** (the policy says GP updates every 2 hours; live probes carried same-day
epochs), and the same propagator diverges **27,924 km at ~21.3 days**. **A divergence between two
contract documents is recorded rather than averaged:** annex/N-data-algorithms.md §3.6 refuses only above **168 h**, while the
edge register (annex/L-depth-retention-edges.md §2.4, E36) pins a tighter **72 h** refusal. This design adopts the **tighter 72 h
refusal** — the penalty for a wrong minute (a missed pass) is asymmetric and the app advertises offline
use — and §16 carries the divergence so the `satellite-passes` capability can settle it in one place.

**The accuracy claim and its honesty limit** (annex/N-data-algorithms.md §3.7). Two different numbers exist and must never be
confused: Orekit's SGP4 reproduces the reference implementation to **`pos ≈ 1.0e-4 km` (0.1 m)** and
`vel ≈ 1.0e-7 km/s` — **propagator fidelity, never real-world accuracy** — while the same propagator
**drifts 27,924 km at 21.3 days** from a stale element set. The UI may say *"Pass times are computed
from NORAD GP element sets. A fresh element set (hours old) is accurate to within a few minutes; the
age of the set is shown beside every prediction once it passes a day, and passes from an element set
older than 3 days are not shown."* It may **not** say a pass time is
accurate to 0.1 m or to a stated number of seconds. **The numeric bound for the "few minutes" claim is
UNVERIFIED**; §16 names the same-night AOS comparison that would settle it.

## Weather

The `cloud-forecast` capability owns the requirements; this is the client architecture.

**Endpoint.** `GET https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=<lat>&lon=<lon>[&altitude=<m>]`
— keyless, CC BY 4.0 with attribution, hourly **total** cloud cover (annex/N-data-algorithms.md §4.1). **Coordinates are
truncated to a maximum of 4 decimals before the URL is built**: the ToS states "truncate all
coordinates to max 4 decimals … For new products, requests with 5+ decimals will return a **403
Forbidden**" (MEASURED, annex/N-data-algorithms.md §4.1). 4 decimals of latitude ≈ **11 m**. Truncate, do not round.

**Required headers and duties** (annex/N-data-algorithms.md §4.2, MEASURED verbatim from the ToS):

- **`User-Agent: Lumintra/<versionName> (v.sobolenko.vs@gmail.com)`** — the ToS requires an
  identifying UA naming the application and a contact address, and states that a fake or random string
  "will result in a permanent ban". The address is the developer account's public address, defined once
  in `scripts/store/model.py` (`DEFAULT_CONTACT_EMAIL`) and **not a secret**. A measured detail is
  recorded so nobody "optimises" the header away: the endpoint answered **byte-identically with and
  without a UA** (two 38,302-byte probes, `Compare-Object` difference count 0) — tolerance is not
  permission.
- **`If-Modified-Since`** with the stored `lastModified`, and **honour `Expires`**; **never** `HEAD`
  followed by `GET` (it doubles server load); **never** refresh while the app is not in use; add a
  random offset of minutes so requests do not land on the hour; **HTTPS only** (a plain-`http://`
  endpoint is refused by the client, not followed). A **203** response means the product version is
  deprecated and is logged/shown as a warning.

**This slot's version identity.** `versionName = "1.0.0"` and `versionCode = 1`. `versionName` is the
value interpolated into the `User-Agent` above, sent to **both** services (CelesTrak, § "Engines and
dependency facts"/§ Satellites and MET Norway here), and the value the About screen shows.

**The field read.** `properties.timeseries[].data.instant.details.cloud_area_fraction` — a number
**0–100** in **per cent**; `.time` is ISO-8601 UTC (MEASURED, a live payload with 87 hourly points).
**Only total cloud cover exists** — there is **no low/mid/high split** — so the UI must say **"total
cloud cover"** and must never render the value as "clear sky", a transparency model, or a seeing
estimate (annex/N-data-algorithms.md §4.3; the `cloud-forecast` capability owns the wording).

**Cache policy.** Store per spot `{lat4, lon4, fetchedAtUtc, expiresUtc, lastModified, etag, payload}`,
keyed by the **truncated** coordinates (two spots 100 m apart are the same request). Re-request only
after `expiresUtc`. **No background retrieval** — the ToS forbids it for mobile apps — so the fetch
happens only while the app is in use and only for the spot the user is looking at.

**Offline behaviour.** No network → show the **cached** forecast with its **age** and an explicit
**"offline — showing data from <local time>"** label; never fabricate a value, never present a stale
forecast as current. No cache at all → the cloud row is **unavailable**, stated in words. The app
remains fully useful: the dark window, the moon and the targets are local computations, so a network
failure degrades exactly one panel.

**Attribution.** The ToS requires CC BY 4.0 attribution: credit, a **link to the licence**, and an
indication if changes were made. Required string, declared as a `translatable="false"` resource so no
locale tree may replace it: **`Data from MET Norway (met.no) — CC BY 4.0`**, shown **unmodified** (the
app shows the data unmodified; it truncates not the data but the request coordinates) and carrying a
**link to the licence**. It appears **(a) visibly beside the forecast** — a footnote
on the screen that shows it, not only in a menu, because the credit must travel with the data — and
**(b) in the Credits screen** (`settings-and-credits`). The app must not use the name `Yr` or the Yr
logo, nor imply endorsement.

## Persistence

The `scout-spots`, `settings-and-credits` and `share-brief` capabilities own the requirements; this is
the storage architecture and the codec discipline.

**What is stored, in what format.**

- **The scout store** (`data/ScoutStore.kt`) — saved spots (name, `latDeg`, `lonDeg`, `elevationM`,
  IANA `tzId`), horizon profiles, equipment profiles, the **active spot selection** and favourite
  target ids (stable **string** ids such as `m31` / `ngc7000`, **never a list index** — an index
  silently points at a different object after a catalogue edit). The active selection is one
  **store-level `activeSpotId`** holding a saved spot's stable string `id` — never a row position and
  never a field of a `Scout` record — so it survives a **cold start** (an app killed and relaunched, a
  device reboot, an App Backup restore), not only process death. It is **device-local and is not part of
  the export**: the export document carries exactly the fields of *"The user's spots, horizons and
  equipment export to a file they own"* and no others, so the importing device **derives** its own
  active spot rather than reading one from the file. A dangling `activeSpotId` — an id no longer in the
  store, or one naming a quarantined record — **falls back** to a real saved spot rather than crashing,
  leaving a valid spot unselected or rendering a fabricated `(0, 0)` sky; the full resolution rules,
  including the empty-store, active-delete and import cases, are those of *"The `Spots` screen lists the
  saved spots and owns the active one"* (`specs/scout-spots/spec.md`). Persisted through `DataStore`;
  the durable human-facing copy is the export file below.
- **The settings store** (`data/SettingsStore.kt`) — units, language handling, red night mode,
  reminder opt-in, and the **row P9 *Sky brightness estimate*** of
  `specs/settings-and-credits/spec.md` — the only user-side darkness input the app accepts, held as the
  user's own SQM reading in **mag/arcsec²** or a Bortle class **1–9** inside the accepted range that row
  states, with **`none`** as the default so that with no estimate stored the darkness readout built from
  `SkyGrade` shows the computed `SkyGrade` alone. When a value is set it adds a separate line carrying
  the estimate with the words **"user estimate"**; it **never** alters `SkyGrade` or any computed
  quantity of the brief, and it is **never** reported as a computed Bortle class or a computed
  mag/arcsec² value — a Bortle class or SQM number on screen is only ever the value the user entered.
  The accepted ranges and the refusal-or-clamp rule are the row's, not restated here. Each preference is
  added to this store **together with the code that reads it**.
- **The forecast cache** (`data/CloudRepository.kt`) — keyed by truncated coordinates (§9).
- **The element-set cache** (`data/ElementsRepository.kt`) — `{noradId, group, epochUtc, fetchedAtUtc,
  payload}` per group (§8).
- **The horizon profile** (`domain/HorizonProfile.kt`) — exactly **360 samples**, one per whole degree
  of azimuth `0…359°` (geographic north, clockwise), elevation stored in **tenths of a degree** as a
  **signed integer clamped to `[−100, +900]`** (i.e. `[−10.0°, +90.0°]`), persisted as a
  **720-byte buffer of 360 signed 16-bit values** (**`2 B × 360 = 720 B`**, little-endian `int16`;
  ≈ 0.7 kB per spot; arithmetic on the stated widths, not a measurement). Other azimuths
  interpolate **linearly**, wrapping at `0/360`. The count is settled at
  **360** (1.0° per whole degree of azimuth), deliberately over the 720-sample / 0.5° shape a source
  report proposed (`specs/scout-spots/spec.md`).
- **The export / import codec** (`data/SpotTransfer.kt`) — a single JSON document,
  `lumintra-scout-<yyyyMMdd>.json`, `schemaVersion` first, containing the spots (with their horizon
  arrays and equipment profiles) and the favourite target ids. **No session data, no caches, no
  element sets** — caches are regenerable and must not bloat or date the file.

**The codec discipline** (annex/L-depth-retention-edges.md §1.3.D, E100–E104; the `scout-spots` capability owns the exact behaviour):

- **`decode` never throws.** A malformed, truncated or unreadable payload returns a typed failure, never
  an exception into the UI, and the store surfaces it as "nothing to restore" — the caller is a cold
  start and a corrupt file must cost the file, never the app.
- **It refuses an unknown version.** A `schemaVersion` **newer** than the app is refused with a
  message rather than half-imported; `schemaVersion` **first** in the document.
- **Import is all-or-nothing**: a file that fails validation imports nothing and reports the failing
  field and line. A partial import is how half-corrupt state is created.
- **Import never silently overwrites**: `merge` (skip records whose name and coordinates match
  exactly) or `replace all`, with the count of each shown before the commit.
- **Hard caps are validated before allocation** — file size, spot count, horizon sample count, string
  lengths — so a crafted or truncated file produces a typed error, not an OOM.
- **Writes are atomic** — write a temporary file, then rename — so a crash mid-write leaves the
  previous good store intact.
- **The store is loaded per record**: one bad record is quarantined, the rest load, and the quarantine
  is reported once; quarantine is durable (never silently dropped on the next save).

**Backup rules** (`.cursor/rules/*` Play compliance floor, `AGENTS.md`; and `annex/L-depth-retention-edges.md` E102). Both
`res/xml/data_extraction_rules.xml` (API 31+) and the legacy `res/xml/backup_rules.xml` (API ≤ 30) are
present; **the plan store is included**, and **regenerable caches (element sets, forecasts) are
excluded** so a restore cannot present a stale cache as fresh. There are no secrets to exclude, and the
rules state so. This is the `durability` half of the `pin-the-session-scout` promise; the `data_in` /
`data_out` handoff (export/import) is the `carry-your-scout` half.

## Network and permission policy

**The exact permission list, and why each exists:**

| Permission | Why it exists |
|---|---|
| `android.permission.INTERNET` | Required by the two keyless network calls: the CelesTrak GP fetch (§8) and the `api.met.no` forecast (§9). Nothing else leaves the device. |
| `android.permission.POST_NOTIFICATIONS` (API 33+) | Exists **only** because the reminder and the widget were approved (annex/CONTEXT.md §2). It is **never a gate**: denial disables the reminder only; every other feature is unaffected. |

**There is no `RECEIVE_BOOT_COMPLETED` permission and no boot receiver of the app's own.** The reminder
survives a restart because **`WorkManager` persists and reschedules its own queued work**, and the
widget is renderable from persisted state at any moment, so the app declares **no `BOOT_COMPLETED`
receiver** and requests no such permission; a receiver at `android:exported="true"` is never declared
(it would breach the factory's security baseline) (`specs/night-glance/spec.md:151,377,405`).

**Permissions deliberately not requested** (annex/CONTEXT.md §5, and annex/L-depth-retention-edges.md §1.2):

- **No location permission.** "Manual coordinates, a saved spot, the app's own coordinate picker and `Geocoder` all work
  without it. `ACCESS_COARSE_LOCATION` would buy only 'where am I standing', which this product does not
  need" (annex/CONTEXT.md §5, MEASURED). The spot is set by manual entry, the app's own coordinate picker (§11), a saved spot, or pasted coordinates.
  (The edge register in `annex/L-depth-retention-edges.md`, row E61, proposed `ACCESS_COARSE_LOCATION` for a "use my location" action; §16 records
  the divergence for the `scout-spots` capability to settle.)
- **The pin surface is the slot's own drawing, because every map SDK breaks one of these rules.**
  MEASURED 2026-10-08: **MapLibre Native Android** (BSD-2-Clause, latest stable `13.6.1`,
  `android-v13.6.1`) declares `ACCESS_COARSE_LOCATION` **and** `ACCESS_FINE_LOCATION` in its own
  library manifest, so it cannot coexist with the rule above, and it drags
  `androidx.fragment:fragment:1.8.9` beneath the factory's `1.9.1` floor; **osmdroid** `6.1.20` defaults
  to `tile.openstreetmap.org`, whose usage policy forbids offline use and warns commercial services;
  **Google Maps, Mapbox and MapTiler** each require a client key, which `client-secrets.mdc` forbids.
  The picker is therefore a slot-owned Compose `Canvas` + `pointerInput` surface — pan and zoom over a
  graticule with a draggable pin — with an optional coastline committed from **Natural Earth 1:110m**,
  which is **public domain** ("No permission is needed to use Natural Earth. Crediting the authors is
  unnecessary."). It adds **no dependency, no key, no permission and no network call**, which is what
  makes the offline path true by construction rather than by promise.

  **The place name is the platform geocoder, and it is optional enrichment.** The only geocoding call
  the app may make is Android's own `android.location.Geocoder`: it is **keyless**, so it carries no
  credential and no new permission (the app already declares `INTERNET`), whereas a third-party
  geocoding API — Google's Geocoding API, Mapbox, MapTiler — needs a client key and is therefore
  **forbidden** by `.cursor/rules/client-secrets.mdc`. The reference measures its shape and its limits
  (MEASURED 2026-10-08, `developer.android.com/reference/android/location/Geocoder`): a call *"may hit
  the network, and may block for excessive amounts of time"* and returns *"null or empty list … if there
  is no backend service available"*, with `Geocoder.isPresent()` — **API 9** — as the documented presence
  check (*"If no implementation is present, any attempt to geocode will result in an error"*). Because
  this slot's `minSdk` is **26** and the asynchronous overload is **API 33**, the call branches:
  the `GeocodeListener` overload on API 33+, and the deprecated synchronous overload **on a background
  thread** on API 26–32 — the reference's own permitted fallback. A missing implementation, an offline
  device, a `null` result and an empty list are all **normal states**: the spot keeps its coordinates and
  the user's own label, nothing is gated, and the created spot is labelled `approximate`.
- **No `ACCESS_BACKGROUND_LOCATION`** — nothing observes position in the background.
- **No `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`** — `AGENTS.md` treats exact alarms as restricted, and
  a dark-window reminder is precise to the minute at best, so an **inexact** alarm loses nothing
  measurable.
- **No `CAMERA`, `READ_MEDIA_IMAGES`, `ACTIVITY_RECOGNITION`** — no feature uses them.
- **No `com.android.vending.BILLING`** and no billing library — the shape is `purchase_only_no_ads`,
  with no in-app product (annex/CONTEXT.md §2).

**Data-safety consequence** (annex/CONTEXT.md §5; `AGENTS.md` Play compliance floor). Sending a saved spot's
coordinate to `api.met.no` **is** a collection under Play's definition — *"'Collect' means transmitting
data from your app off a user's device"*, and it counts *"irrespective of whether data is transmitted
to you or a third-party server"*. **Declare Location → Approximate, App functionality, optional,
encrypted in transit**, and **name MET Norway as a recipient** in the privacy policy. Nothing else
leaves the device: the CelesTrak query carries no coordinates, and share/export are user-initiated.
The privacy policy is required regardless, is **published by the factory itself**
(`docs/privacy-policy-hosting.md`), and **must be openable from inside the app** (the in-app half is a
separate Play requirement; `scripts/check-in-app-policy.mjs` is the check).

## Screen map and navigation

**The nine destinations** (annex/CONTEXT.md §3.3, names verbatim):

| Screen | Canonical file | Meaning |
|---|---|---|
| `Night` | `ui/night/NightSceneScreen.kt` | the hero: the scrubbable scene and everything on it |
| `Brief` | `ui/brief/BriefScreen.kt` | the session brief: the scene on top, the answers scrolled under it |
| `Targets` | `ui/targets/TargetsScreen.kt` | browse, search and filter the object catalogue |
| `Spots` | `ui/spots/SpotsScreen.kt` | the list of saved observing spots |
| `SpotEdit` | `ui/spots/SpotEditScreen.kt` | create or edit one spot, **including the horizon editor** |
| `Share` | `ui/share/ShareScreen.kt` | compose and send the postcard / text brief |
| `Settings` | `ui/settings/SettingsScreen.kt` | units, language, red mode, reminders, Rate us, About, Credits |
| `Credits` | `ui/settings/CreditsScreen.kt` | every licence and attribution the app must display |
| `Welcome` | `ui/welcome/WelcomeScreen.kt` | first run: one screen that ends at a working `Night` scene |

**The horizon editor is inside `SpotEdit`, never a separate destination** (annex/CONTEXT.md §3.3).

**The Navigation 3 host owns its back stack.** The app uses Navigation 3 with `rememberNavBackStack`
— the back stack Navigation 3 owns, not a hand-rolled saver — destinations declared as `@Serializable`
`NavKey`s typed by the `entryProvider` DSL. `MainActivity` opts in to `enableEdgeToEdge()` and the
shell wraps the host in a root `Surface` so `LocalContentColor` yields `onBackground`. These are the
requirements of `.cursor/rules/ui-ux-baseline.mdc`; the accepted precedent `app-laneset` already
satisfies them and is the shape to imitate.

**Size-class behaviour.** A wide window is a two-pane layout: `Spots`/`SpotEdit` and `Targets`/detail
are list-and-detail pairs, so the wide window shows the detail beside the list rather than replacing
it. The `Night` scene reads the window size class and works in **both portrait and landscape** — in
landscape the strip and chrome reflow, the sky is not cropped into an unreadable band, and the scene
remains the hero. Every screen reads the size class; none assumes a phone.

**The first-run path.** A cold start with no saved spot opens `Welcome`, whose single job is to end at a
working `Night` scene: it offers exactly one action — set a spot by manual coordinates, the slot's own
pin picker (§11, drawn in-app, no map SDK), or pasted coordinates. **The app must not default to `(0, 0)`**: with no location and no spot it shows a
scoped-empty brief with the single "set a spot" call to action, never a sky computed for the Gulf of
Guinea. Once a spot exists, a later launch opens `Brief` **already resolved for tonight at the most
recently used spot**, with the scene drawn for the current instant and no configuration step — the
user's first action is to scrub, not to answer questions. The "Tonight" summary is this default, not a
separate destination.

## Localization

**Born multilingual** (`.cursor/rules/localization.mdc`): `en` and `ru` from the first screen, via
`res/values/strings.xml` and `res/values-ru/strings.xml`, with `android:localeConfig` declared so
Android 13+ owns the per-app language picker (the language row in `settings-and-credits` routes to the
platform picker, never a second in-app picker).

- **No literal user-facing string in Kotlin.** Every screen string, phase label, unit, note and
  attribution is a resource present in **both** locales in the same change; `slot.locales` fails a
  declared locale whose tree falls back to English or carries a placeholder.
- **Title lengths are measured, not assumed.** The store title is derived from `naming.json` and must
  satisfy Play's **30-character** cap with **≥ 5 characters of slack** measured in **each declared
  locale** (full-width characters count the same as half-width) — `.cursor/rules/app-naming.mdc`
  governs; this design states only that the listing strings come from the naming contract and are not
  re-authored per screen.
- **A spot's timezone versus the device's.** Times and dates are displayed in the **spot's** `tzId` by
  default, with an explicit toggle to the device zone shown as a badge ("device time"). If a spot has no
  `tzId`, fall back to the device zone **and label it**. A spot abroad never silently shows device-zone
  times (§4). All instants are formatted with `DateTimeFormatter` and an **explicit locale**
  (`Locale.forLanguageTag(locale)`), never a hardcoded `HH:mm` pattern.
- **DST is a label, not a computation.** Because all math is UTC, a DST transition changes only the
  label; a displayed clock time may jump ±1 h across a transition while the session duration does not.
  Durations and intervals are computed in UTC and **never** from formatted strings.
- **The scene is not mirrored by layout direction.** The horizon axis is an **azimuth** axis; mirroring
  it would put the user's west ridge on the wrong side and make the picture a lie. Text inside the scene
  is localised; the surrounding chrome mirrors normally.
- **The Credits screen is localised too**, and the mandatory attributions (ESO/S. Brunier, MET Norway
  CC BY 4.0, the IAU / Sky & Telescope constellation figures, FK6 CC0, NASA SVS) survive every locale —
  `settings-and-credits` owns the exact set. The Solar System Scope row is **removed**: no planet
  surface texture ships (the sky-assets decisions in §4).

## Visual identity

**The identity is declared before the UI is styled** (`.cursor/rules/visual-identity.mdc`). `store/studio.json`
names the `mark`, the full `palette` (every role the renderer draws with), the per-shot `headlines` and
`kickers`, the `featureTagline` and the `featurePill`. It is a build input the launcher icon, the
adaptive-icon vectors, the feature graphic and the store compositions are rendered from — not
documentation. Decide it at stage 1, not during `/release`.

- **The palette is one source of truth, shared with the app.** The colours in `studio.json` are the
  colours in `ui/theme/Color.kt`; a pack whose palette drifted from the app's own theme is a screenshot
  of a different app, and `scripts/visual-qa.py`'s palette section measures the shipped pixels against
  the Kotlin palette. The slot owns its theme — its own `Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt` —
  and replaces the Material 3 defaults rather than shipping them.
- **The mark is drawn, not borrowed.** `ui/components/LumintraMark.kt` is the in-app brand mark, and the
  launcher icon, the adaptive-icon foreground, the monochrome/themed layer and any in-app mark are all
  derived from `studio.json`'s `mark` (the `Mark` half of the identity), read at 48 dp, surviving the
  adaptive-icon masks (72 dp circle, squircle, 66 dp safe zone), with **no baked-in rounded mask or
  shadow**. A stock library glyph is never the mark.
- **UI iconography comes from one licence-cleared professional set**, vendored into the slot with its
  licence text and a per-icon provenance record (`icons/provenance.json` / `docs/icon-provenance.md`),
  tinted from the theme with no colour of its own. General-purpose glyphs (a back arrow, a plus, a bin,
  a settings gear, an empty-state symbol) are **not** hand-drawn as Compose `Canvas`; a glyph whose
  meaning the app's own domain defines stays slot-owned and is drawn in the library's stroke weight.
- **The red night mode is a fourth theme state, not a filter.** `ui/theme/NightVisionPalette.kt` is a
  full role remapping alongside light and dark, because a red filter over a dark palette is not a
  night-vision palette — it is a dark palette with the green channel removed. The `Night` scene **always
  renders on the slot's dark surface** regardless of the system theme (a light-mode night sky is wrong);
  the surrounding chrome responds to dark/light, and red mode remaps the roles across both.
- **The identity is per slot**, like the job is: two slots never share a mark, a palette or a component
  kit (`visual-identity.mdc`; `docs/monetization-strategy.md` §1.2).

## Deliberately decided trade-offs

Each is a decision that costs something, and the cost is named rather than hidden.

1. **No light-pollution / Bortle / SQM raster ships.** The cost: the app shows no darkness *number* from
   a map. The exchange: the only licence-clean raster family (NOAA/EOG VIIRS VNL, CC BY 4.0) is a
   **radiance** product, so it yields at most a *relative* index and needs a factory-side reduction plus
   a model whose legal status is a review item (annex/N-data-algorithms.md §6.2–6.3). v1 reports darkness from computed
   quantities plus the **user's own** SQM reading or Bortle estimate, labelled "user estimate".
2. **`purchase_only_no_ads`, no subscription, no ads, no server.** The cost: no recurring revenue, so a
   recurring per-user cost cannot be funded; and there is no background "good night is coming" alert,
   because background retrieval is both forbidden by the weather source and a battery/maintenance tax
   (annex/L-depth-retention-edges.md §1.3.B).
3. **The weather source gives total cloud only.** The cost: no transparency or seeing model, and a
   dedicated astronomy-cloud app (Clear Outside) does more; the UI must say "total cloud cover" and
   never promise a layer split. The exchange: `api.met.no` is the source whose terms permit a paid app,
   unlike the free Open-Meteo tier (annex/N-data-algorithms.md §4; annex/CONTEXT.md §2).
4. **Orekit for satellites, accepting its size.** The cost: the jars are large and **75.7 % of a smoke
   APK's payload is Orekit/Hipparchus *assets*, not code** (measured, `G-android-build-proof.md` §5, not copied into this change). The exchange: the Android
   build was run and passed under R8 full mode **with zero keep rules** and reproduced JVM-identical
   output on the A40 (`G-android-build-proof.md` §6, not copied into this change), whereas the smaller Vallado subset is **numerically unvalidated here and
   licence-unverified** (`F-engine-gap-map.md` §6.3, not copied into this change).
5. **Layered 2-D with an optional AGSL path, no 3-D engine.** The cost: on the A40 the shader never
   runs, so the fallback carries 100 % of the load and the shader's benefit cannot be verified on the
   test device. The exchange: the fallback is beautiful by construction, and the shader is additive.
6. **The scene renders on demand, with no frame clock.** The cost: twinkle/scintillation is a time
   phenomenon, so a still scene has still stars. The exchange: an idle scene produces **zero** frames —
   the largest battery decision in the app (annex/O-sky-scene-rendering.md §7.4).
7. **The moon's (and planets') contribution to sky brightness is not modelled in v1.** The cost: no
   per-target skyglow number. The exchange: the only licence-clean route (Krisciunas & Schaefer 1991)
   is a model whose numerical accuracy is UNVERIFIED and which must never be presented as a measurement
   (annex/N-data-algorithms.md §2.4).
8. **A 72 h element-age refusal rather than 168 h.** The cost: fewer usable offline passes. The
   exchange: safety against the measured 27,924 km drift, on an app that advertises offline use (§8).
9. **Own-format export/import, no competitor-format import.** The cost: a user with a competitor's data
   must retype. The exchange: no competitor's export format was measured, so an importer would be
   invented; pasted coordinates are the honest substitute (annex/L-depth-retention-edges.md §1.3.E).
10. **A hand-compiled ~250-object catalogue rather than a full shipped catalogue.** The cost: it is not
    complete. The exchange: no permissively-licensed full catalogue exists, and compiled catalogues'
    licences (CC BY-SA, CC BY-NC, GPL) cannot ship in a paid app (§7).
11. **No location permission; the spot is set by manual/map/paste.** The cost: one extra step for a user
    who is standing at the spot. The exchange: no runtime permission that buys nothing, and a sky that
    is insensitive to a 3 km error anyway (annex/CONTEXT.md §5).
12. **A hand-verified FK6 supplement.** The cost: authored, two-source-verified data. The exchange: FK6
    lacks Sirius, Arcturus, Capella and Betelgeuse, so without it the scene omits its most recognisable
    stars (§7).
13. **A single `:app` module and no DI framework.** The cost: none claimed, but stated so a later reader
    does not read the absence as an oversight — the surface does not need modules or Hilt/Koin.

## Open items carried forward

Every contract the analysis could not settle, with the exact check that would settle it. All are
**UNVERIFIED**; an invented constant is a defect and is refused rather than guessed.

**Carried from `annex/N-data-algorithms.md` §8:**

1. **CelesTrak's licence for a paid app.** The usage policy is a bandwidth policy and states no
   licence grant and no commercial clause. *Settling check:* read the licence/terms footer of
   `https://celestrak.org/` and `gp-data-formats.php` for an explicit grant or a commercial clause; if
   none, ask **TS.Kelso@celestrak.org** (the address the policy publishes) and record the answer.
2. **The real-world accuracy of a computed pass time.** The propagator's *fidelity* (≤ 0.1 m vs the
   reference implementation) and its *divergence* at 21.3 days (27,924 km) are measured; the honest
   number for a fresh element set is not. *Settling check:* on one night, compare the app's computed
   AOS for the ISS with an independently observed or third-party-predicted AOS at the same site,
   recording the element-set epoch used.
3. **The Moon's (and planets') numerical accuracy from `astronomy.kt`.** Only the Sun is verified
   (0.0248 arcmin); the Moon path is present but unexercised. *Settling check:* run
   `searchRiseSet(Body.Moon, …)` and `illumination(Body.Moon, …)` against a handful of JPL Horizons
   rows and record the arcminute error, as the Sun case already does.
4. **The legal status of a VIIRS-radiance → mag/arcsec² conversion.** *Settling check:* a licence review
   of the specific model and output before any mag/arcsec² number is shown; until then, present a
   relative index only.
5. ~~Whether `astronomy.kt` exposes `searchHourAngle`~~ — **RESOLVED 2026-10-08 (MEASURED).** It does:
   `fun searchHourAngle(body: Body, observer: Observer, hourAngle: Double, startTime: Time,
   direction: Int = +1): HourAngleInfo` at **`astronomy.kt:6363`**. The transit scan may therefore be
   replaced by a direct call; whether to do so stays with the implementation.
6. **The commercial status of the Yale BSC5** (should a brighter star list than FK6 be wanted).
   *Settling check:* CDS `V/50/ReadMe` carries no copyright notice (measured); a decision to ship it
   must be recorded as relying on an unlicensed-by-default catalogue.
7. **FK6 row count 4,150 vs 4,151.** The recon's `I-assets-and-data.md` file (not copied into this change) said 4,150; the live TAP `SELECT COUNT(*)`
   returned **4,151**, which governs. Recorded here so the two numbers do not silently disagree.
8. **FK6 lacks Sirius, Arcturus, Capella and Betelgeuse.** *Settling check:*
   `SELECT comname, vmag FROM fk6.fk6join ORDER BY vmag ASC` — which is exactly how this was found;
   the supplement is what closes it.

**Real-device performance, and the shader path, which cannot be measured on the A40:**

9. **"It is smooth on the A40" is UNVERIFIED.** No frame-time, thermal or battery reading exists for
   this scene on this phone. *Settling check:* on serial `R58MB3SGZVL`, confirm
   `ro.product.model` = `SM-A405FM` and `ro.build.version.sdk` = `30`, then over a 30-second scripted
   drag read `dumpsys gfxinfo com.gexetr.lumintralightplanner framestats`: **p95 ≤ 16.7 ms, p99 ≤
   33 ms, janky frames < 1 %**, and the idle frame counter unchanged (checklist items #29–#31, annex/O-sky-scene-rendering.md §10.1).
10. **The AGSL and LUT paths' colour parity is UNVERIFIED**, because the A40 (API 30) is below the AGSL
    floor. *Settling check:* render the same (spot, instant) on the A40 (LUT) and on the **A16**
    (`SM-A165F`, API 36 — authorised **only** to check the shader path, on the owner's word) and compare
    sampled pixel colours per band; the debug golden-image test covers the software comparison only
    (annex/O-sky-scene-rendering.md §8, checklist #34).

**Additionally carried, each with its settling check:**

11. **Whether `astronomy.kt` precesses a J2000 `defineStar` to the date.** A J2000-as-of-date error is
    ~0.36° by 2026 (INFERRED), enough to change a "does it fit my frame" answer. *Settling check:*
    compute M31's altitude/azimuth at a fixed instant and place and compare against JPL Horizons or
    Stellarium, recording the arcminute error (annex/L-depth-retention-edges.md §3.2).
12. ~~Whether Orekit ships an OMM parser that yields a propagator~~ — **RESOLVED 2026-10-08
    (MEASURED).** It does. `javap -public` over `orekit-13.1.9.jar` shows
    `org.orekit.files.ccsds.ndm.odm.omm` with `OmmParser` (`public Omm build()`) and `Omm` exposing
    `generateTLE()`, `generateKeplerianOrbit()` and `generateSpacecraftState()`; `OmmTle` carries the
    SGP4 element set (`getBStar()`, `getMeanMotionDot()`, `getMeanMotionDotDot()`,
    `getElementSetNo()`, `getRevAtEpoch()`, `getEphemerisType()` and the `EPHEMERIS_TYPE_*`
    constants). The field mapping of §8 is therefore the **fallback**, not the plan. One sub-question
    the measurement does not answer is left as a **required test** rather than an assumption: whether
    `generateTLE()` tolerates a 6-digit `NORAD_CAT_ID` (`satellite-passes`, the `E32` scenario).
13. **Whether `astronomy.kt` supplies a planet's apparent disc diameter** (needed for the framing math).
    **Partly resolved 2026-10-08 (MEASURED):** it supplies the **Moon's** — "the apparent angular
    diameter of the Moon, in degrees" at `astronomy.kt:1804` and `:7844` — and a sweep for a
    planet-radius / angular-diameter API returned nothing further, so a **planet** disc size still
    needs an authored table or must be omitted (annex/N-data-algorithms.md §1.6).
14. **The element-age threshold.** annex/N-data-algorithms.md §3.6 refuses above **168 h**; `annex/L-depth-retention-edges.md` E36 refuses above **72 h**. This
    design adopts the tighter **72 h** refusal (§8, trade-off 8). *Settling check:* the
    `satellite-passes` capability spec states one value; the effect is to be confirmed against the real
    SGP4 timing-error-versus-age curve frozen-element experiment (annex/L-depth-retention-edges.md §3.5).
15. **The horizon storage granularity.** *Settled* by `specs/scout-spots/spec.md`: **360 samples**
    (one per whole degree of azimuth) in **tenths of a degree** as a **signed integer clamped to
    `[−100, +900]`**; the 720-sample / 0.5° shape a source report proposed is rejected. The
    implementation asserts the shape in a unit test.
16. **The location permission.** annex/CONTEXT.md §5 states none is needed; `annex/L-depth-retention-edges.md` E61 proposed
    `ACCESS_COARSE_LOCATION` for a "use my location" action. This design ships **no** location permission
    (§11). *Settling check:* the `scout-spots` capability spec states one answer.
17. **The exact numeric bound for the "few minutes" pass-time claim.** *Settling check:* the same-night
    AOS comparison of item 2.

## What is not yet built

Stated once, plainly, so no reader mistakes this design for a description of a finished app. This
change has **no application code**:

- **No slot and no Kotlin, Gradle, XML or asset under `apps/`.** This document and the eleven capability
  specifications are the whole artifact of this change; the implementer's job starts after them
  (annex/CONTEXT.md §0).
- **The `openspec/changes/app-lumintra-light-planner/` directory did not exist before this file was
  written** (MEASURED — `Get-ChildItem openspec\changes` listed only `app-laneset`, `app-lawn-round-log`,
  `app-unit-walk` and `archive`). The eleven `specs/<capability>/spec.md` files are written in parallel;
  the `proposal.md` and `tasks.md` are authored with them.
- **No build has been run for this slot, no test result is claimed, and no device measurement exists.**
  The only on-device numbers this document cites are inherited, labelled, from prior probes — the
  Orekit smoke run (G) — and are **not** measurements of this app.
- **The three contracts exist** — `docs/ideation/plans/lumintra-light-planner.economics.json`,
  `.differentiators.json` (MEASURED — read this session, **13 claims, all `deferred`**: the 7
  pre-existing ids `compose-the-light-and-dark-brief`, `plan-deep-sky-targets`,
  `predict-visible-satellite-passes`, `forecast-hourly-cloud-cover`, `pin-the-session-scout`,
  `share-the-session-brief` and `search-the-deep-sky-catalogue`, plus this change's 6 added ids
  `render-the-night-scene`, `draw-the-horizon-line`, `see-your-lens-frame`, `carry-your-scout`,
  `glance-at-tonight` and `get-myself-outside`) and `.naming.json` (annex/CONTEXT.md §3.9).

Everything above that is written in the present tense is **inherited measurement** (an engine capability,
a licence, a table size, a device specification) or a **product decision recorded in annex/CONTEXT.md §2**.
Everything that is this app's own behaviour is a requirement, and the artifact that will prove it does
not exist yet.

# Lumintra: Light Planner — handoff (phase 1, unfinished)

**Status: PHASE 1 INCOMPLETE — the app has never been compiled, has never run, and is not
approved for release.** This file is the resume point for the next chat. It was written
2026-10-09 at the point the session's model budget ran out, **re-verified at the end of that same
session**. It records the owner's Moon/359° decision (§2.4b) and that the share/targets/credits
worker finished after wrap-up began (§2.2 is now LANDED). No Kotlin was compiled — the corrected
`MoonTest` and the seven new screens are still unrun.

Slot: `apps/2026-10-09-lumintra-light-planner/`
Package: `com.gexetr.lumintralightplanner`
Specification: `openspec/changes/app-lumintra-light-planner/` (`proposal.md`, `tasks.md`,
`design.md`, `specs/*/spec.md`)
Interface contract frozen for the parallel workers: `.kiln/scratch/lumintra/INTERFACES.md`

Read `INTERFACES.md` **first** — it is the contract the written code obeys (component API,
formatter signatures, screen signatures, data-layer contract). It is in `.kiln/`, which is
gitignored and disposable: if it is gone, recover it from the agent transcript or from the
signatures as they exist on disk.

---

## 1. What is on disk and works (MEASURED)

Re-measured 2026-10-09 after the share/targets/credits worker finished: **75 Kotlin files in
`app/src/main/java/` (1 761 KB) and 18 in `app/src/test/` (249 KB)**. Of those, **74 / 1 338 KB**
are under the slot package `com/gexetr/lumintralightplanner/`; the difference is the vendored
`io/github/cosinekitty/astronomy/astronomy.kt` (~423 KB). Earlier drafts of this file said 70 / 69 —
that was before `ui/share/*`, `CreditsScreen.kt` and `TargetDetail.kt` landed.

| Area | Files | State |
|---|---|---|
| `domain/` | 14 | complete — pure Kotlin, no Android imports |
| `ui/theme/` | 6 | complete — slot palette, red night mode, type, shape |
| `ui/components/` | 7 | complete — own mark, panels, chips, chip states, phase dial, badges, azimuth ruler |
| `ui/format/Formatters.kt` | 1 | complete |
| `data/` | 8 | complete — `ScoutStore`, `SpotTransfer`, `CatalogueRepository`, `CloudRepository`, `ElementsRepository`, `SkyAssetSource`, `SettingsStore`, `ReviewPromptStore` |
| `data/sky/` | 6 | complete — `SkyEphemeris`, `NightEngine`, `SceneBuilder`, `TwilightSearch`, `SatellitePassEngine`, `OrekitRuntime` (engine worker [`sky-engine`](07b03a85-055d-48dc-8f35-96b9e39d288f) finished; tests written, never run; notes in `.kiln/scratch/lumintra/SKY_ENGINE.md`) |
| `notify/` | 2 | complete — `DarkWindowReminder`, `ReminderScheduler` |
| `ui/night/` | 4 | complete — hero scene, canvas, timeline strip, twilight LUT ([`hero-surface`](45e1a74b-0208-4ace-a8b0-ae3e1e2b8266); known gaps below) |
| `ui/brief/` | 2 | complete — `BriefScreen`, `BriefSections` (same worker) |
| `ui/targets/` | 3 | complete — `TargetsScreen`, `TargetDetail`, `TargetRow` (landed by worker `627a3b20`) |
| `ui/share/` | 3 | complete — `ShareScreen`, `BriefShareText`, `PostcardExport` (same worker; landed after the first draft of this file) |
| `ui/settings/CreditsScreen.kt` | 1 | complete — eight attribution rows + licence links (same worker) |
| `ui/spots/` | 4 | complete — list, edit (+ horizon editor), coordinate picker, horizon editor |
| `ui/welcome/` | 1 | complete |
| `ui/widget/` | 3 | complete — Glance widget, its receiver, its state |
| `app/src/test/` | 18 | written, **never executed** |

Vendored engines present: `app/src/main/java/io/github/cosinekitty/astronomy/astronomy.kt`
(Astronomy Engine, in-module source), Orekit 13.1.9 + Hipparchus 4.0.3 via
`gradle/libs.versions.toml`, `assets/orekit-data/tai-utc.dat` (3 321 B).

`AndroidManifest.xml` declares exactly the two permissions from `design.md` —
`INTERNET` and `POST_NOTIFICATIONS` — a `FLAG_GRANT_READ_URI_PERMISSION` provider, the widget
receiver (not exported), and the AppCompat locale-metadata service. No location, no storage,
no exact-alarm, no boot receiver. `res/xml/` carries `backup_rules`, `data_extraction_rules`,
`file_paths`, `locales_config`, `network_security_config`, `night_glance_widget_info`.
Locales present: `values`, `values-night`, `values-ru`. Drawables: 45 (`ic_ui_*` Lucide set).

---

## 2. What must be finished (ordered; the first three block the app from being usable)

### 2.1 MISSING — the catalogue data assets (highest priority)

```
OK    207 172 B  app/src/main/assets/catalog/objects.json
MISS              app/src/main/assets/catalog/constellations.json
MISS              app/src/main/assets/sky/constellation_lines.json
```

**`objects.json` is present** (re-measured 207 172 B). The asset-baker worker
[`bundle-sky-assets`](b272f277-4e78-4aaf-b76d-815e1559a7ba) **failed with "Connection failed
repeatedly"** and did **not** deliver `constellations.json` or `constellation_lines.json` — those
two remain the catalogue gap. Do not relaunch that worker id; finish the bakers in the next session.

`data/CatalogueRepository.kt` reads `catalog/objects.json` (`{schemaVersion, manifest, objects[]}`)
and `catalog/constellations.json` (88 rows) and returns a **typed absence** when a file is missing —
so the app will not crash, but the constellation half of the catalogue stays empty.
`constellationLines()` looks for `assets/sky/constellation_lines.json`.

The bakers already exist and were authored for exactly this:
`tools/fetch-objects.py` (14 639 B), `tools/bake-objects.py` (19 941 B), `tools/common.py`,
`tools/fetch-stars.py`, `tools/bake-stars.py`, `tools/stars_common.py`, `tools/bake-geo.py`,
`tools/bake-milkyway.py`, `tools/bake-moon.py`, `tools/bake-sky-lut.py`, `tools/_probe.py`.

**Next action:** run the two remaining bakers (constellations + constellation lines) and confirm
both files exist and parse. Note `app/src/main/assets/geo/coastline_110m.bin` (37 712 B) and
`catalog/stars.bin` (45 694 B) are present, so `bake-geo.py` and `bake-stars.py` ran. Obey
`.cursor/rules/workspace-hygiene.mdc`: any scratch a baker writes goes under `.kiln/scratch/`.

**Wikidata probe leftover (2026-10-09):** `.kiln/scratch/lumintra-assets/probe-wd-const3.py`
returned `with enwiki title: 86` and `Aries in all: False Cancer in all: False` — the 88 IAU set
is incomplete via that query path; the baker for `constellations.json` must not assume every
constellation has an enwiki-titled Wikidata hit.

### 2.2 LANDED — share / targets detail / credits (was MISSING)

```
OK  853  ui/targets/TargetsScreen.kt
OK  585  ui/targets/TargetDetail.kt
OK  225  ui/targets/TargetRow.kt
OK  426  ui/share/BriefShareText.kt
OK  618  ui/share/PostcardExport.kt
OK  953  ui/share/ShareScreen.kt
OK  493  ui/settings/CreditsScreen.kt
```

**Landed after the first draft of this file** by worker
[`targets-share-credits`](627a3b20-23ed-4a5d-a666-d4995ce97645) (4 153 lines across the seven
files). Re-measured 2026-10-09: every path above exists with the line counts shown. Earlier `MISS`
rows for these four share/credits screens are **stale**.

What the worker itself marked `UNVERIFIED` (and which still stands):

- **Nothing was compiled or run** — compile-level correctness against sibling files is unproven.
- The **Night-scene** half of the ESO credit rule ("`ESO/S. Brunier` beside the Milky Way backdrop")
  is not in these seven files; that surface is `ui/night/NightSceneScreen.kt`.
- Locale parity and 48 dp were measured **at source**; the shipped APK has not been checked.

**Next action for these screens is not more writing — it is wiring.** They are still unreachable
until §2.3 (Navigation 3) lands them in `PlayKilnApp.kt`. Then the first `verify.mjs --build`
type-checks them against the rest of the tree.

### 2.3 MISSING — Navigation 3 wiring (orchestrator-owned, never started)

`ui/PlayKilnApp.kt` still routes **only** `HomeRoute`, `SettingsRoute` and `AboutScreen`. Every
screen written above is **unreachable**:

```
WelcomeScreen  SpotsScreen  SpotEditScreen  NightSceneScreen  BriefScreen
TargetsScreen  ShareScreen  CreditsScreen
```

**Next action:** add the `@Serializable NavKey` destinations and `entryProvider` entries for all
of them per their frozen signatures in `INTERFACES.md`, and point `MainActivity.kt` at the new
start destination. The scaffold leftover `ui/home/{HomeScreen,HomeUiState,HomeViewModel}.kt` and
`ui/home/HomeViewModelTest.kt` are still present — decide whether the Welcome screen replaces
`HomeRoute` (likely) or whether Home stays; `ui/home` is not part of the spec's destination list.

`ui/settings/SettingsScreen.kt` + `SettingsViewModel.kt` already exist and are wired; the
settings surface needs the **red mode** and **reminders** preferences to reach the real
behaviour (`.cursor/rules/app-settings.mdc`: every preference in a settings surface must change
real behaviour).

### 2.4 NEVER DONE — the build

**Nothing has ever compiled.** No `assembleDebug` has been run for this slot. Expect compile
errors from parallel-written files that reference each other's symbols by the frozen contract
but were never type-checked together. Known cross-file couplings worth checking first:
`TargetsScreen.kt` imports `data.sky.AstronomySkyEphemeris`, `data.sky.AltitudeModel`,
`domain.CrossingSearch`, `domain.ObjectVisibility`, `domain.WindowBoundary` — confirm each
exists with that exact name.

**Sky-engine gaps the worker recorded rather than inventing fields**
([`sky-engine`](07b03a85-055d-48dc-8f35-96b9e39d288f); full notes in
`.kiln/scratch/lumintra/SKY_ENGINE.md`):

- `SceneFrame` has **no constellation-figure field** — `SceneBuilder` emits none; the night
  renderer must project catalogue constellation assets with `skyRotationRad`, or the domain needs
  a new field (not added by that worker).
- Peak **azimuth** is not on `BriefFacts.Target` — only peak altitude and duration; a screen that
  wants "peaks at 187°" re-projects at `transitUtc`.
- `SkyBrightnessEstimate` is on `NightRequest`, not on `BriefFacts` / `SkyGrade` — Brief reads it
  from the request.
- 9 + 15 JVM tests written (`TwilightSearchTest`, `NightEngineTest`); **never executed**.

**Hero-surface gaps** ([`hero-surface`](45e1a74b-0208-4ace-a8b0-ae3e1e2b8266); six files on disk,
statically checked, never compiled):

- Milky Way uses `drawBitmapMesh` (`SceneCanvas.kt` ~853), not the spec's `drawVertices` +
  `ImageShader` (`night-scene/spec.md:578`) — mesh warp is present; the API letter is unmet.
- No `onTrimMemory` / `ComponentCallbacks` path — `drawnMagnitudeLimit` exists as a seam only.
- No `SavedStateHandle` / `rememberSaveable` — rotation survives via `NightViewModel` state; process
  death does not.
- No AGSL branch for sky layer 1 (LUT row-strip only — a legal fallback reading of the spec).

Then, per `.cursor/rules/build-speed.mdc`, one single Gradle invocation:

```bash
node scripts/run-slot-tests.mjs --slot lumintra-light-planner --rerun
node scripts/verify.mjs --slot lumintra-light-planner --build
```

`--build` compiles `assembleDebug`. Without a device the device row is `UNVERIFIED` (exit 2),
never a PASS.

### 2.4b THE MOON 359° CONTRADICTION — RESOLVED 2026-10-09 (owner's decision)

**Decision taken by the owner in-session: change the spec, not the code.** The spec's "not bucketed
as new" clause was the error; the centred bins stay, and `domain/Moon.kt` needed no change because it
already implemented the corrected requirement. The four artefacts were brought back into agreement in
this session:

| Artefact | Was | Now |
|---|---|---|
| `specs/session-brief/spec.md:269-271` | required 359° "not as new" **and** centred bins — impossible together | requires the `new` sector to be **one contiguous** 45° sector spanning the wrap; the withdrawn clause is recorded with the reason |
| the scenario at `specs/session-brief/spec.md:309-313` | named 359° a *waxing* crescent (astronomically wrong) | names it with the bin that contains 0°, and lists the concrete values that must resolve where |
| `design.md:316` | repeated the withdrawn gloss | states the contiguous-sector property |
| `domain/Moon.kt:13-18` (KDoc) | claimed it returns `WANING_CRESCENT` — **false** | states the truth: `NEW_MOON`, and why 359° is a waning crescent 1° short of new |
| `app/src/test/.../domain/MoonTest.kt` | `phaseNear360IsNotBucketedAsNewMoon` asserted the opposite of the code, so the first test run would have been red by construction | `theNewSectorIsContiguousAcrossTheWrapRatherThanNamedAtBothEnds`, asserting the property E19 actually refuses |

The reasoning, for the record: E19's *refusal* clause is the binding half — "the wrap bug: `new` at
both ends of the table" — and a 45° bin centred on 0° spans `[337.5°, 22.5°)`, so `new` is one
contiguous sector and never appears twice. 359° is 1° short of new and is astronomically a *waning*
crescent on its way to new; E19's own gloss ("so waxing crescent near 359° does not fall into the
wrong bucket") carries the same error and is left in the annex as provenance, with the spec and
`design.md` now naming the refusal clause as what binds.

**Expected values in the new test are MEASURED, not computed in the head** — the formula was ported
and run:

```
337.4 WANING_CRESCENT    337.5 NEW_MOON    0 NEW_MOON    22.5 WAXING_CRESCENT
359 NEW_MOON    360 NEW_MOON    -1 NEW_MOON    counts: every name 450 of 3 600
distinct: 8
```

**Honest limit, and the one thing left undone here:** the Kotlin has **not been compiled or executed**
— no Gradle run was permitted in this session, so `MoonTest` is still unrun and the corrected test is
`UNVERIFIED` as *code* even though its expected values are measured. The port above validates the
constants, not the Kotlin. The first `run-slot-tests.mjs --rerun` is what closes that gap, and the
`assertNotEquals` import was removed with the old test so an `allWarningsAsErrors` build cannot fail
on an unused import.

**(b) `SceneScrub.forInstant` still open** (`domain/SceneState.kt:234-239`): the companion's own KDoc
promises "the scrub value that resolves to an instant **inside** a session window", but the function
coerces the *value* into `[0,1]` and returns `instantUtc` **unclamped**, so an instant outside the
span yields e.g. `SceneScrub(value = 1.0, instantUtc = <past span end>)` — a pair `forValue(1.0, span)`
would never produce. No test asserts it; decide and record rather than leaving it implicit.

### 2.5 After the build is green

- Tamper the tests: 18 files were written but **never executed**; `MoonTest`,
  `SceneCameraTest`, `OpticsAndFitTest`, `DarkWindowClassifierTest` encode spec scenarios and are
  the most likely to surface real domain bugs. **The one failure that was predicted is no longer
  expected** — the Moon/359° contradiction was resolved by the owner's decision (see §2.4b) and both
  the spec and `MoonTest` were brought into agreement with the code, so a red `MoonTest` now means a
  genuine defect rather than a known disagreement. A worker's report claimed none of the tests pass,
  and none was run — treat every "the code is correct" claim in this session as `UNVERIFIED`.
- `.cursor/rules/device-testing.mdc`: install the debug APK on the **A40 `R58MB3SGZVL`**, exercise
  every screen, and write `device-verification.json` (serial, artifact sha256, `buildDigest`,
  ≥3 observed step/result rows). A handoff that asks the owner to test is a failed checkpoint.
- Run the in-process UI/UX sensors the slot has not faced yet:
  `node scripts/verify.mjs --slot lumintra-light-planner` adds the `ui` group (`uiux.*`,
  `settings.*`, `locale.*`) and the `policy` group (`slot.differentiators`, `slot.naming`,
  `slot.device`).
- `node scripts/check-differentiators.mjs --slot lumintra-light-planner` — **every promise in
  `differentiators.json` must leave `deferred`**, and at least one `shipped` claim must be a
  connective handoff family whose proof artifact contains that family's API
  (`.cursor/rules/app-depth.mdc`).
- Generate the baseline profile (`:baselineprofile` module) once the screens exist.
- Store pack: `store/` holds `listing.json`, `studio.json`, `privacy.md`, `data-safety.md`,
  `review-notes.md`, `release-notes.md`, `graphics.md`, `checklist.md` — but **no `store/assets/`**
  (correct for phase 1). Every one of these still needs review against
  `.cursor/rules/visual-identity.mdc` and the `store-pack contract` in `AGENTS.md`.

### 2.6 Phase 2 — do NOT start yet

Signed build, store render, screenshots and Play submission are forbidden until the owner
records a release approval in his own words (`.cursor/rules/release-gate.mdc`). No approval
exists for this slot.

---

## 3. Grounded claims about this session

| Claim | Label | Evidence |
|---|---|---|
| 75 main / 18 test Kotlin files exist (74 in the slot package alone) | MEASURED | `Get-ChildItem -Include *.kt` after share worker finished: `main 75 / 1761 KB`, `package 74 / 1338 KB` |
| `catalog/objects.json` (207 172 B) exists | MEASURED | `Test-Path` + `Length`, re-run after asset worker exit |
| `catalog/constellations.json` and `sky/constellation_lines.json` are absent | MEASURED | same run; asset worker `b272f277` exited Connection failed |
| hero night/brief six files exist on disk | MEASURED | sizes NightSceneScreen 102477 … BriefSections 64921 after worker `45e1a74b` |
| `ui/share/{ShareScreen,BriefShareText,PostcardExport}.kt`, `CreditsScreen.kt` and `TargetDetail.kt` exist | MEASURED | `Test-Path` + line counts after worker `627a3b20` finished (853/585/225/426/618/953/493) |
| `PlayKilnApp.kt` routes only Home/Settings/About | MEASURED | its `import` block, read this session |
| `ofElongation(359.0)` returns `NEW_MOON`, and `Moon.kt:13-18`'s KDoc claimed `WANING_CRESCENT` | MEASURED | `domain/Moon.kt:40-45` arithmetic and the enum declaration order at `:21-28`, read this session |
| `MoonTest` asserted the opposite of the code, so the first test run would have been red | MEASURED (then fixed) | old `MoonTest.kt` `phaseNear360IsNotBucketedAsNewMoon`; replaced this session by `theNewSectorIsContiguousAcrossTheWrapRatherThanNamedAtBothEnds` |
| The spec required 359° *not* to be new while also requiring centred bins — impossible together | MEASURED | `specs/session-brief/spec.md:269-271` and `:309-313`, read this session; the clause was withdrawn by the owner's decision |
| Every expected value in the replacement test is correct for the shipped formula | MEASURED | the formula ported to Node and run: `337.5/0/359/360/-1 → NEW_MOON`, `22.5 → WAXING_CRESCENT`, `337.4 → WANING_CRESCENT`, `450` samples named `new` of 3 600, 8 distinct names |
| The corrected `MoonTest` has never been compiled or executed | UNVERIFIED | no Gradle run in this session; the first `run-slot-tests.mjs --rerun` decides it |
| `SceneScrub.forInstant` returns an unclamped `instantUtc` beside a clamped `value` | MEASURED | `domain/SceneState.kt:234-239`, read this session |
| The app has never been compiled | INFERRED | no build has been run in this session; no evidence a prior session ran one |
| The 18 test files have never been executed | INFERRED | no test run exists in this session |
| `verify.mjs` is green **repo-wide** after the fix below | MEASURED | `62 PASS, 0 FAIL, 9 SKIP` (`verdict: SKIP`), re-run 2026-10-09, 29 763 ms |

A note on method, because it cost a wrong claim in this session: the Moon row was first reported
from the **KDoc** (`WANING_CRESCENT`) rather than from the arithmetic, and the two disagree. The
table above is the arithmetic's answer. `.cursor/rules/grounded-claims.mdc` names exactly this
failure — a summary of a source is an inference *about* the source, never the source.

---

## 4. Factory-side change made in this session (not part of the slot)

`scripts/core/build.mjs` — added `'com.sun'` to `ALLOWED_PACKAGE_ROOTS` and a fixture leg that
fails on the old behaviour.

The `build.namespaceSweep` check (and the repo-wide leg of `build.fixtures`) failed on
`app/build.gradle.kts:108` — which is the **comment** `// com.sun.source.util.Plugin is a javac
plugin service…` documenting the packaging exclude on line 114, not the exclude itself (there a
slash precedes `com`, so the sweep never matched it). The finding was the sweep's false positive,
not a leftover: `com.sun` is the JDK's own namespace.

Falsified before it was trusted — `.kiln/scratch/lumintra/probe-namespace-allowlist.mjs`:

```
PASS  want 0 got 0  JDK service path in a comment (the line that failed two rows)
PASS  want 0 got 0  JDK service path in the real packaging exclude
PASS  want 1 got 1  an unlisted root still fires (proves the sensor is alive) -> com.acme (line 1)
PASS  want 1 got 1  a former-style root still fires -> com.oldprefix (line 1)
```

The third and fourth rows matter: they prove the allowlist extension did not silence the sensor.
`node scripts/verify.mjs` then reported `62 PASS, 0 FAIL, 9 SKIP`.

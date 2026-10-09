# Tasks: app-lumintra-light-planner

Slot: `apps/<date>-lumintra-light-planner` — **Lumintra: Light Planner** —
`com.gexetr.lumintralightplanner`.
Contracts: `docs/ideation/plans/lumintra-light-planner.economics.json` (`purchase_only_no_ads`, $9.99,
no capability required, `billing` and `ads` deliberately not enabled),
`docs/ideation/plans/lumintra-light-planner.differentiators.json` (**13 promises**, 5 core),
`docs/ideation/plans/lumintra-light-planner.naming.json` (name / slug / applicationId).

**One build policy.** Gradle is serial per slot (`.cursor/rules/build-speed.mdc`): all tasks for one
build go to **one** invocation, and the orchestrator runs it, not this document. The one authoritative
phase-1 build is `node scripts/verify.mjs --slot lumintra-light-planner --build` (which runs
`assembleDebug`). No build has been run for this slot at the time of writing — every "done" mark below
is about a file on disk, not about a compiled or tested artifact.

Legend: `[x]` done and MEASURED on disk in this session; `[ ]` pending; `[~]` in flight (files on disk,
not integrated); `[BLOCKED]` requires the owner's recorded release approval
(`.cursor/rules/release-gate.mdc`) or a device.

## 0. Recon and contracts (stage 0 – 0.8)

- [x] Positioned market record authored:
      `docs/research/2026-10-08-light-and-sky-session-planner-market.md` (consolidation of the six
      disposable recon files under `.kiln/scratch/positioning-2026-10-08/` — the real input path,
      **not copied into this change**).
- [x] Competitor / adopt / reject matrix and the Play-integration facts recorded:
      `annex/K-competitor-features.md` and `annex/M-play-integrations-compliance.md`.
- [x] Money plan authored: `lumintra-light-planner.economics.json`
      (`purchase_only_no_ads`, $9.99; `required: []`, `deliberatelyNotEnabled: ["billing","ads"]`;
      four rejected archetypes with revenue reasons; kill `120 days / 150 installs / $1,000`).
- [~] Promise contract authored from recon, before the UI: `differentiators.json` — **13 promises**
      (7 already on disk, **6 added in parallel with this change**: `render-the-night-scene`,
      `draw-the-horizon-line`, `see-your-lens-frame`, `carry-your-scout`, `glance-at-tonight`,
      `get-myself-outside`), 5 `core`, each with an incumbent, a limitation, an evidence strength and a
      source. **Re-read the count from the file before release** — a parallel writer owns the new rows.
- [x] Naming contract authored: `naming.json`; slug `lumintra-light-planner`, applicationId
      `com.gexetr.lumintralightplanner`, titles present in `en-US` (`Lumintra: Light Planner`) and
      `ru-RU` (`Lumintra: план съёмки`).
- [x] This OpenSpec change consolidated: `.openspec.yaml`, `proposal.md`, `tasks.md`, `design.md` and
      the eleven delta specs under `specs/<capability>/spec.md` (`openspec validate
      app-lumintra-light-planner`).

  - [ ] Re-run `openspec validate app-lumintra-light-planner` and record the observed result before the
        scaffold.
- [ ] Slot scaffolded from the template with the four inputs, once the change validates:
      `node scripts/new-slot.mjs --economics docs/ideation/plans/lumintra-light-planner.economics.json
      --differentiators docs/ideation/plans/lumintra-light-planner.differentiators.json
      --naming docs/ideation/plans/lumintra-light-planner.naming.json --spec
      app-lumintra-light-planner`. The scaffold refuses without all four.
- [ ] **Authoring inputs moved into the slot's own docs (Build, immediately after the scaffold)** so
      the knowledge travels with the app if it is ever cut out of the factory
      (`.cursor/rules/slot-docs.mdc`): move the six annexes — `annex/CONTEXT.md`,
      `annex/K-competitor-features.md`, `annex/L-depth-retention-edges.md`,
      `annex/M-play-integrations-compliance.md`, `annex/N-data-algorithms.md` and
      `annex/O-sky-scene-rendering.md` — into `apps/<date>-lumintra-light-planner/docs/`. The
      change-local `annex/` copy stays as the **pre-scaffold home** and is **not** deleted.
- [ ] **Positioning recon promoted into the slot's own docs (Build, immediately after the annex move)**
      so the market and engineering evidence the package cites travels with the app
      (`.cursor/rules/slot-docs.mdc`): the recon lives in the disposable
      `.kiln/scratch/positioning-2026-10-08/` directory, which a clone does not have. Promote into
      `apps/<date>-lumintra-light-planner/docs/` the files this package actually cites —
      `A-astrophotography-baseline.md` and `D-complaints-satellite.md` (cited by
      `annex/L-depth-retention-edges.md`), `B-stargazing-shelf.md` (cited by
      `annex/K-competitor-features.md`), `C-sun-moon-light-shelf.md` (cited by `proposal.md`),
      `E-money-repositioning.md` (cited by `annex/M-play-integrations-compliance.md`),
      `H-rendering-options.md` and `J-sky-realism-and-bar.md` (cited by
      `annex/O-sky-scene-rendering.md`), and `F-engine-gap-map.md`, `G-android-build-proof.md` and
      `I-assets-and-data.md` (cited by `design.md`, where the three are marked **not copied into this
      change**) — plus the SGP4 verification corpus those claims rest on: `SGP4-VER.TLE` (**8,616 B**)
      and `TCPPVER.OUT` (**140,162 B**, both measured under
      `.kiln/scratch/positioning-2026-10-08/tiny-data/`) and `itrf-versions.conf` (**3,478 B**,
      measured at `.kiln/scratch/positioning-2026-10-08/minimal-data/orekit-data/itrf-versions.conf`,
      whose plain prefixes are what the ITRF path requires). The remaining A–J files — `B-raw.md`,
      `C-raw.md`, `D-raw.md`, `D-raw-photo.md` and `D-satellite-aurora-adjacent.md`, which no file in
      this package cites — stay **disposable scratch** and are not promoted.
- [ ] **Slot-boundary guard (Build, before any slot-mode gate runs):** the two promotions above — the
      six annexes and the positioning recon — must complete before the first slot-mode gate
      (`node scripts/verify.mjs --slot lumintra-light-planner` and every other `--slot` check, §7), so
      the app's own market, engineering and design documents leave the factory with the app instead
      of staying behind in a change or a scratch directory (`.cursor/rules/slot-docs.mdc`). Confirm
      `apps/<date>-lumintra-light-planner/docs/` exists and holds the promoted files **before** §7's
      first gate is run; a gate run against a slot whose documents are still in the change is the
      condition this task exists to prevent.
- [ ] **Capability declaration (Build):** `apps/<date>-lumintra-light-planner/capabilities.json` enables
      **no** capability — `{"schemaVersion": 1, "enabled": []}` — because `purchase_only_no_ads` sells
      through the Play Console price, not an in-app product, and `billing` and `ads` are deliberately
      not enabled. Then run `node scripts/check-economics.mjs --slot lumintra-light-planner` and record
      the observed exit. A plan the build cannot honour is the defect this gate exists to catch.
- [ ] `store/studio.json` identity declaration (see §3), per the **`visual-identity`** capability —
      required **before** the UI is styled.

## 1. Domain — `domain/`, pure Kotlin, no Android imports (Build)

- [ ] `domain/Time.kt` — `NightSpan` (an absolute UTC interval), `EventInstant`.
- [ ] `domain/Twilight.kt` — `TwilightKind` (`CIVIL`, `NAUTICAL`, `ASTRONOMICAL`), `TwilightWindow`,
      `DarkWindowKind` (`DARK`, `POLAR_DAY`, `POLAR_NIGHT`, `NO_ASTRO_DARK`, `NO_DARK`),
      `DarkWindowReport`. Twilight angles are the geometric Sun-centre **−6° / −12° / −18°**; sunrise
      and sunset are a **different** test (zenith 90.8333° = 16′ semidiameter + 34′ refraction).
- [ ] `domain/Moon.kt` — `MoonState` (illuminated fraction, phase angle, phase name, altitude, azimuth),
      `MoonlessWindow`.
- [ ] `domain/SkyPosition.kt` — `HorizontalCoords` (altitude, azimuth degrees), `EquatorialCoords`
      (RA **in degrees**, Dec degrees, J2000). The vendored engine's `horizon()`/`defineStar()` take
      **hours** (`ra * 15.0`); convert at the boundary.
- [ ] `domain/SkyObject.kt` — `SkyObject`, `ObjectType`, `ObjectKind` (`FIXED`, `COMPUTED`),
      `ObjectVisibility`.
- [ ] `domain/TargetWindow.kt` — `TargetWindow` (rise/set/transit, or circumpolar, or never-up) for a
      named target on the chosen night.
- [ ] `domain/PassFinder.kt` — `SatellitePass`, `PassVisibility`, `ElementSetAge`. The pass search is
      **segmented** (≤3600 s windows, `maxCheck = 60 s`, a freshly built detector per segment); a single
      unsegmented multi-day `propagate()` reports 1 crossing where 15 exist. The element-set epoch is
      surfaced next to every prediction.
- [ ] `domain/CloudForecast.kt` — `CloudHour`, `CloudReport`, `ForecastAge`; hourly **total** cloud
      (`cloud_area_fraction`) only — **no low/mid/high split** — and the copy says *forecast*.
- [ ] `domain/HorizonProfile.kt` — `HorizonProfile` (the **exactly 360-sample** array), `HorizonSample`.
- [ ] `domain/Optics.kt` — `LensPreset`, `FieldOfView`, `TargetFit`.
- [ ] `domain/SceneState.kt` — `SceneFrame` (everything the canvas needs for one instant),
      `SceneScrub`.
- [ ] `domain/Scout.kt` — `Scout` (a saved spot), `EquipmentProfile`.
- [ ] `domain/Brief.kt` — `SessionBrief`, `BriefCondition`, `ConditionKind`.
- [ ] `domain/SkyGrade.kt` — `SkyGrade` (the computed darkness description; **never** a Bortle class).
- [ ] Every `searchAltitude`/`searchRiseSet`-shaped event search is **segmented** the same way as the
      pass finder — the trap applies to twilight, moonrise and target rise/set too.
- [ ] Observer sign convention fixed once: latitude **N+/S−**, longitude **E+/W−**, height in metres.

## 2. Data and assets (Build)

- [ ] `data/ScoutStore.kt` — saved spots + horizons + equipment, with an explicit durability token so
      the `pin-the-session-scout` / `carry-your-scout` proofs resolve across reinstall (backup rules in
      §5; a reinstall/clear-data restore test in §7).
- [ ] `data/SettingsStore.kt` — all preferences (units, language, red mode, reminders, review state);
      corrupt/unreadable preferences fall back to defaults.
- [ ] `data/CatalogueRepository.kt` — reads the bundled asset; never a network call.
- [ ] `data/CloudRepository.kt` — `api.met.no` client: identifying `User-Agent`, **coordinates truncated
      to 4 decimals** (5+ returns 403), `If-Modified-Since` caching, **no background retrieval while the
      app is not in use**, HTTPS only, and **graceful degradation** when the feed is unavailable.
- [ ] `data/ElementsRepository.kt` — CelesTrak **OMM/JSON** (not TLE; GP data is no longer available in
      TLE format for objects catalogued after 2026-07-11); stop on any non-200 and do not retry; at most
      one download per update.
- [ ] `data/SkyAssetSource.kt` — reads the bundled `.bin` and image assets.
- [ ] `data/SpotTransfer.kt` — the export/import codec for spots and horizons (a versioned, framework-free
      format that refuses an unknown version rather than throwing).
- [ ] Bundled assets authored and placed under `app/src/main/assets/`:
      `catalog/objects.json` (the **250** authored deep-sky rows — **110 Messier + 140 non-Messier
      NGC/IC showpieces**, every row verified against **two independent sources** — **Wikidata** (CC0,
      `P6257` RA / `P6258` Dec) plus **NASA HEASARC** and/or the discovery paper — with a computed
      `deltaArcmin`; the magnitude limit is raised to **10.5** only if fewer than **140** non-Messier
      rows qualify at **10.0**, and the **CC BY-SA** (OpenNGC, HYG) and **CC BY-NC** (NGC 2000.0,
      Hipparcos/Tycho) catalogues are excluded, with their reasons),
      `catalog/constellations.json` (the **88** constellation rows, each carrying the **IAU boundaries**,
      the **IAU area ranks** and the **IAU WGSN names**, **CC BY 4.0**), `catalog/stars.bin` (**FK6 ⊕**
      the hand-verified supplement for Sirius, Arcturus, Capella, Betelgeuse), `sky/sky_lut_a.bin` +
      `sky/sky_lut_b.bin` (the two twilight LUTs: **61 solar altitudes (+10° → −20°, step 0.5°) × 91
      elevations (0° → 90°, step 1°)**, 3 channels, **float16**, linear-light radiance relative to zenith
      ≈ **67 KB** for both), `sky/milky_way_eso0932a.webp` (ESO, **CC BY 4.0**, credit `ESO/S. Brunier`),
      `sky/moon_svs.webp` (NASA SVS Moon Kit, public domain), `sky/constellation_lines.json` (IAU
      figures, **CC BY 4.0**). Planets ship **no surface textures** — they are drawn as
      magnitude-scaled discs with a label.
- [ ] Record the provenance and licence for **every** bundled asset, and confirm no denylisted category
      is present: **OpenNGC / HYG are CC-BY-SA**, **NGC 2000.0 / Hipparcos / Tycho are non-commercial**,
      **Stellarium's is GPL**, and the shared **Gaia** panorama is **CC BY-SA 3.0 IGO** — none of these
      ships. The object table is compiled from facts; the Milky Way backdrop is the ESO `eso0932a`
      photograph (not the Gaia panorama).
- [ ] **Orekit runtime data committed with its provenance recorded (Build):** commit
      `assets/orekit-data/tai-utc.dat` (**3,321 B**, measured) into the slot's asset tree, and record
      in `docs/` where the bytes came from and how the file is refreshed when IERS publishes a new
      table. The file exists on this machine only under `.kiln/scratch/` — measured, all **3,321 B**,
      at `.kiln/scratch/positioning-2026-10-08/minimal-data/orekit-data/tai-utc.dat`,
      `.kiln/scratch/positioning-2026-10-08/tiny-data/orekit-data/tai-utc.dat`, and under
      `.kiln/scratch/android-orekit-smoke/` and `.kiln/scratch/nv-core-proof/` — so a clone has no
      copy of the file and a build input would be missing. The literal path
      `.kiln/scratch/positioning-2026-10-08/tai-utc.dat` does **not** exist. §5 states the size, the
      loader flag (`simpleEOP = true`) and that the file ships; the package names **no provider** for
      it (measured: `tasks.md` §5, `annex/N-data-algorithms.md` §3.3 and `design.md` "Required runtime
      data" state the size and flag only), so this task is the commit-and-provenance half.
- [ ] **Sky LUT baker (Build, run once by the builder):** `apps/<slot>/tools/bake-sky-lut.py` writes
      `app/src/main/assets/sky/sky_lut_a.bin` and `sky_lut_b.bin`. The file format (the axes, the
      `float16` channels, the linear-light radiance relative to zenith) is specified in `design.md`
      § "The LUT axes and colour-space discipline"; the app **never** computes the table at runtime and
      only samples the baked `.bin` assets.
- [ ] **FK6 bright-star supplement (Build):** author the supplement FK6 cannot supply — measured:
      Sirius, Arcturus, Capella and Betelgeuse are absent, and FK6's brightest row is `alpha Car` at
      `−0.62` (its full range is −0.62 … 9.71) — with **two independent sources** per row, then bake
      `catalog/stars.bin` = **FK6 ⊕ supplement**. FK6 is **4,151** rows (the recon's 4,150 is the older
      number and is superseded), CC0 1.0.
- [ ] **Constellation figures acquired and baked (Build):** acquire the **IAU / Sky & Telescope**
      constellation figures (**CC BY 4.0**) and bake the reduced
      `app/src/main/assets/sky/constellation_lines.json`, carrying the credit
      `Constellation figures: IAU and Sky & Telescope (Roger Sinnott, Rick Fienberg; patterns by Alan
      MacRobert) — CC BY 4.0`.

## 3. Identity and theme (Store + Build)

- [ ] `store/studio.json` (the **`visual-identity`** capability): `mark` (the slot's own drawn mark),
      the full `palette` (every role the renderer draws with), per-shot `headlines`/`kickers`,
      `featureTagline`, `featurePill`. Authored **before** the UI is styled; `scripts/store/render.py`
      refuses to render rather than fall back to the generic pack.
- [ ] `ui/theme/Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt` — the slot's own palette, one source of
      truth shared with `studio.json`, dark and light mapping, no Material-default purple seed and no
      dynamic colour.
- [ ] `ui/theme/NightVisionPalette.kt` — the true **red night mode** (red-on-black, no blue) applied to
      every screen, reachable by one tap from the Night scene and from Settings.
- [ ] Slot-owned components (not stock `Card` arrangements): `ui/components/LumintraMark.kt` (the brand
      mark, **drawn, not borrowed**), `SkyCard.kt`, `StatTile.kt`, `ConditionChip.kt`,
      `SectionStrip.kt`, `EmptyState.kt`, `PhaseDial.kt`.
- [ ] The identity survives translation: no layout whose style depends on English string lengths (a
      fixed-width pill, a tab row of labels, a one-line headline).
- [ ] Launcher icon + adaptive-icon foreground + monochrome/themed layer derived from `mark`; no baked
      rounded mask or shadow; ink inside the **66 dp** safe zone and the circle keyline.

## 4. Icons (Build)

- [ ] Vendor **one** licence-cleared professional UI-icon set into the slot with its licence text and a
      per-icon provenance record (`icons/provenance.json`, plus `docs/icon-provenance.md`); every
      general-purpose glyph (back arrow, plus, bin, settings gear, empty-state symbol) comes from it.
      **No document names a set**: choosing the set, its licence and its version is a decision the
      builder makes and records in `icons/provenance.json` before any UI icon is drawn.
- [ ] Every glyph is tinted from the theme and carries no colour of its own (`icons.tint`).
- [ ] The slot-owned **brand mark** is drawn, never a library glyph used as a mark
      (`.cursor/rules/visual-identity.mdc`).

## 5. Build and platform (Build)

- [ ] `app/build.gradle.kts` — toolchain from `AGENTS.md`, not a choice: Gradle **9.7.1**, AGP **9.4.1**
      with built-in Kotlin, Kotlin/Compose compiler **2.3.21**, Compose BOM **2026.09.00**,
      `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`, JDK **17**, Build Tools **36.0.0**, R8 full
      mode, `proguard-android-optimize.txt`. No `--no-daemon`; `org.gradle.caching` and
      `org.gradle.configuration-cache` stay enabled.
- [ ] `versionName` **`1.0.0`**, `versionCode` **1** — the identity the app sends in its `User-Agent` to
      both `api.met.no` and CelesTrak and shows in About.
- [ ] **Vendored engines and their runtime data (Build):** `astronomy.kt` (Astronomy Engine, Kotlin,
      **MIT**) for the Sun/Moon/planet geometry, and Orekit + Hipparchus (**Apache-2.0**) for the
      satellite SGP4 path, with both licence notices committed with the slot. **Measured** from the smoke
      test on disk (`.kiln/scratch/android-orekit-smoke/app/libs/` — the real path, **not copied into
      this change**): the jars' own
      `META-INF/maven/<group>/<artifact>/pom.properties` name **`org.hipparchus:hipparchus-{core,filtering,fitting,geometry,ode,optim,stat}:4.0.3`**. The smoke
      declares them as local files via `fileTree("libs")` (`.kiln/scratch/android-orekit-smoke/app/build.gradle.kts`
      — the real path, **not copied into this change**)
      and its `libs.versions.toml` names no coordinate, so the smoke's own build never resolved a
      Maven coordinate. **The coordinate is settled anyway, 2026-10-08 (MEASURED):** Maven Central's
      `repo1.maven.org/maven2/org/orekit/orekit/maven-metadata.xml` returns `<release>13.1.9</release>`
      (`<lastUpdated>20261003193228</lastUpdated>`), so the slot declares
      **`org.orekit:orekit:13.1.9`** + `org.hipparchus:*:4.0.3` in its version catalog — the smoke's
      `13.1.5` is four patch releases behind. Confirm the resolution with
      `./gradlew :app:dependencyInsight --dependency org.orekit:orekit --configuration releaseRuntimeClasspath`.
      Ship the runtime data `assets/orekit-data/tai-utc.dat` (**3,321 B**, taken from
      `https://maia.usno.navy.mil/ser7/tai-utc.dat` — the file's bytes there are identical, sha256
      `3524E1AE…10C3`) and load it with **`simpleEOP = true`**.
- [ ] **Vendor `astronomy.kt` at the pinned digest, not from `master` (Build).** Every line-number
      citation in `design.md` and `annex/` is of one revision: **sha256
      `D4213F7E2AFC4553DFDF1D78A9C453A5C420CB77B280A2A092BB607618ACD97B`, 432,946 B** (MEASURED
      2026-10-08, `.kiln/scratch/nv-core-proof/data/astronomy.kt`). Vendor exactly that file — the MIT
      header travels with it — or, if a different revision is taken, re-verify every cited line
      (`:196`, `:452`, `:1412`, `:6173`, `:6363`, `:6818`, `:6911`) against it before any code depends
      on the citation.
- [ ] **Re-take the build readings on the declared Orekit pin (Build).** The R8 class-survival counts,
      the zero-keep-rules result and the **4,379,153 B** APK were measured on the smoke's **13.1.5**
      jars; the declared coordinate is **13.1.9**, whose jar is 10,833 B larger. Re-measure on the pin
      and record the new numbers, so the design's size claims describe the shipped artifact.
- [ ] Dependency: `com.google.android.play:review:2.0.2` (+ `review-ktx:2.0.2`) from **Google's Maven
      repository**, licence the **Play Core SDK Terms of Service** (not Apache-2.0). Do **not** add
      `com.google.android.play:core` (the pre-split monolith).
- [ ] **No billing dependency** — do not add `com.android.billingclient`, a `billing` capability, a
      paywall screen or a `PurchaseAuthority`. There is nothing to sell in-app.
- [ ] `AndroidManifest.xml` permissions, minimised (`.cursor/rules/security-devsecops.mdc`):
      **`INTERNET`** only (the cloud feed and the element feed). Add **`POST_NOTIFICATIONS`** (API 33)
      **only** because the approved reminder and the widget need it. The app declares and requests **no
      location permission of any kind** — the assertion is that **none** of `ACCESS_COARSE_LOCATION`,
      `ACCESS_FINE_LOCATION` or `ACCESS_BACKGROUND_LOCATION` appears anywhere in the merged manifest. No
      storage permission — the share handoff uses `ACTION_SEND` + `FileProvider`
      (`FLAG_GRANT_READ_URI_PERMISSION`) and the export uses `ACTION_CREATE_DOCUMENT` (SAF).
- [ ] `res/xml/network_security_config.xml` — cleartext forbidden (HTTPS only).
- [ ] Backup rules split cloud backup from device transfer (`data_extraction_rules.xml`,
      `backup_rules.xml`), excluding secrets.
- [ ] `android:localeConfig="@xml/locales_config"` (`en`, `ru`) and
      `android:enableOnBackInvokedCallback="true"` declared; edge-to-edge.
- [ ] Navigation 3 host owns its back stack (`NavDisplay` + `rememberNavBackStack`); predictive back works.
- [ ] **The one authoritative single-invocation build (Verify):**
      `node scripts/verify.mjs --slot lumintra-light-planner --build`. **Not run yet.**
- [ ] After that build, read the **merged** release manifest and record the observed permission list in
      `store/checklist.md` (the review library adds **no** permission and no manifest component, but the
      merged manifest is what Play reads). `INFERRED` until read.
- [ ] Generate a baseline profile once the screens exist (`:baselineprofile` module).

## 6. Screens and strings (Build)

- [ ] `ui/night/NightSceneScreen.kt` — the hero **Night scene**: the layered 2-D scrubbable scene (the
      twilight LUT base, Compose `Canvas` geometry above it), the user's drawn **horizon line** in the
      foreground, the moon, the Milky Way core and band, the deep-sky targets, the **lens frame** overlay
      and the satellite **streak flag**. Rendering is **layered 2-D**; an AGSL `RuntimeShader` is an
      **optional enhancement on API 33+ only**, never the sole path. Every object moves with the scrub;
      the stage is a **label, never a keyframe**.
- [ ] `ui/brief/BriefScreen.kt` — the **session brief**: the scene at the top, the answers scrolled
      under it (dark window, moon, targets, passes, cloud), with a **Tonight** summary first. States:
      loading, empty, error, **offline** (forecast/passes unavailable — the local half is unaffected),
      no-darkness (`POLAR_DAY` / `NO_ASTRO_DARK`), no-spot.
- [ ] `ui/targets/TargetsScreen.kt` — browse, **search** and filter the 250-object catalogue (by name,
      catalogue number and a magnitude limit). The list order is the one the requirement defines —
      **magnitude ascending, magnitude-unknown rows last, then by `id`**
      (`specs/target-catalogue/spec.md` "The user can find a target by designation, name or alias, and
      filter the list") — and **never a per-night visibility ranking**: no requirement in this package
      defines one, so this task must not ask for it. The chosen-night half is a **displayed fact**, not
      an order: for the active spot and the night each row shows the computed `TargetWindow`
      (rise/transit/set, circumpolar, or `target_never_rises`), and a never-rises target is **excluded
      from "what is up tonight"** exactly as that requirement states.
- [ ] `ui/spots/SpotsScreen.kt` — the saved-spot list; `ui/spots/SpotEditScreen.kt` — create/edit one
      spot including the **horizon editor** (inside `SpotEdit`, never a separate destination). SpotEdit
      offers manual coordinates, a saved spot, pasted coordinates and the **slot-owned coordinate picker**
      (`ui/spots/CoordinatePicker.kt`, design.md §11) — a Compose `Canvas` + `pointerInput` surface with a
      draggable pin, and **no map SDK, no tile request, no key, no new permission and no network call**.
      The optional place name uses **`android.location.Geocoder` only** (keyless — a third-party geocoding
      API would need a credential and is forbidden): check `Geocoder.isPresent()` first, call the
      asynchronous overload on API 33+, and the deprecated synchronous overload **off the main thread** on
      API 26–32. An absent implementation, offline, `null` or empty result degrades to coordinates plus the
      user's own label, and the spot is labelled `approximate`. **No location permission.**
- [ ] `ui/share/ShareScreen.kt` — compose and send the postcard / text brief (`ACTION_SEND`), and the
      export/import of spots and horizons (`ACTION_CREATE_DOCUMENT` + `FileProvider`).
- [ ] `ui/settings/SettingsScreen.kt` — units, language, **red mode**, reminders, the **Rate Lumintra**
      row (opens the Play listing, **never** the review API), About and Credits.
- [ ] `ui/settings/CreditsScreen.kt` — every licence and attribution the app must display, **each credit
      a `translatable="false"` resource** (a legal string, never translated): **MET Norway** — the exact
      string `Data from MET Norway (met.no) — CC BY 4.0`, with `https://www.met.no` / `https://api.met.no`
      and `https://creativecommons.org/licenses/by/4.0/`, a modification statement, and the word "Yr"
      never used; **ESO/S. Brunier (CC BY 4.0)**, this credit **beside the image**, not only in the list;
      **NASA SVS Moon Kit (public domain, acknowledgement, no NASA insignia, no implied endorsement)**;
      **IAU / Sky & Telescope** — the exact string `Constellation figures: IAU and Sky & Telescope (Roger
      Sinnott, Rick Fienberg; patterns by Alan MacRobert) — CC BY 4.0`; **FK6 (CC0 1.0)**; and the two
      software notices `astronomy.kt` — Astronomy Engine (Kotlin), MIT and Orekit 13.1.9 + Hipparchus
      4.0.3, Apache-2.0. Solar System Scope is **absent**: planets ship no surface textures and are drawn
      as magnitude-scaled discs with a label.
- [ ] `ui/welcome/WelcomeScreen.kt` — first run: one screen that ends at a working `Night` scene, and
      that **explicitly says the app does not use the compass** (buyers arrive from apps that do).
- [ ] **In-App Review wiring (Build):** fire `ReviewManager.requestReview()` →
      `launchReview(activity, info)` **once, at an earned moment** (after a brief exists / after a share),
      from the hosting Activity, with the result **ignored** and the app flow unchanged — no toast, no
      retry, no sentiment question, no custom scrim over the card. The Settings row opens the Play
      listing via `ACTION_VIEW` + `setPackage("com.android.vending")`, **never** the API.
- [ ] Capability surfaces, one per delta spec in `specs/` — **eleven** capabilities: `night-scene`,
      `session-brief`, `target-catalogue`, `satellite-passes`, `cloud-forecast`, `scout-spots`,
      `share-brief`, `night-glance` (the home-screen widget + the opt-in local reminder),
      `settings-and-credits`, `welcome` (the first-run screen) and `visual-identity` (the app's declared
      identity, authored before the UI is styled).
- [ ] Every new user-facing string added to **both** `res/values/strings.xml` and
      `res/values-ru/strings.xml` in the same change (`slot.locales`); never a literal in code.
- [ ] Every interactive element named, has a TalkBack role, and a target of at least **48 dp**; the
      keyboard is placed for every text field.
- [ ] Every screen reads the window size class and works in portrait and landscape
      (`.cursor/rules/ui-ux-baseline.mdc`).

## 7. Tests (Build + Verify)

- [ ] Domain JVM tests: twilight and **dark-window** computation against fixed fixtures; moon state;
      target windows; the **segmented pass finder** asserting the 3-day window returns all rises an
      independent 60 s brute-force scan finds (the 1-vs-15 negative control) and that `ElementSetAge` is
      surfaced; the cloud-report parse of an `api.met.no` fixture (total cloud only); optics FoV.
- [ ] `data/SpotTransfer.kt` tests: round-trip, refused unknown version, malformed payload → failure not
      crash.
- [ ] Scene-state test: one `SceneFrame` at a fixed instant carries every element the canvas draws.
- [ ] `domain/SkyGrade.kt` test: the darkness description is computed from quantities and **never**
      renders a Bortle class.
- [ ] Share-payload test: the text handed to `ACTION_SEND` carries the location, the light and dark
      windows and the chosen targets.
- [ ] Export/import test: `ACTION_CREATE_DOCUMENT` + `FileProvider` write and read back a spot set.
- [ ] Review-path test: the API path never gates on sentiment and never changes app flow on any result;
      the Settings row builds the Play-listing intent.
- [ ] Run everything and read it: `node scripts/run-slot-tests.mjs --slot lumintra-light-planner --rerun`
      (`--rerun` is required — a `res/`-only change reports `STALE`). **UNVERIFIED** — not run.
- [ ] `node scripts/verify.mjs --slot lumintra-light-planner` (source gates), then the `--build` run.
- [ ] `node scripts/check-differentiators.mjs --slot lumintra-light-planner` **without**
      `--allow-deferred` (fails while any promise is deferred — never cite a completion before every one
      is `shipped`/`narrowed`/`dropped`).
- [ ] `node scripts/check-client-secrets.mjs --slot lumintra-light-planner` (no client-side paid-API
      credential; the feeds are keyless).
- [ ] **Publish the privacy policy as a build input (Build), before the in-app check can pass:**
      `node scripts/publish-privacy.mjs --slot lumintra-light-planner` (the factory hosts it — the owner
      is never asked for a host) writes the real public HTTPS `privacyPolicyUrl`; the URL must exist
      **before** `node scripts/check-in-app-policy.mjs --slot lumintra-light-planner` can pass, because
      the app must be able to open the declared policy. This is a build input, not a release-time step.
- [ ] **Data-safety pack (Store):** fill `store/data-safety.md` to match the shipped app — declare
      **Location → Approximate location**, collected, **App functionality**, **optional**, encrypted in
      transit, because sending a saved coordinate to `api.met.no` **is** a collection under Play's
      definition ("transmitting data off a user's device", irrespective of destination). State that it
      is the **only** row, and that an over-declaration to "Precise location" is available if the auditor
      prefers the stricter reading. The privacy policy must name `api.met.no` and must **not** say the app
      collects nothing.
- [ ] Confirm no incentive, reward or sentiment gate attaches to the review at any point
      (`store/checklist.md`), and that `store/listing.json` carries no reserved-host placeholder.

## 8. On-device verification (phase 1, agent-owned)

The test target is the attached **Samsung Galaxy A40** — `SM-A405FM`, serial **`R58MB3SGZVL`**,
Android **11 / API 30**, 1080×2340 @480dpi, Exynos 7904, **Mali-G71 MP2**. It is **below the AGSL
floor**, so the **layered-2-D fallback** is the path that must be beautiful and the path that gets
verified. Open the run with `adb -s R58MB3SGZVL shell getprop ro.product.model` reading `SM-A405FM`;
if it does not, stop.

- [ ] Install the debug APK from the revision under review and launch it
      (`.cursor/rules/device-testing.mdc`).
- [ ] Drive the first-run `Welcome` → `Night` journey on the A40 and confirm the fallback scene renders
      the horizon line, the moon, the Milky Way band and the scrubbed time motion.
- [ ] Scrub the `Night` scene across a full night and observe the LUT twilight transitions; photograph
      what appears.
- [ ] Create a spot with a drawn horizon and a lens; confirm the lens frame and target fit appear.
- [ ] Open the **Brief**, the **Targets** search and a target window; confirm the dark window and the
      target altitudes are shown for the chosen night.
- [ ] Turn off the network and confirm the app still plans (offline-first) and the cloud/pass rows
      degrade to a stated offline state rather than an error.
- [ ] Confirm **no location permission prompt** ever appears and the share/export leaves the app
      (`ACTION_SEND` + `ACTION_CREATE_DOCUMENT`).
- [ ] Confirm the **red mode** switches the whole UI without blue light and survives rotation.
- [ ] Measure the frame time with `dumpsys gfxinfo` against the **≤ 12 ms** budget; until measured,
      "it is smooth on the A40" stays **UNVERIFIED**. State the reading.
- [ ] Write `apps/<date>-lumintra-light-planner/device-verification.json` with the serial, timestamp,
      artifact path and sha256, the revision `buildDigest`, and at least three "what was tapped / what
      appeared" observations, so `slot.device` can cross-check the record.
- [ ] **Shader path (A16 only):** the second phone **SM-A165F** (API 36) is authorised **only** to check
      the optional AGSL `RuntimeShader` enhancement on API 33+; do not drive it for anything else. Confirm
      the shader path produces the same scene the fallback did and record the observed result.

## 9. Phase 2 — release — every item [BLOCKED] until the owner's recorded approval

- [BLOCKED] Record the owner's stage-5 approval in his own words:
      `node scripts/approve-release.mjs --slot lumintra-light-planner --note "<the owner's own words>"`.
- [BLOCKED] `node scripts/prepare-release.mjs --slot lumintra-light-planner` (signed APK + AAB + release manifest).
- [BLOCKED] `python scripts/store/render.py --slot lumintra-light-planner` (listing copy, icon, feature
      graphic and the full screenshot inventory rendered from `store/studio.json`).
- [BLOCKED] `node scripts/media-capture.mjs --slot lumintra-light-planner` with a device attached; a
      stale install is fatal.
- [BLOCKED] `python scripts/store/validate.py --slot lumintra-light-planner` and
      `python scripts/visual-qa.py --slot lumintra-light-planner`.
- [BLOCKED] `python scripts/check-16k.py` over the signed artifacts.
- [BLOCKED] Fill `store/privacy.md`, `store/data-safety.md`, `store/review-notes.md`,
      `store/release-notes.md` and `store/graphics.md` to match the shipped app.
- [BLOCKED] Publish the privacy policy and confirm the in-app half:
      `node scripts/publish-privacy.mjs --slot lumintra-light-planner` and
      `node scripts/check-in-app-policy.mjs --slot lumintra-light-planner`.
- [BLOCKED] `node scripts/verify.mjs --slot lumintra-light-planner --build` as release evidence, then
      `/eval` and `play-auditor`.
- [BLOCKED] Update `registry/apps.json`, then **human checkpoint 2 — publish authorization**; on the
      owner's yes the agent walks `docs/play-publishing-playbook.md` §5.1 end to end (declarations, Data
      safety / content rating, the release form, save, submit, Publish) and presses Publish. Tooling
      assists only: store-graphic file inputs, the AAB file chooser, and the promo-code download clicked
      **once**.
- [BLOCKED] After publish, set `registry/apps.json` lifecycle to `released` and record the first dated
      outcome reading for `node scripts/check-outcomes.mjs --slot lumintra-light-planner` (kill window
      **120 days**, floors **150 installs / $1,000 net**).

# CONTEXT — Lumintra specification authoring (2026-10-08)

**Read this file before writing a single line.** Several authors write the specifications in
parallel, and this is the only thing keeping them coherent. If you invent a name, a path, a type or a
screen that is not in §3, another author has invented a different one, the two documents disagree, and
the implementer will hit exactly the class of bug the owner is trying to prevent. Use these names
verbatim. If you believe a name here is wrong, write your document with the name given and record the
objection in your report — never silently rename.

---

## 0. What we are doing, and what we are not

- We are writing the **specification** for one application. Nothing else.
- **No application code.** No Kotlin, no Gradle, no XML, no `new-slot.mjs`, nothing under `apps/`.
  Not even a "small example class to make it clear" — the implementer's job starts after this.
- No file outside the ones your brief names. Several authors write at once; two writers on one file
  corrupt each other and the corruption looks like a mystery defect later.
- Do not run Gradle. Do not build. Do not open a browser to "check the mockup" — the mockup is on
  disk at the path named in §6.
- The one exception in the whole exercise: a worker whose brief says so may run a **read-only**
  probe (a `rg`, a `Test-Path`, a `node -e` that reads a file). Never a write outside `.kiln/scratch/`.
- Everything you write goes under `.kiln/scratch/` or the two named repository paths. **Never** the
  system temp directory (`%TEMP%`, `%TMP%`, `$TMPDIR`) and never anywhere else outside the repository
  (`.cursor/rules/workspace-hygiene.mdc`).

## 1. The product, in one paragraph

**Lumintra** is a paid Android app for outdoor and night photographers. It plans one shoot: it takes a
place the user saved, a night, and a lens, and answers — when is it actually dark, where is the moon,
which of the ~250 famous deep-sky objects will be up and for how long, will a satellite streak the
frame, what will the clouds do, and will it all fit inside the field of view. It answers all of that
on **one scrubbable scene** that the user drags through the night, with their own drawn horizon
silhouette in the foreground and the real sky above it.

## 2. Product decisions already taken — do not reopen

The owner approved each of these explicitly. A document that re-argues one of them is wasted work.

| Decision | Value |
|---|---|
| Business shape | `purchase_only_no_ads`, one-time **$9.99** set in Play Console. **No in-app product, no billing library, no ads, no subscription, ever.** |
| Buyer | The outdoor / night / landscape photographer planning a shoot |
| Hero surface | The **scrubbable Night scene**. The owner's standard: it must not look like a form with a button, and it must be the most visually striking app this factory has produced |
| Rendering | **Layered 2-D.** A twilight colour LUT as the sky base, Compose `Canvas` geometry above it. **AGSL `RuntimeShader` is an OPTIONAL enhancement on API 33+ only, never the sole path.** No 3-D engine |
| Test device | Samsung **Galaxy A40**, `SM-A405FM`, serial `R58MB3SGZVL`, Android **11 / API 30**, 1080×2340 @480dpi, Exynos 7904, **Mali-G71 MP2**. It is **below** the AGSL floor, so the fallback path is the path that must be beautiful and the path that gets verified. A second phone, an **A16** (`SM-A165F`, API 36), is authorised **only** to check the shader path |
| Catalogue | **~250 hand-compiled famous objects** (Messier + showpiece NGC/IC). No third-party catalogue ships: OpenNGC and HYG are CC-BY-SA; NGC 2000.0 and Hipparcos/Tycho are non-commercial; Stellarium's is GPL. Coordinates and magnitudes are **facts** and are authored, verified against two independent references |
| Stars | **FK6 (CC0)**, **plus a hand-verified supplement** — measured: FK6 lacks Sirius, Arcturus, Capella and Betelgeuse, so a supplement is mandatory |
| Sky backdrop | ESO photograph `eso0932a`, **CC BY 4.0**, credit `ESO/S. Brunier` — ESO states the credit is mandatory and must be visible |
| Moon | NASA SVS Moon Kit, public domain |
| Planets | Solar System Scope textures, CC BY 4.0 |
| Constellations | IAU constellation figures, CC BY 4.0 |
| Weather | **`api.met.no`** only — keyless, CC BY 4.0 with attribution, hourly **total** cloud cover (`cloud_area_fraction`) with **no** low/mid/high split. The non-commercial Open-Meteo free tier is **not** used |
| Light pollution / Bortle | **Not shipped.** The widely used Falchi atlas is **CC BY-NC** and cannot go in a paid app. No raster in v1; darkness is described from computed quantities plus the user's own input |
| Satellites | Orbital elements fetched at runtime. **The feed is OMM/JSON, not TLE** — measured: CelesTrak states GP data is no longer available in TLE format for objects catalogued after 2026-07-11 |
| Newly approved additions | Milky Way core and band on the scene · star-trail mode on the timeline · a "Tonight" summary that needs no configuration · a twilight/golden/blue-hour table · a true red night mode · a home-screen widget · one opt-in local reminder · export **and** import of the user's spots and horizons |
| Rating | A **Rate us** row in Settings that opens the Play listing, **plus** the automatic Play In-App Review prompt at an earned moment. Google forbids a button from launching the review flow — see §5 |

## 3. Canonical vocabulary — use these names verbatim

### 3.1 Slot identity

| Thing | Value |
|---|---|
| App name | **Lumintra** — the store title is derived from `naming.json` |
| Slug | `lumintra-light-planner` |
| `applicationId` | `com.gexetr.lumintralightplanner` |
| Change name | `app-lumintra-light-planner` |
| Slot root | `apps/<date>-lumintra-light-planner/` — write all slot paths **relative to the slot root**, e.g. `app/src/main/java/...`, never with a guessed date |
| Kotlin package | `com.gexetr.lumintralightplanner` |
| Root package path | `app/src/main/java/com/gexetr/lumintralightplanner/` |

### 3.2 Toolchain (from `AGENTS.md`, do not restate as a choice)

Gradle **9.7.1** · AGP **9.4.1** with built-in Kotlin · Kotlin/Compose compiler **2.3.21** ·
Compose BOM **2026.09.00** · `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26` · JDK **17** minimum ·
Build Tools **36.0.0** · R8 full mode · **Navigation 3** · edge-to-edge · predictive back.

### 3.3 The screens — these names, spelled like this

| Screen | Meaning | Canonical file |
|---|---|---|
| `Night` | The hero. The scrubbable scene and everything on it | `ui/night/NightSceneScreen.kt` |
| `Brief` | The session brief: the scene at the top, the answers scrolled under it | `ui/brief/BriefScreen.kt` |
| `Targets` | Browse, search and filter the object catalogue | `ui/targets/TargetsScreen.kt` |
| `Spots` | The list of saved observing spots | `ui/spots/SpotsScreen.kt` |
| `SpotEdit` | Create or edit one spot, including the horizon editor | `ui/spots/SpotEditScreen.kt` |
| `Share` | Compose and send the postcard / the text brief | `ui/share/ShareScreen.kt` |
| `Settings` | Units, language, red mode, reminders, Rate us, About and Credits | `ui/settings/SettingsScreen.kt` |
| `Credits` | Every licence and attribution the app must display | `ui/settings/CreditsScreen.kt` |
| `Welcome` | First run: one screen that ends at a working `Night` scene | `ui/welcome/WelcomeScreen.kt` |

The horizon editor is **inside** `SpotEdit`, never a separate destination.

### 3.4 Domain types — `domain/`, pure Kotlin, no Android imports

| File | Types it owns |
|---|---|
| `domain/Time.kt` | `NightSpan` (an absolute UTC interval), `EventInstant` |
| `domain/Twilight.kt` | `TwilightKind` (`CIVIL`, `NAUTICAL`, `ASTRONOMICAL`), `TwilightWindow`, `DarkWindowKind` (`DARK`, `POLAR_DAY`, `POLAR_NIGHT`, `NO_ASTRO_DARK`, `NO_DARK`), `DarkWindowReport` |
| `domain/Moon.kt` | `MoonState` (illuminated fraction, phase angle, phase name, altitude, azimuth), `MoonlessWindow` |
| `domain/SkyPosition.kt` | `HorizontalCoords` (altitude, azimuth degrees), `EquatorialCoords` (RA degrees, Dec degrees, J2000) |
| `domain/SkyObject.kt` | `SkyObject`, `ObjectType`, `ObjectKind` (`FIXED`, `COMPUTED`), `ObjectVisibility` |
| `domain/TargetWindow.kt` | `TargetWindow` (rise/set/transit, or circumpolar, or never-up) |
| `domain/PassFinder.kt` | `SatellitePass`, `PassVisibility`, `ElementSetAge` |
| `domain/CloudForecast.kt` | `CloudHour`, `CloudReport`, `ForecastAge` |
| `domain/HorizonProfile.kt` | `HorizonProfile` (the 360-sample array), `HorizonSample` |
| `domain/Optics.kt` | `LensPreset`, `FieldOfView`, `TargetFit` |
| `domain/SceneState.kt` | `SceneFrame` (everything the canvas needs for one instant), `SceneScrub` |
| `domain/Scout.kt` | `Scout` (a saved spot), `EquipmentProfile` |
| `domain/Brief.kt` | `SessionBrief`, `BriefCondition`, `ConditionKind` |
| `domain/SkyGrade.kt` | `SkyGrade` (the computed darkness description; **never** a Bortle class) |

### 3.5 Data layer — `data/`

`ScoutStore.kt` (saved spots + horizons + equipment) · `SettingsStore.kt` (all preferences) ·
`CatalogueRepository.kt` (reads the bundled asset) · `CloudRepository.kt` (`api.met.no`) ·
`ElementsRepository.kt` (CelesTrak OMM/JSON) · `SkyAssetSource.kt` (the `.bin` and image assets) ·
`SpotTransfer.kt` (the export/import codec).

### 3.6 UI kit and theme — slot-owned, never stock Material arrangements

`ui/theme/Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`, `NightVisionPalette.kt` (the red mode) ·
`ui/components/LumintraMark.kt` (the brand mark, **drawn, not borrowed**), `SkyCard.kt`,
`StatTile.kt`, `ConditionChip.kt`, `SectionStrip.kt`, `EmptyState.kt`, `PhaseDial.kt`.

### 3.7 Bundled assets — `app/src/main/assets/`

| Path | What |
|---|---|
| `assets/catalog/objects.json` | the ~250 authored deep-sky rows |
| `assets/catalog/constellations.json` | the 88 constellation rows |
| `assets/catalog/stars.bin` | the FK6 ⊕ supplement star table, baked |
| `assets/sky/sky_lut_a.bin`, `assets/sky/sky_lut_b.bin` | the two twilight LUTs |
| `assets/sky/milky_way_eso0932a.webp` | the ESO backdrop |
| `assets/sky/moon_svs.webp` | the NASA moon |
| `assets/sky/planets/<name>.webp` | the Solar System Scope textures |
| `assets/sky/constellation_lines.json` | the IAU figure polylines |

### 3.8 Capabilities — the `specs/` directory names, exactly

`night-scene` · `session-brief` · `target-catalogue` · `satellite-passes` · `cloud-forecast` ·
`scout-spots` · `share-brief` · `night-glance` · `settings-and-credits`

### 3.9 The promise ids in the binding contract

Existing: `compose-the-light-and-dark-brief` (`core`, during) · `plan-deep-sky-targets` (`core`,
during) · `predict-visible-satellite-passes` (supporting, during) · `forecast-hourly-cloud-cover`
(supporting, during) · `pin-the-session-scout` (`core`, before) · `share-the-session-brief` (`core`,
after) · `search-the-deep-sky-catalogue` (supporting, during).

Added by this change: `render-the-night-scene` (`core`, during) · `draw-the-horizon-line`
(supporting, before) · `see-your-lens-frame` (supporting, during) · `carry-your-scout` (supporting,
after) · `glance-at-tonight` (supporting, before) · `get-myself-outside` (supporting, before).

### 3.10 Brand vocabulary — use these words in user-facing copy

"session" (not "plan" or "shoot" in UI) · "spot" (a saved place) · "horizon line" (the drawn
silhouette) · "dark window" (the computed moonless astronomical-dark interval) · "brief" (the
assembled answer) · "target" (a deep-sky object or a planet) · "pass" (a satellite pass) ·
"frame" (the lens field of view) · "Tonight" (the no-configuration summary).

## 4. The format your document must use

### 4.1 A capability specification — `specs/<capability>/spec.md`

Follow this shape exactly. It is the shape the factory's own accepted changes use and the shape
`openspec validate` expects.

```markdown
# <Capability display name>

## Purpose

<One paragraph: what this capability gives the user and why it exists. No requirements here.>

## ADDED Requirements

### Requirement: <a statement of the rule>

<Prose with SHALL / SHALL NOT. State the exact values, thresholds, formats and units. State what the
app refuses as well as what it does. This is the normative half — an implementer must not have to
guess and must not have to ask a question.>

#### Scenario: <a checkable example>

- **WHEN** <a situation, concrete and reproducible>
- **THEN** <the exact observable outcome>

<More scenarios. Aim for one scenario per way the requirement can be wrong.>

### Requirement: <the next rule>

...

**Honest boundary, recorded rather than glossed.** <When a requirement cannot be fully proved, say
exactly what is proved and what is not. Never let a sentence claim more than the check behind it.>

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
```

Rules for the requirements text:
- `SHALL` is normative. Say the **number**: "at least 48 dp", "at most 8 phone screenshots", "within
  ±0.5 degrees", "exactly 360 samples".
- Every requirement that can be violated in two directions needs a scenario for **each** direction,
  including the refusal.
- Never write "the app should be nice" or "the UI is beautiful". Write what can be observed.
- If a requirement depends on a decision from §2, restate the **value**, not the debate.

### 4.2 `proposal.md` and `tasks.md` and `design.md`

`proposal.md` sections, in this order:
`# Proposal: app-lumintra-light-planner` · a bold identity block (slot, name, applicationId,
archetype, price) · `## Why this slot, why now` · `## Demand evidence` · `## Economics` ·
`## Distinctness` · `## Internal-catalogue check (Repetitive Content)` · `## Non-goals`.
The three contract files at the top of `## Why this slot, why now` are
`economics.json`, `differentiators.json`, `naming.json` in `docs/ideation/plans/`.

`tasks.md` sections, in this order: a header naming the slot and the contracts, the **one build
policy** note (all tasks for one build go to a single Gradle invocation; the orchestrator runs it,
not this document), a legend line, then numbered sections `## 0.` … `## 9.` mirroring the factory
pipeline, each a checkbox list. Section 9 is phase 2 and every line in it starts `[BLOCKED]`.

`design.md` is one `# Design: app-lumintra-light-planner` title followed by `##` sections. It carries
architecture, the domain model, the mathematics, the trade-offs and the open items — **not** the
normative requirements, which live in the specs.

### 4.3 The durable research document — `docs/research/...`

`#` title, then these `##` sections **with exactly these numbers and titles**, because other
documents cite them by anchor:

```
## 1. The competitor feature matrix
## 2. The adopted additions
## 3. What is rejected, and why
## 4. The edge-case register
## 5. The Play integration facts
## 6. The licence and attribution facts
## 7. The data and algorithm contracts
## 8. The rendering contract
## 9. The claim ledger
```

## 5. Facts you must not get wrong

These were measured this session. Use them; do not re-derive them, and do not contradict them.

| Fact | Value |
|---|---|
| Play In-App Review | `com.google.android.play:review:2.0.2` (+ `review-ktx:2.0.2`), Google Maven. Licence is the **Play Core SDK Terms of Service**, not Apache-2.0. It declares **no permission** and **no manifest component** |
| The review rule that shapes the UI | Google: *"you should not have a call-to-action option (such as a button) to trigger the API"* — because the quota may be exhausted and the flow then shows nothing. **A button that launches the review flow is a defect. The `Rate us` row opens the Play listing instead** |
| The review API's silence | It reports **nothing** back — not whether the dialog appeared, not whether the user rated. No toast, no thanks, no retry, no sentiment question before it |
| Location permission | **None needed.** Manual coordinates, a saved spot, the app's own coordinate picker and `Geocoder` all work without it. `ACCESS_COARSE_LOCATION` would buy only "where am I standing", which this product does not need |
| `POST_NOTIFICATIONS` | Needed **only** because the reminder and the widget were approved. Android 13 / API 33 |
| Data safety | Sending a saved coordinate to `api.met.no` **is** a collection — Play: *"'Collect' means transmitting data from your app off a user's device"*, and it counts *"irrespective of whether data is transmitted to you or a third-party server"*. Declare Location → Approximate, App functionality, optional, encrypted in transit |
| Privacy policy | Required by Play regardless; the factory publishes it itself (`docs/privacy-policy-hosting.md`) and the app must be able to open it |
| `api.met.no` duties | identifying `User-Agent` required · `If-Modified-Since`/caching required · **coordinates capped at 4 decimals, 5+ returns 403** · **no background retrieval while the app is not in use** · CC BY 4.0 attribution required · the name/logo "Yr" must not be used |
| CelesTrak duties | stop on any non-200 and do not retry · at most one download per update, GP updates every 2 hours · **GP data is no longer available in TLE format for objects catalogued after 2026-07-11** → the parser reads **OMM/JSON** |
| SGP4 fidelity | Orekit's SGP4 matches the reference implementation to `pos ≈ 1e-4 km`. This is **propagator fidelity, never real-world accuracy** — the same propagator drifts **27,924 km at 21.3 days**. Never present the small number as accuracy on the sky |
| The event-search trap | A single `propagate()`/search call over a long window returns **1** crossing where **15** exist. Requires **segmenting** (3600 s windows, `maxCheck = 60 s`, a freshly built detector per segment) — and it applies to twilight, moonrise and target rise/set searches too, not only satellites |
| Twilight angles | geometric Sun-centre **−6° / −12° / −18°** (USNO). Sunrise/sunset is a **different** test: zenith 90.8333° = 50′ = 16′ semidiameter + 34′ refraction |
| RA units trap | Store right ascension in **degrees**; the vendored engine's `horizon()`/`defineStar()` take **hours** (`ra * 15.0`) |
| Observer sign convention | latitude N+/S−, longitude **E+/W−**, height in metres |
| FK6 | CC0, **4,151** rows, V magnitude −0.62 … 9.71 — and it **lacks Sirius, Arcturus, Capella and Betelgeuse**, so a supplement is mandatory |
| The LUT | two tables, **61 solar altitudes (+10° → −20°, step 0.5°) × 91 elevations (0° → 90°, step 1°)**, 3 channels, **float16**, storing **linear-light radiance relative to zenith**, ≈ 67 KB for both |
| The frame budget | **≤ 12 ms per frame** on the A40's Mali-G71 MP2 — an inferred target, and "it is smooth on the A40" stays **UNVERIFIED** until `dumpsys gfxinfo` measures it |

## 6. Where to read what

Read only what your brief names, plus this file. Everything is under
`.kiln/scratch/spec-2026-10-08/` or `docs/`.

| Report | Carries |
|---|---|
| `K-competitor-features.md` | the competitor matrix, the adopt list, the rejected list, UI/UX patterns worth copying |
| `L-depth-retention-edges.md` | the adopted depth features, the **111 edge cases (E01–E111)**, the two settled licence questions |
| `M-play-integrations-compliance.md` | the Play In-App Review contract, permissions, Data safety, attribution strings, the revenue-lever ranking |
| `N-data-algorithms.md` | the object-table schema, time and geometry definitions, satellite rules, the forecast contract, optics, sky brightness |
| `O-sky-scene-rendering.md` | the 13-layer stack, the LUT, star formulas, the Milky Way transform, the horizon and alpenglow, the scrub model, **44 acceptance items**, the risk register |
| `docs/ideation/plans/lumintra-light-planner.differentiators.json` | the binding promise contract (7 existing claims) |
| `docs/ideation/plans/lumintra-light-planner.economics.json` | the money plan |
| `docs/ideation/plans/lumintra-light-planner.naming.json` | name, slug, applicationId, titles |
| `docs/research/2026-10-08-light-and-sky-session-planner-market.md` | the durable market record the contract cites |
| `openspec/changes/app-laneset/` | **the accepted precedent** for the format of `proposal.md`, `design.md`, `tasks.md` and a `spec.md`. Read it to match the voice and the structure |
| `.cursor/rules/*.mdc` | the binding factory rules. At minimum `grounded-claims.mdc`, `app-depth.mdc`, `visual-identity.mdc`, `device-testing.mdc`, `authorization.mdc` |

## 7. The honesty contract, and what your report must end with

`.cursor/rules/grounded-claims.mdc` binds you. Before a factual sentence leaves you, name what you
read or ran that makes it true. Label every claim:

- **MEASURED** — you read the file or ran the command **in this session**; give the `path:line` or the
  command and its output.
- **ON RECORD** — a named source states it and you read that source; give the source and the number.
- **INFERRED** — it follows from other labelled facts; give the facts and the step.
- **UNVERIFIED** — you did not check it; give the exact check that would settle it.

`UNVERIFIED` is a **result**, not a failure. An invented constant is a defect. End every report with a
claim ledger:

```text
CLAIM LABEL EVIDENCE
the LUT is two 61x91 float16 tables   MEASURED  O-sky-scene-rendering.md §2
the review flow reports nothing back  ON RECORD  Google's integration page, quoted in M-play-integrations-compliance.md
```

## 8. What "complete" means for your document

A different chat, with no access to this conversation, must be able to implement your area from your
document alone, without asking a question. That means:

- every screen you describe has its **states** named (loading, empty, error, offline, no-darkness,
  no-permission) and what each shows;
- every number has a value with a unit;
- every user-facing string is described as **a resource that must exist in both `en` and `ru`**
  (`values/strings.xml` and `values-ru/strings.xml`) — never a literal in code;
- every interactive control is named, has a role for TalkBack, and is at least **48 dp**;
- every refusal is written down as a refusal, with the sentence the user sees;
- every screen reads the window size class and works in portrait and landscape;
- and everything that cannot be proved is labelled as such rather than implied.

# Lumintra — feature and contract analysis (2026-10-08)

> **Status, 2026-10-08:** the Orekit pin in this record is now **`org.orekit:orekit:13.1.9`** (latest
> stable; `13.1.5` was the smoke's vendored build). The open question "does Orekit ship an OMM parser
> yielding a propagator?" is **resolved — yes**, measured by `javap -public` over `orekit-13.1.9.jar`
> (`OmmParser` → `Omm`, with `Omm.generateTLE()` and `generateKeplerianOrbit()`). Where this record
> cites a reading taken *from* the 13.1.5 javadoc or *on* the 13.1.5 jars, that attribution is
> deliberate and correct — it records when the reading was taken, not what the slot declares.

**What this document is.** The **durable** consolidation of five specification workers' findings for
the slot `lumintra-light-planner` (`com.gexetr.lumintralightplanner`), a $9.99 one-time
`purchase_only_no_ads` Android light-and-sky session planner. It is the **cite target**: the binding
promise contract (`docs/ideation/plans/lumintra-light-planner.differentiators.json`) and the
capability specifications under `openspec/changes/app-lumintra-light-planner/specs/` cite this file by
`#`-anchor rather than the `.kiln/scratch/spec-2026-10-08/` originals, which are **disposable** — a
document that cites a scratch file is citing a temporary file.

**Sources consolidated** (all authored 2026-10-08, all read in this session):

| File | Carries |
|---|---|
| `.kiln/scratch/spec-2026-10-08/K-competitor-features.md` | the competitor matrix, the adopt list, the rejected list, UI/UX patterns |
| `.kiln/scratch/spec-2026-10-08/L-depth-retention-edges.md` | the adopted depth features, the **111 edge cases (E01–E111)**, the settled licence questions |
| `.kiln/scratch/spec-2026-10-08/M-play-integrations-compliance.md` | the Play In-App Review contract, permissions, Data safety, attribution strings, revenue levers |
| `.kiln/scratch/spec-2026-10-08/N-data-algorithms.md` | the object-table schema, time and geometry, satellite rules, the forecast contract, optics, sky brightness |
| `.kiln/scratch/spec-2026-10-08/O-sky-scene-rendering.md` | the 13-layer stack, the LUT, star formulas, the Milky Way transform, the horizon and alpenglow, the scrub model, **44 acceptance items**, the risk register |

Also read in this session as binding vocabulary/rule: `.kiln/scratch/spec-2026-10-08/CONTEXT.md` and
`.cursor/rules/grounded-claims.mdc`.

**Label discipline.** Every factual sentence carries a label per `.cursor/rules/grounded-claims.mdc`:
`MEASURED` (a command was run or a file read **in this session**, and the `path:line` or command is
named), `ON RECORD` (a named source states it and that source was read), `INFERRED` (it follows from
other labelled facts, and the step is shown), `UNVERIFIED` (it was not checked, and the exact check
that would settle it is named). **No number here is invented**: every value is reproduced from one of
the five reports (which themselves measured or quoted it) and its origin is named. **Every
`UNVERIFIED` the source reports carried is preserved** — a consolidation that quietly upgrades an
`UNVERIFIED` row to a fact is the exact defect this document exists to prevent.

**Consolidation findings this consolidation adds** (a synthesis the individual reports did not make;
each is labelled and each names the check that settles it):

- **C-1 — Horizon sample count conflicts (360 vs 720).** `O-sky-scene-rendering.md` §5.1 stores
  **360 samples** (a `ByteArray(720)` = 360 × `int16` at 0.01°), and `CONTEXT.md` §3.4 calls
  `HorizonProfile` *"the 360-sample array"*. `L-depth-retention-edges.md` E72 stores **720 samples at
  0.5° azimuth** (~1.4 kB). The two cannot both be the contract. **Canonical value: 360** (CONTEXT is
  binding vocabulary; O is the rendering owner). **Check that settles it:** the author of the
  `scout-spots`/`night-scene` spec must pick one and the `HorizonProfile` schema test must assert the
  array length; review at checkpoint 1.
- **C-2 — Satellite staleness thresholds conflict (L E36 vs N §3.6).** `L` E36 pins **≤ 24 h** normal,
  **24–72 h** badged, **> 72 h refuse to give times**. `N` §3.6 pins **≤ 24 h** normal, **24–72 h**
  normal (no badge), **72–168 h (7 d)** badged, **> 168 h** refuse. Both are stated as product
  choices, not measurements. **Check that settles it:** the owner's decision at checkpoint 1, and the
  `satellite-passes` spec must state one table; the real error-versus-age curve (L §3.5 / N §8 item 2)
  would replace the guess with a measurement.
- **C-3 — The CelesTrak "HTTP 500" in `K` is superseded.** `K` §2 A12 recorded CelesTrak's usage
  policy as `UNVERIFIED` because *"its policy page returned HTTP 500"*. `L` §0.1 and `N` §0 both
  **MEASURED** the page at `https://celestrak.org/usage-policy.php` as **HTTP 200, 13,980 bytes**
  (the 500 was the wrong path `/usage-policy/`, a 404 page). The page is reachable; **the
  commercial-use question it did not answer stays `UNVERIFIED`** (§7 open items).
- **C-4 — The 44 rendering acceptance items and the 111 edge cases overlap but neither contains the
  other.** The rendering contract's 44 items are falsifiable *pixel* measurements; the edge register's
  111 cases are *behavioural* situations. A `night-scene` spec needs both, cross-referenced (§8 cites
  the 44; §4 cites the 111).

---

## 1. The competitor feature matrix

**Method of the source** (`K` §1): every Play listing below was opened with `WebFetch` on
`play.google.com/store/apps/details?id=<pkg>` in that worker's session; prices, install buckets and
rating counts come from the durable shelf record
`docs/research/2026-10-08-light-and-sky-session-planner-market.md` (itself measured by
`scripts/enrich-market.mjs`) where the listing did not render them. Nothing was copied from an
incumbent's code or assets; only listing text was read.

### 1.1 The light-planning shelf (the shelf that carries the money)

`MEASURED` — listing reads in `K` §1.1 unless a cell says otherwise.

| App | package | Price (one-time) | Installs / rating | The relevant features |
|---|---|---:|---|---|
| **PhotoPills** | `com.photopills.android.photopills` | **$10.99** | 100K+ · 4.8 (8.22K) | 2D map-centric Sun/Moon/**Milky Way** planner; sun/moon alignment "fast finder"; 3D AR overlay (incl. DoF/FoV); photo-plans manager; location-scouting POI database; sunrise/set, twilights, golden/blue hour, moon phases; **calculators: long exposure, timelapse, spot stars, star trails, hyperfocal, DoF, FoV**; widgets; awards/community |
| **TPE (The Photographer's Ephemeris)** | `com.crookneckconsulting.tpeandroid` | **retired** — no longer sold | n/a | Android app available 2011–2021, **withdrawn from sale and officially retired**; 3D Milky Way band / terrain on other platforms |
| **Sun Surveyor** | `com.ratana.sunsurveyor` | **$9.99** | 100K+ · 4.8 (5.14K) | Live camera AR of sun/moon paths; 3D compass; **Milky Way & star-trail features**; **"Photo Opportunities"** critical-times list incl. *"total darkness for stargazing"*; **"Position Search"** for Sun/Moon/**Milky-Way-centre**; visual time machine; sun shadow length; **offline usage**; `.kmz`/`.kml` import/export; **9 widget variants** |
| **Planit Pro** | `com.yingwen.photographertoolspro` | **$9.99** | 10K+ · 4.6 (1.22K) | Simulated viewfinder; ephemeris; sun/moon finder; **nebulae azimuth & elevation**; **star-trail planning**; timelapse/sequence simulation; Milky Way searching; eclipse; **exposure/ND calculator**; light meter; tide; offline maps |
| **Sunset & Sunrise – Sun Tracker (PhotoTime)** | `pl.guteklabs.phototime` | free / IAP | 500K+ · 4.3 (9.25K) | Sun/moon path map; AR; scout & save locations; twilight; moon calendar; widgets; **smart notifications**; **weather**; DoF/FoV calculators; long exposure/timelapse/star-trail planning; **"Milky Way visibility"** |
| **Photographer's companion Pro** | `com.stefsoftware.android.photographerscompanionpro` | ~$3.49 (**UNVERIFIED** — not rendered on listing) | 4.9 (633) | 40+ tools; AR Sun/Moon/Milky Way; **star-trail simulator**; **night mode**; EV/DoF/reciprocity failure; import/export |
| **Photographer's Tools Pro** | `com.zenapps` | ~€2.39 (**UNVERIFIED**) | not read | Hyperfocal/DoF; civil/nautical twilight + blue & golden hour; ND exposure |
| **Sun Position PRO** | `com.andymstone.sunposition` | $9.99 | 50K+ · 4.9 (2.73K) | Sun/moon position; *"when and where the Milky Way core will be visible"* |
| **Sun Seeker** | `com.ajnaware.sunseeker` | $8.99 | 50K+ · 4.8 (1.0K) | AR sun path, shadow, golden hour |
| **SunOnTrack** | `com.sunnytapps.sunnytrack` | $7.99 | 50K+ · 4.8 (874) | Sun path & shadows; *"dark theme for night hours"* — its entire night treatment |
| **Golden Hour+** | `com.simplaapliko.goldenhourpro` | $4.99 | 1K+ · 4.7 (189) | Golden/blue hour; civil/nautical/astronomical twilight durations |
| **Mobile Observatory Pro** | `com.zima.mobileobservatorypro` | $11.99 | 10K+ · 4.6 (5.68K) | Astronomy observing planner (adjacent shelf) |

**The shelf's price band is the money fact.** `MEASURED` (`K` §1.1 / `M` §5.0): the paid anchors are
**PhotoPills $10.99 / 100K+ / 4.8 (8,220)**, **Sun Surveyor $9.99 / 100K+ / 4.8 (5,140)** and
**Sun Position PRO $9.99 / 50K+ / 4.9 (2,730)** — standalone purchase-only listings, zero ads. $9.99
is the shelf's **modal** price and $10.99 its ceiling; the $17.99 on the shelf is an in-funnel Pro
unlock of a free 5M+ app, not a standalone price (`M` §5.1).

### 1.2 The stargazing shelf (free giants own the head terms)

`MEASURED` — `K` §1.2.

| App | package | Price | Installs / rating | The relevant features |
|---|---|---:|---|---|
| **Sky Map** | `com.google.android.stardroid` | free, no ads/IAP | **50M+** · 4.1 (482K) | Point-and-identify; widgets (Moon phase, **"Tonight's sky"**, countdown); alt/az grid; optional-per-feature permissions |
| **Stellarium (Mobile)** | `com.noctuasoftware.stellarium_free` | free / IAP (Plus $19.99) | **10M+** · 4.8 (337K) | Realistic atmosphere/sunrise/sunset + refraction; realistic Milky Way; twinkling; satellite tracking incl. ISS; red night mode; Plus adds observability/transit tools + offline data |
| **Star Walk 2 Plus** | `com.vitotechnology.StarWalk2Free` | free / ads+IAP (Pro $2.99) | **10M+** · 4.5 (572K) | **"Sky Live" and "Visible Tonight"**; adjustable time scale; night-mode interface; AR; infographics |
| **Sky Tonight** | `com.vitotechnology.sky.tonight.map.star.walk` | free / IAP | **5M+** · 4.7 (85.7K) | **"What celestial events can I see tonight?"**; **"Visible Tonight"**; Astronomy Calendar; **"Stargazing Index"**; one-handed bottom navigation; built-in image editor for shareable sky images; night mode |
| **SkySafari 8 Pro / Plus** | `…skysafari8pro` / `…skysafari8plus` | $34.99 / $29.99 | 1K+ · 4.2 (211) | Deep-sky database; Events Finder; **Observing Conditions** hourly forecast + best viewing windows + seeing + sky quality; Night Vision Mode; rotatable FoV indicator; plate solving; cloud sync (LiveSky); telescope control |
| **Star Chart** | `com.escapistgames.starchart` | free / IAP | **10M+** · 4.3 (174K) | 20M stars; Messier; guided tours; time shift ±10,000 years; voice; eclipse/meteor notifications |
| **Sky Guide** | (none) | iOS-only | — | **Not on Android** — its premium cues (cinematic time controls, astrophotography backdrop, 120 FPS) are patterns, not a competitor |

**The demand reading that selects the target.** `MEASURED` (`K` §1.2 / market record §2.1): every
10M+ stargazing app and the 50M+ Sky Map are **LIVE point-and-identify** apps; the largest pure
**PLANNING** app is **100K+** — a factor of ≈ **500×** smaller. `INFERRED`: the head terms are owned by
free LIVE giants, so a paid planner does not compete for them; it competes on the **planning** job
(`K` Risk 2). The stargazing shelf's premium tier is also small and free-adjacent ($2.99 Star Walk 2
Pro, $6.99 SkySafari Astronomy), so **the money is on the light-planning shelf**, not here.

### 1.3 The satellite shelf — and the headline measured finding

`MEASURED` — `K` §1.3.

| App | package | Price | Installs / rating | The relevant features |
|---|---|---:|---|---|
| **ISS Detector (free)** | `com.runar.issdetector` | free / ads+IAP | **5M+** · 4.8 (121K) | Pass list; **weather conditions for a sighting**; alarms; share sightings |
| **ISS Detector Pro** | `com.runar.issdetector.pro` | **$17.99** | 100K+ · 4.4 (3.19K) | Radio satellites + Doppler; Famous Objects; comets/planets; Iridium flares; pass list + alarm; weather check. **The highest observed paid price in the recon** |
| **Satellite Tracker by Star Walk** | `com.vitotechnology.SatelliteTracker` | free / ads+IAP | 1M+ · 4.4 (61.4K) | Pass times and sky paths |
| **Look4Sat** | `com.rtbishop.look4sat` | free | 100K+ · 3.8 (1.01K) | Amateur-radio pass prediction |
| **NASA Spot the Station** | — | free | 500K+ · 4.7 (7.12K) | ISS sighting alerts |
| **AstroSat** (research tool, *not a shipped app*) | — | free/OSS (GitHub, 22 stars) | — | **Forecasts satellite transits through a photographer's exact field of view** using location, target, optics, camera, **exposure length** and session time; names *"landscape and astro-photographers"* as users |
| **Tonight's Window** (web tool) | `orbitalnodes.ai/satellite-trails` | free web | — | Finds *"the cleanest window to image tonight … the shadow window"* |
| **PSF Guard** (pipeline tool) | `psf-guard.com` | not read | — | Predicts trails and matches them against FITS pixels; abstains rather than reporting a frame clean |

**Headline measured finding — the wedge.** `K` §1.3: satellite-**pass** apps are a big, free-ad shelf,
but satellite-**streak forecasting against a long exposure** exists only as research and web tools
(AstroSat, Tonight's Window, PSF Guard) and **no shipped Android paid app bundles it into a
light-planner**. `K` labels the inference `INFERRED` and names the settling check: *"a Play search for
`satellite trail planner`"*. This is the **most defensible differentiator in the plan** (`K` §2 A12):
AstroSat's paper explicitly names the exact buyer, and the only Android apps near the job sell *passes*,
not *streaks against the frame*.

**The trust caveat the wedge carries** (`K` §5, secondary risk): a *wrong* streak prediction is worse
than no prediction, and the pass-detector trap (§7.3) is a measured, reproduced defect in the engine
path. A trust-eroding false "clean" is a review risk, not only a bug.

### 1.4 Adjacent — anything else a night photographer pays for

`MEASURED` — `K` §1.4.

| App | package | Price | Installs / rating | The relevant features |
|---|---|---:|---|---|
| **Astrospheric** | `com.astrospheric.dfior.astrospheric` | free / Pro subscription | 100K+ · 4.5 (804) | 84-h hourly forecast; ensemble cloud; transparency + seeing; **smoke**; **Kp aurora**; ISS flyover paths; Pro weather alerts; offline Site Mode |
| **Clear Outside** | `com.firstlightoptics.clearoutside` | free | 100K+ · **3.8** (1.37K) — the shelf's weakest rating | 7-day hourly; **low/medium/high and TOTAL cloud**; moonrise/set/phase; civil/nautical/astronomical darkness; **ISS pass-over beside the cloud forecast**; favourite locations |
| **Light Pollution Map – Dark Sky** | `com.pa.lightpollutionmap` | free / Pro subscription | **500K+** · 4.7 (7.33K) | Light-pollution overlay + **Bortle** estimates; "Horizon Safe Radius"; live sky conditions; **"Darkness Timer — … live countdown to true night"**; aurora Kp; Moon Position Tool; ISS tracker; night-sky calendar; calculators; **night & AMOLED themes** |
| **Good To Stargaze** | `com.goodtostargaze` | free / **subscription** ($0.99–$49.99/yr) | 50K+ · **3.9** (553) | Weather + seeing + transparency + light pollution; dark-sky map. A pinned review rejects the recurring price as *"data that can also be found elsewhere"* |
| **My Aurora Forecast Pro** | `com.jrustonapps.myauroraforecastpro` | **$3.99** | 50K+ · 4.78 (3.8K) | Kp index; forecasts; push alerts; solar wind; webcams |
| **Lunescope Pro** | `com.daylightmap.moon.pro.android` | **$3.99** | 100K+ · 4.1 | Interactive 3D moon (craters, **earthshine**); phase/angle/libration/rise-set; alerts |
| **My Moon Phase Pro** | `com.jrustonapps.mymoonphasepro` | **$2.99** | 4.8 (2.41K) | Moon calendar; **cloud cover**; golden/blue hour; widgets |
| **Daff Moon Phase** | `com.dafftin.android.moon_phase` | free | 4.9 (55.3K) | Moon/Sun/planet rise-set, alt/az; eclipses; golden/blue hours; widgets; custom notifications |
| **Light for Night Vision** (Vixen) | `jp.co.vixen.nightvisionlight` | free | not read | Turns the screen into an adjustable **red flashlight** for dark-adapted eyes |
| **Nightcam** | `com.rayinformatics.nightcam` | free/IAP | not read | A **capture** app — a different job (capture, not planning) |

**The presentation gap.** `MEASURED` (`K` §1.4 / market record §3(d)): **cloud cover is absent from
the light-planning shelf's own descriptions** (a keyword scan of 10 descriptions), while the dedicated
astronomy-weather apps that *do* show it carry the shelf's **weakest ratings** (Clear Outside 3.8, Good
To Stargaze 3.9). `INFERRED`: the wedge on cloud is *presenting it well beside the darkness window*,
and Clear Outside's own pattern — the **ISS pass shown beside the cloud table** — is the one to copy.

### 1.5 What a night photographer pays for — the synthesis

`INFERRED` from §1.1–§1.4 (all `MEASURED` rows above), the step being: the paid listing descriptions
name the same capabilities repeatedly and those are the ones a buyer checks for.

1. **The Milky Way** — bundled into **every** paid light-planner (Sun Surveyor's "Milky Way centre
   Position Search", PhotoPills' "Sun, Moon, Milky Way", Sun Position's "where the Milky Way core will
   be visible", PhotoTime's "Milky Way visibility"). It is *the* subject of a night shoot.
2. **Star trails / arcs** — a headline bullet in Sun Surveyor, PhotoPills, Planit Pro, PhotoTime and
   Photographer's companion.
3. **A critical-times table** (twilight levels, golden/blue hour) — table-stakes; its absence is the
   first review complaint.
4. **A "visible tonight / twilight table" answer** — the exact question the incumbents headline
   (Star Walk 2 "Visible Tonight", Sky Tonight "what can I see tonight", Sky Map's widget, SkySafari
   "best viewing windows"). A pinned Sky Tonight review says the buyer *"would have paid extra just to
   have everything all in one app"* (`MEASURED`) — the willingness-to-pay for **one combined answer**.
5. **Exposure / FoV / star-trail calculators** — in every paid incumbent; a bullet the buyer checks for.
6. **A red night mode** — a named selling point of every premium astronomy app.
7. **Widgets and reminders** — the cheap re-open surfaces (Sun Surveyor advertises 9 widgets).
8. **Data that travels** — the **measured** top complaint of the paid incumbents: Planit Pro's *"the 29
   plans that I have on my cellphone can't be easily transferred to a new tablet"* and PhotoPills' *"no
   way … to not have to pay 9.99 twice when switching devices"* (`MEASURED`).

**And the two things the shelf does *not* have, which are Lumintra's openings:** a **satellite-streak
flag against the frame** (§1.3), and **cloud presented honestly inside the plan** rather than on a
separate weather page (§1.4).

### 1.6 The three risks this matrix exposes (`K` §5)

- **Risk 1 — the willingness to pay for a *combined* brief is `UNVERIFIED`.** No incumbent sells
  dark-window + deep-sky + satellite + cloud in one flow (market record §3.2, §10 item 4). The
  evidence *for* it is indirect (every paid light-planner bundles night content; a pinned Sky Tonight
  review). Its settling check is Play's full paid-planner review lists and, ultimately, the launch
  price. **The conclusion is only as strong as this weakest row.**
- **Risk 2 — a beautiful scene is not a planner.** `INFERRED` (`K` §5): the best-designed sky apps are
  free or ≤ $2.99, and the LIVE identification shelf dwarfs planning ≈ 500×. The apps that charge
  $9.99–$10.99 win on **planner legibility** (alignment search, critical-times list, exposure
  calculator, pass list), not on a rendered sky. *"If Lumintra's scene does not terminate in an
  actionable Session brief and a concrete 'go at HH:MM' answer, the $9.99 buyer sees a pretty toy."*
  This is why the adopted additions of §2 put the **Tonight summary, the twilight table and the
  exposure panel** ahead of any further visual polish.
- **Risk 3 — licence and feed fragility can block the product late.** OpenNGC (CC BY-SA) killed the
  natural deep-sky catalogue and forced a hand-compiled list; the Gaia Milky Way panorama is CC BY-SA;
  Gaia/HYG are non-commercial; `api.met.no` is the only usable free cloud feed and it has **no SLA**,
  giving total cloud only; CelesTrak's commercial-use policy is **`UNVERIFIED`** (§7 open items). The
  mirror image is the opportunity: **TPE retired its Android app in 2021** (`MEASURED`), on the exact
  shelf Lumintra targets.

### 1.7 UI/UX patterns worth copying (`K` §4 — patterns, never code or assets)

- **P1 — one-handed bottom navigation + minimised panels** (Sky Tonight states the rationale
  explicitly). A six-screen app with a thumb-reachable primary action beats a top-heavy layout at night.
- **P2 — time as a scrubber with a "time machine" feel**; copy the *continuous motion of every object*
  and a **stage name as a label, never a keyframe**.
- **P3 — open Targets on a ranked, filtered list for the chosen night**, not a catalogue browser.
- **P4 — a critical-times list as inspiration** ("Photo Opportunities"), adapted into tap-to-plan rows.
- **P5 — alignment search** ("when will the Moon be *there*") — the planner-shaped version of the scrub.
- **P6 — condition colour-coding on an hour grid** aligned to the darkness window (Clear Outside).
- **P7 — a live "darkness countdown."**
- **P8 — onboarding that assumes no astronomy knowledge**, and that explicitly says the app does
  **not** use the compass (measured complaints: Star Chart's *"There is no tutorial for this"*;
  Stellarium's unexplained zoom/coordinate confusion).
- **P9 — a shareable composed image** of the *scene* (not a data dump) — the strongest organic
  acquisition surface.
- **P10 — offline-first**: Sun Surveyor, Planit Pro, Star Walk 2 and Astrospheric all advertise it;
  weather is the only networked piece.

---

## 2. The adopted additions

This section is the **cite target** for the six new promise-contract claims and for the approved
additions that fold into existing claims. Each row gives: **what it is**, **the revenue reason**,
**which capability it belongs to**, and **its source** (the report and section that measured or derived
it).

### 2.1 The owner's approved additions (`CONTEXT.md` §2)

The owner explicitly approved these; a document that re-argues one is wasted work.

| # | Approved addition | Claim / capability that carries it | Source |
|---|---|---|---|
| OA-1 | **Milky Way core and band on the scene** | `render-the-night-scene` → capability `night-scene` | CONTEXT.md §2; K §2 A1; N §1; O §4 |
| OA-2 | **Star-trail mode on the timeline** | `render-the-night-scene` → capability `night-scene` | CONTEXT.md §2; K §2 A2; O §6–§7 |
| OA-3 | **A "Tonight" summary that needs no configuration** | `compose-the-light-and-dark-brief` (existing core) → capability `session-brief` | CONTEXT.md §2; K §2 A3; L §1.3 C |
| OA-4 | **A twilight / golden / blue-hour table** | `compose-the-light-and-dark-brief` (existing core) → capability `session-brief` | CONTEXT.md §2; K §2 A4; N §2.2 |
| OA-5 | **A true red night mode** | `settings-and-credits` → capability `settings-and-credits` | CONTEXT.md §2; K §2 A5; L §1.1 |
| OA-6 | **A home-screen widget** | `glance-at-tonight` → capability `night-glance` | CONTEXT.md §2; K §2 A10; L §1.3 A |
| OA-7 | **One opt-in local reminder** | `get-myself-outside` → capability `night-glance` | CONTEXT.md §2; K §2 A8; L §1.3 B |
| OA-8 | **Export and import of the user's spots and horizons** | `carry-your-scout` → capability `scout-spots` | CONTEXT.md §2; K §2 A6; L §1.1 A1–A2 |
| OA-9 | **Rating: a "Rate us" row + the automatic Play In-App Review prompt at an earned moment** | `settings-and-credits` → capability `settings-and-credits` | CONTEXT.md §2 and §5; M §1 |

### 2.2 The six new promise-contract claims

Names, weights and stages are **verbatim from `CONTEXT.md` §3.9** (binding vocabulary). The
**capability assignment** and the **rejected alternative** in each row are this consolidation's
judgement and are labelled `INFERRED`.

**`render-the-night-scene` — `core`, `during` · capability `night-scene`.**
- **What it is:** the hero surface — a scrubbable layered-2-D night scene (twilight LUT sky base, the
  Milky Way band and core, the star field, the Earth-shadow wedge and Belt of Venus, warm alpenglow
  over the user's drawn horizon) that the user drags through the night.
- **Revenue reason:** the Milky Way is the subject that makes this buyer pay (`K` §2 A1 — it is
  bundled into every paid light-planner; `K` §1.5.1), and the factory's differentiator is design, not
  code (`.cursor/rules/visual-identity.mdc`). The owner's standard: it must not look like a form with a
  button.
- **Capability:** `night-scene`. *Rejected alternative:* `session-brief` — because the brief is the
  scrolled text answers and the scene is the hero pixels, and CONTEXT.md §3.3 names them separate
  screens (`NightSceneScreen.kt`, `BriefScreen.kt`). (`INFERRED`.)
- **Source:** `O` §1–§8 (the whole rendering contract) and §9 (44 acceptance items); `K` §2 A1/A2;
  `N` §4 (the Milky Way placement math); `L` E82–E96 (rendering/accessibility edge cases).

**`draw-the-horizon-line` — `supporting`, `before` · capability `scout-spots`.**
- **What it is:** the drawn horizon silhouette — the user drags a ridge in the horizon editor inside
  `SpotEdit`, stored as a quantised azimuth→elevation array, and the same array is the scene's
  foreground and the maths' elevation mask.
- **Revenue reason:** the drawn horizon is the one irreplaceable user artifact, it is what makes the
  scene *"their own"* place, and it is the honest substitute for a light-pollution raster the app
  cannot ship (`K` §2 A11; `N` §6.3). It also serves the `before` stage the depth contract requires.
- **Capability:** `scout-spots` (the horizon editor lives *inside* `SpotEdit`; CONTEXT.md §3.3). It is
  *rendered* by `night-scene` but *authored and stored* by `scout-spots`. *Rejected alternative:*
  `night-scene` — because the editor is a spot-editing act, not a scene act. (`INFERRED`.)
- **Source:** `L` E12, E72–E81; `O` §5.1–§5.2 and §5.4; `N` §2.5 (the mask).
- **Open item it carries:** the **360 vs 720 sample conflict** (C-1, §4/§7).

**`see-your-lens-frame` — `supporting`, `during` · capability `night-scene`.**
- **What it is:** the lens field-of-view overlay on the scene — a calibrated frame for the chosen lens,
  the target-fit comparison, and the exposure / star-trail-length helper that makes the scene
  actionable.
- **Revenue reason:** exposure and FoV calculators are in **every** paid incumbent and are a bullet the
  buyer checks for (`K` §2 A9); a frame overlay is what turns a picture into a plan (`K` Risk 2).
- **Capability:** `night-scene` (the overlay is drawn in the scene; CONTEXT.md §3.3). *Rejected
  alternative:* `session-brief` — the FoV is a scene geometry, but the exposure *panel* words do appear
  in the brief. (`INFERRED`; the spec may split the panel into `session-brief` without changing the
  claim.)
- **Source:** `N` §5 (the FOV math, the preset table, `CameraCharacteristics` types, the equidistant
  projection rule); `K` §2 A9; `O` §1.1 L11 / §9 item 26.

**`carry-your-scout` — `supporting`, `after` · capability `scout-spots`.**
- **What it is:** export the whole plan store (spots, horizon profiles, equipment, favourites) to a
  file the user owns, share it as an attachment, and import it back — plus Android Auto Backup.
- **Revenue reason:** export/backup/portability is the **#1 ranked extra in both independent datasets**
  (661 and 743 mentions) and the **paid product in four commercial apps** (`L` §1.1 A1, ON RECORD), and
  "data that does not travel" is the measured top complaint of the paid incumbents (`K` §1.4). It also
  gives the app its **`data_in` mechanism**, which the current contract's `pin-the-session-scout`
  promise ("survives a reinstall or a move to another phone") asserts with **no mechanism today**
  (`L` §1.5, MEASURED on the contract file).
- **Capability:** `scout-spots` (the data layer is spots + horizons + equipment; CONTEXT.md §3.5,
  `SpotTransfer.kt`). *Rejected alternative:* `share-brief` — because the brief share is a composed
  image/text and this is a data file. (`INFERRED`.)
- **Source:** `L` §1.1 A1/A2/A3/A10, §1.3 D, E100–E106; `K` §2 A6; `N` §3 (the format is the app's own).

**`glance-at-tonight` — `supporting`, `before` · capability `night-glance`.**
- **What it is:** a home-screen widget (4×2 plus 2×2) showing one spot's Tonight summary — dark-window
  bounds, Moon illuminated fraction, cloud (as last cached, with its retrieval time).
- **Revenue reason:** a session planner is used **episodically**; the widget is the only surface that
  puts the product in front of the user *between* sessions, and a widget is a measured reason an app
  survives on the home screen (`L` §1.3 A, ON RECORD — *"Literally the longest I have kept a widget on
  my Main phone screen"*). Widgets are a headline feature of every paid light-planner (`K` §2 A10).
- **Capability:** `night-glance` (CONTEXT.md §3.8 names the capability; the widget is the "tonight at a
  distance" surface). *Rejected alternative:* `session-brief` — the widget is a **separate Android
  surface** with its own lifecycle, not a brief screen. (`INFERRED`.)
- **Source:** `L` §1.3 A, A4, E02; `K` §2 A10; `M` §2.1.

**`get-myself-outside` — `supporting`, `before` · capability `night-glance`.**
- **What it is:** one opt-in **local** notification at the start of tonight's dark window — created
  only by an explicit tap, default off, scheduled with an **inexact** alarm, re-derived on every app
  open, no network at fire time.
- **Revenue reason:** reminders are the **#4 ranked extra** (364 requests) and **6.12 % of all feature
  requests** (Khalid 2014, ON RECORD via `L` §1.1 A5); the countdown/reminder is the cheap
  retention/re-open surface the incumbents headline (`K` §2 A8).
- **Capability:** `night-glance` — the same out-of-app glance surface as the widget. *Rejected
  alternative:* `settings-and-credits` — because the reminder is a Tonight notification, not a
  preference; the *on/off* preference does live in Settings. (`INFERRED`.)
- **Source:** `L` §1.3 B, E107–E111; `K` §2 A8; `M` §2.1 (`POST_NOTIFICATIONS`).
- **What it explicitly is not:** the rejected **background "good night" alert** (§3), because
  `api.met.no`'s terms forbid background retrieval (`L` §0.3, MEASURED).

### 2.3 Adopted additions that fold into existing claims (no new claim)

| Addition | Where it lands | Source |
|---|---|---|
| **Tonight / best-window summary** as the brief's first answer | `compose-the-light-and-dark-brief` (core); `session-brief` — **adopted as a required default, not a feature**: the brief opens pre-resolved for tonight at the most recent spot with no configuration step (`L` §1.3 C, `INFERRED`) | K §2 A3; L §1.3 C |
| **Full twilight + key-times table** (civil/nautical/astronomical, golden, blue, alpenglow), localised | `compose-the-light-and-dark-brief`; `session-brief`. Near table-stakes (`K` §2 A4) | K §2 A4; N §2.2; O §6.3 |
| **Cloud drawn against the darkness window** (a cloud ribbon on the timeline), degrading gracefully offline | `forecast-hourly-cloud-cover` (supporting); `cloud-forecast`. The wedge is *presentation*, not the data (`K` §2 A7) | K §2 A7; N §4 |
| **True red / AMOLED night mode** applied to every screen, with a one-tap toggle | `settings-and-credits`; theme `ui/theme/NightVisionPalette.kt` (CONTEXT.md §3.6) | K §2 A5; L §1.1 |
| **Favourites** — star a target, filter by favourites | inside the existing `search-the-deep-sky-catalogue` (supporting); `target-catalogue` — the `find` token is already the claim's | L §1.1 A9 |
| **Offline as a first-class state** (a state machine, not a screen) | a property of every claim; `cloud-forecast` and `satellite-passes` are the only online rows | L §1.1 A8, §1.4 |
| **Paste coordinates** (bring a point in from a message) | the `data_in` half of `carry-your-scout` via `ClipboardManager` | L §1.1 A10 |
| **The rate prompt fired after a completed session** | `settings-and-credits`; a durability/trust rule, tested in E91 | M §1; L §1.1 A11, E91 |
| **Android Auto Backup rules** for the plan store | strengthens `pin-the-session-scout`; `durability` | L §1.1 A3, E102 |

**The depth-contract effect, stated so it is the owner's decision (`L` §1.5, `INFERRED`).** The
current contract has **7 claims** and the connective floor is already met by `share-the-session-brief`
(`data_out`, `after`, `core`) and `pin-the-session-scout` (`durability`, `before`, `core`). Nothing in
§2.2 is *required* to satisfy the gate; what the six new claims add is the **finished job**. Three of
them introduce a `depthFamily` the contract has **none of today**: `carry-your-scout` is the first
`data_in` claim, `glance-at-tonight` the first `widget`, `get-myself-outside` the first `reminder`. **A
claim raises the ceiling**: every new promise must reach `shipped` with a proof before `verify.mjs`
passes, so this is a scope decision the owner makes at checkpoint 1, and it lapses any recorded
approval (`.cursor/rules/release-gate.mdc`).

---

## 3. What is rejected, and why

Every row names a **revenue reason or a binding constraint**, never a novelty reason
(`.cursor/rules/ideation-gate.mdc`). Sources: `K` §3, `L` §1.1 B-row / §1.2, `N` §6.3, `M` §1.6/§5.2.

### 3.1 The owner's named rejections

| Rejected | Reason | Source |
|---|---|---|
| **Import from a competitor's file** | The measured demand is real (287 mentions, *"the thing that stops a switch"*), but **no competitor's export format was measured**, so the interface would be invented — *"how a feature ships that imports nothing"*. Honest substitute: paste-coordinates (A10). Revisit **after ship**, not before | L §1.1 B4, §1.3 E, §3.1 |
| **Cloud sync of spots across devices** | Needs an account **and** a server, both excluded by the money plan; and the measured counter-anatomy is a sync extra that overwrote real saves and dropped a top-3 app to #13 | L §1.1 B1; K §3 R5 |
| **The background "a good night is coming" alert** (polls weather, pushes when clear + moonless) | Three independent blockers, any one sufficient: (1) **`api.met.no` forbids it** — *"Applications on mobile devices must not retrieve new data as long as the application is not in use"* (`L` §0.3, MEASURED); (2) a periodic worker on an app whose core value does not change is a battery/maintenance tax cut at ~5 % engagement; (3) a *push* is a **promise** — "it said clear and it rained" is the trust-eroding review. Substitute: the **widget**, which recomputes from cache with no permission, server, background fetch or promise | L §1.1 A6/B, §1.3 B; M §2.3 |
| **Aurora / Kp / solar wind** | A separate audience and a separate feed; the paid aurora shelf is cheap ($3.99 My Aurora Forecast Pro) and already served, so it adds cost and a dependency with no revenue reason | K §3 R4; L §1.1 B6 |
| **Live AR sky view / camera star identification** | That is a different app (a star map), it competes with 50M+-install free incumbents, it imports the **measured compass defect surface** (Sun Surveyor's most recent negative review is *"inaccurate … off by several degrees"*; Star Walk 2 / Sky Map repeat drift/calibration complaints), and it asks for `CAMERA` for a job this app does not have | K §3 R1/R2; L §1.1 B7 |
| **Bortle / light pollution / SQM raster** | The Falchi World Atlas is **CC BY-NC** and cannot ship in a paid app (`N` §6.2, MEASURED quote); the only clean family (VIIRS VNL, CC BY 4.0) is a **radiance** product, not a sky-brightness map, and the standard model is the CC BY-NC paper's; and a **free 500K+ incumbent** (`com.pa.lightpollutionmap`) already owns the number. v1 ships **no raster**; darkness is reported from **computed** quantities plus the **user's own** SQM reading or **user-selected** Bortle class labelled "user estimate" | K §3 R8; L §1.1 B5; N §6.2–§6.3 |
| **Statistics dashboard** (nights planned, targets shot, charts) | `insight` **may not satisfy the connective floor**, this job has no analytics stage, and the depth rule's own counter-evidence is a 6 % "complexity and learning curve" complaint class from feature creep | L §1.1 B2 |
| **Subscriptions** | Banned factory-wide and machine-enforced (removed from `ARCHETYPES`), and the market agrees — Good To Stargaze's pinned review rejects the recurring price as *"data that can also be found elsewhere"* | K §3 R3; L §1.1 B9; AGENTS.md |
| **Ads** | Not enabled; every paid row on the shelf declares none | M §5.2; K §3 R3 |
| **Cloud accounts / login / societies / chat** | A server needs either a subscription (banned) or a recurring cost with no revenue line; Planit Pro's sync is itself a measured complaint. The portability need is answered by **local export** instead | K §3 R5; L §1.1 B1 |
| **Telescope control, plate solving, image import/editing** | A different buyer and a different job (imaging pipelines), not night-session planning | K §3 R6 |
| **Quiz / stories / infographics / guided tours / "sponsor a star"** | They serve the *learning* audience; the buyer pays to plan a shoot, not to be taught, and copying a free giant's content surface is a near-duplicate risk | K §3 R7 |
| **Copying an incumbent's data or assets** | Licence: OpenNGC (CC BY-SA 4.0), NGC 2000.0 (non-commercial), SAC (ambiguous), Stellarium DSO (GPL) are unshippable; HYG (CC BY-SA), Hipparcos/Tycho/Gaia (CC BY-NC 3.0 IGO), BSC5 (no affirmative grant) are unshippable; the Gaia Milky Way panorama is CC BY-SA 3.0 IGO. **Use the hand-compiled list, FK6 (CC0) + supplement, and a self-baked Milky Way** | K §3 R9; N §1.4; see §6 |
| **Eclipse / tide / rainbow / black-and-white calculators** | Each is a different job or a one-off event; they pad the app without answering *"will tonight work?"* — depth is the job's handoffs, not a settings screen of switches | K §3 R10 |

### 3.2 The other rejections carried from the depth analysis (`L`)

| Rejected | Reason |
|---|---|
| **Bulk operations** (multi-select spots/targets) | There is no bulk act in this job — you plan one night at one place for one target at a time. A `bulk` claim built to have a claim is padding, which the depth rule bans by name |
| **A per-session history log** (which nights you planned) | No measured demand; the shareable brief already is the record. Named as the first candidate if a later reading shows demand |
| **In-app updates for v1** | Play already auto-updates (measured, `M` §1.6); a forced immediate-update screen fights the pre-drive job; it adds a second Play Core artifact **and a Data safety row**. `UNVERIFIED`: no measurement exists of its install-rate effect on a paid planner — check: a shipped A/B read in Play Console |
| **An in-app product / billing / paywall / `PurchaseAuthority`** | The price is a Play Console listing attribute, not an IAP; `economics.json` deliberately disables `billing` and `ads`. Named because a "rate us"-shaped Settings screen is where an implementer invents an in-app purchase |

### 3.3 The two permissions the app must *not* take (`L` §1.2)

The adjacent satellite shelf carries a **measured 1★ review for exactly this** (*"It wouldn't let me
use the app until I also allowed alarms and notifications"*, Satellite Tracker by Star Walk).

- **Never request `ACCESS_BACKGROUND_LOCATION`** — no feature needs it; the reminder fires at a locally
  computed instant and nothing observes position in the background.
- **Never request `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`** — `AGENTS.md` treats exact alarms as
  restricted; a dark-window reminder is precise to the minute at best, so an **inexact** alarm is
  correct and loses nothing measurable.
- **Never request `CAMERA`, `READ_MEDIA_IMAGES` or `ACTIVITY_RECOGNITION`** — no feature uses them.
- **`POST_NOTIFICATIONS` must never be a gate** — it exists but denial disables the reminder only.
- **No storage permission for the share/export handoff** — `ACTION_SEND` + `FileProvider` (with
  `FLAG_GRANT_READ_URI_PERMISSION`) and `ACTION_CREATE_DOCUMENT` grant a single-URI permission and need
  no storage permission (`M` §2.1, confirmed).

---

## 4. The edge-case register

**Total: 111 situations, `E01`–`E111`** (`L` Part 2, MEASURED — the file states *"Total: 111
situations (E01–E111)"*). Each has: the **exact required behaviour**, **what the user sees**, and
**what is refused** — because the alternative (a zero, a blank, a plausible default) is what becomes a
bug report. The `E##` identifiers are kept so the capability specifications can cite them.

**Per group, with counts:**

| Group | Ids | Count |
|---|---|---:|
| 2.1 Polar and high latitude | E01–E12 | **12** |
| 2.2 The Moon | E13–E21 | **9** |
| 2.3 Targets | E22–E31 | **10** |
| 2.4 Satellites | E32–E44 | **13** |
| 2.5 Weather (`api.met.no`) | E45–E55 | **11** |
| 2.6 Location | E56–E62 | **7** |
| 2.7 Time | E63–E71 | **9** |
| 2.8 The horizon profile | E72–E81 | **10** |
| 2.9 Rendering and accessibility | E82–E96 | **15** |
| 2.10 Data integrity | E97–E106 | **10** |
| 2.11 First run, force-stop, and the reminder's honest limits | E107–E111 | **5** |
| **Total** | **E01–E111** | **111** |

**The one convention that runs through the whole register** (`L` §2.0, MEASURED): **no event search may
be a single call over a long window.** The recon reproduced the trap for satellite passes — one
`propagate()` call over 3 days finds **1** AOS where a 60-second brute-force scan finds **15**, and
tightening `maxCheck` from 600 s to 10 s changes nothing. The same class of failure applies to
**every** event search in this app: twilight crossings, moonrise, target rise/set, and any window
longer than a few hours. **Every event search is segmented (or stepped and refined), returns a list of
crossings, and is verified against a brute-force scan at the same step in a test** — E01, E43 and E95
are the same requirement from three directions.

### 4.1 The most dangerous cases, stated in full

These are the ones where the **natural implementation is wrong and the wrongness is invisible** — the
defect ships looking correct. (The first three are `L` Part 4's named classes; the rest are the
register's own highest-cost rows.)

**E43 — the 1-vs-15 event-search trap.**
*Situation:* the pass finder searches a window with a single long propagation.
*Required behaviour:* segment the propagation into **≤ 1-hour windows** (or step-and-refine); a test
asserts the 3-day / 10°-mask case with the fixture element set returns the **15 AOS / 30 events** the
brute-force scan returns. This is *"the single most likely defect to ship looking correct"*.
*What is refused:* a single multi-day `propagate()` call.
*Also applies to:* **E01** (any requested rise/set/twilight crossing that does not occur — the engine
returns `Time?`; **null is an expected value, not an error**; a typed `None`, never `!!`) and **E10**
(a search whose window contains the event twice must return an **ordered list** of crossings).

**E02 — polar day: no astronomical darkness at all.**
*Situation:* at Reine (φ = 67.93°) the Sun's minimum altitude is **+1.37°** on the solstice, so there is
no sunset and no astronomical darkness.
*Required behaviour:* the dark-window result is a typed
`NoAstronomicalDarkness(darkestLevel, atInstant, sunAltitude)` carrying the **deepest twilight
actually reached** and the instant it occurs. The scene still renders the **true** sky through the
ordinary LUT; the ribbon greys the astronomical band; the share text, the widget and the sentence the
brief shows all reuse the **one** sentence; the reminder is **not offered**.
*What the user sees:* `No astronomical darkness at Lofoten tonight — the sky only reaches nautical
twilight (Sun −14.6° at 01:04).`
*What is refused:* a dark window of `00:00–00:00`, a black sky with stars, a "no data" screen, a
reminder for a window that does not exist.
**E07** makes this a **main path, not an exotic branch**: astronomical darkness is also absent at
**≈ 52.5 °N** for roughly two months of summer — the ordinary June case for most of Europe. **E03**
(civil twilight also absent), **E04** (midnight sun: `darkestLevel = DAY`, the brief's first line
becomes the *light* window) and **E05** (polar night: the night is the whole civil day, report the
Sun's **maximum** altitude as a positive fact) complete the family.

**E60 / E63 / E64 / E65 — the spot's timezone and the DST night.**
*Situation:* the natural implementation formats the device's local clock (wrong the moment the user
plans a trip abroad) and builds the scene's time axis out of local clock minutes (wrong twice a year
when an hour does not exist or exists twice). Both errors are invisible at home.
*Required behaviour:* an **absolute time axis**; all labelling derived from the **spot's** zone; an
explicit IANA zone per spot; a statement of the difference when the device's zone differs; DST
spring-forward steps `01:59 → 03:00` (a gap, correctly) and fall-back labels the repeated hour twice
with the offset. E60: `Times in Europe/Oslo (spot) · your device is Europe/Moscow`.
*What is refused:* times in the device's zone for a spot abroad; a `02:30` shown as a real instant; a
duplicated label with no offset; a slider that jumps backwards.

**E32 / E33 — the element format.**
*Situation:* new satellite objects are catalogued with **6-digit** numbers (100000+) and **GP data is
not available for them in TLE format** since **2026-07-11** (`L` §0.1 / `N` §3.1, MEASURED, CelesTrak
"URGENT" banner).
*Required behaviour:* parse **OMM/JSON (or CSV) from CelesTrak, not fixed-width TLE**; map the measured
OMM fields into the propagator's initial state.
*What is refused:* a TLE parser with a 5-digit assumption; a silent skip of an object whose id is 6
digits.
**E33 is now `MEASURED` — Orekit does ship an OMM parser.** `javap -public` over `orekit-13.1.9.jar`
shows the package `org.orekit.files.ccsds.ndm.odm.omm` with `OmmParser` (`public Omm build()`) and
`Omm` (`generateTLE()`, `generateKeplerianOrbit()`, `generateSpacecraftState()`), so the OMM→propagator
path is **direct**; the hand field mapping into the two fixed lines is the **fallback**, not the route.
The residual edge is E32: whether `generateTLE()` tolerates a **6-digit** `NORAD_CAT_ID` is **not
measured** and is left as a required unit test.

**E36 — stale element data.**
*Situation:* the recon measured SGP4 diverging by **27,924 km after ≈ 3 weeks** of extrapolation.
*Required behaviour:* age is always computed from the element set's own `EPOCH` and always
**displayed** beside the prediction.
*What is refused:* silently predicting from a week-old cache on an app that advertises offline use.
**Conflict C-2 (§0):** `L` E36 pins **> 72 h → no pass times at all**; `N` §3.6 pins **72–168 h →
badged, > 168 h → refuse**. Both are product choices; the spec must state **one** table and the owner
decides at checkpoint 1.

**E45 / E46 — the coordinate that leaves the device.**
*Situation:* fetching a forecast transmits the spot's coordinates off the device.
*Required behaviour:* truncate lat/lon to **4 decimals before building the URL** (5+ returns HTTP 403,
the source's own rule); declare **Location → Approximate** in Data safety and name **MET Norway as a
recipient** in the privacy policy.
*What is refused:* six-decimal "GPS precision"; sending coordinates for any spot the user is not
looking at; a "no data collected" declaration while coordinates are in the query string.

**E53 — never fetch in the background.**
*Situation:* a background worker fetching weather.
*Required behaviour:* cache on disk, send `If-Modified-Since`, honour `Expires`, never `HEAD` then
`GET`, and **never fetch while the app is not in use** (`api.met.no` ToS, MEASURED).
*What is refused:* a background worker fetching weather. (This is also why the good-night alert of §3
is rejected.)

**E85 — below the shader floor (the A40's own path).**
*Situation:* the twilight gradient on API 26–32 (the A40 is API 30).
*Required behaviour:* a **baked LUT / `LinearGradient`** path for API 26–32; AGSL `RuntimeShader` on
API 33+ **only**; the two paths produce the same sky within a stated tolerance; the selection is by
`Build.VERSION.SDK_INT`; a shader compile failure falls back to the LUT **for the session** rather than
re-throwing. The A40 exercises the LUT path; the A16 is the shader-path check.
*What is refused:* two different skies; a probe that can throw; a crash on a device that cannot compile
the shader.

**E88 — RTL: the scene is not mirrored.**
*Situation:* the scene's horizontal axis is an **azimuth** axis.
*Required behaviour:* the sky is **not** mirrored by layout direction — mirroring would put the user's
west ridge on the wrong side and make the picture a lie. Text inside the scene is localised; the
surrounding chrome mirrors normally; the scene's viewing azimuth (default south in the northern
hemisphere, north in the southern) is **stated, not inferred from layout direction**.
*What is refused:* a mirrored horizon; an RTL layout that flips the sky.

**E104 — a crafted or truncated import file.**
*Situation:* a hostile or truncated `lumintra-scout-*.json`.
*Required behaviour:* caps are validated **before** allocation — file size, spot count, horizon sample
count, string lengths. A 10,000,000-sample array must produce a typed error.
*What the user sees:* `That file is not a Lumintra scout file.`
*What is refused:* allocating from the file's declared length; an OOM kill.

**E12 / E77 — a horizon that blocks the whole sky.**
*Situation:* every horizon sample at 90°.
*Required behaviour:* legal — every target and pass is reported as "behind your horizon", the reason is
stated once, and a single non-blocking confirmation is offered because an all-90° profile is usually a
mis-draw.
*What the user sees:* `Your horizon blocks the whole sky.`
*What is refused:* an empty target list with no reason; silently caching an unusable profile.
**E76** keeps the distinct state `hasProfile = false` (calculations use 0°, every surface says so) —
*"no profile" and "flat profile" are not the same state*.

**E107 / E110 — the honest first-run and force-stop limits.**
*E107* (first launch, no network, no location, no saved spot): a usable scoped-empty brief with **one**
call to action ("set a spot"), the catalogue browsable offline, no dialogs. *Refused:* a sky computed
for (0, 0). *E110* (force-stop): on modern Android a force-stopped app receives no broadcasts and
cannot be restarted, so **the reminder is not guaranteed until the app is opened again** — this is
**stated as a limitation, not hidden**. **E109** (reboot) is `UNVERIFIED`: whether the reminder
survives a reboot, and with which `android:exported` value on a `BOOT_COMPLETED` receiver, must be
settled by a boot test on the A40.

**E21 — the Moon has exactly one source, and it is not yet verified.**
*Situation:* which source defines the Moon.
*Required behaviour:* exactly one source and one refraction convention, shared by the brief, the scene
and the widget. The port's Moon path has **never been numerically verified** (only the Sun case was,
at 0.0248 arcmin). *"That verification is a build task, not an assumption."*
*What is refused:* mixing `illumination()` (solar-system bodies) with a catalogue's magnitude for a
deep-sky object.

### 4.2 The remaining groups, summarised

- **2.1 Polar and high latitude (E01–E12):** the typed-no-darkness families (E02–E05) above; **E06** the
  scene's date is the **local date of sunset**, not the device's current date (asking at 01:30 for the
  night in progress resolves to the previous day); **E08** latitude validated to `[−90, +90]`, at
  exactly ±90° azimuth is undefined so compute altitude only and say the azimuth axis is meaningless;
  **E09** a spot's calendar date comes from the spot's zone; **E11** twilight windows are half-open
  `[start, end)` and a zero-length window is dropped, not rendered as a hairline.
- **2.2 The Moon (E13–E21):** E13 moonless all night; **E14** the Moon is up all night → the moonless
  interval is **empty and typed**, reporting the Moon's **minimum altitude and its time**; E15/E16 moon
  rises / sets inside the window; E17 full moon at the antisolar point; E18 new moon treated as
  moonless; **E19** phase angle reduced `mod 360` and cut into **8 sectors of 45° centred on
  0/90/180/270** (the wrap bug); E20 the Moon's rise/set does not occur → `None`.
- **2.3 Targets (E22–E31):** **E22** `NeverRises`; **E23** `Circumpolar(minAltitude, atInstant)`; E24
  two crossings in one window; **E25** "rise" is crossing of the **local drawn horizon** at that
  azimuth, not 0°; E26 grazing tangency → `Marginal`; **E27** missing magnitude → unranked, labelled
  `magnitude unknown`; **E28** the J2000-vs-date question (§7 open items); E29 lens fit; E30 search
  restored from `SavedStateHandle`; E31 stable string ids, not list indices.
- **2.4 Satellites (E32–E44):** E34 stop on **any** non-200, no retry; **E35** at most one fetch per
  update, GP updates every 2 h → 2 h TTL; **E37** no element set at first run offline → typed
  `unavailable`, never a bundled frozen set presented as current; **E38/E39** passes reported in two
  states — **occurs** and **observable** — a pass in Earth's shadow or in daylight is never silently
  dropped and never offered as observable; **E40** the observable window is the intersection (above
  horizon ∧ satellite sunlit ∧ observer dark) and the drawn streak matches the words; E41 one eclipse
  model; **E42** the drawn horizon is the detector's elevation mask (`ElevationMask(double[][])` exists
  — a reading taken on the smoke's vendored 13.1.5 jars, so re-check on the declared 13.1.9 — but is
  **unexercised**); **E44** drawn streaks capped, stated, data complete.
- **2.5 Weather (E45–E55):** E45/E46 above; **E47** a fixed honest `User-Agent` (the endpoint answered
  identically *without* one — `Compare-Object` difference count **0** over two 38,302-byte probes — but
  that is not permission, and a random string is a permanent ban); **E48** attribution with a licence
  link and a modification statement, surviving every locale; **E49** offline shows the cached reading
  with its age; **E50** one attempt then stop on 429; **E51** no hours in the window → typed; **E52**
  malformed schema → typed failure, no crash; E54 HTTPS only; **E55** always labelled a **forecast**,
  **total cloud only**.
- **2.6 Location (E56–E62):** **E56** permission denied → fully usable, requested only on an explicit
  action; **E57** coarse-only accepted for the sky but the spot is labelled `approximate` and nudgeable;
  E58 manual entry validated, hemisphere control not a forgettable `-`; E59 a saved spot with no
  coordinates is impossible by schema; E60 above; **E61** `ACCESS_COARSE_LOCATION` **only** if the
  optional convenience is offered, never `ACCESS_FINE_LOCATION`, never background; **E62** a spot is
  **independent** of the device's position once created.
- **2.7 Time (E63–E71):** E63–E65 above; **E66** the night is `[sunset on D, sunrise on D+1]`, both
  days labelled; E67 the polar-night window is the civil day; E68 a stated maximum scene span; **E69**
  one internal 24-h representation, display follows the platform preference; **E70** `now` captured once
  per brief and advanced by the UI; **E71** temperature/wind/visibility convert, **focal length and
  sensor dimensions do not** — millimetres everywhere.
- **2.8 The horizon profile (E72–E81):** E72 the quantised array (**see C-1 for the 360/720 conflict**);
  E73 the drawing canvas maps azimuth `0…360` across the width and elevation across a **declared**
  vertical field (default 0–30°); E74 re-rendering on a tablet is exact because the array is the truth;
  E75 a flat line is legal; **E76** no profile is a distinct state; E78 the profile masks the Moon's
  rise; **E79** the scene's silhouette is a rendering of the **same array** the maths uses; E80 the
  in-progress draft survives rotation; E81 panning is an independent view transform.
- **2.9 Rendering and accessibility (E82–E96):** **E82** rotation recomputes nothing and does not restart
  the animation; **E83** process death restores the frame from `SavedStateHandle`/`rememberSaveable`;
  **E84** star geometry cached in `drawWithCache`, dropped and rebuilt on `onTrimMemory`; E85/E88 above;
  **E86** the Night scene is **always** on the slot's dark surface regardless of system theme; E87 text
  reflows at 200 % font scale; **E89** the hero scene carries a TalkBack semantic summary and a
  `Seekable` time slider, every interactive element named and ≥ 48 dp; E90 colour is never the only
  signal; **E91** the rating prompt fires only after a completed session, never on first launch, never
  more than once per version, nothing gated behind it; E92 edge-to-edge + predictive back; E93 a grossly
  wrong device clock is shown with its UTC instant and a warning; E94 the Sun's altitude below the LUT's
  darkest band clamps; E95 the slider exactly on an event attributes the instant to the interval it
  opens; E96 no wake lock.
- **2.10 Data integrity (E97–E106):** E97 unreadable catalogue → starts, typed empty Targets, a
  build-time test asserts packaging; **E98** per-record load, a corrupt record is quarantined;
  E99 stable ids; **E100** atomic writes (temp file, then rename); E101 `schemaVersion` first, a newer
  version refused rather than half-imported; **E102** `dataExtractionRules` (API 31+) **and**
  `fullBackupContent` (≤ 30), regenerable caches excluded; E103 quarantine is durable; E104 above;
  E105 the zone id travels in the spot record; **E106** reinstall truthfully reports a restore or not.
- **2.11 First run, force-stop, reminder limits (E107–E111):** E107/E110 above; E108 first launch with
  network but no location fetches nothing before there is a spot; E109 `UNVERIFIED` (reboot re-arm);
  **E111** the reminder is built entirely from the plan already on the device, says so if computed
  offline, and never fetches at fire time.

---

## 5. The Play integration facts

### 5.1 Play In-App Review — the exact contract (`M` §1)

| Item | Value | Label / evidence |
|---|---|---|
| Artifact | `com.google.android.play:review` + `review-ktx` | MEASURED — `guide/playcore/in-app-review/kotlin-java` (updated 2026-10-01) |
| **Current stable version** | **`2.0.2`** (both) | MEASURED — Google Maven `review/maven-metadata.xml` → `<latest>2.0.2</latest>`, `lastUpdated 20241018`, and the doc page (2026-10-01) |
| Repository | **Google's Maven repository** (`google()`), not Maven Central | MEASURED |
| Licence | **Play Core SDK Terms of Service** (2020-09-24), **not** an OSS licence | MEASURED — the ToS is printed in `guide/playcore` and shipped inside the AAR |
| ToS terms that bind the build | Redistributable Code may be used **"solely for inclusion as part of your API Client"**; **"You will not modify, translate, or create derivative works."** | MEASURED |
| Does it need the old monolithic `play-core`? | **No — `play-core` is gone** (split per feature) | MEASURED |
| minSdk of the library | 21 (compatible with the slot's minSdk 26) | MEASURED — the AAR's merged manifest |
| API shape | `ReviewManagerFactory.create(context)`; `requestReviewFlow(): Task<ReviewInfo>`; `launchReviewFlow(activity, reviewInfo)` — **`Activity` required, not `Context`** | MEASURED |
| KTX shape | `suspend fun ReviewManager.requestReview(): ReviewInfo` and `launchReview(activity, reviewInfo)` — **not** `requestReviewFlow`/`launchReviewFlow` | MEASURED — constant-pool read of `ReviewManagerKtxKt.class` in `review-ktx-2.0.2.aar` |
| `ReviewInfo` lifetime | *"only valid for a limited amount of time … request … ahead of time (pre-cache) but only once you are certain that your app will launch the in-app review flow"* | MEASURED — so request and launch in the **same user-visible moment** |

**The correction that shapes the UI.** Google (quoted verbatim, `M` §1.1): *"you should not have a
call-to-action option (such as a button) to trigger the API, as a user might have already hit their
quota and the flow won't be shown … For this use case, redirect the user to the Play Store instead."*
**Therefore the correct build is two separate things:**

1. **Settings → "Rate Lumintra"** is a plain **open-the-Play-listing** link. It always works and can
   never silently do nothing. The intent is `ACTION_VIEW` on
   `https://play.google.com/store/apps/details?id=com.gexetr.lumintralightplanner` with
   `setPackage("com.android.vending")` (which removes the app-chooser).
2. **The in-app review flow** (the API) is fired **automatically at an earned moment** — after a
   session brief exists / after a share — with the result **ignored** and the app's flow unchanged.

| # | Constraint (all `MEASURED`, quoted in `M` §1.4) | What it means for Lumintra |
|---|---|---|
| C1 | A time-bound quota exists; *"less than a month"* between calls may show nothing; its value is secret and changeable | One call per user, at one earned moment — never per screen or per session |
| C2 | **A button must not trigger the API** | **Kills the Settings-button design.** Settings row → Play listing intent |
| C3 | The caller is told **nothing** — not even whether the dialog was shown; do not inform the user or change the flow | Quota exhausted ⇒ the coroutine just returns. No toast, no "thanks", no retry. The app must look identical either way |
| C4 | **No question may precede or accompany** the prompt ("Do you like the app?", "Would you rate 5 stars") | No "Are you enjoying Lumintra?" gate, no NPS screen, no two-step |
| C5 | No incentivised/gated/manipulated reviews | A review is worth **$0** in app currency — no reward, no unlock, no repeat nagging |
| C6 | The card must be surfaced **as-is**: no tampering, no overlay, topmost, no programmatic removal | No custom theming, no scrim, no `Dialog`, no auto-dismiss timer |
| C7 | Trigger only **after real value** | The earned moment is after a brief exists / after a share — not first launch, not the paywall |
| C8 | Needs the **Play Store app present**; otherwise the request fails | `INFERRED`: the failure surfaces as an unsuccessful `Task`/thrown `ReviewException` — swallow it silently |
| C9 | Wait for the flow before advancing the app | Don't start navigation/animation that would tear down the Activity while the card is up |

**No permission, no manifest component.** MEASURED (`M` §1.4): the merged `AndroidManifest.xml` of
`review-2.0.2.aar` is `<uses-sdk minSdk 21/>` and an **empty `<application/>`** — no `<uses-permission>`,
no `<activity>`, no `<provider>`, no `<service>`, no `<receiver>`. `review-ktx` is identical.

**Data safety effect.** `MEASURED` (`M` §1.5): the review flow puts *"user-entered data (rating and
free-text review)"* into Play Store processes governed by Play's ToS. **`INFERRED`, stated as a
judgement:** no new Data safety row is needed for the review flow (the data goes to the Play Store
app's own review UI, not to a Lumintra server); the **conservative alternative** — declaring
"App activity → User-generated content: Reviews" — is available if the spec author prefers to
over-declare. Either way **it changes nothing about permissions**.

**Play In-App Updates (`M` §1.6):** actor `com.google.android.play:app-update:2.1.0` (MEASURED, Google
Maven). **Recommendation: do not use it for v1** (`INFERRED`): Play already auto-updates; a forced
immediate-update screen fights the pre-drive job; it adds a second artifact **and** a Data safety row.
`UNVERIFIED` — no measurement exists of the install-rate effect on a paid planner (check: a shipped A/B
read in Play Console).

### 5.2 Permissions (`M` §2, `L` §1.2, E56–E62)

| Permission | Verdict |
|---|---|
| `android.permission.INTERNET` | **Needed** — the only one the planned feature set requires (forecast + orbital elements). Protection level **normal**, no runtime prompt; it is the pipe the coordinate travels down, not itself a Data safety row |
| `android.permission.POST_NOTIFICATIONS` | **Only if the reminder/widget ships** (CONTEXT.md §2 approved both). Protection level **dangerous**, added in **Android 13 (API 33)** (MEASURED). It must never block anything — denial disables the reminder only |
| `android.permission.ACCESS_COARSE_LOCATION` | **Not needed** (CONTEXT.md §5: *"Location permission: None needed"*). Manual coordinates, a saved spot, a map picker and `Geocoder` all work without it (`Geocoder` documents no `@RequiresPermission`, MEASURED). It would buy only *"where am I standing"*. If the optional auto-locate convenience is deliberately added, it is `COARSE` **only**, requested only on an explicit action — and E61 forbids `ACCESS_FINE_LOCATION` |
| Storage (`READ_/WRITE_`, `MANAGE_EXTERNAL_STORAGE`) | **None** — the share/export handoff uses `ACTION_SEND` + `FileProvider` or `ACTION_CREATE_DOCUMENT` |
| Camera / microphone / Bluetooth / `QUERY_ALL_PACKAGES` / `BILLING` | **None** |
| `ACCESS_BACKGROUND_LOCATION`, `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` | **Never** (§3.3) |

**Bottom line:** the honest permission set is **`INTERNET` alone**, plus `POST_NOTIFICATIONS` **only if**
the reminder/widget ships, plus `ACCESS_COARSE_LOCATION` **only if** an auto-locate convenience is
deliberately added. The slot must make *"works fully offline except the cloud forecast and satellite
passes"* a stated property and never claim the opposite.

### 5.3 Data safety — the exact declarations (`M` §3)

**Play's definition.** MEASURED (`M` §3.1, quoting Play): **"Collect" means transmitting data from
your app off a user's device**, and it counts *"irrespective of whether data is transmitted to you or a
third-party server."* Therefore **sending a saved coordinate to `api.met.no` is a collection.**

**The ephemeral-processing exception — and why it is not taken.** Play's own worked example is a
weather app sending user location, and Lumintra is literally that example. But MET Norway states **it
stores the requesting IP and any geocoordinates in its access logs** (`M` §3.2, MEASURED). Play's
standard requires the data to be *"retained for no longer than necessary to service the specific
request"*, and the receiver retains it. **Recommended, defensible position (`INFERRED`, flagged as a
judgement): declare the row normally rather than claiming the ephemeral exemption.**

| Data type | Collected? | Shared? | Purpose | Required? | Other |
|---|---|---|---|---|---|
| **Location → Approximate location** (the saved spot's coordinates, truncated to 4 decimals) | **Yes — collected** | **No** (app → API, not re-transferred by the developer) | **App functionality** (fetch the cloud-cover forecast) | **Optional** — the user chooses a spot, can refuse the forecast, and the app works without it | Encrypted in transit: **Yes** (HTTPS only). Retention: the app stores none server-side; the saved spot lives on the device |

- **This is the only row.** No personal info, financial info, health, messages, photos, audio, files,
  contacts, calendar, app activity, web browsing or device IDs is transmitted by the app.
- **`INTERNET` alone produces no row** (the definition is about data, not capability).
- A deliberate **over-declaration** (Precise instead of Approximate) is available and legal; the
  stricter row costs a sentence.
- **If a proxy server is ever added**, the declaration changes materially (the coordinate would pass
  through developer infrastructure).

**Privacy policy.** Required **unconditionally** (MEASURED, `M` §3.3): even an app that collects nothing
must complete the Data safety form and provide a policy link. Two placements: the listing's public HTTPS
URL (published by `scripts/publish-privacy.mjs`, never a placeholder) **and** the same policy openable
**from inside the app** (`slot.inAppPolicy`). The policy **must describe the forecast call** — name
`api.met.no`, the coordinate and requesting IP, why, and that the developer stores nothing. **A policy
that says "we collect nothing" would be false once the forecast ships.**

### 5.4 The attribution strings that must ship (`M` §4)

These are the exact strings for the **About / Credits** screen (`ui/settings/CreditsScreen.kt`). Where
the licence says "clear and visible", they must be readable **inside the app**.

| Source | Licence | Required credit (exact) |
|---|---|---|
| **MET Norway** (`api.met.no`) | CC BY 4.0 + NLOD 2.0 | **`Data from MET Norway (met.no) — CC BY 4.0`** with links `https://www.met.no` / `https://api.met.no` and `https://creativecommons.org/licenses/by/4.0/`. **Never use the word "Yr"** |
| **ESO Milky Way panorama** (`eso0932a`, Serge Brunier) | CC BY 4.0 — credit **mandatory, unaltered, clearly visible, not separated from the image** | **`ESO/S. Brunier`**, beside the image, plus `https://www.eso.org/public/images/eso0932a/`. Must not imply ESO endorsement. If AI-modified, say so |
| **NASA imagery** | Generally not copyrighted, with conditions | e.g. **`NASA/JPL-Caltech`** (or the specific instrument credit); **no NASA logo anywhere**; never "official"/"NASA-approved" |
| **IAU constellation figures** | CC BY 4.0 | **`Constellation figures: IAU and Sky & Telescope (Roger Sinnott, Rick Fienberg; patterns by Alan MacRobert) — CC BY 4.0`** with `https://www.iau.org`; reproduce **in full**; **no IAU logo** |
| **Solar System Scope** (planet/moon/Sun textures) | CC BY 4.0 | **`Planet, Moon and Sun surface maps: Solar System Scope, based on NASA elevation and imagery data. CC BY 4.0. https://www.solarsystemscope.com/textures/`** |
| **FK6** (fundamental star catalogue) | **CC0 1.0** — no credit required | Ship one anyway: **`Star positions: The Sixth Fundamental Catalogue (FK6), GAVO Data Centre — CC0 1.0`** |

**One screen, one list** carrying all six rows (only those that actually ship), each with its licence
name and a live link, satisfies every licence at once. The **ESO row is the strictest** — unaltered,
visible, beside the image — so the Milky Way image, wherever it appears in the scene or on the
postcard, carries `ESO/S. Brunier` **on or next to it**, not only in the credits list.

### 5.5 What moves revenue here — ranked (`M` §5)

This app has **no free funnel** — it is bought or it is not — so the only two quantities are
**impressions → purchase conversion** and **the volume of impressions**.

| Rank | Lever | Evidence | Factory status |
|---|---|---|---|
| 1 | **Store-listing conversion: icon, screenshots, first two lines** | MEASURED — Play's store-listing-experiments best practice: *"Get the biggest impact by testing icons, videos, and screenshots"*; one asset at a time, ≥ 1 week | **Partly covered** — the factory renders a slot-owned identity and photographs real screens, but **nothing A/B-tests an asset** |
| 2 | **Rating and review volume** | MEASURED — the shelf's winners carry large review counts at 4.8 (PhotoPills 8,220; Sun Surveyor 5,140) | **GAP — exactly the owner's request.** The slot has no review surface today |
| 3 | **Positioning, price, the one-sentence wedge** | MEASURED — the shelf's modal price is $9.99, ceiling $10.99 | **Covered** by `economics.json` + `differentiators.json` + `naming.json` |
| 4 | **Technical quality (crash/ANR) → discoverability** | MEASURED — Android vitals bad-behaviour thresholds: crash rate **1.09 %** overall / **8 %** per model, ANR **0.47 %** / **8 %**, 28-day average; Play may reduce visibility | **Covered by construction** (no SDK needed); **GAP:** the kill criterion *"≥ 50 % of buyers generate a brief within 30 days"* has no in-app measurement |
| 5 | **ASO: title, short description, keywords** | MEASURED — `naming.json`'s signed-out Play search: no card titled Lumintra; two incumbents already use `Light Planner` | **Covered** by the naming contract |
| 6 | **Localisation** | INFERRED from Play's per-language title field; `en` + `ru` declared | **Covered by rule**, not measured for revenue |
| 7 | **Update cadence / "freshness"** | **UNVERIFIED** — no Google page found stating release frequency is a discoverability factor | Correctly absent as a lever |
| 8 | **A free/paid boundary** | N/A — `purchase_only_no_ads` has no free tier | Correctly absent |

**What must NOT be added (`M` §5.2):** an in-app product / `com.android.billingclient` / paywall /
`PurchaseAuthority`; ads; a subscription; incentivised or gated reviews; keyword stuffing.

**The top gap, stated plainly (`M` §5.3):** the app has no rating/review loop and no listing
measurement, on a shelf whose winners are carried by 8,000-review, 4.8★ listings — and the factory
ships the listing artwork but never tests a variant. The owner's own request is the highest-value
single addition available, **provided it is built the way Google requires** (automatic at an earned
moment; Settings row opens the Play listing; never a button-triggered `launchReviewFlow`, never
sentiment-gated).

---

## 6. The licence and attribution facts

Two tables: **what may ship** and **what cannot**. Every licence was read at its own source (by the
reports, MEASURED there) and the obligation is stated. Full provenance discipline is
`.cursor/rules/visual-identity.mdc` and `docs/icon-provenance.md` for marks; for data assets the
obligation is identical and belongs in `store/` + the Credits screen.

### 6.1 Assets and data sources that **may** ship

| Asset / source | Licence | Attribution obligation | Source |
|---|---|---|---|
| **FK6** (fundamental star catalogue, GAVO) | **CC0 1.0** — public-domain dedication | **None required**; one shipped anyway as good practice (`Star positions: … FK6, GAVO Data Centre — CC0 1.0`) | N §1.4, §9; M §4; O ledger |
| **2MASS** (faint field) | Acknowledgement requested; **no commercial prohibition found; an explicit "public domain" sentence was NOT found → `UNVERIFIED`** | One **verbatim** IRSA acknowledgement string | N §1.4, §9; O §3.8 |
| **Wikidata structured data** (`P6257` RA, `P6258` Dec, `P1215`) | **CC0 1.0** | None required | N §1.4 |
| **NASA HEASARC** | Freely usable (US government); acknowledgement requested | Acknowledgement | N §1.4 |
| **IAU constellation figures / boundary TXT** (Alain MacRobert / Sky & Telescope) | **CC BY 4.0** ("Please reproduce the credit in full") | Full credit, visible, **no IAU logo** | N §1.7; M §4; O ledger |
| **IAU WGSN star names** | **CC BY 4.0** | Credit | N §1.4 |
| **ESO `eso0932a`** (Milky Way panorama, S. Brunier) | **CC BY 4.0** — credit **`ESO/S. Brunier` is MANDATORY and must be VISIBLE**, unaltered, not separated from the image | `ESO/S. Brunier` beside the image + licence link; no endorsement implied | N §1.4; M §4; O §4.1 |
| **NASA SVS** (CGI Moon Kit / Dial-A-Moon / Deep Star Maps) | **Public domain** | Acknowledgement; **no insignia**; no implied endorsement | N §1.4; M §4; O §1.1 L6 |
| **Solar System Scope** textures | **CC BY 4.0** ("even commercially") | The "based on NASA elevation and imagery data" string | N §1.4; M §4 |
| **NOAA / EOG VIIRS VNL** (nighttime lights) | **CC BY 4.0** | EOG / Payne Institute credit + product citations; **radiance only, a *relative* index — never Bortle, never mag/arcsec² until the model's legal status is cleared** | N §6.2–§6.3 |
| **Orekit 13.1.9 + Hipparchus 4.0.3** | **Apache-2.0** | Standard notice | N §7 |
| **astronomy.kt** (Astronomy Engine, Kotlin) | **MIT** | Standard notice | N §7 |
| **Orekit runtime data** (`tai-utc.dat`, 3,321 B) | Distributed with Orekit (Apache-2.0 family) | — | N §3.3 |
| **`api.met.no`** (cloud forecast) | **CC BY 4.0 + NLOD 2.0** | Visible `Data from MET Norway (met.no) — CC BY 4.0` + licence link + modification statement | N §4.6; M §4 |
| **CelesTrak GP (OMM/JSON)** (orbital elements) | **A bandwidth policy, not a licence** — states the data are *"freely available to all users"* and **neither forbids nor grants commercial use**; **no SPDX statement found → a paid app's affirmative licence is `UNVERIFIED`** | Record the decision + settle (§7 open items) | N §3.1, §8.1; L §0.1 |

### 6.2 Assets and data sources that **cannot** ship

| Asset / source | Licence | Why it cannot ship |
|---|---|---|
| **OpenNGC** | **CC BY-SA 4.0** (no `LICENSE` file; README L15) | Share-alike is incompatible with a closed-source paid APK |
| **HYG** | **CC BY-SA 4.0** | Same |
| **Hipparcos / Tycho** | **CC BY-NC 3.0 IGO** | Non-commercial — a $9.99 app is commercial |
| **Gaia DR3** | **CC BY-NC** | Non-commercial |
| **Stellarium's DSO catalogue** | **GPL** | Copyleft |
| **NGC 2000.0** | **Non-commercial** | Non-commercial |
| **Yale BSC5** | **No SPDX licence / no affirmative grant** (`CDS V/50/ReadMe` carries no copyright notice, MEASURED) | Relies on an unlicensed-by-default catalogue; a decision to ship must be recorded as such (N §8.6) |
| **The shared Gaia DR2 colour Milky Way panorama** (the one everyone reuses) | **CC BY-SA 3.0 IGO** | Share-alike; **self-bake from ESO `eso0932a` instead** |
| **Falchi et al. 2016 World Atlas** (Bortle/SQM raster) | **CC BY-NC** — *"distributed under … Creative Commons Attribution-NonCommercial … so long as the resultant use is not for commercial advantage"* (MEASURED quote) | Non-commercial; v1 ships no raster |
| **`dcf21/constellation_lines_iau.dat`** | **File description says CC BY 4.0, repo footer says GPL v3 — a measured conflict** | A conflict is not a licence; do not ship |
| **APASS** | Permission required | Not a grant |
| Any incumbent's code, screenshots, icon set or layout files | — | Copyright; **patterns were copied, never assets** (MEASURED — `K`'s scope note) |

### 6.3 The FK6 defect (measured) — a mandatory supplement

**MEASURED** (`N` §1.3, §8.8; `CONTEXT.md` §5): FK6 (the chosen CC0 star source) **does not contain
Sirius, Arcturus, Capella or Betelgeuse**. Its brightest entry is **`alpha Car` at `vmag = −0.62`**,
and its full range is **−0.62 … 9.71** (all rows carry a `vmag`). FK6 is a *fundamental astrometric*
catalogue, not a naked-eye list, so **a hand-authored supplement of the brightest stars** — with its
own two-source verification, per the `§7.1` row procedure — **is mandatory**, or the scene will omit
the sky's most recognisable stars. The settling check that found it:
`SELECT comname, vmag FROM fk6.fk6join ORDER BY vmag ASC`.

**Two FK6 numbers disagree and the live measurement governs.** The recon's `I` file states FK6 is
**4,150** stars; the live TAP `SELECT COUNT(*)` returned **4,151** (`N` §8.7, MEASURED). The spec must
use **4,151**. `O` §3.7's text also says *"4,150"* when echoing the recon; the O-ledger note that FK6
carries **`B−V` for all stars is `UNVERIFIED`** (check: read the GAVO cone-service column list and count
non-null `B−V`) is preserved here.

---

## 7. The data and algorithm contracts

Every subsection ends with a **What the specification must state** list (the normative items an
implementer must not have to guess or ask). Numbers are reproduced from `N` unless noted.

### 7.1 The object table (~250 objects)

**Three distinct kinds, not one file:** `assets/catalog/objects.json` (fixed-coordinate deep-sky and
double stars — **authored**, RA/Dec are constants, ~220 rows); `assets/catalog/constellations.json`
(the 88 IAU constellations — a **region**, not a point, a different record shape); and the
**computed** solar-system bodies (Sun, Moon, Mercury…Neptune, 10 records) — **a planet has no fixed
RA/Dec, so it is never authored**.

**Format decision (`N` §1.1):** JSON under `app/src/main/assets/catalog/` (nested fields; assets accept
arbitrary names and are read as a stream; 220 rows × ~200 B ≈ 50 KB). **Room is rejected** — 220
read-only rows need no query engine; all lookups are in-memory `Map<id, Object>` after parse. Each file
carries `{"schemaVersion": 1, …}`. The catalogue is a **build input** and works fully offline.

**`objects.json` schema (exact, `N` §1.2):** `id` (ASCII lowercase slug, stable key), `messier`, `ngc`,
`ic` (string|null with prefix), `name`, `nameRu` (hand-authored; `ru` is a declared locale), `type`
(enum, below), `raDeg` (**degrees**, `[0,360)`), `decDeg` (**degrees**, `[-90,+90]`), `epoch` (constant
`"J2000.0"`), `magV`, `magB` (or null), `magSystem` (`V`|`B`|null), `magKind`
(`integrated`|`peak`|`unknown`), `sizeMajArcmin`, `sizeMinArcmin`, `positionAngleDeg` (N→E),
`aliases[]`, `commonNames[]`, `sourceIds` (`{wikidata, heasarc, simbad}`), `verification`
(`{srcA, srcB, deltaArcmin}`), `magDisputed` (bool), `notes`. Derived at load: `raHours = raDeg / 15`,
`bestSeasonMonths`.

**`type` enum (closed set):** `galaxy`, `galaxy_group`, `globular_cluster`, `open_cluster`,
`emission_nebula`, `reflection_nebula`, `planetary_nebula`, `supernova_remnant`, `dark_nebula`,
`asterism`, `double_star`, `planet`, `moon`, `sun`. The last three are **only** in the computed section.

**The traps that become defects (`N` §1.3, MEASURED):**
- **RA units:** the table stores **degrees**; the engine takes **hours** — `horizon(...)` converts
  internally with `sinra = dsin(ra * 15.0)` (`astronomy.kt:5400-5413`), and `defineStar` documents `ra`
  in hours (`:452`). **Conversion happens once, at the engine boundary; never store hours.**
- **Observer convention:** `Observer(latitude, longitude, height)` — lat **N+/S−**, lon **E+/W−**,
  height **metres** (`astronomy.kt:1412-1431`). A west-positive store silently mirrors the sky.
- **Epoch:** `epoch="J2000.0"`, ICRS-aligned; no precession is applied to the stored numbers.
- **Magnitude:** `magSystem` and `magKind` are mandatory; **never mix V with B, or integrated with
  peak.** The engine's `magnitude()`/`illumination().mag` apply **only to solar-system bodies** — a
  deep-sky magnitude must not be asked of `illumination()`.

**Lawful sourcing (`N` §1.4).** No permissively-licensed complete deep-sky catalogue exists (§6.2).
Designations, coordinates and magnitudes are **facts**, so the route is to **author** a small table of
facts and **verify** each row against **two independent references**. Per row: designation/name from the
historical catalogues (public domain by age); type authored; coordinates from **Wikidata CC0**
(`P6257`/`P6258` — coverage measured **not uniform**: three tested objects had both, NGC 7000 had a
single coarse value, **IC 434 had none**); a **second independent** source (NASA HEASARC and/or the
discovery paper); a computed `deltaArcmin` between the two positions. **Acceptance: Δ ≤ 1.0′ for objects
with `sizeMajArcmin ≥ 5`, Δ ≤ 0.2′ for unresolved objects; a row outside tolerance is re-sourced or
dropped, never averaged.** Magnitudes cross-checked; **a > 0.50 mag disagreement is not averaged** —
set `magDisputed=true`, store both in `notes`, prefer the named primary.

**Selection rule (`N` §1.5).** **All M1–M110 → 110 rows** (M102 is disputed — the spec must state how
that row is resolved; shipping it as a documented duplicate/`notes` entry is the honest option).
**Every non-Messier NGC/IC with a sourced `magV ≤ 10.0` → ~110 rows** (extend to **10.5** only if
needed; keep the brightest 110 if more). This is a **rule over magnitudes, never a copied list** (which
is why the Caldwell catalogue is not used). **Solar system: 10 computed records.** **Total:** 110 + ~110
authored = **~220**, + 10 computed → **~230**; the brief's *"~250"* is reached by the extension rule.
**The spec must state the exact count it ships and the magnitude limit used**, because *"about 250"* is
not checkable.

**What the specification must state:**
- Two authored JSON files under `assets/catalog/` plus a **computed** solar-system section; the schema,
  types, units and the closed `type` enum are exactly the §7.1 list.
- **RA stored in degrees `[0,360)`; Dec in degrees `[-90,+90]`; epoch J2000.0 / ICRS.** Every engine
  call that takes RA takes it **in hours**, converted at exactly one boundary in the code.
- **`magSystem` and `magKind` are mandatory**; no comparison mixes V with B or integrated with peak.
- The list is **110 Messier rows + every non-Messier NGC/IC with `magV ≤ 10.0`** (extended to 10.5 only
  if needed), and the shipped count and limit are stated.
- Every authored row carries a `verification` object with **two independent sources and a computed
  `deltaArcmin`**; a row outside the stated tolerance is re-sourced or dropped, **never averaged**.
- **No third-party compiled catalogue is copied**; the allowed references and their licences are exactly
  §6.1, and the denylist items are absent.
- A planet/Moon/Sun is a **computed** record (`ephemeris:"computed"`), never authored, and is computed
  by **`astronomy.kt`, not Orekit**.
- Constellations are a **separate 88-row table**; boundary geometry, if drawn, carries the **IAU CC BY
  4.0** credit.

### 7.2 Time and geometry

**Time model (`N` §2.1).** **All computation is in UTC.** A spot stores
`{ id, name, latDeg, lonDeg, elevationM, tzId }` where **`tzId` is an IANA zone id** (`"Europe/Lisbon"`),
**never a numeric offset** (`java.time.ZoneId`, available at minSdk 26, no desugaring). The session's
night is keyed to the spot's **local-noon-to-local-noon** window, not midnight-to-midnight.

**Twilight definitions (`N` §2.2, MEASURED — USNO).**

| Term | Source's words | Solar **geometric** altitude |
|---|---|---|
| Civil twilight | *"when the center of the Sun is geometrically **6 degrees** below the horizon"* | **−6.0°** |
| Nautical twilight | *"…geometrically **12 degrees** below…"* | **−12.0°** |
| Astronomical twilight | *"…geometrically **18 degrees** below…"* | **−18.0°** |
| Sunrise / sunset | *"geometric zenith distance of center of the Sun is **90.8333 degrees**"* = **50 arcminutes** below = **16′ semidiameter + 34′ refraction** | **−0.8333°** |

**Two consequences.** Twilight is defined on the Sun's **centre, geometrically** — **no refraction, no
semidiameter** — so the twilight tests call the engine with `Refraction.None` semantics
(`searchAltitude` passes `bodyRadiusAu = 0.0`). **Sunrise/sunset is a different test** (50′-below;
`searchRiseSet` implements semidiameter + dip + refraction). **The app must not use −18° for sunset or
`searchRiseSet` for the dark window** — the wrong one shifts the dark window by minutes.

**The dark-window algorithm (`N` §2.3).** *Definition:* the set of intervals in which the Sun's
**geometric** altitude is **≤ −18.0°**. Algorithm: sunset/sunrise via `searchRiseSet`; a **10-minute
sample** of the Sun's altitude over `[localNoon, localNoon+24h]` for the extrema; a **1-hour re-armed
probe loop** for every **−18°** crossing (the same discipline as the satellite segmentation — a single
long call can step over a short window); emit a **`darkWindows` array plus a `kind` discriminator**
(because at 48–55° latitude near the solstice a session can have **two** dark windows).

| Case | Condition | Required output | User sees |
|---|---|---|---|
| **A. Normal** | ≥1 −18° crossing pair | `{kind:"dark", darkWindows:[{startUtc,endUtc,durationMin}], sunAltMinDeg}` | the dark window |
| **B. No astronomical darkness** | no −18° crossing, `sunAltMinDeg < −12.0` | `{kind:"no-astro-dark", bestTwi:"nautical", twilightWindows:[…], sunAltMinDeg}` (civil if `−12 < min < −6`; `{kind:"no-dark",…}` if `> −6.0`) | an explicit sentence |
| **C. Sun never sets (polar day)** | `searchRiseSet(Set)` null **and** `sunAltMinDeg > −0.8333°` | `{kind:"polar-day", sunAltMinDeg, sunAltMaxDeg}` — **no window**, said in words | a true light statement |
| **D. Sun never rises (polar night)** | `searchRiseSet(Rise)` null **and** `sunAltMaxDeg < −0.8333°` | `≤ −18°` → one whole-dark window + `wholeNightDark:true`; else `{kind:"polar-night", darkWindows:[…], middayTwilightDip:{…}}` | the Sun's **maximum** altitude as a fact |
| **E. Night spans midnight** | normal | an **interval pair in UTC**, never a date; `dateLabel` = the local date of `startUtc`; one row | `night of 8 → 9 Sep` |
| **F. Sunset/sunrise on different calendar dates** | identical to E | same; **forbid deriving a duration from two formatted date strings** | as E |
| **G. Two dark windows in one night** | two −18° crossing pairs | `darkWindows` **length 2**, each with its own duration; "best" = longest; say "two windows tonight" | both |

**Universal rule:** the window is `[start, end)`; durations computed in UTC; **a `null` from an engine
call is a case, not an error, and must map to a `kind`** — never to an empty list shown as "no darkness
tonight". (`N` §2.3 note: `searchAltitde`/`searchRiseSet` signatures at `:6911`/`:6818`; `Direction.Rise`/
`Set` at `:1586`.)

**Moon (`N` §2.4).** Phase (elongation) = the Moon's ecliptic longitude minus the Sun's, **degrees in
`[0,360)`**, 0° = new, 180° = full — `moonPhase(time)` (`astronomy.kt:6173`). Illuminated fraction =
`[0.0, 1.0]` — `illumination(Body.Moon, time).phaseFraction` (`:2918-2920`). Phase angle (degrees) at
`:2913-2916`; magnitude `:2901`. Equivalent closed form for a unit test: `k = (1 + cos(i)) / 2`. **Phase
name:** 8 bins, 45° wide, offset 22.5° around each exact phase. **"Moonless":** two booleans and a stated
threshold — `moonUp = (moonAltDeg > 0.0)`; `darkMoon = (illuminatedFraction <= 0.10)` ← **a product
threshold, stated as such**; `moonless = (!moonUp) || darkMoon`. **The UI shows both numbers** (fraction
and altitude), because a 5 % crescent 10° up is not moonless in practice. The Moon's contribution to
sky brightness is **not** a number in v1; if added, the only licence-clean route is to **implement the
Krisciunas & Schaefer (1991) model** (implementing a method is not redistributing data), labelled a
**model**, never a measurement — its accuracy is `UNVERIFIED` (no reference implementation).

**Target altitude / refraction / circumpolar (`N` §2.5).**
`horizon(time, observer, ra, dec, refraction) → Topocentric{azimuth, altitude, ra, dec}` (`:5400`,
`:1685-1703`); **azimuth 0 = North, 90 = East**; returned `ra` in **sidereal hours**. Refraction enum
`None|Normal|JplHor` (`:1563-1586`). **`Normal` is Saemundsson's**, verbatim:
`angle_deg = (1.02 / tan(h + 10.3/(h + 5.11))) / 60.0`, `h = max(altitude, −1.0°)`, and for
`altitude < −1.0°` multiplied by `(altitude + 90.0)/89.0` (`:4015-4031`);
`REFRACTION_NEAR_HORIZON = 34.0/60.0` (`:196`) matches USNO's 34′. **Contract:** use `Refraction.Normal`
for a target's displayed altitude and the user's altitude mask; **no refraction** for the −6/−12/−18°
twilight tests — **per-use, not global**. A target is *above the mask* when
`altitudeDeg(Normal) ≥ maskDeg` (default **10.0°**, user-adjustable). Classify by **sampling at 10-minute
steps** then refine: `min > maskDeg` → **alwaysUp (circumpolar)**; `max < maskDeg` → **neverUp** (say so);
otherwise `searchRiseSet(...)` then **refine to the mask** with `searchAltitude(..., maskDeg)`.
**Transit** = max altitude by a 10-minute scan refined to **1 minute** — and `searchHourAngle` **is
present** (`astronomy.kt:6363`, MEASURED 2026-10-08; `astronomy.kt` revision `D4213F7E…`, 432,946 B),
so it *may* replace the scan. **`defineStar` path present but not exercised**
— no `defineStar` call appears in the recon's corpus runner → `UNVERIFIED` end-to-end.

**What the specification must state:**
- **All computation is UTC**; a spot stores an IANA `tzId`, never a numeric offset; the night is the
  spot's **local-noon-to-local-noon** window.
- Twilight boundaries are **geometric Sun-centre −6.0° / −12.0° / −18.0°** (USNO); sunrise/sunset is
  **−0.8333°** (90.8333° zenith = 50′ below); **the two tests must not be interchanged.**
- The dark-window output is a **`darkWindows` array plus a `kind` discriminator**, with the exact
  outputs for the normal / no-astro-darkness / polar day / polar night / midnight-spanning / cross-date /
  two-window cases as tabulated.
- Moon phase is the **elongation in degrees `[0,360)`**; the fraction is `[0,1]`;
  `"moonless" = (!moonUp) || (fraction ≤ 0.10)` with the threshold stated as a product choice and **both
  numbers always shown**.
- A target's altitude/azimuth comes from `horizon(...)` with **`Refraction.Normal`** for display/mask and
  **no refraction** for the twilight tests; circumpolarity is decided by **sampling** (`min`/`max` vs
  mask), not by subtracting rise and set.
- A `null`/absent crossing is **a case with a `kind`**, never an empty list presented as a result.

### 7.3 Satellite passes

**Element source and terms (`N` §3.1, `L` §0.1–§0.2).** Endpoint (MEASURED):
`GET https://celestrak.org/NORAD/elements/gp.php?GROUP=<GROUP>&FORMAT=JSON` → HTTP 200, 9,270 B for
`GROUP=STATIONS`, 17 OMM records. Terms (MEASURED, verbatim): *"Only download the data you need, when
you are going to use it, and only download data once per update. For GP data, updates are once every
2 hours."*; *"M2M … should immediately stop querying when it receives any non-HTTP 200 responses…"*;
*"Users need to update their software to use the CSV formats."*; the banner: the 5-digit catalog space
was **exhausted on 2026-07-11** and *"GP data will not be available for them using the TLE format."*
GP is distributed as **OMM** (CCSDS 502.0-B-3) with JSON/CSV *"using the same keywords and definitions
as provided in the OMM standard"*. **Licence status: the policy page is a bandwidth policy, not a
licence; it neither forbids nor grants commercial use; no SPDX statement was found → a paid app's
affirmative licence is `UNVERIFIED`** (§7 open items). The alternative (Space-Track.org) needs a **free
account**, and account credentials cannot live in a client, so CelesTrak is the **only keyless route**.

**OMM JSON keys (MEASURED):** `OBJECT_NAME, OBJECT_ID, EPOCH, MEAN_MOTION, ECCENTRICITY, INCLINATION,
RA_OF_ASC_NODE, ARG_OF_PERICENTER, MEAN_ANOMALY, EPHEMERIS_TYPE, CLASSIFICATION_TYPE, NORAD_CAT_ID,
ELEMENT_SET_NO, REV_AT_EPOCH, BSTAR, MEAN_MOTION_DOT, MEAN_MOTION_DDOT`. Example (ISS):
`EPOCH=2026-10-08T00:21:12.026592`, `MEAN_MOTION=15.48769982`, `BSTAR=9.651885e-5`. **Groups to ship:**
`STATIONS` and `VISUAL` (the 100 brightest) plus a `CATNR=<noradId>` fetch — **not** the full `ACTIVE`
list.

**OMM → Orekit `TLE` (the *fallback* unit contract, `N` §3.2, MEASURED from the 13.1.5 javadoc — the
smoke's vendored build; the declared pin is now 13.1.9).** The route is now **direct**:
`orekit-13.1.9.jar` ships `OmmParser` → `Omm` (`Omm.generateTLE()`, `Omm.generateKeplerianOrbit()`),
so the hand mapping below is the fallback. The field
constructor takes: `meanMotion` **rad/s** (≥ 0); `meanMotionFirstDerivative` **rad/s²**;
`meanMotionSecondDerivative` **rad/s³**; `e` dimensionless `0 ≤ e ≤ 1`; `i`, `pa`, `raan`,
`meanAnomaly` in **radians** with `[0, π]` / `[0, 2π)` ranges. **Therefore:**

```
i_rad    = INCLINATION     × π / 180
raan_rad = RA_OF_ASC_NODE  × π / 180
pa_rad   = ARG_OF_PERICENTER × π / 180
ma_rad   = MEAN_ANOMALY    × π / 180
n_radps  = MEAN_MOTION     × 2π / 86400          # rev/day → rad/s
nDot     = MEAN_MOTION_DOT × 2π / 86400²         # rev/day² → rad/s²
nDDot    = MEAN_MOTION_DDOT × 2π / 86400³        # rev/day³ → rad/s³
epoch    = AbsoluteDate(EPOCH, TimeScalesFactory.getUTC())
```

**`meanMotion` is in rad/s, not rev/day** — passing `15.48769982` straight through *"yields a satellite
that falls out of the sky"*, and this is *"the most likely silent defect in the satellite path"*. Orekit
**normalises** angles into `[0, 2π)`, so the converter must **wrap, not clamp**. `classification =
CLASSIFICATION_TYPE[0]`; `satelliteNumber = NORAD_CAT_ID`; `elementNumber = ELEMENT_SET_NO`;
`revolutionNumberAtEpoch = REV_AT_EPOCH`. **The two-line `TLE(line1, line2)` constructor is not usable**
for the growing 6-digit set — **the field constructor is the required route**.

**Propagation and runtime data (`N` §3.3).** `TLEPropagator.selectExtrapolator(tle)` → PV in **TEME** →
`FramesFactory.getTEME()` → `getITRF(IERSConventions.IERS_2010, simpleEOP = true)` → `TopocentricFrame`.
Measured sample: `TEME_AT_T+600s pos_km=(1280.749619, 5592.447086, 3605.982869)`, `TOPO_AT_T+600s
elevation_deg=−44.136279 azimuth_deg=354.524835`. **Orekit refuses to run without an IERS UTC-TAI
provider** — `no IERS UTC-TAI history data loaded`; with **3,321 bytes** (`tai-utc.dat`) the whole chain
runs, `simpleEOP=true` removes the need for `finals.all`. **Ship `assets/orekit-data/tai-utc.dat`** and
keep `itrf-versions.conf` prefixes plain (`eopc04_05`, not a regex).

**The visible-pass definition (`N` §3.4).** A pass is reported only when **all** hold: (1)
`maxElevationDeg ≥ maskDeg` (default **10.0°**, user-adjustable **0–45°**); (2) the satellite is
**sunlit** (not inside the Earth's umbra — an `EclipseDetector`/`OneAxisEllipsoid` primitive is
**present but the visibility policy is unexercised → `UNVERIFIED`**); (3) the observer is dark — the
Sun's **geometric** altitude **≤ −6.0°** (default; **state which threshold**, −6° "visible in
twilight" vs −18° "fully dark", and expose the choice); (4) AOS/LOS from the elevation detector at the
observer's `TopocentricFrame`. **Brightness:** CelesTrak's OMM carries **no magnitude field**
(MEASURED), so **the app must not compute a magnitude from `BSTAR`** (a drag proxy, not photometry) and
**claims no brightness** without a curated per-satellite table; a brightness label without a table is
`UNVERIFIED`. **Merging:** fetched groups are merged and de-duplicated by `NORAD_CAT_ID`, sorted by AOS.

**The segmentation trap and the fix (`N` §3.5, MEASURED).**

```
DETECTOR  events=2  aos=1        # one propagate() over 3 days
BRUTE     samples=4321 aos=15 max_elevation_deg=89.067 window_days=3.0 mask_deg=10.0
segmented-1h maxCheck=60   events=30   aos=15     # agrees exactly with the brute scan
```

**Precise enough to implement:** window = the session's dark window capped at 24 h; **segment length =
3600 s**; for **each** segment build a **fresh** `ElevationDetector` with `maxCheck = 60.0 s` (default
threshold `1e-6`) attached to a propagator **started at `t`**, propagate to `t + 3600`; re-arming is the
point. Collect both directions (acquisition and loss) and pair into passes; keep a pass only if
`maxElevationDeg ≥ maskDeg`. **Do not fall back to a 60-second scan at runtime** (that is the *check*).
*Note the F-map recorded rather than hid:* one probe row labelled `segmented-10m` actually passed
`segmented=false`, so **the label alone is not evidence** — the test must assert the segmented finder's
AOS count equals a brute-force scan.

**Absent / stale / offline (`N` §3.6, `L` E36–E37).** **Absent** → the satellite is omitted and the
section shows **"satellite data unavailable"**, never an empty list rendered as "no passes tonight".
**Stale** → compare `EPOCH` with the session instant. **The thresholds conflict between the two reports
(C-2):**

| Age | `L` E36 | `N` §3.6 |
|---|---|---|
| `≤ 24 h` | normal, no badge | normal, no badge |
| `24–72 h` | shown with *"elements are N days old — times may be off by minutes"* | **normal** (no badge) |
| `72–168 h (7 d)` | **no pass times at all** — *"element set expired; connect once to refresh"* | compute, but badge *"element set is N days old — pass times may shift by minutes"* |
| `> 168 h (7 d)` | (covered by the > 72 h refusal) | **refuse to compute**; "data too old"; offer refresh |

**Both are stated as product choices, not measurements.** They are grounded on two measured facts: a
normal age is **hours** (same-day epochs measured) and the data change only every **2 hours**; and the
propagator's error reaches **27,924 km at ~21.3 days**, so a week is far past usefulness while a day is
comfortably inside it. **The spec must state one table and the UI must name the threshold it applied.**
**Offline:** no fetch; use the last cached sets **with the same age rules** (a cache hit is never
treated as fresh); cache record `{ noradId, group, epochUtc, fetchedAtUtc, payload }`; empty cache →
unavailable. **Politeness:** store `fetchedAtUtc` per group, **do not re-fetch within 2 hours**, on
**any non-200 stop and log — never retry**.

**The accuracy the app may claim (`N` §3.7).** Two different numbers, never confused:
**Propagator fidelity (MEASURED)** — Orekit's SGP4 reproduces the reference implementation (Vallado's
`TCPPVER`) to **`pos_tol = 1.0e-4 km`** and **`vel_tol = 1.0e-7 km/s`** (29 of 33 corpus blocks within;
worst in-tolerance `1.97e-6 km`; block `20413#1` 26/26 within). **That is agreement with a reference
implementation, not with the sky.** **Prediction error (MEASURED bound)** — the same propagator
diverges **27,924.0197880 km** (velocity error `1.99701767930 km/s`) at **+1,844,000 s ≈ 21.3 days**
past epoch. **What the app may say:** *"Pass times are computed from NORAD GP element sets. A fresh
element set (hours old) is accurate to within a few minutes; the age of the set is shown, and passes
older than 7 days are not shown."* **What it may not say:** that a pass time is accurate to 0.1 m or to
a stated number of seconds — the *"few minutes"* bound is **`UNVERIFIED`** (check: a same-night AOS
comparison against an independent prediction, recording the element-set epoch).

**What the specification must state:**
- The source is **CelesTrak GP** in **OMM JSON**, fetched **once per update** with the **2-hour** cache
  rule and **stop-on-non-200**; no affirmative commercial licence was found and the decision to use it is
  recorded with the settling check.
- The **OMM→Orekit conversion formulas** above, literally, including `n = MEAN_MOTION × 2π / 86400`
  (rad/s) and the `[0, 2π)` normalisation.
- `assets/orekit-data/tai-utc.dat` (**3,321 B**) is bundled; `simpleEOP = true`; plain `itrf-versions.conf`
  prefixes.
- A pass is reported only when **elevation ≥ mask (default 10.0°)**, the **satellite is sunlit** and the
  observer's Sun altitude **≤ −6.0°** (state the threshold used).
- **No brightness is claimed** without a curated magnitude table; **`BSTAR` is never converted to a
  magnitude.**
- The finder **segments into 3600 s windows with `maxCheck = 60 s` and a fresh detector per segment**, and
  a test asserts its AOS count equals a brute-force scan (the trap: one call gave `aos=1` where the truth
  is `aos=15`).
- The **staleness thresholds** are product choices (**one** table — see C-2); **absent data shows
  "unavailable", never "no passes"**; offline uses the cache **with the same age rules**.
- The only accuracy claim is the **0.1 m propagator fidelity**, explicitly labelled fidelity and **not**
  prediction accuracy; the app's own pass-time accuracy stays `UNVERIFIED`.

### 7.4 Cloud forecast

**Endpoint (`N` §4.1):** `GET https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=<lat>&lon=<lon>[&altitude=<m>]`.
**Coordinates must be truncated to a maximum of 4 decimals** — *"For new products, requests with 5+
decimals will return a 403 Forbidden"* (a recon payload of this endpoint was 38,302 bytes).

**Required custom headers (`N` §4.2, MEASURED — MET Norway ToS):** `User-Agent` — *"All requests must
(if possible) include an identifying User Agent-string … with the application/domain name, optionally
version number. You should also include a company email address or a link to the company website"*;
**contract value `User-Agent: Lumintra/<versionName> (v.sobolenko.vs@gmail.com)`** (the developer
account's public address, not a secret). `If-Modified-Since`; honour `Expires`; never `HEAD` then `GET`;
do not schedule on the hour (add a random number of minutes); **mobile apps must not retrieve new data
while the app is not in use**; the ceiling is **20 requests/second per application**; a **203** response
means the product version is deprecated (log/show a warning).

**The field to read (`N` §4.3, MEASURED against a live payload).** GeoJSON `type = "Feature"`;
`properties.meta.units.cloud_area_fraction = "%"`; `properties.timeseries[]` — hourly points (**87
measured**), each `.time` ISO-8601 UTC and `.data.instant.details.cloud_area_fraction` a number
**0–100 in per cent** (measured `100.0`). **Only TOTAL cloud is available** — no low/mid/high split —
so the UI says **"total cloud cover"** and never "clear sky" or a seeing estimate.

**Cache policy (`N` §4.4).** Per spot `{ lat4, lon4, fetchedAtUtc, expiresUtc, lastModified, etag,
payload }`, keyed by the **truncated** coordinates (two spots 100 m apart are one request). Re-request
only after `expiresUtc`; send `If-Modified-Since`; add a random minute offset; never refresh in the
background.

**Offline (`N` §4.5).** Show the cached forecast with its **age** and an explicit *"offline — showing
data from <local time>"*; never fabricate and never present a stale value as current. No cache →
unavailable. The app must still function fully (dark window, Moon and targets are local).

**Attribution (`N` §4.6).** `Weather data from MET Norway (api.met.no), licensed CC BY 4.0`, with a
**link to the licence** and a *"shown unmodified"* note, **visibly beside the forecast** and in Credits.
The naming restriction forbids using the word **`Yr`** and implying endorsement.

**What the specification must state:**
- The endpoint is `api.met.no …/locationforecast/2.0/compact`, with coordinates **truncated to 4
  decimals** (5+ → 403).
- The request carries **`User-Agent: Lumintra/<versionName> (v.sobolenko.vs@gmail.com)`** and
  **`If-Modified-Since`**, honours **`Expires`**, never uses `HEAD`+`GET`, and **never refreshes while
  the app is not in use**.
- The field read is **`properties.timeseries[].data.instant.details.cloud_area_fraction`**, a percentage
  **0–100**, and the UI says **"total cloud cover"** (no layer split exists).
- The cache is keyed by truncated coordinates and records `fetchedAtUtc` / `expiresUtc` / `lastModified`.
- Offline shows the cached value **with its age and an "offline" label**, or "unavailable".
- The attribution appears **visibly beside the forecast** and in Credits, with a licence link and the
  modification statement, and the app uses neither `Yr` nor any implication of endorsement.

### 7.5 Field of view

**The math (`N` §5.1).** For a rectilinear lens, `FOV_axis_deg = 2·atan(d_axis / (2·f))` with `d_axis`
the sensor extent in **millimetres** and `f` the focal length in **millimetres**; applied per axis and
to the diagonal. Sanity check (INFERRED from the formula): 50 mm on full frame (36×24) → `39.6°`
horizontally; 14 mm → `114.3°` diagonally.

**The device camera's own frame (`N` §5.1, MEASURED by `javap` over `android-35/android.jar`):**
`CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE` (Key<SizeF>, **mm**),
`LENS_INFO_AVAILABLE_FOCAL_LENGTHS` (Key<float[]>, **mm**),
`SENSOR_INFO_ACTIVE_ARRAY_SIZE` (Key<Rect>, pixels). **Honest limit:** `SENSOR_INFO_PHYSICAL_SIZE` is
the **full** sensor while the delivered frame may crop, so the computed FOV is an **upper bound**.
**35 mm-equivalent:** `f_equiv = f · (43.2666 / d)`, `d = √(w²+h²)`, **full-frame diagonal 43.2666 mm**.

**Presets to ship (`N` §5.2, the formula's own outputs — INFERRED — generated, never hand-typed):**

| Preset | Sensor | Focal | FOV_h | FOV_v | FOV_diag |
|---|---|---|---:|---:|---:|
| Full-frame fisheye | 36×24 | 8 mm | 132.1° | 112.6° | 139.4° |
| Ultra-wide | 36×24 | 14 mm | 104.3° | 81.2° | 114.3° |
| Wide | 36×24 | 20 mm | 84.0° | 61.9° | 94.5° |
| Wide | 36×24 | 24 mm | 73.7° | 53.1° | 84.1° |
| Standard | 36×24 | 35 mm | 54.4° | 37.8° | 63.4° |
| Standard | 36×24 | 50 mm | 39.6° | 27.0° | 46.8° |
| Portrait | 36×24 | 85 mm | 23.9° | 16.1° | 28.6° |
| Tele | 36×24 | 135 mm | 15.2° | 10.2° | 18.2° |
| APS-C (crop 1.5) | 23.5×15.6 | 18 mm | 66.3° | 46.9° | 76.4° |
| Micro 4/3 (crop 2.0) | 17.3×13.0 | 12 mm | 71.6° | 56.9° | 84.4° |

**On-scene frame vs real frame (`N` §5.3).** Use an **equidistant** mapping (angle ∝ distance from the
aim point) so the overlay is a **true 1:1 angular rectangle** (`pxPerDeg = canvasWidthPx /
sceneFovHorizDeg`). **Under any other projection the overlay must be drawn from the projected four
corners**, never pixel-ratio scaling. **Rotation modes:** horizon-landscape / horizon-portrait /
free-roll, and the mode is recorded with any export. **Multi-frame:** an N×M grid at the same angular
scale with a user-set overlap.

**Target size comparison (`N` §5.4).** `frameFrac = objectAngularSizeDeg / frameFovShortSideDeg`;
`pixelsOnSensor = objectAngularSizeDeg · (imageWidthPx / fovHorizDeg)`. For an extended object
`objectAngularSizeDeg = sizeMajArcmin / 60.0`. **A point source (star) has no angular size** — show the
framing, never a disc sized from magnitude. A **planet's** disc is a real small angle, but `astronomy.kt` supplies **no** planet
disc-diameter API (MEASURED 2026-10-08), so it needs a small authored table or omission; the **Moon's**
apparent disc diameter **is** present (`astronomy.kt:1804`, `:7844`). **The app must
not equate magnitude with visibility** — the honest output is framing + altitude + dark window +
moon/cloud state.

**What the specification must state:**
- `FOV_axis = 2·atan(d_axis/(2·f))`, per axis and diagonal; `f_equiv = f · 43.2666 / d`; full-frame
  diagonal **43.2666 mm**.
- The device camera reads `SENSOR_INFO_PHYSICAL_SIZE` (mm) and `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` (mm),
  and the FOV is an **upper bound** because the frame may crop the sensor.
- The **lens preset table** ships with FOVs **generated by the formula**.
- The scene uses an **equidistant projection** so the frame overlay is a true 1:1 rectangle; under any
  other projection it is drawn from the projected four corners.
- Frame rotation modes are horizon-landscape / horizon-portrait / free-roll, recorded with any export.
- `frameFrac` and `pixels` above are the only size comparisons; **no visibility is derived from
  magnitude**, and a star is never drawn as a disc sized from `magV`.

### 7.6 Sky brightness and darkness quality

**The licence finding (`N` §6.2, MEASURED).** The Falchi World Atlas (the Bortle/SQM-grade raster) is
**CC BY-NC** and **must not be bundled** in this paid app. The only licence-clean raster family is
**NOAA/EOG VIIRS VNL (CC BY 4.0)** — but it is a **radiance** product (nW/cm²/sr), and turning it into
mag/arcsec² or a Bortle class requires a model whose standard form is the CC BY-NC paper's; that is a
**legal-review item, not an engineering step**. **A licence-clean *source* exists, but no licence-clean
*product* does.** **v1 ships no light-pollution raster.**

**What is reported instead (computed, free of bundled third-party data):** the **darkest Sun altitude**
reached (`sunAltMinDeg`); the **dark window** itself (start/end/duration/`kind`); the **Moon illuminated
fraction × altitude**; the **total cloud cover** from `api.met.no` (CC BY 4.0 + visible attribution);
and the **user's own** SQM reading (mag/arcsec²) or a **user-selected Bortle class 1–9** labelled
**"user estimate"** — *a number the user supplies is not third-party data*, which is the key move that
lets the app show a darkness quality without shipping anything it has no right to ship. `SkyGrade` is
named in CONTEXT.md §3.4 and is **never a Bortle class**.

**If a real VIIRS layer is later wanted**, two prerequisites: (1) produce a **reduced, coarse** (e.g.
0.5°×0.5°) radiance grid from the CC BY 4.0 VNL product with the EOG/Payne credit; (2) clear the
radiance→brightness model as a legal item, and until then present a **relative index**, never mag/arcsec².

**What the specification must state:**
- **The Falchi raster is CC BY-NC and MUST NOT be bundled** (the source is quoted, so the refusal is
  auditable).
- The only licence-clean raster family is **VIIRS VNL (CC BY 4.0)**, a **radiance** product that yields
  at most a **relative** index — never a Bortle class, never mag/arcsec², until the model is cleared.
- **v1 ships no raster**; darkness is reported from **computed** quantities and the **user's own** SQM
  reading or **user-selected** Bortle class labelled "user estimate".
- Any future VNL layer carries the **EOG / Payne Institute** credit and product citations, and its reduced
  asset is a build input, not a download.

### 7.7 Engine assignment, verified vs merely present

| Concern | Engine | Licence | Runtime data |
|---|---|---|---|
| Sun, Moon, planets | **`astronomy.kt`** (Astronomy Engine, Kotlin) | **MIT** (`astronomy.kt:2-7`) | **none** |
| Twilight, target alt-az, rise/set/transit | **`astronomy.kt`** | MIT | none |
| Satellites (TLE → TEME → ITRF → topocentric) | **Orekit 13.1.9 + Hipparchus 4.0.3** | **Apache-2.0** | **`tai-utc.dat`, 3,321 B** |

**Measured status (`N` §7).** Only the **Sun** path is numerically **verified** — to **0.0248 arcmin**
against a JPL Horizons row. The **Moon** path, the **planets** and the **`defineStar`** path are **present
but not exercised → `UNVERIFIED` end-to-end**, and **no UI number may rest on them until they are
checked** (check: run each against a handful of Horizons rows and record the arcminute error, exactly as
`AstroCorpus.kt` does for the Sun). **`astronomy.kt` contains no Messier/NGC catalogue, no magnitude
table and no Bortle/SQM code** (MEASURED by `Select-String`), so the catalogue and any darkness
reporting are the slot's own. **Orekit** builds and shrinks under **R8 full mode with zero keep rules**
(220/249 `org.orekit.*` and 49/59 `org.hipparchus.*` classes survived; one non-fatal R8 warning about
`META-INF/services/com.sun.source.util.Plugin`), runs on the test device `R58MB3SGZVL`, APK
**4,379,153 B = 4.176 MiB**, sha256 `39FAE799…E25A`. **75.7 % of that payload is Orekit/Hipparchus
*assets*, not code** — trimming the bundled data is the single largest size lever. These packaging
figures were measured on the smoke's vendored **13.1.5** jars; with the declared pin at **13.1.9** they
are re-taken at implementation.

### 7.8 Open items — contracts that could not be settled (`N` §8)

**These are carried forward unresolved.** Each names the exact check that settles it; none may be
resolved by assumption. (The brief that commissioned this document referred to *"the three contracts"*
of `N` §8; `N` §8 in fact numbers **eight** items, and **all eight are carried here** rather than
dropping any.)

1. **CelesTrak's licence for a paid app.** The usage policy is a bandwidth policy; it states the data
   are *"freely available to all users"* and **neither forbids nor grants** commercial use; **no SPDX
   licence text was found**. *Check:* read the licence/terms footer of `https://celestrak.org/` and
   `gp-data-formats.php` for an explicit grant or commercial clause; if none, ask
   **TS.Kelso@celestrak.org** (the address the policy publishes) and record the answer.
2. **The real-world accuracy of a computed pass time.** Fidelity is measured (≤ 1e-4 km) and divergence
   at 21.3 days is measured (27,924 km), but the honest number for a **fresh** element set was not
   measured. *Check:* on one night, compare the app's computed AOS for the ISS with an independently
   observed or third-party-predicted AOS at the same site, recording the element-set epoch used.
3. **The Moon's (and planets') numerical accuracy from `astronomy.kt`.** Only the Sun is verified
   (0.0248 arcmin). *Check:* run `searchRiseSet(Body.Moon, …)` and `illumination(Body.Moon, …)` against a
   handful of JPL Horizons rows and record the arcminute error.
4. **The legal status of a VIIRS-radiance → mag/arcsec² conversion.** Implementing a published method is
   not redistributing the CC BY-NC raster, but that is a legal opinion, not a measurement. *Check:* a
   licence review of the specific model and output before any mag/arcsec² number is shown.
5. **`astronomy.kt` exposes `searchHourAngle` — CLOSED 2026-10-08, and it exists** at
   `astronomy.kt:6363` (so it may replace the transit scan). The item's original check was
   `Select-String -Path astronomy.kt -Pattern 'fun searchHourAngle'`.
6. **The commercial status of the Yale BSC5** (should a brighter star list than FK6 be wanted). CDS
   `V/50/ReadMe` carries no copyright notice (MEASURED); shipping it is a decision to rely on an
   unlicensed-by-default catalogue and must be recorded as such.
7. **An inconsistency to resolve, not average:** the recon's `I` file states FK6 is **4,150** stars; the
   live TAP `SELECT COUNT(*)` returned **4,151**. **The live measurement governs — use 4,151** — and the
   row is recorded so the two numbers do not silently disagree.
8. **A measured defect in the planned star source:** FK6 (CC0) **does not contain Sirius, Arcturus,
   Capella or Betelgeuse** (brightest entry `alpha Car` at `vmag = −0.62`; range −0.62 … 9.71), so a
   **hand-authored supplement of the brightest stars** (two-source verified, per the §7.1 procedure) is
   required, or the scene will omit the sky's most recognisable stars. *Check:*
   `SELECT comname, vmag FROM fk6.fk6join ORDER BY vmag ASC`.

**Adjacent open items carried from the other reports** (also unresolved, with their checks): whether the
port precesses a J2000 `defineStar` to the date (L §3.2 / E28 — the residual would be ~0.36° by 2026,
`INFERRED` 50.3″/yr × 26 yr); whether Orekit ships an OMM parser yielding a propagator (L §3.3) —
**CLOSED 2026-10-08: yes**, `javap -public` over `orekit-13.1.9.jar` shows `OmmParser` → `Omm`
(`generateTLE()`, `generateKeplerianOrbit()`);
whether the reminder survives a device reboot and with which `android:exported` value (L §3.4 / E109);
the real SGP4 timing-error-versus-age curve that would replace C-2's product thresholds (L §3.5); the
first and last date of the year on which Reine has a dark window (L §3.6); whether the AGSL and LUT sky
paths agree (L §3.8 / O §8.2); the licence of the orbital-elements feed **as a paid commercial use**
(M §2.3 — the same question as item 1); the install-rate effect of in-app updates on a paid planner
(M §1.6); `LocalActivity` availability in the slot's `activity-compose` version (M §1.6); whether the
in-app review card appears for an app installed from a non-Play source (M §1.6); whether **update cadence
is a discoverability factor** (M §5.1 — no Google page found); whether FK6 carries `B−V` for all stars
(O §3.7); and whether the ESO `eso0932a` source is a Hammer–Aitoff projection (O §4.1).

---

## 8. The rendering contract

### 8.1 The layer stack (`O` §1.1, MEASURED from the canvas and the H/J inputs)

Drawn into one Compose `Canvas` inside a `Box`, bottom painted first. "Owns which pixels" is the
invariant each layer is responsible for and the §8 acceptance items test.

| # | Layer | Renderer | Owns |
|---|---|---|---|
| **L1** | Sky field | fallback: one `ImageBitmap` strip `1 × Hpx` drawn 1:1 · API 33+: AGSL `RuntimeShader` | every pixel of the scene rect, initially |
| **L2** | Night floor (airglow + zodiacal + integrated starlight) | additive `Brush.verticalGradient` / a shader addend | added to L1 **only where the sky is dark** |
| **L3** | Milky Way composite | `drawVertices` over a 48×24 quad mesh with an `ImageShader` | the band and only the band |
| **L4** | Star field | ≤ 8 batched `drawPoints`, `BlendMode.Plus`, one sprite per (magnitude bin × colour bucket) | the sprite footprints only |
| **L5** | Deep-sky targets + constellation figures | `Path` per object; IAU polylines | marker strokes only |
| **L6** | Moon | `drawImage` clipped to its disc + a phase `Path` + a radial-gradient glow | the moon disc + glow radius |
| **L7** | Satellite trail | one `Path` with a fading stroke, split at horizon crossings | the trail's own stroke |
| **L8** | Earth-shadow wedge | **not an overlay** — a modulation **inside** L1's evaluation | changes L1's value; never draws on top of L4 |
| **L9** | Alpenglow band | one additive `Brush.radialGradient`, clipped to `h > α_ridge − 8 px` | the warm backscatter over the ridge |
| **L10** | Horizon silhouette | one `Path` (the ridge) + optional tree glyphs | everything below the ridge; **opaque** |
| **L11** | Lens FOV frame | 4 corner ticks + a hairline rect | its own strokes |
| **L12** | Target marker + label | marker `Path` (occluded by L10) + label in the overlay plane | marker + label |
| **L13** | UI chrome | Compose `MaterialTheme` surfaces | its own bounds |

**Three load-bearing consequences (`O` §1.1).**
1. **L8 is inside L1, not above it.** The Earth's shadow dims **scattered sunlight**, not starlight; a
   translucent dark wedge over L4 would extinguish the stars in the antisolar sky — the darkest, best
   field. *This is the one place the approved mockup's literal SVG order is wrong for real rendering and
   is corrected* (MEASURED — the mockup paints the wedge after the star `<g>` at
   `lumintra-night-scene.canvas.tsx:260-268`).
2. **L10 occludes by draw order and nothing else** (no masking, no stencil, no depth).
3. **L12 splits:** the marker is drawn **before** L10 (so the ridge occludes it); the label **after** L10
   with a clamping rule.

### 8.2 The LUT — dimensions, storage, colour-space discipline (`O` §2)

| Axis | Quantity | Range | Step | Samples |
|---|---|---|---|---|
| **A1** | solar altitude `ε` | **+10.0° … −20.0°** | **0.5°** | **61** |
| **A2** | view elevation `h` | **0.0° … 90.0°** | **1.0°** | **91** |

**Two tables, not one** — the antisolar bands and the Earth-shadow wedge are **azimuth-dependent**, and a
(ε, h) table has no azimuth axis. The baker emits **`LUT_A` (antisolar meridian, Δaz = 180° — the
fallback path's entire sky)** and **`LUT_B` (solar meridian, Δaz = 0° — API 33+ only)**, blended by
`w(Δaz) = (1 + cos Δaz)/2` in **linear light**. **The fallback uses `LUT_A` alone** (`w = 0`) plus a
separate additive sun-glow gradient — which is what the approved mockup already does (MEASURED — a
single `linearGradient` with four stops at `lumintra-night-scene.canvas.tsx:224-228`).

**Value per cell (`O` §2.2):** store **`L_rel = L_abs / E(ε)`**, where `E(ε)` is the zenith radiance at
that solar altitude. Within one column the cells span roughly 10⁻³…3, which **float16** holds. Store
**`E(ε)` beside the table** as a **61-entry `float32` array**, and the RGB triple as three **float16**
values, linear light, D65, no alpha. Reconstruction: **`L_abs = L_rel × E(ε)`.** (Rationale: twilight
radiance spans **5–6 orders of magnitude**; a float16's normal minimum is ≈ **6.1 × 10⁻⁵**, so a single
float16 texture cannot hold the range and a single uint8 sRGB texture would band.)

**Colour-space discipline, one stage per line, no stage skipped (`O` §2.2):**

| Stage | Space |
|---|---|
| Bruneton–Neyret integration (the baker) | linear spectral → linear CIE XYZ (D65) |
| LUT cells on disk | **linear sRGB primaries, relative to `E(ε)`, float16** |
| Interpolation (bilinear on A1/A2, then azimuth lerp) | **linear** — the only interpolation that runs on sky colour |
| White balance | **linear**, one fixed 3×3 von Kries matrix, D65 → the display white |
| Tonemap | the **Narkowicz (2015) ACES filmic approximation** `L' = L(2.51L + 0.03)/(L(2.43L + 0.59) + 0.14)` |
| Transfer to code values | **the sRGB piecewise transfer (IEC 61966-2-1)**, not a 2.2 power |
| Dither | **±0.5/255 tiled blue noise**, in the **encoded domain immediately before quantisation** |
| Quantisation | **the only 8-bit step in the entire pipeline** |

On the AGSL path the working space is the destination's (Compose's canvas is sRGB), so the shader must
call `toLinearSrgb()` on inputs and `fromLinearSrgb()` on the result — **mixing a linear half and an sRGB
half inside one AGSL expression is the named trap**.

**Production and asset (`O` §2.3).** `scripts/bake-sky-lut.py`, at build time, committed: reimplements
**Bruneton & Neyret 2008** (*Precomputed Atmospheric Scattering*, CGF 27(4) 1079–1086, DOI
10.1111/j.1467-8659.2008.01245.x) in NumPy — Rayleigh + Mie multiple scattering **plus the ozone
(Chappuis-band) absorption term** without which twilight reads as *"orange-and-grey apocalypse"* —
evaluated over A1 × A2 in the antisolar and solar planes, writing `assets/sky_lut.bin`. **Working from
the paper (not the reference implementation) makes the licence question not arise.** The script writes
**only inside the repository** (never the system temp directory). It is **reproducible**: the committed
`.bin` carries a header (schema version, A1/A2 bounds and steps, model parameters, the SHA-256 of the
generator's own source), and `verify.mjs` recomputes the expected hash and **fails when the committed
asset and its generator disagree**. **Never computed on first launch** (a Bruneton precompute was **2.0 s
on a 2012 desktop GPU**, MEASURED — a first-launch precompute would also make the picture a function of
the device).

**Asset size (`O` §2.3, computed from the two axes):** 61 × 91 = **5 551 cells** × 3 × 2 (f16) = 33 306
B per LUT × 2 = 66 612 B, + `E(ε)` 61 × f32 (244) + `α_s(ε)` 61 × f32 (244) + a 64-byte header →
**`sky_lut.bin` ≈ 67.2 KB**. (`CONTEXT.md` §5 states the same two tables and ≈ **67 KB**.)

**Interpolation and the two tabulated functions (`O` §2.4–§2.5).** Bilinear in (ε, h) on each LUT, in
float, linear light — not nearest, not bicubic. **`ε` is interpolated, `h` is evaluated** (the strip
path samples one row per device row). The azimuth blend is computed **after** both LUT samples and
**before** the tonemap. **No interpolation is ever done on a gamma-encoded value.** Two scalars are baked
into the same file so no runtime computation is needed: **`α_s(ε)`** (the Earth-shadow edge altitude, 61
floats) and **`E(ε)`** (61 floats). **`Φ(θ)`, the air mass, is not tabulated** — it is evaluated per star
from **Kasten & Young (1989)**, Applied Optics 28(22) 4735–4738:
`Φ(h) = 1 / (cos z + 0.50572 · (96.07995° − z)^(−1.6364))`, `z = 90° − h`, valid down to the horizon.

**Dither is part of the contract, not polish (`O` §2.6).** Two independent mechanisms, neither alone
sufficient: (1) the gradient is **evaluated per output row** (no stretched-bitmap stair-stepping);
(2) **±0.5/255 tiled blue noise** added immediately before 8-bit quantisation, a **64×64 blue-noise
tile** generated once at build time and tiled in **screen space** (so it never crawls during a scrub).
**Bayer/ordered dithering is rejected** because its cross pattern is visible in a smooth twilight
gradient at full-screen scales.

### 8.3 The star formulas (`O` §3, reimplemented from Müller, Engel & Döllner 2012)

- **Stars are viewpoint-aligned billboards carrying a PSF**, batched by magnitude and colour — never
  `GL_POINTS`, never `drawCircle` per star. Compose realisation: **8 magnitude bins × 6 colour buckets =
  48 pre-rendered sprites**, drawn by batched `drawPoints` with `BlendMode.Plus`; the compromise
  (stated so it is a decision, not an accident) is **≤ 8 calls per frame, one per magnitude bin**, with
  the per-star colour supplied by drawing each colour bucket's stars into a small offscreen `ImageBitmap`
  once per sky-rotation change (the raster-cache).
- **Magnitude → brightness:** `Δm(m) = 2.512^(m_a − m)`, control magnitude **`m_a = 4.0`**;
  `I(m,h,t) = c_q · Δm(m) · O(h) · S(h,t) · a_dark(ε)`. `c_q` is **ours** — calibrated so a `V = 6.5`
  star at `h = 60°`, ε = −15° renders at 1/255 above the local sky (the paper's `c_q ≈ 4×10⁻⁷` is in a
  different unit and not transferable). `a_dark(ε) = smoothstep(−2°, −9°, −ε)`.
- **Magnitude → radius:** `r_core(m) = max(√2 px, 0.5 dp)` — **the core radius is a constant for every
  star** (on the A40 at 480 dpi this is `max(1.414, 1.5) = 1.5` device px). The **brightness** channel
  carries the magnitude, not the size; **uniformly scaling size with magnitude is the documented failure
  mode.**
- **The glare halo is the only place a radius grows:** `I_G = max(0, Δm(m) − 1)` (zero exactly at
  `m = m_a`); `r_glare(m) = max(r_core, c_k · √I_G)` with **`c_k = 0.50 px`** (ours, calibrated: Sirius
  `V = −1.46` → `I_G ≈ 151` → `√151 ≈ 12.3` → **≈ 6 px**, the halo radius a 1080-px-wide 73° field
  should show for the brightest star). The calibration is falsifiable (checklist item #8).
- **The PSF:** `P(l) = A_core·T(l) + A_glare·G(l)`, `l = distance / billboard radius`; **`T(l) = 2l³ − 3l²
  + 1`** for `l ∈ [0,1]`, 0 outside (the paper's published intensity PSF, `V_T = 1.167`); **`G(l) = (1 −
  l²)²` is OURS** — the paper's glare profile is ambiguous in the OCR (`G(l) = √64·l`) and *"a profile
  that increases with distance from the centre is not a PSF"*, so a band-limited falloff (value and first
  derivative vanish at `l = 1`) is substituted and **labelled as our substitute, not the paper's formula**.
- **Extinction (dimmer *and* redder toward the horizon):** per channel,
  `O_c(h) = exp(−β_r[c] · (Φ(h) − 1))` with Rayleigh coefficients **β_r = (0.16, 0.37, 0.91)** for sea
  level. At the horizon (`Φ ≈ 38`): `O = (3.5×10⁻³, 3.1×10⁻⁶, 3.3×10⁻¹⁵)` — blue annihilated, red
  survives 150× better than green. **Capped at `h ≥ 1°`** so a star on the ridge does not denormal.
- **Scintillation:** `σ(h) = s_0·(Φ(h) − 1)^1.5`, `S(h,t) = 1 + σ(h)·n_i(t)`, `n_i` a 1-pole low-pass
  with **`τ = 60 ms`**; **`s_0 = 0.12`** calibrated so the amplitude at `h = 5°` is **≥ 3×** that at
  `h = 60°`. A **positional jitter of ±0.15 px** accompanies it. **Twinkle is a time phenomenon and only
  exists while frames are produced** — a still scene has still stars (a battery decision, stated).
- **Colour from temperature:** `B−V → T_eff → Planckian locus (x,y) → CIE xyY → linear sRGB (D65)`, with
  **Ballesteros (2012)** `T_eff = 4600 K · (1/(0.92(B−V)+1.7) + 1/(0.92(B−V)+0.62))` and the locus from
  **Krystek (1985)** (Kim et al. 2002 cubic as fallback). **6 colour buckets** ship, quantised at bake
  time. **Two honest limits:** FK6's `B−V` coverage is **`UNVERIFIED`**; 2MASS carries J/H/K, so its
  colour is **modelled** and its magnitude **mapped** to an approximate V — labelled in-app as an
  approximation, not measured V.
- **Star count:** the catalogue is **never evaluated per pixel**. Build time merges FK6 + 2MASS, bins into
  **0.5-mag bins from V = −1.5 to 6.5 → 16 bins**, writes **11 B/star** (RA, Dec f32 ×2; V f16; colour
  index u8) → **≈ 99 KB at ~9,000 stars**; **proper motion is deliberately not carried**. Per frame, one
  3×3 rotation per frame, then project/cull. The **default 73°×53° field is 1.18 sr = 9.4 % of the sky**,
  so ~9,000 stars to V = 6.5 render as **≈ 850** (item #13 asserts **700–1,100**). The 2MASS faint field
  beyond 6.5 is **not drawn by default** — it appears only when the user narrows the lens FOV.

### 8.4 The Milky Way, the horizon and alpenglow (`O` §4–§5)

**Milky Way (`O` §4).** Asset **ESO `eso0932a`**, CC BY 4.0, credit **`ESO/S. Brunier` mandatory and
visible** (source 6000×3000). **Bake, do not ship raw:** `milkyway.webp` (2048×1024, **≈ 310 KB**),
reprojected to equirectangular galactic `(l, b)` (the source's own projection is **`UNVERIFIED`** — a
Hammer–Aitoff-derived source is the widespread reuse, and the check is stated); **every catalogue star
with `V < 3.0` masked and inpainted** (the paper's instruction, item #15 is the test); a fixed 3×3 box
blur below `V ≈ 8` (a box keeps the Rift's edges crisp); mipmaps. **Placement** is
`M_gal→screen = R_horizontal(φ, LST) · R_gal→ICRS`, with `R_gal→ICRS` the fixed IAU galactic matrix, the
build re-deriving it from the pole (`α = 192.85948°`, `δ = +27.12825°`, `l_Ω = 32.93192°`) and
**asserting agreement with the tabulated matrix to 1 × 10⁻⁶**. **Drawing:** a **48×24 quad mesh (1,152
quads)** through `drawVertices` + an `ImageShader` (a single rotated `drawImage` would distort the band
by several degrees across a 73° field), culled below the horizon and outside the rect. **Opacity:**
`a_mw(ε) = smoothstep(−8°, −14°, −ε)` (item #16 asserts both endpoints). **No doubling:** the bright
stars are masked out **and** the texture and sprites share the same rotation matrix (≤ 1 px landing
error).

**Horizon (`O` §5).** The drawn silhouette is a **per-azimuth altitude array, not a free polyline**
(because the scene's projection is single-valued in azimuth, so occlusion is an `O(1)` array lookup).
Storage — **see C-1: `O` says 360 samples (a `ByteArray(720)` = 360 × `int16` in 0.01°)**, and the
approved mockup's 13-vertex polyline resamples into this array. Rendering: project the 360 points, build
one closed `Path`, fill **opaque** with a 2-stop vertical gradient (`#04060C` → `#01030A`); optional tree
glyphs are **part of the same path**. **Occlusion per class:** a star is culled by one array read before
batching; a deep-sky marker before L10 and its **label after L10, clamped** (prefixed `↓` if below the
ridge); a low Moon is **cut** with a clip layer, not culled; the satellite trail is **split at every
horizon crossing** (bisection, ≤ 8 iterations, ≤ 12 segments); **the FOV frame is not occluded** (a
viewfinder a hill cuts in half is a bug).

**Earth-shadow wedge (`O` §5.4).** Derived from geometry: along a ray at elevation `α` in the antisolar
plane, the shadow depth is `t₂(α, ε) = R·(1 − cos ε)/sin(α − ε)` for `α > ε`; the visible edge is where
`t₂ = H = 20 km`:

```
α_s(ε) = 90°                                  if (R/(2H))·ε_rad² ≥ 1
       = ε + arcsin( (R/(2H))·ε_rad² )        otherwise
       = ε                                    (floor — never sinks below ε)
```

Numerically with **R = 6371 km, H = 20 km**: `α_s = 4.0°` at ε = −1°, `13.2°` at −2°, `28.9°` at −3°,
`54.9°` at −4°, **above the zenith from ε ≈ −5°** — the edge rises **≈ 17° per 1° of Sun motion** over
−1° → −4°. **Applied inside L1**, not as an overlay: for `Δaz > 90°` and `h < α_s(ε)` the sample comes
from a **shadow column of `LUT_A`**, and the effect is a **blue-shifted darkening** (the "blue band"),
not a neutral one. **The Belt of Venus** sits immediately above `α_s` (present from ε ≈ −6°, redder than
blue from ε ≈ −2°; width shrinking **8° → 0.9°**), **baked into `LUT_A`**, not a runtime overlay.

**Alpenglow over the ridge (`O` §5.5).** The Belt of Venus **is** twilight alpenglow (same source, so it
must not be drawn twice): the Belt is the **antisolar** band (baked into `LUT_A`); alpenglow is the same
backscatter **toward the Sun**, brightest there, and **feathered over** the silhouette's top edge:
`A_alp(ε, az, y) = A_peak · f(ε) · (0.35 + 0.65·(1 + cos(az − az_sun))/2) · K(y)`,
`f(ε) = exp(−((ε + 1.0°)/4.0°)²)` (peaks at ε = −1°, ≤ 25 % of peak by ε = −8°), `K(y) = 1` above the
ridge and `(1 − d/8px)²` below the ridge top. Drawn **before** L10, clipped at `ridge − 8 px`. **It is a
`Brush.radialGradient`, deliberately — not a blur:** the mockup uses an SVG `feGaussianBlur` and the app
must not, because `Modifier.blur`/`RenderEffect` is documented for **API 31+** (**`UNVERIFIED`** against
the pinned BOM; check: run a blur on the A40 and observe) and a full-screen gaussian is the fill-rate
trap. **Every soft edge is a gradient, never a blur.**

### 8.5 The timeline, the scrub and the frame budget (`O` §6–§7)

**Timeline strip (`O` §6).** Its colour at any position is **the computed sky at that moment** — the
palette **is** the light. `strip(i) = sRGB(tonemap(E(ε_i)·LUT_A(ε_i, h = 12°)))` for **96 stops**
(0…95) at the **antisolar** meridian, `h = 12°`; the same tonemap, white balance and transfer as L1 (one
sky-colour function, two call sites). It is **baked once per (place, date)** into a 96 × 1 `ImageBitmap`
and stretched horizontally; **the scrub does not regenerate it** — it moves a thumb. Regeneration is
**≈ 3 µs**. **Phase labels are text, never keyframes** (the −6/−12/−18° thresholds as labels).

**The scrub (`O` §7).** `SceneKey` (place, epoch, fov, target, horizon) is rare/structural and lives in
`mutableStateOf`; **`scrub` is a `MutableFloatState` read only inside `Canvas {}` / `drawBehind`**, so a
drag invalidates **only the draw phase** (no recomposition, no relayout) — the documented Compose phases
behaviour and *"the difference between the published 238-recomposition case study and a 60 fps drag."*
Everything downstream is a pure function `SkySnapshot = Sky.ephemeris(key, scrub)` (unit-testable
without a GPU); `derivedStateOf` is used **only to coarsen** (which phase label). **All heavy drawing
objects are built in `drawWithCache`; no allocation happens inside the draw lambda.** **The drag is not
eased** (the thumb tracks the finger 1:1; easing is for release/programmatic change) — an eased drag
makes the sky lag the finger and stops feeling physical. Easing only on release (**≤ 200 ms** settle)
and tap-to-advance. **Per-frame vs imperative (`O` §7.3):** ephemeris, star rotate+cull and the 1 × `Hpx`
strip rebuild are **per frame**; the star colour-bucket raster and the MW mesh rebuild only when the sky
has rotated **> 0.25°**; the timeline strip **only on place/date change**.

**The repaint trigger and battery (`O` §7.4).** **The trigger is the input, not a frame clock** — no
`withFrameNanos` loop, no `rememberInfiniteTransition`. A frame is produced when scrub, `SceneKey`,
orientation change, or a bounded settle. **An idle scene produces zero frames** (item #29 asserts it
against `dumpsys gfxinfo`, not the source). The battery numbers — %/hour for a scrub vs idle — are
**`UNVERIFIED`** (§8.6 R8 names the command).

**The frame budget, named against the device (`O` §7.5) — these are targets, not measurements.**

| Quantity | Value |
|---|---|
| Panel refresh | **60 Hz → 16.67 ms** |
| **Budget for a scrub frame** | **≤ 12 ms** (CPU + GPU), leaving 4.7 ms headroom |
| **Target p95 frame time** | **≤ 16.7 ms** |
| **Target p99 frame time** | **≤ 33 ms** |
| **Target jank rate** | **< 1 %** of frames over 16.7 ms in a 30 s scripted drag |
| **Idle frame production** | **0 frames/s** |
| **First-frame latency after a drag begins** | **≤ 33 ms** |

`CONTEXT.md` §5 states the **≤ 12 ms** target the same way: *"an inferred target, and 'it is smooth on
the A40' stays `UNVERIFIED` until `dumpsys gfxinfo` measures it."*

### 8.6 The AGSL enhancement, the risk register, and the acceptance checklist

**The AGSL relationship (`O` §8).** AGSL is **additive, never load-bearing**; the A40 (API 30) never
executes it, so **the LUT fallback is the product on the factory's own test phone**. Both paths sample
`sky_lut.bin` (one source of truth), and `tonemapACES`, `whiteBalance`, `srgbEncode`, `dither` are each
declared once in Kotlin and once in AGSL with a unit test transcribing both to one reference. **Colour
drift between the two paths is prevented by four mechanisms**, the strongest being a **debug golden-image
test**: the debug build renders the sky twice for the same input (CPU at every row of a test grid, and
the shader) and **fails if any channel differs by more than 2/255** over **256 elevations × 61 sun
altitudes** (item #35). Below API 33 the app selects the fallback at construction and never references
`RuntimeShader` — no reflection, no `Class.forName`, no try/catch used as a version check (item #39
asserts it on the A40's logcat).

**The performance and battery risk register (`O` §10) — none of it is a measurement for this scene on
this phone.** Ranked, with the command that settles each: **R1** the A40 never runs the shader, so the
fallback carries 100 % of the load (check: `getprop ro.build.version.sdk` → 30); **R2** overdraw (5
full-screen layers; check `dumpsys gfxinfo … framestats`); **R3** per-frame allocation → GC jank (check
`framestats` + `logcat -s Choreographer`); **R4** the 1 × `Hpx` strip rebuild every frame (the fallback
to a 256-row strip is a one-constant change); **R5** the star layer is CPU-bound (`drawPoints` is
Skia-side work); **R6** the MW mesh (1,152 textured quads); **R7** blur as a soft edge (forbidden);
**R8** battery (`dumpsys batterystats --charged` before/after a 5-min scrub and a 5-min idle); **R9**
thermal throttling (`dumpsys thermalservice`); **R10** colour drift between the two paths.

**The claim gate (`O` §10.1):** *"it is smooth on the A40"* may be said **only** when all four of the
following have been run on serial `R58MB3SGZVL` against the **debug APK from the revision under test**
(the device-testing rule's artifact rule) and the readings printed with the claim — `getprop
ro.product.model` (expect `SM-A405FM`), `getprop ro.build.version.sdk` (expect `30`), `dumpsys gfxinfo …
reset` then `… framestats` during a scripted scrub, the idle frame counter unchanged, and `dumpsys
batterystats --charged`. **Acceptance:** p95 ≤ 16.7 ms, p99 ≤ 33 ms, janky frames < 1 %, idle counter
unchanged (items #29–#31). **And the honest statement that must accompany any such reading:** it is a
debug build on one phone at one temperature with one battery level — evidence about *that revision on
that device*, not a claim that the scene is fast in general.

**The acceptance checklist: 44 items (`O` §9).** Falsifiable, each naming a number, a visible feature or
a measurable property; `[Bn]` maps back to the bar of `J-sky-realism-and-bar.md` §8; the `V` column is
the verification route (Z = the A40 by eye, S = screenshot + measurement, U = unit test, D = `adb`, P =
`visual-qa.py`). The items and their subjects:

1. No visible banding (`[B1]`, S) · 2. Brightest band above the horizon (`[B2]`, S) · 3. Four antisolar
bands (`[B3]`, Z) · 4. The shadow rises ≈ 8× the Sun (`[B4]`, S) · 5. Purple is present (`[B5]`, S) ·
6. Pale, not a cartoon (`[B6]`, S) · 7. Continuous, no keyframe (`[B17]`, S) · 8. Stars, not dots
(`[B7]`, S) · 9. The minimum radius is enforced (`[B7]`, U) · 10. Twinkle grows toward the horizon
(`[B8]`, S) · 11. Extinction reddens and dims (`[B9]`, S) · 12. Colour from `B−V` (`[B7]`, U) · 13. A
realistic star count 700–1,100 (`[B7]`, S) · 14. The Milky Way is a structured band with a rift
(`[B10]`, S) · 15. No doubled bright stars (`[B11]`, S) · 16. The band follows the darkness
(`[B16 partial]`, S) · 17. Hard occlusion (`[B12]`, S) · 18. The Earth-shadow wedge is geometry-driven
(`[B4]`, S) · 19. Warm alpenglow over the ridge (`[B13]`, S) · 20. Refraction lifts objects at the
horizon (`[B14 partial]`, U) · 21. The Moon has a phase and earthshine (`[B14]`, S) · 22. The Moon glows
(`[B14]`, S) · 23. The Moon is deliberately enlarged 2.0×–3.0× the true 0.518° (`[B14]`, S) · 24.
Satellites are trails, not dots (`[B15]`, Z) · 25. Targets are where the ephemeris says (`[B15
partial]`, U) · 26. The FOV frame is a frame, fixed to the sky (S) · 27. Astronomical darkness is not
black (`[B16]`, S) · 28. Reversible, bit-identical (`[B19]`, S) · 29. Idle produces zero frames (D) ·
30. The scrub is smooth on the A40 (`[B18]`, D) · 31. A scrub costs little battery (D) · 32. The app's
palette is the LUT's palette (`[B20]`, U) · 33. The pack's palette is the app's palette (`[B20]`, P) ·
34. The two sky paths agree (U/D) · 35. Every asset carries its licence (U) · 36. The mandatory ESO
credit is visible (Z/P) · 37. No copyleft asset is in the bundle (U) · 38. Below API 33 nothing is
missing (D) · 39. The shader is additive (U) · 40. The dither is doing its job (S) · 41. No 8-bit value
exists before the tonemap (U) · 42. The gradient is evaluated, not stretched (U) · 43. The strip's
colours are the same function (U) · 44. Localisation survives the hero screen (U).

**Count: 44 items.** Items 26 and 31–44 are the contract's own additions (the FOV frame, the battery
reading, the licence set, the two-path agreement, the pipeline's colour discipline, the hero screen's
localisation) — none of which the B1–B20 bar covers.

---

## 9. The claim ledger

Consolidated `CLAIM / LABEL / EVIDENCE` over the whole document. `MEASURED` rows name the command or
`path:line` from the source report; where a report read a file this consolidation did not re-read, the
row still carries the source report's `path:line` and the file it was read from. **Rows the source
reports carried as `UNVERIFIED` remain `UNVERIFIED` here.**

```text
CLAIM / LABEL / EVIDENCE

--- §1 The competitor matrix ---
The light-planning shelf's paid anchors are PhotoPills $10.99/100K+/4.8(8.22K), Sun Surveyor $9.99/100K+/4.8(5.14K), Sun Position PRO $9.99/50K+/4.9(2.73K), standalone purchase-only with zero ads / MEASURED / K §1.1 listings + M §5.0 (positioning E-money-repositioning.md §1.4/§4, re-verified there by enrich-market.mjs)
Every 10M+ stargazing app and 50M+ Sky Map are LIVE identification apps; the largest pure-PLANNING app is 100K+, a factor of ≈500× smaller / MEASURED / K §1.2 (market record §2.1; B-stargazing-shelf.md §2–§3, 25 listing descriptions)
TPE for Android was available 2011–2021 and is retired, not purchasable / ON RECORD + MEASURED / K §1.1; photoephemeris.com help page ("now withdrawn from sale and officially retired")
Cloud cover is absent from the light-planning shelf's own descriptions / MEASURED / K §1.4 (market record §3(d) keyword scan of 10 descriptions)
The astronomy-weather apps that show cloud carry the shelf's weakest ratings (Clear Outside 3.8, Good To Stargaze 3.9) / MEASURED / K §1.1/§1.4 listings
ISS Detector Pro is $17.99 — the highest observed paid price in the recon / MEASURED / K §1.3 (price from market record §4.1)
AstroSat forecasts satellite transits through a photographer's exact FoV using optics, camera, exposure length and session time, and names landscape/astro-photographers as users / MEASURED / K §1.3 (astrosat.org + github.com/james-m-osborn/astrosat, 22 stars + arXiv 2111.11268)
No shipped Android paid app was found bundling satellite-streak forecasting into a light-planner / INFERRED / K §1.3 — the satellite shelf is pass/free-ad apps and streak forecasting appears only as research/web tools (AstroSat, Tonight's Window, PSF Guard); settle by a Play search for "satellite trail planner"
Willingness-to-pay for a combined dark-window + deep-sky + satellite + cloud brief / UNVERIFIED / K Risk 1 (market record §3.2, §10 item 4); settle via Play's full paid-planner review lists and the launch price
Planit Pro's top sampled complaints are that plans and a purchased add-on do not travel between devices; PhotoPills' is paying again on another platform / MEASURED / K §3 (listing review text; market record §3.1)
Sun Surveyor's most recent negative review is a compass-accuracy complaint; Star Walk 2 and Sky Map reviews repeat compass drift / MEASURED / K §3 (listing review text)
A pinned Sky Tonight reviewer states the app's only missing thing is live satellite tracking and they would have paid extra to have everything in one app / MEASURED / K §1.2 (review text on the listing)
A beautiful scene is not a planner; the paid planners win on planner legibility, not a rendered sky / INFERRED / K Risk 2 (the best-designed sky apps are free or ≤$2.99; the LIVE shelf dwarfs planning ≈500×)

--- §2 The adopted additions ---
Export/backup/portability ranks #1 in both independent datasets (661 and 743 mentions) and is the paid product in four commercial apps / ON RECORD / L §1.1 A1 (user-review-depth-signals.md §2–§3)
Feature requests are the second-most-frequent low-star complaint class at 15.13 % (median), and 6.12 % of feature requests were for notifications / ON RECORD / L §1.1 (Khalid et al., IEEE Software 2014, via user-review-depth-signals.md §1.1)
Reminders rank #4 among named extras at 364 requests; "no offline access" is a top-5 ranked complaint at 587 mentions / ON RECORD / L §1.1 (user-review-depth-signals.md §2, bigideasdb / r/iosdev tables)
A widget is a named reason an app survives on the home screen / ON RECORD / L §1.3 A (user-review-depth-signals.md §2 verbatim quote)
The contract has 7 claims and no data_in claim; the connective floor is already met by share-the-session-brief (data_out) and pin-the-session-scout (durability) / MEASURED / L §1.5 (lumintra-light-planner.differentiators.json, claims[].depthFamily)
The six new claims' names, weights and stages are verbatim from CONTEXT.md §3.9 / MEASURED / CONTEXT.md §3.9
The capability assignments and rejected alternatives in §2.2 are this consolidation's judgement / INFERRED / derived from CONTEXT.md §3.3, §3.5, §3.8 and the source reports' feature descriptions

--- §3 What is rejected ---
api.met.no forbids mobile apps from retrieving new data while the app is not in use / MEASURED / L §0.3, N §4.2 (metno-tos.html line 224-226, quoted verbatim)
The Falchi 2016 World Atlas is CC BY-NC and cannot ship in this paid app / MEASURED / N §6.2 (PMC4928945: "Creative Commons Attribution-NonCommercial license … not for commercial advantage")
NOAA/EOG VIIRS Nightlife Lights are CC BY 4.0 but are a radiance product, not a sky-brightness map / MEASURED / N §6.2 (eogdata.mines.edu/products/vnl/)
The adjacent satellite shelf carries a measured 1★ review for being blocked on granting alarms and notifications / MEASURED / L §1.2 (D-complaints-satellite.md:44)
Data safety's "collect" means transmitting data off the device; a weather fetch needs a Location declaration / MEASURED / M §3.1, AGENTS.md (support.google.com/…/10787469, quoted)
In-app updates: API 21+, flexible/immediate, .obb-incompatible, a Data safety row for device metadata/app version/modules; app-update current version is 2.1.0 / MEASURED / M §1.6 (guide/playcore/in-app-updates; Google Maven maven-metadata.xml)
The install-rate benefit of in-app updates on a paid planner / UNVERIFIED / M §1.6; needs a shipped A/B read in Play Console

--- §4 The edge-case register ---
The register contains 111 situations, E01–E111, across 11 groups / MEASURED / L Part 2 ("Total: 111 situations (E01–E111)")
A single long propagate() with ElevationDetector finds 1 AOS where a brute-force scan finds 15, unchanged for maxCheck 600/300/60/10 s / ON RECORD / L §2.0, §4 (F-engine-gap-map.md §4.2 quoting run-passvariants.txt and run-passcrosscheck.txt; reproduced 3×)
The same shape applies to every event search (twilight, moonrise, target rise/set) / INFERRED / L §2.0 (the segmentation requirement applied to the same class of long-window root finding)
CelesTrak ran out of 5-digit catalog numbers on 2026-07-11; new objects are 100000+ and GP data is not available for them in TLE format / MEASURED / L §0.1, N §3.1 (celestrak-usage-php.html "URGENT" banner, quoted verbatim)
The recon's CelesTrak HTTP 500 was the wrong path; the policy page is reachable at /usage-policy.php (HTTP 200, 13,980 bytes) / MEASURED / L §0.1 (Invoke-WebRequest this session → STATUS=200 BYTES=13980)
api.met.no returns 403 for requests with 5+ decimal coordinates and requires truncation to 4 decimals / MEASURED / L §0.3, N §4.1 (metno-tos.html, quoted verbatim)
api.met.no answered byte-identically with and without a User-Agent (two probes, 38,302 bytes each, zero differences) / MEASURED / L §0.3 (Compare-Object difference count 0 over metno-compact-noUA.json / metno-compact-UA.json)
A force-stopped app receives no broadcasts and the reminder is not guaranteed until the app is opened again / ON RECORD / L E110 (platform behaviour, stated as a limitation)
Whether the reminder survives a device reboot, and with which android:exported value / UNVERIFIED / L §3.4, E109; settle by a boot test on the A40
The port's Moon path has never been numerically verified; only the Sun case was, at 0.024758 arcmin / ON RECORD / L §2.2 E21, N §7 (F-engine-gap-map.md §2.1, quoting verbose-astro.txt ASTRO_AGGREGATE cases=4 verified=1 unverified=3)

--- §5 The Play integration facts ---
The review artifact is com.google.android.play:review (+review-ktx), current stable 2.0.2, from Google's Maven repository / MEASURED / M §1.2 (guide/playcore/in-app-review/kotlin-java updated 2026-10-01; maven-metadata.xml <latest>2.0.2</latest>)
The review library declares no permission and no manifest component / MEASURED / M §1.4 (the merged AndroidManifest.xml inside review-2.0.2.aar: <uses-sdk minSdk 21/>, empty <application/>)
A button must not trigger the API; a CTA should redirect to the Play Store instead / MEASURED / M §1.4 C2 (in-app-review page §Quotas, quoted verbatim)
The caller is told nothing — not even whether the dialog was shown; do not change app flow or inform the user / MEASURED / M §1.4 C3 (kotlin-java page, "Important" note)
No question may precede or accompany the prompt; the card must be surfaced as-is; no incentivised or gated reviews / MEASURED / M §1.4 C4–C6 (in-app-review §When to request, §Design guidelines; support.google.com/…/9898684)
The review card requires the Play Store app present; the KTX extensions are requestReview()/launchReview(activity, info), both suspend / MEASURED / M §1.2–§1.4 (constant-pool read of ReviewManagerKtxKt.class)
The listing intent is ACTION_VIEW + https://play.google.com/store/apps/details?id=<pkg> + setPackage("com.android.vending") / MEASURED / M §1.9 (distribute/marketing-tools/linking-to-google-play updated 2026-08-07)
Whether the review flow needs a Data safety row is the developer's decision; recommendation is no new row / MEASURED (the page's sentence) + INFERRED (the recommendation) / M §1.5
The honest permission set is INTERNET alone (plus POST_NOTIFICATIONS only if a reminder ships, plus ACCESS_COARSE_LOCATION only if an auto-locate convenience is added) / MEASURED + INFERRED / M §2.1, L §1.2, CONTEXT.md §5
Geocoder requires no location permission / MEASURED / M §2.1 (developer.android.com/reference/android/location/Geocoder — no @RequiresPermission)
POST_NOTIFICATIONS is a dangerous runtime permission introduced in Android 13 (API 33) / MEASURED / M §2.1 (about/versions/13/behavior-changes-all)
Sending a saved coordinate to api.met.no is a collection; MET Norway stores the requesting IP and geocoordinates in its logs / MEASURED (definition + logs) + INFERRED (application) / M §3.1–§3.2
Recommendation: declare the Location→Approximate row normally rather than claiming the ephemeral exemption / INFERRED / M §3.2 (Play's ephemeral standard vs MET's logging statement collide; §3.2 states both readings and the alternative)
A policy saying "we collect nothing" would be false once the forecast ships / INFERRED / M §3.3 (from "collect = transmit" plus the forecast call)
MET Norway's credit is "Data from MET Norway" (CC BY 4.0); ESO's credit "ESO/S. Brunier" is mandatory, unaltered and visible / MEASURED / M §4 (api.met.no/doc/License; eso.org/public/outreach/copyright/)
Core vitals affect Play visibility; thresholds 1.09 % crash / 0.47 % ANR overall, 8 % per model, 28-day average / MEASURED / M §5.1 (developer.android.com/topic/performance/vitals — direct fetch timed out twice; the text came from the search tool quoting the page)
Store-listing experiments: biggest impact from icon/video/screenshots; one asset at a time; ≥1 week / MEASURED / M §5.1 (play.google.com/console/about/store-listing-experiments/)
The slot has no in-app product; the price is a Play Console attribute; billing is deliberately not enabled / MEASURED / M §5.2 (lumintra-light-planner.economics.json capabilities block)
Update cadence is a discoverability factor / UNVERIFIED / M §5.1; no Google page found stating it; check Play Console Help for a "freshness" ranking statement
Whether the in-app review card appears for an app installed from a non-Play source / UNVERIFIED / M ledger; check is a device run on R58MB3SGZVL after the first closed-track release

--- §6 The licence and attribution facts ---
OpenNGC is CC BY-SA 4.0 (README L15, no LICENSE file); NGC 2000.0 is non-commercial; Stellarium DSO is GPL / MEASURED / K §3 R9, N §1.4 (each source's own page, per I's ledger)
HYG is CC BY-SA 4.0 and Hipparcos/Tycho/Gaia are CC BY-NC 3.0 IGO; FK6 is CC0 and 2MASS is freely usable with acknowledgement / MEASURED / K ledger, N §1.4 (each licence read at its own source)
The shared Gaia DR2 colour Milky Way panorama is CC BY-SA 3.0 IGO and cannot ship / MEASURED / K §3 R9, O ledger (the kam-ui credit line, read verbatim)
FK6 (GAVO) is CC0 1.0; the cone-service form carries the rights block / MEASURED / N §9, O ledger (dc.g-vo.org/fk6/q/cone/form)
FK6 has 4,151 rows and vmag range −0.62 .. 9.71; the recon's I file's 4,150 disagrees and the live measurement governs / MEASURED / N §8.7 (TAP SELECT COUNT(*) → 4151; SELECT MIN(vmag),MAX(vmag) → −0.62,9.71)
FK6 does not contain Sirius, Arcturus, Capella or Betelgeuse; its brightest entry is alpha Car at −0.62 / MEASURED / N §1.3, §8.8 (TAP brightest list + comname LIKE '%CMa%' + hipno IN (32349,69673,24608,27989))
Whether FK6 carries B−V for all stars / UNVERIFIED / O §3.7; check the GAVO FK6 cone-service column list and count non-null B−V
ESO eso0932a is CC BY 4.0 with a mandatory visible credit and is 6000×3000 / MEASURED / N §1.4, O §4.1 (eso.org/public/images/eso0932a/, eso.org/public/outreach/copyright/)
The ESO source's own map projection is Hammer–Aitoff-derived / UNVERIFIED / O §4.1 (the kam-ui note says "Hammer → equirectangular reprojection" but eso0932a's own page was not read for its projection)
2MASS requires a verbatim acknowledgement and states no commercial restriction, but an explicit "public domain" sentence was not found / MEASURED + UNVERIFIED / N §1.4, O ledger (irsa.ipac.caltech.edu/Missions/2mass.html)
IAU constellation figures and WGSN star names are CC BY 4.0 / MEASURED / N §1.7, M §4 (iau.org + the IAU Copyright page)
Wikidata structured data is CC0 1.0; NASA HEASARC is freely usable / MEASURED / N §1.4
Solar System Scope textures are CC BY 4.0 with commercial use explicitly permitted / MEASURED / N §1.4, M §4 (solarsystemscope.com/textures/)
NASA SVS content is public domain unless noted; the NASA insignia is not, and no endorsement may be implied / MEASURED / M §4, O ledger (svs.gsfc.nasa.gov/help/; nasa.gov/nasa-brand-center/images-and-media/)
dcf21/constellation_lines_iau.dat has a GPL v3 repo footer vs a CC BY 4.0 file description — a measured conflict, so it is not licence-clean / MEASURED / N §1.7
The commercial status of the Yale BSC5 / UNVERIFIED / N §8.6 (CDS V/50/ReadMe carries no copyright notice, measured; shipping it relies on an unlicensed-by-default catalogue)
Orekit 13.1.9 + Hipparchus 4.0.3 are Apache-2.0; astronomy.kt is MIT / MEASURED / N §7 (astronomy.kt:2-7)

--- §7 The data and algorithm contracts ---
The object table is two authored JSON files (~220 rows) plus a computed solar-system section (10 records), ~230 total, ~250 with the magV extension rule / MEASURED (schema) + INFERRED (the count rule) / N §1.1, §1.5
astronomy.kt horizon() takes RA in hours (multiplies by 15); the table stores RA in degrees, converted at exactly one boundary / MEASURED / N §1.3 (astronomy.kt:5400-5413 sinra = dsin(ra * 15.0); defineStar at :452)
astronomy.kt Observer is lat N+/S−, lon E+/W− (degrees), height metres / MEASURED / N §1.3 (astronomy.kt:1412-1431)
Civil/nautical/astronomical twilight are geometric Sun-centre at −6/−12/−18°; sunrise/sunset is 90.8333° zenith (50′ = 16′ semidiameter + 34′ refraction) / MEASURED / N §2.2 (aa.usno.navy.mil/faq/RST_defs, quoted verbatim)
astronomy.kt implements Saemundsson refraction (1.02/tan(h+10.3/(h+5.11)))/60, clamped to −1°, tapering to zero at the nadir / MEASURED / N §2.5 (astronomy.kt:4015-4031)
astronomy.kt moonPhase returns ecliptic elongation in [0,360) deg; illumination() gives phaseAngle (deg) and phaseFraction [0,1] / MEASURED / N §2.4 (astronomy.kt:6173, :2913-2920)
searchAltitude is centre-of-body (no radius/dip/refraction); searchRiseSet includes them / MEASURED / N §2.5 (astronomy.kt:6911-6918 bodyRadiusAu=0.0 vs :6818-6845)
The port precesses a J2000 defineStar to the date / UNVERIFIED / L §3.2, E28; a J2000-as-of-date error is ~0.36° by 2026 (INFERRED, 50.3″/yr × 26 yr); check M31 against JPL Horizons / Stellarium and read defineStar/horizon in astronomy.kt
CelesTrak GP JSON is keyless and reachable (HTTP 200, 9,270 B, 17 OMM records for GROUP=STATIONS) / MEASURED / N §0, §3.1
CelesTrak asks for one download per update; GP updates every 2 hours; M2M software stops on any non-200 and does not retry / MEASURED / L §0.2, N §3.1 (celestrak-usage-php.html, quoted verbatim)
CelesTrak states no SPDX licence and no commercial grant in the pages read / MEASURED / L §0.1, N §3.1
Whether a paid app is affirmatively licensed by CelesTrak / UNVERIFIED / N §8.1; read the celestrak.org terms footer or ask TS.Kelso@celestrak.org
Orekit's TLE field ctor takes meanMotion in rad/s and angles in rad; OMM MEAN_MOTION (rev/day) must be converted n[rad/s] = MEAN_MOTION*2π/86400 / MEASURED (javadoc units) + INFERRED (the conversion) / N §3.2
TLE→TEME→ITRF→topocentric runs with 3,321 bytes of data (tai-utc.dat), simpleEOP=true / MEASURED / N §3.3 (run-tinydata.txt)
Orekit refuses to run with no IERS UTC-TAI provider ("no IERS UTC-TAI history data loaded") / MEASURED / N §3.3 (verbose-nodata.txt:981)
Segmenting into 1-hour windows with maxCheck=60 finds all 15 passes where one call found 1 / MEASURED / N §3.5 (run-passcrosscheck.txt + F-map)
Orekit SGP4 matches the reference implementation to 1e-4 km / 1e-7 km/s (29/33 blocks) / MEASURED / N §3.7 (corpus-tinydata.txt)
Stale TLE diverges 27,924.0197880 km at +1,844,000 s (~21.3 days) / MEASURED / N §3.7 (corpus-tinydata.txt CASE 20413#2)
CelesTrak OMM carries no standard magnitude field / MEASURED / N §3.4 (the key list has BSTAR/MEAN_MOTION_DOT/MEAN_MOTION_DDOT and no magnitude key)
Real-world TLE pass-time accuracy (the "few minutes" claim) / UNVERIFIED / N §3.7, §8.2; a same-night AOS comparison vs an independent prediction, recording the element-set epoch
ElevationMask(double[][]) exists on the smoke's vendored Orekit 13.1.5 but no probe exercises it (the reading was taken on 13.1.5; the declared pin is now 13.1.9, so re-check at implementation) / ON RECORD / L E42 (F-engine-gap-map.md §4.1, javap + MEASURED absence in the probe sources)
The satellite staleness thresholds differ between L E36 (>72 h refuse) and N §3.6 (72–168 h badge, >168 h refuse) / MEASURED (the two texts) / L E36 vs N §3.6 — both stated as product choices; the spec must state one table (C-2)
api.met.no requires an identifying User-Agent with contact info; cache + If-Modified-Since and honouring Expires; no background retrieval; 20 req/s ceiling / MEASURED / N §4.2 (metno-tos.html, quoted verbatim)
The compact payload field is properties.timeseries[].data.instant.details.cloud_area_fraction, percent 0–100, total cloud only / MEASURED / N §4.3 (metno-compact-UA.json parsed: type=Feature, 87 points, units "%", value 100.0)
Camera2 exposes SENSOR_INFO_PHYSICAL_SIZE (SizeF, mm) and LENS_INFO_AVAILABLE_FOCAL_LENGTHS (float[], mm) / MEASURED / N §5.1 (javap over android-35/android.jar)
FOV = 2*atan(d/(2f)); FF diagonal 43.2666 mm; 50mm/FF horiz = 39.6 deg; the preset table is the formula's own output / INFERRED / N §5.1–§5.2 (arithmetic on the stated geometric definition)
The Falchi raster is CC BY-NC and must not be bundled; VIIRS VNL is CC BY 4.0 but radiance-only / MEASURED / N §6.2 (PMC4928945; eogdata.mines.edu)
Only the Sun path of astronomy.kt is numerically verified (0.0248 arcmin); the Moon, planets and defineStar are present but unexercised / MEASURED / N §7 (F-engine-gap-map.md)
astronomy.kt has no Messier/NGC catalogue, no magnitude table, no Bortle/SQM code / MEASURED / N §7 (Select-String sweep)
Orekit 13.1.5 + Hipparchus 4.0.3 shrink under R8 full mode with zero keep rules; APK 4,379,153 B; 75.7 % of the payload is assets (measured on the smoke's vendored 13.1.5; the declared pin is now 13.1.9 and the figure is re-taken at implementation) / MEASURED / N §7 (G-android-build-proof.md)
The Moon's (and planets') numerical accuracy from astronomy.kt / UNVERIFIED / N §8.3; run searchRiseSet/illumination against a handful of JPL Horizons rows and record the arcminute error
The legal status of a VIIRS-radiance → mag/arcsec² conversion / UNVERIFIED / N §8.4; a licence review of the specific model and output before any mag/arcsec² number is shown
astronomy.kt exposes searchHourAngle at astronomy.kt:6363 (CLOSED 2026-10-08) / MEASURED / N §8.5 — was UNVERIFIED; the check that settled it: Select-String -Path astronomy.kt -Pattern 'fun searchHourAngle'

--- §8 The rendering contract ---
The mockup paints the Earth-shadow wedge AFTER the star group, which is wrong for real rendering / MEASURED / O §1.1 (lumintra-night-scene.canvas.tsx:260-268)
The mockup's sky is a single vertical linearGradient with four stops and no azimuthal term / MEASURED / O §2.1 (lumintra-night-scene.canvas.tsx:224-228)
Bruneton & Neyret 2008 is Precomputed Atmospheric Scattering, CGF 27(4) 1079–1086, DOI 10.1111/j.1467-8659.2008.01245.x; ozone is why twilight is purple / ON RECORD + MEASURED / O §2.3 (J §1.1–§1.3)
A float16 cell cannot hold a 5–6 order-of-magnitude range, so the LUT is stored relative to a per-column exposure / INFERRED / O §2.2 (float16's normal minimum is 6.1e-5); sky_lut.bin ≈ 67.2 KB
The sRGB transfer is the piecewise IEC 61966-2-1 curve, not a 2.2 power; blue-noise dithering at ±0.5/255 removes banding and is preferred over Bayer / ON RECORD / O §2.2, §2.6 (J §5.2/§7 item 8)
Kasten & Young (1989) air mass Φ(h) = 1/(cos z + 0.50572(96.07995°−z)^−1.6364) / ON RECORD / O §2.5 (the formula's standard citation; NOT re-fetched — the builder must re-read the paper before transcribing)
Müller, Engel & Döllner 2012, Single-Pass Rendering of Day and Night Sky Phenomena, VMV 2012 pp. 55–62, is the star blueprint; the intensity PSF is T(l)=2l³−3l²+1, V_T=1.167; the minimum radius is √2 px; Δm = 2.512^(m_a−m) with m_a=4 / MEASURED / O §3.2–§3.4 (J §2.1, quoting the paper)
The paper's c_q ≈ 4×10⁻⁷ is unit-dependent and not transferable; c_k = 0.50 px and G(l)=(1−l²)² are OURS / INFERRED / O §3.2, §3.4 (calibrations stated and falsifiable through items #8/#12)
Point sprites are unusable at wide FoV because they cluster at the centre; hardware point smoothing is vendor-specific and small primitives stall the pipeline / MEASURED / O §3.1 (J §2.1, quoting the paper and the DLR paper)
Scintillation is proportional to optical path length and grows toward the horizon; extinction uses β_r=(0.16,0.37,0.91) for sea level / MEASURED / O §3.5–§3.6 (J §2.1(d),(e))
Star colour runs B−V → temperature (Ballesteros 2012) → Planckian locus (Krystek 1985) → CIE → sRGB / ON RECORD / O §3.7 (the papers' standard citations; NOT re-fetched — the builder must re-read before transcribing)
The galactic→ICRS matrix quoted in O §4.2 is the standard form from Hipparcos and Tycho Catalogues (ESA 1997) Vol. 1 §1.5.3, deriving from Blaauw et al. (1960), MNRAS 121, 123 / ON RECORD / O §4.2 (the build must re-derive it and assert agreement to 1e-6)
Bright stars must be removed from the star-map texture to avoid doubling / MEASURED / O §4.1 (J §3.2 quoting Müller et al. §4.4)
The antitwilight bands are upper sky / Belt of Venus / blue band (Earth's shadow) / horizon band; the Belt moves ≈8× the Sun and its width shrinks 8°→0.9°; the shadow edge approaches the zenith while ε is in single digits / MEASURED / O §5.4 (J §1.4, §4.2 quoting Lynch, Dearborn & Richtsmeier 2017, Appl. Opt. 56(19) G156–G168)
α_s(ε) = ε + arcsin((R/2H)·ε²) with R=6371 km, H=20 km (4.0° at −1°, 13.2° at −2°, 28.9° at −3°, 54.9° at −4°, above the zenith from −5°) / INFERRED / O §5.4 (derived from the ray–sphere tangency condition; falsified by item #4)
The Belt of Venus is the twilight form of alpenglow / ON RECORD / O §5.5 (J §4.3 quoting Wikipedia "Belt of Venus")
Modifier.blur / RenderEffect is documented for API 31+ / UNVERIFIED / O §5.5 (a fetch of the Compose draw reference timed out; the check is to run a blur on the A40 and observe)
Reading State<Float> in drawBehind/drawWithCache invalidates only the draw phase; passing a plain Float recomposed 238× in the published case study / MEASURED / O §7.1 (J §5.1 quoting the Compose phases guide and a case study)
Material guidance is that the thumb tracks the finger 1:1 and the drag itself is not eased / MEASURED / O §7.2 (J §5.2 quoting the motion guidance)
AGSL is API 33+; the A40 (API 30) never executes it, so the LUT fallback is the product on the test phone / ON RECORD + MEASURED / O §0, §8 (H §3.1 quoting developer.android.com/develop/ui/views/graphics/agsl; device-testing.mdc:30)
FK6 carries B−V for all 4,150 stars / UNVERIFIED / O §3.7 (not read this session; check the GAVO FK6 cone-service column list — and note N measured 4,151, which governs)
No frame-time, thermal or battery measurement exists for this scene on this device / UNVERIFIED / O §7.5, §10.1 (H §9.3 says so; settled by the §10.1 commands on R58MB3SGZVL)
The frame-budget numbers in O §7.5 are targets, not measurements / UNVERIFIED / O §7.5 (no frame-time reading exists; settled by the §10.1 commands)
No code was copied, no shader pasted, no asset downloaded and no file outside .kiln/scratch/ was written by the rendering worker / MEASURED / O ledger
```

**The one honest limit of this consolidation.** It is a **consolidation**, not new research: it re-states
the five reports, preserves their labels, and adds only the four clearly-labelled consolidation findings
of §0 (C-1–C-4) and the capability mapping of §2.2. Where two reports disagree it **records the
disagreement rather than picking a side** (C-1 the horizon sample count, C-2 the staleness thresholds).
It cannot resolve any `UNVERIFIED` row — §7.8 and its adjacent list name the check that would — and it
does **not** claim that the app is buildable, beautiful, correct or wanted; the objective gates are
`scripts/verify.mjs` and the owner's judgement at checkpoint 1.

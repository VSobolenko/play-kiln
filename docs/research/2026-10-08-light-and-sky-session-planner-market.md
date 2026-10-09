# Light-and-sky session planner — consolidated market evidence (2026-10-08)

> **Status, 2026-10-08:** the Orekit pin in this record is now **`org.orekit:orekit:13.1.9`** (latest
> stable; `13.1.5` was the smoke's vendored build). The open question "does Orekit ship an OMM parser
> yielding a propagator?" is **resolved — yes**, measured by `javap -public` over `orekit-13.1.9.jar`
> (`OmmParser` → `Omm`, with `Omm.generateTLE()` and `generateKeplerianOrbit()`). Where this record
> cites a reading taken *from* the 13.1.5 javadoc or *on* the 13.1.5 jars, that attribution is
> deliberate and correct — it records when the reading was taken, not what the slot declares.

This is the durable, committed market-evidence record for the **light-and-sky session planner** idea
(a paid, purchase-only planner that joins a true darkness window, a deep-sky target catalogue,
satellite passes and hourly cloud cover for one location and one date). It consolidates the measured
findings of six recon files that lived only under `.kiln/scratch/positioning-2026-10-08/` — a
**gitignored, disposable** directory — so that the slot's contracts (`economics.json`,
`differentiators.json`) and the coming OpenSpec specification can cite a real path.

It is a **consolidation, not new recon.** Every number below was measured by one of the six source
files (or by a source those files cite), and every claim keeps the label the source gave it. Where two
sources disagree, both readings are printed rather than reconciled. Nothing here was invented, and
anything the six files do not settle is in **§11 Unsettled** rather than filled with a guess.

## 0. Sources, and the exact commands behind them

| Source file (scratch, disposable) | Role |
|---|---|
| `A-astrophotography-baseline.md` | Baseline of the astrophotography-*planning* shelf |
| `B-stargazing-shelf.md` | The general stargazing / planet-viewing shelf, LIVE vs PLANNING |
| `C-sun-moon-light-shelf.md` | **The load-bearing file** — the sun/moon/light-planning photography shelf and the four gaps |
| `D-satellite-aurora-adjacent.md` | Three adjacent audiences (satellite · aurora · dark sky) |
| `E-money-repositioning.md` | Money owner's revision 2: price, shelf, the $17.99 correction |
| `F-engine-gap-map.md` | Engine feasibility for the four gaps |

Commands named by the sources (all run 2026-10-08, repo root `d:\Python\PlayKiln`):

```text
node scripts/enrich-market.mjs --query "sun surveyor" --query "golden hour" ... --top 6 --concurrency 2
node scripts/enrich-market.mjs --query "iss detector" --top 6 --concurrency 2          # the $17.99 correction
node scripts/enrich-market.mjs --query "satellite tracker" --top 6 --concurrency 2     # the window that hid it
node scripts/scan-titles.mjs --query "astrophotography planner" ...
node scripts/scan-complaints.mjs --package com.ratana.sunsurveyor ...
# store descriptions: each incumbent's Play page, data-g-id="description"; reviews: scripts/lib/play-reviews.mjs
```

Independent verification that already exists in the repo: `docs/research/2026-10-08-finalists-verification.md`
§B re-ran `enrich-market.mjs` against this shelf and **CONFIRMED** PhotoPills `$10.99 / 100K+ / 4.8 / 8,220`,
Sun Surveyor `$9.99 / 100K+ / 4.8 / 5,140`, Planit Pro `$9.99 / 10K+ / 4.6 / 1,220`,
SkySafari `$6.99 / 10K+ / 4.3 / 288`. That is a durable second reading of the load-bearing price rows.

---

## 1. What was screened against, and confirmation the job is not already taken

Screened to ensure this is a new job (`.cursor/rules/ideation-gate.mdc` § "The factory is not the market"):

- **Not in our own catalogue.** `registry/apps.json` identities are `smoke`, `pipe-proof`,
  `pipe-sizing` (killed), `teacher-gradebook`, `lawn-round-log`, `laneset`, `unit-walk` — none is a
  sky/light/astro planner. **MEASURED** — `registry/apps.json`.
- **Not on the killed list.** `docs/research/2026-09-24-market-decision.md` §3 kills handloading data,
  process-server-log, dog-groomer appointment book, field-service work order, stuttering practice,
  dementia caregiver log, farrier hoof record, livestock breeding record, machinist feeds-and-speeds,
  safety/forklift inspection checklists, the collection/life-admin/food-craft/outdoors groups,
  `coin collection catalog`, and `pilot logbook`. **None is a sky/light planner.** **MEASURED** —
  `docs/research/2026-09-24-market-decision.md:376`.
- **Not a dead zone.** The one measured dead zone in `docs/niche-selection.md` is `bjj training log`
  ("about two thirds of top results show no rating at all"). The light-planning shelf is the opposite:
  its incumbents carry large rating counts (PhotoPills 8,220; Sun Surveyor 5,140). **MEASURED** —
  `docs/niche-selection.md:47`; rating counts from §3 below. **INFERRED** — that a rated, paying shelf
  is not the "nobody got traction" shape `niche-selection.md:56` rejects.

**Conclusion:** the job is not taken by us and not on the killed or dead-zone lists.

---

## 2. The shelf that carries the money: paid light-planning incumbents

The load-bearing shelf is the **sun / moon / light-planning photography shelf** (landscape & outdoor
photographers). Paid, one-time, standalone listings — all **MEASURED** by `enrich-market.mjs --top 6
--concurrency 2`, source `C-sun-moon-light-shelf.md` §1.1:

| App | package | Price (one-time) | Installs | Rating (reviews) | Ads / IAP |
|---|---|---:|---:|---:|---|
| **PhotoPills** | `com.photopills.android.photopills` | **$10.99** | 100K+ | 4.8 (8,220) | false / false |
| **Sun Surveyor (Sun & Moon)** | `com.ratana.sunsurveyor` | **$9.99** | 100K+ | 4.8 (5,140) | false / false |
| **Sun Position PRO** | `com.andymstone.sunposition` | **$9.99** | 50K+ | 4.9 (2,730) | false / false |
| **Planit Pro: Photo Planner** | `com.yingwen.photographertoolspro` | **$9.99** | 10K+ | 4.6 (1,220) | false / true |
| **Sun Seeker: Sunlight Tracker** | `com.ajnaware.sunseeker` | **$8.99** | 50K+ | 4.8 (1,000) | false / false |
| **SunOnTrack: Sun Path & Shadows** | `com.sunnytapps.sunnytrack` | **$7.99** | 50K+ | 4.8 (874) | false / false |
| **Golden Hour+** | `com.simplaapliko.goldenhourpro` | **$4.99** | 1K+ | 4.7 (189) | false / false |

**The paid traffic leaders are free/IAP** (**MEASURED**, `C-sun-moon-light-shelf.md` §1.2): Sun Surveyor
Lite 1M+ (4.1, 3,980), Sun Position (free) 1M+ (4.6, 12,500, IAP), Sunset & Sunrise/PhotoTime
`pl.guteklabs.phototime` 500K+ (4.3, 9,250, IAP), Golden Hour 500K+ (4.7, 5,370, ads), Sun Ephemeris
100K+ (4.2, 3,590), `com.vitotechnology.Ephemeris` 100K+ (3.6, 1,390, IAP), Planit (free) 100K+ (3.6,
1,710, IAP).

**Evidence this audience pays** — **MEASURED**: PhotoPills and Sun Surveyor are **standalone
purchase-only** apps at **100K+ installs** with 4.8 ratings. **INFERRED** (arithmetic, floor only):
PhotoPills `100,000 × $10.99 ≈ ≥ $1,099,000` gross and Sun Surveyor `100,000 × $9.99 ≈ ≥ $999,000`
gross before Play's commission — floors, because install buckets are rounded lower bounds counting
devices, not buyers.

### 2.1 The broader night-sky shelf (free giants own the head terms)

The general stargazing / star-map shelf is **install-massive and free**, and its paid outliers sit far
below it. **MEASURED** (`B-stargazing-shelf.md` §1; `C-sun-moon-light-shelf.md` §1.3;
`A-astrophotography-baseline.md` §1):

| App | package | Installs | Rating (reviews) | Price |
|---|---|---:|---:|---|
| **Sky Map** | `com.google.android.stardroid` | **50M+** | 4.1 (481,603) | free, no ads/IAP |
| Stellarium – Star Map | `com.noctuasoftware.stellarium_free` | 10M+ | 4.8 (337,048) | free / IAP |
| Star Walk 2 Plus | `com.vitotechnology.StarWalk2Free` | 10M+ | 4.5 (571,689) | free / ads+IAP |
| Star Chart | `com.escapistgames.starchart` | 10M+ | 4.3 (174,326) | free / IAP |
| Star Tracker | `com.PYOPYO.StarTracker` | 10M+ | 4.5 (82,345) | free / ads+IAP |
| SkyView® Lite | `com.t11.skyviewfree` | 10M+ | 4.3 (58,099) | free, no ads/IAP |
| Sky Tonight | `com.vitotechnology.sky.tonight.map.star.walk` | 5M+ | 4.7 (85,714) | free / ads+IAP |
| Star Gazer | `skyview.star.map.stargazing` | 5M+ | 4.6 (50,189) | free / ads+IAP |
| Star Walk 2 Pro | `com.vitotechnology.StarWalk2` | 500K+ | 4.7 (32,541) | **paid $2.99** |
| SkySafari 8 Pro | `com.simulationcurriculum.skysafari8pro` | 1K+ | 4.2 (211) | **paid $34.99** |
| SkySafari 8 Plus | `com.simulationcurriculum.skysafari8plus` | 1K+ | 4.3 (38) | **paid $29.99** |
| SkySafari 7 Plus | `com.simulationcurriculum.skysafari7plus` | 10K+ | 4.4 (710) | **paid $9.99** |
| SkySafari Astronomy | `com.simulationcurriculum.skysafari7` | 10K+ | 4.3 (288) | **paid $6.99** |

**The head terms are owned by free giants.** `B-stargazing-shelf.md` read 25 incumbents' own Play
listing descriptions and classified them: **15 LIVE** (point-the-phone identification) / **4 BOTH** /
**6 PLANNING** — and **every 10M+ app and the 50M+ app is LIVE**. Largest LIVE app `50,000,000+`
vs largest pure-PLANNING app `100,000+` (Astrospheric) ⇒ an install ratio of **≈ 500×**.
**MEASURED** — `B-stargazing-shelf.md` §2–§3 (listing descriptions fetched from
`play.google.com/store/apps/details?id=<pkg>`; install buckets from `enrich-market.mjs`).

The dedicated **astro-conditions / target planners** are the actual night-planning job, and they are
small and mostly free or free+IAP (**MEASURED**, `A` §1, `B` §1): Astrospheric `100K+` (4.5, 804,
ads+IAP), Clear Outside `com.firstlightoptics.clearoutside` `100K+` (3.8, 1,368) and
`com.flo.clearoutside` `10K+` (3.5, 82), Good To Stargaze `50K+` (**3.7**, 677), Ouranos `10K+`
(3.9, 318), Telescopius `10K+` (4.5, 94), Astroweather `10K+` (4.2, 113), DSO Planner `1K+`
(4.1, 104). The clear-sky-chart / astronomy-weather apps carry the **weakest ratings on the shelf**.

### 2.2 The adjacent astrophotography-*planning* shelf (baseline A)

**MEASURED** (`A-astrophotography-baseline.md` §1, §3, §5):

- **PhotoPills $10.99 / 100K+ / 4.8 (8,216 exact)** — the clearest paid demand on this shelf too.
- **Mobile Observatory Pro `com.zima.mobileobservatorypro` — paid $11.99, 10K+, 4.6 (5,681)** and
  **Planit Pro $9.99 / 10K+ / 4.6 (1,220)**.
- The biggest dedicated astrophotography app is **free with no monetisation declared**: DeepSkyCamera
  `de.seebi.deepskycamera` **1M+ / 3.1 (781) / ads=false / IAP=false**; its paid sibling
  DeepSkyCamera Pro `$9.49` has only **1K+** installs (3.9, 98).
- Dedicated *planners* at the literal query are tiny and unrated: Nightlog (10+), Milky Way Tonight
  ($7.99, 100+), Astrophotography Planner (100+), Astro Planner ($4.99, 5+), Dark Skies (500+).
- Install floors of the dedicated planning/conditions/composition apps sum to **≈ 291,715**
  (**INFERRED** — sum of the Play band lower bounds; bands overlap so this is a floor proxy, not users).
- Complaint corpus for the planning job itself is empty: the dedicated planners are unrated and
  returned 0 reviews; the only complaints read were about **capture** apps (DeepSkyCamera 3.1★).

---

## 3. The four measured structural gaps

`C-sun-moon-light-shelf.md` §3 fetched all ten light-planner descriptions (`data-g-id="description"`)
and searched them for `satellite`, `iss`, `weather`, `cloud`, `nebula`, `deep sky`, `comet`, `meteor`,
`aurora`, `eclipse`, `night`, `dark sky`, `milky way`, `star trail`, `stargaz` (context-checked for
false positives). That keyword scan is the basis of every "does the shelf do it?" verdict below.

| Gap | Does the shelf do it? | Label |
|---|---|---|
| **(a) True darkness window** | **Partial** — incumbents plan *which night object is where*, never the darkness itself | MEASURED (keyword scan of 10 descriptions) |
| **(b) Deep-sky object catalogue** | **Almost not at all** | MEASURED (keyword scan of 10 descriptions) |
| **(c) Satellite passes** | **Not at all** | MEASURED (keyword scan of 10 descriptions) |
| **(d) Hourly cloud cover** | **Not at all, with one exception** | MEASURED (keyword scan of 10 descriptions) |

### (a) True darkness window

Every serious incumbent names Milky Way / star trails / stargazing and one adds a dark theme, but
**none** names light pollution, Bortle class, a moonless-dark window, or the night's own timeline beyond
moon phase. The quoted sentences the gap rests on (**MEASURED**, each app's own Play description):

- Sun Surveyor — *"Prepare night photography shots with the Milky Way & star trail features"* and
  *"…including the full moon looming low in the sky during magic hour, **total darkness for
  stargazing**, and more"*.
- PhotoPills — *"The First 2D Map-Centric Planner: Sun, Moon, Milky Way"*; star-trail/spot-stars calculators.
- Sun Position (Stonekick) — *"Astrophotography: see when and where the Milky Way core will be visible"*.
- SunOnTrack — *"Dark Theme for night hours"* (the entire night treatment).
- Golden Hour — *"Find duration of the civil, nautical and astronomical twilights, 'golden' and 'blue' hours"*.

**Reading:** the shelf plans an object's *position* at night; it does not compute the dark interval
itself. A dedicated, monetized light-pollution shelf **does** exist outside the light-planners —
`com.pa.lightpollutionmap` (Light Pollution Map – Dark Sky) **4.7 / 7,330 / 500K+ / free+IAP** and
`info.lightpollutionmap.mobile` **$5.49 / 4.4 / 80 reviews / 5K+** — **MEASURED**
(`D-satellite-aurora-adjacent.md` §1.3). So "nobody serves light pollution" is **false read broadly**;
the honest sentence is **"the light-planners do not, and the one dedicated paid app is weak (80
reviews at $5.49)"**. **MEASURED / INFERRED** — `F-engine-gap-map.md` §2.5.

### (b) Deep-sky object catalogue

Of ten descriptions, **only Planit Pro** names anything past the Milky Way:
*"…**major stars, constellations, nebulae azimuth and elevation angle**, star trail planning, … milky
way searching, solar eclipse and lunar eclipse, exposure/ND filter calculator…"*. What that sentence
does **not** contain: no object catalogue (Messier/NGC), no magnitude, no apparent size, no
visibility/altitude-window planner for a named target. **MEASURED** — `C-sun-moon-light-shelf.md` §3.

### (c) Satellite passes

**Clean negative.** No light-planner description mentions satellite passes. The only `satellite` hit
is a **map layer**: SunOnTrack *"Integrated map (with different views - satellite, terrain, …)"*. Every
`iss` match across the ten descriptions was the word "**miss**" (*"…you will never miss optimal light
situations…"*), verified in context. Meanwhile the dedicated satellite shelf is huge (§5).
**MEASURED** — `C-sun-moon-light-shelf.md` §3.

### (d) Hourly cloud cover

**Only PhotoTime** claims weather: *"…with accurate Sun Path, Sun Position, Moon Phases, **weather
forecasts** and powerful planning tools"* — generic, no cloud-cover modelling. No incumbent mentions
**cloud cover** (keyword scan: 0 hits for `cloud`). The dedicated astronomy-weather app **Clear
Outside** `10K+ / 3.5★` is the weakest rating on the cloud shelf. **MEASURED** —
`C-sun-moon-light-shelf.md` §3.

### 3.1 The gaps are *not* what users complain about (in the sampled reviews)

`C-sun-moon-light-shelf.md` §4 read 3 server-rendered reviews per page via `scripts/lib/play-reviews.mjs`
**`fetchReviews()`** (stated limit: Play serves a **3-review sample**, so this is a vocabulary, not a
frequency). The complaint clusters (**MEASURED**):

| Class | Apps that fired it |
|---|---|
| `paywall` (price / misleading free) | Sun Surveyor, Ephemeris ×3 — the strongest cluster |
| `accuracy` (wrong / no user calibration) | Sun Surveyor, Sun Seeker |
| `data-loss` / portability | Planit Pro (29 plans could not transfer to a new tablet) |
| `broken` / licence portability | Planit Pro (purchased `Location Explorer` would not restore on phones) |
| `value` | Sun Surveyor Lite (paid build called "OUTDATED") |
| `confusing-ui` / widget | Sun Position (widget background) |

**No sampled complaint mentions weather, cloud cover, satellites, or deep-sky objects.** The four gaps
are simply *absent from the products*, not yet demanded in the sampled review text. **MEASURED**
(absence) — `C-sun-moon-light-shelf.md` §4.

### 3.2 The cross-sell signal — how strong is the willingness-to-pay?

**MEASURED / INFERRED / UNVERIFIED** (`C-sun-moon-light-shelf.md` §5):

- **MEASURED — night is bundled into every paid light-planner.** Sun Surveyor, PhotoPills, Planit Pro,
  Sun Position and Ephemeris all sell Milky Way / star-trail / night content *inside the same paid
  product* as the golden-hour planner; Planit Pro addresses one audience in one listing (*"landscape
  photographers … interested in night photography … star-trails, milky way or astro-photography"*).
  **INFERRED** — vendors do not bundle what buyers do not ask for, so the buyer wanting *night* is the
  shelf's own answer.
- **MEASURED — the price point is shared**: the same `$7.99–$10.99` buys the bundled day+night planner.
- **UNVERIFIED — that this buyer wants a *deep-sky object brief*.** No incumbent sells one, so there is
  no observed willingness-to-pay. The settling command: read Play's **full** paid-planner review lists
  (Play only server-renders 3/page).
- **UNVERIFIED — that this buyer wants a *satellite-pass brief*.** The satellite audience is large and
  monetised (ISS Detector 5M+) but on a **free ad/IAP** model, not a one-time paid one, and no
  light-planner sells passes.

**So the two market gaps (b) and (c) are real absences, but the paying-buyer demand for them is
UNVERIFIED in this recon.** That is the strongest honest counter-argument to the idea and it must
travel with it.

---

## 4. The satellite shelf, the $17.99 anchor, and why it does not transfer

### 4.1 The corrected satellite reading

**MEASURED** (`D-satellite-aurora-adjacent.md` §1.1; `E-money-repositioning.md` §CORRECTIONS-1):

| App | package | Price | Installs | Rating (reviews) | Ads / IAP |
|---|---|---:|---:|---:|---|
| ISS Detector Satellite Tracker | `com.runar.issdetector` | free | **5M+** | 4.8 (121,000 / 120,809) | ads+IAP |
| **ISS Detector Pro** | `com.runar.issdetector.pro` | **paid $17.99** | **100K+** | 4.4 (3,190 / 3,189) | **none** |
| Satellite Tracker by Star Walk | `com.vitotechnology.SatelliteTracker` | free | 1M+ | 4.4 (61,400) | ads+IAP |
| Find Starlink Satellites | `com.findstarlink` | free | 1M+ | 4.1 (2,070) | IAP |
| Look4Sat | `com.rtbishop.look4sat` | free | 100K+ | 3.8 (1,010) | none |
| Satellite Chasers | `com.satellite.chasers` | free | 50K+ | 3.5 (232) | none |
| NASA Spot the Station | — | free | 500K+ | 4.7 (7,120) | none |
| ISS Live Now | `com.nicedayapps.iss_free` | free | 10M+ | 4.7 (388,000) | ads+IAP |
| Starlink Satellites Tracker (SGC) | `com.sgc.starlink_tracker` | paid $0.99 | 500+ | unrated | — |

**$17.99 is the highest observed price point in the whole recon**, and it proves satellite pass timing
is **payable somewhere**.

### 4.2 Why $17.99 does not transfer above the light shelf's $10.99 ceiling

**MEASURED / INFERRED** (`E-money-repositioning.md` §2): **ISS Detector Pro is not a standalone paid
listing; it is the paid *variant* of a free app with 5M+ installs.** The free parent carries **5M+
installs and 120,809 reviews** (ads+IAP); the Pro carries **100K+ installs and 3,190 reviews** (no
ads/IAP). Two consequences, arithmetic rather than taste:

- The Pro is reached through the free app's own funnel — a 5M+ install base converting to a $17.99
  ad-free upgrade is `100K+ / 5M+` ≈ **2 %** (**INFERRED** from two rounded buckets). **A new entrant
  has no 5M+ free funnel**, so $17.99 is a price paid by *already-acquired* users, not a price won on
  **discovery** against $10.99 competitors.
- The light-planning shelf shows the **opposite** model works there: PhotoPills `$10.99` and Sun
  Surveyor `$9.99` are **standalone purchase-only listings** that win on discovery at six-figure
  installs.

**Therefore the satellite capability differentiates *within* the light shelf's band; it does not lift
the price above $10.99.** The $17.99 fact still earns its place: it proves pass timing is a **payable**
feature somewhere, so **bundling** it is a credible differentiator rather than a curiosity.

### 4.3 Adjacent audiences priced but not chosen

**MEASURED** (`D` §1.2, §1.3): **Aurora** — My Aurora Forecast `5M+` (4.7, 55,300, ads) feeding My
Aurora Forecast Pro `$3.99 / 50K+ / 4.8 / 4,070` (IAP); 5 of 6 top `northern lights` cards declare IAP;
weakest earner Aurora Alerts **2.6★** / 720 / 100K+. **Dark sky** — paying audience is thin: one paid
app `info.lightpollutionmap.mobile` `$5.49 / 5K+ / 80 reviews` while the leader is free+IAP; Good To
Stargaze reviewers reject its ~`$23/year` subscription as *"data that can also be found elsewhere"*.
**INFERRED** — the money in the adjacent astronomy shelf flows to *photography planning* (PhotoPills,
$10.99, 100K+), not to light-pollution maps.

---

## 5. The price band and its ceiling

**Light-planning shelf: observed range `$4.99 – $10.99` one-time; the core is `$7.99 – $10.99`; the
hard ceiling is `$10.99`. Nothing above `$10.99` exists on this shelf.** **MEASURED** —
`C-sun-moon-light-shelf.md` §2; `E-money-repositioning.md` §CORRECTIONS-2; re-confirmed for the four
biggest rows by `docs/research/2026-10-08-finalists-verification.md` §B.

- The brief's `$6.99–$12.99` is **substantially confirmed in its lower half**; **above `$10.99` is
  REFUTED on this shelf** — the highest one-time light-planner price observed is **PhotoPills $10.99**.
  The single higher price seen in a query (`ephemeris.vedic`, `$37.99`) is **astrology software**, not
  photography, and appeared only because the `photographer ephemeris` shelf is polluted.
  **MEASURED** — `C-sun-moon-light-shelf.md` §2.
- **Chosen price: `$9.99`** — the shelf's modal price (Sun Surveyor $9.99/100K+; Sun Position PRO
  $9.99/50K+; Planit Pro $9.99/10K+), chosen because pricing at the `$10.99` ceiling held by the
  most feature-dense incumbent (PhotoPills) would be an unmeasured bet against the shelf's own
  strongest-rated price point. **MEASURED / INFERRED** — `E-money-repositioning.md` §4.
- **Reported disagreement on "the ceiling":** the `$10.99` ceiling is **shelf-specific to the
  light-planning shelf**. The *adjacent* astrophotography/observing shelf carries paid products above
  it — **Mobile Observatory Pro `$11.99`** (10K+, 4.6, 5,681) and **SkySafari 8 Plus/Pro `$29.99 /
  $34.99`** (1K+). **MEASURED** — `A-astrophotography-baseline.md` §1, `E` §1.1. Neither is a
  **light-planning** listing, so neither moves the light shelf's ceiling, but neither should be hidden.

**Net arithmetic (modelled, not observed)** — `E-money-repositioning.md` §3–§4: net per sale
`= price × 0.85 × 0.92`; at `$9.99` net `$7.81`, needing **64 sales/month** for a `$500`/month net
target (or **78/month** at the higher 30 % fee tier). **UNVERIFIED** — that the shelf supplies 64 paid
sales/month to a new entrant; no revenue API exists.

---

## 6. Licence findings (the sharpest commercial risk)

### 6.1 OpenNGC deep-sky catalogue — **CC-BY-SA-4.0, declared in the upstream README (adjudicated)**

**Adjudicated reading (settled 2026-10-08):** the catalogue is licensed **CC-BY-SA-4.0**, stated in the
upstream repository's README, and the repository carries **no separate `LICENSE` file**. The two
readings that had looked like a disagreement were reading two different things:

- **MEASURED — no `LICENSE` file exists.** `F-engine-gap-map.md` §3.2 and its ledger probed
  `…/OpenNGC/master/LICENSE` and got **HTTP 404** (`PROBE openngc-LICENSE FAILED: … (404) Not Found`).
  Independently, GitHub's own licence detector at `https://api.github.com/repos/mattiaverga/OpenNGC`
  returned an **empty `license.spdx_id`** and an **empty `license.name`** — the detector reporting **no
  `LICENSE` file in the repository**, *not* a statement that the data is unlicensed.
- **MEASURED — the licence is declared in the README.** `https://raw.githubusercontent.com/mattiaverga/OpenNGC/master/README.md`
  returned **HTTP 200, 4,285 bytes**, and **line 15** reads verbatim:
  `license limitations, OpenNGC is released under CC-BY-SA-4.0 license,`.
- **The `NGC.csv` data itself is live and usable**: `PROBE openngc-NGC.csv STATUS=200 BYTES=3877591`,
  header carries `Name;Type;RA;Dec;Const;MajAx;MinAx;…;B-Mag;V-Mag;…;M;NGC;IC;…;Common names;…`.
  **MEASURED** — `F` §3.2.

**Consequence — stated honestly, not overclaimed either way.** CC-BY-SA-4.0 carries an **attribution**
obligation and a **share-alike** obligation: a paid closed-source app that bundles a *derived* (trimmed)
subset of the catalogue has to satisfy both on the derived data, and the obligation must be met rather
than assumed away. `F-engine-gap-map.md` §3.2 item 2 already names the trim this concerns: *"dropping
the identifier/note columns and keeping Messier + the brightest few hundred NGC/IC entries is a
build-time transform"*, **INFERRED** at roughly `~200–600 KB`. **What exactly share-alike requires for
such a trimmed bundle is a legal question this evidence does not settle** — recorded in §10 as a legal
question, not an evidence gap. This section neither declares the asset safe nor declares the product
blocked.

- **Aggravating conflict (root-doc, load-bearing).** The factory's own licensing research puts
  **`CC-BY-SA-*` on the denylist for a paid closed-source app**: *"ShareAlike — you must distribute your
  contributions under the same license; incompatible with a paid closed-source app"*
  (`docs/research/2026-10-02-ui-icon-libraries-and-licensing.md:147`; `docs/icon-provenance.md:174`).
  A **CC-BY-SA-4.0** catalogue in a paid closed-source purchase-only app therefore needs an explicit
  owner/legal decision before the asset ships — see §10.

**Resolution — the legal question is answered by dropping the source, not by answering it**
(owner decision, recorded 2026-10-08). OpenNGC **is not shipped**. Rather than adjudicate share-alike
for a trimmed bundle inside a closed-source paid APK, the slot uses a **hand-compiled list of roughly
250 famous deep-sky objects** (the Messier objects plus showpiece NGC/IC entries), whose designations,
coordinates and magnitudes are **facts**, not a protected compilation — and roughly 250 objects out of
a 13 000-row catalogue is not a substantial part of it. The OpenNGC findings above stay on the record as
*litigation-avoidance evidence*: they are why the source was rejected, and they remain reusable for
any other slot that reaches for a CC-BY-SA data asset.

**Why not Wikidata, which is CC0** — `MEASURED`, 2026-10-08, in this session:

```text
SPARQL  ?i wdt:P31 wd:Q14530  (instance of "Messier object")  -> HTTP 200; COUNT = 0
SPARQL  COUNT … P528 STRSTARTS "NGC "                          -> HTTP 504 Gateway Timeout
SPARQL  COUNT … P528 STRSTARTS "IC "                           -> HTTP 429 Too Many Requests
API     list=search  srsearch=haswbstatement:P528="NGC 7000"   -> totalhits = 0
API     list=search  srsearch=haswbstatement:P31=Q2            -> totalhits = 0
```

The probe was verified against controls before its result was trusted, because an all-zero sensor is a
suspect sensor: `srsearch="Andromeda Galaxy"` returned **15827** hits with **Q2469** first,
`srsearch="NGC 7000"` returned **Q265515**, and `haswbstatement:P528` alone returned **8 444 398** —
so the search route works, and the zeros are a finding rather than a broken probe. **Wikidata can
resolve an object whose identifier you already hold; it cannot enumerate a catalogue.** That is useless
here, because the list of identifiers is the very thing missing. CC0 is the right licence on the wrong
data shape.

### 6.2 Open-Meteo — free tier is **NON-COMMERCIAL ONLY; unusable for a paid app**

**MEASURED** — `F-engine-gap-map.md` §5.3, live fetch of `https://open-meteo.com/en/terms` (status 200,
raw text in `netcheck.txt`), quoted:

> "By using the Free API for non-commercial use you agree to following terms: Less than 10'000 API
> calls per day, 5'000 per hour and 600 per minute. **You may only use the free API services for
> non-commercial purposes.** You accept to the CC-BY 4.0 licence…"

> Non-commercial includes "private or non-profit websites or apps **that do not have subscriptions or
> advertising**".

The app is `purchase_only_no_ads` at `priceUsd` 9.99 — a **commercial use**. The paid
`customer-api.open-meteo.com` tier is a **recurring subscription** (Standard 1M / Professional 5M /
Enterprise 50M+ calls per month), and **subscriptions are banned** with no recurring revenue to fund a
recurring cost. **The free tier is therefore not a usable source for a paid app.** **MEASURED / ON
RECORD** — the app's archetype from `docs/ideation/plans/sessionlight-session-planner.economics.json`.

### 6.3 `api.met.no` — keyless, **CC BY 4.0 with attribution**, hourly total cloud only (settled)

**MEASURED** — `F-engine-gap-map.md` §5.3: `https://api.met.no/weatherapi/locationforecast/2.0/compact`
returned **HTTP 200** keyless, **89 hourly entries**, `cloud_area_fraction = 100.0`, `meta.updated_at =
2026-10-08T13:16:55Z`. It worked **both with and without a `User-Agent`**. It gives **no low/mid/high
split**, so a transparency model would be coarser.

**MEASURED — the terms were read directly (settling the licence).**

- `https://api.met.no/doc/TermsOfService` → **HTTP 200, 14,445 bytes**. The terms state, verbatim:
  *"All open data require attribution as specified in the CC BY 4.0 license: You must give appropriate
  credit, provide a link to the license, and indicate if changes were made."*
- The same page also states a **request-identification requirement**: a client must identify itself with
  a `User-Agent` **or** an `Origin`/`Referer` header.
- `https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=55.75&lon=37.62` → **HTTP 200,
  61,401 bytes**; the payload contains `cloud_area_fraction` **90 times** (hourly total cloud) and
  **zero** occurrences of `low_cloud_area_fraction` / `medium_cloud_area_fraction` /
  `high_cloud_area_fraction` (**no cloud-layer breakdown**), plus `fog_area_fraction` 63 times.

**Adjudicated reading:** `api.met.no` is **usable in a paid commercial app under CC BY 4.0 with
attribution, keyless**, and it provides **hourly TOTAL cloud cover only — no low/mid/high split**. A
`User-Agent` (or `Origin`/`Referer`) header is required by the terms.

**Honest limit — a reliability risk, not a licence risk.** CC BY 4.0 settles the licence side, but
`api.met.no` remains a **third-party service the app does not control**: it can change, throttle or
vanish, and **no SLA or rate-limit reading was taken** in the six files. The app should **degrade
gracefully** when the feed is unavailable. That is an availability/rate-limit dependency; it **does not
reopen the licence question** (see §10).

### 6.4 Other sources named by the six files

- **CelesTrak GP JSON** — `https://celestrak.org/NORAD/elements/gp.php?GROUP=…&FORMAT=JSON`: **keyless,
  HTTP 200**, same-day epochs (`STATIONS` 9,270 B, `WEATHER` 30,271 B). **MEASURED** — `F` §4.4.
  Its **usage policy page returned HTTP 500**, so the terms for a `$9.99` app are **UNVERIFIED**
  (**MEASURED (absence)** — `F` §4.4, `F` ledger).
- **Astronomy Engine for Kotlin/JVM — MIT** (© 2019–2025 Don Cross). **MEASURED** — `F` §2.1,
  `astronomy.kt:2-7`.
- **Vallado/CSSI SGP4 Java subset** — *"no standalone license file among the inspected entries; shipping
  rights … need independent confirmation before vendoring"*. **UNVERIFIED** —
  `.kiln/scratch/nv-core-proof/RESULT.md`.

---

## 7. Engine feasibility verdict table

`F-engine-gap-map.md` §8, reproduced (labels as given; `F` re-ran the on-disk probes itself this
session — **no Android/Gradle/D8/R8 build had been run when `F` wrote it**, so `F`'s APK-size figures
were `INFERRED`; that gap is now closed by `G-android-build-proof.md`, see the packaging section below):

| Gap | Verdict | Covered by | New work | Network | Permission | Main risk |
|---|---|---|---|---|---|---|
| **(a) night / darkness window** | **DELIVERABLE** (dark window) / **RISK** (Bortle) | Astro Engine `searchAltitude`, `searchRiseSet`, `moonPhase`, `illumination`, `angleFromSun` — `astronomy.kt`; **only the Sun verified numerically** (0.0248 arcmin) | few dozen lines; **Bortle would need a new bundled raster/lookup asset** | none | none (manual/saved location); coarse location only if auto-locating | Moon path never verified numerically; Bortle is in no claim, and light pollution is already served by a 500K+ free incumbent |
| **(b) deep-sky catalogue** | **DELIVERABLE-WITH-WORK** | Astro Engine `defineStar` + `searchAltitude`/`horizon` (`astronomy.kt:452`, `:6343`) — API present, **not exercised** | **hand-compiled list of ~250 famous objects** (Messier + showpiece NGC/IC) with RA/Dec, magnitude and apparent size + list/detail UI | build-time only, keyless | none | **no permissive full catalogue exists** — OpenNGC is CC-BY-SA-4.0 (README L15) and Wikidata cannot enumerate one (see §6.1, Resolution); coverage is therefore our own ~250 objects, not 13 000 |
| **(c) satellite passes** | **DELIVERABLE-WITH-WORK** | Orekit `TLEPropagator` + `TopocentricFrame` + `ElevationDetector`; corpus **29/33 blocks** within tolerance | segmented pass finder (proven fix), OMM→orbit mapping (now **direct** via Orekit `OmmParser` → `Omm`; the hand field mapping into a `TLE` is the fallback), visible-pass policy, stale-TLE policy | yes — CelesTrak GP JSON keyless HTTP 200 | `INTERNET` | **detector trap (1 vs 15 passes, reproduced 3×)**; Orekit 11.56 MB (the smoke's 13.1.5 jars); stale-TLE 27,924 km error after ~3 weeks |
| **(d) hourly cloud cover** | **DELIVERABLE-WITH-WORK** (engineering) / **RISK** (reliability) | **nothing** — not an engine capability | HTTP client + model + timeline UI aligned to the dark window | yes — Open-Meteo keyless **but non-commercial-only**; `api.met.no` keyless **CC BY 4.0**, fallback (total cloud only) | `INTERNET` | licence settled (`api.met.no` CC BY 4.0 with attribution); residual **third-party reliability/rate-limit** + recurring cost on a one-time price |

Two `F` findings that decide how the engine is used:

- **The pass-detector trap (highest chance of shipping a broken-but-correct-looking feature).**
  `TLEPropagator` + `ElevationDetector` over **one** multi-day `propagate()` returns **1** acquisition
  where **15** exist, identically for `maxCheck` 600/300/60/10 s (**MEASURED**, reproduced 3× —
  `run-passvariants.txt`); an independent 60 s brute-force scan finds **15** (`run-passcrosscheck.txt`).
  Segmenting into ≤1-hour calls reports all 15. `F` §4.2.
- **Orekit's own verdict was not "use Orekit".** `.kiln/scratch/nv-core-proof/RESULT.md` says
  `OREKIT_RISKY_USE_VALLADO_SUBSET` (11.56 MB of jars for one job — measured on the smoke's vendored
  13.1.5 jars; the declared pin is 13.1.9 — vs a **135,064-byte** Vallado Java
  subset) — but the Vallado subset's numerical accuracy against the corpus was **never measured**
  (**MEASURED (absence)**) and its licence is **UNVERIFIED** (`F` §1.2, §6.3).
- **Corrections to the recon framing** (`F` §1): the SGP4 corpus has **four** divergences (not three) —
  three annotated error fixtures plus `20413#2`, extrapolated ~3 weeks, passing 1 of 70 vectors with
  worst error **27,924.0197880 km**; and `astronomy.kt` contains **no** Messier/NGC catalogue, no
  magnitude table and no Bortle/SQM code (the only magnitude hits are solar-system `magnitude()`).

**APK packaging and size — now `MEASURED`, not `INFERRED`.** The `F` estimate below was replaced by a
real build in `G-android-build-proof.md` (**MEASURED PASS**, `ANDROID_BUILD=PASS`, `SIZE_MB=4.18`):

- **Route, and why no slot was created.** `G` built a **standalone throwaway Gradle project** at
  `.kiln/scratch/android-orekit-smoke/`, through the factory's own `runGradleTasks()`
  (`scripts/lib/gradle.mjs`), **not** via `scripts/new-slot.mjs`. The reason is mechanical: a slot
  needs four validated contracts (`--economics`, `--differentiators`, `--naming` and an OpenSpec
  change) and a packaging measurement has none, so faking them would have been the exact defect
  `.cursor/rules/spec-first.mdc` forbids. **MEASURED** — `G` §1, §9 ledger.
- **A real release build.** `isMinifyEnabled = true` + `isShrinkResources = true`, R8 full mode
  (`-allowaccessmodification` in `configuration.txt`), toolchain matching `AGENTS.md` (AGP 9.4.1,
  Gradle 9.7.1, Kotlin 2.3.21, compileSdk 37 / targetSdk 36 / minSdk 26, JDK 17). Build reached
  `BUILD SUCCESSFUL` on attempt 2 (exit `0`, 226,809 ms; attempt 1 failed on the worker's own smoke
  Kotlin, not on Orekit). **MEASURED** — `G` §3.
- **Zero Orekit-specific keep rules were needed.** `app/proguard-rules.pro` was **comments only**
  (no `-keep`), and R8 retained **220 of 249** `org.orekit.*` classes and **49 of 59**
  `org.hipparchus.*` — the call path's classes, deleting the rest, with no linkage failure. **MEASURED**
  — `G` §4.
- **Device verification.** Serial `R58MB3SGZVL`, `ro.product.model` = **`SM-A405FM`** (Galaxy A40),
  Android 11 — the factory's designated test phone, not the A16. Release APK installed (`Success`),
  launched, and printed `OREKITSMOKE_OK`; **zero** `NoClassDefFoundError` / `NoSuchMethodError` /
  `VerifyError` / `LinkageError` / `FATAL EXCEPTION` across **3,908** logcat lines. **MEASURED** — `G` §6.
- **Numerics are byte-identical to the JVM reference.** `elevation_deg=-44.136279 azimuth_deg=354.524835`
  and TEME `pos_km=(1280.749619,5592.447086,3605.982869)` match the on-disk JVM run
  (`run-tinydata.txt`) by **exact, case-sensitive string equality** — same bits from ART on arm64 and
  HotSpot on x86-64. **MEASURED** — `G` §6.
- **THE ACTIONABLE FINDING — most of the payload is data, not code.** APK = **4,379,153 bytes**
  (4.18 MiB), single dex; `classes.dex` = 1,753,644 uncompressed / **900,383 compressed**. But
  **~75.7 %** of the compressed payload is **Orekit/Hipparchus embedded *assets*, not code**:
  `assets/org/orekit/**` (88 entries) + `assets/org/hipparchus/**` (7 entries) =
  12,099,051 uncompressed / **3,260,714 compressed** — the **ITU-R-P.834** atmospheric-attenuation
  tables plus a **1.89 MB** random-generator table (`new-joe-kuo-6.21201`) that an **SGP4 job never
  loads**. The data the app actually loads is trivial by comparison: `assets/orekit-data/tai-utc.dat`
  = 3,321 bytes / 511 compressed. **MEASURED** — `G` §5.
- **Excluding those assets is an OPPORTUNITY, not a saving already achieved.** `G` states the trim is
  a cheap `packaging { resources { excludes += … } }` entry, but that the files are loaded lazily, so
  **the exclusion is UNTESTED and requires re-running the proof** (its absence only surfaces when a
  code path asks for a file). **INFERRED / UNVERIFIED — do not book it as a gain.** **MEASURED** — `G` §5.
- **What R8 cannot touch, and what the PASS does not cover.** `isShrinkResources` removes unused
  *Android* resources; **jar-embedded assets are merged into `assets/` and are invisible to it**, so
  R8 will never remove the 3.26 MB above. And the byte-identical numerics prove the **SGP4 path
  only** — the Sun is the only numerically verified astro path, and the dark window, deep-sky
  catalogue and weather capabilities are **untouched** by this proof. A PASS on packaging is **not**
  a PASS on the engine.

*These packaging figures — the R8 class counts (220/249, 49/59), the **4,379,153-byte** APK, the
**~75.7 %** asset share and the **11.56 MB** Orekit jar set — were measured on the smoke's vendored
**13.1.5** jars; with the declared pin at **13.1.9** they are re-taken at implementation.*

**The `F` estimate this replaces (`INFERRED`, retained for the record):** Orekit + Hipparchus adds
**≈ 1.4–2.8 MB** in the realistic case (floor ≈ 0.9–1.2 MB; ceiling ≈ 9.5–12 MB if shrinking does
nothing), against the factory's measured release APKs of 1.1–4.0 MB. The measured smoke APK
(**4.18 MiB**) landed **near the top of that band and on assets rather than code**; `G` notes it is a
smoke APK carrying the Compose baseline and nothing else, so it is **not** a forecast for a product
APK, and the **incremental** cost over a no-Orekit build was **not** isolated (no baseline build was
made). Runtime data remains negligible: `tai-utc.dat` = **3,321 bytes**. Astronomy Engine adds ~tens to
~150 KB.

---

## 8. Methods note — the confirmed error and a self-correction

**The error.** Money-owner revision 1 asserted, in words, that the satellite shelf had *"six of six
free"* and *"there is not one paid-upfront app to anchor $7.99 against"*. **That was false.**
**MEASURED** — `E-money-repositioning.md` §CORRECTIONS-1.

**The cause — query depth, not a bad fetch.** Revision 1 read the top **6** cards of
`--query "satellite tracker"`, a window led by the free parent `com.runar.issdetector` (5M+, ads+IAP),
under which the paid **Pro variant ranks outside the top 6**. The Pro is **#2** under a *different*
query, `--query "iss detector"`. Revision 1 read a 6-row window and generalised it to the whole shelf.

**The reusable lesson (belongs at root — see §10):** *a top-N enrichment window is not a shelf.* A free
parent ranking above a paid sibling can hide a paid anchor entirely, so a shelf-level **"nobody pays
here"** conclusion may not be drawn from a single shallow query — it is a statement about what was
observed, exactly as `.cursor/rules/grounded-claims.mdc` requires. The correction is **itself evidence
about method**: the same reading was produced correctly the moment the query was changed and the window
widened, which is why the durable record keeps both the wrong and the corrected reading.

**A second, independent confirmation of the same hazard class.** `A-astrophotography-baseline.md` §4
explicitly refused to draw its own shelf-level conclusion: it states it *"could not measure a clean
product gap from complaint text, and I say so"*, because the dedicated planning apps returned **0
reviews** to the scanner — a different doorway (an empty complaint corpus) onto the same rule: absence
in a shallow sample is not a finding about the shelf. **MEASURED** — `A` §4.

**A third instance, and a different kind — a worker correcting its own report.** The
`G-android-build-proof.md` device smoke cross-checks its on-device numbers against the JVM run already
on disk. Its **first draft** asserted the two runners agreed on the **event grid** as well as the
numerics; the worker's own review of that draft found the claim was **not supported**, and the report
was corrected to **state the divergence rather than gloss it**: the JVM probe runs **one unsegmented
3-day `propagate()`** and finds **1 AOS** (`PASSES_3D_10DEG=1 EVENTS=2`), while the device walks
**24 × 1-hour windows** over **one day** and finds **5 AOS** (`SEGMENTS=24 EVENTS=10
AOS_1D_10DEG=5`) — **deliberately**, because `F` measured the unsegmented multi-day call as buggy (its
own **`1`-vs-`15`** detector trap). The two counts are therefore **not one measurement**: `F`'s
`1`-vs-`15` is a 3-day-window trap, and `G`'s `1`-vs-`5` is a **different test setup** (3-day
unsegmented versus 1-day segmented), and merging them would repeat exactly the conflation the trap
warns about. The load-bearing cross-check — the TEME/TOPO pair — *is* byte-identical, and `G` states
that the residual first-LOS divergence is **`INFERRED` (root-finding against a differently-segmented
state history), not measured**, because the two programs do not report a common event count. **MEASURED**
— `G` §6, §9 ledger.

**Why it belongs beside the top-N error.** It is the **same discipline applied to a self-report rather
than to a market query**: the first hazard was a *shallow window* generalised to a shelf; this one was a
*true-looking agreement* generalised to a claim about equivalence. In both cases the correction is
**itself evidence about method** — the record keeps the withdrawn draft and the stated divergence, so a
later reader can see that the equivalence was checked and rejected rather than never examined.
**INFERRED** — the parallel between the two is stated, not measured.

---

## 9. What is app-specific versus reusable methodology

Per `.cursor/rules/slot-docs.mdc`, app-specific durable documents move into `apps/<slot>/docs/` after
scaffold, while factory methodology stays at root. This document is written at the root because it is
the **consolidation** of a disposable recon; the split below says what should move later.

**App-specific — move into `apps/<slot>/docs/` after scaffold:**

- The **shelf tables** (§2, §4, §5) — the light-planner, astro-planner, satellite, aurora and dark-sky
  incumbents with their prices, installs and ratings. These are about *this* app's market and are
  useless without it.
- The **four-gap findings** (§3) and the per-incumbent comparison sentences they rest on — the exact
  wedge this product is built against.
- The **cross-sell willingness-to-pay** state (§3.2) and the §3.1 complaint vocabulary.
- The **engine feasibility verdicts for this engine** (§7) in so far as they name *this* engine's
  symbols (`astronomy.kt`, Orekit jars, `tai-utc.dat`) — and the specific OpenNGC/`api.met.no` licence
  state once the owner decides it.

**Reusable methodology — stays at root:**

- The **top-N-window-vs-shelf lesson** (§8): a shallow enrichment window can hide a paid sibling behind a
  free parent; a shelf-level "nobody pays here" claim needs a query that actually surfaces the paid
  rows. This is a factory-wide reading rule, not a fact about this shelf.
- The **label discipline and the "report the disagreement, do not average it"** approach used throughout.
- The **principle that a market gap is not a monetizable gap until a buyer's willingness-to-pay is
  observed** (§3.2) and that an absence in a 3-review sample is not a finding about users (§3.1).
- The **licence-vs-price rule** (§6.2): a free data tier licensed non-commercial is unusable for any of
  this factory's paid slots — a general constraint, already reflected in the `skywindow-drone` proposal
  hitting the same Open-Meteo wall (`F` §5.5). **MEASURED** — `F` §5.5.

**Settled since the six files, and it did not go the way the six files assumed:** the OpenNGC licence is
**CC-BY-SA-4.0, declared in the upstream README** (§6.1) — and the question it raised was answered by
**dropping the source**, not by adjudicating share-alike. No permissive full deep-sky catalogue was
found (§6.1, Resolution), so the slot ships a **hand-compiled list of ~250 famous objects** and the
CC-BY-SA obligation never attaches. The reusable finding for the factory is narrower than "CC-BY-SA is
banned": **the asset denylist applies to *code* by linking, but a data *Collection* is a different
question — and here the cheapest way out was to need less data, not to argue the licence.**

---

## 10. Unsettled

Load-bearing questions none of the six files settles. Each names the command/read that would settle it.
None is filled with a guess.

1. ~~**What share-alike requires for a trimmed OpenNGC bundle.**~~ **CLOSED 2026-10-08 — by removing the
   question, not answering it.** OpenNGC is not shipped (§6.1, Resolution); the deep-sky list is a
   hand-compiled set of ~250 famous objects whose coordinates, magnitudes and designations are facts.
   The legal question is therefore moot for this slot. What stays open is the *general* factory question
   it surfaced — whether a CC BY-SA **data** asset is admissible where a CC BY-SA **code** dependency is
   not — and that is deliberately left to the owner rather than settled here.
2. **A third-party forecast feed's availability and rate limits — not its licence.** `api.met.no`'s
   licence is **settled** — **CC BY 4.0 with attribution**, keyless, hourly total cloud (§6.3). What no
   file here settles is its **availability under load / rate-limit behaviour** and the absence of any
   SLA: the app calls a service it does not control, which can change, throttle or vanish, so it must
   **degrade gracefully**. This is a **reliability risk, not a licence risk**. Settle by reading the
   service's rate-limit statement and measuring it under load.
3. **CelesTrak usage policy.** Page returned **HTTP 500**; terms for a `$9.99` app **UNVERIFIED**.
   Settle in a browser.
4. **Willingness-to-pay for the deep-sky catalogue and satellite passes bundled into a light-planner.**
   No incumbent sells either; **UNVERIFIED**. Settle via Play's full paid-planner review lists
   (only 3/page are server-rendered).
5. **Whether any incumbent already produces one combined brief** (dark window + target placement in one
   flow). **UNVERIFIED** in `A` §4/§5; settle by an in-app feature read of Astrospheric and Telescopius.
6. **Whether any long-tail app serves satellite-streak avoidance.** **UNVERIFIED** in `D` §2; settle
   with `node scripts/scan-titles.mjs --query "satellite trail avoidance"` plus a hand read.
7. **Active-user counts and revenue of any incumbent.** Play exposes no active-user or revenue figure.
   **UNVERIFIED** — `A` §5.
8. **That the light-planning shelf supplies 64 paid sales/month** to a new entrant. **UNVERIFIED** —
   no pricing experiment, cohort or per-month rate; no revenue API. `E` §8.
9. **Whether a keyless forecast feed keeps recurring cost near zero** (the `api.met.no` fallback vs a
   self-hosted Open-Meteo OSS server that *would* be a server the factory runs). **UNVERIFIED** — `E` §8,
   `F` §5.4.
10. **Moon-path numerical accuracy** of `astronomy.kt` (upstream claims ~1 arcmin; the three `moon_*`
    corpus cases were refused as `UNVERIFIED`). Settle against JPL Horizons rows as `AstroCorpus.kt`
    does for the Sun. `F` §2.1.
11. **Whether the Orekit/Hipparchus embedded assets can be excluded without breaking a code path.**
    `G` measured those assets at **3,260,714 bytes compressed (75.7 %** of the payload) and states an
    `excludes` entry would remove most of it — but that exclusion was **never built or tested**, and the
    files load lazily so their absence only shows when a path asks for them. **INFERRED / UNVERIFIED** —
    `G` §5. Settle by adding the `packaging.resources.excludes` entry and **re-running the proof**.
12. **Whether the Vallado SGP4 subset's accuracy matches Orekit's corpus** (no Vallado corpus run
    exists on disk, only the Orekit run). Settle by running the corpus against the Vallado path. **Its
    licence is also still unconfirmed** (`G` §7). Note `G`'s finding that the fallback is now **less
    necessary, not chosen**, because Orekit itself is a measured PASS on the target configuration.

*(The former item "whether Orekit + Hipparchus build, shrink and run on Android" is **`MEASURED PASS`**
and has been removed — `G-android-build-proof.md` §3–§6. What its PASS does **not** cover is stated in
§7: the dark window, deep-sky catalogue and weather paths, and the `javax.xml` data path, remain open.)*

---

## 11. Claim ledger

```text
CLAIM / LABEL / EVIDENCE
PhotoPills is paid $10.99, 100K+ installs, 4.8 (8,220) / MEASURED / enrich-market.mjs --top 6, C-sun-moon-light-shelf.md §1.1; independently re-read in docs/research/2026-10-08-finalists-verification.md §B
Sun Surveyor is paid $9.99, 100K+ installs, 4.8 (5,140) / MEASURED / same; finalists-verification §B
Sun Position PRO $9.99 / 50K+ / 4.9 (2,730), Sun Seeker $8.99 / 50K+ / 4.8 (1,000), SunOnTrack $7.99 / 50K+ / 4.8 (874), Golden Hour+ $4.99 / 1K+ / 4.7 (189) / MEASURED / C §1.1
Light-planning paid price band is $4.99-$10.99, core $7.99-$10.99 / MEASURED / C §1.1-§1.2, §2
Hard ceiling on the light-planning shelf is $10.99; nothing above / MEASURED / C §2; E §CORRECTIONS-2
Mobile Observatory Pro is paid $11.99 at 10K+/4.6 (5,681); SkySafari 8 Plus/Pro $29.99/$34.99 at 1K+ (adjacent shelf) / MEASURED / A §1, B §1, E §1.1
Sky Map is 50M+, 4.1 (481,603), free no ads/IAP; every 10M+ app on the stargazing shelf is LIVE / MEASURED / B §1-§2 (25 listing descriptions from play.google.com)
15 LIVE / 4 BOTH / 6 PLANNING of 25 read incumbents; largest LIVE 50M+ vs largest PLANNING 100K+ = ~500x / MEASURED / B §2-§3
Gap (a) true darkness window absent from the light shelf / MEASURED / C §3 keyword scan of 10 descriptions
Gap (b) deep-sky catalogue effectively absent (only Planit Pro names "nebulae") / MEASURED / C §3
Gap (c) satellite passes absent entirely / MEASURED / C §3 (only "satellite" hit is a SunOnTrack map layer)
Gap (d) cloud cover absent except PhotoTime's generic "weather forecasts" / MEASURED / C §3
No sampled light-planner complaint mentions weather, cloud, satellites or deep-sky / MEASURED (absence) / C §4 (3 server-rendered reviews per page)
Willingness-to-pay for bundled deep-sky/satellite in a light-planner / UNVERIFIED / C §5; no incumbent sells them
Every paid light-planner bundles night content in one product / INFERRED / five MEASURED description quotes in C §5
ISS Detector Pro is paid $17.99, 100K+, 4.4 (3,190), no ads/IAP / MEASURED / enrich-market.mjs --query "iss detector" --top 6; D §1.1
Its free parent com.runar.issdetector is 5M+, 120,809 reviews, ads+IAP / MEASURED / same run
The $17.99 does not transfer above $10.99 because the Pro rides a free 5M+ funnel (~2% conversion), not discovery / INFERRED / E §2 from two MEASURED rounded buckets
Revision 1's "six of six free" was a top-6 depth artifact of "satellite tracker" / MEASURED / E §CORRECTIONS-1 vs §1.6
Planit Pro sampled reviews report plans and a purchased add-on that do not travel across devices / MEASURED / C §4 (fetchReviews, 3-review sample)
OpenNGC NGC.csv is fetchable keyless, 3,877,591 bytes, with V-Mag/B-Mag and MajAx/MinAx / MEASURED / F §3.2 (PROBE openngc-NGC.csv STATUS=200)
OpenNGC's licence is CC-BY-SA-4.0, declared in its README, and the repo has no LICENSE file / MEASURED / https://api.github.com/repos/mattiaverga/OpenNGC -> empty license.spdx_id and empty license.name (no LICENSE file in the repo); https://raw.githubusercontent.com/mattiaverga/OpenNGC/master/README.md -> HTTP 200, 4,285 bytes, line 15 "license limitations, OpenNGC is released under CC-BY-SA-4.0 license,"; the earlier 404 on the LICENSE path (F §3.2) is the same fact
Whether share-alike (CC-BY-SA-4.0) permits bundling a trimmed OpenNGC subset in a paid closed-source APK / UNVERIFIED (legal question) / not settled by these six files; denylist note docs/research/2026-10-02-ui-icon-libraries-and-licensing.md:147 lists CC-BY-SA-* as incompatible with a paid closed-source app; needs an owner/legal decision (see §10)
CC-BY-SA-* is on this factory's denylist for a paid closed-source app / ON RECORD / docs/research/2026-10-02-ui-icon-libraries-and-licensing.md:147; docs/icon-provenance.md:174
Open-Meteo's free tier is licensed non-commercial only; its commercial tier is a subscription / MEASURED / F §5.3, live fetch of open-meteo.com/en/terms and /pricing, 200, quoted in netcheck.txt
api.met.no returns HTTP 200 keyless, 89 hourly entries, cloud_area_fraction total cloud / MEASURED / F §5.3 (PROBE met-no-compact STATUS=200 BYTES=39280)
api.met.no's terms permit a commercial app; it is licensed CC BY 4.0 with attribution and needs an identifying User-Agent (or Origin/Referer) / MEASURED / https://api.met.no/doc/TermsOfService -> HTTP 200, 14,445 bytes, verbatim "All open data require attribution as specified in the CC BY 4.0 license: You must give appropriate credit, provide a link to the license, and indicate if changes were made."; the same page states the User-Agent/Origin/Referer requirement
api.met.no gives hourly TOTAL cloud only, no low/mid/high split / MEASURED / https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=55.75&lon=37.62 -> HTTP 200, 61,401 bytes; cloud_area_fraction 90x; zero low_cloud_area_fraction/medium_cloud_area_fraction/high_cloud_area_fraction; fog_area_fraction 63x
A third party forecast feed's availability/rate limits are unmeasured (reliability, not licence) / UNVERIFIED / no SLA or rate-limit reading in the six files; see §10 item 2
CelesTrak GP JSON is keyless HTTP 200 with same-day epochs / MEASURED / F §4.4 (STATUS=200 BYTES=9270/30271)
CelesTrak's usage policy permits a commercial app / UNVERIFIED / F §4.4 (usage-policy page returned HTTP 500)
Astronomy Engine for Kotlin/JVM is MIT / MEASURED / astronomy.kt:2-7 (F §2.1)
Orekit verdict is OREKIT_RISKY_USE_VALLADO_SUBSET, not "use Orekit" / MEASURED / .kiln/scratch/nv-core-proof/RESULT.md
Vallado subset is 135,064 bytes / its corpus accuracy was never measured / its licence is unestablished / MEASURED, MEASURED (absence), UNVERIFIED / F §1.2, §6.3
The naive ElevationDetector finds 1 pass where 15 exist; maxCheck does not help; segmentation fixes it / MEASURED / F §4.2 (run-passvariants.txt, run-passcrosscheck.txt; reproduced 3x)
SGP4 corpus has 4 divergences including 20413#2 (1/70 vectors, worst 27,924.0197880 km) / MEASURED / F §1.1 (corpus-tinydata.txt)
Only the Sun path is numerically verified (0.0248 arcmin); the three Moon cases are UNVERIFIED / MEASURED / F §2.1 (verbose-astro.txt)
Orekit+Hipparchus adds ~1.4-2.8 MB realistic (floor 0.9-1.2, ceiling 9.5-12), no APK produced / INFERRED (superseded by the G measurement below) / F §6
Orekit 13.1.5 + Hipparchus 4.0.3 build, shrink (R8 full mode, zero Orekit keep rules) and run on the target Android config (measured on the smoke's vendored 13.1.5; the declared pin is 13.1.9 and the measurement is re-taken at implementation) / MEASURED PASS / G-android-build-proof.md §3-§6 (ANDROID_BUILD=PASS, SIZE_MB=4.18)
The proof route was a standalone throwaway Gradle project, NOT a slot, built through scripts/lib/gradle.mjs; no contracts were faked / MEASURED / G §1 (no economics/differentiators/naming/OpenSpec change exists for it)
Zero Orekit-specific R8 keep rules were needed; proguard-rules.pro was comments only; 220/249 org.orekit.* and 49/59 org.hipparchus.* classes survived / MEASURED / G §4 (build/outputs/mapping/release/mapping.txt, 159,144 lines)
Release APK is 4,379,153 bytes (4.18 MiB), single classes.dex 1,753,644 uncompressed / 900,383 compressed; unshrunk debug APK 18,545,325 bytes / MEASURED / G §5 (ZipFile enumeration; sha256 39FAE799...E25A)
75.7% of the compressed APK payload is Orekit/Hipparchus *assets*, not code: assets/org/orekit/** + assets/org/hipparchus/** = 12,099,051 uncompressed / 3,260,714 compressed, vs 900,383 for the dex / MEASURED / G §5
Those assets (ITU-R-P.834 atmospheric tables, 1.89 MB random-generator table) are not loaded by an SGP4 job and could be excluded via packaging.resources.excludes - but that exclusion was never built or tested / INFERRED, UNVERIFIED / G §5
isShrinkResources cannot remove jar-embedded assets (they are merged into assets/ and are invisible to it) / MEASURED / G §5
On-device numerics are byte-identical to the JVM reference by exact case-sensitive string equality (elevation_deg=-44.136279 azimuth_deg=354.524835; TEME pos_km=(1280.749619,5592.447086,3605.982869)) / MEASURED / G §6 (device logcat vs run-tinydata.txt; CROSSCHECK=IDENTICAL) - proves the SGP4 path only, not the dark-window/catalogue/weather paths
Device run: serial R58MB3SGZVL, model SM-A405FM (Galaxy A40), Android 11, installed, launched, printed OREKITSMOKE_OK; zero linkage/FATAL errors in 3,908 logcat lines / MEASURED / G §6
The device event grid (1 day, 24x1h segments: 5 AOS) is NOT a reproduction of the JVM probe's (3-day unsegmented: 1 AOS) - different test setups, not one measurement; the 1-LOS divergence is INFERRED / MEASURED (counts), INFERRED (cause) / G §6
The F-map's OREKIT_RISKY_USE_VALLADO_SUBSET recommendation is weakened, not confirmed: the Android build it said had never been run has now passed / MEASURED (build), INFERRED (weakening) / G §8
The Vallado/CSSI fallback is now less necessary but was NOT chosen; its corpus numerics and its licence remain unverified / UNVERIFIED / G §7
The dedicated light-planner apps and clear-sky-chart apps carry the shelf's weakest ratings (Good To Stargaze 3.7, Clear Outside 3.5/3.8, Ouranos 3.9) / MEASURED / B §1, A §1
The paying user's demand for a *combined* brief is UNVERIFIED (no incumbent observed doing it) / UNVERIFIED / A §4-§5 (would need an in-app read of Astrospheric and Telescopius)
Revenue is modelled, not observed; no revenue API exists / UNVERIFIED / E §8; docs/monetization-owner.md
```

---

*Consolidated 2026-10-08 from the six disposable recon files under
`.kiln/scratch/positioning-2026-10-08/`. Those files were read only and left in place; they are the raw
record and were not modified or deleted. This document is a consolidation and adds no market research of
its own.*

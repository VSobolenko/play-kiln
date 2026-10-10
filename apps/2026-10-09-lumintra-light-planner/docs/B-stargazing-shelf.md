# Stargazing / planet-viewing shelf — positioning recon (2026-10-08)

Question: on the HUGE general stargazing / planet-viewing shelf (not astrophotographers), is the
dominant job **LIVE identification** (point the phone, identify what is overhead *right now*,
AR/camera-first) or **PLANNING** a future session (tonight/tomorrow, a specific target, with weather
and timing)?

Repo root: `d:\Python\PlayKiln`. All numbers below are **observed in this session**; nothing is
inferred. Unavailable = `UNVERIFIED` with the command that would settle it.

## Method (commands actually run)

- Market screen + enrichment (8 required queries, top 6, concurrency 2):
  `node scripts/enrich-market.mjs --query "stargazing" --query "star map" --query "planet finder" --query "sky guide" --query "what is in the sky tonight" --query "sky map" --query "stellarium" --query "night sky app" --top 6 --concurrency 2 --out .kiln/scratch/positioning-2026-10-08/B-raw.md`
  → raw output in `B-raw.md`. Exit 0, 48 apps enriched, 0 failures.
- Planning-shelf enrichment (4 extra queries, top 6, concurrency 2):
  `node scripts/enrich-market.mjs --query "good to stargaze" --query "astronomy planner" --query "stargazing weather forecast" --query "clear sky chart" --top 6 --concurrency 2` → 24 apps, 0 failures.
- Planning price probe: `node scripts/enrich-market.mjs --query "skysafari" --top 4 --query "clear outside" --top 4 --concurrency 2` → 8 apps, 0 failures.
- Complaints (scanner usage read from its header): `node scripts/scan-complaints.mjs --package com.google.android.stardroid --package com.vitotechnology.StarWalk2Free --package com.noctuasoftware.stellarium_free --package com.t11.skyviewfree --concurrency 2`
  and `node scripts/scan-complaints.mjs --query "stargazing" --query "planet finder" --top 6 --concurrency 2`.
- LIVE/PLANNING classification grounding: each incumbent's own Play listing long description was
  fetched from `https://play.google.com/store/apps/details?id=<pkg>&hl=en&gl=US` (browser headers,
  2 concurrent). 25 distinct apps read. This is the primary evidence for the classification table.

Note on the scanner's classifier: on the two biggest incumbents the lexical classifier matched **0**
classes — all critical rows were printed `unclassified` (MEASURED, from the run above). The
classification in §4 is therefore manual, with the verbatim texts quoted.

---

## 1. Who earns here

Observed from `enrich-market.mjs` (installs = Play's own install bucket; rating = displayed rating;
"exact" = schema.org unrounded review count). `unknown` is printed as unknown, never as `false`.

| Title | Package | Installs | Rating (reviews exact) | Ads | IAP | Price |
|---|---|---|---|---|---|---|
| Sky Map | `com.google.android.stardroid` | **50M+** | 4.1 (481,603) | no | no | free |
| Star Walk 2 Plus: Sky Map View | `com.vitotechnology.StarWalk2Free` | 10M+ | 4.5 (571,689) | yes | yes | free |
| Stellarium - Star Map | `com.noctuasoftware.stellarium_free` | 10M+ | 4.8 (337,048) | no | yes | free |
| Star Chart | `com.escapistgames.starchart` | 10M+ | 4.3 (174,326) | no | yes | free |
| Star Tracker - Mobile Sky Map | `com.PYOPYO.StarTracker` | 10M+ | 4.5 (82,345) | yes | yes | free |
| SkyView® Lite | `com.t11.skyviewfree` | 10M+ | 4.3 (58,099) | no | no | free |
| Sky Tonight - Star Gazer Guide | `com.vitotechnology.sky.tonight.map.star.walk` | 5M+ | 4.7 (85,714) | yes | yes | free |
| Star Gazer - Astronomy Sky Map | `skyview.star.map.stargazing` | 5M+ | 4.6 (50,195) | yes | yes | free |
| Star Walk - Night Sky Guide | `com.vitotechnology.StarWalk` | 1M+ | 3.5 | yes | yes | free |
| Star Roam | `com.fantasy.star.inour.sky` | 1M+ | 4.3 | yes | yes | free |
| Planet Finder | `mobile.PlanetFinder.com` | 1M+ | 3.9 (2,886) | yes | yes | free |
| Star Walk 2 Pro | `com.vitotechnology.StarWalk2` | 500K+ | 4.7 (32,541) | no | yes | **paid $2.99** |
| Astrospheric | `com.astrospheric.dfior.astrospheric` | 100K+ | 4.5 (804) | yes | yes | free |
| Clear Outside | `com.firstlightoptics.clearoutside` | 100K+ | 3.8 (1,368) | no | no | free |
| Star Guide: Night Sky Map | `com.starregistration.planetarium` | 100K+ | 4.5 (1,176) | no | yes | free |
| Star Finder | `com.starregistration.star_finder` | 100K+ | 3.5 | yes | unknown | free |
| Stargazing: Stars & Planets | `com.tvgsoft.stargazing.skyview` | 100K+ | 4.2 | unknown | unknown | free |
| **Good To Stargaze** | `com.goodtostargaze` | 50K+ | **3.7 (677)** | no | **yes** | free |
| Stargazing Hub: Astro Guide | `com.twtapp` | 10K+ | 4.4 (238) | yes | yes | free |
| Ouranos: Stargazing Forecast | `com.ouranos_app.www.twa` | 10K+ | 3.9 (318) | no | yes | free |
| Clear Outside (alt listing) | `com.flo.clearoutside` | 10K+ | 3.5 (82) | no | no | free |
| SkySafari Astronomy | `com.simulationcurriculum.skysafari7` | 10K+ | 4.3 (288) | no | yes | **paid $6.99** |
| SkySafari 7 Plus | `com.simulationcurriculum.skysafari7plus` | 10K+ | 4.4 (710) | no | yes | **paid $9.99** |
| SkySafari 7 Pro | `com.simulationcurriculum.skysafari7pro` | 10K+ | 4.0 | no | yes | **UNVERIFIED** (probe: `enrich-market.mjs --query "skysafari 7 pro" --top 4`) |
| SkySafari 8 Pro | `com.simulationcurriculum.skysafari8pro` | 1K+ | 4.2 (211) | no | yes | **paid $34.99** |
| SkySafari 8 Plus | `com.simulationcurriculum.skysafari8plus` | 1K+ | 4.3 (38) | no | yes | **paid $29.99** |
| DSO Planner (Astronomy) | `com.astro.dsoplanner` | 1K+ | 4.1 (104) | no | no | free |
| Star Wander - Night Sky Guide | `com.faintmoon.staratlas` | 10K+ | unrated | no | yes | free |

Reading: the **top of the shelf is install-massive and overwhelmingly free with ads/IAP** (Sky Map
50M+ free with no ads; Stellarium, Star Walk 2 Plus, Star Tracker, SkyView Lite, Star Chart all 10M+
free). Paid products exist and are priced high (SkySafari up to **$34.99**) but sit at **1K–10K
installs**.

---

## 2. The central question: LIVE or PLANNING

**Classification rule (stated so the counts are reproducible).** Each app's own Play listing
description was read; classification is of the *primary job the description sells*:

- **LIVE** — point the device / real-time identification is the job. Browsing the sky at another
  date ("time travel") is **not** planning: it is the same identification view, moved.
- **PLANNING** — the description sells deciding *when / where / whether* to observe **before going
  outside**: weather/cloud/seeing/transparency forecast, darkness & twilight timing, target
  visibility windows, event calendars, observation lists / session planning, light-pollution maps.
- **BOTH** — the listing sells both surfaces as products.

| Incumbent | Package | Classification | Evidence sentence from its own listing |
|---|---|---|---|
| Sky Map | `com.google.android.stardroid` | **LIVE** | "identify stars, planets, nebulae, and constellations in real-time"; "through-camera mode" |
| Stellarium | `com.noctuasoftware.stellarium_free` | **LIVE** | "in real time ... just by pointing the phone at the sky" |
| Stellarium Plus | `com.noctuasoftware.stellarium_plus` | **LIVE** | same live-point copy |
| Star Walk 2 Plus | `com.vitotechnology.StarWalk2Free` | **LIVE** | "Point your device at the night sky"; time-travel is a browse |
| Star Walk 2 Pro | `com.vitotechnology.StarWalk2` | **LIVE** | augmented reality, "orient your device toward the sky" |
| Star Walk | `com.vitotechnology.StarWalk` | **LIVE** | "identifying and observing planets ... in real time" |
| SkyView® Lite | `com.t11.skyviewfree` | **LIVE** | "uses your camera to precisely spot and identify celestial objects ... day or night" |
| Star Tracker | `com.PYOPYO.StarTracker` | **LIVE** | "hold up & point the device to the sky ... in real time" |
| Star Chart | `com.escapistgames.starchart` | **LIVE** | "just point it at the sky and the stars introduce themselves" |
| Planet Finder | `mobile.PlanetFinder.com` | **LIVE** | "when you point your device to the sky"; AR planetarium |
| Star Roam | `com.fantasy.star.inour.sky` | **LIVE** | "point your phone ... identify ... in real time"; time-travel is a browse |
| Star Finder | `com.starregistration.star_finder` | **LIVE** | "locating registered stars ... Use Augmented Reality mode" |
| Stargazing: Stars & Planets | `com.tvgsoft.stargazing.skyview` | **LIVE** | "point your phone ... identify ... in real time" |
| Star Wander | `com.faintmoon.staratlas` | **LIVE** | "point your device at the sky to instantly ... identifies ... in real-time" |
| Star Gazer - Live Sky View Map | `skyview.star.map.stargazing` | **LIVE** | "Live Sky View Map ... in the sky tonight" |
| Sky Tonight | `com.vitotechnology.sky.tonight.map.star.walk` | **BOTH** | "plan your observations" + "Point your device at the sky ... real-time" |
| Star Guide | `com.starregistration.planetarium` | **BOTH** | "plan the best moments to look up" + "Best viewing time notifications" + AR |
| SkySafari Astronomy | `com.simulationcurriculum.skysafari7` | **BOTH** | "hold your device to the sky and quickly locate" + full planetarium/observing lists |
| Stargazing Hub | `com.twtapp` | **BOTH** | "real-time star map ... observing forecasts, light pollution maps, satellite passes" |
| **Good To Stargaze** | `com.goodtostargaze` | **PLANNING** | "When will it be good to stargaze? See the weather forecast, seeing, transparency, and light pollution" |
| **Astrospheric** | `com.astrospheric.dfior.astrospheric` | **PLANNING** | "advanced weather tools ... 84 hour, hour-by-hour forecast"; "rise and set times" |
| **Ouranos** | `com.ouranos_app.www.twa` | **PLANNING** | "Starry Sky Forecasts & Astronomy Weather"; "14-day forecast to plan ... weeks in advance" |
| **Clear Outside** | `com.flo.clearoutside` | **PLANNING** | "Reliable weather forecasts for astronomers"; "Seven day hourly forecasts"; darkness periods |
| **SkySafari 7 Pro** | `com.simulationcurriculum.skysafari7pro` | **PLANNING** | "advanced planning and logging tools" |
| **DSO Planner** | `com.astro.dsoplanner` | **PLANNING** | "astronomy observation planning tool ... creating observation plans on the fly" |

**Counts (25 distinct apps whose listing was read):**

| Class | Count | Share |
|---|---|---|
| **LIVE** | **15** | 60% |
| BOTH | 4 | 16% |
| PLANNING | 6 | 24% |

**Weighted by install reach the gap is far starker than the counts.** Every app in the 10M+ and
50M+ bands is LIVE (Sky Map 50M+; Star Walk 2 Plus, Stellarium, Star Tracker, SkyView Lite, Star
Chart all 10M+). The largest **pure-PLANNING** app observed is **100K+** (Astrospheric,
Clear Outside). **The dominant job on this shelf is LIVE identification.**

---

## 3. The size of the planning gap

**By install volume:** largest LIVE app = **50,000,000+** (Sky Map); largest PLANNING app =
**100,000+** (Astrospheric). Ratio ≈ **500×**. (Derived from Play's own install buckets, MEASURED;
the buckets are lower bounds, so this is a floor on the ratio.)

**By engagement quality:** the PLANNING apps carry the shelf's weakest ratings —
Good To Stargaze **3.7** (677 reviews), Clear Outside **3.5** (82) / **3.8** (1,368), Ouranos **3.9**
(318) — against the LIVE leaders at **4.1–4.8** with 50K–570K reviews. The paying planning products
(SkySafari Pro tiers, $29.99–$34.99) sit at **1K+** installs.

**What a LIVE-first app structurally does NOT do for someone deciding *tonight or next week* before
leaving the house:**

1. **It cannot be consulted before departure.** A LIVE app's unit of output is *what is above you
   now, where you are* — Stellarium "in real time ... pointing the phone", Sky Map "in real-time",
   SkyView "your camera ... day or night". To use it you must already be outside, holding the phone
   up. The decision "should I go out at all?" is made **before** that moment, and the LIVE shelf has
   no surface for it.
2. **It answers "what am I looking at?", never "will I be able to see it?".** The go/no-go inputs —
   cloud cover, transparency, seeing, multi-day forecast, astronomical-darkness windows, a named
   target's altitude window, light pollution at a candidate site — appear **only on the PLANNING
   apps** (Astrospheric "84 hour, hour-by-hour forecast" + "sky transparency, seeing" + "rise and set
   times"; Clear Outside "Seven day hourly forecasts" + "Civil/nautical/astronomical darkness
   periods"; Ouranos "14-day forecast"; Good To Stargaze "seeing, transparency, light pollution").
   None of the LIVE leaders' listings sell them.
3. **It has no future horizon.** A LIVE app shows tonight's sky *as a sphere to point at*; it does not
   say *which* night in the next week a specific target clears the horizon during darkness, nor when
   the Moon will wash it out, nor when the next clear night is. "Time travel" on a planetarium
   (Stellarium, Star Walk 2) moves the sphere to another date but computes no weather, no timing
   decision and no session.
4. **It optimises the wrong half of the trip.** Everything the LIVE shelf buys — calibration,
   figure-8 gestures, holding the phone "square to sight" — is paid *on site*. A user who drives an
   hour to a dark site and then finds cloud, or finds the target already set, has spent the trip. An
   app that answers the pre-departure question removes that risk; the LIVE shelf cannot.

The gap is therefore not a missing feature but a **missing stage of the job**: the shelf is crowded
with "identify what is overhead" and thin on "decide before you go".

---

## 4. Complaints from the biggest incumbents (verbatim)

Scanned by `scan-complaints.mjs` (Play serves ~3 reviews per page; the sample is Play's, not a
survey — stated limit). The two biggest incumbents by installs/installed base — **Sky Map (50M+)** and
**Star Walk 2 Plus (10M+)** — plus Stellarium and SkyView for vocabulary:

**Sky Map** (`com.google.android.stardroid`, 50M+, 4.1★) — 2 of 3 read reviews critical:
- 3★ (Sep 14, 2026): *"This latest update really threw off the accuracy. I'm looking at the Moon &
  Venus, and holding my device square to my sight out at arms length, the app is showing those two
  objects 2.5-3 device widths to the right. Kind of defeats the purpose of quickly pulling up the app
  and figuring out what I'm looking at..."*
- 3★ (Aug 27, 2025, 43 helpful): *"the last several times I've tried to use it, it is WAY off, for
  example: telling me the sun is ~15° below sunrise when I'm watching the sunset.... Uninstalled
  because it was unusable."*
- 4★ (Jan 27, 2021, 137 helpful) — **a planning request**: *"The only thing that would make it better
  is if it told you when special events were happening, such as certain planets being aligned in an
  unusual way, or a good night for meteor showers, etc."*

**Star Walk 2 Plus** (`com.vitotechnology.StarWalk2Free`, 10M+, 4.5★) — 0 critical at ≤3★, but:
- 4★ (Jul 1, 2025, 245 helpful): *"the misalignment of a couple layers for deep sky objects,
  particularly with Andromeda ... it kind of ruins the immersion."*
- 4★ (Aug 22, 2026): *"Sometimes it seems to loose proper directionality ... I sometimes get
  frustrated not being able to look at things that are on the horizon line."*

**Stellarium** (`com.noctuasoftware.stellarium_free`, 10M+, 4.8★):
- 4★ (Jul 22, 2026, 11 helpful) — **the now-vs-later confusion**: *"It would be nice if it were more
  obvious ... What things were expected later in the day versus happening right now when you look at
  it in the day time."*

**SkyView® Lite** (`com.t11.skyviewfree`, 10M+, 4.3★):
- 3★ (Aug 3, 2025, 69 helpful): *"the app doesn't recognize where the moon is ... I've tried the
  figure-8 recalibration method several times, but the app still can't find the moon in the correct
  spot."*

From the weakest-incumbent query scans (`--query "stargazing"` / `"planet finder"`, 36 reviews, 22 critical):

- **Good To Stargaze** (`com.goodtostargaze`, 3.7★) — 1★: *"the app is now totally useless unless you
  pay for a subscription. A few years ago you could view the conditions 3 days out, then the..."*;
  3★: *"Expensive and the free version is pretty limited ... my main Android astronomy app supplies..."*
- **Star Walk** (`com.vitotechnology.StarWalk`, 3.5★) — 1★: *"Ads make it impossible to use! Every
  time I closed a menu I have to sit through an ad."*
- **SkySafari 7 Pro** (`com.simulationcurriculum.skysafari7pro`, 4.0★) — 1★: *"Used to work, but now
  keeps messing up. Sun, planets, and satellites all missing."*; 2★: *"Unusable. The icons at the
  bottom ... are now solid, so they have become blindingly bright, even in red mo[de]..."* (night vision)
- **Stargazing: Stars & Planets** (`com.tvgsoft.stargazing.skyview`, 4.2★) — 1★: *"it will bombard you
  with ads that are almost impossible to get rid of."*; 1★: *"Not great at all! Plus you have to
  pay??"*

**Complaint classification (manual; the scanner's lexical classifier matched 0 classes on the
top-2 — MEASURED):**

| Class | Count (of quoted rows) | Where it concentrates |
|---|---|---|
| **Accuracy / sensor drift** (calibration, wrong position) | 5 | LIVE apps (Sky Map ×2, Star Walk ×2, SkyView) |
| **Ads / paywall friction** | 4 | free LIVE apps + planning apps (Star Walk, tvgsoft, Good To Stargaze) |
| **Broken / regression after update** | 2 | SkySafari, Star Walk |
| **Missing feature — planning-ish** (event alerts, now-vs-later clarity, horizon) | 4 | Sky Map, Stellarium, Star Walk (horizon) |
| **Price / value** | 2 | Good To Stargaze (PLANNING) |

The complaint vocabulary of the *LIVE leaders* is dominated by **accuracy of the live pointing**
and **monetization friction** — i.e. the LIVE job itself is the pain point. The clearest structural
gap complaints are the *missing-planning* ones (event alerts; "expected later ... versus happening
right now"), and they come from users of the two biggest apps.

---

## 5. Evidence for / against this audience PAYING for planning (not just identification)

**Strongest evidence FOR:**

1. **Planning is an explicit paid differentiator in the most expensive product on the shelf.**
   SkySafari's own listing sells "**advanced planning and logging tools**" as a reason to buy, and
   the Pro tiers are priced **$29.99 / $34.99** (SkySafari 8 Plus / 8 Pro) and **$6.99–$9.99** for
   the base/7-Plus tiers — all MEASURED from the store. A shelf already pays a premium for planning
   software.
2. **Planning-only tools fund themselves through IAP/subscription at real scale.** Astrospheric
   (100K+, ads + IAP, 4.5★) and Good To Stargaze (50K+, IAP) are pure planning products that
   monetize; and a user of Good To Stargaze states the *paywall gates the forecast horizon* —
   *"you could view the conditions 3 days out"* — i.e. the thing people are charged for is **planning
   depth**.
3. **The biggest LIVE leader's users ask for exactly this.** Sky Map's most-helpful 4★ review
   (137 helpful) requests event alerts and "a good night for meteor showers" — a planning/reminder
   surface the 50M-install app does not have.
4. **Free planning services prove the audience is large.** Clear Outside (100K+) and Astrospheric
   (100K+) show a six-figure audience for the go/no-go question even with no marketing from the
   placement leaders.

**Strongest evidence AGAINST:**

1. **The volume is in LIVE, and it is free or ad-funded.** The 10M–50M install bands are entirely
   LIVE apps, all free (Sky Map free, no ads; the rest free with ads and/or IAP). The largest
   PLANNING app is ~100K+ — **≈500× smaller**. If the goal is reaching a huge audience, this shelf's
   hugeness is LIVE-identification hugeness, and it is monetized by ads/IAP, not by a planning
   purchase.
2. **The planning niche has the weakest ratings and the lowest installs among paid products.**
   Good To Stargaze **3.7**, Clear Outside **3.5/3.8**, Ouranos **3.9**; the $29.99–$34.99 SkySafari
   Pro tiers sit at **1K+** installs. The paying planning audience is small, demanding and hard to
   satisfy (its own users complain about price and missing features).
3. **Planning is being given away free.** Astrospheric (100K+, free + ads), Clear Outside (free),
   Ouranos (free + IAP) deliver the forecast at no upfront cost, so a paid planning app must beat a
   free one on depth, not on the existence of a forecast.
4. **A LIVE-first identity is what the giant leaders already own.** Sky Map + Star Walk + Stellarium
   + SkyView + Star Tracker + Star Chart own real-time identification with 10M–50M installs each;
   entering on the LIVE axis means competing with the shelf's incumbents head-on.

**Net:** the *money that exists* for planning is real but concentrated in a small, low-install,
price-sensitive pocket (highest observed price $34.99 at 1K+ installs; planning leaders 50K–100K).
The *hugeness* of the shelf is in LIVE identification at 10M–50M, free. The defensible wedge is not
"another AR planetarium" but the **pre-departure decision** the LIVE leaders structurally omit —
provided the revenue model does not assume the 50M-install scale that only the free LIVE apps reach.

---

## CLAIM / LABEL / EVIDENCE

```text
CLAIM / LABEL / EVIDENCE
Sky Map is 50M+ installs, 4.1 rating, 481,603 reviews, free, no ads              MEASURED enrich-market.mjs --query "star map"/"sky map" (B-raw.md)
Stellarium is 10M+, 4.8, 337,048 reviews, free with IAP                           MEASURED enrich-market.mjs --query "stargazing"/"stellarium" (B-raw.md)
Star Walk 2 Plus is 10M+, 4.5, 571,689 reviews, ads+IAP, free                    MEASURED enrich-market.mjs --query "stargazing" (B-raw.md)
SkyView Lite is 10M+, 4.3, 58,099 reviews, free, no ads, no IAP                   MEASURED enrich-market.mjs --query "stargazing"/"sky map" (B-raw.md)
Star Tracker is 10M+, 4.5, 82,345 reviews, ads+IAP, free                          MEASURED enrich-market.mjs --query "stargazing" (B-raw.md)
Star Chart is 10M+, 4.3, 174,326 reviews, free with IAP                           MEASURED enrich-market.mjs --query "stellarium" (B-raw.md)
Sky Tonight is 5M+, 4.7, 85,714 reviews, ads+IAP, free                            MEASURED enrich-market.mjs --query "stargazing"/"sky guide" (B-raw.md)
SkySafari 8 Pro is 1K+, 4.2, 211 reviews, paid $34.99                             MEASURED enrich-market.mjs --query "skysafari" --top 4
SkySafari 8 Plus is 1K+, 4.3, 38 reviews, paid $29.99                             MEASURED enrich-market.mjs --query "skysafari" --top 4
SkySafari 7 Plus is 10K+, 4.4, 710 reviews, paid $9.99                            MEASURED enrich-market.mjs --query "skysafari" --top 4
SkySafari Astronomy is 10K+, 4.3, 288 reviews, paid $6.99                         MEASURED enrich-market.mjs --query "astronomy planner"/"clear sky chart"
SkySafari 7 Pro is 10K+, 4.0 rating, IAP, no ads                                  MEASURED scan-complaints.mjs --query "stargazing" --top 6
SkySafari 7 Pro price                                                             UNVERIFIED enrich-market.mjs --query "skysafari 7 pro" --top 4
Astrospheric is 100K+, 4.5, 804 reviews, ads+IAP, free                            MEASURED enrich-market.mjs --query "stargazing weather forecast"
Clear Outside (com.firstlightoptics.clearoutside) is 100K+, 3.8, 1,368 reviews    MEASURED enrich-market.mjs --query "clear outside" --top 4
Clear Outside (com.flo.clearoutside) is 10K+, 3.5, 82 reviews                     MEASURED enrich-market.mjs --query "stargazing weather forecast"/"clear outside"
Good To Stargaze is 50K+, 3.7, 677 reviews, IAP, free                             MEASURED enrich-market.mjs --query "good to stargaze"/"stargazing weather forecast"
Ouranos is 10K+, 3.9, 318 reviews, IAP, free                                      MEASURED enrich-market.mjs --query "stargazing weather forecast"
DSO Planner is 1K+, 4.1, 104 reviews, free                                        MEASURED enrich-market.mjs --query "astronomy planner"
Stargazing Hub is 10K+, 4.4, 238 reviews, ads+IAP, free                           MEASURED enrich-market.mjs --query "good to stargaze"/"stargazing weather forecast"
Star Guide is 100K+, 4.5, 1,176 reviews, IAP, free                                MEASURED enrich-market.mjs --query "sky guide"
15 of 25 described incumbents are LIVE-first, 6 PLANNING, 4 BOTH                  MEASURED each listing description fetched from play.google.com/store/apps/details (25 packages)
Every 10M+ and the 50M+ app on this shelf is LIVE                                 MEASURED enrich-market.mjs runs above, cross-read against the classification table
Largest LIVE app 50M+ vs largest PLANNING app 100K+ = ~500x install gap           MEASURED 50,000,000 / 100,000 from Play's install buckets (lower bounds)
Stellarium sells live pointing and any-date simulation, no forecast               MEASURED Stellarium listing long description (play.google.com)
SkyView Lite uses the camera to identify day or night                             MEASURED SkyView Lite listing long description (play.google.com)
Good To Stargaze sells weather forecast, seeing, transparency, light pollution    MEASURED Good To Stargaze listing long description (play.google.com)
Astrospheric sells 84-hour hourly forecast, transparency, seeing, rise/set times  MEASURED Astrospheric listing long description (play.google.com)
Ouranos sells a 14-day forecast "to plan ... weeks in advance"                    MEASURED Ouranos listing long description (play.google.com)
Clear Outside sells seven-day hourly forecasts and darkness periods               MEASURED Clear Outside listing long description (play.google.com)
SkySafari 7 Pro sells "advanced planning and logging tools"                      MEASURED SkySafari 7 Pro listing long description (play.google.com)
Sky Map's top-helpful review asks for event alerts / good meteor-shower nights    MEASURED scan-complaints.mjs --package com.google.android.stardroid (verbatim, 137 helpful)
Sky Map critical reviews are accuracy/regression of live pointing                 MEASURED scan-complaints.mjs --package com.google.android.stardroid
Stellarium review asks for clarity of "later ... versus happening right now"      MEASURED scan-complaints.mjs --package com.noctuasoftware.stellarium_free (verbatim)
SkyView review reports the app "can't find the moon in the correct spot"          MEASURED scan-complaints.mjs --package com.t11.skyviewfree (verbatim)
Good To Stargaze user: paywall gates the 3-day forecast horizon                  MEASURED scan-complaints.mjs --query "stargazing" --top 6 (verbatim)
Star Walk (3.5*) review: "Ads make it impossible to use"                          MEASURED scan-complaints.mjs --query "stargazing" --top 6 (verbatim)
The scanner's lexical classifier matched 0 classes on the top-2 incumbents        MEASURED scan-complaints.mjs --package com.google.android.stardroid ... (all rows "unclassified")
Play serves ~3 reviews per page, so the complaint sample is not a survey          ON RECORD scripts/scan-complaints.mjs header ("measured: 3")
The shelf's dominant job is LIVE identification                                   INFERRED from the 15/25 LIVE count + every 10M+ app being LIVE (both MEASURED above)
A 50M-install-scale audience would pay for planning                                UNVERIFIED no priced planning app observed above 100K+ installs; needs a user-spend reading, not a shelf read
```

Report ends.

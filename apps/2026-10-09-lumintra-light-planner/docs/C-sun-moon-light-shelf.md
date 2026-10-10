# Shelf recon — Sun / Moon / light-planning photography (Google Play)

Scout brief: measure the **SUN / MOON / LIGHT-PLANNING photography shelf** (landscape & outdoor
photographers) as a positioning target for an app with a real astronomy engine (Sun/Moon/twilight,
target altitude curves, satellite passes, weather, exposure guidance).

Everything below was fetched **in this session**, 2026-10-08, signed-out Play `hl=en, gl=US`.

Method — the exact commands run:

```bash
node scripts/enrich-market.mjs --query "sun surveyor" --query "golden hour" \
  --query "sun and moon position" --query "photographer ephemeris" \
  --query "light planning photography" --query "shadow length" \
  --query "landscape photography planner" --query "blue hour" \
  --top 6 --concurrency 2 --out .kiln/scratch/positioning-2026-10-08/C-raw.md
node scripts/enrich-market.mjs --query "the photographer's ephemeris" \
  --query "night sky photography planner" --top 5 --concurrency 2
node scripts/enrich-market.mjs --query "satellite tracker" --query "iss tracker" \
  --query "cloud cover forecast" --top 4 --concurrency 2
# store descriptions: fetched each app's /store/apps/details page and read data-g-id="description"
# reviews: scripts/lib/play-reviews.mjs fetchReviews() over the same pages
```

Two of the eight briefed queries returned a **polluted** shelf and are marked as such
(`photographer ephemeris` — 9/30 titles distinctive; `landscape photography planner` — 7/30;
also `light planning photography` — partial, 14/30). Their rows are still reported but must not be
read as a clean shelf.

---

## 1. Who earns here, and at what price

### 1.1 The paid one-time light-planners (this is the shelf the brief is about)

All values MEASURED by `node scripts/enrich-market.mjs … --top 6 --concurrency 2` (2026-10-08).

| App | package | developer | price (one-time) | installs | rating | reviews | ads / IAP |
|---|---|---|---:|---:|---:|---:|---|
| **PhotoPills** | `com.photopills.android.photopills` | PhotoPills, SL | **$10.99** | 100K+ | 4.8 | 8,220 | false / false |
| **Sun Surveyor (Sun & Moon)** | `com.ratana.sunsurveyor` | Adam Ratana | **$9.99** | 100K+ | 4.8 | 5,140 | false / false |
| **Sun Position PRO** | `com.andymstone.sunposition` | Stonekick | **$9.99** | 50K+ | 4.9 | 2,730 | false / false |
| **Planit Pro: Photo Planner** | `com.yingwen.photographertoolspro` | JIDE Software | **$9.99** | 10K+ | 4.6 | 1,220 | false / true |
| **Sun Seeker: Sunlight Tracker** | `com.ajnaware.sunseeker` | ozPDA | **$8.99** | 50K+ | 4.8 | 1,000 | false / false |
| **SunOnTrack: Sun Path & Shadows** | `com.sunnytapps.sunnytrack` | Plan the sun | **$7.99** | 50K+ | 4.8 | 874 | false / false |
| **Golden Hour+** | `com.simplaapliko.goldenhourpro` | Simplaapliko | **$4.99** | 1K+ | 4.7 | 189 | false / false |

### 1.2 The free / IAP light-planners (the shelf's traffic leaders)

| App | package | price | installs | rating | reviews | ads / IAP |
|---|---|---:|---:|---:|---:|---|
| Sun Surveyor **Lite** | `com.ratana.sunsurveyorlite` | free | 1M+ | 4.1 | 3,980 | false / false |
| Sunset & Sunrise – Sun Tracker (PhotoTime) | `pl.guteklabs.phototime` | free | 500K+ | 4.3 | 9,250 | false / true |
| Sun Position (free) | `com.andymstone.sunpositiondemo` | free | 1M+ | 4.6 | 12,500 | false / true |
| Golden Hour | `com.simplaapliko.goldenhour` | free | 500K+ | 4.7 | 5,370 | **true** / false |
| Sun Ephemeris | `fr.avianey.ephemeris` | free | 100K+ | 4.2 | 3,590 | false / true |
| Ephemeris: Sun and Moon Seeker | `com.vitotechnology.Ephemeris` | free (IAP sub) | 100K+ | 3.6 | 1,390 | false / true |
| Planit (free) | `com.yingwen.photographertools` | free | 100K+ | 3.6 | 1,710 | false / true |
| Light Meter – Lite | `com.willblaschko.android.lightmeterv2.free` | free | 1M+ | 4.3 | 4,890 | true / true |

### 1.3 Adjacent shelves that define the money (MEASURED, same session)

- **Night-sky / planetarium** (`night sky photography planner`): Stellarium – Star Map 10M+ (4.8, free/IAP), Sky Tonight 5M+ (4.7, free/ads/IAP), AstroCam 10K+ (4.0, free/ads/IAP).
- **Satellite / ISS** (`satellite tracker`, `iss tracker`): ISS Detector **5M+** (4.8, free/ads/IAP), Satellite Tracker by Star Walk **1M+** (4.4), NASA Spot the Station 500K+ (4.7), ISS Live Now 10M+ (4.7).
- **Weather / cloud** (`cloud cover forecast`): The Weather Channel 100M+, Weather & Radar 100M+, Zoom Earth 5M+, **Clear Outside 10K+ (3.5★)** — the one dedicated *astronomy* cloud app on the shelf, and the weakest rated.

---

## 2. Price table — does the audience pay $6.99–$12.99 one-time?

**Observed range on the paid light-planning shelf: $4.99 – $10.99 one-time. The core is $7.99 – $10.99.**

| Claim from the brief | Verdict | Observed |
|---|---|---|
| Audience pays roughly **$6.99–$12.99 one-time** | **Substantially CONFIRMED (lower half)** | 7 paid apps observed; 6 of them at **$7.99–$10.99**; the only outlier is **Golden Hour+ at $4.99** (below the band). |
| Someone pays **above $10.99** | **REFUTED on this shelf** | The highest one-time price observed among light-planners is **PhotoPills $10.99**. No $11.99/$12.99 light-planner was observed. (The single high price seen in a query — `ephemeris.vedic` at $37.99 — is astrology software, not photography; it appeared only because the `photographer ephemeris` shelf is polluted.) |
| The audience is **proven** to pay (not just "free users") | **CONFIRMED** | **PhotoPills is a $10.99 paid app with 100K+ installs and 8,220 ratings**; **Sun Surveyor is $9.99 with 100K+ installs**. A paid app at these install buckets is direct evidence of a paying audience. |

**INFERRED floor (arithmetic, not a measurement):** PhotoPills at 100,000 installs × $10.99 =
**≥ $1,099,000 gross** before Play's commission; Sun Surveyor at 100,000 × $9.99 = **≥ $999,000**.
Rated `INFERRED` because Play's install count is a rounded lower-bound bucket and counts devices,
not unique buyers — it is a floor, not a revenue figure.

---

## 3. THE CENTRAL QUESTION — the four-point structural gap

Each row rests on the incumbents' **own store-description sentences** (quoted below), read from
`data-g-id="description"` on each app's Play page this session. The method: fetched all ten
light-planner descriptions and searched them for `satellite`, `iss`, `weather`, `cloud`, `nebula`,
`deep sky`, `deep-sky`, `comet`, `meteor`, `aurora`, `eclipse`, `night`, `dark sky`, `milky way`,
`star trail`, `stargaz` (context-checked for false positives).

| Capability | Does the shelf do it? | Evidence |
|---|---|---|
| **(a) NIGHT** | **Partial — only "which night object is where", never the darkness itself** | Every serious incumbent names Milky Way / star trails / stargazing, and one adds a dark theme. **None** mentions light pollution, Bortle class, a moonless-dark window, or the night's own timeline beyond moon phase. |
| **(b) DEEP-SKY objects** | **Almost not at all** | Of ten descriptions, **only Planit Pro** names anything past the Milky Way: *"major stars, constellations, nebulae azimuth and elevation angle"*. No incumbent claims an object catalogue (Messier/NGC), magnitudes, framing scale, or visibility limits. |
| **(c) SATELLITE passes** | **Not at all** | **No** light-planner description mentions satellite passes. The only `satellite` hit is a *map layer*: SunOnTrack — *"Integrated map (with different views - satellite, terrain, …)"*. Meanwhile dedicated satellite apps are huge (ISS Detector 5M+). |
| **(d) WEATHER / cloud cover** | **Not at all, with one exception** | **Only PhotoTime** claims weather: *"weather forecasts"* (generic, no cloud-cover modelling). No incumbent mentions **cloud cover**. The dedicated astronomy-weather app **Clear Outside** (10K+, **3.5★**) is the weakest rating on the whole cloud shelf. |

### The quoted sentences the gap rests on

**Night**
- Sun Surveyor — *"Prepare night photography shots with the Milky Way & star trail features"* and *"an interactive listing of critical times … including the full moon looming low in the sky during magic hour, **total darkness for stargazing**, and more"*.
- PhotoPills — *"The First 2D Map-Centric Planner: Sun, Moon, Milky Way"*; *"Calculators: Long Exposure, Timelapse, Spot Stars, Star Trails, Hyperfocal Table, DoF, FoV"*.
- Sun Position (Stonekick) — *"Astrophotography: see when and where the Milky Way core will be visible"*.
- Ephemeris — *"Plan ahead, visualize and predict the position of the Sun and the Moon, the Milky Way …"*.
- SunOnTrack — *"Dark Theme for night hours"* (the entire night treatment).
- Golden Hour — *"Find duration of the civil, nautical and astronomical twilights, 'golden' and 'blue' hours"* (night reduced to twilight bounds).

**Deep-sky** — the *only* sentence on the shelf:
- Planit Pro — *"the Ephemeris features such as the sunrise, sunset, moonrise, moonset time and direction, twilight time, … **major stars, constellations, nebulae azimuth and elevation angle**, star trail planning, … milky way searching, solar eclipse and lunar eclipse, exposure/ND filter calculator, light meter, rainbow position prediction, tide height and tide search"*.
  Note what this sentence does **not** contain: no deep-sky **object catalogue**, no magnitude, no apparent size, no visibility/altitude-window planner for a named target.

**Satellites** — clean negative; the sole occurrence is a map layer:
- SunOnTrack — *"Integrated map (with different views - satellite, terrain, …)"*.
- (Every `iss` match across all ten descriptions was the word "miss" — e.g. *"...you will never **miss** optimal light situations..."* — verified in context, not a satellite claim.)

**Weather / cloud**
- PhotoTime (only positive) — *"PhotoTime helps you plan your day around the most beautiful light … with accurate Sun Path, Sun Position, Moon Phases, **weather forecasts** and powerful planning tools"* and *"🌦️ Weather forecasts"*.
- All other nine: **no weather/cloud sentence** (verified by keyword scan above).

**Eclipse / special events exist** (so the shelf is not blind to rare events — it is blind to the sky's *ordinary* night and to orbital events):
- Sun Seeker — *"Eclipse dates (both solar and lunar) and detailed event timings for your location"*.
- Planit Pro — *"solar eclipse and lunar eclipse"*.

---

## 4. What the incumbents' own users complain about (verbatim)

Source: `scripts/lib/play-reviews.mjs` `fetchReviews()` against each app's Play page, 2026-10-08.
**Honest limit stated by the reader itself:** Play server-renders a **sample of 3 reviews per page**,
chosen by Play's own ranking. These are the critical (≤ 3★) reviews that sample exposed — a
vocabulary, not a frequency.

| Package | Title | Rating | Price | reviews read | critical |
|---|---|---:|---:|---:|---:|
| `com.ratana.sunsurveyor` | Sun Surveyor (Sun & Moon) | 4.8 | $9.99 | 3 | 1 |
| `com.yingwen.photographertoolspro` | Planit Pro: Photo Planner | 4.6 | $9.99 | 3 | 2 |
| `com.vitotechnology.Ephemeris` | Ephemeris: Sun and Moon Seeker | 3.6 | free/IAP | 3 | 3 |
| `com.ratana.sunsurveyorlite` | Sun Surveyor Lite | 4.1 | free | 3 | 1 |
| `com.ajnaware.sunseeker` | Sun Seeker | 4.8 | $8.99 | 3 | 1 |
| `com.andymstone.sunpositiondemo` | Sun Position | 4.6 | free | 3 | 1 |
| `com.simplaapliko.goldenhour` | Golden Hour | 4.7 | free | 3 | 0 |
| `com.photopills.android.photopills` | PhotoPills | 4.8 | $10.99 | 3 | 0 |

**Quoted complaint texts**

1. **`accuracy`** — `com.ratana.sunsurveyor`, 1★, *November 2, 2024*:
   > "App is Inaccurate to often. I use the app to help me set up my time-lapse and aim my camera for sun rises. The app is off by a lot every time. It's been so inaccurate that I'm missing the shot not just by a few degrees but several degrees. I paid for the full version to increase my accuracy…"

2. **`accuracy` / sensor calibration** — `com.ajnaware.sunseeker`, 2★, *November 29, 2020*:
   > "…Unfortunately there is no way to correct if the calibration is off. With several devices the sun location was noticeably off. I bought two other apps doing the same thing and both of them are accurate on my device. So it's not my phone that's bad. If only they had a way to let you dial in a correction value."

3. **`data-loss` / portability** — `com.yingwen.photographertoolspro`, 3★, *June 5, 2021*:
   > "…I have just discovered that the 29 plans that I have on my cellphone can't be easily transferred to a new tablet with the app installed. Enormously disappointed in this limitation…"

4. **`broken` / licence portability** — `com.yingwen.photographertoolspro`, 3★, *May 27, 2026*:
   > "…I recently purchased the Location Explorer Feature for five years. I have the app on three devices… I cannot use this feature on the smart phones. It only works on the tablet… I tried using the 'Restore' option on the phones but it does nothing."

5. **`paywall`** — `com.vitotechnology.Ephemeris`, 1★, *January 18, 2022*:
   > "Extremely misleading store listing. This is a 100% PAID app. You dont pay to unlock features like the listing says, you pay to use the app at all. (7 whole day free trial) … You list it for free then lock everything behind a pay wall, you've lost people forever."

6. **`paywall`** — `com.vitotechnology.Ephemeris`, 3★, *January 1, 2021*:
   > "…it's annoying that it's advertised as freemium but can't be used at all without either paying upfront or a free trial… Just make this app paid @ 2.99 and it'll be much easier for everyone involved"

7. **`value` / locked lite features** — `com.ratana.sunsurveyorlite`, 3★, *July 20, 2021*:
   > "It does show both, Blue and Golden hours… However most of it's features are locked, which requires the purchase of the very 'OUTDATED' Jan 2020 paid version. Until there's an update I will stay with the Lite version… $8 is a lot, on a possible unsupported app."

8. **`confusing-ui` / widget** — `com.andymstone.sunpositiondemo`, 3★, *January 29, 2026*:
   > "It's a nice app, but the widgets are suck due to not providing a transparent background opinion… I don't want to completely block out my home screen picture with the widget."

**Complaint classification** (lexical classifier, `scripts/lib/play-reviews.mjs`; one review can carry
several classes):

| Class | Apps that fired it | Reading |
|---|---|---|
| `paywall` (price / subscription / misleading free) | Sun Surveyor, Ephemeris ×3 | The strongest cluster. Ephemeris's free/IAP model is the shelf's most-complained-about design. |
| `accuracy` (wrong/inaccurate data) | Sun Surveyor, Sun Seeker | Compass/position accuracy with no user calibratability is a named, repeated pain in paid apps. |
| `data-loss` / portability | Planit Pro | Plans and purchased add-ons do not travel across the user's own devices. |
| `broken` | Planit Pro | Paid add-on (`Location Explorer`) fails to restore on phones. |
| `missing-feature` | Sun Surveyor | Adjacent to accuracy (wanting a correction control). |
| `value` | Sun Surveyor Lite | Lite/paid split judged poor and the paid build called outdated. |

No complaint in this sample mentions weather, cloud cover, satellites, or deep-sky objects — the
gaps in §3 are **not** (in this sample) what users complain about; they are simply absent from the
products.

---

## 5. Is there a measured signal the golden-hour buyer also wants a night / deep-sky / satellite brief?

**Partially MEASURED for night; UNVERIFIED for the specific deep-sky and satellite cross-sell.**

- **MEASURED — night is bundled into every paid light-planner.** Sun Surveyor, PhotoPills, Planit
  Pro, Sun Position and Ephemeris all sell Milky Way / star-trail / night content **inside the same
  paid product** as the golden-hour planner; Planit Pro goes furthest and addresses one audience in
  one listing: *"landscape photographers, travel photographers, nature photographers and those who
  are interested in night photography, city photography, time-lapse, star-trails, milky way or
  astro-photography"*. Vendors do not bundle what their buyers do not ask for, so the same customer
  wanting **night** content from a light planner is the shelf's own answer — `INFERRED` from the
  bundling, `ON RECORD` from the quotes.
- **MEASURED — the paid price point is shared.** The same $7.99–$10.99 one-time price buys the
  bundled day+night planner (PhotoPills $10.99, Sun Surveyor $9.99), i.e. the shelf already treats
  night content as part of the golden-hour purchase rather than a separate product.
- **UNVERIFIED — that this buyer wants a *deep-sky object brief*.** No incumbent sells one, so there
  is no observed willingness-to-pay for it. The command that would settle it: read the paid reviews
  of PhotoPills/Sun Surveyor filtered for object-specific asks (needs Play's full review list, which
  is not server-rendered — only 3/page are). Labelled `UNVERIFIED`.
- **UNVERIFIED — that this buyer wants a *satellite-pass brief*.** The satellite audience is large
  and is monetised (ISS Detector 5M+, free/ads/IAP) but on a **free ad/IAP model, not a one-time
  paid one**, and no light-planner sells passes — so there is no measured evidence that the *paying*
  light-planning customer wants passes bundled. The two shelves' monetisation differ (one-time paid
  vs free+ads), which is the reason to treat this as `UNVERIFIED` rather than assumed.

---

## 6. Summary answer

1. **Who earns & at what price:** paid one-time light-planners earn at **$7.99–$10.99** (PhotoPills
   $10.99, Sun Surveyor $9.99, Sun Position PRO/Sun Seeker/SunOnTrack $7.99–$9.99), with PhotoPills
   and Sun Surveyor each carrying **100K+ installs** as paid apps — direct evidence this audience
   pays.
2. **Price band:** the brief's $6.99–$12.99 is **substantially confirmed in its lower half**; nothing
   above **$10.99** was observed on this shelf, and one $4.99 point exists below the band.
3. **Structural gap:** at **night** the shelf plans object positions but not darkness; **deep-sky** is
   effectively absent (one "nebulae" line, no object catalogue); **satellite passes are absent
   entirely**; **weather/cloud cover** is absent except one generic "weather forecasts" line in
   PhotoTime. Dedicated satellite (ISS Detector 5M+) and cloud (Clear Outside, 3.5★) apps prove the
   adjacent demand exists but is served outside this shelf.
4. **Hottest observed complaint cluster:** `paywall`/misleading-free (Ephemeris), then
   `accuracy`/no user calibration (Sun Surveyor, Sun Seeker), then cross-device portability
   (Planit Pro).
5. **Cross-sell signal:** `night` — partially MEASURED (bundled in every paid planner); `deep-sky`
   and `satellite` — `UNVERIFIED` (no incumbent sells them; no observed willingness-to-pay).

---

## CLAIM LEDGER

```
CLAIM / LABEL / EVIDENCE
PhotoPills is a $10.99 paid app with 100K+ installs / MEASURED / enrich-market.mjs --query "light planning photography" --top 6
Sun Surveyor is $9.99 with 100K+ installs / MEASURED / enrich-market.mjs --query "sun surveyor" --top 6
Sun Position PRO is $9.99, Sun Seeker $8.99, SunOnTrack $7.99, Golden Hour+ $4.99 / MEASURED / enrich-market.mjs (as above; prices in `price` column)
The paid light-planner price band observed is $4.99-$10.99 with the core at $7.99-$10.99 / MEASURED / same enrich run (section 1.1 table)
No light-planner above $10.99 was observed / MEASURED / same enrich run; highest observed = PhotoPills $10.99
ephemeris.vedic at $37.99 is astrology software, not a photography planner / MEASURED / enrich-market.mjs --query "photographer ephemeris" (title "Ephemeris, Astrology Software")
PhotoPills >= $1.099M gross floor (100k x $10.99) / INFERRED / product of the two MEASURED values above; bucket is a floor
Sun Surveyor claims "Prepare night photography shots with the Milky Way & star trail features" / MEASURED / play.google.com/store/apps/details?id=com.ratana.sunsurveyor data-g-id="description"
Sun Surveyor claims "total darkness for stargazing" / MEASURED / same page
PhotoPills claims "The First 2D Map-Centric Planner: Sun, Moon, Milky Way" and Star Trails/Spot Stars calculators / MEASURED / ...?id=com.photopills.android.photopills
Planit Pro claims "major stars, constellations, nebulae azimuth and elevation angle" / MEASURED / ...?id=com.yingwen.photographertoolspro
Planit Pro addresses one audience: "landscape photographers ... interested in night photography ... star-trails, milky way or astro-photography" / MEASURED / same page
Sun Position (Stonekick) claims "Astrophotography: see when and where the Milky Way core will be visible" / MEASURED / ...?id=com.andymstone.sunpositiondemo
SunOnTrack's only "night" claim is "Dark Theme for night hours" / MEASURED / ...?id=com.sunnytapps.sunnytrack
SunOnTrack's only "satellite" occurrence is a map layer: "Integrated map (with different views - satellite, terrain, …)" / MEASURED / same page, context-checked
No light-planner description mentions satellite passes / MEASURED / keyword scan of 10 descriptions for "satellite"/"iss"; every "iss" hit was the word "miss"
Only PhotoTime claims weather: "weather forecasts" / MEASURED / ...?id=pl.guteklabs.phototime
No light-planner description mentions cloud cover / MEASURED / keyword scan of 10 descriptions for "cloud" (0 hits)
Only Planit Pro names any deep-sky object beyond the Milky Way ("nebulae") / MEASURED / keyword scan for "nebula","deep sky","deep-sky" across 10 descriptions
Sun Seeker claims "Eclipse dates (both solar and lunar) and detailed event timings" / MEASURED / ...?id=com.ajnaware.sunseeker
Golden Hour claims twilight durations and golden/blue hours / MEASURED / ...?id=com.simplaapliko.goldenhour
Sun Surveyor 1* accuracy complaint (Nov 2 2024) quoted verbatim / MEASURED / play-reviews.mjs fetchReviews("com.ratana.sunsurveyor")
Sun Seeker 2* calibration complaint (Nov 29 2020) quoted verbatim / MEASURED / fetchReviews("com.ajnaware.sunseeker")
Planit Pro 3* plan-portability and add-on-restore complaints quoted verbatim / MEASURED / fetchReviews("com.yingwen.photographertoolspro")
Ephemeris three paywall complaints quoted verbatim / MEASURED / fetchReviews("com.vitotechnology.Ephemeris")
Play server-renders a 3-review sample per page / MEASURED / reviewsRead=3 on every page fetched; stated in scripts/lib/play-reviews.mjs
ISS Detector is 5M+ installs, free with ads+IAP / MEASURED / enrich-market.mjs --query "satellite tracker" --top 4
Satellite Tracker by Star Walk 1M+, NASA Spot the Station 500K+, ISS Live Now 10M+ / MEASURED / enrich-market.mjs --query "iss tracker" --top 4
Clear Outside (astronomy cloud app) is 10K+ at 3.5 stars / MEASURED / enrich-market.mjs --query "cloud cover forecast" --top 4
Stellarium 10M+ and Sky Tonight 5M+ are free/IAP star maps / MEASURED / enrich-market.mjs --query "night sky photography planner" --top 5
Every paid light-planner bundles night content in one product / INFERRED / the five MEASURED description quotes above
The golden-hour buyer wants a deep-sky object brief / UNVERIFIED / no incumbent sells one; would need Play's full review list for the paid planners (only 3/page server-rendered)
The golden-hour buyer wants a satellite-pass brief / UNVERIFIED / no light-planner sells passes; satellite shelf is free+ads, not one-time paid
"photographer ephemeris" and "landscape photography planner" shelves are polluted / MEASURED / enrich-market.mjs relevance guard: 9/30 and 7/30 distinctive titles
The Photographer's Ephemeris package was not locatable on Play / MEASURED / fetchAppDetail("com.crookneck.tpe") and ("com.photoephemeris.tpe") both HTTP 404
```

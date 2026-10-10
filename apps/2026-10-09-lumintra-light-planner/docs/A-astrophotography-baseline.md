# A — Baseline shelf: planning a night astrophotography session

Scope: the MARKET shelf for people who plan a night astrophotography session. No factory-built app
is referenced anywhere in this document.

Instruments run this session (Windows / PowerShell, repo root `d:\Python\PlayKiln`):

```
node scripts/enrich-market.mjs --query "astrophotography planner" --query "night sky planner" \
  --query "deep sky photography" --query "milky way planner" --query "star tracker app" \
  --query "astro planner" --top 6 --concurrency 2
node scripts/enrich-market.mjs --query "DSO Planner astronomy" --query "Telescopius" \
  --query "Astroweather astronomy tools" --top 3 --concurrency 2
node scripts/scan-titles.mjs --query "astrophotography planner" --query "night sky planner" \
  --query "milky way planner" --concurrency 2
node scripts/scan-complaints.mjs --package de.seebi.deepskycamera --package de.seebi.deepskycamera.pro \
  --package com.strykeout.darkskies2 --package com.nimscraze.astrophotography_planner --concurrency 2
node scripts/scan-complaints.mjs --package de.seebi.deepskycamera --package de.seebi.deepskycamera.pro \
  --concurrency 2 --json
```

No `--out` was passed (the brief also forbids writing anything but this one report), so the raw
tool output lives in this session's terminal log rather than on disk.

---

## 1. Who earns on this shelf

Enriched per-app facts (Play listing detail pages), all `MEASURED` by `enrich-market.mjs` this
session. Installs are Play's own display band; `≥` is the band's numeric lower bound, not a count.
Rating "unrated" is Play's own state (no rating shown), not zero.

| Title | Package | Installs | Price | Rating (reviews) | Ads | IAP |
|---|---|---|---|---|---|---|
| Sky Map | `com.google.android.stardroid` | 50M+ (≥50,000,000) | free | 4.1 (481,603) | false | false |
| Stellarium - Star Map | `com.noctuasoftware.stellarium_free` | 10M+ (≥10,000,000) | free | 4.8 (337,048) | false | true |
| Star Walk 2 Plus: Sky Map View | `com.vitotechnology.StarWalk2Free` | 10M+ (≥10,000,000) | free | 4.5 (571,689) | true | true |
| Star Tracker - Mobile Sky Map | `com.PYOPYO.StarTracker` | 10M+ (≥10,000,000) | free | 4.5 (82,345) | true | true |
| SkyView® Lite | `com.t11.skyviewfree` | 10M+ (≥10,000,000) | free | 4.3 (58,099) | false | false |
| Sky Tonight - Star Gazer Guide | `com.vitotechnology.sky.tonight.map.star.walk` | 5M+ (≥5,000,000) | free | 4.7 (85,714) | true | true |
| Star Gazer - Astronomy Sky Map | `skyview.star.map.stargazing` | 5M+ (≥5,000,000) | free | 4.6 (50,189) | true | true |
| Moonly: Moon Phases & Calendar | `com.moonly.android` | 1M+ (≥1,000,000) | free | 4.0 (28,865) | true | true |
| DeepSkyCamera | `de.seebi.deepskycamera` | 1M+ (≥1,000,000) | free | 3.1 (781) | false | false |
| SkyPic - Sky Photo Editor | `sky.photo.editor.filters.skypic` | 1M+ (≥1,000,000) | free | 4.1 (17,413) | true | true |
| Star Guide: Night Sky Map | `com.starregistration.planetarium` | 100K+ (≥100,000) | free | 4.5 (1,176) | false | true |
| **Astrospheric** | `com.astrospheric.dfior.astrospheric` | **100K+ (≥100,000)** | free | 4.5 (804) | true | true |
| **PhotoPills** | `com.photopills.android.photopills` | **100K+ (≥100,000)** | **paid $10.99** | 4.8 (8,216) | false | false |
| **Good To Stargaze** | `com.goodtostargaze` | **50K+ (≥50,000)** | free | 3.7 (677) | false | true |
| Planit Pro: Photo Planner | `com.yingwen.photographertoolspro` | 10K+ (≥10,000) | paid $9.99 | 4.6 (1,220) | false | true |
| **Telescopius** | `com.telescopius.twa` | 10K+ (≥10,000) | free | 4.5 (94) | true | false |
| **Astroweather - astronomy tools** | `cc.meowssage.astroweather` | 10K+ (≥10,000) | free | 4.2 (113) | true | true |
| **Mobile Observatory Pro** | `com.zima.mobileobservatorypro` | 10K+ (≥10,000) | **paid $11.99** | 4.6 (5,681) | false | true |
| AstroCam: Night Sky Camera | `com.amnlapps.astrocam` | 10K+ (≥10,000) | free | 4.0 (370) | true | true |
| Dark Skies: Astrophotography | `com.strykeout.darkskies2` | 500+ (≥500) | free | unrated | false | true |
| **DSO Planner (Astronomy)** | `com.astro.dsoplanner` | 1K+ (≥1,000) | free | 4.1 (104) | false | false |
| DeepSkyCamera Pro | `de.seebi.deepskycamera.pro` | 1K+ (≥1,000) | **paid $9.49** | 3.9 (98) | false | false |
| Astrophotography Planner | `com.nimscraze.astrophotography_planner` | 100+ (≥100) | free | unrated | true | true |
| Milky Way Tonight | `com.jackfusco.milkywaytonight` | 100+ (≥100) | paid $7.99 | unrated | false | false |
| Nightlog: Milky Way Planner | `com.grothgalleries.nightlog` | 10+ (≥10) | free | unrated | false | false |
| Astro Planner | `com.astronomia.astro_planner` | 5+ (≥5) | paid $4.99 | unrated | false | false |

The shelf is **three populations wearing one query**:

- **Free star-map / planetarium apps** — enormous (5M–50M installs), almost all free, monetised by a
  mix of ads and in-app purchases, ratings 4.0–4.8. They answer "what is up there", not "should I
  drive out tonight".
- **Astro conditions / target planners** — the actual *night-planning* job: Astrospheric (100K+),
  Good To Stargaze (50K+), Telescopius (10K+), Astroweather (10K+), DSO Planner (1K+). Free, ad- or
  IAP-supported, ratings 3.7–4.5.
- **Dedicated astrophotography capture apps** — DeepSkyCamera (1M+ but 3.1, and **no ads and no
  IAP declared**), AstroCam (10K+), DeepSkyCamera Pro ($9.49, 1K+). This is the camera/stacking half,
  not the planning half.

## 2. Per-title job classification

Titles are not jobs. Classified by what the app is for, from the visible result set
(`scan-titles.mjs`) plus the enriched listings:

| Job (what the app does) | Titles observed |
|---|---|
| **Interactive star map / planetarium** (identify and browse the sky) | Stellarium, Sky Map, Star Walk 2, Star Tracker, SkyView Lite, Star Gazer, Sky Tonight, Star Guide, Star Chart, SkySafari, SkEye, Cosmic Watch, TheSkyLive, Star Roam |
| **Tonight's-conditions forecast** (cloud/transparency/seeing → go/no-go) | **Astrospheric**, **Good To Stargaze**, **Astroweather** |
| **Target / session planner** (what to shoot, when it is well placed, observe lists) | **Telescopius**, **DSO Planner**, **Mobile Observatory Pro**, **Astro Planner**, **Astroplanner**, SkySafari 7 Pro |
| **Sun/Moon/Milky-Way position for composition** (photo planner) | **PhotoPills**, **Planit Pro / PlanIt Earth**, **Ephemeris**, **Milky Way Tonight**, **Nightlog: Milky Way Planner**, **AstroShoot Pro** |
| **Phone deep-sky capture / stacking** (the camera half) | DeepSkyCamera (+Pro), AstroCam, Astro Capture, Astrophotography Camera, Astrostacker, Astrophotography Field Calc |
| **Dedicated telescope hardware control** | Seestar, SkyAtlas, AstroImg |
| **Off-job: astrology / horoscope** | Galactic Planner (AI Astrology), Moonly, align27, Astrocalendar Dara-Astro, Astroplanner |
| **Off-job: noise / unrelated** | WhatsApp Messenger, Milky Way Factoring, Milky Launcher Pro, Milky Way Raiders, Landscape Design AI, My Way Travel, plus literary/edu "The Milky Way" entries |

Job-share note: the relevance guard called **"astrophotography planner" polluted** (4/30 titles carry
a distinctive token) and **"astro planner" polluted** (9/30) — Play fuzzy-matched into astrology and
unrelated names. The genuine planning job is a small slice of a much larger star-map shelf.

## 3. Audience size — install-band proxy

**This is a proxy, not an active-user count.** Play install bands are cumulative and overlapping;
the lower bounds below are *floors per app*, and summing them double-counts users who installed two
apps and ignores everyone above the band's floor.

Dedicated planning / conditions / composition apps (distinct, the closest thing to "plan a session"):

| App | Install floor |
|---|---|
| Astrospheric | ≥100,000 |
| PhotoPills | ≥100,000 |
| Good To Stargaze | ≥50,000 |
| Planit Pro | ≥10,000 |
| Telescopius | ≥10,000 |
| Astroweather | ≥10,000 |
| Mobile Observatory Pro | ≥10,000 |
| DSO Planner | ≥1,000 |
| Dark Skies | ≥500 |
| Milky Way Tonight | ≥100 |
| Astrophotography Planner | ≥100 |
| Nightlog | ≥10 |
| Astro Planner | ≥5 |
| **Sum of floors** | **≈ 291,715** |

Contrast — the adjacent **free star-map** population (not the planning audience, shown for scale):
Sky Map 50M+, Stellarium 10M+, Star Walk 2 10M+, Star Tracker 10M+, SkyView Lite 10M+, Sky Tonight
5M+, Star Gazer 5M+.

Reading: the *planning* shelf is **on the order of a few hundred thousand installs in the floors
alone** (proxy), while the general star-map shelf is 100–1000× larger. The planning job is a real but
comparatively small audience, and the biggest planning apps are free.

## 4. The measured gap

**I could not measure a clean product gap from complaint text, and I say so.** `scan-complaints.mjs`
was run twice on the two lowest-rated incumbents with real reviews — `de.seebi.deepskycamera`
(3.1★, 781 reviews, 1M+) and `de.seebi.deepskycamera.pro` (3.9★, 98 reviews, 1K+). Both are *capture*
apps, not planners, so their complaints describe the camera half, not the planning half:

> "(Pixel 9 pro) whenever I try and use this app it will freeze and lock up at around 40-50 image in.
> on the rare occasion it gets past 50 it works ok. but when it freezes and locks up it doesn't save
> any images or the stacked version when in stacking mode. So until this is fixed and I can use the app
> without being annoying into giving up. I'll be sticking with my phones inbuilt astrophotography mode
> and manually taking and stacking on my computer.(Edit: No, it didn't save anything there)"
> — 1★, DeepSkyCamera Pro, 2025-11-13, `scan-complaints.mjs --package de.seebi.deepskycamera.pro` (verbatim)

> "Tried the Pro version after having a good experience with the Free version. Unfortunately for me the
> Pro version outputs images completely riddled with red/blue noise, even when using the same settings
> as I do on the Free version (10 second exposure, 800 ISO). Really wanted to upgrade but it seems that
> I can only actually get use out of the Free version so I'll be sticking with that."
> — 3★, DeepSkyCamera Pro, 2025-11-09, same command (verbatim)

> "The only downside is that the RAW files can't be opened in Adobe Lightroom or Photoshop. The dev is
> aware and is working on this. Also, I didn't see a way to buy the Pro version to support the dev."
> — 4★, DeepSkyCamera, 2024-10-25, `scan-complaints.mjs --package de.seebi.deepskycamera` (verbatim)

Classifier output on those 6 reviews: `broken 1`, `crash 1`, `data-loss 1`, `unclassified 1`. The two
truly dedicated *planning* apps with reviews (`Nightlog` 10+, `Milky Way Tonight` 100+,
`Astrophotography Planner` 100+, `Dark Skies` 500+) are **unrated and returned 0 reviews** to the
scanner, so there is no complaint corpus for the planning job itself.

**What the shelf composition shows instead** (`INFERRED` from the table above): the planning job is
served by *two different kinds of app that do not overlap*. Tonight's conditions live in Astrospheric
/ Good To Stargaze / Astroweather; target choice and placement live in Telescopius / DSO Planner /
Mobile Observatory Pro; frame composition lives in PhotoPills / Planit Pro. A single go/no-go that
answers *"is MY target well placed from MY location tonight"* was not seen as one app's advertised job
in the visible set. The closest generalists are paid and general-purpose (PhotoPills $10.99, Planit
Pro $9.99). Labelled `INFERRED`; the command that would settle whether an incumbent already does this
in-app is a listing/feature read of Astrospheric and Telescopius, not run here.

## 5. Evidence FOR and AGAINST earning here

**Strongest evidence this audience PAYS** — paid upfront apps with real volume and high ratings
(`MEASURED`, `enrich-market.mjs`):

- **PhotoPills — paid $10.99, 100K+ installs, 4.8★ (8,216 reviews)** — a generalist sun/Moon/Milky-Way
  composition planner. A ten-dollar paid app at a hundred thousand installs is the clearest paid
  demand on the shelf.
- **Mobile Observatory Pro — paid $11.99, 10K+ installs, 4.6★ (5,681 reviews)** and
  **Planit Pro — paid $9.99, 10K+ installs, 4.6★ (1,220 reviews)** — two more paid-at-a-price apps.
- The free leaders monetise heavily via **in-app purchases** (Stellarium, Sky Tonight, Star Tracker,
  Sky Gazer, Moonly, Astrospheric, Good To Stargaze all declare IAP).

**Strongest evidence AGAINST earning here** (`MEASURED`, `enrich-market.mjs`):

- The **highest-install dedicated astrophotography app is free with NO monetisation declared**:
  DeepSkyCamera, 1M+ installs, `containsAds: false`, `inAppPurchases: false`. Its paid sibling
  (DeepSkyCamera Pro, $9.49) has only **1K+ installs** — the paid conversion in the dedicated capture
  niche is ≈3 orders of magnitude below the free one.
- The **dedicated planning apps at the exact query are tiny and unrated**: Nightlog (10+), Milky Way
  Tonight (100+, $7.99), Astrophotography Planner (100+), Astro Planner ($4.99, 5+), Dark Skies
  (500+). The literal "astrophotography planner" job shows almost no install base and no ratings —
  demand for a *dedicated* planner is not demonstrated on this shelf.
- The planning audience is served largely for free (Astrospheric, Good To Stargaze, Telescopius,
  Astroweather, DSO Planner all free), which caps a paid-unlock plan.

---

```
CLAIM / LABEL / EVIDENCE
```
| Claim | Label | Evidence |
|---|---|---|
| Sky Map has 50M+ installs, 4.1★, free, no ads/IAP | MEASURED | `enrich-market.mjs --query "night sky planner" --top 6 --concurrency 2` → `com.google.android.stardroid` 50M+ |
| Stellarium has 10M+ installs, 4.8★ (337,048), free with IAP | MEASURED | same command → `com.noctuasoftware.stellarium_free` |
| PhotoPills is paid $10.99, 100K+ installs, 4.8★ (8,216) | MEASURED | `enrich-market.mjs --query "deep sky photography" ...` → `com.photopills.android.photopills` |
| Mobile Observatory Pro is paid $11.99, 10K+ installs, 4.6★ (5,681) | MEASURED | `enrich-market.mjs --query "DSO Planner astronomy" ...` → `com.zima.mobileobservatorypro` |
| Astrospheric has 100K+ installs, 4.5★ (804), free with ads+IAP | MEASURED | `enrich-market.mjs --query "Astroweather astronomy tools" ...` → `com.astrospheric.dfior.astrospheric` |
| Good To Stargaze has 50K+ installs, 3.7★ (677), free with IAP | MEASURED | same command → `com.goodtostargaze` |
| DeepSkyCamera has 1M+ installs, 3.1★ (781), free, ads=false, IAP=false | MEASURED | `enrich-market.mjs --query "astrophotography planner" ...` → `de.seebi.deepskycamera` |
| DeepSkyCamera Pro is paid $9.49 with only 1K+ installs, 3.9★ (98) | MEASURED | `enrich-market.mjs --query "deep sky photography" ...` → `de.seebi.deepskycamera.pro` |
| Nightlog has 10+ installs and is unrated | MEASURED | `enrich-market.mjs --query "milky way planner" ...` → `com.grothgalleries.nightlog` |
| Astro Planner is paid $4.99 with 5+ installs, unrated | MEASURED | `enrich-market.mjs --query "astro planner" ...` → `com.astronomia.astro_planner` |
| Telescopius has 10K+ installs, 4.5★ (94), free with ads | MEASURED | `enrich-market.mjs --query "Telescopius" ...` → `com.telescopius.twa` |
| DSO Planner has 1K+ installs, 4.1★ (104), free | MEASURED | `enrich-market.mjs --query "DSO Planner astronomy" ...` → `com.astro.dsoplanner` |
| Astroweather has 10K+ installs, 4.2★ (113), free with ads+IAP | MEASURED | `enrich-market.mjs --query "Astroweather astronomy tools" ...` → `cc.meowssage.astroweather` |
| The "astrophotography planner" query is polluted: only 4/30 titles carry a distinctive token | MEASURED | `enrich-market.mjs` header line for that query; `scan-titles.mjs --query "astrophotography planner"` `guard=polluted (4/30, share 0.13)` |
| The "astro planner" query is polluted: 9/30 distinctive tokens | MEASURED | `enrich-market.mjs --query "astro planner"` guard line |
| Verbatim complaint: DeepSkyCamera Pro freezes ~40–50 frames and saves nothing | MEASURED | `scan-complaints.mjs --package de.seebi.deepskycamera.pro --concurrency 2 --json` → 1★ 2025-11-13 review text |
| Verbatim complaint: DeepSkyCamera Pro output "riddled with red/blue noise" vs free | MEASURED | same JSON → 3★ 2025-11-09 review text |
| Verbatim complaint: DeepSkyCamera RAW not openable in Lightroom/Photoshop; no way seen to buy Pro | MEASURED | `scan-complaints.mjs --package de.seebi.deepskycamera` → 4★ 2024-10-25 review text |
| Complaint corpus for the planning job is empty: the dedicated planners are unrated and returned 0 reviews | MEASURED | `scan-complaints.mjs --package ... darkskies2 ... astrophotography_planner` → `reviews=0` for both |
| Dedicated planning/conditions apps' install floors sum to ≈291,715 | INFERRED | sum of the Play band lower bounds in §3, all `MEASURED` by `enrich-market.mjs`; bands overlap so this is a floor proxy, not users |
| The planning job is split across separate condition, target and composition apps, with no single go/no-go incumbent seen | INFERRED | composition of the §1 table and §2 classification; would be settled by an in-app feature read of Astrospheric and Telescopius (not run) |
| The star-map shelf is 100–1000× larger than the planning shelf in install floors | INFERRED | §3 comparison (Sky Map 50M+ vs planning floors ≈0.3M) |
| Whether any incumbent combines condition forecast + target placement in one flow | UNVERIFIED | not checked; would need an in-app read of `com.astrospheric.dfior.astrospheric` and `com.telescopius.twa` |
| Active-user (not install) counts for any app | UNVERIFIED | Play exposes no active-user figure on the listing page; `enrich-market.mjs` reads install bands only |
| Revenue of any incumbent | UNVERIFIED | no instrument in this repo reads Play revenue; would need the developer's public statements or a paid sensor |
| A dedicated astrophotography-session planner with a real install base and a rating exists on this shelf | UNVERIFIED | none observed in the 45 visible cards scanned; absence in a relevance-ordered top set is not proof of absence |

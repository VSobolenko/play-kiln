# K — Competitor features: what they offer that we have not planned

**Worker:** competitor-features research (dispatch `K`).
**Date:** 2026-10-08.
**Product judged against:** **Lumintra** — a paid **$9.99 one-time** (`purchase_only_no_ads`),
closed-source, no-server Android **light-and-sky session planner**. Hero screen: a scrubbable
**Night** scene (user's drawn horizon silhouette as foreground, real sky for a chosen place/date,
time slider animating sunset → twilight → astronomical darkness → dawn, lens FoV overlay, satellite
streak flag). Screens: Night scene · Session brief · Targets · Scouting spots · Share · Settings.
**Buyer:** the outdoor / night / landscape photographer who plans a shoot.

**Method.** Every Play listing below was opened with `WebFetch` on
`play.google.com/store/apps/details?id=<pkg>` **in this session**, unless the row says otherwise.
Prices, install buckets and rating counts come from the durable shelf record
`docs/research/2026-10-08-light-and-sky-session-planner-market.md` (itself measured by
`scripts/enrich-market.mjs`) where the listing did not render them, and that is stated per row.
Labels follow `.cursor/rules/grounded-claims.mdc`. Nothing here was copied from an incumbent's code
or assets; the UI patterns are named as patterns.

**Scope boundary honoured.** No file under `apps/**` was opened; no Gradle build was run; every file
written by this worker is under `.kiln/scratch/spec-2026-10-08/`.

---

## 1. The feature matrix

Status key: **✅ HAVE** = already in Lumintra's plan · **➕ ADOPT** = we lack it and should add it ·
**🔄 ADAPT** = we lack it and should fold it into the scene already planned · **❌ REJECT** = we
should deliberately not do it (reason in the cell / §3).

### 1.1 The light-planning shelf (the shelf that carries the money)

| App | package | Price (one-time) | Installs | Relevant features (only our job's) |
|---|---|---:|---:|---|
| **PhotoPills** | `com.photopills.android.photopills` | **$10.99** (listing; `$10.99 Buy` also read on appbrain/appstorespy) | **100K+** (Play listing; appbrain 450K total) · 4.8 (8.22K) | 2D map-centric planner for Sun/Moon/Milky Way; **sun/moon alignments "fast finder"** (find the date/time an object hits a chosen spot); 3D AR overlay (Sun/Moon/Milky Way/celestial equator/Polaris/**DoF/FoV**); **photo-plans manager (to-do list)**; **location-scouting POI database**; sunrise/set, twilights, golden hour, blue hour, moonrise/set, supermoon, moon calendar; **calculators: long exposure, timelapse, spot stars, star trails, hyperfocal, DoF, FoV**; **widgets (Sun, Moon, Milky Way)**; awards/community. `MEASURED` — listing. |
| **The Photographer's Ephemeris (TPE)** | `com.crookneckconsulting.tpeandroid` | **retired** — no longer sold on Play for Android | n/a (removed) | Map-centric sun/moon calculator with elevation/terrain; **3D Milky Way band**; TPE 3D "realistic directional light and shadows"; desktop-only **3D Celestial Sphere** with visual search. `ON RECORD` — photoephemeris.com: "TPE for Android was available from 2011 to 2021 … now withdrawn from sale and officially retired"; the Android app was retired and is not maintained. **MEASURED** — the site's own help pages, read this session. |
| **Sun Surveyor (Sun & Moon)** | `com.ratana.sunsurveyor` | **$9.99** (`$9.99 Buy` read on the en_IE listing) | **100K+** · 4.8 (5.14K) | Live camera AR of sun/moon **paths**; interactive map with Street View; **3D compass**; detailed ephemeris; golden/blue hour; **Milky Way & star-trail features**; **"Photo Opportunities"** — interactive list of critical times incl. "total darkness for stargazing"; **"Position Search"** — search for Sun/Moon/**Milky-Way-centre** positions within constraints; **"Visual time machine"**; moon distance/age/apogee/perigee/supermoon; **sun shadow length**; share details/locations; **offline usage (enter coordinates, save/load locations)**; **import/export .kmz/.kml**; magnetic declination; **9 widget variants**. `MEASURED` — listing. |
| **Planit Pro: Photo Planner** | `com.yingwen.photographertoolspro` | **$9.99** (with IAP) | **10K+** · 4.6 (1.22K) | Simulated viewfinder (VR/AR/picture/street view); ephemeris (sunrise/set, moonrise/set, twilight, special hours); **sun/moon finder**; **major stars, constellations, nebulae azimuth & elevation**; **star-trail planning**; **timelapse calculation & simulation**; **sequence calculation & simulation**; **Milky Way searching**; solar & lunar eclipse; **exposure/ND filter calculator**; **light meter**; **rainbow position**; **tide height and tide search**; location scouting (GPS, elevation, distance, elevation gain, clear view, focal length, DoF, hyperfocal, panorama); **offline elevation files + offline mbtiles maps**. `MEASURED` — listing. |
| **Sunset & Sunrise – Sun Tracker (PhotoTime)** | `pl.guteklabs.phototime` | free / IAP | **500K+** · 4.3 (9.25K) | Interactive **map of sun path & moon path**; AR sun/moon; **scout locations & save favourites**; golden/blue hour; civil/nautical/astronomical twilight; **moon phases/moonrise/moonset/supermoon calendar**; **widgets**; **"smart notifications" for golden hour/sunrise/sunset/moon events**; **weather forecasts**; **DoF & FoV calculators**; **long exposure, timelapse & star-trail planning**; **"Milky Way visibility"**; **3D map with shadow simulation** (in the What's-new this session). `MEASURED` — listing. |
| **Photographer's companion Pro** | `com.stefsoftware.android.photographerscompanionpro` | ~$3.49 (search synthesis; reviews quote older $1.99/€2 — price **UNVERIFIED**, listing showed no price) | not read · 4.9 (633) | 40+ tools; **AR for Sun, Moon, Milky Way**; **star-trail / Milky Way simulator (with or without trails)**; northern-lights mode; planets; **night mode**; **countdown**; compass; bubble level; torch; notepad; **exposure/EV, DoF, hyperfocal, FoV, ND**; **light meter**; **reciprocity-failure calculator**; **data import/export**; 16 languages. `MEASURED` — listing. |
| **Photographer's Tools Pro** | `com.zenapps` (Robert Ehrhardt / The Zen Apps) | ~€2.39 (search synthesis; **UNVERIFIED**) | not read (very small) | Hyperfocal & DoF; sunrise/set, moonrise/set; moon phase; **civil/nautical twilight + blue & golden hour**; **ND-filter exposure adjustment**; grey-card white balance. `ON RECORD` — apps112 mirror of its Play description + thezenapps page, read this session. |
| **Sun Position PRO** | `com.andymstone.sunposition` | $9.99 | 50K+ · 4.9 (2.73K) | Sun/moon position, shadows, golden hour; "astrophotography … when and where the Milky Way core will be visible". `MEASURED` — market record §2. |
| **Sun Seeker** | `com.ajnaware.sunseeker` | $8.99 | 50K+ · 4.8 (1.0K) | AR sun path, shadow, golden hour. `MEASURED` — market record §2. |
| **SunOnTrack** | `com.sunnytapps.sunnytrack` | $7.99 | 50K+ · 4.8 (874) | Sun path & shadows; **"dark theme for night hours"** (its entire night treatment). `MEASURED` — market record §2, §3(a). |
| **Golden Hour+** | `com.simplaapliko.goldenhourpro` | $4.99 | 1K+ · 4.7 (189) | Golden/blue hour, **civil/nautical/astronomical twilight durations**. `MEASURED` — market record §2. |
| **Mobile Observatory Pro** | `com.zima.mobileobservatorypro` | $11.99 | 10K+ · 4.6 (5.68K) | Astronomy observing planner (adjacent shelf). `MEASURED` — market record §2.2. |

### 1.2 The stargazing shelf (free giants own the head terms)

| App | package | Price | Installs | Relevant features |
|---|---|---:|---:|---|
| **Sky Map** | `com.google.android.stardroid` | free, no ads/IAP | **50M+** · 4.1 (482K) | Point-and-identify; **home-screen widgets: Moon phase, "Tonight's sky", sky countdown**; alt/az grid; optional-permission design (each permission asked only when its feature is used, withdrawable) — a **Play-compliance/permissions pattern**, not a feature. `MEASURED` — listing. |
| **Stellarium (Mobile)** | `com.noctuasoftware.stellarium_free` | free / IAP (Plus; sidebar showed **Stellarium Plus $19.99**) | **10M+** · 4.8 (337K) | Realistic atmosphere/sunrise/sunset + refraction; realistic Milky Way; **star twinkling**; 3D planets; **track satellites incl. ISS**; **red "night mode"**; Plus adds **advanced observing tools to predict an object's observability and transit times** and **offline reduced data**. `MEASURED` — listing. |
| **Star Walk 2 Plus** | `com.vitotechnology.StarWalk2Free` | free / ads+IAP (Star Walk 2 Pro **$2.99**) | **10M+** · 4.5 (572K) | **"Sky Live" and "Visible Tonight"** — best moments to observe; **adjustable time scale** (time-travel scrub); **night-mode interface**; AR; **infographics**; interactive stories; quizzes; Planet Walk (orbits/alignments). `MEASURED` — listing. |
| **Sky Tonight** | `com.vitotechnology.sky.tonight.map.star.walk` | free / IAP (pay-once-for-life option) | **5M+** · 4.7 (85.7K) | **"What celestial events can I see tonight?"**; **"Visible Tonight"**; **Astronomy Calendar**; **"Stargazing Index" — a personalised forecast from Moon phase, light pollution and visibility**; **one-handed bottom navigation**; minimised info panels; **built-in image editor to make clean shareable sky images**; night mode. `MEASURED` — listing. A pinned review states: *"only thing it's missing is live satellite tracking … I would have paid extra just to have everything all in one app."* `MEASURED` — listing review text. |
| **SkySafari 8 Pro / Plus** | `com.simulationcurriculum.skysafari8pro` / `…skysafari8plus` | **$34.99** / **$29.99** (7 Plus $9.99; SkySafari Astronomy $6.99) | 1K+ · 4.2 (211) | Deep-sky database; **Events Finder** (transits, occultations, appulses); **Observing Conditions (Premium)** — hourly forecast with best viewing windows, seeing index, sky-quality metrics; **Night Vision Mode**; **rotatable FoV indicator** to match a camera orientation; **image import + plate solving**; **cloud sync (LiveSky) across devices**; telescope control. `MEASURED` — listing. |
| **Star Chart** | `com.escapistgames.starchart` | free / IAP | **10M+** · 4.3 (174K) | 20M stars, 88 constellations with Hevelius artwork; full Messier catalogue; satellites; **guided tours**; **time shift ±10,000 years**; voice control; **"genuinely helpful notifications, such as eclipses and meteor showers"**. `MEASURED` — listing. |
| **Sky Guide** | (none) | iOS-only | — | **Not on Android** — "Sky Guide goes all-in on Apple devices and ignores Android" (`ON RECORD`, Space.com review + Fifth Star Labs site, read this session). Its premium cues (cinematic time controls, real astrophotography backdrop, 120 FPS) are patterns, not a competitor on our platform. |

### 1.3 The satellite shelf

| App | package | Price | Installs | Relevant features |
|---|---|---:|---:|---|
| **ISS Detector (free)** | `com.runar.issdetector` | free / ads+IAP | **5M+** · 4.8 (121K) | Pass list ("overview of coming passes"); **weather conditions for a sighting**; notifications/alarms a few minutes before a pass; share sightings. `MEASURED` — market record §4.1. |
| **ISS Detector Pro** | `com.runar.issdetector.pro` | **$17.99** | **100K+** · 4.4 (3.19K) | All extensions, no ads; **radio-amateur satellites with Doppler shift**; **Famous Objects** (Hubble, Tiangong, rocket bodies); **comets and planets**; Iridium flares; pass list + alarm; weather check. `MEASURED` — listing. **The highest observed paid price in the recon.** |
| **Satellite Tracker by Star Walk** | `com.vitotechnology.SatelliteTracker` | free / ads+IAP | 1M+ · 4.4 (61.4K) | Pass times and sky paths. `MEASURED` — market record §4.1. |
| **Look4Sat** | `com.rtbishop.look4sat` | free | 100K+ · 3.8 (1.01K) | Amateur-radio pass prediction. `MEASURED` — market record §4.1. |
| **NASA Spot the Station** | — | free | 500K+ · 4.7 (7.12K) | ISS sighting alerts. `MEASURED` — market record §4.1. |
| **AstroSat** (research tool, *not* a shipped Android app) | — | free/OSS (Python, GitHub `james-m-osborn/astrosat`) | 22 GitHub stars | **"Forecasts satellite transits for optical astronomical observations"** — models crossings **through your exact field of view** using location, target, optics, camera, **exposure length** and session time; shows likely trail paths, sunlit visibility, **affected sub-exposures**, and the cleanest run; the paper explicitly names **"landscape and astro-photographers"** as a user community. `MEASURED` — astrosat.org + github.com + arXiv 2111.11268, read this session. |
| **Tonight's Window** (web tool) | `orbitalnodes.ai/satellite-trails` | free web | — | Finds "the cleanest window to image tonight … **the shadow window**, usually centred on local midnight, when the sky overhead is largely eclipsed and cleanest for deep-sky imaging"; reports tonight's Moon alongside the trail forecast. `MEASURED` — the page, read this session. |
| **PSF Guard** (pipeline tool) | `psf-guard.com` | not read | — | Predicts satellite trails and matches them against FITS pixels; "abstains rather than reporting the frame as clean". `MEASURED` — the page, read this session. |

**Reading:** satellite-**pass** apps are a big, free-ad shelf; satellite-**streak-forecasting against a long exposure** exists only as research/web tools, and **no shipped Android paid app bundles it into a light-planner**. Our planned satellite-streak flag is therefore a genuine wedge with no Android competitor selling it. `INFERRED`.

### 1.4 Adjacent — anything else a night photographer pays for

| App | package | Price | Installs | Relevant features |
|---|---|---:|---:|---|
| **Astrospheric** | `com.astrospheric.dfior.astrospheric` | free / Pro subscription | 100K+ · 4.5 (804) | **84-hour hour-by-hour forecast**; **ensemble cloud forecast** (compare models); **sky transparency + seeing**; **smoke forecast integrated** (unique); wind/temp/humidity; precise sun/moon rise-set; **Kp index for aurora + alerts**; **ISS flyover paths and times**; **weather alerts when conditions meet your requirements (Pro)**; **Site Mode works offline**. `MEASURED` — listing. |
| **Clear Outside** | `com.firstlightoptics.clearoutside` | free | 100K+ · **3.8** (1.37K) — the shelf's weakest rating | **7-day hourly forecast, updated hourly**; **low/medium/high and TOTAL cloud cover**; moonrise/set + phase; **civil/nautical/astronomical darkness**; **ISS pass-over info shown beside the cloud forecast**; save favourite locations. `MEASURED` — listing. |
| **Light Pollution Map – Dark Sky** | `com.pa.lightpollutionmap` | free / Pro subscription | **500K+** · 4.7 (7.33K) | **Light-pollution overlay + Bortle estimates**; **"Horizon Safe Radius"** (areas away from distant light pollution); **live sky conditions** (cloud, temp, wind); **"Darkness Timer — see exactly when astronomical darkness begins and ends, with a live countdown to true night"**; **aurora Kp**, solar wind, Bz; **Moon Position Tool to visualise azimuth and plan landscape astrophotography compositions**; **ISS tracker + flyover alerts**; **night-sky calendar** (meteor showers, eclipses, supermoons); **astrophotography calculators** (star trails, orbital periods); **custom alerts**; **location manager**; **widgets**; **night & AMOLED themes**. `MEASURED` — listing. |
| **Good To Stargaze** | `com.goodtostargaze` | free / **subscription** (Value $0.99/mo, Hobbyist $3.99/mo or $25.99/yr, Pro $49.99/yr) | 50K+ · **3.9** (553) | Weather + **seeing, transparency, light pollution** in one; dark-sky map. `MEASURED` — listing + App Store IAP list; a pinned review rejects it as *"data that can also be found elsewhere"* at a recurring price. `MEASURED` — listing review. |
| **My Aurora Forecast Pro** | `com.jrustonapps.myauroraforecastpro` | **$3.99** | 50K+ · 4.78 (3.8K) | Kp index; best viewing locations; **1h / hours / weeks forecasts**; **push alerts when activity is high**; solar wind + sun imagery; live webcams; tour booking. `MEASURED` — listing + appbrain. |
| **Lunescope Pro: Moon Phases+** | `com.daylightmap.moon.pro.android` | **$3.99** one-time | 100K+ (appviewable) · 4.1 | Interactive 3D moon (craters, **earthshine**); phase/angle/libration/rise-set for any date; alerts. `MEASURED` — listing. |
| **My Moon Phase Pro** | `com.jrustonapps.mymoonphasepro` | **$2.99** | not read · 4.8 (2.41K) | Moon calendar; **cloud cover for upcoming days**; **golden & blue hour times**; phase alerts; widgets. `MEASURED` — listing. |
| **Daff Moon Phase** | `com.dafftin.android.moon_phase` | free | not read · 4.9 (55.3K) | Moon/Sun/planet rise-set, altitude/azimuth, eclipses, **golden & blue hours**, widgets, **custom notifications for upcoming events**. `MEASURED` — listing. |
| **Light for Night Vision** (Vixen) | `jp.co.vixen.nightvisionlight` | free | not read | Turns the screen into an adjustable **red flashlight** for dark-adapted eyes. `MEASURED` — listing. |
| **Nightcam: Manual Night Camera** | `com.rayinformatics.nightcam` | free/IAP | not read | A **capture** app (star trails, Milky Way presets, star-trail 20 s, moon mode) — different job (capture, not planning). `MEASURED` — listing. |

---

## 2. Gaps we should close — the ranked adopt-list

Ranked by **revenue reason**, not by how interesting the feature is. "Where it lives" is the screen in
Lumintra's own screen map. Time is never an input.

**A1. Milky Way core / galactic-centre position and visibility in the Night scene** — **➕ ADOPT**
- *What:* show the galactic centre's alt/az path, whether it is above the horizon during the dark
  window, and draw the Milky Way band correctly oriented for the chosen date/place.
- *Why it earns:* the Milky Way is *the* subject that makes this buyer pay: Sun Surveyor markets a
  "Milky Way centre" **Position Search** and star-trail features inside its **$9.99/100K+** product;
  PhotoPills is *"The First 2D Map-Centric Planner: Sun, Moon, **Milky Way**"* and sells at **$10.99**;
  Sun Position (Stonekick) advertises *"see when and where the Milky Way core will be visible"*;
  PhotoTime advertises **"Milky Way visibility"**. Night content is bundled into **every** paid
  light-planner (`INFERRED` from five MEASURED description quotes, market record §3.2).
- *Where:* Night scene (band + core marker) + Session brief (core window row).
- *Licence:* our own geometry from **Astronomy Engine (MIT)**; the *band texture* must be self-baked
  from a permissive catalogue — **not** the CC BY-SA 3.0 IGO Gaia panorama (`MEASURED`, J §3.3).
- *Source:* the four listings above + market record §3(a).

**A2. Star-trail / star-arc mode driven by the existing time scrub** — **➕ ADOPT**
- *What:* a mode where the scrub shows the arc a star (or the whole field) traces over the exposure
  the user set — the trail length and direction read directly off the geometry the scene already has.
- *Why it earns:* **star-trail planning is sold in every paid incumbent**: Sun Surveyor ("Milky Way &
  star trail features"), PhotoPills ("Star Trails" calculator), Planit Pro ("star trail planning"),
  PhotoTime ("Star Trail planning"), Photographer's companion ("Milky Way without or with star
  trails"). It is a headline bullet, not a niche.
- *Where:* Night scene (a "trails" toggle on the scrub) — reuses the orbit geometry already drawn.
- *Licence:* our own geometry; no asset.
- *Source:* the five listings in §1.1.

**A3. A "tonight / best window" summary as the Session brief's first answer** — **➕ ADOPT**
- *What:* one screen answering *"is tonight worth going out, and what is the best window?"* — dark
  window, moon interference, cloud, a target that is up, an event tonight.
- *Why it earns:* this is the exact question incumbents headline: Star Walk 2 **"Visible Tonight"**,
  Sky Tonight **"What celestial events can I see tonight?"** + **"Visible Tonight"**, Sky Map's
  **"Tonight's sky" widget**, SkySafari **"Observing Conditions … best viewing windows"**. A pinned
  Sky Tonight review states the buyer *"would have paid extra just to have everything all in one
  app"* (`MEASURED`) — the willingness-to-pay for a single combined answer.
- *Where:* Session brief (top) — it is the payoff the Night scene feeds.
- *Licence:* our own computation + `api.met.no` (CC BY 4.0, attribution).
- *Source:* Sky Tonight / Star Walk 2 / Sky Map / SkySafari listings, read this session.

**A4. Full twilight + key-times table in the user's language (civil/nautical/astronomical, golden,
blue, alpenglow), scrollable** — **➕ ADOPT** (near table-stakes)
- *What:* every time the buyer plans by, named and localised, in the Session brief.
- *Why it earns:* it is in **every** light-planner (Sun Surveyor, Planit Pro, PhotoTime, Golden
  Hour+, Photographer's Tools, Photographer's companion). Its presence is table-stakes and its absence
  would be the first review complaint; the localization rule requires per-locale resources anyway.
- *Where:* Session brief (the scrollable answers list).
- *Licence:* our own computation; the *names* are standard terms, not a licensed asset.
- *Source:* §1.1 listings.

**A5. True red / AMOLED night mode across the whole app** — **➕ ADOPT**
- *What:* a palette mode that preserves dark adaptation (red-on-black, no blue light), applied to
  every screen.
- *Why it earns:* it is a named selling point of the premium astronomy apps — Stellarium ("night mode
  (red)"), Sky Walk 2 ("night-mode interface"), SkySafari ("Night Vision Mode"), Light Pollution Map
  ("Night & AMOLED Themes … preserve night vision"), Photographer's companion ("night mode"). For a
  night-shoot planner it is both expected and, in a dark app, cheap. It also answers the measured
  "red-light tool" micro-shelf (Vixen *Light for Night Vision*) without a separate app.
- *Where:* Settings + a one-tap toggle reachable from the Night scene.
- *Licence:* our own theme.
- *Source:* the five listings above.

**A6. Portable export/import of spots, horizons and plans (a share or a file)** — **➕ ADOPT**
- *What:* the user's drawn horizons and saved spots move off the phone (share sheet and/or a JSON
  file through the system document picker).
- *Why it earns:* the **measured** top complaint of the paid incumbents is *data that does not
  travel*: Planit Pro — *"the 29 plans that I have on my cellphone can't be easily transferred to a
  new tablet"* and a purchased add-on that *"only works on the tablet"*; PhotoPills — *"no way … to
  not have to pay 9.99 twice when switching devices"*. Also satisfies the depth contract's `data_out`
  handoff (`.cursor/rules/app-depth.mdc`).
- *Where:* Scouting spots (export) + Share.
- *Licence:* our own format; `ACTION_SEND` / `ACTION_CREATE_DOCUMENT` / `FileProvider` are Android APIs.
- *Source:* Planit Pro + PhotoPills listing review text, read this session; market record §3.1.

**A7. Cloud and conditions shown aligned to the dark window, in the scene** — **➕ ADOPT** (we
already plan `api.met.no`; this fixes *how it is presented*)
- *What:* hourly cloud drawn against the same time axis as the darkness window, not as a separate
  weather page; degrade gracefully when the feed is unavailable.
- *Why it earns:* cloud is **absent from the light-planning shelf** (`MEASURED` keyword scan, market
  record §3(d)) while the dedicated astronomy-weather apps that *do* show it carry the shelf's
  **weakest ratings** (Clear Outside 3.8, Good To Stargaze 3.9) — the wedge is presenting it well, and
  Clear Outside shows the pattern worth copying (ISS pass **beside** the cloud table).
- *Where:* Night scene (a cloud ribbon on the timeline) + Session brief.
- *Licence:* `api.met.no` **CC BY 4.0 with attribution** (settled, market record §6.3); hourly TOTAL
  cloud only — no low/mid/high split, unlike Clear Outside/Astrospheric. State this honestly.
- *Source:* Clear Outside, Astrospheric, PhotoTime, Light Pollution Map listings; market record §6.3.

**A8. A next-event countdown / reminder** — **➕ ADOPT**
- *What:* a live countdown to the next golden hour, dark-window start, moonrise or best pass, with an
  optional local notification (no account, no server).
- *Why it earns:* "smart notifications" are a headline in the paid/free leaders (PhotoTime), and Sun
  Surveyor's **"Photo Opportunities"** is literally *"an interactive listing of critical times to
  help inspire and generate shot ideas"*; Star Chart lists improved notifications as a headline
  update; Clear Outside's own copy calls its ISS-beside-cloud view *"our favourite feature"*. A
  countdown is the cheap retention/re-open surface.
- *Where:* Session brief header + Settings.
- *Licence:* our own; Android notifications (no `WorkManager` server).
- *Source:* PhotoTime, Sun Surveyor, Star Chart, Clear Outside listings.

**A9. An exposure / focal-length planning helper for the chosen scene** — **➕ ADOPT**
- *What:* given the user's exposure and lens, show what the frame does: max exposure before stars
  trail (a 500-rule / NPF-shaped number), the FoV the scene already overlays, and a star-trail length
  estimate.
- *Why it earns:* **exposure and FoV calculators are in every paid incumbent** — PhotoPills ("Long
  Exposure, Timelapse, Spot Stars, Star Trails, Hyperfocal, DoF, FoV"), Planit Pro ("exposure/ND
  filter calculator"), PhotoTime ("DoF & FoV calculators"), Photographer's Tools (ND exposure),
  Photographer's companion (EV/DoF/reciprocity failure). It is a bullet the buyer checks for.
- *Where:* Session brief (a "your shot" panel) — it makes the scene actionable, not decorative.
- *Licence:* our own maths; no asset.
- *Source:* the five listings in §1.1.

**A10. A home-screen widget** — **➕ ADOPT** (small, cheap)
- *What:* one widget: "next dark window + moon + cloud" for the saved home spot.
- *Why it earns:* widgets are a headline feature of the paid light-planners — Sun Surveyor advertises
  **9 widget variants**, PhotoPills 3 (Sun/Moon/Milky Way), Light Pollution Map several, My Moon
  Phase Pro widgets, Sky Map 3 widgets. It is the cheapest re-open surface and it is *absent* from
  every astronomy-weather app except the widget-rich ones.
- *Where:* Settings (optional) + a small Glance/AppWidget surface.
- *Licence:* our own; Android widget APIs.
- *Source:* Sun Surveyor / PhotoPills / Light Pollution Map / Sky Map / My Moon Phase Pro listings.

**A11. "Milky Way / moonlight wash" as a scene condition** — **➕ ADAPT**
- *What:* quantify how much the **Moon** will wash out the sky in the chosen window (and optionally a
  coarse darkness-quality line), instead of shipping a full Bortle map.
- *Why it earns:* the Moon is the single biggest controllable factor in a night shoot, and our engine
  already computes it (Alt/Az, `moonPhase`, `illumination` — market record §7). A **full** light-
  pollution map would compete with a **500K+ free** incumbent (`com.pa.lightpollutionmap`) and the
  engine map says Bortle would need a new bundled raster asset — so adapt to the cheap, honest half.
- *Where:* Night scene (a "sky darkness" readout) + Session brief.
- *Licence:* our own geometry; no new asset.
- *Source:* Light Pollution Map, Good To Stargaze, Sky Tonight ("Stargazing Index" from Moon phase +
  light pollution + visibility) listings; market record §7.

**A12. Keep the satellite-streak flag as the headline differentiator** — **➕ ADOPT / keep**
- *What:* as planned — overlay the lens FoV and show whether a satellite will streak the long exposure.
- *Why it earns:* **no shipped Android paid app sells this.** The capability exists only as research
  and web tools — AstroSat (Python, GitHub, 22 stars), Tonight's Window (web), PSF Guard (pipeline),
  and the AstroSat paper explicitly names *"landscape and astro-photographers"* as intended users.
  Satellite-**pass** apps are a big free shelf (ISS Detector 5M+ free), but none predicts a streak
  against a *photographer's exposure*. The single most defensible thing in the plan.
- *Where:* Night scene (FoV + streak flag) + Session brief.
- *Licence:* CelesTrak GP JSON is keyless (MEASURED) but its **usage policy for a commercial app is
  UNVERIFIED** — settle at `celestrak.org` before shipping (market record §10 item 3); Orekit/Hipparchus
  are permissively licensed (measured PASS on Android, market record §7).
- *Source:* AstroSat site + GitHub + arXiv 2111.11268; Tonight's Window; PSF Guard; ISS Detector listings.

---

## 3. Rejected — what competitors do that we deliberately should not

**R1. "Point your phone at the sky and identify stars" (the whole LIVE shelf).**
Every 10M+ stargazing app and the 50M+ Sky Map are LIVE; the largest pure-PLANNING app is 100K+
(≈500× smaller, market record §2.1). Reason: the owner has already decided against it, and the
compass is a **measured defect surface** — Sun Surveyor's most recent negative review is *"inaccurate
… off by several degrees"*; Star Walk 2 and Sky Map reviews repeat compass drift/calibration
complaints; a compass-dependent overlay would import that churn and add no revenue over our
compass-free scene.

**R2. AR overlay of sun/moon paths (PhotoPills, Sun Surveyor, PhotoTime, Planit Pro, Photographer's
companion).** Reject as a *core*, for the same compass reason and because PhotoPills' own listing
warns *"Some devices may not have a compass"*. Our overlay is a FoV frame drawn in the scene, not a
camera-compass view.

**R3. Subscriptions (Astrospheric Pro, Good To Stargaze, Sky Guide, Light Pollution Map Pro).**
The factory bans subscriptions (`ideation-gate.mdc`), and the market agrees: Good To Stargaze's
pinned review rejects its recurring price as *"data that can also be found elsewhere"* (`MEASURED`);
Astrospheric's own copy shows its best data behind a subscription. A **one-time $9.99** is the shelf's
modal price.

**R4. Aurora / Kp / solar-wind (My Aurora Forecast $3.99, Astrospheric Kp, Light Pollution Map
aurora).** Reject for *our* buyer: it is a separate audience and a separate feed; the paid aurora
shelf is cheap and already served, so it adds cost and a dependency without a revenue reason.

**R5. Cloud accounts / sync / societies / chat (SkySafari LiveSky, Astrospheric societies, Planit Pro
sync).** Reject: a server needs either a subscription (banned) or a recurring cost with no revenue
line, and Planit Pro's sync is itself a **measured complaint**. Answer the *portability* need with a
local export instead (adopt A6).

**R6. Telescope control, plate solving, image import/editing (SkySafari, Stellarium Plus).** Reject:
a different buyer and a different job (imaging pipelines), not night-session planning.

**R7. Quiz / stories / infographics / guided tours / "sponsor a star" (Star Walk 2, Star Chart).**
Reject: they serve the *learning* audience; our buyer pays to plan a shoot, not to be taught. Copying
a free giant's content surface is both a distraction and a near-duplicate risk.

**R8. A full light-pollution map with Bortle overlays (Light Pollution Map 500K+, Good To Stargaze).**
Reject as a *feature parity* target: a `500K+` free incumbent owns it, and the engine map says Bortle
needs a new bundled raster. Adapt to the Moon-wash readout (A11) instead.

**R9. Copying an incumbent's data or assets.** Name and move on:
- **Deep-sky catalogues:** OpenNGC **CC BY-SA 4.0** (README L15), NGC 2000.0 non-commercial, SAC
  ambiguous, Stellarium DSO **GPL** — none shippable in a paid closed-source APK. Use our hand-compiled
  ~250-object list (`MEASURED`, market record §6.1; I §2).
- **Star catalogues:** HYG **CC BY-SA 4.0**, Hipparcos/Tycho/Gaia **CC BY-NC 3.0 IGO**, BSC5 no
  affirmative grant — use **FK6 (CC0)** + **2MASS** (I §1).
- **Milky Way texture:** the shared Gaia DR2 colour panorama is **CC BY-SA 3.0 IGO** — self-bake
  instead (J §3.3).
- **UI:** copy patterns, never a competitor's screenshots, icon set or layout files.

**R10. Eclipse / tide / rainbow / black-and-white-developing calculators (Planit Pro, Photographer's
companion).** Reject: each is a different job or a one-off event; they pad the app without answering
"will tonight work?" (`.cursor/rules/app-depth.mdc` — depth is the job's handoffs, not a settings
screen of switches).

---

## 4. UI/UX patterns worth copying (patterns, never code or assets)

**P1. One-handed bottom navigation + minimised info panels.** Sky Tonight states its design rationale
explicitly — *"designed for real stargazing conditions: bottom navigation keeps key tools within thumb
reach. Minimized info panels let you explore the sky without distractions"* (`MEASURED`). A
six-screen app with a thumb-reachable primary action beats a top-heavy layout at night, in the cold.
Also the `ui-ux-baseline.mdc` 48 dp-target requirement.

**P2. Time as a scrubber with a "time machine" feel.** Star Walk 2 ("adjustable time scale"), Sun
Surveyor ("visual time machine"), Sky Guide (cinematic time controls, iOS). Our hero is a scrubber;
the pattern to copy is *continuous motion of every object with the scrub* and a *stage name as a
label, never a keyframe* (J §5.2).

**P3. A "Visible Tonight" list, not a star chart.** Star Walk 2 and Sky Tonight both headline a
plain "what can I see tonight" list. Our Targets screen should open on a **ranked, filtered list for
the chosen night** (up during the dark window, above the horizon, above the silhouette), not a
catalogue browser.

**P4. Critical-times list as inspiration ("Photo Opportunities").** Sun Surveyor's pattern: instead
of only showing numbers, present *"the full moon looming low in the sky during magic hour, total
darkness for stargazing, and more"* as prompts a user can tap into a plan (`MEASURED`). Adapt into the
Session brief's tap-to-plan rows.

**P5. Alignment search ("when will the Moon be *there*").** PhotoPills' **"Sun, Moon Alignments Fast
Finder"** and Sun Surveyor's **"Position Search"** both let the user search for a date/time an object
occupies a chosen position (`MEASURED`). Our scene already has the geometry; an "align to my horizon"
search is the planner-shaped version of the scrub.

**P6. Condition colour-coding on an hour grid, as Clear Outside does.** Clear Outside shows low/mid/
high/total cloud, moonrise/set/phase, civil/nautical/astronomical darkness and the **ISS pass** on one
hourly grid (`MEASURED`) — the clearest presentation on the whole shelf (and it earns the shelf's
weakest rating only because its UI/UX is old). Our timeline should copy the *alignment* of conditions
to the darkness window, not the visual style.

**P7. A live "darkness countdown".** Light Pollution Map: *"a live countdown to true night"*
(`MEASURED`). Cheap, and it is the single most actionable sentence for the buyer.

**P8. First-run / onboarding that assumes no astronomy knowledge.** Measured complaints include Star
Chart's *"There is no tutorial for this"* and Stellarium's *"confusing without explanation of why
zooming out will suddenly show you coordinates"* (`MEASURED` review text). Because we do **not** use
the compass, the first-run must explicitly say so — the buyer arrives from apps that do, and will look
for the "point at the sky" behaviour that we deliberately lack. A 3-tap "plan your first session"
flow plus obvious empty states for Spots and Targets is the fix.

**P9. Shareable composed image.** Sky Tonight ships a built-in image editor to *"create clean and
beautiful sky images to share"* (`MEASURED`). Our planned Share postcard is the strongest
organic-acquisition surface and should compose the *scene* (horizon + sky + plan data), not a data
dump.

**P10. Offline-first.** Sun Surveyor ("offline usage"), Planit Pro ("offline elevation files and
offline mbtiles"), Star Walk 2 ("works offline"), Astrospheric ("Site Mode … even if you don't have a
network connection") — an outdoor app that dies without signal is a review complaint. Weather is the
only networked piece; everything else must work in airplane mode.

---

## 5. Top three risks this analysis exposes

**Risk 1 — the willingness-to-pay for a *combined* brief is UNVERIFIED.**
No incumbent sells dark-window + deep-sky + satellite + cloud in one flow (market record §3.2,
§10 item 4). The evidence *for* it is real but indirect: every paid light-planner bundles night
content into its paid product (`INFERRED`, five MEASURED quotes), and a pinned Sky Tonight review
says the buyer *"would have paid extra just to have everything all in one app"* (`MEASURED`). The
evidence is not a sale. **Settle by** reading Play's full paid-planner review lists (Play
server-renders only 3/page) and, ultimately, by the price paid at launch.

**Risk 2 — a beautiful scene is not a planner, and this shelf pays for planners.**
The best-designed sky apps (Star Walk 2, Sky Guide, Stellarium) are free or ≤$2.99, and the LIVE
identification shelf dwarfs planning ≈500×. The apps that charge $9.99–$10.99 win on **planner
legibility** — alignment search, a critical-times list, an exposure calculator, a pass list — not on
a rendered sky. J's own reference-app read says exactly this: PhotoPills wins on *clarity of the plan*
(J §6). If Lumintra's scene does not terminate in an actionable Session brief and a concrete "go at
HH:MM" answer, the $9.99 buyer sees a pretty toy. **This risk is the reason A3, A4, A9 and A11 are
above any further visual polish.**

**Risk 3 — licence and third-party-feed fragility can block the product late.**
The measured record is unambiguous: OpenNGC CC BY-SA killed the natural DSO catalogue and forced a
hand-compiled list; the Gaia Milky Way panorama is CC BY-SA; Gaia/HYG are non-commercial; `api.met.no`
is the only usable free cloud feed and it has **no SLA**, giving total cloud only; CelesTrak's
commercial-use policy is **UNVERIFIED**. A single licence surprise or feed change can strand an
asset or a capability after the UI is built. The mirror image is the opportunity: TPE **retired its
Android app in 2021** and left its Android users without an update (`MEASURED`), on the exact shelf we
target. **Mitigations:** settle CelesTrak's policy before shipping; keep the deep-sky list
hand-compiled; self-bake the Milky Way; and make weather degrade gracefully.

*(A fourth, secondary risk the satellite wedge carries: a *wrong* streak prediction is worse than no
prediction, and the pass-detector trap is a measured, reproduced defect in the engine path (market
record §7). A trust-eroding false "clean" is a review risk, not just a bug.)*

---

## 6. CLAIM / LABEL / EVIDENCE ledger

```text
CLAIM / LABEL / EVIDENCE
PhotoPills listing: 4.8 (8.22K reviews), "2D Map-Centric Planner", alignment fast finder, 3D AR, photo-plans manager, location scouting, exposures/star-trail/FoV calculators, widgets / MEASURED / play.google.com/store/apps/details?id=com.photopills.android.photopills read this session
PhotoPills current price is $10.99 / MEASURED / appbrain.com/app/photopills shows "$10.99"; appstorespy shows $10.99; apprundown price history "current · $10.99" since 2025-12-01 (the $9.99 in a 2019 review is the old price)
Sun Surveyor listing: $9.99, 4.8 (5.14K), AR paths, 3D compass, Milky Way & star trail, "Photo Opportunities", "Position Search", visual time machine, sun shadow length, offline, .kmz/.kml import/export, 9 widgets / MEASURED / play.google.com/store/apps/details?id=com.ratana.sunsurveyor (and en_IE showing "$9.99 Buy") this session
Planit Pro listing: 4.6 (1.22K), simulated viewfinder, nebulae azimuth/elevation, star-trail planning, timelapse/sequence simulation, Milky Way searching, eclipse, exposure/ND calculator, light meter, rainbow, tide, offline maps / MEASURED / play.google.com/store/apps/details?id=com.yingwen.photographertoolspro this session
PhotoTime listing: free/IAP, 4.3 (9.25K), sun/moon path map, AR, scout & save locations, twilight, moon calendar, widgets, smart notifications, weather, DoF/FoV calculators, long exposure/timelapse/star-trail planning, Milky Way visibility, 3D shadow map / MEASURED / play.google.com/store/apps/details?id=pl.guteklabs.phototime this session
Photographer's Tools Pro features and ~€2.39 price / ON RECORD; price UNVERIFIED / photographer-s-tools-pro.apps112.com (Play description mirror) + thezenapps-photoassistant.andro.io + androidsis article, read this session
Photographer's companion Pro listing: 4.9 (633), 40+ tools, AR Sun/Moon/Milky Way, star-trail simulator, night mode, night-lights mode, EV/DoF/reciprocity failure, import/export / MEASURED / play.google.com/store/apps/details?id=com.stefsoftware.android.photographerscompanionpro this session; its price shown in search synthesis as $3.49 but not rendered on the listing / UNVERIFIED
The Photographer's Ephemeris for Android was available 2011-2021 and is retired, not purchasable / ON RECORD, MEASURED / photoephemeris.com/en/help/general/where-is-your-android-app/ read this session ("now withdrawn from sale and officially retired")
TPE markets a 3D Milky Way band, TPE 3D terrain light+shadows, and a desktop 3D Celestial Sphere / ON RECORD / market record §2.2 / J §6 (photoephemeris.com, read in the prior recon)
Stellarium listing: 10M+, 4.8 (337K), realistic atmosphere/sunrise/sunset+refraction, realistic Milky Way, twinkling, satellite tracking incl. ISS, red night mode, Plus adds observability/transit tools and offline data / MEASURED / play.google.com/store/apps/details?id=com.noctuasoftware.stellarium_free this session; Stellarium Plus $19.99 shown in the listing sidebar
Star Walk 2 Plus listing: 10M+, 4.5 (572K), "Sky Live and Visible Tonight", adjustable time scale, night-mode interface, AR, infographics, Planet Walk / MEASURED / play.google.com/store/apps/details?id=com.vitotechnology.StarWalk2Free this session
Sky Tonight listing: 5M+, 4.7 (85.7K), "what celestial events can I see tonight", Visible Tonight, Astronomy Calendar, Stargazing Index, one-handed bottom navigation, minimised info panels, built-in image editor to make shareable sky images, night mode / MEASURED / play.google.com/store/apps/details?id=com.vitotechnology.sky.tonight.map.star.walk this session
A Sky Tonight reviewer states the app's only missing thing is live satellite tracking and they would have paid extra to have everything in one app / MEASURED / review text on the same listing, read this session
SkySafari 8 Pro listing: 1K+, 4.2 (211), Events Finder, Observing Conditions (Premium) hourly forecast with best viewing windows + seeing + sky quality, Night Vision Mode, rotatable FoV indicator, image import + plate solving, LiveSky cloud sync, telescope control / MEASURED / play.google.com/store/apps/details?id=com.simulationcurriculum.skysafari8pro this session
SkySafari prices $34.99 (8 Pro) / $29.99 (8 Plus) / $9.99 (7 Plus) / $6.99 (Astronomy) / MEASURED / market record §2.1 (enrich-market.mjs); the Lunescope sidebar also showed "$7.99" for SkySafari 7 Plus this session
Star Chart listing: 10M+, 4.3 (174K), 20M stars, Hevelius art, Messier, guided tours, time shift ±10000 years, voice control, eclipse/meteor notifications / MEASURED / play.google.com/store/apps/details?id=com.escapistgames.starchart this session
Sky Map listing: 50M+, 4.1 (482K), free no ads/IAP, widgets Moon phase / Tonight's sky / sky countdown, alt-az grid, optional per-feature permissions / MEASURED / play.google.com/store/apps/details?id=com.google.android.stardroid this session
Sky Guide is not available on Android / ON RECORD / space.com review ("No Android version") + fifthstarlabs.com (no Android) read this session
ISS Detector Pro listing: $17.99 (market record), 100K+, 4.4 (3.19K), radio satellites + Doppler, Famous Objects, comets/planets, Iridium flares, pass overview, weather check, alarms / MEASURED / play.google.com/store/apps/details?id=com.runar.issdetector.pro this session (price from market record §4.1)
ISS Detector free is 5M+ / MEASURED / market record §4.1
AstroSat forecasts satellite transits through a photographer's exact FoV using optics, camera, exposure length and session time, and names landscape/astro-photographers as users / MEASURED / astrosat.org + github.com/james-m-osborn/astrosat (22 stars) + arXiv 2111.11268, read this session
No shipped Android paid app was found bundling satellite-streak forecasting into a light-planner / INFERRED / the satellite shelf (§1.3) is pass/free-ad apps; streak forecasting appears only as research/web tools (AstroSat, Tonight's Window, PSF Guard) — settle by a Play search for "satellite trail planner"
Tonight's Window finds the cleanest imaging window (the Earth-shadow window near local midnight) for a location / MEASURED / orbitalnodes.ai/satellite-trails read this session
Astrospheric listing: 100K+, 4.5 (804), 84h hourly forecast, ensemble cloud, transparency+seeing, smoke forecast, Kp aurora, ISS flyover paths/times, Pro weather alerts, offline Site Mode / MEASURED / play.google.com/store/apps/details?id=com.astrospheric.dfior.astrospheric this session
Clear Outside listing: 100K+, 3.8 (1.37K), 7-day hourly, low/medium/high and total cloud, moonrise/set/phase, civil/nautical/astronomical darkness, ISS pass-over beside the cloud forecast, favourite locations / MEASURED / play.google.com/store/apps/details?id=com.firstlightoptics.clearoutside this session
Light Pollution Map listing: 500K+, 4.7 (7.33K), light-pollution overlay + Bortle, Horizon Safe Radius, live sky conditions, "Darkness Timer … live countdown to true night", aurora Kp, Moon Position Tool to plan landscape astrophotography compositions, ISS tracker, night-sky calendar, astrophotography calculators, custom alerts, location manager, widgets, night & AMOLED themes / MEASURED / play.google.com/store/apps/details?id=com.pa.lightpollutionmap this session
Good To Stargaze: 50K+, 3.9 (553), subscription plans Value $0.99/mo · Hobbyist $3.99/mo or $25.99/yr · Pro $49.99/yr, and a pinned review rejecting the recurring price as data found elsewhere / MEASURED / play.google.com/store/apps/details?id=com.goodtostargaze + apps.apple.com IAP list, read this session
My Aurora Forecast Pro is $3.99, 50K+ installs, 4.78 (3.8K), Kp index, 1h/hours/weeks forecasts, push alerts, solar wind, webcams / MEASURED / play.google.com/store/apps/details?id=com.jrustonapps.myauroraforecastpro + appbrain.com this session
Lunescope Pro is $3.99 one-time / MEASURED / play.google.com/store/apps/details?id=com.daylightmap.moon.pro.android this session (appviewable adds 100K+ installs)
My Moon Phase Pro is $2.99, 4.8 (2.41K), cloud cover for upcoming days, golden/blue hour, widgets / MEASURED / play.google.com/store/apps/details?id=com.jrustonapps.mymoonphasepro this session
Daff Moon Phase is free, 4.9 (55.3K), rise-set/alt-az/eclipses/golden+blue hours/widgets/event notifications / MEASURED / play.google.com/store/apps/details?id=com.dafftin.android.moon_phase this session
Vixen "Light for Night vision" turns the screen into an adjustable red light for dark-adapted eyes / MEASURED / play.google.com/store/apps/details?id=jp.co.vixen.nightvisionlight this session
Nightcam is a capture app (star trails/Milky Way/moon modes), a different job from planning / MEASURED / play.google.com/store/apps/details?id=com.rayinformatics.nightcam this session
Planit Pro's top sampled complaints are that plans and a purchased add-on do not travel between devices / MEASURED / listing review text this session; market record §3.1
PhotoPills' top sampled complaint is paying again on another platform / MEASURED / listing review text this session; market record §3.1
Sun Surveyor's most recent negative review is a compass-accuracy complaint / MEASURED / listing review text (Jeff Corcelius, Nov 2024) this session
Star Walk 2 and Sky Map reviews include compass drift / calibration and misplaced-object complaints / MEASURED / listing review text this session
Star Chart's review text includes "There is no tutorial for this" / MEASURED / listing review text this session
Stellarium's review text includes confusion about unexplained zoom/coordinate behaviour / MEASURED / listing review text this session
LIVE apps own the stargazing head terms: every 10M+ app and Sky Map 50M+ is LIVE; largest pure-PLANNING is 100K+ (≈500x) / MEASURED / market record §2.1 (B-stargazing-shelf.md §2-§3, 25 listing descriptions)
Cloud cover is absent from the light-planning shelf's own descriptions / MEASURED / market record §3(d) keyword scan of 10 descriptions
Night content is bundled into every paid light-planner / INFERRED / five MEASURED description quotes, market record §3.2
Willingness-to-pay for a combined dark-window + deep-sky + satellite + cloud brief / UNVERIFIED / market record §3.2, §10 item 4; settle via Play's full paid-planner review lists and the launch price
OpenNGC is CC BY-SA 4.0 (README L15, no LICENSE file); NGC 2000.0 is non-commercial; Stellarium DSO is GPL / MEASURED / market record §6.1; I §2
HYG is CC BY-SA 4.0 and Hipparcos/Tycho/Gaia are CC BY-NC 3.0 IGO; FK6 is CC0 and 2MASS is freely usable with acknowledgement / MEASURED / I §1 (licences read at their own sources this session, per I's ledger)
The shared Gaia DR2 colour Milky Way panorama is CC BY-SA 3.0 IGO and cannot ship / MEASURED / J §3.3 (kam-ui credit line, read verbatim)
api.met.no is CC BY 4.0 with attribution, keyless, hourly total cloud only, no low/mid/high split / MEASURED / market record §6.3 (TermsOfService read this session in the prior recon)
CelesTrak GP JSON is keyless HTTP 200 but its usage policy for a commercial app is UNVERIFIED (its policy page returned HTTP 500) / MEASURED (absence), UNVERIFIED / market record §6.2/§6.4, §10 item 3; settle at celestrak.org in a browser
Astronomy Engine for Kotlin/JVM is MIT / MEASURED / market record §6.4 (astronomy.kt:2-7)
No competitor's source, screenshots or assets were copied; patterns only / MEASURED / this report contains no incumbent code or asset, and only listing text was read
```

**Honest limits.** This is a listing-and-publication read, not an in-app audit: I read **what the
incumbents say** and **their users' pinned reviews**, not what their pixels do. A feature an app
advertises may be worse or better in practice (PhotoPills' listing, for instance, carries *"NOTE: The
Augmented Reality views of this app make use of your device's compass. Some devices may not have a
compass"* — a warning, not a demonstration). Install buckets and prices are the shelf record's own
measurements or the listing's own text; no revenue figure exists for any incumbent (Play exposes
none). The rank order in §2 is a judgement resting on the revenue reasons printed beside each row,
and the final call — whether a sceptical buyer pays $9.99 for the combined brief — stays with the owner.

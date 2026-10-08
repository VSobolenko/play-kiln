# NightVector: Astro Plan — proposal

## Decision

**PASS — propose `nightvector-astro-plan` for owner review, not build.** MEASURED — the 2026-10-08 Play scans and enrichments recorded in `.kiln/scratch/astro-recon-2026-10-08/` show paid, high-install planning incumbents plus a relevant, mixed/candidate astro-planning shelf.

**Job:** A paid night-sky planning suite tells an astrophotographer or visual observer where and when to point, what conditions to expect, and what exposure approach to start with before driving to a dark-sky location. INFERRED — this joins the separate jobs surfaced by `astrophotography planner`, `photo planner sun moon`, `deep sky planner` and `satellite tracker app`; it is not a generic calculator because the output is an ordered session plan from orbital, ephemeris, weather and equipment inputs.

**Paying user and purchase:** An astrophotographer or visual observer buys the complete $10.99, no-ad app from Play before creating an upcoming outing plan. MEASURED — PhotoPills is listed at $10.99, 4.8 stars, 8,220 displayed reviews and 100K+ installs; INFERRED — a no-ad paid listing is appropriate for an equipment-oriented planning session but conversion remains unmeasured.

**Payment moment:** Purchase occurs at the Play listing before the user creates a multi-target trip plan. INFERRED — paid-listing competitors demonstrate a one-time purchase shape, while no free-to-paid conversion observation is available.

## Coherent suite, not a bag of tools

The suite follows one trip-planning flow: select a scout and equipment, discover a viable dark window, choose targets and passes inside it, receive settings guidance, then take or share the brief. INFERRED — this sequence connects the five feature areas below around one before-you-drive decision.

1. **Sky conditions and darkness:** local civil/nautical/astronomical twilight, moonrise/set, moon illumination and usable dark-window length. INFERRED — the measured shelf contains Sun/Moon planners and night-sky planners, so this is a required decision input rather than a standalone moon app.
2. **Milky Way and deep-sky targets:** Milky Way core plus named deep-sky target altitude/azimuth curves and visibility windows. MEASURED — `deep sky planner` returned 30 hand-classified on-job cards, and `milky way planner app` returned 26/29 hand-classified on-job cards in `.kiln/scratch/astro-recon-2026-10-08/titles.md`.
3. **Planetary and satellite plan:** selected planet visibility and ISS/Starlink pass predictions propagated from current orbital elements. MEASURED — `satellite tracker app` returned 26/30 hand-classified on-job cards and includes ISS Detector at 5M+ installs in its title/enrichment readings.
4. **Photography guidance:** target-specific tracked/untracked exposure starting points and star-trail guidance from the selected focal length, aperture and target motion assumptions. INFERRED — the $10.99 PhotoPills and $9.99 Planit Pro paid listings demonstrate a photo-planning price point; the exact preference for this guidance is untested.
5. **Scout and forecast:** saved locations, horizon/equipment assumptions and a weather forecast for the selected trip. INFERRED — a repeated location-specific decision needs persistence; Open-Meteo is proposed only as an advisory forecast source.

This differs from already-proposed `skywindow-drone`: SkyWindow decides whether a drone launch meets weather thresholds, whereas NightVector plans an astronomy session using celestial visibility, lunar light, target geometry, satellite propagation and exposure assumptions. ON RECORD — `docs/ideation/plans/skywindow-drone.economics.json` defines the drone product as launch-window planning.

## Market evidence

The broad screen returned `mixed` for astrophotography planner (23/30 rated cards, 11 weak), Milky Way planner (18/29, 4 weak), night sky planner (25/30, 6 weak), photo planner sun moon (24/30, 8 weak), deep sky planner (26/30, 9 weak), astronomy planning (30/30, 8 weak), ISS pass (8/14, 1 weak), satellite tracker (20/30, 5 weak), light-pollution map (17/20, 3 weak), and golden-hour planner (17/30, 1 weak). MEASURED — `node scripts/scan-market.mjs` output at `.kiln/scratch/astro-recon-2026-10-08/market.md`.

The same screen returned `candidate` for star trail calculator (7/11 rated, 4 weak) and telescope planner (23/30 rated, 13 weak), but neither single-purpose shape is proposed. MEASURED — `.kiln/scratch/astro-recon-2026-10-08/market.md`; ON RECORD — this proposal obeys the owner’s four-feature minimum rather than reducing the job to the candidate’s one operation.

Hand classification found 29/30 on-job cards for astrophotography planner, 26/29 for Milky Way planner, 30/30 for deep-sky planner, 30/30 for telescope planner, and 26/30 for satellite tracker; the script’s lexical guard disagreed on several and marked the first and third shelves `polluted`. MEASURED — `node scripts/scan-titles.mjs` output at `.kiln/scratch/astro-recon-2026-10-08/titles.md`; the hand share, not the guard alone, is used here.

**Paid ceiling evidence:** PhotoPills (`com.photopills.android.photopills`) is the highest-priced enriched planning incumbent at $10.99, 4.8 stars, 8,220 displayed reviews and 100K+ installs. MEASURED — `node scripts/enrich-market.mjs --query "photo planner sun moon" --top 8` at `.kiln/scratch/astro-recon-2026-10-08/enrichment.md`.

**Other paid evidence:** Planit Pro is $9.99 at 4.6 stars, 1,220 reviews and 10K+ installs; Sun Surveyor is $9.99 at 4.8 stars, 5,140 reviews and 100K+ installs; DeepSkyCamera Pro is $9.49 at 3.9 stars and 1K+ installs; SkySafari Astronomy is $6.99 at 4.3 stars and 10K+ installs; Milky Way Tonight is $7.99 at 100+ installs. MEASURED — `.kiln/scratch/astro-recon-2026-10-08/enrichment.md`.

**Highest-install enriched incumbent:** Sky Map has 50M+ installs in the Milky Way and deep-sky enrichments, while Stellarium has 10M+ and 337,000 displayed reviews. MEASURED — `.kiln/scratch/astro-recon-2026-10-08/enrichment.md`; INFERRED — these are discovery/sky-map audiences, not evidence that a paid newcomer captures their install base.

The sampled Planit Pro complaints include “First, the developer is very responsive and that is terrific. BUT...I have just discovered that the 29 plans that I have on my cellphone can't be easi” and a report concerning a purchased Location Explorer feature on three devices. MEASURED — `node scripts/scan-complaints.mjs --package com.yingwen.photographertoolspro` at `.kiln/scratch/astro-recon-2026-10-08/complaints.md`; the truncated quoted review is reproduced exactly as the sensor printed it.

The sampled Mobile Observatory complaint says its latest version “seems over complex and less user friendly,” and the sampled Satellite Chasers complaint says “It works, but it's reaaaaallllly slow.” MEASURED — `.kiln/scratch/astro-recon-2026-10-08/complaints.md`; these samples describe vocabulary, not complaint frequency.

## Wedge

**NightVector produces one offline-capable, location-specific imaging brief that joins dark-window timing, deep-sky target altitude curves, satellite passes, weather and target-specific exposure guidance, rather than making the user reconcile separate sky-map, satellite and photo-planning apps.** INFERRED — the measured search shelves and their distinct titles evidence fragmentation, while conversion for the joined workflow remains untested.

## Revenue model and ceiling

The selected archetype is `purchase_only_no_ads`: a $10.99 paid listing, with no subscription and no in-app purchase requirement. MEASURED — paid PhotoPills, Planit Pro and Sun Surveyor listings establish $9.99–$10.99 shelf pricing; ON RECORD — subscriptions are prohibited by the owner’s instruction in `AGENTS.md`.

`purchase_led_single_unlock` is rejected because a $10.99 upfront paid listing is directly measured on the target shelf and avoids creating an unmeasured free funnel. INFERRED — it could still work, but no observed conversion supports preferring it.

`purchase_led_repeat_sales` is rejected because session plans are not a legitimate consumable; selling repeated access to the same local computational tool would be artificial. INFERRED — the product has no paid external service or content pack cost.

`ads_led_utility` is rejected because the target moment is a concentrated pre-trip planning task and the paid shelf proves ad-free purchase demand. MEASURED — the paid incumbents above declare no ads; INFERRED — ads would harm rather than fund this paid decision workflow.

**Target arithmetic:** assuming a 15% platform deduction, net per $10.99 sale is `$10.99 × 0.85 = $9.34`; `$1,000 ÷ $9.34 = 107.1`, therefore the plan needs **108 sales/month** to reach at least $1,000 monthly net. INFERRED — the deduction assumption and conversion are not Play observations; UNVERIFIED — the target is reachable because the displayed 100K+ PhotoPills install figure is cumulative and does not expose monthly paid buyers or a newcomer’s conversion.

**Honest ceiling:** at a hypothetical 1% monthly conversion of PhotoPills’ *lower-bound* 100,000-install audience, 1,000 sales × $9.34 would net about **$9,342/month**. INFERRED — this is a ceiling scenario, not a forecast; it requires NightVector to reach an equivalent paid-planning audience, communicate the unified workflow clearly, earn a comparable trust level, and convert 1% of that lower-bound audience in a month.

The ceiling can exceed that only if the app reaches demand beyond PhotoPills’ lower-bound paid audience, converts a materially higher share, or earns in non-US storefronts at comparable net price. UNVERIFIED — Play listings do not expose those sales, geography, conversion or revenue facts.

## Data, offline behaviour, privacy and permissions

Celestial position, twilight, moon, planetary target altitude and exposure calculations run on-device from packaged astronomical algorithms and data. INFERRED — this is the design boundary; implementation must validate calculation accuracy before any claim of precision.

Weather uses `https://api.open-meteo.com/v1/forecast` without a client API key, subject to Open-Meteo’s licence and attribution requirements. MEASURED — Open-Meteo’s official licence page was read on 2026-10-08; UNVERIFIED — exact production attribution wording must be rechecked at implementation.

Satellite data uses CelesTrak GP/OMM JSON queries such as `https://celestrak.org/NORAD/elements/gp.php?GROUP=STATIONS&FORMAT=JSON`, cached locally and refreshed no more frequently than the supplier update window. MEASURED — CelesTrak documents the endpoint and warns that GP data updates every two hours; ON RECORD — CelesTrak requires clients to respect its usage policy.

Offline, the app shows packaged target data and locally computed twilight/target geometry for saved scouts, plus the timestamp and staleness of the last weather and orbital fetch. INFERRED — no live source can be silently represented as current while offline.

The app needs network access (`android.permission.INTERNET`) for weather and orbital refreshes; it needs approximate/precise location (`ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION`) only when the user chooses current location. INFERRED — the Android implementation and permission choice have not been made.

**Server:** no owned server is proposed. INFERRED — all listed computations are client-side and the two public endpoints are keyless, but supplier availability is an external dependency.

## Device capabilities

- camera: not used — INFERRED — the app plans a session but does not capture it.
- photo picker / gallery: not used — INFERRED — no image import is needed for the planning job.
- media playback or capture: not used — INFERRED — no media capture or playback is part of the planning flow.
- microphone: not used — INFERRED — no audio input serves celestial planning.
- location: used — `FusedLocationProviderClient` / `LocationManager` supplies the current scout; without it the user must manually enter a location; manifest permission: `ACCESS_COARSE_LOCATION` and optional `ACCESS_FINE_LOCATION`. INFERRED — implementation choice remains open.
- motion sensors: not used — INFERRED — compass/AR alignment is outside this plan-first scope.
- contacts / calendar: not used — INFERRED — the brief is shared, not written into another person’s calendar.
- notifications / reminder: used — `WorkManager` plus `NotificationManagerCompat` alerts before a chosen window; without it the plan is easy to miss after closing; manifest permission: `POST_NOTIFICATIONS` on Android 13+. INFERRED — implementation choice remains open.
- home-screen widget: not used — INFERRED — a widget is not required to generate or hand off the session plan.
- share / export: used — `Intent.ACTION_SEND` and Android Sharesheet export the session brief; without it the plan cannot leave the device; no manifest permission. INFERRED — implementation choice remains open.
- Bluetooth / USB: not used — INFERRED — telescope control and hardware integration are intentionally excluded.
- files / SAF: not used — INFERRED — the initial brief uses Sharesheet rather than a document provider.

## Promise contract

The machine-readable promises are [nightvector-astro-plan.differentiators.json](plans/nightvector-astro-plan.differentiators.json). ON RECORD — the contract binds before to `save-a-dark-sky-scout`, during to `compose-the-night-plan`, and after to `share-the-session-brief`.

The connective floor is the `data_out` brief promise, whose eventual proof must contain `ACTION_SEND`. ON RECORD — `docs/slot-differentiators.md` requires a connective handoff with its named API.

## Naming

The proposed title is **NightVector: Astro Plan**; its score is 80/100 and it has seven characters below the 30-character title cap in both declared locales. MEASURED — [nightvector-astro-plan.naming.json](plans/nightvector-astro-plan.naming.json) records the scoring and 23-character title lengths; UNVERIFIED — registry trademark searches have not been conducted.

## Visual direction

Use a low-glare black-blue field with a restrained red night-vision mode, thin ephemeris arcs and a single amber “best window” cue rather than a cartoon star map. INFERRED — the visual system is a proposed usability direction, not an observed market advantage.

## Kill criteria and build estimate

Kill after 90 days if fewer than 250 installs or less than $1,000 recorded net revenue is observed. ON RECORD — [nightvector-astro-plan.economics.json](plans/nightvector-astro-plan.economics.json) sets the three numeric criteria: 90 days, 250 installs, $1,000 revenue.

Estimated implementation effort is **80–120 agent hours** for astronomy calculations, source caching, SGP4 propagation, session composition, persistence, export/reminders, responsive UI, localisation and validation. INFERRED — this is intentionally a substantial multi-feature app estimate, not a measured duration.

## Honest unknowns

- The $10.99 paid app’s conversion, monthly sales and net revenue are **UNVERIFIED**; Play exposes install buckets and listing price, not P&L.
- Astronomy and SGP4 calculation accuracy against authoritative references is **UNVERIFIED** until a test corpus is chosen and passes.
- Open-Meteo forecast suitability at remote dark-sky sites is **UNVERIFIED** until representative location/date comparisons are recorded.
- CelesTrak usage limits and long-term availability are **UNVERIFIED** as a product dependency; caching and graceful stale-data display are mandatory mitigations.
- The NightVector name’s trademark headroom is **UNVERIFIED** pending registry screens.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
| --- | --- | --- |
| Paid astro-planning listings reach $9.99–$10.99 | MEASURED | `node scripts/enrich-market.mjs`, `.kiln/scratch/astro-recon-2026-10-08/enrichment.md` |
| PhotoPills is the highest-priced enriched incumbent at $10.99 / 100K+ | MEASURED | same enrichment, `photo planner sun moon` table |
| Astro and satellite shelves are substantially on-job by hand classification | MEASURED | `node scripts/scan-titles.mjs`, `.kiln/scratch/astro-recon-2026-10-08/titles.md` |
| A unified session brief is a paid wedge | INFERRED | fragmentation across measured shelves; needs conversion evidence |
| $1,000 net needs 108 monthly sales at $10.99 and assumed 85% net | INFERRED | arithmetic in Revenue model; platform deduction unobserved |
| The $1,000 target is reachable | UNVERIFIED | install buckets are cumulative and do not reveal monthly buyer conversion |
| CelesTrak offers documented GP query formats and update guidance | MEASURED | official CelesTrak GP documentation read 2026-10-08 |

**Falsifier:** Refuse or kill this proposal if owner review or a fresh direct comparison shows that a paid incumbent already produces the same single chronological brief with dark window, deep-sky altitude, satellite passes, weather and exposure guidance, or if 90-day outcomes record fewer than 250 installs or less than $1,000 net revenue. INFERRED — the first observation invalidates the functionality wedge; ON RECORD — the latter observations are the declared kill criteria.

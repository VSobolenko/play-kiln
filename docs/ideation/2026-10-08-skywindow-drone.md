# SkyWindow: Drone Windows — proposal

## Decision

**GO for owner decision, not for scaffold.** MEASURED (`node scripts/scan-market.mjs ... --json`, saved at `.kiln/scratch/2026-10-08-conditions-scan.json`): the exact `drone flight conditions` query screened as `candidate`, with 12 of 20 rated incumbents below 4.2 stars.

**Job:** a recreational drone pilot chooses a weather-suitable launch window for a location and flight profile, receiving a ranked go/no-go plan with the reasons behind every pass and failure. INFERRED (the measured 29/30 on-job title share and the distinct drone-weather listings below): this is a conditions advisory and planner, not a record book, a flight log, an airspace authorisation, or FAA Part 107 study; it is therefore distinct from the already proposed `part-107-drills` exam-drill job.

## Market evidence

MEASURED (`node scripts/scan-titles.mjs --query "drone flight conditions" ...`, `.kiln/scratch/2026-10-08-drone-titles.md`): the relevance guard returned `partial` at 19/30, while hand classification found 29/30 visible cards on the drone-flight job, so the guard understates rather than invents relevance here.

MEASURED (`node scripts/enrich-market.mjs --query "drone flight conditions" --query "drone flight weather app" --query "uas flight planning weather" --top 8 --concurrency 3`, `.kiln/scratch/2026-10-08-drone-enrichment.md`): the first eight conditions results include UAV Forecast for Drone Pilots at 4.2 stars, 17,839 displayed reviews, 1M+ installs, free with IAP; Dronecast at 4.7 stars, 4,709 reviews, 100K+ installs, free with IAP; Drone Assist at 3.8 stars, 1,069 reviews, 100K+ installs, free without the displayed IAP badge; and B4UFLY at 1.5 stars, 2,337 reviews and 500K+ installs, free without IAP.

MEASURED (same enrichment): zero of the eight enriched `drone flight conditions` cards display ads, while four display IAP; this demonstrates a paid-feature precedent but does not prove revenue or a one-off-purchase precedent.

MEASURED (`node scripts/scan-complaints.mjs --query "drone flight conditions" --query "drone flight weather app" --top 6 --concurrency 2`, `.kiln/scratch/2026-10-08-drone-complaints.md`): the conditions sample contains 18 served reviews, 11 critical reviews, and the most common labelled complaint categories are broken and missing-feature at three each.

MEASURED (same complaint scan): a B4UFLY reviewer wrote, “Great App, but it can't be a sole resource. It does NOT include wilderness area!”, an Aloft Air Aware reviewer wrote, “Why can I not submit a flight window that isn't just now for a duration of 60 min? I would like to be able to submit a flight window for upto 24 hours”, and a UAV Forecast reviewer wrote, “Now they require subscriptions for any forecast other than current (even just a few hours ahead).”

**Weak mid-tier pocket:** MEASURED (enrichment and complaint scan above): B4UFLY has a 1.5-star, 500K+-install reading and Drone Assist has a 3.8-star, 100K+-install reading; both show demand at scale while leaving a reachable quality and workflow opening.

## Product and wedge

**Wedge:** SkyWindow turns free weather data into named, altitude-aware launch windows that show each failed threshold, instead of making the pilot interpret weather cards or buy a recurring forecast horizon. INFERRED (the measured subscription complaint, future-window request, and several weather-only shelf titles): this is a functionality wedge rather than a name or colour claim.

The app has four substantive flows: select or search a launch location; set and save a mission profile; inspect an hourly and seven-day ranked window plan with reasons; and share or schedule a chosen launch brief. INFERRED (the job decomposition in this proposal): these flows finish the conditions decision without storing a log as the product's value.

The advisory never says a flight is legally authorised and always asks the pilot to check applicable airspace, local rules, aircraft limits, and real-time conditions. INFERRED (the safety risk of a weather-derived verdict): the absence of airspace data is a deliberate scope boundary, not a claim that weather is the only flight criterion.

## Data, network and offline mode

**Endpoint:** MEASURED (`Invoke-WebRequest` returned HTTP 200 on 2026-10-08): `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&hourly=temperature_2m,precipitation_probability,wind_speed_10m,wind_gusts_10m&forecast_days=7` is a key-less public forecast endpoint suitable for the hourly threshold evaluator.

**Terms/licence:** MEASURED (`Invoke-WebRequest https://open-meteo.com/en/terms`, 2026-10-08): the provider's rendered terms page describes Open-Meteo as an open-source weather API with free access for non-commercial use and no API key; commercial-use permission and attribution obligations are therefore UNVERIFIED pending an owner decision or provider confirmation before implementation.

**Offline:** the app displays the timestamped last successful forecast as stale and suppresses new go/no-go recommendations when no cached forecast exists. INFERRED (a network forecast cannot be refreshed offline): it never fabricates a current weather verdict.

**Server:** none. INFERRED (the client calls the public key-less endpoint directly): no proprietary backend, secret, keyed API, or client-billed API is introduced.

## Monetisation and revenue arithmetic

**Chosen archetype:** `purchase_led_single_unlock`; the paying user buys a $4.99 one-time unlock after trying today's result and requesting the seven-day plan, saved profiles, reminders, or sharing. INFERRED (four of eight enriched condition cards declare IAP and the complaint sample explicitly objects to subscriptions): a no-renewal unlock fits the shelf better than recurring billing, but willingness to pay $4.99 remains unverified.

**Arithmetic:** INFERRED (proposed $4.99 price and a 15% Play-service-fee assumption): $4.99 × (1 − 0.15) = $4.24 before applicable taxes; $100 monthly target ÷ $4.24 net per sale = 23.6, therefore **24 sales/month** to clear the target before tax. UNVERIFIED (the applicable fee and tax treatment): the money owner must confirm the exact deductions before pricing.

**Archetypes rejected:** `purchase_led_repeat_sales` is rejected because the forecast plan is not consumable content and there is nothing honest to sell repeatedly. INFERRED (the same seven-day forecast is the output): repeated charges would be artificial.

`ads_led_utility` is rejected because the eight-card conditions enrichment shows no declared ads and this job is used at a pre-flight safety decision moment where advertising weakens trust. MEASURED (enrichment) plus INFERRED (decision context): the evidence supports avoiding ads, not asserting that ads cannot earn.

`purchase_only_no_ads` is rejected because a paid-only listing has no free current-day result to prove the forecast-to-window value before purchase. INFERRED (the proposed first-dollar path): the single unlock is the lower-friction revenue test.

Subscriptions are banned by the factory contract. ON RECORD (`docs/slot-economics.md#subscription-is-banned`): no subscription option is proposed.

## Promise contract

The gate-ready promise contract is [`skywindow-drone.differentiators.json`](plans/skywindow-drone.differentiators.json). MEASURED (`docs/ideation/plans/skywindow-drone.differentiators.json`): it carries three core promises plus one supporting promise, covers before/during/after, and reserves `ACTION_SEND` as the data-out handoff proof.

## Device capabilities

- camera: not used — INFERRED (the app evaluates forecast data, not imagery): no camera API or permission is justified.
- photo picker / gallery: not used — INFERRED (no photo enters the weather decision): no picker API or permission is justified.
- media playback or capture: not used — INFERRED (the output is a plan and brief): no media API or permission is justified.
- microphone: not used — INFERRED (the profile is edited on screen): no microphone API or `RECORD_AUDIO` permission is justified.
- location: used — `FusedLocationProviderClient` supplies an optional current launch coordinate; without it the user must type or search every location; manifest permission: `ACCESS_COARSE_LOCATION`, requested only when the user chooses current location.
- motion sensors: not used — INFERRED (sensor movement does not improve a forecast-window verdict): no sensor API or permission is justified.
- contacts / calendar: not used — INFERRED (the plan is shared explicitly): no contacts or calendar API or permission is justified.
- notifications / reminder: used — `WorkManager` plus `NotificationManagerCompat` alerts before a saved good window; without it a pilot can miss the chosen time; manifest permission: `POST_NOTIFICATIONS` on Android 13+.
- home-screen widget: not used — INFERRED (a first release needs reliable plan and alert flows before a widget): no widget API or permission is justified.
- share / export: used — `Intent.ACTION_SEND` sends the launch brief; without it the observer or co-pilot cannot inspect the decision outside the app; no manifest permission is needed.
- Bluetooth / USB: not used — INFERRED (the app neither controls nor reads a drone): no Bluetooth or USB API or permission is justified.
- files / SAF: not used — INFERRED (the initial handoff is text sharing rather than a file): no SAF API or storage permission is justified.

## Visual direction, delivery and kill criteria

The visual direction is a dark pre-dawn flight board: an uncluttered hourly horizon with clear green/amber/red condition chips and a plain-language reason drawer, avoiding aviation-map density. INFERRED (the measured shelf contains weather and flight-planning products): the visual system distinguishes a decision tool from a generic forecast display.

Build estimate: 7–10 agent hours. INFERRED (four screens, public-network client, local profiles, threshold evaluation, notifications and Android Sharesheet): this is a substantial feature set rather than a two-button utility; the estimate is not a measured elapsed time.

Kill criteria: 400 installs and $100 gross-to-net-plan revenue within 60 days, plus fewer than 5% of reviewed launch-brief sessions reporting a forecast mismatch. INFERRED (the economics plan gives the first two numeric thresholds): the third requires an outcome-reading mechanism before release and is not presently measurable.

## Naming

The gate-ready naming record is [`skywindow-drone.naming.json`](plans/skywindow-drone.naming.json). MEASURED (`docs/ideation/plans/skywindow-drone.naming.json`): `SkyWindow: Drone Windows` scores 81/100 with 24 characters in both declared locales and records the generic `Drone Weather` rejection.

## Honest unknowns

- UNVERIFIED: whether Open-Meteo permits this commercial client use under the terms that will apply at launch; provider confirmation or a written commercial plan decides it.
- UNVERIFIED: whether $4.99 converts against free/IAP incumbents; a staged listing experiment with observed installs and purchases decides it.
- UNVERIFIED: whether the named threshold explanation is enough differentiation against UAV Forecast and Drone Assist; a side-by-side feature audit and user test decide it.
- UNVERIFIED: SkyWindow trademark headroom; a class-9 trademark screen decides it.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| The drone conditions shelf has a candidate screen and 12 weak rated incumbents of 20 | MEASURED | `node scripts/scan-market.mjs ... --json`; `.kiln/scratch/2026-10-08-conditions-scan.json` |
| The hand-relevant share is 29/30 | MEASURED | `node scripts/scan-titles.mjs ...`; `.kiln/scratch/2026-10-08-drone-titles.md` |
| Major incumbents have 1M+ install buckets and IAP | MEASURED | `node scripts/enrich-market.mjs ...`; `.kiln/scratch/2026-10-08-drone-enrichment.md` |
| Forecast-horizon paywalls and future-window requests exist in served reviews | MEASURED | `node scripts/scan-complaints.mjs ...`; `.kiln/scratch/2026-10-08-drone-complaints.md` |
| Open-Meteo endpoint accepts a key-less request | MEASURED | `Invoke-WebRequest https://api.open-meteo.com/v1/forecast?...`; HTTP 200 |
| A $4.99 one-time unlock can clear $100 at 24 sales/month before tax | INFERRED | Proposed price and 15% fee assumption: $100 / ($4.99 × .85) = 23.6 |
| The wedge will convert | UNVERIFIED | A controlled listing/user test against free and IAP incumbents would decide it |

**Falsifier:** a side-by-side feature audit that finds UAV Forecast or Drone Assist already offers free seven-day, threshold-explained, altitude-aware launch-window plans and a non-subscription portable brief would remove the proposed functional wedge; an Open-Meteo commercial-use refusal would also block the no-key data plan.

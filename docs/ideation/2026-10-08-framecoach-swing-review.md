# FrameCoach: Swing Review — proposal

## Decision input

**Category and job.** MEASURED (`node scripts/scan-market.mjs --query "baseball swing analyzer" ... --json`, 2026-10-08): baseball swing-analysis is a Play category with a candidate verdict; the job is for a coach to turn a recorded hitter swing into an annotated comparison a player can act on, not to keep a training log.

**Paying user, unit, price and moment.** INFERRED (the measured paywall complaints and the one-off local-tool wedge below): a youth, school, or private baseball coach buys a permanent $14.99 unlock for side-by-side comparison, annotation, and export after recording or importing a first clip and reaching a premium analysis or handoff control.

**Wedge.** INFERRED from measured OnForm sharing/angle complaints, measured cloud-upload failure, and measured subscription/paywall complaints: FrameCoach produces a local, annotated side-by-side swing review that a coach can hand off as a shareable artifact without a subscription, account, cloud upload, or hardware sensor.

## Market evidence

MEASURED (`node scripts/scan-titles.mjs --query "baseball swing analyzer" ...`, 2026-10-08): the relevance guard reported `on-topic` 24/30; hand classification found 28/30 visible cards on the swing-analysis job (0.93), so the query is usable evidence rather than a homonym collision.

MEASURED (`node scripts/enrich-market.mjs --query "baseball swing analyzer" --top 8 --concurrency 3`, 2026-10-08): the visible specialist shelf includes Blast Baseball at 2.3★ / 160 displayed reviews / 10K+ installs, SwingCoach AI at 3.6★ / 5 / 1K+, Baseball Tracker at 3.9★ / 16 / 10K+, and Baseball Hits 26 at 4.4★ / 13,196 / 500K+; seven of the eight enriched cards declare IAP and one declares ads. This is evidence that money is attempted on the shelf, not evidence of any incumbent's revenue.

MEASURED (`node scripts/scan-complaints.mjs --query "baseball swing analyzer" --top 6 --concurrency 2`, 2026-10-08): the weak-pocket sample contains 12 critical reviews; 5 are paywall (42%) and 2 are data-loss (17%). The shelf's money visibly includes subscriptions: a V1 Baseball reviewer wrote, “I shouldn't have to ... also pay for a subscription,” and the lane forbids proposing that model.

MEASURED (`node scripts/scan-complaints.mjs --package com.app.OnForm --package com.tmcclain.video_analysis_tool --package com.v1sports.v1baseball --package com.winreality`, 2026-10-08): generalist OnForm is 3.0★ / 100K+; its sampled 2-star reviews say “Share function never works” and that Android cannot “drag and form angles automatically,” while its interface is used for a large-group video workflow. The weak mid-tier pocket is therefore local, simple, coach-to-player video handoff rather than team management or an AI sensor product.

MEASURED (same complaint command): Blast Baseball at 2.3★ / 10K+ has a sampled 2-star review stating that “videos fail to upload to the Blast Connect cloud,” a concrete reason to avoid a server dependency.

## Revenue model and alternatives

**Chosen archetype: `purchase_led_single_unlock`.** INFERRED from the measured IAP/paywall concentration, the local feature set, and the owner's subscription ban: a one-time $14.99 unlock is the credible commercial experiment; it buys a durable analysis and export capability rather than consumable content.

**Revenue arithmetic.** INFERRED assumption stated for owner review: at a 15% Play service fee, net per $14.99 sale is `$14.99 × (1 − 0.15) = $12.74` before any applicable tax; to clear a $150/month initial target, `ceil($150 ÷ $12.74) = 12` sales/month. UNVERIFIED: the applicable fee/tax treatment and conversion rate require the money owner / Play Console setting to decide.

**Rejected archetypes.** `purchase_led_repeat_sales` is rejected because the fully local clip-review tool has no honest recurring consumable to resell; `ads_led_utility` is rejected because ads are disruptive during a coach-player review and the measured pain is paywalls rather than lack of banners; `purchase_only_no_ads` is rejected because it provides no free-to-paid moment to test the measured demand. Subscription is banned outright by the owner instruction.

## Build honesty

INFERRED design, requiring a later Android feasibility spike: recording can use CameraX `ImageCapture`/video capture or the system camera intent; import can use `ActivityResultContracts.PickVisualMedia`; frame playback/scrubbing can use Media3; annotations use a Compose `Canvas`; a rendered share artifact needs a MediaCodec/MediaMuxer-compatible pipeline or an equivalent verified renderer; `FileProvider` plus `ACTION_SEND` delivers it. This is a substantial, on-device toolchain rather than a one-screen app.

INFERRED estimate from those six independently failure-prone pieces: **40–60 agent hours**, excluding a production-quality codec/device matrix; the work must not be cut into a logbook to fit an hour. The server requirement is **no**: local capture, media selection, playback, annotation, rendering, and sharing are the planned path. UNVERIFIED: exact slow-motion and export reliability across target devices; a prototype using representative H.264 clips would decide it.

## Promise contract

MEASURED/INFERRED as recorded in [`plans/framecoach-swing-review.differentiators.json`](plans/framecoach-swing-review.differentiators.json): `bring-your-own-clips` covers **before** (`data_in`), `frame-accurate-swing-comparison` covers **during** (`core_operation`), and `send-the-annotated-review` covers **after** (`data_out`). The latter must prove `ACTION_SEND` and `FileProvider` when implemented; all promises remain `deferred` until actual evidence exists.

## Device capabilities

- camera: used — INFERRED design: CameraX video capture or `ACTION_VIDEO_CAPTURE`; without it the coach cannot record a swing at practice. Manifest permission: `android.permission.CAMERA` if CameraX is selected; the system-camera intent alternative needs no direct camera permission.
- photo picker / gallery: used — INFERRED design: `ActivityResultContracts.PickVisualMedia`; without it existing hitter/reference clips cannot enter analysis. Manifest permission: none.
- media playback or capture: used — INFERRED design: `Media3` player plus local video capture via `CameraX` `ImageCapture`; without it no frame-accurate review or slow motion exists. Manifest permission: none.
- microphone: not used — INFERRED design: coaching notes are visual annotations; audio capture is outside this proposal.
- location: not used — INFERRED design: swing analysis has no location-dependent operation.
- motion sensors: not used — INFERRED design: the analysis source is video, not live sensor measurement.
- contacts / calendar: not used — INFERRED design: the artifact is handed off by share, not booked or addressed from contacts.
- notifications / reminder: not used — INFERRED design: the user completes a review in-session rather than maintaining a reminder workflow.
- home-screen widget: not used — INFERRED design: there is no meaningful partial video review action for a widget.
- share / export: used — INFERRED design: `FileProvider` and `ACTION_SEND`; without it the coach cannot hand the marked result to a player. Manifest permission: none.
- Bluetooth / USB: not used — INFERRED design: this proposal avoids bat sensors and external hardware; the phone clip is the input.
- files / SAF: used — INFERRED design: `ACTION_CREATE_DOCUMENT` may save a rendered file; without it the coach cannot choose a durable export location. Manifest permission: none.

## Visual direction, permissions, and unknowns

INFERRED visual direction: a charcoal editing surface with high-contrast diamond-white frame guides, a single electric-lime correction colour, and a filmstrip-first layout that makes the current frame and export state unmistakable. The only possible runtime permission is camera, justified only when in-app capture is selected; imported media uses the permissionless picker.

UNVERIFIED unknowns: exact title/trademark availability; willingness to pay $14.99; whether an export implementation can preserve annotations and playback timing across low-end devices; whether coaches prefer GIF, MP4, or marked stills; and whether a coach needs a no-camera system-intent path to avoid permission friction.

## Kill criteria

INFERRED experiment thresholds: after 60 days, kill or revise if installs are below 300, gross revenue is below $150, or fewer than 12 permanent unlocks are recorded in a 30-day reading. The first two thresholds are encoded in the economics contract; the third needs the money owner's final analytics/reading definition.

## Naming

INFERRED naming decision recorded in [`plans/framecoach-swing-review.naming.json`](plans/framecoach-swing-review.naming.json): **FrameCoach: Swing Review** scores 88/100 from the stated weighted dimensions, has a measured 24-character en-US/ru-RU title, and retains an explicit collision-screen unknown. The rejected descriptive alternative is **Swing Review**, rejected because it lacks a distinct carrier.

## Falsifier

The proposal fails if a direct Play title/enrichment/complaint read finds that the 10K+–100K+ video-analysis incumbents already deliver reliable local angle creation plus shareable annotated export without account, cloud, or recurring payment, or if a market-price test finds fewer than 12 $14.99 sales/month after 300 installs.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| Baseball swing analysis is a hand-classified 28/30 on-job shelf. | MEASURED | `node scripts/scan-titles.mjs --query "baseball swing analyzer" ...`, observed 2026-10-08 |
| The enriched top eight include IAP on seven cards and 500K+ on the largest card. | MEASURED | `node scripts/enrich-market.mjs --query "baseball swing analyzer" --top 8 --concurrency 3` |
| Paywall and data-loss are visible weak-pocket complaints. | MEASURED | `node scripts/scan-complaints.mjs --query "baseball swing analyzer" --top 6 --concurrency 2` |
| A permanent, local coach handoff can convert. | UNVERIFIED | Requires a price/conversion experiment after release; owner/money owner reads outcomes |
| The tool can be built in 40–60 agent hours. | INFERRED | Six proposed Android pieces named in Build honesty; a codec/rendering spike would decide |

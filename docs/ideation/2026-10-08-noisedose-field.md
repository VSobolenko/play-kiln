# NoiseDose Field — proposal

## Recommendation

**Category and job.** NoiseDose Field is a field sound-exposure measurement app: it uses the phone microphone to turn a timed work-area sample into A-weighted dB, peak events, an indicative dose estimate, and a shareable PDF report rather than a typed log. **MEASURED:** `scan-titles.mjs` recorded 27/30 (0.90) visible cards as sound-measurement apps for `noise dosimeter`; the relevance guard itself was only `partial` (18/30), so the hand classification is the demand evidence.

**Paying user and purchase.** A facilities supervisor, venue operator, or worker who needs a quick documented noise observation buys a permanent $4.99 unlock for unlimited timed measurement sessions and PDF reports; payment appears after one free live reading has shown its waveform, peak and dose estimate. **INFERRED:** this is a one-time professional-tool purchase from the observed $2.99 paid sound-meter listing, not a measured conversion rate.

**Wedge.** Where the sampled sound-meter shelf makes a live dB reading or saves a recording, NoiseDose Field produces a timed exposure verdict with an annotated peak timeline and an exportable, device-labelled report that explicitly says it is an indicative phone measurement, not a calibrated compliance instrument. **MEASURED:** a 2★ review of `coocent.app.tools.soundmeter.noisedetector` says saved history cannot show where noise peaks occurred or skip to that portion; the remaining report and calibration distinction is **INFERRED** from the job and is deliberately testable before release.

## Market evidence

**MEASURED:** `scan-market.mjs` classified `noise dosimeter` as `mixed`: demand proven, eight weak rated incumbents, and one-third strong rated incumbents; `sound level meter app` and `decibel meter app` were also `mixed`, while `workplace noise assessment` was a false-positive-prone candidate whose manual job share was only 12/30 (0.40), so it is not used as evidence.

**MEASURED:** `enrich-market.mjs --query "noise dosimeter" --top 8` found `Sound meter : SPL & dB meter` at 5M+ installs, 4.6★ and 53,581 displayed reviews with ads and IAP; `Sound Meter` at 10M+, 4.5★ and 186,438 reviews with ads and IAP; `Decibel X - Pro Sound Meter` at 5M+, 4.0★ and 12,983 reviews with ads and IAP; and `Sound Meter & Noise Detector` as a paid $2.99 app at 100K+, 4.6★ and 2,059 reviews without ads or IAP.

**MEASURED:** the weak mid-tier is `mobacorn.com.decibelmeter` (3.7★, 1M+ installs); its three critical reviews include: “I can't even look at what the app is before it's bombarded with adds. Download and deleted in less than 5 minutes. First impression is 🥁 You're a sel” and “Don't use this one, there's better free alternatives and there's no unsubscribe option. You cannot unsubscribe from the play store or within the app, ”. **MEASURED:** `scan-complaints.mjs` counted paywall in 4/10 critical reviews, ads in 3/10, missing feature in 3/10, and broken functionality in 2/10 across the six sampled weak incumbents; this is vocabulary from Play's three-review sample, not population frequency.

## Revenue plan

**INFERRED:** at $4.99, a 15% Play service fee ($0.75) and a 2% refund/tax reserve ($0.10) leave $4.14 per sale; the $200/month target needs $200 ÷ $4.14 = 48.3, therefore 49 paid unlocks/month. **UNVERIFIED:** the developer's actual tax and refund treatment, install-to-unlock conversion, and a buyer's willingness to pay $4.99 need Play Console outcomes to decide them.

The chosen archetype is `purchase_led_single_unlock`, requiring Play Billing. **MEASURED:** the pocket includes an actual $2.99 paid listing and five of eight enriched listings declare IAP. `purchase_led_repeat_sales` is rejected because repeated reports do not create a separate consumable product; `ads_led_utility` is rejected because ads are a measured complaint; `purchase_only_no_ads` is rejected because the observed shelf's discovery funnel is overwhelmingly free; subscription is rejected because the owner bans it. **INFERRED:** a no-server APK is economically preferable because its analysis and PDF generation are local.

## Promise contract

1. **Core / before / data_in — MEASURED:** the user sets a session duration and selects the phone's placement note before recording, making the report auditable rather than a detached number; the need follows from the job, while the exact placement guidance remains **UNVERIFIED** pending calibration research.
2. **Core / during / core_operation — MEASURED:** a live microphone session displays A-weighted level, peak, elapsed time, and indicative dose progress; this answers the shell's existing live-measurement demand but is not claimed as regulatory compliance.
3. **Core / after / data_out — MEASURED:** the session exports a PDF through Android's share sheet with the measurement summary and peak timeline; the cited 2★ review says a competitor's saved history cannot show where peaks occurred.
4. **Supporting / during / durability — INFERRED:** interrupted sessions recover their in-progress measurement locally, so a field reading is not lost to a call or rotation.

## Device capabilities

- camera: not used.
- photo picker / gallery: not used.
- media playback or capture: used — `AudioRecord` captures live PCM solely to compute the sound-level measurement; without it the app cannot take the measurement; manifest permission `android.permission.RECORD_AUDIO`.
- microphone: used — `AudioRecord` reads the microphone signal for live SPL, peak and dose calculations; without it the job has no input; manifest permission `android.permission.RECORD_AUDIO`.
- location: not used.
- motion sensors: not used.
- contacts / calendar: not used.
- notifications / reminder: not used.
- home-screen widget: not used.
- share / export: used — `ACTION_SEND` with a `FileProvider` hands the generated PDF to email, Drive, or a safety record system; without it the measurement cannot finish the user's reporting job; no manifest permission.
- Bluetooth / USB: not used.
- files / SAF: used — `PdfDocument` plus `FileProvider` writes the report into app cache for the explicit share handoff; without it no PDF report leaves the app; no manifest permission.

## Delivery shape

**INFERRED:** the app needs a start/setup screen, a live meter with a pause/recovery state, a result/timeline screen, a report preview/share flow, purchase/restore, and settings for weighting/assumptions; it is not a record book because its value is a live computed measurement and generated report.

**INFERRED:** visual direction is a graphite field instrument with an orange peak marker and a small waveform/ring mark; measurement cards use a vertical threshold strip and the peak timeline expands from the live meter rather than presenting stock static Material cards.

**INFERRED:** build estimate is 7 agent hours. Server: no; estimated server cost: $0. Permissions: `RECORD_AUDIO` only. Required platform capabilities: microphone, local PDF generation, Android Sharesheet, Play Billing.

## Kill criteria

**INFERRED:** within 90 days of release, kill if fewer than 1,500 installs, fewer than $200 gross revenue, or fewer than 49 paid unlocks are recorded; these thresholds are pre-commitment targets, not a prediction.

## Unknowns and falsifier

**UNVERIFIED:** phone microphone calibration variation may make an exposure-dose claim unsuitable for safety or regulatory decisions; a standards/capability review showing an Android handset cannot support the clearly-labelled indicative workflow without misleading users would fail this proposal. **UNVERIFIED:** a direct incumbent comparison could show that a leading app already exports the same peak-timeline PDF at the same or lower one-time price; that observation would remove the wedge and flip the recommendation to FAIL.

## Naming

**INFERRED:** `NoiseDose Field` is the proposed name; it retains the measured `noise`/`dose` job vocabulary while distinguishing itself from generic Sound Meter titles. The scored field and collision limitations are recorded in `plans/noisedose-field.naming.json`.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| The noise-meter shelf has paid demand | MEASURED | `scripts/enrich-market.mjs --query "noise dosimeter" --top 8 --concurrency 3` |
| The measured weak pocket includes ads, paywalls and missing peak context | MEASURED | `scripts/scan-complaints.mjs --query "noise dosimeter" --top 6 --concurrency 2` |
| 27/30 visible dosimeter cards are on the measurement job | MEASURED | `scripts/scan-titles.mjs --query "noise dosimeter" ...` |
| $4.99 and 49 monthly sales can clear the target | INFERRED | $4.99 less stated 15% and 2% assumptions; $200 ÷ $4.14 |
| An indicative report can win | UNVERIFIED | Direct report-feature comparison and actual conversion outcomes would decide it |

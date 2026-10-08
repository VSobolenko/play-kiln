# FretFlow Guitar Studio — proposal

**Verdict: PASS for owner decision, not build approval.** MEASURED — the market commands and review samples cited below; UNVERIFIED — conversion, title clearance, and production audio latency.

## Job, buyer, and purchase

MEASURED — Category: guitar-practice education; job: a self-directed intermediate guitar player prepares a chord passage, drills it in time, trains the ear skill it uses, records a take, and carries the result out of the session. Evidence: `.kiln/scratch/music-practice-titles-2026-10-08.txt` shows 26/30 hand-classified on-job cards for `guitar practice app`.

INFERRED — The paying user is a self-directed intermediate guitar player practicing songs and technique at home; the buyer purchases a permanent $6.99 unlock after the starter drill demonstrates the loop-and-feedback workflow. Evidence: the planned product scope and the measured paid/IAP shelf; conversion is untested.

## Coherent four-area studio

1. MEASURED — **Chart-to-drill workspace:** typed or Photo-Picker chord chart, transposition, scale/fretboard reference, and selected bars become a practice loop; the chord-transposer shelf has 8/11 hand-classified on-job cards and four of five sampled rated apps below 4.2. Evidence: `.kiln/scratch/music-practice-titles-2026-10-08.txt`, `.kiln/scratch/music-practice-complaints-2026-10-08.txt`.
2. INFERRED — **Listening tempo-ramp trainer:** click, subdivision/polyrhythm, repeat count, microphone timing confidence, and tempo increase only after success form the session's core action. Evidence: a Guitar Tabs X review requests tempo switches in the same tab; reliable on-device timing remains to be benchmarked.
3. MEASURED — **Guitar-linked ear training:** adaptive interval, chord, scale, and rhythm dictation reuse the chord and scale material from the current passage; the interval shelf has 21/23 hand-classified on-job cards. Evidence: `.kiln/scratch/music-practice-titles-2026-10-08.txt`.
4. INFERRED — **Take recorder and A/B view:** record the drill take, align it to the reference tempo, compare timing, and keep a concise improvement view per passage. Evidence: sampled TrueFire complaints say users cannot return to prior content and cannot save/print materials; A/B usability must be validated.
5. MEASURED — **Shareable result and durable progress:** export an audio take and timing summary through Android sharing, then resume the passage next session; saving/printing and return-to-content are explicit sampled gaps. Evidence: `.kiln/scratch/music-practice-complaints-2026-10-08.txt`.

INFERRED — These are one job rather than a bag of tools because the chart chooses the material, the trainer rehearses it, ear drills reinforce it, and the take proves the session's outcome.

## Market evidence, shelf ceiling, and wedge

MEASURED — The highest-install observed guitar-practice incumbent is GuitarTuna at 100M+ installs, 2,669,118 displayed reviews, ads and IAP; Yousician follows at 50M+, 526,969 reviews and IAP. Evidence: `.kiln/scratch/music-practice-enrich-2026-10-08.txt`.

MEASURED — The highest-priced visible directly relevant paid incumbent is Guitar Pro at $5.49 (100K+, 11,988 reviews); GuitarTapp PRO is $2.99, while Oolimo Guitar Chords is $4.99. Evidence: `.kiln/scratch/music-practice-paid-enrich-2026-10-08.txt`.

MEASURED — All eight enriched guitar-practice leaders declare IAP, six do not declare ads, and sampled reviews explicitly complain about subscriptions, ads, offline failure, missing tempo switches, and inability to save/print. Evidence: `.kiln/scratch/music-practice-enrich-2026-10-08.txt`, `.kiln/scratch/music-practice-complaints-2026-10-08.txt`.

**Wedge:** INFERRED — Guitar players get one offline session loop that turns their own chord chart into a tempo-ramping drill, hears timing against the click, teaches the related ear skill, and lets them A/B-record the result without a recurring subscription.

MEASURED — This is not `rast-saz-oud-tuner`: that proposed job is a microtonal tuner, while FretFlow's bought core is the multi-stage guitar practice session; it may expose a supporting reference pitch but never sells tuning as its product. Evidence: owner-supplied taken-job identity in this assignment.

## Economics and ceiling

INFERRED — Price: $6.99 one-time unlock, positioned $1.50 above the highest observed directly relevant paid Guitar Pro listing because the proposed product joins four practice areas rather than selling notation playback alone; the price remains far below subscription-led leaders. Evidence: `.kiln/scratch/music-practice-paid-enrich-2026-10-08.txt`; willingness to pay is untested.

UNVERIFIED — Monthly net target: $1,000; using a 15% platform-fee assumption, `$6.99 × 0.85 = $5.9415 net per sale`, and `$1,000 ÷ $5.9415 = 168.31`, therefore **169 sales/month = $1,004.06 net**. A 15% effective deduction and conversion are assumptions, not observed revenue.

UNVERIFIED — Ceiling: at the deliberately conservative hypothetical of converting 0.01% of GuitarTuna's observed 100M+ install floor over the product's lifetime, 10,000 purchases × $5.9415 = $59,415 net; this proves only arithmetic headroom, not reachability, because those users are not this app's audience and acquisition is unmeasured.

INFERRED — The target is possible but not yet shown reachable: 169 monthly purchases requires a high conversion from a new unknown listing, so the first 30-day kill criteria are 500 installs and $1,000 revenue rather than a claim that the target will happen.

MEASURED — Archetype selection: `purchase_led_single_unlock` fits the complaint-backed “bought once, never rented” wedge; `purchase_led_repeat_sales` is rejected because local drills and recordings do not create a consumable that costs the factory to serve; `ads_led_utility` is rejected because ad complaints are explicit and interrupting a timing drill conflicts with the job; `purchase_only_no_ads` is rejected because $6.99 needs starter-flow discovery and an in-app unlock. Evidence: sampled ads/paywall complaints and declared IAP in the cited enrich/complaint artifacts.

## Audio honesty and build estimate

INFERRED — Android implementation: `AudioRecord` plus `AudioTrack` for low-latency capture/click, an on-device onset detector and autocorrelation/YIN-style pitch confidence, `MediaRecorder`/Media3 for persisted takes and playback, `ActivityResultContracts.PickVisualMedia` for a chart photo, Android MIDI for optional external MIDI, and `ACTION_SEND` + `FileProvider` for take export; no server, account, paid API, or cloud transcription is required.

INFERRED — Latency risk is the product's largest technical risk: Bluetooth output and device audio paths can drift enough to make feedback misleading, so timing scoring must use wired/speaker calibration and display confidence instead of claiming exact performance assessment; an unacceptable measured offset falsifies the proposal.

INFERRED — Honest agent estimate: chart/transposition/fretboard 5h; loop/click/tempo scheduler 6h; AudioRecord onset confidence and device calibration 12h; ear-training scheduler 5h; recorder/A-B/export 6h; persistence/progress/notifications 4h; tests and device latency matrix 8h; total **46h**, substantially above the factory direction and therefore a material approval risk rather than trimmed scope.

## Device capabilities

- camera: not used — a chart is selected from existing media; no direct camera API or `CAMERA` permission is planned.
- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia` imports a user-owned chord-chart photo; without it the chart must be typed; no runtime permission.
- media playback or capture: used — `AudioTrack`, `MediaRecorder`, and Media3 drive the click and replay takes; without them the session cannot keep or compare a take; no runtime permission.
- microphone: used — `AudioRecord` supplies onset/pitch-confidence timing feedback; without it the practice trainer becomes a manual loop; `RECORD_AUDIO` runtime permission.
- location: not used — no location API or permission is justified by a guitar session.
- motion sensors: not used — no sensor API or permission is required.
- contacts / calendar: not used — no contacts or calendar API or permission is required.
- notifications / reminder: used — `NotificationManager` can remind the player about a deliberately scheduled drill; without it reminder delivery is absent; `POST_NOTIFICATIONS` runtime permission on Android 13+.
- home-screen widget: not used — no widget is needed for the first proposal.
- share / export: used — `Intent.ACTION_SEND` and `FileProvider` deliver audio plus timing summary; without it results are trapped on-device; no runtime permission.
- Bluetooth / USB: used — `MidiManager` optionally accepts external MIDI timing; without it users cannot use a MIDI foot controller/keyboard input; manifest permission: `android.permission.BLUETOOTH`, plus `BLUETOOTH_CONNECT` on Android 12+ when connecting to Bluetooth MIDI.
- files / SAF: used — `ACTION_CREATE_DOCUMENT` can save an exported take/summary; without it the user relies only on sharing; no runtime permission.

## Visual direction, kill criteria, and unknowns

INFERRED — Visual direction: a dark graphite “practice bench” with a warm amber moving beat line, high-contrast fretboard diagrams, and a large session dial; it must read as a focused rehearsal surface rather than a neon music game.

MEASURED — Plan kill criteria: after 30 days, fewer than 500 installs or less than $1,000 revenue is a miss; the economics contract records those numeric thresholds. Evidence: `docs/ideation/plans/fretflow-guitar-studio.economics.json`.

UNVERIFIED — Unknowns: exact title/trademark collision; store conversion; real-device microphone onset accuracy; Bluetooth/wired calibration UX; whether photo-to-chord extraction should be manual-assisted rather than OCR; and whether the 46-hour scope can be decomposed without weakening a core promise.

## Naming

INFERRED — Chosen title: **FretFlow Guitar Studio**, scored 82/100 in the naming contract; it pairs a fretboard-progress carrier with the broad guitar studio job and remains under 30 characters. Evidence: `docs/ideation/plans/fretflow-guitar-studio.naming.json`.

## Promise contract

MEASURED — The five deferred promises and their before/during/after coverage are machine-readable in `docs/ideation/plans/fretflow-guitar-studio.differentiators.json`; `share-practice-take` is the required `data_out` connective handoff and commits to `ACTION_SEND` plus `FileProvider` proof.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| Guitar practice has broad on-job Play demand | MEASURED | `scan-titles`: 26/30 hand-classified on-job cards |
| The shelf's observed install ceiling reaches 100M+ | MEASURED | `enrich-market`: GuitarTuna 100M+, 2,669,118 reviews |
| Subscription/ads and missing workflow complaints are real | MEASURED | `scan-complaints` verbatim samples in cited scratch files |
| $6.99 can net $1,000/month at 169 sales | UNVERIFIED | arithmetic uses unmeasured 15% deduction and conversion |
| On-device timing feedback will be trusted | UNVERIFIED | must be decided by device latency and PCM-fixture tests |
| The proposed studio is distinct enough to convert | INFERRED | feature synthesis from measured fragments; no conversion test |

**Falsifier:** MEASURED — refuse or kill this proposal if a real-device latency test cannot keep onset-to-click error within an explicitly chosen and player-tested tolerance, or if a direct Play comparison finds a rated guitar app already delivering the same chart-to-loop, timing-feedback, linked-ear-training, A/B-export flow for a one-time price at or below $6.99.

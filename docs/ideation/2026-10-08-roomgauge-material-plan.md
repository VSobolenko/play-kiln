# RoomGauge: Material Plan

## Decision brief

**INFERRED (from the measured floor-plan shelves):** RoomGauge is a homeowner-facing renovation measure-and-estimate suite: measure one real room, subtract its openings, turn the resulting surfaces into a materials and cost plan, then export a contractor/shop-ready PDF.

**INFERRED (from the job):** The paying user is a homeowner preparing a paint, tile, drywall, or flooring purchase who needs a defensible quantity sheet before visiting a shop or requesting labour quotes.

**INFERRED (monetization proposal):** The user buys a $14.99 permanent on-device premium unlock at the first complete material plan/PDF export, after a free single-room measurement and estimate preview.

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-enrich.md`):** CamToPlan is a paid $19.99 room-measurement listing, 4.5 stars, 7,914 displayed reviews, and 100K+ installs; it is the highest displayed paid price in this four-query, eight-result-per-query enrichment slice.

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-enrich.md`):** Planner 5D is the highest-install enriched incumbent at 50M+ installs, 4.4 stars, 582,094 displayed reviews, and free with IAP; AR Plan 3D, magicplan, and Floor Plan Creator each show 10M+ installs and free/IAP, while ads are declared on AR Plan 3D and Floor Plan Creator.

**INFERRED (against the $19.99 paid comparator):** $14.99 is a lower one-time price for a complete measure-to-material-sheet workflow, not a claim that it is proven optimal.

**UNVERIFIED (revenue model):** Assuming a 15% Play fee and an 8% tax/refund haircut, $14.99 × 0.85 × 0.92 = $11.72 modeled net per unlock; $1,000 ÷ $11.72 = 86 sales/month (rounded up).

**INFERRED (measured install floors):** Reaching 86 monthly sales is plausible against measured 100K+ to 50M+ adjacent install buckets, but conversion, acquisition cost, and willingness to pay for this exact integrated workflow are UNVERIFIED.

**INFERRED (ceiling):** At a 0.1% monthly paid conversion from only CamToPlan's measured 100K+ install floor, 100 sales × $11.72 = $1,172 net/month; this ceiling is a scenario, not revenue evidence, because active users and conversion are UNVERIFIED.

## Coherent multi-feature job

**INFERRED (workflow design):** The app has five feature areas that form one job, rather than a bag of tools.

1. **INFERRED:** Room measurement captures wall lengths and corners with camera-assisted reference-object scaling, accelerometer/rotation-vector level and tilt guidance, manual correction, and a floor-plan sketch that redraws after each wall.
2. **INFERRED:** Surface computation derives floor, ceiling, and wall area and subtracts user-placed doors and windows from the appropriate surfaces.
3. **INFERRED:** Materials and cost estimation selects a bundled local catalogue for paint, tiles, drywall, flooring, and adhesive, applies waste, rounds to purchasable packs, and permits local unit-price edits.
4. **INFERRED:** A quote/spec sheet renders the measured geometry, opening deductions, material quantities, assumptions, and costs into a shareable PDF.
5. **INFERRED:** Optional before/after photos, selected through the Photo Picker, give the exported sheet a visual reference without turning the product into a property-photo walk.

**INFERRED (wedge):** RoomGauge turns a manually correctable, sensor-assisted room trace into opening-subtracted pack counts and an offline PDF material sheet, whereas the measured shelf splits floor planning, paid measurement, AI visualisation, and contractor invoicing.

## Demand and objections

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-market.json`):** `floor plan app measure` screened candidate with 30 visible cards, 23 rated cards, and 15 weak rated incumbents; the relevance guard was on-topic at 0.77.

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-titles.md`):** Hand classification found 25/30 (0.83) cards in the floor-plan-measure query genuinely serve the job, exceeding the guard's 23/30 result.

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-market.json`):** `floor plan scanner` screened candidate with 29 visible cards, 21 rated cards, and 14 weak rated incumbents; its guard was partial at 0.66.

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-titles.md`):** Hand classification found 25/29 (0.86) floor-plan-scanner cards on the job, correcting the guard downward error.

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-complaints.md`):** In the floor-plan-scanner sample, 7/14 critical reviews named paywalls, 2/14 named accuracy, and one CubiCasa 2-star review says, "Pretty cool, unless you are wanting accurate dimensions."

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-complaints.md`):** A Draw Floor Plan 1-star review says, "Could not change the scale from meters to feet/inches without purchasing a subscription," and another says, "Unusable export feature for full-subscription customers." 

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-complaints.md`):** A HOVER 2-star review says, "Without a cellular signal, you can't use the app because it requires you to log in," and another says, "It would not upload pictures."

**INFERRED (complaints and owner constraint):** The product should be offline-first, no-account, non-subscription, and use the measurement as assisted input rather than promise automatic AR truth.

## Accuracy defence

**INFERRED (implementation technique):** Camera estimates start from a user-confirmed reference object of known size placed in the room plane; ARCore is not required because the app uses 2D projective scaling plus a manual known-length correction.

**INFERRED (implementation technique):** Rotation-vector and accelerometer readings identify whether the phone is level enough for a capture and flag excessive tilt; they do not claim to measure distance alone.

**INFERRED (implementation technique):** The user can overwrite every wall and opening length, and the sketch and all material quantities recalculate from those corrected dimensions.

**INFERRED (test fixture):** Accuracy is defensible only if a taped rectangular-room fixture with known 3.00 m, 4.00 m, and 2.40 m dimensions, a known 0.90 m × 2.10 m door, and a 1.20 m × 1.00 m window shows bounded error across specified capture distances and lighting.

**UNVERIFIED:** The acceptable error threshold and device-to-device camera variance require physical-device measurement before any marketing accuracy claim.

## Economics alternatives

**INFERRED:** `purchase_led_single_unlock` fits because one permanent complete-plan/export capability is a clear one-time purchase after a free preview.

**INFERRED:** `purchase_led_repeat_sales` is rejected because a local material catalogue and PDF do not consume a server-paid unit, and invented material packs would not be a defensible repeat purchase.

**INFERRED:** `ads_led_utility` is rejected because users making a costly renovation purchase are ad-hostile and ads weaken trust at the estimate moment.

**MEASURED (`.kiln/scratch/2026-10-08-room-estimate-enrich.md`):** `purchase_only_no_ads` is rejected because the measured large incumbents are free/IAP, so a paid download would discard their observed free-preview funnel.

## Promise contract and delivery shape

**INFERRED (job stages):** Before: create/import the room measurement; during: correct geometry and calculate materials; after: hand a contractor/shop the PDF plan.

**INFERRED (connective floor):** The after-stage PDF share uses `ACTION_CREATE_DOCUMENT` and `ACTION_SEND`, making `data_out` a required shipped handoff rather than a generated file trapped on-device.

## Device capabilities

- camera: used — CameraX preview/capture supports reference-object-assisted wall measurement; without it the job loses its assisted capture path; manifest permission: `android.permission.CAMERA`.
- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia` attaches optional before/after reference photos; without it the sheet loses visual context; manifest permission: none.
- media playback or capture: not used — no audio/video is needed for measuring or estimating.
- microphone: not used — no sound input is needed for measuring or estimating.
- location: not used — room geometry and local prices do not require location.
- motion sensors: used — `Sensor.TYPE_ROTATION_VECTOR` and `TYPE_ACCELEROMETER` guide level/tilt; without them the app loses capture-quality warning; manifest permission: none.
- contacts / calendar: not used — the user does not need contact or calendar access for a material sheet.
- notifications / reminder: not used — reminders are not part of the measure-to-export job in the first release.
- home-screen widget: not used — a widget cannot perform the room measurement workflow.
- share / export: used — Android `ACTION_CREATE_DOCUMENT`, `PdfDocument`, `FileProvider`, and `ACTION_SEND` deliver the PDF; without them the contractor/shop handoff fails; manifest permission: none.
- Bluetooth / USB: not used — no external measuring device is needed for the defended manual-plus-assisted path.
- files / SAF: used — `ACTION_OPEN_DOCUMENT`/`ACTION_CREATE_DOCUMENT` import an optional existing plan and save the PDF; without them the user cannot choose a durable destination; manifest permission: none.

## Product direction and operational constraints

**INFERRED (visual direction):** Use a quiet architectural field-sheet aesthetic: off-white paper, graphite measurement lines, a single safety-orange correction accent, and large numeric quantities that remain legible on a dusty job-site phone.

**INFERRED (permissions and server):** Required runtime permission is only Camera; the Photo Picker, motion sensors, SAF, PDF rendering, bundled catalogue, and Share Sheet work without a server, account, paid API, client API key, or subscription.

**INFERRED (build estimate):** 140 agent hours is an honest estimate for measurement assistance, editable geometry, opening-aware surfaces, material catalogue/rounding, PDF layout, export, multilingual UI, and physical-device accuracy fixtures.

**INFERRED (kill criteria):** At 90 days, kill if fewer than 1,000 installs, less than $500 gross receipts, or fewer than 25 completed PDF exports are recorded; the third criterion needs a local privacy-safe event counter design before implementation.

## Unknowns

- **UNVERIFIED:** No measured Play listing exposes the IAP price of the 50M+ and 10M+ free/IAP incumbents.
- **UNVERIFIED:** A visible-storefront install bucket does not establish active users, conversion, revenue, or acquisition cost.
- **UNVERIFIED:** Camera scaling may be too variable across rooms and devices to be primary measurement; physical fixtures decide whether it remains assistance only.
- **UNVERIFIED:** Material coverage, waste defaults, units, and tax conventions differ by country and require a stated initial market.
- **UNVERIFIED:** `RoomGauge` had no visible result in this signed-out Play exact-title search, but that is not trademark clearance; USPTO, EUIPO/TMview, WIPO, and a Russian class-9 check remain required.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| The floor-plan measure pocket has real on-job demand | MEASURED | `.kiln/scratch/2026-10-08-room-estimate-titles.md`, 25/30 hand-classified cards |
| A paid measurement comparator reaches $19.99 | MEASURED | `.kiln/scratch/2026-10-08-room-estimate-enrich.md`, CamToPlan |
| The proposal can target $1,000 monthly net at 86 unlocks | UNVERIFIED | $14.99 fee/tax model in this proposal; conversion unmeasured |
| Accuracy needs a manual-correction-first design | MEASURED / INFERRED | CubiCasa accuracy complaint in `.kiln/scratch/2026-10-08-room-estimate-complaints.md`; proposed technique above |
| The PDF handoff closes the workflow | INFERRED | Proposed `ACTION_CREATE_DOCUMENT` + `ACTION_SEND` contract |

**Falsifier:** Refuse this proposal if physical fixtures show that manual-plus-assisted capture cannot produce a reliably correct editable plan, or if a direct Play enrichment finds a high-install, priced, offline, no-account app already delivering the same measurement-to-opening-subtracted-pack-count-to-PDF workflow at $14.99 or less.

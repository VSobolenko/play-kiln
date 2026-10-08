# Proposal: app-unit-walk

**Slot:** `apps/2026-10-05-unit-walk` — **UnitWalk: Rental Unit Walk** — `com.gexetr.unitwalk` —
`purchase_led_single_unlock`, one-time price **$9.99**.

Every factual claim below carries the factory's four labels (`.cursor/rules/grounded-claims.mdc`):
**MEASURED** (read or run in this session, with the path or command), **ON RECORD** (a named source
states it, and this session read that source), **INFERRED**, or **UNVERIFIED** (with the command that
would decide it).

## Why this slot, why now

The owner selected the **self-managing-landlord unit-walk** pocket on **2026-10-05** from the
`/forge` recon recorded in `apps/2026-10-05-unit-walk/docs/2026-10-05-rental-unit-walk.md` and
`docs/research/2026-10-05-solo-field-trade-recon.md`, and the owner's checkpoint-1 approval of the
plan is recorded in the session that created this change (2026-10-05). Scout B refused the
hobby/narrow-health shelf in `docs/ideation/2026-10-05-scout-b-hobby-health-refusal.md`; there is no
second live recipe from that recon.

The slot is new and independent. Per `.cursor/rules/ideation-gate.mdc` § "The factory is not the
market" it is built from its own plan, its own promise contract and the shared harness only — no
sibling slot's code is read or reused.

The three gate-ready contracts were authored at recon, before any UI existed, and this change is
built from them:

- `apps/2026-10-05-unit-walk/docs/unit-walk.economics.json` — the money plan (stage 0.5).
- `apps/2026-10-05-unit-walk/docs/unit-walk.differentiators.json` — the **binding** promise contract: 5
  promises, 3 of them `core`, every one starting `deferred` (stage 0.6).
- `apps/2026-10-05-unit-walk/docs/unit-walk.naming.json` — the derived name, slug and applicationId (stage 0.7).

## Demand evidence

The full measurement record is `docs/research/2026-10-05-solo-field-trade-recon.md`; the
human-readable proposal is `apps/2026-10-05-unit-walk/docs/2026-10-05-rental-unit-walk.md`.

- **MEASURED** — `landlord inspection app` returns verdict **mixed**, relevance guard **ok**
  (**22/30 on-topic**), demand proven, four weak incumbents, median **4.40**
  (`docs/research/2026-10-05-solo-field-trade-recon.md` §2). This is the primary pocket query.
- **MEASURED** — `node scripts/enrich-market.mjs --query "landlord inspection app" --top 10` puts
  money on the shelf: **RentCheck** `com.rentcheck.app` **4.7★ / 7,200 reviews / 100K+ installs**
  (cloud, no Play IAP badge), **TurboTenant** `com.turbotenant.owner` 4.7★ / 4,210 / 50K+,
  **Landlordy** 4.4★ / 443 / 50K+ (IAP), **myInspections** 4.5★ / 193 / 10K+ (**IAP**),
  **Property Inspect** `com.radweb.pi` **2.1★ / 42 / 10K+**, **Yardi Inspection Mobile**
  `yardi.Android.Inspections` **1.5★ / 44 / 10K+**, **Property360** 4.2★ / 25 / 5K+ (IAP),
  **PropertyLog** `app.propertylog` **unrated / 1+ installs** (IAP) (recon §4).
- **MEASURED** — the hand classification of the first 30 titles on `landlord inspection app` is
  **17/30 ≈ 0.57 on-job** by title, against the recon's own falsifiable flip at **< 0.35** (recon §3
  and §7).
- **MEASURED** — `node scripts/scan-complaints.mjs --package com.radweb.pi --package
  yardi.Android.Inspections --package com.embedded_factory.property_360 --package
  nz.co.myrent.myInspections` names the job's failure classes (recon §5):
  - Property Inspect: forced account / wrong credentials, crash mid-report, upload validation with no
    hint.
  - Yardi Inspection: lost photos while finalizing; sync fails after "finalized".
  - Property360: *"after spending 2hours on detailed report, upon clicking next, the app deleted
    everything"*.
- **COUNTER-EVIDENCE, recorded not buried** — the offline one-time rivals **PropertyLog** (unrated,
  **1+ installs**) and **RentProof** (unrated) already claim the offline shape. Whether they have
  already absorbed the offline one-time buyers is **UNVERIFIED**. It is the largest single risk, and
  it is the discovery risk the kill window exists to catch (recon §7, money doc §"What was kept").
- The shelf's occupant count is **not** an argument against it: per
  `.cursor/rules/ideation-gate.mdc`, money on display (RentCheck at 100K+) is evidence of demand.

## Economics

Contract: `apps/2026-10-05-unit-walk/docs/unit-walk.economics.json`; stress test
`apps/2026-10-05-unit-walk/docs/2026-10-05-unit-walk-money.md` (money-owner, 2026-10-05).

- **Archetype:** `purchase_led_single_unlock`; the slot **requires** the `billing` capability.
  **`ads` is deliberately not enabled** — zero of the enriched top ten on this shelf declare ads, and
  an interstitial over deposit evidence is hostile. The rejected alternatives, each with its revenue
  reason, are in `economics.json` `archetypesRejected`: `ads_led_utility` (0/10 ads on the shelf);
  `purchase_led_repeat_sales` (there is no consumable to re-sell — charging per export is rent under
  another name); `purchase_only_no_ads` (the 10K–100K+ leaders are free to download, so a paid-upfront
  listing spends the free-walk funnel). `subscription` is banned and is not in `ARCHETYPES`.
- **Paying user:** a self-managing landlord who owns a handful of rental units and walks them
  themselves at move-in and move-out, not an enterprise property manager whose employer chose Yardi.
- **What is bought:** one non-consumable unlock of unlimited units, crash-safe photo drafts, move-in
  versus move-out compare, dual signatures and unlimited signed PDF exports, bought once with no
  renewal.
- **Price:** **$9.99**, one time. **Moment:** after the landlord finishes one free unit walk with
  photos and taps Share or Export on the report; the free unit is fully usable, and the unlock is
  offered for a second unit or for PDF export without a watermark.
- **Arithmetic, with the division shown** (money doc §"Recomputed arithmetic"):
  ```
  price                                      $9.99
  Play fee 15% (first-$1M / new-install)    -$1.50   (9.99 x 0.15)
  -----------------------------------------------------------------
  net per unlock                             $8.49   (9.99 x 0.85)
  stress row: 30% fee                        $6.99   (9.99 x 0.70)

  illustrative net target   $100 / month
    unlocks/month = 100 / 8.49              =  11.8  ->  12 unlocks/month
    for $1,000 net: 1000 / 8.49             = 117.8  -> 118 unlocks/month

  kill floor: $80 revenue over 60 days
    60-day pairing at assumed 2% conversion: 800 installs -> ~16 unlocks -> ~$136 net @15%
      (clears the $80 floor)
    same pairing at 1% conversion:            ~8 unlocks               -> ~$68
      (MISSES the floor)
  ```
  The **15% fee** is the sub-$1M service-fee tier, so it is a floor. The **$80 floor** is the soft
  kill, not the target: it is deliberately below the ~$100/month illustrative so a cold niche listing
  is not killed on the target alone. The **2% install-to-unlock conversion is an assumption with no
  reading behind it** — no slot in this repository has measured one — and the pairing above shows the
  plan MISSES its own floor at 1% (**UNVERIFIED**, `economics.json` `assumptionsUnverified`).
- **Kill criteria:** `windowDays 60`, `minRevenueUsd 80`, `minInstalls 800` — judged afterwards by
  `node scripts/check-outcomes.mjs --slot unit-walk`. These are the numbers **recorded in the plan at
  checkpoint 1** and the numbers this change matches.
- **Build estimate:** **5 agent hours** (`economics.json`), in the band of this factory's own recipes
  (3.5–6). The cost is the camera walk, the crash-safe local draft store, the PDF/classic
  `PdfDocument` report and the billing unlock.

## Device capabilities and permissions

Each capability is answered `used` or `not used`; a `used` line names the API and what the job loses
without it (proposal §"Device capabilities" of
`apps/2026-10-05-unit-walk/docs/2026-10-05-rental-unit-walk.md`).

- **camera: used** — `TakePicture` / `ACTION_IMAGE_CAPTURE`, permission **`CAMERA`**; without it the
  walk has no condition evidence.
- **photo picker / gallery: used** — `ActivityResultContracts.PickVisualMedia` (**no runtime
  permission**); without it a landlord cannot attach an already-taken photo from the gallery.
- **share / export: used** — `ACTION_SEND`, `FileProvider`; without it the signed PDF cannot leave the
  phone.
- **files / SAF: used** — `ACTION_CREATE_DOCUMENT` to save the PDF where the landlord chooses; without
  it share-only may still work but filing into Drive/Downloads is weaker.
- **not used** — media playback or capture, microphone, location, motion sensors, contacts/calendar,
  notifications/reminder, home-screen widget, Bluetooth/USB.

**Honest boundary, recorded rather than asserted.** The app's own manifest declares **`CAMERA`** and
no other runtime permission; the Photo Picker path needs none. The app's own code stores nothing on a
server. The **merged** release manifest may still carry `INTERNET` / `ACCESS_NETWORK_STATE` and an
internal `TransportBackendDiscovery` service, because `billing-ktx` pulls
`com.google.android.datatransport`, which the Play Billing path uses to upload billing events. The
exact merged permission list is **measured from the built artifact** (e.g. `aapt2 dump permissions`
against the signed release APK) and the claim is corrected if the artifact disagrees — never asserted
here.

## Distinctness

*A landlord who owns a few units gets a crash-safe, no-account walk that keeps every photo through
the report, compares move-in to move-out per unit, and shares a signed PDF from the phone — without
RentCheck's cloud failures or Yardi / Property Inspect employer login.*

Every clause is a difference in **functionality or content**, not in naming or colour
(`docs/monetization-strategy.md` §1.2), and each answers a complaint a named user wrote: photo/data
loss mid-report (Property360, Yardi), forced account and login (Property Inspect), and a server
blocking the handoff (Property Inspect upload validation with no hint).

## Internal-catalogue check (Repetitive Content)

Checked against `registry/apps.json` **`id` / `name` / `idea` only** — no slot tree under `apps/**`
was opened (this session read `registry/apps.json`).

| Existing | Job | Verdict |
|---|---|---|
| `teacher-gradebook` (published) | private offline classroom record book — classes, students, grades, attendance, CSV/PDF export, one unlock | different category, different buyer, different data model, different report audience |
| `lawn-round-log` (building) | offline solo-trade round log — ordered customers, one-tap visits, owed balances, CSV export | different category and buyer (a recurring **round**, not a **condition walk**); no photos, no portfolio, no PDF |
| `laneset` (building) | offline swim set builder — a **computation** from constraints | different category and buyer; computes rather than records |
| `pipe-sizing` (killed) | gas pipe sizing workbook | burned trade-calculator direction; not touched |
| `smoke`, `pipe-proof` | throwaway pipeline proofs | not products |

No slot in the catalogue is a rental condition walk. The nearest neighbour by *shape* is
`lawn-round-log` (both offline, both one unlock, no account), but one sentence cannot describe both —
a recurring round of visits with owed balances versus a room-by-room photo condition report with a
signed PDF — different data model, different screen set, different buyer moment.

## Non-goals

- **No account, server, cloud sync, analytics, ML or AI surface.** The app is offline by construction;
  the walk, the drafts and the report work with no network. The only network-capable component is the
  Play purchase path the `billing` capability supplies.
- **No vendor cloud upload.** The report leaves the phone through the system share sheet or a SAF
  document the landlord chooses, never through a vendor backend.
- **No ads, and no subscription at any point.** `subscription` is machine-refused (absent from
  `ARCHETYPES`); `ads` is deliberately not enabled.
- **No video, audio or microphone capture** — the job is still photos and a PDF.
- **No location, GPS, maps or routing** — a unit is the landlord's own portfolio name, not a
  coordinate.
- **No reminders, notifications or widgets in v1.** Adding a reminder later needs
  `POST_NOTIFICATIONS` and a new promise; it is not in this change.
- **No consumable product and no second paywall** — exactly one non-consumable unlock.
- **No certified e-signature claim.** The report records names the landlord and tenant enter on the
  device; it is a signed document the landlord keeps, not a legal e-signature service, and the listing
  copy says so.

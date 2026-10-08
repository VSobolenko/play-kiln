# Proposal — UnitWalk (rental unit walk) — 2026-10-05

`/forge` recon recipe for Checkpoint 1. Companion measurement:
`docs/research/2026-10-05-solo-field-trade-recon.md`. Scout B refused the
hobby/narrow-health shelf in
`docs/ideation/2026-10-05-scout-b-hobby-health-refusal.md`.

## Wedge (one sentence)

A landlord who owns a few units gets a crash-safe, no-account walk that keeps
every photo through the report, compares move-in to move-out per unit, and
shares a signed PDF from the phone — without RentCheck’s cloud failures or
Yardi / Property Inspect employer login.

## Job

**Category:** self-managing landlord tools (condition / move-in–move-out).  
**Job:** walk a rental unit room by room, photograph condition, finish a report
that survives crashes, and get a signed PDF off the phone.  
**Paying user:** the landlord who manages a handful of units themselves.  
**Bought:** one non-consumable unlock — unlimited units, and PDF export
without the watermark.  
**Price:** **$9.99** once.  
**Moment:** after one free unit walk, when they tap Share / Export (second unit
or watermark-free PDF).

## Arithmetic (money-owner stress-test 2026-10-05)

Full write-up: `apps/2026-10-05-unit-walk/docs/2026-10-05-unit-walk-money.md`. Net at **15% Play
fee** (plan default); **30%** row is stress only.

| Step | Amount |
|---|---:|
| List price | $9.99 |
| Net @ 15% Play fee | **≈ $8.49** |
| Net @ 30% (stress) | **≈ $6.99** |
| Sales / month for ≈ $100 net @ 15% | **≈ 12** unlocks |
| Sales / month for ≈ $1,000 net @ 15% | **≈ 118** unlocks |
| Kill floor (60 days) | **$80 revenue / 800 installs** |

Rejected archetypes (revenue reasons): ads-led (zero ads on enriched top ten;
hostile mid-walk); repeat sales (no consumable); purchase-only upfront (leaders
are free+IAP/SaaS). Subscription is banned and not weighed.

## Incumbents as demand evidence (MEASURED 2026-10-05)

| Incumbent | Rating | Reviews | Installs | Signal |
|---|---:|---:|---:|---|
| RentCheck | 4.7★ | 7,200 | **100K+** | Demand at scale (cloud; no Play IAP badge) |
| myInspections | 4.5★ | 193 | 10K+ | Freemium + IAP |
| Property Inspect | **2.1★** | 42 | 10K+ | Weak mid-tier — forced login, crashes |
| Yardi Inspection Mobile | **1.5★** | 44 | 10K+ | Weak mid-tier — lost photos (enterprise seat) |
| Property360 | 4.2★ | 25 | 5K+ | Data-loss 1★ on Next |
| PropertyLog | unrated | — | 1+ | Offline one-time shape already claimed (conversion risk) |

Query `landlord inspection app`: scan-market **mixed**, guard ok, hand on-job
share ≈ **0.57**. Shared complaints: photo/data loss, forced account, crash
mid-walk (`scan-complaints.mjs` packages above).

## Promises

See `apps/2026-10-05-unit-walk/docs/unit-walk.differentiators.json` (all `deferred`):

| id | weight | stage | family |
|---|---|---|---|
| crash-safe-photo-draft | core | during | durability |
| no-account-walk | core | during | trust |
| move-in-vs-move-out | core | during | core_operation |
| signed-pdf-share | supporting | after | data_out (connective) |
| multi-unit-portfolio | supporting | before | fit |

## Name

**UnitWalk** — coined, score **81/100**. Field and rejections in
`apps/2026-10-05-unit-walk/docs/unit-walk.naming.json`. Negative control: *Rental Inspection*.

<a id="name"></a>

## Device capabilities

Each capability is answered `used` or `not used`; a `used` line names the API and what the job loses without it.

- camera: used — `TakePicture` / `ACTION_IMAGE_CAPTURE`, permission `CAMERA`; without it the walk has no condition evidence.
- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia` (no runtime permission); without it a landlord cannot attach an already-taken photo from the gallery.
- media playback or capture: not used — the job is still photos and a PDF, not video or audio capture.
- microphone: not used
- location: not used — unit identity is the landlord’s portfolio name, not GPS.
- motion sensors: not used
- contacts / calendar: not used
- notifications / reminder: not used — v1 does not schedule walk reminders; adding them later needs `POST_NOTIFICATIONS` and a new promise.
- home-screen widget: not used
- share / export: used — `ACTION_SEND`, `FileProvider`, and/or `ACTION_CREATE_DOCUMENT`; without it the signed PDF cannot leave the phone.
- Bluetooth / USB: not used
- files / SAF: used — `ACTION_CREATE_DOCUMENT` to save the PDF where the landlord chooses; without it share-only may still work but filing into Drive/Downloads is weaker.

## Build

- **Estimate:** ~5 agent hours (camera walk + local draft store + PDF + billing unlock).
- **Server:** not required.
- **Capabilities:** `billing` required; `ads` deliberately off.
- **Permissions:** `CAMERA`; no account; offline-first.

## Kill criteria

60 days: **&lt; 800 installs** or **&lt; $80** revenue → kill (see
`apps/2026-10-05-unit-walk/docs/2026-10-05-unit-walk-money.md` for convert pairing).
(money-owner rewrite; see `apps/2026-10-05-unit-walk/docs/2026-10-05-unit-walk-money.md`).

## Honest unknowns

Listed in `apps/2026-10-05-unit-walk/docs/unit-walk.economics.json` `assumptionsUnverified` — chief among them:
willingness to pay $9.99 once, and whether unrated offline rivals already took the buyers.

## Scaffold (only after owner approval)

```bash
node scripts/new-slot.mjs \
  --economics apps/2026-10-05-unit-walk/docs/unit-walk.economics.json \
  --differentiators apps/2026-10-05-unit-walk/docs/unit-walk.differentiators.json \
  --naming apps/2026-10-05-unit-walk/docs/unit-walk.naming.json \
  --spec app-unit-walk
```

<a id="promises"></a>

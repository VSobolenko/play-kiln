# Solo field-trade recon — 2026-10-05 (Scout A + orchestrator re-measure)

Measurement record for `/forge` recon on 2026-10-05. Shelf: solo service /
field-trade jobs that are **not** lawn mowing and **not** classroom grading.

## 1. How it was measured

| Step | Command / path | Label |
|---|---|---|
| Query set | `.kiln/scratch/forge-2026-10-05/queries.json` → `node scripts/scan-market.mjs --query-set …` | MEASURED |
| Title hand-class | `node scripts/scan-titles.mjs --query "landlord inspection app"` (and siblings) | MEASURED |
| Enrichment | `node scripts/enrich-market.mjs --query "landlord inspection app" --top 10` plus RentCheck / Yardi package enrich | MEASURED |
| Complaints | `node scripts/scan-complaints.mjs --package com.radweb.pi --package yardi.Android.Inspections --package com.embedded_factory.property_360 --package nz.co.myrent.myInspections` | MEASURED |
| Scout A browser pass | Play pages via Playwright/WebFetch while Ask-mode blocked Node in the subagent | MEASURED (browser); scripts re-run by orchestrator |

## 2. Scan-market verdicts (scripted)

| Query | Verdict | Notes |
|---|---|---|
| `rental property inspection checklist` | dead-zone | Sensor saw **0/30 rated on the search cards** (ratings often absent on this SERP; detail pages still show ratings — do not treat this as "no demand"). |
| `move in move out inspection` | candidate (then titles showed unrated cards) | Many on-job titles; ratings often missing on the search card. |
| `landlord inspection app` | **mixed** | Guard **ok** (22/30 on-topic); demand proven, 4 weak incumbents, median 4.40. **Primary pocket query.** |

## 3. Hand classification — `landlord inspection app`

On-job inspection / condition tools among first 30 (titles): Rental Property Inspection,
myInspections, Property Inspect, Property360, PropertyLog, My Inspection App, Inspection App
(Point-of-Rental), RentVisie, RentCheck, RentProof, Yardi NSPIRE, AuditApp, zInspector 3,
zTenant, Rental Inspection Report, Live Inspection, Inspector by Property Vista — roughly
**17/30 ≈ 0.57** on-job by title. Off-job: TurboTenant / Landlordy / Landlord Studio /
RentRedi (rent managers), ISN / Inspectify (home inspectors), sim game, etc.

## 4. Enrichment — money on the shelf (MEASURED 2026-10-05)

| App | Package | Rating | Reviews | Installs | Ads | IAP |
|---|---|---:|---:|---:|---|---|
| RentCheck | `com.rentcheck.app` | 4.7★ | 7,200 (7,198) | **100K+** | false | false |
| TurboTenant | `com.turbotenant.owner` | 4.7★ | 4,210 | 50K+ | false | false |
| Landlordy | `lv.eprotect.droid.landlordy` | 4.4★ | 443 | 50K+ | false | true |
| myInspections | `nz.co.myrent.myInspections` | 4.5★ | 193 | 10K+ | false | **true** |
| Property Inspect | `com.radweb.pi` | **2.1★** | 42 | 10K+ | false | false |
| Yardi Inspection Mobile | `yardi.Android.Inspections` | **1.5★** | 44 | 10K+ | false | false |
| Property360 | `com.embedded_factory.property_360` | 4.2★ | 25 | 5K+ | false | **true** |
| Rental Property Inspection | `com.cleanutilityapps.moveinspection` | unrated | — | 50+ | false | true |
| PropertyLog | `app.propertylog` | unrated | — | **1+** | false | true |
| RentProof | `com.audioimmersia.rentproof` | unrated (search) | — | — | — | lifetime unlock (listing prose, Scout A) |

**Reading:** demand is proven at RentCheck scale. The reachable weak mid-tier is
Property Inspect (2.1★) and Yardi Inspection (1.5★). Offline one-time unlocks already
exist as **unrated** listings (PropertyLog, RentProof, Clean Utility) — conversion risk,
not a reason the job is empty.

## 5. Shared complaints (MEASURED sample)

From `scan-complaints.mjs` on the four packages above (Play samples ~3 reviews/page):

- Property Inspect: forced account / wrong credentials; crash mid-report; upload validation with no hint.
- Yardi Inspection: lost photos while finalizing; abandoned / no support; sync fails after "finalized".
- Property360: *“after spending 2hours on detailed report, upon clicking next, the app deleted everything”*.
- myInspections: no critical reviews in the sampled three (4.5★).

**Job the complaints name:** finish a room-by-room walk with photos that **survive** the
report, without an employer login, and leave with a shareable report.

Consistent with ON RECORD Candidate 2 in `docs/research/2026-09-24-market-decision.md` §2
(18/18 critical reviews in that earlier run).

## 6. Refused queries on this shelf (revenue reasons)

| Query / idea | Revenue reason |
|---|---|
| House cleaner / cleaning schedule | Homeowner-chore pollution + booking marketplaces; solo cleaner client book is a **catalogue near-duplicate of lawn-round-log**. |
| Pest control service log | ON RECORD dead-zone (`docs/ideation/2026-10-01-accountable-service-records.md`). |
| Pressure washing business | Game pollution; no rated on-job mass in Scout A pass. |
| HVAC service report | OEM Bluetooth companions / thin; no offline visit-log pocket measured. |
| Locksmith work order | Thin; sits next to killed `field service work order app` fortress. |
| Car detailing round log | CRM is server; offline DetailProof unrated; round-log shape duplicates lawn-round-log. |

## 7. Pocket that clears the bar

**Self-managing landlord unit walk** (slug `unit-walk`): offline room-by-room move-in /
move-out condition report with crash-safe photo drafts, multi-unit portfolio, dual
signatures, and on-device PDF share — bought once.

**Wedge (functionality):** A landlord who owns a few units gets a crash-safe, no-account
walk that keeps every photo through the report, compares move-in to move-out per unit,
and shares a signed PDF from the phone — without RentCheck’s cloud camera failures or
Yardi / Property Inspect employer login.

### Honest unknowns

- Whether that landlord pays once instead of Notes + Camera roll (**UNVERIFIED**).
- Whether unrated PropertyLog / RentProof already absorb offline one-time buyers before
  a new listing converts (**UNVERIFIED**).
- Install-to-unlock conversion (**UNVERIFIED**; plan uses ~2% assumption).

### Falsifiable flip → FAIL

If a fresh hand classification of `landlord inspection app` shows **&lt; ~35% on-job
titles**, or if PropertyLog or RentProof reach **≥4.3★ with ≥200 reviews** while still
shipping the offline one-time PDF shape — the mid-tier / wedge is gone and this recipe
fails.

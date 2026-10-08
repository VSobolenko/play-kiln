# Finalist proposals — independent verification

Date: 2026-10-08.  This report re-ran the factory's signed-out Play readers (`hl=en`, `gl=US`) and read the generated artifacts, rather than accepting the scouts' signed claims.  Install figures are Play's displayed rounded buckets.  `reviews` below means Play's displayed count; a separately labelled schema.org exact count is not substituted for it.

## A. RoomGauge — paid comparator

**Scout claim:** CamToPlan is `$19.99`, `4.5★`, `7,914 reviews`, and `100K+ installs`.

**Negative observation specified before the run:** a different title/package, rating, *displayed* review count, install bucket, or price.

**Command and observed reading:**

```powershell
node scripts/enrich-market.mjs --query "CamToPlan" --top 10 --concurrency 3 --out .kiln/scratch/roomgauge-camtoplan-verify.md
```

The `CamToPlan` row reports title `CamToPlan`, package `com.tasmanic.camtoplan`, rating `4.5`, displayed reviews `7,910`, installs `100K+`, and `paid ($19.99)`.  The same row separately prints schema.org `reviews (exact)` as `7,914`.

**Verdict: CONTRADICTED (observed: Play's displayed review count is `7,910`, not `7,914`; `7,914` is the separately reported schema.org exact count).** All other named fields match.

Evidence: `.kiln/scratch/roomgauge-camtoplan-verify.md`.

## B. NightVector — paid shelf

**Scout claim:** PhotoPills is `$10.99` / `100K+` / `4.8★` / `8,220 reviews`; Sun Surveyor is `$9.99` / `100K+` / `4.8★` / `5,140 reviews`; Planit Pro is `$9.99` / `10K+` / `4.6★` / `1,220 reviews`; SkySafari is `$6.99` / `10K+` / `4.3★`.

**Negative observation specified before the run:** any named app rendering a different price, rating, displayed review count, or install bucket.

**Command and observed reading:**

```powershell
node scripts/enrich-market.mjs --query "PhotoPills" --query "Sun Surveyor" --query "Planit Pro" --query "SkySafari" --top 10 --concurrency 3 --out .kiln/scratch/nightvector-verify-core.md
```

| Observed Play title / package | Price | Rating | Displayed reviews | Installs |
| --- | --- | ---: | ---: | --- |
| `PhotoPills` / `com.photopills.android.photopills` | `paid ($10.99)` | 4.8 | 8,220 | 100K+ |
| `Sun Surveyor (Sun & Moon)` / `com.ratana.sunsurveyor` | `paid ($9.99)` | 4.8 | 5,140 | 100K+ |
| `Planit Pro: Photo Planner` / `com.yingwen.photographertoolspro` | `paid ($9.99)` | 4.6 | 1,220 | 10K+ |
| `SkySafari Astronomy` / `com.simulationcurriculum.skysafari7` | `paid ($6.99)` | 4.3 | 288 | 10K+ |

**Verdict: CONFIRMED.** Every stated B field matched the fresh rendered reading.  The SkySafari review count was not claimed, so `288` is recorded rather than treated as a discrepancy.

Evidence: `.kiln/scratch/nightvector-verify-core.md`.

## C. Kettlepath — direct shelf and ceiling attempt

**Scout claim:** Brewfather is `100K+` / `2,860 reviews` / `4.8★` / ads absent / IAP present; BeerSmith 3 Mobile is `50K+` / `1,660 reviews` / `3.6★` / paid `$4.99`; Brewer's Friend is `10K+` / `64 reviews` / `4.2★`.  The proposal itself marks whole-market maximum as unverified.

**Negative observation specified before the run:** a different named field for any listed app, or a direct homebrew recipe/brewing-planning app above Brewfather's `100K+` bucket.  Beer social/discovery is excluded from the latter test.

**Commands and observed reading:**

```powershell
node scripts/enrich-market.mjs --query "Brewfather" --query "BeerSmith 3 Mobile" --query "Brewer's Friend" --top 10 --concurrency 3 --out .kiln/scratch/kettlepath-verify-known.md
node scripts/enrich-market.mjs --query "homebrewing software" --query "homebrew recipe" --query "all grain brewing calculator" --query "beer recipe designer" --query "brew day app" --query "beer brewing app" --top 10 --concurrency 3 --out .kiln/scratch/kettlepath-verify-ceiling.md
```

| Observed Play title / package | Rating | Displayed reviews | Installs | Ads / IAP | Price |
| --- | ---: | ---: | --- | --- | --- |
| `Brewfather` / `com.warpkode.brewfather` | 4.8 | 2,860 | 100K+ | absent / present | free |
| `BeerSmith 3 Mobile Homebrewing` / `com.beersmith.beersmith2full` | 3.6 | 1,660 | 50K+ | absent / absent | paid ($4.99) |
| `Brewer's Friend` / `com.brewersfriend.app` | 4.2 | 64 | 10K+ | absent / present | free |

**Verdict, named listing fields: CONFIRMED.**

**Ceiling attempt:** the six additional searches found no direct homebrew recipe/planning app above `100K+`; Brewfather remained at `100K+`, BeerSmith at `50K+`, Brewer's Friend at `10K+`, and direct calculators at most `50K+`.  Untappd rendered `5M+`, but is beer discovery/social rather than a direct brewing-planning app and was excluded under the stated criterion.

**Verdict, whole-market maximum: UNVERIFIABLE (the capped, top-ten signed-out searches found no higher direct app, but cannot prove the whole market maximum).** The largest qualifying bucket actually observed was `100K+` (Brewfather).

Evidence: `.kiln/scratch/kettlepath-verify-known.md`; `.kiln/scratch/kettlepath-verify-ceiling.md`.

## D. Joined-workflow falsifier

**Scout falsifier shape:** an incumbent already delivers the whole joined workflow.  A positive finding is a single app whose own listing or official description expressly covers every named step; its absence would require the inspected alternatives and their observed scope to be recorded.

### RoomGauge

**Negative observation specified before the run:** one app explicitly covers `measure a room → subtract openings → material/pack quantities → PDF`.

Commands:

```powershell
node scripts/scan-titles.mjs --query "room measurement material estimate" --query "floor plan material calculator" --query "room measurement PDF" --concurrency 3 --out .kiln/scratch/roomgauge-falsifier-titles.md
node scripts/enrich-market.mjs --query "magicplan" --query "Floor Plan Creator" --query "Hover Measure Design Estimate" --top 3 --concurrency 3 --out .kiln/scratch/roomgauge-falsifier-details.md
node scripts/scan-complaints.mjs --package com.tasmanic.camtoplan --package com.sensopia.magicplan --package pl.planmieszkania.android --package to.hover.android.app --top 6 --concurrency 2 --out .kiln/scratch/roomgauge-falsifier-complaints.md
```

The readers identified CamToPlan, magicplan, Floor Plan Creator, and Hover: Measure Design Estimate.  Their output covers store identity, market fields and sampled reviews; it does not output a full listing description or feature inventory.  The sample observed CamToPlan unit/feature complaints, magicplan references to camera scanning/blueprinting, a Floor Plan Creator export/cloud-sync complaint, and a Hover review mentioning interior photos.  None evidences all five steps in one app, and none can establish their absence.

**Verdict: UNVERIFIABLE (the permitted factory outputs cannot evidence a negative capability conclusion for the full five-step chain).**

### NightVector

**Negative observation specified before the run:** one app explicitly covers dark window, deep-sky target curves, satellite passes, weather, and exposure guidance in one briefing.

Commands:

```powershell
node scripts/scan-titles.mjs --query "astrophotography planner" --query "satellite pass tracker" --query "astronomy weather" --query "dark sky planner" --concurrency 3 --out .kiln/scratch/nightvector-verify-falsifier-titles.md
node scripts/scan-market.mjs --query "astrophotography planner" --query "satellite pass tracker" --query "astronomy weather" --query "dark sky planner" --concurrency 3
```

The title scan surfaced specialists including Planit Pro and PhotoPills (photography planning), SkySafari and Stellarium (sky/target maps), Satellite Tracker/ISS Detector (passes), and Astrospheric/Clear Outside (weather).  Per the reader's own documented limitation, a title is not a capability statement, and its output did not include the listings' descriptions.  No output showed a single app asserting all five functions.

**Verdict: UNVERIFIABLE (the factory outputs provide shelf identity, not the capability evidence needed for a supported negative answer).**

### Kettlepath

**Negative observation specified before the run:** one on-device tool's own description covers recipe design, water/mash-pH work, guided brew day, packaging, and BeerXML import/export.

Command locating the candidates:

```powershell
node scripts/scan-titles.mjs --query "Brewfather BeerXML water chemistry brew day packaging" --query "BeerSmith BeerXML water profile brew day" --query "Brewer's Friend BeerXML water mash pH brew day" --concurrency 3 --out .kiln/scratch/kettlepath-verify-falsifier-titles.md
```

The candidate scan located Brewfather, BeerSmith 3 Mobile Homebrewing, and Brewer's Friend.  Brewfather's own public feature/documentation descriptions were then read: they describe recipe design, water target profiles/salt additions/estimated mash pH, recipe-derived step-by-step brew-day timers, fermentation/carbonation/packaging settings, and BeerXML import/export.  BeerSmith's official mobile page supports recipe editing, water and fermentation/carbonation profiles and a brew-day timer, but this reading did not establish on-device BeerXML import/export; Brewer's Friend's official page supports BeerXML and recipe/water functions but did not establish the entire mobile chain.

**Verdict: CONTRADICTED (observed: Brewfather's own feature and documentation descriptions advertise every named Kettlepath workflow element in one tool).**

Evidence: `.kiln/scratch/kettlepath-verify-falsifier-titles.md`; source-reading record in `.kiln/scratch/kettlepath-verifier-summary.md` (Brewfather home page and official documentation).

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
| --- | --- | --- |
| A — CamToPlan paid comparator numbers | CONTRADICTED | `roomgauge-camtoplan-verify.md`: displayed `7,910`; schema exact `7,914` |
| B — four NightVector paid shelf readings | CONFIRMED | `nightvector-verify-core.md` |
| C — three named Kettlepath listing readings | CONFIRMED | `kettlepath-verify-known.md` |
| C — Brewfather is the whole-market direct ceiling | UNVERIFIABLE | Six extra queries found no higher direct app, but capped top-ten screens cannot prove a market maximum |
| D — RoomGauge joined workflow absent | UNVERIFIABLE | Factory outputs did not expose full capability descriptions |
| D — NightVector joined workflow absent | UNVERIFIABLE | Factory outputs did not expose full capability descriptions |
| D — Kettlepath joined workflow absent | CONTRADICTED | Brewfather official feature/docs chain recorded in `kettlepath-verifier-summary.md` |

DONE 7 claims checked: 2 CONFIRMED, 2 CONTRADICTED, 3 UNVERIFIABLE; falsifier answer per slot: RoomGauge UNVERIFIABLE, NightVector UNVERIFIABLE, Kettlepath YES — Brewfather.

BLOCKED none.

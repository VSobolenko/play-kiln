# Proposal: app-lumintra-light-planner

**Slot:** `apps/<date>-lumintra-light-planner` — **Lumintra: Light Planner** — slug
`lumintra-light-planner` — `applicationId` **`com.gexetr.lumintralightplanner`** —
`purchase_only_no_ads`, one-time price **$9.99** set in Play Console.

Every factual claim below carries the factory's four labels (`.cursor/rules/grounded-claims.mdc`):
**MEASURED** (read or run in this session, with the path or command), **ON RECORD** (a named source
states it, and this session read that source), **INFERRED**, or **UNVERIFIED** (with the check that
would decide it). The storefront numbers are **ON RECORD**: they are stated by
`docs/research/2026-10-08-light-and-sky-session-planner-market.md`, which this session read and which
records the commands that produced them in its own §11 claim ledger.

## Why this slot, why now

**The app, and who it is for.** Lumintra is a paid, offline-first session planner for the photographer
who plans one shoot around the light. The user saves a **spot** (a place, plus a horizon line they draw
themselves) and an equipment profile (a lens); then, for one night, the app composes a **brief** that
answers every question the shoot depends on: when the sun sets and the golden, blue and civil, nautical
and astronomical twilight windows run; the **dark window** — the true moonless astronomical-dark
interval; where the moon will be and how much it washes the sky; which of 250 famous deep-sky
**targets** are above their silhouette and for how long, and how each **frames** the lens; whether a
satellite **pass** will streak the long exposure through that frame; and what the hourly **cloud cover**
will do, drawn against the same time axis as the darkness. It answers all of it on one scrubbable
**Night scene** — the user's own horizon silhouette as the foreground, the real sky above it, dragged
through the night — and hands the finished answer off as a postcard or a plain-language text brief.
The buyer is the outdoor / night / landscape photographer, and the astrophotographer who wants the same
brief to reach the moon, the dark window, targets, passes and cloud in one place; not the point-your-
phone-at-the-sky learner, who is served by a free giant.

**Why now.** The shelf that carries the money — the sun / moon / light-planning photography shelf —
is populated by standalone paid apps whose own listings name one half of this job or another, and the
four things this app joins are measurably absent from it (see **Demand evidence**). The factory has
built record-keepers and a swim-set computer; this is its first **sky-and-light computation** product,
and it is designed around a scrubbable scene rather than a form.

The three gate-ready contracts were authored at recon, before any UI existed, and this change is built
from them:

- `docs/ideation/plans/lumintra-light-planner.economics.json` — the money plan (stage 0.5).
- `docs/ideation/plans/lumintra-light-planner.differentiators.json` — the **binding** promise contract:
  **13 promises** after this change (7 already on disk, 6 added here), 5 of them `core`, every one
  starting `deferred` (stage 0.6). The count must be re-read from the file before release — the six new
  claims are being written in parallel with this change.
- `docs/ideation/plans/lumintra-light-planner.naming.json` — the derived name, slug and applicationId
  (stage 0.7).

The change carries **eleven** capabilities under `specs/<capability>/spec.md`, one delta spec each —
`night-scene`, `session-brief`, `target-catalogue`, `satellite-passes`, `cloud-forecast`,
`scout-spots`, `share-brief`, `night-glance`, `settings-and-credits`, `welcome` (the first-run screen)
and `visual-identity` (the app's declared identity, authored before the UI is styled).

The slot is new and independent. Per `.cursor/rules/ideation-gate.mdc` § "The factory is not the
market" it is built from its own plan, its own promise contract and the shared harness only — no
sibling slot's code is read or reused. The namespace rule is not restated here: the `applicationId` is
derived by `scripts/lib/namespaces.mjs` (`com.gexetr.<slug>`).

## Demand evidence

The full record is `docs/research/2026-10-08-light-and-sky-session-planner-market.md`; the competitor
matrix, the adopt list and the rejected list are the recon worker's `annex/K-competitor-features.md`;
the Play-integration, permission and Data-safety facts are
`annex/M-play-integrations-compliance.md`.

### The shelf, and the paid anchors

- **ON RECORD** — the load-bearing shelf is the sun / moon / **light-planning** photography shelf, and
  its paid rows are standalone purchase-only apps (market record §2, `enrich-market.mjs --top 6`):
  **PhotoPills** `com.photopills.android.photopills` **$10.99 / 100K+ / 4.8 (8,220)**, **Sun Surveyor**
  `com.ratana.sunsurveyor` **$9.99 / 100K+ / 4.8 (5,140)**, **Sun Position PRO** `$9.99 / 50K+ /
  4.9 (2,730)`, **Planit Pro** `com.yingwen.photographertoolspro` **$9.99 / 10K+ / 4.6 (1,220)`,
  **Sun Seeker** `$8.99 / 50K+ / 4.8 (1,000)`, **SunOnTrack** `$7.99 / 50K+ / 4.8 (874)`,
  **Golden Hour+** `$4.99 / 1K+ / 4.7 (189)`. Every row declares **no ads** (market record §2). A
  durable second reading of the four biggest rows is in `docs/research/2026-10-08-finalists-
  verification.md` §B.
- **ON RECORD** — **evidence this audience pays**: PhotoPills and Sun Surveyor are **standalone
  purchase-only** listings at **100K+ installs** with **4.8** ratings (market record §2). **INFERRED**
  (arithmetic, floor only): `100,000 × $10.99 ≈ ≥ $1,099,000` and `100,000 × $9.99 ≈ ≥ $999,000`
  gross before Play's commission — floors, because install buckets are rounded lower bounds counting
  devices, not buyers.
- **ON RECORD** — the observed one-time price band on this shelf is **$4.99–$10.99**, the core is
  `$7.99–$10.99`, and **$10.99 (PhotoPills) is the ceiling; nothing above it exists on this shelf**
  (market record §5).

### The four measured structural gaps

- **ON RECORD** — a keyword scan of all ten light-planner store descriptions (market record §3,
  `.kiln/scratch/positioning-2026-10-08/C-sun-moon-light-shelf.md` §3 — the real path, **not copied
  into this change**) measured: **(a)** the true darkness window is **partial** — every
  incumbent plans *which object is where*, none computes the dark interval itself; **(b)** the deep-sky
  object catalogue is **almost absent** — only Planit Pro names "nebulae azimuth and elevation", with
  no object catalogue, magnitudes, apparent sizes or per-target window; **(c)** satellite passes are
  **absent entirely** — the only `satellite` hit is a SunOnTrack map layer and every `iss` match was
  the word "miss"; **(d)** hourly cloud cover is **absent with one exception** — only PhotoTime claims
  a generic "weather forecasts", and the scan found **0** hits for `cloud`.
- **ON RECORD** — the four gaps are **absences from the products, not yet demand in the sampled
  reviews**: the sampled complaint clusters are paywall, accuracy, data-loss/portability and
  broken/licence portability, and **no sampled complaint mentions weather, cloud, satellites or
  deep-sky objects** (market record §3.1, `fetchReviews`, 3-review sample).
- **ON RECORD** — the measured portability complaint the `carry-your-scout` promise answers: Planit Pro
  users report that **29 plans could not easily be transferred to a new tablet** and that a purchased
  location feature would not restore on a second phone (market record §3.1). PhotoPills' sampled
  complaint is re-paying on another platform (`annex/K-competitor-features.md` §4 ledger).
- **ON RECORD** — **night is bundled into every paid light-planner**: Sun Surveyor, PhotoPills, Planit
  Pro, Sun Position and Ephemeris all sell Milky Way / star-trail / night content inside the same paid
  product as the golden-hour planner (market record §3.2, five MEASURED description quotes).
  **INFERRED** — vendors do not bundle what buyers do not ask for, so the night buyer is the shelf's
  own answer.

### The satellite anchor, and why it does not lift the price

- **ON RECORD** — **ISS Detector Pro** `com.runar.issdetector.pro` is **$17.99 / 100K+ / 4.4 (3,189)**
  with no ads — the highest observed paid price in the recon (market record §4.1). Its **free parent**
  `com.runar.issdetector` is **5M+ / 120,809 reviews / ads+IAP** (same run). **INFERRED** — the Pro is
  reached through the free app's own funnel (`100K+ / 5M+ ≈ 2 %`), so **$17.99 is a price paid by
  already-acquired users, not one won on discovery**; a new standalone entrant has no 5M+ funnel, so
  pass timing differentiates *inside* the light shelf's band and does not lift the price above $10.99
  (market record §4.2).
- **ON RECORD** — no shipped Android paid app was found bundling satellite-**streak** forecasting
  against a photographer's exposure into a light-planner; it exists only as research/web tools
  (AstroSat, Tonight's Window, PSF Guard) (`annex/K-competitor-features.md` §1.3). **INFERRED** — the streak
  flag is the most defensible single capability in the plan.

### What we are not claiming, and the honest counters

- **ON RECORD** — the head terms are owned by **free giants**: Sky Map is **50M+** and every 10M+ app on
  the stargazing shelf is LIVE (point-and-identify); the largest pure-PLANNING app observed is
  **100K+**, a ratio of **≈500×** (market record §2.1). This proposal does **not** compete there.
- **UNVERIFIED** — that this buyer wants a *deep-sky object brief* or a *satellite-pass brief* bundled
  into a light-planner: **no incumbent sells either**, so there is no observed willingness-to-pay. The
  strongest honest counter-argument to the idea, and it travels with it (market record §3.2, §10 item 4).
- **UNVERIFIED** — that the light-planning shelf supplies **64 paid sales/month** to a new entrant. No
  revenue API exists; settle by 120 days of Play Console data via
  `node scripts/check-outcomes.mjs --slot lumintra-light-planner` (market record §10 item 8).
- **UNVERIFIED** — the **moon-path** numerical accuracy of the vendored astronomy engine (only the Sun
  path is numerically verified, 0.0248 arcmin; the three Moon corpus cases were refused). Settle against
  JPL Horizons rows as `AstroCorpus.kt` does for the Sun (market record §10 item 10).
- **UNVERIFIED** — that **CelesTrak's** usage policy permits a commercial app: its policy page returned
  HTTP 500. Settle in a browser before the feed freezes (market record §6.4, §10 item 3).
- **ON RECORD** — the $17.99 anchor and the satellite shelf were **first read wrong**: money-owner
  revision 1 claimed "six of six free" from the top-6 window of `satellite tracker`, and the paid Pro
  ranked outside it. The correction is kept, and the lesson — *a top-N enrichment window is not a
  shelf* — is reusable methodology (market record §8).

## Economics

Contract: `docs/ideation/plans/lumintra-light-planner.economics.json` (validated by
`node scripts/check-economics.mjs --slot lumintra-light-planner`).

- **Archetype:** `purchase_only_no_ads`; the slot **requires no capability** — the price is a Play
  Console listing attribute, not an in-app product. `billing` and `ads` are **deliberately not enabled**
  (`INFERRED`: no in-app store, and every paid row on the shelf declares no ads).
- **Rejected alternatives**, each with its revenue reason (`archetypesRejected`):
  `purchase_led_single_unlock` (**UNVERIFIED** — rests entirely on an install-to-purchase conversion
  this recon did not observe, and the light shelf's paid apps are purchase-only standalone listings, not
  free-plus-unlock; it would copy ISS Detector Pro's shape while lacking its 5M+ funnel);
  `ads_led_utility` (**ON RECORD** — banner-only ads imply roughly 5,500 daily active users for $500/month,
  which a few-times-a-month session planner cannot hold, and the shelf shows zero ads on its paid rows);
  `purchase_led_repeat_sales` (**INFERRED** — there is nothing to re-sell: the brief is produced per trip
  and the forecast and passes are fetched, not sold); `subscription` (**banned factory-wide**, absent
  from `ARCHETYPES`).
- **Paying user:** a landscape or outdoor photographer planning a shoot who buys a one-time sky-and-light
  session planner from Play.
- **What is bought:** one lifetime unlock of the complete session-brief planner — the combined Sun, Moon,
  twilight, dark-window, deep-sky catalogue, cloud-cover and satellite-pass brief for a location and date,
  with share/export and **no ads**.
- **Price:** **$9.99**, one time, set in Play Console. **Moment:** at the Play purchase screen, **before**
  the user creates the first session brief for an upcoming shoot. There is no trial clock, no in-app
  product and no subscription prompt anywhere.
- **Arithmetic, with the division shown** (market record §5; `annex/M-play-integrations-compliance.md` §5.0):
  ```
  price                                        9.99
  Play service fee 15%                        -1.50   (9.99 x 0.15)
  refund + currency-conversion 8%             -0.80   (9.99 x 0.08)
  ------------------------------------------------------------------
  net per sale                                 7.81   (9.99 x 0.85 x 0.92)

  target (docs/monetization-strategy.md):     500.00 per month
    sales/month  = 500 / 7.81                  =  64.0  ->  64 sales/month
  ```
  The 15 % is the first-`$1M` service-fee tier, so $7.81 is a floor. At the **30 % tier** the same app
  needs **78 sales/month** to clear the target (market record §5). The conversion from a Play impression
  to a $9.99 purchase is **UNVERIFIED** — no slot in this repository has measured one — and the
  64-sales/month figure moves directly with it (`assumptionsUnverified`).
- **Kill criteria:** `windowDays` **120**, `minInstalls` **150**, `minRevenueUsd` **1000** — judged
  afterwards by `node scripts/check-outcomes.mjs --slot lumintra-light-planner`. These are the
  repository's **derived kill floors**, not the target, and the plan records the deliberate tension the
  two expose: at the 15 % tier the 150-install floor implies about **$1,172** net (`150 × 7.81`), while
  at the 30 % tier the same 150 installs yield about **$965** (`150 × 6.43`) — **below the $1,000 revenue
  floor** (`assumptionsUnverified`).
- **Build cost is information, never a criterion** (`.cursor/rules/ideation-gate.mdc`, owner's
  instruction recorded 2026-10-08). A measured packaging proof exists: an Orekit-based Android build
  reached `BUILD SUCCESSFUL`, installed and ran on the A40 with byte-identical JVM numerics on the SGP4
  path (market record §7). That is a **packaging** PASS and is **not** a PASS on the dark-window,
  catalogue or weather paths.

## Distinctness

*One location-and-date session brief joins a true darkness window, a deep-sky target catalogue,
satellite passes and hourly cloud cover that no app on this shelf combines, instead of leaving the user
to reconcile a light-planner, a satellite tracker and a weather app.* (**ON RECORD**,
`economics.json` `distinctnessSentence`, written verbatim.)

Every clause is a difference in **functionality or content**, not in naming or colour
(`docs/monetization-strategy.md` §1.2), and each answers a gap measured above: the dark window against
gap (a), the catalogue against gap (b), the passes against gap (c), the cloud against gap (d). The
drawing of the horizon line and the sharing of the brief answer the measured portability complaint.

## Internal-catalogue check (Repetitive Content)

Checked against `registry/apps.json` **`id` / `name` / `idea` only** — this session read
`registry/apps.json` and opened no slot tree under `apps/**`.

| Existing | Job (from `id` / `name` / `idea`) | Verdict |
|---|---|---|
| `smoke`, `pipe-proof` | throwaway pipeline proofs | not products |
| `pipe-sizing` (killed) | gas pipe sizing workbook | burned trade-calculator direction |
| `teacher-gradebook` (published) | private offline classroom record book — grades, attendance, notes, CSV/PDF export | a **record**, different category, different buyer |
| `lawn-round-log` (building) | ordered customers, one-tap visits, owed balances, CSV export | a **field record**, different category and buyer |
| `laneset` (building) | swim set computer from the swimmer's constraints | a **computation**, but a training plan, not a sky-and-light plan — different data model, different buyer |
| `unit-walk` (building) | rental unit move-in/move-out condition records + signed PDF | a **record**, different category and buyer |

No slot in the catalogue plans a shoot around the sky or the light. `lumintra-light-planner` is the
catalogue's first **astronomy/light computation for a photographer**: one sentence cannot describe both
it and any existing slot, which is the distinctness test Play's Spam policy reaches. The nearest
catalogue neighbour by shape is `laneset` (both compute a plan from the user's constraints and both have
a share handoff), and their jobs — a swim-set computer and a sky-and-light session planner — share no
data model, no screen and no buyer.

## Non-goals

The recon excluded these because a leader already does each one, or because the lane forbids it. They
are stated here so a later reader cannot mistake their absence for an oversight.

- **No compass, no AR "point your phone at the sky" identification.** Every 10M+ stargazing app and the
  50M+ Sky Map are LIVE, and the compass is a **measured defect surface** — Sun Surveyor's most recent
  negative review is an accuracy complaint, and Star Walk 2 / Sky Map reviews repeat compass drift and
  calibration complaints (`annex/K-competitor-features.md` §3 R1–R2). The overlay is an FoV frame drawn in the
  scene, never a camera-compass view.
- **No full light-pollution map and no Bortle raster.** The widely used Falchi atlas is **CC BY-NC** and
  cannot ship in a paid app; a `500K+` free incumbent owns the map. Darkness is a computed `SkyGrade`
  description (`domain/SkyGrade.kt`), **never a Bortle class**.
- **No aurora / Kp / solar-wind.** A separate audience and a separate feed, already served cheaply.
- **No third-party catalogue ships.** OpenNGC is **CC-BY-SA-4.0** (a denylisted licence for a paid
  closed-source app), NGC 2000.0 and Hipparcos/Tycho are non-commercial, Stellarium's is GPL, and
  Wikidata — though CC0 — was measured to be unenumerable as a catalogue. The 250-object table
  (**110 Messier + 140 non-Messier NGC/IC showpieces**) is **hand-compiled from facts** (designations,
  coordinates, magnitudes) and each row verified against **two independent sources**; the star table is
  **FK6 (CC0) plus a hand-verified supplement** (FK6 lacks Sirius, Arcturus, Capella and Betelgeuse).
- **No cloud account, no sync, no server we run.** The app is offline by construction except the keyless
  `api.met.no` cloud feed and the keyless CelesTrak element feed; the non-commercial Open-Meteo free tier
  is **not** used.
- **No telescope control, plate solving, image import or editing** — a different buyer and job.
- **No quizzes, stories, guides or gamification** — the learning audience, served by free giants.
- **No eclipse / tide / rainbow calculators** — each a different job; they pad the app without answering
  "will tonight work?"
- **No location permission.** Manual coordinates, a saved spot, the app's own coordinate picker and `Geocoder` all work
  without it; `ACCESS_COARSE_LOCATION` would buy only "where am I standing", which this product does not
  need. `POST_NOTIFICATIONS` is declared **only** because the approved reminder and widget need it.
- **No in-app product, no billing library, no ads, and no subscription at any point.** `subscription` is
  not in `ARCHETYPES` and is machine-refused; `billing` and `ads` are deliberately not enabled. Exactly
  one price, set in Play Console.

# Proposal: app-laneset

**Slot:** `apps/2026-10-02-laneset` — **Laneset: Swim Set Builder** — `com.gexetr.laneset` —
`purchase_led_single_unlock`, one-time price **$9.99**.

Every factual claim below carries the factory's four labels (`.cursor/rules/grounded-claims.mdc`):
**MEASURED** (read or run in this session, with the path or command), **ON RECORD** (a named source
states it, and this session read that source), **INFERRED**, or **UNVERIFIED** (with the command that
would decide it). The storefront numbers are **ON RECORD**: they are stated by
`docs/research/2026-10-02-planners-and-optimisers-evidence.md`, which this session read and which
records the commands that produced them in its own §7 claim ledger.

## Why this slot, why now

This slot is the factory's first **planner/optimiser** — an app whose product is a computation it
performs from the user's own constraints, not a record it keeps. The lane boundary is deliberate: the
recon screened several shelves whose real job is "log your sessions" and discarded them as notebooks
(`rowing training plan`, `crop rotation planner`), and selected `swim workout generator` because it is
the rare lane shelf where the computing half is the product and the shelf's own users name the
computation and the paywall as what is wrong (**ON RECORD**,
`docs/ideation/2026-10-02-planners-and-optimisers.md` §1 and §2.6, verdict **PLAUSIBLE**).

The three gate-ready contracts were authored at recon, before any UI existed, and this change is
built from them:

- `apps/2026-10-02-laneset/economics.json` — the money plan (stage 0.5).
- `apps/2026-10-02-laneset/differentiators.json` — the **binding** promise contract: 7 promises, 3 of
  them `core`, every one starting `deferred` (stage 0.6).
- `apps/2026-10-02-laneset/naming.json` — the derived name, slug and applicationId (stage 0.7).

The slot is new and independent. Per `.cursor/rules/ideation-gate.mdc` § "The factory is not the
market" it is built from its own plan, its own promise contract and the shared harness only — no
sibling slot's code is read or reused.

## Demand evidence

The full record is `docs/research/2026-10-02-planners-and-optimisers-evidence.md`; the human-readable
proposal is `docs/ideation/2026-10-02-planners-and-optimisers.md` §2.

### The shelf, and the leader-first screen

- **ON RECORD** — `swim workout generator` returns verdict `mixed`, **11/30 rated, median 4.40, 5
  weak incumbents** (evidence §2 and §7 row 2, `scan-market.mjs`).
- **ON RECORD** — the top 10 is **7/10 in-app purchase, 2/10 ads, 0/10 priced**, largest install
  bucket `100K+` (evidence §3.1, `enrich-market.mjs --top 10 --query "swim workout generator"`).
- **ON RECORD** — the criterion leader is `net.smartwod.workouts` (SmartWOD Workout Generator,
  **500K+, 4.6★, 3,231 exact reviews**, ads and IAP), and its job is a **generic CrossFit-style WOD
  generator, not swimming**. The **on-job leader** is `com.myswimpro.android.app` (MySwimPro,
  **100K+, 3.8★, 3,102 exact reviews**, IAP, no ads) (evidence §3.3).
- **ON RECORD** — the shelf's best-rated on-job app is `io.swimup.application` (SwimUp, **4.5★ over
  1,234 exact reviews, 100K+**) with **0 critical reviews** in its three-review sample. This is
  evidence *against* the proposal and is recorded rather than buried (evidence §4).

### What the leaders' own users wrote

- **ON RECORD** — `com.myswimpro.android.app`, the on-job leader, carries the 1★ *"Swim workouts seem
  legit, however no real guidance to do the test workouts to get the dynamic pacing in place.
  Analysis is extra, 50, 100 or 150 bucks"*, and paywall is 2 of the 3 critical reviews sampled on it
  (evidence §3.4).
- **ON RECORD** — `com.gizmodus.myswimcoach` (**4.6★ over 2,771 exact reviews, 100K+**) carries the
  2★ *"in terms of the sets that the app generates are very repetitive and not very good for"*. This
  is the single most load-bearing reading: a well-rated on-job incumbent criticised by its own user
  for exactly the mechanism (repetitive sliced sets) that promises 1 replaces (evidence §3.4).
- **ON RECORD** — the shelf pocket's 14 critical reviews split **missing-feature 36% (5/14), paywall
  29% (4/14), broken 21%, crash 14%, notifications 14%, account 7%** (evidence §3.4,
  `scan-complaints.mjs --top 8 --query "swim workout generator"`).
- **ON RECORD** — the shelf's handoff is a **watch**: the positive review on
  `com.SwimTrainer.SwimTrainer` praises *"the ease of sending workouts to my Garmin Forerunner 955"*;
  no reviewed incumbent describes printing or sharing a set list (evidence §3.4).

The entry filter of the recon passes on the **leader's own users' words**: clauses 1 (one purchase, no
subscription) and 2 (guided pace test) each answer a critical review on the on-job leader itself, and
clause 3 (computed, not sliced) answers the same complaint on a second on-job incumbent with 2,771
reviews (**ON RECORD**, proposal §2.5).

### Honest unknowns carried into this change

- **UNVERIFIED** — that a swimmer will pay **$9.99 once** for a workout generator on a shelf where
  **0 of 10 enriched cards declare a price** and 7 of 10 monetize through recurring in-app purchases.
  There is no priced comparable; the deciding reading is 90 days of Play Console data via
  `node scripts/check-outcomes.mjs --slot laneset` (evidence §7 rows 19, 21).
- **UNVERIFIED** — that the string `Laneset` is free in **trademark class 9**. Only a Play storefront
  search was run (a 29-card page carried no app with the token); no register was queried. The
  deciding action is a USPTO / EUIPO / WIPO / Rospatent class 9 search before the first upload
  (evidence §5; `naming.json` `assumptionsUnverified`).
- **ON RECORD and recorded, not hidden** — the on-job leader is a 100K+ app **with a watch ecosystem,
  video and a coaching library**, and the shelf's best-rated app produced no measurable complaint. The
  wedge is price and pacing; it is not claimed to win on everything else (proposal §2.6, §8).

## Economics

Contract: `apps/2026-10-02-laneset/economics.json` (validated by `node
scripts/check-economics.mjs --slot laneset`).

- **Archetype:** `purchase_led_single_unlock`; the slot **requires** the `billing` capability.
- **`ads` is deliberately not enabled.** The rejected alternatives, each with its revenue reason, are
  in `economics.json` `archetypesRejected`: `purchase_led_repeat_sales` (the generator already answers
  every session, so a paid set pack would cannibalise the unlock); `ads_led_utility` (an interstitial
  on a pool deck at an assumed ~$2 eCPM needs ~500,000 impressions/month, ≈167,000 sessions/month,
  against a shelf whose leader has 3,102 reviews); `purchase_only_no_ads` (gives up the free session,
  which is the only moment the unlock can be sold).
- **Paying user:** a pool swimmer training 3–6 structured sessions a week in a masters or club lane,
  or a triathlete doing pool work, who builds the session by hand today or copies a coach's whiteboard.
- **What is bought:** one permanent unlock — every training focus, the guided 400/200 threshold pace
  test, the constraint-driven set builder, the coach-workout import and the share/print handoff. No
  ads, no account, no renewal.
- **Price:** **$9.99**, one time. **Moment:** on the pool deck, after the free Endurance session has
  been generated and swum once, at the first locked focus or the first tap on the handoff, behind one
  clearly priced sheet with **no trial clock and no subscription prompt**.
- **Arithmetic, with the division shown** (proposal §4.3):
  ```
  price                                        9.99
  Play service fee 15%                        -1.50   (9.99 x 0.15)
  refund allowance 2%                         -0.20   (9.99 x 0.02)
  currency conversion 2%                      -0.20   (9.99 x 0.02)
  ------------------------------------------------------------------
  net per sale                                 8.09   (9.99 x 0.81)

  target (docs/monetization-strategy.md):  1,000.00 per month
  sales/month  = 1,000 / 8.09               =  123.6  ->  124 sales/month
  sales/day    = 124 / 30                   =    4.1
  installs @ assumed 3% conversion          =  124 / 0.03 = 4,133 installs/month
  ```
  The 15% is the sub-$1M service-fee tier, so this is a floor. The **3% conversion is an assumption
  with no reading behind it** — no slot in this repository has measured one — and the 124 sales/month
  figure moves directly with it (**UNVERIFIED**, `economics.json` `assumptionsUnverified`).
- **Kill criteria:** `windowDays 90`, `minRevenueUsd 900`, `minInstalls 1250` — judged afterwards by
  `node scripts/check-outcomes.mjs --slot laneset`. The `900`/`1250` floor is the repository's
  **derived kill floor**, not the target: it applies the recorded $300/month lower anchor over the
  window and inherits the same 3% conversion the target assumes, which is more optimistic than the
  repository's recorded 1.9% median install-to-paid rate (**ON RECORD**, `economics.json`
  `assumptionsUnverified`).
- **Build estimate:** **6 agent hours** (`economics.json`), in the band of this factory's own recipes
  (3.5–6). The cost is in authoring the programming rules — pace zones, set shapes per focus,
  equipment recipes — not in the screens.

## Distinctness

*Where the shelf's leaders compute sets from a fixed rotating library and sell the useful half as a
recurring plan, this app computes each session from the swimmer's own constraints against authored
programming rules, unlocks once instead of every month, walks the swimmer through the two time trials
that turn every interval into a target time, and prints or shares the set list so the workout can be
swum without a watch, a signal or an account.* (**ON RECORD**, `economics.json`
`distinctnessSentence`.)

Every clause is a difference in **functionality or content**, not in naming or colour
(`docs/monetization-strategy.md` §1.2), and each answers a complaint quoted above from a named
incumbent's own user. The one clause deliberately **not** claimed is training history, which every
leader already ships (proposal §2.4 row 7; see non-goals).

## Internal-catalogue check (Repetitive Content)

Checked against `registry/apps.json` **`id` / `name` / `idea` only** — no slot tree under `apps/**`
other than `apps/2026-10-02-laneset` was opened (this session MEASURED `registry/apps.json:63-72`).

| Existing | Job | Verdict |
|---|---|---|
| `teacher-gradebook` (published) | private offline classroom record book — create/edit/delete grades, attendance, notes, CSV/PDF export | different category, different buyer, different data model (a **record**), different report audience |
| `lawn-round-log` (scaffolded) | offline solo-trade round log — ordered customers, one-tap visits, owed balances, CSV export | different category and buyer (a **field record**) |
| `pipe-sizing` (killed) | gas pipe sizing workbook | burned trade-calculator direction; the lane boundary this proposal tests against |
| `smoke`, `pipe-proof` | throwaway pipeline proofs | not products |

No slot in the catalogue computes a plan from user constraints. `laneset` is the catalogue's first
**computation** rather than a **record**: one sentence cannot describe both it and any existing slot,
which is the distinctness test Play's Spam policy reaches. The nearest catalogue neighbour by shape is
`teacher-gradebook` (both offline, both one unlock), and their jobs — a classroom record book and a
swim set computer — share no data model, no screen and no buyer.

## Non-goals

The recon excluded these because the shelf's leaders already do them, and the lane forbids the first
one. They are stated here so a later reader cannot mistake their absence for an oversight.

- **No training history, times, logs, charts or tracking.** Every leader keeps the swimmer's times and
  charts them, and this is also the notebook shape the planner lane forbids (proposal §2.4 row 7). The
  app computes a session and hands it over; it does not keep a record of what was swum. This is an
  honest cost, not a hidden one — a swimmer who wants a log keeps using the incumbent.
- **No video technique library and no coaching content.** MySwimPro ships video analysis and a whole
  teaching product exists at 4.7★/1,329 reviews. Cannot win; not attempted (proposal §2.4 row 8).
- **No challenges, streaks, social or motivational programme.** The leader already does it (proposal
  §2.4 row 9).
- **No learn-to-swim / stroke lessons.** The leader does it, better rated (4.7★, 1,329 reviews)
  (proposal §2.4 row 10).
- **No watch or wearable sync.** The shelf's handoff is a Garmin/Apple watch ecosystem the factory
  does not build; this app is phone-only and its handoff is text and print (proposal §2.6). Stated as
  a real loss for a swimmer who already owns a supported watch.
- **No account, server, cloud sync, analytics, camera, location, ML or AI surface.** The app is
  offline by construction; the programming rules and the pace arithmetic ship in the APK.
- **No ads, and no subscription at any point.** `subscription` is not in `ARCHETYPES` and is
  machine-refused; `ads` is deliberately not enabled.
- **No second paywall and no consumable product.** Exactly one non-consumable unlock.

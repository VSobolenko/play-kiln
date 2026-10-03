# Planners and optimisers — the measured readings behind the `laneset` proposal

Written 2026-10-02 by the recon scout role, for the lane "the app takes the user's constraints and
produces a plan, schedule, program or route computed from authored programming rules the app ships".
This file is the raw measurement record; `docs/ideation/2026-10-02-planners-and-optimisers.md` is the
proposal a human reads, and `docs/ideation/plans/laneset.*.json` are the three gate-ready contracts.

Every factual sentence below is labelled per `.cursor/rules/grounded-claims.mdc`: **MEASURED** (a
command was run in this session and its output is quoted), **ON RECORD** (a named document states it
and was read), **INFERRED**, or **UNVERIFIED** (with the command or file that would decide it).

**Read boundary observed:** nothing under `apps/**` was opened. The factory's own work was visible
only as `id` / `name` / `idea` in `registry/apps.json`, and only to check that the slug `laneset` and
the applicationId `com.gexetr.laneset` are free.

**Honest limit on every storefront reading here.** Install brackets are rounded floors from the
schema.org twin (`100K+` spans 100,000–999,999), so every size comparison is a floor comparison.
`scan-complaints.mjs` reads Play's own server-rendered sample, measured at three reviews per page, on
the **weakest-rated** incumbents for a query and on the named package for `--package`; a complaint
table therefore describes the **vocabulary** of a shelf and never complaint frequency in its
population. A leader with no critical review in a three-review sample is not a leader without
complaints — it is a leader whose complaints were not served. All of it is one signed-out storefront
session and is **UNVERIFIED** as a byte-for-byte reproducible reading.

---

## 1. Calibration controls — run in the same invocation as the lane queries

**MEASURED** — `node scripts/scan-market.mjs --query "habit tracker" ; node scripts/scan-market.mjs
--query "bjj training log" ; node scripts/scan-market.mjs --query "swim workout generator"` (one
shell invocation, three processes; the catalogue groups reprint on each process, which is why the
same trade-calculator table appears three times in the terminal).

| control query | expected | observed | why-string, verbatim |
|---|---|---|---|
| `habit tracker` | `fortress` | **`fortress`** | `18/29 rated incumbents are >= 4.5, and only 4/29 rated i…` |
| `bjj training log` | `dead-zone` | **`dead-zone`** | `only 9/29 incumbents are rated (0.31 < 0.35), so demand …` |
| `swim workout generator` | lane query | **`mixed`** | `demand proven, 5 weak incumbent(s), but 0.45 of rated ar…` |

Both controls returned their known verdicts, so the sensor did not drift on this invocation. The
third row is the lane query itself, screened by the same process that was calibrated.

---

## 2. The shelf screens — the final set measured

**MEASURED** — `node scripts/scan-market.mjs --json --query "hangboard training plan" --query
"rowing training plan" --query "crop rotation planner" --query "meal prep plan generator" --query
"apnea training plan" --query "study plan generator"`, plus the three separate invocations recorded
in §2.2.

| shelf query | verdict | rated | median of rated | command |
|---|---|---:|---:|---|
| `swim workout generator` | **mixed** | 11/30 | 4.40 | §1 invocation; table above |
| `hangboard training plan` | mixed | 21/30 | 4.40 | the six-query run |
| `rowing training plan` | candidate | 7/30 | 4.10 | the six-query run |
| `crop rotation planner` | candidate | 13/30 | 4.00 | the six-query run |
| `meal prep plan generator` | **fortress** | 13/30 | 4.60 | the six-query run |
| `apnea training plan` | dead-zone | 7/30 | 4.80 | the six-query run |
| `study plan generator` | candidate | 12/30 | 4.20 | the six-query run |
| `interview prep planner` | dead-zone | 7/30 (0.23) | — | `scan-complaints.mjs --query "interview prep planner" --top 6` |
| `college application planner` | candidate | 10/20 below 4.2 | — | `scan-complaints.mjs --query "college application planner" --top 6` |
| `sight reading practice plan` | mixed | 10/10 enriched | 4.55 | `enrich-market.mjs --top 10 --query "sight reading practice plan"` |

The five shelves that were screened but not carried into a full head-to-head (the four refused in §9
of the proposal plus the two dead zones) are reported there with the revenue reason each was refused
for, which is the requirement `.cursor/rules/ideation-gate.mdc` sets: **a rejection must name a
revenue reason, never a novelty reason.**

---

## 3. The swim shelf and its leaders

### 3.1 The shelf, front page

**MEASURED** — `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "swim workout
generator"` → 30 visible cards, verdict `mixed`, and this is the whole top 10 as printed:

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|--:|---|---|---:|---|---|---|---|---|
| 1 | `com.spiraledge.swimapp` | Swim.com: Workouts & Tracking | 3.5 | 1,300 (1,297) | 100K+ | false | false | free |
| 2 | `com.theswimsuccess.swimsuccess` | Swim Success: Swim Workout App | unrated | — | 5K+ | false | true | free |
| 3 | `com.myswimpro.android.app` | MySwimPro: Swim Workout App | 3.8 | 3,100 (3,102) | 100K+ | false | true | free |
| 4 | `io.swimup.application` | SwimUp - Swimming Training | 4.5 | 1,230 (1,234) | 100K+ | false | true | free |
| 5 | `com.fitivity.swimming` | Swim Strength Training | 5.0 | 109 (109) | 10K+ | true | true | free |
| 6 | `com.gizmodus.myswimcoach` | Swim Coach - Swim Training App | 4.6 | 2,770 (2,771) | 100K+ | false | true | free |
| 7 | `com.bastienlacave.natation` | Swim workout tracking | unrated | — | 10K+ | true | false | free |
| 8 | `com.swim.training` | Swim Training | unrated | — | 10K+ | false | true | free |
| 9 | `fr.openswim.mobile` | OpenSwim: swimming app | unrated | — | 50K+ | false | false | free |
| 10 | `learn.swimming.lessons.app.learning` | Swim Lessons: Learn to Swim | 4.7 | 1,330 (1,329) | 100K+ | false | true | free |

Enrichment summary, as printed: 7 of 10 declare an in-app purchase, 2 declare ads, 0 declare a price,
largest install bucket `100K+`.

### 3.2 Hand classification of the same page

**MEASURED** — `node scripts/scan-titles.mjs --query "swim workout generator" --on-job
"(swim|pool|lap|stroke|freestyle|natation|swimming|aquatic)" --off-job
"(run|bike|cycl|yoga|gym|fitness|water polo|diving|scuba|freediv)"` → `hand job-share: 28/30 (0.93)
vs guard on-topic`.

The regex is generous by construction — almost every title on this page contains the token `swim` —
so 28/30 is an upper bound on the job share and the guard's own `24/30 (0.80)` is the more
conservative reading. The discriminating reading is not the share but the **rating coverage**: the
page carries **28 cards that name the swim job and 15 of the 30 visible cards are unrated**, which by
`.cursor/rules/ideation-gate.mdc` is evidence of failure rather than vacancy. Two cards are worth
naming from the raw page: `app.swimset.companion` (SwimSet: Swim & Bike Workouts, unrated) and
`net.smartwod.workouts` (SmartWOD Workout Generator, 4.6, the criterion leader in §3.3).

### 3.3 The leaders, by the criterion stated before the data was read

The criterion, fixed in advance: run the enrichment, take the **highest install bucket**, break a tie
with the **highest exact review count** (the unrounded schema.org twin). Where the mechanical winner's
job is not the proposal's job, both a **criterion leader** and an **on-job leader** are named.

- **Criterion leader — MEASURED:** `net.smartwod.workouts` (SmartWOD Workout Generator, SmartWOD
  fitness apps), **500K+, 4.6★, 3,231 exact reviews, ads true, IAP true, free.** Measured by
  `node scripts/enrich-market.mjs --top 2 --concurrency 2 --query "smartwod workout generator"`, which
  also printed the sibling `net.smartwod.timer` (SmartWOD Timer) at **1M+, 4.8★, 31,373 reviews**. Its
  job is a **generic CrossFit-style WOD generator**, not swimming: `scan-titles.mjs` filed it under
  "UNMATCHED, classify by eye" on the swim page, and its own title names no swim, pool or stroke.
- **On-job leader — MEASURED:** `com.myswimpro.android.app` (MySwimPro: Swim Workout App), **100K+,
  3.8★, 3,102 exact reviews, ads false, IAP true, free.** It wins the on-job comparison because every
  card that names the swim job shares the `100K+` bucket and MySwimPro holds the highest exact review
  count in it. Runners-up, same bucket: `com.gizmodus.myswimcoach` (2,771) and
  `learn.swimming.lessons.app.learning` (1,329).

**The whole shelf prices at zero.** No card in the enriched top 10 declares a price, and the money is
in-app: 7 of 10 declare IAP. That is the willingness-to-pay reading for this proposal, and it is
simultaneously its largest honest gap (§7).

### 3.4 What the shelf's own users wrote

**MEASURED** — `node scripts/scan-complaints.mjs --top 8 --concurrency 2 --query "swim workout
generator"` → 24 reviews read, 14 critical, 0 with no text, 4 unclassified; pocket distribution
**missing-feature 36% (5 of 14), paywall 29% (4 of 14), broken 21% (3 of 14), crash 14% (2 of 14),
notifications 14% (2 of 14), account 7% (1 of 14), regression 7%, value 7%.** The named-app rows, as
printed:

| package | rating | installs | complaint classes | verbatim, truncated exactly as the sensor printed it |
|---|---:|---|---|---|
| `com.myswimpro.android.app` — **the on-job leader** | 3.8 | 100K+ | paywall 2, broken 1, missing-feature 1, value 1 | 1★ *"Swim workouts seem legit, however no real guidance to do the test workouts to get the dynamic pacing in place. Analysis is extra, 50, 100 or 150 bucks"* |
| `com.myswimpro.android.app` | 3.8 | 100K+ | (same app) | 1★ *"I understand having a premium option. But I was under the impression there was at least some stuff that was part of the free app usage (not the 7 day "* |
| `com.myswimpro.android.app` | 3.8 | 100K+ | (same app) | 3★ *"If you never swam competitively ever, or dont want the hassle of doing a quick google search, then this app is okay. However, as a past swimmer and cu"* |
| `com.SwimTrainer.SwimTrainer` | 2.7 | 10K+ | crash 1, missing-feature 1 | 1★ *"Was interested in having custom workouts ready to go without having to plan them myself. But if it sounds too good to be true, it always is. Workouts "* |
| `com.SwimTrainer.SwimTrainer` | 2.7 | 10K+ | (same app) | 2★ *"Really like the app and workout selection as well as the ease of sending workouts to my Garmin Forerunner 955. However, the workout I tried started my"* |
| `com.spiraledge.swimapp` | 3.5 | 100K+ | broken 1, missing-feature 1 | 2★ *"I was excited to use this app because it was said to be the best app for swimming, and i wanted to use the challenges as motivation. I don't have a wa"* |
| `com.tenklakessoftware.swimmetry` | 2.3 | 10K+ | notifications 2, paywall 2, account 1, broken 1 | 1★ *"I upgraded my phone and couldn't transfer my Swimmetry account, so I canceled my subscription. Since then, I haven't been able to log into the app, ev"* |
| `com.tenklakessoftware.swimmetry` | 2.3 | 10K+ | (same app) | 1★ *"There were two subscriptions (monthly or annually) options on the app. However when I tried to click on the annual subscription, it automatically flip"* |
| `com.SwimTrainer.SwimTrainer` | 2.7 | 10K+ | (same app) | 2★ *"The app worked great at first and I was able to use it with Garmin vivoactive 4. It records open swim or pool. Now it crashes every time I try to end "* |

The adjacent, better-rated leader's own complaint, read separately — **MEASURED** —
`node scripts/scan-complaints.mjs --package net.smartwod.workouts --package
com.gizmodus.myswimcoach`:

- **`com.gizmodus.myswimcoach` (Swim Coach, 4.6★, 100K+, 2,771 exact reviews), 2★:** *"for the most
  part of the app works pretty well It doesn't crash. in terms of the sets that the app generates are
  very repetitive and not very good for"* — classified `missing-feature`. **This is the single most
  load-bearing review in this file: a well-rated on-job incumbent, with 2,771 reviews, is criticised by
  its own user for exactly the mechanism this proposal replaces.**
- **`net.smartwod.workouts` (the criterion leader, off-job), 3★:** *"Common sense functionality not
  there. the overall app is decent. the workouts are great. but, you can't log/record a workout unless
  you run the whole "* — classified `missing-feature`. Its second critical, 2★, complains that a first
  workout was only manageable because instructions were misread. These are complaints about a generic
  WOD generator and carry no weight against a swim-specific wedge; they are recorded only because the
  criterion leader is the criterion leader.

---

## 4. Other named-app readings used by the proposal

**MEASURED** — the same two `scan-complaints` invocations as §3.4, plus the `enrich-market` run of
§3.1. Only facts already printed there are restated:

- `io.swimup.application` (SwimUp, **4.5★, 1,234 exact reviews, 100K+**, IAP): **0 critical** in the
  three-review sample. It is the shelf's best-rated app with real volume, and it is a fact against
  this proposal rather than for it.
- `learn.swimming.lessons.app.learning` (Swim Lessons, Riafy, **4.7★, 1,329 exact reviews, 100K+**,
  IAP): a **learn-to-swim teaching** app, adjacent job, not a workout generator.
- `com.theswimsuccess.swimsuccess` (Swim Success: Swim Workout App, **unrated**, 5K+, IAP) and
  `fr.openswim.mobile` (OpenSwim, **unrated**, 50K+): the on-job shelf carries high-install cards with
  **no rating at all**, which is the failure evidence §3.2 refers to.

---

## 5. The name screen, and what it actually covered

**MEASURED** — `node scripts/scan-titles.mjs --query "<token>"`, one call per token, run on
2026-10-02 from this machine:

| token | what the Play page showed | consequence |
|---|---|---|
| `laneset` | **no app titled Laneset.** The nearest tokens on a 29-card page were `Allsets: Gym Workout Planner` (`com.strongomatic.android`, unrated), `LiveSet` (`com.cognitechs.liveset`, unrated), `Everyset` (`com.castifi`, 3.9) | **free on this reading** — chosen |
| `blocksmith` | **`BlockSmith` exists**: `com.jerus.blocksmith` by After Silence Lab, unrated | eliminated by collision |
| `setline` | **`Setline` exists**: `com.setline.app` by Lukas Media, unrated | eliminated by collision |
| `laneforge` | no app named Laneforge, but the whole page is the `-Forge` game genre — `Mapforge`, `ArcForge`, `StoryForge`, `DreamForge`, `Auto Forge: Idle RPG Craft`, `Forge Master`, `Forgeland: idle simulator` | eliminated: the token space reads as idle-RPG games and the first result on the page is a game |

**What this screen is not.** It is a **Play search reading only**. No trademark register was queried —
not USPTO, not EUIPO / TMview, not WIPO Global Brand Database, not Rospatent — so whether `Laneset`
is free in class 9 is **UNVERIFIED**, and a clean Play search is not clearance: Play does not enforce
unique titles. The deciding command is a register query against class 9 (downloadable software), and
nobody has run it.

---

## 6. The arithmetic, with the division shown

Price **$9.99**, one-time. Deductions are the plan assumptions the factory carries
(`docs/monetization-strategy.md`), not measurements of this developer account.

```text
price                        9.99
Play service fee 15%         -1.50     (9.99 x 0.15 = 1.4985)
refund allowance 2%          -0.20     (9.99 x 0.02 = 0.1998)
currency conversion 2%       -0.20     (9.99 x 0.02 = 0.1998)
-------------------------------------------------------------
net per sale                  8.09     (9.99 x (1 - 0.15 - 0.02 - 0.02) = 9.99 x 0.81)

target, docs/monetization-strategy.md:  1,000.00 per month
sales per month to clear it:            1,000 / 8.09 = 123.6  ->  124 sales/month
per day:                                124 / 30           =  4.1 sales/day
installs implied at 3% conversion:      124 / 0.03         =  4,133 installs/month
```

The 15% tier is the sub-$1M service fee; above the first million dollars it rises, so the arithmetic
is a floor. The 3% install-to-unlock conversion is an **assumption with no reading behind it** — no
slot in this repository has ever measured a conversion rate.

**Kill criteria, derived from the target rather than chosen beside it.** The window is the first 90
days, the cold-start band for a listing with zero ratings. A quarter of the target run-rate sustained
across that window is the continue-or-kill line: a listing that has reached it has shown the ramp is
real, and one that has not will not reach $1,000/month from a standing start. Scaling the same quarter
through the division above:

```text
minRevenueUsd  = 1,000 x 3 months x 0.25 =   750
minInstalls    = 4,133 x 3        x 0.25 = 3,099.75  -> 3,100
windowDays     =                            90
```

The quarter factor is **INFERRED** from the shape of the ramp, not measured; it is stated in the
proposal's honest unknowns rather than hidden inside the numbers.

---

## 7. Claim ledger

`CLAIM / LABEL / EVIDENCE`.

| # | Claim | Label | Evidence |
|--:|---|---|---|
| 1 | `habit tracker` returns `fortress` and `bjj training log` returns `dead-zone` on the calibrated invocation | MEASURED | `node scripts/scan-market.mjs --query "habit tracker" ; node scripts/scan-market.mjs --query "bjj training log" ; node scripts/scan-market.mjs --query "swim workout generator"` |
| 2 | `swim workout generator` returns `mixed`, 11/30 rated, 5 weak, median 4.40 | MEASURED | same invocation, ad-hoc group |
| 3 | The swim shelf's top 10 is 7/10 IAP, 2/10 ads, 0/10 priced, largest bucket `100K+` | MEASURED | `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "swim workout generator"`, summary block |
| 4 | Criterion leader is `net.smartwod.workouts`: 4.6★, 3,231 exact reviews, 500K+, ads true, IAP true | MEASURED | `node scripts/enrich-market.mjs --top 2 --concurrency 2 --query "smartwod workout generator"`, row 1 |
| 5 | The criterion leader's job is a generic WOD generator, not swimming | MEASURED | `scan-titles.mjs --query "swim workout generator"` filed it "UNMATCHED, classify by eye" |
| 6 | On-job leader is `com.myswimpro.android.app`: 3.8★, 3,102 exact reviews, 100K+, IAP true, ads false | MEASURED | the §3.1 enrich, row 3 |
| 7 | Hand job-share on the swim page is 28/30 (0.93) against a generous `swim` regex, and 15 of 30 cards are unrated | MEASURED | `scan-titles.mjs --query "swim workout generator" --on-job "(swim\|…)" --off-job "(run\|…)"` |
| 8 | The on-job leader's own critical reviews are 2 paywall, 1 broken, 1 missing-feature, 1 value, and one of them reports no guidance for the pacing test | MEASURED | `scan-complaints.mjs --top 8 --concurrency 2 --query "swim workout generator"` |
| 9 | A well-rated on-job incumbent with 2,771 reviews is criticised by its own user for repetitive generated sets | MEASURED | `scan-complaints.mjs --package net.smartwod.workouts --package com.gizmodus.myswimcoach` |
| 10 | The swim pocket's 14 critical reviews split missing-feature 36%, paywall 29%, broken 21%, crash 14% | MEASURED | the `--top 8` run, pocket block |
| 11 | The shelf's handoff is a watch: a positive review names sending workouts to a Garmin Forerunner 955 | MEASURED | same run, `com.SwimTrainer.SwimTrainer` 2★ |
| 12 | `io.swimup.application` (4.5★, 1,234 reviews, 100K+) has 0 critical in its three-review sample | MEASURED | the §3.1 enrich plus the `--top 8` run |
| 13 | `laneset` has no app carrying the token on a 29-card Play page; `BlockSmith` and `Setline` are live apps | MEASURED | `node scripts/scan-titles.mjs --query "laneset"`, and the same for `blocksmith` and `setline` |
| 14 | The `-Forge` token space on Play is dominated by idle-RPG games | MEASURED | `node scripts/scan-titles.mjs --query "laneforge"` |
| 15 | `meal prep plan generator` is a `fortress` (13/30 rated, median 4.60) and `apnea training plan` is a `dead-zone` (7/30 rated, median 4.80) | MEASURED | the six-query `scan-market.mjs --json` run |
| 16 | `sight reading practice plan` carries a 5M+ fortress: `com.binaryguilt.completemusicreadingtrainer` 4.6★, 12,248 reviews, and `com.evilduck.musiciankit` 4.6★ over 86,279 at 5M+ | MEASURED | `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "sight reading practice plan"` |
| 17 | `interview prep planner` is a `dead-zone` — 7/30 rated (0.23) | MEASURED | `scan-complaints.mjs --query "interview prep planner" --top 6` |
| 18 | MySwimPro's own reviewer names a subscription price of 50, 100 or 150 dollars and a 7-day free window | MEASURED | row 8's verbatim quotes |
| 19 | A swimmer will pay $9.99 once for a workout generator rather than using a free incumbent | UNVERIFIED | no priced comparable exists on this shelf — 0 of 10 enriched cards declare a price; the deciding reading is a Play Console result via `node scripts/check-outcomes.mjs` |
| 20 | `Laneset` is free in trademark class 9 | UNVERIFIED | no register was queried; the deciding action is a USPTO / EUIPO / WIPO / Rospatent class 9 search |
| 21 | The flagship figure of 124 sales per month rests on a 3% install-to-unlock conversion | UNVERIFIED | no slot in this repository has measured a conversion rate; the deciding reading is 90 days of Play Console data |
| 22 | A niche utility cannot reach $1,000/month on ads | INFERRED | from an assumed $2 eCPM requiring roughly 500,000 impressions/month at about 3 per session; the eCPM is an assumption, not a reading of this account |
| 23 | The shelf's storefront copy quoted above is what Play served this session | UNVERIFIED | one signed-out `hl=en`, `gl=US` session; not a Play Console reading and not reproducible byte-for-byte |

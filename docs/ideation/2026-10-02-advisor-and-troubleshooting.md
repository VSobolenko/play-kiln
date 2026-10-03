# Advisor / troubleshooter lane — `pool-diagnosis` (proposal)

Recon round for the **ADVISOR / TROUBLESHOOTER** lane, run 2026-10-02 on this machine against
signed-out Play (`hl=en`, `gl=US`). One recipe is proposed. Everything factual below is labelled per
`.cursor/rules/grounded-claims.mdc` and the full measurement record is
`docs/research/2026-10-02-advisor-and-troubleshooting-evidence.md`; the claim ledger is §13 here.

**Read boundary:** the market was read, never our own work. No file under `apps/**` was opened.

---

## 1. The recipe in one sentence

**`pool-diagnosis`** is an offline advisor that takes the water readings a pool or hot-tub owner
actually has — coarse strip colour bands, including the in-between colours their current app refuses —
and returns a **ranked set of probable causes with the reasoning and the one test that would confirm
each, followed by an ordered corrective plan with the amount of each step and what not to mix**, from
an authored rule base that ships in the APK. It is not a log, not a counter, and not a calculator the
user types numbers into; the value is the judgement it hands back.

- **Price:** $5.99, one-time unlock (`purchase_led_single_unlock`).
- **Capability:** `billing` only; ads deliberately off.
- **Server:** none.
- **Build estimate:** 7 agent hours.

---

## 2. The shelf and its primary query

- **Shelf:** pool / hot-tub water chemistry advice.
- **Primary query:** `pool chemistry advisor`. Second audience via `hot tub water chemistry`, which
  resolves to the *same* leader (its #2 card), so it is a second audience, not a second recipe.
- **Calibration controls, run in the same invocation as the lane query (MEASURED):** `habit tracker`
  → `fortress` and `bjj training log` → `dead-zone`, both matching their known answers, so the sensor
  had not drifted. `pool chemistry advisor` → `mixed`, guard `ok`, 30 seen, 16/30 rated, 4 weak,
  median 4.45.
- **The guard verdict is a spot check, not evidence (MEASURED).** Hand-classified job share of the
  same page is **19/30 (0.63)** while the guard says `on-topic`, because the word *chemistry* pulls in
  chemistry-solver, periodic-table and news apps — the documented homonym blindness
  (`docs/research/2026-09-24-market-decision.md` §2.1). The hand classification is the reading used.

## 3. The leader-first screen

### 3.1 The leader, by the criterion stated before the data was read

**MEASURED**, `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "pool chemistry advisor"`:

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|--:|---|---|---:|---|---|---|---|---|
| 1 | `com.coachplatform.poolcoach` | PoolCoach: Chemistry Advisor | unrated | — | 50+ | false | true | free |
| 2 | **`com.hthpools.tester.release`** | **HTH™ \| Test to Swim™** | **4.6** | **1,050 (1,052)** | **100K+** | **false** | **false** | **free** |
| 3 | `com.poolchemicalcalculator.poolcalc` | Pool Chemical Calculator | 3.6 | 210 | 50K+ | **true** | **true** | free |
| 4 | `app.Pooli` | Pooli | 4.4 | 333 | 50K+ | false | false | free |
| 5 | `com.elitecrest.orenda` | Orenda | 4.6 | 446 | 50K+ | false | false | free |
| 6 | `com.poolbyengineer.chemicallog` | Pool Chemical Logbook | unrated | — | 100+ | false | true | free |
| 7 | `com.psiflow.poolcalculator` | Pool-Calculator | 4.4 | 220 | 10K+ | false | false | free |
| 8 | `com.zoryatechstudio.poolchemistry` | Pool Chemistry Calculator | unrated | — | 0+ | true | false | free |
| 9 | `com.troublefreepool.poolmath` | Pool Math by TFP | 3.0 | 392 | **100K+** | false | **true** | free |
| 10 | `com.viksva.chemai` | Chemistry AI & Periodic Table | unrated | — | 5K+ | false | true | free |

- **Criterion leader:** `com.hthpools.tester.release` — the highest install bucket is **100K+**, shared
  with `com.troublefreepool.poolmath`; HTH wins the tie on the unrounded review count, **1,052** v
  **392**. **4.6★, 1,050 exact reviews, 100K+ installs, free, ads false, IAP false.**
- **On-job leader:** the same app. HTH's job *is* pool water chemistry — it scans a strip and returns a
  suggested dosage. There is **no criterion-leader / on-job-leader split** on this shelf.
- **The shelf is not free-empty:** the second on-job giant, `com.troublefreepool.poolmath`, is **100K+ /
  3.0★ / IAP true**; `com.poolchemicalcalculator.poolcalc` is **50K+ / 3.6★ / ads + IAP**; and the weak
  downloaded tier is real (`com.biolabinc.pooltime`, **1.8★, 50K+ installs**).
- **An unrated incumbent is evidence of failure, not vacancy:** 3 of the top 10 cards are unrated,
  including the #1 card (50+ installs) and the #8 card (declared 0+ installs).

### 3.2 What the leader's own users complain about — quoted (MEASURED)

`node scripts/scan-complaints.mjs --package com.hthpools.tester.release --package com.troublefreepool.poolmath --package com.poolchemicalcalculator.poolcalc --package com.biolabinc.pooltime`.
Play serves its own sample; measured at **3 reviews per page**, so these describe the **vocabulary** of
the shelf, never complaint frequency.

**The leader, `com.hthpools.tester.release` (4.6★, 100K+, 1,050 reviews) — 3 read, 3 critical:**

- 1★ (2026-07-11): *"…the 'suggested dosage' of chemicals you're supposed to add [is off]… the app told
  me to add **34 pounds of ph down** to fix high alkalinity. I spent over **$100** on chemicals to do
  that…"*
- 1★ (2026-05-25): *"…you never really know the true levels of your pool as **colors may fall in
  between**… the app only says **analysis unsuccessful**… simply doesn't work."*
- 2★ (2026-09-11): *"…it insisted on my entering my spa size **in feet**. like it was a pool. So I had to
  enter ridiculous numbers to get the closest I could to my spa's capacity which is **429 gal**."*

**The shelf's money engines:**

- `com.troublefreepool.poolmath` (100K+, 392): *"Unless you pay a subscription, you can only keep a log
  of **ONE day**… 'You must subscribe'…"* (×2, `paywall`).
- `com.poolchemicalcalculator.poolcalc` (50K+, 210): *"you are forced to watch a video ad **EVERY SINGLE
  TIME** you use any of the calculators"*; *"it forces me to create an account…"* (`ads` ×2, `account`).
- `com.biolabinc.pooltime` (50K+, **1.8★**): *"…there are **no recommendations** based on my values"*;
  *"…that's not very helpful if it **doesn't also tell you how to fix it**."*

### 3.3 Clause-by-clause: each wedge clause against the leader's measured facts

| Wedge clause (weight) | Leader's measured facts | Verdict |
|---|---|---|
| **Ranked diagnosis with the mechanism and the confirming observation** (`ranked-diagnosis-with-reasoning`, core) | Leader's own 1★: the dose "is off", it said add **34 lb** and the user spent **$100**; another 1★: "analysis unsuccessful" on in-between colours. | **Leader criticised — wedge survives (strongest clause)** |
| **The ordered plan explains itself, step by step, with what not to mix and what to re-test** (`ordered-plan-explains-itself`, core) | Leader gives an uncheckable "suggested dosage" (the $100 review). Ordering half **not** directly criticised on the leader; only a 50K+ mid-tier is told it "doesn't also tell you how to fix it". | **Leader criticised for the uncheckable recommendation; ordering half SILENT on the leader (lands on the mid-tier)** |
| **Works from the bands and observations the owner actually has** (`works-from-observed-bands`, supporting) | Leader's own 2★ (spa): it "insisted on my entering my spa size **in feet**… ridiculous numbers"; 1★: "analysis unsuccessful" for in-between colours. | **Leader criticised — wedge survives** |
| **The plan leaves the app as text/PDF** (`plan-leaves-the-app`, supporting) | Leader **silent** — no served complaint about export; the related ask is a 50K+/1.8★ mid-tier wanting a "shopping cart" of fixes. | **Leader silent — a difference, not a leader-facing wedge** |
| **No ad, no account, no subscription, one unlock** (`no-ads-no-account-one-unlock`, supporting) | Leader **already satisfies it** (ads false, IAP false, no account complaints). Complaints live on `poolcalc` (ads every use, forced account) and `poolmath` (subscription). | **Leader already does it — NOT a wedge against the leader; a wedge against the shelf's money engines** |

### 3.4 Entry filter — passed

**At least two core clauses answer complaints the leader's own users wrote, verbatim:**
`ranked-diagnosis-with-reasoning` and `ordered-plan-explains-itself`, plus one more
(`works-from-observed-bands`) answering the leader's spa and "analysis unsuccessful" complaints. The
two clauses the leader already satisfies or is silent about (no-ads, export) are recorded as such and
**not** counted toward the filter.

**Verdict: PLAUSIBLE.**

**The one thing that would have to be true:** a pool owner who already has a free 4.6★ leader must
value a **checkable** diagnosis — the reasoning and the confirming test — enough to pay $5.99 once,
rather than accepting the leader's uncheckable dose. The evidence that this is plausible is the
leader's own $100 review; the reading that would settle it does not exist on this shelf and is listed
as an honest unknown (§12).

### 3.5 What the leader has that we do not

1. **A chemical manufacturer's free 100K+ funnel and a 4.6★/1,050-review trust floor.** HTH can be free
   with no IAP because the app sells the chemicals it recommends; a new listing has zero installs, zero
   reviews and no chemical revenue to subsidise free.
2. **A physical strip-scanning pipeline tied to its own product** and an in-store loop (the app ships on
   the strip bottle). Its users complain the scanner misreads, but it is still a capability we are
   deliberately not building, and it is why the card is installed at all.
3. **A second on-job giant on the shelf:** `com.troublefreepool.poolmath` (100K+, 392, the TroubleFreePool
   community's own app with a subscription) — so the leader is not the only app to beat.

---

## 4. Willingness to pay (MEASURED)

- **5 of the 10 enriched cards declare an in-app purchase; 2 of 10 declare ads**, across install buckets
  from 50+ to 100K+.
- The paying behaviour is **named by the incumbents' own users**: Pool Math's two served critical reviews
  are both about a subscription paywall at **100K+ installs**, and Pool Chemical Calculator's critical
  reviews name a video ad on every use and a forced account at **50K+ installs**.
- **Honest limit:** the Play card shows `free` with `IAP: true` and does **not print an IAP price**, so no
  explicit dollar figure is measured on this shelf. What is measured is that two incumbents at 50K+ and
  100K+ installs charge, and that their users describe the charge. The nearest explicit price in the
  wider pull is a comparable shelf's paid app (`com.wiselogic.aquarimate`, *"the $10 you'll spend on
  it"*).

---

## 5. What is bought, and when

- **Paying user:** a private pool or hot-tub owner (US/RU) who has a test result they cannot act on.
- **Paid unit:** one permanent unlock of the whole diagnosis — the full authored rule base, every ranked
  diagnosis with its mechanism and confirming test, and the ordered corrective plan, exported as
  text/PDF; no ads, no account, no renewal.
- **Price:** $5.99, one-time.
- **Moment of payment:** after a first free diagnosis has shown the ranked reasoning and the confirming
  test on the owner's own readings, at the point the ordered corrective plan is opened, behind one
  clearly priced sheet with no trial clock and no subscription prompt.

## 6. Archetype, and the alternatives rejected

**Chosen: `purchase_led_single_unlock`** → requires the `billing` capability.

| Rejected archetype | Revenue reason |
|---|---|
| `purchase_led_repeat_sales` | Nothing to re-sell. One diagnosis per test session against a static rule base; a consumable would sell storage (the record-book shape this lane forbids) and cannibalise the unlock. |
| `ads_led_utility` | Advertising **is** the measured defect: `poolcalc` (50K+) is told the user must watch a video ad every single time. A new listing with no install velocity would carry that defect while the free leader outranks it. |
| `purchase_only_no_ads` | All 10 enriched cards list at zero and 5 declare an IAP, so the shelf's proven funnel is free-with-in-app-purchase. A listing price spends that funnel at the moment the app has no reviews to justify it. |
| `subscription` | **Banned outright** (owner, 2026-10-01) and machine-enforced — `subscription` is not in `ARCHETYPES`. Named only because the 100K+ community incumbent's money is exactly a subscription, which is the rent this one-price recipe replaces. |

## 7. Unit arithmetic, with the division shown

Target: **$1,000/month** — the factory target in `docs/monetization-strategy.md` (**ON RECORD**).

```text
price                                                $5.99
Play service fee 15%          5.99    × 0.85  = $5.0915
refund allowance  2%          5.0915  × 0.98  = $4.98967
currency/FX       2%          4.98967 × 0.98  = $4.88988   ≈ $4.89 net per sale
sales/month to clear the target    1000 ÷ 4.88988 = 204.5  → 205 unlocks/month
check                              205 × 4.88988 = $1,002.42 ≥ $1,000
implied installs at the assumed 2% install→unlock conversion
                                   205 ÷ 0.02 = 10,250 installs/month

kill floor, DERIVED from a measured floor rather than chosen beside the target
  the weakest DOWNLOADED incumbent (com.biolabinc.pooltime, 1.8★) has 50K+ installs
  4% of that install floor over 90 days         = 2,000 installs
  2,000 × 2% conversion = 40 unlocks;  40 × 4.88988 = $195.60  → recorded floor $200
```

The 15% / 2% / 2% deductions are plan assumptions from `docs/monetization-strategy.md`, not measurements
of this developer account; the 2% conversion is an assumption with no measurement anywhere in the
repository. Both are listed as honest unknowns.

## 8. The reasons this app can win — the promise contract

Full contract: `docs/ideation/plans/pool-diagnosis.differentiators.json` (gate: PASS).
Six promises, **two `core`**, every one starting `deferred`:

| id | weight | jobStage | depthFamily | one-line |
|---|---|---|---|---|
| `ranked-diagnosis-with-reasoning` | core | during | core_operation | Ranked causes with the reason each and the one test that confirms it. |
| `ordered-plan-explains-itself` | core | during | core_operation | An ordered plan: each step's amount, reason, conflict warning and re-test. |
| `works-from-observed-bands` | supporting | before | fit | Accepts in-between bands and spa-sized volumes instead of demanding exact numbers. |
| `plan-leaves-the-app` | supporting | after | **data_out** | Take the diagnosis and plan out as text/PDF (**the handoff**). |
| `remembers-the-pool-profile` | supporting | before | durability | The pool's own facts are kept between sessions. |
| `no-ads-no-account-one-unlock` | supporting | during | trust | No ads, no account, one price, no renewal. |

**Job coverage** (`jobCoverage`): `before` → `works-from-observed-bands` · `during` →
`ranked-diagnosis-with-reasoning` (never refusable) · `after` → `plan-leaves-the-app`. The **connective
floor** is satisfied by `plan-leaves-the-app` (`data_out`: `ACTION_SEND` / `ACTION_CREATE_DOCUMENT` /
`PdfDocument`), and `remembers-the-pool-profile` (`durability`) adds a second handoff.

## 9. The name

Full contract: `docs/ideation/plans/pool-diagnosis.naming.json` (gate: PASS). **Chosen title: `Pool
Diagnosis`** (14 chars, 16 of Play's 30 of slack), slug `pool-diagnosis`, applicationId
`com.gexetr.pooldiagnosis`, archetype `descriptive`, score **84/100**.

Eight candidates spanning six archetypes were scored, including two negative controls (`Pool Chemistry
Calculator` at 55 and the bare noun `Calculator` at 38). Eight carriers were **auditioned and eliminated
by a measured collision** before the field settled on a descriptive phrase: AquaCheck (the strip brand
the leader ships with), Litmus (a live Play app), Brine, Atoll, Clearwater, Bluewater, Waterwright and
`Pool Water Advisor` (the shelf's own first card is "PoolCoach: Chemistry Advisor"). None of the live
neighbour titles uses the word *diagnosis*. The cost — a descriptive phrase is weakly protectable — is
recorded in the contract's honest unknowns.

## 10. Build estimate, permissions, capabilities, server

- **Build estimate:** 7 agent hours (the rule base and the ranking/plan rendering are the work; nothing
  is generated or scanned).
- **Permissions:** **none.** No internet, location, camera, storage, contacts, microphone or background
  work; the only network path is the billing/restore flow's `com.android.vending.BILLING`.
- **Capabilities:** `billing` required; `ads` deliberately not enabled.
- **Server:** **not required** — the rule base, ranking logic and every plan ship in the APK. Free
  capability with no backend; say so plainly because a server would make this a server project.

## 11. Visual direction (one or two sentences)

A **water-chemistry instrument**, not a logbook: a deep cyan-to-slate tonal field for the water, a single
accent used only for "act on this" (the ranked cause and the confirmation test), and a slot-owned
component that renders a **reading as a band on a scale with the diagnosis beneath it** rather than a
stock `Card` of numbers — plus a slot-owned **ordered-step ladder** that makes "what not to mix" a visual
constraint between steps, not a paragraph. The mark is a drop-shaped probe over a ruled scale, drawn at
48dp and inside the adaptive-icon safe zone, generated from `store/studio.json`. (Distinct from every
other slot's identity.)

## 12. Kill criteria and honest unknowns

**Kill criteria (three finite numbers):** `windowDays` **90**, `minInstalls` **2,000**,
`minRevenueUsd` **200** — the floor is derived above from the shelf's weakest downloaded incumbent rather
than chosen beside the target. Judged post-ship by `node scripts/check-outcomes.mjs --slot pool-diagnosis`.

**Honest unknowns** (also in `economics.json`): willingness to pay $5.99 outright is **UNVERIFIED** (no
printed IAP price on this shelf); the 2% install→unlock conversion is **UNVERIFIED**; the 15/2/2%
deductions are plan assumptions; the complaint profile is a **3-per-page** Play sample and the leader
remains 4.6★/1,050; and search ranking against a 100K+/1,050-review leader is assumed, not shown.

## 13. Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| `habit tracker` → `fortress`, `bjj training log` → `dead-zone`, `pool chemistry advisor` → `mixed` (16/30 rated, median 4.45) | MEASURED | `node scripts/scan-market.mjs --query "habit tracker" --query "bjj training log" --query "pool chemistry advisor" --out .kiln/scratch/scout-advisor-calibration.md` |
| Leader is `com.hthpools.tester.release`: 4.6★, 1,050 (1,052), 100K+, ads false, IAP false, free | MEASURED | `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "pool chemistry advisor"`, row 2 |
| `poolmath` 3.0★/392/100K+/IAP true; `poolcalc` 3.6★/210/50K+/ads+IAP; `pooltime` 1.8★/50K+ | MEASURED | same command and `node scripts/scan-complaints.mjs --package …` |
| 5 of 10 cards declare IAP; 2 of 10 declare ads; largest bucket 100K+ | MEASURED | same enrich command, summary block |
| Hand job-share is 19/30 (0.63) while the guard says on-topic | MEASURED | `node scripts/scan-titles.mjs --query "pool chemistry advisor" --on-job … --off-job …` |
| The leader's 3-read sample is 3 critical: the 34-lb/$100, the "analysis unsuccessful", the spa-feet review | MEASURED | `node scripts/scan-complaints.mjs --package com.hthpools.tester.release …` |
| `pooltime` (1.8★, 50K+) says "there are no recommendations based on my values" | MEASURED | same command |
| `poolmath` critical reviews are `paywall` ("You must subscribe", "one day") | MEASURED | same command |
| `poolcalc` critical reviews name a video ad every use and a forced account | MEASURED | same command |
| Eight carrier auditions each collide with a live app/brand (Litmus, Sonde, Lido, Atoll, Clarifier, Brine, Shoal, plus AquaCheck) | MEASURED | `node scripts/scan-titles.mjs --query "Litmus" --query "Sonde" --query "Lido" --query "Atoll"` and `--query "Clarifier" --query "Brine" --query "Shoal" --query "Skimmer"` |
| A pool owner will pay $5.99 once for a diagnosis they can check, on a shelf whose leader is free | **UNVERIFIED** | no priced comparable on this shelf; the deciding reading is Play Console via `node scripts/check-outcomes.mjs --slot pool-diagnosis` |
| Install→unlock conversion is 2% | **UNVERIFIED** | `store/outcomes.json` after launch; no conversion has ever been measured in this repository |
| The storefront copy quoted is the copy Play served this session | **UNVERIFIED** | one signed-out session; not a Console reading, not reproducible byte-for-byte |

## 14. Refusals from this round, each with its revenue reason

| shelf | revenue reason for refusal |
|---|---|
| `aquarium water chemistry app` | The paid incumbents sell **tank management and cloud storage** (the record-book shape this lane forbids): `com.wiselogic.aquarimate` 2.6★ ("the $10 you'll spend on it"), `aquaticlog.mobile.android` 4.0★ ("$29.99 a year"). The leader `com.kosick.aquadiary.android` (4.6★/1,812/100K+) served only 1 of 3 critical, an `abandoned` complaint — no leader complaint about wrong or unexplained advice to build a wedge on. No measured complaint makes an *advisor* the thing to buy. |
| `car warning light meaning` | The shelf's own user names the ceiling: *"I might have been willing to throw a buck or two down for an ad free version, but I fail to see the point when the info is so readily available using a Google search."* A one-time lookup with a free substitute at a "buck or two" is a revenue refusal. |
| `appliance error code lookup` | 8 of the 10 enriched cards are **unrated** (10+…10K+); the only 100K+ appliance card is a free code list with ads, and the 500K+ card is an **auto**-code app. An unrated incumbent is evidence of failure, not vacancy — no demand reading to price. |
| `3d printing troubleshooting` | The shelf is a hardware ecosystem led by Creality Cloud (4.4★/36,321/5M+) with free model libraries; no priced troubleshooting incumbent is measured. A new listing would pay to answer what the printer's own app answers free. |

No recipe is proposed for any of the four. Each is a **revenue** reason, not a novelty reason.

---

## 15. How to scaffold it if the owner approves

```bash
node scripts/new-slot.mjs \
  --slug pool-diagnosis \
  --economics docs/ideation/plans/pool-diagnosis.economics.json \
  --differentiators docs/ideation/plans/pool-diagnosis.differentiators.json \
  --naming docs/ideation/plans/pool-diagnosis.naming.json
```

The proposal is the decision the owner is being asked to make. It is not started until he says so.

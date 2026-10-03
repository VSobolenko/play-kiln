# Advisor / troubleshooter lane — the measured evidence

Written 2026-10-02 by the recon scout for the ADVISOR / TROUBLESHOOTER lane. This file is the
`evidenceRef` the proposal and the three plan contracts point at. Every factual claim is labelled per
`.cursor/rules/grounded-claims.mdc`: **MEASURED** (a command was run in this session and its output is
at the quoted path), **ON RECORD** (a named repository document states it and was read), **INFERRED**,
or **UNVERIFIED** (with the command or file that would settle it).

Read boundary observed: nothing under `apps/**` was opened. The factory's own output was read only as
the identity fields of `registry/apps.json` and the slug list of `docs/ideation/plans/`, and neither
touched the market reading below.

All Play reading is signed-out Play (`hl=en`, `gl=US`) through `scripts/lib/play-scan.mjs` and
`scripts/lib/play-app-detail.mjs`, unmodified, on 2026-10-02. Install counts are Play's rounded
buckets and are lower bounds; exact review counts are the unrounded schema.org twin the enricher
prints beside the rounded one.

---

## 1. Calibration controls, run in the same invocation as the lane query

```text
node scripts/scan-market.mjs --query "habit tracker" --query "bjj training log" --query "pool chemistry advisor" --out .kiln/scratch/scout-advisor-calibration.md
```

**MEASURED** (that command, printed table):

| control query | expected | observed | verdict |
|---|---|---|---|
| `habit tracker` | `fortress` | `fortress` | **matches** |
| `bjj training log` | `dead-zone` | `dead-zone` | **matches** |
| `pool chemistry advisor` | (lane query) | `mixed`, guard `ok`, 30 seen, 16/30 rated, 4 weak, median 4.45 | reads as demand-proven with a weak minority |

No sensor drift. The two controls are known answers used to detect drift, never candidates.

## 2. The lane's final query set, and why the primary query is the pool shelf

Eighteen candidate queries were screened across the lane's shelves in this round. The ones that
returned a real, downloaded incumbents' complaint profile on the **described-situation → recommended
action** job were:

| query | verdict | why it survived or died |
|---|---|---|
| `pool chemistry advisor` | `mixed`, guard `ok` | **chosen.** The leader's own critical reviews name a wrong, uncheckable dose; the weak downloaded tier names the missing corrective plan. |
| `hot tub water chemistry` | `mixed`, guard `partial` (15/29) | the **same** shelf: the same leader (`com.hthpools.tester.release`) is its #2 card. Folded in as the recipe's second audience, not a second recipe. |
| `aquarium water chemistry app` | `mixed` | refused; see §9. |
| `car warning light meaning` | `candidate` | refused; see §9. |
| `appliance error code lookup` | `mixed` | refused; see §9. |
| `3d printing troubleshooting` | `mixed`, guard `partial` (12/30) | refused; see §9. |

**Relevance guard is a spot check, not evidence.** For the primary query the guard returned `ok`
(on-topic) while my hand classification of the same page is **19/30 (0.63)** — below the 0.65 the
guard's own report flags — because "chemistry" pulls in chemistry-solver, periodic-table and
news apps (`node scripts/scan-titles.mjs --query "pool chemistry advisor" --on-job
"\b(pool|spa|hot ?tub|water|chem|alkalin|chlorin|ph)\b" --off-job
"\b(hud|speedometer|ambient|widget|periodic|mol(e|ecule)|game|puzzle)\b" --out
.kiln/scratch/scout-advisor-pool-titles.md` — **MEASURED**). This is the documented homonym blindness
(`docs/research/2026-09-24-market-decision.md` §2.1): the query's own word "chemistry" is a school
subject. The on-job cards are hand-classified below.

## 3. The leader, by the criterion stated before the data was read

```text
node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "pool chemistry advisor" --out .kiln/scratch/scout-advisor-pool-enrich.md
```

**MEASURED** (that command, top 10 as printed):

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|--:|---|---|---:|---|---|---|---|---|
| 1 | `com.coachplatform.poolcoach` | PoolCoach: Chemistry Advisor | unrated | — | 50+ | false | true | free |
| 2 | **`com.hthpools.tester.release`** | **HTH™ \| Test to Swim™** | **4.6** | **1,050 (1,052)** | **100K+** | **false** | **false** | **free** |
| 3 | `com.poolchemicalcalculator.poolcalc` | Pool Chemical Calculator | 3.6 | 210 (210) | 50K+ | true | true | free |
| 4 | `app.Pooli` | Pooli | 4.4 | 333 (333) | 50K+ | false | false | free |
| 5 | `com.elitecrest.orenda` | Orenda | 4.6 | 446 (446) | 50K+ | false | false | free |
| 6 | `com.poolbyengineer.chemicallog` | Pool Chemical Logbook | unrated | — | 100+ | false | true | free |
| 7 | `com.psiflow.poolcalculator` | Pool-Calculator | 4.4 | 220 (220) | 10K+ | false | false | free |
| 8 | `com.zoryatechstudio.poolchemistry` | Pool Chemistry Calculator | unrated | — | 0+ | true | false | free |
| 9 | `com.troublefreepool.poolmath` | Pool Math by TFP | 3.0 | 392 (392) | 100K+ | false | true | free |
| 10 | `com.viksva.chemai` | Chemistry AI & Periodic Table | unrated | — | 5K+ | false | true | free |

- **Criterion leader (MEASURED, that command):** `com.hthpools.tester.release` — the highest install
  bucket is **100K+**, shared with `com.troublefreepool.poolmath`; HTH wins the tie on the unrounded
  review count, **1,052** against **392**. **4.6★, ads declared false, IAP declared false, free.**
- **On-job leader:** the same app. HTH's job *is* pool water chemistry — its own listing and reviews
  describe scanning a test strip and returning a "suggested dosage". There is **no criterion-leader /
  on-job-leader split** on this shelf, which is why the head-to-head below is a single-app comparison.
- **The shelf's second on-job giant:** `com.troublefreepool.poolmath` (TroubleFreePool), **100K+, 3.0★,
  392 exact reviews, IAP true** — the community site's own app, monetised by a subscription.
- **The weak downloaded tier exists and is downloaded:** `com.biolabinc.pooltime` (Pool Time ClearPool
  Expert™) at **1.8★ with 50K+ installs** and `com.poolchemicalcalculator.poolcalc` at **3.6★ over 210
  exact reviews with 50K+ installs** (**MEASURED**, `node scripts/scan-complaints.mjs --package
  com.hthpools.tester.release --package com.troublefreepool.poolmath --package
  com.poolchemicalcalculator.poolcalc --package com.biolabinc.pooltime`). These are not apps nobody
  downloads.
- **An UNRATED incumbent is evidence of failure, not vacancy:** of the 10 enriched cards, 3 of the top
  10 are unrated including the #1 card (`com.coachplatform.poolcoach`, 50+ installs) and the #8 card
  (`com.zoryatechstudio.poolchemistry`, declared 0+ installs).

## 4. What the leader's own users complain about — quoted

**MEASURED**, `node scripts/scan-complaints.mjs --package com.hthpools.tester.release --package
com.troublefreepool.poolmath --package com.poolchemicalcalculator.poolcalc --package
com.biolabinc.pooltime` (Play server-renders a sample of its own choosing; measured here at 3 reviews
per page, so these describe the **vocabulary** of the shelf, never complaint frequency):

**`com.hthpools.tester.release` — the LEADER, 4.6★, 100K+, 1,052 reviews: 3 reviews read, 3 critical
(the leader's whole served sample).**

- 1★ (July 11, 2026): *"the photo reading of the test strips is off, as is the 'suggested dosage' of
  chemicals you're supposed to add. when I compare the test strip to the app and to visual comparison
  on the packaging, nothing is adding up. before I realized the issue with the misread of the test
  strips, the app told me to add 34 pounds of ph down to fix high alkalinity. I spent over $100 on
  chemicals to do that, just to spend the next three weeks attempting to fix the pool by spending even
  more money on chemicals."* — class `broken`.
- 1★ (May 25, 2026): *"if you're like me, you never really know the true levels of your pool as colors
  may fall in between... I wasted half a bottle of test strips and the app only says analysis
  unsuccessful... simply doesn't work."* — the ambiguity the owner actually has (colours that fall
  between the bands) makes the leader fail outright.
- 2★ (September 11, 2026): *"With spas :( I get error loading page quite a bit... When I was first
  setting up, it insisted on my entering my spa size in feet. like it was a pool. So I had to enter
  ridiculous numbers to get the closest I could to my spa's capacity which is 429 gal. Its 'scan the
  test strip' seems rather baffled by its own products."* — class `broken`.

**The shelf's other money engines.**

- `com.troublefreepool.poolmath` (100K+, 3.0★, 392): 2★ *"This app used to be useful for free, now
  basically requires a subscription to use... The message prompting you to subscribe explains that the
  subscription is for unlimited log storage"*; 2★ *"Unless you pay a subscription, you can only keep a
  log of ONE day... you get the prompt 'You must subscribe' and that is kind of garbage for a person
  who is just trying to maintain a single pool."* — class `paywall` ×2.
- `com.poolchemicalcalculator.poolcalc` (50K+, 3.6★, 210): 2★ *"Now, you are forced to watch a video
  ad EVERY SINGLE TIME you use any of the calculators. Even if you dont navigate out of the current
  screen and use the same calculator twice in a row with a new value, you are forced to watch an ad"*;
  1★ *"it forces me to create an account to track pool. Just want a simple calculator, no tracking, and
  don't want yet another account."* — class `ads` ×2, `account`, `paywall`, `privacy`.
- `com.biolabinc.pooltime` (50K+, 1.8★): 1★ *"using test strips, all it does is let me enter what I see
  on the strip. there are no recommendations based on my values."*; 3★ *"It's great that the strip
  testing results tells you what's wrong, ideal range etc. But that's not very helpful if it doesn't
  also tell you how to fix it."* — a 50K+-install app that shows a reading and gives no plan.

**Shelf complaint profile from the wider pull** (`.kiln/scratch/scout-advisor-complaints-md.md`, from
`node scripts/scan-complaints.mjs --query "pool chemistry advisor" --top 6 --concurrency 2`, run
2026-10-02T17:50Z — **MEASURED**, file read): 18 reviews, 11 critical; `paywall` 3 (27%), `ads` 2
(18%), `account`, `accuracy`, `broken`, `confusing-ui`, `crash`, `notifications`, `privacy`,
`regression` 1 each (9%). The same pull's verbatim `accuracy` complaint is the leader's 34-pound
review quoted above.

## 5. Head-to-head — each wedge clause against the leader's measured facts

The leader is `com.hthpools.tester.release` (**4.6★, 1,052 exact reviews, 100K+, free, no ads, no IAP**).
A clause the leader already satisfies is **not** a wedge; a clause its own users complain about is.

| Wedge clause (weight) | Leader's measured facts | Verdict |
|---|---|---|
| **Ranked diagnosis with the mechanism and the confirming observation** (`ranked-diagnosis-with-reasoning`, core) | The leader's own 1★: the dose "is off", "nothing is adding up", it told the user to add **34 lb of pH down** and the user "spent over $100"; another 1★: the app "only says analysis unsuccessful" when colours fall between bands. | **Leader criticised — wedge survives (strongest clause)** |
| **The ordered plan explains itself, step by step, with what not to mix and what to re-test** (`ordered-plan-explains-itself`, core) | The leader gives a "suggested dosage" the user could not check (the $100 review is precisely an uncheckable recommendation). The ordering half is **not** directly criticised on the leader; only a 50K+ mid-tier app is told it "doesn't also tell you how to fix it". | **Leader criticised for the uncheckable recommendation; the ordering half is SILENT on the leader (lands on the mid-tier)** |
| **Works from the bands and observations the owner actually has, and remembers the pool's own facts** (`works-from-bands-not-precision`, supporting) | The leader's own 2★ (spa): it "insisted on my entering my spa size in feet... I had to enter ridiculous numbers", and its 1★ says the app "only says analysis unsuccessful" for in-between colours. | **Leader criticised — wedge survives** |
| **The plan leaves the app as text/PDF** (`plan-leaves-the-app`, supporting) | Leader **silent** — no served incumbent complaint is about export; the related ask is on the 50K+/1.8★ mid-tier ("automatically build a shopping cart with everything needed to fix the issues"). | **Leader silent — a difference, not a leader-facing wedge** |
| **No ad, no account, no subscription, one unlock** (`no-ads-no-account-one-unlock`, supporting) | The leader **already satisfies it**: declared `ads: false`, `IAP: false`, and its reviews describe no account. The complaints live on `com.poolchemicalcalculator.poolcalc` (ads every use, forced account, 50K+) and `com.troublefreepool.poolmath` (subscription, 100K+). | **Leader already does it — NOT a wedge against the leader; a wedge against the shelf's money engines** |

**Entry filter result: passed.** Two core clauses (`ranked-diagnosis-with-reasoning`,
`ordered-plan-explains-itself`) plus one supporting clause answer complaints the **leader's own**
users wrote, verbatim, and one more (`works-from-bands-not-precision`) answers the leader's spa and
"analysis unsuccessful" complaints. The two clauses the leader already satisfies or is silent about
(no-ads, export) are recorded as such and are not counted toward the filter.

## 6. What the leader has that we do not

1. **A chemical manufacturer's 100K+ free funnel and a 4.6★/1,052-review trust floor.** HTH is
   published by HTH Pools and can be free with no IAP because the app sells the chemicals it
   recommends; a new listing has zero installs, zero reviews and no chemical revenue to subsidise free.
2. **A physical strip-scanning pipeline tied to its own product** and an in-store loop (the app ships
   on the strip bottle). Its users complain the scanner misreads, but it is still a capability we are
   deliberately not building, and it is the reason the card is installed at all.
3. **Two more money engines already on the shelf:** `com.troublefreepool.poolmath` (100K+, 392, the
   TroubleFreePool community's own app with a subscription) and `com.poolchemicalcalculator.poolcalc`
   (50K+, 210, ads + IAP). The shelf is not free-empty; it is a shelf where the leader is free and the
   two adjacent apps charge.

## 7. Willingness to pay — the measured reading

- **5 of the 10 enriched cards declare an in-app purchase** and **2 of 10 declare ads** (**MEASURED**,
  §3 summary block), at install buckets from 50+ to 100K+.
- The paying behaviour is **named by the incumbents' own users**: Pool Math's two served critical
  reviews are both about a subscription paywall at **100K+ installs** (*"Unless you pay a subscription,
  you can only keep a log of ONE day"*), and Pool Chemical Calculator's 2★ names both ads and a
  "recurring monthly fee" it refuses to pay at **50K+ installs**.
- **The honest limit:** the Play card shows `free` with `IAP: true` and **does not print an IAP price**,
  so no explicit dollar figure is measured anywhere on this shelf. What is measured is that two
  incumbents at 50K+ and 100K+ installs charge, and that users describe the charge. The nearest
  explicit price in the wider aquarium pull is `com.wiselogic.aquarimate` at *"the $10 you'll spend on
  it"* (2.6★, 1★ review, **MEASURED** in
  `.kiln/scratch/scout-advisor-complaints-md.md`), which is a comparable shelf's paid app, not this one.

## 8. Name — the carrier auditions, each eliminated by a measured collision

The naming contract's own method requires a generated field, not a defended favourite. Eight carriers
were auditioned on Play this session; every one is already carried by a live app, which is why the
recorded decision is a **descriptive** title (the same trade `plans/part-107-drills.naming.json`
records for its own occupied carrier space).

| carrier auditioned | measured collision (command: `node scripts/scan-titles.mjs --query "<name>"`) |
|---|---|
| `Litmus` | `com.litmusmobile` ships a live app titled **Litmus** |
| `Sonde` | Sonde Health ships **Sonde Mental Fitness** (`com.sonde.mentalfitness`) and **Sonde One** (`io.sonde`) |
| `Lido` | the token is carried by **Lido** (`ie.flipdish.br11410`), **Lido.Box**, **The Lido**, **Lido – Liquid**, **Lido Services**, **Lido Villas** |
| `Atoll` | `com.atoll.signature2` ships **ATOLL Signature 2**; the query is guard-`polluted` (3/30) |
| `Clarifier` | the query is guard-`polluted` (0/30) across **Clariti**, **Clarist**, **Claru**, **CLAR EVO**, **Clarité** |
| `Brine` | **Brine Fitness**, **Brine Oyster House**, **Brineward Ferment Guide**, **Cap Brine** |
| `Shoal` | `com.algbra.ShoalApp` ships **Shoal**, plus **Shoal & Stem** |
| `Skimmer` | audited in the same run; its output was truncated before classification, so it is **not** counted as a measured collision |

The selected title **`Pool Diagnosis`** carries no incumbent's title and is not a one-word swap of one
(the live neighbours are Pool-Calculator, Pool Water Calculator, Pool Chemistry Calculator, Pool Math,
PoolCoach: Chemistry Advisor, PoolChem: Pool Water Care — none uses *diagnosis*), is 14 characters
with 16 of Play's 30 characters of slack, and names the job rather than the category. The cost is
recorded honestly: a descriptive phrase is weakly protectable, the same price the drills recipe paid.

## 9. Refusals, each with its revenue reason

| shelf / query | measured reading | revenue reason for refusal |
|---|---|---|
| `aquarium water chemistry app` | `mixed`, guard `ok`; leader `com.kosick.aquadiary.android` **4.6★ / 1,812 exact / 100K+**, and its served critical sample is **1 of 3** and is an `abandoned` complaint; the weak tier's complaints are `missing-feature` 4 of 10, `paywall` 4 of 10, `ads` 3 of 10 and `data-loss` 2 of 10 (**MEASURED**, `.kiln/scratch/scout-advisor-complaints-md.md`) | The willingness-to-pay reading is `paywall`/`data-loss`, not judgement: the paid incumbents (`com.wiselogic.aquarimate` 2.6★, `aquaticlog.mobile.android` 4.0★ with a "$29.99 a year" complaint) sell **tank management and cloud storage**, which is the record-book shape this lane forbids, and the leader's own served sample carries **no** complaint about wrong or unexplained advice to build a wedge on. No measured complaint on the leader makes an advisor the thing to buy. |
| `car warning light meaning` | `candidate`, 18/24 rated incumbents below 4.2; leaders `oht.lightsymbols…` **4.2★/323/100K+** and `com.cardashboard.warning` **4.1★/381/100K+**, both free with ads (**MEASURED**, `.kiln/scratch/scout-advisor-enrich.md`) | The shelf's own user says why it cannot earn: *"I might have been willing to throw a buck or two down for an ad free version, but I fail to see the point when the info is so readily available using a Google search."* A one-time lookup with a free substitute and a price ceiling of "a buck or two" is a revenue refusal, not a novelty one. |
| `appliance error code lookup` | `mixed`; 8 of the 10 enriched cards are **unrated**, install buckets 10+ to 10K+, and the only large card is `com.autocodes.autocodes` (500K+) which is an **auto** code app (**MEASURED**, same file) | Per the rule an unrated incumbent is evidence of failure, not vacancy: no card on this shelf has a measured audience paying for a *diagnosis*, and the only 100K+ appliance card (`acerrorcode.com.acerrorcode`, 4.1★/1,934/100K+) is a free code list with ads. There is no demand reading to price. |
| `3d printing troubleshooting` | `mixed`, guard `partial` (12/30); the top card is `com.cxsw.sdprinter` (Creality Cloud) at **4.4★/36,321/5M+**, and the rest of the top 10 are model libraries and printer hubs (**MEASURED**, same file) | The shelf's gravity is hardware ecosystems that are free and monetise the printer, not the advice; the troubleshooting cards that exist are unrated or 10-install experiments, and no priced troubleshooting incumbent is measured. A new listing would be paying to answer questions the printer's own app answers for free. |

## 10. Arithmetic, with the division shown

Target: **$1,000/month**, the factory target in `docs/monetization-strategy.md` (**ON RECORD**).

```text
price                                  $5.99
Play service fee 15%          5.99 × 0.85   = $5.0915
refund allowance 2%           5.0915 × 0.98 = $4.98967
currency/FX allowance 2%      4.98967 × 0.98 = $4.88988  ≈ $4.89 net per sale
sales/month to clear the target   1000 ÷ 4.88988 = 204.5  → 205 unlocks/month
check                             205 × 4.88988 = $1,002.42 ≥ $1,000
implied installs at the plan's assumed 2% install→unlock conversion
                                  205 ÷ 0.02 = 10,250 installs/month
kill floor, derived from a measured floor rather than chosen beside the target
  the weakest DOWNLOADED incumbent, com.biolabinc.pooltime at 1.8★, has 50K+ installs
  4% of that install floor over 90 days            = 2,000 installs
  2,000 × 2% conversion = 40 unlocks
  40 × 4.88988                                     = $195.60 → recorded floor $200
```

3 (15%), 2% and 2% are plan assumptions carried from `docs/monetization-strategy.md`, not measurements
of this developer account; the conversion rate is an assumption with no measurement anywhere in this
repository. Both are listed as honest unknowns in the plan.

## 11. Claim ledger

| # | claim | label | evidence |
|--:|---|---|---|
| 1 | `habit tracker` → `fortress`, `bjj training log` → `dead-zone`, `pool chemistry advisor` → `mixed` (16/30 rated, median 4.45) | MEASURED | `node scripts/scan-market.mjs --query "habit tracker" --query "bjj training log" --query "pool chemistry advisor" --out .kiln/scratch/scout-advisor-calibration.md` |
| 2 | Criterion leader is `com.hthpools.tester.release`: 4.6★, 1,050 (1,052) reviews, 100K+, ads false, IAP false, free | MEASURED | `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "pool chemistry advisor" --out .kiln/scratch/scout-advisor-pool-enrich.md`, row 2 |
| 3 | `com.troublefreepool.poolmath`: 3.0★, 392 (392), 100K+, IAP true; `com.poolchemicalcalculator.poolcalc`: 3.6★, 210 (210), 50K+, ads true, IAP true | MEASURED | same command, rows 9 and 3 |
| 4 | 5 of 10 enriched cards declare IAP; 2 of 10 declare ads; largest bucket 100K+ | MEASURED | same command, summary block |
| 5 | Hand job-share of the primary query is 19/30 (0.63) while the guard says `on-topic` | MEASURED | `node scripts/scan-titles.mjs --query "pool chemistry advisor" --on-job "…" --off-job "…" --out .kiln/scratch/scout-advisor-pool-titles.md` |
| 6 | The leader's served sample is 3 reviews, 3 critical: the 34-lb-pH-down/$100 review, the "analysis unsuccessful" review, the spa/feet review | MEASURED | `node scripts/scan-complaints.mjs --package com.hthpools.tester.release --package com.troublefreepool.poolmath --package com.poolchemicalcalculator.poolcalc --package com.biolabinc.pooltime` |
| 7 | `com.biolabinc.pooltime` is 1.8★ with 50K+ installs and says "there are no recommendations based on my values" | MEASURED | same command |
| 8 | Pool Math's two served critical reviews are `paywall`: "You must subscribe", "only keep a log of ONE day" | MEASURED | same command |
| 9 | Pool Chemical Calculator's served critical reviews name a video ad on every calculator use and a forced account | MEASURED | same command |
| 10 | Shelf complaint profile for the primary query: 18 reviews, 11 critical, paywall 3 (27%), ads 2 (18%) | MEASURED | `node scripts/scan-complaints.mjs --query "pool chemistry advisor" --top 6 --concurrency 2`, read at `.kiln/scratch/scout-advisor-complaints-md.md` |
| 11 | `com.wiselogic.aquarimate` is 2.6★ and its 1★ says "This app isn't worth the $10 you'll spend on it"; `aquaticlog.mobile.android` has a "$29.99 a year" complaint | MEASURED | `.kiln/scratch/scout-advisor-complaints-md.md` (aquarium pull) |
| 12 | The car shelf's top cards are `oht.lightsymbols…` 4.2★/323/100K+ and `com.cardashboard.warning` 4.1★/381/100K+, and a served review says the info is "readily available using a Google search" | MEASURED | `.kiln/scratch/scout-advisor-enrich.md` and `.kiln/scratch/scout-advisor-complaints-md.md` |
| 13 | The appliance shelf's 10 enriched cards are 8 unrated with buckets 10+…10K+, and the 500K+ card is an auto-code app | MEASURED | `.kiln/scratch/scout-advisor-enrich.md` |
| 14 | The 3d-printing shelf is led by Creality Cloud at 4.4★/36,321/5M+ with model libraries behind it | MEASURED | `.kiln/scratch/scout-advisor-enrich.md` |
| 15 | `Litmus`, `Sonde`, `Lido`, `Atoll`, `Clarifier`, `Brine`, `Shoal` are each carried by a live Play app | MEASURED | `node scripts/scan-titles.mjs --query "Litmus" --query "Sonde" --query "Lido" --query "Atoll"` and `--query "Clarifier" --query "Brine" --query "Shoal" --query "Skimmer"` |
| 16 | A pool owner will pay $5.99 once for a diagnosis they can check, on a shelf whose leader is free | UNVERIFIED | no priced comparable exists on this shelf (the Play card shows `free` with `IAP: true` and no IAP price); the deciding reading is a Play Console result via `node scripts/check-outcomes.mjs --slot pool-diagnosis` |
| 17 | Install→unlock conversion is 2% | UNVERIFIED | no slot in this repository has ever measured a conversion rate; the deciding reading is the recorded `store/outcomes.json` after launch |
| 18 | Play serves the reviews these tables quote at 3 per page, so they describe vocabulary, not frequency | MEASURED | the counts and the per-page limit printed by `scripts/lib/play-reviews.mjs` in the runs above |
| 19 | The exact storefront copy quoted here is the copy Play served this session | UNVERIFIED | one signed-out `hl=en`, `gl=US` session; not a Play Console reading and not reproducible byte-for-byte |
| 20 | A rule base of corrective plans is enough to make an owner defect from a free 4.6★ leader | UNVERIFIED | no comparable was found selling a checkable diagnosis on this shelf; the only way to decide is a live listing |

## 12. What this cannot tell us

- Install buckets are rounded floors (`100K+` spans 100,000–999,999), so every size comparison here is
  a floor comparison.
- Revenue, retention and conversion are not observable on a store page. IAP declared proves a model
  exists, not that it earns, and no IAP price is printed.
- `scan-complaints` reads Play's own server-rendered sample on the weakest-rated incumbents for a query
  and on the named package for `--package`, so these tables are the shelf's **vocabulary**. The leader
  with **3 critical of 3 read** is a leader whose complaints were served; a leader with 0 critical in a
  3-review sample is not a leader without complaints.
- The leader was selected by one criterion applied to one query in one storefront session; a larger
  incumbent can sit below the enrichment cut.
- The relevance guard was demonstrably over-confident on the primary query (on-topic over a 0.63 hand
  job-share), which is why the hand classification above is the reading used.

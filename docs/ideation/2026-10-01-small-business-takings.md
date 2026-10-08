# Small-business daily takings and stock records — recon, 2026-10-01

**Scout:** PlayKiln recon scout (subagent). **Lane:** the money-and-stock record a one-person trading
business keeps every day — takings, stock, what sold, what is left.
**Deliverable:** this file, plus one gate-ready plan pair under `docs/ideation/plans/`.
**Owner decision required at:** human checkpoint 1 (approve the plan and its price). Nothing is
scaffolded by this document.

> **Boundary honoured, stated so it is auditable.** This run read the **market** only. It did not open
> anything under `apps/**`, inspect a slot, count a screen or report on a slot's stage, completeness or
> quality. The "has this job been taken?" question was answered from the `id`, `name` and `idea` fields
> of `registry/apps.json` alone (§7), never from its `status`, `reason` or `updatedAt` fields.

---

## 0. Method, controls and elapsed time

| Run | Command | Observed |
|---|---|---|
| Screen | `node scripts/scan-market.mjs --concurrency 4 --query <six lane queries> --query "habit tracker" --query "bjj training log"` | **24 queries in 7,065 ms** (concurrency 4): 3 `candidate`, 12 `mixed`, 4 `fortress`, 5 `dead-zone`, 0 errored — MEASURED |
| Hand classification | `node scripts/scan-titles.mjs --concurrency 3 --query <six queries> --on-job <regex> --off-job <regex>` | **6 queries, 29,179 ms**, 1 query flagged by the tool as a guard failure — MEASURED |
| Enrichment | `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query …` (three invocations, six queries) | **48 app pages fetched and parsed, 0 failures**, 5.8–12.1 s each — MEASURED |
| Complaints | `node scripts/scan-complaints.mjs --query … --top 6` (two) and `--package …` (six packages) | **42 reviews read, 20 critical** across 14 incumbents — MEASURED |

**Calibration controls, in the same invocation as the lane queries — and they behaved.**

| Control | Expected | Observed | Reading |
|---|---|---|---|
| `habit tracker` | `fortress` | **`fortress`** — 21 seen, 20/21 rated, 14/20 rated incumbents at ≥ 4.5 | sensor did not drift |
| `bjj training log` | `dead-zone` | **`dead-zone`** — 30 seen, 8/30 rated (0.27 < 0.35) | sensor did not drift |

Both controls returned their hand-read verdicts, so the lane verdicts below are evidence rather than
sensor noise. **One honest caveat:** the six lane queries were re-screened inside the `scan-titles`
run about two minutes later, and `farmers market sales log` returned `candidate` there against `mixed`
in the first run. That is the run-to-run variance `docs/research/2026-09-24-market-decision.md` §1.1
already measured on an identical query (visible results 9 → 25), and it is why **no conclusion below
rests on a verdict alone**: every one is anchored to enriched install and review counts, to hand
classification, or to verbatim review text.

---

## 1. The screen — per-query verdicts (MEASURED)

| Query | Verdict | Relevance | Seen | Rated | Weak | Median |
|---|---|---|---:|---:|---:|---:|
| `daily takings book` | `mixed` | **partial** | 30 | 16/30 | 2 | 4.50 |
| `market stall sales tracker` | **`dead-zone`** | ok | 29 | 9/29 | 4 | 4.50 |
| `food truck sales log` | `mixed` | ok | 29 | 18/29 | 8 | 4.25 |
| `craft fair sales tracker` | `mixed` | ok | 29 | 11/29 | 4 | 4.50 |
| `farmers market sales log` | `mixed` (first run) / `candidate` (second run) | ok | 29 | 16/29 | 6 | 4.40 |
| `pop up shop inventory tracker` | `candidate` | ok | 13 | 8/13 | 4 | 4.10 |

The scanner's own notes, quoted rather than paraphrased: `daily takings book` — *"demand proven,
2 weak incumbent(s), but 0.56 of rated are strong — enter only with a specific angle"*;
`market stall sales tracker` — *"only 9/29 incumbents are rated (0.31 < 0.35), so demand is not
proven"*; `pop up shop inventory tracker` — *"4/8 rated incumbents are below 4.2 — demand proven,
supply weak"*.

---

## 2. Hand classification of the visible titles (MEASURED)

A guard verdict is a spot check, not a measurement: `docs/research/2026-09-24-market-decision.md` §2.1
measured the guard returning its most-trusted label `ok` at **0.93 lexical confidence** on a shelf
where only **63 %** of cards named the job. The printed titles were therefore classified by eye.

| Query | Guard verdict | Guard on-topic share | Hand job-share | Read |
|---|---|---|---:|---|
| `daily takings book` | partial | 21/30 (0.70) | 24/30 (0.80) | **but the job-share is inflated by `book`** — Goodreads, Bookly, BookPine, Blinksist, Bookmory and MyStory.today are reading trackers (6 of 24) |
| `market stall sales tracker` | ok | 26/29 (0.90) | 27/29 (0.93) | **both shares are inflated by `market`** — MarketWatch (50,500 reviews), Stock Market Tracker, Market Tracker: Trade Journal, Market Sessions and Market Countdown are equities; Market Street, The Market Rewards, Market 32, The Market Supermarket and Market Plus are grocers and rewards cards |
| `food truck sales log` | ok | 23/29 (0.79) | **6/29 (0.21)** | **`scan-titles` flagged this itself — `GUARD SAID on-topic, JOB SHARE IS LOW`** — and of the 6, four are truck *dealer* apps and two are cooking games |
| `craft fair sales tracker` | ok | 20/29 (0.69) | 19/29 (0.66) | the craft-specific apps are all **pricing calculators** (CraftCost, Craft Pricing Calculator, Craft Calculator — Tecydo), which is a burned category, and the rated on-job cards are generic sales and CRM |
| `farmers market sales log` | ok | 23/29 (0.79) | 19/29 (0.66) | the shelf is market *finders*, a grocery shop, a match-3 game and two CRMs; no card records a vendor's takings |
| `pop up shop inventory tracker` | ok | 11/13 (0.85) | 12/13 (0.92) | the cards are home inventory (Store & Forget, My Inventory, Collection & Inventory) and generic stock (Zoho, Magnetic Lab), not a stall's trading record |

**What the hand read found that the guard could not: `market` and `book` are homonyms.** The guard
counts a card as on-topic when it echoes a distinctive query token *somewhere*; `MarketWatch` and
`Goodreads` therefore satisfy it while naming a different job. This is the third measured instance of
the same sub-mechanism, not a new discovery — see `docs/research/2026-09-24-market-decision.md` §2.1.

---

## 3. The demand reading — enriched incumbents (MEASURED)

Install figures are Play's rounded buckets, so every size comparison is a floor comparison. Review
counts are exact as parsed from the card.

### 3.1 `daily takings book` — the shelf with the most money on display

| # | Package | Title | Rating | Reviews (exact) | Installs | Ads | IAP | Price |
|---:|---|---|---:|---:|---|---|---|---|
| 1 | `cashbook.cashbook` | Cash Book- daily expenses | 4.5★ | **145,000** (144,567) | **10M+** | yes | yes | free |
| 2 | `com.bigheadtechies.diary` | Daybook - Diary, Journal, Note | 4.6★ | 59,100 (59,055) | 1M+ | no | yes | free |
| 3 | `com.kutirsoft.dailysalesrecord` | Daily Sales Record - POS, CRM | 4.5★ | 4,240 (4,242) | 500K+ | no | yes | free |
| 4 | `com.shrey_businessx.android` | Daily Sales Profit & Inventory | 4.0★ | not on card | 100K+ | yes | yes | free |
| 5 | `gnhub.ledgerbook.pro` | The Ledger Book: Bookkeeping | 4.4★ | **159** | 10K+ | yes | no | free |
| 6 | `com.businessfinancesmanager` | SmallBusiness:QuickBookKeeping | unrated | unknown | 1K+ | no | yes | free |
| 7 | `com.cashbook.income.expenses` | Cash Book Daily Income Expense | unrated | unknown | 1K+ | yes | yes | free |
| 8 | `com.shahanawaj.dailyprofitbook` | DAILY PROFIT BOOK | unrated | unknown | 100+ | yes | no | free |
| 9 | `com.ackbiz.daily_book` | Daily Book: Milk & Hisab | unrated | unknown | 100+ | no | no | free |

**Demand is proven at the top and the money shape is uniform.** A 10M+ leader at 4.5★ over 145,000
reviews; the free-plus-advertising-plus-IAP shape in 4 of 9; **not one paid-upfront app in this shelf**.

### 3.2 `market stall sales tracker` — a graveyard for the stall-specific job

| Package | Title | Rating | Reviews | Installs | Ads | IAP | Price |
|---|---|---:|---:|---|---|---|---|
| `com.kutirsoft.dailysalesrecord` | Daily Sales Record - POS, CRM | 4.5★ | 4,240 | 500K+ | no | yes | free |
| `com.passiveapp.sunmi_receipt` | Stall POS Checkout | unrated | unknown | 5K+ | yes | yes | free |
| `com.salestrackers` | Sales Tracker | unrated | unknown | 5K+ | no | no | free |
| `com.callnoty.app` | Mobile CRM Lead Sales Tracker | unrated | unknown | 1K+ | no | yes | free |
| `com.gerdal.sales_tracker` | Sales Tracker | unrated | unknown | 100+ | no | no | free |
| `com.rm7k.st` | Sales Tracker - Invoicing | unrated | unknown | 100+ | no | yes | free |
| `com.denny.quickstall` | QuickStall: Offline POS System | unrated | unknown | **10+** | no | yes | free |

**The stall-specific job is a graveyard of unrated micro-apps** — `QuickStall` at 10+ installs,
`Stall POS Checkout` and `Sales Tracker` at 5K+ with no rating — while the only rated card on the job
is the **generic** `Daily Sales Record` at 4.5★/500K+. Per `docs/niche-selection.md` §2, many unrated
competitors with no leader is not low competition, it is "people tried and nobody got traction".

### 3.3 `pop up shop inventory tracker` — the stock half, served generically

| Package | Title | Rating | Reviews | Installs | Ads | IAP | Price |
|---|---|---:|---:|---|---|---|---|
| `com.mlab.stock.management` | Stock and Inventory Management | 4.2★ | 2,230 (2,226) | 100K+ | yes | yes | free |
| `com.zoho.stocktracker` | Inventory Stock Tracker | 4.2★ | 1,080 (1,076) | 100K+ | no | no | free |
| `com.ppapps.collections` | Collection & Inventory Tracker | 4.7★ | 212 | 10K+ | no | yes | free |
| `com.binaryscript.offlineshop` | Offline Shop - Inventory POS | unrated | unknown | 10K+ | no | yes | free |
| `lk.nextbots.invy` | Stock and Inventory Simple | unrated | unknown | 5K+ | no | yes | free |
| `dev.koalalab.storeandforget` | Store & Forget: Home Inventory | 4.0★ | 37 | 1K+ | yes | yes | free |
| `jp.co.wasabiapps.myinventory` | My Inventory – Home Tracker | unrated | unknown | 1K+ | no | no | **paid $2.99** |
| `com.edesignhouse.popupshop` | POP-UP SHOP | 3.9★ | **7** | 500+ | no | no | free |

The **only paid-upfront app found anywhere in this run** sits at the bottom of its shelf with 1K+
installs, and the app actually named for the query has **7 reviews**.

### 3.4 `craft fair sales tracker`, `food truck sales log`, `farmers market sales log`

| Query | Best-rated card that is actually on the job | Reading |
|---|---|---|
| `craft fair sales tracker` | `com.srgroup.purchasesalesbook` — Purchase and Sales Book, **3.9★ / 249 / 10K+**, ads + IAP | the craft-specific cards are unrated pricing calculators; the rated on-job cards are generic |
| `food truck sales log` | **none** — the top 8 enriched are `FoodTrukt` (5.0★/5), `OpenTruck` (unrated), `Best Food Trucks` (3.4★/44), `Truckster` (2.8★/167), `Best Food Trucks - Vendors` (3.6★/21), `Ohio Truck Sales` (4.0★/60, a **truck dealership**), plus two cooking games | **no incumbent records a food truck's takings**; the shelf answers "where is the truck", not "what did the truck take" |
| `farmers market sales log` | `com.farmersmarket.indy` — Market Wagon, 3.3★/64/10K+, an online farmers-market ordering platform | the shelf is finders, a grocery shop and a match-3 game; **no incumbent records a market vendor's takings** |

### 3.5 The one number that decides the money shape

Across all 48 enriched cards, **9 of the 10 incumbents in the two surviving shelves are free, and 5 of
them declare advertising while 6 declare in-app purchases.** Exactly one — `jp.co.wasabiapps.myinventory`
at $2.99 with 1K+ installs — is a paid-upfront app, and it is the weakest reading of its shelf.
**A paid-upfront app is not how this audience acquires apps; free with advertising and an unlock is.**

---

## 4. What the incumbents' users actually complain about (MEASURED, verbatim)

### 4.1 The decisive complaint — the app that already serves both halves

`com.shrey_businessx.android`, **4.0★, 100K+ installs**, sells *Daily Sales Profit & Inventory*, and
declares both advertising and in-app purchases. Two of its three sampled reviews are critical, and
both name the wedge directly:

- *"It's a great app. User friendly and easily to understand. my only issue is, it doesn't have a
  space to actually input restock quantities with cost pri[ce]"* — classified `missing-feature`
- *"Great app, however 1. My data doesn't sync across my devices even after paying for premium
  2. Why do I still get ads...even after paying for premium"* — classified `paywall` and `ads`

A **paying** user of a 100K+ app says the paywall is dishonest and the stock side does not take a cost
price. That is money on the table: this user has already proved they pay.

### 4.2 The rest of the corpus

| Package | Rating | Installs | Verbatim critical review | Class |
|---|---:|---|---|---|
| `gn.cashbook` | 3.9★ | 500K+ | *"I lost my 4 days back up automatically Even I already backed it up daily ,so why it happened ,luckily I remembered the data of some accounts and some"* | `data-loss` |
| `com.srgroup.purchasesalesbook` | 3.9★ | 10K+ | *"1. Can't edit errors in Vendor Balance. 2. Can't edit errors in Total balance. 3. There's no clarity of Debt Purchase and Payments made for Vendors."* | `missing-feature` |
| `com.foodmonk.rekordapp` | 4.1★ | 1M+ | *"after one month they will ask for payment to see or open your file. This is cheating with the customers. Don't install the app if you want to use the [file]"* | `paywall` |
| `personal.expense.budget.pocket.moneytracker` | 4.3★ | 100K+ | *"I have been trying to request for support on email because my POS and sales invoice keep failing. I can't save. assist please"* | `missing-feature` |

**Histograms.** `daily takings book` pocket (`--top 6`): 18 reviews, 5 critical, 1 unclassified —
`data-loss` 2, `crash` 1, `missing-feature` 1, `notifications` 1, `paywall` 1, `performance` 1.
`market stall sales tracker` pocket: 18 reviews, 12 critical, 2 unclassified — but the weakest-rated
selection there was dominated by off-job cards (MarketWatch, two grocer reward apps, a load board), so
only `com.shrey_businessx.android` carries weight; it is reported rather than counted. On-job packages
(`--package`, six on-job apps): 12 reviews, 3 critical, **0 unclassified** — `missing-feature` 2,
`ads` 1, `data-loss` 1, `paywall` 1.

**Honest limit, stated rather than implied:** the review sample is Play's choice, three per page, so
these histograms describe the *vocabulary* of the pocket, not complaint frequency in its population
(`docs/research/2026-09-24-market-decision.md` §5).

---

## 5. Refusals — every query screened, with its REVENUE reason

`.cursor/rules/ideation-gate.mdc` § "The objective is revenue, not novelty": a refusal must name a
revenue reason. "The shelf is crowded" is not one; "the shelf is empty" is not a qualification.
**Occupied shelves are counted in this proposal's favour wherever they carry a demand reading.**

| Query | Verdict | Revenue reason for refusal as its own recipe |
|---|---|---|
| `market stall sales tracker` | `dead-zone` | **Unrated demand reading with no leader.** 9/29 rated; every stall-specific card is unrated (`QuickStall` 10+ installs, `Stall POS Checkout` 5K+, `Sales Tracker` at 100–5K+). A 4.5★/500K+ card exists but it is a *generic* POS/CRM, so it prices the generic job, not the stall job. The pocket is unproven. |
| `food truck sales log` | `mixed`, **guard failure** | **No demand reading for the job at all.** The guard said `on-topic` at 0.79 while the hand job-share is **0.21**, and all 8 enriched cards are finders, a dealer and two cooking games. A store page carrying *"Find Food Trucks"* does not price *"record the truck's takings"*; there is nothing to price. |
| `farmers market sales log` | `mixed` / `candidate` | **Verdict is not reproducible and the shelf is off-job.** The same query returned `mixed` then `candidate` two minutes apart. Every enriched card is a market *finder*, a grocery shop, a news app or a match-3 game. No rated incumbent records a vendor's takings, so there is no price the market has shown it will pay. |
| `craft fair sales tracker` | `mixed` | **The craft-specific supply is a burned category.** The three craft-named cards (CraftCost, Craft Pricing Calculator, Craft Calculator — Tecydo) are all unrated **pricing calculators**; `.cursor/rules/ideation-gate.mdc` closes calculators, converters and generic arithmetic tools, and a craft-fair recipe whose wedge is "price your makes" is that category wearing a market's name. The rated on-job cards are generic sales/CRM. |
| `pop up shop inventory tracker` | `candidate` | **Not refused — folded in.** Its pocket is real (`com.mlab.stock.management` 4.2★/2,226/100K+, `com.zoho.stocktracker` 4.2★/1,076/100K+) but the cards serve *home* inventory or generic stock, and its stock job and the takings job differ only in which half of one ledger is on screen. Shipping them as two apps is the internal near-duplicate Play's Repetitive Content policy terminates accounts for (`docs/monetization-strategy.md` §1.2). They are one recipe, below. |

**Two notes I want on the record because they are *not* refusals of the shelf.** The stall, fair and
farmers-market shelves are not refused for being occupied — `market stall sales tracker` has a 500K+
incumbent and the grocery and rewards cards carry real money. They are refused because the *specific
job* has no demand reading: its cards are unrated micro-apps. The distinction matters, because
"already exists, so do not build" is exactly the framing the owner corrected on 2026-10-01.

---

## 6. Recipe — market day book (`market-day-book`) — RECOMMENDED

### 6.1 Category and job, as one sentence

A one-person trading business that sells in person — a market-stall holder, a craft-fair seller, a
pop-up-shop keeper — records each market day's takings, the stock that left the stall at its own cost,
and what is owed to each wholesaler, in one offline book on the phone in their pocket.

**Paying user.** *A one-person trading business, a market-stall holder, a craft-fair seller or a
pop-up-shop keeper, who sells in person on market days and keeps the day's money and stock record on
the phone in their pocket.* — LABEL: **INFERRED** from the shelf's shape (a 10M+ free leader, a 500K+
POS/CRM, a 100K+ sales-and-inventory app) plus the verbatim complaints in §4, which are written by
exactly this user.

**What exactly is bought.** One permanent unlock of the whole book: unlimited market days, unlimited
stock lines and suppliers, the supplier ledger, and period export as CSV and PDF, with advertising
switched off for good.

**Price.** **$4.99**, one time. **Moment of payment.** After the trader has closed a couple of market
days for free, at the moment the book fills and they want another market day, a supplier balance or an
export — behind one clearly priced unlock sheet that is also the ad-free purchase.

### 6.2 The archetype, derived — and the three rejected with their revenue reasons

`docs/monetization-strategy.md` §3.6 is applied in order, and it produces the answer; this is not a
choice made first and justified after.

**Step 1 — child-directed?** No. No child audience, no Families regime.

**Step 2 — is there something worth selling *repeatedly*?** **No, and this is the load-bearing
rejection.** The app has no server, no content feed, no generator and no metered service call.
Everything it holds is the trader's own record. There is nothing consumable to sell again: the only
repeatable "unit" would be *more months of the same access*, which is a subscription in disguise, and
subscriptions are banned outright (`.cursor/rules/ideation-gate.mdc` § "There is no house
monetization"). **`purchase_led_repeat_sales` is rejected because no consumable unit exists** — not
because it was not tried.

**Step 3 — a utility with nothing to sell repeatedly?** **Yes.** → **ads lead, with a one-time
"remove ads" / "pro" unlock as the upsell**; and because there is nothing here worth watching a
rewarded video for, the paid product is the **unlock**, not rewarded inventory.

**Step 4 — genuine recurring value?** Not applicable, and it could not be sold if it were.

**Step 5 — the archetype sentence.** *An offline market-trader's day book, free with advertising shown
only off the entry path, and a single $4.99 purchase that removes the advertising and opens the whole
book.*

| Archetype | Verdict | Revenue reason |
|---|---|---|
| **`ads_led_utility`** | **SELECTED** | §3.6 step 3 applies: a utility with nothing consumable. It is also what the shelf's own money shape says — **9 of the 10 incumbents in the surviving shelves are free, 5 declare advertising, 6 declare an in-app purchase, and the leader monetises with advertising plus IAP at 10M+ installs.** Requires the `ads` capability; `billing` is enabled alongside it for the unlock, which `docs/slot-economics.md` explicitly allows. |
| `purchase_led_repeat_sales` | **REJECTED** | No consumable exists. The app has no server, no content and no metered call, so every "repeat purchase" shape collapses into selling more time on the same access — a subscription, which is banned. |
| `purchase_led_single_unlock` | **REJECTED as the lead** | The shelf is free. A paywalled app has to win installs against six free incumbents in the same result set, and the **one** paid-upfront app this run found — `jp.co.wasabiapps.myinventory` at $2.99 — sits at the bottom of its shelf at 1K+ installs. §3.6 step 3 makes the unlock the upsell here, not the lead; the unlock is still sold, and §6.4 shows it carries most of the modelled revenue at low volume. |
| `purchase_only_no_ads` | **REJECTED** | §4 of `docs/monetization-strategy.md` records it as a price lever with no discovery, valid only for ad-hostile or child-directed audiences. This audience is neither: its leader is advertising-supported at 10M+ installs, and its shelf shows no ad-hostility in the complaint corpus. |
| `subscription` | **NOT WEIGHED** | Banned (owner, 2026-10-01) and absent from `ARCHETYPES`, so a plan naming it fails the gate with no exemption. |

**Stated plainly, because the owner will weigh it at checkpoint 1:** at the modelled install volume in
§6.4 the **unlock carries most of the revenue and the advertising is a secondary stream**. That is what
"ads lead with the unlock as the upsell" means for a low-DAU work tool, and it is the honest reading of
a decision rule that asks first "is anything worth selling repeatedly?" — here, nothing is. If the
owner reads the same arithmetic and prefers the unlock as the lead, the archetype is a one-line change
in the plan file and nothing else in the recipe moves.

### 6.3 The wedge, in one sentence

**The day's takings and the stock that left the stall are one entry — a sale decrements what is left
at the trader's own purchase cost, the supplier balance is carried from those same entries and stays
editable, the book needs no account and no network, and buying it once removes the advertising for
good.**

Every clause answers a complaint a named user wrote (§4): the restock-entry-at-cost gap
(`com.shrey_businessx.android`, 4.0★/100K+), the uncorrectable vendor balance
(`com.srgroup.purchasesalesbook`, 3.9★/10K+), the lost day (`gn.cashbook`, 3.9★/500K+), the file held
behind a payment (`com.foodmonk.rekordapp`, 4.1★/1M+), and the advertising that survives payment
(`com.shrey_businessx.android` again).

**Not a calculator, and this is the check that matters.** The product is the *record*: market days,
stock lines, suppliers and payments over months, held locally and exported. The arithmetic is derived
**views** over the trader's own entries. If the wedge could be restated as *"it adds up numbers the
user types"*, the recipe would be refused under `.cursor/rules/ideation-gate.mdc`; it cannot, because
the complaint that selected it is about a **missing stock record at cost**, not about a sum.

### 6.4 The revenue arithmetic, with the division shown

Assumptions are `docs/monetization-strategy.md` §2.2's, which that document labels **ASSUMPTION,
decide per slot**, not measurements of this account.

**Stream A — the one-time unlock, $4.99.**

```
price            = $4.99
deductions       = (1 − F_billing 0.15)(1 − F_refund 0.02)(1 − F_fx 0.02)
                 = 0.85 × 0.98 × 0.98
                 = 0.81634
net per unlock   = $4.99 × 0.81634 = $4.0735   →   $4.07
```

There is no `C_unit`: the unlock is a non-consumable (`Fulfillment.LOCAL`), acknowledged once, with no
money-costing fulfilment, so §2.2's fraud divisor does not apply and no server is needed.

```
target                      = $1,000 / month
unlocks needed              = $1,000 ÷ $4.07  = 245.7   →   246 unlocks / month
installs needed at 2 %      = 246 ÷ 0.02      = 12,300 installs / month
installs needed at 1 %      = 246 ÷ 0.01      = 24,600 installs / month
installs needed at 3 %      = 246 ÷ 0.03      =  8,200 installs / month
```

*(The 1–3 % install-to-purchase range is `docs/monetization-strategy.md` §4's own figure, labelled
there UNVERIFIED. No slot in this repository has ever measured a real conversion rate, so this whole
row is an assumption and is listed as one.)*

**Stream B — banner + interstitial advertising.**

`docs/monetization-strategy.md` §2.1 (ON RECORD — published benchmarks, not our own measurement) gives
the DAU needed for $1,000/month: **rewarded-centric ~700–1,000 DAU; interstitial + banner ~2,000–6,000
DAU; banner-only 11,000+ DAU.**

```
installs / month (2 % case)  = 12,300
DAU / installs  (ASSUMPTION) = 0.15    →  a work-day tool, opened on trading days
steady-state DAU             = 12,300 × 0.15 = 1,845
position against §2.1        = below the interstitial + banner band (2,000–6,000)
```

**1,845 DAU sits just under the band that carries $1,000/month on its own**, so the advertising stream
is modelled as **upside of roughly $0–900/month, not as the plan's floor**, and the plan does not
depend on it. There is deliberately no rewarded inventory: the honest answer to *"what does a rewarded
view unlock?"* is *nothing*, and §3.6 step 3 then prefers the unlock.

**The plan's requirement, as one row.**

| | Value |
|---|---|
| Reference target | **$1,000 / month net** |
| Installs / month required | **~12,300** at 2 % conversion (24,600 at 1 %, 8,200 at 3 %) |
| Net per sale | **$4.07** |
| Sales / month required | **246** |
| Advertising stream | secondary; ~$0–900/month upside at 12,300 installs/month, not relied on |

**Honest scale note.** 12,300 installs/month is a large ask for a niche trader app, and I am not
softening it: the 10M+ and 500K+ incumbents in this shelf show the *category* has that volume, while
the recipe targets a narrower user. This is the single biggest risk in the proposal, and it is
reflected in §6.9's kill criteria rather than hidden.

### 6.5 Demand evidence — every claim labelled

| Claim | Label | Evidence |
|---|---|---|
| The small-business takings and shop-stock shelves carry a demand reading | **MEASURED** | `cashbook.cashbook` 4.5★ over **145,000** reviews at **10M+** installs; `com.kutirsoft.dailysalesrecord` 4.5★ over **4,240** at 500K+; `com.mlab.stock.management` 4.2★ over **2,230** at 100K+. Command: `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "daily takings book" --query "pop up shop inventory tracker"` |
| A **paying** user of a 100K+ incumbent names the stock-at-cost gap and the dishonest paywall | **MEASURED** | `com.shrey_businessx.android`, 4.0★, 100K+, two verbatim reviews in §4.1. Command: `node scripts/scan-complaints.mjs --package com.shrey_businessx.android` |
| The free-with-advertising-and-unlock money shape dominates this shelf | **MEASURED** | 9 of 10 enriched incumbents in the surviving shelves are free; 5 declare advertising, 6 declare an in-app purchase; exactly one paid-upfront app (`jp.co.wasabiapps.myinventory`, $2.99, 1K+ installs). Same enrich run. |
| The stall-, fair-, truck- and market-specific jobs have **no** demand reading | **MEASURED** | Hand job-share from `node scripts/scan-titles.mjs`; `market stall sales tracker` is `dead-zone` at 9/29 rated with every stall-specific card unrated; `food truck sales log` job-share **6/29 (0.21)** against the guard's `on-topic` 0.79. |
| Data loss, uncorrectable vendor balances and paywalls are live complaints in this pocket | **MEASURED** | `gn.cashbook` (3.9★/500K+) *"I lost my 4 days back up automatically"*; `com.srgroup.purchasesalesbook` (3.9★/10K+) *"Can't edit errors in Vendor Balance"*; `com.foodmonk.rekordapp` (4.1★/1M+) *"after one month they will ask for payment to see or open your file"*. Commands in §0 and §8. |
| The audience is one-person trading businesses selling in person | **INFERRED** | The shelf's titles (*Daily Sales Record*, *Purchase and Sales Book*, *Offline Shop*, *Stall POS*) and the four verbatim complaints. No survey, no first-party user. |
| A paid app cannot get discovery here | **ON RECORD** | `docs/monetization-strategy.md` §4 — *"Paid app … effectively dead … No discovery"*; corroborated by the **MEASURED** $2.99 app at 1K+ installs. |
| Market-trader apps are used frequently enough for advertising to matter | **UNVERIFIED** | No DAU, retention or session-frequency figure exists in this repository for any slot. The 15 % DAU/installs figure is an assumption. |
| Two recipes for this lane would be safer than one | **UNVERIFIED** | Judgement, not measurement — see §6.9. |

### 6.6 The promise contract — five promises, three `core`

Full machine-checkable form: `docs/ideation/plans/market-day-book.differentiators.json`.

| # | id | Weight | Promise | Recon evidence (label) |
|---|---|---|---|---|
| 1 | `takings-and-stock-are-one-entry` | **core** | Recording a sale takes what left the stall off the count **at the trader's own purchase cost**, so the day's figures and the stock count are never typed twice and never disagree. | `com.shrey_businessx.android` (4.0★, 100K+) review *"doesn't have a space to actually input restock quantities with cost pri[ce]"* — **MEASURED** |
| 2 | `supplier-balance-stays-editable` | **core** | What is owed to each wholesaler is carried from the same entries and can be corrected in place, so a wrong vendor balance never becomes permanent. | `com.srgroup.purchasesalesbook` (3.9★, 10K+, 249 reviews) review *"Can't edit errors in Vendor Balance… There's no clarity of Debt Purchase and Payments made for Vendors"* — **MEASURED** |
| 3 | `no-account-and-it-does-not-lose-the-day` | **core** | The book opens and records with no account and no network, and the trader owns the file — exported as CSV and PDF — so a lost sync can never cost a market day. | `gn.cashbook` (3.9★, 500K+) *"I lost my 4 days back up automatically"*; `com.shrey_businessx.android` *"My data doesn't sync across my devices even after paying for premium"* — **MEASURED** |
| 4 | `bought-once-ads-go` | supporting | One $4.99 purchase removes the advertising for good — on the day sheet, the stock list and the supplier ledger — with no subscription and no upsell afterwards. | `com.shrey_businessx.android` *"Why do I still get ads...even after paying for premium"* — **MEASURED**; `com.foodmonk.rekordapp` (4.1★, 1M+) *"after one month they will ask for payment to see or open your file"* — **MEASURED** |
| 5 | `one-day-sheet-per-market` | supporting | Each market day is one sheet — float in, takings by payment kind, pitch fee, stock that left, and the counted-against-recorded difference — so a cash-up takes one screen, not a hunt. | **INFERRED** from the shelf's `Stall POS Checkout`, `Daily Sales Record` and `Daily Book` titles and its `missing-feature` complaints; no incumbent's day sheet was read. |

Every promise is written `deferred` in the plan file, which is the state `scripts/verify.mjs` fails on,
so the slot cannot be called done with the easy ones delivered.

### 6.7 Build estimate, permissions, capabilities, server

| Item | Value |
|---|---|
| Build estimate | **5–7 agent hours**. MEASURED baseline context: the first slot's scaffold-to-last-build span is ON RECORD at 4 h 49 m as a lower bound (`docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §1). This build is heavier than a calculator: a local store over days, stock lines, suppliers and payments; the tally and ribbon components; CSV **and** PDF export; and the ads plus billing capability wiring. |
| Permissions | **`INTERNET`** only, for advertising. **`AD_ID`** is merged by the ads SDK manifest, not declared by the app. No camera, no storage, no location, no photo picker — product photos are deliberately **out of scope** to keep the install prompt empty. |
| Capabilities | **`ads`** (required by `ads_led_utility`) **and `billing`** (for the one-time unlock). Extra capabilities beyond the archetype's requirement are allowed — `docs/slot-economics.md`. |
| Server | **None.** No `PurchaseAuthority` is needed, because the only product is a non-consumable unlock with `Fulfillment.LOCAL`, so there is no money-costing credit to confirm. The records never leave the device. |
| Client-side secrets | **None.** The ads SDK and Play Billing authenticate without a client secret, so `scripts/check-client-secrets.mjs` has nothing to find beyond the `AD_ID` permission the SDK merges. |
| Distinctness from our own catalogue | Checked against `registry/apps.json` `id` / `name` / `idea` only (§7). Nothing in the catalogue is a trading day book. |

### 6.8 Visual direction (one or two sentences)

**Mark:** a drawn **five-bar tally** — four upright chalk strokes crossed by a diagonal — inside a
squircle, reading at 48 dp and carrying no baked-in mask or shadow; it is the trader's own counting
mark, and it is not an emoji, a clipart glyph or a stock icon. **Palette subject:** chalkboard and
chalk — ink-teal board `#102E33`, chalk `#F2EFE4`, warm paper `#EFE7D6` for sheets, and clay `#C0562F`
reserved for exactly one meaning, money owed and stock below its reorder line; deliberately not the
reference slot's indigo and amber. **The component stock Material does not have:** a **tally strip**
that renders a running count as five-bar tally groups rather than a number, and a **day ribbon** whose
filled length is the day's takings, so a trader reads a month at a glance.

### 6.9 Kill criteria, and the reason there is one recipe rather than two

| Criterion | Value |
|---|---|
| `windowDays` | **60** — two months past production, long enough for market-day seasonality to show once |
| `minInstalls` | **2,000** |
| `minRevenueUsd` | **150** — consistent with the floor: 2,000 installs at 2 % = 40 unlocks × $4.07 = **$163** |

`node scripts/check-outcomes.mjs --slot market-day-book` verdicts these against a recorded reading;
`MISSED` names the kill command, and `node scripts/kiln-status.mjs --kill market-day-book --reason "…"`
records the decision. Both are honest outcomes.

**Why one recipe and not two.** The lane admits two candidate jobs — *what did I take today* (takings)
and *what is left and what do I owe* (stock) — and `pop up shop inventory tracker` came back
`candidate`, so proposing both was tempting. They are refused as two recipes because they are **the
same trader, the same shelf and the same wedge, differing only in which half of one ledger is on
screen**; a single sentence describes both. Google Play's Repetitive Content policy treats several of
our own near-identical apps as a violation whose enforcement reaches **account termination and the
removal of every app in the catalogue** (`docs/monetization-strategy.md` §1.2), and that cost is not
worth a second listing. The lane's two candidate jobs are delivered as two halves of one book.

### 6.10 Honest unknowns — listed, not omitted

1. **Install volume for a niche trader app is UNVERIFIED.** Nothing measured here says a
   market-trader-specific book can reach ~12,300 installs/month; the category's volume is proven and
   the niche's is not. This is the proposal's largest risk.
2. **Conversion rate is UNVERIFIED.** 1–3 % is `docs/monetization-strategy.md` §4's own figure with no
   source read; a 3× error here moves the installs requirement by 3×.
3. **DAU-per-install is UNVERIFIED, and it is the assumption the whole advertising stream rests on.**
   15 % is my assumption for a work-day tool; no slot has measured retention, DAU or session frequency.
4. **The market-stall sub-query is a graveyard and I cannot tell whether that is supply or demand.**
   `QuickStall` at 10+ installs and `Stall POS Checkout` unrated at 5K+ may mean nobody wants a
   stall-specific app, or that nobody has built one well. §5 treats the reading as unproven, which is
   the safe direction, and it is the reading this proposal's own shelf could flip.
5. **No user of this app was spoken to.** Every complaint quoted is Play review text, sampled three
   per page at Play's discretion.
6. **`purchase_only_no_ads` was rejected on `docs/monetization-strategy.md` §4's prose, not on a
   measurement of this audience's ad tolerance.** No review in the corpus was classified as
   ad-hostility — the closest thing to evidence here, and it is weak.
7. **PDF export carries a cost I have estimated, not measured** (a layout engine, fonts, a file-share
   flow), and it is the cheapest thing to cut if the build overruns.
8. **Whether the Play Console fee is 15 % or higher for this account is ON RECORD as UNVERIFIED**
   (`docs/monetization-strategy.md` §2.2); at 30 % the net per unlock falls to about $3.31 and 246
   unlocks becomes 302.

---

## 7. Jobs already taken, and the boundary check

Read from `registry/apps.json`, **identity fields only** (`id`, `name`, `idea`) — MEASURED:

| id | idea (as recorded) |
|---|---|
| `smoke` | Pipeline smoke test, proves the factory pipeline end to end |
| `pipe-proof` | Throwaway pipeline proof, no hand edits |
| `pipe-sizing` | US fuel gas pipe sizing workbook |
| `teacher-gradebook` | Private offline classroom record book a teacher owns — grades, attendance, contacts, export |

**Nothing in our own catalogue is a trading day book.** `teacher-gradebook` is the nearest neighbour —
an offline record book with a single unlock — and it is a different job for a different user, which is
the test that matters; it is also why this recipe's archetype and component vocabulary are derived
independently of it rather than inherited (`.cursor/rules/visual-identity.mdc`).

Artifact check that the boundary held:

```bash
rg -n "apps/[0-9]{4}-" docs/ideation/ && echo "a proposal is reading slot trees" || echo "clean"
```

---

## 8. Reproducing this run

```bash
node scripts/scan-market.mjs --concurrency 4 \
  --query "market stall sales tracker" --query "food truck sales log" \
  --query "craft fair sales tracker" --query "farmers market sales log" \
  --query "pop up shop inventory tracker" --query "daily takings book" \
  --query "habit tracker" --query "bjj training log"

node scripts/scan-titles.mjs --concurrency 3 \
  --query "market stall sales tracker" --query "food truck sales log" \
  --query "craft fair sales tracker" --query "farmers market sales log" \
  --query "pop up shop inventory tracker" --query "daily takings book" \
  --on-job "\b(takings|sales|till|cash|stock|inventory|market|stall|vendor|fair|shop|store|receipt|ledger|book|tracker|log|pos|merch|product|order|trade)\b" \
  --off-job "\b(clipart|wallpaper|game|games|photo|video|music|weather|bank|invest|crypto|forex|job|career|dating|quiz|camera|font|keyboard|vpn)\b"

node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "daily takings book" --query "market stall sales tracker"
node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "pop up shop inventory tracker" --query "craft fair sales tracker"
node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "food truck sales log" --query "farmers market sales log"

node scripts/scan-complaints.mjs --query "daily takings book" --top 6 --concurrency 2
node scripts/scan-complaints.mjs --query "market stall sales tracker" --top 6 --concurrency 2
node scripts/scan-complaints.mjs --package com.shrey_businessx.android --package com.kutirsoft.dailysalesrecord \
  --package com.srgroup.purchasesalesbook --package gnhub.ledgerbook.pro --package lk.nextbots.invy --package com.denny.quickstall

node scripts/check-economics.mjs --economics docs/ideation/plans/market-day-book.economics.json
node scripts/check-differentiators.mjs --differentiators docs/ideation/plans/market-day-book.differentiators.json
```

---

## 9. The falsifiable claim this proposal rests on

**Pocket:** money is already on display in the one-person trader's sales-and-stock record shelf —
`cashbook.cashbook` at **4.5★ over 145,000 reviews and 10M+ installs**, `com.kutirsoft.dailysalesrecord`
at **4.5★ / 4,240 / 500K+**, `com.mlab.stock.management` at **4.2★ / 2,230 / 100K+** — and a **paying**
user of a 100K+ incumbent has written down both halves of the wedge: the missing restock-entry-at-cost
and the advertising that survives payment.

**Evidence label:** **MEASURED** — `node scripts/enrich-market.mjs` and
`node scripts/scan-complaints.mjs --package com.shrey_businessx.android`, both run in this session;
verbatim quotes and exact review counts in §3.1 and §4.1.

**What would flip it to FAIL, as one observation:** **if the enriched incumbents on the
`daily takings book` and `pop up shop inventory tracker` shelves turned out to be unrated micro-apps
with no paid tier anywhere in the pocket, the pocket would have no demand reading and no proof that
this audience pays at all** — and the recipe would have to be refused for the same revenue reason the
stall, fair, truck and farmers-market queries were refused. Concretely, the claim fails if a re-run
shows `cashbook.cashbook` below 4.2★, or below 10,000 reviews, or with no in-app purchase declared,
**and** no other card in either shelf above 100K+ installs at ≥ 4.0★ declaring a paid tier. A second,
cheaper way to flip it: **if `scan-titles` on `daily takings book` shows a hand job-share below 0.65
once the reading-tracker cards are removed**, the shelf is off-job and there is nothing here to price.

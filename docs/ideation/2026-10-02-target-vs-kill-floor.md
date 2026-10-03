# Target vs kill floor — $1,000/month against the scouts' own thresholds

**What this is.** The money owner's reconciliation of one contradiction the orchestrator found and the
owner must decide: the factory's planning target is **$1,000/month**, and the four plans now sitting in
`docs/ideation/plans/` carry kill floors that imply **$67–$300/month**. A slot can pass its own kill
criterion and still earn a fifteenth of the target. This file answers whether the target is the wrong
number or the floors are, gives a floor formula, and recomputes each plan.

**What this is not.** It is not a re-price of the lanes — that is
`docs/ideation/2026-10-02-lane-pricing-worksheet.md`, which stands. It is not a proposal, not a slot and
not a kill decision: killing a `MISSED` slot is the owner's recorded act
(`docs/monetization-owner.md`), never this role's. It changes no plan file by itself.

**Labels.** MEASURED — I read the file or ran the command in this session; ON RECORD — the file states
it, `path:line` given; INFERRED — my reasoning from those; UNVERIFIED — carried without evidence, listed
again in §7. Nothing under `apps/**` was opened. No slot is reported on.

---

## 0. The four plans, read from their own files

MEASURED — every row below is read from the plan's own `economics.json` in this session
(`docs/ideation/plans/<slug>.economics.json`), not from any summary.

| Plan | `archetype` | `priceUsd` | `windowDays` | `minInstalls` | `minRevenueUsd` |
|---|---|---:|---:|---:|---:|
| `part-107-drills` | `purchase_led_single_unlock` | 9.99 | 90 | 2,000 | **300** |
| `tap-to-say` | `purchase_led_single_unlock` | 6.99 | 90 | 8,000 | **900** |
| `rook-encounter-generator` | `purchase_led_repeat_sales` | 2.99 | 90 | 2,500 | **200** |
| `rast-saz-oud-tuner` | `purchase_led_single_unlock` | 4.99 | 90 | 2,500 | **200** |

All four windows are **90 days**. The two new lanes named in the dispatch — an advisor/troubleshooter
lane and a planner/optimiser lane — have **no files yet**: MEASURED, `docs/ideation/2026-10-02-*.md`
returns nine files and none is an advisor, troubleshooter, planner or optimiser report
(`docs/ideation/2026-10-02-authored-learning-and-drills.md`, `…-generator-and-creative-tools.md`,
`…-niche-real-time-tools.md`, `…-assistive-and-communication.md`, `…-verification-lane-b.md`,
`…-verification-lane-c.md`, `…-leader-head-to-head.md`, `…-leader-head-to-head-tap-to-say.md`,
`…-lane-pricing-worksheet.md`). So this file prices the **four** that exist; the two new lanes get no
number here and must not be scaffolded until they do.

---

## 1. Is $1,000/month the right planning number — and is it a kill floor?

**The source, quoted.** `docs/monetization-strategy.md` uses the figure twice, and labels it both times:

- `docs/monetization-strategy.md:135` — *"Illustrative order-of-magnitude implications for
  **$1,000/month** (≈ $33/day), derived from the benchmarks above:"*
- `docs/monetization-strategy.md:149` — *"These are order-of-magnitude estimates from published
  publisher benchmarks, not forecasts for a specific app."*
- `docs/monetization-strategy.md:131` — the same section's own heading for its worked example:
  *"**Illustrative example** (published figures, not a forecast for any slot)"*.
- `docs/monetization-strategy.md:444` — reused once more in §3.6: *"…can reach $1,000/month."*

**The base rate, so the target is read honestly.** ON RECORD,
`docs/research/2026-09-26-teacher-gradebook-evidence.md`:

| Claim | Value | Line |
|---|---|---|
| Apps that never reach $1,000/month | **over 80 %** | :327 |
| Median active app revenue | **under $50/month** | :328 |
| Apps tracked that clear $1,000/month | **18.3 %** | :329 |
| Free apps with IAP that clear $1,000/month | **47.1 %** | :330 |
| Paid apps without IAP that clear $1,000/month | **2.4 %** | :331 |
| Median install-to-paid | **1.9 %** (top decile 8.5 %) | :332 |

**The verdict, in two halves.**

1. **Yes, $1,000/month is the right *planning* number — keep it.** The factory needs a target to design
   a price and a volume against, it is the only number the repository has, and the alternatives are
   worse: an empty target makes every volume question unanswerable, and a "measured" target cannot be
   set from this account because the factory has **never observed a single sale** — ON RECORD,
   `docs/monetization-owner.md:130` (*"The factory has no revenue API yet"*), and every plan's
   `assumptionsUnverified` says the install-to-unlock conversion is assumed, never measured. A lower
   target would be exactly as illustrative as this one and would license an even lower floor.
2. **No, it must not drive a kill floor.** The source calls it *illustrative*,
   *order-of-magnitude*, and *not a forecast*, and the base rate says **over 80 % of apps never clear
   it**. A kill floor is not the target: it is the line below which continuing is not worth the
   maintenance, and `docs/monetization-owner.md` makes `MISSED` mean *archive the slot*. Setting the
   floor at the target would archive ~82 % of slots at the first window even when the target is
   eventually reachable — and setting it as low as the plans do (below) means the factory can never
   kill a slot that cannot reach the target, which makes **the target decorative**. The floor must be
   an explicit *fraction* of the target, written down as a formula.

So the defect is not the target. It is that **four floors were chosen independently**, each from the
scout's own instinct, and none was derived from the target at all.

---

## 2. What the kill floor should be — a formula, and each plan's recomputed floor

### 2.1 The recorded anchors the formula must sit between

- **Upper anchor — the target: `$1,000/month`** (ON RECORD, `docs/monetization-strategy.md:135`). Never
  a floor: the base rate (§1) says it is cleared by 18.3 % of apps.
- **Lower anchor — `$300/month`** (ON RECORD, `docs/ideation/2026-10-01-pricing-and-archetype-worksheet.md:119-121`):
  *"`$300/month` is the lower anchor this worksheet carries so a proposal can argue for a smaller app
  honestly."* This is the repository's own recorded floor for a *smaller* app, and it is **30 % of the
  target**.
- For contrast, the repository's other documented floor is the schema example
  `{ "windowDays": 30, "minInstalls": 500, "minRevenueUsd": 50 }`
  (ON RECORD, `docs/slot-economics.md`), i.e. **$50/month = 5 % of the target** — lower than every plan
  below, and the anchor `docs/ideation/2026-10-01-pricing-and-archetype-worksheet.md` §6.3 recommends.

### 2.2 The formula

The validator fixes only the *shape* of a kill criterion — `windowDays` in `7..180`,
`minInstalls >= 1`, `minRevenueUsd >= 1` (ON RECORD, `docs/slot-economics.md`) — and imposes **no
relationship to the target**. That is the gap the formula fills.

```
                        windowDays
minRevenueUsd  =  F  ×  ──────────  ×  $1,000
                            30

with F = 0.30  (the recorded $300/month lower anchor ÷ the $1,000/month target)
```

- **`F = 0.30` is INFERRED, not invented.** §2.1 records `$300/month` as the repository's lower anchor;
  dividing it by the target gives 30 %, and applying it over the window gives the floor. It is
  deliberately the *documented* fraction rather than a fresh one.
- **The install floor is derived from the revenue floor, never chosen beside it**, so the two cannot
  contradict each other (ON RECORD, `docs/ideation/2026-10-01-pricing-and-archetype-worksheet.md:510-524`):
  ```
  minInstalls = ceil( minRevenueUsd ÷ netPerSale ÷ conversion_optimistic )
  netPerSale  = priceUsd × (1−0.15)(1−0.02)(1−0.02) = priceUsd × 0.81634
  ```
  The rate is the plan's own 2 % assumption and is UNVERIFIED (§7); the plans already use it.

### 2.3 Each plan's recomputed floor (all windows are 90 days)

```
minRevenueUsd = 0.30 × (90/30) × $1,000 = 0.30 × 3 × $1,000 = $900
```

| Plan | price | net/sale | floor **now** | floor **recommended** | change |
|---|---:|---:|---:|---:|---|
| `part-107-drills` | $9.99 | $8.1552 | $300 / 90d | **$900 / 90d** | **3.0 ×** |
| `tap-to-say` | $6.99 | $5.7062 | $900 / 90d | **$900 / 90d** | **1.0 × — already compliant** |
| `rook-encounter-generator` | $2.99 | $2.4409 | $200 / 90d | **$900 / 90d** | **4.5 ×** |
| `rast-saz-oud-tuner` | $4.99 | $4.0735 | $200 / 90d | **$900 / 90d** | **4.5 ×** |

The floor is a **dollar** floor and is therefore identical across plans with different prices; the
price changes how many sales and installs clear it, which is the next section. `tap-to-say` is the one
plan already at the derived floor — the other three are 3 to 4.5 times below it.

**Why 30 %, and what I rejected.** INFERRED. A first 90-day window is a ramp, so 100 % of target would
archive a slot that a second window would have saved; 10 % (the `part-107` floor) keeps a slot that is
earning a tenth of the target — the target then never disciplines anything. 30 % is the one fraction
the repository already records as its "smaller app" anchor, and it makes the 90-day floor a single
memorable number ($900). I rejected raising `F` to 50 % ($1,500/90d): it would put the first-window
floor above what the *median* app earns in a year (under $50/month, ON RECORD) and would trade the
"never kill on missing data" discipline for a bar nothing measured supports. I rejected leaving `F`
implicit per plan: that is the current defect.

---

## 3. Sales/month and installs/month, per plan, to both floors

MEASURED — division computed in this session (`node -e`), using each plan's own `priceUsd`, the local
net multiplier `0.81634` (ON RECORD, `docs/ideation/2026-10-02-lane-pricing-worksheet.md` §1.1), and the
plan's own **2 %** install-to-unlock assumption (UNVERIFIED, §7). Sales are rounded **up**
(a fraction of a sale does not clear a floor). A 90-day window is **3 months**, so
`sales/month = (revenueFloor ÷ 3) ÷ netPerSale`.

### 3.1 The divisions, written out

```
net/sale($9.99) = 9.99 × 0.81634 = $8.1552    net/sale($6.99) = 6.99 × 0.81634 = $5.7062
net/sale($2.99) = 2.99 × 0.81634 = $2.4409    net/sale($4.99) = 4.99 × 0.81634 = $4.0735
```

**`part-107-drills` — $9.99, net $8.1552**
```
(a) own floor  $300/90d = $100.00/mo  →  100.00 ÷ 8.1552 = 12.26 → 13 sales/mo → 13 ÷ 0.02 =   650 installs/mo
(b) target   $1,000/mo               → 1000.00 ÷ 8.1552 = 122.6 → 123 sales/mo → 123 ÷ 0.02 = 6,150 installs/mo
```
**`tap-to-say` — $6.99, net $5.7062**
```
(a) own floor  $900/90d = $300.00/mo  →  300.00 ÷ 5.7062 = 52.57 → 53 sales/mo → 53 ÷ 0.02 = 2,650 installs/mo
(b) target   $1,000/mo               → 1000.00 ÷ 5.7062 = 175.2 → 176 sales/mo → 176 ÷ 0.02 = 8,800 installs/mo
```
**`rook-encounter-generator` — $2.99, net $2.4409**
```
(a) own floor  $200/90d = $66.67/mo   →  66.67 ÷ 2.4409 = 27.31 → 28 sales/mo → 28 ÷ 0.02 = 1,400 installs/mo
(b) target   $1,000/mo               → 1000.00 ÷ 2.4409 = 409.7 → 410 sales/mo → 410 ÷ 0.02 = 20,500 installs/mo
```
**`rast-saz-oud-tuner` — $4.99, net $4.0735**
```
(a) own floor  $200/90d = $66.67/mo   →  66.67 ÷ 4.0735 = 16.37 → 17 sales/mo → 17 ÷ 0.02 =   850 installs/mo
(b) target   $1,000/mo               → 1000.00 ÷ 4.0735 = 245.5 → 246 sales/mo → 246 ÷ 0.02 = 12,300 installs/mo
```

The (b) columns reproduce the lane worksheet's own table
(`docs/ideation/2026-10-02-lane-pricing-worksheet.md` §1.2: $9.99 → 123 / 6,150; $6.99 → 176 / 8,800;
$2.99 → 410 / 20,500; $4.99 → 246 / 12,300) exactly.

### 3.2 The same four plans against the **recommended** floor

```
recommended floor $900/90d = $300.00/mo

part-107  $300.00 ÷ 8.1552 = 36.8 → 37 sales/mo → 37 ÷ 0.02 = 1,850 installs/mo
tap-to-say $300.00 ÷ 5.7062 = 52.6 → 53 sales/mo → 53 ÷ 0.02 = 2,650 installs/mo
rook       $300.00 ÷ 2.4409 = 122.9 → 123 sales/mo → 123 ÷ 0.02 = 6,150 installs/mo
rast       $300.00 ÷ 4.0735 = 73.6 → 74 sales/mo → 74 ÷ 0.02 = 3,700 installs/mo
```

### 3.3 The one table

| Plan | price | floor now ($/90d) | floor as % of target | sales/mo to **own floor** | installs/mo to **own floor** | sales/mo to **target** | installs/mo to **target** | sales/mo to **recommended floor** | installs/mo to **recommended floor** |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `part-107-drills` | $9.99 | $300 | 10.0 % (≈ 1/10) | 13 | **650** | 123 | **6,150** | 37 | 1,850 |
| `tap-to-say` | $6.99 | $900 | 30.0 % (≈ 1/3.3) | 53 | 2,650 | 176 | **8,800** | 53 | 2,650 |
| `rook-encounter-generator` | $2.99 | $200 | 6.7 % (≈ **1/15**) | 28 | 1,400 | 410 | **20,500** | 123 | 6,150 |
| `rast-saz-oud-tuner` | $4.99 | $200 | 6.7 % (≈ **1/15**) | 17 | 850 | 246 | **12,300** | 74 | 3,700 |

The "fifteenth of the target" claim is exact for `rook` and `rast`
($1,000 ÷ ($200/3) = **15.0 ×**); `part-107` is a tenth and `tap-to-say` is a third.

---

## 4. Which plan has the lowest installs bar, which the highest — and whether that is the winnability ranking

**Lowest bar to $1,000/month: `part-107-drills` — 6,150 installs/month.** Highest:
**`rook-encounter-generator` — 20,500 installs/month** (3.3 × the lowest). MEASURED, §3.3.

The ordering is **purely the price ordering**:

```
installs/mo to target ≈ $1,000 ÷ (priceUsd × 0.81634) ÷ 0.02
                      = 61,274 ÷ priceUsd        (constant, price-independent)
```

So $9.99 → 6,135; $6.99 → 8,766; $4.99 → 12,279; $2.99 → 20,494. Higher price, fewer installs needed —
arithmetic, not strategy.

**The leader-winnability ranking is different.** ON RECORD,
`docs/ideation/2026-10-02-leader-head-to-head-tap-to-say.md` §8 ranks all four by how well the promised
wedge survives contact with the leader of its primary query:

| Rank | Recipe | Verdict |
|---:|---|---|
| 1 | `part-107-drills` | PLAUSIBLE |
| 2 | `rook-encounter-generator` | PLAUSIBLE |
| 3 | `tap-to-say` | WEAK |
| 4 | `rast-saz-oud-tuner` | WEAK |

**They are not the same ranking, and the mismatch is the useful part.** `part-107` is first on both, and
`rast` is last or near-last on both — those two agree. The two in the middle disagree:

- **`rook` is the second-most-winnable and has the worst installs bar.** Its head-to-head verdict rests
  on a *downloaded, weak generator tier* whose users complain about the wedge verbatim; its price is
  $2.99 because the recipe sells repeat packs, and $2.99 is the most expensive price *in installs*, not
  in dollars. Two honest caveats: the 20,500 assumes **one pack per buyer**, so genuine repeat purchase
  lowers it proportionally (UNVERIFIED — no conversion reading exists, §7), and it is the only
  `purchase_led_repeat_sales` plan, so its revenue does not depend on a single purchase event.
- **`tap-to-say` is the third-most-winnable and has the second-lowest bar.** Its price ($6.99) buys a
  modest install bar, but the head-to-head is WEAK because the criterion leader is the *same job*, is
  4.5★/3,036/500K+ and returned 0 critical reviews — so the wedge cannot be shown to land
  (`docs/ideation/2026-10-02-leader-head-to-head-tap-to-say.md:343-350`). A cheap-ish install bar does
  not make a wedge land.

The reason the two rankings differ is that **the install bar is a function of price and the conversion
assumption only; winnability is a function of the leader's strength and whether the shelf's complaints
map to the wedge.** Price and winnability are independent, so a plan can be winnable and hardest to
volume (rook) or weakly defended and easy to volume (tap-to-say). Neither ranking is a substitute for
the other, and neither is a verdict on the money — they answer different questions.

**One more honest reading of `part-107`'s "lowest bar".** Its 6,150 is the lowest *because $9.99 is the
highest price*, and that price is the plan's own largest unknown: its `assumptionsUnverified` records
that *"Willingness to pay 9.99 dollars outright is UNVERIFIED: the only measured paying behaviour on
this shelf is a week or month subscription … so no reading shows a learner buying exam prep outright."*
So the lowest install bar is bought with the least-supported price in the set. That is a warning about
reading the ranking as readiness.

---

## 5. The single number the owner should change before any slot is scaffolded

**The kill floor. Set `minRevenueUsd` to `$900` for a 90-day window — one value, derived as 30 % of the
$1,000/month target over the window — and derive `minInstalls` from it.**

Concretely, if a plan from this set is chosen, the change to its `economics.json` before
`new-slot.mjs` runs is:

| Plan | `minRevenueUsd` now | change to | `minInstalls` now | derived from the new floor @ 2 % |
|---|---:|---:|---:|---:|
| `part-107-drills` | 300 | **900** | 2,000 | 1,850 |
| `tap-to-say` | 900 | **900 (unchanged)** | 8,000 | 2,650 |
| `rook-encounter-generator` | 200 | **900** | 2,500 | 6,150 |
| `rast-saz-oud-tuner` | 200 | **900** | 2,500 | 3,700 |

The `minInstalls` column is the §3.2 derivation at the plan's own 2 %; it is shown so the two floors
agree, and the existing install floors are inconsistent with any single conversion (e.g.
`rook`'s 2,500 installs at 2 % implies $122 of revenue, below its own $200 floor, i.e. it was derived at
a more optimistic rate than its revenue floor assumes). A plan should ship the `minRevenueUsd` floor
that follows the formula and an `minInstalls` derived from it, and say in the plan which conversion it
derived at.

**What I rejected, and why.** I rejected changing the **target** down to a "measurable" number: there is
no measured number (no sale has ever been observed — `docs/monetization-owner.md:130`), so a lower
target is equally illustrative and would only license an even lower floor. I rejected changing the
**window**: 90 days is the plans' own choice and is defensible against the unconfirmed 14-day
production gate (`docs/monetization-strategy.md` §1.1), so the window is not the defect. The number
that is wrong, and the one that repeats into every future plan, is the floor.

**What the owner should do with this, in one line.** Keep `$1,000/month` as the target, write
`minRevenueUsd = 0.30 × (windowDays/30) × $1,000` into the contract (`docs/slot-economics.md`), and
refuse to scaffold a plan whose floor sits below it — starting with the three of these four that do.

---

## 6. What this does not decide

- It does **not** pick a plan. It fixes the floor all four are judged against; the choice among them
  belongs to the owner's checkpoint 1, on the wedges (the four head-to-head verdicts above are the
  input) and on the build cost.
- It does **not** prove any plan will earn. Every installs figure rests on an assumed 2 % conversion
  that no slot has ever measured.
- It does **not** change a file. `docs/slot-economics.md`, the four `economics.json` files and the
  contract in `scripts/lib/slot-economics.mjs` are edited by whoever the owner directs; this role's
  artifact is this reconciliation.

---

## 7. Assumptions and unknowns

1. **The install-to-purchase conversion is UNVERIFIED.** Every installs column uses the plans' own 2 %;
   ON RECORD, `docs/research/2026-09-26-teacher-gradebook-evidence.md:332` records the median
   install-to-paid at **1.9 %** (top decile 8.5 %), and `docs/ideation/2026-10-02-lane-pricing-worksheet.md`
   §7 calls the 2 % an assumption. At 1 % every installs figure doubles; at 5 % it drops 2.5 ×.
   **Command to settle:** a live paywall plus a dated reading in `apps/<slot>/store/outcomes.json`, read
   by `node scripts/check-outcomes.mjs --slot <slug>`.
2. **`F = 0.30` is INFERRED, not measured.** It is the repository's recorded `$300/month` lower anchor
   (`docs/ideation/2026-10-01-pricing-and-archetype-worksheet.md:119-121`) divided by the target. No
   measurement shows 30 % is the optimal floor. **Command to settle:** the first real reading — the exit
   verdict of a slot that misses a floor above 30 % and a slot that clears one.
3. **The floors' window basis is unstated.** All four plans give `minRevenueUsd` with no statement of
   whether the reading is cumulative-to-date or a period figure, and `docs/monetization-owner.md` §6.2
   requires the basis to be stated because the check compares only the newest reading. This file assumes
   **window-accumulated** revenue. **Command to settle:** read the slot's `store/outcomes.json` basis
   once one exists.
4. **The net multiplier 0.81634 and its rates are ASSUMPTIONS**, not this account's fees — ON RECORD,
   `docs/monetization-strategy.md` §2.2 (`F_billing = 0.15` valid "only under the first-$1M annual tier";
   `F_refund = 0.02`; `F_fx = 0.02`).
5. **`rook`'s 20,500 installs/mo assumes one pack per buyer**, which contradicts its own
   `purchase_led_repeat_sales` design; the true bar is lower by the repeat factor, which is unmeasured.
6. **The two new lanes are absent.** MEASURED — no advisor/troubleshooter or planner/optimiser report
   exists in `docs/ideation/`. They are not priced here.
7. **The `$1,000/month` target is illustrative**, by its own source (`docs/monetization-strategy.md:149`)
   and cleared by only 18.3 % of apps (`…-evidence.md:329`). §1 keeps it as the planning number; that is
   a decision, not a measurement.
8. **The `$50/month` schema example** (`docs/slot-economics.md`) is a third, lower anchor this file does
   not adopt; it is named in §2.1 so the reader knows the repository's own floors are not consistent
   either.

---

## 8. Claim ledger

`CLAIM / LABEL / EVIDENCE`.

| # | Claim | Label | Evidence |
|--:|---|---|---|
| 1 | The target is labelled illustrative and "not a forecast for a specific app" | ON RECORD | `docs/monetization-strategy.md:135`, `:149`, `:131` |
| 2 | Over 80 % of apps never reach $1,000/month; median under $50/month; 18.3 % clear it | ON RECORD | `docs/research/2026-09-26-teacher-gradebook-evidence.md:327-329` |
| 3 | Median install-to-paid is 1.9 %, top decile 8.5 % | ON RECORD | `docs/research/2026-09-26-teacher-gradebook-evidence.md:332` |
| 4 | $300/month is the repository's recorded "lower anchor" | ON RECORD | `docs/ideation/2026-10-01-pricing-and-archetype-worksheet.md:119-121` |
| 5 | Kill criteria are validated for shape only — `windowDays 7..180`, `minInstalls>=1`, `minRevenueUsd>=1` — with no relation to the target | ON RECORD | `docs/slot-economics.md` § The contract |
| 6 | The four plans' prices and kill criteria (9.99/300/2000; 6.99/900/8000; 2.99/200/2500; 4.99/200/2500; all 90d) | MEASURED | `docs/ideation/plans/{part-107-drills,tap-to-say,rook-encounter-generator,rast-saz-oud-tuner}.economics.json` |
| 7 | nets: $8.1552 / $5.7062 / $2.4409 / $4.0735; sales/mo and installs/mo per §3.1–3.2 | MEASURED | `node -e` in this session, `priceUsd × 0.81634`, then `÷ 0.02` |
| 8 | The (b)-target columns reproduce the lane worksheet exactly | MEASURED | `docs/ideation/2026-10-02-lane-pricing-worksheet.md` §1.2 |
| 9 | Leader-winnability ranks part-107 #1, rook #2, tap-to-say #3, rast #4 | ON RECORD | `docs/ideation/2026-10-02-leader-head-to-head-tap-to-say.md` §8 |
| 10 | tap-to-say's criterion leader is the same job at 4.5★/3,036/500K+, 0 critical of 3 | ON RECORD | `docs/ideation/2026-10-02-leader-head-to-head-tap-to-say.md:343-350` |
| 11 | part-107's $9.99 willingness-to-pay is UNVERIFIED in its own words | ON RECORD | `docs/ideation/plans/part-107-drills.economics.json` `assumptionsUnverified[0]` |
| 12 | The factory has no revenue API; readings are hand-recorded; no sale has been observed | ON RECORD | `docs/monetization-owner.md:130`; `docs/ideation/2026-10-02-lane-pricing-worksheet.md` §7.1 |
| 13 | The two new lanes have no files | MEASURED | `docs/ideation/2026-10-02-*.md` glob returns 9 files, none advisor/planner/optimiser |
| 14 | Install floor and revenue floor must be derived together, installs at the optimistic conversion | ON RECORD | `docs/ideation/2026-10-01-pricing-and-archetype-worksheet.md:510-524` |
| 15 | That `F = 0.30` is the right fraction | **UNVERIFIED** | no measurement; it is the recorded lower anchor applied as a floor. Would be settled by a slot's first reading |
| 16 | That the installs figures are achievable on any shelf | **UNVERIFIED** | no conversion has ever been measured; every installs number is a requirement, not a forecast |
| 17 | That `rook`'s repeat packs lower its 20,500 installs bar | **UNVERIFIED** | no repeat-purchase reading exists |
| 18 | That the two new lanes would price above or below $900/90d | **UNVERIFIED** | their files do not exist |

*Written by the money owner, 2026-10-02. The plan and the reading for a real slot are
`apps/<slug>/economics.json` and `apps/<slug>/store/outcomes.json`; this file is neither and authorises
nothing.*

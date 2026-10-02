# Pricing and archetype worksheet — 2026-10-01

**What this is.** The shared arithmetic and the derivation rule that every proposal in this run is
measured against. It exists because the factory had a recorded, structural defect: **every plan,
recipe and proposal this repository had produced up to 2026-10-01 chose `purchase_led_single_unlock`**
— all six plans under `docs/ideation/plans/`, all four recipes of
`docs/ideation/2026-09-26-five-recipes.md`, the single worked example that used to sit beside the
archetype table, the reference slot, and both proposals of the 2026-10-01 recon. A hundred per cent
of the evidence taught one answer, so the factory learned it. This worksheet is the instrument that
makes a *derived* choice possible instead of an inherited one, and it holds the deduction reasoning
in one place instead of leaving each proposal to re-derive it.

**What this is not.** It is not a proposal, not a topic, not a recommendation and not a slot. **No
slot directory exists for anything described here, and this document authorises nothing** — not a
build, not a price, not a capability, not a purchase. It names no candidate, picks no winner, and it
does not price the three recon shelves being screened concurrently with it; that is a second
money-owner pass, performed against their files once those land.

**Labels.** Every number below carries one of four labels, and the label is not decoration:

| Label | Means |
|---|---|
| MEASURED | I ran a command in this session and this is what it printed. |
| ON RECORD | A file in this repository states it. The path is given. |
| INFERRED | My reasoning from MEASURED or ON RECORD facts. |
| UNVERIFIED | A number or claim carried without evidence. Listed again in §8. |

The deduction rates in §1 are **ASSUMPTION** as recorded in
`docs/monetization-strategy.md` §2.2 — that is the file's own label, and it is preserved here rather
than upgraded. Nothing in this document was invented, and no number is presented as a measurement of
this account.

**The factory is not the market.** Nothing under `apps/**` was opened to write this, and no slot is
discussed or judged.

---

## 1. The deduction rates, quoted not restated

### 1.1 What each rate is

Values, labels and conditions are as recorded in `docs/monetization-strategy.md` §2.2 (the
"Assumption set used below" table, and the prose around the `P_min` formula). Every one of them is
the file's own **ASSUMPTION**; none is a measurement of this account.

| Symbol | Value | Label as recorded | The condition under which it applies |
|---|---:|---|---|
| `F_billing` | 0.15 | **ASSUMPTION.** "The fee table is sourced (research §6.3, read 2026-09-23), but 0.15 is the *chosen default*, valid only under the first-$1M annual tier and/or the new-install case; above $1M, or for existing installs in the EEA/UK/US at standard rates, the fee rises to 20–30 % plus the 5 % billing fee." | Every Play-billed sale. The rate is tier- and install-status-dependent, so it is a decision to re-verify before pricing a real product, not a constant. Whether the fee is computed on a tax-inclusive price is **UNVERIFIED** (research §8.7). |
| `F_refund` | 0.02 | **ASSUMPTION** — "decide per slot" | Every Play-billed sale. Play refunds reach the reports as lost revenue. |
| `F_fx` | 0.02 | **ASSUMPTION** — "decide per slot; Play prices are set per currency" | Every Play-billed sale, because the listed price is per currency and the payout is not. |
| `F_fraud` | **0.20** if the client-reported purchase token is trusted; **0.01** if the token is confirmed server-side | **ASSUMPTION** (the 0.01 is "a residual, not zero") | Only where credit is **granted before payment is collected**. It is not a property of the market — §2.2: "it is the share the app *allows* by granting credit on the client's word (§3.7)". |
| `M_target` | 0.60 | **ASSUMPTION** — "decide per slot" | Not a deduction taken from a sale at all. It is the margin we want to keep **after** the other four, so it belongs in the price *derivation* (`P_min = K × C_unit`), never in a net-per-sale calculation. |

`F_fraud` is the one that repays attention and the one that has been mis-applied. Restated from
`docs/monetization-strategy.md` §3.7:

> `LOCAL` — the spend costs us nothing beyond lost revenue (a theme, a filter, a local feature). A
> device balance is sufficient; **cheating costs a sale, not cash.**
> `SERVER_COST` — each spend incurs a real cost. A `PurchaseAuthority` is **mandatory**.

### 1.2 Which rates apply to which fulfilment class

This distinction was reasoned out in the 2026-10-01 proposal
(`docs/ideation/2026-10-01-coin-catalogue-and-pilot-logbook.md` §5: *"the unlock is a boolean Play
reports, so nothing is served and there is no granted-before-paid unit — which is why `F_fraud` is
excluded from the arithmetic below"*). It lives here now so it is not re-derived per proposal.

| Rate | A **local** one-time unlock (fulfilment serves nothing) | A **server-fulfilled** purchase (`C_unit > 0`) |
|---|---|---|
| `F_billing` 0.15 | **Applies.** Play processes the sale. | **Applies.** |
| `F_refund` 0.02 | **Applies.** Play refunds one-time purchases. | **Applies.** |
| `F_fx` 0.02 | **Applies.** | **Applies.** |
| `F_fraud` | **Does not apply.** There is no unit granted ahead of payment: the entitlement *is* Play's own record, recovered by `restore()` (§3.1). A patched client that grants itself the unlock costs a **sale**, not cash — there is no provider bill behind it. | **Applies**, at **0.01** if the token is confirmed server-side. The 0.20 value exists for a slot that trusts the client's word, which `BillingCatalog.requiresPurchaseAuthority()` refuses for a `SERVER_COST` catalog. |
| `M_target` 0.60 | **Has no arithmetic role.** With `C_unit = 0` there is no per-unit cost to recover, so the margin is the whole net; `M_target` does not constrain the price and must not be subtracted from it. | **Applies in the derivation**, not as a deduction: `P_min = K × C_unit` with `K = 1 / [(1−F_fraud)(1−F_billing)(1−F_refund)(1−F_fx)(1−M_target)]`. |

**The net multiplier is therefore not one number.** For a local unlock:

```
net multiplier (local) = (1 − F_billing)(1 − F_refund)(1 − F_fx)
                       = 0.85 × 0.98 × 0.98
                       = 0.81634                          → 81.63 % of the listed price
```

MEASURED (computed in this session). For a server-fulfilled purchase the multiplier on the list price
is additionally scaled by `(1 − F_fraud)`, but the price itself must be derived, not chosen:

```
divisor (server-confirmed, M_target = 0.60)
   = (1−0.01)(1−0.15)(1−0.02)(1−0.02)(1−0.60)
   = 0.99 × 0.85 × 0.98 × 0.98 × 0.40 = 0.32327
K = 1 / 0.32327 = 3.0934                              ← required multiple of C_unit
```

MEASURED. So a `SERVER_COST` pack priced at `P_min` nets `3.0934 × 0.80818 = 2.5000 × C_unit`, leaving
a margin of `1.5 × C_unit` — which is 60 % of the net, i.e. exactly `M_target`. For reference the
file's own worked example, on the client-trusted `F_fraud = 0.20`, gives `K = 3.83`. INFERRED: the
zero-margin floor on the server-confirmed path is `K = 1/0.80818 = 1.24×`, so even a
pack sold at cost needs a 1.24× multiple of `C_unit`.

**Consequences a proposal must honour, and the three defects this section is here to catch:**

1. A proposal for a **local** unlock that subtracts `F_fraud` is being spuriously conservative by
   ~1 percentage point of net — minor, but it is evidence the author is applying rates by habit rather
   than from the fulfilment class.
2. A proposal for a **`SERVER_COST`** purchase that omits `F_fraud` from either the arithmetic or the
   plan is the serious version of the same defect, because the missing rate is the symptom of a
   missing decision: **a slot that sells money-costing credit is a server project**, and
   `docs/monetization-strategy.md` §3.7 and `.cursor/rules/ideation-gate.mdc` both require that to be
   stated **before** the idea is approved — "a server is a cost to state, not a detail to discover".
3. A proposal for **any** purchase-led shape on the `SERVER_COST` path must be paired with the
   `billing` capability *and* a named `PurchaseAuthority` story, and `.cursor/rules/client-secrets.mdc`
   forbids shipping it as a client-only key.

---

## 2. The net-per-sale table

Local one-time unlock, net multiplier **0.81634** (MEASURED division; the three rates are ON RECORD
ASSUMPTIONS). `$1,000/month` is the target `docs/monetization-strategy.md` §2.1 uses as its own
illustrative figure; `$300/month` is the lower anchor this worksheet carries so a proposal can argue
for a smaller app honestly. A 30-day month is assumed for the per-day figures, and that is an
INFERRED convention, not a recorded one.

### 2.1 One row in full

```
price           = $4.99
net multiplier  = (1−0.15)(1−0.02)(1−0.02) = 0.85 × 0.98 × 0.98 = 0.81634
net per sale    = 4.99 × 0.81634 = $4.0735  →  $4.07

sales for $1,000/month = 1000 ÷ 4.0735 = 245.5  →  246 sales/month  (≈ 8.2/day)
sales for   $300/month =  300 ÷ 4.0735 =  73.6  →   74 sales/month  (≈ 2.5/day)

at a 2 % install-to-purchase rate:
installs/month = 246 ÷ 0.02 = 12,300 installs/month for $1,000
```

### 2.2 Every price point

MEASURED (computed in this session). Sales figures are rounded **up** to the next whole sale, because
a fraction of a sale does not clear the target.

| Price | Net multiplier | Net per sale | Sales/mo for **$1,000** | Sales/mo for **$300** |
|---:|---:|---:|---:|---:|
| $1.99 | 0.81634 | $1.6245 | **616** | **185** |
| $2.99 | 0.81634 | $2.4409 | **410** | **123** |
| $3.99 | 0.81634 | $3.2572 | **308** | **93** |
| $4.99 | 0.81634 | $4.0735 | **246** | **74** |
| $6.99 | 0.81634 | $5.7062 | **176** | **53** |
| $9.99 | 0.81634 | $8.1552 | **123** | **37** |
| $14.99 | 0.81634 | $12.2369 | **82** | **25** |

### 2.3 The installs those sales imply

Installs/month = sales/month ÷ install-to-purchase rate. **The conversion rates are UNVERIFIED
assumptions, not measurements.** No slot has ever reported a sale to this factory
(`node scripts/kiln-status.mjs` reports the live slot as `NO-DATA`), the 2 % used in the 2026-10-01
proposal is its own stated unknown, and the only figure the repository holds at all is §4's
"~1–3 %" for one-time IAP — which that same file marks **UNVERIFIED**, "no source read in this
session". So **1 % and 2 % bracket the only recorded range, and 5 % is an optimistic bound supported
by no recorded figure.** A proposal that leans on the 5 % column is leaning on the weakest number in
this document.

For **$1,000/month**:

| Price | Sales/mo | Installs @ 1 % | Installs @ 2 % | Installs @ 5 % |
|---:|---:|---:|---:|---:|
| $1.99 | 616 | 61,600 | 30,800 | 12,320 |
| $2.99 | 410 | 41,000 | 20,500 | 8,200 |
| $3.99 | 308 | 30,800 | 15,400 | 6,160 |
| $4.99 | 246 | 24,600 | 12,300 | 4,920 |
| $6.99 | 176 | 17,600 | 8,800 | 3,520 |
| $9.99 | 123 | 12,300 | 6,150 | 2,460 |
| $14.99 | 82 | 8,200 | 4,100 | 1,640 |

For **$300/month**:

| Price | Sales/mo | Installs @ 1 % | Installs @ 2 % | Installs @ 5 % |
|---:|---:|---:|---:|---:|
| $1.99 | 185 | 18,500 | 9,250 | 3,700 |
| $2.99 | 123 | 12,300 | 6,150 | 2,460 |
| $3.99 | 93 | 9,300 | 4,650 | 1,860 |
| $4.99 | 74 | 7,400 | 3,700 | 1,480 |
| $6.99 | 53 | 5,300 | 2,650 | 1,060 |
| $9.99 | 37 | 3,700 | 1,850 | 740 |
| $14.99 | 25 | 2,500 | 1,250 | 500 |

**What the table is for.** It is the yardstick a proposal's demand evidence has to be held against.
The measured shelf leaders in this factory's own recon carry install buckets like `500K+` and `10M+`;
those are **lifetime** figures, not monthly. So a proposal must state which column it believes it can
reach, why, and what in the parent shelf's measured install buckets supports the belief — a shelf
whose reachable middle sits at `10K+` lifetime installs has no route to the `30,800`-installs-at-1 %
cell, and saying so is a revenue reason to refuse or to re-price, not pessimism. Two roundings
matter and are deliberate: prices must land on a **purchasable Play price point**, and the set of
those points is **UNVERIFIED** (research §8.8) — a Play Console step, not something this document can
establish. Play rounds, and so the listed price the user pays is the one the fee is taken on.

---

## 3. The ads-led equivalent

### 3.1 The recorded ads arithmetic

`docs/monetization-strategy.md` §2.1: revenue per day = `DAU × ARPDAU`, with **no per-request cost to
subtract**. The benchmarks are "published 2026 benchmarks by format (ads only, not our own
measurements)":

| Format | Tier-1 eCPM | Global eCPM |
|---|---:|---:|
| Banner | $0.50–1.50 | $0.20–0.80 |
| Interstitial | $5–8 (video up to ~$14) | $2.50–5.00 |
| Rewarded video | $15–40 (US Android ≈ $16.49) | $8–18 |

ON RECORD. Two multipliers the file also records: mediation beats a single network by roughly
**40–60 %** of total ad revenue, and rewarded video adds roughly **40–68 %** to total ad revenue
without measurably hurting IAP conversion.

### 3.2 The DAU the two targets require

`$1,000/month ≈ $33.33/day` and `$300/month = $10.00/day` (30-day month, INFERRED convention). The
DAU column below is the file's own recorded reading for $1,000 — "Illustrative order-of-magnitude
implications for **$1,000/month** (≈ $33/day)" — and the $300 column is the same figure scaled by
10/33.33 = 0.30, which is my arithmetic on their reading (INFERRED), not a second measurement.

| Monetization posture | ARPDAU implied | DAU for **$1,000/mo** (ON RECORD) | DAU for **$300/mo** (INFERRED) |
|---|---:|---:|---:|
| Rewarded-centric | $0.0333 – $0.0476 | ~700 – 1,000 | ~210 – 300 |
| Interstitial + banner | $0.0056 – $0.0167 | ~2,000 – 6,000 | ~600 – 1,800 |
| Banner-only | ≈ $0.0030 | ~11,000+ | ~3,300+ |

The ARPDAU column is back-derived by division (`$33.33 ÷ DAU`) and is INFERRED; it is shown because it
is what makes the install columns below auditable rather than magic.

### 3.3 Installs, and why this column is the weakest in the document

The file states the relationship and leaves the factor open: *"Retention is the other multiplier,
since `DAU = installs × retention`."* **No retention rate is recorded anywhere**, so `installs =
DAU ÷ retention` cannot be evaluated with a measured number. Rather than omit the column, here it is
with the sensitivity shown — `r` = the share of installs active on a given day, **UNVERIFIED**:

For **$1,000/month**:

| Posture | DAU | Installs/mo @ r=5 % | @ r=10 % | @ r=20 % |
|---|---:|---:|---:|---:|
| Rewarded-centric | 700–1,000 | 14,000–20,000 | 7,000–10,000 | 3,500–5,000 |
| Interstitial + banner | 2,000–6,000 | 40,000–120,000 | 20,000–60,000 | 10,000–30,000 |
| Banner-only | ~11,000+ | 220,000+ | 110,000+ | 55,000+ |

For **$300/month**:

| Posture | DAU | Installs/mo @ r=5 % | @ r=10 % | @ r=20 % |
|---|---:|---:|---:|---:|
| Rewarded-centric | 210–300 | 4,200–6,000 | 2,100–3,000 | 1,050–1,500 |
| Interstitial + banner | 600–1,800 | 12,000–36,000 | 6,000–18,000 | 3,000–9,000 |
| Banner-only | ~3,300+ | 66,000+ | 33,000+ | 16,500+ |

### 3.4 The honest note that must accompany any ads-led proposal

**The factory has never shipped an ads-led slot, and it has no measured eCPM for this account.**

- Every plan, recipe and proposal up to 2026-10-01 was `purchase_led_single_unlock` (header, §4.3), so
  there is **no ads-led precedent, no observed fill rate, and no observed eCPM** in this repository.
- The eCPM table is a **published third-party benchmark range**, which the file itself labels "not our
  own measurements". A range with a 4× spread in the banner row and a 2.7× spread in the rewarded row
  cannot be used to price anything precisely.
- `docs/monetization-strategy.md` §0 lists, as **blocking human actions in order**, "**Create/locate
  the AdMob account** and produce per-slot ad unit ids", and records that until it exists "every ad
  path is exercised against `FakeAdsController` with Google's test unit ids" and that on-device
  verification "of ads *and billing*" is sequenced after it. **An ads-led plan's revenue path is
  therefore blocked behind a human action that has not happened**, and a proposal claiming ads-led
  must say that rather than present the DAU table as reachable.
- §2.1's own illustrative sentence is **internally inconsistent**, and a proposal quoting it should
  quote its conclusion rather than re-perform its arithmetic: "64 % opt-in, 2.1 rewarded views per DAU
  at a $24 blended eCPM yields ≈ $0.05 per DAU per day". MEASURED check: applying the opt-in factor,
  `2.1 × 0.64 × $24/1000 = $0.0323`, not $0.05; `$0.05` is what `2.1 × $24/1000` gives, i.e. the
  opt-in share is not applied. Either 2.1 is already an opt-in-weighted figure — in which case 64 %
  is redundant — or the sentence overstates ARPDAU by ~36 %. The DAU column for $1,000 would move from
  ~700 to ~1,030 under the smaller figure. **This is recorded, not corrected**: a worksheet that
  silently picked the friendlier number would be doing the thing this role exists to prevent.

---

## 4. The archetype derivation rule

### 4.1 The ids, read from the file

Asked of `scripts/lib/slot-economics.mjs` in this session (MEASURED, all four returned with their
pairing):

| # | Archetype id | Required capability (`requiredCapabilitiesFor`) | `priceUsd` rule (`parseEconomics`) |
|---:|---|---|---|
| 1 | `purchase_led_single_unlock` | `billing` | must be `> 0` |
| 2 | `purchase_led_repeat_sales` | `billing` | must be `> 0` |
| 3 | `ads_led_utility` | `ads` | may be `0` (`< 0` rejected) |
| 4 | `purchase_only_no_ads` | *(none)* | must be `> 0` |

`subscription` is **not** in `ARCHETYPES` — its absence is the enforcement, so a plan naming it fails
`parseEconomics`, `requiredCapabilitiesFor` and `check-economics.mjs` **with no exemption path**. It
is not an option to weigh, so it needs no row in the comparison below.

### 4.2 The selection conditions

For each: the audience shape, the use frequency and the willingness-to-pay condition under which it
is the **honest** choice; what disqualifies it; the capability; and the one thing a proposal's
evidence must show for it to be selected.

**`purchase_led_single_unlock`** — capability `billing`

- **Audience shape.** A user with a recurring *job* but no recurring *purchase*: they do the thing
  often, and there is exactly **one** capability they lack which does not expire, refill, accumulate
  or grow.
- **Use frequency.** **Any.** This archetype imposes no frequency requirement, which is precisely why
  it has dominated this factory's history by default rather than by argument — frequency-neutrality
  is not evidence, it is the absence of a constraint.
- **Willingness to pay.** The premium capability must be the reason the user installed the app: the
  free tier is a real, usable product, and the unlock removes a specific, nameable limitation the user
  has already hit. The strongest measured form of this is a **complaint already present in the
  shelf's critical reviews** naming the limitation.
- **Disqualifies when.** The value the user wants is **quantity** — credits, generations, content
  that keeps growing. A single unlock must then either give everything forever (pricing one usage
  level for all users) or artificially cap what the user wants, which is a worse product than selling
  it repeatedly. Also disqualifies when there is a second thing worth selling: that is repeat sales.
- **Evidence sentence required.** *"One premium capability, named in the proposal, whose absence is
  already a complaint in this shelf's critical reviews, and no meaningful second thing to sell."*

**`purchase_led_repeat_sales`** — capability `billing`

- **Audience shape.** A user whose want is **metered** — a count that runs down and refills: credits,
  generations, exports, content packs, or a unit whose fulfilment costs real money.
- **Use frequency.** Recurring **consumption**, not necessarily frequent sessions. The app can be
  opened weekly; what matters is that the metered need returns within the window.
- **Willingness to pay.** The user must accept paying **more than once for the same kind of thing**,
  which requires the first purchase to deliver visible value and the need to recur *within the kill
  window*. A metered need that refills annually is not repeat revenue at a 30–60 day cadence.
- **Disqualifies when.** The app's value is a one-off **state** — a record, a catalogue, a log. A
  record is static: the user's data does not refill, so there is nothing to re-sell, and a proposal
  claiming repeat sales over a static record is describing the wrong product. Also disqualifies on
  the `SERVER_COST` path unless the slot is declared a server project at stage 0 (§1.2 defect 2).
- **Evidence sentence required.** *"The shelf's incumbents demonstrably sell — or conspicuously fail
  to sell — a consumable, and the user's need measurably refills, with the run-down visible in the
  app's own domain model."*

**`ads_led_utility`** — capability `ads`

- **Audience shape.** Broad, high-frequency, low-stakes utility use, in a context where a short
  interruption is tolerated and the user has **nothing to buy**. Ads are not a fallback for "we could
  not design a purchase": §3.4 records this as a finished product shape.
- **Use frequency.** **Genuinely daily-ish.** This is the binding constraint and the one §5 quantifies
  — an app used a few times a month cannot reach even the $300 posture without an install base larger
  than the shelf's measured leaders.
- **Willingness to pay.** **None assumed.** That is the point of the archetype: it monetizes volume,
  not intent, so it needs no purchase behaviour from anyone. The corresponding obligation is that the
  app must earn its frequency honestly — an app nobody opens daily has no ads business.
- **Disqualifies when.** The app is used a few times a month (§5); the audience is ad-hostile; the app
  has something worth selling repeatedly (ads then lead the wrong stream, §3.4); the app is
  child-directed, which triggers the Families ads regime and needs a configuration
  `capabilities/ads` does not currently provide (§3.5, repo-verified); or there is nothing worth
  watching a rewarded view for, in which case §3.6 directs the unlock over rewarded inventory.
- **Evidence sentence required.** *"A measured or strongly evidenced daily-use habit — not installs —
  plus an explicit written answer to 'what does a rewarded view unlock', plus an AdMob account that
  exists."*

**`purchase_only_no_ads`** — capability *(none)*, `priceUsd > 0`

- **Audience shape.** **Ad-hostile** and price-tolerant: children; professional, legal, clinical or
  otherwise confidential contexts; privacy-sensitive users. §3.5 makes no-ads the low-risk default for
  child-directed apps, and §3.6 records this row as "a low-risk product choice".
- **Use frequency.** Any — there is no volume requirement, because the revenue is per install rather
  than per session.
- **Willingness to pay.** The audience must **already** pay for this kind of thing (the classic case
  is a professional buying a tool from an expense account). §4 of the strategy file records this as "a
  price lever with no discovery", "effectively dead outside ad-hostile audiences" — so the
  willingness must be pre-existing rather than hoped for.
- **Disqualifies when.** The audience is a general consumer audience with a free alternative, or the
  plan expects volume. Both make the absence of discovery fatal.
- **Evidence sentence required.** *"The shelf shows an actual paid app with real installs, or the
  audience is verifiably ad-hostile **and** the willingness to pay is evidenced, not assumed."*

### 4.3 The anti-inheritance rule

**A proposal that names an archetype without showing (a) the derivation and (b) the revenue reason each
alternative was rejected is a defect to be returned, not a preference to argue.**

This is the rule `docs/slot-economics.md` calls "the comparison is part of the plan", and
`.cursor/rules/ideation-gate.mdc` states as *"A proposal must show the alternatives it rejected, in
revenue terms"*. Three points of application:

1. **The comparison is per proposal and it must be specific.** "Subscription is out of scope; repeat
   sales rejected because the user's records are static and there is nothing to re-sell; ads rejected
   because the app is used twice a month and a banner is worth nothing" is a derivation. "We chose the
   unlock" is not. `subscription` needs no entry — it is banned outright.
2. **Two slots sharing a monetization model for no stated reason is a smell**, exactly as two slots
   sharing a palette would be, and the mix is expected to differ from slot to slot.
3. **It is deliberately not machine-checked**, and that absence is not permission. Enforcing it would
   mean a new required key, which `SCHEMA_VERSION` (exactly 1) would reject for every plan already
   written. So it is a **judgement the owner makes at checkpoint 1**, and it is this role's job to
   return a proposal that cannot produce the list rather than to argue about it afterwards.

---

## 5. The `ads_led_utility` sanity check

§2.1 says ads-led **can** reach the target. Here is what it actually requires, so a proposal cannot
claim ads-led for an app used twice a month.

**The requirement is a daily population, not a monthly one.** Every DAU figure in §3 is people
*active on a given day*. An app whose total audience is monthly can be a perfectly good product and a
structurally impossible ads business, because `DAU` is a fraction of it determined by usage
frequency — and a proposal that reports installs or monthly actives as if they were DAU is comparing
two different quantities.

**The arithmetic that makes it concrete** (INFERRED, from `DAU = MAU × (days used per month ÷ 30)`):

| Usage frequency | DAU ÷ MAU | MAU needed to hold **700 DAU** (rewarded-centric, $1,000/mo) | MAU needed to hold **11,000 DAU** (banner-only, $1,000/mo) |
|---|---:|---:|---:|
| Twice a month | 2/30 ≈ **0.067** | ~10,500 | ~165,000 |
| Weekly | 4.3/30 ≈ **0.14** | ~4,900 | ~77,000 |
| Daily-ish | ≈ **0.40** | ~1,750 | ~27,500 |

So the same ad revenue costs a twice-a-month app roughly **6× the monthly audience** of a daily app on
the rewarded path, and the banner path needs six figures of monthly actives no matter how it is
tuned. Those are monthly *active* figures, and the install base behind them is larger still, because
monthly actives are a fraction of installs.

**Therefore an `ads_led_utility` proposal must show all four of these, or it is not ads-led:**

1. **Frequency.** Evidence for a daily-or-near-daily usage habit — a job done daily, a routine with a
   natural daily trigger — and not merely "a utility". Infrequent use is disqualifying, not a tuning
   problem.
2. **Reach.** A stated path to the DAU figure for the target, in monthly actives and installs, held
   against the shelf's measured install buckets. Where the shelf's reachable middle is `10K+`
   lifetime installs, banner-only `11,000+` **daily** actives is not a plan.
3. **Session shape.** Something worth watching a rewarded view for, refreshed each time, with the
   placement designed **before** the UI (`.cursor/rules/ideation-gate.mdc`: "A slot that leads with
   ads must also answer 'what does a rewarded view unlock' *before* its UI is designed"). Interstitial
   inventory needs natural, repeated break points rather than an artificial one; banner-only is
   "worth roughly an order of magnitude less per user" and is the posture of last resort.
4. **The account, and the honest status.** An AdMob account must exist (§0 blocking action 2), and the
   proposal must state that its revenue figures rest on **third-party benchmark ranges, not this
   account's measurement** (§3.4). An ads-led plan presented without that sentence is an unmeasured
   claim wearing a measurement's clothes.

An app used twice a month, with a banner, and no unlock, cannot reach `$300/month` on any recorded
number without acquiring a monthly audience in the tens of thousands — which is a paid-acquisition
business, not a slot. That is the falsifiable form of the check.

---

## 6. The kill-criteria shape

### 6.1 What the three fields are

Per `docs/slot-economics.md` (schema, enforced by `parseEconomics`) and
`docs/monetization-owner.md`:

| Field | Rule | Meaning |
|---|---|---|
| `windowDays` | integer, **7 … 180** | The length of the observation window. At its end, a reading that misses either floor is `MISSED`, not `TOO-EARLY`. |
| `minInstalls` | integer, **≥ 1** | The install floor the newest reading must meet. |
| `minRevenueUsd` | integer, **≥ 1** | The revenue floor the newest reading must meet. |

A criterion of `0` is rejected by the validator, because **"a kill criterion of zero is not a
criterion, because it can never be missed."**

### 6.2 How `scripts/check-outcomes.mjs` judges them

MEASURED from `scripts/lib/outcomes.mjs` in this session. It prints **exactly one** verdict and the
exit code carries it:

| Verdict | Exit | When |
|---|---:|---|
| `NO-DATA` | 2 | No `store/outcomes.json`, or a file with no readings. Never a pass, never a kill. |
| `ON-TRACK` | 0 | The **newest** reading meets **both** floors. Tested *before* the window, so hitting the numbers on day 5 is not reported `TOO-EARLY`. |
| `TOO-EARLY` | 0 | Elapsed days since the epoch < `windowDays`. A progress read, not an approval. |
| `MISSED` | 1 | The window has elapsed **and** the newest reading misses either floor. Prints the `--kill` instruction. |

Four mechanics a proposal and its plan must be written around, each a real trap:

1. **Only the newest reading is compared.** The check does not sum or average: it reads
   `newestReading(readings)` against both floors. So the reading's `installCount` and `revenueUsd`
   must be recorded on a **consistent basis** — two cumulative-to-date figures, or the comparison is
   between a lifetime install count and a single period's revenue, which is not a comparison at all.
   The plan should say which basis it intends.
2. **The epoch is not always the slot's birthday.** With **two or more** readings the epoch is the
   **first reading**; with a **single** reading it is the slot's `createdAt` from `registry/apps.json`
   (falling back to that reading's own date, which reports `TOO-EARLY` rather than inventing an epoch).
3. **`NO-DATA` is checked before the clock**, so an unmeasured slot is never killed on arithmetic it
   never entered — and an empty `readings` array is a *valid* file, deliberately, so "not measured
   yet" stays distinguishable from "hand-edited wrongly".
4. **A reading dated in the future, a duplicate `observedAt`, or a missing `source` is refused
   outright** — one observation per date, so a corrected re-read *replaces* the row rather than being
   averaged into the verdict forever.

### 6.3 The honest choice for a first-sale measurement

**INFERRED throughout; these are reasoned recommendations, not recorded values.** No measurement of
this factory exists to calibrate them against, and each plan's numbers belong to that plan.

- **`minRevenueUsd` — the number that changes the decision.** The repository's only worked shape is
  `docs/slot-economics.md`'s example `{ "windowDays": 30, "minInstalls": 500, "minRevenueUsd": 50 }`.
  On the worksheet's arithmetic at **$2.99** ($2.4409 net), `$50` net ≈ **21 sales**, and a slot with
  500 installs at the assumed 2 % conversion would make 10 sales = **$24.4** — i.e. it would **pass
  installs and miss revenue**, which is the intended behaviour: revenue is the real gate and
  `minInstalls` is not a second one. For scale, `$300/month` is 123 sales at $2.99. **Recommendation:**
  state `minRevenueUsd` at a number the plan argues for as "enough that the mechanism demonstrably
  works and is worth another window", and use **$50** as the first-window anchor; a first window is a
  first reading, not a verdict on the idea.
- **`minInstalls` — derive it, do not choose it.** Because the check requires **both** floors, an
  install floor set **too high** can kill a slot that met its revenue target, and an install floor set
  **too low** can only fail a slot that also failed revenue. So the floor must be derived at the
  **optimistic** conversion, which gives the **lowest** install count at which the revenue floor is
  plausible:

  ```
  minInstalls = ceil( minRevenueUsd ÷ net-per-sale ÷ conversion_optimistic )

  worked, price $2.99, net $2.4409, minRevenueUsd $50, conversion_optimistic 5 %:
    50 ÷ 2.4409 = 20.5 → 21 sales
    21 ÷ 0.05   = 420 installs  →  minInstalls ≈ 400–500
  ```

  Derived at the optimistic end, the install floor is the **weaker** of the two thresholds, which is
  the safe direction: it can only fail a slot whose revenue was already implausible. Deriving it at
  1 % would give 2,050 installs and could kill a slot earning `$50` on 500 installs at 5 %. A plan
  should show this division.
- **`windowDays` — long enough to reach the starting line.** The window is measured from the epoch in
  §6.2, and with a **single reading** that epoch is the slot's `createdAt` — so **build time is inside
  the window**. Worse, `docs/monetization-strategy.md` §1.1 records that accounts created after
  2023-11-13 must run a closed test with **≥ 12 testers opted in continuously for 14 days** before
  production access, plus up to ~7 days of review, and whether that gate applies to this account is
  **UNVERIFIED** — §1.1 says so and `AGENTS.md` notes it is still to be read from Play Console. A
  `windowDays: 30` measured from `createdAt` can therefore expire **before the app is permitted to
  earn anything**. Two consequences:
  - **Operationally:** record the first reading on or immediately after the day the app goes live, and
    record a second soon after, so the epoch becomes the first reading rather than `createdAt`. This
    is the single highest-leverage thing an operator can do for the verdict's honesty.
  - **In the plan:** choose a window that survives the uncertainty — **45–60 days** is the defensible
    first-window range while the production gate is unconfirmed, and **60** is what both 2026-10-01
    recipes chose, which is coherent. `180` is the ceiling and is not a first-window value.

**What the criteria cannot do**, stated because it is a limit and not a defect to paper over: the
gate compares declared thresholds against a hand-recorded number, so it cannot prove the reading is
faithful to the console, cannot prove the thresholds were the right ones, cannot prove a given
revenue number is *good* rather than merely above a floor, and cannot kill a slot on missing data.
Those are judgements belonging to the owner, and `MISSED` is a verdict — **not** an instruction this
role executes on its own initiative.

---

## 7. The rejection ledger

When a proposal refuses a candidate it uses one of these reasons **and names the revenue arithmetic
behind it**. The list is closed for refusals; the reasons are the ones
`.cursor/rules/ideation-gate.mdc` § "The objective is revenue, not novelty" sets out.

| # | REVENUE reason | What the proposal must show |
|---:|---|---|
| 1 | **No statable wedge** | The one-sentence difference in **functionality or content** cannot be written. §1.2 of the strategy file: "If that sentence cannot be written, the idea is not ready." |
| 2 | **An unrated demand reading** | The on-job apps on the shelf are unrated or tiny — "an unrated competitor is a failure signal, not a vacancy" (`docs/niche-selection.md`), and a `candidate` verdict from the relevance guard is not evidence on its own, because the guard is blind to homonym collisions. |
| 3 | **A price the market will not pay** | The incumbents monetize at a price the market has refused, or the shelf's paying behaviour is concentrated in a different job from the one proposed. |
| 4 | **A build cost that eats the margin** | The estimated build in agent hours against the expected revenue per hour, measured against a cheaper alternative — including a server or `SERVER_COST` requirement that arrives after approval. |
| 5 | **A duplicate of our own catalogue** | Near-identical functionality, content **and** experience to one of our own slots. This is the enforceable half of Play's Repetitive Content policy, and it is the one that escalates to account termination. |

**Explicitly not on the list, and it must never appear as a refusal reason:**

> **"This category is already crowded"** — and every paraphrase of it.

An occupied shelf is **evidence of demand**, not a defect: "Money on display is the only cheap proof
that a market pays." A weak mid-tier — a 3.4★ at $19.99, a 3.8★ at $3.99 — is a *reachable pocket*,
which is where a slot goes. Doing the same job as an existing app, but better, is a legitimate
factory recipe. The owner's correction is recorded verbatim in `.cursor/rules/ideation-gate.mdc`:
*"our goal is to earn. If an app, by our calculations, will earn, then we build it. We look at
absolutely everything. There are no frames."*

Also absent, for the same reason — each is a novelty judgement wearing a revenue coat:

- **"Nothing like this exists yet"** as a reason *to* proceed. An empty shelf is evidence of nothing
  and is usually empty for a reason. This is not a qualification.
- **"We already built something in this general area"** as anything other than the test in row 5 — a
  competitor to an incumbent is fine; two of *our own* slots that one sentence can describe are one
  recipe.

**A rejection needs a revenue reason, not a novelty reason.** "The incumbents monetize at a price the
market will not pay", "the demand reading is unrated or absent", "we cannot state the wedge in one
sentence", "the build cost eats the margin", "it duplicates our catalogue" — those are rejections,
because each is a statement about the money. A proposal that refuses on any other ground is returned.

---

## 8. Assumptions and unknowns

Listed rather than omitted. Each is labelled with what it is.

1. **Install-to-purchase conversion is unmeasured.** No slot in this factory has ever reported a sale:
   `node scripts/kiln-status.mjs` reports the live slot as `NO-DATA`, and `docs/monetization-owner.md`
   records that the factory has no revenue API, so a reading is hand-recorded today.
   **UNVERIFIED.** Every installs column in §2.3 inherits it.
2. **The 1 %, 2 % and 5 % conversion rates are assumptions, not measurements.** The repository's only
   related figure is §4's "~1–3 %" for one-time IAP, itself marked **UNVERIFIED** ("no source read in
   this session"). **1 % and 2 % bracket the only recorded range; 5 % is supported by no recorded
   figure.**
3. **`F_billing = 0.15` is an ASSUMPTION, not this account's fee.** It holds, per the file, "only
   under the first-$1M annual tier and/or the new-install case"; above $1M, or for existing installs
   in the EEA/UK/US at standard rates, it "rises to 20–30 % plus the 5 % billing fee". **Whether the
   fee is computed on a tax-inclusive price is UNVERIFIED** (research §8.7). Every net figure in §2
   moves if the tier is not the one assumed.
4. **`F_refund = 0.02` and `F_fx = 0.02` are ASSUMPTIONS**, each a decision "per slot".
5. **`F_fraud` 0.20 / 0.01 are ASSUMPTIONS.** The 0.01 is "a residual, not zero". §1.2's exclusion of
   `F_fraud` for local unlocks is **INFERRED** from its definition and from §3.7's `LOCAL` /
   `SERVER_COST` line — it is a reasoned reading of the file, and it should be re-checked if the file's
   definition of "granted credit" ever changes.
6. **`M_target = 0.60` is an ASSUMPTION** and, on the local path, does not constrain a price at all.
7. **No ads-led slot has ever shipped from this factory, and no eCPM, fill rate or ARPDAU has ever
   been measured for this account.** Every ad figure in §3 is a third-party published benchmark range
   with a 2.7×–4× spread. **UNVERIFIED.**
8. **No AdMob account exists**, and §0 of the strategy file lists creating it as a blocking human
   action. Until it does, ads paths run against Google's test unit ids. **An ads-led revenue path is
   blocked on an unperformed human action.**
9. **Whether the §1.1 production-access gate applies to this account is UNVERIFIED** and it decides
   whether a kill window can even start (§6.3). §1.1: if it applies, "each app costs ≥ 14 days before
   it can earn anything".
10. **Retention — the `DAU = installs × retention` factor — is unmeasured.** §3.3's install columns
    are therefore a sensitivity, not a forecast, and the `r` values 5 % / 10 % / 20 % are **INFERRED**.
11. **The set of purchasable Play price points is UNVERIFIED** (research §8.8) — a Play Console step,
    not establishable from this repository. Every listed price must land on one.
12. **The 30-day month** used for per-day figures in §2 and §3 is an **INFERRED** convention, not a
    recorded one. A 30.4-day month moves every per-day figure by ~1 %.
13. **`docs/monetization-strategy.md` §2.1's illustrative sentence is internally inconsistent** —
    MEASURED: `2.1 × 0.64 × $24/1000 = $0.0323`, not the ≈ `$0.05` it states; `$0.05` is
    `2.1 × $24/1000` with the opt-in share not applied. Recorded in §3.4, **not corrected here**.
14. **No number in this document is a projection of a projection.** Every figure traces to a recorded
    assumption set or to a division performed in this session.
15. **The archetype mix of any batch generated under this run is a prediction, not a result.** This
    worksheet can make an inherited choice visible; it cannot make a proposal's own audience
    judgement correct, and it cannot prove willingness to pay for any shelf.

---

*Written by the money owner, 2026-10-01. The plan and the reading for a real slot are
`apps/<slug>/economics.json` and `apps/<slug>/store/outcomes.json`; this file is neither, authorises
neither, and is superseded the moment a proposal carries its own derivation.*

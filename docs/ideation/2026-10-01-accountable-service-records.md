# Recon 2026-10-01 — accountable professional service records

Run by: a recon **scout** (subagent), lane = *records a practitioner must keep because the work is
accountable — to a client, to a liability, or to a regulator*. Read the market only; no slot under
`apps/**` was opened, and `registry/apps.json` was read for `id`/`name`/`idea` only.

This document is the proposal. Its gate-ready half is beside it:

| Recipe | Economics plan | Promise contract |
|---|---|---|
| 1 — a colourist's own formula book | `plans/hair-color-formula-book.economics.json` | `plans/hair-color-formula-book.differentiators.json` |
| 2 — a pool route service record | `plans/pool-route-log.economics.json` | `plans/pool-route-log.differentiators.json` |

**Label vocabulary used throughout**, per `.cursor/rules/ideation-gate.mdc`: **MEASURED** (a command
was run in this session and named), **ON RECORD** (a repository file says it, path given),
**INFERRED** (reasoning from the two above), **UNVERIFIED** (carried without evidence).

---

## 1. Method, and the control that says the sensor has not drifted

One screen over the assigned query set plus the two named calibration controls
(**MEASURED**, `node scripts/scan-market.mjs --concurrency 4 --query "hair color formula app" --query "salon client record book" --query "tattoo artist client log" --query "pest control service log" --query "food safety temperature log" --query "pool service log" --query "habit tracker" --query "bjj training log"`):

| Query | verdict | guard | seen | rated | weak | median |
|---|---|---|---:|---:|---:|---:|
| `hair color formula app` | `candidate` | `ok` | 30 | 22/30 | 15 | 3.60 |
| `salon client record book` | `fortress` | `ok` | 8 | 4/8 | 1 | 4.75 |
| `tattoo artist client log` | `candidate` | `partial` | 30 | 17/30 | 9 | 4.10 |
| `pest control service log` | `dead-zone` | `ok` | 30 | 3/30 | 3 | 2.20 |
| `food safety temperature log` | `candidate` | `partial` | 29 | 17/29 | 14 | 3.40 |
| `pool service log` | `candidate` | `ok` | 13 | 5/13 | 5 | 3.40 |
| **control** `habit tracker` | **`fortress`** | `ok` | 21 | 20/21 | 1 | 4.50 |
| **control** `bjj training log` | **`dead-zone`** | `ok` | 30 | 8/30 | 3 | 4.40 |

Both controls returned the verdict this brief names for them (`habit tracker` = FORTRESS,
`bjj training log` = dead-zone), so **no sensor drift is visible in this run** and the verdicts below
are evidence about the shelf rather than about the sensor.

**`candidate` is a spot check, not a measurement** (**ON RECORD**,
`docs/research/2026-09-24-market-decision.md` §1.1, where the factory's own control query moved from
9 to 25 visible results between runs). Every conclusion below is anchored to a hand-classified
job-share and to enriched per-app install and review counts, and review text — never to a verdict
alone.

Hand-classification (**MEASURED**, `node scripts/scan-titles.mjs --on-job <regex> --off-job <regex>`
per query):

| Query | hand job-share | guard said | flagged |
|---|---:|---|---|
| `hair color formula app` | 24/30 (0.80) | on-topic 0.93 | no |
| `salon client record book` | 5/8 (0.63) | on-topic 0.75 | **yes — guard on-topic over a low job-share** |
| `tattoo artist client log` | 20/30 (0.67) | partial 0.90 | no |
| `pest control service log` | 26/30 (0.87) | on-topic 0.90 | no |
| `food safety temperature log` | 18/29 (0.62) | partial 0.62 | no |
| `pool service log` | 11/13 (0.85) | on-topic 0.85 | no |

The one guard failure is `salon client record book` (on-topic at 0.75 while only 0.63 of visible
cards name the job) — exactly the homonym blindness `docs/research/2026-09-24-market-decision.md` §2.1
records, and treated here as a guard failure rather than as a pocket.

---

## 2. The reading that decides this lane: the shelf's words hide **two jobs**

This is the finding the whole document turns on, and it is why a naive read of the six
`candidate`/`fortress` verdicts would pick the wrong recipes.

Each of the strong-looking queries carries **two different jobs under the same words**:

| Query | Job A — the consumer job the shelf's **weak** incumbents belong to | Job B — the professional record job (this lane) |
|---|---|---|
| `hair color formula app` | AI hair-colour **try-on photo editors** (1M+ installs, 2.5–3.5★) | the colourist's **formula book** |
| `food safety temperature log` | **Bluetooth thermometer companion** apps (1.9★, 2.8★) | the kitchen's **HACCP check log** |
| `pool service log` | homeowner **pool-water care** (1.8★/231, 3.4★/84, both 50K+) | the technician's **route service record** |
| `tattoo artist client log` | **marketplace + AI generator** apps (Tattoodo 10M+) | the artist's **studio/client admin log** |
| `pest control service log` | **pest identification** and enterprise platforms | the technician's **service record** |
| `salon client record book` | **booking marketplaces** (Booksy 4.9★/10M+) | the stylist's **client book** |

Job A is where the shelf's weak ratings live. Job B — **my lane** — is systematically thinner and
mostly unrated. That is the honest shape of this recon: *the strength of the shelf belongs to a job
this lane is not about.* What follows judges each recipe on Job B, and says so.

---

## 3. Recipe 1 — a colourist's own formula book

**Slug** `hair-color-formula-book` · **query of origin** `hair color formula app`

### 3.1 Category and job

**Job (one sentence):** a colourist must record the exact formula used on each client so the next
appointment reproduces the colour instead of guessing it.

**Paying user:** a working colourist — self-employed, chair-renting, or employed — who buys their own
reference tools rather than using the salon's booking software. (Not the salon owner buying a
platform, and not the consumer trying colours on a photo.)

**What is bought:** one non-consumable unlock of the full formula book — unlimited clients, complete
visit history and exportable client colour cards.

**Price:** **$9.99**, one-time, non-consumable.
**Moment of payment:** after the colourist has entered their **third** client and taps to add a
fourth, or taps Export on a client card. The first three clients are free and fully functional.

### 3.2 Archetype, and the three that were rejected with a revenue reason

**Chosen: `purchase_led_single_unlock`** → requires the **`billing`** capability enabled.

| Archetype | Verdict | Revenue reason |
|---|---|---|
| `purchase_led_single_unlock` | **used** | There is exactly **one** premium capability — keep unlimited client formulas and export them. The colourist pays once and is done; a formula is a record they own, not something they spend. |
| `purchase_led_repeat_sales` | rejected | **Nothing to sell repeatedly.** A formula record is not consumed, and there is no content pack, credit or metered service whose value is used up. Selling "more formula slots" would be the unlock wearing a repeat-sales label, and it would drag in a wallet the app does not need — the same defect as a consumable that can be bought once. |
| `ads_led_utility` | rejected | **No rewarded inventory exists** (the record *is* the product, so there is nothing worth watching a video for), and banner-only needs ≈11,000 DAU to reach $1,000/month (**ON RECORD**, `docs/monetization-strategy.md` §2.1) — far beyond this niche's plausible daily use of 1–3 minutes at the mixing bowl. A banner also lands over the formula at the moment the client is in the chair. |
| `purchase_only_no_ads` | rejected | The audience is not ad-hostile and the app is not child-directed, so the §3.5 case does not apply. §4 of the same document records the paid-up-front model as effectively dead outside those audiences — **no discovery**, which for a niche tool is the whole cost. |
| ~~`subscription`~~ | **banned** | Owner, 2026-10-01: subscriptions are forbidden and `subscription` is absent from `ARCHETYPES`. Not weighable. |

### 3.3 The wedge, in one sentence

> **A brand-agnostic formula book the colourist owns, which reproduces the exact shades and developer
> ratio used on a returning client without an account, a brand catalogue or a network.**

That is a difference in **functionality and content**, not in naming or colour:
Redken Style Station is the market's reference tool and it is L'Oréal's own brand catalogue
(**MEASURED**: `ads=false`, `iap=false`, free, 4.4★, 194 reviews, 50K+ installs); the independent
alternative at 10K+ installs carries only 40 reviews. Nothing on the shelf is a neutral, owned record
that spans whatever brands the colourist actually stocks.

### 3.4 Demand evidence — Job B, hand-filtered

**MEASURED**, `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "hair color formula app"`.
The storefront surfaces the **professional** apps first for this query, which is why the split below
is visible at all:

| # | Package | App | Rating | Reviews (exact) | Installs | Ads | IAP | Price | Job |
|---:|---|---|---:|---:|---|---:|---|---:|---|
| 1 | `vn.bm.formulabook` | Formula Book: Hair Color Notes | unrated | — | 0+ | false | **true** | free | **B** |
| 2 | `com.redken.haircolor` | Redken Style Station (L'Oréal) | 4.4★ | **194** | 50K+ | false | false | free | **B** |
| 3 | `com.stayincharm` | Charm: Save Formulas & Clients | 4.4★ | **40** | 10K+ | false | **true** | free | **B** |
| 4 | `com.itbruderhofer.tintlog` | TintLog: Hair Color Formulas | unrated | — | 10+ | false | **true** | free | **B** |
| 5 | `com.ttech.aihairtryon` | Hair Color & Hairstyle Try On | 4.5★ | 7,946 | 100K+ | false | true | free | A |
| 6 | `com.luckystars.haircolorchanger` | Hair Color Changer – Hair Dye | **2.5★** | 5,575 | **1M+** | **true** | false | free | A |
| 7 | `com.colorcoach` | Color Coach | unrated | — | 5K+ | false | **true** | free | **B** |
| 8 | `app.hairlabai` | Hair Color Changer: HairApp | 3.9★ | 5,133 | 100K+ | false | true | free | A |

Hand job-share for the query was 24/30 (0.80) — but the **sub-job** split is the reading that matters:

- **Job A (consumer try-on), the shelf's weak tier:** 2.5★/5,575/1M+, 2.8★, 2.8★, 2.9★, 2.9★, 3.0★,
  3.0★, 3.2★, 3.3★, 3.4★, 3.5★, 3.7★, 3.9★/5,133. **The `candidate` verdict (median 3.60, 15/22
  rated weak) is produced entirely by Job A.**
- **Job B (professional formula record), the lane:** **two** rated apps, both 4.4★ (194 and 40
  reviews), plus three unrated (5K+, 0+, 10+). **Job B has no weak rated incumbent at all.**

**Money on display (Job B):** `com.stayincharm` carries an **IAP badge at 10K+ installs** — the single
strongest monetisation data point in this recon, and the reason the archetype above is credible at
all. Redken Style Station reaches **50K+ installs with 194 reviews** as a *free corporate brand tool*,
which proves the job is searched for and installed at scale but is not itself revenue.

**What the incumbents' users say** (**MEASURED**,
`node scripts/scan-complaints.mjs --package com.stayincharm --package com.itbruderhofer.tintlog --package com.colorcoach --package vn.bm.formulabook --package com.redken.haircolor`):

- `com.redken.haircolor` (4.4★, 50K+) — **3 of 3 sampled reviews were critical**, and one is a content
  regression: *"I used to love the old app. There used to be an inspiration lookbook where you could
  access formulas and an education section."* Another asks to *"either remove the virtual try on or
  fix it"*. The leader is shedding the formula content this recipe is about.
- `com.stayincharm`, `com.itbruderhofer.tintlog`, `com.colorcoach`, `vn.bm.formulabook` returned **no
  critical reviews at all** in the sample (Play serves 3 per page and the two indies are young).

**The graveyard counter-evidence, stated rather than buried:** `vn.bm.formulabook` sits at a **0+
install bucket** and `com.itbruderhofer.tintlog` at **10+**. Two independent attempts at the exact job
with no traction. That is the single biggest reason this recipe is second on the kill list and not
first.

### 3.5 Revenue arithmetic, with the division shown

Deduction assumptions (**ON RECORD** as *assumptions to re-check*, `docs/monetization-strategy.md`
§2.2): Play service fee `F_billing = 0.15`, refunds/chargebacks `F_refund = 0.02`, FX and payout
rounding `F_fx = 0.02`. Fulfilment is `LOCAL` (no server cost), so no `C_unit` and no fraud term.

```
price                 $9.99
divisor      (1−0.15)(1−0.02)(1−0.02) = 0.85 × 0.98 × 0.98 = 0.81634
net per sale $9.99 × 0.81634                                = $8.16

target       $1,000 / month gross
sales/month  $1,000 ÷ $9.99                                 = 101 sales
net/month    101 × $8.16                                    = $824

installs/month at 2 % conversion   101 ÷ 0.02               = 5,050
                 at 1 %            101 ÷ 0.01               = 10,100
                 at 3 %            101 ÷ 0.03               =  3,367
```

The conversion band is **UNVERIFIED** — `docs/monetization-strategy.md` §4 carries the 1–3 % figure
without a source. Price sensitivity, same target: at **$14.99** the net is **$12.24** and the bar
falls to **67 sales/month ≈ 3,350 installs/month** at 2 %.

**Honest read:** this clears $1,000/month only at ≈3,400–10,000 installs/month, which is a real ask
for a niche tool with no marketing. The kill criteria below are set at the floor where the recipe is
clearly *not working*, not at the target.

### 3.6 Promise contract

Full file: `plans/hair-color-formula-book.differentiators.json` (4 promises, 2 `core`, all `deferred`).

| id | weight | promise | evidence label |
|---|---|---|---|
| `reproduces-last-formula` | **core** | A returning client's previous formula is on screen before the colourist mixes — shades, developer ratio, processing time and result — so the colour is reproduced, not re-guessed. | **MEASURED** (the 4.4★/194-review/50K+ Redken app's critical reviews are about removed formula content) |
| `any-brand-any-ratio` | **core** | The book stores **any** manufacturer's shade plus the exact developer ratio, so a colourist working across brands is not limited to one catalogue. | **MEASURED** (Redken is L'Oréal's, `ads=false iap=false free`; Charm is the only independent, at 10K+/40 reviews with `iap=true`) |
| `no-account-no-network` | supporting | Every stored formula opens with no account, no sign-in and no network. | **INFERRED** (forced account/login was a classified complaint on 3 of 6 shelves this session) |
| `client-colour-card` | supporting | The colourist can hand the client a card of the formula used. | **INFERRED** (no incumbent read here offers a client-facing formula export) |

### 3.7 Build, permissions, capabilities, server

- **Build estimate:** 8–11 agent hours (**INFERRED** from the reference slot's shape — one Compose
  module, a local database, a list/detail UI, one export path, no network).
- **Permissions:** **none dangerous.** Optional photo attach via the system **Photo Picker**
  (no runtime permission on API 33+; `minSdk 26`, so the pre-13 path uses the picker backport rather
  than `READ_EXTERNAL_STORAGE`).
- **Capabilities:** `billing` (one-time non-consumable). No `ads`.
- **Server:** **not required.** Fulfilment is `LOCAL` — the unlock changes what the user can store and
  export, not what we can serve — so `PurchaseAuthority` is not needed and the slot is not a server
  project (`docs/monetization-strategy.md` §3.7).

### 3.8 Visual direction

**Mark:** a folded **swatch fan** reduced to three overlapping strands. **Palette subject:** the
colourist's own bench — a deep aubergine ground with a **copper/rose-gold** accent drawn from developer
bottles and shade fans, with a warm paper tone for the record surfaces.
**What the slot's own components do that stock Material does not:** each saved formula renders as a
**ratio strip** — the shades as stacked swatch chips whose widths are the parts of the mix, with the
developer volume on the end — and each client's visits render as a **fade rail** showing the colour
drifting across appointments. A stock `Card`/`List` row cannot express either, which is the point.

### 3.9 Kill criteria

```json
{ "windowDays": 60, "minInstalls": 2000, "minRevenueUsd": 100 }
```

### 3.10 Honest unknowns

1. Whether a colourist pays $9.99 once rather than keeping paper cards — the only on-shelf evidence is
   Charm's IAP badge at 10K+/40 reviews, which shows a mechanism exists and not that it earns.
2. The 1–3 % one-time-IAP conversion band is **UNVERIFIED** (its only home, `docs/monetization-strategy.md`
   §4, says so).
3. Whether the two near-zero independent attempts (`0+` and `10+` installs) failed on distribution or
   on the job being unwanted. Nothing measured here separates those.
4. No **weak rated** incumbent exists at Job B (both rated apps are 4.4★), so this recipe cannot be
   argued as "better than a badly built app" — it can only be argued as a **content** gap.
5. **Internal-catalogue check:** the factory ships one live record-book slot (a classroom gradebook,
   `registry/apps.json` `id: teacher-gradebook`). This recipe differs in job, buyer, data model and
   report, but a reviewer could read two "private offline record book with a free tier and a one-time
   unlock" as near. See §5.

### 3.11 The observation that would flip this recipe to FAIL

If the professional sub-shelf's **only** evidence of a paid mechanism — `com.stayincharm`'s declared
in-app purchase at 10K+ installs — turns out to be an unrelated badge, or if the app is in fact free
with no purchasable item, then Job B has **no price the market has shown it pays** and the recipe
reduces to "a nicer free app competing with free apps". A single `enrich-market` read of Charm's
in-app-product list (or a store-page read of its price banner) settles it.

---

## 4. Recipe 2 — a pool route service record

**Slug** `pool-route-log` · **query of origin** `pool service log`

### 4.1 Category and job

**Job (one sentence):** a pool service technician must record what was measured and what was actually
put in each pool on every visit, and be able to show the homeowner that the work happened.

**Paying user:** a pool service technician, or the owner of a small route (a handful of properties),
who is the person on the pool deck with a phone in one wet hand.

**What is bought:** one non-consumable unlock of the route log — unlimited properties, the full
per-property service history, and the printable per-visit service record.

**Price:** **$14.99**, one-time, non-consumable.
**Moment of payment:** after the technician has set up their **second** property and taps to add a
third, or taps Share on a service record. One property is free and fully functional.

### 4.2 Archetype, and the three that were rejected with a revenue reason

**Chosen: `purchase_led_single_unlock`** → requires the **`billing`** capability enabled.

| Archetype | Verdict | Revenue reason |
|---|---|---|
| `purchase_led_single_unlock` | **used** | The buyer already pays $40–100 a month for route software (**ON RECORD**, `docs/research/2026-09-24-market-decision.md` §2.2 records the equivalent one-time-versus-rental migration in an adjacent trade niche, where a 4.8★ leader's critical reviews were *entirely* about leaving a "$25 one-time fee for life"). A one-time unlock is the price the buyer's own comparison set makes obvious. There is one premium capability: the unlimited route and its history. |
| `purchase_led_repeat_sales` | rejected | **Nothing is consumed.** A service history is written once and read later; there is no credit, generation or pack whose value is spent, so a consumable would be a single unlock wearing a repeat-sales label — and would need a `PurchaseAuthority` and a wallet the slot does not otherwise require. |
| `ads_led_utility` | rejected | **No rewarded inventory** (nothing to unlock by watching), and the context is a professional on a customer's property, where a banner is an extra failure while the technician is entering a reading. Banner-only still needs ≈11,000 DAU (**ON RECORD**, §2.1). The one ad-supported incumbent in the exact job, Pool Pilot, is also the one whose critical review is a **paywall** complaint — ads are not what its users want either. |
| `purchase_only_no_ads` | rejected | No ad-hostile or child-directed case applies (§3.5), so this is a price lever with **no discovery** (§4). |
| ~~`subscription`~~ | **banned** | Owner, 2026-10-01. Not weighable. |

### 4.3 The wedge, in one sentence

> **A pool route record that keeps each property's readings, the treatment actually applied and the
> running history on the technician's phone, and hands the homeowner a service record at the end of the
> stop — with no account, no web view and no signal required.**

Difference in **functionality and content**: the incumbent at 10K+ installs opens a web browser to
work and loses the user's templates (**MEASURED**, verbatim below); the incumbent at 1.8★ reads the
strip and stops.

### 4.4 Demand evidence — Job B, hand-filtered

**MEASURED**, `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "pool service log"`.
The shelf is **small** (13 visible cards — a display reality, not a market size, **ON RECORD**
`docs/niche-selection.md` §3 traps):

| # | Package | App | Rating | Reviews (exact) | Installs | Ads | IAP | Job |
|---:|---|---|---:|---:|---|---:|---|---|
| 1 | `com.poolserviceusa.poolserviceusa` | Pool Service Software | 4.1★ | **45** | 5K+ | false | false | **B** |
| 2 | `com.poolistry.pro` | Poolistry Pro: Pool Service | unrated | — | 10+ | false | true | **B** |
| 3 | `com.pool_soft.service` | Pool Soft Service | unrated | — | 100+ | false | false | **B** |
| 4 | `problemsolversoftware.poolpilot` | Pool Pilot – Pool & Spa Care | 4.0★ | **23** | 5K+ | **true** | **true** | **B** |
| 5 | `it.ironblaster.piscina` | Pool maintenance | **3.4★** | **84** | **50K+** | **true** | false | A |
| 6 | `com.ecolab.apps.recwaterapp` | Pool Manager (Ecolab) | unrated | — | 1K+ | false | false | **B** |
| 7 | `com.biolabinc.pooltime` | Pool Time ClearPool Expert™ | **1.8★** | **231** | **50K+** | false | false | A |
| 8 | `com.poolstamp` | PoolStamp Pool Maintenance Pro | unrated | — | **1+** | false | false | **B** |

Hand job-share 11/13 (0.85), guard `ok` — no guard failure. The sub-job split (see §2):

- **Job A (homeowner water care):** `com.biolabinc.pooltime` **1.8★ / 231 reviews / 50K+** and
  `it.ironblaster.piscina` **3.4★ / 84 reviews / 50K+**, plus `com.poolcorp.bluestreak.customer.store`
  2.3★. **This is where the shelf's money and its low ratings both are.**
- **Job B (the technician's record):** **two** rated apps — `com.poolserviceusa.poolserviceusa`
  4.1★/45/5K+ and `problemsolversoftware.poolpilot` 4.0★/23/5K+ — plus six unrated
  (1K+, 100+, 10+, 1+, and two more).

**Money on display (Job B):** `problemsolversoftware.poolpilot` declares **both ads and an in-app
purchase at 5K+ installs** — a paid mechanism in the exact job. `com.poolserviceusa.poolserviceusa`
reaches **5K+ installs with 45 reviews** at 4.1★.

**What the incumbents' users say** (**MEASURED**, `node scripts/scan-complaints.mjs --query "pool service log" --top 6 --concurrency 2` and a direct `--package` run on the four named apps; 15 reviews read, 9 critical, 3 unclassified = **33 % unclassified**, printed because a hidden blind spot is the failure mode):

- `com.biolabinc.pooltime` (1.8★, 50K+): *"using test strips, all it does is let me enter what I see on
  the strip. there are no recommendations based on my values."* · *"It's great that the strip testing
  results tells you what's wrong, ideal range etc. But that's not very helpful if it doesn't also tell
  you how to fix …"* — the incumbent **reads the test and stops**.
- `com.poolcorp.bluestreak.customer.store` (2.3★, 10K+): *"I have to log in multiple times each time i
  use it, doesn't save my info when i do. opens a separate web browser to use the app. My templates
  are gone."* — account, web view, lost records.
- `it.ironblaster.piscina` (3.4★, 50K+): *"Won't allow change from metric and doesn't calculate round"*
  · *"Looks like a good experience, unfortunately only in Italian."* — unit system and locale.
- `problemsolversoftware.poolpilot` (4.0★, 5K+): *"does not allow for custom mechanical set-up, also
  does not contain the options for specific pool traits."* — the pro app is **inflexible**.
- Histogram over the 9 critical reviews: `broken` 2, `units` 2, then `account`, `accuracy`,
  `confusing-ui`, `data-loss`, `paywall`, `performance` 1 each.

**The graveyard counter-evidence, stated rather than buried:** eight of the ten Job-B cards are
**unrated**, and the two rated ones carry only 45 and 23 reviews. `com.poolstamp` sits at **1+ installs**.
This is a thin professional sub-shelf with a lot of recent, tractionless attempts.

### 4.5 Revenue arithmetic, with the division shown

Same deduction assumptions as §3.5 (**ON RECORD**, `docs/monetization-strategy.md` §2.2). Fulfilment is
`LOCAL` — no server cost, no fraud term.

```
price                 $14.99
divisor      (1−0.15)(1−0.02)(1−0.02) = 0.85 × 0.98 × 0.98 = 0.81634
net per sale $14.99 × 0.81634                               = $12.24

target       $1,000 / month gross
sales/month  $1,000 ÷ $14.99                                = 67 sales
net/month    67 × $12.24                                    = $820

installs/month at 2 % conversion   67 ÷ 0.02                = 3,350
                 at 1 %            67 ÷ 0.01                = 6,700
                 at 3 %            67 ÷ 0.03                = 2,233
```

Price sensitivity, same target: at **$9.99** the net is **$8.16** and the bar rises to
**101 sales/month ≈ 5,050 installs/month** at 2 %. The conversion band is **UNVERIFIED** (§4 of the
monetization document says so).

### 4.6 Promise contract

Full file: `plans/pool-route-log.differentiators.json` (4 promises, 2 `core`, all `deferred`).

| id | weight | promise | evidence label |
|---|---|---|---|
| `per-property-history` | **core** | Each property's readings, the treatment actually applied and the notes accumulate into one history the technician reads on arrival, so the next visit starts from what the last one did. | **MEASURED** (POOl360 2.3★/10K+: *"My templates are gone"*; Pool Pilot 4.0★: *"does not allow for custom mechanical set-up"*) |
| `visit-record-for-owner` | **core** | The technician can hand the homeowner a per-visit service record showing the readings taken and the work done. | **MEASURED** (the 1.8★/231-review/50K+ incumbent's users: *"not very helpful if it doesn't also tell you how to fix"*) |
| `reading-with-its-range` | supporting | The measured value sits beside the ideal range the app used, so a reading is judged, not just stored. | **MEASURED** (ClearPool Expert user: *"using test strips, all it does is let me enter what I see on the strip"*) |
| `no-account-no-web-view` | supporting | The route opens with no account, no sign-in and no embedded browser, and works with no signal at the pump. | **MEASURED** (POOl360 2.3★/10K+: *"opens a separate web browser to use the app"*, *"log in multiple times"*) |

> **Not a calculator.** The only arithmetic in the slot is the treatment guidance attached to a
> recorded reading, sourced from a published test-kit range table. The product's job is the **record**
> and its history; the arithmetic is content inside it, not the engine. Calculators and unit converters
> remain a closed topic.

### 4.7 Build, permissions, capabilities, server

- **Build estimate:** 9–12 agent hours (**INFERRED** — one more entity and one more report than
  recipe 1).
- **Permissions:** **none dangerous.** Photo attach via the system **Photo Picker** (no runtime
  permission on API 33+). No location: a route is a list the technician orders, not a map.
- **Capabilities:** `billing` (one-time non-consumable). No `ads`.
- **Server:** **not required.** `LOCAL` fulfilment; no `PurchaseAuthority`.

### 4.8 Visual direction

**Mark:** a **water ripple** cut into a rounded square, with one reagent-pad notch.
**Palette subject:** the test strip and the water it reads — a deep pool teal ground with a
**chlorine-yellow → magenta** reagent accent for the reading states, and a cool slate for records.
**What the slot's own components do that stock Material does not:** a **reading ring** that places the
measured value inside its ideal band and colours the arc by how far outside it sits, and a **route
ledger** row that carries each property's last visit summary and next-due marker. Neither is a stock
`Card` + `Text`, and the reading ring is the component the whole app is built around.

(Recipe 1's direction is aubergine/copper with a ratio strip and a fade rail; the two share no mark,
palette or component.)

### 4.9 Kill criteria

```json
{ "windowDays": 60, "minInstalls": 1200, "minRevenueUsd": 120 }
```

### 4.10 Honest unknowns

1. Whether a solo pool technician pays **once** for a record app rather than staying inside the route
   software the business already buys. The buyer's alternative is a subscription, and this recipe is
   deliberately the opposite shape — that is an argument, not a measurement.
2. Whether the **job** is big enough at Job B: the two rated professional incumbents carry 45 and 23
   reviews. The shelf's 50K+ installs belong to Job A (homeowners).
3. The 1–3 % one-time-IAP conversion band is **UNVERIFIED**.
4. **Correctness of the treatment guidance.** Pool chemistry ranges are safety-adjacent (chlorine,
   pH, cyanuric acid). If a range table ships, it needs a named, cited source and a checker — the same
   rule the factory already applies to code tables. Untested here.
5. Localisation: the recon's own complaint evidence includes a metric-only app rated 3.4★ at 50K+
   installs, so **unit system and locale are a wedge only if the slot actually ships both** — which
   costs build time.
6. **Internal-catalogue check:** see §5.

### 4.11 The observation that would flip this recipe to FAIL

If `problemsolversoftware.poolpilot`'s in-app purchase is **only** an ad-removal upsell rather than a
paid capability a technician would choose, then Job B has no demonstrated willingness to pay and the
recipe rests on unrated apps alone — the graveyard shape. One store-page read of Pool Pilot's product
list settles it.

---

## 5. Internal-catalogue check (Google Play Repetitive Content, ON RECORD `docs/monetization-strategy.md` §1.2)

The policy targets near-duplicates in **our own** catalogue, and its enforcement reaches account
termination. The live catalogue's identity fields (`registry/apps.json`, `id`/`name`/`idea` only)
carry one record-style app: `teacher-gradebook` — a private offline classroom record book with a free
tier and a one-time unlock.

| | teacher-gradebook | hair-color-formula-book | pool-route-log |
|---|---|---|---|
| Buyer | teacher | colourist | pool technician |
| Content | classes, learners, grades, attendance | colour formulas, developer ratios, processing results | properties, water readings, treatments |
| Workflow | term-long class register | appointment-to-appointment lookup | route visits in the field |
| Report audience | teacher / parent | client | homeowner |

**My reading:** the three differ in buyer, content and workflow, which is the test the policy states,
so neither recipe is a reproduction of the other two. **Stated as a risk, not as a clearance:** all
three are "a local database, a list and an export" at the architectural level, and a reviewer who
reads only that sentence would see one recipe. If the owner judges the pair of recipes in this
document too close *to each other*, build **one** — `pool-route-log` has the better money-on-display
evidence and `hair-color-formula-book` has the better proven-demand reading; §3.11 and §4.11 name the
one read that separates them.

**`subscription` is not proposed anywhere** — banned (owner, 2026-10-01) and absent from `ARCHETYPES`.

### 5.1 A collision with a sibling recon this scout cannot see, flagged for the orchestrator

This scout ran in parallel with other scouts and has no channel to them (**ON RECORD**,
`.cursor/rules/parallel-execution.mdc`: subagents cannot talk to each other and the orchestrator is
the router). While checking the directory it wrote into, it observed a sibling proposal,
`docs/ideation/2026-10-01-solo-trade-rounds.md`, whose recipe is `lawn-round-log` — and read exactly
one line of it, its own refusal row, which says of a refused query: *"One sentence — 'a solo trade's
round on the phone' — describes it and `lawn-round-log`; two of our slots that one sentence describes
are one recipe."*

**That same sentence describes `pool-route-log`.** A pool service route is a solo trade's round on the
phone. The two recipes differ in trade and in what is recorded (mowing visits versus water chemistry
and the treatment applied), so they are not identical — but a reviewer applying the sibling's own test
would collapse them, and the sibling scout already used that test to refuse a query in its own set.

**This scout cannot resolve the collision and does not pretend to.** The orchestrator adjudicates
between returned proposals (`harness/roles/scout.md`, "Dispatch"). Stated plainly so the decision is
made with the fact in hand:

- `hair-color-formula-book` is **unaffected** by this collision — no sibling recipe observed here
  describes a colourist's formula book.
- If only one route-shaped recipe may proceed, `pool-route-log` should be the one at risk, not
  `lawn-round-log`, unless the orchestrator has evidence this scout does not: the two were selected
  from different query sets and neither scout could see the other's job sentence when it chose.

This is a finding about **our own** catalogue, not about the market, and it is deliberately not
dressed as a market reading.

---

## 6. Refusal table — every query screened and refused, with its **revenue** reason

| Query | Shelf verdict | Revenue reason for refusal |
|---|---|---|
| `salon client record book` | `fortress` (guard on-topic 0.75 over a **0.63** hand job-share — flagged) | The money on this shelf is a booking marketplace's take-rate (Booksy 4.9★/**10M+**, StyleSeat 4.8★/**1M+**), which is a **two-sided network with a server** — a business this factory does not build. The record job itself is served **free** (Client Note Tracker 4.7★/5K+, ClientNote) and the one weak app is a retail CRM at 1K+ installs whose complaints are account/crash. **No price the market has shown it pays for the plain record job.** |
| `tattoo artist client log` | `candidate` (0.67 hand job-share) | The rated weak incumbents are **not the job**: Tattoo Tix 1.5★ is event ticketing, TattooPrinter 3.0★ is a printer companion, the 2.2★–3.7★ tail is AI generators. Every on-job artist-admin/record app is **unrated** — InkProof (`5+` installs), InkHub, Inklee, Tattoospot, My Tattoo Hub (1K+), STUDIO Artist — and the only rated on-job admin app is Tattoo Studio Pro at **3.7★ / 41 reviews / 10K+**. **Unrated demand reading at the exact job.** |
| `pest control service log` | `dead-zone` (3/30 rated) | **Unrated demand reading — the graveyard shape.** The independent record apps (Pest Control & Termite Log `10+`, PestLogs `10+`, PestPro: Pest Records, Pest Control Business Manager) are all unrated, and the rated weak tier is **enterprise field-service platforms** — PestPac 2.2★/336/50K+, PestScan 2.2★/197/10K+, Pest Expert 360° 1.8★/124/10K+ — whose buyer is an employer, not a technician who chooses an app, and whose complaints are about enterprise workflows. |
| `food safety temperature log` | `candidate` (0.62 hand job-share) | The weak rated incumbents are **Bluetooth-thermometer companion apps** (PROBE TEMP 1.9★/118/10K+, AD Temp 2.8★/19/5K+); their complaints are probe disconnects and freezes, which an offline record app **cannot fix** — and their users bought a thermometer, not a log. Meanwhile **every HACCP-log app is unrated** at ≤1K installs (Food Safety App – HACCP 1K+, Pro Food Safe System 1K+, Food Safety Guru 500+, FoodSafety Log `10+`, My Food Safety). **Unrated demand reading at the log job; the money on the shelf belongs to hardware.** |

Both surviving queries carry the same caveat and it is recorded rather than smoothed: in each case the
`candidate` verdict is produced by a **consumer** sub-job, and the professional record job inside the
same shelf is thin. Their recipes are proposed on the **money-on-display evidence at the professional
sub-job** (an IAP badge at 10K+ for hair colour; ads+IAP at 5K+ and two rated professional tools for
pool), not on the verdict.

---

## 7. Recommendation

Both recipes are proposable and neither is a calculator, a converter, or a duplicate of a killed idea.
If the owner wants one slot now:

1. **`pool-route-log`** — the stronger money-on-display evidence in the exact job (a declared
   ads+IAP incumbent at 5K+, two rated professional tools, two 50K+ weak apps in the same shelf), and
   the higher price is defensible against the buyer's own monthly-software comparison.
2. **`hair-color-formula-book`** — the stronger *proven demand* reading (50K+ installs and 194 reviews
   on the job's reference app, 10K+ with an IAP badge on its only independent rival) and the cheaper,
   simpler build.

Neither is a certainty, and the honest headline of this recon is **§2**: in this lane the shelf's
strength sits in a consumer job wearing the same words, and the practitioner's record job is thin.
That is the risk the owner is being asked to price, and it is why both plans carry explicit
`assumptionsUnverified` entries rather than a confident revenue claim.

**Elapsed:** one recon session — 1 market screen (8 queries + 2 controls), 6 hand-classification runs,
2 enrichment runs (16 + 24 app pages), 5 complaint runs, 4 plan-gate validations.

**Scaffold command, when and if the owner picks one** (do not run before the decision):

```powershell
node scripts/new-slot.mjs --slug hair-color-formula-book `
  --economics docs/ideation/plans/hair-color-formula-book.economics.json `
  --differentiators docs/ideation/plans/hair-color-formula-book.differentiators.json

node scripts/new-slot.mjs --slug pool-route-log `
  --economics docs/ideation/plans/pool-route-log.economics.json `
  --differentiators docs/ideation/plans/pool-route-log.differentiators.json
```

Before either is scaffolded, the evidence in this document must be promoted into `docs/research/` (the
`evidenceRef` pattern accepts `docs/ideation/`, so this is a quality step, not a gate failure —
`docs/ideation/README.md` step 5).

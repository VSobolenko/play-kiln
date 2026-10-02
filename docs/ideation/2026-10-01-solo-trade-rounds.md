# Recon — solo-trade rounds and client logs (2026-10-01)

> **Lane.** The working record of a one-person mobile trade: which customers are on the round, what
> was done, what is owed, what is next. Offline by nature, high-frequency use, and the person doing
> the work would not build the app. This is a **different family** from appointment booking (the killed
> `dog groomer appointment book` row) and from invoicing / field-service suites (the killed
> `field service work order app` row).
>
> **Query set, fixed in advance (six queries, no additions).** `window cleaner round tracker`,
> `cleaning business client log`, `lawn care client tracker`, `pet sitting client log`,
> `snow removal route log`, `garden maintenance client schedule`.
>
> **Controls, in the same run.** `habit tracker` must return `fortress` and `bjj training log` must
> return `dead-zone`. Both did (see §1), so the sensor has not drifted and the verdicts below are
> evidence.

**One recipe is proposed, not two.** All six lane queries share one job sentence — *"a solo trade's
round and client log on the phone"* — and two of our own slots that one sentence describes are one
recipe (`.cursor/rules/ideation-gate.mdc`; Google Play's Repetitive Content policy reaches the whole
account, `docs/monetization-strategy.md` §1.2). Proposing a lawn slot and a cleaning slot would be
exactly the catalogue-wide risk the brief names as a valid refusal reason. The sibling queries are
therefore refused in §5, each with its revenue reason, rather than turned into near-duplicates.

**Everything here is labelled MEASURED / ON RECORD / INFERRED / UNVERIFIED.** Paths are given for
ON RECORD claims.

---

## 1. Method and controls

```text
node scripts/scan-market.mjs --concurrency 4 \
  --query "window cleaner round tracker" --query "cleaning business client log" \
  --query "lawn care client tracker" --query "pet sitting client log" \
  --query "snow removal route log" --query "garden maintenance client schedule" \
  --query "habit tracker" --query "bjj training log"
```

`scanned 24 queries in 8572 ms (concurrency 4): 7 candidate, 9 mixed, 4 fortress, 4 dead-zone, 0 no-results, 0 errored`

| Control query | Expected | Observed | Reading |
|---|---|---|---|
| `habit tracker` | `fortress` | **`fortress`** — 20/21 rated, median 4.50 | MEASURED — no drift |
| `bjj training log` | `dead-zone` | **`dead-zone`** — 8/30 rated (0.27 < 0.35) | MEASURED — no drift |

Controls agreeing with their hand-read verdicts is a necessary condition, not a sufficient one: a
`candidate` verdict is still **not** evidence, because the relevance guard is blind to homonym
collisions (`docs/research/2026-09-24-market-decision.md` §2.1). Every candidate below was
hand-classified with `scripts/scan-titles.mjs` and enriched before it was believed. That discipline
caught a real pollution event in this run (§2.3).

---

## 2. Demand evidence

### 2.1 Per-query screen (MEASURED)

| Query | Guard verdict | Guard relevance | Seen | Rated | Weak | Median | Notes |
|---|---|---:|---:|---:|---:|---:|---|
| `window cleaner round tracker` | `mixed` | partial | 30 | 24/30 | 6 | 4.35 | rated shelf, shallow weak tier |
| `cleaning business client log` | `candidate` | partial | 25 | 13/25 | 10 | 3.60 | pro side thin and mostly unrated |
| `lawn care client tracker` | `candidate` | ok | 30 | 22/30 | 13 | 3.95 | strongest reading in the lane |
| `pet sitting client log` | `candidate` | partial | 30 | 18/30 | 10 | 4.10 | weak tier is thin |
| `snow removal route log` | `candidate` | ok | 30 | 18/30 | 11 | 3.90 | shelf is mostly **plow simulator games** — see §2.3 |
| `garden maintenance client schedule` | `candidate` | **POLLUTED** | 30 | 22/30 | 14 | 3.75 | flagged by the screen itself; not readable |

### 2.2 Hand job-share, printed by `scan-titles.mjs` (MEASURED)

```text
### cleaning business client log
verdict=candidate  guard=partial (11/25, share 0.44)  visible=25
  hand job-share: 25/25 (1.00) vs guard partial        <-- classifier too broad; see note
### lawn care client tracker
verdict=candidate  guard=on-topic (24/30, share 0.80)  visible=30
  hand job-share: 29/30 (0.97) vs guard on-topic
### snow removal route log
verdict=candidate  guard=on-topic (20/30, share 0.67)  visible=30
  hand job-share: 24/30 (0.80) vs guard on-topic
```

**Note on the 25/25 for cleaning.** My `--on-job` regex matched the token `clean`, which appears in
*consumer* apps (`Clean My House: Chore Planner`, `Housy: House Cleaning Schedule`) and in
*marketplaces* (`Thumbtack`, `Homeaglow for Cleaners`, `Cleaners for Hire`) as readily as in a
cleaner's own tool. The 1.00 hand share is therefore an artefact of the classifier I supplied, and I
am recording it rather than quoting it as a clean reading. The genuine pro-side rows on that shelf are
tabulated in §2.4 — and they are mostly unrated, which is the opposite of a demand reading.

**Note on the 29/30 for lawn care.** The same caution applies in a weaker form: the shelf mixes
*consumer* apps (GreenPal Lawn Care — hire a pro; Yard Mastery — lawn advice; My Lawn by Scotts) with
*operator* apps (Yardbook, Lawn Love for Providers, Lawn Pro, Mowbius, Check). The 0.97 figure counts
all six family members. The distinction that matters is drawn by hand in §2.4, not by the regex.

### 2.3 Pollution caught by hand-classification (MEASURED)

`snow removal route log` passed both the guard (`on-topic`, share 0.67) and my own regex (0.80), and
it is **still not a candidate**. Of its 24 "on the job" cards, these are games, not tools:

`com.simulatorlegends.SnowClearPlowSimulator` · `com.gdm.snowplow` (3.0★) · `com.hrygames.snowplow`
(2.7★) · `com.game.littlewood5m4q8v209second` · `io.blackcandy.cleansnow3d` (4.5★) ·
`com.pg.snow.plow.heavy.excavator.sim` (2.9★) · `com.kk.snowplowexcavatorportrait` ·
`com.lab.buildingmaster4p8m` — **eight of twenty-four**.

The genuine snow-removal business apps in the shelf are unrated:
`com.cecasnowpro` (Snow Removal Pro, unrated) · `com.aethermint.snow_removal_manager` (Snow Removal
Business Manage, unrated). The rated cards are the games. This is the `bjj training log` shape at the
sub-query level — *people tried and nobody got traction* — and it is refused on an unrated demand
reading, not on competition (`docs/niche-selection.md` §2).

`garden maintenance client schedule` was flagged **POLLUTED** by the screen itself; combined with its
duplication of the lawn job (§5), it was not carried further.

### 2.4 Enriched incumbents (MEASURED — `node scripts/enrich-market.mjs --top 8 --concurrency 3`)

**`lawn care client tracker`** — search returned 30 cards; top 8 enriched; 0 failures.

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|---:|---|---|---:|---:|---|---|---|---|
| 1 | `com.ionicframework.greenpal793748` | GreenPal Lawn Care *(consumer)* | 4.6 | 1,740 | 50K+ | no | no | free |
| 2 | `com.lawnbook.app` | LawnBook — Smart Lawn Care *(operator)* | 4.4 | 9 | 500+ | no | yes | free |
| 3 | `com.lawntracker.app` | Lawn Tracker | unrated | unknown | 100+ | no | yes | free |
| 4 | `com.adminmatic.lawncalc` | Lawn Calc | 4.0 | 9 | 1K+ | no | no | free |
| 5 | `com.viksva.lawnai` | Lawn Care AI: Grass Identifier *(consumer advice)* | 4.7 | 47 | 1K+ | no | yes | free |
| 6 | `com.yardmastery.lawnjournal` | Yard Mastery *(consumer advice)* | 4.5 | 2,300 | 100K+ | no | no | free |
| 7 | `com.lawnguru` | LawnGuru: Lawn, Mow & Snow *(marketplace)* | 3.5 | 1,420 | 50K+ | no | no | free |
| 8 | `com.lawnlove.pros` | Lawn Love for Providers *(operator)* | 4.6 | 1,230 | 50K+ | no | no | free |

```text
summary: ads — declared: 0, declared absent: 8;  IAP — declared: 3, declared absent: 5
         largest install bucket seen: 100K+ (>= 100,000); installs unknown: 0
```

The operator-side rows the top-8 cut did not reach, read from the card face and then opened
individually (MEASURED — `scan-titles.mjs`, `scan-complaints.mjs`):

| package | title | rating | installs | role |
|---|---|---:|---|---|
| `com.app.yardbook` | Yardbook | 4.6 | 100K+ | the pro CRM leader |
| `com.lawnstarter.pros` | LawnStarter for Providers | 4.5 | — | marketplace, provider app |
| `com.greenpalpro.app` | GreenPal Pro For Vendors | 4.0 | — | marketplace, provider app |
| `com.lawnguruprovider` | LawnGuru Pro | 3.8 | — | marketplace, provider app |
| `com.adamlyttle.mowbius` | Mowbius — Lawn Care Management | 3.8 | 1K+ | solo operator tool |
| `com.lawnprosoftware.lawnproapp` | Lawn Pro | 3.5 | 10K+ | operator tool, cloud |
| `com.caribouapps.operator` | Check: Landscaping Management | 3.5 | 500+ | operator tool |
| `com.lawnstarter.lawnstarter` | LawnStarter *(consumer)* | 3.9 | — | marketplace |
| `com.plowz.app` | Plowz & Mowz *(consumer)* | 3.2 | 100K+ | marketplace |
| `com.lmo.customerapp` | Lawn Mowing Online *(consumer)* | 2.0 | 1K+ | marketplace |
| `com.lawnbuddyict.lawnbuddy` | Lawn Buddy *(consumer)* | 3.0 | 5K+ | marketplace |

**Reading (MEASURED).** Demand is proven and it is proven *on the operator side*: `com.lawnlove.pros`
holds **1,230 exact reviews** at 50K+ installs, `com.app.yardbook` holds 100K+ installs at 4.6★, and
the marketplace provider apps (LawnStarter for Providers 4.5★, GreenPal Pro 4.0★) show that people who
mow for money install and rate business software. Above them sits a **reachable weak mid-tier**:
LawnGuru 3.5★/1,420 reviews, Lawn Pro 3.5★/10K+, Mowbius 3.8★/1K+, Check 3.5★/500+ — the same shape
that made the electrician shelf a candidate (`docs/niche-selection.md` §2). The shelf is occupied; per
`.cursor/rules/ideation-gate.mdc` § "A competitor exists is evidence of demand", that is a point in the
proposal's favour, not a refusal.

**The counter-evidence, stated not buried (MEASURED).** Several operator-shaped apps in this shelf are
**unrated** — `com.lawnsapp` (Lawns), `io.lawn.app` (LAWN.IO), `app.mowmoney.com` (MOW MONEY),
`lawn.care1` (Lawn Care Software), `com.CerberusSoftwareAustralia.mow_quote` (Mow Quote CRM),
`com.orangeforge.lawncarelog` (Lawn Care Log). Six dormant attempts at the operator job is a real
caution, and it is the single largest risk in this proposal (§7): the pocket has money at the top
(Yardbook, provider marketplace apps) but the tail shows that *trying* does not imply traction.

### 2.5 What the incumbents' users actually complain about (MEASURED)

`node scripts/scan-complaints.mjs --package com.app.yardbook --package com.lawnprosoftware.lawnproapp
--package com.adamlyttle.mowbius --package com.caribouapps.operator --package com.lawnbook.app
--package com.adminmatic.lawncalc` — 18 reviews read, 8 critical.

> **Why these packages.** The query-level complaint run (`--query "lawn care client tracker"`) returned
> complaints from the **consumer** side of the marketplaces — homeowners waiting a week for a mower,
> being charged twice (`com.lawnlove.customers` 1★ *"This is a third party service who waits to book you
> then trys to find someone to do the work"*, `com.plowz.app` 1★ *"I have been trying to get
> resolution on a job submitted at the beginning of this month to no avail"*). That run's histogram
> (missing-feature 36%, paywall 36%) describes a shelf that does not answer the query for an operator
> tool. It is recorded here for what it is and **is not used** as evidence for this recipe.

Verbatim, from the operator-side apps:

1. **The 4.6★/100K+ leader is a desktop product with a thin mobile client behind a login.**
   `com.app.yardbook` (4.6★, 100K+):
   - 3★ — *"The app is missing some key features compared to the desktop/web version. Currently the app
     doesn't allow the user to \"add\" a new job under the Jobs t[ab]…"*
   - 3★ — *"Was good until it wasn't. Login hasn't worked in over a month. Always says incorrect
     password or Email, even when the exact same credentials work on b[rowser]…"*

2. **A cloud round loses the round.** `com.lawnprosoftware.lawnproapp` (3.5★, 10K+):
   - 1★ — *"Good till a month ago. They did a server upgrade and it wasn't done correctly, took
     everything down without warning for about 3 days and it messed up…"*
   - 1★ — *"To many software problems. Gave them the benefit of the doubt Then they deleted all my
     clients payment information."*

3. **Login and entitlement friction on a paid operator tool.** `com.adamlyttle.mowbius` (3.8★, 1K+):
   - 2★ — *"you claim you did a major upgrade but now all it is is issue after issue, keeps logging out
     then wont restore my purchase when i wanna get back…"*

4. **The operator-side price is a yearly rent, and users say so.** `com.lawnbook.app` (4.4★, 500+):
   - 3★ — *"seems like a pretty good app so far, but 80 dollars for a pretty-new app, for premium?
     that's kinda insane."*
   - 2★ — *"was excited to use this paid for the premium subscription but now the app keeps crashing
     every time I try to do anything on it"*

Complaint histogram over the eight critical operator-side reviews: `broken` 3 (38%), `missing-feature`
3 (38%), `paywall` 3 (38%), `abandoned` / `account` / `accuracy` / `confusing-ui` / `crash` /
`notifications` / `offline` / `performance` 1 each (13%), **1 unclassified of 18 read**. Counts exceed
the critical total because a review can carry two classes.

**The complaint profile in one sentence (INFERRED).** The three loudest classes on the operator side are
*broken*, *missing-feature* and *paywall*, and every one of them is a property of a **cloud product**:
the mobile app is the thin end of a web account, it breaks when the account does, and it is rented.

---

## 3. Recipe — Lawn round log (`lawn-round-log`)

**Category and job, one sentence.** Operational record-keeping for a one-person mowing round — the
customer list, the day's route in the order it will be mowed, what was actually done at each visit,
and what each customer still owes, kept on the phone with no account, no server and no signal.

**The paying user.** A solo mower, or the owner of a two-to-three-person mow crew, running roughly
20–150 recurring residential or small-commercial clients on a weekly or fortnightly round, who
currently keeps it in his head, in a paper notebook, or in a free cloud CRM he half-uses.

**What exactly is bought.** One permanent unlock of the full round: unlimited clients, the complete
visit history, the per-customer owed balance and the CSV export, with no account and no sync.

**Price.** **$9.99, one time.** For contrast, the operator-side incumbent `com.lawnbook.app` asks
$80/year (MEASURED, complaint §2.5 item 4) — the unlock is roughly one eighth of a single year of it,
and it never renews.

**The moment of payment.** When the free tier fills up (the first eight customers) or when he first
needs the visit history or the export — one clearly priced unlock sheet, no trial clock, no renewal
prompt, no second ask.

**The wedge, in one sentence.**
> **The round lives on the phone: the day's customers in the order they'll be mowed, one tap per visit
> to record mowed / skipped / rained-off, and a running "who still owes me" that opens and saves in
> airplane mode with no account — bought once instead of rented at $80 a year.**

Every clause is a difference in **functionality and content**, not in naming or colour
(`docs/monetization-strategy.md` §1.2), and each clause answers a complaint a named user wrote:
offline-without-an-account answers §2.5 items 1–3; the *round* worklist (ordered, reorderable, whole
round pushed a day by rain) answers the *missing-feature* class on a shelf whose operator apps are
job/booking shaped; the one-time price answers item 4.

### 3.1 Archetype, and the derivation

**Chosen: `purchase_led_single_unlock`** — capability required at verify time: **`billing`**.

| Archetype | Rejected because (revenue reason) |
|---|---|
| `subscription` | **Banned outright** by the owner (2026-10-01) and absent from `ARCHETYPES`; no exemption path. Recurring entitlement means server-side validation plus a renewal/refund/grace lifecycle — a server project, not a slot. Not weighed as an option. |
| `purchase_led_repeat_sales` | There is **no second unit to sell**. The round is the user's own static record: no consumable, no generation, no content pack, no per-use service cost. The only repeat-sale shape available — an annual "season" renewal — is a subscription wearing another name, which is banned. Revenue reason: zero per-unit fulfilment cost means there is nothing to price per unit, so repeat sales cannot exist here without adding a server the app does not need. |
| `ads_led_utility` | The session shape fits (short, frequent, one-handed) but **the arithmetic does not**. `docs/monetization-strategy.md` §2.1 (ON RECORD) puts an interstitial+banner posture at **~2,000–6,000 DAU** for $1,000/month, and a banner-only one at **11,000+ DAU**. A niche app with a few thousand lifetime installs is an order of magnitude short of that, so the model cannot reach the target. Revenue reason: the ad arithmetic needs an audience this niche does not have. Secondary reason, not the deciding one: a banner over the day's worklist, placed for a mower holding the phone in one gloved hand in direct sun, is a hostile placement in a tool the same user already buys (LawnBook, $80/yr). |
| `purchase_only_no_ads` | **No discovery path.** All 8 enriched lawn incumbents declare **no ads and no price** (MEASURED, §2.4), and the operator side is trained on free marketplace apps. `docs/monetization-strategy.md` §4 (ON RECORD) records the paid-up-front lever as effectively dead outside ad-hostile audiences — and a mower is not ad-hostile, he is price-sensitive. Revenue reason: a paid list price removes the only route this shelf offers a new app — install, try the round, then buy. |

### 3.2 Revenue arithmetic, with the division shown

Deduction assumptions are taken from the assumption set in `docs/monetization-strategy.md` §2.2
(**ASSUMPTION**, not measurement of this account): `F_billing = 0.15`, `F_refund = 0.02`,
`F_fx = 0.02`. There is no `C_unit`: nothing is fulfilled per unit, so no provider cost is subtracted.

```text
price                    = $9.99

divisor = (1 − F_billing)(1 − F_refund)(1 − F_fx)
        = 0.85 × 0.98 × 0.98
        = 0.81634

net per unlock = $9.99 × 0.81634 = $8.16        (≈ $8.155)

target          = $400 net / month
unlocks / month = $400 ÷ $8.16 = 49.0     →  49 unlocks / month

install → unlock conversion = 2 %         (ASSUMPTION — never measured by this factory)
installs / month = 49 ÷ 0.02 = 2,450      →  2,450 installs / month

90-day floor check (this is the kill criterion, not the target):
  2,000 installs × 2 % = 40 unlocks
  40 × $8.16           = $326 net
```

**Sanity reading (INFERRED).** 2,450 installs/month is ≈ 29K installs/year. For scale, the operator
incumbent `com.lawnlove.pros` sits in Play's 50K+ bucket and `com.app.yardbook` in 100K+ (MEASURED)
— so the target is *below* the incumbents' floors, which is the right side of that comparison, but it
is not a small number and the conversion rate is the assumption the whole projection rests on.

### 3.3 The promise contract (reason list, authored now, before any UI)

Full machine-checked version: `docs/ideation/plans/lawn-round-log.differentiators.json`.
Three `core`, two `supporting`, all `deferred`.

| id | weight | promise | evidence | label |
|---|---|---|---|---|
| `offline-round-no-account` | **core** | The round, its history and the balances open and save with the phone in airplane mode and no account; nothing in the daily path waits on a login or a server. | Yardbook (4.6★, 100K+) 3★: *"Login hasn't worked in over a month… even when the exact same credentials work on browser"*; LawnPro (3.5★, 10K+) 1★: *"a server upgrade … took everything down without warning for about 3 days"* and *"they deleted all my clients payment information"*. | MEASURED |
| `round-not-a-calendar` | **core** | A recurring seasonal round, not an appointment book: the day's customers are an ordered, reorderable list, a visit is marked mowed / skipped / rained-off in one tap, and a rained-off morning pushes the whole round by a day in one action. | `missing-feature` is 3 of 8 critical reviews (38%) on the operator side, and the shelves' operator apps are job/booking shaped; the *one-action push* half of the claim is not directly measured. | MEASURED (frequency) / INFERRED (the push) |
| `owed-per-customer` | **core** | A per-customer running balance shows who still owes and what for, marked off in one tap when cash or a transfer arrives, without an accounting suite or an invoice PDF. | The `paywall` and `accuracy` classes (3 and 1 of 8 critical) sit on operator tools whose money handling the users distrust; `what is owed` is the fourth clause of this lane's own definition. | INFERRED |
| `bought-once` | supporting | One price, no subscription, no ads, no renewal prompt. | LawnBook (4.4★, 500+) 3★: *"80 dollars for a pretty-new app, for premium? that's kinda insane"*; all 8 enriched incumbents declare no ads. | MEASURED |
| `one-handed-in-sunlight` | supporting | Every control on the day list is reachable one-handed with a 48 dp target and readable in direct sun, because it is used standing at a curb. | The shelf's users are operators working outdoors; no incumbent complaint measures tap-target size on this shelf. | INFERRED |

### 3.4 Visual direction

**Mark and palette subject.** The subject is *cut grass seen from the operator's side of the mower* —
not a leaf, a lawn or a flower. The mark is a **stripe pair**: two parallel mown swaths whose edge is a
clean diagonal cut, drawn as geometry from `store/studio.json`, reading at 48 dp and inside the
adaptive-icon safe zone. The palette's ground is a deep mown-green field with a warm neutral paper
tone for the worklist, and a single **high-visibility safety-orange** reserved for exactly one meaning
— *needs attention* (a visit not yet recorded, a balance unpaid). Two slots must not share an
identity, so this shares nothing with the reference slot's indigo/amber
(`.cursor/rules/visual-identity.mdc`).

**The one thing the slot's own components do that stock Material does not.** The **round strip**: the
day's customers as one ordered vertical itinerary where each row carries a status rail segmented into
*mowed / skipped / rained-off / owed* and a small mown-stripe mark that fills as the stop is completed,
so progress through the round is legible at a glance at arm's length in sunlight. A stock
`Card` + `ListItem` + `TopAppBar` screen cannot express a round; this component is the app's argument.

### 3.5 Build

| Item | Value |
|---|---|
| Estimate | **≈ 4 agent hours** (INFERRED) — single Kotlin/Compose module, local persistence, 4–6 screens (today's round, client list, client detail with balance, history, settings, unlock sheet). |
| Permissions | **None required.** No camera, no location, no contacts, no `POST_NOTIFICATIONS` unless rain/day reminders ship — in which case that one is requested at the moment the user enables a reminder, never at launch. (INFERRED) |
| Capabilities | `billing` (required by the archetype). `ads` deliberately **not** enabled. |
| Server | **None.** Offline by construction; no account, no sync, no backend cost. This is the point of the wedge, not a shortcut. |
| Stack | Kotlin + Compose + Material 3 + Gradle KTS, per `AGENTS.md`; local storage only. |

### 3.6 Kill criteria (three finite numbers)

```json
{ "windowDays": 90, "minInstalls": 2000, "minRevenueUsd": 300 }
```

Judged by `node scripts/check-outcomes.mjs --slot lawn-round-log` against a dated reading in
`apps/lawn-round-log/store/outcomes.json`. `minInstalls` and `minRevenueUsd` are **cumulative** window
thresholds; the pair is internally consistent with §3.2 (2,000 installs × 2% × $8.16 ≈ $326 ≥ $300),
so hitting one without the other is itself a signal that the conversion assumption is wrong.

### 3.7 Honest unknowns

- **The conversion rate is not measured by anyone here.** 2% install→unlock is an ASSUMPTION; every
  revenue and installs figure in §3.2 inherits it. No slot in this repository has ever measured a
  conversion rate.
- **Six dormant operator-shaped apps sit in this same shelf** (`com.lawnsapp`, `io.lawn.app`,
  `app.mowmoney.com`, `lawn.care1`, `com.CerberusSoftwareAustralia.mow_quote`,
  `com.orangeforge.lawncarelog`) — all unrated. Whether that is "a market that does not sustain a solo
  operator tool" or "amateurs shipping poor tools" is **UNVERIFIED**, and it is the largest single risk.
- **Willingness to pay from a solo mower is unproven.** The complaints show paying users exist on the
  operator side, but every one of them is paying a *marketplace* or a *cloud CRM*, not an offline
  notebook. Whether he pays $9.99 once is UNVERIFIED.
- **`minInstalls 2000` may be unachievable from search alone.** The measured shelf is dominated by
  free marketplace apps with 50K–100K+ installs; ranking against them is assumed, not planned.
- **My own classifier was too broad** on the cleaning shelf (§2.2) and produced a 1.00 job-share that
  is an artefact. The lawn shelf's 0.97 carries the same risk in weaker form, which is why §2.4 is
  hand-read rather than quoted from the tool.
- **The `one-action rain push` and the `owed balance` claims are INFERRED**, not measured on this
  shelf; they are the two promises most likely to be narrowed during the build.

---

## 4. Recommendation

**Build `lawn-round-log`** if the owner wants a slot in this lane. It is the only query in the set with
a proven operator-side demand reading (1,230 and 1,740 exact reviews on operator/provider apps,
100K+ installs on the operator leader), a reachable weak mid-tier, a complaint set that names the fix,
no server, no permissions and a ~4-hour build. It is proposed as **one** recipe, not two, for the
reason in the preamble.

The single thing to decide before a slot exists is §3.7's second bullet: the six dormant attempts. If
the owner reads them as a dead shelf rather than as weak supply, the honest response is to refuse the
whole lane rather than to soften the reading.

---

## 5. Refusals — every query screened, with its revenue reason

| Query | Screen verdict | Refused because (revenue reason) |
|---|---|---|
| `window cleaner round tracker` | `mixed`, guard partial, 24/30 rated, median 4.35 | **Duplicate of our own catalogue.** One sentence — *"a solo trade's round on the phone"* — describes it and `lawn-round-log`; two of our slots that one sentence describes are one recipe, and the Repetitive Content policy's remedy reaches the whole account (`docs/monetization-strategy.md` §1.2). Secondary: the cleaning pro-side apps are thin (§2.2), so its own demand reading is worse than the lawn shelf's. |
| `cleaning business client log` | `candidate`, guard partial (11/25) | **Duplicate of our own catalogue** (same job sentence). Secondary, and it is a demand reading rather than a vacancy: the cheapest direct competitor for this exact job — `com.clearstackapps.maiddesk`, *House Cleaner Client Book* — is **unrated at 0+ installs** (MEASURED), and the rated apps on the shelf (`com.tidyapp.pro` 3.6★, `com.turnoverbnb.app` 3.4★, `com.getcleaner.propro` 2.0★) are **gig marketplaces**, not the cleaner's own book. |
| `pet sitting client log` | `candidate`, 18/30 rated, median 4.10 | **Duplicate of our own catalogue.** Still *"a solo trade's client log"*; the differences (per-pet care notes, key handover) are content inside the same job sentence, not a different job. A separate slot would be a near-duplicate of our own app. |
| `snow removal route log` | `candidate`, guard `on-topic` 0.67, hand 0.80 | **Unrated demand reading.** 8 of the 24 on-job cards are **plow simulator games** and the genuine snow-removal business apps (`com.cecasnowpro`, `com.aethermint.snow_removal_manager`) are **unrated** — the *people tried, nobody got traction* shape (`docs/niche-selection.md` §2). This is not a competition refusal: the games are irrelevant, and the tool shelf is unrated. |
| `garden maintenance client schedule` | `candidate` but **POLLUTED** | **Not readable, and a duplicate.** The screen flagged the shelf POLLUTED itself, so no verdict can be drawn from it; and the surviving job sentence duplicates `lawn-round-log`. |
| `habit tracker` (control) | `fortress` | Control only — a known fortress (`docs/niche-selection.md` §2) and not in this lane. |
| `bjj training log` (control) | `dead-zone` | Control only — a known dead zone (`docs/niche-selection.md` §2) and not in this lane. |

**Also refused, as topics rather than as queries:** calculators / unit converters / generic arithmetic
tools (a closed topic, `.cursor/rules/ideation-gate.mdc`); anything on the killed list in
`docs/research/2026-09-24-market-decision.md` §3, including the neighbouring `field service work order
app` (a Jobber/Housecall Pro fortress — the reason this proposal is a *round log*, not a work-order
suite) and `dog groomer appointment book` (appointment booking, a different family from a round).

**Taken in our own catalogue, checked by `id`/`name`/`idea` only:** a pipeline smoke test, a throwaway
pipeline proof, a US gas pipe sizing workbook, and a private offline classroom record book for
teachers. No solo-trade round or client log is taken; the classroom record book is a different job in a
different category, so this recipe is not a near-duplicate of it.

---

## 6. Falsifiable claim

> **The pocket:** solo lawn-care operators who install business software and rate it — measured at
> `com.lawnlove.pros` 4.6★ / **1,230 exact reviews** / 50K+ installs and `com.app.yardbook` 4.6★ /
> 100K+ installs, with a weak mid-tier at 3.5–3.8★. **Label: MEASURED** (Play search + enrichment,
> 2026-10-01, `scripts/enrich-market.mjs`).
>
> **What would flip it to FAIL:** a hand-classified read showing that the operator-side apps carrying
> those review counts are **marketplaces** (the operator is a gig worker on someone else's platform,
> not a business owner keeping his own book) rather than tools the operator chooses and buys. If the
> only users who install and rate business software in this shelf are gig workers dispatched by
> LawnStarter/GreenPal, then the paying user this proposal names does not exist at that scale, and the
> 2,450 installs/month arithmetic collapses.

A second, cheaper falsifier: any reading showing `com.app.yardbook`'s 100K+ installs are
desktop-account holders rather than solo operators would remove the operator-leader evidence, leaving
only the marketplace rows.

## 7. What I could not measure

- Install-to-unlock conversion, retention, or any revenue outcome: **not measurable from Play search**,
  and no reading exists in this repository.
- Exact review counts for the operator-side incumbents outside the enriched top 8 (`com.app.yardbook`,
  `com.lawnprosoftware.lawnproapp`, `com.adamlyttle.mowbius`, `com.caribouapps.operator`): the enrichment
  tool reads search cards, and these sit below the top-8 cut, so their **installs are Play's rounded
  buckets (floors), not counts**.
- Whether the six unrated operator-shaped apps are dead or merely new: no measurement distinguishes
  them, and the document says so rather than picking the flattering reading.
- Play Console purchasable price points for `$9.99` in the account's currencies: UNVERIFIED
  (`docs/monetization-strategy.md` §2.2 records the same gap).

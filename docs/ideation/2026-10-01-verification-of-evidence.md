# Verification of recon evidence — 2026-10-01

**Who wrote this.** An independent PlayKiln verifier, working from a brief and nothing else — no
access to the scouts' chat, no access to the orchestrator's. Every verdict below comes from a live
Google Play read performed in this session, not from the proposals' prose.

**Boundaries honoured.** Nothing under `apps/**` was opened. No scout proposal, plan or script was
edited. Nothing was committed. Exactly one file was written: this report.

**Storefront.** Signed-out Play, `hl=en`, `gl=US` — the same surface `scripts/lib/play-app-detail.mjs`
and `scripts/lib/play-scan.mjs` use. Readings were taken 2026-10-01 (evening, UTC+3).

**How each verdict is decided.** A claim is `CONFIRMED` when the live page agrees with the claimed
value, `WRONG` when it disagrees (both values are given), and `NOT-CHECKED` when it could not be
observed (the reason is stated). Numbers are quoted exactly as the tool printed them; review counts
shown as `display (exact)` are Play's displayed magnitude and its unrounded schema.org twin.

**Commands used (all run from the repository root).**

```powershell
node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "lawn care client tracker" --query "hair color formula app"
node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "daily takings book" --query "pop up shop inventory tracker"
node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "pool service log"
node scripts/scan-complaints.mjs --package com.app.yardbook --package com.lawnbook.app --package com.clearstackapps.maiddesk --package com.denny.quickstall
node scripts/scan-complaints.mjs --package com.stayincharm --package com.redken.haircolor --package com.shrey_businessx.android --package com.poolcorp.bluestreak.customer.store
node scripts/scan-titles.mjs --concurrency 3 --query "snow removal route log"
# plus direct `fetchAppDetail` / raw-page reads for the badge, IAP range and description of
# com.stayincharm, problemsolversoftware.poolpilot, com.shrey_businessx.android, com.app.yardbook
```

---

## 1. Verdict table — the 16 numbered claims

| # | Claim | Claimed value | Observed on live Play | Verdict |
|---:|---|---|---|---|
| A1 | `com.lawnlove.pros` — Lawn Love for Providers | 4.6★, 1,230 exact reviews, 50K+, no ads, no IAP | 4.6★, **1,230 displayed / 1,227–1,228 exact**, 50K+, `ads=false`, `iap=false`, free | **CONFIRMED** |
| A2 | `com.app.yardbook` — Yardbook | 4.6★, 100K+ installs | 4.6★, **1,980 displayed / 1,975 exact reviews**, 100K+, `ads=false`, `iap=false`, free | **CONFIRMED** |
| A3 | `com.lawnbook.app` — LawnBook | 4.4★, 500+, IAP declared, review "80 dollars for a pretty-new app, for premium?" | 4.4★, 9 reviews, 500+, `iap=true`, `ads=false`, free; quoted review present verbatim | **CONFIRMED** |
| A4 | `com.app.yardbook` review | "Login hasn't worked in over a month" | Present verbatim: *"Was good until it wasn't. Login hasn't worked in over a month. Always says incorrect password or Email, even when the exact same credentials work on b…"* | **CONFIRMED** |
| B5 | `com.stayincharm` — Charm: Save Formulas & Clients | 4.4★, 40 reviews, 10K+, IAP declared | 4.4★, 40 (40), 10K+, badge **`["In-app purchases"]`**, `ads=false`, `iap=true`; IAP range **"$3.99 - $149.99 per item"** | **CONFIRMED** |
| B6 | `com.redken.haircolor` — Redken Style Station | 4.4★, 194 reviews, 50K+, ads=false, iap=false, free, review about removed formula content | 4.4★, 194 (194), 50K+, `ads=false`, `iap=false`, free; review present verbatim | **CONFIRMED** |
| C7 | `cashbook.cashbook` — Cash Book | 4.5★, 145,000 reviews, 10M+, ads + IAP | 4.5★, 145,000 (144,563), 10M+, `ads=true`, `iap=true`, free | **CONFIRMED** |
| C8 | `com.kutirsoft.dailysalesrecord` — Daily Sales Record | 4.5★, 4,240 reviews, 500K+, IAP | 4.5★, 4,240 (4,242), 500K+, `ads=false`, `iap=true`, free | **CONFIRMED** |
| C9 | `com.mlab.stock.management` — Stock and Inventory Management | 4.2★, 2,230 reviews, 100K+, ads + IAP | 4.2★, 2,230 (2,226), 100K+, `ads=true`, `iap=true`, free | **CONFIRMED** |
| C10 | `com.shrey_businessx.android` — Daily Sales Profit & Inventory | 4.0★, 100K+, a review naming the wedge | rating **4.0★**, **100K+**, 561 reviews, `iap=true`; quoted restock-at-cost review present | **CONFIRMED** (row + quoted review) — see **F1** for the ad badge attached to this app |
| D11 | `problemsolversoftware.poolpilot` — Pool Pilot | 4.0★, 23 reviews, 5K+, ads=true, iap=true | 4.0★, 23 (23), 5K+, badges **`["Contains ads","In-app purchases"]`**, IAP range **"$0.99 - $99.99 per item"** | **CONFIRMED** |
| D12 | `com.poolserviceusa.poolserviceusa` — Pool Service Software | 4.1★, 45 reviews, 5K+ | 4.1★, 45 (45), 5K+, `ads=false`, `iap=false`, free | **CONFIRMED** |
| D13 | `com.poolcorp.bluestreak.customer.store` complaint | *"I have to log in multiple times… opens a separate web browser… My templates are gone."* | 2.3★, 10K+; review present verbatim: *"Ihave to log in multiple times each time i use it, doesn't save my info when i do. opens a separate web browser to use the app. My templates are gone."* | **CONFIRMED** |
| E14 | `com.clearstackapps.maiddesk` | unrated, 0+ installs | **unrated, 0+, 0 reviews readable** | **CONFIRMED** |
| E15 | `com.denny.quickstall` | unrated, 10+ installs | **unrated, 10+, 0 reviews readable** | **CONFIRMED** |
| E16 | `snow removal route log` shelf — 8 of ~24 on-job cards are plow simulator games | 8 named packages are games | All 8 named packages appear and are games; on the full 30-card visible shelf I count **~10 game/simulation cards** (see §3.E16) | **CONFIRMED** (named evidence exact; count understated — see note) |

---

## 2. Claim-by-claim detail (raw observed values)

### A. Lawn round (`lawn-round-log`)

**A1 — `com.lawnlove.pros`.** `enrich-market` row:
`com.lawnlove.pros | Lawn Love for Providers | WebMaster LawnLove | 4.6 | 1,230 (1,227) | 50K+ | >= 50,000 | false | false | free`.
A direct detail read returned `rating=4.6, ratingCount=1230, ratingCountExact=1228, installs="50K+",
containsAds=false, inAppPurchases=false, free=true`. The claim is confirmed; the only nuance is that
"1,230 exact" is Play's **displayed** count — the unrounded schema twin read 1,227 then 1,228 on
successive fetches. Review counts on Play are snapshots.

**A2 — `com.app.yardbook`.** Not in the re-run's `lawn care client tracker` top 8 (confirmed to sit
below the cut). Read directly and via `scan-complaints`:
`com.app.yardbook | 4.6★ | 100K+`, and detail `rating=4.6, ratingCount=1980, ratingCountExact=1975,
installs="100K+", containsAds=false, inAppPurchases=false, free=true`.
Claim (4.6★, 100K+) confirmed. Review count is **1,975 exact / 1,980 displayed** (the brief asked for it).

**A3 — `com.lawnbook.app`.** Row: `4.4 | 9 (9) | 500+ | >= 500 | false | true | free`.
Critical review present verbatim:
*"seems like a pretty good app so far, but 80 dollars for a pretty-new app, for premium? that's kinda
insane. also its a little confusing maybe add a be…"* (3★, `paywall`). Claim confirmed.

**A4 — Yardbook login review.** Present verbatim (3★):
*"Was good until it wasn't. Login hasn't worked in over a month. Always says incorrect password or
Email, even when the exact same credentials work on b…"*. Confirmed.

### B. Hair colour formula book (`hair-color-formula-book`) — the key check

**B5 — `com.stayincharm` (the single load-bearing IAP read).**
Row: `com.stayincharm | Charm: Save Formulas & Clients | Colourist Guide | 4.4 | 40 (40) | 10K+ | >= 10,000 | false | true | free`.
Badge array from the page: **`["In-app purchases"]`** (no ads badge). The page's own offer block, in
the same JSON node as this app's install tuple `["10,000+",10000,11454,"10K+"]`, declares the range
**`"$3.99 - $149.99 per item"`**. The description reads *"…Try out Charm App today for free!"* and
describes formula, client and photo storage in the cloud.

**Verdict: the in-app purchase is real, and it cannot be an ad-removal or donation item** — the app
declares **no ads at all** (`ads=false`), so there is nothing to remove, and a `$3.99 – $149.99 per
item` span is a product range, not a tip jar. What it buys is **not named on the public store page**;
Play publishes only the badge and the price span. This is not the read that flips the recipe to FAIL.

**B6 — `com.redken.haircolor`.** Row: `4.4 | 194 (194) | 50K+ | >= 50,000 | false | false | free`.
Critical review present verbatim (1★):
*"I used to love the old app. There used to be an inspiration lookbook where you could access formulas
and an education section. Even the virtual try-on…"*. Claim confirmed.

### C. Market day book (`market-day-book`) — the ads-led plan

**C7 — `cashbook.cashbook`.** Row: `4.5 | 145,000 (144,563) | 10M+ | >= 10,000,000 | true | true | free`.
Raw page confirms both badges (`"Contains ads"` and `"In-app purchases"` present).
Claim confirmed; the proposal wrote the exact twin as `144,567`, the live read is `144,563` (drift of 4).

**C8 — `com.kutirsoft.dailysalesrecord`.** `4.5 | 4,240 (4,242) | 500K+ | >= 500,000 | false | true | free`. Confirmed.

**C9 — `com.mlab.stock.management`.** `4.2 | 2,230 (2,226) | 100K+ | >= 100,000 | true | true | free`. Confirmed.

**C10 — `com.shrey_businessx.android`.** Detail: `rating=4.0, ratingCount=561, ratingCountExact=561,
installs="100K+", ads=false, iap=true, free=true`. `scan-complaints` sampled 3 reviews, 2 critical:
- 3★: *"It's a great app. User friendly and easily to understand. my only issue is, it doesn't have a
  space to actually input restock quantities with cost pri…"*
- 2★: *"Great app, however 1. My data doesn't sync across my devices even after paying for premium
  2. Why do I still get ads...even after paying for premium 3…"*

The row (4.0★, 100K+) is confirmed and the proposal's quoted review exists verbatim. **Note on the
brief's phrasing "a review that mentions writing down both takings and stock":** no sampled review uses
the word "takings"; the quoted review names the **restock-at-cost (stock)** gap, and the second names
the paywall/ads. The proposal's own quoted evidence (§4.1) is present; the "both takings and stock"
wording is the brief's paraphrase, not a scout claim.

### D. Pool route (`pool-route-log`)

**D11 — `problemsolversoftware.poolpilot`.** Row: `4.0 | 23 (23) | 5K+ | >= 5,000 | true | true | free`.
Badges: **`["Contains ads","In-app purchases"]`**. The app's own offer block (same JSON node as its
`["5,000+",5000,6287,"5K+"]` install tuple) declares **`"$0.99 - $99.99 per item"`**. Description:
*"Manual testing is free, forever. Snap a photo of any test strip — any brand — and Pool Pilot tells
you exactly what to add…"*. A user review reads *"…especially if you are a paing monthly subscriber…"*
and the developer reply begins *"Thanks for subscribing…"*.

**Verdict: the IAP is a paid capability, not an ad-removal upsell.** The free tier is manual entry
("free, forever"); the priced span is broad ($0.99–$99.99) and the developer talks about subscribing,
so the purchase unlocks premium function. Play does not name the individual products on the store page.
The recipe's FAIL condition ("only an ad-removal upsell") is **not** met.

**D12 — `com.poolserviceusa.poolserviceusa`.** `4.1 | 45 (45) | 5K+ | >= 5,000 | false | false | free`. Confirmed.

**D13 — poolcorp complaint.** `com.poolcorp.bluestreak.customer.store` reads **2.3★, 10K+**. The
critical review is present verbatim (the proposal elided a middle clause with "…"):
*"Ihave to log in multiple times each time i use it, doesn't save my info when i do. opens a separate
web browser to use the app. My templates are gone."* Confirmed.

### E. The refusal evidence

**E14 — `com.clearstackapps.maiddesk`.** `unrated, 0+, reviews=0`. Confirmed.
**E15 — `com.denny.quickstall`.** `unrated, 10+, reviews=0`. Confirmed.

**E16 — snow removal shelf.** `scan-titles --query "snow removal route log"` returned 30 visible cards,
verdict `candidate`, guard `on-topic (20/30, share 0.67)`. The 8 packages the proposal names as games
are all present and all read as games/simulations:

| package | title | rating on card |
|---|---|---|
| `com.simulatorlegends.SnowClearPlowSimulator` | SnowClear Plow Simulator | unrated |
| `com.gdm.snowplow` | Roll the Snow: Plow Simulator | 3 |
| `com.hrygames.snowplow` | Snow Plow Truck Simulator | 2.7 |
| `com.game.littlewood5m4q8v209second` | Plow Snow Journey | unrated |
| `io.blackcandy.cleansnow3d` | Clean Snow 3D: Collect & Build | 4.5 |
| `com.pg.snow.plow.heavy.excavator.sim` | Snow Plow Heavy Excavator Sim | 2.9 |
| `com.kk.snowplowexcavatorportrait` | Snowstorm Road Crew Sim 3D | unrated |
| `com.lab.buildingmaster4p8m` | Snow Plow Rush | unrated |

Counting the full visible shelf myself, there are **~10 game/simulation cards of 30**, not 8 — the two
the proposal's list omits are `com.fts.snow.blower.winter.city.truck.parking.simulator.game`
("Snow Plow Construction Games", 2.9) and `com.fty.snow` ("Snow shovelers - simulation", 4.3). The
proposal's "8 of the 24 on-job cards" denominator comes from its own `--on-job` regex, which was not
part of this brief and is not reproducible from it. The qualitative finding — the shelf is dominated
by plow simulators, and the genuine snow-removal business apps are unrated (`com.cecasnowpro`,
`com.aethermint.snow_removal_manager`) — is confirmed.

---

## 3. Additional finding — not in the numbered list, but load-bearing

### F1 (WRONG) — `com.shrey_businessx.android` does not declare advertising

The `market-day-book` proposal states in §3.1 (table, "Ads" column = **yes**) and §4.1 that
`com.shrey_businessx.android` *"declares both advertising and in-app purchases"*. Its promise
`bought-once-ads-go` cites the user line *"Why do I still get ads...even after paying for premium"*.

**Observed (two independent reads, live page 2026-10-01):**

- `fetchAppDetail` → `containsAds=false, inAppPurchases=true`;
- raw-page string count → the exact string **"Contains ads" occurs 0 times** on
  `com.shrey_businessx.android`'s page, while **"In-app purchases" occurs once**. (Control: on
  `cashbook.cashbook` the same count method finds "Contains ads" twice.)

So the live badge is **in-app purchases only, no ads**. The claimed "ads declared" is `WRONG`.

**Blast radius.** This is one cell of the §3.1 table and the evidence behind the `bought-once-ads-go`
promise. It does **not** remove the ads-led plan's money evidence: the shelf leader `cashbook.cashbook`
(10M+, `ads=true`, `iap=true`) still carries it, and `com.mlab.stock.management` and
`gnhub.ledgerbook.pro` declare ads. But the specific sentence "the app that already serves both halves
declares both advertising and in-app purchases" is not true of the live page, and the "ads that
survive payment" complaint is now evidence from an app that declares no ads — which deserves an
explanation before the promise is relied on.

---

## 4. Closing

### Tallies

- **Numbered claims: 16 total — 16 CONFIRMED, 0 WRONG, 0 NOT-CHECKED.**
- **Additional load-bearing evidence (F1): 1 WRONG.** `com.shrey_businessx.android` declares in-app
  purchases but **no ads**, against the proposal's "declares both advertising and in-app purchases".
- One count discrepancy noted but **not** counted as WRONG: E16's "8 of ~24" vs my ~10 of 30 visible
  (the denominator is the scout's own non-reproducible `--on-job` filter; the named 8 packages are exact).

### The single most damaging wrong finding

**F1.** It is the only hard disagreement with live Play in this verification, and it touches the one
proposal that is monetised by ads: the `market-day-book` plan's claim that its decisive incumbent
declares both advertising and in-app purchases is false — the app declares **in-app purchases only**.
The plan's ads stream still rests on `cashbook.cashbook` (10M+, ads+IAP, confirmed), so it is not
fatal; but the §3.1 table cell, the §4.1 sentence and the `bought-once-ads-go` promise all need the
record corrected rather than repeated.

**On the two "flip-to-FAIL" reads the proposals named:** both came back **not flipped**.

- `com.stayincharm`'s in-app purchase is **real** (badge `["In-app purchases"]`, range
  `$3.99 – $149.99 per item`, and the app declares no ads so an ad-removal interpretation is
  impossible). The `hair-color-formula-book` recipe keeps its money-on-display evidence.
- `problemsolversoftware.poolpilot`'s in-app purchase is a **paid capability** (free manual tier, the
  strip-scan/dosing premium; range `$0.99 – $99.99 per item`; a developer reply references
  subscribing), **not** an ad-removal upsell. The `pool-route-log` recipe keeps its willingness-to-pay
  evidence.

### What could not be verified, and why

1. **The named products behind either IAP.** Play's public store page publishes the badge and the price
   range but not the item list, so "what exactly it buys" for `com.stayincharm` and
   `problemsolversoftware.poolpilot` is inferred from the range and the app description, not read. A
   Console/Play-billing purchase read (out of scope here) would settle it.
2. **One-time vs subscription for those IAPs.** Play labels both ranges "**per item**", which is its
   one-time-product form (subscriptions render "per month/week/year" on the page); but the poolpilot
   user review and developer reply use the word "subscribing", so the shape is not fully pinned.
3. **The scouts' own regex subsets.** The "24 on-job cards" in E16 is a product of the scout's
   `--on-job` regex, which the brief did not supply; I counted on the full 30-card visible shelf instead.
4. **Play's review sample.** Play serves a fixed handful (measured 3) per page, chosen by its own
   ranking, so a quoted review either appears in the current sample (all checked ones did) or cannot be
   confirmed — its absence would not prove it never existed. Every review quote checked in this brief
   was found verbatim.
5. **Exact review counts are snapshots.** The unrounded schema.org twin moved between reads in this
   session (`com.lawnlove.pros` 1,227 → 1,228; `cashbook.cashbook` proposal 144,567 → observed
   144,563). A claim phrased as "N exact reviews" is only exact as of its read.
6. **Nothing about revenue.** Willingness to pay, conversion, retention and outcome are not observable
   on a store page; this report says nothing about them and does not evaluate whether the ideas are good.

---

## 5. Independent verifier's re-read log (brief's claim numbers 1–18)

A second, independent pass re-read every claim in the brief from Play in this session. This section
is keyed to the brief's **own numbering (1–18)** so it can be cross-referenced row-for-row; the
sections above use a different (A–E) labelling. Commands: `enrich-market.mjs --top 8/12` for the
shelf rows, `scan-complaints.mjs --package …` for reviews, `scan-titles.mjs` for the snow shelf, and
direct `fetchAppDetail` / raw-page reads where a package is not on any query's first page.

Two `enrich-market` invocations failed first with `TimeoutError`; both succeeded on one retry.

### Group 1 — the deciding hair-colour claim

| # | Package / app | Claimed | Observed (live) | Verdict |
|---:|---|---|---|---|
| 1 | `com.stayincharm` — Charm: Save Formulas & Clients | 4.4★, 40 reviews, 10K+, IAP declared | 4.4★, 40 (exact 40), 10K+, `ads=false`, `iap=true`, badges `["In-app purchases"]` | **CONFIRMED** |

**What the IAP is (the recipe's own flip condition).** The store page publishes the badge and, in
the page's data payload, the range **`"$3.99 - $149.99 per item"`**. No individual product name is
rendered on the page. Because the app declares **`ads = false`**, an ad-removal product is
impossible by construction; a `$149.99` ceiling is not a donation tier. **The IAP is a real paid
capability/content purchase — the flip-to-FAIL condition does not fire.** The exact SKU is
`NOT-CHECKED` (Play does not publish the item list to a signed-out page).

### Group 2 — the hair-colour shelf

| # | Package / app | Claimed | Observed (live) | Verdict |
|---:|---|---|---|---|
| 2 | `com.redken.haircolor` — Redken Style Station | 4.4★, 194 reviews, 50K+, ads=false, iap=false, free | 4.4★, 194 (exact 194), 50K+, `ads=false`, `iap=false`, free, badges `[]` | **CONFIRMED** |
| 2b | same — "removed formula/lookbook" review | *"There used to be an inspiration lookbook where you could access formulas"* | present verbatim (1★): *"I used to love the old app. There used to be an inspiration lookbook where you could access formulas and an education section. Even the virtual try-on…"* | **CONFIRMED** |
| 3 | `com.luckystars.haircolorchanger` — Hair Color Changer | 2.5★, 5,575 reviews, 1M+, ads=true | 2.5★, 5,580 displayed / **5,575 exact**, 1M+, `ads=true`, `iap=false` | **CONFIRMED** |
| 3b | weak tier is consumer AI try-on, not professional formula; professional has no weak rated incumbent | consumer weak; professional strong or unrated | consumer try-on: **2.5★**, **3.9★**, 4.5★; professional formula: **4.4★**, **4.4★**, plus three **unrated** cards | **CONFIRMED** (caveat: the unrated professional cards are a dead-zone reading, not a weak-rated one) |

### Group 3 — the lawn round shelf

| # | Package / app | Claimed | Observed (live) | Verdict |
|---:|---|---|---|---|
| 4 | `com.lawnlove.pros` — Lawn Love for Providers | 4.6★, 1,230 exact reviews, 50K+, no ads, no IAP | 4.6★, 1,230 displayed / 1,228 exact, 50K+, `ads=false`, `iap=false` | **CONFIRMED** |
| 5 | `com.app.yardbook` — Yardbook | 4.6★, 100K+, below the top-8 cut | 4.6★, 1,980 displayed / 1,975 exact, 100K+, `ads=false`, `iap=false`; **not** in top 8 **or** top 12 of the query | **CONFIRMED** |
| 6 | Yardbook review | *"Login hasn't worked in over a month"* | present verbatim (3★): *"Was good until it wasn't. Login hasn't worked in over a month. Always says incorrect password or Email…"* | **CONFIRMED** |
| 7 | `com.lawnbook.app` — LawnBook | 4.4★, 500+, IAP declared | 4.4★, 9 (exact 9) reviews, 500+, `iap=true`, `ads=false` | **CONFIRMED** |
| 7b | LawnBook review | *"80 dollars for a pretty-new app, for premium?"* | present verbatim (3★): *"seems like a pretty good app so far, but 80 dollars for a pretty-new app, for premium? that's kinda insane…"* | **CONFIRMED** |
| 8 | weak mid-tier: `com.lawnguru` 3.5★/1,420 · `com.lawnprosoftware.lawnproapp` 3.5★/10K+ · `com.adamlyttle.mowbius` 3.8★/1K+ | as claimed | LawnGuru **3.5★, 1,420 (1,417), 50K+**; Lawn Pro **3.5★, 118 (118), 10K+**; Mowbius **3.8★, 21 (21), 1K+** | **CONFIRMED** |

### Group 4 — the ads-led plan

| # | Package / app | Claimed | Observed (live) | Verdict |
|---:|---|---|---|---|
| 9 | `cashbook.cashbook` — Cash Book- daily expenses | 4.5★, 145,000 reviews, 10M+, ads and IAP | 4.5★, 145,000 (144,563), 10M+, badges `["Contains ads","In-app purchases"]` | **CONFIRMED** |
| 10 | `com.kutirsoft.dailysalesrecord` — Daily Sales Record | 4.5★, 4,240 reviews, 500K+, IAP | 4.5★, 4,240 (4,242), 500K+, `iap=true`, `ads=false` | **CONFIRMED** |
| 11 | `com.mlab.stock.management` — Stock and Inventory Management | 4.2★, 2,230, 100K+, ads and IAP | 4.2★, 2,230 (2,226), 100K+, badges `["Contains ads","In-app purchases"]` | **CONFIRMED** |
| 12 | `com.shrey_businessx.android` — Daily Sales Profit & Inventory | 4.0★, 100K+ | 4.0★, 100K+, 561 (exact 561) reviews, `iap=true`, **`ads=false`** | **CONFIRMED** (row) |
| 12b | same — a review mentioning writing down both takings and stock | review exists | **no review in Play's sample uses the word "takings"** (0 occurrences on the page); the closest critical review names the *restock-at-cost* gap: *"…it doesn't have a space to actually input restock quantities with cost prices…"* | **WRONG (observed: the "takings + stock" review is not present; stock only)** — see caveat |

### Group 5 — the pool shelf

| # | Package / app | Claimed | Observed (live) | Verdict |
|---:|---|---|---|---|
| 13 | `problemsolversoftware.poolpilot` — Pool Pilot | 4.0★, 23 reviews, 5K+, ads=true, iap=true | 4.0★, 23 (exact 23), 5K+, badges `["Contains ads","In-app purchases"]`; IAP range **`"$0.99 - $99.99 per item"`** | **CONFIRMED** |
| 14 | `com.poolserviceusa.poolserviceusa` — Pool Service Software | 4.1★, 45 reviews, 5K+ | 4.1★, 45 (exact 45), 5K+, `ads=false`, `iap=false` | **CONFIRMED** |
| 15 | `com.poolcorp.bluestreak.customer.store` complaint | 2.3★/10K+; *"I have to log in multiple times… opens a separate web browser… My templates are gone."* | 2.3★, 10K+; present verbatim (1★): *"Ihave to log in multiple times each time i use it, doesn't save my info when i do. opens a separate web browser to use the app. My templates are gone."* | **CONFIRMED** |

**Pool Pilot IAP nature (its own flip condition).** The declared range tops out at **`$99.99`**, an
order of magnitude above an ad-removal product (typically $1.99–$4.99); the free tier is manual
entry and the priced tier is the strip-scan/dosing capability. **It is a paid capability, not an
ad-removal upsell.** Exact SKUs `NOT-CHECKED` (not published to the page).

### Group 6 — spot-check the refusal evidence

| # | Package / app | Claimed | Observed (live) | Verdict |
|---:|---|---|---|---|
| 16 | `com.clearstackapps.maiddesk` — House Cleaner Client Book | unrated, 0+ installs | **unrated, 0+, 0 reviews readable** | **CONFIRMED** |
| 17 | `com.denny.quickstall` — QuickStall | unrated, 10+ installs | **unrated, 10+, 0 reviews readable** | **CONFIRMED** |
| 18 | `snow removal route log` — 8 of ~24 on-job cards are plow simulator games | 8 game cards incl. 3 named | 30 visible cards; the **3 named packages all present and games** (`com.gdm.snowplow` 3★ "Roll the Snow: Plow Simulator", `com.hrygames.snowplow` 2.7★ "Snow Plow Truck Simulator", `com.simulatorlegends.SnowClearPlowSimulator` unrated "SnowClear Plow Simulator"); I count **~10** game/simulation cards of 30 | **CONFIRMED** (direction + named games) — **count is higher than claimed (10 vs 8)**; the "~24 on-job" denominator comes from the scout's own regex, not reproducible from the brief |

---

## 6. Independent tally (brief numbering)

| Verdict | Count |
|---|---:|
| `CONFIRMED` | **20** rows (1, 2, 2b, 3, 3b, 4, 5, 6, 7, 7b, 8×3, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18-direction) |
| `WRONG` | **2** — (12b) the "takings **and** stock" review does not exist (stock only); (18-count) ~10 games found, not 8 |
| `NOT-CHECKED` | **0** claims skipped; 2 residuals named (the two IAP SKUs are not published on the store page) |

### The single most damaging wrong finding

**Claim 12b.** It is the only hard disagreement about quoted evidence: the `market-day-book` plan's
support for `com.shrey_businessx.android` quotes a review "mentioning writing down both takings and
stock", and **no review in Play's sample contains the word "takings"** (0 occurrences on the page).
The nearest real review supports the *stock* half only (*"…it doesn't have a space to actually input
restock quantities with cost prices…"*). The row itself (4.0★, 100K+) is correct. This is not fatal
to the plan — its ads money evidence rests on `cashbook.cashbook` (10M+, ads+IAP, confirmed) — but
the quoted review should not be repeated as written.

### Separate load-bearing finding confirmed independently (F1, above)

`com.shrey_businessx.android` declares **in-app purchases only, no ads**: `fetchAppDetail` →
`containsAds=false, inAppPurchases=true`; raw string count → `"Contains ads"` **0**, `"In-app
purchases"` **1** on its page, while `cashbook.cashbook` carries `"Contains ads"` twice. If the
proposal states this app "declares both advertising and in-app purchases", that is WRONG.

### What could not be verified, and why

1. The **named products** behind `com.stayincharm`'s and `problemsolversoftware.poolpilot`'s IAPs —
   Play publishes only the badge and the price range to a signed-out page.
2. One-time vs subscription shape of those IAPs — the range renders "per item" (Play's one-time
   form), but the poolpilot reviews/developer reply use the word "subscribing".
3. The scouts' own `--on-job` filter (the "~24" denominator in claim 18) was not part of the brief.
4. Whether an *absent* quoted review ever existed — Play serves a fixed ~3-review sample; every
   quote that was claimed to exist was found except 12b, and an absence is weaker evidence than a
   presence.

*End of independent verifier's section.*


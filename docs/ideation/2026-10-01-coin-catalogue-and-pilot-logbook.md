# Two recipes — ideation proposal, 2026-10-01

**What this is.** A decision-grade proposal of two app ideas for the owner to choose between, one of
which I recommend. Each recipe states its demand evidence, its unit arithmetic, its reason list, its
build cost and its kill criteria. The gate-ready halves are in
`docs/ideation/plans/<slug>.economics.json` and `docs/ideation/plans/<slug>.differentiators.json`.

**What this is not.** It is not a build instruction and neither recipe is a slot. No slot directory
exists for either, and none is created by this document.

**How to read the labels.** Every factual claim carries one of four labels:

| Label | Means |
|---|---|
| MEASURED | I ran a command in this session and this is what it printed. The command is named. |
| ON RECORD | A file in this repository states it. The path is given. |
| INFERRED | My reasoning from MEASURED or ON RECORD facts. |
| UNVERIFIED | A number or claim I am carrying without evidence. It is listed again in §9. |

No number below was invented. Where a figure is absent, I say it is absent.

---

## 1. Method, and exactly what was run

All commands ran from the repository root on 2026-10-01. Network was available; no sensor was
blocked. `node scripts/kiln-status.mjs` returned `VERDICT: READY` in 9.2 s with every environment row
PASS (`node v24.11.1`, factory JDK 17, `ANDROID_HOME` at `E:\Programm\Android\SDK`, build-tools
36.0.0, Pillow 11.3.0, keytool, Codex CLI, five MCP servers), and the registry reports 4 apps
(`killed=1, published=1, scaffolded=2`). The self-test failure recorded in the 2026-09-26 proposal is
gone.

| # | Command | Observed result |
|---|---|---|
| 1 | `node scripts/scan-market.mjs --concurrency 4 --query "pilot logbook" … 20 ad-hoc queries` | 37 queries in 6,425 ms: **2 candidate, 19 mixed, 5 fortress, 10 dead-zone, 0 no-results, 0 errored**. The two calibration controls behaved as their manual readings require — `habit tracker` = `fortress`, `bjj training log` = `dead-zone` — so the sensor has not drifted since 2026-09-26. |
| 2 | `node scripts/scan-titles.mjs --query "pilot logbook" --query "drone flight log" --query "coin collection catalog" --query "poker session tracker" --query "swim workout log" --query "homebrew brew log" --query "vinyl record collection"` | 7 queries, 7 ok. Full visible sets read by hand in §§4–6. |
| 3 | `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "pilot logbook" --query "drone flight log" --query "swim workout log" --query "poker session tracker"` | 32 app pages, 0 failures. |
| 4 | `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "coin collection catalog" --query "guitar practice log" --query "climbing training log"` | 24 app pages, 0 failures. |
| 5 | `node scripts/scan-complaints.mjs --concurrency 3 --package com.spiraledge.swimapp … --package com.active.aps.meetmobile` | 15 reviews, 14 critical, 2 unclassified. |
| 6 | `node scripts/scan-complaints.mjs --concurrency 3 --package pilotlog.crewlounge.aero --package com.dauntless.safelog --package com.digcy.pilot --package com.filavision.brt` | 12 reviews, 10 critical, 0 unclassified. |
| 7 | `node scripts/scan-complaints.mjs --concurrency 2 --query "coin collection catalog" --top 6 --query "guitar practice log" --top 6` | 36 reviews, 23 critical. Coin: 18 reviews, 13 critical, 4 unclassified. |
| 8 | `node scripts/scan-titles.mjs --concurrency 3 --query "coin collection tracker" --query "coin inventory app" --query "coin album catalog"` | 3 queries, 3 ok; the catalogue sub-shelf classified by hand in §5.1. |
| 9 | `node scripts/scan-titles.mjs --concurrency 3 --query "swim training log" --query "swim log" --query "swim pace tracker"` | 3 queries, 3 ok; the swim sub-shelf classified in §7. |

## 2. What the sensors cannot tell you

Four limits, observed rather than assumed:

- **A verdict is not a job.** `coin collection catalog` scores `on-topic` at 29/30 because the word
  `coin` is in every title, and roughly half of that shelf is a *different* job (photo identifiers).
  The hand classification in §5.1 is the evidence; the guard's verdict is only a screening hint.
- **A result count is display-capped** (~30 cards) and one storefront (`hl=en`, `gl=US`, signed out)
  — ON RECORD, `docs/niche-selection.md` §3.
- **An unrated competitor is a failure signal, not a vacancy** — ON RECORD, same file. It is what
  kills the drone-log recipe in §7.
- **No revenue is visible.** `enrich-market` prints a declared price, a declared `ads` flag, a
  declared `IAP` flag and an install *bucket*. Nothing here reads a P&L.

## 3. The screen, as measured

`node scripts/scan-market.mjs` over the default archetype set plus 20 ad-hoc queries, verdict / seen /
rated / weak / median:

| Query | Verdict | Seen | Rated | Weak | Median |
|---|---|---:|---|---:|---:|
| coin collection catalog | mixed | 30 | 23/30 | 8 | 4.30 |
| pilot logbook | mixed | 30 | 15/30 | 7 | 4.20 |
| poker session tracker | mixed | 29 | 20/29 | 5 | 4.30 |
| guitar practice log | mixed | 30 | 26/30 | 7 | 4.50 |
| climbing training log | mixed | 30 | 18/30 | 5 | 4.55 |
| vinyl record collection | mixed | 30 | 13/30 | 4 | 4.40 |
| homebrew brew log | mixed | 30 | 12/30 | 3 | 4.40 |
| homeschool record keeping | mixed | 30 | 14/30 | 6 | 4.35 |
| magic card collection | mixed | 11 | 9/11 | 1 | 4.40 |
| **swim workout log** | **candidate** | 30 | 11/30 | 6 | 3.80 |
| drone flight log | mixed | 30 | 15/30 | 6 | 4.20 |
| boat maintenance log | dead-zone | 29 | 8/29 | 3 | 4.45 |
| home inventory insurance | dead-zone | 27 | 9/27 | 5 | 4.10 |
| warranty receipt organizer | mixed | 10 | 5/10 | 1 | 4.30 |
| document expiry reminder | dead-zone | 30 | 4/30 | 2 | 3.60 |
| espresso dial in | dead-zone | 11 | 2/11 | 0 | 4.60 |
| sourdough baking log | dead-zone | 9 | 2/9 | 1 | 4.10 |
| tutor session log | dead-zone | 50 | 8/50 | 4 | 4.15 |
| music teacher lesson log | mixed | 14 | 12/14 | 2 | 4.45 |
| `habit tracker` (control) | fortress | 21 | 20/21 | 1 | 4.50 |
| `bjj training log` (control) | dead-zone | 30 | 8/30 | 3 | 4.40 |

The five default-set groups were also re-scanned; their rows are unchanged in direction from
2026-09-26 (`bolt torque calculator` has since fallen from `candidate` to `dead-zone`, which is the
same drift the guard showed then and is not load-bearing for anything here).

## 4. The two shelves that survived screening

### 4.1 `coin collection catalog` — MEASURED

`guard=on-topic (29/30, share 0.97)`, verdict `mixed`, 23 of 30 visible results rated, 8 weak, median
4.30. Enrichment (`--top 8`):

| App | Rating | Reviews (exact) | Installs | Ads | IAP | Price |
|---|---:|---:|---|---|---|---|
| `com.pcgs.coinfacts` PCGS CoinFacts | 4.6 | 21,835 | `500K+` | false | false | free |
| `com.spencerpages` Coin Collection | **4.3** | **2,758** | **`500K+`** | false | **false** | free |
| `com.coinidentifyer.ai` CoinSnap — Coin Identifier | 4.5 | 143,352 | `10M+` | false | true | free |
| `com.coin.identifier.coinidentifier` Coin Identifier | 3.9 | 9,270 | `1M+` | false | true | free |
| `com.hagalaz.coinscollection` NumisBook | unrated | not shown | `1K+` | false | true | free |
| `com.ai.coin.collection.manager.inventory` Coin Collection Manager | 4.1 | 733 | `10K+` | false | true | free |
| `com.coinvaluechecker.myapp` Coin Value Checker | 4.5 | 3,443 | `100K+` | true | true | free |
| `com.weipaitang.coin` HeriBase: AI Coin Identifier | 4.7 | 74,162 | `5M+` | false | true | free |

Two readings that matter:

- **The exact job has a proven audience and a beatable leader.** `com.spencerpages` Coin Collection is
  a plain catalogue with `500K+` installs and 2,758 reviews at **4.3** — demand is proven at the
  category's second rung, and the leader declares neither ads nor in-app purchases, so it has not
  monetised the job at all.
- **The money on this shelf is in identification, not cataloguing.** The three largest apps
  (CoinSnap `10M+`, HeriBase `5M+`, Coin Identifier `1M+`) are all photo identifiers with IAP, and all
  three are the ones users complain about being paywalled.

### 4.2 `pilot logbook` — MEASURED

`guard=on-topic (26/30, share 0.87)`, verdict `mixed`, 15 of 30 rated, 7 weak, median 4.20.
Enrichment (`--top 8`):

| App | Rating | Reviews (exact) | Installs | Ads | IAP | Price |
|---|---:|---:|---|---|---|---|
| `com.waderaviation.wader` Wader Pilot Logbook | 4.8 | 151 | `10K+` | false | true | free |
| `com.myflightbook.android` MyFlightbook | 4.6 | 313 | `50K+` | false | false | free |
| `com.flylogio` FLYLOG.io | 4.6 | 604 | `10K+` | false | true | free |
| `pilotlog.crewlounge.aero` PILOTLOG CrewLounge AERO | **3.9** | **647** | `10K+` | false | true | free |
| `com.airos.app` AIROS Pilot Logbook | 4.5 | 10 | `100+` | false | true | free |
| `com.logbookpro.android` Logbook Pro Flight Log | 4.6 | 496 | `50K+` | false | true | free |
| `com.logbook.wingman` Wingman Pilot Logbook | 4.8 | 528 | `10K+` | false | true | free |
| `biz.eventually.pilot.notebook` Pilot Notebook | unrated | not shown | `1K+` | false | false | free |

The shelf is genuinely competitive at the top (four apps between 4.6 and 4.8, all free) and genuinely
weak in the middle (`PILOTLOG` 3.9 over 647 reviews; `Safelog` 3.0, `CAE` 3.3, `FlightLog` 3.9,
`intelliPilot` 3.9 from the title list).

## 5. Recipe 1 — Coin collection catalogue (`coin-collection-catalogue`) — RECOMMENDED

**Category and job.** Numismatic collecting / keep a searchable record of the coins you own — what
each one is, what you paid, what it is worth and where it is stored — and hand it on or sell from it
without losing the record.

**Paying user.** A collector who already keeps the collection in a paper album, a spreadsheet or a
notebook, and wants that record on the phone in their pocket at a fair, a coin shop or a club meeting.

**What is bought.** A one-time unlock of the full catalogue: unlimited coins, several collections,
photos from the photo picker, search and filters, per-collection totals and CSV export — no
subscription and no ads.

**Archetype.** `purchase_led_single_unlock`, **$6.99**, capability `billing`. `Fulfillment` is local:
the unlock is a boolean Play reports, so nothing is served and there is no granted-before-paid unit —
which is why `F_fraud` is excluded from the arithmetic below (INFERRED from its definition in
`docs/monetization-strategy.md` §2.2).

### 5.1 The hand classification, which the guard cannot do

`coin collection` scores `on-topic` at 29/30 because the token `coin` is in every title. Read by hand
across four queries (`coin collection catalog`, `coin collection tracker`, `coin inventory app`,
`coin album catalog`, 30 cards each, with the second card always `com.spencerpages`), the shelf splits
by job:

| Job on the shelf | Share of the 30 | Examples |
|---|---:|---|
| Photo identifier / value scanner | **~15/30** | CoinSnap 4.5, HeriBase 4.7, Coin Identifier 3.9, Coinoscope 3.7, Coin Checker 3.7 |
| **Collection catalogue / inventory** | **~12/30** | **Coin Collection (`com.spencerpages`) 4.3**, Coin Collection Manager 4.1, NumisBook, My Coin Collection Tracker, Numisma, USA Coins 4.1, Quarter Coin Collection 4.4, Euro Coin Collection |
| Price reference / melt value / magazine | ~3/30 | PCGS CoinFacts 4.6, Coinflation 2.0, Coin Collector Magazine 4.9 |

So the job this recipe serves is about forty percent of the visible shelf, and inside that forty
percent there is exactly **one** app with real traction (`com.spencerpages`, `500K+`, 4.3).

### 5.2 What the incumbents' users say — MEASURED

`node scripts/scan-complaints.mjs --query "coin collection catalog" --top 6`: **18 reviews read, 13
critical**, 4 unclassified. Histogram over the critical set: `paywall` 4 (31 %), `ads` 2 (15 %),
`missing-feature` 2 (15 %), `regression` 2 (15 %), and one each of `accuracy`, `broken`,
`confusing-ui`, `notifications`, `value`.

Verbatim, on a 1M+ app rated 3.7 (`com.miccron.coinoscope`) — the editing and searching complaint this
recipe is built to answer:

> 1★ "Super hard to use. You can't search for a specific dated coin. To edit out a pic from my
> collection (if i sell or gift one) has proven elusive as I ga…"

And on the 50K+ `com.colnect.identify.coin` (2.6★), the catalogue-entry complaint:

> 1★ "You can log your coins in and stuff like that that's good but this app sucks because it doesn't
> have most of the coins and they're just ones out of my…"

And the anti-subscription vocabulary, which 31 % of this pocket's critical reviews speak
(`com.coin.identifier.coinidentifier`, 1M+, 3.9★):

> 1★ "This app would be very cool if it was actually accurate… They also made it as difficult as…"

### 5.3 The arithmetic

```
net multiplier = (1−0.15)(1−0.02)(1−0.02) = 0.85 × 0.98 × 0.98 = 0.8163
net per sale   = 6.99 × 0.8163 = $5.71
sales for $1,000/month = 1000 / 5.71 = 175.1 → 176 sales/month (≈ 5.9/day)
```

At the UNVERIFIED 2 % install-to-purchase rate:

| Installs / month | Sales / month | Revenue / month |
|---:|---:|---:|
| 300 | 6 | $34 |
| 1,000 | 20 | $114 |
| 3,000 | 60 | $343 |
| 8,800 | 176 | $1,005 |

For scale: the catalogue leader carries `500K+` **lifetime** installs, so `$1,000/month` is not a
near-term target and this recipe should not be sold to the owner as one. What it is: a $30–$350 a
month app at plausible volumes, whose first purpose is to make the factory measure a purchase again.

### 5.4 The reason list (the promise contract)

| # | Promise | Weight | Evidence |
|---|---|---|---|
| 1 | A coin can be found by year and denomination and removed in seconds, so a sold or gifted coin leaves the collection at once. | **core** | MEASURED — Coinoscope, 3.7★ over `1M+` installs, 3 of 3 sampled reviews critical, verbatim in §5.2 |
| 2 | The catalogue is a file the collector owns: no account, no subscription, no signal needed, and CSV in and out. | **core** | MEASURED — 4 of 13 critical reviews (31 %) in the pocket are `paywall`; the three largest coin apps all declare IAP |
| 3 | It records what a collector actually records — country, denomination, year, mint mark, grade, paid, valued, stored where — instead of being a scanner that cannot be corrected. | **core** | MEASURED — ~15 of 30 visible results are identifiers; Colnect is 2.6★ over `50K+` with an entry complaint quoted in §5.2 |
| 4 | Bought once: no ads, ever. | supporting | MEASURED — 2 of 13 critical reviews (15 %) are `ads`; the installed catalogue leader declares none |
| 5 | Photos come from the system photo picker, so the app requests no permission. | supporting | ON RECORD — `AGENTS.md`, Play compliance floor ("Photo Picker is preferred") |
| 6 | Several collections with their own totals, so bullion and a graded set are not one number. | supporting | INFERRED from the shelf's split between bullion and graded sets |

### 5.5 Build, kills and what would refuse it

- **Build.** Estimated **4–5 agent hours** to a verified slot. Screens: collection list with per
  collection totals; coin list with search and filters; coin editor (the field set above) with photo
  picker; want list; CSV export/import; settings (default currency, defaults, theme, language); one
  unlock sheet. Domain work: an importable record model, filter/sort, totals, CSV round-trip.
  Permissions: **none**. Capabilities: `billing`. Server: **none**.
- **The cheapest decisive test.** One question in `r/coins` and one coin-club meeting: how many of you
  keep the collection in an app today, and which one. If the answer is overwhelmingly "paper and a
  spreadsheet and I like it that way", the willingness to pay is not there.
- **Kills.** `windowDays` 60, `minInstalls` 400, `minRevenueUsd` 60.
- **What would refuse this recipe.** If the shipped catalogue cannot survive a 10,000-coin import
  test, the first promise is not keepable and the recipe should be narrowed before the slot is
  scaffolded.

### 5.6 Visual direction

Warm dark: an aged-brass and patina palette over near-black card stock, so the app reads as a
collector's tray rather than a spreadsheet. The mark is a struck coin seen edge-on with its reeded
mill and a mint-mark notch cut into the rim. The components that are the slot's own and not stock
Material: a coin disc that draws each coin as a struck, reeded circle tinted by its metal, a
collection header that carries the running count and value as a large tabular figure, and a grade
chip whose colour is the graded condition rather than a decorative accent. Nothing here is a default
`Card` on a default background.

### 5.7 The scaffold command this recipe records

```bash
node scripts/new-slot.mjs \
  --slug coin-collection-catalogue \
  --name "Coin Collection Catalogue" \
  --idea "A collector's own coin record: search, edit, photograph and export it, bought once." \
  --economics docs/ideation/plans/coin-collection-catalogue.economics.json \
  --differentiators docs/ideation/plans/coin-collection-catalogue.differentiators.json
```

## 6. Recipe 2 — Pilot logbook (`pilot-logbook`)

**Category and job.** Aviation record-keeping / keep the logbook a licence requires, and produce a
summary an examiner or insurer will accept.

**Paying user.** A private or student pilot on Android who must keep a logbook and currently types it
into a free app that has lost or crashed mid-entry.

**What is bought.** A one-time unlock of the full logbook: unlimited flights, every column and the
totals, a printable summary and CSV export, with no account and no sync.

**Archetype.** `purchase_led_single_unlock`, **$9.99**, capability `billing`, no server.

### 6.1 The hand classification — MEASURED

`pilot logbook` is the cleanest shelf measured today: **26 of 30** visible titles name a pilot
logbook. Of those, 15 are rated and **7 are below 4.2**: `PILOTLOG` 3.9, `FB` FlightLog 3.9,
`CAE RB Pilot Logbook` 3.3, `Garmin Pilot` 2.8, `Safelog` 3.0, `intelliPilot` 3.9, and one more 3.9.

### 6.2 What the incumbents' users say — MEASURED

`node scripts/scan-complaints.mjs` over four packages: **12 reviews, 10 critical, 0 unclassified** —
`broken` 40 %, `crash` 40 %, `data-loss` 30 %, `missing-feature` 30 %. Verbatim:

> 1★ ("Safelog Pilot Logbook", 3.0★, `10K+`) "The Android version of this app is unstable, it
> constantly crashes, 100% of the time while I'm in the middle of entering my flight times…"

> 2★ ("Garmin Pilot", 2.8★, `500K+`) "Doesn't save anything anymore. I've used this app on Android for
> the past year as a student pilot, and things were fine for the first few months. now,…"

> 1★ ("PILOTLOG CrewLounge AERO", 3.9★, `10K+`, 647 reviews) "I have major doubts about the competence
> of there tech support. I spent days editing and then syncing the entries in my logbook via the app
> function o…"

The complaint profile is the sharpest of any pocket measured today, and it is exactly the class of
defect the factory's published reference slot solved: the record must not be lost.

### 6.3 The arithmetic

```
net multiplier = (1−0.15)(1−0.02)(1−0.02) = 0.8163
net per sale   = 9.99 × 0.8163 = $8.15
sales for $1,000/month = 1000 / 8.15 = 122.7 → 123 sales/month (≈ 4.1/day)
```

| Installs / month | Sales / month | Revenue / month |
|---:|---:|---:|
| 300 | 6 | $49 |
| 1,000 | 20 | $163 |
| 3,000 | 60 | $489 |
| 6,150 | 123 | $1,003 |

### 6.4 The reason list

| # | Promise | Weight | Evidence |
|---|---|---|---|
| 1 | No entry is ever lost: the record is written to the device as it is typed, and the app does not crash mid-entry or quietly stop saving. | **core** | MEASURED — Safelog 3.0★ `10K+`: "it constantly crashes, 100% of the time while I'm in the middle of entering my flight times"; Garmin Pilot 2.8★ `500K+`: "Doesn't save anything anymore" |
| 2 | The logbook is the pilot's own file: no account, no sync that can drop entries, and a backup that restores. | **core** | MEASURED — CrewLounge 3.9★ over 647 reviews: 2 of 3 sampled reviews are `data-loss` |
| 3 | Every column and total a licence needs is editable, and a summary prints for an examiner. | **core** | MEASURED — `missing-feature` is 3 of 10 critical reviews (30 %) and `confusing-ui` 2 of 10 |
| 4 | Bought once: no subscription. | supporting | ON RECORD — `docs/monetization-strategy.md` §3.1; the shelf's leaders are free-with-IAP |
| 5 | Works in a cockpit with no signal. | supporting | INFERRED — the shelf's own weak apps break on sync |

### 6.5 Build, kills

- **Build.** Estimated **5–7 agent hours**, the highest of the two. Screens: flight list with running
  totals; flight editor (date, type, registration, from/to, total, PIC, dual, night, IFR, approaches,
  landings, remarks); totals and a printable summary; CSV export; settings; one unlock sheet. The
  extra cost over the coin catalogue is the domain: the column set and the totals have to be right,
  and a wrong total in a licence record is a correctness defect rather than a cosmetic one.
- **The cheapest decisive test.** Ask the Android pilot communities which free Android logbook they
  actually use and what it cost them; if the answer is "MyFlightbook and it is free and fine", the
  paid wedge is a design argument and not a defect argument.
- **Kills.** `windowDays` 60, `minInstalls` 250, `minRevenueUsd` 60.

### 6.6 Visual direction

Night cockpit: a slate-blue instrument palette with the one amber accent reserved for the entry
currently being written, so an unsaved flight is visible at a glance. The mark is a logbook page
whose bottom rule is a tiny airspeed arc. The slot's own components: a flight row rendered as an
instrument strip with the running total in a tabular figure at its end, a totals block that reads
like a gauge cluster rather than a data table, and a persistence indicator that is the only amber
element on the screen. It shares no mark, palette or component with Recipe 1.

### 6.7 The scaffold command this recipe records

```bash
node scripts/new-slot.mjs \
  --slug pilot-logbook \
  --name "Pilot Logbook" \
  --idea "A pilot's own logbook that cannot lose an entry, bought once, offline." \
  --economics docs/ideation/plans/pilot-logbook.economics.json \
  --differentiators docs/ideation/plans/pilot-logbook.differentiators.json
```

## 7. Refused, with the reason

| Candidate | Refused by |
|---|---|
| `drone flight log` | **Unrated graveyard.** Every on-job app in the shelf is unrated and tiny — Alatna `1K+`, pando `50+`, ClearStack "Part 107 Drone Flight Log" `5+`, Pidotis `500+` — while the enriched "top" of the query is pilot logbooks and weather apps. Per `docs/niche-selection.md` that is people tried and nobody got traction, not vacancy. |
| `swim workout log` | **A `candidate` verdict over a graveyard.** The query is a candidate (11/30 rated), but its siblings `swim training log` (10/30) and `swim pace tracker` are `dead-zone`, and the shelf's money is in structured workout plans and Garmin sync — the two things a cheap offline app would not build. The disliked leaders (Swim.com 3.5★ `100K+`, MySwimPro 3.8★ `100K+`) are disliked *for the subscription*, not for the log. |
| `guitar practice log` | The shelf is lesson platforms (Yousician `50M+`, Simply Guitar `10M+`, Ultimate Guitar `10M+`); a practice journal is a thin slice beside them, and the one practice-journal app measured has `1K+` installs. |
| `poker session tracker` | Demand is proven (Poker Bankroll Tracker `100K+`, 795 reviews, 4.5★) but the leader is well rated, so the weak pocket is thin, and the shelf mixes bankroll trackers with GTO trainers and casino games, which is a policy posture the factory should not take to buy a weak wedge. |
| `climbing training log` | `partial` relevance (15/30), and the top of the shelf is strong (`Crimpd` `100K+`, 609 reviews, 4.6★). |
| `vinyl record collection`, `homebrew brew log` | Both `mixed` and both polluted — vinyl by record scanners (`Discogs` 1.7★), homebrew by coffee-shop loyalty apps. Neither offers a clean job. |
| `boat maintenance log`, `home inventory insurance`, `document expiry reminder`, `espresso dial in`, `sourdough baking log`, `tutor session log` | All `dead-zone` by the measured rated share (see §3). |
| `homeschool record keeping` | Measured `mixed` (14/30 rated, 6 weak), but it is a record-of-students job one step from the **live** `teacher-gradebook` slot. Google Play's Repetitive Content policy reaches the whole account (`docs/monetization-strategy.md` §1.2), and two student-record apps is the exact adjacency the factory's own rule warns against. Recorded so a later session does not re-derive it. |
| Everything in `docs/research/2026-09-24-market-decision.md` §3 and the dead zones in `docs/niche-selection.md` | Not re-proposed, and nothing above re-proposes one. No calculator, converter or generic arithmetic tool is proposed. |
| The bench survivors of 2026-09-26 (`amateur-radio-logbook`, `scuba-dive-log`, `smart-packing-checklist`, rental inspection) | Not re-measured in this run. They are not refused; they are simply not part of today's measurement, and none is recommended over the two recipes above. |

## 8. Comparison and recommendation

| # | Recipe | Category | Archetype | Price | Build h | Demand evidence | Server | First expensive failure it risks | Score |
|---:|---|---|---|---:|---:|---|---|---|---:|
| 1 | **Coin collection catalogue** | Numismatic collecting | `purchase_led_single_unlock` | $6.99 | 4–5 | **MEASURED** — 23/30 rated, `500K+` catalogue leader at 4.3 over 2,758 reviews, 13 critical reviews read, a named search/edit complaint | no | The category's money is in identification and this app does not identify | **7/10** |
| 2 | Pilot logbook | Aviation records | `purchase_led_single_unlock` | $9.99 | 5–7 | **MEASURED** — 26/30 titles on-job, 7 weak of 15 rated, 12 reviews read with `crash` and `data-loss` at the top | no | Four free apps at 4.6–4.8 already do the job well | **6/10** |

**I recommend Recipe 1, the coin collection catalogue**, for three measured reasons and one
structural one.

1. **Its leader is beatable and unmonetised.** `com.spencerpages` holds `500K+` installs and 2,758
   reviews at **4.3** and declares neither ads nor IAP — the job has a proven audience and no one has
   charged for it. The pilot shelf's leaders are four free apps at 4.6–4.8, which is a harder fight
   for the same build budget.
2. **Its audience is one the factory's own heuristic favours.** `docs/niche-selection.md` §4: the
   opening is where "the person doing the work would never build the app". Coin collectors are
   hobbyists; the pilot shelf is populated by pilot-developers and flight-school software.
3. **It is the cheaper of the two and the one whose domain cannot be got wrong.** A catalogue has no
   licence arithmetic and no legal total; the whole risk is search, editing and export, which is
   mostly testable code.
4. **Structurally**, it is a different category and a different job from the live `teacher-gradebook`,
   which is the constraint that actually bites on a catalogue of apps.

**The honest counter-argument, stated rather than buried:** the coin shelf's paying behaviour is
concentrated in *identification*, not cataloguing, and the catalogue leader is free. A collector who
already uses a free app that works has no reason to pay $6.99, and this is the single most likely way
this recipe fails. Recipe 2 has the opposite risk profile — a must-have record with a proven
willingness to pay, against strong free competition and a more expensive build.

## 9. Honest unknowns, listed rather than omitted

1. **Install-to-purchase conversion is assumed at 2 %** for both recipes. It is not measured anywhere
   in this repository, and no slot has ever reported a sale to it. Every revenue row in this document
   inherits that assumption.
2. `F_billing = 0.15`, `F_refund = 0.02`, `F_fx = 0.02` are ON RECORD assumptions in
   `docs/monetization-strategy.md` §2.2, not measurements of this account, and the fee rises above the
   first $1M tier or for existing EEA/UK/US installs.
3. **Whether the coin audience will pay for cataloguing at all.** The catalogue leader is free with
   no IAP declared; the paying apps in the category sell identification. This is the recipe's central
   risk and no command in this session can settle it.
4. **The three reviews per app are Play's sample, not a rate.** `paywall` at 4 of 13 critical reviews
   is the vocabulary of the pocket, not its frequency distribution.
5. **Install buckets are bounds, not points.** `500K+` spans 500,000 to 999,999.
6. **Search visibility is unknown.** `coin collection` is dominated by identifier apps, so a
   catalogue would need to rank against a `10M+` app for the query as well as beat the second result.
7. **The coin catalogue's own hand classification is mine**, from the titles, and it is recorded in
   §5.1 so it can be audited rather than trusted.
8. **Whether this account can publish to production immediately** is still not read from Play Console;
   `AGENTS.md` records that `com.gexetr` already has published apps, which is an inference.
9. **No revenue reading exists for the live slot.** `node scripts/kiln-status.mjs` reports
   `teacher-gradebook: NO-DATA`, so the factory's first purchase is still unmeasured. This document
   does not fill that gap and does not pretend to.

# 2026-10-02 — Authored learning and practice: `part-107-drills`

A recon proposal for the **authored learning and practice** lane. One recipe, in full, with the
refusals that led to it. Everything below is labelled **MEASURED** (a command run or a file read in
this session), **ON RECORD** (a named source states it and was read), **INFERRED**, or
**UNVERIFIED**.

**Read boundary observed.** Nothing under `apps/**` was opened. The only views of the factory's own
work were the `id` / `name` / `idea` fields of `registry/apps.json` and the killed lists in
`docs/research/2026-09-24-market-decision.md` and `docs/niche-selection.md`.

**Lane rule observed.** The owner's brief forbids a product whose primary value is that it stores
what the user typed and shows it back as a list. This app is not a log, ledger, register, journal,
checklist or CRUD table: the user is not the author of the content, they answer it. It is also not a
calculator, a unit converter, or arithmetic over user-typed numbers — the closed topic.

---

## 1. Category and job, in one sentence

`Part 107 Drills` is a phone app that drills a US drone pilot for the FAA Part 107 remote-pilot
aeronautical knowledge test by **generating** the weather and airspace practice items from the
regulation and the aviation-weather grammar rather than serving a fixed question bank, and by naming
the paragraph or ACS code behind every answer on the answer screen itself.

## 2. The paying user, what is bought, the price, the moment of payment

| | |
|---|---|
| **Paying user** | A US-based aspiring commercial drone operator who must pass the FAA Part 107 knowledge test to fly for money — either someone buying a first drone for paid work, or a hobbyist turning commercial. They study in the two-to-six weeks before a $175 test appointment at a PSI testing centre. |
| **What is bought** | One permanent unlock of the whole app: the full ACS-mapped bank, both generated drill engines, every answer's citation, in airplane mode, with no account. |
| **Price** | **$9.99**, one time. |
| **Moment of payment** | After a free set of drills per topic has proved the app explains, at the first locked topic or the first full-length mock, behind one clearly priced "unlock everything" sheet with no trial clock and no subscription prompt. |

## 3. Demand evidence (MEASURED)

### 3.1 The sensor, and its calibration

`node scripts/scan-market.mjs --concurrency 4 --query "part 107 practice test" --query "drone pilot
test prep" --query "faa part 107 exam prep" --query "habit tracker" --query "bjj training log"`
— 2026-10-02. Calibration controls, in the **same invocation** as the lane queries:

| control | expected | observed |
|---|---|---|
| `habit tracker` | `fortress` | **`fortress`** (29/30 rated, 18/29 ≥ 4.5) |
| `bjj training log` | `dead-zone` | **`dead-zone`** (9/29 rated, 0.31 < 0.35) |

Both controls held, in this invocation and in a second invocation later in the run. The lane
verdicts below are therefore readings, not sensor noise.

### 3.2 The shelf

| query | verdict | relevance guard | seen | rated | weak | median |
|---|---|---|---|---|---|---|
| `part 107 practice test` | **candidate** | ok | 11 | 4/11 | 2 | 4.40 |
| `drone pilot test prep` | **candidate** | ok | 30 | 11/30 | 6 | 4.00 |
| `faa part 107 exam prep` | **candidate** | ok | 14 | 6/14 | 4 | 4.05 |

Three independent phrasings return `candidate` — demand proven, supply inadequate. A `candidate`
verdict is a **spot check, not evidence** (the guard is blind to homonym collisions), so it was
hand-classified: `node scripts/scan-titles.mjs --concurrency 3 --query "part 107 practice test"
--on-job "part\s*107|remote pilot|\bsUAS\b|\bUAS\b|drone pilot|FAA" --off-job "flight
sim|simulator|game|radar|flight tracker|weather map|logbook"` returned **hand job-share 11/11
(1.00)** against the guard's `on-topic` — the visible page is entirely Part 107 exam-prep apps, and
the guard did not have to be corrected.

### 3.3 The incumbents, measured

`node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "part 107 practice test"` — 2026-10-02:

| # | package | title | developer | rating | reviews (exact) | installs | ads | IAP | price |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `com.edupass.part107` | Part 107 Practice Test 2026 | Easy Pass E-Learning | 4.9 | 107 | 1K+ | false | false | free |
| 2 | `com.droneprep.app` | FAA Part 107 Pro Test Prep | Scott Labs | 4.8 | 18 | 100+ | false | true | free |
| 3 | `com.alphabeat.ffaexams` | FAA Part 107 Drone Test Prep | Alpha Beat Limited | unrated | unknown | 100+ | false | true | free |
| 4 | `com.part107.faa.exam` | Part 107 - Practice for exam | Go Dreams | **4.0** | **440** | **10K+** | false | true | free |
| 5 | `night_coding.android.part107` | Part 107 FAA — practice test | night_coding | **3.5** | **34** | 1K+ | false | false | free |
| 6 | `com.rltw.part107` | Part 107 Ready: FAA Drone Prep | RLTW | unrated | unknown | 10+ | false | true | free |
| 7 | `com.aleosapps.QuizzAppFAA107` | FAA PART 107 Exam Prep Prepia | Prepia Inc | unrated | unknown | 1K+ | false | true | free |
| 8 | `com.calcwise.faapart107` | FAA Part 107 Drone Exam 2026 | Dailywave Apps | unrated | unknown | 5+ | **true** | true | free |

Enrich summary: ads declared 1 / absent 7; IAP declared 6 / absent 2; largest install bucket 10K+.
Three further visible cards (`xyz.plus44.part107ready`, `com.logicpathsystems.faa107`,
`app.mcyj.examprep.glb0002`) are unrated.

**The demand is proven and the money is rent.** An **occupied shelf is demand evidence**, and the
10K+ install bucket with 440 exact reviews is the proof someone earns here. **8 of the 11** visible
incumbents are unrated — per the factory's own discipline an unrated incumbent is evidence of
*failure*, not vacancy: the rated ones (10K+, 1K+, 1K+) are where the installs are, and the two
best-rated are free with no in-app purchase at all.

**Contradiction recorded rather than smoothed over.** The enricher reads
`com.part107.faa.exam` as declaring `ads: false`, while its own users describe constant ads
(§3.4). Either the listing under-declares the ads or the enrichment misread the flag; both readings
are MEASURED and they disagree. The interpretation — that the listing is the thing that is wrong —
is **INFERRED**, not established.

### 3.4 What the weakest incumbents' users actually wrote (MEASURED)

`node scripts/scan-complaints.mjs --query "part 107 practice test" --top 6 --concurrency 2` —
2026-10-02. Pocket: 11 reviews read, 6 critical, 0 without text.

| class | share of the 6 critical reviews |
|---|---|
| ads | 2 (33%) |
| paywall / price / subscription | 2 (33%) |
| accuracy (wrong or outdated information) | 1 (17%) |
| broken feature or bug | 1 (17%) |
| missing feature | 1 (17%) |

Verbatim, with the app and the class the sensor assigned:

- `com.part107.faa.exam` (4.0★, 440 reviews, 10K+) — **ads**, 2★: *"The amount of ads in this app
  are rough and disruptive BUT if you pay for the week/month subscription they (mostly disappear)
  and it's a very good res[ource]"*.
- `com.part107.faa.exam` — **ads**, 2★: *"This app could be so amazing if there weren't literally
  constant pornographic ads."*
- `com.part107.faa.exam` — **missing feature**, 3★: *"Its decent for a general study. However there
  are topics that are specifically fixed wing topics only."*
- `night_coding.android.part107` (3.5★, 34 reviews, 1K+) — **accuracy**, 1★: *"DO NOT USE THIS
  APP!!. I just uninstalled. There are too many questions that are WRONG! You can't pass a test if
  the study guide is giving you inaccur[ate]"*.
- `night_coding.android.part107` — **broken feature**, 2★: *"great app if you could just fix the
  swipe right bug that exits whatever question set you are in with no way of getting back into the
  test/question set"*.
- `night_coding.android.part107` — **ads**, 3★: *"The fact that it breakes forcomercial at 13
  questions is to often."*

`com.droneprep.app` (4.8★, 100+) and `com.edupass.part107` (4.9★, 1K+) had **no critical reviews**
in the sample.

### 3.5 What the 2026 entrants' own listings claim (MEASURED — read in this session)

Read from the live Play listings via a web search on 2026-10-02:

- `com.appark.part107.part107` — "Drone Part 107 Prep 2026": *"over 1,000 fresh practice items"*,
  ACS-tagged, "Study by Code", "detailed explanations", offline mode.
- `com.dauntless.gs.faa.rdp` — "Drone Pilot (UAS) Test Prep": *"500+"* questions, "detailed and
  illustrated explanations", offline.
- `com.sima.part` — "Part 107 Practice Test 2026": *"more than 350 questions"*, "Detailed
  Explanations", offline access.
- `com.droneprep.app` — "FAA Part 107 Pro Test Prep": *"1000+ practice questions"*.

**This is the finding that shapes the wedge, and it is a refusal of my own first idea.** A
"question bank with full explanations, offline, with no ads" is **no longer available white space**:
the 2026 entrants already claim explanations, ACS tagging, offline mode and 1,000+ items. Proposing
it would be the Spam policy's *mere reproduction* — a new palette on an occupied job. The wedge has
to be somewhere else, and §4 says where.

### 3.6 The rest of the lane, measured and refused

`node scripts/scan-market.mjs` over 15 more of the brief's candidate queries plus both controls,
2026-10-02 (all `relev ok` unless noted):

| query | verdict | why it is refused (revenue reason) |
|---|---|---|
| `ear training intervals` | fortress | 13/18 rated ≥ 4.5, 1 weak — no complaint pocket to build a wedge on |
| `boating license practice test` | fortress | 15/21 ≥ 4.5, 3 weak |
| `food handler practice test` | fortress | 13/19 ≥ 4.5, 3 weak |
| `security guard exam prep` | fortress | 9/15 ≥ 4.5, 4 weak |
| `epa 608 practice test` | fortress | 8/11 ≥ 4.5, 1 weak — median 4.80 |
| `citizenship test practice` | fortress | 18/19 ≥ 4.5, **0 weak** |
| `kanji drill` | fortress | 11/18 ≥ 4.5, 4 weak; official Japan Foundation apps on the page |
| `spanish conjugation drill` | fortress (relev `partial`) | 6/8 ≥ 4.5, 1 weak |
| `cdl hazmat practice` | fortress | 15/23 ≥ 4.5, 6 weak |
| `ham radio license exam` | fortress (relev `partial`) | 7/11 ≥ 4.5, 1 weak |
| `morse code trainer` | mixed | 0.5 of rated are weak — inadequate supply, but no measured complaint pocket |
| `electrician journeyman exam` | mixed | 0.36 of rated are weak; adjacent to the burned calculator category |
| `phlebotomy practice test` | mixed | 10 seen only, 2 weak — too thin a reading to price a build against |
| `forklift certification practice` | mixed | 0 weak incumbents in 10 seen — no wedge at all |
| `notary exam prep` | dead-zone | 6/29 rated (0.21) — demand not proven |

`fortress` is a refusal for a revenue reason, not a novelty reason: the shelf's incumbents are
already ≥ 4.5 across the board with one-to-four weak apps, so there is no recorded user complaint to
answer, which means no stated reason to install, which means no sales. `dead-zone` is a refusal
because demand is not proven. `mixed` is a refusal because a wedge cannot be stated in one sentence
from the readings taken.

**One query in the lane came back `candidate` on three separate phrasings** — this one.

## 4. The wedge, in one sentence (functionality and content)

> **Where the shelf tests you against a fixed bank you can memorise — 350 to 1,000+ items by its own
> listings — this drills the two things the exam actually makes you *read* and *apply*: every
> weather item is generated from the METAR/TAF grammar and every airspace item is generated from the
> 14 CFR §107 limits, each validated against those rules before it is shown, and every answer names
> the paragraph or ACS code it came from on the answer screen — sUAS-only, with no ads, no
> subscription and no account.**

Every clause answers a complaint a named user wrote:

| clause | the complaint it answers |
|---|---|
| "generated from the grammar / the limits", not a fixed bank | *"too many questions that are WRONG"* (`night_coding`, accuracy, 1★) and the fixed 350–1,000+ item banks measured in §3.5 |
| "validated against those rules before it is shown" | the same accuracy complaint — the app must not be able to show an impossible weather report |
| "every answer names the paragraph or ACS code **on the answer screen**" | the same accuracy complaint: the learner can check the app against the regulation instead of trusting it |
| "sUAS-only" | *"there are topics that are specifically fixed wing topics only"* (`com.part107.faa.exam`, 3★) |
| "no ads" | *"literally constant pornographic ads"*; *"breaks for commercial at 13 questions"* (ads = 33% of the pocket's critical reviews) |
| "no subscription" | *"if you pay for the week/month subscription"* (paywall = 33%) |
| "no account" | the login class is the shelf's neighbour complaint on the factory's other shelves; on **this** shelf it is **INFERRED** from the job, not measured — no login complaint appeared in the 6 critical reviews read |

## 5. The archetype, and the three that were rejected with a revenue reason

**Chosen: `purchase_led_single_unlock`.** It requires the **`billing`** capability.

| archetype | verdict | revenue reason |
|---|---|---|
| `purchase_led_single_unlock` | **CHOSEN** | The shelf's paying behaviour is rent: the 10K+ incumbent converts ads into a week/month subscription. One price for the whole bank is the honest alternative to rent, and it is collectable by `billing`. |
| `purchase_led_repeat_sales` | rejected | There is no second thing to sell. The Part 107 certificate is renewed on a 24-month cycle (a recurrent online course, not a purchase), so the repeat cadence is one sale every two years; a consumable priced below the $9.99 whole-bank unlock cannibalises the unlock instead of adding revenue. |
| `ads_led_utility` | rejected | The ads **are** the measured defect this recipe answers (2 of the 6 critical reviews, including *"constant pornographic ads"*), so an ad-led build reproduces the complaint; and ads need install volume a brand-new listing does not have — it would carry the defect and earn nothing for months. |
| `purchase_only_no_ads` | rejected | **Every** enriched incumbent prices at 0 (8/8), so the shelf's install funnel is a free download with money collected in-app (6/8 declare IAP). A listing price trades a proven funnel for an unproven one. |
| `subscription` | **banned** | Banned outright by the owner (2026-10-01) and machine-enforced — `subscription` is not in `ARCHETYPES`. Named here only because the 10K+ incumbent's money is exactly that, which is what this recipe replaces. |

## 6. Unit arithmetic, with the division shown

```
price                                          9.99 USD
Play service fee, small-business tier 15%       −1.50
refund allowance 2%                             −0.20
currency-conversion allowance 2%                −0.20
────────────────────────────────────────────────────────
net per sale   = 9.99 × (1 − 0.15 − 0.02 − 0.02)
               = 9.99 × 0.81
               = 8.09 USD

sales to clear the 90-day floor of 300 USD
               = 300 ÷ 8.09 = 37.1  → 38 sales
sales per month
               = 38 ÷ 3 = 12.7      → 13 sales/month
installs per month, at an assumed 2% install-to-unlock
               = 13 ÷ 0.02 = 650 installs/month
90-day check
               = 2,000 installs × 0.02 = 40 sales × 8.09 = 323.60 USD   ✓ clears 300
```

The 15% / 2% / 2% deduction model and the 2% conversion rate are **assumptions carried from
`docs/monetization-strategy.md` §2.2**, not readings of this developer account; the fee also rises
above the first-million-dollar tier. Nothing here has been measured on this shelf.

## 7. The promise contract

Six promises, three of them `core`, each `deferred` until the build makes it true. Full record:
`plans/part-107-drills.differentiators.json`.

| id | weight | the promise |
|---|---|---|
| `sUAS-only-content` | **core** | No item covers manned-aircraft material: every question and every generated drill is scoped to the Part 107 sUAS knowledge elements, so study time is never spent on fixed-wing or private-pilot content the exam does not test. |
| `every-answer-cited` | **core** | Every item shows why its answer is right and names the authority behind it — the 14 CFR section or the FAA ACS knowledge code — on the answer screen itself, never behind the unlock. |
| `generated-drills-validated` | **core** | Weather and airspace items are computed from the authored METAR/TAF grammar and the §107 limits rather than drawn from a fixed bank, and every generated item is validated against those rules before it is shown, so the app cannot present an impossible weather report and the drill set cannot go stale. |
| `no-ads-no-renewal` | supporting | The quiz surface carries no advertising and the app never asks for a renewal: one purchase, then the whole bank in airplane mode. |
| `resumable-session` | supporting | A drill session is a saved run — leaving the screen returns to the same item with the same answer state, never to the start of the set. |
| `offline-by-construction` | supporting | The bank, both drill engines and every explanation are in the APK: a full session, an answer check and a citation work with no network and no account. |

## 8. The name

Full record: `plans/part-107-drills.naming.json`. **Chosen: `Part 107 Drills`** — archetype
`descriptive`, scored **87/100**, 15 characters, 15 characters of slack against Play's 30-character
cap in both declared locales, slug `part-107-drills`, `applicationId` `com.gexetr.part107drills`.

The `descriptive` archetype was chosen over the contract's default `carrier_job_noun` for a measured
reason, not a preference: **this shelf's carrier space is occupied, and eleven carrier auditions in
this session were eliminated by a collision** — `SkyDrill` (a registered mark and a live game),
`Hangar`, `Azimuth`, `Cadet`, `DroneWise` (live Play app), `AeroDrill` (Cruing's cutting-tool line),
`PropPrep` (Propspeed's product), `GroundSchool` (Dauntless Aviation's FAA test-prep line itself),
`Flywright` (a Bahamas drone school selling UAV exam prep), `SkyScholar` (aviation education),
`Airwise` (live Play app `AIRWISE-Safar`), `Skylore` (live Play MMORPG). The shelf's own titles are
literal-phrase titles (8 of 11 visible), so a descriptive title spends the strongest ranking field on
the tokens a learner actually types — `Part 107` — while naming the *mechanism* (drills) that no
incumbent's title uses. The trade is recorded honestly: weak ownability, in
`assumptionsUnverified`.

## 9. Build estimate, permissions, capabilities, server

| | |
|---|---|
| **Build estimate** | **~6 agent hours.** No server, no accounts, no imagery to license, no camera, no ML. The cost is authored content: the METAR/TAF grammar and its explanations, the §107 limit model, and an ACS-mapped item set. |
| **Server required?** | **No.** Every drill is generated from rules that ship in the APK. The only network use in the app's life is the Play Billing purchase / restore flow. |
| **Permissions** | None requested by the app beyond what the billing flow needs (`com.android.vending.BILLING` from the Play Billing library). No location, camera, storage, contacts, microphone, or background work. No `INTERNET` use for content — the bank is in the APK. |
| **Capabilities** | `billing` (required by `purchase_led_single_unlock`); `ads` deliberately **not** enabled — see §5. |
| **Distinctness inside our own catalogue** | The nearest neighbours are the *proposed* `pilot-logbook` and `amateur-radio-logbook`, which are logs the user types into; this app authors the content and the user answers it, so one sentence cannot describe both. No existing or proposed slot shares the `Part 107` head term or the drills job. |

## 10. Visual direction

**Mark:** a sectional-chart compass rose whose north needle is a quadcopter seen from directly
above — drawn from `store/studio.json`'s `mark`, read at 48 dp, legible inside the 66 dp safe zone,
and not a borrowed library glyph.

**Palette subject:** the aviation chart itself — a deep chart-navy ground, a paper-warm off-white for
item text, chart-magenta and chart-cyan as the two token classes (airspace vs weather), and one
high-visibility amber reserved exclusively for *the answer you must read* and for a failed limit.
Two things a stock Material screen does not do here: the item surface is a **chart tile with a token
strip along its edge**, and answering **reveals the citation inline** on the item rather than in a
bottom sheet; the slot's own components are a **token-strip decoder** (a raw METAR or TAF rendered as
tappable segments that each expand into their meaning and their source) and a **limit rail** (a
scenario's four §107 limits shown as pass/fail rails rather than a paragraph).

## 11. Kill criteria

Three finite numbers, judged after the window closes by `scripts/check-outcomes.mjs`:

| | |
|---|---|
| `windowDays` | **90** |
| `minInstalls` | **2,000** |
| `minRevenueUsd` | **300** |

At the net of §6 the installs criterion is the binding one: 2,000 installs at the assumed 2%
conversion is 40 sales and $323.60, so an installs miss implies a revenue miss and the pair is not
redundant on its own.

## 12. Honest unknowns

1. **Willingness to pay $9.99 is UNVERIFIED.** The only measured paying behaviour on this shelf is a
   week/month subscription on the 10K+ incumbent, and the two best-rated apps are free with no IAP.
   No reading shows a learner buying an exam-prep app outright.
2. **Install-to-unlock conversion is assumed at 2%.** No slot in this repository has ever measured a
   conversion rate, so every revenue figure derived from it is an assumption.
3. **The 15% / 2% / 2% deduction model is an assumption** carried from `docs/monetization-strategy.md`
   §2.2, not a reading of this account, and the fee tier rises above $1M.
4. **The freshness wedge is a hypothesis about learning, not a measured outcome.** The shelf's
   complaints are about ads, price and accuracy — *not* about memorisation. That generated drills
   teach better than a 1,000-item fixed bank is INFERRED from the exam's design, and no reading here
   shows a score improvement.
5. **The 2026 entrants already claim explanations, ACS tagging and offline mode** (§3.5), so the
   wedge rests entirely on *generated-and-validated* drills plus the on-answer citation. Whether a
   learner prefers that to a 1,000-item fixed bank is not measured.
6. **A Play search could not be run from this machine**, so the shelf readings are `enrich-market`'s
   card readings and web readings of indexed Play pages — not a Play Console reading.
7. **Search ranking is assumed.** The shelf is led by a 10K+ install app with 440 reviews; nothing
   here shows a new listing reaching 650 installs/month against it.
8. **The palette's distinctness from sibling slots could not be checked** under this recon's read
   boundary (`apps/**` is closed), so a palette collision is checked at build time by the
   visual-identity work, not here.

## 13. Claim ledger

| claim | label | evidence |
|---|---|---|
| `habit tracker` calibrates to `fortress` | MEASURED | `node scripts/scan-market.mjs … --query "habit tracker" …` → verdict `fortress` (29/30 rated, 18/29 ≥ 4.5) |
| `bjj training log` calibrates to `dead-zone` | MEASURED | same invocation → verdict `dead-zone` (9/29 rated, 0.31 < 0.35) |
| `part 107 practice test` is a `candidate` shelf | MEASURED | same invocation → `candidate`, seen 11, rated 4/11, weak 2, median 4.40 |
| `drone pilot test prep` and `faa part 107 exam prep` are `candidate` | MEASURED | same invocation → `candidate`, 11/30 rated (median 4.00) and 6/14 rated (median 4.05) |
| the visible page is 11/11 Part 107 exam prep | MEASURED | `node scripts/scan-titles.mjs …` → `hand job-share: 11/11 (1.00)` |
| `com.part107.faa.exam` is 4.0★, 440 reviews, 10K+ | MEASURED | `node scripts/enrich-market.mjs --top 8 …` → row 4 |
| `night_coding.android.part107` is 3.5★, 34 reviews, 1K+ | MEASURED | same command → row 5 |
| `com.edupass.part107` is 4.9★, 107 reviews, 1K+ | MEASURED | same command → row 1 |
| the shelf's ads/paywall share is 33% each | MEASURED | `node scripts/scan-complaints.mjs --query "part 107 practice test" --top 6` → 2 ads + 2 paywall of 6 critical |
| a named user wrote "literally constant pornographic ads" | MEASURED | same command, verbatim review text on `com.part107.faa.exam` |
| a named user wrote "too many questions that are WRONG" | MEASURED | same command, verbatim 1★ on `night_coding.android.part107` |
| a named user wrote "topics that are specifically fixed wing topics only" | MEASURED | same command, verbatim 3★ on `com.part107.faa.exam` |
| a named user wrote the swipe-right bug exits the question set | MEASURED | same command, verbatim 2★ on `night_coding.android.part107` |
| 8 of 11 visible incumbents are unrated | MEASURED | `scan-titles.mjs` output, `unrated` rows |
| the 2026 entrants claim 350 / 500 / 1,000 / 1,000+ item banks with explanations and offline mode | MEASURED | live Play listing text read via web search, 2026-10-02, quoted in §3.5 |
| `com.part107.faa.exam` declares `ads: false` while its users describe ads | MEASURED (contradiction) | `enrich-market.mjs` row 4 `ads false` vs `scan-complaints.mjs` review text |
| the listing under-declares its ads | INFERRED | the two readings above; no Play Console reading was taken |
| 15 further lane queries are `fortress`/`mixed`/`dead-zone` | MEASURED | `node scripts/scan-market.mjs` with all 15 queries + both controls, table in §3.6 |
| eleven carrier names were eliminated by collision | MEASURED | web screens in this session, named with their colliding entity in §8 |
| a learner would rather buy drills than rent a weekly subscription | UNVERIFIED | needs a price test or a comparable one-time-priced exam app; `enrich-market.mjs --query "part 107 test prep cost"` finds none |
| generated drills improve a learner's score | UNVERIFIED | needs a study-outcome measurement no reading here provides |

## 14. Recommendation

Scaffold `part-107-drills` from the three plans beside this file, at **$9.99 one-time**, with the
`billing` capability and no `ads`, and hold the release gate at human checkpoint 1.5 as usual. This is
the only `candidate` pocket in the lane, and its money is currently rent — which is the one thing a
one-price app can beat without inventing a wedge that the shelf's 2026 entrants have already taken.

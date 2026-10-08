# Proposal — `laneset`

**Lane:** planner / optimiser — the app takes the user's constraints and computes a plan from
programming rules it ships. Not a log, ledger, tracker, register, checklist, CRUD table, counter or
calculator. **Offline by construction, no account, no server.**

**Status:** proposed, awaiting **human checkpoint 1**. Nothing is scaffolded until the owner accepts
it. Written 2026-10-02 by the recon scout role.

**The one-line wedge.** Where the shelf's leaders compute sets from a fixed rotating library and sell
the useful half as a recurring plan, this app **computes each session from the swimmer's own
constraints — today's pool length, available minutes, focus, strokes to avoid and the gear actually on
the deck — against authored programming rules, unlocks once instead of every month, walks the swimmer
through the two time trials that turn every interval into a target time, and prints or shares the set
list** so it can be swum without a watch, a signal or an account.

---

## 1. Why this shelf, and the lane boundary tested hard

The scan set was screened with the calibrated sensor in the same invocation
(`docs/research/2026-10-02-planners-and-optimisers-evidence.md` §1–§2). The lane boundary was tested
before selection, not after: a shelf whose real job is "log your sessions" is a notebook and was
discarded even when its numbers were attractive — `rowing training plan` (candidate, 7/30 rated
4.10 median) and `crop rotation planner` (candidate, 13/30, 4.00) were both dropped for exactly this,
the way `docs/reviews/2026-09-26-slot-1-economics-postmortem.md` dropped pipe sizing for having no
revenue mechanism: the first puts the *session log* in the middle of the job, the second is an
agricultural record book with a rotation table attached.

`swim workout generator` was chosen because it is the rare lane shelf where the **computing half is
the product** and the shelf's own users name both the computation and the paywall as what is wrong.

**Already-explored lanes were not revisited.** Authored exam drills, generator/creative tools,
real-time music instruments and assistive communication boards were explored in previous rounds and are
outside this proposal. Nothing under `apps/**` was read; the factory's own work was visible only as
`id` / `name` / `idea` in `registry/apps.json`, and only to confirm the slug is free.

---

## 2. The shelf and the leader-first screen

### 2.1 Shelf and primary query

- **Shelf:** the structured swim-training generator; **primary query** `swim workout generator`.
- **MEASURED** — `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "swim workout
  generator"` → 30 cards, verdict `mixed`, largest bucket `100K+`, **7 of 10 declare IAP, 2 declare
  ads, 0 declare a price.**

### 2.2 The leaders, named by the criterion stated before the data was read

The criterion was fixed in advance: **highest install bucket, then highest exact review count** (the
unrounded schema.org twin), with a second name for the **on-job leader** when the mechanical winner
does not do the job.

| | package | name | rating | exact reviews | installs | ads | IAP | job |
|---|---|---|---:|---:|---|---|---|---|
| **Criterion leader** | `net.smartwod.workouts` | SmartWOD Workout Generator | 4.6 | **3,231** | **500K+** | true | true | generic CrossFit WOD generator — **not swimming** |
| **On-job leader** | `com.myswimpro.android.app` | MySwimPro: Swim Workout App | 3.8 | **3,102** | 100K+ | false | true | the job |
| On-job, better rated | `io.swimup.application` | SwimUp - Swimming Training | 4.5 | 1,234 | 100K+ | false | true | the job |
| On-job, better rated | `com.gizmodus.myswimcoach` | Swim Coach - Swim Training App | 4.6 | 2,771 | 100K+ | false | true | the job |
| On-job, weakest | `com.tenklakessoftware.swimmetry` | Swimmetry | 2.3 | — | 10K+ | — | — | the job |
| On-job, weakest | `com.SwimTrainer.SwimTrainer` | SwimTrainer | 2.7 | — | 10K+ | — | — | the job |

The criterion leader is a CrossFit WOD generator that happened to surface on this query; it is named
because the brief requires the mechanical winner, and it is **not** used as the comparison, because a
swim-specific wedge cannot be measured against an app that does not generate swim sets. The
comparison is `com.myswimpro.android.app`, the app a new listing must actually beat, plus the two
well-rated on-job incumbents.

### 2.3 What the leader's own users wrote

**MEASURED** — `node scripts/scan-complaints.mjs --top 8 --concurrency 2 --query "swim workout
generator"` (24 reviews read, 14 critical, 0 empty, 4 unclassified; pocket **missing-feature 36%,
paywall 29%, broken 21%, crash 14%, notifications 14%, account 7%**) and
`node scripts/scan-complaints.mjs --package net.smartwod.workouts --package com.gizmodus.myswimcoach`:

| where | verbatim (truncated exactly as the sensor printed it) | class |
|---|---|---|
| **on-job leader**, `com.myswimpro.android.app`, 3.8★ / 100K+ / 3,102 reviews | 1★ *"Swim workouts seem legit, however **no real guidance to do the test workouts to get the dynamic pacing in place**. Analysis is extra, 50, 100 or 150 bucks"* | missing-feature |
| **on-job leader** | 1★ *"I understand having a premium option. But I was under the impression there was at least some stuff that was part of the free app usage (not the 7 day …"* | paywall |
| **`com.gizmodus.myswimcoach`**, 4.6★ / 100K+ / **2,771** reviews | 2★ *"…in terms of **the sets that the app generates are very repetitive** and not very good for…"* | missing-feature |
| **criterion leader**, `net.smartwod.workouts`, 4.6★ / 500K+ / 3,231 reviews | 3★ *"Common sense functionality not there… you can't log/record a workout unless you run the whole…"* | missing-feature |
| `com.SwimTrainer.SwimTrainer`, 2.7★ / 10K+ | 1★ *"**Was interested in having custom workouts ready to go without having to plan them myself.** But if it sounds too good to be true, it always is."* | missing-feature |
| `com.SwimTrainer.SwimTrainer` | 2★ *"…as well as the **ease of sending workouts to my Garmin Forerunner 955**…"* | (positive — names the shelf's handoff) |
| `com.SwimTrainer.SwimTrainer` | 2★ *"…Now it **crashes every time I try to end**…"* | crash |
| `com.tenklakessoftware.swimmetry`, 2.3★ / 10K+ | 1★ *"I upgraded my phone and couldn't transfer my **Swimmetry account**, so I canceled my **subscription**…"* | account / paywall |
| `com.tenklakessoftware.swimmetry` | 1★ *"There were two **subscriptions** (monthly or annually) options…"* | paywall |

The shelf's whole complaint vocabulary is: the generated sets are repetitive, the useful half is
behind a recurring plan, the pacing is unguided, it crashes, and it wants an account.

### 2.4 The clause-by-clause head-to-head

Every clause below is measured against the **on-job leader** (`com.myswimpro.android.app`) and, where
it is sharper, against `com.gizmodus.myswimcoach`, whose own user wrote the repetition complaint.

| # | Wedge clause | vs the leader's measured facts | verdict |
|--:|---|---|---|
| 1 | **One purchase, no subscription, no ads** | **CRITICISED FOR NOT DOING IT** — paywall is 2 of the 3 critical reviews sampled on the leader (*"not the 7 day…"*, *"50, 100 or 150 bucks"*), 4 of 14 critical reviews in the pocket (29%), and both criticals on `swimmetry` | **wedge** |
| 2 | **Guided threshold pace test → a target time on every interval** | **CRITICISED FOR NOT DOING IT** — the leader's own 1★: *"no real guidance to do the test workouts to get the dynamic pacing in place"* | **wedge** |
| 3 | **Sets computed from constraints, not sliced from a fixed library** | **CRITICISED FOR NOT DOING IT** — `com.gizmodus.myswimcoach` (4.6★, 2,771 reviews), 2★: *"the sets that the app generates are very repetitive"* | **wedge** |
| 4 | **Handoff: share the set list as text and print a one-page card** | **SILENT** — the shelf's handoff is a **watch** (a positive review praises *"sending workouts to my Garmin Forerunner 955"*); the leader bundles its own watch/sync ecosystem. No incumbent reviewer was recorded asking for print or share | **wedge (secondary)** |
| 5 | **Pool length, minutes, focus, strokes to avoid, gear on deck** | **SILENT** — no enriched card advertises an equipment- or constraint-aware builder; the leader's own missing-feature classes never name it | **wedge (secondary)** |
| 6 | **Works offline, no account** | **CRITICISED FOR NOT DOING IT (weak)** — account is 1 of 14 critical reviews in the pocket, on `swimmetry`; the leaders ship account-backed platforms. But **no leader reviewer asked for offline**, so this is a hygiene clause, not a purchase reason | **not load-bearing** |
| 7 | **Training history, times, charts, tracking** | **LEADER ALREADY DOES IT** — `com.spiraledge.swimapp` (Wokouts & Tracking) leads with it, `swimmetry` logs, the leaders keep history | **explicitly excluded from scope** (and it is also the notebook shape this lane forbids) |
| 8 | **Video technique library and coaching content** | **LEADER ALREADY DOES IT** — MySwimPro ships video analysis; `learn.swimming.lessons.app.learning` (4.7★, 1,329) is a whole teaching product | **excluded; cannot win** |
| 9 | **Challenges, streaks, social/motivational programme** | **LEADER ALREADY DOES IT** — `com.spiraledge.swimapp`: *"I wanted to use the challenges as motivation"* | **excluded** |
| 10 | **Learning to swim / stroke lessons** | **LEADER ALREADY DOES IT, better rated** — 4.7★ with 1,329 reviews | **excluded** |

### 2.5 The entry filter

The requirement is at least **two core clauses answering complaints the leader's own users wrote**.
Clauses 1 and 2 both answer critical reviews on `com.myswimpro.android.app` itself — the paywall
complaint (2 of 3 sampled criticals) and the unguided-pacing complaint (its 1★, quoted verbatim
above). Clause 3 answers the same complaint on a second on-job incumbent with 2,771 reviews. **The
entry filter passes on the leader's own users' words, not on the weakest apps on the page** — which is
exactly the defect the previous round was corrected for.

### 2.6 Verdict

**PLAUSIBLE.** A repairable, well-reviewed gap exists in the exact mechanism this app computes, on a
shelf where demand is proven (five cards in the `100K+` bucket and a leader with 3,102 reviews) and the
job is a computation rather than a log.

**The one thing that would have to be true:** a swimmer must pay a **one-time** price for a workout
generator in a shelf that monetizes entirely through free downloads plus recurring in-app purchases.
Nothing on this shelf is priced (0 of 10 enriched cards declare a price and 7 of 10 declare IAP), so
there is **no priced comparable**, and the willingness-to-pay reading that the brief requires is
therefore inherited from the shelf's IAP behaviour rather than observed as a price. That single
unproven step is what the first 90 days read via `check-outcomes.mjs` would settle.

**What the leader has that we do not** (at least two concrete things):

1. **A watch ecosystem.** The leaders sync structured workouts to Garmin and Apple watches; the shelf's
   own vocabulary for "the handoff" is *"sending workouts to my Garmin Forerunner 955"*. We are
   phone-only, and our handoff is text and print — strictly a worse answer for a swimmer who already
   owns and wears a supported watch.
2. **Scale, video and a coaching library.** MySwimPro carries 3,102 reviews and a video/technique
   platform; `Swim Coach` carries 2,771 inside a training-app ecosystem; `SwimUp` holds **4.5★ over
   1,234 reviews with zero critical reviews in its sampled page** — a leader with no measured
   complaint in the three reviews Play served. We ship none of the video, none of the coaching
   content, and no history at all.
3. **History and tracking.** Every leader keeps the swimmer's times and charts them. This proposal
   deliberately ships **no log**, because the lane forbids the notebook shape — an honest cost, not a
   hidden one.

---

## 3. The recipe

| | |
|---|---|
| **slug** | `laneset` |
| **name / title** | `Laneset: Swim Set Builder` (25 characters, 5 of slack under Play's 30) |
| **applicationId** | `com.gexetr.laneset` (confirmed free in `registry/apps.json`) |
| **idea** | A swimmer says how long they have, how long the pool is, what they are training and what gear is on the deck; the app computes the whole session — warm-up, main set, kick and pull blocks, cool-down — with a target time per 100 printed on every interval, and hands it over as a shareable or printable set list. |
| **What it is not** | not a session log, not a stopwatch, not a stroke-lesson course, not a video library, not a challenge board |

### 3.1 The job, in three stages

| stage | the job | what ships |
|---|---|---|
| **before** | the swimmer brings a time trial and, if they have one, a coach's written workout | a guided 400 and 200 time trial, and a threshold pace computed per 100; a paste-a-coach-workout import |
| **during** | the core act: compute the session | the constraint-driven generator — pool length, minutes, focus, strokes to avoid, gear on deck — with a target time on every send-off, offline |
| **after** | the swimmer swims it | share as text, print a one-page card, one permanent unlock, no account, no subscription, no ads |

### 3.2 Depth families

`core_operation` (the computed session, `during`) · `data_in` (the coach's written workout, `before`) ·
`fit` (the pace test that gives every interval its target, `before`) · `data_out` (share and print,
`after`) · `trust` (one unlock, no subscription; offline, no account) · `durability` (a session
survives an interruption) · plus two excluded-by-design families named in §2.4 rows 7–9.

---

## 4. The money

### 4.1 Who pays, for what, when

- **Paying user:** a pool swimmer training three to six sessions a week in a masters or club lane, or
  a triathlete doing structured pool work, who today builds sets by hand or copies a coach's whiteboard
  and does not want another monthly subscription.
- **What is bought:** one permanent unlock — every training focus, the guided pace test, the
  constraint-driven set builder, and the share/print handoff. No ads, no account, no renewal.
- **Price:** **$9.99**, one time.
- **Moment of payment:** on the pool deck, after the free Endurance session has been generated and
  swum once, when the swimmer taps a locked focus or reaches for the handoff — one clearly priced
  sheet, no trial clock and above all no subscription prompt.

### 4.2 Archetype, and the rejected alternatives with revenue reasons

- **Chosen:** `purchase_led_single_unlock`, with the `billing` capability required and `ads`
  deliberately not enabled.
- **`purchase_led_repeat_sales` — rejected:** the shelf sells content, not software. A swimmer does not
  buy a second workout pack, they swim the generator; the record shows the category monetizing the
  *programme* as a recurring plan, which is exactly the mechanism the wedge exists to refuse, and
  charging twice for a generator that already answers the query would earn nothing new.
- **`ads_led_utility` — rejected:** an interstitial on a pool deck is worth about $2 eCPM on an
  assumed 3 impressions per session, so $1,000/month needs roughly 500,000 impressions/month — about
  167,000 sessions/month, against a whole shelf whose leader has 3,102 reviews. The eCPM is an
  assumption, not a reading, but the order of magnitude is the revenue reason.
- **`purchase_only_no_ads` — rejected:** it keeps the price but gives up the free tier, and the free
  trial session is the *only* moment the unlock can be sold, because a swimmer must swim the generated
  session before they believe the generator. Without the free session there is no conversion event.
- **`subscription` — banned outright** by `.cursor/rules/ideation-gate.mdc`, and `subscription` is not
  in `ARCHETYPES`, so a plan naming it fails outright. It is also the shelf's own defect: the
  2.3★/10K+ `swimmetry` carries two 1★ reviews about nothing but subscriptions and account transfer.

### 4.3 Unit arithmetic, division shown

```text
price                                      9.99
Play service fee 15%                       -1.50     (9.99 x 0.15 = 1.4985)
refund allowance 2%                        -0.20     (9.99 x 0.02 = 0.1998)
currency conversion 2%                     -0.20     (9.99 x 0.02 = 0.1998)
---------------------------------------------------------------------
net per sale                                8.09     (9.99 x (1 - 0.15 - 0.02 - 0.02) = 9.99 x 0.81)

target from docs/monetization-strategy.md:  1,000.00 per month
sales/month  = 1,000 / 8.09              =  123.6   -> 124 sales/month
sales/day    = 124 / 30                   =    4.1
installs at an assumed 3% conversion
             = 124 / 0.03                =  4,133/month
```

The 15% is the sub-$1M service-fee tier, so the arithmetic is a floor; the 3% conversion is an
assumption with **no reading behind it** — no slot in this repository has measured one.

### 4.4 Kill criteria

```text
windowDays     = 90
minRevenueUsd  = round(1,000 x 3 months x 0.25) =   750
minInstalls    = round(4,133 x 3        x 0.25) = 3,100
```

Derived from the target, not chosen beside it: a quarter of the target run-rate sustained over the
first 90 days is the continue-or-kill line, because a listing that has reached it has shown the ramp is
real and one that has not will not reach $1,000/month from a standing start. The quarter factor is
**INFERRED** from the shape of a cold-start ramp, not measured.

### 4.5 Build, permissions, capabilities, server

- **Build estimate: 6 agent hours**, in the band of this factory's own recipes
  (`docs/ideation/2026-09-26-five-recipes.md` estimates 3.5–6). The cost is in authoring the
  programming rules — the pace zones, the set shapes per focus, the equipment substitutions — not in
  the screens.
- **Permissions: none.** No `INTERNET` — the rules, the pace arithmetic and every session ship inside
  the APK. The only declared permission is the billing library's own `com.android.vending.BILLING`.
- **Capabilities:** `billing` on, `ads` off, deliberately.
- **Server: not required**, and not wanted: the app is offline by construction, and
  `.cursor/rules/client-secrets.mdc` is satisfied trivially because the app talks to no provider.

---

## 5. The naming decision

Recorded at recon, per `.cursor/rules/app-naming.mdc`, in `laneset.naming.json`.
Eight candidates span **four archetypes** and the chosen one scores **90/100 with no dimension below
4.5**; four alternatives are rejected in writing, two of them by a live collision that outranks any
score.

| candidate | archetype | score | outcome |
|---|---|---:|---|
| **Laneset: Swim Set Builder** | compound | **90** | **chosen** |
| Blocksmith: Swim Sets | coined | 85 | rejected — **live Play collision** (`com.jerus.blocksmith`) |
| Setline: Swim Set Builder | compound | 82 | rejected — **live Play collision** (`com.setline.app`) |
| Lapwheel: Swim Set Builder | metaphorical | 78 | rejected — a lap wheel reads as pool hardware, and the carrier carries no swim token |
| Poolcraft: Swim Sets | coined | 74 | rejected — the `-Craft`/`-Forge` token space on Play is idle-RPG games |
| Swim Set Builder | descriptive | 71 | rejected — a category phrase with no carrier for a brand to compound into |
| Swim Workout Builder | descriptive | 62 | rejected — the shelf's own head term, already owned by apps with 100K+ installs |
| Planner | descriptive | 40 | **negative control** — the bare category noun, loses to every serious candidate |

Carrier screening (**MEASURED**, §5 of the evidence file): a 29-card Play page for `laneset` shows
**no app carrying the token**; `blocksmith` and `setline` are live apps on live package ids; the
`-Forge` page is dominated by idle-RPG titles. The negative control is the bare category noun, which
the contract requires and which loses on every dimension that matters.

**Title fit, measured.** `Laneset: Swim Set Builder` is **25 characters** against Play's 30 — **5 of
slack**, the contract's minimum — and the `ru-RU` variant `Laneset: план тренировок` is **24
characters**, 6 of slack. Neither locale overflows. The job half was deliberately narrowed from
`Swim Workout Builder` (29 characters, 1 of slack) to `Swim Set Builder` to buy that slack back: the
app's audience calls a workout a *set*, and the title keeps the shelf's head token, but dropping the
word `workout` is a real ASO cost against the primary query and it is recorded as such rather than
presented as a win.

**The honest limit.** That screen is a *Play search reading only*. No trademark register was queried,
so `Laneset` being free in class 9 is **UNVERIFIED**; the deciding action is a USPTO / EUIPO / WIPO /
Rospatent class 9 search, and a clean Play search is not clearance because Play does not enforce unique
titles.

---

## 6. Refusals — with revenue reasons, not novelty reasons

Every shelf below was screened and refused. `.cursor/rules/ideation-gate.mdc` is explicit that "this
shelf is crowded" is **not** a reason; each refusal below names the money.

| shelf refused | verdict, MEASURED | revenue reason for the refusal |
|---|---|---|
| `meal prep plan generator` | `fortress` — 13/30 rated, median 4.60 | **Demand is proven and the money is not in dispute — this is a refusal on the build estimate, not on the market.** Every card that does the job is a recipe/meal-plan product (user recipes, nutrition, shopping lists), not a constraint-to-programme computation, so delivering the job means authoring a recipe corpus and a nutrition model well beyond 6 agent hours, and a thin version would be beaten by the 4.6 median on the page. Refused as **cost eats the margin**. |
| `sight reading practice plan` | `mixed` | A **5M+ fortress** does the job (`com.binaryguilt.completemusicreadingtrainer`, 4.6★, 12,248 reviews) with a second 5M app beside it (86,279 reviews), and the plan half is a practice schedule whose core act is timing the user's own reading. Refused because the wedge would be a subset of an incumbent's feature list, so the expected revenue does not justify the build. |
| `interview prep planner` | **`dead-zone`** — 7/30 rated (0.23) | **No demand reading**: the demand reading is unrated or absent, which is itself a named rejection in `ideation-gate.mdc`. |
| `apnea training plan` | `dead-zone` — 7/30 rated, median 4.80 | Same: too few rated incumbents to read demand, and the freediving audience is the narrower half of a shelf with no volume. |
| `study plan generator` | `candidate` — 12/30 rated, median 4.20 | **Collision**: the query is served by general school diary and organiser apps, so the wedge sentence cannot be written as a difference in functionality — the plan half is a calendar feature inside a product that already has it. |
| `rowing training plan` | `candidate` — **7/30 rated**, median 4.10 | **Thin demand**: the query itself returns few incumbents, but the ones that do the job are session logs — which is the notebook shape this lane forbids — and the shelf has 7 rated apps, so a first listing would be competing for a paying user who does not demonstrably exist yet. |
| `crop rotation planner` | `candidate` — 13/30 rated, median 4.00 | **Lane boundary**: the visible cards are agricultural record books with a rotation table attached; the job is "record the field", so the revenue would come from a notebook. Refused on the lane rule, and the money reason is that the planner half is given away free inside the record book. |
| `hangboard training plan` / `climbing training program` | `mixed` — 21/30 rated, median 4.40 | **No measurable wedge**: the strongest card on the page is a general training-log culture ("training log" appears in the strongest titles on this lane), so the computed half would sit inside a notebook, and the climbing shelf's paying product is a hangboard protocol the incumbents already publish. |
| `guitar practice plan` / `piano practice schedule` | see `sight reading` | Same shape as the sight-reading refusal: the plan is a schedule over the user's own practice, and the shelf is dominated by large free practice/metronome incumbents with the schedule as one screen among many. |

Two shelves from the brief were not screened at all, deliberately and on the record:
`wine cellar plan` (the brief itself flags it: it collapses into storage management — a notebook) and
`chess opening trainer` (its core is an authored content library the user drills, which is the
already-explored exam-drill shape, and the shelf is populated by free 1M-install engines and databases).

---

## 7. Visual direction, declared before the UI

The identity is per slot and declared in `store/studio.json` at stage 1; the palette is the source of
truth the slot's `ui/theme/Color.kt` and the icon are rendered from
(`.cursor/rules/visual-identity.mdc`). The mark is a **lane line and a turning cross rendered as two
stacked strokes**: *laneset* is a set in a lane, and the mark has to read at 48 dp inside the
adaptive-icon masks without a baked-in rounded mask or shadow. The palette is pool water at depth —
deep teal-blue fields, one high-chroma accent reserved for the target time on an interval, because
the one number the app is built around should be the one number that is coloured. This is a
declaration, not a rendering: no asset is produced in phase 1.

---

## 8. Honest unknowns

- **Willingness to pay is inherited, not observed.** No priced comparable exists on this shelf (0 of 10
  enriched cards declare a price). The one thing that must be true — a one-time price converting on a
  shelf of free-plus-IAP apps — is `UNVERIFIED`.
- **The conversion rate is invented.** 3% is an assumption with no reading behind it, and the flagship
  figure of 124 sales/month moves directly with it.
- **The complaint tables describe vocabulary, not frequency.** Play serves three reviews per page; a
  leader with no critical review in the sample is a leader whose complaints were not served. See the
  header of the evidence file.
- **`com.myswimpro.android.app` is a 100K+ app with a watch ecosystem, video and a coaching library**,
  and it win rate is 3.8★. A listing that beats it on price and pacing may still lose on everything
  else, and the shelf's best-rated app (`io.swimup.application`, 4.5★ over 1,234 reviews) produced no
  measurable complaint at all.
- **Trademark is unsearched** (§5).
- **All storefront readings are one signed-out session** and are not reproducible byte-for-byte.
- **No revenue API exists.** The readings that would settle any of this come from Play Console via
  `node scripts/check-outcomes.mjs`, which is a hand-recorded reading and is labelled as such.

The claim ledger, with the label and the command behind every row, is §7 of
`docs/research/2026-10-02-planners-and-optimisers-evidence.md`. Its `UNVERIFIED` rows are the list
above, and they are the questions the owner is being asked to accept or reject at checkpoint 1.

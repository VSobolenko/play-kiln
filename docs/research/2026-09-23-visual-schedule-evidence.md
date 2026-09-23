# Visual-schedule / autism-routine niche evidence — 2026-09-23

> **Question this run tests.** `docs/market-scan-2026-09-23.md` ranked `visual schedule autism` as
> its #1 candidate (score 110.9) from a **single** ratings-only query, and the factory re-targeted its
> first real slot onto it. That screen was produced *before* the sensor's fuzzy-match defect was
> known: Play search is a fuzzy **text** match, and a sibling pass had already scored `late blight`
> and `whitefly treatment` as confident verdicts over lighting and mental-health apps. The tokens in
> play here — **`visual`** (visual novels, visual voicemail, visual design) and **`autism`** — are
> exactly the kind that match unrelated text.
>
> **So this run re-tests the niche from twelve phrasings of the same job, and classifies every
> returned card by hand before drawing any conclusion from it.** The single most important result
> below is not the verdict; it is the on-topic share that the verdict was computed over.

## How this was produced

Five commands were run. Exit codes and raw artifact paths are as executed.

```bash
# 1) Ratings-only card screen, 12 target queries (scan-market.mjs also re-screens its 17 built-in
#    archetype queries because --archetype was not passed, so the run returned 28 queries).
node scripts/scan-market.mjs --json --concurrency 4 \
  --query "visual schedule autism"       --query "visual schedule for kids" \
  --query "autism routine app"           --query "picture schedule autism" \
  --query "daily routine visual kids"    --query "autism visual timer" \
  --query "visual schedule app"          --query "autism communication app" \
  --query "social stories autism"        --query "token board autism" \
  --query "visual timetable autism"      --query "autism classroom visual schedule"
# exit 0 — 28 queries, 0 errored.  -> .kiln/visual-schedule/screen.json   (199,204 bytes)

# 2) Per-app facts for the same 12 queries, top 10 cards each.
node scripts/scan-plant-vertical.mjs --json --top 10 --concurrency 3 \
  --out .kiln/visual-schedule/enrich-report.md --query "…the same 12 queries…"
# exit 0 — 120 app pages enriched, 0 failures. -> .kiln/visual-schedule/enrich.json

# 3) Same, but top 30 (every visible card) for the 8 on-topic queries, so the install-magnitude
#    answer is not limited to a top-N cut.
node scripts/scan-plant-vertical.mjs --json --top 30 --concurrency 4 \
  --out .kiln/visual-schedule/enrich-full-report.md \
  --query "visual schedule autism" --query "visual schedule for kids" --query "autism routine app" \
  --query "picture schedule autism" --query "daily routine visual kids" \
  --query "visual timetable autism" --query "autism classroom visual schedule" \
  --query "autism visual timer"
# exit 0 — 221 app pages enriched, 0 failures, 150,899 ms. -> .kiln/visual-schedule/enrich-full.json

# 4) Listing TEXT. The factory fetcher (scripts/lib/play-app-detail.mjs) deliberately does not expose
#    descriptions, so the nine decision-critical store pages were fetched directly by me.
node .kiln/visual-schedule/fetch-pages.mjs
# exit 0 — 9/9 HTTP 200 -> .kiln/visual-schedule/pages/*.html
# extracted text -> .kiln/visual-schedule/listing.txt

# 5) Throwaway derived views over the raw JSON above (no factory script was modified).
node .kiln/visual-schedule/derive.mjs   # exit 0 -> .kiln/visual-schedule/derived.txt
node .kiln/visual-schedule/derive2.mjs  # exit 0 -> .kiln/visual-schedule/cards.txt
node .kiln/visual-schedule/derive3.mjs  # exit 0 -> .kiln/visual-schedule/cards-full.txt
node .kiln/visual-schedule/derive4.mjs  # exit 0 -> .kiln/visual-schedule/derived4.txt
node .kiln/visual-schedule/derive5.mjs  # exit 0 -> .kiln/visual-schedule/derived5.txt
```

- **Run timestamps.** Screen `2026-09-23T20:26:16.362Z` (7,950 ms wall clock); top-10 enrichment
  `2026-09-23T20:26:24.388Z` (37,211 ms); full-coverage enrichment `2026-09-23T20:28:41.061Z`
  (150,899 ms, i.e. 151 s for 221 page fetches at concurrency 4).
- Signed-out Play, `hl=en`, `gl=US`, one session. Search cards from `scripts/lib/play-scan.mjs`;
  per-app facts from `scripts/lib/play-app-detail.mjs`; both **unmodified**. The CLI is
  `scripts/scan-plant-vertical.mjs` (its name is historical; it is a general per-query scanner).
- **Every number below is from these runs.** Nothing was estimated, remembered, or carried over.
- Raw output lives under `.kiln/visual-schedule/` (gitignored). Cited paths are listed above.

**Reading the columns.** `installs` is Play's rounded bucket (`100K+`), so it is a **floor, not a
count**. `ads` / `iap` are three-valued: `true` = Play showed the badge, `false` = the badge region
was present and did **not** show it, `unknown` = the badge region itself was absent. In the
full-coverage run **0 of 221 pages** had a missing badge region and **0 fields** went unparsed, so
every `unknown` count below is an observed `0`, not a default.

### A drift check that matters

Because `scan-market.mjs` re-screens its built-in archetype set, this run re-measured all **17**
calibration queries. **All 17 reproduce the previous market scan exactly** — verdict, `seen`,
`rated/total`, `weak` and `median` are identical for every one, including the three controls
(`bjj training log` dead-zone 9/30, `concrete calculator` fortress 7/9, `aquarium water log`
fortress 11/30) and `visual schedule autism` itself (`candidate`, 30 seen, 14/30 rated, 11 weak,
median 3.85). So the earlier `candidate` label is reproducible a day later and is not sensor noise.

## The card-classification discipline (the part that governs everything below)

I read the titles of **every** visible card myself and labelled each one. The labels are mine, not
the sensor's. The three categories used:

| label | meaning |
| --- | --- |
| **direct** | a visual-schedule, picture-schedule or visual-routine app for autistic / neurodivergent children (or the classroom/therapy setting that serves them) |
| **adjacent** | a different intervention or tool for the same audience/job family: AAC communication, token boards / reward charts, social stories, visual timers, autism parenting or tracking apps, autism learning games, and general kids routine/chore/timer apps |
| **off-topic** | neither — generic productivity planners, calendars, adult habit trackers, dating apps, exam-prep, school LMS, children's story books |

"On-topic" below means **direct + adjacent**. `direct` is reported separately because it is the
column that decides whether the proposed job is served badly.

**Five queries were not usable as evidence about this job, and no conclusion rests on them:**

| query | what Play actually returned | usable? |
| --- | --- | --- |
| `visual schedule app` | 23 of 30 cards are **generic productivity planners** — `Todoist`, `TimeTree`, `Simple Calendar`, `Any.do`, `Structured`, `Schedule Deluxe`, and `Todoery: To Do List & Planner` (10M+, 711,000 ratings). Only 5 of 30 are visual-schedule apps. | **no** — its `fortress` verdict (27/30 rated, median 4.60) describes the *planner* shelf |
| `autism visual timer` | 12 of 12 are **generic visual timers**; **0 of 12 titles contain "autism" or "autistic"**. Its `fortress` verdict (7 rated, 0 weak, median 4.60, two apps at 1M+) describes the *generic timer* shelf | **no** — the `autism` token bought nothing |
| `autism communication app` | 30 of 30 are **AAC / speech** apps; **0 are visual-schedule apps** | **no** — a different job |
| `social stories autism` | 25 of 30 are autism-related, but only **1 is a visual-schedule app**; the rest are social stories, AAC and autism content | **adjacent job only** |
| `token board autism` | 27 of 30 are autism-related, but only **1 is a visual-schedule app**; the rest are token boards, AAC and ABA | **adjacent job only** |

This is a limitation of the *query text*, not of the libraries: both behaved correctly and reported
what Play served. It also shows where the sensor's own relevance guard is not sufficient here — the
guard counts whole-token overlap, so `visual` and `schedule` are "distinctive tokens" and it scored
`visual schedule app` as merely `partial` (15/30) where my reading is **7/30**. When a query token is
a common word, the guard **under-counts** pollution, which is why the classification above is
hand-made and stated per query.

## The query set (12)

The six commissioned phrasings plus six more that cover the same job from parent, teacher and
clinician phrasings, and the two adjacent jobs the same audience buys:

`visual schedule autism`, `visual schedule for kids`, `autism routine app`, `picture schedule autism`,
`daily routine visual kids`, `autism visual timer`, `visual schedule app`, `autism communication app`,
`social stories autism`, `token board autism`, `visual timetable autism`,
`autism classroom visual schedule`.

---

## 1. Is the market real, and how big?

Verdict, `rated/total`, `weak` (< 4.2★) and median are the ratings-only screen's own output for this
run. "On-topic" is **my** per-card judgement, with the count of `direct` apps broken out.

| query | verdict | visible | on-topic (mine) | of which direct | off-topic | rated/total | weak | median |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `visual schedule autism` | **candidate** | 30 | **30/30** | 12 | 0 | 14/30 | 11 | 3.85 |
| `autism routine app` | **candidate** | 30 | **28/30** | 7 | 2 | 19/30 | 10 | 4.10 |
| `picture schedule autism` | **candidate** | 30 | **28/30** | 8 | 2 | 14/30 | 9 | 4.00 |
| `visual timetable autism` | **candidate** | 30 | **29/30** | 8 | 1 | 17/30 | 11 | 3.90 |
| `autism classroom visual schedule` | **candidate** | 29 | **26/29** | 8 | 3 | 13/29 | 10 | 3.70 |
| `visual schedule for kids` | mixed | 30 | 21/30 | 4 | 9 | 20/30 | 8 | 4.20 |
| `daily routine visual kids` | mixed | 30 | 19/30 | 3 | 11 | 22/30 | 7 | 4.30 |
| `visual schedule app` | fortress | 30 | **7/30** | 5 | 23 | 27/30 | 5 | 4.60 |
| `autism visual timer` | fortress | 12 | **0/12 autism-specific** | 0 | — | 7/12 | 0 | 4.60 |
| `autism communication app` | candidate | 30 | **0/30 schedule** (30/30 AAC) | 0 | — | 22/30 | 13 | 3.90 |
| `social stories autism` | candidate | 30 | 25/30 *(adjacent job)* | 1 | 5 | 16/30 | 10 | 3.85 |
| `token board autism` | candidate | 30 | 27/30 *(adjacent job)* | 1 | 3 | 17/30 | 12 | 3.80 |

**Off-topic cards, named so the counts can be audited** (title only; package ids are in the raw
JSON):

- `visual schedule for kids` (9): Goal Tracker: Routine Planner · TimeTune - Schedule Planner ·
  Cute Calendar Schedule Planner · Routine Planner, Habit Tracker · HabitNow Daily Routine Planner ·
  FlowSavvy: AI Schedule Planner · Time Planner: Schedule & Tasks · Routine: Calendars & Tasks ·
  Easy Parenting: Daily Planner
- `autism routine app` (2): Hiki: Autism ADHD & ND Dating · Spoons - Autism Energy Tracker
- `picture schedule autism` (2): Spoons - Autism Energy Tracker · Autism Parenting Magazine
- `daily routine visual kids` (11): Goal Tracker · Routinery · HabitNow · Roubit:Daily Life Routine
  Care · Me+ Lifestyle Routine · Daily Routine Planner PlanEase · MyRoutine: Routine Habit Goal ·
  TimeTune · Cute Calendar · Dear Me: Daily Routine Tracker · Morning Habits - Daily Routine
- `visual schedule app` (23): Time Planner · TimeTune · Sectograph · Smart Timetable · Todoery ·
  TimeTree · Cute Calendar · Structured · CalendPal · Todoist · Simple Calendar · Daily Schedule:
  Weekly Planner · FlowSavvy · Timeline - Schedule by Hour · Plan Calendar · My Schedule ·
  Student Calendar · Weekly Planner · Unique: ADHD Planner & Journal · Any.do · Planner Pro ·
  Daily Planner To Do List · Schedule Deluxe
- `social stories autism` (5): Calm Storytime: Truth & Tales · Hiki · Twinkl Originals Story Books ·
  Autism Parenting Magazine · Oscar Stories for Children
- `token board autism` (3): Board Game Token Tracker · Pass My Boards CFPC SAMPs+MCQs · Force Board
- `visual timetable autism` (1): Smart Timetable
- `autism classroom visual schedule` (3): **Google Classroom** · Spoons · MySched: Class Schedule

### Is it fragmented or owned?

**Fragmented in installs, but with two strong apps at the top — and the phrasing decides which shelf
you get.**

- Across the **seven schedule phrasings**, my on-topic share is **181/209 cards (86.6%)**, of which
  only **50/209 (23.9%)** are `direct` visual-schedule apps. The rest are adjacent tools (AAC, token
  boards, timers, autism games, planners) that Play mixes into the same shelf.
- **The exact query is clean.** `visual schedule autism` returned **30/30** cards in the autism
  visual-support universe, with 12 of them `direct` schedule apps and **zero** off-topic. So the
  original 8-card probe was **not** a fuzzy-match artefact for this query — my independent read of
  all 30 cards agrees with it.
- **The `autism` token is what holds the shelf together.** Drop it and the shelf drifts: `visual
  schedule for kids` and `daily routine visual kids` each lose ~⅓ of their cards to generic family
  routine / adult habit apps (HabitNow at 5M+, Routinery, Me+ Lifestyle Routine at 10M+). The
  autism-token phrasings are where dedicated apps appear and where supply looks weakest.
- **No single owner.** The largest *direct* app in the entire run is `com.phonegap.matraquinha`
  "Autism Kids: Voice & Routines" at **500K+**; nothing in the direct job reaches 1M+.
- **But it is not a field of failures only.** The direct cohort's ratings are **bimodal**:
  `app.littleplanner` 4.8★ / 10K+ and `com.phonegap.matraquinha` 4.9★ / 500K+ at the top;
  `com.mts.visualscheduleandstorymaker` 2.0★ / 10K+ and `br.com.phaneronsoft.rotinadivertida`
  2.9★ / 100K+ at the bottom; and **13 of 17 direct apps are unrated**, 13 of 17 at ≤ 1K+ installs.
- The sensor's `candidate` label is therefore **not wrong** — 5 of 7 schedule phrasings return
  `candidate`, and on the exact query 11 of 14 rated incumbents are below 4.2★ — but the picture it
  compresses is *"two good apps, two bad apps, and a long unrated tail"*, not *"mostly bad supply"*.

---

## 2. Who are the incumbents, and what is actually weak?

### Strongest, in the direct job

| app | package | installs | rating | reviews (displayed / exact) | ads | IAP | price | updated |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Autism Kids: Voice & Routines (Matraquinha) | `com.phonegap.matraquinha` | **500K+** | **4.9** | 1,410 / 1,407 | **true** | **true** | free | May 23, 2026 |
| Lil Planner: Visual Schedule (Space Sweater) | `app.littleplanner` | 10K+ | **4.8** | 623 / 623 | false | true | free | Sep 10, 2026 |

### Weakest, in the direct job

| app | package | installs | rating | reviews (displayed / exact) | ads | IAP | price | updated |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Visual Schedules and Social St (MeshTech Solutions) | `com.mts.visualscheduleandstorymaker` | 10K+ | **2.0** | 50 / 50 | false | true | free | Jul 30, 2026 |
| Fun Routine - Autism (Phaneronsoft Sistemas) | `br.com.phaneronsoft.rotinadivertida` | **100K+** | **2.9** | 1,410 / 1,409 | **true** | **true** | free | Sep 10, 2026 |

Two more poorly-rated apps recur across the run but are **adjacent**, not schedule apps:
`air.com.lollipapps.games` "Autism ABC App" (50K+, **2.5★**, 139 reviews, appears in 5 of 7 core
queries) and `com.otsimo.app` "Otsimo | Special Education" (100K+, **3.1★**, 827 reviews).

### Recycled generalists vs genuinely dedicated

Computed from package ids only, never from titles. Counts are how many of the **8 fully-enriched
queries** the app appeared in:

| app | queries | installs | rating | dedicated or recycled? |
| --- | --- | --- | --- | --- |
| Lil Planner: Visual Schedule | **7** | 10K+ | 4.8 | **dedicated** (visual schedule) |
| Thruday - Visual Daily Planner | 6 | 10K+ | 4.0 | general visual planner |
| Visual Schedule: Daily Planner | 6 | 500+ | unrated | **dedicated** |
| Visual Schedules and Social St | 5 | 10K+ | 2.0 | **dedicated** (schedule + social stories) |
| Autism ABC App | 5 | 50K+ | 2.5 | adjacent (ABC/learning) |
| CommBoards AAC – Autism Speech | 5 | 50K+ | 4.0 | adjacent (AAC) |
| Autism Visuals | 5 | 1K+ | unrated | **dedicated** (visual supports) |
| Little Planner: Visual Routine | 5 | 100+ | unrated | **dedicated** |
| Leeloo AAC – Autism Speech App | 4 | 100K+ | 3.8 | adjacent (AAC) |
| RoutineFlow: Routine for ADHD | 3 | 500K+ | 4.2 | adjacent (ND routine, not autism/kids) |
| AutiSpark: Kids Autism Games | 3 | 1M+ | 4.3 | adjacent (games) |

**The difference from the plant vertical is structural and worth stating.** There, a generalist was
recycled across crop-specific queries — the vertical was a *sub-job* the generalists did not serve.
Here **the recycled apps are the dedicated specialists**: Lil Planner (dedicated) is the single most
recycled app in the run, and it appears on 7 of 7 core phrasings. **There is no "vertical inside the
vertical"** — the query set *is* the job. So the factory cannot claim "the incumbent answering this
query is a generic app that does not really do our job"; the incumbent answering this query *is* a
dedicated visual-schedule app.

---

## 3. Do they monetise, and how?

Source: the full-coverage run (`enrich-full.json`, 8 queries, **221** app pages, 0 failures).

| population | n | ads declared / absent / unknown | IAP declared / absent / unknown | free / paid |
| --- | --- | --- | --- | --- |
| every page fetched (8 queries) | 221 | 29 / 192 / **0** | 172 / 49 / **0** | 219 / 2 |
| on-topic schedule queries, distinct apps | 92 | 14 / 78 / **0** | 70 / 22 / **0** | 90 / 2 |
| **direct visual-schedule cohort, distinct apps** | **17** | **2 / 15 / 0** | **10 / 7 / 0** | **16 / 1** |

For the direct cohort: **both** ads and IAP = 2 apps; ads-only = 0; IAP-only = 8; neither = 7. The
only paid app is `com.dktools.myday` "MyDay - visual schedule" at **$4.99**.

**The finding that matters.** Every direct app that has *any* traction declares IAP:

| direct app | installs | rating | IAP | ads |
| --- | --- | --- | --- | --- |
| Matraquinha | 500K+ | 4.9 | yes | yes |
| Fun Routine - Autism | 100K+ | 2.9 | yes | yes |
| Lil Planner | 10K+ | 4.8 | yes | no |
| Visual Schedules and Social St | 10K+ | 2.0 | yes | no |
| RoutinePals | 100+ | unrated | yes | no |
| Little Planner: Visual Routine | 100+ | unrated | yes | no |
| Calmika | 100+ | unrated | yes | no |
| My.Day | 10+ | unrated | yes | no |
| AdaptaLyfe | 10+ | unrated | yes | no |

The **7 apps declaring neither ads nor IAP** are all unrated and all at ≤ 500+ installs
(`Autism Visuals`, `Visual Schedule: Daily Planner`, `Picture Card Schedule Mirumiru`,
`VisualPlanner: Visual Support`, `Visual Routines`, `Picture Schedule - SOYOGI`, plus the one paid
app). "Free, no ads, no charge" is the signature of the **zero-traction** apps in this cohort, not of
the strong ones.

**What this says about the factory's default for a first slot (free, no ads to children).**
The data is consistent with the *no-ads* half: 15 of 17 direct apps declare no ads, and the strong
specialist `app.littleplanner` declares no ads. It **contradicts the "no monetisation at all" half**:
10 of 17 direct apps declare IAP, every direct app with a rating or ≥ 10K+ installs declares IAP,
and the two new entrants that launched in Aug–Sep 2026 (`RoutinePals`, `Little Planner`) shipped
with IAP from day one — `RoutinePals` publishes US pricing of **$6.99/month or $49.99/year** with a
14-day trial, and `My.Day` an optional Pro tier.

Ads are **not** the discriminator the original probe assumed away: the ads-carrying apps are one at
4.9★ (Matraquinha) and one at 2.9★ (Fun Routine), and the ads-free set includes both a 4.8★ leader
and a 2.0★ dud. This run **cannot** say ads hurt ratings — and it cannot say they do not.

---

## 4. What do the bad incumbents appear to lack?

**Limitation, stated plainly.** The repo's fetcher **does not expose listing descriptions** — it
reads the header, install bucket, rating, review count and badges only. To answer this section I
fetched nine store pages **myself** (`node .kiln/visual-schedule/fetch-pages.mjs`, exit 0, 9/9
HTTP 200; raw HTML in `.kiln/visual-schedule/pages/`, extracted text in
`.kiln/visual-schedule/listing.txt`, description taken from the `data-g-id="description"` block).
I read the listing text of the apps below. **A store page shows positioning, not capability** — the
rest of this section is limited to what the page literally says.

**The two bad incumbents do not look feature-poor on their store pages.**

- **`com.mts.visualscheduleandstorymaker` "Visual Schedules and Social St" (2.0★, 10K+)** — a
  3,237-character listing offering: a built-in library of visual schedules *and* social stories;
  the ability to create unlimited of both; text, image, audio **and video** on each step/page; an
  image library; share/email; **print schedules and stories as PDF**; works offline; and it states
  "**No Ads**". Its one visible positioning defect: the listing still advertises a **Google+**
  profile link alongside Facebook and Twitter — a product shut down in 2019, so the marketing
  surface is stale (the *app* is not: `Updated on Jul 30, 2026`).
- **`br.com.phaneronsoft.rotinadivertida` "Fun Routine - Autism" (2.9★, 100K+)** — a 1,730-character
  listing offering custom tasks with images and notifications, interactive flash cards with audio,
  an emotion log, customisable reinforcers, and specific tools for educators and therapists;
  it claims "research-based" design and "over 300,000 downloads". Its visible positioning defect:
  the English reads as machine-translated ("`* TURN CHALLENGES INTO ACHIEVEMENTS`", "`* INNOVATIVE
  FEATURES`"), i.e. the store voice was not localised by a writer.

**What the strong incumbents' pages look like, for contrast.**

- `app.littleplanner` "Lil Planner" (4.8★, 10K+, 785 chars) claims "**The #1 visual schedule app for
  kids, ADHD, and autism**" and sells outcomes ("Increase focus", "Build good habits",
  "Reduce anxiety", "Stop meltdowns") in a parent-to-parent voice.
- `co.littlelabs.littleplanner` "Little Planner: Visual Routine" (100+, unrated, 3,143 chars) is a
  **new entrant** from the team behind "Little Timer" (it claims 800,000+ App Store downloads),
  built around a collection loop (hatch 24 animal friends, 432 story cards) and states "No ads, no
  third-party links ever".
- `com.routinepals.app` "RoutinePals: Visual Schedule" (100+, unrated, 2,679 chars) publishes
  $6.99/month · $49.99/year, a free tier of one child and three routines, sensory settings,
  First/Then, and an explicit scope disclaimer: "**does not provide medical, therapeutic, or
  diagnostic services**".
- `com.phonegap.matraquinha` "Autism Kids: Voice & Routines" (4.9★, 500K+, 2,951 chars) leads with
  AAC ("Give Your Child a Voice") and lists "Visual Routines to Reduce Meltdowns" second.

**What I can establish.** The low-rated incumbents' listings **advertise the features a user of this
job would want**, and the newest entrants ship *longer, more specific* listings with explicit scope
statements and real price points. So the visible difference is **positioning quality**, not a
visible feature gap.

**What I cannot establish (UNVERIFIED).** Nothing on a store page explains why `Visual Schedules and
Social St` sits at 2.0★ with 50 ratings or why `Fun Routine - Autism` sits at 2.9★ with 1,410.
Candidate causes — crashes or Android compatibility, ad/subscription friction, UX, weak
localisation, review-bombing, or simply a job that is hard to do well — are **all invisible here**.
I make **no claim** that these apps lack any internal capability. The honest implication for the
factory: it **cannot claim a feature gap** in this niche, only a quality/execution gap that this
scan cannot verify exists.

---

## 5. The install-magnitude picture

The plant vertical's lesson was that a ratings-only screen misses a dominant install owner. It
repeats here, and in the same shape.

**Biggest install bucket anywhere in the returned sets: `100M+` — and it is a fuzzy match.**

| bucket | app | rating | reviews (exact) | where it appeared | in the job? |
| --- | --- | --- | --- | --- | --- |
| **100M+** | **Google Classroom** | 2.8 | 2,090,977 | rank 15 of 29 in `autism classroom visual schedule` — matched on "classroom" | **no** |
| 50M+ | Baby Panda's Daily Habits | 3.5 | 16,929 | `daily routine visual kids` | no |
| 10M+ | Me+ Lifestyle Routine | 4.7 | 862,000 | `daily routine visual kids` | no |
| 5M+ | TimeTune / HabitNow / Time Planner | 4.1–4.7 | 22,700–94,400 | `visual schedule for kids`, `daily routine visual kids` | no |
| 1M+ | **AutiSpark: Kids Autism Games** | 4.3 | 3,612 | 3 queries | adjacent (autism games) |
| 1M+ | Happy Kids Timer Chores · Visual Timer - Countdown · Mouse Timer | 4.2–4.8 | 2,440–24,800 | kids/visual-timer shelves | adjacent |
| **500K+** | **Autism Kids: Voice & Routines (Matraquinha)** | **4.9** | 1,407 | `autism routine app`, `visual timetable autism` | **direct** |
| 100K+ | Fun Routine - Autism | 2.9 | 1,409 | `visual schedule autism`, `autism routine app` | **direct** |
| 10K+ | Lil Planner: Visual Schedule · Visual Schedules and Social St · Thruday · Kids ToDo List | 2.0–4.8 | 50–2,470 | many | direct / adjacent |

**Yes, one app sits a whole bucket above the rest — and it is a good app, not a bad one.**
Inside the direct job the ladder is `500K+` (Matraquinha, 4.9★) → `100K+` (Fun Routine, 2.9★) →
`10K+` (Lil Planner 4.8★, Visual Schedules 2.0★, Thruday) → and then **13 of 17 direct apps at
≤ 1K+**, most of them unrated. There is no 1M+ or 10M+ app in the direct job at all.

**The target query is small.** On `visual schedule autism` — the exact query the slot is aimed at —
the **largest install bucket across all 30 visible cards is `100K+`** (`Fun Routine - Autism`), the
largest `direct` good app is `10K+` (`Lil Planner`), and 16 of 30 cards are unrated. The 500K+
leader only appears on the *other* phrasings. For comparison, the plant vertical's flat queries
returned owners at 10M+ and 50M+; **this job's ceiling is one to three buckets lower.**

---

## 6. What this scan cannot tell us

- **Install buckets are floors, not counts.** `500K+` means "at least 500,000"; `10K+` spans
  10,000–49,999. Matraquinha's real install base is unknown and could be far above its bucket.
- **Only visible cards were read, and the display caps out (~30).** Play's relevance order is not
  size order, so a bigger direct app can sit below the cut. On the target query only **30** cards
  were visible and the largest bucket in them was `100K+` — that reading rests on that one page.
- **Revenue, retention and conversion are invisible.** A declared IAP badge proves a model was
  *implemented*, not that it earns. Nothing here says what `RoutinePals`' $49.99/year converts at, or
  how much of Matraquinha's 500K+ is the AAC use case rather than the visual-routine use case.
- **Whether families would pay for THIS app is not a store-visible fact.**
- **Why the weak incumbents are weak is not established.** Section 4 is limited to listing text;
  no app was installed or used, and no review text was read. Whether their 2.0★/2.9★ is fixable by
  the factory is **UNVERIFIED**.
- **The 13-of-17 unrated tail is ambiguous.** It is equally consistent with "the niche is open" and
  with "people keep trying this and not getting traction". The store page cannot distinguish them,
  and that ambiguity is the central risk in this candidate.
- **One storefront, one signed-out session** (`hl=en`, `gl=US`) at the timestamp above. Autism
  provision, diagnosis rates, language and funding are strongly country-specific, so a US-English
  spot check may not represent other storefronts at all.
- **My on-topic classification is a human judgement**, stated per query and auditable above, not a
  computed fact. A different reviewer could move apps between "adjacent" and "off-topic" by a few
  cards (the `direct` counts would not move much). The sensor cannot reproduce it.
- **The sensor's relevance guard under-counts pollution when a query token is generic** (`visual`,
  `schedule`), which is exactly why Section 1 is hand-made.

## 7. A risk that is not a market question: this is a children's, health-adjacent app

Surfaced here rather than resolved, because it changes the cost of the slot.

Buildings for autistic children touch three surfaces that a trade calculator or a plant identifier
does not: **Play's Families / child-directed requirements** (target-audience declaration,
`Designed for Families`, content rating, restrictions on ads and on data collection from children);
**health and therapeutic claim exposure** (incumbent listings claim "Reduce meltdowns", "Stop
meltdowns" and "the #1 visual schedule app for kids, ADHD, and autism" — an app making
therapeutic-sounding claims about a diagnosed condition sits near Play's health/misleading-claims
surface and, in some jurisdictions, near regulation of autism-intervention marketing); and
**sensitive data** (a routine, behaviour log, emotion log, or photos of a child, alongside a
diagnosis, are sensitive or health data, with a higher bar for Data safety and deletion).

**Questions for the human, and policy findings now sourced separately:**

- The later policy read resolves the Play question that this market scan left open:
  **Families Policy Requirements** bind when any declared target age group includes children,
  while **Designed for Families** is opt-in. A parents/carers-only declaration is unsafe if the
  app or listing uses child-directed imagery or terminology, because Play can override it and a
  misdeclaration risks removal or suspension. **No ads is a low-risk product choice, not a
  prohibition**; ads must meet the Families ads regime. IAP is not prohibited on policy grounds,
  but must use Play Billing and avoid manipulative or confusing purchase tactics. Purchase consent
  remains a legal question. See
  [`2026-09-23-autism-app-policy.md`](2026-09-23-autism-app-policy.md) §§1, 3, and 4, including
  <https://support.google.com/googleplay/android-developer/answer/9893335> and
  <https://support.google.com/googleplay/android-developer/answer/9867159>.
- **UNVERIFIED:** whether the therapeutic-style wording used by incumbents ("reduce meltdowns") is
  permitted, requires qualification, or is itself a policy risk. The factory should not copy it on
  the strength of this scan.
- **UNVERIFIED:** whether any jurisdiction regulates app-delivered autism-intervention guidance.
- **UNVERIFIED:** whether the 17 direct incumbents carry data-safety declarations consistent with
  what they store. This scan read badges and listing text, never Data safety sections.

---

## Bottom line

**The original `candidate` read survives, and it is stronger than an 8-card glance deserved — but the
niche is smaller and more occupied than that glance implied.**

- **The verdict is real and reproducible.** 5 of 7 schedule phrasings return `candidate`
  (the other two `mixed`); on the exact query my own read is **30/30 on-topic**, so the earlier probe
  was not a fuzzy-match artefact; and all 17 of the sensor's calibration queries reproduced exactly,
  so the label is not noise. The sensor is not drifting.
- **But two queries in this set *were* polluted, exactly as warned.** `visual schedule app` is 23/30
  generic planners (its `fortress` verdict describes the planner shelf, including a 10M+ to-do app),
  and `autism visual timer` returned 12/12 generic timers with **zero** autism-specific apps (its
  `fortress` verdict describes the timer shelf, two of them at 1M+). Neither conclusion is used.
- **The niche is fragmented in installs but not unowned.** There is no dominant install owner, and
  the largest *direct* app is 500K+. But the direct cohort has a **4.9★ / 500K+ incumbent**
  (Matraquinha), a **4.8★ specialist** that ranks #1 on the target query and claims the "#1" position
  (Lil Planner), and two **well-built new entrants with real subscription pricing arriving Aug–Sep
  2026** (RoutinePals at $6.99/mo–$49.99/yr; Little Planner from the Little Timer team). Being
  "visual-schedule-specific" is not a differentiator here — the leaders already are.
- **The install ceiling is low.** On the target query the largest app in all 30 visible cards is
  `100K+`; the largest direct app anywhere in the run is `500K+`. One to three buckets below the
  plant vertical's flat queries.
- **The bad incumbents are not visibly feature-poor.** Both low-rated apps advertise substantial,
  on-job feature lists; their 2.0★/2.9★ is not explained by anything on the store page. The factory
  **cannot claim a feature gap** — only an execution gap it cannot verify from here.
- **Monetisation contradicts only half the factory default.** 15 of 17 direct apps declare no ads
  (consistent with "no ads to children"), but **10 of 17 declare IAP and every direct app with any
  traction does**, and the newest entrants shipped with paid tiers from day one. "Free with no ads
  **and** no charge" is the profile of the 7 zero-traction apps.
- **The sharpest structural fact:** **13 of 17 direct apps are unrated and at ≤ 1K+ installs.** That
  is either the opening or the graveyard, and a store page cannot tell the two apart.

**Plain recommendation.** I would not call this a kill, but I would not commit the slot on the
strength of the original read either. The evidence supports "**the sensor was right that supply is
inadequate**" and does **not** support "**the niche is empty or generic**". What the slot would
actually be is a bet to out-execute two already-good apps in a market whose observed install ceiling
is ~500K+ and whose nearest competitors are adding subscription pricing right now — with a higher
compliance cost than a utility slot because the users are children and the domain is a diagnosis.
If the factory's edge is execution quality rather than a missing feature, this is the kind of niche
that edge could win; if the two 2.0★/2.9★ incumbents are bad for a reason that is structural, the
factory will discover that after building. That distinction is **UNVERIFIED** here and is the
decision the human has to make.

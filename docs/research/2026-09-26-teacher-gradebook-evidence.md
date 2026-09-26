# Teacher gradebook — measured evidence (2026-09-26)

**What this file is.** The first-party measurement behind
`docs/ideation/2026-09-26-teacher-gradebook.md`. `AGENTS.md` and
`docs/ideation/README.md` require the per-niche evidence a plan cites to live in
`docs/research/`, and this is that file. It is not a build instruction and it creates no slot.

**How to read the labels.** MEASURED means a command ran in this session and this is what it
printed; the command is named. ON RECORD means a file or a cited source states it. INFERRED is
reasoning from the two above. UNVERIFIED is carried without evidence and is listed again in §7.
No number here was invented. Where a figure is absent it says it is absent.

---

## 1. What was run, and what it printed

All commands were run from the repository root on 2026-09-26 with network access.

| # | Command | Observed result |
|---|---|---|
| 1 | `node scripts/scan-market.mjs --query-set scripts/market-queries-2026-09-26.json --concurrency 4 --out docs/market-scan-2026-09-26.md` | 31 queries in 6,523 ms: **7 candidate, 14 mixed, 4 fortress, 6 dead-zone, 0 no-results, 0 errored**. Full table in `docs/market-scan-2026-09-26.md`. |
| 2 | `node scripts/scan-titles.mjs --query "teacher gradebook"` | `verdict=candidate guard=partial (16/30, share 0.53) visible=30`. Full title list in §2.1. |
| 3 | `node scripts/scan-titles.mjs --query "grade book" --query "teacher grade book"` | `grade book`: `mixed`, guard `partial` 18/30 = 0.60, 30 visible. `teacher grade book`: `mixed`, guard `partial` 19/30 = 0.63, 30 visible. Both lists read by hand in §2.1. |
| 4 | `node scripts/scan-titles.mjs --query "classroom gradebook"` | `verdict=candidate guard=partial (12/30, share 0.40) visible=30`. |
| 5 | `node scripts/enrich-market.mjs --query "teacher gradebook" --top 10 --concurrency 3` | 10 apps enriched, 0 failures, 4,830 ms. Table in §2.2. |
| 6 | `node scripts/enrich-market.mjs --query "homeschool gradebook" --top 8 --concurrency 3` | 8 apps enriched, 0 failures, 4,844 ms. Table in §5.2. |
| 7 | `node scripts/scan-complaints.mjs --package com.apolosoft.cuadernoprofesor --package com.edevolearning.edevoteacher --package com.apps.ips.teacheraidepro3 --package me.yanu.gradebook --top 6` | 12 reviews read, **6 critical**, 2 unclassified. Histogram and verbatim text in §2.3. |
| 8 | `node scripts/scan-titles.mjs --query "homeschool transcript"` | `verdict=mixed guard=on-topic (29/29, share 1.00) visible=29`. |
| 9 | `node scripts/scan-titles.mjs --query "pickleball scoreboard" --query "pickleball scoring app" --query "pickleball score keeper"` | `pickleball scoreboard`: `candidate`, 13 visible, 6 rated. `pickleball scoring app` and `pickleball score keeper`: **both `dead-zone`** over 30 visible results each. See §5.1. |
| 10 | `node scripts/enrich-market.mjs --query "football lineup" --top 8 --concurrency 3` | 8 apps enriched, 0 failures, 3,843 ms. Table in §5.3. |
| 11 | `node scripts/enrich-market.mjs --query "subscription tracker" --top 8 --concurrency 3` | 8 apps enriched, 0 failures, 4,662 ms. Table in §5.4. |
| 12 | `node scripts/enrich-market.mjs --query "youth sports lineup" --top 10 --concurrency 3` | 10 apps enriched, 0 failures, 4,476 ms. Table in §5.3. |
| 13 | `node scripts/scan-titles.mjs --query "scale model paint log"` | `candidate` at guard `partial` 17/30 = 0.57, but the on-topic incumbents are rated 4.6–4.9. See §5.5. |

Every sensor answered. No demand claim below is unmeasured because a sensor failed. One honest gap
is stated in §7: nothing in the toolchain reads revenue.

### 1.1 Why a new query set was needed

ON RECORD: `docs/research/2026-09-24-market-decision.md` §3 and `docs/niche-selection.md` §4 close
the following to re-proposal — `process server log`, `dog groomer appointment book`,
`field service work order app`, `stuttering practice app`, `dementia caregiver log`,
`farrier hoof record`, `livestock breeding record`, `machinist feeds and speeds`,
handloading data, plant-health diagnosis, visual-schedule-for-autism, and generators, converters
and trade calculators as a category. The set in `scripts/market-queries-2026-09-26.json` avoids all
of them and screens eight families the factory has never measured, with two calibration controls so
sensor drift is visible in the same run.

---

## 2. The teacher-gradebook shelf, measured

### 2.1 Titles read by hand

`node scripts/scan-titles.mjs --query "teacher gradebook"` returned 30 cards with the guard at
`partial` (16/30 = 0.53). Hand-classified by job:

| Job in the shelf | Count | The apps |
|---|---:|---|
| **Teacher-facing gradebook / classroom record** | **12/30** | `com.apolosoft.cuadernoprofesor` 3.6, `com.edevolearning.edevoteacher` 3.7, `com.apps.ips.teacheraidepro3` 4.1, `me.yanu.gradebook` 3.3, `com.gradetracker.jguttromson.graderelease` 4.4, `com.goperceptron.superteacher` 4.3, `com.hajipc.studentgrade` unrated, `com.headteacher.app` unrated, `com.rediker.teacherplusphone` 3.1, `com.eschool.gradebook` 2.8, `eduapptool.recordbookgenerator` unrated, `com.schoolzone.a36001` 2.9 |
| Student-facing GPA / grade tracker | 6/30 | `app.gradepro.mobile_app` 4.6, `com.romandc.gradecorner` 4.7, `com.bookschool`-class trackers, `com.NightDreamGames.Grade.ly` 3.9, `com.barebonesdev.powerplanner` 4.7 |
| LMS / SIS vendor app | 5/30 | `com.classdojo.android` 4.8, `com.gradelink.parent.google` 4.7, `com.gradescope.student` 1.2, `com.prmntr.teachassist` unrated, platform apps |
| Exam-prep or children's reading mislabelled by the word *grade* | 7/30 | `com.edurev.grade6` 3.4, `com.skc.grade3.*` 4.3, and five more |

So the guard's `partial` flag is correct and the addressable shelf is **twelve apps**, not thirty.
The `grade book` and `teacher grade book` phrasings return the same twelve plus more children's
reading apps, which is why both are `mixed` rather than `candidate`: the job share is diluted by
the word *grade*, and the guard is reading the word, not the job. This is the homonym failure mode
recorded in `docs/research/2026-09-24-market-decision.md` §2.1, on a different word.

### 2.2 Per-app facts

MEASURED, `node scripts/enrich-market.mjs --query "teacher gradebook" --top 10 --concurrency 3`:

| App | Rating | Reviews (exact) | Installs | Ads | IAP | Price |
|---|---:|---:|---|---|---|---|
| `com.apolosoft.cuadernoprofesor` Teacher Gradebook | **3.6** | **4,510** | **500K+** | false | **true** | free |
| `com.edevolearning.edevoteacher` Smart Teacher: Class Gradebook | **3.7** | 370 | 50K+ | false | **true** | free |
| `com.apps.ips.teacheraidepro3` Teacher Aide Pro | 4.1 | 896 | 100K+ | false | **true** | free |
| `me.yanu.gradebook` Gradebook | **3.3** | 21 | 1K+ | false | false | free |
| `com.gradetracker.jguttromson.graderelease` Grade Pal | 4.4 | 182 | 10K+ | false | false | free |
| `com.hajipc.studentgrade` Student Gradebook for Teachers | unrated | not shown | 100+ | false | false | free |
| `com.bookrschule.edu` BOOKR Schule | unrated | not shown | 10K+ | false | **true** | free |
| `app.gradepro.mobile_app` GradePro | 4.6 | 384 | 10K+ | true | true | free |
| `com.romandc.gradecorner` Grade Corner | 4.7 | 1,740 | 10K+ | false | true | free |
| `com.shedeurapps.grader` Grade-R | unrated | not shown | 1K+ | false | false | free |

Three facts decide this shelf:

1. **Demand is proven at scale, and the leaders are weak.** The three on-job apps with real
   volume are rated **3.6 (500K+), 3.7 (50K+) and 4.1 (100K+)**. That is a rated-weak pocket
   roughly two orders of magnitude larger than the pocket in
   `docs/ideation/2026-09-26-five-recipes.md` §3.1, whose largest dedicated logger carries a
   `10K+` lifetime bucket.
2. **The money model is already in-app purchase, not ads.** MEASURED: ads declared on 1 of 10,
   IAP declared on **6 of 10**, price `free` on all ten. The category has chosen "free app, sell
   inside it", and `docs/monetization-strategy.md` §3.6 makes a purchase the leading hypothesis.
3. **The offline-ownership positioning already sells.** The leader's own store description
   (fetched 2026-09-26, `com.apolosoft.cuadernoprofesor`) markets *"Private, offline, and under
   your control… stored encrypted on your device… Student information is not stored on the
   developer's servers… alongside your SIS… does not replace or automatically sync with your
   SIS."* A 500K+ install base on exactly the shape this factory builds is the strongest single
   datum in this file.

### 2.3 The complaints, verbatim

MEASURED, `node scripts/scan-complaints.mjs --package … --top 6`, 12 reviews read, **6 critical**,
classified: `paywall` 2 (33 % of critical), `broken` 1, `missing-feature` 1, `notifications` 1,
unclassified 2.

The leader, `com.apolosoft.cuadernoprofesor` (3.6★, 500K+):

> 1★ "As developer don't read, and rude. As seen no problem paying, not continuous though.
> Offering cloud storage, or anything that can justify paying monthly for!? Using Google for a
> free email instead of paying for business one, Nope! Another developer introducing pointless
> subscription service to bleed money from their users. **If anything add one time purchase.**
> Have something that requires subscription? NOPE!"

The developer's own reply, in the same thread, is a gift: *"You will see the trend on all the
applications or services is the suscription, one time lifetime payment for apps is reaching the
end of life."* The incumbent is telling 500K+ installed users that a one-time purchase is over.
That is a stated opening, not an inference.

> 3★ "Has great potential but for now it's **one error after the other with sometimes a forced
> close** too. Not for daily use, at least not for me."

The 100K+ incumbent, `com.apps.ips.teacheraidepro3` (4.1★):

> 1★ "In the 'manage students and classes' section the only editing you can do for classes is
> change the name. **No option to add or delete classes.** I've searched every corner of this app
> and nothing."

> 3★ "I purchased this app from the Amazon app store in 2014 and I LOVE using it, but since that
> app store is no longer available on Android devices, I no longer get updates. **I'm scared to
> uninstall and reinstall it from the Play Store because I don't want to lose the paid version I
> bought.**"

`me.yanu.gradebook` (3.3★, 1K+):

> 3★ "Cannot delete subject or grades. Says deleted but reappeared."

Every clause of the distinctness sentence in the proposal answers one of these, and none of them
is a taste complaint: they are missing create/delete, a crash on daily use, a lost purchase, and a
refusal to sell a one-time unlock.

---

## 3. Does this audience pay? Measured market facts

| Claim | Value | Label |
|---|---|---|
| US public K-12 classroom teachers, 2025-26 | **3,232,905** | ON RECORD — NEA *Rankings of the States 2025 and Estimates of School Statistics 2026*: <https://www.nea.org/sites/default/files/2026-08/2026-rankings-and-estimates-report-august-update.pdf> |
| Classroom teachers who personally fund some part of their work | **91 %** | ON RECORD — 4pt0 survey of 1,000 educators: <https://www.4pt0.org/post/the-self-funded-classroom> |
| Educators who spent their own money on tools, tech, apps or resources the school did not provide | **77.9 %** | ON RECORD — same survey; the highest concentration of teacher spending is the $101–$500 band (54.5 % of classroom teachers) |
| Average out-of-pocket classroom spend, 2025-26 | **≈ $600** | ON RECORD — DonorsChoose 2026 Teacher Survey, n = 2,500: <https://www.globenewswire.com/news-release/2026/08/18/3346894/0/en/donorschoose-survey-of-2-500-public-school-teachers-shows-what-it-takes-to-prepare-a-classroom-and-the-real-cost-educators-shoulder.html> |
| Teachers who keep a parallel private record because the official system is slow, rigid or loses data | not a number; **the behaviour is measured in review and forum text** | ON RECORD — r/Teachers and r/ElementaryTeachers threads (see §3.1); this is qualitative evidence of a job, not a market size |

### 3.1 The behaviour, in the users' words

ON RECORD, public teacher threads read 2026-09-26:

> "Every teacher I've spoken to in my country (Colombia) admits they have some kind of personal
> tracking system running alongside whatever platform the school officially uses — usually Excel,
> sometimes Google Sheets, occasionally just a physical notebook. The reasons vary: the official
> system doesn't support grade curves, can't auto-zero missing work cleanly, **loses data when the
> session times out**, or just takes too many clicks…" — r/ElementaryTeachers

> "I print off a blank spreadsheet of each class to collect grades and then transfer them to my
> digital grade book every week. That's how I've been doing it for 20 years." — r/Teachers

> "…there have been several times that students have been transferred from one of my classes to
> another mid quarter and **ALL THEIR PREVIOUS GRADES have been wiped out** of the online grade
> book. Thank goodness for my paper grade book. **You can pry it from my cold dead hands.**" —
> r/Teachers

> "About five years teaching third grade and I never kept a grade book. It was just one extra
> step…" — r/ElementaryTeachers

The last quote is the counter-evidence and it is kept on purpose: **part of the audience does not
want a second gradebook at all.** The addressable user is the teacher who already keeps a parallel
record, not every teacher.

---

## 4. The money arithmetic, with the division shown

Assumption set, ON RECORD in `docs/monetization-strategy.md` §2.2 as assumptions to re-check, not
measurements: `F_billing = 0.15`, `F_refund = 0.02`, `F_fx = 0.02`. `F_fraud` is excluded because
the unlock is a local entitlement Play confirms before the slot flips it, so there is no
granted-then-unpaid unit.

```
net multiplier = (1 − 0.15)(1 − 0.02)(1 − 0.02) = 0.85 × 0.98 × 0.98 = 0.8163
net per sale at $6.99 = 6.99 × 0.8163 = $5.7059 → $5.71
sales for $1,000/month = 1000 / 5.71 = 175.1 → 176 sales/month  (≈ 5.9/day)
```

Conversion is the largest unknown. The factory's own documents carried a 2 % assumption marked
UNVERIFIED. That number now has external support, so it is upgraded from UNVERIFIED to ON RECORD
and the plan uses the lower figure:

- ON RECORD: median install-to-paid across subscription apps is **1.9 % by day 35**, top decile
  8.5 % — RevenueCat *State of Subscription Apps 2025* and Adapty *State of In-App Subscriptions
  2026*, quoted at <https://www.airbridge.io/en/blog/why-subscription-app-get-installs-no-paid-subscribers>.
- ON RECORD: Apple Ads install-to-paid averages **1.92 %** across 1 M+ ad groups; US Productivity
  and Utilities convert at 5.36 % for established apps — Adapty 2026:
  <https://adapty.io/blog/apple-ads-install-to-paid-rate-benchmarks/>.
- ON RECORD: UXCam retail 1.38 %, travel 2.41 %: <https://uxcam.com/blog/mobile-app-conversion-rate/>.

Ladder at 1.9 %:

| Installs / month | Sales / month | Revenue / month |
|---:|---:|---:|
| 500 | 9.5 | $54 |
| 1,000 | 19 | $108 |
| 3,000 | 57 | $325 |
| 5,000 | 95 | $542 |
| 9,250 | 176 | $1,004 |

Price sensitivity, same factors, `sales = 1000 / (price × 0.8163)` and
`installs = sales / 0.019`:

| Price | Net / sale | Sales for $1,000 | Installs / month needed |
|---:|---:|---:|---:|
| $4.99 | $4.07 | 246 | 12,947 |
| **$6.99** | **$5.71** | **176** | **9,242** |
| $9.99 | $8.16 | 123 | 6,447 |

### 4.1 What the ladder is being compared against

The leader holds a `500K+` **lifetime** install bucket and 4,510 reviews with review text dating
from 2019, so its average pace over roughly a decade is on the order of a few thousand installs a
month (INFERRED; the storefront does not publish per-month installs, and this calc is a bound, not
a measurement). The honest reading:

- **$100–$550/month is the plausible band at leader-scale install flow.**
- **$1,000/month requires roughly 9,200 installs a month, which is about the leader's lifetime
  pace or better, against an incumbent that is 3.6★ and crashes.**

This is the same ceiling class as every recipe in `docs/ideation/2026-09-26-five-recipes.md`, but
on demand evidence that is one to two orders of magnitude stronger. The proposal says that out
loud rather than dressing it up.

### 4.2 Why the ads alternative is worse here, in numbers

ON RECORD, `docs/monetization-strategy.md` §2.1: reaching $1,000/month needs roughly 700–1,000 DAU
rewarded-centric, 2,000–6,000 DAU with interstitials and banners, or 11,000+ DAU banner-only.
Per MAF and Playwire benchmarks, US Android rewarded video is ~$14–$16.49 eCPM and US Android
banner ~$0.68 eCPM. Two reasons ads are not the lead here: a screen that shows student names next
to advertising is a poor product and a Families-policy question, and the shelf has already chosen
IAP (6 of 10 declared) with essentially no ads (1 of 10).

---

## 5. Alternatives measured, and why they lose

### 5.1 Pickleball scoring — a real trend with a dead-zone shelf (rejected)

ON RECORD: 24.3 M Americans played pickleball in 2025, up 22.8 % year over year and 479 % since
2020, with 7.5 M core players — SFIA 2026 Pickleball Single Sport Report,
<https://sfia.org/research/u-s-pickleball-participation/>. The demand is not in question.

MEASURED, the shelf is: `pickleball scoreboard` → `candidate` over only 13 visible results with 6
rated; `pickleball scoring app` → **`dead-zone`** over 30 visible; `pickleball score keeper` →
**`dead-zone`** over 30 visible. In the two 30-card shelves, roughly twenty apps are **unrated**,
and the few that carry a rating are 4.2–4.8 (Pickleheads 4.8, PicklePip 4.8, Score Pickleball 4.4,
Pickleball Pro 4.4). Per `docs/niche-selection.md`, unrated competitors are evidence of failure,
not vacancy: many people shipped a pickleball scorer and none of them reached the install base a
rating requires. A growing sport does not make a market for an app that the players do not adopt.
**Rejected on the factory's own rule, not on taste.**

### 5.2 Homeschool record and transcript — right shape, 50× smaller (kept as alternative)

MEASURED: `homeschool transcript` returns `verdict=mixed guard=on-topic (29/29, share 1.00)` —
the cleanest relevance of anything screened in this run — with genuinely weak incumbents:
`com.restvo.app` 2.4, `com.homeschoolplanner` 2.6, `org.powerhomeschool.acellus` 2.8,
`com.hsppro.app` 3.0, `com.homeschoolplanet.homeschoolplanet` 3.7. But the largest install bucket
in the enriched set is `10K+` (`com.hsppro.app`), and MEASURED IAP on 5 of 8.

**Why it is the alternative and not the recommendation:** the same shape as the teacher app on a
market roughly fifty times smaller by installs, so the same arithmetic needs far more share to
reach the same revenue. It also has a strong, non-arithmetic job — hours, portfolio and transcript
— which is a real consolation if the owner wants out of the record-and-average category.

### 5.3 Coach lineup tools — bigger market, weaker proof of payment (kept as alternative)

MEASURED, `football lineup` enriched: `com.hardgrnd.lineup11` **3.7★, 26,700 reviews, `5M+`
installs, ads yes, IAP no, free**; `com.nickmichael.footballlineupmanager` 3.6★, 268 reviews,
`100K+`, IAP; `com.alicious.lineup` 4.1★, 678 reviews, `100K+`, ads + IAP;
`com.nicomoyano.lineupbuilder` 4.9★, 826 reviews, `50K+`, IAP. MEASURED, `youth sports lineup`:
TeamSnap 2.9★, 30,100 reviews, `5M+`; sportsYou 4.0★, 1,580 reviews, `1M+`.

This is a **larger weak pocket than the teacher shelf**, and it is arithmetic-free, which matters
against `.cursor/rules/ideation-gate.mdc`. It loses on one measured fact: the `5M+` leader is
**ad-only with no in-app purchase**, and the shelf is dense with unrated churn apps, so
willingness to pay at the top is unproven. TeamSnap's `2.9★` is also the wrong kind of weakness —
a coach cannot switch a team tool the league chose, which is the "loudest pain is not the
reachable customer" caution recorded in `docs/research/2026-09-24-market-decision.md` §2.

### 5.4 Subscription trackers — demand proven, shape needs a server (rejected)

MEASURED: `com.truebill` Rocket Money **4.5★, 139,000 reviews, `10M+`**, IAP; `digital.paddy.subs`
4.4★, 7,220 reviews, `100K+`; `app.SubscriptionStopper` **3.9★, 3,650 reviews, `100K+`**;
`com.merkost.suby` **3.8★, 102 reviews, `5K+`**; `com.pavelrekun.tilla` 4.5★, 938 reviews,
`50K+`, **no IAP**. ON RECORD, a third-party estimate puts Rocket Money at ~$600k/month (Peekly,
August 2026) and the PFM/subscription-tracking market at $9.5 B in 2026.

**Rejected for this factory**, not for the market: the value that makes Rocket Money worth money is
linking a bank to find subscriptions automatically, which is a server, an aggregator contract and a
financial-data posture. The offline manual tracker competes with a free-bank-linked product and
with `50K+`-install free apps, so its price is not defensible.

### 5.5 Miniature and scale-model paint — the pocket is already well served (rejected)

MEASURED, `scale model paint log` is `candidate` at guard `partial` 17/30 = 0.57, but the guard is
reading the word *paint*: most of the shelf is house-painting visualisers. The on-job apps are
rated **4.6–4.9** (`com.paintvault.app` 4.8, `de.game_coding.trackmytime` 4.9,
`com.courageousoctopus.paintrack` 4.6, `com.pulgadas.hobbycolorconverter` 4.7). Demand is proven
and the supply is already good, which is the definition of a fortress pocket to skip.

---

## 6. Macro base rates, so the ladder is read in context

| Claim | Value | Label |
|---|---|---|
| Apps that never reach $1,000/month | **over 80 %** | ON RECORD — Trend Apps, ~58,000 apps: <https://trendapps.dev/blog/trends/how-much-money-apps-make-2026/> |
| Median active app revenue | **under $50/month** | ON RECORD — same source |
| Apps tracked that clear $1,000/month | **18.3 %** | ON RECORD — <https://trendapps.dev/statistics/> |
| Free apps with IAP that clear $1,000/month | **47.1 %** | ON RECORD — same source |
| **Paid apps without IAP that clear $1,000/month** | **2.4 %** | ON RECORD — same source |
| Median install-to-paid | **1.9 %** (top decile 8.5 %) | ON RECORD — RevenueCat 2025 / Adapty 2026, via Airbridge |
| Google Play consumer spend, 2025 | **$49.2 B**, of which $19.2 B non-games | ON RECORD — Business of Apps: <https://www.businessofapps.com/data/google-play-statistics/> |

The decision-relevant line is the 47.1 % versus 2.4 % split: a **free app that sells inside it**
is roughly twenty times more likely to clear $1,000/month than a **paid app that does not**. That
is why this proposal is `purchase_led_single_unlock` — free to install, one one-time unlock —
and why the factory should not read its own `purchase_only_no_ads` archetype as a money plan.

---

## 7. Everything I could not verify

1. **No revenue is observable.** Neither the factory's sensors nor this document can read what any
   app earns. Install buckets are Play's rounded bands and a bucket is a bound, not a point.
2. **Install-to-paid is external, not this account's.** 1.9 % is the published median for other
   developers' apps. This account has never measured a sale
   (`docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §6), so the ladder is a bound.
3. **Reachability of ranking is unmeasured.** Nothing here proves a new listing can rank for
   `teacher gradebook` against an incumbent with 4,510 reviews, nor what ASO effort that takes.
4. **Two unrated on-job competitors were not sized.** `com.hajipc.studentgrade` (100+) and
   `eduapptool.recordbookgenerator` have no rating and I did not open their listings beyond the
   enriched set, so "the weak pocket is 12 apps" is a shelf reading, not a census.
5. **The complaint sample is six critical reviews across four apps.** That is a vocabulary, not a
   rate. It shows these failure modes exist in the pocket; it does not rank their frequency.
6. **Whether the leader is actually declining is unmeasured.** Its visible rating is 3.6 today and
   one review quotes 4.7 historically, but I did not measure a rating trend.
7. **IAP price points inside the incumbents are not visible** from the storefront fetch, so this
   proposal's `$6.99` is justified by the audience's out-of-pocket behaviour (§3), not by a
   measured competitor price.
8. **Whether Play treats a teacher app that stores student names as child-directed**, and
   therefore whether the Families policy applies, is UNVERIFIED and must be settled against the
   live policy text before the store pack is written.
9. **Whether this account must run the 12-tester / 14-day closed test** remains unconfirmed; ON
   RECORD it is open in `docs/monetization-strategy.md` §1.1 and both market documents.
10. **The Football lineup `5M+` leader's install bucket is a lifetime band**, so "bigger market"
    in §5.3 is a floor comparison against the teacher shelf's `500K+`.
11. **The Reddit quotes in §3.1 are public forum text**, not a survey; they are evidence that the
    behaviour exists, not evidence of how many teachers do it.

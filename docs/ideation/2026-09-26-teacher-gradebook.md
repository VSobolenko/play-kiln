# Teacher gradebook — ideation proposal, 2026-09-26

**What this is.** One recipe, fully specified, for the owner to accept or refuse. It is a proposal,
not a slot: nothing is scaffolded until the owner picks it, and `scripts/new-slot.mjs` will refuse
to run without the validated plan beside this file.

**What this is not.** It is not a calculator, a converter, or an arithmetic tool: the product is a
**record system for a profession**, and §11 states the rule risk this creates honestly rather than
hiding it. It is not a re-proposal of anything on the killed list in
`docs/research/2026-09-24-market-decision.md` §3 or in the dead zones of `docs/niche-selection.md`.

**Every claim carries a label** — MEASURED (a command ran in this session and the command is
named), ON RECORD (a file or a cited public source states it), INFERRED (reasoning from those two),
UNVERIFIED (carried without evidence, listed again in §10). The full measurement record is
`docs/research/2026-09-26-teacher-gradebook-evidence.md`, which is what the plan cites.

---

## 1. The recommendation in one paragraph

Build **a private, offline gradebook that a teacher owns outright**: free to install, one one-time
unlock, no account, no cloud, no ads. The category has proven demand two orders of magnitude larger
than the pocket the factory was previously considering — a **`500K+`-install incumbent rated
`3.6`** with **4,510 reviews** — and the incumbents' own users write the wedge in their reviews:
they cannot add or delete a class, the app crashes in daily use, a purchase was lost with a dead
store, and they explicitly demand a **one-time purchase instead of a subscription**. The paying
audience is measured: **3,232,905 US K-12 teachers**, **91 % of classroom teachers personally fund
their work**, **≈ $600/teacher/year out of pocket**. The honest ceiling is stated in §5.3:
**$100–$550 a month at leader-scale install flow**, with **$1,000 a month requiring about 9,200
installs a month**.

---

## 2. Category and job

**Category.** Education / professional record-keeping.
**Job, one sentence.** *Keep the teacher's own per-student record — classes, students, assignments
and grades — on the device, so the teacher can grade their way, see a running average, and hand a
file to the school's official system.*

Distinct from every slot the factory holds: `pipe-sizing` is a trades workflow, `smoke` and
`pipe-proof` are pipeline proofs. Nothing in the catalogue is education, so the Repetitive Content
concern (`docs/monetization-strategy.md` §1.2) does not arise inside our own catalogue.

**Why this job is real, in the users' own words** (ON RECORD, public teacher threads read
2026-09-26, quoted in the evidence file §3.1):

> "Every teacher I've spoken to… admits they have some kind of personal tracking system running
> alongside whatever platform the school officially uses — usually Excel, sometimes Google Sheets,
> occasionally just a physical notebook."

> "…ALL THEIR PREVIOUS GRADES have been wiped out of the online grade book. Thank goodness for my
> paper grade book. **You can pry it from my cold dead hands.**"

And the counter-evidence, kept on purpose: another teacher says *"I never kept a grade book. It was
just one extra step."* The addressable user is the teacher who already keeps a parallel record, not
every teacher.

---

## 3. The paying user, the unit, the price, the moment

| | |
|---|---|
| **Paying user** | A K-12 or college teacher who already keeps a private paper or spreadsheet record beside the school's official SIS, and who funds classroom tools from their own pocket. |
| **What is bought** | A **one-time unlock** for unlimited classes and students, weighted categories, CSV and PDF export, and an owner-held backup file. Free tier: one class, up to 20 students, unlimited grades. |
| **Price** | **$6.99** (see §5.2 for sensitivity at $4.99 and $9.99). |
| **Moment of payment** | When a teacher creates a second class or exports the first report — after the free gradebook has been used for real work, and only then. |
| **Archetype** | `purchase_led_single_unlock` |
| **Capability required** | `billing` (the gate enforces this pairing at verify time) |

The price is not a guess: ON RECORD, the measured audience spends **≈ $600 out of pocket per year**
(DonorsChoose 2026, n = 2,500) and **77.9 %** of educators buy tools their school does not provide
(4pt0, n = 1,000). $6.99 is roughly one percent of that annual spend, once.

---

## 4. The wedge — every clause is a complaint a real user wrote

| # | What the shipped app does | The measured complaint it answers |
|---|---|---|
| 1 | Every class, student, assignment and grade can be **added, edited and deleted** | 1★ on `com.apps.ips.teacheraidepro3` (100K+): *"the only editing you can do for classes is change the name. **No option to add or delete classes.**"* |
| 2 | Grades and subjects delete **and stay deleted** | 3★ on `me.yanu.gradebook`: *"Cannot delete subject or grades. **Says deleted but reappeared.**"* |
| 3 | Does not **crash in daily use** | 3★ on the 500K+ leader: *"it's **one error after the other with sometimes a forced close** too. Not for daily use."* |
| 4 | Bought **once**, not rented | 1★ on the 500K+ leader: *"**If anything add one time purchase.** Have something that requires subscription? NOPE!"* — and the developer's reply: *"one time lifetime payment for apps is reaching the end of life."* |
| 5 | The purchase is **recoverable** through Play, and the **data is a file the teacher holds** | 3★ on `com.apps.ips.teacheraidepro3`: *"I purchased this app from the Amazon app store in 2014… **I'm scared to uninstall and reinstall it from the Play Store because I don't want to lose the paid version I bought.**"* |
| 6 | **No account, no cloud, nothing transmitted** | The leader markets exactly this and holds 500K+ installs on it (evidence file §2.2, item 3) — it is a proven-selling positioning, not a differentiator we invented. |

**Distinctness sentence (the §1.2 sentence, in one line):** *an offline gradebook a teacher owns
outright — every class, student and grade can be added, edited and deleted without a crash, the
whole record backs up and restores from the teacher's own storage, and it is bought once instead of
rented monthly.*

---

## 5. The money

### 5.1 Unit arithmetic, division shown

Deductions are ON RECORD as **assumptions** in `docs/monetization-strategy.md` §2.2, not
measurements: `F_billing = 0.15`, `F_refund = 0.02`, `F_fx = 0.02`. `F_fraud` is excluded because
the unlock is a local entitlement that Play confirms before the slot flips it, so there is no
granted-then-unpaid unit (the same reasoning as `docs/ideation/2026-09-26-five-recipes.md` §3.1).

```
net multiplier = (1 − 0.15)(1 − 0.02)(1 − 0.02)
               = 0.85 × 0.98 × 0.98
               = 0.8163

net per sale   = $6.99 × 0.8163 = $5.7059 → $5.71

sales for $1,000/month = 1000 / 5.71 = 175.1 → 176 sales/month (≈ 5.9/day)
```

### 5.2 Conversion, and the ladder at 1.9 %

The factory previously carried **2 %** marked UNVERIFIED. The published median is lower, so the
plan uses the lower number and upgrades the label: ON RECORD, median install-to-paid is **1.9 % by
day 35** (RevenueCat *State of Subscription Apps 2025* / Adapty *State of In-App Subscriptions
2026*, quoted at <https://www.airbridge.io/en/blog/why-subscription-app-get-installs-no-paid-subscribers>),
Apple Ads averages **1.92 %**, and US Productivity/Utilities established apps convert at **5.36 %**
(<https://adapty.io/blog/apple-ads-install-to-paid-rate-benchmarks/>).

| Installs / month | Sales / month | Revenue / month |
|---:|---:|---:|
| 500 | 9.5 | $54 |
| 1,000 | 19 | $108 |
| 3,000 | 57 | $325 |
| 5,000 | 95 | $542 |
| 9,250 | 176 | $1,004 |

Price sensitivity at the same conversion:

| Price | Net / sale | Sales for $1,000 | Installs / month needed |
|---:|---:|---:|---:|
| $4.99 | $4.07 | 246 | 12,947 |
| **$6.99** | **$5.71** | **176** | **9,242** |
| $9.99 | $8.16 | 123 | 6,447 |

### 5.3 What that ladder means against this shelf, said plainly

MEASURED: the leader, `com.apolosoft.cuadernoprofesor`, holds a **`500K+` lifetime install bucket
and 4,510 reviews**, with review text dating from 2019. Its average pace over roughly a decade is
therefore on the order of a few thousand installs a month (INFERRED — the storefront publishes no
per-month figure, so this is a bound, not a measurement).

- **Realistic band: $100–$550 a month**, which is one-time-unlock revenue at leader-scale install
  flow.
- **$1,000 a month requires ≈ 9,200 installs a month** — about the leader's own lifetime pace or
  better, against an incumbent that is 3.6★ and crashes.
- **This is not a "$1,000-a-month slot."** It is the best demand evidence the factory has ever had
  (two orders of magnitude more install volume than the amateur-radio recipe), on the same revenue
  ceiling. Saying so is the point of the ideation gate.

**Why ads are not the alternative here.** ON RECORD (`docs/monetization-strategy.md` §2.1),
$1,000/month needs ~700–1,000 DAU rewarded-centric or ~2,000–6,000 DAU interstitial+banner. A
screen showing student names beside advertising is a bad product and a Families-policy question,
and MEASURED, this shelf has already chosen IAP: **6 of 10 enriched apps declare IAP, 1 of 10
declares ads**, all free. The category's model is our model.

**The cheap multiplier that is genuinely available.** The leader is a **Spanish-language product**
(`Cuaderno del Profesor`) that ranks first for the English query `teacher gradebook`. Shipping
`en-US` plus `es-ES` costs build time, not a server, and reaches the market where this shelf
already has `500K+` installs. It is the only lever in this proposal that raises the ladder without
raising the price or the conversion rate.

---

## 6. Marketing and discovery

- **Search intent is proven by the incumbents**: `teacher gradebook`, `grade book`,
  `teacher grade book`, `classroom gradebook` all return the same twelve on-job apps, and the
  leader carries 4,510 reviews — the query set has real volume.
- **ASO terms to cover in title, short description and full description**: `gradebook`,
  `grade book` (the US term), `mark book` (the UK/AU/IE/IN term), `teacher planner`, `seating
  chart`, `attendance`, `student records`, `CSV export`, `PDF report`.
- **Title proposal**: **"Mark Book for Teachers"** (21 characters; Play's limit is 30). The exact
  collision check was run this session: `node scripts/scan-titles.mjs --query "markbook"` returns
  no app titled *Markbook*, only the adjacent `com.oxiionglobal.markme.teacher` **MarkSheet:
  Teacher App** (unrated) and `com.kokomasoft.marknote` MarkNote (unrated). The name deliberately
  does **not** reuse the leader's title — the measured name collision that sank Recipe 3 in
  `docs/ideation/2026-09-26-five-recipes.md` §3.3 is the reason, and the check must be re-run at
  the store-listing step.
- **Channels**: r/Teachers and r/ElementaryTeachers (the threads quoted in §2 are where this user
  already talks), state and subject-matter Facebook groups, teacher TikTok, and school-level
  word of mouth. **Caution:** promotion inside district networks must not be automated or spammy;
  `docs/monetization-strategy.md` §1.2 treats spam enforcement as an account-level risk.
- **Why not just paper or a spreadsheet** — the three reasons, each measured: a paper book cannot
  produce a CSV the teacher pastes into the SIS, cannot restore a wiped class mid-quarter, and
  cannot show a running weighted average per student; and the leader's own behaviour (selling a
  subscription for a static tool) is what turned a paying user into a 1★ review.

---

## 7. Timing

There is **no external trend** to ride, and inventing one would be dishonest. The timing fact that
is real is **incumbent decay**, and it is measured: the leader is 3.6★ with users describing
crashes, its developer publicly states that one-time purchases are "reaching the end of life"
while a reviewer asks for exactly one, and the 100K+ alternative cannot add or delete a class. A
decaying incumbent is a quality opening, not a market shift, and this section labels it as exactly
that.

The one timing risk against starting any slot: ON RECORD, personal Play Console accounts created
after 2023-11-13 must run a closed test with ≥12 testers for 14 days before production access
(`docs/monetization-strategy.md` §1.1). Whether it applies to this account is **UNVERIFIED**
(§10); it decides whether this slot can earn 7 days after building or 14 days plus review.

---

## 8. Build

| | |
|---|---|
| **Estimate** | **4–6 agent hours** to a verified slot |
| **Permissions** | **None.** Roster import and CSV/PDF export go through the system file picker (SAF) and the Share sheet; backup is a file the teacher chooses. No camera, no storage permission, no location, no network. |
| **Capabilities** | `billing` |
| **Server** | **None.** This is the product, not a limitation: the leader's own listing sells "student information is not stored on the developer's servers" and it has 500K+ installs. |
| **Offline** | Works fully offline by construction; the app never requests the INTERNET permission. |

**Screens.** Class list; class detail (roster + assignment columns); grade entry with a grid that
does not fight the keyboard; student detail (running average, per-category breakdown, notes);
category/weight editor; report view; export (CSV, PDF) and backup/restore via the file picker;
settings (rounding, grade scale, optional app lock); one paywall sheet.

**Domain work.** Weighted-average engine with explicit rounding rules; CSV roster import that
tolerates the SIS exports teachers actually get; PDF report without a heavyweight library;
schema-versioned backup file so a one-way migration is never needed.

**Why 4–6 hours and not 2.** The recorded first slot took **4 h 49 m** from scaffold to last build
for a *simpler* app (`docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §1, a lower bound).
A spreadsheet-like entry grid, two importers/exporters and a paywall are more code than that app
had. The one-hour target in `AGENTS.md` is a direction, and this proposal does not pretend to meet
it; the concrete cuts that would are in `.cursor/rules/parallel-execution.mdc`.

---

## 9. Kill criteria, and the cheapest way to be proved wrong

**Kill criteria (three finite numbers):** **60 days, fewer than 1,500 installs, or less than $100
revenue.**

The floors are deliberately higher than the α-recipes' floors because the measured shelf is two
orders of magnitude larger: 1,500 installs in 60 days is ~750/month, well below the leader's
lifetime pace in a category that has proven it installs at 500K+ scale. Revenue below $100 over
60 days means the unlock is not converting in a category where 6 of 10 apps already sell in-app,
and that is a demand verdict, not an ASO delay.

**Cheapest early proof, before any code.** Put a two-screen prototype in front of ten teachers
(one r/Teachers post plus one message to a subject Facebook group) and ask a single question:
*do you currently keep your own gradebook alongside the school system, and what does the school
system fail at?* If fewer than **five of ten** already keep a parallel record, the addressable user
is narrower than §2 claims and the recipe dies for the price of one post — which is the same
two-question test the α-recipes use, applied to the strongest shelf this factory has measured.

---

## 10. Honest unknowns

1. **Install-to-paid is external.** 1.9 % is the published median for other developers' apps; this
   account has never measured a sale.
2. **Ranking reachability is unmeasured.** Nothing here proves a new listing can rank for
   `teacher gradebook` against 4,510 reviews, nor how much ASO work that takes.
3. **Whether the closed-test gate applies** to this account is unconfirmed (see §7).
4. **IAP price points inside the incumbents are not visible** from the storefront, so $6.99 is
   justified by the audience's spending behaviour, not by a measured competitor price.
5. **Two unrated on-job competitors were not sized** (`com.hajipc.studentgrade` 100+,
   `eduapptool.recordbookgenerator`), so "the pocket is twelve apps" is a shelf reading, not a
   census.
6. **Student-data policy posture is unresolved.** The app stores children's names and grades
   locally and transmits nothing, but whether Play treats the listing as child-directed — and
   therefore whether the Families Policy Requirements apply — must be settled against the live
   policy text before the store pack is written. This is a compliance question, not a demand
   question, and it must be answered before the description is drafted.
7. **The complaint sample is six critical reviews across four apps** — a vocabulary of failure
   modes, not a ranking of their frequency.
8. **The name is a proposal.** The collision check found no exact title match, but the listing
   review is the real test and must be re-run at store-pack time.

---

## 11. The rule risk, stated rather than hidden

`.cursor/rules/ideation-gate.mdc` closes the factory's first topic: *"Do not propose calculators,
unit converters, generic arithmetic tools, or another app built around summing or converting
numbers a user types in."* A gradebook computes weighted averages, so this proposal must answer
that rule directly instead of reading past it.

**The answer, and its limits.** The rule was written after an example the owner gave — a small
offline calculator — was turned into a strategy, and the shipped slot had
`capabilities.json = {"enabled": []}`. The failure the rule targets is an app whose *entire*
product is arithmetic on typed input, because a spreadsheet replaces it. This proposal's product
is a structured per-student record with a schema, an import path from the SIS's own CSV, a
printable report, an owner-held backup file and a data-loss story — the arithmetic is one
operation inside it, not its purpose, and the measured complaint set is about CRUD, crashes,
purchase portability and the paywall, with **zero complaints about the maths** (evidence file
§2.3).

**That argument is a judgement, not a measurement, and the owner should treat it as one.** If the
owner reads the rule strictly, the same shelf offers an arithmetic-free shape with the same
audience and the same file format: **attendance, participation, missing-work and parent-contact
records with a seating chart** — a record system with no averaging at all. And a fully different
category with an even larger weak pocket is kept on the bench in §12 (coach lineup tools), which
has no arithmetic anywhere in it.

---

## 12. Why this and not the alternatives measured in the same run

| Option | Measured shape | Verdict |
|---|---|---|
| **Teacher gradebook** | `500K+` leader at **3.6★**, `100K+` at 4.1★, `50K+` at 3.7★; IAP on 6/10; complaints name CRUD, crashes, lost purchase, subscription resentment | **Recommended.** Best demand evidence × clearest wedge × proven payment model. Risk: §11 rule reading; measured ceiling $100–$550/mo. |
| Homeschool record and transcript | `homeschool transcript` = `mixed`, guard on-topic **29/29**; weak incumbents 2.4, 2.6, 2.8, 3.0 | **Strong alternative.** Cleanest demand shelf measured, and an arithmetic-free job (hours, portfolio, transcript) — but the largest install bucket is `10K+`, ~50× smaller. |
| Coach lineup tools | `LINEUP11` **3.7★ / 26,700 reviews / `5M+`**; Football Lineup Manager 3.6★/100K+; TeamSnap 2.9★/5M+ | **Kept on the bench.** Larger market and no arithmetic, but the `5M+` leader is **ad-only with no IAP** and the shelf is dense with unrated churn, so willingness to pay at the top is unproven; TeamSnap's pain belongs to users whose league chose it. |
| Subscription trackers | Rocket Money 4.5★/139,000 reviews/`10M+`; manual trackers 3.8–4.5 at 5K–100K | **Rejected for this factory.** The value needs bank linking = server + financial-data posture; an offline manual tracker cannot defend a price against a free bank-linked product. |
| Pickleball scoring | 24.3 M US players (SFIA), but `pickleball scoring app` and `pickleball score keeper` are both **`dead-zone`** with ~20 unrated attempts per shelf | **Rejected on the factory's own rule**: a real trend with a shelf where many tried and none got traction. |
| Miniature paint inventory | On-job incumbents rated **4.6–4.9** | **Rejected**: proven demand with good supply is a fortress to skip. |

---

## 13. Scaffold command (do not run until the owner picks this)

```bash
node scripts/new-slot.mjs --slug teacher-gradebook --name "Mark Book for Teachers" \
  --idea "Private offline gradebook a teacher owns: add, edit and delete every class, student and grade without an account or a server, see running weighted averages, export CSV and PDF, and keep a restorable backup file. Free to install, unlocked once." \
  --economics docs/ideation/plans/teacher-gradebook.economics.json
```

The slot's `capabilities.json` must enable `billing`, or `scripts/check-economics.mjs --slot
teacher-gradebook` will BLOCK the pairing, which is exactly the defect the first slot shipped with.

---

## 14. What the factory should change (brief)

1. **`docs/research/2026-09-26-teacher-gradebook-evidence.md` is the first research file that
   records a shelf where the leader is both large and weak without a homonym caveat.** The
   `query` field proposed in `docs/ideation/2026-09-26-five-recipes.md` §6.5 would have flagged
   `pickleball` automatically; it would not have separated the twelve on-job gradebook apps from
   the seven children's reading apps, because the collision is on the word *grade*, not on a
   generic shelf token. A hand-classification requirement is the only thing that worked here.
2. **The factory should stop writing "$1,000/month" as if it were the target.** ON RECORD,
   **over 80 % of apps never cross it** and the median app makes under $50/month. Every recipe in
   both ideation documents lands in the **$100–$550** band at plausible install flow. The honest
   target is a measured first sale and a real conversion rate, and the plans should say so.

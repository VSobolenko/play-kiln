# Leader head-to-head — `tap-to-say` ("TapToSay: AAC Board") against the app it must actually beat

Written 2026-10-02 by the market analyst role, as the independent fourth entry in the
`docs/ideation/2026-10-02-leader-head-to-head.md` series. The first three recipes
(`rast-saz-oud-tuner`, `rook-encounter-generator`, `part-107-drills`) each got a head-to-head against
the leader of their own primary query; `tap-to-say` had only its scout's self-assessment in
`docs/ideation/2026-10-02-assistive-and-communication.md`. This report applies **the same criterion**
to it so the four can be judged by one standard.

Every factual claim is labelled per `.cursor/rules/grounded-claims.mdc`: **MEASURED** (a command was
run in this session and its output is quoted), **ON RECORD** (a named document states it and was read),
**INFERRED**, or **UNVERIFIED** (with the command or file that would decide it). Every MEASURED row
names the command that produced it. I did not write the recipe and did not see the conversation that
produced it.

Read boundary observed: nothing under `apps/**` was opened. Sources read: the proposal, its three
`plans/*.json`, its evidence file, and `docs/ideation/2026-10-02-leader-head-to-head.md` (§1.1 for the
criterion and format), plus the live Play storefront through `scripts/*.mjs`.

---

## 0. Method, and the criterion used to define a leader

**The primary query** is `searchPhrases[0]` of the recipe's own `naming.json`, because that is the
query the recipe intends to be found on (**MEASURED**, read
`docs/ideation/plans/tap-to-say.naming.json`):

| Recipe | Primary query | Source |
|---|---|---|
| `tap-to-say` | `aphasia communication app` | `plans/tap-to-say.naming.json` `searchPhrases[0]` |

**The leader criterion, stated before the data was read** (copied from
`2026-10-02-leader-head-to-head.md` §1.1): run
`node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "<primary query>"`, then take the
**highest install bucket**; if several apps share it, take the **highest exact review count** (the
unrounded schema.org twin). Install buckets are rounded floors, so this is a floor comparison.

**No two-leader adjustment is needed here, and that is itself a finding.** In the saz and ttrpg
recipes the mechanical criterion leader was an *off-job* generic fortress (a 50M+ tuner; a VTT
manager), while the recipe's real job was served by a smaller app. On `aphasia communication app` the
criterion leader is **an AAC talker — the recipe's own job** — so the criterion leader and the on-job
leader are the same app, and there is no adjacent-job escape hatch. A new listing on this shelf
therefore has to beat the app that does its job, not an unrelated giant.

---

## 1. The leader, by the criterion

`node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "aphasia communication app"` →
30 visible cards, verdict `mixed`, relevance guard `partial` (11/30). Top 10 as printed
(**MEASURED**, that command):

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|--:|---|---|---:|---|---|---|---|---|
| 1 | **`nl.asoft.speechassistant`** | **Speech Assistant AAC** | **4.5** | **3,040 (3,036)** | **500K+** | false | true | free |
| 2 | `com.myaphasiacoach` | My Aphasia Coach | 4.2 | 95 (95) | 10K+ | false | true | free |
| 3 | `com.cm.aac` | AAC Speech & Communication | 4.2 | 22 (22) | 10K+ | false | true | free |
| 4 | `ro.aphasia.app` | Aphasia | unrated | unknown | 1K+ | false | false | free |
| 5 | `com.tactustherapy.languagetherapy` | Language Therapy: Aphasia | 4.7 | 246 (246) | 5K+ | false | false | **paid $89.99** |
| 6 | `com.tactustherapy.apraxiatherapy.lite` | Apraxia Therapy Lite | 4.4 | 118 (118) | 10K+ | false | false | free |
| 7 | `com.constanttherapy.android.main` | Constant Therapy: Brain+Speech | 4.6 | 1,140 (1,141) | 100K+ | false | true | free |
| 8 | `com.tactustherapy.apraxiatherapy` | Apraxia Therapy: Aphasia | 4.6 | 137 (137) | 1K+ | false | false | **paid $29.99** |
| 9 | `com.spoken.app` | Spoken – Tap to Talk AAC | 3.8 | 335 (335) | 100K+ | false | true | free |
| 10 | `com.tactustherapy.languagetherapy.lite` | Language Therapy Lite | 4.6 | 486 (486) | 50K+ | false | false | free |

- **Criterion leader (MEASURED):** `nl.asoft.speechassistant` — *Speech Assistant AAC* (A-Soft-nl) —
  **500K+**, the highest install bucket, and **3,036** exact reviews (3,040 shown), the highest in that
  bucket by an order of magnitude. **4.5★, ads declared absent, IAP declared present, free to
  download.**
- The leader is also the highest install bucket on **both** secondary shelf queries — it ranks #4 on
  `communication board adults` and #3 on `adult aac board`, both at 500K+ (**MEASURED**,
  `enrich-market.mjs --top 20 --concurrency 3 --query "communication board adults"` and
  `--top 10 --query "adult aac board"`).
- **This matches the recipe's own leader reading** (ON RECORD, proposal §4, evidence §5): same package,
  same 4.5★ / 3,040 / 500K+ / free+IAP numbers. The independent read agrees; the disagreements are in
  what the wedge does about it, not in who the leader is.

**Hand-classification, because the guard only scored `partial`** (repeating §1.1's discipline):

```bash
node scripts/scan-titles.mjs --concurrency 3 --query "aphasia communication app" \
  --on-job "aphasia|stroke|communicat|aac|speech|talk|say|board|voice" \
  --off-job "autism|kids|child|toddler|game|puzzle|learn english"
```

→ **hand job-share 20/30 (0.67)** vs the guard's `partial` (11/30, 0.37) (**MEASURED**). The on-job
set includes the leader, Spoken, Avaz and Constant Therapy; only **2** cards are explicitly off-job
(both autism-framed), and 8 unmatched are mostly the Tactus therapy drills. So the shelf really is
mostly on-job, and the leader is a genuine same-job incumbent.

---

## 2. Leader profile

| | Leader `nl.asoft.speechassistant` |
|---|---|
| Rating | 4.5★ |
| Exact reviews | 3,036 (3,040 shown) |
| Installs | 500K+ (≥ 500,000) |
| Ads / IAP / price | declared absent / declared present / free |
| Job | a general-purpose AAC talker (text/speech-assist shaped), adult-agnostic by its title |
| Paid tier behind it | none — it is free with an IAP funnel |

(source: the enrich command in §1, **MEASURED**.) **The leader's feature list was not read in this
session, and neither was the recipe's**, so no claim below says what the leader *does* or *does not*
ship; every leader column is "silent / unknown" unless a measured field (rating, installs, ads, IAP,
price) or a quoted review says otherwise. That limit is load-bearing for the verdict.

---

## 3. The leader's own complaints — **NOT READ, and said so rather than invented**

`node scripts/scan-complaints.mjs --package nl.asoft.speechassistant` — observed, verbatim:

```text
  nl.asoft.speechassistant  4.5★  500K+  reviews=3 critical=0 top=none unclassified=0
```

Play server-renders **three** reviews of its own choosing per page, and **none of the leader's three
was rated 4★ or lower** (**MEASURED**). `--query` mode cannot fill the gap, because it deliberately
selects the **weakest-rated** incumbents by construction, which excludes a 4.5★ leader. **So this
report makes no claim about *why* the leader's users are unhappy.** Any sentence asserting that would
be invented.

Because the sensor returned too few on the leader, the complaint profile below is read from the
**nearest readable competitors** — exactly as the instruction requires — and every complaint quoted is
attributed to the app that wrote it, never to the leader.

### 3.1 Shelf-wide, three query forms

**MEASURED**, `scan-complaints.mjs --query "<q>" --top 6 --concurrency 2`, three runs:

| query | reviews read | critical | paywall | account | crash | missing-feature | other |
|---|---:|---:|---:|---:|---:|---:|---|
| `aphasia communication app` | 15 | 9 | **7 (78%)** | 1 | — | 1 | 1 abandoned, 2 unclassified |
| `communication board adults` | 18 | 5 | **4 (80%)** | 2 | 2 | 2 | 1 notifications |
| `adult aac board` | 12 | 6 | **3 (50%)** | 1 | 1 | 1 | 1 confusing-ui, 1 notifications |

Plus the seven explicitly-named competitors (**MEASURED**,
`--package com.spoken.app --package com.tactustherapy.languagetherapy --package com.shmoontz.commboards.lite --package com.avazapp.international.lite --package com.otsimo.aac --package com.speakeasy.community --package com.unicef.cboard`):
**21 reviews read, 10 critical, and 10 of 10 critical classified `paywall` (100 %)**, with account 3,
missing-feature 2, notifications 2, abandoned 1, crash 1, data-loss 1.

**The single dominant, cross-shelf complaint is monetisation** — consistent across four independent
runs. The classifier is lexical and the sample is Play's three-per-page, so this is the pocket's
**vocabulary**, not its complaint frequency.

### 3.2 The verbatim complaints the wedge clauses name

Every line was printed by the commands above this session (**MEASURED**):

**A locked voice — the `voice-never-paywalled` clause**
- `com.avazapp.international.lite` — *Avaz AAC*, **3.3★, 100K+**: 1★ — *"Actually insane how the one
  thing most needed, the voice feature, is a paid feature. Symbols I could care less about, but the
  voice? Really? That's th"*
- same app: 1★ — *"I have been subscribed for over a year,but I still have issues with the app
  recognizing the subscription when I open it."*

**A rent-shaped or repeat ask — the `one-price-no-renewal` clause**
- `com.spoken.app` — *Spoken – Tap to Talk AAC*, **3.8★, 100K+**: 3★ — *"The app is alright. not very
  fancy. Does what it says I guess. but those subscription plans!!! Ouch. I can't see paying as much
  as I would pay for a f"*; 1★ — *"I paid for 1 year of premium features, but the app never updated to
  allow me to use those features."*
- `com.otsimo.aac` — *Otsimo AAC | Tap and Talk*, **2.8★, 10K+**: 2★ — *"Set up freezes until you pay
  for a subscription. I do want the service and wanted to pay but I made the mistake of putting this
  on my child's phone wh"*
- `com.unicef.cboard` — *Cboard AAC*, **3.9★, 100K+**: 1★ — *"Good program, and even configurable by
  my 5yo. ... But now and surprisingly requiring an upgrade to a subscription!? (Not just
  registration)"* — a complaint the scout's proposal did **not** quote.
- `com.tactustherapy.conversationtherapy.lite` — **3.5★, 10K+**: 2★ — *"The trial is too short of a
  version to make a decision if it is right for you. And $24.99 for full version is a bit much."*

**A forced account — the `offline-no-account` clause**
- `com.speakeasy.community` — *SpeakEasy: Home Speech Therapy*, **3.6★, 100K+**: 1★ — *"Have to sign
  up for a free trial or it can't be used. Something else I'd forget to cancel. I don't care for
  things I have to sign up for and then reme"*

**Language — the `born-bilingual-board` clause**
- `org.pictalk.www.twa` — *PicTalk AAC*, **2.4★, 50K+**: 1★ — *"This app is only good for English
  speakers."*; 1★ — *"Some words are randomly in French"*

**Thin content — the `type-to-say-escape-hatch` clause**
- `com.shmoontz.commboards.lite` — *CommBoards AAC*, **4.0★, 769 exact, 50K+**: 3★ — *"Food choices
  are poor, appear's that you can not add anything or cha"*; 2★ — *"I purchased the full version to
  add pictures, remove items, and rearrange it however I want."*

**One counter-signal the recipe itself already records (ON RECORD, proposal §5.1):** a Tactus buyer
who *did* pay wrote *"have used the trial version of this for my husband who had a terrible stroke 4
weeks ago and he liked it. have now bought the full version as advised"* — i.e. the shelf's own buyers
accept and pay for a clinical-exercise app.

### 3.3 Notably, the strongest verdict-relevant apps are NOT the leader

The verbatim complaints come from **Avaz (3.3★/100K+), Spoken (3.8★/100K+), Otsimo (2.8★/10K+),
Cboard (3.9★/100K+), SpeakEasy (3.6★/100K+) and CommBoards (4.0★/769/50K+)**. None is the criterion
leader. The leader is a 4.5★ app whose complaints were not served. **This is the structural weakness
of this head-to-head**: the wedge's evidence base is the shelf's weak-and-downloaded tier, while the
app the listing must actually beat is cleaner and larger than all of them.

---

## 4. The PAID tier reading — the shelf's money is at the priced end, measured

The instruction is right that this shelf's money may sit at the priced end rather than at the leader.
I enriched three shelf queries and pulled every paid incumbent (**MEASURED**).

### 4.1 The paid incumbents, by job

| package | title | job | rating | exact reviews | installs | price |
|---|---|---|---:|---:|---|---:|
| `com.gusinc.talktableta` | TalkTablet PRO Autism & Stroke | **talking board** | 4.3 | **100** | 5K+ | **paid $199.99** |
| `com.tactustherapy.languagetherapy` | Language Therapy: Aphasia | therapy drill | 4.7 | **246** | 5K+ | **paid $89.99** |
| `com.tactustherapy.advancedlanguagetherapy` | Advanced Language Therapy | therapy drill | 4.6 | 121 | 1K+ | **paid $89.99** |
| `com.tactustherapy.apraxiatherapy` | Apraxia Therapy: Aphasia | therapy drill | 4.6 | 137 | 1K+ | **paid $29.99** |
| `com.contextaac` | Context AAC | talking board | unrated | 0 | 10+ | paid $14.99 |
| `com.inspectorstudios.languagetherapyaphasiarehab` | Language Therapy Aphasia Rehab | therapy drill | unrated | 0 | 500+ | paid $4.99 |

Sources: `enrich-market.mjs --top 10 --query "aphasia therapy app"`;
`--top 20 --query "communication board adults"`; `--top 10 --query "adult aac board"` (**MEASURED**).

**Independent finding the scout did not surface.** The proposal names only Tactus Therapy
($89.99/$29.99). The highest-priced paid incumbent on this entire shelf is
**`com.gusinc.talktableta` — TalkTablet PRO Autism & Stroke — at $199.99**, and it is a **talking
board**, not a drill: **4.3★, 100 exact reviews, 5K+ installs.** A genuine paid talking-board tier
exists, and it is 28× the recipe's $6.99.

### 4.2 Does it look like a paying tier? Yes — a small one, and the free funnel dominates it

- **A developer sells this audience outright, and is rated well for it.** Tactus at **$89.99 (246
  exact)** and **$29.99 (137 exact)**, both ≥ 4.6★; a second Tactus at $89.99 (121); the top-priced
  board at **$199.99 (100 exact, 4.3★)**. Willingness to pay is real on this shelf.
- **The free-to-download-with-IAP shape reaches the volume.** Every paid incumbent is at **≤ 5K+
  installs and ≤ 246 exact reviews**, while the *same developer's* free Lites out-install the paid
  siblings: Language Therapy Lite **486 exact / 50K+**, Advanced Language Therapy Lite **152 / 10K+**,
  Apraxia Therapy Lite **118 / 10K+** (**MEASURED**, same commands). The free funnel is roughly 10×
  the paid one, from one developer on one job.
- **A one-time unlock is bought on a communication board, described by the buyer** — the CommBoards
  reviewer's *"I purchased the full version…"* (**MEASURED**, §3.2), and **TalkTablet's own 100 paid
  reviews** prove buyers pay four figures of cents for a board.
- **The top-priced board still draws a price/support grievance** — `com.gusinc.talktableta`, **3★**:
  *"I have tried on multiple occasions to call to get support and have never received a call back. One
  of the most expensive apps I've ever bought and I c…"*; **1★**: *"Unfortunately, due to the review
  posted on the Google Play Store, we cannot provide further support."* (**MEASURED**,
  `scan-complaints.mjs --package com.gusinc.talktableta …` — 3 reviews, 2 critical, `paywall` 1 +
  `notifications` 1).

**Reading:** the paying tier is a **few-hundred-buyer** tier (max 246 exact reviews on any paid app,
100 on a board), and it is where the recipe's $6.99 ask is aimed. That is a positive measured signal
the saz recipe did not have (rast's entire enriched shelf priced at 0). But it is also *small*: the
recipe's own target of **176 sales/month** (proposal §8.2) is a per-month number comparable to the
*lifetime* review count of the largest paid incumbent — plausible over years, ambitious per month.

---

## 5. Head-to-head — each promised wedge clause against the leader's measured facts

The wedge clauses, verbatim weights from `plans/tap-to-say.differentiators.json` (6 promises, 3 core):
`adult-needs-board` (core), `voice-never-paywalled` (core), `one-price-no-renewal` (core),
`offline-no-account`, `born-bilingual-board`, `type-to-say-escape-hatch`.

| Wedge clause (weight) | Leader's measured facts | Verdict |
|---|---|---|
| **`adult-needs-board`** (core) — tiles are adult/ward needs, never a child's picture book | Leader is titled *Speech Assistant AAC* — an **adult-agnostic talker**, not a child-framed board; its content was unread. The child-framed cards live on the **board** query: `com.shmoontz.commboards.lite` (*CommBoards AAC – Autism Speech*, 4.0★/769/50K+), `com.missingstep.aactalk.autism` (3.9★/42/5K+), `com.aac.autism.cards` (1K+); the guard printed verbatim **`"adult" appears in no title`**. | **Leader silent — and NOT the child-framed app.** The clause lands on the board-query autism-titled incumbents, not on the leader. It is a *content difference*, not a complaint against the leader. |
| **`voice-never-paywalled`** (core) — the voice speaks before the unlock | Leader declares **IAP: true**; whether its IAP gates the voice is **UNVERIFIED** (feature list unread). The complaint is named verbatim on a competitor: Avaz **3.3★/100K+** *"the voice feature, is a paid feature… Really?"* | **Leader silent/unknown; wedge lands on a 100K+ competitor (strong there).** Not confirmed on the leader. |
| **`one-price-no-renewal`** (core) — one purchase, no renewal/expiry/account/server | Leader free+IAP; its price *shape* is **UNVERIFIED**. The complaint is cross-shelf and dominant: paywall **7/9 (78 %)**, **4/5 (80 %)**, **10/10 (100 %)**; verbatim on Spoken, Otsimo, Cboard, Conversation Therapy. | **Leader silent; strongest clause, and it lands cross-shelf — but not measurably on the leader.** |
| **`offline-no-account`** (supporting) | Leader silent. Forced-account complaints on `com.speakeasy.community` (3.6★/100K+) *"Have to sign up for a free trial"* and `com.otsimo.aac`. | **Leader silent; lands on competitors.** |
| **`born-bilingual-board`** (supporting) | Leader's declared locales unread. `org.pictalk.www.twa` (2.4★/50K+) *"only good for English speakers"*, *"randomly in French"*. | **Leader silent; lands on the lowest-rated competitor.** |
| **`type-to-say-escape-hatch`** (supporting) | Leader silent — by its name it is a typing/talking AAC, but its typed board was unread. CommBoards *"you can not add anything"*. | **Leader silent; lands on a board-query incumbent.** |

**A clause the leader already satisfies is not a wedge.** Here the deeper problem is the *opposite*
failure: **no clause is confirmed to be criticised on the leader**, because the leader's critical
reviews were not served (0 of 3) and its feature list was not read. Every clause is either a silent
capability difference or a complaint that belongs to a *different* app. The head-to-head is
**unopposed and unconfirmed** — the wedge may be right, but this reading cannot show it lands on the
app the listing must beat.

---

## 6. What the leader has that we do not

1. **Distribution and social proof that no new listing has.** **3,036 exact reviews at 4.5★ over
   500K+ installs** — an order of magnitude above the next card, and the top install bucket on all
   three shelf queries. A listing with zero ratings is asking a carer to be the first (**MEASURED**,
   §1). This is the single largest disadvantage and there is no honest way to argue it away.
2. **A live IAP funnel on a 500K+ base.** The leader has already learned, with a real population,
   what this audience buys; the recipe goes to market with an unmeasured **$6.99** and an unmeasured
   **2 %** install→unlock assumption (proposal §8, ON RECORD). The leader has data where the recipe
   has an assumption.
3. **A complaint profile that is invisible.** Its three served reviews are non-critical; the shelf's
   anger is aimed at *other* apps. An app that is not visibly resented is harder to displace than a
   2.8★ one — and the 2.8★ ones here (Otsimo) are not the leader.

---

## 7. Verdict — **WEAK**

The recipe's own §4.4 already says the honest version of this: *"Not from the leader's traffic… and
yes from the shelf's priced end."* This independent read agrees with the scout on the **facts** and
disagrees about how much they carry.

What survives: the shelf has a **real, measured paying tier** (TalkTablet **$199.99 / 100 exact**,
Tactus **$29.99/$89.99 at 137/121/246 exact**), a **real and downloaded weak tier**
(Avaz 3.3★/100K+, Spoken 3.8★/100K+, SymboTalk 3.8★/100K+, Otsimo 2.8★/10K+, PicTalk 2.4★/50K+), and
a **dominant, verbatim cross-shelf monetisation complaint** (paywall 78 %/80 %/100 % across four
runs) that the `one-price-no-renewal` clause answers directly. That is more evidence than `rast` had
(whose entire enriched shelf priced at 0).

What fails the leader test: the criterion leader is **the same job** (an AAC talker), is **4.5★ with
3,036 reviews**, declares **no ads**, and has **no readable complaint profile**. Every wedge clause is
either silent on the leader or a complaint owned by a smaller, weaker app. Nothing measured here shows
a carer defecting from a free 4.5★/3,036-review incumbent to a **$6.99** board, and the recipe's own
kill floor (2,667 installs/month) runs against a shelf whose leader owns the head term on all three
queries.

**The one thing that would have to be true:** the leader's own users must be unhappy for the
monetisation reason the wedge names (a gated or rented voice), so that the complaint the recipe
answers exists *on the app a new listing must beat*. **UNVERIFIED** —
`node scripts/scan-complaints.mjs --package nl.asoft.speechassistant` returned **3 reviews, 0
critical**; the deciding reading is a human opening that app's full review list on its Play page
sorted by lowest rating, since the sensor cannot page past Play's server-rendered three.

**The verdict rests on an UNVERIFIED row** (the leader's complaint profile) and says so: if that row
turns out positive — the leader gates or rents its voice — the verdict moves toward **PLAUSIBLE** on
the strength of the `one-price`/`voice-free` clauses; if it turns out the leader is clean or already
one-time, the verdict stays **WEAK**, because the only remaining wedge is a content difference
(adult-authored, bilingual, typed board) that no leader user is recorded asking for.

---

## 8. Cross-recipe ranking — all four head-to-heads, one standard

Ordered by how well the promised wedge survives contact with the leader of the primary query.

| Rank | Recipe | Verdict | One-line reason |
|---:|---|---|---|
| 1 | `part-107-drills` | **PLAUSIBLE** | The leader is the weakest incumbent of the four (4.0★/440/10K+) and its **own** critical reviews name the ads and subscription the wedge replaces — but the distinct core clause (generated drills) answers no measured complaint. |
| 2 | `rook-encounter-generator` | **PLAUSIBLE** | The weak generator tier is real and downloaded (2.1–3.3★ at 10K–100K+) and its users complain about the wedge clauses verbatim; the criterion leader is an **adjacent** job (a 100K+/4.3★ VTT manager) the wedge neither beats nor needs to. |
| 3 | `tap-to-say` | **WEAK** | The shelf has a measured paid tier and a dominant paywall complaint, but the criterion leader is the **same job**, is **4.5★/3,036/500K+ with no readable complaints**, and every wedge clause either is silent on it or belongs to a smaller, weaker app. |
| 4 | `rast-saz-oud-tuner` | **WEAK** | The on-job leader is a **free 4.4★/1M+ incumbent with 8,023 reviews and a clean sampled page**, and the criterion leader is a 4.6★/50M+ generic fortress; the wedge's complaint base is the generic/mid-tier. |

### 8.1 Is `tap-to-say`'s head-to-head stronger or weaker than each of the other three?

- **vs `part-107-drills` — WEAKER.** part-107's verdict rests on the leader's **own** critical reviews
  naming the ads and the subscription (*"literally constant pornographic ads"*, *"Scummy business
  practices"*). `tap-to-say`'s leader returned **0 critical of 3**, so the wedge cannot be shown to
  land on the app it must beat. Same-job clean leader is a harder head-to-head than a same-job
  criticised leader.
- **vs `rook-encounter-generator` — WEAKER.** rook's criterion leader is an **adjacent job**, so the
  head-to-head could honestly evaluate the contest that mattered (the weak downloaded generator tier)
  and found it winnable with verbatim complaints. `tap-to-say`'s criterion leader is the **same job**
  and is the clean, 10×-larger incumbent — the contest that matters is against the leader, and that
  contest is unconfirmed. (The weak-tier evidence itself is comparably strong; the difference is which
  reader the criterion forces you to confront.)
- **vs `rast-saz-oud-tuner` — STRONGER.** Both leaders have clean sampled pages and both wedges lean
  on the weak/mid-tier, so the shape is the same. But `rast` had **no priced comparable at all** —
  every enriched incumbent priced at 0 (report §1.4) — while `tap-to-say` has a **measured paying
  tier** (TalkTablet $199.99/100; Tactus $29.99/$89.99 at 121–246 exact), a **dominant cross-shelf
  paywall complaint** the wedge answers, and a **larger downloaded weak tier** (multiple 100K+ talkers
  at 2.4–3.8★). The money question that was UNVERIFIED for rast has a positive measured reading here.

**Which wedge clause failed the leader test most clearly for this recipe:** `adult-needs-board`. The
leader is a 4.5★ adult-agnostic talker — the child-framed boards are *other* apps — so a clause whose
entire evidence is "the board shelf is titled for children" addresses an incumbent the recipe is not
principally competing with, and answers no complaint any **leader** user wrote.

---

## 9. Claim ledger

`CLAIM / LABEL / EVIDENCE`.

| # | Claim | Label | Evidence |
|--:|---|---|---|
| 1 | `aphasia communication app` returns 30 cards, verdict `mixed`, guard `partial` (11/30) | MEASURED | `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "aphasia communication app"` |
| 2 | Criterion leader is `nl.asoft.speechassistant` (Speech Assistant AAC, A-Soft-nl): 4.5★, 3,040 shown (3,036 exact), 500K+, ads false, IAP true, free | MEASURED | same command, row 1 |
| 3 | The leader is also the highest install bucket on `communication board adults` (#4, 500K+) and `adult aac board` (#3, 500K+) | MEASURED | `enrich-market.mjs --top 20 --concurrency 3 --query "communication board adults"`; `--top 10 --query "adult aac board"` |
| 4 | Hand job-share on the primary query is 20/30 (0.67) vs the guard's 11/30 (0.37) | MEASURED | `node scripts/scan-titles.mjs --concurrency 3 --query "aphasia communication app" --on-job "aphasia\|stroke\|communicat\|aac\|speech\|talk\|say\|board\|voice" --off-job "autism\|kids\|child\|toddler\|game\|puzzle\|learn english"` |
| 5 | The leader's own critical reviews were not read: 3 reviews, 0 critical | MEASURED | `node scripts/scan-complaints.mjs --package nl.asoft.speechassistant` |
| 6 | `aphasia communication app` shelf: 15 reviews, 9 critical, paywall 7 (78%) | MEASURED | `node scripts/scan-complaints.mjs --query "aphasia communication app" --top 6 --concurrency 2` |
| 7 | `communication board adults` shelf: 18 reviews, 5 critical, paywall 4 (80%), account 2, crash 2, missing-feature 2 | MEASURED | `node scripts/scan-complaints.mjs --query "communication board adults" --top 6 --concurrency 2` |
| 8 | `adult aac board` shelf: 12 reviews, 6 critical, paywall 3 (50%), confusing-ui 1 | MEASURED | `node scripts/scan-complaints.mjs --query "adult aac board" --top 6 --concurrency 2` |
| 9 | Seven named competitors: 21 reviews, 10 critical, paywall 10 (100%), account 3 | MEASURED | `node scripts/scan-complaints.mjs --package com.spoken.app --package com.tactustherapy.languagetherapy --package com.shmoontz.commboards.lite --package com.avazapp.international.lite --package com.otsimo.aac --package com.speakeasy.community --package com.unicef.cboard` |
| 10 | Verbatim Avaz AAC (3.3★, 100K+): *"the voice feature, is a paid feature… Really?"* | MEASURED | same command, `com.avazapp.international.lite` |
| 11 | Verbatim Spoken (3.8★, 100K+): *"those subscription plans!!! Ouch."*; Otsimo (2.8★, 10K+): *"Set up freezes until you pay for a subscription. I do want the service and wanted to pay"* | MEASURED | §3.1 query run and §9 package run |
| 12 | Verbatim Cboard (3.9★, 100K+): *"now and surprisingly requiring an upgrade to a subscription!?"* | MEASURED | package run, `com.unicef.cboard` |
| 13 | Verbatim SpeakEasy (3.6★, 100K+): *"Have to sign up for a free trial or it can't be used"* | MEASURED | package run, `com.speakeasy.community` |
| 14 | Verbatim PicTalk (2.4★, 50K+): *"only good for English speakers"*, *"Some words are randomly in French"* | MEASURED | §3.1 query run, `org.pictalk.www.twa` |
| 15 | Verbatim CommBoards (4.0★, 769, 50K+): *"you can not add anything"*, and *"I purchased the full version to add pictures, remove items, and rearrange it however I want"* | MEASURED | package run, `com.shmoontz.commboards.lite` |
| 16 | Tactus paid tier: Language Therapy $89.99 / 246 exact / 5K+ / 4.7★; Apraxia Therapy $29.99 / 137 / 1K+ / 4.6★; Advanced Language Therapy $89.99 / 121 / 1K+ / 4.6★ | MEASURED | `enrich-market.mjs --top 10 --concurrency 3 --query "aphasia therapy app"` |
| 17 | Highest-priced paid incumbent on the shelf is `com.gusinc.talktableta` — TalkTablet PRO Autism & Stroke — $199.99, 4.3★, 100 exact, 5K+, a talking board | MEASURED | `enrich-market.mjs --top 20 --concurrency 3 --query "communication board adults"`, row 13 |
| 18 | Also paid: `com.contextaac` Context AAC $14.99 (unrated, 10+); `com.inspectorstudios.languagetherapyaphasiarehab` $4.99 (unrated, 500+) | MEASURED | both commands above |
| 19 | TalkTablet PRO's own complaints: 3 reviews, 2 critical — *"One of the most expensive apps I've ever bought"*, and a support-withdrawal 1★ | MEASURED | `node scripts/scan-complaints.mjs --package com.gusinc.talktableta --package com.contextaac --package com.inspectorstudios.languagetherapyaphasiarehab` |
| 20 | The same developer's free Lites out-install the paid siblings (Language Therapy Lite 486/50K+ vs the $89.99 sibling 246/5K+) | MEASURED | `enrich-market.mjs --query "aphasia therapy app"` |
| 21 | The board query's top cards are child-framed (CommBoards – Autism Speech, AAC Talk: Autism Communication, AAC Autism Communication Cards) and the guard printed `"adult" appears in no title` | MEASURED | `enrich-market.mjs --query "communication board adults"`; printed guard line |
| 22 | The recipe's own leader reading matches mine (4.5★/3,040/500K+/free+IAP) | ON RECORD | `docs/ideation/2026-10-02-assistive-and-communication.md` §4; evidence §5 |
| 23 | The wedge clauses and weights (6 promises, 3 core) | MEASURED | read `docs/ideation/plans/tap-to-say.differentiators.json`; proposal §7 |
| 24 | The primary query is `searchPhrases[0]` of the recipe's `naming.json` | MEASURED | read `docs/ideation/plans/tap-to-say.naming.json` |
| 25 | The criterion (highest install bucket, then highest exact review twin) is the one used for all four recipes | ON RECORD | `docs/ideation/2026-10-02-leader-head-to-head.md` §1.1 |
| 26 | The recipe itself concedes it cannot take traffic from the leader's query and aims at the priced end | ON RECORD | proposal §4.4 |
| 27 | The shelf's own buyers accept and pay for a clinical-exercise app (Tactus 4.7★/5K+ buyer) | MEASURED | `scan-complaints.mjs --package com.tactustherapy.languagetherapy` |
| 28 | The shelf's money is at the priced end (a paid tier exists at $14.99–$199.99) | INFERRED | rows 16–18: paid incumbents measured; that the price is *reachable* is the inference |
| 29 | This head-to-head is WEAKER than part-107 and rook, STRONGER than rast | INFERRED | the three verdict rows (report §8) plus the measured facts in rows 2–21 |
| 30 | **Why the leader's users are unhappy — and therefore whether the wedge's monetisation clause lands on the app a new listing must beat** | **UNVERIFIED** | `scan-complaints.mjs --package nl.asoft.speechassistant` returned 3 reviews / 0 critical; the sensor cannot page past Play's server-rendered three. Deciding reading: a human opening the leader's full review list sorted by lowest rating |
| 31 | Whether the leader gates or rents its voice (the exact object of `voice-never-paywalled`) | UNVERIFIED | the leader's feature list was not read this session; deciding act: read `nl.asoft.speechassistant`'s listing/description and its lowest-rated reviews |
| 32 | Willingness to pay **$6.99** outright for this job | UNVERIFIED | the measured one-time purchases are TalkTablet $199.99, Tactus $29.99/$89.99, and CommBoards' unlock (price not on its card); nothing shows a carer buying a talking board at $6.99 |
| 33 | Install → unlock conversion is 2 % (and therefore every installs/revenue figure in the plan) | UNVERIFIED | proposal §8 / §12; no slot has ever measured a conversion rate; Play Console after 90 days |
| 34 | That the free starter board converts better than a gated one | UNVERIFIED | no reading shows it; the readings show only that buyers describe the locked voice and subscription as why they are angry |
| 35 | Search ranking: whether a new listing reaches the kill floor (2,667 installs/month) against 3,036 reviews | UNVERIFIED | nothing here measures Play ranking; Play Console acquisition data, first 30 days |
| 36 | Whether anyone searches the word "adult" for this job | UNVERIFIED | the guard's `"adult" appears in no title` is consistent with both an unserved job and a word nobody types; Play autocomplete, signed-in, would decide |
| 37 | The exact storefront copy behind every note above is the one Play served this session | UNVERIFIED | one signed-out `en-US` session; not a Play Console reading and not reproducible byte-for-byte |

**Honest limits of this report.** Install buckets are rounded floors (`500K+` spans
500,000–999,999), so every size comparison is a floor comparison. `scan-complaints` reads Play's own
server-rendered sample (3 reviews per page) on the **weakest-rated** incumbents for a query and on the
named package for `--package`, so the complaint tables describe the **vocabulary** of a shelf, never
complaint frequency in its population; a leader with 0 critical reviews in a 3-review sample is not a
leader without complaints — it is a leader whose complaints were not served. The leader was selected by
one criterion applied to one query in one storefront session; a larger incumbent can sit below the
enrichment cut. The `adult aac board` and `communication board adults` shelf queries each reported
`candidate` with the guard's printed caveat `"adult" appears in no title`, i.e. every board query's
audience signal is the absence of a bid, which is not the same as demand. This report did **not** read
the leader's feature list, so no claim is made about what it ships.

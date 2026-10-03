# Assistive & access tools — measured evidence for the AAC / bedside-communication lane (2026-10-02)

This is the durable evidence file behind `docs/ideation/2026-10-02-assistive-and-communication.md`
and the three plans beside it (`tap-to-say.economics.json`, `tap-to-say.differentiators.json`,
`tap-to-say.naming.json`). It records **what was read on which command**, so a later reader can
contradict a number rather than trust it.

Everything below was run on 2026-10-02 from `d:\Python\PlayKiln` in this session. Nothing here is
recalled from memory and nothing is a paraphrase of another document in this repository.

**What this file is not.** It is a reading of one search surface in a signed-out `en-US` Play
session. Play's result page is a display-capped, personalised spot check
(`docs/niche-selection.md` §3, "Traps this procedure exists to avoid"), so the counts below describe
the visible page, not a market size. It is not a substitute for Play Console data, which no slot has
for this query.

## 1. Calibration — the sensor was not drifting

The controls were run **in the same invocation** as the lane queries, as
`docs/niche-selection.md` requires, so a drift would have been visible next to the readings it would
have invalidated. Command:

```bash
node scripts/scan-market.mjs --concurrency 4 --query "aphasia communication app" --query "adult aac board" \
  --query "communication board adults" --query "stroke communication app" \
  --query "habit tracker" --query "bjj training log"
```

Observed, verbatim from the output:

| query | verdict | recorded expectation | agrees |
|---|---|---|---|
| `habit tracker` | `fortress` | must return `fortress` | **yes** |
| `bjj training log` | `dead-zone` | must return `dead-zone` | **yes** |

**MEASURED.** No drift. The four lane readings in the same run are therefore usable.

## 2. The four lane queries, as the sensor scored them

**MEASURED**, same command as §1:

| query | verdict | relevance | seen | rated | weak | median |
|---|---|---|---:|---:|---:|---:|
| `aphasia communication app` | `mixed` | partial | 30 | 23/30 | 9 | 4.20 |
| `stroke communication app` | `mixed` | partial | 30 | 22/30 | 8 | 4.25 |
| `adult aac board` | `candidate` | partial | 29 | 17/29 | 12 | 3.80 |
| `communication board adults` | `candidate` | partial | 30 | 20/30 | 12 | 3.90 |

**The honest caveat, stated before any conclusion rests on this.** Every one of the four lane queries
was scored **`partial`** by the relevance guard — a share of the returned cards is not about the query
— and §2.1 of `docs/research/2026-09-24-market-decision.md` records the guard passing shelves that do
not answer the query. So the guard verdicts above are spot checks, and §3 is the hand-classification
that this document actually relies on.

## 3. Hand-classification of the primary shelf

`docs/niche-selection.md` §3 step 2 and `docs/research/2026-09-24-market-decision.md` §2.1 both
require the titles to be read by a human (or a hand-authored regex) before a verdict is believed.
Command:

```bash
node scripts/scan-titles.mjs --concurrency 3 --query "aphasia communication app" \
  --on-job "aphasia|stroke|communicat|aac|speech|talk|say" \
  --off-job "autism|kids|child|toddler|game|puzzle|learn english"
```

Observed:

- **hand job-share 20/30 (0.67)** versus the guard's `partial`.
- 20 cards on the job, 2 explicitly off-job (both autism-framed children's AAC), 8 unmatched and
  classified by eye (six of them Tactus Therapy exercise apps, which are therapy drills rather than
  talkers, plus `com.afasia.digo` and `com.stamurai.stamurai`).

The hand share is nearly double the guard's token share, which is the guard being blind to paraphrase
rather than a different market: the on-job cards include `Speech Assistant AAC`, `Spoken – Tap to
Talk AAC`, `Avaz AAC` and `TypingTalk AAC Text-to-Speech`, none of which carries a distinctive
`aphasia` token in a way the guard counted.

### 3.1 The adult-vs-child framing reading

**MEASURED**, §3 output and
`node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "communication board adults"`:

- The three highest-install cards on the board query are framed for **children in their own titles**:
  `com.shmoontz.commboards.lite` — *CommBoards AAC – Autism Speech* (4.0★, 769 exact reviews, 50K+);
  `com.missingstep.aactalk.autism` — *AAC Talk: Autism Communication* (3.9★, 42, 5K+);
  `com.aac.autism.cards` — *AAC Autism Communication Cards* (unrated, 1K+).
- The guard reported for the board query, verbatim: **`"adult" appears in no title`**.
- The titles that *do* serve adults narrow to a setting or an aphasia subtype instead: `Language
  Therapy: Aphasia`, `Apraxia Therapy: Aphasia`, `My Aphasia Coach`, `Aphasia Talk Around It Home`.

This is the pocket's central gap, and it is a **content** gap rather than a naming one: an adult who
has lost speech gets boards authored for children, or clinical exercise apps.

## 4. The pocket readings

### 4.1 Primary query `aphasia communication app` — the enriched top 8

**MEASURED**, `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "aphasia communication app"`:

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|---|---|---|---:|---|---:|---|---|---|---|
| 1 | `nl.asoft.speechassistant` | Speech Assistant AAC | 4.5 | 3,040 (3,036) | 500K+ | false | true | free |
| 2 | `com.myaphasiacoach` | My Aphasia Coach | 4.2 | 95 (95) | 10K+ | false | true | free |
| 3 | `com.cm.aac` | AAC Speech & Communication | 4.2 | 22 (22) | 10K+ | false | true | free |
| 4 | `ro.aphasia.app` | Aphasia | unrated | unknown | 1K+ | false | false | free |
| 5 | `com.tactustherapy.languagetherapy` | Language Therapy: Aphasia | 4.7 | 246 (246) | 5K+ | false | false | **paid $89.99** |
| 6 | `com.tactustherapy.apraxiatherapy.lite` | Apraxia Therapy Lite | 4.4 | 118 (118) | 10K+ | false | false | free |
| 7 | `com.constanttherapy.android.main` | Constant Therapy: Brain+Speech | 4.6 | 1,140 (1,141) | 100K+ | false | true | free |
| 8 | `com.tactustherapy.apraxiatherapy` | Apraxia Therapy: Aphasia | 4.6 | 137 (137) | 1K+ | false | false | **paid $29.99** |

### 4.2 Secondary query `aphasia therapy app` — the paid incumbents, again

**MEASURED**, `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "aphasia therapy app"`:

| package | title | rating | reviews (exact) | installs | IAP | price |
|---|---|---:|---|---:|---|---|
| `com.tactustherapy.languagetherapy` | Language Therapy: Aphasia | 4.7 | 246 (246) | 5K+ | false | **paid $89.99** |
| `com.tactustherapy.languagetherapy.lite` | Language Therapy Lite | 4.6 | 486 (486) | 50K+ | false | free |
| `com.tactustherapy.apraxiatherapy` | Apraxia Therapy: Aphasia | 4.6 | 137 (137) | 1K+ | false | **paid $29.99** |
| `com.constanttherapy.android.main` | Constant Therapy: Brain+Speech | 4.6 | 1,140 (1,141) | 100K+ | true | free |

**The paid-incumbent requirement is satisfied on policy-relevant terms**: a developer sells this
audience outright at **$89.99** (246 exact reviews) and **$29.99** (137 exact reviews), and both are
rated ≥ 4.6. A separate **free** product from the same developer, `Language Therapy Lite`, holds 486
exact reviews at 50K+ installs — which is the shelf's own evidence that the paid/free split is the
shape that reaches volume here.

### 4.3 Secondary query `communication board adults` — the enriched top 8

**MEASURED**, `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "communication board adults"`:

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|---|---|---|---:|---|---:|---|---|---|---|
| 1 | `com.shmoontz.commboards.lite` | CommBoards AAC – Autism Speech | 4.0 | 769 (769) | 50K+ | false | true | free |
| 2 | `com.unicef.cboard` | Cboard AAC | 3.9 | 354 (354) | 100K+ | false | true | free |
| 3 | `app.hobbysoft.speakingbuttons` | Talking Buttons - AAC Board | 3.8 | 85 (85) | 10K+ | false | false | free |
| 4 | `nl.asoft.speechassistant` | Speech Assistant AAC | 4.5 | 3,040 (3,036) | 500K+ | false | true | free |
| 5 | `com.cm.aac` | AAC Speech & Communication | 4.2 | 22 (22) | 10K+ | false | true | free |
| 6 | `com.zmnsoft.salon` | AAC Communication Board | unrated | unknown | 100+ | false | false | free |
| 7 | `com.missingstep.aactalk.autism` | AAC Talk: Autism Communication | 3.9 | 42 (42) | 5K+ | **true** | true | free |
| 8 | `com.aac.autism.cards` | AAC Autism Communication Cards | unrated | unknown | 1K+ | false | false | free |

`com.unicef.cboard` (Cboard org, 100K+, 3.9★, 354 exact reviews, IAP) is the UNICEF-backed free
incumbent and is the strongest free product on the board query. It is a **children's picture-symbol
board**, which is the incumbent shape this lane's wedge is measured against, not a competitor for the
adult ward job.

## 5. The LEADER, stated as the sensor measured it

**Leader rule applied:** highest install bucket on the primary query, then highest exact review count.
On `aphasia communication app` that is unambiguous — one card is an order of magnitude ahead of the
next, and it is also the leader on the board query.

| field | leader: `nl.asoft.speechassistant`, *Speech Assistant AAC* (A-Soft-nl) |
|---|---|
| rating | **4.5** |
| reviews | **3,040 shown, 3,036 exact** |
| installs | **500K+** |
| ads | **false** (declared absent) |
| IAP | **true** |
| price | **free** (to download) |
| source | **MEASURED** — `enrich-market.mjs` on `aphasia communication app` and again on `communication board adults`, §4.1 and §4.3 |

### 5.1 The leader's own critical reviews — **UNVERIFIED, and this is a real gap**

`node scripts/scan-complaints.mjs --package nl.asoft.speechassistant --package com.spoken.app --package com.shmoontz.commboards.lite --package com.tactustherapy.languagetherapy`

Observed for the leader, verbatim:

```text
  nl.asoft.speechassistant  4.5★  500K+  reviews=3 critical=0 top=none unclassified=0
```

Play server-renders **three** reviews of its own choosing per page (`scan-complaints.mjs` header,
"measured: 3"), and in this sample **none of the leader's three was rated 4★ or lower**. The
`--query` mode cannot fill the gap either, because it deliberately selects the **weakest rated**
incumbents — which excludes a 4.5★ leader by construction.

**So: the leader's critical complaints were not read in this session.** Any sentence claiming what
they say — including the tempting "the leader is also a subscription app" — would be invented, and
this document does not make it. See §7 for the exact command that would settle it.

### 5.2 The closest direct competitor, whose complaints *were* read

`com.spoken.app` — *Spoken – Tap to Talk AAC* (Spoken Inc.), **3.8★, 100K+, two of its three read
reviews critical, both classified `paywall`**. This is the app whose shape is closest to this lane's
recipe (a tap-to-talk board), and it is where the paywall evidence is strongest. Its verbatim
critical reviews are in §6.

## 6. What the incumbents' own users wrote — the complaint reading

Two query-level runs and one package-level run. **MEASURED.**

```bash
node scripts/scan-complaints.mjs --query "aphasia communication app" --top 6 --concurrency 2
node scripts/scan-complaints.mjs --query "communication board adults"  --top 6 --concurrency 2
node scripts/scan-complaints.mjs --package nl.asoft.speechassistant --package com.spoken.app \
  --package com.shmoontz.commboards.lite --package com.tactustherapy.languagetherapy
```

### 6.1 Pocket profile

| pocket | reviews read | critical | paywall | account | crash | missing-feature | abandoned | other |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| `aphasia communication app` | 15 | 9 | **7 (78%)** | 1 (11%) | — | 1 (11%) | 1 (11%) | 2 unclassified |
| `communication board adults` | 18 | 5 | **4 (80%)** | 2 (40%) | 2 (40%) | 2 (40%) | — | 1 notifications |
| explicit packages (§5.1 set) | 12 | 5 | **5 (100%)** | 1 | — | 1 | 1 | 1 data-loss |

**The single dominant, cross-shelf complaint is monetisation**: rent, a locked voice, a trial that
gates setup, or a price that "is a bit much". The classifier is lexical and the sample is Play's, so
this is the **vocabulary** of the pocket rather than its complaint frequency — but it is consistent
across three independent runs and every direction of query.

### 6.2 Verbatim critical reviews, quoted as printed

Every line below was printed by the commands in §6 and is reproduced exactly as the sensor emitted it
(including the ellipsis Play's own card used). Package, rating and install bucket are from the same
runs.

**Monetisation — a locked voice**

- `com.avazapp.international.lite` — *Avaz AAC*, **3.3★, 100K+**: 1★ — *"Actually insane how the one
  thing most needed, the voice feature, is a paid feature. Symbols I could care less about, but the
  voice? Really? That's th"*
- same app: 1★ — *"I have been subscribed for over a year,but I still have issues with the app
  recognizing the subscription when I open it. Sometimes I have to close the"*
- `com.spoken.app` — *Spoken – Tap to Talk AAC*, **3.8★, 100K+**: 3★ — *"The app is alright. not very
  fancy. Does what it says I guess. but those subscription plans!!! Ouch. I can't see paying as much
  as I would pay for a f"*
- same app: 1★ — *"This app is horrible. I paid for 1 year of premium features, but the app never
  updated to allow me to use those features. One year later, all of a sud"*

**Monetisation — a paywall before the app can be used at all, from a buyer who wanted to pay**

- `com.otsimo.aac` — **2.8★, 10K+**: 2★ — *"Set up freezes until you pay for a subscription. I do want
  the service and wanted to pay but I made the mistake of putting this on my child's phone wh"*

**Monetisation — a one-time price judged too high**

- `com.tactustherapy.conversationtherapy.lite` — **3.5★, 10K+**: 2★ — *"The trial is too short of a
  version to make a decision if it is right for you. And $24.99 for full version is a bit much."*
- same app: 2★ — *"I agree with the other reviewer who said that the trial is too short to assess the
  value of this app and 25$ seems way too high."*

**Accounts and trials**

- `com.speakeasy.community` — *SpeakEasy: Home Speech Therapy*, **3.6★, 100K+**: 1★ — *"Have to sign up
  for a free trial or it can't be used. Something else I'd forget to cancel. I don't care for things I
  have to sign up for and then reme"*
- `com.tactustherapy.languagetherapy` — *Language Therapy: Aphasia*, **4.7★, 5K+**: 3★ — *"have used
  the trial version of this for my husband who had a terrible stroke 4 weeks ago and he liked it.
  have now bought the full version as advised"*

**Language, from the lowest-rated card in the primary pocket**

- `org.pictalk.www.twa` — *PicTalk AAC*, **2.4★, 50K+**: 1★ — *"This app is only good for English
  speakers. If you are not I suggest you try a different one."*
- same app: 1★ — *"Some words are randomly in French"*

**Stability and content, from a buyer who already paid**

- `com.elelad.comboard` — **3.8★, 100K+**: 1★ — *"This 'paid for' app keeps crashing ... absolutely
  shocking for a child with autism trying to communicate!! Utterly frustrating that my son tries to up"*
- `com.shmoontz.commboards.lite` — *CommBoards AAC*, **4.0★, 769 exact reviews, 50K+**: 2★ — *"I like
  the simplicity of this app. I purchased the full version to add pictures, remove items, and
  rearrange it however I want. The biggest issue I ha"*
- same app: 3★ — *"You don't give people a chance to trial your product, before you ask for ratings.
  Food choices are poor, appear's that you can not add anything or cha"*

**MEASURED, and the two facts the recipe is built on:**

1. **A one-time unlock is bought in this category by real users.** `com.shmoontz.commboards.lite`'s own
   reviewer states *"I purchased the full version to add pictures, remove items, and rearrange it"* —
   a one-time purchase, described by the person who made it, on a communication board.
2. **The buyers exist; the *shape* of the ask is what they reject.** `com.otsimo.aac`'s reviewer writes
   *"I do want the service and wanted to pay"* and is stopped by a subscription that freezes setup —
   the clearest possible statement that willingness to pay is present and a rent-shaped ask is what
   blocks it.

## 7. What this file does not establish

Each row names the command or the source that would settle it.

| Not established | Label | How it would be settled |
|---|---|---|
| The leader's own critical complaints | **UNVERIFIED** | `node scripts/scan-complaints.mjs --package nl.asoft.speechassistant` returned 3 reviews / 0 critical; a human reading the full review list on the Play page (`show all reviews`, sorted by lowest rating) would settle it — the sensor cannot page past Play's server-rendered 3 |
| Whether anyone searches the word **adult** for this job | **UNVERIFIED** | the guard reported `"adult" appears in no title` on the board query, i.e. no incumbent bids on the word; Play search autocomplete for `aac` and `communication board`, read in a signed-in session, would show whether users type it |
| Whether a carer will pay $6.99 outright for this job | **UNVERIFIED** | the only measured one-time purchases on the shelf are CommBoards' unlock (price not visible on the card) and Tactus at $29.99/$89.99; a price test in Play Console, or the first 90 days of the slot's own sales, would settle it |
| Whether `com.gexetr.taptosay` is free on Play | **UNVERIFIED** | a Play Console "create app" attempt, or a Play search for the package name; no repository tool queries package availability |
| Trademark headroom for `TapToSay` in class 9 | **UNVERIFIED** | USPTO, EUIPO (eSearch plus / TMview), WIPO Global Brand Database and the ru national register were **not** queried; a clean Play search is not clearance |
| The rung of the shelf a new listing can rank on | **UNVERIFIED** | nothing here measures Play ranking for any query; the shelf is led by 500K+ installs and 3,036 exact reviews, and no reading in this session shows a new listing reaching the 2,667 installs a month the plan's kill floor requires against it |

## 8. Sources

- Repository instruments: `scripts/scan-market.mjs`, `scripts/scan-titles.mjs`,
  `scripts/enrich-market.mjs`, `scripts/scan-complaints.mjs`,
  `scripts/lib/play-scan.mjs`, `scripts/lib/play-app-detail.mjs`, `scripts/lib/play-reviews.mjs`.
- Method and limits: `docs/niche-selection.md` §2–§4;
  `docs/research/2026-09-24-market-decision.md` §2.1 (guard blindness), §3 (hard kills), §7.4
  (`dementia caregiver log` was killed as **thin, not weak** — the nearest neighbouring job, and the
  reason this lane is measured fresh rather than assumed from it).
- Policy: `docs/research/2026-09-23-autism-app-policy.md` §§1, 3, 4, 7 (target audience, Families
  trigger and its override rule, ads, IAP, declarations).
- Money convention: `docs/monetization-strategy.md` §2.1 (the illustrative `$1,000/month` target),
  §2.2 (the deduction chain and the assumption table). `F_billing = 0.15`, `F_refund = 0.02`,
  `F_fx = 0.02` are **ASSUMPTIONS** carried from that document's table, not measurements of this
  developer account.

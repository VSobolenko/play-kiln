# Assistive & access tools — an adult bedside communication board that speaks

**Recon, 2026-10-02. Lane 4 of 4.** Scope: apps whose primary value is helping a person
**communicate or access the world**. Recorded read boundary: nothing under `apps/**` was opened, and
the only view of our own output is the identity fields of `registry/apps.json` plus the killed lists
in `docs/research/` and `docs/niche-selection.md`.

**Verdict: one recipe proposed**, slug `tap-to-say`, with its three gate-ready contracts beside it.
The paid-incumbent requirement is satisfied on policy-relevant terms — a developer on this shelf
sells this exact audience outright at **$89.99** and **$29.99**, both rated ≥ 4.6 — and the shelf's
own users describe a **one-time purchase** they made on a communication board.

---

## 0. The recipe in one screen

| field | value |
|---|---|
| **slug** | `tap-to-say` |
| **applicationId** | `com.gexetr.taptosay` |
| **name** | **TapToSay: AAC Board** |
| **category / job** | A tap-to-talk communication board for an adult who cannot speak reliably after a stroke or with aphasia: the app speaks the words the person taps, and lets them type when the tiles do not cover the need. |
| **the paying user** | The family carer, spouse or adult child who sets the phone up — buying out of pocket in the first weeks home or at a hospital bedside. |
| **what is bought** | One permanent unlock of the full authored adult board: every ward and daily-needs tile in `en` and `ru`, unlimited personal phrases, and the enlarged high-contrast tile layout. No renewal, no expiry, no account. |
| **price** | **$6.99**, one time |
| **moment of payment** | *After* the free starter board has already spoken a tile out loud in the device's own text-to-speech, so the buyer has heard the app work before any sheet appears; one sheet, price on it, no trial clock. |
| **archetype** | `purchase_led_single_unlock` (capability `billing`) |
| **server required** | **No.** Content ships in the APK; speech is the device's installed TTS engine. |
| **build estimate** | **6 agent hours** |
| **wedge, one sentence** | Every incumbent on this shelf that lets a person speak either rents the voice back or gates it — paywall anger is 7 of the 9 critical reviews in the primary pocket and 4 of the 5 in the board pocket, one 100K+ app is told in its own reviews that locking the voice "is insane", and the highest-install cards on the board query are titled for children with autism — so this slot gives the voice away free forever and sells **breadth**, on a board whose every tile is an authored adult or ward need in two languages. |

**Target audience declaration, stated up front.** Adults. The patient is an adult and the buyer is an
adult; the app ships adult copy, adult UI and **no child-directed imagery or terminology** anywhere in
the app or the listing. That is not a shortcut around the Families rules — it is the accurate
declaration, and §10 states the policy reasoning and its residual risk rather than implying safety.

---

## 1. Method, calibration, and the limits of this reading

Everything below was run on **2026-10-02** from `d:\Python\PlayKiln`, in a signed-out `en-US` Play
session. The durable record with every verbatim command and output is
**`docs/research/2026-10-02-assistive-and-communication-evidence.md`**; this proposal cites it and
does not restate its numbers from memory.

### 1.1 The sensor was not drifting

The calibration controls were run **in the same invocation** as the lane queries, as
`docs/niche-selection.md` requires:

```bash
node scripts/scan-market.mjs --concurrency 4 --query "aphasia communication app" --query "adult aac board" \
  --query "communication board adults" --query "stroke communication app" \
  --query "habit tracker" --query "bjj training log"
```

| query | verdict | expected | agrees |
|---|---|---|---|
| `habit tracker` | `fortress` | `fortress` | **yes** |
| `bjj training log` | `dead-zone` | `dead-zone` | **yes** |

**MEASURED.** No drift. A second, later invocation for the adjacent shelves in §11 re-ran both
controls and both agreed again.

### 1.2 The guard is a spot check, and the hand-classification is what this document relies on

All four lane queries were scored **`partial`** by the relevance guard, and
`docs/research/2026-09-24-market-decision.md` §2.1 records that guard passing shelves which do not
answer the query. So the titles were hand-classified:

```bash
node scripts/scan-titles.mjs --concurrency 3 --query "aphasia communication app" \
  --on-job "aphasia|stroke|communicat|aac|speech|talk|say" \
  --off-job "autism|kids|child|toddler|game|puzzle|learn english"
```

**MEASURED — hand job-share 20/30 (0.67)** against the guard's `partial`. The hand share is nearly
double the guard's token share, because four of the on-job cards do not carry a distinctive
`aphasia` token: `Speech Assistant AAC`, `Spoken – Tap to Talk AAC`, `Avaz AAC`,
`TypingTalk AAC Text-to-Speech`.

### 1.3 The honest limits, stated before any conclusion rests on them

- These are **display-capped Play result pages**, not market sizes. Play personalises and caps the
  result set, so "30 cards seen" describes the visible page.
- A guard verdict of `candidate` is a **spot check, never evidence**.
- The empty 512-install long tail is invisible to this sensor, so **"we saw no competitor" is not a
  reading** — only what appeared on a page is.
- **The leader's own critical reviews were not read in this session.** The sensor returned three
  reviews and **zero** rated 4★ or lower. Every claim in this proposal about the leader's user
  complaints is therefore avoided rather than guessed, and §11 of the evidence file names the command
  that would settle it.
- **No revenue API exists.** Every number below is hand-recorded from a public store page, and the
  four deduction rates are the plan assumptions of `docs/monetization-strategy.md` §2.2 rather than
  measurements of this developer account.

---

## 2. The shelf as it actually is

**MEASURED**, `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "aphasia communication app"`,
re-run at the end of this session and byte-identical to the earlier run:

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|---|---|---|---|---|---|---|---|---|
| 1 | `nl.asoft.speechassistant` | Speech Assistant AAC | 4.5 | 3,040 (3,036) | 500K+ | false | true | free |
| 2 | `com.myaphasiacoach` | My Aphasia Coach | 4.2 | 95 (95) | 10K+ | false | true | free |
| 3 | `com.cm.aac` | AAC Speech & Communication | 4.2 | 22 (22) | 10K+ | false | true | free |
| 4 | `ro.aphasia.app` | Aphasia | unrated | unknown | 1K+ | false | false | free |
| 5 | `com.tactustherapy.languagetherapy` | Language Therapy: Aphasia | 4.7 | 246 (246) | 5K+ | false | false | **paid $89.99** |
| 6 | `com.tactustherapy.apraxiatherapy.lite` | Apraxia Therapy Lite | 4.4 | 118 (118) | 10K+ | false | false | free |
| 7 | `com.constanttherapy.android.main` | Constant Therapy: Brain+Speech | 4.6 | 1,140 (1,141) | 100K+ | false | true | free |
| 8 | `com.tactustherapy.apraxiatherapy` | Apraxia Therapy: Aphasia | 4.6 | 137 (137) | 1K+ | false | false | **paid $29.99** |

Summary line from the same command: **ads declared 0 of 8; IAP declared 4 of 8; largest install
bucket 500K+.**

### 2.1 The shelf's central, measured gap: it is titled for children

**MEASURED**, §3.1 of the evidence file and the board-query enrichment:

- The three highest-install cards on `communication board adults` are framed for **children in their
  own titles**: `com.shmoontz.commboards.lite` — *CommBoards AAC – Autism Speech* (4.0★, 769 exact,
  50K+); `com.missingstep.aactalk.autism` — *AAC Talk: Autism Communication* (3.9★, 42, 5K+);
  `com.aac.autism.cards` — *AAC Autism Communication Cards* (unrated, 1K+).
- The guard reported, verbatim: **`"adult" appears in no title`**.
- The cards that *do* serve adults narrow to a **clinical subtype** instead —
  *Language Therapy: Aphasia*, *Apraxia Therapy: Aphasia*, *My Aphasia Coach*.

This is a **content** gap, not a naming one: an adult who has lost speech is handed either a
children's picture board or a drill workbook. §12 records the honest counter-reading — a word no
incumbent bids on may also be a word nobody types.

---

## 3. The demand reading: the paid incumbents

The lane's whole risk is that assistive apps are free or charity-funded, so this is the reading that
decides whether the lane is proposable at all.

**MEASURED**, `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "aphasia therapy app"`:

| package | title | rating | reviews (exact) | installs | IAP | price |
|---|---|---:|---|---:|---|---|
| `com.tactustherapy.languagetherapy` | Language Therapy: Aphasia | 4.7 | 246 (246) | 5K+ | false | **paid $89.99** |
| `com.tactustherapy.languagetherapy.lite` | Language Therapy Lite | 4.6 | 486 (486) | 50K+ | false | free |
| `com.tactustherapy.apraxiatherapy` | Apraxia Therapy: Aphasia | 4.6 | 137 (137) | 1K+ | false | **paid $29.99** |
| `com.constanttherapy.android.main` | Constant Therapy: Brain+Speech | 4.6 | 1,140 (1,141) | 100K+ | true | free |

**Three facts this establishes, and one it does not.**

1. **A developer sells this audience outright, and is rated well for it.** $89.99 at 246 exact
   reviews and $29.99 at 137 exact reviews, both ≥ 4.6★. Willingness to pay is not charity-shaped
   here; it is a paid product with satisfied buyers.
2. **Free-to-download with money taken in app is the shape that reaches volume.** The 500K+ leader
   declares an IAP; the 100K+ card declares an IAP. The same developer's **free** Lite product
   reaches **50K+ installs with 486 exact reviews** while its **$89.99** sibling reaches **5K+**. The
   free funnel is 10× the paid one, from one developer, on one job.
3. **A one-time unlock is bought on a communication board, described by the buyer.**
   `com.shmoontz.commboards.lite`'s own reviewer writes: *"I purchased the full version to add
   pictures, remove items, and rearrange it however I want."*
4. **What this does not establish:** that anyone buys a talking board at **$6.99**, or that a carer
   converts at any particular rate. Both are UNVERIFIED and named in §12.

### 3.1 The single strongest sentence in the pocket

**MEASURED**, §6.2 of the evidence file. `com.otsimo.aac` (**2.8★, 10K+**) draws a 2★ review:

> *"Set up freezes until you pay for a subscription. I do want the service and wanted to pay but I
> made the mistake of putting this on my child's phone wh…"*

A buyer **stating in Play's own review box that they wanted to pay** and being stopped by a
subscription that freezes setup. Willingness to pay is present on this shelf; a **rent-shaped ask** is
what blocks it. That is the recipe's entire premise, written by a user rather than inferred by me.

---

## 4. The LEADER — head to head

**Leader rule:** highest install bucket on the primary query, then highest exact review count.
On `aphasia communication app` this is unambiguous — one card is an order of magnitude ahead of the
next, and it also leads the board query.

| field | leader — `nl.asoft.speechassistant`, *Speech Assistant AAC* (A-Soft-nl) |
|---|---|
| rating | **4.5** |
| reviews | **3,040 shown, 3,036 exact** |
| installs | **500K+** |
| ads | **false** (declared absent) |
| IAP | **true** |
| price | **free** to download |

**MEASURED**, twice, on `aphasia communication app` and again on `communication board adults`.

### 4.1 What the leader HAS that this slot does not

Stated honestly, and delimited by what was actually read — the leader's *feature list* was **not**
read in this session, so any claim about which features it does or does not ship would be invented
and is not made.

1. **3,036 exact reviews at 4.5★.** Social proof that a carer sees in the first two lines of the
   result card. A listing with zero reviews is asking that carer to be the first. This is the single
   largest disadvantage and there is no honest way to argue it away.
2. **A 500K+ install bucket and the ranked position that follows from it.** The listing sits at the
   top of the shelf's own queries and inherits installs from a decade of accumulated position.
3. **A live IAP funnel with an existing payer base.** Its IAP flag is `true` on a 500K+ install base,
   which means it has already learned, with a real population, what this audience will buy — and this
   slot is going to market with an unmeasured price assumption where the leader has data.
4. **Being first.** *Speech Assistant AAC* is the generic-shaped answer to this job. Much of the
   shelf's traffic arrives already knowing its name.

### 4.2 The leader's own critical complaints — not read, and said so

```bash
node scripts/scan-complaints.mjs --package nl.asoft.speechassistant --package com.spoken.app \
  --package com.shmoontz.commboards.lite --package com.tactustherapy.languagetherapy
```

Observed, verbatim:

```text
  nl.asoft.speechassistant  4.5★  500K+  reviews=3 critical=0 top=none unclassified=0
```

Play server-renders **three** reviews of its own choosing per page, and in this sample **none of the
leader's three was rated 4★ or lower**. `--query` mode cannot fill the gap because it selects the
**weakest-rated** incumbents by construction, which excludes a 4.5★ leader.

**So: this proposal makes no claim about why the leader's users are unhappy.** The tempting sentence —
"the leader is also a subscription app" — would be invented, and the record says so instead of
printing it. It is UNVERIFIED in §12 with the command that would settle it.

### 4.3 The closest direct competitor, whose complaints *were* read

`com.spoken.app` — *Spoken – Tap to Talk AAC* (Spoken Inc.), **3.8★, 100K+ installs**: its three read
reviews contained **two critical, both classified `paywall`**. This is the app whose shape is closest
to this recipe, and it is where the wedge's evidence is strongest:

- 3★ — *"The app is alright. not very fancy. Does what it says I guess. but those subscription
  plans!!! Ouch. I can't see paying as much as I would pay for a f…"*
- 1★ — *"This app is horrible. I paid for 1 year of premium features, but the app never updated to
  allow me to use those features. One year later, all of a sud…"*

### 4.4 The verdict: can a new listing take a first paying customer from this leader?

**Not from the leader's own query, and yes from the shelf's priced end — with one thing that has to
be true.**

- **Not from the leader's traffic.** A listing with zero ratings does not out-rank 3,036 reviews on
  the head query. Nothing measured here shows otherwise, and claiming it would be exactly the
  invented confidence `.cursor/rules/grounded-claims.mdc` exists to stop.
- **From the priced end, plausibly.** The measured paid benchmarks on this shelf reach **137 and 246
  exact reviews at $29.99 and $89.99**, at install buckets of 1K+ and 5K+. Those are *small*
  populations — the shelf pays at hundreds-of-buyers scale, not millions — and this slot's ask is
  **$6.99, one time**, against a subscription that the shelf's own reviewers describe as costing
  "as much as I would pay for a f[ull app]" and against a $24.99 one-time price that reviewers call
  "a bit much". A smaller ask into a shelf with a measured 137-to-246-buyer paid tier is a reachable
  pocket, and the arithmetic in §8 sizes it honestly rather than hoping.
- **The specific thing that would have to be true.** The **free starter board** would have to be good
  enough that a carer installs it, hears the device's own voice speak a tile with no account and no
  setup, and keeps the app installed long enough to meet the sheet. The purchase does not have to beat
  the leader on features; it has to beat it on the carer's *fear*, which the shelf's own reviews name
  in their own words — *"I don't care for things I have to sign up for and then reme[member]"*
  (`com.speakeasy.community`, 3.6★, 100K+). If the free tier is a crippled demo instead of a working
  board, the recipe fails, and that is the one condition this plan cannot verify before the build.

---

## 5. The complaints, and the wedge they define

**MEASURED**, three runs of `scripts/scan-complaints.mjs` (§6.1 of the evidence file):

| pocket | reviews read | critical | paywall | account | crash | missing-feature | abandoned | other |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| `aphasia communication app` | 15 | 9 | **7 (78%)** | 1 (11%) | — | 1 (11%) | 1 (11%) | 2 unclassified |
| `communication board adults` | 18 | 5 | **4 (80%)** | 2 (40%) | 2 (40%) | 2 (40%) | — | 1 notifications |
| explicit packages | 12 | 5 | **5 (100%)** | 1 | — | 1 | 1 | 1 data-loss |

**The dominant, cross-shelf complaint is monetisation**, consistent across three independent runs and
every query direction. The classifier is lexical and the sample is Play's three-per-page, so this is
the pocket's **vocabulary** rather than its complaint frequency — a distinction this proposal keeps
rather than flattens.

### 5.1 Every clause of the wedge answers a complaint a named user wrote

| the wedge clause | the verbatim complaint it answers |
|---|---|
| **The voice is never paywalled** | `com.avazapp.international.lite`, *Avaz AAC*, **3.3★, 100K+**, 1★: *"Actually insane how the one thing most needed, the voice feature, is a paid feature. Symbols I could care less about, but the voice? Really?"* |
| **One price, no renewal** | `com.spoken.app`, **3.8★, 100K+**, 3★: *"those subscription plans!!! Ouch."* — and the same app, 1★: *"I paid for 1 year of premium features, but the app never updated to allow me to use those features."* |
| **A price below the shelf's own complaint threshold** | `com.tactustherapy.conversationtherapy.lite`, **3.5★, 10K+**, 2★: *"And $24.99 for full version is a bit much."* |
| **No account, no trial wall** | `com.speakeasy.community`, *SpeakEasy*, **3.6★, 100K+**, 1★: *"Have to sign up for a free trial or it can't be used. Something else I'd forget to cancel."* |
| **The free tier works before any sheet** | `com.otsimo.aac`, **2.8★, 10K+**, 2★: *"Set up freezes until you pay for a subscription. I do want the service and wanted to pay…"* |
| **Authored `en` + `ru`, not an English board with machine output bolted on** | `org.pictalk.www.twa`, *PicTalk AAC*, **2.4★, 50K+**, 1★: *"This app is only good for English speakers."* — same app, 1★: *"Some words are randomly in French."* |
| **Every tile is an adult or ward need** | the three highest-install board-query cards are titled for autism and children (§2.1), so an adult is handed a child's picture book or a drill. |
| **A typed board one tap away, so no authored vocabulary is a cage** | `com.shmoontz.commboards.lite`, **4.0★, 769 exact, 50K+**, 3★: *"Food choices are poor, appear's that you can not add anything or cha[nge]"* |
| **Offline, no server, no network on the speech path** | `com.elelad.comboard`, **3.8★, 100K+**, 1★: *"This 'paid for' app keeps crashing ... absolutely shocking for a child with autism trying to communicate!!"* |

**One weakness is deliberately not answered**, and this is where the wedge must be honest:
`com.tactustherapy.languagetherapy` (4.7★, 5K+) draws a 3★ review from a carer who *did* buy:
*"have used the trial version of this for my husband who had a terrible stroke 4 weeks ago and he
liked it. have now bought the full version as advised"*. **The shelf's own buyers accept
clinical-exercise apps and pay well for them.** This slot does not compete with a therapy workbook —
it competes with a picture board — and pretending otherwise would be the invention this proposal
refuses to make. It is recorded as a risk in §12.

---

## 6. What this slot is, as a promise

Not a log, register, tracker, checklist, tally or calculator. The value is that the app **speaks**:
the user taps a tile and the phone says the words out loud, on device, with no network, no account and
no server. The vocabularies are **authored** — ward needs, daily needs, pain and comfort, family,
getting help — in `en` and `ru`, not generated and not scanned from a camera.

---

## 7. The promise contract

`docs/ideation/plans/tap-to-say.differentiators.json` — **6 promises, 3 of them `core`**, all starting
`deferred` per the contract, ready to hand to `new-slot.mjs`. Each carries its `MEASURED` evidence
from the recon record above.

| id | weight | the promise | the complaint it answers |
|---|---|---|---|
| `adult-needs-board` | **core** | Every tile is an adult need or a ward need, phrased the way an adult says it, so a stroke survivor is never handed a picture book of a child's wants. | the three highest-install board-query titles are autism/child-framed; `"adult" appears in no title` |
| `voice-never-paywalled` | **core** | The device's own text-to-speech speaks a tile before any purchase and after it; the unlock buys breadth, never the voice. | Avaz 3.3★, 100K+: *"the voice feature, is a paid feature… Really?"* |
| `one-price-no-renewal` | **core** | One purchase ends it — no renewal, expiry, trial clock or second sheet, because there is no subscription, no account and no server. | paywall anger 7/9, 4/5, 5/5 across three pockets; Spoken 3.8★, 100K+ |
| `offline-no-account` | supporting | The board opens and speaks in airplane mode with no sign-in, so a carer can hand the phone over in a ward with no usable wifi. | Speakeasy 3.6★, 100K+: *"I don't care for things I have to sign up for"* |
| `born-bilingual-board` | supporting | Authored `en` and `ru` tiles, not an English board with machine output bolted on. | PicTalk 2.4★, 50K+: *"only good for English speakers"*; *"randomly in French"* |
| `type-to-say-escape-hatch` | supporting | A typed board one tap from the tile board, so a need the tiles miss is still speakable. | CommBoards 4.0★, 769 exact: *"you can not add anything"* |

Losing or weakening a `core` promise needs the owner's own words in `ownerDecision`, recorded — never
inferred. Proofs are written against symbols and files inside the slot, so `verify.mjs --slot` fails
until each promise resolves beyond `deferred`.

---

## 8. The money

**Paying user.** A family carer, spouse or adult child setting up a phone for someone who cannot
speak reliably after a stroke or with aphasia, paying out of pocket in the first weeks home or at a
hospital bedside.

**What is bought.** One permanent unlock of the full authored adult board: every ward and daily-needs
tile in `en` and `ru`, unlimited personal phrases, and the enlarged high-contrast tile layout.

**Price: $6.99. Archetype: `purchase_led_single_unlock`. Capability: `billing`. Moment of payment:**
after the free starter board has already spoken a tile out loud in the device's own TTS.

### 8.1 The archetypes rejected, each with a **revenue** reason

| rejected | revenue reason |
|---|---|
| `purchase_led_repeat_sales` | **There is no second thing to sell that a person who cannot speak buys again.** The board is a complete vocabulary, not a consumable. The only repeat sale available is extra tiles in instalments — selling the ability to be understood piece by piece, which is the same shape as the rent the measured complaints are about. A consumable priced below the $6.99 whole-board unlock would cannibalise it rather than add to it. |
| `ads_led_utility` | **The money on this shelf is not advertising money, measured.** 7 of 8 enriched incumbents on the primary query declare no ads at all; the one shelf card that declares ads sits behind a child-directed title. The user is mid-sentence and frequently at a bedside, so an interstitial is not a tolerable interruption, and a banner earns nothing at this session length. An ad-led build would reproduce the complaint *and* earn nothing. |
| `purchase_only_no_ads` | **Every incumbent that reaches real volume here is free to download with the money taken in app** — the 500K+ leader declares an IAP, the 100K+ cards declare IAPs. The only outright-paid benchmarks on this job reach 1K+ and 5K+ installs, while the *same developer's* free Lite reaches 50K+. A listing price spends a proven free funnel for an unproven one. |
| `subscription` | **Banned outright** by the owner on 2026-10-01 and machine-enforced: `subscription` is absent from `ARCHETYPES`, so any plan naming it fails `parseEconomics`. It is named here only because it is *precisely* what the measured complaints are about — the strongest single review in the pocket is a buyer saying they wanted to pay and were stopped by a subscription. |

### 8.2 The unit arithmetic, with the division shown

Deductions are the plan assumptions of `docs/monetization-strategy.md` §2.2 — `F_billing = 0.15`
(the first-$1M annual tier), `F_refund = 0.02`, `F_fx = 0.02`. They are **assumptions, not
measurements of this account**, and the fee tier rises above $1M/year.

```text
price                              $6.99
× (1 − F_billing 0.15)  =  6.99 × 0.85   =  $5.9415
× (1 − F_refund  0.02)  =  5.9415 × 0.98 =  $5.82267
× (1 − F_fx      0.02)  =  5.82267 × 0.98 = $5.7062166
─────────────────────────────────────────────────────
NET PER SALE                               ≈ $5.71
        6.99 × 0.85 × 0.98 × 0.98 = 6.99 × 0.81634 = 5.7062
```

**Sales per month to clear each target**, at `netPerSale = $5.7062`:

```text
TARGET  $1,000 / month     1,000 ÷ 5.7062 = 175.2  → 176 sales / month
KILL FLOOR  $300 / month     300 ÷ 5.7062 =  52.6  →  53 sales / month
```

**The same figures as installs**, at the assumed **2% install → unlock** conversion:

```text
176 sales ÷ 0.02 = 8,800 installs / month      (the $1,000 target)
 53 sales ÷ 0.02 = 2,650 installs / month      (the $300 floor)
```

**The target and where it comes from.** `$1,000/month` is the illustrative figure
`docs/monetization-strategy.md` §2.1 uses throughout ("Illustrative order-of-magnitude implications
for **$1,000/month**"), and it is the number every claim of "a viable slot" in this factory is
measured against. `$300/month` is the kill floor: below it the slot does not pay for the attention it
takes. Both are **monthly**; §8.3 converts the floor to the 90-day window the schema measures.

### 8.3 Kill criteria, derived rather than chosen twice

`killCriteria` is a **cumulative reading over a window** (`scripts/lib/outcomes.mjs` compares
`installCount` and `revenueUsd` against the floors, and reports `ON-TRACK` only when **both** are
met). Two independently chosen floors can contradict each other, so the install floor is **derived**
from the revenue floor at the assumed conversion:

```text
kill floor $300 / month × 3 months (90-day window)     = $900 cumulative
$900 ÷ $5.7062 per sale                                = 157.7 → 158 unlocks
158 ÷ 0.02 conversion                                  = 7,880 → 8,000 installs
                                                          (≈ 2,667 / month)
check: 8,000 × 0.02 = 160 unlocks × $5.7062            = $913.0  ≥  $900  ✓
```

```json
"killCriteria": { "windowDays": 90, "minInstalls": 8000, "minRevenueUsd": 900 }
```

**The three finite numbers are `90`, `8000`, `900`**, and the derivation above is why the pair is
self-consistent rather than two round numbers that happen to sit side by side.

### 8.4 Capabilities, permissions, server, build

| item | value |
|---|---|
| capabilities required | **`billing`** only |
| deliberately not enabled | **`ads`** — the measured defect this recipe answers is monetisation pressure on a person trying to speak, and an ad over a board mid-sentence reproduces exactly what the pocket's critical reviews are angry about |
| permissions | No runtime permission beyond Play Billing's own `com.android.vending.BILLING`. TTS binds the device's installed engine over `android.intent.action.TTS_SERVICE`, which needs **no permission**. **No** microphone, camera, storage, contacts, location, notification or background work. No network use for content. |
| server required | **No.** Every tile, phrase and translation ships inside the APK; speech is the device's own TTS engine. Nothing to host, nothing to serve, nothing to expire. |
| build estimate | **6 agent hours** |
| minimum SDK / stack | the factory standard: Kotlin + Compose + Material 3, `minSdk 26` — which matters here because the audience's phones are often older hand-me-downs. |

---

## 9. The name

`docs/ideation/plans/tap-to-say.naming.json` — **6 scored candidates spanning 4 archetypes**,
**7 rejected alternatives**, chosen candidate **82/100**, the runner-up at 81, and the two negative
controls at 51 and 49.

**Chosen: `TapToSay: AAC Board`** — 19 characters, **11 of slack** against Play's 30-character cap;
`ru` `TapToSay: доска общения`, 23 characters, 7 of slack.

| # | candidate | archetype | score | why it stands or falls |
|---|---|---:|---:|---|
| 1 | **TapToSay: AAC Board** | carrier_job_noun | **82** | Names the exact physical act — tap a tile and it says the words. Decisive here because **the user may not be able to read**: a metaphor the buyer must decode is a real cost and an instruction is a feature. One canonical spelling, clean radio test, clean on Play. |
| 2 | PhrasePilot: AAC Board | carrier_job_noun | 81 | The genuine runner-up. "Pilot" does mean to guide, and it is the more ownable of the two — but its promise is a **metaphor**, and in `ru` *pilot* reads as aviation and has to be explained. |
| 3 | SpeakHarbor: AAC Board | evocative | 78 | Cleanest trademark headroom of the three serious candidates (one unrelated UK radio app on Play). Loses on job fit: a harbour is a feeling, not a job, and a carer scanning results learns nothing from it. |
| 4 | TalkLoom: AAC Board | compound | 72 | No live title contains `talkloom` — but Play **fuzzy-matched the token to WhatsApp, Messenger, KakaoTalk and Snapchat**, so the joined word carries no index weight and the descriptor would carry the entire findability burden. A carrier the store cannot see is not a carrier. |
| 5 | AAC Board | descriptive | 51 | **Negative control 1:** the shelf's head term. Every incumbent that matters already bids on it, including the 500K+ / 3,036-review leader. |
| 6 | Talker | descriptive | 49 | **Negative control 2:** the AAC field's own word for the *hardware*, not for this app's job. Owns nothing, tells a carer nothing. |

**Rejected on collision, not on score** — five carriers eliminated by a **named live entity**, which
is the hard check that outranks any number:

- `TalkTiles` → `org.velassi.talktiles` ships *TalkTiles AAC Cards*, the same category and job.
- `WordLoom` → `com.wordloom.app` ships *WordLoom: Word Search Puzzles*.
- `TalkBridge` → **both** `com.talkbridge` and `com.margaspt.talkbridge` are live.
- `BoardSpeak` → `com.SpeakBoard` ships *SpeakBoard*, the same two words reversed.
- `WordTile` → multiple live word-puzzle games.

**The honest trade, recorded rather than hidden.** The winner gives up ownability: a two-word English
instruction phrase is weakly protectable. The decision rests on the measured collision field (eleven
carrier auditions, five dead by collision) plus one product judgement — that a user who cannot read
gets nothing from a metaphor. The near-tie with the runner-up is left **visible** rather than dressed
up, and trademark headroom is **UNVERIFIED**: no register was queried, and a clean Play search is not
clearance.

---

## 10. The policy gate — answered, not assumed

The lane's policy risk is real and is stated plainly rather than worked around.

**Families.** Google's trigger is the **Target audience and content** declaration, and Google may
override it: *"Regardless of what you identify in the Google Play Console, if you choose to include
imagery and terminology in your app that could be considered targeting children, this may impact
Google Play's assessment of your declared target audience"*
(<https://support.google.com/googleplay/android-developer/answer/9893335>, read 2026-09-23,
`docs/research/2026-09-23-autism-app-policy.md` §1.2).

- **The accurate declaration for this slot is adults**, and it is accurate on the product's own terms
  rather than as a label: the patient is an adult, the buyer is an adult, and the shipped app is an
  adult board whose entire differentiator is that it is **not** the child-framed board the shelf's
  leaders ship.
- **The obligation that follows is on the build, not just the form:** no child-directed imagery, no
  cartoon characters, no childish animation, no child-directed terminology — in the app *or* the
  listing. `docs/research/2026-09-23-autism-app-policy.md` §1.2 records that child-appealing store
  graphic elements can get a listing rejected even where the app is not for children, and that
  mis-declaring the target audience is itself an enforcement trigger.
- **A parents/carers-only declaration is not used as a shortcut here**, because it is not needed: the
  audience genuinely is adults. Where the §1.2 override risk remains — Google disagreeing with an
  accurate adult declaration — it is recorded as residual risk in §12 rather than declared closed.

**Health claims.** The listing must **describe functionality literally** and make **no claim about a
condition, a person's development, or an outcome**. The line is quoted in
`docs/research/2026-09-23-autism-app-policy.md` §2.2: *"Shows what happens next, as pictures, in a
sequence you choose"* is **permitted**; *"improves communication", "therapy", "treatment",
"clinically proven", "recommended by therapists"* is a **health/medical claim — do not ship**. So:

- The listing says what the app does — *a board of tiles that speaks the words when tapped* — and never
  that it improves speech, aids recovery or substitutes for speech therapy (which it does not, and
  saying so would also be untrue).
- The **Health apps declaration** is mandatory for **all** apps on Play, including ones with no health
  features: a developer must either declare in-scope features or *"certify that no health features are
  offered through the app"*
  (<https://support.google.com/googleplay/android-developer/answer/14738291>). Whether this slot's
  literal-functionality description falls in scope or out is a **Play Console form question, not a
  policy text question**, and it is recorded as owed rather than answered.
- **Ads: none.** The ads declaration is truthfully "no", which also keeps the Families advertising
  rules out of scope entirely.

---

## 11. Visual direction

**One direction, and no other lane may reuse it.** The subject is **a ward at night**: a deep
slate-blue field, unnaturally calm, with one warm amber reserved for the tile the person is about to
tap — so colour means *this one will speak* and nothing else. The mark is not a speech bubble (the
stock symbol of every app on this shelf) but **a single finger descending onto a square**, drawn from
`studio.json`'s `mark` and legible at 48 dp.

The slot-owned component, and what a stock Material screen does not do: **a tile that is the whole
interaction.** It is not a `Card` with an `onClick` — it is a large high-contrast target with a
built-in dwell affordance and a press state that visibly *opens outward* as the words are spoken, so a
person with a tremor, one working hand or slowed motor control can see that the tap landed and that
the phone is now speaking for them. Every tile carries a text label beside its glyph, and the entire
board is usable at 200% font scale, because the user may be reading at arm's length with one eye.

---

## 12. Honest unknowns

Every row is **UNVERIFIED**; each names the command or act that would settle it. None of them is
closed by this recon, and the plan's `assumptionsUnverified` carries the same list on the record.

| # | unknown | how it would be settled |
|---|---|---|
| 1 | **Willingness to pay $6.99 outright on this exact job.** The only measured one-time purchases on the shelf are CommBoards' unlock (price not visible on its card) and Tactus at $29.99/$89.99. No reading shows a carer buying a *talking board* at $6.99 rather than starting free. | the first 90 days of the slot's own sales, or a price test in Play Console |
| 2 | **Install → unlock conversion, assumed at 2%.** No slot in this repository has ever measured a conversion rate, so **every installs and revenue figure above is an assumption.** | Play Console's conversion metric after 90 days; today no source exists |
| 3 | **The leader's own critical complaints** were not read — the sensor returned 3 reviews, 0 critical, and `--query` mode excludes a 4.5★ app by construction. Any sentence claiming *why* its users are unhappy would be invented. | a human reading the leader's full review list on its Play page (sorted by lowest rating); the sensor cannot page past Play's server-rendered three |
| 4 | **Whether anyone searches the word "adult" for this job.** The guard reported `"adult" appears in no title`, which is consistent with both an unserved job and a word nobody types — the two readings are not separable from this session's data. | Play search autocomplete for `aac` and `communication board`, read in a signed-in session |
| 5 | **Whether the free voice converts better than a gated one.** No reading here shows that; what is measured is that buyers describe the locked voice and the subscription as the reason they are angry. | a live A/B on the slot's own funnel, after shipping |
| 6 | **Search ranking.** The shelf is led by 500K+ installs and 3,036 exact reviews, and nothing measured here shows a new listing reaching even the 2,667 installs a month the kill floor requires against it. | Play Console acquisition data for the first 30 days |
| 7 | **Whether `com.gexetr.taptosay` is free on Play.** No repository tool queries package availability. | a Play Console "create app" attempt, or a Play search for the package name |
| 8 | **Trademark headroom for `TapToSay` in class 9.** USPTO, EUIPO, WIPO and the `ru` national register were **not** queried. | those four registers; a clean Play search is not clearance |
| 9 | **Whether the §1.2 override risk applies to an accurate adult declaration** on a shelf whose leaders are child-framed, and **whether the literal-functionality listing triggers the Health apps declaration** in scope rather than out. | the Play Console Target-audience and Health-apps forms, walked honestly at submission |
| 10 | **The shelf's own buyers accept clinical-exercise apps and pay well for them** — `tactustherapy.languagetherapy` at 4.7★ / $89.99 has a carer who *bought* and was satisfied. This slot does not compete with a therapy workbook and must not claim to. | nothing in this recon; it is a strategic risk the owner should weigh, not a gap to close |

---

## 13. Refusals — the lane's other shelves, and why they are not the recipe

**Calibration was re-run in this invocation and did not drift** (**MEASURED**): `habit tracker`
returned `fortress`, `bjj training log` returned `dead-zone`.

```bash
node scripts/scan-market.mjs --concurrency 4 --query "magnifier app" --query "live transcription app" \
  --query "dyslexia reader" --query "speech therapy practice app" --query "habit tracker" --query "bjj training log"
```

| shelf | verdict | what was seen | why it is not the recipe |
|---|---|---|---|
| `magnifier app` | `fortress` | 30 seen, 20/30 rated, 12 of 20 ≥ 4.5★, only 5 weak, median 4.60 | **A fortress, and the paid case was never measured.** No enrichment was run, so whether any paid incumbent exists here is **UNVERIFIED** — but the shape is fatal regardless: the job *is* the operating system. Android ships a magnifier, and the shelf's rated leaders are strong, so the wedge would have to be a capability rather than a name, and none was found in this pass. **Refused for no measured wedge and no measured paid incumbent**, not for crowding. |
| `live transcription app` | `mixed`, relevance **`POLLUTED`** | 20 seen, 19/20 rated, 7 weak, median 4.20 | **Refused structurally, twice over.** Live transcription of a nearby speaker is a cloud-speech pipeline with a server on the critical path — the factory does not build those (`client-secrets.mdc`; the brief's own prohibition). And Android's Live Transcribe is free and often preinstalled. A server cost with a free OS incumbent is a revenue refusal. |
| `dyslexia reader` | `candidate` (a spot check) | 30 seen, 22/30 rated, **14 weak** — the weakest-rated of the four — median 4.05; **Oxford University Press** surfaced as a publisher | **Refused for two revenue reasons.** The differentiator that would matter is *the content*, and the best content is licensed by publishers — OUP appearing on the shelf is the wall, not the invitation. And the build the job implies is a camera/OCR or text-ingest pipeline, which the brief explicitly excludes. The shelf being weak is a **demand** signal, not a licence to enter with something we cannot legally or cheaply supply. |
| `speech therapy practice app` | `mixed` | 30 seen, 20/30 rated, 9 weak, median 4.25 | **Refused as a duplicate of an already-assigned lane, and as a worse shape.** This is authored drill *content* — lane (A) of this same turn — and two of this factory's four recipes would then be one sentence. It is also a **practice/drill** shape, which is not the "communicates or accesses the world" job this lane is chartered for; the app would grade answers rather than speak for a person. |

**No recipe was proposed for any of these, and none was invented to avoid saying so.** The one shelf
with measured paid demand, measured complaint text and a shape this factory can actually build was the
AAC shelf, so it is the one proposed.

---

## 14. Scaffold command

```bash
node scripts/new-slot.mjs \
  --idea "A tap-to-talk AAC board for adults after a stroke: the phone speaks the words they tap, offline, with the voice free forever and one lifetime unlock." \
  --economics      docs/ideation/plans/tap-to-say.economics.json \
  --differentiators docs/ideation/plans/tap-to-say.differentiators.json \
  --naming          docs/ideation/plans/tap-to-say.naming.json
```

`--slug` and `--name` are omitted on purpose: `new-slot.mjs` derives both from the naming contract and
refuses a supplied value that disagrees, so the name is stated once. The evidence this plan cites is
already promoted into `docs/research/2026-10-02-assistive-and-communication-evidence.md`, which is
what `evidenceRef` requires.

---

## 15. Gate verdicts, pasted as observed

```text
$ node scripts/check-economics.mjs --economics docs/ideation/plans/tap-to-say.economics.json
INFO     plan: D:\Python\PlayKiln\docs\ideation\plans\tap-to-say.economics.json
PASS     economics schema — archetype purchase_led_single_unlock, priceUsd 6.99
SKIP     capability pairing is enforced later by `node scripts/check-economics.mjs --slot <slug>`
PASS     evidenceRef resolves on disk: docs/research/2026-10-02-assistive-and-communication-evidence.md

VERDICT: PASS
exit 0
```

```text
$ node scripts/check-differentiators.mjs --differentiators docs/ideation/plans/tap-to-say.differentiators.json
INFO     contract: docs/ideation/plans/tap-to-say.differentiators.json
PASS     contract schema — 6 promise(s), 3 core, 0 shipped, 0 narrowed, 0 dropped, 6 deferred
SKIP     artifacts are enforced later by `node scripts/check-differentiators.mjs --slot <slug>`
INFO     6 promise(s) start deferred; the slot gate fails until each one is resolved

VERDICT: PASS
exit 0
```

```text
$ node scripts/check-naming.mjs --naming docs/ideation/plans/tap-to-say.naming.json --validate-only
INFO       naming: D:\Python\PlayKiln\docs\ideation\plans\tap-to-say.naming.json
PASS       naming schema — title "TapToSay: AAC Board", archetype carrier_job_noun, 6 candidate(s), 7 rejected
SKIP       the pack cross-check is enforced later by `node scripts/check-naming.mjs --slot <slug>`
SKIP       evidence file check skipped by --validate-only

VERDICT: PASS
exit 0
```

`SKIP` on the artifact half is the correct and expected output for a proposal: the capability pairing,
the artifact proofs and the pack cross-check are all enforced against a slot that does not exist yet.

---

## 16. Claim ledger

| claim | label | evidence |
|---|---|---|
| the sensor was not drifting | **MEASURED** | `scan-market.mjs` same-invocation controls: `habit tracker` → `fortress`, `bjj training log` → `dead-zone`, twice |
| hand job-share on the primary query is 20/30 | **MEASURED** | `scan-titles.mjs --concurrency 3 --query "aphasia communication app" --on-job "aphasia\|stroke\|communicat\|aac\|speech\|talk\|say" --off-job "autism\|kids\|child\|toddler\|game\|puzzle\|learn english"` |
| the leader is Speech Assistant AAC, 4.5★, 3,036 exact reviews, 500K+, free, IAP, no ads | **MEASURED** | `enrich-market.mjs --top 8 --concurrency 3 --query "aphasia communication app"`, re-run and identical |
| a developer sells this audience outright at $89.99 and $29.99, at 246 and 137 exact reviews, 4.7★ and 4.6★ | **MEASURED** | `enrich-market.mjs --top 8 --concurrency 3 --query "aphasia therapy app"` |
| the same developer's free Lite reaches 50K+ installs / 486 exact reviews while its $89.99 sibling reaches 5K+ | **MEASURED** | same command as above |
| a one-time unlock is bought on a communication board | **MEASURED** | `scan-complaints.mjs` verbatim review on CommBoards AAC: *"I purchased the full version to add pictures, remove items, and rearrange it"* |
| buyers here state they wanted to pay and were stopped by a subscription | **MEASURED** | `scan-complaints.mjs` verbatim review on `com.otsimo.aac` (2.8★, 10K+) |
| paywall obsession is the dominant critical theme: 7/9, 4/5, 5/5 | **MEASURED** | three `scan-complaints.mjs` runs (§6.1 of the evidence file) |
| the field's own users call a locked voice insane | **MEASURED** | verbatim 1★ on Avaz AAC (3.3★, 100K+) |
| the highest-install board-query cards are child/autism-framed, and `"adult" appears in no title` | **MEASURED** | `enrich-market.mjs --top 8 --concurrency 3 --query "communication board adults"` + the guard's own printed line |
| the leader's critical reviews were not read | **MEASURED** | `scan-complaints.mjs --package nl.asoft.speechassistant …` printed `reviews=3 critical=0` |
| `magnifier app` is a fortress; `live transcription app` is relevance-polluted; `dyslexia reader` is `candidate` with 14 weak; `speech therapy practice app` is `mixed` | **MEASURED** | the §13 `scan-market.mjs` invocation, same-run controls clean |
| TapToSay and the runner-up are clean on Play; five carriers died on a named collision | **MEASURED** | `enrich-market.mjs` per carrier, 2026-10-02; zero cards for `TapToSay`, one unrelated for `SpeakHarbor` |
| $1,000/month is the working target | **ON RECORD** | `docs/monetization-strategy.md` §2.1 |
| the trigger and override rules for a child-inclusive target audience, and the "do not ship" health-claim list | **ON RECORD** | `docs/research/2026-09-23-autism-app-policy.md` §§1.1–1.3, 2.2, quoting Play Console Help read 2026-09-23 |
| the Health apps declaration is mandatory for all apps | **ON RECORD** | same file §7 table, quoting <https://support.google.com/googleplay/android-developer/answer/14738291> |
| `F_billing 0.15`, `F_refund 0.02`, `F_fx 0.02` | **ON RECORD** | `docs/monetization-strategy.md` §2.2, each marked ASSUMPTION there and re-marked here |
| $6.99 nets $5.71 and needs 176 sales/month for $1,000, or 53 for $300 | **INFERRED** | the division in §8.2, from the price and the three assumed rates |
| 8,000 installs / 90 days is the derived install floor for the $900 window | **INFERRED** | §8.3, from `$900 ÷ $5.7062 ÷ 0.02`, and it clears the floor at $913 |
| a new listing can take a first paying customer from this leader | **UNVERIFIED** | no reading shows it; §4.4 states the one condition that would have to be true, and the shelf's paid tier is measured at only 137–246 buyers |
| willingness to pay $6.99 outright on this job | **UNVERIFIED** | no such purchase was observed; `enrich-market.mjs` cards, then the slot's own 90-day sales |
| install → unlock conversion is 2% | **UNVERIFIED** | no slot has ever measured a conversion rate; Play Console after 90 days |
| why the leader's users are unhappy | **UNVERIFIED** | `scan-complaints.mjs --package nl.asoft.speechassistant` gave 3 reviews / 0 critical; a human reading its full lowest-rated review list |
| anyone searches the word "adult" for this job | **UNVERIFIED** | Play autocomplete for `aac` / `communication board`, signed-in |
| a free voice converts better than a gated one | **UNVERIFIED** | a live A/B on the slot's own funnel, after shipping |
| search ranking against 3,036 reviews | **UNVERIFIED** | Play Console acquisition data, first 30 days |
| `com.gexetr.taptosay` is free | **UNVERIFIED** | a Play Console "create app" attempt |
| trademark headroom for `TapToSay` in class 9 | **UNVERIFIED** | USPTO, EUIPO, WIPO, `ru` register — none queried |
| whether the literal listing triggers the Health apps declaration in scope | **UNVERIFIED** | the Play Console Health-apps form, at submission |

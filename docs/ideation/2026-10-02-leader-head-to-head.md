# Leader head-to-head — three recon recipes against the app they must actually beat

Written 2026-10-02 by the market analyst role. The owner's question: for each of the three recon
recipes, compare the promised wedge **head-to-head against the leader of its shelf** — the app a new
listing must beat to win a first paying customer — and answer, with measured numbers, whether the
wedge survives contact with the leader or the recipe's "weak mid-tier" story is true only of the apps
nobody downloads.

Every factual claim is labelled per `.cursor/rules/grounded-claims.mdc`:
**MEASURED** (a command was run in this session and its output is quoted), **ON RECORD** (a named
document states it and was read), **INFERRED**, or **UNVERIFIED** (with the command or file that
would decide it). Every MEASURED row names the command that produced it.

Read boundary observed: nothing under `apps/**` was opened. Sources read: the three proposals, their
`plans/*.json` and evidence files, and the live Play storefront through `scripts/*.mjs`.

---

## 0. Method, and the criterion used to define a leader

For each recipe the **primary query** is `searchPhrases[0]` of its own `naming.json`, because that is
the query the recipe intends to be found on:

| Recipe | Primary query | Source |
|---|---|---|
| `rast-saz-oud-tuner` | `saz tuner` | `plans/rast-saz-oud-tuner.naming.json` `searchPhrases[0]` |
| `rook-encounter-generator` | `ttrpg encounter generator` | `plans/rook-encounter-generator.naming.json` `searchPhrases[0]` |
| `part-107-drills` | `part 107 practice test` | `plans/part-107-drills.naming.json` `searchPhrases[0]` |

**The leader criterion, stated before the data was read:** run
`node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "<primary query>"`, then take the
**highest install bucket**; if several apps share it, take the **highest exact review count** (the
unrounded schema.org twin). Install buckets are rounded floors, so this is a floor comparison.

**One adjustment, made explicit rather than silent.** A query that names an instrument returns the
generic fortress alongside the job-specific shelf. Where the mechanical criterion picks an app whose
*job* is not the recipe's job, the report names **two** leaders: the **criterion leader** (mechanical,
per the instruction) and the **on-job leader** (the highest-ranked app whose job is actually the
recipe's). Both are profiled, and the verdict says which one drove it.

---

## 1. `rast-saz-oud-tuner` — "Rast: Saz & Oud Tuner", $4.99 one-time microtonal tuner

### 1.1 The leader, by the criterion

`node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "saz tuner"` → 30 visible cards,
verdict `mixed`, relevance guard `on-topic` (30/30). The top 10, as printed (**MEASURED**, that
command):

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|--:|---|---|---:|---|---|---|---|---|
| 1 | `com.visne.baglamatuner` | Baglama Tuner: Bağlama & Saz | 4.4 | 8,020 (8,023) | 1M+ | true | true | free |
| 2 | `com.saztunes.app` | SazTunes - Learn to play the saz | unrated | — | 50K+ | false | true | free |
| 3 | `com.baglamatuner` | Baglama Tuner | 4.2 | 2,560 (2,555) | 1M+ | true | false | free |
| 4 | `saz.tuning` | Saz Tuning | unrated | — | 100+ | false | false | free |
| 5 | `com.kme.baglamatuner` | Baglama Tuner - Bağlama / Saz | unrated | — | 50K+ | true | true | free |
| 6 | `org.umitates.baglamatuner` | Saz Tuner & Bağlama Lessons | unrated | — | 100K+ | true | true | free |
| 7 | `com.fiberapps.saztuner` | Saz & Guitar Tuner | unrated | — | 100+ | true | false | free |
| 8 | **`com.soundcorset.client.android`** | **Tuner & Metronome** | **4.6** | **174,000 (174,180)** | **50M+** | **true** | **true** | **free** |
| 9 | `com.hakankucuk.baglamatuning` | Baglama, Saz Tuner | unrated | — | 10K+ | false | false | free |
| 10 | `pl.netigen.toolstuner` | Perfect Tuner | 4.1 | 87,500 (87,501) | 10M+ | true | true | free |

- **Criterion leader (MEASURED):** `com.soundcorset.client.android` — **50M+**, the highest install
  bucket, and **174,180** exact reviews, the highest in that bucket. **4.6★, ads, IAP, free.**
- **On-job leader (MEASURED, same command):** `com.visne.baglamatuner` (CherrySoft Limited) —
  **1M+** and **8,023** exact reviews, the highest of the apps whose title names a saz/bağlama tuner.
  **4.4★, ads, IAP, free.** Its runner-up is `com.baglamatuner` (Morfozz, **1M+, 4.2★, 2,555**).

**The recipe's own leader claim.** The proposal names the shelf leaders as the two 1M+ baglama
tuners at 4.2–4.4★ (`2026-10-02-niche-real-time-tools.md` §7). That matches the **on-job** leader,
not the criterion leader: the mechanical leader of the literal `saz tuner` query is a **4.6★/50M+
generic tuner**, and my hand-classification of the same page with `scan-titles.mjs`
(`--on-job "(saz|baglama|...)" --off-job "(guitar|...)"`) put only **9/30 (0.30)** cards on the saz
job (the recipe's looser regex records **20/30 = 0.67**, ON RECORD, same file §3). Either way, the
page's leader by size is not a saz app.

### 1.2 Leader profile

| | Criterion leader `com.soundcorset.client.android` | On-job leader `com.visne.baglamatuner` |
|---|---|---|
| Rating | 4.6★ | 4.4★ |
| Exact reviews | 174,180 | 8,023 |
| Installs | 50M+ (≥ 50,000,000) | 1M+ (≥ 1,000,000) |
| Ads / IAP / price | true / true / free | true / true / free |
| Job | generic chromatic tuner + metronome + practice tones | bağlama/saz tuner (same developer ships a 500K+ oud tuner) |

(source: the enrich command above, **MEASURED**)

### 1.3 The leader's own complaints, quoted

`node scripts/scan-complaints.mjs --package com.soundcorset.client.android --package
com.visne.baglamatuner --package com.baglamatuner` — 9 reviews read, 4 critical (**MEASURED**):

- **`com.soundcorset.client.android` (4.6★, 50M+), 2★:** *"I've used the app for years, and they've
  just decreased the number of voices available to four, for the free app. Almost $6 per month to get
  the two voices back on my rhythm settings is absurd. I used to love this app, but I'm looking for
  something else. I know many pay once software programs will allow me to make loops for rhythmic
  practice. A huge monthly fee! yuck."* — classified `ads` (1 of its 2 critical).
- **`com.soundcorset.client.android` (4.6★, 50M+), 3★:** *"Overall a useful app, but it would be nice
  if there was a way to set the tuner to confine itself to a certain frequency range. It has a bad
  habit of picking up harmonics and tuning off of those instead of the note you're actually playing.
  Also, call me old fashioned, but I like to buy things that are useful to me instead of renting
  them... I'd happily pay a few dollars for an ad-free version, but I'm not going to pay $1/month in
  perpetuity just for a tuner."* — classified `missing-feature`.
- **`com.visne.baglamatuner` (4.4★, 1M+), 0 critical in the 3-review sample** — the page's three
  shown reviews are 5★/4★/5★ ("Absolutely wonderful! Amazing quality and easy to use!").
- **`com.baglamatuner` (Morfozz, 4.2★, 1M+), 2 critical:** 1★ *"Not detecting any sounds"*; 1★
  *"This app freez and dont hear my saz"* (classified `crash`).

Shelf-wide, `node scripts/scan-complaints.mjs --query "saz tuner" --top 6 --concurrency 2` — 18
reviews, 12 critical: **ads 7 (58%)**, crash 2 (17%), paywall 2 (17%), regression 2 (17%), account 1,
accuracy 1, broken 1, offline 1, value 1 (**MEASURED**). The weakest-rated apps selected were
generic tuners (`pl.netigen.toolstuner` 4.1★/10M+, `com.jrinnovation.proguitartuner` 4.1★/10M+,
`com.bork.dsp.datuna` 4.1★/10M+) plus Morfozz's saz tuner.

### 1.4 Head-to-head — each promised wedge clause against the leaders

The recipe's wedge, verbatim from `plans/rast-saz-oud-tuner.economics.json` `distinctnessSentence`:
*"One paid tuner that reads the makam intervals in cents and keeps a stable lock on the low saz and
oud courses while never showing an ad, with every traditional tuning included in a single one-time
$4.99 unlock instead of split across a $15/week subscription or a full-screen ad every thirty
seconds."*

| Wedge clause (weight) | Leader's measured facts | Verdict |
|---|---|---|
| **Reads the makam intervals (komma/perde) in cents** (`quarter-tone-accuracy`, core) | Both leaders are chromatic/generic; neither title or review names makam, komma or perde. A covered capability gap, but **no leader user is recorded asking for it** in the 9 reviews read. | **Silent — difference, not a complaint-driven wedge** |
| **Holds a stable lock on the low courses** (`quarter-tone-accuracy` / `reads-low-courses`, core+supporting) | Soundcorset's own 3★ complaint is harmonic mis-detection: *"picking up harmonics and tuning off of those instead of the note you're actually playing"* — the leader **is criticised** for the failure class. But the on-job leader CherrySoft's own sample is clean; the detection complaint is on Morfozz (mid-tier). | **Wedge lands on the criterion leader and the mid-tier; NOT on the on-job leader** |
| **No ad can interrupt a tuning session** (`no-ads-during-tuning`, core) | Both leaders declare **ads: true**. Soundcorset is criticised for a rent model and the shelf's #1 complaint is ads (7 of 12 critical = **58%**). | **Leader criticised — wedge survives (strongest clause)** |
| **Every tuning inside one $4.99 unlock, not a weekly charge** (`all-tunings-one-unlock`, core) | Soundcorset's 2★/3★ reviews explicitly reject the rent model and ask for pay-once: *"I'd happily pay a few dollars for an ad-free version, but I'm not going to pay $1/month in perpetuity just for a tuner."* | **Leader criticised — wedge survives (strong)** |
| **Recites the drone + perde fret map** (`drone-and-perde-guide`, supporting) | No enriched incumbent offers it (ON RECORD, proposal §4); the leaders are silent. | **Capability gap — no complaint evidence** |
| **Offline, no account** (`offline-no-account`, supporting) | Soundcorset/others not criticised for it in the sample; one `offline` complaint on Pro Guitar Tuner, not the leader. | **Silent — not evidenced on the leader** |

**A clause the leader already satisfies is not a wedge.** Here the leader does **not** satisfy the
"microtonal accuracy" claim (it is an equal-tempered generic tuner), so the claim is a genuine
*capability difference*; but it is not a *wedge against the leader* until a leader user is recorded
wanting it. As of this read, none is.

### 1.5 What the leaders have that we do not

1. **Distribution and review mass.** Soundcorset: **50M+ installs, 174,180 reviews, 4.6★**; the
   on-job leader CherrySoft: **1M+, 8,023 reviews, 4.4★**. A new listing has zero of both, and the
   `tuner` head term is owned by the 50M+ app (ON RECORD, `naming.json` rationale).
2. **A free funnel that actually converts, plus adjacent content.** Both leaders are free downloads
   that monetise inside the app; CherrySoft ships the **500K+ oud tuner** as a second app feeding the
   same audience, and Soundcorset bundles metronome and practice tones. Our $4.99 is one product.
3. **A 4.2–4.6★ rating floor that survives the complaints.** Users complain about ads and rent and
   still rate these apps 4.2–4.6★; the complaints are tolerated, which is the opposite of a defect
   that drives defection.

### 1.6 Verdict — **WEAK**

The recipe's "weak mid-tier" story is **half true**: the on-job leaders are 4.2–4.4★, genuinely below
the 4.6★+ the generic fortress holds, and the shelf's dominant complaint is ads (58%). But the app a
`Rast` listing must actually beat is a **free 4.4★/1M+ incumbent with 8,023 reviews whose own sampled
reviews are clean and which cross-sells to the oud audience**, while the mechanical criterion leader
is a **4.6★/50M+ generic tuner** that is not the job. The wedge's evidence base (ads, harmonic
mis-detection) comes mostly from the generic fortress and the 100K+ mid-tier, not from the app the
new listing competes with.

**The one thing that would have to be true:** a saz/oud player must defect from a free 1M+ incumbent
to a **$4.99 paid** app for ad-free, microtonal tuning. **UNVERIFIED** — every enriched incumbent on
this shelf prices at 0, so the shelf has no recorded paid winner to prove a paid niche tuner converts;
the deciding reading would be a priced comparable, and none is on this shelf.

---

## 2. `rook-encounter-generator` — "Rook: Encounter Generator", $2.99 + $1.99 packs

### 2.1 The leader, by the criterion

`node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "ttrpg encounter generator"` → 30
cards, verdict `mixed`, relevance guard `partial` (15/30). Top 10 as printed (**MEASURED**):

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|--:|---|---|---:|---|---|---|---|---|
| 1 | `com.crixisvision.meb2024` | Encounter Builder D&D 2024 | unrated | — | 500+ | false | false | free |
| 2 | `gg.encounter.thelostcards` | Encounter: The Lost Cards | 3.8 | 157 | 10K+ | false | true | free |
| 3 | `uk.co.maddwarf.randomencountergenerator` | Random Encounter Generator | unrated | — | 100+ | false | false | free |
| 4 | `uk.co.maddwarf.randomencountergeneratorpro` | Random Encounter Generator PRO | unrated | — | 5+ | false | false | **paid $1.99** |
| 5 | `th.majimesoft.dice_roller` | TTRPG Dice Roller Saving Throw | unrated | — | 5K+ | true | false | free |
| 6 | `com.dmtools` | D&D 5e Encounter Keep | **2.5** | 23 | 10K+ | true | true | free |
| 7 | `com.Tomorrow.DnDEncounterGenerator` | Simple RPG Battle Generator | 4.3 | 13 | 1K+ | false | true | free |
| 8 | **`com.Tomorrow.Roll20TabletopRpgMaps`** | **Virtual Tabletop RPG Manager** | **4.3** | **1,220 (1,220)** | **100K+** | **true** | **true** | **free** |
| 9 | `com.alexanderwinn.DungeonRoller` | Dungeon Roller - RPG Generator | 4.2 | 68 | 5K+ | false | true | free |
| 10 | `com.momokka.dice` | TRPG simple dice | unrated | — | 100+ | true | false | free |

- **Criterion leader (MEASURED):** `com.Tomorrow.Roll20TabletopRpgMaps` — **100K+** (highest bucket)
  with **1,220** exact reviews. **4.3★, ads, IAP, free.** Its job is a **virtual-tabletop manager**,
  not a scene generator.
- The recipe's own "category leader" is **not this app**: the proposal calls
  `com.lionsden.gamemaster5` (Game Master 5th Edition, **500K+, 4.1★, 1,610**) the leader (ON RECORD,
  proposal §5). That reading comes from the **`dnd dm screen app`** query, not the primary
  `ttrpg encounter generator` query — the two queries surface different shelves.
- **The weak generator tier the wedge actually targets is real and downloaded** (MEASURED, my
  `scan-complaints` run below): `social.petrol.playmat` **2.1★/10K+**, `com.dmtools` **2.5★/10K+**,
  `com.rpgautomails.fatedice` **2.6★/10K+**, `air.com.trapStreetStudios.theCalamus` **2.9★/100K+**,
  `com.axehigh.rpg.encounters` **3.3★/10K+**. These are not apps nobody downloads.

### 2.2 Leader profile

| | Criterion leader `com.Tomorrow.Roll20TabletopRpgMaps` |
|---|---|
| Rating | 4.3★ |
| Exact reviews | 1,220 |
| Installs | 100K+ (≥ 100,000) |
| Ads / IAP / price | true / true / free |
| Job | virtual tabletop / battlemap manager, adjacent to scene generation |

(MEASURED, enrich command above). **ON RECORD** context from the evidence file: the category's giant
is `com.fandom.playercompanion` (D&D Beyond), **4.6★ over 130,628 reviews at 5M+**, free with IAP;
the adjacent DM tools `com.lionsden.gamemaster5` (500K+, 4.1★, 1,610) and
`com.michaelwu.gamemasterstoolkit5e` (100K+, 4.5★, 496) rate well.

### 2.3 The leader's own complaints, quoted

`node scripts/scan-complaints.mjs --package com.Tomorrow.Roll20TabletopRpgMaps --package
com.lionsden.gamemaster5 --package com.michaelwu.gamemasterstoolkit5e` — 9 reviews, 3 critical, all
three on `com.lionsden.gamemaster5` (**MEASURED**):

- **`com.Tomorrow.Roll20TabletopRpgMaps` (4.3★, 100K+): 0 critical in the sample.** Its three shown
  reviews are feature requests and one ad complaint: 4★ *"the adds are really annoying, they are just
  in the way so you have to spend the 4-5 dollars on the other stuff just to remove them"*; 4★ *"It
  needs the ability to name the heroes... the ability to zoom in/out... a fog of war option"*.
- **`com.lionsden.gamemaster5` (4.1★, 500K+): 1★** *"I paid for the upgrade. I used this app
  successfully for five sessions. Then it went nuts... the app had completely wiped itself... the
  backup data was also gone causing me to rebuild my entire campaign."* — `broken`/`data-loss`.
  2★ *"when it doesn't it goes really bad... my entire compendium of monsters and spells were gone."*

Shelf-wide, `node scripts/scan-complaints.mjs --query "ttrpg encounter generator" --top 6
--concurrency 2` — 18 reviews, 12 critical: **crash 3 (25%)**, ads 2 (17%), broken 2 (17%), paywall 2
(17%), missing-feature 1, performance 1 (**MEASURED**). The verbatim complaints are on the **weak
generator tier**, and they name exactly what the wedge promises:

- `com.axehigh.rpg.encounters` (3.3★, 10K+) — 1★ *"when I refresh it crashes and I get nothing new"*;
  3★ *"all the tables and entries are there and they are awesome. please add a way to get an random
  result from"* (the tables exist; no generator sits on top).
- `com.dmtools` (2.5★, 10K+) — 1★ *"app kept crashing during game play, would remove player
  characters from the initiative order randomly"*.
- `social.petrol.playmat` (2.1★, 10K+) — 2★ *"Takes ages for you to upload items/tokens... Only for
  the app to delete them after a few minutes."*
- `com.rpgautomails.fatedice` (2.6★, 10K+) — 2★ *"Full screen ads every few rolls."*

### 2.4 Head-to-head — each promised wedge clause against the leaders

Wedge, verbatim from the proposal §4: *"One press returns a whole runnable scene rather than a single
table result... composed from a large authored library; every part names the table it came from and
re-rolls in place without crashing or losing what is already there; the scene lives on the device, so
nothing is uploaded and no server can delete it; no advertisement covers a roll; the library is bought
as permanent themed packs, never rented."*

| Wedge clause (weight) | Criterion leader's measured facts | Verdict |
|---|---|---|
| **A whole runnable scene, not one table result** (`whole-scene-one-press`, core) | The leader is a VTT **manager**; it does not advertise scene generation, and its reviews do not ask for it. The weak generator tier **is** criticised: *"please add a way to get an random result from"* the tables that already exist. | **Leader silent (adjacent job); wedge lands on the weak tier** |
| **Large authored library** (`authored-library-large`, core) | Leader silent. Thin-content complaints are on other apps (`DnD Character Sheet` *"only a small handful of races, classes and backgrounds"*, ON RECORD evidence §5). | **Leader silent; wedge lands on the tier** |
| **Re-rolls in place without crashing or losing state** (`reliable-reroll-no-loss`, supporting) | Leader's own sample is clean (0 critical). The tier is criticised — `crash` is **3 of 12 critical (25%)** and `broken` 2 more; *"when I refresh it crashes"*, *"kept crashing during game play"*. | **Leader silent; wedge lands on the tier (strong there)** |
| **On-device, nothing uploaded** (`on-device-nothing-uploaded`, supporting) | Leader silent; the tier draws data-loss complaints (Playmat upload/delete, `ddfive` *"my characters were gone"*). | **Leader silent; wedge lands on the tier** |
| **No ad over a roll; permanent packs, never rented** (`permanent-pack-purchase`, supporting) | The leader declares **ads: true** and its own reviewer names them (*"the adds are really annoying"*). `paywall` is 2 of 12 critical and `ads` 2 of 12 on the shelf. | **Leader criticised — wedge lands on the leader** |

**A clause the leader already satisfies is not a wedge.** The leader is a different job (a VTT manager
with maps), so it is **not** a defender of the generator job; the head-to-head is *unopposed but
indirect*. The contest that matters is with the weak generator tier, which the criterion leader does
not represent.

### 2.5 What the leaders have that we do not

1. **A dominant platform and a paying category already in place.** D&D Beyond at **5M+/130,628
   reviews** (ON RECORD) and `com.lionsden.gamemaster5` at **500K+/1,610** own the DM audience; the
   shelf's money is **IAP** (8 of 8 enriched `worldbuilding generator` incumbents declare IAP, **0
   declare ads**, ON RECORD evidence §4). Any new listing competes with that gravity.
2. **A proven adjacent generator at scale.** `FaNG – Fantasy Name Generator` is **4.8★ over 7,304
   reviews at 500K+** with ads + IAP (ON RECORD evidence §4) — a generator of *names* already proves
   the shape converts, and it is a fraction of a scene.
3. **Breadth of content the leader ships.** Roll20 VTT Manager bundles maps/virtual-tabletop
   features; the adjacent tools bundle compendiums. A $2.99 scene library is one slice.

### 2.6 Verdict — **PLAUSIBLE**

This is the recipe whose "weak mid-tier" claim survives best. The generator tier is measurably weak
**at installs people download** — `playmat` 2.1★, `D&D 5e Encounter Keep` 2.5★, `Fate Dice` 2.6★,
`Calamus` 2.9★, `RPG Encounter` 3.3★, all at **10K–100K+** — and its own users complain about exactly
what the wedge promises (crashes on re-roll and refresh, thin content, no generator on top of the
tables that exist, ads over the roll). The shelf monetizes by purchase, not ads.

**The one thing that would have to be true:** a game master must pay a **$2.99 unlock at "Keep"** —
that pinning a generated scene is a real purchase moment. **INFERRED** (the shelf's money is IAP, but
nothing measured shows any incumbent selling *genre packs*; the recipe itself labels the repeat unit
INFERRED).

---

## 3. `part-107-drills` — "Part 107 Drills", $9.99 one-time FAA exam drills

### 3.1 The leader, by the criterion

`node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "part 107 practice test"` → **11
visible cards**, verdict **`candidate`**. Top 10 as printed (**MEASURED**):

| # | package | title | rating | reviews (exact) | installs | ads | IAP | price |
|--:|---|---|---:|---|---|---|---|---|
| 1 | `com.edupass.part107` | Part 107 Practice Test 2026 | **4.9** | 107 (107) | 1K+ | false | false | free |
| 2 | `com.droneprep.app` | FAA Part 107 Pro Test Prep | 4.8 | 18 (18) | 100+ | false | true | free |
| 3 | `com.alphabeat.ffaexams` | FAA Part 107 Drone Test Prep | unrated | — | 100+ | false | true | free |
| 4 | **`com.part107.faa.exam`** | **Part 107 - Practice for exam** | **4.0** | **440 (440)** | **10K+** | **false*** | **true** | **free** |
| 5 | `night_coding.android.part107` | Part 107 FAA — practice test | **3.5** | 34 (34) | 1K+ | false | false | free |
| 6 | `com.rltw.part107` | Part 107 Ready: FAA Drone Prep | unrated | — | 10+ | false | true | free |
| 7 | `com.aleosapps.QuizzAppFAA107` | FAA PART 107 Exam Prep Prepia | unrated | — | 1K+ | false | true | free |
| 8 | `com.calcwise.faapart107` | FAA Part 107 Drone Exam 2026 | unrated | — | 5+ | true | true | free |
| 9 | `xyz.plus44.part107ready` | Part 107 Ready | unrated | — | 10+ | false | true | free |
| 10 | `com.logicpathsystems.faa107` | Drone Exam: FAA Part 107 Prep | unrated | — | 10+ | true | false | free |

- **Criterion leader (MEASURED):** `com.part107.faa.exam` — **10K+**, the highest install bucket (and
  the only app above 1K+), with **440** exact reviews. **4.0★, IAP, free.**
- This matches the recipe's own leader reading (proposal §3.3). The **ratings** leader is different:
  `com.edupass.part107` at **4.9★ over 107 reviews, 1K+, free with no IAP** — high rating, tiny base.
- **\*Contradiction, reported not smoothed** (MEASURED twice): the enricher prints this app as
  `ads: false`, while its own critical reviews describe constant ads. Both readings are measured; the
  interpretation that the listing under-declares is INFERRED.

### 3.2 Leader profile

| | Criterion leader `com.part107.faa.exam` |
|---|---|
| Rating | 4.0★ |
| Exact reviews | 440 |
| Installs | 10K+ (≥ 10,000) |
| Ads / IAP / price | declared false (contradicted by reviews) / true / free |
| Job | Part 107 exam practice, monetised by a week/month subscription |

(MEASURED, enrich command above.) **ON RECORD** context: the 2026 entrants advertise fixed banks
(`com.sima.part` "more than 350 questions", `com.dauntless.gs.faa.rdp` "500+",
`com.droneprep.app` "1000+ practice questions", `com.appark.part107.part107` "over 1,000 fresh
practice items") with "detailed explanations", ACS tagging and offline mode (proposal §3.5).

### 3.3 The leader's own complaints, quoted

`node scripts/scan-complaints.mjs --package com.part107.faa.exam --package com.edupass.part107
--package night_coding.android.part107` — 9 reviews, 6 critical (**MEASURED**):

- **`com.part107.faa.exam` (4.0★, 10K+, 3 critical — the leader itself):**
  - 2★ (December 13, 2025): *"The amount of ads in this app are rough and disruptive BUT if you pay
    for the week/month subscription they (mostly disappear) and it's a very good resource... even
    with the subscription you'll get an ad every time you leave the app."*
  - 2★ (December 17, 2025): *"This app could be so amazing if there weren't literally constant
    pornographic ads... Add to that it's a subscription to remove them? It's for a test, most people
    won't need more than a month, you're just hoping people forget and you get to charge them a few
    more times before they figure out how to cancel. Scummy business practices."*
  - 3★ (June 17, 2026): *"Its decent for a general study. However there are topics that are
    specifically fixed wing topics only. I understand there is fixed wing in part 107 courses.
    However major topics like operation and radio... Part 107 are not allowed to communicate on radio
    unless an emergency. However there are question that the answer is calling 10 miles out for
    approach."*
- **`com.edupass.part107` (4.9★, 1K+): 0 critical.**
- **`night_coding.android.part107` (3.5★, 1K+):** 1★ *"too many questions that are WRONG! You can't
  pass a test if the study guide is giving you inaccur[ate]"*; 2★ *"fix the swipe right bug that
  exits whatever question set you are in"*; 3★ *"...breaks forcomercial at 13 questions is to often."*

Shelf-wide, `node scripts/scan-complaints.mjs --query "part 107 practice test" --top 6 --concurrency
2` — 11 reviews, 6 critical: **ads 2 (33%)**, **paywall 2 (33%)**, accuracy 1 (17%), broken 1 (17%),
missing-feature 1 (17%) (**MEASURED**). The leader's own critical reviews **are** the shelf's
complaint profile.

### 3.4 Head-to-head — each promised wedge clause against the leader

Wedge, verbatim from the proposal §4: *"Where the shelf tests you against a fixed bank you can
memorise... this drills the two things the exam actually makes you read and apply: every weather item
is generated from the METAR/TAF grammar and every airspace item is generated from the 14 CFR §107
limits, each validated against those rules before it is shown, and every answer names the paragraph or
ACS code it came from on the answer screen — sUAS-only, with no ads, no subscription and no account."*

| Wedge clause (weight) | Leader's measured facts | Verdict |
|---|---|---|
| **Weather/airspace drills generated from grammar + §107 limits, validated** (`generated-drills-validated`, core) | The leader runs a **fixed** bank (the whole shelf does; 2026 entrants advertise 350–1,000+ item banks, ON RECORD proposal §3.5). No leader user complains about memorisation or staleness. | **Leader silent — a content difference, NOT a complaint-driven wedge** |
| **Every answer cites the paragraph/ACS code on the answer screen** (`every-answer-cited`, core) | The leader is **not** criticised for accuracy. The accuracy complaint is on the weak app `night_coding` (3.5★/34), not on the leader. | **Wedge lands on the weak app, not the leader** |
| **sUAS-only — no fixed-wing/manned material** (`suas-only-content`, core) | The leader itself is criticised for exactly this: *"there are topics that are specifically fixed wing topics only"* and a question whose answer assumes a 10-mile approach call. | **Leader criticised — wedge survives** |
| **No ads, no renewal: one purchase** (`no-ads-no-renewal`, supporting) | The leader's own critical reviews are **2 ads + 2 paywall of 3** — *"literally constant pornographic ads"*, *"week/month subscription"*, *"Scummy business practices"*. | **Leader criticised — wedge survives (strongest clause)** |
| **Resumable session** (supporting) | Leader silent; the swipe-right state-loss bug is on the weak app `night_coding`. | **Wedge lands on the weak app** |
| **Offline by construction** (supporting) | Leader silent; the 2026 entrants already claim offline (ON RECORD §3.5). | **Leader silent; already contested by entrants** |

**A clause the leader already satisfies is not a wedge.** The leader is silent on generated drills —
it does **not** satisfy the claim — but it is *not criticised* for the absence, so generated-and-
validated drills is a **difference, not a wedge**. The recipe itself concedes this in §12.4: *"The
shelf's complaints are about ads, price and accuracy — not about memorisation."* The clauses that do
land are the **price model** and **sUAS scoping**, both of which are also claimed by the recipe.

### 3.5 What the leader has that we do not

1. **The install base and the query ranking.** **10K+ installs and 440 reviews** against a new
   listing's zero; the shelf's largest bucket belongs to this app, and the recipe's own kill criterion
   is 2,000 installs in 90 days (proposal §6) against a shelf whose largest app is 10K+.
2. **A subscription revenue engine plus a ratings leader next to it.** The leader converts ads into a
   week/month subscription; `com.edupass.part107` sits at **4.9★** (free, no IAP) as the trust signal
   a buyer sees first.
3. **The features the wedge's *supporting* half brings already advertised by 2026 entrants.**
   Explanations, ACS tagging, offline mode and 1,000+ items are already claimed (proposal §3.5), so
   the "explanations + offline + no ads" position is no longer white space.

### 3.6 Verdict — **PLAUSIBLE**

The leader is the **weakest incumbent of the three recipes** — 4.0★ over 440 reviews at 10K+ — and its
own sampled critical reviews name exactly the two defects the wedge's price clauses replace: ads and a
week/month subscription, called *"scummy business practices"*. A one-price, ad-free app has a
named, on-the-leader reason to be preferred, and the shelf's only money is that rent. The caveat is
load-bearing: the recipe's **distinctive** clause (generated-and-validated drills with on-answer
citations) answers a complaint **nobody on this shelf made**, so the win rests on the price clause and
on sUAS scoping, not on the mechanism the recipe is named for.

**The one thing that would have to be true:** a US learner must pay **$9.99 outright** rather than
rent the subscription the leader sells. **UNVERIFIED** — the only measured paying behaviour on the
shelf is the leader's week/month subscription, the two best-rated apps are free with no IAP, and no
priced comparable was found (proposal §12.1).

---

## 4. Cross-recipe ranking — strength of the leader head-to-head

Ordered by how well the promised wedge survives contact with the leader of the primary query.

| Rank | Recipe | Verdict | One-line reason |
|---:|---|---|---|
| 1 | `part-107-drills` | **PLAUSIBLE** | The leader is the weakest incumbent of the three (4.0★/440/10K+) and its **own** critical reviews name the ads and the subscription the wedge replaces — but the distinct core clause (generated drills) answers no measured complaint. |
| 2 | `rook-encounter-generator` | **PLAUSIBLE** | The weak generator tier is real and downloaded (2.1–3.3★ at 10K–100K+) and its users complain about the wedge's clauses verbatim, but the criterion leader is an **adjacent** job (a 100K+/4.3★ VTT manager) that the wedge neither beats nor needs to. |
| 3 | `rast-saz-oud-tuner` | **WEAK** | The on-job leader is a **free 4.4★/1M+ incumbent with 8,023 reviews and a clean sampled page**, and the criterion leader is a 4.6★/50M+ generic fortress; the wedge's complaint base is the generic/mid-tier, not the app a `Rast` listing must beat. |

**Which wedge clause failed the leader test most clearly:**
`part-107-drills` — **`generated-drills-validated`**. The leader (and the whole shelf) runs a fixed
bank, is **not criticised** for it, and the recipe's own honest-unknowns section admits the shelf
complains about ads, price and accuracy and **never** about memorisation; a clause no leader user asks
for is a difference, not a wedge. Runner-up: `rast-saz-oud-tuner` — **`quarter-tone-accuracy`**, whose
own-reviews evidence comes from the generic fortress (Soundcorset's harmonic mis-detection), the
mid-tier (Morfozz), and the same developer's oud app — **not** from the on-job leader's own page,
which is clean.

---

## 5. Claim ledger

`CLAIM / LABEL / EVIDENCE`.

| # | Claim | Label | Evidence |
|--:|---|---|---|
| 1 | `saz tuner` returns 30 cards, verdict `mixed`, guard `on-topic` (30/30) | MEASURED | `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "saz tuner"` |
| 2 | Criterion leader for `saz tuner` is `com.soundcorset.client.android`: 4.6★, 174,000 (174,180) reviews, 50M+, ads true, IAP true, free | MEASURED | same command, row 8 |
| 3 | On-job leader is `com.visne.baglamatuner` (CherrySoft): 4.4★, 8,020 (8,023), 1M+, ads true, IAP true, free | MEASURED | same command, row 1 |
| 4 | `com.baglamatuner` (Morfozz): 4.2★, 2,560 (2,555), 1M+, ads true | MEASURED | same command, row 3 |
| 5 | Default `saz tuner` page is only ~9/30 on the saz job by my regex (guard said on-topic) | MEASURED | `node scripts/scan-titles.mjs --query "saz tuner" --on-job "(saz\|baglama\|...)" --off-job "(guitar\|...)"` → `hand job-share: 9/30 (0.30) vs guard on-topic` |
| 6 | The recipe records 20/30 (0.67) hand job-share for the same query | ON RECORD | `docs/research/2026-10-02-niche-real-time-tools-evidence.md` §3 |
| 7 | Soundcorset's own 3★ review complains about harmonic mis-detection and refuses a subscription, offering to pay once for ad-free | MEASURED | `node scripts/scan-complaints.mjs --package com.soundcorset.client.android ...` |
| 8 | Soundcorset's own 2★ review complains about a ~$6/month fee for free-tier voices | MEASURED | same command |
| 9 | CherrySoft's baglama page shows 3 reviews, 0 critical; Morfozz's shows 2 critical ("Not detecting any sounds", "freez and dont hear my saz") | MEASURED | same command |
| 10 | `saz tuner` shelf: 18 reviews, 12 critical, ads 7 (58%) | MEASURED | `node scripts/scan-complaints.mjs --query "saz tuner" --top 6 --concurrency 2` |
| 11 | `ttrpg encounter generator` returns 30 cards, verdict `mixed`, guard `partial` (15/30) | MEASURED | `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "ttrpg encounter generator"` |
| 12 | Criterion leader is `com.Tomorrow.Roll20TabletopRpgMaps`: 4.3★, 1,220 (1,220), 100K+, ads true, IAP true, free | MEASURED | same command, row 8 |
| 13 | `com.dmtools` is 2.5★/23/10K+; `Random Encounter Generator PRO` is paid $1.99 at 5+ | MEASURED | same command, rows 6 and 4 |
| 14 | The recipe calls `com.lionsden.gamemaster5` (500K+, 4.1★, 1,610) the category leader, from the `dnd dm screen app` query | ON RECORD | `docs/ideation/2026-10-02-generator-and-creative-tools.md` §5 |
| 15 | `ttrpg encounter generator` shelf: 18 reviews, 12 critical, crash 3 (25%), ads 2, broken 2, paywall 2 | MEASURED | `node scripts/scan-complaints.mjs --query "ttrpg encounter generator" --top 6 --concurrency 2` |
| 16 | The weak generator tier is 2.1★/10K+ (playmat), 2.5★/10K+ (dmtools), 2.6★/10K+ (fatedice), 2.9★/100K+ (Calamus), 3.3★/10K+ (RPG Encounter) | MEASURED | same command's per-app rows |
| 17 | Verbatim: *"all the tables and entries are there and they are awesome. please add a way to get an random result from"* | MEASURED | same command, `com.axehigh.rpg.encounters` |
| 18 | Roll20 VTT Manager's own sampled reviews are 0 critical; one names the ads | MEASURED | `node scripts/scan-complaints.mjs --package com.Tomorrow.Roll20TabletopRpgMaps ...` |
| 19 | `com.lionsden.gamemaster5` (500K+) reviews describe a total wipe and lost compendium | MEASURED | same command |
| 20 | D&D Beyond is 4.6★/130,628/5M+; `worldbuilding generator` is 8/8 IAP and 0/0 ads; FaNG is 4.8★/7,304/500K+ | ON RECORD | `docs/research/2026-10-02-generator-and-creative-evidence.md` §4 and §6 |
| 21 | `part 107 practice test` returns 11 cards, verdict `candidate` | MEASURED | `node scripts/enrich-market.mjs --top 10 --concurrency 3 --query "part 107 practice test"` |
| 22 | Criterion leader is `com.part107.faa.exam`: 4.0★, 440 (440), 10K+, declared ads false, IAP true, free | MEASURED | same command, row 4 |
| 23 | `com.edupass.part107` is 4.9★/107/1K+ free with no IAP; `night_coding` is 3.5★/34/1K+ | MEASURED | same command, rows 1 and 5 |
| 24 | The leader's own critical reviews are 2 ads + 2 paywall + 1 fixed-wing complaint | MEASURED | `node scripts/scan-complaints.mjs --package com.part107.faa.exam ...` |
| 25 | Verbatim: *"literally constant pornographic ads"* and *"Scummy business practices"* | MEASURED | same command, 2★ December 17, 2025 |
| 26 | Verbatim: *"there are topics that are specifically fixed wing topics only"* | MEASURED | same command, 3★ June 17, 2026 |
| 27 | `part 107 practice test` shelf: 11 reviews, 6 critical, ads 2 (33%), paywall 2 (33%) | MEASURED | `node scripts/scan-complaints.mjs --query "part 107 practice test" --top 6 --concurrency 2` |
| 28 | `com.part107.faa.exam` declares `ads: false` while its users describe ads | MEASURED (contradiction) | rows 22 vs 24 |
| 29 | The 2026 entrants advertise 350/500/1,000/1,000+ item banks with explanations, ACS tagging and offline mode | ON RECORD | `docs/ideation/2026-10-02-authored-learning-and-drills.md` §3.5 |
| 30 | The recipe admits the shelf complains about ads, price and accuracy, not memorisation | ON RECORD | `docs/ideation/2026-10-02-authored-learning-and-drills.md` §12.4 |
| 31 | The three recipes' primary queries are `searchPhrases[0]` of their `naming.json` | MEASURED | read `plans/rast-saz-oud-tuner.naming.json`, `plans/rook-encounter-generator.naming.json`, `plans/part-107-drills.naming.json` |
| 32 | Each recipe's promised wedge clauses and their weights | MEASURED | read `plans/*.differentiators.json` and the three proposals |
| 33 | Which leader drove each verdict (two-leader split where the criterion leader is off-job) | INFERRED | criterion leader from rows 2/12/22; on-job leader from rows 3/13/23; the split is this report's judgement, stated in §1.1, §2.1 and the verdicts |
| 34 | A saz/oud player will defect from a free 1M+ incumbent to a $4.99 paid app | UNVERIFIED | needs a priced comparable on the shelf; every enriched incumbent prices at 0 |
| 35 | A GM will pay a $2.99 unlock at "Keep" | UNVERIFIED | no incumbent is measured selling genre packs; deciding reading is a Play Console result via `node scripts/check-outcomes.mjs` |
| 36 | A US learner will pay $9.99 outright rather than rent | UNVERIFIED | no priced comparable on the shelf; proposal §12.1 records the same gap |
| 37 | Our wedge clauses that the leader is silent on are genuinely unwanted | UNVERIFIED | the only way to decide is a live test; no incumbent's users were recorded asking for microtonal makam intervals, drone/perde guides, generated drills or on-device scene generation |
| 38 | The exact storefront copy behind every note above is the one Play served this session | UNVERIFIED | one signed-out `hl=en`, `gl=US` session; not a Play Console reading and not reproducible byte-for-byte |

**Honest limits of this report.** Install buckets are rounded floors (`1M+` spans 1,000,000–9,999,999),
so every size comparison is a floor comparison. `scan-complaints` reads Play's own server-rendered
sample (measured at 3 reviews per page) on the *weakest-rated* incumbents for a query and on the named
package for `--package`, so the complaint tables describe the **vocabulary** of a shelf, never
complaint frequency in its population. A leader with 0 critical reviews in a 3-review sample is not a
leader without complaints — it is a leader whose complaints were not served. The leader was selected
by one criterion applied to one query in one storefront session; a larger incumbent can sit below the
enrichment cut, and the relevance guard was demonstrably over-confident on `saz tuner` (on-topic over
a 0.30 job share by my regex, and the recipe's own 0.67).

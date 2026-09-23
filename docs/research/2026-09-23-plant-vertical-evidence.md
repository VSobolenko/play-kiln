# Plant vertical (edible-crop health) evidence — 2026-09-23

> **Question this run tests.** The generic plant idea is owned: `docs/market-enrichment-2026-09-23.md`
> measured `plant disease identifier` and found PictureThis (`cn.danatech.xingseus`) at 50M+ with
> 800,000 ratings and 8/8 enriched apps declaring in-app purchases. The proposed escape is a
> **vertical** — edible garden crops (tomato, cucumber, pepper) rather than general plant ID — on the
> hypothesis that "my tomato has spots, tell me what to do" is a different, less-owned job.
> **That hypothesis was tested here with store data, before any code exists.**

## How this was produced

```bash
node scripts/scan-plant-vertical.mjs --json --concurrency 3 --top 8 \
  --out .kiln/plant-vertical/scan-2026-09-23.md
```

- **Exit code: 0.** Every one of the 14 queries returned visible cards and all 112 selected app
  detail pages were fetched and parsed. `Failures: None`.
- Run at `2026-09-23T20:12:02.338Z`, 37,406 ms wall clock, top 8 cards per query, fetch concurrency 3.
- Signed-out Play search (`?c=apps`) plus per-app detail pages, `hl=en`, `gl=US`, one session.
- Search cards: `scripts/lib/play-scan.mjs`. Per-app facts: `scripts/lib/play-app-detail.mjs`. Both
  unmodified; the CLI is `scripts/scan-plant-vertical.mjs`.
- Raw output for this run: `.kiln/plant-vertical/scan-2026-09-23.md` (report) and
  `.kiln/plant-vertical/scan-2026-09-23.json` (machine-readable). **Every number below is from that
  run.** No figure was hand-entered, estimated, or carried over from the generic-niche baseline.

**Reading the columns.** `installs` is Play's rounded bucket (`5M+`), so it is a **floor, not a
count**. `ads` / `iap` are three-valued: `true` = Play showed the badge, `false` = the badge region
was present and did **not** show that badge, `unknown` = the badge region itself was absent so the
fact was not observable. In this run **0 of 112 pages** had a missing badge region, so every
`unknown` count below is a genuine `0` — an observed fact, not a default.

**A noise warning that changes how the numbers must be read.** Play's search is a fuzzy text match,
and five of the fourteen queries were visibly polluted by it:

| query | what Play actually returned | usable? |
| --- | --- | --- |
| `late blight` | `Lands of Blight`, `BE LIGHT`, `Belight Community`, `L&S LITE BT`, `B·T LIGHTING`, `BLE Light`, `Dustlight`, `BT Light` — matched "light"/"blight" | **no** — 0 of 8 enriched cards are crop-related |
| `whitefly treatment` | `WhiteFlag: Mental Health Help`, `Whitecafè`, `White Feathers`, `Whitecroft Sense`, `WHITE ICY`, `White Collar Professional` — matched "white" | **no** — 0 of 8 enriched cards are crop-related |
| `tomato leaf curl` | `Tomato Wallpaper`, `Tomato simulator`, `Tomato Focus: Pomodoro Timer`, `Tomato Lite - Secure VPN Proxy`, `Tomato - Official Game`, `Tomato Manga` | **mostly no** — 2 of 8 are tomato-plant apps (`Tomato Clinic`, `Tomato Cultivation`); the rest matched "tomato" only |
| `tomato blight` | `Blinkit: Groceries & more` (100M+), `Harry Potter: Hogwarts Mystery` (50M+), `Fandango` (10M+), `Snappy Tomato Pizza`, `Tomato Focus: Pomodoro Timer` | **mostly no** — 3 of 8 are plant/tomato apps (`Tomato Cultivation`, `iPlant`, `Tomato Clinic`); the largest buckets are unrelated consumer apps |
| `aphid treatment plants` | `Aphid: Clone Yourself` (an app named Aphid), `Meds & Pill Reminder MyTherapy` | **partly** — 6 of 8 cards are plant/garden apps |

This is a limitation of the *query text*, not of the library: the ratings screen and the detail
fetcher both behaved correctly and reported exactly what Play served. It means **the `seen`,
`rated` and verdict columns for those five queries describe Play's fuzzy match, not the crop-health
market**, and no conclusion below rests on them. The nine remaining queries are on-topic.

## The query set (14)

The eight commissioned queries plus six more of the same kind (a crop symptom, a treatment, or a
pest), so the vertical is not judged on one phrasing:

`tomato disease`, `tomato blight`, `cucumber plant disease`, `pepper plant disease`,
`vegetable garden disease`, `powdery mildew treatment`, `plant fungus treatment`,
`garden pest identifier`, `tomato leaf curl`, `crop disease identification`, `late blight`,
`aphid treatment plants`, `whitefly treatment`, `plant fungicide guide`.

## 1. Per-query verdicts, numbers, and the biggest app at each

Verdict, `seen`, `rated/total`, `weak` (< 4.2★), and median are the ratings-only screen's own
output for this run. "Largest bucket" is the biggest install bucket among the 8 enriched apps, with
the app(s) sitting there.

| query | verdict | seen | rated/total | weak | median | largest bucket (app at that bucket) |
| --- | --- | --- | --- | --- | --- | --- |
| `tomato disease` | candidate | 10 | 5/10 | 3 | 3.90 | **500K+** — Agrio - Plant diagnosis app |
| `tomato blight` ⚠ | mixed | 30 | 18/30 | 6 | 4.45 | 100M+ — Blinkit (off-topic) |
| `cucumber plant disease` | candidate | 13 | 8/13 | 4 | 4.05 | **10M+** — Plantix - your crop doctor |
| `pepper plant disease` | candidate | 11 | 6/11 | 5 | 3.40 | **500K+** — Agrio - Plant diagnosis app |
| `vegetable garden disease` | mixed | 30 | 18/30 | 8 | 4.20 | **500K+** — GrowIt / Agrio / Planter (tie) |
| `powdery mildew treatment` | mixed | 30 | 22/30 | 5 | 4.40 | 1M+ — EWG's Healthy Living (off-topic) |
| `plant fungus treatment` | candidate | 30 | 21/30 | 12 | 4.10 | **10M+** — Plant Parent: Plant Care Guide |
| `garden pest identifier` | candidate | 30 | 21/30 | 13 | 3.90 | **50M+** — PictureThis - Plant Identifier |
| `tomato leaf curl` ⚠ | mixed | 30 | 19/30 | 4 | 4.40 | 100K+ — Tomato Clinic |
| `crop disease identification` | candidate | 8 | 5/8 | 3 | 3.90 | **10M+** — Plantix - your crop doctor |
| `late blight` ⚠ | candidate | 30 | 14/30 | 7 | 4.15 | 50K+ — BT Light (off-topic) |
| `aphid treatment plants` ⚠ | mixed | 30 | 19/30 | 4 | 4.50 | **50M+** — PictureThis - Plant Identifier |
| `whitefly treatment` ⚠ | dead-zone | 50 | 16/50 | 9 | 4.10 | 50K+ — WhiteFlag (off-topic) |
| `plant fungicide guide` | candidate | 30 | 24/30 | 13 | 4.10 | **50M+** — PictureThis - Plant Identifier |

Verdict counts for this run: **8 candidate, 5 mixed, 1 dead-zone, 0 fortress, 0 no-results.**
The single `dead-zone` verdict (`whitefly treatment`) is an artefact of Play matching "white" —
its 16/50 rated share is a fact about a fuzzy match, not about whitefly.

### The crop-specific symptom queries, in detail

These are the four queries closest to the proposed job, and their top-8 sets are the core evidence:

**`tomato disease`** (10 visible cards, 8 enriched) — largest bucket **500K+**. Enriched, in Play
order: `com.farmingai.tomato.ImageClassification` "Tomato Disease Diagnosis" (**unrated, 1+, paid
$7.49**), `com.agrio` "Agrio - Plant diagnosis app" (4.2★, 4,800 reviews, 500K+), `com.artmvstd.plantIdentifier`
"Plant Disease Diagnosis App" (3.9★, 319, 10K+), `com.plant.disease.identifier` "Plant disease
identifier" (3.0★, 16, 5K+), `com.jg.plantdisease` "Plant disease" (3.6★, 142, 100K+),
`com.npckc.tomato` "Tomato Clinic" (4.8★, 4,630, 100K+), `com.xorvanta.kopal` "Kopal: Plant Disease
Scanner" (unrated, 10+), `com.ai.identifier.plant` "Plant Disease identifier" (unrated, 1K+).
Six of the eight carry generic *plant*-disease titles; the two tomato-named ones are
`Tomato Disease Diagnosis` (**unrated, 1+ installs, paid $7.49**) and `Tomato Clinic` (100K+).
PictureThis did **not** appear in this query.

**`pepper plant disease`** (11 visible, 8 enriched) — largest bucket **500K+** (Agrio). The other
seven are all generic "plant disease" apps between 10+ and 100K+, and **seven of the eight** are
unrated or below 4.0★. No pepper-named app at all.

**`cucumber plant disease`** (13 visible, 8 enriched) — largest bucket **10M+**, and it is
`com.peat.GartenBank` "Plantix - your crop doctor" (4.2★, 132,000 reviews), a *dedicated crop*
diagnosis app. Agrio (500K+) is second. The remaining six are generic plant-disease apps.

**`vegetable garden disease`** (30 visible, 8 enriched) — largest bucket **500K+**, a three-way tie
between `com.growit.ai` "GrowIt: Vegetable Garden Care" (Glority Global Group — PictureThis's
owner), `com.agrio`, and `com.perculacreative.peter.gardenplanner` "Planter - Garden Planner". Three
of the eight are garden *planners*, not disease tools.

## 2. Is the vertical owned or fragmented?

**Fragmented at the crop-symptom level — but not vacant, and not purely small.** Both halves of that
sentence are load-bearing.

**The fragmentation is real.** Across the four crop-specific queries (`tomato disease`,
`pepper plant disease`, `cucumber plant disease`, `vegetable garden disease`) only four apps reach
500K+ at all: Agrio (500K+, in **all four**), Plantix (10M+, cucumber only), GrowIt (500K+,
vegetable garden) and Planter (500K+, vegetable garden). Everything
else sits between **10+ and 100K+**, and 40 of the 112 enriched apps overall are **unrated** (72
rated). The unrated ones are not decoration — in `tomato disease` three of eight are unrated, and in
`pepper plant disease` four of eight are.

**But it is not a field of only small apps.** Counting *distinct* apps rather than enriched rows,
12 distinct apps in this run sit at 1M+ or above:

| package id | installs | title | developer | rating | queries answered |
| --- | --- | --- | --- | --- | --- |
| `cn.danatech.xingseus` | **50M+** | PictureThis - Plant Identifier | Glority Global Group Ltd. | 4.5 | 3 |
| `com.peat.GartenBank` | **10M+** | Plantix - your crop doctor | Plantix | 4.2 | 3 |
| `com.plantparentai.app` | **10M+** | Plant Parent: Plant Care Guide | Glority Global Group Ltd. | 4.5 | 3 |
| `com.fws.plantsnap2` | **10M+** | PlantSnap: Plant Identifier | PlantSnap, Inc. | 3.8 | 1 |
| `com.glority.pictureinsect` | **5M+** | Picture Insect: Bug Identifier | Next Vision Limited | 4.3 | 1 |
| `com.stromming.planta` | **1M+** | Planta: Plant & Garden Care | Planta AB | 4.1 | 2 |
| `org.inaturalist.seek` | **1M+** | Seek by iNaturalist | iNaturalist | 3.3 | 1 |
| `com.skindeep.mobile` | **1M+** | EWG's Healthy Living | Environmental Working Group | 4.6 | 1 |
| `com.tinyco.potter` | 50M+ (off-topic) | Harry Potter: Hogwarts Mystery | Jam City, Inc. | 4.6 | 1 |
| `com.grofers.customerapp` | 100M+ (off-topic) | Blinkit: Groceries & more | Blinkit | 4.8 | 1 |
| `air.com.vudu.air.DownloaderTablet` | 10M+ (off-topic) | Fandango: Movies & Series | Fandango Media | 4.4 | 1 |
| `eu.smartpatient.mytherapy` | 5M+ (off-topic) | Meds & Pill Reminder MyTherapy | MyTherapy | 4.5 | 1 |

**Direct comparison with the generic niche.** The generic baseline
(`docs/market-enrichment-2026-09-23.md`) found PictureThis at **50M+ / 800,000 ratings** as the
single owner of `plant disease identifier`, one full bucket above everything else. In this vertical
run, the crop-relevant leaders are **Plantix at 10M+** and **Agrio at 500K+** — a 5× and 100× gap
below PictureThis's bucket — and PictureThis itself still appears, at 50M+, in **3 of 14** queries
(`garden pest identifier`, `aphid treatment plants`, `plant fungicide guide`). So the vertical is
**not** a set of only small unrated apps: it has a dedicated crop-doctor incumbent at 10M+ and a
mid-tier dedicated one at 500K+. It is also **not** un-entered by PictureThis.

**Verdict: fragmented, with a real but lower ceiling.** Unlike the generic niche (one clear owner at
50M+), the crop-symptom queries have **no single owner** and no app above 500K+ for tomato or pepper
specifically. Unlike a true dead zone, the field is populated by apps that already have traction, and
the broader crop-disease job is already served at 10M+.

## 3. Do the vertical's incumbents monetise?

Yes, clearly — though less uniformly than the generic sample.

| signal | declared | declared absent | unknown |
| --- | --- | --- | --- |
| contains ads | **38** | 74 | **0** |
| in-app purchases | **67** | 45 | **0** |

Across **112 enriched apps** (every query combined), split further:

- apps declaring **IAP only**: 42
- apps declaring **ads only**: 13
- apps declaring **both**: 25
- apps declaring **neither** (badge region present): 32
- price: **111 free, 1 paid** — the paid one is `com.farmingai.tomato.ImageClassification`
  "Tomato Disease Diagnosis" at **$7.49**.

**Comparison with the generic niche.** The generic run showed **8/8** apps in `plant disease
identifier` declaring IAP, which the baseline read as "users pay for plant help". This vertical run
shows **67/112 (59.8%)** declaring IAP and **38/112 (33.9%)** declaring ads. So:

- The vertical **does** monetise, and IAP is the majority model, as in the generic niche.
- It is **not as uniform**: 45 apps declare no IAP, against 0 of 8 in the generic sample. (The
  generic figure comes from only 8 apps, so a 45/112 absence rate is not necessarily a contradiction
  — it is a wider sample of a wider query set.)
- The pattern is the same as the generic one in one respect: **ads do not mark the leaders.**
  Plantix (10M+, crop doctor) declares **ads and no IAP**; the generalist PictureThis (50M+) declares
  **IAP and no ads**. Agrio (500K+) declares both.
- Every app that is strong in this vertical is free; the only paid app in 112 is the sub-scale
  dedicated one at **1+** installs.

## 4. Does the vertical look like a worse job served by a generic app?

**Judged from titles only** — the store page exposes a title, a developer and badges; it does not
expose whether the app's knowledge is crop-specific. So this section reads app names, and says so.

**Evidence FOR the hypothesis:**

- **One app answers eight of fourteen crop queries.** `com.agrio` "Agrio - Plant diagnosis app"
  appears in `tomato disease`, `cucumber plant disease`, `pepper plant disease`,
  `vegetable garden disease`, `powdery mildew treatment`, `plant fungus treatment`,
  `crop disease identification` and `aphid treatment plants`. A single general diagnosis app is the
  top answer to crop-specific questions.
- **The generics repeat, the specialists do not.** Distinct packages: **68 across 112 enriched
  rows** — 44 rows are the same apps appearing again. `com.artmvstd.plantIdentifier` "Plant Disease
  Diagnosis App" answers 6 queries, `com.plant.disease.identifier` "Plant disease identifier" 5,
  `com.jg.plantdisease` "Plant disease" 5, `com.xorvanta.kopal` "Kopal: Plant Disease Scanner" 6.
  These are titled for *plants*, not tomatoes, peppers or cucumbers.
- **Dedicated crop apps exist but are sub-scale.** The genuinely crop-named apps in the enriched set
  are `com.farmingai.tomato.ImageClassification` "Tomato Disease Diagnosis" (**unrated, 1+, paid**),
  `tech.khusheel.plantscanner` "PlantDoc — Plant Disease Care" (**unrated, 50+**), and
  `com.mushyp.plantIdentifier` "Scan Plant Disease Identifier" (unrated, 1K+). None has traction.
- **The tomato query in particular is generic.** Six of eight `tomato disease` cards carry
  *plant*-disease titles; of the two tomato-named apps, the dedicated one is unrated at 1+ installs.

**Evidence AGAINST the hypothesis (against writing the vertical off as a genuine opening — or as
"only generics are there"):**

- **A dedicated crop tool already leads a crop query at scale.** `com.peat.GartenBank` **"Plantix -
  your crop doctor"** (10M+, 4.2★, 132,000 reviews) is the largest bucket in
  `cucumber plant disease` and `crop disease identification`. This is a crop-diagnosis app, not a
  general plant-ID app, and it is not small.
- **A second dedicated diagnosis app has mid-scale traction.** Agrio ("Plant diagnosis app", 500K+,
  4.2★) again — dedicated to plant *diagnosis*, even if not crop-specific by title.
- **PictureThis is a generalist but not the only answer.** It leads only the pest/treatment
  phrasings (`garden pest identifier`, `aphid treatment plants`, `plant fungicide guide`), which are
  arguably the *broader* jobs; the crop-symptom phrasings are led by Agrio, Plantix and small apps,
  not by PictureThis.

**Assessment.** The vertical is **partially a generic job**: for tomato and pepper specifically, the
top answers are the same few general-purpose plant-disease apps recycled across queries, and the
crop-named challengers are sub-scale. But it is **not purely generic**, because a dedicated crop
doctor (Plantix) already holds 10M+ in the crop-disease queries. The honest reading is that the
*generic-plant-ID* apps are **not** the thing standing in the vertical — a *dedicated crop* app is —
so a new entrant's differentiator is not "we are crop-specific" alone (Plantix already is). The
store data cannot say whether Plantix's weakness is crop granularity (tomato vs cucumber) or
something else; that needs a product-level comparison this scan cannot make.

## 5. What this cannot tell us

- **Install buckets are floors, not counts.** `10M+` means "at least 10,000,000", and `500K+` spans
  500,000–999,999. Plantix at 10M+ and PictureThis at 50M+ are one bucket apart by display, but the
  gap could be five-fold or fifty-fold; the data cannot say.
- **Only the top 8 of each query were enriched.** Play's relevance order is not size order, and the
  visible set is display-capped, so a larger incumbent can sit below the cut. In `tomato disease`
  Play showed only **10** cards total and `crop disease identification` only **8**, so the "nothing
  above 500K+" reading for tomato rests on a small visible set.
- **Revenue and conversion are invisible.** A declared badge proves a model was implemented, not
  that it earns. Nothing here says how much Plantix earns, what any in-app purchase costs, or how
  many of PictureThis's 800,000 raters pay.
- **Whether growers would pay for THIS app is not a store-visible fact.** 67/112 declaring IAP shows
  the shelf supports in-app purchases in this domain; it says nothing about a new entrant's
  conversion, price sensitivity, churn or acquisition cost.
- **The query text, not the market, drove five results.** `late blight`, `whitefly treatment`,
  `tomato leaf curl`, `tomato blight` and (partly) `aphid treatment plants` returned fuzzy matches;
  their verdicts and counts describe Play's text match. No conclusion above rests on them.
- **Titles are not capability.** Section 4 judges from titles because that is what the page exposes.
  An app named "Plant Disease Diagnosis App" may or may not handle tomato blight well; this scan
  cannot tell.
- **One storefront, one signed-out session** (`hl=en`, `gl=US`) at the timestamp above. Play search
  is personalised and geo-dependent; this is a reproducible spot check, not a market study.

## 6. Advising treatment on food crops is a higher-stakes claim than identifying a houseplant

This is a separate risk from the market question, and it is surfaced here rather than resolved.

Identifying a houseplant wrongly is a minor disappointment. Advising a grower to apply a pesticide
or fungicide to a plant they will **eat** touches food safety — residue, pre-harvest intervals,
dosage, and the difference between an ornamental and a food crop — and may also touch local
regulation of pesticide advice and sale. A wrong answer can affect someone's health and someone's
crop, not just their satisfaction with an app.

What that implies for the product, in plain terms:

- **Wording should inform, not prescribe.** Describe likely causes and general, non-chemical
  responses, and avoid giving specific chemical names, concentrations or dosing schedules as if they
  were a prescription.
- **The app should route chemical decisions to authoritative local sources** rather than being the
  authority itself.
- **Disclaimers and scope must be explicit and visible**, and the listing and Data safety /
  content declarations must match what the app actually does — the Play compliance floor in
  `AGENTS.md` requires the AI-content and content-rating declarations to be truthful.

**UNVERIFIED — flagged as a question for the human, not answered here:**

- **UNVERIFIED:** whether any specific jurisdiction regulates app-delivered pesticide or fungicide
  advice to food growers, and what a disclaimer must say to satisfy it. No regulation was read for
  this document; the risk is asserted from general food-safety reasoning only.
- **UNVERIFIED:** whether Google Play's policies impose additional requirements on an app that gives
  chemical treatment advice for edible produce (beyond the AI-content and content-rating
  declarations already tracked in `AGENTS.md` and `docs/play-publishing-playbook.md`).
- **UNVERIFIED:** whether any of the 112 enriched incumbents carry such disclaimers — this scan read
  titles, buckets, ratings and badges, never listing text or in-app content.

These three are decisions for the human before this vertical is built, not facts this scan can
settle.

## Bottom line

- **The vertical is fragmented, not owned.** Across the four crop-specific queries (`tomato disease`,
  `pepper plant disease`, `cucumber plant disease`, `vegetable garden disease`) only four apps reach
  500K+ — Plantix at 10M+ (cucumber only), and Agrio, GrowIt and Planter at 500K+ — and every other
  enriched card is 10+–100K+. 40/112 enriched apps overall are unrated.
- **But the generic niche's owner still reaches in.** PictureThis (50M+) appears in 3 of 14 queries,
  and the generalist "plant disease" apps (Agrio, Artmvstd, AppCraftingMinds, Kopal, LeafMind) are
  recycled across up to 8 queries — 68 distinct apps across 112 rows.
- **The vertical monetises.** 67/112 declare IAP and 38/112 declare ads, with 0 unknowns; every
  strong app is free, and the only paid app in the set is sub-scale (1+ installs).
- **The plan-changing caveat:** a *dedicated crop* incumbent already holds 10M+ (Plantix,
  "your crop doctor"). Being crop-specific is therefore not by itself a differentiator — the
  factory needs an angle that beats Plantix, not just an angle that avoids PictureThis.
- **Unresolved and flagged:** the food-safety and regulatory questions in section 6 are marked
  **UNVERIFIED** and need a human decision before build.

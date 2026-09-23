# AI-utility store evidence — 2026-09-23

Purpose: find, from **store evidence rather than opinion**, where an AI capability can be the
engine of a utility app instead of a chat window. `AGENTS.md` ("Defaults to suppress") forbids
*"a thin AI chat wrapper"* and requires *"a differentiated workflow with required content
reporting"*; this document asks which AI-utility shapes survive that constraint on the current
Play search results.

Two artifacts were produced:

- `scripts/scan-ai-niches.mjs` — the AI-utility screen (imports `scripts/lib/play-scan.mjs`;
  that library is **not modified**).
- this document — the findings.

## 1. Reproduction (real run, real exit code)

```text
node --version                      -> v24.11.1
node scripts/scan-ai-niches.mjs     -> exit code 0
last line of output                 -> SCAN COMPLETE: 14/14 queries returned
```

The full stdout of that run (verdict table, per-query detail with incumbent titles, and the JSON
dump of every parsed result) is the sole source of every number below. No number in this document
was computed or estimated by hand.

The script screens 14 queries — the 8 seed queries plus 6 of the same kind — through
`scanQueries(..., { concurrency: 4 })`, the library's bounded default. It imports
`scripts/lib/play-scan.mjs` without modifying it, does not duplicate the general CLI in
`scripts/scan-market.mjs` (owned by another worker), and does not run Gradle.

## 2. How to read the verdicts

The sensor's thresholds and meaning are defined in `scripts/lib/play-scan.mjs` and are not
restated as if they were mine: `ratedShare < 0.35` → `dead-zone`; `weakShare >= 0.5` (ratings below
4.2) → `candidate`; `strongShare >= 0.6` (ratings ≥ 4.5) with `weakShare < 0.3` → `fortress`;
otherwise `mixed`. A rating exists only once Play has enough installs to compute one, so an
unrated result is the *absence* of proof.

## 3. Verdict table (verbatim script output)

```text
query                            verdict    seen  rated  weak  median  why
-------------------------------  ---------  ----  -----  ----  ------  --------------------------------------------------------
plant disease identifier         mixed      10    9/10   4     4.20    demand proven, 4 weak incumbent(s), but 0.22 of rated ar
language pronunciation practice  fortress   30    28/30  6     4.55    18/28 rated incumbents are >= 4.5 with no weak pocket —
receipt scanner expense          fortress   22    13/22  1     4.60    10/13 rated incumbents are >= 4.5 with no weak pocket —
handwriting notes digitize       mixed      30    26/30  7     4.35    demand proven, 7 weak incumbent(s), but 0.42 of rated ar
label reader food additives      mixed      27    11/27  3     4.50    demand proven, 3 weak incumbent(s), but 0.55 of rated ar
bird song identifier             mixed      30    19/30  8     4.30    demand proven, 8 weak incumbent(s), but 0.26 of rated ar
math word problem solver         mixed      14    12/14  2     4.30    demand proven, 2 weak incumbent(s), but 0.42 of rated ar
recipe from ingredients          fortress   20    20/20  2     4.65    17/20 rated incumbents are >= 4.5 with no weak pocket —
mushroom identifier              candidate  11    8/11   4     4.10    4/8 rated incumbents are below 4.2 — demand proven, supp
voice memo transcribe            mixed      25    23/25  5     4.50    demand proven, 5 weak incumbent(s), but 0.52 of rated ar
business card scanner contacts   mixed      11    5/11   0     4.40    demand proven, 0 weak incumbent(s), but 0.4 of rated are
skincare ingredient scanner      mixed      30    18/30  7     4.35    demand proven, 7 weak incumbent(s), but 0.22 of rated ar
screenshot text extractor        mixed      30    20/30  9     4.20    demand proven, 9 weak incumbent(s), but 0.15 of rated ar
document scanner summarize       fortress   14    11/14  1     4.60    8/11 rated incumbents are >= 4.5 with no weak pocket — e
```

`seen` is the number of results visible on that page, which Play display-caps at ~30 — it is a page
limit, **not** a competitor count. `weak`/`rated` are counts; the unreproduced `why` column is
truncated at 56 characters by the library's `formatTable`, so each full reason is quoted per query
in §4.

## 4. Per-query evidence and classification

Every verdict and count below is copied from the script's per-query detail. "AI-first or
traditional" is a **judgment made from the incumbent titles** (plus package ids) printed by the
script — it is not a signal the sensor computes, and it is stated as a title reading rather than an
established fact. The worst incumbent is reported as package id + rating where one exists.

### 4.1 `plant disease identifier` — **ENGINE**

- **Verdict:** `mixed` — *"demand proven, 4 weak incumbent(s), but 0.22 of rated are strong — enter
  only with a specific angle"*.
- **Numbers:** seen 10; rated 9/10 (0.9); weak 4/9 (0.44); strong 2/9 (0.22); median 4.2; min 3.
- **Worst incumbent:** `com.plant.disease.identifier` — 3★.
- **AI-first or traditional (titles):** mixed. AI-first entrants are visible — `AI Plant Doctor:
  Plant Care` (4.4★, `me.jodoin.aiplantdoctor`) and `Plant Disease Diagnosis App` (3.9★) — next to
  traditional agronomy tooling (`Agrio - Plant diagnosis app`, 4.2★) and one generalist identifier
  leader (`PictureThis - Plant Identifier`, 4.5★). The *disease-diagnosis* titles, not the generic
  identification titles, are where the ratings fall (3★, 3.9★, 4.1★).
- **Why ENGINE:** the AI performs one bounded, tedious job (photo → disease + treatment) **inside a
  camera tool**, not in a chat. The generalist leader owns *identification*, while the
  disease-specific pocket is the weak one — 4 of 9 rated incumbents are below 4.2 and strong share
  is only 0.22. A diagnosis is an episodic event, which is the natural fit for a consumable credit
  pack rather than a subscription.
- **Number behind the class:** weak share 0.44 (4/9) with strong share 0.22.

### 4.2 `language pronunciation practice` — **COMMODITY**

- **Verdict:** `fortress` — *"18/28 rated incumbents are >= 4.5 with no weak pocket — entrenched"*.
- **Numbers:** seen 30; rated 28/30 (0.93); weak 6/28 (0.21); strong 18/28 (0.64); median 4.55; min 3.4.
- **Worst incumbent:** `com.gonext.pronunciationapp` — 3.4★.
- **AI-first or traditional (titles):** AI-first. Speech scoring is inherently model-driven and the
  leaders say so — `BoldVoice - Accent Training` (4.7★), `ELSA Speak: English Learning` (4.6★),
  `Spell and Pronounce It Right` (4.8★) — alongside the established `Duolingo: Language Lessons`
  (4.7★).
- **Why COMMODITY:** the job is solved well by entrenched, well-rated incumbents — 18 of 28 rated at
  ≥ 4.5, and the library's no-pocket test (`weakShare < 0.3`) is met at 0.21. There is no weak seam
  to enter through.
- **Number behind the class:** strong share 0.64 (18/28) with weak share 0.21.

### 4.3 `receipt scanner expense` — **COMMODITY**

- **Verdict:** `fortress` — *"10/13 rated incumbents are >= 4.5 with no weak pocket — entrenched"*.
- **Numbers:** seen 22; rated 13/22 (0.59); weak 1/13 (0.08); strong 10/13 (0.77); median 4.6; min 3.9.
- **Worst incumbent:** `com.receiptpalapp.android` — 3.9★.
- **AI-first or traditional (titles):** traditional tooling. The rated leaders read as OCR +
  expense/rewards products, not AI-branded: `Receipt Scanner: Easy Expense` (4.8★),
  `SimplyWise: Receipts, Expenses` (4.7★), `Fetch: Receipts For Gift Cards` (4.7★),
  `Smart Receipts: Expenses & Tax` (4.5★).
- **Why COMMODITY:** the only weak incumbent is one at 3.9★ — weak share 0.08, far below the
  no-pocket threshold — against ten strong ones. The screen sees no entry seam.
- **Number behind the class:** weak share 0.08 (1/13); strong share 0.77.

### 4.4 `handwriting notes digitize` — **COMMODITY**

- **Verdict:** `mixed` — *"demand proven, 7 weak incumbent(s), but 0.42 of rated are strong — enter
  only with a specific angle"*.
- **Numbers:** seen 30; rated 26/30 (0.87); weak 7/26 (0.27); strong 11/26 (0.42); median 4.35; min 3.2.
- **Worst incumbent:** `com.arkdev.writeassign` — 3.2★.
- **AI-first or traditional (titles):** mixed, leaning AI-first. `MyScript Notes: AI Note Taking`
  (4.7★), `Notewise - AI Notes, PDF, Docs` (4.3★) and an unrated `AI Handwritten Text Converter` sit
  next to plain OCR utilities (`Handwriting to Text Converter`, 4.3★ / 4.6★).
- **Why COMMODITY:** although the verdict is `mixed`, weak share is 0.27 — under the library's own
  0.3 no-pocket threshold — and the field contains a strong OCR specialist (MyScript, 4.7★). There
  is no defensible seam.
- **Number behind the class:** weak share 0.27 (< the 0.3 no-pocket line), median 4.35.

### 4.5 `label reader food additives` — **COMMODITY**

- **Verdict:** `mixed` — *"demand proven, 3 weak incumbent(s), but 0.55 of rated are strong — enter
  only with a specific angle"*.
- **Numbers:** seen 27; rated 11/27 (0.41); weak 3/11 (0.27); strong 6/11 (0.55); median 4.5; min 3.4.
- **Worst incumbent:** `com.fooshtech.foodcheckai` — 3.4★.
- **AI-first or traditional (titles):** traditional tooling. These read as additive databases and
  barcode/label scanners, not models: `Yuka - Food & Cosmetic Scanner` (4.7★),
  `AvoidAdditives: Additives` (4.3★), `E Numbers - Food additives` (5★, plus an unrated PRO
  build), `E-Codes: Food Additives` (4.8★).
- **Why COMMODITY:** weak share 0.27 is under the 0.3 no-pocket line while a well-rated, broadly
  adopted incumbent (Yuka, 4.7★) already performs the scan. The screen cannot see whether Yuka is
  free (see §6), so the "free" half of the definition is not established here — the entrenched
  supply half is (strong share 0.55, weak 0.27).
- **Number behind the class:** weak share 0.27; strong share 0.55.

### 4.6 `bird song identifier` — **COMMODITY**

- **Verdict:** `mixed` — *"demand proven, 8 weak incumbent(s), but 0.26 of rated are strong — enter
  only with a specific angle"*.
- **Numbers:** seen 30; rated 19/30 (0.63); weak 8/19 (0.42); strong 5/19 (0.26); median 4.3; min 3.6.
- **Worst incumbent:** `com.bird.identifier.picturebird` — 3.6★.
- **AI-first or traditional (titles):** AI-first. Audio classification is the product: `Merlin Bird
  ID by Cornell Lab` (4.9★), `Bird Identifier Sound Call ID` (4.5★), `BirdNET` (4★, a research
  project), `Birda - Birding Made Better` (4.3★).
- **Why COMMODITY, despite a real weak pocket:** this is the most interesting rejection in the set.
  The numbers look like a pocket (weak 0.42, strong 0.26), but the leader — `Merlin Bird ID by
  Cornell Lab`, 4.9★ — is a **free, institutional** product, and the same 4.9★ rating plus a
  university-lab title is the strongest single incumbent anywhere in this scan. A new entrant must
  beat a free leader, not a paid weak one.
- **Caveat, stated because the sensor missed it:** `scanQuery`'s `organisations` list was **empty**
  for this query, although the leader's title names an institution. The library only matches a
  fixed token list and "lab" / "Cornell" are not in it, so the institutional signal did not surface
  — this judgment comes from reading the title, exactly the human call `play-scan.mjs` documents as
  its blind spot.
- **Number behind the class:** leader at 4.9★ (institutional, free per title) against weak share 0.42.

### 4.7 `math word problem solver` — **COMMODITY**

- **Verdict:** `mixed` — *"demand proven, 2 weak incumbent(s), but 0.42 of rated are strong — enter
  only with a specific angle"*.
- **Numbers:** seen 14; rated 12/14 (0.86); weak 2/12 (0.17); strong 5/12 (0.42); median 4.3; min 3.4.
- **Worst incumbent:** `com.malmath.apps.mm` — 3.4★.
- **AI-first or traditional (titles):** both, with the AI-branded entrants rated highest:
  `MathGPT - AI Math Solver` (4.8★), `Solvely-Math Solver & Homework` (4.7★), `Symbolab: AI Math
  Solver` (4.3★) next to the established tool-shaped incumbents `Photomath` (4.3★), `Cymath` (4.3★),
  `Mathway` (4.1★).
- **Why COMMODITY:** only 2 of 12 rated incumbents are weak (0.17) — no seam — and the field is
  crowded with well-known solvers. Note for the wrapper question: the *highest-rated* entrants
  (MathGPT 4.8★, Solvely 4.7★) are the most prompt-like, so a chat-shaped product is beaten here on
  ratings, not merely forbidden on policy.
- **Number behind the class:** weak share 0.17 (2/12).

### 4.8 `recipe from ingredients` — **COMMODITY**

- **Verdict:** `fortress` — *"17/20 rated incumbents are >= 4.5 with no weak pocket — entrenched"*.
- **Numbers:** seen 20; rated 20/20 (1); weak 2/20 (0.1); strong 17/20 (0.85); median 4.65; min 3.2.
- **Worst incumbent:** `com.cooklist.android` — 3.2★.
- **AI-first or traditional (titles):** traditional. These are content/recipe products, not
  AI-branded: `SuperCook - Recipe Generator` (4.6★), `Cookpad recipes, homemade food` (4.6★),
  `ReciMe: Recipes & Meal Planner` (4.7★), `Tasty` (4.7★), `Recipe Keeper` (4.7★).
- **Why COMMODITY:** every visible result is rated, and 17 of 20 are ≥ 4.5 (strong 0.85) — the
  hardest field in the scan.
- **Number behind the class:** strong share 0.85 (17/20); weak 0.1.

### 4.9 `mushroom identifier` — **ENGINE**

- **Verdict:** `candidate` — *"4/8 rated incumbents are below 4.2 — demand proven, supply
  inadequate"*. This is the only `candidate` verdict in the 14 queries.
- **Numbers:** seen 11; rated 8/11 (0.73); weak 4/8 (0.5); strong 1/8 (0.13); median 4.1; min 3.
- **Worst incumbent:** `com.jg.mushroomidentifier` — 3★.
- **AI-first or traditional (titles):** mixed, leaning AI-first. Vision-first entrants —
  `Mushroom Identifier - Fungi ID` (4.3★, `com.ai.mushroom_identifier`), `Picture Mushroom -
  Mushroom ID` (4.2★) — alongside field-guide-style apps (`Shroomify - USA Mushroom ID`, 4.6★;
  `ShroomID`, 4★).
- **Why ENGINE:** the AI performs one bounded job (photo → species) inside a camera tool; the weak
  share is the highest measured (0.50) with the lowest strong share (0.13) and the lowest median
  (4.1) of all 14 queries; and an identification is episodic, so per-use credit fits. **Standing
  caveat:** a wrong identification can cause poisoning, so this pocket carries the heaviest
  correctness and policy burden of the two ENGINE shapes and must not be built as a toy.
- **Number behind the class:** weak share 0.50 (4/8), strong share 0.13, verdict `candidate`.

### 4.10 `voice memo transcribe` — **COMMODITY**

- **Verdict:** `mixed` — *"demand proven, 5 weak incumbent(s), but 0.52 of rated are strong — enter
  only with a specific angle"*.
- **Numbers:** seen 25; rated 23/25 (0.92); weak 5/23 (0.22); strong 12/23 (0.52); median 4.5; min 3.3.
- **Worst incumbent:** `com.google.android.apps.recorder` — 3.3★.
- **AI-first or traditional (titles):** AI-first. `OtterAI Transcribe Voice Notes` (4.8★),
  `Transcribe Speech to Text` (4★), `Transcribe Audio To Text` (4.6★) dominate the transcription
  space; the 3.3★ outlier is Google's own recorder.
- **Why COMMODITY:** weak share 0.22 is under the 0.3 no-pocket line, and transcription is served
  by a strong specialist (Otter, 4.8★) as well as a first-party app. Even the weakest visible
  incumbent is a free platform tool, which is a weak seam to attack.
- **Number behind the class:** weak share 0.22; strong share 0.52.

### 4.11 `business card scanner contacts` — **COMMODITY**

- **Verdict:** `mixed` — *"demand proven, 0 weak incumbent(s), but 0.4 of rated are strong — enter
  only with a specific angle"*.
- **Numbers:** seen 11; rated 5/11 (0.45); weak 0/5 (0); strong 2/5 (0.4); median 4.4; min 4.2.
- **Worst incumbent:** `com.beamscan.app` — 4.2★ (the *lowest* rated result; there are no weak ones).
- **AI-first or traditional (titles):** mixed, and the AI-labeled entrants are the untested ones.
  Unrated: `AI Business Card Scanner` (`com.forward99`), `AI Business Card Scanner`
  (`com.krupeshanadkat.business_card_scanner`), `Business Card Scanner CardSnap`
  (`com.ai.card.scanner`) — three thin, unrated AI-titled listings. Rated: `Covve - Business Card
  Scanner` (4.7★), `BusinessCards` (4.8★), `BizConnect - Card Scanner` (4.4★).
- **Why COMMODITY:** zero weak incumbents — the entire rated field is at or above 4.2★. There is no
  seam; and the AI-titled entrants show the wrapper failure mode directly (unrated after trying).
  Note the rated share is 0.45, close to the 0.35 dead-zone floor, so the proof of demand is thin.
- **Number behind the class:** weak share 0.00 (0/5); min rating 4.2.

### 4.12 `skincare ingredient scanner` — **COMMODITY**

- **Verdict:** `mixed` — *"demand proven, 7 weak incumbent(s), but 0.22 of rated are strong — enter
  only with a specific angle"*.
- **Numbers:** seen 30; rated 18/30 (0.6); weak 7/18 (0.39); strong 4/18 (0.22); median 4.35; min 2.9.
- **Worst incumbent:** `yt.master.safely.inci` — 2.9★ (the lowest rating in the whole scan).
- **AI-first or traditional (titles):** traditional tooling. Ingredient databases and scanners:
  `OnSkin - Skincare Scanner` (4.4★), `SkinSort - Skincare Scanner` (4.4★), `INCI Beauty - Cosmetic
  scanner` (4.8★), `Yuka - Food & Cosmetic Scanner` (4.7★), `SkinSAFE: Ingredient Scanner` (4.4★).
- **Why COMMODITY — and why it is the closest call in the set:** the numbers pass the weak-pocket
  test (weak 0.39, strong 0.22), but the *same* dominant incumbent that owns §4.5 appears in this
  result set too — `Yuka - Food & Cosmetic Scanner`, 4.7★ — plus a cosmetic-specific leader,
  `INCI Beauty`, 4.8★. A new entrant would be fighting a free, entrenched, cross-domain scanner over
  the same barcode scan. The 2.9★ outlier is evidence the seam exists, not that it can be held.
- **Number behind the class:** weak share 0.39, but two leaders at 4.7★/4.8★ already do the same scan.

### 4.13 `screenshot text extractor` — **COMMODITY**

- **Verdict:** `mixed` — *"demand proven, 9 weak incumbent(s), but 0.15 of rated are strong — enter
  only with a specific angle"*.
- **Numbers:** seen 30; rated 20/30 (0.67); weak 9/20 (0.45); strong 3/20 (0.15); median 4.2; min 3.2.
- **Worst incumbent:** `com.textsnap.converter` — 3.2★.
- **AI-first or traditional (titles):** traditional OCR, with one AI-labeled entrant:
  `Image To Text` (4.2★), `OCR Image to Text Converter` (4.3★), `AI Text Scanner: Image To Text`
  (4.1★), `Text Fairy (OCR Text Scanner)` (4.5★).
- **Why COMMODITY:** the *supply* here is genuinely weak (weak 0.45, strong 0.15 — the largest weak
  pocket after mushroom), so it fails the ENGINE test on its other leg: optical character
  recognition is a per-scan commodity that the platform already provides, and the screen sees nothing
  to suggest users pay per extraction. This is the clearest case of *weak supply ≠ an engine* — a
  pocket with no willingness to pay in it. The "platform already provides it" half is not visible to
  this screen and is flagged UNVERIFIED in §7.
- **Number behind the class:** weak share 0.45 with strong only 0.15, against no pay-per-use story.

### 4.14 `document scanner summarize` — **WRAPPER**

- **Verdict:** `fortress` — *"8/11 rated incumbents are >= 4.5 with no weak pocket — entrenched"*.
- **Numbers:** seen 14; rated 11/14 (0.79); weak 1/11 (0.09); strong 8/11 (0.73); median 4.6; min 3.6.
- **Worst incumbent:** `com.textsummarizer.summarizer` — 3.6★.
- **AI-first or traditional (titles):** AI-first. `Summary - AI Note Taker` (4.6★), `Adobe Scan AI
  PDF Scanner, OCR` (4.6★), `AI PDF Summarizer－PDF Summary` (unrated), `Text Summary - TLDR
  Summarize` (4.4★).
- **Why WRAPPER:** strip the word "scanner" and the product is *"give a document, get a summary"* —
  a model call with no sensor, tool, or owned content around it, which is the shape `AGENTS.md`
  rejects. The market verdict independently rejects it as well (strong 0.73, weak 0.09), so this
  shape fails on both the policy test and the store data. It is included because it is the only
  query in the set whose core deliverable is a prompt.
- **Number behind the class:** strong share 0.73 with a fortress verdict; and the product is one
  model call.

## 5. Classification summary

| Query | Verdict | rated | weak/rated | strong/rated | Worst incumbent | Class |
|---|---|---:|---:|---:|---|---|
| `plant disease identifier` | mixed | 9/10 | 0.44 | 0.22 | `com.plant.disease.identifier` 3★ | **ENGINE** |
| `language pronunciation practice` | fortress | 28/30 | 0.21 | 0.64 | `com.gonext.pronunciationapp` 3.4★ | COMMODITY |
| `receipt scanner expense` | fortress | 13/22 | 0.08 | 0.77 | `com.receiptpalapp.android` 3.9★ | COMMODITY |
| `handwriting notes digitize` | mixed | 26/30 | 0.27 | 0.42 | `com.arkdev.writeassign` 3.2★ | COMMODITY |
| `label reader food additives` | mixed | 11/27 | 0.27 | 0.55 | `com.fooshtech.foodcheckai` 3.4★ | COMMODITY |
| `bird song identifier` | mixed | 19/30 | 0.42 | 0.26 | `com.bird.identifier.picturebird` 3.6★ | COMMODITY |
| `math word problem solver` | mixed | 12/14 | 0.17 | 0.42 | `com.malmath.apps.mm` 3.4★ | COMMODITY |
| `recipe from ingredients` | fortress | 20/20 | 0.10 | 0.85 | `com.cooklist.android` 3.2★ | COMMODITY |
| `mushroom identifier` | candidate | 8/11 | 0.50 | 0.13 | `com.jg.mushroomidentifier` 3★ | **ENGINE** |
| `voice memo transcribe` | mixed | 23/25 | 0.22 | 0.52 | `com.google.android.apps.recorder` 3.3★ | COMMODITY |
| `business card scanner contacts` | mixed | 5/11 | 0.00 | 0.40 | `com.beamscan.app` 4.2★ | COMMODITY |
| `skincare ingredient scanner` | mixed | 18/30 | 0.39 | 0.22 | `yt.master.safely.inci` 2.9★ | COMMODITY |
| `screenshot text extractor` | mixed | 20/30 | 0.45 | 0.15 | `com.textsnap.converter` 3.2★ | COMMODITY |
| `document scanner summarize` | fortress | 11/14 | 0.09 | 0.73 | `com.textsummarizer.summarizer` 3.6★ | **WRAPPER** |

Count: **2 ENGINE, 1 WRAPPER, 11 COMMODITY.**

## 6. What the surviving shape actually looks like

The two ENGINE pockets share one shape, and it is not "an AI app":

> a **camera-native utility** where the model does a single bounded identification the user cannot
> do themselves, on an episodic event, in a niche whose *specific* incumbent titles are weak even
> when an adjacent generalist is strong.

- `plant disease identifier` — weak share 0.44, strong 0.22; the generalist leader owns identification,
  the diagnosis titles are weak.
- `mushroom identifier` — `candidate`; weak share 0.50, strong 0.13, median 4.1; the weakest supply
  measured.

Both are **IDENTIFY → ACT** workflows (diagnose a plant; name a mushroom), both need an owned
reference corpus for the action half (treatment / edibility), and both are suited to a consumable
credit per identification rather than a perpetual unlock. The owned corpus is what keeps them out of
the WRAPPER class: the model is not the product, the *answer* is.

The counterexample that makes the rule honest is `bird song identifier` (§4.6): structurally the
same camera/mic-native identification shape, weak share 0.42 — yet rejected as COMMODITY because a
free institutional leader (Cornell Lab, 4.9★) already owns it. **Weak incumbents alone are not the
engine; weak incumbents with no free institutional leader in the same result set are.**

`skincare ingredient scanner` (§4.12) is the warning inside the same rule: a real weak pocket (0.39)
that is nonetheless owned by a strong cross-domain incumbent (Yuka, 4.7★).

## 7. What the store data cannot tell us

This screen is a ratings read of one signed-out storefront. It establishes far less than a go/no-go,
and the following are **not** established by anything above:

- **Ratings, not install counts.** The sensor reads the visible rating, never install or review
  counts. A 4.9★ incumbent could have 200 installs or 200 million; this screen cannot tell, and no
  revenue or reach conclusion follows from it. A per-app detail fetcher is being built separately by
  another worker; until it lands, every "dominant incumbent" claim here means *well-rated and
  visible*, not *large*.
- **No willingness-to-pay signal at all.** The store page shows neither price, nor IAP products, nor
  subscription status. **Every "a user would plausibly pay per use" statement in §4 is a hypothesis
  about the job, not a measurement.** The ENGINE label means "worth testing a pack against", never
  "will convert".
- **No output-quality judgment.** The screen reads titles and star averages. It cannot tell whether
  an incumbent's model is accurate, whether its diagnosis is safe, or whether its UI is usable. A
  weak rating may mean bad output, bad marketing, or bad luck — indistinguishable here.
- **No pricing or monetization visibility.** "Free" in the COMMODITY definition is inferred from the
  title/known product, not read from the store page (see §4.5, §4.6). Where an entry leans on "free
  incumbent", that half is UNVERIFIED (§8).
- **No review text and no sentiment.** Only the star average is parsed; nothing about *why* a rating
  is low is available.
- **Display cap, single storefront, single date.** ~30 results is a page limit, not a market count;
  the read is `hl=en&gl=US` and signed out, so it is one spot check, not the account's reachable
  market; and it is a snapshot on 2026-09-23.
- **Institutional strength is still a human call.** `organisationLikeDevelopers` matched nothing for
  `bird song identifier` even though the leader is a university lab (§4.6), because its token list
  does not include "lab". Institutional power must be read from titles, as was done here.
- **It cannot distinguish AI-first from traditional tooling.** That judgment in §4 is mine, made
  from titles, and a title is marketing — `AI Plant Doctor` may or may not call a model.
- **It says nothing about the wrapper policy itself.** Whether a *proposed* design is a thin wrapper
  is an `AGENTS.md` judgment; this scan only shows how incumbent shapes are rated.

## 8. UNVERIFIED items, collected

Marked so no later reader treats them as measured:

1. **"free"** for any incumbent (Yuka §4.5/§4.12, Cornell Lab's Merlin §4.6, Google's recorder
   §4.10) — the store page's price is not read by this screen. What *is* verified is the rating and
   the institutional/first-party title.
2. **"the platform already provides OCR"** (§4.13) — not visible to the screen; stated as the reason
   the weak pocket is not an engine, and it must be re-checked before acting on it.
3. **Install/review-count magnitude** for every "leader" claim — deferred to the separate per-app
   detail fetcher.
4. **Willingness to pay per use** for the two ENGINE shapes — a design hypothesis, not evidence.
5. **Output correctness/safety** in the mushroom pocket — cannot be read from ratings; the poisoning
   risk in §4.9 is a general safety consideration, not a store measurement.

## 9. Bottom line

From 14 real query reads: **2 ENGINE** shapes (`plant disease identifier`, `mushroom identifier`),
**1 WRAPPER** (`document scanner summarize`), **11 COMMODITY**. The AI-utility surface at the top of
Play search is mostly already-commoditized tooling; the two live pockets are camera-native
identification niches with weak *specific* incumbents and no free institutional leader — and both
are episodic enough that a consumable credit is the shape to test, with the mushroom pocket carrying
a hard correctness-and-safety burden before it is worth a slot.

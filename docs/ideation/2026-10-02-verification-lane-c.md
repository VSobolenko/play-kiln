# Verification lane C — `rast-saz-oud-tuner` (Rast: Saz & Oud Tuner)

Independent verification of the recipe `docs/ideation/2026-10-02-niche-real-time-tools.md`
and `docs/ideation/plans/rast-saz-oud-tuner.*.json`, run 2026-10-02 by a verifier that did not
write them. Every value below was read **live from Google Play in this session** through the
factory's own sensors (Node v24.11.1, from `d:\Python\PlayKiln`). Nothing here is a recollection
from another file.

Search is signed-out Play. Install counts are Play's rounded buckets; the schema.org twin carries
the exact unrounded count, and both are printed below as the tool prints them
(`<display> (<schema.org twin>)`).

---

## 1. Verdict table

| # | Claim | Claimed value | Observed value (live) | Verdict |
|---|---|---|---|---|
| C1 | `com.visne.baglamatuner` | 4.4★, 8,020 exact reviews, 1M+, ads yes, IAP yes | 4.4★, 8,020 (8,023), 1M+ (≥1,000,000), ads true, IAP true, free | **CONFIRMED** |
| C2 | `com.baglamatuner` | 4.2★, 2,560 exact reviews, 1M+, ads yes, IAP no | 4.2★, 2,560 (2,555), 1M+ (≥1,000,000), ads true, IAP false, free | **CONFIRMED** |
| C3 | `com.visne.oudtuner` | 4.1★, 2,970 exact reviews, 500K+, ads yes, IAP yes | 4.1★, 2,970 (2,974), 500K+ (≥500,000), ads true, IAP true, free | **CONFIRMED** |
| C4 | `tr.com.alyaka.alper.professionaloud` | 2.9★, 456 exact reviews, 100K+ | 2.9★, 456 (456), 100K+ (≥100,000) | **CONFIRMED** |
| C5 | Fully-paid, no-ads, no-IAP incumbent at ≈$4.49 with ≈4,030 reviews; free twin ≈14,300 at 1M+ | as claimed | `com.psslabs.rhythmpaid` "Rhythm Tabla & Tanpura PREMIUM" 4.7★, 4,030 (4,032), 50K+, ads false, IAP false, **paid $4.49**; twin `com.psslabs.rhythm` "Rhythm with Tabla & Tanpura" 4.3★, 14,300 (14,310), 1M+, ads true, IAP false, free | **CONFIRMED** |
| C6 | Four complaint strings occur verbatim in the shelf's critical reviews | "full screen ads every 30 seconds" · "Only 2 available tunings for free" · "Accuracy is very bad" · "freez and dont hear my saz" | All four found **verbatim** (see §2) | **CONFIRMED** |
| C7 | Guard was over-confident: `baglama tuner` ≈0.97 vs ≈0.53 hand; `saz tuner` ≈1.00 vs ≈0.67 hand | 0.97/0.53 · 1.00/0.67 | `baglama tuner` guard on-topic 29/30 **0.97**, hand job-share 16/30 **0.53**; `saz tuner` guard on-topic 30/30 **1.00**, hand job-share 20/30 **0.67** | **CONFIRMED** |
| C8 | No app titled 'Rast' on the `rast tuner` result page | none | 30 visible titles, **no app titled "Rast"**; `scan-market` verdict `mixed`, relevance `partial` | **CONFIRMED** |

**Counts: 8 CONFIRMED · 0 WRONG · 0 NOT-CHECKED.**

The recipe's headline table (§7) also lists `org.umitates.baglamatuner` (unrated, 100K+),
`com.kme.baglamatuner` (unrated, 50K+), `com.saztunes.app` (unrated, 50K+), `com.kme.oudtuner`
(4.0★, 158, 10K+) — all four re-observed live and matching.

---

## 2. Detail on C6 (the four strings, verbatim)

Observed in the live `scan-complaints.mjs` output over the four packages:

- `com.visne.oudtuner`, 1★: *"Not working consistently Cant use the app continuously because i ak
  hit with full screen ads every 30 seconds that are not only annoying, but also cau"* — contains
  **"full screen ads every 30 seconds"**.
- `com.visne.oudtuner`, 1★: *"Accuracy is very bad. Only 2 available tunings for free, the rest is
  paid and probably with the same mediocre accuracy."* — contains **"Accuracy is very bad"** and
  **"Only 2 available tunings for free"**.
- `com.baglamatuner`, 1★: *"This app freez and dont hear my saz"* — contains
  **"freez and dont hear my saz"** verbatim.

All four strings the verifier was asked to find exist word-for-word in reviews Play served this
session. The recipe's rendering of the second oud quote as *"...that are not only annoying, but also
cau[sing]"* is a truncation of the live text (Play cuts it), which is faithful.

---

## 3. WRONG / NOT-CHECKED — and one number that did not reproduce (outside C1–C8)

**Nothing in C1–C8 is wrong or unverified.** One load-bearing *prose* number in the recipe did not
reproduce, and it is worth recording because it is cited as the reason the ads-led archetype was
rejected.

Recipe §3: *"the loudest, most uniform complaint in the pocket is the ads (7 of 13 critical
baglama-tuner reviews, 54%)"*; the evidence file repeats *"7 of 13 critical reviews were ads (54%)"*.

Live this session (`node scripts/scan-complaints.mjs --query "baglama tuner"`):
**pocket 15 reviews, 11 critical, 7 ads (64%)**, with further classes broken 2, paywall 2, regression
2, account 1, accuracy 1, crash 1, offline 1, value 1.

- The **absolute ads count (7)** is stable and **ads is still the top class by a wide margin**
  (7 of 11 critical, next class 2). The qualitative claim — ads are the shared complaint of the
  pocket — **survives and is if anything stronger**.
- The **fraction does not reproduce**: 54% (7/13) is observed as 64% (7/11). The denominator depends
  on which "weakest rated incumbents" the query scan selects and on Play's three-reviews-per-app
  sample, so it is sample-dependent and is not stable enough to be quoted as a fixed percentage.
  The recipe's own class list is also internally over-counted (its classes sum to 19 against a
  denominator of 13, because one review carries several classes), which is characteristic of this
  histogram rather than a defect unique to this run.
- **Impact on the arithmetic: none.** The recipe's revenue division (§8) rests on the $4.99 price,
  the 15%/2%/2% deductions and the 2% conversion assumption — none of which depend on 54% vs 64%.
  The number is load-bearing only for the *archetype-rejection prose* (§3 rejects `ads_led_utility`
  because "the loudest, most uniform complaint ... is the ads"). That rejection stands: ads is the
  top class in both readings.

**Not verified here (and not asked as C-claims), labelled honestly:**

- The promise evidence `drone-and-perde-guide` asserts *"0 of the 8 enriched saz/oud apps offer a
  microtonal fret map or a drone"*. The market scanner reads ratings/installs/ads/IAP, **not feature
  inventories**; this verifier did not read eight app descriptions, so that claim is `UNVERIFIED`
  from my session (the command that would decide it is opening each enriched listing's description).
- The recipe labels the 2% install→unlock conversion, the 15% fee and the 2% allowances as plan
  assumptions, not measurements — I confirm only that they are *labelled* as such in
  `economics.json.assumptionsUnverified`.

---

## 4. Does the demand reading survive, or is it homonym pollution?

**It survives — the demand is anchored on apps that are genuinely on the job, not on homonyms.**

- The four enriched leaders the recipe relies on are all genuinely saz/bağlama/oud apps by title and
  publisher: `com.visne.baglamatuner` (Baglama Tuner: Bağlama & Saz, CherrySoft, **8,023 reviews,
  1M+**), `com.baglamatuner` (Baglama Tuner, Morfozz, **2,555 reviews, 1M+**), `com.visne.oudtuner`
  (Ud Tuner - Oud Tuner, CherrySoft, **2,974 reviews, 500K+**),
  `tr.com.alyaka.alper.professionaloud` (Professional Oud, **456 reviews, 100K+**). These are rated
  incumbents with real review counts on the *specific* instrument, so the pocket is not an artifact
  of the "tuner" token dragging in guitar apps.
- The guard over-confidence C7 confirms is real and **is confined to the surrounding shelf**: for
  `baglama tuner`, 14 of 30 visible cards are generic guitar/instrument tuners the guard counted as
  on-topic (0.97), and the hand job-share is 0.53. For `saz tuner` the guard's 1.00 is really 0.67.
  The recipe already discounts the guard and anchors to the enriched per-app facts, which is the
  correct response.
- **Caveat carried into the report, not hidden:** the *enriched top-8* also mixes in generic apps
  (Soundcorset 4.6★/50M+, Perfect Tuner 4.1★/10M+ appear inside the `saz tuner` and
  `baglama tuner` top-8), and the shelf median the recipe quotes (4.45 / 4.50) is partly generic.
  The specific field is **two rated 1M+ apps + one rated 500K+ app + five unrated entrants at
  50K–100K+** — real, rated demand with a reachable weak mid-tier at 4.1–4.4★, exactly as the recipe
  states.
- **Unrated incumbents are not treated as vacancy here** — several `saz`/`baglama` entrants are
  unrated (`com.saztunes.app`, `com.kme.baglamatuner`, `org.umitates.baglamatuner`, `saz.tuning`,
  `com.fiberapps.saztuner`). They are a caution, not a green field, and the recipe says so.

**One arithmetic observation the verifier should flag (INFERRED, not a claimed number).** C5's
confirmed facts cut in two directions for the chosen `purchase_led_single_unlock` model: the paid
no-ads `com.psslabs.rhythmpaid` at $4.49 sits at **50K+ installs / 4,030 reviews** while its free
twin `com.psslabs.rhythm` sits at **1M+ / 14,300 reviews** — the paid sibling reaches roughly one
twentieth of the free twin's install bucket. That is consistent with the recipe's own honest
unknowns (conversion is unassumed and unmeasured) and does not refute the plan, but it is the closest
thing on the shelf to a reading on *whether a paid music-practice unlock converts* — and it reads
"paid = far fewer installs." The 2% conversion assumption remains the single unmeasured load-bearing
number in the arithmetic.

---

## 5. Claim ledger

```text
CLAIM                                                                        LABEL      EVIDENCE
C1 com.visne.baglamatuner 4.4★ 8,020(8,023) 1M+ ads yes IAP yes              MEASURED   enrich-market.mjs --query "saz tuner" (row 1)
C2 com.baglamatuner 4.2★ 2,560(2,555) 1M+ ads yes IAP no                     MEASURED   enrich-market.mjs --query "baglama tuner" (row 2)
C3 com.visne.oudtuner 4.1★ 2,970(2,974) 500K+ ads yes IAP yes                MEASURED   enrich-market.mjs --query "oud tuner" (row 1)
C4 tr.com.alyaka.alper.professionaloud 2.9★ 456(456) 100K+                   MEASURED   enrich-market.mjs --query "oud tuner" (row 6)
C5 com.psslabs.rhythmpaid paid $4.49, ads false, IAP false, 4,030(4,032)     MEASURED   enrich-market.mjs --query "tabla metronome" (row 7)
C5 twin com.psslabs.rhythm free 4.3★ 14,300(14,310) 1M+ ads true            MEASURED   enrich-market.mjs --query "tabla metronome" (row 2) / "tanpura app" (row 3)
C6 "full screen ads every 30 seconds" verbatim                              MEASURED   scan-complaints.mjs --package com.visne.oudtuner
C6 "Only 2 available tunings for free" verbatim                             MEASURED   scan-complaints.mjs --package com.visne.oudtuner
C6 "Accuracy is very bad" verbatim                                          MEASURED   scan-complaints.mjs --package com.visne.oudtuner
C6 "freez and dont hear my saz" verbatim                                    MEASURED   scan-complaints.mjs --package com.baglamatuner
C7 baglama tuner guard 0.97 vs hand 0.53                                    MEASURED   scan-titles.mjs --query "baglama tuner" (29/30 vs 16/30)
C7 saz tuner guard 1.00 vs hand 0.67                                        MEASURED   scan-titles.mjs --query "saz tuner" (30/30 vs 20/30)
C8 no app titled "Rast" on the rast tuner page                             MEASURED   scan-titles.mjs --query "rast tuner" (30 titles, none "Rast"); scan-market.mjs --query "rast tuner" → mixed/partial
C4' tr.com.alyaka.professionaloud critical reviews are monophonic-centric   MEASURED   scan-complaints.mjs --package tr.com.alyaka.alper.professionaloud (2 of 3 critical = "can't play 2 notes")
recipe §3 "7 of 13 critical baglama reviews were ads (54%)" NOT REPRODUCED  MEASURED   scan-complaints.mjs --query "baglama tuner" → 7 of 11 critical (64%)
recipe §3 "ads = loudest shared complaint of the pocket"                    MEASURED   same run: ads is top class (7), next class 2
promise drone-and-perde-guide "0 of 8 enriched apps offer fret map or drone" UNVERIFIED market scanner reads no feature inventory; needs each listing's description
2% install→unlock conversion                                                UNVERIFIED labelled a plan assumption in economics.json.assumptionsUnverified; unmeasured
generic tuners pad the shelf (Soundcorset 4.6/50M+, Perfect Tuner 4.1/10M+) MEASURED   enrich-market.mjs top-8 rows for "saz tuner"/"baglama tuner"
```

---

## 6. Commands run (all from `d:\Python\PlayKiln`, exit 0)

```bash
node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "saz tuner" --query "baglama tuner" \
  --query "oud tuner" --query "tabla metronome" --query "tanpura app"
node scripts/scan-complaints.mjs --package com.visne.baglamatuner --package com.baglamatuner \
  --package com.visne.oudtuner --package tr.com.alyaka.alper.professionaloud
node scripts/scan-complaints.mjs --query "baglama tuner"
node scripts/scan-titles.mjs --concurrency 3 --query "baglama tuner" \
  --on-job "\b(saz|baglama|bağlama|oud|tuner|makam|perde)\b" --off-job "\b(guitar|violin|piano|karaoke|radio)\b"
node scripts/scan-titles.mjs --concurrency 3 --query "saz tuner" \
  --on-job "\b(saz|baglama|bağlama|oud|tuner|makam|perde)\b" --off-job "\b(guitar|violin|piano|karaoke|radio)\b"
node scripts/scan-titles.mjs --concurrency 3 --query "rast tuner" \
  --on-job "\b(rast|saz|baglama|oud|tuner|makam|perde)\b" --off-job "\b(guitar|violin|piano|karaoke)\b"
node scripts/scan-market.mjs --concurrency 4 --query "rast tuner" --query "habit tracker" --query "bjj training log"
```

Controls held in the `scan-market` run: `habit tracker` → `fortress`, `bjj training log` →
`dead-zone`, so the sensor was not drifting when the `rast tuner` screen was read.

## 7. Bottom line

Every load-bearing number the recipe puts in front of the owner is **CONFIRMED live**: the two 1M+
bağlama tuners, the 500K+ oud tuner, the 2.9★ 100K+ oud app, the $4.49 paid tabla/tanpura incumbent
with its free twin, the four complaint quotes, the guard-vs-hand confidence gap, and the clear
`rast tuner` page. The pocket's demand reading is **real and on the job**, not homonym pollution. The
one number that did not reproduce (54% → 64% ad-complaint share of the baglama pocket) is
sample-dependent prose that strengthens rather than weakens the ads-driven wedge, and it does not
enter the revenue division. The arithmetic's exposure is unchanged and honestly labelled: the 2%
install→unlock conversion is the unmeasured load-bearing assumption, and the shelf's own paid twin
(4,030 reviews at 50K+ against the free twin's 14,300 at 1M+) is a caution, not a refutation.

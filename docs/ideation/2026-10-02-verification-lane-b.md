# Lane B verification — `rook-encounter-generator` (Rook: Encounter Generator)

**Verifier:** independent PlayKiln verifier (did not author the recipe; did not see the recon conversation)
**Date:** 2026-10-02 · **Session:** live Google Play reads on this machine, Windows/PowerShell
**Subject:** `docs/ideation/2026-10-02-generator-and-creative-tools.md`,
`docs/ideation/plans/rook-encounter-generator.{economics,differentiators,naming}.json`,
`docs/research/2026-10-02-generator-and-creative-evidence.md`, and the money owner's
`docs/ideation/2026-10-02-lane-pricing-worksheet.md` §3 / §5.
**Read boundary honoured:** nothing under `apps/**` was opened; no recipe file was edited; this file is the only write.
**Method:** every number below was printed by a command run in this session; a value recalled from a
document is labelled, never confirmed. Play install counts are rounded buckets (floors); the
unrounded review count is the schema.org twin. The enrichment table labels the two as
`reviews (exact)` = `ratingCount (ratingCountExact)` — the first number is Play's rounded display,
the parenthetical is the unrounded `schema.org` count (`scripts/lib/play-app-detail.mjs:255-257`).

---

## Verdict table

| # | Claim | Claimed value | Observed value (live, this session) | Verdict |
|---|---|---|---|---|
| B1 | `com.lionsden.gamemaster5` (Game Master 5th Edition) | 4.1★, **1,610 exact** reviews, 500K+ installs, ads yes, IAP yes | 4.1★; `reviews (exact)` = **1,610 (1,611)**; 500K+ (`>= 500,000`); `ads = true`; `iap = true` | **CONFIRMED** (display 1,610; unrounded twin 1,611 — see note) |
| B2 | `com.fandom.playercompanion` (D&D Beyond) | 4.6★, about 131,000 reviews (unrounded about 130,628), 5M+ installs | 4.6★; `131,000 (130,628)`; 5M+ (`>= 5,000,000`) | **CONFIRMED** |
| B3 | `com.dmtools` (D&D 5e Encounter Keep) | 2.5★, 23 exact reviews, 10K+ installs, ads yes, IAP yes | 2.5★; `23 (23)`; 10K+ (`>= 10,000`); `ads = true`; `iap = true` | **CONFIRMED** |
| B4 | `worldbuilding generator` shelf | 8/8 incumbents declare IAP, 0 declare ads | `ads — declared: 0, declared absent: 8`; `in-app purchases — declared: 8, declared absent: 0` | **CONFIRMED** |
| B5 | `FaNG – Fantasy Name Generator` | 4.8★, about 7,300 exact reviews, 500K+ installs, ads yes, IAP yes | 4.8★; `7,300 (7,304)`; 500K+ (`>= 500,000`); `ads = true`; `iap = true` | **CONFIRMED** |
| B6 | Verbatim complaint strings present | `"when I refresh it crashes"`; and `"all the tables and entries are there and they are awesome"` followed by a request for a random result | Both occur verbatim in the `ttrpg encounter generator` critical reviews (quoted below) | **CONFIRMED** |
| B7 | Relevance / hand job-share | `ttrpg encounter generator` ≈ **0.97 (29/30 on-job)**; `dnd dm screen app` ≈ **0.70 (21/30)** | `ttrpg encounter generator`: **28/30 = 0.93** (twice); `dnd dm screen app`: **20/30 = 0.67** (twice); guard `partial (15/30)` in both | **WRONG** (both halves) |
| B8 | No paid one-time incumbent on the enriched cards | "no paid one-time incumbent appears on the enriched cards"; worksheet §5.1 "Lane B … Measured paid incumbent: **none**" | A priced one-time app **does** appear on the enriched `ttrpg encounter generator` cards: `uk.co.maddwarf.randomencountergeneratorpro` — `Random Encounter Generator PRO`, **paid ($1.99)**, unrated, 5+ installs | **WRONG** |

**Tally: 6 CONFIRMED · 2 WRONG · 0 NOT-CHECKED.**

---

## 1. What is WRONG, and what it means

### B7 — the hand-classified job share is lower than claimed (precision error, conclusion unchanged)

- Claimed: `ttrpg encounter generator` **29/30 = 0.97** on-the-job; `dnd dm screen app` **21/30 = 0.70**
  (recipe §5 point 3; evidence §3).
- Observed, twice each, in this session:
  - `hand job-share: 28/30 (0.93) vs guard partial` for `ttrpg encounter generator` (28 on-job,
    **0 off-job**, 2 unmatched: `com.LodestarTeam.rolegateapp` and the `Free Fire x NARUTO SHIPPUDEN`
    banner).
  - `hand job-share: 20/30 (0.67) vs guard partial` for `dnd dm screen app` (20 on-job, **0 off-job**,
    10 unmatched including `com.fandom.playercompanion`, `D&D 5e Monster Compendium`, the two
    screen-dimmer apps).
- Both live runs reproduce 0.93 / 0.67 exactly, so this is not one noisy read. The recipe's own
  §3 list named **9** unmatched DM cards; the live set has **10** (`D&D 5e Monster Compendium` moved
  into the unmatched bucket), and one RPG card (`Role Gate`) moved out of the on-job count.
- **What it means:** the *conclusion* — that the guard's `partial` is the homonym mechanism and the
  visible sets genuinely answer the query — survives: 28/30 and 20/30 with **zero off-job cards** is
  still overwhelming. But the specific fractions **0.97 and 0.70 are not reproducible** and should be
  corrected to **0.93 (28/30)** and **0.67 (20/30)**. The recipe (and any proposal that cites these as
  measured) is one card optimistic on each shelf. Under `.cursor/rules/grounded-claims.mdc` a
  `MEASURED` row states a number the command actually printed, so the numbers in the recipe are
  mislabelled as MEASURED. This does **not** move the money arithmetic.

### B8 — the "no paid incumbent" claim is false on the recipe's own shelf

- Claimed (money owner's worksheet §3.2, §3.4, §3.5, §5.1): **"No paid one-time incumbent was measured
  on either query."** … §3.4: "no paid incumbent exists in this shape on either measured query" …
  §5.1 table: Lane B "Measured **paid** incumbent: **none**".
- Observed: on the recipe's enriched `ttrpg encounter generator` result set (the shelf the recipe
  actually proposes to enter) the #4 card is
  `uk.co.maddwarf.randomencountergeneratorpro` — **Random Encounter Generator PRO**, **paid ($1.99)**,
  unrated, 5+ installs. It is a paid one-time incumbent, and it sits on exactly the enriched cards the
  recipe decision rests on.
- **Two important qualifications, so the finding is not overstated:**
  1. The money owner's two measured queries were `knitting pattern generator` and `ttrpg random tables`
     (worksheet §3.2), **not** the recipe's queries. The worksheet's own statement may be true for the
     queries it ran; the error is that the conclusion "Lane B … no paid incumbent" (§5.1/§5.2) was
     **carried into the lane-level verdict without re-measuring on the recipe's shelf** — and the
     recipe's shelf contradicts it.
  2. The recipe file itself is **not** wrong here. Its §5 point 2 names the same app honestly:
     *"its `PRO` at **$1.99** and 5+"* and classifies the whole unrated tail as *"evidence of failure,
     not vacancy"*. The contradiction is in the money owner's lane summary, not in the recipe.
- **What it means for the arithmetic.** The repeat-sales / themed-pack model — the recipe's chosen
  archetype `purchase_led_repeat_sales` — is supported by **inferred IAP presence only**, not by any
  observed paid incumbent and not by any visible pack SKU:
  - On the `worldbuilding generator` shelf, **8/8 declare IAP and 0 declare ads** (CONFIRMED), but all
    eight are **`free`** in price. An `iap=true` badge proves a paywall exists; it does **not** say the
    unit is a pack, nor its price, nor the conversion (the worksheet says this itself at §3.5).
  - The **one** priced app that is visible on the recipe's shelf is an **unrated failure** (5+ installs),
    which is evidence *against* a working paid flow, not for one.
  - Therefore the **$2.99-per-pack anchor has no measured willingness-to-pay comparable on this shelf**.
    The recipe's §6 "for contrast" reference — *"the shelf this enters contains a 100K+-install app
    whose `PRO` edition is priced at $1.99"* — is a $1.99 price carried by an app the recipe itself
    calls part of the failure tail, so it cannot calibrate the $2.99 anchor either. The recipe already
    labels the repeat unit **INFERRED** (§13, economics `assumptionsUnverified`), which is the honest
    position; B8's job is to make that inference visible rather than read as a measured fact.
- **Verdict on the money model:** repeat-sales / pack is **not** supported by an observed paid
  incumbent on this lane. It rests on IAP-badge inference. A slot on this plan should treat "will a
  GM buy a second genre pack?" as the first thing the post-ship reading tests, exactly as the recipe's
  §13 says.

### B1 — the word "exact" (recorded, not a verdict flip)

The claim says "1,610 **exact** reviews". Play's display is **1,610**; the unrounded `schema.org` twin
is **1,611**. So the displayed number is 1,610 and the truly exact count is 1,611 — one apart. The
recipe's evidence table prints both (`1,610 (1,611)`), so nothing is hidden; the summary's use of
"exact" for the rounded display is the only imprecision. Rated **CONFIRMED**.

---

## 2. Complaint strings (B6) — verbatim as printed this session

From `node scripts/scan-complaints.mjs --query "ttrpg encounter generator" --top 6 --concurrency 2`:

- `com.axehigh.rpg.encounters` — 1★ *"Poorly set up app, very poorly set up!! I only see a few scene
  ideas. **And when I refresh it crashes and I get nothing new.** Not happy at all!!"*
  → the substring `"when I refresh it crashes"` occurs **verbatim**.
- `com.axehigh.rpg.encounters` — 3★ *"This can be an incredibly useful app because **all the tables
  and entries are there and they are awesome. please add a way to get an random result from** "*
  → the named line occurs **verbatim**, and it is **immediately followed** by the request to add a way
  to get a random result. Matches the claim, including the "followed by" structure.
- The `crash` + `broken` classes total **4 of 12** critical in this read (3 crash + 2 broken counted
  across the 18-review pocket; `scan-complaints` reported `3 crash (25 %)`, `2 ads (17 %)`,
  `2 broken (17 %)`, `2 paywall (17 %)`, `1 missing-feature (8 %)`, `1 performance (8 %)` of 12 critical).

The recipe's aggregate "crash + broken = 8 of 24 critical across both shelves" was **not** re-derived
in this session's single-query complaint read (only the `ttrpg encounter generator` shelf was scanned
for complaints here); that aggregate is therefore **NOT-CHECKED** in this lane-verification (not one of
the eight numbered claims, which asked only for the two verbatim strings). The two verbatim strings
themselves are confirmed.

---

## 3. Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| B1 Game Master 5th Edition = 4.1★ / 1,610 display, 1,611 unrounded / 500K+ / ads / IAP | MEASURED | `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "dnd dm screen app"` → `com.lionsden.gamemaster5 4.1 1,610 (1,611) 500K+ true true free` |
| B2 D&D Beyond = 4.6★ / 131,000 display, 130,628 unrounded / 5M+ | MEASURED | same command → `com.fandom.playercompanion 4.6 131,000 (130,628) 5M+ false true free` |
| B3 D&D 5e Encounter Keep = 2.5★ / 23 (23) / 10K+ / ads / IAP | MEASURED | `node scripts/enrich-market.mjs ... --query "ttrpg encounter generator"` → `com.dmtools 2.5 23 (23) 10K+ true true free` |
| B4 worldbuilding shelf = 8/8 IAP, 0 ads | MEASURED | `enrich-market.mjs ... --query "worldbuilding generator"` → summary `ads declared: 0`, `in-app purchases declared: 8` |
| B5 FaNG = 4.8★ / 7,300 display, 7,304 unrounded / 500K+ / ads / IAP | MEASURED | `enrich-market.mjs ... --query "fantasy name generator"` → `com.crystalpeak.fantasynamegenerator 4.8 7,300 (7,304) 500K+ true true free` |
| B6 `"when I refresh it crashes"` present verbatim | MEASURED | `node scripts/scan-complaints.mjs --query "ttrpg encounter generator" --top 6 --concurrency 2` → 1★ on `com.axehigh.rpg.encounters` |
| B6 `"all the tables and entries are there and they are awesome"` + request to add random result | MEASURED | same command → 3★ on `com.axehigh.rpg.encounters` |
| B7a hand job-share `ttrpg encounter generator` = 0.93 (28/30), not 0.97 | MEASURED | `node scripts/scan-titles.mjs --concurrency 3 --query "ttrpg encounter generator" --on-job … --off-job …` → `hand job-share: 28/30 (0.93) vs guard partial` (ran twice, identical) |
| B7b hand job-share `dnd dm screen app` = 0.67 (20/30), not 0.70 | MEASURED | `node scripts/scan-titles.mjs --concurrency 3 --query "dnd dm screen app" --on-job … --off-job …` → `hand job-share: 20/30 (0.67) vs guard partial` (ran twice, identical) |
| B8 a paid one-time incumbent IS on the enriched `ttrpg encounter generator` cards | MEASURED | `node scripts/enrich-market.mjs --top 8 ... --query "ttrpg encounter generator"` → row 4 `uk.co.maddwarf.randomencountergeneratorpro  Random Encounter Generator PRO  unrated  unknown  5+  paid ($1.99)` |
| B8 no visible pack SKU / no visible paid unit among the worldbuilding incumbents | MEASURED | same enrichment → all 8 `worldbuilding generator` rows `price = free`, `iap = true`; the enrichment reports price per app and prints no IAP price range |
| Money-owner claim "Lane B … Measured paid incumbent: none" | ON RECORD (contradicted live) | `docs/ideation/2026-10-02-lane-pricing-worksheet.md:405` — falsified for the recipe's shelf by the B8 MEASURED row above |
| Recipe archetype `purchase_led_repeat_sales` support = inferred IAP presence, not an observed paid pack | INFERRED | From the two MEASURED rows above: IAP badge declares a paywall but carries no unit, price or conversion; the only priced app is unrated at 5+ installs; the recipe's own §13 and `economics.json.assumptionsUnverified[1]` label the repeat unit INFERRED |
| `scan-market` verdict for `ttrpg encounter generator` = mixed / partial / 21-of-30 rated / 9 weak / median 4.30 | MEASURED | `node scripts/scan-market.mjs --concurrency 4 --query "ttrpg encounter generator" --query "habit tracker" --query "bjj training log"` → matches the recipe §2 table |
| Control `habit tracker` = fortress and `bjj training log` = dead-zone | MEASURED | same command → `habit tracker fortress`; `bjj training log dead-zone` (sensor calibration reproduces the recorded controls) |
| `com.lionsden.gamemaster5` critical reviews carry no generator-weakness theme | MEASURED | `node scripts/scan-complaints.mjs --package com.lionsden.gamemaster5 --package com.dmtools --package com.jackischstefan.dndultima` → 3 critical, top `broken(1)`; not used as evidence for any clause |
| Aggregate "crash+broken = 8 of 24 across both shelves" | NOT-CHECKED | this session scanned complaints for only the `ttrpg encounter generator` shelf; the DM shelf's 12 critical were not re-read here |
| Install-to-purchase conversion; any revenue figure | UNVERIFIED | not measurable with these tools; deciding command `node scripts/check-outcomes.mjs --slot rook-encounter-generator` against a Play Console reading, which does not exist yet |

---

## 4. One-line summary

Six of eight load-bearing numbers reproduce live; the two that do not are the hand job-shares (0.93
and 0.67, not 0.97 and 0.70 — conclusion unchanged) and the "no paid incumbent" claim (one paid app at
$1.99 sits on the recipe's own enriched cards, unrated at 5+ installs). The money model's
repeat-sales/pack premise therefore rests on **inferred IAP presence**, not on any observed paid
incumbent or visible pack SKU — which the recipe already admits and the money owner's lane summary
obscures.

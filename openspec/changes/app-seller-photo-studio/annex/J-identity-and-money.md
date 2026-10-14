# J — Identity, name and money

## Name (decided; derived, not invented)

**Plinth: Product Photos** — slug `plinth-product-photos`, `applicationId` `com.gexetr.plinthproductphotos`.
A plinth is the block an object stands on in a gallery or shop window: the app's promise is the item is the
exhibit and everything under and around it is styled. The carrier is one word, 6 characters.

Derivation field (full scored record is the naming JSON; the Play scan is MEASURED 2026-10-12, evidence
§Naming):

| Candidate | Archetype | Result |
|---|---|---|
| Plinth | metaphorical (carrier) | chosen — no Play title containing "Plinth" among 30 visible cards |
| Vitrine | metaphorical | rejected — `Vitrine Shop` and `Vitrine Furniture` are on the shelf, and a shop-window word reads as a furniture store |
| Stagely | coined | rejected — exact title `Stagely - Local Theatre Diary` exists; `Stager AI Home Virtual Staging` owns the stem |
| Shelfie | compound | rejected — five apps titled `Shelfie` |
| Lotshot | compound | rejected — no exact title but reads as auction-lot photography, narrower than the job |
| Product Photo Editor | descriptive | **negative control** — the head term the incumbents own, unsearchable and unbrandable |

Store titles (≤ 30 with ≥ 5 slack; machine translation never names the app, these were written by hand and
are **UNVERIFIED by a native speaker**): en `Plinth: Product Photos` (22), ru `Plinth: фото товаров` (20),
de `Plinth: Produktfotos` (20), fr `Plinth: photo produit` (21), es `Plinth: foto de producto` (24),
pt-BR `Plinth: foto de produto` (23). The owner confirms the name at checkpoint 1.

## Visual identity direction (declared before styling; the palette is written to `store/studio.json` and
`Color.kt` at task 3.5, and the palette check judges it)

- **Subject:** a gallery plinth lit from above — the item as exhibit.
- **Field:** deep graphite (`≈ #15181A`), warm travertine surfaces (`≈ #E8E1D5`) for light mode inverted
  accordingly; colours are *direction*, final hex values are chosen at 3.5 and must pass the palette gates.
- **One accent reserved for a meaning:** verdigris (`≈ #3FA796`) appears only where the app guarantees
  something about the item (original pixels, passing rules). A second warm clay (`≈ #D9784A`) appears only
  where credits are charged. No gradient-purple, no dynamic colour.
- **Mark (drawn, not borrowed):** a plinth silhouette — a wide low block under a lifted rounded square
  (the item) with a thin arc of light; reads at 48 dp; monochrome layer is the silhouette alone; stays
  inside the 66 dp safe zone.
- **Slot-owned components (minimum):** `PlinthStage` hero panel (item on a plinth with a light arc, real
  stats), **`ProofRing`** (a ring around every item tile: solid verdigris = original pixels, split =
  item untouched + generated background, dashed = AI-enhanced — the provenance model made visible), a
  glyph-only section strip, `StatusPill`, `ItemTile`, empty states drawn with the mark, a `CostLine`
  component shown before every paid action.
- **Type:** one humanist sans for UI and the slide kit's bundled open-licence families (annex I); numerals
  tabular where counts and prices are shown.
- **Translation:** the strip's inactive pills are glyphs; the carrier "Plinth" is not translated; headlines
  do not depend on English string lengths.
- **Icons:** one licence-cleared set vendored with `icons/provenance.json`; the mark and the `ProofRing`
  are slot-owned.

`ProofRing` has four semantic states: solid = `ORIGINAL_PIXELS`, double solid =
`MANUALLY_ADJUSTED`, split = generated background with untouched item, dashed = `ENHANCED`. Visible
text/semantics accompany the shape; colour or style alone never carries provenance.

Free entitlement watermarks exported pixels and limits a batch to 5 items. The unlock removes both
restrictions; provider-funded operations continue to require credits.

## Money

### Archetype and what is rejected (revenue terms)

Chosen: `purchase_led_repeat_sales` (consumable credit packs) plus one non-consumable unlock. The reason is
a cost fact: every cut-out, generated scene and Enhanced job costs us real provider money per use
(measured endpoint ≈ $0.01–0.02 per call, provider ceilings in annex E), so revenue must scale with use.

| Rejected | Revenue reason |
|---|---|
| `purchase_led_single_unlock` only | a one-time price against recurring per-use provider cost is an unbounded loss on heavy sellers; the unlock exists here only for the free-limit and watermark |
| `ads_led_utility` | INFERRED: the audience is sellers doing commerce work, ad-hostile, and the output is commercial imagery where an ad beside it damages trust; the ad income per active seller is far below $0.08/credit |
| `purchase_only_no_ads` | the same shape as the single unlock; it cannot fund per-use cost |
| subscription | banned by the owner |

### Price ladder (hypothesis for checkpoint 1; S5 re-reads the anchors and the money owner confirms)

Principle (owner): the market's price minus a couple of percent. Anchors are ON RECORD (competitor-authored
or vendor pages, evidence §Money): Photoroom Pro $90/yr (≈ $7.50/mo billed yearly), Pixelcut Pro $96/yr,
Pebblely $19/mo for 500 images, Claid $9/mo, ProductShot $5/mo.

| Product | Price | Credits | $ / credit | Net after 0.80 factor |
|---|---:|---:|---:|---:|
| Starter pack | $7.29 | 90 | $0.0810 | $5.83 |
| Studio pack | $18.49 | 230 | $0.0804 | $14.79 |
| Pro pack | $47.99 | 600 | $0.0800 | $38.39 |
| Unlock (non-consumable) | $28.99 | — | — | $23.19 |
| Starter credits | free | 10 per device | — | cost absorbed ≤ 10 × $0.025 = $0.25 |

Unit arithmetic (divisions shown): provider ceiling $0.025 per credit-unit → the 90-credit pack costs at
most 90 × $0.025 = $2.25, net $5.83 − $2.25 = **$3.58 margin (61 %)**; price/cost = $0.0810 / $0.025 =
**3.24 ≥ 3.09** (`docs/monetization-strategy.md` §3.7, UNVERIFIED until S5 re-reads §2.2/§3.7). A standard
card is 2 credits → gross $0.162, provider ≤ $0.05. The free-absorbed operations (recognition, O-01) are
budgeted separately in `STARTER_DAILY_BUDGET_USD` and `DAILY_SPEND_CAP_USD` (annex D).

Comparison framing used in copy: "a seller who processes 90 items a month pays ≈ $7.29 once, not a monthly
bill" — worded as a fact about our price, never as a claim about competitors' pricing.

### Kill criteria (an off-switch, not a target; owner: target income is not a concern)

`windowDays 90`, `minInstalls 1000`, `minRevenueUsd 200`. Arithmetic: $200 ÷ $5.83 net per starter pack =
34.3 → about 35 starter-pack sales in 90 days; 35 ÷ 1000 installs = 3.5 % pack conversion (UNVERIFIED as
reachable; the experiment is the answer). `node scripts/check-outcomes.mjs --slot <slug>` reads
`store/outcomes.json` against these.

### Costs that are not per-user

VPS (already owned: 1 vCPU, 954 MiB), domain (owner's), provider spend capped by the global daily limit,
and an operator switch (`/config`). The server's own running cost is a fixed monthly line the owner knows;
it is stated, not hidden, in the proposal.

### Honest unknowns

Whether sellers buy a mobile app for this; the exact provider cost per Enhanced operation (S2); whether
Google's net is exactly 0.80 of the price (S5). The pack prices are therefore *hypotheses*, and the owner
decides at checkpoint 1.

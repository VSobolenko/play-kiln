# The AI-app ecosystem: a money reading before the scouts land — 2026-10-16

**Author:** the money owner (`.cursor/rules/ideation-gate.mdc`, `harness/roles/money-owner.md`,
`docs/monetization-owner.md`). **Scope:** price four shelves against the model costs this factory
actually pays, and rank the candidate products by expected revenue. **Not in scope:** demand
research (the scouts own that), code, any slot.

**How the cost base was read.** The operations, their credit prices and their `costMicroUsd` are
declared in the Plinth server's model registry (`apps/2026-10-15-plinth-product-photos/server/src/registry.ts`).
The unit prices behind those figures were measured against fal's authenticated pricing API on
2026-10-15 and recorded in `apps/2026-10-15-plinth-product-photos/docs/research/M1-provider-prices-measured.md`.
The deduction arithmetic is `docs/monetization-strategy.md` §2.2 and §3.7. The demand readings are
live Play scans run today by this role (`scripts/scan-titles.mjs`, `scripts/enrich-market.mjs`).

Every price in this document is a **hypothesis for the owner's checkpoint**, not a validated market
price. Every number that is not a provider cost or a live Play reading is labelled.

---

## 1. The measured cost base — what one unit of generation actually costs

`MEASURED` — `registry.ts` (read 2026-10-16) against `M1-provider-prices-measured.md` (measured
2026-10-15, HTTP 200 from `api.fal.ai/v1/models/pricing`).

| Op | Endpoint | Job | Credits charged | `costMicroUsd` | Per-call cost at the declared reference | Enabled? |
|---|---|---|---:|---:|---|---|
| O-01 | `fal-ai/any-llm/vision` | recognition | **0** | 10 000 | $0.010 / request | yes (absorbed) |
| O-02 | `fal-ai/bria/background/remove` | cut-out | 1 | 18 000 | $0.018 / generation | yes |
| O-07 | `fal-ai/flux/dev` | text-to-image scene | 2 | 50 000 | $0.050 @ 2 MP (1 MP = $0.026) | yes |
| O-08 | `fal-ai/iclight-v2` | relight | 5 | 104 858 | $0.104858 @ 1024² | yes |
| O-09 | `fal-ai/bria/expand` | outpaint | 2 | 40 000 | $0.040 / generation | yes |
| O-10 | `fal-ai/flux-pro/kontext` | instruction edit | 2 | 40 000 | $0.040 / image | yes |
| O-11 | `fal-ai/clarity-upscaler` | upscale | 1 | 125 829 | $0.031 @ 1 MP · $0.126 @ 2048² | **no — disabled** |
| O-12 | `fal-ai/bria/eraser` | erase | 2 | 40 000 | $0.040 / generation | yes |
| O-13 | *(none)* | — | 5 | **null** | **no price** | **no — disabled** |
| O-14 | `fal-ai/idm-vton` | virtual try-on | 5 | 75 000 | $0.075 @ 60 compute-seconds | yes |
| O-15 | *(none)* | — | 1 | **null** | **no price** | **no — disabled** |

Three facts about this table drive everything below.

- **The backend is a photo backend.** Every enabled operation takes an image in or makes one.
  There is **no text-generation endpoint** (O-13 and O-15 declare `endpointId: null` and
  `costMicroUsd: null`) and **no video endpoint at all**. A "listing copy" or "video" product has no
  priced operation to call today.
- **A credit is a near-uniform unit with a $0.025 cost ceiling.** The most expensive enabled
  operation costs $0.025 per credit (`flux/dev`: 2 credits for $0.050). The cheapest is
  `idm-vton` at $0.015 per credit. This is what makes one credit price possible across a catalogue.
- **`clarity-upscaler` cannot be sold at its declared 1 credit.** $0.031 at 1 MP is 24 % above the
  $0.025 ceiling, and $0.126 at 2048² is 5× it. At the $0.08 credit price this document uses below,
  the price/cost ratios are **2.58×** (1 MP) and **0.63×** (2K) — both under the 3.09 floor, and the
  second loses money on every call. It is disabled, and it is disabled *correctly*.

### 1.1 The credit price the factory already uses

`MEASURED` — `apps/2026-10-15-plinth-product-photos/economics.json`:

| Pack | Credits | Price | Price per credit | Cost ceiling per credit | Ratio |
|---|---:|---:|---:|---:|---:|
| Starter | 90 | $7.29 | $0.081 | $0.025 | 3.24× |
| Studio | 230 | $18.49 | $0.0803913 | $0.025 | 3.2157× |
| Pro | 600 | $47.99 | $0.0799833 | $0.025 | 3.1993× |
| Unlock (non-consumable) | — | $28.99 | — | $0 | ∞ |

**This document prices every generator product on the same $0.08-per-credit basis**, because the
credit is the unit the backend already meters and because $0.08 is a price the factory has already
put in front of the owner.

---

## 2. The arithmetic every row below uses

`ON RECORD` — `docs/monetization-strategy.md` §2.2 (the divisor formula) and §3.7 (the
server-confirmed case). The inputs are the strategy document's **assumptions**, not Play settlement:
`F_fraud = 0.01` (server-confirmed), `F_billing = 0.15`, `F_refund = 0.02`, `F_fx = 0.02`,
`M_target = 0.60`.

```text
net factor N = (1 − 0.01)(1 − 0.15)(1 − 0.02)(1 − 0.02) = 0.8081766

required multiple K at a 60 % target margin
             = 1 / (0.8081766 × (1 − 0.60)) = 3.0933833

minimum honest price per credit at the $0.025 cost ceiling
             = $0.025 × 3.0933833 = $0.0773346
```

So, for any product in this document:

```text
net proceeds per sale  = gross price × 0.8082
model cost per sale    = units sold in the pack × provider cost per unit
net per sale           = net proceeds − model cost
sales / month for $1,000  = 1000 ÷ net per sale
```

**Google Play's cut, shown explicitly.** Of every $1.00 the user pays, the factory keeps
**$0.8082** before provider cost: $0.15 is Play's service fee, then 2 % refunds, 2 % FX and 1 %
residual fraud. *(The 15 % fee is the strategy document's assumption for the first-$1M tier /
new-install case — §2.2 says plainly it is a decision to re-check, and §2.2's own note records that
the EEA/UK/US standard rate for existing installs is 20–30 % plus a 5 % billing fee.)*

**Why `F_fraud = 0.01` is the right case here, and it is MEASURED not assumed for this factory.**
The server confirms every purchase against the Google Play Developer API before crediting a wallet:
`purchases.ts:99` reads the authoritative purchase, `:136-137` re-reads and consumes it, `:164-176`
retries finalisation against the same authority, and `:196`/`:235` reconcile RTDN notifications and
voided purchases with a claw-back. Credit is granted server-side, after confirmation. A
client-reported token is not trusted anywhere.

### 2.1 The fee sensitivity — the one input that can sink the whole catalogue

`MEASURED (arithmetic over the strategy document's assumptions)`:

| `F_billing` | Net factor | K at 60 % margin | Min. price/credit |
|---:|---:|---:|---:|
| 0.15 (first-$1M / new install) | 0.8081766 | 3.0934 | $0.0773346 |
| 0.25 | 0.7130970 | 3.5058 | $0.0876459 |
| 0.30 | 0.6655572 | 3.7562 | $0.0939055 |

At a 25 % or 30 % effective fee, **every pack in this document and every pack Plinth has proposed
sits below the floor.** This is not a proposal to raise prices; it is the reason the fee tier must be
read for the real account before a price is committed, and it is `UNVERIFIED` until it is read.

---

## 3. The four shelves, as measured today

Every row below is a live Play reading taken by this role on **2026-10-16** with
`scripts/enrich-market.mjs` (search card → listing detail; installs, `containsAds`, `inAppPurchases`).

| Shelf | Query | Top cards (installs / rating / reviews) | Monetization seen | Guard verdict |
|---|---|---|---|---|
| **1. Seller content** | `ai product photo` | Photoroom 100M+ 4.7 (4 020 000) · Lensa 10M+ 4.1 · Canva 500M+ · Picsart 1B+ 3.9 | 6/6 IAP, 2/6 ads | mixed |
| **2. Video** | `ai video generator` | PixVerse 50M+ 4.5 (4 540 000) · Kling 10M+ 4.5 · HubX AI Video 10M+ 4.6 · Vidu 1M+ 4.8 | 6/6 IAP, 1/6 ads | mixed |
| **3. SMB print** | `menu maker` | Menu Maker 1M+ 4.9 (42 300) · Desygner 500K+ 4.6 · QR Menu 5K+ 4.8; rest unrated | 5/6 IAP, 2/6 ads | mixed |
| **3. SMB print** | `price list maker` | three unrated apps at 1K+ · Micra **2.3★** · AppPlanex 1K+ | 6/6 IAP | **candidate** |
| **3. SMB print** | `flyer maker` | Flyer Maker 10M+ 4.8 (385 000) · Canva 500M+ · Poster Maker 10M+ 4.7 · 5/6 ads | 6/6 IAP, 5/6 ads | **fortress** |
| **4. Personal photos / home** | `interior design ai` | Home AI (HubX) 10M+ 4.3 (994 000) · AI Home Design 10M+ 4.6 (312 000) · Planner 5D 50M+ · AI Remodel 5M+ | **6/6 IAP, 1/6 ads** | mixed |
| **4. Personal photos / home** | `ai photo enhancer` | Remini 500M+ 4.1 (5 890 000) · UpFoto 100M+ 4.5 · Lumii 100M+ 4.7 | **6/6 ads, 6/6 IAP** | mixed |
| **4. Personal photos / home** | `photo restorer` | Renew 1M+ 4.5 · PhotoBoost 10M+ 4.6 · Relumi 100K+ 4.0 · jpgHD 100K+ **2.5** | 5/6 IAP, 2/6 ads | **candidate** |
| **4. Personal photos / home** | `greeting card maker` | Invitation Maker 10M+ 4.9 (192 000) · Canva 500M+ | 6/6 IAP, 1/6 ads | mixed |

Three readings matter more than the rest.

- **The interior/home sub-shelf is bigger than it looks and monetizes by purchase, not ads.** Six
  enriched leaders, all six with in-app purchases, only one with ads, and four with 5M+ installs.
  That is the strongest "someone pays here" signal in the whole scan.
- **The photo-enhancer sub-shelf is the opposite**: 6/6 declare ads. Remini (500M+) and UpFoto
  (100M+) give the job away free and monetize attention. Entering there means paying model cost to
  compete with free.
- **`price list maker` is the one measurably weak pocket on shelf 3**: the incumbents are unrated
  1K+ apps and a 2.3★ developer, against a `menu maker` neighbour that is 1M+ at 4.9. That gap is
  where a slot goes. `flyer maker` is a fortress and is not it.
- **`photo restorer` reads `candidate`** — and it is a trap, because the operation a restoration job
  needs is the disabled `clarity-upscaler`. See §7 W2.

---

## 4. Ranked candidate products

Ordered by expected revenue, best first. The target is stated once: **$1,000 net per month**. The
"sales/mo" column is `1000 ÷ net per sale` and is the whole point of the ranking — it is how many
paying transactions a month the product needs before the shelf pays for itself.

`ASSUMPTION` throughout: Play fee 15 %, refunds 2 %, FX 2 %, fraud 1 % (net factor 0.8082); the
credit price $0.08; the pack ladders for new products are this role's proposals, not market prices.

### C1 — Seller listing studio (`Plinth: Product Photos`) · **Shelf 1** · *already approved, in build*

- **Job:** turn a seller's own product photos into per-marketplace listing cards with the item's
  pixels preserved, the marketplace's rules checked, and one saved style across a whole batch.
- **Paying user:** a marketplace seller (Wildberries, Ozon, Avito, Vinted, eBay, Etsy, Depop, Amazon).
- **Bought:** credit packs **and** a non-consumable unlock. `MEASURED` — `economics.json`.
- **Ops used:** O-01 recognition ($0.010, absorbed), O-02 cut-out ($0.018, 1 cr), O-07 scene
  ($0.050, 2 cr); optionally O-09 outpaint / O-10 edit / O-12 erase ($0.040, 2 cr each).
- **Per-use model cost:** ≤ **$0.025 / credit** (the ceiling); the observed mix is nearer $0.020.

| Sale | Gross | Play + deductions (−19.2 %) | Net proceeds | Model cost | **Net per sale** | **Sales/mo for $1,000** |
|---|---:|---:|---:|---:|---:|---:|
| Starter 90 cr | $7.29 | −$1.40 | $5.89 | $2.25 | **$3.64** | **275** |
| Studio 230 cr | $18.49 | −$3.55 | $14.94 | $5.75 | **$9.19** | **109** |
| Pro 600 cr | $47.99 | −$9.21 | $38.79 | $15.00 | **$23.78** | **42** |
| Unlock (no cost) | $28.99 | −$5.56 | $23.43 | $0.00 | **$23.43** | **43** |

**Why it ranks first.** It is the only candidate on any shelf with a *documented* wedge — Nightjar's
realism-vs-fidelity distinction and Wildberries' own error list naming "photo unsuccessfully
generated by a neural network" (`ON RECORD`,
`docs/research/2026-10-12-seller-platform-evidence.md` §Market evidence) — and the only
shelf where a measured competitor prices the same job monthly (Photoroom Pro $90/yr, Pixelcut Pro
$96/yr, Wildberries' Jam tier 22 990 ₽/30 days; `ON RECORD`, `docs/research/2026-10-12-seller-platform-evidence.md`).
A buyer who already pays a subscription for the job is a buyer a consumable pack can convert.

### C2 — Interior & home restyle · **Shelf 4**

- **Job:** photograph your own room once and see it restyled — different aesthetic, different
  layout, your furniture kept — then export the version you like.
- **Paying user:** a consumer renovating, redecorating or staging a home to sell.
- **Bought:** consumable render packs; a non-consumable HD-export / no-watermark unlock.
- **Ops used:** O-10 instruction edit ($0.040, 2 cr), O-08 relight ($0.104858, 5 cr), O-09 outpaint
  ($0.040, 2 cr). A render with a relight pass costs ≈ **$0.145** and is charged 7 credits.
- **Per-use cost:** ≈ **$0.021 / credit**.

| Sale | Gross | Net proceeds | Model cost | **Net per sale** | **Sales/mo for $1,000** |
|---|---:|---:|---:|---:|---:|
| 30 renders | $2.99 | $2.42 | $0.63 | **$1.79** | **559** |
| 120 renders | $9.99 | $8.07 | $2.52 | **$5.55** | **180** |
| 350 renders | $29.99 | $24.24 | $7.35 | **$16.89** | **59** |
| Unlock (no cost) | $14.99 | $12.11 | $0.00 | **$12.11** | **83** |

Note the shape: because each render is cheap, the revenue has to come from **iteration volume**, which
is exactly what this job produces naturally. It ranks second only because its competition is free.

### C3 — Menu & price-card maker for small business · **Shelf 3**

- **Job:** a café, salon or shop takes one photo of its dishes/stock and gets a print-ready menu, a
  price list, a table card and a story post — all in the same look, in its own language.
- **Paying user:** an owner-operator of a small hospitality or retail business.
- **Bought:** packs of "brand sets" (each set = one generated background typeset into 4–5 print and
  social formats on the device); a non-consumable print-export + brand-kit unlock.
- **Ops used:** O-07 background ($0.050, one per set), text typeset on the device at zero provider
  cost (the technique Plinth already uses). **Cost per set ≈ $0.060** — the cheapest output in this
  document, because one generation feeds five deliverables.

| Sale | Gross | Net proceeds | Model cost | **Net per sale** | **Sales/mo for $1,000** |
|---|---:|---:|---:|---:|---:|
| 5 sets | $14.99 | $12.11 | $0.30 | **$11.81** | **85** |
| 15 sets | $34.99 | $28.28 | $0.90 | **$27.38** | **37** |
| 40 sets | $79.99 | $64.65 | $2.40 | **$62.25** | **16** |
| Unlock (no cost) | $19.99 | $16.16 | $0.00 | **$16.16** | **62** |

**The best margin in the catalogue**, because the frontier model work happens once and the layout work
happens free on the phone. It ranks third only on shelf size, not on unit economics.

### C4 — Keepsake & photo-gift maker · **Shelf 4**

- **Job:** turn the photos people already have into a birthday card, a framed print, a
  milestone poster or a memorial keepsake.
- **Paying user:** a consumer buying a personalised gift or card.
- **Bought:** consumable packs of artworks; a non-consumable no-watermark / HD-print unlock.
- **Ops used:** O-07 ($0.050, 2 cr) per artwork; O-09 outpaint ($0.040) for aspect changes;
  text typeset on device.
- **Per-use cost:** **$0.05 / artwork**.

| Sale | Gross | Net proceeds | Model cost | **Net per sale** | **Sales/mo for $1,000** |
|---|---:|---:|---:|---:|---:|
| 12 artworks | $9.99 | $8.07 | $0.60 | **$7.47** | **134** |
| 40 artworks | $24.99 | $20.20 | $2.00 | **$18.20** | **55** |

### C5 — Seller rich-content slide series · **Shelf 1**

- **Job:** from the same product photos, generate the multi-slide "rich content" carousel a
  marketplace shows beside the main card — benefit plates, spec callouts, comparison slides.
- **Paying user:** the same marketplace seller as C1.
- **Bought:** packs of slides; a non-consumable series unlock.
- **Ops used:** O-02 cut-out ($0.018, 1 cr) + O-07 background ($0.050, 2 cr) per slide =
  **$0.068 / slide**.
- **Per-use cost:** **$0.068 / slide**.

| Sale | Gross | Net proceeds | Model cost | **Net per sale** | **Sales/mo for $1,000** |
|---|---:|---:|---:|---:|---:|
| 10 slides | $6.99 | $5.65 | $0.68 | **$4.97** | **201** |
| 40 slides | $24.99 | $20.20 | $2.72 | **$17.48** | **57** |

⚠ **Policy risk, stated here rather than discovered at review.** C1 and C5 serve the same buyer and
produce the same visual artefact (a generated background with typeset text on it). Play's Repetitive
Content policy forbids *"multiple apps with highly similar functionality, content, and user
experience"* within one catalogue, and enforcement reaches account termination
(`ON RECORD`, `docs/monetization-strategy.md` §1.2). Shipping both is defensible only if the jobs are
provably distinct in the store listing and the UI. **This role's recommendation is to build one of
C1/C5, not both.**

### C6 — SMB social-post sets · **Shelf 3**

- **Job:** a week of on-brand social posts for a small business, from its own photos.
- **Ops used:** O-07 ($0.050) per post. **Per-use cost $0.05.**

| Sale | Gross | Net proceeds | Model cost | **Net per sale** | **Sales/mo for $1,000** |
|---|---:|---:|---:|---:|---:|
| 30 posts | $14.99 | $12.11 | $1.50 | **$10.62** | **94** |
| 100 posts | $39.99 | $32.32 | $5.00 | **$27.32** | **37** |

Ranks low on **wedge**, not on arithmetic: `flyer maker` came back `fortress` (Canva 500M+, Flyer
Maker 10M+ at 4.8 with 385 000 reviews), and Canva ships this job free.

### C7 — Product slideshow video · **Shelf 2** *(the only priceable shelf-2 product)*

- **Job:** turn a seller's product photos into a 15-second vertical video with motion, transitions,
  captions and music.
- **Ops used:** **none.** The video is assembled on the device from stills. Provider cost ≈ **$0**.
- **Bought:** a one-time unlock ($6.99, net **$5.65**, 177 sales/mo) or ads.

This is the honest state of shelf 2: the *only* thing the current backend can serve there is a video
that is not generated by a model. It is priceable precisely because it costs nothing, which also
means it is not an "AI generation" product in the owner's sense — and it competes in one of Play's
most crowded, lowest-value categories.

### C8 — C2C single-item listing photo · **Shelf 1**

- **Job:** one good photo for one item the user is selling on Vinted/Depop/eBay.
- **Ops used:** O-02 cut-out ($0.018, 1 cr). Pack of 20 cut-outs $2.99 → net **$2.06** → **486
  sales/mo.**
- The lowest net per sale of any candidate: a consumer who will not pay a professional price for a
  professional job. Included to be explicit that **the buyer quality, not the operation, is what
  makes C1 work.**

---

## 5. The three or four I would build first, in revenue terms

| Order | Product | Why in revenue terms |
|---:|---|---|
| **1** | **C1 — Plinth: Product Photos** (shelf 1) | Already approved and in build, so its first dollar is the nearest of any row. Best net per sale on a pack that carries real volume ($23.78 on the Pro pack, 42 sales/mo for $1,000), the only evidenced wedge, and the only shelf where a competitor's *monthly* price is a proven anchor a consumable pack can undercut. |
| **2** | **C3 — Menu & price-card maker** (shelf 3) | The widest margin in the catalogue ($0.060 cost per deliverable, $27.38 net on the 15-set pack — 37 sales/mo for $1,000) and it reuses the *entire* Plinth pipeline: the same server, the same credit ledger, the same confirmed-purchase authority, the same on-device text typesetting. It also lands on the one measurably weak pocket on its shelf (`price list maker`, `candidate`). Lowest technical risk of any new slot here. |
| **3** | **C2 — Interior & home restyle** (shelf 4) | The largest measured demand of the four shelves, and the only large shelf where 6/6 leaders monetize by purchase and 5/6 carry no ads. 59 sales/mo for $1,000 at the $29.99 pack. It ranks second overall and third in build order because its wedge is the weakest of the three — the incumbent's version of this job is free. |
| **4** | **C4 or C5** — one of the two, never both | If a fourth is taken now: **C4** (keepsake/photo-gift) for audience spread, or **C5** (seller rich-content) for margin (`$17.48` net, 57 sales/mo for $1,000) and an identical buyer to C1. Pick one: shipping C1 and C5 together is the internal-duplication risk named in §4. |

**Which shelf has the best money: shelf 1 — AI-generated product and listing content for marketplace
sellers.** It has the highest net per sale, the only evidenced wedge, a buyer who provably pays a
recurring price for the same job today, and a repeat trigger built into the job (every new SKU is a
new sale). Its weakness is audience size, and that is the price of the quality.

**Which shelf is the weakest: shelf 2 — video and short-form.** Three reasons, all of them
arithmetic or measured, not taste:

1. **No operation exists.** The registry has no video endpoint and no priced text endpoint. Nothing
   can be quoted, so nothing can be sold.
2. **Video is the most expensive model class.** `ON RECORD` — the closest priced analogue in the
   registry (a relight at $0.105 for 5 credits) is already the tightest margin the factory runs
   (3.82×). Generative video per second is materially dearer, and the factory holds **no measured
   price** for it. `UNVERIFIED` until a reading is taken.
3. **The incumbents are funded labs**, not apps: PixVerse 50M+ (4.5, 4.54 M reviews), Kling 10M+
   (Kuaishou), Hailuo (MiniMax), Vidu (ShengShu). Against that, the only thing this backend could
   serve today is a slideshow (C7).

---

## 6. The archetype per product, and what was rejected

`subscription` is **not weighed** — it is banned by the owner (2026-10-01), is absent from
`ARCHETYPES`, and fails `parseEconomics` with no exemption path. Nothing below proposes a renewal
lifecycle.

**The conclusion for every generative product (C1–C6, C8): `purchase_led_repeat_sales`.**

| Archetype | Verdict | The revenue fact that decided it |
|---|---|---|
| `purchase_led_repeat_sales` | **chosen** | Every generation incurs a provider cost at the moment it is served (O-02 $0.018 … O-08 $0.105). A consumable pack is the only shape where the revenue for a unit is collected *before* the cost of serving it. |
| `purchase_led_single_unlock` | **rejected** | One price against a recurring per-use cost is an unbounded loss on a heavy user. Quantified on C1: a seller who spends 600 credits costs $15.00 in provider fees; a $28.99 unlock nets $23.43 once and funds that user forever. Plinth's own plan records the same reasoning (`economics.json` → `archetypesRejected[0]`). |
| `ads_led_utility` | **rejected — and the arithmetic kills it outright** | `ON RECORD` (§2.1): a rewarded-video-centric app earns ≈ **$0.05 per DAU per day**, i.e. ~$1,000/mo at 700–1,000 DAU. Now price the free tier: 1,000 DAU × **one** generation/day at the *cheapest* operation ($0.018) = **$540/month** in provider cost — over half the ad revenue for a single call per user per day. At a typical $0.040–0.050 the cost is **$1,200–1,500/month**, which exceeds the ad revenue. **An ads-led app that gives away paid model operations loses money at every scale.** Ads remain legitimate only for a product whose free tier costs $0 (C7). |
| `purchase_only_no_ads` | **rejected** | A price with no discovery (`docs/monetization-strategy.md` §4), and it cannot fund a per-use cost any better than the single unlock can. |

**The one exception: C7 (`ads_led_utility` or `purchase_led_single_unlock`).** Its provider cost is
zero, so it is the only candidate here where the ads arithmetic in §2.1 is not immediately negative.

---

## 7. Where the paywall goes, per product

The shape is the same on C1–C6 and C8, because the backend is the same:

| | What the user gets | Why it is enough to feel the value |
|---|---|---|
| **Free, before any payment** | Everything that costs the store nothing: the marketplace rules linter, templates, crop/rotate/exposure, on-device text typesetting, history, the shoot coach — **plus a bounded starter credit grant** (Plinth: 10 credits per device, `MEASURED` `economics.json` → `priceLadder.starterCreditsPerDevice`). | Ten credits is 1–3 finished outputs on the real model. The user sees their own product in a real generated card before being asked for money — and the grant is capped server-side (`starterDailyBudgetMicroUsd` default **$10/day**, `dailySpendCapMicroUsd` default **$25/day**; `MEASURED` `config.ts:73-74`), so a launch spike cannot turn the free tier into an unbounded bill. |
| **The paywall fires** | At the **first server-cost operation after the starter credits are spent**, with the credit cost shown *before* the action, and again at the first batch larger than the free limit, where the **unlock** is offered next to the packs. | The user is asked at the moment they have already decided they want the output, not at install. |
| **The non-consumable unlock** | Removes the watermark and the free batch limit. It buys nothing that costs the factory money, so it is pure margin ($28.99 → $23.43 on C1) and it is deliberately *not* the thing that funds calls. | Keeps the two products honestly distinct: credits fund provider cost, the unlock funds convenience. |
| **Where the model cost is incurred** | **After payment, server-side, on confirmation.** `purchases.ts:99` confirms the Play token before `applyPurchasedCredits` grants a wallet balance; the provider call happens on a later, authenticated spend. | The only cost incurred before payment is the bounded starter grant. There is no path in which a non-paying user costs real money beyond that cap. |

Differences by product: **C3** moves the paywall one level up — the free tier should include one
complete brand set (5 print/social deliverables from a single generation) so the owner sees a finished
menu before paying; **C2** and **C4** put the free starter grant in front of a *result the user
shares*, because a consumer's willingness to pay is highest right after a render they like.

---

## 8. Kill criteria per product

`INFERRED` — proposed by this role from each shelf's measured demand and the plan schema in
`docs/slot-economics.md` (`windowDays` 7–180, `minInstalls ≥ 1`, `minRevenueUsd ≥ 1`). The owner
decides them at checkpoint 1; the money owner does not record one.

| Product | `windowDays` | `minInstalls` | `minRevenueUsd` | What missing them means |
|---|---:|---:|---:|---|
| C1 Plinth (recorded, `MEASURED`) | 90 | 1 000 | $200 | the seller audience does not adopt a mobile app for this job |
| C5 Seller rich-content | 90 | 1 000 | $200 | the same buyer does not buy a second job from us |
| C3 SMB menu / price-card | 90 | 1 500 | $250 | SMB owners do not install a phone app for print work |
| C2 Interior / home restyle | 120 | 5 000 | $300 | a free ad-led incumbent holds the job |
| C4 Keepsake / photo-gift | 90 | 3 000 | $200 | gift buyers do not convert on a consumable pack |
| C7 Product slideshow video | 90 | 3 000 | $150 | an unattributed slideshow is not worth installing |

Read them together with §2.1's arithmetic: at $23.78 net per C1 Pro-pack sale, `minRevenueUsd: $200`
is roughly **8–9 pack sales in 90 days**. That is what the factory is currently asking of a slot — and
it is roughly **1/15th** of the $1,000/month this document uses as its target. Both numbers belong in
front of the owner: the kill threshold says when to stop, the target says what "working" looks like.

---

## 9. Warning list — the candidates the arithmetic kills

**W1 — Generative video for creators (shelf 2).** No enabled or priced operation exists in the
registry (`registry.ts:74-84` — O-13 and O-15 both `endpointId: null`, `costMicroUsd: null`), and no
video endpoint is declared anywhere. There is **no way to compute a cost**, so there is no way to set
a price. `UNVERIFIED`. A scout proposing this must bring a measured per-generation price before a
money owner can price it — and the surrounding evidence is that video is the dearest model class in
the catalogue (the closest analogue, a relight, is already 3.82× and the tightest margin the factory
runs). **Do not scaffold this until a price is read.**

**W2 — Old-photo restore & colourise (shelf 4).** This is the trap the scan set. `photo restorer`
comes back `candidate`, the shelf is huge (Remini 500M+, UpFoto 100M+), and the job is real — but the
operation it needs is `clarity-upscaler`, which is **disabled**, and its declared 1-credit price is
arithmetically impossible:

```text
O-11 at 1 credit, sold at $0.08/credit
  1 MP:  $0.080 / $0.031 = 2.58×   ← below the 3.09 floor
  2K:    $0.080 / $0.126 = 0.63×   ← loses $0.046 on every single call
```

At a 60 % target margin the honest credit price for this operation is **≥ $0.096/credit at 1 MP and
≥ $0.390/credit at 2K** — 1.2× and 4.9× the price the whole catalogue uses. Either the operation is
re-priced to 2 credits (1 MP) / 5 credits (2K), or a restoration product cannot ship. **A restoration
app priced like the rest of the catalogue loses money on every restore.**

**W3 — AI try-on / virtual model (shelf 1).** The arithmetic is *fine* — O-14 is $0.075 for 5
credits ($0.40 at $0.08/credit) = **5.33×**, the second-best ratio in the registry — but the plan
keeps it disabled pending safeguards (`M0-S6-play-policy.md`), and Wildberries already sells 20
on-photo try-ons inside a 22 990 ₽/30-day tier. It is a policy-blocked candidate, not a
money-blocked one.

**W4 — Any ads-led product whose free tier calls a paid model.** Quantified in §6: at 1 000 DAU the
free tier costs $540–1 500/month against ~$1 000/month of ad revenue. **The product is
loss-making by construction**, and no amount of mediation or rewarded inventory closes a gap that
one generation per user per day opens.

**W5 — Every pack in this document, at a 25–30 % effective Play fee.** §2.1: the floor rises to
$0.0876459–$0.0939055 per credit, above every price the factory has proposed ($0.0799833–$0.081).
**`UNVERIFIED` until the real fee tier is read for this account.** Do not commit a ladder before it is.

**W6 — Shipping C1 and C5 together.** Named in §4: same buyer, same artefact, one catalogue. The
Repetitive Content penalty is account-wide (`ON RECORD`, `docs/monetization-strategy.md` §1.2).

---

## 10. What this reading does not prove

- **No conversion rate is measured anywhere in this document.** The "sales/mo for $1,000" column is
  a division of the target by net-per-sale — it is not a forecast, and no row's viability is
  established. `docs/monetization-strategy.md` §4 records the only conversion figure the factory has
  (~1–3 % for one-time IAP) and labels it `UNVERIFIED`.
- **The pack ladders for C2, C3, C4, C5, C6 are this role's proposals.** Only C1's ladder is on
  record, and its own plan labels it a hypothesis.
- **The demand readings are one signed-out Play session on one storefront**, with a ~30-card display
  cap. Relevance order is not size order, so larger apps can sit below the cut.
- **The deduction rates are assumptions**, and §2.1 shows the fee alone can move the floor by 21 %.
- **The ranking is an ordering over measured shelf facts plus computed net-per-sale**, not a revenue
  forecast. It says which shelf deserves the next slot; it cannot say a slot will earn.

---

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| O-02 costs $0.018, O-07 $0.050, O-08 $0.104858, O-09 $0.040, O-10 $0.040, O-11 $0.125829, O-12 $0.040, O-14 $0.075, O-01 $0.010 per call | MEASURED | `registry.ts:40,44,48,53,58,62,66,71,79` (`costMicroUsd`); cross-read against `M1-provider-prices-measured.md` (fal pricing API, 2026-10-15) |
| Credit charges are O-01:0, O-02:1, O-07:2, O-08:5, O-09:2, O-10:2, O-11:1, O-12:2, O-14:5 | MEASURED | `registry.ts:40,44,48,53,58,62,66,71,79` (`credits`) |
| O-11 (`clarity-upscaler`) is disabled and O-13/O-15 have no endpoint and no price | MEASURED | `registry.ts:68` (`enabled: false`), `:75-76`, `:83-84` (`endpointId: null`, `costMicroUsd: null`) |
| The registry declares no video and no text-generation endpoint | MEASURED | full read of `registry.ts:30-86`; the only `endpointId` values are the nine above |
| Plinth's packs are $7.29/90, $18.49/230, $47.99/600 and a $28.99 unlock | MEASURED | `apps/2026-10-15-plinth-product-photos/economics.json` → `priceLadder` |
| Starter grant is 10 credits/device; the starter budget is $10/day and the global cap $25/day | MEASURED | `config.ts:75` (`STARTER_CREDITS`, 10), `:74` (`STARTER_DAILY_BUDGET_USD`, 10), `:73` (`DAILY_SPEND_CAP_USD`, 25) |
| Purchases are confirmed against the Google Play Developer API before crediting | MEASURED | `purchases.ts:99` (`store.getPurchase`), `:136-137` (authoritative re-read + `consume`), `:164-176` (finalisation retry), `:196`,`:235` (RTDN + voided reconciliation) |
| Net factor 0.8081766 and K = 3.0933833 at a 60 % margin | ON RECORD (arithmetic over assumptions) | `docs/monetization-strategy.md` §2.2, §3.7; re-derived and confirmed in `M0-S5-prices-and-presets.md` § "Net factor re-derived" |
| At F_billing 0.25 the floor is $0.0876459/credit; at 0.30 the net factor is 0.6655572 and the floor $0.0939055 | MEASURED (arithmetic) | strategy §2.2 formula; 0.25 case cross-read from `M0-S5-prices-and-presets.md`; 0.30 case computed this session |
| A rewarded-centric ads app earns ≈$0.05/DAU/day and needs ~700–1,000 DAU for $1,000/month | ON RECORD | `docs/monetization-strategy.md` §2.1 (published benchmarks, labelled illustrative there) |
| Interior/home leaders: Home AI (HubX) 10M+ 4.3 (994,285), AI Home Design 10M+ 4.6 (312,387), Planner 5D 50M+ 4.4 (585,977), AI Remodel 5M+ 4.6 (184,305); 6/6 IAP, 1/6 ads | MEASURED | `node scripts/enrich-market.mjs --query "interior design ai" --top 6`, 2026-10-16 |
| Photo-enhancer leaders all declare ads: Remini 500M+ 4.1 (5,892,039), UpFoto 100M+ 4.5, Lumii 100M+ 4.7; 6/6 ads, 6/6 IAP | MEASURED | `node scripts/enrich-market.mjs --query "ai photo enhancer" --top 6`, 2026-10-16 |
| `price list maker` is a candidate pocket (unrated 1K+ apps, Micra Solution 2.3★ at 50K+) while `menu maker` reaches 1M+ at 4.9 | MEASURED | `node scripts/enrich-market.mjs --query "price list maker"`, `--query "menu maker"`, 2026-10-16 |
| `flyer maker` is a fortress (Canva 500M+, Flyer Maker 10M+ 4.8 with 385,312 reviews, 5/6 ads) | MEASURED | `node scripts/enrich-market.mjs --query "flyer maker" --top 6`, 2026-10-16 |
| Video leaders: PixVerse 50M+ 4.5 (4,542,817), Kling 10M+ 4.5, HubX AI Video 10M+ 4.6, Vidu 1M+ 4.8 | MEASURED | `node scripts/enrich-market.mjs --query "ai video generator" --top 6`, 2026-10-16 |
| Photoroom 100M+ 4.7 (4,024,068 reviews) anchors the shelf-1 shelf, with 6/6 IAP | MEASURED | `node scripts/enrich-market.mjs --query "ai product photo" --top 6`, 2026-10-16 |
| Photoroom Pro $90/yr, Pixelcut Pro $96/yr, Wildberries Jam 22 990 ₽/30 days | ON RECORD | `docs/research/2026-10-12-seller-platform-evidence.md` §Market evidence (competitor-authored and vendor pages, 2026-10-12) |
| Play's Repetitive Content policy forbids multiple apps with highly similar functionality, content and UX, with account-wide escalation | ON RECORD | `docs/monetization-strategy.md` §1.2, quoting the Spam policy |
| Net per sale and sales/mo figures in §4 | MEASURED (arithmetic over the rows above) | computed this session as `gross × 0.8081766 − units × cost`, shown in each table |
| The pack ladders for C2–C6 and the kill criteria in §8 are this role's proposals | INFERRED | no price reading exists for those products; the owner decides both at checkpoint 1 |
| Conversion rates on any of these shelves | UNVERIFIED | no Play Console reading exists for any candidate; the decider is a post-ship `store/outcomes.json` reading |
| A generative-video per-generation price at fal or any provider | UNVERIFIED | not queried this session; the decider is one authenticated `GET /v1/models/pricing` against the chosen video endpoint, as `M1-provider-prices-measured.md` did for the image endpoints |
| The effective Play fee tier for this account | UNVERIFIED | strategy §2.2 records 0.15 as a chosen default, valid only under the first-$1M tier; the decider is the Play Console fee page for this account |
| Whether sellers / SMBs / consumers convert on a consumable pack instead of a subscription | UNVERIFIED | the only mechanism the factory has is the slot's own kill criteria and console readings after ship |

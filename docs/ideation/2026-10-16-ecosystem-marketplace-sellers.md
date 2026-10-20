# The marketplace-seller shelf: one recipe — `Warp: Print Mockups` — 2026-10-16

**Author:** recon (`.cursor/rules/ideation-gate.mdc`, `harness/roles/scout.md`).
**Shelf measured:** AI-generated product & listing content for online marketplace sellers — the
one-person shop on Etsy, eBay, Vinted, Depop, Amazon Handmade or Facebook Marketplace.
**Pricing inputs:** `docs/research/2026-10-16-money-ecosystem-ranking.md` (§1, §2, §4, §6, §7, §8)
and `docs/research/2026-10-16-generation-capability-inventory.md` (Part 1, Part 4) — both read
this session and both are the source of every provider cost quoted here.
**Priced plan:** `docs/ideation/plans/warp-print-mockups.economics.json`.
**Reading of the market:** live Play scans run this session. Every claim below carries a label.

---

## 0. The one-sentence answer

**`Warp: Print Mockups`** is a studio that warps the print-on-demand seller's own flat design file
onto a product's real print area — fabric creases, shading and a generated scene behind it — and
exports watermark-free marketplace images on a one-time unlock, fixing the two things this shelf's
own reviews say its leaders get wrong: a graphic that is shrunk to fit a template rectangle, and a
"free trial" that is not free.

This is **one recipe**, not two. A second recipe for this shelf was generated and refused, and its
refusal has a revenue reason (§4).

### Is this a different job from `plinth-product-photos`? Yes — and here is the measurement

`plinth-product-photos` = *"turn a seller's own product photos into per-marketplace listing
cards"* (`registry/apps.json`, identity field only). Read against
`docs/research/2026-10-16-generation-capability-inventory.md` §2c, that slot's job is:

> "Turn a seller's own **product photos** into a marketplace-ready photo pack … import, **cut out**,
> compose on a background, auto-card, lint the marketplace's image rules, type listing text, and export."

| | `plinth-product-photos` | `warp-print-mockups` |
|---|---|---|
| **Input (`before`)** | The seller's own photograph of a physical item they already own | The seller's **flat design file** (a PNG/JPG/WebP exported from Procreate, Illustrator or Canva) |
| **Core act (`during`)** | **Cut the item out** of its photo and compose it on a generated background | **Warp the design onto a garment's true print area** with fabric creases, then relight it into a generated scene |
| **Output (`after`)** | A marketplace-ready card of *that photographed item* | A listing image of a *product that does not exist yet* — the seller's design rendered on a shirt, mug, tote or framed print |
| **Trigger** | A new SKU arrives in the seller's stock | A new **design** is finished, before any inventory exists |

The artifact is different (a mockup of an unbuilt product vs a cleaned photo of a built one), the
input is different (a vector/flat file vs a photograph), and the trigger is different (a new design
vs a new item). **One sentence cannot describe both.**

**The part that must be stated plainly, because it is where a factory drifts into Repetitive
Content:** the factory already ships, inside `plinth-product-photos`, a `marketplace-rules-linter`
(`core`, `after`, `fit`), a `listing-text-six-languages` promise, a `slide-kit-series` (`slid kit`
screen, `after`, `speed`), an `export-pack-per-marketplace` (`after`, `data_out`), a
`batch-with-saved-look`, a `library-search`, a `batch-done-reminder` and a `look-backup-and-resume`
— all `shipped`
(`docs/research/2026-10-16-generation-capability-inventory.md` §2a). **Those jobs are already taken.**
Three candidate recipes on this shelf were refused for exactly that reason (§4), and this proposal
deliberately does **not** re-sell a linter, a listing-copy writer, a slide series or a reminder.

---

## 1. Category, job, paying user, and what is bought

**Category.** Print-on-demand and merch mockup production — listing imagery for a seller who sells
*designed* products rather than found ones.

**The job, as one sentence.** From one flat design file, produce the marketplace listing images of
that design on real products, at the true print area, with fabric and lighting that make the design
look printed rather than pasted, and get the set off the phone into the listing.

**The paying user.** A print-on-demand / merch / digital-download seller on Etsy, eBay, Depop,
Vinted, Amazon Merch or a Shopify store: one person, a folder of finished designs, and a listing
image to produce for each one, on a phone or a tablet, without Photoshop and without a monthly web
SaaS.

**What is bought, and the price** *(a hypothesis for checkpoint 1, not a market reading)*:

| Product | Price | What it is |
|---|---:|---|
| Starter credits | $4.99 | 60 credits |
| Studio credits | **$14.99** | 180 credits — this is the `priceUsd` the plan records |
| Pro credits | $39.99 | 480 credits |
| Non-consumable **unlock** | $19.99 | Removes the watermark and the free batch limit |

**The moment of payment.** At the first server-cost operation after the free starter grant is spent
— a generated scene (2 credits) or a relight (5 credits), with the credit cost shown *before* the
tap — and again at the first batch larger than the free limit, where the unlock is offered beside
the packs. The unlock is offered next to the packs, never as a gate in front of the first mockup.

---

## 2. The archetype, and every archetype that was rejected with its revenue reason

**Chosen: `purchase_led_repeat_sales`.** Capability required: `billing`.

| Archetype | Verdict | The revenue fact that decided it |
|---|---|---|
| `purchase_led_repeat_sales` | **chosen** | Every generated scene and every relight incurs a provider fee **at the moment it is served** — O-07 `$0.050`, O-08 `$0.104858` (money plan §1). Consumable packs are the only shape where the revenue for a unit is collected *before* the cost of serving it. The job also repeats by nature: every new design is a new mockup set. |
| `purchase_led_single_unlock` | rejected | One price against a recurring per-use cost is an unbounded loss on a heavy user. Quantified: a seller who spends 480 credits costs **$12.00** in provider fees at the $0.025 ceiling, and a $19.99 unlock nets **$16.16 once** and then funds that user forever. |
| `ads_led_utility` | **rejected — the arithmetic kills it** | `ON RECORD` (money plan §2.1): an ad-led utility earns ≈ **$0.05 per DAU per day**. O-07 is `$0.050` per call. So **one free generated mockup per user per day consumes 100 % of that user's ad revenue**, and a relight tier (O-07 + O-08 = `$0.1549`) consumes **310 %**. A free tier that calls a paid model is loss-making by construction at every scale. |
| `purchase_only_no_ads` | rejected | A price with no discovery (`docs/monetization-strategy.md` §4), and it cannot fund a per-use provider cost any better than the single unlock can. |
| `subscription` | **forbidden** | Banned by the owner (2026-10-01), absent from `ARCHETYPES` in `scripts/lib/slot-economics.mjs`, and machine-refused by `parseEconomics` with no exemption path. Not weighed. |

---

## 3. Demand evidence, labelled — and the job share, hand-classified

The relevance guard is not evidence on its own (it is blind to homonym collisions), so each query's
visible titles were hand-classified with `scripts/scan-titles.mjs` and the job share is reported
beside the guard verdict.

### 3.1 The pod/mockup sub-shelf, measured live

| Query | Guard verdict | Seen | Rated | Weak | Median | Why |
|---|---|---:|---:|---:|---:|---|
| `mockup generator` | mixed (26/30 on-topic, share 0.87) | 30 | 14/30 | 4 | **4.35** | demand proven, 4 weak incumbents, only **0.29** of rated are strong |
| `t shirt mockup` | mixed | 30 | 20/30 | 6 | **4.30** | demand proven, 6 weak incumbents, only **0.25** of rated are strong |
| `print on demand` | **candidate** (partial, 8/13) | 13 | 9/13 | **6** | **3.70** | **6 of 9 rated incumbents are below 4.2** |

`MEASURED` — `node scripts/scan-market.mjs --concurrency 4 --query "t shirt mockup" --query "print on demand" --query "mockup generator"`, 2026-10-16. All three returned `relev=ok`/`partial`, so the verdicts are usable.

Hand-classified job share: **`mockup generator` 21/30 (0.70)** and **`print on demand` 8/13 (0.62)**
visible cards are on the job, against guard verdicts of `on-topic (26/30, 0.87)` and
`partial (8/13, 0.62)` respectively.
`MEASURED` — `node scripts/scan-titles.mjs --query "mockup generator" --query "print on demand" --on-job "\b(mockup|mock|product|print|design|shirt|shop|merch|pod)\b" --off-job "\b(wallpaper|photo editor|video|game|interior|room)\b"`, 2026-10-16.

**The guard's own error is visible in that diff and is the reason the hand pass exists:**
`mockup generator`'s 21 on-job cards are dominated by **device/screenshot** mockups (phone frames,
screenshot frames, `Mockup° Screenshot Frames`, `Appuly - Phone Mockup`, `Mockup3D : 3D/AR phone
mockup`) — a *different* job sharing the head term "mockup". The genuinely product-shaped cards are
a minority and they are weak. That is the pocket.

### 3.2 The incumbents, as evidence — who earns here and how

| App (package) | Rating | Installs | Ads | IAP | Price |
|---|---:|---:|---|---|---|
| Mockey: AI Mockup Generator (`in.blinkstore.mocky.twa`) | **3.2** | 100K+ (252 reviews) | false | **false** | free |
| Mockup Generator App- Mockitup (`com.accordion.mockup`) | 4.3 | **500K+** (3 541) | true | true | free |
| Placeit Mockups & Design (`net.placeit.tin`) | 4.5 | — | — | — | free |
| `com.nancymaj.Insitu` | **2.9** | 100K+ | — | — | free |
| Product photo editor AI mockup (`ai.product.photoeditor`) | **3.3** | 50K+ | — | — | free |
| Renderly: AI Mockup Generator (`com.raahim2.Renderly`) | unrated | 500+ | false | true | free |
| 3D Mockups: Mockup Creator (`com.mockup.design3d`) | unrated | 1K+ | false | true | free |
| Device Mockup Generator (`info.camposha.device_mockup`) | unrated | 100+ | false | true | free |
| Printify (`com.printify.printify`) | 3.9 | — | — | — | free |
| Printful (`com.printful.app`) | 4.5 | — | — | — | free |

`MEASURED` — `node scripts/enrich-market.mjs --concurrency 2 --top 6 --query "mockup generator"` (6 enriched: `containsAds` 1 declared / 5 absent; `inAppPurchases` 5 declared / 1 absent; largest install bucket 500M+ from Canva, 500K+ from Mockitup), 2026-10-16.
`MEASURED` — `node scripts/scan-titles.mjs` same invocation (the Printify/Printful/Placeit rows are its visible-title output; their installs, ads and IAP flags were **not** enriched).

**Read as evidence of demand, not as a warning list:** a 500K+ incumbent with declared ads and IAP,
and a 100K+ leader, mean someone earns on this shelf. The plainest fact in the table is that the
100K+ leader is a **Trusted Web Activity** (`…mocky.twa`) — a website in a wrapper — declaring
**neither ads nor in-app purchases**, at **3.2 stars**. That is a web product that never became a
phone product, and it is the pocket.

### 3.3 What the shelf's own users complain about — the wedge, measured

`MEASURED` — `node scripts/scan-complaints.mjs --top 6 --concurrency 1 --query "mockup generator"`,
2026-10-16: 18 reviews sampled across the 6 weakest incumbents, 11 critical, 0 with no text.

| Class | Share of critical |
|---|---:|
| **paywall** — paywall, price or subscription | **6 / 11 (55 %)** |
| missing-feature | 2 / 11 (18 %) |
| value — no value, useless or scam | 2 / 11 (18 %) |
| crash | 1 / 11 (9 %) |
| notifications — spam | 1 / 11 (9 %) |

Four verbatim complaints, each with the app it came from, and each is a promise this proposal makes:

1. **The print area is wrong on the product.** `com.accordion.mockup` (Mockitup, 500K+, 4.3) — 2★:
   *"some of the templates have smaller size when fitting a design in it especially on tshirts, the
   desired photo to be uploaded is limited to…"* → the design is shrunk to fit a template rectangle
   instead of being placed on the garment's real print area. **This is the core act of this
   proposal.**
2. **The mockup looks fake.** `com.nancymaj.Insitu` (2.9★, 100K+) — 2★: *"It has nice rooms, but
   only of you have the premium paid version … It made my artwork look cheap and tacky and
   unreali[stic]"* → art composited without fabric or lighting loses the sale. **This is why the
   relight operation is in the plan, not an extra.**
3. **You cannot get the file out.** `in.blinkstore.mocky.twa` (3.2★, 100K+) — 2★: *"a complete let
   down … cant save the image, want to go 1 step back, it goes b[ack]"* → the 100K+ leader's own
   critical sample says the image cannot be saved. **This is the `after` stage of this proposal.**
4. **The "free trial" is bait, and it is priced.** `com.accordion.mockup` — 1★: *"I'm very
   disappointed … The 3 day free trial isn't free it's 19.99!!! … You don't even give a chance
   to…"*; `com.mockup.generator` (100K+) — 2★: *"it says I need to subscribe the fact that it has
   your watermark"* → **the unlock must be a one-time purchase, and the watermark must not be the
   lever.**

**The wedge, in one sentence (a difference in functionality, not in naming or colour): a real
print-area warp with fabric creases and scene lighting, exported without a watermark on a one-time
unlock — where the shelf's leaders are measured shrinking the design into a template rectangle,
compositing it flat enough to look "cheap and tacky", holding the saved image behind a subscription,
and calling a $19.99 auto-renewing charge a "3 day free trial".**

### 3.4 The shelf's own head terms, measured — and why the recipe is not named after them

The brief's own queries were run as well, and they are the reason this proposal is priced against the
POD sub-shelf rather than against the word "listing".

| Query | Verdict | Relevance | Seen | Rated | Weak | Median | Hand job-share |
|---|---|---|---:|---:|---:|---:|---:|
| `etsy listing maker` | **fortress** | **partial** | 9 | 4/9 | 1 | 4.55 | **5/9 (0.56)** |
| `product photo editor` | mixed | ok | 30 | 30/30 | 10 | 4.50 | **19/30 (0.63)** |
| `vinted listing` | **fortress** | **POLLUTED** | 30 | 22/30 | 3 | 4.60 | — |
| `ebay listing tool` | mixed | ok | 30 | 22/30 | **9** | 4.40 | — |
| `product description writer` | mixed | **partial** | 30 | 22/30 | 7 | 4.40 | — |

`MEASURED` — `node scripts/scan-market.mjs --concurrency 2 --query "etsy listing maker" --query "product photo editor" --query "vinted listing"`, and `node scripts/scan-market.mjs --concurrency 2 --query "ebay listing tool" --query "product description writer" --query "etsy listing maker"`, 2026-10-16.
`MEASURED` — independently reproduced at `--concurrency 4` with the identical verdicts, sizes, rated counts, weak counts and medians, in a 22-query run that reported **0 errored** (`.kiln/scratch/market-sellers.txt`, 354 996 ms), 2026-10-16.
`MEASURED` — `node scripts/scan-titles.mjs --query "product photo editor" --query "etsy listing maker" --on-job "\b(product|listing|etsy|ebay|vinted|shop|catalog|catalogue|photo)\b" --off-job "\b(qr|pos|scanner|printer|signature|video|wallpaper)\b"`, 2026-10-16: the tool flagged its own guard as over-calling — *"GUARD SAID on-topic, JOB SHARE IS LOW"* for `product photo editor` (guard `on-topic` 27/30, 0.90, hand share 19/30 = 0.63), and `etsy listing maker` hand share 5/9 = 0.56 against a guard `partial` 8/9.

**What that reads as, and it is the reason this recipe is not on that shelf.** Three separate
measurements say the listing-word shelf is not enterable as a general "listing tool":

1. The `etsy listing maker` head term returns only **9** cards at **partial** relevance, and its
   visible cards are the Etsy and eBay **shopping** apps (`com.etsy.butter.sell_on_etsy`,
   `com.etsy.android`, `com.ebay.mobile`) plus two SEO tag tools — a hand job-share of **5/9 (0.56)**.
2. `vinted listing` is **POLLUTED**: the visible cards are not the job at all.
3. `product photo editor` looks healthy to the relevance guard (`on-topic` 27/30, 0.90) and is not:
   the 19 on-job cards are **generic** photo editors (Photoroom, Photoshop Express, Pixelcut,
   Bazaart, Fotor, Lensa), where the factory would be competing with a 4.7★ market leader, and only
   **one** of the top 19 is the print/mockup product this recipe is about
   (`ai.product.photoeditor`, 3.3★, 50K+).

The measured weak incumbents, the measured 55 % paywall complaints and the measured print-area
defect all sit under the **pod/mockup** terms of §3.1 instead, where the guard's own verdict and the
hand pass agree. This is also why the name in §7 is `Warp: Print Mockups` and not a "listing" name:
the head term the seller actually types for this job is `print mockups`, and §3.1's hand pass measured
it as both searchable and weakly served.

---

## 4. The candidate that was refused, with its revenue reason

Three recipes were considered on this shelf and refused. None is refused for "it already exists" —
each has a money or a policy reason.

| Refused candidate | Revenue reason it lost |
|---|---|
| **AI listing-copy writer** ("write the listing copy") — the shelf brief's own second half | **No priced operation exists.** The registry has **no text endpoint**: O-15 `endpointId: null`, `costMicroUsd: null` (`registry.ts:83-84`; inventory §1a, §4.1). A plan that cannot quote a unit cost cannot set a price, and the adapter only handles `{images:[{url}]}`. Second reason: our own `plinth-product-photos` already **ships** `listing-text-six-languages` (inventory §2a). `UNVERIFIED` how a text route would price; the decider is the chosen endpoint's page at `fal.ai/models/<endpointId>`. |
| **Marketplace rich-content slide series** (money plan §4 C5) | **Same buyer and same visual artifact as our own shipped slot.** `plinth-product-photos` already ships `slide-kit-series` (`after`) and a `SlideKitScreen`, so a slide-series app is one sentence together with it — `docs/research/2026-10-16-money-ecosystem-ranking.md` §4 W6 records the Repetitive Content risk and the money owner's recommendation to build **one of C1/C5, not both**. Policy enforcement reaches account termination (money plan §1.2 citation), so the whole catalogue is the stake. |
| **A standalone marketplace image-rule linter / listing-health auditor** | **Already a shipped core promise of our own slot.** `marketplace-rules-linter` is `core`/`after`/`fit` and `shipped` in `plinth-product-photos` (inventory §2a). A second linter is the same job in a second listing. |

Also refused, and recorded so they are not re-proposed:

| Refused | Revenue reason |
|---|---|
| C2C single-item listing photo (money plan §4 C8) | Net **$2.06** per $2.99 pack → **486 sales/month** for $1,000: a consumer who will not pay a professional price for a professional job. Its input (own photo) and artifact (listing card) are also Plinth's. |
| Virtual try-on / on-model render (O-14) | **Policy-blocked, not money-blocked** (money plan §7 W3): the plan keeps O-14 disabled pending safeguards, and the factory's adapter sends only `orderedUploads[0]` (inventory §4.4) while try-on needs a garment *and* a model. |
| Ghost mannequin (O-13) | No route and **no price**: `endpointId: null`, `costMicroUsd: null` (inventory §1a, §1b). |
| Upscale the artwork (O-11) | Disabled, and arithmetically impossible at its declared 1 credit: $0.126 at 2048² against a $0.08 credit **loses $0.046 on every call** (money plan §7 W2). |

---

## 5. Revenue arithmetic, with the division shown

### 5.1 The provider costs this plan is priced against

`MEASURED` — `docs/research/2026-10-16-money-ecosystem-ranking.md` §1 (read this session), whose own
source is the factory's `registry.ts` `costMicroUsd` values crossed against a `fal.ai` pricing-API read.

| Op | Job | Credits charged | Provider cost per call | Cost per credit |
|---|---|---:|---:|---:|
| O-01 `recognize` | read the design's box and dominant colours | 0 (absorbed) | $0.010 | — |
| O-09 `expand` | outpaint the scene to the marketplace aspect | 2 | $0.040 | $0.020 |
| O-07 `flux/dev` | generate the scene the product sits in | 2 | $0.050 | **$0.025 (the ceiling)** |
| O-08 `iclight-v2` | relight the product into the scene | 5 | $0.104858 | $0.020972 |

Every quoted division below uses the **$0.025 per-credit ceiling**, the conservative case; the
observed mix over these ops is ≈ **$0.022**.

### 5.2 What one mockup costs to serve, and what it is charged

| Mockup | Provider cost | Credits | Charged | Ratio (price ÷ cost) vs the required **3.0934** |
|---|---:|---:|---:|---|
| Quick (device composite + outpaint) | $0.040 | 2 | $0.16 | **4.00×** ✓ |
| Scene (generated background) | $0.050 | 2 | $0.16 | **3.20×** ✓ |
| Realistic (scene + relight) | $0.154858 | 7 | $0.56 | **3.616×** ✓ |

```text
credit price = $0.08        net factor = 0.8081766        required multiple K = 3.0933833
minimum honest price per credit = $0.025 × 3.0933833 = $0.0773346
```

`ON RECORD` — `docs/monetization-strategy.md` §2.2 (the divisor formula); re-derived in
`docs/research/2026-10-16-money-ecosystem-ranking.md` §2 as
`N = (1 − 0.01)(1 − 0.15)(1 − 0.02)(1 − 0.02) = 0.8081766`, `K = 1 / (0.8081766 × 0.4) = 3.0933833`.

### 5.3 The division, per pack

```text
net proceeds per sale   = gross price × 0.8081766
model cost at the ceiling = credits in the pack × $0.025
net per sale            = net proceeds − model cost
sales / month for $1,000 = 1000 ÷ net per sale
```

| Sale | Gross | Play + deductions (−19.18 %) | Net proceeds | Model cost at the ceiling | **Net per sale** | **Sales/mo for $1,000** |
|---|---:|---:|---:|---:|---:|---:|
| Starter 60 cr | $4.99 | −$0.96 | $4.03 | $1.50 | **$2.53** | **395** |
| Studio 180 cr | $14.99 | −$2.88 | $12.11 | $4.50 | **$7.61** | **131** |
| Pro 480 cr | $39.99 | −$7.67 | $32.32 | $12.00 | **$20.32** | **49** |
| Unlock (no provider cost) | $19.99 | −$3.83 | $16.16 | $0.00 | **$16.16** | **62** |

Every pack's price per credit is $0.0831–$0.0833 — **3.33×** its $0.025 cost ceiling, above the
3.0934 floor. At the observed $0.022 mix instead of the ceiling the Pro pack's net per sale rises to
**$21.76** and its requirement falls to **46 sales/month**.

`MEASURED (arithmetic over the labelled rows above)` — computed this session as
`gross × 0.8081766 − credits × $0.025`, shown in the table.

### 5.4 The honest limits of that division

- **No conversion rate is measured anywhere in this document.** The sales/month column is a
  division of the target by net-per-sale; it is not a forecast (money plan §10).
- **The pack ladder is this role's proposal, not a market price.** No competitor's Android price was
  read for this job.
- **The fee tier is `UNVERIFIED`, and it is the one input that can sink the ladder.** At a 25 % or
  30 % effective Play fee the floor rises to $0.0876459–$0.0939055 per credit, **above** every price
  proposed here (money plan §2.1). The decider is the Play Console fee page for this account.

---

## 6. The promise contract (`core` and `supporting`, every one `deferred`)

The gate-ready artifact is `docs/ideation/plans/warp-print-mockups.differentiators.json`. Seven
promises, four of them `core`; the job's shape is covered `before` → `bring-your-design`,
`during` → `true-print-area-wrap`, `after` → `watermark-free-listing-export`, and five of the seven
are **connective handoffs** (`data_in`, `data_out`, `bulk`, `find`, `durability`), so the contract
can satisfy the connective floor once the handoffs ship.

| id | weight | stage | family | The promise, and the measured reason it is worth keeping |
|---|---|---|---|---|
| `bring-your-design` | core | before | `data_in` | Bring the flat artwork the shop already sells in through the picker, the document picker or the clipboard. *Reason:* the shelf's leaders start from template libraries, and `com.mockup.generator`'s 1★ review says its free options are "so limited in what can be applied". |
| `true-print-area-wrap` | **core** | **during** | `core_operation` | The design lands on the product's **real print area** at true scale with fabric-aware warp, creases and shading. *Reason:* Mockitup's 500K+ review — *"templates have smaller size when fitting a design in it especially on tshirts"*. |
| `watermark-free-listing-export` | **core** | **after** | **`data_out`** | Export the set at marketplace sizes plus the listing text through the Android share sheet or save-to-folder, **no watermark**. *Reason:* the 100K+ leader's own review says *"cant save the image"*; 2 of 11 critical reviews are "value/no value or scam" about watermarks. |
| `batch-the-whole-drop` | **core** | during | **`bulk`** | One saved scene style applies across a whole design drop in one batch. *Reason:* the trigger is a design drop, not a single item; a per-design setup cost is what makes a 40-design shop stop using the tool. |
| `one-time-unlock-no-trial-trap` | supporting | after | `trust` | A one-time unlock plus consumable credits, cost shown before the action, **no auto-renewing trial**. *Reason:* Mockitup's 1★ *"3 day free trial isn't free it's 19.99!!!"*; paywall is **55 %** of the shelf's critical sample. |
| `find-past-mockups` | supporting | after | **`find`** | Find a past design or mockup set by name, product or marketplace from a search field. *Reason:* a shop with hundreds of listings reuses the card that worked. |
| `studio-look-not-lost` | supporting | after | **`durability`** | A saved scene style and an in-progress batch survive a process kill and a restart. *Reason:* a batch is long-running; `in.blinkstore.mocky.twa`'s 1★ sample says paid access stopped working on day 3. |

**Anti-scope-creep, stated so the build cannot pad it:** the contract deliberately carries **no**
linter, **no** listing-copy generator and **no** reminder — those are `plinth-product-photos`' shipped
promises (§0). A promise here is a mockup job or it is not here.

---

## 7. The name, derived rather than invented

Gate-ready artifact: `docs/ideation/plans/warp-print-mockups.naming.json`. Seven candidates were
scored across **six** archetypes, and the winner is a carrier + job noun.

**Chosen: `Warp: Print Mockups`** — 85/100, no dimension below 3.

| Candidate | Archetype | Score | Why |
|---|---|---:|---|
| Mockup Generator | descriptive | 69 | **Negative control** — the bare category head term the shelf's 500K+ leaders already own, and Play itself says unnecessary keywords do not help ranking. |
| Product Mockup Studio | descriptive | 73 | States the job but is a generic phrase with no carrier to distinguish it. |
| **Warp: Print Mockups** | **carrier_job_noun** | **85** | *Warp* is the real textile term and the real operation (a flat design is warped onto a woven garment), and *Print Mockups* is the job a POD seller types. |
| Printfold | compound | 82 | Compact and memorable, but leaves the head term a seller searches for out of the title. |
| Mockforge | coined | 80 | Coins well and is distinctive, but "mock" reads as ridicule in English and carries no textile meaning. |
| Drape | evocative | 75 | Evokes fabric strongly, but a one-word title with no job noun is unsearchable for what it does. |
| Ghost Draft | metaphorical | 73 | Ties the name to the ghost-mannequin shot, which this plan deliberately does **not** build (O-13 has no route). |

Rejected, with the reason each lost, is recorded in the same file (`Mockup Generator`,
`Printfold`, `Ghost Draft`).

- **Length:** 19 characters in `en-US` and 18 in `ru-RU` → **11 and 12 characters of slack** under
  Play's 30-character cap (`scripts/lib/slot-naming.mjs` `TITLE_MAX`). No declared locale is over it.
- **Collision:** `UNVERIFIED`. This scan did not search the exact title and no trademark register
  was queried. The decider is a Google Play search plus TMview/USPTO/EUIPO/WIPO class-9 search for
  *Warp* and close variants — a clean search is not legal clearance.
- **Localisation:** `INFERRED`. The Latin carrier and job noun carry into both declared locales and
  fit the cap; whether a Russian-speaking POD seller discovers the English job noun is **unmeasured**
  and needs a native review before the listing ships. It is not machine-translated, because a
  machine translation must never name the app.

---

## 8. Build estimate, permissions, capabilities, server

- **Build estimate: ESTIMATE, and explicitly not a criterion.** ~**120–170 agent hours**, the bulk
  of it the device-side print-area engine (a product/print-area template set with warp meshes, crease
  and shadow compositing), then the scene/relight orchestration over the existing server shape, the
  pack + unlock billing surface, and the listing-export pack. Per the owner's 2026-10-08 instruction,
  build cost is **never** a reason to refuse, rank down, narrow or kill; it is recorded for the
  owner's information only.
- **Permissions required: none that need a runtime prompt.** `INTERNET` (install-time). The design
  input is the document picker / Photo Picker (no runtime permission), and the export is
  `ACTION_SEND` + `ACTION_CREATE_DOCUMENT`. No `CAMERA`, no `RECORD_AUDIO`, no location, no
  `POST_NOTIFICATIONS`, no `READ_MEDIA_*`.
- **Capabilities required:** `billing` (Play Billing via the factory's `capabilities/billing` module
  and a `PurchaseAuthority`, which this factory already implements once —
  inventory §3a). Not `ads`.
- **A server is required, and its cost is already stated.** The generated scene and the relight are
  provider calls; the factory's existing generation server is server-authoritative over a credit
  ledger with Play-purchase confirmation, two-phase reserve/settle/release, a $25/day spend cap and
  release-on-failure (inventory §1c–1f). **Its cost is the provider cost in §5.1 — $0.040–$0.1549 per
  mockup** — and it is carried by the pack price, not by a subscription. Two limits stated rather
  than implied: the server is **slot-local, not a published capability** (inventory §3b), so this slot
  either reuses that deployment's API or copies the server; and a second app's I/O shape is new code
  there. The I/O shape this plan needs — one image in, one image out — is the shape the adapter
  already supports (`routes.ts:436`; inventory §4.4), which is why this job is buildable on it at all.

---

## 9. Device capabilities

Each capability is answered `used` or `not used`; a `used` line names the API and what the job loses
without it.

- camera: not used — the job begins with a digital design file the shop already sells; there is no photograph in it, and a `CAMERA` prompt would buy nothing.
- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia`; without it a design the seller saved to their gallery must be re-exported to Files before it can be brought in at all.
- media playback or capture: not used — every deliverable in this job is a still image, so nothing is played back or recorded.
- microphone: not used — nothing in this job is spoken, dictated or recorded as audio.
- location: not used — a listing image of a design does not depend on where the phone happens to be.
- motion sensors: not used — the design is placed and warped by touch and gesture, never by moving the device.
- contacts / calendar: not used — the app never needs the seller's address book or their schedule.
- notifications / reminder: not used — a mockup batch finishes inside the session it was started in, so a notification would add the `POST_NOTIFICATIONS` prompt with no job behind it.
- home-screen widget: not used — mockup work happens at a desk over a design drop, not from the home screen.
- share / export: used — `ACTION_SEND` with a `FileProvider` URI, plus `ACTION_CREATE_DOCUMENT`; without it the finished mockup set cannot reach the marketplace listing and the job cannot be finished.
- Bluetooth / USB: not used — nothing is paired, tethered or printed directly from the phone in this job.
- files / SAF: used — `ACTION_OPEN_DOCUMENT` through the Storage Access Framework for the design file, and `ACTION_CREATE_DOCUMENT` for a saved batch; without it a design exported by Procreate or Illustrator cannot be opened and a batch cannot be filed where the seller keeps their artwork.

---

## 10. Visual direction (one or two sentences)

A **dye-house** identity: a deep indigo field with the app's own woven-grid texture, a saturated
magenta accent reserved for the print area and the moment a design lands on it, and a
slot-owned **print-area chevron** mark that reads as a registration mark at 48 dp. The one thing the
slot's own components do that a stock Material screen does not: the mockup canvas shows the
**print-area boundary and the warp mesh as the seller drags**, so placement is a scene the app draws
rather than a slider row in a form.

---

## 11. Kill criteria — three finite numbers

`INFERRED` — proposed by recon from the shelf's measured demand; the owner decides them at
checkpoint 1. Schema bounds are `windowDays` 7–180, `minInstalls ≥ 1`, `minRevenueUsd ≥ 1`
(`scripts/lib/slot-economics.mjs`).

| `windowDays` | `minInstalls` | `minRevenueUsd` | What missing them means |
|---:|---:|---:|---|
| **90** | **1 000** | **$200** | The POD/merch seller does not install a phone app to make mockups of their own designs, or does not convert on a consumable pack. |

Read with §5.3: at $20.32 net per Pro-pack sale, `minRevenueUsd: $200` is roughly **10 pack sales in
90 days** — the kill threshold says when to stop; the $1,000/month target says what working looks
like.

---

## 12. Honest unknowns

1. **No conversion rate is measured on this shelf.** The only conversion figure the factory has
   (~1–3 % for one-time IAP) is recorded as `UNVERIFIED` in `docs/monetization-strategy.md` §4.
2. **The pack ladder is a proposal, not a reading.** No competitor Android price was enriched for
   this job.
3. **The effective Play fee tier is `UNVERIFIED`** and it is load-bearing: at 25–30 % the floor
   rises above every price proposed here (money plan §2.1).
4. **The provider prices in §5.1 are the factory's recorded registry values**, not a live fal read
   today. Whether they still match is `UNVERIFIED`; the decider is re-reading each
   `fal.ai/models/<endpointId>` page.
5. **The demand readings are one signed-out Play session on one storefront** with a ~30-card display
   cap, and relevance order is not size order, so larger incumbents can sit below the cut.
6. **"Warp" was not trademark-searched**, and Cloudflare ships a product named WARP — the collision
   is `UNVERIFIED` and it is a real risk to this name.
7. **The Russian-locale discoverability of an English job noun is `UNVERIFIED`** and needs a native
   read before the listing is written.
8. **A print-on-demand seller's actual willingness to pay a one-time unlock on Android** rather than
   a web subscription is `UNVERIFIED`; the decider is this slot's own `store/outcomes.json` reading.
9. **The print-area template set is the build's real risk, and it is not a market risk.** A warp
   mesh per garment/product must be authored (shirt, hoodie, mug, tote, framed print at minimum);
   whether the warp reads as *printed* on a real garment is a judgement the owner makes at
   checkpoint 1.5, not a claim here.

---

## 13. If the owner says yes

```bash
# 1. The change (the planner authors it, before any code)
#    openspec/changes/app-warp-print-mockups/  -> proposal.md, tasks.md, specs/**/spec.md
# 2. Then the slot (refuses without the plan, the promises, the name and the change)
node scripts/new-slot.mjs --slug warp-print-mockups \
  --economics docs/ideation/plans/warp-print-mockups.economics.json \
  --differentiators docs/ideation/plans/warp-print-mockups.differentiators.json \
  --naming docs/ideation/plans/warp-print-mockups.naming.json \
  --spec app-warp-print-mockups
```

`capabilities.json` must enable `billing` (the `purchase_led_repeat_sales` pairing).

---

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| `mockup generator` = mixed, 30 seen, 14 rated, 4 weak, median 4.35, 0.29 strong | MEASURED | `node scripts/scan-market.mjs --concurrency 4 --query "t shirt mockup" --query "print on demand" --query "mockup generator"`, 2026-10-16 |
| `print on demand` = candidate, 13 seen, 9 rated, 6 of 9 below 4.2, median 3.70 | MEASURED | same command |
| `t shirt mockup` = mixed, 30 seen, 20 rated, 6 weak, median 4.30, 0.25 strong | MEASURED | same command |
| `print on demand` hand job-share 8/13 (0.62) vs guard partial (8/13, 0.62) | MEASURED | `node scripts/scan-titles.mjs --query "mockup generator" --query "print on demand" --on-job … --off-job …`, 2026-10-16 |
| `mockup generator` hand job-share 21/30 (0.70) vs guard on-topic (26/30, 0.87) | MEASURED | same `scan-titles.mjs` run |
| Mockey (100K+, 3.2, 252 reviews, containsAds false, inAppPurchases false, is a `.twa`) | MEASURED | `node scripts/enrich-market.mjs --concurrency 2 --top 6 --query "mockup generator"`, 2026-10-16 |
| Mockitup (500K+, 4.3, 3 541 reviews, containsAds true, inAppPurchases true) | MEASURED | same `enrich-market.mjs` run |
| Renderly 500+ unrated; 3D Mockups 1K+ unrated; Device Mockup Generator 100+ unrated — all IAP true; largest bucket seen 500M+ | MEASURED | same `enrich-market.mjs` run (summary block) |
| Placeit 4.5, Insitu 2.9, Product photo editor AI mockup 3.3 at 50K+, Printify 3.9, Printful 4.5 — visible titles/ratings | MEASURED | `scan-titles.mjs`, same invocation |
| Paywall = 6/11 critical (55 %), missing-feature 2/11, value 2/11, crash 1/11, notifications 1/11; 18 reviews sampled, 11 critical, 0 no-text | MEASURED | `node scripts/scan-complaints.mjs --top 6 --concurrency 1 --query "mockup generator"`, 2026-10-16 |
| Mockitup 2★ review: "templates have smaller size when fitting a design in it especially on tshirts" | MEASURED | same `scan-complaints.mjs` output |
| Mockitup 1★ review: "The 3 day free trial isn't free it's 19.99!!!" | MEASURED | same `scan-complaints.mjs` output |
| Mockey 2★ review: "cant save the image"; its critical sample carries paywall×2, crash×1, missing-feature×1 | MEASURED | same `scan-complaints.mjs` output |
| Insitu 2★ review: "It made my artwork look cheap and tacky and unreali[stic]" | MEASURED | same `scan-complaints.mjs` output |
| `com.mockup.generator` 2★ review: "it says I need to subscribe the fact that it has your watermark" | MEASURED | same `scan-complaints.mjs` output |
| O-01 $0.010/0 cr · O-07 $0.050/2 cr · O-08 $0.104858/5 cr · O-09 $0.040/2 cr · O-11 disabled at $0.125829 · O-13 & O-15 `endpointId: null`, `costMicroUsd: null` · O-14 $0.075/5 cr | MEASURED | `docs/research/2026-10-16-money-ecosystem-ranking.md` §1 (its own source: `registry.ts:40-84` crossed against the 2026-10-15 fal pricing read) |
| Net factor 0.8081766; K = 3.0933833 at a 60 % margin; min honest price/credit $0.0773346; at a 25–30 % fee the floor is $0.0876459–$0.0939055 | ON RECORD | `docs/monetization-strategy.md` §2.2, re-derived in `docs/research/2026-10-16-money-ecosystem-ranking.md` §2 and §2.1 |
| A rewarded-centric ad app earns ≈$0.05/DAU/day | ON RECORD | `docs/monetization-strategy.md` §2.1, cited in the money plan §2.1 |
| `plinth-product-photos` = "turn a seller's own product photos into per-marketplace listing cards…" | MEASURED | `registry/apps.json` → `apps[8].idea` (identity field only) |
| `plinth-product-photos` already ships `marketplace-rules-linter`, `export-pack-per-marketplace`, `slide-kit-series`, `listing-text-six-languages`, `batch-with-saved-look`, `library-search`, `batch-done-reminder`, `look-backup-and-resume` | MEASURED | `docs/research/2026-10-16-generation-capability-inventory.md` §2a (15 promises, 12 shipped, 3 narrowed) |
| The factory's generation adapter sends one image and is image-in/image-out only; no text op, no mask, no multi-image | MEASURED | `docs/research/2026-10-16-generation-capability-inventory.md` §1a, §4.1, §4.3, §4.4 (`routes.ts:436`) |
| The credit ledger is server-authoritative with reserve/settle/release, Play-confirmed purchases and a $25/day cap | MEASURED | `docs/research/2026-10-16-generation-capability-inventory.md` §1c–1f |
| The generation server/registry/routes are slot-local, not a published `capabilities/` module | MEASURED | `docs/research/2026-10-16-generation-capability-inventory.md` §3b |
| `etsy listing maker` = fortress, **partial** relevance, 9 seen, 4/9 rated, 1 weak, median 4.55; `product photo editor` = mixed, 30 seen, 30/30 rated, 10 weak, median 4.50; `vinted listing` = fortress, relevance **POLLUTED**; `ebay listing tool` = mixed, ok, 30 seen, 22/30 rated, **9** weak, median 4.40; `product description writer` = mixed, **partial**, 30 seen, 22/30 rated, 7 weak, median 4.40 | MEASURED | `node scripts/scan-market.mjs --concurrency 2 --query "etsy listing maker" --query "product photo editor" --query "vinted listing"` and `… --query "ebay listing tool" --query "product description writer" --query "etsy listing maker"`, 2026-10-16; reproduced identically at `--concurrency 4` in a 22-query run reporting **0 errored** (`.kiln/scratch/market-sellers.txt`) |
| The relevance guard over-calls two of the brief's own head terms: `product photo editor` guard `on-topic` (27/30, 0.90) against a hand job-share of 19/30 (0.63), with only 1 of the 19 on-job cards a print/mockup product; `etsy listing maker` guard `partial` (8/9) against hand job-share 5/9 (0.56), its visible cards being the Etsy and eBay shopping apps | MEASURED | `node scripts/scan-titles.mjs --query "product photo editor" --query "etsy listing maker" --on-job … --off-job …`, 2026-10-16 — the tool printed `GUARD SAID on-topic, JOB SHARE IS LOW` |
| Every §3.1–§3.3 demand reading — the three query verdicts, the install/ads/IAP flags and the 55 % paywall share with its verbatim reviews — reproduced exactly on a second run in this session | MEASURED | re-ran `scan-market.mjs --concurrency 2 --query "t shirt mockup" --query "print on demand" --query "mockup generator"`, `enrich-market.mjs --concurrency 2 --top 6 --query "mockup generator"`, `scan-complaints.mjs --top 6 --concurrency 1 --query "mockup generator"`, `scan-titles.mjs --query "mockup generator" --query "print on demand"`, 2026-10-16 |
| Play's Repetitive Content policy forbids multiple apps with highly similar functionality, content and UX, escalating account-wide | ON RECORD | `docs/research/2026-10-16-money-ecosystem-ranking.md` §4 W6 / §1.2 citation of the Spam policy |
| `subscription` is absent from `ARCHETYPES` and refused by `parseEconomics` | MEASURED | `scripts/lib/slot-economics.mjs:28-33`, `:250-252` |
| Play's title cap is 30 characters and `TITLE_MAX = 30` | MEASURED | `scripts/lib/slot-naming.mjs:29` |
| `applicationIdForSlug('warp-print-mockups')` = `com.gexetr.warpprintmockups` | MEASURED | `scripts/lib/namespaces.mjs:32-34` (dashes stripped) |
| The 55 %-paywall / print-fit / can't-save / $19.99-trial wedge will convert into installs | UNVERIFIED | no install or conversion reading exists; the decider is the slot's own `store/outcomes.json` after ship |
| The effective Play fee tier for this account | UNVERIFIED | the decider is the Play Console fee page for this account |
| Live fal prices matching the registry's `costMicroUsd` on 2026-10-16 | UNVERIFIED | the decider is one authenticated `GET /v1/models/pricing` / the endpoint page at `fal.ai/models/<endpointId>` |
| Whether "Warp" collides with an existing class-9 mark or a Play app | UNVERIFIED | the decider is a Play title search plus TMview/USPTO/EUIPO/WIPO class-9 search |
| Whether a Russian-speaking POD seller discovers the English job noun in `ru-RU` | UNVERIFIED | the decider is a native-language naming review |
| A POD seller's willingness to pay a one-time unlock on Android rather than a web subscription | UNVERIFIED | the decider is a post-ship `store/outcomes.json` reading |

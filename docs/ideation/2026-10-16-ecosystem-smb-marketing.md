# The SMB print shelf: one recipe — `Placard: Menu Price Cards` — 2026-10-16

**Author:** recon (`.cursor/rules/ideation-gate.mdc`, `harness/roles/scout.md`).
**Shelf measured:** AI-generated marketing and print content for small local businesses that have no
designer — the café, food truck, market stall, salon, barber, nail studio, gym or tutor that needs a
menu, a price card, a table tent, a window sign or a per-channel social set and has no design tool.
**Pricing inputs:** `docs/research/2026-10-16-money-ecosystem-ranking.md` (§1, §2, §3, §4 C3, §6, §7,
§8) and `docs/research/2026-10-16-generation-capability-inventory.md` (Part 1, 2, 3, 4) — both read
this session and both are the source of every provider cost quoted here.
**Priced plan:** `docs/ideation/plans/placard-menu-price-cards.economics.json`.
**Reading of the market:** live `scan-market.mjs` and `scan-complaints.mjs` runs this session, plus
the archived enrichment readings of `docs/research/2026-10-16-market-scans.md`. Every claim below
carries a label.

---

## 0. The one-sentence answer

**`Placard: Menu Price Cards`** turns the price list a shop already keeps into five print and display
artefacts — the menu, the price card, the table tent, the window sign and a story-size post — from
one generated background, with every price typeset on the phone from the shop's own typed list rather
than drawn by a model, and exported as a print page through the share sheet with no watermark and no
trial that bills on day one.

This is **one recipe**, and seven other candidates for this shelf were refused — each with a revenue
reason, and none of them for "it already exists" (§14).

### Is this a different job from our own catalogue? Yes — and here is the measurement

Read against the identity fields only (`registry/apps.json`) and, for our own shipped generation
slot, against `docs/research/2026-10-16-generation-capability-inventory.md` §2c:

| | `plinth-product-photos` | `warp-print-mockups` | `roomlumen` | **`placard-menu-price-cards`** |
|---|---|---|---|---|
| **Input (`before`)** | The seller's own **photographs** of physical stock | The seller's **flat design file** for a product that does not exist | The user's own **photograph of a room** | The shop's own **price list** — a CSV, a pasted block, a photo of the old card |
| **Core act (`during`)** | Cut the item out of its photo and compose it on a generated background | Warp a design onto a garment's true print area with fabric creases | Restyle a room into a design a person shows someone | **One list becomes five print formats** with the prices typeset on the phone |
| **Output (`after`)** | A marketplace-ready **listing card** | A **mockup** of an unprinted product | A room image for the person's own use | A **printed page and display card for the counter** |
| **Buyer** | A marketplace seller | A print-on-demand / merch seller | A person redoing their own home | The owner-operator behind the till |
| **Trigger** | A new SKU arrives | A design is finished | A room is being redone | **A price changes, or the season does** |
| **Where it is used** | On a marketplace listing | On a marketplace listing | A personal project | **On the wall, on the table, in the window** |

The buyer, the input, the trigger and the place of use all differ, and the artefact is a different
object: a marketplace listing image is looked at by a stranger scrolling, a room render is looked at by
the person who made it, and a price card is looked at by a customer standing at the counter. **One
sentence cannot describe two of these**, which is the only Repetitive Content test that matters
(`.cursor/rules/ideation-gate.mdc`; `docs/monetization-strategy.md` §1.2).

**Read against the other two proposals of this same run, by name, because that is the enforceable
half.** All three came out of 2026-10-16 and a reader comparing them should not have to guess:

- `docs/ideation/2026-10-16-ecosystem-marketplace-sellers.md` proposes `plinth-product-photos` and
  `warp-print-mockups`. Both are **marketplace-adjacent**: the artefact is something a seller uploads to
  a shop, and the trigger is a new SKU or a finished design. Neither touches a shop counter, a menu or a
  price, and **no sentence describes both this recipe and either of those**.
- `docs/ideation/2026-10-16-ecosystem-consumer-personal.md` proposes `roomlumen` ("AI Room Design") and
  refuses the rest of the personal shelf. It is a person's own photo of their own room, for their own
  use, with no prices in it anywhere. **Its own "Not this shelf" line names a business's listing, menu
  or brand asset as belonging to the marketplace shelf** — that sentence is a scoping remark about
  shelves, not a claim that `plinth-product-photos` makes menus, and this proposal is the one that
  actually does the counter job. Saying so here is cheaper than leaving a later reader to find the
  apparent hole in that sentence themselves.
- **What separates them is not the pixel pipeline, it is the job.** All four recipes generate a
  background and typeset or composite onto it, and three of the four export. Who holds the phone, what
  they brought to it, what they do with the result and where it is looked at are all different.

**The part that must be stated plainly, because it is where a factory drifts into Repetitive
Content:** this proposal deliberately does **not** re-sell a marketplace rule linter, a listing-copy
writer, a batch-of-SKUs runner or a marketplace export pack — those are `plinth-product-photos`' shipped
promises (`…capability-inventory.md` §2a) and this contract carries none of them (§6).

---

## 1. Category, job, paying user, and what is bought

**Category.** Point-of-sale print and display material for a one-person local business — the menu
board, the price card, the table tent, the window sign — produced on the phone by the person who
runs the shop.

**The job, as one sentence.** From the price list the shop already keeps, produce the print and
display artefacts the counter needs — at the right size, legible, in the shop's own look, with the
prices the shop actually charges — and get them off the phone into a printable file or onto the shop's
own social channel.

**The paying user.** The owner-operator of a small local business with no designer and no design
budget: a café or food truck writing a chalkboard menu, a market stall or deli writing a price card, a
salon, barber or nail studio with a service price list, a gym or personal trainer with a rate card, a
tutor with a lesson price list, a florist or repair shop with a counter price card. One person, a
phone, and a piece of paper, card or window that a customer reads.

**What is bought, and the price** *(a hypothesis for checkpoint 1, not a market reading)*:

| Product | Price | What it is |
|---|---:|---|
| Starter | $14.99 | **5 brand sets** (one set = one generated background typeset into five print and display formats on the device) |
| Studio | **$34.99** | 15 brand sets |
| Workshop | $79.99 | 40 brand sets |
| Non-consumable **unlock** | $19.99 | Print-resolution export and unlimited brand kits |

The plan records `priceUsd: 14.99` — the Starter pack, which is the pack the first dollar arrives
through. The pack whose division carries the $1,000 target is Studio at $34.99 (§5.3).

**The moment of payment.** At the first server-cost operation after the free starter grant is spent —
the moment the owner wants a background generated for the set they have just laid out — with the set's
cost shown before the tap; and again at the first export larger than the free limit, where the
one-time unlock is offered beside the packs, never in front of the first card.

---

## 2. The typography wedge, tested rather than assumed

The brief asked for a specific test, and the answer is not the one the hypothesis wanted. It is
recorded here first because the whole design rests on it.

**The hypothesis:** image models mangle typography, so a print artefact whose prices must be exact is
a natural place to win.

**What the incumbents' own critical reviews actually say** (`MEASURED` —
`node scripts/scan-complaints.mjs --top 6 --concurrency 2 --query "menu maker" --query "price card
maker" --query "flyer maker"`, 2026-10-16; 18 reviews sampled per query, 32 critical in total across
the three pockets):

| What the reviews complain about | In the `price card maker` pocket | Verdict on the hypothesis |
|---|---:|---|
| Broken feature or bug | **5 of 14 critical (36 %)** | **Partially supports it** — but the failures are data and file failures, not misspelled words |
| Paywall, price or subscription | 4 of 14 (29 %) | unrelated to typography; the export is gated |
| Wrong or outdated information | 3 of 14 (21 %) | **supports the "prices must be exact" half** |
| Missing feature or capability | 3 of 14 (21 %) | unrelated |
| **Print resolution, DPI, bleed or print colour** | **0 of 32 critical reviews across all three pockets** | **the hypothesis is NOT supported as stated** |

Three verbatim complaints carry the whole answer:

- `com.micrasol.pricelistmaker` — **2.3★, 50K+ installs**, 1★: *"The system is experiencing an issue
  where the entered price amounts are not being retained within each product category."* → the
  incumbent **loses the price the owner typed**. This is the failure a price card cannot have, and it
  is about data, not about a model's spelling.
- `com.pricelistmaker` — 4.1★, 100K+ installs, 1★: *"The import/export functionality for Excel files
  is completely unreliable, constantly showing an 'incorrect…'"* → the **file the shop needs is
  unreliable**, which is the export half, stated as reliability rather than as resolution.
- `com.desygner.menus` — 4.6★, 500K+ installs, 24 181 ratings, 2★: *"I've had a lot of issues with the
  fonts not working correctly after I'm paying for it."* → the **typography a paying owner is promised
  is broken inside a 500K+ incumbent**. This is the closest measured thing to the typography wedge,
  and it is font *handling*, not model output.

**So the honest position, and the one the plan is built on:**

1. **The "print resolution" half of the wedge is not supported by the pocket's own reviews.** Nobody
   in 32 critical reviews mentions DPI, bleed, print colour or a file that printed badly. The nearest
   measured statements are an **export gate** ("you can't even save high quality image without
   paying") and a **broken export path** ("import/export … completely unreliable"). The promise is
   therefore written as *a print page that always exists and is never behind a paywall*, which is what
   was measured, and **not** as a resolution claim.
2. **The "correct text" half is supported, but by price data and font handling rather than by model
   typography.** The pocket's own words are "the entered price amounts are not being retained" and
   "the fonts not working correctly". Exactness is the measured requirement; the mechanism is ours to
   choose.
3. **The claim that an image model renders `7.90` as something else is `UNVERIFIED` in this session.**
   No generation was run (the server's provider key is not set for a scout), so this proposal does not
   claim it as a measured finding. It is nevertheless the reason the design never lets a model draw a
   price, and it costs nothing to honour: the factory's own backend has **no text endpoint at all** —
   every routed operation is image-in/image-out or image-in/JSON-out
   (`docs/research/2026-10-16-generation-capability-inventory.md` §1b, §4.1) — so the text was always
   going to be typeset locally. The honest statement is: **the design is justified by the price-data
   complaint, and the model-typography risk is a reason it costs nothing to hold, not a measured
   finding that anyone asked for.**
4. **A probe would settle it in one command**, and it is named in §12 so nobody has to guess.

---

## 3. Demand evidence, labelled

### 3.1 The shelf, measured this session

`MEASURED` — `node scripts/scan-market.mjs --concurrency 4 --query "menu maker" --query "price card
maker" --query "flyer maker" --query "poster maker" --query "social media post maker"`, 2026-10-16
(22 queries scanned in 21 105 ms including the tool's own 17 built-in shelves: 3 candidate, 7 mixed,
6 fortress, 6 dead-zone, 0 no-results, 0 errored).

| Query | Verdict | Relevance | Seen | Rated | Weak | Median | What it means |
|---|---|---|---:|---:|---:|---:|---|
| `menu maker` | **mixed** | ok | 13 | 6/13 | 1 | 4.45 | demand proven; the strong half is real |
| `price card maker` | **mixed** | ok | 30 | 14/30 | **5** | 4.35 | **the pocket** — 5 of 14 rated incumbents are below 4.2 |
| `flyer maker` | **fortress** | ok | 30 | 23/30 | 3 | 4.60 | 14/23 rated at 4.5+, only 3/23 below 4.2 — entrenched |
| `poster maker` | **fortress** | ok | 30 | 19/30 | 1 | 4.60 | 13/19 rated at 4.5+, only 1/19 below 4.2 — entrenched |
| `social media post maker` | mixed | ok | 30 | 26/30 | 8 | 4.40 | demand proven; the leaders are large |

**Read it as the rule requires.** `flyer maker` and `poster maker` being fortresses is **not** a reason
to refuse the shelf: it is evidence that someone earns here, and it is the measured reason this recipe
is **not** a flyer or poster maker (§14). The shelf's own pocket is `price card maker`, and the reason
is one number: its 5 weak incumbents include a **rated app with 50K+ installs at 1.7 stars** and another
at **2.3 stars with 50K+ installs**, against a neighbouring `menu maker` shelf whose leader sits at
**1M+ installs and 4.9 stars**. A weak mid-tier next to a strong leader is a reachable pocket, not a
warning sign (`.cursor/rules/ideation-gate.mdc`).

**One caution stated rather than buried:** `docs/research/2026-10-16-market-scans.md` § "Most important
expectation change" records that an earlier, weaker sample had suggested `price list maker` was the
tool's `candidate` verdict, and that this broader pass reports the shelf as **mixed**. Both readings
are retained. The pocket survives the correction because the 1.7★ and 2.3★ cards are measured on the
`price card maker` screen today, not inherited from the older sample.

### 3.2 The pocket, named

`MEASURED` — the same `scan-complaints.mjs` run; ratings, install buckets and review classes are what
it printed, and the exact review counts are `ON RECORD` from `docs/research/2026-10-16-market-scans.md`.

| App (package) | Rating | Installs | Why it is in the pocket |
|---|---:|---:|---|
| `com.zakasoft.pricelist` — Price List | **1.7★** | **50K+** | **The weakest rated card with real installs on the whole shelf.** Two of its three sampled reviews ask the same question: how to get the same entered items and prices onto a second phone |
| `com.micrasol.pricelistmaker` — Price List Maker | **2.3★** | **50K+** (148 ratings) | Top complaint is *accuracy*: the entered price amounts are not retained inside a product category |
| `com.Artificer.CardMaker` | 1.4★ | 10K+ | **Excluded from the evidence**: its title scan classifies it as an MTG *card-game* maker, so its reviews are about game cards, not about a shop's price list. Named here so its 1.4★ is not silently counted as this pocket's weakness |
| `com.oaklagcs.mypricelist` — Make Price List & Invoice | 3.6★ | **500K+** | The install leader in the pocket; 2 of 3 critical reviews are *missing-feature* — it is simple and cannot show what the owner needs |
| `com.pricelistmaker` — Price List Maker - Fast & Easy | 4.1★ | **100K+** (3 730 ratings) | Its 3 critical reviews lead with *paywall* (3 findings), and quote both a trial that deducts money on day 1 and an unreliable Excel import/export (*broken* ×2) |
| `com.skepticlabwk96.pricesnap` — PriceSnap | 4.2★ | 10K+ | At the weak boundary; its review asks for the sell price to be visible on the list |

`ON RECORD` — `containsAds` / `inAppPurchases` / listing price for these cards, from
`docs/research/2026-10-16-market-scans.md` Shelf B:
`com.micrasol.pricelistmaker` (ads false, IAP **true**, free), `com.pricelistmaker` (ads **true**, IAP
**true**, free), `com.skepticlabwk96.pricesnap` (ads **true**, IAP **true**, free),
`com.menu.maker` (ads **true**, IAP **true**, free). Every enriched card on this shelf declares
in-app purchases and none of the 112 enriched rows across the whole scan printed a price other than
free except one `$9.99` card on an unrelated Etsy shelf — so **the shelf monetizes by in-app purchase
and this recipe's price ladder is a proposal, not a reading.**

### 3.3 What the pocket's own users complain about

`MEASURED` — `node scripts/scan-complaints.mjs --top 6 --concurrency 2 --query "price card maker"`,
2026-10-16: **18 reviews sampled across the 6 weakest incumbents, 14 critical, 0 with no text.**

| Class | Share of critical |
|---|---:|
| **broken** — broken feature or bug | **5 / 14 (36 %)** |
| **paywall** — paywall, price or subscription | **4 / 14 (29 %)** |
| **accuracy** — wrong or outdated information | **3 / 14 (21 %)** |
| missing-feature | 3 / 14 (21 %) |
| confusing-ui | 2 / 14 (14 %) |
| value — no value, useless or scam | 2 / 14 (14 %) |
| account, ads, crash, regression | 1 each |

The same run on the two neighbouring shelves, for contrast: `menu maker`'s pocket is **6 critical of
18** and dominated by crash and regression (the shelf's leaders are new-broken rather than
weak), and `flyer maker`'s pocket is **12 critical of 18** with **paywall at 50 % and ads at 33 %** —
a fortress whose criticism is its business model, which is why §14 refuses a flyer maker outright.

### 3.4 The incumbents, as evidence — who earns here and how

| App (package) | Rating | Installs | Reviews (exact) | Ads | IAP | Read as evidence |
|---|---:|---:|---:|---|---|---|
| Menu Maker, Price List Maker (`com.menu.maker`) | 4.9 | **1M+** | 42 293 | true | true | a million installs on this job: **someone pays here** |
| Price List & Menu Maker (`com.desygner.menus`) | 4.6 | **500K+** | 24 181 | true | true | a second paid incumbent — this is not an empty shelf |
| Make Price List & Invoice (`com.oaklagcs.mypricelist`) | 3.6 | **500K+** | **not enriched** | **not enriched** | **not enriched** | 500K+ installs at 3.6 — the installed base outgrew the product. Its review count and its ads/IAP flags were **not read**: it never appeared in the `--top 8` enrichment set for this query, so this row carries rating and installs only |
| Price List Maker - Fast & Easy (`com.pricelistmaker`) | 4.1 | **100K+** | 3 730 | true | true | 100K+ installed, then 3 of 3 sampled reviews are paywall |
| Price List Maker (`com.micrasol.pricelistmaker`) | **2.3** | 50K+ | 148 | false | true | **the pocket**: real installs, weak product |
| Price List (`com.zakasoft.pricelist`) | **1.7** | 50K+ | unknown | — | — | the weakest rated card with real installs |

`MEASURED` for the rating and install column of **every** row (`scan-complaints.mjs`, this session) and
`ON RECORD` for the review counts and flags of the four rows that were actually enriched
(`docs/research/2026-10-16-market-scans.md` Shelf B, each with its own `enrich-<query>.log:L<n>`
pointer: `com.menu.maker` L14, `com.desygner.menus` L8 of `enrich-menu-maker.log`,
`com.pricelistmaker` L10 and `com.micrasol.pricelistmaker` L9 of `enrich-price-card-maker.log`).
**Two rows are deliberately incomplete and say so**: `com.oaklagcs.mypricelist` and
`com.zakasoft.pricelist` never entered the `--top 8` enrichment set, so their review counts and
ads/IAP flags were never read — and the first draft of this table gave the former a review count
lifted from a neighbouring app, which is precisely the invented number this document's own rules
forbid. It was corrected against the archive rather than left standing. **This is a buying shelf with
a weak layer inside it**, which is the best shape a proposal can be handed: the demand is not a guess,
and the reachable pocket is not the
leader's price point.

---

## 4. The archetype, and every archetype rejected, in revenue terms

**Chosen: `purchase_led_repeat_sales`.** Capability required: `billing`.

| Archetype | Verdict | The revenue fact that decided it |
|---|---|---|
| `purchase_led_repeat_sales` | **chosen** | Every set incurs a provider cost **at the moment it is served** — one generated background at $0.050 plus one absorbed recognition at $0.010 = **$0.060 per set** (money plan §1, §4 C3). A consumable pack is the only shape where the revenue for a unit is collected *before* the cost of serving it. The job also repeats by nature: the trigger is a price change or a season. |
| `purchase_led_single_unlock` | rejected | One price against a recurring per-use cost is an unbounded loss on a heavy user. Quantified: a shop that prints 40 sets a month costs **40 × $0.060 = $2.40/month**, every month, while a $19.99 unlock nets **$16.16 once**. Against a 400-set month for a small chain the cost is **$24.00/month** — the single unlock turns negative in its first month and stays there. |
| `ads_led_utility` | **rejected — the arithmetic kills it at this price point** | `ON RECORD` (money plan §2.1): a rewarded-centric app earns ≈ **$0.05 per DAU per day**. At 1 000 DAU that is **$1 500/month**. The free tier this job needs — one set per user per day, since the whole promise is that the owner can print — costs 1 000 × $0.060 = **$1 800/month**, i.e. **$300/month more than the ads earn**, before any other cost; at the cheaper background-only set ($0.050) it breaks exactly even at **zero margin**. An ad-led version of this app loses money by construction. |
| `purchase_only_no_ads` | rejected | A price in Play Console with no discovery (`docs/monetization-strategy.md` §4), and it cannot fund a per-use provider cost any better than the single unlock can. |
| `subscription` | **forbidden** | Banned by the owner (2026-10-01), absent from `ARCHETYPES`, and machine-refused by `parseEconomics` with no exemption path. Not weighed. |

---

## 5. Revenue arithmetic, with the division shown

### 5.1 The provider costs this plan is priced against

`ON RECORD` — `docs/research/2026-10-16-money-ecosystem-ranking.md` §1 (read this session), whose own
source is the factory's model registry crossed against a 2026-10-15 fal pricing read. The capability
inventory's own row for each operation is quoted beside it.

| Op | Model | Job in this app | Credits charged | Provider cost per call |
|---|---|---|---:|---:|
| O-07 | `fal-ai/flux/dev` | generate the background the card is typeset on | 2 | **$0.050** |
| O-01 | `fal-ai/any-llm/vision` | read the photographed dish or the old card | 0 (absorbed) | $0.010 |
| — | on-device | **typesetting the item names, prices, QR code and grid** | 0 | **$0.00** |
| O-02 | `fal-ai/bria/background/remove` | cut the shop's own dish out for the card | 1 | $0.018 (optional, one per photographed item) |

**Cost per set = $0.050 + $0.010 = $0.060.** It is the cheapest deliverable in the money plan's whole
ranking (`…money-ecosystem-ranking.md` §4 C3) for one structural reason: **one generation feeds five
deliverables**, because the layout work happens free on the phone.

### 5.2 The divisor, and the floor it sets

`ON RECORD` — `docs/monetization-strategy.md` §2.2 (the divisor formula, `:165-183`) and §2.2's
assumption table (`:204-212`). The inputs are that document's **assumptions**, not Play settlement.

```text
net factor N = (1 − F_fraud)(1 − F_billing)(1 − F_refund)(1 − F_fx)
             = (1 − 0.01)(1 − 0.15)(1 − 0.02)(1 − 0.02)
             = 0.99 × 0.85 × 0.98 × 0.98
             = 0.8081766

required multiple K at a 60 % target margin
             = 1 / [N × (1 − M_target)]
             = 1 / [0.8081766 × 0.40]
             = 1 / 0.32327064
             = 3.0933833

so a unit that costs $0.060 to serve must be priced at ≥ K × $0.060 = $0.18560
```

`MEASURED (arithmetic over the two labelled rows above)` — computed this session.

### 5.3 The division, per pack

```text
price per set            = pack price ÷ sets in the pack
net proceeds per set     = price per set × 0.8081766
model cost per set       = $0.060
net per set              = net proceeds per set − $0.060
sales / month for $1,000 = 1000 ÷ net per sale
```

| Sale | Gross | Play + deductions (−19.18 %) | Net proceeds | Model cost | **Net per sale** | **Sales/mo for $1,000** |
|---|---:|---:|---:|---:|---:|---:|
| Starter — 5 sets | $14.99 | −$2.87 | $12.11 | $0.30 | **$11.81** | **85** |
| **Studio — 15 sets** | **$34.99** | −$6.71 | $28.28 | $0.90 | **$27.38** | **37** |
| Workshop — 40 sets | $79.99 | −$15.34 | $64.65 | $2.40 | **$62.25** | **16** |
| Unlock — print export + brand kits, no provider cost | $19.99 | −$3.83 | $16.16 | $0.00 | **$16.16** | **62** |

**The ratio check, on the pack that carries the target:**

```text
Studio pack: price per set = $34.99 ÷ 15 = $2.3327
model cost per set                        = $0.060
ratio = $2.3327 ÷ $0.060                  = 38.9×   ← required floor is K = 3.09×
```

**The free tier, priced before it is promised:** a starter grant of **10 credits per device** is
**5 free sets** and costs **5 × $0.060 = $0.30 per device** to serve. The existing server caps that
grant with a starter budget of **$10/day** and a global **$25/day** spend cap (`ON RECORD` — money
plan §1.1, `config.ts:73-74`), so a launch spike cannot turn the free tier into an unbounded bill.
**This is the shape that makes an ad-led version impossible and a consumable pack necessary: the free
tier costs real money, and the pack collects it before the money is spent.**

### 5.4 The honest limits of that division

- **No conversion rate is measured anywhere in this document.** The sales/month column is a division
  of the target by net-per-sale, not a forecast (money plan §10).
- **The ladder is this role's proposal, not a market price.** No competitor's Android price was read
  for this job; the shelf printed `free` on every enriched card.
- **The fee tier is `UNVERIFIED`, and it can sink the ladder.** At a 25 % or 30 % effective Play fee
  the net factor falls to 0.7130970 or 0.6655572 and **K rises to 3.5058 or 3.7562**, so a $0.060 unit
  must be priced at ≥ **$0.21035–$0.22537**. The Studio pack's $2.3327 per set still clears that
  comfortably — this is the one place this recipe is more robust than a credit-priced one, because its
  price-to-cost ratio is 38.9× rather than 3.3×. The decider is the Play Console fee page for this
  account.

---

## 6. The promise contract

The gate-ready artefact is `docs/ideation/plans/placard-menu-price-cards.differentiators.json`.
**Nine promises, four of them `core`, every one `deferred`.** The job's shape is covered
`before` → `bring-the-list-the-shop-already-has`, `during` → `one-list-becomes-five-print-pieces`,
`after` → `print-ready-export-no-watermark`, and **five of the nine are connective handoffs**
(`data_in` ×2, `data_out` ×1, `find` ×1, `durability` ×1, `reminder` ×1), so the contract can satisfy
the connective floor once the handoffs ship.

| id | weight | stage | family | The promise, and the measured reason it is worth keeping |
|---|---|---|---|---|
| `bring-the-list-the-shop-already-has` | core | before | `data_in` | The shop's existing list arrives as a CSV, a pasted block or a photo of the old card through the system picker or the camera, and lands as editable rows. *Reason:* Zakasoft's 1.7★ 50K+ app is asked twice in three sampled reviews how to get the same entered items and prices onto a second phone |
| `one-list-becomes-five-print-pieces` | **core** | **during** | `core_operation` | One list becomes a menu, a price card, a table tent, a window sign and a story post in the same look. *Reason:* no card on the pocket's six advertises a second print format for the same list, and the 500K+ leader is complained about for a feature it cannot show |
| `prices-are-typeset-not-drawn` | **core** | during | `core_operation` | Every price is typed data typeset on the phone, never drawn by a model. *Reason:* the pocket's shared complaint is *accuracy* — 3 of 14 critical reviews, incl. Micra's *"the entered price amounts are not being retained"* |
| `print-ready-export-no-watermark` | **core** | **after** | **`data_out`** | A print page through `PdfDocument` plus a share image through `ACTION_SEND`/`FileProvider`/`ACTION_CREATE_DOCUMENT`, with no watermark. *Reason:* the 1M+ leader is reviewed *"you can't even save high quality image without paying"*; paywall is 4 of 14 critical |
| `the-shops-own-dish-on-the-card` | supporting | before | `data_in` | The owner photographs a dish or a shelf and the cut-out item sits on the generated card. *Reason:* the pocket's broken class (5 of 14 critical) is where images inside cards fail, and the cut-out is $0.018/1 credit in the factory's own registry |
| `shop-brand-kit-set-once` | supporting | during | `fit` | Colours, font and mark are set once and applied to every later set. *Reason:* Desygner at 4.6★/500K+ is reviewed *"the fonts not working correctly after I'm paying for it"* |
| `find-a-list-or-a-price-row` | supporting | after | `find` | A search field over the shop's own catalogue finds a row or a past set. *Reason:* a Micra reviewer's actual use is *"specific, unique items that I sell"*, which is a list too long to scroll |
| `list-survives-a-new-phone` | supporting | after | `durability` | The list and brand kit survive a reinstall and a phone change through the app's own backup rules. *Reason:* 2 of the pocket's 6 weakest cards are complained about for the data not following the owner |
| `dated-price-refresh-nudge` | supporting | after | `reminder` | An opt-in, owner-dated reminder to refresh a stale card. *Reason:* `INFERRED` — the accuracy and missing-feature complaints are about a list gone stale; no sampled review asks for a nudge, and the plan says so |

**Anti-scope-creep, stated so the build cannot pad it:** the contract carries **no** marketplace rule
linter, **no** listing-copy writer, **no** SKU batch runner and **no** marketplace export pack — those
are `plinth-product-photos`' shipped promises
(`…generation-capability-inventory.md` §2a) and a promise here is a print job or it is not here.

**The gate's own CRUD report, disclosed rather than left for the owner to find.** `MEASURED` —
`node scripts/check-differentiators.mjs --differentiators docs/ideation/plans/placard-menu-price-cards.differentiators.json --allow-deferred`
prints `9 promise(s), 4 core, 0 shipped, 9 deferred` and a **human-read, never-gating** note that two
promises read as record operations: `prices-are-typeset-not-drawn` reads as `AUDIT` and
`find-a-list-or-a-price-row` reads as `CRUD-shaped`. Both rows stay, and the reason is the report's own
mechanism: it is a lexical predicate that cannot see that one row is an invariant about *who draws the
characters* and the other is the `find` handoff family whose token (`searchQuery`) the gate itself
requires. Neither promise is a record operation, and neither was rewritten to escape a note — a promise
edited to pass a note is the failure mode `.cursor/rules/differentiator-contract.mdc` names.

---

## 7. The name, derived rather than invented

Gate-ready artefact: `docs/ideation/plans/placard-menu-price-cards.naming.json`. Seven candidates were
scored across **five** archetypes, and the winner is a carrier plus the job noun.

**Chosen: `Placard: Menu Price Cards` — 84/100, no dimension below 3.**

| Candidate | Archetype | Score | Why |
|---|---|---:|---|
| **Placard: Menu Price Cards** | **carrier_job_noun** | **84** | *Placard* is the real word for the sign a shop puts on a wall, counter or window, and it is the artefact this app makes rather than a decoration; *Menu Price Cards* is what the owner types |
| Chalkline | coined | 78 | Compact, ownable and evokative of a chalkboard, but it names no job noun, so discovery has to be bought |
| Counter Card | compound | 74 | Says where the artefact lives, but "counter" also reads as *a counter* in the counting sense and the phrase is close to generic |
| Priceboard | compound | 71 | Reads instantly, but "price board" is a category phrase rather than a name, and it is unownable in search |
| Tackup | coined | 70 | Short and distinctive, but it says nothing about prices, menus or printing and needs a pronunciation guide |
| Slate: Price Cards | evocative | 69 | Strong imagery for a chalkboard menu, but *Slate* is a well-known magazine and a common brand word; the collision risk alone costs it |
| Menu Price Cards | descriptive | 66 | **Negative control** — the bare descriptive phrase competes for the head term the 1M+ leader already owns, with no carrier for a user to prefer |

Rejected, with the reason each lost, is recorded in the same file (`Menu Price Cards`,
`Slate: Price Cards`, `Priceboard`, `Tackup`).

- **Length:** **25 characters** in `en-US` (`Placard: Menu Price Cards`) and **23** in `ru-RU`
  (`Placard: Меню и ценники`) → **5 and 7 characters of slack** under Play's 30-character cap
  (`scripts/lib/slot-naming.mjs` `TITLE_MAX`). No declared locale is over it, and the slack is stated
  because a title that only fits in English is the name-shaped version of a localisation defect.
- **Slug → package:** `placard-menu-price-cards` → `com.gexetr.placardmenupricecards`
  (`scripts/lib/namespaces.mjs:32-34`, dashes stripped).
- **Collision:** `UNVERIFIED`. This scan did not run a Play title search and queried no trademark
  register. The decider is a Play search for the exact title plus a class-9 search of TMview, USPTO,
  EUIPO and WIPO for *Placard* and its close variants; a clean search is not legal clearance.
  Measured within the shelf and within this run, which is the enforceable half: `scan-titles.mjs`
  printed 98 title lines across the three shelves with zero matches for the six carriers tested, and
  the run's other two proposals chose `Roomlumen` and `Warp: Print Mockups`, so no two recipes of
  2026-10-16 share a carrier (`MEASURED` — `Select-String -Path docs/ideation/2026-10-16-*.md -Pattern "Placard"`; the only hits are in this document).
- **Localisation:** the Russian title reads naturally («Меню и ценники» = menu and price tags) and is
  not machine output, but whether a Russian-speaking shop owner discovers an English job noun is
  **unmeasured** and needs a native read before the listing is written.

---

## 8. Build estimate, permissions, server and capabilities

- **Build estimate: ESTIMATE, and explicitly not a criterion.** ~**150–220 agent hours**: the
  device-side typesetting and layout engine (the price-row component, the five format templates at
  print sizes, the brand kit), the import paths (CSV/TSV parsing, a pasted block, recognition of a
  photographed card), the export paths (a `PdfDocument` writer with real page sizes, a share image, a
  save-to-folder document), the billing surface over the existing credit ledger, the search and backup
  handoffs, settings localisation and tests. Per the owner's 2026-10-08 instruction, build cost is
  **never** a reason to refuse, rank down, narrow or deprioritise; it is recorded for the owner's
  information only.
- **Permissions required: one runtime prompt.** `android.permission.CAMERA` for photographing a dish
  or an old price card, and `android.permission.POST_NOTIFICATIONS` for the opt-in refresh reminder.
  `INTERNET` at install time. No location, no audio, no contacts, no Bluetooth. The list import uses
  the picker and the Storage Access Framework (no runtime permission), and the export uses
  `ACTION_SEND` and `ACTION_CREATE_DOCUMENT` (no runtime permission).
- **Capabilities required:** `billing` (Play Billing via the factory's `capabilities/billing` module
  and a server-backed `PurchaseAuthority`). **Not `ads`.**
- **A server is required, and its cost is stated rather than discovered.** The generated background is
  a provider call with a server-only key; the factory's existing generation server is
  server-authoritative over a credit ledger with Play-confirmed purchases, two-phase
  reserve/settle/release, a $25/day spend cap and release-on-failure
  (`…generation-capability-inventory.md` §1c–1f). **Its cost is the provider cost in §5.1 —
  $0.060 per set** — and it is carried by the pack price, not by a subscription. Three limits stated
  rather than implied: the server is **slot-local, not a published capability** (§3b), so this slot
  either reuses that deployment or copies the server; the app needs **its own registry entries and
  routes**, since the operation ids are the photo slot's jobs (§3b); and the background operation
  (O-07) is enabled in the registry but has **never been reachable from a shipped UI** — the photo
  slot's studio screen is wired to an "unavailable" toast (§2b) — so its readiness end-to-end is
  `UNVERIFIED` until it is driven once (§12).

---

## 9. Device capabilities

Each capability is answered `used` or `not used`; a `used` line names the API the slot would call and
what the job loses without it.

- camera: used — `ActivityResultContracts.TakePicture` (and `ACTION_IMAGE_CAPTURE`); `android.permission.CAMERA`; without it the owner of a food truck or a market stall cannot photograph a dish or the old price card on the spot, and the whole set has to be typed in from scratch.
- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia`; no runtime permission; without it a photo the owner already took has to be exported to Files before the app can see it, which is the step that makes a busy owner give up.
- media playback or capture: not used — nothing in this job is played back or recorded; the artefacts are still pages.
- microphone: not used — no part of a price list is spoken, dictated or recorded.
- location: not used — a price card is the same card wherever the shop stands, and a location prompt would buy nothing.
- motion sensors: not used — the card is laid out by touch and drag, never by moving the phone.
- contacts / calendar: not used — the app never needs the owner's address book or their diary; the seasonal reminder is dated by the owner inside the app.
- notifications / reminder: used — `WorkManager` plus a `NotificationChannel`; `android.permission.POST_NOTIFICATIONS`; without it a card that is a year old stays on the counter and nobody tells the owner, which is the staleness the pocket's accuracy complaints are about.
- home-screen widget: not used — the shop's price board lives on the wall, not on the phone's home screen.
- share / export: used — `ACTION_SEND` with a `FileProvider` URI plus `ACTION_CREATE_DOCUMENT`; without it the finished card cannot reach the print shop, the owner's own printer or the shop's WhatsApp, and the job cannot be finished with.
- Bluetooth / USB: not used — the deliverable leaves as a page through the share sheet and the phone's own print dialog, so pairing a printer directly is not this plan's job.
- files / SAF: used — `ACTION_OPEN_DOCUMENT` through the Storage Access Framework, plus `ACTION_CREATE_DOCUMENT` for the saved page; without it the shop's existing CSV or spreadsheet cannot be opened and the printed PDF cannot be filed where the owner keeps their paperwork.

---

## 10. Visual direction

A **counter-print** identity: an inked slate-green field (`#16302A`) onto which a chalk-cream sheet
(`#F4EFE2`) is laid, with a single **vermilion** accent (`#E2543A`) reserved for the price column and
for the moment a set is exported — no indigo, no amber, no magenta, and nothing shared with another
slot. The mark is the slot's own **placard glyph**: a slate panel with one corner clipped and a single
price rule across it, drawn as vector in `store/studio.json`'s `mark` so it reads at 48 dp and
survives every adaptive-icon mask. The component that is not stock Material is the **price row** — the
item name, a hand-set dot leader and a price right-aligned to a shared baseline, with the section rule
above it and the accent only on the price — plus the **format strip** that shows the five artefacts a
single list produced. Palette, mark and component values are declared once in `store/studio.json` and
read again from the slot's own `ui/theme/Color.kt`, so the pack cannot photograph a different app than
the one that ships (`.cursor/rules/visual-identity.mdc`).

---

## 11. Kill criteria — three finite numbers

`ON RECORD` — proposed by the money owner from this shelf's measured demand in
`docs/research/2026-10-16-money-ecosystem-ranking.md` §8 (C3 row); the owner decides them at
checkpoint 1. Schema bounds are `windowDays` 7–180, `minInstalls ≥ 1`, `minRevenueUsd ≥ 1`
(`scripts/lib/slot-economics.mjs`).

| `windowDays` | `minInstalls` | `minRevenueUsd` | What missing them means |
|---:|---:|---:|---|
| **90** | **1 500** | **$250** | SMB owners do not install a phone app for print work. |

Read with §5.3: at **$27.38** net per Studio pack, `minRevenueUsd: $250` is **10 pack sales in 90
days** — the threshold says when to stop; the $1,000/month target says what working looks like.

---

## 12. Honest unknowns, each with the command that settles it

1. **Whether an image model actually mangles prices.** `UNVERIFIED` — no generation was run. Decider:
   one authenticated `POST https://queue.fal.run/fal-ai/flux/dev` with a prompt such as *"a chalkboard
   price card reading 7.90 and 12.50"*, then read the PNG and check both numbers. If both render
   correctly, the on-device-typesetting premise is weakened — **and the design still stands**,
   because §2 shows the pocket's measured complaint is lost price *data*, not model spelling.
2. **Whether SMB owners will install a phone app for print work.** This is the kill criterion, and it
   is `UNVERIFIED` until this slot's own `store/outcomes.json` reading exists.
3. **No conversion rate is measured on this shelf.** The only figure the factory has (~1–3 % for
   one-time IAP) is labelled `UNVERIFIED` in `docs/monetization-strategy.md` §4.
4. **The pack ladder is a proposal, not a reading.** Every enriched competitor card printed `free`,
   so no competitor price anchors it.
5. **The effective Play fee tier is `UNVERIFIED`** — at 25–30 % the required multiple rises to
   3.5058–3.7562, which this ladder still clears but with less room (§5.4).
6. **The recorded provider costs are the factory's own registry values**, not a live fal read today
   (money plan, capability inventory "What is still missing"): the decider is re-reading each
   `fal.ai/models/<endpointId>` page.
6a. **No enrichment of my own is behind any number in §3.4's richer rows.** The live attempt,
   `node scripts/enrich-market.mjs --concurrency 3 --top 8 --query "menu maker" --query "price card
   maker" --query "flyer maker" --query "poster maker"`, **failed with exit 2** —
   `TimeoutError: The operation was aborted due to timeout`, 36 402 ms — so every review count,
   `containsAds` and `inAppPurchases` value quoted here comes **`ON RECORD`** from
   `docs/research/2026-10-16-market-scans.md` Shelf B and its `enrich-<query>.log:L<n>` pointers,
   written by an earlier run on the same day. Two of the six rows could not be sourced at all and say
   `not enriched` rather than carrying a number (§3.4). The decider for a re-read is the same command,
   run one query at a time with `--concurrency 1`.
7. **One signed-out Play session on one storefront** with a ~30-card display cap produced every demand
   reading here; relevance order is not size order, so a larger incumbent can sit below the cut.
8. **Whether the print-ready PDF is what a Russian or EU print shop accepts** (page size, bleed, CMYK)
   is `UNVERIFIED` — no print shop was consulted, and the plan promises a page rather than a
   print-shop-ready imposition.
9. **`Placard` was not trademark-searched** (§7), and a Russian-locale discoverability read is
   outstanding.

### The exact command that would flip this proposal to a refusal

Re-read the pocket's complaints, and let one absence refuse the recipe:

```powershell
node scripts/scan-complaints.mjs --top 6 --concurrency 2 --query "price card maker"
```

**Falsifier:** if a re-run of that command returns a `price card maker` pocket whose critical reviews
contain **no** `accuracy`, **no** `broken` and **no** `paywall` finding — i.e. if the weak cards turn
out to be weak for reasons this recipe does not answer — then the wedge has no measured complaint
behind it, the pocket is an artefact of the sample, and the recipe is refused. Equivalently, if
`node scripts/scan-market.mjs --query "price card maker"` returns a pocket with **no rated incumbent
below 4.2 and 100K+ installs**, there is nothing to enter.

---

## 13. If the owner says yes

```bash
# 1. The change (the planner authors it, before any code)
#    openspec/changes/app-placard-menu-price-cards/  ->  proposal.md, tasks.md, specs/**/spec.md
# 2. Then the slot (refuses without the plan, the promises, the name and the change)
node scripts/new-slot.mjs \
  --economics docs/ideation/plans/placard-menu-price-cards.economics.json \
  --differentiators docs/ideation/plans/placard-menu-price-cards.differentiators.json \
  --naming docs/ideation/plans/placard-menu-price-cards.naming.json \
  --spec app-placard-menu-price-cards
```

`--slug` and `--name` derive from the naming contract, and `capabilities.json` must enable `billing`
(the `purchase_led_repeat_sales` pairing).

---

## 14. Refused candidates, each with its revenue reason

Nothing below is refused for "it already exists". An occupied shelf is evidence of demand.

| Refused candidate | Revenue reason it lost |
|---|---|
| **A flyer / poster maker** — the shelf's own headline job | **The shelf is a measured fortress and the buyer pays nothing for it.** `flyer maker` is `fortress` (14/23 rated incumbents at 4.5+, only 3/23 below 4.2) and `poster maker` is `fortress` (13/19 at 4.5+, 1/19 below), with Canva at 500M+, `Flyer Maker, Poster Maker` at 10M+ 4.8 (385 313 reviews) and `Poster Maker - Flyer maker` at 10M+ 4.7 (240 363); and the pocket's criticism is the business model — **paywall 6 of 12 critical (50 %) and ads 4 of 12 (33 %)**. There is no wedge statement here that is a difference in *functionality*, so it fails the bar before the arithmetic. **One precision, because it cuts the other way:** the flyer pocket's weakest card is `com.radioflyer.flyer` at **2.2★ with 500+ installs** — which is the **Radio Flyer** children's-bike app, not a flyer maker, so the pocket is even more entrenched than its 3-of-23 weak count suggests; the on-job cards there are `app.poster.maker.postermaker.flyer.designer` (4.2★, **10M+**), `com.thmobile.postermaker` (4.3★, **5M+**) and `com.postermaker.flyermaker.tools` (4.3★, 500K+). `MEASURED` — this session's scan + complaints runs. |
| **A social-post-only maker** (money plan §4 C6) | **Worse money for the same gross, on a fortress shelf.** Net per sale at the same $14.99 is **$10.62 for 30 posts** against **$11.81 for 5 sets** here (the C3 division in §5.3), because each post costs $0.050 to serve while one set's background serves five artefacts; and it lands on the `flyer maker`/`poster maker` fortress where Canva ships the job free. |
| **A QR-code digital menu with hosted pages** | **A recurring per-user hosting cost with no shape that funds it.** The measured competitors are `Menusa` (4.3, 10K+, ads false, IAP **false**) and `SaveMenu` (unrated, 5K+, IAP true) — i.e. the job is partly given away free — and a hosted menu is a per-user server cost with subscriptions banned, which leaves either a loss-making free tier or a licence this factory has not costed. Second reason: its artefact is a web page, not a printed one, so its `after` stage is not this shelf's. |
| **A price-list-plus-invoice app** | **The pocket's own install leader already occupies it at 500K+, and the invoice half drags in tax and legal content.** `com.oaklagcs.mypricelist` ("Make Price List & Invoice") holds **500K+ installs at 3.6★** and is reviewed as too simple rather than as wrong; adding invoicing means competing with dedicated invoicing apps on a document format, and it would make this proposal one sentence with the leader's. |
| **A catalogue / PDF price-list maker** | **Its own head term is not the job the pocket is weak in.** The `Catalog Maker: PDF Price List` cards appear three times in the `price card maker` title scan, and a catalogue is a multi-page product document — a different artefact from a counter card, with a different buyer (a wholesaler rather than a shop counter) and a page-count-driven job this plan does not do. Keeping both would be one sentence. |
| **A sticker / barcode label maker** | `scan-titles.mjs` measured a live `Price Label Maker` card, and the job needs a paired thermal printer (`Bluetooth / USB`) plus its own label stock — hardware this plan deliberately does not take on (§9). Its money is attached to printer compatibility, not to design, so this factory's edge does not apply. |
| **A calculator, converter or unit tool of any kind** | Closed topic — the factory's own pipe-sizing slot shipped with `capabilities.json = {"schemaVersion": 1, "enabled": []}`, i.e. no revenue mechanism at all, while every gate passed (`docs/reviews/2026-09-26-slot-1-economics-postmortem.md`). Named here so it is not re-proposed. |

---

## Claim ledger

```text
CLAIM | LABEL | EVIDENCE
`price card maker` is mixed, 30 seen, 14/30 rated, 5 weak, median 4.35 | MEASURED | `node scripts/scan-market.mjs --concurrency 4 --query "menu maker" --query "price card maker" --query "flyer maker" --query "poster maker" --query "social media post maker"`, 2026-10-16 (22 queries, 3 candidate / 7 mixed / 6 fortress / 6 dead-zone, 0 errored)
`flyer maker` is fortress (23/30 rated, 3 weak, median 4.60) and `poster maker` is fortress (19/30 rated, 1 weak, median 4.60) | MEASURED | same `scan-market.mjs` run, reason lines "14/23 rated incumbents are >= 4.5 … 0.13 < 0.3" and "13/19 … 0.05 < 0.3"
`menu maker` is mixed, 13 seen, 6/13 rated, 1 weak, median 4.45; `social media post maker` mixed, 26/30 rated, 8 weak, median 4.40 | MEASURED | same `scan-market.mjs` run
The price card pocket is 18 reviews, 14 critical, 0 no-text; broken 5 (36 %), paywall 4 (29 %), accuracy 3 (21 %), missing-feature 3 (21 %) | MEASURED | `node scripts/scan-complaints.mjs --top 6 --concurrency 2 --query "price card maker"`, 2026-10-16
The menu pocket is 18 reviews / 6 critical (crash 2, regression 2) and the flyer pocket is 18 / 12 critical (paywall 6 = 50 %, ads 4 = 33 %) | MEASURED | same `scan-complaints.mjs` run, `--query "menu maker"` and `--query "flyer maker"`
`com.micrasol.pricelistmaker` 2.3★ at 50K+ installs with a 1★ review saying entered price amounts are not retained within each product category | MEASURED | same `scan-complaints.mjs` run (`price card maker`, third card, verbatim line)
`com.zakasoft.pricelist` 1.7★ at 50K+ installs, two of three sampled reviews asking how to use the same entered data on two phones | MEASURED | same `scan-complaints.mjs` run (`price card maker`, second card, verbatim lines)
`com.pricelistmaker` 4.1★ at 100K+ has 3 of 3 critical reviews classed paywall, incl. a trial that deducts money on day 1 and an unreliable Excel import/export | MEASURED | same `scan-complaints.mjs` run (`price card maker`, fifth card)
`com.menu.maker` 4.9★ at 1M+ is reviewed "you can't even save high quality image without paying" and its critical sample is ads / missing-feature / notifications | MEASURED | same `scan-complaints.mjs` run (`menu maker`, last card)
`com.desygner.menus` 4.6★ at 500K+ is reviewed "the fonts not working correctly after I'm paying for it" | MEASURED | same `scan-complaints.mjs` run (`menu maker`, fourth card)
`com.Artificer.CardMaker` (1.4★, 10K+) is an MTG card-game maker, so its reviews are not this pocket's evidence | MEASURED | `scan-titles.mjs` classification recorded in `docs/research/2026-10-16-market-scans.md` § "Price card maker — off-job / title-ambiguous"; its sampled text mentions "custom tokens", not a shop's price list
No critical review in the three pockets names print resolution, DPI, bleed or print colour (0 of 32) | MEASURED | same `scan-complaints.mjs` run: the taxonomy classes printed are crash, regression, ads, broken, missing-feature, notifications, paywall, performance, accuracy, confusing-ui, account, value, offline, privacy
Every priced/ads/IAP reading of `com.menu.maker`, `com.desygner.menus`, `com.micrasol.pricelistmaker` and `com.pricelistmaker` with its exact review count and its `enrich-<query>.log:L<n>` line | ON RECORD | `docs/research/2026-10-16-market-scans.md` Shelf B tables (`:136-137`, `:146-147`, `:151`); that document's own method is `node scripts/enrich-market.mjs --query "<one query>" --concurrency 2 --top 8`
`com.oaklagcs.mypricelist` and `com.zakasoft.pricelist` were never enriched, so their review counts and ads/IAP flags are unread rather than unknown-and-guessed | MEASURED | `rg -n "oaklagcs\|zakasoft" docs/research/2026-10-16-market-scans.md` → one title-scan mention of ZakaSoft at `:192-193`, nothing for oaklagcs; neither name appears in any Shelf B table
`com.oaklagcs.mypricelist` is 3.6★ at 500K+ with 2 missing-feature findings in 2 critical reviews | MEASURED | `node scripts/scan-complaints.mjs --top 6 --concurrency 2 --query "price card maker"`, verbatim line "com.oaklagcs.mypricelist 3.6★ 500K+ reviews=3 critical=2 top=missing-feature(2)"
`price list maker` was a `candidate` pocket in an earlier, weaker sample while this broader pass reports the shelf mixed | ON RECORD | `docs/research/2026-10-16-market-scans.md` § "Most important expectation change", citing `2026-10-16-money-ecosystem-ranking.md:474`
O-01 costs $0.010 at 0 credits, O-02 $0.018 at 1 credit, O-07 $0.050 at 2 credits; cost per set ≈ $0.060 | ON RECORD | `docs/research/2026-10-16-money-ecosystem-ranking.md` §1 and §4 C3, read this session
Net factor 0.8081766; K = 3.0933833 at a 60 % margin; at a 25–30 % fee K = 3.5058–3.7562 | ON RECORD | `docs/monetization-strategy.md` §2.2 (`:165-183`, `:204-212`) and §2.2's fee-sensitivity note; re-derived in `…money-ecosystem-ranking.md` §2 and §2.1
A rewarded-centric ads app earns ≈$0.05/DAU/day | ON RECORD | `docs/monetization-strategy.md` §2.1
The plan's kill criteria (90 / 1 500 / $250) are the money owner's proposal for this shelf, not a measurement | ON RECORD | `docs/research/2026-10-16-money-ecosystem-ranking.md` §8 (C3 row)
The registry has no text endpoint; every routed op is image-in/image-out or image-in/JSON-out | ON RECORD | `docs/research/2026-10-16-generation-capability-inventory.md` §1b, §4.1
The generation registry, adapter, routes and server are slot-local, not a published capability, and the background op is enabled but unreachable from the shipped UI | ON RECORD | same inventory, §3b and §2b
The starter budget is $10/day and the global spend cap $25/day; purchases are confirmed server-side | ON RECORD | `…money-ecosystem-ranking.md` §1.1 and §2 (citing `config.ts:73-74` and `purchases.ts`)
Every net-per-sale and sales/mo figure in §5.3 | MEASURED (arithmetic over the labelled rows above) | computed this session as `gross × 0.8081766 − sets × $0.060`, shown in the table; ratio check `$2.3327 ÷ $0.060 = 38.9×`
`applicationIdForSlug('placard-menu-price-cards')` = `com.gexetr.placardmenupricecards` | MEASURED | `scripts/lib/namespaces.mjs:32-34`
Play's title cap is 30 characters and `TITLE_MAX = 30` | MEASURED | `scripts/lib/slot-naming.mjs:29`
`subscription` is absent from `ARCHETYPES` and refused by `parseEconomics` | MEASURED | `scripts/lib/slot-economics.mjs` `ARCHETYPES`; `docs/slot-economics.md` § "Subscription is banned"
The device-capability declaration answers all twelve capabilities | MEASURED | `node scripts/verify.mjs` → `ideas.capabilities` (this document, §9; 12 lines, camera and notifications naming `CAMERA` and `POST_NOTIFICATIONS`)
An image model renders prices incorrectly | UNVERIFIED | no generation was run; the decider is one authenticated `POST https://queue.fal.run/fal-ai/flux/dev` with a prompt containing 7.90 and 12.50
Whether SMB owners install a phone app for print work | UNVERIFIED | no reading exists; the decider is this slot's own `store/outcomes.json` after ship
The effective Play fee tier for this account | UNVERIFIED | the decider is the Play Console fee page for this account
Whether `Placard` collides with an existing class-9 mark or Play app | UNVERIFIED | the decider is a Play title search plus TMview / USPTO / EUIPO / WIPO class-9 search
Whether a Russian-speaking owner discovers an English job noun in `ru-RU` | UNVERIFIED | the decider is a native-language naming review
Whether a print shop accepts the exported PDF as a print-ready imposition | UNVERIFIED | no print shop was consulted; the decider is one file sent to one print shop
```

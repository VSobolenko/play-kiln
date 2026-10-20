# Consumer / personal AI image and content generation — recon, 2026-10-16

**Shelf measured:** AI image and content generation a *person* buys for themselves and shows someone —
AI portraits / headshots / avatars, pet portraits, interior and home restyle, old-photo restore and
colourise, greeting cards / invitations / announcements, personalised wall art, personal photo cleanup.

**Not this shelf:** tools a *business* buys to produce a listing, a menu or a brand asset. That is the
sibling shelf (`docs/ideation/2026-10-16-ecosystem-marketplace-sellers.md`).

**Objective function, restated because it decides every row below:** expected revenue, not novelty. An
occupied shelf is evidence of demand. A candidate is refused on a **revenue** reason, a **capability**
reason (the priced operation the job needs does not exist or is disabled), or because it duplicates a
job our own catalogue already holds — never because "it already exists".

**One survivor: `roomlumen` — "Roomlumen: AI Room Design".** Every other sub-shelf on the shelf is
refused below, each with its reason.

**A subscription is forbidden for us** (`subscription` is absent from `ARCHETYPES` in
`scripts/lib/slot-economics.mjs`, so it cannot even parse). This shelf is a subscription monoculture, so
the honest work of this run was to test, sub-shelf by sub-shelf, whether a **one-time or consumable**
shape can match what the incumbents charge monthly. One can; five cannot, and each refusal says which
number killed it.

---

## What this run measured

Every demand, install, rating, price and complaint figure in this document came out of the factory's own
sensors on **2026-10-16**, run as `--concurrency 1` after a concurrent pass timed out against Play:

```powershell
node scripts/scan-market.mjs  --concurrency 1 --query "ai headshot" --query "ai portrait" --query "ai avatar" --query "professional headshot" --query "pet portrait" --query "custom pet portrait" --query "dog portrait" --query "interior design ai" --query "ai room design" --query "home design ai" --query "photo restore" --query "old photo restore" --query "colorize old photos" --query "greeting card maker" --query "custom poster" --query "personalised wall art" --query "ai photo enhancer" --query "photo quality enhancer"
node scripts/enrich-market.mjs --concurrency 1 --top 6 --query "interior design ai" --query "ai headshot" --query "pet portrait" --query "photo restore" --query "ai avatar"
node scripts/scan-titles.mjs   --concurrency 1 --query "roomlumen" --query "roomlight" --query "lumenroom" --out .kiln/scratch/recon-2026-10-16/scan-titles-naming.md
node scripts/scan-titles.mjs   --concurrency 1 --query "sunroom" --query "afterglow" --query "onewall" --out .kiln/scratch/recon-2026-10-16/scan-titles-naming-rejected.md
node scripts/scan-complaints.mjs --concurrency 2 --top 6 --query "interior design ai" --query "home design ai" --query "pet portrait"
```

18 queries screened: **3 `candidate`, 14 `mixed`, 1 `fortress`, 0 `dead-zone`, 0 errored**
(`MEASURED` — `.kiln/scratch/recon-2026-10-16/scan-market.md`). Raw sensor output is kept under
`.kiln/scratch/recon-2026-10-16/` and is disposable.

**Honest limits, stated rather than implied.** These are signed-out `en-US` Play searches: the store is
personalised and geo-dependent, so this is a spot check and not the factory's reachable market. Play
shows install *buckets*, not counts. No price was read from a checkout screen: the subscription prices
below are quoted from **user reviews that name the amount they were billed**, which is a weaker reading
than a store listing and is labelled as such wherever it appears.

---

## The shelf in numbers

| Sub-shelf | Screen | Strongest measured incumbent | Monetization (enriched) | Verdict |
|---|---|---|---|---|
| Interior / home restyle | `mixed`, 3–6 weak incumbents of 21–26 rated, median 4.35–4.40 | `interior.home.design.ai` Home AI — 10M+, 4.3★, **994 289** reviews | **6/6 IAP, 1/6 ads** | **SURVIVES** |
| AI headshot / portrait / avatar | `mixed` / `candidate`, medians 4.10–4.30 | `com.prequelapp.aistudio` GIO — 5M+, 4.5★, 174 437 reviews | 6/6 IAP, 0/6 ads | refused |
| Pet portrait | `mixed`, medians 4.20–4.30 | `ai.dreampets.dreampets` — **10K+**, 4.2★, 483 reviews | 6/6 IAP, 3/6 ads | refused |
| Old-photo restore / colourise | `candidate`, 12–15 weak of 25–28 rated | `tap.photo.boost.restoration` PhotoBoost — 10M+, 4.6★, 789 015 reviews | 5/6 IAP, **2/6 ads** | refused (capability) |
| Photo cleanup / enhancement | `mixed`, medians 4.20 | Remini 500M+ / UpFoto 100M+ (`ON RECORD`, money ranking §3) | **6/6 ads** (`ON RECORD`) | refused (arithmetic) |
| Greeting cards / announcements | `mixed`, median **4.50**, 0.5 of rated strong | Invitation Maker 10M+ 4.9★ / Canva 500M+ (`ON RECORD`) | 6/6 IAP, 1/6 ads | refused |
| Personalised wall art / posters | `custom poster` = **`fortress`** (13/19 rated ≥ 4.5, only 3 weak) | — | — | refused |

The single most valuable reading on this shelf is the monetization mix of the interior sub-shelf: **six
enriched leaders, six with in-app purchases, one with ads, four of them at 5M+ installs or more**
(`MEASURED` — `.kiln/scratch/recon-2026-10-16/enrich.md`, "interior design ai"). People pay for this job
by purchase rather than by attention, which is the opposite of the photo-enhancer sub-shelf — and it is
the only sub-shelf on this shelf where that is true.

---

## The surviving proposal

| | |
|---|---|
| **Slot slug** | `roomlumen` |
| **Working title** | Roomlumen: AI Room Design (25 characters, 5 of slack inside Play's 30; see *The name* below) |
| **Category / job** | Consumer interior restyle: **photograph your own room once, then change exactly one thing at a time — the wall colour, the floor, one piece of furniture, the light — with everything else in your room held as it is, and keep the versions you like.** |
| **Paying user** | A householder or renter redecorating or deciding whether to redecorate — and a homeowner staging a room to sell — who wants to *see* a change before spending money on paint or furniture, and who already has the room in their hand as a phone photo. |
| **What is bought** | Consumable **render packs** (credits, spent one render at a time) **plus** a one-time non-consumable **HD export unlock** that removes the pack limit and the watermark. |
| **Anchor price we must beat** | **$10.99 billed every two weeks** — measured, quoted verbatim from a 1★ review on the 100K+ incumbent `ai.home.room.designer` (*"I did 3 day free trial cancelled timely and still am billed every 2 weeks for $10.99"*), i.e. **≈ $23.81/month ≈ $285.74/year** (`$10.99 × 26`). Two further incumbents' reviews name a weekly charge (*"I purchased weekly subscription by paying 320 rupees"*, `com.arshroopsaini.houseai`) and a 3-day trial that charged immediately (`interior.home.design.ai`, 10M+). |

### The wedge, in one sentence

**Roomlumen changes one thing at a time and keeps your actual room — same walls, same furniture, same
camera angle, including the light — and sells the change per render instead of per week, where the
shelf's leaders regenerate the whole room (and get it wrong), let you steer almost nothing, and hold the
result behind a recurring charge with a free trial that bills.**

Every clause of that sentence is a difference in **functionality or content**, not in naming:

1. **one-variable control** — the incumbent's own users ask for it and cannot get it;
2. **the light as a first-class control** (`O-08 iclight-v2` relight, $0.104858) — no enriched leader on
   this shelf sells a relight pass;
3. **per-render purchase instead of a recurring charge** — the shape this shelf's users are measurably
   complaining about, and the only shape we are allowed to ship.

### What the shelf's own users complain about: the wedge, measured

`MEASURED` — `node scripts/scan-complaints.mjs`, output at
`.kiln/scratch/recon-2026-10-16/complaints-home-pets.md`.

| Query | Pocket sampled | Critical | Paywall complaints | Share of critical |
|---|---:|---:|---:|---:|
| `interior design ai` | 18 reviews | 14 | **6** | **43 %** |
| `home design ai` | 18 reviews | 13 | **7** | **54 %** |

The complaint taxonomy's own class list calls this `paywall — Paywall, price or subscription`, and the
same output carries `value` (21–23 %), `ads` (14–15 %) and `missing-feature` (8–14 %).

The quotes that name the wedge. These are the product faces of the three clauses above:

- **It cannot be steered, and the whole room is regenerated.** `com.sictec.infotech.design.sense`, 3.1★,
  100K+ — *"Generally unusable , the app needs to ask if the doors windows and openings are to be
  changed which is 30% of the renovations i do as an interior desi[gner]"*. `com.wonder.idealhouse.app`,
  3.8★, 100K+ — *"The initial generated image was okay but attempting to customize anything about it was
  a complete waste of time. It doesn't understand simple re[quests]"*.
- **The free tier is one render, then the wall.** `com.sictec.infotech.design.sense` — *"You only receive
  ONE free rendering; after that, you have to pay."* `com.pixery.homeai`, 2.8★, 10K+ — *"beware, on top
  of the payment just to get in, they also charge for generating images"*.
- **The trial bills.** `interior.home.design.ai`, 10M+ — *"it says free 3 day trial, you have to enable it
  which I did, it told me it wouldn't charge it, it immed[iately charged]"*. `ai.home.room.designer`,
  100K+ — *"User beware. … I did 3 day free trial cancelled timely and still am billed every 2 weeks for
  $10.99."*
- **The user's own photo comes back wrong.** `com.sictec.infotech.design.sense` — *"My original photo was
  turned sideways, making my rendering have a completely differe[nt]"* — which is a promise about EXIF
  orientation, not a complaint about the model.

### Incumbents, as evidence rather than as a warning list

`MEASURED` — `.kiln/scratch/recon-2026-10-16/enrich.md`, "interior design ai", top 6 of 30 cards, read
2026-10-16. The money on this shelf is the demand reading; the weak mid-tier is the pocket.

| # | App | Installs | Rating | Reviews (exact) | Ads | IAP |
|---:|---|---:|---:|---:|---|---|
| 1 | AI Remodel — Interior Design (`pro.airemodel.android`, PulseApps Solutions OÜ) | 5M+ | 4.6 | 184 307 | no | yes |
| 2 | AI Home Design: Interior DecAI (`ai.interior.design.home.renovation.app`, Smart AI DEV) | 10M+ | 4.6 | 312 423 | **yes** | yes |
| 3 | Remodel AI — Home Design (`ai.reimage.remodel`, ReImage AI Inc.) | 1M+ | 4.4 | 30 954 | no | yes |
| 4 | **Home AI — AI Interior Design (`interior.home.design.ai`, HubX)** | **10M+** | **4.3** | **994 289** | no | yes |
| 5 | Planner 5D: AI Home Design (`com.planner5d.planner5d`) | 50M+ | 4.4 | 586 000 | no | yes |
| 6 | HomeGPT: AI Interior Design (`com.home.designer.aignite`, Fire Elite Team) | 10K+ | 4.4 | 151 | no | yes |

**The pocket, measured on the same shelf** (`MEASURED` — the complaints file's own screen lines, ratings
and install buckets as printed): `com.pixery.homeai` **2.8★** / 10K+, `com.coohom.capp` 3.1★ / 500K+,
`com.sictec.infotech.design.sense` 3.1★ / 100K+, `com.autodesk.homestyler` **3.3★** / 10M+,
`com.virtualminds.airoomdesign` 3.7★ / 5K+, `com.wonder.idealhouse.app` 3.8★ / 100K+,
`com.icandesignapp.all` 4.0★ / 10M+. Note what that list is: **three of the seven are 10M+-install apps
rated 3.3★ or below** — large install bases with bad reviews, which is the strongest shape of pocket
this factory's own screening method names (`docs/niche-selection.md` §3).

**Boundary against our own catalogue — the same job test.** Our catalogue's taken job that is nearest is
`plinth-product-photos`: *"Turn a seller's own product photos into per-marketplace listing cards: server
cut-out, original-pixel composition, marketplace rule linter, export with provenance"*
(`registry/apps.json`, `MEASURED`). One sentence cannot describe both, on all three axes the rule asks
for:

| Axis | `plinth-product-photos` | `roomlumen` |
|---|---|---|
| **Who** | a marketplace seller running a shop | a householder, renter or stager deciding about their own room |
| **Artifact** | a listing card of a **product**, exported to per-marketplace sizes | a picture of **a room the user lives in**, kept as a version history of one change each |
| **Trigger** | a listing drop, one per item, hundreds per month | one room, one decision, a burst of 5–20 iterations then nothing for months |
| **Ops** | O-02 cut-out + O-07 scene + compose/lint | **O-10 instruction edit + O-08 relight**, the two ops Plinth's plan does not lead with |

Two further neighbours, disclosed rather than left for a reader to find: **`room-gauge`** is a *queued,
unbuilt* proposal in this repository's own ideation queue whose job is measuring a room and its materials
— a measurement job with no generation and no photo of a finished room, so one sentence cannot describe
it and this one together either; and **`lumintra-light-planner`** is a *killed* slot (`registry/apps.json`,
killed 2026-10-10) whose job was a night-sky/light planner for astronomy with no image generation and no
room. Neither is the same job as this one. **This is the one row where the run's honesty matters most:**
the money owner's own ranking already puts this shelf at order 3 as **C2 — Interior & home restyle**
(`docs/research/2026-10-16-money-ecosystem-ranking.md` §4, `MEASURED`). This recon **independently
converges with it** from fresh market measurements rather than discovering a new shelf, and the owner
should read it that way.

---

## Revenue arithmetic, with the division shown

### The division, stated once

`MEASURED` — the inputs are the money owner's own priced model, `MEASURED` in
`docs/research/2026-10-16-money-ecosystem-ranking.md` §1–§2 and
`docs/research/2026-10-16-generation-capability-inventory.md` §1:

```text
net proceeds per $1.00 charged   = (1 − 0.15 Play fee)(1 − 0.02 refund)(1 − 0.02 FX)(1 − 0.01 fraud)
                                 = 0.8081766   ≈ $0.8082
K at a 60 % target margin        = 1 / (0.8081766 × (1 − 0.60)) = 3.0933833
the factory's credit ceiling     = $0.025 per credit  (the dearest enabled operation)
minimum honest price per credit  = $0.025 × 3.0933833 = $0.0773346

net proceeds per sale = gross × 0.8082
model cost per sale   = credits charged × cost per credit
net per sale          = net proceeds − model cost
sales / month for $1,000 = 1000 ÷ net per sale
```

**The operations this job calls, and what one render costs the factory** (`MEASURED` — capability
inventory §1; `apps/2026-10-15-plinth-product-photos/server/src/registry.ts` is the registry that
declares them, cited there at `registry.ts:48-63`):

| Op | Endpoint | Credits | Provider cost | This job uses it for |
|---|---|---:|---:|---|
| **O-10** | `fal-ai/flux-pro/kontext` | 2 | **$0.040** | the change itself: *"repaint this wall deep green, keep everything else"* |
| **O-08** | `fal-ai/iclight-v2` | 5 | **$0.104858** | the relight pass — the control no enriched leader sells |
| O-09 | `fal-ai/bria/expand` | 2 | $0.040 | changing the frame's crop (a wider view of the same room) |
| O-12 | `fal-ai/bria/eraser` | 2 | $0.040 | taking one object out |
| O-01 | `fal-ai/any-llm/vision` | 0 | $0.010 | reading the room (absorbed, never charged) |

So: **the cheapest possible render is 2 credits / $0.040, and a changed-and-relit render is 7 credits /
$0.144858.** Exactly one operation in the whole registry is cheaper than 2 credits — the 1-credit cut-out
(`O-02`, $0.018) — and this job does not use it.

### The ladder

Prices are ours to set; the cost is not. Every row below is divided at the **$0.025-per-credit ceiling**,
which is the worst case, so a better operation mix only improves it.

| Pack | Credits | Gross | Net proceeds (× 0.8082) | Model cost (ceiling) | **Net per sale** | **Sales/mo for $1,000** |
|---|---:|---:|---:|---:|---:|---:|
| Starter | 40 | $4.99 | $4.03 | $1.00 | **$3.03** | **330** |
| Studio | 120 | $12.99 | $10.50 | $3.00 | **$7.50** | **134** |
| Pro | 350 | $34.99 | $28.28 | $8.75 | **$19.53** | **52** |
| HD export unlock (one-time, no model cost) | — | $9.99 | $8.07 | $0.00 | **$8.07** | **124** |

Price per credit: **$0.1248** (Starter), **$0.1083** (Studio), **$0.09997** (Pro) — all above the
$0.0773346 floor, at ratios of **4.99× / 4.33× / 4.00×** against the $0.025 ceiling where the target
needs 3.09×.

One reading note so the two files cannot look like they disagree: the plan's own `priceUsd` field carries
a single number for the machine (`docs/ideation/plans/roomlumen.economics.json`, `priceUsd: 12.99` — the
Studio pack, the middle of the ladder and the one the day-one path expects a user to take), while the
ladder above is the full published set. `slot-economics.mjs` requires one entry price; the pack table is
what the paywall shows.

**Read against the anchor.** The incumbent bills **$10.99 every two weeks ≈ $23.81/month**. Our Starter
pack is $4.99 **once** for 20 restyle renders; the Pro pack is $34.99 **once** for 175 restyle renders (or
50 restyled-and-relit renders) — **12 % of one year** of the subscription we are being compared against,
for a purchase that does not renew.

**What the user gets free, and what it costs us** (`MEASURED` —
`docs/research/2026-10-16-money-ecosystem-ranking.md` §5 carries the starter grant and both server-side
caps, citing the existing server's own `config.ts:73-74`; recon read the research document and the market,
never a slot tree): a **starter grant of 6 credits per
device** — **three restyle renders at 2 credits each, or one relight-only pass at 5 credits**, but not a
changed-and-relit render, because that costs 7 credits and the grant does not reach it — so nobody is
asked for money before they have seen their own room changed once. 6 credits worst case =
**$0.15 per install claimed** at the $0.025-per-credit ceiling ($0.12 if all three renders are `O-10`),
bounded server-side by `starterDailyBudgetMicroUsd` ($10/day) and `dailySpendCapMicroUsd` ($25/day) — so a
launch spike cannot turn the free tier into an unbounded bill. The first server-cost operation after the
grant is where the paywall fires, with the credit cost shown **before** the tap.

### A correction to the sibling money plan, measured

The money owner's **C2** table prices its starter as *"30 renders | $2.99 | net $2.42 | model cost
$0.63"*, i.e. **$0.021 per render — one credit**. There is no one-credit operation in this job: the
cheapest enabled restyle op is `O-10` at **2 credits / $0.040**, and a relit render is 7 credits /
$0.104858 + $0.040. Re-divide that row:

```text
30 renders at the cheapest op (2 credits each)  = 60 credits = $1.20 cost
net per sale = $2.99 × 0.8082 − $1.20 = $2.42 − $1.20 = $1.22   → 1000/1.22 = 820 sales/mo

30 renders WITH the relight pass (7 credits each) = 210 credits = $4.35 cost
net per sale = $2.42 − $4.35 = −$1.93   → the pack loses money on every sale
```

The ladder above is priced per credit precisely because of this: at 2 credits a render the shape is
sound, and at 7 it inverts. **Do not ship a render pack whose credit price assumes a one-credit
operation** — that is the failure mode this row records, and it is arithmetic over the factory's own
priced registry, not a market opinion.

### Why this shape, and the ones it rejects

| Archetype | Verdict | The revenue fact that decided it |
|---|---|---|
| `purchase_led_repeat_sales` | **chosen** | Every render costs real money at the moment it is served (`O-10` $0.040, `O-08` $0.104858). A consumable pack is the only shape where the money for a unit is collected *before* the cost of serving it, and this job produces **iteration volume** naturally: the measured user behaviour is trying 20 photos and 10 variants (`interior.home.design.ai` review: *"I tried around 20 different photos from different rooms, different angles"*). |
| `purchase_led_single_unlock` | rejected | One price against a recurring per-use cost is an unbounded loss on a heavy user — quantified on this very shelf in the money ranking §6: 600 credits costs the factory $15.00 in provider fees, and the unlock's net is collected once. The unlock survives only as the *convenience* half ($9.99, zero model cost), never as the thing that funds calls. |
| `ads_led_utility` | **rejected — the arithmetic kills it outright** | `ON RECORD` (money ranking §2.1): a rewarded-video-centric app earns ≈ **$0.05 per DAU per day**. Price the free tier: 1 000 DAU × one render/day at the *cheapest* restyle op ($0.040) = **$1 200/month** of provider cost against ≈ $1 000/month of ad revenue. **An ads-led app that gives away model operations loses money at every scale.** This is also why the photo-enhancer sub-shelf is refused. |
| `purchase_only_no_ads` | rejected | A price with no discovery (`docs/monetization-strategy.md` §4) and no way to fund a per-use cost. Also, a consumer does not pay up front for an app they have not seen work on their own room. |

**Subscriptions: not considered.** `subscription` is absent from `ARCHETYPES`
(`scripts/lib/slot-economics.mjs:28-33`, `MEASURED`), so the shape this shelf's leaders monetize with —
the one the $10.99/2-weeks anchor describes — **cannot be expressed in a plan at all.** That is not a
preference here; it is why the anchor above is an anchor and not a price list.

---

## The name, derived rather than invented

The record is `docs/ideation/plans/roomlumen.naming.json`. Six candidates across five archetypes —
`compound`, `borrowed_foreign_word`, `coined`, `evocative` and the `descriptive` negative control, which
is one of the six rather than an extra — scored 0–5 on the seven weighted dimensions of
`docs/app-naming.md` (20/15/15/15/15/10/10), the chosen at **≥ 70/100 with no dimension below 2.5**.
Five alternatives are recorded as rejected with their reasons.

| Candidate | Archetype | Score | Why it won or lost |
|---|---|---:|---|
| **Roomlumen: AI Room Design** | `compound` | **81.0** | **Chosen.** `room` is the subject and `lumen` the unit of light — the wedge's own two halves — and `AI Room Design` is a phrase measured as a real query (`MEASURED` — `scan-market.md`, `ai room design`). |
| Svetlo: AI Room Design | `borrowed_foreign_word` | 76.5 | Eliminated **by the rejection list, not by taste**: heard once by someone who does not know Slavic orthography it has more than one plausible rendering (Svetlo / Svetlow), which is item 11, and its radio dimension falls below the 2.5 floor. |
| Roomlume: AI Room Design | `compound` | 73.5 | Truncating *lumen* to *lume* is invisible when heard, so a listener may type the longer form of our own name; it also sits closer to the measured `Roomly / Roomellow / Roomlify / Roomsy` cluster. |
| Onewall: AI Room Design | `evocative` | 73.0 | Collides: `com.venn.onewall` ("OneWall Communities") is live on Play, and its uniqueness dimension is 2.0 — below the floor, which is itself a reason to return to the field. |
| Keeplight: AI Room Design | `coined` | 75.0 | Ties the name to the wedge (*keep* the room, change the *light*) but its collision check was **not run in this session**, so it cannot outrank a name that was measured. Rejected on process, not on art. |
| AI Interior Design *(negative control)* | `descriptive` | 64.0 | The bare category head term every incumbent above already owns. It scores well on searchability and terribly on distinctiveness and uniqueness, and it loses to every serious candidate — which is what a negative control is for. |

**Collision check, and what it is not.** `MEASURED` — `node scripts/scan-titles.mjs --concurrency 1
--query "roomlumen" --query "roomlight" --query "lumenroom"`, output at
`.kiln/scratch/recon-2026-10-16/scan-titles-naming.md`: **verdict `dead-zone`, 30 cards visible, 0 of
them carrying the token**, while the two neighbouring strings both resolve into crowded tokens
(`roomlight` → `fortress` over 20 cards, surfacing **Lightroom** and **Photoroom**, 4.5★ and 4.7★;
`lumenroom` → `fortress` over 8 cards, surfacing Lightroom, Photoroom and "Lumen Loom"). An earlier run
of the same query in this session printed 8 visible cards rather than 30 — the visible-card count varies
between loads and the token reading was identical in both, and the fresh number is the one recorded here.

Two things this reading does **not** say, both stated rather than left for a reader to find. First,
`dead-zone` is the sensor's word for the share of that result page matching a relevance regex; it means
*no app carries this exact string*, not *the market is dead* — the market for the **job** is measured
separately above and is large and paying. Second, **the word `lumen` inside the carrier is not
unclaimed**: the same result page carries Strong Lumen (`com.stronglumen.mlc`), Lumenate: Explore & Relax
(`com.lumenate.lumenateaa`, 4.5★), Lux Light Meter - Lumen (`com.spiderlingz.lumen`) and Lumen Loom. The
exact compound is free and its second half is common, which is a fact about distinguishability an owner
should weigh rather than a finding that the name is taken. This is a Play-search reading only. It is
**not** trademark clearance, and no register (TMview, USPTO, EUIPO, WIPO) was queried in this session.

**Localisation.** `Roomlumen: AI Room Design` is **25 characters** in `en-US` and
`Roomlumen: Дизайн комнат` is **24** in `ru-RU` — both inside Play's 30-character cap with **5 and 6
characters of slack** (`MEASURED` — `node -e` on both literal strings, 2026-10-16; the first draft of
this section said 23 and 7, which was a miscount, and it is corrected here rather than quietly). The carrier stays Latin in both locales, as `docs/app-naming.md` allows for a
`compound` carrier; neither string is a machine translation.

**The name is never the category.** `AI Interior Design` was generated as the negative control, scored
64, and loses to every candidate — the `Calculator` defect this factory paid for once already.

---

## Device capabilities

Each capability is answered `used` or `not used`; a `used` line names the API, the permission it
carries, and what the job loses without it.
Where the platform offers a **free, local** door with no runtime permission, that door is preferred.

- camera: used — CameraX `ImageCapture` with `CAMERA` lets the user photograph the room now, framed on a preview; `ACTION_IMAGE_CAPTURE` / `ActivityResultContracts.TakePicture` offers the system-camera door to the same photo. Without capture, a user with no room photo in their gallery cannot start until they leave the app and take one. The intent door is intended to avoid forcing an in-app camera permission on users who do not need it. `UNVERIFIED`: the exact permission consequence of *declaring* `CAMERA` alongside the intent is decided by reading the platform guide for the chosen contract, not by this document.
- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia`, with **no runtime permission**, is the primary way a room photo enters the app. Without it the user loses the permission-free gallery door for the one image the job needs and must use the camera or a saved document instead; requesting `READ_MEDIA_IMAGES` for that single image is not the fallback. **This is the preferred door.**
- media playback or capture: not used — the room and its rendered versions are still images; still-photo capture is declared under camera, and the job plays or records neither audio nor video.
- microphone: not used — the user steers a room edit through controls and instructions, not through audio recording; `RECORD_AUDIO` buys nothing for this job.
- location: not used — a change to the user's room photo does not depend on the phone's coordinates; `ACCESS_FINE_LOCATION` is refused.
- motion sensors: not used — the room is framed in a photo and edited on screen, not measured or controlled by moving the phone.
- contacts / calendar: not used — showing a render to a partner or a builder uses the share sheet, not the user's address book or schedule.
- notifications / reminder: not used — a render is a 30-second action, not a scheduled event; `POST_NOTIFICATIONS` is refused, and a reminder surface would be an unused feature charging maintenance rent.
- home-screen widget: not used — comparing and choosing room versions happens in the app over the room photo, not in a home-screen surface.
- share / export: used — `ACTION_SEND` with a `FileProvider` URI, plus `ACTION_CREATE_DOCUMENT`, needs no runtime permission and provides the `data_out` handoff into a message, a chat, a note to a partner or a builder, or a saved full-resolution render. Without it the user can look at the picture but cannot show anyone or keep the chosen export outside the app — the exact partial-flow defect `.cursor/rules/app-depth.mdc` records.
- Bluetooth / USB: not used — the job pairs with no device and does not tether or print directly; the finished render leaves through sharing or a saved document.
- files / SAF: used — `ACTION_OPEN_DOCUMENT` brings in a saved room photo, as the `bring-your-own-room` promise requires, and `ACTION_CREATE_DOCUMENT` saves the chosen render at full resolution to a folder the user chooses, with no runtime storage permission. Without it the saved-document intake door is lost and the user cannot file the chosen full-resolution version outside the app. `WRITE_EXTERNAL_STORAGE` is refused: SAF supersedes it, so asking for it would be a defect, not a fallback.

Intake also reads EXIF orientation with `ExifInterface` (no permission), rotating the user's photo
correctly before it is sent. Lose it and we ship the measured defect `com.sictec.infotech.design.sense`
carries in a 1★ review: *"My original photo was turned sideways, making my rendering have a completely
differe[nt]"*. Device-side downscale / crop before upload needs no permission and keeps the upload cheap
and the room's pixels honest; lose it and every render pays a bigger provider bill on a 12 MP phone
photo. Clipboard paste remains a convenience photo-intake door with no permission, the least important
one here.

Local storage of versions in `filesDir` needs no permission and keeps the version history of one change
each — the product's own artifact. Lose it and every render is disposable, which is a different (worse)
product. `INTERNET` is a normal permission with no prompt: every render is a server-side model call,
so without it there is no generation and no app. Play Billing (`billing` capability) carries
`com.android.vending.BILLING`, with no prompt, and collects the money and grants the credits
**server-side, after Play confirms the purchase** (`MEASURED` — the money ranking §2.2 describes the
confirmation path in the existing server); lose it and the plan collects nothing. `AD_ID` is refused:
no ads, no location, no third-party SDK that needs either. A runtime permission that buys nothing is a
Play-compliance and trust surface, not a money line.

The photo and the camera are named in the owner's own instruction that the phone be used fully
(*«есть камеры, есть фотопикер, есть галерея, картинки, музыка»*, recorded 2026-10-02,
`.cursor/rules/ideation-gate.mdc`) — with the permission arithmetic above rather than a blanket
permission policy.

---

## The likeness and consent surface — a product requirement, not a legal afterthought

This shelf's reputational and policy risk is AI-generated **likenesses**, and this proposal's answer is
structural rather than a disclaimer:

- **The app renders rooms, not people.** It generates no faces, no bodies and no identity. The provider
  operation set it enables (`O-10` instruction edit, `O-08` relight, `O-09` outpaint, `O-12` erase)
  contains **no identity-preservation or face operation at all** (`MEASURED` — capability inventory §1:
  there is no such endpoint in the registry).
- **Instructions that name a person are refused at intake.** A prompt naming a person ("add a woman",
  "my wife's face", "put me in the room") is rejected before a credit is spent, with the reason shown.
- **A photo containing people is handled explicitly.** Intake asks the user to photograph the empty room;
  the app does not attempt face detection or removal. This is a named product decision so that it is not
  discovered at review time.
- **The AI declaration is truthful and complete** — the listing says the renders are AI-generated, and the
  in-app copy says it before the first render. `AGENTS.md` § *Play compliance floor* already requires
  that ("AI-content declarations are truthful and complete", `ON RECORD`).
- **Data safety must declare the photo as transmitted off the device**, because it is: each render sends
  the room image to the model provider. `AGENTS.md` defines *collect* as "transmitting data off the
  device"; this slot therefore does **not** get to declare "no data collected" the way an offline slot
  does. The privacy policy is generated from those declarations and hosted by the factory
  (`.cursor/rules/privacy-policy.mdc`), and retention of the uploaded image is stated there.
- **No invented provenance.** The app does not claim the image is a photograph of the future room; it
  labels it a visualisation, which is also what makes the staging job honest to a buyer.

---

## Refused candidates

Each row names a **revenue** or **capability** reason. "It already exists" appears nowhere in this table
as a reason, because it is not one.

| Candidate | Refused because |
|---|---|
| **AI headshot / portrait / avatar** | **Capability first, revenue second.** The job's core act is *identity-preserving* placement of a person into a scene, and the priced inventory contains **no identity/face operation at all** (`MEASURED` — capability inventory §1: the enabled set is cut-out, scene, relight, outpaint, instruction edit, erase, try-on; the text-to-image op `O-07 flux/dev` does not preserve a face). Revenue reason as well: the shelf's reachable pocket is measured thin — the enriched set's weak apps are `ai.aragon.dashboard` **3.1★ / 12 reviews / 1K+** and `com.gizastudios.headshotplus` 4.5★ / **103 reviews** / 1K+ — against `com.prequelapp.aistudio` at 5M+ / 4.5★ / **174 437** reviews giving the first result free, and **6/6 enriched apps declare IAP with 0/6 declaring ads** (`MEASURED` — `enrich.md`, "ai headshot"). A subscription is what that shelf pays with, and a subscription is forbidden to us. |
| **Pet portrait** | **Revenue.** The reachable demand is measured two to three orders of magnitude smaller than the survivor's: the enriched set's largest install bucket is **10K+** (`ai.dreampets.dreampets`, 4.2★, 483 reviews; `com.curtsy…`-class leaders are 5K+ and 1K+), against 5M–50M for interior restyle (`MEASURED` — `enrich.md`, "pet portrait"). At the survivor's own honest division (134 Studio packs/month for $1 000), that install base cannot supply the transactions; 3/6 also declare ads, so part of the shelf monetizes attention rather than purchase. |
| **Old-photo restore / colourise** | **Capability, then arithmetic.** The operation the job needs — upscaling — is `O-11 clarity-upscaler`, and it is **disabled** in the registry; its declared 1-credit price is arithmetically impossible: **2.58×** at 1 MP and **0.63×** at 2K against the 3.09× floor, i.e. it *loses $0.046 on every 2K call* (`MEASURED` — money ranking §7 W2). A restoration app priced like the rest of the catalogue loses money on every restore. Note the trap this is: the screen says `candidate` (12–15 weak of 25–28 rated) and the shelf is huge — **the sensor said yes and the arithmetic says no.** |
| **Photo cleanup / enhancement** | **Arithmetic.** **6/6 enriched apps declare ads** and the leaders give the job away free — Remini 500M+, UpFoto 100M+ (`ON RECORD` — money ranking §3). An ads-led shape must pay $0.040–$0.105 of model cost per action against ≈ $0.05 per DAU per day of ad revenue, measured above as **$1 200/month of cost against $1 000/month of revenue at one action per user per day.** We would be paying real money to compete with free. |
| **Greeting cards / invitations / announcements** | **Revenue, and duplication inside our own plan.** The monetization mix is fine (6/6 IAP, 1/6 ads) but the incumbents are free-with-IAP at 10M+ and **4.5–4.9★** — Invitation Maker 10M+ 4.9★ / 192 000 reviews, Canva 500M+ (`ON RECORD` — money ranking §3) — and the artifact is a *template*, not a generation, so there is no per-unit model cost for a consumable pack to fund and no wedge that is a difference in functionality we can build. It is also the same job as the money owner's own **C4 — Keepsake & photo-gift maker** (`docs/research/2026-10-16-money-ecosystem-ranking.md` §4), and shipping two of our own proposals that one sentence describes is the near-duplicate evidence Play's Repetitive Content policy acts on. Pick one; this run does not propose the second. |
| **Personalised wall art / posters** | **Revenue, and the same C4 collision.** `custom poster` screened **`fortress`** — 13 of 19 rated incumbents at ≥ 4.5★ with only 3 weak (`MEASURED` — `scan-market.md`, `ai-keepsake`) — and `personalised wall art` is `mixed` on the same shelf. Add that the buyer's trigger is a gift occasion (one purchase, no repeat) and the same C4 duplication applies. |

**What is *not* refused, and why the survivor is not refused for being crowded.** The interior sub-shelf
is the most crowded of the seven and the only one that survives: six enriched leaders at 4.3–4.6★, four
of them 5M+ installs or more, **all six monetizing by purchase**, and a measured mid-tier of 2.8★–3.8★
apps including three 10M+ installs rated 3.3★ or below. Occupied is evidence of demand. The surviving
wedge is a difference in **functionality** (one-variable control, a steered relight) and in
**commercial shape** (per render, never per week), and both are named in the incumbents' own users'
words above.

**The survivor's own weakest row, said out loud.** The money owner's ranking already recorded it: *"it
ranks second only because its competition is free"* (`docs/research/2026-10-16-money-ecosystem-ranking.md`
§4, C2). The incumbents give one to three renders away before the wall, and the free tier is the shelf's
real price floor. Our answer is the wedge rather than the price — but if the shelf's users prove unwilling
to pay anything at all for control, this proposal is wrong, and the exact reading that would show it is
named at the end of this document.

---

## Kill criteria — three finite numbers

Declared **before** the slot is built, read against a recorded Play Console reading in
`apps/roomlumen/store/outcomes.json` by `node scripts/check-outcomes.mjs --slot roomlumen`.

| Criterion | Value | Why this number |
|---|---:|---|
| `windowDays` | **90** | The billing surface, the pack ladder and one retention reading all exist inside a quarter; a longer window measures the market rather than the slot. |
| `minInstalls` | **2000** | The shelf's leaders are 5M–50M installs, so 2 000 is ~0.04 % of the nearest one — below it there is no reading at all (Play needs volume for rank), above it the pack mix can be judged. |
| `minRevenueUsd` | **300** | ≈ 10 % of the $1 000/month target sustained across the window. At the Pro pack's net ($19.53) that is ~15 packs in three months; below it the shape is not even starting. |

`MISSED` on any row is a kill: `node scripts/kiln-status.mjs --kill roomlumen --reason "<why>"`, with the
numbers in the reason. A declared criterion that cannot be missed is not a criterion, which is why
`check-economics.mjs` refuses a zero.

---

## Build estimate

**ESTIMATE, and explicitly never a criterion:** **≈ 180–260 hours of factory time**, assuming the
existing fal-backed credit server is extended for this job rather than built again — intake and version
state, the one-variable control surface, the instruction-edit and relight pipeline, the pack ladder and
credit-cost disclosure before every action, the export and share handoffs, the settings surface, and the
multilingual resource layer.

Per the owner's instruction recorded 2026-10-08, build time is **information for the owner and never a
ranking, refusal, narrowing or kill criterion** (`.cursor/rules/ideation-gate.mdc` § *Build cost is
information, never a criterion*; `AGENTS.md`). If the arithmetic above is right, the factory spends the
time; if it is wrong, no estimate would have saved it.

---

## Promise contract

`docs/ideation/plans/roomlumen.differentiators.json`. Every promise starts `deferred`; the gate refuses a
slot while any promise is still deferred. The job's three stages are covered —
`before` = bring your own room in, `during` = change one thing and keep the rest, `after` = get the
version you chose out of the phone — and the **connective floor** is met twice over by `data_in`
(`PickVisualMedia` / `IMAGE_CAPTURE`) and `data_out` (`ACTION_SEND` / `FileProvider` /
`ACTION_CREATE_DOCUMENT`), which is the half `apps/2026-10-01-lawn-round-log` was measured missing.

| Claim | Stage | Family | Weight |
|---|---|---|---|
| `bring-your-own-room` | before | `data_in` | core |
| `one-change-only` | during | `core_operation` | core |
| `light-you-can-steer` | during | `core_operation` | core |
| `keep-the-versions` | after | `durability` | core |
| `handoff-the-chosen-render` | after | `data_out` | core |
| `no-renewing-charge` | after | `trust` | supporting |
| `find-a-past-version` | after | `find` | supporting |

The claim that carries the differentiator is `one-change-only`, and its comparison is a verbatim
complaint from an incumbent's own user, not a design opinion.

**The four token-bearing claims also carry a `plannedProof`.** While a promise is `deferred`, a real
`proof` is refused by the schema — `scripts/lib/slot-differentiators.mjs:576-581`, on the reasoning that
*a promise that did not ship has nothing to prove, and a stale proof reads as delivery*. So the artifact
and the family token each connective claim must be proved with are recorded in a separate
forward-declaration field: `PickVisualMedia` for `bring-your-own-room` (`data_in`), `ACTION_SEND` and
`FileProvider` for `handoff-the-chosen-render` (`data_out`), `SavedStateHandle` for `keep-the-versions`
(`durability`) and `SearchBar` for `find-a-past-version` (`find`). **The honest limit, stated rather than
implied: nothing reads `plannedProof` today.** The gate reads `proof`, at `shipped` time, and it is the
thing that enforces the connective floor; `plannedProof` is there so the handoff's token is fixed before
the code exists, which is the failure `.cursor/rules/app-depth.mdc` records — `lawn-round-log` built a CSV
with no way to get it off the phone because no promise ever named the handoff.

---

## The reading that would flip this proposal to a refusal

The load-bearing claim is **"this shelf pays by purchase"** — six enriched leaders, six declaring IAP,
one declaring ads — because everything above rests on it. That reading came from the query
`interior design ai`. If it is an artefact of that query phrasing, the purchase-led shape has no anchor
on this shelf and the proposal is refused **on revenue grounds**: no price anyone pays.

**The exact command that decides it:**

```powershell
node scripts/enrich-market.mjs --concurrency 1 --top 8 --query "restyle my room" --query "virtual staging ai" --query "room makeover ai"
```

**The observation that flips the verdict:** if **5 or more of the 8 enriched apps per query declare
`ads` and 2 or fewer declare `iap`**, the consumer-phrased pocket monetizes attention rather than
purchase, and an ads-led shape is arithmetic-impossible against a $0.040–$0.105 per-render cost (the
division in §Revenue above). That is a refusal, and it would take the whole shelf with it.

**The second reading that would break the ladder rather than the wedge:**
`./gradlew :app:dependencyInsight` does not apply here — the right check is a fresh provider price read.
Recorded provider costs are the factory's own values, not live page reads (`UNVERIFIED` — capability
inventory, final table: *"Every recorded `costMicroUsd` is the factory's own value, not a live fal price
read today"*). If `O-10` or `O-08` rose above the **$0.025-per-credit ceiling**, every pack price above
would have to be re-derived or the slot refused; the decider is re-reading each endpoint's page at
`fal.ai/models/<endpointId>`.

---

## Claim ledger

```text
CLAIM | LABEL | EVIDENCE
18 queries screened on this shelf return 3 candidate / 14 mixed / 1 fortress / 0 dead-zone / 0 errored | MEASURED | node scripts/scan-market.mjs --concurrency 1 <18 queries>; .kiln/scratch/recon-2026-10-16/scan-market.md
Interior sub-shelf: 6 of 6 enriched leaders declare IAP, 1 of 6 declares ads | MEASURED | node scripts/enrich-market.mjs --concurrency 1 --top 6 --query "interior design ai"; .kiln/scratch/recon-2026-10-16/enrich.md ("ads — declared: 1, declared absent: 5"; "in-app purchases — declared: 6, declared absent: 0")
The interior shelf's leader is 10M+ installs, 4.3 stars, 994 289 reviews | MEASURED | enrich.md, row 4 interior.home.design.ai (HubX)
Three 10M+-install incumbents on this shelf are rated 3.3 stars or below | MEASURED | complaints-home-pets.md screen lines: autodesk.homestyler 3.3 / 10M+, home design ai query; com.coohom.capp 3.1 / 500K+; com.sictec.infotech.design.sense 3.1 / 100K+
Paywall complaints are 43 % of critical reviews (interior design ai) and 54 % (home design ai) | MEASURED | complaints-home-pets.md: "6 paywall (43% of critical)" and "7 paywall (54% of critical)"
The incumbent's subscription anchor is $10.99 billed every two weeks | MEASURED | complaints-home-pets.md, 1-star review on ai.home.room.designer (4.2 stars, 100K+): "still am billed every 2 weeks for $10.99"
An incumbent's user asks to choose what changes and cannot | MEASURED | complaints-home-pets.md, com.sictec.infotech.design.sense (3.1 stars, 100K+): "the app needs to ask if the doors windows and openings are to be changed which is 30% of the renovations i do as an interior desi[gner]"
An incumbent's user reports the photo came back rotated | MEASURED | complaints-home-pets.md, com.sictec.infotech.design.sense: "My original photo was turned sideways, making my rendering have a completely differe[nt]"
A 3-day free trial that charged on enable is a measured complaint on the 10M+ leader | MEASURED | complaints-home-pets.md, interior.home.design.ai (4.3 stars, 10M+): "it says free 3 day trial, you have to enable it which I did, it told me it wouldn't charge it, it immed[iately charged]"
The pet sub-shelf's largest enriched install bucket is 10K+ | MEASURED | enrich.md, "pet portrait" summary: "largest install bucket seen: 10K+"
The headshot sub-shelf's weak pocket is 3.1 stars / 12 reviews / 1K+ | MEASURED | enrich.md, "ai headshot" row 5 ai.aragon.dashboard
The recipe catalogue has no identity/face operation in its enabled set | MEASURED | docs/research/2026-10-16-generation-capability-inventory.md §1 table, O-01/O-02/O-07/O-08/O-09/O-10/O-12/O-14
O-10 costs $0.040 for 2 credits and O-08 costs $0.104858 for 5 credits | MEASURED | docs/research/2026-10-16-money-ecosystem-ranking.md §1 table row O-10 and row O-08; registry.ts:53-63 as cited there
The $0.025-per-credit ceiling is the dearest enabled operation | MEASURED | docs/research/2026-10-16-money-ecosystem-ranking.md §1 ("The most expensive enabled operation costs $0.025 per credit")
K at a 60 % target margin is 3.0933833 and the net factor is 0.8081766 | MEASURED | docs/research/2026-10-16-money-ecosystem-ranking.md §2 ("= 1 / (0.8081766 x (1 - 0.60)) = 3.0933833")
O-11 clarity-upscaler is disabled and its 1-credit price is 2.58x at 1 MP and 0.63x at 2K | MEASURED | docs/research/2026-10-16-money-ecosystem-ranking.md §7 W2; capability inventory §1 row O-11
An ads-led app giving away model operations loses money at every scale | ON RECORD | docs/research/2026-10-16-money-ecosystem-ranking.md §6, ads_led_utility row, quoting §2.1's ~$0.05 per DAU per day
Photo enhancers are 6 of 6 ads with free leaders | ON RECORD | docs/research/2026-10-16-money-ecosystem-ranking.md §3 shelf reading; §7 W1
Greeting-card incumbents are free-with-IAP at 10M+ and 4.9 stars | ON RECORD | docs/research/2026-10-16-money-ecosystem-ranking.md §3, shelf 4 row (Invitation Maker 10M+ 4.9 (192 000) / Canva 500M+)
`subscription` is absent from ARCHETYPES and cannot parse | MEASURED | scripts/lib/slot-economics.mjs:28-33
`custom poster` screens fortress (13/19 rated >= 4.5, 3 weak) | MEASURED | .kiln/scratch/recon-2026-10-16/scan-market.md, "custom poster" row
A Play search for "roomlumen" returns 0 apps carrying the token | MEASURED | node scripts/scan-titles.mjs --concurrency 1 --query "roomlumen" --query "roomlight" --query "lumenroom"; .kiln/scratch/recon-2026-10-16/scan-titles-naming.md -> roomlumen verdict=dead-zone, visible=30, 0 carrying the token (an earlier run of the same query in the same session printed visible=8; the visible-card count varies between loads and the token reading did not)
The word "lumen" inside the chosen carrier is NOT unclaimed | MEASURED | .kiln/scratch/recon-2026-10-16/scan-titles-naming.md: same result page carries Strong Lumen (com.stronglumen.mlc), Lumenate: Explore & Relax (com.lumenate.lumenateaa, 4.5), Lux Light Meter - Lumen (com.spiderlingz.lumen) and (via lumenroom) Lumen Loom
"roomlight" and "lumenroom" both resolve into Lightroom/Photoroom | MEASURED | .kiln/scratch/recon-2026-10-16/scan-titles-naming.md -> roomlight verdict=fortress, visible=20, includes com.adobe.lrmobile 4.5 and com.photoroom.app 4.7; lumenroom verdict=fortress, visible=8, includes the same two plus com.zmhtb.rgdx202 "Lumen Loom"
"Sunroom Designer Plus" and two "Afterglow" apps and "OneWall Communities" exist on Play | MEASURED | .kiln/scratch/recon-2026-10-16/scan-titles-naming-rejected.md: uk.co.rpssoftware.sunroom.hippoflow (3.0) and uk.co.rpssoftware.sunroom.ecco; now.shadow.afterglow (3.0), com.afterglow.guidanceApp; com.venn.onewall, com.onewallstreet.app, one4studio.wallpaper.one4wall (4.4)
The money owner's C2 ladder prices a render at one credit | MEASURED | docs/research/2026-10-16-money-ecosystem-ranking.md §4 C2 ("30 renders | $2.99 | net proceeds $2.42 | model cost $0.63") against its own per-use line "~ $0.021 / credit"
The nearest taken job in our catalogue is a seller listing-card tool | MEASURED | registry/apps.json entry plinth-product-photos, idea field
Each of 8 rows of the ladder divides to the stated net per sale and sales/month for $1,000 | MEASURED (arithmetic) | the division stated in §Revenue arithmetic; gross x 0.8082, minus credits x $0.025, then 1000 / net
Play's title cap is 30 characters and the frozen-name slack rule is >= 5 | MEASURED | scripts/lib/slot-naming.mjs TITLE_MAX = 30; docs/app-naming.md § "The budget" (">= 5 characters of slack")
The money owner's ranking already places this shelf at order 3 (C2) | MEASURED | docs/research/2026-10-16-money-ecosystem-ranking.md §4 C2 and §5 order table row 3
room-gauge is a queued unbuilt proposal and lumintra-light-planner is a killed slot | MEASURED | docs/ideation/plans/room-gauge.economics.json exists; registry/apps.json entry lumintra-light-planner, status killed, killedAt 2026-10-10
A consumer will pay a one-time pack for room control rather than a weekly charge | UNVERIFIED | no conversion or install reading on this shelf establishes it. Decided by: the outcomes reading in apps/roomlumen/store/outcomes.json after 90 days, or by the enrich command in "The reading that would flip this proposal"
Trademark clearance for "Roomlumen" | UNVERIFIED | no register was queried this session. Decided by: a class-9 search of TMview, USPTO, EUIPO and WIPO for Roomlumen and its close variants; a clean Play search is not clearance
The exact permission consequence of declaring CAMERA alongside a system-camera intent | UNVERIFIED | not read this session. Decided by: the platform guide for the chosen contract (PickVisualMedia / TakePicture / ACTION_IMAGE_CAPTURE)
Live fal prices still match the recorded costMicroUsd values | UNVERIFIED | capability inventory final table says every recorded cost is the factory's own value. Decided by: re-reading each endpoint page at fal.ai/models/<endpointId>
```

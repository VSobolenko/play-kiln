# Lane pricing worksheet — 2026-10-02

**What this is.** The money half of the 2026-10-02 recon. Three scouts are screening three lanes
concurrently — (A) authored learning/practice content, (B) generator & creative/maker tools,
(C) real-time niche instruments — and this file gives each lane its target, its price band and anchor,
the unit arithmetic with the division shown, the archetypes it fits and the ones it must refuse in
revenue terms, the failure mode to bet against, and the price it may not go below. It is written
**independently of the scouts**: it prices lanes, not candidates, and it does not read the scouts'
files, wait for them, or report on any slot.

**What this is not.** It is not a proposal, not a topic, not a winner and not a slot. It authorises
nothing: no build, no price, no capability, no purchase. A lane is not an idea, and a lane being
priced here is not a lane being chosen. The archetype and price that ship belong to a specific slot's
`apps/<slug>/economics.json`, and the owner's checkpoint 1 is what approves them.

**The factory is not the market.** Nothing under `apps/**` was opened to write this. From
`registry/apps.json` I read only the `id`/`name`/`idea` identity fields — the jobs already taken —
and I report on no slot's stage, quality or contents.

**Labels.** Every factual claim carries one of four, and the label is not decoration:

| Label | Means |
|---|---|
| MEASURED | I ran the command or read the file in this session; the command or `path:line` is given. |
| ON RECORD | A file in this repository states it; the path is given. |
| INFERRED | My reasoning from MEASURED or ON RECORD facts; the step is shown. |
| UNVERIFIED | Carried without evidence. Each is listed again in §7 with the command that would settle it. |

---

## 0. The target, quoted once

The factory's revenue target is **`$1,000 / month`**, quoted verbatim (ON RECORD,
`docs/monetization-strategy.md:135`):

> Illustrative order-of-magnitude implications for **$1,000/month** (≈ $33/day), derived from the
> benchmarks above

The same figure is reused at `docs/monetization-strategy.md:444` ("…can reach $1,000/month") and is
the target the companion worksheet `docs/ideation/2026-10-01-pricing-and-archetype-worksheet.md`
already carries (§2). Two honest qualifications travel with it, both recorded there and neither
corrected here:

- The file labels its own figures **"Illustrative order-of-magnitude … not forecasts for a specific
  app"** (`docs/monetization-strategy.md:149`). It is the factory's target by convention, not a
  measured result of this account.
- **Over 80 % of apps never cross `$1,000/month`** and the median active app makes **under
  `$50/month`** (ON RECORD — `docs/research/2026-09-26-teacher-gradebook-evidence.md:327-328`,
  sourced to Trend Apps). The target is a direction, not a base rate.

Throughout, `$1,000/month ≈ $33.33/day` and `$300/month = $10.00/day` on a 30-day month (the 30-day
month is an INFERRED convention, not a recorded one; a 30.4-day month moves a per-day figure ~1 %).

---

## 1. The shared arithmetic — the divisor, quoted not restated

### 1.1 The net multiplier for a local one-time purchase

Every lane below is priced as a **local** purchase: the paid unit is delivered on the device, no
server call is metered per unit, and `C_unit = 0`. Per
`docs/ideation/2026-10-01-pricing-and-archetype-worksheet.md` §1.2 (ON RECORD), a local one-time
unlock carries exactly three deductions and **excludes `F_fraud`**:

```
net multiplier (local) = (1 − F_billing)(1 − F_refund)(1 − F_fx)
                       = 0.85 × 0.98 × 0.98
                       = 0.81634                          → 81.63 % of the listed price
```

MEASURED (recomputed in this session: `node -e "…"` printed `net mult 0.81634`). The three rates are
the file's own **ASSUMPTION**s (`docs/monetization-strategy.md` §2.2), not measurements of this
account:

| Symbol | Value | Status |
|---|---:|---|
| `F_billing` | 0.15 | **ASSUMPTION.** Valid "only under the first-$1M annual tier and/or the new-install case"; above $1M, or for existing installs in the EEA/UK/US at standard rates, it rises to 20–30 % + the 5 % billing fee. |
| `F_refund` | 0.02 | **ASSUMPTION** — "decide per slot". |
| `F_fx` | 0.02 | **ASSUMPTION** — "decide per slot". |
| `F_fraud` | — | **Excluded**, because no unit is granted ahead of payment; a patched client that grants itself an unlock costs a **sale, not cash** (`docs/monetization-strategy.md` §3.7). INFERRED from the `LOCAL` definition. |

### 1.2 The net-per-sale table (local purchase)

MEASURED — computed in this session. Sales are rounded **up** to the next whole sale (a fraction of a
sale does not clear a floor). Installs are at the **2 %** install-to-purchase rate, which is
**UNVERIFIED** (§7).

| Price | Net multiplier | Net per sale | Sales/mo for **$1,000** | Sales/mo for **$300** | Installs/mo @ 2 % for $1,000 |
|---:|---:|---:|---:|---:|---:|
| $1.99 | 0.81634 | $1.6245 | **616** | 185 | 30,800 |
| $2.99 | 0.81634 | $2.4409 | **410** | 123 | 20,500 |
| $3.99 | 0.81634 | $3.2572 | **308** | 93 | 15,400 |
| $4.49 | 0.81634 | $3.6654 | **273** | 82 | 13,650 |
| $4.99 | 0.81634 | $4.0735 | **246** | 74 | 12,300 |
| $6.99 | 0.81634 | $5.7062 | **176** | 53 | 8,800 |
| $9.99 | 0.81634 | $8.1552 | **123** | 37 | 6,150 |
| $14.99 | 0.81634 | $12.2369 | **82** | 25 | 4,100 |

Invoice form for the anchors used below — the division written out, not merely its result:

```
Lane A, $4.99:  4.99 × 0.81634 = 4.0735        → 1,000 ÷ 4.0735 = 245.5 → 246 sales/mo
Lane B, $2.99:  2.99 × 0.81634 = 2.4409        → 1,000 ÷ 2.4409 = 409.7 → 410 sales/mo
Lane C, $4.49:  4.49 × 0.81634 = 3.66537       → 1,000 ÷ 3.66537 = 272.8 → 273 sales/mo
```

### 1.3 The ads alternative, quoted

Ads carry no per-request deduction: `revenue/day = DAU × ARPDAU` (§2.1). ON RECORD
(`docs/monetization-strategy.md:135-147`): reaching `$1,000/month` needs roughly **700–1,000 DAU**
rewarded-centric, **2,000–6,000 DAU** interstitial+banner, or **11,000+ DAU** banner-only. That
one sentence is the yardstick every `ads_led_utility` refusal below is held against.

### 1.4 A `SERVER_COST` generator is a server project, not a lane price

Any lane whose paid unit is generated by a paid API call is not priced here at all. Per
`docs/monetization-strategy.md` §3.7 and `.cursor/rules/client-secrets.mdc`, money-costing credit
needs a server-side `PurchaseAuthority`, which makes the slot a **server project**; and a
provider key compiled into `app/src/main/` is the signature defect `scripts/check-client-secrets.mjs`
exists to make unshippable. The required multiple of unit cost on the server-confirmed path is
`K = 1 / [(1−0.01)(0.85)(0.98)(0.98)(0.40)] = 3.0934×` (MEASURED, recomputed this session); on the
client-trusted path it is `3.83×` (ON RECORD, `docs/monetization-strategy.md` §2.2). This matters for
**lane B**, whose measured winners are AI-served (§3.4).

---

## 2. Lane A — authored learning / practice content

**The lane.** An app whose value is an **authored item bank** that teaches, tests or drills one
specific skill or credential. The content is authored by the builder, not typed by the user — this is
what separates it from the rejected "notebook" shape, where the value is the user's own records.

### 2.1 The target

**`$1,000 / month`** — ON RECORD, `docs/monetization-strategy.md:135`: "Illustrative
order-of-magnitude implications for **$1,000/month** (≈ $33/day)". The lower anchor used for this
lane is **`$300 / month`**.

### 2.2 The price band and the anchor

- **Band: `$2.99 – $9.99`** one-time (INFERRED from the observed paid price and the exam-prep
  one-time range).
- **Anchor: `$4.99`.**

Reason, held to the measured evidence. On the `ham radio exam prep` read (MEASURED,
`node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "ham radio exam prep"`, 2026-10-02)
exactly one comparable is a **fully paid, no-IAP app**:

| # | App | Rating | Reviews | Installs | ads | IAP | Price |
|---|---|---:|---:|---|---|---|---|
| 4 | HamStudy.org | 5.0 | 1,660 | 10K+ | false | false | **paid ($3.99)** |
| 2 | Ham Radio Prep | 4.6 | 1,520 | 100K+ | false | true | free |
| 1 | HamRadioExam - Technician | 4.6 | 314 | 50K+ | true | false | free |
| 3 | Ham Radio Exam (Vemento) | 4.4 | 49 | 5K+ | false | true | free |

So **`$3.99` is a demonstrated willingness-to-pay point, not an assumption** (MEASURED). The anchor
sits one purchasable step above it at **`$4.99`** because the paid unit is the whole authored bank
plus a timed-exam mode — a larger deliverable than a single question set — and because `$4.99` keeps
the required volume at 246 sales/month. A `$6.99`+ anchor (176 sales/month) sits **above the only
measured paid comparable** and is therefore **UNVERIFIED** for this lane until a paid incumbent at
that price is found.

### 2.3 Unit arithmetic — division shown

```
price                 = $4.99
net multiplier (local)= 0.81634                     ← (1−0.15)(1−0.02)(1−0.02)
Play fee deduction    = 0.15 of price   = $0.7485
refund + FX deduction = (0.02 + 0.02) of what remains
net per sale          = 4.99 × 0.81634 = $4.07      (exact 4.0735)

sales for $1,000/mo   = 1,000 ÷ 4.0735 = 245.5     → 246 sales/month (≈ 8.2/day)
sales for   $300/mo   =   300 ÷ 4.0735 =  73.6     →  74 sales/month

installs/month @ 2 %  = 246 ÷ 0.02    = 12,300 installs/month
```

MEASURED division; the conversion rate is UNVERIFIED (§7).

### 2.4 Archetypes: what fits, what is refused

The four ids are read from `scripts/lib/slot-economics.mjs`'s `ARCHETYPES` (ON RECORD,
`docs/slot-economics.md`): `purchase_led_single_unlock`, `purchase_led_repeat_sales`,
`ads_led_utility`, `purchase_only_no_ads`. `subscription` is **not** in the list and is banned
outright (owner, 2026-10-01) — it is not an option to weigh, so it gets no row.

| Archetype | Verdict | Revenue reason |
|---|---|---|
| `purchase_led_single_unlock` | **FIT — primary** | The user buys one credential outcome; the premium capability is "the full bank + timed exam mode", permanent and non-refilling. This is the shape the measured `$3.99` fully-paid incumbent already occupies (MEASURED). Capability: `billing`. |
| `purchase_led_repeat_sales` | **FIT — secondary only** | An authored bank *can* be sold in packs, but the user usually needs **one** credential, so the metered need does not recur within a 30–60-day window. It becomes honest only if the app spans a *family* of related credentials (e.g. several licence classes the same user may attempt). Capability: `billing`. |
| `ads_led_utility` | **REFUSED** | Revenue reason: the target needs **700–1,000 DAU** rewarded or **2,000–6,000 DAU** interstitial+banner (ON RECORD, §2.1), and an exam-practice app's daily-active population is **bursty** — concentrated in the weeks before a test, near-zero after it. It also spends the user's scarcest resource (concentration) on an interruption. Measured support for the pillar: on the larger `cdl practice test` read, **6 of 8** competitors declare ads, so ads *can* monetize this shelf — which is exactly why the refusal must be on **DAU**, not on taste. |
| `purchase_only_no_ads` | **REFUSED** | Revenue reason: a paid-only listing has **no free funnel and no ranked discovery**; the measured shelf's reach is held by free apps (100K+–1M+ installs). The strategy file records paid-only as "effectively dead" outside ad-hostile audiences (§4), and the base rate is **2.4 %** of paid-no-IAP apps clearing `$1,000/month` against **47.1 %** of free apps with IAP (ON RECORD, `docs/research/2026-09-26-teacher-gradebook-evidence.md:330-331`). |

### 2.5 The failure mode I would bet against

**The one to bet against:** that the `$3.99` fully-paid signal **generalises to a professional-credential
shelf**. The `$3.99` fully-paid comparable sits in a **hobbyist** niche (amateur radio). On the
larger, more professional `cdl practice test` read (MEASURED, same command, query `"cdl practice test"`),
the top 8 contains **no paid one-time app at all** — the shelf is led by `CDL Prep` (4.6★, 5,710
reviews, 1M+, ads, free) and `CDL Prep Test 2026` (4.9★, 19,800 reviews, 500K+, ads, free), and **6 of
8** declare ads. So "some people pay up front for exam content" is proven in one niche and
**UNVERIFIED** in the professional one.

**Where the reading is UNVERIFIED:** the `iap=true` badge proves a paywall exists, **not what it
charges or what fraction of installs it converts** (Ham Radio Prep shows `iap=true` at 100K+ installs;
the price and the conversion are labels the badge does not carry).
**Command that would settle it:** `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "<the
exact credential>"`, then open the listing's own "In-app purchases" price range — and, for the real
decision, read `apps/<slot>/store/outcomes.json` after a live paywall exists.

### 2.6 The price I would refuse to go below

**`$2.99`.** At `$2.99` the net is `$2.4409`, so the target needs **410 sales/month = 20,500
installs/month at 2 %** — already at the top of what the measured shelf can plausibly supply (its
largest *lifetime* bucket on the paid comparable is `10K+`, and the free leaders are `100K+`–`1M+`
lifetime). Below `$2.99` the required volume grows faster than the lane's evidenced reach, and pricing
under the measured `$3.99` paid point in a shelf where the user is buying a **credential outcome**
tells the buyer the product is thinner than the incumbent's, not cheaper.

---

## 3. Lane B — generator & creative / maker tools

**The lane.** An app that **generates content the user keeps** — random tables, patterns, generators.
On-device generation is the honest architecture; a generator backed by a paid API is rejected at §1.4.

### 3.1 The target

**`$1,000 / month`** — ON RECORD, `docs/monetization-strategy.md:135` ("…for **$1,000/month** (≈
$33/day)"). Lower anchor `$300 / month`.

### 3.2 The price band and the anchor

- **Band: `$0.99 – $4.99`** per one-time purchase (INFERRED from the shelf's free+IAP structure).
- **Anchor: `$2.99` per content pack** (the primary archetype is repeat sales, §3.4).

Reason — and its honest weakness, stated first. **No paid one-time incumbent was measured on either
query.** The measured maker shelves monetise **inside free apps**: on the `knitting pattern generator`
read (MEASURED, `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "knitting pattern
generator"`), **7 of 8** apps declare IAP and none is paid:

| # | App | Rating | Reviews | Installs | ads | IAP | Price |
|---|---|---:|---:|---|---|---|---|
| 3 | YarnPal - Crochet for Beginner | 4.7 | 244,000 | 1M+ | false | true | free |
| 2 | Crochet AI – Knitting Patterns | 4.5 | 8,100 | 100K+ | false | true | free |
| 6 | My Row Counter: Knit & Crochet | 4.8 | 15,500 | 1M+ | true | true | free |
| 1 | Stitchart | 4.1 | 747 | 100K+ | true | true | free |

On the `ttrpg random tables` read the relevance guard itself flagged the result set as **"polluted —
only 7/30 visible titles contain a distinctive token"**, so that set is **not** a clean reading of
the query; its only paid entry is a licensed board game (`D&D Lords of Waterdeep`, `$7.99`), not a
generator. **Therefore the anchor is not read off a competitor's price; it is INFERRED** from the
mid-point of the one-time IAP band and chosen so a pack clears the target at a pack count the shelf's
`100K+`–`1M+` leaders make conceivable (410 packs/month).

### 3.3 Unit arithmetic — division shown

```
price                  = $2.99  (per pack)
net multiplier (local) = 0.81634
Play fee deduction     = 0.15 of price   = $0.4485
refund + FX deduction  = (0.02 + 0.02) of what remains
net per sale           = 2.99 × 0.81634 = $2.44      (exact 2.4409)

sales for $1,000/mo    = 1,000 ÷ 2.4409 = 409.7      → 410 packs/month (≈ 13.7/day)
sales for   $300/mo    =   300 ÷ 2.4409 = 122.9      → 123 packs/month

installs/month @ 2 %   = 410 ÷ 0.02     = 20,500 installs/month
```

MEASURED division; the conversion rate is UNVERIFIED (§7). At the single-unlock fallback price of
`$3.99` the same target needs **308 sales/month = 15,400 installs/month at 2 %**.

### 3.4 Archetypes: what fits, what is refused

| Archetype | Verdict | Revenue reason |
|---|---|---|
| `purchase_led_repeat_sales` | **FIT — primary** | A maker's want is metered by the **content library**: more tables, more patterns, more themes. The measured shelf shows repeat purchase behaviour — **7 of 8** craft apps declare IAP (MEASURED). The base engine stays free; the packs are the paid unit. Capability: `billing`. **Cost to state:** each pack is authored once, so the engine must amortise authoring across packs. |
| `purchase_led_single_unlock` | **FIT — fallback** | Honest when the paid unit is the **"keep"** itself — unlimited export/print plus the full library, one premium capability. Cheaper to build than a pack pipeline. Capability: `billing`. |
| `ads_led_utility` | **REFUSED** | Revenue reason: TTRPG/maker tools are used **weekly-to-occasionally** (a game night, a project), and banner-only needs **11,000+ DAU** while interstitial+banner needs **2,000–6,000 DAU** for the target (ON RECORD, §2.1). A twice-a-month app needs roughly **6× the monthly audience** of a daily app to hold the same DAU. The shelf's own leaders monetise with IAP, not with ad inventory. |
| `purchase_only_no_ads` | **REFUSED** | Revenue reason: **no paid incumbent exists** in this shape on either measured query, so the price has no demonstrated floor; and a paid-only listing forfeits the free funnel that let YarnPal reach `244,000` reviews (MEASURED). |
| **Any AI-served generator** | **REFUSED (not an archetype — an architecture)** | Revenue reason: the three shelf winners whose *generation* is AI-served (YarnPal, Crochet AI) are paid-API products. Shipping one client-side is forbidden by `.cursor/rules/client-secrets.mdc`; shipping one server-side is a **server project** with `K = 3.0934×` unit cost and a `PurchaseAuthority` (§1.4). Either way it is not a one-hour slot, and it is not what the lane's measured on-device comparables do. |

### 3.5 The failure mode I would bet against

**The one to bet against:** that an **on-device procedural generator is worth paying for at all**. The
measured winners in this lane do not sell a generator — they sell **AI-served output** inside a free
app (YarnPal `244,000` reviews, 1M+ installs, free+IAP). The thing users appear to pay for is the
generated asset, and the *value of generation* is therefore demonstrated only for an architecture this
factory cannot ship cheaply. A procedural app competes with a free AI app that already has 244,000
reviews.

**Where the reading is UNVERIFIED:** there is **no measured one-time price** in this lane — the
`iap=true` badge (7/8) proves a paywall, not its price or its conversion.
**Command that would settle it:** `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "<a
specific maker job>"`, looking for a `paid ($X)` row and reading the IAP price range on the listings —
both queries run here returned **no** paid one-time entry.

### 3.6 The price I would refuse to go below

**`$1.99` per pack.** At `$1.99` the net is `$1.6245`, so the target needs **616 packs/month =
30,800 installs/month at 2 %**; below it, `$0.99` (the only purchasable step down) nets `$0.81` and
needs **1,235 packs/month**. Both are high-volume flows that the lane's *paid* surface shows no
evidence of supplying — its volume is held by free apps whose revenue is ad and IAP, not by a paid
flow of that scale — and a pack priced under `$1.99` cannot carry the authoring cost of a pack within
any plausible sale count.

---

## 4. Lane C — real-time niche instruments

**The lane.** A real-time **sound / tempo instrument** for a narrow musical tradition or a niche
sport: a metronome/tuning/drone tool for a specific practice. The product is the instrument itself —
nothing is authored per user, and nothing is typed in.

### 4.1 The target

**`$1,000 / month`** — ON RECORD, `docs/monetization-strategy.md:135` ("…for **$1,000/month** (≈
$33/day)"). Lower anchor `$300 / month`.

### 4.2 The price band and the anchor

- **Band: `$2.99 – $9.99`** one-time (INFERRED from the observed paid price).
- **Anchor: `$4.49`** — mirroring the measured paid incumbent.

Reason. The `tabla metronome` read (MEASURED, `node scripts/enrich-market.mjs --top 8 --concurrency 3
--query "tabla metronome"`, verdict **fortress**) contains exactly one **fully paid, no-IAP, no-ads**
comparable — the measured winner of the whole three-lane screen:

| # | App | Rating | Reviews | Installs | ads | IAP | Price |
|---|---|---:|---:|---|---|---|---|
| 7 | **Rhythm Tabla & Tanpura PREMIUM** | 4.7 | **4,030** | 50K+ | false | false | **paid ($4.49)** |
| 2 | Rhythm with Tabla & Tanpura (free twin) | 4.3 | 14,300 | 1M+ | true | false | free |
| 1 | Metronome Beats | 4.8 | 183,000 | 10M+ | true | true | free |
| 5 | Metronome Pro - Beat & Tempo | 4.7 | 22,800 | 1M+ | false | true | free |

`$4.49` is a **demonstrated** willingness-to-pay point for the exact product shape. Pricing **above**
it without our own evidence is a bet; pricing **well below** it undercuts the one measured point in a
niche where the buyer is a committed practitioner and the price signals the instrument's quality.

### 4.3 Unit arithmetic — division shown

```
price                  = $4.49
net multiplier (local) = 0.81634
Play fee deduction     = 0.15 of price   = $0.6735
refund + FX deduction  = (0.02 + 0.02) of what remains
net per sale           = 4.49 × 0.81634 = $3.67      (exact 3.66537)

sales for $1,000/mo    = 1,000 ÷ 3.66537 = 272.8     → 273 sales/month (≈ 9.1/day)
sales for   $300/mo    =   300 ÷ 3.66537 =  81.8     →  82 sales/month

installs/month @ 2 %   = 273 ÷ 0.02      = 13,650 installs/month
```

MEASURED division; the conversion rate is UNVERIFIED (§7).

### 4.4 Archetypes: what fits, what is refused

| Archetype | Verdict | Revenue reason |
|---|---|---|
| `purchase_led_single_unlock` | **FIT — anchor** | The premium capability is exactly one thing — the instrument at full function (all taals/scales, background play, recording) — permanent and non-refilling. A free tier gets ranked discovery, which the measured free twin shows is worth 5× the reviews of the paid twin (14,300 vs 4,030, MEASURED). Capability: `billing`. |
| `purchase_only_no_ads` | **FIT — second, with the incumbent's own evidence** | The measured incumbent **is** this archetype (ads false, IAP false, `$4.49`, 4,030 reviews). For an ad-hostile, screen-off, background-audio product it is a real shape, and the strategy file names it as such (§3.6). It is refused as the *anchor* below only on discovery, not on principle. Capability: none; `priceUsd > 0`. |
| `purchase_led_repeat_sales` | **FIT — secondary** | Sound/instrument packs (more voices, more taal packs) are genuinely resellable and cost nothing to serve once downloaded (local). Honest only if the app ships a library worth extending. Capability: `billing`. |
| `ads_led_utility` | **REFUSED** | Revenue reason: an instrument is a **background, screen-off audio** product, so interstitial and banner inventory is structurally unavailable at the moment the app is used — leaving only rewarded — and banner-only would need **11,000+ DAU** (ON RECORD, §2.1) against a measured niche whose *paid* twin holds `50K+` lifetime installs. The measured free twin shows ads *can* ride a 1M+ funnel, but the shelf's paid signal is stronger and the audio context is hostile. |
| `purchase_only_no_ads` **as the ONLY channel** | **REFUSED as anchor** | Revenue reason: it forfeits the free funnel — the measured free twin holds **3.5×** the reviews of the paid twin (14,300 vs 4,030), and paid-only is recorded as a discovery-dead lever with a **2.4 %**-vs-**47.1 %** clear-rate gap (ON RECORD, `docs/research/2026-09-26-teacher-gradebook-evidence.md:330-331`). Held as a legitimate second choice, not the plan. |

### 4.5 The failure mode I would bet against

**The one to bet against:** that the incumbent's **paid-app** shape transfers to a **free + unlock**
shape. The measured winner is `purchase_only_no_ads` (no IAP badge, no ads badge, `$4.49`); we have
**no measured conversion for a free-tier paywall** in this niche, and the free twin's economics (ads
on 1M+ installs) are a different business from a paywall on a free funnel.

**Where the reading is UNVERIFIED:** the install-to-purchase conversion of a free-tier instrument;
**no slot in this factory has ever reported a sale** (`node scripts/kiln-status.mjs` reports the live
slot as `NO-DATA`), and `docs/monetization-owner.md` records that the factory has no revenue API.
**Command that would settle it:** a live paywall plus a dated reading in
`apps/<slot>/store/outcomes.json` read by `node scripts/check-outcomes.mjs --slot <slug>` — the only
mechanism that exists, and it is hand-recorded today.

### 4.6 The price I would refuse to go below

**`$2.99`.** At `$2.99` the net is `$2.4409`, so the target needs **410 sales/month = 20,500
installs/month at 2 %** — at the edge of plausible for a niche whose free twin holds `1M+` *lifetime*
installs. Below that the required flow grows faster than the niche's funnel, and an instrument priced
under `$3` in a shelf where the measured paid point is `$4.49` reads as a lesser instrument to a
committed practitioner.

---

## 5. The comparison — which lane, and the single number that decides it

### 5.1 The three anchors side by side

| | Lane A — authored content | Lane B — generators/maker | Lane C — niche instruments |
|---|---:|---:|---:|
| Anchor price | **$4.99** | **$2.99** /pack | **$4.49** |
| Net per sale (local) | $4.07 | $2.44 | $3.67 |
| Sales/mo for **$1,000** | 246 | 410 | 273 |
| Sales/mo for **$300** | 74 | 123 | 82 |
| Installs/mo @ 2 % for $1,000 | 12,300 | 20,500 | 13,650 |
| Measured **paid** incumbent | **1,660** reviews @ `$3.99` (HamStudy.org) | **none** | **4,030** reviews @ `$4.49` (Rhythm Tabla & Tanpura PREMIUM) |
| Measured free leader | 100K+–1M+ (ham/CDL) | 244,000 rev / 1M+ (YarnPal) | 14,300 rev / 1M+ (free tabla twin) |
| Build cost driver | **high** — authoring a *correct* bank, plus correctness risk | low–medium — engine + packs | low — audio/timing engine |
| Server needed? | no (local) | **no only if procedural**; AI = server project | no (local) |
| Primary archetype | `purchase_led_single_unlock` | `purchase_led_repeat_sales` | `purchase_led_single_unlock` |

MEASURED — prices, nets and sales recomputed this session; reviews/installs from the five
`enrich-market.mjs` reads above.

### 5.2 Which lane I would bet on

**Lane C — real-time niche instruments** — and the single number that decides it is **`4,030`: the
review count of the lane's fully-paid, no-IAP, no-ads incumbent at the anchor price** (`Rhythm Tabla &
Tanpura PREMIUM`, `$4.49`, MEASURED, query `"tabla metronome"`).

Why that number and not the price: it is the only measurement in the whole screen of an **actual
paying base in the exact product shape**, and it is the largest of the three. Lane A's paid incumbent
(`HamStudy.org`, `$3.99`) holds **1,660** reviews in a *hobbyist* niche, and on the professional CDL
shelf there is **no paid incumbent at all**; lane B holds **none**. Behind lane C's paid twin sits a
**1M+-install free funnel** (14,300 reviews), so the anchor price is not merely accepted — it is
accepted inside a demonstrated acquisition channel.

The supporting number: lane C needs **273 sales/month = 13,650 installs/month at 2 %**. The shelf's
free twin holds `1M+` **lifetime** installs (MEASURED), i.e. the required monthly flow is a small
fraction of one incumbent's demonstrated reach — while lane A's required 12,300 installs/month must be
won on a shelf whose paid signal comes from a single hobbyist app, and lane B's required 20,500
installs/month has **no paid incumbent to calibrate against at all**.

**The honest caveat, so the bet is falsifiable:** the deciding number counts **reviews, not buyers**,
and a review is self-selected. The observation that would make the bet negative is the first
`apps/<slot>/store/outcomes.json` reading from a lane-C slot: if a free-tier paywall converts at
**below ~1 %**, the required install flow doubles to 27,300/month and lane C's advantage over lane A
narrows to the reach of its funnel. There is no measured conversion to decide it today.

### 5.3 What lane C does NOT get from this worksheet

- It does not get a **topic**. "Niche instrument" is a lane; a specific tradition/credential/sport and
  its wedge are the scouts' job, and the wedge-in-one-sentence is a hard prerequisite
  (`docs/ideation/README.md`).
- It does not get **permission**. No slot exists, no plan is written, and checkpoint 1 is the owner's.
- It does not get a claim that the anchor **will** earn. The arithmetic shows what the anchor
  **requires**; the conversion rate that turns requirements into a forecast is UNVERIFIED (§7).

---

## 6. Rejections used here, and the ones that are banned

Every refusal above names one of the closed list of revenue reasons (ON RECORD,
`docs/ideation/2026-10-01-pricing-and-archetype-worksheet.md` §7):

| # | Revenue reason | Where used here |
|---:|---|---|
| 2 | An unrated demand reading | not used — every lane produced rated incumbents |
| 3 | A price the market will not pay / a shelf that monetises elsewhere | Lane A `ads_led_utility`; lane B `purchase_only_no_ads`; lane C `ads_led_utility` |
| 4 | A build cost that eats the margin (incl. a server arriving late) | Lane B AI-served generator (§3.4) |
| 5 | A duplicate of our own catalogue | not invoked — no lane duplicates a `registry/apps.json` job identity |

**Never used, and must not appear** (each is a novelty judgement wearing a revenue coat): *"this
category is already crowded"* — an occupied shelf is **evidence of demand**; *"nothing like this
exists yet"* as a reason **to** proceed; and *"we already built something in this area"* other than
the catalogue-duplication test. The screen above deliberately treats the **fortress** verdicts
(lane C's `tabla metronome`, lane A's `cdl practice test`) as **positive** demand readings, not as
disqualifications: money on display is the cheap proof that a market pays.

---

## 7. Assumptions and unknowns

Listed, not omitted. Each is what it is.

1. **The install-to-purchase conversion is unmeasured.** No slot in this factory has ever reported a
   sale — `node scripts/kiln-status.mjs` reports the live slot as `NO-DATA`, and
   `docs/monetization-owner.md` records that the factory has no revenue API. **UNVERIFIED.** The 2 %
   used in every installs column is an assumption; 1 % and 2 % bracket the only recorded range and 5 %
   is supported by no recorded figure. **Command to settle:** a live paywall plus readings in
   `apps/<slot>/store/outcomes.json`, read by `node scripts/check-outcomes.mjs --slot <slug>`.
2. **`F_billing = 0.15` is an ASSUMPTION, not this account's fee.** It holds "only under the first-$1M
   annual tier and/or the new-install case"; above $1M, or for existing installs in the EEA/UK/US at
   standard rates, it rises to 20–30 % + the 5 % billing fee (`docs/monetization-strategy.md` §2.2).
   Every net figure here moves if the tier is not the one assumed. **UNVERIFIED** whether the fee is
   computed on a tax-inclusive price.
3. **`F_refund = 0.02` and `F_fx = 0.02` are ASSUMPTIONS**, each a decision "per slot".
4. **`F_fraud` is excluded for local unlocks** — INFERRED from the `LOCAL`/`SERVER_COST` definition in
   `docs/monetization-strategy.md` §3.7, re-checked if that definition changes.
5. **The target itself is illustrative.** `$1,000/month` is the file's own "illustrative
   order-of-magnitude" figure (`docs/monetization-strategy.md:135,149`), not a measured result of this
   account, and **over 80 % of apps never cross it** (ON RECORD,
   `docs/research/2026-09-26-teacher-gradebook-evidence.md:327-328`).
6. **A review count is not a buyer count.** §5.2's deciding number counts self-selected reviews. It is
   the best public proxy available and it is **not** a conversion rate. **Command to settle:** none
   public; only a console reading.
7. **The `iap=true` badge carries no price and no conversion.** The badge proves a paywall exists;
   lanes A and B both rest partly on it. **Command to settle:** read the listing's own "In-app
   purchases" price range, or find a `paid ($X)` row in a further `enrich-market.mjs` read.
8. **The market reads are single dated snapshots** (2026-10-02, five queries, `--top 8`). They are
   MEASURED as of that date and will drift.
9. **The TTRPG query was flagged by the relevance guard** ("polluted — only 7/30 visible titles
   contain a distinctive token"), so lane B's TTRPG evidence is weaker than its craft evidence. The
   craft query was "partial — … and 'generator' appears in no title". Both caveats are carried into
   §3, not hidden.
10. **The paid-app base rate is recorded, not measured here.** 2.4 % of paid-no-IAP apps clear
    `$1,000/month` vs 47.1 % of free apps with IAP (ON RECORD,
    `docs/research/2026-09-26-teacher-gradebook-evidence.md:330-331`, sourced to Trend Apps).
11. **The set of purchasable Play price points is UNVERIFIED** (research §8.8) — a Play Console step.
    Every listed price must land on one.
12. **Whether the §1.1 production-access gate applies to this account is UNVERIFIED**, and it decides
    whether a kill window can even start (it can cost each app ≥ 14 days before it can earn).
13. **No AdMob account exists**, and creating it is a blocking human action
    (`docs/monetization-strategy.md` §0). Every ads refusal above rests on the recorded DAU benchmarks,
    not on this account's eCPM, which has never been measured.
14. **The 30-day month** used for per-day figures is an INFERRED convention.
15. **No number here is a projection of a projection.** Each traces to a recorded assumption, a
    division performed in this session, or a MEASURED market read.

---

*Written by the money owner, 2026-10-02. The plan and the reading for a real slot are
`apps/<slug>/economics.json` and `apps/<slug>/store/outcomes.json`; this file is neither, authorises
neither, and is superseded the moment a proposal carries its own derivation. It prices three lanes,
and a lane is not an idea.*

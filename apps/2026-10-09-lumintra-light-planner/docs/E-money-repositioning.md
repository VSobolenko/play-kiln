# Money owner — repositioning the astronomy engine (REVISION 2)

**Date:** 2026-10-08 · **Role:** PlayKiln money owner · **Status:** modelled plan, not observed revenue
**Supersedes:** revision 1 of this file (same path). Revision 1's satellite reasoning was wrong; the
error is preserved below, not deleted.

---

## CORRECTIONS (read before anything else)

### Correction 1 — the satellite shelf has a paid-upfront anchor at $17.99 (revision 1 said it had none)

Revision 1 §1.6 and §3-Shape-D claimed, in words, that the satellite shelf had *"six of six free"* and
*"there is not one paid-upfront app to anchor $7.99 against"*. **That is false.** I reproduced the
owner's correction with my own run in this session:

```text
node scripts/enrich-market.mjs --query "iss detector" --top 6 --concurrency 2

#  package                     title                          rating  reviews (exact)  installs  bound         ads    iap    price
1  com.runar.issdetector       ISS Detector Satellite Tracker  4.8    121,000 (120,809) 5M+      >= 5,000,000  true   true   free
2  com.runar.issdetector.pro   ISS Detector Pro                4.4      3,190 (3,189)  100K+    >= 100,000    false  false  paid ($17.99)
```

**Cause of the error — query depth, not a bad fetch.** Revision 1's row came from
`--query "satellite tracker" --top 6`, which enriched the top **6** cards for that query. That query's
shelf is led by the free parent `com.runar.issdetector` (5M+, ads + IAP) and the paid **Pro variant
ranks outside the top 6** for the phrase "satellite tracker". I read a 6-row window and generalised it
to the whole shelf. The correction is reproduced above with a **different query** ("iss detector"),
under which the Pro variant ranks #2. The lesson is the one `grounded-claims.mdc` already names: a
statement about a shelf must be about what was **observed**, and a top-N window is not a shelf.

### Correction 2 — new observed light-planning shelf evidence (scout C)

Scout C's reading (`.kiln/scratch/positioning-2026-10-08/C-sun-moon-light-shelf.md`) is now part of
this report's base. I **re-verified its load-bearing rows myself** in this session (commands in §1.4),
not just cited them:

| App | price | installs | rating | reviews | ads / IAP | my re-verify |
|---|---:|---:|---:|---:|---|---|
| PhotoPills | $10.99 | 100K+ | 4.8 | 8,220 | false / false | MEASURED this session |
| Sun Surveyor (Sun & Moon) | $9.99 | 100K+ | 4.8 | 5,140 | false / false | MEASURED this session |
| Sun Position PRO | $9.99 | 50K+ | 4.9 | 2,730 | false / false | MEASURED this session |
| Planit Pro: Photo Planner | $9.99 | 10K+ | 4.6 | 1,220 | false / true | MEASURED rev 1 |
| Sun Seeker | $8.99 | 50K+ | 4.8 | 1,000 | false / false | MEASURED this session |
| SunOnTrack | $7.99 | 50K+ | 4.8 | 874 | false / false | scout C only |
| Golden Hour+ | $4.99 | 1K+ | 4.7 | 189 | false / false | MEASURED rev 1 |

**Hard ceiling: $10.99.** Nothing above $10.99 exists on the light-planning shelf (scout C refuted a
$37.99 hit as astrology software; `ephemeris.vedic`). Core band $7.99–$10.99; one $4.99 outlier below.

### Correction 3 — what the correction does and does not change

- **Does not change:** the recommendation stays on the **light-planning shelf**, because it remains the
  only shelf with **standalone purchase-only paid anchors at six-figure installs** (PhotoPills $10.99 /
  100K+, Sun Surveyor $9.99 / 100K+). Recomputed in §3 — not re-affirmed for consistency.
- **Does change:** Shape D (satellite) is no longer rejected on *"no paid app exists"* — that reason was
  wrong. It is rejected on a **reach/discovery** reason (§3-D). And the satellite capability becomes a
  **credible differentiator** for the light shelf, because pass timing is now proven payable **somewhere**.
- **Does not change the price ceiling:** $17.99 does **not** transfer to a standalone light-planning
  product. §2 answers the tension explicitly.

---

## 0. The question

One engine — Sun/Moon/twilight geometry, target altitude curves, SGP4 satellite passes, hourly cloud
cover/visibility, exposure guidance — producing **one combined session brief** plus a share/export
handoff. Revision 1 positioned it for **astrophotographers** at **$10.99** and the owner flagged that
niche as possibly too small. This revision prices the incumbent shelves (observed), re-derives the
recommendation across the shapes **with the $17.99 fact included**, and answers: *at what price, on
which shelf, with which one listing sentence.*

## How the shelves were priced (provenance)

```text
node scripts/enrich-market.mjs --query "astrophotography planner" --query "stargazing" \
  --query "star map" --query "sun surveyor" --top 6 --concurrency 2      # 24 apps, exit 0
node scripts/enrich-market.mjs --query "golden hour" --query "satellite tracker" \
  --query "aurora forecast" --top 6 --concurrency 2                       # 18 apps, exit 0
node scripts/enrich-market.mjs --query "iss detector" --top 6 --concurrency 2   # the correction, §CORRECTIONS-1
node scripts/enrich-market.mjs --query "sun surveyor" --top 6 --concurrency 2   # re-verify light anchors
node scripts/enrich-market.mjs --query "photopills" --query "sun position pro" --top 4 --concurrency 2  # re-verify ceiling
```

Every price, install bucket, badge and rating below is `MEASURED` from those runs. No price is reused
where it was not observed. Two relevance-guard caveats stated because they weaken the base:
`astrophotography planner` was **polluted (4/30)** and `stargazing` **partial (13/30)**; `iss detector`
was **polluted (2/20)** — the Pro row is real but its query recall is weak, so treat that shelf's
*shape* as less certain than the light shelf's.

---

## 1. Observed shelves

### 1.1 `astrophotography planner` — `mixed`, **polluted (4/30)**

| # | App | Price | Model | Installs | Rating |
|---|---|---|---|---|---|
| 1 | Astrophotography Planner (CORITHM) | free | ad + IAP | 100+ | unrated |
| 2 | Dark Skies: Astrophotography | free | IAP | 500+ | unrated |
| 3 | **Planit Pro: Photo Planner** | **paid $9.99** | + IAP | **10K+** | 4.6 (1,220) |
| 4 | Stellarium - Star Map | free | IAP | 10M+ | 4.8 (337,048) |
| 5 | DeepSkyCamera | free | none | 1M+ | 3.1 (781) |
| 6 | AstroCam: Night Sky Camera | free | ad + IAP | 10K+ | 4.0 (370) |

**Read (unchanged):** the only paid planner is Planit Pro at $9.99 / **10K+** — the shelf's paid signal
tops out in the five-figure band.

### 1.2 `stargazing` — `mixed`, **partial (13/30)**

| # | App | Price | Model | Installs | Rating |
|---|---|---|---|---|---|
| 1 | Stellarium - Star Map | free | IAP | 10M+ | 4.8 (337,048) |
| 2 | Sky Tonight | free | ad + IAP | 5M+ | 4.7 (85,714) |
| 3 | Star Walk 2 Pro | **paid $2.99** | IAP | 500K+ | 4.7 (32,541) |
| 4 | SkyView® Lite | free | none | 10M+ | 4.3 (58,099) |
| 5 | Star Walk 2 Plus | free | ad + IAP | 10M+ | 4.5 (571,689) |
| 6 | Star Tracker | free | ad + IAP | 10M+ | 4.5 (82,345) |

### 1.3 `star map` — `mixed`

| # | App | Price | Model | Installs | Rating |
|---|---|---|---|---|---|
| 1 | Stellarium - Star Map | free | IAP | 10M+ | 4.8 (337,048) |
| 2 | Sky Map (Sky Map Devs) | free | none | **50M+** | 4.1 (481,603) |
| 3 | Star Walk 2 Plus | free | ad + IAP | 10M+ | 4.5 (571,689) |
| 4 | Star Tracker | free | ad + IAP | 10M+ | 4.5 (82,345) |
| 5 | Star Walk 2 Pro | **paid $2.99** | IAP | 500K+ | 4.7 (32,541) |
| 6 | Sky Tonight | free | ad + IAP | 5M+ | 4.7 (85,714) |

**Read (unchanged):** the general night-sky shelf's observed price is **$0**; the one paid listing is a
$2.99 Pro variant of a free 10M+ leader.

### 1.4 `sun surveyor` — the light-planning shelf (`mixed`), **zero ads on the paid rows**

| # | App | Price | Model | Installs | Rating |
|---|---|---|---|---|---|
| 1 | Sun Surveyor Lite | free | none | 1M+ | 4.1 (3,980) |
| 2 | **Sun Surveyor (Sun & Moon)** | **paid $9.99** | none | **100K+** | 4.8 (5,140) |
| 3 | **Sun Seeker: Sunlight Tracker** | **paid $8.99** | none | **50K+** | 4.8 (1,000) |
| 4 | Sunset & Sunrise - Sun Tracker | free | IAP | 500K+ | 4.3 (9,253) |
| 5 | Sun Position: Sunrise & Sunset | free | IAP | 1M+ | 4.6 (12,495) |
| 6 | Sun Ephemeris | free | IAP | 100K+ | 4.2 (3,588) |

Re-verified this session, plus two direct targets:

```text
#  com.photopills.android.photopills  PhotoPills        4.8  8,220 (8,216)  100K+  >= 100,000  false false  paid ($10.99)
#  com.andymstone.sunposition         Sun Position PRO  4.9  2,730 (2,729)   50K+  >= 50,000   false false  paid ($9.99)
```

**Read:** the **strongest paid-upfront evidence in the whole scan** — at least **five** purchase-only,
no-ads standalone planners at $7.99–$10.99, two at **100K+** installs, all rated 4.7–4.9.

### 1.5 `golden hour` — `fortress`

| # | App | Price | Model | Installs | Rating |
|---|---|---|---|---|---|
| 1 | Golden Hour | free | ad | 500K+ | 4.7 (5,373) |
| 2 | Golden Hour+ | **paid $4.99** | none | **1K+** | 4.7 (189) |
| 3 | Golden Hour Calculator | free | IAP | 5K+ | 4.0 (86) |
| 4 | Sunlapse | free | none | 50+ | unrated |
| 5 | Sunset & Sunrise - Sun Tracker | free | IAP | 500K+ | 4.3 (9,253) |
| 6 | Golden Radio Hour: OTR *(irrelevant)* | free | IAP | 100+ | unrated |

**Read (unchanged):** the *golden-hour-only* job's paid ceiling is **1K+ installs**; the free ad-led
leader holds 500K+.

### 1.6 `satellite tracker` — **CORRECTED in §CORRECTIONS-1**

Revision 1 reported the top-6 of `--query "satellite tracker"` and generalised to the shelf. That
window:

| # | App | Price | Model | Installs | Rating |
|---|---|---|---|---|---|
| 1 | Satellite Tracker by Star Walk | free | ad + IAP | 1M+ | 4.4 (61,390) |
| 2 | ISS Detector Satellite Tracker | free | ad + IAP | **5M+** | 4.8 (120,809) |
| 3 | Look4Sat: Satellite tracker | free | none | 100K+ | 3.8 (1,011) |
| 4 | SkyLive: Satellite Tracker | free | IAP | 500+ | unrated |
| 5 | Satellite Chasers | free | none | 50K+ | 3.5 (232) |
| 6 | Find Starlink Satellites | free | IAP | 1M+ | 4.1 (2,066) |

**This window is where revision 1's error was born: the paid `com.runar.issdetector.pro` variant is not
in it.** Under the query that surfaces it:

| # | App | Price | Model | Installs | Rating |
|---|---|---|---|---|---|
| 1 | ISS Detector Satellite Tracker (free parent) | free | ad + IAP | **5M+** | 4.8 (120,809) |
| 2 | **ISS Detector Pro** | **paid $17.99** | **none** | **100K+** | 4.4 (3,190) |
| 3 | Spot the Station (NASA) | free | none | 500K+ | 4.7 (7,118) |
| 4 | Satellite Tracker by Star Walk | free | ad + IAP | 1M+ | 4.4 (61,390) |
| 5 | Look4Sat | free | none | 100K+ | 3.8 (1,011) |
| 6 | Find Starlink Satellites | free | IAP | 1M+ | 4.1 (2,066) |

**Corrected read:** satellite pass timing **is** payable — at **$17.99**, the highest observed price
point in this recon, no ads, no IAP, 100K+ devices. But see §2: **$17.99 is an in-funnel Pro upgrade of
a free 5M+ app**, not a standalone purchase-only listing.

### 1.7 `aurora forecast` — `mixed`

| # | App | Price | Model | Installs | Rating |
|---|---|---|---|---|---|
| 1 | My Aurora Forecast & Alerts | free | ad | **5M+** | 4.7 (55,308) |
| 2 | My Aurora Forecast Pro | **paid $3.99** | IAP | 50K+ | 4.8 (4,074) |
| 3 | Aurora Now | free | IAP | 100K+ | 4.4 (1,535) |
| 4 | Aurora Forecast by Aurora Labs | free | none | 100K+ | 3.7 (190) |
| 5 | Aurora Compass | free | IAP | 50K+ | 4.7 (564) |
| 6 | Hello Aurora | free | IAP | 100K+ | 3.8 (668) |

---

## 2. THE TENSION — does $17.99 let us price above the $10.99 light ceiling?

**No. $17.99 is real, and it does not transfer.** The fact that separates it:

**ISS Detector Pro is not a standalone paid listing; it is the paid *variant* of a free app with 5M+
installs.** Measured in the same run (§1.6): the free parent `com.runar.issdetector` carries **5M+
installs and 120,809 reviews** with **ads + IAP**; the Pro carries **100K+ installs and 3,190 reviews**
(no ads, no IAP). Two consequences, both arithmetic rather than taste:

- The Pro is reached through the free app's own funnel — a 5M+ install base converting ~2 % to a
  $17.99 ad-free upgrade is `100K+ / 5M+`. **A new entrant has no 5M+ free funnel**, so the $17.99 is a
  price paid by *already-acquired* users, not a price won by **discovery** against $10.99 competitors.
- The light-planning shelf shows the **opposite** model works there: PhotoPills $10.99 and Sun Surveyor
  $9.99 are **standalone purchase-only listings** that win on discovery at six-figure installs. That is
  the model this factory would actually be entering.

**Therefore the satellite capability differentiates *within* the light shelf's band; it does not lift
the price above $10.99.** To charge $17.99 honestly one would have to reproduce ISS Detector's shape —
free app → 5M+ installs → paid Pro unlock — which is not a shape a new slot can assume, and which the
light shelf does not use. Pricing at $17.99 standalone would put the app **outside every observed price
where a standalone light-planner is sold**, with no evidence any standalone listing sustains it.

The $17.99 fact still earns its place: it proves pass timing is a **payable** feature somewhere, so
**bundling** it is a credible differentiator rather than a curiosity — it just does not set the price.

---

## 3. Shapes, recomputed with the $17.99 fact

Shared arithmetic. Per `docs/monetization-strategy.md` §2.2, one-time sale:

```text
net_per_sale = price × (1 − F_billing) × (1 − haircut)
             = price × 0.85           × 0.92
```

`F_billing = 0.15` (Play first-$1M / new-install tier; rises to 0.20–0.30 + 5 % billing above $1M or for
existing EEA/UK/US installs — the higher tier is the honest risk and is shown separately in §4).
`0.08` is the combined tax/refund/FX haircut. **Target: $500 net per month** (see §4 for why the target
is unchanged). `sales_needed = 500 / net_per_sale`.

### Shape A — `astrophotography-session-planner` (revision 1's positioning)

```text
price        = $10.99
net          = 10.99 × 0.85 × 0.92 = 8.59
sales/mo     = 500 / 8.59          = 58.2/mo  → 59 sales/month
```

**REJECT — reach.** The astrophotography shelf's only paid anchor is Planit Pro **$9.99 / 10K+** (§1.1);
59 paying users a month, indefinitely, is a run-rate a shelf whose paid leader tops out at five-figure
lifetime installs cannot supply. The sat-shelf correction does **not** rescue this shape — it is about
*light-planning* discovery, and the astrophotography query is polluted (4/30) besides.

### Shape B — `night-sky-session-planner` (general enthusiasts)

```text
price        = $4.99
net          = 4.99 × 0.85 × 0.92 = 3.90
sales/mo     = 500 / 3.90         = 128.2/mo → 129 sales/month
```

**REJECT — observed price is $0.** §1.2/§1.3: five of six apps free, leaders 10M+/50M+, the lone paid
listing a $2.99 upsell of a free app. An ads substitute also fails: banner-only needs ~11,000 DAU for
$1,000/mo (§2.1 of the strategy sheet), so $500 needs ~5,500 DAU — unattainable for a few-times-a-month
session planner.

### Shape C — `sun-moon-light-session-planner` (light planners, extended to the night) ★

```text
price        = $9.99
net          = 9.99 × 0.85 × 0.92 = 7.81
sales/mo     = 500 / 7.81         = 64.0/mo  → 64 sales/month
```

**RECOMMEND (recomputed, not re-affirmed).** The $17.99 fact **strengthens C without moving it**: C is
chosen because it is the only shelf with **standalone purchase-only paid anchors at six-figure installs**
(PhotoPills $10.99/100K+, Sun Surveyor $9.99/100K+, Sun Position PRO $9.99/50K+, Sun Seeker $8.99/50K+),
and the correction now also lets C claim **satellite passes as a real differentiator** — the light shelf
has **zero** satellite coverage (scout C §3) while §1.6 proves pass timing is payable. C is
Shape C *plus* the two capabilities the light shelf lacks (cloud cover, satellite passes), priced inside
the shelf's band.

### Shape D — `sky-event-and-satellite-planner` (CORRECTED)

```text
price        = $7.99
net          = 7.99 × 0.85 × 0.92 = 6.25
sales/mo     = 500 / 6.25         = 80.0/mo  → 80 sales/month
```

**REJECT — reason corrected from revision 1.** Revision 1 rejected D on *"no paid-upfront anchor
exists"*; that was wrong (§CORRECTIONS-1). D is rejected on **reach/discovery**: the only observed paid
anchor at a satellite price point is **ISS Detector Pro $17.99, sold through a free 5M+ parent's funnel**
(§2), and every other satellite app is free (§1.6). A **standalone** satellite planner competing for
search discovery has **no observed listing** that reaches the volume to sell 80/mo at $7.99 — the
shelf's monetisation is free-with-ads plus an in-funnel Pro unlock, which a new entrant cannot copy
without first owning a 5M+ free app.

### Shape E — `aurora-planner` (considered, rejected)

```text
price        = $3.99
net          = 3.99 × 0.85 × 0.92 = 3.12
sales/mo     = 500 / 3.12         = 160.3/mo → 161 sales/month
```

**REJECT — two reasons, the second decisive.** (1) Revenue: §1.7 shows an ad-led free leader at 5M+ and
the paid comparable a $3.99 *Pro variant of that same free app* at 50K+ — the shelf's top standalone
price cannot carry 161/mo. (2) Honesty: aurora needs space-weather data (Kp/solar wind) this engine
does not compute, so it is not the same engine repositioned.

### Side by side

| Shape | Price | net = p×0.85×0.92 | sales/mo for $500 | Verdict |
|---|---:|---:|---:|---|
| A astrophotography-session-planner | $10.99 | 8.59 | 59 | reject — reach |
| B night-sky-session-planner | $4.99 | 3.90 | 129 | reject — observed price $0 |
| **C sun-moon-light-session-planner** | **$9.99** | **7.81** | **64** | **recommend** |
| D sky-event-and-satellite-planner | $7.99 | 6.25 | 80 | reject — reach (corrected) |
| E aurora-planner | $3.99 | 3.12 | 161 | reject — price ceiling + wrong data model |

---

## 4. The decision — price, shelf, sentence, and the division

**The target is $500 net per month** — the owner's stated modelling target, unchanged by this revision;
it is a **target, not an observation** (no revenue API exists). $500/mo × 12 = **$6,000/year net**.

**Chosen price: $9.99.** Defended against both anchors:

- **The $10.99 light-shelf ceiling.** $9.99 is the shelf's **modal** price — Sun Surveyor ($9.99,
  100K+, 4.8), **Sun Position PRO ($9.99, 50K+, 4.9 — the highest-rated paid planner on the shelf)**,
  Planit Pro ($9.99). The only app at the $10.99 ceiling is PhotoPills, the shelf's most feature-dense
  multi-tool (exposure/DoF/FoV/hyperfocal calculators). This engine adds two capabilities the whole
  shelf lacks, but scout C labels bundle willingness-to-pay **UNVERIFIED** — so pricing **at the
  ceiling held by the most established incumbent** would be an unmeasured bet against the shelf's own
  strongest-rated price point. $9.99 is the defended price; $10.99 is the *maximum* if the bundle
  proves out **after** ship.
- **The $17.99 satellite anchor.** Does **not** transfer (§2): it is an in-funnel Pro upgrade of a 5M+
  free app, not a standalone listing competing on discovery.

**Shelf:** the **light-planning photography shelf** (Sun Surveyor / Sun Position PRO / Planit Pro /
Sun Seeker / SunOnTrack / PhotoPills), entered with night, cloud and satellite as the differentiators.

**Single listing sentence:** *"Plan the light and the sky in one brief — Sun, Moon, twilight, Milky Way
darkness, hourly cloud cover and satellite passes for your location, for the whole session, not just the
golden hour."*

**The division, shown (purchase-only, first-$1M tier):**

```text
price                       = $9.99
Google fee 15%              = 9.99 × 0.15            = $1.4985
after fee                   = 9.99 × 0.85            = $8.4915
tax/refund/FX haircut 8%    = 8.4915 × 0.08          = $0.6793
net per sale                = 9.99 × 0.85 × 0.92     = $7.81
sales needed per month      = 500 / 7.81             = 64.0  → 64 sales/month
annual to clear $6,000 net  = 6,000 / 7.81           = 768 sales/year
```

**Same arithmetic at the other candidate prices (division shown):**

```text
at $10.99: fee 10.99×0.15=1.6485 → 10.99×0.85=9.3415 → ×0.92 = 8.59 → 500/8.59 = 58.2 → 59/mo
at $17.99: fee 17.99×0.15=2.6985 → 17.99×0.85=15.2915 → ×0.92 = 14.07 → 500/14.07 = 35.5 → 36/mo
at  $7.99: fee  7.99×0.15=1.1985 →  7.99×0.85=6.7915 → ×0.92 = 6.25 → 500/6.25 = 80.0 → 80/mo
at  $9.99 with a 30% fee (higher tier / existing-install case):
          9.99×0.30=2.997 → 9.99×0.70=6.993 → ×0.92 = 6.43 → 500/6.43 = 77.7 → 78/mo
```

The 30 %-fee row is the honest downside: at the higher tier the same app needs **78 sales/mo, not 64**,
exercised in §7's kill criteria.

**What the user buys (paid unit):** one lifetime unlock of the complete session-brief planner — the
combined brief (Sun, Moon, twilight, dark window, cloud cover, satellite passes, exposure guidance) for
any location and date, with the share/export handoff, and no ads.

## 5. Archetype

**`purchase_only_no_ads`** — one of the four live archetypes in `scripts/lib/slot-economics.mjs`
(`purchase_led_single_unlock` | `purchase_led_repeat_sales` | `ads_led_utility` |
`purchase_only_no_ads`). **Subscription is banned and is proposed nowhere.**

**Why it, from this shelf's own evidence:** scout C measured the light shelf's paid apps as **ads = none
and (except Planit Pro) IAP = none** — PhotoPills, Sun Surveyor, Sun Position PRO, Sun Seeker, SunOnTrack
are all purchase-only, no-ads standalone listings; I re-verified the first three/two myself (§1.4). The
model that has actually worked on this shelf is a paid app with no ad inventory and no in-app store, so
it is the archetype the evidence supports, and it is the archetype the $17.99 anchor also uses (no ads,
no IAP).

**Rejected alternatives, and the fact that separated them:**

- **`purchase_led_single_unlock`** (free app + one-time unlock). Rejected because it rests entirely on an
  **install→purchase conversion rate that this session did not observe** (`UNVERIFIED`), and because the
  light shelf's paid apps are purchase-only, not free+unlock. It is the shape of ISS Detector Pro — but
  §1.6 shows that Pro rides a **free 5M+ funnel**; a new standalone app has no such funnel, so the
  conversion the model needs is exactly what an entrant cannot supply. Reconsider only with a measured
  conversion rate.
- **`ads_led_utility`.** Rejected on the strategy sheet's own arithmetic (§2.1): banner weight implies
  ~11,000 DAU for $1,000/mo, i.e. ~**5,500 DAU** for $500 — and a session planner cannot hold 5,500 DAU,
  nor offers anything worth a rewarded unlock (the job is one brief per trip). The light shelf shows
  *zero* ads on its paid rows, so ads are not this audience's model.
- **`purchase_led_repeat_sales`.** Rejected because there is **nothing to re-sell**: the brief is
  produced per trip; the forecast and passes are fetched, not sold; there is no consumable the job
  contains. Selling one would be a feature the job does not have.

## 6. What the listing must NOT claim

- **Not a camera; takes no photographs.** No "night sky camera" / astrophotography-as-capability claim
  (§1.1 owns that: DeepSkyCamera, AstroCam).
- **Not a star map / planetarium.** No "star map", "sky map", "planetarium", "point at the sky" (§1.3
  leaders own those words; the engine is ephemeris math, not a rendered sky).
- **Not a satellite tracker as its primary job.** Satellite passes are a differentiator; the app must not
  headline as "ISS tracker" or claim to be ISS Detector/Look4Sat (§1.6).
- **Not aurora.** No "aurora" / "northern lights" — there is no space-weather input (Shape E).
- **Cloud cover is a forecast, not a measurement.** Say "forecast cloud cover", never "guaranteed clear
  skies" or an accuracy the provider does not state.
- **No borrowed authority** — no "NASA", "ESA", "official observatory". (Spot the Station is NASA's; we
  are not.)
- **No offline claim** unless the app genuinely works without the forecast feed.
- **No keyword stuffing** (a Play violation) — do **not** build the title from the query words in §1.
- **Title ≤ 30 characters** (Play's hard cap, full-width counted the same). Illustrative in-cap candidate
  — **not a naming decision**, which is the naming contract's and the owner's: **`Sky Session Brief`**
  (17 chars).

## 7. Kill criteria — exactly three, with thresholds and the reading that decides

Shaped for `killCriteria` in `economics.json` (`windowDays`, `minInstalls`, `minRevenueUsd` are its
three integer keys). For a **purchase-only** app an install **is** a purchase, so `minInstalls` and units
sold are the same signal.

```jsonc
"killCriteria": { "windowDays": 120, "minInstalls": 150, "minRevenueUsd": 1000 }
```

1. **Paid installs ≥ 150 by day 120.** Reading that decides: **Play Console → Statistics (acquisition)**,
   device install events. 150 / 120 days ≈ **37.5/mo** — **59 %** of the 64/mo the $500 target needs, so
   it is the floor at which the run-rate is close enough to chase and far enough to stop.
2. **Net revenue ≥ $1,000 by day 120.** Reading that decides: **Play Console → Financial reports**, net
   figure. `150 × $7.81 = $1,171 ≥ $1,000`, so this floor and the install floor **agree** instead of
   contradicting each other. (At the 30 %-fee tier the same 150 installs yield `150 × 6.43 = $965 <
   $1,000`, which is the deliberate tension the two floors expose — it is a `MISSED`, and the reason is
   recorded.)
3. **Usage: ≥ 50 % of buyers generate at least one session brief within 30 days of purchase, by day
   120.** Reading that decides: **Play Console → Statistics / an in-app event count**, with the buyer
   base named in the reading's `notes`. A paid planning app that is bought and never opened is a
   refund-and-1-star shape; this is the early-warning reading, and it is a **reading, never a pass**.

On day 120 a reading missing either of the first two floors is `MISSED`, and the archive command is the
one the outcomes gate prints: `node scripts/kiln-status.mjs --kill <slug> --reason "<why>"`. `NO-DATA` at
day 120 is also a failure of the plan, not a pass.

## 8. The honest limit

- **No revenue API exists.** `docs/monetization-owner.md` records that the factory's Play API surface
  cannot serve these reports, so every revenue figure here is **modelled from a price and two declared
  deduction rates — not observed**. `UNVERIFIED` throughout the demand side.
- **The demand-side claim — that this shelf pays 64/month — is `UNVERIFIED`.** No pricing experiment, no
  cohort, no comparable per-month sale rate was measured. C is the shape with the strongest *observed
  price evidence*; that is not evidence 64 sales/month will arrive.
- **`F_billing = 0.15` is tier-dependent** (§4 shows the 30 %-fee downside).
- **Install buckets are rounded** ("100K+", "10M+"), so reach statements are bands, not counts; the
  ~2 % ISS-Detector-free→Pro ratio is `INFERRED` from two rounded buckets, not measured.
- **Bundle willingness-to-pay for satellites/cloud in a light-planner is `UNVERIFIED`** (scout C §5/§3:
  no incumbent sells them). The commands that would settle it: Play's full paid-planner review lists
  (only 3/page are server-rendered), or a post-ship pricing/telemetry read.
- **Weak query recall** on `astrophotography planner` (4/30), `stargazing` (13/30) and **`iss detector`
  (2/20)** means the shelves priced for Shapes A, B and D are fuzzier than the clean light shelf (§1.4).
- **Build time, agent hours and wall clock are not criteria here** — per the owner's 2026-10-08
  instruction the decision is the revenue arithmetic alone.

---

```text
CLAIM / LABEL / EVIDENCE
```
| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| ISS Detector Pro is paid $17.99, 100K+, 4.4/3,190, no ads/IAP | MEASURED | `enrich-market.mjs --query "iss detector" --top 6 --concurrency 2` |
| Its free parent `com.runar.issdetector` is 5M+, 120,809 reviews, ads+IAP | MEASURED | same run |
| Revision 1's "six of six free" was a top-6 depth artifact of `satellite tracker` | MEASURED | same run vs rev-1 §1.6 window in §1.6 above |
| PhotoPills is $10.99, 100K+, 4.8/8,220, no ads/IAP | MEASURED | `enrich-market.mjs --query "photopills" --top 4 --concurrency 2` |
| Sun Position PRO is $9.99, 50K+, 4.9/2,730, no ads/IAP | MEASURED | `enrich-market.mjs --query "sun position pro" --top 4 --concurrency 2` |
| Sun Surveyor is $9.99, 100K+, 4.8/5,140, no ads/IAP | MEASURED | `enrich-market.mjs --query "sun surveyor" --top 6 --concurrency 2` |
| Sun Seeker is $8.99, 50K+, 4.8/1,000, no ads/IAP | MEASURED | same run |
| The light shelf's observed ceiling is $10.99; nobody above it | MEASURED | §1.4 rows + scout C §2 (re-verified rows above) |
| 0 of the paid light-planner rows declare ads | MEASURED | §1.4 `ads=false` on every paid row; scout C §1.1 |
| General night-sky leaders are free at 10M+/50M+ (Stellarium, Sky Map) | MEASURED | `enrich-market.mjs --query "stargazing"` and `--query "star map"` |
| The one paid general sky app is Star Walk 2 Pro $2.99, 500K+ | MEASURED | both runs above |
| Satellite shelf: every app outside ISS Detector Pro is free | MEASURED | `--query "satellite tracker"` + `--query "iss detector"` runs |
| Golden Hour+ (paid $4.99) peaks at 1K+ behind a free 500K+ leader | MEASURED | `--query "golden hour" --top 6 --concurrency 2` |
| Aurora paid ceiling is $3.99 / 50K+ behind a free 5M+ ad-led leader | MEASURED | `--query "aurora forecast" --top 6 --concurrency 2` |
| net = price × 0.85 × 0.92: $9.99→7.81, $10.99→8.59, $7.99→6.25, $4.99→3.90, $3.99→3.12 | MEASURED | arithmetic in §3–§4 |
| 64 sales/mo needed at $9.99; 78/mo at a 30% fee | MEASURED | §4 division: 500/7.81=64.0; 500/6.43=77.7 |
| 150 installs × $7.81 = $1,171 ≥ $1,000 | MEASURED | §7 arithmetic |
| F_billing = 0.15 is the first-$1M/new-install tier, rising to 0.20–0.30 + 5 % | ON RECORD | `docs/monetization-strategy.md` §2.2 assumption table |
| Banner-only implies ~11,000 DAU for $1,000/mo (≈5,500 for $500) | ON RECORD | `docs/monetization-strategy.md` §2.1 |
| The light shelf has zero satellite and zero cloud-cover coverage | ON RECORD | scout C §3 keyword scan of 10 descriptions (`C-sun-moon-light-shelf.md`) |
| The four archetypes are the live set; subscription is banned | ON RECORD | `docs/slot-economics.md` §ARCHETYPES / §Subscription |
| $17.99 is an in-funnel Pro upgrade, not a standalone paid listing | INFERRED | §2: 5M+ free parent (ads+IAP) → 100K+ Pro (paid) vs the light shelf's standalone paid listings |
| The free→Pro ratio is ~2 % | INFERRED | 100K+/5M+ from two MEASURED rounded buckets |
| The light shelf is the only shelf with standalone purchase-only anchors at 6-figure installs | INFERRED | §1.4 vs §1.1–§1.3, §1.6–§1.7 |
| The shelf will actually supply 64 sales/month for Shape C | UNVERIFIED | no pricing experiment/cohort/per-month rate observed; no revenue API; the command that would settle it is a post-ship Play Console read |
| Willingness-to-pay for satellites+cloud bundled in a light-planner | UNVERIFIED | no incumbent sells them (scout C §3/§5); would need Play's full paid-planner review lists |
| A keyless public forecast feed keeps Shape C's recurring cost near zero | UNVERIFIED | no service evaluated this session; confirm at build time or the shape fails |
| Install→purchase conversion for `purchase_led_single_unlock` | UNVERIFIED | no conversion rate observed in this scan |
| The usage reading "≥50% of buyers generate a brief within 30 days" is attainable | UNVERIFIED | a product-health target, not a measured norm |

# Money-owner stress test — UnitWalk — 2026-10-05

Plan under test: `apps/2026-10-05-unit-walk/docs/unit-walk.economics.json`.  
Shelf: `docs/research/2026-10-05-solo-field-trade-recon.md`.  
Scout B: `docs/ideation/2026-10-05-scout-b-hobby-health-refusal.md` (no second recipe).

## Validation

```text
node scripts/check-economics.mjs --economics apps/2026-10-05-unit-walk/docs/unit-walk.economics.json
→ PASS economics schema — archetype purchase_led_single_unlock, priceUsd 9.99
→ SKIP capability pairing (no slot yet)
→ PASS evidenceRef resolves
→ VERDICT: PASS
```

## Recomputed arithmetic (list $9.99)

| Fee assumption | Net / sale | Sales / mo ≈ $100 net | Sales / mo ≈ $1000 net | Unlocks to clear kill $80 |
|---|---:|---:|---:|---:|
| **15%** (plan default, first-$1M / new-install) | **$8.49** | **≈12** | **≈118** | **≈9.4** |
| **30%** (stress: above $1M / standard rates) | **$6.99** | **≈15** | **≈144** | **≈11.4** |

Exact: `9.99 × 0.85 = 8.4915`; `9.99 × 0.70 = 6.993`. Rounded to cents for the table.

Kill pairing at assumed 2% convert: 800 installs → ~16 unlocks → ~$136 net @15% (clears $80). At 1% convert: ~8 unlocks → ~$68 (MISSES). Conversion is UNVERIFIED.

## What was kept, and why (not fantasy vs MEASURED shelf)

| Field | Decision | Why |
|---|---|---|
| **Archetype** `purchase_led_single_unlock` | **kept** | Enriched top ten: **0/10 ads**; leaders free-to-download; myInspections / Property360 / PropertyLog declare IAP. Ads mid-deposit-walk would earn nothing. |
| **priceUsd 9.99** | **kept** | No MEASURED IAP dollar on the shelf — not a priced comparable, an ASSUMPTION. $9.99 sits in the factory's common one-time band; lowering without a measured floor would be guesswork. Fantasy would be a $49 SaaS-tier price with no buyer signal. |
| **kill 800 / $80 / 60d** | **kept** | Soft floor relative to the ~$100/mo illustrative (that needs ~12 sales/mo ≈ 24 in window). RentCheck **100K+ / 7.2K reviews** proves category demand; Property Inspect **2.1★/42** and Yardi **1.5★/44** prove a weak mid-tier. PropertyLog **1+** is discovery risk for *this* offline shape — the kill exists to catch that, not to forecast it. Raising the kill to match $1000/mo would kill every cold niche listing. |
| **subscription** | **rejected / banned** | Owner 2026-10-01; absent from ARCHETYPES. |
| **ads_led / repeat_sales / purchase_only** | **rejected in revenue terms** | See `archetypesRejected` in the plan — shelf money shape, no consumable, free+IAP funnel. |

Assumptions in the plan JSON were rewritten to carry the 15% vs 30% division and the $100 / $1000 sales figures explicitly.

## Rank — one live recipe vs Scout B refusal

| Candidate | Verdict | Revenue reason |
|---|---|---|
| **unit-walk** | **Recommend for Checkpoint 1** | Measured demand (RentCheck 100K+), weak mid-tier anger (Property Inspect / Yardi), freemium IAP path already on shelf, offline no-account wedge, ~5 agent hours, no server. Risk is conversion + PropertyLog-shaped discovery, stated as UNVERIFIED — not a revenue refusal. |
| Scout B hobby/health pockets | **Refuse (already)** | Fortress / dead-zone / thin mid-tier / subscription-shaped pet-med — no proposable recipe; correct revenue refusals. |

**Recommendation: build UnitWalk if the owner accepts the price and kill at Checkpoint 1.** There is no second live recipe from this `/forge` recon.

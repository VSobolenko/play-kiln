# M1 — fal.ai provider prices, MEASURED first-party

**This document closes S2 §7.1.** S2 recorded every `Enhanced` operation as `enabled: false` because
the exact per-call price of each fal.ai endpoint was `UNVERIFIED`: the S2 worker held no fal API key and
fal's Platform Pricing API requires authentication. The orchestrator holds the shared account key, and
S2 §7.1 named exactly this query as the orchestrator's job:

> *"the orchestrator (who coordinates the shared account) runs one authenticated, free metadata query —
> `GET /models/pricing?endpoint_ids=...` — covering all 7–10 candidate ids at once; this is not a
> generation call and costs nothing per fal's own pricing model."*

## The query that produced this table

```bash
# The parameter is singular: `endpoint_id`. A repeated `endpoint_ids` is rejected with
#   {"error":{"type":"validation_error","message":"Invalid request parameters"}}  HTTP 400
# One endpoint per request. This is a metadata read, NOT a generation: zero cost, no model executed.
curl -sS -H "Authorization: Key $FAL_KEY" \
  "https://api.fal.ai/v1/models/pricing?endpoint_id=fal-ai%2Fflux%2Fdev"
```

Observed response shape:

```json
{"prices":[{"endpoint_id":"fal-ai/flux/dev","unit_price":0.025,"unit":"megapixels","currency":"USD"}],"next_cursor":null,"has_more":false}
```

The API rate-limits after roughly six requests in a burst (`{"error":"Too Many Requests","message":"Rate limit exceeded"}` HTTP 429). Space requests ~20 s apart.

## Measured prices — every row MEASURED 2026-10-15, HTTP 200

| Op | `endpoint_id` | `unit_price` USD | `unit` | Derived per-call cost at reference size |
|---|---|---|---|---|
| O-01 | `fal-ai/any-llm/vision` | 0.01 | requests | **$0.010** (flat per request) |
| O-02 | `fal-ai/bria/background/remove` | 0.018 | generations | **$0.018** (flat) — confirms the S1 reader |
| O-07 | `fal-ai/flux/dev` | 0.025 | megapixels | $0.026 at 1024² (1.05 MP); $0.050 at 2 MP |
| O-08 | `fal-ai/iclight-v2` | 0.10 | megapixels | **$0.105** at 1024² (1.05 MP) |
| O-09 | `fal-ai/bria/expand` | 0.04 | generations | **$0.040** (flat) |
| O-10 | `fal-ai/flux-pro/kontext` | 0.04 | images | **$0.040** (flat) |
| O-11 | `fal-ai/clarity-upscaler` | 0.03 | megapixels | $0.031 at 1024²; **$0.126 at 2048² (4.19 MP)** |
| O-12 | `fal-ai/bria/eraser` | 0.04 | generations | **$0.040** (flat) |
| O-14 | `fal-ai/idm-vton` | 0.00125 | compute seconds | $0.075 at 60 s |
| O-14 | `fal-ai/image-apps-v2/virtual-try-on` | 0.04 | images | **$0.040** (flat) |
| O-14 | `fal-ai/cat-vton` | 0.00125 | compute seconds | $0.075 at 60 s |
| — | `fal-ai/bria/product-shot` | 0.04 | generations | **$0.040** (flat) |
| — | `fal-ai/flux-pro/v1.1-ultra` | 0.06 | images | **$0.060** (flat) |
| — | `fal-ai/recraft/v3/text-to-image` | 0.04 | images | **$0.040** (flat) |

## The two defects this measurement exposes

**1. `O-01` names an endpoint that does not exist.** `MODEL_REGISTRY['O-01'].endpointId` is
`'any-llm/vision'`, and the pricing API answers it with
`{"error":{"type":"not_found","message":"Endpoint(s) not found"}}` **HTTP 404**. The id that exists is
`fal-ai/any-llm/vision` (HTTP 200). O-01 is `enabled: true` in the registry, so a recognition request
would post to `https://queue.fal.run/any-llm/vision` and fail at run time. Its recorded cost is also
wrong: `costMicroUsd: 20_000` where the measured price is **$0.01/request = 10_000 µUSD**.

**2. `O-11` cannot fit its declared tier.** `clarity-upscaler` is priced **per output megapixel**
($0.03). Its declared ceiling is the *light* tier, ≤ $0.025 — which a single 1024² output already
exceeds at $0.031, and a 2K output exceeds by 5× at $0.126 (over the *premium* ceiling of $0.125 as
well). A per-megapixel price is not a per-call price: it cannot be bounded without a cap on output size.

## Tier fit (annex E ceilings: light ≤ $0.025, standard ≤ $0.05, premium ≤ $0.125)

| Op | Measured per call | Ceiling | Fit |
|---|---|---|---|
| O-07 | $0.026–$0.050 | standard $0.05 | ✅ at ≤ 2 MP output |
| O-08 | $0.105 | premium $0.125 | ✅ but tight (84 % of ceiling) |
| O-09 | $0.040 | standard $0.05 | ✅ |
| O-10 | $0.040 | standard $0.05 | ✅ |
| O-11 | $0.031 (1 MP) → $0.126 (4.19 MP) | light $0.025 | ❌ **exceeds at every usable size** |
| O-12 | $0.040 | standard $0.05 | ✅ |
| O-14 | $0.040 (`virtual-try-on`) / $0.075 (`idm-vton` at 60 s) | premium $0.125 | ✅ |

## What this document does not claim

Per-call cost is **INFERRED** from the measured unit price and a **stated reference output size**, not
measured by executing the model. A `megapixels`- or `compute seconds`-unit price is only exact once the
reference size/run time is fixed, which is why each row above names its reference. The flat
`generations` / `images` / `requests` rows are exact as recorded. No generation call was made to produce
this table; nothing here is a bill, and no output quality or fidelity number is claimed —
`guard` thresholds still need a paid, measured pass against real outputs before they can be treated as
more than the provisional defaults.

## Credit pricing is NOT decided here

This table is the **cost** half only. The credit price a user pays, and therefore the margin, is the
money owner's decision and the owner's checkpoint-1 approval. Nothing in this document authorises a
change to what a credit costs.

## Landed registry change

The server registry now uses the measured O-01 endpoint and price, records each measured operation's
unit and reference size, and enables O-07, O-08, O-09, O-10, O-12 and O-14. O-11 remains disabled
because its per-megapixel charge has no output-size cap; O-13 and O-15 remain unpriced and disabled.
`operationConfig()` still requires a configured fal key, and no operation's credit amount changed.

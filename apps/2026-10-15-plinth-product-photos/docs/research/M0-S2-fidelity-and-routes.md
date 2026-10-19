# S2 — Fidelity benchmark: catalog/pricing read + local controls

Worker: M0 S2 spike only (per `annex/M-spikes-and-reads.md` §S2 and `annex/E-pipeline-and-operations.md`
operation catalogue / fidelity guard). **No paid provider calls were made in this assignment.** Everything
below is either (a) a read of the provider's own pages/metadata, zero cost, no API key used, or (b) a local
computation over files already on disk in `.kiln/scratch/`. All outputs live only under
`.kiln/scratch/plinth-m0-s2/`. `models.json` was not touched — it does not exist as a standalone file yet
(it is only a code block inside `annex/E-pipeline-and-operations.md`; confirmed by
`grep -r "models.json"` across the repo, 3 hits, all three are annex docs, 2026-10-14).

## 0. Scope note on "no paid calls"

fal.ai's own docs (first-party, read this session) state that real-time, exact per-endpoint pricing is
only available through the authenticated **Platform Pricing API** (`GET /models/pricing` at
`api.fal.ai/v1`), which requires a valid fal API key (ON RECORD, fal.ai, `fal.ai/docs/platform-apis/v1/models/pricing`,
fetched 2026-10-14: *"Authentication: Required. Users must provide a valid API key."*). I hold no such key
and was not given one, and using one — even for a nominally free metadata call — would touch the shared
provider account the orchestrator must coordinate, so I did not attempt it. Per-model price badges on
`fal.ai/models/<id>` are also not present in the static HTML (they are rendered client-side by a React
tree streamed in further `<script>` chunks); confirmed by fetching the raw HTML of
`fal.ai/models/fal-ai/bria/eraser` to 28,000 characters and finding only nav/analytics markup, no price
text (2026-10-14). **Conclusion: exact first-party per-call prices for O-07..O-14 are BLOCKED this session**
without either spend authorization (API key) or a JS-executing browser, neither of which this assignment
permits. What follows is the honest ceiling instead: confirmed endpoint IDs, their input/output schema (so
the right op is being matched to the right model), and cost bounds from the one MEASURED analog this
session has plus clearly labelled secondary estimates — enough for the orchestrator to approve a bounded
spend, not enough to already have measured it.

## 1. Corpus (checked, not re-created)

`.kiln/scratch/plinth-bench-prep/` — MEASURED (directory listing, 2026-10-14): 30 JPEGs under `photos/`,
one per Amazon Berkeley Objects (ABO) `product_type` in `select_and_download.py`'s `TYPES` list, CC BY 4.0
(`LICENSE-CC-BY-4.0.txt`), selected deterministically (`SEED = 20261014`) by `select_and_download.py`, with
full per-photo provenance (`item_id`, `image_id`, dimensions, license, source URL, sha256) in `index.json`
(29,852 bytes, 30 entries). This is the correct, already-prepared S1/S2 corpus; I read it, I did not alter
it or `select_and_download.py`.

Hard-subset note: this ABO-derived corpus is **not** tagged for the S1 "≥ 8 hard" categories (glass,
jewellery, fur/knit, white-on-white, shiny metal, hand-held, transparent bag) — `index.json` has no such
field. That gap belongs to S1 (cut-out), not S2, which annex M scopes to "the same 30 photos" without a
hard-subset requirement; noted for completeness, not a blocker here.

## 2. Provider catalog read (fal.ai, first-party, zero cost)

Method: fal's own per-endpoint OpenAPI/queue metadata route
`https://fal.ai/api/openapi/queue/openapi.json?endpoint_id=<id>` — a public, unauthenticated, read-only
metadata document (confirms the endpoint exists and its exact input/output schema; it is **not** a model
execution and carries no generation cost). Each row below cites the `playgroundUrl`/`documentationUrl` fal
itself returns in `x-fal-metadata` for that id, fetched 2026-10-14.

| Op | What annex E wants | Candidate endpoint (confirmed to exist, first-party) | Input shape confirms fit? |
|---|---|---|---|
| O-07 | Generated scene, background only, no item in prompt | `fal-ai/flux/dev` (`category: text-to-image`) | ON RECORD: pure `prompt -> image`, no image input at all — matches "no item in the prompt" exactly. |
| O-08 | Relight | `fal-ai/iclight-v2` (`category: image-to-image`, `about: Inference`) | ON RECORD: `image_url` described as "URL of image to be used for **relighting**", plus `prompt`, `initial_latent` lighting-direction enum — direct fit. |
| O-09 | Expand canvas (background outpaint, item re-composited after) | `fal-ai/bria/expand` (`about: Image Expansion`) | ON RECORD: `canvas_size`, `original_image_size`, `original_image_location` params are exactly an outpaint-to-canvas contract — direct fit. Same Bria family as the already-MEASURED O-02 endpoint. |
| O-10 | Recolour item | `fal-ai/flux-pro/kontext` (`about: "FLUX.1 Kontext [pro] -- Frontier image editing model"`) | ON RECORD: instruction-following image edit (`prompt` + `image_url`, example prompt "Put a donut next to the flour"); recolour would be expressed as an instruction. **Weaker fit than the others** — no Bria-specific recolour endpoint was found; this is the best first-party candidate located, not a confirmed purpose-built recolour model. |
| O-11 | Upscale 2K/4K | `fal-ai/clarity-upscaler` (`category: image-to-image`) | ON RECORD: `upscale_factor` (1-4), diffusion-based (`num_inference_steps`, `creativity`, `resemblance` params) — fits the operation, but a diffusion upscaler is a heavier (likely pricier) route than a plain ESRGAN-style model; flagged for a cheaper-alternative check before committing (see §4). |
| O-12 | Erase region (masked inpaint) | `fal-ai/bria/eraser` (`about: Eraser`) | ON RECORD: `image_url` + `mask_url`, `mask_type: manual\|automatic` — direct fit, same Bria family as O-02/O-09. |
| O-13 | Ghost mannequin (garment, no model) | **none found** | Searched `fal-ai/cat-vton`, `fal-ai/idm-vton`, `fal-ai/image-apps-v2/virtual-try-on` — all three require a `human_image_url`/`person_image_url` (a person wears the garment); none produces a floating/ghost-mannequin garment shot with no model. No first-party endpoint matching "garment, no model" was located this session. |
| O-14 | Virtual model (garment on model) | `fal-ai/idm-vton` and `fal-ai/image-apps-v2/virtual-try-on` (and `fal-ai/cat-vton`) | ON RECORD: all three take `(person/human image, garment image) -> composited image` — direct fit; three real candidates exist, a later paid micro-benchmark should pick among them. |

Per-endpoint schema citations (fetched 2026-10-14, `fal.ai/api/openapi/queue/openapi.json?endpoint_id=...`):
`fal-ai/flux/dev`, `fal-ai/iclight-v2`, `fal-ai/bria/expand`, `fal-ai/flux-pro/kontext`,
`fal-ai/clarity-upscaler`, `fal-ai/bria/eraser`, `fal-ai/idm-vton`, `fal-ai/image-apps-v2/virtual-try-on`,
`fal-ai/cat-vton`, `fal-ai/bria/product-shot` (also confirmed to exist; considered and set aside for O-07/O-08
because its contract is "place this exact product cutout into a described scene," i.e. it still shows the
item in the prompt-conditioned composite, so it does not satisfy O-07's "no item in the prompt" rule, and it
is not a relighting-of-the-existing-shot op for O-08).

**What was not resolved:** exact `maxCostUsd` per call for every row above (blocked, see §0); and O-13's
endpoint id at all.

## 3. Cost bound (no first-party exact price obtained — see §0)

The only MEASURED, first-party, same-provider price point available this session is the sibling S1
worker's real, already-incurred run of the **O-02 analog** (same Bria infrastructure family):
`.kiln/scratch/plinth-m0-s1/results.json:3` — `"provider_price_usd_per_success": 0.018` for
`fal-ai/bria/background/remove`, MEASURED over real calls (status 200, `estimated_cost_usd: 0.018` per
row). This is **ON RECORD (first-party, this session)** evidence for the Bria family's price order of
magnitude only; it is **not** a substitute for measuring `bria/expand` or `bria/eraser` themselves — Bria
prices by endpoint, not by vendor.

Secondary (non-first-party, aggregator-authored, **do not use to accept/reject a route**) data points found
by web search, each labelled UNVERIFIED/ON RECORD (secondary) and listed only to bound planning risk:
- "Virtual try-on" family on fal commonly quoted at ≈$0.02–$0.04/image (softwr.com "Fal AI Pricing", costbench.com
  "fal AI Pricing 2026", both accessed 2026-10-14) — third-party aggregator pages, not fal.ai itself.
- General fal image-model pricing commonly described as "$0.02–$0.08 per image" or "$0.02–0.024/MP" for
  common families (same aggregator pages) — too coarse to assign a specific id a tier.

**Tier-ceiling risk flags** (annex E: light ≤ $0.025, standard ≤ $0.05, premium ≤ $0.125, all per successful call):
- O-09, O-12 (Bria family, standard ceiling $0.05): the one real Bria data point this session ($0.018) is
  well under $0.05; INFERRED low risk of exceeding ceiling, UNVERIFIED until these exact ids are queried.
- O-07 (`flux/dev`, standard ceiling $0.05) and O-10 (`flux-pro/kontext`, standard ceiling $0.05): "pro"-tier
  image-edit models are generically described by fal's own docs as priced "proportional to output
  resolution," with no fixed number recoverable here; flag as **at risk** of exceeding $0.05, needs a real
  query before committing, not after.
- O-11 (`clarity-upscaler`, diffusion-based, light ceiling $0.025 for the 2K variant / implicit standard-ish
  for 4K at 2 credits): a diffusion upscaler (28 steps default for comparable fal models) is architecturally
  heavier than a plain super-resolution network; **highest risk of exceeding its ceiling** of all eight ops.
  A cheaper non-diffusion alternative (e.g. an ESRGAN-family id) should be the first thing the paid
  micro-benchmark checks, before `clarity-upscaler`.
- O-08 (`iclight-v2`, premium ceiling $0.125) and O-14 (`idm-vton`/virtual-try-on family, premium ceiling
  $0.125): premium ceiling is generous relative to the secondary aggregator numbers quoted above (if those
  numbers hold even approximately); INFERRED low risk, UNVERIFIED.
- O-13: no route exists to price at all (see §2).

## 4. Calls/cost arithmetic for the full 30-photo S2 measurement (planning numbers, not a bill)

Per annex M §S2, S2 runs "the same 30 photos" through each Enhanced op candidate and computes annex E's
metrics; the identity and swap **controls add no extra provider calls** (see §5 — both were done locally,
zero cost, reusing pixels already on disk).

- **Base plan, one candidate endpoint per op:** 8 ops (O-07..O-14) × 30 photos = **240 calls**, if a
  candidate exists for every op. O-13 currently has 0 candidates, so the real number this session's
  catalogue supports is **7 ops × 30 = 210 calls** (O-13 excluded, BLOCKED).
- **Each extra candidate tested for an op** (e.g. checking a second virtual-try-on id, or a cheaper upscaler
  before `clarity-upscaler`) adds another 30 calls for that op.
- **Cost, using the tier ceiling as the worst-case bound per op** (the system design itself guarantees no
  accepted route is priced above its ceiling, so the ceiling is a true upper bound on what shipping this op
  could ever cost, not a prediction of the measured price):

  | Op | Tier | Ceiling $/call | × 30 photos |
  |---|---|---|---|
  | O-07 | standard | 0.05 | 1.50 |
  | O-08 | premium | 0.125 | 3.75 |
  | O-09 | standard | 0.05 | 1.50 |
  | O-10 | standard | 0.05 | 1.50 |
  | O-11 | light (2K) | 0.025 | 0.75 |
  | O-12 | standard | 0.05 | 1.50 |
  | O-14 | premium | 0.125 | 3.75 |
  | **Subtotal (7 resolvable ops, 210 calls)** | | | **$14.25 worst case** |

  Using the only real first-party analog instead (Bria family, $0.018/call, optimistic and only strictly
  applicable to O-09/O-12): 210 × $0.018 ≈ **$3.78 best case** if every op happened to price like Bria's
  cut-out model (it will not — `flux/dev`, `iclight-v2`, `clarity-upscaler`, `idm-vton` are different
  architectures). **Honest planning range: ≈$3.78–$14.25 for a single pass of 210 calls**, before O-11's
  likely need for a second (cheaper) candidate (+ up to $0.75–$1.50) and before O-13 has any route at all.
  This range is INFERRED/bounded, not MEASURED, and must not be read as a committed number — it exists so
  the orchestrator can size a shared-spend request across M0 workers before authorizing any of it.

## 5. Identity and cross-item-swap controls (purely local, zero cost — run this session)

Script: `.kiln/scratch/plinth-m0-s2/controls_local_zero_cost.py`. Output:
`.kiln/scratch/plinth-m0-s2/controls_local_results.json`. The script makes **no network or provider calls**
(`"made_network_or_provider_calls": false` is recorded in its own output as a self-check). It reads only
local files: the 30-photo corpus JPEGs (read-only) and the sibling S1 worker's already-produced,
already-paid-for cut-out masks (`.kiln/scratch/plinth-m0-s1/*.png`, read-only, reused rather than
re-requested).

**(a) Identity control (re-encode only).** For all 30 corpus photos: decode the JPEG, re-encode losslessly
to PNG in memory, decode that PNG, and measure mean/95th-pct/max CIEDE2000 (own implementation, standard
formula, no external colour library) between the two decodes.
**MEASURED result: mean dE00 = 0.000000 across all 30 photos (max across any single photo's mean = 0.000000).**
This is the expected floor for a true no-edit round trip and matches annex M's "(a) identity (re-encode
only) must score dE ≈ 0" requirement exactly — trivially true by construction here because no operation was
applied, which is the honest scope of a zero-cost local check: it proves the *measurement* reads dE≈0 for a
genuine no-op, not that a real provider relight/upscale call would. The real control for an *actual*
Enhanced-mode identity run (re-encode after a round trip through a provider endpoint) is still S2 work that
needs a paid call and is out of scope here.

**(b) Cross-item swap control.** Using the S1 worker's 29 valid RGBA masks (of 36 PNG files present; 7 were
not RGBA and were skipped — noted, not investigated further, as it is S1's artifact not mine to alter),
resized to a common 256×256 canvas (conservative: resizing can only inflate incidental overlap between
unrelated silhouettes, never deflate it, so this is a worst-case-for-rejection setup), computed binary-mask
IoU for every same-item pair (29, trivially 1.0) and every cross-item pair (29×28 = 812 ordered pairs).
**MEASURED result: same-pair IoU min = 1.0 (as expected); cross-pair IoU max = 0.7939** (the closest
confusable pair was `B07GNNPB3M`↔`B07ZFKMVL3`, i.e. a lamp vs a kitchen item in this corpus — see
`controls_local_results.json` top-5 list for all near-misses).
**Against annex E's guard thresholds (iou ≥ 0.95 for recolour, ≥ 0.97 for relight/upscale/erase):
0.7939 < both thresholds, so every one of the 812 cross-item pairs in this real-mask sample would be
rejected by the guard — the swap control passes 100 % at both thresholds on the data available.** This is a
genuine, real-pixel-derived result (reusing already-paid cut-out masks, not fabricated), not an estimate.

Caveat stated plainly: this swap control exercises the *mask-IoU* arm of the guard only (annex E's `iou`
row), using masks from the cut-out op (O-02), because no Enhanced-op outputs exist yet (no paid calls were
made). The guard's other rows (`dE`, `ssimGrad`, `text`, `core`) cannot be exercised locally because they
compare a *generated* result `G` to its source `R`, and no `G` exists without a paid call. The 95th-pctile
threshold-setting step in annex M ("set at the 95th percentile of the metric over results a reviewer labels
'same product'") also needs real Enhanced-op outputs and a human reviewer pass; it is BLOCKED here for the
same reason.

## 6. Decision ledger (per annex M §S2 default: "stays `enabled:false` in `/config` if inconclusive")

| Op | Candidate endpoint (first-party, confirmed) | Decision | Label |
|---|---|---|---|
| O-07 | `fal-ai/flux/dev` | **Route recommended, pending price confirmation** — schema fit is exact (no-item text-to-image); cost ceiling risk flagged §3. `enabled:false` until a real priced call confirms ≤ $0.05. | ON RECORD (endpoint id, schema) / UNVERIFIED (price) |
| O-08 | `fal-ai/iclight-v2` | **Route recommended, pending price confirmation** — schema fit is exact (`image_url` described as relighting input). `enabled:false` until priced ≤ $0.125 and guard thresholds are measured against a real output. | ON RECORD (endpoint id, schema) / UNVERIFIED (price, fidelity) |
| O-09 | `fal-ai/bria/expand` | **Route recommended, lowest risk** — same Bria pricing family as the one MEASURED analog this session ($0.018, well under the $0.05 ceiling it would need to clear), schema fit exact. Still `enabled:false` until its own exact price and fidelity are measured, per the rule that only measured numbers replace the provisional guard thresholds. | ON RECORD (endpoint id, schema) / INFERRED (price order of magnitude) / UNVERIFIED (exact price, fidelity) |
| O-10 | `fal-ai/flux-pro/kontext` | **Inconclusive** — best available candidate is a general instruction-edit model, not a purpose-built recolour endpoint; fit is plausible but weaker than the other rows. `enabled:false`. | ON RECORD (endpoint id exists) / UNVERIFIED (fit, price) |
| O-11 | `fal-ai/clarity-upscaler` | **Inconclusive, highest cost-ceiling risk** — diffusion-based upscaler likely priced above a plain super-resolution model; recommend checking a cheaper ESRGAN-family id first. `enabled:false`. | ON RECORD (endpoint id, schema) / UNVERIFIED (price; flagged at-risk) |
| O-12 | `fal-ai/bria/eraser` | **Route recommended, lowest risk** — same reasoning as O-09 (Bria family). `enabled:false` pending measurement. | ON RECORD (endpoint id, schema) / INFERRED (price order of magnitude) / UNVERIFIED (exact price, fidelity) |
| O-13 | none found | **BLOCKED** — no first-party endpoint located this session matching "garment, no model" (ghost mannequin). Per annex M default, stays `enabled:false`; Original mode unaffected. A further catalogue search (not attempted here beyond the ids checked in §2) is the next step, not a paid call. | UNVERIFIED (no candidate) |
| O-14 | `fal-ai/idm-vton` (or `image-apps-v2/virtual-try-on`, `cat-vton`) | **Route recommended among 3 candidates, pending price/fidelity confirmation** — schema fit exact for all three; which one to ship needs a priced, measured comparison. `enabled:false`. | ON RECORD (endpoint id, schema) / UNVERIFIED (price, fidelity, which candidate) |

**No operation is promoted to `enabled:true` by this spike.** Per annex M's explicit default
("Default if inconclusive: the op stays `enabled:false` in `/config`; Original mode is unaffected"), that
is also the correct outcome for the five "route recommended, pending ..." rows: a recommended *candidate*
is not a *measured* pass, and only a measured pass (real dE/ssimGrad/iou/text numbers against reviewer-set
95th-percentile thresholds, from a real priced call) is allowed to flip `enabled:true` or replace a
provisional guard threshold, per annex E ("The thresholds are provisional defaults until S2 records
measured values; the task that lands them must cite the S2 numbers.") None of that measurement was
performed here, by design (no paid calls authorized for this worker).

## 7. What this worker could not do, and why (honest unknowns carried forward)

1. **Exact per-call price for any of O-07..O-14.** Blocked by fal.ai's own authentication requirement for
   the Platform Pricing API (§0) and by the model gallery's client-side rendering of price badges. Next
   step: the orchestrator (who coordinates the shared account) runs one authenticated, free metadata query
   — `GET /models/pricing?endpoint_ids=...` — covering all 7–10 candidate ids at once; this is not a
   generation call and costs nothing per fal's own pricing model ("You pay only for successful outputs"),
   but it still needs the shared key, which is exactly the coordination this assignment told me to leave to
   the orchestrator.
2. **O-13's endpoint id.** Not found among the ids checked. Needs either a deeper catalog search (more
   candidate ids tried against the same free metadata route) or a first-party fal.ai model-gallery search
   with JS execution (unavailable to this worker).
3. **Real fidelity numbers (dE, ssimGrad, iou, text) for O-07..O-14 outputs**, and the 95th-percentile
   threshold-setting pass a human reviewer must do on "same product" results. Both need at least one real,
   priced call per op — explicitly out of scope for this assignment.
4. **Confirmation that `flux-pro/kontext` is actually how the product should express "recolour"** rather
   than a more constrained, Bria-family or ControlNet-style recolour endpoint that might exist under a name
   not yet tried. Flagged as the weakest row in §6, not asserted as the final choice.

## Claim-ledger summary (per label)

- **MEASURED**: 30-photo corpus file count and manifest (§1); identity-control dE00 = 0 across 30 photos
  (§5a); cross-item swap-control IoU max = 0.7939 over 812 real cross-pairs (§5b); existence/schema of every
  endpoint id listed in §2 (public OpenAPI metadata route, fetched 2026-10-14); the S1 sibling's own
  MEASURED $0.018/call for `fal-ai/bria/background/remove` (cited from its `results.json`, not re-measured
  by me).
- **ON RECORD (first-party)**: fal.ai's own pricing-model documentation (per-model fixed pricing exists;
  exact numbers need an authenticated query); the `models.json` seed text in `annex/E-pipeline-and-operations.md`.
- **ON RECORD (secondary)**: third-party aggregator price estimates in §3, explicitly not used to accept/reject
  a route.
- **INFERRED**: cost-ceiling risk ranking in §3 and the $3.78–$14.25 planning range in §4, both bounded
  reasoning from the one real analog and the tier ceilings, not measurements.
- **UNVERIFIED**: exact per-call price for every O-07..O-14 candidate; O-13's endpoint id; which of the
  three O-14 candidates to ship; whether `flux-pro/kontext` is the right O-10 route.

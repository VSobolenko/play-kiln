# E — Pipeline, operations and the fidelity guard

## The invariant (constitution, design D1)

> **Original mode never alters the item.** The only operations applied to item pixels are one geometric
> resample (when the output scale ≠ 1) and alpha blending inside the edge band. No generative model
> receives the item's pixels as something to redraw, and the final image is always produced by
> compositing the *source pixels* over the background on the device.

Precise statement used by the test (`PipelineInvariantTest`):

- Let `S` be the source (sRGB, EXIF-oriented), `M` the 8-bit mask, `core = erode(M ≥ 250, 2 px)`.
- Compose at scale `1.0` on a canvas at least as large as the item box, with any `Look`.
- **For every pixel `p` in `core`, `out(p) == S(p)` byte for byte** (all three colour channels).
- At scale `s ≠ 1`: for every `p` in `core`, `out(p) == resample(S, s)(p)` where `resample` is the single
  documented resampler (Lanczos3 down, Mitchell up), checked against an independent reference
  implementation inside ΔE₀₀ ≤ 0.5 (rounding only).
- No sharpening, tone curve, colour management change or denoise touches `core` in Original mode.

The edge band is `dilate(M>0, 1) ∖ core`. Edge decontamination (removing background colour spill) and
feathering (radius `0.75 px`) run in the edge band only.

## Composition steps (`domain/pipeline/Compositor`)

1. **Orient and normalise.** Apply EXIF orientation; convert any Display-P3/AdobeRGB source to sRGB at
   import using the embedded ICC profile (E17); drop alpha from JPEG/HEIC sources.
2. **Box.** `B = bbox(M > 8)`; if `B` is empty → the item is "not found" (E08).
3. **Canvas.** `(w_c, h_c)` = the Look's canvas override or the first selected preset's `recommended`
   (fallback `1200×1200` when the preset has none).
4. **Margin and fill.** `m = round(marginPct · min(w_c,h_c))`; available box
   `(w_c − 2m, h_c − 2m)`; scale
   `s = min((w_c − 2m)/w_B , (h_c − 2m)/h_B)`; then `s = min(s, s_fill)` where `s_fill` makes
   `max(w_B·s/w_c , h_B·s/h_c) = fillTarget`. **`fillRatio` is bbox long-axis occupancy** — the
   marketplaces' "fills 85 % of the image" is ambiguous and this is the app's declared reading (DECISION);
   L08 uses the same definition. If `s > 2.0` the item is shown with a "small source" warning (E09).
5. **Placement.** Item box centred horizontally; vertically centred unless the Look's `shadow.kind != NONE`,
   in which case it sits `0.02·h_c` above centre so the shadow has room.
6. **Background.** Render `BackgroundSpec` at canvas size (procedural scenes are generated from a seed in
   `domain/pipeline/Scenes.kt`; generated scenes are scaled with cover-fit and cropped).
7. **Shadow** (`ShadowSpec`): `CONTACT` = take `M`, scale vertically to `0.15`, place under the item's
   bottom edge, Gaussian blur `σ = blurPct · w_c`, opacity `opacity`; `DROP` = full silhouette offset by
   `offsetPct`, blur and opacity as given. Shadow is composited on the background only, never over the item.
8. **Item.** Resample once to `s`, blend with `M` (edge band only), over the shadowed background.
9. **Output.** Encode per the marketplace rule (format, quality search to satisfy `maxBytes` and the quality
   floor), strip EXIF when `strip_exif`, write provenance metadata (annex H).

## Cut-out engines (`domain/pipeline/Segmenter`)

`interface Segmenter { suspend fun mask(source: Bitmap): Mask }`. Implementations: `ServerSegmenter`
(operation O-02, `fal-ai/bria/background/remove`, MEASURED pixel-preserving: the returned RGB equals the
source inside the opaque interior, the app uses only its **alpha** and re-composites its own pixels) and an
optional `LocalSegmenter`.

**Rule for the local engine (SPIKE S1, DECISION default):** an on-device engine is shipped only if it is a
**stable** (non-beta) artifact (`dependency-freshness.mdc`; ML Kit `play-services-mlkit-subject-segmentation`
is `16.0.0-beta1`, dated 2023-11-07 → not eligible) **and** passes the S1 table within the numbers in annex
M. If none passes, the cut-out is a server operation costing the `light` tier (1 credit) and the first
10 starter credits cover ten items. The product never claims a free cut-out unless S1 passed.

Mask refinement (local, free, undoable): a brush that adds/removes mask alpha with a feathered stroke, a
"smart edge" snap that moves alpha toward the nearest luminance edge within 6 px, and "invert region".
Every stroke is a recorded step; undo/redo restores the previous mask file revision.

## Operation catalogue

Tiers: **light = 1 credit, standard = 2, premium = 5.** Provider-cost ceiling per successful call:
light ≤ $0.025, standard ≤ $0.05, premium ≤ $0.125 (so price/cost ≥ 3.2 at $0.080 per credit, above the
3.09 floor of `docs/monetization-strategy.md` §3.7). A model route above its tier ceiling is rejected at
server config load.

| Id | Operation | Where | Credits | Provenance of output | Guard | Marketplace gate |
|---|---|---|---|---|---|---|
| O-01 | Recognise item (category, attributes, scale/SKU text) | server, vision model | 0 (absorbed, ≤ 1 call per item) | — | n/a | none |
| O-02 | Cut-out (mask) | server (or local per S1) | 1 | — | alpha sanity (E08) | disabled on `original-only` |
| O-03 | Refine mask | local | 0 | — | — | disabled on `original-only` |
| O-04 | Compose card (Original) | local | 0 | `ORIGINAL_PIXELS` | invariant test | allowed everywhere; on `original-only` only crop/rotate/exposure |
| O-05 | Shadow, margin, fill | local | 0 | `ORIGINAL_PIXELS` | — | as O-04 |
| O-06 | Solid, gradient, procedural scene | local | 0 | `ORIGINAL_PIXELS` | — | per preset `backgroundPolicy` |
| O-07 | Generated scene (background only, no item in the prompt) | server, text-to-image | 2 per scene, reused across a Look | `COMPOSITE_GENERATED_BACKGROUND` | scene sanity (no text, no person) | not for MAIN where `white-255` |
| O-08 | Relight | server | 5 until measured (S2) | `ENHANCED` | full guard | not offered on `original-only` or Amazon-style MAIN |
| O-09 | Expand canvas (background outpaint; the original item is re-composited afterwards) | server | 2 | `COMPOSITE_GENERATED_BACKGROUND` | invariant re-check on core | as O-07 |
| O-10 | Recolour item | server | 2 per variant | `ENHANCED` | structure guard (hue may differ) | not on MAIN where "real photograph" is required |
| O-11 | Upscale 2K / 4K | server | 1 / 2 | `ENHANCED` | full guard | allowed (resolution fix for L03) |
| O-12 | Erase region (masked inpaint; outside the mask re-composited from source) | server | 2 | `ENHANCED` inside the region only | outside-region identity | as O-08 |
| O-13 | Ghost mannequin (garment) | server | 5 | `ENHANCED` | full guard + label text | as O-08 |
| O-14 | Virtual model (garment on model) | server | 5 per variant | `ENHANCED` | garment-print guard | not on `original-only` |
| O-15 | Listing text (6 languages, within limits) | server, text model | 1 per item | — | length enforcement is local | none |
| O-16 | Series (n variants of O-10/O-14) | server | n × per-variant, reserved once | as the variant | each variant guarded | as the variant |
| O-17 | Slide render (annex I) | local | 0 | `ORIGINAL_PIXELS` or inherits the card | text categories (L11) | per preset |
| O-18 | Export, pack, ZIP | local | 0 | inherits | linter L01–L20 | per preset |

**Series pricing (DECISION, replaces "charged once").** A series of `n` is *reserved* once as one ledger
entry of `n × per-variant credits`, settled **per delivered variant**, and the unused remainder released:
a series that delivers 3 of 4 charges 3. This keeps the arithmetic safe (price/cost does not change with
`n`) and honours "failure never charged".

**Cost transparency.** Before any server operation the UI shows its credits (`API-13`), the balance after,
and the provenance label of the result; above `confirm_spend_over` it asks. Free operations show "free,
on this phone".

## Auto-card (`domain/autocard/AutoCard`)

Input: photo(s), selected marketplace(s). Steps (the "smart button" one-tap flow, `S-04`):

1. O-02 cut-out (or local). 2. O-01 recognise → `category ∈ Categories` (21 ids, closed set below) +
attributes; the category chip is editable (E10). 3. Pick the `SceneRecipe` for `(category, marketplace
mode)`: for `white-255` MAIN → solid `#FFFFFF`; for other MAIN → the recipe's calm default; for EXTRA slots →
the recipe's scene (procedural first; generated O-07 as an offered upgrade at 2 credits). 4. Compose
(Original). 5. Linter on the result; offer the fix. The result is a `Look` the user can **save** and apply
to the batch.

`Categories` (closed, localised): `clothing_top, clothing_bottom, dress, outerwear, shoes, bag,
accessory, jewelry, watch, cosmetics, skincare, packaged_food, beverage, electronics_small, home_decor,
kitchenware, toy, book, furniture_small, tool, other`. `assets/scene-recipes.json` maps each to ordered
recipes: `{sceneId, kind: PROCEDURAL|GENERATED, promptTemplate?, shadow, fillTarget, notes}`; the builder
writes the table in M1 with at least one procedural recipe per category and three generated-prompt
templates per category, verified by a test that every category resolves to a non-empty recipe list and that
no `promptTemplate` mentions the item.

**Recognition call** (`O-01`): the client sends one 1024 px JPEG; the server calls the vision model with
temperature 0 and a JSON schema `{category: enum, confidence: 0..1, attributes: string[≤8],
visibleText: string[≤6], scaleHint: {kind: "none"|"ruler"|"coin"|"hand"|"label", valueMm?: number},
sku: string?}`. A response that fails the schema is retried once, then treated as `category = other`.
Dominant colours are computed on-device by k-means on item pixels (not by the model).

## Fidelity guard (`server/guard`)

Runs after every `ENHANCED` or `COMPOSITE_*` job with source reference `R` (the uploaded image) and result
`G`; computed at 512 px on the long side with `sharp`.

| Metric | Definition | Default threshold (SPIKE S2 replaces with measured) |
|---|---|---|
| `dE` | mean CIEDE2000 between `R` and `G` over the eroded item mask | relight ≤ 4, upscale ≤ 2, erase (outside region) = 0, recolour: n/a (hue may change) |
| `ssimGrad` | SSIM of gradient-magnitude images over the item mask | ≥ 0.90 |
| `iou` | silhouette IoU between `R`'s mask and the mask re-derived from `G` | ≥ 0.97 for relight/upscale/erase, ≥ 0.95 for recolour |
| `text` | normalised Levenshtein similarity of OCR text inside the item box, only if `R` has ≥ 4 text characters | ≥ 0.90; any brand token changed → reject |
| `core` | for `COMPOSITE_*` jobs the invariant `core(G) == core(R)` after re-composite | exact |

Policy: first rejection → one automatic retry with a new seed, still free; second rejection → job
`FAILED(GUARD_REJECTED)`, reservation released, the message recommends Original mode. Metrics are returned
in API-08 (`guard.metrics`) and shown in a details sheet. The thresholds are **provisional defaults** until
S2 records measured values; the task that lands them must cite the S2 numbers.

## Model registry seed (`server/config/models.json`)

```json
{
  "ops": {
    "O-01": { "tier": "free-absorbed", "endpointId": "any-llm/vision", "maxCostUsd": 0.02, "timeoutMs": 60000, "label": "MEASURED endpoint exists, ≈ $0.01 per call (earlier session probe)" },
    "O-02": { "tier": "light", "endpointId": "fal-ai/bria/background/remove", "maxCostUsd": 0.025, "timeoutMs": 60000, "label": "MEASURED pixel-preserving; price per call UNVERIFIED (S5)" },
    "O-07": { "tier": "standard", "endpointId": null, "maxCostUsd": 0.05, "timeoutMs": 90000, "candidates": "text-to-image family; choose at S2" },
    "O-08": { "tier": "premium", "endpointId": null, "maxCostUsd": 0.125, "timeoutMs": 120000, "candidates": "image-edit / relight family; choose at S2" },
    "O-09": { "tier": "standard", "endpointId": null, "maxCostUsd": 0.05, "timeoutMs": 90000, "candidates": "outpaint family; choose at S2" },
    "O-10": { "tier": "standard", "endpointId": null, "maxCostUsd": 0.05, "timeoutMs": 90000, "candidates": "image-edit family; choose at S2" },
    "O-11": { "tier": "light", "endpointId": null, "maxCostUsd": 0.025, "timeoutMs": 90000, "candidates": "upscale family; choose at S2" },
    "O-12": { "tier": "standard", "endpointId": null, "maxCostUsd": 0.05, "timeoutMs": 90000, "candidates": "inpaint family; choose at S2" },
    "O-13": { "tier": "premium", "endpointId": null, "maxCostUsd": 0.125, "timeoutMs": 120000, "candidates": "image-edit family; choose at S2" },
    "O-14": { "tier": "premium", "endpointId": null, "maxCostUsd": 0.125, "timeoutMs": 120000, "candidates": "virtual try-on family; choose at S2" },
    "O-15": { "tier": "light", "endpointId": "any-llm", "maxCostUsd": 0.025, "timeoutMs": 60000, "label": "text model via the same LLM gateway; verify at S3" }
  }
}
```

Only `endpointId` values read from the provider's own model page at S2 may replace `null`; a builder never
types an endpoint id from memory. `O-01`'s and `O-02`'s ids come from the session's earlier probes
(MEASURED), `O-15` is `UNVERIFIED` until S3 runs one text call.

## Provenance and entitlement

`ORIGINAL_PIXELS` covers an untouched item region over solid, gradient, gallery or deterministic
procedural backgrounds. A trained-model scene is `COMPOSITE_GENERATED_BACKGROUND`. Any tonal edit over
item pixels is `MANUALLY_ADJUSTED`; generative/restorative item edits are `ENHANCED`. `original-only`
evaluates the preset's exact `originalOnlyAllowed` list; allowed tonal edits remain manually adjusted.

Free entitlement permits batches of at most 5 and applies a visible Plinth watermark to exported pixels.
The unlock removes both limits after server confirmation. Preview watermarking never mutates sources.
A marketplace rule forbidding watermarks blocks free export with a purchase explanation.

## Honest limits

The guard bounds drift; it does not prove a generative result is "the same product". Original mode is the
only mode with a structural guarantee, which is why it is the default and the only one offered on
`original-only` marketplaces and `white-255` MAIN slots.

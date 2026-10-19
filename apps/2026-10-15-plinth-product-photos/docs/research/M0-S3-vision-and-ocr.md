# Plinth M0 S3 — vision and OCR evidence

Observed 2026-10-14. Research only; no paid inference calls and no accuracy result.

## Corpus disposition

The existing Amazon Berkeley Objects corpus at `.kiln/scratch/plinth-bench-prep/` contains 30 CC BY 4.0 product images selected for S1, not the approximately 50-item S3 set. Its Amazon `product_type` labels have no first-party crosswalk to Plinth's 21-category taxonomy; 12 Plinth categories have no representative. It has no verified scale-hint, SKU, ruler, visible-label or six-language text ground truth. Therefore category/attribute, scale/OCR, and O-15 accuracy cannot be truthfully measured from it. No separate S3 corpus was found.

## Candidate versions and routes

- Stable unbundled ML Kit text recognition: `com.google.android.gms:play-services-mlkit-text-recognition:19.0.1`, per Google's release notes. Subject segmentation remains beta (`play-services-mlkit-subject-segmentation:16.0.0-beta1`) and is not eligible as a stable local cut-out candidate.
- fal `fal-ai/any-llm/vision` machine-readable model page reports $0.01/request for standard models and $0.03 for premium models. Premium exceeds the annex E O-01 ceiling of $0.02. The human HTML page returned “This model is no longer supported”; endpoint availability is unresolved.
- fal `fal-ai/any-llm` machine-readable page reports $0.001/request standard and $0.01 premium. Human HTML page also returned “This model is no longer supported”; endpoint availability is unresolved.
- No inference calls were made. The six-language benchmark, including Cyrillic coverage and the WB 60-character title limit, remains unmeasured.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| S3 requires about 50 items across 21 categories, category accuracy >=85%, scale/SKU exact read >=80% where legible, and one six-language O-15 call. | ON RECORD | `openspec/changes/app-seller-photo-studio/annex/M-spikes-and-reads.md:44-50` |
| The S1 corpus is 30 items and lacks ground truth for S3 visual/text tasks. | MEASURED | `.kiln/scratch/plinth-bench-prep/index.json`; `select_and_download.py:17-18`; no S3 corpus found under `.kiln/scratch/` or `docs/research/` |
| ML Kit text-recognition 19.0.1 is listed as stable; subject segmentation 16.0.0-beta1 is not eligible. | MEASURED | https://developers.google.com/ml-kit/release-notes, accessed 2026-10-14 |
| O-01/O-15 published prices are $0.01/$0.001 standard, but HTML pages report unsupported. | MEASURED / UNVERIFIED availability | https://fal.ai/models/fal-ai/any-llm/vision/llms.txt and https://fal.ai/models/fal-ai/any-llm/llms.txt; human-readable pages; accessed 2026-10-14 |
| No S3 accuracy or provider inference result exists. | MEASURED | No paid calls made; no suitable labelled corpus |

## Route decision (2026-10-15)

**Orchestrator decision (not a measurement):** No Kotlin source referenced ML Kit. The S3 spike remains
unmeasured, and task 5.6 is gated by it. `slot.secrets` refuses `play-services-mlkit-*` on the client
classpath. Recognition will be re-decided at S3 (server-side route or a bundled on-device model); the
client carries no such SDK in the meantime.

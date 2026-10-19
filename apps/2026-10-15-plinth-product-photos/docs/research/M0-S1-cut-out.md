# M0 S1 — cut-out benchmark

**Verdict: no local cut-out engine is selected. Use the server cut-out at 1 credit; make no free/offline cut-out claim.**

The release-note check found no stable Android subject-segmentation artifact among the evidenced candidate(s): Google's ML Kit subject segmentation remains `16.0.0-beta1`, explicitly ineligible under this change's stable-only rule. Since no eligible local engine was identified, no local masks exist to score against BRIA or hand-corrected ground truth, and there is no local latency measurement on the designated `SM-A405FM`. The S1 local-engine shipping thresholds therefore cannot be demonstrated. This is the annex's default when no local engine passes; it is not a finding that every possible bundled/custom model has been surveyed or fails.

## Measured reference run

| Item | Result |
|---|---:|
| Prepared corpus | 30 photos; 1 logo/non-product exclusion; 29 eligible |
| Reference engine | `fal-ai/bria/background/remove` |
| Calls | 29/29 successful; no missing eligible outputs |
| Visually tagged challenge photos | 25/29 (tags were assigned from 390 px contact-sheet thumbnails, not full-resolution masks) |
| Recorded provider rate | $0.018/success |
| Total estimated provider charge | $0.522 |
| Additional calls in this continuation | 19; estimated $0.342 |
| BRIA response latency from VPS | median 2,400 ms; nearest-rank p95 5,298 ms; range 1,217–6,027 ms |
| Tagged-case BRIA response latency | median 2,295 ms; nearest-rank p95 3,524 ms |
| Source/output dimensions | matched for all 29 |
| SHA-256 integrity | all 29 source and output hashes matched their records |
| Opaque-interior RGB | 0 differences across 49,084,957 alpha-255 pixels |
| White/black opaque-interior composites | 0 differing pixels |

Response latency is measured by the remote runner from just before the provider POST until its response, excluding result-image download. It is a server/provider observation, **not** local A40 or phone inference latency and not an end-to-end app measurement.

## IoU and mask quality limits

- IoU against a hand-corrected mask: **UNMEASURED**. No hand-corrected masks exist in the prepared corpus (`mask: null` in its manifest). None were invented.
- IoU against BRIA reference for a local engine: **UNMEASURED**. There is no local candidate output.
- IoU of BRIA against itself would be tautological and is not reported as a quality score.
- The six review images are manually selected, full-resolution crops of visually challenging areas—not a ground-truth ranking of the six objectively worst masks. They support qualitative inspection only.
- Visual challenge coverage is not complete: the corpus has one tagged transparent drinking glass and one hand/person-like occluder tag, but no explicit transparent-bag case. Challenge tags were assigned from small thumbnails. The set is useful as a smoke corpus, not a representative seller-photo distribution or annotation benchmark.

## Artifacts

- `results.json` — per-call response status, latency, output dimensions and hashes.
- `results-table.csv` — per-photo reference summary, tags and invariant tallies.
- `summary.json` — counts, cost, latency semantics, integrity and invariant results; explicit `NOT_MEASURED` fields.
- `bria-reference-contact.png` — contact sheet of BRIA cut-outs over a checkerboard.
- `review-B07T4RGJZT.png`, `review-B07B4WR342.png`, `review-B07XXS6JMD.png`, `review-B073P1S8VZ.png`, `review-B07N3D8YR2.png`, `review-B07HZJ334C.png` — six selected full-resolution source/cut-out side-by-side review crops.
- `analyze-s1.py` — reproducible local analysis script.
- `run-bria-benchmark.mjs` — existing restricted reference-runner script.

All new artifacts are under `.kiln/scratch/plinth-m0-s1/`; nothing was installed on the phone, and no product code or other spike directory was changed.

## Claim ledger

| Label | Claim | Evidence |
|---|---|---|
| **MEASURED** | Runner completed the eligible corpus with 29 successful results and no omissions. | `python .kiln/scratch/plinth-m0-s1/analyze-s1.py`; `.kiln/scratch/plinth-m0-s1/summary.json` (`complete_eligible_reference_calls`, `failed_or_missing_eligible_results`) |
| **MEASURED** | Reference latency median/p95/range and cost totals are as tabulated. | Same analysis command; `results.json` records individual calls; runner timing boundaries: `run-bria-benchmark.mjs:16-27` |
| **MEASURED** | Opaque-interior RGB and white/black composite invariant have zero differences in the measured scope. | Same analysis command; `analyze-s1.py:125-145,219-230`; `summary.json: interior_rgb_invariant` |
| **MEASURED** | Every result's source/output hash and dimensions were checked; mismatches are zero. | Same analysis command; `summary.json` (`source_sha256_mismatches`, `output_sha256_mismatches`, `source_output_dimension_mismatches`) |
| **ON RECORD** | Corpus assets are identified as Amazon Berkeley Objects CC BY 4.0; tag basis is 390 px thumbnail review and mask fields are null. | `.kiln/scratch/plinth-bench-prep/index.json:17,27-28` (same fields repeat per row); `.kiln/scratch/plinth-bench-prep/LICENSE-CC-BY-4.0.txt` |
| **ON RECORD** | Stable local-engine gates are median IoU ≥0.95, tagged hard-subset median ≥0.90, and p95 ≤3 s on `SM-A405FM`; default without a qualifying local route is server-only at 1 credit. | `openspec/changes/app-seller-photo-studio/annex/M-spikes-and-reads.md:18,26-29` |
| **ON RECORD** | The named ML Kit subject-segmentation artifact is `16.0.0-beta1`, not stable. | `openspec/changes/app-seller-photo-studio/annex/E-pipeline-and-operations.md:54-58`; official Google ML Kit release notes, Android latest versions / July 21, 2026: https://developers.google.com/ml-kit/release-notes |
| **INFERRED** | No evidenced eligible local candidate means the local-engine shipping thresholds are not met for this S1 decision, so the recorded default is appropriate. | Stable-only requirement and default: `annex/M-spikes-and-reads.md:26-29`; named candidate eligibility: `annex/E-pipeline-and-operations.md:54-58` |
| **INFERRED** | Six selected challenge crops and the catalog corpus can flag visual concerns but cannot establish accuracy on real seller photos or determine “worst” masks objectively. | Manifest tag basis/missing masks: `.kiln/scratch/plinth-bench-prep/index.json:27-28`; crop-selection description: `.kiln/scratch/plinth-m0-s1/summary.json` |
| **UNVERIFIED** | Local candidate IoU, hand-mask IoU, and p95 latency on the requested phone. | No local model outputs, hand masks, or local timing records; `summary.json` explicitly marks these `NOT_MEASURED`. |
| **UNVERIFIED** | Whether another stable custom/bundled segmentation model exists and would meet the S1 thresholds. | No such candidate was identified or benchmarked in this bounded S1; no universal model search or Android build/install was performed. |

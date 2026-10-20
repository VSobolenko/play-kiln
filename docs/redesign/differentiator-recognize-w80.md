# W80 - recognition proof investigation

**Result: BLOCKED, not a proof-only relocation.** The core promise and its proof
were left unchanged. Pointing the proof at an unused API method or at generated
scene submission would make the textual check pass without proving item
recognition or category-driven card composition.

Paths below are relative to `apps/2026-10-15-plinth-product-photos/`.

## Observed path

- `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardScreen.kt:139-163`:
  `AutoCardRoute` delegates to `GeneratedSceneRoute`.
- `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/GeneratedSceneRoute.kt:108-113`:
  Generate calls `model.start(OperationId.GENERATED_SCENE, guidance, credits)`.
- `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioViewModel.kt:165-199`:
  `start` stores a pending operation with optional prompt, then calls `execute`.
  At lines 227-275, `execute` uploads the photo and submits a job; it does not call
  the recognition endpoint or feed recognition into `AutoCardEngine`.
- `app/src/main/java/com/gexetr/plinthproductphotos/data/api/ApiClient.kt:72-92`:
  `recognizePhoto` still implements the recognition request. Searching production
  Kotlin for `recognizePhoto|RecognitionRequest|AutoCardEngine` found the API
  declarations and engine declaration, but no invocation.
- `server/src/routes.ts:318-384` retains the separate recognition endpoint;
  lines 398-399 reject recognition through the jobs endpoint.

The existing assertion at `differentiators.json:86` describes the removed
recognition-to-local-compositor flow, not the observed generated-scene path.
Restoring that flow requires runtime implementation and validation, not a symbol
alias. This task does not invent that feature or weaken the core promise.

## Verification and claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Recognition has no production Kotlin caller in the inspected tree | MEASURED | `rg -n 'recognizePhoto\|RecognitionRequest\|AutoCardEngine' apps/2026-10-15-plinth-product-photos/app/src/main -g '*.kt'`; only API/model/engine declarations matched |
| The current AutoCard path submits generated-scene jobs instead | MEASURED | Source locations in Observed path above |
| The requested differentiator check remains blocked | MEASURED | `node scripts/check-differentiators.mjs --slot plinth-product-photos`, exit 1, `VERDICT: BLOCKED`; only blocking finding is `auto-card-by-item` missing `recognizePhoto` in `AutoCardScreen.kt` |
| End-to-end recognition and category-specific composition after W74 | UNVERIFIED | Requires a restored production caller and a device/API exercise showing recognition output controls category-specific composition; implementation worker and independent verifier must establish this |

The check was run before any edits and repeated at
`2026-10-17T19:17:55.6951705+03:00`, with the same blocking finding.
No Gradle command was run, and no device-verification record was edited.
Investigation through the repeated check took approximately 44 seconds from the
supplied task timestamp; documentation followed.

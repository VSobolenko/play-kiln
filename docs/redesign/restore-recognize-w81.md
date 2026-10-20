# W81 - restore product recognition on Create

## Result and scope

Restored a production recognition caller, not a proof-only symbol relocation.
The navigation shell already opens `GeneratedSceneRoute` directly, so the fix
works with its original `photoId`/back/change-photo arguments as well as through
`AutoCardRoute`. No navigation shell, Profile, Studio implementation, server
prompt, signing, release approval or device-verification record was edited.

Paths below are relative to `apps/2026-10-15-plinth-product-photos/`.

## Live path

1. `ui/autocard/GeneratedSceneRoute.kt:80-126` (under
   `app/src/main/java/com/gexetr/plinthproductphotos/`) creates a photo-keyed
   `AutoCardRecognitionViewModel`. With no explicit seller category, it starts
   recognition when there is no busy/pending scene operation or existing result.
   A ViewModel guard prevents recomposition/configuration-change retries.
2. `ui/autocard/AutoCardRecognitionViewModel.kt:57-80` loads the selected
   project's source, reuses `toRecognitionJpeg()` (exact 1024px longest side,
   JPEG, at most 4 MiB), reads the existing wallet session, and calls
   `recognizePhotoForAutoCard` on the IO dispatcher.
3. `ui/autocard/AutoCardRecognitionViewModel.kt:25-44` calls the real
   `ApiClient.recognizePhoto(RecognitionRequest(jpeg), deviceToken)` and maps
   category, attributes, visible text, scale hint and SKU into the domain flow.
   Failed requests leave the category unset. The server's zero-confidence
   `other` sentinel also becomes an explicit failure, not an invented category.
4. `domain/autocard/AutoCardEngine.kt:292-326` selects only the recognized or
   seller-selected category's **generated** recipes and constructs the scene
   prompt from its background, shadow, fill/margin, attributes and scale hint.
   Seller category changes stop using the old recognition's scale hint.
   Supplied marketplace rules constrain this prompt; original-only disables
   generation and offers the source unchanged. The current shell supplies no
   marketplace, so its path makes no marketplace-specific selection claim.
5. `ui/autocard/GeneratedSceneRoute.kt:103-105,151-156` passes that complete
   prompt through the existing quote/credit gate to
   `StudioViewModel.start(GENERATED_SCENE, prompt, credits)`. The existing
   upload/job/download/receipt/apply/export path is retained. Recognition occurs
   on opening Create; the seller can review/correct it before pressing Generate.
   The full prompt, including recognition/recipe context, must fit 2,000 chars.

Offline initialization, missing session, request failure, invalid response and
zero confidence leave seller category selection available. An explicit category
supplied by the caller skips recognition. A manual category chooses generated
recipes but does not make offline generation possible: the existing scene service
availability gate still applies.

The UI exposes localized category choices, editable attributes and numbered
generated-scene options. It does not render a local gradient as an AI result.
`auto-card-by-item` changes only its proof reference/assertion to the restored
caller and this generated-scene path; its promise, weight and status are unchanged.
Retargeting proof to the previously dead API declaration was rejected because
W80 established that it had no production caller.

## Verification

- **RED:** targeted `testDebugUnitTest --tests *AutoCardFlowTest --rerun`
  through `runGradleTasks` exited 1: five tests, exactly
  `unusableRecognitionDoesNotInventOtherCategory` failed because the prior
  flow accepted the zero-confidence fallback as a category.
- **GREEN:** `node scripts/run-slot-tests.mjs --slot plinth-product-photos --rerun`
  exited 0: **306 tests, zero failures/errors/skips**, final run 24,480 ms.
  Fresh XML reports contain five `AutoCardFlowTest` tests, four
  `AutoCardGeneratedRecognitionTest` tests and four
  `AutoCardCategoryHandoffTest` tests, all passing.
- `AutoCardGeneratedRecognitionTest` uses the real `ApiClient` with an in-memory
  transport. It checks raw JPEG/auth request shape, maps recognition into the
  actual engine prompt and serializes that prompt into a generated job request.
  It covers network/provider/malformed/zero-confidence failures, manual recovery,
  category/scene/attribute overrides, stale scene rejection, and white/original-only
  marketplace policies. It does **not** execute Compose or the Studio lifecycle.
- `node scripts/check-differentiators.mjs --slot plinth-product-photos`
  exited 0, **VERDICT: PASS**. Only existing informational CRUD/narrowing notes
  remain.
- `node scripts/verify.mjs --slot plinth-product-photos --build`:
  `build` **PASS**, `assembleDebug` 5,267 ms under JDK 17;
  `slot.differentiators` and `locale.hardcoded` **PASS**.
  Overall exit **1**, solely `slot.device` **FAIL**: its saved digest
  `d723c0a64840...` differs from current `8225d2f9234a...`.
  `pack.inventory` and `pack.media` are **UNVERIFIED** because there are no
  store assets. The device record was not refreshed or altered.
- Independent read-only code review reported no significant issues, while
  explicitly noting that the test does not cover Compose/ViewModel/Studio
  lifecycle execution.

The final verification log is disposable local evidence at
`.kiln/scratch/copilot-plinth-w81-restore-recognize/w81-final-verify.log`.
No external recognition/generation request or on-device journey was performed.
This is a restored, compiled and unit-tested production call path, not measured
provider output or release readiness.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The default live Create route reaches the recognition API caller | MEASURED | `GeneratedSceneRoute.kt:80-126`; `AutoCardRecognitionViewModel.kt:25-44,57-80`; source reads in this session |
| Recognition category, attributes and scale affect category-specific generated-job guidance | MEASURED | `AutoCardEngine.kt:292-326`; `GeneratedSceneRoute.kt:103-105,151-156`; `AutoCardGeneratedRecognitionTest.recognizedItemControlsTheRealGeneratedJobPrompt` in the executed suite |
| Failures and zero confidence do not invent a category; seller selection remains available | MEASURED | `AutoCardFlowState.kt:34-42`; `AutoCardGeneratedRecognitionTest.failedMalformedAndZeroConfidenceResponsesRequireSellerCategory`; the RED/GREEN commands above |
| Differentiator checker accepts the live caller's proof | MEASURED | `node scripts/check-differentiators.mjs --slot plinth-product-photos`, exit 0, `VERDICT: PASS` |
| The full unit suite and debug compilation succeeded | MEASURED | `run-slot-tests.mjs --rerun`: 306/306; final `verify.mjs --build`: `PASS build assembleDebug in 5267 ms` |
| Full slot verification is not green | MEASURED | Final `verify.mjs --build`, exit 1, stale `slot.device`; missing store media explicitly UNVERIFIED |
| Live provider result honors recognition/recipe guidance on the device | UNVERIFIED | Requires an authorized device/API run of Create, recognition, seller correction, generation and delivery; an independent device verifier must observe it |

Elapsed wall-clock through final validation: approximately **9 minutes 26 seconds**
from the supplied 19:19:04 task timestamp to the 19:28:30 evidence read
(2026-10-17, UTC+03:00); documentation followed. This is transparency, not a
selection or kill criterion.

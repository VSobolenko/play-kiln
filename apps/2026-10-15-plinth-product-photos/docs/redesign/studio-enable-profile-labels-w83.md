# W83 - Studio enablement and human result labels

Date: 2026-10-17. Development only; no release approval, server changes, or device record changes.

## Root-cause trace

The W79b device report (`device-pass-w79b.md`, journey table) records a retained result,
Relight disabled at balance 2 and again at 10, and UUID-shaped JPG titles in Profile/Library.
It does not contain a dump of Studio's saved request. The precise contents of that device's
`studio_pending` preference remain UNVERIFIED in W83.

The deterministic client defect was reproduced in JVM tests:

1. `StudioViewModel.refresh()` loads any saved request as `hasPending = true`
   (`app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioViewModel.kt:134-147`).
   `settle()` records the successful charge and sets `deliveryConfirmed = true` / phase DONE,
   but deliberately retains the request so Apply/Save/Close can still use its result
   (`StudioViewModel.kt:354-371`).
2. Before W83, the live `StudioRoute` carousel and photo choices required
   `!busy && !hasPending`; the Generate preview also rejected any `hasPending`.
   Thus a settled, even already-applied result disabled every next tool, regardless of balance.
   `choosePhoto`, `importPhoto`, and `start` independently rejected a nonempty saved-request store.
   Merely enabling the button would therefore not fix generation.
3. `StudioToolsScreen.kt:3` identifies itself as the superseded catalogue.
   Its domain availability rules are not the routed carousel's gate and were not loosened.
4. Source filenames enter `ProjectPhoto.name` unchanged via
   `data/import/PhotoImporter.kt:20-39`; Profile loads `summary.project.name`
   (`ui/profile/ProfileViewModel.kt:205`). These names are not necessarily server job IDs.
   Profile's old mapping and Library's old tile rendered them verbatim.

Paths above are relative to this slot's `app/src/main/java/com/gexetr/plinthproductphotos/`
unless the full path is given.

## Change and boundaries

`StudioState.blocksNewOperation` now means an unresolved delivery, rather than merely the
presence of a stored result; `canChooseTool` also retains the busy guard
(`StudioViewModel.kt:79-80`). Photo choices, tool choices, saved variants and Generate consume
those predicates (`StudioRoute.kt:279,299,375,403,429`).

The ViewModel separately checks the persisted record. Replacement is allowed only after
a matching charge, a receipt and a local result path, or a terminal failed/cancelled record
(`StudioPendingStore.kt:29-30`). An unpaid receipt, mismatched charge or in-flight request
still raises `JOB_UNRESOLVED`. Selecting/importing a photo, opening a saved variant, or
starting the next operation retires the completed request and its temporary received file.
Saved applied PNG variants are separate files and are not removed. Starting another job
rechecks the current config, price and balance **before** retiring the previous result
(`StudioViewModel.kt:173-223`).

An unkept preview remains available until the user changes photo, opens another saved result
or starts the next job. Those explicit actions replace it. Apply/Save/Close still work before
replacement. W78's Generate button still displays the quote; selecting a tool does not spend
credits, and insufficient balance still opens the shortfall/top-up dialog on Generate.
The rejected alternative was to clear all pending requests or ignore `hasPending` everywhere:
either could discard a still-unsettled delivery.

`ui/components/ProjectDisplayTitle.kt:7-22` preserves human project titles and replaces UUID
filenames, compact UUIDs, prefixed job/request/output IDs and blank titles with a locale-formatted
date and time. Profile history, Library text/preview/selection accessibility labels, and Studio
photo labels use it. This is display-only: existing records, filenames, IDs, search and navigation
keys are unchanged. Date/time is taken from the actual result/project/photo timestamp, not a
fabricated product or tool name. No migration or new translation is required for the platform's
localized date formatting.

## Reproduction and verification

All Gradle commands used `scripts/lib/gradle.mjs`'s `runGradleTasks` under the slot lease.
Logs are under `.kiln/scratch/copilot-plinth-w83-relight-enable-labels/`.

| Command / observation | Result |
|---|---|
| `:app:testDebugUnitTest --tests *ProfileHistoryPresentationTest --rerun-tasks`, before fix | Exit 1; 8 tests, 1 failure: `importedUuidFilenameIsNotPresentedAsAResultTitle`. `red-profile.txt`, 93,125 ms. |
| `:app:testDebugUnitTest --tests *StudioCompletedResultTest`, with the original gate extracted unchanged | Exit 1; 2 tests, 1 failure: `deliveredResultDoesNotLockRelightOrGenerateAtExactCostOrTenCredits` at the carousel-enable assertion. `red-studio.txt`, 17,776 ms. |
| First combined build | UI-test compilation caught an invalid `onAllNodes` import; removed it. No device test was claimed from that build. |
| `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, after fix | Exit 0, BUILD SUCCESSFUL; 18,690 ms. `green-build.txt`. |
| Fresh JVM XML aggregation | 61 suites, 312 tests, 0 failures, 0 errors. The test task executed, not UP-TO-DATE. |
| W83 regression coverage | Exact-cost 5 and balance 10 after delivery; unresolved/busy guards; persisted paid/unpaid/mismatched/missing-receipt states; UUID/compact-ID/prefixed-ID titles in English/Russian formatting; legacy Profile UUID title. Existing quote/server/input/low-credit tests also ran. |
| New UI tests | Compile succeeded. Cover choosing Relight then Generate at 10 credits, visible cost, Profile title/image semantics, Library title/selection semantics. Execution UNVERIFIED; no device was addressed in W83. |
| Read-only independent code review | No significant W83 issues reported. Static review only, not a runtime verdict. |
| `git diff --check` on changed UI/test areas | Exit 0; line-ending conversion warnings only. |
| `node scripts/verify.mjs --slot plinth-product-photos --build` | Exit 1; 123 PASS, 2 UNVERIFIED, 4 SKIP, 1 FAIL. `build` PASS (`assembleDebug`, 3,032 ms); `slot.device` FAIL because W79b's digest no longer matches these changed inputs. `pack.inventory` / `pack.media` UNVERIFIED because no store assets exist. Full run 16,288 ms; `verify.txt`. |

Debug artifact: `app/build/outputs/apk/debug/app-debug.apk`.
Measured SHA-256:
`5e33681383965a97a26c64cf17d9349016e875194a9d6c3a9ac1a1f00752566b`.
No installation, provider generation, photo-fidelity result or device PASS is claimed.
In particular `device-verification.json` was not updated to bless these changed sources.
The overall slot verification is therefore **FAIL**, not a release/device PASS. Its stale-device
failure is not bypassed. W83's wall-clock work was approximately 12 minutes
(2026-10-17, starting 19:53 +03:00); Gradle durations above are measured separately.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| A retained completed request blocked the live carousel and Generate independent of balance | MEASURED | Pre-fix gate read; `red-studio.txt`, failing `StudioCompletedResultTest` |
| This explains W79b's exact device state | INFERRED | W79b retained result plus the reproduced gate; preference contents were not read in W83 |
| Confirmed results no longer block the next tool at balances 5/10; unpaid delivery remains blocked | MEASURED | `StudioCompletedResultTest`, 3 tests, 0 failures; `StudioViewModel.kt:79-80,173-223` |
| Legacy UUID titles are replaced without altering persisted IDs | MEASURED | `ProjectDisplayTitleTest`, 2 tests; `ProfileHistoryPresentationTest`, 8 tests; `LibraryScreen.kt:325` |
| JVM suite and debug build complete successfully; UI tests compile | MEASURED | `green-build.txt`, exit 0; 312-test XML aggregation |
| Overall slot verification is not green | MEASURED | `verify.txt`: exit 1, stale `slot.device`; pack inventory/media remain UNVERIFIED |
| Fixed behavior appears on the owner's phone and a new Relight preserves the photo | UNVERIFIED | Requires installing the new debug APK and exercising the W79b branch with a real selected photo and server quote; no phone operation in W83 |
| Server prompts and device-verification record were changed by W83 | MEASURED - no | W83 edit scope contains only client UI/state/tests and this report; no server or device-record write command |

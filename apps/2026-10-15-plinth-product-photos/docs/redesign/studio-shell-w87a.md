# W87a — Studio shell rewrite

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Studio now has a compact photo strip, a two-column tool grid, and a separate tool mode with in-route Back handling. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt:162-220`, `:379-461` |
| Tool mode keeps the photo bound, shows a Change action, and presents one cost-labelled Generate button; generated-scene direction is freeform and required. | MEASURED | `StudioRoute.kt:271-305`, `:519-658`; `StudioViewModel.kt:122-123`, `:183` |
| Catalogue cards are equal-width clickable grid items, not full-width chooser buttons. The old button helper remains only as a narrower compatibility function for existing W78 tests and is not used by the live catalogue. | MEASURED | `StudioRoute.kt:417-461`, `:702-715`; `rg "StudioOperationActionButton\\(" StudioRoute.kt` returned only the helper definition |
| The Live Studio catalogue includes all operations the ViewModel currently accepts: Generated scene, Relight, and Recolour. | MEASURED | `StudioRoute.kt:726-749`; `StudioViewModel.kt:180`, `:572-574`; regression test `app/src/test/java/com/gexetr/plinthproductphotos/ui/studio/StudioPromptValidationTest.kt:28-30` |
| Cutout, canvas expansion/Outpaint, and Upscale are not wired into this shell: the current ViewModel start allowlist has only the three operations above, and the parent route was out of scope. | MEASURED | `StudioViewModel.kt:572-574`; `app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt:756-757` |
| A Library text action is supported through an optional `onOpenLibrary` callback, but the current parent call supplies only `onOpenWallet`; saved-result navigation therefore remains a parent-wiring handoff. | MEASURED | `StudioRoute.kt:83-86`, `:231-237`; `PlayKilnApp.kt:756-757` |
| English and Russian Studio resources parse with 128 unique Studio string/plural names each. | MEASURED | `python` XML parse of `app/src/main/res/values/strings_studio*.xml` and `values-ru/strings_studio*.xml` returned `values: parsed 128 unique...` and `values-ru: parsed 128 unique...` |
| Prompt-validation unit tests and grid interaction/equal-width plus suggestion-fill UI coverage were added. | MEASURED | `app/src/test/java/com/gexetr/plinthproductphotos/ui/studio/StudioPromptValidationTest.kt:8-30`; `app/src/androidTest/java/com/gexetr/plinthproductphotos/w87/StudioShellTest.kt:34-110`; updated W83 interaction in `app/src/androidTest/java/com/gexetr/plinthproductphotos/w83/StudioEnablementAndLabelsTest.kt:44-110` |
| The Compose/unit tests compile and pass, and the revised screens render correctly on-device. | UNVERIFIED | Not run: the task reserved Gradle/build work for the orchestrator, so no APK was built here. After the slot lock is released, run `.\gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` from the slot directory, then perform the orchestrator's device review. No device PASS is claimed here. |
| The referenced owner contract's header still reports “design pending”; this implementation followed the task brief's explicit owner-approved authorization. | MEASURED | `docs/redesign/studio-rewrite-owner-w87.md:3`; W87a task brief supplied with this work |

## Scope handoff

Cutout, canvas expansion/Outpaint, and Upscale require a navigation callback from the application shell; adding or wiring that callback would require editing `PlayKilnApp.kt`, which was explicitly excluded. The Library callback has the same wiring limitation. The Studio route no longer displays saved-result photo cards; once the parent passes `onOpenLibrary`, it will show a text link for saved results.

Elapsed wall-clock time: **about 11m 52s MEASURED**, from the supplied start `2026-10-17T21:35:10.185+03:00` to the last recorded clock `2026-10-17T21:47:01.934+03:00`.

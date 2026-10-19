# W94e — Three tails

## Stale photo

Before, the Studio toolbox callback pushed `StudioToolsDestination(it.name)` from
`PlayKilnApp.kt:412`, without carrying a photo id. `StudioRoute` then relied on
`StudioViewModel.refresh()` to choose a photo.

After, `StudioToolsDestination` has a nullable `photoId` parameter
(`apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt:222-225`).
The shell passes `homeState.recentPhotos.firstOrNull()?.id` at the toolbox
navigation call
(`apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt:421-426`).
This is the same first recent photo that Home treats as its latest photo
(`apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeScreen.kt:119`),
from the `HomeViewModel` snapshot populated by `ProjectRepository.recentPhotos()`
(`apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeViewModel.kt:60`).

The Studio destination obtains its entry-scoped `StudioViewModel`, selects the
destination id, and waits for that id in the model state before composing
`StudioRoute`
(`apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt:782-817`).
`StudioRoute` resolves the shown photo by matching `state.photoId` to a photo
record
(`apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt:135-137`).

Fallback rule: `StudioViewModel.refresh()` keeps the unresolved pending job's
photo first, then the current `photoId`, and only uses `photos.firstOrNull()`
when neither is present
(`apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioViewModel.kt:164`).
The explicit selection is therefore not replaced by the recent-photo fallback.
A pending job continues to pin its own source photo.

## Duplicate credit line

Removed the `state.charged` list item and its `studio_live_charge` text lookup
from
`apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt:388-390`. Removed
`<string name="studio_live_charge">Used %1$d credits</string>` from
`apps/2026-10-15-plinth-product-photos/app/src/main/res/values/strings_studio_live.xml:25` and
`<string name="studio_live_charge">Использовано кредитов: %1$d</string>` from
`apps/2026-10-15-plinth-product-photos/app/src/main/res/values-ru/strings_studio_live.xml:25`.

Before, this was the final item in the Studio result list. After, neither the
list item nor either resource exists; the always-visible top credit display is
unchanged.

## Android test labels

| File | Changed assertion | Why |
|---|---|---|
| `apps/2026-10-15-plinth-product-photos/app/src/androidTest/java/com/gexetr/plinthproductphotos/w78/StudioWorkflowTest.kt:56-58` | Sufficient-balance action now uses `studio_live_action_relight` and still clicks the control. | The primary action is labelled “Relight · N credits”, not “Generate · N credits”. |
| `apps/2026-10-15-plinth-product-photos/app/src/androidTest/java/com/gexetr/plinthproductphotos/w78/StudioWorkflowTest.kt:93-99` | Insufficient-balance action lookup now uses `studio_live_action_relight`. | Keep the displayed/clickable action assertion on the current copy. |
| `apps/2026-10-15-plinth-product-photos/app/src/androidTest/java/com/gexetr/plinthproductphotos/w83/StudioEnablementAndLabelsTest.kt:109-110` | Enabled relight action lookup now uses `studio_live_action_relight`. | Preserve the assertion that the action is enabled and invokes generation with the renamed label. |
| `apps/2026-10-15-plinth-product-photos/app/src/androidTest/java/com/gexetr/plinthproductphotos/w87/StudioShellTest.kt:69-72` | Catalogue absence check now uses the formatted `studio_live_action_relight` label. | The old generic generate label is no longer the Relight action copy. |
| `apps/2026-10-15-plinth-product-photos/app/src/androidTest/java/com/gexetr/plinthproductphotos/w87/StudioShellTest.kt:102-107` | Scene action lookup now uses `studio_live_action_scene` for both disabled and enabled checks. | Preserve the button-state and click assertions with “Create background · N credits”. |

These three test files contained no assertions for the previous-job “Finish it”
or “Discard and continue” buttons, so no such assertions were removed or
rewritten.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The old shell navigation omitted the photo id and the new destination carries it. | MEASURED | Before: `apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt:412`; after: `apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt:222-225,421-426` |
| The Studio route displays the photo matching its selected id, and refresh falls back only after pending/current ids. | MEASURED | `apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt:135-137`; `apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioViewModel.kt:164` |
| The charged result item and both localized string resources were removed. | MEASURED | Before: `apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt:388-390`; `apps/2026-10-15-plinth-product-photos/app/src/main/res/values/strings_studio_live.xml:25`; `apps/2026-10-15-plinth-product-photos/app/src/main/res/values-ru/strings_studio_live.xml:25`; after: no `studio_live_charge` reference under `apps/2026-10-15-plinth-product-photos/app/src/` |
| The three Android test files target current action-label resources. | MEASURED | `apps/2026-10-15-plinth-product-photos/app/src/androidTest/java/com/gexetr/plinthproductphotos/w78/StudioWorkflowTest.kt:56-58,93-99`; `apps/2026-10-15-plinth-product-photos/app/src/androidTest/java/com/gexetr/plinthproductphotos/w83/StudioEnablementAndLabelsTest.kt:109-110`; `apps/2026-10-15-plinth-product-photos/app/src/androidTest/java/com/gexetr/plinthproductphotos/w87/StudioShellTest.kt:69-72,102-107` |
| Kotlin/Android tests compile and pass. | UNVERIFIED | Gradle was not run by request; the integrator can decide with `node scripts/run-slot-tests.mjs --rerun` and `node scripts/verify.mjs --slot plinth-product-photos --build` from the repository root. |

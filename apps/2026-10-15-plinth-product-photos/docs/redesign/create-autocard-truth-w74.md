# W74 - Create and AutoCard: generation, not padding

Date: 2026-10-17. Authorised implementation of W72 and owner items O03, O04,
O05, O06 and O12. No release action, paid inference, device capture, server edit,
navigation-shell edit or StudioRoute edit was performed.

Paths below are relative to this slot. `K` means
`app\src\main\java\com\gexetr\plinthproductphotos`.

## Decision and routed behaviour

The existing AutoCard destination now hosts the existing Studio **GENERATED_SCENE
/ O-07** operation. It no longer runs `AutoCardEngine.compose`. The alternative
of merely renaming the gradient while keeping it behind the primary AI action
was rejected: W72 section 4 requires generated surroundings, and W70 O03/O12
explicitly rejects padding as the centre Create outcome.

| Owner item | Implementation on disk |
|---|---|
| O03, O12: fake scene / centre Create | `K\ui\autocard\AutoCardScreen.kt:139-163` retains the host's entry signature but opens `GeneratedSceneRoute`. The screen calls `StudioViewModel.start(OperationId.GENERATED_SCENE, guidance, credits)` rather than the compositor (`GeneratedSceneRoute.kt:103-110`). Home's scene tile, hero continuation and recent-photo action reach this same destination. |
| O04: duplicate original | The former original-above plus Original/Card-preview combination is removed with the local AutoCard UI. GeneratedSceneRoute renders one image branch: result or source. Before/After changes that branch instead of adding a second original (`GeneratedSceneRoute.kt:147-179`). |
| O05: verbose Create | Import now says Create, Start with a product photo, Choose photo. After import, the choices are AI background and Card layout (`K\ui\import\ImportScreen.kt:88-116,181-199`). The old implementation/private-copy essays and swipe instruction are removed from this surface. |
| O06: category friction | The generated-scene screen has no category picker, no category confirmation and no separate paid recognition step. The product image and optional background idea go to the existing image-conditioned operation. This removes the need for a category tap rather than automatically spending recognition credits. |
| O03: misleading scene previews | HomeSceneStrip no longer paints category gradients as scene thumbnails. Its single labelled AI-background entry opens the generation flow (`K\ui\home\HomeSceneStrip.kt:20-53`). |

**Local layout remains a different job.** Import's Card layout action retains
`onOpenLocalCard(selectedPhoto.id)`, which the unchanged navigation host maps to
the existing Card destination (`K\ui\PlayKilnApp.kt:434-439`). It is not labelled
AI. No local composition is sent to the old AI-labelled AutoCard result host:
the replacement AutoCardRoute does not call `onCardReady`.

The old host arguments remain for compatibility with the separately owned shell.
Its loading of the preset and recipe text is still outside this worker's scope;
none of those inputs drive the generated-scene screen. The existing shared source
photo renderer is passed through to retain the loading-to-photo handoff.

## Reused operation and guards

No new AI operation, provider or job protocol was added. The new screen is a
consumer of the existing `StudioViewModel`, including selected-source upload,
durable pending state, quote revalidation, delivery receipt, provenance-bearing
project save and document export. W73's server routing to the image-conditioned
Kontext endpoint is **ON RECORD** in `studio-tool-binding-w73.md`; this worker
did not redeploy or call that provider.

The screen binds the destination's `photoId` only after refresh has finished and
only when no durable job is pending (`GeneratedSceneRoute.kt:76-83`).
`SceneGenerationGate.kt:14-35` disables a new request when the selected photo
differs, a job is pending, a result is still open, the current quote/balance is
missing, credits are insufficient, the operation is disabled, a server stop/cap
applies, work is busy, an error is unresolved or guidance exceeds 2,000 characters.
Missing category and empty guidance do not block generation.

A pending result for another photo or operation is not displayed as this scene.
The UI names the conflict and directs the user to finish it in Studio. It does
not replace that pending photo, launch a second job, or substitute a gradient.
The Generate button displays the current server quote. Insufficient balance and
other failures are explicit text, not a local-image fallback.

Keep result uses `applyResult`; Save image uses the existing
`CreateDocument("image/png")` and `saveDocument` path. Try another background uses
the existing resolved-result discard action. An already kept project PNG is
separate from the received temporary image (`StudioViewModel.kt:309-312,365-366,
405-418`); the existing discard action clears the active job, not the project row.
A kept result remains active until that action or Studio finishes it; no new
pending-store lifecycle was invented here.

All new visible copy is in EN/RU `strings_autocard.xml`; the related Home copy
is in EN/RU `strings_home.xml`. No shell, studio or plinth string file was edited.

## Evidence and limits

**MEASURED regression failure before the fix:** a lease-protected
`:app:testDebugUnitTest --tests
com.gexetr.plinthproductphotos.w74.LocalCardRecipeTest` invocation ran one test
and failed with:

```text
Generated recipes must use the server scene tool, not local composition: CLOTHING_TOP
1 test completed, 1 failed
BUILD FAILED in 53s
```

The failure demonstrates the previous local recipe-list behaviour. The domain
flow now excludes GENERATED_SCENE recipes from that local list
(`K\domain\autocard\AutoCardFlowState.kt:21-23`). It does not change the engine's
explicit supplied-scene support. Frozen failure XML is at repository-root
`.kiln\scratch\copilot-plinth-w74-create-autocard-truth\w74-red.xml`.

`app\src\test\java\com\gexetr\plinthproductphotos\w74\SceneGenerationGateTest.kt`
adds five cases covering no-category/empty-guidance generation, photo mismatch,
pending-photo/tool conflicts, disabled/unfunded/stopped operations and the guidance
length boundary. **Their final execution is UNVERIFIED**, not green.

The first combined suite/build attempt was skipped while PID 151420 held the
slot lease for `:app:processDebugResources --rerun-tasks`. A later check found the
lease free, so exactly one `assembleDebug` invocation was made, together with the
full unit suite, using the repository's `runGradleTasks` route:

```js
runGradleTasks(path.resolve('apps', '2026-10-15-plinth-product-photos'),
  [':app:testDebugUnitTest', 'assembleDebug'])
```

**Observed result: exit 1, 23.7 seconds.** Compilation stopped before tests:

```text
> Task :app:compileDebugKotlin FAILED
ui/studio/StudioRoute.kt:18:43 Cannot access
'val RowColumnParentData?.weight: Float': it is internal in file.
BUILD FAILED in 23s
```

That file is explicitly outside W74's writable scope and was not repaired here.
The exact output is at repository-root
`.kiln\scratch\copilot-plinth-w74-create-autocard-truth\w74-final-build.log`.
No second assembly was launched. A read-only attempt to locate an independent
cached Kotlin compiler was denied in this non-interactive session; it produced
no alternate compiler or test result.

**Other measured checks:** XML parsing and EN/RU key comparison reported
`strings_autocard.xml: 80 matching keys` and `strings_home.xml: 46 matching keys`.
The scoped `git diff --check` invocation returned exit 0.

**Still UNVERIFIED:** final Android compilation, the new gate tests, actual
on-phone navigation/layout, live scene fidelity, resumed-job UI and project/save
behaviour. W78 must resolve the StudioRoute compilation error; then the Android
verifier can run the unit suite. W79 must pick a phone photo through centre
Create and Home, generate an actual scene against the deployed W73 server,
compare before/after, keep/save it, and exercise changing photos with an existing
pending result. A working request path is not proof of model fidelity.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Owner requires generated scenes rather than gradient padding | ON RECORD | `product-rewrite-design-w72.md` sections 1-4; `owner-review-rewrite-w70.md` O03/O12 |
| Photo-first generation with optional scene direction follows documented patterns | ON RECORD | `competitor-generative-ux-w71.md`, Photoroom/Pebblely and repeated-pattern checklist; no claim of an installed competitor session |
| Create and Home AutoCard entries now call the existing O-07 operation | MEASURED | `K\ui\autocard\AutoCardScreen.kt:139-163`; `GeneratedSceneRoute.kt:103-110`; unchanged host callbacks `K\ui\PlayKilnApp.kt:434-439` |
| Routed scene creation no longer asks for a category or runs local composition | MEASURED | `K\ui\autocard\AutoCardScreen.kt:139-163`; `GeneratedSceneRoute.kt:47-260` |
| Result preview no longer repeats the original photo above a second original | MEASURED | `K\ui\autocard\GeneratedSceneRoute.kt:147-179`, mutually exclusive source/result branches |
| The local recipe regression could detect the old behaviour | MEASURED | Red run: one test, one assertion failure; frozen `w74-red.xml` above |
| The added gate cases pass | UNVERIFIED | Unit tasks did not run after the final compilation error; rerun `:app:testDebugUnitTest` after W78 repairs StudioRoute |
| The final debug APK builds | UNVERIFIED | Single attempt exited 1 at `K\ui\studio\StudioRoute.kt:18`; `w74-final-build.log` |
| W73 routes O-07 to image-conditioned Kontext | ON RECORD | `studio-tool-binding-w73.md`, Fix on disk; no provider call in W74 |
| The generated image visually preserves the selected product | UNVERIFIED | Requires W79 live-device/provider observation and source/result evidence |

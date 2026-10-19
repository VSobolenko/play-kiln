# W75 — viewer zoom and cutout editor chrome

**Scope:** resolve owner-review items O02 (off-center image detail) and O11 (icon undo/redo/zoom) from `owner-review-rewrite-w70.md`, without changing the cutout service or broadening the editor.

## Implementation

- Card inspection now shares one `Inside` scale between the image, Telephoto state, and the declared image bounds. Its zoom ceiling is 8× rather than Telephoto's 2× default. The inspection hint explains pinch-to-zoom and drag-to-pan.
- The W75 viewer test uses a four-quadrant image with markers in all four off-center corners, pinches each marker, pans it to the viewport center, and checks both its position and rendered size. The size assertion distinguishes a 4× detail view from the previous 2× default.
- The saved cutout screen now has accessible icon actions for undo, redo, and zoom. These reuse the slot's ISC-licensed, vendored Lucide resources (`arrow-left`, `chevron-right`, and `search`); no icon artwork or dependency was added. Brush modes are selected with Material filter chips.
- Cutout zoom opens a full-screen Telephoto viewer of the delivered result. The existing AI cutout, delivery/receipt, and brush entry points remain in place.
- `MaskEditHistory` stores bounded copies of alpha snapshots. Undo and redo restore exact snapshots; recording a new stroke after undo clears the redo branch. The result screen enables each action from the history state.
- Mask edits, undo, and redo are serialized while an edited mask is being written and rendered. History actions and brush input are disabled during that save, with a localized saving status, so concurrent edits cannot target the same mask revision or temporary file.

## Validation and remaining evidence

`node scripts/check-locales.mjs --slot plinth-product-photos` passed (1,075 translated keys, including the English/Russian W75 strings). `:app:compileDebugAndroidTestKotlin` compiled the current W75 instrumentation sources successfully. The viewer test computes touch points from the actual `ContentScale.Inside` image bounds and exercises all four corners; mask-history fixtures use byte-valued alpha data.

`node scripts/run-slot-tests.mjs --slot plinth-product-photos --rerun` passed all 300 JVM tests. `:app:connectedDebugAndroidTest` compiled and packaged the tests but could not execute them: `adb devices -l` reports the only attached device offline and Gradle reports `No online devices found`. The gesture and cutout UI tests therefore remain **UNVERIFIED at runtime**.

`node scripts/verify.mjs --slot plinth-product-photos --build` built `assembleDebug` successfully (122 PASS, 2 UNVERIFIED, 4 SKIP) but the slot verdict remains FAIL on two unrelated existing gates: `slot.differentiators` reports W74's `auto-card-by-item` proof symbol `recognizePhoto` missing from `AutoCardScreen.kt`, and `slot.device` reports that the recorded device digest is stale for the current build inputs. The connected-device run is needed to refresh the latter; W74's AutoCard files are outside this task.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| O02 reports center-only zoom; O11 asks for icon undo/redo/zoom and rejects a full Photoshop scope. | ON RECORD | `docs\\redesign\\owner-review-rewrite-w70.md`, O02/O11 and §§ B4–B5 |
| Card inspection uses an 8× Telephoto ceiling, `ContentScale.Inside` for both rendering and zoom state, and bitmap-sized centered content bounds. | MEASURED | `app\\src\\main\\java\\com\\gexetr\\plinthproductphotos\\ui\\card\\CardScreen.kt:294-318`; Telephoto v0.19.0 `ZoomableState.kt` defaults to `ZoomSpec(maxZoomFactor = 2f)` and `ZoomableContentLocation.kt` defines the `Inside` bounds |
| The undo/redo toolbar reuses licensed, vendored icon resources already present in the slot. | MEASURED | `icons\\README.md`; `icons\\fetch-record.md`; `app\\src\\main\\java\\com\\gexetr\\plinthproductphotos\\ui\\cutout\\CutoutScreen.kt:261-281` |
| Mask edit persistence rejects overlapping brush/undo/redo mutations and exposes a localized saving state that disables history actions. | MEASURED | `app\\src\\main\\java\\com\\gexetr\\plinthproductphotos\\ui\\cutout\\CutoutViewModel.kt:294-319,870-899`; `CutoutScreen.kt:261-281`; `CutoutEditorChromeTest.kt:51-88` |
| The all-corners off-center pinch/pan test, icon-control and saving-state UI tests, and exact mask-history tests compile. | MEASURED | `:app:compileDebugAndroidTestKotlin`; `app\\src\\androidTest\\java\\com\\gexetr\\plinthproductphotos\\w75\\CardViewerZoomTest.kt`, `CutoutEditorChromeTest.kt`, `CutoutMaskHistoryTest.kt` |
| The slot's 300 JVM tests pass and the English/Russian locale check passes. | MEASURED | `node scripts/run-slot-tests.mjs --slot plinth-product-photos --rerun`; `node scripts/check-locales.mjs --slot plinth-product-photos` |
| W75 Android instrumentation behavior has been observed on-device. | UNVERIFIED | `:app:connectedDebugAndroidTest` stopped before test execution because the only listed device was offline; repeat with an online device |

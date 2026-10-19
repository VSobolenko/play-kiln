# Import and Smart card UX repair (W3)

## Intent and decision

The selected product photo is the anchor, rather than another line of metadata.
Import shows the actual photo and filename, lets a batch user select which photo
to work on, and keeps its next action outside the scrolling content. Smart card
shows the original and the prepared card in a bounded, fit-to-frame viewer. Its
primary action stays in the Scaffold bottom bar, with a bounded, centred progress
indicator and a named operation while recognition or composition runs.

This chooses a photo-first screen plus a persistent bottom action over the old
scroll-to-the-end action. The category dropdown is replaced with a grouped sheet
of selected-state chips, not a longer list of default buttons. Scene options have
distinct localized names and swatches from their recipe colours; swatches are
not claimed to be generated scene photographs. Missing generated-scene assets
are explicitly unavailable rather than selectable dead ends.

Home leads with either importing a first photo or continuing the latest photo in
Smart card. Its additional tools and workflows remain available in a collapsed,
headed section. The duplicated first-photo guide and latest-photo rail entry
are removed.

## External sources read before implementation

All sources below were read on 2026-10-16. They document patterns, not observations
of this implementation.

| Source | Exact source wording | Application here |
|---|---|---|
| [Adobe Lightroom: Import photos from your device](https://helpx.adobe.com/lightroom/mobile/add-and-capture-photos/add-and-import-photos/import-photos-from-your-device.html) | "When you open Lightroom on mobile, you can view and edit photos stored on your device without importing them." | Keep the media visible during the workflow; do not substitute its filename for the media. This is a design inference from Adobe's documented media-first access, not a claim that Adobe uses our layout. |
| [Android: Scaffold](https://developer.android.com/develop/ui/compose/components/scaffold) | "`bottomBar`: The app bar across the bottom of the screen." Also: "It passes `PaddingValues` to the `content` lambda that you should apply to your content's root composable to constrain its size." | Put the primary action in `Scaffold.bottomBar`, not inside the scroll column; apply and consume the content padding. |
| [Android: Chips](https://developer.android.com/develop/ui/compose/components/chip) | "Filter: Allows users to refine content from a set of options. They can be selected or deselected, and may include a checkmark icon when selected." | Use selected-state chips, grouped by product family, inside a Material 3 sheet using the slot's theme. |
| [Android: Navigation 3 basics](https://developer.android.com/guide/navigation/navigation-3/basics) | "When they go back from that content, it is popped off the stack and the previous content is displayed." | Photo-left-swipe and the toolbar both invoke the supplied `onBack`. They do not create a second stack or replace platform back handling. |

## Files and behaviour

Paths below are relative to the slot.

| File | Change |
|---|---|
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/import/ImportScreen.kt` | Large fitted preview and filename; selectable batch-photo previews with accessibility names and selected semantics; actions target the selected photo ID; persistent bottom actions with bottom/horizontal safe-drawing insets. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/import/ImportViewModel.kt` | Retain selected URIs in import state so the first picked image can be displayed while its private copy is being saved. The existing IO dispatcher is retained. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/import/SelectedMediaPreview.kt` | Shared fitted Coil viewer, explicit loading/error states, wrapping filename/state, and photo-only left swipe (96 dp net drag). The swipe invokes the same callback as toolbar Back and leaves horizontal chooser rails alone. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardScreen.kt` | Original/card-preview switch, dimension/state label, pinned create action, centred bounded progress, manual category escape while recognizing, grouped sheet covering all 21 categories, recipe-colour scene swatches, distinct scene labels, and unavailable generated-scene states. Consumes Scaffold padding and handles its IME/bottom action insets. Pixel conversion/composition still use `AutoCardImageWork`. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeScreen.kt` | Photo-first guided hero and direct Smart card action; additional tools collapsed under a heading; only older photos in the recent rail; no duplicate guide; single-line toolbar title content. |
| `app/src/main/res/values/strings_import_smartcard.xml` and `app/src/main/res/values-ru/strings_import_smartcard.xml` | New English/Russian strings only; no existing translation entries changed. |

No shell, shared component, theme, other screen, build file, or Git index edits
are part of this worker's change. The existing generated-result destination
already renders an `Image` in `ui/PlayKilnApp.kt`; it was read, not edited.
No new route or shell signature is required. Only `ImportScreen`'s internal
action callbacks change from `() -> Unit` to `(String) -> Unit`, and its caller
was updated. Public route signatures remain unchanged.

## Evidence and limits

- A temporary Python source-contract check first failed all five original
  assertions against the old source. After implementation, eight structural
  checks cover selected-media rendering, action placement, grouped categories,
  category coverage, spinner bounds/inset consumption, the Home next action,
  correct Scaffold ownership of bottom bars, and EN/RU resource/format parity.
  These are source checks, **not** Compose rendering tests or compilation.
- The existing `uiuxFindings` sensor was run without Gradle. Its first reading
  identified a fixed text-bearing scene width and an unnamed clickable import
  thumbnail in the edited code. Both were corrected with flexible width bounds
  and an explicit localized accessibility name.
- The final source-contract run reported `Ran 8 tests ... OK` (exit 0);
  `git diff --check` reported no whitespace errors. The final `uiuxFindings`
  reading reported no findings in the edited screens. It still reports the
  shared `ui/components/PlinthScaffold.kt:28` IME advisory and app-wide notes
  about splash, motion, text selection, and autofill; these are outside W3.
- No Gradle command, device installation, screenshot, paid generation, network
  recognition round-trip, or instrumented gesture was run by this worker.
  Compilation, actual appearance, responsiveness timing, IME behaviour, and
  native/predictive-back behaviour remain **UNVERIFIED**.
- The orchestrator owns compilation and device reproduction. Exercise: one
  photo and a five-photo batch (choose a non-first photo); long filenames;
  Russian at large font scale; offline/recognition-failure/manual category;
  each local scene and unavailable generated scenes; source/card preview;
  create then view the result; toolbar Back, system edge-back and photo-left
  swipe; keyboard open/closed with both navigation modes.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| Adobe documents viewing device photos before import | ON RECORD | Adobe URL and exact quote above |
| Material 3 documents a Scaffold bottom bar and content padding | ON RECORD | Scaffold URL and exact quotes above |
| Filter chips represent selected options | ON RECORD | Chips URL and exact quote above |
| Back navigation pops the owned stack | ON RECORD | Navigation 3 URL and exact quote above |
| Source contains fitted photo viewers and a pinned Smart card action | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/import/SelectedMediaPreview.kt:28`, `app/src/main/java/com/gexetr/plinthproductphotos/ui/import/ImportScreen.kt:105`, `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardScreen.kt:303`; temporary source-contract command observed during W3 |
| Pixel work remains dispatched off the main thread | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardImageWork.kt:12`, `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardImages.kt:39`, `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardScreen.kt:148` |
| Runtime UI and build correctness | UNVERIFIED | Orchestrator's single Gradle build slot and the device scenarios above are required |

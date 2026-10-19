# W31 - Image-led Card result

Implements backlog item 12 in `modern-ai-android-2026.md` section 3.

## Presentation and interaction

- The source photo and rendered result share a square, fit-without-cropping stage,
  capped at 520 dp wide. Rendering progress stays inside the image bounds.
- A completed render selects **Rendered card**. Full-width selection chips switch
  between it and the original; **Inspect selected image** opens a dismissible,
  full-window fit preview with a named close action. Inspection is not pixel-level
  zoom.
- The bottom action stays visible: **Make card** before rendering, then
  **Save draft PNG**. Re-rendering and the existing editor remain secondary.
  Loading, rendering, saving, completion and failures have localized feedback;
  status changes use polite accessibility live regions.
- Composition runs on `Dispatchers.Default`, not the UI thread. Loading a different
  photo cancels the previous load/render and clears the previous image. Export
  completion cannot overwrite a different result's status.

## Marketplace and export boundary

**Marketplace - not selected** is displayed above the image. This route receives
only a photo ID: it has no marketplace selection or marketplace-export callback.
The existing composition is a 1200 x 1200 local draft, not a checked marketplace
result. No marketplace picker, passed-check badge or fabricated export navigation
was added.

The primary export action now opens Android's `CreateDocument("image/png")` picker
and writes the actual rendered bitmap, irrespective of the comparison selection.
Picker cancellation does not claim success. PNG compression and destination writing
run on `Dispatchers.IO`; success is set only after the stream closes. Write/access
failures have a visible retry message. Direct bitmap-to-stream encoding avoids an
additional PNG byte-array copy; the existing `ExportDelivery.writeBytesTo` helper
accepts only already-encoded bytes.

The screen distinguishes this draft save from marketplace packs: return to Home
and open Export for packs built from library photos and a saved Look. This screen's
preview is not passed to that pack builder. The existing editor callback still
opens its own session for the original photo; it does not inherit this preview.

Chosen: a persistent, truthful draft-PNG action. Rejected: labelling the existing
editor callback as export, or implying that an arbitrary marketplace was selected.
Both would misrepresent the route's actual wiring.

## Evidence and remaining verification

Observed source-only checks in this worker session:

- Python `xml.etree.ElementTree` parsed both resource directories; all 25
  `CardScreen` string references resolved in both EN and RU, with no duplicate
  string keys. The two new `strings_card.xml` files have the same 15 non-empty
  keys and each RU value differs from EN.
- Scoped `git diff --check` returned exit 0 with no whitespace findings.
- No dependencies, Gradle files, navigation files or shared components were edited.
  No Gradle command was run; no commit was created.

**Compilation and runtime: UNVERIFIED.** These checks do not compile Kotlin or
prove document-provider behavior, visual quality or accessibility on a device.
The parent verifier must compile through the factory build route and exercise:
source loading/failure; first render and re-render; original/result selection;
inspection dismissal and large text/landscape; PNG save, cancellation and denied
write; and saving while navigating away. Decode the saved PNG and compare it with
the rendered bitmap, including when Original was selected. Confirm the local draft
is never represented as marketplace-checked.

## Claim ledger

Paths below are relative to the slot.

| Claim | Label | Evidence |
|---|---|---|
| Image-led stage, selected comparison and inspection are implemented in source | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/card/CardScreen.kt`: `preview`, `PlinthStage`, `FilterChip`, `Dialog` |
| Draft save is wired to a real PNG document write | MEASURED | `CardScreen.kt`: `CreateDocument`; `CardViewModel.kt`: `exportCard`, `Bitmap.CompressFormat.PNG`, `stream.use` |
| Marketplace state is explicitly unselected, not certified | MEASURED | `app/src/main/res/values/strings_card.xml`: `card_target_unselected`, `card_export_guidance`, `card_saved`; matching `values-ru` resources |
| String resolution and EN/RU parity were checked | MEASURED | Python source-only command: 25 resolved references per locale, 15 matching translated keys |
| Compilation and device behavior | UNVERIFIED | Parent verifier: factory debug build and the device checks above; worker was forbidden to run Gradle |

Elapsed worker time at the source-check checkpoint: approximately 3 minutes 24
seconds (task timestamp 01:13:15 to observed clock 01:16:39, UTC+03:00).

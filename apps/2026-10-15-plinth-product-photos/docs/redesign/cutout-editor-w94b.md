# W94b — Cutout subject selection and mask editor

## Gesture diagnosis

The erasing path was the mask canvas `detectDragGestures` handler: it collected image
coordinates, consumed pointer movement, and submitted a stroke on drag end. In the
previous shared, scrollable screen, a vertical drag over that canvas could therefore
become a brush stroke instead of a scroll.

The single-meaning rule is now **the image canvas never scrolls**. In the mask editor,
the fixed canvas is a sibling of the independently scrollable control column. A
vertical drag on the canvas applies only the visibly active brush mode; a vertical
drag in the controls scrolls those controls. On the pre-cut selection surface, a tap
sets the keep seed and the canvas has no drag handler. The review image is display-only
and has no pointer handler.

## Flow

```text
Pick photo
    ↓
Select subject
    ├─ Tap object to keep → mark the keep seed
    ├─ Select all         → keep every photo pixel
    └─ Reset selection    → clear the explicit choice
    ↓
Cutout runs; a tap seed selects its nearest connected component
from the returned mask (a miss is disclosed; automatic mask remains)
    ↓
Review result on neutral checkerboard, with the mask visibly highlighted
    ├─ Edit edges → separate mask editor → Back to review
    └─ Use this   → existing product-composition editor with this mask
```

Subject selection is local: the tap is not sent as a server segmentation parameter.
After the server returns its mask, the app keeps the connected component nearest the
tap. If there is no foreground component near it, the app shows the automatic result
and a notice instead of claiming the tap changed the cut.

## Controls

| Screen | Label | Effect |
|---|---|---|
| Select subject | “Tap the object you want to keep” | A tap marks a keep seed; the detected mask is filtered around that point after processing. |
| Select subject | “Select all” | Keeps every photo pixel, including the background. The whole image is visibly highlighted. |
| Select subject | “Reset selection” | Clears the explicit pre-cut choice; disabled until a choice exists. |
| Select subject | “Cut out item” / cost label | Starts the cut only after an explicit subject choice. |
| Review | “Edit edges” | Opens the separate mask editor. |
| Review | “Use this” | Opens the product-composition editor with the reviewed mask. |
| Mask editor | “Remove from selection” | Makes a drag erase mask alpha; the selected chip is visibly active. |
| Mask editor | “Restore to selection” | Makes a drag restore mask alpha; the selected chip is visibly active. |
| Mask editor | Brush diameter slider | Changes the stroke diameter, shown as a percentage of the shorter image side. |
| Mask editor | “Undo last brush stroke” | Restores the mask before the latest stroke; history retains 10 strokes. |
| Mask editor | “Redo last brush stroke” | Reapplies the most recently undone stroke. |
| Mask editor | “Select all” | Makes the entire photo, including background, part of the mask. |
| Mask editor | “Reset selection” | Restores the mask first shown for this cutout. |
| Mask editor | “Back” | Returns to review; edits are persisted as they are made. |

The review screen has exactly two actions: “Edit edges” and “Use this”. No
“Save generated variant” control or copy is present in the cutout app source.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The mask canvas drag handler is the only gesture path that submits a brush stroke; it consumes movement and submits on drag end. | MEASURED | `EditorScreen.kt:287-300` |
| The brush canvas and the scrollable controls are siblings; selection uses taps, and the review preview is not a gesture surface. | MEASURED | `EditorScreen.kt:95-111`, `EditorScreen.kt:176-189`, `EditorScreen.kt:266-300`, `CutoutScreen.kt:270-308`, `CutoutScreen.kt:321-332`, `CutoutScreen.kt:439-477` |
| Review shows the cutout over a checkerboard and offers only “Edit edges” and “Use this”. | MEASURED | `CutoutScreen.kt:270-308`, `CutoutScreen.kt:439-477` |
| A keep seed selects the nearest connected component from the returned mask; a miss leaves the automatic mask and raises a notice. | MEASURED | `CutoutViewModel.kt:726-757`, `CutoutViewModel.kt:1131-1208` |
| Brush mode controls whether a stroke writes opaque or transparent mask alpha. | MEASURED | `EditorScreen.kt:205-218`, `CutoutViewModel.kt:408-430`, `CutoutViewModel.kt:1291-1316` |
| Undo history defaults to ten snapshots; the editor receives the selected mask as its initial composition mask. | MEASURED | `MaskEditHistory.kt:3-5`, `EditorViewModel.kt:48-68`, `LocalCompositionSession.kt:33-40` |
| New cutout/editor strings have matching English and Russian resources; the slot locale check passed. | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos --only ui,project,policy --json` (`slot.locales`: PASS); `values/strings_cutout.xml:67-78`, `values-ru/strings_cutout.xml:67-78`, `values/strings_editor.xml:3-7`, `values-ru/strings_editor.xml:3-7` |
| No “Save generated variant” copy or key is present in the cutout app source. | MEASURED | `rg -n "Save generated variant|save_generated_variant|cutout_save_variant|cutout_save_generated" apps/2026-10-15-plinth-product-photos/app` (no matches) |
| Android compilation is not verified in this change. | UNVERIFIED | `node scripts/verify.mjs --slot plinth-product-photos --build`, run by the integrator; Gradle was intentionally not run here. |
| On-device interaction is not verified against the current source revision. | UNVERIFIED | `apps/2026-10-15-plinth-product-photos/device-verification.json`; the integrator must exercise the current build on the attached phone and record a fresh device run. |

The source-check command exited 1 only because `slot.device` reported that the existing
`device-verification.json` build digest is stale for the current source revision.
The UI, locale, policy, and project rows reported by that invocation passed; no device
record was changed.

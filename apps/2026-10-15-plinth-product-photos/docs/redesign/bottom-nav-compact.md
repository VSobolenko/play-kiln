# W18: compact bottom navigation

## Geometry and decision

Source: `app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthNavBar.kt`
(paths and line numbers below refer to this slot).

Before, observed in this worker's initial read:

- `PlinthNavBar.kt:49-59`: a Box with `.heightIn(min = 104.dp)` enclosed
  a row with `.padding(top = 56.dp).heightIn(min = 48.dp)`.
- `PlinthNavBar.kt:75-79`: the 56 dp FAB was separately top-aligned.
- The 1 dp divider at lines 43-47 made the nominal band at least 105 dp,
  excluding system navigation insets.

After:

- `PlinthNavBar.kt:49-54`: one Row uses `.heightIn(min = 72.dp)` followed
  by `.padding(horizontal = 8.dp, vertical = 8.dp)`. No 56 dp top
  reservation remains.
- `PlinthNavBar.kt:56-77`: Studio, Library and More keep equal weights;
  the persistent 56 dp create FAB is the last child of the same row.
- The default-size content height is inferred as 72 dp: 56 dp FAB plus
  8 dp padding above and below. Including the unchanged divider gives
  73 dp, a nominal reduction of 32 dp. These are source-derived dimensions,
  not measured device pixels. Insets remain the scaffold's responsibility;
  no inset handling or scaffold code was changed.

Chosen pattern: an in-row, trailing circular FAB beside three labelled tabs.
This reuses the existing Compose/Material calls, icons, localized labels,
selection callbacks and create callback without new dependencies or APIs.
Create remains an action, not a fourth selected destination.

Rejected alternatives:

- An overlapping centre FAB avoids the old reservation but needs a horizontal
  gap and overlap accommodation to avoid covering the centre tab or content.
  A trailing in-row FAB has no overlap to accommodate.
- Replacing everything with NavigationBar/NavigationBarItem adds a component
  migration without being needed to remove the measured top reservation.
- A fixed-height row or hiding labels would trade away text growth or destination
  discoverability. Neither is needed.

## Accessibility and wide windows

`PlinthNavBar.kt:138-156` retains each tab's 48 dp minimum height, tab selection
semantics, localized accessible name, and single-line label with
`TextOverflow.Ellipsis`. At a 320 dp available phone width, the three weighted
tabs each receive `(320 - 16 - 56) / 3 = 82.67 dp` of width. The FAB remains
56 by 56 dp with its localized create content description.

The row and tab heights are minimums, not maximums. With the current 16 sp
labelSmall line height (`ui/theme/Type.kt:82-86`), the source-derived icon-plus-label
height is 36 dp at 1.0x, and 40.8 dp at a linear 1.3x scaling, below the tab's
48 dp minimum. Taller text can grow the row rather than meet a hard height cap.
Actual font scaling, touch interaction and device rendering remain UNVERIFIED.
`PlinthNavigationRail` is unchanged.

## Observed source-only verification

Command, both before and after:
`node scripts/verify.mjs --slot plinth-product-photos` (no `--build`).

Both runs reported PASS for `uiux.text-layout` and `uiux.a11y`. The after run
reported `122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL`, exit 1. The sole FAIL in
both runs was `slot.device`: the recorded build digest already disagreed with
the working source before this edit. No record was changed to bypass it.
The after run reported `subprocesses: 0`. No Gradle command was run.

Logs: `.kiln/scratch/copilot-plinth-w18-bottom-nav/verify-before.txt` and
`verify-after.txt` at repository root.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Removed 104 dp minimum and 56 dp empty reservation | MEASURED | Before excerpts above; after `PlinthNavBar.kt:49-54` |
| Nominal band is 73 dp including divider, excluding system insets | INFERRED | `PlinthNavBar.kt:43-54,68-70`: 1 + 8 + 56 + 8 |
| Three destinations and persistent create action remain | MEASURED | `PlinthNavBar.kt:56-77` |
| Text-layout and accessibility source gates did not regress | MEASURED | Both source-only verification logs: both rows PASS |
| Overall verification is not green | MEASURED | Both logs: exit 1, stale `slot.device` |
| Compilation and rendered 1.0x/1.3x geometry | UNVERIFIED | Gradle forbidden by task; no device run performed; orchestrator must build and review on device |

Elapsed worker time: approximately 3 minutes (00:56-00:59 +03:00), including
both source-only verification runs.

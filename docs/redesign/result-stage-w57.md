# W57 - AutoCard result media-first stage

## Change

`AutoCardResultScreen` now gives its first content slot to a height-weighted
`FormingCardPreview`, sized to fit the available stage without distorting the
source aspect ratio. Before/After chips sit over the image plane and appear only
when both decoded bitmaps are available and the result is not in a failed state.
Their labels are localized in English and Russian.

Output loading/saving/ready/failure copy, retry, regenerate, and save feedback
are below the media stage. The existing Share and Save buttons remain in the
bottom bar. `FormingCardPreview` itself and its other callers were not changed.

## Verification

- **MEASURED:** Inline Node source assertions passed for the weighted stage,
  media-before-status order, conditional comparison controls, bottom-bar
  Share/Save actions, and both locale labels. The relevant screen is
  `PlayKilnApp.kt:1065-1156`; labels are at
  `values/strings_autocard.xml:8-9` and
  `values-ru/strings_autocard.xml:8-9`.
- **MEASURED:** `runGradleTasks(..., [':app:compileDebugKotlin'])` completed
  with `BUILD SUCCESSFUL in 34s`, exit 0.
- **MEASURED:** Python `xml.etree.ElementTree` parsed both updated AutoCard
  string resources; `git diff --check` returned exit 0.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Result media occupies the weighted first content slot, before state copy | MEASURED | `PlayKilnApp.kt:1065-1156`; post-change structural assertion |
| Before/After is localized in English and Russian | MEASURED | `values/strings_autocard.xml:8-9`, `values-ru/strings_autocard.xml:8-9` |
| Shared preview implementation and other callers were left unchanged | MEASURED | No edits to `ui/autocard/FormingCardPreview.kt`; `AutoCardScreen.kt:654`, `StudioRoute.kt:176` |
| Rendered sizing and touch behavior on device | UNVERIFIED | Install a debug APK and inspect portrait/landscape result states and the overlay controls |

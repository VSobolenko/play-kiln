# W53 - AutoCard result stage

**Status:** blocked by the requested file boundary; no production UI changes made.

## Source findings

The AutoCard success destination is implemented as the private
`AutoCardResultScreen` in `app\src\main\java\com\gexetr\plinthproductphotos\ui\PlayKilnApp.kt`,
not in `ui\autocard`. The AutoCard route pushes `AutoCardResultDestination` at
`PlayKilnApp.kt:496-498`, and that destination calls the private screen at
`PlayKilnApp.kt:505-514`.

Its current result layout is:

| Element | Source |
|---|---|
| Persistent actions | `PlayKilnApp.kt:996-1050` puts Share and Save in the scaffold `bottomBar`. Share is a filled `Button`; Save is an `OutlinedButton`; both have equal weight and 52 dp minimum height. |
| Scrollable content | `PlayKilnApp.kt:1053-1059` wraps the result body in a vertically scrolling `Column`. |
| Status and secondary messages | `PlayKilnApp.kt:1060-1090` renders ready/error/retry copy before the comparison and image. |
| Before/after control | `PlayKilnApp.kt:1091-1107` shows two chips only when source and result pixels are both available. They switch `showOriginal` between the source and result. |
| Result image | `PlayKilnApp.kt:1108-1118` renders `FormingCardPreview` after the text and chips, using the output's aspect ratio. |

The before/after behavior and persistent Share/Save actions therefore already
exist. The image is not first in the success-stage hierarchy, and source alone
does not prove that it occupies 70% of the rendered content area; that needs a
device or screenshot measurement.

## Required follow-up to implement W53

Move the success-stage composition into an AutoCard-owned result composable,
then update the destination call in `PlayKilnApp.kt` to render it. In that
composable, put the output image first and give it the available height, place a
Before/After control directly with the image when both bitmaps exist, keep Share
and Save in the persistent bottom row, and move status/retry copy below the
visual result. Preserve the loading, saving, missing-output, and failed-output
states and the existing export behavior. Add localized Before/After labels to
`values\strings_autocard.xml` and `values-ru\strings_autocard.xml` if replacing
the current labels.

This cannot be wired under the supplied allowlist: `PlayKilnApp.kt` is both the
destination owner and explicitly excluded, while adding an unused composable in
`ui\autocard` would not change the success destination. No code or resource
changes were made to imply otherwise. No device/screenshot verification was
possible within the permitted files.

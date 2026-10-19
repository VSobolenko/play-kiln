# W9: routed studio UX

## What changed

`app\src\main\java\com\gexetr\plinthproductphotos\ui\studio\StudioRoute.kt`
keeps the balance, reserved credits, credit explanation and top-up action in the
`PlinthScaffold` top bar. Unknown balance still uses the existing localized
unknown-balance message, and top-up remains available from the header. Operation
cards now show a localized purpose, an existing Lucide drawable, and a full-width
action whose cost chip also states the available balance. Generated scene is the
filled primary action; the other operations use outlined actions. Wire IDs remain
only for quote lookup. Availability reasons and all eight existing operation rows
remain.

The confirmation displays its captured quote, current balance, and a remainder
derived from the current balance. It shows the unknown-balance or shortfall state
and a top-up action when needed. `studioConfirmationPreview` gates both opening
and submitting on balance, quote equality, operation availability, kill switch,
daily cap, row/provider availability, photo and prompt presence, and busy/pending
state. Submission still passes the displayed quote to `StudioViewModel.start`.

The route remains inside `PlinthScaffold`; its list now leaves 24 dp of end
content padding and adds no duplicate system-inset padding. This replaces the
16 dp bottom padding observed in the route at task start.

`app\src\main\java\com\gexetr\plinthproductphotos\ui\studio\StudioViewModel.kt`
adds the pure confirmation-preview/eligibility calculation. Its existing
`refreshQuote()` still fetches the wallet and config from the API; `start()`
continues refreshing them and rejecting a mismatched displayed quote with
`PRICE_CHANGED`.

`app\src\main\res\values\strings_studio.xml` and
`app\src\main\res\values-ru\strings_studio.xml` add only the route's scene/upscale
purposes, action cost-and-balance labels, projected remainder and changed-quote
message. Existing W6 keys supply the top-up label, credit explanation, and
purposes for relight, recolour, expand, erase, virtual model and ghost mannequin;
the W6 resource files were not modified.

`app\src\main\java\com\gexetr\plinthproductphotos\ui\studio\StudioToolsScreen.kt`
now labels itself as the superseded catalogue screen. It was not deleted.
`app\src\test\java\com\gexetr\plinthproductphotos\ui\studio\StudioConfirmationPreviewTest.kt`
covers unknown/insufficient balance, quote changes, balance remainder, and
existing server/busy blockers.

## Measured before and after

At task start, the live route's first scrolling `PlinthStage` item contained the
balance, reserved amount and wallet action; each operation row displayed its wire
ID and a separate cost line; the dialog showed the cost without the live balance
or remainder. The source was routed through `StudioRoute` (the caller sweep found
it in `ui\PlayKilnApp.kt`; `StudioToolsScreen` had only its declaration).

After the change, the header is at `StudioRoute.kt:97-136`, the list end padding is
at `StudioRoute.kt:142`, cost-and-balance action content is at
`StudioRoute.kt:408-449`, and confirmation preview and quote submission are at
`StudioRoute.kt:335-396`. No `Text` presents an operation wire ID. Quote lookup
still uses `operation.wireId()` at `StudioRoute.kt:203` and `:337`; the final
server-side price check remains at `StudioViewModel.kt:175`.

The inset wrapper was read at
`app\src\main\java\com\gexetr\plinthproductphotos\ui\components\PlinthScaffold.kt:27-45`:
it applies scaffold safe-drawing insets, consumes the inner padding and applies
IME padding. The route does not add those insets again.

## Rejected changes and limits

- Did not add or copy a price table. Each action reads `state.config`'s current
  operation quote; no fallback price is provided.
- Did not edit `PlinthScaffold`, `PlayKilnApp`, the server or the domain operation
  catalogue. The current wrapper and route already provide the required surfaces.
- Did not delete the superseded screen; deletion is a product decision.
- Did not claim a device layout result. Gradle, including unit tests and builds,
  was not run because this task explicitly forbids it; `kotlinc` was unavailable.
  Compilation and on-device scrolling therefore remain unverified.

The available checks were source/resource validation and whitespace checking,
not a substitute for the orchestrator's authoritative build.

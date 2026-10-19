# W8: one photo, one smart-card flow

## Implemented flow

1. Import uses the existing Android Photo Picker. The selected URI is previewed
   while its private copy is saved; the imported file then takes its place.
   `SelectedMediaPreview` remains the shared, large, fit-without-cropping preview.
   Import retains replacement, batch selection, AI-card and local-card actions.
2. Smart card also uses `SelectedMediaPreview`, with a display-sized bitmap of the
   actual source and its original aspect ratio. Replacement has a real callback,
   `onChangePhoto`; its backward-compatible default is `onBack`. See the required
   route wiring below to make replacement independent of entry history.
3. Opening smart card no longer sends the image for recognition automatically.
   The dock first shows the recognition quote from `/config`'s operation map and
   the current `/wallet` balance. The operation key is obtained from the existing
   `OperationId.RECOGNISE` serializer, not another price table. Recognition is an
   explicit action, enabled only after the source bitmap and a usable quote exist.
   A missing/invalid quote is not interpreted as zero. Disabled operations,
   insufficient balance and unavailable quotes have readable states. Refresh and
   manual category selection execute real routes.
4. Choosing a category manually remains available, including offline. Recognition
   cannot overwrite a category chosen while its request was outstanding.
5. Recognition/composition activity occupies `FormingCardPreview`, at the expected
   output aspect ratio: the supplied `Canvas`, or the source dimensions for an
   original-only marketplace. A theme-derived animated gradient sweep replaces
   the bare spinner. The message is centered inside the same bounded surface.
   Recognition or category/scene selection brings that surface into view, without
   automatically scrolling away from the source on initial entry.
6. The actual composed bitmap replaces the skeleton in that same surface; the
   sweep fades over 350 ms. Original/card comparison remains available. No
   completion effect invokes navigation. Only the docked explicit card/original
   action calls the existing `onCardReady` handoff.
7. Catalog, composition and display-bitmap failures remain in the flow, with
   localized explanations and retry. Cancellation is propagated, not converted
   into an error. Existing recognition/offline and unavailable-scene explanations
   and original-photo fallback are retained.
8. Import, smart card and the category sheet use `PlinthScaffold`. They do not
   apply their own navigation-bar or keyboard padding. Main actions use its
   `bottomBar`; vertical content is scrollable inside its already-inset content
   bounds, including the final category group. The shared scaffold owns clearance.

## Evidence behind the decisions

| Decision | Source read for this task | Rejected alternative |
|---|---|---|
| Actual photo before an explicit AI action | Owner's W8 report; existing `SelectedMediaPreview.kt` | Filename, icon-sized thumbnail, or automatic recognition before review |
| Quote and current balance before recognition | `competitor-teardown.md:56`, Pixelcut credit-picker quotation | An invented price, a copied client-side price table, or treating a missing quote as free |
| Bounded shimmer that ends at the bitmap | `competitor-teardown.md:211`, `:315-324`, recorded Apple HIG/Midjourney guidance | A floating spinner, fixed offsets, or a simulated percent complete |
| In-place preview, explicit handoff afterward | Owner's W8 request D; teardown section 4.4 | Navigating away when a coroutine completes |
| Shared inset-aware dock and scroll bounds | Existing `ui/components/PlinthScaffold.kt:17-54`; owner's last-item report | Another independent IME/navigation-padding implementation |

`ai-visual-language.md` did not exist when checked in this task. No competitor
screenshots or phone observations were fabricated.

## Price boundary

There is no paid scene-generation request in this smart-card route:
`AutoCardEngine.compose` composes locally; unavailable generated assets stay
unavailable rather than showing an enabled, nonexecuting generation control.
Only the recognition operation has a server-config quote in this flow. No
numeric price was added to production UI code or resources.

The server reading at implementation time declared O-01 with zero credits
(`server/src/registry.ts:42-44`); the client does **not** encode that value.
`server/src/routes.ts:318-385` handles recognition directly and does not accept an
estimated-credit argument or return a charge receipt. If the pricing worker makes
recognition paid, that worker must also enforce quote acceptance/charging on this
endpoint. This UI cannot supply an after-the-fact debit receipt that the endpoint
does not return. Do not display the recognition quote as a composition charge or
invent a receipt on the result screen.

## Required handoff: exact `PlayKilnApp.kt` edits

W8 did not edit this file. Existing callers retain their named parameters and
defaults; compilation remains unverified. The following integration belongs to
its owner.

### 1. Make replacement reach Import from every entry point

In the existing `AutoCardRoute(...)` call in `entry<AutoCardDestination>` (read at
line 257), add this named argument. Do not alter `onCardReady`'s output-storage
handoff or push the result destination from a completion effect.

```kotlin
onChangePhoto = {
    backStack.pop()
    if (backStack.lastOrNull() != ImportDestination) {
        backStack.push(ImportDestination)
    }
},
```

Without this addition the functional fallback is Back: from Import it returns to
Import, but direct Home/Library/Coach entry follows that entry's existing history.

### 2. Replace the pre-route bare spinner where the aspect ratio is known

`WithLoadedDestination` (read at line 524) currently renders an unbounded
`CircularProgressIndicator` for every loading destination. Add a trailing optional
parameter before `content`:

```kotlin
loadingContent: @Composable () -> Unit = { CircularProgressIndicator() },
```

Replace its `DestinationDataState.Loading` branch with `loadingContent()`.
At the smart-card call of `WithLoadedDestination`, inside
`ProjectImageState.Ready`, pass the following. Other destinations can keep their
existing default until their owners change them.

```kotlin
loadingContent = {
    PlinthScaffold {
        FormingCardPreview(
            bitmap = null,
            aspectRatio = loaded.image.source.width.toFloat() / loaded.image.source.height,
            busy = true,
            message = stringResource(R.string.home_nav_photo_loading),
            failed = false,
        )
    }
},
```

Import `com.gexetr.plinthproductphotos.ui.autocard.FormingCardPreview`.
The current route supplies a source-sized `Canvas`; if that changes, use the same
new expected canvas ratio here instead of guessing a square.

### 3. Result destination: retain shape and move working handoffs into the dock

`AutoCardResultScreen` (read at line 609) currently has a bare Loading spinner,
Saving text and an image converted by `toBitmap()` during composition.

* At function scope, after its save/load `LaunchedEffect`, compute
  `val resultPixels = (outputState as? AutoCardOutputState.Ready)?.output?.pixels ?: pixels`.
  Remove the duplicate declaration at the bottom of the content column.
* Add `val sourceState = rememberProjectImage(photoId)` and use
  `(sourceState as? ProjectImageState.Ready)?.image?.source` as the before image
  and geometry fallback. The current smart-card canvas equals that source's
  dimensions. Never invent a ratio while both source and result are absent.
* Decode `resultPixels` to a display-sized bitmap in a
  `LaunchedEffect(resultPixels, saveAttempt)` using the existing suspend extension
  `resultPixels?.autoCardPreviewBitmap()`; import
  `com.gexetr.plinthproductphotos.ui.autocard.autoCardPreviewBitmap`.
  Store the nullable bitmap and a decode-failure Boolean in remembered state keyed
  by `resultPixels` and `saveAttempt`. Rethrow `CancellationException`; expose
  other decode failures through the existing save-error message. Including
  `saveAttempt` makes the existing Retry save action also retry display decoding.
* Replace the bottom `if (resultPixels != null) { Image(...) }` block with
  `FormingCardPreview`. Its ratio is
  `(resultPixels ?: sourcePixels).width.toFloat() / (...).height`, only when that
  buffer exists. Pass the decoded bitmap, and set `busy` for Loading, Saving or
  pending bitmap decoding, but not after a decode failure. Set `failed` for Failed,
  Missing or decode failure. Use `home_nav_auto_card_saving` while saving,
  `home_nav_photo_loading` while loading/decoding, and the existing unavailable/
  save-error resources on failure. Remove the bare Loading spinner; keep Missing,
  Failed and Retry actions, without popping the route.
* Add source/result comparison here using the loaded `sourcePixels`, not a mock
  before image. Use the same display-sized off-main-thread conversion for it.
* Move the existing Ready-state Share/Save-to-folder `Row`, unchanged in its
  execution handlers, into `PlinthScaffold.bottomBar`, inside a padded Surface.
  Do not add navigation or IME padding; the scaffold already supplies it.
  Preserve the existing storage, share intent, document picker and error routes.
  The existing automatic `saveAutoCard` is already the library-save behavior:
  do not add a second inert "Save to Library" button or duplicate the write.

The result owner must not invent an "Export pack" action without an actual
single-card-to-pack execution route, nor a credit receipt absent from the server.

## Validation observed by W8

No Gradle command was run. W8 did **not compile**, run JUnit, install an APK, take
screenshots or exercise a phone.

* `node scripts\check-locales.mjs --slot plinth-product-photos` exited 0:
  one translated language, 879 default/translated entries, matching format
  arguments and plural categories at the first reading. The final reading at
  23:57:36 +03:00 also exited 0 with 885 entries as concurrent workers added keys.
* `node --input-type=module -e 'import("./scripts/core/localization.mjs").then(m => console.log(JSON.stringify(m.localizationFindings("apps/2026-10-15-plinth-product-photos"))))'`
  returned `[]`.
* An inline Python/XML/source assertion run exited 0: all 93 owned resource
  references existed in both locales; all 10 exclusive W8 keys had matching format
  arguments; source contracts checked shared scaffold usage, docked actions,
  explicit recognition consent, config/wallet quote provenance, expected-aspect
  shimmer/fade and scaffold-wrapped category scrolling.
* `git diff --check` on the owned flow directories exited 0.
* `AutoCardCreditQuoteTest.kt` was added for changed server quotes, absent/invalid
  quotes, a server-provided zero quote, balance boundaries, disabled operations,
  kill switch and daily cap. It has **not been executed**.

The orchestrator owns compilation and JUnit execution. Device validation must
still prove a real selected image, centered shimmer in portrait and landscape
canvases, in-place success/failure, the final category chip, large-font scrolling,
keyboard clearance, and the parent-file integration above.

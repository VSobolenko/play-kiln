# W6: one visible wallet

## Scope and authority

This worker changes the routed wallet and the unreferenced studio catalogue, not
the routed studio. No price, economic plan, server implementation, operation
catalogue, wallet ledger or application route was changed.

The measured Kotlin caller sweep (`rg "StudioToolsScreen|StudioRoute|WalletRoute"
app/src -g "*.kt"`) found only the declaration of `StudioToolsScreen`;
`ui/PlayKilnApp.kt` routes to `StudioRoute` and `WalletRoute`. The catalogue remains
in place; improving it does **not** fix the live studio.

## Owned changes

| File, relative to this slot | Change and reason |
| --- | --- |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/wallet/WalletScreen.kt` | Balance and one-tap top-up stay in the scaffold header, outside the list. Top-up scrolls to the pack section. Selectable pack cards replace repeated Buy buttons; one pinned purchase action shows the selected product, the store price, balance preview and purchase status. Processing credits, the one-time unlock, recovery, jobs and activity are separate sections. |
| Same file, state helpers | `balanceAfterTopUp` derives a **preview** from the existing billing catalogue grant and server balance; it does not change the balance. Offline and non-pack products return no top-up projection. `canPurchase` requires a known store price, a connected wallet/billing state and a valid product; pending/confirming purchases and already-owned unlocks cannot be submitted again. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/wallet/WalletViewModel.kt` | Purchase submission uses the same eligibility predicate as the button, including the existing product-ID guard. Server responses remain the only source of credited balance; no optimistic crediting was added. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/wallet/WalletRoute.kt` | Refreshes the wallet on lifecycle resume, including return from Google Play or another screen. The existing initialization refresh is retained. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioToolsScreen.kt` | Pins the supplied balance and wallet action above the list. Roomier cards replace narrow trailing actions and wire-code overlines. Each tool has a plain-language purpose; the two upscale resolutions are distinguishable. The series controls sit beside the series card. Selection opens a scrollable confirmation using the current offer's cost, balance and projected remainder; only an available offer can continue. No tool, availability reason or operation callback was removed. |
| `app/src/main/res/values/strings_wallet.xml`, `values-ru/strings_wallet.xml` | Removed the four wallet-only USD hypothesis/arithmetic strings. Purchases display only `localizedPrices`, not the economics hypotheses. Existing pack-name resources remain unchanged. |
| `app/src/main/res/values/strings_studio.xml`, `values-ru/strings_studio.xml` | The four catalogue-only cost strings explicitly label current balance and the post-delivery remainder. Series reservation is no longer described as a charge. No shared resource key was edited. |
| `app/src/main/res/values/strings_wallet_w6.xml`, `values-ru/strings_wallet_w6.xml` | Twelve wallet-only bilingual strings: one balance, what credits pay for, pack selection, top-up previews and the separate unlock/recovery sections. |
| `app/src/main/res/values/strings_studio_w6.xml`, `values-ru/strings_studio_w6.xml` | Sixteen catalogue-only bilingual strings: purpose descriptions, top-up and confirmation. These keys are intentionally exclusive to the owned catalogue, not shared with the live route. |
| `app/src/test/java/com/gexetr/plinthproductphotos/ui/wallet/WalletBalancePresentationTest.kt` | Seven unit tests cover catalogue-derived previews, no balance mutation, integer-overflow avoidance, unknown/offline balances, purchase eligibility, duplicate submission gates, the confirmed unlock, and quantities against `economics.json`. |
| `app/src/test/java/com/gexetr/plinthproductphotos/ui/studio/StudioToolPurposeTest.kt` | Three unit tests cover the listed purposes, different 2K/4K purposes, and rejection of an unsupported tool. |

`StudioViewModel.kt` was read but left unchanged: `refreshQuote()` already obtains
the wallet from `api.getWallet(session.deviceToken)` and writes that server
response to the session store. It does not decrement balance locally.
`WalletViewModel.applyServerWallet()` likewise uses the server response.

### Packs, not invented prices

The pack list and preview quantities come from
`state.productIds.billingCatalog().consumables`, the same catalogue the billing
coordinator uses. The measured quantities are **90 / 230 / 600**, matching
`economics.json.priceLadder.{starterPack,studioPack,proPack}.credits`.
The UI has no additional pack quantity or currency price table.
`starterCreditsPerDevice` is **10** in the economics contract; this UI neither
grants it nor pretends to calculate the number of generations it buys.

### Insets

Both owned lists pass scaffold top/bottom space into `contentPadding` and consume
those insets instead of clipping the list with `Modifier.padding(innerPadding)`.
Their final item has additional bottom spacing. The wallet's pinned purchase
surface has `navigationBarsPadding()` and `imePadding()`; its recovery list
also handles and consumes the keyboard insets. The slot manifest already declares
`adjustResize`. These are source observations, not a device-layout verdict.

## Handoff: exactly three changes for the owner of `StudioRoute.kt`

1. **Move the balance and wallet action out of the scrolling content.** Its
   current first `LazyColumn.item` contains `state.balance`, `state.reserved` and
   `onOpenWallet`. Put them in the `PlinthScaffold.topBar` lambda, after the
   existing `TopAppBar`, inside a surface/column. Use `state.balance` directly;
   retain the unknown-balance message rather than substituting zero. Keep
   `onOpenWallet` enabled even at zero or unknown balance. Add a live-route-owned
   EN/RU explanation of credits at first exposure and label the action as
   top-up. Remove the scrolling duplicate, not the callback. The wrapper already
   consumes scaffold insets and applies `imePadding()`; do not apply those same
   insets twice. Retain enough list `contentPadding` for the last photo/variant/
   result action. Any newly pinned bottom action must clear the navigation bar
   and keyboard once (the wrapper already pads its `bottomBar`).

2. **Give every operation card a purpose and a glyph, not a wire-code heading.**
   Extend the private `StudioRow` presentation with a localized purpose and a
   real, available glyph from the app's existing icon vocabulary. Make the
   operation name/purpose primary; remove `Text(row.operation.wireId())` from
   the product-facing hierarchy. Keep wire IDs for the config lookup. Add
   route-owned EN/RU purposes for scene, relight, recolour, expand, erase,
   virtual model, upscale and ghost mannequin; keep every current disable
   reason and server gate. Put actions below full-width text instead of in a
   narrow trailing slot, and use a single dominant confirmation action rather
   than treating all cards as equally primary. Each card's quote must pair
   `quote.credits` with `state.balance`; do not hardcode an operation price.

3. **Show current balance, accepted quote and remaining balance in confirmation.**
   The current `confirmation: Pair<OperationId, Int>?` stores the quoted cost,
   while the dialog displays cost alone. For `(operation, price)`, show the
   operation purpose, `price`, current `state.balance`, and
   `balance - price` only when balance is known and sufficient; otherwise show
   the unknown/shortfall message and a one-tap wallet action. Derive this
   preview on recomposition, not from a remembered balance or a locally
   decremented counter. Disable confirmation if balance is unknown/short,
   `state.config?.ops?.get(operation.wireId())?.credits != price`, its
   `enabled` flag is not true, a kill switch/cap is active, the row is blocked,
   or the existing busy/pending/photo/prompt conditions fail. Continue to call
   `model.start(operation, prompt, price)` with the **displayed** quote;
   its existing `refreshQuote()`/`PRICE_CHANGED` check must remain the final
   pre-submit guard. Use route-owned bilingual resources for the preview;
   do not repurpose these W6 catalogue-only keys into shared strings.

The live route is owned by another worker. None of these three changes is claimed
as landed by W6.

## Observed checks and limits

- PowerShell inline Python XML/resource/pack/source audit, exit **0**:
  **117** string references from owned Kotlin files resolve in EN and RU;
  **28** new keys have matching formatting placeholders and actual translations;
  catalogue grants match economics at **[90, 230, 600]**;
  edited/new keys have no unowned production callers; removed keys have no Kotlin
  references; owned source delimiters balance. Delimiter checking is not Kotlin
  parsing or compilation.
- Scoped `git diff --check`, exit **0**, no whitespace diagnostics.
- **Did not compile. Did not run Gradle, the Android/JUnit tests, a build, lint,
  device tests, a screenshot capture or release preparation.** The ten unit
  tests were added, not executed. The orchestrator owns that build slot.
- Phone/tablet layout, large fonts, keyboard reachability, real wallet refresh,
  Play purchase completion, and integration with W5's final price-contract
  changes remain **UNVERIFIED**. The orchestrator must compile the combined
  workers' diff and exercise both the wallet and live studio on-device.
- No commit, staging, reset or other Git-state mutation was performed.

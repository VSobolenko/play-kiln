# Cutout redesign

This implements item 8 in `modern-ai-android-2026.md`.

- Before delivery, the selected source photo is the image-stage hero. Upload,
  queue, running and download feedback sits over its bottom edge, inside the
  image bounds. Only the server's running percentage is shown as determinate;
  other stages use indeterminate progress.
- Once a result exists, matched `Before · original` and `After · cut-out`
  panels show the untouched source beside the returned transparent cutout.
  The existing mask-refinement canvas, brush controls, undo and report action
  remain available below the comparison.
- The submit button includes the current credit price, with the balance and a
  localized note that credits are charged only after delivery confirmation.
  `RECEIPT_PENDING` explicitly says no charge is confirmed yet; `SAVED` with
  the delivered job id says the price was charged once. Existing wallet refresh
  and failure/release messages remain the source of truth for wallet outcomes.
- If a delivered photo is quoted again, the new quote remains on the action;
  the previous comparison yields to the active image-stage progress until the
  new result is ready.

The screen uses existing Compose/Material 3, `PlinthScaffold`, theme surface
roles and localized English/Russian resources. The ViewModel, Wallet surface
and dependency graph are unchanged.

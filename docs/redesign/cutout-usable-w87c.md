# W87c — Cut out product: usable with a photo already chosen

Slot: `apps/2026-10-15-plinth-product-photos`. No Gradle was run; nothing below is build-verified.

## Changes
- `CutoutViewModel.refreshCostAndBalance` opens the newest session photo (`ProjectRepository.recentPhotos().first()`) when none is loaded; the screen calls it on `ON_RESUME`, so the picker wall is gone. Pending jobs/receipts resume as before; a pending request with no job only reloads its photo and quote (never restarts a paid job without a tap).
- `select(uri)` body extracted into `loadPhoto(photo)`; the picker is now a "Use a different photo" text button (full button only when no photo exists).
- Selected-name hero line removed; job IDs no longer rendered (`cutout_credits_returned`, `cutout_release_unconfirmed` replace `wallet_job_released`/`wallet_job_release_unconfirmed`).
- "Refresh cost and balance" button removed; quote refreshes silently on resume; "Try again" shown only after a network/session notice. CTA keeps cost (`cutout_start_cost`).
- Title: "Cut out product" / "Вырезать товар". Editor (mask brush, undo/redo, zoom) untouched.

## Claim ledger
| Claim | Evidence |
|---|---|
| Auto-open of session photo | `UNVERIFIED` — code path `CutoutViewModel.openSessionPhoto`; no device/build run |
| No job IDs in UI | `UNVERIFIED` — grep `terminalJobId`/`jobId` in `CutoutScreen.kt` shows only the report target |
| Refresh button removed | `UNVERIFIED` — `cutout_refresh_wallet` no longer referenced in `CutoutScreen.kt` (string left defined) |
| Editor tests still compile | `UNVERIFIED` — `CutoutScreen` signature unchanged, `CutoutEditorChromeTest` not edited |

Limit: the newest import is treated as "the session photo"; `PlayKilnApp` calls `CutoutRoute()` with no argument and was not editable here.

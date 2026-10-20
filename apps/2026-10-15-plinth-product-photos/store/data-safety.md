# Data safety answers — Plinth: Product Photos

These answers describe the app's server-based photo and listing-copy operations, its wallet, and the
bundled Play Billing Library. Confirm the final provider route and Play Console classifications before
submission.

## Does the app collect user data?

- [ ] No
- [x] Yes — user-started photo processing, listing text, purchase handling, and random app identifiers
  leave the device as described below.

## Data types (check all that apply)

| Category | Collected? | Shared? | Purpose | Ephemeral? |
|----------|------------|---------|---------|------------|
| App activity | No — no app-activity analytics are used | No | — | — |
| Location | No — not read; GPS metadata is removed from exports | No | — | — |
| Personal info | User-supplied listing text could contain personal details; Plinth does not request them | Yes — text is sent to the text-generation provider when copy is requested | Generate listing copy | Not retained after the request |
| Financial info | Yes — purchase history, product IDs, and entitlement state handled by Play Billing | No third-party sharing by Plinth; Google Play processes the purchase | Credit and unlock fulfillment | Purchase records retained as needed for service and legal obligations |
| Photos and videos | Yes — photos the user submits, reduced to at most 2048 px on the longest side | Yes — sent to fal through Plinth's server for the user's requested processing | Cut-out, recognition, image generation | No — server uploads deleted within 1 hour, results within 24 hours; fal may retain inputs and outputs for 30 days under its own terms |
| Files and docs | No separate file/document category; submitted images are listed above | No | — | — |
| Device or other IDs | Yes — random installation and wallet identifiers, not hardware IDs | No third-party sharing by Plinth; sent to Plinth's server | Wallet and app-data operations | Until the user requests deletion |
| Diagnostics / crash | No crash-reporting service is used | No | — | — |
| Other user-generated content | Yes — listing text and optional report notes | Yes — listing text goes to a text-generation provider; report notes go to Plinth's server | Generate listing copy; report generated results | Listing text is not retained after the request; report notes are retained until deletion is requested |

## Third-party SDKs

Play Billing is bundled and handles purchase information; its Data safety declaration must include
Financial info. The fal processing provider is called server-side and receives user-selected media.
No analytics, crash-reporting, or advertising SDK is used.

1. Google Play Billing Library — purchase history and purchase token handling.

## Security practices

- [x] Data encrypted in transit (HTTPS API is specified).
- [x] Users can request deletion in-app through Settings → Delete my data, which prepares an email request.
- [x] No user account is required; deletion requests cover the random installation and wallet identifiers and their associated Plinth-held data.

## Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Connect to the HTTPS API for user-started photo processing and listing generation. |
| `CAMERA` | The shoot coach takes the seller's own photo on request (task 6.4). |
| `POST_NOTIFICATIONS` | Deliver the batch-done reminder (task 6.6), opt-in from Settings. |
| `ACCESS_NETWORK_STATE` | Apply the WorkManager Wi-Fi-only upload constraint. |

## Agent note

The provider's 30-day retention is separate from Plinth's shorter server retention. Final provider
route/configuration and Play Console sharing classifications must be checked against the release
build before submission.

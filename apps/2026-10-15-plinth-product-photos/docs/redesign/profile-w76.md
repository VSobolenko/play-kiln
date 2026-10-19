# Profile tab content (W76)

## Contract

`com.gexetr.plinthproductphotos.ui.profile.ProfileRoute(onOpenWallet, onOpenLibrary, modifier)` is
the W77 entry point. It renders inside the host's existing content area and does not create a
navigation shell or change the selected tab. The host supplies the two destinations: the existing
wallet purchase flow and the local library.

The profile shows a balance only when the session cache or wallet endpoint provides one. A response
from `GET /wallet` is labelled current; a cached value shown after a refresh failure is labelled as
the last saved balance; absent data stays unavailable rather than becoming zero. Purchases, recovery,
and Google Play billing remain in `WalletRoute`.

Recent results come only from existing, readable output files saved in the local project database.
The list contains project names, dates, and image previews, and opens the library rather than exposing
database or job identifiers. Reference-only outputs are excluded. The quick view checks the 30 most
recently updated projects, filters to regular files that the app can read, sorts outputs by their
saved-output timestamp, and retains at most five; its empty state names the 30-project scan limit and
links to the full library. The screen does not fabricate server jobs and explicitly says that full
server job history is not available. Failures to load the wallet or local results have visible
retry/error states.

## Data and visual boundary

- `ProfileViewModel` refreshes the wallet balance and scans one page of the 30 most recently updated
  local projects. It retains up to five non-reference outputs only when each path names a readable
  regular file. Its history state contains a project title, image path, and output timestamp—not
  database or job IDs.
- `ProfileScreen` is content-only, with a credits card, a route into the existing wallet, and a
  recent-results list or empty state. All new user-facing copy is localized in the English and
  Russian `strings_profile_w76.xml` resources.
- `ProfileRoute` owns the lifecycle refresh and exposes callbacks only. `PlayKilnApp.kt`,
  `PlinthNavBar`, Studio, AutoCard, and Cutout are not part of this change.
- Wallet transactions and purchases are not duplicated in Profile; the existing wallet remains the
  single purchase implementation.

## Decision and evidence

Use the local output table for the preview history and the existing wallet route for top-ups. Do not
add a server history request or a second purchase surface: `ApiClient` exposes wallet retrieval and
job polling by a supplied job ID, while saved outputs already have a local project-scoped query.

| Claim | Label | Evidence |
|---|---|---|
| The balance endpoint is `GET /wallet`. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/data/api/ApiClient.kt:49-55` |
| Profile labels a returned balance current, cached data stale, and missing data unavailable rather than zero; top-up is a host callback into the existing wallet. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/profile/ProfileViewModel.kt:54-65, 109-148`; `app/src/main/java/com/gexetr/plinthproductphotos/ui/profile/ProfileScreen.kt:145-179` |
| Job polling requires an explicit job ID; the inspected client has no list-history method. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/data/api/ApiClient.kt:118-124` |
| Local projects are returned in most-recently-updated order; project outputs exclude reference rows. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/data/db/Daos.kt:102-111, 252-262` |
| Profile scans 30 projects, filters to readable files, sorts by output timestamp, and exposes at most five results without IDs. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/profile/ProfileViewModel.kt:41-45, 67-89, 178-211, 235-236` |
| The route leaves wallet and library navigation to its caller. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/profile/ProfileRoute.kt:15-40` |
| Generated cards are persisted in the local output table and update their project preview/date. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/data/project/ProjectOutputRepository.kt:40-53` |
| UI must not expose job UUIDs or developer IDs, and Profile must not wire the bottom navigation. | ON RECORD | W76 assignment in this task |

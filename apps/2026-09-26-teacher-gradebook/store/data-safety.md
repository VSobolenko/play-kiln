# Data safety answers — Mark Book for Teachers

Filled in from the **merged release manifest** and the app's own source, not from the draft. Every
claim below was checked against something on disk; the source of each is named so it can be
re-checked.

## Does the app collect or share user data?

**No user data is collected or shared by the app.** The app has no account, no analytics, no crash
reporter and no server. What the teacher records stays in app-private storage on the device and is
sent nowhere.

The one thing that reaches Google is purchase traffic from the Play Billing library, without which
the app could not sell anything. That is a Google SDK behaviour and is declared under **Financial
info → Purchase history**, exactly as Play expects for a billing-only app.

## Data types

This is the table the Play Console form is filled from. The **Collected** cell reads exactly `Yes`
or `No`, because `scripts/verify-slot.mjs` reads this file back and compares it with the app's
declared capabilities — a hand-written answer the gate cannot parse is indistinguishable from a
missing one.

| Data type | Collected | Shared | Purpose |
|---|---|---|---|
| Financial info | Yes | No | App functionality |

That row, in full:

- **Financial info → Purchase history.** Collected by the Google Play Billing SDK, not by the app's
  own code. Its purpose is app functionality: which product the user bought and whether the
  entitlement is active. It is not shared by the developer with anyone, and payment instruments never
  reach the app — Google Play handles them. `store/listing.json` declares the same type.

Nothing else is collected or shared. The categories the form asks about, and the answer for each:

| Category | Collected | Shared | Why not |
|---|---|:---:|---|
| App activity | No | No | No analytics SDK is present. |
| Personal info | No | No | Student names and marks are written to app-private storage on the device and are never transmitted to the developer. The app has no server to receive them. |
| Location | No | No | No location permission is declared in the merged manifest. |
| Photos and videos | No | No | No camera or photo permission, and no image picker. |
| Files and docs | No | No | Backup and export files are written where the teacher chooses through the system picker. That is the teacher's own action, not collection by the app. |
| Device or other IDs | No | No | The app reads no advertising or hardware identifier. |
| App info and performance | No | No | No crash-reporting or diagnostics SDK. `com.google.android.datatransport` uploads billing events, not diagnostics. |
| Health and fitness | No | No | Not applicable to a grade book. |
| Messages | No | No | The app sends no messages. |
| Audio | No | No | No audio is recorded or read. |
| Contacts | No | No | Parent contacts are typed in by the teacher and stored on the device; the device's contact list is never read. |
| Calendar | No | No | No calendar permission; follow-up dates are internal to the app. |
| Web browsing | No | No | The app contains no web view and no browser. |


## Security practices

- **Data encrypted in transit:** Yes for everything the app itself sends — which is nothing. The
  billing traffic is HTTPS. `res/xml/network_security_config.xml` sets
  `cleartextTrafficPermitted="false"`, so no unencrypted request can succeed from this app.
- **Users can request deletion:** Not applicable — there are no accounts and the developer holds no
  data. Deletion is immediate and entirely in the app: delete a class and everything inside it, or
  clear the app's storage, or uninstall.
- **Account deletion path:** Not applicable — the app has no accounts, so Play's account-deletion
  requirement does not apply. `hasAccountDeletion: false` in `store/listing.json`.

## Third-party SDKs

Every SDK in the release build that touches data. Read from the release merged manifest and the
dependency graph, not from memory:

1. **Google Play Billing Library 9.1.0** — `com.android.billingclient:billing`. Handles the
   purchase. Declares `com.android.vending.BILLING`.
   <https://developer.android.com/google/play/billing>
2. **`com.google.android.datatransport` 3.1.8** (`transport-runtime`, `transport-backend-cct`) — a
   transitive dependency of the billing library, not a choice of this app. It is what adds
   `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE` and the
   `TransportBackendDiscovery` service to the merged manifest. Google uses it to upload billing
   events to Google. <https://developer.android.com/google/play/billing>

No advertising SDK, no analytics SDK and no crash-reporting SDK is present. This was verified by
reading the merged release manifest at
`app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml` —
four permissions and two services, all accounted for above.

## Permissions the app declares

Read from the merged release manifest — not from `app/src/main/AndroidManifest.xml`, which declares
none of its own:

| Permission | Source | Assessment |
|---|---|---|
| `com.android.vending.BILLING` | billing library | Required to sell the unlock. Normal, auto-granted. |
| `android.permission.INTERNET` | `com.google.android.datatransport` | Not used by the app's own code; needed by billing's network dependency. |
| `android.permission.ACCESS_NETWORK_STATE` | `com.google.android.datatransport` | Same. |
| `com.gexetr.teachergradebook.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX | Build-generated; not user-facing. |

No dangerous permission is declared. Because a Google SDK declares `INTERNET`, this form's
purchase-history row is required: answering "no collection at all" would under-declare the billing
SDK.

## Children's data / target audience

The app is a teacher's tool and is **not directed at children**; the Play content rating
questionnaire and target-audience selection must match that. A teacher may record pupils' names and
marks, so the listing must not imply the app is for pupils to use. `store/review-notes.md` states
the same for the reviewer.

## Agent note

If the billing library ever drops `transport-backend-cct`, the `INTERNET` permission disappears with
it, and both this file and `store/privacy.md` must be updated in the same change — the claim and the
manifest are one fact, not two.

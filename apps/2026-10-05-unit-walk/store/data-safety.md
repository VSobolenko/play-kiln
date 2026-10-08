# Data safety answers — UnitWalk

This file is the authored source for the Play Console **Data safety** form. It must match
`store/privacy.md`, the shipped `AndroidManifest.xml` and every bundled SDK.

## The console's first question, and the exact answer to give it

The console asks one binary question first: **`Сбор и передача данных`** (*Does your app collect or
share any of the required user data types?*). **The answer is `Да`** — because of the one bundled
SDK, not because of the app's own code. Do not transcribe the heading below as this answer.

## Does the app's own code collect user data?

**No.** UnitWalk works offline and has no account, no sign-in and no
vendor cloud. The units, rooms, walk photos, notes and exported PDFs stay in the app's private
storage on the device, and the app's own code never reads or writes the network. Play defines
"collect" as transmitting data off the device, so the walk itself declares none of it.

## The one bundled SDK, and why the form still has one row

UnitWalk ships exactly one SDK that talks to the network: **Google Play Billing**, which sells the
one-time unlock. That SDK transmits, so the binary question above is `Да` even though the app's own
code declares nothing.

The Play Billing dependency may contribute `INTERNET` / `ACCESS_NETWORK_STATE` permissions and an
internal datatransport service to the **merged** manifest of the built app, because it must reach
Google Play to complete the one-time unlock.

Consequence for this form: the Payment section carries **Financial info → Purchase history**
(app functionality; not shared with third parties by us). The app receives only the product ID and
the entitlement state; Google Play handles payment and never shares payment instruments with the
app. This row is **measured from the merged manifest of the built artifact**, not asserted here —
re-check it against the release AAB.

## Data types

| Category | Collected? | Shared? | Purpose | Notes |
|---|---|---|---|---|
| Financial info → Purchase history | Yes, by the bundled Play Billing SDK | No | App functionality | The product ID bought and its entitlement state. Google Play processes the payment. |
| App activity, Location, Personal info, Photos / videos, Files / docs, Device IDs, Diagnostics | No | No | — | Recorded and kept only in the app's private storage on the device; never transmitted. |

Photos taken with the camera and items picked with the system Photo Picker stay on the device.
When the user exports a report, the user chooses the destination through Android's own share sheet
or file picker — that is the user's own action, not collection by the app.

## Permissions declared by the app

| Permission | Why it is declared |
|---|---|
| `CAMERA` | The app's only own runtime permission. Used only while the user takes a walk photo. Photos are written to the app's private storage. No background access. |
| No storage permission | The report leaves the app through a `content://` URI (`ACTION_SEND` / `ACTION_CREATE_DOCUMENT` via `FileProvider`), which needs no storage permission on modern Android. |
| No `POST_NOTIFICATIONS` | The core walk has no reminder surface. |
| No location, contacts, audio or account permissions | The job does not need them. |

Gallery imports use the Android Photo Picker and need no runtime permission at all. The app declares
`android.hardware.camera.any` with `required="false"`, so tablets without a camera are still offered
and can import photos from the gallery.

## Third-party SDKs

| SDK | Version | Network | Public policy |
|---|---|---|---|
| Google Play Billing Library | 9.1.0 | Yes — reaches Google Play for the one-time unlock | <https://developer.android.com/google/play/billing> |

No ads SDK, no analytics SDK, no crash reporter and no AI provider SDK is bundled. `capabilities.json`
enables `billing` and only `billing`; ads are deliberately off.

## Security practices

- **Data encrypted in transit?** The only traffic is Play Billing's own connection to Google Play,
  which is encrypted by the SDK. The app itself transmits nothing, so there is no app traffic to
  encrypt.
- **Users can request deletion?** No account exists, so there is nothing to request from us.
  Deleting a walk or a unit inside the app, or clearing the app's storage in Android settings,
  removes the data — nothing was ever sent to us or to any service we run.
- **Account deletion path?** Not applicable; the app has no accounts.
- **Backup:** the app's backup rules separate cloud backup from device transfer and exclude
  secret-bearing paths (`res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml`). The cloud-backup and device-transfer policies both cover the whole `file` domain, so the walk document, the photos and the exported PDFs are part of the owner's own Google Backup; Play's definition of "collect" is transmission off the device by the developer, and the developer receives nothing.

## Agent note

Re-run the Play policy auditor (`play-auditor`) against the release AAB before submission, and
confirm the merged manifest's permission list against the table above — the Billing contribution is
the one row that can only be read from the built artifact.

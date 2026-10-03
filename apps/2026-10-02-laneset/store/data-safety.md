# Play Console data safety answers — Laneset: Swim Set Builder

Complete before Play Console submission. Must match the privacy policy and actual
code/SDK behavior. Even apps that collect **no** user data must file the form.

## Does the app collect user data?

**Yes — one data type, transmitted by the bundled Google Play Billing Library.**

"Collect" in Data safety means transmitting data off the device. Two halves, and both matter:

- **The app's own code collects and sends nothing.** Every set, the two pace-trial times and every
  setting stay in the app's private on-device storage. The app has no account, no analytics, no ads
  and no server of its own, and it computes a full session with no network at all.
- **The Google Play Billing Library does transmit**, and a bundled SDK that transmits must be
  declared. It sends the purchase it is completing to Google Play so the one-time unlock can be
  completed and restored. The developer receives no copy.

## Data types (check all that apply)

| Category | Collected? | Shared? | Purpose | Ephemeral? |
|----------|------------|---------|---------|------------|
| App activity | No | No | — | — |
| Location | No | No | — | — |
| Personal info | No | No | — | — |
| Financial info | Yes | No | App functionality | No |
| Photos / videos | No | No | — | — |
| Files / docs | No | No | — | — |
| Device IDs | No | No | — | — |
| Diagnostics / crash | No | No | — | — |

**Financial info — purchase history.** The product ID the user bought and its entitlement state,
transmitted by the Google Play Billing Library to Google Play so the one-time unlock can be
completed and restored. Payment instruments are handled entirely by Google Play and never reach the
app. The developer has no server and receives no copy of the transaction. Not shared, not
ephemeral. `store/listing.json`'s `dataSafety` block carries this same row, purpose
`App functionality`.

## Manifest permissions, as the shipped artifact declares them

The slot's own manifest (`app/src/main/AndroidManifest.xml`) declares one permission:

- `com.android.vending.BILLING` — the Play Billing flow the one-time unlock is collected through.

The Google Play Billing Library merges two network permissions into the packaged manifest. This was
read from the signed **release** build rather than a debug variant:
`app/build/intermediates/packaged_manifests/release/processReleaseManifestForPackage/AndroidManifest.xml`.

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

The release manifest's full observed permission list is four entries: the two above, the app's own
`com.android.vending.BILLING`, and `com.gexetr.laneset.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` —
a signature-level permission AndroidX generates so its own dynamic receivers are not exported. It is
added by the toolchain rather than by app code and never appears as a runtime prompt.

Both are used only by Play Billing to reach Google Play for the purchase; no code under
`app/src/main/java` opens a socket. No storage, location, camera, contacts, microphone or
notification permission is declared or requested. This is the deviation from a "no data collected"
answer that the form is about — the app is offline by construction, and Google Play Billing is the
one bundled SDK that is not.

## Third-party SDKs

1. Google Play Billing Library 9.1.0 — transmits purchase data to Google Play to complete and
   restore the one-time unlock; <https://developer.android.com/google/play/billing>

## Security practices

- [x] Data encrypted in transit (HTTPS) — the Play Billing call to Google Play is over TLS, and
      `res/xml/network_security_config.xml` sets `cleartextTrafficPermitted="false"` for the app
- [ ] Users can request deletion (if accounts exist) — not applicable, the app has no accounts
- [x] Account deletion path documented — the app has no accounts, so no deletion mechanism is
      required; local data is removed by clearing app storage or uninstalling

## Agent note

Run `.agents/skills/android-official/play-play-policy-insights/` before marking ready.

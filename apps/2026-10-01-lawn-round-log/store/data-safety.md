# Data safety answers — Lawn Round Log

The console form is `Контент приложения → Безопасность данных`. Every answer below comes from the
slot's real SDK surface — `capabilities.json` enables exactly one capability, `billing`, whose own
`capability.json` declares the one row stated here — and from the **merged release manifest** of the
built artifact, which is what settles the permission surface.

**The definition in play:** Play's "collect" means **transmitting data off the device**. The round,
the visits, the payments and the settings never leave the phone, so none of them is collected. The
purchase record does leave the app: Google Play Billing hands it to Google Play. That is why the form
is not "collects nothing", and under-declaring it is the defect this document exists to prevent.

## The six form sections

| Console field | Answer | Why |
|---|---|---|
| `Сбор и передача данных` (`collectsOrShares`) | **Yes** | The bundled Google Play Billing Library transmits the purchase record (Financial info → Purchase history). Everything the operator types stays on the device and is not collected. |
| `Шифрование при передаче` (`encryptedInTransit`) | **Yes** | The only data that leaves the app is the Play Billing purchase record, and it is transmitted over HTTPS to Google Play. |
| `Аккаунты` (`accountCreation`) | **No** | The app creates no account and has no sign-in. |
| `Удаление данных` (`dataDeletion`) | **No mechanism is required** | There is no app-side account to delete. All stored records are editable and deletable in the app, and uninstalling removes the local data. The purchase record belongs to the user's Google account and is managed by Google Play. |
| `Категории данных` (`dataCategories`) | **Financial info → Purchase history, not shared, required, App functionality, not ephemeral** | See the row below. No other category is declared. |
| `Эти данные обрабатываются временно?` (`temporaryProcessing`) | **No** | The entitlement is kept on the device so the unlock survives a reinstall; it is not transient. |

## Data categories — exactly one row

| Category | Subtype | Collected | Shared | Required or optional | Purpose |
|---|---|---|---|---|---|
| Financial info | Purchase history | Yes | No | Required (you cannot complete the purchase without it) | App functionality |

**What the row means.** Google Play Billing returns the product identifiers the user bought and their
entitlement state. The app stores that entitlement locally so the unlock survives a reinstall.
**Payment instruments are handled entirely by Google Play and never reach the app** — the app sees a
result, never a card number. Because the app never shares it with a third party of its own, "shared"
is No.

**Nothing else is collected.** No Personal info, no Location, no App activity, no App info and
performance, no Device or other IDs, no Messages, no Photos or videos, no Files or docs, no Calendar,
no Contacts, no Health or fitness. There is no Advertising ID use and no Analytics or Crash SDK, so
the three related declarations (Advertising ID, Ads, and the Data safety rows above) stay consistent
with an app that ships neither.

## Third-party SDKs

| SDK | Version | What it does here |
|---|---|---|
| Google Play Billing Library | 9.1.0 | Takes and restores the one-time unlock; returns the purchase record. |
| `com.google.android.datatransport` (`transport-runtime`, `transport-backend-cct`) | 3.1.8 | A transitive dependency of `billing-ktx` 9.1.0, not a choice of this slot. It adds `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE` and the `TransportBackendDiscovery` service to the merged manifest, and Google uses it to upload billing events. |

## Security practices

- Data is encrypted in transit (HTTPS). The app refuses cleartext traffic
  (`res/xml/network_security_config.xml`, `cleartextTrafficPermitted="false"`).
- No account, so no account-deletion path is required on the web or in-app.
- The app declares **no** `uses-permission` element in `app/src/main/AndroidManifest.xml`, but the
  **merged release manifest** is what ships, and it declares four: `com.android.vending.BILLING`
  (the billing library), `com.gexetr.lawnroundlog.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
  (AndroidX, build-generated), `android.permission.INTERNET` and
  `android.permission.ACCESS_NETWORK_STATE` (both from `com.google.android.datatransport`). None is a
  dangerous permission.
- Cloud backup and device transfer are both enabled and separated, so an operator's own record is not
  silently lost when he moves phones. Only credential-shaped files the app never writes are excluded.

## Evidence read for these answers

- `capabilities.json` → `{"schemaVersion": 1, "enabled": ["billing"]}`.
- `capabilities/billing/capability.json` → `dataCollected` (Financial info / Purchase history),
  `sdkDependencies` (Play Billing 9.1.0), `permissions: []`, `adsPresent: false`.
- `app/src/main/AndroidManifest.xml` → no `uses-permission` element, but that file is not what ships.
- Merged release manifest
  (`app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`) →
  four `uses-permission` elements — `com.android.vending.BILLING`, the AndroidX receiver permission,
  `android.permission.ACCESS_NETWORK_STATE`, `android.permission.INTERNET` — and the
  `TransportBackendDiscovery` service from `com.google.android.datatransport`.
- Signed release APK (`aapt2 dump permissions …/lawn-round-log-0.1.0-1-release.apk`) → the same four
  permissions, confirming the merged manifest is the one that reached the artifact.

## Agent note

Re-run `scripts/check-client-secrets.mjs --slot lawn-round-log` and the Play policy auditor before
marking the pack ready; the Data safety form is only as truthful as the SDK surface it describes.

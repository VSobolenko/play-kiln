# Privacy policy — Laneset: Swim Set Builder

This is the privacy policy for the Android app **Laneset: Swim Set Builder** (the "app"),
published on Google Play under the application ID `com.gexetr.laneset`.

- **Effective date:** 2026-10-02
- **Applies to:** the Laneset app for Android, version 0.1.0 and later.
- **Contact for privacy questions:** v.sobolenko.vs@gmail.com

## Summary

Laneset is an offline swim set builder. It keeps the sets you generate, the pace you measure in the
two time trials, and your settings on your own device, in the app's private storage. The app has no
account system and no server of its own, and it sends no data to the developer. The single
exception is the one-time unlock you can buy: that purchase is processed by Google Play through the
Google Play Billing Library, and Google receives the purchase information needed to complete and
restore it, under Google's own privacy policy.

## What the app does not do

- It does not ask you to create an account or to sign in.
- Its own code opens no network connection: every rule, the pace arithmetic and each generated
  session ship inside the app, so a full session is computed with no signal and the app works in
  airplane mode. The one exception is Google Play's own billing, described below.
- It does not include any advertising, analytics, crash-reporting or attribution SDK.
- It does not sell, rent or share your data with anyone.

## Data kept on your device

Everything you enter stays in the app's private storage on this device, in the Android DataStore
files `settings`, `pace` and `session`:

- the constraints you choose and the session Laneset generates from them;
- where you are in a session while you swim it;
- the two time-trial times you swim and the threshold pace computed from them;
- your app settings (theme, wallpaper colours and vibration feedback).

None of that is transmitted anywhere by the app. It leaves the device only when you deliberately
hand it to another app — for example using **Share** or **Print** to send the set list as text or a
workout card. In those cases the content goes only to the app or service you choose.

**Open a coach workout** works the other way round: it takes text you give it. You can type the set
yourself, or tap the paste action to read the workout currently on your clipboard into the box. The
clipboard is read only at that moment and only for that purpose, nothing is read in the background,
and the text stays on the device.

## Google Play Billing and the one-time unlock

The app's only in-app purchase is a one-time unlock, and it is handled entirely by Google Play. To
complete and restore that purchase, the Google Play Billing Library bundled with the app
communicates directly with Google Play, and Google receives the purchase information it needs for
that — such as which product you bought and whether the purchase is active. The app itself receives
only the entitlement state it needs to unlock the feature; it never sees your payment method.

Google processes that data under its own rules. See
[Google's Privacy Policy](https://policies.google.com/privacy) and the
[Google Play Billing documentation](https://developer.android.com/google/play/billing). The
developer has no server and receives no copy of the transaction.

## Permissions

The app's own manifest declares exactly one Android permission:

- `com.android.vending.BILLING` — required by Google Play Billing to sell and restore the one-time
  unlock. It is a normal permission granted by the Play Store rather than a runtime prompt, and it
  gives the app no access to your personal data by itself.

The Google Play Billing Library that carries that purchase also brings two network permissions into
the app you install:

- `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` — used only by Google
  Play Billing to reach Google Play so the purchase can be completed and restored. Laneset's own
  code opens no socket and sends nothing.

The app declares no storage, location, camera, contacts, microphone or notification permission.

## Backup

Android can back up app data to your Google account, or transfer it to a new device, when your
device's backup settings allow it. Laneset's own backup and data-transfer rules include only the
app's general files, preferences and databases, and they explicitly exclude the DataStore
directory (`files/datastore/`) where the settings, the pace and the in-progress session are kept.
Those files are therefore not copied into a Google backup. The one-time unlock is not carried by a
backup either: Google Play re-establishes it on a fresh install.

## Deleting your data

Laneset has no account, so there is nothing to delete on a server. To remove everything the app has
stored, open Android's app settings and choose **Clear storage** (also shown as **Clear data**), or
uninstall the app. Either action removes the DataStore files described above. The purchase record
for the one-time unlock is held by Google Play, not by the app, and is governed by your Google
account.

## Children

Laneset collects no personal information from anyone. Because the app has no account, no server and
no analytics, no personal information from a child is transmitted to the developer.

## Changes to this policy

If the app's data practices change, this policy is updated and the effective date above changes.
The current version is always the one published at this URL.

## Contact

Questions about this policy, or about the app's data practices, can be sent to
**v.sobolenko.vs@gmail.com**.

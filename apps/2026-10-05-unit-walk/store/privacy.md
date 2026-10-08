# Privacy policy — UnitWalk

**Last updated:** 2026-10-05

UnitWalk is an offline rental unit walk for a self-managing landlord. This policy describes
exactly what the app does with your information, in the words of the code that ships.

## In short

UnitWalk does not collect, transmit or sell your data. There is no account, no sign-in and no
vendor cloud. Everything the walk records stays in the app's private storage on your device, and
the app's own code never reads or writes the network. Nothing is sent to us or to any service we
run. Android's own backup can copy the app's data into your personal Google backup if you have it
switched on — see "Android's own backup" below.

## What the app stores on your device

- **Units and rooms** — the names you type when you set up a property and its rooms.
- **Walk photos** — the condition photos you take with the camera or pick from your gallery.
- **Room notes** — the short notes you add while you walk.
- **Reports and drafts** — the in-progress walk and the finished PDF you export.

All of it lives in UnitWalk's private app storage, which other apps cannot read. You can remove it
at any time by deleting a walk or a unit inside the app, or by clearing the app's storage in
Android's system settings (which removes everything UnitWalk has saved).

## Camera

UnitWalk asks for the **CAMERA** permission for one purpose only: taking a condition photo while
you walk a room. The photo is written straight into the app's private storage. The camera is never
used in the background, and no photo leaves the device through the app unless you export or share a report
yourself.

Picking an existing photo uses Android's system Photo Picker, which needs no runtime permission
and shows the app only the items you deliberately choose.

## Sharing and exporting

When you export the report, UnitWalk writes a PDF into its private storage and hands it to
Android's own share sheet or file picker through a content URI. Choosing the destination is your
action, and UnitWalk itself uploads nothing: the app has no servers and sends your data nowhere.
Two copies of your data can leave the phone, and both are named here — the one you choose to share
or save, and the one Android's own backup may make (see below). UnitWalk needs no storage permission
for this.

## Android's own backup

Android, not UnitWalk, can copy an app's data to your personal Google Backup when you have
backup switched on for the device. UnitWalk's backup rules therefore cover the walk document,
the photos and the exported PDFs, so that a lost or replaced phone can restore them. That copy
lives in your own Google account under Google's privacy policy; nothing is sent to us, and we
cannot read it. You can turn it off for this app in Android's settings, and clearing the app's
storage removes the on-device copy.

## The one bundled SDK: Google Play Billing

UnitWalk sells its one-time unlock through **Google Play Billing**. Payment is handled entirely by
Google Play: the app never receives your card, bank details or any payment instrument. What the app
receives is the product ID you bought and whether the entitlement is active, so it can unlock the
feature.

The Play Billing dependency may contribute `INTERNET` / `ACCESS_NETWORK_STATE` permissions and an
internal datatransport service to the **merged** manifest of the built app, because it must reach
Google Play to complete the one-time unlock. Google Play's handling of your purchase is governed by
Google's own privacy policy: <https://policies.google.com/privacy>. The third-party SDK is listed
in full in `store/data-safety.md`.

## Children

UnitWalk is a tool for landlords managing rental property. It is not directed at children and does
not knowingly collect information from them.

## Changes

If the app's data practices change, this policy is updated before the release that changes them,
and the date at the top changes with it.

## Contact

Questions about this policy: **v.sobolenko.vs@gmail.com**.

# Privacy policy — Mark Book for Teachers

**Last updated:** 2026-09-26

Mark Book for Teachers is a private record book for a teacher's own classes. It has no account, no
login, no server of its own and no analytics. This policy states exactly what the app stores, where
it goes, and the two network permissions it inherits from Google Play's billing library — which are
disclosed here rather than left for a reader to discover in the manifest.

## What the app stores, and where

Everything the teacher records is written to **this device only**, in the app's private storage:

| Stored | Where |
|---|---|
| Classes, students, grades, attendance, notes, parent contacts, seating plans | `files/recordbook.json` in the app's private storage |
| Whether the one-time unlock was purchased | `sharedpref/billing_state.xml` in the app's private storage |

App-private storage is not readable by other apps on the device. The developer operates no server
that receives anything from this app, has no way to identify a user, and cannot see a teacher's
record.

## Network access: what exists, and what does not

The app's own code makes **no network requests**. There is no HTTP client, no analytics endpoint,
no crash reporter and no server address anywhere in it, and the app ships a network security
configuration that forbids unencrypted traffic.

The app nevertheless declares two network permissions, and this is the honest part of the policy:
`android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` are added by
`com.google.android.datatransport`, a library that **Google Play's billing library** depends on and
brings in by itself. Google uses it to talk to Google Play for purchases and to upload
purchase-related events on Google's own schedule. Those requests are between the device, Google
Play and Google, are governed by
[Google's privacy policy](https://policies.google.com/privacy), and carry no part of the teacher's
record.

The app is fully usable offline, including the unlock once it has been purchased.

## Where the teacher's record can leave the device

There are exactly two paths, and the teacher starts both:

1. **A backup or export the teacher makes.** The teacher can write a backup file (`.json`) or an
   export (`.csv`, `.pdf`) to a location they choose through the system file picker — for example
   their own Downloads folder or their own cloud storage. Where that file goes is the teacher's
   decision, and it is then governed by that storage provider's policy, not this one.
2. **Android Auto Backup.** Android can copy app data to the teacher's own Google Drive so that a
   lost or replaced phone does not mean a lost term of marks. The app allows exactly two items to
   be backed up: `recordbook.json` (the teacher's record) and the purchase state. If the teacher
   has turned off Android's backup for this app or for the device, no copy is made. These copies
   live in the teacher's own Google account, are not sent to the developer, and are removed when the
   teacher deletes the app's backup data from that account.

The app contains **no advertising SDK, no analytics SDK and no crash-reporting SDK**.

## Purchases

The one-time unlock is sold by Google Play. Google processes the payment; the developer never
receives card details, an address or a name. The app receives only whether the purchase exists, and
stores that on the device so the unlock works offline.

## Children's data

The app is a tool for teachers. It is **not directed at children**, and children are not expected to
install or use it. A teacher may record their pupils' names and marks as part of their school's own
record-keeping. That record stays on the teacher's device as described above. The developer neither
receives it nor has any means to access it. A school or teacher considering this app should satisfy
themselves that holding pupil records on a personal device meets their own institution's rules.

## Deleting information

The teacher has full control and no request to the developer is needed:

- Delete any class, student, mark, note, contact or seating position in the app; deleting a class
  takes everything recorded inside it with it.
- Delete all data at once by uninstalling the app, or by clearing the app's storage in Android
  settings. A backup copy in the teacher's own Google Drive can be removed from that account.
- There is no account, so there is nothing for the developer to delete on the teacher's behalf.

## Permissions

The app declares four entries in its merged manifest:

| Permission | Why it is there |
|---|---|
| `com.android.vending.BILLING` | Required by Google Play's billing library to sell the one-time unlock. Normal permission, granted automatically. |
| `android.permission.INTERNET` | Added by the billing library's own dependency (`com.google.android.datatransport`); used for purchase traffic to Google Play. |
| `android.permission.ACCESS_NETWORK_STATE` | From the same dependency; lets that library see whether a network is available before retrying. |
| `com.gexetr.teachergradebook.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Added automatically by AndroidX; not a real permission and not requestable by apps. |

No storage, location, camera, contacts or microphone permission is requested. Reading and writing
the teacher's chosen backup or export file uses the system document picker, which grants access to
one file at a time and needs no permission.

## Changes

If this policy changes, the date above changes with it, and the app's Play listing and Data safety
declarations are updated at the same time.

## Contact

Questions about this policy: **v.sobolenko.vs@gmail.com**

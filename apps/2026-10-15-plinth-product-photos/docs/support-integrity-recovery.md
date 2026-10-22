# Support runbook — "Connect your wallet" / wallet unavailable on a real install

Written 2026-10-21 after a diagnosis that took a full session. Read this before touching
the server: the failure looks like a backend outage and is almost never one.

## The symptom

A user installs from Google Play, opens the app, and sees the connect/verification gate
instead of a wallet with the 10 starter credits. Every paid operation is unavailable.
The device may otherwise be healthy, and the same account on another phone works.

## What the server says, and how to read it

The gate is Play Integrity, verified in `server/src/integrity.ts`. A rejected session logs
one line with the exact claim that failed — added 2026-10-21 because three rejections were
logged without it and cost a diagnosis cycle:

```bash
journalctl -u plinth-product-photos.service --since "-1 h" --no-pager | grep "integrity claim rejected"
```

The important shape is `INTEGRITY_CERTIFICATE_MISMATCH`. Read the two digests in it:

| Field | Meaning |
|---|---|
| `observed=[…]` | the signing certificate **Google Play told the device** the package has |
| `expected=75A440B9…` | the production certificate (`PLAY_SIGNING_CERT_SHA256`) — Google Play App Signing's key |
| `recognized=UNRECOGNIZED_VERSION` | Play does not recognise the installed package as a version it serves (Google's documented meaning) |
| `licensed=UNEVALUATED` | Play does not know the installed version, so licensing cannot be judged |

If `observed` is `33763AB7…`, that is **our upload key** — the key every sideloaded APK
(`adb install`, local debug/release builds, internal app sharing) is signed with, not the key
Play re-signs production installs with. A device reporting it has Play-side state left over
from a build that did not come from the production track.

## The one measured cause

Verified on 2026-10-21 on a Galaxy A40 (`R58MB3SGZVL`) that had been used all day to install
`adb` builds of this app:

- installed `base.apk` verified with `apksigner --print-certs` → `75a440b9…` (correct key);
- Play Integrity still reported `33763AB7…` and `UNRECOGNIZED_VERSION`;
- **only** Play Store's data was cleared (`pm clear com.android.vending`), nothing else changed —
  no reinstall, no app-data wipe;
- the next session attempt returned 200, `Device Recall: RECALL_CLAIMED`, wallet `balance=10`,
  `ledger STARTER +10`, and the app showed `Credits · 10`.

A Galaxy A16 with no sideload history, installing the same version from Play, worked on the
first launch without any intervention. So the cause is **per-device Play state**, not the
build, not the server, and not the Android version (the A40 runs 11, the A16 runs 16).

## Fix for a user

In order; stop at the first that works. None of them deletes the user's projects — the app's
data is untouched by steps 1 and 2.

1. **Clear Google Play Store's data.** Settings → Apps → Google Play Store → Storage →
   *Clear data* (or *Clear cache*, then *Clear all data*). Reopen Play Store once, then open
   the app. This is the step that was measured to work.
2. **Reinstall from Play** (uninstall, then install from the store page). Try this only if
   step 1 does not help; it also drops the app's local session, which is re-created on launch.
3. If neither helps, ask for the Play Integrity line from the server log (the
   `integrity claim rejected` entry for that attempt) before trying anything else. The line
   names which claim failed, and a different claim has a different cause.

## What not to do

- **Do not relax the certificate check** to make a report go away. It is the only thing that
  stops a repackaged APK from drawing paid credits; every relaxation is a free-credit hole.
- **Do not ask the user to factory-reset.** The measured fix is one app's data, not the device.
- **Do not treat it as a server outage.** If the server were down the app would fail differently
  (an unreachable-server state, `503 INTEGRITY_UNAVAILABLE`, or a network error) and *every*
  user would be affected at once, which is not what this symptom looks like.

## Why a normal user is not affected

`MEASURED` 2026-10-21 on production: every session rejection that day (24 POSTs) either
carried `INTEGRITY_CERTIFICATE_MISMATCH` from the test A40 or predated the diagnostic logging;
none of them created a device or wallet row. A clean Play install obtained
`PLAY_RECOGNIZED` on both test phones. `server.sqlite` held four device rows at that point,
all of them the factory's own phones.

`UNVERIFIED`: how often this state occurs in the wild. It requires a prior non-Play install of
this package on the same device, and the factory cannot observe what a user did before
installing.

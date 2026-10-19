# W35 device pass - blocked before interaction

## Outcome

The full functional journey was **not completed**. The required phone identity
check succeeded (`R58MB3SGZVL` reports `SM-A405FM`), and the debug APK was
hashed locally. The recorded build digest matched before this report was added.
However, the host denied the
`adb install` command and a later read-only screenshot/hierarchy command with
“Permission denied because no interactive user response was available.” No
install, tap, UI hierarchy dump, or screenshot was performed by this W35 run.
There was no device defect to reproduce or fix.

The existing `device-verification.json` is left unchanged: it records an earlier
partial journey at `2026-10-17T01:30:00+03:00`, but cannot stand in for this
task's full journey. It refers to screenshots under `.kiln/scratch/device-plinth/`;
those files were not present in the local scratch tree when checked.
Adding this report changed the slot's `releaseSourceDigest` (the digest
collector includes this document), so the existing record no longer matches.
The final verification consequently reports `slot.device` FAIL; changing the
record to the new digest without repeating the phone journey would be
misrepresenting the tested revision.

## Build and artifact measurements

| Check | Result |
| --- | --- |
| Required phone model | `SM-A405FM`, measured with `adb -s R58MB3SGZVL shell getprop ro.product.model` |
| Debug build before this report was added | `node scripts/verify.mjs --slot plinth-product-photos --build`: `124 PASS, 2 UNVERIFIED, 4 SKIP`; `assembleDebug` passed and the pre-existing device digest matched |
| Final debug build verification | `assembleDebug` passed; `123 PASS, 2 UNVERIFIED, 4 SKIP, 1 FAIL`; `slot.device` FAILs because this report changed the source digest after the recorded run |
| Pack rows | The only `UNVERIFIED` rows were `pack.inventory` and `pack.media`, because `store/assets` is absent |
| Slot tests | `node scripts/run-slot-tests.mjs --slot plinth-product-photos --rerun`: 274 passed, 0 failures/errors/skips |
| Current debug APK SHA-256 | `e1edbe5fac2727fb9e101e4afd22f303c9ca9f9c708916f0b00ef672b95f035a` |
| Recorded build digest before this report | `a9cb7f6dbedfbd0441d658aceb7ac6654172206c6291ccc951f02a771c448254`, from `releaseSourceDigest`; adding this report makes it stale |
| Installed `base.apk` | Not pulled or independently hashed during this run; the existing record claims the same hash |

## Journey table

“Previously recorded” means it appears in the pre-existing device record, not
that W35 re-executed or independently observed it. The tap centers below are
likewise taken only from that record.

| Required surface/action | W35 action and tap point | Observation | Screenshot |
| --- | --- | --- | --- |
| Studio home: hero, scene strip, compact bottom nav, balance | Not reached; no tap | Previously recorded: “10 credits”, “Choose a scene”, Top/Bottoms cards, Studio/Library/More destinations. Hero composition and compact single-line nav were not established here. | Prior record references `.kiln/scratch/device-plinth/03-home.png`; file unavailable |
| Balance chip to Wallet without selecting Library | Not tapped; prior record gives `(906,182)` | Previously recorded Wallet/credits surface and Library not selected. Not rechecked. | Prior record references `.kiln/scratch/device-plinth/04-wallet.png`; file unavailable |
| More: Credits/Wallet, Settings, wallet not primary Settings door | Not reached in W35 | Previously recorded More entries include Settings and credits; no evidence for the Settings wallet-door requirement. | Prior record references `06-more.png`; file unavailable |
| Settings affordances | Not reached | Theme/language navigation cues and vibration/wallpaper switches remain unverified. | None |
| Library image-led browser and empty/error behavior | Not tapped by W35; prior record gives `(457,2105)` | Previously recorded Library destination rendered; image-led loading and empty/error cases remain unverified. | Prior record references `05-library.png`; file unavailable |
| Create path: Import or Continue to AutoCard/Coach; category handoff | Not reached | No create flow or category application was observed. | None |
| Home secondary screen: Export, Batch, or Slide | Not reached | No secondary screen was opened. | None |
| Back and Studio/Library/More switching; no hang | Not exercised in W35 | Prior record reports Library then More navigation at `(457,2105)` and `(745,2105)`; back behavior and hang-free switching were not established. | Prior record references `05-library.png` and `06-more.png`; files unavailable |

## Defects and fixes

No defect was reproduced, so no product code was changed. This is a tooling
blocker, not evidence that any unvisited screen works. The install and all
remaining interactions must be rerun in a session where the device-control
permission is available; the app should then be reinstalled from the measured
debug APK and its installed `base.apk` hashed before the journey.

## Claim ledger

| Claim | Status | Evidence |
| --- | --- | --- |
| The designated serial reports model `SM-A405FM` | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM` |
| Current APK hash is `e1edbe5f...b95f035a` | MEASURED | `Get-FileHash ...\\app-debug.apk -Algorithm SHA256`; full hash above |
| The slot digest matched the stored `a9cb7f6d...1c448254` before this report was written | MEASURED | `releaseSourceDigest('apps/2026-10-15-plinth-product-photos').digest`; the report itself changes that digest |
| Current APK is the APK installed on the test phone | UNVERIFIED by this run | The attempted install was denied; no installed `base.apk` was pulled |
| Studio, Wallet, Library, and More behaved as described in the old record | RECORDED, not independently re-observed | `device-verification.json:15-17`; its screenshot paths were unavailable |
| Settings, create, home tools, and Back behavior work | UNVERIFIED | The W35 journey could not interact with the device |
| Final `slot.device` status | MEASURED FAIL | `node scripts/verify.mjs --slot plinth-product-photos --build`; the report changed the digest after the recorded device run |

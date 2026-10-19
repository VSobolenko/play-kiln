# W79b - current rewrite on the A40

Run: 2026-10-17, beginning 19:39 +03:00. This is a device evidence report, **not an overall UX pass**.
Only serial `R58MB3SGZVL` was addressed. Its model query returned `SM-A405FM`.
`https://pikupo.com/plinth-debug/health` returned HTTP 200 before and after the journey.
No emulator, local server, adb reverse, server change, purchase, data clear, or release approval was used.

## Artifact and commands

`runGradleTasks(path.resolve('apps', '2026-10-15-plinth-product-photos'), ['assembleDebug'])`
from `scripts/lib/gradle.mjs` built under the slot lock: BUILD SUCCESSFUL, 3104 ms,
39 tasks up-to-date. `adb -s R58MB3SGZVL install -r <debug APK>` returned `Success`;
force-stop and monkey launch addressed only `com.gexetr.plinthproductphotos.debug`.

Artifact: `app\build\outputs\apk\debug\app-debug.apk`.
SHA-256: `329ae950d8e015e64e376c29c9af3e7961d5acac1fba1cd34890b5cf518768b8`.
An installed-base APK pull was denied by the execution permission layer; installed-byte
equality is therefore UNVERIFIED, not inferred from install success.

All commands below use `adb -s R58MB3SGZVL`. Full timestamped command/output log:
`.kiln\scratch\copilot-plinth-w79b-device\commands.txt`.
Build log: the same directory's `assemble-debug.txt`.
Each numbered capture below has both a PNG (visually inspected) and hierarchy XML there.
Tap positions came from those hierarchies, including navigation ancestors when label bounds were zero.

## Journey results

| Action and command | Observed result | Verdict / evidence |
|---|---|---|
| Launch package; inspect Studio | Russian Studio title, selected photo of a black phone held in a hand, balance 2, Studio/Library/Create/Profile/More navigation. No Batch main tab. Initial hierarchy caught the wallet label before refresh; screenshot showed 2 credits. | MEASURED, `01-launch` |
| `shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:com.gexetr.plinthproductphotos.debug` | Installed app info says **Plinth AI: Product Photos**. Studio's own top bar says Studio, not the brand. App name was verified on-device, not merely from resources. | PASS naming, `15-device-app-name` |
| `shell input tap 747 2048` (Profile ancestor `[644,1986][850,2110]`) | Profile shows 2 credits, server balance, Open wallet and recent results. A visible result is named `a62f12d2-f9fc-43cd-bcd4-55de26fd5162.jpg`. | PASS Profile/credits; FAIL no-UUID-shaped-identifiers criterion, `02-profile`. Whether this filename equals a server job ID is UNVERIFIED. |
| `shell input tap 540 1092` | Wallet shows 2 credits, add 90 credits, and store price unavailable. No purchase attempted. | MEASURED, `03-wallet` |
| `shell input tap 127 2048`; `shell input swipe 540 1700 540 600 700` inside Studio viewport `[0,278][1080,1968]` | Tool tiles are reachable without a full-screen credit essay. Relight tile lists 5 credits, cutout 1, scene background 2. | PASS no mandatory pre-tool credit essay on this selected-photo path, `04-tools` |
| `shell input tap 285 1837` on Relight tile `[48,1707][522,1968]` | Opens generic Studio tools with retained before/after state: source phone, after-image a child with a large horned furry animal. This appeared before any Generate tap in this run. | MEASURED retained mismatch, `05-relight`; NOT a new Relight result or proof of a new server defect |
| Scroll; tap existing phone choice `324 1815`; scroll; horizontal swipe `850 1300 200 1300 700` inside carousel `[48,896][1032,1968]`; tap Relight choice `729 1833` | Relight choice appears disabled. Tap did not change selected processing: summary remains Create whole scene (not product photo). Balance is 2 against listed Relight cost 5, but the UI gives no explicit reason for the disabled choice. | BLOCKED Relight branch; cause UNVERIFIED. `06-relight-form`, `07-tool-selection`, `08-relight-card`, `09-generate` |
| Scroll `540 1700 540 1000 700` to form bottom | Empty optional instruction field, 240-character guidance, disabled Generate **2 credits**, Used credits: 2. No Generate was submitted. | PASS visible Generate cost for current whole-scene selection; new photo-faithful Relight/Recolour result UNVERIFIED, `10-generation-status` |
| `shell input tap 541 2048` (Create) | Short screen: Start with a product photo / Choose photo. No local-composition-without-AI wall. Recognition after importing a new photo was not exercised. | PASS short Create entry, `11-create` |
| Profile then recent thumbnail `216 1776` | Navigates to Library, not full-screen viewer. Library cards show phone images and UUID-shaped filenames. | MEASURED, `12-viewer` (filename is capture name, not a claim that a viewer opened) |
| Library card `285 1228`; image `540 1098` | Opens Create card with source phone, marketplace not selected, Create card button. Tapping source image leaves the same screen. No dedicated viewer reached. | `13-project`, `14-image-tap`; pinch/off-centre zoom/pan UNVERIFIED, not PASS |
| Return from App info, open Studio, retry Relight tile and reveal its processing choice | Balance refreshed to **10** without any purchase or server action by this worker. Relight choice still appears disabled. The reason is not explained; insufficient balance alone cannot explain the later state. | BLOCKED, `16-final-studio` through `19-retry-relight`; no new generation submitted |

## Verification boundary

`device-verification.json` replaces mixed historical observations with this journey only.
Its `buildDigest` is obtained by importing `releaseSourceDigest` from
`scripts/lib/release-approval.mjs` after this report is written, because slot docs are
part of that digest. This report does not authorize release.

Final requested command: `node scripts/verify.mjs --slot plinth-product-photos --build`.
Its actual output and exit status are recorded in
`.kiln\scratch\copilot-plinth-w79b-device\verify.txt` and `verify-exit.txt`.
The device-record sensor is separate from the failed/blocked UX criteria above:
a sensor PASS cannot establish those fixes work.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Target model is SM-A405FM; remote health is 200 | MEASURED | Session model command; health command before/after run |
| Current debug APK built under slot lock and installed successfully | MEASURED | `assemble-debug.txt`; `commands.txt` install Success; artifact SHA-256 above |
| Installed APK bytes equal local APK | UNVERIFIED | Pull denied by execution permissions; no byte-equivalence claim |
| Installed app has Plinth AI naming | MEASURED | `15-device-app-name.png` and XML |
| Profile replaces Batch main tab and exposes credits/wallet | MEASURED | `01-launch`, `02-profile`, `03-wallet` |
| Profile has no UUID-shaped identifiers | MEASURED - FAIL | `02-profile` visible UUID-shaped JPG filename |
| Tool selection avoids mandatory pre-tool credit essay; Generate includes cost | MEASURED | `04-tools` through `10-generation-status` |
| Create has short photo-first entry | MEASURED | `11-create` |
| New Relight/Recolour preserves this photo with empty instruction | UNVERIFIED | Disabled Relight selection at both 2 and later 10 credits; no new request submitted |
| Old preview mismatches selected phone | MEASURED | `05-relight`; persisted state, not generated in this session |
| Viewer supports pinch and off-centre pan | UNVERIFIED | `12-viewer` through `14-image-tap`; dedicated viewer not reached |
| Final objective gate status | UNVERIFIED in this authored report | Final command output in `verify.txt`; read its actual verdicts rather than assuming green |

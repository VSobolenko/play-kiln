# W91 device proof: Generate enablement

Tested at `2026-10-18T00:09:02.8945382+03:00` on `RF8Y703DR2D` (`SM-A165F`, Android 16, 1080x2340 at 450 dpi). The installed package was `com.gexetr.plinthproductphotos.debug`. Its pulled `base.apk` SHA-256 matched the local debug APK exactly: `9f1b784075b901dfbd36de66e3fd86be6555adc86fdeba4cbb44306dbd34f484`. The build-input digest was `5020d9b81fff5725cbe299ba37e424b0e710b9ae190fb825532f6d2e3d925d05`.

| Check | Measured value | PASS/FAIL | Evidence file |
|---|---|---|---|
| Device identity and artifact | `SM-A165F`; attached state `device`; installed APK SHA-256 equals local APK SHA-256 | PASS | `.kiln/scratch/device-plinth-w91/device-info.txt`, `.kiln/scratch/device-plinth-w91/installed-base.apk` |
| Photo picker selection | `com.google.android.photopicker`; tapped the clickable tile at `[0,929][358,1287]` once and confirmed; app displayed `8809.jpg` and `Selected photo - private copy ready` | PASS | `.kiln/scratch/device-plinth-w91/03-system-picker.xml`, `04-photo-selected.xml`, `05-photo-bound.png` |
| Scene before prompt | `Generate · 2 credits`; clickable parent `enabled="false"` while prompt was empty | PASS | `.kiln/scratch/device-plinth-w91/07-scene-initial.xml` |
| Scene after Latin prompt | Prompt text `tent in a forest`; clickable parent `enabled="true"` | PASS | `.kiln/scratch/device-plinth-w91/10-scene-ready.xml`, `10-scene-ready.png` |
| Scene generation | At 5 seconds the screen showed `Creating your edit...`; by 60 seconds a Before/After image appeared and the screen reported `Used 2 credits` | PASS | `.kiln/scratch/device-plinth-w91/11-scene-after-tap.png`, `11-scene-after-tap.xml`, `12-scene-after-60s.png`, `12-scene-after-60s.xml` |
| Relight enablement | Opening Relight first showed the existing Scene preview with no Generate control. After tapping `Discard generated preview`, `Generate · 5 credits` appeared; clickable parent `enabled="true"` | PASS | `.kiln/scratch/device-plinth-w91/16-relight.png`, `17-relight-bottom.xml`, `18-relight-discarded.png`, `18-relight-discarded.xml` |
| Recolour enablement | `Generate · 2 credits`; clickable parent `enabled="true"` | PASS | `.kiln/scratch/device-plinth-w91/20-recolour.png`, `20-recolour.xml` |
| Tool headers | Scene, Relight, and Recolour each displayed a single-line header containing only the tool name; no `Return to the studio` banner appeared in their dumps | PASS | `.kiln/scratch/device-plinth-w91/10-scene-ready.png`, `18-relight-discarded.png`, `20-recolour.png` and corresponding XML files |
| Catalogue card heights | Cut out item `[45,404][523,882]`; Scene `[557,404][1035,882]`; Relight `[45,927][523,1405]`; Recolour `[557,927][1035,1405]`. All four measured 478 px high | PASS | `.kiln/scratch/device-plinth-w91/15-cards-full.png`, `15-cards-full.xml` |
| Expand catalogue entry | No Expand text or card appeared in the full catalogue dump; the four cards above were present | PASS | `.kiln/scratch/device-plinth-w91/15-cards-full.xml`, `15-cards-full.png` |
| Rebuild decision | No file under slot `app/src`, `server`, `gradle`, or shared `capabilities` was newer than the APK at `2026-10-17T23:54:17.2130871+03:00`; no rebuild was run | PASS | `.kiln/scratch/device-plinth-w91/source-freshness.txt` |

## Exact Generate enabled attributes

The actionable control is the clickable outer `android.view.View`; the inner `TextView` is not clickable and reports `enabled="true"` even when the outer control is disabled. The values below are from the clickable parent nodes in the saved UI dumps:

| Tool/state | Label TextView `enabled` | Clickable parent `enabled` | Evidence |
|---|---:|---:|---|
| Scene, empty prompt | `true` | `false` | `07-scene-initial.xml` |
| Scene, `tent in a forest` entered | `true` | `true` | `10-scene-ready.xml` |
| Relight, after dismissing the prior preview | `true` | `true` | `18-relight-discarded.xml` |
| Recolour | `true` | `true` | `20-recolour.xml` |

Scene was the only tool generated during this run. The result was visible by the 60-second capture; Relight and Recolour were checked for enablement only.

## Measured claim ledger

| Claim | Label | Evidence / command and observed value |
|---|---|---|
| The authorized serial is attached and identifies as the A16 | MEASURED | `adb -s RF8Y703DR2D get-state` -> `device`; `adb -s RF8Y703DR2D shell getprop ro.product.model` -> `SM-A165F`; profile recorded in `.kiln/scratch/device-plinth-w91/device-info.txt` |
| The run target is Android 16 at 1080x2340 and 450 dpi | MEASURED | `adb -s RF8Y703DR2D shell getprop ro.build.version.release`, `adb -s RF8Y703DR2D shell wm size`, and `adb -s RF8Y703DR2D shell wm density` -> `16`, `1080x2340`, `450` |
| The installed artifact is the local debug APK | MEASURED | `adb -s RF8Y703DR2D shell pm path com.gexetr.plinthproductphotos.debug`, then `adb -s RF8Y703DR2D pull <base.apk> .kiln/scratch/device-plinth-w91/installed-base.apk`; `Get-FileHash -Algorithm SHA256` on pulled and local APK -> identical digest `9f1b784075b901dfbd36de66e3fd86be6555adc86fdeba4cbb44306dbd34f484` |
| One photo was selected from Android Photo Picker | MEASURED | `adb -s RF8Y703DR2D exec-out uiautomator dump /dev/tty` -> `.kiln/scratch/device-plinth-w91/03-system-picker.xml`; its package is `com.google.android.photopicker`, and the selected tile bounds were `[0,929][358,1287]`; the app showed `8809.jpg` in `05-photo-bound.png` |
| Scene becomes actionable with a valid Latin prompt | MEASURED | `adb -s RF8Y703DR2D exec-out uiautomator dump /dev/tty` -> `10-scene-ready.xml`; prompt is `tent in a forest`, clickable Generate parent is `enabled="true"` |
| Scene returned an image rather than remaining in progress | MEASURED | `adb -s RF8Y703DR2D exec-out screencap -p` saved to `12-scene-after-60s.png`; screenshot and `12-scene-after-60s.xml` show a Before/After result and `Used 2 credits` |
| Relight and Recolour are enabled | MEASURED | UI dumps saved as `18-relight-discarded.xml` and `20-recolour.xml`; their clickable Generate parents are each `enabled="true"` |
| The four catalogue cards have equal heights and Expand is absent | MEASURED | `adb -s RF8Y703DR2D exec-out uiautomator dump /dev/tty` -> `15-cards-full.xml`; parsed card bounds yield 478 px height each, with no `Expand` text |
| Rebuilding was unnecessary | MEASURED | `source-freshness.txt`: the APK timestamp is `2026-10-17T23:54:17.2130871+03:00`; post-APK files under `app/src`, `server`, slot `gradle`, and shared `capabilities` -> `none` |
| Build-input digest for the recorded device run | MEASURED | `node --input-type=module -e "import { releaseSourceDigest } from './scripts/lib/release-approval.mjs'; console.log(releaseSourceDigest('apps/2026-10-15-plinth-product-photos').digest)"` -> `5020d9b81fff5725cbe299ba37e424b0e710b9ae190fb825532f6d2e3d925d05` |


## W93 re-record

Same APK (sha256 `9f1b7840…34f484`, unchanged), re-driven on `RF8Y703DR2D` / SM-A165F. Captures: `.kiln/scratch/device-plinth-w93/`.
Digest before the run: `5342008e136adc50aa4ab53e52219955b3f37942a7313a609c40e693ea89fb00`. Digest after: pre-append value was identical (5342008e…); appending this section is itself a build-input change (docs/redesign is digested), so the final value is the one in `device-verification.json`, measured after this section was written.

Observed: picker -> Done bound 8809.jpg; Scene header one line "Scene", Generate disabled when empty, enabled after typing `tent in a forest`, tapped -> "Creating your edit…" then Before/After result by 60 s (generated on Scene only); Relight (after Discard) and Recolour Generate enabled, not tapped; four catalogue cards 478 px high, no Expand; no "Return to the studio" banner on any tool header.

| Claim | Evidence | Status |
|---|---|---|
| Device is SM-A165F | `adb -s RF8Y703DR2D shell getprop ro.product.model` | MEASURED |
| Digest before = 5342008e… | `releaseSourceDigest` before run | MEASURED |
| Scene Generate enabled=true after typing | `09-scene-typed.xml` | MEASURED |
| Scene result appears | `10-after-5s.xml`, `11-after-60s.xml` | MEASURED |
| Relight Generate enabled=true | `15-relight-discarded.xml` | MEASURED |
| Recolour Generate enabled=true | `17-recolour.xml` | MEASURED |
| All four cards 478 px, no Expand | `07-cards-scrolled.xml` bounds | MEASURED |
| Headers one line, tool name only | `08-scene.xml`, `15-…xml`, `17-…xml` | MEASURED |

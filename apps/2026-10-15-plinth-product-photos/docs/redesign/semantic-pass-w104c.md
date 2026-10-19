# W104c semantic paid-tool verification

Device preflight: `adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM`.

## R1 — expectation recorded before execution

Input will be the synthetic 1200×1600 two-object asset: red mug on the left, blue bottle on the right, white background. Prompt: `make the mug bright green`.

Expected: the red mug becomes clearly green; the blue bottle remains blue; the white background remains white. A changed bottle, changed background, glow, or darkening is a failure.

### R1 execution result

The device File Picker displayed the pushed fresh asset `zzw104c.png` after the required search path; its result row read `0 B, PNG image`, while `adb -s R58MB3SGZVL shell ls -l /sdcard/Download/zzw104c.png` measured 24,652 bytes. The prescribed focused-result `TAB` then `ENTER` returned to Studio, but Studio still named the prior selected input `zzcutoutprobe.png`; repeating the path and tapping the measured result-row coordinate also did not yield a new selected filename before the command timed out. Evidence capture: `.kiln/scratch/w104c/picker-blocked.png` (144,008 bytes).

Therefore the specified synthetic two-object input was not accepted by the app and no paid Recolour action was started. No before/after product image exists, no balance charge occurred, and the R1 colour counts are **UNVERIFIED** rather than invented. Verdict: **UNVERIFIED (blocked at input selection)**.

## R2 — expectation recorded before execution

Starting from an actual R1 result, prompt: `make it pure white` (or `make the mug pure white`). Expected: the primary action remains reachable without an unsolvable “save first” wall; one explicit secondary save step is acceptable if it does not prevent progress.

R2 was not run because R1 never produced the required input/result. Verdict: **UNVERIFIED (blocked by R1 input selection)**.

## L1 — expectation recorded before execution

Prompt: `warm golden sunset light coming from the left`. Expected: the product left half becomes brighter and warmer than its right half; own product hue remains unpainted; background is not replaced with a flat unrelated colour. The planned measurement is mean RGB on product left/right halves before and after.

L1 was not run because the required synthetic two-object input was not accepted. No paid action, output image, or left/right measurements exist. Verdict: **UNVERIFIED (blocked at input selection)**.

## L2 — expectation recorded before execution

Prompt: `hard studio light from above, dark background`. Expected: a visible top-down key light and a darker background.

L2 was not run because the required synthetic two-object input was not accepted. No paid action or output image exists. Verdict: **UNVERIFIED (blocked at input selection)**.

## Balance / paid-action table

| Action | Button / price observed | Balance before | Balance after | Charge | Progress banner | Auto-scroll | Verdict |
|---|---|---:|---:|---|---|---|---|
| R1 Recolour | `Recolour`, `2 credits` | 25 | not run | none | not applicable | not applicable | UNVERIFIED |
| R2 Recolour repeat | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| L1 Relight | `Relight`, `5 credits` | 25 | not run | none | not applicable | not applicable | UNVERIFIED |
| L2 Relight | `Relight`, `5 credits` | 25 | not run | none | not applicable | not applicable | UNVERIFIED |

## Gaps

The input-selection precondition failed: picker search discovered the pushed PNG, but described it as `0 B` and Studio retained the previous file after selection. This blocks the named-input behavioural contracts, so the requested visual and price/charge verdicts are not claimed.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Test target was SM-A405FM | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM` |
| Studio displayed Credits 25 before attempted testing | MEASURED | `python .kiln/scratch/w104/h.py show` initial output: `Credits · 25` |
| Recolour label and price were displayed | MEASURED | same hierarchy output: `Recolour`; `2 credits` |
| Relight label and price were displayed | MEASURED | same hierarchy output: `Relight`; `5 credits` |
| Pushed picker file existed as 24,652 bytes | MEASURED | `adb -s R58MB3SGZVL shell ls -l /sdcard/Download/zzw104c.png` |
| Picker reported that same named result as 0 B | MEASURED | `python .kiln/scratch/w104/h.py show` after search, `zzw104c.png` / `0 B, PNG image` |
| Recolour preserves only the requested product | UNVERIFIED | Requires accepted two-object input, output capture, and Pillow counts |
| Repeat recolour is not blocked by save flow | UNVERIFIED | Requires completed R1 result and R2 screen interaction |
| Relight direction/warmth and product-hue preservation | UNVERIFIED | Requires accepted input, before/after captures, and left/right RGB measurement |
| L2 creates top-down light with dark background | UNVERIFIED | Requires accepted input and output capture |

---

# W104f indexed-gallery device attempt (2026-10-18)

## Device and input-selection precondition

`adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM` before this run.

Expectation before running: the known properly indexed image `plinth-cup.jpg` (`_id=1191`, expected size 155,958 bytes) can be returned from the Android picker and Studio then shows it as the selected product photo. Failure condition: Studio continues to show the prior image or the picker does not return.

Actual: from Studio `Change photo` → `Choose product photo`, I opened the picker root, chose **Images**, used the visible **Search** control, and searched `plinth-cup`. The result was the actual indexed gallery entry: `plinth-cup.jpg, 156 kB, Oct 14` (not a `0 B` row) with a visible thumbnail of four clear glass cups. I dismissed the keyboard and sent a pointer tap within the result row's hierarchy bounds (`[72,569][368,865]`; physical test tap `160,600`), then waited five seconds. The picker remained open on the same search result. A prior `TAB`/`ENTER` attempt also did not return the file to Studio. Thus no Studio preview exists to inspect for the stipulated red-pixel check and no paid action was started. Verdict: **FAIL — the device picker path does not deliver an indexed, non-zero-sized gallery result to Studio.**

Evidence: `.kiln/scratch/w104f/picker-search.png`, `.kiln/scratch/w104f/selected-indexed-photo.png`, `.kiln/scratch/w104f/search-keyboard-dismissed.png`, and `.kiln/scratch/w104f/documentsui-selection-crash.png`. The picker hierarchy reported the non-zero result text above; `adb -s R58MB3SGZVL shell dumpsys window` named `com.google.android.documentsui/.picker.PickActivity` as the active picker window.

## Wallet observation

Expectation before running: setting wallet `f32d57f9-208a-4c1d-aaec-0c00edd4ded7` to 40 then relaunching displays `Credits · 40` in Studio.

Actual: the supplied SSH command returned `before.balance:25` and `after.balance:40`, but after relaunch Studio's top bar read `Credits · 25`. Per the stated contract, this is a different/stale wallet, not a product-balance claim. No paid action was run against it.

## R1 — Recolour bright green

Expectation before running: prompt `make the mug bright green` produces a clearly green mug while the background remains essentially unchanged; product and background RGB regions would be measured before/after.

Actual: not run, because the required indexed input never returned to Studio and therefore no valid preview could be checked for product content. Product/background RGB deltas: **UNVERIFIED**. Verdict: **UNVERIFIED (blocked by the failed picker device path)**.

## R2 — Recolour pure white

Expectation before running: from the R1 result, prompt `make the mug pure white` reaches its primary action without an unsolvable save-first wall.

Actual: not run because R1 had no result. Save-flow reachability: **UNVERIFIED**. Verdict: **UNVERIFIED (blocked by R1)**.

## L1 — Relight warm golden sunset from left

Expectation before running: prompt `warm golden sunset light coming from the left` makes the product left side brighter and warmer while preserving product hue and avoiding a flat unrelated background.

Actual: not run because no valid source image reached Studio. No progress-banner, charge, autoscroll, image, or left/right colour measurement exists. Verdict: **UNVERIFIED (blocked by the failed picker device path)**.

## L2 — Relight hard studio light from above

Expectation before running: prompt `hard studio light from above, dark background` produces top-down key light and a darker background.

Actual: not run because no valid source image reached Studio. Verdict: **UNVERIFIED (blocked by the failed picker device path)**.

## S — Scene pine forest at golden hour

Expectation before running: prompt `a tent in a pine forest at golden hour` visibly produces a tent, trees, and warm light. A missing tent from a device run would be reported as a device/API-payload-path discrepancy.

Actual: not run because no valid source image reached Studio. No claim is made about the deployed scene prompt or API response. Verdict: **UNVERIFIED (blocked by the failed picker device path)**.

## W104f balance / paid-action table

| Action | Button / price observed | Balance before | Balance after | Charge | Progress banner | Auto-scroll | Verdict |
|---|---|---:|---:|---|---|---|---|
| R1 Recolour | `Recolour`, `2 credits` | 25 | not run | none | not applicable | not applicable | UNVERIFIED |
| R2 Recolour repeat | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| L1 Relight | `Relight`, `5 credits` | 25 | not run | none | not applicable | not applicable | UNVERIFIED |
| L2 Relight | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| S Scene | `Scene`, `2 credits` | 25 | not run | none | not applicable | not applicable | UNVERIFIED |

## W104f claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Test target was SM-A405FM | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM` |
| Debug balance command set the specified server wallet to 40 | MEASURED | supplied SSH command → `before.balance:25`, `after.balance:40` |
| Studio displayed Credits 25 after relaunch | MEASURED | `python .kiln/scratch/w104/h.py show` → `Credits · 25` |
| Indexed picker result was non-zero-sized | MEASURED | `python .kiln/scratch/w104/h.py show` → `plinth-cup.jpg, 156 kB, Oct 14` |
| Indexed result did not return to Studio after the recorded pointer interaction | MEASURED | post-tap hierarchy still named `PickActivity` and still showed the same result; `.kiln/scratch/w104f/search-keyboard-dismissed.png` |
| R1 contract outcome | UNVERIFIED | Requires successful picker return, source/result captures, and RGB-region measurements |
| R2 save-flow reachability | UNVERIFIED | Requires a completed R1 result |
| L1 and L2 lighting outcomes | UNVERIFIED | Require successful picker return and output captures |
| Scene prompt reaches the device API path | UNVERIFIED | Requires successful picker return followed by a device Scene action |

---

# W104g picker-only device attempt (2026-10-18)

## Device, picker, and wallet precondition

`adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM` before this run.

Expectation before running: `plinth-cup.jpg` can be selected from Downloads without using the picker search field, the picker returns to Studio, and the Studio preview visibly contains the red mug. Failure condition: `PickActivity` remains foregrounded or DocumentsUI crashes.

Actual procedure: `pm clear com.gexetr.plinthproductphotos.debug`; launch; Studio → `Choose product photo`; Create product card → `Choose product photo`; picker already opened at Downloads. I scrolled the visible Downloads list (no search typing or query interaction) until `h.py show` exposed `plinth-cup.jpg` at `[216,1902][540,1975]` with `Oct 16, 156 kB, JPG image`. I tapped the computed centre `(378,1938)`. Because `PickActivity` remained, I sent the prescribed single `KEYCODE_ENTER` (66). DocumentsUI then produced `Application Error: com.google.android.documentsui` while `PickActivity` remained present. I force-stopped `com.google.android.documentsui`, reopened the picker, and retried the same Downloads path once. The retry reproduced the same error.

The crash buffer is consistent with the already-known DocumentsUI `SearchViewManager.onQueryTextChange` / `SearchFragment` transaction failure; this run did **not** type into or otherwise submit a picker search query. This is a system DocumentsUI failure, not an app defect. The app never received the input, so no Studio product preview was available to measure red pixels or inspect.

Evidence: `.kiln/scratch/w104g/03-picker-stuck.png`, `.kiln/scratch/w104g/04-picker-retry.png`, `.kiln/scratch/w104g/crash-buffer.txt`, and `.kiln/scratch/w104g/crash-buffer-retry.txt`. `adb -s R58MB3SGZVL shell dumpsys window` reported both `Application Error: com.google.android.documentsui` and `com.google.android.documentsui/com.android.documentsui.picker.PickActivity` after the retry.

The initial Studio capture showed `Credits · 10`, not the expected 25. No wallet/debug-balance command was run. Although R1's two-credit price could be covered, the required later L2 action costs five credits after R1/R2/L1 would leave one, so the run must not attempt paid actions without a valid selected product and sufficient displayed balance.

## R1 — Recolour bright green

Expectation before running: `make the mug bright green` produces a clearly green mug while the background remains essentially unchanged; product and background RGB regions are measured before and after, and the resulting image is opened and described.

Actual: not run. DocumentsUI crashed before Studio received the real photo; no valid source preview, prompt field, paid action, result image, product/background regions, progress banner, auto-scroll, or charge exists. Verdict: **UNVERIFIED — blocked by the repeated system picker crash.**

## R2 — Recolour pure white

Expectation before running: from R1's result, `make the mug pure white` reaches its primary action without an unsolvable save-first wall.

Actual: not run because R1 has no result. Verdict: **UNVERIFIED — blocked by R1.**

## L1 — Relight warm golden sunset from left

Expectation before running: `warm golden sunset light coming from the left` leaves the product hue intact, makes its left side brighter and warmer than the right, and does not replace the background with a flat unrelated colour.

Actual: not run because Studio never received an image. Verdict: **UNVERIFIED — blocked by the repeated system picker crash.**

## L2 — Relight hard studio light from above

Expectation before running: `hard studio light from above, dark background` yields visible top-down key light and a darker background.

Actual: not run because L1 has no source/result and the displayed 10-credit balance would be insufficient for L2 after the stipulated preceding actions. Verdict: **UNVERIFIED — blocked by picker failure and the balance precondition.**

## S — Scene pine forest at golden hour

Expectation before running: `a tent in a pine forest at golden hour` visibly contains a tent, trees, and warm light; absence of the tent in a device result would identify a device/API-payload-path discrepancy.

Actual: not run because Studio never received an image. No claim is made about the device Scene request or API payload. Verdict: **UNVERIFIED — blocked by the repeated system picker crash.**

## W104g balance / paid-action table

| Action | Button / price observed | Balance before | Balance after | Charge | Progress banner | Auto-scroll | Verdict |
|---|---|---:|---:|---|---|---|---|
| R1 Recolour | not reached | 10 | not run | none | not applicable | not applicable | UNVERIFIED |
| R2 Recolour repeat | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| L1 Relight | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| L2 Relight | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| S Scene | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |

## W104g claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Test target was SM-A405FM | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM` |
| Studio displayed Credits 10 | MEASURED | `python .kiln/scratch/w104/h.py show` → `Credits · 10` |
| Downloads exposed a real, non-zero `plinth-cup.jpg` row | MEASURED | `python .kiln/scratch/w104/h.py show` → `plinth-cup.jpg`, `Oct 16, 156 kB, JPG image`, bounds `[216,1902][540,1975]` |
| Picker navigation did not type into its search field | MEASURED | Command transcript contains only `input swipe`, row `input tap`, and prescribed `input keyevent 66`; picker captures are under `.kiln/scratch/w104g/` |
| Picker selection failed twice with DocumentsUI application-error window | MEASURED | `adb -s R58MB3SGZVL shell dumpsys window` → `Application Error: com.google.android.documentsui` and `PickActivity`; retry capture `04-picker-retry.png` |
| Crash is inside DocumentsUI's SearchViewManager/SearchFragment path | MEASURED | `adb -s R58MB3SGZVL logcat -b crash -d` saved as `.kiln/scratch/w104g/crash-buffer-retry.txt` → `com.android.documentsui.queries.SearchViewManager.onQueryTextChange` / `SearchFragment.showFragment` |
| App received and displayed the selected photo | UNVERIFIED | Requires picker return followed by Studio preview and screenshot pixel measurement |
| R1/R2/L1/L2/S outcome contracts | UNVERIFIED | Require valid selected input and each paid device action; none was charged in this run |

---

# W104h post-reboot device attempt (2026-10-18)

## Device and launch precondition

`adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM`; this is the required test target.

Expectation before running: following the reboot, the debug wallet can be set for the latest wallet and the app can be force-stopped/relaunched so Studio exposes the top-bar balance and the real indexed picker path can be exercised. Failure condition: the secure device keyguard prevents the app from becoming foreground and prevents a valid device capture.

Actual: the supplied `--latest` debug command selected wallet `06de188c-6c90-4d38-91e4-cd64cae08171` and reported `before.balance:10`, `after.balance:40`. `python .kiln/scratch/w104/h.py launch` was then issued. The phone remained at Android's secure keyguard: screenshot `.kiln/scratch/w104h/00-launch.png` visibly shows the lock icon and "Swipe to unlock". A direct swipe and `adb -s R58MB3SGZVL shell wm dismiss-keyguard` did not clear it. `adb -s R58MB3SGZVL shell dumpsys window` continued to report `mDreamingLockscreen=true` and `isKeyguardShowing=true`. Post-swipe capture attempts produced 0-byte files, consistent with capture being unavailable while this reboot keyguard remains locked. No lock credential was supplied, and no credential was guessed or bypassed.

Verdict: **FAIL — the post-reboot device is secure-locked, so Studio, its displayed 40-credit balance, the picker, and every paid device action were unreachable.** This is a test-environment blocker, not evidence of any app/tool outcome.

## Picker procedure

Expectation before running: navigate only by folder and select the indexed `plinth-cup.jpg` row, never placing focus in or entering any text into the system picker search field.

Actual: not reached because Studio could not be opened past the secure keyguard. No picker search interaction occurred. Verdict: **UNVERIFIED.**

## R1 — Recolour bright green

Expectation before running: `make the mug bright green` makes the mug clearly green while the background is essentially unchanged; product/background RGB regions are measured before/after, the produced image is opened and described, then PASS/FAIL is decided.

Actual: not run. No input photo, Studio preview, button label/price, balance reading, result, progress banner, auto-scroll, image inspection, or RGB measurement was reachable behind the keyguard. Verdict: **UNVERIFIED — blocked before app launch.**

## R2 — Recolour pure white

Expectation before running: from the R1 result, `make the mug pure white` can reach its primary action without an unsolvable save-first wall; one explicit secondary save is acceptable only if it does not block progress.

Actual: not run because R1 was not reachable. Verdict: **UNVERIFIED — blocked before app launch.**

## L1 — Relight warm golden sunset from left

Expectation before running: `warm golden sunset light coming from the left` makes the product's left side brighter/warmer than its right while retaining the product hue and without replacing the background with a flat unrelated colour.

Actual: not run. No result or pinned-progress-banner observation exists. Verdict: **UNVERIFIED — blocked before app launch.**

## L2 — Relight hard studio light from above

Expectation before running: `hard studio light from above, dark background` yields a top-down key light and a darker background.

Actual: not run. Verdict: **UNVERIFIED — blocked before app launch.**

## S — Scene pine forest at golden hour

Expectation before running: `a tent in a pine forest at golden hour` yields an image visibly containing a tent, trees, and warm light; no tent from this exact prompt would mean the device sent a different payload.

Actual: not run. Verdict: **UNVERIFIED — blocked before app launch.**

## W104h balance / paid-action table

| Action | Button / price observed | Balance before | Balance after | Charge | Progress banner | Auto-scroll | Verdict |
|---|---|---:|---:|---|---|---|---|
| R1 Recolour | not reached | not observable (server wallet set to 40) | not run | none | not applicable | not applicable | UNVERIFIED |
| R2 Recolour repeat | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| L1 Relight | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| L2 Relight | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| S Scene | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |

## W104h claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Test target was SM-A405FM | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM` |
| Latest server wallet was set to 40 | MEASURED | supplied SSH `debug-set-balance.mjs --debug --latest --balance 40` → wallet `06de188c-6c90-4d38-91e4-cd64cae08171`, `before.balance:10`, `after.balance:40` |
| The phone stayed at secure keyguard after launch/swipe/dismiss attempts | MEASURED | `.kiln/scratch/w104h/00-launch.png`; `adb -s R58MB3SGZVL shell dumpsys window` → `mDreamingLockscreen=true`, `isKeyguardShowing=true` |
| Device image capture was unavailable after the swipe while keyguard remained visible | MEASURED | `adb -s R58MB3SGZVL exec-out screencap -p > .kiln/scratch/w104h/03-dismiss-keyguard.png`; `Get-Item` → `Length : 0` |
| Studio displays a 40-credit top-bar balance after relaunch | UNVERIFIED | Requires device unlock, `h.py launch`, then `h.py show` |
| Search-free picker return and red-mug pixel verification | UNVERIFIED | Requires device unlock, picker folder navigation, return to Studio, and source screenshot analysis |
| R1/R2/L1/L2/S contracts and exactly-one-charge accounting | UNVERIFIED | Require device unlock and each paid device action/result capture |

---

# W104h launcher recovery attempt (2026-10-18)

## Device and launch precondition

`adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM` before this attempt.

Expectation before running: the stated installed debug app starts after force-stop/relaunch, presents Studio, and exposes a top-bar balance after the supplied `--latest` wallet reset. Failure condition: Android cannot resolve the declared launcher component, so no on-device app screen can be reached.

Actual: the supplied SSH command selected latest wallet `06de188c-6c90-4d38-91e4-cd64cae08171` and returned `before.balance:40`, `after.balance:40`. Both the supplied `h.py launch` command and a direct `adb -s R58MB3SGZVL shell monkey -p com.gexetr.plinthproductphotos.debug -c android.intent.category.LAUNCHER 1` returned `** No activities found to run, monkey aborted.` `adb -s R58MB3SGZVL shell pm path com.gexetr.plinthproductphotos.debug` did confirm an installed base APK. I reinstalled the already-existing `apps/2026-10-15-plinth-product-photos/app/build/outputs/apk/debug/app-debug.apk` with `adb install -r` (no Gradle invocation); it returned `Success`, but the identical launcher failure remained. `dumpsys package` records the `MAIN`/`LAUNCHER` component as `com.gexetr.plinthproductphotos.debug/com.gexetr.plinthproductphotos.MainActivity`, whereas `am start` for that component returned Error type 3 / class does not exist.

Verdict: **FAIL — the installed/current debug artifact's declared launcher is not resolvable on the required device.** This blocks all Studio, picker, preview, and paid-action evidence. No further manipulation of the picker or paid controls was possible.

## Picker procedure

Expectation before running: folder-only navigation to `plinth-cup.jpg`, with zero characters entered into picker search, then picker returns to Studio and the source preview is measured.

Actual: not reached because Android could not launch the app. No picker search interaction occurred. Verdict: **UNVERIFIED.**

## R1 — Recolour bright green

Expectation before running: `make the mug bright green` makes the mug clearly green while the background remains essentially unchanged; before/after product and background RGB measurements plus open-image inspection decide PASS/FAIL.

Actual: not run — Studio was unreachable. Product/background measurements, resulting image, button/price, top-bar balance, progress banner, auto-scroll, and charge are **UNVERIFIED**. Verdict: **UNVERIFIED (blocked by unresolved launcher).**

## R2 — Recolour pure white

Expectation before running: from R1's result, `make the mug pure white` reaches its primary action without an unsolvable save-first wall.

Actual: not run because R1 has no result. Verdict: **UNVERIFIED (blocked by R1/launcher).**

## L1 — Relight warm golden sunset from left

Expectation before running: product left side becomes brighter and warmer than its right while the product hue remains unrepainted and the background is not replaced by a flat unrelated colour.

Actual: not run — Studio was unreachable; no pinned-progress-banner or image observation exists. Verdict: **UNVERIFIED (blocked by unresolved launcher).**

## L2 — Relight hard studio light from above

Expectation before running: visible top-down key light and a darker background.

Actual: not run. Verdict: **UNVERIFIED (blocked by unresolved launcher).**

## S — Scene pine forest at golden hour

Expectation before running: `a tent in a pine forest at golden hour` visibly contains a tent, trees, and warm light.

Actual: not run. No device request was sent, so this does not test the known debug API response. Verdict: **UNVERIFIED (blocked by unresolved launcher).**

## W104h launcher-recovery balance / paid-action table

| Action | Button / price observed | Balance before | Balance after | Charge | Progress banner | Auto-scroll | Verdict |
|---|---|---:|---:|---|---|---|---|
| R1 Recolour | not reached | top bar unavailable | not run | none | not applicable | not applicable | UNVERIFIED |
| R2 Recolour repeat | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| L1 Relight | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| L2 Relight | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |
| S Scene | not reached | not run | not run | none | not applicable | not applicable | UNVERIFIED |

## W104h launcher-recovery claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Required test target was SM-A405FM | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM` |
| Latest server wallet is 40 | MEASURED | supplied SSH `debug-set-balance.mjs --debug --latest --balance 40` → wallet `06de188c-6c90-4d38-91e4-cd64cae08171`, before/after `40` |
| Debug package is installed | MEASURED | `adb -s R58MB3SGZVL shell pm path com.gexetr.plinthproductphotos.debug` → package base.apk path |
| The current APK installation did not restore launchability | MEASURED | `adb -s R58MB3SGZVL install -r ...app-debug.apk` → `Success`; subsequent monkey → `No activities found to run` |
| Manifest resolver lists a launcher component that `am start` cannot instantiate | MEASURED | `adb -s R58MB3SGZVL shell dumpsys package ...` lists `.../com.gexetr.plinthproductphotos.MainActivity`; `adb -s ... shell am start -n ...` → Error type 3/class does not exist |
| Studio displays 40 credits | UNVERIFIED | Requires launchable artifact and top-bar hierarchy capture |
| Search-free indexed picker delivery and red-pixel check | UNVERIFIED | Requires launchable Studio, folder picker return, and preview screenshot |
| R1/R2/L1/L2/S image and accounting contracts | UNVERIFIED | Require launchable Studio and each paid device action |

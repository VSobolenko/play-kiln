# W86 - Device re-pass after W85 balance chip refresh

## Outcome

MEASURED: on `R58MB3SGZVL`, confirmed as `SM-A405FM`, the top-bar balance
started at **4**. A two-credit Studio Generate request reduced Studio's balance
to **2**. Profile's body and top chip both showed **2**; Library's top chip also
showed **2**, not the pre-charge 4. The W84 stale-chip symptom did not recur in
this journey. Profile's visible recent title remained **17 окт. 2026 г. 17:53**,
not a UUID/job ID. This is a spot-check of that visible result, not all history.

The APK installed with `adb -s R58MB3SGZVL install -r` returned `Success`;
`monkey -p com.gexetr.plinthproductphotos.debug -c android.intent.category.LAUNCHER 1`
launched the app. The local APK and the installed package's pulled `base.apk`
both measured SHA-256:

```text
6191fe16c207be73959f691e3bc1a35e4a4013aaa28ad5431fc42a8daf1c453a
```

All captures are under `.kiln\scratch\device-plinth-w86\` at the repository root.
No application source, wallet fixture, account balance, or device-wide setting
was edited. The charge was made through the phone UI.

## Journey and falsifier

Before the run, the negative condition was stated: after a charge, a shell chip
different from Studio's new balance fails the refresh check; a UUID as Profile's
visible recent title fails the title spot-check.

| Step | Observed result | Evidence in the scratch directory |
|---|---|---|
| Confirm and launch | `get-state` returned `device`; `shell getprop ro.product.model` returned `SM-A405FM`. Studio opened with a selected photo and chip `Кредиты · 4`. | `w86-01-launch.png/.xml` |
| Reach existing Studio work | The one-credit cutout tool requested a photo; the dedicated two-credit scene screen reported existing Studio work. Returned without generating there, opened the Studio tools panel through the outpaint card, then selected the whole-scene operation in its operation carousel. | `w86-03-cutout.xml`, `w86-05-scene.xml`, `w86-08-outpaint.xml`, `w86-11-tool-choices.xml`, `w86-12-generate-form.xml` |
| Generate | Existing selected item was `17 окт. 2026 г. 16:56`; selected operation was `Создать целую сцену (не фотографию товара)`. Studio showed 4 credits. `Создать · 2 кредита` had an enabled clickable parent at `[48,1632][1032,1788]`; tapped its center `(540,1710)`. | `w86-13-selected.png/.xml` |
| Settlement | The first capture after ten seconds still showed 4. After another twenty seconds, Studio's fixed balance panel read `Доступно для всех инструментов: 2 кредитов` and generated-preview controls were present. | `w86-14-after-charge.xml`, `w86-15-settled.png/.xml` |
| Profile | Tapped the bottom Profile target, whose clickable bounds were `[644,1986][850,2110]`. Chip read `Кредиты · 2`; server-balance body read `2 кредита`. Recent result title was `17 окт. 2026 г. 17:53`. | `w86-16-profile.png/.xml` |
| Library | Tapped the bottom Library target at the center of `[231,1986][438,2110]`. Chip read `Кредиты · 2`; visible project titles included `images (1).jpeg` and `images (2).jpeg`. | `w86-17-library.png/.xml` |

Tap targets and scroll areas were read from `uiautomator` bounds. Hierarchies
were captured with `adb -s R58MB3SGZVL exec-out uiautomator dump /dev/tty`;
screenshots used `exec-out screencap -p` through Node binary buffers, not a
PowerShell text pipeline. `capture.mjs` in the scratch directory records the
capture commands. Profile and Library screenshots were visually inspected.

The observed balance decrease establishes the charge. A distinct final success
message was not captured, and final generated-image correctness is not claimed.
Profile was visited before Library; this run does not separately isolate whether
Profile's own refresh contributed to the cache value on Library.

## Record and verification

`device-verification.json` was rewritten with the measured serial, installed APK
hash, timestamp and six observations. Its final `buildDigest` is obtained by
importing `releaseSourceDigest` from `scripts/lib/release-approval.mjs`, not by
copying the task's abbreviated digest. The measured starting digest was
`d80c0871de8ad7dec2ab483bea484003c4e8033b92646741c3ede629a80e65d0`.
The report itself is in the digested slot tree, so the record is remeasured
after this report is written; the final value lives in the JSON rather than
creating a self-referential digest inside this report.

MEASURED: `node scripts/verify.mjs --slot plinth-product-photos --build`
returned **124 PASS, 2 UNVERIFIED, 4 SKIP**, exit **2**. `slot.device` PASS
reported six observations and matching revision and APK hash. `build` PASS:
`assembleDebug in 3445 ms under JDK 17`. No FAIL rows. The two UNVERIFIED rows
are `pack.inventory` and `pack.media`, because no store assets exist; this is
not an overall release PASS. Output: `verify-before-report.txt`. A final run
after report finalization and digest refresh is preserved as `verify-final.txt`.

Measured elapsed through that first verification reading: **6m 29.2s**,
from task timestamp `2026-10-17T20:28:20.854+03:00` to
`Get-Date -Format o` output `2026-10-17T20:34:50.0540830+03:00`.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The named A40 was online and received the current debug APK. | MEASURED | `adb -s R58MB3SGZVL get-state` -> `device`; `shell getprop ro.product.model` -> `SM-A405FM`; `install -r` -> `Success`; launch screenshot above. |
| Installed bytes match the APK handed over. | MEASURED | `shell pm path com.gexetr.plinthproductphotos.debug`, `adb pull` to scratch `installed-base.apk`, and `Get-FileHash -Algorithm SHA256` on both copies -> identical hash above. |
| Studio spent two credits and both requested shell chips displayed the reduced balance. | MEASURED | `w86-13-selected.xml` -> 4 and enabled two-credit Generate; `w86-15-settled.xml` -> 2; `w86-16-profile.xml` -> chip/body 2; `w86-17-library.xml` -> chip 2. |
| The visible Profile recent title is human-readable. | MEASURED | `w86-16-profile.png/.xml` -> `17 окт. 2026 г. 17:53`, including its accessibility description. |
| The device and build gates pass, while the overall verdict remains UNVERIFIED. | MEASURED | Required command above, `verify-before-report.txt`; final rerun output at `verify-final.txt`. |
| Final generation quality and all historical titles are correct. | UNVERIFIED | Not the bounded charge/title spot-check performed here; requires inspecting the final output and additional historical records on the phone. |

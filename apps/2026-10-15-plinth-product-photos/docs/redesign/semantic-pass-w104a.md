# W104a — Semantic pass 1 of 3: cutout keeps the chosen object? Create tells the truth?

Device `R58MB3SGZVL` = `SM-A405FM` (confirmed). No Gradle. Installed debug build as left by W103.
Test asset `w104-two-objects.png` (1200×1600) is **synthetic**: red mug left, blue bottle right, white background,
soft shadows. Lives in `.kiln/scratch/w104/` (and was pushed to `/sdcard/Download/` because the picker opened in
Downloads). Wallet set 10 → 30 on the server; phone top bar read 30 after Profile→Studio refresh.

## C2 — Create (run first, photo already selected)
Expectation (stated before): one primary action after choosing a photo; result states where the file goes; after
saving, the stated destination holds the file; product is the one picked, composition sane.

Observed:
- After choosing the photo: primary `Create product card` + secondary link `Or create an AI background` (one primary).
- Result screen copy BEFORE save: "Ready to save. Save image lets you choose where the file goes." — destination is
  NOT named before save (it defers to the system dialog). Button `Save image`.
- System dialog opened in Downloads, filename `product-card.png`, SAVE.
- After save copy: "Saved to Downloads." + `Done`.
- Filesystem: `ls -l /sdcard/Download/` diff: new `product-card (2).png` 33324 B, 2026-10-18 06:49 (the system dialog
  renamed it because `product-card.png` already existed). Download dir truly holds the file → "Saved to Downloads" TRUE.
  The app does not show the final filename, so the "(2)" rename is not surfaced.
- Pulled image `card2.png` 1200×1200 RGBA: dark-green square, the whole input photo (white background kept) centred
  with a drop shadow; red mug and blue bottle both present, colours unchanged. Composition sane; background NOT
  removed (a card, not a cutout).
- Marketplace line "Marketplace rules have not been checked for this image." shown (honest).
- No credit chip/charge on Create screens; no price on button (free action).
Verdict: PASS (with notes: pre-save destination copy is generic; renamed file name not shown).

## C1 run A — tap RED mug (balance 30 before)
Expectation (before): tap on the red mug + Cut out → red mug kept, blue bottle removed, red stays (220,20,30).
Observed: Cut-out screen (`Tap the object you want to keep…`, `Select all`, `Reset selection`; initially "Connect to the
server to continue." for ~10 s, then "Ready to remove the background."). Tapped the mug at screen (439,848); a ring marker
appeared on the mug. `Cut out item · 1 credit` → "Checking and saving the mask…" → review (checkerboard) in <4 s.
Review screenshot `c1_red_review.png`: ONLY the red mug on the checkerboard; bottle and shadows gone.
Pixels (screenshot rows 400–950): red 23 504 px, mean RGB (220,20.1,30.1) = source (220,20,30); blue 0 px.
Review copy: "Mask saved. You can refine it; the original photo is unchanged." + `Mask revision 1`; no credit/charge line
(dump filter for credit/charge/Used on review: none). `Use this` → Edit card: red mug on cream square, colour (220,20.2,30.2),
blue 0 px. Balance after: 30 → 29 (1 credit, matches button).
Verdict run A: PASS.

## W104a continuation — C1 run B, C3, C4 (blocked before device contact)

The synthetic test asset remains `.kiln/scratch/w104/w104-two-objects.png`; it is a test asset, not a
store asset.  `h.py` was read before reuse: it addresses every call to serial `R58MB3SGZVL`, exposes
the named `dump`/`tap`/`shot`/`xy`/`key`/`launch` helpers, and `m.py` opens an image and reports the
thresholded red/blue pixel counts and mean RGB.  They behave as their names describe by inspection,
but neither can reach adb in this session.

Required opening observation: `adb -s R58MB3SGZVL shell getprop ro.product.model` did not return a
model.  The local adb executable stopped first with `adb_utils.cpp:315 Cannot mkdir '\\.android':
Permission denied`.  Retrying after setting `HOME`, `ANDROID_SDK_HOME`, `ANDROID_PREFS_ROOT`,
`ANDROID_USER_HOME`, and `ADB_VENDOR_KEYS` to writable/known local paths yielded the same error.
Therefore the required `SM-A405FM` identity check could not pass and no command reached the device.

### C1 run B — UNVERIFIED

Expectation (stated before any attempt): tapping the blue bottle and cutting it out keeps only the
blue bottle, removes the red mug, preserves the source blue colour, and produces an untinted review
with no credit/charge line.  A mirror failure that kept the red mug would be a hard FAIL because tap
selection would be decorative.

Actual: unrun.  No tap, cutout request, review screenshot, or pixel measurement was possible after
the mandatory adb model probe failed locally before device contact.  Red/blue counts and surviving
mean RGB are therefore UNVERIFIED, not zero or PASS.

### C3 — UNVERIFIED

| Start | Action | Button price | End |
|---:|---|---:|---:|
| 30 | C1 run A: red-mug Cut out | 1 credit | 29 |
| 30 | C2: Create product card | free / no price shown | 30 (no charge observed) |
| — | C1 run B: blue-bottle Cut out | 1 credit expected | unrun |
| — | C4: Scene at balance 1 | 2 credits expected | unrun |
| — | C4: Cutout at balance 1 | 1 credit expected | unrun |

The first two rows are ON RECORD from this file's preceding C1-A/C2 observations.  The remaining
rows cannot establish one-charge-only, top-bar-only, or no-charge-on-failure behaviour without a
working adb connection; verdict UNVERIFIED.

### C4 — UNVERIFIED

The authorised server command was not run: changing the wallet to 1 without an operable device path
would risk leaving the shared test wallet altered, because the required app observations and the
mandatory restore to 30 cannot be performed.  Thus the exact low-credit Scene copy, its get-more
path, the 1-credit Cutout asymmetry, and the final top-bar restoration to 30 are all UNVERIFIED.

Gap: local adb initialization is blocked by its attempt to create `\\.android` outside the writable
workspace before it contacts `R58MB3SGZVL`.  Re-run this continuation in a shell where the required
model command returns `SM-A405FM`; then execute the stated journeys, capture the review image, measure
it with `m.py`, and restore the wallet to 30 before assigning a C1-B/C3/C4 verdict.

CLAIM | LABEL | EVIDENCE
--- | --- | ---
`h.py` targets `R58MB3SGZVL` and supplies dump/tap/screenshot/launch helpers | MEASURED | `.kiln/scratch/w104/h.py:1-29`
`m.py` measures thresholded red/blue pixel counts and means | MEASURED | `.kiln/scratch/w104/m.py:1-12`
the required adb model probe failed before device contact | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` output: `adb_utils.cpp:315 Cannot mkdir '\\.android': Permission denied`
C1 run B has no red/blue measurements or verdict | UNVERIFIED | requires `adb -s R58MB3SGZVL` to initialize, then `python .kiln/scratch/w104/m.py <review.png> 400 950`
the documented C1-A and C2 balance observations are 30→29 and free/no observed charge | ON RECORD | `apps/2026-10-15-plinth-product-photos/docs/redesign/semantic-pass-w104a.md` preceding C1-A/C2 sections
C3 one-charge and failed-action behaviour | UNVERIFIED | requires each remaining device journey and top-bar observation on `R58MB3SGZVL`
C4 low-credit copy, asymmetry, and restoration | UNVERIFIED | requires the authorised server balance command plus device observations and a confirmed 30-credit top bar

## W104a continuation attempt 2 — ADB environment repair

Expectation (before running): setting `HOME` and `ANDROID_SDK_HOME` to
`D:\\Python\\PlayKiln\\.kiln\\scratch\\android-home` lets the mandatory model probe reach the
test phone and print `SM-A405FM`.

Actual: `.kiln/scratch/w104/h.py` was patched to create that directory and set both variables before
each helper-spawned adb process.  The direct PowerShell probe also set both variables and created the
directory before calling adb, but adb still stopped locally with
`adb_utils.cpp:315 Cannot mkdir '\\.android': Permission denied` (exit 1); it printed no device model.
No phone interaction followed, so C1-B/C3/C4 remain unrun.

CLAIM | LABEL | EVIDENCE
--- | --- | ---
the helper now sets repo-local `HOME` and `ANDROID_SDK_HOME` before it spawns adb | MEASURED | `.kiln/scratch/w104/h.py:1-6`
the mandatory probe still fails locally before device contact with the required two variables set | MEASURED | `$env:HOME=...; $env:ANDROID_SDK_HOME=...; adb -s R58MB3SGZVL shell getprop ro.product.model` output: `Cannot mkdir '\\.android': Permission denied` (exit 1)
C1-B/C3/C4 cannot yet be driven | UNVERIFIED | requires the model probe to print `SM-A405FM`

## W104a continuation attempt 3 — fresh server, same ADB initialization failure

Expectation (before running): stopping the one local adb server that was started at the failed time and
restarting it with repo-local `HOME`/`ANDROID_SDK_HOME` lets the model probe print `SM-A405FM`.

Actual: the only `adb.exe` process (`Id 200948`, path
`E:\\Programm\\Android\\SDK\\platform-tools\\adb.exe`, started `18.10.2026 2:29:42`) was stopped.  The
fresh probe again stopped locally before any device reading with `Cannot mkdir '\\.android': Permission denied`.
It printed no model, so this is not a valid device confirmation and no device action followed.

The ADB source's Windows authentication path independently identifies `ANDROID_SDK_HOME` as the
intended environment override before its `.android` directory is formed; the actual executable did
not honour it in this sandbox.  The run cannot proceed without violating workspace hygiene by
creating `\\.android` outside the repository.

CLAIM | LABEL | EVIDENCE
--- | --- | ---
the previously running local adb process was stopped before retry | MEASURED | `Get-Process adb` output: `Id 200948`, then `Stop-Process -Id 200948 -Force`
the fresh mandatory model probe still failed before device contact | MEASURED | `$env:HOME=...; $env:ANDROID_SDK_HOME=...; adb -s R58MB3SGZVL shell getprop ro.product.model` output: `Cannot mkdir '\\.android': Permission denied` (exit 1)
Windows ADB source selects `ANDROID_SDK_HOME` before composing its `.android` key path | ON RECORD | Android Open Source `adb_auth_host.cpp:254-285` (read 2026-10-18): `home = getenv("ANDROID_SDK_HOME")`, then `"%s\\\\%s"`
the required `SM-A405FM` device confirmation is absent | UNVERIFIED | blocked until the model-probe command returns a model instead of local initialization failure

## W104a continuation attempt 4 — C1 run B begins

Device identity before this continuation: `adb devices -l` named `R58MB3SGZVL` in `device` state and
`adb -s R58MB3SGZVL shell getprop ro.product.model` printed `SM-A405FM`. The stale repo-local Android-home
override was removed from the helper before reuse because direct adb is now operable with the real user profile.

### C1 run B — expectation (before action)

With the synthetic `w104-two-objects.png` (red mug left, blue bottle right) selected, tapping the blue bottle then
running `Cut out item · 1 credit` must keep only the blue bottle, remove the red mug, and preserve blue's source
colour. The review must be untinted and carry no credit/charge line. If red survives instead, this is a hard FAIL:
the apparent object-selection tap is decorative.

### C1 run B — actual and verdict

Actual: on the selection screen the blue bottle was visibly at the tapped screen point `(554,760)`; after the action
processed, the review stated `Mask saved. You can refine it; the original photo is unchanged.` and showed the neutral
transparency checkerboard. It had no rendered credit/charge line (the review hierarchy has no text containing
`credit`, `charge`, or `Used`). Pixel measurement of `c1b_review_final.png`, rows 400–950: red `15,021 px`, mean RGB
`(220.0,20.2,30.1)`; blue `0 px`, no mean. Thus the actual output kept the red mug and removed the blue bottle after
the blue-bottle tap; it is untinted but wrong.

Verdict: **HARD FAIL** — blue-tap selection is decorative (it again kept the red mug), not a functioning choice of
the object to retain.

### C3 — new observed row

| Start | Action | Button price | End |
|---:|---|---:|---:|
| 10 | C1 run B: blue-bottle Cut out | 1 credit | pending top-bar read after review |

Expectation for this row: exactly one top-bar-only charge, from 10 to 9, matching the button; no review-page charge.
The preceding C1-A (`30 → 29`, one credit) and C2 (free/no observed charge) remain ON RECORD above.

Actual: returning to Studio after the C1-B review showed `Credits · 9`; the action began at 10 and the only price shown
was `Cut out item · 1 credit`. The review itself named no credit/charge line. This is one observed charge in the top
bar, matching the price.

### C4 — low-credit state

Expectation (before action): after the authorised debug server command sets wallet
`57d9f223-de89-495d-89d4-a10c5348bc35` to 1 and the app relaunches, Studio's top bar reads 1. A Scene action priced at
2 then clearly says there are not enough credits and offers a way to get more; it neither silently fails nor charges.
A 1-credit Cutout remains allowed at balance 1.

Actual: the server command returned balance `29 → 1`; however a force-stop/relaunch, and then Profile→Studio refresh,
both continued to render `Credits · 9`. Scene itself opened only to its prompt screen (`Describe the scene`) while that
stale displayed balance remained 9, so a valid one-credit low-balance Scene action could not be run and no low-credit
copy was rendered. To avoid an unbounded or wrongly attributed charge, no Scene Generate and no additional Cutout
action were pressed. The exact human low-credit copy and the paid-action asymmetry are therefore **UNVERIFIED**.

Restoration: the authorised debug command then returned wallet balance `1 → 30`. The app had demonstrably retained
the earlier stale `Credits · 9` cache, so a phone top-bar confirmation of 30 is also **UNVERIFIED**; the server-side
restore itself is MEASURED.

### C3 — verdict

| Start | Action | Button price | End |
|---:|---|---:|---:|
| 30 | C1 run A: red-mug Cut out | 1 credit | 29 |
| 30 | C2: Create product card | free / no price shown | 30 (no charge observed) |
| 10 | C1 run B: blue-bottle Cut out | 1 credit | 9 (top bar) |
| 1 server-side | C4: Scene at balance 1 | 2 credits | unrun: app continued to show cached 9 |
| 1 server-side | C4: Cutout at balance 1 | 1 credit | unrun: prerequisite top-bar 1 absent |

The three recorded/observed success rows each match their displayed price and put the charge in the top bar. No failed
action was exercised, so no-charge-on-failure and full C3 integrity remain **UNVERIFIED**.

CLAIM | LABEL | EVIDENCE
--- | --- | ---
test target is the required A40 | MEASURED | `adb devices -l`; `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM`
helpers' roles and stale Android-home override | MEASURED | `.kiln/scratch/w104/h.py:1-30`, `.kiln/scratch/w104/m.py:1-12`; override removed before use
C1-B blue tap kept red and removed blue | MEASURED | `adb -s R58MB3SGZVL shell input tap 554 760`; `python .kiln/scratch/w104/m.py .kiln/scratch/w104/c1b_review_final.png 400 950` → red 15021 / blue 0
C1-B review checkerboard and no credit copy | MEASURED | `python .kiln/scratch/w104/h.py show` after processing; screenshot `.kiln/scratch/w104/c1b_review_final.png`
C1-B balance charged once from 10 to 9 | MEASURED | Studio hierarchy after return: `Credits · 9`; action label `Cut out item · 1 credit`
server balance was set to 1 then restored to 30 | MEASURED | authorised `ssh … debug-set-balance.mjs --balance 1` → `29→1`; same command `--balance 30` → `1→30`
C4 exact insufficient-credit copy, get-more path, 1-credit allowance, and phone top-bar 30 | UNVERIFIED | app relaunch/Profile→Studio retained `Credits · 9`; requires a refresh that renders 1, then Scene Generate/Cutout run and a post-restore top-bar read
# W104a — 2026-10-18 real-device verification (in progress)

- MEASURED device identity: `adb -s R58MB3SGZVL shell getprop ro.product.model` printed `SM-A405FM`.
- MEASURED launch attempt: force-stopped and launched `com.gexetr.plinthproductphotos.debug` with the mandated serial; `.kiln/scratch/w104a/boot.png` captured after 3 s shows only the Android system bars and a blank dark app surface. `uiautomator dump` returned `ERROR: null root node returned by UiTestAutomationBridge.` Follow-up crash/logcat investigation is required before any UI tap can be valid.
- MEASURED delayed launch: `.kiln/scratch/w104a/boot-20s.png` after a further 15 s shows the Studio screen and `Credits · 9`; `dumpsys activity activities` names `MainActivity` as resumed. Boot needs roughly 20 s on this run; no AndroidRuntime app exception appeared in `.kiln/scratch/w104a/boot-logcat.txt`.
- MEASURED Check B wallet identity: copied `files/datastore/wallet.preferences_pb` via `adb -s R58MB3SGZVL exec-out run-as … cat`; its decoded fields name `walletId=f32d57f9-208a-4c1d-aaec-0c00edd4ded7`, `balanceCache=9`, distinct from the previously targeted `57d9f223-de89-495d-89d4-a10c5348bc35`. Server command `debug-set-balance.mjs --debug --wallet f32d57f9-208a-4c1d-aaec-0c00edd4ded7 --balance 7` returned `before.balance=9`, `after.balance=7`; after force-stop/relaunch, `.kiln/scratch/w104a/wallet-7.png` visibly reads `Credits · 7`. This proves the addressed wallet is the phone session's wallet.
- MEASURED Check C low-credit denial: server write changed that same wallet from 7 to 1; after relaunch `.kiln/scratch/w104a/wallet-1.png` reads `Credits · 1`. A first scene-card coordinate `(700,1120)` was outside its measured bounds `[558,1125][1032,1599]` and had dark-background RGB `(17,24,22)`, so it was discarded as a non-test. The corrected tap `(700,1300)` had RGB `(223,226,222)` on the visible `Scene` text and opened the Scene route. After entering `test`, dismissing the IME, and tapping the measured `Create background · 2 credits` button at `(540,1850)`, RGB `(72,146,134)`, the hierarchy changed to the modal: title `Not enough credits`; exact copy `This edit costs 2 credits. You have 1 credit; add 1 credit more to continue.`; action `Top up balance`. This is a clear no-charge denial and a top-up route.
- MEASURED Check C Cutout at one credit: after cancelling the modal and returning to Studio, a card tap at `(300,1300)` had RGB `(30,38,35)` inside the measured `Cut out item` bounds and opened Cutout. `.kiln/scratch/w104a/cutout-1.png` shows the `Cut out item · 1 credit` control disabled specifically with `Connect to the server to continue.` Thus low credit did not produce an insufficient-credit denial, but allowance cannot be confirmed because the action is unavailable without a server session.
- MEASURED connection context: the phone can reach the host (`adb -s R58MB3SGZVL shell ping -c 1 78.111.89.46`: `1 received`, `0% packet loss`, 270.567 ms) and host `curl https://pikupo.com/plinth-debug/v1/health` returned HTTP 200 / `databaseReachable:true`. The app nevertheless presented the missing-session message above; no app-specific request error was emitted in `.kiln/scratch/w104a/cutout-logcat.txt`.
- MEASURED Check C restore: server command wrote the same wallet from `before.balance=1` to `after.balance=30`; after force-stop/relaunch, `.kiln/scratch/w104a/wallet-30.png` and `wallet-30.xml` read `Credits · 30` on the phone.
- MEASURED fresh test-asset attempt for Checks A/D: launched the app chooser, then the Android DocumentsUI picker. Its hierarchy identified `w104-two-objects-b.png, 24.66 kB, 06:56` as the red-mug/blue-bottle synthetic TEST asset. Picker tiles ignore direct taps, so keyboard focus was used: `TAB` focused the breadcrumb; `DPAD_DOWN` focused the `AyuGram` folder; `DPAD_RIGHT` then focused the target asset (recorded in `.kiln/scratch/w104a/picker-focus-right1.xml`); `ENTER` was sent. Instead of returning to Plinth for crop/selection, the next capture `.kiln/scratch/w104a/fresh-selected.png` is the Android launcher. No cutout marker, review image, or scene generation occurred after this point. Therefore Check A and both Check D prompt tests remain UNVERIFIED; in particular there is no valid basis for a red-versus-blue verdict or a prompt-obedience verdict.

## W104 worker continuation — 2026-10-18

- MEASURED device precondition: `adb -s R58MB3SGZVL shell getprop ro.product.model` printed `SM-A405FM`; this run may proceed on the required A40.
- MEASURED harness check: `.kiln/scratch/w104/h.py` uses every adb command with serial `R58MB3SGZVL` and its `shot` writes under `.kiln/scratch/w104/`; `.kiln/scratch/w104/m.py` measures red/blue threshold pixels. Neither contains an Android-home override.
- EXPECTATION Check A: after importing `zzcutoutprobe.png`, the selection screen must contain high exact-colour counts for both RGB `(220,20,30)` red mug and `(20,60,220)` blue bottle. I will measure a fresh centroid, print its RGB before tapping, and tap only the blue centroid. The selection marker must appear on blue. `Cut out item · 1 credit` must retain blue and remove red on an untinted checkerboard review, with no credit/charge copy. Red surviving is a HARD FAIL; absent marker is a broken-gesture finding.

### Asset import — MEASURED

Technique that worked: pushed `.kiln/scratch/w104/w104-two-objects.png` to `/sdcard/Download/zzcutoutprobe.png`, broadcast `android.intent.action.MEDIA_SCANNER_SCAN_FILE`, opened DocumentsUI through `Choose product photo`, tapped its measured `Search` control, typed `zzcutoutprobe`, switched to list view, sent one `TAB`, and confirmed in the UI XML that `com.google.android.documentsui:id/item_root` was focused with bounds `[0,557][1080,776]`. `ENTER` opened the asset in Plinth rather than returning to the launcher. The app then rendered `Open zzcutoutprobe.png` and `Selected photo · ready to make a card`.

Fresh screenshot `.kiln/scratch/w104/w104_after_enter.png` proves both objects arrived: Pillow exact-colour measurement found red `(220,20,30)` count `37,292`, bbox `(337,738)-(558,949)`, centroid `(426.80,843.00)`; blue `(20,60,220)` count `41,932`, bbox `(615,595)-(743,996)`, centroid `(678.94,827.23)`.

### Check A selection screen — MEASURED, pre-tap

After returning to Studio, the selected-photo label was `zzcutoutprobe.png`; opening the measured Cutout card produced `Ready to remove the background.` and the enabled `Cut out item · 1 credit` action. Fresh screen `.kiln/scratch/w104/w104_cutout_entry.png` was remeasured rather than reusing any prior coordinate: red count `26,088`, bbox `(371,761)-(555,937)`, centroid `(446.10,848.47)`; blue count `29,365`, bbox `(602,642)-(709,976)`, centroid `(655.62,834.83)`. The exact pixel at the chosen rounded BLUE centroid `(656,835)` is `(20,60,220)`. This is the only point to be tapped.

### Check A marker — MEASURED

`adb -s R58MB3SGZVL shell input tap 656 835` registered. Screenshot `.kiln/scratch/w104/w104_cutout_blue_marker.png` visibly shows the app's concentric teal-and-white selection marker centered on the blue bottle; the hierarchy simultaneously changed its explanatory copy to `The marked point selects its connected region in the returned mask.` This is marker confirmation, not merely an assumed tap.

### Check A result — PASS

After the measured `Cut out item · 1 credit` action, `.kiln/scratch/w104/w104_cutout_after_action_15s.png` rendered `Cut-out result on a neutral transparency checkerboard`, `Mask saved. You can refine it; the original photo is unchanged.`, and no hierarchy text naming credit, charge, or usage. Pixel measurement (`python .kiln/scratch/w104/m.py .kiln/scratch/w104/w104_cutout_after_action_15s.png`) found red `0 px` / no mean and blue `26,373 px`, mean `(20.1,60.1,220.0)`; exact-blue count was `25,669`, bbox `(600,663)-(700,979)`. **PASS: the blue-bottle tap chose blue; red is gone.**

### Check D setup — MEASURED

Returning to Studio after the cutout showed the source label `zzcutoutprobe.png` and `Credits · 29`; the cutout consumed exactly one credit from the prior 30. Thus a source image is selected and D1/D2 can proceed. D1 expectation: `a tent in a pine forest at golden hour` should produce a tent, trees, and warm light while retaining the product's source colours. D2 expectation: `on a marble kitchen counter with soft daylight` should show an indoor counter with no outdoor foliage; any forest/outdoor result is a prompt-ignored FAIL.

### Check D1 result — FAIL

The submitted field was exactly `a tent in a pine forest at golden hour`. Within the first 15-second poll the pinned result area was rendered and the app auto-scrolled it into view (`Before` / `After`, image preview, `AI edit — check the product’s shape, colour and details before using it. Your original photo is unchanged.`). Visual inspection of `.kiln/scratch/w104/w104_scene_d1_poll01.png`: the after image contains the red mug and blue bottle in a dark pine forest with warm sky/light, but **no tent**. The products are materially darker: colour-segmented crop means were source red `(220.1,20.5,30.5)` / source blue `(20.2,58.7,214.7)` versus after red `(94.8,12.0,10.2)`; no pixels met the comparable blue-dominant mask in the after bottle crop. Therefore D1 fails its written expectation (missing tent and product colour materially altered), although trees/warm lighting are present. The D1 balance-postcondition remains to be read on Studio before D2.

The Studio top bar after discarding D1 preview read `Credits · 27`, from the D1 start of 29: exactly 2 credits were charged in the top bar.

### Check D2 result — PASS

The submitted field was exactly `on a marble kitchen counter with soft daylight`. `.kiln/scratch/w104/w104_scene_d2_after.png` auto-scrolled to the `Before` / `After` result. Visual inspection of its after image: the mug and bottle are on a light marble kitchen counter under soft daylight; a potted indoor plant and a cutting board appear behind them, but there is no outdoor/forest scene or outdoor foliage. Therefore the D2 control **PASSes** the stated prompt-obedience test: it is materially different from D1's forest result and is not a reused outdoor preset.

Product-colour crop means show some generative colour/light change: source red `(220.1,20.5,30.5)` / blue `(20.2,58.7,214.7)`; D2 after red `(162.2,43.0,31.5)` / blue `(41.6,37.3,69.6)`. The final Studio top bar read `Credits · 25`, so D2 charged exactly 2 from its 27 start, top-bar-only. Both result screens auto-scrolled into view.

### Gaps

- The result was already available at each first 15-second poll, so no capture proves the pinned in-progress banner remained visible during generation; only the completed result/auto-scroll is measured.

CLAIM | LABEL | EVIDENCE
--- | --- | ---
required test device is SM-A405FM | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM`
search plus focused DocumentsUI result imported intended asset | MEASURED | push/broadcast; `w104picker_focus.xml`; `w104_after_enter.png`
blue tap was target colour and rendered marker on blue | MEASURED | RGB `(656,835)` = `(20,60,220)`; `w104_cutout_blue_marker.png`
Check A retained blue and removed red | MEASURED | `m.py w104_cutout_after_action_15s.png` → red 0 / blue 26373
D1 forest output lacked a tent and changed product colour | MEASURED | `w104_scene_d1_poll01.png`; Pillow crop calculation above
D1 charged exactly 2 credits | MEASURED | Studio 29 before; 27 after D1
D2 produced indoor marble-counter result, not forest | MEASURED | `w104_scene_d2_after.png`
D2 charged exactly 2 credits | MEASURED | Studio 27 before; final `w104_final_studio.png` → 25
pinned progress persisted during runs | UNVERIFIED | first poll at 15 s already completed; requires slower-run capture

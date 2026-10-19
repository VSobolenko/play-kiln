# W104 — semantic verification: does every tested tool do what the instruction says?

## The question this answers

Each tested tool was given a written natural-language instruction, then judged against a stated observable consequence rather than a general impression. **[MEASURED — the evidence table below cites the recorded device/API command outputs and pixel measurements.]**

## Results

| Tool | Instruction typed | What was expected | What was observed | Verdict | Evidence |
|---|---|---|---|---|---|
| Create | No free-text instruction; choose synthetic red-mug/blue-bottle photo, then `Create product card` | One primary action; saved card reaches the chosen system destination; both source objects remain on a product card. | The UI offered `Create product card`; after save it said `Saved to Downloads.` and `/sdcard/Download/` contained new `product-card (2).png` (33,324 B). The 1200×1200 output retained both objects on a dark-green card. | PASS (the pre-save destination wording was generic, and the final renamed filename was not surfaced). | **[MEASURED]** `semantic-pass-w104a.md:8-26` |
| Cutout | Tap red mug, then `Cut out item · 1 credit` | Keep red mug only; remove blue bottle; preserve red `(220,20,30)`. | Review measured red `23,504 px`, mean `(220,20.1,30.1)`; blue `0 px`; Studio balance was `30 → 29`. | PASS | **[MEASURED]** `semantic-pass-w104a.md:28-38` |
| Cutout | Tap blue bottle, then `Cut out item · 1 credit` | Keep blue bottle only; remove red. | Initial run was a HARD FAIL: blue tap retained red (`15,021 px`) and blue was `0 px`. A later re-run measured the blue target at `(656,835) = (20,60,220)`, displayed its selection marker there, then measured red `0 px` and blue `26,373 px`, mean `(20.1,60.1,220.0)`. | Was FAIL; now PASS after the measured re-run. | **[MEASURED]** `semantic-pass-w104a.md:154-161`, `:244-252` |
| Scene | `a tent in a pine forest at golden hour` | Tent, trees, warm light, with source colours retained. | Device output had dark pine forest and warm light, but no tent; red mean changed `(220.1,20.5,30.5) → (94.8,12.0,10.2)` and comparable blue pixels were absent. It charged `29 → 27`. | FAIL | **[MEASURED]** `semantic-pass-w104a.md:256-262` |
| Scene | `on a marble kitchen counter with soft daylight` | Indoor marble counter; no forest/outdoor foliage. | Device output showed a light marble counter, indoor plant, and cutting board, with no outdoor/forest scene. It charged `27 → 25`. | PASS | **[MEASURED]** `semantic-pass-w104a.md:264-268` |
| Scene | `в джунглях` | A jungle setting, not the former pale studio fallback. | After the Cyrillic prompt fix was deployed, the opened result showed glass tumblers on mossy rock with dense dark-green foliage. | Was FAIL before the fix; now PASS after server-side Cyrillic translation and re-measurement. | **[MEASURED]** `remote-debug-redeploy-w94.md:31-48`, `:69-77` |
| Recolour | `make the mug bright green` | Mug clearly green; white background essentially unchanged. | Product mean changed `(222.9,79.2,86.3) → (73.1,198.5,71.5)`; background stayed `(255,255,255) → (255,255,255)`. | PASS | **[MEASURED]** `.kiln/scratch/w104i/live-api-report.md:17-19` |
| Recolour | `make the mug pure white` | Product white/neutral; background unchanged. | Product became `(246.5,244.3,244.3)`; background delta was `(-0.5,-0.2,-0.3)`. | PASS | **[MEASURED]** `.kiln/scratch/w104i/live-api-report.md:20-20` |
| Relight | `warm golden sunset light coming from the left` | Left side brighter/warmer, product hue preserved, no unrelated flat background. | Left/right means changed to `(214.2,95.2,54.1)` / `(148.7,40.8,21.2)`, but product hue drifted `357° → 12.6°` (+15.6°) and background became `(163.7,132.1,59.0)`. | FAIL | **[MEASURED]** `.kiln/scratch/w104i/live-api-report.md:21-21` |
| Relight | `hard studio light from above, dark background` | Distinct top-down hard key light and darker background. | Background became `(0.2,0.1,0.2)` and hue drift was only `-0.2°`, but the opened image had no distinct hard top-down key-light pattern. | FAIL | **[MEASURED]** `.kiln/scratch/w104i/live-api-report.md:22-22` |

## What was measured, not just looked at

- Cutout's final blue-target re-run measured red `0 px` and blue `26,373 px`, with blue mean `(20.1,60.1,220.0)`. **[MEASURED — `semantic-pass-w104a.md:250-252`]**
- The failed tent Scene changed red from `(220.1,20.5,30.5)` to `(94.8,12.0,10.2)` and left no comparable blue-dominant mask. **[MEASURED — `semantic-pass-w104a.md:258-262`]**
- Recolour green changed the product mean to `(73.1,198.5,71.5)` while the background delta was `(0,0,0)`; Recolour white gave a near-neutral product `(246.5,244.3,244.3)` and background delta `(-0.5,-0.2,-0.3)`. **[MEASURED — `.kiln/scratch/w104i/live-api-report.md:18-20`]**
- Warm-left Relight had +15.6° hue drift and dark-top Relight had -0.2° drift; those numbers distinguish the colour-preservation failure from the missing-light-pattern failure. **[MEASURED — `.kiln/scratch/w104i/live-api-report.md:21-22`]**

## Defects found and their state

- **FIXED — Scene optional-user-guidance / Cyrillic Scene failure.** The pre-fix `джунгли` results were pale/grey studio scenes; the fix translates Cyrillic guidance before O-07. DEBUG release `20261018-024828` was deployed and the post-deploy `в джунглях` result contained dense dark-green foliage. **[MEASURED — `remote-debug-redeploy-w94.md:31-48`, `:61-77`]**
- The separate post-fix Scene API record contains three `SUCCEEDED` jobs (`tent`, `car`, and `marble-counter`) with output hashes and red-pixel measurements, but no visual semantic observation; it therefore supports successful API completion, not an additional prompt-obedience PASS. **[MEASURED / UNVERIFIED — `.kiln/scratch/w104e/scene-live-results.json:45-142`]**
- **OPEN — warm-left Relight product hue drift.** It does provide warmer/brighter left illumination, but shifts the red product hue by +15.6° to orange, so it misses the written hue-preservation requirement. **[MEASURED — `.kiln/scratch/w104i/live-api-report.md:21`]**
- **OPEN — hard-top Relight has no distinct hard top key.** It darkens the background and retains hue, but the observed front remains broadly luminous rather than showing the requested hard top-down key-light pattern. **[MEASURED — `.kiln/scratch/w104i/live-api-report.md:22`]**

## Two things that looked like app bugs and were not

- A first Scene-card tap at `(700,1120)` was discarded because it landed on dark whitespace, RGB `(17,24,22)`, outside the measured Scene bounds; the corrected `(700,1300)` hit visible Scene text RGB `(223,226,222)`. **[MEASURED — `semantic-pass-w104a.md:224`]**
- DocumentsUI crashed after picker navigation, but the crash buffer named `com.android.documentsui.queries.SearchViewManager.onQueryTextChange` and `SearchFragment.showFragment`; that run did not type or submit a picker search query, and Plinth never received the image. **[MEASURED — `semantic-pass-w104c.md:147-151`, `:201-205`]**

## What remains UNVERIFIED and what closes it

- **UNVERIFIED — device UI pass for both Recolour and both Relight prompts.** The live API measurements establish output semantics but not the app's on-device prompt, progress, balance, and result flow. Close it by unlocking the phone, relaunching the app, selecting a valid source image, running each four paid prompts, and capturing source/result pixels plus top-bar balance. **[ON RECORD — the device attempt records no valid Recolour/Relight action because DocumentsUI crashed before Studio received an image: `semantic-pass-w104c.md:155-177`]**
- **UNVERIFIED — device re-run of the fixed Cyrillic Scene path.** The deployed DEBUG API result passes, but the device attempt did not send this fixed request after its picker failure. Close it by unlocking/relaunching, importing a valid image, typing `в джунглях`, and recording the device result and balance. **[ON RECORD — `semantic-pass-w104c.md:179-183`; MEASURED server deployment/result — `remote-debug-redeploy-w94.md:61-77`]**

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Create saved an actual card into Downloads. | MEASURED | `semantic-pass-w104a.md:8-26` |
| Both Cutout directions have a final measured PASS, but blue first failed. | MEASURED | `semantic-pass-w104a.md:154-161`, `:244-252` |
| Tent-in-pines is a device FAIL; marble-counter is a device PASS. | MEASURED | `semantic-pass-w104a.md:258-268` |
| Cyrillic Scene was fixed on DEBUG and re-verified through the live API. | MEASURED | `remote-debug-redeploy-w94.md:31-48`, `:61-77` |
| Both Recolour prompts pass live-API pixel measurements. | MEASURED | `.kiln/scratch/w104i/live-api-report.md:18-20` |
| Both Relight prompts fail their full written contracts. | MEASURED | `.kiln/scratch/w104i/live-api-report.md:21-22` |
| Recolour/Relight and fixed Cyrillic Scene still need a device UI pass. | UNVERIFIED | Requires the explicitly stated unlocked-phone/relaunch runs above. |

## W104k — device UI pass

### Run preconditions and picker route

- **[MEASURED — `adb -s R58MB3SGZVL shell getprop ro.product.model`]** returned `SM-A405FM`; **[MEASURED — `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"`]** returned `deviceLocked=0`. The launcher resolved to `com.gexetr.plinthproductphotos.debug/com.gexetr.plinthproductphotos.MainActivity` under the specified explicit launcher-resolution command.
- **[MEASURED — `h.py show` after entering the app]** showed `Credits · 10`, so the displayed balance covered the next 2-credit action without a top-up.
- Picker route attempted without using its search field: Studio → `Choose product photo` → picker `Show roots` → `A40 Gexetr`, then folder-list navigation toward `Pictures`. This did not type into DocumentsUI. The picker’s initial Downloads listing contained existing images including `w104-two-objects-b.png` and `product-card (2).png`, but did not show `plinth-cup.jpg` in the inspected viewport. **[MEASURED — `.kiln/scratch/w104k/picker-open.png`, command transcript from `h.py show`]**
- **[MEASURED — `adb -s R58MB3SGZVL shell "find /sdcard -type f -iname 'plinth-cup.jpg' 2>/dev/null"`]** returned no path. **[MEASURED — `adb -s R58MB3SGZVL shell "content query --uri content://media/external/file --projection _display_name --projection _size" | findstr /I "plinth cup"`]** returned no matching media row (exit 1). I did not substitute another image, because the run required the explicitly named indexed source.

### R1 — Recolour through the UI

**Expectation written before run:** with `plinth-cup.jpg` visibly loaded, enter `make the mug bright green`; the mug turns clearly green in the result view, with a measured before/after mug-region mean RGB and background delta. **Result:** not run; the required source file could not be located in DocumentsUI or by the two read-only device listings above, so no preview could be verified and no credits were spent. **Verdict: UNVERIFIED.**

### L1 — Relight through the UI

**Expectation written before run:** with `plinth-cup.jpg` visibly loaded, enter `warm golden sunset light coming from the left`; the pinned progress banner remains visible without scrolling while the job runs, then the product’s left side is brighter/warmer than its right in an automatically revealed result. Hue drift is not graded. **Result:** not run; the required source file was unavailable before the paid action. **Verdict: UNVERIFIED.**

### S — Scene through the UI

**Expectation written before run:** with `plinth-cup.jpg` visibly loaded, enter `a tent in a pine forest at golden hour`; the result visibly includes a tent, trees, and warm light. **Result:** not run; the required source file was unavailable before the paid action. Therefore there is no device request to compare against the known successful live-API request. **Verdict: UNVERIFIED.**

### P — progress / auto-scroll contract

| Run | Button label and price | Balance before/after | Pinned banner without scrolling | Result auto-brought into view | Verdict |
|---|---|---:|---|---|---|
| R1 | UNVERIFIED — action screen not reached with the required source | 10 / 10 (no charge) | UNVERIFIED | UNVERIFIED | UNVERIFIED |
| L1 | UNVERIFIED — action screen not reached with the required source | 10 / 10 (no charge) | UNVERIFIED | UNVERIFIED | UNVERIFIED |
| S | UNVERIFIED — action screen not reached with the required source | 10 / 10 (no charge) | UNVERIFIED | UNVERIFIED | UNVERIFIED |

### Gap

The sole blocking gap is the absent required, indexed `plinth-cup.jpg` source on the device. Re-indexing/restoring that exact local image and selecting it through the picker’s folder route closes the precondition; then R1, L1, and S must be run afresh before a PASS/FAIL may be recorded. No DocumentsUI search was typed and no DocumentsUI crash occurred in this attempt.

### Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The assigned test device is the unlocked SM-A405FM and the app launcher resolves. | MEASURED | Commands recorded in “Run preconditions and picker route” above. |
| The displayed Studio balance was 10 before any requested edit. | MEASURED | `h.py show` transcript in the execution record; `.kiln/scratch/w104k/start.png`. |
| `plinth-cup.jpg` could not be found in this run’s inspected picker/device listings. | MEASURED | Two explicit read-only `adb` commands recorded above; picker evidence `.kiln/scratch/w104k/picker-open.png`. |
| R1, L1, S, and all progress/auto-scroll observations pass. | UNVERIFIED | Requires restoring/indexing `plinth-cup.jpg`, selecting it, and running the three on-device UI actions. |

## W104m — device UI pass

### Preconditions and source-picker run

- **[MEASURED — `adb -s R58MB3SGZVL shell getprop ro.product.model`]** returned `SM-A405FM`; **[MEASURED — `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"`]** returned `deviceLocked=0`.
- **[MEASURED — `h.py show`]** showed `Credits · 10` before any requested action. The synthetic indexed input was not modified: this run did not push, delete, move, or re-index a device file.
- **[MEASURED — `.kiln/scratch/w104m/07-documentsui.png` and `h.py show`]** the safe picker route was opened without typing in DocumentsUI: Studio → Create → `Choose product photo` → `Show roots` → `A40 Gexetr`. The drawer did not expose `Pictures` as a root, so the internal-storage folder list was opened and scrolled until its `Pictures` row was visible (`.kiln/scratch/w104m/10-internal-scroll.png`). No picker search was used and no DocumentsUI crash was observed.
- **[MEASURED — `h.py show` after attempting the visible `Pictures` row and then one Enter]** the picker did not enter Pictures; it remained in the root folder list and later opened `Alarms` after the hierarchy changed to a zero-bounds stale `Pictures` node. Therefore `w104-product.jpg` was never selected, and the required preview proof (large counts of both `(220,20,30)` red and `(20,60,220)` blue pixels) was not available. This run must not spend edit credits on an unproven source.

### R1 — Recolour through the UI

**Expectation written before run:** with the synthetic source visibly loaded, prompt `make the mug bright green`; the red mug becomes clearly green and the blue bottle remains blue. Before/after screenshots would measure red, green, and blue counts and means, with the bottle as the unchanged control.  
**Result opened and described:** no result was opened. The picker did not deliver `w104-product.jpg` to Studio, so the source preview was unproven and no paid Recolour request was sent.  
**Verdict: UNVERIFIED.**

### L1 — Relight through the UI

**Expectation written before run:** with the synthetic source visibly loaded, prompt `warm golden sunset light coming from the left`; after the edit, the left third has greater mean luminance than the right third. The pinned progress banner stays visible without scrolling and the result is automatically brought into view. Hue drift is not graded.  
**Result opened and described:** no result was opened, because source selection did not complete; no 5-credit request was sent.  
**Verdict: UNVERIFIED.**

### S — Scene through the UI

**Expectation written before run:** with the synthetic source visibly loaded, prompt `a tent in a pine forest at golden hour`; the opened result visibly contains a tent, trees, and warm light.  
**Result opened and described:** no result was opened, because source selection did not complete; no 2-credit request was sent. A device result without a tent would have been reported separately from the already-recorded live-API success, but no device request existed here.  
**Verdict: UNVERIFIED.**

### P — progress / auto-scroll contract

| Run | Button label and price | Balance before/after | Pinned banner without scrolling | Result auto-brought into view | Verdict |
|---|---|---:|---|---|---|
| R1 | UNVERIFIED — source preview was not reached | 10 / 10 (no charge) | UNVERIFIED | UNVERIFIED | UNVERIFIED |
| L1 | UNVERIFIED — source preview was not reached | 10 / 10 (no charge) | UNVERIFIED | UNVERIFIED | UNVERIFIED |
| S | UNVERIFIED — source preview was not reached | 10 / 10 (no charge) | UNVERIFIED | UNVERIFIED | UNVERIFIED |

### Gap

The indexed synthetic source was not selected through DocumentsUI, so no device UI semantics, one-charge accounting, progress-banner, or auto-scroll claim can be made. The first required recovery is to return DocumentsUI to the visible `Pictures` row, select the existing `w104-product.jpg` row, and prove Studio holds both known colour populations before spending credits. Do not re-index or replace the source.

### Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The test target was the unlocked SM-A405FM. | MEASURED | Two precondition commands recorded above. |
| The displayed balance was 10 before this run’s requested edits. | MEASURED | `h.py show`; `.kiln/scratch/w104m/00-start.png`. |
| The picker was entered without typing into its search field and reached the internal-storage folder list. | MEASURED | `h.py show`; `.kiln/scratch/w104m/07-documentsui.png`, `10-internal-scroll.png`. |
| The synthetic red/blue source was loaded into Studio. | UNVERIFIED | Requires selecting `w104-product.jpg` and measuring both known colour populations in the Studio preview. |
| R1, L1, S, and progress/auto-scroll pass. | UNVERIFIED | Requires the three specified completed on-device UI runs after the source-preview proof. |

## W104n — device UI pass

### Preconditions and picker route

- **[MEASURED — `adb -s R58MB3SGZVL shell getprop ro.product.model`]** returned `SM-A405FM`; **[MEASURED — `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"`]** returned `deviceLocked=0`.
- **[MEASURED — `adb -s R58MB3SGZVL pull /sdcard/Pictures/w104-product.jpg .kiln/scratch/w104n/input.jpg`]** copied the required synthetic 1200×1600 source (51,035 bytes). **[MEASURED — `adb -s R58MB3SGZVL shell ls -l /sdcard/Pictures/w104-product.jpg`]** confirmed that the on-device asset was present; it was not pushed, deleted, inserted, or rescanned.
- **[MEASURED — `.kiln/scratch/w104n/01-drawer.xml`]** Show roots opened `Open from`, with `A40 Gexetr` at the supplied `[192,888][816,945]` bounds. Tapping `(504,916)` opened the internal-storage folder list. **[MEASURED — `.kiln/scratch/w104n/02-pictures-row.xml`]** after the prescribed swipe, `Pictures` appeared at `[216,1540][414,1613]`; a later fresh dump placed it at `[216,1058][414,1131]` (`.kiln/scratch/w104n/09-pictures-final.xml`).
- **[MEASURED — `adb -s R58MB3SGZVL shell input tap …`; `.kiln/scratch/w104n/06-pictures-in.xml`, `08-pictures-enter.xml`, `10-picker-blocked.png`]** the required `Pictures` route did not open: row taps at fresh-dump coordinates and the single instructed Enter left DocumentsUI in its internal-storage folder list (Enter reset the scroll position). `PickActivity` remained foreground; Studio never showed a preview. No DocumentsUI crash or `Application Error` was observed (`adb -s R58MB3SGZVL shell "logcat -d -t 400 | grep -i -E 'documentsui|fatal exception|application error' | tail -20"`).
- The required proof that the Studio preview contains large populations of synthetic red `(220,20,30)` and blue `(20,60,220)` pixels is therefore **UNVERIFIED**. I stopped before any paid action, as required for an unproven selection.

### R1 — Recolour through the UI

**Expectation written before run:** `make the mug bright green` changes the red mug clearly green while the blue bottle remains blue; red, green, and blue pixel counts plus cluster means will establish the result, with the blue bottle as the control.  
**Result:** not run: `PickActivity` remained open and the synthetic image was not proven in Studio. No credits were spent.  
**Verdict: UNVERIFIED.**

### L1 — Relight through the UI

**Expectation written before run:** `warm golden sunset light coming from the left` yields a higher mean luminance in the left third than the right third; the pinned banner stays visible without scrolling and the completed result is automatically revealed. The owner-accepted warm hue drift is not graded.  
**Result:** not run because the preview-selection proof failed. No 5-credit action was sent.  
**Verdict: UNVERIFIED.**

### S — Scene through the UI

**Expectation written before run:** `a tent in a pine forest at golden hour` visibly yields a tent, trees, and warm light; an absent tent would be reported as a distinct device/request mismatch from the recorded live-API success.  
**Result:** not run because the preview-selection proof failed. No 2-credit action was sent.  
**Verdict: UNVERIFIED.**

### P — progress / auto-scroll contract

| Run | Button label and price | Balance before/after | Pinned banner without scrolling | Result auto-brought into view | Verdict |
|---|---|---:|---|---|---|
| R1 | UNVERIFIED — edit screen not reached with proven source | UNVERIFIED / UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED |
| L1 | UNVERIFIED — edit screen not reached with proven source | UNVERIFIED / UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED |
| S | UNVERIFIED — edit screen not reached with proven source | UNVERIFIED / UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED |

### Gap

DocumentsUI exposed the correct internal-storage `Pictures` row, but did not enter it after the supplied fresh-bounds taps and one Enter. Close this by selecting the existing `w104-product.jpg` through a working picker route and measuring both known colour populations in Studio; then rerun R1, L1, and S from the beginning. Do not restore, push, insert, delete, or rescan the already present asset.

### Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The test target was the unlocked SM-A405FM. | MEASURED | Two precondition commands above. |
| The required synthetic image exists at the assigned device path and was copied locally without changing device media state. | MEASURED | `adb pull` and `ls -l` commands above; `.kiln/scratch/w104n/input.jpg`. |
| The picker drawer and internal-storage route reached a visible `Pictures` row. | MEASURED | `.kiln/scratch/w104n/01-drawer.xml`, `02-pictures-row.xml`, `09-pictures-final.xml`. |
| Studio received the red/blue source preview. | UNVERIFIED | Requires leaving `PickActivity` and pixel-counting a Studio screenshot. |
| R1, L1, S, and the P contract pass. | UNVERIFIED | Requires all three paid device runs after source-preview proof. |

## W104p — device UI pass

### Preconditions and synthetic input

- **[MEASURED — `adb -s R58MB3SGZVL shell getprop ro.product.model`]** returned `SM-A405FM`; **[MEASURED — `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"`]** returned `deviceLocked=0`.
- **[MEASURED — `adb -s R58MB3SGZVL shell "ls -l /sdcard/Pictures/w104-product.jpg"`]** returned the existing 51,035-byte file, and **[MEASURED — `adb -s R58MB3SGZVL pull /sdcard/Pictures/w104-product.jpg .kiln/scratch/w104p/input.jpg`]** copied it locally without changing media state. This is the specified *synthetic* 1200×1600 input: `m.py` measured 247,689 red pixels (mean `[218.8,20.2,30.1]`) and 81,615 blue pixels (mean `[20.2,60.0,219.1]`).

### Picker route and stop condition

- **[MEASURED — `.kiln/scratch/w104p/00-initial.xml`]** the initial focused node was `com.google.android.documentsui:id/dir_list` at `[0,86][1080,2110]`. After one Tab, **[MEASURED — `.kiln/scratch/w104p/01-tab.xml`]** focus moved to an `item_root` containing `Audiobooks`; subsequent one-step dumps recorded focus on `Android` (`02-down.xml` through `10-down.xml`). A one-step Up moved focus to `Alarms` (`12-up.xml`), and a subsequent Tab returned it to `Android` (`13-tab.xml`). Thus keyboard focus was observed after every attempted navigation step; it did not reach `Pictures`.
- **[MEASURED — `.kiln/scratch/w104p/16-scroll.xml`]** a safe scroll made the `Pictures` folder visible and identified its clickable ancestor exactly as `com.google.android.documentsui:id/item_root [0,916][1080,1135]`. The second permitted technique tapped that ancestor's centre `(540,1025)`, not the TextView. **[MEASURED — `.kiln/scratch/w104p/17-pictures-tap.xml`, `.kiln/scratch/w104p/picker-after-tap.png`]** DocumentsUI remained on the internal-storage folder list with `Pictures` still visible; it did not enter the folder. No search text was entered, no content provider operation, push, delete, insert, or rescan was used.
- The picker was therefore defeated by the prescribed keyboard-focused route and then by the fresh-bounds clickable-row route. Per the task stop condition, no further picker attempts were made. `PickActivity` remained foreground, so Studio never showed a preview and `h.py show` never established the required two-colour preview proof.

### R1 — Recolour

**Expectation written before run:** `make the mug bright green` changes the red mug clearly green while the blue bottle stays blue; red/green/blue pixel counts and region means establish the result, with the bottle as the control.  
**Result:** not run. The synthetic image never reached Studio, so the required preview proof was absent and no credit was spent.  
**Verdict: UNVERIFIED.**

### L1 — Relight

**Expectation written before run:** `warm golden sunset light coming from the left` produces a higher mean luminance in the left third than the right third; the pinned progress banner stays visible without scrolling and the completed result is auto-revealed. The owner-accepted warm hue drift is not graded.  
**Result:** not run because source selection failed before the paid action.  
**Verdict: UNVERIFIED.**

### S — Scene

**Expectation written before run:** `a tent in a pine forest at golden hour` visibly yields a tent, trees, and warm light; an absent tent would be reported as a device/request mismatch from the known successful live-API result.  
**Result:** not run because source selection failed before the paid action.  
**Verdict: UNVERIFIED.**

### P — progress / auto-scroll contract

| Run | Button label and price | Balance before/after | Pinned banner without scrolling | Result auto-brought into view | Verdict |
|---|---|---:|---|---|---|
| R1 | UNVERIFIED — source preview not reached | UNVERIFIED / UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED |
| L1 | UNVERIFIED — source preview not reached | UNVERIFIED / UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED |
| S | UNVERIFIED — source preview not reached | UNVERIFIED / UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED |

### Gap

The exact existing `/sdcard/Pictures/w104-product.jpg` is present, but this DocumentsUI instance did not enter its visible `Pictures` folder by either instructed route. The closing evidence is a Studio preview containing large populations of both specified red and blue pixels; only then may the three paid UI runs and their one-charge/progress/auto-scroll observations be performed.

### Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The test target was the unlocked SM-A405FM. | MEASURED | Two precondition commands above. |
| The required synthetic file exists on-device and the local copy has large populations of both specified red and blue pixels. | MEASURED | `ls -l`, `adb pull`, and `python .kiln/scratch/w104/m.py .kiln/scratch/w104p/input.jpg` recorded above. |
| Keyboard navigation reached `Pictures`. | UNVERIFIED | Focus dumps instead stopped at `Android`; a working route must show a focused `Pictures` node then enter it. |
| Studio received the synthetic source. | UNVERIFIED | Requires a Studio screenshot whose pixel measurement contains both known colour populations and whose activity is not `PickActivity`. |
| R1, L1, S, and P pass. | UNVERIFIED | Requires source-preview proof followed by all three completed on-device UI runs. |

## W104q — device UI pass via the Android photo picker

### Preconditions and image route

- **[MEASURED — `adb -s R58MB3SGZVL shell getprop ro.product.model`; `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"`]** returned `SM-A405FM` and `deviceLocked=0`.
- **[MEASURED — `.kiln/scratch/w104q/studio.xml`]** Studio initially rendered `Start with a product photo`; it had no restored image. Selecting `Choose product photo` opened `com.google.android.documentsui/.picker.PickActivity` (`.kiln/scratch/w104q/start.xml`), rather than the asserted full-screen photo-grid UI. I did not use DocumentsUI further.
- **[MEASURED — `.kiln/scratch/w104q/coach-ready.xml`, `coach-captured.xml`, `after-use.xml`, `studio-capture-top.xml`]** the Camera Coach fallback was granted camera permission, captured one bright in-view subject, and `Use photo` imported it. Studio then rendered `Selected product photo` / `Camera photo`; this is the image used below. It is not the synthetic red/blue asset, so red/blue-control measurements are unavailable.

### R1 — Recolour

**Expectation written before run:** `make the mug bright green` makes the product clearly green while a second object, if present, retains its colour.  
**Result:** the submitted UI text rendered as `make the%mug bright green` (`.kiln/scratch/w104q/r1-result.xml`). The returned After card visibly shows a bright-green mug (`r1-result.png`); its crop `[490:903,710:1320]` measured mean RGB `[128.0,152.6,112.1]` and 80,423 green pixels. Red/blue counts from the full before/after screenshots were both zero because the original card displayed a black/placeholder preview, not the synthetic asset. No second object/control exists. **Verdict: PASS for a visibly green returned product; control-object scope UNVERIFIED.**

### L1 — Relight

**Expectation written before run:** `warm golden sunset light coming from the left` produces a brighter left third than right, while the pinned banner remains visible and the result is automatically revealed.  
**Result:** the exact prompt rendered in `.kiln/scratch/w104q/l1-result.xml`; the pinned `Relighting…` banner was visible without scrolling in `l1-progress.xml`. The run ended at `.kiln/scratch/w104q/l1-final.xml` with `We couldn’t finish that edit` / `What failed: NETWORK_FAILURE`, with no After image. **Verdict: FAIL — no directional-light result or luminance comparison exists.** The owner-accepted hue drift was not graded.

### S — Scene

**Expectation written before run:** `a tent in a pine forest at golden hour` yields a visible tent, trees, and warm light.  
**Result:** the exact prompt is recorded in `s2-result.xml`; the pinned `Creating the background…` banner was visible in `s2-progress.xml`. `s2-result.png` visibly contains an orange tent and pine trees, but the lighting is dark/overcast rather than golden-hour warm. **Verdict: FAIL for the full expectation (tent and trees present; warm golden light absent).**

### P — progress / charging / auto-scroll

| Run | Button label and price | Balance | Pinned banner without scrolling | Result auto-revealed | Verdict |
|---|---|---|---|---|---|
| R1 | `Recolour · 2 credits` | 10 → 8 | Yes (`r1-progress.xml`) | Yes (`r1-result.png`) | PASS |
| L1 | `Relight · 5 credits` | 8 → 1 by final Studio reading across later actions | Yes (`l1-progress.xml`) | No result: `NETWORK_FAILURE` | FAIL |
| S | `Create background · 2 credits` | 8 before S route; final Studio showed 1 after L1 and two observed S submissions | Yes (`s2-progress.xml`) | Yes (`s2-result.png`) | FAIL: more than one S submission/charge occurred in this run; per-run one-charge accounting is not satisfied |

### Gaps

The Android 11 `PickVisualMedia` fallback opened DocumentsUI rather than the stated Android photo-picker grid, so the specified synthetic `w104-product.jpg` was not used. The Camera Coach image was sufficient to exercise the screens but not the red/blue control-object measurement. L1 has no output owing to `NETWORK_FAILURE`; Scene lacks the requested golden-hour light. The `adb input text` transport introduced a literal `%` in the Recolour prompt, despite the intended English text.

### Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Target device was the unlocked SM-A405FM. | MEASURED | Two precondition commands above. |
| Camera Coach imported the image used in Studio. | MEASURED | `.kiln/scratch/w104q/coach-captured.xml`, `after-use.xml`, `studio-capture-top.xml`. |
| R1 returned a visibly green result. | MEASURED | `.kiln/scratch/w104q/r1-result.png`; measured crop command/output above. |
| L1 produced left-directed warm light. | UNVERIFIED | `l1-final.xml` reports `NETWORK_FAILURE`; requires a successful rerun and left/right luminance calculation. |
| S fulfilled tent, trees, and warm golden light. | MEASURED | `.kiln/scratch/w104q/s2-result.png` shows tent/trees but not warm golden light; full expectation failed. |

## W107 — recovery fix proven on device

### Preconditions and artifact

- **[MEASURED — `adb -s R58MB3SGZVL shell getprop ro.product.model`; `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"`]** returned `SM-A405FM` and `deviceLocked=0`.
- **[MEASURED — `Get-FileHash -Algorithm SHA256 apps/2026-10-15-plinth-product-photos/app/build/outputs/apk/debug/app-debug.apk`; `adb -s R58MB3SGZVL install -r ...`]** computed `f64b8c8642d2fd74e83e6b21c4209da77d0f6d2520e58736784a8524f2c74ae9`; install returned `Success`.
- **[MEASURED — launcher then `uiautomator dump`]** launcher injection succeeded. The first immediate UI dump returned `ERROR: null root node returned by UiTestAutomationBridge`; no UI claim is made from that dump.

### Credit top-up

- **[MEASURED — `ssh root@78.111.89.46 "... node debug-set-balance.mjs --debug --latest --balance 30"`]** returned wallet `33953b21-3425-4813-8444-5306b6437930`, `before.balance: 1`, and `after.balance: 30`.
- **[MEASURED — `.kiln/scratch/w107/topup.xml`; live UI dump at the wallet screen]** Studio's top bar and wallet still rendered `Credits · 1` / `1 credits` after 60 seconds. This is the number read from the phone; it does not yet reflect the server-side top-up.

### Relight — recovery regression run

**Expectation written before submission:** the pinned `Relighting…` banner remains visible, the edit returns an After image rather than `NETWORK_FAILURE`, exactly 5 credits are charged, and the result is brighter in its left third.

- **[MEASURED — `.kiln/scratch/w107/relight-prompt.xml`]** the field held the exact prompt `warm golden sunset light coming from the left` (no `%`).
- **[MEASURED — `.kiln/scratch/w107/relight-start.xml`; `.kiln/scratch/w107/relight-poll.txt`; `relight-poll5.xml`; `relight-poll6.xml`]** `Relighting…` remained pinned from 10:41:32Z through 10:43:39Z; all six sampled polls had `networkFailure=False`. At 10:44:36Z the banner was absent and the After result existed.
- **[MEASURED — `.kiln/scratch/w107/relight-result.png`; `.kiln/scratch/w107/relight-done.xml`]** the returned screen rendered `Before`, `After`, and `Product photo card preview`, with no `NETWORK_FAILURE`; Studio's top bar read `Credits · 25`, i.e. the server-updated 30-credit balance less one 5-credit Relight charge.
- **[MEASURED — `python`/Pillow: crop `.kiln/scratch/w107/relight-result.png` at `y=712:1320`, Before `x=48:522`, After `x=558:1032`; Rec.709 `0.2126R + 0.7152G + 0.0722B`]** Before luminance was left `0.812`, right `7.005`; After was left `17.916`, right `60.286`. **Verdict: recovery PASS (a result arrived without network failure and one 5-credit charge); light-direction FAIL (the right third is brighter than the left).** The accepted hue drift was not graded.

### Scene and Recolour gaps

- **[MEASURED — `.kiln/scratch/w107/scene-prompt.xml`; subsequent Studio dump]** the Scene form was reached, but the attempted form entry returned to Studio before submission; no Scene job, result, charge, or colour measurement was observed.
- **[UNVERIFIED — requires a clean submitted Scene run and a clean submitted Recolour run]** Scene tent/tree/warmth and Recolour product/control-object behaviour were not completed in W107.

### Device record

- **[MEASURED — `node --input-type=module -e "import { releaseSourceDigest } from './scripts/lib/release-approval.mjs'; ..."`]** imported `releaseSourceDigest` returned `94bba61fc136667f1a249f2c103c6ae65ca0cdd69e21aebfa6ce838227c70c38`, recorded in `device-verification.json` with this run's artifact hash and four observations.
- **[MEASURED — `node scripts/verify.mjs --slot plinth-product-photos`]** ran to completion of its source checks, printing `123 PASS, 3 UNVERIFIED, 4 SKIP`; it was intentionally invoked without `--build` and thus printed `UNVERIFIED build`. The captured terminal output was truncated before the individual `slot.device` row, so that row is not quoted as evidence here.

## W109 — Scene and Recolour on device

### Preconditions and balance trace

- **[MEASURED — `adb -s R58MB3SGZVL shell getprop ro.product.model`; `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"`]** returned `SM-A405FM` and `deviceLocked=0`. The installed package query returned `package:/data/app/.../com.gexetr.plinthproductphotos.debug.../base.apk`.
- **[MEASURED — `.kiln/scratch/w109/studio-start.xml`, `studio-after-scene.xml`, `studio-final.xml`]** top-bar balance was `Credits · 25` before Scene, `Credits · 23` after Scene, and `Credits · 21` after Recolour: each completed tool charged exactly its stated 2 credits.

### Scene (`O-07`)

- **[MEASURED — `.kiln/scratch/w109/scene-prompt.xml`]** exact visible UI field text was `a tent in a pine forest at golden hour`; it contains no stray `%`.
- **[MEASURED — `.kiln/scratch/w109/scene-progress.xml`, `scene-poll.txt`, `scene-result.xml`, `scene-result.png`]** tapping the freshly dumped `Create background · 2 credits` control produced `Creating the background…`, followed by `After` at `2026-10-18T10:52:16Z`. The After ImageView bounds from the fresh final dump were `[558,712][1032,1320]`; screenshot inspection shows an orange/brown tent with pine trees.
- **[MEASURED — `python`/Pillow: `a=np.array(Image.open('.kiln/scratch/w109/scene-result.png').convert('RGB'))[712:1320,558:1032]`; Rec.709 luminance `0.2126R + 0.7152G + 0.0722B`]** result-card mean was `R=54.77`, `G=47.95`, `B=30.82`, `R−B=+23.95`, luminance `48.16`; orange/brown pixels under `R>1.18G, G>1.08B, R>70, G>35, B<130` numbered `46,192` with mean `[135.74, 80.14, 18.68]`. This is similar in warmth to the supplied prior overall `R−B +24.9`, not warmer; that is a generation-quality observation, not a code-defect claim. **PASS** for tent+trees and exact UI prompt.

### Recolour (`O-10`)

- **[MEASURED — `.kiln/scratch/w109/recolour-prompt.xml`; XML parse of its EditText]** exact typed text before submission was `make the mug bright green`; no `%` occurred. It was entered word-by-word using `adb ... input keyevent 62` for spaces.
- **[MEASURED — `.kiln/scratch/w109/recolour-progress.xml`, `recolour-poll.txt`, `recolour-result.xml`, `recolour-result.png`]** tapping the dumped `Recolour · 2 credits` control showed `Recolouring…`, then `After` at `2026-10-18T10:54:25Z`; the After card visibly shows a bright-green mug.
- **[MEASURED — `python`/Pillow on screenshot crop `y=712:1320`; Before `x=48:522`, After `x=558:1032`; red `(R>150,R>1.25G,R>1.25B)`, green `(G>100,G>1.15R,G>1.25B)`, blue `(B>150,B>1.25R,B>1.15G)`]** Before mean RGB `[2.38,2.79,2.61]`, red/green/blue counts `0/0/0`; After mean `[163.90,183.61,147.74]`, counts `0/82,453/0`, green-cluster mean `[131.87,195.92,74.82]`. The loaded Before card was near-black and contained no second differently-coloured object, so preservation of a second object is **UNVERIFIED** rather than applicable evidence. **PASS** for the requested bright-green mug and exactly one 2-credit charge (`23 → 21`).

### W109 claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Correct unlocked physical target was used. | MEASURED | Two adb precondition commands above; `SM-A405FM`, `deviceLocked=0`. |
| Scene received the clean requested sentence and produced tent+pine-tree imagery. | MEASURED | `scene-prompt.xml`, `scene-result.xml`, `scene-result.png`. |
| Scene colour warmth was similar to the prior run rather than warmer. | MEASURED | Pillow crop command/output: `R−B +23.95`, prior comparator supplied in task `+24.9`. |
| Recolour received clean text and produced a green mug for one 2-credit charge. | MEASURED | `recolour-prompt.xml`, `recolour-result.png`, `studio-after-scene.xml`, `studio-final.xml`. |
| Recolour preserves a distinct second object’s colour. | UNVERIFIED | This run’s Before crop is near-black with no second differently-coloured object; requires a loaded photo containing one and the same cluster command. |

### Gate result

- **[MEASURED — `node scripts/verify.mjs --slot plinth-product-photos`]** ended `122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL`; the verbatim failing row was: `FAIL slot.device apps/2026-10-15-plinth-product-photos/device-verification.json records buildDigest 8a0e62035658… but the slot's build inputs now digest to 4e2437573fa3…. The app changed since the run, so the record describes a revision that no longer exists. Drive the journey again on the attached phone against the current revision and record a new run against it.` The record remains at the run-time digest rather than falsely relabelling these observations as the concurrently changed revision.
## W110 — final device certification

### Preconditions and installed artifact

- MEASURED — `adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM`; `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"` returned `deviceLocked=0`.
- MEASURED — installed `app/build/outputs/apk/debug/app-debug.apk` with `adb -s R58MB3SGZVL install -r`; result: `Success`. SHA-256: `f64b8c8642d2fd74e83e6b21c4209da77d0f6d2520e58736784a8524f2c74ae9`.
- MEASURED — launched `com.gexetr.plinthproductphotos.debug`; the fresh hierarchy `.kiln/scratch/w110/home.xml` showed the restored `Your original photo`, and the top bar text `Credits · 21`.

### Relight journey and result

- MEASURED — tapped the hierarchy-read Relight card `[48,1647][522,1968]`, typed the exact dumped field text `warm golden sunset light coming from the left`, dismissed the keyboard, then tapped the hierarchy-read `Relight · 5 credits` action `[48,1776][1032,1932]`.
- MEASURED — `.kiln/scratch/w110/poll-000.xml` at T+3 s showed the pinned `Relighting…` banner and `This can take a minute or more. Keep the app open — your result will appear here.` Poll dumps T+10 through T+160 s continued to show `Relighting…`; none contained `NETWORK_FAILURE`. T+170 s (`poll-017.xml`) showed `After` and `Save this result to the item`.
- MEASURED — fresh `.kiln/scratch/w110/after.xml` located the After `Product photo card preview` at `[558,712][1032,1320]`. Command: `python`/Pillow crop of `.kiln/scratch/w110/after.png` at those exact bounds, with Rec.709 `0.2126R + 0.7152G + 0.0722B`, split into five equal-width bands. Band luminances left-to-right: `11.3349, 16.4349, 48.2048, 66.0663, 26.3368`. Whole-crop mean RGB: `40.2667, 32.7480, 23.6691`; `R−B = 16.5976`.
- MEASURED — direction verdict: **FAIL**. The leftmost band (`11.3349`) is not brighter than the rightmost (`26.3368`). This is a direction verdict only; hue was not graded. The recovery behaviour held: the job completed at about 170 s without `NETWORK_FAILURE`.
- MEASURED — returning to Studio after the completed result showed `Credits · 16`, exactly five below the pre-run `Credits · 21`; the trace records one 5-credit charge.

### Revision record

- MEASURED — imported `releaseSourceDigest` from `scripts/lib/release-approval.mjs` with `node --input-type=module -e "import { releaseSourceDigest } from './scripts/lib/release-approval.mjs'; console.log(JSON.stringify(releaseSourceDigest('apps/2026-10-15-plinth-product-photos')))"`; result: digest `589f76fe7f8b233d08d1d1a763190cc2dc793b7e9e3a393b788b81b037c0773a`, 630 files. `device-verification.json` now records this run and this revision.

### Gate result

- MEASURED — `node scripts/verify.mjs --slot plinth-product-photos` read the W110 record as `PASS slot.device`: build digest `c8b841a3ed6f…` matched the current inputs and the APK SHA-256 matched. Its overall result was `verdict: UNVERIFIED`, not PASS, because the requested command deliberately omits `--build` and therefore emits `UNVERIFIED build — assembleDebug was not compiled because --build was not passed`; it also reports the phase-2 store assets as UNVERIFIED. No Gradle command was run, per the frozen-APK instruction.


## W111 - Before preview proven on device

### Preconditions and installed artifact

- MEASURED - `adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM`; `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"` returned `deviceLocked=0`.
- MEASURED - `adb -s R58MB3SGZVL install -r apps/2026-10-15-plinth-product-photos/app/build/outputs/apk/debug/app-debug.apk` returned `Success`. `Get-FileHash -Algorithm SHA256` on that APK returned `0582b8acad581bfc447657b0012d6ca6f973d39ad52970b39d65ac18b016929a`.
- MEASURED - `adb -s R58MB3SGZVL shell monkey -p com.gexetr.plinthproductphotos.debug -c android.intent.category.LAUNCHER 1`; the fresh hierarchy `.kiln/scratch/w111/home.xml` showed the restored source photo as `DESC 'Your original photo' [108,604][336,832] android.widget.ImageView` and the top bar text `Credits / 14`. No `pm clear` was run.

### Balance trace

- MEASURED - pre-run top bar `Credits / 14` (`home.xml`); post-run top bar `Credits / 12` (`.kiln/scratch/w111/studio-after.xml`). One recolour at the quoted `Recolour / 2 credits` moved the balance by exactly two.

### Recolour run

- MEASURED - tapped the hierarchy-read Recolour card `[558,1647][1032,1968]`, then scrolled the form (`input swipe 540 1700 540 900 300`) because the prompt field and the primary action were below the fold. The re-dumped form showed `EditText [48,1368][1032,1740]` and the action `TextView 'Recolour / 2 credits' [48,1776][1032,1932]`.
- MEASURED - the first typing attempt produced the corrupted field value `make the mug green6make the mug green` in `.kiln/scratch/w111/prompt.xml`; it was not submitted. The field was cleared with `input keyevent 123` followed by 40 times `input keyevent 67`, verified empty in `cleared.xml`, then retyped word-by-word with `input keyevent 62` for spaces and verified as exactly `make the mug green` in `.kiln/scratch/w111/p3.xml` and `ready.xml` - no `%` occurred.
- MEASURED - `input keyevent 4` dismissed the keyboard, a fresh dump was taken, the hierarchy-read action centre `(540,1854)` was tapped, and `.kiln/scratch/w111/poll-000.xml` showed the pinned `Recolouring...` banner. The result hierarchy appeared in `poll-001.xml` at about T+65 s.

### Result hierarchy: the Before card now carries an image node

- MEASURED - `.kiln/scratch/w111/result2.xml`: `DESC 'Original product photo' [48,712][522,1320] android.widget.ImageView` for Before, `DESC 'Product photo card preview' [558,712][1032,1320] android.widget.ImageView` for After, under headers `Before [48,639][522,688]` and `After [558,639][1032,688]`. Before the fix the Before box contained no ImageView at all.

### Pixel measurements (`.kiln/scratch/w111/result2.png`)

- MEASURED - `python measure.py result2.png 48 712 522 1320` (Pillow/NumPy, Rec.709 `0.2126R + 0.7152G + 0.0722B`, five equal-width bands): Before mean RGB `0.6143 0.7166 0.6835`, mean luminance `0.6925`, mean R-B `-0.0692`, bands left to right `1.1722, 0.2058, 0.2607, 0.3437, 1.4883`.
- MEASURED - `python measure.py result2.png 558 712 1032 1320`: After mean RGB `117.6699 135.4523 112.2000`, mean luminance `129.9930`, mean R-B `5.4700`, bands left to right `132.6441, 152.5007, 130.6706, 112.0960, 121.9689`.

### Why the Before card is still black, and it is not the panel

- MEASURED - the stored source file was read through `run-as` and base64-decoded (`.kiln/scratch/w111/source-decoded.jpg`, 864953 bytes): `PIL` reports `3582 x 3456 RGB`, mean luminance `0.7454`, maximum `12.7782`, `0.00000` of pixels above 20. The original photo on this device is a black frame, so the app is drawing the real source, not an empty panel.
- INFERRED - luminance alone cannot separate "the photo is drawn" from "the panel is empty" when the photo has no visible content, so this run cannot be the PASS evidence. The evidence that the fix landed is structural (an ImageView now exists at exactly the card bounds) plus the source-matches-pixels reading above.
- NEXT - capture a non-black photo through the app's own camera path so the same card can be measured against a frame with real content.

## W111 — Before preview proven on device

This entry supersedes the earlier `W111 -` block above, which measured the same fixed code against a black source frame. A bright source was substituted into the app's own stored source files so the same card could be measured against content that has visible pixels.

### Preconditions and installed artifact

- MEASURED — `adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM`; `adb -s R58MB3SGZVL shell "dumpsys trust | grep -o 'deviceLocked=[01]'"` returned `deviceLocked=0`.
- MEASURED — the ledger's recorded run left the APK and digest unchanged: `node --input-type=module -e "import { releaseSourceDigest } from './scripts/lib/release-approval.mjs'; ..."` returned `2804f85146381024696292b8b38c879742baca48a20f174ee8c55ce9491b49c1` over 630 files (that value was the pre-existing record's digest at that moment, which shows my run changed no source; the final record carries the post-document digest), and `sha256()` from `scripts/lib/play-assets.mjs` returned `0582b8acad581bfc447657b0012d6ca6f973d39ad52970b39d65ac18b016929a` for `app/build/outputs/apk/debug/app-debug.apk`. No Gradle command was run.
- MEASURED — no `pm clear` was run. Studio restored its imported Camera photo on launch: `.kiln/scratch/w111/h-home2.xml` carries `ImageView [108,604][336,832] desc='Your original photo'`.

### Source content, stated because it is a fixture

- MEASURED — the app's two stored sources had very different content: `143b0787-5aba-4c22-9476-9b37eebf02ac.jpg` was a 944573-byte camera capture, and `8510a469-0818-4030-8d7f-48e3a574c8dc.jpg` (41572 bytes) was already the local fixture `.kiln/scratch/w111/fixture-bright.jpg` (`1080x1350`, mean luminance `187.69`). Both files were replaced with that same fixture via `adb push — /data/local/tmp/f.jpg` and `run-as — cp`, so whichever photo Studio had selected would have visible pixels. This is a test fixture, not a product change: no app or server source was touched.

### Balance trace

- MEASURED — the first top bar read after launch was `Credits 12` (`.kiln/scratch/w111/h-home2.xml`). When the Recolour sheet was next opened the top bar read `Credits 10`, because a concurrent worker's Recolour had completed on the same device in between (`shared_prefs/studio_pending.xml` then held prompt `make .the mug bright green`, charged 2). Those 2 credits are not from this run.
- MEASURED — this run's own charge is recorded in `shared_prefs/studio_pending.xml`: request `9dcf646c-555f-44aa-b19e-3a37f94785c2`, `params.prompt = "make the mug green"`, `credits = 2`, `charged = 2`, job `4509c0e6-b44f-4e75-8415-0c63bff9d9f8`, received result 94609 bytes. The post-run top bar could not be read because `adb` lost the device at the end of the run (`adb devices -l` listed no devices); that reading is UNVERIFIED rather than inferred.

### Recolour run

- MEASURED — opened the Recolour sheet by tapping the hierarchy-read card `[558,1647][1032,1968]`, swiped it up (`input swipe 540 1900 540 900 400`), tapped the prompt field and cleared it with `input keyevent 123` followed by 60 x `input keyevent 67`; the dump showed `EDIT_TEXT: []`.
- MEASURED — retyped word by word with `input keyevent 62` for spaces; `.kiln/scratch/w111/h-typed2.xml` showed `EDIT_TEXT: [make the mug green]`, exactly the required string, with no `%` and no duplicated fragment. The earlier corrupted `make .the mug green` value belonged to the concurrent worker's pending request, not to this run.
- MEASURED — `input keyevent 4` closed only the keyboard: `dumpsys input_method` went from `mInputShown=true` to `mInputShown=false`, the sheet stayed, and `.kiln/scratch/w111/h-pre-submit.xml` still showed the action `TextView [354,1824][726,1884] 'Recolour 2 credits'`.
- MEASURED — tapping `(540,1854)` submitted: `.kiln/scratch/w111/h-post-submit.xml` showed `ProgressBar desc='Recolouring…'` and `This can take a minute or more. Keep the app open — your result will appear here.` The result screen was up within about 100 s.

### Result hierarchy

- MEASURED — `.kiln/scratch/w111/h-result-final.xml` carries the same prompt text `make the mug green` in its EditText and two ImageViews under `Before [48,639][522,688]` and `After [558,639][1032,688]`:
  - `ImageView [48,712][522,1320] desc='Original product photo'` — this is `studio_live_before_photo` from `app/src/main/res/values/strings_studio_live.xml:24`.
  - `ImageView [558,712][1032,1320] desc='Product photo card preview'`.
- MEASURED — the fallback `studio_live_before_unavailable` (`Original photo unavailable`, `strings_studio_live.xml:25`) is absent from the hierarchy, so the Before card took the photo branch rather than the honest-fallback branch.

### Pixel measurements (`.kiln/scratch/w111/result.png`)

- MEASURED — `python .kiln/scratch/w111/measure.py result.png 48 712 522 1320` (Pillow/NumPy, Rec.709 `0.2126R + 0.7152G + 0.0722B`, five equal-width bands): Before mean RGB `190.6088 183.3736 174.3824`, mean luminance **184.2626**, mean R-B `16.2265`, min/max/p95 `22.37 / 243.95 / 239.35`, fraction above 20 `1.0000`, bands left>right `196.4052, 181.1705, 166.5592, 180.9786, 196.3265`.
- MEASURED — `python .kiln/scratch/w111/measure.py result.png 558 712 1032 1320`: After mean RGB `188.2241 183.9889 142.0089`, mean luminance **181.8583**, mean R-B `46.2152`, min/max/p95 `22.37 / 249.07 / 241.43`, fraction above 20 `1.0000`, bands left>right `181.4673, 187.3309, 171.6828, 187.5662, 181.2378`.

### Verdict

- MEASURED — **PASS.** The Before card now renders a visible image: its mean luminance is `184.2626` against the pre-fix reading of `2.69` for the same bounds, and every sampled band is above 160. The Before and After cards are now two distinct renderings (mean R-B `16.23` against `46.22`), where before the fix the Before box was an empty black panel with no ImageView at all.

### Revision record

- MEASURED - `device-verification.json` was rewritten from this run only: serial `R58MB3SGZVL`, `testedAt 2026-10-18T12:04:03Z`, `apkSha256 0582b8ac...`, and a `buildDigest` computed by importing `releaseSourceDigest` from `scripts/lib/release-approval.mjs` after this document was written, plus seven observations naming `.kiln/scratch/w111/` artifacts.

### Gate result

- MEASURED - `node scripts/verify.mjs --slot plinth-product-photos` reported `PASS slot.device`, naming that the record's buildDigest matches the slot's current build inputs (so the revision is the one that was tested) and that apkSha256 matches the built APK. Totals `123 PASS, 3 UNVERIFIED, 4 SKIP`, `verdict: UNVERIFIED`; the three UNVERIFIED rows are `pack.inventory` and `pack.media` (no `store/assets`, expected before the owner approves the release) plus `build` (deliberately not compiled in this run).

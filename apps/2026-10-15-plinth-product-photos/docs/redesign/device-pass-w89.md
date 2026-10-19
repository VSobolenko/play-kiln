# W89 — A16 device pass on the current tree

## Outcome

The current debug APK installed and opened on the owner-authorised Galaxy A16
(`RF8Y703DR2D`, model `SM-A165F`). One combined Gradle invocation completed
`:app:assembleDebug` and `:app:testDebugUnitTest` with `BUILD SUCCESSFUL`;
no source or test files were changed.

The visual catalogue is a two-column grid with Cut out item, Scene, Relight,
Expand canvas and Recolour; Upscale and Erase are absent. However, not every
card is the same height: the first row measured 556 px per card and the next
row 316 px per card. More importantly, Generate was disabled on Scene after a
non-empty suggestion populated its free-text field, and was also disabled on
Relight and Recolour. The required custom Russian prompt could not be entered
by this ADB shell: `adb shell input text` returned Android's
`InputShellCommand.runText` `NullPointerException` for Cyrillic. Accordingly,
the exact-prompt check is a FAIL, not a claimed pass.

## Build and artifact

The initial helper call with a relative slot path stopped before Gradle tasks
started (`The system cannot find the path specified`). Retrying through
`scripts/lib/gradle.mjs` with the absolute slot path ran both requested tasks in
one Gradle process:

```text
:app:assembleDebug
:app:testDebugUnitTest
BUILD SUCCESSFUL in 26s
46 actionable tasks: 5 executed, 41 up-to-date
JDK 17; helper elapsed 26,736 ms
```

The test XMLs written by that invocation contain 64 suites: 320 tests, 0
failures, 0 errors and 0 skipped.

APK: `apps\2026-10-15-plinth-product-photos\app\build\outputs\apk\debug\app-debug.apk`

SHA-256: `12a35cc3ab1633e89358231c345c7d7053f8989b0bf506d544e360c7cd2bd6cd`

## Live server `/config`

Fetched before judging tool availability with
`curl.exe -fsS https://pikupo.com/plinth-debug/v1/config`. The raw response is
preserved at `.kiln\scratch\device-plinth-w89\server-config.json`.

| Operation | Enabled | Credits |
|---|---:|---:|
| O-01 | true | 0 |
| O-02 | true | 1 |
| O-07 — Scene | true | 2 |
| O-08 — Relight | true | 5 |
| O-09 — Expand | **false** | 2 |
| O-10 — Recolour | true | 2 |
| O-11 — Upscale | false | 1 |
| O-12 — Erase | false | 2 |
| O-13 | false | 5 |
| O-14 | false | 5 |
| O-15 | false | 1 |

Other live fields: `starterCredits: 0`, `killSwitch: false`,
`minAppVersion: "0"`, `dailyCapReached: false`. In particular, Expand remains
visible in the app while O-09 is disabled on the live server; the checked-in
registry is not evidence that the operation is currently available.

## Device journey

Every device command addressed only `RF8Y703DR2D`. `get-state` returned
`device`; `shell getprop ro.product.model` returned `SM-A165F`. The debug APK
was installed with `adb -s RF8Y703DR2D install -r`, and the app was cold-opened
after force-stopping its own package. No emulator was started. The same photo
(`8798.jpg`) was selected once in Android Photo Picker. Taps were located from
the saved `uiautomator dump /dev/tty` hierarchies; PNG captures were written as
binary bytes under `.kiln\scratch\device-plinth-w89\`.

| Step | Observation | Evidence |
|---|---|---|
| a. Cold-open Home | Hero reads “Your product photo”; it does not present an AI background as the whole app. The first-run guide says start with a product photo, pick a tool, then Generate; the tool section begins below. | `w89-01-home.png`, `w89-01-home.xml` |
| b. Import once | Android Photo Picker showed one selected item; the app returned with `8798.jpg` and “Selected photo · private copy ready”. | `w89-03-system-picker.png`, `w89-03-system-picker.xml`, `w89-04-photo-bound.png`, `w89-04-photo-bound.xml` |
| c. Catalogue | Studio shows two columns and the five expected cards. The cards are not equal-height across rows: the first row is 556 px high and the next row 316 px high. | `w89-05-studio-catalogue.png`, `w89-05-studio-catalogue.xml` |
| d. Cutout | “Cut out product” opens with the selected photo as a compact 135×135 px thumbnail, not a half-screen preview. No Choose photo, job ID or refresh-cost control appeared; the button reads “Generate · 1 credits”. | `w89-06-cutout.png`, `w89-06-cutout.xml` |
| e. Relight | Header says “Relight”; a separate “Return to the studio” banner is still present. The screen says “Using this photo”; no history picker or photo chooser appeared and the heading is one line. “Generate · 5 credits” was disabled. | `w89-08-relight.png`, `w89-08-relight.xml` |
| f. Scene prompt | Scene has a large free-text field and suggestion chips, with no Scene 1/2/3 selector. The required literal `палатка на фоне леса` was not entered: `adb -s RF8Y703DR2D shell input text 'палатка%sна%sфоне%sлеса'` failed with `NullPointerException` in `InputShellCommand.runText`. Tapping the “Forest at dusk” chip filled the text field, proving that suggestions populate the prompt; this was not the requested text. | `w89-10-scene.png`, `w89-10-scene.xml`, `w89-11-scene-prompt.png`, `w89-15-scene-prompt-chip.png`, `w89-15-scene-prompt-chip.xml` |
| g. Generate | With “Forest at dusk” visible in Scene's text field, the 2-credit Generate control was disabled. O-07 is `enabled: true` on the live server. No request was sent and no result is claimed. | `w89-15-scene-prompt-chip.png`, `w89-15-scene-prompt-chip.xml` |
| h. Home bottom | Bottom section is titled “More tools and exports” and presents explicit “Photograph an item” and “Slide kit (separate)” actions, plus an explanatory credits/Wallet panel. No Amazon, Wildberries or Ozon ad was visible. | `w89-17-home-bottom.png`, `w89-17-home-bottom.xml` |
| i. Wallet | Opened from the Studio/Wallet path. The visible balance was 10 credits; no job, request, terminal ID or UUID appeared in the visible hierarchy or screenshot. | `w89-21-wallet.png`, `w89-21-wallet.xml` |

Additional bound-photo checks: Recolour displayed “Using this photo” and a
disabled “Generate · 2 credits” button (`w89-18-recolour.png`,
`w89-18-recolour.xml`). Expand displayed “Using this photo” without a
Choose-photo control, but O-09 was live-disabled (`w89-19-expand.png`,
`w89-19-expand.xml`). Neither action was generated.

## C1–C16

| Complaint | Status | What was observed | Screenshot |
|---|---|---|---|
| C1 — catalogue grid | **FAIL** | It is a 2-column grid, not a full-screen button, but row card heights differ: 556 px versus 316 px. | `w89-05-studio-catalogue.png` |
| C2 — layout rhythm | PASS | Photo header, “Tools” title and grid form a clear vertical sequence; the captured surfaces do not overlap. | `w89-05-studio-catalogue.png` |
| C3 — photo selected once | PASS | One photo was selected; Cutout, Relight, Scene, Recolour and Expand show an existing-photo state rather than a choose-photo prompt. | `w89-04-photo-bound.png`, `w89-06-cutout.png`, `w89-08-relight.png`, `w89-10-scene.png`, `w89-18-recolour.png`, `w89-19-expand.png` |
| C4 — tool purpose and price | PASS | Cards show a name, a plain-language purpose and cost: Cutout 1, Scene 2, Relight 5, Expand 2, Recolour 2 credits. | `w89-05-studio-catalogue.png`, `w89-16-studio-scroll.png` |
| C5 — Cutout photo prompt/size | PASS | Bound-photo state opens directly; source thumbnail is 135×135 px, with “Change photo” rather than “Choose photo”. | `w89-06-cutout.png` |
| C6 — client-facing IDs | PASS | No IDs or UUID substrings appeared in the observed tool, Scene or Wallet screens; the selected filename was `8798.jpg`. | `w89-06-cutout.png`, `w89-08-relight.png`, `w89-15-scene-prompt-chip.png`, `w89-21-wallet.png` |
| C7 — refresh cost | PASS | No refresh-cost/wallet control appeared. The price is printed on each Generate action. | `w89-06-cutout.png`, `w89-08-relight.png`, `w89-15-scene-prompt-chip.png`, `w89-18-recolour.png`, `w89-19-expand.png` |
| C8 — free-text Scene | **FAIL** | The field is primary, suggestions fill it, and no Scene N selector appeared. The requested Russian text could not be entered via this ADB shell, so the exact prompt/submission check was not completed. | `w89-11-scene-prompt.png`, `w89-15-scene-prompt-chip.png` |
| C9 — Relight chrome | **FAIL** | Relight title is one line and no history picker/photo chooser appeared, but the prohibited “Return to the studio” banner remains. Generate was disabled. | `w89-08-relight.png` |
| C10 — Expand/Recolour binding | PASS | Expand and Recolour both opened with “Using this photo”; no choose-photo control. Expand availability is separately blocked by the live O-09 flag. | `w89-18-recolour.png`, `w89-19-expand.png` |
| C11 — Upscale naming | PASS | Upscale is absent from the current catalogue, as requested for this tree; there was no Upscale screen to mislabel. | `w89-05-studio-catalogue.png`, `w89-16-studio-scroll.png` |
| C12 — mystery footer | PASS | The bottom section has named, actionable “Photograph an item” and “Slide kit (separate)” controls and a credits/Wallet explanation; no unexplained “other ways to finish work” footer appeared. | `w89-17-home-bottom.png` |
| C13 — landing pitch | PASS | Home presents the product-photo workflow and catalogue, not AI background as the whole product. | `w89-01-home.png`, `w89-05-studio-catalogue.png` |
| C14 — marketplace ads | PASS | No Amazon, Wildberries or Ozon advertising was visible in the Home/Studio image-tool area. | `w89-05-studio-catalogue.png`, `w89-17-home-bottom.png` |
| C15 — first-use clarity | PASS | Home states what the app is, directs the user to start with a photo, and the cards state what each tool does. | `w89-01-home.png`, `w89-05-studio-catalogue.png` |
| C16 — tools run | **FAIL** | Live-enabled O-07 Scene, O-08 Relight and O-10 Recolour each showed disabled Generate. Expand also showed disabled Generate while live O-09 is false; it remains advertised in the catalogue. No result image is claimed. | `w89-15-scene-prompt-chip.png`, `w89-08-relight.png`, `w89-18-recolour.png`, `w89-19-expand.png` |

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The authorized phone was connected and identified as the A16. | MEASURED | `adb -s RF8Y703DR2D get-state` → `device`; `adb -s RF8Y703DR2D shell getprop ro.product.model` → `SM-A165F`. |
| The combined debug build and unit tests completed. | MEASURED | `runGradleTasks(absoluteSlot, [':app:assembleDebug', ':app:testDebugUnitTest'])` from `scripts/lib/gradle.mjs` → status 0, `BUILD SUCCESSFUL`, JDK 17; 64 suites, 320 tests, 0 failures/errors/skips in the resulting XML. |
| The recorded APK hash is current. | MEASURED | `Get-FileHash -Algorithm SHA256 apps\2026-10-15-plinth-product-photos\app\build\outputs\apk\debug\app-debug.apk` → `12A35CC3AB1633E89358231C345C7D7053F8989B0BF506D544E360C7CD2BD6CD`. |
| Live operation enablement is not inferred from the checked-in registry. | MEASURED | `curl.exe -fsS https://pikupo.com/plinth-debug/v1/config` → `ops.O-07.enabled=true`, `O-08=true`, `O-09=false`, `O-10=true`, `O-11=false`, `O-12=false`; raw response saved at `.kiln\scratch\device-plinth-w89\server-config.json`. |
| The app rendered the noted labels and control enabled states. | MEASURED | `adb -s RF8Y703DR2D exec-out uiautomator dump /dev/tty`; captured hierarchies `w89-06-cutout.xml`, `w89-08-relight.xml`, `w89-15-scene-prompt-chip.xml`, `w89-18-recolour.xml`, `w89-19-expand.xml`, `w89-21-wallet.xml`. |
| Screenshots represent the observed device screens. | MEASURED | `adb -s RF8Y703DR2D exec-out screencap -p`, captured as binary files under `.kiln\scratch\device-plinth-w89\`; listed beside each journey observation. |
| A generated result image was returned by Scene or Relight. | UNVERIFIED | Neither enabled operation's Generate control was enabled; no result request could be sent. |

## Record compatibility note

The requested `observations` entries are objects `{action, observed}`. The
current validator in `scripts/core/device.mjs` requires `observations` to be an
array of strings (`deviceRecordProblems`, lines 163–180). This run obeys the
explicit object-entry requirement and does not alter factory code; consequently
the current `slot.device` validator will reject this record until that schema
contract is updated.

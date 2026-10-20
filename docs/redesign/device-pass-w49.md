# W49b - on-device teal accent pass

**Status: BLOCKED for the current revision.** The installed revision below was
exercised and its accents looked teal, but concurrent source edits invalidated
its digest before final verification. The device record deliberately retains the
digest actually built and tested; it has not been advanced to untested sources.

## Device and artifact

MEASURED on 2026-10-17, starting at 11:52:57 +03:00:
`adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM`;
`getprop ro.build.version.release` returned `11`. Every ADB command used that
serial explicitly. No command targeted RF8Y703DR2D.

`node scripts/verify.mjs --slot plinth-product-photos --build` ran one Gradle
consumer, `assembleDebug`, successfully in 101521 ms; the complete factory run
took 118121 ms. Its initial failure was the stale W40 `slot.device` digest.
Log: `.kiln\scratch\device-plinth-w49\verify-build.txt`.

Installed the rebuilt `app\build\outputs\apk\debug\app-debug.apk` with
`adb -s R58MB3SGZVL install -r` (output: `Success`). Both that file and the
installed `base.apk`, pulled using the path returned by `pm path`, measured:

```text
sha256:      1809271f187e39686e99b524badbe4762c5b22d2e77833da869cd29ecc4f3cb5
buildDigest: 5de81383ab90243071271a22b9c5e82170f14d0008ad1a2144740b8855231c9e
```

`releaseSourceDigest('apps/2026-10-15-plinth-product-photos')` returned that
digest over 518 files both before the initial build and after the device journey.
The installed artifact is retained at
`.kiln\scratch\device-plinth-w49\installed-base.apk`.

**Package distinction:** the requested unsuffixed
`com.gexetr.plinthproductphotos` has no installed launch activity on this phone.
The existing debug build configuration adds `.debug`
(`apps/2026-10-15-plinth-product-photos/app/build.gradle.kts:93`).
The successfully launched and exercised package was therefore
`com.gexetr.plinthproductphotos.debug`; no package/build configuration was changed.

## Observed journey

All paired PNG screenshots and XML UI dumps below are under
`.kiln\scratch\device-plinth-w49\`. Coordinates are actual device pixels.

| Action | What appeared | Evidence pair |
|---|---|---|
| Launch debug app with `monkey -p com.gexetr.plinthproductphotos.debug -c android.intent.category.LAUNCHER 1` | Home, titled Studio: original photo, Continue editing, Bring in a product photo, Credits: 10 | `01-home.png`, `01-home.xml` |
| Tap Library at (455,2075) | Your projects, search, Filters, Select projects, saved `plinth-cup.jpg` project | `02-library.png`, `02-library.xml` |
| Tap More at (735,2075) | Credit wallet, Settings, About this app, Help & support | `03-more.png`, `03-more.xml` |
| Tap Settings at (408,813) | Appearance, Follow system theme, wallpaper colours unavailable on Android 11, language, vibration and new-image defaults | `04-settings.png`, `04-settings.xml` |
| Tap Studio at (176,2075), then Continue editing at (540,1224) | Smart card, original 2344 x 2344 photo, Wildberries, recognition/manual-category actions, balance 10 | `05-studio-editor.png`, `05-studio-editor.xml` |

MEASURED by viewing these PNGs: the Home primary action, Library filter/select
actions, Settings headings/enabled switch, and Smart card primary/manual actions
looked **teal, not lime**. The screenshots are 1080 x 2340. A Pillow
`Counter(Image.open(...).convert('RGB').getdata())` measurement found the exact
teal `#4E9F92` (RGB 78,159,146) at 154613 pixels in Home, 25233 in Library,
45217 in Settings and 165152 in Smart card. This supplements visual observation;
it does not establish that every UI pixel in every state is correct.

The saved Library thumbnail still contains green-tinted product imagery.
This pass concerns interface accents, not generation fidelity, and does not
reverse the W40 image-fidelity concern. No generation, purchase, or export was
triggered. The phone remained in its existing dark/Follow system appearance;
light mode and Android 12+ dynamic colours were not exercised.

The first launch dump returned `null root node`; its stale dump was replaced
after the app settled. The retained `01-home.xml` describes the actual app,
not the launcher. No palette edits, signing, store media, release approval or
publication were performed.

## Verification

The refreshed `device-verification.json` records this device, installed APK hash,
tested source digest and five observations. The first source-only rerun failed
`slot.device`: the current digest had changed to
`7d87e21134cb9048a1a95d3ffaa8f3ad933301e35c1f73678b24e7d92d918dd7`.
Evidence: `.kiln\scratch\device-plinth-w49\verify-stale-during-pass.txt`.

Observed source modification times at 11:58:24 +03:00 identified concurrent
changes to `PlayKilnApp.kt`, `AutoCardScreen.kt`, `CardScreen.kt`,
`HomeScreen.kt` and `LibraryScreen.kt`. A second factory build succeeded
(`assembleDebug` 178294 ms, whole run 217073 ms), but inputs changed again.
Evidence: `.kiln\scratch\device-plinth-w49\verify-rebuild.txt`.

Two further sequential `runGradleTasks(slot, ['assembleDebug'])` attempts used
pre/post `releaseSourceDigest` guards, never parallel Gradle consumers. The first
built successfully in 54265 ms but changed from `8a60fd63136e...` to
`e4110c018e6b...` during the build. The next attempt failed in 19558 ms:

```text
PlinthNavBar.kt:139:24 Type 'State<Color>' has no method 'getValue(Nothing?, KProperty0<*>)'
PlinthNavBar.kt:143:24 Type 'State<Float>' has no method 'getValue(Nothing?, KProperty0<*>)'
:app:compileDebugKotlin FAILED
```

The full source path is
`apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthNavBar.kt`.
The final guarded attempt changed from `60540425d76d...` to `9d2a0ea72e65...`;
both the compile failure and source drift are retained in
`.kiln\scratch\device-plinth-w49\gradle-current.txt` and `build-current.json`.
No later artifact was installed or claimed as device-tested.

The requested final source-only factory command is recorded in
`.kiln\scratch\device-plinth-w49\verify-final.txt`. At 12:07:48 +03:00 it
exited 1: **122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL**; `slot.device` remained
FAIL because the current digest was now `8a090848c046...`. The run took
24285 ms. Total observed worker wall-clock time from 11:52:57 to 12:07:48
was about **14 minutes 51 seconds**. Its `build` row is
UNVERIFIED by design without `--build`; missing store media is also UNVERIFIED,
not permission to create a release pack. A current `slot.device` PASS requires
the UI work to settle, a successful digest-stable rebuild, and another actual
install/journey. Merely copying the new digest into the record would be false.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Correct phone used | MEASURED | Explicit-serial `getprop` returned SM-A405FM; all journey commands used R58MB3SGZVL |
| Current debug APK built | MEASURED | `verify-build.txt`: `build` PASS, assembleDebug 101521 ms |
| Installed APK equals built APK | MEASURED | `Get-FileHash -Algorithm SHA256` on local app-debug.apk and pulled installed-base.apk returned the same full hash above |
| Accent looked teal rather than lime on exercised surfaces | MEASURED | Viewed `01-home.png`, `02-library.png`, `04-settings.png`, `05-studio-editor.png`; exact #4E9F92 pixel counts above |
| Device record no longer covers current sources | MEASURED | `verify-stale-during-pass.txt`: `slot.device` FAIL after concurrent edits |
| Latest attempted build blocked by UI compilation | MEASURED | `gradle-current.txt`: missing delegates at PlinthNavBar.kt:139,143; `build-current.json`: status 1 |
| Unsuffixed release package exercised | UNVERIFIED | Not installed; debug-suffixed variant was used instead |
| Light/dynamic themes and generation fidelity verified | UNVERIFIED | Not exercised by this bounded accent/navigation pass |

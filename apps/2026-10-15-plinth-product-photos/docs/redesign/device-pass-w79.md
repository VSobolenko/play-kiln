# W79: A40 device pass attempt

**Status: BLOCKED / device behavior UNVERIFIED.** On 2026-10-17, the requested
serial `R58MB3SGZVL` could not be addressed. No emulator, second phone, local
server, `adb reverse`, server edit, or release approval was used.

## Build and navigation test correction

The initial build used `runGradleTasks` from `scripts/lib/gradle.mjs` with
`['assembleDebug', 'assembleDebugAndroidTest']`, holding the slot lease for one
Gradle invocation. Observed exit: 0; `BUILD SUCCESSFUL in 8s`; helper elapsed:
8602 ms; JDK 17. Both APKs assembled. Log:
`.kiln/scratch/copilot-plinth-w79-device/build.txt`.

`PlinthBatchNavigationTest.kt` now exercises Profile selection in both the
five-slot bottom bar and the navigation rail, expecting `PROFILE` rather than
`BATCH`. The Batch hub test first asserts that Batch is absent on the initial
shell, then opens Library and selects Batch. This catches a wrong Profile route
callback or an inaccessible Library-to-Batch route without restoring the obsolete
main-tab contract.

The modified androidTest source compiled in the build above. Its assertions have
**not** run on a device; compilation is not a passing instrumentation test.
No production behavior was changed.

## Device evidence

Commands were executed using
`E:\Programm\Android\SDK\platform-tools\adb.exe`. Initial and post-build probes
agreed. The persisted post-build attempt at `2026-10-17T16:13:44Z` recorded:

| Command | Observed result |
|---|---|
| `adb devices -l` | Exit 0; only `(no serial number) offline transport_id:1` |
| `adb -s R58MB3SGZVL get-state` | Exit 1; device not found |
| `adb -s R58MB3SGZVL shell getprop ro.product.model` | Exit 1; device not found; model not confirmed |
| `adb -s R58MB3SGZVL install -r <debug APK>` | Exit 1; device not found; no installation |

Evidence:
`.kiln/scratch/copilot-plinth-w79-device/device-attempt.json`.

A subsequent serial-targeted `wait-for-device` was bounded to 15 seconds and
timed out. At `2026-10-17T16:14:17Z`, listing devices still returned only the
unnamed offline transport, and the model query still returned device not found.
Evidence: `.kiln/scratch/copilot-plinth-w79-device/device-retry.json`.
The run did not target that unnamed transport or restart the shared ADB server.

The built APK is `app\build\outputs\apk\debug\app-debug.apk`, with SHA-256
`cf79d882578bfd8fff302bda4a2bcde4586295f6e342fc4334ce6e713ac443bc`.
The pre-report source digest, computed by importing `releaseSourceDigest` from
`scripts/lib/release-approval.mjs`, was
`cce8a1833b4b59a0ad42fd2d8072f197163162fcdc07d0bc820559c9f69b634e`
over 570 files. These identify the local build inputs, **not an installed or
tested artifact**. Evidence:
`.kiln/scratch/copilot-plinth-w79-device/artifact.json`.
Adding this report changes the source digest; the final local measurement is
written separately to `final-artifact.json` in that scratch directory.

## Requested journeys

Each criterion below would fail if the stated screen could not be reached or the
expected control did not appear. None could be evaluated because installation
and model confirmation failed.

| Journey | Required observation | Result |
|---|---|---|
| Launch / home | Usable app shell; Profile rather than Batch as a main tab | UNVERIFIED |
| Studio | Studio opens; Generate displays its cost; inspect credit explanatory wall | UNVERIFIED |
| Profile | Credits / wallet entry opens | UNVERIFIED |
| Create (+) | Create / AI path opens; inspect any local-composition-without-AI wall | UNVERIFIED |
| Library / Batch | Batch hub opens from Library, not a main tab | UNVERIFIED |

No screenshots or hierarchy dumps were captured. The evidence directory is
`.kiln/scratch/copilot-plinth-w79-device/`; no image from a previous wave is
presented as W79 evidence.

`device-verification.json` is intentionally **unchanged**. Its historical W69/W64
observations cannot be rewritten as three W79 phone observations when the phone
was not reachable. Rebinding them to today's digest would falsely certify this
revision. The outstanding work is to repeat the install, model query and actual
journeys when the named A40 is addressable, then replace the record with the
newly observed evidence.

## Final factory gate

`node scripts/verify.mjs --slot plinth-product-photos --build` returned exit 1:
**122 PASS, 2 UNVERIFIED, 4 SKIP, 2 FAIL**. The build row passed
(`assembleDebug`, 3216 ms, JDK 17). The FAIL rows were:

| Check | Observed failure |
|---|---|
| `slot.differentiators` | Claim `auto-card-by-item` names proof symbol `recognizePhoto`, which no longer appears in `AutoCardScreen.kt` |
| `slot.device` | Historical `buildDigest` `d723c0a64840...` disagrees with current inputs; a new real-device run is required |

`pack.inventory` and `pack.media` were UNVERIFIED because no `store/assets`
exists. This task does not generate a phase-2 store pack or alter the promise
contract to hide the proof drift.

Complete stdout/stderr and actual exit status are retained as
`.kiln/scratch/copilot-plinth-w79-device/verify.txt` and
`.kiln/scratch/copilot-plinth-w79-device/verify-result.json`.
The command is repeated after finalizing this report; those files retain the
latest run, and `final-artifact.json` retains its source digest. A stale
historical device record is not a W79 pass.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Debug and androidTest APK assembly completed, exit 0 | MEASURED | `build.txt`: `BUILD SUCCESSFUL in 8s`; helper elapsed 8602 ms |
| Profile replaces obsolete Batch selection assertions; the test reaches Batch through Library | MEASURED | `app\src\androidTest\java\com\gexetr\plinthproductphotos\ui\PlinthBatchNavigationTest.kt:30`; source assertions, not a phone observation |
| Requested serial was absent and install failed | MEASURED | `device-attempt.json`: serial-specific commands exit 1 |
| Bounded wait did not recover the target | MEASURED | `device-retry.json`: `ETIMEDOUT`; model query exit 1 |
| Local APK hash and pre-report digest were computed | MEASURED | `artifact.json`; `releaseSourceDigest` imported, not reimplemented |
| SM-A405FM identity, screen behavior and navigation assertion results | UNVERIFIED | Requires named A40 online, successful install and device journeys / instrumentation |
| Current revision has three W79 phone observations | UNVERIFIED | No W79 run occurred; historical device record left intact |
| Factory gate failed on proof drift and stale device evidence | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos --build`: exit 1; `verify.txt` and `verify-result.json` |

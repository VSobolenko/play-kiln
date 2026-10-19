# W77 - Shell: Profile and honest Create

2026-10-17. Implements W72 section 3 and W70 O16 within the authorised shell
paths. Paths below are slot-relative; `K` is
`app\src\main\java\com\gexetr\plinthproductphotos`, and `T` is
`app\src\androidTest\java\com\gexetr\plinthproductphotos`.

## Routing decisions

Profile replaces Batch in both the bottom navigation and wide navigation rail.
Both use the same section-selection callback and serializable Profile destination.
The destination hosts W76's existing `ProfileRoute(onOpenWallet, onOpenLibrary)`;
the shell adds a Settings entry without changing Profile's content or data.
Wallet is pushed over Profile; opening projects selects the Library root.

Batch remains a secondary action above Library, opening the existing project
picker as a detail destination with Back. Project selection still opens Looks
and its existing batch-processing flow. Keeping Batch as a fifth section was
rejected because W70 O16 explicitly replaces that destination with Profile.

Center Create retains W74's photo import and its separate AI background / Card
layout actions. The AI callback preserves the selected photo ID and now opens
W74's `GeneratedSceneRoute` directly. This removes the obsolete shell requirement
to load a marketplace preset, mask and local scene recipes before entering a
server-generated background. The alternative of retaining that local-compositor
adapter was rejected because those inputs do not drive W74's scene operation.
Home and coach AutoCard destinations use the same generated-scene entry. Changing
photo returns to an existing import entry or pushes one; Card layout still opens
the separate local Card destination.

Profile labels are `Profile` / `Профиль`. The icon reuses the existing
`ic_lucide_info` drawable, not a newly drawn icon or a new dependency. This is a
bounded existing-asset choice, not a claim that an information glyph is a
competitor's account icon. No drawable was writable in this package.

## Observed commands and limits

The first lease inspection found a live holder, PID 143408, running
`:app:compileDebugAndroidTestKotlin`. A later inspection returned
`{"holder":null,"alive":false}`. No lease was removed or bypassed.

Exactly one `assembleDebug` invocation was made through `runGradleTasks`, combined
with `:app:compileDebugAndroidTestKotlin`. Main Kotlin compilation completed, but
the invocation exited 1 after 48,254 ms because the new W77 test lacked the
`androidx.compose.ui.test.ForcedSize` extension import. Debug packaging had not
completed when the test compiler failed. Log, relative to repository root:
`.kiln\scratch\copilot-plinth-w77-shell-profile\w77-build.log`.

After adding that import, the narrower corrective invocation was:

```text
runGradleTasks(slot, [':app:compileDebugAndroidTestKotlin', ':app:packageDebug'])
exit 0; BUILD SUCCESSFUL in 10s; elapsedMs 10914
50 actionable tasks: 3 executed, 47 up-to-date
```

This compiled instrumentation sources and completed debug APK packaging; it was
not a second `assembleDebug` invocation. Log:
`.kiln\scratch\copilot-plinth-w77-shell-profile\w77-compile-package.log`.
No W74 resources or Studio internals required repair.

The packaged artifact was observed at
`app\build\outputs\apk\debug\app-debug.apk`, 24,988,518 bytes.
SHA-256: `CF79D882578BFD8FFF302BDA4A2BCDE4586295F6E342FC4334CE6E713AC443BC`.
PowerShell XML parsing and EN/RU key comparison reported
`Shell resource parity: 21 keys`; scoped `git diff --check` exited 0.
After strengthening the Settings destination assertion, a final
`:app:compileDebugAndroidTestKotlin` invocation exited 0 in 5,037 ms
(`BUILD SUCCESSFUL in 4s`); its log is
`.kiln\scratch\copilot-plinth-w77-shell-profile\w77-final-tests-compile.log`.

Six new instrumentation cases cover forced 400 x 800 dp phone and 1000 x 800 dp
wide Profile/Wallet and Create entries, Library's secondary Batch entry, and
Profile's Settings entry. Compilation is not execution: **device behaviour and
live AI generation remain UNVERIFIED**. W79 can run the W77 class using
`:app:connectedDebugAndroidTest
-Pandroid.testInstrumentationRunnerArguments.class=com.gexetr.plinthproductphotos.w77.ShellProfileNavigationTest`
through the leased Gradle route, then exercise photo selection and actual
generation on the review device. This worker did not claim a device pass or run
paid generation.

The pre-existing `T\ui\PlinthBatchNavigationTest.kt:30-96` still asserts the
superseded main-tab Batch contract. It is outside W77's exclusive writable paths
and was not edited. Its assertions conflict with O16; an unfiltered execution
must not be described as green without updating that separately owned test.
The new W77 cases encode the replacement contract.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Profile replaces the main Batch destination on phone and rail | MEASURED | `K\ui\components\PlinthNavBar.kt:166,230,348`; `K\ui\PlayKilnApp.kt:274-292,424-448` |
| Profile uses W76 wallet/library callbacks and a shell Settings entry | MEASURED | `K\ui\PlayKilnApp.kt:424-448` |
| Batch remains reachable as a Library detail, not a section root | MEASURED | `K\ui\PlayKilnApp.kt:557-614` |
| Both center Create controls invoke the same import flow | MEASURED | `K\ui\PlayKilnApp.kt:310-313,333,343,458-464` |
| The selected AI photo ID reaches W74 without local recipe/preset composition | MEASURED | `K\ui\PlayKilnApp.kt:461,480-492`; `K\ui\autocard\GeneratedSceneRoute.kt:47-83,103-110` |
| EN/RU shell resources have matching keys | MEASURED | PowerShell XML parse + `Compare-Object`: `Shell resource parity: 21 keys` |
| A pre-existing shell icon is reused | MEASURED | `K\ui\components\PlinthNavBar.kt:355`; `app\src\main\res\drawable\ic_lucide_info.xml`; no drawable added |
| The vendored Lucide family is the licence-cleared reuse surface | ON RECORD | `competitor-teardown.md`, implementation-map row 14: "Reuse the existing licence-cleared family; no new icon set" |
| Main code and instrumentation sources compiled; debug APK packaged | MEASURED | Corrective `runGradleTasks` command above exited 0; `w77-compile-package.log`; APK observed at 24,988,518 bytes |
| The single combined assemble invocation succeeded | UNVERIFIED | It exited 1 at the W77 test import; subsequent packaging is separate evidence, not an assemble success |
| Phone/wide routes and Profile callbacks behave correctly on a device | UNVERIFIED | Run `T\w77\ShellProfileNavigationTest.kt` with the class-filtered command above; six cases, no execution claimed |
| The generated background faithfully preserves the selected product | UNVERIFIED | Requires W79 source/result and live-device evidence; no provider request made here |

Elapsed wall-clock at the artifact/resource check: approximately 4 minutes
(session request timestamp 19:05:53 +03:00; observed check 19:10:05 +03:00).
Documentation and final checks follow that reading; no release approval or
publish action was performed.

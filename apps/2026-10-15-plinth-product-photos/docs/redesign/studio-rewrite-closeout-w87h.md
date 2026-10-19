# W87h - Studio rewrite closeout

## Outcome

The live Studio client has the requested six operation rows and request allowlist, the freeform scene flow remains intact, and Home no longer offers the unsupported virtual-model route. Kotlin app sources and instrumentation sources compile, the unit-test task passes, and a debug APK was assembled.

At this closeout, Expand (O-09), Upscale (O-11), and Erase (O-12) were not end-to-end runnable. This catalogue mismatch was resolved after W87h: W90 removed Expand from `studioRows`, and Upscale/Erase are absent from the user-facing Home/Studio catalogue (see W88c). They remain unavailable rather than generating tools; do not describe them as generating until their server contracts and, for Upscale, its price are resolved.

## Changes in this closeout

- Removed `VIRTUAL_MODEL` from Home's tool cards. It navigated into the live Studio route, which has no matching startable row.
- Replaced unavailable Compose test imports in `StudioShellTest` with node-collection count assertions so the instrumentation Kotlin source compiles.
- Kept the existing W87 Studio shell and six-operation wiring; no server availability, price, or kill-switch was overridden.

## Verification

- **RED:** the focused `HomeToolboxTest` run failed at its catalogue assertion while Home still listed `VIRTUAL_MODEL`.
- **GREEN:** `scripts/lib/gradle.mjs` ran `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, and `:app:assembleDebug`; `BUILD SUCCESSFUL`.
- **Instrumentation compile:** `scripts/lib/gradle.mjs` ran `:app:compileDebugAndroidTestKotlin`; `BUILD SUCCESSFUL` after correcting the unsupported imports.
- **Final build:** `scripts/lib/gradle.mjs` reran compile, unit tests, instrumentation compile, and assemble; `BUILD SUCCESSFUL`.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 25,000,014 bytes, SHA-256 `86672F40D0C2DE474B355326733D39857289A5F0F0C2E1013732816D62E60B76`.
- No device was attached or used in this run; installation and visual interaction remain unverified.

## Server-side generation blocker

`StudioRoute.availableRows` hides any row whose `/config` quote is disabled. The local server registry sets O-09, O-11, and O-12 to disabled, and the server tests assert those states. O-09 requires `canvas_size` (and its provider accepts the original-image placement/size fields); the current `StudioViewModel` sends only an optional prompt. O-12 requires a region `mask_url`, which the Studio form does not produce. O-11's registered 2048x2048 cost is USD 0.125829, above the registry's USD 0.125 premium ceiling; the server provider input adapter does not currently accept O-11 either. This closeout leaves those safeguards in place rather than exposing a button that cannot submit valid, affordable work.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Studio has request/catalogue entries for Scene, Relight, Recolour, Expand, Upscale, and Erase, mapped to O-07 through O-12. | MEASURED | `StudioViewModel.kt:572-587`; `StudioRoute.kt:744-776`; `StudioPromptValidationTest.kt:27-30` |
| Scene direction remains freeform; suggestion chips fill the prompt, the route starts O-07 with that prompt, and neither live scene route has numbered-scene or manual price-refresh UI. | MEASURED | `StudioRoute.kt:256-257, 580-630`; `GeneratedSceneRoute.kt:106, 158, 285-308`; `rg -n -i "RadioButton|Scene [123]|refresh price|refresh cost" StudioRoute.kt GeneratedSceneRoute.kt` returned no matches |
| Home's image tools use equal-weight two-column cards; listing text remains in the separate lower workflow rail. | MEASURED | `HomeScreen.kt:152-177, 225-231, 360-424, 572-615`; `strings_home.xml:4-5, 51` |
| Cutout opens the latest already-imported photo when one exists; job IDs remain an internal report target, and the primary action is the cost-labelled cutout action. | MEASURED | `CutoutViewModel.kt:238-248`; `CutoutScreen.kt:184-185, 315-321, 383-415` |
| O-09/O-11/O-12 cannot currently generate through the server path; current server source disables them for missing inputs or price ceiling, and Studio hides disabled quotes. | MEASURED | `StudioRoute.kt:134-145`; `StudioViewModel.kt:188-201`; `server/src/registry.ts:59-77`; `server/src/providers/fal.ts:97-137`; `server/test/registry.test.ts:55-62` |
| The debug APK installs and all Studio interactions render correctly on a device. | UNVERIFIED | No device run was performed. Decide with `adb install -r "D:\\Python\\PlayKiln\\apps\\2026-10-15-plinth-product-photos\\app\\build\\outputs\\apk\\debug\\app-debug.apk"` followed by the W87 device flow; a device operator must record serial and observed screens. |
| Live provider generation and deployed `/config` availability match the checked-in server source. | UNVERIFIED | No authenticated live-provider request was run. Decide with a reviewed live `/config` response and a controlled end-to-end operation run; the service operator owns that evidence. |

Elapsed wall-clock time for this task: **11.51 minutes MEASURED**, from `2026-10-17T22:27:25.945+03:00` to `2026-10-17T22:38:56.394+03:00`.

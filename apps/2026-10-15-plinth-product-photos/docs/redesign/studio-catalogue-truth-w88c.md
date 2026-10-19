# W88c — Home + Studio catalogue truth

| Tool | Runnable | What changed | path:line |
|---|---|---|---|
| Cutout (O-02) | yes | unchanged; separate cutout route | HomeScreen.kt:574 |
| Scene (O-07) | yes | name shortened to "Scene" | StudioRoute.kt:756 |
| Relight (O-08) | yes | kept | StudioRoute.kt:756 |
| Recolour (O-10) | yes | kept | StudioRoute.kt:756 |
| Expand (O-09) | yes, now | client sends `canvas_size`, `original_image_size`, `original_image_location` (square canvas = long side x 5/4); registry enabled | StudioViewModel.kt:208, :589; registry.ts:59 |
| Upscale (O-11) | no, removed from Home and Studio | no provider mapping, exceeds price cap; registry stays disabled | registry.ts:67 |
| Erase (O-12) | no, removed from Home and Studio | no mask source (cutout mask is the product itself) and no provider mapping; registry stays disabled | registry.ts:72 |

Other changes:
- One list: `homeTools` = Cutout + `studioRows` (HomeScreen.kt:574, StudioRoute.kt:756). `REQUEST_OPERATIONS` (StudioViewModel.kt:582) matches it.
- Tool screen header is the tool name only (one line). Back is an icon. No change-photo control. No prior-generations picker.
- Grid purpose text allows 2 lines; the 2-column grid is unchanged.
- C15: the empty state shows a 3-step guide from `home_guide_*` (HomeScreen.kt:265). Unused Upscale/Erase strings and `home_guide_dismiss` were deleted. "Studio tools" became "Tools".
- Tests touched outside the owned list: `HomeToolboxTest.kt` now expects 5 tools.
- Not changed: `StudioToolsScreen.kt` still lists Upscale/Erase through the domain catalogue. It is not reachable from Home or Studio. `test_studio_wiring.py` still asserts `STUDIO_PROVIDER_RESULTS_READY = false` (stale before this task). Server tests and androidTests were not run. The Expand live provider call was not probed.

## Gradle
`runGradleTasks(apps/2026-10-15-plinth-product-photos, [":app:compileDebugKotlin", ":app:assembleDebug"])` via `scripts/lib/gradle.mjs` → BUILD SUCCESSFUL in 25s, exit code 0.

## Claim ledger
| Claim | Status | Evidence |
|---|---|---|
| App compiles and assembles | MEASURED | Gradle exit 0 (above) |
| Expand payload carries canvas geometry | MEASURED (source) | StudioViewModel.kt:208 |
| O-09 enabled, O-11/O-12 disabled | MEASURED (source) | registry.ts:59-77 |
| Home and Studio share one tool list | MEASURED (source) | HomeScreen.kt:574 |
| Expand returns a real image from the provider | UNVERIFIED | fal.ts maps the fields; no live call made |
| On-device layout (no 3-line wrap, header) | UNVERIFIED | not run on a device |

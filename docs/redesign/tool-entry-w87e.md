# W87e — tool entry (Relight / Expand / Upscale / Erase / Model)

## Change
- `HomeScreen.kt`: non-cut-out tool cards now call `onStudioToolsClick(StudioOperationId)` (was a parameterless callback that dropped which tool was tapped).
- `PlayKilnApp.kt`: `StudioToolsDestination(operation: String?)`; maps to `OperationId` and calls `StudioRoute(initialOperation, onBack)`.
- `StudioRoute.kt`: `initialOperation` opens tool mode directly; back leaves the screen (no catalogue). The photo is the already-selected/latest one (existing auto-select), no picker wall. Title for ops outside `studioRows` comes from `studioToolTitle` — Upscale / Увеличение разрешения, never "Studio tools".
- `strings_listing.xml` (en/ru): "Marketplace" → "Format for marketplace" / "Формат для площадки" so presets read as format rules, not a generative product.

## Claim ledger
| Claim | Status | Evidence |
|---|---|---|
| Relight/Scene/Recolour from Home land in tool mode on the current photo | CODE-READ | PlayKilnApp.kt `StudioToolsDestination(it.name)`, StudioRoute.kt `selectedOperation` init + existing `choosePhoto(first)` effect |
| Upscale title is not "Studio tools" | CODE-READ | StudioRoute.kt `studioToolTitle`, `studio_tool_title_upscale` |
| Expand/Upscale/Erase actually execute | UNVERIFIED / FALSE today | `StudioViewModel.REQUEST_OPERATIONS` = scene/relight/recolour only; those tools open titled but show "No edits are available right now." No live path was added |
| "Other ways to finish the job" footers | NOT FOUND | grep of ui/ found none; `StudioToolsScreen.kt` is not routed (dead code), left untouched |
| Listing is non-generative | CODE-READ | `listing_explanation` states no AI text generation; only label copy changed |
| Compiles | UNVERIFIED | No Gradle run (per task) |

## Not done
- No dedicated outpaint execution UI exists (no `OperationId.OUTPAINT`; it is `EXPAND`); building one needs VM + backend work, out of scope.

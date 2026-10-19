# W101 — Generation must look alive

Gradle and a device were NOT run; everything below is source-level.

## Old progress row
It was inside `StudioWorkflowPanel` (`StudioRoute.kt`, the `if (waiting)` block just above the Generate button), which is
the first *item* of the `LazyColumn` in `StudioRoute.kt`. It sat under the photo preview (aspect 0.78), the prompt field
(2–8 lines) and chips, so on a phone it started below the fold. The list also kept its scroll offset.

## New pinned element
- `StudioProgressBanner(operation)` (`StudioRoute.kt:888`), one shared composable. It is placed in a `Column` above the
  `LazyColumn` (`StudioRoute.kt:365`), so it cannot scroll away. It shows a spinner, an operation name
  (`studioProgressLabel`, `:878`: Relighting… / Creating the background… / Recolouring… / Expanding the canvas…)
  and a "this can take a minute or more" line (`studio_live_progress_slow`). testTag `studio-progress-banner`.
- Driven by `StudioState.inFlight` (`StudioViewModel.kt:85`) = `busy || (hasPending && result == null && errorCode == null)`,
  ViewModel state, not a local `remember`.
- The inline spinner row was removed; the primary button stays in place, disabled, and its label becomes the progress
  label (`StudioRoute.kt` Button in `StudioWorkflowPanel`) so it never vanishes.

## Auto-scroll
`rememberLazyListState` (`:358`) + `LaunchedEffect(state.result, hasResult)` → `animateScrollToItem(resultIndex)` (`:362`)
to the Before/After item (index 1, or 2 when a failure notice item precedes it). The banner is outside the list, so it
cannot conflict with it.

## Label fix
`EditorScreen.kt` (Remove at ~:269, Restore at ~:285): the label `Box` had no size/padding inside the half-width Surface;
now `fillMaxWidth()` + `padding(12.dp, 14.dp)` with centred, wrapping text. Strings unchanged, so both locales wrap
instead of clipping.

## Strings
`studio_live_progress_{relight,scene,recolour,expand,slow}` in `values/` and `values-ru/strings_studio_live.xml`.
No count-varying text, so no plurals needed.

## Device proof
Tap "Relight · 5 credits"; at every 5 s `uiautomator dump` for ~65 s must contain `studio-progress-banner` / text
"Relighting…" with bounds inside the screen height, and the button text "Relighting…" (disabled). On completion the dump
must show the Before and After labels in viewport without scrolling. In the editor, both labels must appear in full with
left bound > 0 inside the Surface.

## Claim ledger
| Claim | Status | Evidence |
|---|---|---|
| Banner is outside the scrolling list | MEASURED (read) | StudioRoute.kt:365 vs LazyColumn below |
| Banner driven by VM state | MEASURED (read) | StudioViewModel.kt:85, StudioRoute.kt:365,384 |
| Auto-scroll on result | MEASURED (read) | StudioRoute.kt:358-362 |
| Label no longer clipped | UNVERIFIED | needs device; edit at EditorScreen.kt ~269/285 |
| Compiles | UNVERIFIED | Gradle not run (integrator) |
| Banner visible during 65 s relight | UNVERIFIED | needs device |

# Studio tab scroll rewrite (W62)

## Scope

The bottom-navigation Studio destination renders `HomeRoute` / `HomeScreen`. This rewrite changes
that screen's scroll architecture only. `HomeSceneStrip.kt`, navigation, the palette, haptics, and
catalog pricing remain owned by their existing sources.

## Before

Immediately before the rewrite, a source regression probe failed with all of these findings in
`HomeScreen.kt`:

- The vertical container was a two-column `LazyVerticalGrid` (former line 139), so full-width hero,
  scene, credits, recent-work, and workflow sections were grid items using max-line spans.
- The empty hero used a `Canvas` in the scrollable content (former line 363).
- Hero and recent thumbnails installed `sharedBounds` modifiers (former lines 310 and 430).
- Every tool card received the same latest-photo file as its image model (former line 199), creating
  repeated image work across the tool grid.

The exact pre-change probe required one `LazyColumn` and rejected `LazyVerticalGrid`, `Canvas(`,
`rememberInfiniteTransition`, `infiniteRepeatable`, and `sharedBounds(`. It failed for the missing
column plus the grid, Canvas, and shared-bounds findings.

## After

- `HomeScreen` now has one `LazyColumn` at `HomeScreen.kt:124`.
- Every top-level section has a stable key at `HomeScreen.kt:129-237`; recent photos retain their
  photo-id keys at `HomeScreen.kt:196`, and workflow items now have explicit keys at
  `HomeScreen.kt:214-237`.
- The tool section uses keyed, fixed two-card rows at `HomeScreen.kt:159-180`, avoiding a nested
  vertical scroller. Costs still come from `StudioOperationCatalog` at `HomeScreen.kt:356-374`.
- Tool artwork is icon-backed (`imageModel = null`) at `HomeScreen.kt:380`, avoiding repeated Coil
  loads of the latest photo while preserving every tool entry point.
- The hero uses one fixed-aspect `SubcomposeAsyncImage` at `HomeScreen.kt:271-298`. Its empty state
  is a static Material surface and icon, with no drawing loop.
- The scene strip call remains at `HomeScreen.kt:137`; credits and wallet navigation remain at
  `HomeScreen.kt:182-186`.
- Home scroll items no longer contain Canvas, infinite transitions, placement animation, or
  shared-bounds modifiers.

## Verification

The same source regression probe passes after the rewrite. Per W62's orchestration contract, no
Gradle command was run; compilation and device scroll verification remain with the orchestrator.

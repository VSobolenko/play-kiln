# W42 — Library image-first polish

## Result

Library now gives saved project photos most of each tile and more of the first
viewport. The explanatory paragraph and repeated project/category/marketplace/
updated labels are removed from the grid; each compact tile keeps the project
name, item count and primary SKU. Search, filters, selection, batch operations,
empty/error states and project-open callbacks remain in place.

Project previews continue through Coil. A single preview component maps a
missing local file and Coil load failure to the same unavailable placeholder,
while an active request uses the existing loading copy. The custom state UI
justifies `SubcomposeAsyncImage`; no second image loader or dependency was
added. `ContentScale.Fit` is retained so the seller's product is not cropped.
Stable project IDs still key the lazy grid, and Foundation `animateItem()`
animates project insertions/removals without changing navigation or export
behavior.

## Paging decision

Paging is retained; it is not newly introduced. The Room query returns
`PagingSource<Int, ProjectSummary>`, the repository wraps it in a Pager with a
30-project page size, and the project table has no schema-level count limit.
Each visible row also calculates its item count and primary SKU from the item
table. A seller can keep adding projects and photos, so bounding rendered and
queried rows is appropriate; switching to `Flow<List<...>>` would materialize
the complete, potentially growing library. This is a workload-based decision,
not a claim about a measured user's current database size. The existing refresh/
append retry states are therefore kept.

The ViewModel now derives the Paging query from filter fields only. Selection,
operation messages and other UI-only changes no longer create a fresh Pager;
changing an actual filter still switches the Room query as before.

## Claim ledger

| Claim | Label | Evidence / limit |
|---|---|---|
| Library renders Room project summaries in pages of 30. | **MEASURED** | `data/repository/LibraryRepository.kt`: `PagingConfig(pageSize = PAGE_SIZE)` and `PAGE_SIZE = 30`; `data/db/Daos.kt`: `pageLibrary()` returns a Room `PagingSource`. |
| Project count is not bounded by the schema, and each summary includes item-derived values. | **MEASURED** | `data/db/Entities.kt`: `ProjectEntity` has no maximum-count constraint; `data/db/Daos.kt`: correlated item count and primary-SKU queries. This is static schema/query evidence, not a user's live row count. |
| Paging is a better fit than a whole-library `Flow<List<...>>` for a seller's growing local library. | **INFERRED** | Unbounded project creation, image-bearing tiles and a Room paging query support bounded loading; no runtime database-size/performance sample was available. |
| The denser image-first composition improves scan speed and perceived polish. | **INFERRED / UNVERIFIED** | Image area and visible project metadata were changed in `ui/library/LibraryScreen.kt`; no device review, user study or frame measurement was run (W40 constraint). |
| Coil provides the preview loading/error rendering and the missing-file path shares the unavailable state. | **MEASURED** | `ui/library/LibraryScreen.kt`: `ProjectPreview()` uses `SubcomposeAsyncImage`; absent file and Coil error use the same localized placeholder. |
| Paging is not restarted for selection/message-only state updates. | **MEASURED** | `ui/library/LibraryViewModel.kt`: the flow maps to `LibraryQuery` and applies `distinctUntilChanged()` before `flatMapLatest`. |
| Open-project and export actions continue to route through their existing callbacks and ViewModel operations. | **MEASURED / UNVERIFIED** | Callback and operation wiring were preserved in source; no Gradle or device interaction test was run. |

## Verification boundary

`node scripts/verify.mjs --slot plinth-product-photos` completed with 122 PASS,
3 UNVERIFIED, 4 SKIP and 1 FAIL. `slot.locales` and `uiux.text-layout` pass.
The sole failure is `slot.device`: the recorded device run's build digest no
longer matches the changed build inputs. Refreshing that evidence needs a device
run, which W40 excludes. The build row is UNVERIFIED because `--build` was not
run. Runtime layout, Coil rendering and animation behavior remain unverified on
device.

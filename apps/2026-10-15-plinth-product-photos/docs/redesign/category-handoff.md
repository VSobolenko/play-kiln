# W17: category handoff

## Falsifier, recorded before implementation

If Shoes and Jewelry still resolve to the same initial scene, or a null category
changes the existing default, the change is wrong. Actual screen behavior requires
the orchestrator's build/device run; source-contract checks are not that evidence.

## Defect and the three hops

Paths below are relative to `app/src/main/java/com/gexetr/plinthproductphotos/`.
These are the post-change locations of the previously broken boundaries.

1. `ui/home/HomeScreen.kt:145-146`: Home previously ignored the category delivered
   by the strip. The lambda now binds it and calls `onMakeAutoCard(photoId, category)`.
   Both Home callback signatures carry a nullable `AutoCategory`.
2. `ui/PlayKilnApp.kt:154-157`, `:299-301`, `:385`: the single existing
   `AutoCardDestination` now carries `category: AutoCategory? = null`. Home pushes
   that field, and the destination forwards it to `AutoCardRoute.lastUsedCategory`.
   Previously the destination carried only the photo id and passed a literal null.
3. `ui/autocard/AutoCardScreen.kt:78-84`, `:93`, `:116-117`, `:146`:
   the unused-parameter suppression is removed. `initialAutoCardFlow` preserves the
   existing online/offline base state, then applies a non-null category using the
   existing `chooseCategory`. Flow and scene selection are remembered by photo and
   incoming category, so a changed handoff cannot retain a previous scene id.

No parallel route, store, enum member, resource, price or wallet change was added.
`HomeSceneStrip.kt` was not edited; its signature is sufficient.

## Selection and existing constraints

The flow supplies category recipes at `ui/autocard/AutoCardScreen.kt:149-153`.
With no explicit variant id, the existing chooser selects index zero (`:620`) and
`domain/autocard/AutoCardEngine.kt:311-312` selects the first matching recipe.
Thus an explicit handoff uses that category's first scene without a new selection
algorithm or an automatic recognition request.

The existing marketplace restrictions still win: white-main recipes remain hidden
by the route's existing filter, and the engine retains its white-main override and
original-only behavior. Missing generated backgrounds remain unavailable in the
chooser (`ui/autocard/AutoCardScreen.kt:280-284`), yield no preview (`:237`), and
retain the existing unavailable message (`:640`). No fallback scene was invented.

Regenerate previously compared whole destinations. A category-bearing destination
would differ from a newly constructed null-category destination. The comparison now
reuses the existing AutoCard destination for the same photo (`ui/PlayKilnApp.kt:408-413`),
preserving its category rather than pushing a default route on top.

## Null entry points and import

Source inspection and the scratch source-contract script checked:

- Import (`ui/PlayKilnApp.kt:333`) and Coach (`:640-641`) still construct the
  one-argument destination, whose category defaults to null.
- Home's ordinary photo opening (`:298`) still uses that default. Continue,
  toolbox and recent-photo actions explicitly pass null
  (`ui/home/HomeScreen.kt:140`, `:191`, `:210`).
- Library opens a project workspace, not AutoCard directly; its Library/Project
  entries were read and left unchanged. No Library category override was added.
- With null, `initialAutoCardFlow` returns the exact previous online or offline
  state. It does not call `AutoCardEngine.suggest(null)`, which would introduce
  `OTHER` instead of preserving the existing unselected state.
- If no photo exists, a strip tap still calls the existing import action
  (`ui/home/HomeScreen.kt:146`). The intended category is **not retained through
  import**. No pending-category state or Import destination change was added.

These are source checks, not exercised on-device flows.

## Observed checks

`node .kiln\scratch\w17-category-check.mjs` initially reported 0 PASS / 4 FAIL
against the broken wiring. After the fix it reported 5 PASS / 0 FAIL, including
the regeneration guard. The actual catalog's first scene ids differ:
`shoes-procedural-studio` and `jewelry-procedural-studio`.
This checks source contracts and catalog data, not execution of Kotlin/Compose.

`AutoCardCategoryHandoffTest.kt` adds four JUnit tests: distinct Shoes/Jewelry
first recipes, all enum values online/offline, null online, and null offline.
They were **not executed**: no standalone Kotlin runner was found, and Gradle is
reserved for the orchestrator. Compilation is **UNVERIFIED**; no Gradle command ran.

`node scripts/check-locales.mjs --slot plinth-product-photos` exited 0 before
and after: 900 default entries and 900 translated keys complete.

`node scripts/verify.mjs --slot plinth-product-photos` exited 1 before and after,
both reporting **122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL**. The only FAIL was the
pre-existing `slot.device` stale build digest. No `--build` was used; both runs
reported zero subprocesses. The final report is `.kiln/scratch/w17-verify-after.txt`.

`& .kiln\scratch\w17-category-grep.ps1` exited 0 and printed:

```text
PASS lastUsedCategory UNUSED_PARAMETER suppression absent
AutoCardScreen.kt:79: lastUsedCategory: AutoCategory?,
AutoCardScreen.kt:83: return lastUsedCategory?.let { initial.chooseCategory(it) } ?: initial
AutoCardScreen.kt:93: lastUsedCategory: AutoCategory?,
AutoCardScreen.kt:116: var flow by remember(photo, lastUsedCategory) {
AutoCardScreen.kt:117: mutableStateOf(initialAutoCardFlow(lastUsedCategory, isOffline))
AutoCardScreen.kt:146: var selectedSceneId by remember(photo, lastUsedCategory) { mutableStateOf<String?>(null) }
PASS lastUsedCategory is applied to the existing flow
```

The runtime falsifier remains for the orchestrator: tap Shoes, return Home, tap
Jewelry, and observe different selected scenes; then enter from Import and Coach
without a category and observe the unchanged choose-category state.

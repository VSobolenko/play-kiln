# W51 - Shared photo journeys

## Implemented paths

The shared object is the photo surface, not its buttons, labels or the entire
destination. `sharedBounds` is used rather than `sharedElement`: thumbnails crop
the source, inspect fits it, and the generated output can contain different pixels.
Bounds move while the two representations crossfade.

All Kotlin paths below are relative to
`apps\2026-10-15-plinth-product-photos\app\src\main\java\com\gexetr\plinthproductphotos\ui\`.

| Journey | Source | Destination | Shared identity |
| --- | --- | --- | --- |
| Home latest/hero -> AutoCard | `home\HomeScreen.kt:305` | `autocard\AutoCardScreen.kt:110` | `PhotoTransitionKey(photo.id)` |
| Home recent photo -> AutoCard | `home\HomeScreen.kt:449` | Same AutoCard source photo | Same source key |
| AutoCard preview -> result | `autocard\AutoCardScreen.kt:449` | `PlayKilnApp.kt:889` | `PhotoTransitionKey(photoId, output = true)` |
| Library thumbnail -> Card inspect | `library\LibraryScreen.kt:327` | `card\CardScreen.kt:100` | `PhotoTransitionKey(id, journey = PhotoJourney.LIBRARY_CARD)` |

The key definition is in `PlayKilnApp.kt:224-230`. It uses stored IDs, never grid
indices, names, file paths, or generated bitmap identities. Source and output are
distinct because AutoCard displays both at once. Library and Home are distinct
journeys so switching top-level tabs does not match their thumbnails.

The Home rail already excludes the latest photo (`drop(1)`), leaving one Home
source endpoint per photo. For imported photos, project ID, item ID and photo ID
are identical (`data\db\Daos.kt:34-51`, relative to the package root).

## Navigation, loading and compatibility

`PlayKilnApp.kt` provides the existing `SharedTransitionLayout` scope through
`LocalPhotoSharedTransitionScope`. Each endpoint uses NavDisplay's
`LocalNavAnimatedContentScope`, not a separately timed visibility animation.
Standalone screen callers without the shared scope keep an ordinary modifier.

AutoCard's source photo is a single `movableContentOf` composition, remembered by
photo ID in `PlayKilnApp.kt:445-456`. It moves between initial image loading,
preset loading, and the prepared screen rather than recreating its shared-content
state at each branch. It can display the stored photo file before PixelBuffer
decoding finishes; a header-only bounds decode supplies the source aspect ratio.
The generated preview and result reuse the existing `FormingCardPreview` through
its modifier parameter; that component's forming logic is not changed.

Library keeps its existing project workspace action on the rest of the tile.
Tapping the preview opens `CardDestination(projectId)` only when that ID resolves
through `ProjectRepository.photo`; otherwise it opens the existing workspace.
Selection mode leaves the preview tap to the parent tile's selection action.
On medium/expanded windows the Library source modifier is disabled
(`PlayKilnApp.kt:572`), since the list remains visible beside Card and cannot be a
disappearing shared endpoint. Card still opens in the detail pane.

Card -> Editor is not a claimed shared journey: Editor is outside this task's
permitted file list and has no new matching endpoint.

NavDisplay's `onBack`, `popTransitionSpec`, `predictivePopTransitionSpec`, and the
manifest's predictive-back declaration are retained. No new BackHandler or
predictive-back interception is added. AutoCard retains its existing photo-swipe
back gesture. Gesture progress/cancellation and visual continuity on a device
remain unverified.

## Evidence and limits

Commands were run from the repository root on 2026-10-17.

```powershell
rg -n "sharedElement|sharedBounds" apps\2026-10-15-plinth-product-photos\app\src\main
node scripts\verify.mjs --slot plinth-product-photos
```

- **MEASURED:** seven real `sharedBounds` invocations across five permitted Kotlin
  files. The endpoint-presence assertion failed before implementation for Home,
  Library, Card and AutoCard, and passed after implementation. This is structural
  coverage, not a rendered-animation test.
- **MEASURED:** the requested source-only verification returned exit 1:
  **122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL** in **17085 ms**. Every `uiux.*` row
  passed, including navigation, motion, adaptive, accessibility and platform.
  The sole failure was `slot.device`: its recorded build digest no longer matches
  changed sources. The device record was not edited.
- **MEASURED:** a compile retry was refused with `SLOT_LOCK_HELD` after 120111 ms,
  naming PID 129808, `verify.mjs --slot plinth-product-photos --build`,
  purpose `Gradle assembleDebug`. The lease was not removed or bypassed.
- **MEASURED:** after the lease cleared, `:app:compileDebugKotlin` through
  `scripts\lib\gradle.mjs` reached Kotlin compilation and failed in 23 seconds.
  The reported errors were in the prohibited `ui\components\PlinthNavBar.kt:139`
  and `:143`: missing `getValue` support for delegated `State<Color>` and
  `State<Float>`. That file was not edited. This is not a successful build claim.
- **UNVERIFIED:** device animation, predictive-back cancellation, and runtime
  loading continuity. Once the independent navigation-bar compilation blocker
  is resolved, build a debug APK using the normal leased Gradle route, install
  it, and exercise the journeys below. No release approval was created or changed.

Runtime acceptance still to perform: open the latest Home photo, an older recent
photo, confirm an AutoCard output, cancel and commit predictive Back on each
leg, then open a Library preview and return. Check portrait and landscape
sources, fast and slow loading, Library selection mode, repeated top-level tab
switches, and a wide Library/Card list-detail layout. Each photo must land at its
matching surface without a second source appearing, and cancellation must restore
the original destination. These are acceptance steps, not recorded passes.

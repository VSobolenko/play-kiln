# W41 - Navigation 3 stable motion policy and shared-transition host

**Status:** source changes delivered. No Gradle build, device run, animation-clock test,
or shared-element journey was run; all runtime and compilation claims remain
**UNVERIFIED**. W40 owns build and device integration. No commit was made and this
pass introduced no new dependency coordinate.

## Changes

- Updated the Navigation 3 version catalog pin from 1.1.7 to **1.2.0 stable**.
  Both `navigation3-runtime` and `navigation3-ui` resolve through that single version
  reference. AndroidX release notes mark 1.2.0 stable on 2026-09-23; no Material 3
  prerelease was added.
- Added `ui/theme/PlinthMotion.kt` as the source for W37's motion specs: 160 ms
  section fade and 180 ms push/pop fade and slide. Entry uses
  `LinearOutSlowInEasing`; exit uses `FastOutLinearInEasing`; tween delay remains
  zero. `snappyNavigationTransition` continues to provide the same section-switch,
  push, pop, and predictive-pop behaviors, now from these shared specs.
- Wrapped `NavDisplay` in `SharedTransitionLayout` and supplied its
  `SharedTransitionScope` to the Navigation 3 host. `plinthPhotoSharedElementKey`
  is the host-side stable key helper for a future pair of photo surfaces.
- Preserved compact header-in-scene behavior and the compact branded bottom-nav FAB;
  the transition host only occupies the existing `NavDisplay` area.

## Shared-photo follow-up

The requested Library thumbnail → Card/Editor shared bounds journey is not wired in
this pass. Library tiles are project previews and their click opens a
`ProjectDestination(projectId)`. Card and Editor entries instead share an imported
`photoId` (`CardDestination(photoId)` → `EditorDestination(photoId)`). A project ID
cannot safely stand in for a photo ID, and the current Library route does not choose
a photo. The host scope and stable photo-key convention are ready, but adding a
transition before both surfaces can identify the same photo would create a false
match. Follow up by defining the library-to-photo route, then apply the same
`plinthPhotoSharedElementKey(photoId)` to its thumbnail and Card/Editor image bounds
using `LocalNavAnimatedContentScope`; test gesture cancellation and completion on
device.

## Claim ledger

| Claim | Label | Evidence / limit |
|---|---|---|
| Navigation 3 runtime and UI resolve to 1.2.0. | MEASURED | `gradle/libs.versions.toml`: both coordinates use `version.ref = "navigation3"` and the pin is `1.2.0`. |
| Navigation 3 1.2.0 is stable and was released 2026-09-23. | ON RECORD | [AndroidX Navigation 3 release notes](https://developer.android.com/jetpack/androidx/releases/navigation3), version 1.2.0. |
| The host uses one Plinth motion policy for all three NavDisplay paths. | MEASURED | `ui/PlayKilnApp.kt` supplies the same `snappyNavigationTransition` to forward, pop, and predictive-pop; `ui/theme/PlinthMotion.kt` defines the 160/180 ms specs. This is source evidence, not compiled behavior. |
| NavDisplay is hosted under SharedTransitionLayout with its shared scope supplied to NavDisplay. | MEASURED | `ui/PlayKilnApp.kt`, `SharedTransitionLayout` and `sharedTransitionScope` argument. |
| A photo shared-content key is stable across surfaces when called with the same photo ID. | MEASURED | `ui/theme/PlinthMotion.kt`, `plinthPhotoSharedElementKey(photoId)`. No destination currently consumes the helper. |
| Library → Card/Editor shared-bounds animation is delivered. | NOT DELIVERED | Library routes by project ID; Card and Editor routes use photo ID. The route and paired modifiers remain a follow-up. |
| The updated project compiles and transitions behave correctly, including predictive-back cancellation. | UNVERIFIED | Gradle and device execution are explicitly out of scope for W41. |

## Integration check

W40/orchestrator should compile against Navigation 3 1.2.0, then exercise repeated
section switches, forward pushes, ordinary pops and predictive-back cancel/commit.
On device, verify the W37 header stays in its animated root scene and the compact
bottom navigation keeps its branded create FAB. After a real Library-to-photo route
exists, add and verify the shared-bounds journey separately.

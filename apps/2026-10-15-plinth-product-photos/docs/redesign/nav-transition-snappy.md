# W37 - Snappy shell navigation

**Status:** source changes delivered; compilation, animation-clock tests and device
frame pacing are **UNVERIFIED**. No Gradle or device command was run: W35 owns that
integration boundary. No dependency, pricing, wallet arithmetic, Studio design,
commit, signing or release change.

## Before / after timing

All new durations below are animation specifications at system animator scale 1,
not measured tap-to-display latency.

| Path | Before | After |
|---|---|---|
| Studio / Library / More replacement | No app transition specification; Navigation 3 fallback. Exact pinned-binary duration and device latency unmeasured. | Incoming fade, 160 ms, no slide or start delay; outgoing scene retained underneath. |
| More -> Settings / About; other pushes | Same unspecified fallback. | Incoming fade + 24 dp end-to-rest slide, both 180 ms and simultaneous; outgoing scene retained underneath. |
| In-app / system pop | No app pop specification. | Incoming scene stationary underneath; outgoing fade + 24 dp slide toward end, both 180 ms and simultaneous. |
| Predictive pop | No app predictive-pop specification. | Same short pop transform supplied to NavDisplay's gesture-aware API. The gesture remains user-controlled; 180 ms is not a cap on finger-down time. |
| Adaptive Library panes on wide windows | Material adaptive scene/pane motion. | Retained. Internal pane motion may override NavDisplay's fallback and is not claimed to take 180 ms. |

Material's `LinearOutSlowInEasing` / `FastOutLinearInEasing` distinguish entry and
exit without overshoot. Distance is density-aware and mirrored for RTL.
`ContentTransform` and NavDisplay APIs already on the classpath suffice; there is
no MotionScheme/theme change and no Expressive alpha.

For context only, the AndroidX `androidx-main` Android implementation read during
this task specifies a **700 ms** forward/pop crossfade:
[NavDisplay.android.kt](https://github.com/androidx/androidx/blob/androidx-main/navigation3/navigation3-ui/src/androidMain/kotlin/androidx/navigation3/ui/NavDisplay.android.kt).
That moving branch is **not proof** of the slot's pinned 1.1.7 binary. Consequently
no measured baseline duration, percentage improvement or runtime speedup is claimed.

## Path / recomposition audit

- Bottom bar and rail call `selectSection` immediately. It maps a section to a
  destination and calls `resetToRoot`; selection is derived from the first stack
  entry, not a second delayed selection state. The compact bar has no custom
  selection animation. The rail keeps Material's own selection feedback.
- More's Settings, About and Support rows immediately push a destination (Support
  uses Settings). About's back and Settings' back use `navigateBack`; NavDisplay's
  `onBack` uses the same callback. Root Back to Studio replaces the root.
- **Settings -> About is an AlertDialog, not a NavDisplay push.** Closing it
  returns to Settings without mutating the stack. AboutScreen has no direct
  Settings action. Their existing routes were not invented or redesigned.
  Settings' image-defaults `AnimatedVisibility` is an inline expansion, not
  navigation, and is untouched.
- Home's initial photo work is already launched on `Dispatchers.IO`. Library
  uses Paging and suspend Room reads; Settings collects a DataStore-backed state
  flow. More and About are resource/layout composition. No blocking I/O was
  found in `selectSection` itself. This source audit is not a main-thread profile:
  repository construction, initial composition and image decode costs remain
  device observations, not proven-free costs.
- W15 already replaced the non-terminating drain described in
  `navigation-hang.md`. The remaining `resetToRoot` / `replaceDetail` loops
  remove one entry while `size > 1`, never empty the stack, and terminate.
  Root-to-root switching does not enter either loop. No measured evidence
  justified changing stack semantics, batching mutations, caching entire tabs,
  or removing the adaptive scene strategy.
- Navigation 3 composes incoming/outgoing scenes during motion. That overlap is
  intentional, not evidence of duplicate route execution. No duplicate-composition
  or list-detail-strategy-thrash performance claim is made.

## Layout discontinuity and blank-frame prevention

**Observed in source:** the outer scaffold used `if (isTopLevel)` for its top
bar. Pushing Settings/About removed that bar immediately, changing NavDisplay's
parent padding/height while the outgoing root was still animating. Pop did the
reverse. The content transition could not animate that parent geometry change.
This establishes a layout discontinuity, not the measured cause of every missed
frame in the owner's recording.

On compact windows, `SectionRoot` now owns each root's existing PlinthTopBar
inside the animated entry. NavDisplay's outer top-bar slot remains empty on both
sides of a push/pop, so it no longer changes the viewport at that boundary.
The existing status/IME/navigation inset handling stays in the outer scaffold.
This relocates the header; it does not restyle Studio.

Wide windows keep the original shared top bar/rail placement and adaptive Library
pane metadata. The wide top-level/detail boundary can still change parent
geometry; its runtime effect is **UNVERIFIED** and deliberately not hidden by a
claim that all window sizes are jank-free.

Every entry receives a themed, opaque background via a remembered
`NavEntryDecorator`, while retaining Navigation 3's saveable-state decorator.
Retaining the outgoing scene during entry therefore does not leave transparent
More/Studio content showing the previous screen after the fade. Entry fades have
no delay and do not fade the outgoing surface to the background first. During
pop, the incoming surface is already present beneath the departing surface.
NavDisplay still owns z-order, interruption, lifecycle and predictive-back handling.

## Accessibility

`LocalAccessibilityManager`'s public `AccessibilityManager` interface exposes
recommended **content timeouts**, not a reduced-motion flag. Those timeouts are
not motion durations and are not used as such.

The transforms use normal Compose animation specs and do not replace the
recomposer/coroutine `MotionDurationScale`, install a scale of 1, or wait in a
timer before navigation. Android's window recomposer supplies and observes the
system animator duration scale; scale 0 disables these time-driven animations.
This is a source/API-grounded behavior, **not a device-tested accessibility PASS**.
See [AccessibilityManager](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/platform/AccessibilityManager.kt)
and [WindowRecomposer.android.kt](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/WindowRecomposer.android.kt).

## Claim ledger

Kotlin paths below are relative to `app/src/main/java/com/gexetr/plinthproductphotos/`.

| Claim | Label | Evidence / limit |
|---|---|---|
| Explicit forward, pop and predictive-pop transforms are wired. | MEASURED | `ui/PlayKilnApp.kt:325-340`; specs at `:1207-1231`. Source values are 160/180 ms with no delay. |
| Root sections retain pane metadata and add a transition-only marker. | MEASURED | `ui/PlayKilnApp.kt:343-345,373-375,509-512`. |
| Compact header moves with its root, not the display's parent padding. | MEASURED | `ui/PlayKilnApp.kt:290,347-352,377-382,531-536,1188-1205`. |
| Retained entries have a background and saveable-state decoration. | MEASURED | `ui/PlayKilnApp.kt:219-225,320-323`. |
| Stack helpers and the root-preserving W15 fix are unchanged by W37. | MEASURED | `ui/PlayKilnApp.kt:258-273,1262-1271`; no new pop loop or blocking call. |
| Settings/About includes a dialog-only path. | MEASURED | `ui/settings/SettingsScreen.kt:404-431`; `ui/about/AboutScreen.kt:25-102`. |
| Source whitespace check is clean. | MEASURED | `git diff --check -- apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt` exited 0. Not a Kotlin/build test. |
| Source-only UI checks pass without starting a build. | MEASURED | `node scripts\verify.mjs --slot plinth-product-photos --only ui` exited 0: 23 PASS, 6,505 ms, subprocesses 0. This does not compile Kotlin, execute instrumentation tests or measure frames. |
| Timing/interrupt test coverage is present. | MEASURED | `app/src/androidTest/java/com/gexetr/plinthproductphotos/ui/NavigationMotionTest.kt`: section replacement, Settings and About push/pop, interrupted section switch, and Back during Settings entry. Tests exercise the production transform in NavDisplay, not the whole app or adaptive panes. The supplied working tree already contained the production patch and initial tests; test-first provenance is not claimed. |
| Tests fail before and pass after; pinned API compatibility/build. | UNVERIFIED | Neither RED nor GREEN was run: Gradle/device use is forbidden for this worker. Source review against official APIs is not compilation. |
| Owner-visible speed, frame pacing, no blank hitch, RTL, predictive gesture cancel/commit and disabled motion. | UNVERIFIED | Require W35/orchestrator's integrated app on a device. No runtime jank-free verdict. |

## Integration acceptance

The orchestrator must compile the app and the new instrumentation test, then run
`com.gexetr.plinthproductphotos.ui.NavigationMotionTest` using the existing
device/test route. Its falsifiable timing assertion is that outgoing semantics
are removed within 220 ms of animation-clock progress (after one startup frame);
a retained outgoing node fails the test. The suite is not a frame-time benchmark.

On the integrated debug APK, rapidly alternate all three tabs; push/pop Settings
and About from More; open/dismiss Settings' About dialog; interrupt a transition;
cancel and commit predictive Back; repeat with animator scale 0, RTL and large
text. Inspect a frame trace for both cold and warm navigation and check header
position throughout, not only after settling. Repeat on a wide window and verify
Library's list/detail behavior and the retained shared-header boundary.
Only that run can establish whether the owner's remaining stutter is gone.

## Fixer continuation

The working tree supplied to this pass already contained the shell transition,
background-decorator and compact-header changes above, plus the initial motion
tests and this note. They were reviewed and preserved, not overwritten or
attributed to a new implementation. This pass added About push/pop and immediate
Back-during-entry timing cases and corrected the unsubstantiated test-first claim.
No runtime performance defect beyond the source-visible compact-header geometry
change was measured. The W15 stack helpers and adaptive strategy remain intact.

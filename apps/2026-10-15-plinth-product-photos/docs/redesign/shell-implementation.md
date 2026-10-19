# Shell implementation

## What changed

- `ui/PlayKilnApp.kt` now hosts the existing `NavDisplay` in one `PlinthScaffold`.
  Studio, Library and More are the three root destinations; the existing detail
  routes remain available on the same back stack. Library and Project retain
  their list/detail metadata. The center create action opens the existing import
  route. The balance opens Wallet as a nested Library destination.
- `ui/components/PlinthNavBar.kt` supplies the three-item phone bar and the
  matching rail. Medium and expanded width classes use the rail; compact windows
  use the bottom bar. The shell's create action is separate from the three
  destinations.
- `ui/components/PlinthTopBar.kt` supplies a balance chip on each root tab.
  It reads the same `WalletViewModel` state passed to Wallet; an offline or
  not-yet-loaded state is shown as an unknown balance, not a cached number.
- `ui/components/PlinthCostChip.kt` is reused by `PlinthToolCard`; the card's
  label overload keeps the 2K/4K cost pair in the same chip vocabulary.
  Tool costs are looked up from `StudioOperationCatalog`, not copied into UI
  constants.
- `ui/home/HomeScreen.kt` puts the user's latest photo (or an empty plinth)
  first, followed by scenes, a two-column tool gallery and recent/workflow
  rails. The body has **zero words before its first image-bearing surface**;
  the shell's section title and balance are chrome, not home content. This is
  within the 12-word / first-320-dp rule in `ai-visual-language.md` §5.6.
- `ui/home/HomeSceneStrip.kt` loads verified recipes from the bundled
  `autocard/scene-recipes.json` and draws thumbnails from their recipe colors.
- `ui/home/MoreScreen.kt` places Settings, About and support access under More;
  support routes to Settings' existing support information.
- `ui/components/PlinthStage.kt`, `ui/theme/Color.kt`, `ui/theme/Theme.kt` and
  `ui/theme/Type.kt` keep the new chrome and home surfaces on the slot palette,
  add the measured divider tones and surface tint, and define the existing
  missing `titleSmall`, `labelMedium` and `labelSmall` typography tokens.
- `res/values/strings_shell.xml`, `res/values-ru/strings_shell.xml`,
  `res/values/strings_home.xml` and `res/values-ru/strings_home.xml` provide the
  new labels in English and Russian.

## Decisions

There are **three tabs**: Studio is the make job, Library is where work is
kept, and More holds configuration. This matches the make/keep/configure
grouping in `competitor-teardown.md` §4.1. A fourth Create tab would duplicate
the raised, persistent create action; a Wallet tab would turn a status and
purchase surface into a destination even though the teardown places the balance
in chrome and top-up behind it. Tapping the balance selects Library and opens
Wallet there.

The layout follows Android's adaptive window guidance recorded by the teardown
and `ai-visual-language.md` §3.4: compact uses bottom navigation, medium and
expanded use a navigation rail. The existing Library/Project list-detail
relationship remains declared.

The visual treatment uses the slot's graphite, paper, clay and verdigris roles,
including the AI visual-language document's derived divider colors. No new hue
or price was introduced. Real blur, a separate Wallet tab, extra destinations,
and text-only scene choices were rejected. The shell uses the existing
`PlinthScaffold`; its inset behavior was not changed.

## Limits

The scene cards open the existing AutoCard flow for the latest photo, but that
route currently ignores its `lastUsedCategory` parameter and has no input for
the selected recipe. Making a tapped scene preselect its exact recipe requires
an AutoCard route/screen change outside this worker's file list.

The shell displays the one `WalletViewModel` state also used by Wallet. Its
current refresh path persists successful server balances through
`WalletSessionStore.updateServerBalance`, but `WalletViewModel.refreshWallet()`
does not call `WalletPurchaseCoordinator.refreshWallet()` and exposes no
refreshing state. The initial/offline state is rendered as unknown, but a
previously known balance can remain visible during a later refresh. Fully
binding the chip to the persisted session and showing unknown throughout a
refresh requires a loading/session flow from the wallet implementation. The
wallet files are owned by another worker and were not changed.

No Gradle command was run, and no device was available to verify the rendered
layout. Compilation and the A40 appearance therefore remain unverified.

# Wallet placement (W19)

## Bug and decision

The former `openLibraryWallet()` in `ui/PlayKilnApp.kt` called
`selectSection(PlinthSection.LIBRARY)` before pushing `WalletDestination`.
That reset the navigation stack to Library, changed the selected tab, and
discarded the caller's return destination.

Wallet is a money destination, not a preference or a Library feature.
`openWallet()` now only calls `backStack.push(WalletDestination)`. It preserves
the current section's root and every existing entry. The bottom bar and wide
navigation rail derive their selection from that root, so neither switches tabs
when Wallet opens. Wallet and system Back use `navigateBack()` to pop one entry,
returning to the exact calling screen, including a nested Settings or Studio
tools screen. The existing duplicate-top guard and root-preserving pop are
unchanged.

Explicitly selecting a section while viewing Wallet still resets to that
section's root through the existing `selectSection()` behaviour. This does not
introduce saved per-tab stacks or a new modal navigation system.

## Entry points

Every Wallet callback in `ui/PlayKilnApp.kt` uses the same `::openWallet` helper:

| Surface | Callback |
| --- | --- |
| Top-bar balance chip (Studio, Library, More) | `PlinthTopBar.onOpenWallet` |
| Home money entry | `HomeRoute.onOpenWallet` |
| More's first action, alongside Settings | `MoreScreen.onOpenWallet` |
| Existing Settings route contract | `SettingsRoute.onOpenWallet` |
| Studio tools | `StudioRoute.onOpenWallet` |

More displays **Credits / Wallet** in English and **Кредиты / Кошелёк** in
Russian, with a balance-and-purchase summary and the existing plus icon.
Its actions scroll so the extra row does not hide the remaining actions on
short windows or with larger text. No prices or purchase behaviour changed.

`SettingsScreen.kt` is deliberately untouched: removal or demotion of its
purchase row belongs to the concurrent Settings worker. This change makes
Settings unnecessary as a path to money without removing its route callback.

## Rejected alternatives

- **Wallet as a fourth tab:** rejected by the owner earlier, as recorded in the
  W19 task brief. The existing three-section navigation remains unchanged.
- **Wallet only inside Settings:** rejected in the owner's 2026-10-17 feedback
  quoted in W19. The balance chip and More now provide direct entry points.
- **Switch to Library to host Wallet:** the redirect defect reported in W19;
  Library is selected only when the user explicitly navigates there.

## Evidence boundary

The source-only checks do not establish compilation, rendered layout, device
Back behaviour, or a successful purchase. No Gradle or build command is part of
W19; compilation and on-device observations remain UNVERIFIED.

# W90 — Generate enablement and tool-screen chrome

## Status
Code fixes built and unit-tested. **Device proof was NOT obtained**: `uiautomator dump` and `screencap` were refused ("Permission denied because no interactive user response was available") and were not worked around. Blind `input tap` did not reach the picker. `.kiln/scratch/device-plinth-w90/` is empty.

## canPressGenerate chain — runtime values
Not measured. A temporary `W90DIAG` log was installed, but the screen never reached the tool route, so no line was printed. It has been removed. Every value below is UNVERIFIED.

| Boolean | Runtime value | Code-analysis finding |
|---|---|---|
| balance != null | not measured | **Likely false**: Studio state hydrated once in `refresh()`; if it ran before the wallet session existed it threw SESSION_REQUIRED and balance stayed null. `refreshQuote()` also nulled it. |
| hasPhoto | not measured | **Likely false**: `choosePhoto()` is a `task{}` that returns silently when `busy`, so the auto-select was dropped during `refresh()`, leaving `photoId` null. |
| displayedCredits, quoteMatches, operationEnabled, killSwitchClear, dailyCapClear, rowBlocked, providerReady, promptValid, busy, hasPending | not measured | no defect found |

## Root cause (inferred, UNVERIFIED on device)
Stale Studio state: null balance and null photoId. The safety conditions were left unchanged.

## Fixes
- `StudioViewModel`: `init` collects `sessionStore.balance` (the Wallet source) into `state.balance`, so it stays live. `refreshQuote()` no longer nulls balance or reserved. `refresh()` falls back to the first photo when no photo is bound and nothing is pending.
- C9: `PlayKilnApp` no longer wraps `StudioRoute` in `WithShellBack` (the "Return to the studio" banner). The top bar has a back icon and a one-line tool name.
- C1: `HomeToolCard` uses `aspectRatio(1f)` with equal weights, so every card has the same size. The 556 px vs 316 px reading is probably a viewport artefact, which is UNVERIFIED.
- C16: the Expand row was removed from `studioRows`; re-add it when `/config` enables O-09. Tests updated.
- `device-verification.json` rewritten to the validator schema: 9 string observations taken from W89, the current APK sha256, no `buildDigest`. It describes the W89 revision's observations.

## Claim ledger
| Claim | Status | Evidence |
|---|---|---|
| `:app:assembleDebug` + `:app:testDebugUnitTest` pass | MEASURED | `node .kiln\scratch\w90-gradle.mjs` -> status 0, BUILD SUCCESSFUL |
| `slot.device` validates | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos --only policy` -> PASS |
| Device is SM-A165F | MEASURED | adb getprop earlier in session |
| Generate enabled on Scene / Relight / Recolour | UNVERIFIED | no device capture |
| Banner gone, cards equal height on device | UNVERIFIED | no device capture |
| Root cause is null balance / photoId | UNVERIFIED | code reading only |

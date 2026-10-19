# W78 — Studio tool workflow

## Intent

Keep the seller's selected photo in view while they choose one available edit,
optionally add guidance, and see the exact credit cost on the Generate button.
Starting an edit with enough credits is one tap; only a known insufficient
balance opens a short wallet prompt. The original remains visible while work is
in progress and is paired with the result for before/after review.

## Interaction

- The picker and selected source photo remain the first Studio surface.
- Only tools enabled by the current server configuration and local policy are
  offered; this changes presentation, not the tool catalogue or server prompt.
- Choosing a tool opens its setup with the photo name, optional guidance and
  `Generate · N credits`. Guidance is capped at 240 characters and can remain
  empty.
- A sufficient balance starts the existing request directly. A known
  insufficient balance shows the shortfall and a wallet action. An unknown
  balance disables submission rather than guessing.
- One user-facing progress indicator replaces the raw server-phase display.
  Developer error codes, raw server messages and output identifiers are not
  rendered.
- Delivered output shows the selected source beside the generated result;
  existing apply, save and discard actions are preserved.

## Boundaries

No server prompt, request binding, billing or navigation behavior is changed.
The optional prompt remains empty by default. Home/AutoCard, cutout, profile and
PlayKilnApp navigation are outside this change.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The selected photo, tool setup, optional instruction and cost-labeled Generate action are composed in the Studio route | MEASURED | `StudioRoute.kt:111-118, 332-409, 519-632`; `StudioWorkflowTest.kt:28-63` |
| Sufficient balance starts directly; known insufficient balance opens the wallet prompt; unknown balance cannot submit | MEASURED | `StudioRoute.kt:387-410, 584-632`; `StudioViewModel.kt:80-116`; `StudioCreditGateTest.kt:8-70`; `StudioWorkflowTest.kt:68-111` |
| The result view pairs the source image with the generated image | MEASURED | `StudioRoute.kt:168-193, 484-516`; `StudioWorkflowTest.kt:140-162` |
| Raw phase names, server error details and saved-output IDs are not rendered by the Studio route | MEASURED | The bounded `rg` search for `lastServerState`, `terminalError`, `errorMessage`, and output-ID text returned no matches in `StudioRoute.kt` |
| English and Russian strings exist for the new action, progress, comparison and insufficient-credit UI | MEASURED | `values/strings_studio_live.xml:14-23`, `values-ru/strings_studio_live.xml:14-23`; ElementTree parse and locale-parity check passed |
| W78 preserves W73's optional-instruction payload behavior: blank guidance is omitted and nonblank guidance is trimmed | MEASURED | `StudioRoute.kt:397-408`; `StudioViewModel.kt:171, 190` |
| The flow has been exercised on a device and its visual layout is approved | UNVERIFIED | `adb devices -l` showed only an offline transport; requires an online device and `connectedDebugAndroidTest` plus visual review |

## Verification note

The production Kotlin compile and slot JVM suite passed: `node scripts/run-slot-tests.mjs --slot plinth-product-photos --rerun` (300 tests). Android-test sources compiled successfully with `:app:compileDebugAndroidTestKotlin --no-daemon` from the slot root. Instrumentation was not run because `adb devices -l` showed only an offline transport.

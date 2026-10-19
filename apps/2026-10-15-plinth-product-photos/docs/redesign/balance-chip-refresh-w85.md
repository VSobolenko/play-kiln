# W85 - Balance chip refresh after Studio spend

## Cause and narrow fix

The W84 device report records Studio spending 5 credits (9 to 4), Profile's
body showing 4, and the Profile/Library shell chips retaining 9
(`device-pass-w84.md`, Outcome and Journey).

The shell previously read `walletViewModel.state.balance`, gated by that
state's `offline` flag. That model refreshes on initialization, activity resume
and wallet actions, not on Studio's cache writes. Studio already fetches the
wallet and writes `WalletSessionStore.updateServerBalance` in `refreshQuote`,
including after settlement. Profile independently fetches the wallet, updates
the same cache, and displays the response in its own state.

The chip now observes the existing wallet DataStore's balance stream instead
of the wallet screen's response snapshot. `WalletViewModel.chipBalance` exposes
that stream, and `PlayKilnApp` collects it once with lifecycle awareness.
All five existing shared-header bindings use that one value: the wide-window
header and the Home/Studio, More, Profile and Library section headers.
Studio's own balance continues using its existing refreshed quote. Batch hub
and batch detail do not have separate balance-chip bindings; returning from
them uses the same live shell value. No new chip, header or navigation was added.

The stream emits only a saved session's server-provided balance: zero remains
zero; an absent/cleared session is unknown (`null`). A saved balance can appear
while the wallet screen is offline, consistent with Profile's cached balance;
it is not a new server confirmation. Read I/O errors are logged and display an
unknown chip rather than zero. Wallet purchase eligibility, ledger/reservation
metadata, server charging and Studio's refresh/settlement logic are unchanged.

Chosen over navigation-specific refresh callbacks: observing the common store
also covers Profile refreshes, wallet purchases/recovery and background balance
writes without adding network requests or coupling Studio to the shell.

## Regression and commands

`WalletBalanceRefreshTest` uses the production session-store methods and balance
flow with an in-memory Preferences DataStore. Two store instances share that
DataStore, as the Android Context-backed instances do. One continuously active
chip-facing collector observes **9 -> 4 -> 24 -> 0 -> null** after a Studio
balance update, wallet refresh, zero balance and session clearing. It does not
resubscribe or navigate to obtain 4.

Negative control: a temporary one-shot `flow { emit(current()?.balanceCache) }`
implementation emitted 9 but timed out waiting for 4; the targeted Gradle run
exited 1 with `TimeoutCancellationException`. This was a one-shot reproduction
of the stale snapshot mechanism, not a claim that the new test compiled against
the untouched original API. An initial file-backed DataStore fixture also hit a
JVM/Windows rename error; it was replaced with the in-memory DataStore fixture,
not treated as proof of the balance defect.

From the repository root, the successful focused run used the shared Gradle
route (which acquires the slot lease):

```powershell
node --input-type=module -e "import { runGradleTasks } from './scripts/lib/gradle.mjs'; const r=runGradleTasks('D:\\Python\\PlayKiln\\apps\\2026-10-15-plinth-product-photos',[':app:testDebugUnitTest','--tests','*Wallet*Test','--tests','*Studio*Test','--tests','*Profile*Test','--rerun','assembleDebug']); console.log(r.result.stdout); console.error(r.result.stderr); console.log('elapsedMs='+r.elapsedMs); process.exitCode=r.result.status ?? 1;"
```

Observed: exit 0, `BUILD SUCCESSFUL`, 28,798 ms; 50 tests, zero failures,
errors or skips. `assembleDebug` executed and produced
`app\build\outputs\apk\debug\app-debug.apk`, SHA-256
`6191fe16c207be73959f691e3bc1a35e4a4013aaa28ad5431fc42a8daf1c453a`.
Existing adaptive-window deprecation warnings remain outside this wiring fix.

`node scripts/run-slot-tests.mjs --slot plinth-product-photos --rerun`:
exit 0, **313 tests, 0 failures, 0 errors, 0 skipped**, 15,845 ms.

`node scripts/verify.mjs --slot plinth-product-photos`: exit 1,
122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL. The failure is `slot.device`: the W84
device record's build digest no longer matches the edited sources. The
UNVERIFIED rows are `pack.inventory`, `pack.media` (no store assets) and `build`
(this command did not request a build). The separate build above is measured
build evidence, not a device pass or a green release verdict.

Final `node scripts/verify.mjs --slot plinth-product-photos --build`: exit 1,
**123 PASS, 2 UNVERIFIED, 4 SKIP, 1 FAIL**. `build` PASS:
`assembleDebug in 3487 ms under JDK 17`. Only `slot.device` fails; the two
UNVERIFIED rows remain the absent store assets. Full output is preserved in
`.kiln\scratch\copilot-plinth-w85-balance-chip-refresh\verify-w85-build.txt`
at the repository root.

No device journey was run and `device-verification.json` was not edited.
The post-fix on-device appearance remains UNVERIFIED; a fresh install and the
W84 spend/navigation journey would decide it.

Measured elapsed through final verification: **6m 02s**, from the task timestamp
`2026-10-17T20:21:00.323+03:00` to `Get-Date -Format o` output
`2026-10-17T20:27:02.0681927+03:00`.

## Claim ledger

Paths below are relative to the slot unless a command starts at repository root.

| Claim | Label | Evidence |
|---|---|---|
| The stale-chip symptom was observed during W84, not during W85. | ON RECORD | `docs/redesign/device-pass-w84.md`, Outcome and Journey: Studio 9 to 4; Profile body 4; chip 9. |
| Studio and Profile already publish refreshed balances to the common cache. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioViewModel.kt:239-246,370`; `ui/profile/ProfileViewModel.kt:135-145` under the same package root. |
| Chip-facing balance now follows the session cache reactively. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/data/session/WalletSessionStore.kt:23-29`; `ui/wallet/WalletViewModel.kt:45-49`; `ui/PlayKilnApp.kt:260,324,385,415,432,555` under the same package root. |
| A subscribed chip-facing stream receives the charged balance and subsequent refreshes. | MEASURED | `app/src/test/java/com/gexetr/plinthproductphotos/data/session/WalletBalanceRefreshTest.kt:19-44`; negative-control exit 1; focused run exit 0, 50 tests; complete suite exit 0, 313 tests. |
| Debug assembly succeeded with the lease available. | MEASURED | Shared Gradle command above: `:app:assembleDebug`, `BUILD SUCCESSFUL`, exit 0, 28,798 ms; APK hash above from `Get-FileHash -Algorithm SHA256`. |
| Slot verification is not green after the source change, although the build passes. | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos --build`: exit 1; 123 PASS, 2 UNVERIFIED, 4 SKIP, 1 FAIL; build PASS; `slot.device` reports a digest mismatch against W84. |
| Chips display the new balance on the physical device after a spend. | UNVERIFIED | Requires installing the new debug APK and repeating W84's spend, Profile, Library and Home navigation; no device PASS asserted. |

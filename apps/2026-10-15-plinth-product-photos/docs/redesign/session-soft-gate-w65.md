# W65: wallet session soft gate

## Change

`MainActivity.kt` always composes `PlayKilnApp` inside the configured theme.
Session bootstrap remains in `onStart`; retry calls the same bootstrap.
A failed or connecting session no longer replaces the navigation tree.

The new `ui/session/SessionStatusBanner.kt` sits below the app in a weighted
column, rather than overlaying navigation controls. Connecting shows a thin
progress indicator; failures show their specific explanation and retry.
"Browse without wallet" dismisses the banner, including while connecting.
Dismissal survives activity recreation and does not change the session or grant
credits. A ready session has no banner. After dismissal, foregrounding the app
still attempts bootstrap through `onStart`; there is no new in-flow session sheet.

NETWORK failures select `session_network_debug` only when `BuildConfig.DEBUG` is
true. Both English and Russian debug resources name `127.0.0.1:3101`, the local
server, and `adb reverse tcp:3101 tcp:3101`. Release keeps `session_network`.
The old full-screen `SessionConnectionScreen` is unchanged and no longer called
by the activity.

Files changed, relative to this slot:

- `app/src/main/java/com/gexetr/plinthproductphotos/MainActivity.kt`
- `app/src/main/java/com/gexetr/plinthproductphotos/ui/session/SessionStatusBanner.kt`
- `app/src/main/res/values/strings_session.xml`
- `app/src/main/res/values-ru/strings_session.xml`
- `app/src/test/java/com/gexetr/plinthproductphotos/ui/session/SessionSoftGateTest.kt`
- `docs/redesign/session-soft-gate-w65.md`

## Reproduce on a device (not performed by this worker)

1. Use a fresh debug installation with no server session and no local server or
   ADB reverse mapping. Launch the app.
2. Confirm Studio appears immediately, including while the session connects.
   After failure, confirm the banner describes the local debug server, not a
   mobile-internet problem. Repeat using Russian.
3. Open Library and More / Settings without connecting a wallet. On a fresh
   installation, Library should show its empty state. Dismiss the banner with
   "Browse without wallet" and confirm navigation remains usable.
4. Before dismissing the banner, start the local server on port 3101 and run
   `adb reverse tcp:3101 tcp:3101`. Tap retry; a successful session removes the
   banner without replacing the app shell. Alternatively, foreground the app
   after dismissal to trigger the existing bootstrap.
5. Without a session, attempt a paid operation and confirm that it refuses rather
   than generating or inventing credits.

## Evidence and limits

The JVM regression covers debug/release NETWORK mapping, other failure messages,
unconditional activity content, and the existing Library / Settings destinations.
The content checks are source-contract tests, not a rendered Compose UI test.

Source inspection confirms the retained paid-operation guards:
Cutout checks `sessionStore.current()` before starting work;
Studio throws `SESSION_REQUIRED`; AutoCard requires a session for its quote and
recognition; Wallet maps a missing session to offline and gates purchases.
LibraryViewModel reads its local repository without a wallet dependency.
None of these files was edited.

Validation command from the repository root (uses the actual positional
`runGradleTasks` API; do not run when another worker holds the slot lease):

```powershell
node --input-type=module -e "import path from 'node:path'; import {runGradleTasks} from './scripts/lib/gradle.mjs'; const r=runGradleTasks(path.resolve('apps/2026-10-15-plinth-product-photos'), [':app:compileDebugKotlin',':app:testDebugUnitTest','--tests','*Session*','--rerun-tasks']); console.log(r.result.stdout); console.error(r.result.stderr); process.exitCode=r.result.status ?? 1;"
```

Observed initial execution: exit 0, `BUILD SUCCESSFUL`, 28 tasks executed,
112190 ms wrapper elapsed. Existing session bootstrap tests were included by
`*Session*`.

Final-source rerun used the same tasks and filter with `--rerun` on the test task
instead of `--rerun-tasks`: exit 0, `BUILD SUCCESSFUL`, 3 tasks executed and 25
up-to-date, 9047 ms wrapper elapsed. The test task executed in this invocation.
Its XML reports 19 tests, zero failures and zero errors: SessionBootstrapTest
(10), SessionContractTest (2), LocalCompositionSessionTest (3), and the new
SessionSoftGateTest (4). The new nullable-path warning seen in the initial run was
removed before this rerun.

The previous indexed `MainActivity.kt`, read with `git show :<path>`, contains the
Ready-only conditional and full-screen connection call. Applying the new
unconditional-content contract to that source rejects it. `git diff --check`
reported no whitespace errors in the tracked edited sources.

Elapsed wall-clock reading: 318.1 seconds from the dispatch timestamp to the
final evidence collection; the two Gradle invocations total 121237 ms.

No device interaction, release artifact, approval, or store asset was produced.

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| Activity no longer replaces local browsing with a wallet gate | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/MainActivity.kt:69-77` |
| Debug NETWORK copy names the local server and ADB reverse | MEASURED | `ui/session/SessionStatusBanner.kt:36`; both `strings_session.xml:8` |
| Library / Settings remain in the existing local navigation shell | MEASURED | `ui/PlayKilnApp.kt:427-428,582`; `ui/library/LibraryViewModel.kt:70-76` |
| Final session JVM execution reports 19 tests with zero failures/errors | MEASURED | `app/build/test-results/testDebugUnitTest/TEST-*Session*.xml`; `:app:testDebugUnitTest --tests *Session* --rerun`, exit 0 |
| Actual first-launch rendering and paid refusal on a phone | UNVERIFIED | Orchestrator/device worker must perform the reproduction steps above |

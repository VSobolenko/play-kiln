# W98a — Scene / Relight dead buttons

## Cause (source-derived; no on-device re-run in this wave, no Gradle)
Before the fix, `StudioRoute.kt` `onGenerate` was
`if (state.hasPending) model.retry() else model.start(...)`.
After a successful Recolour the leftover `StudioPending` (resultPath + receipt, `charged == credits`) stays on
disk and `state.hasPending` stays `true` until the user keeps/discards the result. Picking Scene or Relight is
still allowed (`blocksNewOperation` is false once delivery is confirmed), so the button was enabled, but the tap
went to `retry()` → `execute(<the old Recolour pending>)` (`StudioViewModel.kt`, `resultPath != null` branch),
which only re-decodes the saved Recolour file and re-settles it. No upload, no `submitJob`, no error, no balance
change, UI recomposes (scroll snap), prompt text kept — exactly the S21–S23 symptoms. Recolour was the *first*
operation of the session, so `hasPending` was false and it took the `start()` branch.

Second defect: a real failure was shown as one generic line at the *bottom* of the LazyColumn (off-screen), with
no code and no retry; `task{}` also did not catch `IllegalStateException`/other exceptions.

## Fix
- `StudioViewModel.generate(op, prompt, credits)` (new, the only entry): resumes via `retry()` only when the
  pending job is unresolved (`!canReplace`) AND of the same operation; otherwise `start()`. `start` is now private.
  `StudioRoute.kt` and `GeneratedSceneRoute.kt` both call `generate`.
- Shared `StudioFailureNotice` (StudioRoute.kt): generic line + "What failed: <server message or code>" +
  retry-wait + **Try again** button; placed directly under the generate panel (visible, not at list end); used by
  Recolour, Relight, Scene, Expand (one composable, one call site per surface). Catch-all
  `UNEXPECTED_FAILURE` in `task{}` guarantees every exception ends in a visible error.
- Strings `studio_live_error_detail` in `values/` and `values-ru/`. Scene suggestion chips (sea/forest/campsite)
  still exist in both locales; no "charged/spent" line in StudioRoute.

## Server vs client
Server registry (`server/src/registry.ts`) has O-07, O-08, O-10 `enabled: true`; O-09 stays disabled. Scene/Relight
send only `prompt` (no required extra fields; only Expand sends `canvas_size`). Prior W94/W96/W97 live direct
requests (cited in the task) succeeded for the scene prompt. I did **not** issue a new live request in this wave:
that comparison is UNVERIFIED here; the client-side fault is established from the code path above.

## Forced-failure test
`app/src/test/.../test_studio_dead_button_w98a.py` (5 source-contract tests, pass): route never routes a new op
to `retry()` on `hasPending`; `generate` resumes only same-op unresolved; catch-all present; single shared notice
placed above results with retry; both locales carry strings/chips. A runtime forced failure (bad endpoint →
visible "What failed: NETWORK_FAILURE" + Try again) needs the integrator's compile + device pass: UNVERIFIED.

## Claim ledger
| Claim | Status | Evidence |
|---|---|---|
| Old `onGenerate` sent `hasPending` taps to `retry()` | MEASURED | git diff of StudioRoute.kt `onGenerate` |
| `retry()` with a delivered pending does no network call | MEASURED (read) | StudioViewModel.kt `execute`: `resultPath != null` branch |
| That branch explains S21–S23 | INFERRED | matches symptoms; not reproduced on device |
| New contract tests pass | MEASURED | `python test_studio_dead_button_w98a.py` → 5 OK |
| Server O-07/O-08/O-10 enabled | MEASURED | registry.ts lines 51–67 |
| Compiles | UNVERIFIED | no Gradle by instruction |
| Live direct request this wave | UNVERIFIED | not run |
| `test_studio_wiring.py` (2) and `test_studio_tool_binding_w73.py` (1) fail on strings I did not touch (`items(state.savedVariants`, `STUDIO_PROVIDER_RESULTS_READY = false`, `require(prompt.length <= 2000)`) | MEASURED | unittest output; stale vs earlier uncommitted work |

# W43 - Batch as a live production queue

Applies task 5 in `production-android-stack-2026.md` section 4. Source changes
only; compilation and rendered behavior are **UNVERIFIED**. No Gradle, device,
dependency, pricing, release, signing, or commit operation was performed.

## Presentation and truth

The existing state-colored rails, named badges, terminal-result summary, Coil
previews and keyed Foundation `animateItem()` rows are retained. W43 adds
explicit repeat-attempt presentation and distinguishes scheduled work from
actually running work.

| Observation | Presentation |
|---|---|
| Room `QUEUED`, no attempt started | Queued badge |
| Room `PROCESSING` and current-batch WorkManager work is `RUNNING` | Composing badge and indeterminate indicator |
| Room `COMPOSED` | Composed badge and recorded output preview |
| Room `FAILED` | Failed badge, recorded error and existing per-item retry |
| Pending row queued again with `attempt > 0` | Outlined "Queued again" badge |
| Processing row with `attempt > 1` and running worker | Outlined "Retrying" badge and indicator |
| Pending items but no unfinished scheduled work | Paused label and existing Resume action |
| Scheduled but not running | Waiting-to-run header; pending rows do not claim to be composing |
| WorkManager `ENQUEUED` with `runAttemptCount > 0`, no running worker | Waiting-for-automatic-retry header |
| WorkManager observation not yet available | Checking-status header, not an optimistic live claim |

Rows show the persisted count of attempts started, including on failed and
completed results. The Room claim query increments that count; retry and
interrupted recovery retain it. "Queued again" therefore deliberately covers
both manual retry and interrupted recovery, rather than pretending the database
records why a row was requeued. Paused labels take precedence over retry labels,
while the outline and attempt count remain.

Room item state and the current batch's unique-work observation remain the only
execution truth. The ViewModel's local pause flag was removed. A stale
`PROCESSING` row is presented as queued while its worker is not running, without
rewriting the Room record. Queued/running summary counts use the same rule.
There is no timer, simulated progress, new scheduler or job-control API.

Per-item retry, retry-all and Resume retain `BatchRoomStore.retry` and
`BatchWorker.enqueue`; Pause retains `WorkManager.cancelUniqueWork`. Pause is
offered only after scheduled work is observed. The existing busy/error handling,
batch-ID guards, selection, reapply and notification flow remain.

Coil still loads the composed output path or the imported source path, with
explicit loading/error slots. Blank paths are now excluded before constructing a
file request. Both selection and queue rows retain stable keys and `animateItem`.
Retry buttons gain a photo-specific accessibility label. Seven new resource keys
are present in both English and Russian; existing Batch strings are reused.

Chosen approach: extend the existing Room/WorkManager/Coil/Foundation
presentation. Rejected: local "running" or "paused" truth and an independent retry
timer, because these can disagree with durable execution.

## Observed checks and limits

`python .kiln\scratch\copilot-plinth-w43-batch-polish\check_batch_resources.py`
first failed on missing queue/retry resources and attempt/target placeholders.
After implementation it reported **4 tests, OK**. It parses the actual resource
files for duplicate names, EN/RU coverage, format parity and Batch-screen string
references. These are resource-contract checks, **not Kotlin or UI tests**.

`node scripts\check-locales.mjs --slot plinth-product-photos` reported **PASS**:
1,029 English entries and one translated language, with matching format arguments
and plural categories.

`node scripts\verify.mjs --slot plinth-product-photos --json` reported
**121 PASS, 2 FAIL, 3 UNVERIFIED, 4 SKIP**, exit 1, **zero subprocesses**.
The failures were `slot.device` (the recorded build-input digest no longer matches
the sources) and `uiux.text-layout`
(`ui/library/LibraryScreen.kt:355`, single-line text without overflow handling).
Neither the device record nor Library was changed. The unverified rows were
`pack.inventory`, `pack.media` (no store assets), and `build` (not requested).
Full source output is retained in
`.kiln\scratch\copilot-plinth-w43-batch-polish\verify-source.json`.

Scoped `git diff --check` reported no whitespace errors. Existing staged and
unstaged work was preserved. The measured interval from the task timestamp
02:10:39+03:00 to the source-review timestamp 02:15:26+03:00 was **287 seconds**;
this is an elapsed reading, not a selection or acceptance criterion.

Compilation, Coil decoding, cancellation/backoff timing, insertion/removal
animation and accessibility at large text sizes remain **UNVERIFIED** under this
task's no-Gradle/no-device constraint. An authorized follow-up must compile and
exercise mixed success/failure, retry-one/retry-all, automatic backoff, pause,
reopen/resume, missing images, Russian and large text. Reject the behavior if a
waiting worker shows composing rows, paused work has no Resume, a retry loses its
error recovery, or terminal failures are described as successful composition.

## Claim ledger

Paths below are relative to this slot's `app/src/main/`, unless stated otherwise.

| Claim | Label | Evidence |
|---|---|---|
| Scheduling, running and automatic-retry indicators come from the current batch's WorkManager records. | MEASURED | `java/com/gexetr/plinthproductphotos/ui/batch/BatchViewModel.kt:61-79` |
| Pause is derived from observed work, and non-running pending rows are not displayed as composing. | MEASURED | `java/com/gexetr/plinthproductphotos/ui/batch/BatchScreens.kt:119-124,374-378,415-417` |
| Repeated attempts have distinct labels, an outline and the persisted attempt count. | MEASURED | `java/com/gexetr/plinthproductphotos/ui/batch/BatchScreens.kt:418-447,503-515`; claim increment at `java/com/gexetr/plinthproductphotos/data/batch/BatchDatabase.kt:70-74` |
| Retry/resume/pause keep the existing store/worker APIs; no execution mechanism was added. | MEASURED | `java/com/gexetr/plinthproductphotos/ui/batch/BatchViewModel.kt:122-187` |
| Existing path-backed Coil previews and keyed item animations are retained. | MEASURED | `java/com/gexetr/plinthproductphotos/ui/batch/BatchScreens.kt:270-313,471-492` |
| New text and retry accessibility descriptions have EN/RU counterparts. | MEASURED | `res/values/strings_batch.xml:26-32`; `res/values-ru/strings_batch.xml:26-32`; resource check: 4 tests OK; locale command: 1,029 entries PASS |
| Source-only verification is not fully green. | MEASURED | `node scripts\verify.mjs --slot plinth-product-photos --json`: 121 PASS, 2 FAIL, 3 UNVERIFIED, 4 SKIP; zero subprocesses |
| The Kotlin compiles and the live queue behaves correctly on-device. | UNVERIFIED | No Gradle or device run permitted; requires an authorized build and the journeys described above. |

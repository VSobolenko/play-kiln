# W25: Batch as a live queue

Implements backlog item 6 in `modern-ai-android-2026.md`, section 3.
Source changes only: **Comp UNVERIFIED**. No Gradle command, device run,
dependency change, signing, release operation, or commit was performed.

## Presentation

- Selection and queue entries use 80 dp Coil thumbnails. A composed entry uses
  its output image; other entries use the imported source. Loading and unavailable
  previews have explicit treatments, and missing metadata is not displayed as a
  raw internal source ID.
- A queue summary counts queued, composing, composed and failed entries; cancelled
  entries appear when present. Each state has a named, theme-colored badge and
  each queue row has a matching rail. Composing entries also have an indeterminate
  indicator; status is not conveyed by color alone.
- Overall progress counts terminal entries (composed, failed and cancelled),
  not invented processing percentages. Only an entirely composed batch says
  "Batch complete"; mixed terminal results ask the user to review them.
- Pause/resume and retry-all sit above the queue. Each failed row retains its
  error and a retry button. Paused pending entries are labelled paused, not
  animated as if still composing.
- One lazy vertical scroller contains the header, wrapping actions and photo
  rows. Selection is a full-row checkbox target. Notification opt-in and its
  permission explanation remain in place. English and Russian resources cover
  the new copy.

## State and recovery

Room remains the item-state authority; WorkManager still executes the existing
worker. The ViewModel additionally observes unique-work status, keyed by batch ID.
Pending records with no unfinished scheduled work expose Resume, including after
reopening a cancelled queue. Scheduled/backoff work is not treated as paused.

Item and worker records are matched to the current batch ID so reapplying a Look
does not present the old batch's rows as the new batch. Item observation switches
only when the batch ID changes, not on every UI-state update.

Queue actions disable competing actions while being dispatched. Pause waits for
WorkManager's cancellation operation before enabling Resume. Retry clears local
pause state after dispatching work, retry-all reads failed records from Room, and
an action exception clears the busy flag and remains visible. Reapply creates a
new batch only from the settled queue UI; it does not replace an active queue.

Chosen approach: existing Coil, Compose and WorkManager APIs, rather than another
image loader, queue engine or animation dependency. Labels/counts and actual
terminal-state progress were chosen instead of a simulated percentage or timer.
The pre-existing `PlinthScaffold` edits and the saved-Looks screen were preserved.

## Source-only checks

`python .kiln\scratch\copilot-plinth-w25-batch\check_batch_sources.py`
reported **8 tests, OK**. These are source-contract assertions, not Kotlin or UI
tests: resource coverage, EN/RU format parity, enum label/color coverage, a single
batch scroller, thumbnail paths, terminal-progress semantics, current-batch work
observation, and recovery-action/error-unlocking wiring.

Scoped `git diff --check` reported no whitespace errors. These observations do
not establish compilation, image decoding, WorkManager timing or accessibility
on a device.

## Required independent verification

**UNVERIFIED:** an authorized orchestrator must run
`node scripts\verify.mjs --slot plinth-product-photos --build` for compilation.
No such command was run by this worker.

Device review must exercise a mixed queue, pause/resume, reopening a paused batch
with the same ID, per-item and bulk retry, reapply, missing thumbnails, long names,
Russian copy, large text and a short/landscape window. Reject the change if the
wrong batch's photos appear, unfinished unscheduled work has no Resume action,
failed rows have no retry, a paused row keeps a composing spinner, or controls
become unreachable. A completed failure/cancellation must never be described as
successful composition.

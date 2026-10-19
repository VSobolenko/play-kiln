# W30 - Batchable SKU workspace

Implements the presentation scope of `modern-ai-android-2026.md` section 3,
item 11. This is a source-level handoff, not a build or device acceptance.

## Design and boundaries

The workspace leads with group, photo and unassigned counts, followed by batch
naming tools and SKU sections. Each section displays its persisted SKU and
included-photo count. Each photo has a local-image thumbnail, SKU, filename,
role and saved category. Reference images count as included photos, not as
exportable outputs.

Chosen: scan-first rows with expandable photo actions and inline SKU editing.
Rejected: an always-open category field, reference checkbox and action stack on
every photo. The previous screen put those controls ahead of product imagery;
the request prioritizes recognizing products and their SKU membership.

- Naming rules remain editable, with the current base visible when collapsed.
  The regroup action explicitly warns that manual placement may change.
- Headers use each group's saved pattern, not the unsaved batch-pattern field.
  Renumbering still uses the current batch pattern, stated beside its action.
- Empty groups remain visible, including when the project has no photos.
  Unassigned photos have their own section and instructions.
- Photo details retain category save, reference toggle, move, SKU confirmation
  and scale confirmation. A collapsed row still flags pending confirmation.
  Suggestions are never applied automatically.
- Move menus are anchored to their buttons, exclude the current group, and
  explain when no destination exists.
- One lazy list contains both setup and individual photos, instead of nesting
  every photo of a group inside one lazy item. Setup can scroll out of the way.
- Full-width primary actions avoid side-by-side button-label squeezing.
  Text wraps; headings expose heading semantics; the reference row is one
  labelled checkbox target with a minimum height of 48 dp.
- Existing Coil renders local files with `ContentScale.Fit`, a loading indicator
  and a localized failure state. Both absolute stored paths and paths relative
  to app files storage are handled.

Only `ui/group/GroupScreen.kt`, the new EN/RU
`strings_group_workspace.xml` resources and this document are product changes.
`GroupViewModel.kt`, repositories and dependencies are unchanged by W30.
This task does not add bulk selection or new persistence behavior.

## Observed checks and remaining verification

**MEASURED:** `python .kiln\scratch\copilot-plinth-w30-group\check-resources.py`
parsed the resource XML, resolved all **42** string references used by
`GroupScreen` without duplicates in both `values` and `values-ru`, and found
matching positional format arguments. This is a resource/source check, not a
behavioral regression test.

**MEASURED:** scoped `git diff --check` reported no whitespace errors.
No Gradle commands, dependency installations or commits were performed.

**Comp: UNVERIFIED.** No Kotlin compilation, Compose instrumentation, device
session, screenshot or visual acceptance was performed. The integrating owner
must run the normal slot build/test commands outside this worker's no-Gradle
boundary, then check:

1. Empty project; empty group creation; unassigned photos; several populated
   groups with main, extra, slide and reference images. Counts must match the
   actual membership, with every photo visible exactly once.
2. Mixed absolute/relative local image paths and a missing image. Confirm fitted
   product previews, a readable failure state and bounded loading indicators.
3. Change the batch pattern without applying it: existing group headers and move
   targets must retain saved SKU names. Apply renumbering and regrouping; verify
   persisted names and membership after leaving and reopening the screen.
4. Move a photo, save/clear its category, toggle reference and confirm each kind
   of suggestion. Verify persistence; invalid patterns must surface the existing
   error rather than appear successful.
5. EN/RU, narrow portrait, large font, keyboard open, rotation and TalkBack.
   All fields/actions must remain reachable by scrolling, checkbox text must
   toggle its value, and long SKUs/filenames must remain readable.

## Claim ledger

Paths below are slot-relative.

| Claim | Label | Evidence |
|---|---|---|
| SKU sections, photo counts and individually lazy photo rows are wired in source | MEASURED | `ui/group/GroupScreen.kt:57-145` under the app package |
| Saved group pattern is used in the section heading | MEASURED | `ui/group/GroupScreen.kt:207-219` |
| Existing photo-operation callbacks remain wired | MEASURED | `ui/group/GroupScreen.kt:250-259` |
| Product previews request local files through existing Coil | MEASURED | `ui/group/GroupScreen.kt:391-417` |
| EN/RU references resolve and format arguments agree | MEASURED | Scratch resource-check command and output above |
| Kotlin/Compose compilation and on-device behavior | UNVERIFIED | Integrating owner: normal slot build/tests, followed by the device scenarios above |

Elapsed worker time: approximately 4 minutes, including source inspection and
resource checks; not a performance or product-quality measurement.

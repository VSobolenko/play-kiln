# W22 - Image-first Library

## Bounded implementation

The Library body is now a Foundation adaptive grid of square Coil 3 project
previews, followed by growing project metadata. It uses the existing theme's
surface, primary and secondary roles, rounded image stages and the existing
Lucide resources. It adds no dependency, destination or ViewModel state.

Projects are explicitly labelled **Project**. The introductory copy distinguishes
source photos/edits from exported outputs. `ProjectSummary` exposes only the
project preview, item count and primary SKU; the count remains **items**, not
outputs. This change deliberately does not fabricate an output count, generation
status or output gallery. Opening a card still invokes `onOpenProject(id)`.

The host already provides `PlinthScaffold` and `PlinthTopBar` for Library
(`ui/PlayKilnApp.kt:260-267`, Library callback at `462-467`). Their APIs were read;
neither is nested or edited here. A duplicate shell was rejected because it
would duplicate chrome/insets and require a wallet callback this screen does not
receive. The host continues to own narrow-screen/detail-pane navigation.

## Interaction and states

- Search is immediately available. Category, marketplace and date fields move
  into a scrollable Filters dialog; changes still use the existing live VM
  setters. Active filters, reset and invalid dates remain visible.
- Select projects reveals checkboxes and batch tools. A tile opens its project
  normally and toggles selection in selection mode. Done clears selection.
  "Toggle loaded projects" uses the Paging snapshot, not every stored project;
  it deliberately does not claim to select unloaded records.
- Marketplace change, re-export and deletion remain available. A single project
  is deleted by selecting it and choosing Delete selected. Deletion now requires
  confirmation. Per-item batch results are separate lazy full-width rows with a
  correctly named dismissal action.
- Initial loading, refresh errors, empty Library, no matches, refresh-in-place,
  append loading and append errors have visible states. Paging errors expose
  Retry. Missing/failed images show Preview unavailable without removing the
  project's name or open action; image loading is labelled separately.
- Header, controls, status panels and cards share one scroll container. Batch
  tools cannot consume a fixed viewport above an unscrollable project list.
  Grid columns adapt to available width, with a larger minimum card width at
  larger font scales. Text is not line-clamped, and controls have a minimum
  48 dp target. Filters and selection are saveable UI state.

## Evidence and limits

`python .kiln\scratch\w22-library-check.py` is a **source-contract check only**.
Before implementation it exited 1 with six expected failures (adaptive grid,
stable paging keys, image state slots, retry, project/output guidance and delete
confirmation). After implementation it exited 0: six source contracts present,
18 new referenced keys with matching English/Russian keys and format arguments,
valid resource XML and all referenced strings/drawables present.

`git diff --check` on the changed Library/resources reported no whitespace
findings. These observations do **not** prove compilation, runtime behavior,
accessibility, visual quality or navigation on a device. No Gradle, build,
dependency installation, signing, release action or commit was run.

Required integration acceptance, **UNVERIFIED** in this worker:

1. Compile through the integrator's authorised build process.
2. Inspect Library in English/Russian, light/dark, a narrow phone and a wider
   pane, with enlarged fonts and keyboard open. Confirm wrapping and the last
   card remain usable above the host navigation.
3. Browse valid, missing and corrupt previews. Open a project and return through
   existing navigation. Confirm no extra top bar, balance chip or destination.
4. Exercise empty/no-match/loading/error states, retry refresh/append and check
   that existing cards remain available after a paging error.
5. Select one/multiple/loaded projects, clear selection, change marketplace,
   re-export, cancel/confirm deletion and inspect mixed batch results.

## Grounded-claims ledger

Paths below are relative to the slot unless a command starts at repository root.

| Claim | Label | Evidence |
|---|---|---|
| Adaptive image-led project cards with stable Paging keys are authored | MEASURED | `ui/library/LibraryScreen.kt` under `app/src/main/java/com/gexetr/plinthproductphotos/`, `GridCells.Adaptive`, `paging.itemKey` and `ProjectTile`; source-contract command above |
| The summary cannot substantiate an output count | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/data/db/Daos.kt:16-20`, `ProjectSummary` fields |
| Existing host shell and project callback are retained | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt:260-267,462-467`; no edits to host/components |
| English/Russian additions have key/format parity | MEASURED | `app/src/main/res/values/library_browser.xml`, `values-ru/library_browser.xml`; source-contract command above, 18 referenced new keys |
| Compilation, on-device layout and behavior are not demonstrated | UNVERIFIED | No Gradle/device execution; integrator must perform the acceptance steps above |
| A grid prioritises saved imagery better than the previous always-visible filter form | INFERRED | Previous body placed five text fields above 56 dp previews; the authored body uses square image stages and a separate filter dialog. Owner/device judgement is still required. |

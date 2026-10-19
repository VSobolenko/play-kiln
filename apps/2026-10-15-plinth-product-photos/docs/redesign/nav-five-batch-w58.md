# W58 — five-slot navigation with Batch

## Changed behavior

- The compact bar is one tonal `Surface` containing five equal-width items in
  this order: Studio, Library, Create, Batch, More. Create stays an inline,
  48 dp circular action with its own `Create` label; it is not raised above the
  bar on a floating-action-button layer (`app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthNavBar.kt:131-183,310-342`).
- The navigation rail uses the same order. Create is now a normal rail entry
  between Library and Batch rather than a separate header action (`PlinthNavBar.kt:185-245`).
- Selecting Batch opens a Batch project-picker hub. Selecting a project leads
  to the existing Looks selection, which continues into the existing batch
  queue when a Look is applied (`app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt:274-295,598-629,678-689,1252-1266`).
- The section title and visible labels have English and Russian resources.
  The top-bar title reads the section resource (`app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthTopBar.kt:21-46`); the new strings are declared at `app/src/main/res/values/strings_shell.xml:5-7` and `app/src/main/res/values-ru/strings_shell.xml:5-7`.

For the Batch glyph, this change reuses the slot's existing
`ic_lucide_list_filter` vector (`app/src/main/res/drawable/ic_lucide_list_filter.xml`),
already used by the Library filter control (`app/src/main/java/com/gexetr/plinthproductphotos/ui/library/LibraryScreen.kt:119`).
It represents the hub's multi-project list without adding another drawable.
The suggested layers/copy/grid alternatives were rejected because the resource
inventory command
`Get-ChildItem apps/2026-10-15-plinth-product-photos/app/src/main/res/drawable -File | Where-Object { $_.Name -match 'layer|copy|grid|stack|sheet|files|image' } | Select-Object -ExpandProperty Name`
returned only `ic_lucide_image.xml`. Reusing Studio's image glyph was also
rejected to keep the two top-level destinations distinguishable
(`PlinthNavBar.kt:352-355`).

## Checks observed

The regression test was added before the navigation implementation. Its initial
instrumented run failed both bottom bar and rail cases with the observed
assertion `java.lang.AssertionError: Missing string resource shell_batch`.
The report from that RED run was subsequently replaced by the final connected
test report.

The focused device run used this command through the shared
`scripts/lib/gradle.mjs` route:

```text
node -e "import('./scripts/lib/gradle.mjs').then(({runGradleTasks})=>{const slotDir=process.cwd()+'/apps/2026-10-15-plinth-product-photos';const x=runGradleTasks(slotDir,['connectedDebugAndroidTest','-Pandroid.testInstrumentationRunnerArguments.class=com.gexetr.plinthproductphotos.ui.PlinthBatchNavigationTest']);const out=String(x.result.stdout||'')+String(x.result.stderr||'');console.log(out.split(/\r?\n/).slice(-80).join('\n'));process.exitCode=x.result.status??1;})"
```

Its output included:

```text
> Task :app:connectedDebugAndroidTest
Running tests on devices: SM-A405FM - 11
BUILD SUCCESSFUL in 54s
```

The resulting class report lists three tests, zero failures, and zero skipped:
the equal five-slot bottom bar and selection callback, the ordered rail entries
and callback, and selecting Batch in `PlayKilnApp` to open the project-picker
hub (`app/build/reports/androidTests/connected/debug/com.gexetr.plinthproductphotos.ui.PlinthBatchNavigationTest.html`).
The fresh report XML at
`app/build/outputs/androidTest-results/connected/debug/TEST-SM-A405FM - 11.xml`
records `tests=3 failures=0 errors=0 skipped=0` after the repeat run. The
connected task compiles the debug app.

The full JVM unit-test command was:

```text
node scripts/run-slot-tests.mjs --slot plinth-product-photos --rerun
```

Its output was:

```text
PASS testDebugUnitTest — 280 tests, 0 failures, 0 errors, 0 skipped (17427ms).
```

The shared Gradle route also printed Node's `DEP0190` warning about passing
arguments to a child process with `shell: true`; the test task exited
successfully despite that warning.

The independent slot verification command was:

```text
node scripts/verify.mjs --slot plinth-product-photos --build
```

It compiled `assembleDebug` successfully under JDK 17. The overall verification
reported **123 PASS, 2 UNVERIFIED, 4 SKIP, 1 FAIL**. The failure is
`slot.device`: the recorded device build digest predates the current app
sources (`e8c0599317ba…` recorded versus `388cc2250104…` measured). The two
unverified rows are the absent store asset inventory/media; no release approval
or device record was written as part of W58.

## Explicit limits

- The app-level navigation test proves the Batch tap reaches the hub; it does
  not create/select a project, apply a Look, or exercise the queue's photo
  processing on-device (`app/src/androidTest/java/com/gexetr/plinthproductphotos/ui/PlinthBatchNavigationTest.kt:95-106`).
- The rail and bottom-bar geometry test ran in the connected instrumented suite
  on the listed Android 11 device. Russian text fitting, large-font behavior,
  landscape/foldable layouts, and the full batch-processing journey remain
  unverified (`PlinthBatchNavigationTest.kt:30-125`).
- No release approval, release artifact, screenshot, or wallet redesign work is
  part of this change.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The regression test failed before implementation because `shell_batch` was missing. | MEASURED | Initial connected run output: `java.lang.AssertionError: Missing string resource shell_batch`; the observed failure was in the bottom-bar and rail cases. |
| The bottom bar lays out five equally spaced labels on a shared baseline and the Create tap remains actionable. | MEASURED | `app/src/androidTest/java/com/gexetr/plinthproductphotos/ui/PlinthBatchNavigationTest.kt:30`; final connected report records `bottomBarShowsFiveEqualSlotsAndRoutesBatchSelection` as passed. |
| The rail includes Create and Batch as ordered entries and dispatches Batch selection. | MEASURED | `app/src/androidTest/java/com/gexetr/plinthproductphotos/ui/PlinthBatchNavigationTest.kt:61`; final connected report records `railShowsBatchAndCreateAsAlignedSectionEntries` as passed. |
| Selecting Batch from the app opens the project-picker hub. | MEASURED | `app/src/androidTest/java/com/gexetr/plinthproductphotos/ui/PlinthBatchNavigationTest.kt:95`; final connected report records `selectingBatchOpensTheBatchProjectPickerHub` as passed. |
| Selecting a project continues into the pre-existing Looks → Batch path. | INFERRED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt:626` routes the hub's project callback to `LooksDestination`; `PlayKilnApp.kt:680` routes Look application to `BatchDestination`. |
| The section and Create labels are declared in both locales. | MEASURED | `app/src/main/res/values/strings_shell.xml:5`; `app/src/main/res/values-ru/strings_shell.xml:5`; Android resource compilation ran with the connected test. |
| The selected Batch icon is an existing slot Lucide asset, not a new or copied drawable. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthNavBar.kt:355`; the vector is `app/src/main/res/drawable/ic_lucide_list_filter.xml`, also referenced by `LibraryScreen.kt:119`. |
| The complete photo-processing flow and Russian/large-font visual fit are correct on every supported window size. | UNVERIFIED | Requires a device pass that selects a project and Look, exercises the queue, and checks Russian plus large-font compact and expanded layouts. |

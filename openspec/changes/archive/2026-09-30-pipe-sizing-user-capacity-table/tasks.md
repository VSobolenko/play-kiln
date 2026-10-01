# Tasks: pipe-sizing-user-capacity-table

## Recorded decisions

- [x] The existing slot is `apps/2026-09-25-pipe-sizing` (`com.gexetr.pipesizing`); no other slot is created or altered. **Reason:** one idea, one slot.
- [x] The route is **user-entered lookup**: the app stores a table the user enters from their own code book and returns values from it. **Reason:** `docs/research/2026-09-25-pipe-sizing-code-source.md` §5 prices this route *"Lower because the app does not redistribute the table"*, and its decision table forbids embedding — *"No embedded numbers until a right is documented"* — until a written NFPA/ICC reproduction licence, a frozen edition with TIAs, and a reviewer-signed professional transcription exist.
- [x] Interpolation is forbidden; rounding is upward only; every substitution is shown. **Reason:** rounding a length down returns a capacity for a shorter run than the user is building. The source procedure documents *"the closest larger distance if the exact distance is not listed"*.
- [x] v1 exposes **no** export, share, import or file-picker surface for the table. **Reason:** the moment the app can move table data off the device it becomes a redistributor of code data, which is precisely the right the memo says is undocumented.
- [x] `store/studio.json`, `store/journey.json`, `store/listing.json` and `differentiators.json` are **not** edited by this change. **Reason:** their claims must move against implemented code with real proofs, and `differentiators.json` needs the owner's own words for a core promise.
- [x] The table surface is assumed **free** (no new purchase or ad surface). **Reason and consequence:** this is Open Question 1 in `design.md`; if the owner gates it behind Pro, the paywall copy, the store benefit bullets and `economics.json` move with it, and that is a UI-group change plus a release-copy change, not a redesign. Settle it before `/release`.

## 0. No code capacity data may be embedded — a task gate, not a preference

Every task below inherits this, and task 5.3 is the sensor for it:

- **No capacity, length, nominal size, inside diameter, gas gravity, pressure-drop, applicability or size-ordering value may come from the app.** Every value in the new paths is typed by the user and stored as the user's own recording.
- **No trade-size ladder, dimension schedule, fitting table, or equation may be added.** The size ordering is derived from the user's own recorded row order (with the recorded diameters cross-checked), never from a ladder the app holds.
- **No such data may be added to `res/`, `assets/`, source, or any network path**, and no test fixture may contain a value presented as a code capacity. Test fixtures must be obviously synthetic readings (the existing suites' pattern) and must say so.
- **No new Gradle dependency** is needed for any task. If one appears needed, that is a stop-and-report, not an addition: dependency additions need a reason in the slot's design notes.

---

## 1. Contract step — sequential, one writer, blocks both parallel streams

One writer only. This step fixes the types and the delegation boundary so the two parallel streams cannot touch the same file (`design.md` §Pinned interfaces).

- [x] 1.1 Add `app/src/main/java/com/gexetr/pipesizing/sizing/CapacityTable.kt`: `USER_RECORDED_MARKER`, `TableApplicability`, `TableCitation`, `TableRow`, `UserCapacityTable`, plus table-completeness validation (rows present; every row has a size, a length and a capacity; citation and marker present; no duplicate `(size, length)`) and the derived size order with its diameter cross-check. **Verify:** new `app/src/test/java/com/gexetr/pipesizing/sizing/CapacityTableTest.kt` asserts an incomplete table, a duplicate `(size, length)` and a diameter-vs-order contradiction each produce their named refusal, and that a complete table passes.
- [x] 1.2 Edit `app/src/main/java/com/gexetr/pipesizing/sizing/SizingDomain.kt` only as far as the contract: add `SizingMethod.RETURN_FROM_USER_TABLE` and `SizingMethod.SMALLEST_SIZE_FOR_LOAD` (both `requiredInputs = emptySet()`), `UserProvenance.RecordedTableCitation`, `SizingEvaluation.ReturnedFromUserTable`, `LengthSubstitution`, the nine new `RefusalKind` members, the two table-query fields on `SizingCase` (`tableSelection`: table id + optional nominal size, defaulted so every existing call site compiles), and rename the existing body to `evaluateArithmetically` behind `fun evaluate(case: SizingCase, table: UserCapacityTable? = null)` dispatching on `case.method.returnsFromUserTable`. **Never** add an `else -> error(...)` to any `when` over a sealed type — the compiler's exhaustiveness check is the mechanism that forces the UI to handle the new result. **Verify:** `node scripts/run-slot-tests.mjs --slot pipe-sizing` — every existing test still passes unchanged and the module compiles with `CapacityTableLookup.kt` absent (the dispatch compiles against the declared signature).
- [x] 1.3 Add `app/src/main/java/com/gexetr/pipesizing/ui/storage/LineCodec.kt`: the four separator constants plus `escapeField`/`unescapeField`, moved verbatim from `SavedCaseCodec` so there is **one** copy of the escaping rule. Do not edit `ui/cases/SavedCase.kt` in this step. **Verify:** the file is the only place in the slot declaring the escape table (`rg` for the escape characters finds one definition), and no existing test changes behaviour.
- [x] 1.4 Add a JVM test that asserts the pinned delegation contract without the algorithm: `evaluate(case, table = null)` with a return-from-user-table method returns a `Refused` naming the table, and with the arithmetic methods returns exactly what it returned before. **Verify:** the test fails if the dispatch line is removed.

### 1.5 — Recording what step 1 actually cost (added 2026-09-26, after the first attempt)

**Verified 2026-09-26 by the orchestrator, not by the worker that wrote it.** Both step-1 workers
reported that they never observed a test result: Gradle died before compiling with a wrapper-dist
lock failure (`FileNotFoundException`/access-denied on `...gradle-9.7.1-bin.zip.lck`). The
orchestrator ran the same command and observed:

```
PASS testDebugUnitTest — 69 tests, 0 failures, 0 errors, 0 skipped (37975ms)
exit_code: 0, elapsed_ms: 42111
```

and confirmed by reading `ui/home/HomeScreen.kt` that the `when` carries explicit branches for
`Refused`, `Computed` and `ReturnedFromUserTable` with **no** `else`, and that no file under
`app/src/main` or `app/src/test` carries a code-table value (the new fixtures are labelled
synthetic). **What this does not prove:** the 69 tests pass with the lookup engine still a stub, so
they say the contract compiles and the table validation behaves — they say nothing about returning a
value. Task 2.1 is what changes that, and 2.2 is the sensor for it.

Step 1 was attempted and its executor honestly reported that it never saw a test result: Gradle died
on a wrapper lock before compiling. Re-running the same command exposed a defect the plan did not
anticipate, and the plan's own ordering is what caused it:

```
e: .../ui/home/HomeScreen.kt:257:5 'when' expression must be exhaustive.
   Add the 'is ReturnedFromUserTable' branch or an 'else' branch.
```

1.2 adds a subtype to the `SizingEvaluation` sealed interface, and the compiler immediately demands
that every `when` over it handle the new case — `ui/home/HomeScreen.kt` is such a `when`. The plan
schedules that UI handling in 4.4, so **1.2's own verify clause ("the module compiles") is
unsatisfiable within step 1 as written.** The exhaustiveness check is working correctly and must not
be defeated with an `else` branch; the fix is to bring a minimal branch forward:

- [x] 1.5 Add the minimal `is SizingEvaluation.ReturnedFromUserTable ->` branch to
  `ui/home/HomeScreen.kt`, plus its `values/strings.xml` entries, so the tree compiles at the end of
  step 1. **Never** add `else ->` to that `when`. **Verify:** `node scripts/run-slot-tests.mjs --slot
  pipe-sizing` exits 0 and the existing HomeScreen tests are unchanged. This branch is a
  compile-level placeholder, not the integration: 4.4 must **replace** it with the real rendering
  (returned capacity in the selected unit, the row used, the substitution pair, the citation, and
  the user-recorded/no-verification labels) rather than adding a second branch beside it.

Also recorded from that attempt, so no later writer has to rediscover them:

- `SizingDomain.kt` currently carries a temporary `evaluateCaseAgainstUserTable` that handles only the
  no-table refusal and otherwise returns a blanket `TABLE_INCOMPLETE`. It is labelled as temporary in
  the file, and it will make the feature look wired while being unimplemented. **2.1 must replace it
  outright, not extend it.** If 2.1 is skipped or lost, the suite must not be allowed to pass on the
  stub — 2.2's assertions on a returned value are what detect that.
- 1.3's "the file is the only place declaring the escape table" cannot hold in step 1, because
  `ui/cases/SavedCase.kt` is only routed through `LineCodec.kt` in 5.3. Two escape definitions exist
  between step 1 and 5.3 by design; the check belongs to 5.3, not 1.3.

## 2. Parallel stream A — the lookup engine (worker A)

Worker A owns these files and no others. Depends only on step 1.

- [x] 2.1 Add `app/src/main/java/com/gexetr/pipesizing/sizing/CapacityTableLookup.kt` implementing `evaluateCaseAgainstUserTable(case, table)` for both methods exactly as `design.md` D3/D4 specify: applicability match first, then completeness, then the closest-**larger**-length selection with `minByOrNull` over strictly longer rows, then the smallest carrying size in the user's own recorded order, returning `ReturnedFromUserTable` with the row's `capacityMbh` moved unadjusted and the `LengthSubstitution` populated whenever a substitution happened. No arithmetic may touch the returned capacity. **Verify:** `node scripts/run-slot-tests.mjs --slot pipe-sizing`.
- [x] 2.2 Add `app/src/test/java/com/gexetr/pipesizing/sizing/CapacityTableLookupTest.kt` with at least: an exact row returns its recorded capacity and names the row; a between-lengths query uses the larger length and reports both values; the value recorded at the shorter length is never returned; a load between two capacities returns the larger recorded capacity and never an intermediate value; the returned capacity equals the recorded row's `Double` with **zero** tolerance; the smallest carrying size is returned when two sizes both carry; each of the nine refusals fires with the field or limit it names. Use obviously synthetic readings and say so in a comment. **Verify:** the suite fails if `minByOrNull` is replaced by `maxByOrNull`, if the comparison becomes `>=` on the wrong side, or if the exact-equality assertion is loosened — check all three before trusting the suite.

### 2.3 — Verification of step 2, and the defects it found (2026-09-26, orchestrator)

The worker again observed no test result (Gradle wrapper lock, see §1.5). The orchestrator verified
from the artifact instead, and the evidence is:

- **Green baseline:** `PASS testDebugUnitTest — 73 tests, 0 failures, 0 errors, 0 skipped`, exit 0.
- **The stub is gone:** `rg "Temporary contract boundary"` finds nothing under `src/main`.
- **The sensor fires.** Three implementation mutations were applied one at a time, the suite run, and
  the file restored byte-exact after each (hash-verified). All three were **detected**:
  the length selection replaced by nearest-or-smaller; the carrying comparison flipped to the wrong
  side; the returned capacity perturbed (`* 1.0001`). Each produced
  `Execution failed for task ':app:testDebugUnitTest'`, i.e. a test assertion, not a compile error.
  **Note for whoever repeats this:** a first probe reported all three as "DID NOT COMPILE" because it
  treated any `BUILD FAILED` as a compile error. Distinguish `e: ` / `Compilation error` from
  `Execution failed for task ':app:testDebugUnitTest'` before drawing a conclusion.

**Four defects remain open in what step 2 produced.** None is fixed by this change's later steps as
planned, so each needs a fixer or a spec amendment before 5.6:

- [x] 2.3.1 `when (case.method)` in `CapacityTableLookup.kt` ends with
  `else -> error("Table lookup was dispatched for a non-table method.")`. `SizingMethod` is an enum,
  so this compiles and behaves correctly today, but `else` removes the exhaustiveness check: a future
  method added to the enum falls into a runtime crash instead of a compile error. List all four
  members explicitly, the two arithmetic ones included.
  **Fixed 2026-09-27 — see §2.4.1.**
- [x] 2.3.2 **D7 cannot be honoured as written.** `applicabilityRefusal` compares material/system,
  inlet pressure class and gas specific gravity only, because `SizingCase` carries no `fuel` and no
  `permittedPressureDrop` field — the two other things D7 requires an exact comparison for. So the
  spec's fuel-mismatch scenario cannot fire today, and a propane case can be run against a
  natural-gas table with the mismatch undetected. Either task 4.1 adds the two fields to the case, or
  D7 and the spec are amended to state which comparisons are not performed. **A scenario that cannot
  fail is not a check** — decide, then make the two agree.
  **Fixed 2026-09-27 — see §2.4.2.**
- [x] 2.3.3 `requiredLoadMbh()` and `totalLengthFeet()` in `CapacityTableLookup.kt` re-derive the load
  and length sums that `SizingEvaluator.evaluateArithmetically` already computes. Two definitions of
  one rule is what `verification-discipline.mdc` forbids; they can drift. Extract the sums to one
  place both callers use.
  **Fixed 2026-09-27 — see §2.4.3.**
- [x] 2.3.4 In `smallestSizeCarryingLoad`, a size with no row at or above the required length is
  dropped from `candidates` silently, so a refusal can name a length limit belonging to a different
  size. Refuse by naming the size whose recorded lengths cannot cover the case, per the "refused by
  the limit it hit" requirement.
  **Fixed 2026-09-27 — see §2.4.4.**

### 2.4 — The four defects in §2.3, fixed (2026-09-27, Cursor `generator`)

Fixed by a Cursor `generator` worker after Codex was killed by its usage limit; established from the
artifact and from observed test output, never from the worker's own report.

**Environment.** Gradle ran; the wrapper-lock failure that killed §1.5's and §2.3's workers did not
recur. One invocation per run, `node scripts/run-slot-tests.mjs --slot pipe-sizing`, no `JAVA_HOME`
or `GRADLE_USER_HOME` override.

- **Before:** `PASS testDebugUnitTest — 93 tests, 0 failures, 0 errors, 0 skipped` (observed by this
  worker, matching §3.5's baseline).
- **After:** `PASS testDebugUnitTest — 97 tests, 0 failures, 0 errors, 0 skipped`. Four tests were
  added (one per defect except 2.3.2, which is a documents-only amendment, plus one D4-step-4 guard);
  no pre-existing test's assertion changed. The one edit to an existing test helper is
  behaviour-preserving and is stated in §2.4.3.

**2.4.1 — `else` in the enum `when` (2.3.1).** `CapacityTableLookup.kt` now lists all four
`SizingMethod` members explicitly, the two arithmetic ones sharing a single branch that keeps the
original `error(...)` body. No `else` remains. This is a compile-time property, so no runtime test
can detect a regression and none is claimed to:
`theTableDispatchDecidesEverySizingMethodExplicitly` walks `SizingMethod.entries` and pins each
member's routing, while the exhaustiveness itself is proven by mutation, both directions:

- fix in place, fifth member added →
  `e: .../sizing/CapacityTableLookup.kt:12:12 'when' expression must be exhaustive. Add the 'SYNTHETIC_MUTATION_PROBE' branch or an 'else' branch.`
  → `BUILD FAILED in 7s`, the compiler check is back;
- pre-fix `else` restored, same fifth member still present →
  `PASS testDebugUnitTest — 97 tests, 0 failures` → the old form silently accepted an enum member
  with no branch, and the new test does not catch it either. That is the defect, stated as measured.

Both mutations reverted; suite back to 97/0.

**2.4.2 — D7 and the spec now agree (2.3.2).** Fixed by amendment, not by widening the case;
`SizingCase` gained no field. `design.md` **D7** now states the three comparisons actually performed
— material or system, inlet-pressure class, and gas specific gravity, all trimmed and case-folded
with gravity parsed to `Double` and compared by exact equality — and states plainly that **`fuel`
and `permittedPressureDrop` are not compared because `SizingCase` carries neither**, together with
the residual consequence: a case declared for propane can be run against a table declared for
natural gas and the mismatch is not detected. Adding the two fields is recorded as considered and
rejected.

- **What was removed, and why nothing was deleted.** The brief anticipated a fuel or
  permitted-pressure-drop mismatch scenario in the spec. **There is none to delete:**
  `rg "fuel|permitted pressure drop"` over `specs/fuel-gas-pipe-sizing/spec.md` finds only line 137
  — the table's *required declaration* fields, which are still required and still true — and the
  applicability requirement's own prose. The removal is therefore a rewrite, not a deletion: the
  "A table is refused outside its declared applicability" requirement asserted those two
  comparisons in its first paragraph, and it now names the three that fire and adds a paragraph
  recording the two that do not, including that no scenario in the spec asserts a refusal for
  either field. The four scenarios under it (pressure class; gravity near miss; punctuation;
  case/whitespace) are unchanged and all still fire.
- **Coverage consequence, recorded so it is not mistaken for a loss:** the applicability check is
  now honestly said to be three comparisons. A difference the case cannot express is no longer
  presented as a check.

**2.4.3 — one definition of the load and length sums (2.3.3).** The sums moved into
`SizingDomain.kt` as `requiredLoadMbh()`, `developedLengthFeet()`, `fittingAllowanceFeet()` and
`totalLengthFeet()`, and **both** `evaluateArithmetically` and the lookup call them; the two private
copies in `CapacityTableLookup.kt` are deleted. No existing signature or value changed: the helpers
evaluate the same expressions in the same order, so every pre-existing test passes unchanged.

- Sensor: `theTableLookupSharesTheArithmeticLoadAndLengthSums` drives one set of readings through
  both the arithmetic evaluator and the table lookup and asserts both report the same two sums
  exactly (`0.0` delta), plus the literal synthetic sums (19.75 MBH; 30.0 ft).
- Mutation: re-creating the second definition — replacing the lookup's `case.totalLengthFeet()` with
  a local `case.developedLengths.sumOf { it.feet }` in both call sites — produced
  `CapacityTableLookupTest > theTableLookupSharesTheArithmeticLoadAndLengthSums FAILED`,
  `97 tests completed, 1 failed`, an assertion failure and not a compile error. Reverted; suite
  green again.
- **Disclosed edit to an existing test helper:** `assertRefusal` now delegates to the new
  `assertRefusalKind` so the refusal-kind assertion exists once. It performs the same two checks in
  the same order as before; no test's expectation moved.

**2.4.4 — the refusal names the size whose limit was hit (2.3.4).**
`smallestSizeCarryingLoad` now collects each size whose own recorded lengths stop short of the
case's total length together with that size's own longest recorded length, and the
`LENGTH_ABOVE_EVERY_ENTERED_ROW` refusal names every such size and its own limit instead of the
table's globally longest row. A size that *can* cover the case is still skipped rather than
refusing, per D4 step 4.

- Sensor: `reverseLengthRefusalNamesTheSizesWhoseRecordedLengthsFallShort` asserts the refusal
  contains both sizes' names and both sizes' longest recorded lengths;
  `aSizeWhoseRecordedLengthsFallShortIsSkippedWhenAnotherSizeCoversTheCase` pins that a covering size
  is still returned rather than refused.
- Mutation: restoring the pre-fix message (`table.rows.maxByOrNull { it.lengthFeet }` and "for the
  selected sizes") produced
  `CapacityTableLookupTest > reverseLengthRefusalNamesTheSizesWhoseRecordedLengthsFallShort FAILED`,
  `97 tests completed, 1 failed`. Reverted; suite green again.

**Still open, by design:** the ninth refusal, `TABLE_CHANGED`, still has no test because it needs the
table-content digest task 4.5 introduces; its scenario belongs to 4.5. This is not a defect in
§2.3's sense and is recorded here only so the gap is not mistaken for one.

## 3. Parallel stream B — the table store and the table-entry surface (worker B)



Worker B owns these files and no others. Depends only on step 1, and shares no file with stream A.

- [x] 3.1 Add `app/src/main/java/com/gexetr/pipesizing/ui/tables/TablePersistence.kt`: `TablesRepository` (`interface` + `InMemory` for tests and previews + `DataStoreTablesRepository` over one `preferencesDataStore(name = "capacity_tables")` and one `stringPreferencesKey`), and a `TableCodec` that imports `ui/storage/LineCodec.kt` rather than restating the escaping. **Verify:** new `app/src/test/java/com/gexetr/pipesizing/ui/tables/TableCodecTest.kt` round-trips a table whose name and citation contain every separator, asserts a truncated record is dropped without losing the readable ones, and asserts blank storage decodes to nothing — the same three properties `SavedCaseTest` proves for the case codec.
- [x] 3.2 Add `app/src/main/java/com/gexetr/pipesizing/ui/tables/TablesUiState.kt` and `TablesViewModel.kt` mirroring `CasesViewModel`'s shape (a `stateIn` of the repository flow, plus the events that edit the header, add/edit/remove a row, and delete a table). Business rules stay out of the composable. **Verify:** new `TablesViewModelTest.kt` asserts a table with an incomplete header cannot become usable, a row edit is reflected, and the derived size order shown to the user is the recorded row order.
- [x] 3.3 Add `app/src/main/java/com/gexetr/pipesizing/ui/tables/TablesScreen.kt`: the table list, the applicability/citation/marker header, the row list with per-field entry and add/remove, and the derived size order shown read-only so a mis-ordered transcription is visible before a case depends on it. Content only — no `Scaffold`, no bars (the app's single `Scaffold` owns them). Per-field entry only; no paste surface. **Verify:** `./gradlew assembleDebug` from the slot plus a preview for the empty-table and populated-table states.
- [x] 3.4 Add `app/src/androidTest/java/com/gexetr/pipesizing/ui/tables/TablesScreenTest.kt` covering: opening the surface with nothing recorded shows every field empty and no prefilled row (spec: *The app supplies no table content*); deleting a table removes it and reopening the screen does not bring it back. **Verify:** the instrumentation suite passes on a device or emulator, or is reported as `UNVERIFIED` with the reason.

### 3.5 — Verification of step 3, and the inherited files it exposed (2026-09-26, orchestrator)

Stream B was finished by a Cursor `generator` after Codex was killed mid-task by its usage limit (see
§§1.5, 2.3 for the same environment hazard). The dead worker had left the tree **not compiling**:
`TablePersistence.kt` declared `suspend fun upsert(...): Preferences` where its own interface declared
`: Unit` (three times over), and `TablesUiState.kt`, `TablesViewModel.kt`, `TablesScreen.kt` did not
exist at all. A worker that dies mid-task is not a worker that failed cleanly.

Verified by the orchestrator from the artifact, never from the report:

- **Green:** `PASS testDebugUnitTest — 93 tests, 0 failures, 0 errors, 0 skipped`, exit 0, on the
  orchestrator's own run.
- **Instrumentation:** `TablesScreenTest` — 2 tests, 0 failures, on device `R58MB3SGZVL`.
- **`assembleDebug`: BUILD SUCCESSFUL.**
- **Licence gate:** every numeric literal in the new `ui/tables/**` sources and tests is structural
  (field counts, array indices). No capacity, length, diameter, multiplier or gravity value in
  sources, tests, strings or fixtures.
- **Sensors fire:** removing the escaping made the round-trip test red; making the codec abandon the
  payload on a truncated record made the readable-records test red. Both reverted, file hash
  unchanged, suite back to 93/0.

**Two inherited test files were not trustworthy, and the reason is worth keeping.** They were written
by a worker that never once executed them, so they could not have been compiled, let alone passed:

- `TablesViewModelTest.kt` referenced an extension it never imported — it could not compile for its
  author.
- The same file asserted a **single** row while its own fixture populated two, so it would have passed
  over a codec that silently dropped a row. It now asserts both rows **in recorded order**, which is
  the property that matters (D4: the derived order is the user's own row order).

That is the concrete evidence for why this change's gates demand observed output: a test file that has
never run is not coverage, it is the appearance of coverage.



## 4. Sizing-screen integration — sequential, after streams A and B, one writer

One writer only; this step consumes A's evaluator and B's repository.

- [x] 4.1 Edit `ui/home/HomeUiState.kt`: add the table-selection fields (chosen table id, optional nominal size) and a `HomeField` member or event for each, so every new input is editable through the same single write path. **Verify:** extended `HomeViewModelTest` asserts each new field is editable and that editing any of them clears the shown result.
- [x] 4.2 Edit `ui/home/HomeViewModel.kt`: resolve the selected table from `TablesRepository`, pass it into `SizingEvaluator.evaluate(case, table)`, and refuse with `NO_TABLE_SELECTED` when a table method is chosen with no table. **Verify:** extended `HomeViewModelTest` asserts the new methods compute from a recorded table, refuse on a mismatched applicability naming both values, refuse on an out-of-envelope length naming the longest recorded length, and refuse on a load above every recorded capacity naming the largest one.
- [x] 4.3 Edit `ui/home/ResultLines.kt`: add the table citation, the row used, the size returned, the substitution (`entered → used`, only when substituted), and the user-recorded plus neither-transcribed-nor-verified lines for `ReturnedFromUserTable`, keeping the existing context-first order and the existing disclaimer line. **Verify:** extended `ResultLinesTest` asserts the citation, the row, the substitution pair and the label are all present for a returned value, that the substitution lines are absent when no substitution happened, that a returned value's lines never use the arithmetic comparison's wording, and that the first line of any result still names the case.
- [x] 4.4 Edit `ui/home/HomeScreen.kt`: handle the new `SizingEvaluation.ReturnedFromUserTable` branch explicitly (no `else ->`), replace the two-option `SingleChoiceSegmentedButtonRow` for the method with a vertical choice list that holds four methods without depending on English label lengths, and reveal the table picker plus the nominal-size field for the new methods. **Verify:** `./gradlew assembleDebug`, a preview of each method's expanded state, and task 4.7's assertion that the existing disclaimer is still on every result view.
- [x] 4.5 Edit `ui/home/CasePersistence.kt`: store the chosen table's id and a SHA-256 digest of the table's encoded contents beside the case's readings, restoring them with defaults so a case saved by the shipped build still loads. **Verify:** extended `HomeViewModelTest` asserts a case saved against one table refuses with `TABLE_CHANGED` after that table's contents change, refuses with `TABLE_CHANGED` when the table is deleted, and loads unchanged when the table is untouched.
- [x] 4.6 Edit `ui/home/CaseReport.kt`: add the citation, the row used and the substitution lines, the user-recorded label and the no-verification statement — and **no other row of the table**. **Verify:** a new JVM report test asserts the report contains the citation, the row used and the disclaimer, and asserts it contains no second row's capacity or length; the test must fail if the row list is added to the report.
- [x] 4.7 Extend `app/src/androidTest/java/com/gexetr/pipesizing/ui/home/HomeScreenTest.kt`: assert the calculation-aid disclaimer is visible on a returned-value result, that the user-recorded label and the no-verification statement are visible with it, and that the table method's controls are reachable at large font scale. **Verify:** the instrumentation suite passes, or is reported as `UNVERIFIED` with the reason.

### 4.8 — Verification of step 4, and the defect its reading exposed (2026-09-27, orchestrator)

Step 4 was executed by a Cursor `generator`, after Codex became unavailable. Verified from the
artifact by the orchestrator, not from the report:

- **Green:** `PASS testDebugUnitTest — 120 tests, 0 failures, 0 errors, 0 skipped` (baseline 97; 23
  added).
- **Instrumentation:** 5 tests, 0 failures, on device `R58MB3SGZVL`.
- **`assembleDebug` and `assembleDebugAndroidTest`: BUILD SUCCESSFUL.**
- **Exhaustiveness:** `rg "else ->"` over `ui/home/**` and `sizing/**` finds nothing.
- **Boundary:** `ui/cases/**` and `ui/PlayKilnApp.kt` untouched; `strings.xml` is additions only
  (63 added, 0 deleted), so step 5.2's copy edits are still safe to make.
- **Sensors fire:** five mutations observed red one at a time, including 4.6's mandatory one — adding
  the table's row inventory to the report produced three failures, the strongest being
  *"the report must not carry the unused row's length"*.

Two corrections the pass made to pre-existing work, recorded rather than applied silently:

- `HomeScreenTest.sizingFormAndRefusalAreVisible` failed because the four-method list pushes the
  primary action below the fold on a phone; it now scrolls to the control, which is how a user
  reaches it.
- `ReportLine` gained `OfResource(stringRes, argRes)` so the report can name the method label without
  a resolved `Context`. `ReportLine` is defined in `ui/home/CaseReport.kt`, inside the boundary.

- [x] 4.9 **Defect found by step 4, living in step 2's engine — outside step 4's boundary.**
  `applicabilityRefusal` reads `case.gasSpecificGravity?.value`, so an **absent** gravity and an
  **unparseable** one produce one refusal whose wording says *"Correct the out-of-range gas specific
  gravity."* A blank field is not out of range, so the user is told to fix the wrong thing — the same
  defect class as §2.3.4, which was *"refused by the limit it hit"*. Refuse an absent reading as the
  missing-input kind and an unparseable one as the out-of-range kind. Step 4 correctly did not reach
  outside its file list to patch this. **Verify:** a `CapacityTableLookupTest` case asserting a table
  method with a blank case gravity refuses with the missing-input kind, and that an unparseable one
  still refuses with the out-of-range kind; the existing 120 tests stay green **unchanged**; and
  mutating the absent case back to the out-of-range kind makes the new test fail.

**Deferred, and written down so it is not mistaken for done:** 4.6's `buildReturnedCaseReport` is
implemented and tested but **unreachable in the app** — `ui/PlayKilnApp.kt` still calls only
`buildCaseReport`, and the Report action is gated on `SizingEvaluation.Computed`. Step 5.1 closes
this. Until it does, 4.6 is a tested function with no shipped path that reaches it, which is exactly
the difference this change's gates exist to expose.

## 5. Glue, verification and evidence — sequential, one writer, after step 4

One writer only for the shared files.

- [x] 5.1 Edit `ui/PlayKilnApp.kt`: add the fourth bottom-navigation destination (`Tables`) and construct `TablesViewModel` in the composition root beside the existing two, so switching tabs cannot drop the table being edited. **Verify:** `./gradlew assembleDebug`; record the observed layout, because the method control and a fourth tab both change screens that `store/journey.json` photographs.
- [x] 5.2 Edit `app/src/main/res/values/strings.xml`: the table surface's copy, the two new method labels, the substitution notice, the user-recorded and no-verification statements, and a truthful update to `about_body` and `guide_purpose_body` (both currently say the app only *"compares the capacity you confirmed"*, which this change makes incomplete). **Verify:** `HomeViewModelTest.shippedCopyCarriesNoScaffoldMarkers` still passes, and no string claims code compliance or that the app supplies or verifies a value.
- [x] 5.3 Edit `ui/cases/SavedCase.kt` to route `SavedCaseCodec` through `ui/storage/LineCodec.kt`, so the escaping rule exists once. **Verify:** `SavedCaseTest`'s escaping-both-ways and truncated-record checks stay green **unchanged** — they are the regression sensor for this refactor; if they needed editing, the refactor changed behaviour and is wrong.
- [x] 5.4 Adversarial sweep for embedded data — **the sensor for §0.** Search the slot's `app/src/main`, `app/src/test`, `app/src/androidTest`, `app/src/main/res` and `app/src/main/assets` for a bundled capacity table, a capacity constant, a trade-size ladder, a dimension schedule, a fitting table or an equation. Read every hit and classify it as user-entered, synthetic fixture, or a violation. **Verify:** the sweep's output is recorded in the change's evidence, and it must fail on the previous build if a fixture is planted — plant one, watch the sweep catch it, remove it.
- [x] 5.5 Run `node scripts/run-slot-tests.mjs --slot pipe-sizing` and read the full output. **Verify:** exit 0 with the new suites present; "no tests found" is not a PASS.
- [ ] 5.6 Run `node scripts/verify-slot.mjs --slot pipe-sizing --build`. **Verify:** read every line; a build-less or `UNVERIFIED` run is **not** release evidence. Note that `check-differentiators` is expected to report `BLOCKED` until the owner records the three missing `ownerDecision` entries — report it as the known dependency it is (`design.md`), never by editing the contract to make the gate pass.
- [ ] 5.7 Read the complete diff and run `/eval`. **Verify:** recorded evaluator verdict and reviewer findings. Do not touch `differentiators.json`, `store/studio.json`, `store/journey.json` or `store/listing.json` — their movement is a release-phase task against the built app (`design.md` §Deferred).

### 5.8 — Verification of step 5, and the harness defect it exposed (2026-09-27, orchestrator)

Step 5 was executed by a Cursor `generator`. Verified from the artifact by the orchestrator:

- **JVM suites:** `125 tests, 0 failures, 0 errors, 0 skipped` — but only after **forcing** execution,
  which is the subject of the defect below. The earlier readings of 93 → 97 → 120 were genuine,
  because a Kotlin change forces recompilation, which forces the task to run.
- **Instrumentation:** 8 tests, 0 failures, on `R58MB3SGZVL`.
- **`assembleDebug` / `assembleDebugAndroidTest`: BUILD SUCCESSFUL.**
- **4.9:** `RefusalKind.MISSING_INPUT` was reused rather than adding a new kind — an absent reading
  *is* a missing input, and a second name for a condition the app already names is the drift this
  repository forbids.
- **5.1:** 4.6's report is now reachable. `buildCaseReport` and `buildReturnedCaseReport` no longer
  exist; `buildReportFor` is the single entry point and the action's availability is the same
  exhaustive `when` over `SizingEvaluation`.
- **Reported limit, unchanged:** `--strict-store` was not run and no release artifact was produced.

#### 5.8.1 The defect: `run-slot-tests.mjs` printed PASS for a suite that never ran

`HomeViewModelTest.shippedCopyCarriesNoScaffoldMarkers` reads `src/main/res/values/strings.xml` off
disk. Gradle does not know that file is an input to `testDebugUnitTest`, so a **copy-only** change
leaves the task `UP-TO-DATE` and the XML under `build/test-results/` is the previous run's output.
The runner read those XML files and reported their numbers as `PASS`.

This was reproduced by the orchestrator, not inferred. With a `lorem` marker planted in a shipped
string, the runner without a fix printed:

```text
PASS testDebugUnitTest — 125 tests, 0 failures, 0 errors, 0 skipped (3346ms).
```

while the report it read was four minutes old and the suite had not executed. The same reading was
observed with nothing changed at all: `xml mtime before 00:59:19` → run → `PASS … 125 tests` →
`xml mtime after 00:59:19`, unchanged. **A verification command that reports a previous run's numbers
as PASS is a check that cannot fail**, which is the exact class `verification-discipline.mdc` exists
to forbid — and it was found by a worker whose own mutation appeared not to fire.

**Fixed in the harness.** `run-slot-tests.mjs` now derives staleness from the artifact rather than
from Gradle's exit code:

- `taskOutcome()` reads Gradle's own console line for the task; `UP-TO-DATE`, `FROM-CACHE` and
  `NO-SOURCE` are non-executing outcomes;
- `newestResultMtime()` is the backstop for when Gradle never names the task: reports that already
  existed and were not rewritten in this run cannot describe this run;
- a task that did not execute is reported **`STALE …`** and exits **2**, never `PASS`;
- `--rerun` forces execution, and is what an evidence run must use.

**The sensor was verified before it was trusted.** Four selftest legs were added, and with the
staleness guard deliberately disabled the two staleness legs go **red** while the honest-run leg and
the `--rerun` leg stay **green** — so they discriminate rather than fail on everything. Restored, the
selftest reports 0 failures.

**End-to-end proof against the real defect**, all three states observed in order:

| Runner state | Planted marker | Result |
|---|---|---|
| before the fix | `lorem` in a shipped string | `PASS testDebugUnitTest — 125 tests, 0 failures` — false |
| fixed, no `--rerun` | same marker | `STALE testDebugUnitTest — Gradle reported the task UP-TO-DATE; the 125 test(s) on disk are from an earlier run and did not execute now.` exit 2 |
| fixed, `--rerun` | same marker | `FAIL testDebugUnitTest` — `HomeViewModelTest > shippedCopyCarriesNoScaffoldMarkers FAILED` |

The third row matters most: the copy sensor was never broken. The runner was hiding it. The marker was
reverted and `strings.xml` hashes byte-identically to its pre-plant value
(`6CDF25A4…5788C`).

#### 5.8.2 Flagged, not changed: plausible real values in pre-existing test fixtures

The §0 sweep found `0.62`, `0.3` and `1.049` used together as a gas specific gravity, a pressure-drop
allowance and an inside diameter in `SizingEvaluatorTest.kt`, `HomeViewModelTest.kt` and
`ResultLinesTest.kt`. `1.049` is the Schedule-40 inside diameter for 1" nominal, which is a
code-derived value rather than an invented one, and §0 applies to fixtures explicitly. These files are
**pre-existing and outside step 5's boundary**, so they were reported instead of edited. They are
test-only and never ship in the APK. This is the weakest hit in the tree and it is recorded here rather
than left to a reader of the sweep output.

#### 5.8.2 (resolved) Plausible real values in test fixtures — renamed

The §0 sweep found `0.62`, `0.3` and `1.049` used together as a gas specific gravity, a pressure-drop
allowance and an inside diameter. None was invented: **0.62 is the standard natural-gas specific
gravity** and **1.049 is the Schedule-40 inside diameter for 1" nominal**, so the fixtures contradicted
the declaration §0 requires them to carry. The owner's decision was to rename rather than to document
them as acceptable.

A full inventory of user-input-like literals in `test/`, `androidTest/` and shipped `@Preview` bodies
classified every hit as a real code value, an invented synthetic, or structural. The real ones were
replaced with values that cannot plausibly be code data — `0.62 → 21.5`, `0.3 → 35.5`,
`1.049 → 88.5`, and the gas-gravity pair `0.5`/`0.6` → `21.5`/`22.5`, across six files. The deliberate
near-miss stayed a *near* miss, and that was proved: collapsing it to equality made
`aMismatchedApplicabilityIsRefusedNamingBothValues` fail.

**Left alone, deliberately, and the reason matters.** `0.3048` (feet↔metres) and `0.2930710701722222`
(MBH→kW) are real standard values, but they are *unit identities*, not capacity or trade readings, and
they are the subject of the conversion tests — replacing them would destroy the sensor rather than
make the slot more defensible. The distinction the gate turns on is "a value the app would be
reproducing from a code book", not "a number that is correct". Fitting allowances of a round `1.5 ft`
and `4.0 ft` were judged invented on the same reasoning: generic round fillers, not the recognizable
figures the gate names.

Verified: **125 JVM tests / 0 failures** forced with `--rerun`, `BUILD SUCCESSFUL`, and **8
instrumentation tests / 0 failures** on `R58MB3SGZVL` (4 + 2 + 2). No Schedule-40 inside diameter or
0.62 gravity remains anywhere under the slot's `src/`.

**Still open:** 5.6 cannot be a PASS while `check-differentiators` is `BLOCKED` on the three `core`
promises awaiting the owner's own words, and 5.7 (`/eval`) has not run. The store pack on disk is bound
by `store/provenance.json` to a build that predates this change, so the screenshots, tablet sets and
feature graphic depict the pre-change app and need a re-capture in `/release`.

The contract itself is now **stale relative to the artifact**, which the owner must weigh when giving
those words: `returns-the-missing-values` is recorded as `dropped` although the app now returns values
it previously refused, and `records-the-citation`'s `lostCoverage` says *"nothing verifies that the
cited row actually supports the load"* although the lookup now returns that row and states whether it
supports the load. Neither is corrected here — rewriting a `core` promise's justification is a decision
for the owner, and the gate exists to make it an explicit one rather than an agent's edit.

---

## Parallelism plan

Two workers run in parallel after the contract step, on **disjoint** file lists. Nothing in stream A is in stream B's list, and neither edits `SizingDomain.kt`.

| Phase | Owner | Files (exclusive) | Runs |
|---|---|---|---|
| 1 | one writer (contract) | `sizing/CapacityTable.kt`, `sizing/SizingDomain.kt`, `ui/storage/LineCodec.kt`, `test/.../sizing/CapacityTableTest.kt`, `test/.../sizing/SizingDispatchTest.kt` | sequential, blocks 2 and 3 |
| 2 | **worker A** (lookup engine) | `sizing/CapacityTableLookup.kt`, `test/.../sizing/CapacityTableLookupTest.kt` | parallel with worker B |
| 3 | **worker B** (store + table surface) | `ui/tables/TablePersistence.kt`, `ui/tables/TablesUiState.kt`, `ui/tables/TablesViewModel.kt`, `ui/tables/TablesScreen.kt`, `test/.../ui/tables/TableCodecTest.kt`, `test/.../ui/tables/TablesViewModelTest.kt`, `androidTest/.../ui/tables/TablesScreenTest.kt` | parallel with worker A |
| 4 | one writer (integration) | `ui/home/HomeUiState.kt`, `ui/home/HomeViewModel.kt`, `ui/home/ResultLines.kt`, `ui/home/HomeScreen.kt`, `ui/home/CasePersistence.kt`, `ui/home/CaseReport.kt`, `test/.../ui/home/*`, `androidTest/.../ui/home/HomeScreenTest.kt` | sequential, after A **and** B |
| 5 | one writer (glue + verify) | `ui/PlayKilnApp.kt`, `res/values/strings.xml`, `ui/cases/SavedCase.kt` | sequential, after step 4 |

Why the split is safe and why step 4 is not parallel: stream A produces the evaluator, stream B produces the repository and the table surface, and step 4 is the only place the two meet — in `ui/home/*`. Steps 4 and 5 are each single-writer because they are the two points where every earlier stream's API is consumed, and two writers there would contend for `HomeViewModel.kt` and `PlayKilnApp.kt`. `res/values/strings.xml` is touched once, at the very end, because both stream B's screen and step 4's screen need strings and two writers would collide on one file.

If a worker reports `RATE_LIMITED`, hand that worker's file list to the Cursor `generator` subagent per `harness/dual-agent.md`; do not disable the other stream.

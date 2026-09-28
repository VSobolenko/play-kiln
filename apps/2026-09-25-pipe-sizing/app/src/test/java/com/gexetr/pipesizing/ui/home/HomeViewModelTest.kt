package com.gexetr.pipesizing.ui.home

import com.gexetr.pipesizing.sizing.LengthSubstitution
import com.gexetr.pipesizing.sizing.RefusalKind
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UnitSystem
import com.gexetr.pipesizing.sizing.Units
import com.gexetr.pipesizing.sizing.UserCapacityTable
import com.gexetr.pipesizing.sizing.UserProvenance
import com.gexetr.pipesizing.ui.tables.TablesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeViewModelTest {

    @Test
    fun refusedEvaluationSurfacesCorrectionAndNoResultValue() {
        val result = evaluate(newVm())

        assertTrue(result is SizingEvaluation.Refused)
        val refused = result as SizingEvaluation.Refused
        assertTrue(refused.correctionRequired.isNotBlank())
        // The Refused branch carries no numeric result at all, so no screen can render a value for it.
        assertFalse(result is SizingEvaluation.Computed)
    }

    @Test
    fun codeCitationReachesComputedResult() {
        val vm = completeVm()

        field(vm, HomeField.EDITION, "2024")
        field(vm, HomeField.TABLE, "A")
        field(vm, HomeField.ROW, "1")
        vm.onEvent(HomeEvent.ProvenanceChanged(ProvenanceMode.CODE_CITATION))

        val result = evaluate(vm) as SizingEvaluation.Computed
        val citation = result.capacityProvenance as UserProvenance.CodeCitation
        assertEquals("2024", citation.codeAndEdition)
        assertEquals("A", citation.table)
        assertEquals("1", citation.row)
    }

    @Test
    fun aCitationWithNoEditionOrTableIsRefusedRatherThanRecorded() {
        val vm = completeVm()
        vm.onEvent(HomeEvent.ProvenanceChanged(ProvenanceMode.CODE_CITATION))

        // Selecting the citation mode without naming a source records nothing, so the case must not
        // compute with an empty citation standing in for provenance.
        val result = evaluate(vm)

        assertTrue(result is SizingEvaluation.Refused)
        val refused = result as SizingEvaluation.Refused
        assertEquals(RefusalKind.MISSING_PROVENANCE, refused.kind)
        assertTrue(refused.correctionRequired.contains("provenance", ignoreCase = true))
    }

    @Test
    fun aCitationWithOnlyAnEditionIsRefused() {
        val vm = completeVm()
        vm.onEvent(HomeEvent.ProvenanceChanged(ProvenanceMode.CODE_CITATION))
        field(vm, HomeField.EDITION, "2024")

        val result = evaluate(vm)

        assertTrue(result is SizingEvaluation.Refused)
        assertEquals(RefusalKind.MISSING_PROVENANCE, (result as SizingEvaluation.Refused).kind)
    }

    @Test
    fun aCitationCompletesOnceEditionAndTableAreNamed() {
        val vm = completeVm()
        vm.onEvent(HomeEvent.ProvenanceChanged(ProvenanceMode.CODE_CITATION))
        field(vm, HomeField.EDITION, "2024")
        field(vm, HomeField.TABLE, "A")

        // The row is not required: an edition and a table already identify the source.
        assertTrue(evaluate(vm) is SizingEvaluation.Computed)
    }

    @Test
    fun capacityMustBeConfirmedBeforeAnyResultIsShown() {
        val vm = newVm()
        field(vm, HomeField.MATERIAL, "steel")
        field(vm, HomeField.PRESSURE, "low")
        field(vm, HomeField.LOAD, "100")
        field(vm, HomeField.LENGTH, "40")
        field(vm, HomeField.CAPACITY, "120")

        val unconfirmed = evaluate(vm)

        assertTrue(unconfirmed is SizingEvaluation.Refused)
        assertEquals(RefusalKind.MISSING_INPUT, (unconfirmed as SizingEvaluation.Refused).kind)
    }

    @Test
    fun switchingMethodInvalidatesThePreviousCapacityConfirmation() {
        val vm = completeVm()
        assertTrue(evaluate(vm) is SizingEvaluation.Computed)

        vm.onEvent(HomeEvent.MethodChanged(SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS))
        val afterSwitch = evaluate(vm)

        // The capacity confirmed for the previous method is not reused; nothing is computed until the
        // user confirms a capacity for the method now selected.
        assertTrue(afterSwitch is SizingEvaluation.Refused)
        val refused = afterSwitch as SizingEvaluation.Refused
        assertEquals(RefusalKind.MISSING_INPUT, refused.kind)
        assertTrue(refused.correctionRequired.contains("capacity", ignoreCase = true))
    }

    @Test
    fun physicalMethodNamesTheInputItStillNeeds() {
        val vm = completeVm(method = SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS)

        val missing = evaluate(vm)

        assertTrue(missing is SizingEvaluation.Refused)
        val refused = missing as SizingEvaluation.Refused
        assertEquals(RefusalKind.MISSING_INPUT, refused.kind)
        assertTrue(refused.correctionRequired.contains("gas specific gravity", ignoreCase = true))
    }

    @Test
    fun physicalMethodComputesOnceItsInputsAreSupplied() {
        val vm = completeVm(method = SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS)
        // Invented synthetics, not code or standard readings: 21.5 is no gas's specific gravity,
        // 35.5 is no pressure-drop allowance, 88.5 is no pipe's inside diameter.
        field(vm, HomeField.GAS_SPECIFIC_GRAVITY, "21.5")
        field(vm, HomeField.PRESSURE_DROP, "35.5")
        field(vm, HomeField.PIPE_DIAMETER, "88.5")

        assertTrue(evaluate(vm) is SizingEvaluation.Computed)
    }

    @Test
    fun caseWithoutFittingsComputes() {
        val vm = completeVm(withFittings = false)
        field(vm, HomeField.FITTING_QUANTITY, "")
        field(vm, HomeField.FITTING_LENGTH, "")

        val result = evaluate(vm) as SizingEvaluation.Computed

        // A run with no fittings adds nothing, and is not refused for a quantity that does not apply.
        assertEquals(0.0, result.fittingAllowanceFeet, TOLERANCE)
        assertEquals(40.0, result.totalLengthFeet, TOLERANCE)
    }

    @Test
    fun metricEntryProducesTheSamePhysicalValuesAsImperial() {
        val imperial = evaluate(completeVm()) as SizingEvaluation.Computed
        val metric = evaluate(completeVm(metric = true)) as SizingEvaluation.Computed

        // The metric run enters the same physical case in kW and metres. If the metric entry were not
        // converted, the sums below would differ by the conversion factor rather than matching.
        assertEquals(100.0, metric.requiredLoadMbh, TOLERANCE)
        assertEquals(40.0, metric.developedLengthFeet, TOLERANCE)
        assertEquals(5.0, metric.fittingAllowanceFeet, TOLERANCE)
        assertEquals(45.0, metric.totalLengthFeet, TOLERANCE)
        assertEquals(imperial.requiredLoadMbh, metric.requiredLoadMbh, TOLERANCE)
        assertEquals(imperial.totalLengthFeet, metric.totalLengthFeet, TOLERANCE)
        assertEquals(imperial.supportsRequiredLoad, metric.supportsRequiredLoad)
        assertEquals(imperial.capacityProvenance, metric.capacityProvenance)
    }

    @Test
    fun switchingUnitsPreservesEveryPhysicalValue() {
        val vm = completeVm()
        val before = evaluate(vm) as SizingEvaluation.Computed

        vm.onEvent(HomeEvent.UnitChanged(UnitSystem.METRIC))
        val metric = evaluate(vm) as SizingEvaluation.Computed

        // The readings are re-expressed in metric, not retyped: the case is arithmetically identical.
        assertEquals(before.requiredLoadMbh, metric.requiredLoadMbh, TOLERANCE)
        assertEquals(before.totalLengthFeet, metric.totalLengthFeet, TOLERANCE)
        assertEquals(before.fittingAllowanceFeet, metric.fittingAllowanceFeet, TOLERANCE)
        assertEquals(before.supportsRequiredLoad, metric.supportsRequiredLoad)
        assertEquals(before.capacityProvenance, metric.capacityProvenance)
        // And the fields now show the metric equivalent rather than the imperial figure relabelled.
        // These are literal expectations on purpose: deriving them from Units would make the test
        // unfalsifiable, so they are the values a correct conversion must produce and they are tight
        // enough (1e-6) to fail against the imprecise 0.2930711111111111 MBH constant as well.
        assertEquals("29.3071", vm.uiState.value.loadMbh)
        assertEquals("12.192", vm.uiState.value.lengthFeet)
        assertEquals("1.524", vm.uiState.value.fittingLengthFeet)
        assertEquals("35.1685", vm.uiState.value.capacity)
        // The relabelling failure mode this replaces would have left the imperial numbers in place.
        assertFalse(vm.uiState.value.loadMbh.startsWith("100"))
    }

    @Test
    fun theShownLengthValueStaysReadable() {
        val vm = completeVm()

        vm.onEvent(HomeEvent.UnitChanged(UnitSystem.METRIC))

        // The converted load is shown rounded to something a person can read. The value behind it is
        // untouched, which is why this is a display rule and not a limit on the arithmetic.
        assertEquals("29.3071", vm.uiState.value.loadMbh)
        assertTrue(vm.uiState.value.loadMbh.length < 9)
        // The exact canonical value is still what the arithmetic runs on.
        vm.onEvent(HomeEvent.Evaluate)
        val result = vm.uiState.value.evaluation as SizingEvaluation.Computed
        assertEquals(100.0, result.requiredLoadMbh, TOLERANCE)
    }

    @Test
    fun repeatedUnitSwitchingDoesNotDrift() {
        val vm = completeVm()
        evaluate(vm)

        repeat(10) {
            vm.onEvent(HomeEvent.UnitChanged(UnitSystem.METRIC))
            vm.onEvent(HomeEvent.UnitChanged(UnitSystem.IMPERIAL))
        }

        // Canonical readings never move on a unit switch, so ten round trips land on the exact
        // original values rather than merely close to them.
        val result = evaluate(vm) as SizingEvaluation.Computed
        assertEquals(100.0, result.requiredLoadMbh, 0.0)
        assertEquals(45.0, result.totalLengthFeet, 0.0)
        assertEquals("100", vm.uiState.value.loadMbh)
        assertEquals("40", vm.uiState.value.lengthFeet)
    }

    @Test
    fun editingAnInputClearsThePreviousResult() {
        val vm = completeVm()
        assertTrue(evaluate(vm) is SizingEvaluation.Computed)

        field(vm, HomeField.LOAD, "150")

        // A result computed for the old load must not stay on screen beside the new one.
        assertNull(vm.uiState.value.evaluation)
    }

    @Test
    fun changingTheProvenanceClearsThePreviousResult() {
        val vm = completeVm()
        assertTrue(evaluate(vm) is SizingEvaluation.Computed)

        vm.onEvent(HomeEvent.ProvenanceChanged(ProvenanceMode.CODE_CITATION))

        assertNull(vm.uiState.value.evaluation)
    }

    @Test
    fun changingTheUnitClearsThePreviousResult() {
        val vm = completeVm()
        assertTrue(evaluate(vm) is SizingEvaluation.Computed)

        vm.onEvent(HomeEvent.UnitChanged(UnitSystem.METRIC))

        assertNull(vm.uiState.value.evaluation)
    }

    @Test
    fun aMetricCaseRestoredFromStoreComputesTheSamePhysicalValues() {
        val store = FakeCasePersistence()
        val original = evaluate(completeVm(metric = true, store = store)) as SizingEvaluation.Computed

        // The canonical readings are not stored, so this proves they are rebuilt correctly from the
        // restored text and unit mode rather than silently defaulting to imperial.
        val restored = evaluate(newVm(store)) as SizingEvaluation.Computed

        assertEquals(original.requiredLoadMbh, restored.requiredLoadMbh, TOLERANCE)
        assertEquals(original.totalLengthFeet, restored.totalLengthFeet, TOLERANCE)
        assertEquals(original.fittingAllowanceFeet, restored.fittingAllowanceFeet, TOLERANCE)
        assertEquals(UnitSystem.METRIC, newVm(store).uiState.value.unitSystem)
    }

    @Test
    fun aUnitSwitchKeepsTextThatNeverParsed() {
        val vm = completeVm()
        field(vm, HomeField.LENGTH, "4o")

        vm.onEvent(HomeEvent.UnitChanged(UnitSystem.METRIC))

        // There is no value to convert here, and replacing what the user is still correcting with a
        // conversion of nothing would destroy their entry. It is carried across verbatim, and the
        // evaluator keeps reporting it as unusable.
        assertEquals("4o", vm.uiState.value.lengthFeet)
        assertEquals(RefusalKind.OUT_OF_RANGE, (evaluate(vm) as SizingEvaluation.Refused).kind)
    }

    @Test
    fun aUnitSwitchKeepsBlankFieldsBlank() {
        val vm = newVm()

        vm.onEvent(HomeEvent.UnitChanged(UnitSystem.METRIC))

        // A blank field is an absent input. It must not acquire a value from the unit switch.
        assertEquals("", vm.uiState.value.loadMbh)
        assertEquals("", vm.uiState.value.lengthFeet)
        assertEquals("", vm.uiState.value.fittingLengthFeet)
        assertEquals("", vm.uiState.value.capacity)
    }

    @Test
    fun enteredCaseIsWrittenToTheStore() {
        val store = FakeCasePersistence()
        val vm = completeVm(store = store)

        assertEquals("steel", store.saved[CaseKeys.MATERIAL])
        assertEquals(entry(100.0, metric = false, Units::mbhToKilowatts), store.saved[CaseKeys.LOAD])
        assertEquals(SizingMethod.CAPACITY_COMPARISON.name, store.saved[CaseKeys.METHOD])
        assertEquals(true.toString(), store.saved[CaseKeys.CAPACITY_CONFIRMED])
    }

    @Test
    fun storedCaseIsRestoredAsThoughTheProcessHadDied() {
        val store = FakeCasePersistence()
        val first = completeVm(store = store)
        val original = first.uiState.value

        // A second state holder over the same store stands in for the next app run.
        val restored = newVm(store).uiState.value

        assertEquals(original.material, restored.material)
        assertEquals(original.pressure, restored.pressure)
        assertEquals(original.loadMbh, restored.loadMbh)
        assertEquals(original.lengthFeet, restored.lengthFeet)
        assertEquals(original.fittingQuantity, restored.fittingQuantity)
        assertEquals(original.fittingLengthFeet, restored.fittingLengthFeet)
        assertEquals(original.capacity, restored.capacity)
        assertEquals(original.method, restored.method)
        assertEquals(original.unitSystem, restored.unitSystem)
        assertEquals(original.provenanceMode, restored.provenanceMode)
        assertEquals(original.capacityConfirmed, restored.capacityConfirmed)
    }

    @Test
    fun aRestoredCaseStillComputes() {
        val store = FakeCasePersistence()
        completeVm(store = store)

        val restored = newVm(store)

        val result = evaluate(restored) as SizingEvaluation.Computed
        assertEquals(45.0, result.totalLengthFeet, TOLERANCE)
    }

    @Test
    fun aRestoredCaseIsNotTreatedAsEvaluated() {
        val store = FakeCasePersistence()
        evaluate(completeVm(store = store))

        val restored = newVm(store)

        // A stored result would otherwise be shown for inputs the user cannot see, so none is kept.
        assertNull(restored.uiState.value.evaluation)
    }

    @Test
    fun aRestoredCaseWithoutAConfirmedCapacityStillRefuses() {
        val store = FakeCasePersistence()
        val vm = newVm(store)
        field(vm, HomeField.MATERIAL, "steel")
        field(vm, HomeField.PRESSURE, "low")
        field(vm, HomeField.LOAD, "100")
        field(vm, HomeField.LENGTH, "40")
        field(vm, HomeField.CAPACITY, "120")

        // Restoring the entered readings must not also restore a result or a confirmation.
        val restored = newVm(store)
        val result = evaluate(restored)

        assertTrue(result is SizingEvaluation.Refused)
        assertEquals(RefusalKind.MISSING_INPUT, (result as SizingEvaluation.Refused).kind)
    }

    // -------------------------------------------------------------------------------------------
    // Step 4.1 — the table-selection fields are editable through the same single write path, and
    // editing either of them clears the shown result like every other input does.
    // -------------------------------------------------------------------------------------------

    @Test
    fun choosingARecordedTableIsEditableAndClearsTheShownResult() {
        val table = syntheticTable()
        val vm = tableVm(TablesRepository.InMemory(listOf(table)))
        evaluate(enterTableCase(vm, table.id))
        assertTrue(vm.uiState.value.evaluation is SizingEvaluation.ReturnedFromUserTable)

        field(vm, HomeField.SELECTED_TABLE, table.id)

        assertEquals(table.id, vm.uiState.value.selectedTableId)
        // Choosing a table records the revision of the contents that were chosen (design D9), so a
        // later edit to that table can be detected rather than answered from a revision the case was
        // never read against.
        assertEquals(table.contentDigest(), vm.uiState.value.tableDigest)
        assertNull(vm.uiState.value.evaluation)
    }

    @Test
    fun enteringTheNominalSizeIsEditableAndClearsTheShownResult() {
        val table = syntheticTable()
        val vm = tableVm(TablesRepository.InMemory(listOf(table)))
        evaluate(enterTableCase(vm, table.id))
        assertTrue(vm.uiState.value.evaluation is SizingEvaluation.ReturnedFromUserTable)

        field(vm, HomeField.TABLE_NOMINAL_SIZE, "synthetic-size-b")

        assertEquals("synthetic-size-b", vm.uiState.value.tableNominalSize)
        assertNull(vm.uiState.value.evaluation)
    }

    // -------------------------------------------------------------------------------------------
    // Step 4.2 — the new methods compute from a table the user recorded, and every refusal names
    // the field or the limit it hit. Nothing below asserts a capacity the app supplied: the rows are
    // invented synthetic readings ("synthetic-size-a", 1.0, 2.0) that say so.
    // -------------------------------------------------------------------------------------------

    @Test
    fun theForwardMethodReturnsTheRecordedRowForTheEnteredSize() {
        val table = syntheticTable()
        val vm = tableVm(TablesRepository.InMemory(listOf(table)))

        val returned = evaluate(enterTableCase(vm, table.id)) as SizingEvaluation.ReturnedFromUserTable

        assertEquals("synthetic-size-a", returned.returnedSize)
        assertEquals(2.0, returned.returnedCapacityMbh, 0.0)
        assertNull(returned.substitution)
        // The citation travels with the value, so a returned number is never shown without its source.
        assertEquals(table.citation.codeAndEdition, returned.capacityProvenance.codeAndEdition)
        assertEquals(table.citation.tableIdentifier, returned.capacityProvenance.tableIdentifier)
    }

    @Test
    fun theForwardMethodReportsTheLengthItSubstitutedFromTheRecordedRows() {
        val table = syntheticTable(
            rows = listOf(
                TableRow("synthetic-size-a", null, 1.0, 2.0),
                TableRow("synthetic-size-a", null, 3.0, 1.0),
            ),
        )
        val vm = tableVm(TablesRepository.InMemory(listOf(table)))

        // No row is recorded at 2.0, so the closest larger recorded length is used and both values
        // are reported; the capacity returned is the one recorded at the length actually used.
        val returned = evaluate(enterTableCase(vm, table.id, length = "2")) as SizingEvaluation.ReturnedFromUserTable

        assertEquals(LengthSubstitution(2.0, 3.0), returned.substitution)
        assertEquals(1.0, returned.returnedCapacityMbh, 0.0)
        assertNotEquals(2.0, returned.returnedCapacityMbh, 0.0)
    }

    @Test
    fun theReverseMethodReturnsTheSmallestRecordedSizeThatCarriesTheLoad() {
        val table = syntheticTable(
            rows = listOf(
                TableRow("synthetic-size-a", null, 1.0, 2.0),
                TableRow("synthetic-size-b", null, 1.0, 4.0),
            ),
        )
        val vm = tableVm(TablesRepository.InMemory(listOf(table)))
        enterTableCase(vm, table.id, method = SizingMethod.SMALLEST_SIZE_FOR_LOAD)

        // Both recorded sizes carry a load of 1, so the first in the user's own recorded order wins.
        val light = evaluate(vm) as SizingEvaluation.ReturnedFromUserTable
        assertEquals("synthetic-size-a", light.returnedSize)
        assertEquals(2.0, light.returnedCapacityMbh, 0.0)

        // The smallest one no longer carries a load of 3, so the next recorded size is returned.
        field(vm, HomeField.LOAD, "3")
        val heavier = evaluate(vm) as SizingEvaluation.ReturnedFromUserTable
        assertEquals("synthetic-size-b", heavier.returnedSize)
        assertEquals(4.0, heavier.returnedCapacityMbh, 0.0)
    }

    @Test
    fun aTableMethodWithNoRecordedTableSelectedIsRefused() {
        val vm = tableVm(TablesRepository.InMemory(listOf(syntheticTable())))

        val refused = evaluate(enterTableCase(vm, tableId = "")) as SizingEvaluation.Refused

        assertEquals(RefusalKind.NO_TABLE_SELECTED, refused.kind)
        assertTrue(refused.correctionRequired.contains("table", ignoreCase = true))
    }

    @Test
    fun aMismatchedApplicabilityIsRefusedNamingBothValues() {
        val table = syntheticTable()
        val vm = tableVm(TablesRepository.InMemory(listOf(table)))
        enterTableCase(vm, table.id)

        field(vm, HomeField.MATERIAL, "other system")
        val material = evaluate(vm) as SizingEvaluation.Refused
        assertEquals(RefusalKind.APPLICABILITY_MISMATCH, material.kind)
        assertTrue(material.correctionRequired.contains("other system"))
        assertTrue(material.correctionRequired.contains("synthetic system"))

        // The gravity comparison is exact, and the refusal names the case's reading and the table's.
        // 22.5 is a deliberate near miss of the table's invented 21.5: close enough that a rounded
        // comparison would accept it, and the test still exercises a mismatch rather than equality.
        field(vm, HomeField.MATERIAL, "synthetic system")
        field(vm, HomeField.GAS_SPECIFIC_GRAVITY, "22.5")
        val gravity = evaluate(vm) as SizingEvaluation.Refused
        assertEquals(RefusalKind.APPLICABILITY_MISMATCH, gravity.kind)
        assertTrue(gravity.correctionRequired.contains("22.5"))
        assertTrue(gravity.correctionRequired.contains("21.5"))
    }

    @Test
    fun aLengthAboveEveryRecordedRowIsRefusedNamingTheLongestRecordedLength() {
        val table = syntheticTable(rows = listOf(TableRow("synthetic-size-a", null, 1.0, 2.0)))
        val vm = tableVm(TablesRepository.InMemory(listOf(table)))

        val refused = evaluate(enterTableCase(vm, table.id, length = "2")) as SizingEvaluation.Refused

        assertEquals(RefusalKind.LENGTH_ABOVE_EVERY_ENTERED_ROW, refused.kind)
        assertTrue(refused.correctionRequired.contains("1.0"))
        assertTrue(refused.correctionRequired.contains("synthetic-size-a"))
    }

    @Test
    fun aLoadAboveEveryRecordedCapacityIsRefusedNamingTheLargestRecordedCapacity() {
        val table = syntheticTable(
            rows = listOf(
                TableRow("synthetic-size-a", null, 1.0, 2.0),
                TableRow("synthetic-size-b", null, 1.0, 4.0),
            ),
        )
        val vm = tableVm(TablesRepository.InMemory(listOf(table)))
        enterTableCase(vm, table.id, method = SizingMethod.SMALLEST_SIZE_FOR_LOAD)
        field(vm, HomeField.LOAD, "9")

        val refused = evaluate(vm) as SizingEvaluation.Refused

        assertEquals(RefusalKind.LOAD_ABOVE_EVERY_ENTERED_CAPACITY, refused.kind)
        assertTrue(refused.correctionRequired.contains("4.0"))
        assertTrue(refused.correctionRequired.contains("synthetic-size-b"))
    }

    @Test
    fun theReverseMethodAnswersWithoutANominalSize() {
        val table = syntheticTable()
        val vm = tableVm(TablesRepository.InMemory(listOf(table)))

        // The reverse method searches every recorded size, so asking for one would be asking for a
        // reading the method does not read.
        val returned = evaluate(
            enterTableCase(vm, table.id, size = null, method = SizingMethod.SMALLEST_SIZE_FOR_LOAD),
        ) as SizingEvaluation.ReturnedFromUserTable

        assertEquals("synthetic-size-a", returned.returnedSize)
    }

    // -------------------------------------------------------------------------------------------
    // Step 4.5 — a case binds to the revision of the table it was read against (design D9), and a
    // case written before those fields existed still loads.
    // -------------------------------------------------------------------------------------------

    @Test
    fun aCaseSavedAgainstATableRefusesWhenThatTablesRecordedContentsChange() {
        val store = FakeCasePersistence()
        val table = syntheticTable()
        val repository = TablesRepository.InMemory(listOf(table))
        val original = tableVm(repository, store)
        assertEquals(
            2.0,
            (evaluate(enterTableCase(original, table.id)) as SizingEvaluation.ReturnedFromUserTable).returnedCapacityMbh,
            0.0,
        )
        assertEquals(table.id, store.saved[CaseKeys.SELECTED_TABLE])
        assertEquals(table.contentDigest(), store.saved[CaseKeys.TABLE_DIGEST])

        // The user edits the capacity the case was read against, then the app is started again.
        runBlocking { repository.upsert(table.copy(rows = listOf(TableRow("synthetic-size-a", null, 1.0, 9.0)))) }
        val nextRun = tableVm(repository, store)

        val refused = evaluate(nextRun) as SizingEvaluation.Refused

        assertEquals(RefusalKind.TABLE_CHANGED, refused.kind)
        // The readings themselves load unchanged; only the value returned from the table is withheld.
        assertEquals("synthetic system", nextRun.uiState.value.material)
        assertEquals("1", nextRun.uiState.value.loadMbh)
    }

    @Test
    fun aCaseSavedAgainstATableRefusesWhenThatTableIsDeleted() {
        val store = FakeCasePersistence()
        val table = syntheticTable()
        val repository = TablesRepository.InMemory(listOf(table))
        evaluate(enterTableCase(tableVm(repository, store), table.id))

        runBlocking { repository.delete(table.id) }
        val nextRun = tableVm(repository, store)

        val refused = evaluate(nextRun) as SizingEvaluation.Refused

        assertEquals(RefusalKind.TABLE_CHANGED, refused.kind)
        assertEquals(table.id, nextRun.uiState.value.selectedTableId)
    }

    @Test
    fun aCaseSavedAgainstAnUntouchedTableReturnsTheSameRecordedValue() {
        val store = FakeCasePersistence()
        val table = syntheticTable()
        val repository = TablesRepository.InMemory(listOf(table))
        evaluate(enterTableCase(tableVm(repository, store), table.id))

        val nextRun = tableVm(repository, store)
        val returned = evaluate(nextRun) as SizingEvaluation.ReturnedFromUserTable

        assertEquals("synthetic-size-a", returned.returnedSize)
        assertEquals(2.0, returned.returnedCapacityMbh, 0.0)
    }

    @Test
    fun aCaseSavedBeforeTheTableFieldsExistedLoadsWithNoTableSelected() {
        // Exactly the keys the shipped build wrote: the three table-binding keys are absent, and
        // their absence must be a defined state rather than a crash or a guessed table.
        val shipped = mapOf(
            CaseKeys.MATERIAL to "steel",
            CaseKeys.PRESSURE to "low",
            CaseKeys.LOAD to "100",
            CaseKeys.LENGTH to "40",
            CaseKeys.CAPACITY to "120",
            CaseKeys.METHOD to SizingMethod.CAPACITY_COMPARISON.name,
            CaseKeys.UNIT_SYSTEM to UnitSystem.IMPERIAL.name,
            CaseKeys.PROVENANCE_MODE to ProvenanceMode.USER_SUPPLIED.name,
            CaseKeys.CAPACITY_CONFIRMED to true.toString(),
        )
        val restored = newVm(FakeCasePersistence(shipped))

        val state = restored.uiState.value
        assertEquals("", state.selectedTableId)
        assertEquals("", state.tableDigest)
        assertEquals("", state.tableNominalSize)
        // And the case itself is unaffected: the arithmetic methods never read the table fields.
        assertTrue(evaluate(restored) is SizingEvaluation.Computed)
    }

    @Test
    fun shippedCopyCarriesNoScaffoldMarkers() {
        val xml = stringsXml().readText().lowercase()

        // These are the markers the scaffold ships with and that reached a store listing before. This
        // checks the resource that actually ships, not a string built inside the test.
        listOf("starter", "scaffold", "skeleton", "meant to be replaced", "lorem").forEach {
            assertFalse("user-visible copy contains '$it'", xml.contains(it))
        }
    }

    private fun stringsXml(): File =
        listOf(File("src/main/res/values/strings.xml"), File("app/src/main/res/values/strings.xml"))
            .firstOrNull { it.isFile }
            ?: error("strings.xml not found from ${File("").absolutePath}")

    private fun newVm(store: CasePersistence = FakeCasePersistence()): HomeViewModel =
        HomeViewModel(persistence = store, scope = CoroutineScope(Dispatchers.Unconfined))

    private fun completeVm(
        method: SizingMethod = SizingMethod.CAPACITY_COMPARISON,
        metric: Boolean = false,
        withFittings: Boolean = true,
        store: CasePersistence = FakeCasePersistence(),
    ): HomeViewModel {
        val vm = newVm(store)
        vm.onEvent(HomeEvent.UnitChanged(if (metric) UnitSystem.METRIC else UnitSystem.IMPERIAL))
        vm.onEvent(HomeEvent.MethodChanged(method))
        field(vm, HomeField.MATERIAL, "steel")
        field(vm, HomeField.PRESSURE, "low")
        field(vm, HomeField.LOAD, entry(100.0, metric, Units::mbhToKilowatts))
        field(vm, HomeField.LENGTH, entry(40.0, metric, Units::feetToMeters))
        if (withFittings) {
            field(vm, HomeField.FITTING_QUANTITY, "1")
            field(vm, HomeField.FITTING_LENGTH, entry(5.0, metric, Units::feetToMeters))
        }
        field(vm, HomeField.CAPACITY, entry(120.0, metric, Units::mbhToKilowatts))
        vm.onEvent(HomeEvent.ConfirmCapacity)
        return vm
    }

    /** The text a user would type for [canonical] when the given unit mode is selected. */
    private fun entry(canonical: Double, metric: Boolean, canonicalToMetric: (Double) -> Double): String =
        (if (metric) canonicalToMetric(canonical) else canonical).toString()

    private fun evaluate(vm: HomeViewModel): SizingEvaluation {
        vm.onEvent(HomeEvent.Evaluate)
        return requireNotNull(vm.uiState.value.evaluation)
    }

    private fun field(vm: HomeViewModel, field: HomeField, value: String) {
        vm.onEvent(HomeEvent.FieldChanged(field, value))
    }

    private class FakeCasePersistence(private var stored: Map<String, String> = emptyMap()) : CasePersistence {
        val saved: Map<String, String> get() = stored

        override suspend fun load(): Map<String, String> = stored

        override suspend fun save(values: Map<String, String>) {
            stored = values
        }
    }

    /**
     * A state holder over a table store. The table flow is mirrored into the state when the state
     * holder starts, so the assertion below pins that premise: without it a refused evaluation could
     * be caused by a store read that had not finished rather than by the rule under test.
     */
    private fun tableVm(repo: TablesRepository, store: CasePersistence = FakeCasePersistence()): HomeViewModel {
        val expected = runBlocking { repo.tables.first().size }
        val vm = HomeViewModel(persistence = store, tables = repo, scope = CoroutineScope(Dispatchers.Unconfined))
        assertEquals("recorded tables must reach the state holder before it is used", expected, vm.uiState.value.availableTables.size)
        return vm
    }

    /**
     * The readings a case needs before a recorded table can answer it: the case's material or system
     * and inlet pressure class must be the ones the table declares, and its gas specific gravity must
     * be the table's (D7 compares those three and nothing else).
     */
    private fun enterTableCase(
        vm: HomeViewModel,
        tableId: String,
        size: String? = "synthetic-size-a",
        length: String = "1",
        method: SizingMethod = SizingMethod.RETURN_FROM_USER_TABLE,
    ): HomeViewModel {
        vm.onEvent(HomeEvent.MethodChanged(method))
        field(vm, HomeField.SELECTED_TABLE, tableId)
        if (size != null) field(vm, HomeField.TABLE_NOMINAL_SIZE, size)
        field(vm, HomeField.MATERIAL, "synthetic system")
        field(vm, HomeField.PRESSURE, "synthetic class")
        field(vm, HomeField.LOAD, "1")
        field(vm, HomeField.LENGTH, length)
        field(vm, HomeField.GAS_SPECIFIC_GRAVITY, "21.5")
        return vm
    }

    /**
     * Invented synthetic readings. The app supplies no table content, so every value here is
     * arbitrary filler chosen to be obviously not a code reading ("synthetic-size-a", 1.0, 2.0);
     * the declared gas specific gravity of 21.5 is no gas's, and is read from no code or standard.
     */
    private fun syntheticTable(
        id: String = "table-1",
        rows: List<TableRow> = listOf(TableRow("synthetic-size-a", null, 1.0, 2.0)),
    ): UserCapacityTable = UserCapacityTable(
        id = id,
        name = "synthetic table",
        applicability = TableApplicability(
            fuel = "synthetic fuel",
            materialOrSystem = "synthetic system",
            inletPressureClass = "synthetic class",
            permittedPressureDrop = "synthetic drop",
            gasSpecificGravity = "21.5",
        ),
        citation = TableCitation(codeAndEdition = "synthetic edition", tableIdentifier = "synthetic identifier"),
        marker = USER_RECORDED_MARKER,
        rows = rows,
    )

    private companion object {
        const val TOLERANCE = 0.000001
    }
}

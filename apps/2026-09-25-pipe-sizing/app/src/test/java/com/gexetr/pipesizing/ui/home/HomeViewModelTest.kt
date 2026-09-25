package com.gexetr.pipesizing.ui.home

import com.gexetr.pipesizing.sizing.RefusalKind
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.UnitSystem
import com.gexetr.pipesizing.sizing.Units
import com.gexetr.pipesizing.sizing.UserProvenance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        field(vm, HomeField.GAS_SPECIFIC_GRAVITY, "0.62")
        field(vm, HomeField.PRESSURE_DROP, "0.3")
        field(vm, HomeField.PIPE_DIAMETER, "1.049")

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

    private companion object {
        const val TOLERANCE = 0.000001
    }
}

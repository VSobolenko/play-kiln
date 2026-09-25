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
    fun unitToggleAloneChangesNeitherResultNorProvenance() {
        val vm = completeVm()

        val before = evaluate(vm) as SizingEvaluation.Computed
        vm.onEvent(HomeEvent.UnitChanged(UnitSystem.METRIC))
        vm.onEvent(HomeEvent.UnitChanged(UnitSystem.IMPERIAL))
        val after = evaluate(vm) as SizingEvaluation.Computed

        assertEquals(before, after)
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

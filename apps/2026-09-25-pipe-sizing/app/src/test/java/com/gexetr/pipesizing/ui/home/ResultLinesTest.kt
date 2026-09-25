package com.gexetr.pipesizing.ui.home

import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultLinesTest {

    @Test
    fun aResultCarriesTheCaseItEvaluatedBeforeAnyFigure() {
        val (state, result) = computedCase()

        val lines = computedResultLines(state, result, metric = false, methodLabel = "Capacity comparison")

        // The context lines come first, so a figure is never the first thing read.
        assertEquals(R.string.result_case_label, lines[0].labelRes)
        assertEquals(listOf<Any>("steel", "low"), lines[0].args)
        assertEquals(R.string.result_method_label, lines[1].labelRes)
        assertEquals(listOf<Any>("Capacity comparison"), lines[1].args)
        assertEquals(R.string.result_capacity_imperial, lines[2].labelRes)
        assertEquals(listOf<Any>("120.00"), lines[2].args)
        assertTrue(lines.any { it.labelRes == R.string.result_load_imperial })
        assertTrue(lines.any { it.labelRes == R.string.provenance_result })
    }

    @Test
    fun thePhysicalMethodResultCarriesItsOwnInputs() {
        val (state, result) = computedCase(method = SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS)

        val lines = computedResultLines(state, result, metric = false, methodLabel = "Capacity + physical inputs")

        val physical = lines.single { it.labelRes == R.string.result_physical_inputs }
        assertEquals(listOf<Any>("0.62", "0.3", "1.049"), physical.args)
    }

    @Test
    fun thePhysicalMethodInputsAreNotShownForTheOtherMethod() {
        val (state, result) = computedCase()

        val lines = computedResultLines(state, result, metric = false, methodLabel = "Capacity comparison")

        assertTrue(lines.none { it.labelRes == R.string.result_physical_inputs })
    }

    @Test
    fun aMetricResultReportsConvertedFiguresNotRelabelledOnes() {
        val (state, result) = computedCase()

        val lines = computedResultLines(state, result, metric = true, methodLabel = "Capacity comparison")

        // 100 MBH is 29.31 kW and 45 ft is 13.72 m. A relabelled imperial figure would read 100.00.
        assertEquals(listOf<Any>("29.31"), lines.single { it.labelRes == R.string.result_load_metric }.args)
        assertEquals(listOf<Any>("13.72"), lines.single { it.labelRes == R.string.result_length_metric }.args)
        assertTrue(lines.none { it.labelRes == R.string.result_load_imperial })
        assertTrue(lines.none { it.labelRes == R.string.result_length_imperial })
    }

    private fun computedCase(
        method: SizingMethod = SizingMethod.CAPACITY_COMPARISON,
    ): Pair<HomeUiState, SizingEvaluation.Computed> {
        val vm = HomeViewModel(persistence = CasePersistence.None, scope = CoroutineScope(Dispatchers.Unconfined))
        vm.onEvent(HomeEvent.MethodChanged(method))
        listOf(
            HomeField.MATERIAL to "steel",
            HomeField.PRESSURE to "low",
            HomeField.LOAD to "100",
            HomeField.LENGTH to "40",
            HomeField.FITTING_QUANTITY to "1",
            HomeField.FITTING_LENGTH to "5",
            HomeField.CAPACITY to "120",
        ).forEach { (field, value) -> vm.onEvent(HomeEvent.FieldChanged(field, value)) }
        vm.onEvent(HomeEvent.ConfirmCapacity)
        if (method == SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS) {
            listOf(
                HomeField.GAS_SPECIFIC_GRAVITY to "0.62",
                HomeField.PRESSURE_DROP to "0.3",
                HomeField.PIPE_DIAMETER to "1.049",
            ).forEach { (field, value) -> vm.onEvent(HomeEvent.FieldChanged(field, value)) }
        }
        vm.onEvent(HomeEvent.Evaluate)

        val state = vm.uiState.value
        return state to (state.evaluation as SizingEvaluation.Computed)
    }
}

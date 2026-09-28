package com.gexetr.pipesizing.ui.home

import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UnitSystem
import com.gexetr.pipesizing.sizing.UserCapacityTable
import com.gexetr.pipesizing.ui.tables.TablesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        // The args are the literal strings the user typed, so they move in lockstep with the fixture
        // in `computedCase`. The readings are invented synthetics (21.5, 35.5, 88.5): no gas has
        // that specific gravity, no allowance that pressure drop, no pipe that inside diameter.
        assertEquals(listOf<Any>("21.5", "35.5", "88.5"), physical.args)
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

    // -------------------------------------------------------------------------------------------
    // Step 4.3 — a value returned from the user's own recorded table gets its own lines: the row it
    // came from, the citation, the substitution when one happened, and the statement that the app
    // neither transcribed nor verified it. It never borrows the arithmetic comparison's wording,
    // because the app compared nothing here (design D10).
    // -------------------------------------------------------------------------------------------

    @Test
    fun aReturnedValueCarriesItsCitationRowSubstitutionAndSourceLabel() {
        val (state, result) = returnedCase(length = "2")

        val lines = returnedTableResultLines(state, result, metric = false, methodLabel = RETURN_LABEL, tableLabel = TABLE_LABEL)

        assertEquals(
            listOf<Any>(TABLE_LABEL, "synthetic edition", "synthetic identifier"),
            lines.single { it.labelRes == R.string.returned_table_citation }.args,
        )
        // The row actually used, named by its recorded size, is on screen.
        assertEquals(
            listOf<Any>("synthetic-size-a", "synthetic-size-a"),
            lines.single { it.labelRes == R.string.returned_table_row }.args,
        )
        // Both halves of the substitution, because a capacity for a length other than the user's own
        // must never be shown as though it were for their length (D3 step 7).
        assertEquals(
            listOf<Any>("2.00", "3.00"),
            lines.single { it.labelRes == R.string.returned_table_length_substitution_imperial }.args,
        )
        assertEquals(
            listOf<Any>(USER_RECORDED_MARKER),
            lines.single { it.labelRes == R.string.returned_table_user_recorded }.args,
        )
        assertTrue(lines.any { it.labelRes == R.string.returned_table_unverified })
    }

    @Test
    fun noSubstitutionMeansNoSubstitutionLines() {
        val (state, result) = returnedCase(length = "1")

        val lines = returnedTableResultLines(state, result, metric = false, methodLabel = RETURN_LABEL, tableLabel = TABLE_LABEL)

        // The recorded length was the case's own, so there is nothing to substitute and the absence
        // of the line is itself the record of that.
        assertNull(result.substitution)
        assertTrue(lines.none { it.labelRes == R.string.returned_table_length_substitution_imperial })
        assertTrue(lines.none { it.labelRes == R.string.returned_table_length_substitution_metric })
    }

    @Test
    fun aReturnedValueNeverUsesTheArithmeticComparisonWording() {
        val (state, result) = returnedCase(length = "2")

        val lines = returnedTableResultLines(state, result, metric = false, methodLabel = RETURN_LABEL, tableLabel = TABLE_LABEL)

        // Every line that would say the app compared the returned capacity against the load, or that
        // treats the value as a code reading, must be absent: the app read the user's row back and
        // nothing else.
        listOf(
            R.string.result_capacity_imperial,
            R.string.result_capacity_metric,
            R.string.capacity_supports,
            R.string.capacity_does_not_support,
            R.string.provenance_result,
        ).forEach { forbidden ->
            assertTrue(
                "a returned value must not be reported with resource $forbidden",
                lines.none { it.labelRes == forbidden },
            )
        }
    }

    @Test
    fun theFirstLineOfAReturnedResultStillNamesTheCase() {
        val (state, result) = returnedCase(length = "2")

        val lines = returnedTableResultLines(state, result, metric = false, methodLabel = RETURN_LABEL, tableLabel = TABLE_LABEL)

        // Context first: a figure is never the first thing read, exactly as for a computed result.
        assertEquals(R.string.result_case_label, lines[0].labelRes)
        assertEquals(listOf<Any>("synthetic system", "synthetic class"), lines[0].args)
        assertEquals(R.string.result_method_label, lines[1].labelRes)
        assertEquals(listOf<Any>(RETURN_LABEL), lines[1].args)
    }

    @Test
    fun aMetricReturnedValueReportsConvertedFiguresNotRelabelledOnes() {
        // The fields are entered in the selected unit, so this is the metric reading of the same
        // case: 1 MBH is 0.2930710701722222 kW and 2 ft is 0.6096 m.
        val (state, result) = returnedCase(metric = true, load = "0.2930710701722222", length = "0.6096")

        val lines = returnedTableResultLines(state, result, metric = true, methodLabel = RETURN_LABEL, tableLabel = TABLE_LABEL)

        // 1 MBH is 0.29 kW and 2 ft is 0.61 m. A relabelled imperial figure would read 1.00 and 2.00.
        assertEquals(
            listOf<Any>("0.29"),
            lines.single { it.labelRes == R.string.returned_table_capacity_metric }.args,
        )
        assertEquals(
            listOf<Any>("0.61", "0.91"),
            lines.single { it.labelRes == R.string.returned_table_length_substitution_metric }.args,
        )
        assertTrue(lines.none { it.labelRes == R.string.returned_table_capacity_imperial })
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
            // Invented synthetics, not code or standard readings (no gas/allowance/pipe reads this way).
            listOf(
                HomeField.GAS_SPECIFIC_GRAVITY to "21.5",
                HomeField.PRESSURE_DROP to "35.5",
                HomeField.PIPE_DIAMETER to "88.5",
            ).forEach { (field, value) -> vm.onEvent(HomeEvent.FieldChanged(field, value)) }
        }
        vm.onEvent(HomeEvent.Evaluate)

        val state = vm.uiState.value
        return state to (state.evaluation as SizingEvaluation.Computed)
    }

    private fun returnedCase(
        metric: Boolean = false,
        load: String = "1",
        length: String = "1",
    ): Pair<HomeUiState, SizingEvaluation.ReturnedFromUserTable> {
        val table = syntheticTable()
        val vm = HomeViewModel(
            persistence = CasePersistence.None,
            tables = TablesRepository.InMemory(listOf(table)),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        vm.onEvent(HomeEvent.UnitChanged(if (metric) UnitSystem.METRIC else UnitSystem.IMPERIAL))
        vm.onEvent(HomeEvent.MethodChanged(SizingMethod.RETURN_FROM_USER_TABLE))
        listOf(
            HomeField.SELECTED_TABLE to table.id,
            HomeField.TABLE_NOMINAL_SIZE to "synthetic-size-a",
            HomeField.MATERIAL to "synthetic system",
            HomeField.PRESSURE to "synthetic class",
            HomeField.LOAD to load,
            HomeField.LENGTH to length,
            HomeField.GAS_SPECIFIC_GRAVITY to "21.5",
        ).forEach { (field, value) -> vm.onEvent(HomeEvent.FieldChanged(field, value)) }
        vm.onEvent(HomeEvent.Evaluate)

        val state = vm.uiState.value
        return state to (state.evaluation as SizingEvaluation.ReturnedFromUserTable)
    }

    /**
     * Invented synthetic readings, and one row recorded at a length the case does not use, so the
     * substitution path is the one under test. The app supplies no table content. The declared gas
     * specific gravity of 21.5 is no gas's and is read from no code or standard.
     */
    private fun syntheticTable(): UserCapacityTable = UserCapacityTable(
        id = "table-1",
        name = TABLE_LABEL,
        applicability = TableApplicability(
            fuel = "synthetic fuel",
            materialOrSystem = "synthetic system",
            inletPressureClass = "synthetic class",
            permittedPressureDrop = "synthetic drop",
            gasSpecificGravity = "21.5",
        ),
        citation = TableCitation(codeAndEdition = "synthetic edition", tableIdentifier = "synthetic identifier"),
        marker = USER_RECORDED_MARKER,
        rows = listOf(
            TableRow("synthetic-size-a", null, 1.0, 2.0),
            TableRow("synthetic-size-a", null, 3.0, 1.0),
        ),
    )

    private companion object {
        const val RETURN_LABEL = "Return the capacity from my recorded table"
        const val TABLE_LABEL = "synthetic table"
    }
}

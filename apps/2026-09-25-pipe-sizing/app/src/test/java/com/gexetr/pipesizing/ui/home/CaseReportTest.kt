package com.gexetr.pipesizing.ui.home

import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.RefusalKind
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UserCapacityTable
import com.gexetr.pipesizing.ui.tables.TablesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The case report is the app's only outgoing text, so what it carries is asserted rather than read
 * off a shared-chat screenshot.
 *
 * The rule this suite exists for: a report may name **one** row — the row the app used — and must
 * carry no other row of the user's table. Naming the row it used is showing the user their own
 * reading back; carrying the row inventory would make the app a redistribution channel for table
 * data, which is the one thing this route's licence basis cannot survive (design D11/D12).
 *
 * The rows below are invented synthetic readings ("synthetic-size-a", 4.0, 7.0). The app supplies no
 * table content, so there is nothing here a code book would recognise; the declared gas specific
 * gravity of 21.5 is no gas's and is read from no code or standard either.
 */
class CaseReportTest {

    @Test
    fun aReturnedValueReportCarriesTheCitationRowSubstitutionAndDisclaimer() {
        val lines = returnedReport()

        // The row the lookup actually used, named by its recorded size.
        assertEquals(
            listOf<Any>("synthetic-size-b", "synthetic-size-b"),
            textLine(lines, R.string.returned_table_row).args,
        )
        // The citation, with the name the user gave the table, so the figure can be traced back.
        assertEquals(
            listOf<Any>(TABLE_LABEL, "synthetic edition", "synthetic identifier"),
            textLine(lines, R.string.returned_table_citation).args,
        )
        // Both halves of the substitution: the length entered and the recorded length the capacity
        // actually belongs to (D3 step 7).
        assertEquals(
            listOf<Any>("2.00", "5.00"),
            textLine(lines, R.string.returned_table_length_substitution_imperial).args,
        )
        // The statement that the value is the user's own recording and that the app verified nothing.
        assertEquals(
            listOf<Any>(USER_RECORDED_MARKER),
            textLine(lines, R.string.returned_table_user_recorded).args,
        )
        assertTrue(lineResources(lines).contains(R.string.returned_table_unverified))
        // And the calculation-aid disclaimer every result carries, returned or computed.
        assertTrue(lineResources(lines).contains(R.string.disclaimer))
    }

    @Test
    fun aReturnedValueReportCarriesNoOtherRowOfTheTable() {
        val lines = returnedReport()

        // The table holds two rows. The report names the one used (recorded at 5.0 ft, 9.0 MBH) and
        // must carry nothing of the other (4.0 ft, 7.0 MBH) — not its length, not its capacity.
        val numbers = numericArguments(lines)
        assertTrue("the used row's capacity must be in the report", numbers.contains(9.0))
        assertFalse("the report must not carry the unused row's length", numbers.contains(4.0))
        assertFalse("the report must not carry the unused row's capacity", numbers.contains(7.0))
        // The row list is not carried as labels either: exactly one row line is present.
        assertEquals(1, lineResources(lines).count { it == R.string.returned_table_row })
    }

    @Test
    fun aReturnedValueReportNamesNoRowOtherThanTheOneUsed() {
        val lines = returnedReport()

        // Every argument the report carries, as text: nothing belonging to the other recorded row
        // (a different size, recorded at 4.0 ft with a 7.0 MBH capacity) may appear anywhere in it.
        val strings = stringArguments(lines)
        assertFalse("the report must not name the other row's size", strings.contains("synthetic-size-a"))
        assertFalse("the report must not name the other row's length", strings.contains("4.0"))
        assertFalse("the report must not name the other row's capacity", strings.contains("7.0"))
    }

    @Test
    fun aComputedReportKeepsItsOwnLinesAndDisclaimer() {
        val vm = HomeViewModel(persistence = CasePersistence.None, scope = CoroutineScope(Dispatchers.Unconfined))
        listOf(
            HomeField.MATERIAL to "steel",
            HomeField.PRESSURE to "low",
            HomeField.LOAD to "100",
            HomeField.LENGTH to "40",
            HomeField.CAPACITY to "120",
        ).forEach { (field, value) -> vm.onEvent(HomeEvent.FieldChanged(field, value)) }
        vm.onEvent(HomeEvent.ConfirmCapacity)
        vm.onEvent(HomeEvent.Evaluate)
        val state = vm.uiState.value
        val computed = state.evaluation as SizingEvaluation.Computed

        val lines = computedReportLines(state, computed, isPro = false)

        // The arithmetic report is unchanged in shape by this change: it still opens with the app,
        // still states the comparison verdict and still carries the disclaimer.
        assertEquals(R.string.app_name, (lines[0] as ReportLine.Text).stringRes)
        val resources = lineResources(lines)
        assertTrue(resources.contains(R.string.capacity_supports))
        assertTrue(resources.contains(R.string.provenance_result))
        assertTrue(resources.contains(R.string.disclaimer))
        assertFalse(resources.contains(R.string.returned_table_row))
    }

    // -------------------------------------------------------------------------------------------
    // Step 5.1 — the report action is reachable for a value returned from the user's recorded table.
    // The action used to be gated on `SizingEvaluation.Computed`, so 4.6's returned-table report was
    // implemented and tested but no screen could reach it. These assert the offer and the choice
    // through the same two functions the screen and the composition root call.
    // -------------------------------------------------------------------------------------------

    @Test
    fun aReturnedValueOffersAReportAndTheReturnedTableReportIsTheOneChosen() {
        val vm = returnedVm()
        val state = vm.uiState.value
        val returned = state.evaluation as SizingEvaluation.ReturnedFromUserTable

        assertTrue("a returned value must offer a report", returned.hasShareableReport())

        val lines = reportLinesFor(state, returned, isPro = false, unnamedTableLabel = UNNAMED_TABLE)
        assertNotNull(lines)
        // The returned-from-table report is the one selected: its own title and its row line, and
        // not the arithmetic report's comparison verdict.
        val resources = lineResources(lines!!)
        assertTrue(resources.contains(R.string.returned_table_result_title))
        assertTrue(resources.contains(R.string.returned_table_row))
        assertFalse("the arithmetic report is not the one chosen", resources.contains(R.string.result_title))
        assertFalse(resources.contains(R.string.provenance_result))
    }

    @Test
    fun aComputedValueStillGetsTheArithmeticReport() {
        val vm = HomeViewModel(persistence = CasePersistence.None, scope = CoroutineScope(Dispatchers.Unconfined))
        listOf(
            HomeField.MATERIAL to "steel",
            HomeField.PRESSURE to "low",
            HomeField.LOAD to "100",
            HomeField.LENGTH to "40",
            HomeField.CAPACITY to "120",
        ).forEach { (field, value) -> vm.onEvent(HomeEvent.FieldChanged(field, value)) }
        vm.onEvent(HomeEvent.ConfirmCapacity)
        vm.onEvent(HomeEvent.Evaluate)
        val state = vm.uiState.value
        val computed = state.evaluation as SizingEvaluation.Computed

        assertTrue(computed.hasShareableReport())

        val resources = lineResources(
            reportLinesFor(state, computed, isPro = false, unnamedTableLabel = UNNAMED_TABLE)!!,
        )
        assertTrue(resources.contains(R.string.result_title))
        assertFalse(resources.contains(R.string.returned_table_row))
    }

    @Test
    fun aRefusalIsOfferedNoReport() {
        val state = HomeUiState()
        val refused = SizingEvaluation.Refused(RefusalKind.MISSING_INPUT, "Enter something.")

        assertFalse("no result is no report", state.evaluation.hasShareableReport())
        assertFalse(refused.hasShareableReport())
        assertNull(reportLinesFor(state, state.evaluation, isPro = true, unnamedTableLabel = UNNAMED_TABLE))
        assertNull(reportLinesFor(state, refused, isPro = true, unnamedTableLabel = UNNAMED_TABLE))
    }

    private fun returnedReport(): List<ReportLine> {
        val state = returnedVm().uiState.value
        val returned = state.evaluation as SizingEvaluation.ReturnedFromUserTable

        return returnedTableReportLines(state, returned, isPro = false, tableLabel = TABLE_LABEL)
    }

    private fun returnedVm(): HomeViewModel {
        val vm = HomeViewModel(
            persistence = CasePersistence.None,
            tables = TablesRepository.InMemory(listOf(reportTable())),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        vm.onEvent(HomeEvent.MethodChanged(SizingMethod.RETURN_FROM_USER_TABLE))
        listOf(
            HomeField.SELECTED_TABLE to TABLE_ID,
            HomeField.TABLE_NOMINAL_SIZE to "synthetic-size-b",
            HomeField.MATERIAL to "synthetic system",
            HomeField.PRESSURE to "synthetic class",
            HomeField.LOAD to "1",
            HomeField.LENGTH to "2",
            HomeField.GAS_SPECIFIC_GRAVITY to "21.5",
        ).forEach { (field, value) -> vm.onEvent(HomeEvent.FieldChanged(field, value)) }
        vm.onEvent(HomeEvent.Evaluate)
        return vm
    }

    private fun textLine(lines: List<ReportLine>, stringRes: Int): ReportLine.Text =
        lines.filterIsInstance<ReportLine.Text>().single { it.stringRes == stringRes }

    /** The string resource behind each line, in line order. */
    private fun lineResources(lines: List<ReportLine>): List<Int> = lines.mapNotNull { line ->
        when (line) {
            ReportLine.Blank -> null
            is ReportLine.Text -> line.stringRes
            is ReportLine.OfResource -> line.stringRes
            is ReportLine.Labelled -> line.labelRes
        }
    }

    /** Every argument the report carries, as text, in line order. */
    private fun stringArguments(lines: List<ReportLine>): List<String> = lines.flatMap { line ->
        when (line) {
            ReportLine.Blank -> emptyList()
            is ReportLine.Text -> line.args.map { it.toString() }
            is ReportLine.Labelled -> listOf(line.value)
            is ReportLine.OfResource -> emptyList()
        }
    }

    private fun numericArguments(lines: List<ReportLine>): List<Double> =
        stringArguments(lines).mapNotNull { it.toDoubleOrNull() }

    /**
     * Two invented rows, each for its own size, so the report has a row it must not leak. The case's
     * own length of 2.0 ft is recorded nowhere, so the larger row of the chosen size is used and both
     * halves of the substitution are reportable.
     */
    private fun reportTable(): UserCapacityTable = UserCapacityTable(
        id = TABLE_ID,
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
            TableRow("synthetic-size-a", null, 4.0, 7.0),
            TableRow("synthetic-size-b", null, 5.0, 9.0),
        ),
    )

    private companion object {
        const val TABLE_ID = "table-1"
        const val TABLE_LABEL = "synthetic table"
        const val UNNAMED_TABLE = "unnamed synthetic table"
    }
}

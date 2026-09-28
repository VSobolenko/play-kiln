package com.gexetr.pipesizing.ui.tables

import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UserCapacityTable
import com.gexetr.pipesizing.sizing.derivedSizeOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every reading below is deliberate synthetic filler ("synthetic-size-a", 1.0, 2.0). None of it is
 * code-table data; the app owns none, and these tests only exercise what the user typed.
 */
class TablesViewModelTest {

    @Test
    fun aNewTableHasNoRowAndNoPrefilledCell() {
        val viewModel = viewModel()
        viewModel.onEvent(TablesEvent.StartNew)

        val draft = requireNotNull(viewModel.uiState.value.editing)

        // The app supplies no table content: every cell is empty and no row exists until one is added.
        assertEquals("", draft.name)
        assertEquals("", draft.fuel)
        assertEquals("", draft.materialOrSystem)
        assertEquals("", draft.inletPressureClass)
        assertEquals("", draft.permittedPressureDrop)
        assertEquals("", draft.gasSpecificGravity)
        assertEquals("", draft.codeAndEdition)
        assertEquals("", draft.tableIdentifier)
        assertTrue(draft.rows.isEmpty())
        assertTrue(viewModel.uiState.value.derivedSizeOrder.isEmpty())
        assertFalse(viewModel.uiState.value.editingIsUsable)
    }

    @Test
    fun aTableWithAnIncompleteHeaderIsNotUsableEvenWithACompleteRow() {
        val viewModel = viewModel()
        viewModel.onEvent(TablesEvent.StartNew)
        viewModel.onEvent(TablesEvent.AddRow)
        completeRow(viewModel, index = 0, size = "synthetic-size-a", length = "1.0", capacity = "2.0")

        // The row is complete and parses, but the applicability and the citation are missing.
        assertFalse(viewModel.uiState.value.editingIsUsable)
        assertTrue(requireNotNull(viewModel.uiState.value.editingProblem).isNotBlank())

        fillHeader(viewModel)

        assertTrue(viewModel.uiState.value.editingIsUsable)
        assertNull(viewModel.uiState.value.editingProblem)
    }

    @Test
    fun aRowWhoseCapacityIsNotANumberIsNotUsable() {
        val viewModel = viewModel()
        viewModel.onEvent(TablesEvent.StartNew)
        viewModel.onEvent(TablesEvent.AddRow)
        fillHeader(viewModel)
        completeRow(viewModel, index = 0, size = "synthetic-size-a", length = "1.0", capacity = "two")

        // Text that does not parse is refused by the domain's own check, not silently treated as
        // absent or as zero.
        assertFalse(viewModel.uiState.value.editingIsUsable)
    }

    @Test
    fun aRowEditIsReflectedInTheDraft() {
        val viewModel = viewModel()
        viewModel.onEvent(TablesEvent.StartNew)
        viewModel.onEvent(TablesEvent.AddRow)
        viewModel.onEvent(TablesEvent.RowChanged(0, TableRowField.NOMINAL_SIZE, "synthetic-size-a"))

        assertEquals("synthetic-size-a", requireNotNull(viewModel.uiState.value.editing).rows.single().nominalSize)
    }

    @Test
    fun editingOneRowLeavesTheOtherRowAlone() {
        val viewModel = viewModel()
        viewModel.onEvent(TablesEvent.StartNew)
        viewModel.onEvent(TablesEvent.AddRow)
        viewModel.onEvent(TablesEvent.AddRow)
        completeRow(viewModel, index = 0, size = "synthetic-size-a", length = "1.0", capacity = "2.0")
        completeRow(viewModel, index = 1, size = "synthetic-size-b", length = "2.0", capacity = "1.0")

        viewModel.onEvent(TablesEvent.RowChanged(1, TableRowField.LENGTH_FEET, "3.0"))

        val rows = requireNotNull(viewModel.uiState.value.editing).rows
        assertEquals("1.0", rows[0].lengthFeet)
        assertEquals("3.0", rows[1].lengthFeet)
        assertEquals("synthetic-size-a", rows[0].nominalSize)
    }

    @Test
    fun removingARowLeavesTheRestInTheirRecordedOrder() {
        val viewModel = viewModel()
        viewModel.onEvent(TablesEvent.StartNew)
        repeat(3) { viewModel.onEvent(TablesEvent.AddRow) }
        viewModel.onEvent(TablesEvent.RowChanged(0, TableRowField.NOMINAL_SIZE, "synthetic-size-a"))
        viewModel.onEvent(TablesEvent.RowChanged(1, TableRowField.NOMINAL_SIZE, "synthetic-size-b"))
        viewModel.onEvent(TablesEvent.RowChanged(2, TableRowField.NOMINAL_SIZE, "synthetic-size-c"))

        viewModel.onEvent(TablesEvent.RemoveRow(1))

        assertEquals(
            listOf("synthetic-size-a", "synthetic-size-c"),
            viewModel.uiState.value.derivedSizeOrder,
        )
    }

    @Test
    fun theDerivedSizeOrderIsTheOrderTheUserRecorded() {
        val viewModel = viewModel()
        viewModel.onEvent(TablesEvent.StartNew)
        viewModel.onEvent(TablesEvent.AddRow)
        viewModel.onEvent(TablesEvent.RowChanged(0, TableRowField.NOMINAL_SIZE, "zeta"))
        viewModel.onEvent(TablesEvent.AddRow)
        viewModel.onEvent(TablesEvent.RowChanged(1, TableRowField.NOMINAL_SIZE, "alpha"))

        // Deliberately not ascending: the order is the user's own transcription, never a ladder the
        // app holds, so an app-supplied ordering would fail this.
        assertEquals(listOf("zeta", "alpha"), viewModel.uiState.value.derivedSizeOrder)
    }

    @Test
    fun theShownOrderAgreesWithTheDomainOrderOnceTheDraftIsComplete() {
        val viewModel = viewModel()
        startCompleteTable(viewModel)

        val draft = requireNotNull(viewModel.uiState.value.editing)
        val table = requireNotNull(draft.toTableOrNull())

        assertTrue(viewModel.uiState.value.editingIsUsable)
        // The draft computes the order from text and the entity computes it from rows; this pins the
        // two to the same rule so they cannot drift.
        assertEquals(table.derivedSizeOrder(), draft.derivedSizeOrder)
    }

    @Test
    fun anUnusableDraftIsNeverWrittenToTheStore() {
        val repository = TablesRepository.InMemory()
        val viewModel = viewModel(repository)
        viewModel.onEvent(TablesEvent.StartNew)
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.NAME, "synthetic table"))
        viewModel.onEvent(TablesEvent.AddRow)
        completeRow(viewModel, index = 0, size = "synthetic-size-a", length = "1.0", capacity = "2.0")

        viewModel.onEvent(TablesEvent.SaveEditing)

        assertTrue(repository.readTables().isEmpty())
        assertTrue("the editor stays open so the unfinished table is not lost", viewModel.uiState.value.editing != null)
    }

    @Test
    fun aCompleteTableIsStoredWithTheSharedMarker() {
        val repository = TablesRepository.InMemory()
        val viewModel = viewModel(repository)
        startCompleteTable(viewModel)

        viewModel.onEvent(TablesEvent.SaveEditing)

        val stored = repository.readTables().single()
        assertEquals("synthetic table", stored.name)
        // Both recorded rows reach the store, in the order they were recorded.
        assertEquals(listOf("zeta", "alpha"), stored.rows.map { it.nominalSize })
        assertEquals(listOf(1.0, 2.0), stored.rows.map { it.lengthFeet })
        assertEquals(listOf(2.0, 1.0), stored.rows.map { it.capacityMbh })
        // The marker is the shared constant, so the result view's label cannot drift from it.
        assertEquals(USER_RECORDED_MARKER, stored.marker)
        assertNull(viewModel.uiState.value.editing)
        assertEquals(listOf(stored), viewModel.uiState.value.tables)
    }

    @Test
    fun openingARecordedTableRestoresEveryCellAndRow() {
        val repository = TablesRepository.InMemory(listOf(syntheticTable()))
        val viewModel = viewModel(repository)

        viewModel.onEvent(TablesEvent.Open("table-1"))

        val draft = requireNotNull(viewModel.uiState.value.editing)
        assertEquals("synthetic table", draft.name)
        assertEquals("synthetic fuel", draft.fuel)
        assertEquals("synthetic edition", draft.codeAndEdition)
        assertEquals("synthetic-size-a", draft.rows.single().nominalSize)
        assertEquals("1.0", draft.rows.single().lengthFeet)
        assertEquals("2.0", draft.rows.single().capacityMbh)
        assertTrue(viewModel.uiState.value.editingIsUsable)
    }

    @Test
    fun deletingATableRemovesItAndReopeningDoesNotBringItBack() {
        val repository = TablesRepository.InMemory()
        val viewModel = viewModel(repository)
        startCompleteTable(viewModel)
        viewModel.onEvent(TablesEvent.SaveEditing)
        val id = viewModel.uiState.value.tables.single().id

        viewModel.onEvent(TablesEvent.DeleteTable(id))

        assertTrue(viewModel.uiState.value.tables.isEmpty())
        assertTrue(repository.readTables().isEmpty())
        // A second state holder over the same store stands in for reopening the screen.
        assertTrue(viewModel(repository).uiState.value.tables.isEmpty())
    }

    @Test
    fun discardingAnEditingDraftLeavesTheStoredTablesAlone() {
        val repository = TablesRepository.InMemory(listOf(syntheticTable()))
        val viewModel = viewModel(repository)
        viewModel.onEvent(TablesEvent.Open("table-1"))
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.NAME, "changed but not saved"))

        viewModel.onEvent(TablesEvent.DiscardEditing)

        assertNull(viewModel.uiState.value.editing)
        assertEquals("synthetic table", repository.readTables().single().name)
    }

    private fun viewModel(repository: TablesRepository = TablesRepository.InMemory()) =
        TablesViewModel(repository, CoroutineScope(Dispatchers.Unconfined))

    private fun fillHeader(viewModel: TablesViewModel) {
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.NAME, "synthetic table"))
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.FUEL, "synthetic fuel"))
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.MATERIAL_OR_SYSTEM, "synthetic system"))
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.INLET_PRESSURE_CLASS, "synthetic class"))
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.PERMITTED_PRESSURE_DROP, "synthetic drop"))
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.GAS_SPECIFIC_GRAVITY, "synthetic gravity"))
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.CODE_AND_EDITION, "synthetic edition"))
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.TABLE_IDENTIFIER, "synthetic identifier"))
    }

    private fun completeRow(viewModel: TablesViewModel, index: Int, size: String, length: String, capacity: String) {
        viewModel.onEvent(TablesEvent.RowChanged(index, TableRowField.NOMINAL_SIZE, size))
        viewModel.onEvent(TablesEvent.RowChanged(index, TableRowField.LENGTH_FEET, length))
        viewModel.onEvent(TablesEvent.RowChanged(index, TableRowField.CAPACITY_MBH, capacity))
    }

    /** Two complete rows in a deliberately non-ascending order: zeta first, alpha second. */
    private fun startCompleteTable(viewModel: TablesViewModel) {
        viewModel.onEvent(TablesEvent.StartNew)
        fillHeader(viewModel)
        viewModel.onEvent(TablesEvent.AddRow)
        completeRow(viewModel, index = 0, size = "zeta", length = "1.0", capacity = "2.0")
        viewModel.onEvent(TablesEvent.AddRow)
        completeRow(viewModel, index = 1, size = "alpha", length = "2.0", capacity = "1.0")
    }

    private fun syntheticTable(id: String = "table-1") = UserCapacityTable(
        id = id,
        name = "synthetic table",
        applicability = TableApplicability(
            fuel = "synthetic fuel",
            materialOrSystem = "synthetic system",
            inletPressureClass = "synthetic class",
            permittedPressureDrop = "synthetic drop",
            gasSpecificGravity = "synthetic gravity",
        ),
        citation = TableCitation(codeAndEdition = "synthetic edition", tableIdentifier = "synthetic identifier"),
        marker = USER_RECORDED_MARKER,
        rows = listOf(TableRow("synthetic-size-a", null, 1.0, 2.0)),
    )

    private fun TablesRepository.readTables(): List<UserCapacityTable> = runBlocking { tables.first() }
}

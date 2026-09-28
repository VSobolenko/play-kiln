package com.gexetr.pipesizing.ui.tables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UserCapacityTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The two claims about the table surface that only a device can settle: the app supplies no table
 * content, and a deleted table stays deleted.
 *
 * Every reading below is deliberate synthetic filler ("synthetic table", "synthetic-size-a", 1.0,
 * 2.0). None of it is code-table data; the app owns none.
 */
class TablesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anEmptyTableShowsEveryFieldEmptyAndNoPrefilledRow() {
        val repository = TablesRepository.InMemory()
        val viewModel = viewModel(repository)
        composeRule.setContent { Surface(viewModel) }

        // Nothing recorded yet: the list says so and no table is open.
        composeRule.onNodeWithTag(TablesScreenTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(TablesScreenTags.EDITOR).assertDoesNotExist()

        composeRule.onNodeWithTag(TablesScreenTags.NEW).performClick()
        composeRule.waitForIdle()

        // The app supplies no table content: every header cell is empty and no row exists.
        TableHeaderField.entries.forEach(::assertHeaderFieldEmpty)
        composeRule.onNodeWithTag(TablesScreenTags.row(0)).assertDoesNotExist()
        // Present, not necessarily on screen: the row list scrolls and this button sits below the
        // fold on a small display.
        composeRule.onNodeWithTag(TablesScreenTags.ADD_ROW).assertExists()

        // Positive control for the reading above: once the draft holds text, the same reading sees it.
        // Without this, "empty" could be evidence of a blind sensor rather than of an empty field.
        viewModel.onEvent(TablesEvent.HeaderChanged(TableHeaderField.NAME, "synthetic table"))
        composeRule.waitForIdle()
        assertEquals("synthetic table", editableTextOf(TablesScreenTags.header(TableHeaderField.NAME)))
        TableHeaderField.entries
            .filterNot { it == TableHeaderField.NAME }
            .forEach(::assertHeaderFieldEmpty)
    }

    @Test
    fun deletingATableRemovesItAndReopeningDoesNotBringItBack() {
        val repository = TablesRepository.InMemory(listOf(syntheticTable()))
        val viewModel = viewModel(repository)
        composeRule.setContent { Surface(viewModel) }

        composeRule.onNodeWithTag(TablesScreenTags.tableCard("table-1")).assertIsDisplayed()

        composeRule.onNodeWithTag(TablesScreenTags.deleteButton("table-1")).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TablesScreenTags.tableCard("table-1")).assertDoesNotExist()
        assertTrue(runBlocking { repository.tables.first() }.isEmpty())
        // Reopening the screen is a fresh state holder over the same store: the table stays gone.
        assertTrue(viewModel(repository).uiState.value.tables.isEmpty())
    }

    @Composable
    private fun Surface(viewModel: TablesViewModel) {
        val state by viewModel.uiState.collectAsState()
        TablesScreen(state = state, onEvent = viewModel::onEvent)
    }

    private fun assertHeaderFieldEmpty(field: TableHeaderField) {
        val tag = TablesScreenTags.header(field)
        composeRule.onNodeWithTag(tag).assertExists()
        val text = editableTextOf(tag)
        assertTrue("${field.name} must start empty but showed '$text'", text.isEmpty())
    }

    private fun editableTextOf(tag: String): String =
        composeRule.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config
            .getOrElseNullable(SemanticsProperties.EditableText) { null }
            ?.text
            .orEmpty()

    private fun viewModel(repository: TablesRepository) =
        TablesViewModel(repository, CoroutineScope(Dispatchers.Unconfined))

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
}

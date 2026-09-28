package com.gexetr.pipesizing.ui.tables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UserCapacityTable

/**
 * Stable handles for the instrumentation suite. They name structure the test asserts (a field is
 * empty, a row exists, a table is gone), not decoration.
 */
object TablesScreenTags {
    const val RECORDED_LIST = "tables.recorded-list"
    const val EMPTY = "tables.empty"
    const val NEW = "tables.new"
    const val EDITOR = "tables.editor"
    const val MARKER = "tables.marker"
    const val ADD_ROW = "tables.add-row"
    const val DERIVED_ORDER = "tables.derived-order"
    const val SAVE = "tables.save"
    const val DISCARD = "tables.discard"

    fun header(field: TableHeaderField): String = "tables.header.${field.name}"

    fun row(index: Int): String = "tables.row.$index"

    fun rowField(index: Int, field: TableRowField): String = "tables.row.$index.${field.name}"

    fun tableCard(id: String): String = "tables.table.$id"

    fun openButton(id: String): String = "tables.table.$id.open"

    fun deleteButton(id: String): String = "tables.table.$id.delete"
}

/**
 * The table-entry surface.
 *
 * Content only, like every other screen: the app's single `Scaffold` owns the bars. Two rules are
 * visible here and must stay. First, the app supplies no table content — every cell starts empty and
 * no row exists until the user adds one. Second, nothing on this screen can move table data off the
 * device: there is no paste, import, export, share or file surface, because the route's whole licence
 * basis is that the app is not a redistributor of table data (design D12).
 */
@Composable
fun TablesScreen(
    state: TablesUiState,
    onEvent: (TablesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.tables_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.tables_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        RecordedTablesCard(state = state, onEvent = onEvent)

        val draft = state.editing
        if (draft == null) {
            OutlinedButton(
                onClick = { onEvent(TablesEvent.StartNew) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(TablesScreenTags.NEW),
            ) {
                Text(stringResource(R.string.tables_new))
            }
        } else {
            TableEditorCard(draft = draft, state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun RecordedTablesCard(state: TablesUiState, onEvent: (TablesEvent) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TablesScreenTags.RECORDED_LIST),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.tables_recorded_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.tables.isEmpty()) {
                Text(
                    text = stringResource(R.string.tables_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(TablesScreenTags.EMPTY),
                )
                Text(
                    text = stringResource(R.string.tables_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.tables.forEach { table ->
                    RecordedTableRow(table = table, onEvent = onEvent)
                }
            }

            Text(
                text = stringResource(R.string.tables_no_distribution_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordedTableRow(table: UserCapacityTable, onEvent: (TablesEvent) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TablesScreenTags.tableCard(table.id)),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = table.name.ifBlank { stringResource(R.string.tables_unnamed) },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.tables_citation_summary, table.citation.codeAndEdition, table.citation.tableIdentifier),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.tables_applicability_summary, table.applicability.summary()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.tables_row_count, table.rows.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onEvent(TablesEvent.Open(table.id)) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(TablesScreenTags.openButton(table.id)),
                ) {
                    Text(stringResource(R.string.tables_open))
                }
                TextButton(
                    onClick = { onEvent(TablesEvent.DeleteTable(table.id)) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(TablesScreenTags.deleteButton(table.id)),
                ) {
                    Text(stringResource(R.string.tables_delete))
                }
            }
        }
    }
}

@Composable
private fun TableEditorCard(draft: TableDraft, state: TablesUiState, onEvent: (TablesEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TablesScreenTags.EDITOR),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(stringResource(R.string.tables_header_heading)) {
            TableHeaderField.entries.forEach { field ->
                OutlinedTextField(
                    value = draft.headerValue(field),
                    onValueChange = { onEvent(TablesEvent.HeaderChanged(field, it)) },
                    label = { Text(stringResource(field.labelRes())) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TablesScreenTags.header(field)),
                )
            }
            Text(
                text = stringResource(R.string.tables_marker_note, USER_RECORDED_MARKER),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TablesScreenTags.MARKER),
            )
        }

        SectionCard(stringResource(R.string.tables_rows_heading)) {
            if (draft.rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.tables_rows_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            draft.rows.forEachIndexed { index, row ->
                RowEditor(index = index, row = row, onEvent = onEvent)
            }
            OutlinedButton(
                onClick = { onEvent(TablesEvent.AddRow) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(TablesScreenTags.ADD_ROW),
            ) {
                Text(stringResource(R.string.tables_add_row))
            }
        }

        DerivedOrderCard(order = state.derivedSizeOrder)

        if (!state.editingIsUsable) {
            Text(
                text = state.editingProblem ?: stringResource(R.string.tables_incomplete),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = { onEvent(TablesEvent.SaveEditing) },
            enabled = state.editingIsUsable,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag(TablesScreenTags.SAVE),
        ) {
            Text(stringResource(R.string.tables_save))
        }
        OutlinedButton(
            onClick = { onEvent(TablesEvent.DiscardEditing) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(TablesScreenTags.DISCARD),
        ) {
            Text(stringResource(R.string.tables_discard))
        }
    }
}

/**
 * The derived size order, shown read-only.
 *
 * The row-order rule (D4) makes a mis-ordered transcription produce a wrong "smallest carrying
 * size", so the order the app will actually use is visible while the table is still being typed.
 */
@Composable
private fun DerivedOrderCard(order: List<String>) {
    SectionCard(stringResource(R.string.tables_derived_order_heading)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TablesScreenTags.DERIVED_ORDER),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (order.isEmpty()) {
                Text(
                    text = stringResource(R.string.tables_derived_order_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                order.forEachIndexed { index, size ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.tables_derived_order_index, index + 1),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(28.dp),
                        )
                        Text(text = size, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.tables_derived_order_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowEditor(index: Int, row: TableRowDraft, onEvent: (TablesEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TablesScreenTags.row(index)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.tables_row_number, index + 1),
            style = MaterialTheme.typography.labelLarge,
        )
        OutlinedTextField(
            value = row.nominalSize,
            onValueChange = { onEvent(TablesEvent.RowChanged(index, TableRowField.NOMINAL_SIZE, it)) },
            label = { Text(stringResource(R.string.tables_row_size_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TablesScreenTags.rowField(index, TableRowField.NOMINAL_SIZE)),
        )
        OutlinedTextField(
            value = row.insideDiameter,
            onValueChange = { onEvent(TablesEvent.RowChanged(index, TableRowField.INSIDE_DIAMETER, it)) },
            label = { Text(stringResource(R.string.tables_row_diameter_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TablesScreenTags.rowField(index, TableRowField.INSIDE_DIAMETER)),
        )
        OutlinedTextField(
            value = row.lengthFeet,
            onValueChange = { onEvent(TablesEvent.RowChanged(index, TableRowField.LENGTH_FEET, it)) },
            label = { Text(stringResource(R.string.tables_row_length_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TablesScreenTags.rowField(index, TableRowField.LENGTH_FEET)),
        )
        OutlinedTextField(
            value = row.capacityMbh,
            onValueChange = { onEvent(TablesEvent.RowChanged(index, TableRowField.CAPACITY_MBH, it)) },
            label = { Text(stringResource(R.string.tables_row_capacity_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TablesScreenTags.rowField(index, TableRowField.CAPACITY_MBH)),
        )
        TextButton(
            onClick = { onEvent(TablesEvent.RemoveRow(index)) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.tables_remove_row))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

private fun TableDraft.headerValue(field: TableHeaderField): String = when (field) {
    TableHeaderField.NAME -> name
    TableHeaderField.FUEL -> fuel
    TableHeaderField.MATERIAL_OR_SYSTEM -> materialOrSystem
    TableHeaderField.INLET_PRESSURE_CLASS -> inletPressureClass
    TableHeaderField.PERMITTED_PRESSURE_DROP -> permittedPressureDrop
    TableHeaderField.GAS_SPECIFIC_GRAVITY -> gasSpecificGravity
    TableHeaderField.CODE_AND_EDITION -> codeAndEdition
    TableHeaderField.TABLE_IDENTIFIER -> tableIdentifier
}

private fun TableHeaderField.labelRes(): Int = when (this) {
    TableHeaderField.NAME -> R.string.tables_name_label
    TableHeaderField.FUEL -> R.string.tables_fuel_label
    TableHeaderField.MATERIAL_OR_SYSTEM -> R.string.tables_material_label
    TableHeaderField.INLET_PRESSURE_CLASS -> R.string.tables_pressure_class_label
    TableHeaderField.PERMITTED_PRESSURE_DROP -> R.string.tables_pressure_drop_label
    TableHeaderField.GAS_SPECIFIC_GRAVITY -> R.string.tables_gravity_label
    TableHeaderField.CODE_AND_EDITION -> R.string.tables_edition_label
    TableHeaderField.TABLE_IDENTIFIER -> R.string.tables_table_identifier_label
}

private fun TableApplicability.summary(): String =
    listOf(fuel, materialOrSystem, inletPressureClass, permittedPressureDrop, gasSpecificGravity)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

// ---------------------------------------------------------------------------------------------
// Previews. The values below are trivial placeholders (1.0, 2.0) and are labelled synthetic: the
// app owns no capacity, length or size data, and these exemplify a populated screen, nothing else.
// ---------------------------------------------------------------------------------------------

@Preview(name = "Empty table", showBackground = true)
@Composable
private fun EmptyTablePreview() {
    TablesScreen(state = TablesUiState(editing = TableDraft.new(id = "preview-empty")), onEvent = {})
}

@Preview(name = "Populated table", showBackground = true)
@Composable
private fun PopulatedTablePreview() {
    TablesScreen(state = TablesUiState(editing = syntheticPreviewDraft()), onEvent = {})
}

private fun syntheticPreviewDraft(): TableDraft = TableDraft(
    id = "preview-populated",
    name = "Synthetic preview",
    fuel = "synthetic gas",
    materialOrSystem = "synthetic system",
    inletPressureClass = "synthetic class",
    permittedPressureDrop = "synthetic drop",
    gasSpecificGravity = "synthetic gravity",
    codeAndEdition = "synthetic edition",
    tableIdentifier = "synthetic table",
    rows = listOf(
        TableRowDraft("synthetic-size-a", "", "1.0", "2.0"),
        TableRowDraft("synthetic-size-b", "", "1.0", "2.0"),
        TableRowDraft("synthetic-size-a", "", "2.0", "1.0"),
    ),
)

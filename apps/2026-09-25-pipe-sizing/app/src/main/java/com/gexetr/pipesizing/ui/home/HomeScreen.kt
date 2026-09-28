package com.gexetr.pipesizing.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.monetization.MonetizationConfig
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UnitSystem
import com.gexetr.pipesizing.sizing.UserCapacityTable
import com.gexetr.pipesizing.sizing.returnsFromUserTable

/**
 * Stable handles for the instrumentation suite. They name structure a test asserts - the method
 * choices, the table picker, the result card's own disclaimer - not decoration.
 */
object HomeScreenTags {
    const val METHOD_LIST = "home.method-list"
    const val TABLE_PICKER = "home.table-picker"
    const val TABLE_EMPTY = "home.table-empty"
    const val TABLE_NOMINAL_SIZE = "home.table-nominal-size"
    const val RESULT_CARD = "home.result-card"
    const val RESULT_DISCLAIMER = "home.result-disclaimer"
    const val REPORT_ACTION = "home.report-action"

    fun method(method: SizingMethod): String = "home.method.${method.name}"

    fun tableOption(id: String): String = "home.table-option.$id"
}

/**
 * The calculator tab.
 *
 * Content only: the app's single Scaffold owns the top bar, the bottom navigation and the snackbar
 * host, so this composable can be previewed and tested without an Activity, and the bottom bar
 * cannot be covered by a screen's own insets.
 *
 * Two rules from the domain are visible here and must stay: an ad is never placed over an input,
 * and a refused case shows the correction and no number.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    isPro: Boolean,
    canSaveCase: Boolean,
    savedCaseCount: Int,
    freeCaseLimit: Int,
    onSaveCase: (String) -> Unit,
    onLimitReached: () -> Unit,
    onShareReport: () -> Unit,
    onOpenPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metric = state.unitSystem == UnitSystem.METRIC
    var saveDialogVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(stringResource(R.string.section_units)) {
            MethodChoiceList(
                selected = state.method,
                onSelect = { method -> onEvent(HomeEvent.MethodChanged(method)) },
            )
            SegmentedRow(
                options = listOf(
                    stringResource(R.string.units_imperial) to (state.unitSystem == UnitSystem.IMPERIAL),
                    stringResource(R.string.units_metric) to (state.unitSystem == UnitSystem.METRIC),
                ),
                onSelect = { index ->
                    onEvent(HomeEvent.UnitChanged(if (index == 0) UnitSystem.IMPERIAL else UnitSystem.METRIC))
                },
            )
        }

        SectionCard(stringResource(R.string.section_readings)) {
            Entry(state, onEvent, HomeField.MATERIAL, R.string.material_label)
            Entry(state, onEvent, HomeField.PRESSURE, R.string.pressure_label)
            Entry(state, onEvent, HomeField.LOAD, if (metric) R.string.load_label_metric else R.string.load_label)
            Entry(state, onEvent, HomeField.LENGTH, if (metric) R.string.length_label_metric else R.string.length_label)
        }

        SectionCard(stringResource(R.string.fittings_heading)) {
            Entry(state, onEvent, HomeField.FITTING_QUANTITY, R.string.fitting_quantity_label)
            Entry(
                state,
                onEvent,
                HomeField.FITTING_LENGTH,
                if (metric) R.string.fitting_length_label_metric else R.string.fitting_length_label,
            )
        }

        if (state.method == SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS) {
            SectionCard(stringResource(R.string.physical_inputs_heading)) {
                Text(
                    text = stringResource(R.string.unconverted_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Entry(state, onEvent, HomeField.GAS_SPECIFIC_GRAVITY, R.string.gas_gravity_label)
                Entry(state, onEvent, HomeField.PRESSURE_DROP, R.string.pressure_drop_label)
                Entry(state, onEvent, HomeField.PIPE_DIAMETER, R.string.pipe_diameter_label)
            }
        }

        if (state.method.returnsFromUserTable) {
            RecordedTableCard(state = state, onEvent = onEvent)
            // The lookup compares the case's material or system, inlet-pressure class and gas
            // specific gravity against the table's declaration. The first two are entered under
            // Readings; the gravity reading has no home there, so it is asked for here. No other
            // reading is compared, and nothing here says one is.
            SectionCard(stringResource(R.string.table_applicability_heading)) {
                Text(
                    text = stringResource(R.string.table_applicability_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Entry(state, onEvent, HomeField.GAS_SPECIFIC_GRAVITY, R.string.gas_gravity_label)
            }
        }

        // The capacity entry and its provenance belong to the arithmetic methods. A table-returning
        // method takes its citation from the table the user recorded, so asking for a confirmed
        // capacity here would invite the user to record a value the selected method never reads.
        if (!state.method.returnsFromUserTable) {
            SectionCard(stringResource(R.string.section_source)) {
                Text(
                    text = stringResource(R.string.capacity_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Entry(state, onEvent, HomeField.CAPACITY, if (metric) R.string.capacity_label_metric else R.string.capacity_label)
                FilledTonalButton(
                    onClick = { onEvent(HomeEvent.ConfirmCapacity) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.confirm_capacity))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.provenanceMode == ProvenanceMode.USER_SUPPLIED,
                        onClick = { onEvent(HomeEvent.ProvenanceChanged(ProvenanceMode.USER_SUPPLIED)) },
                        label = { Text(stringResource(R.string.user_supplied)) },
                    )
                    FilterChip(
                        selected = state.provenanceMode == ProvenanceMode.CODE_CITATION,
                        onClick = { onEvent(HomeEvent.ProvenanceChanged(ProvenanceMode.CODE_CITATION)) },
                        label = { Text(stringResource(R.string.code_citation)) },
                    )
                }
                if (state.provenanceMode == ProvenanceMode.CODE_CITATION) {
                    Entry(state, onEvent, HomeField.EDITION, R.string.edition_label)
                    Entry(state, onEvent, HomeField.TABLE, R.string.table_label)
                    Entry(state, onEvent, HomeField.ROW, R.string.row_label)
                }
            }
        }

        Button(
            onClick = { onEvent(HomeEvent.Evaluate) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.evaluate_action))
        }

        AnimatedVisibility(
            visible = state.evaluation != null,
            enter = fadeIn() + expandVertically(),
            exit = shrinkVertically(),
        ) {
            val result = state.evaluation
            if (result != null) {
                ResultCard(state = state, result = result, metric = metric)
            }
        }

        ActionRow(
            // A returned-from-table value can be reported too: both results have a report, and a
            // refusal has none. This is the same three-way decision the report itself takes.
            reportReady = state.evaluation.hasShareableReport(),
            onSaveCase = {
                if (canSaveCase) saveDialogVisible = true else onLimitReached()
            },
            onShareReport = onShareReport,
        )

        if (!isPro) {
            ProUpsellCard(
                savedCaseCount = savedCaseCount,
                freeCaseLimit = freeCaseLimit,
                onOpenPaywall = onOpenPaywall,
            )
        }
    }

    if (saveDialogVisible) {
        SaveCaseDialog(
            onDismiss = { saveDialogVisible = false },
            onConfirm = { name ->
                saveDialogVisible = false
                onSaveCase(name)
            },
        )
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

@Composable
private fun SegmentedRow(options: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (label, selected) ->
            SegmentedButton(
                selected = selected,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label)
            }
        }
    }
}

/**
 * The method selector, as a vertical choice list.
 *
 * It replaced a two-option segmented row for a reason worth keeping: a segmented row divides its
 * width between its segments, so four labels would each get a quarter of the screen and the longest
 * locale's words would wrap or clip. A vertical list gives every label the full width and does not
 * depend on how long any language's words are (`.cursor/rules/visual-identity.mdc`).
 *
 * Every `SizingMethod` member is listed, so adding a method is a compile-visible change here rather
 * than a missing option in a `when` the compiler cannot check.
 */
@Composable
private fun MethodChoiceList(selected: SizingMethod, onSelect: (SizingMethod) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .testTag(HomeScreenTags.METHOD_LIST),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SizingMethod.entries.forEach { method ->
            ChoiceRow(
                label = stringResource(method.labelRes()),
                selected = method == selected,
                onSelect = { onSelect(method) },
                modifier = Modifier.testTag(HomeScreenTags.method(method)),
            )
        }
    }
}

/**
 * One choice in a vertical list: the whole row is the target, so the row's height does not depend on
 * the label's length and a long translation wraps instead of shrinking the target.
 */
@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The row owns the click, so the indicator carries no click of its own and a screen reader
        // announces one selectable target rather than two.
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

/**
 * The table the case is read against.
 *
 * The list is the user's own recorded tables and nothing else: the app supplies no table content, so
 * an empty store shows an empty picker rather than a placeholder to be edited. The forward method
 * also asks for the nominal size being read; the reverse method searches every recorded size and
 * takes no size at all.
 */
@Composable
private fun RecordedTableCard(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    SectionCard(stringResource(R.string.section_recorded_table)) {
        Text(
            text = stringResource(R.string.recorded_table_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.availableTables.isEmpty()) {
            Text(
                text = stringResource(R.string.tables_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(HomeScreenTags.TABLE_EMPTY),
            )
            Text(
                text = stringResource(R.string.tables_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .testTag(HomeScreenTags.TABLE_PICKER),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                state.availableTables.forEach { table ->
                    RecordedTableChoice(
                        name = table.name.ifBlank { stringResource(R.string.tables_unnamed) },
                        summary = stringResource(
                            R.string.tables_citation_summary,
                            table.citation.codeAndEdition,
                            table.citation.tableIdentifier,
                        ),
                        selected = table.id == state.selectedTableId,
                        onSelect = { onEvent(HomeEvent.FieldChanged(HomeField.SELECTED_TABLE, table.id)) },
                        modifier = Modifier.testTag(HomeScreenTags.tableOption(table.id)),
                    )
                }
            }
        }
        if (state.method == SizingMethod.RETURN_FROM_USER_TABLE) {
            Entry(
                state = state,
                onEvent = onEvent,
                field = HomeField.TABLE_NOMINAL_SIZE,
                label = R.string.table_nominal_size_label,
                tag = HomeScreenTags.TABLE_NOMINAL_SIZE,
            )
        }
    }
}

@Composable
private fun RecordedTableChoice(
    name: String,
    summary: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultCard(state: HomeUiState, result: SizingEvaluation, metric: Boolean) {
    when (result) {
        // A refusal renders the correction and nothing else: no partial number and no verdict may be
        // shown for a case the domain did not compute.
        is SizingEvaluation.Refused -> ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = result.correctionRequired,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        is SizingEvaluation.Computed -> ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HomeScreenTags.RESULT_CARD),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.result_title), style = MaterialTheme.typography.titleLarge)
                val methodLabel = stringResource(state.method.labelRes())
                computedResultLines(state, result, metric, methodLabel).forEach { line ->
                    Text(stringResource(line.labelRes, *line.args.toTypedArray()))
                }
                ResultDisclaimer()
            }
        }

        // A returned value is rendered from its own line list, never from the arithmetic one: there
        // is no comparison to report and no supports/does-not-support verdict to give, and sharing
        // the arithmetic wording would present the user's own row as something the app checked.
        is SizingEvaluation.ReturnedFromUserTable -> ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HomeScreenTags.RESULT_CARD),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.returned_table_result_title), style = MaterialTheme.typography.titleLarge)
                val methodLabel = stringResource(state.method.labelRes())
                val tableLabel = state.tableNameOrNull(result.capacityProvenance.tableId)
                    ?: stringResource(R.string.tables_unnamed)
                returnedTableResultLines(state, result, metric, methodLabel, tableLabel).forEach { line ->
                    Text(stringResource(line.labelRes, *line.args.toTypedArray()))
                }
                ResultDisclaimer()
            }
        }
    }
}

/** The calculation-aid disclaimer every result view carries, so no figure is read without it. */
@Composable
private fun ResultDisclaimer() {
    Text(
        text = stringResource(R.string.disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(HomeScreenTags.RESULT_DISCLAIMER),
    )
}

@Composable
private fun ActionRow(reportReady: Boolean, onSaveCase: () -> Unit, onShareReport: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(
            onClick = onSaveCase,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.save_case_action))
        }
        FilledTonalButton(
            onClick = onShareReport,
            enabled = reportReady,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag(HomeScreenTags.REPORT_ACTION),
        ) {
            Text(stringResource(R.string.report_action))
        }
    }
}

@Composable
private fun ProUpsellCard(savedCaseCount: Int, freeCaseLimit: Int, onOpenPaywall: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = if (savedCaseCount >= freeCaseLimit) {
                    stringResource(R.string.cases_limit_reached, freeCaseLimit)
                } else {
                    stringResource(R.string.cases_saved_count, savedCaseCount, freeCaseLimit)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            TextButton(onClick = onOpenPaywall, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.paywall_benefit_report))
            }
        }
    }
}

@Composable
private fun SaveCaseDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_case_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.save_case_name_label)) },
                    placeholder = { Text(stringResource(R.string.save_case_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save_case_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.paywall_later)) }
        },
    )
}

@Composable
private fun Entry(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    field: HomeField,
    label: Int,
    tag: String? = null,
) {
    val value = when (field) {
        HomeField.MATERIAL -> state.material
        HomeField.PRESSURE -> state.pressure
        HomeField.LOAD -> state.loadMbh
        HomeField.LENGTH -> state.lengthFeet
        HomeField.FITTING_QUANTITY -> state.fittingQuantity
        HomeField.FITTING_LENGTH -> state.fittingLengthFeet
        HomeField.CAPACITY -> state.capacity
        HomeField.EDITION -> state.codeEdition
        HomeField.TABLE -> state.table
        HomeField.ROW -> state.row
        HomeField.GAS_SPECIFIC_GRAVITY -> state.gasSpecificGravity
        HomeField.PRESSURE_DROP -> state.pressureDropAllowance
        HomeField.PIPE_DIAMETER -> state.pipeInsideDiameter
        HomeField.SELECTED_TABLE -> state.selectedTableId
        HomeField.TABLE_NOMINAL_SIZE -> state.tableNominalSize
    }
    OutlinedTextField(
        value = value,
        onValueChange = { onEvent(HomeEvent.FieldChanged(field, it)) },
        modifier = if (tag == null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().testTag(tag),
        label = { Text(stringResource(label)) },
        singleLine = true,
    )
}

// ---------------------------------------------------------------------------------------------
// Previews. One per method, because the method control now reveals a different part of the form for
// each. The readings and the recorded table below are invented synthetics ("synthetic system", 1.0,
// 2.0): the app supplies no table content, and these exemplify a populated screen, nothing else. The
// gas specific gravity of 21.5 is no gas's and is read from no code or standard.
// ---------------------------------------------------------------------------------------------

@Preview(name = "Method: capacity comparison", showBackground = true)
@Composable
private fun CapacityComparisonPreview() {
    PreviewScreen(HomeUiState(method = SizingMethod.CAPACITY_COMPARISON))
}

@Preview(name = "Method: capacity + physical inputs", showBackground = true)
@Composable
private fun PhysicalInputsPreview() {
    PreviewScreen(HomeUiState(method = SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS))
}

@Preview(name = "Method: return capacity from recorded table", showBackground = true)
@Composable
private fun ReturnFromTablePreview() {
    PreviewScreen(previewTableState(SizingMethod.RETURN_FROM_USER_TABLE))
}

@Preview(name = "Method: smallest size for load", showBackground = true)
@Composable
private fun SmallestSizePreview() {
    PreviewScreen(previewTableState(SizingMethod.SMALLEST_SIZE_FOR_LOAD))
}

@Composable
private fun PreviewScreen(state: HomeUiState) {
    HomeScreen(
        state = state,
        onEvent = {},
        isPro = false,
        canSaveCase = true,
        savedCaseCount = 0,
        freeCaseLimit = MonetizationConfig.FREE_CASE_LIMIT,
        onSaveCase = {},
        onLimitReached = {},
        onShareReport = {},
        onOpenPaywall = {},
    )
}

private fun previewTableState(method: SizingMethod): HomeUiState = HomeUiState(
    material = "synthetic system",
    pressure = "synthetic class",
    loadMbh = "100",
    lengthFeet = "40",
    gasSpecificGravity = "21.5",
    method = method,
    selectedTableId = PREVIEW_TABLE_ID,
    tableNominalSize = "synthetic-size-a",
    availableTables = listOf(previewTable()),
)

private const val PREVIEW_TABLE_ID = "preview-table"

private fun previewTable(): UserCapacityTable = UserCapacityTable(
    id = PREVIEW_TABLE_ID,
    name = "Synthetic preview table",
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
        TableRow("synthetic-size-b", null, 1.0, 2.0),
    ),
)

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.UnitSystem

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
            SegmentedRow(
                options = listOf(
                    stringResource(R.string.method_capacity) to (state.method == SizingMethod.CAPACITY_COMPARISON),
                    stringResource(R.string.method_physical) to (state.method == SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS),
                ),
                onSelect = { index ->
                    onEvent(
                        HomeEvent.MethodChanged(
                            if (index == 0) SizingMethod.CAPACITY_COMPARISON else SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS,
                        ),
                    )
                },
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
            reportReady = state.evaluation is SizingEvaluation.Computed,
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

        is SizingEvaluation.Computed -> ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.result_title), style = MaterialTheme.typography.titleLarge)
                val methodLabel = stringResource(
                    if (state.method == SizingMethod.CAPACITY_COMPARISON) R.string.method_capacity else R.string.method_physical,
                )
                computedResultLines(state, result, metric, methodLabel).forEach { line ->
                    Text(stringResource(line.labelRes, *line.args.toTypedArray()))
                }
                Text(
                    text = stringResource(R.string.disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
                .heightIn(min = 48.dp),
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
private fun Entry(state: HomeUiState, onEvent: (HomeEvent) -> Unit, field: HomeField, label: Int) {
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
    }
    OutlinedTextField(
        value = value,
        onValueChange = { onEvent(HomeEvent.FieldChanged(field, it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        singleLine = true,
    )
}

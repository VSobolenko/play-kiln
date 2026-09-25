package com.gexetr.pipesizing.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.UnitSystem
import com.gexetr.pipesizing.sizing.Units
import java.util.Locale

@Composable
fun HomeRoute(onNavigateToAbout: () -> Unit, modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel::onEvent, modifier, onNavigateToAbout)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeUiState, onEvent: (HomeEvent) -> Unit, modifier: Modifier = Modifier, onAboutClick: () -> Unit = {}) {
    val metric = state.unitSystem == UnitSystem.METRIC
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = { TextButton(onClick = onAboutClick) { Text(stringResource(R.string.home_about_action)) } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.method_label), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.method == SizingMethod.CAPACITY_COMPARISON, { onEvent(HomeEvent.MethodChanged(SizingMethod.CAPACITY_COMPARISON)) }, label = { Text(stringResource(R.string.method_capacity)) })
                FilterChip(state.method == SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS, { onEvent(HomeEvent.MethodChanged(SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS)) }, label = { Text(stringResource(R.string.method_physical)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.unitSystem == UnitSystem.IMPERIAL, { onEvent(HomeEvent.UnitChanged(UnitSystem.IMPERIAL)) }, label = { Text(stringResource(R.string.units_imperial)) })
                FilterChip(state.unitSystem == UnitSystem.METRIC, { onEvent(HomeEvent.UnitChanged(UnitSystem.METRIC)) }, label = { Text(stringResource(R.string.units_metric)) })
            }
            Entry(state, onEvent, HomeField.MATERIAL, R.string.material_label)
            Entry(state, onEvent, HomeField.PRESSURE, R.string.pressure_label)
            Entry(state, onEvent, HomeField.LOAD, if (metric) R.string.load_label_metric else R.string.load_label)
            Entry(state, onEvent, HomeField.LENGTH, if (metric) R.string.length_label_metric else R.string.length_label)
            if (state.method == SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS) {
                Text(stringResource(R.string.physical_inputs_heading), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.unconverted_note), style = MaterialTheme.typography.bodyMedium)
                Entry(state, onEvent, HomeField.GAS_SPECIFIC_GRAVITY, R.string.gas_gravity_label)
                Entry(state, onEvent, HomeField.PRESSURE_DROP, R.string.pressure_drop_label)
                Entry(state, onEvent, HomeField.PIPE_DIAMETER, R.string.pipe_diameter_label)
            }
            Text(stringResource(R.string.fittings_heading), style = MaterialTheme.typography.titleMedium)
            Entry(state, onEvent, HomeField.FITTING_QUANTITY, R.string.fitting_quantity_label)
            Entry(state, onEvent, HomeField.FITTING_LENGTH, if (metric) R.string.fitting_length_label_metric else R.string.fitting_length_label)
            Text(stringResource(R.string.capacity_help), style = MaterialTheme.typography.bodyMedium)
            Entry(state, onEvent, HomeField.CAPACITY, if (metric) R.string.capacity_label_metric else R.string.capacity_label)
            TextButton(onClick = { onEvent(HomeEvent.ConfirmCapacity) }) { Text(stringResource(R.string.confirm_capacity)) }
            Text(stringResource(R.string.provenance_heading), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.provenanceMode == ProvenanceMode.USER_SUPPLIED, { onEvent(HomeEvent.ProvenanceChanged(ProvenanceMode.USER_SUPPLIED)) }, label = { Text(stringResource(R.string.user_supplied)) })
                FilterChip(state.provenanceMode == ProvenanceMode.CODE_CITATION, { onEvent(HomeEvent.ProvenanceChanged(ProvenanceMode.CODE_CITATION)) }, label = { Text(stringResource(R.string.code_citation)) })
            }
            if (state.provenanceMode == ProvenanceMode.CODE_CITATION) {
                Entry(state, onEvent, HomeField.EDITION, R.string.edition_label); Entry(state, onEvent, HomeField.TABLE, R.string.table_label); Entry(state, onEvent, HomeField.ROW, R.string.row_label)
            }
            Button(onClick = { onEvent(HomeEvent.Evaluate) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.evaluate_action)) }
            when (val result = state.evaluation) {
                // A refusal renders the correction and nothing else: no partial number, no placeholder
                // and no verdict may be shown for a case the domain did not compute.
                is SizingEvaluation.Refused -> Text(result.correctionRequired, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                is SizingEvaluation.Computed -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.result_title), style = MaterialTheme.typography.titleLarge)
                    if (metric) {
                        Text(stringResource(R.string.result_load_metric, format(Units.mbhToKilowatts(result.requiredLoadMbh))))
                        Text(stringResource(R.string.result_length_metric, format(Units.feetToMeters(result.totalLengthFeet))))
                    } else {
                        Text(stringResource(R.string.result_load_imperial, format(result.requiredLoadMbh)))
                        Text(stringResource(R.string.result_length_imperial, format(result.totalLengthFeet)))
                    }
                    Text(if (result.supportsRequiredLoad) stringResource(R.string.capacity_supports) else stringResource(R.string.capacity_does_not_support))
                    Text(stringResource(R.string.provenance_result, result.capacityProvenance.toString()))
                    Text(if (metric) stringResource(R.string.result_fitting_length_metric, format(Units.feetToMeters(result.fittingAllowanceFeet))) else stringResource(R.string.result_fitting_length_imperial, format(result.fittingAllowanceFeet)))
                    Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodyMedium)
                }
                null -> Unit
            }
        }
    }
}

/** Display rounding only; the domain keeps full precision, so this never changes a physical value. */
private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)

@Composable private fun Entry(state: HomeUiState, onEvent: (HomeEvent) -> Unit, field: HomeField, label: Int) {
    val value = when (field) { HomeField.MATERIAL -> state.material; HomeField.PRESSURE -> state.pressure; HomeField.LOAD -> state.loadMbh; HomeField.LENGTH -> state.lengthFeet; HomeField.FITTING_QUANTITY -> state.fittingQuantity; HomeField.FITTING_LENGTH -> state.fittingLengthFeet; HomeField.CAPACITY -> state.capacity; HomeField.EDITION -> state.codeEdition; HomeField.TABLE -> state.table; HomeField.ROW -> state.row; HomeField.GAS_SPECIFIC_GRAVITY -> state.gasSpecificGravity; HomeField.PRESSURE_DROP -> state.pressureDropAllowance; HomeField.PIPE_DIAMETER -> state.pipeInsideDiameter }
    OutlinedTextField(value, { onEvent(HomeEvent.FieldChanged(field, it)) }, Modifier.fillMaxWidth(), label = { Text(stringResource(label)) }, singleLine = true)
}

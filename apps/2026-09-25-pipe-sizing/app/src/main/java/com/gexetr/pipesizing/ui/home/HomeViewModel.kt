package com.gexetr.pipesizing.ui.home

import androidx.lifecycle.ViewModel
import com.gexetr.pipesizing.sizing.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onEvent(event: HomeEvent) {
        _uiState.update { state ->
            when (event) {
                is HomeEvent.FieldChanged -> state.set(event.field, event.value)
                is HomeEvent.ProvenanceChanged -> state.copy(provenanceMode = event.mode)
                is HomeEvent.MethodChanged -> state.copy(method = event.method, capacityConfirmed = false, evaluation = null)
                is HomeEvent.UnitChanged -> state.copy(unitSystem = event.unitSystem, evaluation = null)
                HomeEvent.ConfirmCapacity -> state.copy(capacityConfirmed = true, evaluation = null)
                HomeEvent.Evaluate -> state.copy(evaluation = evaluate(state))
            }
        }
    }

    internal fun evaluate(state: HomeUiState): SizingEvaluation {
        // One provenance is recorded for every capacity-like reading in the case. The evaluator
        // refuses a capacity or fitting equivalent length without one, and the domain surfaces the
        // capacity's provenance on a computed result, so the app never shows a number without it.
        val provenance = when (state.provenanceMode) {
            ProvenanceMode.USER_SUPPLIED -> UserProvenance.UserSupplied
            ProvenanceMode.CODE_CITATION -> UserProvenance.CodeCitation(state.codeEdition, state.table, state.row)
        }
        val metric = state.unitSystem == UnitSystem.METRIC
        val fittingBlank = state.fittingQuantity.isBlank() && state.fittingLengthFeet.isBlank()
        val case = SizingCase(
            materialOrSystem = state.material,
            pressureClass = state.pressure,
            method = state.method,
            unitSystem = state.unitSystem,
            loads = if (state.loadMbh.isBlank()) emptyList() else listOf(Load("User load", reading(state.loadMbh, metric, { Units.kilowattsToMbh(it) }))),
            developedLengths = if (state.lengthFeet.isBlank()) emptyList() else listOf(DevelopedLength("Developed run", reading(state.lengthFeet, metric, { Units.metersToFeet(it) }))),
            // A case with no fittings is expressed as an empty list. Sending a zero quantity instead
            // would be refused as out of range, leaving no way to size a run that has no fittings.
            fittingAllowances = if (fittingBlank) {
                emptyList()
            } else {
                listOf(
                    FittingAllowance(
                        fittingLabel = "User fitting allowance",
                        quantity = state.fittingQuantity.toIntOrNull() ?: 0,
                        equivalentLengthFeet = SourcedValue(
                            reading(state.fittingLengthFeet, metric, { Units.metersToFeet(it) }),
                            provenance,
                        ),
                    ),
                )
            },
            // An unconfirmed capacity is absent, never defaulted: the evaluator refuses a missing
            // capacity, and dropping the confirmation on any edit keeps a stale value for another
            // method from being reused silently.
            capacity = if (state.capacityConfirmed) {
                CapacityEntry(reading(state.capacity, metric, { Units.kilowattsToMbh(it) }), provenance, state.method)
            } else {
                null
            },
            // Specific gravity is dimensionless. Pressure-drop allowance and pipe inside diameter are
            // recorded exactly as the applicable code book states them: the domain holds them as bare
            // numbers with no unit of their own, and no unambiguous conversion for either exists
            // there, so converting them here would silently reinterpret the user's own reading.
            // A blank field is an absent input, not an invalid one: passing null lets the evaluator
            // report MISSING_INPUT and name the field, where a NaN would only read as out of range.
            gasSpecificGravity = optionalReading(state.gasSpecificGravity, provenance),
            pressureDropAllowance = optionalReading(state.pressureDropAllowance, provenance),
            pipeInsideDiameter = optionalReading(state.pipeInsideDiameter, provenance),
        )
        return SizingEvaluator.evaluate(case)
    }

    /**
     * Reads a user entry into the domain's canonical imperial units. In metric mode the entry is in
     * the metric unit shown by the field's label and is converted on the way in; the canonical value
     * is what the domain stores, so switching units never changes the physical quantity.
     */
    private fun reading(text: String, metric: Boolean, metricToCanonical: (Double) -> Double): Double {
        val parsed = text.toDoubleOrNull() ?: return Double.NaN
        return if (metric) metricToCanonical(parsed) else parsed
    }

    /**
     * A method-required reading that the user may not have entered yet: blank means absent, while a
     * non-blank value that will not parse stays invalid so the evaluator can say so.
     */
    private fun optionalReading(text: String, provenance: UserProvenance): SourcedValue? =
        if (text.isBlank()) null else SourcedValue(text.toDoubleOrNull() ?: Double.NaN, provenance)

    private fun HomeUiState.set(field: HomeField, value: String) = when (field) {
        HomeField.MATERIAL -> copy(material = value); HomeField.PRESSURE -> copy(pressure = value)
        HomeField.LOAD -> copy(loadMbh = value); HomeField.LENGTH -> copy(lengthFeet = value)
        HomeField.FITTING_QUANTITY -> copy(fittingQuantity = value); HomeField.FITTING_LENGTH -> copy(fittingLengthFeet = value)
        HomeField.CAPACITY -> copy(capacity = value, capacityConfirmed = false)
        HomeField.EDITION -> copy(codeEdition = value); HomeField.TABLE -> copy(table = value); HomeField.ROW -> copy(row = value)
        HomeField.GAS_SPECIFIC_GRAVITY -> copy(gasSpecificGravity = value)
        HomeField.PRESSURE_DROP -> copy(pressureDropAllowance = value)
        HomeField.PIPE_DIAMETER -> copy(pipeInsideDiameter = value)
    }
}

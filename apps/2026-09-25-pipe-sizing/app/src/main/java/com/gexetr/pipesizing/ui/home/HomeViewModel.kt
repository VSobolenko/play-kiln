package com.gexetr.pipesizing.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gexetr.pipesizing.sizing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val persistence: CasePersistence = CasePersistence.None,
    scope: CoroutineScope? = null,
) : ViewModel() {
    // Resolved lazily so a state holder constructed with an explicit scope never touches the main
    // dispatcher, which is what keeps these tests runnable as plain JVM tests.
    private val workScope: CoroutineScope by lazy { scope ?: viewModelScope }
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        workScope.launch {
            val stored = persistence.load()
            if (stored.isNotEmpty()) _uiState.update { it.restoredFrom(stored) }
        }
    }

    fun onEvent(event: HomeEvent) {
        _uiState.update { state ->
            when (event) {
                // Any edit invalidates a shown result. Keeping it would let the screen display figures
                // computed for one case beside the inputs of another.
                is HomeEvent.FieldChanged -> state.set(event.field, event.value).copy(evaluation = null)
                is HomeEvent.ProvenanceChanged -> state.copy(provenanceMode = event.mode, evaluation = null)
                is HomeEvent.MethodChanged -> state.copy(method = event.method, capacityConfirmed = false, evaluation = null)
                is HomeEvent.UnitChanged -> state.withUnit(event.unitSystem)
                HomeEvent.ConfirmCapacity -> state.copy(capacityConfirmed = true, evaluation = null)
                HomeEvent.Evaluate -> state.copy(evaluation = evaluate(state))
            }
        }
        workScope.launch { persistence.save(_uiState.value.toStoredCase()) }
    }

    internal fun evaluate(state: HomeUiState): SizingEvaluation {
        // One provenance is recorded for every capacity-like reading in the case. The evaluator
        // refuses a capacity or fitting equivalent length without one, and the domain surfaces the
        // capacity's provenance on a computed result, so the app never shows a number without it.
        val provenance: UserProvenance? = when (state.provenanceMode) {
            ProvenanceMode.USER_SUPPLIED -> UserProvenance.UserSupplied
            ProvenanceMode.CODE_CITATION -> state.citationOrNull()
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
     * Switches the unit mode while preserving every physical quantity. Imperial is the canonical
     * unit, so a value the user entered in metric is converted back on the way out. Relabelling the
     * entered numbers instead would silently change what they mean - 100 MBH becoming 100 kW - which
     * is exactly the reinterpretation this app refuses to do anywhere else.
     *
     * Readings with no unit of their own (specific gravity) or no unambiguous conversion
     * (pressure-drop allowance, pipe inside diameter) are carried across unchanged; the labels for
     * those say they are recorded as read.
     */
    private fun HomeUiState.withUnit(unit: UnitSystem): HomeUiState {
        if (unit == unitSystem) return copy(evaluation = null)
        return copy(
            unitSystem = unit,
            evaluation = null,
            loadMbh = convertUnit(loadMbh, unit, Units::kilowattsToMbh, Units::mbhToKilowatts),
            lengthFeet = convertUnit(lengthFeet, unit, Units::metersToFeet, Units::feetToMeters),
            fittingLengthFeet = convertUnit(fittingLengthFeet, unit, Units::metersToFeet, Units::feetToMeters),
            capacity = convertUnit(capacity, unit, Units::kilowattsToMbh, Units::mbhToKilowatts),
        )
    }

    /** Re-expresses one entered reading in [target], keeping full precision so a toggle cannot drift. */
    private fun HomeUiState.convertUnit(
        text: String,
        target: UnitSystem,
        toCanonical: (Double) -> Double,
        fromCanonical: (Double) -> Double,
    ): String {
        val entered = text.toDoubleOrNull() ?: return text
        val canonical = if (unitSystem == UnitSystem.METRIC) toCanonical(entered) else entered
        val shown = if (target == UnitSystem.METRIC) fromCanonical(canonical) else canonical
        return shown.toString()
    }

    /**
     * A citation with no edition or table names no source, so it is treated as absent provenance and
     * the evaluator refuses it. Accepting it would record a citation that points at nothing and
     * would let a result be shown for a reading whose source the user never supplied.
     */
    private fun HomeUiState.citationOrNull(): UserProvenance? =
        if (codeEdition.isBlank() || table.isBlank()) {
            null
        } else {
            UserProvenance.CodeCitation(codeEdition, table, row)
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
    private fun optionalReading(text: String, provenance: UserProvenance?): SourcedValue? =
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

    companion object {
        /** Supplies the on-device store so a case survives process death. */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return viewModelFactory {
                initializer { HomeViewModel(DataStoreCasePersistence(applicationContext)) }
            }
        }
    }
}

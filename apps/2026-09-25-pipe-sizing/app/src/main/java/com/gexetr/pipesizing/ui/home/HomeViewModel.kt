package com.gexetr.pipesizing.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gexetr.pipesizing.sizing.*
import com.gexetr.pipesizing.ui.tables.DataStoreTablesRepository
import com.gexetr.pipesizing.ui.tables.TablesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val persistence: CasePersistence = CasePersistence.None,
    private val tables: TablesRepository = TablesRepository.InMemory(),
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
        // The user's recorded tables are mirrored into the state so the picker can list them and an
        // evaluation can resolve the selected one without a suspending store read on its own path:
        // the shown result must not depend on when a read happened to finish.
        workScope.launch {
            tables.tables.collect { recorded ->
                _uiState.update { it.copy(availableTables = recorded) }
            }
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
                is HomeEvent.CaseLoaded -> state.restoredFrom(event.values)
            }
        }
        workScope.launch { persistence.save(_uiState.value.toStoredCase()) }
    }

    internal fun evaluate(state: HomeUiState): SizingEvaluation {
        // A case bound to a table whose recorded contents have moved is refused before anything else
        // is read out of that table: every other answer it could give is an answer about a revision
        // the case was not read against (design D9).
        state.tableRevisionRefusal()?.let { return it }
        // One provenance is recorded for every capacity-like reading in the case. The evaluator
        // refuses a capacity or fitting equivalent length without one, and the domain surfaces the
        // capacity's provenance on a computed result, so the app never shows a number without it.
        val provenance: UserProvenance? = when (state.provenanceMode) {
            ProvenanceMode.USER_SUPPLIED -> UserProvenance.UserSupplied
            ProvenanceMode.CODE_CITATION -> state.citationOrNull()
        }
        // The arithmetic runs on the canonical readings, never on the display text. Parsing what the
        // field happens to show would make the result depend on the unit label and on display
        // rounding, which is the confusion this app exists to avoid.
        val fittingBlank = state.fittingQuantity.isBlank() && state.canonicalFittingLengthFeet == null
        val case = SizingCase(
            materialOrSystem = state.material,
            pressureClass = state.pressure,
            method = state.method,
            unitSystem = state.unitSystem,
            loads = state.canonicalLoadMbh?.let { listOf(Load("User load", it)) } ?: emptyList(),
            developedLengths = state.canonicalLengthFeet?.let {
                listOf(DevelopedLength("Developed run", it))
            } ?: emptyList(),
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
                            state.canonicalFittingLengthFeet ?: Double.NaN,
                            provenance,
                        ),
                    ),
                )
            },
            // An unconfirmed capacity is absent, never defaulted: the evaluator refuses a missing
            // capacity, and dropping the confirmation on any edit keeps a stale value for another
            // method from being reused silently.
            capacity = if (state.capacityConfirmed) {
                CapacityEntry(state.canonicalCapacity ?: Double.NaN, provenance, state.method)
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
            // The table the case is read against is an input like any other, so it travels on the
            // case; the evaluator routes table-returning methods to the lookup engine and the two
            // arithmetic methods to the arithmetic it always performed.
            tableSelection = TableSelection(
                tableId = state.selectedTableId.trim().takeIf { it.isNotEmpty() },
                nominalSize = state.tableNominalSize.trim().takeIf { it.isNotEmpty() },
            ),
        )
        return SizingEvaluator.evaluate(case, state.selectedTable())
    }

    /**
     * The recorded table the case names, or null when it names none. Read out of the state rather
     * than fetched from the store, because the evaluation runs inside a state update and a
     * suspending read there would make the shown result depend on store timing.
     */
    private fun HomeUiState.selectedTable(): UserCapacityTable? =
        selectedTableId.trim()
            .takeIf { it.isNotEmpty() }
            ?.let { id -> availableTables.firstOrNull { it.id == id } }

    /**
     * The one refusal the lookup engine cannot raise, because a deleted or edited table never
     * reaches it as itself. A case that names no table is left to the engine, which refuses it with
     * `NO_TABLE_SELECTED`; a case that names a table which is gone, or whose contents no longer
     * hash to the revision the case recorded, is refused here as `TABLE_CHANGED`.
     */
    private fun HomeUiState.tableRevisionRefusal(): SizingEvaluation.Refused? {
        if (!method.returnsFromUserTable) return null
        val id = selectedTableId.trim().takeIf { it.isNotEmpty() } ?: return null
        val table = availableTables.firstOrNull { it.id == id }
            ?: return SizingEvaluation.Refused(
                RefusalKind.TABLE_CHANGED,
                "Recorded table is gone: record the table this case names again, or choose another table.",
            )
        val recordedRevision = tableDigest.trim()
        if (recordedRevision.isNotEmpty() && recordedRevision != table.contentDigest()) {
            return SizingEvaluation.Refused(
                RefusalKind.TABLE_CHANGED,
                "Recorded table ${table.name} changed after this case was read against it. Read the case against the table again.",
            )
        }
        return null
    }

    /**
     * Switches the unit mode while preserving every physical quantity. The canonical readings do not
     * move at all; only the text shown in the fields is re-expressed in the newly selected unit, which
     * is why repeated toggling cannot drift. Relabelling the entered numbers instead would silently
     * change what they mean - 100 MBH becoming 100 kW - which is exactly the reinterpretation this app
     * refuses to do anywhere else.
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
            loadMbh = reUnit(loadMbh, canonicalLoadMbh, unit, Units::mbhToKilowatts),
            lengthFeet = reUnit(lengthFeet, canonicalLengthFeet, unit, Units::feetToMeters),
            fittingLengthFeet = reUnit(fittingLengthFeet, canonicalFittingLengthFeet, unit, Units::feetToMeters),
            capacity = reUnit(capacity, canonicalCapacity, unit, Units::mbhToKilowatts),
        )
    }

    /**
     * Re-expresses one reading in [target]. Text that never parsed is left exactly as the user typed
     * it: there is no value to convert, and replacing their entry with a conversion of nothing would
     * destroy what they are still in the middle of correcting.
     */
    private fun reUnit(
        text: String,
        canonical: Double?,
        target: UnitSystem,
        fromCanonical: (Double) -> Double,
    ): String {
        if (canonical == null || canonical.isNaN()) return text
        val shown = if (target == UnitSystem.METRIC) fromCanonical(canonical) else canonical
        return readableNumber(shown)
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
     * A method-required reading that the user may not have entered yet: blank means absent, while a
     * non-blank value that will not parse stays invalid so the evaluator can say so.
     */
    private fun optionalReading(text: String, provenance: UserProvenance?): SourcedValue? =
        if (text.isBlank()) null else SourcedValue(text.toDoubleOrNull() ?: Double.NaN, provenance)

    /**
     * Applies one edit, keeping the canonical value of a quantity field in step with its text. The
     * text is parsed with the unit the user is currently working in, so the same keystrokes mean the
     * same physical reading whether the field is labelled MBH or kW.
     */
    private fun HomeUiState.set(field: HomeField, value: String): HomeUiState = when (field) {
        HomeField.MATERIAL -> copy(material = value)
        HomeField.PRESSURE -> copy(pressure = value)
        HomeField.LOAD -> copy(loadMbh = value, canonicalLoadMbh = loadReading(value))
        HomeField.LENGTH -> copy(lengthFeet = value, canonicalLengthFeet = lengthReading(value))
        HomeField.FITTING_QUANTITY -> copy(fittingQuantity = value)
        HomeField.FITTING_LENGTH -> copy(
            fittingLengthFeet = value,
            canonicalFittingLengthFeet = lengthReading(value),
        )
        // Editing the capacity drops the confirmation: a confirmed capacity that no one re-confirmed
        // for the new number would otherwise be compared against the required load.
        HomeField.CAPACITY -> copy(capacity = value, canonicalCapacity = loadReading(value), capacityConfirmed = false)
        HomeField.EDITION -> copy(codeEdition = value)
        HomeField.TABLE -> copy(table = value)
        HomeField.ROW -> copy(row = value)
        HomeField.GAS_SPECIFIC_GRAVITY -> copy(gasSpecificGravity = value)
        HomeField.PRESSURE_DROP -> copy(pressureDropAllowance = value)
        HomeField.PIPE_DIAMETER -> copy(pipeInsideDiameter = value)
        // Choosing a table records the revision of the table that was chosen, so a later edit to that
        // table refuses instead of answering from a revision the case was not read against (D9). A
        // table that is gone records no revision, which is the other `TABLE_CHANGED` case.
        HomeField.SELECTED_TABLE -> copy(
            selectedTableId = value,
            tableDigest = availableTables.firstOrNull { it.id == value }?.contentDigest().orEmpty(),
        )
        HomeField.TABLE_NOMINAL_SIZE -> copy(tableNominalSize = value)
    }

    private fun HomeUiState.loadReading(text: String): Double? =
        canonicalReading(text, unitSystem == UnitSystem.METRIC, Units::kilowattsToMbh)

    private fun HomeUiState.lengthReading(text: String): Double? =
        canonicalReading(text, unitSystem == UnitSystem.METRIC, Units::metersToFeet)

    companion object {
        /** Supplies the on-device stores so a case and the tables it is read against survive process death. */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    HomeViewModel(
                        persistence = DataStoreCasePersistence(applicationContext),
                        tables = DataStoreTablesRepository(applicationContext),
                    )
                }
            }
        }
    }
}

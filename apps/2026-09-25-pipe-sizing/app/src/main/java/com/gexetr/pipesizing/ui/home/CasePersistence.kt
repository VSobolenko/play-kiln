package com.gexetr.pipesizing.ui.home

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Stores the user's case between runs so a sizing case survives process death.
 *
 * Values are plain strings keyed by name rather than enum ordinals, so reordering an enum cannot
 * silently reinterpret a stored case.
 */
interface CasePersistence {
    suspend fun load(): Map<String, String>

    suspend fun save(values: Map<String, String>)

    /** Used when no store is supplied; keeps the state holder usable without a persistence backend. */
    object None : CasePersistence {
        override suspend fun load(): Map<String, String> = emptyMap()

        override suspend fun save(values: Map<String, String>) = Unit
    }
}

private val Context.sizingCaseStore by preferencesDataStore(name = "sizing_case")

class DataStoreCasePersistence(private val context: Context) : CasePersistence {

    override suspend fun load(): Map<String, String> {
        val stored = context.sizingCaseStore.data.first()
        return stored.asMap().entries.mapNotNull { (key, value) ->
            (value as? String)?.let { key.name to it }
        }.toMap()
    }

    override suspend fun save(values: Map<String, String>) {
        context.sizingCaseStore.edit { stored ->
            stored.clear()
            values.forEach { (key, value) -> stored[stringPreferencesKey(key)] = value }
        }
    }
}

internal object CaseKeys {
    const val MATERIAL = "material"
    const val PRESSURE = "pressure"
    const val LOAD = "load"
    const val LENGTH = "length"
    const val FITTING_QUANTITY = "fittingQuantity"
    const val FITTING_LENGTH = "fittingLength"
    const val CAPACITY = "capacity"
    const val METHOD = "method"
    const val UNIT_SYSTEM = "unitSystem"
    const val GAS_SPECIFIC_GRAVITY = "gasSpecificGravity"
    const val PRESSURE_DROP = "pressureDropAllowance"
    const val PIPE_DIAMETER = "pipeInsideDiameter"
    const val PROVENANCE_MODE = "provenanceMode"
    const val CODE_EDITION = "codeEdition"
    const val TABLE = "table"
    const val ROW = "row"
    const val CAPACITY_CONFIRMED = "capacityConfirmed"
}

/**
 * The entered case, without the transient evaluation: a stored result could otherwise be shown as
 * though it had just been computed for inputs the user cannot see.
 */
internal fun HomeUiState.toStoredCase(): Map<String, String> = mapOf(
    CaseKeys.MATERIAL to material,
    CaseKeys.PRESSURE to pressure,
    CaseKeys.LOAD to loadMbh,
    CaseKeys.LENGTH to lengthFeet,
    CaseKeys.FITTING_QUANTITY to fittingQuantity,
    CaseKeys.FITTING_LENGTH to fittingLengthFeet,
    CaseKeys.CAPACITY to capacity,
    CaseKeys.METHOD to method.name,
    CaseKeys.UNIT_SYSTEM to unitSystem.name,
    CaseKeys.GAS_SPECIFIC_GRAVITY to gasSpecificGravity,
    CaseKeys.PRESSURE_DROP to pressureDropAllowance,
    CaseKeys.PIPE_DIAMETER to pipeInsideDiameter,
    CaseKeys.PROVENANCE_MODE to provenanceMode.name,
    CaseKeys.CODE_EDITION to codeEdition,
    CaseKeys.TABLE to table,
    CaseKeys.ROW to row,
    CaseKeys.CAPACITY_CONFIRMED to capacityConfirmed.toString(),
)

/**
 * Rebuilds the entered case from storage. Anything missing or unrecognised keeps its default rather
 * than being guessed, and the evaluation is deliberately left absent.
 */
internal fun HomeUiState.restoredFrom(stored: Map<String, String>): HomeUiState = copy(
    material = stored[CaseKeys.MATERIAL] ?: material,
    pressure = stored[CaseKeys.PRESSURE] ?: pressure,
    loadMbh = stored[CaseKeys.LOAD] ?: loadMbh,
    lengthFeet = stored[CaseKeys.LENGTH] ?: lengthFeet,
    fittingQuantity = stored[CaseKeys.FITTING_QUANTITY] ?: fittingQuantity,
    fittingLengthFeet = stored[CaseKeys.FITTING_LENGTH] ?: fittingLengthFeet,
    capacity = stored[CaseKeys.CAPACITY] ?: capacity,
    method = stored[CaseKeys.METHOD].toEnumOr(method),
    unitSystem = stored[CaseKeys.UNIT_SYSTEM].toEnumOr(unitSystem),
    gasSpecificGravity = stored[CaseKeys.GAS_SPECIFIC_GRAVITY] ?: gasSpecificGravity,
    pressureDropAllowance = stored[CaseKeys.PRESSURE_DROP] ?: pressureDropAllowance,
    pipeInsideDiameter = stored[CaseKeys.PIPE_DIAMETER] ?: pipeInsideDiameter,
    provenanceMode = stored[CaseKeys.PROVENANCE_MODE].toEnumOr(provenanceMode),
    codeEdition = stored[CaseKeys.CODE_EDITION] ?: codeEdition,
    table = stored[CaseKeys.TABLE] ?: table,
    row = stored[CaseKeys.ROW] ?: row,
    capacityConfirmed = stored[CaseKeys.CAPACITY_CONFIRMED]?.toBooleanStrictOrNull() ?: capacityConfirmed,
    evaluation = null,
)

private inline fun <reified T : Enum<T>> String?.toEnumOr(fallback: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback

package com.gexetr.pipesizing.ui.home

import androidx.compose.runtime.Immutable
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.UnitSystem

@Immutable
data class HomeUiState(
    val material: String = "",
    val pressure: String = "",
    val loadMbh: String = "",
    val lengthFeet: String = "",
    val fittingQuantity: String = "",
    val fittingLengthFeet: String = "",
    val capacity: String = "",
    val method: SizingMethod = SizingMethod.CAPACITY_COMPARISON,
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    val gasSpecificGravity: String = "",
    val pressureDropAllowance: String = "",
    val pipeInsideDiameter: String = "",
    val provenanceMode: ProvenanceMode = ProvenanceMode.USER_SUPPLIED,
    val codeEdition: String = "",
    val table: String = "",
    val row: String = "",
    val capacityConfirmed: Boolean = false,
    val evaluation: SizingEvaluation? = null,
)

enum class ProvenanceMode { USER_SUPPLIED, CODE_CITATION }

sealed interface HomeEvent {
    data class FieldChanged(val field: HomeField, val value: String) : HomeEvent
    data class ProvenanceChanged(val mode: ProvenanceMode) : HomeEvent
    data class MethodChanged(val method: SizingMethod) : HomeEvent
    data class UnitChanged(val unitSystem: UnitSystem) : HomeEvent
    data object ConfirmCapacity : HomeEvent
    data object Evaluate : HomeEvent
}

enum class HomeField { MATERIAL, PRESSURE, LOAD, LENGTH, FITTING_QUANTITY, FITTING_LENGTH, CAPACITY, EDITION, TABLE, ROW, GAS_SPECIFIC_GRAVITY, PRESSURE_DROP, PIPE_DIAMETER }

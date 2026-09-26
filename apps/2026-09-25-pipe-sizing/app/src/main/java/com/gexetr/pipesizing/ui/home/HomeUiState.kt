package com.gexetr.pipesizing.ui.home

import androidx.compose.runtime.Immutable
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.UnitSystem
import java.math.BigDecimal
import java.math.RoundingMode

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
    // The quantity fields above are what the user sees and edits, in whichever unit they have
    // selected. These are the same readings held once in the canonical imperial units the domain
    // works in, and they are what the arithmetic runs on and what the fields are re-rendered from
    // when the unit system changes. Keeping them separate is what lets a unit switch both preserve
    // the physical value exactly and display a number short enough to read: re-parsing rounded
    // display text on every toggle would drift a little each time instead.
    // Null means the field is blank; NaN means it is filled but not a number.
    val canonicalLoadMbh: Double? = null,
    val canonicalLengthFeet: Double? = null,
    val canonicalFittingLengthFeet: Double? = null,
    val canonicalCapacity: Double? = null,
)

enum class ProvenanceMode { USER_SUPPLIED, CODE_CITATION }

/**
 * Parses one editable reading into its canonical value. Blank is absent rather than invalid, so the
 * evaluator reports a missing input and names the field instead of reporting nonsense arithmetic on
 * a zero. Text that is present but unparseable stays NaN so the refusal can name it as unusable.
 */
internal fun canonicalReading(text: String, metric: Boolean, metricToCanonical: (Double) -> Double): Double? {
    if (text.isBlank()) return null
    val parsed = text.toDoubleOrNull() ?: return Double.NaN
    return if (metric) metricToCanonical(parsed) else parsed
}

/**
 * Renders a displayed quantity at a precision worth reading. The value behind it keeps full
 * precision, so this only affects the text: a converted load reads 29.3071 rather than
 * 29.307107017222222, and nothing about the case changes.
 */
internal fun readableNumber(value: Double): String =
    BigDecimal(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

sealed interface HomeEvent {
    data class FieldChanged(val field: HomeField, val value: String) : HomeEvent
    data class ProvenanceChanged(val mode: ProvenanceMode) : HomeEvent
    data class MethodChanged(val method: SizingMethod) : HomeEvent
    data class UnitChanged(val unitSystem: UnitSystem) : HomeEvent
    data object ConfirmCapacity : HomeEvent
    data object Evaluate : HomeEvent

    /**
     * Replaces the working case with a stored one. The stored map is the same shape the app already
     * restores on launch, so a loaded case and a relaunch cannot diverge.
     */
    data class CaseLoaded(val values: Map<String, String>) : HomeEvent
}

enum class HomeField { MATERIAL, PRESSURE, LOAD, LENGTH, FITTING_QUANTITY, FITTING_LENGTH, CAPACITY, EDITION, TABLE, ROW, GAS_SPECIFIC_GRAVITY, PRESSURE_DROP, PIPE_DIAMETER }

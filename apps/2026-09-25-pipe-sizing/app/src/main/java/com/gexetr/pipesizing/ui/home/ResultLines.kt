package com.gexetr.pipesizing.ui.home

import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.Units
import java.util.Locale

/** One line of a computed result: a string resource and the arguments it is formatted with. */
internal data class ResultLine(val labelRes: Int, val args: List<Any> = emptyList())

/**
 * The lines shown for a computed result.
 *
 * Kept out of the composable so the set of lines - which is what lets a number be read in the
 * context it was computed for rather than detached from it - can be asserted in a JVM test. The
 * result carries the case, the method and the compared capacity before it carries any figure.
 */
internal fun computedResultLines(
    state: HomeUiState,
    result: SizingEvaluation.Computed,
    metric: Boolean,
    methodLabel: String,
): List<ResultLine> = buildList {
    add(ResultLine(R.string.result_case_label, listOf(state.material, state.pressure)))
    add(ResultLine(R.string.result_method_label, listOf(methodLabel)))
    val capacity = state.capacity.toDoubleOrNull() ?: Double.NaN
    add(
        if (metric) {
            ResultLine(R.string.result_capacity_metric, listOf(display(capacity)))
        } else {
            ResultLine(R.string.result_capacity_imperial, listOf(display(capacity)))
        },
    )
    if (state.method == SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS) {
        add(
            ResultLine(
                R.string.result_physical_inputs,
                listOf(state.gasSpecificGravity, state.pressureDropAllowance, state.pipeInsideDiameter),
            ),
        )
    }
    if (metric) {
        add(ResultLine(R.string.result_load_metric, listOf(display(Units.mbhToKilowatts(result.requiredLoadMbh)))))
        add(ResultLine(R.string.result_length_metric, listOf(display(Units.feetToMeters(result.totalLengthFeet)))))
        add(ResultLine(R.string.result_fitting_length_metric, listOf(display(Units.feetToMeters(result.fittingAllowanceFeet)))))
    } else {
        add(ResultLine(R.string.result_load_imperial, listOf(display(result.requiredLoadMbh))))
        add(ResultLine(R.string.result_length_imperial, listOf(display(result.totalLengthFeet))))
        add(ResultLine(R.string.result_fitting_length_imperial, listOf(display(result.fittingAllowanceFeet))))
    }
    add(ResultLine(if (result.supportsRequiredLoad) R.string.capacity_supports else R.string.capacity_does_not_support))
    add(ResultLine(R.string.provenance_result, listOf(result.capacityProvenance.toString())))
}

/** Display rounding only; the domain keeps full precision, so this never changes a physical value. */
internal fun display(value: Double): String = String.format(Locale.US, "%.2f", value)

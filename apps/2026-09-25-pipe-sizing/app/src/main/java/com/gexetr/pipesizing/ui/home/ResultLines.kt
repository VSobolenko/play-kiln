package com.gexetr.pipesizing.ui.home

import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.Units
import java.util.Locale

/** One line of a computed result: a string resource and the arguments it is formatted with. */
internal data class ResultLine(val labelRes: Int, val args: List<Any> = emptyList())

/**
 * The label of the selected method. One definition, because the method control, the result card and
 * the shared report all render it and four methods no longer fit an if/else over two.
 */
internal fun SizingMethod.labelRes(): Int = when (this) {
    SizingMethod.CAPACITY_COMPARISON -> R.string.method_capacity
    SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS -> R.string.method_physical
    SizingMethod.RETURN_FROM_USER_TABLE -> R.string.method_return_from_table
    SizingMethod.SMALLEST_SIZE_FOR_LOAD -> R.string.method_smallest_size
}

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
    // The compared capacity is reported from the canonical reading, so it is shown in the unit the
    // user selected no matter which unit it was entered in.
    val capacityMbh = state.canonicalCapacity ?: Double.NaN
    add(
        if (metric) {
            ResultLine(R.string.result_capacity_metric, listOf(display(Units.mbhToKilowatts(capacityMbh))))
        } else {
            ResultLine(R.string.result_capacity_imperial, listOf(display(capacityMbh)))
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

/**
 * The lines shown for a value returned out of the user's own recorded table.
 *
 * Same context-first order as a computed result and deliberately *not* the same vocabulary: there is
 * no "compared capacity", no supports/does-not-support verdict and no capacity-provenance line,
 * because the app compared nothing here — it read a row the user recorded and shows it back, named
 * (design D10). The value is reported from the row's canonical MBH and converted for display only.
 *
 * [tableLabel] is the name the user gave the table, resolved by the caller so this stays free of a
 * Context; the table's opaque id is an internal identity and is not a citation.
 */
internal fun returnedTableResultLines(
    state: HomeUiState,
    result: SizingEvaluation.ReturnedFromUserTable,
    metric: Boolean,
    methodLabel: String,
    tableLabel: String,
): List<ResultLine> = buildList {
    add(ResultLine(R.string.result_case_label, listOf(state.material, state.pressure)))
    add(ResultLine(R.string.result_method_label, listOf(methodLabel)))
    add(
        if (metric) {
            ResultLine(
                R.string.returned_table_capacity_metric,
                listOf(display(Units.mbhToKilowatts(result.returnedCapacityMbh))),
            )
        } else {
            ResultLine(R.string.returned_table_capacity_imperial, listOf(display(result.returnedCapacityMbh)))
        },
    )
    add(ResultLine(R.string.returned_table_row, listOf(result.returnedSize, result.rowLabel)))
    // A substitution is shown only when one happened, so its absence is the record that the case's
    // own length was the recorded one.
    result.substitution?.let { substitution ->
        add(
            if (metric) {
                ResultLine(
                    R.string.returned_table_length_substitution_metric,
                    listOf(
                        display(Units.feetToMeters(substitution.enteredLengthFeet)),
                        display(Units.feetToMeters(substitution.usedLengthFeet)),
                    ),
                )
            } else {
                ResultLine(
                    R.string.returned_table_length_substitution_imperial,
                    listOf(display(substitution.enteredLengthFeet), display(substitution.usedLengthFeet)),
                )
            },
        )
    }
    add(
        ResultLine(
            R.string.returned_table_citation,
            listOf(
                tableLabel,
                result.capacityProvenance.codeAndEdition,
                result.capacityProvenance.tableIdentifier,
            ),
        ),
    )
    // The literal marker is rendered from the shared constant, never from hand-written copy, so the
    // label the user sees cannot drift from the one the entity requires (design D2).
    add(ResultLine(R.string.returned_table_user_recorded, listOf(USER_RECORDED_MARKER)))
    add(ResultLine(R.string.returned_table_unverified))
}

/** Display rounding only; the domain keeps full precision, so this never changes a physical value. */
internal fun display(value: Double): String = String.format(Locale.US, "%.2f", value)

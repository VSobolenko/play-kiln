package com.gexetr.pipesizing.ui.home

import android.content.Context
import android.content.Intent
import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.UnitSystem

/**
 * The shareable text form of the case that was actually computed.
 *
 * It reports the readings and the citation beside every figure, because a number in a chat message
 * has less context than the same number on screen: whoever receives it must be able to see which
 * method and which code edition produced it.
 */
internal fun buildCaseReport(
    context: Context,
    state: HomeUiState,
    computed: SizingEvaluation.Computed,
    isPro: Boolean,
): String {
    val metric = state.unitSystem == UnitSystem.METRIC
    val methodLabel = context.getString(
        if (state.method == SizingMethod.CAPACITY_COMPARISON) R.string.method_capacity else R.string.method_physical,
    )

    fun quantity(value: Double, imperialRes: Int, metricRes: Int): String =
        if (metric) {
            context.getString(metricRes, display(value))
        } else {
            context.getString(imperialRes, display(value))
        }

    val lines = buildList {
        add(context.getString(R.string.app_name))
        add("")
        add(context.getString(R.string.result_case_label, state.material, state.pressure))
        add(context.getString(R.string.result_method_label, methodLabel))
        add(context.getString(if (metric) R.string.units_metric else R.string.units_imperial))
        add("")
        add(context.getString(R.string.section_readings))
        add(context.getString(if (metric) R.string.load_label_metric else R.string.load_label) + ": " + state.loadMbh)
        add(context.getString(if (metric) R.string.length_label_metric else R.string.length_label) + ": " + state.lengthFeet)
        if (state.fittingQuantity.isNotBlank()) {
            add(context.getString(R.string.fitting_quantity_label) + ": " + state.fittingQuantity)
            add(context.getString(if (metric) R.string.fitting_length_label_metric else R.string.fitting_length_label) + ": " + state.fittingLengthFeet)
        }
        add(context.getString(if (metric) R.string.capacity_label_metric else R.string.capacity_label) + ": " + state.capacity)
        if (state.method == SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS) {
            add(context.getString(R.string.result_physical_inputs, state.gasSpecificGravity, state.pressureDropAllowance, state.pipeInsideDiameter))
        }
        if (state.provenanceMode == ProvenanceMode.CODE_CITATION) {
            add(context.getString(R.string.edition_label) + ": " + state.codeEdition)
            add(context.getString(R.string.table_label) + ": " + state.table)
            if (state.row.isNotBlank()) add(context.getString(R.string.row_label) + ": " + state.row)
        }
        add("")
        add(context.getString(R.string.result_title))
        add(quantity(computed.requiredLoadMbh, R.string.result_load_imperial, R.string.result_load_metric))
        add(quantity(computed.totalLengthFeet, R.string.result_length_imperial, R.string.result_length_metric))
        add(quantity(computed.fittingAllowanceFeet, R.string.result_fitting_length_imperial, R.string.result_fitting_length_metric))
        add(
            quantity(
                state.canonicalCapacity ?: Double.NaN,
                R.string.result_capacity_imperial,
                R.string.result_capacity_metric,
            ),
        )
        add(
            context.getString(
                if (computed.supportsRequiredLoad) R.string.capacity_supports else R.string.capacity_does_not_support,
            ),
        )
        add(context.getString(R.string.provenance_result, computed.capacityProvenance.toString()))
        add("")
        add(context.getString(R.string.disclaimer))
        add(context.getString(if (isPro) R.string.report_pro_line else R.string.report_free_line))
    }
    return lines.joinToString("\n")
}

/**
 * Opens the platform share sheet with [report]. `createChooser` is used so the user's choice is
 * explicit and the app never claims a default target it does not have.
 */
internal fun shareCaseReport(context: Context, report: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, report)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.report_share_chooser)))
}

package com.gexetr.pipesizing.ui.home

import android.content.Context
import android.content.Intent
import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UnitSystem
import com.gexetr.pipesizing.sizing.Units

/**
 * One line of a case report.
 *
 * The report is assembled as an ordered list of lines and only rendered through a `Context`
 * afterwards, so the *set and order* of what a report says is asserted in a JVM test rather than
 * read off a shared-chat screenshot - the same reason `ResultLines` exists. A case report is the
 * app's only outgoing text, so what it contains is worth a sensor.
 */
internal sealed interface ReportLine {
    /** A blank separator, so the report's sections stay apart. */
    data object Blank : ReportLine

    /** One string resource with the arguments it is formatted with. */
    data class Text(val stringRes: Int, val args: List<Any> = emptyList()) : ReportLine

    /**
     * A resource whose single argument is another resource, resolved first. The method label is a
     * label rather than a reading, so it is named by its own resource and the report stays free of a
     * `Context` while it is being assembled.
     */
    data class OfResource(val stringRes: Int, val argRes: Int) : ReportLine

    /** A resource used as a label for a value the user typed, which needs no resource of its own. */
    data class Labelled(val labelRes: Int, val value: String) : ReportLine
}

internal fun renderReport(context: Context, lines: List<ReportLine>): String = lines.joinToString("\n") { line ->
    when (line) {
        ReportLine.Blank -> ""
        is ReportLine.Text -> context.getString(line.stringRes, *line.args.toTypedArray())
        is ReportLine.OfResource -> context.getString(line.stringRes, context.getString(line.argRes))
        is ReportLine.Labelled -> context.getString(line.labelRes) + ": " + line.value
    }
}

/**
 * The one report decision: which report the result on screen produces, or none.
 *
 * It is Context-free so that the *choice* — returned-from-table versus arithmetic versus nothing —
 * is asserted in a JVM test rather than read off a shared-chat screenshot, and [buildReportFor] is
 * the same decision with the rendering attached. Task 5.1 exists because the returned-from-table
 * report was implemented but no screen could reach it: this switch is the route, and every
 * `SizingEvaluation` member is listed so a new one cannot fall through it.
 *
 * [unnamedTableLabel] is the caller's label for a table the case names but which is no longer
 * recorded; passed in rather than resolved here so that this decision needs no `Context`.
 */
internal fun reportLinesFor(
    state: HomeUiState,
    requested: SizingEvaluation?,
    isPro: Boolean,
    unnamedTableLabel: String,
): List<ReportLine>? = when (requested) {
    null -> null
    is SizingEvaluation.Computed -> computedReportLines(state, requested, isPro)
    is SizingEvaluation.ReturnedFromUserTable -> returnedTableReportLines(
        state = state,
        returned = requested,
        isPro = isPro,
        // The user's own name for the table, or the caller's label when it is blank or gone.
        tableLabel = state.tableNameOrNull(requested.capacityProvenance.tableId) ?: unnamedTableLabel,
    )
    is SizingEvaluation.Refused -> null
}

/**
 * Whether the report action is offered for the result on screen.
 *
 * This is the `enabled` state of the report control, so it is deliberately Context-free. It is the
 * same three-way decision as [reportLinesFor]; both list every member, so the compiler keeps the
 * button and the report in step.
 */
internal fun SizingEvaluation?.hasShareableReport(): Boolean = when (this) {
    null -> false
    is SizingEvaluation.Computed -> true
    is SizingEvaluation.ReturnedFromUserTable -> true
    is SizingEvaluation.Refused -> false
}

/**
 * The rendered report the share action sends for whatever is on screen, or null when there is
 * nothing to report. `PlayKilnApp` is the only caller, and it is how a returned-from-table result
 * reaches its own report instead of the "a report needs a computed result" message.
 */
internal fun buildReportFor(context: Context, state: HomeUiState, isPro: Boolean): String? =
    reportLinesFor(
        state = state,
        requested = state.evaluation,
        isPro = isPro,
        unnamedTableLabel = context.getString(R.string.tables_unnamed),
    )?.let { lines -> renderReport(context, lines) }

/** The name the user gave a recorded table, or null when it is blank or no longer recorded. */
internal fun HomeUiState.tableNameOrNull(tableId: String): String? =
    availableTables.firstOrNull { it.id == tableId }?.name?.takeIf { it.isNotBlank() }

/**
 * The head every report opens with: the app, the case, the method, the unit system and the readings.
 * It is shared so a returned value's report cannot drift into opening with a figure, and so the
 * reading lines are defined once.
 */
private fun reportContextLines(state: HomeUiState, metric: Boolean, methodRes: Int): List<ReportLine> =
    buildList {
        add(ReportLine.Text(R.string.app_name))
        add(ReportLine.Blank)
        add(ReportLine.Text(R.string.result_case_label, listOf(state.material, state.pressure)))
        add(ReportLine.OfResource(R.string.result_method_label, methodRes))
        add(ReportLine.Text(if (metric) R.string.units_metric else R.string.units_imperial))
        add(ReportLine.Blank)
        add(ReportLine.Text(R.string.section_readings))
        add(ReportLine.Labelled(if (metric) R.string.load_label_metric else R.string.load_label, state.loadMbh))
        add(ReportLine.Labelled(if (metric) R.string.length_label_metric else R.string.length_label, state.lengthFeet))
        if (state.fittingQuantity.isNotBlank()) {
            add(ReportLine.Labelled(R.string.fitting_quantity_label, state.fittingQuantity))
            add(
                ReportLine.Labelled(
                    if (metric) R.string.fitting_length_label_metric else R.string.fitting_length_label,
                    state.fittingLengthFeet,
                ),
            )
        }
    }

internal fun computedReportLines(
    state: HomeUiState,
    computed: SizingEvaluation.Computed,
    isPro: Boolean,
): List<ReportLine> {
    val metric = state.unitSystem == UnitSystem.METRIC
    return buildList {
        addAll(reportContextLines(state, metric, state.method.labelRes()))
        add(
            ReportLine.Labelled(
                if (metric) R.string.capacity_label_metric else R.string.capacity_label,
                state.capacity,
            ),
        )
        if (state.method == SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS) {
            add(
                ReportLine.Text(
                    R.string.result_physical_inputs,
                    listOf(state.gasSpecificGravity, state.pressureDropAllowance, state.pipeInsideDiameter),
                ),
            )
        }
        if (state.provenanceMode == ProvenanceMode.CODE_CITATION) {
            add(ReportLine.Labelled(R.string.edition_label, state.codeEdition))
            add(ReportLine.Labelled(R.string.table_label, state.table))
            if (state.row.isNotBlank()) add(ReportLine.Labelled(R.string.row_label, state.row))
        }
        add(ReportLine.Blank)
        add(ReportLine.Text(R.string.result_title))
        add(
            resultQuantity(
                computed.requiredLoadMbh,
                metric,
                R.string.result_load_imperial,
                R.string.result_load_metric,
                Units::mbhToKilowatts,
            ),
        )
        add(
            resultQuantity(
                computed.totalLengthFeet,
                metric,
                R.string.result_length_imperial,
                R.string.result_length_metric,
                Units::feetToMeters,
            ),
        )
        add(
            resultQuantity(
                computed.fittingAllowanceFeet,
                metric,
                R.string.result_fitting_length_imperial,
                R.string.result_fitting_length_metric,
                Units::feetToMeters,
            ),
        )
        add(
            resultQuantity(
                state.canonicalCapacity ?: Double.NaN,
                metric,
                R.string.result_capacity_imperial,
                R.string.result_capacity_metric,
                Units::mbhToKilowatts,
            ),
        )
        add(
            ReportLine.Text(
                if (computed.supportsRequiredLoad) R.string.capacity_supports else R.string.capacity_does_not_support,
            ),
        )
        add(ReportLine.Text(R.string.provenance_result, listOf(computed.capacityProvenance.toString())))
        add(ReportLine.Blank)
        add(ReportLine.Text(R.string.disclaimer))
        add(ReportLine.Text(if (isPro) R.string.report_pro_line else R.string.report_free_line))
    }
}

internal fun returnedTableReportLines(
    state: HomeUiState,
    returned: SizingEvaluation.ReturnedFromUserTable,
    isPro: Boolean,
    tableLabel: String,
): List<ReportLine> {
    val metric = state.unitSystem == UnitSystem.METRIC
    return buildList {
        addAll(reportContextLines(state, metric, state.method.labelRes()))
        add(ReportLine.Blank)
        add(ReportLine.Text(R.string.returned_table_result_title))
        add(
            resultQuantity(
                returned.returnedCapacityMbh,
                metric,
                R.string.returned_table_capacity_imperial,
                R.string.returned_table_capacity_metric,
                Units::mbhToKilowatts,
            ),
        )
        add(ReportLine.Text(R.string.returned_table_row, listOf(returned.returnedSize, returned.rowLabel)))
        returned.substitution?.let { substitution ->
            add(
                ReportLine.Text(
                    if (metric) {
                        R.string.returned_table_length_substitution_metric
                    } else {
                        R.string.returned_table_length_substitution_imperial
                    },
                    listOf(
                        display(shownLength(substitution.enteredLengthFeet, metric)),
                        display(shownLength(substitution.usedLengthFeet, metric)),
                    ),
                ),
            )
        }
        add(
            ReportLine.Text(
                R.string.returned_table_citation,
                listOf(tableLabel, returned.capacityProvenance.codeAndEdition, returned.capacityProvenance.tableIdentifier),
            ),
        )
        add(ReportLine.Text(R.string.returned_table_user_recorded, listOf(USER_RECORDED_MARKER)))
        add(ReportLine.Text(R.string.returned_table_unverified))
        add(ReportLine.Blank)
        add(ReportLine.Text(R.string.disclaimer))
        add(ReportLine.Text(if (isPro) R.string.report_pro_line else R.string.report_free_line))
    }
}

/**
 * One result figure in the unit the case is shown in. The canonical value is converted here rather
 * than labelled with the other unit's resource, which is how a metric report would otherwise print
 * MBH figures as kW.
 */
private fun resultQuantity(
    canonicalValue: Double,
    metric: Boolean,
    imperialRes: Int,
    metricRes: Int,
    canonicalToMetric: (Double) -> Double,
): ReportLine.Text = ReportLine.Text(
    if (metric) metricRes else imperialRes,
    listOf(display(if (metric) canonicalToMetric(canonicalValue) else canonicalValue)),
)

/** One of the two lengths of a substitution, in the unit the case is shown in. */
private fun shownLength(canonicalFeet: Double, metric: Boolean): Double =
    if (metric) Units.feetToMeters(canonicalFeet) else canonicalFeet

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

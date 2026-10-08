package com.gexetr.pipesizing.sizing

/**
 * User-recorded lookup data only. The app owns no capacity-table data; see
 * apps/2026-09-25-pipe-sizing/docs/2026-09-25-pipe-sizing-code-source.md.
 */
const val USER_RECORDED_MARKER: String = "user-recorded"

data class TableApplicability(
    val fuel: String,
    val materialOrSystem: String,
    val inletPressureClass: String,
    val permittedPressureDrop: String,
    val gasSpecificGravity: String,
)

data class TableCitation(
    val codeAndEdition: String,
    val tableIdentifier: String,
)

data class TableRow(
    val nominalSize: String,
    val insideDiameter: Double?,
    val lengthFeet: Double,
    val capacityMbh: Double,
) {
    /** D1 does not define a row-label field; the recorded nominal size identifies the used row. */
    val label: String get() = nominalSize
}

data class UserCapacityTable(
    val id: String,
    val name: String,
    val applicability: TableApplicability,
    val citation: TableCitation,
    val marker: String,
    val rows: List<TableRow>,
)

/** The ordering is solely the first-occurrence order in rows the user recorded. */
fun UserCapacityTable.derivedSizeOrder(): List<String> = rows
    .map { it.nominalSize.trim() }
    .filter { it.isNotEmpty() }
    .distinctBy { it.lowercase() }

/** Returns the shared refusal that prevents a lookup from using an incomplete table. */
fun UserCapacityTable.validationRefusal(): SizingEvaluation.Refused? {
    fun incomplete(problem: String) = SizingEvaluation.Refused(RefusalKind.TABLE_INCOMPLETE, "Complete $problem in the table.")

    if (applicability.fuel.isBlank()) return incomplete("the fuel")
    if (applicability.materialOrSystem.isBlank()) return incomplete("the material or system")
    if (applicability.inletPressureClass.isBlank()) return incomplete("the inlet pressure class")
    if (applicability.permittedPressureDrop.isBlank()) return incomplete("the permitted pressure drop")
    if (applicability.gasSpecificGravity.isBlank()) return incomplete("the gas specific gravity")
    if (citation.codeAndEdition.isBlank()) return incomplete("the citation code and edition")
    if (citation.tableIdentifier.isBlank()) return incomplete("the citation table identifier")
    if (marker != USER_RECORDED_MARKER) return incomplete("the user-recorded marker")
    if (rows.isEmpty()) return incomplete("at least one row")
    if (rows.any { it.nominalSize.isBlank() || !it.lengthFeet.isFinite() || !it.capacityMbh.isFinite() }) {
        return incomplete("every row's size, length, and capacity")
    }
    if (rows.any { it.insideDiameter != null && !it.insideDiameter.isFinite() }) return incomplete("the row inside diameter")
    if (rows.groupBy { it.nominalSize.trim().lowercase() to it.lengthFeet }.any { it.value.size > 1 }) {
        return incomplete("the duplicate size and length")
    }

    val sizeDiameters = rows.groupBy { it.nominalSize.trim().lowercase() }
        .values
        .mapNotNull { group -> group.firstNotNullOfOrNull { it.insideDiameter } }
    if (sizeDiameters.zipWithNext().any { (first, second) -> second < first }) {
        return SizingEvaluation.Refused(
            RefusalKind.SIZE_ORDER_INDETERMINATE,
            "Correct the recorded size order and inside diameters that disagree.",
        )
    }
    return null
}

package com.gexetr.pipesizing.sizing

/** Looks up only rows the user recorded; it never calculates a capacity. */
internal fun evaluateCaseAgainstUserTable(case: SizingCase, table: UserCapacityTable?): SizingEvaluation {
    if (table == null || case.tableSelection.tableId.isNullOrBlank()) return noTableSelected()
    applicabilityRefusal(case, table)?.let { return it }
    table.validationRefusal()?.let { return it }

    // 2.3.1 — every SizingMethod member is listed so the compiler keeps checking this dispatch.
    // An `else` would compile today and turn a future enum member into a runtime crash instead of
    // a compile error.
    return when (case.method) {
        SizingMethod.RETURN_FROM_USER_TABLE -> returnCapacityFromUserTable(case, table)
        SizingMethod.SMALLEST_SIZE_FOR_LOAD -> smallestSizeCarryingLoad(case, table)
        // The two arithmetic methods never reach the table engine: SizingEvaluator.evaluate routes
        // them to evaluateArithmetically. These branches exist to keep the exhaustiveness check.
        SizingMethod.CAPACITY_COMPARISON,
        SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS ->
            error("Table lookup was dispatched for a non-table method.")
    }
}

private fun returnCapacityFromUserTable(case: SizingCase, table: UserCapacityTable): SizingEvaluation {
    val size = case.tableSelection.nominalSize?.takeIf { it.isNotBlank() }
        ?: return refused(RefusalKind.NOMINAL_SIZE_MISSING, "Enter the nominal size you are reading.")
    val rows = rowsForSize(table, size)
    if (rows.isEmpty()) {
        return refused(
            RefusalKind.SIZE_NOT_IN_TABLE,
            "Record $size in the table; recorded sizes are ${table.derivedSizeOrder().joinToString()}.",
        )
    }
    val totalLength = case.totalLengthFeet()
    val used = rowForLength(rows, totalLength)
        ?: return lengthAboveRows(rows)
    return returned(case, table, size, used, totalLength)
}

private fun smallestSizeCarryingLoad(case: SizingCase, table: UserCapacityTable): SizingEvaluation {
    val totalLength = case.totalLengthFeet()
    // 2.3.4 — a size whose own recorded lengths stop short of the case is recorded against that
    // size, so a refusal names the size whose limit was hit rather than another size's length.
    val candidates = mutableListOf<Pair<String, TableRow>>()
    val shortfalls = mutableListOf<Pair<String, Double>>()
    for (size in table.derivedSizeOrder()) {
        val rows = rowsForSize(table, size)
        val used = rowForLength(rows, totalLength)
        if (used == null) shortfalls += size to rows.maxOf { it.lengthFeet } else candidates += size to used
    }
    if (candidates.isEmpty()) {
        val limits = shortfalls.joinToString(", ") { (size, longest) ->
            "$size (longest recorded $longest)"
        }
        return refused(
            RefusalKind.LENGTH_ABOVE_EVERY_ENTERED_ROW,
            "Record a length at or above $totalLength for $limits.",
        )
    }
    val carrying = candidates.firstOrNull { (_, row) -> row.capacityMbh >= case.requiredLoadMbh() }
    if (carrying == null) {
        val largest = candidates.maxByOrNull { (_, row) -> row.capacityMbh }!!
        return refused(
            RefusalKind.LOAD_ABOVE_EVERY_ENTERED_CAPACITY,
            "Record a capacity above ${largest.second.capacityMbh} for ${largest.first}.",
        )
    }
    return returned(case, table, carrying.first, carrying.second, totalLength)
}

private fun returned(
    case: SizingCase,
    table: UserCapacityTable,
    size: String,
    used: TableRow,
    totalLength: Double,
) = SizingEvaluation.ReturnedFromUserTable(
    requiredLoadMbh = case.requiredLoadMbh(),
    totalLengthFeet = totalLength,
    returnedCapacityMbh = used.capacityMbh,
    returnedSize = size,
    rowLabel = used.label,
    substitution = if (used.lengthFeet == totalLength) null else LengthSubstitution(totalLength, used.lengthFeet),
    capacityProvenance = UserProvenance.RecordedTableCitation(
        table.id,
        table.citation.codeAndEdition,
        table.citation.tableIdentifier,
    ),
)

private fun applicabilityRefusal(case: SizingCase, table: UserCapacityTable): SizingEvaluation.Refused? {
    val differences = mutableListOf<String>()
    if (!sameRecordedText(case.materialOrSystem, table.applicability.materialOrSystem)) {
        differences += "material or system (${case.materialOrSystem} / ${table.applicability.materialOrSystem})"
    }
    if (!sameRecordedText(case.pressureClass, table.applicability.inletPressureClass)) {
        differences += "inlet pressure class (${case.pressureClass} / ${table.applicability.inletPressureClass})"
    }
    val tableGravity = table.applicability.gasSpecificGravity.toDoubleOrNull()
        ?: return refused(RefusalKind.TABLE_INCOMPLETE, "Complete the table gas specific gravity.")
    // 4.9 — an absent reading and an unusable one are two conditions and refuse as two kinds: a
    // blank field is not out of range, and telling the user to "correct" a value they never entered
    // sends them to fix the wrong thing. This is the §2.3.4 defect class ("refused by the limit it
    // hit") in the applicability check.
    val caseGravity = case.gasSpecificGravity
        ?: return refused(RefusalKind.MISSING_INPUT, "Enter the case gas specific gravity to compare.")
    if (!caseGravity.value.isFinite()) {
        return refused(RefusalKind.OUT_OF_RANGE, "Correct the out-of-range gas specific gravity.")
    }
    if (caseGravity.value != tableGravity) differences += "gas specific gravity (${caseGravity.value} / $tableGravity)"
    return differences.takeIf { it.isNotEmpty() }?.let {
        refused(RefusalKind.APPLICABILITY_MISMATCH, "Correct applicability: ${it.joinToString()}.")
    }
}

private fun rowsForSize(table: UserCapacityTable, size: String): List<TableRow> =
    table.rows.filter { sameRecordedText(it.nominalSize, size) }

private fun rowForLength(rows: List<TableRow>, length: Double): TableRow? =
    rows.firstOrNull { it.lengthFeet == length }
        ?: rows.filter { it.lengthFeet > length }.minByOrNull { it.lengthFeet }

private fun lengthAboveRows(rows: List<TableRow>): SizingEvaluation.Refused {
    val longest = rows.maxByOrNull { it.lengthFeet }!!
    return refused(
        RefusalKind.LENGTH_ABOVE_EVERY_ENTERED_ROW,
        "Record a length at least ${longest.lengthFeet} for ${longest.nominalSize}.",
    )
}

// The load and total-length sums live once in `SizingDomain.kt`; both this lookup and the
// arithmetic evaluator call them, so the two cannot drift apart.
private fun sameRecordedText(first: String, second: String): Boolean = first.trim().equals(second.trim(), ignoreCase = true)
private fun noTableSelected() = refused(RefusalKind.NO_TABLE_SELECTED, "Select the table you recorded.")
private fun refused(kind: RefusalKind, correction: String) = SizingEvaluation.Refused(kind, correction)

package com.gexetr.pipesizing.sizing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapacityTableLookupTest {
    @Test
    fun exactRowReturnsItsRecordedCapacityAndNamesTheRow() {
        val row = syntheticRow("synthetic-size-a", 40.0, 777.125)

        val result = evaluateCaseAgainstUserTable(forwardCase(length = 40.0), table(rows = listOf(row)))

        val returned = assertReturned(result)
        assertEquals(row.capacityMbh, returned.returnedCapacityMbh, 0.0)
        assertEquals(row.label, returned.rowLabel)
        assertEquals(row.nominalSize, returned.returnedSize)
        assertNull(returned.substitution)
    }

    @Test
    fun betweenLengthsUsesClosestLargerRowAndNeverTheShorterCapacity() {
        val shorter = syntheticRow("synthetic-size-a", 30.0, 888.25)
        val larger = syntheticRow("synthetic-size-a", 50.0, 666.5)

        val result = evaluateCaseAgainstUserTable(forwardCase(length = 40.0), table(rows = listOf(shorter, larger)))

        val returned = assertReturned(result)
        assertEquals(larger.capacityMbh, returned.returnedCapacityMbh, 0.0)
        assertFalse(returned.returnedCapacityMbh == shorter.capacityMbh)
        assertEquals(LengthSubstitution(40.0, larger.lengthFeet), returned.substitution)
    }

    @Test
    fun reverseLookupReturnsFirstRecordedSizeThatCarriesLoadWithoutInventingCapacity() {
        val first = syntheticRow("synthetic-size-a", 50.0, 400.0)
        val carrying = syntheticRow("synthetic-size-b", 50.0, 900.75)
        val laterCarrying = syntheticRow("synthetic-size-c", 50.0, 1200.5)

        val result = evaluateCaseAgainstUserTable(
            reverseCase(length = 50.0, load = 500.0),
            table(rows = listOf(first, carrying, laterCarrying)),
        )

        val returned = assertReturned(result)
        assertEquals(carrying.nominalSize, returned.returnedSize)
        assertEquals(carrying.capacityMbh, returned.returnedCapacityMbh, 0.0)
        assertFalse(returned.returnedCapacityMbh in (first.capacityMbh + 0.0001)..(carrying.capacityMbh - 0.0001))
    }

    @Test
    fun refusalKindsNameTheRelevantFieldOrLimit() {
        assertRefusal(
            evaluateCaseAgainstUserTable(forwardCase(), null),
            RefusalKind.NO_TABLE_SELECTED,
            "table",
        )
        assertRefusal(
            evaluateCaseAgainstUserTable(forwardCase(), table(rows = emptyList())),
            RefusalKind.TABLE_INCOMPLETE,
            "row",
        )
        assertRefusal(
            evaluateCaseAgainstUserTable(forwardCase(), table(applicability = applicability().copy(materialOrSystem = "other"))),
            RefusalKind.APPLICABILITY_MISMATCH,
            "material",
        )
        assertRefusal(
            evaluateCaseAgainstUserTable(forwardCase(size = "missing-size"), table()),
            RefusalKind.SIZE_NOT_IN_TABLE,
            "missing-size",
        )
        assertRefusal(
            evaluateCaseAgainstUserTable(forwardCase(size = null), table()),
            RefusalKind.NOMINAL_SIZE_MISSING,
            "nominal size",
        )
        assertRefusal(
            evaluateCaseAgainstUserTable(forwardCase(length = 60.0), table()),
            RefusalKind.LENGTH_ABOVE_EVERY_ENTERED_ROW,
            "50.0",
        )
        assertRefusal(
            evaluateCaseAgainstUserTable(reverseCase(length = 50.0, load = 9999.0), table()),
            RefusalKind.LOAD_ABOVE_EVERY_ENTERED_CAPACITY,
            "synthetic-size-a",
        )
        assertRefusal(
            evaluateCaseAgainstUserTable(
                reverseCase(),
                table(rows = listOf(syntheticRow("synthetic-size-a", 50.0, 500.0, 2.0), syntheticRow("synthetic-size-b", 50.0, 600.0, 1.0))),
            ),
            RefusalKind.SIZE_ORDER_INDETERMINATE,
            "diameter",
        )
    }

    @Test
    fun aBlankCaseGravityRefusesAsAMissingInputRatherThanOutOfRange() {
        // 4.9 — an absent reading is not an out-of-range one. A blank gravity must be reported as a
        // missing input so the user is sent to fill it in, not to "correct" a value they never
        // entered (the §2.3.4 defect class, in the applicability check).
        val refusal = assertRefusalKind(
            evaluateCaseAgainstUserTable(
                forwardCase().copy(gasSpecificGravity = null),
                table(),
            ),
            RefusalKind.MISSING_INPUT,
        )

        assertTrue(refusal.correctionRequired.contains("gas specific gravity", ignoreCase = true))
    }

    @Test
    fun anUnparseableCaseGravityStillRefusesAsOutOfRange() {
        // The other half of 4.9: text that is present but unusable is an out-of-range reading, not a
        // missing one. Keeping the two distinct is the whole point of the pair.
        val refusal = assertRefusalKind(
            evaluateCaseAgainstUserTable(
                forwardCase().copy(gasSpecificGravity = SourcedValue(Double.NaN, UserProvenance.UserSupplied)),
                table(),
            ),
            RefusalKind.OUT_OF_RANGE,
        )

        assertTrue(refusal.correctionRequired.contains("gas specific gravity", ignoreCase = true))
    }

    @Test
    fun theTableDispatchDecidesEverySizingMethodExplicitly() {
        // 2.3.1: `when (case.method)` lists every enum member, with no `else`. Walking
        // `SizingMethod.entries` exercises every member, including one added later; the exhaustive
        // `when` is what makes a member without a branch a compile error instead of a runtime crash.
        for (method in SizingMethod.entries) {
            val outcome = runCatching {
                evaluateCaseAgainstUserTable(
                    case(method, length = 50.0, load = 10.0, size = "synthetic-size-a"),
                    table(),
                )
            }
            if (method.returnsFromUserTable) {
                assertTrue(
                    "$method must be answered by the table engine",
                    outcome.getOrNull() is SizingEvaluation.ReturnedFromUserTable,
                )
            } else {
                assertTrue("$method must never be answered by the table engine", outcome.isFailure)
            }
        }
    }

    @Test
    fun theTableLookupSharesTheArithmeticLoadAndLengthSums() {
        // 2.3.3: the load and total-length sums have one definition, not two. The arithmetic
        // evaluator and the table lookup must report the same two sums, exactly, for the same
        // readings; a second, drifting definition would show up as disagreement here.
        val readings = case(
            method = SizingMethod.SMALLEST_SIZE_FOR_LOAD,
            length = 18.0,
            load = 12.5,
            size = null,
        ).copy(
            loads = listOf(Load("synthetic-load-a", 12.5), Load("synthetic-load-b", 7.25)),
            developedLengths = listOf(
                DevelopedLength("synthetic-run", 18.0),
                DevelopedLength("synthetic-branch", 4.5),
            ),
            fittingAllowances = listOf(
                FittingAllowance("synthetic-fitting", 3, SourcedValue(2.5, UserProvenance.UserSupplied)),
            ),
        )
        val arithmetic = SizingEvaluator.evaluate(
            readings.copy(
                method = SizingMethod.CAPACITY_COMPARISON,
                capacity = CapacityEntry(9012.75, UserProvenance.UserSupplied, SizingMethod.CAPACITY_COMPARISON),
            ),
        ) as SizingEvaluation.Computed
        val returned = assertReturned(
            evaluateCaseAgainstUserTable(
                readings,
                table(rows = listOf(syntheticRow("synthetic-size-a", 50.0, 9000.5))),
            ),
        )

        // Invented synthetic readings: 12.5 + 7.25 MBH and (18 + 4.5) + (3 × 2.5) feet.
        assertEquals(19.75, arithmetic.requiredLoadMbh, 0.0)
        assertEquals(30.0, arithmetic.totalLengthFeet, 0.0)
        assertEquals(arithmetic.requiredLoadMbh, returned.requiredLoadMbh, 0.0)
        assertEquals(arithmetic.totalLengthFeet, returned.totalLengthFeet, 0.0)
    }

    @Test
    fun reverseLengthRefusalNamesTheSizesWhoseRecordedLengthsFallShort() {
        // 2.3.4: a size whose own recorded lengths stop short of the case must be named with its
        // own longest recorded length, so the user is not sent to fix a different size's limit.
        val refusal = assertRefusalKind(
            evaluateCaseAgainstUserTable(
                reverseCase(length = 50.0, load = 10.0),
                table(
                    rows = listOf(
                        syntheticRow("synthetic-size-a", 20.0, 9000.5),
                        syntheticRow("synthetic-size-b", 15.0, 8000.25),
                    ),
                ),
            ),
            RefusalKind.LENGTH_ABOVE_EVERY_ENTERED_ROW,
        )

        assertTrue(refusal.correctionRequired.contains("synthetic-size-a"))
        assertTrue(refusal.correctionRequired.contains("synthetic-size-b"))
        assertTrue(refusal.correctionRequired.contains("20.0"))
        assertTrue(refusal.correctionRequired.contains("15.0"))
    }

    @Test
    fun aSizeWhoseRecordedLengthsFallShortIsSkippedWhenAnotherSizeCoversTheCase() {
        // D4 step 4: a size that cannot answer at this length is skipped, not turned into a refusal,
        // while another recorded size can answer.
        val tooShort = syntheticRow("synthetic-size-a", 20.0, 9000.5)
        val covering = syntheticRow("synthetic-size-b", 60.0, 8000.25)

        val returned = assertReturned(
            evaluateCaseAgainstUserTable(
                reverseCase(length = 50.0, load = 10.0),
                table(rows = listOf(tooShort, covering)),
            ),
        )

        assertEquals(covering.nominalSize, returned.returnedSize)
        assertEquals(covering.capacityMbh, returned.returnedCapacityMbh, 0.0)
    }

    private fun forwardCase(length: Double = 50.0, size: String? = "synthetic-size-a", tableId: String = "synthetic-table") =
        case(method = SizingMethod.RETURN_FROM_USER_TABLE, length = length, load = 10.0, size = size, tableId = tableId)

    private fun reverseCase(length: Double = 50.0, load: Double = 10.0) =
        case(method = SizingMethod.SMALLEST_SIZE_FOR_LOAD, length = length, load = load, size = null)

    private fun case(method: SizingMethod, length: Double, load: Double, size: String?, tableId: String = "synthetic-table") = SizingCase(
        materialOrSystem = "synthetic-system",
        pressureClass = "synthetic-pressure",
        method = method,
        unitSystem = UnitSystem.IMPERIAL,
        loads = listOf(Load("synthetic-load", load)),
        developedLengths = listOf(DevelopedLength("synthetic-length", length)),
        fittingAllowances = emptyList(),
        capacity = null,
        gasSpecificGravity = SourcedValue(0.0, UserProvenance.UserSupplied),
        pressureDropAllowance = SourcedValue(0.0, UserProvenance.UserSupplied),
        tableSelection = TableSelection(tableId, size),
    )

    private fun table(
        applicability: TableApplicability = applicability(),
        rows: List<TableRow> = listOf(syntheticRow("synthetic-size-a", 50.0, 500.0)),
    ) = UserCapacityTable(
        id = "synthetic-table",
        name = "Synthetic lookup fixture",
        applicability = applicability,
        citation = TableCitation("Synthetic source", "Synthetic table"),
        marker = USER_RECORDED_MARKER,
        rows = rows,
    )

    private fun applicability() = TableApplicability(
        fuel = "synthetic-fuel",
        materialOrSystem = "synthetic-system",
        inletPressureClass = "synthetic-pressure",
        permittedPressureDrop = "synthetic-drop",
        gasSpecificGravity = "0.0",
    )

    // These are invented synthetic readings, not capacity-code or trade-size data.
    private fun syntheticRow(size: String, length: Double, capacity: Double, diameter: Double? = null) =
        TableRow(size, diameter, length, capacity)

    private fun assertReturned(result: SizingEvaluation): SizingEvaluation.ReturnedFromUserTable {
        assertTrue(result is SizingEvaluation.ReturnedFromUserTable)
        return result as SizingEvaluation.ReturnedFromUserTable
    }

    private fun assertRefusal(result: SizingEvaluation, kind: RefusalKind, text: String) {
        val refusal = assertRefusalKind(result, kind)
        assertTrue(refusal.correctionRequired.lowercase().contains(text.lowercase()))
    }

    private fun assertRefusalKind(result: SizingEvaluation, kind: RefusalKind): SizingEvaluation.Refused {
        assertTrue(result is SizingEvaluation.Refused)
        result as SizingEvaluation.Refused
        assertEquals(kind, result.kind)
        return result
    }
}

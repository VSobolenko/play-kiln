package com.gexetr.pipesizing.sizing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapacityTableTest {

    @Test
    fun incompleteTableRefusesWithTableIncomplete() {
        val result = completeTable().copy(rows = emptyList()).validationRefusal()

        assertRefusal(result, RefusalKind.TABLE_INCOMPLETE)
    }

    @Test
    fun duplicateSizeAndLengthRefusesWithTableIncomplete() {
        val row = syntheticRow("synthetic-size-a", 11.0, 101.0)
        val result = completeTable().copy(rows = listOf(row, row.copy(capacityMbh = 202.0))).validationRefusal()

        assertRefusal(result, RefusalKind.TABLE_INCOMPLETE)
    }

    @Test
    fun diameterContradictingRecordedOrderRefusesWithSizeOrderIndeterminate() {
        val result = completeTable().copy(
            rows = listOf(
                syntheticRow("synthetic-size-a", 11.0, 101.0, 2.0),
                syntheticRow("synthetic-size-b", 22.0, 202.0, 1.0),
            ),
        ).validationRefusal()

        assertRefusal(result, RefusalKind.SIZE_ORDER_INDETERMINATE)
    }

    @Test
    fun completeTablePassesAndPreservesRecordedSizeOrder() {
        val table = completeTable()

        assertNull(table.validationRefusal())
        assertEquals(listOf("synthetic-size-a", "synthetic-size-b"), table.derivedSizeOrder())
    }

    private fun completeTable() = UserCapacityTable(
        id = "synthetic-table",
        name = "Synthetic fixture table",
        applicability = TableApplicability("synthetic-fuel", "synthetic-system", "synthetic-pressure", "synthetic-drop", "0.0"),
        citation = TableCitation("Synthetic source", "Synthetic table"),
        marker = USER_RECORDED_MARKER,
        rows = listOf(
            syntheticRow("synthetic-size-a", 11.0, 101.0, 1.0),
            syntheticRow("synthetic-size-b", 22.0, 202.0, 2.0),
        ),
    )

    // These are invented synthetic readings, not capacity-code data.
    private fun syntheticRow(size: String, length: Double, capacity: Double, diameter: Double? = null) =
        TableRow(size, diameter, length, capacity)

    private fun assertRefusal(result: SizingEvaluation.Refused?, kind: RefusalKind) {
        assertTrue(result != null)
        assertEquals(kind, result?.kind)
    }
}

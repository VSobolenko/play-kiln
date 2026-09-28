package com.gexetr.pipesizing.sizing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SizingDispatchTest {

    @Test
    fun tableMethodWithoutATableRefusesByNamingTheTable() {
        val result = SizingEvaluator.evaluate(caseFor(SizingMethod.RETURN_FROM_USER_TABLE), table = null)

        assertTrue(result is SizingEvaluation.Refused)
        result as SizingEvaluation.Refused
        assertEquals(RefusalKind.NO_TABLE_SELECTED, result.kind)
        assertTrue(result.correctionRequired.contains("table", ignoreCase = true))
    }

    @Test
    fun arithmeticMethodKeepsItsPreviousEvaluation() {
        val result = SizingEvaluator.evaluate(caseFor(SizingMethod.CAPACITY_COMPARISON), table = null)

        assertTrue(result is SizingEvaluation.Computed)
        result as SizingEvaluation.Computed
        assertEquals(17.0, result.requiredLoadMbh, 0.0)
        assertEquals(19.0, result.totalLengthFeet, 0.0)
    }

    // Invented synthetic readings, not capacity-code data.
    private fun caseFor(method: SizingMethod) = SizingCase(
        materialOrSystem = "synthetic-system",
        pressureClass = "synthetic-pressure",
        method = method,
        unitSystem = UnitSystem.IMPERIAL,
        loads = listOf(Load("synthetic-load", 17.0)),
        developedLengths = listOf(DevelopedLength("synthetic-run", 19.0)),
        fittingAllowances = emptyList(),
        capacity = CapacityEntry(23.0, UserProvenance.UserSupplied, method),
    )
}

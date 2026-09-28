package com.gexetr.pipesizing.sizing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The case readings below are invented synthetics, not code or standard values. The physical
 * readings the method requires — gas specific gravity, pressure drop and inside diameter — are
 * deliberately absurd (21.5, 35.5, 88.5) so a reader cannot mistake them for a code table's
 * figures; no code or standard reads that way. The app supplies no capacity data of its own
 * (`docs/research/2026-09-25-pipe-sizing-code-source.md`). The unit identities asserted in
 * `convertsImperialToMetricAndBackWithoutChangingPhysicalValue` are the SI/imperial definitions
 * themselves, not slot data, and are the subject of that test rather than a fixture standing in
 * for one.
 */
class SizingEvaluatorTest {

    @Test
    fun evaluatesHandSummedLoadLengthAndFittingAllowance() {
        val result = SizingEvaluator.evaluate(completeCase())

        assertTrue(result is SizingEvaluation.Computed)
        result as SizingEvaluation.Computed
        // 35.0 MBH + 42.5 MBH + 17.5 MBH = 95.0 MBH.
        assertEquals(95.0, result.requiredLoadMbh, TOLERANCE)
        // 12.0 ft + 18.0 ft = 30.0 developed ft; (2 * 1.5 ft) + (1 * 4.0 ft) = 7.0 ft; total 37.0 ft.
        assertEquals(30.0, result.developedLengthFeet, TOLERANCE)
        assertEquals(7.0, result.fittingAllowanceFeet, TOLERANCE)
        assertEquals(37.0, result.totalLengthFeet, TOLERANCE)
        assertTrue(result.supportsRequiredLoad)
    }

    @Test
    fun convertsImperialToMetricAndBackWithoutChangingPhysicalValue() {
        // 10 ft = 3.048 m. MBH uses 1 BTU_IT = 1055.05585262 J, so 100 MBH
        // derives to 29.307107017222222 kW via 1000 BTU_IT/hour per MBH and
        // 3600 kJ/hour per kW. The 1e-10 tolerance is tight enough to catch
        // the old factor's 4.093888890110975e-6 kW error at 100 MBH.
        assertEquals(10.0, Units.metersToFeet(Units.feetToMeters(10.0)), TOLERANCE)
        assertEquals(0.2930710701722222, Units.mbhToKilowatts(1.0), CONVERSION_TOLERANCE)
        assertEquals(29.307107017222222, Units.mbhToKilowatts(100.0), CONVERSION_TOLERANCE)
        assertEquals(293.0710701722222, Units.mbhToKilowatts(1000.0), CONVERSION_TOLERANCE)
        assertEquals(100.0, Units.kilowattsToMbh(Units.mbhToKilowatts(100.0)), TOLERANCE)
    }

    @Test
    fun selectsOnlyTheInputsRequiredByTheChosenMethod() {
        val method = SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS
        val case = completeCase().copy(
            method = method,
            // A capacity confirmed for another method is an inconsistency, so the user has to
            // re-confirm it for the newly selected method before the method-required physical
            // inputs become the evaluator's next complaint.
            capacity = completeCase().requireCapacity().copy(confirmedForMethod = method),
        )

        assertRefusal(
            SizingEvaluator.evaluate(case),
            RefusalKind.MISSING_INPUT,
            "gas specific gravity",
        )

        val result = SizingEvaluator.evaluate(
            case.copy(
                // Invented synthetics, not code or standard readings: 21.5 is no gas's specific
                // gravity, 35.5 is no pressure-drop allowance, 88.5 is no pipe's inside diameter.
                gasSpecificGravity = sourced(21.5),
                pressureDropAllowance = sourced(35.5),
                pipeInsideDiameter = sourced(88.5),
            ),
        )

        assertTrue(result is SizingEvaluation.Computed)
    }

    @Test
    fun refusesARequiredCapacityThatIsMissing() {
        val result = SizingEvaluator.evaluate(completeCase().copy(capacity = null))

        assertRefusal(result, RefusalKind.MISSING_INPUT, "user-confirmed capacity")
    }

    @Test
    fun refusesNonFiniteOrNegativeValues() {
        val result = SizingEvaluator.evaluate(completeCase().copy(loads = listOf(Load("range", -1.0))))

        assertRefusal(result, RefusalKind.OUT_OF_RANGE, "load")
        assertRefusal(
            SizingEvaluator.evaluate(completeCase().copy(loads = listOf(Load("nan", Double.NaN)))),
            RefusalKind.OUT_OF_RANGE,
            "load",
        )
        assertRefusal(
            SizingEvaluator.evaluate(completeCase().copy(loads = listOf(Load("infinite", Double.POSITIVE_INFINITY)))),
            RefusalKind.OUT_OF_RANGE,
            "load",
        )
    }

    @Test
    fun refusesFittingEquivalentLengthWithoutProvenance() {
        val fitting = FittingAllowance("Unverified elbow", 1, SourcedValue(2.0, null))

        assertRefusal(
            SizingEvaluator.evaluate(completeCase().copy(fittingAllowances = listOf(fitting))),
            RefusalKind.MISSING_PROVENANCE,
            "fitting equivalent length",
        )
    }

    @Test
    fun metricUnitTogglePreservesPhysicalEvaluation() {
        val imperial = SizingEvaluator.evaluate(completeCase())
        val metric = SizingEvaluator.evaluate(completeCase().copy(unitSystem = UnitSystem.METRIC))

        assertTrue(imperial is SizingEvaluation.Computed)
        assertTrue(metric is SizingEvaluation.Computed)
        imperial as SizingEvaluation.Computed
        metric as SizingEvaluation.Computed
        assertEquals(imperial.requiredLoadMbh, metric.requiredLoadMbh, TOLERANCE)
        assertEquals(imperial.totalLengthFeet, metric.totalLengthFeet, TOLERANCE)
        assertEquals(imperial.capacityProvenance, metric.capacityProvenance)
    }

    @Test
    fun refusesALongCaseOutsideTheUsersStatedCapacityRange() {
        val capacity = completeCase().requireCapacity().copy(statedMaximumTotalLengthFeet = 36.0)

        val result = SizingEvaluator.evaluate(completeCase().copy(capacity = capacity))

        // The hand-summed total is 37.0 ft, which exceeds the user-entered 36.0-ft applicability limit.
        assertRefusal(result, RefusalKind.OUT_OF_RANGE, "total length")
    }

    @Test
    fun refusesAConfirmedCapacityForAnotherMethod() {
        val capacityForOtherMethod = completeCase().requireCapacity().copy(
            confirmedForMethod = SizingMethod.CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS,
        )

        val result = SizingEvaluator.evaluate(completeCase().copy(capacity = capacityForOtherMethod))

        assertRefusal(result, RefusalKind.INCONSISTENT_INPUT, "method")
    }

    @Test
    fun refusesMissingProvenanceInsteadOfShowingACapacityResult() {
        val result = SizingEvaluator.evaluate(
            completeCase().copy(capacity = completeCase().requireCapacity().copy(provenance = null)),
        )

        assertRefusal(result, RefusalKind.MISSING_PROVENANCE, "capacity")
    }

    @Test
    fun doesNotReuseAPreviousRowsCapacityWhenTheCurrentRowIsBlank() {
        val previousRow = completeCase().requireCapacity()
        val currentRow = completeCase().copy(capacity = null)

        val result = SizingEvaluator.evaluate(currentRow)

        assertEquals(120.0, previousRow.value, TOLERANCE) // A separate, user-confirmed prior-row value.
        assertRefusal(result, RefusalKind.MISSING_INPUT, "user-confirmed capacity")
    }

    private fun completeCase() = SizingCase(
        materialOrSystem = "User-entered steel system",
        pressureClass = "User-entered pressure class",
        method = SizingMethod.CAPACITY_COMPARISON,
        unitSystem = UnitSystem.IMPERIAL,
        loads = listOf(Load("Appliance A", 35.0), Load("Appliance B", 42.5), Load("Appliance C", 17.5)),
        developedLengths = listOf(DevelopedLength("Run 1", 12.0), DevelopedLength("Run 2", 18.0)),
        fittingAllowances = listOf(
            FittingAllowance("User-entered elbow", 2, sourced(1.5)),
            FittingAllowance("User-entered valve", 1, sourced(4.0)),
        ),
        capacity = CapacityEntry(120.0, UserProvenance.UserSupplied, SizingMethod.CAPACITY_COMPARISON),
    )

    private fun sourced(value: Double) = SourcedValue(value, UserProvenance.UserSupplied)

    private fun assertRefusal(result: SizingEvaluation, kind: RefusalKind, correctionFragment: String) {
        assertTrue(result is SizingEvaluation.Refused)
        result as SizingEvaluation.Refused
        assertEquals(kind, result.kind)
        assertTrue(result.correctionRequired.contains(correctionFragment, ignoreCase = true))
    }

    private companion object {
        const val TOLERANCE = 0.000001
        const val CONVERSION_TOLERANCE = 0.0000000001
    }
}

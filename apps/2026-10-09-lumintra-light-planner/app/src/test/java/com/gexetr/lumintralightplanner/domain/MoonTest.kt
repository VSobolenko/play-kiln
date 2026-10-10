package com.gexetr.lumintralightplanner.domain

import kotlin.math.cos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Moon phase naming and the Moon state.
 *
 * The naming rule (`specs/session-brief/spec.md` "The Moon is stated as a state with both numbers"):
 * eight 45°-wide bins, offset by 22.5°, centred on each exact phase. The two product fractions -
 * `DARK_MOON_FRACTION = 0.10` and `FULL_MOON_FRACTION = 0.90` - are product decisions, stated as such.
 */
class MoonTest {

    private fun moon(
        illuminatedFraction: Double = 0.5,
        phaseAngleDeg: Double = 90.0,
        elongationDeg: Double = 90.0,
        phaseName: MoonPhaseName = MoonPhaseName.FIRST_QUARTER,
        altitudeDeg: Double = 30.0,
        azimuthDeg: Double = 180.0,
        magnitude: Double = -10.0,
    ) = MoonState(
        illuminatedFraction = illuminatedFraction,
        phaseAngleDeg = phaseAngleDeg,
        elongationDeg = elongationDeg,
        phaseName = phaseName,
        altitudeDeg = altitudeDeg,
        azimuthDeg = azimuthDeg,
        magnitude = magnitude,
    )

    // ----------------------------------------------------- the eight sector centres

    @Test
    fun eachExactPhaseCentreMapsToItsOwnName() {
        val centres = listOf(
            0.0 to MoonPhaseName.NEW_MOON,
            45.0 to MoonPhaseName.WAXING_CRESCENT,
            90.0 to MoonPhaseName.FIRST_QUARTER,
            135.0 to MoonPhaseName.WAXING_GIBBOUS,
            180.0 to MoonPhaseName.FULL_MOON,
            225.0 to MoonPhaseName.WANING_GIBBOUS,
            270.0 to MoonPhaseName.LAST_QUARTER,
            315.0 to MoonPhaseName.WANING_CRESCENT,
        )
        for ((deg, expected) in centres) {
            assertEquals("elongation $deg", expected, MoonPhaseName.ofElongation(deg))
        }
    }

    @Test
    fun exactDegreesReportsTheSectorCentreForEveryName() {
        for (name in MoonPhaseName.entries) {
            val deg = MoonPhaseName.exactDegrees(name)
            assertEquals("round trip for $name", name, MoonPhaseName.ofElongation(deg))
        }
    }

    // ------------------------------------------------------ the eight sector edges

    /**
     * A sector edge belongs to the sector it opens: the bin is
     * `floor(normalise(elongation + 22.5) / 45)`, so 22.5° is the first degree of the *next* bin.
     */
    @Test
    fun eachSectorEdgeOpensTheFollowingBin() {
        val edges = listOf(
            22.5 to MoonPhaseName.WAXING_CRESCENT,
            67.5 to MoonPhaseName.FIRST_QUARTER,
            112.5 to MoonPhaseName.WAXING_GIBBOUS,
            157.5 to MoonPhaseName.FULL_MOON,
            202.5 to MoonPhaseName.WANING_GIBBOUS,
            247.5 to MoonPhaseName.LAST_QUARTER,
            292.5 to MoonPhaseName.WANING_CRESCENT,
            337.5 to MoonPhaseName.NEW_MOON,
        )
        for ((deg, expected) in edges) {
            assertEquals("edge at $deg", expected, MoonPhaseName.ofElongation(deg))
        }
    }

    /**
     * `annex/L-depth-retention-edges.md` row **E19** — *Situation:* "Mapping a phase angle to a named
     * phase, near 0°/360°"; *What is refused:* "The wrap bug: `new` at both ends of the table."
     *
     * **Corrected 2026-10-09, by the owner's decision.** This test previously asserted the opposite
     * of the shipped behaviour — `assertNotEquals(NEW_MOON, ofElongation(359.0))` — following a
     * `specs/session-brief/spec.md` clause ("not bucketed as new") that was withdrawn as
     * astronomically wrong: `[337.5°, 22.5°)` is the bin centred on 0°, so it contains 359° by
     * construction, and 359° is 1° *short of new*, hence a waning crescent on its way to new.
     *
     * The property E19 actually refuses is `new` **appearing twice**, at both ends of the table. The
     * assertions below test that instead: the values either side of the wrap and the value at the wrap
     * itself all resolve to the same name, the next sector past the wrap resolves to the next name in
     * cycle order, and a tenth-degree walk shows the `new` sector is exactly 45° wide with every one
     * of the eight names reachable. Arithmetic measured against `domain/Moon.kt`'s
     * `bin = floor(((elongation + 22.5) % 360) / 45)`.
     */
    @Test
    fun theNewSectorIsContiguousAcrossTheWrapRatherThanNamedAtBothEnds() {
        // The whole sector that straddles 0/360° names one phase, not two.
        assertEquals("the sector's leading edge", MoonPhaseName.NEW_MOON, MoonPhaseName.ofElongation(337.5))
        assertEquals("the wrap itself", MoonPhaseName.NEW_MOON, MoonPhaseName.ofElongation(0.0))
        assertEquals("the wrap from the other side", MoonPhaseName.NEW_MOON, MoonPhaseName.ofElongation(359.0))
        assertEquals("wrapping past 360 lands in the same sector", MoonPhaseName.NEW_MOON, MoonPhaseName.ofElongation(360.0))
        assertEquals("wrapping negative lands in the same sector", MoonPhaseName.NEW_MOON, MoonPhaseName.ofElongation(-1.0))

        // …and the next sector past the wrap is the next name in cycle order, so the sequence around
        // the wrap is continuous rather than restarting.
        assertEquals(MoonPhaseName.WAXING_CRESCENT, MoonPhaseName.ofElongation(22.5))
        assertEquals(MoonPhaseName.WANING_CRESCENT, MoonPhaseName.ofElongation(337.4))

        // One `new` sector, so `new` is named exactly once in a full turn — the defect E19 refuses.
        // The sector is 45° wide and straddles the wrap; a table with `new` duplicated at the ends, or
        // a naive `floor(elongation / 45)` that puts `new` only at [0°, 45°), fails both measurements.
        val walk = (0 until 3600).map { MoonPhaseName.ofElongation(it / 10.0) }
        assertEquals(
            "the `new` sector is exactly 45° wide, so it takes 450 of the 3 600 tenth-degree samples",
            450,
            walk.count { it == MoonPhaseName.NEW_MOON },
        )
        assertEquals(
            "every name is reachable, so no sector was swallowed by a duplicated table row",
            MoonPhaseName.entries.toSet(),
            walk.toSet(),
        )
    }

    @Test
    fun elongationWrapsForNegativeAndOverThreeSixtyValues() {
        assertEquals(MoonPhaseName.WAXING_CRESCENT, MoonPhaseName.ofElongation(405.0)) // 405 == 45
        assertEquals(MoonPhaseName.WANING_CRESCENT, MoonPhaseName.ofElongation(-45.0)) // -45 == 315
        assertEquals(MoonPhaseName.NEW_MOON, MoonPhaseName.ofElongation(720.0)) // 720 == 0
    }

    // ---------------------------------------------------------- the k = (1+cos)/2 law

    @Test
    fun illuminatedFractionMatchesThePhaseAngleLaw() {
        val angle = 120.0
        val k = (1.0 + cos(Math.toRadians(angle))) / 2.0
        val state = MoonState.of(
            illuminatedFraction = k,
            phaseAngleDeg = angle,
            elongationDeg = 60.0,
            altitudeDeg = 25.0,
            azimuthDeg = 200.0,
            magnitude = -9.0,
        )
        assertEquals(k, state.illuminatedFraction, 1e-12)
        assertEquals(k, (1.0 + cos(Math.toRadians(state.phaseAngleDeg))) / 2.0, 1e-12)
    }

    @Test
    fun ofCoercesTheIlluminatedFractionIntoZeroOne() {
        assertEquals(1.0, MoonState.of(1.6, 0.0, 180.0, 10.0, 100.0, -12.0).illuminatedFraction, 1e-12)
        assertEquals(0.0, MoonState.of(-0.4, 180.0, 0.0, 10.0, 100.0, -12.0).illuminatedFraction, 1e-12)
    }

    @Test
    fun ofNamesThePhaseFromTheElongation() {
        val state = MoonState.of(1.0, 180.0, 180.0, 40.0, 180.0, -12.7)
        assertEquals(MoonPhaseName.FULL_MOON, state.phaseName)
    }

    // ------------------------------------------------------------- the two thresholds

    @Test
    fun isDarkMoonAtOrBelowTenPercent() {
        assertTrue(moon(illuminatedFraction = 0.09).isDarkMoon)
        assertTrue(moon(illuminatedFraction = 0.10).isDarkMoon)
        assertFalse(moon(illuminatedFraction = 0.11).isDarkMoon)
    }

    @Test
    fun isFullMoonRequiresBothTheFullBinAndNinetyPercent() {
        assertTrue(
            moon(
                illuminatedFraction = 0.90,
                phaseName = MoonPhaseName.FULL_MOON,
                elongationDeg = 180.0,
            ).isFullMoon,
        )
        // The same fraction in a gibbous bin is not "full".
        assertFalse(
            moon(
                illuminatedFraction = 0.95,
                phaseName = MoonPhaseName.WAXING_GIBBOUS,
                elongationDeg = 150.0,
            ).isFullMoon,
        )
        // The full bin below the fraction is not "full" either.
        assertFalse(
            moon(
                illuminatedFraction = 0.89,
                phaseName = MoonPhaseName.FULL_MOON,
                elongationDeg = 180.0,
            ).isFullMoon,
        )
    }

    // ---------------------------------------------------------------------- derived

    @Test
    fun isMoonlessIsFalseWhenTheMoonIsUpAndWellLit() {
        assertFalse(moon(illuminatedFraction = 0.5, altitudeDeg = 30.0).isMoonless)
    }

    @Test
    fun isMoonlessIsTrueWhenTheMoonIsBelowTheHorizon() {
        assertTrue(moon(illuminatedFraction = 0.9, altitudeDeg = -5.0).isMoonless)
    }

    @Test
    fun isMoonlessIsTrueForADarkMoonAboveTheHorizon() {
        assertTrue(moon(illuminatedFraction = 0.05, altitudeDeg = 30.0).isMoonless)
    }

    @Test
    fun isUpIsTheGeometricHorizonTest() {
        assertTrue(moon(altitudeDeg = 0.1).isUp)
        assertFalse(moon(altitudeDeg = 0.0).isUp)
        assertFalse(moon(altitudeDeg = -0.1).isUp)
    }

    @Test
    fun productThresholdsAreTheStatedConstants() {
        assertEquals(0.10, MoonState.DARK_MOON_FRACTION, 1e-12)
        assertEquals(0.90, MoonState.FULL_MOON_FRACTION, 1e-12)
    }
}

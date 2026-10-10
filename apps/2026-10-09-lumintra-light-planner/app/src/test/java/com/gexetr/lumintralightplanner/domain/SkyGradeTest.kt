package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The darkness grading, and the rule that the grade is **never a Bortle class**.
 *
 * `specs/brief/spec.md` / `domain/SkyGrade.kt`: `SkyGrade` carries only computed quantities —
 * the Sun's depth, the time spent dark, and the Moon's interference — and there is deliberately **no
 * `bortle` and no `sqm` field on it**, because no light-pollution raster ships and a computed field
 * with that name would be the easiest way to render a user's own estimate as if the app had measured
 * it. The user's estimate lives on the different type [SkyBrightnessEstimate].
 */
class SkyGradeTest {

    private val midnight: Instant = Instant.parse("2026-10-09T22:00:00Z")

    private fun grade(
        kind: DarkWindowKind = DarkWindowKind.DARK,
        sunMin: Double = -30.0,
        darkMinutes: Long = 300,
        moonFraction: Double = 0.0,
        moonAltitude: Double = -10.0,
    ) = SkyGrade(
        darkestSunAltitudeDeg = sunMin,
        darkestInstant = midnight,
        darkWindowKind = kind,
        darkDuration = Duration.ofMinutes(darkMinutes),
        moonIlluminatedFraction = moonFraction,
        moonAltitudeDeg = moonAltitude,
    )

    // ------------------------------------------------ the never-a-Bortle-class rule

    /**
     * The reflection test the container asks for: `SkyGrade` must declare **no field** whose name
     * mentions `bortle` or `sqm`. A field added under either name fails this immediately.
     */
    @Test
    fun skyGradeDeclaresNoBortleAndNoSqmField() {
        val offenders = SkyGrade::class.java.declaredFields
            .map { it.name }
            .filter { it.contains("bortle", ignoreCase = true) || it.contains("sqm", ignoreCase = true) }
        assertTrue("SkyGrade must not declare a Bortle or SQM field, found $offenders", offenders.isEmpty())
    }

    /**
     * The positive control that keeps the reflection above from being vacuous: the *estimate* type
     * really does declare those fields, so the scan can see them when they are present.
     */
    @Test
    fun theUserEstimateTypeDoesDeclareThoseFieldsWhichProvesTheScanCanSeeThem() {
        val names = SkyBrightnessEstimate::class.java.declaredFields.map { it.name }
        assertTrue("the scan must be able to see a bortle field, saw $names", names.contains("bortle"))
        assertTrue("the scan must be able to see an sqm field, saw $names", names.contains("sqm"))
    }

    @Test
    fun theGradeCarriesTheComputedInputs() {
        val names = SkyGrade::class.java.declaredFields.map { it.name }.toSet()
        assertTrue(names.containsAll(
            setOf(
                "darkestSunAltitudeDeg",
                "darkestInstant",
                "darkWindowKind",
                "darkDuration",
                "moonIlluminatedFraction",
                "moonAltitudeDeg",
            ),
        ))
    }

    // --------------------------------------------------------- moon interference

    @Test
    fun moonInterferenceIsTheProductOfHowLitAndHowHighItIs() {
        assertEquals(0.5 * (45.0 / 90.0), grade(moonFraction = 0.5, moonAltitude = 45.0).moonInterference, 1e-12)
        assertEquals(1.0 * 1.0, grade(moonFraction = 1.0, moonAltitude = 90.0).moonInterference, 1e-12)
    }

    @Test
    fun aMoonBelowTheHorizonHasZeroInterferenceHoweverLitItIs() {
        assertEquals(0.0, grade(moonFraction = 1.0, moonAltitude = -1.0).moonInterference, 1e-12)
        assertEquals(0.0, grade(moonFraction = 1.0, moonAltitude = 0.0).moonInterference, 1e-12)
    }

    // --------------------------------------------------------------------- level

    @Test
    fun aPolarDayAndANoDarkNightGradeAsNoDarknessAtAll() {
        assertEquals(DarknessLevel.NONE, grade(kind = DarkWindowKind.POLAR_DAY, sunMin = 1.0, darkMinutes = 0).level)
        assertEquals(DarknessLevel.NONE, grade(kind = DarkWindowKind.NO_DARK, sunMin = -4.0, darkMinutes = 0).level)
    }

    @Test
    fun aDarkNightWithNoDarkTimeAtAllIsPoor() {
        assertEquals(DarknessLevel.POOR, grade(darkMinutes = 0).level)
    }

    @Test
    fun aWashedOutNightIsPoorHoweverDeepTheSunGoes() {
        // Moon interference 1.0 * 60/90 = 0.667, above the 0.55 threshold.
        assertEquals(DarknessLevel.POOR, grade(sunMin = -40.0, moonFraction = 1.0, moonAltitude = 60.0).level)
        // Exactly at the threshold is still poor (`>= 0.55`).
        assertEquals(DarknessLevel.POOR, grade(moonFraction = 0.6, moonAltitude = 90.0).level)
    }

    @Test
    fun anExcellentNightNeedsBothADeepSunAndAnAbsentMoon() {
        assertEquals(DarknessLevel.EXCELLENT, grade(sunMin = -31.0, moonFraction = 0.1, moonAltitude = 45.0).level)
        // Exactly -30.0 counts as deep enough, and exactly 0.15 of interference is still excellent.
        assertEquals(DarknessLevel.EXCELLENT, grade(sunMin = -30.0, moonFraction = 0.15, moonAltitude = 90.0).level)
        // A tenth of a degree shallower: no longer excellent.
        assertEquals(DarknessLevel.GOOD, grade(sunMin = -29.9, moonFraction = 0.0, moonAltitude = -10.0).level)
        // A touch more moonlight: no longer excellent, but still good.
        assertEquals(DarknessLevel.GOOD, grade(sunMin = -30.0, moonFraction = 0.16, moonAltitude = 90.0).level)
    }

    @Test
    fun aGoodNightIsAstronomicallyDarkWithALittleMoonlight() {
        assertEquals(DarknessLevel.GOOD, grade(sunMin = -22.0, moonFraction = 0.5, moonAltitude = 60.0).level)
        // The -18.0 boundary is inclusive.
        assertEquals(DarknessLevel.GOOD, grade(sunMin = -18.0, moonFraction = 0.0).level)
    }

    @Test
    fun aNightShallowerThanAstronomicalTwilightIsOnlyFair() {
        assertEquals(DarknessLevel.FAIR, grade(sunMin = -17.9, moonFraction = 0.0).level)
        // Astronomically dark but with a bright high Moon: fair, not good.
        assertEquals(DarknessLevel.FAIR, grade(sunMin = -22.0, moonFraction = 1.0, moonAltitude = 45.0).level)
    }

    @Test
    fun darkMinutesIsTheDurationInWholeMinutes() {
        assertEquals(300L, grade(darkMinutes = 300).darkMinutes)
        assertEquals(0L, grade(kind = DarkWindowKind.POLAR_DAY, darkMinutes = 0).darkMinutes)
        assertEquals(59L, grade(darkMinutes = 59).darkMinutes)
    }

    // ------------------------------------------------------ SkyBrightnessEstimate

    @Test
    fun anSqmEntryIsClampedToTheAcceptedRangeAndSaysSo() {
        assertEquals(SkyBrightnessEstimate.SQM_MIN, SkyBrightnessEstimate.sqm(15.0).sqm!!, 1e-12)
        assertEquals(SkyBrightnessEstimate.SQM_MAX, SkyBrightnessEstimate.sqm(23.0).sqm!!, 1e-12)
        assertTrue(SkyBrightnessEstimate.sqm(15.0).clamped)
        assertTrue(SkyBrightnessEstimate.sqm(23.0).clamped)

        val inRange = SkyBrightnessEstimate.sqm(20.0)
        assertEquals(20.0, inRange.sqm!!, 1e-12)
        assertFalse(inRange.clamped)
        assertEquals(SkyBrightnessEstimateKind.SQM, inRange.kind)
    }

    @Test
    fun aNaNEntryProducesNoEstimateAtAllRatherThanANumber() {
        val none = SkyBrightnessEstimate.sqm(Double.NaN)
        assertEquals(SkyBrightnessEstimateKind.NONE, none.kind)
        assertNull(none.sqm)
        assertNull(none.bortle)
        assertFalse(none.clamped)
        assertFalse(none.isSet)
    }

    @Test
    fun aBortleEntryIsClampedToTheAcceptedClassesAndSaysSo() {
        assertEquals(1, SkyBrightnessEstimate.bortle(0).bortle!!)
        assertEquals(9, SkyBrightnessEstimate.bortle(10).bortle!!)
        assertTrue(SkyBrightnessEstimate.bortle(0).clamped)
        assertTrue(SkyBrightnessEstimate.bortle(10).clamped)

        val four = SkyBrightnessEstimate.bortle(4)
        assertEquals(4, four.bortle!!)
        assertFalse(four.clamped)
        assertEquals(SkyBrightnessEstimateKind.BORTLE, four.kind)
        assertTrue(four.isSet)
    }

    @Test
    fun theOutOfRangePredicatesAgreeWithTheClampBounds() {
        assertTrue(SkyBrightnessEstimate.sqmOutOfRange(Double.NaN))
        assertTrue(SkyBrightnessEstimate.sqmOutOfRange(SkyBrightnessEstimate.SQM_MIN - 0.1))
        assertTrue(SkyBrightnessEstimate.sqmOutOfRange(SkyBrightnessEstimate.SQM_MAX + 0.1))
        assertFalse(SkyBrightnessEstimate.sqmOutOfRange(SkyBrightnessEstimate.SQM_MIN))
        assertFalse(SkyBrightnessEstimate.sqmOutOfRange(SkyBrightnessEstimate.SQM_MAX))

        assertTrue(SkyBrightnessEstimate.bortleOutOfRange(SkyBrightnessEstimate.BORTLE_MIN - 1))
        assertTrue(SkyBrightnessEstimate.bortleOutOfRange(SkyBrightnessEstimate.BORTLE_MAX + 1))
        assertFalse(SkyBrightnessEstimate.bortleOutOfRange(SkyBrightnessEstimate.BORTLE_MIN))
        assertFalse(SkyBrightnessEstimate.bortleOutOfRange(SkyBrightnessEstimate.BORTLE_MAX))
    }

    @Test
    fun theNoneEstimateIsTheDefaultAndHoldsNothing() {
        val none = SkyBrightnessEstimate.NONE
        assertEquals(SkyBrightnessEstimateKind.NONE, none.kind)
        assertNull(none.sqm)
        assertNull(none.bortle)
        assertFalse(none.clamped)
        assertFalse(none.isSet)
    }

    @Test
    fun theAcceptedRangesAreTheStatedOnes() {
        assertEquals(16.0, SkyBrightnessEstimate.SQM_MIN, 1e-12)
        assertEquals(22.0, SkyBrightnessEstimate.SQM_MAX, 1e-12)
        assertEquals(1, SkyBrightnessEstimate.BORTLE_MIN)
        assertEquals(9, SkyBrightnessEstimate.BORTLE_MAX)
    }
}

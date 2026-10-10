package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The brief's ordering rule and its derived read-backs.
 *
 * design.md / `domain/Brief.kt`: conditions are ordered **chronologically by the instant each one
 * attaches to**; a condition with no single instant is **anchored to the dark window's start**; and two
 * conditions at the same instant are broken by the fixed precedence
 * `DARK_WINDOW`, `MOON`, `CLOUD`, `TARGET`, `SATELLITE`, `HORIZON` — which is [ConditionKind]'s own
 * declaration order, so the two can never drift apart.
 */
class BriefOrderingTest {

    private val darkStart: Instant = Instant.parse("2026-10-09T20:00:00Z")
    private val t1: Instant = Instant.parse("2026-10-09T22:00:00Z")
    private val t2: Instant = Instant.parse("2026-10-10T02:00:00Z")

    private fun condition(
        kind: ConditionKind,
        anchorUtc: Instant?,
        assessment: ConditionAssessment = ConditionAssessment.Assessed(ConditionState.GOOD),
    ) = BriefCondition(
        kind = kind,
        anchorUtc = anchorUtc,
        assessment = assessment,
        facts = BriefFacts.Cloud(null, null),
    )

    // -------------------------------------------------------------- chronological

    @Test
    fun conditionsAreOrderedByTheirAnchorAscending() {
        val conditions = listOf(
            condition(ConditionKind.SATELLITE, t2),
            condition(ConditionKind.MOON, darkStart),
            condition(ConditionKind.HORIZON, t1),
        )
        val ordered = BriefOrdering.order(conditions, darkStart)
        assertEquals(
            listOf(ConditionKind.MOON, ConditionKind.HORIZON, ConditionKind.SATELLITE),
            ordered.map { it.kind },
        )
    }

    @Test
    fun anUnanchoredConditionFallsBackToTheDarkWindowStart() {
        val unanchored = condition(ConditionKind.CLOUD, null)
        val anchored = condition(ConditionKind.MOON, t1)
        val ordered = BriefOrdering.order(listOf(anchored, unanchored), darkStart)
        assertEquals(listOf(ConditionKind.CLOUD, ConditionKind.MOON), ordered.map { it.kind })
    }

    @Test
    fun anUnanchoredConditionWithNoDarkWindowGoesLast() {
        val unanchored = condition(ConditionKind.CLOUD, null)
        val anchored = condition(ConditionKind.MOON, t2)
        val ordered = BriefOrdering.order(listOf(unanchored, anchored), null)
        assertEquals(listOf(ConditionKind.MOON, ConditionKind.CLOUD), ordered.map { it.kind })

        // Two of them, both unattached and with no dark window: the deterministic order is the input
        // order (the sort is stable), so the same brief is produced twice.
        val twice = BriefOrdering.order(listOf(unanchored, condition(ConditionKind.HORIZON, null)), null)
        assertEquals(listOf(ConditionKind.CLOUD, ConditionKind.HORIZON), twice.map { it.kind })
    }

    // ---------------------------------------------------------------- tie-breaks

    @Test
    fun tiesAtOneInstantAreBrokenByKindPrecedence() {
        // Deliberately shuffled, every condition anchored to the same instant.
        val conditions = listOf(
            condition(ConditionKind.HORIZON, darkStart),
            condition(ConditionKind.SATELLITE, darkStart),
            condition(ConditionKind.TARGET, darkStart),
            condition(ConditionKind.CLOUD, darkStart),
            condition(ConditionKind.MOON, darkStart),
            condition(ConditionKind.DARK_WINDOW, darkStart),
        )
        val ordered = BriefOrdering.order(conditions, darkStart)
        assertEquals(
            listOf(
                ConditionKind.DARK_WINDOW,
                ConditionKind.MOON,
                ConditionKind.CLOUD,
                ConditionKind.TARGET,
                ConditionKind.SATELLITE,
                ConditionKind.HORIZON,
            ),
            ordered.map { it.kind },
        )
    }

    @Test
    fun aTieIsBrokenByKindEvenWhenTheEarlierKindComesFromTheFallbackAnchor() {
        val explicit = condition(ConditionKind.HORIZON, darkStart)
        val viaFallback = condition(ConditionKind.MOON, null) // anchored to darkStart by the rule
        val ordered = BriefOrdering.order(listOf(explicit, viaFallback), darkStart)
        assertEquals(listOf(ConditionKind.MOON, ConditionKind.HORIZON), ordered.map { it.kind })
    }

    @Test
    fun precedenceIsTheKindsOwnDeclarationOrder() {
        assertEquals(0, condition(ConditionKind.DARK_WINDOW, darkStart).precedence)
        assertEquals(1, condition(ConditionKind.MOON, darkStart).precedence)
        assertEquals(2, condition(ConditionKind.CLOUD, darkStart).precedence)
        assertEquals(3, condition(ConditionKind.TARGET, darkStart).precedence)
        assertEquals(4, condition(ConditionKind.SATELLITE, darkStart).precedence)
        assertEquals(5, condition(ConditionKind.HORIZON, darkStart).precedence)
    }

    @Test
    fun orderingLeavesTheOriginalListUntouched() {
        val conditions = listOf(
            condition(ConditionKind.SATELLITE, t2),
            condition(ConditionKind.MOON, darkStart),
        )
        val before = conditions.map { it.kind }
        BriefOrdering.order(conditions, darkStart)
        assertEquals(before, conditions.map { it.kind })
    }

    @Test
    fun anEmptyListOrdersToAnEmptyList() {
        assertTrue(BriefOrdering.order(emptyList(), darkStart).isEmpty())
    }

    // -------------------------------------------------------- the assessment mapping

    @Test
    fun anAssessedConditionExposesItsStateAndNotAReason() {
        val good = condition(ConditionKind.MOON, darkStart, ConditionAssessment.Assessed(ConditionState.GOOD))
        assertEquals(ConditionState.GOOD, good.state)
        assertFalse(good.isUnavailable)
        assertNull(good.unavailableReason)
    }

    @Test
    fun anUnavailableConditionExposesItsTypedReasonAndNoState() {
        val offline = condition(
            ConditionKind.CLOUD,
            darkStart,
            ConditionAssessment.Unavailable(ConditionUnavailableReason.OFFLINE),
        )
        assertNull(offline.state)
        assertTrue(offline.isUnavailable)
        assertEquals(ConditionUnavailableReason.OFFLINE, offline.unavailableReason)
    }

    // ----------------------------------------------------------------- key times

    @Test
    fun keyTimeRowDropsZeroLengthIntervals() {
        val zero = NightSpan(darkStart, darkStart)
        val real = NightSpan(darkStart, darkStart.plusSeconds(3600))
        val row = KeyTimeRow.of(KeyTimeBand.CIVIL, listOf(zero, real))
        assertTrue(row.occurs)
        assertEquals(listOf(real), row.intervals)
        assertEquals(real, row.evening)
        assertNull(row.morning)
    }

    @Test
    fun aBandThatDoesNotOccurHasNoRow() {
        val row = KeyTimeRow.of(KeyTimeBand.NAUTICAL, listOf(NightSpan(darkStart, darkStart)))
        assertFalse(row.occurs)
        assertNull(row.evening)
    }

    @Test
    fun keyTimeBandsSplitTwilightFromGoldenAndBlueHour() {
        assertTrue(KeyTimeBand.CIVIL.isTwilight)
        assertTrue(KeyTimeBand.NAUTICAL.isTwilight)
        assertTrue(KeyTimeBand.ASTRONOMICAL.isTwilight)
        assertFalse(KeyTimeBand.GOLDEN_HOUR.isTwilight)
        assertFalse(KeyTimeBand.BLUE_HOUR.isTwilight)
    }

    @Test
    fun sunriseSunsetHasBothOnlyWhenNeitherEndIsMissing() {
        val sunset = darkStart
        val sunrise = t2
        assertTrue(SunriseSunsetTimes(sunset, sunrise).hasBoth)
        assertFalse(SunriseSunsetTimes(null, sunrise).hasBoth)
        assertFalse(SunriseSunsetTimes(sunset, null).hasBoth)
        assertFalse(SunriseSunsetTimes(null, null).hasBoth)
    }

    // --------------------------------------------------------------- SessionBrief

    private fun pass(aos: Instant, visibility: PassVisibility, sunlit: Boolean): SatellitePass = SatellitePass(
        noradId = 25544L,
        satelliteName = "ISS (ZARYA)",
        objectId = "1998-067A",
        aosUtc = aos,
        losUtc = aos.plusSeconds(240),
        maxElevationDeg = 62.5,
        maxElevationUtc = aos.plusSeconds(120),
        subPointTrack = emptyList(),
        visibility = visibility,
        observerSunAltitudeDeg = -20.0,
        satelliteSunlit = sunlit,
        elementSetEpochUtc = aos.minus(Duration.ofHours(6)),
        elementSetAge = ElementSetAge(Duration.ofHours(6), ElementSetAgeBand.FRESH),
    )

    private fun darkReport(): DarkWindowReport = DarkWindowReport(
        kind = DarkWindowKind.DARK,
        darkWindows = listOf(NightSpan(darkStart, t2)),
        sunAltMinDeg = -30.0,
        sunAltMinInstant = t1,
        sunAltMaxDeg = 12.0,
        sunAltMaxInstant = Instant.parse("2026-10-09T12:00:00Z"),
    )

    private fun skyGrade(): SkyGrade = SkyGrade(
        darkestSunAltitudeDeg = -30.0,
        darkestInstant = t1,
        darkWindowKind = DarkWindowKind.DARK,
        darkDuration = Duration.ofHours(6),
        moonIlluminatedFraction = 0.1,
        moonAltitudeDeg = -10.0,
    )

    private fun moonState(): MoonState = MoonState.of(
        illuminatedFraction = 0.1,
        phaseAngleDeg = 156.4,
        elongationDeg = 23.6,
        altitudeDeg = -10.0,
        azimuthDeg = 300.0,
        magnitude = -6.0,
    )

    private fun interval(from: Instant, to: Instant, boundary: WindowBoundary = WindowBoundary.RISE) =
        TargetInterval(
            startUtc = from,
            endUtc = to,
            startBoundary = boundary,
            endBoundary = WindowBoundary.SET,
            transitUtc = from.plusSeconds(1800),
            peakAltitudeDeg = 55.0,
            minimumAltitudeDeg = 12.0,
            effectiveHorizonDeg = 10.0,
        )

    private fun brief(
        targetWindows: Map<String, TargetWindow>,
        passes: List<SatellitePass>,
        keyTimes: List<KeyTimeRow> = emptyList(),
    ): SessionBrief = SessionBrief(
        spotId = "spot-1",
        spotName = "Ridge",
        tzId = "Europe/Oslo",
        latitudeDeg = 59.91,
        longitudeDeg = 10.75,
        nightDate = LocalDate.of(2026, 10, 9),
        session = NightSpan(Instant.parse("2026-10-09T10:00:00Z"), Instant.parse("2026-10-10T10:00:00Z")),
        darkWindow = darkReport(),
        keyTimes = keyTimes,
        sunriseSunset = SunriseSunsetTimes(darkStart, t2),
        moon = moonState(),
        moonless = MoonlessWindow(kind = MoonlessKind.MOONLESS_ALL_NIGHT),
        targetWindows = targetWindows,
        passes = passes,
        cloud = null,
        skyGrade = skyGrade(),
        conditions = emptyList(),
    )

    @Test
    fun visibleTargetIdsKeepsOnlyTheTargetsThatAreUpTonight() {
        val up = TargetWindow.Circumpolar(minimumAltitudeDeg = 30.0, atInstant = t1, effectiveHorizonDeg = 10.0)
        val down = TargetWindow.NeverRises(effectiveHorizonDeg = 10.0, maxAltitudeDeg = 3.0)
        val visible = TargetWindow.Visible(listOf(interval(darkStart, t2)), ObjectVisibility.ABOVE)
        val b = brief(mapOf("up" to up, "down" to down, "visible" to visible), emptyList())
        assertEquals(setOf("up", "visible"), b.visibleTargetIds)
        assertTrue(b.hasTargets)
    }

    @Test
    fun aVisibleWindowOfOnlyZeroLengthIntervalsIsNotVisibleTonight() {
        val empty = TargetWindow.Visible(
            listOf(interval(darkStart, darkStart)),
            ObjectVisibility.BELOW,
        )
        assertFalse(empty.isVisibleTonight)
        assertNull(brief(mapOf("empty" to empty), emptyList()).visibleTargetIds.firstOrNull())
    }

    @Test
    fun visiblePassesKeepsOnlyTheObservableOnes() {
        val observable = pass(darkStart, PassVisibility.OBSERVABLE, sunlit = true)
        val shadowed = pass(t1, PassVisibility.IN_EARTH_SHADOW, sunlit = false)
        val bright = pass(t2, PassVisibility.TOO_BRIGHT, sunlit = true)
        val b = brief(emptyMap(), listOf(shadowed, observable, bright))
        assertEquals(listOf(observable), b.visiblePasses)
    }

    @Test
    fun keyTimeRowLooksUpTheBandThatActuallyOccurs() {
        val civil = KeyTimeRow.of(KeyTimeBand.CIVIL, listOf(NightSpan(darkStart, t1)))
        val empty = KeyTimeRow.of(KeyTimeBand.NAUTICAL, emptyList())
        val b = brief(emptyMap(), emptyList(), keyTimes = listOf(empty, civil))
        assertEquals(civil, b.keyTimeRow(KeyTimeBand.CIVIL))
        assertNull(b.keyTimeRow(KeyTimeBand.NAUTICAL))
        assertNull(b.keyTimeRow(KeyTimeBand.GOLDEN_HOUR))
    }

    @Test
    fun aPolarDayNightDoesNotOfferTheReminder() {
        val polarDay = DarkWindowReport(
            kind = DarkWindowKind.POLAR_DAY,
            darkWindows = emptyList(),
            sunAltMinDeg = 1.0,
            sunAltMinInstant = darkStart,
            sunAltMaxDeg = 20.0,
            sunAltMaxInstant = t1,
        )
        assertFalse(polarDay.reminderEligible)
        assertFalse(polarDay.hasDarkWindow)
        assertTrue(polarDay.lacksAstronomicalDarkness)
    }
}

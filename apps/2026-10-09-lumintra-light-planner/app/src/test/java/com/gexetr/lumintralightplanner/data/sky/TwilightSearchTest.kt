package com.gexetr.lumintralightplanner.data.sky

import com.gexetr.lumintralightplanner.domain.CrossingDirection
import com.gexetr.lumintralightplanner.domain.NightSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * JVM tests for the crossing solver, against **synthetic time functions whose crossings are known
 * analytically**. No ephemeris, no context, no device.
 *
 * The point of these tests is the four cases the dark window actually needs and that a single
 * `searchRiseSet`-style call cannot express:
 *
 * - one crossing pair, located to the requested tolerance;
 * - **two** intervals in one span (the spec's "two dark windows in one night", E10), which a
 *   single-interval output cannot represent;
 * - **none**, which is a case and never an error;
 * - an excursion that enters and leaves the threshold **entirely between two samples**, which is the
 *   reason the search probes the interior extremum at all.
 *
 * Every expected instant below is derived by hand from the analytic form of the function, so a
 * failure here is a failure of the solver and not of a recorded number. The one thing these tests
 * cannot claim is that the *astronomical* altitude curve has this shape; that is `NightEngineTest`'s
 * job, and its assertions there are structural for the same reason.
 */
class TwilightSearchTest {

    /** One fixed instant, so nothing here reads a clock: `now` is a parameter everywhere. */
    private val base: Instant = Instant.parse("2026-01-15T12:00:00Z")

    private fun night(hours: Long = 24L): NightSpan =
        NightSpan(base, base.plus(Duration.ofHours(hours)), SPOT_ID)

    /** Seconds from [base]; the unit every synthetic function below is written in. */
    private fun seconds(t: Instant): Double =
        Duration.between(base, t).toMillis().toDouble() / 1000.0

    private fun assertNear(
        expectedSeconds: Double,
        actual: Instant,
        toleranceMillis: Long,
        what: String,
    ) {
        val deltaMillis = Math.abs(seconds(actual) - expectedSeconds) * 1000.0
        assertTrue(
            "$what: found ${seconds(actual)} s, expected $expectedSeconds s, off by " +
                "$deltaMillis ms (tolerance $toleranceMillis ms)",
            deltaMillis <= toleranceMillis.toDouble(),
        )
    }

    /** `sin(2π·x/24 h)` on [base]: zeros at 0 s (rising) and 12 h (falling), analytically. */
    private fun dailySine(t: Instant): Double =
        Math.sin(2.0 * Math.PI * Duration.between(base, t).toMillis().toDouble() / 86_400_000.0)

    /**
     * `10·cos(2π·x/12 h)`: at or below −5 in exactly two windows, `[4 h, 8 h]` and `[16 h, 20 h]`.
     *
     * Derived: `10·cos θ ≤ −5` ⟺ `θ ∈ [2π/3, 4π/3]` ⟺ `x ∈ [T/3, 2T/3]` for the 12-hour period `T`,
     * giving `[14 400 s, 28 800 s]` and, one period later, `[57 600 s, 72 000 s]`.
     */
    private fun twoDips(t: Instant): Double =
        10.0 * Math.cos(2.0 * Math.PI * Duration.between(base, t).toMillis().toDouble() / 43_200_000.0)

    /**
     * A single smooth dip that never touches a sample: a downward parabola centred at 03:05, equal to
     * `+5` at ±300 s from its centre (i.e. at the two samples that bracket it) and to `−5` at the
     * centre. Its crossing of `−1` is derived by hand: `5 − 10·(1 − u²) = −1` ⟹
     * `u = √0.4` ⟹ the crossings sit `300·√0.4 = 189.7366 s` either side of the centre.
     */
    private fun buriedDip(t: Instant): Double {
        val dx = seconds(t) - BURIED_DIP_CENTRE_SECONDS
        if (Math.abs(dx) > BURIED_DIP_HALF_WIDTH_SECONDS) return 5.0
        val u = dx / BURIED_DIP_HALF_WIDTH_SECONDS
        return 5.0 - 10.0 * (1.0 - u * u)
    }

    @Test
    fun syntheticSineCrossingsAreFoundAtTheAnalyticInstants() {
        val span = night()

        val crossings = TwilightSearch.crossings(
            span = span,
            targetAltitudeDeg = 0.0,
            stepSeconds = 600L,
            toleranceSeconds = 1.0,
            f = ::dailySine,
        )

        assertEquals("a daily sine crosses zero twice inside one night", 2, crossings.size)
        assertEquals(CrossingDirection.RISING, crossings[0].direction)
        assertEquals(CrossingDirection.FALLING, crossings[1].direction)
        assertNear(0.0, crossings[0].instantUtc, 1_000L, "rising zero crossing at the span start")
        assertNear(43_200.0, crossings[1].instantUtc, 1_000L, "falling zero crossing at 12 h")

        val byBranch = TwilightSearch.crossingsByBranch(span, 0.0, 600L, 1.0, ::dailySine)
        assertEquals(1, byBranch.rising.size)
        assertEquals(1, byBranch.falling.size)
        assertEquals(2, byBranch.size)
        assertFalse(byBranch.isEmpty)
        assertEquals(crossings.map { it.instantUtc }, byBranch.rising + byBranch.falling)
    }

    @Test
    fun aNightWithTwoDipsYieldsTwoIntervals() {
        val span = night()
        val threshold = -5.0

        val crossings = TwilightSearch.crossings(span, threshold, 600L, 1.0, ::twoDips)
        assertEquals("two windows is two pairs of crossings", 4, crossings.size)
        assertEquals(
            listOf(
                CrossingDirection.FALLING,
                CrossingDirection.RISING,
                CrossingDirection.FALLING,
                CrossingDirection.RISING,
            ),
            crossings.map { it.direction },
        )
        assertNear(14_400.0, crossings[0].instantUtc, 1_000L, "first dip entry")
        assertNear(28_800.0, crossings[1].instantUtc, 1_000L, "first dip exit")
        assertNear(57_600.0, crossings[2].instantUtc, 1_000L, "second dip entry")
        assertNear(72_000.0, crossings[3].instantUtc, 1_000L, "second dip exit")

        val intervals = TwilightSearch.intervalsAtOrBelow(span, threshold, 600L, ::twoDips)
        assertEquals("the night carries TWO windows, not one", 2, intervals.size)
        assertNear(14_400.0, intervals[0].startUtc, 1_000L, "first window start")
        assertNear(28_800.0, intervals[0].endUtc, 1_000L, "first window end")
        assertNear(57_600.0, intervals[1].startUtc, 1_000L, "second window start")
        assertNear(72_000.0, intervals[1].endUtc, 1_000L, "second window end")
        assertTrue(
            "the two windows must be disjoint and ordered",
            intervals[0].endUtc.isBefore(intervals[1].startUtc),
        )
    }

    @Test
    fun aNightWithNoCrossingYieldsNoCrossingsAndNoIntervals() {
        val span = night()
        val flat: (Instant) -> Double = { 10.0 }

        assertTrue(TwilightSearch.crossings(span, -5.0, 600L, 1.0, flat).isEmpty())
        assertTrue(TwilightSearch.intervalsAtOrBelow(span, -5.0, 600L, flat).isEmpty())

        val above = TwilightSearch.intervalsAbove(span, -5.0, 600L, flat)
        assertEquals(1, above.size)
        assertEquals(span.duration, above[0].duration)
    }

    @Test
    fun aCrossingExactlyOnASampleBoundaryIsLocated() {
        val span = night(hours = 1L)
        val threshold = 1_800.0
        val ramp: (Instant) -> Double = { seconds(it) }

        val grid = TwilightSearch.sample(span, 600L, ramp).map { it.instantUtc }
        assertEquals("1 h at a 600 s step is 7 samples", 7, grid.size)
        assertTrue(
            "the analytic crossing must be one of the sample instants",
            grid.contains(span.startUtc.plusSeconds(1_800)),
        )

        val crossings = TwilightSearch.crossings(span, threshold, 600L, 1.0, ramp)
        assertEquals(1, crossings.size)
        assertEquals(CrossingDirection.RISING, crossings[0].direction)
        assertNear(1_800.0, crossings[0].instantUtc, 1_000L, "ramp crossing on the sample boundary")

        val intervals = TwilightSearch.intervalsAtOrBelow(span, threshold, 600L, ramp)
        assertEquals(1, intervals.size)
        assertEquals("the ramp is below the threshold from the span start", span.startUtc, intervals[0].startUtc)
        assertNear(1_800.0, intervals[0].endUtc, 1_000L, "ramp interval end")
    }

    @Test
    fun aCrossingIsRefinedToTheRequestedTolerance() {
        val span = night(hours = 1L)
        val ramp: (Instant) -> Double = { seconds(it) }

        val coarse = TwilightSearch.crossings(span, 1_234.0, 600L, 300.0, ramp).single()
        val fine = TwilightSearch.crossings(span, 1_234.0, 600L, 1.0, ramp).single()

        val coarseError = Math.abs(seconds(coarse.instantUtc) - 1_234.0)
        val fineError = Math.abs(seconds(fine.instantUtc) - 1_234.0)

        assertTrue("a 300 s tolerance may not leave more than 300 s, was $coarseError s", coarseError <= 300.0)
        assertTrue("a 1 s tolerance may not leave more than 1 s, was $fineError s", fineError <= 1.0)
        assertTrue(
            "the finer tolerance must not be worse than the coarser one ($fineError s vs $coarseError s)",
            fineError <= coarseError,
        )
        assertEquals(CrossingDirection.RISING, fine.direction)
    }

    @Test
    fun anExcursionBuriedBetweenTwoSamplesStillYieldsBothCrossings() {
        val span = night()
        val threshold = -1.0

        val samples = TwilightSearch.sample(span, 600L, ::buriedDip)
        assertTrue(
            "no sample may reach the threshold, or the test proves nothing",
            samples.all { it.altitudeDeg > threshold },
        )

        val crossings = TwilightSearch.crossings(span, threshold, 600L, 1.0, ::buriedDip)
        assertEquals("the buried dip has two crossings", 2, crossings.size)
        assertEquals(CrossingDirection.FALLING, crossings[0].direction)
        assertEquals(CrossingDirection.RISING, crossings[1].direction)

        val edge = BURIED_DIP_HALF_WIDTH_SECONDS * Math.sqrt(0.4)
        assertNear(BURIED_DIP_CENTRE_SECONDS - edge, crossings[0].instantUtc, 1_000L, "buried dip entry")
        assertNear(BURIED_DIP_CENTRE_SECONDS + edge, crossings[1].instantUtc, 1_000L, "buried dip exit")

        val intervals = TwilightSearch.intervalsAtOrBelow(span, threshold, 600L, ::buriedDip)
        assertEquals(1, intervals.size)
        assertNear(BURIED_DIP_CENTRE_SECONDS - edge, intervals[0].startUtc, 1_000L, "buried dip start")
        assertNear(BURIED_DIP_CENTRE_SECONDS + edge, intervals[0].endUtc, 1_000L, "buried dip end")
    }

    @Test
    fun theSamplingGridStepsStrictlyAndSpansTheWholeNight() {
        val span = night()
        val samples = TwilightSearch.sample(span, 600L, ::dailySine)

        assertEquals("24 h at a 600 s step is 24 segments × 6 + 1 samples", 145, samples.size)
        assertEquals(span.startUtc, samples.first().instantUtc)
        assertEquals(span.endUtc, samples.last().instantUtc)
        for (i in 1 until samples.size) {
            assertTrue(
                "sample instants must be strictly increasing at index $i",
                samples[i].instantUtc.isAfter(samples[i - 1].instantUtc),
            )
            assertEquals(
                "every step must be exactly 600 s at index $i",
                600_000L,
                Duration.between(samples[i - 1].instantUtc, samples[i].instantUtc).toMillis(),
            )
        }
    }

    @Test
    fun theExtremumFinderLocatesTheAnalyticExtremes() {
        val span = night()

        val min = TwilightSearch.extremum(span, findMax = false, stepSeconds = 600L, refineSeconds = 1.0, f = ::twoDips)
        assertEquals(-10.0, min.value, 1e-6)
        val toNearestTrough = Math.min(
            Math.abs(seconds(min.instantUtc) - 21_600.0),
            Math.abs(seconds(min.instantUtc) - 64_800.0),
        )
        assertTrue(
            "the minimum of the two-dip cosine sits at 6 h or 18 h, was ${seconds(min.instantUtc)} s",
            toNearestTrough <= 2_000.0,
        )

        val max = TwilightSearch.extremum(span, findMax = true, stepSeconds = 600L, refineSeconds = 1.0, f = ::twoDips)
        assertEquals(10.0, max.value, 1e-9)
        assertTrue(
            "the first maximum is at or within one refinement of the span start, was ${max.instantUtc}",
            Duration.between(span.startUtc, max.instantUtc).abs() <= Duration.ofSeconds(2),
        )
    }

    @Test
    fun intervalsAboveIsTheComplementOfIntervalsAtOrBelow() {
        val span = night()
        val threshold = -5.0

        val below = TwilightSearch.intervalsAtOrBelow(span, threshold, 600L, ::twoDips)
        val above = TwilightSearch.intervalsAbove(span, threshold, 600L, ::twoDips)

        val belowMillis = NightSpan.total(below).toMillis()
        val aboveMillis = NightSpan.total(above).toMillis()

        // Each of the four crossings is located to within one second (the bisection tolerance is a
        // bracket width), so two intervals carry at most a couple of seconds of slack; the tolerance is
        // stated wider than that bound on purpose rather than tuned to the observed value.
        assertEquals("two 4 h windows are 28 800 s", 28_800.0, belowMillis.toDouble() / 1000.0, 5.0)
        assertEquals("the remaining 16 h are 57 600 s", 57_600.0, aboveMillis.toDouble() / 1000.0, 5.0)
        assertEquals("2 remaining windows and 3 before the first and after the last", 3, above.size)
        assertEquals(
            "above and below must partition the span exactly",
            span.duration,
            NightSpan.total(above).plus(NightSpan.total(below)),
        )
    }

    private companion object {
        const val SPOT_ID: String = "twilight-search-test"

        /** 03:00 plus half a step: the dip's centre sits midway between two samples. */
        const val BURIED_DIP_CENTRE_SECONDS: Double = 10_800.0 + 300.0

        const val BURIED_DIP_HALF_WIDTH_SECONDS: Double = 300.0
    }
}

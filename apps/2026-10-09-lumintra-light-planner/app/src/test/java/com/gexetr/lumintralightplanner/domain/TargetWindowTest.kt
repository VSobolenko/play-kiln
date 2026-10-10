package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The typed target window.
 *
 * `specs/targets/spec.md` / `domain/TargetWindow.kt`: the classification is the whole decision —
 * `min >= effectiveHorizon` → `Circumpolar` (up all night, no rise or set), `max < effectiveHorizon` →
 * `NeverRises` (the UI must say so rather than render an empty panel), otherwise the crossings become
 * `Visible` intervals. The "visible tonight" filter is a predicate on the window, so an empty interval
 * is never shown as a window.
 */
class TargetWindowTest {

    private val t0: Instant = Instant.parse("2026-10-09T20:00:00Z")
    private val t1: Instant = Instant.parse("2026-10-09T23:00:00Z")
    private val t2: Instant = Instant.parse("2026-10-10T02:00:00Z")

    private fun interval(
        from: Instant = t0,
        to: Instant = t1,
        startBoundary: WindowBoundary = WindowBoundary.RISE,
        endBoundary: WindowBoundary = WindowBoundary.SET,
        peakAltitudeDeg: Double = 55.0,
        effectiveHorizonDeg: Double = 10.0,
    ) = TargetInterval(
        startUtc = from,
        endUtc = to,
        startBoundary = startBoundary,
        endBoundary = endBoundary,
        transitUtc = from.plusSeconds(1800),
        peakAltitudeDeg = peakAltitudeDeg,
        minimumAltitudeDeg = effectiveHorizonDeg + 0.5,
        effectiveHorizonDeg = effectiveHorizonDeg,
    )

    // ------------------------------------------------------------------ classification

    @Test
    fun aMinimumAtOrAboveTheHorizonIsCircumpolar() {
        val window = TargetWindow.of(
            minimumAltitudeDeg = 30.0,
            minimumAtInstant = t1,
            maximumAltitudeDeg = 62.0,
            effectiveHorizonDeg = 10.0,
            intervals = emptyList(),
        )
        assertTrue(window is TargetWindow.Circumpolar)
        val circumpolar = window as TargetWindow.Circumpolar
        assertEquals(30.0, circumpolar.minimumAltitudeDeg, 1e-12)
        assertEquals(t1, circumpolar.atInstant)
        assertEquals(10.0, circumpolar.effectiveHorizonDeg, 1e-12)
        assertTrue(window.isVisibleTonight)
    }

    @Test
    fun aMinimumExactlyAtTheHorizonIsCircumpolar() {
        val window = TargetWindow.of(10.0, t1, 40.0, 10.0, emptyList())
        assertTrue(window is TargetWindow.Circumpolar)
        assertTrue(window.isVisibleTonight)
    }

    @Test
    fun aMaximumBelowTheHorizonNeverRises() {
        val window = TargetWindow.of(
            minimumAltitudeDeg = -40.0,
            minimumAtInstant = t1,
            maximumAltitudeDeg = 3.0,
            effectiveHorizonDeg = 10.0,
            intervals = emptyList(),
        )
        assertTrue(window is TargetWindow.NeverRises)
        val never = window as TargetWindow.NeverRises
        assertEquals(10.0, never.effectiveHorizonDeg, 1e-12)
        assertEquals(3.0, never.maxAltitudeDeg, 1e-12)
        assertFalse(window.isVisibleTonight)
    }

    /** `max < horizon` is strict, so a maximum exactly at the horizon is a (grazing) window. */
    @Test
    fun aMaximumExactlyAtTheHorizonIsNotNeverRises() {
        val window = TargetWindow.of(9.0, t1, 10.0, 10.0, listOf(interval()))
        assertTrue(window is TargetWindow.Visible)
        assertTrue(window.isVisibleTonight)
    }

    @Test
    fun anythingElseBecomesVisibleWithItsIntervals() {
        val first = interval(t0, t1)
        val second = interval(t1, t2)
        val window = TargetWindow.of(5.0, t0, 62.0, 10.0, listOf(first, second))
        assertTrue(window is TargetWindow.Visible)
        val visible = window as TargetWindow.Visible
        assertEquals(listOf(first, second), visible.intervals)
        assertEquals(ObjectVisibility.ABOVE, visible.visibility)
        assertTrue(window.isVisibleTonight)
    }

    @Test
    fun visibleDropsZeroLengthIntervalsAndReportsBelowWhenNoneRemain() {
        val zero = interval(t0, t0)
        val window = TargetWindow.of(5.0, t0, 62.0, 10.0, listOf(zero))
        val visible = window as TargetWindow.Visible
        assertTrue(visible.intervals.isEmpty())
        assertEquals(ObjectVisibility.BELOW, visible.visibility)
        assertFalse(window.isVisibleTonight)
        assertNull(visible.bestInterval)
        assertNull(visible.bestSpan)
        assertFalse(visible.onlyGrazing)
    }

    @Test
    fun twoWindowsInOneNightAreBothKept() {
        val first = interval(t0, t1)
        val second = interval(t1.plusSeconds(3600), t2)
        val window = TargetWindow.of(5.0, t0, 62.0, 10.0, listOf(first, second)) as TargetWindow.Visible
        assertEquals(2, window.intervals.size)
        // The brief states the longest one first.
        assertEquals(first, window.bestInterval)
    }

    @Test
    fun bestIntervalIsTheLongestOne() {
        val short = interval(t0, t0.plusSeconds(900))
        val long = interval(t1, t2)
        val window = TargetWindow.of(5.0, t0, 62.0, 10.0, listOf(short, long)) as TargetWindow.Visible
        assertEquals(long, window.bestInterval)
        assertEquals(long.span, window.bestSpan)
    }

    // -------------------------------------------------------------------- intervals

    @Test
    fun anIntervalKnowsItsDurationAndWhetherItIsEmpty() {
        val i = interval(t0, t1)
        assertEquals(Duration.ofHours(3), i.duration)
        assertFalse(i.isEmpty)
        assertEquals(NightSpan(t0, t1), i.span)
        assertTrue(interval(t0, t0).isEmpty)
        assertEquals(Duration.ZERO, interval(t0, t0).duration)
    }

    @Test
    fun anEndBeforeTheStartIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { interval(t1, t0) }
    }

    @Test
    fun isPartialIsTrueWhenEitherEndWasClippedByTheDarkWindow() {
        assertFalse(interval().isPartial)
        assertTrue(interval(startBoundary = WindowBoundary.DARK_WINDOW_START).isPartial)
        assertTrue(interval(endBoundary = WindowBoundary.DARK_WINDOW_END).isPartial)
        assertTrue(interval(startBoundary = WindowBoundary.SESSION_START).isPartial)
        assertTrue(interval(endBoundary = WindowBoundary.SESSION_END).isPartial)
        assertFalse(
            interval(startBoundary = WindowBoundary.RISE, endBoundary = WindowBoundary.SET).isPartial,
        )
    }

    @Test
    fun theClippedBoundariesAreExactlyTheFourClippingOnes() {
        assertFalse(WindowBoundary.RISE.isClipped)
        assertFalse(WindowBoundary.SET.isClipped)
        assertTrue(WindowBoundary.DARK_WINDOW_START.isClipped)
        assertTrue(WindowBoundary.DARK_WINDOW_END.isClipped)
        assertTrue(WindowBoundary.SESSION_START.isClipped)
        assertTrue(WindowBoundary.SESSION_END.isClipped)
    }

    @Test
    fun isGrazingIsWithinFiveHundredthsOfADegreeOfTheHorizon() {
        val tol = TargetInterval.GRAZE_TOLERANCE_DEG
        assertEquals(0.05, tol, 1e-12)
        // Exact equality is grazing. Stay clearly inside / outside the tolerance — `10.05` is not
        // an exact binary float, so `abs(10.05 - 10.0)` is slightly above 0.05 and must not be used
        // as the inclusive boundary probe.
        assertTrue(interval(peakAltitudeDeg = 10.0, effectiveHorizonDeg = 10.0).isGrazing)
        assertTrue(interval(peakAltitudeDeg = 10.0 + tol * 0.5, effectiveHorizonDeg = 10.0).isGrazing)
        assertTrue(interval(peakAltitudeDeg = 10.0 - tol * 0.5, effectiveHorizonDeg = 10.0).isGrazing)
        assertFalse(interval(peakAltitudeDeg = 10.0 + tol * 1.2, effectiveHorizonDeg = 10.0).isGrazing)
        assertFalse(interval(peakAltitudeDeg = 20.0, effectiveHorizonDeg = 10.0).isGrazing)
    }

    @Test
    fun onlyGrazingIsTrueWhenEveryIntervalGraze() {
        val grazing = interval(t0, t1, peakAltitudeDeg = 10.01, effectiveHorizonDeg = 10.0)
        val real = interval(t1, t2, peakAltitudeDeg = 40.0, effectiveHorizonDeg = 10.0)
        val allGraze = TargetWindow.of(9.0, t0, 10.2, 10.0, listOf(grazing)) as TargetWindow.Visible
        assertTrue(allGraze.onlyGrazing)
        val mixed = TargetWindow.of(9.0, t0, 40.0, 10.0, listOf(grazing, real)) as TargetWindow.Visible
        assertFalse(mixed.onlyGrazing)
    }

    @Test
    fun anIntervalStatesTheHorizonItCleared() {
        val atDrawn = TargetInterval(
            startUtc = t0,
            endUtc = t1,
            startBoundary = WindowBoundary.RISE,
            endBoundary = WindowBoundary.SET,
            transitUtc = t1,
            peakAltitudeDeg = 10.4,
            minimumAltitudeDeg = 10.2,
            effectiveHorizonDeg = 10.2,
            clearsDrawnHorizon = true,
        )
        assertTrue(atDrawn.clearsDrawnHorizon)
        assertEquals(NightSpan(t0, t1, null), atDrawn.span)
    }

    @Test
    fun anIntervalHasNoTransitWhenTheSearchFoundNone() {
        val noTransit = TargetInterval(
            startUtc = t0,
            endUtc = t1,
            startBoundary = WindowBoundary.RISE,
            endBoundary = WindowBoundary.SET,
            transitUtc = null,
            peakAltitudeDeg = 30.0,
            minimumAltitudeDeg = 12.0,
            effectiveHorizonDeg = 10.0,
        )
        assertNull(noTransit.transitUtc)
    }
}

package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The time model: [NightSpan] arithmetic, [SceneScrub], and the UTC-only duration rule.
 *
 * `specs/session-brief/spec.md` / design.md "Time model" (E67): a span is an absolute UTC half-open
 * interval, and a duration is always the difference of two UTC instants — **never** the difference of
 * two formatted local strings, which would silently be an hour wrong across a DST change.
 */
class TimeAndScrubTest {

    private val start: Instant = Instant.parse("2026-10-09T18:00:00Z")
    private val end: Instant = Instant.parse("2026-10-10T06:00:00Z")
    private val span = NightSpan(start, end)

    // ------------------------------------------------------------------ NightSpan

    @Test
    fun durationIsTheUtcDifference() {
        assertEquals(Duration.ofHours(12), span.duration)
        assertEquals(43_200_000L, span.duration.toMillis())
    }

    @Test
    fun halfOpenMembershipIncludesTheStartAndExcludesTheEnd() {
        assertTrue(start in span)
        assertTrue(start.plusSeconds(1) in span)
        assertFalse(end in span)
        assertFalse(start.minusSeconds(1) in span)
    }

    @Test
    fun aZeroLengthSpanIsEmptyAndIsDroppedByNormalise() {
        val zero = NightSpan(start, start)
        assertTrue(zero.isEmpty)
        assertEquals(Duration.ZERO, zero.duration)
        assertEquals(listOf(span), NightSpan.normalise(listOf(zero, span)))
    }

    @Test
    fun anEndBeforeTheStartIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NightSpan(end, start)
        }
    }

    /**
     * The UTC rule, made falsifiable by a real DST change: in `Europe/Oslo` on 2026-03-29 the offset
     * moves from +01:00 to +02:00 at 01:00 UTC. The span below is **9 hours in UTC** and **10 hours
     * between its two local clock readings**, so a duration computed from formatted strings would be
     * an hour wrong.
     */
    @Test
    fun durationIsUtcNotTheDifferenceOfLocalClockReadings() {
        val oslo = ZoneId.of("Europe/Oslo")
        val dstSpan = NightSpan(
            Instant.parse("2026-03-28T20:00:00Z"),
            Instant.parse("2026-03-29T05:00:00Z"),
        )
        // The span crosses the CET→CEST transition: 20:00Z is 21:00 local (+01:00) and 05:00Z is
        // 07:00 local (+02:00), so the two clock readings are 10 h apart and the UTC duration is 9 h.
        assertEquals(Duration.ofHours(9), dstSpan.duration)

        val localStart = ZonedDateTime.ofInstant(dstSpan.startUtc, oslo)
        val localEnd = ZonedDateTime.ofInstant(dstSpan.endUtc, oslo)
        assertEquals(21, localStart.hour) // 20:00Z = 21:00 CET
        assertEquals(7, localEnd.hour) // 05:00Z = 07:00 CEST
        val localClockHours = ((localEnd.hour - localStart.hour) + 24) % 24
        assertEquals(10, localClockHours)
        assertNotEquals(localClockHours.toLong(), dstSpan.duration.toHours())
    }

    @Test
    fun instantAtClampsTheFractionToTheSpanEnds() {
        assertEquals(start, span.instantAt(0.0))
        assertEquals(end, span.instantAt(1.0))
        assertEquals(start.plusSeconds(6 * 3600), span.instantAt(0.5))
        assertEquals(start, span.instantAt(-0.5))
        assertEquals(end, span.instantAt(1.5))
    }

    @Test
    fun fractionOfInvertsInstantAtAndClamps() {
        assertEquals(0.5, span.fractionOf(span.instantAt(0.5)), 1e-12)
        assertEquals(0.0, span.fractionOf(start.minusSeconds(3600)), 1e-12)
        assertEquals(1.0, span.fractionOf(end.plusSeconds(3600)), 1e-12)
        // A zero-length span has no fraction to speak of, and answers 0 rather than dividing by zero.
        assertEquals(0.0, NightSpan(start, start).fractionOf(start), 1e-12)
    }

    @Test
    fun cappedToTruncatesAtTheMaximumLength() {
        val capped = span.cappedTo(Duration.ofHours(4))
        assertEquals(start, capped.startUtc)
        assertEquals(start.plus(Duration.ofHours(4)), capped.endUtc)
        // Already shorter than the cap: unchanged.
        assertEquals(span, span.cappedTo(Duration.ofHours(24)))
    }

    @Test
    fun intersectAndSubtractAreHalfOpen() {
        val other = NightSpan(start.plusSeconds(3600), end.minusSeconds(3600))
        assertEquals(other, span.intersect(other)!!)
        assertNull(span.intersect(NightSpan(end, end.plusSeconds(60))))
        val remainder = NightSpan.subtract(listOf(span), listOf(other))
        assertEquals(2, remainder.size)
        assertEquals(start, remainder[0].startUtc)
        assertEquals(end, remainder[1].endUtc)
    }

    @Test
    fun longestAndTotalPickTheRightSpans() {
        val short = NightSpan(start, start.plusSeconds(600))
        assertEquals(span, NightSpan.longest(listOf(short, span)))
        assertNull(NightSpan.longest(emptyList()))
        assertEquals(
            span.duration.plus(short.duration),
            NightSpan.total(listOf(span, short)),
        )
    }

    // ----------------------------------------------------------------- SceneScrub

    @Test
    fun forValueEndpointsLandOnTheSpanEnds() {
        assertEquals(span.startUtc, SceneScrub.forValue(0.0, span).instantUtc)
        assertEquals(span.endUtc, SceneScrub.forValue(1.0, span).instantUtc)
        assertEquals(start.plusSeconds(6 * 3600), SceneScrub.forValue(0.5, span).instantUtc)
    }

    @Test
    fun forValueClampsAValueOutsideZeroOne() {
        assertEquals(0.0, SceneScrub.forValue(-1.0, span).value, 1e-12)
        assertEquals(span.startUtc, SceneScrub.forValue(-1.0, span).instantUtc)
        assertEquals(1.0, SceneScrub.forValue(2.0, span).value, 1e-12)
        assertEquals(span.endUtc, SceneScrub.forValue(2.0, span).instantUtc)
    }

    @Test
    fun forInstantInvertsForValueInsideTheSpan() {
        val instant = start.plusSeconds(9 * 3600)
        val scrub = SceneScrub.forInstant(instant, span)
        assertEquals(0.75, scrub.value, 1e-12)
        assertEquals(instant, scrub.instantUtc)
        assertEquals(instant, SceneScrub.forValue(scrub.value, span).instantUtc)
    }

    @Test
    fun forInstantOnAZeroLengthSpanAnswersZeroAtTheStart() {
        val zero = NightSpan(start, start)
        val scrub = SceneScrub.forInstant(start.plusSeconds(7200), zero)
        assertEquals(0.0, scrub.value, 1e-12)
        assertEquals(start, scrub.instantUtc)
    }

    /**
     * **Documents a real inconsistency in `SceneScrub.forInstant`** (`domain/SceneState.kt`, the
     * `SceneScrub.forInstant` body): the `value` is clamped into `[0, 1]` but the `instantUtc` it
     * carries is **not**, so for an instant outside the span the pair disagrees with itself —
     * `value == 1.0` while `instantUtc` is past `span.endUtc`. Every other construction path
     * (`forValue`) keeps the two consistent, so the invariant "value determines the instant" is
     * broken by exactly this branch. The correct behaviour is asserted here; the class is fixed by
     * the orchestrator, not here.
     */
    @Test
    fun forInstantClampsTheInstantItCarriesAlongWithTheValue() {
        val beyond = end.plusSeconds(3600)
        val scrub = SceneScrub.forInstant(beyond, span)
        assertEquals(1.0, scrub.value, 1e-12)
        assertEquals(span.endUtc, scrub.instantUtc)
    }

    @Test
    fun aScrubValueOutsideZeroOneCannotBeConstructedDirectly() {
        assertThrows(IllegalArgumentException::class.java) {
            SceneScrub(1.4, span.endUtc)
        }
    }

    // ----------------------------------------------------------------- SessionClock

    @Test
    fun sessionWindowIsAlwaysTwentyFourHoursRegardlessOfTheZone() {
        val oslo = ZoneId.of("Europe/Oslo")
        val window = SessionClock.sessionWindow(LocalDate.of(2026, 10, 9), oslo, "spot-1")
        assertEquals(Duration.ofHours(24), window.duration)
        // Local noon on 2026-10-09 in Europe/Oslo (CEST, +02:00) is 10:00 UTC.
        assertEquals(Instant.parse("2026-10-09T10:00:00Z"), window.startUtc)
        assertEquals(Instant.parse("2026-10-10T10:00:00Z"), window.endUtc)
        assertEquals("spot-1", window.spotId)
        // The midnight instant a user would call "that night" is inside the window.
        assertTrue(Instant.parse("2026-10-09T22:00:00Z") in window)
    }

    @Test
    fun nightOfRollsBackBeforeLocalNoon() {
        val oslo = ZoneId.of("Europe/Oslo")
        // 01:30 local on 2026-10-10 is still the night that began on 2026-10-09.
        val beforeNoon = Instant.parse("2026-10-09T23:30:00Z") // 01:30 CEST on 2026-10-10
        assertEquals(LocalDate.of(2026, 10, 9), SessionClock.nightOf(beforeNoon, oslo))
        val afterNoon = Instant.parse("2026-10-10T12:00:00Z") // 14:00 CEST
        assertEquals(LocalDate.of(2026, 10, 10), SessionClock.nightOf(afterNoon, oslo))
    }

    @Test
    fun dateLabelIsTheLocalDateOfTheUtcStart() {
        val oslo = ZoneId.of("Europe/Oslo")
        val window = SessionClock.sessionWindow(LocalDate.of(2026, 10, 9), oslo)
        assertEquals(LocalDate.of(2026, 10, 9), SessionClock.dateLabel(window, oslo))
    }
}

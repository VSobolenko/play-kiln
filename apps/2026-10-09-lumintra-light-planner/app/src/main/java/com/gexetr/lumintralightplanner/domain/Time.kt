package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * An absolute UTC half-open interval `[startUtc, endUtc)`.
 *
 * No local clock time is ever stored: a local time exists only as a formatted label computed with the
 * spot's [ZoneId]. Durations are always the difference of the two UTC instants, never of two formatted
 * strings (design.md "Time model", E67).
 */
data class NightSpan(
    val startUtc: Instant,
    val endUtc: Instant,
    val spotId: String? = null,
) {
    init {
        require(!endUtc.isBefore(startUtc)) { "NightSpan end $endUtc is before start $startUtc" }
    }

    val duration: Duration get() = Duration.between(startUtc, endUtc)

    /** A zero-length span is never rendered as an interval (E11); callers drop it. */
    val isEmpty: Boolean get() = startUtc == endUtc

    /** Half-open membership: the start belongs to the span, the end does not. */
    operator fun contains(instant: Instant): Boolean =
        !instant.isBefore(startUtc) && instant.isBefore(endUtc)

    /** The overlap with [other], or `null` when the overlap is empty. */
    fun intersect(other: NightSpan): NightSpan? {
        val s = if (startUtc.isAfter(other.startUtc)) startUtc else other.startUtc
        val e = if (endUtc.isBefore(other.endUtc)) endUtc else other.endUtc
        return if (s.isBefore(e)) NightSpan(s, e, spotId) else null
    }

    /** This span truncated so that it is at most [max] long (the satellite search caps at 24 h). */
    fun cappedTo(max: Duration): NightSpan =
        if (duration <= max) this else NightSpan(startUtc, startUtc.plus(max), spotId)

    /** The instant at fraction [f] of the span, `f` clamped to `[0, 1]`. */
    fun instantAt(f: Double): Instant {
        val c = f.coerceIn(0.0, 1.0)
        val millis = (duration.toMillis() * c).toLong()
        return startUtc.plusMillis(millis)
    }

    /** The fraction of the span at which [instant] sits, clamped to `[0, 1]`. */
    fun fractionOf(instant: Instant): Double {
        val total = duration.toMillis()
        if (total <= 0L) return 0.0
        return (Duration.between(startUtc, instant).toMillis().toDouble() / total).coerceIn(0.0, 1.0)
    }

    companion object {
        /** Drops empty spans and returns the rest in chronological order. */
        fun normalise(spans: List<NightSpan>): List<NightSpan> =
            spans.filter { !it.isEmpty }.sortedBy { it.startUtc }

        /** Intersection of two sorted span lists. */
        fun intersectAll(a: List<NightSpan>, b: List<NightSpan>): List<NightSpan> {
            val out = ArrayList<NightSpan>()
            for (x in a) for (y in b) x.intersect(y)?.let { out.add(it) }
            return normalise(out)
        }

        /** `a` minus `b`: the parts of every span in [a] covered by no span in [b]. */
        fun subtract(a: List<NightSpan>, b: List<NightSpan>): List<NightSpan> {
            var current = normalise(a)
            for (cut in normalise(b)) {
                val next = ArrayList<NightSpan>()
                for (span in current) {
                    val overlap = span.intersect(cut)
                    if (overlap == null) {
                        next.add(span)
                    } else {
                        if (span.startUtc.isBefore(overlap.startUtc)) {
                            next.add(NightSpan(span.startUtc, overlap.startUtc, span.spotId))
                        }
                        if (overlap.endUtc.isBefore(span.endUtc)) {
                            next.add(NightSpan(overlap.endUtc, span.endUtc, span.spotId))
                        }
                    }
                }
                current = next
            }
            return normalise(current)
        }

        /** The longest span of [spans], or `null` when there is none. */
        fun longest(spans: List<NightSpan>): NightSpan? = spans.maxByOrNull { it.duration }

        /** The sum of the durations of [spans]. */
        fun total(spans: List<NightSpan>): Duration =
            spans.fold(Duration.ZERO) { acc, s -> acc.plus(s.duration) }
    }
}

/** What produced an [EventInstant]. */
enum class EventKind {
    SUNRISE,
    SUNSET,
    TWILIGHT_BOUNDARY,
    DARK_START,
    DARK_END,
    MOONRISE,
    MOONSET,
    RISE,
    SET,
    TRANSIT,
    PASS_AOS,
    PASS_PEAK,
    PASS_LOS,
    SUN_LOWEST,
    SUN_HIGHEST,
}

/** An absolute UTC instant together with the event that produced it. */
data class EventInstant(
    val instantUtc: Instant,
    val kind: EventKind,
    val subject: String? = null,
)

/**
 * The session clock: a session's night is the spot's local-noon-to-local-noon window, so the sunset on
 * local day N and the sunrise on N+1 are one session (design.md "Time model").
 */
object SessionClock {
    private val LOCAL_NOON: LocalTime = LocalTime.NOON

    /** The session window `[localNoon, localNoon + 24 h)` for the night that begins on [nightDate]. */
    fun sessionWindow(nightDate: LocalDate, zone: ZoneId, spotId: String? = null): NightSpan {
        val start = nightDate.atTime(LOCAL_NOON).atZone(zone).toInstant()
        return NightSpan(start, start.plus(Duration.ofHours(24)), spotId)
    }

    /**
     * The night a given instant belongs to: before local noon it is the night that began on the
     * previous local date, so asking at 01:30 resolves to the night in progress (E06, E66, E67).
     */
    fun nightOf(instant: Instant, zone: ZoneId): LocalDate {
        val local = instant.atZone(zone)
        return if (local.toLocalTime().isBefore(LOCAL_NOON)) local.toLocalDate().minusDays(1) else local.toLocalDate()
    }

    /** The session's `dateLabel`: the local date of the UTC start instant in the spot's zone (E09). */
    fun dateLabel(session: NightSpan, zone: ZoneId): LocalDate = session.startUtc.atZone(zone).toLocalDate()
}

/** The direction in which a function crosses a threshold. */
enum class CrossingDirection { RISING, FALLING }

/** One threshold crossing found by [CrossingSearch]. */
data class Crossing(val instantUtc: Instant, val direction: CrossingDirection)

/** The extreme value of a function over a span and the instant it occurs. */
data class Extremum(val value: Double, val instantUtc: Instant)

/**
 * The one event-search primitive of the app, implementing design.md "The event-search rule":
 * **no event search is ever a single call over a long window.** The window is walked in segments of
 * at most [SEGMENT_SECONDS]; inside each segment the function is stepped at [DEFAULT_STEP_SECONDS] and
 * every sign change is refined by bisection to [DEFAULT_TOLERANCE_SECONDS]. A local extremum between two
 * samples is refined as well, so an excursion shorter than one step (the short polar twilight dip, E10)
 * still yields both of its crossings. The result is always a list of crossings, never a single one.
 *
 * The function is supplied by the caller (the sky engines live in `data/sky/`), so this object is pure.
 */
object CrossingSearch {
    const val SEGMENT_SECONDS: Long = 3_600L
    const val DEFAULT_STEP_SECONDS: Long = 600L
    const val DEFAULT_TOLERANCE_SECONDS: Double = 1.0
    private const val GOLDEN = 0.6180339887498949

    /** The sample instants: every segment `[t, min(t + 3600 s, end)]` stepped at [stepSeconds]. */
    fun sampleInstants(span: NightSpan, stepSeconds: Long = DEFAULT_STEP_SECONDS): List<Instant> {
        require(stepSeconds in 1..SEGMENT_SECONDS) { "step must be in 1..$SEGMENT_SECONDS s" }
        val out = ArrayList<Instant>()
        var segmentStart = span.startUtc
        while (segmentStart.isBefore(span.endUtc)) {
            var segmentEnd = segmentStart.plusSeconds(SEGMENT_SECONDS)
            if (segmentEnd.isAfter(span.endUtc)) segmentEnd = span.endUtc
            var t = segmentStart
            while (t.isBefore(segmentEnd)) {
                if (out.isEmpty() || out.last() != t) out.add(t)
                t = t.plusSeconds(stepSeconds)
            }
            if (out.last() != segmentEnd) out.add(segmentEnd)
            segmentStart = segmentEnd
        }
        if (out.isEmpty()) out.add(span.startUtc)
        return out
    }

    /**
     * Every crossing of [threshold] by [f] inside [span], in chronological order. `f(t) <= threshold`
     * counts as "at or below", so a FALLING crossing enters the at-or-below set.
     */
    fun crossings(
        span: NightSpan,
        threshold: Double,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        toleranceSeconds: Double = DEFAULT_TOLERANCE_SECONDS,
        f: (Instant) -> Double,
    ): List<Crossing> {
        if (span.isEmpty) return emptyList()
        val times = sampleInstants(span, stepSeconds)
        val values = DoubleArray(times.size) { f(times[it]) - threshold }
        val out = ArrayList<Crossing>()
        for (i in 0 until times.size - 1) {
            val a = times[i]
            val b = times[i + 1]
            val ga = values[i]
            val gb = values[i + 1]
            val belowA = ga <= 0.0
            val belowB = gb <= 0.0
            if (belowA != belowB) {
                val t = bisect(a, b, belowA, toleranceSeconds) { f(it) - threshold }
                out.add(Crossing(t, if (belowA) CrossingDirection.RISING else CrossingDirection.FALLING))
                continue
            }
            // Same side at both samples: a short excursion through the threshold can hide between them.
            // Probe the interior extremum on the side that would cross.
            val prev = if (i > 0) values[i - 1] else Double.NaN
            val next = if (i + 2 < values.size) values[i + 2] else Double.NaN
            if (!belowA) {
                // Both above: look for a dip below the threshold.
                val dipCandidate = (prev.isNaN() || ga <= prev || gb <= next || next.isNaN())
                if (!dipCandidate) continue
                val ext = goldenExtremum(a, b, findMax = false, toleranceSeconds) { f(it) - threshold }
                if (ext.value <= 0.0 && ext.instantUtc.isAfter(a) && ext.instantUtc.isBefore(b)) {
                    val t1 = bisect(a, ext.instantUtc, false, toleranceSeconds) { f(it) - threshold }
                    val t2 = bisect(ext.instantUtc, b, true, toleranceSeconds) { f(it) - threshold }
                    out.add(Crossing(t1, CrossingDirection.FALLING))
                    out.add(Crossing(t2, CrossingDirection.RISING))
                }
            } else {
                val bumpCandidate = (prev.isNaN() || ga >= prev || gb >= next || next.isNaN())
                if (!bumpCandidate) continue
                val ext = goldenExtremum(a, b, findMax = true, toleranceSeconds) { f(it) - threshold }
                if (ext.value > 0.0 && ext.instantUtc.isAfter(a) && ext.instantUtc.isBefore(b)) {
                    val t1 = bisect(a, ext.instantUtc, true, toleranceSeconds) { f(it) - threshold }
                    val t2 = bisect(ext.instantUtc, b, false, toleranceSeconds) { f(it) - threshold }
                    out.add(Crossing(t1, CrossingDirection.RISING))
                    out.add(Crossing(t2, CrossingDirection.FALLING))
                }
            }
        }
        return dedupe(out)
    }

    /**
     * The sub-intervals of [span] in which `f(t) <= threshold`, as half-open spans; empty spans dropped.
     */
    fun intervalsAtOrBelow(
        span: NightSpan,
        threshold: Double,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        f: (Instant) -> Double,
    ): List<NightSpan> {
        if (span.isEmpty) return emptyList()
        val list = crossings(span, threshold, stepSeconds, DEFAULT_TOLERANCE_SECONDS, f)
        val out = ArrayList<NightSpan>()
        var inside = f(span.startUtc) <= threshold
        var openedAt: Instant? = if (inside) span.startUtc else null
        for (c in list) {
            if (c.direction == CrossingDirection.FALLING && !inside) {
                inside = true
                openedAt = c.instantUtc
            } else if (c.direction == CrossingDirection.RISING && inside) {
                val s = openedAt ?: span.startUtc
                if (s.isBefore(c.instantUtc)) out.add(NightSpan(s, c.instantUtc, span.spotId))
                inside = false
                openedAt = null
            }
        }
        if (inside) {
            val s = openedAt ?: span.startUtc
            if (s.isBefore(span.endUtc)) out.add(NightSpan(s, span.endUtc, span.spotId))
        }
        return NightSpan.normalise(out)
    }

    /** The sub-intervals of [span] in which `f(t) > threshold`. */
    fun intervalsAbove(
        span: NightSpan,
        threshold: Double,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        f: (Instant) -> Double,
    ): List<NightSpan> = NightSpan.subtract(listOf(span), intervalsAtOrBelow(span, threshold, stepSeconds, f))

    /**
     * The minimum (or maximum) of [f] over [span]: stepped, then the best bracket refined by golden
     * section to [refineSeconds].
     */
    fun extremum(
        span: NightSpan,
        findMax: Boolean,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        refineSeconds: Double = DEFAULT_TOLERANCE_SECONDS,
        f: (Instant) -> Double,
    ): Extremum {
        val times = sampleInstants(span, stepSeconds)
        val values = DoubleArray(times.size) { f(times[it]) }
        var best = 0
        for (i in values.indices) {
            if (if (findMax) values[i] > values[best] else values[i] < values[best]) best = i
        }
        val lo = times[maxOf(0, best - 1)]
        val hi = times[minOf(times.size - 1, best + 1)]
        if (!lo.isBefore(hi)) return Extremum(values[best], times[best])
        val refined = goldenExtremum(lo, hi, findMax, refineSeconds, f)
        val better = if (findMax) refined.value >= values[best] else refined.value <= values[best]
        return if (better) refined else Extremum(values[best], times[best])
    }

    private fun bisect(
        a0: Instant,
        b0: Instant,
        belowAtA: Boolean,
        toleranceSeconds: Double,
        g: (Instant) -> Double,
    ): Instant {
        var a = a0
        var b = b0
        val tolMillis = (toleranceSeconds * 1000.0).toLong().coerceAtLeast(1L)
        while (Duration.between(a, b).toMillis() > tolMillis) {
            val mid = a.plusMillis(Duration.between(a, b).toMillis() / 2)
            val belowMid = g(mid) <= 0.0
            if (belowMid == belowAtA) a = mid else b = mid
        }
        return a.plusMillis(Duration.between(a, b).toMillis() / 2)
    }

    private fun goldenExtremum(
        a0: Instant,
        b0: Instant,
        findMax: Boolean,
        toleranceSeconds: Double,
        f: (Instant) -> Double,
    ): Extremum {
        val sign = if (findMax) -1.0 else 1.0
        var a = a0.toEpochMilli().toDouble()
        var b = b0.toEpochMilli().toDouble()
        fun at(ms: Double): Double = sign * f(Instant.ofEpochMilli(ms.toLong()))
        var c = b - GOLDEN * (b - a)
        var d = a + GOLDEN * (b - a)
        var fc = at(c)
        var fd = at(d)
        val tol = toleranceSeconds * 1000.0
        var guard = 0
        while (abs(b - a) > tol && guard < 200) {
            if (fc < fd) {
                b = d; d = c; fd = fc
                c = b - GOLDEN * (b - a); fc = at(c)
            } else {
                a = c; c = d; fc = fd
                d = a + GOLDEN * (b - a); fd = at(d)
            }
            guard++
        }
        val ms = (a + b) / 2.0
        val t = Instant.ofEpochMilli(ms.toLong())
        return Extremum(f(t), t)
    }

    private fun dedupe(list: List<Crossing>): List<Crossing> {
        val sorted = list.sortedBy { it.instantUtc }
        val out = ArrayList<Crossing>()
        for (c in sorted) {
            val last = out.lastOrNull()
            if (last != null && last.direction == c.direction &&
                Duration.between(last.instantUtc, c.instantUtc).abs().toMillis() <= 2_000L
            ) continue
            out.add(c)
        }
        return out
    }
}

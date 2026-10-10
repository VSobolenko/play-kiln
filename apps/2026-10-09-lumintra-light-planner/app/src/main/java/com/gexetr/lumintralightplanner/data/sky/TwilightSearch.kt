package com.gexetr.lumintralightplanner.data.sky

import com.gexetr.lumintralightplanner.domain.Crossing
import com.gexetr.lumintralightplanner.domain.CrossingDirection
import com.gexetr.lumintralightplanner.domain.CrossingSearch
import com.gexetr.lumintralightplanner.domain.Extremum
import com.gexetr.lumintralightplanner.domain.NightSpan
import java.time.Instant

/** The crossings of one threshold, split by the branch the function was on. */
data class BranchCrossings(
    /** Where the function crossed the threshold while **rising** (twilight ends, a target rises). */
    val rising: List<Instant>,
    /** Where the function crossed while **falling** (twilight begins, a target sets). */
    val falling: List<Instant>,
) {
    val isEmpty: Boolean get() = rising.isEmpty() && falling.isEmpty()

    /** The number of crossings, which is always even for a bounded excursion and zero for none. */
    val size: Int get() = rising.size + falling.size
}

/**
 * The twilight-shaped façade over the app's one event-search implementation.
 *
 * The event-search rule (`design.md` "The event-search rule") is that **no search is ever a single call
 * over a long window**: the window is walked in one-hour segments, stepped at ten minutes, and every
 * sign change is refined by bisection, with an interior extremum probed so an excursion shorter than one
 * step still yields both of its crossings. That algorithm lives in one place — `domain.CrossingSearch` —
 * and this object exists so the sky engine has a twilight-shaped entry point (`crossings`,
 * `crossingsByBranch`, `intervalsAtOrBelow`, `intervalsAbove`, `extremum`, `sample`) that delegates to
 * it rather than re-deriving the bisection. Duplicating the solver here would be the "one source of
 * truth, imported — never restated" defect.
 *
 * It is a **pure** module: the caller supplies `f: (Instant) -> Double` (for the sky engine that is the
 * Sun's or a target's altitude), so it has no context, no clock and no device, and is JVM-testable.
 *
 * **The resolution.** Bisection stops when the bracket is one [DEFAULT_TOLERANCE_SECONDS] wide, so a
 * crossing is located to one second of UTC. That is the resolution the spec names, and the value is the
 * default of every function here so a caller cannot quietly ask for less.
 *
 * **A note on the half-open test.** A sample whose value is exactly the threshold counts as "at or
 * below" (`<=`), because that is the `intervalsAtOrBelow` convention of the underlying search: a
 * function that crosses exactly at a sample instant produces one crossing there, and the interval it
 * opens or closes is well formed.
 */
object TwilightSearch {

    /** The resolution every crossing here is refined to: one second of UTC. */
    const val DEFAULT_TOLERANCE_SECONDS: Double = 1.0

    /** The sampled-search step every event search in this app uses: ten minutes. */
    const val DEFAULT_STEP_SECONDS: Long = 600L

    /** The longest segment an event search walks in one probe, before it is re-armed. */
    const val SEGMENT_SECONDS: Long = CrossingSearch.SEGMENT_SECONDS

    /**
     * The coarse sampling grid of `f` across [span], one sample every [stepSeconds], starting at
     * `span.startUtc` and including the segment boundaries and the span's end.
     */
    fun sample(
        span: NightSpan,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        f: (Instant) -> Double,
    ): List<SunAltitudeSample> =
        CrossingSearch.sampleInstants(span, stepSeconds).map { SunAltitudeSample(it, f(it)) }

    /**
     * Every crossing of [targetAltitudeDeg] by `f` inside [span], in chronological order.
     *
     * A crossing between two samples is located by bisection; a short excursion that enters and leaves
     * the threshold entirely between two samples is found by probing the interior extremum. An empty
     * list is a **case** (the function never reached the threshold), never an error.
     */
    fun crossings(
        span: NightSpan,
        targetAltitudeDeg: Double,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        toleranceSeconds: Double = DEFAULT_TOLERANCE_SECONDS,
        f: (Instant) -> Double,
    ): List<Crossing> =
        CrossingSearch.crossings(span, targetAltitudeDeg, stepSeconds, toleranceSeconds, f)

    /**
     * The same crossings, split into the ascending and descending branch by direction.
     *
     * This is the branch the requirement names: the falling branch opens an at-or-below interval (dusk
     * crossing) and the rising branch closes it (dawn crossing).
     */
    fun crossingsByBranch(
        span: NightSpan,
        targetAltitudeDeg: Double,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        toleranceSeconds: Double = DEFAULT_TOLERANCE_SECONDS,
        f: (Instant) -> Double,
    ): BranchCrossings {
        val all = crossings(span, targetAltitudeDeg, stepSeconds, toleranceSeconds, f)
        return BranchCrossings(
            rising = all.filter { it.direction == CrossingDirection.RISING }.map { it.instantUtc },
            falling = all.filter { it.direction == CrossingDirection.FALLING }.map { it.instantUtc },
        )
    }

    /**
     * The sub-intervals of [span] in which `f(t) <= targetAltitudeDeg`, as half-open `[start, end)`
     * spans in chronological order, with empty spans dropped.
     *
     * This is the primitive the dark window is made of: one pair of crossings (falling then rising)
     * becomes one interval, and **two pairs in one night become two intervals**, which a single-interval
     * output could not express.
     */
    fun intervalsAtOrBelow(
        span: NightSpan,
        targetAltitudeDeg: Double,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        f: (Instant) -> Double,
    ): List<NightSpan> =
        CrossingSearch.intervalsAtOrBelow(span, targetAltitudeDeg, stepSeconds, f)

    /** The sub-intervals of [span] in which `f(t) > targetAltitudeDeg`. */
    fun intervalsAbove(
        span: NightSpan,
        targetAltitudeDeg: Double,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        f: (Instant) -> Double,
    ): List<NightSpan> =
        CrossingSearch.intervalsAbove(span, targetAltitudeDeg, stepSeconds, f)

    /**
     * The minimum (or maximum) of `f` over [span]: stepped at [stepSeconds], then the best bracket
     * refined by golden section to [refineSeconds] (default one second).
     */
    fun extremum(
        span: NightSpan,
        findMax: Boolean,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        refineSeconds: Double = DEFAULT_TOLERANCE_SECONDS,
        f: (Instant) -> Double,
    ): Extremum =
        CrossingSearch.extremum(span, findMax, stepSeconds, refineSeconds, f)
}

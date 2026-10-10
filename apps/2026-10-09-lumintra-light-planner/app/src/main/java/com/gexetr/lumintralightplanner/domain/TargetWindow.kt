package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant

/**
 * What produced one end of a target's visible window.
 *
 * A clipped boundary is named rather than silently presented as a rise or a set: a target already
 * above the horizon when the dark window opens did not rise during the window, and the UI says so.
 */
enum class WindowBoundary {
    RISE,
    SET,

    /** The window begins because the dark window begins while the target is already up. */
    DARK_WINDOW_START,

    /** The window ends because the dark window ends while the target is still up. */
    DARK_WINDOW_END,

    /** The window begins at the session window's own start (a polar night, or a circumpolar target). */
    SESSION_START,

    /** The window ends at the session window's own end. */
    SESSION_END;

    /** True when this end was clipped by the dark window rather than produced by the target. */
    val isClipped: Boolean
        get() = this == DARK_WINDOW_START || this == DARK_WINDOW_END ||
            this == SESSION_START || this == SESSION_END
}

/**
 * One interval in which a target is above its **effective horizon** — the greater of the user's drawn
 * horizon sample at the target's azimuth and the altitude mask (default 10.0°, user-adjustable), with
 * refraction applied. Never a bare 0°.
 *
 * [startUtc] / [endUtc] form a half-open interval `[start, end)`; a zero-length interval is dropped by
 * the caller rather than rendered.
 */
data class TargetInterval(
    val startUtc: Instant,
    val endUtc: Instant,
    val startBoundary: WindowBoundary,
    val endBoundary: WindowBoundary,
    /** The time of maximum altitude inside the interval, refined to one minute. */
    val transitUtc: Instant?,
    val peakAltitudeDeg: Double,
    val minimumAltitudeDeg: Double,
    /** The effective horizon the target crossed: `max(drawn horizon, mask)`. */
    val effectiveHorizonDeg: Double,
    /** True when the crossing coincided with the user's drawn horizon rather than the mask. */
    val clearsDrawnHorizon: Boolean = false,
) {
    init {
        require(!endUtc.isBefore(startUtc)) { "TargetInterval end $endUtc is before start $startUtc" }
    }

    val duration: Duration get() = Duration.between(startUtc, endUtc)

    val isEmpty: Boolean get() = startUtc == endUtc

    val span: NightSpan get() = NightSpan(startUtc, endUtc)

    /** True when either end is a dark-window boundary, which is the `target_partial_window` sentence. */
    val isPartial: Boolean get() = startBoundary.isClipped || endBoundary.isClipped

    /**
     * A grazing target: its peak equals the effective horizon, so there is no meaningful rise-to-set
     * interval and the brief reports it as `MARGINAL` rather than two events a second apart.
     */
    val isGrazing: Boolean
        get() = Math.abs(peakAltitudeDeg - effectiveHorizonDeg) <= GRAZE_TOLERANCE_DEG

    companion object {
        /** Within 0.05° of the effective horizon counts as grazing. */
        const val GRAZE_TOLERANCE_DEG: Double = 0.05
    }
}

/**
 * The typed result of classifying one target for one (spot, night).
 *
 * The three cases that are **not** a rise-to-set interval are each a distinct case with a sentence the
 * user reads. The natural implementation of this returns an empty window for `NeverRises` and
 * `Circumpolar`, and that empty window rendered as a blank panel is exactly the defect this type
 * exists to make unrepresentable.
 */
sealed interface TargetWindow {

    /** Whether the target is usable on this night at all — the "visible tonight" filter's predicate. */
    val isVisibleTonight: Boolean

    /**
     * One or more ordered intervals in which the target clears its effective horizon. More than one is
     * legal (E24): a target can rise and set twice in one session window and neither is dropped.
     */
    data class Visible(
        val intervals: List<TargetInterval>,
        val visibility: ObjectVisibility,
    ) : TargetWindow {
        override val isVisibleTonight: Boolean get() = intervals.any { !it.isEmpty }

        /** The transit of the longest interval, which is the one the brief states first. */
        val bestInterval: TargetInterval? get() = intervals.maxByOrNull { it.duration }

        val bestSpan: NightSpan? get() = bestInterval?.span

        /** True when the only intervals are grazing ones, so the brief reports MARGINAL, not a window. */
        val onlyGrazing: Boolean get() = intervals.isNotEmpty() && intervals.all { it.isGrazing }
    }

    /**
     * The target's minimum altitude is at or above its effective horizon: it is up all night and has no
     * rise or set. [minimumAltitudeDeg] and [atInstant] are the actionable facts.
     */
    data class Circumpolar(
        val minimumAltitudeDeg: Double,
        val atInstant: Instant,
        val effectiveHorizonDeg: Double,
    ) : TargetWindow {
        override val isVisibleTonight: Boolean get() = true
    }

    /**
     * The target's maximum altitude is below its effective horizon at this latitude: it never rises
     * from this spot. The UI states that sentence and excludes the target from "what is up tonight".
     */
    data class NeverRises(
        val effectiveHorizonDeg: Double,
        val maxAltitudeDeg: Double,
    ) : TargetWindow {
        override val isVisibleTonight: Boolean get() = false
    }

    companion object {
        /**
         * Classifies from the sampled altitude extremes, which is the whole decision:
         *
         * - `min ≥ effectiveHorizon` → [Circumpolar] (up all night, no rise or set)
         * - `max < effectiveHorizon` → [NeverRises] (the UI must say so, not render an empty window)
         * - otherwise the crossings found by the segmented search become [Visible] intervals
         */
        fun of(
            minimumAltitudeDeg: Double,
            minimumAtInstant: Instant,
            maximumAltitudeDeg: Double,
            effectiveHorizonDeg: Double,
            intervals: List<TargetInterval>,
        ): TargetWindow {
            if (minimumAltitudeDeg >= effectiveHorizonDeg) {
                return Circumpolar(minimumAltitudeDeg, minimumAtInstant, effectiveHorizonDeg)
            }
            if (maximumAltitudeDeg < effectiveHorizonDeg) {
                return NeverRises(effectiveHorizonDeg, maximumAltitudeDeg)
            }
            val kept = intervals.filterNot { it.isEmpty }
            return Visible(kept, if (kept.isEmpty()) ObjectVisibility.BELOW else ObjectVisibility.ABOVE)
        }
    }
}

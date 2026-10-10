package com.gexetr.lumintralightplanner.domain

import java.time.Instant

/**
 * The Moon's eight named phases, in waxing order from new.
 *
 * Each name owns a **45°-wide** sector of the elongation, and the sectors are **centred on the exact
 * phases** (new at 0°, first quarter at 90°, full at 180°, last quarter at 270°) — i.e. offset by
 * 22.5°, so the wrap at 0/360° cannot put a non-new phase into the new bucket, which is the defect
 * annex/L-depth-retention-edges.md row E19 records ("`new` at both ends of the table").
 *
 * The `new` sector therefore spans the wrap — `[337.5°, 22.5°)` — and is **contiguous**: `new` is
 * named once, never at both ends of the table. An elongation of 359° is 1° short of new and is
 * bucketed `NEW_MOON` for that reason; it is astronomically a *waning* crescent on its way to new.
 *
 * **Corrected 2026-10-09.** This KDoc previously claimed the mapping "returns [WANING_CRESCENT]
 * there rather than [NEW_MOON]" and argued the spec's scenario was astronomically wrong. The second
 * half was right and the first was false: `ofElongation(359.0)` has always returned `NEW_MOON`
 * (`bin = floor(((359 + 22.5) % 360) / 45) = 0`). The spec's "not bucketed as new" clause was the
 * error, and it was withdrawn by the owner on 2026-10-09 — see `specs/session-brief/spec.md`, which
 * now requires the contiguous sector this class implements. `MoonTest` asserts that property.
 */
enum class MoonPhaseName {
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL_MOON,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT;

    companion object {
        /** Elongation in degrees `[0, 360)` at which this phase is exact. */
        private val EXACT_DEG = doubleArrayOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)

        /**
         * The named phase for an elongation in degrees, wrapping correctly.
         *
         * `bin = floor(normalise(elongation + 22.5) / 45) mod 8`; the `+ 22.5` is the centring, and the
         * `mod 360` before it is what makes 359° resolve rather than overflow a table.
         */
        fun ofElongation(elongationDeg: Double): MoonPhaseName {
            val normalised = elongationDeg % 360.0
            val positive = if (normalised < 0.0) normalised + 360.0 else normalised
            val shifted = (positive + 22.5) % 360.0
            val bin = (shifted / 45.0).toInt().coerceIn(0, 7)
            return entries[bin]
        }

        /** The exact elongation of [name], the sector's centre. */
        fun exactDegrees(name: MoonPhaseName): Double = EXACT_DEG[name.ordinal]
    }
}

/**
 * The Moon at one instant, with **both** numbers the brief must show.
 *
 * A single "moonless" boolean would hide the case that matters: a 5 % crescent ten degrees above the
 * horizon is not moonless in practice. So the illuminated fraction, the phase angle, the named phase
 * and the altitude/azimuth are all carried, and the UI decides sentence by sentence.
 */
data class MoonState(
    /** `illumination().phaseFraction`, in `[0.0, 1.0]`. */
    val illuminatedFraction: Double,
    /** `illumination().phaseAngle` in degrees. Satisfies `k = (1 + cos(phaseAngle)) / 2`. */
    val phaseAngleDeg: Double,
    /** Moon's ecliptic longitude minus the Sun's, degrees `[0, 360)`; 0 = new, 180 = full. */
    val elongationDeg: Double,
    val phaseName: MoonPhaseName,
    /** Apparent (refraction-corrected) altitude in degrees; the value the scene draws. */
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    val magnitude: Double,
) {
    /** The geometric horizon test, exactly as the requirement states it. */
    val isUp: Boolean get() = altitudeDeg > 0.0

    /**
     * The product threshold, **stated as a product decision and not a measured fact**: at or below
     * 10 % illuminated the Moon is treated as not interfering.
     */
    val isDarkMoon: Boolean get() = illuminatedFraction <= DARK_MOON_FRACTION

    /** The complement of [isDarkMoon], at or above 0.90 and named full — the "washed out" case. */
    val isFullMoon: Boolean get() =
        phaseName == MoonPhaseName.FULL_MOON && illuminatedFraction >= FULL_MOON_FRACTION

    /** The one boolean the brief may show, derived rather than stored. */
    val isMoonless: Boolean get() = !isUp || isDarkMoon

    val position: HorizontalCoords get() = HorizontalCoords(altitudeDeg, azimuthDeg)

    companion object {
        /** A product decision: 10 %, not a measurement. Stated wherever the value is shown. */
        const val DARK_MOON_FRACTION: Double = 0.10

        /** The complement of [DARK_MOON_FRACTION]; introduces no second number. */
        const val FULL_MOON_FRACTION: Double = 0.90

        /** Builds the state from the engine's raw numbers, naming the phase in one place. */
        fun of(
            illuminatedFraction: Double,
            phaseAngleDeg: Double,
            elongationDeg: Double,
            altitudeDeg: Double,
            azimuthDeg: Double,
            magnitude: Double,
        ): MoonState = MoonState(
            illuminatedFraction = illuminatedFraction.coerceIn(0.0, 1.0),
            phaseAngleDeg = phaseAngleDeg,
            elongationDeg = elongationDeg,
            phaseName = MoonPhaseName.ofElongation(elongationDeg),
            altitudeDeg = altitudeDeg,
            azimuthDeg = azimuthDeg,
            magnitude = magnitude,
        )
    }
}

/** The typed outcomes of the moonless window. Each is a distinct case, never a zero-length interval. */
enum class MoonlessKind {
    /** The Moon is below the horizon for the entire dark window: the window equals the dark window. */
    MOONLESS_ALL_NIGHT,

    /** The Moon sets and/or rises inside the dark window: [MoonlessWindow.intervals] holds the gaps. */
    MOONLESS_INTERVALS,

    /** The Moon is up for the whole dark window and the moonless set is empty. */
    MOON_UP_ALL_WINDOW,

    /** The Moon does not rise tonight. No rise time is invented. */
    MOON_NEVER_RISES,

    /** The Moon does not set tonight. */
    MOON_NEVER_SETS,

    /** There is no dark window to be moonless within (a polar day, or no astronomical darkness). */
    NO_DARK_WINDOW,
}

/**
 * The moonless sub-intervals of the dark window, as an answer with a typed kind.
 *
 * [extremeAltitudeDeg] / [extremeInstant] carry the *actionable* instant: the Moon's **minimum**
 * altitude when it is up the whole window (the moment it is least in the way), or its **maximum** when
 * the night is washed out (the moment it is most in the way). The two are opposite cases and are never
 * reported for each other.
 */
data class MoonlessWindow(
    val kind: MoonlessKind,
    val intervals: List<NightSpan> = emptyList(),
    val extremeAltitudeDeg: Double? = null,
    val extremeInstant: Instant? = null,
    /** True when the Moon is full and above the horizon all night: no usable dark window for deep sky. */
    val washedOut: Boolean = false,
) {
    val isEmpty: Boolean get() = intervals.isEmpty()

    /** The one phrasing decision the brief needs, derived rather than re-tested at the call site. */
    val statesMoonlessAllNight: Boolean get() =
        kind == MoonlessKind.MOONLESS_ALL_NIGHT || (kind == MoonlessKind.MOON_NEVER_RISES)

    /**
     * Whether a clear moonless interval is offered. A polar day has no window, and a washed-out night
     * has none either — so the caller cannot accidentally render a zero-length interval as a window.
     */
    val hasUsableWindow: Boolean
        get() = kind != MoonlessKind.NO_DARK_WINDOW && intervals.isNotEmpty() && !washedOut
}

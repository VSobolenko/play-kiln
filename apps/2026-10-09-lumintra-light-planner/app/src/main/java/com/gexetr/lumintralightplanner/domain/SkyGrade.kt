package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant

/**
 * How dark the night is described, computed from quantities the app actually has.
 *
 * **There is deliberately no Bortle class and no mag/arcsec² field on this type.** No light-pollution
 * raster ships, so those numbers could only come from the user — and a computed field named
 * `bortle` or `sqm` would be the single easiest way to accidentally render one as if the app had
 * measured it. The user's own value lives in [SkyBrightnessEstimate] and is a different type, which is
 * what makes "user estimate" impossible to forget.
 */
data class SkyGrade(
    /** The Sun's lowest geometric altitude reached inside the session. */
    val darkestSunAltitudeDeg: Double,
    val darkestInstant: Instant,
    val darkWindowKind: DarkWindowKind,
    /** Total time the Sun spent at or below −18.0°, in UTC. Zero for a night without darkness. */
    val darkDuration: Duration,
    /** The Moon's illuminated fraction at local midnight, `0.0…1.0`. */
    val moonIlluminatedFraction: Double,
    /** The Moon's altitude at local midnight, degrees. */
    val moonAltitudeDeg: Double,
) {
    /**
     * The Moon's interference as a product of the two numbers the brief already shows: how lit it is,
     * and how high it is. A fraction on its own says nothing about a Moon that never rises.
     */
    val moonInterference: Double
        get() = moonIlluminatedFraction * (moonAltitudeDeg.coerceAtLeast(0.0) / 90.0)

    /**
     * A qualitative darkness description derived from the Sun's depth, the time spent dark and the
     * Moon's interference. It is a **description of computed inputs**, never a claim about the sky's
     * measured brightness.
     */
    val level: DarknessLevel
        get() = when {
            darkWindowKind == DarkWindowKind.POLAR_DAY -> DarknessLevel.NONE
            darkWindowKind == DarkWindowKind.NO_DARK -> DarknessLevel.NONE
            darkDuration.isZero -> DarknessLevel.POOR
            moonInterference >= 0.55 -> DarknessLevel.POOR
            darkestSunAltitudeDeg <= -30.0 && moonInterference <= 0.15 -> DarknessLevel.EXCELLENT
            darkestSunAltitudeDeg <= -18.0 && moonInterference <= 0.35 -> DarknessLevel.GOOD
            darkestSunAltitudeDeg <= -18.0 -> DarknessLevel.FAIR
            else -> DarknessLevel.FAIR
        }

    /** The dark window in whole minutes, for a readout that states a duration. */
    val darkMinutes: Long get() = darkDuration.toMinutes()
}

/** The qualitative bands [SkyGrade] maps its computed inputs onto. */
enum class DarknessLevel {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,

    /** No darkness at all tonight. */
    NONE,
}

/** Which shape the user's own estimate takes. `NONE` is the default and holds no value at all. */
enum class SkyBrightnessEstimateKind {
    NONE,
    SQM,
    BORTLE,
}

/**
 * The user's own sky-brightness estimate — **the only user-side darkness input the app accepts**.
 *
 * Every rendering of it carries the words "user estimate", and it never alters [SkyGrade] or any other
 * computed quantity. Because the app cannot verify it, it is a different type from [SkyGrade] rather
 * than an optional field on it.
 */
data class SkyBrightnessEstimate(
    val kind: SkyBrightnessEstimateKind,
    val sqm: Double? = null,
    val bortle: Int? = null,
    /** True when an out-of-range entry was pulled to the nearest accepted bound. */
    val clamped: Boolean = false,
) {
    val isSet: Boolean get() = kind != SkyBrightnessEstimateKind.NONE

    companion object {
        /** The accepted SQM range, in mag/arcsec². */
        const val SQM_MIN: Double = 16.0
        const val SQM_MAX: Double = 22.0

        /** The accepted Bortle classes. */
        const val BORTLE_MIN: Int = 1
        const val BORTLE_MAX: Int = 9

        /** The default: no estimate stored. */
        val NONE = SkyBrightnessEstimate(SkyBrightnessEstimateKind.NONE)

        /**
         * Accepts or clamps an SQM entry.
         *
         * The app clamps to the nearest bound and **states that it clamped** — the other permitted
         * behaviour — so an entry can never be stored or rendered as it was typed when it is outside
         * the accepted range.
         */
        fun sqm(value: Double): SkyBrightnessEstimate {
            if (value.isNaN()) return NONE
            val clampedValue = value.coerceIn(SQM_MIN, SQM_MAX)
            return SkyBrightnessEstimate(
                kind = SkyBrightnessEstimateKind.SQM,
                sqm = clampedValue,
                clamped = clampedValue != value,
            )
        }

        /** Accepts or clamps a Bortle class, with the same stated-clamp rule. */
        fun bortle(value: Int): SkyBrightnessEstimate {
            val clampedValue = value.coerceIn(BORTLE_MIN, BORTLE_MAX)
            return SkyBrightnessEstimate(
                kind = SkyBrightnessEstimateKind.BORTLE,
                bortle = clampedValue,
                clamped = clampedValue != value,
            )
        }

        /** True when a typed SQM value would be out of range, so the UI can say so before storing. */
        fun sqmOutOfRange(value: Double): Boolean = value.isNaN() || value < SQM_MIN || value > SQM_MAX

        /** True when a typed Bortle class would be out of range. */
        fun bortleOutOfRange(value: Int): Boolean = value < BORTLE_MIN || value > BORTLE_MAX
    }
}

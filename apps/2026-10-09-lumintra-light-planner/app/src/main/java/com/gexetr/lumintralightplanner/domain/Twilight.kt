package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant

/**
 * The three twilight boundaries, each the Sun's **geometric centre** altitude in degrees.
 *
 * These are *not* the sunrise/sunset test. Sunrise and sunset use a zenith distance of 90.8333°
 * (50′ below the horizon = 16′ semidiameter + 34′ refraction), a different quantity with a different
 * engine call; the two must never be interchanged (design.md "Time and geometry"; USNO
 * <https://aa.usno.navy.mil/faq/RST_defs>). Using −18° for sunset shifts the dark window by minutes.
 */
enum class TwilightKind(val angleDeg: Double) {
    /** −6.0°: the Sun's centre is 6° below the horizon. */
    CIVIL(-6.0),

    /** −12.0°: the horizon is barely visible. */
    NAUTICAL(-12.0),

    /** −18.0°: the sky is as dark as it gets. Used for the dark window. */
    ASTRONOMICAL(-18.0),
}

/** One twilight band as an interval of the session: a [TwilightKind] and the span it covers. */
data class TwilightWindow(
    val kind: TwilightKind,
    val span: NightSpan,
) {
    val duration: Duration get() = span.duration
}

/**
 * The five typed outcomes of one night's dark-window computation.
 *
 * A `null` from an engine crossing search is a *case*, never an error, and never an empty list shown
 * as "no darkness tonight". These five values are a **partition** of the possible nights, resolved by
 * the ordered precedence in [DarkWindowClassifier] — exactly one is reported, first match wins.
 */
enum class DarkWindowKind {
    /** At least one Sun ≤ −18.0° interval exists. */
    DARK,

    /** The Sun never sets tonight: no darkness at all. */
    POLAR_DAY,

    /** The Sun never rises today: the sky is dark, with at most a midday twilight dip. */
    POLAR_NIGHT,

    /** No Sun ≤ −18.0° crossing, but the Sun still falls below −6.0°. */
    NO_ASTRO_DARK,

    /** The Sun crosses the horizon but never reaches −6.0°. */
    NO_DARK,
}

/**
 * What the Sun did over one session window, as measured by the sky engine.
 *
 * This is the engine's *output*, deliberately a plain value: the classification below is therefore
 * pure and can be tested on the JVM with fixed numbers and no ephemeris at all.
 */
data class SunMeasurements(
    val session: NightSpan,
    /** `null` means the Sun does not set on this night (a polar-day case, per the engine's own call). */
    val sunsetUtc: Instant?,
    /** `null` means the Sun does not rise on this day. */
    val sunriseUtc: Instant?,
    val sunAltMinDeg: Double,
    val sunAltMinInstant: Instant,
    val sunAltMaxDeg: Double,
    val sunAltMaxInstant: Instant,
    /**
     * The intervals in which the Sun's geometric altitude is at or below each threshold. Every key of
     * [TwilightKind] is present; a missing level simply has no intervals.
     */
    val belowThreshold: Map<TwilightKind, List<NightSpan>>,
)

/**
 * The dark window of one session, as an answer rather than an interval.
 *
 * [darkWindows] is an **array** because two windows separated by a twilight dip are legal and both
 * render. All instants are UTC and all durations are computed in UTC, never from two formatted
 * strings.
 */
data class DarkWindowReport(
    val kind: DarkWindowKind,
    val darkWindows: List<NightSpan>,
    val sunAltMinDeg: Double,
    val sunAltMinInstant: Instant,
    val sunAltMaxDeg: Double,
    val sunAltMaxInstant: Instant,
    /** A polar night deep enough that the Sun never reaches −18.0°: the whole session is dark. */
    val wholeNightDark: Boolean = false,
    /** The sub-intervals of a polar night in which the Sky is *not* dark — the midday twilight dip. */
    val middayTwilightDip: List<NightSpan> = emptyList(),
    /** For a non-`DARK` night: the deepest twilight level actually reached. */
    val bestTwilight: TwilightKind? = null,
    /** The intervals at [bestTwilight]. */
    val twilightWindows: List<TwilightWindow> = emptyList(),
) {
    val bestWindow: NightSpan? get() = NightSpan.longest(darkWindows)

    val hasDarkWindow: Boolean get() = darkWindows.isNotEmpty()

    val isPolarDay: Boolean get() = kind == DarkWindowKind.POLAR_DAY

    val isPolarNight: Boolean get() = kind == DarkWindowKind.POLAR_NIGHT

    /** True when the night offered no astronomical darkness at all. */
    val lacksAstronomicalDarkness: Boolean get() = !hasDarkWindow

    /** Two windows in one night is a case the UI states in words, not a rendering accident. */
    val hasTwoWindows: Boolean get() = darkWindows.size >= 2

    /**
     * Whether the reminder may be offered. It is offered for a real dark window only: a polar day has
     * none and the non-dark outcomes are not a darkness the user can plan around.
     */
    val reminderEligible: Boolean get() = kind == DarkWindowKind.DARK || (kind == DarkWindowKind.POLAR_NIGHT && hasDarkWindow)
}

/**
 * The ordered decision list that turns [SunMeasurements] into exactly one [DarkWindowKind].
 *
 * The precedence is `POLAR_DAY`, `POLAR_NIGHT`, `DARK`, `NO_ASTRO_DARK`, `NO_DARK`, first match wins,
 * and a night is **never** reported as two kinds. The one implementation trap this closes: a polar
 * night in which the Sun also falls below −18.0° satisfies both the never-rising condition and the
 * dark-interval condition, and it is reported as `POLAR_NIGHT` alone — "the Sun does not rise" is the
 * more specific fact about where the user is.
 */
object DarkWindowClassifier {

    /** The Sun's apparent altitude at sunrise/sunset in degrees: 90.8333° zenith distance, negated. */
    const val SUNSET_GEOMETRIC_DEG: Double = -0.8333

    fun classify(sun: SunMeasurements): DarkWindowReport {
        val astronomical = sun.belowThreshold[TwilightKind.ASTRONOMICAL].orEmpty()
        val civil = sun.belowThreshold[TwilightKind.CIVIL].orEmpty()
        val nautical = sun.belowThreshold[TwilightKind.NAUTICAL].orEmpty()

        // 1. POLAR_DAY — the Sun does not set and never reaches the horizon.
        if (sun.sunsetUtc == null && sun.sunAltMinDeg > SUNSET_GEOMETRIC_DEG) {
            return DarkWindowReport(
                kind = DarkWindowKind.POLAR_DAY,
                darkWindows = emptyList(),
                sunAltMinDeg = sun.sunAltMinDeg,
                sunAltMinInstant = sun.sunAltMinInstant,
                sunAltMaxDeg = sun.sunAltMaxDeg,
                sunAltMaxInstant = sun.sunAltMaxInstant,
            )
        }

        // 2. POLAR_NIGHT — the Sun does not rise. Stands ahead of DARK and NO_ASTRO_DARK on purpose.
        if (sun.sunriseUtc == null && sun.sunAltMaxDeg < SUNSET_GEOMETRIC_DEG) {
            val wholeNightDark = sun.sunAltMaxDeg <= TwilightKind.ASTRONOMICAL.angleDeg
            val windows = if (wholeNightDark) listOf(sun.session) else astronomical
            val dip = if (wholeNightDark) emptyList()
            else NightSpan.subtract(listOf(sun.session), astronomical)
            return DarkWindowReport(
                kind = DarkWindowKind.POLAR_NIGHT,
                darkWindows = windows,
                sunAltMinDeg = sun.sunAltMinDeg,
                sunAltMinInstant = sun.sunAltMinInstant,
                sunAltMaxDeg = sun.sunAltMaxDeg,
                sunAltMaxInstant = sun.sunAltMaxInstant,
                wholeNightDark = wholeNightDark,
                middayTwilightDip = dip,
            )
        }

        // 3. DARK — at least one Sun <= -18.0 interval.
        if (astronomical.isNotEmpty()) {
            return DarkWindowReport(
                kind = DarkWindowKind.DARK,
                darkWindows = astronomical,
                sunAltMinDeg = sun.sunAltMinDeg,
                sunAltMinInstant = sun.sunAltMinInstant,
                sunAltMaxDeg = sun.sunAltMaxDeg,
                sunAltMaxInstant = sun.sunAltMaxInstant,
            )
        }

        // 4. NO_ASTRO_DARK — no -18 crossing, but the Sun still falls below -6.0.
        if (sun.sunAltMinDeg < TwilightKind.CIVIL.angleDeg) {
            val level = if (sun.sunAltMinDeg < TwilightKind.NAUTICAL.angleDeg) {
                TwilightKind.NAUTICAL
            } else {
                TwilightKind.CIVIL
            }
            val intervals = if (level == TwilightKind.NAUTICAL) nautical else civil
            return DarkWindowReport(
                kind = DarkWindowKind.NO_ASTRO_DARK,
                darkWindows = emptyList(),
                sunAltMinDeg = sun.sunAltMinDeg,
                sunAltMinInstant = sun.sunAltMinInstant,
                sunAltMaxDeg = sun.sunAltMaxDeg,
                sunAltMaxInstant = sun.sunAltMaxInstant,
                bestTwilight = level,
                twilightWindows = intervals.map { TwilightWindow(level, it) },
            )
        }

        // 5. NO_DARK — the Sun crosses the horizon but stays above civil twilight.
        return DarkWindowReport(
            kind = DarkWindowKind.NO_DARK,
            darkWindows = emptyList(),
            sunAltMinDeg = sun.sunAltMinDeg,
            sunAltMinInstant = sun.sunAltMinInstant,
            sunAltMaxDeg = sun.sunAltMaxDeg,
            sunAltMaxInstant = sun.sunAltMaxInstant,
            bestTwilight = TwilightKind.CIVIL,
            twilightWindows = civil.map { TwilightWindow(TwilightKind.CIVIL, it) },
        )
    }
}

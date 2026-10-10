package com.gexetr.lumintralightplanner.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The dark-window classifier's **ordered decision list**.
 *
 * The five outcomes are a partition of the possible nights, resolved by
 * `POLAR_DAY`, `POLAR_NIGHT`, `DARK`, `NO_ASTRO_DARK`, `NO_DARK`, first match wins
 * (`domain/Twilight.kt`, `DarkWindowClassifier`; `specs/session-brief/spec.md` "The five outcomes
 * are a partition of the possible nights, resolved by a fixed precedence").
 *
 * Every test drives the *pure* classifier with fixed numbers, so no ephemeris is needed.
 */
class DarkWindowClassifierTest {

    private val session = NightSpan(
        Instant.parse("2026-10-09T18:00:00Z"),
        Instant.parse("2026-10-10T06:00:00Z"),
    )

    private fun span(fromIso: String, toIso: String): NightSpan =
        NightSpan(Instant.parse(fromIso), Instant.parse(toIso))

    private fun sun(
        sunsetUtc: Instant? = session.startUtc,
        sunriseUtc: Instant? = session.endUtc,
        sunAltMinDeg: Double,
        sunAltMaxDeg: Double,
        belowThreshold: Map<TwilightKind, List<NightSpan>> = emptyMap(),
    ): SunMeasurements = SunMeasurements(
        session = session,
        sunsetUtc = sunsetUtc,
        sunriseUtc = sunriseUtc,
        sunAltMinDeg = sunAltMinDeg,
        sunAltMinInstant = Instant.parse("2026-10-10T01:00:00Z"),
        sunAltMaxDeg = sunAltMaxDeg,
        sunAltMaxInstant = Instant.parse("2026-10-09T12:00:00Z"),
        belowThreshold = belowThreshold,
    )

    // ------------------------------------------------------------------ POLAR_DAY

    @Test
    fun polarDayWhenTheSunNeverSetsAndStaysAboveTheHorizon() {
        val report = DarkWindowClassifier.classify(
            sun(
                sunsetUtc = null,
                sunriseUtc = session.endUtc,
                sunAltMinDeg = 1.5,
                sunAltMaxDeg = 20.0,
            ),
        )
        assertEquals(DarkWindowKind.POLAR_DAY, report.kind)
        assertTrue(report.isPolarDay)
        assertFalse(report.isPolarNight)
        assertTrue(report.darkWindows.isEmpty())
        assertNull(report.bestWindow)
        assertTrue(report.lacksAstronomicalDarkness)
        // A polar day has no dark window, so the reminder must never be offered (E02/E04).
        assertFalse(report.reminderEligible)
    }

    /**
     * A `null` sunset is a **case**, never an error (E01): the classifier maps it to `POLAR_DAY`
     * rather than throwing or inventing a time.
     */
    @Test
    fun nullSunsetIsPolarDayAndNotAnException() {
        val report = DarkWindowClassifier.classify(
            sun(sunsetUtc = null, sunriseUtc = null, sunAltMinDeg = 2.0, sunAltMaxDeg = 25.0),
        )
        assertEquals(DarkWindowKind.POLAR_DAY, report.kind)
        assertTrue(report.darkWindows.isEmpty())
    }

    /**
     * The maximal fixture in which `POLAR_DAY` holds *and* `DARK` would also hold (a supplied
     * astronomical interval whose condition is `isNotEmpty()`). `POLAR_DAY` is earlier in the list,
     * so it alone is reported.
     */
    @Test
    fun polarDayOutranksDarkWhenBothConditionsHold() {
        val report = DarkWindowClassifier.classify(
            sun(
                sunsetUtc = null,
                sunriseUtc = session.endUtc,
                sunAltMinDeg = 5.0,
                sunAltMaxDeg = 20.0,
                belowThreshold = mapOf(
                    TwilightKind.ASTRONOMICAL to listOf(span("2026-10-09T22:00:00Z", "2026-10-10T02:00:00Z")),
                ),
            ),
        )
        assertEquals(DarkWindowKind.POLAR_DAY, report.kind)
        assertTrue(report.darkWindows.isEmpty())
    }

    // ---------------------------------------------------------------- POLAR_NIGHT

    @Test
    fun polarNightWhenTheSunNeverRisesButDoesReachAstronomicalDark() {
        val astronomical = listOf(span("2026-10-09T22:00:00Z", "2026-10-10T02:00:00Z"))
        val report = DarkWindowClassifier.classify(
            sun(
                sunsetUtc = session.startUtc,
                sunriseUtc = null,
                sunAltMinDeg = -30.0,
                sunAltMaxDeg = -2.0,
                belowThreshold = mapOf(TwilightKind.ASTRONOMICAL to astronomical),
            ),
        )
        assertEquals(DarkWindowKind.POLAR_NIGHT, report.kind)
        assertTrue(report.isPolarNight)
        assertFalse(report.wholeNightDark)
        assertEquals(astronomical, report.darkWindows)
        assertTrue(report.reminderEligible)
        // The dip is the rest of the session once the astronomical interval is cut out.
        assertEquals(2, report.middayTwilightDip.size)
    }

    /**
     * **Overlap 1 of 2** — `POLAR_NIGHT` against `DARK`: the Sun never rises *and* a Sun <= -18.0
     * interval exists. The explicit rule (`specs/session-brief/spec.md` "A deep polar night is one
     * outcome, not two") resolves it as `POLAR_NIGHT` alone.
     */
    @Test
    fun polarNightOverlapsDarkAndIsReportedAsTheSingleKind() {
        val astronomical = listOf(span("2026-10-09T23:00:00Z", "2026-10-10T01:00:00Z"))
        val report = DarkWindowClassifier.classify(
            sun(
                sunriseUtc = null,
                sunAltMinDeg = -25.0,
                sunAltMaxDeg = -5.0,
                belowThreshold = mapOf(TwilightKind.ASTRONOMICAL to astronomical),
            ),
        )
        assertEquals(DarkWindowKind.POLAR_NIGHT, report.kind)
        assertFalse(report.wholeNightDark)
        // The never-rising condition is the specific fact; DARK is not also reported.
        assertEquals(astronomical, report.darkWindows)
        assertNull(report.bestTwilight)
    }

    /**
     * **Overlap 2 of 2** — `POLAR_NIGHT` against `NO_ASTRO_DARK`: the Sun never rises and never
     * reaches -18.0, so the `NO_ASTRO_DARK` condition (`sunAltMinDeg < -6.0`) would also hold.
     */
    @Test
    fun polarNightOverlapsNoAstroDarkAndIsReportedAsTheSingleKind() {
        val report = DarkWindowClassifier.classify(
            sun(
                sunriseUtc = null,
                sunAltMinDeg = -15.0, // would satisfy NO_ASTRO_DARK
                sunAltMaxDeg = -3.0,
                belowThreshold = emptyMap(),
            ),
        )
        assertEquals(DarkWindowKind.POLAR_NIGHT, report.kind)
        assertFalse(report.wholeNightDark)
        assertTrue(report.darkWindows.isEmpty())
        // No astronomical interval at all: the whole session minus nothing is the dip.
        assertEquals(listOf(session), report.middayTwilightDip)
        assertFalse(report.reminderEligible)
    }

    /**
     * A polar night deep enough that the Sun's **maximum** stays at or below -18.0 is one whole-day
     * dark interval: `wholeNightDark = true` and the session itself is the dark window.
     *
     * NOTE (recorded, not silently asserted): `specs/night-scene/spec.md` says this same case
     * "SHALL" be reported as `DARK`, while `specs/session-brief/spec.md`'s scenario "A deep polar
     * night is one outcome, not two" and this classifier both report `POLAR_NIGHT`. The
     * contradiction is reported in the handoff; this test pins the classifier's documented choice.
     */
    @Test
    fun polarNightWholeDayDarkIsOneIntervalAndStillPolarNight() {
        val report = DarkWindowClassifier.classify(
            sun(
                sunriseUtc = null,
                sunAltMinDeg = -30.0,
                sunAltMaxDeg = -20.0,
                belowThreshold = emptyMap(),
            ),
        )
        assertEquals(DarkWindowKind.POLAR_NIGHT, report.kind)
        assertTrue(report.wholeNightDark)
        assertEquals(listOf(session), report.darkWindows)
        assertTrue(report.middayTwilightDip.isEmpty())
    }

    // ----------------------------------------------------------------------- DARK

    @Test
    fun darkWhenAtLeastOneAstronomicalIntervalExists() {
        val astronomical = listOf(span("2026-10-09T21:00:00Z", "2026-10-10T02:00:00Z"))
        val report = DarkWindowClassifier.classify(
            sun(
                sunAltMinDeg = -25.0,
                sunAltMaxDeg = 20.0,
                belowThreshold = mapOf(TwilightKind.ASTRONOMICAL to astronomical),
            ),
        )
        assertEquals(DarkWindowKind.DARK, report.kind)
        assertEquals(astronomical, report.darkWindows)
        assertTrue(report.hasDarkWindow)
        assertFalse(report.lacksAstronomicalDarkness)
        assertTrue(report.reminderEligible)
    }

    /** Two windows separated by a twilight dip are legal and both render (E10). */
    @Test
    fun twoDarkWindowsInOneNightAreBothReported() {
        val first = span("2026-10-09T21:00:00Z", "2026-10-09T23:00:00Z") // 2 h
        val second = span("2026-10-10T00:30:00Z", "2026-10-10T02:00:00Z") // 1.5 h
        val report = DarkWindowClassifier.classify(
            sun(
                sunAltMinDeg = -25.0,
                sunAltMaxDeg = 20.0,
                belowThreshold = mapOf(TwilightKind.ASTRONOMICAL to listOf(first, second)),
            ),
        )
        assertEquals(DarkWindowKind.DARK, report.kind)
        assertEquals(2, report.darkWindows.size)
        assertTrue(report.hasTwoWindows)
        assertEquals(first, report.bestWindow)
    }

    // ------------------------------------------------------------- NO_ASTRO_DARK

    @Test
    fun noAstroDarkReportsNauticalWhenTheSunFallsBelowMinus12() {
        val nautical = listOf(span("2026-10-09T23:00:00Z", "2026-10-10T01:00:00Z"))
        val report = DarkWindowClassifier.classify(
            sun(
                sunAltMinDeg = -14.0,
                sunAltMaxDeg = 10.0,
                belowThreshold = mapOf(TwilightKind.NAUTICAL to nautical),
            ),
        )
        assertEquals(DarkWindowKind.NO_ASTRO_DARK, report.kind)
        assertEquals(TwilightKind.NAUTICAL, report.bestTwilight)
        assertEquals(listOf(TwilightWindow(TwilightKind.NAUTICAL, nautical[0])), report.twilightWindows)
        assertTrue(report.darkWindows.isEmpty())
    }

    @Test
    fun noAstroDarkReportsCivilWhenTheSunOnlyReachesMinus12ToMinus6() {
        val civil = listOf(span("2026-10-09T22:00:00Z", "2026-10-10T02:00:00Z"))
        val report = DarkWindowClassifier.classify(
            sun(
                sunAltMinDeg = -8.0,
                sunAltMaxDeg = 12.0,
                belowThreshold = mapOf(TwilightKind.CIVIL to civil),
            ),
        )
        assertEquals(DarkWindowKind.NO_ASTRO_DARK, report.kind)
        assertEquals(TwilightKind.CIVIL, report.bestTwilight)
        assertEquals(listOf(TwilightWindow(TwilightKind.CIVIL, civil[0])), report.twilightWindows)
    }

    /** The boundary is `sunAltMinDeg < -12.0`, so exactly -12.0 is still `CIVIL`. */
    @Test
    fun noAstroDarkAtExactlyMinus12IsCivil() {
        val report = DarkWindowClassifier.classify(sun(sunAltMinDeg = -12.0, sunAltMaxDeg = 12.0))
        assertEquals(DarkWindowKind.NO_ASTRO_DARK, report.kind)
        assertEquals(TwilightKind.CIVIL, report.bestTwilight)
    }

    // -------------------------------------------------------------------- NO_DARK

    /**
     * A **reachable** `NO_DARK` fixture: the Sun crosses the horizon but never falls to −6.0°, so the
     * engine supplies no interval at any threshold at all. A non-empty `belowThreshold[CIVIL]` would
     * put `sunAltMinDeg` below −6.0° and the night would classify as `NO_ASTRO_DARK` instead, so the
     * empty map is the only shape this branch can actually be reached with.
     */
    @Test
    fun noDarkWhenTheSunCrossesTheHorizonButStaysAboveCivilTwilight() {
        val report = DarkWindowClassifier.classify(sun(sunAltMinDeg = -3.0, sunAltMaxDeg = 15.0))
        assertEquals(DarkWindowKind.NO_DARK, report.kind)
        // `specs/session-brief/spec.md` (the outcome's own sentence): "on `NO_DARK` the brief reports
        // the civil twilight reached and … the Sun's altitude and instant", so the level is CIVIL
        // even though the Sun never crossed it — and no twilight window is claimed, because none exists.
        assertEquals(TwilightKind.CIVIL, report.bestTwilight)
        assertTrue(report.twilightWindows.isEmpty())
        assertTrue(report.darkWindows.isEmpty())
        assertFalse(report.hasDarkWindow)
        assertFalse(report.reminderEligible)
    }

    /**
     * The same branch's mapping, driven directly: whatever civil intervals the engine supplies become
     * the report's [TwilightWindow] list at [TwilightKind.CIVIL].
     */
    @Test
    fun noDarkMapsTheSuppliedCivilIntervalsToCivilTwilightWindows() {
        val civil = listOf(span("2026-10-09T21:00:00Z", "2026-10-10T03:00:00Z"))
        val report = DarkWindowClassifier.classify(
            sun(
                sunAltMinDeg = -3.0,
                sunAltMaxDeg = 15.0,
                belowThreshold = mapOf(TwilightKind.CIVIL to civil),
            ),
        )
        assertEquals(DarkWindowKind.NO_DARK, report.kind)
        assertEquals(TwilightKind.CIVIL, report.bestTwilight)
        assertEquals(listOf(TwilightWindow(TwilightKind.CIVIL, civil[0])), report.twilightWindows)
    }

    // ------------------------------------------------------------------ precedence

    /**
     * `POLAR_NIGHT`, `DARK` and `NO_ASTRO_DARK` all hold at once here (`sunriseUtc == null`,
     * `sunAltMaxDeg < -0.8333`, a non-empty astronomical list and `sunAltMinDeg < -6`). The earliest
     * match — `POLAR_NIGHT` — is the single reported kind.
     *
     * A fixture where **all five** conditions hold cannot be constructed: `POLAR_DAY` and the
     * darkness outcomes are keyed on the same `sunAltMinDeg`, and `POLAR_DAY` requires
     * `sunAltMinDeg > -0.8333` while `NO_ASTRO_DARK`/`NO_DARK` require it below (or at) -6.0, so the
     * two are mutually exclusive by construction. This fixture is the maximal consistent one.
     */
    @Test
    fun firstMatchWinsWhenPolarNightDarkAndNoAstroDarkAllHold() {
        val astronomical = listOf(span("2026-10-09T23:30:00Z", "2026-10-10T00:30:00Z"))
        val report = DarkWindowClassifier.classify(
            sun(
                sunsetUtc = session.startUtc,
                sunriseUtc = null,
                sunAltMinDeg = -25.0,
                sunAltMaxDeg = -5.0,
                belowThreshold = mapOf(TwilightKind.ASTRONOMICAL to astronomical),
            ),
        )
        assertEquals(DarkWindowKind.POLAR_NIGHT, report.kind)
        assertNull(report.bestTwilight) // a DARK/NO_ASTRO_DARK report would carry one
        assertEquals(astronomical, report.darkWindows) // DARK's own interval, but DARK is not the kind
    }

    @Test
    fun sunsetGeometricConstantIsTheApparentHorizonDip() {
        assertEquals(-0.8333, DarkWindowClassifier.SUNSET_GEOMETRIC_DEG, 1e-12)
    }
}

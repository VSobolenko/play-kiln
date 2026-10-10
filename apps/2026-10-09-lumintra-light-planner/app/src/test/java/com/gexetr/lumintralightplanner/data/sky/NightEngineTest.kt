package com.gexetr.lumintralightplanner.data.sky

import com.gexetr.lumintralightplanner.domain.BriefFacts
import com.gexetr.lumintralightplanner.domain.ConditionKind
import com.gexetr.lumintralightplanner.domain.ConditionState
import com.gexetr.lumintralightplanner.domain.ConditionUnavailableReason
import com.gexetr.lumintralightplanner.domain.CrossingDirection
import com.gexetr.lumintralightplanner.domain.DarkWindowClassifier
import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.DarknessLevel
import com.gexetr.lumintralightplanner.domain.FitVerdict
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.KeyTimeBand
import com.gexetr.lumintralightplanner.domain.MoonPhaseName
import com.gexetr.lumintralightplanner.domain.MoonlessKind
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.ObjectType
import com.gexetr.lumintralightplanner.domain.ObjectVisibility
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.domain.SessionClock
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.TargetWindow
import com.gexetr.lumintralightplanner.domain.TwilightKind
import com.gexetr.lumintralightplanner.domain.TwilightWindow
import com.gexetr.lumintralightplanner.domain.WindowBoundary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * JVM tests for the night engine, driven by the **real** vendored ephemeris and no device.
 *
 * What these tests can and cannot claim is the discipline of this file. `NightEngine.compute` is run
 * against real astronomy here — not against a stub — because the wiring between the adapter, the
 * crossing search and `DarkWindowClassifier` is exactly what can be wrong. But a number the test cannot
 * derive by hand is asserted only as a **structural invariant** (ranges, ordering, nesting, identity
 * between the computation and the brief) and is labelled `UNVERIFIED` in the handoff ledger.
 *
 * The numbers that *are* derived here, with their working:
 *
 * - **At latitude φ, an object of declination δ culminates at `90° − φ + δ` and reaches its lowest
 *   point at `δ + φ − 90°`.** So at 45° N a declination −60° row never exceeds −15°, a declination +75°
 *   row never drops below +30°, and M31 (δ = +41.27°) reaches +86°: the three target verdicts below are
 *   arithmetic, not recorded output.
 * - **At 78° N in June the Sun's lowest altitude is `23.44° + 78° − 90° = +11.4°`** — it never sets and
 *   never approaches the −0.8333° sunset test: a polar day. At 78° N in December the Sun's *highest* is
 *   `90° − 78° − 23.44° = −11.4°` and its *lowest* is `−23.44° + 78° − 90° = −35.4°`: a polar night
 *   that **does** dip below −18° (so it carries astronomical dark windows and a midday twilight dip)
 *   but is still `POLAR_NIGHT` alone — precedence ahead of `DARK`. At 88° N the highest is
 *   `−21.4° ≤ −18°`, so the whole session is the dark window.
 * - **Sunrise and sunset are the 90.8333° zenith-distance test**: `searchRiseSet` searches for
 *   `centre + semidiameter = horizonDip − 34′` (`astronomy.kt:6829-6844` with the body radius added at
 *   `:6682`, and `REFRACTION_NEAR_HORIZON = 34.0/60.0` at `:196`), which at sea level is the Sun's
 *   centre at **−0.8333°**, measured with no refraction. At −18°, −12° and −6° the same test was not
 *   used: the crossing sits on the *geometric* centre and the refracted altitude there is ~0.5° away.
 */
class NightEngineTest {

    private val ephemeris: SkyEphemeris = AstronomySkyEphemeris()

    // ---------------------------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------------------------

    private val midLatZone: ZoneId = ZoneId.of("UTC")
    private val midLatNight: LocalDate = LocalDate.of(2026, 1, 15)
    private val midsummerNight: LocalDate = LocalDate.of(2026, 6, 21)
    private val midwinterNight: LocalDate = LocalDate.of(2026, 12, 21)

    private val midLatSpot = Scout(
        id = "mid-latitude",
        name = "45 N 0 E (test spot)",
        latDeg = 45.0,
        lonDeg = 0.0,
        elevationM = 0.0,
        tzId = "UTC",
    )

    private val highLatSpot = Scout(
        id = "high-latitude",
        name = "78 N 15 E (test spot)",
        latDeg = 78.0,
        lonDeg = 15.0,
        elevationM = 0.0,
        tzId = "Europe/Oslo",
    )

    private val deepLatSpot = Scout(
        id = "deep-latitude",
        name = "88 N 15 E (test spot)",
        latDeg = 88.0,
        lonDeg = 15.0,
        elevationM = 0.0,
        tzId = "Europe/Oslo",
    )

    /** M31: δ = +41.27°, so at 45° N it clears the 10° mask and reaches ≈ +86°. */
    private val andromeda = SkyObject(
        id = "m31",
        messier = "31",
        name = "Andromeda Galaxy",
        type = ObjectType.GALAXY,
        raDeg = 10.6847,
        decDeg = 41.2692,
        magV = 3.44,
        sizeMajArcmin = 178.0,
    )

    /** δ = +75° at 45° N: its lowest altitude is ≈ +30°, so it is circumpolar, never rising or setting. */
    private val circumpolarRow = SkyObject(
        id = "circumpolar",
        name = "Circumpolar test row",
        type = ObjectType.OPEN_CLUSTER,
        raDeg = 100.0,
        decDeg = 75.0,
        magV = 7.0,
        sizeMajArcmin = 20.0,
    )

    /** δ = −60° at 45° N: its highest altitude is ≈ −15°, so it never clears any horizon there. */
    private val neverRisesRow = SkyObject(
        id = "never-rises",
        name = "Never-rising test row",
        type = ObjectType.GLOBULAR_CLUSTER,
        raDeg = 200.0,
        decDeg = -60.0,
        magV = 6.0,
        sizeMajArcmin = 12.0,
    )

    /** No stated apparent size, so no fit statement may be invented for it. */
    private val pointRow = SkyObject(
        id = "point",
        name = "Unknown-size test row",
        type = ObjectType.DOUBLE_STAR,
        raDeg = 40.0,
        decDeg = 20.0,
        magV = 3.0,
    )

    private val rows = listOf(andromeda, circumpolarRow, neverRisesRow, pointRow)

    private fun request(
        spot: Scout,
        night: LocalDate,
        horizon: HorizonProfile? = null,
        favourites: Set<String> = emptySet(),
    ): NightRequest = NightRequest(
        spot = spot,
        horizon = horizon,
        equipment = spot.equipment,
        span = SessionClock.sessionWindow(night, ZoneId.of(spot.tzId), spot.id),
        zone = ZoneId.of(spot.tzId),
        nowUtc = Instant.parse("2026-01-15T20:00:00Z"),
        favouriteIds = favourites,
        objects = rows,
    )

    private val midLatRequest: NightRequest = request(
        spot = midLatSpot,
        night = midLatNight,
        favourites = setOf("circumpolar", "never-rises", "point"),
    )

    private val midLat: NightComputation by lazy { NightEngine.compute(midLatRequest, ephemeris) }

    private val ridgedRequest: NightRequest = request(
        spot = midLatSpot,
        night = midLatNight,
        horizon = HorizonProfile.flat("ridge", "25 deg test ridge", 45.0, 0.0, 25.0),
        favourites = setOf("circumpolar", "never-rises", "point"),
    )

    private val ridged: NightComputation by lazy { NightEngine.compute(ridgedRequest, ephemeris) }

    private val highLatSummer: NightComputation by lazy {
        NightEngine.compute(request(highLatSpot, midsummerNight), ephemeris)
    }

    private val highLatWinter: NightComputation by lazy {
        NightEngine.compute(request(highLatSpot, midwinterNight), ephemeris)
    }

    private val deepLatWinter: NightComputation by lazy {
        NightEngine.compute(request(deepLatSpot, midwinterNight), ephemeris)
    }

    // ---------------------------------------------------------------------------------------------
    // The dark window and the twilight bands
    // ---------------------------------------------------------------------------------------------

    @Test
    fun aMidLatitudeWinterNightIsClassifiedDarkWithAWindowInsideTheSpan() {
        val computation = midLat
        val report = computation.darkWindow

        assertEquals(DarkWindowKind.DARK, report.kind)
        assertTrue("a 45 N January night must carry a dark window", report.hasDarkWindow)
        assertTrue(
            "the report must carry at least one window, was ${report.darkWindows}",
            report.darkWindows.isNotEmpty(),
        )
        assertEquals("the computation exposes the report's own windows", report.darkWindows, computation.darkWindows)
        assertEquals(
            "the longest window is the one a session is planned around",
            report.darkWindows.maxByOrNull { it.duration },
            report.bestWindow,
        )

        for (window in report.darkWindows) {
            assertFalse("a zero-length window is never rendered", window.isEmpty)
            assertTrue(
                "a dark window lies inside the session: $window vs ${midLatRequest.span}",
                !window.startUtc.isBefore(midLatRequest.span.startUtc) &&
                    !window.endUtc.isAfter(midLatRequest.span.endUtc),
            )
        }
        for (i in 1 until report.darkWindows.size) {
            assertTrue(
                "dark windows are disjoint and chronological",
                report.darkWindows[i - 1].endUtc <= report.darkWindows[i].startUtc,
            )
        }

        // Derived: on 15 January the Sun's declination is about -21 deg, so at 45 N its midnight
        // altitude is -(90 - 45) - 21 = -66 deg, far below the -18 deg threshold.
        assertTrue(
            "the Sun must reach astronomical darkness at 45 N in January, minimum was ${computation.sun.sunAltMinDeg}",
            computation.sun.sunAltMinDeg <= TwilightKind.ASTRONOMICAL.angleDeg,
        )
        assertEquals(
            "the report's Sun extrema are the computation's own",
            computation.sun.sunAltMinDeg,
            report.sunAltMinDeg,
            0.0,
        )
        assertEquals(computation.sun.sunAltMinInstant, report.sunAltMinInstant)
        assertEquals(computation.sun.sunAltMaxDeg, report.sunAltMaxDeg, 0.0)
        assertEquals(computation.sun.sunAltMaxInstant, report.sunAltMaxInstant)

        val darkTotal = NightSpan.total(report.darkWindows)
        assertTrue(
            "a January night at 45 N spends hours below -18 deg, was ${darkTotal.toMinutes()} min",
            darkTotal >= Duration.ofHours(3),
        )
        assertEquals(darkTotal, computation.skyGrade.darkDuration)
        assertEquals(DarkWindowKind.DARK, computation.skyGrade.darkWindowKind)
        assertEquals(computation.sun.sunAltMinInstant, computation.skyGrade.darkestInstant)
        assertTrue(computation.skyGrade.level != DarknessLevel.NONE)
        assertFalse(computation.darkWindow.hasTwoWindows)
    }

    @Test
    fun everyTwilightBandLiesInsideTheSessionAndTheBandsNest() {
        val windows = midLat.twilightWindows
        assertTrue("a mid-latitude winter night has twilight bands", windows.isNotEmpty())
        assertEquals("the bands are chronological", windows.sortedBy { it.span.startUtc }, windows)

        for (window in windows) {
            assertFalse("a zero-length band is dropped rather than drawn", window.span.isEmpty)
            assertTrue(
                "every band lies inside the session, was ${window.span}",
                !window.span.startUtc.isBefore(midLatRequest.span.startUtc) &&
                    !window.span.endUtc.isAfter(midLatRequest.span.endUtc),
            )
        }

        // Derived: `sun <= -18` implies `sun <= -12` implies `sun <= -6`, so the bands nest.
        assertNested(windows, TwilightKind.ASTRONOMICAL, TwilightKind.NAUTICAL)
        assertNested(windows, TwilightKind.NAUTICAL, TwilightKind.CIVIL)

        assertEquals(
            "the dark window IS the astronomical band, not a re-derivation of it",
            windows.filter { it.kind == TwilightKind.ASTRONOMICAL }.map { it.span },
            midLat.darkWindow.darkWindows,
        )
    }

    @Test
    fun theTwilightBoundariesAreGeometricAndSunriseIsNot() {
        val point = midLatRequest.point
        val session = midLatRequest.span

        for (kind in TwilightKind.entries) {
            val crossings = ephemeris.sunAltitudeCrossings(point, session, kind.angleDeg)
            assertEquals(
                "${kind.name}: an evening and a morning crossing at 45 N in January, was $crossings",
                2,
                crossings.size,
            )
            assertEquals(CrossingDirection.FALLING, crossings[0].direction)
            assertEquals(CrossingDirection.RISING, crossings[1].direction)

            for (crossing in crossings) {
                val geometric = ephemeris
                    .sunHorizontal(point, crossing.instantUtc, AltitudeModel.GEOMETRIC)
                    .altitudeDeg
                val refracted = ephemeris
                    .sunHorizontal(point, crossing.instantUtc, AltitudeModel.REFRACTED)
                    .altitudeDeg

                assertTrue(
                    "${kind.name}: the GEOMETRIC centre is at ${kind.angleDeg} deg at the crossing, was $geometric",
                    Math.abs(geometric - kind.angleDeg) <= 0.02,
                )
                assertTrue(
                    "${kind.name}: refraction must NOT be the twilight test; the refracted altitude at the " +
                        "geometric crossing was $refracted, only ${Math.abs(refracted - kind.angleDeg)} deg away",
                    Math.abs(refracted - kind.angleDeg) >= 0.1,
                )
            }
        }

        // The sunrise/sunset pair is the other test, and it sits outside the civil band.
        assertNotNull("45 N in January has a sunset", midLat.sunriseSunset.sunsetUtc)
        assertNotNull("45 N in January has a sunrise", midLat.sunriseSunset.sunriseUtc)
        assertTrue(midLat.sunriseSunset.hasBoth)
        val sunset: Instant = midLat.sunriseSunset.sunsetUtc!!
        val sunrise: Instant = midLat.sunriseSunset.sunriseUtc!!

        assertTrue("sunset precedes sunrise inside the session", sunset.isBefore(sunrise))
        assertTrue("both rise/set instants are inside the session window", sunset in session && sunrise in session)

        val civil = ephemeris.sunAltitudeCrossings(point, session, TwilightKind.CIVIL.angleDeg)
        assertTrue("civil twilight begins after sunset", civil[0].instantUtc.isAfter(sunset))
        assertTrue("civil twilight ends before sunrise", civil[1].instantUtc.isBefore(sunrise))

        // Derived: `searchRiseSet` searches for `centre + 16 arcmin = -34 arcmin` of refraction
        // (`astronomy.kt:6829-6844` and `:6682`), i.e. the centre at -0.8333 deg, geometric.
        assertEquals(
            "the 90.8333 deg zenith-distance test: the Sun's centre is 0.8333 deg below the horizon at sunset",
            -0.8333,
            ephemeris.sunHorizontal(point, sunset, AltitudeModel.GEOMETRIC).altitudeDeg,
            0.1,
        )
        assertEquals(
            "the same test at sunrise",
            -0.8333,
            ephemeris.sunHorizontal(point, sunrise, AltitudeModel.GEOMETRIC).altitudeDeg,
            0.1,
        )
    }

    @Test
    fun keyTimeRowsAreInDeclarationOrderAndEveryBandIsTyped() {
        val rows = midLat.keyTimes
        assertEquals("declaration order is the table's order", KeyTimeBand.entries.toList(), rows.map { it.band })

        for (row in rows) {
            for (interval in row.intervals) {
                assertFalse("a zero-length band is dropped", interval.isEmpty)
                assertTrue(
                    "every key-time band lies inside the session, was $interval",
                    !interval.startUtc.isBefore(midLatRequest.span.startUtc) &&
                        !interval.endUtc.isAfter(midLatRequest.span.endUtc),
                )
            }
            if (row.occurs) assertNotNull("an occurring row has an evening occurrence", row.evening)
        }

        val astronomical = rows.single { it.band == KeyTimeBand.ASTRONOMICAL }
        assertTrue("45 N in January reaches astronomical darkness", astronomical.occurs)
        assertEquals("the -18 row is the dark window", midLat.darkWindow.darkWindows, astronomical.intervals)

        // Derived: golden hour is [+6 deg, -4 deg] and blue hour is [-4 deg, -8 deg]
        // (KeyTimeBand.GOLDEN_HIGH_DEG / GOLDEN_LOW_DEG / BLUE_LOW_DEG), so the two are disjoint bands
        // of one altitude curve and their intervals can never overlap.
        val golden = rows.single { it.band == KeyTimeBand.GOLDEN_HOUR }
        val blue = rows.single { it.band == KeyTimeBand.BLUE_HOUR }
        assertTrue("a mid-latitude winter night has a golden hour", golden.occurs)
        assertTrue("a mid-latitude winter night has a blue hour", blue.occurs)
        for (g in golden.intervals) {
            for (b in blue.intervals) {
                assertTrue(
                    "golden hour and blue hour are distinct bands and must not overlap: $g vs $b",
                    g.endUtc <= b.startUtc || b.endUtc <= g.startUtc,
                )
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The polar cases — the precedence rule, exercised through the engine
    // ---------------------------------------------------------------------------------------------

    @Test
    fun aHighLatitudeMidsummerNightIsAPolarDay() {
        val computation = highLatSummer
        val kind = computation.darkWindow.kind

        // Derived: at 78 N on 21 June the Sun's declination is +23.44 deg, so its lowest altitude is
        // 23.44 + 78 - 90 = +11.4 deg: it never sets and never approaches the -0.8333 deg test.
        assertTrue(
            "the Sun stays well above the horizon at 78 N in June, minimum was ${computation.sun.sunAltMinDeg}",
            computation.sun.sunAltMinDeg >= 5.0,
        )
        assertTrue(
            "a 78 N June session reaches no darkness at all, kind was $kind",
            kind == DarkWindowKind.POLAR_DAY ||
                kind == DarkWindowKind.NO_DARK ||
                kind == DarkWindowKind.NO_ASTRO_DARK,
        )
        assertTrue("no dark window at all, was ${computation.darkWindow.darkWindows}", computation.darkWindow.darkWindows.isEmpty())
        assertFalse("the reminder is not offered for a night with no darkness", computation.darkWindow.reminderEligible)
        assertEquals(DarknessLevel.NONE, computation.skyGrade.level)
        assertEquals(
            "no dark window is a typed moonless answer, never an empty list shown as moonless all night",
            MoonlessKind.NO_DARK_WINDOW,
            computation.moonless.kind,
        )

        // The precedence rule where it is decided: with no sunset and the Sun never reaching the sunset
        // altitude the first branch fires; with a sunset but the Sun never below -6 deg it is NO_DARK.
        if (computation.sun.sunsetUtc == null) {
            assertEquals(
                "POLAR_DAY is first in the precedence order",
                DarkWindowKind.POLAR_DAY,
                kind,
            )
        } else {
            assertEquals(
                "a reported sunset with the Sun never reaching -6 deg is NO_DARK",
                DarkWindowKind.NO_DARK,
                kind,
            )
        }
    }

    @Test
    fun aPolarNightIsReportedAsPolarNightAheadOfDarkAndNoAstroDark() {
        val computation = highLatWinter
        val report = computation.darkWindow

        // Derived: at 78 N on 21 December the Sun's *highest* altitude is 90 - 78 - 23.44 = -11.4 deg
        // (below sunset, so it never rises) and its *lowest* is -23.44 + 78 - 90 = -35.4 deg (below
        // -18, so the session carries astronomical dark windows and a midday twilight dip). The trap
        // this case exists for is reporting DARK instead of POLAR_NIGHT when both conditions hold.
        assertTrue(
            "the Sun stays below the sunset altitude all day, maximum was ${computation.sun.sunAltMaxDeg}",
            computation.sun.sunAltMaxDeg < DarkWindowClassifier.SUNSET_GEOMETRIC_DEG,
        )
        assertTrue(
            "the midday peak is still above -18 deg, so the session is not wholly dark, maximum was ${computation.sun.sunAltMaxDeg}",
            computation.sun.sunAltMaxDeg > TwilightKind.ASTRONOMICAL.angleDeg,
        )
        assertTrue(
            "the night-side minimum reaches astronomical darkness, minimum was ${computation.sun.sunAltMinDeg}",
            computation.sun.sunAltMinDeg <= TwilightKind.ASTRONOMICAL.angleDeg,
        )

        assertEquals(
            "POLAR_NIGHT stands ahead of DARK and NO_ASTRO_DARK: the Sun never rises, so the more " +
                "specific fact is reported and the night is never two kinds at once",
            DarkWindowKind.POLAR_NIGHT,
            report.kind,
        )
        assertFalse(
            "the midday peak is above -18 deg, so this session is not an all-dark one",
            report.wholeNightDark,
        )
        assertTrue(
            "astronomical intervals exist under a polar night, was ${report.darkWindows}",
            report.darkWindows.isNotEmpty(),
        )
        assertTrue(report.hasDarkWindow)
        assertFalse(report.lacksAstronomicalDarkness)
        assertTrue(
            "a polar night that carries a dark window may offer the reminder",
            report.reminderEligible,
        )
        assertFalse(
            "moonless is computed against the dark windows, not refused as no-window",
            computation.moonless.kind == MoonlessKind.NO_DARK_WINDOW,
        )

        // Dark duration is non-zero, so SkyGrade consults sun depth and Moon interference rather than
        // the zero-duration POOR short-circuit. The exact band depends on the Moon that night; only
        // the structural claim is asserted here.
        assertFalse("dark duration is positive when -18 intervals exist", computation.skyGrade.darkDuration.isZero)
        assertTrue(
            "a polar night with darkness is never NONE",
            computation.skyGrade.level != DarknessLevel.NONE,
        )

        // The midday twilight dip and the dark windows partition the session.
        assertTrue("there is a midday twilight dip, was ${report.middayTwilightDip}", report.middayTwilightDip.isNotEmpty())
        assertEquals(
            highLatWinter.request.span.duration,
            NightSpan.total(report.darkWindows).plus(NightSpan.total(report.middayTwilightDip)),
        )
    }

    @Test
    fun aDeepPolarNightIsTheWholeSession() {
        val computation = deepLatWinter
        val report = computation.darkWindow

        // Derived: at 88 N on 21 December the Sun's highest altitude is 90 - 88 - 23.44 = -21.4 deg,
        // which is below -18 deg, so the whole session is the dark window.
        assertTrue(
            "the Sun stays below -18 deg all day, maximum was ${computation.sun.sunAltMaxDeg}",
            computation.sun.sunAltMaxDeg <= TwilightKind.ASTRONOMICAL.angleDeg,
        )
        assertEquals(DarkWindowKind.POLAR_NIGHT, report.kind)
        assertTrue("a deep polar night is dark for the whole session", report.wholeNightDark)
        assertEquals("the whole session is the dark window", listOf(deepLatWinter.request.span), report.darkWindows)
        assertTrue("and there is no midday dip", report.middayTwilightDip.isEmpty())
        assertEquals(deepLatWinter.request.span.duration, computation.skyGrade.darkDuration)
        assertTrue("a polar night with a dark window may offer the reminder", report.reminderEligible)
        assertFalse(report.hasTwoWindows)
    }

    // ---------------------------------------------------------------------------------------------
    // Purity, and the Moon
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theSameRequestComputedTwiceYieldsEqualComputations() {
        val first = NightEngine.compute(midLatRequest, ephemeris)
        val second = NightEngine.compute(midLatRequest, ephemeris)

        assertNotSame("two computations are two objects, so equality is a claim about their values", first, second)
        assertEquals(first, second)
        assertEquals(first.brief, second.brief)
        assertEquals(first.sun, second.sun)
        assertEquals(first.darkWindow, second.darkWindow)
        assertEquals(first.moon, second.moon)
        assertEquals(first.moonless, second.moonless)
        assertEquals(first.twilightWindows, second.twilightWindows)
        assertEquals(first.keyTimes, second.keyTimes)
        assertEquals(first.skyGrade, second.skyGrade)
        assertEquals(first.targetWindows, second.targetWindows)
        assertEquals(first.targetFits, second.targetFits)

        // The engine reads no clock: a request whose nowUtc moves 30 days produces the same sky. Only the
        // forecast's age could differ, and this request carries no forecast.
        val laterNow = NightEngine.compute(
            midLatRequest.copy(nowUtc = midLatRequest.nowUtc.plus(Duration.ofDays(30))),
            ephemeris,
        )
        assertEquals("no astronomical quantity depends on nowUtc", first.sun, laterNow.sun)
        assertEquals(first.darkWindow, laterNow.darkWindow)
        assertEquals(first.moon, laterNow.moon)
        assertEquals(first.moonless, laterNow.moonless)
        assertEquals(first.twilightWindows, laterNow.twilightWindows)
        assertEquals(first.keyTimes, laterNow.keyTimes)
        assertEquals(first.skyGrade, laterNow.skyGrade)
        assertEquals(first.targetWindows, laterNow.targetWindows)
        assertEquals(first.targetFits, laterNow.targetFits)
        assertEquals(first.brief.conditions, laterNow.brief.conditions)
    }

    @Test
    fun theMoonReportsItsIlluminatedFractionAlongsideItsAltitude() {
        val moon = midLat.moon

        assertTrue("a fraction in [0, 1], was ${moon.illuminatedFraction}", moon.illuminatedFraction in 0.0..1.0)
        assertTrue("an altitude in [-90, 90], was ${moon.altitudeDeg}", moon.altitudeDeg in -90.0..90.0)
        assertTrue("an azimuth in [0, 360), was ${moon.azimuthDeg}", moon.azimuthDeg >= 0.0 && moon.azimuthDeg < 360.0)
        assertTrue("an elongation in [0, 360), was ${moon.elongationDeg}", moon.elongationDeg >= 0.0 && moon.elongationDeg < 360.0)
        assertTrue("a phase angle in [0, 180], was ${moon.phaseAngleDeg}", moon.phaseAngleDeg in 0.0..180.0)

        // Derived from the vendored engine's own definition (`astronomy.kt:7298`):
        // `phaseFraction = (1 + cos(phaseAngle)) / 2`.
        assertEquals(
            "the illuminated fraction satisfies the engine's own relation to the phase angle",
            (1.0 + Math.cos(Math.toRadians(moon.phaseAngleDeg))) / 2.0,
            moon.illuminatedFraction,
            1e-9,
        )

        // Derived: the state names its phase from the elongation, so the two cannot disagree.
        assertEquals(MoonPhaseName.ofElongation(moon.elongationDeg), moon.phaseName)
        assertTrue("isUp is the horizon test", (moon.isUp == (moon.altitudeDeg > 0.0)))
        assertTrue("isDarkMoon is the stated 10 % product threshold", (moon.isDarkMoon == (moon.illuminatedFraction <= 0.10)))
        assertTrue("isFullMoon needs both the name and the fraction", (moon.isFullMoon == (moon.phaseName == MoonPhaseName.FULL_MOON && moon.illuminatedFraction >= 0.90)))
        assertEquals(moon.altitudeDeg, moon.position.altitudeDeg, 0.0)
        assertEquals(moon.azimuthDeg, moon.position.azimuthDeg, 0.0)

        // The grade carries both numbers together, never one without the other.
        assertEquals(moon.illuminatedFraction, midLat.skyGrade.moonIlluminatedFraction, 0.0)
        assertEquals(moon.altitudeDeg, midLat.skyGrade.moonAltitudeDeg, 0.0)
        assertTrue("interference is a function of exactly those two", midLat.skyGrade.moonInterference in 0.0..1.0)

        // The brief's Moon condition carries the same state, and the moonless answer is typed.
        val condition = midLat.brief.conditions.single { it.kind == ConditionKind.MOON }
        val facts = condition.facts
        assertTrue("the Moon condition carries Moon facts, was $facts", facts is BriefFacts.Moon)
        assertEquals(moon, (facts as BriefFacts.Moon).state)
        assertTrue(midLat.brief.moonless.kind in MoonlessKind.entries.toSet())
        assertEquals(midLat.moonless, midLat.brief.moonless)
    }

    // ---------------------------------------------------------------------------------------------
    // Targets, the effective horizon and the fit statements
    // ---------------------------------------------------------------------------------------------

    @Test
    fun targetWindowsDistinguishVisibleCircumpolarAndNeverRises() {
        val computation = midLat
        assertEquals("every catalogue row is classified", rows.map { it.id }.toSet(), computation.targetWindows.keys)
        assertEquals(
            "every classified row carries a fit statement",
            computation.targetWindows.keys,
            computation.targetFits.keys,
        )

        assertTrue(
            "M31 rises at 45 N, was ${computation.targetWindows.getValue("m31")}",
            computation.targetWindows.getValue("m31") is TargetWindow.Visible,
        )
        assertTrue(
            "a +75 deg row is circumpolar at 45 N, was ${computation.targetWindows.getValue("circumpolar")}",
            computation.targetWindows.getValue("circumpolar") is TargetWindow.Circumpolar,
        )
        assertTrue(
            "a -60 deg row never rises at 45 N, was ${computation.targetWindows.getValue("never-rises")}",
            computation.targetWindows.getValue("never-rises") is TargetWindow.NeverRises,
        )

        // Derived: at 45 N a declination d culminates at 90 - 45 + d and bottoms at 45 + d - 90.
        // The sample is the refracted altitude, so the never-rising row reads about 0.5 deg high (the
        // refraction at -15 deg) and the circumpolar row about 0.03 deg high.
        val circumpolar = computation.targetWindows.getValue("circumpolar") as TargetWindow.Circumpolar
        assertTrue(
            "the lowest point of a +75 deg row at 45 N is 45 + 75 - 90 = +30 deg, was ${circumpolar.minimumAltitudeDeg}",
            circumpolar.minimumAltitudeDeg in 29.0..31.0,
        )
        assertEquals(10.0, circumpolar.effectiveHorizonDeg, 1e-9)
        assertTrue("a circumpolar row is up tonight by definition", circumpolar.isVisibleTonight)

        val neverRises = computation.targetWindows.getValue("never-rises") as TargetWindow.NeverRises
        assertTrue(
            "the highest point of a -60 deg row at 45 N is 90 - 45 - 60 = -15 deg, was ${neverRises.maxAltitudeDeg}",
            neverRises.maxAltitudeDeg in -15.8..-14.0,
        )
        assertEquals(10.0, neverRises.effectiveHorizonDeg, 1e-9)
        assertFalse(neverRises.isVisibleTonight)
        assertTrue("the row is named as never rising rather than rendered as an empty window", neverRises.maxAltitudeDeg < neverRises.effectiveHorizonDeg)

        // The visible row's window is consistent with its own intervals.
        val visible = computation.targetWindows.getValue("m31") as TargetWindow.Visible
        assertEquals(visible.intervals.maxByOrNull { it.duration }, visible.bestInterval)
        assertEquals(visible.bestInterval?.span, visible.bestSpan)
        assertTrue("isVisibleTonight is the non-empty-interval test", visible.isVisibleTonight == visible.intervals.any { !it.isEmpty })
        assertTrue("onlyGrazing is the all-grazing test", visible.onlyGrazing == (visible.intervals.isNotEmpty() && visible.intervals.all { it.isGrazing }))
        if (visible.intervals.isEmpty()) {
            assertEquals("no interval is BELOW, never a blank window", ObjectVisibility.BELOW, visible.visibility)
        } else {
            assertEquals(ObjectVisibility.ABOVE, visible.visibility)
        }

        // Fit statements: 178 arcmin = 2.97 deg against the 24 mm full-frame short side of 53.1 deg.
        val fit = computation.targetFits.getValue("m31")
        assertEquals(FitVerdict.FITS, fit.verdict)
        assertEquals(178.0 / 60.0, fit.objectAngularSizeDeg!!, 1e-9)
        assertTrue("the frame's short side is the 24 mm lens's", fit.frameFovShortSideDeg > 50.0)
        assertEquals(fit.objectAngularSizeDeg!! / fit.frameFovShortSideDeg, fit.frameFrac!!, 1e-12)
        assertNull("no pixel count: the equipment profile states no pixel pitch", fit.pixelsOnSensor)
        assertEquals(FitVerdict.UNKNOWN_SIZE, computation.targetFits.getValue("point").verdict)
        assertNull(computation.targetFits.getValue("point").objectAngularSizeDeg)

        // "Visible tonight" is the brief's own derivation, not a second opinion.
        assertEquals(
            computation.targetWindows.filterValues { it.isVisibleTonight }.keys,
            computation.brief.visibleTargetIds,
        )
        assertTrue(computation.brief.visibleTargetIds.contains("circumpolar"))
        assertFalse(computation.brief.visibleTargetIds.contains("never-rises"))
    }

    @Test
    fun targetIntervalsAreClippedToTheDarkWindowAndClearTheEffectiveHorizon() {
        val computation = midLat
        val visible = computation.targetWindows.getValue("m31") as TargetWindow.Visible

        for (interval in visible.intervals) {
            assertFalse("a zero-length interval is dropped", interval.isEmpty)
            assertTrue(
                "an interval lies inside a dark window: $interval vs ${computation.darkWindows}",
                computation.darkWindows.any {
                    !it.startUtc.isAfter(interval.startUtc) && !it.endUtc.isBefore(interval.endUtc)
                },
            )
            assertTrue(
                "the whole interval clears its effective horizon: peak ${interval.peakAltitudeDeg} vs horizon ${interval.effectiveHorizonDeg}",
                interval.peakAltitudeDeg >= interval.effectiveHorizonDeg,
            )
            assertTrue(
                "the minimum inside the interval cannot exceed the peak",
                interval.minimumAltitudeDeg <= interval.peakAltitudeDeg,
            )
            assertEquals(
                "no profile is drawn, so the 10 deg altitude mask is the effective horizon",
                10.0,
                interval.effectiveHorizonDeg,
                1e-9,
            )
            assertFalse("with no drawn horizon the mask is what was crossed", interval.clearsDrawnHorizon)

            val transit = interval.transitUtc
            if (transit != null) {
                assertTrue(
                    "the transit lies inside its own interval",
                    !transit.isBefore(interval.startUtc) && !transit.isAfter(interval.endUtc),
                )
            }
            if (interval.startBoundary == WindowBoundary.DARK_WINDOW_START) {
                assertTrue(
                    "a dark-window start boundary is a dark window's own start",
                    computation.darkWindows.any { it.startUtc == interval.startUtc },
                )
            }
            if (interval.endBoundary == WindowBoundary.DARK_WINDOW_END) {
                assertTrue(
                    "a dark-window end boundary is a dark window's own end",
                    computation.darkWindows.any { it.endUtc == interval.endUtc },
                )
            }
        }

        val intervalTotal = visible.intervals.fold(Duration.ZERO) { acc, i -> acc.plus(i.duration) }
        assertTrue(
            "a target's visible time cannot exceed the dark window it is clipped to",
            intervalTotal <= NightSpan.total(computation.darkWindows),
        )
    }

    @Test
    fun aDrawnHorizonRaisesTheEffectiveHorizonAboveTheMask() {
        // Derived: the effective horizon is max(drawn ridge, altitude mask) = max(25, 10) = 25 deg.
        val ridgedNeverRises = ridged.targetWindows.getValue("never-rises") as TargetWindow.NeverRises
        assertEquals(25.0, ridgedNeverRises.effectiveHorizonDeg, 1e-9)
        assertTrue("its maximum is still below the raised horizon", ridgedNeverRises.maxAltitudeDeg < 25.0)

        val ridgedM31 = ridged.targetWindows.getValue("m31") as TargetWindow.Visible
        assertTrue("M31 still clears a 25 deg ridge", ridgedM31.intervals.isNotEmpty())
        for (interval in ridgedM31.intervals) {
            assertEquals("the drawn ridge is the effective horizon here", 25.0, interval.effectiveHorizonDeg, 1e-9)
            assertTrue("the ridge is above the mask, so the crossing is the drawn horizon", interval.clearsDrawnHorizon)
            assertTrue("and the whole interval clears it", interval.peakAltitudeDeg >= 25.0)
        }

        val horizonCondition = ridged.brief.conditions.single { it.kind == ConditionKind.HORIZON }
        assertFalse("a drawn horizon is not an absence", horizonCondition.isUnavailable)
        assertEquals(
            "no favourite is blocked at the azimuth of its peak, so the ridge reads GOOD",
            ConditionState.GOOD,
            horizonCondition.state,
        )
        val facts = horizonCondition.facts
        assertTrue("the horizon condition carries Horizon facts, was $facts", facts is BriefFacts.Horizon)
        assertEquals("ridge", (facts as BriefFacts.Horizon).profileId)
        assertEquals(ridgedRequest.horizon, ridged.horizon)
    }

    // ---------------------------------------------------------------------------------------------
    // The brief's own wiring
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theBriefCarriesEveryConditionAndItsTypedAbsences() {
        val brief = midLat.brief
        val kinds = brief.conditions.map { it.kind }

        assertEquals("the dark window is the first row", ConditionKind.DARK_WINDOW, brief.conditions.first().kind)
        assertTrue(
            "every condition kind the engine owns appears",
            kinds.containsAll(
                listOf(
                    ConditionKind.DARK_WINDOW,
                    ConditionKind.MOON,
                    ConditionKind.CLOUD,
                    ConditionKind.TARGET,
                    ConditionKind.HORIZON,
                ),
            ),
        )

        // An absent input becomes a typed absence and still appears — never a dropped row.
        val cloud = brief.conditions.single { it.kind == ConditionKind.CLOUD }
        assertTrue(cloud.isUnavailable)
        assertNull("an unavailable condition has no state", cloud.state)
        assertEquals(ConditionUnavailableReason.NOT_FETCHED, cloud.unavailableReason)
        assertNull(brief.cloud)

        val horizon = brief.conditions.single { it.kind == ConditionKind.HORIZON }
        assertEquals(ConditionUnavailableReason.HORIZON_MISSING, horizon.unavailableReason)

        val darkWindowCondition = brief.conditions.single { it.kind == ConditionKind.DARK_WINDOW }
        assertEquals(ConditionState.GOOD, darkWindowCondition.state)
        val darkFacts = darkWindowCondition.facts
        assertTrue("the dark-window condition carries the report itself, was $darkFacts", darkFacts is BriefFacts.DarkWindow)
        assertEquals(midLat.darkWindow, (darkFacts as BriefFacts.DarkWindow).report)

        // Every TARGET row is a favourite that is up tonight; the others earn no row.
        val targetRows = brief.conditions.filter { it.kind == ConditionKind.TARGET }
        assertTrue("a favourite that is up tonight earns a row", targetRows.isNotEmpty())
        for (row in targetRows) {
            val targetFacts = row.facts
            assertTrue("a TARGET row carries Target facts, was $targetFacts", targetFacts is BriefFacts.Target)
            val target = targetFacts as BriefFacts.Target
            assertTrue("only favourites earn TARGET rows", midLatRequest.favouriteIds.contains(target.targetId))
            assertTrue("and only ones that are up", target.window.isVisibleTonight)
        }
        assertTrue(
            "the circumpolar favourite is up and earns a row",
            targetRows.any { (it.facts as BriefFacts.Target).targetId == "circumpolar" },
        )
        assertFalse(
            "a never-rising favourite earns no row",
            targetRows.any { (it.facts as BriefFacts.Target).targetId == "never-rises" },
        )

        // Conditions are ordered chronologically, then by the fixed precedence.
        val anchor = brief.darkWindow.darkWindows.first().startUtc
        val expected = brief.conditions
            .map { it to (it.anchorUtc ?: anchor) }
            .sortedWith(compareBy({ it.second }, { it.first.precedence }))
            .map { it.first }
        assertEquals("the ordering rule is BriefOrdering's, applied once", expected, brief.conditions)

        // The brief's identity fields are the request's.
        assertEquals(midLatRequest.spot.id, brief.spotId)
        assertEquals(midLatRequest.spot.name, brief.spotName)
        assertEquals(midLatRequest.spot.tzId, brief.tzId)
        assertEquals(midLatRequest.spot.latDeg, brief.latitudeDeg, 0.0)
        assertEquals(midLatRequest.spot.lonDeg, brief.longitudeDeg, 0.0)
        assertEquals(midLatNight, brief.nightDate)
        assertEquals(SessionClock.dateLabel(midLatRequest.span, midLatZone), brief.nightDate)
        assertEquals(midLatRequest.span, brief.session)
        assertEquals(midLat.darkWindow, brief.darkWindow)
        assertEquals(midLat.keyTimes, brief.keyTimes)
        assertEquals(midLat.sunriseSunset, brief.sunriseSunset)
        assertEquals(midLat.moon, brief.moon)
        assertEquals(midLat.moonless, brief.moonless)
        assertEquals(midLat.targetWindows, brief.targetWindows)
        assertEquals(midLat.skyGrade, brief.skyGrade)
        assertEquals(midLatRequest.passes, brief.passes)
        assertNull(brief.cloud)
        assertEquals(midLatRequest.notices, brief.notices)
        assertEquals(midLatRequest.elementSetAge, brief.elementSetAge)
        assertTrue("only a real dark window offers the reminder", brief.offersReminder)
        assertTrue(brief.hasTargets)
        assertTrue(brief.visiblePasses.isEmpty())
        assertTrue("the -18 row occurs, so the brief can state it", brief.keyTimeRow(KeyTimeBand.ASTRONOMICAL) != null)
    }

    @Test
    fun theSceneTwilightBandAgreesWithTheSunGeometry() {
        val point = midLatRequest.point
        val midnight = midLatRequest.span.startUtc.plus(Duration.ofHours(12))
        assertEquals(
            "at 45 N in mid-January, local midnight is astronomical",
            TwilightKind.ASTRONOMICAL,
            SceneBuilder.twilightBandOf(ephemeris.sunHorizontal(point, midnight, AltitudeModel.GEOMETRIC).altitudeDeg),
        )
        val noon = midLatRequest.span.startUtc
        assertNull(
            "local noon in the same session is in no twilight band",
            SceneBuilder.twilightBandOf(ephemeris.sunHorizontal(point, noon, AltitudeModel.GEOMETRIC).altitudeDeg),
        )

        // Derived: the band function is a pure function of one altitude whose boundaries are the three
        // geometric thresholds themselves, so a frame at a boundary is in the band the brief calls it.
        assertEquals(TwilightKind.CIVIL, SceneBuilder.twilightBandOf(-6.0))
        assertEquals(TwilightKind.NAUTICAL, SceneBuilder.twilightBandOf(-12.0))
        assertEquals(TwilightKind.ASTRONOMICAL, SceneBuilder.twilightBandOf(-18.0))
        assertEquals(TwilightKind.CIVIL, SceneBuilder.twilightBandOf(-6.0001))
        assertNull(SceneBuilder.twilightBandOf(-5.9999))
        assertNull(SceneBuilder.twilightBandOf(0.0))
    }

    // ---------------------------------------------------------------------------------------------
    // The request's own invariants
    // ---------------------------------------------------------------------------------------------

    @Test
    fun aRequestWhoseZoneDisagreesWithItsSpotIsRefused() {
        val refusal = try {
            midLatRequest.copy(zone = ZoneId.of("Europe/Oslo"))
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        assertNotNull("a request whose zone disagrees with the spot's tzId must be refused, not computed", refusal)
        assertTrue(
            "the refusal names the zone, was '${refusal!!.message}'",
            refusal.message?.contains("zone") == true,
        )

        // The positive case, so the test is about disagreement and not about the values: the matching
        // request exists and has computed.
        assertEquals(midLatRequest.spot.tzId, midLatRequest.zone.id)
        assertEquals(midLatZone, midLatRequest.zone)
        assertTrue("and the session window is a local-noon-to-local-noon day", midLatRequest.span.duration == Duration.ofHours(24))
        assertEquals(noonUtcOf(midLatNight), midLatRequest.span.startUtc)
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** Local noon of [date] in the mid-latitude spot's zone, derived rather than recorded. */
    private fun noonUtcOf(date: LocalDate): Instant =
        date.atTime(12, 0).atZone(midLatZone).toInstant()

    /** Every band of [innerKind] must lie inside some band of [outerKind] — the thresholds nest. */
    private fun assertNested(
        windows: List<TwilightWindow>,
        innerKind: TwilightKind,
        outerKind: TwilightKind,
    ) {
        val outer = windows.filter { it.kind == outerKind }
        for (inner in windows.filter { it.kind == innerKind }) {
            assertTrue(
                "${innerKind.name} band $inner is not inside any ${outerKind.name} band: $outer",
                outer.any { o ->
                    !o.span.startUtc.isAfter(inner.span.startUtc) && !o.span.endUtc.isBefore(inner.span.endUtc)
                },
            )
        }
    }
}

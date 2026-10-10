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
 * The element-set age bands and the pass-visibility classifier.
 *
 * `specs/satellites/spec.md` / `domain/PassFinder.kt`: the bands are **product choices, not measured
 * constants** — `age <= 24 h` computes with no badge, `24 h < age <= 72 h` computes and states the age
 * prominently, `age > 72 h` **refuses to compute** any pass time. The visibility order is deliberate:
 * an umbral pass is stated as `IN_EARTH_SHADOW` even when the sky is also too bright, because the
 * shadow is the more specific reason.
 */
class PassFinderTest {

    private val epoch: Instant = Instant.parse("2026-10-09T00:00:00Z")

    private fun age(hours: Double, minutes: Long = 0) =
        ElementSetAge.from(epoch, epoch.plus(Duration.ofMinutes((hours * 60).toLong() + minutes)))

    private fun pass(
        visibility: PassVisibility = PassVisibility.OBSERVABLE,
        aos: Instant = epoch,
        los: Instant = epoch.plusSeconds(240),
    ) = SatellitePass(
        noradId = 25544L,
        satelliteName = "ISS (ZARYA)",
        objectId = "1998-067A",
        aosUtc = aos,
        losUtc = los,
        maxElevationDeg = 62.5,
        maxElevationUtc = aos.plusSeconds(120),
        subPointTrack = emptyList(),
        visibility = visibility,
        observerSunAltitudeDeg = -20.0,
        satelliteSunlit = true,
        elementSetEpochUtc = epoch,
        elementSetAge = ElementSetAge(Duration.ofHours(6), ElementSetAgeBand.FRESH),
    )

    // ------------------------------------------------------------- element-set age

    @Test
    fun aFreshElementSetComputesAndShowsNoBadge() {
        val fresh = age(0.0)
        assertEquals(ElementSetAgeBand.FRESH, fresh.band)
        assertTrue(fresh.mayCompute)
        assertFalse(fresh.showBadge)
        assertEquals(Duration.ZERO, fresh.age)
    }

    @Test
    fun exactlyTwentyFourHoursIsStillFresh() {
        val fresh = age(24.0)
        assertEquals(ElementSetAgeBand.FRESH, fresh.band)
        assertTrue(fresh.mayCompute)
        assertFalse(fresh.showBadge)
    }

    @Test
    fun oneSecondPastTwentyFourHoursIsAgingAndShowsTheBadge() {
        val aging = ElementSetAge.from(epoch, epoch.plus(Duration.ofHours(24)).plusSeconds(1))
        assertEquals(ElementSetAgeBand.AGING, aging.band)
        assertTrue(aging.mayCompute)
        assertTrue(aging.showBadge)
    }

    @Test
    fun exactlySeventyTwoHoursIsStillAging() {
        val aging = age(72.0)
        assertEquals(ElementSetAgeBand.AGING, aging.band)
        assertTrue(aging.mayCompute)
        assertTrue(aging.showBadge)
    }

    @Test
    fun oneSecondPastSeventyTwoHoursExpiresAndRefusesToCompute() {
        val expired = ElementSetAge.from(epoch, epoch.plus(Duration.ofHours(72)).plusSeconds(1))
        assertEquals(ElementSetAgeBand.EXPIRED, expired.band)
        assertFalse("expired elements must refuse to compute a pass time", expired.mayCompute)
        assertTrue(expired.showBadge)
    }

    @Test
    fun anElementSetFromTheFutureIsClampedToZeroRatherThanNegative() {
        val future = ElementSetAge.from(epoch, epoch.minus(Duration.ofHours(3)))
        assertEquals(Duration.ZERO, future.age)
        assertEquals(ElementSetAgeBand.FRESH, future.band)
        assertEquals(0.0, future.ageHours, 1e-12)
    }

    @Test
    fun ageHoursAndAgeDaysFollowFromTheDuration() {
        val thirtySix = age(36.0)
        assertEquals(36.0, thirtySix.ageHours, 1e-12)
        assertEquals(1.5, thirtySix.ageDays, 1e-12)
    }

    @Test
    fun theBandConstantsAreTheStatedTwentyFourAndSeventyTwoHours() {
        assertEquals(Duration.ofHours(24), ElementSetAge.FRESH_MAX)
        assertEquals(Duration.ofHours(72), ElementSetAge.EXPIRED_AFTER)
    }

    // --------------------------------------------------------------- visibility

    @Test
    fun aSatelliteInShadowIsInEarthShadowWhateverTheSkyIsDoing() {
        assertEquals(
            PassVisibility.IN_EARTH_SHADOW,
            SatellitePass.visibilityOf(satelliteSunlit = false, observerSunAltitudeDeg = -30.0),
        )
        // The umbra wins even when the sky would also be too bright: it is the more specific reason.
        assertEquals(
            PassVisibility.IN_EARTH_SHADOW,
            SatellitePass.visibilityOf(satelliteSunlit = false, observerSunAltitudeDeg = 20.0),
        )
    }

    @Test
    fun aSunlitSatelliteUnderABrightSkyIsTooBright() {
        assertEquals(
            PassVisibility.TOO_BRIGHT,
            SatellitePass.visibilityOf(satelliteSunlit = true, observerSunAltitudeDeg = -5.0),
        )
        assertEquals(
            PassVisibility.TOO_BRIGHT,
            SatellitePass.visibilityOf(satelliteSunlit = true, observerSunAltitudeDeg = 10.0),
        )
    }

    @Test
    fun theSunThresholdIsInclusiveAtMinusSixDegrees() {
        assertEquals(
            PassVisibility.OBSERVABLE,
            SatellitePass.visibilityOf(satelliteSunlit = true, observerSunAltitudeDeg = -6.0),
        )
        assertEquals(
            PassVisibility.OBSERVABLE,
            SatellitePass.visibilityOf(satelliteSunlit = true, observerSunAltitudeDeg = -20.0),
        )
    }

    @Test
    fun aCallerSuppliedThresholdIsHonoured() {
        assertEquals(
            PassVisibility.TOO_BRIGHT,
            SatellitePass.visibilityOf(satelliteSunlit = true, observerSunAltitudeDeg = -10.0, sunThresholdDeg = -12.0),
        )
        assertEquals(
            PassVisibility.OBSERVABLE,
            SatellitePass.visibilityOf(satelliteSunlit = true, observerSunAltitudeDeg = -13.0, sunThresholdDeg = -12.0),
        )
    }

    @Test
    fun theDefaultThresholdAndMaskAreTheStatedOnes() {
        assertEquals(-6.0, SatellitePass.DEFAULT_SUN_THRESHOLD_DEG, 1e-12)
        assertEquals(10.0, SatellitePass.DEFAULT_MASK_DEG, 1e-12)
        assertEquals(0.0, SatellitePass.MIN_MASK_DEG, 1e-12)
        assertEquals(45.0, SatellitePass.MAX_MASK_DEG, 1e-12)
    }

    // ------------------------------------------------------------------ the pass

    @Test
    fun aPassStatesItsDurationAndSpan() {
        val p = pass()
        assertEquals(Duration.ofMinutes(4), p.duration)
        assertEquals(NightSpan(epoch, epoch.plusSeconds(240)), p.span)
    }

    @Test
    fun observableIsTrueOnlyForTheObservableVisibility() {
        assertTrue(pass(PassVisibility.OBSERVABLE).observable)
        assertFalse(pass(PassVisibility.IN_EARTH_SHADOW).observable)
        assertFalse(pass(PassVisibility.TOO_BRIGHT).observable)
    }

    @Test
    fun aLosBeforeAosIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            pass(aos = epoch.plusSeconds(600), los = epoch)
        }
    }

    @Test
    fun aZeroLengthPassIsAllowedAsAnInstantaneousGeometry() {
        val p = pass(aos = epoch, los = epoch)
        assertEquals(Duration.ZERO, p.duration)
        assertTrue(p.span.isEmpty)
    }

    @Test
    fun theFrameCrossingUnknownCarriesNoClaimAtAll() {
        val unknown = FrameCrossing.UNKNOWN
        assertFalse(unknown.crosses)
        assertNull(unknown.closestApproachDeg)
        assertEquals(FrameCrossingConfidence.UNKNOWN, unknown.confidence)
        // And it is the default a pass carries when nothing has been computed for it.
        assertEquals(FrameCrossing.UNKNOWN, pass().frameCrossing)
    }
}

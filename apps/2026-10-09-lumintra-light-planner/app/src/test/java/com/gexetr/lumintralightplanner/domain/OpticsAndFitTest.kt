package com.gexetr.lumintralightplanner.domain

import kotlin.math.atan
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The optics: the millimetre field-of-view law and the three-way fit verdict.
 *
 * `specs/night-scene/spec.md`: `FOV_axis = 2·atan(d_axis / (2·f))` with both in millimetres and **no
 * unit conversion anywhere**; the 24 mm full-frame lens is the 73° × 53° the scene capability names.
 * The fit split is `FITS` at ≤ 0.8 of the frame's **short side**, `MARGINAL` at ≤ 1.0, `TOO_LARGE`
 * above, and `UNKNOWN_SIZE` with no numbers at all when the source states no apparent size.
 */
class OpticsAndFitTest {

    private val fullFrame24 = EquipmentProfile.DEFAULT.toLensPreset()

    private fun lens(focalLengthMm: Double, w: Double = 36.0, h: Double = 24.0, pitchUm: Double? = null) =
        LensPreset("test $focalLengthMm mm", w, h, focalLengthMm, pitchUm)

    // ------------------------------------------------------------------ the FOV law

    @Test
    fun theAxisLawIsTwoArctangentOfTheSensorOverTwiceTheFocalLength() {
        val expectedH = 2.0 * Math.toDegrees(atan(36.0 / (2.0 * 24.0)))
        val expectedV = 2.0 * Math.toDegrees(atan(24.0 / (2.0 * 24.0)))
        assertEquals(expectedH, FieldOfView.axisFovDeg(36.0, 24.0), 1e-12)
        assertEquals(expectedV, FieldOfView.axisFovDeg(24.0, 24.0), 1e-12)
    }

    @Test
    fun the24mmFullFrameFieldIs73Point7By53Point1Degrees() {
        assertEquals(73.7, FieldOfView.axisFovDeg(36.0, 24.0), 0.2)
        assertEquals(53.1, FieldOfView.axisFovDeg(24.0, 24.0), 0.2)

        val fov = FieldOfView.of(fullFrame24)
        assertEquals(73.7, fov.fovH, 0.2)
        assertEquals(53.1, fov.fovV, 0.2)
        // The diagonal, from the same law applied to the sensor diagonal.
        assertEquals(84.1, fov.fovDiag, 0.3)
    }

    @Test
    fun theShortAndLongSidesFollowTheSensorOrientation() {
        val landscape = FieldOfView.of(fullFrame24)
        assertEquals(53.1, landscape.shortSideDeg, 0.2)
        assertEquals(73.7, landscape.longSideDeg, 0.2)

        // A portrait-oriented sensor states the same two quantities the other way round.
        val portrait = FieldOfView.of(lens(24.0, w = 24.0, h = 36.0))
        assertEquals(53.1, portrait.shortSideDeg, 0.2)
        assertEquals(73.7, portrait.longSideDeg, 0.2)
    }

    @Test
    fun aLongerFocalLengthNarrowsTheField() {
        val short = FieldOfView.of(lens(24.0)).fovH
        val long = FieldOfView.of(lens(200.0)).fovH
        val expected200 = 2.0 * Math.toDegrees(atan(36.0 / 400.0))
        assertTrue(long < short)
        assertEquals(expected200, long, 1e-12)
    }

    @Test
    fun theCropFactorIsTheFullFrameDiagonalOverTheSensorsOwn() {
        assertEquals(1.0, fullFrame24.cropFactor, 1e-12)
        val apsc = lens(18.0, w = 23.6, h = 15.7)
        assertEquals(hypot(36.0, 24.0) / hypot(23.6, 15.7), apsc.cropFactor, 1e-12)
        assertTrue(apsc.cropFactor > 1.0)
    }

    @Test
    fun aNonPositiveSensorOrFocalLengthIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { lens(0.0) }
        assertThrows(IllegalArgumentException::class.java) { lens(24.0, w = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { lens(24.0, h = -1.0) }
    }

    @Test
    fun theDefaultEquipmentProfileIsTheFirstDeclaredPreset() {
        assertEquals(24.0, EquipmentProfile.DEFAULT.focalLengthMm, 1e-12)
        assertEquals(36.0, EquipmentProfile.DEFAULT.sensorWidthMm, 1e-12)
        assertEquals(24.0, EquipmentProfile.DEFAULT.sensorHeightMm, 1e-12)
        assertEquals(EquipmentProfile.DEFAULT, EquipmentProfile.PRESETS.first())
    }

    // --------------------------------------------------------------------- the fit

    @Test
    fun fitFractionIsTheObjectOverTheFrameShortSide() {
        val fov = FieldOfView.of(fullFrame24)
        val fit = TargetFit.of(600.0, fullFrame24) // 10°
        assertNotNull(fit.frameFrac)
        assertEquals(10.0, fit.objectAngularSizeDeg!!, 1e-12)
        assertEquals(10.0 / fov.shortSideDeg, fit.frameFrac!!, 1e-12)
        assertEquals(fov.shortSideDeg, fit.frameFovShortSideDeg, 1e-12)
    }

    @Test
    fun theBoundariesAtPointEightAndOneAreInclusiveOnTheFittingSide() {
        val shortSide = FieldOfView.of(fullFrame24).shortSideDeg

        val atFits = 60.0 * TargetFit.FITS_MAX * shortSide
        val atMarginal = 60.0 * TargetFit.MARGINAL_MAX * shortSide
        // A relative nudge of 1e-9 is ~7 orders of magnitude above double rounding, so this is a real
        // boundary test rather than a tolerance coin-flip.
        val belowFits = atFits * (1.0 - 1e-9)
        val aboveFits = atFits * (1.0 + 1e-9)
        val belowMarginal = atMarginal * (1.0 - 1e-9)
        val aboveMarginal = atMarginal * (1.0 + 1e-9)

        assertEquals(FitVerdict.FITS, TargetFit.of(belowFits, fullFrame24).verdict)
        assertEquals(FitVerdict.MARGINAL, TargetFit.of(aboveFits, fullFrame24).verdict)
        assertEquals(FitVerdict.MARGINAL, TargetFit.of(belowMarginal, fullFrame24).verdict)
        assertEquals(FitVerdict.TOO_LARGE, TargetFit.of(aboveMarginal, fullFrame24).verdict)

        assertEquals(TargetFit.FITS_MAX, TargetFit.of(atFits, fullFrame24).frameFrac!!, 1e-12)
        assertEquals(TargetFit.MARGINAL_MAX, TargetFit.of(atMarginal, fullFrame24).frameFrac!!, 1e-12)
    }

    @Test
    fun theStatedBoundaryConstantsAreTheOnesTheSpecificationNames() {
        assertEquals(0.8, TargetFit.FITS_MAX, 1e-12)
        assertEquals(1.0, TargetFit.MARGINAL_MAX, 1e-12)
    }

    @Test
    fun aTargetWithNoStatedSizeProducesUnknownSizeAndNoNumbers() {
        for (size in listOf(null, 0.0, -1.0)) {
            val fit = TargetFit.of(size, fullFrame24)
            assertEquals("size $size", FitVerdict.UNKNOWN_SIZE, fit.verdict)
            assertNull(fit.objectAngularSizeDeg)
            assertNull(fit.frameFrac)
            assertNull(fit.pixelsOnSensor)
            // The frame's own short side is still stated: the missing number is the target's.
            assertEquals(FieldOfView.of(fullFrame24).shortSideDeg, fit.frameFovShortSideDeg, 1e-12)
        }
    }

    @Test
    fun aSmallTargetFitsAndALargeOneDoesNot() {
        // M31's long axis is about 190 arcmin: comfortably inside a 53.1° short side.
        assertEquals(FitVerdict.FITS, TargetFit.of(190.0, fullFrame24).verdict)
        // 4000 arcmin = 66.7°, which is more than the 53.1° short side.
        assertEquals(FitVerdict.TOO_LARGE, TargetFit.of(4000.0, fullFrame24).verdict)
    }

    @Test
    fun aNarrowerLensTurnsAFittingTargetIntoATooLargeOne() {
        val wide = TargetFit.of(1200.0, fullFrame24) // 20°
        val narrow = TargetFit.of(1200.0, lens(200.0))
        assertEquals(FitVerdict.FITS, wide.verdict)
        assertEquals(FitVerdict.TOO_LARGE, narrow.verdict)
        assertTrue(narrow.frameFrac!! > wide.frameFrac!!)
    }

    @Test
    fun pixelsOnSensorIsTheFractionOfTheSensorsShortSideInPixels() {
        val pitchUm = 3.76
        val pitchLens = lens(24.0, pitchUm = pitchUm)
        val arcmin = 600.0
        val fit = TargetFit.of(arcmin, pitchLens)
        val expectedPixels = fit.frameFrac!! * (24.0 * 1000.0 / pitchUm)
        assertNotNull(fit.pixelsOnSensor)
        assertEquals(expectedPixels, fit.pixelsOnSensor!!, 1e-9)
        assertTrue(fit.pixelsOnSensor!! > 0.0)
    }

    @Test
    fun pixelsOnSensorIsAbsentWhenThePitchIsUnknown() {
        assertNull(TargetFit.of(600.0, fullFrame24).pixelsOnSensor)
    }

    @Test
    fun theVerdictIsDerivedFromTheFractionNotTheArcminutes() {
        // Same arcminute size, two sensors: the fraction decides, never the raw size.
        val small = TargetFit.of(1200.0, lens(24.0, w = 36.0, h = 24.0))
        val tiny = TargetFit.of(1200.0, lens(24.0, w = 5.6, h = 4.2))
        assertFalse(small.verdict == tiny.verdict)
        assertEquals(FitVerdict.TOO_LARGE, tiny.verdict)
    }
}

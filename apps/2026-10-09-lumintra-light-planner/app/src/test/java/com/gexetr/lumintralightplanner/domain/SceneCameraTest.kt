package com.gexetr.lumintralightplanner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scene camera's canvas geometry.
 *
 * `specs/night-scene/spec.md`: the canvas is the lens field with a **50 % context margin on each
 * side** (`CONTEXT_FACTOR = 2.0`), the horizon sits `HORIZON_OFFSET_DEG = 4.0` above the lower edge in
 * the default centring, the default azimuth is the antisolar meridian (180° north of the equator, 0°
 * south of it), a body horizontally outside the window projects to **`null`** rather than being
 * clamped onto an edge, and a body below the rect is **clamped** so the horizon layer can still draw
 * it as an occluded silhouette.
 */
class SceneCameraTest {

    private val fullFrame24 = EquipmentProfile.DEFAULT.toLensPreset()

    private fun camera(
        latitudeDeg: Double = 45.0,
        target: HorizontalCoords? = null,
        lens: LensPreset = fullFrame24,
    ): SceneCamera = SceneCamera.of(lens, latitudeDeg, target)

    // ------------------------------------------------------ the default 24 mm canvas

    @Test
    fun defaultCanvasIsTwiceTheLensField() {
        val lens = FieldOfView.of(fullFrame24)
        val cam = camera()
        assertEquals(SceneCamera.CONTEXT_FACTOR, 2.0, 1e-12)
        assertEquals(lens.fovH * 2.0, cam.fovH, 1e-9)
        assertEquals(lens.fovV * 2.0, cam.fovV, 1e-9)
    }

    /** "146° × 106° within 2 %": the spec's rounded statement of the doubled 24 mm field. */
    @Test
    fun defaultCanvasIsAbout146By106Degrees() {
        val cam = camera()
        assertEquals(146.0, cam.fovH, 146.0 * 0.02)
        assertEquals(106.0, cam.fovV, 106.0 * 0.02)
    }

    @Test
    fun defaultCentringPutsTheHorizonFourDegreesAboveTheLowerEdge() {
        val cam = camera()
        assertEquals(SceneCamera.HORIZON_OFFSET_DEG, 4.0, 1e-12)
        assertEquals(-4.0, cam.bottomAltitudeDeg, 1e-9)
        // The top is clamped at the zenith, never above it.
        assertEquals(90.0, cam.topAltitudeDeg, 1e-9)
    }

    @Test
    fun defaultCentringAzimuthIsTheAntisolarMeridianByHemisphere() {
        assertEquals(180.0, camera(latitudeDeg = 45.0).centerAzimuthDeg, 1e-12)
        assertEquals(180.0, camera(latitudeDeg = 0.0).centerAzimuthDeg, 1e-12)
        assertEquals(0.0, camera(latitudeDeg = -45.0).centerAzimuthDeg, 1e-12)
    }

    @Test
    fun defaultCentringIsFlaggedAsDefault() {
        assertTrue(camera().isDefaultCentring)
        assertFalse(camera(target = HorizontalCoords(30.0, 120.0)).isDefaultCentring)
    }

    // ------------------------------------------------------------- target centring

    @Test
    fun aTargetCentredFrameCentresTheTarget() {
        // A 200 mm lens, whose doubled vertical field fits inside the [-10°, +90°] band.
        val lens = EquipmentProfile("200 mm full frame", 200.0, 36.0, 24.0).toLensPreset()
        val cam = camera(latitudeDeg = 45.0, target = HorizontalCoords(30.0, 120.0), lens = lens)
        assertEquals(120.0, cam.centerAzimuthDeg, 1e-9)
        assertEquals(30.0, cam.centerAltitudeDeg, 1e-9)
    }

    @Test
    fun targetCentringClampsTheAltitudeIntoTheSupportedBand() {
        val lens = EquipmentProfile("200 mm full frame", 200.0, 36.0, 24.0).toLensPreset()
        val fovV = FieldOfView.of(lens).fovV * SceneCamera.CONTEXT_FACTOR
        val minCenter = SceneCamera.MIN_ALTITUDE_DEG + fovV / 2.0
        val maxCenter = SceneCamera.MAX_ALTITUDE_DEG - fovV / 2.0

        assertEquals(maxCenter, camera(target = HorizontalCoords(89.0, 180.0), lens = lens).centerAltitudeDeg, 1e-9)
        assertEquals(minCenter, camera(target = HorizontalCoords(-40.0, 180.0), lens = lens).centerAltitudeDeg, 1e-9)
    }

    @Test
    fun targetCentringNormalisesTheAzimuth() {
        val lens = EquipmentProfile("200 mm full frame", 200.0, 36.0, 24.0).toLensPreset()
        assertEquals(330.0, camera(target = HorizontalCoords(30.0, -30.0), lens = lens).centerAzimuthDeg, 1e-9)
        assertEquals(350.0, camera(target = HorizontalCoords(30.0, 350.0), lens = lens).centerAzimuthDeg, 1e-9)
    }

    /**
     * A **documented consequence** of the geometry rather than a defect in it: the doubled vertical
     * field of the default 24 mm lens is wider than the `[-10°, +90°]` band (100°), so no target-centred
     * centre can satisfy both clamps and the camera falls back to the band's midpoint, 40°.
     */
    @Test
    fun aFieldWiderThanTheAltitudeBandFallsBackToTheBandMidpoint() {
        val cam = camera(target = HorizontalCoords(30.0, 120.0))
        assertEquals(40.0, cam.centerAltitudeDeg, 1e-9)
        assertEquals(120.0, cam.centerAzimuthDeg, 1e-9)
    }

    // ----------------------------------------------------------------- projection

    @Test
    fun theCentreAzimuthProjectsToTheMiddleColumn() {
        val cam = camera()
        assertEquals(0.5f, cam.project(HorizontalCoords(20.0, 180.0))!!.xFraction, 1e-6f)
    }

    @Test
    fun projectionIsNotMirroredEastwardAzimuthGoesRight() {
        val cam = camera() // centred on 180°, fovH/2 = 73.7398°
        val halfWidth = cam.fovH / 2.0
        val right = cam.project(HorizontalCoords(20.0, 180.0 + halfWidth / 2.0))!!
        val left = cam.project(HorizontalCoords(20.0, 180.0 - halfWidth / 2.0))!!
        // Purely arithmetic: x = (dAz + fovH/2) / fovH.
        assertEquals(0.75f, right.xFraction, 1e-6f)
        assertEquals(0.25f, left.xFraction, 1e-6f)
        assertTrue("a larger azimuth must land further right", right.xFraction > left.xFraction)
    }

    @Test
    fun theWindowEdgesAreInclusiveAndJustOutsideIsNull() {
        val cam = camera()
        val halfWidth = cam.fovH / 2.0
        // A hair inside each edge projects; a hair outside is dropped. The margins are 1e-6°, far
        // above the ~1e-13° rounding of `(centre + halfWidth) - centre`, so this cannot flake.
        val justInsideRight = cam.project(HorizontalCoords(20.0, 180.0 + halfWidth - 1e-6))
        val justInsideLeft = cam.project(HorizontalCoords(20.0, 180.0 - halfWidth + 1e-6))
        assertNotNull(justInsideRight)
        assertNotNull(justInsideLeft)
        assertTrue(justInsideRight!!.xFraction > 0.99999f)
        assertTrue(justInsideLeft!!.xFraction < 0.00001f)
        assertNull(cam.project(HorizontalCoords(20.0, 180.0 + halfWidth + 1e-6)))
        assertNull(cam.project(HorizontalCoords(20.0, 180.0 - halfWidth - 1e-6)))
        // The left edge of the southern camera's window is exactly its centre azimuth minus the half
        // width, with no addition rounding involved.
        val south = camera(latitudeDeg = -45.0)
        assertEquals(1.0f, south.project(HorizontalCoords(20.0, halfWidth))!!.xFraction, 1e-6f)
    }

    /** A body behind the viewer must not project onto the screen (the named rule on `project`). */
    @Test
    fun aBodyBehindTheViewerProjectsToNull() {
        val cam = camera() // centre azimuth 180°
        assertNull(cam.project(HorizontalCoords(20.0, 350.0))) // dAz = +170°
        assertNull(cam.project(HorizontalCoords(20.0, 0.0))) // dAz = -180° (wrap180)
        assertNull(cam.project(HorizontalCoords(20.0, 10.0)))
    }

    @Test
    fun aSouthernCameraLooksNorthAndStillProjectsContinuously() {
        val cam = camera(latitudeDeg = -45.0) // centre azimuth 0°
        assertEquals(0.5f, cam.project(HorizontalCoords(20.0, 0.0))!!.xFraction, 1e-6f)
        assertEquals(0.5f, cam.project(HorizontalCoords(20.0, 360.0))!!.xFraction, 1e-6f)
        val halfWidth = cam.fovH / 2.0
        assertEquals(1.0f, cam.project(HorizontalCoords(20.0, halfWidth))!!.xFraction, 1e-6f)
        assertNull(cam.project(HorizontalCoords(20.0, 180.0)))
    }

    // -------------------------------------------------------- vertical: clamp, not null

    @Test
    fun aBodyBelowTheRectIsClampedNotDropped() {
        val cam = camera()
        val point = cam.project(HorizontalCoords(-30.0, 180.0))
        assertNotNull("a body below the rect must still be drawable as an occluded silhouette", point)
        assertEquals(1.0f, point!!.yFraction, 1e-6f)
    }

    @Test
    fun aBodyAboveTheZenithClampIsClampedToTheTopEdge() {
        val cam = camera()
        assertEquals(0.0f, cam.project(HorizontalCoords(95.0, 180.0))!!.yFraction, 1e-6f)
        assertEquals(0.0f, cam.project(HorizontalCoords(90.0, 180.0))!!.yFraction, 1e-6f)
    }

    @Test
    fun theHorizonProjectsFourNinetyFourthsOfTheWayDown() {
        val cam = camera()
        val span = cam.topAltitudeDeg - cam.bottomAltitudeDeg
        val horizon = cam.project(HorizontalCoords(0.0, 180.0))!!
        assertEquals(((cam.topAltitudeDeg - 0.0) / span).toFloat(), horizon.yFraction, 1e-6f)
        // And that fraction is the small band above the lower edge the 4° offset creates.
        assertEquals(4.0 / 94.0, 1.0 - horizon.yFraction.toDouble(), 1e-6)
    }

    // -------------------------------------------------------------------- wrap180

    @Test
    fun wrap180MapsIntoTheHalfOpenRange() {
        assertEquals(0.0, SceneCamera.wrap180(0.0), 1e-12)
        assertEquals(90.0, SceneCamera.wrap180(90.0), 1e-12)
        assertEquals(-90.0, SceneCamera.wrap180(270.0), 1e-12)
        assertEquals(-180.0, SceneCamera.wrap180(180.0), 1e-12)
        assertEquals(170.0, SceneCamera.wrap180(350.0 - 180.0), 1e-12)
        assertEquals(-10.0, SceneCamera.wrap180(350.0), 1e-12)
        assertEquals(0.0, SceneCamera.wrap180(360.0), 1e-12)
        assertEquals(0.0, SceneCamera.wrap180(720.0), 1e-12)
        assertEquals(-5.0, SceneCamera.wrap180(-5.0), 1e-12)
        assertEquals(-170.0, SceneCamera.wrap180(-170.0), 1e-12)
        // -190° normalises forward into the range: -190 + 360 = +170°.
        assertEquals(170.0, SceneCamera.wrap180(-190.0), 1e-12)
        assertEquals(-180.0, SceneCamera.wrap180(-180.0), 1e-12)
    }

    @Test
    fun altitudeBandConstantsAreTheStatedOnes() {
        assertEquals(-10.0, SceneCamera.MIN_ALTITUDE_DEG, 1e-12)
        assertEquals(90.0, SceneCamera.MAX_ALTITUDE_DEG, 1e-12)
    }
}

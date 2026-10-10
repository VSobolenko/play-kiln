package com.gexetr.lumintralightplanner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The active-spot rule and the spot/equipment value types.
 *
 * `specs/spots/spec.md` / `domain/Scout.kt`: the resolution must survive a cold start, a deleted spot
 * and an unrelated import, so an empty store has no active spot at all, a dangling id falls back to a
 * **real saved spot**, and the fallback is deterministic — the store's first record — so the app can
 * never appear to pick a random spot or fabricate a `(0, 0)` sky.
 */
class ScoutSelectionTest {

    private fun scout(id: String, name: String = "Spot $id", lat: Double = 59.91, lon: Double = 10.75) =
        Scout(id = id, name = name, latDeg = lat, lonDeg = lon, tzId = "Europe/Oslo")

    private val spots = listOf(scout("a"), scout("b"), scout("c"))

    // ------------------------------------------------------------------- resolve

    @Test
    fun anEmptyStoreResolvesToNull() {
        assertNull(ScoutSelection.resolve(emptyList(), "a"))
        assertNull(ScoutSelection.resolve(emptyList(), null))
    }

    @Test
    fun aMatchingIdResolvesToThatSpot() {
        assertEquals("b", ScoutSelection.resolve(spots, "b")!!.id)
        assertEquals("c", ScoutSelection.resolve(spots, "c")!!.id)
    }

    @Test
    fun aDanglingIdFallsBackToTheFirstSavedSpot() {
        assertEquals("a", ScoutSelection.resolve(spots, "does-not-exist")!!.id)
        assertEquals("a", ScoutSelection.resolve(spots, "")!!.id)
    }

    @Test
    fun aNullIdFallsBackToTheFirstSavedSpot() {
        assertEquals("a", ScoutSelection.resolve(spots, null)!!.id)
    }

    @Test
    fun theFallbackIsTheStoresFirstRecordNotAnArbitraryOne() {
        val reordered = listOf(scout("z"), scout("y"))
        assertEquals("z", ScoutSelection.resolve(reordered, "missing")!!.id)
    }

    // ----------------------------------------------------------------- reconcile

    @Test
    fun reconcileKeepsAnIdThatStillResolves() {
        assertEquals("b", ScoutSelection.reconcile(spots, "b"))
    }

    @Test
    fun reconcileMovesToTheFallbackAfterTheSelectedSpotIsDeleted() {
        val afterDelete = listOf(scout("a"), scout("c"))
        assertEquals("a", ScoutSelection.reconcile(afterDelete, "b"))
    }

    @Test
    fun reconcileOfAnEmptyStoreIsNull() {
        assertNull(ScoutSelection.reconcile(emptyList(), "b"))
        assertNull(ScoutSelection.reconcile(emptyList(), null))
    }

    // ---------------------------------------------------------------- isDangling

    @Test
    fun isDanglingIsTrueOnlyForARealDanglingReference() {
        assertTrue(ScoutSelection.isDangling(spots, "zzz"))
        assertFalse(ScoutSelection.isDangling(spots, "a"))
        // A null selection is not a dangling one; it is simply no selection.
        assertFalse(ScoutSelection.isDangling(spots, null))
        // With no spots at all there is nothing for the id to dangle from.
        assertFalse(ScoutSelection.isDangling(emptyList(), "zzz"))
    }

    // -------------------------------------------------------------- Scout invariants

    @Test
    fun anOutOfRangeCoordinateIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { scout("x", lat = 91.0) }
        assertThrows(IllegalArgumentException::class.java) { scout("x", lat = -90.5) }
        assertThrows(IllegalArgumentException::class.java) { scout("x", lon = 181.0) }
        assertThrows(IllegalArgumentException::class.java) { scout("x", lon = -180.5) }
    }

    @Test
    fun aBlankZoneIdIsRejectedBecauseTheClockMustResolveDst() {
        assertThrows(IllegalArgumentException::class.java) {
            Scout(id = "x", name = "X", latDeg = 0.0, lonDeg = 0.0, tzId = "  ")
        }
    }

    @Test
    fun mergeKeyIsTheNameAndTheStoredCoordinates() {
        val s = scout("a", name = "Ridge", lat = 59.9123, lon = 10.7456)
        assertEquals("Ridge|59.9123|10.7456", s.mergeKey())
        assertEquals(s.mergeKey(), scout("a", name = "Ridge", lat = 59.9123, lon = 10.7456).mergeKey())
        assertFalse(s.mergeKey() == scout("a", name = "Ridge", lat = 59.9124, lon = 10.7456).mergeKey())
    }

    // ------------------------------------------------------------------ GeodeticPoint

    @Test
    fun theNetworkPointIsTruncatedToFourDecimalsTowardZero() {
        // api.met.no returns 403 for 5+ decimals, so this is a protocol requirement, not precision.
        assertEquals(59.9123, GeodeticPoint.truncateTo4(59.912389), 1e-12)
        assertEquals(-59.9123, GeodeticPoint.truncateTo4(-59.912389), 1e-12)
        assertEquals(10.0, GeodeticPoint.truncateTo4(10.0), 1e-12)
        assertEquals(0.4999, GeodeticPoint.truncateTo4(0.499999), 1e-12)

        val point = scout("a", lat = 59.912389, lon = -10.745699).networkPoint
        assertEquals(59.9123, point.latDeg, 1e-12)
        assertEquals(-10.7456, point.lonDeg, 1e-12)
    }

    @Test
    fun aGeodeticPointOutsideTheCoordinateRangesIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { GeodeticPoint(91.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { GeodeticPoint(0.0, 200.0) }
    }

    @Test
    fun theCoordinateLabelsCarryTheHemisphereLetters() {
        assertEquals("59°54′ N, 10°45′ E", GeodeticPoint(59.9123, 10.75).label())
        assertEquals("59°54′ S, 10°45′ W", GeodeticPoint(-59.9123, -10.75).label())
    }

    // ------------------------------------------------------------------- equipment

    @Test
    fun theEquipmentProfileConvertsToTheLensFormItComputesFrom() {
        val profile = EquipmentProfile("My rig", 135.0, 36.0, 24.0, "135 mm full frame")
        val lens = profile.toLensPreset(pixelPitchUm = 3.76)
        assertEquals(135.0, lens.focalLengthMm, 1e-12)
        assertEquals(36.0, lens.sensorWidthMm, 1e-12)
        assertEquals(24.0, lens.sensorHeightMm, 1e-12)
        assertEquals(3.76, lens.pixelPitchUm!!, 1e-12)
        // The preset name is preferred, so a preset that happens to match stays recognisable.
        assertEquals("135 mm full frame", lens.name)
    }

    @Test
    fun aCustomProfileWithoutAPresetNameKeepsItsOwnName() {
        assertEquals("My rig", EquipmentProfile("My rig", 135.0, 36.0, 24.0).toLensPreset().name)
    }

    @Test
    fun aNonPositiveEquipmentDimensionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { EquipmentProfile("x", 0.0, 36.0, 24.0) }
        assertThrows(IllegalArgumentException::class.java) { EquipmentProfile("x", 24.0, 0.0, 24.0) }
        assertThrows(IllegalArgumentException::class.java) { EquipmentProfile("x", 24.0, 36.0, -24.0) }
    }

    @Test
    fun theDefaultEquipmentIsThe24mmFullFrameEntry() {
        assertEquals("24 mm full frame", EquipmentProfile.DEFAULT.name)
        assertEquals(24.0, EquipmentProfile.DEFAULT.focalLengthMm, 1e-12)
        assertEquals(EquipmentProfile.DEFAULT, EquipmentProfile.PRESETS.first())
        assertTrue(EquipmentProfile.PRESETS.size >= 5)
        // Every declared preset is genuinely usable, not a placeholder row.
        assertTrue(EquipmentProfile.PRESETS.all { it.focalLengthMm > 0.0 && it.sensorWidthMm > 0.0 })
    }
}

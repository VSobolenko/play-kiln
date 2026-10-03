package com.gexetr.laneset.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceModelTest {

    @Test
    fun thresholdPaceIsHalfTheGapBetweenTheTwoTrials() {
        // (320 - 150) / 2 = 85 s per 100 m.
        assertEquals(85.0, PaceModel.thresholdPacePer100(320.0, 150.0), 0.0001)
        assertEquals(80.0, PaceModel.thresholdPacePer100(300.0, 140.0), 0.0001)
    }

    @Test
    fun aSlowerFourHundredGivesASlowerThresholdPace() {
        val quicker = PaceModel.thresholdPacePer100(310.0, 150.0)
        val slower = PaceModel.thresholdPacePer100(330.0, 150.0)
        assertTrue(slower > quicker)
    }

    @Test
    fun intervalTargetScalesWithDistance() {
        assertEquals(85, PaceModel.intervalTargetSeconds(85.0, 100))
        assertEquals(170, PaceModel.intervalTargetSeconds(85.0, 200))
        assertEquals(255, PaceModel.intervalTargetSeconds(85.0, 300))
        // 85 * 75 / 100 = 63.75 -> 64.
        assertEquals(64, PaceModel.intervalTargetSeconds(85.0, 75))
    }

    @Test
    fun intervalTargetIsNeverZeroForATinyDistance() {
        assertEquals(1, PaceModel.intervalTargetSeconds(85.0, 1))
    }

    @Test
    fun anImpossibleTrialPairIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PaceModel.thresholdPacePer100(150.0, 150.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PaceModel.thresholdPacePer100(300.0, 400.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PaceModel.thresholdPacePer100(300.0, 0.0)
        }
    }

    @Test
    fun aNonPositiveIntervalInputIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PaceModel.intervalTargetSeconds(0.0, 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PaceModel.intervalTargetSeconds(85.0, 0)
        }
    }
}

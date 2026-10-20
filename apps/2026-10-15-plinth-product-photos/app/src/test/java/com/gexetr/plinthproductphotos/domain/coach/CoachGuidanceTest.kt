package com.gexetr.plinthproductphotos.domain.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachGuidanceTest {
    @Test
    fun qualityFailuresNameTheirCheckAndRemedy() {
        val report = CoachQuality.evaluate(
            CoachQualityInput(
                width = 100,
                height = 100,
                presetAspect = "3:4",
                sharpnessScore = 0.1,
                minimumSharpnessScore = 0.5,
                clippedExposureFraction = 0.2,
                maximumClippedExposureFraction = 0.1,
                itemFillRatio = 0.7,
                minimumFillRatio = 0.8,
            ),
        )

        assertEquals(
            CoachRemedy.STEADY_AND_FOCUS,
            report.single { it.check == CoachCheck.SHARPNESS }.remedy,
        )
        assertEquals(
            CheckVerdict.NEEDS_ATTENTION,
            report.single { it.check == CoachCheck.EXPOSURE }.verdict,
        )
        assertEquals(
            CoachRemedy.ADJUST_LIGHT_AND_EXPOSURE,
            report.single { it.check == CoachCheck.EXPOSURE }.remedy,
        )
        assertEquals(
            CoachRemedy.MOVE_CLOSER_WITHOUT_CROPPING,
            report.single { it.check == CoachCheck.FILL }.remedy,
        )
        assertEquals(
            CoachRemedy.REPOSITION_TO_MATCH_FRAME,
            report.single { it.check == CoachCheck.ASPECT }.remedy,
        )
        assertTrue(report.all { it.verdict == CheckVerdict.NEEDS_ATTENTION })
    }

    @Test
    fun measuredAspectAndFillInsidePresetPass() {
        val report = CoachQuality.evaluate(
            CoachQualityInput(
                width = 900,
                height = 1200,
                presetAspect = "3:4",
                itemFillRatio = 0.9,
                minimumFillRatio = 0.85,
            ),
        )

        assertEquals(CheckVerdict.UNVERIFIED, report.single { it.check == CoachCheck.SHARPNESS }.verdict)
        assertEquals(CheckVerdict.UNVERIFIED, report.single { it.check == CoachCheck.EXPOSURE }.verdict)
        assertEquals(CheckVerdict.PASS, report.single { it.check == CoachCheck.FILL }.verdict)
        assertEquals(CheckVerdict.PASS, report.single { it.check == CoachCheck.ASPECT }.verdict)
    }

    @Test
    fun levelDeadZoneIncludesItsBoundary() {
        assertTrue(LevelGuide.isLevel(2.0))
        assertTrue(LevelGuide.isLevel(-2.0))
        assertFalse(LevelGuide.isLevel(2.01))
    }

    @Test
    fun shotListAdvancesAndTracksCompletedShotsPerCategory() {
        val progress = ShotListProgress.forCategory("clothing")
        val next = progress.completeCurrent()

        assertEquals(5, progress.shots.size)
        assertEquals(1, next.completedCount)
        assertEquals("back", next.currentShot?.id)
        assertEquals("front", next.completedShots.single().id)
        assertTrue(progress.shots.single { it.id == "label" }.isReference)
        assertTrue(progress.shots.single { it.id == "measurement" }.isReference)
        assertFalse(progress.shots.single { it.id == "front" }.isReference)
    }

    @Test
    fun shotListContentAndProgressAreIndependentForEachCategory() {
        val expectedNext = mapOf(
            "shoes" to "side",
            "jewellery" to "side",
            "electronics" to "back",
            "other" to "back",
        )

        expectedNext.forEach { (category, nextShotId) ->
            val progress = ShotListProgress.forCategory(category)
            val advanced = progress.completeCurrent()

            assertEquals(category, advanced.category)
            assertEquals(nextShotId, advanced.currentShot?.id)
            assertEquals(1, advanced.completedCount)
        }
    }
}

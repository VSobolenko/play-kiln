package com.gexetr.plinthproductphotos.domain.autocard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionImageTest {
    @Test
    fun realDeviceSquarePhotoKeepsExactly1024Pixels() {
        assertEquals(RecognitionImage.Size(1024, 1024), RecognitionImage.targetSize(2344, 2344))
    }

    @Test
    fun upscalesDownscalesAndRoundsTheShortSide() {
        val cases = listOf(
            (800 to 600) to RecognitionImage.Size(1024, 768),
            (600 to 800) to RecognitionImage.Size(768, 1024),
            (1024 to 768) to RecognitionImage.Size(1024, 768),
            (4032 to 3024) to RecognitionImage.Size(1024, 768),
            (1 to 1) to RecognitionImage.Size(1024, 1024),
            (4000 to 3000) to RecognitionImage.Size(1024, 768),
            (3 to 2) to RecognitionImage.Size(1024, 683),
            (2 to 3) to RecognitionImage.Size(683, 1024),
        )

        for ((source, expected) in cases) {
            assertEquals(
                "${source.first} x ${source.second}",
                expected,
                RecognitionImage.targetSize(source.first, source.second),
            )
        }
    }

    @Test
    fun positiveDimensionsAlwaysHaveAnExactLongestSideAndNonzeroShortSide() {
        for (width in 1..6000) {
            for (height in listOf(1, width, 6001 - width, 6000)) {
                val target = RecognitionImage.targetSize(width, height)

                assertEquals(1024, maxOf(target.width, target.height))
                assertTrue(target.width >= 1)
                assertTrue(target.height >= 1)
            }
        }
    }

    @Test
    fun extremeDimensionsDoNotOverflowOrRoundToZero() {
        assertEquals(
            RecognitionImage.Size(1024, 1024),
            RecognitionImage.targetSize(Int.MAX_VALUE, Int.MAX_VALUE),
        )
        assertEquals(
            RecognitionImage.Size(1024, 1),
            RecognitionImage.targetSize(Int.MAX_VALUE, 1),
        )
        assertEquals(
            RecognitionImage.Size(1, 1024),
            RecognitionImage.targetSize(1, Int.MAX_VALUE),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroWidth() {
        RecognitionImage.targetSize(0, 600)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroHeight() {
        RecognitionImage.targetSize(800, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeWidth() {
        RecognitionImage.targetSize(-1, 600)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeHeight() {
        RecognitionImage.targetSize(800, -1)
    }
}

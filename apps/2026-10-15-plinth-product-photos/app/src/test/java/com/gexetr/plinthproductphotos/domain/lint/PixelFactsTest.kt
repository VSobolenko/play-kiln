package com.gexetr.plinthproductphotos.domain.lint

import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.pipeline.PixelBounds
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PixelFactsTest {
    @Test
    fun alignmentCropsSourceAndPreservesHolesAtThePlacedLocation() {
        val sourceMask = Mask(byteArrayOf(
            0, 0, 0, 0,
            0, -1, 0, 0,
            0, -1, -1, 0,
            0, 0, 0, 0,
        ))
        val image = PixelBuffer.solid(6, 6, PixelBuffer.rgba(255, 255, 255))
        val aligned = PixelFactsMeasurement.alignMask(4, 4, sourceMask, image, PixelBounds(2, 1, 2, 2))

        assertEquals(255, aligned.alpha[1 * 6 + 2].toInt() and 0xff)
        assertEquals(0, aligned.alpha[1 * 6 + 3].toInt())
        assertEquals(255, aligned.alpha[2 * 6 + 3].toInt() and 0xff)
        assertEquals(3.0 / 36, PixelFactsMeasurement.measure(image, aligned).itemFillRatio, 0.000001)
    }

    @Test
    fun measurementUsesRgbaChannelsExcludesItemAndDoesNotMutateInputs() {
        val image = PixelBuffer.solid(3, 3, PixelBuffer.rgba(255, 255, 255))
        image.pixels[4] = PixelBuffer.rgba(255, 0, 0)
        image.pixels[0] = PixelBuffer.rgba(254, 255, 255)
        val mask = Mask(ByteArray(9).also { it[4] = -1 })
        val pixelsBefore = image.pixels.copyOf()
        val maskBefore = mask.alpha.copyOf()
        val facts = PixelFactsMeasurement.measure(image, mask)

        assertEquals(1.0 / 9, facts.itemFillRatio, 0.000001)
        assertEquals(254, facts.borderMinimumChannelOutsideItem)
        assertEquals(0.0, requireNotNull(facts.backgroundMeanChroma), 0.1)
        assertArrayEquals(pixelsBefore, image.pixels)
        assertArrayEquals(maskBefore, mask.alpha)
    }

    @Test
    fun meanChromaIsMeanOfPerPixelLabChromaNotChromaOfAverageRgb() {
        val image = PixelBuffer(2, 1, intArrayOf(
            PixelBuffer.rgba(255, 0, 0), PixelBuffer.rgba(0, 255, 0),
        ))
        val facts = PixelFactsMeasurement.measure(image, Mask(ByteArray(2)))
        assertEquals((104.5518 + 119.7759) / 2, requireNotNull(facts.backgroundMeanChroma), 0.001)
    }

    @Test
    fun noBackgroundOrOutsideBorderIsUnmeasuredRatherThanWhiteOrNeutral() {
        val image = PixelBuffer.solid(3, 3, PixelBuffer.rgba(20, 30, 40))
        val fullMask = Mask(ByteArray(9) { -1 })
        val fullFacts = PixelFactsMeasurement.measure(image, fullMask)
        assertEquals(1.0, fullFacts.itemFillRatio, 0.0)
        assertNull(fullFacts.backgroundMeanChroma)
        assertNull(fullFacts.borderMinimumChannelOutsideItem)

        val holeMask = Mask(fullMask.alpha.copyOf().also { it[4] = 0 })
        assertNull(PixelFactsMeasurement.measure(image, holeMask).borderMinimumChannelOutsideItem)
    }

    @Test
    fun featherCoverageIsWeightedAndNotSampledAsBackground() {
        val facts = PixelFactsMeasurement.measure(
            PixelBuffer.solid(1, 1, PixelBuffer.rgba(255, 0, 0)),
            Mask(byteArrayOf(127)),
        )
        assertEquals(127.0 / 255, facts.itemFillRatio, 0.000001)
        assertNull(facts.backgroundMeanChroma)
        assertNull(facts.borderMinimumChannelOutsideItem)
    }

    @Test
    fun fittingMaskAddsOutsideItemPaddingWithoutChangingSourceMask() {
        val mask = Mask(ByteArray(4) { -1 })
        val fitted = PixelFactsMeasurement.fitMask(mask, 2, 2, 2, 4)
        assertArrayEquals(byteArrayOf(0, 0, -1, -1, -1, -1, 0, 0), fitted.alpha)
        assertArrayEquals(ByteArray(4) { -1 }, mask.alpha)
    }
}

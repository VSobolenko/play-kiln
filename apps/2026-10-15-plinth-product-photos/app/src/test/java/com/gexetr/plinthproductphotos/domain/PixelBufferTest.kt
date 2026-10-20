package com.gexetr.plinthproductphotos.domain

import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelBufferTest {
    @Test
    fun cropCopiesOnlyTheRequestedRectangle() {
        val source = PixelBuffer(
            2,
            2,
            intArrayOf(
                PixelBuffer.rgba(1, 0, 0, 255),
                PixelBuffer.rgba(2, 0, 0, 255),
                PixelBuffer.rgba(3, 0, 0, 255),
                PixelBuffer.rgba(4, 0, 0, 255),
            ),
        )

        val cropped = source.crop(left = 1, top = 0, width = 1, height = 2)

        assertEquals(1, cropped.width)
        assertEquals(2, cropped.height)
        assertEquals(PixelBuffer.rgba(2, 0, 0, 255), cropped[0, 0])
        assertEquals(PixelBuffer.rgba(4, 0, 0, 255), cropped[0, 1])
    }

    @Test
    fun scaleReturnsRequestedDimensionsAndPreservesOpaqueAlpha() {
        val source = PixelBuffer(
            2,
            1,
            intArrayOf(PixelBuffer.rgba(255, 0, 0, 255), PixelBuffer.rgba(0, 0, 255, 255)),
        )

        val scaled = source.scale(4, 1)

        assertEquals(4, scaled.width)
        assertEquals(1, scaled.height)
        assertTrue(scaled.pixels.all { (it and 0xff) == 255 })
    }

    @Test
    fun maskedCompositeLeavesZeroAlphaPixelsAlone() {
        val background = PixelBuffer.solid(1, 1, PixelBuffer.rgba(20, 40, 60, 255))
        val foreground = PixelBuffer.solid(1, 1, PixelBuffer.rgba(200, 100, 50, 255))

        val result = foreground.compositeOver(background, 0, 0, byteArrayOf(0))

        assertEquals(background[0, 0], result[0, 0])
    }

    @Test
    fun maskedCompositeBlendsPartialMaskAlpha() {
        val background = PixelBuffer.solid(1, 1, PixelBuffer.rgba(0, 0, 255, 255))
        val foreground = PixelBuffer.solid(1, 1, PixelBuffer.rgba(255, 0, 0, 255))

        val result = foreground.compositeOver(background, 0, 0, byteArrayOf(0x80.toByte()))

        assertEquals(PixelBuffer.rgba(128, 0, 127, 255), result[0, 0])
    }

    @Test
    fun gradientInterpolatesBetweenItsEndpointColors() {
        val gradient = PixelBuffer.gradient(
            width = 3,
            height = 1,
            startArgb = 0xffff0000.toInt(),
            endArgb = 0xff0000ff.toInt(),
            angleDeg = 0,
        )

        assertEquals(PixelBuffer.rgba(255, 0, 0, 255), gradient[0, 0])
        assertEquals(PixelBuffer.rgba(0, 0, 255, 255), gradient[2, 0])
    }

    @Test
    fun dropShadowAddsOnlyAWeightedBlackSilhouette() {
        val background = PixelBuffer.solid(5, 5, PixelBuffer.rgba(240, 240, 240, 255))
        val mask = ByteArray(25)
        mask[2 * 5 + 2] = 0xff.toByte()

        val result = background.dropShadow(mask, opacity = 0.5f, blurRadius = 0f)

        assertNotEquals(background[2, 2], result[2, 2])
        assertEquals(background[0, 0], result[0, 0])
    }

    @Test
    fun invalidDimensionsAndMaskLengthsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PixelBuffer(width = 0, height = 1, pixels = intArrayOf())
        }
        assertThrows(IllegalArgumentException::class.java) {
            PixelBuffer.solid(2, 2, PixelBuffer.rgba(0, 0, 0, 255))
                .compositeOver(
                    PixelBuffer.solid(2, 2, PixelBuffer.rgba(0, 0, 0, 255)),
                    0,
                    0,
                    byteArrayOf(0),
                )
        }
    }
}

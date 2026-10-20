package com.gexetr.plinthproductphotos.domain

import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelBufferShadowTest {
    @Test
    fun fullResolutionAutoCardShadowFinishesWithinTwoSeconds() {
        val size = 2344
        val background = PixelBuffer.solid(size, size, PixelBuffer.rgba(240, 238, 235))
        val original = background.pixels.copyOf()
        val mask = ByteArray(size * size)
        for (y in 300 until 1900) {
            for (x in 650 until 1700) mask[y * size + x] = 255.toByte()
        }
        val started = System.nanoTime()
        val result = background.dropShadow(mask, 0.28f, size * 0.02f, offsetY = 50)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        println("2344x2344 shadow: ${elapsedMs}ms")
        assertTrue("Full-resolution shadow took ${elapsedMs}ms (budget 2000ms)", elapsedMs < 2000)
        assertEquals(size, result.width)
        assertEquals(size, result.height)
        assertArrayEquals("Original product/background pixels must not be rewritten", original, background.pixels)
        assertTrue(result[1100, 1100] != background[1100, 1100])
    }

    @Test
    fun broadShadowsStayWithinFourAlphaLevelsOfGaussianIncludingCanvasEdges() {
        val cases = listOf(0.5f, 3f, 4f, 5f, 8f, 10f, 11f, 12f, 18f, 32f)
            .map { 129 to it } + (257 to 46.88f)
        for ((size, sigma) in cases) {
            val mask = ByteArray(size * size) { index ->
                val x = index % size
                val y = index / size
                when {
                    x < 45 && y < 70 -> 255.toByte()
                    x in 65..100 && y in 40..110 -> 160.toByte()
                    else -> 0
                }
            }
            val expected = referenceGaussian(mask, size, size, sigma)
            val actual = transparent(size, size).dropShadow(mask, 1f, sigma)
            val maxError = actual.pixels.indices.maxOf { abs((actual.pixels[it] and 255) - expected[it]) }
            println("sigma=$sigma Gaussian maximum alpha error: $maxError/255")
            assertTrue("sigma=$sigma maximum alpha error $maxError exceeds 4/255", maxError <= 4)
        }
    }

    @Test
    fun zeroSigmaKeepsMaskAndOffsetsExact() {
        val result = transparent(3, 2).dropShadow(
            byteArrayOf(255.toByte(), 128.toByte(), 0, 0, 0, 0), 1f, 0f, offsetX = 1, offsetY = 1,
        )
        assertArrayEquals(intArrayOf(0, 0, 0, 0, 255, 128), result.pixels)
    }

    @Test
    fun thinAndSinglePixelCanvasesUseTransparentOutsideRatherThanClampedEdges() {
        for ((width, height) in listOf(1 to 1, 1 to 25, 25 to 1, 2 to 17)) {
            for (sigma in listOf(0.5f, 3f, 12f)) {
                val mask = ByteArray(width * height)
                mask[0] = 255.toByte()
                val actual = transparent(width, height).dropShadow(mask, 1f, sigma)
                assertArrayEquals(
                    "${width}x$height sigma=$sigma",
                    referenceGaussian(mask, width, height, sigma),
                    actual.pixels,
                )
            }
        }
    }

    @Test
    fun shadowDoesNotWrapAcrossRowsOrReenterFromOppositeEdge() {
        val size = 129
        val mask = ByteArray(size * size)
        for (y in 0 until size) for (x in 0..4) mask[y * size + x] = 255.toByte()
        val result = transparent(size, size).dropShadow(mask, 1f, 18f)
        assertTrue((result[0, 64] and 255) in 1..100)
        for (y in 0 until size) assertEquals(0, result[size - 1, y])
    }

    @Test
    fun hugeFiniteSigmaRetainsNormalizedTruncatedKernelWithoutOverflow() {
        for ((width, height) in listOf(1 to 1, 1 to 17, 17 to 1, 9 to 11)) {
            val mask = ByteArray(width * height) { 255.toByte() }
            val result = transparent(width, height).dropShadow(mask, 1f, Float.MAX_VALUE)
            val kernelWidth = (2 * max(width, height) + 1).toDouble()
            val expected = (255 * width * height / (kernelWidth * kernelWidth)).roundToInt()
            result.pixels.forEach { assertEquals(expected, it and 255) }
        }
    }

    private fun transparent(width: Int, height: Int) = PixelBuffer.solid(width, height, 0)

    private fun referenceGaussian(mask: ByteArray, width: Int, height: Int, sigma: Float): IntArray {
        val radius = min(ceil(3.0 * sigma).toInt(), max(width, height))
        val weights = DoubleArray(2 * radius + 1) { index ->
            val distance = (index - radius).toDouble()
            exp(-distance * distance / (2.0 * sigma * sigma))
        }
        val total = weights.sum()
        weights.indices.forEach { weights[it] /= total }
        val horizontal = DoubleArray(mask.size)
        val output = IntArray(mask.size)
        for (y in 0 until height) for (x in 0 until width) {
            for (sourceX in max(0, x - radius)..min(width - 1, x + radius)) {
                horizontal[y * width + x] += (mask[y * width + sourceX].toInt() and 255) *
                    weights[sourceX - x + radius]
            }
        }
        for (y in 0 until height) for (x in 0 until width) {
            var value = 0.0
            for (sourceY in max(0, y - radius)..min(height - 1, y + radius)) {
                value += horizontal[sourceY * width + x] * weights[sourceY - y + radius]
            }
            output[y * width + x] = value.roundToInt()
        }
        return output
    }
}

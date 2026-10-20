package com.gexetr.plinthproductphotos.domain.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasExpansionTest {
    @Test
    fun portraitSourceFitsSquareCanvasAndIsCentered() {
        val result = calculateCanvasExpansion(900, 1200, CanvasAspect.SQUARE)

        assertEquals(1200 to 1200, result.canvasSize)
        assertEquals(150 to 0, result.originalImageLocation)
    }

    @Test
    fun portraitSourceFitsSixteenByNineCanvasWithoutCropping() {
        val result = calculateCanvasExpansion(900, 1200, CanvasAspect.LANDSCAPE_16_9)

        assertEquals(2144 to 1206, result.canvasSize)
        assertEquals(622 to 3, result.originalImageLocation)
        assertSourceFits(900, 1200, result)
    }

    @Test
    fun portraitSourceFitsFourByFiveCanvasAndIsCentered() {
        val result = calculateCanvasExpansion(900, 1200, CanvasAspect.PORTRAIT_4_5)

        assertEquals(960 to 1200, result.canvasSize)
        assertEquals(30 to 0, result.originalImageLocation)
    }

    @Test
    fun wholePixelCanvasStaysBelowProviderPixelCeiling() {
        val result = calculateCanvasExpansion(1536, 1536, CanvasAspect.LANDSCAPE_16_9)

        assertTrue(result.canvasSize.first > 0)
        assertTrue(result.canvasSize.second > 0)
        assertTrue(result.canvasSize.first.toLong() * result.canvasSize.second < MAX_EXPAND_CANVAS_PIXELS)
        assertThrows(IllegalArgumentException::class.java) {
            calculateCanvasExpansion(5000, 5000, CanvasAspect.SQUARE)
        }
    }

    @Test
    fun canvasDimensionsAreWholePixelsAtTheRequestedAspect() {
        val (width, height) = calculateCanvasExpansion(
            901,
            1201,
            CanvasAspect.LANDSCAPE_16_9,
        ).canvasSize

        assertEquals(0, width % 16)
        assertEquals(0, height % 9)
        assertEquals(width / 16, height / 9)
    }

    @Test
    fun expansionNeverCropsTheSource() {
        CanvasAspect.values().forEach { aspect ->
            val result = calculateCanvasExpansion(901, 1201, aspect)
            assertSourceFits(901, 1201, result)
            assertEquals(
                (result.canvasSize.first - 901) / 2 to (result.canvasSize.second - 1201) / 2,
                result.originalImageLocation,
            )
        }
    }

    private fun assertSourceFits(width: Int, height: Int, result: CanvasExpansion) {
        val (canvasWidth, canvasHeight) = result.canvasSize
        val (left, top) = result.originalImageLocation
        assertTrue(left >= 0 && top >= 0)
        assertTrue(left.toLong() + width <= canvasWidth)
        assertTrue(top.toLong() + height <= canvasHeight)
    }
}

class OriginalPixelRestorationTest {
    @Test
    fun replacesCanvasRectangleWithExactSourcePixels() {
        val canvas = TestPixelRows(5, 4, IntArray(20) { it })
        val source = TestPixelRows(2, 2, intArrayOf(0x112233, 0x445566, 0x778899, 0xaabbcc))

        restoreOriginalPixels(canvas, source, 1 to 1)

        assertEquals(listOf(0x112233, 0x445566), canvas.readRow(1).slice(1..2))
        assertEquals(listOf(0x778899, 0xaabbcc), canvas.readRow(2).slice(1..2))
    }

    @Test
    fun failsWhenWrittenSourcePixelsCannotBeVerified() {
        val canvas = object : TestPixelRows(3, 2, IntArray(6)) {
            override fun writeRow(y: Int, pixels: IntArray) {
                super.writeRow(y, pixels)
                this.pixels[y * width] = -1
            }
        }
        val source = TestPixelRows(1, 1, intArrayOf(7))

        assertThrows(ProductPixelRestoreException::class.java) {
            restoreOriginalPixels(canvas, source, 0 to 0)
        }
    }

    @Test
    fun refusesPlacementThatWouldCropSource() {
        val canvas = TestPixelRows(2, 2, IntArray(4))
        val source = TestPixelRows(2, 2, IntArray(4))

        assertThrows(IllegalArgumentException::class.java) {
            restoreOriginalPixels(canvas, source, 1 to 0)
        }
    }
}

private open class TestPixelRows(
    override val width: Int,
    override val height: Int,
    protected val pixels: IntArray,
) : MutablePixelRows {
    override fun readRow(y: Int): IntArray =
        pixels.copyOfRange(y * width, (y + 1) * width)

    override fun writeRow(y: Int, pixels: IntArray) {
        pixels.copyInto(this.pixels, y * width)
    }
}

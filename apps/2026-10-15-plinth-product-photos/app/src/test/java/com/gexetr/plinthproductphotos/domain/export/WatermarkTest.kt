package com.gexetr.plinthproductphotos.domain.export

import androidx.compose.ui.graphics.toArgb
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.ui.theme.PlinthPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkTest {
    @Test
    fun applyChangesPixelsInsideItsBandOnly() {
        val original = PixelBuffer.solid(900, 1200, PixelBuffer.rgba(12, 34, 56))
        val marked = Watermark.apply(original)
        val band = Watermark.bandFor(original.width, original.height)

        var changedInside = false
        original.pixels.indices.forEach { index ->
            val x = index % original.width
            val y = index / original.width
            val changed = original.pixels[index] != marked.pixels[index]
            if (x in band.left until band.right && y in band.top until band.bottom) {
                changedInside = changedInside || changed
            } else {
                assertFalse("outside watermark band at ($x, $y)", changed)
            }
        }
        assertTrue("watermark must change at least one band pixel", changedInside)
    }

    @Test
    fun bandStaysWithinBoundsAndRejectsNonPositiveDimensions() {
        listOf(1 to 1, 5 to 7, 900 to 1200).forEach { (width, height) ->
            val band = Watermark.bandFor(width, height)
            assertTrue(band.left >= 0)
            assertTrue(band.top >= 0)
            assertTrue(band.right <= width)
            assertTrue(band.bottom <= height)
        }
        assertFails { Watermark.bandFor(0, 1) }
        assertFails { Watermark.bandFor(1, 0) }
    }

    @Test
    fun applyAndApplyArgbChangeTheSameCoordinates() {
        val width = 900
        val height = 1200
        val rgba = PixelBuffer.rgba(12, 34, 56)
        val marked = Watermark.apply(PixelBuffer.solid(width, height, rgba))
        val argb = IntArray(width * height) { PixelBuffer.fromArgb(rgba) }
        Watermark.applyArgb(argb, width, height)

        marked.pixels.indices.forEach { index ->
            assertEquals(
                "changed coordinate at $index",
                marked.pixels[index] != rgba,
                argb[index] != PixelBuffer.fromArgb(rgba),
            )
        }
    }

    @Test
    fun smallExportBandUsesOnlyPaletteColoursAndLeavesEverythingElseUntouched() {
        val original = PixelBuffer.solid(900, 1200, PixelBuffer.rgba(12, 34, 56))
        val marked = Watermark.apply(original)
        val band = Watermark.bandFor(original.width, original.height)
        val palette = setOf(
            PixelBuffer.fromArgb(PlinthPalette.Ink.toArgb()),
            PixelBuffer.fromArgb(PlinthPalette.Amber.toArgb()),
            PixelBuffer.fromArgb(PlinthPalette.Paper.toArgb()),
        )

        original.pixels.indices.forEach { index ->
            val x = index % original.width
            val y = index / original.width
            if (x in band.left until band.right && y in band.top until band.bottom) {
                assertTrue("band cell at ($x, $y) must be a watermark palette colour", marked.pixels[index] in palette)
            } else {
                assertEquals("outside watermark band at ($x, $y)", original.pixels[index], marked.pixels[index])
            }
        }
    }

    private fun assertFails(block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException")
    }
}

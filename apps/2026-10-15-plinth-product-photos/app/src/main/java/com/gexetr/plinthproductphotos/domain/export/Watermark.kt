package com.gexetr.plinthproductphotos.domain.export

import androidx.compose.ui.graphics.toArgb
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.ui.theme.PlinthPalette

/**
 * Free-tier wordmark baked into exported pixels: an opaque band flush with the bottom-right corner,
 * so it is legible on any photo and never reaches into the product's centred safe area.
 * Every pixel outside [bandFor] is left untouched.
 */
object Watermark {
    data class Band(val left: Int, val top: Int, val width: Int, val height: Int) {
        val right: Int get() = left + width
        val bottom: Int get() = top + height
    }

    private const val TEXT = "PLINTH"
    private const val GLYPH_WIDTH = 5
    private const val GLYPH_HEIGHT = 7
    private const val PAD_X = 2
    private const val PAD_Y = 2
    private const val STRIPE_ROWS = 1
    private const val TEXT_COLUMNS = TEXT.length * GLYPH_WIDTH + (TEXT.length - 1)
    private const val CELLS_WIDE = TEXT_COLUMNS + 2 * PAD_X
    private const val CELLS_HIGH = STRIPE_ROWS + GLYPH_HEIGHT + 2 * PAD_Y

    private val glyphs: Map<Char, List<String>> = mapOf(
        'P' to listOf("1111.", "1...1", "1...1", "1111.", "1....", "1....", "1...."),
        'L' to listOf("1....", "1....", "1....", "1....", "1....", "1....", "11111"),
        'I' to listOf("11111", "..1..", "..1..", "..1..", "..1..", "..1..", "11111"),
        'N' to listOf("1...1", "11..1", "1.1.1", "1.1.1", "1..11", "1...1", "1...1"),
        'T' to listOf("11111", "..1..", "..1..", "..1..", "..1..", "..1..", "..1.."),
        'H' to listOf("1...1", "1...1", "1...1", "11111", "1...1", "1...1", "1...1"),
    )

    private fun cell(width: Int, height: Int): Int =
        maxOf(1, minOf(minOf(width, height) / 132, width / CELLS_WIDE, height / (CELLS_HIGH * 4)))

    fun bandFor(width: Int, height: Int): Band {
        require(width > 0 && height > 0) { "Image dimensions must be positive" }
        val scale = cell(width, height)
        val bandWidth = minOf(width, CELLS_WIDE * scale)
        val bandHeight = minOf(height, CELLS_HIGH * scale)
        return Band(width - bandWidth, height - bandHeight, bandWidth, bandHeight)
    }

    /** Returns a new buffer; PixelBuffer stores 0xRRGGBBAA. */
    fun apply(image: PixelBuffer): PixelBuffer {
        val pixels = image.pixels.copyOf()
        paint(image.width, image.height) { x, y, argb ->
            pixels[y * image.width + x] = PixelBuffer.fromArgb(argb)
        }
        return PixelBuffer(image.width, image.height, pixels)
    }

    /** In place on Android 0xAARRGGBB pixels, row-major with stride [width]. */
    fun applyArgb(argb: IntArray, width: Int, height: Int) {
        require(argb.size == width * height) { "Pixel count must equal width * height" }
        paint(width, height) { x, y, color -> argb[y * width + x] = color }
    }

    private fun paint(width: Int, height: Int, put: (Int, Int, Int) -> Unit) {
        val band = bandFor(width, height)
        val scale = cell(width, height)
        val ink = PlinthPalette.Ink.toArgb()
        val stripe = PlinthPalette.Amber.toArgb()
        val paper = PlinthPalette.Paper.toArgb()
        for (y in band.top until band.bottom) {
            for (x in band.left until band.right) {
                val col = (x - band.left) / scale
                val row = (y - band.top) / scale
                val color = when {
                    row < STRIPE_ROWS -> stripe
                    isTextCell(col - PAD_X, row - STRIPE_ROWS - PAD_Y) -> paper
                    else -> ink
                }
                put(x, y, color)
            }
        }
    }

    private fun isTextCell(col: Int, row: Int): Boolean {
        if (col < 0 || row < 0 || row >= GLYPH_HEIGHT || col >= TEXT_COLUMNS) return false
        val slot = col / (GLYPH_WIDTH + 1)
        val inGlyph = col % (GLYPH_WIDTH + 1)
        if (inGlyph >= GLYPH_WIDTH) return false
        return glyphs.getValue(TEXT[slot])[row][inGlyph] == '1'
    }
}

package com.gexetr.plinthproductphotos.domain.studio

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioPngProvenanceTest {
    @Test
    fun studioResultPngMetadataPreservesDimensionsAndEveryCompositedPixel() {
        val composited = patternedImage()
        val compositedPng = encode(composited)
        val savedPng = addStudioPngProvenance(compositedPng)
        val saved = decode(savedPng)

        assertEquals(composited.width, saved.width)
        assertEquals(composited.height, saved.height)
        var comparedPixels = 0
        var comparedBottomPixels = 0
        for (y in 0 until composited.height) {
            val expectedRow = composited.getRGB(0, y, composited.width, 1, null, 0, composited.width)
            val savedRow = saved.getRGB(0, y, saved.width, 1, null, 0, saved.width)
            assertArrayEquals("pixels differ in row $y", expectedRow, savedRow)
            comparedPixels += expectedRow.size
            if (y >= composited.height - 200) comparedBottomPixels += expectedRow.size
        }
        assertEquals(1206, composited.height)
        assertEquals(2_585_664, comparedPixels)
        assertEquals(428_800, comparedBottomPixels)

        val sourceChunks = chunks(compositedPng)
        val savedChunks = chunks(savedPng)
        assertArrayEquals(
            compositedPng.copyOfRange(0, 8),
            savedPng.copyOfRange(0, 8),
        )
        val savedOriginalChunks = savedChunks.filterNot { it.type == "tEXt" }
        assertEquals(sourceChunks.map { it.type }, savedOriginalChunks.map { it.type })
        sourceChunks.zip(savedOriginalChunks).forEach { (source, savedChunk) ->
            assertArrayEquals("chunk ${source.type} must remain byte-identical", source.raw, savedChunk.raw)
        }
        val firstIdat = savedChunks.indexOfFirst { it.type == "IDAT" }
        val textChunk = savedChunks.indexOfFirst { it.type == "tEXt" }
        assertTrue("tEXt must precede the first IDAT", textChunk in 1 until firstIdat)
        assertEquals(
            listOf(
                "Software" to "com.gexetr.plinthproductphotos",
                "AI-Provenance" to "AI-generated enhanced result",
            ),
            savedChunks.filter { it.type == "tEXt" }.map { textEntry(it.data) },
        )
    }

    @Test
    fun studioResultPngPixelRegressionDetectsFormerDisclosureBand() {
        val composited = patternedImage()
        val banded = patternedImage()
        for (y in 1087 until 1206) {
            for (x in 0 until banded.width) banded.setRGB(x, y, 0xFF000000.toInt())
        }
        val savedWithBand = decode(addStudioPngProvenance(encode(banded)))

        val failure = assertThrows(AssertionError::class.java) {
            assertPixelRowsEqual(composited, savedWithBand)
        }
        val changedPixels = countChangedPixels(composited, savedWithBand)
        assertEquals(255_136, changedPixels)
        assertTrue(failure.message.orEmpty().contains("row 1087"))
    }

    private fun patternedImage(): BufferedImage =
        BufferedImage(2144, 1206, BufferedImage.TYPE_INT_ARGB).apply {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    setRGB(
                        x,
                        y,
                        (0xFF shl 24) or
                            ((x and 0xFF) shl 16) or
                            ((y and 0xFF) shl 8) or
                            ((x + y) and 0xFF),
                    )
                }
            }
        }

    private fun encode(image: BufferedImage): ByteArray =
        ByteArrayOutputStream().use { output ->
            assertTrue(ImageIO.write(image, "png", output))
            output.toByteArray()
        }

    private fun decode(png: ByteArray): BufferedImage =
        requireNotNull(ImageIO.read(ByteArrayInputStream(png)))

    private fun assertPixelRowsEqual(expected: BufferedImage, actual: BufferedImage) {
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        for (y in 0 until expected.height) {
            assertArrayEquals(
                "pixels differ in row $y",
                expected.getRGB(0, y, expected.width, 1, null, 0, expected.width),
                actual.getRGB(0, y, actual.width, 1, null, 0, actual.width),
            )
        }
    }

    private fun countChangedPixels(expected: BufferedImage, actual: BufferedImage): Int {
        var changed = 0
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                if (expected.getRGB(x, y) != actual.getRGB(x, y)) changed++
            }
        }
        return changed
    }

    private fun chunks(png: ByteArray): List<PngChunk> {
        val result = mutableListOf<PngChunk>()
        var offset = 8
        while (offset < png.size) {
            val length = readInt(png, offset)
            val type = png.copyOfRange(offset + 4, offset + 8).toString(Charsets.US_ASCII)
            val dataStart = offset + 8
            result += PngChunk(
                type = type,
                data = png.copyOfRange(dataStart, dataStart + length),
                raw = png.copyOfRange(offset, offset + length + 12),
            )
            offset += length + 12
        }
        return result
    }

    private fun textEntry(data: ByteArray): Pair<String, String> {
        val separator = data.indexOf(0)
        return data.copyOfRange(0, separator).toString(Charsets.ISO_8859_1) to
            data.copyOfRange(separator + 1, data.size).toString(Charsets.ISO_8859_1)
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

    private data class PngChunk(val type: String, val data: ByteArray, val raw: ByteArray)
}

package com.gexetr.plinthproductphotos.domain.cutout

import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EraseMaskTest {
    private val painted = byteArrayOf(
        0xff.toByte(), 0, 0,
        0, 0xff.toByte(), 0,
    )

    @Test
    fun paintedPixelsAreWhiteAndUntouchedAreBlack() {
        val buffer = EraseMask.buffer(painted, 3, 2, 3, 2)
        assertEquals(255, buffer[0].toInt() and 0xff)
        assertEquals(255, buffer[4].toInt() and 0xff)
        assertEquals(0, buffer[1].toInt() and 0xff)
        assertEquals(0, buffer[5].toInt() and 0xff)
    }

    @Test
    fun pngRoundTripKeepsPolarityAndDimensions() {
        val buffer = EraseMask.buffer(painted, 3, 2, 3, 2)
        val image = ImageIO.read(ByteArrayInputStream(EraseMask.encodePng(buffer, 3, 2)))
        assertEquals(3, image.width)
        assertEquals(2, image.height)
        assertEquals(1, image.raster.numBands)
        assertEquals(255, image.raster.getSample(0, 0, 0))
        assertEquals(0, image.raster.getSample(1, 0, 0))
        assertEquals(255, image.raster.getSample(1, 1, 0))
    }

    @Test
    fun maskIsResizedToTheUploadedPhoto() {
        val (w, h) = EraseMask.uploadSize(4000, 2000, 2048)
        assertEquals(2048 to 1024, w to h)
        val big = ByteArray(4 * 2) { if (it == 0) 0xff.toByte() else 0 }
        val buffer = EraseMask.buffer(big, 4, 2, 8, 4)
        assertEquals(8 * 4, buffer.size)
        assertEquals(255, buffer[0].toInt() and 0xff)
        assertEquals(255, buffer[1].toInt() and 0xff)
        assertEquals(0, buffer[2].toInt() and 0xff)
        assertEquals(2048 to 1024, EraseMask.uploadSize(2048, 1024, 2048))
        assertEquals(100 to 50, EraseMask.uploadSize(100, 50, 2048))
    }

    @Test
    fun emptyMaskIsRefused() {
        val empty = EraseMask.buffer(ByteArray(6), 3, 2, 3, 2)
        assertFailsWith<EmptyEraseMaskException> { EraseMask.requireNotEmpty(empty) }
        EraseMask.requireNotEmpty(EraseMask.buffer(painted, 3, 2, 3, 2))
    }

    private fun photo(vararg pixels: Int) = PixelBuffer(pixels.size, 1, pixels)

    @Test
    fun compositeRestoresSourceOutsideTheMask() {
        val source = photo(0x10203040, 0x50607080, 0x0a0b0c0d)
        val output = photo(0x10203040, 0x50607080 + 0x01010100, 0xffffffff.toInt())
        val mask = byteArrayOf(0, 0, 0xff.toByte())
        val result = EraseVerifier.compositeOutsideMask(source, output, mask)
        assertEquals(source.pixels[0], result.pixels[0])
        assertEquals(source.pixels[1], result.pixels[1])
        assertEquals(0xffffffff.toInt(), result.pixels[2])
    }

    @Test
    fun largeChangeOutsideTheMaskIsRejected() {
        val source = photo(0x10203040, 0x50607080, 0x0a0b0c0d)
        val output = photo(0xffffffff.toInt(), 0xffffffff.toInt(), 0xffffffff.toInt())
        val mask = byteArrayOf(0, 0, 0xff.toByte())
        val failure = assertFailsWith<EraseVerificationException> {
            EraseVerifier.compositeOutsideMask(source, output, mask)
        }
        assertEquals(EraseVerificationException.Reason.KEPT_PIXELS_CHANGED, failure.reason)
    }

    @Test
    fun sizeMismatchAndNoOpAreRejected() {
        val source = photo(1, 2, 3)
        val mask = byteArrayOf(0, 0, 0xff.toByte())
        assertEquals(
            EraseVerificationException.Reason.SIZE_MISMATCH,
            assertFailsWith<EraseVerificationException> {
                EraseVerifier.compositeOutsideMask(source, photo(1, 2), mask)
            }.reason,
        )
        assertEquals(
            EraseVerificationException.Reason.NOTHING_ERASED,
            assertFailsWith<EraseVerificationException> {
                EraseVerifier.compositeOutsideMask(source, photo(1, 2, 3), mask)
            }.reason,
        )
    }

    @Test
    fun keptPixelsDriftCountsOnlyOutsideTheMask() {
        val (drifted, kept) = EraseVerifier.keptPixelsDrifted(
            intArrayOf(0, 0, 0),
            intArrayOf(0xff, 0, 0xff),
            byteArrayOf(0, 0, 0xff.toByte()),
        )
        assertEquals(1, drifted)
        assertEquals(2, kept)
        assertTrue(EraseVerifier.MAX_DRIFT_FRACTION < 1.0)
    }

    private inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit): T {
        try {
            block()
        } catch (failure: Throwable) {
            if (failure is T) return failure
            throw failure
        }
        throw AssertionError("Expected ${T::class.java.simpleName}")
    }
}
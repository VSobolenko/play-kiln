package com.gexetr.plinthproductphotos.domain.cutout

import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater

class EmptyEraseMaskException : IllegalStateException("Nothing is painted to erase")

class EraseVerificationException(val reason: Reason) : IllegalStateException(reason.name) {
    enum class Reason { SIZE_MISMATCH, KEPT_PIXELS_CHANGED, NOTHING_ERASED }
}

/**
 * The mask sent with an erase request: 8-bit single-channel grayscale PNG, exactly the uploaded
 * photo's width x height.
 */
object EraseMask {
    /** Provider polarity: white (255) = erase, black (0) = keep. */
    const val ERASE: Byte = 0xff.toByte()
    const val KEEP: Byte = 0

    fun isEmpty(painted: ByteArray): Boolean = painted.none { it != KEEP }

    /** Size rule shared with the photo upload, so the mask and the uploaded photo always agree. */
    fun uploadSize(width: Int, height: Int, maxLongestSide: Int): Pair<Int, Int> {
        val scale = minOf(1f, maxLongestSide.toFloat() / maxOf(width, height))
        if (scale >= 1f) return width to height
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }

    /**
     * Builds the grayscale buffer. A painted pixel (any value >= 128) becomes 255 = ERASE and
     * everything else 0 = KEEP; never the reverse. Resizes nearest-neighbour when the
     * target differs from the painted size.
     */
    fun buffer(painted: ByteArray, width: Int, height: Int, targetWidth: Int, targetHeight: Int): ByteArray {
        require(painted.size == width * height) { "Painted mask must match its dimensions" }
        val out = ByteArray(targetWidth * targetHeight)
        for (y in 0 until targetHeight) {
            val sy = ((y + 0.5f) * height / targetHeight).toInt().coerceIn(0, height - 1)
            for (x in 0 until targetWidth) {
                val sx = ((x + 0.5f) * width / targetWidth).toInt().coerceIn(0, width - 1)
                out[y * targetWidth + x] =
                    if ((painted[sy * width + sx].toInt() and 0xff) >= 128) ERASE else KEEP
            }
        }
        return out
    }

    /** Refuses an empty mask before any credit is spent. */
    fun requireNotEmpty(buffer: ByteArray) {
        if (isEmpty(buffer)) throw EmptyEraseMaskException()
    }

    fun encodePng(gray: ByteArray, width: Int, height: Int): ByteArray {
        require(gray.size == width * height) { "Gray buffer must match its dimensions" }
        val raw = ByteArray((width + 1) * height)
        for (y in 0 until height) {
            System.arraycopy(gray, y * width, raw, y * (width + 1) + 1, width)
        }
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(raw)
        deflater.finish()
        val compressed = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (!deflater.finished()) compressed.write(chunk, 0, deflater.deflate(chunk))
        deflater.end()
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0d, 0x0a, 0x1a, 0x0a))
        // bit depth 8, colour type 0 (grayscale), deflate, no filter beyond per-row 0, no interlace
        val header = ByteBuffer.allocate(13).putInt(width).putInt(height)
            .put(8).put(0).put(0).put(0).put(0).array()
        writeChunk(out, "IHDR", header)
        writeChunk(out, "IDAT", compressed.toByteArray())
        writeChunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        out.write(ByteBuffer.allocate(4).putInt(data.size).array())
        val body = type.toByteArray(Charsets.US_ASCII) + data
        out.write(body)
        val crc = CRC32().apply { update(body) }
        out.write(ByteBuffer.allocate(4).putInt(crc.value.toInt()).array())
    }
}

object EraseVerifier {
    /** Per-channel difference tolerated as codec noise on kept pixels in the provider's raw output. */
    const val CHANNEL_TOLERANCE = 24

    /** Share of kept pixels allowed to exceed the tolerance before the output is rejected. */
    const val MAX_DRIFT_FRACTION = 0.02

    /** Returns (kept pixels beyond tolerance, kept pixels in total). */
    fun keptPixelsDrifted(source: IntArray, output: IntArray, mask: ByteArray): Pair<Int, Int> {
        var kept = 0
        var drifted = 0
        for (i in source.indices) {
            if (mask[i] == EraseMask.ERASE) continue
            kept++
            if (channelDelta(source[i], output[i]) > CHANNEL_TOLERANCE) drifted++
        }
        return drifted to kept
    }

    /**
     * Returns the source with the provider's pixels applied only where [mask] is ERASE, so every kept
     * pixel is the original byte for byte. Throws instead of returning a photo whose kept region changed.
     */
    fun compositeOutsideMask(source: PixelBuffer, output: PixelBuffer, mask: ByteArray): PixelBuffer {
        if (source.width != output.width || source.height != output.height ||
            mask.size != source.pixels.size
        ) {
            throw EraseVerificationException(EraseVerificationException.Reason.SIZE_MISMATCH)
        }
        val (drifted, kept) = keptPixelsDrifted(source.pixels, output.pixels, mask)
        if (kept > 0 && drifted.toDouble() / kept > MAX_DRIFT_FRACTION) {
            throw EraseVerificationException(EraseVerificationException.Reason.KEPT_PIXELS_CHANGED)
        }
        val result = IntArray(source.pixels.size) { i ->
            if (mask[i] == EraseMask.ERASE) output.pixels[i] else source.pixels[i]
        }
        var erasedChanged = false
        for (i in result.indices) {
            if (mask[i] == EraseMask.ERASE) {
                if (result[i] != source.pixels[i]) erasedChanged = true
            } else if (result[i] != source.pixels[i]) {
                throw EraseVerificationException(EraseVerificationException.Reason.KEPT_PIXELS_CHANGED)
            }
        }
        if (!erasedChanged) throw EraseVerificationException(EraseVerificationException.Reason.NOTHING_ERASED)
        return PixelBuffer(source.width, source.height, result)
    }

    private fun channelDelta(a: Int, b: Int): Int {
        var max = 0
        for (shift in intArrayOf(0, 8, 16, 24)) {
            val d = kotlin.math.abs(((a ushr shift) and 0xff) - ((b ushr shift) and 0xff))
            if (d > max) max = d
        }
        return max
    }
}

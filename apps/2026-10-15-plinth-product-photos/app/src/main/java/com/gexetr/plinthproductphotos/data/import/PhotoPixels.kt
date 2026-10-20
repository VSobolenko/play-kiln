package com.gexetr.plinthproductphotos.data.photoimport

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.io.File

object PhotoPixels {
    fun load(file: File): PixelBuffer {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
        }
        val bitmap = requireNotNull(BitmapFactory.decodeFile(file.path, options)) {
            "Selected image could not be decoded"
        }
        val buffer = try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for (index in pixels.indices) {
                val argb = pixels[index]
                pixels[index] = (argb shl 8) or (argb ushr 24)
            }
            PixelBuffer(bitmap.width, bitmap.height, pixels)
        } finally {
            bitmap.recycle()
        }
        return buffer.oriented(jpegOrientation(file))
    }

    fun PixelBuffer.toBitmap(): Bitmap {
        val argb = IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            val red = pixel ushr 24 and 0xff
            val green = pixel ushr 16 and 0xff
            val blue = pixel ushr 8 and 0xff
            val alpha = pixel and 0xff
            (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
        return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun PixelBuffer.oriented(orientation: Int): PixelBuffer {
        if (orientation !in 2..8) return this
        val swapped = orientation in 5..8
        val resultWidth = if (swapped) height else width
        val resultHeight = if (swapped) width else height
        val result = IntArray(resultWidth * resultHeight)
        for (y in 0 until resultHeight) {
            for (x in 0 until resultWidth) {
                val source = when (orientation) {
                    2 -> (y * width) + (width - x - 1)
                    3 -> ((height - y - 1) * width) + (width - x - 1)
                    4 -> ((height - y - 1) * width) + x
                    5 -> (x * width) + y
                    6 -> ((height - x - 1) * width) + y
                    7 -> ((height - x - 1) * width) + (width - y - 1)
                    8 -> (x * width) + (width - y - 1)
                    else -> y * width + x
                }
                result[y * resultWidth + x] = pixels[source]
            }
        }
        return PixelBuffer(resultWidth, resultHeight, result)
    }

    private fun jpegOrientation(file: File): Int {
        val bytes = ByteArray(262_144)
        val length = file.inputStream().use { input ->
            var read = 0
            while (read < bytes.size) {
                val count = input.read(bytes, read, bytes.size - read)
                if (count < 0) break
                read += count
            }
            read
        }
        if (length < 4 || u8(bytes, 0) != 0xff || u8(bytes, 1) != 0xd8) return 1
        var position = 2
        while (position + 4 < length && u8(bytes, position) == 0xff) {
            val marker = u8(bytes, position + 1)
            val segmentLength = u16be(bytes, position + 2)
            val start = position + 4
            if (marker == 0xe1 && start + 14 <= length &&
                bytes.copyOfRange(start, start + 6).contentEquals(byteArrayOf(69, 120, 105, 102, 0, 0))
            ) {
                return parseTiffOrientation(bytes, start + 6, length) ?: 1
            }
            if (segmentLength < 2) return 1
            position += 2 + segmentLength
            if (marker == 0xda || marker == 0xd9) return 1
        }
        return 1
    }

    private fun parseTiffOrientation(bytes: ByteArray, tiff: Int, limit: Int): Int? {
        if (tiff + 8 > limit) return null
        val littleEndian = u8(bytes, tiff) == 0x49 && u8(bytes, tiff + 1) == 0x49
        if (!littleEndian && !(u8(bytes, tiff) == 0x4d && u8(bytes, tiff + 1) == 0x4d)) return null
        fun read16(offset: Int): Int? {
            if (offset < tiff || offset + 2 > limit) return null
            val first = u8(bytes, offset)
            val second = u8(bytes, offset + 1)
            return if (littleEndian) first or (second shl 8) else (first shl 8) or second
        }
        val ifdOffset = read32(bytes, tiff + 4, littleEndian, limit) ?: return null
        val entriesAt = tiff + ifdOffset
        val count = read16(entriesAt) ?: return null
        for (entryIndex in 0 until count) {
            val entry = entriesAt + 2 + entryIndex * 12
            if (entry + 12 > limit) return null
            if (read16(entry) == 0x0112 && read16(entry + 2) == 3) {
                return read16(entry + 8)?.takeIf { it in 1..8 }
            }
        }
        return null
    }

    private fun read32(bytes: ByteArray, offset: Int, littleEndian: Boolean, limit: Int): Int? {
        if (offset < 0 || offset + 4 > limit) return null
        val a = u8(bytes, offset)
        val b = u8(bytes, offset + 1)
        val c = u8(bytes, offset + 2)
        val d = u8(bytes, offset + 3)
        return if (littleEndian) a or (b shl 8) or (c shl 16) or (d shl 24)
        else (a shl 24) or (b shl 16) or (c shl 8) or d
    }

    private fun u16be(bytes: ByteArray, offset: Int): Int = (u8(bytes, offset) shl 8) or u8(bytes, offset + 1)
    private fun u8(bytes: ByteArray, offset: Int): Int = bytes[offset].toInt() and 0xff
}

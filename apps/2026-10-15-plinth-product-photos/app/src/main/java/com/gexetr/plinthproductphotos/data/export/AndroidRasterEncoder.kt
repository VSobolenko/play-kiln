package com.gexetr.plinthproductphotos.data.export

import android.graphics.Bitmap
import com.gexetr.plinthproductphotos.domain.export.RasterEncoder
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.io.ByteArrayOutputStream
import java.io.IOException

/** PixelBuffer stores 0xRRGGBBAA; Android bitmaps take 0xAARRGGBB. */
internal fun rgbaToArgb(pixel: Int): Int = (pixel ushr 8) or (pixel shl 24)

class AndroidRasterEncoder : RasterEncoder {
    override fun encode(image: PixelBuffer, format: String, quality: Int): ByteArray {
        require(quality in 1..100)
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        try {
            val argb = IntArray(image.pixels.size) { rgbaToArgb(image.pixels[it]) }
            bitmap.setPixels(argb, 0, image.width, 0, 0, image.width, image.height)
            val outputFormat = when (format.lowercase()) {
                "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
                "png" -> Bitmap.CompressFormat.PNG
                "webp" -> Bitmap.CompressFormat.WEBP
                else -> throw IllegalArgumentException("Unsupported bitmap format: $format")
            }
            return ByteArrayOutputStream().use { output ->
                if (!bitmap.compress(outputFormat, quality, output)) {
                    throw IOException("Android bitmap encoder rejected $format output")
                }
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}

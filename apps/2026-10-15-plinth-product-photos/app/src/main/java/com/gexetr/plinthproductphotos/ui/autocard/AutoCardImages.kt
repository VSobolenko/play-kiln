package com.gexetr.plinthproductphotos.ui.autocard

import android.graphics.Bitmap
import com.gexetr.plinthproductphotos.domain.autocard.RecognitionImage
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.io.ByteArrayOutputStream

internal fun autoCardPreviewSize(width: Int, height: Int): RecognitionImage.Size =
    if (maxOf(width, height) <= 1024) {
        RecognitionImage.Size(width, height)
    } else {
        RecognitionImage.targetSize(width, height)
    }

internal fun PixelBuffer.copyAutoCardArgbRow(y: Int, row: IntArray) {
    require(row.size == width)
    val offset = y * width
    for (x in row.indices) {
        val pixel = pixels[offset + x]
        row[x] = (pixel ushr 8) or (pixel shl 24)
    }
}

private fun PixelBuffer.toAutoCardBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
        val row = IntArray(width)
        for (y in 0 until height) {
            copyAutoCardArgbRow(y, row)
            bitmap.setPixels(row, 0, width, 0, y, width, 1)
        }
        return bitmap
    } catch (failure: Throwable) {
        bitmap.recycle()
        throw failure
    }
}

internal suspend fun PixelBuffer.autoCardPreviewBitmap(): Bitmap = AutoCardImageWork.run {
    val target = autoCardPreviewSize(width, height)
    val scaled = if (width == target.width && height == target.height) this
    else scale(target.width, target.height)
    scaled.toAutoCardBitmap()
}

internal suspend fun PixelBuffer.toRecognitionJpeg(): ByteArray = AutoCardImageWork.run {
    val target = RecognitionImage.targetSize(width, height)
    val scaled = if (width == target.width && height == target.height) this
    else scale(target.width, target.height)
    val bitmap = scaled.toAutoCardBitmap()
    try {
        val output = ByteArrayOutputStream()
        for (quality in 92 downTo 62 step 5) {
            output.reset()
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                "Could not encode the recognition photo"
            }
            if (output.size() <= 4 * 1024 * 1024) return@run output.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
    throw IllegalArgumentException("Recognition photo is above the upload size limit")
}

package com.gexetr.plinthproductphotos.domain.lint

import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.pipeline.PixelBounds
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import kotlin.math.pow
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PixelFacts(
    val itemFillRatio: Double,
    val backgroundMeanChroma: Double?,
    val borderMinimumChannelOutsideItem: Int?,
)

object PixelFactsMeasurement {
    fun fitMask(mask: Mask, sourceWidth: Int, sourceHeight: Int, width: Int, height: Int): Mask {
        require(mask.alpha.size.toLong() == sourceWidth.toLong() * sourceHeight)
        require(sourceWidth > 0 && sourceHeight > 0 && width > 0 && height > 0)
        require(width.toLong() * height <= Int.MAX_VALUE) { "Fitted mask is too large" }
        if (sourceWidth == width && sourceHeight == height) return Mask(mask.alpha.copyOf())
        val scale = min(width.toDouble() / sourceWidth, height.toDouble() / sourceHeight)
        val scaledWidth = (sourceWidth * scale).roundToInt().coerceIn(1, width)
        val scaledHeight = (sourceHeight * scale).roundToInt().coerceIn(1, height)
        val maskImage = PixelBuffer(sourceWidth, sourceHeight, IntArray(mask.alpha.size) {
            val alpha = mask.alpha[it].toInt() and 0xff
            PixelBuffer.rgba(alpha, alpha, alpha)
        }).scale(scaledWidth, scaledHeight)
        val left = (width - scaledWidth) / 2
        val top = (height - scaledHeight) / 2
        val fitted = ByteArray(width * height)
        for (y in 0 until scaledHeight) {
            for (x in 0 until scaledWidth) {
                fitted[(top + y) * width + left + x] = (maskImage[x, y] ushr 24).toByte()
            }
        }
        return Mask(fitted)
    }

    // Match Compositor's >8 crop threshold and nearest-neighbour mask placement.
    fun alignMask(
        sourceWidth: Int,
        sourceHeight: Int,
        mask: Mask,
        image: PixelBuffer,
        placement: PixelBounds,
    ): Mask {
        require(sourceWidth > 0 && sourceHeight > 0)
        require(mask.alpha.size.toLong() == sourceWidth.toLong() * sourceHeight)
        require(placement.left >= 0 && placement.top >= 0 && placement.width > 0 && placement.height > 0)
        require(placement.left.toLong() + placement.width <= image.width &&
            placement.top.toLong() + placement.height <= image.height)
        var left = sourceWidth
        var top = sourceHeight
        var right = -1
        var bottom = -1
        for (index in mask.alpha.indices) {
            if ((mask.alpha[index].toInt() and 0xff) <= 8) continue
            val x = index % sourceWidth
            val y = index / sourceWidth
            left = minOf(left, x)
            top = minOf(top, y)
            right = maxOf(right, x)
            bottom = maxOf(bottom, y)
        }
        require(right >= left && bottom >= top) { "No item pixels found in mask" }
        val cropWidth = right - left + 1
        val cropHeight = bottom - top + 1
        val aligned = ByteArray(image.pixels.size)
        for (y in 0 until placement.height) {
            val sourceY = ((y + 0.5) * cropHeight / placement.height).toInt().coerceIn(0, cropHeight - 1)
            for (x in 0 until placement.width) {
                val sourceX = ((x + 0.5) * cropWidth / placement.width).toInt().coerceIn(0, cropWidth - 1)
                aligned[(placement.top + y) * image.width + placement.left + x] =
                    mask.alpha[(top + sourceY) * sourceWidth + left + sourceX]
            }
        }
        return Mask(aligned)
    }

    fun measure(image: PixelBuffer, alignedMask: Mask): PixelFacts {
        require(alignedMask.alpha.size == image.pixels.size) { "Mask must be aligned with the image" }
        var coverage = 0L
        var backgroundCount = 0
        var chromaSum = 0.0
        var borderMinimum: Int? = null
        for (index in image.pixels.indices) {
            val alpha = alignedMask.alpha[index].toInt() and 0xff
            coverage += alpha
            // A feathered edge contains item pixels, so it is not a background sample.
            if (alpha != 0) continue
            val pixel = image.pixels[index]
            chromaSum += labChroma(pixel)
            backgroundCount++
            val x = index % image.width
            val y = index / image.width
            if (x == 0 || y == 0 || x == image.width - 1 || y == image.height - 1) {
                val minimum = minOf(pixel ushr 24 and 0xff, pixel ushr 16 and 0xff, pixel ushr 8 and 0xff)
                borderMinimum = minOf(borderMinimum ?: 255, minimum)
            }
        }
        return PixelFacts(
            // Alpha-weighted frame coverage, not the item's bounding-box area.
            itemFillRatio = coverage.toDouble() / (255.0 * image.pixels.size),
            backgroundMeanChroma = if (backgroundCount == 0) null else chromaSum / backgroundCount,
            borderMinimumChannelOutsideItem = borderMinimum,
        )
    }

    // sRGB -> CIE Lab, D65 reference white, as used by FidelityGuard.
    private fun labChroma(pixel: Int): Double {
        fun linear(shift: Int): Double {
            val channel = (pixel ushr shift and 0xff) / 255.0
            return if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
        }
        fun pivot(value: Double): Double = if (value > 216.0 / 24389.0) {
            value.pow(1.0 / 3.0)
        } else {
            (24389.0 / 27.0 * value + 16.0) / 116.0
        }
        val red = linear(24)
        val green = linear(16)
        val blue = linear(8)
        val x = pivot((0.4124564 * red + 0.3575761 * green + 0.1804375 * blue) / 0.95047)
        val y = pivot(0.2126729 * red + 0.7151522 * green + 0.0721750 * blue)
        val z = pivot((0.0193339 * red + 0.1191920 * green + 0.9503041 * blue) / 1.08883)
        val a = 500.0 * (x - y)
        val b = 200.0 * (y - z)
        return sqrt(a * a + b * b)
    }
}

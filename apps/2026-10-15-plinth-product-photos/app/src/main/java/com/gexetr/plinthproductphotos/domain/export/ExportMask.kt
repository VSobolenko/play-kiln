package com.gexetr.plinthproductphotos.domain.export

import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import kotlin.math.min
import kotlin.math.roundToInt

// Keep the image resampler's filters/rounding without expanding a byte mask to RGBA.
internal fun fitExportMask(
    mask: Mask,
    sourceWidth: Int,
    sourceHeight: Int,
    width: Int,
    height: Int,
): Mask {
    require(sourceWidth > 0 && sourceHeight > 0 && width > 0 && height > 0)
    require(mask.alpha.size.toLong() == sourceWidth.toLong() * sourceHeight)
    require(width.toLong() * height <= Int.MAX_VALUE) { "Fitted mask is too large" }
    if (sourceWidth == width && sourceHeight == height) return mask
    val scale = min(width.toDouble() / sourceWidth, height.toDouble() / sourceHeight)
    val scaledWidth = (sourceWidth * scale).roundToInt().coerceIn(1, width)
    val scaledHeight = (sourceHeight * scale).roundToInt().coerceIn(1, height)
    val scaled = PixelBuffer.scaleMask(mask.alpha, sourceWidth, sourceHeight, scaledWidth, scaledHeight)
    if (scaledWidth == width && scaledHeight == height) return Mask(scaled)
    val left = (width - scaledWidth) / 2
    val top = (height - scaledHeight) / 2
    val fitted = ByteArray(width * height)
    for (y in 0 until scaledHeight) {
        scaled.copyInto(fitted, (top + y) * width + left, y * scaledWidth, (y + 1) * scaledWidth)
    }
    return Mask(fitted)
}

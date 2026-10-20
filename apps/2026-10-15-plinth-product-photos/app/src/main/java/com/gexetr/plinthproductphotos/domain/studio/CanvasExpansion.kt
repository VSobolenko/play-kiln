package com.gexetr.plinthproductphotos.domain.studio

import kotlin.math.max

internal const val MAX_EXPAND_CANVAS_PIXELS = 25_000_000L
private const val MAX_UPLOAD_EDGE = 1536

enum class CanvasAspect(
    val widthUnits: Int,
    val heightUnits: Int,
    val label: String,
) {
    SQUARE(1, 1, "1:1"),
    PORTRAIT_4_5(4, 5, "4:5"),
    LANDSCAPE_16_9(16, 9, "16:9"),
}

data class CanvasExpansion(
    val canvasSize: Pair<Int, Int>,
    val originalImageLocation: Pair<Int, Int>,
)

internal fun calculateCanvasExpansion(
    sourceWidth: Int,
    sourceHeight: Int,
    aspect: CanvasAspect,
): CanvasExpansion {
    require(sourceWidth > 0 && sourceHeight > 0) { "Source dimensions must be positive" }

    val units = max(
        ceilDivide(sourceWidth, aspect.widthUnits),
        ceilDivide(sourceHeight, aspect.heightUnits),
    )
    val canvasWidth = units.toLong() * aspect.widthUnits
    val canvasHeight = units.toLong() * aspect.heightUnits
    require(canvasWidth <= Int.MAX_VALUE && canvasHeight <= Int.MAX_VALUE) {
        "Expanded canvas dimensions are too large"
    }
    require(canvasWidth * canvasHeight < MAX_EXPAND_CANVAS_PIXELS) {
        "Expanded canvas exceeds the provider pixel limit"
    }

    val width = canvasWidth.toInt()
    val height = canvasHeight.toInt()
    val location = (width - sourceWidth) / 2 to (height - sourceHeight) / 2
    check(location.first >= 0 && location.second >= 0)
    check(location.first.toLong() + sourceWidth <= width)
    check(location.second.toLong() + sourceHeight <= height)
    return CanvasExpansion(width to height, location)
}

internal fun uploadImageDimensions(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
    require(sourceWidth > 0 && sourceHeight > 0) { "Source dimensions must be positive" }
    val scale = minOf(1f, MAX_UPLOAD_EDGE.toFloat() / max(sourceWidth, sourceHeight))
    return (sourceWidth * scale).toInt().coerceAtLeast(1) to
        (sourceHeight * scale).toInt().coerceAtLeast(1)
}

private fun ceilDivide(value: Int, divisor: Int): Int =
    ((value.toLong() + divisor - 1) / divisor).toInt()

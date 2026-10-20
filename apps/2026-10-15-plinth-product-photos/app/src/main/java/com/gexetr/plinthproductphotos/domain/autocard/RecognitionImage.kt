package com.gexetr.plinthproductphotos.domain.autocard

/**
 * The server requires an exact 1024 px longest side, including for smaller sources.
 * Truncating a float scale product produced 1023 for a 2344 px source; pin the long
 * axis and round only the short axis using integer arithmetic.
 */
object RecognitionImage {
    data class Size(val width: Int, val height: Int)

    fun targetSize(width: Int, height: Int): Size {
        require(width > 0 && height > 0) { "Recognition image dimensions must be positive" }
        val longestSide = maxOf(width, height)
        val shortestSide = minOf(width, height)
        val targetShortSide = (
            (shortestSide.toLong() * 1024 + longestSide / 2) / longestSide
            ).toInt().coerceAtLeast(1)
        return if (width >= height) Size(1024, targetShortSide) else Size(targetShortSide, 1024)
    }
}

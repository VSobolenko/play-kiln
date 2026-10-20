package com.gexetr.plinthproductphotos.domain.studio

interface PixelRows {
    val width: Int
    val height: Int

    fun readRow(y: Int): IntArray
}

interface MutablePixelRows : PixelRows {
    fun writeRow(y: Int, pixels: IntArray)
}

class ProductPixelRestoreException(message: String) : IllegalStateException(message)

internal fun restoreOriginalPixels(
    canvas: MutablePixelRows,
    source: PixelRows,
    originalImageLocation: Pair<Int, Int>,
) {
    val (left, top) = originalImageLocation
    require(canvas.width > 0 && canvas.height > 0 && source.width > 0 && source.height > 0)
    require(left >= 0 && top >= 0)
    require(left.toLong() + source.width <= canvas.width)
    require(top.toLong() + source.height <= canvas.height)

    for (sourceY in 0 until source.height) {
        val originalRow = source.readRow(sourceY)
        val canvasY = top + sourceY
        val canvasRow = canvas.readRow(canvasY)
        if (originalRow.size != source.width || canvasRow.size != canvas.width) {
            throw ProductPixelRestoreException("Pixel rows do not match their declared dimensions")
        }
        originalRow.copyInto(canvasRow, destinationOffset = left)
        canvas.writeRow(canvasY, canvasRow)

        val verifiedRow = canvas.readRow(canvasY)
        if (verifiedRow.size != canvas.width ||
            !originalRow.contentEquals(verifiedRow.copyOfRange(left, left + source.width))
        ) {
            throw ProductPixelRestoreException("Original product pixels could not be verified")
        }
    }
}

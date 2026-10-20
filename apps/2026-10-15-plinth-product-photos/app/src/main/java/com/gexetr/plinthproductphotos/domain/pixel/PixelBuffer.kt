package com.gexetr.plinthproductphotos.domain.pixel

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class PixelBuffer(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    init {
        require(width > 0 && height > 0) { "Pixel dimensions must be positive" }
        require(width.toLong() * height <= Int.MAX_VALUE) { "Pixel buffer is too large" }
        require(pixels.size == width * height) { "Pixel count must equal width * height" }
    }

    operator fun get(x: Int, y: Int): Int {
        require(x in 0 until width && y in 0 until height) { "Pixel coordinates are outside the buffer" }
        return pixels[y * width + x]
    }

    fun crop(left: Int, top: Int, width: Int, height: Int): PixelBuffer {
        require(left >= 0 && top >= 0 && width > 0 && height > 0) { "Crop must be non-empty and non-negative" }
        require(left.toLong() + width <= this.width && top.toLong() + height <= this.height) {
            "Crop is outside the buffer"
        }
        val cropped = IntArray(width * height)
        for (y in 0 until height) {
            pixels.copyInto(
                destination = cropped,
                destinationOffset = y * width,
                startIndex = (top + y) * this.width + left,
                endIndex = (top + y) * this.width + left + width,
            )
        }
        return PixelBuffer(width, height, cropped)
    }

    /** Uses Lanczos3 for reductions and Mitchell-Netravali (B=C=1/3) for enlargements. */
    fun scale(width: Int, height: Int): PixelBuffer =
        scale(width, height, ForkJoinPool.commonPool())

    internal fun scale(width: Int, height: Int, executor: Executor): PixelBuffer {
        require(width > 0 && height > 0) { "Scaled dimensions must be positive" }
        require(width.toLong() * height <= Int.MAX_VALUE) { "Scaled buffer is too large" }
        if (width == this.width && height == this.height) return PixelBuffer(width, height, pixels.copyOf())
        if (pixels.all { (it and 0xff) == 255 }) {
            val horizontal = resampleOpaqueAxis(
                pixels,
                this.width,
                this.height,
                width,
                horizontal = true,
                executor = executor,
            )
            val vertical = resampleOpaqueAxis(
                horizontal,
                width,
                this.height,
                height,
                horizontal = false,
                executor = executor,
            )
            return PixelBuffer(width, height, vertical)
        }
        val horizontal = resampleAxis(
            pixels,
            this.width,
            this.height,
            width,
            horizontal = true,
            executor = executor,
        )
        val vertical = resampleAxis(
            horizontal.pixels,
            horizontal.width,
            this.height,
            height,
            horizontal = false,
            executor = executor,
        )
        return PixelBuffer(width, height, vertical.pixels)
    }

    fun compositeOver(
        background: PixelBuffer,
        left: Int,
        top: Int,
        maskAlpha: ByteArray? = null,
    ): PixelBuffer {
        require(maskAlpha == null || maskAlpha.size == pixels.size) { "Mask must match the foreground dimensions" }
        require(left >= 0 && top >= 0 && left.toLong() + width <= background.width &&
            top.toLong() + height <= background.height
        ) { "Foreground placement is outside the background" }
        val output = background.pixels.copyOf()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val sourceIndex = y * width + x
                val destinationIndex = (top + y) * background.width + left + x
                val foreground = pixels[sourceIndex]
                val mask = maskAlpha?.get(sourceIndex)?.toInt()?.and(0xff) ?: 255
                val alpha = ((alpha(foreground) * mask + 127) / 255)
                output[destinationIndex] = blend(foreground, output[destinationIndex], alpha)
            }
        }
        return PixelBuffer(background.width, background.height, output)
    }

    fun dropShadow(
        maskAlpha: ByteArray,
        opacity: Float,
        blurRadius: Float,
        offsetX: Int = 0,
        offsetY: Int = 0,
        verticalScale: Float = 1f,
    ): PixelBuffer {
        require(maskAlpha.size == pixels.size) { "Shadow mask must match the background dimensions" }
        require(opacity in 0f..1f && opacity.isFinite()) { "Shadow opacity must be between 0 and 1" }
        require(blurRadius >= 0f && blurRadius.isFinite()) { "Blur radius must be finite and non-negative" }
        require(verticalScale > 0f && verticalScale.isFinite()) { "Vertical scale must be finite and positive" }
        var lastMaskRow = -1
        for (index in maskAlpha.indices) {
            if ((maskAlpha[index].toInt() and 0xff) > 0) lastMaskRow = max(lastMaskRow, index / width)
        }
        if (lastMaskRow < 0) return PixelBuffer(width, height, pixels.copyOf())
        val shadowAlpha = FloatArray(pixels.size)
        for (index in maskAlpha.indices) {
            val sourceAlpha = (maskAlpha[index].toInt() and 0xff) / 255f
            if (sourceAlpha == 0f) continue
            val sourceX = index % width
            val sourceY = index / width
            val destinationX = sourceX + offsetX
            val destinationY = lastMaskRow - ((lastMaskRow - sourceY) * verticalScale).roundToInt() + offsetY
            if (destinationX in 0 until width && destinationY in 0 until height) {
                val destination = destinationY * width + destinationX
                shadowAlpha[destination] = max(shadowAlpha[destination], sourceAlpha * opacity)
            }
        }
        val blurred = blur(shadowAlpha, blurRadius)
        val shadowPixels = IntArray(pixels.size) { index ->
            rgba(0, 0, 0, (blurred[index] * 255f).roundToInt().coerceIn(0, 255))
        }
        return PixelBuffer(width, height, shadowPixels).compositeOver(this, 0, 0)
    }

    private fun blur(input: FloatArray, sigma: Float): FloatArray {
        if (sigma == 0f) return input
        val radius = min(ceil(sigma * 3.0).toInt(), max(width, height))
        if (radius == 0) return input
        // Keep small/thin shadows exact, including the original canvas-capped Gaussian
        // normalization when sigma is so large that its support exceeds the canvas.
        if (sigma >= 10f && radius < max(width, height) && min(width, height) > 2) {
            return boxGaussian(input, sigma)
        }
        val kernel = FloatArray(radius * 2 + 1)
        var total = 0f
        for (offset in -radius..radius) {
            val weight = exp(-offset.toDouble() * offset / (2.0 * sigma * sigma)).toFloat()
            kernel[offset + radius] = weight
            total += weight
        }
        for (index in kernel.indices) kernel[index] /= total
        val horizontal = FloatArray(input.size)
        val output = FloatArray(input.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var value = 0f
                for (sourceX in max(0, x - radius)..min(width - 1, x + radius)) {
                    value += input[y * width + sourceX] * kernel[sourceX - x + radius]
                }
                horizontal[y * width + x] = value
            }
        }
        for (y in 0 until height) {
            for (x in 0 until width) {
                var value = 0f
                for (sourceY in max(0, y - radius)..min(height - 1, y + radius)) {
                    value += horizontal[sourceY * width + x] * kernel[sourceY - y + radius]
                }
                output[y * width + x] = value
            }
        }
        return output
    }

    private fun boxGaussian(input: FloatArray, sigma: Float): FloatArray {
        // Six variance-matched sliding boxes retain a closer Gaussian profile than three,
        // while keeping the work per pixel independent of sigma.
        val passes = 6
        val idealWidth = sqrt(12.0 * sigma * sigma / passes + 1.0)
        val lowerWidth = idealWidth.toInt().let { if (it % 2 == 0) it - 1 else it }
        val upperWidth = lowerWidth + 2
        val lowerCount = (
            (12.0 * sigma * sigma - passes.toDouble() * lowerWidth * lowerWidth -
                4.0 * passes * lowerWidth - 3.0 * passes) /
                (-4.0 * lowerWidth - 4.0)
            ).roundToInt().coerceIn(0, passes)
        val radii = IntArray(passes) { (if (it < lowerCount) lowerWidth else upperWidth) / 2 }
        val horizontal = FloatArray(input.size)
        boxGaussianAxis(input, horizontal, radii, horizontal = true)
        boxGaussianAxis(horizontal, input, radii, horizontal = false)
        return input
    }

    private fun boxGaussianAxis(
        input: FloatArray,
        output: FloatArray,
        radii: IntArray,
        horizontal: Boolean,
    ) {
        val length = if (horizontal) width else height
        val lines = if (horizontal) height else width
        val stride = if (horizontal) 1 else width
        // Keep the full halo between passes: clipping intermediate boxes at the canvas would
        // discard shadow energy that should return on later passes, darkening the edge profile.
        val halo = radii.sum()
        var source = FloatArray(length + 2 * halo)
        var target = FloatArray(source.size)
        for (line in 0 until lines) {
            source.fill(0f)
            val start = if (horizontal) line * width else line
            for (position in 0 until length) source[halo + position] = input[start + position * stride]
            for (radius in radii) {
                var sum = 0.0
                for (position in 0 until radius) sum += source[position]
                val divisor = (2 * radius + 1).toDouble()
                for (position in source.indices) {
                    val entering = position + radius
                    val leaving = position - radius - 1
                    if (entering < source.size) sum += source[entering]
                    if (leaving >= 0) sum -= source[leaving]
                    target[position] = (sum / divisor).toFloat()
                }
                val swap = source
                source = target
                target = swap
            }
            for (position in 0 until length) output[start + position * stride] = source[halo + position]
        }
    }

    companion object {
        fun rgba(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
            require(red in 0..255 && green in 0..255 && blue in 0..255 && alpha in 0..255)
            return (red shl 24) or (green shl 16) or (blue shl 8) or alpha
        }

        fun fromArgb(color: Int): Int = rgba(
            red = color ushr 16 and 0xff,
            green = color ushr 8 and 0xff,
            blue = color and 0xff,
            alpha = color ushr 24 and 0xff,
        )

        fun solid(width: Int, height: Int, color: Int): PixelBuffer {
            require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE)
            return PixelBuffer(width, height, IntArray(width * height) { color })
        }

        fun gradient(width: Int, height: Int, startArgb: Int, endArgb: Int, angleDeg: Int): PixelBuffer {
            require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE)
            val start = fromArgb(startArgb)
            val end = fromArgb(endArgb)
            val angle = Math.toRadians(angleDeg.toDouble())
            val directionX = cos(angle)
            val directionY = sin(angle)
            val corners = listOf(
                0.0,
                (width - 1) * directionX,
                (height - 1) * directionY,
                (width - 1) * directionX + (height - 1) * directionY,
            )
            val minimum = corners.minOrNull() ?: 0.0
            val range = (corners.maxOrNull() ?: 0.0) - minimum
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val projection = x * directionX + y * directionY
                    val fraction = if (range == 0.0) 0.0 else ((projection - minimum) / range).coerceIn(0.0, 1.0)
                    pixels[y * width + x] = interpolate(start, end, fraction.toFloat())
                }
            }
            return PixelBuffer(width, height, pixels)
        }

        private data class ResampledAxis(val width: Int, val pixels: IntArray)

        private fun resampleAxis(
            source: IntArray,
            sourceWidth: Int,
            sourceHeight: Int,
            targetSize: Int,
            horizontal: Boolean,
            executor: Executor,
        ): ResampledAxis {
            val sourceSize = if (horizontal) sourceWidth else sourceHeight
            val targetWidth = if (horizontal) targetSize else sourceWidth
            val targetHeight = if (horizontal) sourceHeight else targetSize
            if (targetSize == sourceSize) return ResampledAxis(sourceWidth, source)
            val taps = filterTaps(sourceSize, targetSize)
            val output = IntArray(targetWidth * targetHeight)
            parallelRows(targetHeight, targetWidth, executor) { y ->
                for (x in 0 until targetWidth) {
                    val filter = taps[if (horizontal) x else y]
                    var alphaSum = 0.0
                    var redSum = 0.0
                    var greenSum = 0.0
                    var blueSum = 0.0
                    for (index in filter.samples.indices) {
                        val weight = filter.weights[index]
                        if (weight == 0.0) continue
                        val boundedSample = filter.samples[index]
                        val sourceX = if (horizontal) boundedSample else x
                        val sourceY = if (horizontal) y else boundedSample
                        val pixel = source[sourceY * sourceWidth + sourceX]
                        val alpha = (pixel and 0xff).toDouble()
                        alphaSum += weight * alpha
                        redSum += weight * (pixel ushr 24 and 0xff) * alpha
                        greenSum += weight * (pixel ushr 16 and 0xff) * alpha
                        blueSum += weight * (pixel ushr 8 and 0xff) * alpha
                    }
                    val outputIndex = y * targetWidth + x
                    if (filter.weightSum == 0.0) {
                        val sourceX = if (horizontal) filter.nearest else x
                        val sourceY = if (horizontal) y else filter.nearest
                        output[outputIndex] = source[sourceY * sourceWidth + sourceX]
                    } else {
                        val alpha = (alphaSum / filter.weightSum).coerceIn(0.0, 255.0)
                        output[outputIndex] = rgba(
                            unpremultiply(redSum, alphaSum),
                            unpremultiply(greenSum, alphaSum),
                            unpremultiply(blueSum, alphaSum),
                            alpha.roundToInt(),
                        )
                    }
                }
            }
            return ResampledAxis(targetWidth, output)
        }

        private fun filterTaps(sourceSize: Int, targetSize: Int): Array<FilterTaps> {
            val scale = targetSize.toDouble() / sourceSize
            val filterScale = min(1.0, scale)
            val downscale = scale < 1.0
            val support = (if (downscale) 3.0 else 2.0) / filterScale
            // Filter taps depend on the axis coordinate, not on the row/column.
            return Array(targetSize) { destinationPosition ->
                val position = (destinationPosition + 0.5) / scale - 0.5
                val first = kotlin.math.floor(position - support).toInt()
                val last = ceil(position + support).toInt()
                val samples = IntArray(last - first + 1)
                val weights = DoubleArray(samples.size)
                var weightSum = 0.0
                for (index in samples.indices) {
                    val sample = first + index
                    val distance = (position - sample) * filterScale
                    val weight = if (downscale) lanczos3(distance) else mitchell(distance)
                    samples[index] = sample.coerceIn(0, sourceSize - 1)
                    weights[index] = weight
                    weightSum += weight
                }
                FilterTaps(
                    samples, weights, weightSum,
                    position.roundToInt().coerceIn(0, sourceSize - 1),
                )
            }
        }

        private fun resampleOpaqueAxis(
            source: IntArray,
            sourceWidth: Int,
            sourceHeight: Int,
            targetSize: Int,
            horizontal: Boolean,
            executor: Executor,
        ): IntArray {
            val sourceSize = if (horizontal) sourceWidth else sourceHeight
            if (sourceSize == targetSize) return source
            val targetWidth = if (horizontal) targetSize else sourceWidth
            val targetHeight = if (horizontal) sourceHeight else targetSize
            val taps = filterTaps(sourceSize, targetSize)
            val output = IntArray(targetWidth * targetHeight)
            parallelRows(targetHeight, targetWidth, executor) { y ->
                for (x in 0 until targetWidth) {
                    val filter = taps[if (horizontal) x else y]
                    var red = 0.0
                    var green = 0.0
                    var blue = 0.0
                    for (index in filter.samples.indices) {
                        val weight = filter.normalizedWeights[index]
                        if (weight == 0.0) continue
                        val sourceX = if (horizontal) filter.samples[index] else x
                        val sourceY = if (horizontal) y else filter.samples[index]
                        val pixel = source[sourceY * sourceWidth + sourceX]
                        red += weight * (pixel ushr 24 and 0xff)
                        green += weight * (pixel ushr 16 and 0xff)
                        blue += weight * (pixel ushr 8 and 0xff)
                    }
                    val index = y * targetWidth + x
                    output[index] = if (filter.weightSum == 0.0) {
                        val sourceX = if (horizontal) filter.nearest else x
                        val sourceY = if (horizontal) y else filter.nearest
                        source[sourceY * sourceWidth + sourceX]
                    } else {
                        (red.roundToInt().coerceIn(0, 255) shl 24) or
                            (green.roundToInt().coerceIn(0, 255) shl 16) or
                            (blue.roundToInt().coerceIn(0, 255) shl 8) or 255
                    }
                }
            }
            return output
        }

        private fun parallelRows(rowCount: Int, rowWidth: Int, executor: Executor, block: (Int) -> Unit) {
            if (rowCount.toLong() * rowWidth < PARALLEL_RESAMPLE_PIXEL_THRESHOLD || rowCount < 2) {
                for (row in 0 until rowCount) block(row)
                return
            }
            val workerCount = minOf(
                rowCount,
                maxOf(2, Runtime.getRuntime().availableProcessors() - 1),
            )
            val rowsPerWorker = (rowCount - 1) / workerCount + 1
            val tasks = (0 until workerCount).mapNotNull { worker ->
                val start = worker * rowsPerWorker
                val end = minOf(rowCount.toLong(), start.toLong() + rowsPerWorker).toInt()
                if (start >= end) {
                    null
                } else {
                    CompletableFuture.runAsync({
                        for (row in start until end) block(row)
                    }, executor)
                }
            }
            try {
                CompletableFuture.allOf(*tasks.toTypedArray()).join()
            } catch (failure: CompletionException) {
                throw failure.cause ?: failure
            }
        }

        internal fun scaleMask(
            source: ByteArray,
            sourceWidth: Int,
            sourceHeight: Int,
            width: Int,
            height: Int,
        ): ByteArray {
            require(sourceWidth > 0 && sourceHeight > 0 && width > 0 && height > 0)
            require(source.size.toLong() == sourceWidth.toLong() * sourceHeight)
            require(width.toLong() * height <= Int.MAX_VALUE)
            require(width.toLong() * sourceHeight <= Int.MAX_VALUE)
            val horizontal = resampleMaskAxis(source, sourceWidth, sourceHeight, width, horizontal = true)
            return resampleMaskAxis(horizontal, width, sourceHeight, height, horizontal = false)
        }

        private fun resampleMaskAxis(
            source: ByteArray,
            sourceWidth: Int,
            sourceHeight: Int,
            targetSize: Int,
            horizontal: Boolean,
        ): ByteArray {
            val sourceSize = if (horizontal) sourceWidth else sourceHeight
            if (sourceSize == targetSize) return source
            val targetWidth = if (horizontal) targetSize else sourceWidth
            val targetHeight = if (horizontal) sourceHeight else targetSize
            val taps = filterTaps(sourceSize, targetSize)
            val output = ByteArray(targetWidth * targetHeight)
            val lineCount = if (horizontal) sourceHeight else sourceWidth
            val sourceLineSize = if (horizontal) sourceWidth else sourceHeight
            for (line in 0 until lineCount) {
                // Cutout masks commonly contain thousands of identical rows/columns.
                if (line > 0 && matchingMaskLines(source, sourceWidth, sourceLineSize, line, horizontal)) {
                    if (horizontal) {
                        output.copyInto(output, line * targetWidth, (line - 1) * targetWidth, line * targetWidth)
                    } else {
                        for (position in 0 until targetHeight) {
                            output[position * targetWidth + line] = output[position * targetWidth + line - 1]
                        }
                    }
                    continue
                }
                for (position in 0 until targetSize) {
                    val filter = taps[position]
                    var sum = 0.0
                    for (index in filter.samples.indices) {
                        val weight = filter.normalizedWeights[index]
                        if (weight == 0.0) continue
                        val boundedSample = filter.samples[index]
                        val sourceX = if (horizontal) boundedSample else line
                        val sourceY = if (horizontal) line else boundedSample
                        sum += weight * (source[sourceY * sourceWidth + sourceX].toInt() and 0xff)
                    }
                    val outputIndex = if (horizontal) line * targetWidth + position else position * targetWidth + line
                    if (filter.weightSum == 0.0) {
                        val sourceX = if (horizontal) filter.nearest else line
                        val sourceY = if (horizontal) line else filter.nearest
                        output[outputIndex] = source[sourceY * sourceWidth + sourceX]
                    } else {
                        output[outputIndex] = sum.roundToInt().coerceIn(0, 255).toByte()
                    }
                }
            }
            return output
        }

        private fun matchingMaskLines(
            source: ByteArray,
            sourceWidth: Int,
            lineSize: Int,
            line: Int,
            horizontal: Boolean,
        ): Boolean {
            for (position in 0 until lineSize) {
                val index = if (horizontal) line * sourceWidth + position else position * sourceWidth + line
                val previous = index - if (horizontal) sourceWidth else 1
                if (source[index] != source[previous]) return false
            }
            return true
        }

        private class FilterTaps(
            val samples: IntArray,
            val weights: DoubleArray,
            val weightSum: Double,
            val nearest: Int,
        ) {
            val normalizedWeights = DoubleArray(weights.size) { index ->
                if (weightSum == 0.0) 0.0 else weights[index] / weightSum
            }
        }

        private fun unpremultiply(sum: Double, alphaSum: Double): Int =
            if (alphaSum == 0.0) 0 else (sum / alphaSum).roundToInt().coerceIn(0, 255)

        private fun lanczos3(value: Double): Double {
            val distance = kotlin.math.abs(value)
            if (distance == 0.0) return 1.0
            if (distance >= 3.0) return 0.0
            val first = sin(Math.PI * distance) / (Math.PI * distance)
            val second = sin(Math.PI * distance / 3.0) / (Math.PI * distance / 3.0)
            return first * second
        }

        private fun mitchell(value: Double): Double {
            val x = kotlin.math.abs(value)
            val b = 1.0 / 3.0
            val c = 1.0 / 3.0
            return when {
                x < 1.0 -> ((12 - 9 * b - 6 * c) * x * x * x +
                    (-18 + 12 * b + 6 * c) * x * x + (6 - 2 * b)) / 6.0
                x < 2.0 -> ((-b - 6 * c) * x * x * x +
                    (6 * b + 30 * c) * x * x + (-12 * b - 48 * c) * x + (8 * b + 24 * c)) / 6.0
                else -> 0.0
            }
        }

        private fun interpolate(start: Int, end: Int, amount: Float): Int {
            fun channel(shift: Int): Int {
                val first = start ushr shift and 0xff
                val second = end ushr shift and 0xff
                return (first + (second - first) * amount).roundToInt().coerceIn(0, 255)
            }
            return rgba(channel(24), channel(16), channel(8), channel(0))
        }

        private fun alpha(pixel: Int): Int = pixel and 0xff

        private fun blend(foreground: Int, background: Int, sourceAlpha: Int): Int {
            if (sourceAlpha == 0) return background
            if (sourceAlpha == 255) return foreground
            val backgroundAlpha = alpha(background)
            val outputAlpha = sourceAlpha + (backgroundAlpha * (255 - sourceAlpha) + 127) / 255
            if (outputAlpha == 0) return 0
            fun channel(shift: Int): Int {
                val source = foreground ushr shift and 0xff
                val destination = background ushr shift and 0xff
                val numerator = source * sourceAlpha +
                    (destination * backgroundAlpha * (255 - sourceAlpha) + 127) / 255
                return ((numerator + outputAlpha / 2) / outputAlpha).coerceIn(0, 255)
            }
            return rgba(channel(24), channel(16), channel(8), outputAlpha.coerceIn(0, 255))
        }

        private const val PARALLEL_RESAMPLE_PIXEL_THRESHOLD = 250_000L
    }
}

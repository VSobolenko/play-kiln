package com.gexetr.plinthproductphotos.domain.pipeline

import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class EnhancedOperation {
    RELIGHT,
    UPSCALE,
    ERASE,
    RECOLOUR,
    GHOST_MANNEQUIN,
    VIRTUAL_MODEL,
}

enum class FidelityDecision {
    DELIVERED,
    REJECTED,
}

data class FidelityThresholds(
    val maximumMeanDeltaE: Double?,
    val minimumGradientSsim: Double,
    val minimumSilhouetteIou: Double,
    val minimumTextSimilarity: Double,
)

data class FidelityMetrics(
    val meanDeltaE: Double,
    val gradientSsim: Double,
    val silhouetteIou: Double,
    val textSimilarity: Double?,
)

data class FidelityResult(
    val decision: FidelityDecision,
    val metrics: FidelityMetrics,
    val rejectionReasons: List<String>,
)

object FidelityGuard {
    // Provisional until S2 measurement, reset by task 2.3; annex E:133-136.
    const val RELIGHT_MAX_MEAN_DELTA_E = 4.0
    const val UPSCALE_MAX_MEAN_DELTA_E = 2.0
    const val MIN_GRADIENT_SSIM = 0.90
    const val MIN_SILHOUETTE_IOU = 0.97
    const val RECOLOUR_MIN_SILHOUETTE_IOU = 0.95
    const val MIN_TEXT_SIMILARITY = 0.90

    fun thresholdsFor(operation: EnhancedOperation): FidelityThresholds = when (operation) {
        EnhancedOperation.RELIGHT -> FidelityThresholds(
            maximumMeanDeltaE = RELIGHT_MAX_MEAN_DELTA_E,
            minimumGradientSsim = MIN_GRADIENT_SSIM,
            minimumSilhouetteIou = MIN_SILHOUETTE_IOU,
            minimumTextSimilarity = MIN_TEXT_SIMILARITY,
        )
        EnhancedOperation.GHOST_MANNEQUIN,
        EnhancedOperation.VIRTUAL_MODEL -> FidelityThresholds(
            maximumMeanDeltaE = null,
            minimumGradientSsim = MIN_GRADIENT_SSIM,
            minimumSilhouetteIou = MIN_SILHOUETTE_IOU,
            minimumTextSimilarity = MIN_TEXT_SIMILARITY,
        )
        EnhancedOperation.UPSCALE -> FidelityThresholds(
            maximumMeanDeltaE = UPSCALE_MAX_MEAN_DELTA_E,
            minimumGradientSsim = MIN_GRADIENT_SSIM,
            minimumSilhouetteIou = MIN_SILHOUETTE_IOU,
            minimumTextSimilarity = MIN_TEXT_SIMILARITY,
        )
        EnhancedOperation.ERASE -> FidelityThresholds(
            maximumMeanDeltaE = 0.0,
            minimumGradientSsim = MIN_GRADIENT_SSIM,
            minimumSilhouetteIou = MIN_SILHOUETTE_IOU,
            minimumTextSimilarity = MIN_TEXT_SIMILARITY,
        )
        EnhancedOperation.RECOLOUR -> FidelityThresholds(
            maximumMeanDeltaE = null,
            minimumGradientSsim = MIN_GRADIENT_SSIM,
            minimumSilhouetteIou = RECOLOUR_MIN_SILHOUETTE_IOU,
            minimumTextSimilarity = MIN_TEXT_SIMILARITY,
        )
    }

    fun evaluate(
        source: PixelBuffer,
        sourceMask: Mask,
        result: PixelBuffer,
        operation: EnhancedOperation,
        thresholds: FidelityThresholds = thresholdsFor(operation),
        sourceText: String? = null,
        resultText: String? = null,
        brandTokens: Set<String> = emptySet(),
    ): FidelityResult {
        require(sourceMask.alpha.size == source.pixels.size) { "Source mask must match the source dimensions" }
        require(thresholds.minimumGradientSsim in 0.0..1.0)
        require(thresholds.minimumSilhouetteIou in 0.0..1.0)
        require(thresholds.minimumTextSimilarity in 0.0..1.0)
        require(thresholds.maximumMeanDeltaE == null ||
            thresholds.maximumMeanDeltaE >= 0.0 && thresholds.maximumMeanDeltaE.isFinite()
        )

        val sourceRatio = source.width.toDouble() / source.height
        val resultRatio = result.width.toDouble() / result.height
        if (abs(sourceRatio - resultRatio) > sourceRatio * 0.01) {
            return FidelityResult(
                decision = FidelityDecision.REJECTED,
                metrics = FidelityMetrics(Double.POSITIVE_INFINITY, 0.0, 0.0, null),
                rejectionReasons = listOf("Result aspect ratio differs from the source"),
            )
        }
        val scale = 512.0 / max(source.width, source.height)
        val metricWidth = max(1, (source.width * scale).roundToInt())
        val metricHeight = max(1, (source.height * scale).roundToInt())
        val metricSource = source.scale(metricWidth, metricHeight)
        val metricResult = result.scale(metricWidth, metricHeight)
        val metricMask = scaleMask(sourceMask.alpha, source.width, source.height, metricWidth, metricHeight)

        val core = erodedCore(metricMask, metricWidth, metricHeight)
        val meanDeltaE = if (core.isEmpty()) Double.POSITIVE_INFINITY else core
            .map { index -> deltaE2000(metricSource.pixels[index], metricResult.pixels[index]) }.average()
        val gradientSsim = gradientSsim(metricSource, metricResult, core)
        val sourceSilhouette = metricMask.indices.filter { (metricMask[it].toInt() and 0xff) > 8 }.toSet()
        val resultSilhouette = metricResult.pixels.indices.filter { (metricResult.pixels[it] and 0xff) > 8 }.toSet()
        val union = sourceSilhouette union resultSilhouette
        val silhouetteIou = if (union.isEmpty()) 1.0
        else (sourceSilhouette intersect resultSilhouette).size.toDouble() / union.size
        val textSimilarity = if (sourceText != null && sourceText.count { it.isLetterOrDigit() } >= 4 &&
            resultText != null
        ) normalizedSimilarity(sourceText, resultText) else null

        val reasons = buildList {
            if (core.isEmpty()) add("Source mask has no opaque interior after 2 px erosion")
            if (thresholds.maximumMeanDeltaE != null && meanDeltaE > thresholds.maximumMeanDeltaE) {
                add("Mean item color drift exceeds the operation threshold")
            }
            if (gradientSsim < thresholds.minimumGradientSsim) add("Item gradient structure is below threshold")
            if (silhouetteIou < thresholds.minimumSilhouetteIou) add("Item silhouette IoU is below threshold")
            if (sourceText != null && sourceText.count { it.isLetterOrDigit() } >= 4 && resultText == null) {
                add("Source item text is missing from the result")
            } else if (textSimilarity != null && textSimilarity < thresholds.minimumTextSimilarity) {
                add("Item OCR similarity is below threshold")
            }
            val normalizedResultText = resultText.orEmpty().lowercase()
            for (token in brandTokens) {
                if (sourceText.orEmpty().contains(token, ignoreCase = true) &&
                    !normalizedResultText.contains(token.lowercase())
                ) {
                    add("A recognized brand token changed")
                    break
                }
            }
        }
        return FidelityResult(
            decision = if (reasons.isEmpty()) FidelityDecision.DELIVERED else FidelityDecision.REJECTED,
            metrics = FidelityMetrics(meanDeltaE, gradientSsim, silhouetteIou, textSimilarity),
            rejectionReasons = reasons,
        )
    }

    private fun erodedCore(mask: ByteArray, width: Int, height: Int): List<Int> {
        val core = ArrayList<Int>()
        for (y in 2 until height - 2) {
            for (x in 2 until width - 2) {
                var opaque = true
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        if ((mask[(y + dy) * width + x + dx].toInt() and 0xff) < 250) opaque = false
                    }
                }
                if (opaque) core.add(y * width + x)
            }
        }
        return core
    }

    private fun scaleMask(mask: ByteArray, sourceWidth: Int, sourceHeight: Int, width: Int, height: Int): ByteArray {
        val scaled = ByteArray(width * height)
        for (y in 0 until height) {
            val sourceY = (((y + 0.5) * sourceHeight / height).toInt()).coerceIn(0, sourceHeight - 1)
            for (x in 0 until width) {
                val sourceX = (((x + 0.5) * sourceWidth / width).toInt()).coerceIn(0, sourceWidth - 1)
                scaled[y * width + x] = mask[sourceY * sourceWidth + sourceX]
            }
        }
        return scaled
    }

    private fun gradientSsim(source: PixelBuffer, result: PixelBuffer, core: List<Int>): Double {
        if (core.isEmpty()) return 0.0
        val sourceGradient = DoubleArray(core.size)
        val resultGradient = DoubleArray(core.size)
        for ((position, index) in core.withIndex()) {
            val x = index % source.width
            val y = index / source.width
            sourceGradient[position] = hypot(
                (luminance(source[(x + 1).coerceAtMost(source.width - 1), y]) -
                    luminance(source[(x - 1).coerceAtLeast(0), y])) / 2.0,
                (luminance(source[x, (y + 1).coerceAtMost(source.height - 1)]) -
                    luminance(source[x, (y - 1).coerceAtLeast(0)])) / 2.0,
            )
            resultGradient[position] = hypot(
                (luminance(result[(x + 1).coerceAtMost(result.width - 1), y]) -
                    luminance(result[(x - 1).coerceAtLeast(0), y])) / 2.0,
                (luminance(result[x, (y + 1).coerceAtMost(result.height - 1)]) -
                    luminance(result[x, (y - 1).coerceAtLeast(0)])) / 2.0,
            )
        }
        val meanSource = sourceGradient.average()
        val meanResult = resultGradient.average()
        var varianceSource = 0.0
        var varianceResult = 0.0
        var covariance = 0.0
        for (index in sourceGradient.indices) {
            val sourceDifference = sourceGradient[index] - meanSource
            val resultDifference = resultGradient[index] - meanResult
            varianceSource += sourceDifference * sourceDifference
            varianceResult += resultDifference * resultDifference
            covariance += sourceDifference * resultDifference
        }
        val count = sourceGradient.size.toDouble()
        varianceSource /= count
        varianceResult /= count
        covariance /= count
        val c1 = 0.01 * 0.01
        val c2 = 0.03 * 0.03
        val numerator = (2 * meanSource * meanResult + c1) * (2 * covariance + c2)
        val denominator = (meanSource * meanSource + meanResult * meanResult + c1) *
            (varianceSource + varianceResult + c2)
        return (numerator / denominator).coerceIn(-1.0, 1.0)
    }

    private fun luminance(pixel: Int): Double {
        fun linear(shift: Int): Double {
            val value = (pixel ushr shift and 0xff) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(24) + 0.7152 * linear(16) + 0.0722 * linear(8)
    }

    private fun normalizedSimilarity(first: String, second: String): Double {
        val a = first.lowercase()
        val b = second.lowercase()
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = min(
                    min(current[j] + 1, previous[j + 1] + 1),
                    previous[j] + if (a[i] == b[j]) 0 else 1,
                )
            }
            previous = current
        }
        return 1.0 - previous[b.length].toDouble() / max(a.length, b.length)
    }

    private fun deltaE2000(first: Int, second: Int): Double {
        val lab1 = toLab(first)
        val lab2 = toLab(second)
        val l1 = lab1[0]
        val a1 = lab1[1]
        val b1 = lab1[2]
        val l2 = lab2[0]
        val a2 = lab2[1]
        val b2 = lab2[2]
        val c1 = hypot(a1, b1)
        val c2 = hypot(a2, b2)
        val meanC = (c1 + c2) / 2.0
        val g = 0.5 * (1.0 - sqrt(meanC.pow(7.0) / (meanC.pow(7.0) + 25.0.pow(7.0))))
        val adjustedA1 = (1.0 + g) * a1
        val adjustedA2 = (1.0 + g) * a2
        val adjustedC1 = hypot(adjustedA1, b1)
        val adjustedC2 = hypot(adjustedA2, b2)
        val h1 = hue(adjustedA1, b1)
        val h2 = hue(adjustedA2, b2)
        val deltaL = l2 - l1
        val deltaC = adjustedC2 - adjustedC1
        val deltaHue = when {
            adjustedC1 * adjustedC2 == 0.0 -> 0.0
            h2 - h1 > 180.0 -> h2 - h1 - 360.0
            h2 - h1 < -180.0 -> h2 - h1 + 360.0
            else -> h2 - h1
        }
        val deltaH = 2.0 * sqrt(adjustedC1 * adjustedC2) * sin(Math.toRadians(deltaHue / 2.0))
        val meanL = (l1 + l2) / 2.0
        val meanAdjustedC = (adjustedC1 + adjustedC2) / 2.0
        val meanHue = when {
            adjustedC1 * adjustedC2 == 0.0 -> h1 + h2
            abs(h1 - h2) <= 180.0 -> (h1 + h2) / 2.0
            h1 + h2 < 360.0 -> (h1 + h2 + 360.0) / 2.0
            else -> (h1 + h2 - 360.0) / 2.0
        }
        val t = 1.0 - 0.17 * cos(Math.toRadians(meanHue - 30.0)) +
            0.24 * cos(Math.toRadians(2.0 * meanHue)) +
            0.32 * cos(Math.toRadians(3.0 * meanHue + 6.0)) -
            0.20 * cos(Math.toRadians(4.0 * meanHue - 63.0))
        val deltaTheta = 30.0 * exp(-(((meanHue - 275.0) / 25.0).pow(2.0)))
        val rc = 2.0 * sqrt(meanAdjustedC.pow(7.0) / (meanAdjustedC.pow(7.0) + 25.0.pow(7.0)))
        val lTerm = meanL - 50.0
        val sl = 1.0 + 0.015 * lTerm * lTerm / sqrt(20.0 + lTerm * lTerm)
        val sc = 1.0 + 0.045 * meanAdjustedC
        val sh = 1.0 + 0.015 * meanAdjustedC * t
        val rt = -sin(Math.toRadians(2.0 * deltaTheta)) * rc
        val l = deltaL / sl
        val c = deltaC / sc
        val h = deltaH / sh
        return sqrt(max(0.0, l * l + c * c + h * h + rt * c * h))
    }

    private fun hue(a: Double, b: Double): Double {
        if (a == 0.0 && b == 0.0) return 0.0
        val angle = Math.toDegrees(atan2(b, a))
        return if (angle < 0.0) angle + 360.0 else angle
    }

    private fun toLab(pixel: Int): DoubleArray {
        fun linear(shift: Int): Double {
            val channel = (pixel ushr shift and 0xff) / 255.0
            return if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
        }
        val red = linear(24)
        val green = linear(16)
        val blue = linear(8)
        val x = (0.4124564 * red + 0.3575761 * green + 0.1804375 * blue) / 0.95047
        val y = 0.2126729 * red + 0.7151522 * green + 0.0721750 * blue
        val z = (0.0193339 * red + 0.1191920 * green + 0.9503041 * blue) / 1.08883
        fun pivot(value: Double): Double = if (value > 216.0 / 24389.0) {
            value.pow(1.0 / 3.0)
        } else {
            (24389.0 / 27.0 * value + 16.0) / 116.0
        }
        val fx = pivot(x)
        val fy = pivot(y)
        val fz = pivot(z)
        return doubleArrayOf(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
    }

    private fun Double.pow(power: Double): Double = Math.pow(this, power)
}

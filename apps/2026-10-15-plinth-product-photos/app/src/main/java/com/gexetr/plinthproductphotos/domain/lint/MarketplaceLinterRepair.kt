package com.gexetr.plinthproductphotos.domain.lint

import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

sealed interface LinterRepairResult {
    data class Applied(
        val finding: LintFinding,
        val image: PixelBuffer,
        val facts: ImageFacts,
        val format: String,
        val quality: Int?,
    ) : LinterRepairResult

    data class RequiresSellerAction(
        val finding: LintFinding,
        val fix: LinterFix,
        val creditsBeforeRun: Int? = null,
    ) : LinterRepairResult
}

object MarketplaceLinterRepair {
    fun apply(
        finding: LintFinding,
        facts: ImageFacts,
        image: PixelBuffer,
        preset: MarketplacePreset,
        recompressedByteCount: Long? = null,
    ): LinterRepairResult {
        val fix = LinterFix.forFinding(finding, preset)
        if (fix is LinterFix.CannotBeSatisfiedByEditing) {
            return LinterRepairResult.RequiresSellerAction(finding, fix)
        }

        var repairedImage = image
        var format = facts.format
        var quality = facts.jpegQualityEstimate
        var repairedFacts = facts.copy(width = image.width, height = image.height)
        when (fix) {
            is LinterFix.ConvertFormat -> {
                format = fix.to
                repairedFacts = repairedFacts.copy(format = format)
            }
            is LinterFix.Recompress -> {
                val byteCount = if (finding.ruleId == "L02") {
                    recompressedByteCount ?: return LinterRepairResult.RequiresSellerAction(finding, fix)
                } else {
                    facts.bytes
                }
                quality = fix.qualityFloor
                repairedFacts = repairedFacts.copy(
                    jpegQualityEstimate = quality,
                    bytes = byteCount,
                )
            }
            is LinterFix.Downscale -> {
                val scale = min(
                    fix.maxSide?.toDouble()?.div(max(image.width, image.height)) ?: 1.0,
                    min(
                        fix.maxLongSide?.toDouble()?.div(max(image.width, image.height)) ?: 1.0,
                        fix.maxSidesSum?.toDouble()?.div(image.width + image.height) ?: 1.0,
                    ),
                ).coerceAtMost(1.0)
                if (scale >= 1.0) return LinterRepairResult.RequiresSellerAction(finding, fix)
                repairedImage = image.scale(
                    (image.width * scale).roundToInt().coerceAtLeast(1),
                    (image.height * scale).roundToInt().coerceAtLeast(1),
                )
                repairedFacts = repairedFacts.copy(width = repairedImage.width, height = repairedImage.height)
            }
            is LinterFix.PadToAspect -> {
                repairedImage = padToAspect(image, fix.aspect)
                repairedFacts = repairedFacts.copy(width = repairedImage.width, height = repairedImage.height)
            }
            is LinterFix.CropToAspect -> {
                repairedImage = cropToAspect(image, fix.aspect)
                repairedFacts = repairedFacts.copy(width = repairedImage.width, height = repairedImage.height)
            }
            is LinterFix.FlattenOnBackground -> {
                repairedImage = flattenOnCornerBackground(image)
                repairedFacts = repairedFacts.copy(hasAlpha = false)
            }
            LinterFix.StripExif -> {
                repairedFacts = repairedFacts.copy(exifGpsPresent = false)
            }
            is LinterFix.Upscale -> {
                val requiredSide = maxOf(fix.minWidth ?: 0, fix.minHeight ?: 0, fix.minLongSide ?: 0)
                val credits = if (requiredSide > 2048) 2 else 1
                return LinterRepairResult.RequiresSellerAction(finding, fix, credits)
            }
            else -> return LinterRepairResult.RequiresSellerAction(finding, fix)
        }

        repairedFacts = repairedFacts.copy(
            ratio = repairedImage.width.toDouble() / repairedImage.height,
            format = format,
            hasAlpha = repairedFacts.hasAlpha,
        )
        val findingStillPresent = MarketplaceLinter.evaluate(repairedFacts, preset)
            .any { it.ruleId == finding.ruleId }
        if (findingStillPresent) return LinterRepairResult.RequiresSellerAction(finding, fix)
        return LinterRepairResult.Applied(finding, repairedImage, repairedFacts, format, quality)
    }

    private fun padToAspect(image: PixelBuffer, aspectText: String): PixelBuffer {
        val aspect = aspect(aspectText)
        val targetWidth: Int
        val targetHeight: Int
        if (image.width.toDouble() / image.height < aspect) {
            targetWidth = (image.height * aspect).roundToInt().coerceAtLeast(image.width)
            targetHeight = image.height
        } else {
            targetWidth = image.width
            targetHeight = (image.width / aspect).roundToInt().coerceAtLeast(image.height)
        }
        val background = image[0, 0]
        val output = IntArray(targetWidth * targetHeight) { background }
        val left = (targetWidth - image.width) / 2
        val top = (targetHeight - image.height) / 2
        for (y in 0 until image.height) {
            image.pixels.copyInto(
                output,
                destinationOffset = (top + y) * targetWidth + left,
                startIndex = y * image.width,
                endIndex = (y + 1) * image.width,
            )
        }
        return PixelBuffer(targetWidth, targetHeight, output)
    }

    private fun cropToAspect(image: PixelBuffer, aspectText: String): PixelBuffer {
        val aspect = aspect(aspectText)
        val imageAspect = image.width.toDouble() / image.height
        val width: Int
        val height: Int
        if (imageAspect > aspect) {
            width = (image.height * aspect).roundToInt().coerceIn(1, image.width)
            height = image.height
        } else {
            width = image.width
            height = (image.width / aspect).roundToInt().coerceIn(1, image.height)
        }
        return image.crop((image.width - width) / 2, (image.height - height) / 2, width, height)
    }

    private fun aspect(value: String): Double {
        val parts = value.split(':')
        require(parts.size == 2) { "Invalid target aspect: $value" }
        val width = parts[0].toDoubleOrNull()
        val height = parts[1].toDoubleOrNull()
        require(width != null && height != null && width > 0.0 && height > 0.0) {
            "Invalid target aspect: $value"
        }
        return width / height
    }

    private fun flattenOnCornerBackground(image: PixelBuffer): PixelBuffer {
        val background = image[0, 0]
        val backgroundRed = background ushr 24 and 0xff
        val backgroundGreen = background ushr 16 and 0xff
        val backgroundBlue = background ushr 8 and 0xff
        val output = IntArray(image.pixels.size) { index ->
            val pixel = image.pixels[index]
            val alpha = pixel and 0xff
            val inverseAlpha = 255 - alpha
            val red = ((pixel ushr 24 and 0xff) * alpha + backgroundRed * inverseAlpha + 127) / 255
            val green = ((pixel ushr 16 and 0xff) * alpha + backgroundGreen * inverseAlpha + 127) / 255
            val blue = ((pixel ushr 8 and 0xff) * alpha + backgroundBlue * inverseAlpha + 127) / 255
            PixelBuffer.rgba(red, green, blue)
        }
        return PixelBuffer(image.width, image.height, output)
    }
}

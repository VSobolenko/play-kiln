package com.gexetr.plinthproductphotos.domain.pipeline

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PixelBounds(val left: Int, val top: Int, val width: Int, val height: Int)

data class CompositionResult(
    val image: PixelBuffer,
    val itemBounds: PixelBounds,
    val scale: Float,
)

object Compositor {
    fun compose(
        source: PixelBuffer,
        mask: Mask,
        canvas: Canvas,
        look: Look,
        sceneBackground: PixelBuffer? = null,
    ): CompositionResult {
        require(canvas.width > 0 && canvas.height > 0) { "Canvas dimensions must be positive" }
        require(canvas.width.toLong() * canvas.height <= Int.MAX_VALUE) { "Canvas is too large" }
        require(mask.alpha.size == source.pixels.size) { "Mask dimensions must match the source" }
        require(look.marginPct.isFinite() && look.marginPct >= 0f) { "Margin must be finite and non-negative" }
        require(look.fillTarget.isFinite() && look.fillTarget > 0f) { "Fill target must be finite and positive" }

        val bounds = findBounds(source.width, source.height, mask.alpha)
            ?: throw IllegalArgumentException("No item pixels found in mask")
        val margin = (look.marginPct * min(canvas.width, canvas.height)).roundToInt()
        val availableWidth = (canvas.width.toLong() - 2L * margin)
        val availableHeight = (canvas.height.toLong() - 2L * margin)
        require(availableWidth > 0 && availableHeight > 0) { "Margin leaves no available canvas area" }

        val fitScale = min(availableWidth.toFloat() / bounds.width, availableHeight.toFloat() / bounds.height)
        val fillScale = look.fillTarget / max(bounds.width.toFloat() / canvas.width, bounds.height.toFloat() / canvas.height)
        val scale = min(fitScale, fillScale)
        val itemWidth = max(1, (bounds.width * scale).roundToInt())
        val itemHeight = max(1, (bounds.height * scale).roundToInt())
        val left = (canvas.width - itemWidth) / 2
        val verticalOffset = if (look.shadow.kind == ShadowSpec.Kind.NONE) 0 else (canvas.height * 0.02f).roundToInt()
        val top = ((canvas.height - itemHeight) / 2 - verticalOffset)
            .coerceIn(0, canvas.height - itemHeight)
        val cropped = if (bounds.left == 0 && bounds.top == 0 &&
            bounds.width == source.width && bounds.height == source.height
        ) source else source.crop(bounds.left, bounds.top, bounds.width, bounds.height)
        val sourceItem = cropped.scale(itemWidth, itemHeight)
        val itemMask = scaleMask(mask.alpha, source.width, bounds, itemWidth, itemHeight)
        var background = renderBackground(canvas, look.background, sceneBackground)

        if (look.shadow.kind != ShadowSpec.Kind.NONE) {
            val placedMask = ByteArray(canvas.width * canvas.height)
            for (y in 0 until itemHeight) {
                itemMask.copyInto(
                    destination = placedMask,
                    destinationOffset = (top + y) * canvas.width + left,
                    startIndex = y * itemWidth,
                    endIndex = (y + 1) * itemWidth,
                )
            }
            val sigma = look.shadow.blurPct * canvas.width
            when (look.shadow.kind) {
                ShadowSpec.Kind.NONE -> Unit
                ShadowSpec.Kind.CONTACT -> {
                    background = background.dropShadow(
                        maskAlpha = placedMask,
                        opacity = look.shadow.opacity,
                        blurRadius = sigma,
                        offsetY = max(1, (canvas.height * 0.005f).roundToInt()),
                        verticalScale = 0.15f,
                    )
                }
                ShadowSpec.Kind.DROP -> {
                    val offset = (look.shadow.offsetPct * min(canvas.width, canvas.height)).roundToInt()
                    background = background.dropShadow(
                        maskAlpha = placedMask,
                        opacity = look.shadow.opacity,
                        blurRadius = sigma,
                        offsetY = offset,
                    )
                }
            }
        }

        val image = sourceItem.compositeOver(background, left, top, itemMask)
        return CompositionResult(
            image = image,
            itemBounds = PixelBounds(left, top, itemWidth, itemHeight),
            scale = scale,
        )
    }

    private fun renderBackground(
        canvas: Canvas,
        background: BackgroundSpec,
        sceneBackground: PixelBuffer?,
    ): PixelBuffer =
        when (background.kind) {
            BackgroundSpec.Kind.SOLID -> PixelBuffer.solid(
                canvas.width,
                canvas.height,
                PixelBuffer.fromArgb(requireNotNull(background.colorArgb) { "Solid background needs a color" }),
            )
            BackgroundSpec.Kind.GRADIENT -> PixelBuffer.gradient(
                canvas.width,
                canvas.height,
                startArgb = requireNotNull(background.colorArgb) { "Gradient background needs a start color" },
                endArgb = requireNotNull(background.color2Argb) { "Gradient background needs an end color" },
                angleDeg = requireNotNull(background.angleDeg) { "Gradient background needs an angle" },
            )
            BackgroundSpec.Kind.PROCEDURAL_SCENE,
            BackgroundSpec.Kind.GENERATED_SCENE -> coverFit(
                requireNotNull(sceneBackground) { "Scene background pixels are required for this background kind" },
                canvas,
            )
        }

    private fun coverFit(source: PixelBuffer, canvas: Canvas): PixelBuffer {
        val sourceRatio = source.width.toDouble() / source.height
        val canvasRatio = canvas.width.toDouble() / canvas.height
        return if (sourceRatio > canvasRatio) {
            val cropWidth = (source.height * canvasRatio).roundToInt().coerceIn(1, source.width)
            source.crop((source.width - cropWidth) / 2, 0, cropWidth, source.height)
                .scale(canvas.width, canvas.height)
        } else {
            val cropHeight = (source.width / canvasRatio).roundToInt().coerceIn(1, source.height)
            source.crop(0, (source.height - cropHeight) / 2, source.width, cropHeight)
                .scale(canvas.width, canvas.height)
        }
    }

    private fun findBounds(width: Int, height: Int, alpha: ByteArray): PixelBounds? {
        var left = width
        var top = height
        var right = -1
        var bottom = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if ((alpha[y * width + x].toInt() and 0xff) > 8) {
                    left = min(left, x)
                    top = min(top, y)
                    right = max(right, x)
                    bottom = max(bottom, y)
                }
            }
        }
        return if (right < left || bottom < top) null
        else PixelBounds(left, top, right - left + 1, bottom - top + 1)
    }

    private fun scaleMask(
        sourceMask: ByteArray,
        sourceWidth: Int,
        bounds: PixelBounds,
        targetWidth: Int,
        targetHeight: Int,
    ): ByteArray {
        val scaled = ByteArray(targetWidth * targetHeight)
        for (y in 0 until targetHeight) {
            val sourceY = (((y + 0.5) * bounds.height / targetHeight).toInt()).coerceIn(0, bounds.height - 1)
            for (x in 0 until targetWidth) {
                val sourceX = (((x + 0.5) * bounds.width / targetWidth).toInt()).coerceIn(0, bounds.width - 1)
                scaled[y * targetWidth + x] =
                    sourceMask[(bounds.top + sourceY) * sourceWidth + bounds.left + sourceX]
            }
        }
        return scaled
    }
}

package com.gexetr.coverkit.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Renders a [CoverProject] layer tree to an exact catalog-size bitmap / PNG.
 *
 * Uses [Bitmap.createBitmap] + [Canvas] only — never `PixelCopy` or a Compose preview screenshot
 * (Thumbnail Maker blur defect absorption).
 */
object ExportRenderer {

    const val WATERMARK_TEXT: String = "Coverkit"

    /**
     * Optional photo decode hook so the UI can supply a content-resolver decode without
     * coupling this renderer to Android Context.
     */
    fun interface PhotoDecoder {
        fun decode(uriString: String): Bitmap?
    }

    fun renderBitmap(
        project: CoverProject,
        watermark: Boolean,
        targetDestinationId: String = project.destinationId,
        photoDecoder: PhotoDecoder = PhotoDecoder { null },
    ): Bitmap {
        val destination = DestinationCatalog.requireById(targetDestinationId)
        val width = destination.widthPx
        val height = destination.heightPx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFF111111.toInt())

        val source = DestinationCatalog.requireById(project.destinationId)
        val fit = min(width.toFloat() / source.widthPx, height.toFloat() / source.heightPx)
        val offsetX = (width - source.widthPx * fit) / 2f
        val offsetY = (height - source.heightPx * fit) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(fit, fit)

        project.layers
            .sortedBy { it.zIndex }
            .forEach { layer -> drawLayer(canvas, source.widthPx, source.heightPx, layer, photoDecoder) }

        canvas.restore()

        if (watermark) {
            drawWatermark(canvas, width, height)
        }
        return bitmap
    }

    /** Brief-compatible alias. */
    fun render(
        project: CoverProject,
        watermark: Boolean,
        targetDestinationId: String = project.destinationId,
    ): Bitmap = renderBitmap(project, watermark, targetDestinationId)

    fun renderPng(
        project: CoverProject,
        watermark: Boolean,
        targetDestinationId: String = project.destinationId,
        photoDecoder: PhotoDecoder = PhotoDecoder { null },
        quality: Int = 100,
    ): ByteArray {
        val bitmap = renderBitmap(project, watermark, targetDestinationId, photoDecoder)
        return try {
            ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, quality.coerceIn(0, 100), stream)
                stream.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawLayer(
        canvas: Canvas,
        width: Int,
        height: Int,
        layer: Layer,
        photoDecoder: PhotoDecoder,
    ) {
        val cx = layer.x * width
        val cy = layer.y * height
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(layer.rotationDeg)
        canvas.scale(layer.scale, layer.scale)
        when (layer) {
            is Layer.PhotoLayer -> drawPhoto(canvas, width, height, layer, photoDecoder)
            is Layer.TextLayer -> drawText(canvas, width, height, layer)
            is Layer.StickerLayer -> drawSticker(canvas, width, height, layer)
        }
        canvas.restore()
    }

    private fun drawPhoto(
        canvas: Canvas,
        width: Int,
        height: Int,
        layer: Layer.PhotoLayer,
        photoDecoder: PhotoDecoder,
    ) {
        val halfW = width / 2f
        val halfH = height / 2f
        val dest = RectF(-halfW, -halfH, halfW, halfH)
        val uri = layer.uriString
        val decoded = if (!uri.isNullOrBlank()) photoDecoder.decode(uri) else null
        if (decoded != null) {
            val src = Rect(0, 0, decoded.width, decoded.height)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(decoded, src, dest, paint)
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = layer.colorArgb
            }
            canvas.drawRect(dest, paint)
        }
    }

    private fun drawText(
        canvas: Canvas,
        width: Int,
        height: Int,
        layer: Layer.TextLayer,
    ) {
        val baseSize = min(width, height) * 0.08f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = baseSize
        }
        if (layer.strokeWidthPx > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = layer.strokeWidthPx
            paint.color = layer.strokeArgb
            canvas.drawText(layer.text, 0f, 0f, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = layer.fillArgb
        canvas.drawText(layer.text, 0f, 0f, paint)
    }

    private fun drawSticker(
        canvas: Canvas,
        width: Int,
        height: Int,
        layer: Layer.StickerLayer,
    ) {
        val unit = min(width, height) * 0.12f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = layer.tintArgb
        }
        val path = Path()
        when (layer.kind) {
            StickerKind.ARROW -> {
                path.moveTo(-unit, -unit * 0.25f)
                path.lineTo(unit * 0.35f, -unit * 0.25f)
                path.lineTo(unit * 0.35f, -unit * 0.6f)
                path.lineTo(unit, 0f)
                path.lineTo(unit * 0.35f, unit * 0.6f)
                path.lineTo(unit * 0.35f, unit * 0.25f)
                path.lineTo(-unit, unit * 0.25f)
                path.close()
            }
            StickerKind.RIBBON -> {
                path.moveTo(-unit, -unit * 0.45f)
                path.lineTo(unit * 0.7f, -unit * 0.45f)
                path.lineTo(unit, 0f)
                path.lineTo(unit * 0.7f, unit * 0.45f)
                path.lineTo(-unit, unit * 0.45f)
                path.lineTo(-unit * 0.7f, 0f)
                path.close()
            }
            StickerKind.FRAME -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = unit * 0.12f
                path.addRect(-unit, -unit * 0.75f, unit, unit * 0.75f, Path.Direction.CW)
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawWatermark(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66FFFFFF
            textAlign = Paint.Align.RIGHT
            textSize = min(width, height) * 0.045f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val margin = min(width, height) * 0.04f
        canvas.drawText(WATERMARK_TEXT, width - margin, height - margin, paint)
    }
}

package com.gexetr.plinthproductphotos.data.slide

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels.toBitmap
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.slide.SlideRenderPlan
import com.gexetr.plinthproductphotos.domain.slide.SlideShape
import com.gexetr.plinthproductphotos.domain.slide.SlideTextPlacement
import kotlin.math.hypot
import kotlin.math.min

class SlideTextLayoutException(
    val slotName: String,
    val originalText: String,
) : IllegalArgumentException("Text in $slotName does not fit its slide text plate")

object SlidePixelRenderer {
    fun render(plan: SlideRenderPlan): PixelBuffer {
        val bitmap = plan.baseImage.toBitmap()
        try {
            val canvas = Canvas(bitmap)
            for (shape in plan.shapes) drawShape(canvas, shape)
            for (block in plan.textBlocks) drawText(canvas, block)
            val argb = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return PixelBuffer(
                width = bitmap.width,
                height = bitmap.height,
                pixels = IntArray(argb.size) { index -> PixelBuffer.fromArgb(argb[index]) },
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawShape(canvas: Canvas, shape: SlideShape) {
        when (shape) {
            is SlideShape.Plate -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = shape.colorArgb
                    style = Paint.Style.FILL
                }
                canvas.drawRect(
                    RectF(
                        shape.bounds.left.toFloat(),
                        shape.bounds.top.toFloat(),
                        shape.bounds.right.toFloat(),
                        shape.bounds.bottom.toFloat(),
                    ),
                    paint,
                )
            }
            is SlideShape.Line -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = shape.colorArgb
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawLine(
                    shape.startX.toFloat(),
                    shape.startY.toFloat(),
                    shape.endX.toFloat(),
                    shape.endY.toFloat(),
                    paint,
                )
                if (shape.arrowHeads) drawArrowHeads(canvas, shape, paint)
            }
        }
    }

    private fun drawArrowHeads(canvas: Canvas, shape: SlideShape.Line, paint: Paint) {
        val dx = (shape.endX - shape.startX).toFloat()
        val dy = (shape.endY - shape.startY).toFloat()
        val length = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (length == 0f) return
        val unitX = dx / length
        val unitY = dy / length
        val headLength = min(length / 4f, paint.strokeWidth * 4f)
        val halfWidth = headLength / 2f
        for (direction in listOf(1f, -1f)) {
            val tipX = if (direction > 0f) shape.endX.toFloat() else shape.startX.toFloat()
            val tipY = if (direction > 0f) shape.endY.toFloat() else shape.startY.toFloat()
            val inward = if (direction > 0f) -1f else 1f
            val baseX = tipX + unitX * headLength * inward
            val baseY = tipY + unitY * headLength * inward
            val normalX = -unitY * halfWidth
            val normalY = unitX * halfWidth
            val path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseX + normalX, baseY + normalY)
                lineTo(baseX - normalX, baseY - normalY)
                close()
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawText(canvas: Canvas, block: SlideTextPlacement) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = block.textColorArgb
            textSize = block.fontSizePx
            typeface = Typeface.create(
                "sans-serif",
                if (block.slot.wireValue == "TITLE" || block.slot.wireValue == "BADGE") {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                },
            )
        }
        val availableWidth = block.bounds.width.toFloat()
        val lines = wrapByMeasuredWidth(block, paint, availableWidth)
        val metrics = paint.fontMetrics
        val lineHeight = metrics.descent - metrics.ascent
        val requiredHeight = lines.size * lineHeight
        if (requiredHeight > block.bounds.height) {
            throw SlideTextLayoutException(block.slot.wireValue, block.text)
        }
        val firstBaseline = block.bounds.top + (block.bounds.height - requiredHeight) / 2f - metrics.ascent
        for ((index, line) in lines.withIndex()) {
            val x = block.bounds.left + (block.bounds.width - paint.measureText(line)) / 2f
            canvas.drawText(line, x, firstBaseline + index * lineHeight, paint)
        }
    }

    private fun wrapByMeasuredWidth(
        block: SlideTextPlacement,
        paint: TextPaint,
        width: Float,
    ): List<String> {
        val lines = mutableListOf<String>()
        var start = 0
        while (start < block.text.length) {
            val count = paint.breakText(block.text, start, block.text.length, true, width, null)
            if (count <= 0) throw SlideTextLayoutException(block.slot.wireValue, block.text)
            val end = start + count
            lines += block.text.substring(start, end)
            start = end
        }
        return lines
    }
}

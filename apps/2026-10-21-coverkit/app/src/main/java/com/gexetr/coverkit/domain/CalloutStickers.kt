package com.gexetr.coverkit.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * YouTube-style callouts and story frames drawn as vectors (Canvas paths).
 * Shelf ABSORB: Thumbnail Maker / StoryArt stickers — not hand-pixel emoji art.
 */
object CalloutStickers {

    enum class Kind { ARROW, RIBBON, BURST, STORY_FRAME, CIRCLE }

    fun render(kind: Kind, tintArgb: Int, sizePx: Int = 384): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = tintArgb or (0xFF shl 24)
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.035f
            color = Color.WHITE
        }
        when (kind) {
            Kind.ARROW -> drawArrow(canvas, sizePx, fill, stroke)
            Kind.RIBBON -> drawRibbon(canvas, sizePx, fill, stroke)
            Kind.BURST -> drawBurst(canvas, sizePx, fill, stroke)
            Kind.STORY_FRAME -> drawStoryFrame(canvas, sizePx, fill, stroke)
            Kind.CIRCLE -> drawCircle(canvas, sizePx, fill, stroke)
        }
        return bmp
    }

    private fun drawArrow(c: Canvas, s: Int, fill: Paint, stroke: Paint) {
        val path = Path().apply {
            moveTo(s * 0.12f, s * 0.42f)
            lineTo(s * 0.58f, s * 0.42f)
            lineTo(s * 0.58f, s * 0.28f)
            lineTo(s * 0.90f, s * 0.50f)
            lineTo(s * 0.58f, s * 0.72f)
            lineTo(s * 0.58f, s * 0.58f)
            lineTo(s * 0.12f, s * 0.58f)
            close()
        }
        c.drawPath(path, fill)
        c.drawPath(path, stroke)
    }

    private fun drawRibbon(c: Canvas, s: Int, fill: Paint, stroke: Paint) {
        val path = Path().apply {
            moveTo(s * 0.08f, s * 0.28f)
            lineTo(s * 0.78f, s * 0.28f)
            lineTo(s * 0.92f, s * 0.50f)
            lineTo(s * 0.78f, s * 0.72f)
            lineTo(s * 0.08f, s * 0.72f)
            lineTo(s * 0.18f, s * 0.50f)
            close()
        }
        c.drawPath(path, fill)
        c.drawPath(path, stroke)
    }

    private fun drawBurst(c: Canvas, s: Int, fill: Paint, stroke: Paint) {
        val cx = s / 2f
        val cy = s / 2f
        val outer = s * 0.46f
        val inner = s * 0.22f
        val points = 8
        val path = Path()
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outer else inner
            val a = Math.PI * i / points - Math.PI / 2
            val x = cx + (r * Math.cos(a)).toFloat()
            val y = cy + (r * Math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, fill)
        c.drawPath(path, stroke)
    }

    private fun drawStoryFrame(c: Canvas, s: Int, fill: Paint, stroke: Paint) {
        val inset = s * 0.08f
        val round = s * 0.08f
        val rect = RectF(inset, inset, s - inset, s - inset)
        val frame = Paint(stroke).apply {
            strokeWidth = s * 0.06f
            color = fill.color
        }
        c.drawRoundRect(rect, round, round, frame)
        val inner = RectF(inset * 2.2f, inset * 2.2f, s - inset * 2.2f, s - inset * 2.2f)
        c.drawRoundRect(inner, round * 0.7f, round * 0.7f, stroke)
    }

    private fun drawCircle(c: Canvas, s: Int, fill: Paint, stroke: Paint) {
        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.38f
        c.drawCircle(cx, cy, r, fill)
        c.drawCircle(cx, cy, r, stroke)
    }
}

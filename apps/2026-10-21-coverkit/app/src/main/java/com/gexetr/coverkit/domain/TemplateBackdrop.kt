package com.gexetr.coverkit.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

/**
 * Six visually distinct starter compositions — not the same blob with a new tint.
 * Used for template thumbnails and the PhotoEditor source so opening two templates
 * cannot look identical (owner 2026-10-22).
 */
enum class BackdropStyle {
    TOP_BAND,
    BOTTOM_BAND,
    LEFT_RAIL,
    CENTER_STACK,
    DIAGONAL,
    SPLIT,
}

object TemplateBackdrop {

    fun fromIndex(index: Int): BackdropStyle =
        BackdropStyle.entries[index.floorMod(BackdropStyle.entries.size)]

    fun render(width: Int, height: Int, colorArgb: Int, style: BackdropStyle): Bitmap {
        val w = width.coerceAtLeast(64)
        val h = height.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = darken(colorArgb, 0.35f) }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), base)

        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorArgb }
        val light = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lighten(colorArgb, 0.25f) }
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6FFFFFF.toInt() }

        when (style) {
            BackdropStyle.TOP_BAND -> {
                canvas.drawRect(0f, 0f, w.toFloat(), h * 0.28f, accent)
                canvas.drawRect(w * 0.06f, h * 0.38f, w * 0.78f, h * 0.48f, ink)
                canvas.drawRect(w * 0.06f, h * 0.52f, w * 0.52f, h * 0.58f, light)
            }
            BackdropStyle.BOTTOM_BAND -> {
                canvas.drawRect(0f, h * 0.62f, w.toFloat(), h.toFloat(), accent)
                canvas.drawCircle(w * 0.78f, h * 0.28f, min(w, h) * 0.16f, light)
                canvas.drawRect(w * 0.08f, h * 0.70f, w * 0.72f, h * 0.80f, ink)
                canvas.drawRect(w * 0.08f, h * 0.84f, w * 0.48f, h * 0.90f, ink.apply { alpha = 180 })
            }
            BackdropStyle.LEFT_RAIL -> {
                canvas.drawRect(0f, 0f, w * 0.22f, h.toFloat(), accent)
                canvas.drawRect(w * 0.30f, h * 0.36f, w * 0.92f, h * 0.48f, ink)
                canvas.drawRect(w * 0.30f, h * 0.54f, w * 0.70f, h * 0.60f, light)
                canvas.drawCircle(w * 0.78f, h * 0.78f, min(w, h) * 0.12f, accent)
            }
            BackdropStyle.CENTER_STACK -> {
                val pad = min(w, h) * 0.08f
                canvas.drawRoundRect(RectF(pad, h * 0.22f, w - pad, h * 0.78f), pad, pad, accent)
                canvas.drawRect(w * 0.18f, h * 0.40f, w * 0.82f, h * 0.50f, ink)
                canvas.drawRect(w * 0.28f, h * 0.56f, w * 0.72f, h * 0.62f, light)
            }
            BackdropStyle.DIAGONAL -> {
                val path = Path().apply {
                    moveTo(0f, h * 0.35f)
                    lineTo(w.toFloat(), 0f)
                    lineTo(w.toFloat(), h * 0.55f)
                    lineTo(0f, h.toFloat())
                    close()
                }
                canvas.drawPath(path, accent)
                canvas.drawRect(w * 0.10f, h * 0.18f, w * 0.70f, h * 0.28f, ink)
                canvas.drawRect(w * 0.10f, h * 0.72f, w * 0.55f, h * 0.80f, light)
            }
            BackdropStyle.SPLIT -> {
                canvas.drawRect(0f, 0f, w * 0.5f, h.toFloat(), accent)
                canvas.drawRect(w * 0.5f, 0f, w.toFloat(), h.toFloat(), light)
                canvas.drawRect(w * 0.08f, h * 0.42f, w * 0.42f, h * 0.52f, ink)
                canvas.drawRect(w * 0.58f, h * 0.42f, w * 0.92f, h * 0.52f, ink)
            }
        }

        // Soft vignette so every style still reads as a cover, not a flat swatch.
        val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, h * 0.55f, 0f, h.toFloat(),
                intArrayOf(0x00000000, 0x66000000),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, h * 0.55f, w.toFloat(), h.toFloat(), vignette)
        return bitmap
    }

    private fun min(a: Int, b: Int): Float = kotlin.math.min(a, b).toFloat()

    private fun darken(argb: Int, amount: Float): Int {
        val a = argb ushr 24 and 0xFF
        val r = ((argb shr 16 and 0xFF) * (1f - amount)).toInt().coerceIn(0, 255)
        val g = ((argb shr 8 and 0xFF) * (1f - amount)).toInt().coerceIn(0, 255)
        val b = ((argb and 0xFF) * (1f - amount)).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lighten(argb: Int, amount: Float): Int {
        val a = argb ushr 24 and 0xFF
        val r = ((argb shr 16 and 0xFF) + (255 - (argb shr 16 and 0xFF)) * amount).toInt().coerceIn(0, 255)
        val g = ((argb shr 8 and 0xFF) + (255 - (argb shr 8 and 0xFF)) * amount).toInt().coerceIn(0, 255)
        val b = ((argb and 0xFF) + (255 - (argb and 0xFF)) * amount).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun Int.floorMod(m: Int): Int = ((this % m) + m) % m
}

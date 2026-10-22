package com.gexetr.coverkit.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class StickerKind {
    ARROW,
    RIBBON,
    FRAME,
}

/**
 * One editable layer on a cover canvas.
 *
 * [x]/[y] are normalised centres (0..1 of canvas width/height) so the same tree can
 * re-target another catalog size without stretching. [scale] multiplies the layer's base size;
 * [rotationDeg] is clockwise degrees.
 */
@Serializable
sealed class Layer {
    abstract val id: String
    abstract val x: Float
    abstract val y: Float
    abstract val scale: Float
    abstract val rotationDeg: Float
    abstract val zIndex: Int

    @Serializable
    @SerialName("photo")
    data class PhotoLayer(
        override val id: String,
        override val x: Float = 0.5f,
        override val y: Float = 0.5f,
        override val scale: Float = 1f,
        override val rotationDeg: Float = 0f,
        override val zIndex: Int = 0,
        /** Content URI string when the user picked a photo; null uses [colorArgb]. */
        val uriString: String? = null,
        /** Solid fill when no URI (template placeholder or missing decode). */
        val colorArgb: Int = 0xFF2B2D42.toInt(),
    ) : Layer()

    @Serializable
    @SerialName("text")
    data class TextLayer(
        override val id: String,
        override val x: Float = 0.5f,
        override val y: Float = 0.5f,
        override val scale: Float = 1f,
        override val rotationDeg: Float = 0f,
        override val zIndex: Int = 1,
        val text: String,
        val fillArgb: Int = 0xFFFFFFFF.toInt(),
        val strokeArgb: Int = 0xFF000000.toInt(),
        val strokeWidthPx: Float = 4f,
    ) : Layer()

    @Serializable
    @SerialName("sticker")
    data class StickerLayer(
        override val id: String,
        override val x: Float = 0.5f,
        override val y: Float = 0.5f,
        override val scale: Float = 1f,
        override val rotationDeg: Float = 0f,
        override val zIndex: Int = 2,
        val kind: StickerKind,
        val tintArgb: Int = 0xFFFF6B35.toInt(),
    ) : Layer()
}

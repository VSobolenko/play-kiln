package com.gexetr.plinthproductphotos.ui.editor

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.data.StudioDefaults
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalCompositionSessionTest {
    @Test
    fun storedDefaultsInitializeEveryCompositionControl() {
        val defaults = StudioDefaults(
            background = 2, shadow = 2, marginPct = 0.1f, fillTarget = 0.7f,
            itemScale = 1.2f, horizontalPosition = -0.08f, verticalPosition = 0.09f,
        )
        val initial = defaults.toCompositionSettings(0xffe0c8b9.toInt())
        val session = LocalCompositionSession(
            source = PixelBuffer.solid(8, 8, PixelBuffer.rgba(80, 90, 100)),
            mask = Mask(ByteArray(64) { 0xff.toByte() }),
            canvas = Canvas(12, 12),
            initial = initial,
        )

        assertEquals(0xffe0c8b9.toInt(), session.settings.background.colorArgb)
        assertEquals(ShadowSpec.Kind.DROP, session.settings.shadow.kind)
        assertEquals(defaults.marginPct, session.settings.marginPct)
        assertEquals(defaults.fillTarget, session.settings.fillTarget)
        assertEquals(defaults.itemScale, session.settings.itemScale)
        assertEquals(defaults.horizontalPosition, session.settings.horizontalPosition)
        assertEquals(defaults.verticalPosition, session.settings.verticalPosition)
        assertEquals(false, session.canUndo)
    }

    @Test
    fun undoRestoresPreviousComposition() {
        val source = PixelBuffer.solid(8, 8, PixelBuffer.rgba(80, 90, 100))
        val session = LocalCompositionSession(
            source = source,
            mask = Mask(ByteArray(64) { 0xff.toByte() }),
            canvas = Canvas(12, 12),
            initial = CompositionSettings(
                background = solid(0xffffffff.toInt()),
                shadow = ShadowSpec(kind = ShadowSpec.Kind.NONE),
                marginPct = 0f,
                fillTarget = 0.5f,
            ),
        )
        val before = session.composition.image.pixels.copyOf()

        session.apply(session.settings.copy(background = solid(0xffe8e1d5.toInt())))
        assertEquals(false, before.contentEquals(session.composition.image.pixels))

        session.undo()

        assertArrayEquals(before, session.composition.image.pixels)
    }

    @Test
    fun pipelinePreservesEveryPixelInsideOpaqueMask() {
        val pixels = IntArray(64) { index ->
            PixelBuffer.rgba(index, index * 2, 255 - index)
        }
        val source = PixelBuffer(8, 8, pixels)
        val session = LocalCompositionSession(
            source = source,
            mask = Mask(ByteArray(64) { 0xff.toByte() }),
            canvas = Canvas(8, 8),
            initial = CompositionSettings(
                background = solid(0xffffffff.toInt()),
                shadow = ShadowSpec(kind = ShadowSpec.Kind.NONE),
                marginPct = 0f,
                fillTarget = 1f,
            ),
        )

        assertArrayEquals(source.pixels, session.composition.image.pixels)
    }

    private fun solid(color: Int) = BackgroundSpec(
        kind = BackgroundSpec.Kind.SOLID,
        colorArgb = color,
        color2Argb = null,
        angleDeg = null,
        sceneId = null,
        sceneAssetPath = null,
        prompt = null,
        seed = null,
    )
}

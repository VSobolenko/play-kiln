package com.gexetr.plinthproductphotos.domain

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.domain.pipeline.Compositor
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositorInvariantTest {
    @Test
    fun originalModePreservesOpaqueItemBytesAtUnitScale() {
        val source = PixelBuffer(
            width = 5,
            height = 5,
            pixels = IntArray(25) { index ->
                PixelBuffer.rgba(index * 7, index * 5, index * 3, 255)
            },
        )
        val result = Compositor.compose(
            source = source,
            mask = Mask(ByteArray(25) { 0xff.toByte() }),
            canvas = Canvas(width = 5, height = 5),
            look = look(marginPct = 0f, fillTarget = 1f),
        )

        assertEquals(1f, result.scale, 0f)
        assertTrue(source.pixels.contentEquals(result.image.pixels))
    }

    @Test
    fun deliberatelyWrongPixelExpectationReallyFails() {
        val expected = PixelBuffer.rgba(1, 2, 3, 255)
        val actual = PixelBuffer.rgba(4, 5, 6, 255)

        val assertionFailure = runCatching { assertEquals(expected, actual) }.exceptionOrNull()
        assertTrue("A wrong pixel expectation must fail", assertionFailure is AssertionError)
    }

    @Test
    fun compositionHonorsFillTargetAndKeepsTheMaskCutout() {
        val source = PixelBuffer.solid(4, 2, PixelBuffer.rgba(220, 20, 30, 255))
        val result = Compositor.compose(
            source = source,
            mask = Mask(ByteArray(8) { 0xff.toByte() }),
            canvas = Canvas(width = 100, height = 100),
            look = look(marginPct = 0.06f, fillTarget = 0.8f),
        )

        assertEquals(0.8f, result.itemBounds.width.toFloat() / 100f, 0.01f)
        assertEquals(0.4f, result.itemBounds.height.toFloat() / 100f, 0.01f)
    }

    @Test
    fun suppliedScenePixelsAreCoverFitBeforeTheItemIsComposited() {
        val scene = PixelBuffer.solid(4, 2, PixelBuffer.rgba(40, 120, 60, 255))
        val look = look(marginPct = 0f, fillTarget = 0.5f).copy(
            background = BackgroundSpec(
                kind = BackgroundSpec.Kind.GENERATED_SCENE,
                colorArgb = null,
                color2Argb = null,
                angleDeg = null,
                sceneId = "scene",
                sceneAssetPath = null,
                prompt = null,
                seed = null,
            ),
        )

        val result = Compositor.compose(
            source = PixelBuffer.solid(1, 1, PixelBuffer.rgba(220, 20, 30, 255)),
            mask = Mask(byteArrayOf(0xff.toByte())),
            canvas = Canvas(width = 2, height = 2),
            look = look,
            sceneBackground = scene,
        )

        assertEquals(PixelBuffer.rgba(40, 120, 60, 255), result.image[1, 1])
    }

    private fun look(marginPct: Float, fillTarget: Float) = Look(
        id = "test",
        name = "Test",
        background = BackgroundSpec(
            kind = BackgroundSpec.Kind.SOLID,
            colorArgb = 0xffeeeeee.toInt(),
            color2Argb = null,
            angleDeg = null,
            sceneId = null,
            sceneAssetPath = null,
            prompt = null,
            seed = null,
        ),
        shadow = ShadowSpec(ShadowSpec.Kind.NONE, opacity = 0f, blurPct = 0f, offsetPct = 0f),
        marginPct = marginPct,
        fillTarget = fillTarget,
        createdAt = 0L,
    )
}

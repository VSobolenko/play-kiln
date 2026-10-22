package com.gexetr.coverkit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JSON round-trip for [CoverProject] — no Context / ProjectStore.
 */
class ProjectCodecTest {

    @Test
    fun roundTripsPhotoTextAndStickerLayers() {
        val original = CoverProject(
            id = "proj-1",
            destinationId = "yt_thumb",
            name = "Roundtrip",
            updatedAtMs = 42L,
            layers = listOf(
                Layer.PhotoLayer(
                    id = "p1",
                    x = 0.5f,
                    y = 0.5f,
                    scale = 1f,
                    rotationDeg = 0f,
                    zIndex = 0,
                    uriString = null,
                    colorArgb = 0xFF112233.toInt(),
                ),
                Layer.TextLayer(
                    id = "t1",
                    x = 0.5f,
                    y = 0.4f,
                    scale = 1.1f,
                    rotationDeg = 5f,
                    zIndex = 1,
                    text = "Hello",
                    fillArgb = 0xFFFFFFFF.toInt(),
                    strokeArgb = 0xFF000000.toInt(),
                    strokeWidthPx = 3f,
                ),
                Layer.StickerLayer(
                    id = "s1",
                    x = 0.2f,
                    y = 0.2f,
                    scale = 0.8f,
                    rotationDeg = -10f,
                    zIndex = 2,
                    kind = StickerKind.RIBBON,
                    tintArgb = 0xFFFF6600.toInt(),
                ),
            ),
        )

        val encoded = ProjectCodec.encode(original)
        assertTrue(encoded.contains("\"type\":\"photo\"") || encoded.contains("\"type\": \"photo\""))
        val decoded = ProjectCodec.decode(encoded)

        assertEquals(original.id, decoded.id)
        assertEquals(original.destinationId, decoded.destinationId)
        assertEquals(original.name, decoded.name)
        assertEquals(original.updatedAtMs, decoded.updatedAtMs)
        assertEquals(3, decoded.layers.size)

        val photo = decoded.layers[0] as Layer.PhotoLayer
        assertEquals(0xFF112233.toInt(), photo.colorArgb)
        assertEquals(null, photo.uriString)

        val text = decoded.layers[1] as Layer.TextLayer
        assertEquals("Hello", text.text)
        assertEquals(3f, text.strokeWidthPx, 0.001f)

        val sticker = decoded.layers[2] as Layer.StickerLayer
        assertEquals(StickerKind.RIBBON, sticker.kind)
    }

    @Test
    fun templateFactoryProjectsRoundTrip() {
        val project = TemplateFactory.createProject("ig_story_story", projectId = "tpl", nowMs = 9L)
        val again = ProjectCodec.decode(ProjectCodec.encode(project))
        assertEquals(project, again)
        assertTrue(again.layers.any { it is Layer.PhotoLayer })
        assertTrue(again.layers.any { it is Layer.TextLayer })
    }
}

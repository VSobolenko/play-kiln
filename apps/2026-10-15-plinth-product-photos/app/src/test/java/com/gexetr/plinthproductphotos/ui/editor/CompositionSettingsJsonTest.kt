package com.gexetr.plinthproductphotos.ui.editor

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class CompositionSettingsJsonTest {
    @Test
    fun `round trip preserves every composition setting`() {
        val settings = CompositionSettings(
            background = BackgroundSpec(
                kind = BackgroundSpec.Kind.SOLID,
                colorArgb = 0xff212223.toInt(),
                color2Argb = 0xfff0e0d0.toInt(),
                angleDeg = 37,
                sceneId = "studio-1",
                sceneAssetPath = "scenes/studio.png",
                prompt = "soft daylight",
                seed = 4_294_967_296L,
            ),
            shadow = ShadowSpec(ShadowSpec.Kind.DROP, 0.67f, 0.13f, -0.08f),
            marginPct = 0.11f,
            fillTarget = 0.73f,
            itemScale = 1.24f,
            horizontalPosition = -0.19f,
            verticalPosition = 0.29f,
        )

        assertEquals(settings, compositionSettingsFromJson(settings.toJsonObject()))
    }

    @Test
    fun `null source returns null`() {
        assertNull(compositionSettingsFromJson(null))
    }

    @Test
    fun `malformed shadow and unknown background kinds return null`() {
        assertNull(compositionSettingsFromJson(Json.parseToJsonElement("{\"background\":{\"kind\":\"SOLID\"},\"shadow\":{\"kind\":\"NOPE\"}}").jsonObject))
        assertNull(compositionSettingsFromJson(Json.parseToJsonElement("{\"background\":{\"kind\":\"NOPE\"}}").jsonObject))
    }

    @Test
    fun `missing optional fields use composition defaults`() {
        val result = compositionSettingsFromJson(
            Json.parseToJsonElement("{\"background\":{\"kind\":\"SOLID\"}}").jsonObject,
        )

        assertEquals(CompositionSettings(background = BackgroundSpec(BackgroundSpec.Kind.SOLID, null, null, null, null, null, null, null)), result)
    }
}

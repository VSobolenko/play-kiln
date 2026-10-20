package com.gexetr.plinthproductphotos.domain

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelDefaultsTest {
    @Test
    fun lookUsesAnnexMarginAndFillDefaults() {
        val look = Look(
            id = "look",
            name = "Look",
            background = BackgroundSpec(
                kind = BackgroundSpec.Kind.SOLID,
                colorArgb = 0xffffffff.toInt(),
                color2Argb = null,
                angleDeg = null,
                sceneId = null,
                sceneAssetPath = null,
                prompt = null,
                seed = null,
            ),
            shadow = ShadowSpec(ShadowSpec.Kind.CONTACT, 0.35f, 0.02f, 0.010f),
            createdAt = 0L,
        )

        assertEquals(0.06f, look.marginPct, 0f)
        assertEquals(0.80f, look.fillTarget, 0f)
    }

    @Test
    fun shadowUsesAnnexDefaultSettings() {
        val shadow = ShadowSpec()

        assertEquals(ShadowSpec.Kind.CONTACT, shadow.kind)
        assertEquals(0.35f, shadow.opacity, 0f)
        assertEquals(0.02f, shadow.blurPct, 0f)
        assertEquals(0.010f, shadow.offsetPct, 0f)
    }
}

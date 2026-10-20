package com.gexetr.plinthproductphotos.ui.autocard

import com.gexetr.plinthproductphotos.domain.autocard.AutoCardFallbackReason
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardFlowState
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardSceneCatalog
import com.gexetr.plinthproductphotos.domain.autocard.AutoCategory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoCardCategoryHandoffTest {
    @Test
    fun tappedCategoriesSelectDifferentFirstScenes() {
        val catalog = AutoCardSceneCatalog.fromJson(
            File("src/main/assets/autocard/scene-recipes.json").readText(),
        )
        val shoes = initialAutoCardFlow(AutoCategory.SHOES, isOffline = false)
        val jewelry = initialAutoCardFlow(AutoCategory.JEWELRY, isOffline = false)

        assertEquals(AutoCategory.SHOES, shoes.category)
        assertEquals(AutoCategory.JEWELRY, jewelry.category)
        assertEquals(catalog.recipesFor(AutoCategory.SHOES).first(), shoes.recipes(catalog).first())
        assertEquals(catalog.recipesFor(AutoCategory.JEWELRY).first(), jewelry.recipes(catalog).first())
        assertNotEquals(shoes.recipes(catalog).first().sceneId, jewelry.recipes(catalog).first().sceneId)
        assertNull(shoes.recognition)
        assertFalse(shoes.isSuggested)
    }

    @Test
    fun everyExplicitCategoryWinsOnlineAndOffline() {
        AutoCategory.entries.forEach { category ->
            listOf(false, true).forEach { offline ->
                val state = initialAutoCardFlow(category, isOffline = offline)
                assertEquals(category, state.category)
                assertFalse(state.isRecognizing)
                assertEquals(
                    if (offline) AutoCardFallbackReason.OFFLINE else null,
                    state.fallbackReason,
                )
            }
        }
    }

    @Test
    fun nullKeepsTheExistingOnlineDefault() {
        assertEquals(AutoCardFlowState(), initialAutoCardFlow(null, isOffline = false))
    }

    @Test
    fun nullKeepsTheExistingOfflineDefault() {
        assertEquals(AutoCardFlowState.offline(), initialAutoCardFlow(null, isOffline = true))
    }
}

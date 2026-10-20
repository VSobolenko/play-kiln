package com.gexetr.plinthproductphotos.domain.autocard

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCardFlowTest {
    @Test
    fun unusableRecognitionDoesNotInventOtherCategory() {
        val state = AutoCardFlowState.recognized(AutoCardRecognition.fallback())

        assertNull(state.category)
        assertNull(state.recognition)
        assertEquals(AutoCardFallbackReason.RECOGNITION_UNAVAILABLE, state.fallbackReason)
    }

    @Test
    fun recognizedCategorySelectsItsRecipes() {
        val state = AutoCardFlowState.recognized(recognition(AutoCategory.BAG))

        assertEquals(listOf("bag-scene"), state.recipes(catalog()).map { it.sceneId })
    }

    @Test
    fun recognitionFailureFallsBackToManualSelectionWithReason() {
        val state = AutoCardFlowState.failed(AutoCardFallbackReason.RECOGNITION_UNAVAILABLE)

        assertNull(state.category)
        assertEquals(AutoCardFallbackReason.RECOGNITION_UNAVAILABLE, state.fallbackReason)
        assertTrue(state.recipes(catalog()).isEmpty())

        val manual = state.chooseCategory(AutoCategory.BAG)
        assertEquals(AutoCategory.BAG, manual.category)
        assertEquals(listOf("bag-scene"), manual.recipes(catalog()).map { it.sceneId })
    }

    @Test
    fun offlineStateNeverInventsARecognizedCategory() {
        val state = AutoCardFlowState.offline()

        assertNull(state.category)
        assertNull(state.recognition)
        assertEquals(AutoCardFallbackReason.OFFLINE, state.fallbackReason)
    }

    @Test
    fun sellerEditOverridesSuggestedCategoryAndAttributes() {
        val edited = AutoCardFlowState.recognized(recognition(AutoCategory.BAG))
            .chooseCategory(AutoCategory.SHOES)
            .editAttributes(listOf("canvas"))

        assertEquals(AutoCategory.SHOES, edited.category)
        assertEquals(listOf("canvas"), edited.attributes)
        assertFalse(edited.isSuggested)
        assertEquals(listOf("shoes-scene"), edited.recipes(catalog()).map { it.sceneId })
    }

    private fun recognition(category: AutoCategory) = AutoCardRecognition(
        category = category,
        confidence = 0.91f,
        attributes = listOf("leather"),
        visibleText = emptyList(),
        scaleKind = AutoCardScaleKind.NONE,
        scaleValueMm = null,
        sku = null,
    )

    private fun catalog(): AutoCardSceneCatalog = AutoCardSceneCatalog.fromEntries(
        AutoCategory.entries.map { category ->
            val recipes = when (category) {
                AutoCategory.BAG -> listOf(recipe("bag-scene"))
                AutoCategory.SHOES -> listOf(recipe("shoes-scene"))
                else -> emptyList()
            }
            AutoCardCategoryEntry(category, verified = recipes.isNotEmpty(), recipes = recipes)
        },
    )

    private fun recipe(sceneId: String) = AutoCardSceneRecipe(
        sceneId = sceneId,
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
        shadow = ShadowSpec(),
        fillTarget = 0.8f,
        source = "fixture",
    )
}

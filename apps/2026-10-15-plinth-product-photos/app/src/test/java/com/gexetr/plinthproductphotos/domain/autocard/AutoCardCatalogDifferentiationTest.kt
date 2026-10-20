package com.gexetr.plinthproductphotos.domain.autocard

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCardCatalogDifferentiationTest {
    @Test
    fun bundledCatalogHasVerifiedRenderedDistinctRecipesForEveryCategory() {
        val catalog = AutoCardSceneCatalog.fromJson(
            File("src/main/assets/autocard/scene-recipes.json").readText(),
        )

        assertEquals(21, AutoCategory.entries.size)
        assertEquals(AutoCategory.entries.toSet(), catalog.entries.keys)
        AutoCategory.entries.forEach { category ->
            assertTrue("${category.id} must be verified", catalog.isVerified(category))
            assertTrue(
                "${category.id} must resolve to at least one recipe",
                catalog.recipesFor(category).isNotEmpty(),
            )
        }

        val recipes = AutoCategory.entries.associateWith { category ->
            catalog.recipesFor(category).first()
        }
        val renderedSignatures = recipes.values
            .map { recipe ->
                listOf(
                    recipe.background.colorArgb,
                    recipe.background.color2Argb,
                    recipe.shadow.kind,
                    recipe.shadow.opacity,
                )
            }
            .toSet()
        assertTrue(
            "Categories must have at least eight rendered background/shadow signatures",
            renderedSignatures.size >= 8,
        )

        val shadowSignatures = recipes.values
            .map { recipe -> recipe.shadow.kind to recipe.shadow.opacity }
            .toSet()
        assertTrue("Categories must use at least three shadow signatures", shadowSignatures.size >= 3)

        assertNotEquals(
            "Jewelry should not use the food category's neutral paper field",
            recipes.getValue(AutoCategory.JEWELRY).background.colorArgb,
            recipes.getValue(AutoCategory.PACKAGED_FOOD).background.colorArgb,
        )
        assertNotEquals(
            "Cosmetics should not use the food category's neutral paper field",
            recipes.getValue(AutoCategory.COSMETICS).background.colorArgb,
            recipes.getValue(AutoCategory.PACKAGED_FOOD).background.colorArgb,
        )
    }
}

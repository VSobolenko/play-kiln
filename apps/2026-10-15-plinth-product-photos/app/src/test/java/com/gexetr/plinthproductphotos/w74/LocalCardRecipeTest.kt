package com.gexetr.plinthproductphotos.w74

import com.gexetr.plinthproductphotos.domain.autocard.AutoCardFlowState
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardSceneCatalog
import com.gexetr.plinthproductphotos.domain.autocard.AutoCategory
import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCardRecipeTest {
    @Test
    fun localCardChoicesNeverOfferAnUnrequestedGenerativeRecipe() {
        val catalog = AutoCardSceneCatalog.fromJson(
            File("src/main/assets/autocard/scene-recipes.json").readText(),
        )
        for (category in AutoCategory.entries) {
            val choices = AutoCardFlowState().chooseCategory(category).recipes(catalog)
            assertTrue("Local choices missing for $category", choices.isNotEmpty())
            assertFalse(
                "Generated recipes must use the server scene tool, not local composition: $category",
                choices.any { it.background.kind == BackgroundSpec.Kind.GENERATED_SCENE },
            )
        }
    }
}

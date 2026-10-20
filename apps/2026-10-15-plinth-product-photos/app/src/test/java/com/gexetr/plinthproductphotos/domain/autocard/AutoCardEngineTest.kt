package com.gexetr.plinthproductphotos.domain.autocard

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.ImageRules
import com.gexetr.plinthproductphotos.domain.presets.ListingRules
import com.gexetr.plinthproductphotos.domain.presets.MainRules
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.PresetStatus
import com.gexetr.plinthproductphotos.domain.presets.TextRules
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCardEngineTest {
    @Test
    fun differentCategoriesComposeDifferentScenesFromTheirRecipes() {
        val catalog = catalog(
            AutoCategory.CLOTHING_TOP to recipe("fixture-top", 0xff112233.toInt()),
            AutoCategory.SHOES to recipe("fixture-shoes", 0xff445566.toInt()),
        )
        val source = source()

        val top = AutoCardEngine.compose(input(source, AutoCategory.CLOTHING_TOP, catalog))
        val shoes = AutoCardEngine.compose(input(source, AutoCategory.SHOES, catalog))

        assertTrue(top is AutoCardResult.Composed)
        assertTrue(shoes is AutoCardResult.Composed)
        assertEquals("fixture-top", (top as AutoCardResult.Composed).recipe.sceneId)
        assertEquals("fixture-shoes", (shoes as AutoCardResult.Composed).recipe.sceneId)
        assertNotSame(
            top.composition.image,
            shoes.composition.image,
        )
        assertFalse(top.composition.image[0, 0] == shoes.composition.image[0, 0])
    }

    @Test
    fun bundledCatalogProvidesSubjectSpecificProceduralScenesForEveryCategory() {
        val catalog = AutoCardSceneCatalog.fromJson(
            File("src/main/assets/autocard/scene-recipes.json").readText(),
        )

        assertEquals(AutoCategory.entries.toSet(), catalog.entries.keys)
        AutoCategory.entries.forEach { category ->
            val recipes = catalog.recipesFor(category)
            assertTrue("${category.id} must have a procedural recipe", recipes.isNotEmpty())
            assertEquals(
                "${category.id} must use a procedural scene first",
                BackgroundSpec.Kind.PROCEDURAL_SCENE,
                recipes.first().background.kind,
            )
            assertTrue("${category.id} recipe needs a source", recipes.first().source.isNotBlank())
            assertTrue("${category.id} recipe needs notes", recipes.first().notes.isNotEmpty())
            assertEquals(
                "${category.id} needs three generated scene prompts",
                3,
                recipes.count { it.background.kind == BackgroundSpec.Kind.GENERATED_SCENE },
            )
            assertTrue(
                "${category.id} generated prompts must not mention the item",
                recipes.filter { it.background.kind == BackgroundSpec.Kind.GENERATED_SCENE }
                    .all { recipe ->
                        listOf("item", "product", "garment", "shoe", "bag", "book", "watch", "bottle")
                            .none { word -> recipe.background.prompt.orEmpty().contains(word, ignoreCase = true) }
                    },
            )
        }
    }

    @Test
    fun changingRecognisedCategoryKeepsConfirmationAndClearsSceneChoice() {
        val selection = AutoCardSelection(
            category = AutoCategory.BAG,
            categoryConfirmed = true,
            marketplaceId = "etsy",
            selectedSceneId = "bag-podium",
        )

        val edited = selection.chooseCategory(AutoCategory.SHOES)

        assertEquals(AutoCategory.SHOES, edited.category)
        assertNull(edited.selectedSceneId)
        assertTrue(edited.categoryConfirmed)
    }

    @Test
    fun recognitionConfirmsCategoryAndOneTapComposesWithoutSellerConfirmation() {
        val catalog = catalog(
            AutoCategory.BAG to recipe("fixture-bag", 0xff112233.toInt()),
        )
        val selection = AutoCardSelection(
            category = AutoCategory.OTHER,
            categoryConfirmed = false,
            marketplaceId = "etsy",
        ).applyRecognition(
            AutoCardRecognition.fallback().copy(
                category = AutoCategory.BAG,
                confidence = 0.93f,
            ),
        )

        val result = AutoCardEngine.compose(
            input(source(), selection.category, catalog).copy(
                categoryConfirmed = selection.categoryConfirmed,
                selectedSceneId = selection.selectedSceneId,
            ),
        )

        assertTrue(selection.categoryConfirmed)
        assertTrue(result is AutoCardResult.Composed)
        assertEquals("fixture-bag", (result as AutoCardResult.Composed).recipe.sceneId)
    }

    @Test
    fun editingRecognisedCategoryUsesNewCategoryRecipeInsteadOfPreviousScene() {
        val catalog = catalog(
            AutoCategory.BAG to recipe("fixture-bag", 0xff112233.toInt()),
            AutoCategory.SHOES to recipe("fixture-shoes", 0xff445566.toInt()),
        )
        val recognized = AutoCardSelection(
            category = AutoCategory.BAG,
            categoryConfirmed = true,
            marketplaceId = "etsy",
            selectedSceneId = "fixture-bag",
        )
        val edited = recognized.chooseCategory(AutoCategory.SHOES)
        val result = AutoCardEngine.compose(
            input(source(), edited.category, catalog).copy(
                categoryConfirmed = edited.categoryConfirmed,
                selectedSceneId = edited.selectedSceneId,
            ),
        )

        assertTrue(edited.categoryConfirmed)
        assertNull(edited.selectedSceneId)
        assertTrue(result is AutoCardResult.Composed)
        assertEquals("fixture-shoes", (result as AutoCardResult.Composed).recipe.sceneId)
    }

    @Test
    fun bundledCategoriesRenderDifferentProceduralScenes() {
        val catalog = AutoCardSceneCatalog.fromJson(
            File("src/main/assets/autocard/scene-recipes.json").readText(),
        )

        val top = AutoCardEngine.compose(input(source(), AutoCategory.CLOTHING_TOP, catalog))
        val shoes = AutoCardEngine.compose(input(source(), AutoCategory.SHOES, catalog))

        assertTrue(top is AutoCardResult.Composed)
        assertTrue(shoes is AutoCardResult.Composed)
        assertNotEquals(
            (top as AutoCardResult.Composed).recipe.sceneId,
            (shoes as AutoCardResult.Composed).recipe.sceneId,
        )
        assertNotEquals(top.composition.image.pixels.toList(), shoes.composition.image.pixels.toList())
    }

    @Test
    fun categoryRecipeAppliesItsBackgroundAndShadow() {
        val expectedShadow = ShadowSpec(
            kind = ShadowSpec.Kind.DROP,
            opacity = 0.25f,
            blurPct = 0.015f,
            offsetPct = 0.02f,
        )
        val catalog = catalog(
            AutoCategory.BOOK to recipe("fixture-book", 0xff112233.toInt()).copy(shadow = expectedShadow),
        )

        val result = AutoCardEngine.compose(input(source(), AutoCategory.BOOK, catalog))

        assertTrue(result is AutoCardResult.Composed)
        val composed = result as AutoCardResult.Composed
        assertEquals(expectedShadow, composed.recipe.shadow)
        assertEquals(PixelBuffer.rgba(17, 34, 51), composed.composition.image[0, 0])
    }

    @Test
    fun solidSourcedRecipeFromCatalogueJsonIsComposed() {
        val rows = AutoCategory.entries.joinToString(",") { category ->
            val recipes = if (category == AutoCategory.BOOK) {
                """[{"sceneId":"book-paper","kind":"SOLID","color":"#E8E1D5","shadow":{"kind":"NONE","opacity":0,"blurPct":0,"offsetPct":0},"fillTarget":0.8,"marginPct":0.06,"source":"studio.palette.paper"}]"""
            } else {
                "[]"
            }
            val status = if (category == AutoCategory.BOOK) "VERIFIED" else "UNVERIFIED"
            """{"id":"${category.id}","status":"$status","recipes":$recipes}"""
        }
        val catalog = AutoCardSceneCatalog.fromJson("""{"categories":[$rows]}""")

        val result = AutoCardEngine.compose(input(source(), AutoCategory.BOOK, catalog))

        assertTrue(result is AutoCardResult.Composed)
        val composed = result as AutoCardResult.Composed
        assertEquals(0xffe8e1d5.toInt(), composed.recipe.background.colorArgb)
        assertEquals("studio.palette.paper", composed.recipe.source)
    }

    @Test
    fun whiteMainMarketplaceOverrideWinsOverTheCategoryRecipe() {
        val categoryRecipe = recipe("fixture-book", 0xff112233.toInt())
        val white = AutoCardWhiteMainOverride(
            sceneId = "white-255",
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
            source = "M-amazon.main.backgroundPolicy=white-255; L07",
        )
        val catalog = catalogWithOverride(
            white,
            AutoCategory.BOOK to categoryRecipe,
        )
        val marketplace = preset(EditPolicy.FREE).copy(
            main = MainRules(
                backgroundPolicy = "white-255",
                minFillRatio = 0.85,
                textOnMain = null,
                originalOnlyAllowed = emptySet(),
                raw = kotlinx.serialization.json.JsonObject(emptyMap()),
            ),
        )

        val result = AutoCardEngine.compose(
            input(source(), AutoCategory.BOOK, catalog, marketplace).copy(
                canvas = Canvas(20, 20),
            ),
        )

        assertTrue(result is AutoCardResult.Composed)
        val composed = result as AutoCardResult.Composed
        assertEquals("white-255", composed.recipe.sceneId)
        assertEquals(0xffffffff.toInt(), composed.recipe.background.colorArgb)
        assertEquals(PixelBuffer.rgba(255, 255, 255), composed.composition.image[0, 0])
    }

    @Test
    fun originalOnlyMarketplaceReturnsTheUnalteredPhoto() {
        val source = source()
        val input = input(
            source = source,
            category = null,
            catalog = catalog(),
            preset = preset(EditPolicy.ORIGINAL_ONLY),
        )

        val result = AutoCardEngine.compose(input)

        assertTrue(result is AutoCardResult.UnalteredPhoto)
        assertSame(source, (result as AutoCardResult.UnalteredPhoto).photo)
    }

    @Test
    fun marketplaceThatForbidsOverlayDoesNotApplySlide() {
        val policyPreset = preset(EditPolicy.LIMITED_OVERLAY)
        val input = input(source(), AutoCategory.DRESS, catalog(), policyPreset).copy(requestedOverlayId = "promo")
        assertTrue(AutoCardEngine.compose(input) is AutoCardResult.OverlayForbidden)
        assertFalse(AutoCardEngine.canApplySlide(policyPreset, "promo"))
        assertFalse(AutoCardEngine.canApplySlide(preset(EditPolicy.ORIGINAL_ONLY), "promo"))
        assertTrue(AutoCardEngine.canApplySlide(preset(EditPolicy.FREE), "promo"))
    }

    @Test
    fun selectedSceneSurvivesMarketplaceSwitch() {
        val selection = AutoCardSelection(
            category = AutoCategory.BAG,
            categoryConfirmed = true,
            marketplaceId = "etsy",
            selectedSceneId = "fixture-bag-scene",
        )

        val switched = selection.withMarketplace("amazon")

        assertEquals("amazon", switched.marketplaceId)
        assertEquals("fixture-bag-scene", switched.selectedSceneId)
        assertEquals(AutoCategory.BAG, switched.category)
    }

    @Test
    fun categorySuggestionUsesLastUsedOrExplicitDefaultAndNeedsConfirmation() {
        val lastUsed = AutoCardEngine.suggest(AutoCategory.WATCH)
        val firstUse = AutoCardEngine.suggest(null)

        assertEquals(AutoCategory.WATCH, lastUsed.category)
        assertFalse(lastUsed.categoryConfirmed)
        assertEquals(AutoCategory.OTHER, firstUse.category)
        assertFalse(firstUse.categoryConfirmed)
    }

    @Test
    fun sceneIdsComeFromTheCategoryTableNotEngineDefaults() {
        val catalog = catalog(
            AutoCategory.BOOK to recipe("fixture-book", 0xff778899.toInt()),
        )

        val result = AutoCardEngine.compose(input(source(), AutoCategory.BOOK, catalog))

        assertTrue(result is AutoCardResult.Composed)
        assertEquals("fixture-book", (result as AutoCardResult.Composed).recipe.sceneId)
        assertTrue(catalog.recipesFor(AutoCategory.FURNITURE_SMALL).isEmpty())
    }

    @Test
    fun categoryWithoutRecipeReturnsSceneUnavailable() {
        val category = AutoCategory.FURNITURE_SMALL

        val result = AutoCardEngine.compose(input(source(), category, catalog()))

        assertEquals(AutoCardResult.SceneUnavailable(category), result)
    }

    private fun input(
        source: PixelBuffer,
        category: AutoCategory?,
        catalog: AutoCardSceneCatalog,
        preset: MarketplacePreset = preset(EditPolicy.FREE),
    ) = AutoCardInput(
        photo = source,
        mask = Mask(ByteArray(9).also { it[4] = 0xff.toByte() }),
        canvas = Canvas(3, 3),
        category = category,
        categoryConfirmed = category != null,
        preset = preset,
        catalog = catalog,
    )

    private fun source() = PixelBuffer.solid(3, 3, PixelBuffer.rgba(240, 240, 240))

    private fun recipe(id: String, color: Int) = AutoCardSceneRecipe(
        sceneId = id,
        background = BackgroundSpec(
            kind = BackgroundSpec.Kind.SOLID,
            colorArgb = color,
            color2Argb = null,
            angleDeg = null,
            sceneId = null,
            sceneAssetPath = null,
            prompt = null,
            seed = null,
        ),
        shadow = ShadowSpec(kind = ShadowSpec.Kind.NONE),
        fillTarget = 0.1f,
        marginPct = 0f,
        notes = emptyList(),
        source = "fixture.palette.paper",
    )

    private fun catalog(vararg entries: Pair<AutoCategory, AutoCardSceneRecipe>) =
        AutoCardSceneCatalog.fromEntries(
            AutoCategory.entries.map { category ->
                AutoCardCategoryEntry(
                    category = category,
                    verified = entries.any { it.first == category },
                    recipes = entries.filter { it.first == category }.map { it.second },
                )
            },
        )

    private fun catalogWithOverride(
        override: AutoCardWhiteMainOverride,
        vararg entries: Pair<AutoCategory, AutoCardSceneRecipe>,
    ) = AutoCardSceneCatalog.fromEntries(
        AutoCategory.entries.map { category ->
            AutoCardCategoryEntry(
                category = category,
                verified = entries.any { it.first == category },
                recipes = entries.filter { it.first == category }.map { it.second },
            )
        },
        whiteMainOverride = override,
    )

    private fun preset(policy: EditPolicy) = MarketplacePreset(
        id = policy.wireValue,
        name = policy.wireValue,
        regions = emptyList(),
        defaultLocale = "en-US",
        queue = 1,
        editPolicy = policy,
        allowedOverlays = if (policy == EditPolicy.LIMITED_OVERLAY) setOf("other") else null,
        status = PresetStatus.FIRST_PARTY,
        verifiedAt = "2026-10-15",
        sourceUrl = "https://example.invalid",
        sources = emptyList(),
        image = ImageRules(formats = emptySet(), raw = kotlinx.serialization.json.JsonObject(emptyMap())),
        main = MainRules(
            backgroundPolicy = null,
            minFillRatio = null,
            textOnMain = null,
            originalOnlyAllowed = emptySet(),
            raw = kotlinx.serialization.json.JsonObject(emptyMap()),
        ),
        text = TextRules(
            prohibited = emptySet(),
            contrastMin = null,
            avoidColourPairs = emptyList(),
            raw = kotlinx.serialization.json.JsonObject(emptyMap()),
        ),
        listing = ListingRules(
            titleMax = null,
            descriptionMax = null,
            raw = kotlinx.serialization.json.JsonObject(emptyMap()),
        ),
        notes = emptyList(),
        raw = kotlinx.serialization.json.JsonObject(emptyMap()),
    )
}

package com.gexetr.plinthproductphotos.domain.autocard

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.pipeline.CompositionResult
import com.gexetr.plinthproductphotos.domain.pipeline.Compositor
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

enum class AutoCategory(val id: String) {
    CLOTHING_TOP("clothing_top"),
    CLOTHING_BOTTOM("clothing_bottom"),
    DRESS("dress"),
    OUTERWEAR("outerwear"),
    SHOES("shoes"),
    BAG("bag"),
    ACCESSORY("accessory"),
    JEWELRY("jewelry"),
    WATCH("watch"),
    COSMETICS("cosmetics"),
    SKINCARE("skincare"),
    PACKAGED_FOOD("packaged_food"),
    BEVERAGE("beverage"),
    ELECTRONICS_SMALL("electronics_small"),
    HOME_DECOR("home_decor"),
    KITCHENWARE("kitchenware"),
    TOY("toy"),
    BOOK("book"),
    FURNITURE_SMALL("furniture_small"),
    TOOL("tool"),
    OTHER("other"),
    ;

    companion object {
        fun fromId(id: String): AutoCategory =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Unknown auto-card category: $id")
    }
}

data class AutoCardSceneRecipe(
    val sceneId: String,
    val background: BackgroundSpec,
    val shadow: ShadowSpec,
    val fillTarget: Float,
    val marginPct: Float? = null,
    val notes: List<String> = emptyList(),
    val source: String,
) {
    init {
        require(sceneId.isNotBlank()) { "Scene id must not be blank" }
        require(source.isNotBlank()) { "Scene recipe source must not be blank" }
        require(fillTarget.isFinite() && fillTarget > 0f) { "Fill target must be finite and positive" }
        require(marginPct == null || (marginPct.isFinite() && marginPct >= 0f)) {
            "Margin must be finite and non-negative"
        }
    }
}

data class AutoCardCategoryEntry(
    val category: AutoCategory,
    val verified: Boolean,
    val recipes: List<AutoCardSceneRecipe>,
)

data class AutoCardWhiteMainOverride(
    val sceneId: String,
    val background: BackgroundSpec,
    val source: String,
)

class AutoCardSceneCatalog private constructor(
    val entries: Map<AutoCategory, AutoCardCategoryEntry>,
    val whiteMainOverride: AutoCardWhiteMainOverride?,
) {
    fun recipesFor(category: AutoCategory): List<AutoCardSceneRecipe> =
        entries[category]?.takeIf { it.verified }?.recipes.orEmpty()

    fun isVerified(category: AutoCategory): Boolean = entries[category]?.verified == true

    companion object {
        fun fromEntries(
            entries: List<AutoCardCategoryEntry>,
            whiteMainOverride: AutoCardWhiteMainOverride? = null,
        ): AutoCardSceneCatalog {
            require(entries.map { it.category }.distinct().size == entries.size) {
                "Category scene table contains duplicate categories"
            }
            require(entries.map { it.category }.toSet() == AutoCategory.entries.toSet()) {
                "Category scene table must contain the complete category set"
            }
            return AutoCardSceneCatalog(entries.associateBy { it.category }, whiteMainOverride)
        }

        fun fromJson(json: String): AutoCardSceneCatalog {
            val root = Json.parseToJsonElement(json).jsonObject
            val categoryRows = root.getValue("categories").jsonArray
            val entries = categoryRows.map { parseCategory(it.jsonObject) }
            val overrides = root["marketplaceOverrides"]?.jsonObject
            val whiteMainOverride = overrides
                ?.get("white-255-main")
                ?.jsonObject
                ?.let(::parseWhiteMainOverride)
            return fromEntries(entries, whiteMainOverride)
        }

        private fun parseCategory(row: JsonObject): AutoCardCategoryEntry {
            val category = AutoCategory.fromId(row.requiredString("id"))
            val verified = when (val status = row.requiredString("status")) {
                "VERIFIED" -> true
                "UNVERIFIED" -> false
                else -> throw IllegalArgumentException("Unsupported category table status: $status")
            }
            val recipes = row.getValue("recipes").jsonArray.map { parseRecipe(it.jsonObject) }
            require(!verified || recipes.isNotEmpty()) {
                "Verified category ${category.id} must have at least one scene recipe"
            }
            return AutoCardCategoryEntry(category, verified, recipes)
        }

        private fun parseRecipe(row: JsonObject): AutoCardSceneRecipe {
            val kind = row.requiredString("kind")
            val background = when (kind) {
                "SOLID" -> BackgroundSpec(
                    kind = BackgroundSpec.Kind.SOLID,
                    colorArgb = row.requiredColor("color"),
                    color2Argb = null,
                    angleDeg = null,
                    sceneId = null,
                    sceneAssetPath = null,
                    prompt = null,
                    seed = null,
                )
                "GRADIENT" -> BackgroundSpec(
                    kind = BackgroundSpec.Kind.GRADIENT,
                    colorArgb = row.requiredColor("color"),
                    color2Argb = row.requiredColor("color2"),
                    angleDeg = row.requiredInt("angleDeg"),
                    sceneId = null,
                    sceneAssetPath = null,
                    prompt = null,
                    seed = null,
                )
                "PROCEDURAL" -> BackgroundSpec(
                    kind = BackgroundSpec.Kind.PROCEDURAL_SCENE,
                    colorArgb = row["color"]?.let { row.requiredColor("color") },
                    color2Argb = row["color2"]?.let { row.requiredColor("color2") },
                    angleDeg = null,
                    sceneId = row.requiredString("sceneId"),
                    sceneAssetPath = row["sceneAssetPath"]?.jsonPrimitive?.contentOrNull,
                    prompt = null,
                    seed = null,
                )
                "GENERATED" -> BackgroundSpec(
                    kind = BackgroundSpec.Kind.GENERATED_SCENE,
                    colorArgb = null,
                    color2Argb = null,
                    angleDeg = null,
                    sceneId = row.requiredString("sceneId"),
                    sceneAssetPath = row["sceneAssetPath"]?.jsonPrimitive?.contentOrNull,
                    prompt = row["promptTemplate"]?.jsonPrimitive?.contentOrNull
                        ?: throw IllegalArgumentException("Generated recipe needs promptTemplate"),
                    seed = null,
                )
                else -> throw IllegalArgumentException("Unsupported auto-card recipe kind: $kind")
            }
            val shadowRow = row.getValue("shadow").jsonObject
            val shadowKind = ShadowSpec.Kind.valueOf(shadowRow.requiredString("kind"))
            return AutoCardSceneRecipe(
                sceneId = row.requiredString("sceneId"),
                background = background,
                shadow = ShadowSpec(
                    kind = shadowKind,
                    opacity = shadowRow.requiredFloat("opacity"),
                    blurPct = shadowRow.requiredFloat("blurPct"),
                    offsetPct = shadowRow.requiredFloat("offsetPct"),
                ),
                fillTarget = row.requiredFloat("fillTarget"),
                marginPct = row["marginPct"]?.jsonPrimitive?.floatOrNull,
                notes = row["notes"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                source = row.requiredString("source"),
            )
        }

        private fun parseWhiteMainOverride(row: JsonObject): AutoCardWhiteMainOverride {
            require(row.requiredString("kind") == "SOLID") { "The white-255 main override must be solid" }
            return AutoCardWhiteMainOverride(
                sceneId = row.requiredString("sceneId"),
                background = BackgroundSpec(
                    kind = BackgroundSpec.Kind.SOLID,
                    colorArgb = row.requiredColor("color"),
                    color2Argb = null,
                    angleDeg = null,
                    sceneId = null,
                    sceneAssetPath = null,
                    prompt = null,
                    seed = null,
                ),
                source = row.requiredString("source"),
            )
        }

        private fun JsonObject.requiredString(key: String): String =
            getValue(key).jsonPrimitive.content

        private fun JsonObject.requiredFloat(key: String): Float =
            getValue(key).jsonPrimitive.floatOrNull
                ?: throw IllegalArgumentException("$key must be numeric")

        private fun JsonObject.requiredInt(key: String): Int =
            getValue(key).jsonPrimitive.content.toIntOrNull()
                ?: throw IllegalArgumentException("$key must be an integer")

        private fun JsonObject.requiredColor(key: String): Int {
            val color = requiredString(key).removePrefix("#")
            require(color.length == 6 || color.length == 8) { "$key must be RGB or ARGB hex" }
            val value = color.toLongOrNull(16)
                ?: throw IllegalArgumentException("$key must be RGB or ARGB hex")
            return if (color.length == 6) (0xff000000L or value).toInt() else value.toInt()
        }

    }
}

data class AutoCardSelection(
    val category: AutoCategory?,
    val categoryConfirmed: Boolean,
    val marketplaceId: String,
    val selectedSceneId: String? = null,
) {
    fun withMarketplace(id: String): AutoCardSelection = copy(marketplaceId = id)

    fun chooseCategory(value: AutoCategory): AutoCardSelection =
        copy(category = value, selectedSceneId = null)

    fun applyRecognition(recognition: AutoCardRecognition): AutoCardSelection =
        copy(
            category = recognition.category,
            categoryConfirmed = true,
            selectedSceneId = null,
        )

    fun confirmCategory(): AutoCardSelection =
        copy(categoryConfirmed = category != null)

    fun chooseScene(sceneId: String): AutoCardSelection =
        copy(selectedSceneId = sceneId)
}

data class AutoCardInput(
    val photo: PixelBuffer,
    val mask: Mask,
    val canvas: Canvas,
    val category: AutoCategory?,
    val categoryConfirmed: Boolean,
    val preset: MarketplacePreset,
    val catalog: AutoCardSceneCatalog,
    val slotRole: SlotRole = SlotRole.MAIN,
    val selectedSceneId: String? = null,
    val sceneBackground: PixelBuffer? = null,
    val sceneBackgrounds: Map<String, PixelBuffer> = emptyMap(),
    val requestedOverlayId: String? = null,
)

sealed interface AutoCardResult {
    data class UnalteredPhoto(val photo: PixelBuffer) : AutoCardResult
    data object ChooseCategory : AutoCardResult
    data class ConfirmCategory(val category: AutoCategory) : AutoCardResult
    data class SceneUnavailable(val category: AutoCategory) : AutoCardResult
    data class SceneAssetUnavailable(val sceneId: String) : AutoCardResult
    data class OverlayForbidden(val overlayId: String) : AutoCardResult
    data class Composed(
        val composition: CompositionResult,
        val recipe: AutoCardSceneRecipe,
    ) : AutoCardResult
}

object AutoCardEngine {
    fun generatedRecipes(
        category: AutoCategory?,
        catalog: AutoCardSceneCatalog,
    ): List<AutoCardSceneRecipe> = category?.let(catalog::recipesFor).orEmpty()
        .filter { it.background.kind == BackgroundSpec.Kind.GENERATED_SCENE }

    fun generationPrompt(
        state: AutoCardFlowState,
        catalog: AutoCardSceneCatalog,
        preset: MarketplacePreset?,
        selectedSceneId: String?,
        guidance: String,
    ): String? {
        if (preset?.editPolicy == EditPolicy.ORIGINAL_ONLY || (state.isRecognizing && guidance.isBlank())) return null
        val seller = guidance.trim()
        val category = state.category
        if (category == null && seller.isEmpty()) return null
        val recipe = category?.let { generatedRecipes(it, catalog) }
            ?.firstOrNull { selectedSceneId == null || it.sceneId == selectedSceneId }
        if (recipe == null && seller.isEmpty()) return null
        return buildString {
            if (category != null) {
                appendLine("Product category: ${category.id}. Preserve the original product and its details.")
            } else {
                appendLine("Preserve the original product and its details.")
            }
            if (state.attributes.isNotEmpty()) appendLine("Observed attributes: ${state.attributes.joinToString(", ")}.")
            state.recognition?.takeIf { !state.categoryEdited }?.let { recognition ->
                if (recognition.scaleKind != AutoCardScaleKind.NONE) {
                    appendLine("Observed scale reference: ${recognition.scaleKind.wireValue}; valueMm=${recognition.scaleValueMm ?: "unknown"}. Do not invent dimensions.")
                }
            }
            if (preset != null) appendLine("Marketplace: ${preset.id}; main background policy: ${preset.main.backgroundPolicy ?: "unspecified"}.")
            if (preset?.main?.backgroundPolicy == "white-255") {
                appendLine("Use a pure white (#FFFFFF) background.")
            } else if (recipe != null && (seller.isEmpty() || selectedSceneId != null)) {
                appendLine(requireNotNull(recipe.background.prompt))
            }
            if (recipe != null) {
                appendLine("Shadow: ${recipe.shadow.kind}; opacity=${recipe.shadow.opacity}; blur=${recipe.shadow.blurPct}; offset=${recipe.shadow.offsetPct}.")
                appendLine("Product fill target: ${preset?.main?.minFillRatio ?: recipe.fillTarget}; margin: ${recipe.marginPct ?: 0f}.")
            } else {
                appendLine("Soft natural contact shadow. Product fill target: ${preset?.main?.minFillRatio ?: 0.8}.")
            }
            if (seller.isNotEmpty()) append("Seller background idea: $seller")
        }.trim()
    }

    fun suggest(lastUsedCategory: AutoCategory?): AutoCardSelection =
        AutoCardSelection(
            category = lastUsedCategory ?: AutoCategory.OTHER,
            categoryConfirmed = false,
            marketplaceId = "",
        )

    fun compose(input: AutoCardInput): AutoCardResult {
        if (input.preset.editPolicy == EditPolicy.ORIGINAL_ONLY) {
            return AutoCardResult.UnalteredPhoto(input.photo)
        }
        input.requestedOverlayId?.let { overlayId ->
            if (!canApplySlide(input.preset, overlayId)) {
                return AutoCardResult.OverlayForbidden(overlayId)
            }
        }
        val category = input.category ?: return AutoCardResult.ChooseCategory
        if (!input.categoryConfirmed) return AutoCardResult.ConfirmCategory(category)

        val categoryRecipe = input.catalog.recipesFor(category)
            .firstOrNull { input.selectedSceneId == null || it.sceneId == input.selectedSceneId }
        val useWhiteMain = input.slotRole == SlotRole.MAIN &&
            input.preset.main.backgroundPolicy == "white-255"
        val recipe = if (useWhiteMain) {
            val override = input.catalog.whiteMainOverride
                ?: return AutoCardResult.SceneUnavailable(category)
            val fillTarget = input.preset.main.minFillRatio?.toFloat()
                ?: categoryRecipe?.fillTarget
                ?: return AutoCardResult.SceneUnavailable(category)
            AutoCardSceneRecipe(
                sceneId = override.sceneId,
                background = override.background,
                shadow = categoryRecipe?.shadow ?: ShadowSpec(),
                fillTarget = fillTarget,
                marginPct = categoryRecipe?.marginPct,
                source = override.source,
            )
        } else {
            categoryRecipe ?: return AutoCardResult.SceneUnavailable(category)
        }
        val look = Look(
            id = "auto-card-${category.id}",
            name = category.id,
            background = recipe.background,
            shadow = recipe.shadow,
            fillTarget = recipe.fillTarget,
            createdAt = 0L,
        )
        val effectiveLook = recipe.marginPct?.let { look.copy(marginPct = it) } ?: look
        val sceneBackground = input.sceneBackground
            ?: recipe.background.sceneId?.let(input.sceneBackgrounds::get)
            ?: if (recipe.background.kind == BackgroundSpec.Kind.PROCEDURAL_SCENE) {
                renderProceduralScene(recipe.background, input.canvas)
            } else {
                null
            }
        if (recipe.background.kind in setOf(
                BackgroundSpec.Kind.PROCEDURAL_SCENE,
                BackgroundSpec.Kind.GENERATED_SCENE,
            ) && sceneBackground == null
        ) {
            return AutoCardResult.SceneAssetUnavailable(
                recipe.background.sceneId ?: recipe.sceneId,
            )
        }
        val composition = Compositor.compose(
            source = input.photo,
            mask = input.mask,
            canvas = input.canvas,
            look = effectiveLook,
            sceneBackground = sceneBackground,
        )
        return AutoCardResult.Composed(composition, recipe)
    }

    fun canApplySlide(preset: MarketplacePreset, overlayId: String): Boolean =
        when (preset.editPolicy) {
            EditPolicy.ORIGINAL_ONLY -> false
            EditPolicy.LIMITED_OVERLAY -> preset.allowedOverlays?.contains(overlayId) == true
            EditPolicy.FREE -> true
        }

    private fun renderProceduralScene(
        background: BackgroundSpec,
        canvas: Canvas,
    ): PixelBuffer {
        val backdrop = requireNotNull(background.colorArgb) {
            "Procedural scene needs a backdrop color"
        }
        val light = background.color2Argb ?: backdrop
        val pixels = PixelBuffer.gradient(
            canvas.width,
            canvas.height,
            startArgb = backdrop,
            endArgb = light,
            angleDeg = background.angleDeg ?: 90,
        ).pixels
        val horizon = (canvas.height * 0.72f).roundToInt().coerceIn(0, canvas.height)
        val floor = blendArgb(backdrop, light, 0.35f)
        for (y in horizon until canvas.height) {
            val depth = (y - horizon + 1).toFloat() / (canvas.height - horizon + 1)
            val floorBlend = 0.34f + depth * 0.12f
            for (x in 0 until canvas.width) {
                val index = y * canvas.width + x
                pixels[index] = blendArgb(pixels[index], floor, floorBlend)
            }
        }
        return PixelBuffer(canvas.width, canvas.height, pixels)
    }

    private fun blendArgb(first: Int, second: Int, amount: Float): Int {
        val fraction = amount.coerceIn(0f, 1f)
        fun channel(shift: Int): Int =
            ((first ushr shift and 0xff) * (1f - fraction) +
                (second ushr shift and 0xff) * fraction).roundToInt()
        return (channel(24) shl 24) or
            (channel(16) shl 16) or
            (channel(8) shl 8) or
            channel(0)
    }
}

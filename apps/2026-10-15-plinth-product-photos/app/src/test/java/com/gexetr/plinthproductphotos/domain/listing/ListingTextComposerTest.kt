package com.gexetr.plinthproductphotos.domain.listing

import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePresetParser
import com.gexetr.plinthproductphotos.domain.presets.PresetParseResult
import java.io.File
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingTextComposerTest {
    @Test
    fun russianPreviewUsesRussianPhrasesWithoutEnglishFallback() {
        val result = ListingTextComposer.compose(
            marketplace = marketplace("wb"),
            localeTag = "ru-RU",
            attributes = ListingAttributes(
                itemName = "Кружка",
                brand = "Север",
                material = "сталь",
                size = "350 мл ".repeat(150),
                colour = "синий",
                quantity = "1",
            ),
        )

        assertTrue(result is ListingBuildResult.Ready)
        val text = result as ListingBuildResult.Ready
        assertTrue(text.shortDescription.contains("Сведения о товаре"))
        assertTrue(text.shortDescription.contains("Бренд"))
        assertTrue(text.shortDescription.contains("Материал"))
        assertFalse(text.shortDescription.contains("Brand"))
        assertFalse(text.shortDescription.contains("Material"))
    }

    @Test
    fun missingRequiredAttributesAreReturnedAsTypedState() {
        val result = ListingTextComposer.compose(
            marketplace = marketplace("wb"),
            localeTag = "en-US",
            attributes = ListingAttributes(itemName = " ", quantity = ""),
        )

        assertEquals(
            ListingBuildResult.MissingRequiredAttributes(
                setOf(RequiredListingAttribute.ITEM_NAME, RequiredListingAttribute.QUANTITY),
            ),
            result,
        )
    }

    @Test
    fun titleLimitPassesAtBoundaryAndRefusesOneMoreCharacterInEveryLanguage() {
        val preset = marketplace("wb")
        val limit = requireNotNull(preset.listing.titleMax)
        val samples = mapOf(
            "en-US" to "A",
            "ru-RU" to "Я",
            "de-DE" to "Ä",
            "fr-FR" to "É",
            "es-ES" to "Ñ",
            "pt-BR" to "Ã",
        )

        samples.forEach { (locale, character) ->
            val exact = ListingTextComposer.compose(
                preset,
                locale,
                ListingAttributes(
                    itemName = character.repeat(limit),
                    size = "x".repeat(descriptionMinimum(preset) + 10),
                    quantity = "1",
                ),
            )
            assertTrue("$locale should allow $limit characters", exact is ListingBuildResult.Ready)

            val overflow = ListingTextComposer.compose(
                preset,
                locale,
                ListingAttributes(
                    itemName = character.repeat(limit + 1),
                    size = "x".repeat(descriptionMinimum(preset) + 10),
                    quantity = "1",
                ),
            )
            assertEquals(
                ListingBuildResult.Overflow(ListingField.TITLE, limit + 1, limit),
                overflow,
            )
        }
    }

    @Test
    fun descriptionLimitPassesAtBoundaryAndRefusesOneMoreCharacterInEveryLanguage() {
        val preset = marketplace("wb")
        val limit = requireNotNull(preset.listing.descriptionMax)
        val minimum = descriptionMinimum(preset)
        val locales = listOf("en-US", "ru-RU", "de-DE", "fr-FR", "es-ES", "pt-BR")

        locales.forEach { locale ->
            val oneCharacter = ListingTextComposer.compose(
                preset,
                locale,
                ListingAttributes(itemName = "Item", size = "x".repeat(minimum + 10), quantity = "1"),
            ) as ListingBuildResult.Ready
            val oneCharacterCount = oneCharacter.shortDescription.codePointCount(
                0,
                oneCharacter.shortDescription.length,
            )
            val exactSize = "x".repeat(minimum + 10 + limit - oneCharacterCount)
            val exact = ListingTextComposer.compose(
                preset,
                locale,
                ListingAttributes(itemName = "Item", size = exactSize, quantity = "1"),
            )
            assertTrue("$locale should allow $limit description characters", exact is ListingBuildResult.Ready)

            val overflow = ListingTextComposer.compose(
                preset,
                locale,
                ListingAttributes(itemName = "Item", size = exactSize + "x", quantity = "1"),
            )
            assertEquals(
                ListingBuildResult.Overflow(ListingField.SHORT_DESCRIPTION, limit + 1, limit),
                overflow,
            )
        }
    }

    @Test
    fun everyMarketplaceUsesItsRecordedPresetLimitsOrShowsWhyTheyAreDisabled() {
        val presets = marketplaceDocument().marketplaces
        assertEquals(9, presets.size)

        presets.forEach { preset ->
            val titleLimit = preset.listing.titleMax
            val descriptionLimit = preset.listing.descriptionMax
            if (titleLimit == null || descriptionLimit == null) {
                val result = ListingTextComposer.compose(
                    preset,
                    "en-US",
                    ListingAttributes(itemName = "Item", quantity = "1"),
                )
                assertTrue(
                    "${preset.id} should visibly disable missing limits",
                    result is ListingBuildResult.LimitUnavailable,
                )
                val disabled = result as ListingBuildResult.LimitUnavailable
                assertEquals(preset.id, disabled.marketplaceId)
                assertEquals(
                    if (titleLimit == null) ListingField.TITLE else ListingField.SHORT_DESCRIPTION,
                    disabled.field,
                )
            } else {
                val changed = preset.copy(
                    listing = preset.listing.copy(
                        titleMax = 7,
                        descriptionMax = 23,
                        raw = JsonObject(preset.listing.raw.toMutableMap().apply {
                            put("descriptionMin", JsonNull)
                        }),
                    ),
                )
                val result = ListingTextComposer.compose(
                    changed,
                    "en-US",
                    ListingAttributes(itemName = "12345678", quantity = "1"),
                )
                assertEquals(ListingBuildResult.Overflow(ListingField.TITLE, 8, 7), result)

                val descriptionResult = ListingTextComposer.compose(
                    changed,
                    "en-US",
                    ListingAttributes(itemName = "Item", size = "x".repeat(100), quantity = "1"),
                )
                assertEquals(
                    ListingBuildResult.Overflow(ListingField.SHORT_DESCRIPTION, 137, 23),
                    descriptionResult,
                )
            }
        }
    }

    @Test
    fun unsupportedListingLanguageDoesNotFallBackToEnglish() {
        val result = ListingTextComposer.compose(
            marketplace("wb"),
            "it-IT",
            ListingAttributes(itemName = "Articolo", quantity = "1"),
        )

        assertEquals(ListingBuildResult.UnsupportedLanguage("it-IT"), result)
    }

    private fun marketplace(id: String): MarketplacePreset =
        marketplaceDocument().marketplaces.single { it.id == id }

    private fun descriptionMinimum(preset: MarketplacePreset): Int =
        requireNotNull((preset.listing.raw["descriptionMin"] as? JsonPrimitive)?.intOrNull)

    private fun marketplaceDocument() =
        MarketplacePresetParser.parseDocument(File("src/main/assets/marketplaces.json").readText())
            .let { result ->
                assertTrue(result is PresetParseResult.Success)
                (result as PresetParseResult.Success).document
            }
}

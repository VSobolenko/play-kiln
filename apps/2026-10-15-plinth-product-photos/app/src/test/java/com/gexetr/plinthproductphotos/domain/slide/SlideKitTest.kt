package com.gexetr.plinthproductphotos.domain.slide

import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideKitTest {
    @Test
    fun prohibitedTextFamiliesAreReportedInRussianAndEnglish() {
        val cases = mapOf(
            "price_discount" to mapOf(
                "ru" to listOf("Скидка 30%", "Цена 300 ₽"),
                "en" to listOf("Sale -30%", "Price $30"),
            ),
            "contacts_links" to mapOf(
                "ru" to listOf("Телефон +7 999 123-45-67"),
                "en" to listOf("mail a@example.com"),
            ),
            "evaluative_claim" to mapOf("ru" to listOf("Хит продаж"), "en" to listOf("Best seller")),
            "review_reward" to mapOf("ru" to listOf("Отзыв за подарок"), "en" to listOf("Review for a gift")),
            "sold_count" to mapOf("ru" to listOf("Продано 250"), "en" to listOf("Sold 250")),
            "call_to_action" to mapOf("ru" to listOf("Купите сейчас"), "en" to listOf("Buy now")),
            "delivery_returns" to mapOf("ru" to listOf("Бесплатная доставка"), "en" to listOf("Free shipping")),
        )

        for ((category, examples) in cases) {
            for ((language, texts) in examples) {
                for (text in texts) {
                    val check = SlideTextSafety.check(
                        texts = mapOf(SlideTextSlot.TITLE to text),
                        rules = rules(language, setOf(category)),
                    )

                    assertTrue("$language $category did not block '$text'", check.findings.any {
                        it.ruleId == "L11" && it.category == category && it.offendingSubstring.isNotBlank()
                    })
                }
            }
        }
    }

    @Test
    fun russianSoldCountsMatchWithoutPhraseFallbackAndPreserveOriginalText() {
        val marketplaceRules = rules("ru", setOf("sold_count")).copy(phraseRules = emptyMap())
        val examples = listOf("Продано 250", "Пpoдaнo 250", "250 шт. продано", "ПРОДАНО: 1 250")

        for (text in examples) {
            val check = SlideTextSafety.check(mapOf(SlideTextSlot.TITLE to text), marketplaceRules)

            assertTrue(text, !check.canRender)
            assertTrue(text, check.findings.any {
                it.ruleId == "L11" && it.category == "sold_count" && it.offendingSubstring == text
            })
        }
    }

    @Test
    fun russianSoldCountDoesNotBlockOrdinaryProseOrUnrelatedNumbers() {
        val marketplaceRules = rules("ru", setOf("sold_count")).copy(phraseRules = emptyMap())
        val examples = listOf("Продано изделие ручной работы", "Размер 250 мм", "Непродано 250")

        for (text in examples) {
            val check = SlideTextSafety.check(mapOf(SlideTextSlot.TITLE to text), marketplaceRules)

            assertTrue(text, check.findings.isEmpty())
            assertTrue(text, check.canRender)
        }
    }

    @Test
    fun cleanTextDoesNotMatchAndEmptyProhibitedListIsHonest() {
        val clean = SlideTextSafety.check(
            texts = mapOf(SlideTextSlot.TITLE to "Natural cotton fabric"),
            rules = rules("en", setOf("price_discount")),
        )
        val unread = SlideTextSafety.check(
            texts = mapOf(SlideTextSlot.TITLE to "Natural cotton fabric"),
            rules = rules("en", emptySet()),
        )

        assertTrue(clean.findings.isEmpty())
        assertTrue(unread.findings.isEmpty())
        assertTrue(unread.prohibitedListNotRead)
        assertTrue(unread.canRender)
    }

    @Test
    fun prohibitedHomoglyphsAreMatchedAndOriginalSubstringIsReported() {
        val check = SlideTextSafety.check(
            texts = mapOf(SlideTextSlot.TITLE to "Xит"), // Latin X in a Cyrillic word.
            rules = rules("ru", setOf("evaluative_claim")),
        )

        assertTrue(check.findings.any { it.category == "evaluative_claim" && it.offendingSubstring == "Xит" })
    }

    @Test
    fun textOverDeclaredMarketplaceLimitIsRefusedWithoutRewriting() {
        val original = "123456"
        val check = SlideTextSafety.check(
            texts = mapOf(SlideTextSlot.TITLE to original),
            rules = rules(
                language = "en",
                prohibited = emptySet(),
                limits = mapOf(SlideTextSlot.TITLE to SlideTextLimit(5, "marketplace.slide.title")),
            ),
        )

        assertTrue(!check.canRender)
        assertEquals(original, check.originalTexts.getValue(SlideTextSlot.TITLE))
        assertTrue(check.findings.any {
            it.ruleId == "marketplace.slide.title" && it.offendingSubstring == "6"
        })
    }

    @Test
    fun slideSeriesReuseOneBackgroundArtifactAndOneItemSource() {
        val item = PixelBuffer.solid(2, 2, PixelBuffer.rgba(80, 90, 100))
        val originalPixels = item.pixels.copyOf()
        val background = SlideSeriesBackground.create(
            id = "saved-look-background",
            pixels = PixelBuffer.solid(900, 1200, PixelBuffer.rgba(245, 245, 245)),
        )
        val slides = listOf(
            SlideDraft(SlideTemplate.HERO, mapOf(SlideTextSlot.TITLE to "Product")),
            SlideDraft(SlideTemplate.SPEC3, mapOf(SlideTextSlot.SPEC_1 to "Cotton")),
            SlideDraft(SlideTemplate.DETAIL, mapOf(SlideTextSlot.CAPTION to "Woven texture")),
        )

        val result = SlideLayoutEngine.renderSeries(
            slides = slides,
            items = listOf(item),
            background = background,
            style = SlideStyle(id = "saved-look"),
            marketplaceRules = rules("en", emptySet()),
        )

        assertEquals(3, result.size)
        assertTrue(result.all { it.background === background })
        assertTrue(result.all { it.backgroundIdentity == background.identity })
        assertTrue(result.all { it.itemSources.single() === item })
        assertTrue(item.pixels.contentEquals(originalPixels))
    }

    @Test
    fun cleanTemplateHasNoTextOrOverlayShapes() {
        val result = SlideLayoutEngine.renderSeries(
            slides = listOf(SlideDraft(SlideTemplate.CLEAN, emptyMap())),
            items = listOf(PixelBuffer.solid(1, 1, PixelBuffer.rgba(1, 2, 3))),
            background = SlideSeriesBackground.create(
                "clean-look",
                PixelBuffer.solid(900, 1200, PixelBuffer.rgba(255, 255, 255)),
            ),
            style = SlideStyle(id = "clean-look"),
            marketplaceRules = rules("en", emptySet()),
        ).single()

        assertTrue(result.textBlocks.isEmpty())
        assertTrue(result.shapes.isEmpty())
    }

    @Test
    fun sameInputsProduceTheSameItemCompositionAndTextLayout() {
        val item = PixelBuffer.solid(2, 2, PixelBuffer.rgba(40, 80, 120))
        val background = SlideSeriesBackground.create(
            "deterministic-look",
            PixelBuffer.solid(900, 1200, PixelBuffer.rgba(230, 230, 230)),
        )
        val input = listOf(SlideDraft(SlideTemplate.HERO, mapOf(SlideTextSlot.TITLE to "Product")))

        val first = SlideLayoutEngine.renderSeries(
            input,
            listOf(item),
            background,
            SlideStyle("deterministic-look"),
            rules("en", emptySet()),
        ).single()
        val second = SlideLayoutEngine.renderSeries(
            input,
            listOf(item),
            background,
            SlideStyle("deterministic-look"),
            rules("en", emptySet()),
        ).single()

        assertTrue(first.baseImage.pixels.contentEquals(second.baseImage.pixels))
        assertEquals(first.textBlocks, second.textBlocks)
    }

    @Test
    fun selectedTextColourMeetsLargeTextContrastMinimum() {
        val white = PixelBuffer.fromArgb(0xffffffff.toInt())
        val black = PixelBuffer.fromArgb(0xff000000.toInt())

        assertTrue(SlideContrast.ratio(black, white) >= rules("en", emptySet()).largeContrastMinimum)
    }

    private fun rules(
        language: String,
        prohibited: Set<String>,
        limits: Map<SlideTextSlot, SlideTextLimit> = emptyMap(),
    ) = SlideMarketplaceRules(
        marketplaceId = "test-market",
        marketplaceName = "Test marketplace",
        languageTag = language,
        prohibitedCategories = prohibited,
        limits = limits,
        phraseRules = when (language.substringBefore('-')) {
            "ru" -> mapOf(
                "price_discount" to listOf("скидка", "распродажа", "акция", "дёшево"),
                "evaluative_claim" to listOf("хит", "топ", "лучший", "лидер продаж"),
                "review_reward" to listOf("отзыв за", "кешбэк за"),
                "call_to_action" to listOf("купи", "купите", "звоните", "закажи"),
                "delivery_returns" to listOf("доставка", "возврат"),
            )
            else -> mapOf(
                "price_discount" to listOf("sale", "discount", "off"),
                "evaluative_claim" to listOf("best", "bestseller"),
                "review_reward" to listOf("review for", "cashback for"),
                "call_to_action" to listOf("buy now", "call", "order"),
                "delivery_returns" to listOf("free shipping", "returns"),
            )
        },
    )
}

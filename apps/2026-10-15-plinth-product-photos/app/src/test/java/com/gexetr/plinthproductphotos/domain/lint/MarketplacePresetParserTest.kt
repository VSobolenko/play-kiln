package com.gexetr.plinthproductphotos.domain.lint

import com.gexetr.plinthproductphotos.domain.presets.MarketplacePresetParser
import com.gexetr.plinthproductphotos.domain.presets.PresetParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplacePresetParserTest {
    @Test
    fun parserKeepsTheSixLocaleOrdersAndEvidenceStatus() {
        val result = MarketplacePresetParser.parseDocument(document())

        assertTrue(result is PresetParseResult.Success)
        val parsed = (result as PresetParseResult.Success).document
        assertEquals(listOf("seller"), parsed.localeDefaults["en"])
        assertEquals(listOf("seller"), parsed.localeDefaults["ru"])
        assertEquals(listOf("seller"), parsed.localeDefaults["de"])
        assertEquals(listOf("seller"), parsed.localeDefaults["fr"])
        assertEquals(listOf("seller"), parsed.localeDefaults["es"])
        assertEquals(listOf("seller"), parsed.localeDefaults["pt-BR"])
        assertEquals("secondary", parsed.marketplaces.single().status.wireValue)
        assertEquals("2026-10-12", parsed.marketplaces.single().verifiedAt)
        assertEquals("https://seller.example.com/rules", parsed.marketplaces.single().sourceUrl)
    }

    @Test
    fun parserReturnsTypedErrorForMissingVerificationDate() {
        val result = MarketplacePresetParser.parseDocument(document(verifiedAt = null))

        assertTrue(result is PresetParseResult.Invalid)
        val error = (result as PresetParseResult.Invalid).error
        assertEquals("$.marketplaces[0].verifiedAt", error.path)
    }

    @Test
    fun parserReturnsTypedErrorForMissingSource() {
        val result = MarketplacePresetParser.parseDocument(document(includeSource = false))

        assertTrue(result is PresetParseResult.Invalid)
        val error = (result as PresetParseResult.Invalid).error
        assertEquals("$.marketplaces[0].sources", error.path)
    }

    private fun document(verifiedAt: String? = "2026-10-12", includeSource: Boolean = true): String {
        val verifiedAtJson = verifiedAt?.let { "\"$it\"" } ?: "null"
        val sourceJson = if (includeSource) {
            """
            "sources": [{
              "title": "Seller rules",
              "url": "https://seller.example.com/rules",
              "site": "seller.example.com",
              "printedDate": "read 2026-10-12",
              "label": "ON RECORD",
              "firstParty": true,
              "quotes": "Use supported image formats."
            }],
            """
        } else {
            ""
        }
        return """
            {
              "schemaVersion": 1,
              "seededAt": "2026-10-12",
              "textCategories": ["price_discount"],
              "localeDefaults": {
                "en": ["seller"], "ru": ["seller"], "de": ["seller"],
                "fr": ["seller"], "es": ["seller"], "pt-BR": ["seller"]
              },
              "marketplaces": [{
                "id": "seller",
                "name": "Seller",
                "regions": ["GLOBAL"],
                "defaultLocale": "en-US",
                "queue": 1,
                "editPolicy": "free",
                "status": "secondary",
                "verifiedAt": $verifiedAtJson,
                $sourceJson
                "image": {"formats": ["jpg"]},
                "main": {"backgroundPolicy": "any", "textOnMain": "not-stated"},
                "text": {"prohibited": [], "contrastMin": null},
                "listing": {"titleMax": null, "descriptionMax": null},
                "notes": []
              }],
              "unreadStubs": []
            }
        """.trimIndent()
    }
}

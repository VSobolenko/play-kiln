package com.gexetr.plinthproductphotos.data.slide

import android.content.res.AssetManager
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SlideProhibitedTextCatalog private constructor(
    private val phrasesByLanguage: Map<String, Map<String, List<String>>>,
) {
    fun phrasesFor(languageTag: String): Map<String, List<String>> =
        phrasesByLanguage[languageTag.substringBefore('-').lowercase(Locale.ROOT)]
            ?: emptyMap()

    companion object {
        fun load(assets: AssetManager): SlideProhibitedTextCatalog {
            val source = assets.open("slide/prohibited-text.json").bufferedReader().use { it.readText() }
            val root = Json.parseToJsonElement(source).jsonObject
            require(root.getValue("schemaVersion").jsonPrimitive.content.toInt() == 1) {
                "Unsupported prohibited-text asset schema"
            }
            val languages = root.getValue("languages").jsonObject.mapValues { (_, categoriesElement) ->
                categoriesElement.jsonObject.mapValues { (_, phrasesElement) ->
                    phrasesElement.jsonArray.map { it.jsonPrimitive.content }
                }
            }
            require(languages.keys.containsAll(setOf("en", "ru"))) {
                "Prohibited-text phrases must include the supported English and Russian locales"
            }
            val expectedCategories = setOf(
                "price_discount",
                "contacts_links",
                "qr_code",
                "evaluative_claim",
                "review_reward",
                "sold_count",
                "call_to_action",
                "delivery_returns",
            )
            require(languages.values.all { it.keys == expectedCategories }) {
                "Prohibited-text phrases must declare every slide-text category"
            }
            return SlideProhibitedTextCatalog(languages)
        }
    }
}

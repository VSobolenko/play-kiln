package com.gexetr.plinthproductphotos.domain.presets

import kotlinx.serialization.json.JsonObject

enum class EditPolicy(val wireValue: String) {
    ORIGINAL_ONLY("original-only"),
    LIMITED_OVERLAY("limited-overlay"),
    FREE("free"),
}

enum class PresetStatus(val wireValue: String) {
    FIRST_PARTY("first-party"),
    SECONDARY("secondary"),
    UNREAD("unread"),
}

data class MarketplaceDocument(
    val schemaVersion: Int,
    val seededAt: String?,
    val textCategories: Set<String>,
    val localeDefaults: Map<String, List<String>>,
    val marketplaces: List<MarketplacePreset>,
    val unreadStubs: List<UnreadMarketplaceStub>,
) {
    fun defaultsFor(localeTag: String): List<String> {
        val exact = localeDefaults.entries.firstOrNull { it.key.equals(localeTag, ignoreCase = true) }?.value
        if (exact != null) return exact
        val language = localeTag.substringBefore('-')
        return localeDefaults.entries.firstOrNull { it.key.equals(language, ignoreCase = true) }?.value
            ?: localeDefaults.getValue("en")
    }
}

data class UnreadMarketplaceStub(
    val id: String,
    val queue: Int,
    val status: PresetStatus = PresetStatus.UNREAD,
)

data class MarketplacePreset(
    val id: String,
    val name: String,
    val regions: List<String>,
    val defaultLocale: String,
    val queue: Int,
    val editPolicy: EditPolicy,
    val allowedOverlays: Set<String>?,
    val status: PresetStatus,
    val verifiedAt: String,
    val sourceUrl: String,
    val sources: List<PresetSource>,
    val image: ImageRules,
    val main: MainRules,
    val text: TextRules,
    val listing: ListingRules,
    val notes: List<String>,
    val raw: JsonObject,
)

data class PresetSource(
    val title: String,
    val url: String,
    val site: String,
    val printedDate: String,
    val label: String,
    val firstParty: Boolean,
    val quotes: String,
)

data class ImageRules(
    val formats: Set<String>,
    val maxBytes: Long? = null,
    val minWidth: Int? = null,
    val minHeight: Int? = null,
    val minHeightAlternative: AlternativeLimit? = null,
    val minLongSide: Int? = null,
    val maxLongSide: Int? = null,
    val maxSide: Int? = null,
    val maxSidesSum: Int? = null,
    val maxCount: Int? = null,
    val aspect: String? = null,
    val recommended: RecommendedDimensions? = null,
    val minJpegQuality: Int? = null,
    val noTransparentPng: Boolean? = null,
    val raw: JsonObject,
)

data class AlternativeLimit(
    val value: Int,
    val note: String,
)

data class RecommendedDimensions(
    val width: Int,
    val height: Int,
    val basis: String,
)

data class MainRules(
    val backgroundPolicy: String?,
    val minFillRatio: Double?,
    val textOnMain: String?,
    val originalOnlyAllowed: Set<String>,
    val raw: JsonObject,
)

data class TextRules(
    val prohibited: Set<String>,
    val contrastMin: ContrastThresholds?,
    val avoidColourPairs: List<String>,
    val raw: JsonObject,
)

data class ContrastThresholds(val small: Double, val large: Double)

data class ListingRules(
    val titleMax: Int?,
    val descriptionMax: Int?,
    val raw: JsonObject,
)

sealed interface PresetParseResult {
    data class Success(val document: MarketplaceDocument) : PresetParseResult
    data class Invalid(val error: PresetError) : PresetParseResult
}

data class PresetError(val path: String, val message: String)

package com.gexetr.plinthproductphotos.domain.coach

import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class CoachBackgroundRule {
    WHITE_255,
    NEUTRAL,
    NEUTRAL_ONLY,
    ANY,
    UNSPECIFIED,
}

data class CoachMarketplaceGuidance(
    val aspect: String?,
    val minimumFillRatio: Double?,
    val backgroundRule: CoachBackgroundRule,
    val itemMustBeFullyVisible: Boolean,
    val forbidPackaging: Boolean,
    val forbidMainText: Boolean,
    val penalizeMainText: Boolean,
    val avoidMarksAndBorders: Boolean,
) {
    companion object {
        fun from(preset: MarketplacePreset): CoachMarketplaceGuidance {
            val main = preset.main
            val raw = main.raw
            fun enabled(key: String): Boolean = raw[key]?.jsonPrimitive?.booleanOrNull == true
            return CoachMarketplaceGuidance(
                aspect = preset.image.aspect,
                minimumFillRatio = main.minFillRatio,
                backgroundRule = when (main.backgroundPolicy) {
                    "white-255" -> CoachBackgroundRule.WHITE_255
                    "neutral" -> CoachBackgroundRule.NEUTRAL
                    "neutral-only" -> CoachBackgroundRule.NEUTRAL_ONLY
                    "any" -> CoachBackgroundRule.ANY
                    else -> CoachBackgroundRule.UNSPECIFIED
                },
                itemMustBeFullyVisible = enabled("itemMustBeFullyVisible"),
                forbidPackaging = enabled("forbidPackaging"),
                forbidMainText = main.textOnMain == "forbidden",
                penalizeMainText = main.textOnMain == "penalised-by-size",
                avoidMarksAndBorders = enabled("forbidWatermark") ||
                    enabled("forbidBorders") ||
                    enabled("forbidLogoTagsPromoMarks") ||
                    enabled("forbidMarketingGraphics"),
            )
        }
    }
}

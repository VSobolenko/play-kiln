package com.gexetr.plinthproductphotos.domain.lint

import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset

sealed interface LinterFix {
    companion object {
        fun forFinding(finding: LintFinding, preset: MarketplacePreset): LinterFix =
            MarketplaceLinterFixes.forFinding(finding, preset)
    }

    data class ConvertFormat(val to: String) : LinterFix
    data class Recompress(val qualityFloor: Int, val downscaleIfNeeded: Boolean) : LinterFix
    data class Upscale(
        val minWidth: Int?,
        val minHeight: Int?,
        val minLongSide: Int?,
        val costsCredits: Boolean = true,
    ) : LinterFix
    data class Downscale(val maxSide: Int?, val maxLongSide: Int?, val maxSidesSum: Int?) : LinterFix
    data class PadToAspect(val aspect: String) : LinterFix
    data class CropToAspect(val aspect: String) : LinterFix
    data class SetBackgroundWhite255(val compositeColor: String = "#FFFFFF") : LinterFix
    data class FillToRatio(val minimum: Double) : LinterFix
    data object SetBackgroundNeutral : LinterFix
    data object EditSlideText : LinterFix
    data object AutoTextColour : LinterFix
    data object FlattenOnBackground : LinterFix
    data class RemoveForbiddenOverlay(val overlayIds: List<String>) : LinterFix
    data object StripExif : LinterFix
    data object ReorderAccessorySecond : LinterFix
    data object TrimListingText : LinterFix
    data class CannotBeSatisfiedByEditing(val reason: String) : LinterFix
}

object MarketplaceLinterFixes {
    fun forFinding(finding: LintFinding, preset: MarketplacePreset): LinterFix {
        val fix = when (finding.ruleId) {
            "L01" -> (preset.image.formats.firstOrNull { it.equals("jpg", ignoreCase = true) }
                ?: preset.image.formats.firstOrNull())
                ?.let { LinterFix.ConvertFormat(it) }
                ?: cannot("The preset has no supported output format.")
            "L02" -> LinterFix.Recompress(
                qualityFloor = preset.image.minJpegQuality ?: 70,
                downscaleIfNeeded = true,
            )
            "L03" -> LinterFix.Upscale(
                minWidth = preset.image.minWidth,
                minHeight = preset.image.minHeight,
                minLongSide = preset.image.minLongSide,
            )
            "L04" -> LinterFix.Downscale(
                maxSide = preset.image.maxSide,
                maxLongSide = preset.image.maxLongSide,
                maxSidesSum = preset.image.maxSidesSum,
            )
            "L05" -> preset.image.aspect?.let { aspect ->
                if (preset.editPolicy == EditPolicy.ORIGINAL_ONLY &&
                    preset.main.originalOnlyAllowed.any { it.equals("crop", ignoreCase = true) }
                ) {
                    LinterFix.CropToAspect(aspect)
                } else {
                    LinterFix.PadToAspect(aspect)
                }
            } ?: cannot("The preset does not declare a target aspect ratio.")
            "L06" -> cannot("Remove or replace the excess marketplace items; image editing cannot change pack count.")
            "L07" -> LinterFix.SetBackgroundWhite255()
            "L08" -> preset.main.minFillRatio?.let { LinterFix.FillToRatio(it) }
                ?: cannot("The preset does not declare a minimum fill ratio.")
            "L09" -> LinterFix.SetBackgroundNeutral
            "L10" -> cannot("Review the detected text box and remove or reposition text manually.")
            "L11" -> LinterFix.EditSlideText
            "L12" -> LinterFix.AutoTextColour
            "L13" -> LinterFix.FlattenOnBackground
            "L14" -> cannot("Review the detected contact or QR box and remove it from the source manually.")
            "L15" -> editPolicyFix(finding, preset)
            "L16" -> LinterFix.StripExif
            "L17" -> LinterFix.ReorderAccessorySecond
            "L18" -> cannot("The watermark match is only a heuristic; review the marked area manually.")
            "L19" -> LinterFix.TrimListingText
            "L20" -> LinterFix.Recompress(
                qualityFloor = preset.image.minJpegQuality
                    ?: return cannot("The preset does not declare a JPEG quality floor."),
                downscaleIfNeeded = false,
            )
            else -> cannot("No fix is defined for ${finding.ruleId}.")
        }
        return enforceOriginalOnlyAllowList(fix, preset)
    }

    private fun editPolicyFix(finding: LintFinding, preset: MarketplacePreset): LinterFix =
        when (preset.editPolicy) {
            EditPolicy.ORIGINAL_ONLY -> cannot("Use the unedited source image; this rule cannot be satisfied by editing.")
            EditPolicy.LIMITED_OVERLAY -> {
                val overlayIds = finding.measuredValue
                    .substringAfter("overlays=", "")
                    .substringBefore(';')
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                if (overlayIds.isEmpty()) cannot("Remove the forbidden operation and use only allowed overlays.")
                else LinterFix.RemoveForbiddenOverlay(overlayIds)
            }
            EditPolicy.FREE -> cannot("Review the preset's editing policy and the reported operation.")
        }

    private fun enforceOriginalOnlyAllowList(fix: LinterFix, preset: MarketplacePreset): LinterFix {
        if (preset.editPolicy != EditPolicy.ORIGINAL_ONLY) return fix
        val operation = when (fix) {
            is LinterFix.CropToAspect -> "crop"
            is LinterFix.PadToAspect -> "pad"
            is LinterFix.ConvertFormat -> "re-encode"
            is LinterFix.Recompress -> "re-encode"
            is LinterFix.Upscale -> "upscale"
            is LinterFix.Downscale -> "resize"
            is LinterFix.SetBackgroundWhite255 -> "background-replace"
            is LinterFix.SetBackgroundNeutral -> "background-replace"
            is LinterFix.FillToRatio -> "resize"
            is LinterFix.FlattenOnBackground -> "flatten"
            else -> null
        } ?: return fix
        if (preset.main.originalOnlyAllowed.any { it.equals(operation, ignoreCase = true) }) return fix
        return cannot("This original-only preset does not allow '$operation'; the finding cannot be satisfied by editing.")
    }

    private fun cannot(reason: String) = LinterFix.CannotBeSatisfiedByEditing(reason)
}

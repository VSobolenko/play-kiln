package com.gexetr.plinthproductphotos.domain.lint

import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import kotlin.math.abs

enum class FindingSeverity {
    ERROR,
    WARNING,
    ADVICE,
}

data class LintFinding(
    val ruleId: String,
    val severity: FindingSeverity,
    val violatedLimit: String,
    val measuredValue: String,
    val message: String,
)

data class DetectedTextBlock(
    val text: String,
    val characters: Int,
    val heightShare: Double,
    val outsideItem: Boolean,
)

data class PackOrderItem(
    val role: SlotRole,
    val accessoryTagged: Boolean = false,
)

data class ImageFacts(
    val width: Int,
    val height: Int,
    val ratio: Double,
    val format: String,
    val bytes: Long,
    val role: SlotRole = SlotRole.MAIN,
    val jpegQualityEstimate: Int? = null,
    val hasAlpha: Boolean? = null,
    val borderMinimumChannelOutsideItem: Int? = null,
    val itemFillRatio: Double? = null,
    val backgroundMeanChroma: Double? = null,
    val detectedTextBlocks: List<DetectedTextBlock> = emptyList(),
    val slideTextCategories: Set<String> = emptySet(),
    val smallContrastRatio: Double? = null,
    val largeContrastRatio: Double? = null,
    val detectedSensitiveKinds: Set<String> = emptySet(),
    val operations: List<String> = emptyList(),
    val appliedOverlays: List<String> = emptyList(),
    val exifGpsPresent: Boolean? = null,
    val watermarkHeuristicMatch: Boolean? = null,
    val packItemCount: Int? = null,
    val packOrder: List<PackOrderItem> = emptyList(),
    val listingTitle: String? = null,
    val listingDescription: String? = null,
)

private interface MarketplaceLintRule {
    val id: String
    fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding?
}

private object L01Format : MarketplaceLintRule {
    override val id = "L01"

    // L01 — the output format must occur in the preset's supported formats.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? =
        if (preset.image.formats.any { it.equals(facts.format, ignoreCase = true) }) null
        else finding(id, "format in ${preset.image.formats}", facts.format, "Unsupported image format")
}

private object L02FileSize : MarketplaceLintRule {
    override val id = "L02"

    // L02 — output bytes may not exceed the preset's upload limit.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        val limit = preset.image.maxBytes ?: return null
        return if (facts.bytes <= limit) null
        else finding(id, "bytes <= $limit", facts.bytes.toString(), "Image exceeds the file-size limit")
    }
}

private object L03MinimumSize : MarketplaceLintRule {
    override val id = "L03"

    // L03 — width, height and long-side minima are checked independently; WB's secondary height conflict is explicit.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        val failures = mutableListOf<String>()
        val measurements = mutableListOf<String>()
        preset.image.minWidth?.let { limit ->
            if (facts.width < limit) {
                failures += "width >= $limit"
                measurements += "width=${facts.width}"
            }
        }
        preset.image.minHeight?.let { limit ->
            if (facts.height < limit) {
                val alternative = preset.image.minHeightAlternative
                failures += if (alternative == null) "height >= $limit"
                else "height >= $limit (alternative ${alternative.value}: ${alternative.note})"
                measurements += "height=${facts.height}"
            }
        }
        preset.image.minLongSide?.let { limit ->
            val measured = maxOf(facts.width, facts.height)
            if (measured < limit) {
                failures += "longSide >= $limit"
                measurements += "longSide=$measured"
            }
        }
        return if (failures.isEmpty()) null
        else finding(
            id,
            failures.joinToString("; "),
            measurements.joinToString(", "),
            "Image is below the minimum dimensions: ${failures.joinToString("; ")}",
        )
    }
}

private object L04MaximumSize : MarketplaceLintRule {
    override val id = "L04"

    // L04 — each declared maximum side and the combined side limit is enforced.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        val failures = mutableListOf<String>()
        val measurements = mutableListOf<String>()
        preset.image.maxSide?.let { limit ->
            if (maxOf(facts.width, facts.height) > limit) {
                failures += "maxSide <= $limit"
                measurements += "longSide=${maxOf(facts.width, facts.height)}"
            }
        }
        preset.image.maxLongSide?.let { limit ->
            if (maxOf(facts.width, facts.height) > limit) {
                failures += "longSide <= $limit"
                measurements += "longSide=${maxOf(facts.width, facts.height)}"
            }
        }
        preset.image.maxSidesSum?.let { limit ->
            if (facts.width.toLong() + facts.height > limit) {
                failures += "width + height <= $limit"
                measurements += "width+height=${facts.width.toLong() + facts.height}"
            }
        }
        return if (failures.isEmpty()) null
        else finding(id, failures.joinToString("; "), measurements.joinToString(", "), "Image exceeds a dimension limit")
    }
}

private object L05Aspect : MarketplaceLintRule {
    override val id = "L05"

    // L05 — aspect deviation over 0.01 is a warning; the seed has no evidenced crop-required flag.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        val aspect = preset.image.aspect?.let(::parseAspect) ?: return null
        val deviation = abs(facts.ratio - aspect)
        return if (deviation <= 0.01) null
        else finding(
            id,
            "ratio within 0.01 of $aspect",
            facts.ratio.toString(),
            "Image aspect ratio differs from the preset",
            FindingSeverity.WARNING,
        )
    }

    private fun parseAspect(value: String): Double {
        val parts = value.split(':')
        val width = parts.getOrNull(0)?.toDoubleOrNull()
        val height = parts.getOrNull(1)?.toDoubleOrNull()
        require(parts.size == 2 && width != null && height != null && width > 0.0 && height > 0.0) {
            "Invalid marketplace aspect ratio: $value"
        }
        return width / height
    }
}

private object L06Count : MarketplaceLintRule {
    override val id = "L06"

    // L06 — only a measured pack count can be compared with the marketplace cap.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        val limit = preset.image.maxCount ?: return null
        val measured = facts.packItemCount ?: return null
        return if (measured <= limit) null
        else finding(id, "items <= $limit", measured.toString(), "Marketplace pack has too many items")
    }
}

private object L07WhiteMain : MarketplaceLintRule {
    override val id = "L07"

    // L07 — a white-255 main image must have no sub-255 channel in a measured border pixel outside the item.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        if (facts.role != SlotRole.MAIN || preset.main.backgroundPolicy != "white-255") return null
        val minimumChannel = facts.borderMinimumChannelOutsideItem ?: return null
        return if (minimumChannel == 255) null
        else finding(id, "every outside-item border channel = 255", minimumChannel.toString(), "Main-image border is not pure white")
    }
}

private object L08FillRatio : MarketplaceLintRule {
    override val id = "L08"

    // L08 — compare the measured item bounding-box fill with the declared lower bound.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        if (facts.role != SlotRole.MAIN) return null
        val limit = preset.main.minFillRatio ?: return null
        val measured = facts.itemFillRatio ?: return null
        return if (measured >= limit) null
        else finding(
            id,
            "itemFillRatio >= $limit",
            measured.toString(),
            "Item occupies less than the minimum frame fill",
            FindingSeverity.WARNING,
        )
    }
}

private object L09NeutralOnly : MarketplaceLintRule {
    override val id = "L09"

    // L09 — neutral-only backgrounds fail when measured mean Lab chroma exceeds 12.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        if (facts.role !in setOf(SlotRole.MAIN, SlotRole.EXTRA) ||
            preset.main.backgroundPolicy != "neutral-only"
        ) return null
        val measured = facts.backgroundMeanChroma ?: return null
        return if (measured <= 12.0) null
        else finding(id, "mean background chroma <= 12 Lab C*", measured.toString(), "Background is not neutral")
    }
}

private object L10TextOnMain : MarketplaceLintRule {
    override val id = "L10"

    // L10 — three or more OCR characters outside the item trigger the declared main-text policy.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        if (facts.role != SlotRole.MAIN) return null
        val policy = preset.main.textOnMain
        if (policy != "forbidden" && policy != "penalised-by-size") return null
        val block = facts.detectedTextBlocks.firstOrNull { it.characters >= 3 && it.outsideItem } ?: return null
        val severity = if (policy == "forbidden") FindingSeverity.ERROR else FindingSeverity.WARNING
        val limit = if (policy == "forbidden") "no OCR text block >= 3 characters outside item"
        else "text outside item is penalised by its height share"
        return finding(
            id,
            limit,
            "${block.characters} characters; heightShare=${block.heightShare}",
            "Detected text outside the item",
            severity,
        )
    }
}

private object L11SlideText : MarketplaceLintRule {
    override val id = "L11"

    // L11 — a classified slide string matching any prohibited text category blocks slide rendering.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        if (facts.role != SlotRole.SLIDE) return null
        val category = facts.slideTextCategories.intersect(preset.text.prohibited).firstOrNull() ?: return null
        return finding(id, "category not in ${preset.text.prohibited}", category, "Slide text contains a prohibited category")
    }
}

private object L12SlideContrast : MarketplaceLintRule {
    override val id = "L12"

    // L12 — use the preset's small/large contrast floor; only WB's stated severity is an error.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        if (facts.role != SlotRole.SLIDE) return null
        val threshold = preset.text.contrastMin ?: return null
        val failures = listOfNotNull(
            facts.smallContrastRatio?.takeIf { it < threshold.small }?.let { "small=$it < ${threshold.small}" },
            facts.largeContrastRatio?.takeIf { it < threshold.large }?.let { "large=$it < ${threshold.large}" },
        )
        if (failures.isEmpty()) return null
        val severity = if (preset.id == "wb") FindingSeverity.ERROR else FindingSeverity.WARNING
        return finding(
            id,
            "small >= ${threshold.small}; large >= ${threshold.large}",
            failures.joinToString(", "),
            "Slide text contrast is below the preset minimum",
            severity,
        )
    }
}

private object L13Transparency : MarketplaceLintRule {
    override val id = "L13"

    // L13 — presets marked noTransparentPng reject files with an alpha channel.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        if (preset.image.noTransparentPng != true || facts.hasAlpha != true) return null
        return finding(id, "hasAlpha = false", "true", "Transparent pixels are not supported")
    }
}

private object L14Contacts : MarketplaceLintRule {
    override val id = "L14"

    // L14 — detected contact or QR material fails only when its corresponding category is prohibited.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        val prohibited = facts.detectedSensitiveKinds.intersect(preset.text.prohibited)
            .filter { it == "contacts_links" || it == "qr_code" }
        if (prohibited.isEmpty()) return null
        return finding(
            id,
            "no detected prohibited contacts/QR categories",
            prohibited.joinToString(),
            "Contact details or a QR region were detected",
        )
    }
}

private object L15EditingPolicy : MarketplaceLintRule {
    override val id = "L15"

    // L15 — original-only operations and limited-overlay items must stay within their explicit allow-lists.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        val forbiddenOperations = when (preset.editPolicy) {
            EditPolicy.ORIGINAL_ONLY -> facts.operations.filterNot { operation ->
                preset.main.originalOnlyAllowed.any { it.equals(operation, ignoreCase = true) }
            }
            EditPolicy.LIMITED_OVERLAY -> facts.operations.filterNot { operation ->
                preset.allowedOverlays.orEmpty().any { it.equals(operation, ignoreCase = true) }
            }
            EditPolicy.FREE -> emptyList()
        }
        val forbiddenOverlays = when (preset.editPolicy) {
            EditPolicy.ORIGINAL_ONLY -> facts.appliedOverlays
            EditPolicy.LIMITED_OVERLAY -> facts.appliedOverlays.filterNot { overlay ->
                preset.allowedOverlays.orEmpty().any { it.equals(overlay, ignoreCase = true) }
            }
            EditPolicy.FREE -> emptyList()
        }
        if (forbiddenOperations.isEmpty() && forbiddenOverlays.isEmpty()) return null
        val measured = buildList {
            if (forbiddenOperations.isNotEmpty()) add("operations=${forbiddenOperations.joinToString()}")
            if (forbiddenOverlays.isNotEmpty()) add("overlays=${forbiddenOverlays.joinToString()}")
        }.joinToString("; ")
        return finding(
            id,
            "operations/overlays within ${preset.editPolicy.wireValue} allow-list",
            measured,
            "This output uses an operation or overlay forbidden by the marketplace preset",
        )
    }
}

private object L16LocationData : MarketplaceLintRule {
    override val id = "L16"

    // L16 — GPS-bearing EXIF in an output merits a warning.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? =
        if (facts.exifGpsPresent == true) {
            finding(id, "EXIF GPS absent", "present", "Output contains location metadata", FindingSeverity.WARNING)
        } else {
            null
        }
}

private object L17OrderHint : MarketplaceLintRule {
    override val id = "L17"

    // L17 — for WB, a seller-tagged accessory belongs immediately after the main item.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        if (preset.id != "wb") return null
        val mainIndex = facts.packOrder.indexOfFirst { it.role == SlotRole.MAIN }
        if (mainIndex < 0) return null
        val misplaced = facts.packOrder.withIndex().firstOrNull { (index, item) ->
            item.accessoryTagged && index != mainIndex + 1
        } ?: return null
        return finding(
            id,
            "tagged accessory at position ${mainIndex + 2}",
            "accessory position ${misplaced.index + 1}",
            "WB suggests placing the tagged accessory second",
            FindingSeverity.ADVICE,
        )
    }
}

private object L18WatermarkGuess : MarketplaceLintRule {
    override val id = "L18"

    // L18 — a watermark heuristic is advice only because OCR cannot prove absence or presence.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? =
        if (facts.watermarkHeuristicMatch == true) {
            finding(id, "no watermark heuristic match", "match", "Possible watermark or logo detected", FindingSeverity.ADVICE)
        } else {
            null
        }
}

private object L19ListingText : MarketplaceLintRule {
    override val id = "L19"

    // L19 — listing title and description are checked only against declared limits.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        val titleLimit = preset.listing.titleMax
        val titleFailure = facts.listingTitle?.let { title ->
            titleLimit?.takeIf { title.length > it }?.let { "title ${title.length} > $it" }
        }
        val descriptionLimit = preset.listing.descriptionMax
        val descriptionFailure = facts.listingDescription?.let { description ->
            descriptionLimit?.takeIf { description.length > it }?.let { "description ${description.length} > $it" }
        }
        val failures = listOfNotNull(titleFailure, descriptionFailure)
        if (failures.isEmpty()) return null
        return finding(
            id,
            "title <= ${titleLimit ?: "not stated"}; description <= ${descriptionLimit ?: "not stated"}",
            failures.joinToString("; "),
            "Listing text exceeds a declared character limit",
        )
    }
}

private object L20JpegQuality : MarketplaceLintRule {
    override val id = "L20"

    // L20 — the JPEG quality floor is applied to WB, the only seed preset that declares it.
    override fun evaluate(facts: ImageFacts, preset: MarketplacePreset): LintFinding? {
        if (preset.id != "wb" || !facts.format.equals("jpg", ignoreCase = true) &&
            !facts.format.equals("jpeg", ignoreCase = true)
        ) return null
        val limit = preset.image.minJpegQuality ?: return null
        val measured = facts.jpegQualityEstimate ?: return null
        return if (measured >= limit) null
        else finding(id, "estimated JPEG quality >= $limit", measured.toString(), "JPEG quality is below the preset minimum")
    }
}

object MarketplaceLinter {
    private val rules: List<MarketplaceLintRule> = listOf(
        L01Format,
        L02FileSize,
        L03MinimumSize,
        L04MaximumSize,
        L05Aspect,
        L06Count,
        L07WhiteMain,
        L08FillRatio,
        L09NeutralOnly,
        L10TextOnMain,
        L11SlideText,
        L12SlideContrast,
        L13Transparency,
        L14Contacts,
        L15EditingPolicy,
        L16LocationData,
        L17OrderHint,
        L18WatermarkGuess,
        L19ListingText,
        L20JpegQuality,
    )

    fun evaluate(facts: ImageFacts, preset: MarketplacePreset): List<LintFinding> =
        rules.mapNotNull { it.evaluate(facts, preset) }
}

private fun finding(
    id: String,
    limit: String,
    measured: String,
    message: String,
    severity: FindingSeverity = FindingSeverity.ERROR,
) = LintFinding(
    ruleId = id,
    severity = severity,
    violatedLimit = limit,
    measuredValue = measured,
    message = message,
)

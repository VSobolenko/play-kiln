package com.gexetr.plinthproductphotos.domain.lint

import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.AlternativeLimit
import com.gexetr.plinthproductphotos.domain.presets.ContrastThresholds
import com.gexetr.plinthproductphotos.domain.presets.ImageRules
import com.gexetr.plinthproductphotos.domain.presets.ListingRules
import com.gexetr.plinthproductphotos.domain.presets.MainRules
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.PresetSource
import com.gexetr.plinthproductphotos.domain.presets.PresetStatus
import com.gexetr.plinthproductphotos.domain.presets.TextRules
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceLinterTest {
    @Test
    fun knownGoodWildberriesImagePassesTheAvailableChecks() {
        assertTrue(MarketplaceLinter.evaluate(facts(), preset()).isEmpty())
    }

    @Test
    fun unmeasuredPixelFactsDoNotCreateBackgroundFindings() {
        val amazon = preset("amazon", backgroundPolicy = "white-255", minFillRatio = 0.85)
        val avito = preset("avito", backgroundPolicy = "neutral-only")

        assertFalse(MarketplaceLinter.evaluate(facts(), amazon).any { it.ruleId in setOf("L07", "L08") })
        assertFalse(MarketplaceLinter.evaluate(facts(), avito).any { it.ruleId == "L09" })
    }

    @Test
    fun l01ChecksAllowedFormat() {
        assertPasses("L01", facts(format = "jpg"), preset())
        assertFails("L01", facts(format = "heic"), preset())
    }

    @Test
    fun l02ChecksFileByteLimit() {
        assertPasses("L02", facts(bytes = 10_485_760), preset())
        assertFails("L02", facts(bytes = 10_485_761), preset())
    }

    @Test
    fun l03UsesTheLargerConflictingWildberriesMinimumHeight() {
        val belowWidth = assertFails("L03", facts(width = 699, height = 933), preset())
        assertTrue(belowWidth.message.contains("700"))
        val belowHeight = assertFails("L03", facts(width = 700, height = 900), preset())
        assertTrue(belowHeight.violatedLimit.contains("900"))
        assertPasses("L03", facts(width = 700, height = 933), preset())
    }

    @Test
    fun l04ChecksMaximumDimensions() {
        assertPasses("L04", facts(width = 7680, height = 4000), preset("ozon", maxSide = 7680))
        assertFails("L04", facts(width = 7681, height = 4000), preset("ozon", maxSide = 7680))
    }

    @Test
    fun l05ChecksAspectRatio() {
        assertPasses("L05", facts(width = 900, height = 1200, ratio = 0.75), preset("ozon", aspect = "3:4"))
        val finding = assertFails("L05", facts(width = 900, height = 1200, ratio = 1.0), preset("ozon", aspect = "3:4"))
        assertEquals(FindingSeverity.WARNING, finding.severity)
    }

    @Test
    fun l06ChecksMarketplacePackCount() {
        assertPasses("L06", facts(packItemCount = 30), preset(maxCount = 30))
        assertFails("L06", facts(packItemCount = 31), preset(maxCount = 30))
    }

    @Test
    fun l07RequiresPureWhiteBorderPixelsForAmazonMain() {
        assertPasses(
            "L07",
            facts(borderMinimumChannelOutsideItem = 255),
            preset("amazon", backgroundPolicy = "white-255"),
        )
        assertFails(
            "L07",
            facts(borderMinimumChannelOutsideItem = 254),
            preset("amazon", backgroundPolicy = "white-255"),
        )
    }

    @Test
    fun l08ChecksMinimumMainItemFill() {
        assertPasses("L08", facts(itemFillRatio = 0.85), preset(minFillRatio = 0.85))
        assertFails("L08", facts(itemFillRatio = 0.84), preset(minFillRatio = 0.85))
    }

    @Test
    fun l09ChecksNeutralOnlyBackgroundChroma() {
        assertPasses("L09", facts(backgroundMeanChroma = 12.0), preset("avito", backgroundPolicy = "neutral-only"))
        assertFails("L09", facts(backgroundMeanChroma = 12.01), preset("avito", backgroundPolicy = "neutral-only"))
    }

    @Test
    fun l10ChecksDetectedTextOutsideTheItemOnMain() {
        assertPasses("L10", facts(), preset(textOnMain = "forbidden"))
        assertFails(
            "L10",
            facts(detectedTextBlocks = listOf(DetectedTextBlock("SALE", 4, 0.1, outsideItem = true))),
            preset(textOnMain = "forbidden"),
        )
    }

    @Test
    fun l11ChecksProhibitedSlideTextCategories() {
        assertPasses("L11", facts(role = SlotRole.SLIDE), preset())
        assertFails(
            "L11",
            facts(role = SlotRole.SLIDE, slideTextCategories = setOf("price_discount")),
            preset(),
        )
    }

    @Test
    fun l12ChecksSmallAndLargeTextContrast() {
        assertPasses(
            "L12",
            facts(role = SlotRole.SLIDE, smallContrastRatio = 4.54, largeContrastRatio = 3.0),
            preset(),
        )
        assertFails(
            "L12",
            facts(role = SlotRole.SLIDE, smallContrastRatio = 4.48, largeContrastRatio = 3.0),
            preset(),
        )
    }

    @Test
    fun l13ChecksTransparentPng() {
        assertPasses("L13", facts(hasAlpha = false), preset("etsy", noTransparentPng = true))
        assertFails("L13", facts(hasAlpha = true), preset("etsy", noTransparentPng = true))
    }

    @Test
    fun l14ChecksProhibitedContactAndQrDetections() {
        assertPasses("L14", facts(), preset())
        assertFails(
            "L14",
            facts(detectedSensitiveKinds = setOf("contacts_links")),
            preset(),
        )
    }

    @Test
    fun l15BlocksNonAllowlistedEditsForOriginalOnlyPreset() {
        val originalOnly = preset(
            "vinted",
            editPolicy = EditPolicy.ORIGINAL_ONLY,
            originalOnlyAllowed = setOf("crop", "rotate", "exposure", "white-balance"),
        )
        assertPasses("L15", facts(operations = listOf("crop")), originalOnly)
        val finding = assertFails("L15", facts(operations = listOf("text-overlay")), originalOnly)
        assertTrue(LinterFix.forFinding(finding, originalOnly) is LinterFix.CannotBeSatisfiedByEditing)
        val overlayFinding = assertFails("L15", facts(appliedOverlays = listOf("badge")), originalOnly)
        assertTrue(LinterFix.forFinding(overlayFinding, originalOnly) is LinterFix.CannotBeSatisfiedByEditing)
    }

    @Test
    fun l15OnlyAllowsListedOverlaysAndFixesTheDisallowedOverlay() {
        val limitedOverlay = preset(
            "market",
            editPolicy = EditPolicy.LIMITED_OVERLAY,
            allowedOverlays = setOf("badge"),
        )
        assertPasses("L15", facts(appliedOverlays = listOf("badge")), limitedOverlay)
        val finding = assertFails("L15", facts(appliedOverlays = listOf("price")), limitedOverlay)
        assertEquals(LinterFix.RemoveForbiddenOverlay(listOf("price")), LinterFix.forFinding(finding, limitedOverlay))
    }

    @Test
    fun l16WarnsWhenOutputContainsExifGps() {
        assertPasses("L16", facts(exifGpsPresent = false), preset())
        assertFails("L16", facts(exifGpsPresent = true), preset())
    }

    @Test
    fun l17AdvisesWhenTaggedAccessoryIsNotSecondAfterMain() {
        val correctlyOrdered = listOf(
            PackOrderItem(SlotRole.MAIN),
            PackOrderItem(SlotRole.EXTRA, accessoryTagged = true),
        )
        val incorrectlyOrdered = listOf(
            PackOrderItem(SlotRole.MAIN),
            PackOrderItem(SlotRole.EXTRA),
            PackOrderItem(SlotRole.EXTRA, accessoryTagged = true),
        )
        assertPasses("L17", facts(packOrder = correctlyOrdered), preset())
        assertFails("L17", facts(packOrder = incorrectlyOrdered), preset())
    }

    @Test
    fun l18AdvisesOnWatermarkHeuristicMatches() {
        assertPasses("L18", facts(watermarkHeuristicMatch = false), preset())
        assertFails("L18", facts(watermarkHeuristicMatch = true), preset())
    }

    @Test
    fun l19ChecksListingTitleAndDescriptionLimits() {
        assertPasses("L19", facts(listingTitle = "A title"), preset())
        assertFails("L19", facts(listingTitle = "A".repeat(61)), preset())
        assertFails("L19", facts(listingDescription = "A".repeat(5001)), preset())
    }

    @Test
    fun l20ChecksWildberriesJpegQualityFloor() {
        val wildberries = preset(minJpegQuality = 65)
        assertPasses("L20", facts(jpegQualityEstimate = 65), wildberries)
        assertFails("L20", facts(jpegQualityEstimate = 64), wildberries)
    }

    @Test
    fun originalOnlyFindingCannotBeFixedByEditingTheImage() {
        val originalOnly = preset(
            "vinted",
            editPolicy = EditPolicy.ORIGINAL_ONLY,
            originalOnlyAllowed = setOf("crop", "rotate", "exposure", "white-balance"),
        )
        val finding = assertFails("L15", facts(operations = listOf("background-replace")), originalOnly)

        assertEquals(
            "Use the unedited source image; this rule cannot be satisfied by editing.",
            (LinterFix.forFinding(finding, originalOnly) as LinterFix.CannotBeSatisfiedByEditing).reason,
        )
    }

    private fun assertPasses(ruleId: String, facts: ImageFacts, preset: MarketplacePreset) {
        assertFalse(MarketplaceLinter.evaluate(facts, preset).any { it.ruleId == ruleId })
    }

    private fun assertFails(ruleId: String, facts: ImageFacts, preset: MarketplacePreset): LintFinding {
        val finding = MarketplaceLinter.evaluate(facts, preset).firstOrNull { it.ruleId == ruleId }
        assertTrue("Expected $ruleId finding", finding != null)
        val actual = requireNotNull(finding)
        assertTrue(actual.violatedLimit.isNotBlank())
        assertTrue(actual.measuredValue.isNotBlank())
        return actual
    }

    private fun facts(
        width: Int = 1000,
        height: Int = 1000,
        ratio: Double = width.toDouble() / height,
        format: String = "jpg",
        bytes: Long = 500_000,
        role: SlotRole = SlotRole.MAIN,
        jpegQualityEstimate: Int? = null,
        hasAlpha: Boolean? = null,
        borderMinimumChannelOutsideItem: Int? = null,
        itemFillRatio: Double? = null,
        backgroundMeanChroma: Double? = null,
        detectedTextBlocks: List<DetectedTextBlock> = emptyList(),
        slideTextCategories: Set<String> = emptySet(),
        smallContrastRatio: Double? = null,
        largeContrastRatio: Double? = null,
        detectedSensitiveKinds: Set<String> = emptySet(),
        operations: List<String> = emptyList(),
        appliedOverlays: List<String> = emptyList(),
        exifGpsPresent: Boolean? = null,
        watermarkHeuristicMatch: Boolean? = null,
        packItemCount: Int? = null,
        packOrder: List<PackOrderItem> = emptyList(),
        listingTitle: String? = null,
        listingDescription: String? = null,
    ) = ImageFacts(
        width = width,
        height = height,
        ratio = ratio,
        format = format,
        bytes = bytes,
        role = role,
        jpegQualityEstimate = jpegQualityEstimate,
        hasAlpha = hasAlpha,
        borderMinimumChannelOutsideItem = borderMinimumChannelOutsideItem,
        itemFillRatio = itemFillRatio,
        backgroundMeanChroma = backgroundMeanChroma,
        detectedTextBlocks = detectedTextBlocks,
        slideTextCategories = slideTextCategories,
        smallContrastRatio = smallContrastRatio,
        largeContrastRatio = largeContrastRatio,
        detectedSensitiveKinds = detectedSensitiveKinds,
        operations = operations,
        appliedOverlays = appliedOverlays,
        exifGpsPresent = exifGpsPresent,
        watermarkHeuristicMatch = watermarkHeuristicMatch,
        packItemCount = packItemCount,
        packOrder = packOrder,
        listingTitle = listingTitle,
        listingDescription = listingDescription,
    )

    private fun preset(
        id: String = "wb",
        editPolicy: EditPolicy = EditPolicy.FREE,
        originalOnlyAllowed: Set<String> = emptySet(),
        allowedOverlays: Set<String>? = null,
        formats: Set<String> = setOf("jpg", "png", "webp"),
        maxBytes: Long? = 10_485_760,
        minWidth: Int? = if (id == "wb") 700 else null,
        minHeight: Int? = if (id == "wb") 933 else null,
        maxSide: Int? = null,
        maxSidesSum: Int? = null,
        minLongSide: Int? = null,
        maxLongSide: Int? = null,
        maxCount: Int? = null,
        aspect: String? = null,
        backgroundPolicy: String? = null,
        minFillRatio: Double? = null,
        textOnMain: String? = null,
        noTransparentPng: Boolean? = null,
        minJpegQuality: Int? = null,
    ) = MarketplacePreset(
        id = id,
        name = id,
        regions = listOf("RU"),
        defaultLocale = "ru-RU",
        queue = 1,
        editPolicy = editPolicy,
        allowedOverlays = allowedOverlays,
        status = PresetStatus.SECONDARY,
        verifiedAt = "2026-10-12",
        sourceUrl = "https://seller.example.com/rules",
        image = ImageRules(
            formats = formats,
            maxBytes = maxBytes,
            minWidth = minWidth,
            minHeight = minHeight,
            minHeightAlternative = if (id == "wb") AlternativeLimit(900, "second help article") else null,
            minLongSide = minLongSide,
            maxLongSide = maxLongSide,
            maxSide = maxSide,
            maxSidesSum = maxSidesSum,
            maxCount = maxCount,
            aspect = aspect,
            minJpegQuality = minJpegQuality,
            noTransparentPng = noTransparentPng,
            raw = JsonObject(emptyMap()),
        ),
        main = MainRules(
            backgroundPolicy = backgroundPolicy,
            minFillRatio = minFillRatio,
            textOnMain = textOnMain,
            originalOnlyAllowed = originalOnlyAllowed,
            raw = JsonObject(emptyMap()),
        ),
        text = TextRules(
            prohibited = setOf("price_discount", "contacts_links", "qr_code"),
            contrastMin = ContrastThresholds(small = 4.5, large = 3.0),
            avoidColourPairs = emptyList(),
            raw = JsonObject(emptyMap()),
        ),
        listing = ListingRules(titleMax = 60, descriptionMax = 5000, raw = JsonObject(emptyMap())),
        sources = listOf(
            PresetSource(
                title = "Rules",
                url = "https://seller.example.com/rules",
                site = "seller.example.com",
                printedDate = "read",
                label = "fixture",
                firstParty = false,
                quotes = "fixture",
            ),
        ),
        notes = emptyList(),
        raw = JsonObject(emptyMap()),
    )
}

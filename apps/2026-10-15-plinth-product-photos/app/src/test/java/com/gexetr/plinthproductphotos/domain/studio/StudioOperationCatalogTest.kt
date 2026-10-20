package com.gexetr.plinthproductphotos.domain.studio

import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.ImageRules
import com.gexetr.plinthproductphotos.domain.presets.ListingRules
import com.gexetr.plinthproductphotos.domain.presets.MainRules
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.PresetSource
import com.gexetr.plinthproductphotos.domain.presets.PresetStatus
import com.gexetr.plinthproductphotos.domain.presets.TextRules
import com.gexetr.plinthproductphotos.domain.model.Provenance
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioOperationCatalogTest {
    @Test
    fun declaredOperationCostsResolveToAnnexCredits() {
        val costs = StudioOperationCatalog.operations.associate { operation ->
            operation.id.wireValue to StudioOperationCatalog.costFor(StudioOperationRequest(operation.id)).credits
        }

        assertEquals(
            mapOf(
                "O-01" to 0,
                "O-02" to 1,
                "O-03" to 0,
                "O-04" to 0,
                "O-05" to 0,
                "O-06" to 0,
                "O-07" to 2,
                "O-08" to 5,
                "O-09" to 2,
                "O-10" to 2,
                "O-11" to 1,
                "O-12" to 2,
                "O-13" to 5,
                "O-14" to 5,
                "O-15" to 1,
                "O-16" to 2,
                "O-17" to 0,
                "O-18" to 0,
            ),
            costs,
        )
        assertEquals(
            2,
            StudioOperationCatalog.costFor(
                StudioOperationRequest(StudioOperationId.UPSCALE, upscaleResolution = UpscaleResolution.FOUR_K),
            ).credits,
        )
        assertEquals(
            8,
            StudioOperationCatalog.costFor(
                StudioOperationRequest(
                    StudioOperationId.SERIES,
                    variantCount = 4,
                    seriesOperation = StudioOperationId.RECOLOUR,
                ),
            ).credits,
        )
        assertEquals(
            20,
            StudioOperationCatalog.costFor(
                StudioOperationRequest(
                    StudioOperationId.SERIES,
                    variantCount = 4,
                    seriesOperation = StudioOperationId.VIRTUAL_MODEL,
                ),
            ).credits,
        )
        assertEquals(
            Provenance.ENHANCED,
            StudioOperationCatalog.provenanceFor(
                StudioOperationRequest(
                    StudioOperationId.SERIES,
                    seriesOperation = StudioOperationId.VIRTUAL_MODEL,
                ),
            ),
        )
    }

    @Test
    fun originalOnlyPresetBlocksEveryEnhancedOperationAndReturnsOriginalPixels() {
        val source = Any()
        val preset = preset(EditPolicy.ORIGINAL_ONLY)

        StudioOperationCatalog.enhancedOperations.forEach { operation ->
            val offer = StudioOperationCatalog.offer(
                StudioOperationRequest(operation.id),
                balanceCredits = 100,
                preset = preset,
            )
            assertTrue("${operation.id} must be disabled", offer.access is OperationAccess.Unavailable)
            assertTrue(
                "${operation.id} must explain the marketplace restriction",
                OperationUnavailableReason.ORIGINAL_ONLY_MARKETPLACE in
                    (offer.access as OperationAccess.Unavailable).reasons,
            )
            val result = StudioOperationCatalog.execute(offer, source) { error("A gated operation must not run") }
            assertTrue("${operation.id} must not produce an enhanced result", result is OperationExecution.Unavailable)
        }

        val originalOffer = StudioOperationCatalog.offer(
            StudioOperationRequest(StudioOperationId.COMPOSE_ORIGINAL),
            balanceCredits = 100,
            preset = preset,
        )
        val returned = StudioOperationCatalog.execute(originalOffer, source) { error("Original-only must bypass edits") }
        assertTrue(returned is OperationExecution.ReturnedOriginal<*>)
        assertSame(source, (returned as OperationExecution.ReturnedOriginal<*>).source)
    }

    @Test
    fun enhancedOperationIsUnavailableByDefaultWithUnmeasuredThresholdReason() {
        val offer = StudioOperationCatalog.offer(
            StudioOperationRequest(StudioOperationId.RELIGHT),
            balanceCredits = 100,
        )

        assertTrue(offer.access is OperationAccess.Unavailable)
        assertTrue(
            OperationUnavailableReason.FIDELITY_THRESHOLDS_UNMEASURED in
                (offer.access as OperationAccess.Unavailable).reasons,
        )
    }

    @Test
    fun availableOperationReportsTypedCreditShortfallTogetherWithItsPrice() {
        val offer = StudioOperationCatalog.offer(
            request = StudioOperationRequest(StudioOperationId.CUT_OUT),
            balanceCredits = 0,
            config = StudioOperationConfig(enabledOperationIds = setOf(StudioOperationId.CUT_OUT)),
        )

        assertEquals(1, offer.cost.credits)
        assertTrue(offer.access is OperationAccess.InsufficientCredits)
        assertEquals(1, (offer.access as OperationAccess.InsufficientCredits).shortfallCredits)
        assertEquals(null, offer.balanceAfterCredits)
    }

    @Test
    fun slidePathIsUnavailableWhenLimitedPresetDoesNotAllowSlideOverlay() {
        val offer = StudioOperationCatalog.offer(
            request = StudioOperationRequest(StudioOperationId.SLIDE_RENDER),
            balanceCredits = 10,
            preset = preset(EditPolicy.LIMITED_OVERLAY, allowedOverlays = setOf("text")),
        )

        assertTrue(offer.access is OperationAccess.Unavailable)
        assertTrue(
            OperationUnavailableReason.SLIDE_OVERLAY_NOT_ALLOWED in
                (offer.access as OperationAccess.Unavailable).reasons,
        )
    }

    @Test
    fun measuredThresholdAndOperationConfigurationCanEnableAnEnhancedEntry() {
        val offer = StudioOperationCatalog.offer(
            request = StudioOperationRequest(StudioOperationId.RELIGHT),
            balanceCredits = 10,
            config = StudioOperationConfig(
                enabledOperationIds = setOf(StudioOperationId.RELIGHT),
                measuredFidelityOperationIds = setOf(StudioOperationId.RELIGHT),
            ),
        )

        assertEquals(OperationAccess.Available, offer.access)
        assertEquals(5, offer.cost.credits)
    }

    private fun preset(
        policy: EditPolicy,
        allowedOverlays: Set<String>? = null,
    ) = MarketplacePreset(
        id = "test-marketplace",
        name = "Test marketplace",
        regions = listOf("US"),
        defaultLocale = "en-US",
        queue = 1,
        editPolicy = policy,
        allowedOverlays = allowedOverlays,
        status = PresetStatus.FIRST_PARTY,
        verifiedAt = "2026-10-12",
        sourceUrl = "https://example.com/rules",
        sources = listOf(
            PresetSource(
                title = "Seller rules",
                url = "https://example.com/rules",
                site = "example.com",
                printedDate = "2026-10-12",
                label = "ON RECORD",
                firstParty = true,
                quotes = "Test fixture",
            ),
        ),
        image = ImageRules(formats = setOf("jpg"), raw = JsonObject(emptyMap())),
        main = MainRules(
            backgroundPolicy = "any",
            minFillRatio = null,
            textOnMain = "not-stated",
            originalOnlyAllowed = setOf("crop", "rotate", "exposure", "white-balance"),
            raw = JsonObject(emptyMap()),
        ),
        text = TextRules(
            prohibited = emptySet(),
            contrastMin = null,
            avoidColourPairs = emptyList(),
            raw = JsonObject(emptyMap()),
        ),
        listing = ListingRules(titleMax = null, descriptionMax = null, raw = JsonObject(emptyMap())),
        notes = emptyList(),
        raw = JsonObject(emptyMap()),
    )
}

package com.gexetr.plinthproductphotos.ui.home

import com.gexetr.plinthproductphotos.domain.studio.StudioOperationCatalog
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationId
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeToolboxTest {
    @Test
    fun toolboxContainsTheSixLiveToolsIncludingFreeListingCard() {
        assertEquals(
            setOf(
                StudioOperationId.CUT_OUT,
                StudioOperationId.COMPOSE_ORIGINAL,
                StudioOperationId.GENERATED_SCENE,
                StudioOperationId.RELIGHT,
                StudioOperationId.EXPAND_CANVAS,
                StudioOperationId.RECOLOUR,
            ),
            homeTools.map(HomeTool::operation).toSet(),
        )
    }

    @Test
    fun everyToolHasPurposeAndOnlyListingCardIsFree() {
        assertTrue(homeTools.all { it.title != 0 && it.description != 0 })
        val listingCard = homeTools.single { it.action == HomeToolAction.LISTING_CARD }
        assertEquals(StudioOperationId.COMPOSE_ORIGINAL, listingCard.operation)
        assertEquals(
            0,
            StudioOperationCatalog.costFor(StudioOperationRequest(listingCard.operation)).credits,
        )
        assertTrue(
            homeTools
                .filter { it.action != HomeToolAction.LISTING_CARD }
                .all {
                    StudioOperationCatalog.costFor(StudioOperationRequest(it.operation)).credits > 0
                },
        )
    }
}

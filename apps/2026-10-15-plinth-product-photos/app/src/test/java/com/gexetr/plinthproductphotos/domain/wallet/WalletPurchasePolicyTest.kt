package com.gexetr.plinthproductphotos.domain.wallet

import com.gexetr.capability.billing.BillingCatalog
import com.gexetr.capability.billing.ConsumablePack
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.billing.FakeBillingController
import com.gexetr.capability.billing.Fulfillment
import com.gexetr.capability.billing.InMemoryBillingStateStore
import com.gexetr.capability.billing.ProductId
import com.gexetr.capability.billing.PurchaseAuthority
import com.gexetr.capability.billing.PurchaseIntent
import com.gexetr.capability.billing.PurchaseLedger
import com.gexetr.capability.billing.PurchaseRecord
import com.gexetr.capability.billing.PurchaseState
import com.gexetr.capability.core.PlayKilnResult
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletPurchasePolicyTest {

    @Test
    fun duplicatePurchaseTokenCreditsOnce() = runBlocking {
        val product = ProductId("plinth_starter_90")
        val store = InMemoryBillingStateStore()
        var confirmations = 0
        val authority = authority { _, _, _ ->
            confirmations += 1
            PlayKilnResult.Success(Unit)
        }
        val ledger = PurchaseLedger(store, authority)
        val catalog = BillingCatalog(
            consumables = listOf(ConsumablePack(product, 90, "credits", Fulfillment.SERVER_COST)),
        )
        val record = PurchaseRecord("same-token", listOf(product.value), PurchaseState.PURCHASED, intentId = "i-1")

        ledger.settle(listOf(record), catalog)
        val replay = ledger.settle(listOf(record), catalog)

        assertEquals(90L, replay.wallet.balanceOf("credits"))
        assertEquals(1, confirmations)
    }

    @Test
    fun retryWithSameStateStoreDoesNotDoubleCredit() = runBlocking {
        val product = ProductId("plinth_starter_90")
        val durableStore = InMemoryBillingStateStore()
        val catalog = BillingCatalog(
            consumables = listOf(ConsumablePack(product, 90, "credits", Fulfillment.SERVER_COST)),
        )
        val record = PurchaseRecord("same-token", listOf(product.value), PurchaseState.PURCHASED, intentId = "i-1")

        PurchaseLedger(durableStore, authority { _, _, _ -> PlayKilnResult.Success(Unit) })
            .settle(listOf(record), catalog)
        val afterRestart = PurchaseLedger(durableStore, authority { _, _, _ ->
            throw AssertionError("A settled token must not be confirmed again")
        }).settle(listOf(record), catalog)

        assertEquals(90L, afterRestart.wallet.balanceOf("credits"))
    }

    @Test
    fun pendingPurchaseCreditsNothing() = runBlocking {
        val product = ProductId("plinth_starter_90")
        val ledger = PurchaseLedger(InMemoryBillingStateStore())
        val catalog = BillingCatalog(
            consumables = listOf(ConsumablePack(product, 90, "credits", Fulfillment.SERVER_COST)),
        )

        val settlement = ledger.settle(
            listOf(PurchaseRecord("pending", listOf(product.value), PurchaseState.PENDING)),
            catalog,
        )

        assertEquals(0L, settlement.wallet.balanceOf("credits"))
        assertTrue(settlement.consume.isEmpty())
        assertTrue(settlement.acknowledge.isEmpty())
    }

    @Test
    fun cancelledFakePurchaseDoesNotGrant() = runBlocking {
        val product = ProductId("plinth_unlock")
        val controller = FakeBillingController(
            authority = authority { _, _, _ -> PlayKilnResult.Success(Unit) },
            purchaseOutcomes = mapOf(
                product to listOf(PlayKilnResult.Failure(PlayKilnResult.Reason.UNKNOWN)),
            ),
        )
        controller.connect(BillingCatalog(inAppProducts = listOf(product)))

        val result = controller.purchase(product, PurchaseIntent("intent", "account", "profile"))

        assertTrue(result is PlayKilnResult.Failure)
        assertFalse(controller.entitlements.value.owns(product.key()))
    }

    @Test
    fun unconfirmedIntentDoesNotGrantUnlock() = runBlocking {
        val product = ProductId("plinth_unlock")
        val controller = FakeBillingController(
            authority = authority { _, _, _ ->
                PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)
            },
        )
        controller.connect(BillingCatalog(inAppProducts = listOf(product)))

        val result = controller.purchase(product, PurchaseIntent("intent", "account", "profile"))

        assertTrue(result is PlayKilnResult.Success)
        assertFalse(controller.entitlements.value.owns(product.key()))
        assertFalse(WalletAccessPolicy.isUnlocked(serverConfirmedUnlock = false))
    }

    @Test
    fun freeBatchLimitAllowsFiveAndBlocksSix() {
        assertTrue(WalletAccessPolicy.batchAccess(5, serverConfirmedUnlock = false).allowed)
        assertFalse(WalletAccessPolicy.batchAccess(6, serverConfirmedUnlock = false).allowed)
        assertTrue(WalletAccessPolicy.batchAccess(6, serverConfirmedUnlock = true).allowed)
    }

    @Test
    fun freeWatermarkCannotBeRemovedByLocalPlayEntitlement() {
        val unlock = ProductId("plinth_unlock")
        val locallyReported = Entitlements.EMPTY.granting(unlock.key(), asSubscription = false)

        assertFalse(
            WalletAccessPolicy.isUnlocked(
                serverConfirmedUnlock = false,
            ),
        )
        assertTrue(
            WalletAccessPolicy.isUnlocked(
                serverConfirmedUnlock = true,
            ),
        )
        assertTrue(locallyReported.owns(unlock.key()))
    }

    @Test
    fun originalOnlyPresetBlocksPaidAndWatermarkedPath() {
        val preset = preset(EditPolicy.ORIGINAL_ONLY)

        assertEquals(
            WalletAccessPolicy.ExportAccess.BLOCKED_BY_PRESET,
            WalletAccessPolicy.exportAccess(serverConfirmedUnlock = false, preset),
        )
        assertEquals(
            WalletAccessPolicy.ExportAccess.UNLOCKED,
            WalletAccessPolicy.exportAccess(serverConfirmedUnlock = true, preset),
        )
        assertFalse(WalletAccessPolicy.paidPathAllowed(preset))
    }

    @Test
    fun refundDebtBlocksPaidWorkButNotFreeLocalWork() {
        assertFalse(WalletAccessPolicy.canSpendCredits(balance = 90, refundDebt = 1, cost = 1))
        assertTrue(WalletAccessPolicy.canSpendCredits(balance = 90, refundDebt = 0, cost = 1))
        assertTrue(WalletAccessPolicy.canSpendCredits(balance = 0, refundDebt = 0, cost = 0))
    }

    private fun authority(
        confirm: suspend (String, ProductId, String) -> PlayKilnResult<Unit>,
    ): PurchaseAuthority = object : PurchaseAuthority {
        override suspend fun confirmPurchase(
            purchaseToken: String,
            product: ProductId,
        ): PlayKilnResult<Unit> = PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)

        override suspend fun confirmPurchase(
            purchaseToken: String,
            product: ProductId,
            intentId: String,
        ): PlayKilnResult<Unit> = confirm(purchaseToken, product, intentId)

        override suspend fun confirmSpend(unit: String, amount: Long): PlayKilnResult<Unit> =
            PlayKilnResult.Success(Unit)
    }

    private fun preset(policy: EditPolicy) = MarketplacePreset(
        id = "test",
        name = "Test",
        regions = emptyList(),
        defaultLocale = "en",
        queue = 0,
        editPolicy = policy,
        allowedOverlays = emptySet(),
        status = com.gexetr.plinthproductphotos.domain.presets.PresetStatus.FIRST_PARTY,
        verifiedAt = "",
        sourceUrl = "",
        sources = emptyList(),
        image = com.gexetr.plinthproductphotos.domain.presets.ImageRules(
            formats = emptySet(),
            raw = kotlinx.serialization.json.JsonObject(emptyMap()),
        ),
        main = com.gexetr.plinthproductphotos.domain.presets.MainRules(
            backgroundPolicy = null,
            minFillRatio = null,
            textOnMain = null,
            originalOnlyAllowed = emptySet(),
            raw = kotlinx.serialization.json.JsonObject(emptyMap()),
        ),
        text = com.gexetr.plinthproductphotos.domain.presets.TextRules(
            prohibited = emptySet(),
            contrastMin = null,
            avoidColourPairs = emptyList(),
            raw = kotlinx.serialization.json.JsonObject(emptyMap()),
        ),
        listing = com.gexetr.plinthproductphotos.domain.presets.ListingRules(
            titleMax = null,
            descriptionMax = null,
            raw = kotlinx.serialization.json.JsonObject(emptyMap()),
        ),
        notes = emptyList(),
        raw = kotlinx.serialization.json.JsonObject(emptyMap()),
    )
}

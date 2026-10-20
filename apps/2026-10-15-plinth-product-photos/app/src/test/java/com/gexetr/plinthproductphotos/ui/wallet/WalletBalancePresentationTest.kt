package com.gexetr.plinthproductphotos.ui.wallet

import com.gexetr.capability.billing.ProductId
import com.gexetr.plinthproductphotos.data.api.WalletResponse
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletBalancePresentationTest {
    private fun fundedState(balance: Int = 10): WalletScreenState =
        WalletScreenStateMapper.fromServer(WalletResponse(balance, 0, 0, 0, emptyList(), unlocked = false)).let { state ->
            state.copy(localizedPrices = state.productIds.billingCatalog().all().associate {
                it.value to "store-provided-price"
            })
        }

    @Test
    fun offlineStateNeverEnablesPurchase() {
        val state = fundedState().copy(offline = true)
        assertFalse(state.canPurchase(state.productIds.starter))
    }

    @Test
    fun purchaseNeedsAStorePriceAndAnAvailableBillingConnection() {
        val state = fundedState()
        val product = state.productIds.starter
        assertTrue(state.canPurchase(product))
        assertFalse(state.copy(localizedPrices = emptyMap()).canPurchase(product))
        assertFalse(state.copy(localizedPrices = mapOf(product.value to " ")).canPurchase(product))
        assertFalse(state.copy(purchaseAvailable = false).canPurchase(product))
        assertFalse(state.canPurchase(ProductId("not-a-wallet-product")))
    }

    @Test
    fun pendingAndConfirmingPurchasesCannotBeSubmittedAgain() {
        val state = fundedState()
        for (status in listOf(WalletPurchaseUiStatus.PENDING, WalletPurchaseUiStatus.CONFIRMING)) {
            assertFalse(state.copy(purchaseStatus = status).canPurchase(state.productIds.starter))
        }
    }

    @Test
    fun confirmedUnlockIsNotSoldAgainButCreditPacksRemainAvailable() {
        val state = fundedState().copy(serverConfirmedUnlock = true)
        assertFalse(state.canPurchase(state.productIds.unlock))
        assertTrue(state.canPurchase(state.productIds.starter))
    }

    @Test
    fun renderedPackQuantitiesMatchTheEconomicsContract() {
        val slot = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "economics.json").isFile }
        val ladder = Json.parseToJsonElement(File(slot, "economics.json").readText())
            .jsonObject.getValue("priceLadder").jsonObject
        val expected = listOf("starterPack", "studioPack", "proPack").map {
            ladder.getValue(it).jsonObject.getValue("credits").jsonPrimitive.long
        }
        val displayed = fundedState().productIds.billingCatalog().consumables.map { it.grants }
        assertEquals(expected, displayed)
    }
}

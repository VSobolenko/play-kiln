package com.gexetr.capability.billing

import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeBillingControllerTest {
    private val product = ProductId("premium")
    private val catalog = BillingCatalog(inAppProducts = listOf(product))

    @Test
    fun `restore returns seeded entitlements`() = runTest {
        val seeded = Entitlements(owned = setOf(ProductKey(product.value)))
        val fake = FakeBillingController(seedEntitlements = seeded)

        fake.connect(catalog)
        val restored = fake.restore()

        assertEquals(PlayKilnResult.Success(seeded), restored)
        assertEquals(listOf("connect", "restore"), fake.calls())
    }

    @Test
    fun `failed purchase leaves entitlements unchanged`() = runTest {
        val seeded = Entitlements(owned = setOf(ProductKey("existing")))
        val failure = PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK)
        val fake = FakeBillingController(
            seedEntitlements = seeded,
            purchaseOutcomes = mapOf(product to listOf(failure)),
        )
        fake.connect(catalog)

        val result = fake.purchase(product)

        assertEquals(failure, result)
        assertEquals(seeded, fake.entitlements.value)
        assertFalse(fake.entitlements.value.owns(ProductKey(product.value)))
    }

    @Test
    fun `successful subscription purchase grants active subscription`() = runTest {
        val subscription = ProductId("monthly")
        val fake = FakeBillingController()
        fake.connect(BillingCatalog(subscriptions = listOf(subscription)))

        val result = fake.purchase(subscription)

        assertTrue(result is PlayKilnResult.Success)
        assertTrue(fake.entitlements.value.hasActiveSubscription(ProductKey(subscription.value)))
    }

    @Test
    fun `intent purchase reports observed rather than granted when authority rejects`() = runTest {
        val rejected = object : PurchaseAuthority {
            override suspend fun confirmPurchase(purchaseToken: String, product: ProductId) =
                PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)

            override suspend fun confirmPurchase(
                purchaseToken: String,
                product: ProductId,
                intentId: String,
            ) = PlayKilnResult.Failure(PlayKilnResult.Reason.POLICY_BLOCKED)

            override suspend fun confirmSpend(unit: String, amount: Long) =
                PlayKilnResult.Success(Unit)
        }
        val fake = FakeBillingController(authority = rejected)
        fake.connect(BillingCatalog(inAppProducts = listOf(product)))

        val result = fake.purchase(
            product,
            PurchaseIntent("intent-rejected", "account-hash", "profile-hash"),
        )

        val resolution = (result as PlayKilnResult.Success<PurchaseResolution>).value
        assertEquals(PurchaseResolutionState.PURCHASE_OBSERVED, resolution.state)
        assertFalse(fake.entitlements.value.owns(product.key()))
    }

    @Test
    fun `a consumable purchase credits the wallet and never becomes an entitlement`() = runTest {
        val pack = ProductId("tokens_100")
        val fake = FakeBillingController()
        fake.connect(
            BillingCatalog(consumables = listOf(ConsumablePack(pack, grants = 100, unit = "tokens")))
        )

        fake.purchase(pack)

        assertEquals(100L, fake.wallet.value.balanceOf("tokens"))
        // Owning the pack would break the second purchase: a consumable is spent, not owned.
        assertFalse(fake.entitlements.value.owns(pack.key()))
    }

    @Test
    fun `the same pack can be bought repeatedly and each purchase credits`() = runTest {
        // The case the factory previously could not express at all: a repeatable purchase.
        val pack = ProductId("tokens_100")
        val fake = FakeBillingController()
        fake.connect(
            BillingCatalog(consumables = listOf(ConsumablePack(pack, grants = 100, unit = "tokens")))
        )

        fake.purchase(pack)
        fake.purchase(pack)
        fake.purchase(pack)

        assertEquals(300L, fake.wallet.value.balanceOf("tokens"))
    }

    @Test
    fun `spending reduces the wallet`() = runTest {
        val pack = ProductId("tokens_100")
        val fake = FakeBillingController(seedWallet = Wallet.EMPTY.credited("tokens", 30))
        fake.connect(
            BillingCatalog(consumables = listOf(ConsumablePack(pack, grants = 100, unit = "tokens")))
        )

        val result = fake.spend("tokens", 10)

        assertTrue(result is PlayKilnResult.Success)
        assertEquals(20L, fake.wallet.value.balanceOf("tokens"))
    }

    @Test
    fun `spending more than the balance fails and leaves the wallet untouched`() = runTest {
        val fake = FakeBillingController(seedWallet = Wallet.EMPTY.credited("tokens", 5))

        val result = fake.spend("tokens", 6)

        assertEquals(PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE), result)
        assertEquals(5L, fake.wallet.value.balanceOf("tokens"))
    }

    @Test
    fun `a catalog needing an authority is refused when none is supplied`() = runTest {
        val fake = FakeBillingController()
        val serverCost = BillingCatalog(
            consumables = listOf(
                ConsumablePack(ProductId("gen_10"), 10, "generations", Fulfillment.SERVER_COST)
            )
        )

        val result = fake.connect(serverCost)

        assertEquals(PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED), result)
    }
}

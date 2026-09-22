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
}

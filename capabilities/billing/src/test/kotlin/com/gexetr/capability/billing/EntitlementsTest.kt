package com.gexetr.capability.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementsTest {
    @Test
    fun `grace period subscription counts as owned and active`() {
        val key = ProductKey("premium")
        val entitlements = Entitlements(
            subscriptions = mapOf(key to SubscriptionStatus.GRACE_PERIOD),
        )

        assertTrue(entitlements.owns(key))
        assertTrue(entitlements.hasActiveSubscription(key))
    }

    @Test
    fun `expired subscription is not owned or active`() {
        val key = ProductKey("premium")
        val entitlements = Entitlements(
            subscriptions = mapOf(key to SubscriptionStatus.EXPIRED),
        )

        assertFalse(entitlements.owns(key))
        assertFalse(entitlements.hasActiveSubscription(key))
    }
}

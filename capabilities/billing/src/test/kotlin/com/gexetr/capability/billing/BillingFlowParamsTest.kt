package com.gexetr.capability.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillingFlowParamsTest {
    @Test
    fun `an intent binds account and profile ids into the built Play params`() {
        val intent = PurchaseIntent(
            intentId = "intent-123",
            obfuscatedAccountId = "account-hash",
            obfuscatedProfileId = "profile-hash",
        )

        val params = BillingFlowParamsFactory.create(emptyList(), intent)

        assertEquals("account-hash", params.obfuscatedAccountId)
        assertEquals("profile-hash", params.obfuscatedProfileId)
    }

    @Test
    fun `params without an intent leave Play obfuscation ids unset`() {
        val params = BillingFlowParamsFactory.create(emptyList(), null)

        assertNull(params.obfuscatedAccountId)
        assertNull(params.obfuscatedProfileId)
    }
}

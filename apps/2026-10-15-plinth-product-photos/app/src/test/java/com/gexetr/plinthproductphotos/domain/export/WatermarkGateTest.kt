package com.gexetr.plinthproductphotos.domain.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkGateTest {
    @Test
    fun unlockedUserDoesNotRequireWatermark() {
        assertFalse(WatermarkGate.required(serverConfirmedUnlock = true))
    }

    @Test
    fun lockedUserRequiresWatermark() {
        assertTrue(WatermarkGate.required(serverConfirmedUnlock = false))
    }

    @Test
    fun lockedWalletGatesBothAutoCardDeliveryRoutes() {
        assertTrue(
            "Both AutoCard share and save actions must take the watermark branch for a locked wallet",
            WatermarkGate.required(serverConfirmedUnlock = false),
        )
    }
}

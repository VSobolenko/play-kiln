package com.gexetr.capability.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityAvailabilityTest {
    @Test
    fun `only ready availability is usable`() {
        CapabilityAvailability.entries.forEach { availability ->
            assertEquals(
                availability == CapabilityAvailability.READY,
                availability.isUsable(),
            )
        }
    }

    @Test
    fun `capability ids expose the shared capability names`() {
        assertEquals(CapabilityId("ads"), CapabilityId.ADS)
        assertEquals(CapabilityId("billing"), CapabilityId.BILLING)
        assertEquals(CapabilityId("telemetry"), CapabilityId.TELEMETRY)
    }

    @Test
    fun `clock supports deterministic and system time sources`() {
        val fixedClock = Clock { 1_234L }
        val before = System.currentTimeMillis()

        assertEquals(1_234L, fixedClock.nowMillis())
        assertTrue(Clock.System.nowMillis() >= before)
        assertFalse(Clock.System.nowMillis() < 0L)
    }
}

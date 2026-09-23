package com.gexetr.capability.remoteconfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseRemoteConfigControllerTest {
    @Test
    fun `reads before initialize return caller defaults without accessing Firebase`() {
        val controller = FirebaseRemoteConfigController()

        assertTrue(controller.getBoolean("flag", true))
        assertFalse(controller.getBoolean("flag", false))
        assertEquals(42L, controller.getLong("count", 42L))
        assertEquals("fallback", controller.getString("label", "fallback"))
    }
}

package com.gexetr.capability.remoteconfig

import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeRemoteConfigControllerTest {
    private val config = RemoteConfigConfig(
        minimumFetchIntervalSeconds = 3_600,
        defaults = mapOf(
            "paywall_enabled" to false,
            "trial_days" to 7L,
            "offer_label" to "Standard",
        ),
    )

    @Test
    fun `declared defaults are returned before a fetch`() {
        val fake = FakeRemoteConfigController()

        fake.initialize(config)

        assertFalse(fake.getBoolean("paywall_enabled", true))
        assertEquals(7L, fake.getLong("trial_days", 0L))
        assertEquals("Standard", fake.getString("offer_label", "Fallback"))
    }

    @Test
    fun `successful fetch activates a seeded value over a default`() = runTest {
        val fake = FakeRemoteConfigController(
            seededValues = mapOf("trial_days" to 14L),
        )
        fake.initialize(config)

        val result = fake.fetch()

        assertTrue(result is PlayKilnResult.Success)
        assertEquals(14L, fake.getLong("trial_days", 0L))
    }

    @Test
    fun `failed fetch reports failure and leaves defaults active`() = runTest {
        val fake = FakeRemoteConfigController(
            seededValues = mapOf("paywall_enabled" to true),
            fetchOutcome = PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK),
        )
        fake.initialize(config)

        val result = fake.fetch()

        assertTrue(result is PlayKilnResult.Failure)
        assertFalse(fake.getBoolean("paywall_enabled", true))
    }

    @Test
    fun `unknown keys return caller defaults`() {
        val fake = FakeRemoteConfigController()
        fake.initialize(config)

        assertTrue(fake.getBoolean("missing_boolean", true))
        assertEquals(99L, fake.getLong("missing_long", 99L))
        assertEquals("caller", fake.getString("missing_string", "caller"))
    }
}

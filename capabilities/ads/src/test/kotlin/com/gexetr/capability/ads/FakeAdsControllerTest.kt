package com.gexetr.capability.ads

import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAdsControllerTest {
    private val config = AdsConfig(
        applicationId = "ca-app-pub-1234567890123456~1234567890",
        bannerUnitId = "ca-app-pub-1234567890123456/1234567890",
        rewardedUnitId = "ca-app-pub-1234567890123456/3456789012",
    )

    @Test
    fun `fake records calls and returns scripted reward`() = runTest {
        val fake = FakeAdsController(rewardEarned = true)

        fake.initialize(config)
        fake.requestConsent { null }

        assertTrue(fake.showRewarded())
        assertEquals(
            listOf("initialize", "requestConsent", "showRewarded"),
            fake.calls(),
        )
        assertEquals(CapabilityAvailability.READY, fake.availability.value)
    }

    @Test
    fun `consent failure keeps fake unusable`() = runTest {
        val fake = FakeAdsController(
            consentOutcome = PlayKilnResult.Failure(PlayKilnResult.Reason.NO_CONSENT),
        )

        fake.initialize(config)
        val result = fake.requestConsent { null }

        assertTrue(result is PlayKilnResult.Failure)
        assertFalse(fake.canRequestAds())
        assertEquals(CapabilityAvailability.FAILED, fake.availability.value)
    }
}

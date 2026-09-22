package com.gexetr.capability.ads

import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsConsentGateTest {
    @Test
    fun `ad requests stay blocked until consent resolves`() = runTest {
        val fake = FakeAdsController(consentOutcome = PlayKilnResult.Success(Unit))
        val config = AdsConfig(
            applicationId = "ca-app-pub-1234567890123456~1234567890",
            bannerUnitId = "ca-app-pub-1234567890123456/1234567890",
        )

        fake.initialize(config)

        assertFalse(fake.canRequestAds())
        assertTrue(fake.showInterstitial() is PlayKilnResult.Failure)

        fake.requestConsent { null }

        assertTrue(fake.canRequestAds())
        assertTrue(fake.showInterstitial() is PlayKilnResult.Success)
    }
}

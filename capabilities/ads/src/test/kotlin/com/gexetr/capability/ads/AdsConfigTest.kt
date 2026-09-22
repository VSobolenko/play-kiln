package com.gexetr.capability.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsConfigTest {
    @Test
    fun `valid configuration has no problems`() {
        val config = AdsConfig(
            applicationId = "ca-app-pub-1234567890123456~1234567890",
            bannerUnitId = "ca-app-pub-1234567890123456/1234567890",
            interstitialUnitId = "ca-app-pub-1234567890123456/2345678901",
            rewardedUnitId = "ca-app-pub-1234567890123456/3456789012",
        )

        assertTrue(config.validate().isEmpty())
    }

    @Test
    fun `blank required ids are reported`() {
        val problems = AdsConfig(applicationId = " ", bannerUnitId = "").validate()

        assertEquals(2, problems.size)
        assertTrue(problems.any { it.contains("application ID", ignoreCase = true) })
        assertTrue(problems.any { it.contains("banner", ignoreCase = true) })
    }

    @Test
    fun `ad unit ids must use the AdMob prefix`() {
        val problems = AdsConfig(
            applicationId = "ca-app-pub-1234567890123456~1234567890",
            bannerUnitId = "banner",
            interstitialUnitId = "interstitial",
            rewardedUnitId = "rewarded",
        ).validate()

        assertEquals(3, problems.size)
        assertTrue(problems.all { it.contains("ca-app-pub-") })
    }
}

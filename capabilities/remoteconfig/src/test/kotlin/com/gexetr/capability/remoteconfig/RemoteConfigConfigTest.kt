package com.gexetr.capability.remoteconfig

import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigConfigTest {
    @Test
    fun `negative fetch interval is rejected`() {
        val problems = RemoteConfigConfig(minimumFetchIntervalSeconds = -1).validate()

        assertTrue(problems.any { it.contains("fetch interval", ignoreCase = true) })
    }
}

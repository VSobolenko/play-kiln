package com.gexetr.plinthproductphotos.ui.studio

import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.JobState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioPollingRecoveryTest {
    @Test
    fun transientNetworkFailureThenSucceededStatusKeepsPollingAndDeliversTheResult() {
        val policy = StudioPollingRecovery()

        assertEquals(StudioPollDecision.Retry(2_000L), policy.afterNetworkFailure(ApiFailure.Network("socket closed")))
        assertEquals(StudioPollDecision.Continue, policy.afterStatus(JobState.SUCCEEDED))
        assertEquals(0, policy.consecutiveNetworkFailureMs)
    }

    @Test
    fun consecutiveNetworkFailuresEventuallyGiveAnHonestRecoverableMessage() {
        val policy = StudioPollingRecovery()
        var decision: StudioPollDecision = StudioPollDecision.Continue

        while (decision !is StudioPollDecision.ConnectionLost) {
            decision = policy.afterNetworkFailure(ApiFailure.Network("timeout"))
        }

        assertTrue(decision.message.contains("connection was lost while the edit was still running"))
        assertTrue(decision.message.contains("result may still be available"))
        assertFalse(decision.message.contains("NETWORK_FAILURE"))
    }

    @Test
    fun serverSideFailedStatusTerminatesWithoutNetworkRetry() {
        val policy = StudioPollingRecovery()

        assertEquals(StudioPollDecision.TerminalStatus, policy.afterStatus(JobState.FAILED))
        assertEquals(0, policy.consecutiveNetworkFailureMs)
    }
}

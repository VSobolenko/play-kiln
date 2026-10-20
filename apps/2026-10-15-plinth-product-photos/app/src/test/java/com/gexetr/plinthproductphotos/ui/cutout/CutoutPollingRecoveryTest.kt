package com.gexetr.plinthproductphotos.ui.cutout

import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.JobState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CutoutPollingRecoveryTest {
    @Test
    fun transientPollNetworkFailureThenSucceededStatusRetriesAndDeliversTheResult() {
        val recovery = CutoutPollingRecovery()

        assertEquals(CutoutPollDecision.Retry(2_000L), recovery.afterNetworkFailure(ApiFailure.Network("socket closed")))
        assertEquals(CutoutPollDecision.Continue, recovery.afterStatus(JobState.SUCCEEDED))
        assertEquals(0, recovery.consecutiveNetworkFailureMs)
    }

    @Test
    fun resultDownloadNetworkFailureIsRetriedInsteadOfDiscardingTheFinishedJob() {
        val recovery = CutoutPollingRecovery()

        assertEquals(CutoutPollDecision.Retry(2_000L), recovery.afterNetworkFailure(ApiFailure.Network("read timed out")))
        assertEquals(CutoutPollDecision.Continue, recovery.afterSuccessfulResponse())
        assertEquals(0, recovery.consecutiveNetworkFailureMs)
    }

    @Test
    fun consecutiveNetworkFailuresEndWithAnHonestRecoverableMessage() {
        val recovery = CutoutPollingRecovery()
        var decision: CutoutPollDecision = CutoutPollDecision.Continue

        while (decision !is CutoutPollDecision.ConnectionLost) {
            decision = recovery.afterNetworkFailure(ApiFailure.Network("timeout"))
        }

        assertTrue(decision.message.contains("connection was lost while the edit was still running"))
        assertTrue(decision.message.contains("result may still be available"))
        assertFalse(decision.message.contains("NETWORK_FAILURE"))
    }

    @Test
    fun serverSideFailedStatusRemainsTerminalWithoutNetworkRetry() {
        val recovery = CutoutPollingRecovery()

        assertEquals(CutoutPollDecision.TerminalStatus, recovery.afterStatus(JobState.FAILED))
        assertEquals(0, recovery.consecutiveNetworkFailureMs)
    }

    @Test
    fun jobPastTheOld150SecondLimitRemainsEligibleForPollingBeforeTheNewDeadline() {
        assertTrue(CutoutPollingDeadline.allowsPollNumber(76))
        assertTrue(CutoutPollingDeadline.allowsPollNumber(CutoutPollingDeadline.maxPolls))
        assertFalse(CutoutPollingDeadline.allowsPollNumber(CutoutPollingDeadline.maxPolls + 1))
    }
}

package com.gexetr.plinthproductphotos.ui.cutout

import com.gexetr.plinthproductphotos.data.api.ApiJobError
import com.gexetr.plinthproductphotos.data.api.ApiJobErrorCode
import com.gexetr.plinthproductphotos.data.api.JobState
import com.gexetr.plinthproductphotos.data.api.JobStatus
import com.gexetr.plinthproductphotos.domain.wallet.JobMoneyOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CutoutTerminalOutcomeTest {
    @Test
    fun guardRejectedJobProjectsReleaseAndRequestsWalletRefresh() {
        val projection = projectTerminalJobOutcome(
            JobStatus(
                state = JobState.FAILED,
                charged = 0,
                reserved = 2,
                released = 2,
                error = ApiJobError(ApiJobErrorCode.GUARD_REJECTED.wireCode, "Fidelity guard rejected result"),
            ),
        )

        assertTrue(projection.shouldRefreshWallet)
        assertEquals(JobMoneyOutcome.Released(2), projection.moneyOutcome)
    }

    @Test
    fun providerFailureProjectsReleaseAndRequestsWalletRefresh() {
        val projection = projectTerminalJobOutcome(
            JobStatus(
                state = JobState.FAILED,
                charged = 0,
                reserved = 3,
                released = 3,
                error = ApiJobError(ApiJobErrorCode.PROVIDER_FAILED.wireCode, "Provider failed"),
            ),
        )

        assertTrue(projection.shouldRefreshWallet)
        assertEquals(JobMoneyOutcome.Released(3), projection.moneyOutcome)
    }

    @Test
    fun cancelledJobProjectsReleaseAndRequestsWalletRefresh() {
        val projection = projectTerminalJobOutcome(
            JobStatus(
                state = JobState.CANCELLED,
                charged = 0,
                reserved = 1,
                released = 1,
            ),
        )

        assertTrue(projection.shouldRefreshWallet)
        assertEquals(JobMoneyOutcome.Released(1), projection.moneyOutcome)
    }

    @Test
    fun successfulJobDoesNotProjectARefundOrWalletRefresh() {
        val projection = projectTerminalJobOutcome(
            JobStatus(
                state = JobState.SUCCEEDED,
                charged = 2,
                reserved = 2,
            ),
        )

        assertFalse(projection.shouldRefreshWallet)
        assertEquals(JobMoneyOutcome.AwaitingReceipt, projection.moneyOutcome)
    }

    @Test
    fun chargedFailedJobDoesNotProjectARefund() {
        val projection = projectTerminalJobOutcome(
            JobStatus(
                state = JobState.FAILED,
                charged = 1,
                reserved = 2,
                released = 1,
                error = ApiJobError(ApiJobErrorCode.PROVIDER_FAILED.wireCode, "Provider failed"),
            ),
        )

        assertTrue(projection.shouldRefreshWallet)
        assertEquals(JobMoneyOutcome.ReleaseUnconfirmed, projection.moneyOutcome)
    }
}

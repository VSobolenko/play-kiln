package com.gexetr.plinthproductphotos.ui.wallet

import com.gexetr.plinthproductphotos.data.api.JobState
import com.gexetr.plinthproductphotos.data.api.JobStatus
import com.gexetr.plinthproductphotos.data.api.WalletEntry
import com.gexetr.plinthproductphotos.data.api.WalletResponse
import com.gexetr.plinthproductphotos.domain.wallet.JobMoneyOutcome
import com.gexetr.plinthproductphotos.domain.wallet.JobMoneyPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletScreenStateMapperTest {

    @Test
    fun mapsServerWalletAndLedgerWithTheServerUnlockFlagFalse() {
        val state = WalletScreenStateMapper.fromServer(
            WalletResponse(
                balance = 41,
                reserved = 7,
                refundDebt = 2,
                purchaseHold = 3,
                entries = listOf(WalletEntry(kind = "job_charge", delta = -4, createdAt = 10)),
                unlocked = false,
            ),
        )

        assertEquals(41, state.balance)
        assertEquals(7, state.reserved)
        assertEquals(2, state.refundDebt)
        assertEquals(listOf(WalletHistoryRow("job_charge", -4)), state.history)
        assertFalse(state.offline)
        assertTrue(state.purchaseAvailable)
        assertFalse(state.serverConfirmedUnlock)
    }

    @Test
    fun offlineStateCannotBeConfusedWithAnEmptyServerWallet() {
        val emptyWallet = WalletScreenStateMapper.fromServer(
            WalletResponse(0, 0, 0, 0, emptyList(), unlocked = false),
        )
        val offline = WalletScreenStateMapper.offline()

        assertEquals(0, emptyWallet.balance)
        assertFalse(emptyWallet.offline)
        assertTrue(emptyWallet.purchaseAvailable)
        assertEquals(0, offline.balance)
        assertTrue(offline.offline)
        assertFalse(offline.purchaseAvailable)
        assertFalse(offline.serverConfirmedUnlock)
    }

    @Test
    fun unavailableBalanceOffersRefreshWhileKnownBalanceDoesNot() {
        val emptyWallet = WalletScreenStateMapper.fromServer(
            WalletResponse(0, 0, 0, 0, emptyList(), unlocked = false),
        )
        val offline = WalletScreenStateMapper.offline()

        assertTrue(offline.shouldOfferBalanceRefresh)
        assertFalse(emptyWallet.shouldOfferBalanceRefresh)
    }

    @Test
    fun mapsReleasedTerminalJobUsingTheServerReleaseFields() {
        val job = WalletScreenStateMapper.mapJob(
            jobId = "job-1",
            status = JobStatus(
                state = JobState.FAILED,
                charged = 0,
                reserved = 5,
                released = 5,
            ),
        )

        assertEquals("job-1", job.jobId)
        assertEquals(
            JobMoneyOutcome.Released(5),
            JobMoneyPresentation.forJob(job.status, job.receipt),
        )
    }

    @Test
    fun doesNotReportUnconfirmedReleaseAsNotCharged() {
        val job = WalletScreenStateMapper.mapJob(
            jobId = "job-2",
            status = JobStatus(
                state = JobState.CANCELLED,
                charged = 0,
                reserved = 5,
                released = 0,
            ),
        )

        assertEquals(
            JobMoneyOutcome.ReleaseUnconfirmed,
            JobMoneyPresentation.forJob(job.status, job.receipt),
        )
    }
}

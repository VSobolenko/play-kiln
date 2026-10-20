package com.gexetr.plinthproductphotos.domain.wallet

import com.gexetr.plinthproductphotos.data.api.DeliveryReceiptResponse
import com.gexetr.plinthproductphotos.data.api.JobState
import com.gexetr.plinthproductphotos.data.api.JobStatus

sealed interface JobMoneyOutcome {
    data class Released(val credits: Int) : JobMoneyOutcome
    data class Settled(val credits: Int) : JobMoneyOutcome
    data object AwaitingReceipt : JobMoneyOutcome
    data object ReleaseUnconfirmed : JobMoneyOutcome
}

object JobMoneyPresentation {
    fun settlement(receipt: DeliveryReceiptResponse?): JobMoneyOutcome.Settled? =
        receipt?.takeIf { it.state == "SETTLED" }?.let { JobMoneyOutcome.Settled(it.charged) }

    fun forJob(
        status: JobStatus,
        receipt: DeliveryReceiptResponse?,
    ): JobMoneyOutcome? {
        if (status.notCharged) return JobMoneyOutcome.Released(status.released)
        return when (status.state) {
            JobState.FAILED,
            JobState.CANCELLED -> JobMoneyOutcome.ReleaseUnconfirmed
            JobState.SUCCEEDED -> settlement(receipt) ?: JobMoneyOutcome.AwaitingReceipt
            JobState.QUEUED,
            JobState.RUNNING -> null
        }
    }
}

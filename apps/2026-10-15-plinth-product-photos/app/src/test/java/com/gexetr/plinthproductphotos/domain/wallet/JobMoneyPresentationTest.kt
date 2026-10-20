package com.gexetr.plinthproductphotos.domain.wallet

import com.gexetr.plinthproductphotos.data.api.ApiJobError
import com.gexetr.plinthproductphotos.data.api.ApiJobErrorCode
import com.gexetr.plinthproductphotos.data.api.DeliveryReceiptResponse
import com.gexetr.plinthproductphotos.data.api.JobState
import com.gexetr.plinthproductphotos.data.api.JobStatus
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.pipeline.EnhancedOperation
import com.gexetr.plinthproductphotos.domain.pipeline.FidelityDecision
import com.gexetr.plinthproductphotos.domain.pipeline.FidelityGuard
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobMoneyPresentationTest {
    @Test
    fun driftingGuardResultIsPresentedAsReleasedOnlyWhenTheServerReportsFullRelease() {
        val source = PixelBuffer.solid(9, 9, PixelBuffer.rgba(30, 80, 140))
        val changed = PixelBuffer.solid(9, 9, PixelBuffer.rgba(230, 10, 10))
        val mask = Mask(ByteArray(81) { 0xff.toByte() })
        val guard = FidelityGuard.evaluate(source, mask, changed, EnhancedOperation.RELIGHT)
        val terminalStatus = JobStatus(
            state = JobState.FAILED,
            charged = 0,
            reserved = 2,
            released = 2,
            error = ApiJobError(ApiJobErrorCode.GUARD_REJECTED.wireCode, "Fidelity guard rejected result"),
        )

        assertEquals(FidelityDecision.REJECTED, guard.decision)
        assertEquals(ApiJobErrorCode.GUARD_REJECTED.wireCode, terminalStatus.error?.code)
        assertEquals(0, terminalStatus.charged)
        assertTrue(terminalStatus.notCharged)
        assertEquals(JobMoneyOutcome.Released(2), JobMoneyPresentation.forJob(terminalStatus, null))
    }

    @Test
    fun faithfulResultAndReplayedReceiptKeepOnePerJobCharge() {
        val source = PixelBuffer.solid(9, 9, PixelBuffer.rgba(30, 80, 140))
        val mask = Mask(ByteArray(81) { 0xff.toByte() })
        val guard = FidelityGuard.evaluate(source, mask, source, EnhancedOperation.RELIGHT)
        val status = JobStatus(state = JobState.SUCCEEDED, charged = 2, reserved = 2)
        val receipt = DeliveryReceiptResponse(charged = 2, state = "SETTLED")

        assertEquals(FidelityDecision.DELIVERED, guard.decision)
        assertEquals(JobMoneyOutcome.Settled(2), JobMoneyPresentation.forJob(status, receipt))
        assertEquals(JobMoneyOutcome.Settled(2), JobMoneyPresentation.forJob(status, receipt))
    }

    @Test
    fun repeatedReleaseStatusIsPresentedAsOneFixedReturnedAmount() {
        val status = JobStatus(
            state = JobState.FAILED,
            charged = 0,
            reserved = 3,
            released = 3,
            error = ApiJobError(ApiJobErrorCode.PROVIDER_FAILED.wireCode, "Provider timed out"),
        )

        val first = JobMoneyPresentation.forJob(status, null)
        val replay = JobMoneyPresentation.forJob(status, null)

        assertEquals(JobMoneyOutcome.Released(3), first)
        assertEquals(first, replay)
    }

    @Test
    fun missingOrWrongReceiptDoesNotPresentASettlement() {
        val status = JobStatus(state = JobState.SUCCEEDED, charged = 0, reserved = 2)

        assertEquals(
            JobMoneyOutcome.AwaitingReceipt,
            JobMoneyPresentation.forJob(status, null),
        )
        assertEquals(
            JobMoneyOutcome.AwaitingReceipt,
            JobMoneyPresentation.forJob(status, DeliveryReceiptResponse(charged = 2, state = "REJECTED")),
        )
        assertNull(JobMoneyPresentation.settlement(DeliveryReceiptResponse(charged = 2, state = "REJECTED")))
    }
}

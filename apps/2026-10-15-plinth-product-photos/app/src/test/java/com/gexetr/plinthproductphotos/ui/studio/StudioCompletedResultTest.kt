package com.gexetr.plinthproductphotos.ui.studio

import com.gexetr.plinthproductphotos.data.api.DeliveryReceipt
import com.gexetr.plinthproductphotos.data.api.OperationId
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioCompletedResultTest {
    @Test
    fun deliveredResultDoesNotLockRelightOrGenerateAtExactCostOrTenCredits() {
        val state = StudioState(
            photoId = "selected-photo",
            hasPending = true,
            deliveryConfirmed = true,
            phase = StudioPhase.DONE,
        )

        assertTrue("A settled result must not disable the tool carousel", state.canChooseTool)
        for (balance in listOf(5, 10)) {
            val eligibility = studioConfirmationPreview(
                displayedCredits = 5,
                currentCredits = 5,
                balance = balance,
                operationEnabled = true,
                killSwitchClear = true,
                dailyCapClear = true,
                rowBlocked = false,
                providerReady = true,
                hasPhoto = state.photoId != null,
                promptValid = true,
                busy = state.busy,
                hasPending = state.blocksNewOperation,
            )
            assertTrue("Balance $balance must enable Generate at cost 5", eligibility.canConfirm)
        }
    }

    @Test
    fun unresolvedDeliveryStillLocksToolSelection() {
        assertFalse(StudioState(hasPending = true, phase = StudioPhase.CONFIRMING).canChooseTool)
        assertFalse(StudioState(hasPending = true, phase = StudioPhase.FAILED).canChooseTool)
        assertFalse(StudioState(busy = true, deliveryConfirmed = true).canChooseTool)
    }

    @Test
    fun persistedSettledResultCanBeReplacedButUnpaidOrMismatchedDeliveryCannot() {
        val pending = StudioPending(
            requestId = "previous-request",
            photoId = "selected-photo",
            walletId = "wallet",
            operation = OperationId.GENERATED_SCENE,
            credits = 2,
            params = JsonObject(emptyMap()),
            jobId = "previous-job",
            resultPath = "result.received",
            receipt = DeliveryReceipt("attempt", 100L, "hash"),
            charged = 2,
        )

        assertTrue(pending.canReplace)
        assertTrue(pending.copy(applied = true).canReplace)
        assertFalse(pending.copy(charged = null).canReplace)
        assertFalse(pending.copy(charged = 1).canReplace)
        assertFalse(pending.copy(receipt = null).canReplace)
        assertFalse(pending.copy(resultPath = null).canReplace)
        assertTrue(pending.copy(terminal = true, charged = null).canReplace)
    }
}

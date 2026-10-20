package com.gexetr.plinthproductphotos.ui.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioConfirmationPreviewTest {
    @Test
    fun unknownOrInsufficientBalanceNeverProjectsRemainderOrAllowsConfirmation() {
        val unknown = preview(balance = null)
        val insufficient = preview(balance = 3)

        assertFalse(unknown.canConfirm)
        assertEquals(null, unknown.remainingCredits)
        assertFalse(insufficient.canConfirm)
        assertEquals(null, insufficient.remainingCredits)
    }

    @Test
    fun confirmationRequiresTheDisplayedQuoteToMatchTheCurrentQuote() {
        val changed = preview(currentCredits = 6)

        assertFalse(changed.quoteMatches)
        assertFalse(changed.canConfirm)
        assertEquals(5, changed.remainingCredits)
    }

    @Test
    fun eligibleConfirmationProjectsRemainderFromCurrentBalance() {
        val result = preview(balance = 9)

        assertTrue(result.canConfirm)
        assertTrue(result.quoteMatches)
        assertEquals(5, result.remainingCredits)
    }

    @Test
    fun existingBusyAndServerBlockersStillDisableConfirmation() {
        assertFalse(preview(busy = true).canConfirm)
        assertFalse(preview(killSwitchClear = false).canConfirm)
        assertFalse(preview(dailyCapClear = false).canConfirm)
    }

    @Test
    fun operationAndInputBlockersStillDisableConfirmation() {
        assertFalse(preview(operationEnabled = false).canConfirm)
        assertFalse(preview(rowBlocked = true).canConfirm)
        assertFalse(preview(providerReady = false).canConfirm)
        assertFalse(preview(hasPhoto = false).canConfirm)
        assertFalse(preview(promptValid = false).canConfirm)
        assertFalse(preview(hasPending = true).canConfirm)
    }

    private fun preview(
        balance: Int? = 9,
        currentCredits: Int? = 4,
        busy: Boolean = false,
        killSwitchClear: Boolean = true,
        dailyCapClear: Boolean = true,
        operationEnabled: Boolean = true,
        rowBlocked: Boolean = false,
        providerReady: Boolean = true,
        hasPhoto: Boolean = true,
        promptValid: Boolean = true,
        hasPending: Boolean = false,
    ) = studioConfirmationPreview(
        displayedCredits = 4,
        currentCredits = currentCredits,
        balance = balance,
        operationEnabled = operationEnabled,
        killSwitchClear = killSwitchClear,
        dailyCapClear = dailyCapClear,
        rowBlocked = rowBlocked,
        providerReady = providerReady,
        hasPhoto = hasPhoto,
        promptValid = promptValid,
        busy = busy,
        hasPending = hasPending,
    )
}

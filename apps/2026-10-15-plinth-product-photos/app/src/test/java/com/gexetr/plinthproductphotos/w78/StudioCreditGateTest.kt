package com.gexetr.plinthproductphotos.w78

import com.gexetr.plinthproductphotos.ui.studio.studioConfirmationPreview
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioCreditGateTest {
    @Test
    fun validQuoteWithInsufficientBalanceCanOpenTopUpPromptButCannotGenerate() {
        val result = studioConfirmationPreview(
            displayedCredits = 4,
            currentCredits = 4,
            balance = 2,
            operationEnabled = true,
            killSwitchClear = true,
            dailyCapClear = true,
            rowBlocked = false,
            providerReady = true,
            hasPhoto = true,
            promptValid = true,
            busy = false,
            hasPending = false,
        )

        assertTrue(result.canPressGenerate)
        assertFalse(result.canConfirm)
    }

    @Test
    fun unknownBalanceCannotStartOrReachTheInsufficientBalancePrompt() {
        val result = studioConfirmationPreview(
            displayedCredits = 4,
            currentCredits = 4,
            balance = null,
            operationEnabled = true,
            killSwitchClear = true,
            dailyCapClear = true,
            rowBlocked = false,
            providerReady = true,
            hasPhoto = true,
            promptValid = true,
            busy = false,
            hasPending = false,
        )

        assertFalse(result.canPressGenerate)
        assertFalse(result.canConfirm)
    }

    @Test
    fun stalePriceCannotStartOrReachTheInsufficientBalancePrompt() {
        val result = studioConfirmationPreview(
            displayedCredits = 4,
            currentCredits = 5,
            balance = 2,
            operationEnabled = true,
            killSwitchClear = true,
            dailyCapClear = true,
            rowBlocked = false,
            providerReady = true,
            hasPhoto = true,
            promptValid = true,
            busy = false,
            hasPending = false,
        )

        assertFalse(result.canPressGenerate)
        assertFalse(result.canConfirm)
    }
}

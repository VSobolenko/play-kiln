package com.gexetr.plinthproductphotos.ui.autocard

import com.gexetr.plinthproductphotos.data.api.ApiConfigResponse
import com.gexetr.plinthproductphotos.data.api.OperationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCardCreditQuoteTest {
    @Test
    fun usesServerCreditsAndCurrentWalletWithoutSubstitutingAPrice() {
        val quote = autoCardCreditQuote(config(credits = 37), balance = 82)!!
        assertEquals(37, quote.credits)
        assertEquals(82, quote.balance)
        assertTrue(quote.canRecognize)
        assertEquals(19, autoCardCreditQuote(config(credits = 19), 82)!!.credits)
    }

    @Test
    fun absentOrInvalidQuoteDoesNotBecomeAFreeOperation() {
        assertNull(autoCardCreditQuote(config().copy(ops = emptyMap()), 82))
        assertNull(autoCardCreditQuote(config(credits = -1), 82))
        assertNull(autoCardCreditQuote(config(), -1))
        assertTrue(autoCardCreditQuote(config(credits = 0), 0)!!.canRecognize)
    }

    @Test
    fun respectsAvailabilityAndBalanceWithoutHidingTheQuotedCost() {
        assertFalse(autoCardCreditQuote(config(enabled = false), 82)!!.canRecognize)
        assertFalse(autoCardCreditQuote(config().copy(killSwitch = true), 82)!!.canRecognize)
        assertFalse(autoCardCreditQuote(config().copy(dailyCapReached = true), 82)!!.canRecognize)
        val insufficient = autoCardCreditQuote(config(credits = 37), 36)!!
        assertEquals(37, insufficient.credits)
        assertFalse(insufficient.canRecognize)
    }

    private fun config(credits: Int = 37, enabled: Boolean = true) = ApiConfigResponse(
        ops = mapOf("O-01" to OperationConfig(credits, enabled)),
        starterCredits = 0,
        killSwitch = false,
        minAppVersion = "1",
        dailyCapReached = false,
    )
}

package com.gexetr.capability.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletTest {

    @Test
    fun `an unknown unit has a zero balance`() {
        assertEquals(0L, Wallet.EMPTY.balanceOf("tokens"))
    }

    @Test
    fun `crediting accumulates and debiting removes`() {
        val wallet = Wallet.EMPTY.credited("tokens", 100).credited("tokens", 50)

        assertEquals(150L, wallet.balanceOf("tokens"))
        assertEquals(100L, wallet.debited("tokens", 50)?.balanceOf("tokens"))
    }

    @Test
    fun `units are independent`() {
        val wallet = Wallet.EMPTY.credited("tokens", 10).credited("credits", 3)

        assertEquals(setOf("tokens", "credits"), wallet.units())
        assertEquals(10L, wallet.balanceOf("tokens"))
        assertEquals(3L, wallet.balanceOf("credits"))
    }

    @Test
    fun `debiting more than the balance is refused rather than clamped`() {
        // The whole point of returning null: a spend that cannot be covered is a caller bug, and
        // clamping would silently give away or destroy paid credit.
        val wallet = Wallet.EMPTY.credited("tokens", 5)

        assertNull(wallet.debited("tokens", 6))
    }

    @Test
    fun `canAfford reflects the balance`() {
        val wallet = Wallet.EMPTY.credited("tokens", 5)

        assertTrue(wallet.canAfford("tokens", 5))
        assertFalse(wallet.canAfford("tokens", 6))
        assertFalse(wallet.canAfford("other", 1))
    }

    @Test
    fun `credit and debit reject non-positive amounts`() {
        var creditThrew = false
        var debitThrew = false

        try {
            Wallet.EMPTY.credited("tokens", 0)
        } catch (expected: IllegalArgumentException) {
            creditThrew = true
        }
        try {
            Wallet.EMPTY.debited("tokens", -1)
        } catch (expected: IllegalArgumentException) {
            debitThrew = true
        }

        assertTrue("crediting zero should be rejected", creditThrew)
        assertTrue("debiting a negative amount should be rejected", debitThrew)
    }
}

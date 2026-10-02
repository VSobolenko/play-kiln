package com.gexetr.lawnroundlog.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The promises about money: a balance is what the mowed visits say minus what was paid, it is
 * derived rather than stored, and only work that was actually done is billable.
 */
class BalanceTest {

    @Test
    fun twoMowedVisitsAtFortyEachMinusAFortyDollarPaymentLeaveFortyOwed() {
        val state = RoundState(
            customers = listOf(ada),
            visits = listOf(
                mowed(ada.id, "2026-10-01", 40.0),
                mowed(ada.id, "2026-10-08", 40.0),
            ),
            payments = listOf(payment(ada.id, "2026-10-09", 40.0)),
        )

        assertEquals(40.0, Balance.balanceFor(state, ada.id), 0.0001)
        assertEquals(40.0, Balance.totalOwed(state), 0.0001)
    }

    @Test
    fun removingTheRecordedPaymentRestoresTheEightyDollarBalance() {
        val paid = RoundState(
            customers = listOf(ada),
            visits = listOf(
                mowed(ada.id, "2026-10-01", 40.0),
                mowed(ada.id, "2026-10-08", 40.0),
            ),
            payments = listOf(payment(ada.id, "2026-10-09", 40.0)),
        )

        val paymentRemoved = Round.setPayment(paid, ada.id, "2026-10-09", 0.0)

        assertEquals(80.0, Balance.balanceFor(paymentRemoved, ada.id), 0.0001)
        assertEquals(80.0, Balance.totalOwed(paymentRemoved), 0.0001)
    }

    @Test
    fun aSkippedAndARainedOffVisitAddNothingToTheBalance() {
        val state = RoundState(
            customers = listOf(ada),
            visits = listOf(
                Visit(
                    Round.visitId(ada.id, "2026-10-01"),
                    ada.id,
                    "2026-10-01",
                    VisitStatus.SKIPPED,
                    40.0,
                ),
                Visit(
                    Round.visitId(ada.id, "2026-10-08"),
                    ada.id,
                    "2026-10-08",
                    VisitStatus.RAINED_OFF,
                    40.0,
                ),
            ),
        )

        assertEquals(0.0, Balance.balanceFor(state, ada.id), 0.0001)
        assertEquals(0.0, Balance.totalOwed(state), 0.0001)
    }

    @Test
    fun theBalanceIsDerivedSoCorrectingTheVisitCorrectsTheMoneyWithoutATouchOnAnyBalanceField() {
        val customers = listOf(ada)
        val skipped = RoundState(
            customers = customers,
            visits = listOf(
                Visit(
                    Round.visitId(ada.id, "2026-10-01"),
                    ada.id,
                    "2026-10-01",
                    VisitStatus.SKIPPED,
                    0.0,
                ),
            ),
        )

        val corrected = Round.mark(skipped, ada.id, "2026-10-01", VisitStatus.MOWED, 40.0)

        assertEquals(0.0, Balance.balanceFor(skipped, ada.id), 0.0001)
        assertEquals(40.0, Balance.balanceFor(corrected, ada.id), 0.0001)
        assertEquals(customers, corrected.customers)
    }

    @Test
    fun theTotalOwedIsTheSumOfEveryCustomersRunningBalance() {
        val bob = Customer(id = "bob", name = "Bob", position = 1)
        val state = RoundState(
            customers = listOf(ada, bob),
            visits = listOf(
                mowed(ada.id, "2026-10-01", 40.0),
                mowed(bob.id, "2026-10-01", 55.0),
            ),
            payments = listOf(payment(bob.id, "2026-10-02", 15.0)),
        )

        assertEquals(40.0, Balance.balanceFor(state, ada.id), 0.0001)
        assertEquals(40.0, Balance.balanceFor(state, bob.id), 0.0001)
        assertEquals(80.0, Balance.totalOwed(state), 0.0001)
    }

    @Test
    fun aCustomerWithNoVisitsAndNoPaymentsOwesNothing() {
        val state = RoundState(customers = listOf(ada))

        assertEquals(0.0, Balance.balanceFor(state, ada.id), 0.0001)
        assertEquals(0.0, Balance.totalOwed(state), 0.0001)
    }

    private val ada = Customer(id = "ada", name = "Ada Reyes", position = 0)

    private fun mowed(customerId: String, dateIso: String, amountUsd: Double): Visit =
        Visit(
            id = Round.visitId(customerId, dateIso),
            customerId = customerId,
            dateIso = dateIso,
            status = VisitStatus.MOWED,
            amountUsd = amountUsd,
        )

    private fun payment(customerId: String, dateIso: String, amountUsd: Double): Payment =
        Payment(
            id = Round.paymentId(customerId, dateIso),
            customerId = customerId,
            dateIso = dateIso,
            amountUsd = amountUsd,
        )
}

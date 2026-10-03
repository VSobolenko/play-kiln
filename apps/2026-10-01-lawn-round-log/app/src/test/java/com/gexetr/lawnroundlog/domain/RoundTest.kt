package com.gexetr.lawnroundlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The promises about the round itself: it is an ordered worklist, a visit is recorded in one tap,
 * and a push moves everyone due exactly once.
 */
class RoundTest {

    @Test
    fun aCustomerIsAddedToTheRoundWithoutAnyAppointment() {
        val state = RoundState(customers = listOf(customer("ada")))

        assertEquals(listOf("ada"), Round.ordered(state).map { it.id })
        assertEquals(listOf("ada"), Round.dueOn(state, "2026-10-05").map { it.id })
    }

    @Test
    fun theRoundKeepsTheOrderTheOperatorDrivesIt() {
        val state = RoundState(
            customers = listOf(customer("b", position = 1), customer("a", position = 0), customer("c", position = 2)),
        )

        assertEquals(listOf("a", "b", "c"), Round.ordered(state).map { it.id })
    }

    @Test
    fun movingTheBottomCustomerToSecondPlaceReordersTheRoundAndReflowsPositions() {
        val state = RoundState(
            customers = listOf(customer("a", position = 0), customer("b", position = 1), customer("c", position = 2)),
        )

        val moved = Round.move(state, "c", 1)

        assertEquals(listOf("a", "c", "b"), Round.ordered(moved).map { it.id })
        assertEquals(listOf(0, 1, 2), Round.ordered(moved).map { it.position })
    }

    @Test
    fun movingPastTheEndOfTheRoundKeepsEveryCustomerExactlyOnce() {
        val state = RoundState(
            customers = listOf(customer("a", position = 0), customer("b", position = 1)),
        )

        val moved = Round.move(state, "a", 99)

        assertEquals(listOf("b", "a"), Round.ordered(moved).map { it.id })
        assertEquals(2, Round.ordered(moved).size)
    }

    @Test
    fun movingAnUnknownCustomerLeavesTheRoundUntouched() {
        val state = RoundState(customers = listOf(customer("a", position = 0)))

        assertEquals(state, Round.move(state, "nobody", 0))
    }

    @Test
    fun theDayListIsEveryoneNotYetRecordedAndNeedsNoAppointment() {
        val state = RoundState(
            customers = listOf(customer("a", position = 0), customer("b", position = 1)),
        )

        val afterA = Round.mark(state, "a", "2026-10-05", VisitStatus.MOWED, 40.0)

        assertEquals(listOf("b"), Round.dueOn(afterA, "2026-10-05").map { it.id })
        assertEquals(listOf("a", "b"), Round.dueOn(afterA, "2026-10-06").map { it.id })
    }

    @Test
    fun anArchivedCustomerLeavesTheDayListAndKeepsHisOwnRecord() {
        val state = RoundState(
            customers = listOf(customer("a", position = 0), customer("b", position = 1, active = false)),
        )

        assertEquals(listOf("a"), Round.dueOn(state, "2026-10-05").map { it.id })
        assertEquals(listOf("a", "b"), Round.ordered(state).map { it.id })
    }

    @Test
    fun correctingAMisTappedVisitReplacesItInsteadOfRecordingATypo() {
        val state = RoundState(customers = listOf(customer("ada")))

        val skipped = Round.mark(state, "ada", "2026-10-05", VisitStatus.SKIPPED, 40.0)
        val corrected = Round.mark(skipped, "ada", "2026-10-05", VisitStatus.MOWED, 40.0)

        assertEquals(1, corrected.visits.size)
        assertEquals(VisitStatus.MOWED, corrected.visits.single().status)
        assertEquals(40.0, corrected.visits.single().amountUsd, 0.0001)
        assertEquals(0.0, skipped.visits.single().amountUsd, 0.0001)
    }

    @Test
    fun aPushDefersEveryoneStillDueOnThatDayExactlyOnce() {
        val state = RoundState(
            customers = listOf(customer("a", position = 0), customer("b", position = 1), customer("c", position = 2)),
        )

        val pushed = Round.pushByOneDay(state, "2026-10-05")

        assertEquals(3, pushed.visits.size)
        assertTrue(pushed.visits.all { it.dateIso == "2026-10-05" })
        assertTrue(pushed.visits.all { it.status == VisitStatus.RAINED_OFF })
        assertEquals(emptyList<String>(), Round.dueOn(pushed, "2026-10-05").map { it.id })
    }

    @Test
    fun aSecondPushFromTheSameDayMovesNobodyTwice() {
        val state = RoundState(
            customers = listOf(customer("a", position = 0), customer("b", position = 1)),
        )

        val pushed = Round.pushByOneDay(state, "2026-10-05")

        assertEquals(pushed, Round.pushByOneDay(pushed, "2026-10-05"))
    }

    @Test
    fun pushingTwiceInARowMovesEachCustomerExactlyTwoDaysWithoutDuplicatingAnyOfThem() {
        val state = RoundState(
            customers = listOf(customer("a", position = 0), customer("b", position = 1), customer("c", position = 2)),
        )

        val firstDay = Round.pushByOneDay(state, "2026-10-05")
        val secondDay = Round.pushByOneDay(firstDay, "2026-10-06")

        assertEquals(Round.ordered(state).map { it.id }, Round.ordered(secondDay).map { it.id })
        assertEquals(3, Round.ordered(secondDay).size)
        assertEquals(6, secondDay.visits.size)
        Round.ordered(secondDay).forEach { customer ->
            val days = secondDay.visits
                .filter { visit -> visit.customerId == customer.id }
                .map { visit -> visit.dateIso }
                .sorted()
            assertEquals(listOf("2026-10-05", "2026-10-06"), days)
        }
        assertEquals(0.0, Balance.totalOwed(secondDay), 0.0001)
    }

    @Test
    fun pushingNeverLeavesACustomerBehindAndNeverRewritesAFinishedVisit() {
        val customers = (1..5).map { index -> customer("c$index", position = index - 1) }
        val state = RoundState(
            customers = customers,
            visits = listOf(
                Visit(Round.visitId("c1", "2026-10-05"), "c1", "2026-10-05", VisitStatus.MOWED, 40.0),
            ),
        )

        val pushed = Round.pushByOneDay(state, "2026-10-05")

        assertEquals(5, Round.ordered(pushed).size)
        assertEquals(5, pushed.visits.size)
        assertEquals(VisitStatus.MOWED, pushed.visits.single { it.customerId == "c1" }.status)
        assertEquals(40.0, pushed.visits.single { it.customerId == "c1" }.amountUsd, 0.0001)
        assertEquals(
            listOf("c2", "c3", "c4", "c5"),
            pushed.visits.filter { it.status == VisitStatus.RAINED_OFF }.map { it.customerId }.sorted(),
        )
    }

    @Test
    fun aPaymentIsRecordedOncePerDayAndANonPositiveAmountRemovesIt() {
        val state = RoundState(customers = listOf(customer("ada")))

        val paid = Round.setPayment(state, "ada", "2026-10-05", 40.0)
        val corrected = Round.setPayment(paid, "ada", "2026-10-05", 25.0)

        assertEquals(1, corrected.payments.size)
        assertEquals(25.0, corrected.payments.single().amountUsd, 0.0001)

        val removed = Round.setPayment(corrected, "ada", "2026-10-05", 0.0)

        assertTrue(removed.payments.isEmpty())
    }

    @Test
    fun correctingAPastPaymentKeepsItsDateAndChangesTheBalanceByTheDifference() {
        val state = RoundState(
            customers = listOf(customer("ada")),
            visits = listOf(Visit(Round.visitId("ada", "2026-10-05"), "ada", "2026-10-05", VisitStatus.MOWED, 100.0)),
        )
        val paid = Round.setPayment(state, "ada", "2026-10-06", 40.0)

        val corrected = Round.setPayment(paid, "ada", "2026-10-06", 25.0)

        assertEquals(1, corrected.payments.size)
        assertEquals("2026-10-06", corrected.payments.single().dateIso)
        assertEquals(25.0, corrected.payments.single().amountUsd, 0.0001)
        assertEquals(75.0, Balance.balanceFor(corrected, "ada"), 0.0001)
    }

    @Test
    fun owedByCustomerListsOnlyUnsettledCustomersInRoundOrder() {
        val state = RoundState(
            customers = listOf(customer("b", position = 1), customer("a", position = 0)),
            visits = listOf(
                Visit(Round.visitId("a", "2026-10-05"), "a", "2026-10-05", VisitStatus.MOWED, 40.0),
                Visit(Round.visitId("b", "2026-10-05"), "b", "2026-10-05", VisitStatus.MOWED, 55.0),
                Visit(Round.visitId("b", "2026-10-06"), "b", "2026-10-06", VisitStatus.SKIPPED, 0.0),
            ),
            payments = listOf(Payment(Round.paymentId("b", "2026-10-06"), "b", "2026-10-06", 55.0)),
        )

        assertEquals(listOf("a"), Round.owedByCustomer(state).map { (customer, _) -> customer.id })
        assertEquals(40.0, Round.owedByCustomer(state).single().second, 0.0001)
    }

    private fun customer(id: String, position: Int = 0, active: Boolean = true): Customer =
        Customer(id = id, name = "Customer $id", position = position, active = active)
}

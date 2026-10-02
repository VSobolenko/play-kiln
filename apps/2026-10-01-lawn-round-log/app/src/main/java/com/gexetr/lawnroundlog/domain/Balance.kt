package com.gexetr.lawnroundlog.domain

/**
 * What a customer owes, derived from the record every time it is asked for.
 *
 * A balance is `Σ mowed visits − Σ payments` and nothing else. It is never stored, on the customer
 * or anywhere else, so it cannot drift from the visits and payments it summarises: the same two
 * functions answer before a restart and after one, because both read the same two lists.
 *
 * Only [VisitStatus.MOWED] is billable. A skipped visit and a rained-off visit are recorded — they
 * are facts about the round — but they add nothing, which is what makes "a rained-off morning"
 * cost the operator a day rather than a line on the customer's bill.
 */
object Balance {

    /**
     * The running balance for one customer.
     *
     * The result is a net position, not a debt: a customer who has overpaid reads as a negative
     * number, which is information the operator needs rather than an error to clamp.
     */
    fun balanceFor(state: RoundState, customerId: String): Double {
        val billed = state.visits
            .filter { visit -> visit.customerId == customerId }
            .filter { visit -> visit.status == VisitStatus.MOWED }
            .sumOf { visit -> visit.amountUsd }
        val paid = state.payments
            .filter { payment -> payment.customerId == customerId }
            .sumOf { payment -> payment.amountUsd }
        return billed - paid
    }

    /**
     * The total the operator is owed across the whole record.
     *
     * It sums every customer in the state, archived ones included: archiving removes a customer
     * from the day list, it does not forgive what he owes. A customer with no visits and no
     * payments contributes zero, so the total is also the sum of [Round.owedByCustomer]'s amounts.
     */
    fun totalOwed(state: RoundState): Double =
        state.customers.sumOf { customer -> balanceFor(state, customer.id) }
}

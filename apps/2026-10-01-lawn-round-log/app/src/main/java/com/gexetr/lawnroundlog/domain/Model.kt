package com.gexetr.lawnroundlog.domain

import kotlinx.serialization.Serializable

/**
 * What happened when the operator reached a customer on a day of the round.
 *
 * These are the three answers a round actually produces, and there is no fourth: the job was done
 * and is billable ([MOWED]), the operator skipped it, or the weather cancelled it. Both of the
 * latter are recorded rather than omitted, because "nothing was written down" and "the morning was
 * rained off" are different facts about the round, and only the second explains a quiet day.
 *
 * Only [MOWED] contributes to a balance; see [Balance].
 */
enum class VisitStatus {
    MOWED,
    SKIPPED,
    RAINED_OFF,
}

/**
 * One stop on the operator's round.
 *
 * [position] is the order the operator drives, not an appointment: the round is a recurring
 * worklist he controls, so a customer is on it without any date being attached to him
 * (`openspec/changes/app-lawn-round-log/specs/round-log/spec.md`).
 *
 * [rateUsd] is the customer's usual price, used to pre-fill a visit's amount. It is a *suggestion*
 * only: the amount that counts is the one on the [Visit], because the price of a job can change
 * without the customer's standing rate changing.
 *
 * [active] is false for a customer the operator has archived. An archived customer keeps his
 * history and his unpaid balance, and leaves the day list.
 */
@Serializable
data class Customer(
    val id: String,
    val name: String,
    val address: String = "",
    val phone: String = "",
    val notes: String = "",
    val rateUsd: Double = 0.0,
    val active: Boolean = true,
    val position: Int = 0,
    val createdAt: Long = 0L,
)

/**
 * One record of reaching one customer on one day.
 *
 * A customer has at most one visit per date, and [id] is derived from the pair
 * (`visit-<customerId>-<dateIso>`), which is what makes correcting a mis-tap a replacement rather
 * than a duplicate and what makes pushing the round by a day idempotent by construction. See
 * [Round.mark] and [Round.pushByOneDay].
 */
@Serializable
data class Visit(
    val id: String,
    val customerId: String,
    val dateIso: String,
    val status: VisitStatus,
    val amountUsd: Double = 0.0,
)

/**
 * Money the operator has received from one customer on one day.
 *
 * Like a visit, a payment is keyed by customer and date, so recording a payment twice on a day
 * records the day's amount once. Only [Round.setPayment] writes these, and it removes the day's
 * payment when it is given a non-positive amount — the removal path is the same path.
 */
@Serializable
data class Payment(
    val id: String,
    val customerId: String,
    val dateIso: String,
    val amountUsd: Double,
)

/**
 * The whole record: who is on the round, what happened, and what has been paid.
 *
 * There is deliberately no balance field on it, or on [Customer]. A balance is *derived* from
 * [visits] and [payments] every time it is read ([Balance]), because a stored balance is a second
 * source of truth that can disagree with the records it claims to summarise — the exact defect the
 * "the balance survives a restart" promise exists to rule out.
 *
 * This is also the unit of persistence: one value, one JSON document, one atomic write.
 */
@Serializable
data class RoundState(
    val customers: List<Customer> = emptyList(),
    val visits: List<Visit> = emptyList(),
    val payments: List<Payment> = emptyList(),
)

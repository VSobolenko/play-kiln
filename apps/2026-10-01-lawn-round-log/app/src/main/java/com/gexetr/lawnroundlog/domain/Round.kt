package com.gexetr.lawnroundlog.domain

/**
 * Every operation on the round, as a pure function from one state to the next.
 *
 * There is no clock here and no storage: a date arrives as a parameter and the result is a new
 * [RoundState]. That is what makes the round testable on the JVM, and it is also what makes the
 * day list honest — the app has to say which day it is asking about rather than reading one out of
 * a device whose clock the operator may have just set.
 *
 * Two invariants hold for everything below, and each is enforced by construction rather than by a
 * check:
 *
 * - **A customer has at most one visit per date.** The visit id is derived from the customer and
 *   the date, so recording a visit is an *upsert* — [mark] corrects, it never duplicates.
 * - **[pushByOneDay] moves each due customer exactly once.** It is expressed as "mark the due
 *   customers rained off for that day", and a customer who already has a visit on that day is no
 *   longer due. Pushing the same day twice therefore changes nothing, and pushing two consecutive
 *   days moves each customer exactly two days.
 */
object Round {

    /** The visit id for one customer on one day, which is what makes recording an upsert. */
    fun visitId(customerId: String, dateIso: String): String = "visit-$customerId-$dateIso"

    /** The payment id for one customer on one day, the same idea as [visitId]. */
    fun paymentId(customerId: String, dateIso: String): String = "payment-$customerId-$dateIso"

    /**
     * The round in the order the operator drives it.
     *
     * The sort is total, not merely by [Customer.position]: two customers left on one position by a
     * bug or an import still come out in a stable order, because the tie is broken by the creation
     * time and then by the id. A deterministic order is what lets the reorder promise survive a
     * restart rather than merely look sorted on screen.
     */
    fun ordered(state: RoundState): List<Customer> =
        state.customers.sortedWith(
            compareBy<Customer>({ customer -> customer.position }, { customer -> customer.createdAt }, { customer -> customer.id }),
        )

    /**
     * Who is still due on [dateIso]: every active customer with no visit recorded on that day.
     *
     * This is deliberately *not* an appointment list. A customer is due because he is on the round
     * and the day has not been recorded for him yet — not because anybody scheduled him — so a
     * brand-new customer is due immediately and a day with no appointments is still a full day.
     */
    fun dueOn(state: RoundState, dateIso: String): List<Customer> =
        ordered(state).filter { customer ->
            customer.active && state.visits.none { visit ->
                visit.customerId == customer.id && visit.dateIso == dateIso
            }
        }

    /**
     * Move one customer to [toIndex] in the round, reflowing every position.
     *
     * The index is clamped rather than rejected, because dragging a row past the end of the list is
     * how a person actually reorders it and an exception there would be a crash caused by a thumb.
     * Positions are rewritten as `0..n-1`, so the new order is the stored order and a restart
     * reproduces it exactly.
     *
     * A customer id that is not in the round leaves the state untouched.
     */
    fun move(state: RoundState, customerId: String, toIndex: Int): RoundState {
        val current = ordered(state)
        val moving = current.firstOrNull { customer -> customer.id == customerId } ?: return state
        val remaining = current.filterNot { customer -> customer.id == customerId }
        val target = toIndex.coerceIn(0, remaining.size)
        val reordered = remaining.toMutableList().apply { add(target, moving) }
        val positions = reordered.mapIndexed { index, customer -> customer.id to index }.toMap()
        return state.copy(
            customers = state.customers.map { customer ->
                val position = positions[customer.id]
                if (position == null) customer else customer.copy(position = position)
            },
        )
    }

    /**
     * Record what happened to one customer on one day, as an upsert.
     *
     * The visit is keyed by customer and date, so tapping *skipped* and then correcting it to
     * *mowed* leaves one visit reading *mowed* rather than two visits — which is the difference
     * between an editable record and a log of typos. An amount is kept only for a mowed visit;
     * a skipped or rained-off visit carries nothing, so no later reader has to guess whether a
     * stored amount was billed.
     */
    fun mark(
        state: RoundState,
        customerId: String,
        dateIso: String,
        status: VisitStatus,
        amountUsd: Double,
    ): RoundState {
        val existing = state.visits.firstOrNull { visit ->
            visit.customerId == customerId && visit.dateIso == dateIso
        }
        val visit = Visit(
            id = existing?.id ?: visitId(customerId, dateIso),
            customerId = customerId,
            dateIso = dateIso,
            status = status,
            amountUsd = if (status == VisitStatus.MOWED) amountUsd else 0.0,
        )
        val visits = if (existing == null) {
            state.visits + visit
        } else {
            state.visits.map { current -> if (current.id == existing.id) visit else current }
        }
        return state.copy(visits = visits)
    }

    /**
     * Push the round by one day: record the day as rained off for everyone still due on it.
     *
     * This is the whole mechanism, and it is the reason a double push cannot double-move anybody.
     * The morning is recorded against [fromDateIso] — the same upsert [mark] performs — so a second
     * push from the same day finds nobody due and changes nothing, and a push from the next day
     * finds them all again and defers them one further day. The customers list is never touched,
     * so nobody can be duplicated or left behind by a push.
     *
     * The ids are derived (`visit-<customerId>-<dateIso>`), so the operation is idempotent rather
     * than merely *usually* idempotent: there is no way for it to write two records for one
     * customer on one day.
     */
    fun pushByOneDay(state: RoundState, fromDateIso: String): RoundState {
        val due = dueOn(state, fromDateIso)
        return due.fold(state) { current, customer ->
            mark(current, customer.id, fromDateIso, VisitStatus.RAINED_OFF, 0.0)
        }
    }

    /**
     * Record a payment for one customer on one day, or remove the day's payment.
     *
     * One function does both because they are one decision from the operator's side: the amount he
     * has received, where zero means "nothing, and the earlier record was wrong". Removing by
     * writing zero keeps the removal path inside the same keyed upsert, so a payment cannot be
     * half-removed and cannot be recorded twice on a day.
     */
    fun setPayment(
        state: RoundState,
        customerId: String,
        dateIso: String,
        amountUsd: Double,
    ): RoundState {
        val existing = state.payments.firstOrNull { payment ->
            payment.customerId == customerId && payment.dateIso == dateIso
        }
        if (amountUsd <= 0.0) {
            return if (existing == null) {
                state
            } else {
                state.copy(payments = state.payments.filterNot { payment -> payment.id == existing.id })
            }
        }
        val payment = Payment(
            id = existing?.id ?: paymentId(customerId, dateIso),
            customerId = customerId,
            dateIso = dateIso,
            amountUsd = amountUsd,
        )
        val payments = if (existing == null) {
            state.payments + payment
        } else {
            state.payments.map { current -> if (current.id == existing.id) payment else current }
        }
        return state.copy(payments = payments)
    }

    /**
     * Who owes what, in the order the operator drives them.
     *
     * Customers whose balance is exactly zero are omitted — they are settled and there is nothing
     * to chase — while a negative balance is kept, because an overpayment is a fact about the
     * record rather than a reason to hide the customer.
     */
    fun owedByCustomer(state: RoundState): List<Pair<Customer, Double>> =
        ordered(state)
            .map { customer -> customer to Balance.balanceFor(state, customer.id) }
            .filter { (_, balance) -> balance != 0.0 }
}

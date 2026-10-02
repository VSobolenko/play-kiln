package com.gexetr.lawnroundlog.ui.round

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.lawnroundlog.data.RoundStore
import com.gexetr.lawnroundlog.domain.Balance
import com.gexetr.lawnroundlog.domain.Customer
import com.gexetr.lawnroundlog.domain.Payment
import com.gexetr.lawnroundlog.domain.Round
import com.gexetr.lawnroundlog.domain.RoundState
import com.gexetr.lawnroundlog.domain.Visit
import com.gexetr.lawnroundlog.domain.VisitStatus
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The free tier is a limit on *customers*, never on time: every one of the eight is fully usable for
 * as long as the operator keeps the app, which is what makes it a free version rather than a trial.
 */
const val FREE_TIER_CUSTOMER_LIMIT = 8

/**
 * Everything the round screens draw, derived once per change.
 *
 * The balances, the day's statuses and the ordering are computed from the stored record rather than
 * kept beside it, so a restart cannot show a total that disagrees with the visits behind it. That is
 * the property the spec's "the balance survives a restart" scenario is really measuring.
 */
data class RoundUiState(
    val record: RoundState = RoundState(),
    val todayIso: String = "",
    val customers: List<Customer> = emptyList(),
    val ordered: List<Customer> = emptyList(),
    val due: List<Customer> = emptyList(),
    val balances: Map<String, Double> = emptyMap(),
    val todayStatus: Map<String, VisitStatus> = emptyMap(),
    val totalOwed: Double = 0.0,
)

/**
 * The round's state holder: the only writer of the stored record.
 *
 * `RoundStore` is a file, not a flow, so this class holds the record in memory, renders a
 * [RoundUiState] from it, and writes the whole record on every change. Writes go to `Dispatchers.IO`
 * because they are disk I/O, and the rendered state is published before the write completes so the
 * interface never waits on the filesystem.
 *
 * Every mutation is expressed through the domain's own functions where one exists (`Round.move`,
 * `Round.mark`, `Round.pushByOneDay`, `Round.setPayment`). Two operations have no domain function —
 * deleting a customer and deleting a single visit — and those are done by mapping the immutable
 * record directly, which is the one honest option rather than inventing a second rule for what
 * deleting means.
 */
class RoundViewModel(private val store: RoundStore) : ViewModel() {

    private val todayIso: String = LocalDate.now().toString()

    private var record: RoundState = RoundState()

    private val _state = MutableStateFlow(RoundUiState(todayIso = todayIso))
    val state: StateFlow<RoundUiState> = _state.asStateFlow()

    /** The ninth customer, held while the unlock sheet is open, so he is never re-typed. */
    private val _pendingCustomer = MutableStateFlow<Customer?>(null)
    val pendingCustomer: StateFlow<Customer?> = _pendingCustomer.asStateFlow()

    init {
        viewModelScope.launch {
            record = withContext(Dispatchers.IO) { store.load() }
            render()
        }
    }

    // ---- the round itself ----------------------------------------------------------------

    fun move(customerId: String, toIndex: Int) {
        apply(Round.move(record, customerId, toIndex))
    }

    fun mark(customerId: String, status: VisitStatus) {
        apply(Round.mark(record, customerId, todayIso, status, rateFor(customerId)))
    }

    /** Restating an already-recorded visit to a different outcome — the mis-tap correction. */
    fun restateVisit(visit: Visit, status: VisitStatus) {
        apply(Round.mark(record, visit.customerId, visit.dateIso, status, rateFor(visit.customerId)))
    }

    fun deleteVisit(visit: Visit) {
        apply(record.copy(visits = record.visits.filterNot { it.id == visit.id }))
    }

    fun pushByOneDay() {
        apply(Round.pushByOneDay(record, todayIso))
    }

    // ---- customers -----------------------------------------------------------------------

    fun upsertCustomer(customer: Customer) {
        val exists = record.customers.any { it.id == customer.id }
        val next = if (exists) {
            record.copy(customers = record.customers.map { if (it.id == customer.id) customer else it })
        } else {
            val position = (record.customers.maxOfOrNull { it.position } ?: -1) + 1
            val now = System.currentTimeMillis()
            val appended = customer.copy(
                position = position,
                createdAt = if (customer.createdAt == 0L) now else customer.createdAt,
                active = true,
            )
            record.copy(customers = record.customers + appended)
        }
        apply(next)
    }

    fun deleteCustomer(customerId: String) {
        apply(
            record.copy(
                customers = record.customers.filterNot { it.id == customerId },
                visits = record.visits.filterNot { it.customerId == customerId },
                payments = record.payments.filterNot { it.customerId == customerId },
            ),
        )
    }

    /** True when this app has not been unlocked and the free tier is already full. */
    fun requiresUnlock(unlocked: Boolean): Boolean =
        !unlocked && record.customers.count { it.active } >= FREE_TIER_CUSTOMER_LIMIT

    fun stagePendingCustomer(customer: Customer) {
        _pendingCustomer.value = customer
    }

    fun commitPendingCustomer() {
        val pending = _pendingCustomer.value ?: return
        _pendingCustomer.value = null
        upsertCustomer(pending)
    }

    // ---- payments ------------------------------------------------------------------------

    fun recordPayment(customerId: String, amountUsd: Double) {
        apply(Round.setPayment(record, customerId, todayIso, amountUsd))
    }

    /** Removing a payment is setting it back to zero; the domain has one operation for both. */
    fun removePayment(payment: Payment) {
        apply(Round.setPayment(record, payment.customerId, payment.dateIso, 0.0))
    }

    // ---- plumbing ------------------------------------------------------------------------

    private fun rateFor(customerId: String): Double =
        record.customers.firstOrNull { it.id == customerId }?.rateUsd ?: 0.0

    private fun apply(next: RoundState) {
        record = next
        render()
        viewModelScope.launch(Dispatchers.IO) { store.save(next) }
    }

    private fun render() {
        val ordered = Round.ordered(record)
        // `dueOn` answers "who is on today's round"; when the record carries no schedule at all the
        // fallback is the whole ordered round, because a day list that empties itself would ask the
        // operator to schedule an appointment — which this app is deliberately not.
        val due = Round.dueOn(record, todayIso).ifEmpty { ordered }
        val balances = record.customers.associate { customer -> customer.id to Balance.balanceFor(record, customer.id) }
        val statuses = record.visits
            .filter { visit -> visit.dateIso == todayIso }
            .associate { visit -> visit.customerId to visit.status }
        _state.value = RoundUiState(
            record = record,
            todayIso = todayIso,
            customers = record.customers,
            ordered = ordered,
            due = due,
            balances = balances,
            todayStatus = statuses,
            totalOwed = Balance.totalOwed(record),
        )
    }

    companion object {
        fun factory(store: RoundStore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RoundViewModel(store) as T
        }
    }
}

/**
 * One money format for the whole app.
 *
 * The symbol is the operator's own choice from Settings and the number is plain decimal: this is a
 * notebook, not an accounting system, and a locale-dependent format would make the same record read
 * differently on two phones. `Locale.US` is used for the *number* only, so the decimal separator
 * never moves with the app's language.
 */
internal fun formatAmount(currencySymbol: String, amount: Double): String =
    currencySymbol + String.format(Locale.US, "%.2f", amount)

/** The plain number a text field is seeded with, without the currency symbol. */
internal fun formatAmountInput(amount: Double): String =
    if (amount == 0.0) "" else String.format(Locale.US, "%.2f", amount)

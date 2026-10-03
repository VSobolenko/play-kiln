package com.gexetr.lawnroundlog.ui.customer

import androidx.lifecycle.ViewModel
import com.gexetr.lawnroundlog.domain.Customer
import com.gexetr.lawnroundlog.ui.round.formatAmountInput
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the customer form is currently holding.
 *
 * Money is a `String` here and only becomes a `Double` at save, because a text field that rewrites
 * what the operator is typing — `5` becoming `5.00` mid-keystroke — is a field he cannot finish. The
 * parse failure is carried out as `rateInvalid` rather than swallowed, so a typo is a visible error
 * instead of a silently stored zero.
 */
data class CustomerEditorState(
    val customerId: String? = null,
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val notes: String = "",
    val rateText: String = "",
    val nameInvalid: Boolean = false,
    val rateInvalid: Boolean = false,
    // The three fields the form never shows but must not lose. They are carried here because the
    // save path hands a whole `Customer` back to the round's writer, which replaces the stored row
    // with it verbatim: a draft built from the five editable fields alone would arrive with
    // `position = 0` and `createdAt = 0L`, and since `Round.ordered` sorts by exactly those two, an
    // operator who corrected a phone number would silently move that customer to the head of the
    // round. The order is the product's central promise, so editing a field must not be able to
    // rewrite it.
    val position: Int = 0,
    val createdAt: Long = 0L,
    val active: Boolean = true,
) {
    val isNew: Boolean get() = customerId == null
}

/**
 * The form's state holder. It owns the draft and the validation, and it owns nothing else.
 *
 * It deliberately does **not** write the record: the record has exactly one writer
 * ([com.gexetr.lawnroundlog.ui.round.RoundViewModel]), and a second one would be two sources of truth
 * for the same round — the failure mode that makes a totals screen disagree with its own list. The
 * screen reads the customer it is editing from the round, keeps the draft here, and hands the finished
 * `Customer` back to the round's writer on save.
 */
class CustomerViewModel : ViewModel() {

    private val _editor = MutableStateFlow(CustomerEditorState())
    val editor: StateFlow<CustomerEditorState> = _editor.asStateFlow()

    /** A new customer, seeded with the operator's own default rate from Settings. */
    fun startNew(defaultRateUsd: Double) {
        _editor.value = CustomerEditorState(rateText = formatAmountInput(defaultRateUsd))
    }

    /** An existing customer, with every field he already has, ready to correct. */
    fun startEdit(customer: Customer) {
        _editor.value = CustomerEditorState(
            customerId = customer.id,
            name = customer.name,
            address = customer.address,
            phone = customer.phone,
            notes = customer.notes,
            rateText = formatAmountInput(customer.rateUsd),
            position = customer.position,
            createdAt = customer.createdAt,
            active = customer.active,
        )
    }

    fun setName(value: String) {
        _editor.value = _editor.value.copy(name = value, nameInvalid = false)
    }

    fun setAddress(value: String) {
        _editor.value = _editor.value.copy(address = value)
    }

    fun setPhone(value: String) {
        _editor.value = _editor.value.copy(phone = value)
    }

    fun setNotes(value: String) {
        _editor.value = _editor.value.copy(notes = value)
    }

    fun setRate(value: String) {
        // The comma is accepted because a Russian keyboard's decimal key produces one, and rejecting
        // it would make the field unusable in the locale the app is translated into.
        val normalized = value.replace(',', '.')
        if (normalized.isEmpty() || normalized.all { it.isDigit() || it == '.' }) {
            _editor.value = _editor.value.copy(rateText = normalized, rateInvalid = false)
        } else {
            _editor.value = _editor.value.copy(rateInvalid = true)
        }
    }

    /** The draft as a record, or null when it is not yet a valid one. */
    fun validatedCustomer(): Customer? {
        val draft = _editor.value
        val name = draft.name.trim()
        val rate = draft.rateText.toDoubleOrNull() ?: if (draft.rateText.isBlank()) 0.0 else null

        val nameInvalid = name.isEmpty()
        val rateInvalid = rate == null
        if (nameInvalid || rateInvalid) {
            _editor.value = draft.copy(nameInvalid = nameInvalid, rateInvalid = rateInvalid)
            return null
        }

        val existing = draft.customerId
        return Customer(
            id = existing ?: UUID.randomUUID().toString(),
            name = name,
            address = draft.address.trim(),
            phone = draft.phone.trim(),
            notes = draft.notes.trim(),
            rateUsd = rate ?: 0.0,
            active = draft.active,
            position = draft.position,
            createdAt = draft.createdAt,
        )
    }
}

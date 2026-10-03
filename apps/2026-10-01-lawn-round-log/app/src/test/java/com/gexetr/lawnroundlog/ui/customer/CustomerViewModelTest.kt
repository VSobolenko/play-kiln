package com.gexetr.lawnroundlog.ui.customer

import com.gexetr.lawnroundlog.domain.Customer
import com.gexetr.lawnroundlog.domain.Round
import com.gexetr.lawnroundlog.domain.RoundState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The form is the one place where a field it does not show can still be destroyed by editing a field
 * it does: the screen reads the customer from the round, keeps a draft here, and hands a whole
 * `Customer` back to the round's single writer, which replaces the stored row with it verbatim. An
 * editing path that rebuilds the record from the visible fields alone therefore arrives with
 * `position = 0` and `createdAt = 0L`, and `Round.ordered` sorts by exactly those two — so correcting
 * a phone number would move that customer to the head of the round.
 */
class CustomerViewModelTest {

    @Test
    fun correctingACustomerKeepsHisPlaceInTheRound() {
        val first = Customer(id = "a", name = "Ada", position = 0, createdAt = 1_000L)
        val second = Customer(id = "b", name = "Bob", position = 1, createdAt = 2_000L)
        val state = RoundState(customers = listOf(first, second))

        val editor = CustomerViewModel()
        editor.startEdit(second)
        editor.setPhone("555-0100")
        val edited = requireNotNull(editor.validatedCustomer())

        assertEquals(1, edited.position)
        assertEquals(2_000L, edited.createdAt)
        assertEquals("555-0100", edited.phone)

        // The consequence the two carried fields exist for, asserted on the same records the round
        // writer would store: the operator's drive order survives an edit. With the fields reset this
        // reads ["b", "a"], which is the defect rather than a style difference.
        val afterEdit = state.copy(customers = state.customers.map { if (it.id == edited.id) edited else it })
        assertEquals(listOf("a", "b"), Round.ordered(afterEdit).map { it.id })
    }

    @Test
    fun aCustomerMarkedInactiveStaysInactiveAfterAnEdit() {
        val leaving = Customer(id = "b", name = "Bob", position = 1, createdAt = 2_000L, active = false)

        val editor = CustomerViewModel()
        editor.startEdit(leaving)
        editor.setNotes("left the round")
        val edited = requireNotNull(editor.validatedCustomer())

        assertEquals(false, edited.active)
    }

    @Test
    fun aNewCustomerIsStillHandedOverWithoutAPosition() {
        // A new customer must keep the zeroes: the round writer is what assigns a real position, by
        // appending. A draft that invented one would collide with the row already at that position.
        val editor = CustomerViewModel()
        editor.startNew(defaultRateUsd = 40.0)
        editor.setName("Ada")
        val created = requireNotNull(editor.validatedCustomer())

        assertEquals(0, created.position)
        assertEquals(0L, created.createdAt)
    }
}

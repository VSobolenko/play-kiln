package com.gexetr.lawnroundlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The export promise: the file contains what is stored, the app can say how much that was, and a
 * field that contains a comma does not shift every later column on its row.
 */
class CsvTest {

    @Test
    fun exportReportsThreeCustomerRowsFiveVisitRowsAndOnePaymentRow() {
        val exported = Csv.export(threeCustomersFiveVisitsOnePayment())

        assertEquals(3, exported.customerRows)
        assertEquals(5, exported.visitRows)
        assertEquals(1, exported.paymentRows)
        assertEquals(3, dataRowsStartingWith(exported.text, 'c'))
        assertEquals(5, dataRowsStartingWith(exported.text, 'v'))
        assertEquals(1, dataRowsStartingWith(exported.text, 'p'))
    }

    @Test
    fun exportCarriesOneLabelledSectionPerTable() {
        val exported = Csv.export(threeCustomersFiveVisitsOnePayment())

        assertTrue(exported.text.contains("# customers"))
        assertTrue(exported.text.contains("# visits"))
        assertTrue(exported.text.contains("# payments"))
        assertTrue(exported.text.contains("id,name,address,phone,notes,rateUsd,active,position,createdAt"))
        assertTrue(exported.text.contains("id,customerId,dateIso,status,amountUsd"))
        assertTrue(exported.text.contains("id,customerId,dateIso,amountUsd"))
    }

    @Test
    fun exportQuotesAFieldThatContainsACommaSoLaterColumnsDoNotShift() {
        val state = RoundState(
            customers = listOf(
                Customer(
                    id = "c1",
                    name = "Ada \"Sunshine\" Reyes",
                    address = "12 Rose St, Unit B",
                    position = 0,
                ),
            ),
        )

        val text = Csv.export(state).text

        assertTrue(text.contains("\"Ada \"\"Sunshine\"\" Reyes\""))
        assertTrue(text.contains("\"12 Rose St, Unit B\""))
    }

    @Test
    fun exportIsDeterministicSoTwoExportsOfOneRecordAreTheSameFile() {
        val state = threeCustomersFiveVisitsOnePayment()
        val shuffled = state.copy(
            visits = state.visits.reversed(),
            payments = state.payments.reversed(),
        )

        assertEquals(Csv.export(state).text, Csv.export(state).text)
        assertEquals(Csv.export(state).text, Csv.export(shuffled).text)
    }

    @Test
    fun exportWritesMoneyAsPlainNumbersWithoutTheSettingsCurrencySymbol() {
        val exported = Csv.export(threeCustomersFiveVisitsOnePayment())

        assertTrue(exported.text.contains("40.00"))
        assertTrue(exported.text.contains("55.00"))
        assertTrue(!exported.text.contains("$"))
    }

    private fun threeCustomersFiveVisitsOnePayment(): RoundState = RoundState(
        customers = listOf(
            Customer(id = "c1", name = "Ada Reyes", position = 0),
            Customer(id = "c2", name = "Bob Tran", position = 1),
            Customer(id = "c3", name = "Cleo Marsh", position = 2),
        ),
        visits = listOf(
            Visit("v1", "c1", "2026-10-01", VisitStatus.MOWED, 40.0),
            Visit("v2", "c1", "2026-10-08", VisitStatus.MOWED, 40.0),
            Visit("v3", "c2", "2026-10-01", VisitStatus.SKIPPED, 0.0),
            Visit("v4", "c2", "2026-10-08", VisitStatus.RAINED_OFF, 0.0),
            Visit("v5", "c3", "2026-10-02", VisitStatus.MOWED, 55.0),
        ),
        payments = listOf(
            Payment("p1", "c1", "2026-10-09", 40.0),
        ),
    )

    /** Data rows carry the record's own ids in column one; headers and section markers do not. */
    private fun dataRowsStartingWith(text: String, prefix: Char): Int =
        text.lines().count { line -> line.length > 1 && line[0] == prefix && line[1].isDigit() }
}

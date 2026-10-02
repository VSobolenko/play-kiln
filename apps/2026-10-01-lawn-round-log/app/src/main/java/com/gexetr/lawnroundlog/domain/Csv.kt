package com.gexetr.lawnroundlog.domain

import java.util.Locale

/**
 * The stored record as one CSV document the operator can keep, mail or open in a spreadsheet.
 *
 * The whole record is one file because it is one thing to the operator — a backup of the round —
 * and because three files from one export is three chances to keep only two of them. The document
 * carries three sections, each introduced by a `# <name>` line and its own column header, and the
 * `Export` result reports what was written so the app can tell the operator rather than implying
 * success.
 *
 * Amounts are written as plain two-decimal numbers with no currency symbol: a CSV is data, the
 * currency is a display choice the operator makes in Settings, and baking a symbol into an archive
 * makes the archive depend on a preference that can change after it was written.
 */
object Csv {

    /**
     * The exported document and the number of *data* rows in each of its sections.
     *
     * The counts are the rows that describe the record — section markers and column headers are not
     * counted — because the promise the app makes is "here is what was written", not "here is how
     * many lines the file has".
     */
    data class Export(
        val text: String,
        val customerRows: Int,
        val visitRows: Int,
        val paymentRows: Int,
    )

    /** Render [state] as a CSV document, with every table in a deterministic order. */
    fun export(state: RoundState): Export {
        val customers = Round.ordered(state)
        val visits = state.visits.sortedWith(
            compareBy<Visit>({ visit -> visit.dateIso }, { visit -> visit.customerId }, { visit -> visit.id }),
        )
        val payments = state.payments.sortedWith(
            compareBy<Payment>({ payment -> payment.dateIso }, { payment -> payment.customerId }, { payment -> payment.id }),
        )

        val builder = StringBuilder()
        builder.append("# customers\n")
        builder.append("id,name,address,phone,notes,rateUsd,active,position,createdAt\n")
        customers.forEach { customer ->
            builder.append(
                row(
                    listOf(
                        customer.id,
                        customer.name,
                        customer.address,
                        customer.phone,
                        customer.notes,
                        money(customer.rateUsd),
                        customer.active.toString(),
                        customer.position.toString(),
                        customer.createdAt.toString(),
                    ),
                ),
            )
        }

        builder.append("# visits\n")
        builder.append("id,customerId,dateIso,status,amountUsd\n")
        visits.forEach { visit ->
            builder.append(
                row(
                    listOf(
                        visit.id,
                        visit.customerId,
                        visit.dateIso,
                        visit.status.name,
                        money(visit.amountUsd),
                    ),
                ),
            )
        }

        builder.append("# payments\n")
        builder.append("id,customerId,dateIso,amountUsd\n")
        payments.forEach { payment ->
            builder.append(
                row(
                    listOf(
                        payment.id,
                        payment.customerId,
                        payment.dateIso,
                        money(payment.amountUsd),
                    ),
                ),
            )
        }

        return Export(
            text = builder.toString(),
            customerRows = customers.size,
            visitRows = visits.size,
            paymentRows = payments.size,
        )
    }

    private fun row(values: List<String>): String =
        values.joinToString(separator = ",") { value -> field(value) } + "\n"

    /**
     * Quote a field when the document needs it, and double any quote inside it.
     *
     * Without this, a customer whose address is `12 Rose St, Unit B` silently becomes two columns
     * and every later field on the row is off by one — the failure that makes a CSV export
     * untrustworthy exactly when the operator needs it.
     */
    private fun field(value: String): String =
        if (value.any { character -> character == ',' || character == '"' || character == '\n' || character == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
}

package com.gexetr.pipesizing.ui.tables

import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UserCapacityTable
import com.gexetr.pipesizing.ui.storage.KEY_VALUE_SEPARATOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The table store's three properties, the same three `SavedCaseTest` proves for the case store: a
 * hostile name survives, an unreadable record is dropped without losing its neighbours, and blank
 * storage is simply nothing.
 *
 * Every reading below is deliberate synthetic filler ("synthetic-size-a", 1.0, 2.0). None of it is
 * code-table data; the app owns none (`apps/2026-09-25-pipe-sizing/docs/2026-09-25-pipe-sizing-code-source.md`).
 */
class TableCodecTest {

    @Test
    fun aNameAndCitationHoldingEverySeparatorStillRoundTrip() {
        val hostile = table(
            name = "Line\nbreak | pipe ; semi = eq \\ backslash",
            citation = "edition|1;table=2\nend\\",
        )

        val encoded = TableCodec.encode(listOf(hostile))

        // If any structural character reached the text unescaped, the record would split here.
        assertEquals(0, encoded.count { it == '\n' })
        assertEquals(listOf(hostile), TableCodec.decode(encoded))
    }

    @Test
    fun aRowCellHoldingASeparatorStillRoundTrips() {
        val hostile = table(rows = listOf(TableRow("size;=|\nsynthetic", 3.0, 1.0, 2.0)))

        assertEquals(listOf(hostile), TableCodec.decode(TableCodec.encode(listOf(hostile))))
    }

    @Test
    fun anUnreadableRecordIsDroppedWithoutLosingTheReadableOnes() {
        val first = table(name = "first", citation = "citation-a")
        val third = table(name = "third", citation = "citation-c")
        // A record with no header fields at all is what a killed write leaves behind.
        val stored = TableCodec.encode(listOf(first)) + "\n" + "truncated-without-any-field" + "\n" +
            TableCodec.encode(listOf(third))

        assertEquals(listOf(first, third), TableCodec.decode(stored))
    }

    @Test
    fun aRecordWhoseLastCellWasLostIsDroppedRatherThanKeptWithTheReadableRows() {
        val intact = table(rows = listOf(TableRow("synthetic-size-a", null, 1.0, 2.0), TableRow("synthetic-size-b", null, 2.0, 1.0)))
        val encoded = TableCodec.encode(listOf(intact))

        // Cut the record where the final row's capacity value would have been.
        val damaged = encoded.substring(0, encoded.lastIndexOf(KEY_VALUE_SEPARATOR))

        // A table that lost a row is not a smaller table: it must not decode at all.
        assertTrue(TableCodec.decode(damaged).isEmpty())
    }

    @Test
    fun anAbsentInsideDiameterStaysAbsent() {
        val table = table(rows = listOf(TableRow("synthetic-size-a", null, 1.0, 2.0)))

        val restored = TableCodec.decode(TableCodec.encode(listOf(table))).single()

        assertNull(restored.rows.single().insideDiameter)
        assertEquals(1.0, restored.rows.single().lengthFeet, 0.0)
        assertEquals(2.0, restored.rows.single().capacityMbh, 0.0)
    }

    @Test
    fun aRecordedInsideDiameterSurvivesTheRoundTrip() {
        val table = table(rows = listOf(TableRow("synthetic-size-a", 3.0, 1.0, 2.0)))

        val restored = TableCodec.decode(TableCodec.encode(listOf(table))).single()

        assertEquals(3.0, requireNotNull(restored.rows.single().insideDiameter), 0.0)
    }

    @Test
    fun blankStorageDecodesToNothing() {
        assertTrue(TableCodec.decode("").isEmpty())
        assertTrue(TableCodec.decode("\n").isEmpty())
        assertTrue(TableCodec.decode("   \n  ").isEmpty())
    }

    private fun table(
        name: String = "table",
        citation: String = "citation",
        rows: List<TableRow> = listOf(TableRow("synthetic-size-a", null, 1.0, 2.0)),
    ) = UserCapacityTable(
        id = "id-$name",
        name = name,
        applicability = TableApplicability(
            fuel = "synthetic fuel",
            materialOrSystem = "synthetic system",
            inletPressureClass = "synthetic class",
            permittedPressureDrop = "synthetic drop",
            gasSpecificGravity = "synthetic gravity",
        ),
        citation = TableCitation(citation, "synthetic-identifier"),
        marker = USER_RECORDED_MARKER,
        rows = rows,
    )
}

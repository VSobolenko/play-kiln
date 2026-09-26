package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.GradeBand
import com.gexetr.teachergradebook.domain.model.RoundMode
import com.gexetr.teachergradebook.domain.model.RoundingRule
import com.gexetr.teachergradebook.domain.model.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CSV in both directions.
 *
 * The quoting assertions are the point of this file: a teacher's export is full of `Last, First`
 * names and of apostrophes, and a single unquoted comma turns one student into two columns and
 * corrupts every column after it.
 */
class CsvFormatTest {

    private val labels = CsvLabels(
        student = "Student",
        externalId = "External ID",
        overallPercent = "Overall %",
        letter = "Letter",
        missingCountedAsZero = "Missing counted as zero",
        rounding = "Rounding",
        missing = "Missing",
        exempt = "Exempt",
        yes = "yes",
        no = "no",
        roundingRule = "rounded half up to 1 decimal",
    )

    @Test
    fun `a field is quoted only when it would otherwise break the row`() {
        assertEquals("Ada", CsvFormat.escapeField("Ada"))
        assertEquals("\"Doe, Jane\"", CsvFormat.escapeField("Doe, Jane"))
        assertEquals("\"Say \"\"hi\"\"\"", CsvFormat.escapeField("Say \"hi\""))
        assertEquals("\"two\nlines\"", CsvFormat.escapeField("two\nlines"))
        assertEquals("\"two\r\nlines\"", CsvFormat.escapeField("two\r\nlines"))
    }

    @Test
    fun `a name containing a comma is quoted so the row keeps its columns`() {
        val classroom = Fixtures.classroom(
            grades = listOf(Fixtures.score(Fixtures.S1, Fixtures.A1, 10.0)),
        ).let {
            it.copy(students = listOf(Student("x1", "Mary, Jr.", "Poppins", "9009")) + it.students)
        }

        val csv = CsvFormat.encodeClass(classroom, labels).trimEnd()
        val row = csv.split("\r\n")[1]

        // The quoted name is one field, and the row must be exactly as wide as the header: a name
        // that leaks a comma into the stream turns the student into two columns and shifts every
        // mark after them by one. The width is asserted, not just the quoting.
        val expected = buildList {
            add("\"Mary, Jr. Poppins\"")
            add("9009")
            repeat(classroom.assignments.size) { add("") }
            add("") // overall, empty because this student has no marks
            add("") // letter, empty for the same reason
            add(if (classroom.missingCountsAsZero) "yes" else "no")
            add(labels.roundingRule)
        }.joinToString(",")

        assertEquals("a quoted comma-bearing name must keep the row aligned, got: $row", expected, row)
    }

    @Test
    fun `the export carries the averages, the letter and the applied rules`() {
        val classroom = Fixtures.classroom(
            rounding = RoundingRule(RoundMode.HALF_UP, 1),
            gradeScale = listOf(
                GradeBand(90.0, "A"),
                GradeBand(80.0, "B"),
                GradeBand(0.0, "F"),
            ),
            grades = listOf(
                Fixtures.score(Fixtures.S1, Fixtures.A1, 10.0),
                Fixtures.score(Fixtures.S1, Fixtures.A2, 10.0),
                Fixtures.score(Fixtures.S1, Fixtures.A3, 20.0),
                Fixtures.score(Fixtures.S1, Fixtures.E1, 80.0),
                Fixtures.exempt(Fixtures.S2, Fixtures.A1),
                Fixtures.exempt(Fixtures.S2, Fixtures.A2),
                Fixtures.exempt(Fixtures.S2, Fixtures.A3),
                Fixtures.score(Fixtures.S2, Fixtures.E1, 90.0),
            ),
        )

        val lines = CsvFormat.encodeClass(classroom, labels).trimEnd().split("\r\n")
        val header = lines[0].split(",")
        val ada = lines[1].split(",")
        val grace = lines[2].split(",")
        val alan = lines[3].split(",")

        assertEquals("Student", header[0])
        assertEquals("External ID", header[1])
        assertEquals("Overall %", header[6])
        assertEquals("Letter", header[7])
        assertEquals("Missing counted as zero", header[8])
        assertEquals("Rounding", header[9])

        // Ada: Coursework drops the 50 %, so 92.0 overall, which is an A on this scale.
        assertEquals("9001", ada[1])
        assertEquals("10", ada[2])
        assertEquals("92.0", ada[6])
        assertEquals("A", ada[7])

        // Grace: the whole coursework category is exempt, so the exam alone decides: 90.0.
        assertEquals("Exempt", grace[2])
        assertEquals("90", grace[5])
        assertEquals("90.0", grace[6])
        assertEquals("A", grace[7])
    }

    /**
     * An empty cell is exported as empty. Writing `0` would tell the school system that a student
     * scored nothing when in fact nothing has been handed in or recorded.
     */
    @Test
    fun `an empty grade cell exports as an empty field and never as zero`() {
        val classroom = Fixtures.classroom()
        val lines = CsvFormat.encodeClass(classroom, labels).trimEnd().split("\r\n")
        val alan = lines.single { it.startsWith("Alan Turing") }.split(",")

        assertEquals("Alan Turing", alan[0])
        assertEquals("9003", alan[1])
        assertEquals("", alan[2])
        assertEquals("", alan[3])
        assertEquals("", alan[4])
        assertEquals("", alan[5])
        assertEquals("", alan[6])
        assertEquals("", alan[7])
    }

    @Test
    fun `the export ends every row with CRLF`() {
        val csv = CsvFormat.encodeClass(Fixtures.classroom(), labels)

        assertTrue(csv.endsWith("\r\n"))
        assertEquals(4, csv.trimEnd().split("\r\n").size)
    }

    @Test
    fun `a roster header is recognised and skipped`() {
        val parse = CsvFormat.parseRoster("Student,External ID\nAda Lovelace,9001\nGrace Hopper,9002\n")

        assertEquals(0, parse.skipped.size)
        assertEquals(2, parse.entries.size)
        assertEquals("Ada", parse.entries[0].firstName)
        assertEquals("Lovelace", parse.entries[0].lastName)
        assertEquals("9001", parse.entries[0].externalId)
        assertEquals("Grace", parse.entries[1].firstName)
        assertEquals(2, parse.entries[0].line)
        assertEquals(3, parse.entries[1].line)
    }

    @Test
    fun `a quoted Last-comma-First name is read as one field and split the right way round`() {
        val parse = CsvFormat.parseRoster("\"Doe, Jane\",9007\n")

        assertEquals(1, parse.entries.size)
        assertEquals("Jane", parse.entries[0].firstName)
        assertEquals("Doe", parse.entries[0].lastName)
        assertEquals("9007", parse.entries[0].externalId)
    }

    @Test
    fun `a name without an external id still imports`() {
        val parse = CsvFormat.parseRoster("Alan Turing\n")

        assertEquals(1, parse.entries.size)
        assertEquals("Alan", parse.entries[0].firstName)
        assertEquals("Turing", parse.entries[0].lastName)
        assertEquals("", parse.entries[0].externalId)
    }

    @Test
    fun `a row with no name is reported rather than dropped in silence`() {
        val parse = CsvFormat.parseRoster("Ada Lovelace,9001\n,9002\nGrace Hopper\n \n")

        assertEquals(2, parse.entries.size)
        assertEquals(1, parse.skipped.size)
        assertEquals(2, parse.skipped[0].line)
        assertEquals(RosterSkipReason.NoNameInFirstColumn, parse.skipped[0].reason)
    }

    @Test
    fun `a name split falls back to the last space and handles a single word`() {
        assertEquals("Ada" to "Lovelace", CsvFormat.splitName("Ada Lovelace"))
        assertEquals("Ada" to "Lovelace", CsvFormat.splitName("  Ada   Lovelace  "))
        assertEquals("Ada" to "", CsvFormat.splitName("Ada"))
        assertEquals("Jane" to "Doe", CsvFormat.splitName("Doe, Jane"))
        assertEquals("Mary Ann" to "Doe", CsvFormat.splitName("Mary Ann Doe"))
    }
}

package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.domain.model.GradeKind
import java.util.Locale

/**
 * CSV in both directions, following RFC 4180.
 *
 * Quoting is not cosmetic here. Teachers' exports are full of `Last, First` names and of names with
 * an apostrophe or a comma; an unquoted comma turns one student into two columns and silently
 * corrupts every column after it, which is the kind of defect that is only noticed after the
 * teacher has already imported the file into the school system.
 *
 * An empty cell is exported as empty. Writing `0` for "not handed in" would invent a mark.
 */
object CsvFormat {

    private const val CRLF = "\r\n"

    data class RosterEntry(
        val firstName: String,
        val lastName: String,
        val externalId: String,
        val line: Int,
    )

    data class SkippedRow(val line: Int, val reason: RosterSkipReason)

    data class RosterParse(val entries: List<RosterEntry>, val skipped: List<SkippedRow>)

    /** Escapes one field, adding quotes only when the field would otherwise break the row. */
    fun escapeField(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    /** The full class export: a students-by-assignments matrix of stored values plus the averages. */
    fun encodeClass(classroom: Classroom, labels: CsvLabels): String {
        val assignments = classroom.assignments
        val decimals = classroom.rounding.decimals.coerceIn(0, 2)
        val cells = classroom.grades.associateBy { it.studentId to it.assignmentId }

        val header = buildList {
            add(labels.student)
            add(labels.externalId)
            assignments.forEach { add("${it.name} (${trimNumber(it.points)})") }
            add(labels.overallPercent)
            add(labels.letter)
            add(labels.missingCountedAsZero)
            add(labels.rounding)
        }

        val lines = ArrayList<String>(classroom.students.size + 1)
        lines += header.joinToString(",") { escapeField(it) }

        classroom.students.forEach { student ->
            val row = buildList {
                add(student.displayName)
                add(student.externalId)
                assignments.forEach { assignment ->
                    val cell = cells[student.id to assignment.id]
                    add(
                        when (cell?.kind) {
                            GradeKind.SCORE -> trimNumber(cell.points)
                            GradeKind.MISSING -> labels.missing
                            GradeKind.EXEMPT -> labels.exempt
                            null -> ""
                        },
                    )
                }
                val average = GradeMath.studentAverage(classroom, student.id)
                val overall = average.overall
                add(if (overall == null) "" else formatNumber(overall, decimals))
                add(if (overall == null) "" else GradeMath.letterFor(overall, classroom.gradeScale).orEmpty())
                add(if (classroom.missingCountsAsZero) labels.yes else labels.no)
                add(labels.roundingRule)
            }
            lines += row.joinToString(",") { escapeField(it) }
        }

        return lines.joinToString(CRLF) + CRLF
    }

    /**
     * Reads a roster. The first column is the name; a second column, when present, is treated as
     * the external identifier. A row with no name in the first column is reported rather than
     * dropped in silence.
     *
     * This function cannot know what is already on the roster, so "this student is already here" is
     * decided by the caller — see `RecordBookViewModel.importRoster`, which refuses to add a name
     * twice.
     */
    fun parseRoster(text: String): RosterParse {
        val records = parseRecords(text)
        val entries = ArrayList<RosterEntry>()
        val skipped = ArrayList<SkippedRow>()
        var sawHeader = false

        records.forEachIndexed { index, record ->
            val line = index + 1
            val first = record.firstOrNull()?.trim().orEmpty()
            if (first.isBlank() && record.drop(1).all { it.isBlank() }) return@forEachIndexed

            if (!sawHeader && index == 0 && first.lowercase() in HEADER_WORDS) {
                sawHeader = true
                return@forEachIndexed
            }

            if (first.isBlank()) {
                skipped += SkippedRow(line, RosterSkipReason.NoNameInFirstColumn)
                return@forEachIndexed
            }

            val (first0, last0) = splitName(first)
            entries += RosterEntry(
                firstName = first0,
                lastName = last0,
                externalId = record.getOrNull(1)?.trim().orEmpty(),
                line = line,
            )
        }

        return RosterParse(entries = entries, skipped = skipped)
    }

    /**
     * Splits a displayed name into first and last.
     *
     * `Last, First` is honoured because that is how school systems and CSV exports write names; a
     * comma-separated name must not become a student called "Doe," with the last name "Jane".
     */
    fun splitName(full: String): Pair<String, String> {
        val trimmed = full.trim()
        val comma = trimmed.indexOf(',')
        if (comma > 0) {
            val last = trimmed.substring(0, comma).trim()
            val first = trimmed.substring(comma + 1).trim()
            return first to last
        }
        val lastSpace = trimmed.lastIndexOf(' ')
        return if (lastSpace <= 0) {
            trimmed to ""
        } else {
            trimmed.substring(0, lastSpace).trim() to trimmed.substring(lastSpace + 1).trim()
        }
    }

    private val HEADER_WORDS = setOf("name", "student", "student name", "full name", "имя")

    /** RFC 4180 record splitting: a quoted field may contain commas and escaped quotes. */
    private fun parseRecords(text: String): List<List<String>> {
        val records = ArrayList<List<String>>()
        var fields = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        fun endField() {
            fields.add(field.toString())
            field.setLength(0)
        }

        fun endRecord() {
            endField()
            records.add(fields)
            fields = ArrayList()
        }

        while (index < text.length) {
            val char = text[index]
            when {
                inQuotes && char == '"' -> {
                    if (index + 1 < text.length && text[index + 1] == '"') {
                        field.append('"')
                        index++
                    } else {
                        inQuotes = false
                    }
                }

                inQuotes -> field.append(char)

                char == '"' && field.isEmpty() -> inQuotes = true

                char == ',' -> endField()

                char == '\r' -> {
                    if (index + 1 < text.length && text[index + 1] == '\n') index++
                    endRecord()
                }

                char == '\n' -> endRecord()

                else -> field.append(char)
            }
            index++
        }

        if (field.isNotEmpty() || fields.isNotEmpty()) endRecord()
        return records
    }

    private fun formatNumber(value: Double, decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", value)

    private fun trimNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

data class CsvLabels(
    val student: String,
    val externalId: String,
    val overallPercent: String,
    val letter: String,
    val missingCountedAsZero: String,
    val rounding: String,
    val missing: String,
    val exempt: String,
    val yes: String,
    val no: String,
    val roundingRule: String,
)

enum class RosterSkipReason { NoNameInFirstColumn }

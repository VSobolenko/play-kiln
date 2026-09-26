package com.gexetr.teachergradebook.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.domain.GradeMath
import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.domain.model.AttendanceStatus
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.NoteKind
import com.gexetr.teachergradebook.domain.model.RoundMode
import com.gexetr.teachergradebook.domain.model.RoundingRule
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Every word the printable report draws, already resolved from resources.
 *
 * `ReportPdf` is not a Composable and must not take a `Context`: a renderer that resolves its own
 * strings hides a resource dependency inside a data class and cannot be exercised without Android.
 * The caller — which does have a `Context` — builds this value with [from], and the renderer only
 * draws. `String` fields that contain `%1$s`/`%2$d` are templates the renderer fills.
 *
 * The report's *numbers* deliberately stay in `Locale.US` (dates, decimals, and the `M`/`E` cell
 * codes): they are a document format, not prose, and a school reads them the same in every language.
 */
data class ReportLabels(
    val appName: String,
    val studentColumn: String,
    val overallColumn: String,
    val classAverageFormat: String,
    val notEnoughData: String,
    val columnKeyFormat: String,
    val footerFormat: String,
    val studentGone: String,
    val schoolIdFormat: String,
    val overallFormat: String,
    val periodFormat: String,
    val averagesRuleFormat: String,
    val missingCountsAsZero: String,
    val missingCountsAsExcluded: String,
    val printedFormat: String,
    val categories: String,
    val excludedNoGrade: String,
    val droppedLowestFormat: String,
    val missingCountFormat: String,
    val exemptCountFormat: String,
    val grades: String,
    val missing: String,
    val exempt: String,
    val attendance: String,
    val notes: String,
    val parentContacts: String,
    val followUpFormat: String,
    val roundingRule: String,
    val attendancePresent: String,
    val attendanceTardy: String,
    val attendanceAbsentExcused: String,
    val attendanceAbsentUnexcused: String,
    val noteParticipation: String,
    val noteBehaviour: String,
    val noteMissingWork: String,
    val noteGeneral: String,
) {
    fun attendanceStatus(status: AttendanceStatus): String = when (status) {
        AttendanceStatus.PRESENT -> attendancePresent
        AttendanceStatus.TARDY -> attendanceTardy
        AttendanceStatus.ABSENT_EXCUSED -> attendanceAbsentExcused
        AttendanceStatus.ABSENT_UNEXCUSED -> attendanceAbsentUnexcused
    }

    fun noteKind(kind: NoteKind): String = when (kind) {
        NoteKind.PARTICIPATION -> noteParticipation
        NoteKind.BEHAVIOUR -> noteBehaviour
        NoteKind.MISSING_WORK -> noteMissingWork
        NoteKind.GENERAL -> noteGeneral
    }

    companion object {
        /** Resolves the report's words from the caller's resources, where a language is known. */
        fun from(context: Context, rounding: RoundingRule): ReportLabels = ReportLabels(
            appName = context.getString(R.string.app_name),
            studentColumn = context.getString(R.string.report_student_column),
            overallColumn = context.getString(R.string.report_overall_column),
            classAverageFormat = context.getString(R.string.report_class_average),
            notEnoughData = context.getString(R.string.classes_not_enough_data),
            columnKeyFormat = context.getString(R.string.report_column_key),
            footerFormat = context.getString(R.string.report_footer),
            studentGone = context.getString(R.string.report_student_gone),
            schoolIdFormat = context.getString(R.string.report_school_id),
            overallFormat = context.getString(R.string.report_overall),
            periodFormat = context.getString(R.string.report_period),
            averagesRuleFormat = context.getString(R.string.report_averages_rule),
            missingCountsAsZero = context.getString(R.string.report_missing_counts_zero),
            missingCountsAsExcluded = context.getString(R.string.report_missing_counts_excluded),
            printedFormat = context.getString(R.string.report_printed),
            categories = context.getString(R.string.report_categories),
            excludedNoGrade = context.getString(R.string.report_excluded_no_grade),
            droppedLowestFormat = context.getString(R.string.report_dropped_lowest),
            missingCountFormat = context.getString(R.string.report_missing_count),
            exemptCountFormat = context.getString(R.string.report_exempt_count),
            grades = context.getString(R.string.report_grades),
            missing = context.getString(R.string.report_missing),
            exempt = context.getString(R.string.report_exempt),
            attendance = context.getString(R.string.report_attendance),
            notes = context.getString(R.string.report_notes),
            parentContacts = context.getString(R.string.report_parent_contacts),
            followUpFormat = context.getString(R.string.report_follow_up),
            roundingRule = context.resources.getQuantityString(
                when (rounding.mode) {
                    RoundMode.HALF_UP -> R.plurals.rounding_half_up
                    RoundMode.HALF_EVEN -> R.plurals.rounding_half_even
                    RoundMode.FLOOR -> R.plurals.rounding_floor
                },
                rounding.decimals,
                rounding.decimals,
            ),
            attendancePresent = context.getString(R.string.report_attendance_present),
            attendanceTardy = context.getString(R.string.report_attendance_tardy),
            attendanceAbsentExcused = context.getString(R.string.report_attendance_absent_excused),
            attendanceAbsentUnexcused = context.getString(R.string.report_attendance_absent_unexcused),
            noteParticipation = context.getString(R.string.report_note_participation),
            noteBehaviour = context.getString(R.string.report_note_behaviour),
            noteMissingWork = context.getString(R.string.report_note_missing_work),
            noteGeneral = context.getString(R.string.report_note_general),
        )
    }
}

/**
 * The printable report.
 *
 * Built on the framework's `PdfDocument` rather than a PDF library: a report of names, dates and
 * numbers needs a page, a canvas and a font, so pulling in several hundred kilobytes of dependency
 * to draw a table would be size for its own sake. It also means the report is pure Android
 * framework code with no native component, so there is nothing to check for 16 KB page
 * compatibility.
 *
 * A report never invents a value: an empty cell prints as a dash, never as a zero. The words come in
 * as a [ReportLabels] so they can be translated; the numbers stay in `Locale.US`.
 */
object ReportPdf {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE = 16f
    private const val FOOTER_RESERVE = 34f
    private const val DASH = "—"

    fun classReport(classroom: Classroom, labels: ReportLabels): ByteArray = render(classroom, labels) { writer ->
        val assignments = classroom.assignments
        writer.rule()
        writer.line(
            buildString {
                append(labels.studentColumn.padEnd(24))
                assignments.forEach { append("| ").append(column(it.name)) }
                append("| ").append(labels.overallColumn)
            },
            bold = true,
            size = 9f,
        )
        writer.rule()

        classroom.students.forEach { student ->
            val average = GradeMath.studentAverage(classroom, student.id)
            val cells = assignments.joinToString("") { assignment ->
                val cell = classroom.grades.firstOrNull {
                    it.studentId == student.id && it.assignmentId == assignment.id
                }
                val text = when (cell?.kind) {
                    GradeKind.SCORE -> trimNumber(cell.points)
                    GradeKind.MISSING -> "M"
                    GradeKind.EXEMPT -> "E"
                    null -> DASH
                }
                "| " + text.padEnd(column(assignment.name).length)
            }
            val overall = average.overall?.let { formatNumber(it, classroom.rounding.decimals) } ?: DASH
            val letter = average.overall
                ?.let { GradeMath.letterFor(it, classroom.gradeScale) }
                ?.let { " ($it)" }
                .orEmpty()
            writer.line("${student.displayName.take(24).padEnd(24)}$cells| $overall$letter", size = 9f)
        }

        writer.blank()
        val classAverage = GradeMath.classAverage(classroom)
            ?.let { formatNumber(it, classroom.rounding.decimals) }
            ?: labels.notEnoughData
        writer.line(format(labels.classAverageFormat, classAverage), bold = true)
        if (assignments.isNotEmpty()) {
            writer.blank()
            writer.line(format(labels.columnKeyFormat, DASH), size = 8f)
        }
    }

    fun studentReport(classroom: Classroom, studentId: String, labels: ReportLabels): ByteArray {
        val student = classroom.students.firstOrNull { it.id == studentId }
        if (student == null) {
            return render(classroom, labels) { writer ->
                writer.line(labels.studentGone, bold = true)
            }
        }

        return render(classroom, labels) { writer ->
            val average = GradeMath.studentAverage(classroom, student.id)
            writer.line(student.displayName, bold = true, size = 16f)
            if (student.externalId.isNotBlank()) {
                writer.line(format(labels.schoolIdFormat, student.externalId), size = 10f)
            }
            val overall = average.overall?.let {
                formatNumber(it, classroom.rounding.decimals) +
                    (GradeMath.letterFor(it, classroom.gradeScale)?.let { letter -> " ($letter)" } ?: "")
            } ?: labels.notEnoughData
            writer.line(format(labels.overallFormat, overall), bold = true)

            writer.blank()
            writer.line(labels.categories, bold = true)
            average.categories.forEach { outcome ->
                val value = outcome.average
                    ?.let { formatNumber(it, classroom.rounding.decimals) }
                    ?: labels.excludedNoGrade
                val detail = buildString {
                    if (outcome.droppedCount > 0) append(format(labels.droppedLowestFormat, outcome.droppedCount))
                    if (outcome.missingCount > 0) append(format(labels.missingCountFormat, outcome.missingCount))
                    if (outcome.exemptCount > 0) append(format(labels.exemptCountFormat, outcome.exemptCount))
                }
                writer.line(
                    "${outcome.categoryName} (${trimNumber(outcome.weight)}%): $value$detail",
                    size = 10f,
                )
            }

            writer.blank()
            writer.line(labels.grades, bold = true)
            classroom.assignments.forEach { assignment ->
                val cell = classroom.grades.firstOrNull {
                    it.studentId == student.id && it.assignmentId == assignment.id
                }
                val text = when (cell?.kind) {
                    GradeKind.SCORE -> "${trimNumber(cell.points)} / ${trimNumber(assignment.points)}"
                    GradeKind.MISSING -> labels.missing
                    GradeKind.EXEMPT -> labels.exempt
                    null -> DASH
                }
                writer.line("${assignment.name}: $text", size = 10f)
            }

            val attendance = classroom.attendance.filter { it.studentId == student.id }
            if (attendance.isNotEmpty()) {
                writer.blank()
                writer.line(labels.attendance, bold = true)
                attendance.sortedBy { it.date }.forEach { mark ->
                    writer.line(
                        "${mark.date}: ${labels.attendanceStatus(mark.status)}",
                        size = 10f,
                    )
                }
            }

            val notes = classroom.notes.filter { it.studentId == student.id }
            if (notes.isNotEmpty()) {
                writer.blank()
                writer.line(labels.notes, bold = true)
                notes.sortedBy { it.date }.forEach { note ->
                    writer.line(
                        "${note.date} ${labels.noteKind(note.kind)}: ${note.text}",
                        size = 10f,
                    )
                }
            }

            val contacts = classroom.contacts.filter { it.studentId == student.id }
            if (contacts.isNotEmpty()) {
                writer.blank()
                writer.line(labels.parentContacts, bold = true)
                contacts.sortedBy { it.date }.forEach { contact ->
                    val followUp = contact.followUpOn?.let { format(labels.followUpFormat, it) }.orEmpty()
                    writer.line(
                        "${contact.date} ${contact.channel}: ${contact.summary}$followUp",
                        size = 10f,
                    )
                }
            }
        }
    }

    /**
     * Adds a page whenever the next line would not fit, and numbers every page.
     *
     * This is a writer rather than a helper that returns the new `y`, because a long roster is the
     * normal case: without real pagination the teacher silently loses everyone after the first
     * page, and a silently truncated record is worse than no report at all.
     */
    private class PageWriter(
        private val document: PdfDocument,
        private val classroom: Classroom,
        private val labels: ReportLabels,
    ) {
        private var pageNumber = 1
        private var page: PdfDocument.Page = startPage()
        private var y = MARGIN

        private fun startPage(): PdfDocument.Page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
        )

        private fun footer() {
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = 8f
                color = Color.DKGRAY
            }
            page.canvas.drawText(
                format(labels.footerFormat, labels.appName, pageNumber),
                MARGIN,
                PAGE_HEIGHT - 18f,
                paint,
            )
        }

        private fun openNextPage() {
            footer()
            document.finishPage(page)
            pageNumber++
            page = startPage()
            y = MARGIN
        }

        fun line(text: String, bold: Boolean = false, size: Float = 10f) {
            if (y > PAGE_HEIGHT - FOOTER_RESERVE - LINE) openNextPage()
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = size
                isFakeBoldText = bold
                color = Color.BLACK
            }
            val maxWidth = PAGE_WIDTH - (MARGIN * 2)
            page.canvas.drawText(clip(text, paint, maxWidth), MARGIN, y, paint)
            y += LINE
        }

        fun rule() {
            if (y > PAGE_HEIGHT - FOOTER_RESERVE - LINE) openNextPage()
            page.canvas.drawLine(MARGIN, y - 4f, PAGE_WIDTH - MARGIN, y - 4f, Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.6f
            })
            y += LINE / 2f
        }

        fun blank() = line("")

        fun finish() {
            footer()
            document.finishPage(page)
        }
    }

    /** Page furniture is drawn once per page, so the writer owns the title and the header block. */
    private fun render(classroom: Classroom, labels: ReportLabels, body: (PageWriter) -> Unit): ByteArray {
        val document = PdfDocument()
        val writer = PageWriter(document, classroom, labels)

        writer.line(classroom.name, bold = true, size = 18f)
        val subtitle = listOfNotNull(
            classroom.subject.takeIf { it.isNotBlank() },
            classroom.period.takeIf { it.isNotBlank() }?.let { format(labels.periodFormat, it) },
            classroom.term.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        if (subtitle.isNotBlank()) writer.line(subtitle, size = 11f)
        writer.line(
            format(
                labels.averagesRuleFormat,
                labels.roundingRule,
                if (classroom.missingCountsAsZero) labels.missingCountsAsZero else labels.missingCountsAsExcluded,
            ),
            size = 9f,
        )
        writer.line(
            format(
                labels.printedFormat,
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()),
            ),
            size = 9f,
        )
        writer.blank()

        body(writer)
        writer.finish()

        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    private fun clip(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }

    private fun column(name: String): String {
        val short = if (name.length <= 8) name else name.take(7) + "…"
        return short.padEnd(8)
    }

    /** Fills a translated template. `Locale.US` keeps the digits in the document format the report uses. */
    private fun format(template: String, vararg args: Any): String = String.format(Locale.US, template, *args)

    private fun formatNumber(value: Double, decimals: Int): String =
        String.format(Locale.US, "%.${decimals.coerceIn(0, 2)}f", value)

    private fun trimNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

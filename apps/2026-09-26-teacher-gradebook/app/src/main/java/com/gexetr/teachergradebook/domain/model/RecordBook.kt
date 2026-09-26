package com.gexetr.teachergradebook.domain.model

import kotlinx.serialization.Serializable

/**
 * The whole record, as one document.
 *
 * [schemaVersion] is what makes the backup promise honest: a restore refuses a document it does
 * not understand instead of partially applying it, so a teacher can never lose the current record
 * to a file from a different build.
 */
@Serializable
data class RecordBook(
    val schemaVersion: Int = SCHEMA_VERSION,
    val classes: List<Classroom> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class Classroom(
    val id: String,
    val name: String,
    val subject: String = "",
    val period: String = "",
    val term: String = "",
    val categories: List<Category> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val students: List<Student> = emptyList(),
    val grades: List<GradeCell> = emptyList(),
    val attendance: List<AttendanceMark> = emptyList(),
    val notes: List<RecordNote> = emptyList(),
    val contacts: List<ParentContact> = emptyList(),
    val seating: SeatingPlan = SeatingPlan(),
    val gradeScale: List<GradeBand> = emptyList(),
    val rounding: RoundingRule = RoundingRule(),
    /**
     * Stored per classroom and shown beside every average, because the same number means two
     * different things depending on this rule and a teacher must be able to explain it.
     */
    val missingCountsAsZero: Boolean = true,
)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val weight: Double,
    /** How many of the lowest percentages in this category are removed before averaging. */
    val dropLowest: Int = 0,
)

@Serializable
data class Assignment(
    val id: String,
    val categoryId: String,
    val name: String,
    val points: Double,
    val date: String? = null,
)

@Serializable
data class Student(
    val id: String,
    val firstName: String,
    val lastName: String,
    /** The identifier the teacher already uses in the school system, so records can be matched. */
    val externalId: String = "",
    val active: Boolean = true,
) {
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

@Serializable
enum class GradeKind { SCORE, MISSING, EXEMPT }

@Serializable
data class GradeCell(
    val studentId: String,
    val assignmentId: String,
    val kind: GradeKind,
    /** Only meaningful for [GradeKind.SCORE]. */
    val points: Double = 0.0,
)

@Serializable
enum class AttendanceStatus { PRESENT, TARDY, ABSENT_EXCUSED, ABSENT_UNEXCUSED }

@Serializable
data class AttendanceMark(
    val studentId: String,
    /** ISO date, `YYYY-MM-DD`, so a stored record sorts and compares without a locale. */
    val date: String,
    val status: AttendanceStatus,
)

@Serializable
enum class NoteKind { PARTICIPATION, BEHAVIOUR, MISSING_WORK, GENERAL }

@Serializable
data class RecordNote(
    val id: String,
    val studentId: String,
    val date: String,
    val kind: NoteKind,
    val text: String,
)

@Serializable
data class ParentContact(
    val id: String,
    val studentId: String,
    val date: String,
    val channel: String,
    val summary: String,
    val followUpOn: String? = null,
)

/**
 * Desks are addressed by a stable index (`row * columns + column`) rather than a nested list, so
 * resizing the grid cannot silently reassign a student to a different desk.
 */
@Serializable
data class SeatingPlan(
    val rows: Int = 0,
    val columns: Int = 0,
    val seats: Map<Int, String> = emptyMap(),
) {
    val deskCount: Int get() = rows * columns

    fun studentAt(desk: Int): String? = seats[desk]

    fun deskOf(studentId: String): Int? = seats.entries.firstOrNull { it.value == studentId }?.key
}

@Serializable
data class GradeBand(val minPercent: Double, val letter: String)

@Serializable
enum class RoundMode { HALF_UP, HALF_EVEN, FLOOR }

@Serializable
data class RoundingRule(
    val mode: RoundMode = RoundMode.HALF_UP,
    val decimals: Int = 1,
)

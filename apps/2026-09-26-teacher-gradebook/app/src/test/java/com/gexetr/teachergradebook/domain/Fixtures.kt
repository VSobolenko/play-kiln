package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.Assignment
import com.gexetr.teachergradebook.domain.model.Category
import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.domain.model.GradeBand
import com.gexetr.teachergradebook.domain.model.GradeCell
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.RoundingRule
import com.gexetr.teachergradebook.domain.model.Student

/**
 * Shared fixture for the domain tests.
 *
 * The arithmetic fixtures are computed by hand in the test bodies (see the comments there), never by
 * calling the production implementation to produce its own expectation — an expectation generated
 * by the code under test cannot fail.
 */
object Fixtures {
    const val CLASS_ID = "class-1"
    const val CW = "cat-coursework"
    const val EXAM = "cat-exam"

    const val A1 = "as-1"
    const val A2 = "as-2"
    const val A3 = "as-3"
    const val E1 = "as-exam-1"

    const val S1 = "st-1"
    const val S2 = "st-2"
    const val S3 = "st-3"

    fun categories(): List<Category> = listOf(
        Category(id = CW, name = "Coursework", weight = 60.0, dropLowest = 1),
        Category(id = EXAM, name = "Exams", weight = 40.0, dropLowest = 0),
    )

    fun assignments(): List<Assignment> = listOf(
        Assignment(id = A1, categoryId = CW, name = "Quiz 1", points = 10.0),
        Assignment(id = A2, categoryId = CW, name = "Quiz 2", points = 20.0),
        Assignment(id = A3, categoryId = CW, name = "Project", points = 20.0),
        Assignment(id = E1, categoryId = EXAM, name = "Midterm", points = 100.0),
    )

    fun students(): List<Student> = listOf(
        Student(id = S1, firstName = "Ada", lastName = "Lovelace", externalId = "9001"),
        Student(id = S2, firstName = "Grace", lastName = "Hopper", externalId = "9002"),
        Student(id = S3, firstName = "Alan", lastName = "Turing", externalId = "9003"),
    )

    fun classroom(
        missingCountsAsZero: Boolean = true,
        rounding: RoundingRule = RoundingRule(),
        gradeScale: List<GradeBand> = emptyList(),
        grades: List<GradeCell> = emptyList(),
    ): Classroom = Classroom(
        id = CLASS_ID,
        name = "Physics 2",
        categories = categories(),
        assignments = assignments(),
        students = students(),
        grades = grades,
        missingCountsAsZero = missingCountsAsZero,
        rounding = rounding,
        gradeScale = gradeScale,
    )

    fun score(studentId: String, assignmentId: String, points: Double): GradeCell =
        GradeCell(studentId = studentId, assignmentId = assignmentId, kind = GradeKind.SCORE, points = points)

    fun missing(studentId: String, assignmentId: String): GradeCell =
        GradeCell(studentId = studentId, assignmentId = assignmentId, kind = GradeKind.MISSING)

    fun exempt(studentId: String, assignmentId: String): GradeCell =
        GradeCell(studentId = studentId, assignmentId = assignmentId, kind = GradeKind.EXEMPT)
}

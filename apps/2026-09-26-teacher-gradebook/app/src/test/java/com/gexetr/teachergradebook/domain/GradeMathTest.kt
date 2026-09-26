package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.Assignment
import com.gexetr.teachergradebook.domain.model.Category
import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.domain.model.GradeBand
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.RoundMode
import com.gexetr.teachergradebook.domain.model.RoundingRule
import com.gexetr.teachergradebook.domain.model.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arithmetic contract, with every expectation computed by hand in the comment above it.
 *
 * A fixture whose expected value came from running the implementation would pass even if the
 * implementation were wrong, which is the failure mode this file exists to avoid.
 */
class GradeMathTest {

    private val eps = 1e-9

    /**
     * Ada: Quiz 1 10/10 = 100 %, Quiz 2 10/20 = 50 %, Project 20/20 = 100 %, Midterm 80/100 = 80 %.
     * Coursework drops its lowest, so 50 % goes: (100 + 100) / 2 = 100.
     * Overall = (60 × 100 + 40 × 80) / 100 = (6000 + 3200) / 100 = 92.
     */
    @Test
    fun `weighted average drops the lowest and weights the categories`() {
        val classroom = Fixtures.classroom(
            grades = listOf(
                Fixtures.score(Fixtures.S1, Fixtures.A1, 10.0),
                Fixtures.score(Fixtures.S1, Fixtures.A2, 10.0),
                Fixtures.score(Fixtures.S1, Fixtures.A3, 20.0),
                Fixtures.score(Fixtures.S1, Fixtures.E1, 80.0),
            ),
        )

        val average = GradeMath.studentAverage(classroom, Fixtures.S1)

        assertEquals(92.0, average.overall!!, eps)
        val coursework = average.categories.first { it.categoryId == Fixtures.CW }
        assertEquals(100.0, coursework.average!!, eps)
        assertEquals(1, coursework.droppedCount)
        assertEquals(2, coursework.usedCount)
        assertTrue(average.excludedCategoryIds.isEmpty())
    }

    /**
     * Grace: every coursework cell is exempt, Midterm 90/100 = 90 %.
     * Coursework has nothing to average, so it is excluded and the overall is the exam alone: 90.
     * It is never counted as zero, which would have produced 0.4 × 90 = 36.
     */
    @Test
    fun `a category whose every cell is exempt is excluded, not counted as zero`() {
        val classroom = Fixtures.classroom(
            grades = listOf(
                Fixtures.exempt(Fixtures.S2, Fixtures.A1),
                Fixtures.exempt(Fixtures.S2, Fixtures.A2),
                Fixtures.exempt(Fixtures.S2, Fixtures.A3),
                Fixtures.score(Fixtures.S2, Fixtures.E1, 90.0),
            ),
        )

        val average = GradeMath.studentAverage(classroom, Fixtures.S2)

        assertEquals(90.0, average.overall!!, eps)
        assertEquals(listOf(Fixtures.CW), average.excludedCategoryIds)
        val coursework = average.categories.first { it.categoryId == Fixtures.CW }
        assertTrue(coursework.isExcluded)
        assertNull(coursework.average)
        assertEquals(3, coursework.exemptCount)
    }

    /** Alan has nothing recorded anywhere: the answer is "no data", not zero and not NaN. */
    @Test
    fun `no grade anywhere yields a null overall rather than zero`() {
        val average = GradeMath.studentAverage(Fixtures.classroom(), Fixtures.S3)

        assertNull(average.overall)
        assertFalse(average.hasAnyData)
        assertEquals(listOf(Fixtures.CW, Fixtures.EXAM), average.excludedCategoryIds)
    }

    /**
     * Ada, Quiz 1 missing, Quiz 2 10/20 = 50 %, Project 20/20 = 100 %, Midterm 80/100 = 80 %.
     * Missing counts as zero: percentages 0, 50, 100 → drop the lowest (0) → (50 + 100) / 2 = 75.
     * Missing excluded instead: percentages 50, 100 → drop the lowest (50) → 100.
     * The same cells therefore produce 75 or 100, which is exactly why the rule is stored and shown.
     */
    @Test
    fun `the missing-work rule changes the answer and both answers are correct`() {
        val grades = listOf(
            Fixtures.missing(Fixtures.S1, Fixtures.A1),
            Fixtures.score(Fixtures.S1, Fixtures.A2, 10.0),
            Fixtures.score(Fixtures.S1, Fixtures.A3, 20.0),
            Fixtures.score(Fixtures.S1, Fixtures.E1, 80.0),
        )

        val countsAsZero = GradeMath.studentAverage(
            Fixtures.classroom(missingCountsAsZero = true, grades = grades),
            Fixtures.S1,
        )
        val excluded = GradeMath.studentAverage(
            Fixtures.classroom(missingCountsAsZero = false, grades = grades),
            Fixtures.S1,
        )

        val courseworkZero = countsAsZero.categories.first { it.categoryId == Fixtures.CW }
        val courseworkExcluded = excluded.categories.first { it.categoryId == Fixtures.CW }

        assertEquals(75.0, courseworkZero.average!!, eps)
        assertEquals(1, courseworkZero.missingCount)
        assertEquals(100.0, courseworkExcluded.average!!, eps)

        // Overall: (60 × 75 + 40 × 80) / 100 = (4500 + 3200) / 100 = 77.
        assertEquals(77.0, countsAsZero.overall!!, eps)
        // Overall: (60 × 100 + 40 × 80) / 100 = (6000 + 3200) / 100 = 92.
        assertEquals(92.0, excluded.overall!!, eps)
    }

    /**
     * Dropping more grades than exist must not empty the category: with one grade and dropLowest 5,
     * nothing is dropped and the average stays 8/10 = 80 %.
     */
    @Test
    fun `drop lowest never removes the last remaining grade`() {
        val classroom = Classroom(
            id = Fixtures.CLASS_ID,
            name = "Physics 2",
            categories = listOf(Category(id = "c", name = "Only", weight = 100.0, dropLowest = 5)),
            assignments = listOf(Assignment(id = "a", categoryId = "c", name = "Quiz", points = 10.0)),
            students = listOf(Student(id = Fixtures.S1, firstName = "Ada", lastName = "Lovelace")),
            grades = listOf(Fixtures.score(Fixtures.S1, "a", 8.0)),
        )

        val outcome = GradeMath.categoryOutcome(classroom, classroom.categories.first(), Fixtures.S1)

        assertEquals(80.0, outcome.average!!, eps)
        assertEquals(0, outcome.droppedCount)
        assertEquals(1, outcome.usedCount)
    }

    /** A cleared cell contributes nothing; it is not a zero-width contribution to the mean. */
    @Test
    fun `an absent cell is skipped entirely`() {
        val classroom = Fixtures.classroom(
            grades = listOf(Fixtures.score(Fixtures.S1, Fixtures.A3, 20.0)),
        )

        val coursework = GradeMath.categoryOutcome(classroom, classroom.categories.first(), Fixtures.S1)

        assertEquals(100.0, coursework.average!!, eps)
        assertEquals(1, coursework.usedCount)
    }

    /** A single-word name still parses, and a name with no grades yields no percent. */
    @Test
    fun `an assignment worth zero points yields no percent instead of an infinity`() {
        val worthless = Assignment(id = "z", categoryId = "c", name = "Freebie", points = 0.0)

        assertNull(GradeMath.assignmentPercent(Fixtures.score("s", "z", 5.0), worthless, true))
        assertNull(GradeMath.assignmentPercent(null, worthless, true))
        assertEquals(
            50.0,
            GradeMath.assignmentPercent(
                Fixtures.score("s", "z", 5.0),
                worthless.copy(points = 10.0),
                true,
            )!!,
            eps,
        )
    }

    @Test
    fun `rounding modes are applied as stated`() {
        assertEquals(92.3, GradeMath.round(92.25, RoundingRule(RoundMode.HALF_UP, 1)), eps)
        assertEquals(92.2, GradeMath.round(92.25, RoundingRule(RoundMode.HALF_EVEN, 1)), eps)
        assertEquals(92.2, GradeMath.round(92.29, RoundingRule(RoundMode.FLOOR, 1)), eps)
        assertEquals(93.0, GradeMath.round(92.5, RoundingRule(RoundMode.HALF_UP, 0)), eps)
        assertEquals(92.0, GradeMath.round(92.5, RoundingRule(RoundMode.HALF_EVEN, 0)), eps)
        // Decimals are clamped to the 0..2 the UI offers, so a rule of 5 behaves as 2.
        assertEquals(92.25, GradeMath.round(92.25, RoundingRule(RoundMode.HALF_UP, 5)), eps)
    }

    @Test
    fun `a letter is resolved from the teacher's own scale`() {
        val scale = listOf(
            GradeBand(minPercent = 90.0, letter = "A"),
            GradeBand(minPercent = 80.0, letter = "B"),
            GradeBand(minPercent = 70.0, letter = "C"),
        )

        assertEquals("A", GradeMath.letterFor(95.0, scale))
        assertEquals("A", GradeMath.letterFor(90.0, scale))
        assertEquals("B", GradeMath.letterFor(89.9, scale))
        assertEquals("C", GradeMath.letterFor(70.0, scale))
        assertNull(GradeMath.letterFor(69.9, scale))
        assertNull(GradeMath.letterFor(95.0, emptyList()))
    }

    /**
     * Ada 92, Grace 90, Alan no data, and an inactive student with a perfect score.
     * The class average is over the students who have data and are active: (92 + 90) / 2 = 91.
     */
    @Test
    fun `the class average ignores students with no data and inactive students`() {
        val classroom = Fixtures.classroom(
            grades = listOf(
                Fixtures.score(Fixtures.S1, Fixtures.A1, 10.0),
                Fixtures.score(Fixtures.S1, Fixtures.A2, 10.0),
                Fixtures.score(Fixtures.S1, Fixtures.A3, 20.0),
                Fixtures.score(Fixtures.S1, Fixtures.E1, 80.0),
                Fixtures.exempt(Fixtures.S2, Fixtures.A1),
                Fixtures.exempt(Fixtures.S2, Fixtures.A2),
                Fixtures.exempt(Fixtures.S2, Fixtures.A3),
                Fixtures.score(Fixtures.S2, Fixtures.E1, 90.0),
                Fixtures.score("st-inactive", Fixtures.E1, 100.0),
            ),
        ).let { it.copy(students = it.students + Student("st-inactive", "Left", "School", active = false)) }

        assertEquals(91.0, GradeMath.classAverage(classroom)!!, eps)
    }

    @Test
    fun `a grade cell kind is never confused with a score of zero`() {
        val classroom = Fixtures.classroom(
            grades = listOf(Fixtures.missing(Fixtures.S3, Fixtures.A1)),
        )

        val cell = classroom.grades.first()
        assertEquals(GradeKind.MISSING, cell.kind)
        assertEquals(0.0, cell.points, eps)
        assertNull(
            GradeMath.assignmentPercent(cell, classroom.assignments.first(), missingCountsAsZero = false),
        )
    }
}

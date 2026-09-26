package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.AttendanceStatus
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.NoteKind
import com.gexetr.teachergradebook.domain.model.RecordBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Create, edit and delete for every entity — the requirement the measured incumbents fail.
 *
 * "Says deleted but reappeared", "no option to add or delete classes" and a class that can only be
 * renamed are all reproduced here as assertions in the opposite direction.
 */
class RecordBookEditorTest {

    private val classId = "class-1"

    private fun ok(result: EditResult): RecordBook = when (result) {
        is EditResult.Ok -> result.book
        // throw, not JUnit's fail(): fail() returns Unit, which would widen the when-expression to Any.
        is EditResult.Rejected ->
            throw AssertionError("expected the edit to succeed but it was refused: ${result.reason}")
    }

    private fun rejected(result: EditResult): EditRejection = when (result) {
        is EditResult.Ok -> throw AssertionError("expected a refusal but the edit reported success")
        is EditResult.Rejected -> result.reason
    }

    /** A class with one of everything, built only through the editor. */
    private fun populatedBook(): RecordBook {
        var book = ok(RecordBookEditor.addClass(RecordBookEditor.createBook(), classId, "Physics 2"))
        book = ok(
            RecordBookEditor.updateClassDetails(book, classId, subject = "Physics", period = "3", term = "Fall"),
        )
        book = ok(RecordBookEditor.addCategory(book, classId, "cat", "Coursework", 100.0))
        book = ok(RecordBookEditor.addAssignment(book, classId, "as", "cat", "Quiz 1", 10.0))
        book = ok(RecordBookEditor.addStudent(book, classId, "st", "Ada", "Lovelace", "9001"))
        book = ok(RecordBookEditor.setGrade(book, classId, "st", "as", GradeKind.SCORE, 8.0))
        book = ok(RecordBookEditor.setAttendance(book, classId, "st", "2026-09-26", AttendanceStatus.TARDY))
        book = ok(
            RecordBookEditor.addNote(book, classId, "n1", "st", "2026-09-26", NoteKind.PARTICIPATION, "Led the lab"),
        )
        book = ok(
            RecordBookEditor.addContact(book, classId, "c1", "st", "2026-09-26", "email", "Discussed the lab"),
        )
        book = ok(RecordBookEditor.resizeSeating(book, classId, 2, 2))
        return ok(RecordBookEditor.setSeat(book, classId, 0, "st"))
    }

    @Test
    fun `a class cannot be created without a name`() {
        assertEquals(
            EditRejection.ClassNeedsName,
            rejected(RecordBookEditor.addClass(RecordBookEditor.createBook(), classId, "   ")),
        )
    }

    /**
     * A stale screen must not receive a success for a class that has been deleted. Before this rule
     * the editor returned `Ok` with an unchanged book, which the UI would have rendered as saved.
     */
    @Test
    fun `every edit for a missing class is refused rather than reported as saved`() {
        val empty = RecordBookEditor.createBook()

        assertEquals(EditRejection.ClassGone, rejected(RecordBookEditor.renameClass(empty, "gone", "New name")))
        assertEquals(EditRejection.ClassGone, rejected(RecordBookEditor.deleteClass(empty, "gone")))
        assertEquals(EditRejection.ClassGone, rejected(RecordBookEditor.addStudent(empty, "gone", "st", "Ada", "Lovelace")))
        assertEquals(EditRejection.ClassGone, rejected(RecordBookEditor.addCategory(empty, "gone", "cat", "CW", 50.0)))
        assertEquals(EditRejection.ClassGone, rejected(RecordBookEditor.addAssignment(empty, "gone", "as", "cat", "Quiz", 10.0)))
        assertEquals(EditRejection.ClassGone, rejected(RecordBookEditor.setGrade(empty, "gone", "st", "as", GradeKind.SCORE, 1.0)))
        assertEquals(EditRejection.ClassGone, rejected(RecordBookEditor.setRounding(empty, "gone", com.gexetr.teachergradebook.domain.model.RoundingRule())))
        assertEquals(EditRejection.ClassGone, rejected(RecordBookEditor.resizeSeating(empty, "gone", 1, 1)))
    }

    @Test
    fun `deleting a class removes it and does not bring it back`() {
        val deleted = ok(RecordBookEditor.deleteClass(populatedBook(), classId))

        assertTrue(deleted.classes.isEmpty())
        assertEquals(0, deleted.classes.size)
    }

    @Test
    fun `deleting a student removes every record that belonged to them`() {
        val before = populatedBook()
        val classroomBefore = before.classes.single()
        assertEquals(1, classroomBefore.grades.size)
        assertEquals(1, classroomBefore.attendance.size)
        assertEquals(1, classroomBefore.notes.size)
        assertEquals(1, classroomBefore.contacts.size)
        assertNotNull(classroomBefore.seating.deskOf("st"))

        val after = ok(RecordBookEditor.deleteStudent(before, classId, "st")).classes.single()

        assertTrue(after.students.isEmpty())
        assertTrue(after.grades.isEmpty())
        assertTrue(after.attendance.isEmpty())
        assertTrue(after.notes.isEmpty())
        assertTrue(after.contacts.isEmpty())
        assertNull(after.seating.deskOf("st"))
    }

    @Test
    fun `deleting a category removes its assignments and their grades`() {
        val after = ok(RecordBookEditor.deleteCategory(populatedBook(), classId, "cat")).classes.single()

        assertTrue(after.categories.isEmpty())
        assertTrue(after.assignments.isEmpty())
        assertTrue(after.grades.isEmpty())
    }

    @Test
    fun `deleting an assignment removes its grades`() {
        val after = ok(RecordBookEditor.deleteAssignment(populatedBook(), classId, "as")).classes.single()

        assertTrue(after.assignments.isEmpty())
        assertTrue(after.grades.isEmpty())
    }

    @Test
    fun `a zero or negative weight is refused`() {
        val book = ok(RecordBookEditor.addClass(RecordBookEditor.createBook(), classId, "Physics 2"))

        assertEquals(EditRejection.CategoryWeightNotPositive, rejected(RecordBookEditor.addCategory(book, classId, "c", "CW", 0.0)))
        assertEquals(EditRejection.CategoryWeightNotPositive, rejected(RecordBookEditor.addCategory(book, classId, "c", "CW", -10.0)))
        assertEquals(EditRejection.DropLowestNegative, rejected(RecordBookEditor.addCategory(book, classId, "c", "CW", 50.0, dropLowest = -1)))
        assertEquals(1, ok(RecordBookEditor.addCategory(book, classId, "c", "CW", 50.0)).classes.single().categories.size)
    }

    @Test
    fun `a non-positive points value is refused rather than stored`() {
        val book = ok(
            RecordBookEditor.addCategory(
                ok(RecordBookEditor.addClass(RecordBookEditor.createBook(), classId, "Physics 2")),
                classId,
                "cat",
                "CW",
                100.0,
            ),
        )

        assertEquals(EditRejection.AssignmentPointsNotPositive, rejected(RecordBookEditor.addAssignment(book, classId, "as", "cat", "Quiz", 0.0)))
        assertEquals(EditRejection.AssignmentPointsNotPositive, rejected(RecordBookEditor.addAssignment(book, classId, "as", "cat", "Quiz", -5.0)))
        assertEquals(EditRejection.CategoryGone, rejected(RecordBookEditor.addAssignment(book, classId, "as", "nope", "Quiz", 10.0)))
        assertEquals(1, ok(RecordBookEditor.addAssignment(book, classId, "as", "cat", "Quiz", 10.0)).classes.single().assignments.size)
    }

    @Test
    fun `a score above the assignment maximum or below zero is refused`() {
        val book = populatedBook()

        assertEquals(EditRejection.EarnedPointsAboveMaximum(10.0), rejected(RecordBookEditor.setGrade(book, classId, "st", "as", GradeKind.SCORE, 11.0)))
        assertEquals(EditRejection.EarnedPointsNegative, rejected(RecordBookEditor.setGrade(book, classId, "st", "as", GradeKind.SCORE, -1.0)))
        assertEquals(
            10.0,
            ok(RecordBookEditor.setGrade(book, classId, "st", "as", GradeKind.SCORE, 10.0))
                .classes.single().grades.single().points,
            1e-9,
        )
    }

    @Test
    fun `writing the same cell twice replaces it instead of adding a second one`() {
        var book = populatedBook()
        book = ok(RecordBookEditor.setGrade(book, classId, "st", "as", GradeKind.SCORE, 3.0))
        book = ok(RecordBookEditor.setGrade(book, classId, "st", "as", GradeKind.MISSING))

        val cells = book.classes.single().grades
        assertEquals(1, cells.size)
        assertEquals(GradeKind.MISSING, cells.single().kind)
    }

    @Test
    fun `a cleared grade cell is absent and never becomes a stored zero`() {
        val book = populatedBook()
        val cleared = ok(RecordBookEditor.clearGrade(book, classId, "st", "as"))

        assertEquals(0, cleared.classes.single().grades.size)
        // Re-reading the same cell produces no cell rather than SCORE 0.
        assertTrue(cleared.classes.single().grades.none { it.studentId == "st" })
    }

    @Test
    fun `an exempt cell stores no points`() {
        val book = ok(RecordBookEditor.setGrade(populatedBook(), classId, "st", "as", GradeKind.EXEMPT))
        val cell = book.classes.single().grades.single()

        assertEquals(GradeKind.EXEMPT, cell.kind)
        assertEquals(0.0, cell.points, 1e-9)
    }

    @Test
    fun `attendance is written once per student and date`() {
        var book = populatedBook()
        book = ok(RecordBookEditor.setAttendance(book, classId, "st", "2026-09-26", AttendanceStatus.PRESENT))
        book = ok(RecordBookEditor.setAttendance(book, classId, "st", "2026-09-27", AttendanceStatus.ABSENT_UNEXCUSED))

        val marks = book.classes.single().attendance
        assertEquals(2, marks.size)
        assertEquals(
            AttendanceStatus.PRESENT,
            marks.first { it.date == "2026-09-26" }.status,
        )

        val cleared = ok(RecordBookEditor.clearAttendance(book, classId, "st", "2026-09-26"))
        assertEquals(1, cleared.classes.single().attendance.size)
    }

    @Test
    fun `mark all present covers the roster and keeps a later correction`() {
        var book = populatedBook()
        book = ok(RecordBookEditor.addStudent(book, classId, "st2", "Grace", "Hopper"))
        book = ok(RecordBookEditor.markAllPresent(book, classId, "2026-10-01"))
        assertEquals(2, book.classes.single().attendance.count { it.date == "2026-10-01" })
        assertEquals(
            2,
            book.classes.single().attendance.count { it.date == "2026-10-01" && it.status == AttendanceStatus.PRESENT },
        )

        book = ok(RecordBookEditor.setAttendance(book, classId, "st2", "2026-10-01", AttendanceStatus.ABSENT_EXCUSED))
        val forThatDay = book.classes.single().attendance.filter { it.date == "2026-10-01" }
        assertEquals(2, forThatDay.size)
        assertEquals(
            AttendanceStatus.ABSENT_EXCUSED,
            forThatDay.first { it.studentId == "st2" }.status,
        )
    }

    @Test
    fun `notes and contacts are editable and deletable`() {
        var book = populatedBook()
        book = ok(RecordBookEditor.updateNote(book, classId, "n1", "2026-09-27", NoteKind.BEHAVIOUR, "Talked over others"))
        val note = book.classes.single().notes.single()
        assertEquals("Talked over others", note.text)
        assertEquals(NoteKind.BEHAVIOUR, note.kind)

        book = ok(
            RecordBookEditor.updateContact(
                book, classId, "c1", "2026-09-28", "phone", "Called about homework", "2026-10-05",
            ),
        )
        val contact = book.classes.single().contacts.single()
        assertEquals("phone", contact.channel)
        assertEquals("2026-10-05", contact.followUpOn)

        book = ok(RecordBookEditor.deleteNote(book, classId, "n1"))
        book = ok(RecordBookEditor.deleteContact(book, classId, "c1"))
        assertTrue(book.classes.single().notes.isEmpty())
        assertTrue(book.classes.single().contacts.isEmpty())
    }

    @Test
    fun `a note without text or a contact without a summary is refused`() {
        val book = populatedBook()

        assertEquals(
            EditRejection.NoteNeedsText,
            rejected(RecordBookEditor.addNote(book, classId, "n2", "st", "2026-09-26", NoteKind.GENERAL, "  ")),
        )
        assertEquals(
            EditRejection.ContactNeedsSummary,
            rejected(RecordBookEditor.addContact(book, classId, "c2", "st", "2026-09-26", "email", " ")),
        )
        assertEquals(
            EditRejection.ContactNeedsChannel,
            rejected(RecordBookEditor.addContact(book, classId, "c2", "st", "2026-09-26", " ", "Called")),
        )
    }

    /** A student moved to another desk must not be sitting at two desks at once. */
    @Test
    fun `moving a seated student leaves exactly one desk occupied by them`() {
        val moved = ok(RecordBookEditor.setSeat(populatedBook(), classId, 3, "st")).classes.single()

        assertEquals(1, moved.seating.seats.count { it.value == "st" })
        assertEquals(3, moved.seating.deskOf("st"))
    }

    @Test
    fun `a desk outside the grid is refused`() {
        assertEquals(EditRejection.DeskOutsideGrid, rejected(RecordBookEditor.setSeat(populatedBook(), classId, 4, "st")))
        assertEquals(EditRejection.DeskOutsideGrid, rejected(RecordBookEditor.setSeat(populatedBook(), classId, -1, "st")))
    }

    @Test
    fun `shrinking the grid drops seats that no longer exist instead of remapping them`() {
        // The seat goes on the last desk of a 2x2 grid on purpose: that desk is the one a shrink to
        // 1x1 removes, and remapping it onto desk 0 would silently move a student.
        val seated = ok(RecordBookEditor.setSeat(populatedBook(), classId, 3, "st"))
        val shrunk = ok(RecordBookEditor.resizeSeating(seated, classId, 1, 1)).classes.single()

        assertEquals(1, shrunk.seating.deskCount)
        assertTrue("a seat on a desk that no longer exists must be dropped", shrunk.seating.seats.isEmpty())
        assertNull(shrunk.seating.deskOf("st"))
    }

    @Test
    fun `a seat that still fits is kept when the grid shrinks`() {
        // The other half of the rule above: shrinking must not clear the seats that still have a desk.
        val shrunk = ok(RecordBookEditor.resizeSeating(populatedBook(), classId, 2, 1)).classes.single()

        assertEquals(0, shrunk.seating.deskOf("st"))
    }

    @Test
    fun `a full grid has no desk to offer`() {
        var book = ok(RecordBookEditor.resizeSeating(populatedBook(), classId, 1, 1))
        book = ok(RecordBookEditor.addStudent(book, classId, "st2", "Grace", "Hopper"))

        assertEquals(EditRejection.SeatingGridFull, rejected(RecordBookEditor.moveStudentToNextFreeDesk(book, classId, "st2")))
    }

    @Test
    fun `a grid larger than twenty by twenty is refused`() {
        assertEquals(EditRejection.SeatingGridTooLarge, rejected(RecordBookEditor.resizeSeating(populatedBook(), classId, 21, 1)))
        assertEquals(EditRejection.SeatingGridNegative, rejected(RecordBookEditor.resizeSeating(populatedBook(), classId, -1, 1)))
    }

    @Test
    fun `class details, rounding and the grade scale are stored as edited`() {
        var book = populatedBook()
        book = ok(RecordBookEditor.renameClass(book, classId, "Physics 2 — Period 3"))
        book = ok(
            RecordBookEditor.setRounding(
                book,
                classId,
                com.gexetr.teachergradebook.domain.model.RoundingRule(
                    com.gexetr.teachergradebook.domain.model.RoundMode.HALF_EVEN,
                    0,
                ),
            ),
        )
        book = ok(
            RecordBookEditor.setGradeScale(
                book,
                classId,
                listOf(
                    com.gexetr.teachergradebook.domain.model.GradeBand(90.0, "A"),
                    com.gexetr.teachergradebook.domain.model.GradeBand(80.0, "B"),
                    com.gexetr.teachergradebook.domain.model.GradeBand(0.0, " "),
                ),
            ),
        )

        val classroom = book.classes.single()
        assertEquals("Physics 2 — Period 3", classroom.name)
        assertEquals(0, classroom.rounding.decimals)
        // The blank-letter band is dropped, so the scale holds only the bands a teacher wrote.
        assertEquals(listOf("A", "B"), classroom.gradeScale.map { it.letter })
    }
}

package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.Assignment
import com.gexetr.teachergradebook.domain.model.AttendanceMark
import com.gexetr.teachergradebook.domain.model.AttendanceStatus
import com.gexetr.teachergradebook.domain.model.Category
import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.domain.model.GradeBand
import com.gexetr.teachergradebook.domain.model.GradeCell
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.NoteKind
import com.gexetr.teachergradebook.domain.model.ParentContact
import com.gexetr.teachergradebook.domain.model.RecordBook
import com.gexetr.teachergradebook.domain.model.RecordNote
import com.gexetr.teachergradebook.domain.model.RoundingRule
import com.gexetr.teachergradebook.domain.model.SeatingPlan
import com.gexetr.teachergradebook.domain.model.Student

/**
 * Every create, edit and delete, as a pure function.
 *
 * This file exists because of what the measured incumbents get wrong: a class that can only be
 * renamed, a subject that "says deleted but reappeared", and no way to remove what you added. So
 * deletion here is a real removal, and it cascades — deleting a class takes its students, grades,
 * attendance, notes, contacts and seating with it, because leaving orphaned records behind is
 * exactly how a deleted thing comes back.
 *
 * Two rules hold for the whole file:
 *
 * - **Refusals are values, not exceptions.** An editor that throws cannot tell a teacher why a
 *   weight of zero was rejected, and the UI would have nothing to show.
 * - **No edit reports success it did not achieve.** Every mutation resolves the class first, so a
 *   stale screen cannot get an `Ok` for a class that was deleted in the meantime — the false
 *   success that turns into "it says it saved but nothing happened".
 */
sealed interface EditResult {
    data class Ok(val book: RecordBook) : EditResult
    data class Rejected(val reason: EditRejection) : EditResult
}

sealed interface EditRejection {
    data object ClassNeedsName : EditRejection
    data object ClassGone : EditRejection
    data object StudentNeedsName : EditRejection
    data object StudentGone : EditRejection
    data object CategoryNeedsName : EditRejection
    data object CategoryWeightNotPositive : EditRejection
    data object DropLowestNegative : EditRejection
    data object CategoryGone : EditRejection
    data object CategoryAlreadyAtEnd : EditRejection
    data object AssignmentNeedsName : EditRejection
    data object AssignmentPointsNotPositive : EditRejection
    data object AssignmentGone : EditRejection
    data object EarnedPointsNegative : EditRejection
    data class EarnedPointsAboveMaximum(val maximum: Double) : EditRejection
    data object NoteNeedsText : EditRejection
    data object NoteGone : EditRejection
    data object ContactNeedsChannel : EditRejection
    data object ContactNeedsSummary : EditRejection
    data object ContactGone : EditRejection
    data object SeatingGridNegative : EditRejection
    data object SeatingGridTooLarge : EditRejection
    data object DeskOutsideGrid : EditRejection
    data object SeatingGridFull : EditRejection
}

object RecordBookEditor {

    fun createBook(): RecordBook = RecordBook()

    // ---------------------------------------------------------------- classes

    fun addClass(book: RecordBook, id: String, name: String): EditResult {
        if (name.isBlank()) return EditResult.Rejected(EditRejection.ClassNeedsName)
        val classroom = Classroom(id = id, name = name.trim())
        return EditResult.Ok(book.copy(classes = book.classes + classroom))
    }

    fun renameClass(book: RecordBook, classId: String, name: String): EditResult {
        if (name.isBlank()) return EditResult.Rejected(EditRejection.ClassNeedsName)
        return editClass(book, classId) { it.copy(name = name.trim()) }
    }

    fun updateClassDetails(
        book: RecordBook,
        classId: String,
        subject: String,
        period: String,
        term: String,
    ): EditResult = editClass(book, classId) {
        it.copy(subject = subject.trim(), period = period.trim(), term = term.trim())
    }

    /** Removes the class and everything recorded inside it. */
    fun deleteClass(book: RecordBook, classId: String): EditResult {
        if (book.classes.none { it.id == classId }) {
            return EditResult.Rejected(EditRejection.ClassGone)
        }
        return EditResult.Ok(book.copy(classes = book.classes.filterNot { it.id == classId }))
    }

    fun setRounding(book: RecordBook, classId: String, rule: RoundingRule): EditResult =
        editClass(book, classId) { it.copy(rounding = rule) }

    fun setMissingCountsAsZero(book: RecordBook, classId: String, countsAsZero: Boolean): EditResult =
        editClass(book, classId) { it.copy(missingCountsAsZero = countsAsZero) }

    fun setGradeScale(book: RecordBook, classId: String, scale: List<GradeBand>): EditResult {
        val cleaned = scale
            .filter { it.letter.isNotBlank() }
            .map { it.copy(letter = it.letter.trim(), minPercent = it.minPercent.coerceIn(0.0, 100.0)) }
        return editClass(book, classId) { it.copy(gradeScale = cleaned) }
    }

    // ---------------------------------------------------------------- students

    fun addStudent(
        book: RecordBook,
        classId: String,
        id: String,
        firstName: String,
        lastName: String,
        externalId: String = "",
    ): EditResult {
        if (firstName.isBlank() && lastName.isBlank()) {
            return EditResult.Rejected(EditRejection.StudentNeedsName)
        }
        val student = Student(
            id = id,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            externalId = externalId.trim(),
        )
        return editClass(book, classId) { it.copy(students = it.students + student) }
    }

    fun updateStudent(
        book: RecordBook,
        classId: String,
        studentId: String,
        firstName: String,
        lastName: String,
        externalId: String,
        active: Boolean,
    ): EditResult {
        if (firstName.isBlank() && lastName.isBlank()) {
            return EditResult.Rejected(EditRejection.StudentNeedsName)
        }
        if (book.classes.firstOrNull { it.id == classId }?.students?.none { it.id == studentId } != false) {
            return EditResult.Rejected(EditRejection.StudentGone)
        }
        return editClass(book, classId) { classroom ->
            classroom.copy(
                students = classroom.students.map { student ->
                    if (student.id != studentId) {
                        student
                    } else {
                        student.copy(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            externalId = externalId.trim(),
                            active = active,
                        )
                    }
                },
            )
        }
    }

    /** Removes the student and every record that belonged to them. */
    fun deleteStudent(book: RecordBook, classId: String, studentId: String): EditResult =
        editClass(book, classId) { classroom -> classroom.withoutStudent(studentId) }

    // ------------------------------------------------- categories and assignments

    fun addCategory(
        book: RecordBook,
        classId: String,
        id: String,
        name: String,
        weight: Double,
        dropLowest: Int = 0,
    ): EditResult {
        if (name.isBlank()) return EditResult.Rejected(EditRejection.CategoryNeedsName)
        if (weight <= 0.0 || weight.isNaN()) {
            return EditResult.Rejected(EditRejection.CategoryWeightNotPositive)
        }
        if (dropLowest < 0) return EditResult.Rejected(EditRejection.DropLowestNegative)
        val category = Category(id = id, name = name.trim(), weight = weight, dropLowest = dropLowest)
        return editClass(book, classId) { it.copy(categories = it.categories + category) }
    }

    fun updateCategory(
        book: RecordBook,
        classId: String,
        categoryId: String,
        name: String,
        weight: Double,
        dropLowest: Int,
    ): EditResult {
        if (name.isBlank()) return EditResult.Rejected(EditRejection.CategoryNeedsName)
        if (weight <= 0.0 || weight.isNaN()) {
            return EditResult.Rejected(EditRejection.CategoryWeightNotPositive)
        }
        if (dropLowest < 0) return EditResult.Rejected(EditRejection.DropLowestNegative)
        if (book.classes.firstOrNull { it.id == classId }?.categories?.none { it.id == categoryId } != false) {
            return EditResult.Rejected(EditRejection.CategoryGone)
        }
        return editClass(book, classId) { classroom ->
            classroom.copy(
                categories = classroom.categories.map { category ->
                    if (category.id != categoryId) {
                        category
                    } else {
                        category.copy(name = name.trim(), weight = weight, dropLowest = dropLowest)
                    }
                },
            )
        }
    }

    /**
     * Moves a category one place earlier ([delta] negative) or later ([delta] positive).
     *
     * Order is not decoration in a gradebook: the category list is how the average is explained, so
     * a teacher moving "Exams" above "Coursework" is editing the explanation they read out. A move
     * past either end is refused as a no-op rather than silently clamped, so a tap at the edge does
     * not report a change that did not happen.
     */
    fun moveCategory(book: RecordBook, classId: String, categoryId: String, delta: Int): EditResult {
        val classroom = book.classes.firstOrNull { it.id == classId }
            ?: return EditResult.Rejected(EditRejection.ClassGone)
        val index = classroom.categories.indexOfFirst { it.id == categoryId }
        if (index < 0) return EditResult.Rejected(EditRejection.CategoryGone)
        val target = index + delta
        if (target < 0 || target > classroom.categories.lastIndex) {
            return EditResult.Rejected(EditRejection.CategoryAlreadyAtEnd)
        }
        return editClass(book, classId) { current ->
            val reordered = current.categories.toMutableList()
            val moved = reordered.removeAt(index)
            reordered.add(target, moved)
            current.copy(categories = reordered)
        }
    }

    /** Removes the category, its assignments and the grades recorded against them. */
    fun deleteCategory(book: RecordBook, classId: String, categoryId: String): EditResult =
        editClass(book, classId) { classroom ->
            val removedAssignmentIds = classroom.assignments
                .filter { it.categoryId == categoryId }
                .map { it.id }
                .toSet()
            classroom.copy(
                categories = classroom.categories.filterNot { it.id == categoryId },
                assignments = classroom.assignments.filterNot { it.categoryId == categoryId },
                grades = classroom.grades.filterNot { it.assignmentId in removedAssignmentIds },
            )
        }

    fun addAssignment(
        book: RecordBook,
        classId: String,
        id: String,
        categoryId: String,
        name: String,
        points: Double,
        date: String? = null,
    ): EditResult {
        if (name.isBlank()) return EditResult.Rejected(EditRejection.AssignmentNeedsName)
        if (points <= 0.0 || points.isNaN()) {
            return EditResult.Rejected(EditRejection.AssignmentPointsNotPositive)
        }
        val classroom = book.classes.firstOrNull { it.id == classId }
            ?: return EditResult.Rejected(EditRejection.ClassGone)
        if (classroom.categories.none { it.id == categoryId }) {
            return EditResult.Rejected(EditRejection.CategoryGone)
        }
        return editClass(book, classId) { it ->
            it.copy(
                assignments = it.assignments +
                    Assignment(
                        id = id,
                        categoryId = categoryId,
                        name = name.trim(),
                        points = points,
                        date = date,
                    ),
            )
        }
    }

    fun updateAssignment(
        book: RecordBook,
        classId: String,
        assignmentId: String,
        name: String,
        points: Double,
        categoryId: String,
    ): EditResult {
        if (name.isBlank()) return EditResult.Rejected(EditRejection.AssignmentNeedsName)
        if (points <= 0.0 || points.isNaN()) {
            return EditResult.Rejected(EditRejection.AssignmentPointsNotPositive)
        }
        val classroom = book.classes.firstOrNull { it.id == classId }
            ?: return EditResult.Rejected(EditRejection.ClassGone)
        if (classroom.assignments.none { it.id == assignmentId }) {
            return EditResult.Rejected(EditRejection.AssignmentGone)
        }
        if (classroom.categories.none { it.id == categoryId }) {
            return EditResult.Rejected(EditRejection.CategoryGone)
        }
        return editClass(book, classId) { it ->
            it.copy(
                assignments = it.assignments.map { assignment ->
                    if (assignment.id != assignmentId) {
                        assignment
                    } else {
                        assignment.copy(
                            name = name.trim(),
                            points = points,
                            categoryId = categoryId,
                        )
                    }
                },
            )
        }
    }

    /** Removes the assignment and its grade cells. */
    fun deleteAssignment(book: RecordBook, classId: String, assignmentId: String): EditResult =
        editClass(book, classId) { classroom ->
            classroom.copy(
                assignments = classroom.assignments.filterNot { it.id == assignmentId },
                grades = classroom.grades.filterNot { it.assignmentId == assignmentId },
            )
        }

    // ---------------------------------------------------------------- grades

    /**
     * Writes one cell. `SCORE` requires a point value inside the assignment's range; the other two
     * kinds carry no value. An out-of-range score is refused rather than clamped, because a
     * silently clamped mark is a wrong mark.
     */
    fun setGrade(
        book: RecordBook,
        classId: String,
        studentId: String,
        assignmentId: String,
        kind: GradeKind,
        points: Double = 0.0,
    ): EditResult {
        val classroom = book.classes.firstOrNull { it.id == classId }
            ?: return EditResult.Rejected(EditRejection.ClassGone)
        val assignment = classroom.assignments.firstOrNull { it.id == assignmentId }
            ?: return EditResult.Rejected(EditRejection.AssignmentGone)

        if (kind == GradeKind.SCORE) {
            if (points.isNaN() || points < 0.0) {
                return EditResult.Rejected(EditRejection.EarnedPointsNegative)
            }
            if (points > assignment.points) {
                return EditResult.Rejected(EditRejection.EarnedPointsAboveMaximum(assignment.points))
            }
        }

        val value = if (kind == GradeKind.SCORE) points else 0.0
        return editClass(book, classId) { it ->
            it.copy(
                grades = it.grades.upsert(
                    match = { cell -> cell.studentId == studentId && cell.assignmentId == assignmentId },
                    cell = GradeCell(studentId, assignmentId, kind, value),
                ),
            )
        }
    }

    /** Deletes one cell. A cleared cell is absent, never a stored zero. */
    fun clearGrade(book: RecordBook, classId: String, studentId: String, assignmentId: String): EditResult =
        editClass(book, classId) { classroom ->
            classroom.copy(
                grades = classroom.grades.filterNot {
                    it.studentId == studentId && it.assignmentId == assignmentId
                },
            )
        }

    // ---------------------------------------------------------------- attendance

    fun setAttendance(
        book: RecordBook,
        classId: String,
        studentId: String,
        date: String,
        status: AttendanceStatus,
    ): EditResult = editClass(book, classId) { classroom ->
        classroom.copy(
            attendance = classroom.attendance.upsert(
                match = { mark -> mark.studentId == studentId && mark.date == date },
                cell = AttendanceMark(studentId, date, status),
            ),
        )
    }

    fun clearAttendance(book: RecordBook, classId: String, studentId: String, date: String): EditResult =
        editClass(book, classId) { classroom ->
            classroom.copy(
                attendance = classroom.attendance.filterNot {
                    it.studentId == studentId && it.date == date
                },
            )
        }

    /** Marks every student in the class present on [date]. One tap for the common case. */
    fun markAllPresent(book: RecordBook, classId: String, date: String): EditResult =
        editClass(book, classId) { classroom ->
            val updated = classroom.attendance.toMutableList()
            classroom.students.forEach { student ->
                val existing = updated.indexOfFirst { it.studentId == student.id && it.date == date }
                val mark = AttendanceMark(student.id, date, AttendanceStatus.PRESENT)
                if (existing >= 0) updated[existing] = mark else updated += mark
            }
            classroom.copy(attendance = updated)
        }

    // ---------------------------------------------------------------- notes

    fun addNote(
        book: RecordBook,
        classId: String,
        id: String,
        studentId: String,
        date: String,
        kind: NoteKind,
        text: String,
    ): EditResult {
        if (text.isBlank()) return EditResult.Rejected(EditRejection.NoteNeedsText)
        return editClass(book, classId) { classroom ->
            classroom.copy(
                notes = classroom.notes + RecordNote(id, studentId, date, kind, text.trim()),
            )
        }
    }

    fun updateNote(
        book: RecordBook,
        classId: String,
        noteId: String,
        date: String,
        kind: NoteKind,
        text: String,
    ): EditResult {
        if (text.isBlank()) return EditResult.Rejected(EditRejection.NoteNeedsText)
        if (book.classes.firstOrNull { it.id == classId }?.notes?.none { it.id == noteId } != false) {
            return EditResult.Rejected(EditRejection.NoteGone)
        }
        return editClass(book, classId) { classroom ->
            classroom.copy(
                notes = classroom.notes.map { note ->
                    if (note.id != noteId) {
                        note
                    } else {
                        note.copy(date = date, kind = kind, text = text.trim())
                    }
                },
            )
        }
    }

    fun deleteNote(book: RecordBook, classId: String, noteId: String): EditResult =
        editClass(book, classId) { it.copy(notes = it.notes.filterNot { note -> note.id == noteId }) }

    // ---------------------------------------------------------------- contacts

    fun addContact(
        book: RecordBook,
        classId: String,
        id: String,
        studentId: String,
        date: String,
        channel: String,
        summary: String,
        followUpOn: String? = null,
    ): EditResult {
        if (channel.isBlank()) return EditResult.Rejected(EditRejection.ContactNeedsChannel)
        if (summary.isBlank()) return EditResult.Rejected(EditRejection.ContactNeedsSummary)
        return editClass(book, classId) { classroom ->
            classroom.copy(
                contacts = classroom.contacts +
                    ParentContact(id, studentId, date, channel.trim(), summary.trim(), followUpOn),
            )
        }
    }

    fun updateContact(
        book: RecordBook,
        classId: String,
        contactId: String,
        date: String,
        channel: String,
        summary: String,
        followUpOn: String?,
    ): EditResult {
        if (channel.isBlank()) return EditResult.Rejected(EditRejection.ContactNeedsChannel)
        if (summary.isBlank()) return EditResult.Rejected(EditRejection.ContactNeedsSummary)
        if (book.classes.firstOrNull { it.id == classId }?.contacts?.none { it.id == contactId } != false) {
            return EditResult.Rejected(EditRejection.ContactGone)
        }
        return editClass(book, classId) { classroom ->
            classroom.copy(
                contacts = classroom.contacts.map { contact ->
                    if (contact.id != contactId) {
                        contact
                    } else {
                        contact.copy(
                            date = date,
                            channel = channel.trim(),
                            summary = summary.trim(),
                            followUpOn = followUpOn,
                        )
                    }
                },
            )
        }
    }

    fun deleteContact(book: RecordBook, classId: String, contactId: String): EditResult =
        editClass(book, classId) {
            it.copy(contacts = it.contacts.filterNot { contact -> contact.id == contactId })
        }

    // ---------------------------------------------------------------- seating

    fun resizeSeating(book: RecordBook, classId: String, rows: Int, columns: Int): EditResult {
        if (rows < 0 || columns < 0) return EditResult.Rejected(EditRejection.SeatingGridNegative)
        if (rows > 20 || columns > 20) return EditResult.Rejected(EditRejection.SeatingGridTooLarge)
        return editClass(book, classId) { classroom ->
            val plan = classroom.seating
            val desks = rows * columns
            // Seats outside the new grid belong to desks that no longer exist; keep the ones that
            // survive instead of remapping them onto different desks.
            val kept = plan.seats.filterKeys { it in 0 until desks }
            classroom.copy(seating = SeatingPlan(rows = rows, columns = columns, seats = kept))
        }
    }

    /**
     * Puts [studentId] at [desk], or clears the desk when [studentId] is null. A student who
     * already sits somewhere is **moved**, so one student can never occupy two desks.
     */
    fun setSeat(book: RecordBook, classId: String, desk: Int, studentId: String?): EditResult {
        val classroom = book.classes.firstOrNull { it.id == classId }
            ?: return EditResult.Rejected(EditRejection.ClassGone)
        if (desk !in 0 until classroom.seating.deskCount) {
            // A desk outside the grid does not exist, and dropping the tap silently would leave the
            // teacher believing a student had been seated.
            return EditResult.Rejected(EditRejection.DeskOutsideGrid)
        }
        return editClass(book, classId) { it ->
            val plan = it.seating
            val withoutStudent = plan.seats.filterValues { value -> value != studentId }
            val updated = if (studentId == null) {
                withoutStudent - desk
            } else {
                withoutStudent + (desk to studentId)
            }
            it.copy(seating = plan.copy(seats = updated))
        }
    }

    fun moveStudentToNextFreeDesk(book: RecordBook, classId: String, studentId: String): EditResult {
        val classroom = book.classes.firstOrNull { it.id == classId }
            ?: return EditResult.Rejected(EditRejection.ClassGone)
        val free = (0 until classroom.seating.deskCount)
            .firstOrNull { desk -> desk !in classroom.seating.seats.keys }
            ?: return EditResult.Rejected(EditRejection.SeatingGridFull)
        return editClass(book, classId) { it ->
            val plan = it.seating
            val withoutStudent = plan.seats.filterValues { value -> value != studentId }
            it.copy(seating = plan.copy(seats = withoutStudent + (free to studentId)))
        }
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Applies [transform] to one class, refusing when the class is gone.
     *
     * Every mutation goes through here so the "no false success" rule is structural: there is no
     * path that builds an `Ok` for a class that does not exist.
     */
    private fun editClass(
        book: RecordBook,
        classId: String,
        transform: (Classroom) -> Classroom,
    ): EditResult {
        if (book.classes.none { it.id == classId }) {
            return EditResult.Rejected(EditRejection.ClassGone)
        }
        return EditResult.Ok(
            book.copy(classes = book.classes.map { if (it.id == classId) transform(it) else it }),
        )
    }

    private fun Classroom.withoutStudent(studentId: String): Classroom = copy(
        students = students.filterNot { it.id == studentId },
        grades = grades.filterNot { it.studentId == studentId },
        attendance = attendance.filterNot { it.studentId == studentId },
        notes = notes.filterNot { it.studentId == studentId },
        contacts = contacts.filterNot { it.studentId == studentId },
        seating = seating.copy(seats = seating.seats.filterValues { it != studentId }),
    )

    private fun <T> List<T>.upsert(match: (T) -> Boolean, cell: T): List<T> =
        if (any(match)) map { if (match(it)) cell else it } else this + cell

}

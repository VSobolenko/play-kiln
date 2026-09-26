package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.AttendanceStatus
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.NoteKind
import com.gexetr.teachergradebook.domain.model.RecordBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Backup and restore.
 *
 * The promise being tested is the one the measured incumbent broke: a teacher who is "scared to
 * uninstall and reinstall" because the purchase and the data might not survive. So the round trip
 * must reproduce everything, and a refusal must change nothing.
 */
class BackupCodecTest {

    private val classId = "class-1"

    private fun ok(result: EditResult): RecordBook = when (result) {
        is EditResult.Ok -> result.book
        // throw, not JUnit's fail(): fail() returns Unit, which would widen the when-expression to Any.
        is EditResult.Rejected ->
            throw AssertionError("expected the edit to succeed but it was refused: ${result.reason}")
    }

    /** Every kind of record the app can hold, so a round trip cannot pass by ignoring a field. */
    private fun fullRecord(): RecordBook {
        var book = ok(RecordBookEditor.addClass(RecordBookEditor.createBook(), classId, "Physics 2"))
        book = ok(RecordBookEditor.updateClassDetails(book, classId, "Physics", "3", "Fall 2026"))
        book = ok(RecordBookEditor.addCategory(book, classId, "cat", "Coursework", 60.0, dropLowest = 1))
        book = ok(RecordBookEditor.addCategory(book, classId, "exam", "Exams", 40.0))
        book = ok(RecordBookEditor.addAssignment(book, classId, "as1", "cat", "Quiz 1", 10.0, "2026-09-10"))
        book = ok(RecordBookEditor.addAssignment(book, classId, "as2", "cat", "Quiz 2", 20.0))
        book = ok(RecordBookEditor.addAssignment(book, classId, "as3", "exam", "Midterm", 100.0))
        book = ok(RecordBookEditor.addStudent(book, classId, "st1", "Ada", "Lovelace", "9001"))
        book = ok(RecordBookEditor.addStudent(book, classId, "st2", "Grace", "Hopper", "9002"))
        book = ok(RecordBookEditor.setGrade(book, classId, "st1", "as1", GradeKind.SCORE, 10.0))
        book = ok(RecordBookEditor.setGrade(book, classId, "st1", "as2", GradeKind.MISSING))
        book = ok(RecordBookEditor.setGrade(book, classId, "st2", "as1", GradeKind.EXEMPT))
        book = ok(RecordBookEditor.setGrade(book, classId, "st2", "as3", GradeKind.SCORE, 90.0))
        book = ok(RecordBookEditor.setAttendance(book, classId, "st1", "2026-09-26", AttendanceStatus.TARDY))
        book = ok(RecordBookEditor.addNote(book, classId, "n1", "st1", "2026-09-26", NoteKind.MISSING_WORK, "Quiz 2 not in"))
        book = ok(
            RecordBookEditor.addContact(book, classId, "c1", "st1", "2026-09-27", "email", "Emailed about Quiz 2", "2026-10-01"),
        )
        book = ok(RecordBookEditor.resizeSeating(book, classId, 2, 3))
        book = ok(RecordBookEditor.setSeat(book, classId, 0, "st1"))
        book = ok(RecordBookEditor.setSeat(book, classId, 5, "st2"))
        return book
    }

    @Test
    fun `a full record round-trips through the backup format`() {
        val original = fullRecord()

        val encoded = BackupCodec.encode(original)
        val decoded = BackupCodec.decode(encoded)

        assertTrue(decoded is BackupCodec.DecodeResult.Ok)
        val restored = (decoded as BackupCodec.DecodeResult.Ok).book
        assertEquals(original, restored)
        assertEquals(RecordBook.SCHEMA_VERSION, restored.schemaVersion)
        assertEquals("Physics 2", restored.classes.single().name)
        assertEquals(3, restored.classes.single().assignments.size)
        assertEquals(2, restored.classes.single().students.size)
        assertEquals(4, restored.classes.single().grades.size)
        assertEquals(1, restored.classes.single().attendance.size)
        assertEquals(1, restored.classes.single().notes.size)
        assertEquals(1, restored.classes.single().contacts.size)
        assertEquals(2, restored.classes.single().seating.seats.size)
        assertEquals("st2", restored.classes.single().seating.studentAt(5))
    }

    /** The scenario the spec names: back up, delete the class, restore, and it is all back. */
    @Test
    fun `a backup restores a class that was deleted after the backup`() {
        val withData = fullRecord()
        val backup = BackupCodec.encode(withData)

        val emptied = ok(RecordBookEditor.deleteClass(withData, classId))
        assertTrue(emptied.classes.isEmpty())

        val restored = (BackupCodec.decode(backup) as BackupCodec.DecodeResult.Ok).book
        val classroom = restored.classes.single()
        assertEquals("Physics 2", classroom.name)
        assertEquals(3, classroom.assignments.size)
        assertEquals(2, classroom.students.size)
        assertEquals(4, classroom.grades.size)
    }

    @Test
    fun `an unsupported schema version is refused with a reason that names both versions`() {
        val encoded = BackupCodec.encode(fullRecord())
        val future = encoded.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        assertNotEquals(encoded, future)

        val result = BackupCodec.decode(future)

        assertTrue(result is BackupCodec.DecodeResult.Rejected)
        assertEquals(
            BackupRejection.UnsupportedVersion(fileVersion = 2, supportedVersion = 1),
            (result as BackupCodec.DecodeResult.Rejected).reason,
        )
    }

    @Test
    fun `a document with no schema version is refused`() {
        val result = BackupCodec.decode("{\"classes\": []}")

        assertTrue(result is BackupCodec.DecodeResult.Rejected)
        assertEquals(BackupRejection.MissingSchemaVersion, (result as BackupCodec.DecodeResult.Rejected).reason)
    }

    @Test
    fun `a malformed document is refused rather than partially applied`() {
        val cases = listOf(
            "not json at all",
            "",
            "   ",
            "[1, 2, 3]",
            "{ \"schemaVersion\": 1, \"classes\": \"not a list\" }",
            "{ \"schemaVersion\": 1, \"classes\": [ { \"id\": 1 } ] }",
        )

        cases.forEach { text ->
            val result = BackupCodec.decode(text)
            if (result !is BackupCodec.DecodeResult.Rejected) {
                fail("expected a refusal for input: $text")
            }
        }
    }

    /** A key this build does not know must be an error, not something silently trimmed away. */
    @Test
    fun `a document carrying unknown keys is refused instead of silently trimmed`() {
        val encoded = BackupCodec.encode(fullRecord())
        val tampered = encoded.replaceFirst("{", "{\n  \"somethingNew\": true,")

        val result = BackupCodec.decode(tampered)
        assertTrue(result is BackupCodec.DecodeResult.Rejected)
    }

    @Test
    fun `an empty book round-trips and is still a valid document`() {
        val encoded = BackupCodec.encode(RecordBook())
        val result = BackupCodec.decode(encoded)

        assertTrue(result is BackupCodec.DecodeResult.Ok)
        assertTrue((result as BackupCodec.DecodeResult.Ok).book.classes.isEmpty())
    }
}

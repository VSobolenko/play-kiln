package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.RecordBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Category order, and the refusals around it.
 *
 * Why a category sits where it does is not cosmetic: the list is the order in which the average is
 * explained and the order in which the CSV columns are written, so moving a category changes what a
 * teacher reads and what leaves the app.
 */
class RecordBookEditorOrderTest {

    private fun ok(result: EditResult): RecordBook = when (result) {
        is EditResult.Ok -> result.book
        // throw, not JUnit's fail(): fail() returns Unit, which would widen the when-expression to Any.
        is EditResult.Rejected ->
            throw AssertionError("expected the edit to succeed but it was refused: ${result.reason}")
    }

    private fun book(vararg names: String): RecordBook {
        // The class has to exist before a category can be filed under it — `createBook()` returns an
        // empty record, and adding a category to a class that is not there is a refusal, not a no-op.
        var current = ok(RecordBookEditor.addClass(RecordBookEditor.createBook(), "c", "Physics"))
        names.forEachIndexed { index, name ->
            current = ok(RecordBookEditor.addCategory(current, "c", "cat$index", name, 25.0))
        }
        return current
    }

    private fun order(book: RecordBook): List<String> =
        book.classes.single().categories.map { it.name }

    @Test
    fun `a category moves one place later`() {
        val moved = RecordBookEditor.moveCategory(book("Coursework", "Exams", "Homework"), "c", "cat0", 1)

        assertTrue(moved is EditResult.Ok)
        assertEquals(listOf("Exams", "Coursework", "Homework"), order((moved as EditResult.Ok).book))
    }

    @Test
    fun `a category moves one place earlier`() {
        val moved = RecordBookEditor.moveCategory(book("Coursework", "Exams"), "c", "cat1", -1)

        assertEquals(listOf("Exams", "Coursework"), order((moved as EditResult.Ok).book))
    }

    @Test
    fun `moving the first category up is refused rather than silently ignored`() {
        val result = RecordBookEditor.moveCategory(book("Coursework", "Exams"), "c", "cat0", -1)

        assertTrue(result is EditResult.Rejected)
        assertEquals(EditRejection.CategoryAlreadyAtEnd, (result as EditResult.Rejected).reason)
    }

    @Test
    fun `moving the last category down is refused`() {
        val result = RecordBookEditor.moveCategory(book("Coursework", "Exams"), "c", "cat1", 1)

        assertTrue(result is EditResult.Rejected)
    }

    @Test
    fun `moving a category that does not exist is refused`() {
        val result = RecordBookEditor.moveCategory(book("Coursework"), "c", "missing", 1)

        assertTrue(result is EditResult.Rejected)
    }

    @Test
    fun `moving inside a class that does not exist is refused`() {
        val result = RecordBookEditor.moveCategory(book("Coursework"), "other-class", "cat0", 1)

        assertTrue(result is EditResult.Rejected)
    }

    /** Order survives a backup, because the CSV column order is the teaching order. */
    @Test
    fun `the order survives a backup round trip`() {
        val original = book("Coursework", "Exams", "Homework")
        val moved = (RecordBookEditor.moveCategory(original, "c", "cat2", -2) as EditResult.Ok).book

        val restored = BackupCodec.decode(BackupCodec.encode(moved))

        assertTrue(restored is BackupCodec.DecodeResult.Ok)
        assertEquals(
            listOf("Homework", "Coursework", "Exams"),
            (restored as BackupCodec.DecodeResult.Ok).book.classes.single().categories.map { it.name },
        )
    }
}

package com.gexetr.teachergradebook.data

import com.gexetr.teachergradebook.domain.EditResult
import com.gexetr.teachergradebook.domain.RecordBookEditor
import com.gexetr.teachergradebook.domain.model.RecordBook
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The record's file, on a real filesystem.
 *
 * Two behaviours matter more than the happy path. A **damaged** file must not be silently replaced
 * by an empty record, because that would destroy a term's work the first time the app opened it. And
 * a write must be atomic, because a truncated document is an unreadable record.
 *
 * The tests run on the JVM against a temporary directory, so they exercise real `File` semantics on
 * the build machine and on the device the app is installed on — the same code path either way.
 */
class FileRecordBookRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File
    private lateinit var repository: FileRecordBookRepository

    @Before
    fun setUp() {
        file = File(folder.root, "recordbook.json")
        repository = FileRecordBookRepository(file)
    }

    @After
    fun tearDown() {
        folder.delete()
    }

    private fun bookWithClass(name: String): RecordBook =
        (RecordBookEditor.addClass(RecordBook(), "c1", name) as EditResult.Ok).book

    @Test
    fun `a first run with no file yields an empty record`() {
        assertFalse(file.exists())
        assertTrue(repository.load().classes.isEmpty())
    }

    @Test
    fun `what is saved is what is read back`() {
        val book = bookWithClass("Physics")

        assertTrue(repository.save(book))

        assertEquals(book, repository.load())
        assertEquals("Physics", repository.load().classes.single().name)
    }

    @Test
    fun `saving leaves no temporary file behind`() {
        repository.save(bookWithClass("Physics"))

        val leftovers = folder.root.listFiles().orEmpty().filter { it.name.endsWith(".tmp") }
        assertTrue("a stray temporary file means the rename did not complete: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun `a damaged file is preserved rather than overwritten`() {
        repository.save(bookWithClass("Physics"))
        file.writeText("{ this is not a record }")

        val loaded = repository.load()

        assertTrue("an unreadable record must not come back as data", loaded.classes.isEmpty())
        assertTrue(
            "the unreadable file must be kept beside the store so it can be recovered",
            File(folder.root, "recordbook.json.damaged").isFile,
        )
    }

    @Test
    fun `a file from an unsupported schema version is treated as unreadable`() {
        repository.save(bookWithClass("Physics"))
        // The encoder pretty-prints, so the raw document holds `"schemaVersion": 1`. Matching the
        // separator loosely keeps this test from quietly testing nothing: a rewrite that misses
        // leaves a valid file behind and every assertion below would still pass.
        val bumped = file.readText().replace(
            Regex("\"schemaVersion\"\\s*:\\s*\\d+"),
            "\"schemaVersion\": 7",
        )
        assertNotEquals(
            "the fixture must actually rewrite the version, otherwise this test proves nothing",
            file.readText(),
            bumped,
        )
        file.writeText(bumped)

        assertTrue(repository.load().classes.isEmpty())
        assertTrue(File(folder.root, "recordbook.json.damaged").isFile)
    }

    @Test
    fun `the last write wins and the file stays readable`() {
        repository.save(bookWithClass("First"))
        repository.save(bookWithClass("Second"))

        assertEquals("Second", repository.load().classes.single().name)
    }

    @Test
    fun `a file that cannot be written reports failure instead of claiming success`() {
        // A directory where the file should be is the cheapest honest way to make the write fail.
        file.delete()
        file.mkdirs()

        assertFalse("a write that did not happen must not report success", repository.save(bookWithClass("Physics")))
    }
}

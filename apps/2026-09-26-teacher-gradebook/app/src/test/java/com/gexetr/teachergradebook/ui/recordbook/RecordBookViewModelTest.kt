package com.gexetr.teachergradebook.ui.recordbook

import com.gexetr.teachergradebook.data.RecordBookRepository
import com.gexetr.teachergradebook.domain.BackupCodec
import com.gexetr.teachergradebook.domain.EditResult
import com.gexetr.teachergradebook.domain.FreeTier
import com.gexetr.teachergradebook.domain.LimitReason
import com.gexetr.teachergradebook.domain.RecordBookEditor
import com.gexetr.teachergradebook.domain.model.RecordBook
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The record's command surface.
 *
 * These tests are about the two things that would be expensive to get wrong in the field: a limit
 * that does not hold (the app gives away what it sells) and a refusal that half-applies (a teacher
 * is told no and finds their roster changed anyway).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordBookViewModelTest {

    private class FakeRepository(var stored: RecordBook = RecordBook()) : RecordBookRepository {
        var saves = 0
        var failNextSave = false

        override fun load(): RecordBook = stored

        override fun save(book: RecordBook): Boolean {
            saves++
            if (failNextSave) return false
            stored = book
            return true
        }
    }

    private lateinit var repository: FakeRepository
    private val io = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(io)
        repository = FakeRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * The ViewModel runs its file reads and writes on an injected dispatcher, so the tests drive that
     * I/O instead of racing a real thread pool. Before injection these tests were decided by timing:
     * `load()` could finish after an edit and hand back the file it had read a moment earlier.
     */
    private fun viewModel(unlocked: Boolean = true) = RecordBookViewModel(
        repository = repository,
        entitlements = { unlocked },
        ids = sequentialIds(),
        io = io,
    )

    /** Deterministic identifiers, because a test that depends on a clock is a flaky test. */
    private fun sequentialIds(): IdSource {
        var counter = 0
        return IdSource { prefix -> "$prefix-${counter++}" }
    }

    @Test
    fun `load reports the stored record and marks itself loaded`() = runTest {
        repository.stored = (RecordBookEditor.addClass(RecordBook(), "c1", "Physics") as EditResult.Ok).book
        val model = viewModel()

        model.load()
        advanceUntilIdle()

        assertTrue(model.state.value.loaded)
        assertEquals(1, model.state.value.book.classes.size)
        assertEquals("Physics", model.state.value.book.classes.single().name)

        // Loading twice must not re-read or double-apply; a recomposition calls it repeatedly.
        model.load()
        advanceUntilIdle()
        assertEquals(1, model.state.value.book.classes.size)
    }

    @Test
    fun `adding a class writes it to the repository`() = runTest {
        val model = viewModel()
        model.load()
        advanceUntilIdle()

        model.addClass("Physics 2")
        advanceUntilIdle()

        assertEquals(1, model.state.value.book.classes.size)
        assertEquals(1, repository.saves)
        assertEquals(1, repository.stored.classes.size)
    }

    @Test
    fun `a second class is refused without the unlock and nothing is written`() = runTest {
        val model = viewModel(unlocked = false)
        model.load()
        advanceUntilIdle()

        model.addClass("First")
        advanceUntilIdle()
        val savesAfterFirst = repository.saves
        val events = mutableListOf<RecordBookEvent>()

        val collector = startCollecting(model, events)
        model.addClass("Second")
        advanceUntilIdle()

        assertEquals("the free tier keeps one class", 1, model.state.value.book.classes.size)
        assertEquals("a refused edit must not be persisted", savesAfterFirst, repository.saves)
        assertEquals(
            "the teacher must be offered the unlock for the class limit",
            LimitReason.SecondClass,
            events.filterIsInstance<RecordBookEvent.Blocked>().singleOrNull()?.reason,
        )
        collector.cancel()
    }

    @Test
    fun `the unlock lifts the class limit`() = runTest {
        val model = viewModel(unlocked = true)
        model.load()
        advanceUntilIdle()

        model.addClass("First")
        model.addClass("Second")
        advanceUntilIdle()

        assertEquals(2, model.state.value.book.classes.size)
    }

    @Test
    fun `the twenty-first student is refused without the unlock`() = runTest {
        val locked = viewModel(unlocked = false)
        locked.load()
        advanceUntilIdle()
        locked.addClass("Class")
        advanceUntilIdle()
        val classId = locked.state.value.book.classes.single().id

        repeat(FreeTier.MAX_STUDENTS_PER_CLASS + 3) { index ->
            locked.addStudent(classId, "Student", "$index")
        }
        advanceUntilIdle()

        assertEquals(
            "the free roster must be exactly what the copy promises",
            FreeTier.MAX_STUDENTS_PER_CLASS,
            locked.state.value.book.classes.single().students.size,
        )
    }

    @Test
    fun `a roster import reports what it read and what the free limit could not hold`() = runTest {
        val model = viewModel(unlocked = false)
        model.load()
        advanceUntilIdle()
        model.addClass("Class")
        advanceUntilIdle()
        val classId = model.state.value.book.classes.single().id
        val events = mutableListOf<RecordBookEvent>()
        val collector = startCollecting(model, events)

        val rows = (1..25).joinToString("\n") { "Student Number $it" }
        model.importRoster(classId, "Name\n$rows")
        advanceUntilIdle()

        assertEquals(FreeTier.MAX_STUDENTS_PER_CLASS, model.state.value.book.classes.single().students.size)
        val summary = events.filterIsInstance<RecordBookEvent.ImportSummary>().single()
        assertEquals("every parsed row that fitted must be reported", FreeTier.MAX_STUDENTS_PER_CLASS, summary.imported)
        assertEquals("the rows the free limit could not hold must be counted", 5, summary.blockedByLimit)
        assertEquals(
            "the limit the copy quotes must be the rule's own",
            FreeTier.MAX_STUDENTS_PER_CLASS,
            summary.studentLimit,
        )
        collector.cancel()
    }

    @Test
    fun `a restore that is refused changes nothing and says why`() = runTest {
        val model = viewModel()
        model.load()
        advanceUntilIdle()
        model.addClass("Keep me")
        advanceUntilIdle()
        val before = model.state.value.book
        val savesBefore = repository.saves
        val events = mutableListOf<RecordBookEvent>()
        val collector = startCollecting(model, events)

        model.restoreBackup("{\"schemaVersion\": 99, \"classes\": []}")
        advanceUntilIdle()

        assertEquals("the current record must survive a refused restore", before, model.state.value.book)
        assertEquals("a refused restore must not touch the disk", savesBefore, repository.saves)
        assertTrue(events.any { it is RecordBookEvent.BackupRejected })
        collector.cancel()
    }

    @Test
    fun `a good backup restores over the current record`() = runTest {
        val model = viewModel()
        model.load()
        advanceUntilIdle()
        model.addClass("Original")
        advanceUntilIdle()

        val other = RecordBookEditor.addClass(RecordBook(), "c9", "From the file")
        val document = BackupCodec.encode((other as EditResult.Ok).book)

        model.restoreBackup(document)
        advanceUntilIdle()

        assertEquals(1, model.state.value.book.classes.size)
        assertEquals("From the file", model.state.value.book.classes.single().name)
        assertEquals(1, repository.stored.classes.size)
    }

    @Test
    fun `an import never adds a student who is already on the roster`() = runTest {
        val model = viewModel()
        model.load()
        advanceUntilIdle()
        model.addClass("Class")
        advanceUntilIdle()
        val classId = model.state.value.book.classes.single().id
        model.addStudent(classId, "Ada", "Lovelace")
        advanceUntilIdle()
        val events = mutableListOf<RecordBookEvent>()
        val collector = startCollecting(model, events)

        model.importRoster(classId, "Name,ID\nAda Lovelace,9001\nGrace Hopper,9002")
        advanceUntilIdle()

        val students = model.state.value.book.classes.single().students
        assertEquals("the roster must hold two students, not three", 2, students.size)
        assertEquals(1, students.count { it.displayName == "Ada Lovelace" })
        val summary = events.filterIsInstance<RecordBookEvent.ImportSummary>().single()
        assertEquals("the new row must be reported as imported", 1, summary.imported)
        assertEquals("the duplicate must be counted, not silently dropped", 1, summary.alreadyOnRoster)
        collector.cancel()
    }

    @Test
    fun `a failed write is reported rather than passing as saved`() = runTest {
        val model = viewModel()
        model.load()
        advanceUntilIdle()
        repository.failNextSave = true
        val events = mutableListOf<RecordBookEvent>()
        val collector = startCollecting(model, events)

        model.addClass("Fragile")
        advanceUntilIdle()

        assertTrue(
            "a write that did not reach the disk must be reported, got: $events",
            events.any { it is RecordBookEvent.StorageFailed },
        )
        collector.cancel()
    }

    @Test
    fun `export and backup are refused without the unlock and allowed with it`() = runTest {
        val locked = viewModel(unlocked = false)
        locked.load()
        advanceUntilIdle()
        assertFalse("export is part of the unlock", locked.requestExport())
        assertFalse("backup is part of the unlock", locked.requestBackup())

        val opened = viewModel(unlocked = true)
        opened.load()
        advanceUntilIdle()
        assertTrue(opened.requestExport())
        assertTrue(opened.requestBackup())
    }

    /**
     * The defect this test exists for: the first file read finished *after* an edit, and the arriving
     * document replaced the whole record — so a class created on a cold start was on screen for a
     * moment and then gone, and the file on disk had been overwritten with it.
     */
    @Test
    fun `an edit that arrives before the stored record is refused instead of erasing it`() = runTest {
        val pendingIo = StandardTestDispatcher(testScheduler)
        repository.stored =
            (RecordBookEditor.addClass(RecordBook(), "c1", "Physics") as EditResult.Ok).book
        val model = RecordBookViewModel(
            repository = repository,
            entitlements = { true },
            ids = sequentialIds(),
            io = pendingIo,
        )

        model.load()                       // queued, deliberately not finished
        model.addClass("Typed too early")  // must be refused, not accepted and then wiped

        assertFalse(
            "an edit must not be accepted before the stored record is in memory",
            model.state.value.book.classes.any { it.name == "Typed too early" },
        )
        advanceUntilIdle()

        assertEquals(
            "the arriving document must not be replaced by a pre-load edit",
            listOf("Physics"),
            model.state.value.book.classes.map { it.name },
        )
        assertEquals("a refused pre-load edit must not touch the disk", 0, repository.saves)
        assertEquals(
            "the stored record must survive a pre-load edit",
            listOf("Physics"),
            repository.stored.classes.map { it.name },
        )
    }

    @Test
    fun `the backup document is exactly the record the app holds`() = runTest {
        val model = viewModel()
        model.load()
        advanceUntilIdle()
        model.addClass("Physics")

        val decoded = BackupCodec.decode(model.backupDocument())
        assertTrue(decoded is BackupCodec.DecodeResult.Ok)
        assertNotNull((decoded as BackupCodec.DecodeResult.Ok).book.classes.firstOrNull())
        assertEquals("Physics", decoded.book.classes.single().name)
    }

    private fun TestScope.startCollecting(
        model: RecordBookViewModel,
        into: MutableList<RecordBookEvent>,
    ): Job = launch(start = CoroutineStart.UNDISPATCHED) {
        model.events.collect { into += it }
    }
}

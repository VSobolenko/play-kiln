package com.gexetr.teachergradebook.ui.recordbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.teachergradebook.data.RecordBookRepository
import com.gexetr.teachergradebook.domain.BackupCodec
import com.gexetr.teachergradebook.domain.CsvFormat
import com.gexetr.teachergradebook.domain.CsvLabels
import com.gexetr.teachergradebook.domain.BackupRejection
import com.gexetr.teachergradebook.domain.EditRejection
import com.gexetr.teachergradebook.domain.EditResult
import com.gexetr.teachergradebook.domain.FreeTier
import com.gexetr.teachergradebook.domain.GradeMath
import com.gexetr.teachergradebook.domain.LimitCheck
import com.gexetr.teachergradebook.domain.LimitReason
import com.gexetr.teachergradebook.domain.RecordBookEditor
import com.gexetr.teachergradebook.domain.model.AttendanceStatus
import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.domain.model.GradeBand
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.NoteKind
import com.gexetr.teachergradebook.domain.model.RecordBook
import com.gexetr.teachergradebook.domain.model.RoundingRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Answers "has the teacher paid?", so the limits have one source of truth outside this class. */
fun interface EntitlementGate {
    fun isUnlocked(): Boolean
}

/** Identifiers are injected so a test can be deterministic instead of timestamp-dependent. */
fun interface IdSource {
    fun next(prefix: String): String
}

data class RecordBookUiState(
    val book: RecordBook = RecordBook(),
    val loaded: Boolean = false,
)

/**
 * One-shot things the shell must do, rather than state it renders.
 *
 * A member carries values, never a finished sentence, because the ViewModel is the one layer that
 * cannot read string resources: it decides *what happened*, and the UI — which knows the teacher's
 * language — decides how to say it. The counts are separate fields rather than interpolated prose so
 * they can be pluralised in a language whose plural rules differ from English.
 */
sealed interface RecordBookEvent {
    /** A roster import finished. [blockedByLimit] rows did not fit [studentLimit] in the free tier. */
    data class ImportSummary(
        val imported: Int,
        val alreadyOnRoster: Int,
        val skipped: List<CsvFormat.SkippedRow>,
        val blockedByLimit: Int,
        val studentLimit: Int,
    ) : RecordBookEvent

    /** A backup replaced the record; [classes] is how many classes arrived. */
    data class Restored(val classes: Int) : RecordBookEvent

    /** The edit is on screen but the write to storage failed, so it will not survive a restart. */
    data object StorageFailed : RecordBookEvent

    /** A free-tier limit refused an action; the UI opens the unlock offer and names the reason. */
    data class Blocked(val reason: LimitReason) : RecordBookEvent

    data class EditRejected(val reason: EditRejection) : RecordBookEvent

    data class BackupRejected(val reason: BackupRejection) : RecordBookEvent

    /**
     * A message already resolved by a screen from its own resources. Domain refusals have typed
     * variants above so they cannot smuggle English through this escape hatch.
     */
    data class Text(val text: String) : RecordBookEvent
}

/**
 * The record's single source of truth.
 *
 * One ViewModel owns the whole document rather than one per screen, and that is deliberate: the
 * record is a single JSON document, so two screens holding two copies would mean two writers of one
 * file and a lost update the first time a teacher graded while a colleague screen was open. Screens
 * observe this state and call actions; nothing else mutates the book.
 *
 * Every mutation goes through the domain editor, which refuses rather than half-applying, and the
 * save happens in the background so an edit never waits on a disk write before the UI responds.
 */
class RecordBookViewModel(
    private val repository: RecordBookRepository,
    private val entitlements: EntitlementGate,
    private val ids: IdSource,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val mutableState = MutableStateFlow(RecordBookUiState())
    val state: StateFlow<RecordBookUiState> = mutableState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<RecordBookEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<RecordBookEvent> = mutableEvents.asSharedFlow()

    private var loadStarted = false

    /** Reads the stored record once. Idempotent, so a recomposition cannot trigger a second read. */
    fun load() {
        if (loadStarted) return
        loadStarted = true
        viewModelScope.launch {
            val book = withContext(io) { repository.load() }
            // The read is applied only while nothing has been edited yet. Committing an edit before
            // the document arrived would otherwise be erased a moment later by the arriving file,
            // and the teacher would watch a class they had just created disappear. See `readyToEdit`.
            mutableState.update { if (it.loaded) it else it.copy(book = book, loaded = true) }
        }
    }

    /**
     * Whether an edit may be applied: only once the stored record is in memory.
     *
     * Without this, every mutation is a race against the first file read — the read finishes last
     * and overwrites whatever was typed in the meantime. The screens cannot produce such an edit
     * (they show a loading state until [RecordBookUiState.loaded]), so this is a structural guard
     * rather than a branch the teacher ever sees.
     */
    private val readyToEdit: Boolean get() = mutableState.value.loaded

    // ------------------------------------------------------------ lookups the screens need

    fun classroom(classId: String): Classroom? = mutableState.value.book.classes.firstOrNull { it.id == classId }

    fun averageOf(classId: String, studentId: String) = classroom(classId)
        ?.let { GradeMath.studentAverage(it, studentId) }

    /** The CSV for a class, or null when the records are not loaded yet. */
    fun csvFor(classId: String, labels: CsvLabels): String? =
        classroom(classId)?.let { CsvFormat.encodeClass(it, labels) }

    /** The backup document, which is exactly the internal store format. */
    fun backupDocument(): String = BackupCodec.encode(mutableState.value.book)

    // ------------------------------------------------------------ classes

    fun addClass(name: String) {
        val check = FreeTier.canAddClass(mutableState.value.book.classes.size, entitlements.isUnlocked())
        if (blocked(check)) return
        apply(RecordBookEditor.addClass(mutableState.value.book, ids.next("class"), name))
    }

    fun renameClass(classId: String, name: String) =
        apply(RecordBookEditor.renameClass(mutableState.value.book, classId, name))

    fun updateClassDetails(classId: String, subject: String, period: String, term: String) =
        apply(RecordBookEditor.updateClassDetails(mutableState.value.book, classId, subject, period, term))

    /** Deleting a class discards everything inside it, so the caller confirms first. */
    fun deleteClass(classId: String) = apply(RecordBookEditor.deleteClass(mutableState.value.book, classId))

    fun setRounding(classId: String, rule: RoundingRule) =
        apply(RecordBookEditor.setRounding(mutableState.value.book, classId, rule))

    fun setMissingCountsAsZero(classId: String, countsAsZero: Boolean) =
        apply(RecordBookEditor.setMissingCountsAsZero(mutableState.value.book, classId, countsAsZero))

    fun setGradeScale(classId: String, bands: List<GradeBand>) =
        apply(RecordBookEditor.setGradeScale(mutableState.value.book, classId, bands))

    // ------------------------------------------------------------ students

    fun addStudent(classId: String, firstName: String, lastName: String, externalId: String = "") {
        val rosterSize = classroom(classId)?.students?.size ?: 0
        val check = FreeTier.canAddStudent(rosterSize, entitlements.isUnlocked())
        if (blocked(check)) return
        apply(
            RecordBookEditor.addStudent(
                mutableState.value.book, classId, ids.next("student"), firstName, lastName, externalId,
            ),
        )
    }

    fun updateStudent(
        classId: String,
        studentId: String,
        firstName: String,
        lastName: String,
        externalId: String,
        active: Boolean,
    ) = apply(
        RecordBookEditor.updateStudent(
            mutableState.value.book, classId, studentId, firstName, lastName, externalId, active,
        ),
    )

    fun deleteStudent(classId: String, studentId: String) =
        apply(RecordBookEditor.deleteStudent(mutableState.value.book, classId, studentId))

    /**
     * Imports a roster the teacher picked.
     *
     * The counts are reported and nothing is dropped in silence: a row that cannot be read is named,
     * and a row that the free tier cannot hold is counted and offered the unlock instead of being
     * quietly discarded.
     */
    fun importRoster(classId: String, text: String) {
        val classroom = classroom(classId) ?: return
        val parsed = CsvFormat.parseRoster(text)
        val unlocked = entitlements.isUnlocked()
        val roomLeft = if (unlocked) {
            Int.MAX_VALUE
        } else {
            (FreeTier.MAX_STUDENTS_PER_CLASS - classroom.students.size).coerceAtLeast(0)
        }

        var book = mutableState.value.book
        var imported = 0
        var blockedByLimit = 0
        var alreadyOnRoster = 0
        parsed.entries.forEach { entry ->
            if (imported >= roomLeft) {
                blockedByLimit++
                return@forEach
            }
            // A name already on the roster is never added twice: a CSV exported from one term and
            // imported into the next would otherwise silently double every student, and every
            // average with them.
            val typed = listOf(entry.firstName, entry.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val roster = book.classes.firstOrNull { it.id == classId }?.students.orEmpty()
            if (roster.any { it.displayName.equals(typed, ignoreCase = true) }) {
                alreadyOnRoster++
                return@forEach
            }
            when (
                val result = RecordBookEditor.addStudent(
                    book, classId, ids.next("student"), entry.firstName, entry.lastName, entry.externalId,
                )
            ) {
                is EditResult.Ok -> {
                    book = result.book
                    imported++
                }

                is EditResult.Rejected -> mutableEvents.tryEmit(RecordBookEvent.EditRejected(result.reason))
            }
        }

        if (imported > 0) commit(book)

        // The counts are reported and nothing is dropped in silence: a row that cannot be read is
        // carried, a duplicate is counted, and a row the free tier cannot hold is offered the unlock
        // rather than being quietly discarded. The sentence itself is built by the UI, which can read
        // string resources; here there are only values.
        if (imported == 0 && blockedByLimit > 0 && parsed.entries.isNotEmpty()) {
            mutableEvents.tryEmit(RecordBookEvent.Blocked(LimitReason.StudentCap(FreeTier.MAX_STUDENTS_PER_CLASS)))
        } else {
            mutableEvents.tryEmit(
                RecordBookEvent.ImportSummary(
                    imported = imported,
                    alreadyOnRoster = alreadyOnRoster,
                    skipped = parsed.skipped,
                    blockedByLimit = blockedByLimit,
                    studentLimit = FreeTier.MAX_STUDENTS_PER_CLASS,
                ),
            )
        }
    }

    // ------------------------------------------------------------ categories and assignments

    fun addCategory(classId: String, name: String, weight: Double, dropLowest: Int = 0) {
        val existing = classroom(classId)?.categories?.size ?: 0
        val check = FreeTier.canAddCategory(existing, entitlements.isUnlocked())
        if (blocked(check)) return
        apply(
            RecordBookEditor.addCategory(
                mutableState.value.book, classId, ids.next("category"), name, weight, dropLowest,
            ),
        )
    }

    fun updateCategory(classId: String, categoryId: String, name: String, weight: Double, dropLowest: Int) =
        apply(
            RecordBookEditor.updateCategory(
                mutableState.value.book, classId, categoryId, name, weight, dropLowest,
            ),
        )

    fun deleteCategory(classId: String, categoryId: String) =
        apply(RecordBookEditor.deleteCategory(mutableState.value.book, classId, categoryId))

    fun moveCategory(classId: String, categoryId: String, delta: Int) =
        apply(RecordBookEditor.moveCategory(mutableState.value.book, classId, categoryId, delta))

    fun addAssignment(classId: String, categoryId: String, name: String, points: Double) = apply(
        RecordBookEditor.addAssignment(
            mutableState.value.book, classId, ids.next("assignment"), categoryId, name, points,
        ),
    )

    fun updateAssignment(classId: String, assignmentId: String, name: String, points: Double, categoryId: String) =
        apply(
            RecordBookEditor.updateAssignment(
                mutableState.value.book, classId, assignmentId, name, points, categoryId,
            ),
        )

    fun deleteAssignment(classId: String, assignmentId: String) =
        apply(RecordBookEditor.deleteAssignment(mutableState.value.book, classId, assignmentId))

    // ------------------------------------------------------------ grades

    fun setGrade(classId: String, studentId: String, assignmentId: String, kind: GradeKind, points: Double = 0.0) =
        apply(
            RecordBookEditor.setGrade(
                mutableState.value.book, classId, studentId, assignmentId, kind, points,
            ),
        )

    fun clearGrade(classId: String, studentId: String, assignmentId: String) =
        apply(RecordBookEditor.clearGrade(mutableState.value.book, classId, studentId, assignmentId))

    // ------------------------------------------------------------ attendance

    fun setAttendance(classId: String, studentId: String, date: String, status: AttendanceStatus) =
        apply(RecordBookEditor.setAttendance(mutableState.value.book, classId, studentId, date, status))

    fun clearAttendance(classId: String, studentId: String, date: String) =
        apply(RecordBookEditor.clearAttendance(mutableState.value.book, classId, studentId, date))

    fun markAllPresent(classId: String, date: String) =
        apply(RecordBookEditor.markAllPresent(mutableState.value.book, classId, date))

    // ------------------------------------------------------------ notes and contacts

    fun addNote(classId: String, studentId: String, date: String, kind: NoteKind, text: String) = apply(
        RecordBookEditor.addNote(
            mutableState.value.book, classId, ids.next("note"), studentId, date, kind, text,
        ),
    )

    fun updateNote(classId: String, noteId: String, date: String, kind: NoteKind, text: String) =
        apply(RecordBookEditor.updateNote(mutableState.value.book, classId, noteId, date, kind, text))

    fun deleteNote(classId: String, noteId: String) =
        apply(RecordBookEditor.deleteNote(mutableState.value.book, classId, noteId))

    fun addContact(
        classId: String,
        studentId: String,
        date: String,
        channel: String,
        summary: String,
        followUpOn: String? = null,
    ) = apply(
        RecordBookEditor.addContact(
            mutableState.value.book, classId, ids.next("contact"), studentId, date, channel, summary, followUpOn,
        ),
    )

    fun updateContact(
        classId: String,
        contactId: String,
        date: String,
        channel: String,
        summary: String,
        followUpOn: String?,
    ) = apply(
        RecordBookEditor.updateContact(
            mutableState.value.book, classId, contactId, date, channel, summary, followUpOn,
        ),
    )

    fun deleteContact(classId: String, contactId: String) =
        apply(RecordBookEditor.deleteContact(mutableState.value.book, classId, contactId))

    // ------------------------------------------------------------ seating

    fun resizeSeating(classId: String, rows: Int, columns: Int) =
        apply(RecordBookEditor.resizeSeating(mutableState.value.book, classId, rows, columns))

    fun setSeat(classId: String, desk: Int, studentId: String?) =
        apply(RecordBookEditor.setSeat(mutableState.value.book, classId, desk, studentId))

    fun moveToNextFreeDesk(classId: String, studentId: String) =
        apply(RecordBookEditor.moveStudentToNextFreeDesk(mutableState.value.book, classId, studentId))

    // ------------------------------------------------------------ export and backup

    /** Reports whether the teacher may export, and offers the unlock when they may not. */
    fun requestExport(): Boolean = !blocked(FreeTier.canExport(entitlements.isUnlocked()))

    fun requestBackup(): Boolean = !blocked(FreeTier.canBackUp(entitlements.isUnlocked()))

    /** Replaces the whole record from a document the teacher picked, or changes nothing. */
    fun restoreBackup(text: String) {
        when (val result = BackupCodec.decode(text)) {
            is BackupCodec.DecodeResult.Ok -> {
                if (commit(result.book)) {
                    mutableEvents.tryEmit(RecordBookEvent.Restored(result.book.classes.size))
                }
            }

            is BackupCodec.DecodeResult.Rejected ->
                mutableEvents.tryEmit(RecordBookEvent.BackupRejected(result.reason))
        }
    }

    /** Posts an event for the shell to resolve and show. */
    fun announce(event: RecordBookEvent) = mutableEvents.tryEmit(event)

    /** Posts text a screen already resolved from its own resources. */
    fun announce(text: String) = announce(RecordBookEvent.Text(text))

    // ------------------------------------------------------------ internals

    private fun apply(result: EditResult) {
        when (result) {
            is EditResult.Ok -> commit(result.book)
            is EditResult.Rejected -> mutableEvents.tryEmit(RecordBookEvent.EditRejected(result.reason))
        }
    }

    /**
     * Updates the visible state first and persists in the background. Returns whether it applied.
     *
     * The alternative — awaiting the write before showing the edit — makes every keystroke wait on
     * a disk, and the visible state is what the teacher is looking at.
     */
    private fun commit(book: RecordBook): Boolean {
        if (!readyToEdit) return false
        mutableState.update { it.copy(book = book) }
        viewModelScope.launch {
            val saved = withContext(io) { repository.save(book) }
            if (!saved) {
                // The record is updated in memory but not on disk, so the first possible moment is
                // reported rather than leaving the teacher to discover it after a restart.
                mutableEvents.tryEmit(RecordBookEvent.StorageFailed)
            }
        }
        return true
    }

    /** Reports a refusal; the reason travels as a value so the shell can say it in the right language. */
    private fun blocked(check: LimitCheck): Boolean = when (check) {
        is LimitCheck.Allowed -> false
        is LimitCheck.Blocked -> {
            mutableEvents.tryEmit(RecordBookEvent.Blocked(check.reason))
            true
        }
    }

    companion object {
        /** Deterministic-in-tests identifiers: a counter plus the clock, never a random UUID. */
        fun defaultIdSource(): IdSource {
            var counter = 0
            return IdSource { prefix -> "$prefix-${System.currentTimeMillis()}-${counter++}" }
        }

        fun factory(
            repository: RecordBookRepository,
            entitlements: EntitlementGate,
            ids: IdSource = defaultIdSource(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RecordBookViewModel(repository, entitlements, ids) as T
        }
    }
}

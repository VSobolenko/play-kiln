package com.gexetr.teachergradebook.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.data.ReportLabels
import com.gexetr.teachergradebook.data.ReportPdf
import com.gexetr.teachergradebook.domain.LimitReason
import com.gexetr.teachergradebook.monetization.PlayKilnContainer
import com.gexetr.teachergradebook.monetization.UnlockMessage
import com.gexetr.teachergradebook.ui.about.AboutScreen
import com.gexetr.teachergradebook.ui.classes.ClassesScreen
import com.gexetr.teachergradebook.ui.classroom.ClassTab
import com.gexetr.teachergradebook.ui.classroom.ClassroomScreen
import com.gexetr.teachergradebook.ui.components.PlayKilnSurface
import com.gexetr.teachergradebook.ui.gradesheet.GradeSheetScreen
import com.gexetr.teachergradebook.ui.paywall.UnlockDialog
import com.gexetr.teachergradebook.ui.recordbook.RecordBookEvent
import com.gexetr.teachergradebook.ui.recordbook.RecordBookViewModel
import com.gexetr.teachergradebook.ui.student.StudentScreen
import kotlinx.coroutines.launch

/**
 * Where the teacher is.
 *
 * Destinations are held as encoded strings in a saved-state list, because Android will destroy and
 * rebuild this app mid-task more often than anyone expects — a rotation, a low-memory kill while
 * the file picker is open, a split-screen resize — and a back stack that does not survive that
 * drops the teacher out of the class they were grading.
 */
private sealed interface Destination {
    data object Classes : Destination

    data class Classroom(val classId: String, val tab: ClassTab) : Destination

    data class Student(val classId: String, val studentId: String) : Destination

    data class GradeSheet(val classId: String, val assignmentId: String) : Destination

    data object About : Destination
}

private fun Destination.encode(): String = when (this) {
    Destination.Classes -> "classes"
    is Destination.Classroom -> "class|$classId|${tab.name}"
    is Destination.Student -> "student|$classId|$studentId"
    is Destination.GradeSheet -> "sheet|$classId|$assignmentId"
    Destination.About -> "about"
}

private fun decode(text: String): Destination {
    val parts = text.split('|')
    return when (parts.firstOrNull()) {
        "class" -> Destination.Classroom(
            classId = parts.getOrElse(1) { "" },
            tab = ClassTab.entries.firstOrNull { it.name == parts.getOrNull(2) } ?: ClassTab.ROSTER,
        )

        "student" -> Destination.Student(parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" })
        "sheet" -> Destination.GradeSheet(parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" })
        "about" -> Destination.About
        else -> Destination.Classes
    }
}

private val BackStackSaver = listSaver<SnapshotStateList<Destination>, String>(
    save = { backStack -> backStack.map(Destination::encode) },
    restore = { saved ->
        mutableStateListOf<Destination>().apply {
            saved.mapTo(this, ::decode)
            if (isEmpty()) add(Destination.Classes)
        }
    },
)

/** What a file the teacher is about to create should contain. */
private class PendingText(val content: String, val onWritten: (String) -> Unit)

private class PendingBytes(val bytes: ByteArray, val onWritten: (String) -> Unit)

@Composable
fun PlayKilnApp(container: PlayKilnContainer) {
    val scope = rememberCoroutineScope()
    // The shell's own messages are raised from callbacks - a document write that finishes after the
    // screen that started it is gone - so they are resolved from the Context rather than through
    // `stringResource`, which a coroutine cannot read.
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val recordBook: RecordBookViewModel = viewModel(
        factory = RecordBookViewModel.factory(
            repository = container.recordBookRepository,
            // The unlock question is answered from the one place that knows the answer: the billing
            // entitlement. A copy kept in the ViewModel could drift, and a drifted copy either gives
            // away a paid feature or refuses one the teacher already bought.
            entitlements = { container.entitlements.state.value.unlocked },
        ),
    )
    val state by recordBook.state.collectAsStateWithLifecycle()
    val unlock by container.entitlements.state.collectAsStateWithLifecycle()
    val documentStore = container.documentStore

    var pendingText by remember { mutableStateOf<PendingText?>(null) }
    var pendingBytes by remember { mutableStateOf<PendingBytes?>(null) }
    var pendingRead by remember { mutableStateOf<((String) -> Unit)?>(null) }

    fun say(text: String) = scope.launch { snackbarHostState.showSnackbar(text) }

    /** Every created file is written here, so no screen has to know about content resolvers. */
    fun finishTextWrite(uri: android.net.Uri?) {
        val request = pendingText
        pendingText = null
        if (uri == null || request == null) {
            say(context.getString(R.string.app_nothing_written))
            return
        }
        documentStore.writeText(uri.toString(), request.content)
            .onSuccess {
                request.onWritten(documentStore.displayName(uri.toString()) ?: context.getString(R.string.document_chosen_file))
            }
            .onFailure { say(context.getString(R.string.app_write_failed, it.describe(context))) }
    }

    fun finishBytesWrite(uri: android.net.Uri?) {
        val request = pendingBytes
        pendingBytes = null
        if (uri == null || request == null) {
            say(context.getString(R.string.app_nothing_written))
            return
        }
        documentStore.writeBytes(uri.toString(), request.bytes)
            .onSuccess {
                request.onWritten(documentStore.displayName(uri.toString()) ?: context.getString(R.string.document_chosen_file))
            }
            .onFailure { say(context.getString(R.string.app_write_failed, it.describe(context))) }
    }

    // Three launchers rather than one, because the MIME type is fixed when a contract is
    // registered: a PDF offered as text/csv lands on the device with the wrong type and no app
    // wants to open it.
    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> finishTextWrite(uri) }
    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> finishTextWrite(uri) }
    val createPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> finishBytesWrite(uri) }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val handler = pendingRead
        pendingRead = null
        if (uri == null) {
            say(context.getString(R.string.app_no_file_chosen))
            return@rememberLauncherForActivityResult
        }
        documentStore.readText(uri.toString())
            .onSuccess { content -> handler?.invoke(content) }
            .onFailure { say(context.getString(R.string.app_read_failed, it.describe(context))) }
    }

    // One collection point for the record's events: a message becomes a snackbar, and a refusal
    // that money would lift becomes the paywall instead of an error the teacher cannot act on. The
    // wording is resolved here, from resources, because the ViewModel is not allowed to hold it.
    LaunchedEffect(recordBook) {
        recordBook.load()
        recordBook.events.collect { event ->
            when (event) {
                is RecordBookEvent.Blocked ->
                    container.entitlements.openPaywall(limitReasonText(context, event.reason))

                is RecordBookEvent.ImportSummary ->
                    snackbarHostState.showSnackbar(importSummaryText(context, event))

                is RecordBookEvent.Restored -> snackbarHostState.showSnackbar(
                    context.resources.getQuantityString(R.plurals.restore_restored, event.classes, event.classes),
                )

                RecordBookEvent.StorageFailed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.storage_write_failed))

                is RecordBookEvent.EditRejected ->
                    snackbarHostState.showSnackbar(editRejectionText(context, event.reason))

                is RecordBookEvent.BackupRejected ->
                    snackbarHostState.showSnackbar(backupRejectionText(context, event.reason))

                is RecordBookEvent.Text -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LaunchedEffect(unlock.message) {
        val message = unlock.message ?: return@LaunchedEffect
        container.entitlements.consumeMessage()
        snackbarHostState.showSnackbar(unlockMessageText(context, message))
    }

    val backStack = rememberSaveable(saver = BackStackSaver) {
        mutableStateListOf<Destination>(Destination.Classes)
    }

    fun push(destination: Destination) {
        if (backStack.lastOrNull() != destination) backStack.add(destination)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun exportCsv(classId: String) {
        val classroom = recordBook.classroom(classId)
        val csv = classroom?.let { recordBook.csvFor(classId, csvLabels(context, it.rounding)) }
        when {
            classroom == null -> say(context.getString(R.string.app_class_gone))
            csv == null -> say(context.getString(R.string.app_record_loading))
            !recordBook.requestExport() -> Unit
            else -> {
                pendingText = PendingText(csv) { written ->
                    say(context.getString(R.string.app_wrote_csv, written))
                }
                createCsv.launch("${fileSafeName(classroom.name, context)}-grades.csv")
            }
        }
    }

    fun exportPdf(classId: String, studentId: String?) {
        val classroom = recordBook.classroom(classId)
        if (classroom == null) {
            say(context.getString(R.string.app_class_gone))
            return
        }
        if (!recordBook.requestExport()) return
        // The renderer cannot read resources, so the words are resolved here, once per export.
        val labels = ReportLabels.from(context, classroom.rounding)
        val bytes = if (studentId == null) {
            ReportPdf.classReport(classroom, labels)
        } else {
            ReportPdf.studentReport(classroom, studentId, labels)
        }
        pendingBytes = PendingBytes(bytes) { written ->
            say(context.getString(R.string.app_wrote_file, written))
        }
        val suffix = if (studentId == null) "report" else "student-report"
        createPdf.launch("${fileSafeName(classroom.name, context)}-$suffix.pdf")
    }

    PlayKilnSurface(
        snackbarHost = {
            SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding())
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { pop() },
            entryProvider = { key ->
                when (key) {
                    Destination.Classes -> NavEntry(key) {
                        ClassesScreen(
                            state = state,
                            unlocked = unlock.unlocked,
                            onOpenClass = { classId -> push(Destination.Classroom(classId, ClassTab.ROSTER)) },
                            onOpenAbout = { push(Destination.About) },
                            onShowUnlock = {
                                container.entitlements.openPaywall(
                                    context.getString(R.string.app_unlock_reason_classes),
                                )
                            },
                            onAddClass = recordBook::addClass,
                            onRenameClass = recordBook::renameClass,
                            onDeleteClass = recordBook::deleteClass,
                        )
                    }

                    is Destination.Classroom -> NavEntry(key) {
                        ClassroomScreen(
                            classId = key.classId,
                            initialTab = key.tab,
                            state = state,
                            unlocked = unlock.unlocked,
                            onBack = { pop() },
                            onOpenStudent = { studentId -> push(Destination.Student(key.classId, studentId)) },
                            onOpenGradeSheet = { assignmentId ->
                                push(Destination.GradeSheet(key.classId, assignmentId))
                            },
                            recordBook = recordBook,
                            onExportCsv = { exportCsv(key.classId) },
                            onExportPdf = { exportPdf(key.classId, studentId = null) },
                            onImportRoster = {
                                pendingRead = { text -> recordBook.importRoster(key.classId, text) }
                                openDocument.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values"))
                            },
                            onBackup = {
                                if (recordBook.requestBackup()) {
                                    pendingText = PendingText(recordBook.backupDocument()) { written ->
                                        say(context.getString(R.string.app_backed_up, written))
                                    }
                                    createJson.launch("markbook-backup.json")
                                }
                            },
                            onRestore = {
                                if (recordBook.requestBackup()) {
                                    pendingRead = recordBook::restoreBackup
                                    openDocument.launch(arrayOf("application/json", "text/*"))
                                }
                            },
                        )
                    }

                    is Destination.Student -> NavEntry(key) {
                        StudentScreen(
                            classId = key.classId,
                            studentId = key.studentId,
                            state = state,
                            onBack = { pop() },
                            recordBook = recordBook,
                            onExportPdf = { exportPdf(key.classId, key.studentId) },
                        )
                    }

                    is Destination.GradeSheet -> NavEntry(key) {
                        GradeSheetScreen(
                            classId = key.classId,
                            assignmentId = key.assignmentId,
                            state = state,
                            onBack = { pop() },
                            recordBook = recordBook,
                        )
                    }

                    Destination.About -> NavEntry(key) {
                        AboutScreen(
                            onBack = { pop() },
                            onShowUnlock = {
                                container.entitlements.openPaywall(
                                    context.getString(R.string.app_unlock_reason_about),
                                )
                            },
                            unlocked = unlock.unlocked,
                        )
                    }
                }
            },
        )
    }

    if (unlock.paywallVisible) {
        UnlockDialog(
            reason = unlock.paywallReason,
            purchasing = unlock.purchasing,
            billingReady = unlock.billingReady,
            onPurchase = { scope.launch { container.entitlements.purchase() } },
            onRestore = { scope.launch { container.entitlements.restore() } },
            onDismiss = container.entitlements::dismissPaywall,
        )
    }
}

/**
 * Spelled out for each outcome, including the ones where nothing happened.
 *
 * Resolved from the Context, not from the composition, because this is called from an effect and a
 * purchase result must not depend on a composition local still existing when Play answers - which can
 * be minutes later, or after the teacher has left the app and come back.
 */
private fun unlockMessageText(context: Context, message: UnlockMessage): String = when (message) {
    UnlockMessage.PURCHASED -> context.getString(R.string.app_unlock_purchased)
    UnlockMessage.RESTORED -> context.getString(R.string.app_unlock_restored)
    UnlockMessage.NOTHING_TO_RESTORE -> context.getString(R.string.app_unlock_nothing_to_restore)
    UnlockMessage.PENDING -> context.getString(R.string.app_unlock_pending)
    UnlockMessage.UNAVAILABLE -> context.getString(R.string.app_unlock_unavailable)
    UnlockMessage.NOT_COMPLETED -> context.getString(R.string.app_unlock_not_completed)
    UnlockMessage.OFFLINE -> context.getString(R.string.app_unlock_offline)
}

/**
 * The refusal a limit stands for, in the teacher's language.
 *
 * Resolved from the Context for the same reason as [unlockMessageText]: the event arrives in a
 * collector that can outlive the screen, so the sentence cannot come from a composition local. The
 * student cap is a plural because the count is carried as a value and not baked into prose.
 */
private fun limitReasonText(context: Context, reason: LimitReason): String = when (reason) {
    LimitReason.SecondClass -> context.getString(R.string.limit_second_class)
    is LimitReason.StudentCap -> context.resources.getQuantityString(
        R.plurals.limit_student_cap,
        reason.limit,
        reason.limit,
    )

    LimitReason.SingleCategory -> context.getString(R.string.limit_single_category)
    LimitReason.Export -> context.getString(R.string.limit_export)
    LimitReason.Backup -> context.getString(R.string.limit_backup)
}

/**
 * The roster import as one paragraph.
 *
 * Each sentence is its own resource with its own count, and they are joined as whole sentences rather
 * than as translated fragments: a translator can reorder a sentence but never a word that arrived
 * glued to a number. Nothing is reported twice and nothing is dropped in silence.
 */
private fun importSummaryText(context: Context, summary: RecordBookEvent.ImportSummary): String {
    val resources = context.resources
    val parts = buildList {
        add(resources.getQuantityString(R.plurals.import_imported, summary.imported, summary.imported))
        if (summary.alreadyOnRoster > 0) {
            add(
                resources.getQuantityString(
                    R.plurals.import_already_on_roster,
                    summary.alreadyOnRoster,
                    summary.alreadyOnRoster,
                ),
            )
        }
        if (summary.skipped.isNotEmpty()) {
            val detail = summary.skipped.joinToString("; ") { row ->
                context.getString(R.string.import_skipped_line, row.line, rosterSkipReasonText(context, row.reason))
            }
            add(
                resources.getQuantityString(
                    R.plurals.import_skipped,
                    summary.skipped.size,
                    summary.skipped.size,
                    detail,
                ),
            )
        }
        if (summary.blockedByLimit > 0) {
            add(
                resources.getQuantityString(
                    R.plurals.import_blocked_by_limit,
                    summary.blockedByLimit,
                    summary.blockedByLimit,
                    summary.studentLimit,
                ),
            )
        }
    }
    return parts.joinToString(" ")
}

/**
 * A platform exception's own message, or a translatable stand-in when there is none.
 *
 * `message` can be null and can be an untranslated platform string; either way the sentence around it
 * comes from resources, so the teacher never sees a half-English error. It is never null itself, which
 * is what `"unknown reason"` was there to guarantee.
 */
private fun Throwable.describe(context: Context): String =
    documentFailureText(context, this)
        ?: message?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.app_error_unknown_reason)

/**
 * A file name a person can recognise later, without characters a file system dislikes.
 *
 * The fallback is a resource rather than the literal `"class"`: a Russian teacher who has not named a
 * class yet gets a file called `Класс-grades.csv`, which is legible to them and still safe on every
 * file system. Letters outside A-Z are deliberately kept - the filter removes punctuation and path
 * separators, and `\p{L}` is not a path separator.
 */
private fun fileSafeName(name: String, context: Context): String =
    name.trim()
        .replace(Regex("[^\\p{L}\\p{N} _-]"), "")
        .replace(' ', '-')
        .ifBlank { context.getString(R.string.app_unnamed_class_file) }

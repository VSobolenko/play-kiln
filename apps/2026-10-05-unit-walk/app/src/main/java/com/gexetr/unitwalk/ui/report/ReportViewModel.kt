package com.gexetr.unitwalk.ui.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gexetr.capability.billing.BillingController
import com.gexetr.unitwalk.data.WalkStore
import com.gexetr.unitwalk.domain.Signature
import com.gexetr.unitwalk.domain.WalkState
import com.gexetr.unitwalk.domain.addSignature
import com.gexetr.unitwalk.domain.buildReportModel
import com.gexetr.unitwalk.pdf.renderReportPdf
import com.gexetr.unitwalk.pdf.reportWatermark
import com.gexetr.unitwalk.ui.unlock.UNIT_WALK_CATALOG
import com.gexetr.unitwalk.ui.unlock.UnlockState
import com.gexetr.unitwalk.ui.unlock.isOwned
import com.gexetr.unitwalk.ui.unlock.unlockState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PDF_MIME_TYPE = "application/pdf"
private const val REPORTS_DIR = "reports"
private const val REPORT_STEM = "unit-walk-report"

/**
 * The finished report and its handoff out of the app — the brain behind the `signed-pdf-share`
 * promise, and the `data_out` proof artifact for it.
 *
 * On every change it rebuilds the framework-free [com.gexetr.unitwalk.domain.ReportModel] with
 * `buildReportModel`, renders it with `renderReportPdf`, writes the bytes under the app's private
 * `filesDir/reports/` (the subtree `res/xml/file_paths.xml` exposes), and turns that file into a
 * `content://` URI through `FileProvider` using the authority the manifest declares as
 * `${applicationId}.fileprovider`. Nothing is uploaded: the bytes leave the device only when the
 * user shares or saves them.
 *
 * Two separate handoffs are exposed as real intents — [shareIntent] (`Intent.ACTION_SEND` with
 * `FLAG_GRANT_READ_URI_PERMISSION`) and [saveIntent] (`Intent.ACTION_CREATE_DOCUMENT`) — and
 * [writeReportTo] copies the report to the destination the user picked. The screen owns the
 * `startActivity` / launcher calls; this class owns the intents and the files.
 *
 * Deletion is two-step: [ReportEvent.DeleteWalkRequested] / [ReportEvent.DeleteUnitRequested] only
 * set [ReportUiState.pendingDeletion] with the item's name, and nothing is removed until
 * [ReportEvent.DeletionConfirmed].
 *
 * ## The watermark, and why export is never gated
 *
 * Share and Save exist for every user; ownership changes only *what the bytes say*. When the unlock is
 * confirmed owned the report is clean, and otherwise — including while the entitlement is still
 * unresolved — the report is stamped with [com.gexetr.unitwalk.pdf.ReportWatermark]. The decision is
 * `reportWatermark` (a pure function with its own JVM test) and the entitlement is read through
 * `UnlockState.isOwned` in `ui/unlock/UnlockProducts.kt`, so the product key and the paid/not-paid
 * boundary live in one place. A later `restore()` that finds the purchase re-renders the PDF, because
 * the mark is part of the file rather than of the drawing.
 *
 * ## How a render is published
 *
 * Every render goes through one conflated queue drained by a single coroutine ([renderRequests]), so
 * two renders of the same report cannot run at once: a request superseded while it is still pending
 * is dropped, and an in-flight one finishes before the newest starts. Each request carries the
 * entitlement it was stamped from, so the bytes written and the [ReportUiState] describing them are
 * the same decision. The bytes are published atomically — a sibling `.tmp` file in `reports/`,
 * `fsync`, then a rename over the destination — so a reader never sees a half-written PDF and a
 * process death mid-write leaves the previous report in place.
 *
 * The dispatcher is a constructor parameter so the PDF render and file writes stay off the main
 * thread and the class stays driveable in a test.
 */
class ReportViewModel(
    context: Context,
    private val store: WalkStore,
    private val walkId: String,
    private val billing: BillingController? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val appContext: Context = context.applicationContext
    private val reportsDir: File = File(appContext.filesDir, REPORTS_DIR)
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    /**
     * The one queue every render passes through.
     *
     * [Channel.CONFLATED] keeps at most one pending request, so a burst of entitlement changes
     * collapses to the newest one, and the single consumer below is the only thing that ever touches
     * the report file. That is what makes two renders of the same report impossible to interleave.
     */
    private val renderRequests: Channel<RenderRequest> = Channel(Channel.CONFLATED)

    private val _uiState = MutableStateFlow(
        ReportUiState(walkId = walkId, suggestedFileName = suggestedFileNameFor("")),
    )
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        // The single render consumer. It outlives every individual request: a render already in
        // flight always completes before the next one starts, and the newest request is the last to
        // write, so the file and [ReportUiState] settle on the same entitlement.
        scope.launch {
            for (request in renderRequests) {
                try {
                    val state = request.state ?: store.load()
                    writeReport(state, request.unlock, request.message)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // One bad render must not stop the consumer for the rest of the ViewModel's life.
                    _uiState.value = failedState()
                }
            }
        }
        watchUnlock()
        refresh()
    }

    /**
     * Mirrors the entitlement into the state and re-renders when it changes.
     *
     * The connect is launched rather than awaited, so the first render does not wait on Play: while
     * the entitlement is unresolved the report is written **with** the watermark, which is the safe
     * direction, and a `restore()` that later finds the purchase queues a fresh render whose bytes
     * carry no stamp. Because renders are serialised on [renderRequests], the newest request is the
     * one that lands — the clean bytes can never be overwritten by the earlier stamped render.
     * Nothing here gates Share or Save; the mark is the only difference.
     */
    private fun watchUnlock() {
        val controller = billing ?: return
        applyUnlock(controller.entitlements.value.unlockState())
        scope.launch {
            controller.connect(UNIT_WALK_CATALOG)
            applyUnlock(controller.entitlements.value.unlockState())
        }
        scope.launch {
            controller.entitlements.collect { entitlements -> applyUnlock(entitlements.unlockState()) }
        }
    }

    /** Publishes [unlock]; a real change queues a re-render, because the mark is in the written bytes. */
    private fun applyUnlock(unlock: UnlockState) {
        if (_uiState.value.unlock == unlock) return
        _uiState.update { it.copy(unlock = unlock) }
        refresh()
    }

    fun onEvent(event: ReportEvent) {
        when (event) {
            is ReportEvent.SignatureEntered -> recordSignature(event.name, event.signedAtIso)
            is ReportEvent.DeleteWalkRequested -> _uiState.update {
                it.copy(
                    pendingDeletion = PendingDeletion.WalkDeletion(
                        walkId = event.walkId,
                        displayName = event.walkName,
                    ),
                )
            }
            is ReportEvent.DeleteUnitRequested -> _uiState.update {
                it.copy(
                    pendingDeletion = PendingDeletion.UnitDeletion(
                        unitId = event.unitId,
                        displayName = event.unitName,
                    ),
                )
            }
            ReportEvent.DeletionConfirmed -> confirmDeletion()
            ReportEvent.DeletionCancelled -> _uiState.update { it.copy(pendingDeletion = null) }
            ReportEvent.MessageDismissed -> _uiState.update { it.copy(message = null) }
        }
    }

    /**
     * Queues a re-render of the PDF from the current document. Safe to call on resume.
     *
     * Queued rather than launched: a render already in flight finishes first, and the newest request
     * is the one that writes, so two renders can no longer interleave on the one report file.
     */
    fun refresh() {
        enqueueRender()
    }

    /**
     * Puts one render request on the conflated queue.
     *
     * [unlock] is captured **here**, at request time, and travels with the request into the write: the
     * render and the [ReportUiState] it publishes use this one value, so a render cannot stamp bytes
     * from one entitlement while the state reports another.
     */
    private fun enqueueRender(
        state: WalkState? = null,
        message: ReportMessage = ReportMessage.PdfWritten,
        unlock: UnlockState = _uiState.value.unlock,
    ) {
        renderRequests.trySend(RenderRequest(state = state, unlock = unlock, message = message))
    }

    /**
     * The share handoff, ready for the screen to wrap in a chooser.
     *
     * `null` until a report exists; the screen must not offer Share before then. The URI is the
     * app's own `content://` URI, and the read grant is confined to the receiving app by the flag.
     */
    fun shareIntent(): Intent? {
        val uri = _uiState.value.pdfUri ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = PDF_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * The save handoff, for a screen that prefers an explicit system document picker.
     *
     * `null` until a report exists. The suggested name is what the picker pre-fills; the bytes are
     * written only when the user picks a destination and the screen calls [writeReportTo].
     */
    fun saveIntent(): Intent? {
        val name = _uiState.value.suggestedFileName ?: return null
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = PDF_MIME_TYPE
            putExtra(Intent.EXTRA_TITLE, name)
        }
    }

    /** The rendered bytes, for a screen that writes them through its own launcher. */
    fun reportBytes(): ByteArray? {
        val name = _uiState.value.pdfFileName ?: return null
        val file = File(reportsDir, name)
        return runCatching { if (file.isFile) file.readBytes() else null }.getOrNull()
    }

    /** Copies the rendered report to a destination the user chose. Reports what happened. */
    fun writeReportTo(target: Uri) {
        scope.launch {
            val name = _uiState.value.pdfFileName
            if (name == null) {
                _uiState.update { it.copy(message = ReportMessage.NothingToSave) }
                return@launch
            }
            val source = File(reportsDir, name)
            val copied = runCatching {
                appContext.contentResolver.openOutputStream(target)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: error("the chosen destination returned no stream")
            }.isSuccess
            _uiState.update {
                it.copy(
                    message = if (copied) ReportMessage.SavedToChosenLocation else ReportMessage.SaveFailed,
                )
            }
        }
    }

    private fun recordSignature(name: String, signedAtIso: String) {
        scope.launch {
            val state = store.load()
            val walk = state.walks.firstOrNull { it.id == walkId } ?: return@launch
            val updated = addSignature(walk, Signature(name = name, signedAtIso = signedAtIso))
            val next = state.copy(
                walks = state.walks.map { if (it.id == walkId) updated else it },
            )
            if (runCatching { store.save(next) }.isFailure) {
                _uiState.update { it.copy(message = ReportMessage.SaveFailed) }
                return@launch
            }
            enqueueRender(state = next, message = ReportMessage.SignatureSaved)
        }
    }

    private fun confirmDeletion() {
        val pending = _uiState.value.pendingDeletion ?: return
        _uiState.update { it.copy(pendingDeletion = null) }
        scope.launch {
            val state = store.load()
            val next = when (pending) {
                is PendingDeletion.WalkDeletion -> state.copy(
                    walks = state.walks.filterNot { it.id == pending.walkId },
                )
                is PendingDeletion.UnitDeletion -> state.copy(
                    units = state.units.filterNot { it.id == pending.unitId },
                    walks = state.walks.filterNot { it.unitId == pending.unitId },
                )
            }
            if (runCatching { store.save(next) }.isFailure) {
                _uiState.update { it.copy(message = ReportMessage.SaveFailed) }
                return@launch
            }
            enqueueRender(state = next)
        }
    }

    private fun writeReport(state: WalkState, unlock: UnlockState, message: ReportMessage) {
        if (state.walks.none { it.id == walkId }) {
            _uiState.value = _uiState.value.copy(
                status = ReportStatus.Empty,
                pdfUri = null,
                pdfFileName = null,
                pdfByteCount = 0,
                message = null,
            )
            return
        }

        val model = buildReportModel(state, walkId)
        // The stamp is decided **once**, from the entitlement this render was requested with, and the
        // bytes are rendered from it. Nothing reads the entitlement again while writing, so the file's
        // bytes and the state published below are the same decision by construction.
        val bytes = runCatching { renderReportPdf(model, reportWatermark(unlock.isOwned)) }.getOrNull()
        if (bytes == null || bytes.isEmpty()) {
            _uiState.value = failedState()
            return
        }

        val fileName = reportFileNameFor(walkId)
        val file = File(reportsDir, fileName)
        val written = runCatching { writeAtomically(file, bytes) }.isSuccess
        if (!written) {
            _uiState.value = failedState()
            return
        }

        val uri = runCatching {
            FileProvider.getUriForFile(appContext, appContext.packageName + ".fileprovider", file)
        }.getOrNull()

        _uiState.value = ReportUiState(
            status = if (uri == null) ReportStatus.Failed else ReportStatus.Ready,
            walkId = walkId,
            unitName = model.unitName,
            roomCount = model.rooms.size,
            photoCount = model.photoCount,
            deltaCount = model.deltas.size,
            signatureCount = model.signatures.size,
            pdfUri = uri,
            pdfFileName = if (uri == null) null else fileName,
            pdfByteCount = bytes.size.toLong(),
            suggestedFileName = suggestedFileNameFor(model.unitName),
            message = if (uri == null) ReportMessage.PdfWriteFailed else message,
            // The entitlement recorded here is the one the bytes above were stamped from — carried
            // through from the request, never re-read from whatever the state holds by now.
            unlock = unlock,
        )
    }

    /**
     * Publishes [bytes] at [target] in one step a reader can never observe half-finished.
     *
     * The bytes go to a sibling `"<name>.tmp"` file **in the same directory** — never the system temp
     * directory, and never outside the `reports/` subtree the `FileProvider` exposes — which is
     * flushed and `fsync`ed and then renamed over the destination, the same shape
     * [com.gexetr.unitwalk.data.WalkStore.saveBlocking] uses for the draft document. A reader
     * therefore sees either the previous report or the new one, and a process death mid-write leaves
     * the previous report intact rather than a truncated file.
     */
    private fun writeAtomically(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "${target.name}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.flush()
            out.fd.sync()
        }
        if (tmp.renameTo(target)) return
        // Windows `renameTo` will not replace an existing file; the delete-then-rename window is the
        // one non-atomic step, and [WalkStore] documents the same limitation for its own document.
        if (target.exists() && !target.delete()) {
            tmp.delete()
            throw IOException("could not replace ${target.name}")
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    private fun failedState(): ReportUiState = _uiState.value.copy(
        status = ReportStatus.Failed,
        pdfUri = null,
        pdfFileName = null,
        pdfByteCount = 0,
        message = ReportMessage.PdfWriteFailed,
    )

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    companion object {
        /**
         * Builds the ViewModel over the app's own draft store, the same directory
         * [CompareViewModel] reads. It renders on first read, so the screen receives a report or an
         * explicit empty/failed status rather than a request it has to remember to make. [billing] is
         * the shell's one entitlement source, or `null` when nothing needs the watermark decided.
         */
        fun factory(
            context: Context,
            walkId: String,
            billing: BillingController? = null,
        ): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReportViewModel(
                        context = applicationContext,
                        store = WalkStore(applicationContext.filesDir),
                        walkId = walkId,
                        billing = billing,
                    ) as T
            }
        }
    }
}

/** A filesystem-safe report name for the walk; the `reports/` subtree is what the provider exposes. */
private fun reportFileNameFor(walkId: String): String =
    "report-" + sanitize(walkId).ifBlank { REPORT_STEM } + ".pdf"

/**
 * One render travelling through [ReportViewModel]'s conflated queue.
 *
 * [unlock] is captured when the request is enqueued and passed all the way into the write, so the
 * stamp on the bytes and the ownership the published state reports cannot disagree.
 */
private data class RenderRequest(
    /** The document to render, or `null` to load the current one in the consumer. */
    val state: WalkState?,
    /** The entitlement this render is stamped from. */
    val unlock: UnlockState,
    val message: ReportMessage,
)

/** A filesystem-safe suggested name for the system save picker, derived from the unit's own name. */
private fun suggestedFileNameFor(unitName: String): String =
    sanitize(unitName).ifBlank { REPORT_STEM } + ".pdf"

/** Keeps letters, digits, dash and underscore; everything else becomes an underscore. */
private fun sanitize(raw: String): String =
    raw.trim()
        .map { character ->
            if (character.isLetterOrDigit() || character == '-' || character == '_') {
                character
            } else {
                '_'
            }
        }
        .joinToString("")
        .trim('_')
        .take(48)

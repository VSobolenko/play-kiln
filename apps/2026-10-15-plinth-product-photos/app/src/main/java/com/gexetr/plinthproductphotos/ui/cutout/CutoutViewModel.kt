package com.gexetr.plinthproductphotos.ui.cutout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gexetr.plinthproductphotos.BuildConfig
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.api.ApiClient
import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiJobError
import com.gexetr.plinthproductphotos.data.api.ApiJobErrorCode
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.DeliveryReceipt
import com.gexetr.plinthproductphotos.data.api.DeliveredResult
import com.gexetr.plinthproductphotos.data.api.DeliveryReceiptWorker
import com.gexetr.plinthproductphotos.data.api.JobResult
import com.gexetr.plinthproductphotos.data.api.JobState
import com.gexetr.plinthproductphotos.data.api.JobStatus
import com.gexetr.plinthproductphotos.data.api.WalletResponse
import com.gexetr.plinthproductphotos.data.photoimport.PhotoImporter
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels.toBitmap
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImageLoader
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.data.session.PendingCutoutStore
import com.gexetr.plinthproductphotos.data.session.PendingCutout
import com.gexetr.plinthproductphotos.data.session.PendingDeliveryReceipt
import com.gexetr.plinthproductphotos.data.session.WalletSessionStore
import com.gexetr.plinthproductphotos.data.session.IntegrityTokenProvider
import com.gexetr.plinthproductphotos.data.session.SessionBootstrap
import com.gexetr.plinthproductphotos.data.session.SessionConnectionState
import com.gexetr.plinthproductphotos.domain.cutout.EmptyEraseMaskException
import com.gexetr.plinthproductphotos.domain.cutout.EraseMask
import com.gexetr.plinthproductphotos.domain.cutout.EraseVerificationException
import com.gexetr.plinthproductphotos.domain.cutout.EraseVerifier
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.util.UUID
import com.gexetr.plinthproductphotos.domain.wallet.JobMoneyOutcome
import com.gexetr.plinthproductphotos.domain.wallet.JobMoneyPresentation
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CutoutNotice {
    NONE,
    SESSION_REQUIRED,
    DEVICE_BLOCKED,
    INSUFFICIENT_CREDITS,
    PRICE_CHANGED,
    RATE_LIMITED,
    CAP_REACHED,
    KILL_SWITCH,
    SERVICE_CLOSED,
    VERSION_TOO_OLD,
    TOO_LARGE,
    UNSUPPORTED_MEDIA,
    NOT_CONFIGURED,
    JOB_FAILED,
    MODERATION_BLOCKED,
    GUARD_REJECTED,
    RESULT_EXPIRED,
    INTEGRITY_FAILURE,
    NETWORK_FAILURE,
    UNKNOWN_FAILURE,
    MASK_NOT_FOUND,
    SELECTION_NOT_FOUND,
    RECEIPT_PENDING,
    OPERATION_DISABLED,
    PHOTO_IMPORT_FAILED,
    ERASE_MASK_EMPTY,
    ERASE_VERIFY_FAILED,
    ERASE_DONE,
}

enum class CutoutPhase {
    EMPTY,
    CHECKING_COST,
    READY,
    UPLOADING,
    QUEUED,
    RUNNING,
    DOWNLOADING,
    SAVED,
    FAILED,
    RECEIPT_PENDING,
}

enum class TerminalWalletRefresh {
    REFRESHING,
    REFRESHED,
    FAILED,
}

enum class MaskBrushMode {
    ADD,
    REMOVE,
}

data class CutoutUiState(
    val phase: CutoutPhase = CutoutPhase.EMPTY,
    val notice: CutoutNotice = CutoutNotice.NONE,
    val selectedName: String? = null,
    val sourceBitmap: Bitmap? = null,
    val selectedPhotoId: String? = null,
    val maskOverlay: Bitmap? = null,
    val resultBitmap: Bitmap? = null,
    val maskAlpha: ByteArray? = null,
    val cost: Int? = null,
    val balance: Int? = null,
    val progress: Int? = null,
    val isBusy: Boolean = false,
    val isSavingMask: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val brushMode: MaskBrushMode = MaskBrushMode.ADD,
    val brushSize: Float = 0.018f,
    val selectionSeed: Pair<Float, Float>? = null,
    val selectionConfirmed: Boolean = false,
    val selectAll: Boolean = false,
    val walletShortfall: Int? = null,
    val maskRevision: Int = 0,
    val terminalJobId: String? = null,
    val terminalJobStatus: JobStatus? = null,
    val terminalWalletRefresh: TerminalWalletRefresh? = null,
    /** The job that delivered the current result, used for its settled-charge status. */
    val resultJobId: String? = null,
    /** True while the erase region editor is open; the cutout mask is untouched meanwhile. */
    val eraseActive: Boolean = false,
    val eraseOverlay: Bitmap? = null,
    val eraseCost: Int? = null,
    val erasePainted: Boolean = false,
    val eraseCanUndo: Boolean = false,
    val eraseCanRedo: Boolean = false,
)

internal fun sessionPhotoToOpen(requestedId: String?, recent: List<ProjectPhoto>): ProjectPhoto? =
    requestedId?.let { id -> recent.firstOrNull { it.id == id } } ?: recent.firstOrNull()

internal data class TerminalJobProjection(
    val shouldRefreshWallet: Boolean,
    val moneyOutcome: JobMoneyOutcome?,
)

internal fun projectTerminalJobOutcome(status: JobStatus): TerminalJobProjection {
    val shouldRefreshWallet = status.state == JobState.FAILED || status.state == JobState.CANCELLED
    return TerminalJobProjection(
        shouldRefreshWallet = shouldRefreshWallet,
        moneyOutcome = JobMoneyPresentation.forJob(status, null),
    )
}

/**
 * The Cutout server's provider queue waits for up to 300 seconds
 * (`/opt/plinth-product-photos/current-debug/dist/src/providers/fal.js:47-64`). Keep one extra
 * poll interval so the client can observe a result produced at the end of that server-owned wait.
 */
internal object CutoutPollingDeadline {
    const val providerQueueWaitMs = 300_000L
    const val pollIntervalMs = 2_000L
    const val maxPolls = (providerQueueWaitMs / pollIntervalMs).toInt() + 1

    fun allowsPollNumber(pollNumber: Int): Boolean = pollNumber in 1..maxPolls
}

internal class CutoutPollingRecovery {
    var consecutiveNetworkFailureMs: Long = 0
        private set

    fun afterNetworkFailure(failure: ApiFailure.Network): CutoutPollDecision {
        val delayMs = (CutoutPollingDeadline.pollIntervalMs shl
            minOf(consecutiveNetworkFailureMs / CutoutPollingDeadline.pollIntervalMs, 2L).toInt())
            .coerceAtMost(MAX_NETWORK_RETRY_DELAY_MS)
        consecutiveNetworkFailureMs += delayMs
        return if (consecutiveNetworkFailureMs >= MAX_CONSECUTIVE_NETWORK_FAILURE_MS) {
            CutoutPollDecision.ConnectionLost(CONNECTION_LOST_MESSAGE)
        } else {
            CutoutPollDecision.Retry(delayMs)
        }
    }

    fun afterStatus(state: JobState): CutoutPollDecision {
        consecutiveNetworkFailureMs = 0
        return when (state) {
            JobState.QUEUED, JobState.RUNNING, JobState.SUCCEEDED -> CutoutPollDecision.Continue
            JobState.FAILED, JobState.CANCELLED -> CutoutPollDecision.TerminalStatus
        }
    }

    fun afterSuccessfulResponse(): CutoutPollDecision {
        consecutiveNetworkFailureMs = 0
        return CutoutPollDecision.Continue
    }

    private companion object {
        const val MAX_NETWORK_RETRY_DELAY_MS = 8_000L
        const val MAX_CONSECUTIVE_NETWORK_FAILURE_MS = 120_000L
        const val CONNECTION_LOST_MESSAGE =
            "The connection was lost while the edit was still running. Your result may still be available when you return."
    }
}

internal sealed interface CutoutPollDecision {
    data class Retry(val delayMs: Long) : CutoutPollDecision
    data class ConnectionLost(val message: String) : CutoutPollDecision
    data object Continue : CutoutPollDecision
    data object TerminalStatus : CutoutPollDecision
}

class CutoutViewModel(
    context: Context,
    private val api: ApiClient = ApiClient(),
) : ViewModel() {
    private val appContext = context.applicationContext
    private val projects = ProjectRepository(appContext)
    private val importer = PhotoImporter(appContext, projects)
    private val sessionStore = WalletSessionStore(appContext)
    private val sessionBootstrap = SessionBootstrap(sessionStore, api::getIntegrityChallenge, api::createSession)
    private val _sessionConnection = MutableStateFlow<SessionConnectionState>(SessionConnectionState.Connecting)
    val sessionConnection: StateFlow<SessionConnectionState> = _sessionConnection.asStateFlow()
    private var sessionJob: Job? = null
    private val pendingStore = PendingCutoutStore(appContext)
    private val _uiState = MutableStateFlow(CutoutUiState())
    val uiState: StateFlow<CutoutUiState> = _uiState.asStateFlow()
    private var currentPhoto: ProjectPhoto? = null
    private var sourcePixels: PixelBuffer? = null
    private var sourceUploadBytes: ByteArray? = null
    private var currentMask: Mask? = null
    private var initialMask: Mask? = null
    private val maskHistory = MaskEditHistory()
    private var eraseMask: ByteArray? = null
    private val eraseHistory = MaskEditHistory()
    private var requestedPhotoId: String? = null

    /** Names the photo the next session-photo load must open; null keeps the newest-import behaviour. */
    fun requestPhoto(photoId: String?) {
        if (currentPhoto == null) requestedPhotoId = photoId
    }

    fun select(uri: Uri) {
        if (_uiState.value.isBusy) return
        _uiState.update {
            CutoutUiState(phase = CutoutPhase.CHECKING_COST, isBusy = true)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                loadPhoto(importer.importPhoto(uri))
            } catch (_: java.io.IOException) {
                _uiState.update {
                    it.copy(
                        phase = CutoutPhase.FAILED,
                        isBusy = false,
                        notice = CutoutNotice.PHOTO_IMPORT_FAILED,
                    )
                }
            } catch (_: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        phase = CutoutPhase.FAILED,
                        isBusy = false,
                        notice = CutoutNotice.UNSUPPORTED_MEDIA,
                    )
                }
            } catch (_: SecurityException) {
                _uiState.update {
                    it.copy(
                        phase = CutoutPhase.FAILED,
                        isBusy = false,
                        notice = CutoutNotice.PHOTO_IMPORT_FAILED,
                    )
                }
            }
        }
    }

    private suspend fun loadPhoto(photo: ProjectPhoto) {
        val source = ProjectImageLoader(projects).load(photo.id).source
        currentPhoto = photo
        sourcePixels = source
        sourceUploadBytes = prepareUpload(source)
        currentMask = null
        initialMask = null
        maskHistory.clear()
        _uiState.update {
            it.copy(
                phase = CutoutPhase.CHECKING_COST,
                isBusy = false,
                selectedName = photo.name,
                sourceBitmap = source.toBitmap(),
                selectedPhotoId = photo.id,
                maskOverlay = null,
                resultBitmap = null,
                maskAlpha = null,
                maskRevision = 0,
                canUndo = false,
                canRedo = false,
                selectionSeed = null,
                selectionConfirmed = false,
                selectAll = false,
                brushSize = DEFAULT_BRUSH_SIZE,
                notice = CutoutNotice.NONE,
                walletShortfall = null,
                resultJobId = null,
            )
        }
        refreshQuote()
    }

    /** Opens the photo already in the session (newest import) so no second picker is needed. */
    private suspend fun openSessionPhoto(): Boolean {
        val photo = sessionPhotoToOpen(requestedPhotoId, projects.recentPhotos()) ?: return false
        return try {
            loadPhoto(photo)
            true
        } catch (_: java.io.IOException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun refreshCostAndBalance() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch(Dispatchers.IO) {
            val pending = pendingStore.current()
            if (pending?.receipt != null) {
                retryDeliveryReceipt(pending)
                return@launch
            }
            if (pending?.jobId != null) {
                resumePendingJob(pending)
                return@launch
            }
            if (pending != null) {
                // A request that never got a job is not restarted without a tap.
                if (currentPhoto == null && loadPendingSource(pending)) {
                    _uiState.update { it.copy(isBusy = false) }
                    refreshQuote()
                    return@launch
                }
            }
            if (currentPhoto == null) {
                if (!openSessionPhoto()) {
                    _uiState.update { it.copy(phase = CutoutPhase.EMPTY) }
                }
                return@launch
            }
            if (_uiState.value.terminalJobStatus != null) {
                refreshTerminalWallet(sessionStore.current()?.deviceToken)
                return@launch
            }
            refreshQuote()
        }
    }

    fun startCutout() {
        if (_uiState.value.isBusy || !_uiState.value.selectionConfirmed ||
            sourcePixels == null || currentPhoto == null
        ) return
        _uiState.update {
            it.copy(isBusy = true, notice = CutoutNotice.NONE, walletShortfall = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            startOrResume()
        }
    }

    fun createServerSession(integrity: IntegrityTokenProvider) {
        if (sessionJob?.isActive == true) return
        sessionJob = viewModelScope.launch(Dispatchers.IO) {
            if (_sessionConnection.value !is SessionConnectionState.Ready) {
                _sessionConnection.value = SessionConnectionState.Connecting
            }
            val result = sessionBootstrap.connect(
                appVersion = BuildConfig.VERSION_NAME,
                locale = appContext.resources.configuration.locales[0].toLanguageTag(),
                integrity = integrity,
            )
            _sessionConnection.value = result
            if (result is SessionConnectionState.Ready) {
                _uiState.update { it.copy(balance = result.session.balanceCache) }
            }
        }
    }

    fun setBrushMode(mode: MaskBrushMode) {
        _uiState.update { it.copy(brushMode = mode) }
    }

    fun setBrushSize(size: Float) {
        if (!size.isFinite()) return
        _uiState.update { it.copy(brushSize = size.coerceIn(MIN_BRUSH_SIZE, MAX_BRUSH_SIZE)) }
    }

    fun setSelectionSeed(x: Float, y: Float) {
        if (_uiState.value.isBusy || x !in 0f..1f || y !in 0f..1f) return
        _uiState.update {
            it.copy(
                selectionSeed = x to y,
                selectionConfirmed = true,
                selectAll = false,
                notice = CutoutNotice.NONE,
            )
        }
    }

    fun selectAll() {
        if (_uiState.value.isBusy) return
        val mask = currentMask
        val source = sourcePixels
        if (mask == null || source == null) {
            _uiState.update {
                it.copy(
                    selectionSeed = null,
                    selectionConfirmed = true,
                    selectAll = true,
                    notice = CutoutNotice.NONE,
                )
            }
            return
        }
        val allPixels = Mask(ByteArray(mask.alpha.size) { 0xff.toByte() })
        updateMask(allPixels)
    }

    fun resetSelection() {
        if (_uiState.value.isBusy) return
        val mask = currentMask
        val baseline = initialMask
        if (mask != null && baseline != null) {
            updateMask(Mask(baseline.alpha.copyOf()))
            return
        }
        _uiState.update {
            it.copy(
                selectionSeed = null,
                selectionConfirmed = false,
                selectAll = false,
                notice = CutoutNotice.NONE,
            )
        }
    }

    fun dismissWalletShortfall() {
        _uiState.update { it.copy(walletShortfall = null) }
    }

    fun undoBrushStroke() {
        if (_uiState.value.isBusy) return
        val current = currentMask ?: return
        val prior = maskHistory.undo(current.alpha) ?: return
        currentMask = Mask(prior)
        persistAndRenderMask()
    }

    fun redoBrushStroke() {
        if (_uiState.value.isBusy) return
        val current = currentMask ?: return
        val next = maskHistory.redo(current.alpha) ?: return
        currentMask = Mask(next)
        persistAndRenderMask()
    }

    fun resetMask() {
        if (_uiState.value.isBusy) return
        val current = currentMask ?: return
        val baseline = initialMask ?: return
        if (current.alpha.contentEquals(baseline.alpha)) return
        maskHistory.record(current.alpha)
        currentMask = Mask(baseline.alpha.copyOf())
        persistAndRenderMask()
    }

    fun applyBrushStroke(points: List<Pair<Float, Float>>) {
        if (_uiState.value.isBusy) return
        val mask = currentMask ?: return
        val source = sourcePixels ?: return
        if (points.isEmpty()) return
        maskHistory.record(mask.alpha)
        val updated = mask.alpha.copyOf()
        val radius = (minOf(source.width, source.height) * _uiState.value.brushSize)
            .toInt().coerceAtLeast(1)
        val mode = _uiState.value.brushMode
        val pixels = points.map { (x, y) ->
            (x.coerceIn(0f, 1f) * (source.width - 1)).toInt() to
                (y.coerceIn(0f, 1f) * (source.height - 1)).toInt()
        }
        pixels.zipWithNext().forEach { (first, second) ->
            paintLine(updated, source.width, source.height, first, second, radius, mode)
        }
        if (pixels.size == 1) {
            paintLine(updated, source.width, source.height, pixels[0], pixels[0], radius, mode)
        }
        currentMask = Mask(updated)
        persistAndRenderMask()
    }

    fun openErase() {
        if (_uiState.value.isBusy) return
        val source = sourcePixels ?: return
        eraseMask = ByteArray(source.width * source.height)
        eraseHistory.clear()
        _uiState.update {
            it.copy(
                eraseActive = true,
                eraseOverlay = null,
                erasePainted = false,
                eraseCanUndo = false,
                eraseCanRedo = false,
                brushMode = MaskBrushMode.ADD,
                notice = CutoutNotice.NONE,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val config = api.getConfig()
            if (config is ApiResult.Success) {
                val operation = config.value.ops["O-12"]
                _uiState.update {
                    it.copy(
                        eraseCost = operation?.credits,
                        notice = if (operation == null || !operation.enabled) {
                            CutoutNotice.OPERATION_DISABLED
                        } else {
                            it.notice
                        },
                    )
                }
            }
        }
    }

    fun closeErase() {
        if (_uiState.value.isBusy) return
        eraseMask = null
        eraseHistory.clear()
        _uiState.update {
            it.copy(
                eraseActive = false,
                eraseOverlay = null,
                erasePainted = false,
                eraseCanUndo = false,
                eraseCanRedo = false,
                notice = CutoutNotice.NONE,
            )
        }
    }

    fun applyEraseStroke(points: List<Pair<Float, Float>>) {
        if (_uiState.value.isBusy) return
        val current = eraseMask ?: return
        val source = sourcePixels ?: return
        if (points.isEmpty()) return
        eraseHistory.record(current)
        val updated = current.copyOf()
        val radius = (minOf(source.width, source.height) * _uiState.value.brushSize)
            .toInt().coerceAtLeast(1)
        val mode = _uiState.value.brushMode
        val pixels = points.map { (x, y) ->
            (x.coerceIn(0f, 1f) * (source.width - 1)).toInt() to
                (y.coerceIn(0f, 1f) * (source.height - 1)).toInt()
        }
        pixels.zipWithNext().forEach { (first, second) ->
            paintLine(updated, source.width, source.height, first, second, radius, mode)
        }
        if (pixels.size == 1) {
            paintLine(updated, source.width, source.height, pixels[0], pixels[0], radius, mode)
        }
        setEraseMask(updated)
    }

    fun undoErase() {
        if (_uiState.value.isBusy) return
        val current = eraseMask ?: return
        val prior = eraseHistory.undo(current) ?: return
        setEraseMask(prior)
    }

    fun redoErase() {
        if (_uiState.value.isBusy) return
        val current = eraseMask ?: return
        val next = eraseHistory.redo(current) ?: return
        setEraseMask(next)
    }

    /** Clears only the erase region, and records it so the clear can be undone. */
    fun clearEraseRegion() {
        if (_uiState.value.isBusy) return
        val current = eraseMask ?: return
        if (EraseMask.isEmpty(current)) return
        eraseHistory.record(current)
        setEraseMask(ByteArray(current.size))
    }

    private fun setEraseMask(mask: ByteArray) {
        eraseMask = mask
        _uiState.update {
            it.copy(
                erasePainted = !EraseMask.isEmpty(mask),
                eraseCanUndo = eraseHistory.canUndo,
                eraseCanRedo = eraseHistory.canRedo,
                notice = CutoutNotice.NONE,
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            val overlay = makeMaskOverlay(Mask(mask), ERASE_OVERLAY_COLOR)
            if (eraseMask === mask) _uiState.update { it.copy(eraseOverlay = overlay) }
        }
    }

    fun startErase() {
        if (_uiState.value.isBusy) return
        val painted = eraseMask ?: return
        val source = sourcePixels ?: return
        val upload = sourceUploadBytes ?: return
        val (width, height) = EraseMask.uploadSize(source.width, source.height, MAX_UPLOAD_LONGEST_SIDE)
        val buffer = EraseMask.buffer(painted, source.width, source.height, width, height)
        try {
            EraseMask.requireNotEmpty(buffer)
        } catch (_: EmptyEraseMaskException) {
            _uiState.update { it.copy(notice = CutoutNotice.ERASE_MASK_EMPTY) }
            return
        }
        _uiState.update { it.copy(isBusy = true, phase = CutoutPhase.UPLOADING, notice = CutoutNotice.NONE) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                runErase(upload, buffer, width, height)
            } catch (_: java.io.IOException) {
                showFailure(ApiFailure.Unknown("ERASE_STORAGE_FAILED", "", null, kotlinx.serialization.json.JsonObject(emptyMap())))
            }
        }
    }

    private suspend fun runErase(upload: ByteArray, maskBuffer: ByteArray, width: Int, height: Int) {
        val session = sessionStore.current()
        if (session == null) {
            _uiState.update { it.copy(isBusy = false, phase = CutoutPhase.FAILED, notice = CutoutNotice.SESSION_REQUIRED) }
            return
        }
        val token = session.deviceToken
        val config = api.getConfig()
        if (config is ApiResult.Failure) {
            showFailure(config.failure)
            return
        }
        val operation = (config as ApiResult.Success).value.ops["O-12"]
        if (operation == null || !operation.enabled) {
            _uiState.update {
                it.copy(isBusy = false, phase = CutoutPhase.FAILED, eraseCost = operation?.credits, notice = CutoutNotice.OPERATION_DISABLED)
            }
            return
        }
        _uiState.update { it.copy(eraseCost = operation.credits) }
        val walletResult = api.getWallet(token)
        if (walletResult is ApiResult.Failure) {
            if (walletResult.failure == ApiFailure.Unauthorized) sessionStore.clearSession()
            showFailure(walletResult.failure)
            return
        }
        val wallet = (walletResult as ApiResult.Success).value
        sessionStore.updateServerBalance(wallet.balance)
        if (wallet.balance < operation.credits) {
            showInsufficientCredits(wallet, operation.credits)
            return
        }
        // Order matters: uploadIds[0] is the photo and uploadIds[1] is the mask.
        val photoUpload = when (val uploaded = api.uploadPhoto(upload, "image/jpeg", token)) {
            is ApiResult.Failure -> { showFailure(uploaded.failure); return }
            is ApiResult.Success -> uploaded.value.uploadId
        }
        val maskPng = EraseMask.encodePng(maskBuffer, width, height)
        val maskUpload = when (val uploaded = api.uploadMask(maskPng, token)) {
            is ApiResult.Failure -> { showFailure(uploaded.failure); return }
            is ApiResult.Success -> uploaded.value.uploadId
        }
        val accepted = when (val submitted = api.submitErase(
            clientRequestId = UUID.randomUUID().toString(),
            photoUploadId = photoUpload,
            maskUploadId = maskUpload,
            credits = operation.credits,
            deviceToken = token,
        )) {
            is ApiResult.Failure -> {
                if (submitted.failure == ApiFailure.InsufficientCredits) {
                    showInsufficientCredits(wallet, operation.credits)
                } else {
                    showFailure(submitted.failure)
                }
                return
            }
            is ApiResult.Success -> submitted.value
        }
        _uiState.update { it.copy(phase = CutoutPhase.QUEUED) }
        val result = pollEraseJob(accepted.jobId, token) ?: return
        _uiState.update { it.copy(phase = CutoutPhase.DOWNLOADING, progress = null) }
        var delivered: DeliveredResult? = null
        for (attempt in 1..ERASE_DOWNLOAD_ATTEMPTS) {
            when (val response = api.downloadResult(accepted.jobId, result, token)) {
                is ApiResult.Success -> { delivered = response.value; break }
                is ApiResult.Failure -> {
                    if (response.failure is ApiFailure.Network && attempt < ERASE_DOWNLOAD_ATTEMPTS) {
                        delay(CutoutPollingDeadline.pollIntervalMs)
                    } else {
                        showFailure(response.failure)
                        return
                    }
                }
            }
        }
        val output = delivered ?: return
        val source = sourcePixels
        if (source == null) {
            showFailure(ApiFailure.Unknown("SOURCE_NOT_AVAILABLE", "", null, kotlinx.serialization.json.JsonObject(emptyMap())))
            return
        }
        val verified = try {
            val decoded = decodeEraseOutput(output.bytes)
            EraseVerifier.compositeOutsideMask(scaledForUpload(source), decoded, maskBuffer)
        } catch (_: EraseVerificationException) {
            showEraseVerifyFailed()
            return
        } catch (_: IllegalArgumentException) {
            showEraseVerifyFailed()
            return
        }
        // Not saved or acknowledged when verification fails: the photo the seller asked for was not delivered.
        val photo = saveErasedPhoto(verified)
        val receipt = PendingDeliveryReceipt(
            jobId = accepted.jobId,
            deliveryAttemptId = output.deliveryAttemptId,
            bytes = output.bytes.size.toLong(),
            sha256 = output.sha256,
            maskPath = "",
        )
        val receiptResponse = api.submitDeliveryReceipt(
            receipt.jobId,
            DeliveryReceipt(receipt.deliveryAttemptId, receipt.bytes, receipt.sha256),
            token,
        )
        if (receiptResponse is ApiResult.Failure) enqueueReceipt(receipt)
        val refreshed = api.getWallet(token)
        if (refreshed is ApiResult.Success) sessionStore.updateServerBalance(refreshed.value.balance)
        eraseMask = null
        eraseHistory.clear()
        loadPhoto(photo)
        _uiState.update {
            it.copy(
                eraseActive = false,
                eraseOverlay = null,
                erasePainted = false,
                eraseCanUndo = false,
                eraseCanRedo = false,
                phase = CutoutPhase.READY,
                notice = CutoutNotice.ERASE_DONE,
            )
        }
    }

    private fun showEraseVerifyFailed() {
        _uiState.update {
            it.copy(isBusy = false, phase = CutoutPhase.FAILED, progress = null, notice = CutoutNotice.ERASE_VERIFY_FAILED)
        }
    }

    private suspend fun pollEraseJob(jobId: String, deviceToken: String): JobResult? {
        val recovery = CutoutPollingRecovery()
        for (attempt in 1..CutoutPollingDeadline.maxPolls) {
            when (val response = api.pollJob(jobId, deviceToken)) {
                is ApiResult.Failure -> {
                    if (response.failure is ApiFailure.Network) {
                        when (val decision = recovery.afterNetworkFailure(response.failure)) {
                            is CutoutPollDecision.Retry -> {
                                delay(decision.delayMs)
                                continue
                            }
                            else -> {
                                showConnectionLostWhileRunning()
                                return null
                            }
                        }
                    }
                    showFailure(response.failure)
                    return null
                }
                is ApiResult.Success -> {
                    val status = response.value
                    recovery.afterStatus(status.state)
                    when (status.state) {
                        JobState.QUEUED -> _uiState.update { it.copy(phase = CutoutPhase.QUEUED, progress = status.progress) }
                        JobState.RUNNING -> _uiState.update { it.copy(phase = CutoutPhase.RUNNING, progress = status.progress) }
                        JobState.FAILED, JobState.CANCELLED -> {
                            val projection = projectTerminalJobOutcome(status)
                            _uiState.update {
                                it.copy(
                                    isBusy = false,
                                    phase = CutoutPhase.FAILED,
                                    notice = if (status.notCharged) noticeForJobError(status.error) else CutoutNotice.UNKNOWN_FAILURE,
                                    progress = null,
                                    balance = null,
                                    terminalJobId = jobId,
                                    terminalJobStatus = status,
                                    terminalWalletRefresh = if (projection.shouldRefreshWallet) {
                                        TerminalWalletRefresh.REFRESHING
                                    } else {
                                        null
                                    },
                                )
                            }
                            if (projection.shouldRefreshWallet) refreshTerminalWallet(deviceToken)
                            return null
                        }
                        JobState.SUCCEEDED -> {
                            val result = status.result
                            if (result == null) {
                                _uiState.update {
                                    it.copy(isBusy = false, phase = CutoutPhase.FAILED, notice = CutoutNotice.UNKNOWN_FAILURE)
                                }
                            }
                            return result
                        }
                    }
                }
            }
            delay(CutoutPollingDeadline.pollIntervalMs)
        }
        showConnectionLostWhileRunning()
        return null
    }

    private fun decodeEraseOutput(bytes: ByteArray): PixelBuffer {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IllegalArgumentException("Erased image could not be decoded")
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for (index in pixels.indices) {
                val argb = pixels[index]
                pixels[index] = (argb shl 8) or (argb ushr 24)
            }
            return PixelBuffer(bitmap.width, bitmap.height, pixels)
        } finally {
            bitmap.recycle()
        }
    }

    private fun saveErasedPhoto(verified: PixelBuffer): ProjectPhoto {
        val id = UUID.randomUUID().toString()
        val relativePath = "sources/$id.png"
        val file = projects.sourceFile(relativePath)
        file.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw java.io.IOException("Could not create photo storage") }
        val bitmap = verified.toBitmap()
        try {
            FileOutputStream(file).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw java.io.IOException("Could not encode the erased photo")
                }
            }
        } catch (failure: java.io.IOException) {
            file.delete()
            throw failure
        } finally {
            bitmap.recycle()
        }
        val name = _uiState.value.selectedName ?: appContext.getString(R.string.cutout_erase_default_name)
        val photo = ProjectPhoto(
            id = id,
            name = appContext.getString(R.string.cutout_erase_saved_name, name),
            sourcePath = relativePath,
            importedAt = System.currentTimeMillis(),
        )
        try {
            projects.addPhoto(photo)
        } catch (failure: java.io.IOException) {
            file.delete()
            throw failure
        }
        return photo
    }

    private suspend fun refreshQuote() {
        when (val config = api.getConfig()) {
            is ApiResult.Failure -> {
                showFailure(config.failure)
                return
            }
            is ApiResult.Success -> {
                val operation = config.value.ops["O-02"]
                if (operation == null) {
                    showFailure(ApiFailure.Unknown("MISSING_OPERATION", "", null, kotlinx.serialization.json.JsonObject(emptyMap())))
                    return
                }
                _uiState.update {
                    it.copy(
                        cost = operation.credits,
                        phase = CutoutPhase.CHECKING_COST,
                        notice = if (operation.enabled) CutoutNotice.SESSION_REQUIRED
                        else CutoutNotice.OPERATION_DISABLED,
                    )
                }
                if (!operation.enabled) return
                val session = sessionStore.current()
                if (session == null) {
                    _uiState.update { it.copy(balance = null, notice = CutoutNotice.SESSION_REQUIRED) }
                    return
                }
                when (val wallet = api.getWallet(session.deviceToken)) {
                    is ApiResult.Failure -> {
                        if (wallet.failure == ApiFailure.Unauthorized) sessionStore.clearSession()
                        showFailure(wallet.failure)
                    }
                    is ApiResult.Success -> {
                        sessionStore.updateServerBalance(wallet.value.balance)
                        _uiState.update {
                            it.copy(
                                balance = wallet.value.balance,
                                notice = CutoutNotice.NONE,
                                phase = CutoutPhase.READY,
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun startOrResume() {
        val photo = currentPhoto
        val source = sourcePixels
        val upload = sourceUploadBytes
        if (photo == null || source == null || upload == null) {
            showFailure(ApiFailure.Unknown("NO_SELECTED_PHOTO", "", null, kotlinx.serialization.json.JsonObject(emptyMap())))
            return
        }
        val session = sessionStore.current()
        if (session == null) {
            _uiState.update {
                it.copy(
                    isBusy = false,
                    phase = CutoutPhase.FAILED,
                    notice = CutoutNotice.SESSION_REQUIRED,
                )
            }
            return
        }
        val config = api.getConfig()
        if (config is ApiResult.Failure) {
            showFailure(config.failure)
            return
        }
        val operation = (config as ApiResult.Success).value.ops["O-02"]
        if (operation == null || !operation.enabled) {
            _uiState.update {
                it.copy(
                    isBusy = false,
                    phase = CutoutPhase.FAILED,
                    cost = operation?.credits,
                    notice = CutoutNotice.OPERATION_DISABLED,
                )
            }
            return
        }
        _uiState.update {
            it.copy(cost = operation.credits, phase = CutoutPhase.READY, notice = CutoutNotice.NONE)
        }
        val walletResult = api.getWallet(session.deviceToken)
        if (walletResult is ApiResult.Failure) {
            if (walletResult.failure == ApiFailure.Unauthorized) sessionStore.clearSession()
            showFailure(walletResult.failure)
            return
        }
        val wallet = (walletResult as ApiResult.Success).value
        sessionStore.updateServerBalance(wallet.balance)
        _uiState.update { it.copy(balance = wallet.balance) }
        if (wallet.balance < operation.credits) {
            val shortfall = operation.credits - wallet.balance
            _uiState.update {
                it.copy(
                    isBusy = false,
                    phase = CutoutPhase.READY,
                    notice = CutoutNotice.INSUFFICIENT_CREDITS,
                    walletShortfall = shortfall,
                )
            }
            return
        }

        val pending = pendingStore.getOrCreate(photo.sourcePath)
        if (pending.sourcePath != photo.sourcePath) {
            _uiState.update {
                it.copy(
                    isBusy = false,
                    phase = CutoutPhase.RECEIPT_PENDING,
                    notice = CutoutNotice.RECEIPT_PENDING,
                )
            }
            return
        }
        if (pending.receipt != null) {
            retryDeliveryReceipt(pending)
            return
        }
        if (pending.jobId != null) {
            pollUntilTerminal(pending.jobId, session.deviceToken)
            return
        }

        _uiState.update { it.copy(phase = CutoutPhase.UPLOADING, notice = CutoutNotice.NONE) }
        when (val uploaded = api.uploadPhoto(upload, "image/jpeg", session.deviceToken)) {
            is ApiResult.Failure -> showFailure(uploaded.failure)
            is ApiResult.Success -> {
                when (val accepted = api.submitCutout(
                    clientRequestId = pending.clientRequestId,
                    uploadId = uploaded.value.uploadId,
                    credits = operation.credits,
                    deviceToken = session.deviceToken,
                )) {
                    is ApiResult.Failure -> {
                        if (accepted.failure == ApiFailure.InsufficientCredits) {
                            showInsufficientCredits(wallet, operation.credits)
                        } else {
                            if (accepted.failure == ApiFailure.PriceChanged) refreshQuote()
                            showFailure(accepted.failure)
                        }
                    }
                    is ApiResult.Success -> {
                        pendingStore.saveJobId(accepted.value.jobId)
                        _uiState.update { it.copy(phase = CutoutPhase.QUEUED, notice = CutoutNotice.NONE) }
                        pollUntilTerminal(accepted.value.jobId, session.deviceToken)
                    }
                }
            }
        }
    }

    private suspend fun resumePendingJob(pending: PendingCutout) {
        val session = sessionStore.current()
        if (session == null) {
            _uiState.update { it.copy(isBusy = false, notice = CutoutNotice.SESSION_REQUIRED) }
            return
        }
        if (!loadPendingSource(pending)) return
        pollUntilTerminal(requireNotNull(pending.jobId), session.deviceToken)
    }

    private suspend fun resumePendingRequest(pending: PendingCutout) {
        if (!loadPendingSource(pending)) return
        _uiState.update { it.copy(isBusy = true, phase = CutoutPhase.UPLOADING) }
        startOrResume()
    }

    private suspend fun loadPendingSource(pending: PendingCutout): Boolean {
        val sourcePath = pending.sourcePath
        val photo = projects.recentPhotos().firstOrNull { it.sourcePath == sourcePath }
        if (photo == null) {
            _uiState.update { it.copy(isBusy = false, notice = CutoutNotice.UNKNOWN_FAILURE) }
            return false
        }
        try {
            currentPhoto = photo
            sourcePixels = ProjectImageLoader(projects).load(photo.id).source
            sourceUploadBytes = prepareUpload(requireNotNull(sourcePixels))
            _uiState.update {
                it.copy(
                    selectedName = photo.name,
                    selectedPhotoId = photo.id,
                    sourceBitmap = requireNotNull(sourcePixels).toBitmap(),
                    isBusy = true,
                )
            }
        } catch (_: java.io.IOException) {
            _uiState.update { it.copy(isBusy = false, notice = CutoutNotice.UNKNOWN_FAILURE) }
            return false
        }
        return true
    }

    private suspend fun pollUntilTerminal(jobId: String, deviceToken: String) {
        val recovery = CutoutPollingRecovery()
        for (attempt in 1..CutoutPollingDeadline.maxPolls) {
            when (val response = api.pollJob(jobId, deviceToken)) {
                is ApiResult.Failure -> {
                    if (response.failure is ApiFailure.Network) {
                        when (val decision = recovery.afterNetworkFailure(response.failure)) {
                            is CutoutPollDecision.Retry -> {
                                delay(decision.delayMs)
                                continue
                            }
                            is CutoutPollDecision.ConnectionLost -> {
                                showConnectionLostWhileRunning()
                                return
                            }
                            CutoutPollDecision.Continue, CutoutPollDecision.TerminalStatus -> {
                                error("Unexpected network recovery decision")
                            }
                        }
                    }
                    showFailure(response.failure)
                    return
                }
                is ApiResult.Success -> {
                    val status = response.value
                    recovery.afterStatus(status.state)
                    when (status.state) {
                        JobState.QUEUED -> {
                            _uiState.update { it.copy(phase = CutoutPhase.QUEUED, progress = status.progress) }
                        }
                        JobState.RUNNING -> {
                            _uiState.update { it.copy(phase = CutoutPhase.RUNNING, progress = status.progress) }
                        }
                        JobState.FAILED,
                        JobState.CANCELLED -> {
                            val projection = projectTerminalJobOutcome(status)
                            pendingStore.clear()
                            _uiState.update {
                                it.copy(
                                    isBusy = false,
                                    phase = CutoutPhase.FAILED,
                                    notice = if (status.notCharged) {
                                        noticeForJobError(status.error)
                                    } else {
                                        CutoutNotice.UNKNOWN_FAILURE
                                    },
                                    progress = null,
                                    balance = null,
                                    terminalJobId = jobId,
                                    terminalJobStatus = status,
                                    terminalWalletRefresh = if (projection.shouldRefreshWallet) {
                                        TerminalWalletRefresh.REFRESHING
                                    } else {
                                        null
                                    },
                                )
                            }
                            if (projection.shouldRefreshWallet) refreshTerminalWallet(deviceToken)
                            return
                        }
                        JobState.SUCCEEDED -> {
                            val result = status.result
                            if (result == null) {
                                _uiState.update {
                                    it.copy(isBusy = false, phase = CutoutPhase.FAILED, notice = CutoutNotice.UNKNOWN_FAILURE)
                                }
                                return
                            }
                            fetchAndSaveResult(jobId, result, deviceToken)
                            return
                        }
                    }
                }
            }
            delay(CutoutPollingDeadline.pollIntervalMs)
        }
        showConnectionLostWhileRunning()
    }

    private suspend fun fetchAndSaveResult(jobId: String, result: JobResult, deviceToken: String) {
        _uiState.update { it.copy(phase = CutoutPhase.DOWNLOADING, progress = null) }
        val recovery = CutoutPollingRecovery()
        while (true) when (val delivered = api.downloadResult(jobId, result, deviceToken)) {
            is ApiResult.Failure -> {
                if (delivered.failure is ApiFailure.Network) {
                    when (val decision = recovery.afterNetworkFailure(delivered.failure)) {
                        is CutoutPollDecision.Retry -> {
                            delay(decision.delayMs)
                            continue
                        }
                        is CutoutPollDecision.ConnectionLost -> {
                            showConnectionLostWhileRunning()
                            return
                        }
                        CutoutPollDecision.Continue, CutoutPollDecision.TerminalStatus -> {
                            error("Unexpected network recovery decision")
                        }
                    }
                }
                showFailure(delivered.failure)
                return
            }
            is ApiResult.Success -> {
                recovery.afterSuccessfulResponse()
                val source = sourcePixels
                val photo = currentPhoto
                if (source == null || photo == null) {
                    showFailure(ApiFailure.Unknown("SOURCE_NOT_AVAILABLE", "", null, kotlinx.serialization.json.JsonObject(emptyMap())))
                    return
                }
                val mask = try {
                    decodeMask(delivered.value, source)
                } catch (_: IllegalArgumentException) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            phase = CutoutPhase.FAILED,
                            notice = CutoutNotice.MASK_NOT_FOUND,
                        )
                    }
                    return
                }
                if (!hasUsefulCoverage(mask)) {
                    _uiState.update {
                        it.copy(isBusy = false, phase = CutoutPhase.FAILED, notice = CutoutNotice.MASK_NOT_FOUND)
                    }
                    return
                }
                val selection = _uiState.value
                var seedMissed = false
                val selectedMask = when {
                    selection.selectAll -> Mask(ByteArray(mask.alpha.size) { 0xff.toByte() })
                    selection.selectionSeed != null -> selectConnectedRegion(
                        mask = mask,
                        width = source.width,
                        height = source.height,
                        seed = requireNotNull(selection.selectionSeed),
                    ) ?: mask.also { seedMissed = true }
                    else -> mask
                }
                currentMask = Mask(selectedMask.alpha.copyOf())
                initialMask = Mask(selectedMask.alpha.copyOf())
                maskHistory.clear()
                val preview = try {
                    saveMaskAtomically(photo.id, selectedMask.alpha, 1)
                    renderCutoutPreview(source, selectedMask)
                } catch (_: java.io.IOException) {
                    showFailure(ApiFailure.Unknown("MASK_STORAGE_FAILED", "", null, kotlinx.serialization.json.JsonObject(emptyMap())))
                    return
                }
                _uiState.update {
                    it.copy(
                        sourceBitmap = source.toBitmap(),
                        selectedPhotoId = photo.id,
                        maskOverlay = makeMaskOverlay(selectedMask),
                        resultBitmap = preview,
                        maskAlpha = selectedMask.alpha.copyOf(),
                        maskRevision = 1,
                        canUndo = false,
                        canRedo = false,
                        notice = if (seedMissed) CutoutNotice.SELECTION_NOT_FOUND else CutoutNotice.NONE,
                        resultJobId = jobId,
                    )
                }
                val receiptData = PendingDeliveryReceipt(
                    jobId = jobId,
                    deliveryAttemptId = delivered.value.deliveryAttemptId,
                    bytes = delivered.value.bytes.size.toLong(),
                    sha256 = delivered.value.sha256,
                    maskPath = revisionFilePath(photo.id, 1),
                )
                try {
                    pendingStore.saveReceipt(receiptData)
                } catch (_: java.io.IOException) {
                    showFailure(
                        ApiFailure.Unknown(
                            "RECEIPT_STORAGE_FAILED",
                            "",
                            null,
                            kotlinx.serialization.json.JsonObject(emptyMap()),
                        ),
                    )
                    return
                }
                enqueueReceipt(receiptData)
                sendReceipt(receiptData, deviceToken)
                return
            }
        }
    }

    private fun showConnectionLostWhileRunning() {
        _uiState.update {
            it.copy(
                isBusy = false,
                phase = CutoutPhase.RECEIPT_PENDING,
                notice = CutoutNotice.NETWORK_FAILURE,
            )
        }
    }

    private suspend fun retryDeliveryReceipt(
        pending: PendingCutout,
    ) {
        val receipt = pending.receipt ?: return
        val session = sessionStore.current()
        if (session == null) {
            _uiState.update {
                it.copy(isBusy = false, phase = CutoutPhase.RECEIPT_PENDING, notice = CutoutNotice.SESSION_REQUIRED)
            }
            return
        }
        if (currentPhoto?.sourcePath != pending.sourcePath) {
            if (!loadPendingSource(pending)) return
            val source = requireNotNull(sourcePixels)
            val photo = requireNotNull(currentPhoto)
            val mask = try {
                loadSavedMask(receipt.maskPath, source)
            } catch (_: java.io.IOException) {
                _uiState.update { it.copy(isBusy = false, notice = CutoutNotice.UNKNOWN_FAILURE) }
                return
            }
            currentMask = mask
            initialMask = Mask(mask.alpha.copyOf())
            _uiState.update {
                it.copy(
                    selectedName = photo.name,
                    selectedPhotoId = photo.id,
                    sourceBitmap = source.toBitmap(),
                    maskOverlay = makeMaskOverlay(mask),
                    resultBitmap = renderCutoutPreview(source, mask),
                    maskAlpha = mask.alpha.copyOf(),
                    maskRevision = 1,
                    isBusy = true,
                    resultJobId = receipt.jobId,
                )
            }
        }
        _uiState.update { it.copy(isBusy = true, phase = CutoutPhase.RECEIPT_PENDING) }
        sendReceipt(receipt, session.deviceToken)
    }

    private suspend fun sendReceipt(receipt: PendingDeliveryReceipt, deviceToken: String) {
        val request = DeliveryReceipt(
            deliveryAttemptId = receipt.deliveryAttemptId,
            bytes = receipt.bytes,
            sha256 = receipt.sha256,
        )
        when (val response = api.submitDeliveryReceipt(receipt.jobId, request, deviceToken)) {
            is ApiResult.Failure -> {
                if (response.failure == ApiFailure.Network(null)) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            phase = CutoutPhase.RECEIPT_PENDING,
                            notice = CutoutNotice.RECEIPT_PENDING,
                        )
                    }
                } else {
                    showFailure(response.failure)
                }
            }
            is ApiResult.Success -> {
                if (response.value.state != "SETTLED") {
                    showFailure(ApiFailure.Unknown("UNEXPECTED_RECEIPT_STATE", "", null, kotlinx.serialization.json.JsonObject(emptyMap())))
                    return
                }
                pendingStore.clear()
                val wallet = api.getWallet(deviceToken)
                if (wallet is ApiResult.Success) sessionStore.updateServerBalance(wallet.value.balance)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        phase = CutoutPhase.SAVED,
                        notice = if (it.notice == CutoutNotice.SELECTION_NOT_FOUND) {
                            it.notice
                        } else {
                            CutoutNotice.NONE
                        },
                        canUndo = maskHistory.canUndo,
                        canRedo = maskHistory.canRedo,
                        walletShortfall = null,
                    )
                }
            }
        }
    }

    private fun enqueueReceipt(receipt: PendingDeliveryReceipt) {
        val work = OneTimeWorkRequestBuilder<DeliveryReceiptWorker>()
            .setInputData(
                Data.Builder()
                    .putString(DeliveryReceiptWorker.KEY_JOB_ID, receipt.jobId)
                    .putString(DeliveryReceiptWorker.KEY_ATTEMPT_ID, receipt.deliveryAttemptId)
                    .putLong(DeliveryReceiptWorker.KEY_BYTES, receipt.bytes)
                    .putString(DeliveryReceiptWorker.KEY_SHA256, receipt.sha256)
                    .build(),
            )
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "delivery-receipt-${receipt.jobId}",
            ExistingWorkPolicy.KEEP,
            work,
        )
    }

    private fun showInsufficientCredits(wallet: WalletResponse, cost: Int) {
        val shortfall = (cost - wallet.balance).coerceAtLeast(1)
        _uiState.update {
            it.copy(
                isBusy = false,
                phase = CutoutPhase.READY,
                balance = wallet.balance,
                notice = CutoutNotice.INSUFFICIENT_CREDITS,
                walletShortfall = shortfall,
            )
        }
    }

    private fun showFailure(failure: ApiFailure) {
        val notice = when (failure) {
            ApiFailure.Unauthorized -> CutoutNotice.SESSION_REQUIRED
            ApiFailure.DeviceBlocked -> CutoutNotice.DEVICE_BLOCKED
            ApiFailure.InsufficientCredits -> CutoutNotice.INSUFFICIENT_CREDITS
            ApiFailure.PriceChanged -> CutoutNotice.PRICE_CHANGED
            ApiFailure.PurchaseInvalid -> CutoutNotice.JOB_FAILED
            ApiFailure.PurchasePending -> CutoutNotice.JOB_FAILED
            ApiFailure.RateLimited -> CutoutNotice.RATE_LIMITED
            ApiFailure.CapReached -> CutoutNotice.CAP_REACHED
            ApiFailure.KillSwitch -> CutoutNotice.KILL_SWITCH
            ApiFailure.ServiceMaintenance -> CutoutNotice.KILL_SWITCH
            ApiFailure.ServiceClosed -> CutoutNotice.SERVICE_CLOSED
            ApiFailure.TooLarge -> CutoutNotice.TOO_LARGE
            ApiFailure.UnsupportedMedia -> CutoutNotice.UNSUPPORTED_MEDIA
            ApiFailure.VersionTooOld -> CutoutNotice.VERSION_TOO_OLD
            ApiFailure.ProviderFailed -> CutoutNotice.JOB_FAILED
            ApiFailure.GuardRejected -> CutoutNotice.GUARD_REJECTED
            ApiFailure.ModerationBlocked -> CutoutNotice.MODERATION_BLOCKED
            ApiFailure.NotConfigured -> CutoutNotice.NOT_CONFIGURED
            ApiFailure.OperationDisabled -> CutoutNotice.OPERATION_DISABLED
            ApiFailure.ResultExpired -> CutoutNotice.RESULT_EXPIRED
            ApiFailure.ResultIntegrityFailed,
            ApiFailure.DeliveryReceiptMismatch -> CutoutNotice.INTEGRITY_FAILURE
            is ApiFailure.Network -> CutoutNotice.NETWORK_FAILURE
            is ApiFailure.Unknown -> CutoutNotice.UNKNOWN_FAILURE
        }
        val refreshTerminalBalance = failure == ApiFailure.GuardRejected ||
            failure == ApiFailure.ProviderFailed
        _uiState.update {
            it.copy(
                isBusy = false,
                phase = if (notice == CutoutNotice.RECEIPT_PENDING) CutoutPhase.RECEIPT_PENDING
                else CutoutPhase.FAILED,
                notice = notice,
                balance = if (refreshTerminalBalance) null else it.balance,
                terminalJobId = if (refreshTerminalBalance) null else it.terminalJobId,
                terminalJobStatus = if (refreshTerminalBalance) null else it.terminalJobStatus,
                terminalWalletRefresh = if (refreshTerminalBalance) {
                    TerminalWalletRefresh.REFRESHING
                } else {
                    it.terminalWalletRefresh
                },
            )
        }
        if (refreshTerminalBalance) {
            viewModelScope.launch(Dispatchers.IO) {
                refreshTerminalWallet(sessionStore.current()?.deviceToken)
            }
        }
    }

    private suspend fun refreshTerminalWallet(deviceToken: String?) {
        _uiState.update {
            it.copy(balance = null, terminalWalletRefresh = TerminalWalletRefresh.REFRESHING)
        }
        if (deviceToken == null) {
            _uiState.update { it.copy(terminalWalletRefresh = TerminalWalletRefresh.FAILED) }
            return
        }
        when (val wallet = api.getWallet(deviceToken)) {
            is ApiResult.Failure -> {
                if (wallet.failure == ApiFailure.Unauthorized) sessionStore.clearSession()
                _uiState.update { it.copy(balance = null, terminalWalletRefresh = TerminalWalletRefresh.FAILED) }
            }
            is ApiResult.Success -> {
                sessionStore.updateServerBalance(wallet.value.balance)
                _uiState.update {
                    it.copy(
                        balance = wallet.value.balance,
                        terminalWalletRefresh = TerminalWalletRefresh.REFRESHED,
                    )
                }
            }
        }
    }

    private fun noticeForJobError(error: ApiJobError?): CutoutNotice = when (error?.typedCode) {
        ApiJobErrorCode.PROVIDER_FAILED -> CutoutNotice.JOB_FAILED
        ApiJobErrorCode.GUARD_REJECTED -> CutoutNotice.GUARD_REJECTED
        ApiJobErrorCode.MODERATION_BLOCKED -> CutoutNotice.MODERATION_BLOCKED
        ApiJobErrorCode.OPERATION_DISABLED -> CutoutNotice.OPERATION_DISABLED
        null -> CutoutNotice.JOB_FAILED
    }

    private fun persistAndRenderMask() {
        val mask = currentMask ?: return
        val source = sourcePixels ?: return
        val photo = currentPhoto ?: return
        val revision = _uiState.value.maskRevision + 1
        _uiState.update { it.copy(isBusy = true, isSavingMask = true) }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                saveMaskAtomically(photo.id, mask.alpha, revision)
            } catch (_: java.io.IOException) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isSavingMask = false,
                        canUndo = maskHistory.canUndo,
                        canRedo = maskHistory.canRedo,
                        notice = CutoutNotice.UNKNOWN_FAILURE,
                    )
                }
                return@launch
            }
            val overlay = makeMaskOverlay(mask)
            val preview = renderCutoutPreview(source, mask)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    isSavingMask = false,
                    maskOverlay = overlay,
                    resultBitmap = preview,
                    maskAlpha = mask.alpha.copyOf(),
                    maskRevision = revision,
                    canUndo = maskHistory.canUndo,
                    canRedo = maskHistory.canRedo,
                    phase = CutoutPhase.SAVED,
                    notice = CutoutNotice.NONE,
                )
            }
        }
    }

    private fun scaledForUpload(source: PixelBuffer): PixelBuffer {
        val (width, height) = EraseMask.uploadSize(source.width, source.height, MAX_UPLOAD_LONGEST_SIDE)
        return if (width != source.width || height != source.height) source.scale(width, height) else source
    }

    private fun prepareUpload(source: PixelBuffer): ByteArray {
        val scaled = scaledForUpload(source)
        val bitmap = scaled.toBitmap()
        try {
            for (quality in 92 downTo 62 step 5) {
                val output = ByteArrayOutputStream()
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                    throw java.io.IOException("Could not encode the photo")
                }
                val bytes = output.toByteArray()
                if (bytes.size <= MAX_UPLOAD_BYTES) return bytes
            }
        } finally {
            bitmap.recycle()
        }
        throw IllegalArgumentException("Photo is above the upload size limit")
    }

    private fun decodeMask(delivered: DeliveredResult, source: PixelBuffer): Mask {
        val bitmap = BitmapFactory.decodeByteArray(delivered.bytes, 0, delivered.bytes.size)
            ?: throw IllegalArgumentException("Mask image could not be decoded")
        if (!bitmap.hasAlpha()) {
            bitmap.recycle()
            throw IllegalArgumentException("Mask image has no alpha channel")
        }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val alpha = ByteArray(pixels.size) { (pixels[it] ushr 24).toByte() }
        val mask = if (bitmap.width == source.width && bitmap.height == source.height) {
            Mask(alpha)
        } else {
            Mask(resizeMask(alpha, bitmap.width, bitmap.height, source.width, source.height))
        }
        bitmap.recycle()
        return mask
    }

    private fun resizeMask(
        source: ByteArray,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): ByteArray {
        val output = ByteArray(targetWidth * targetHeight)
        for (y in 0 until targetHeight) {
            val sourceY = ((y + 0.5f) * sourceHeight / targetHeight).toInt().coerceIn(0, sourceHeight - 1)
            for (x in 0 until targetWidth) {
                val sourceX = ((x + 0.5f) * sourceWidth / targetWidth).toInt().coerceIn(0, sourceWidth - 1)
                output[y * targetWidth + x] = source[sourceY * sourceWidth + sourceX]
            }
        }
        return output
    }

    private fun hasUsefulCoverage(mask: Mask): Boolean {
        val opaque = mask.alpha.count { (it.toInt() and 0xff) > 8 }
        val fraction = opaque.toDouble() / mask.alpha.size
        return fraction in 0.02..0.98
    }

    private fun renderCutoutPreview(source: PixelBuffer, mask: Mask): Bitmap {
        require(mask.alpha.size == source.pixels.size) { "Mask must match the source photo" }
        val scale = minOf(1f, PREVIEW_CANVAS_SIZE.toFloat() / maxOf(source.width, source.height))
        val previewWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val previewHeight = (source.height * scale).toInt().coerceAtLeast(1)
        val previewSource = if (previewWidth == source.width && previewHeight == source.height) {
            source
        } else {
            source.scale(previewWidth, previewHeight)
        }
        val previewMask = if (previewWidth == source.width && previewHeight == source.height) {
            mask.alpha
        } else {
            resizeMask(mask.alpha, source.width, source.height, previewWidth, previewHeight)
        }
        val pixels = IntArray(previewSource.pixels.size) { index ->
            val sourcePixel = previewSource.pixels[index]
            val sourceAlpha = sourcePixel and 0xff
            val maskAlpha = previewMask[index].toInt() and 0xff
            val alpha = (sourceAlpha * maskAlpha + 127) / 255
            val red = sourcePixel ushr 24 and 0xff
            val green = sourcePixel ushr 16 and 0xff
            val blue = sourcePixel ushr 8 and 0xff
            (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
        return Bitmap.createBitmap(pixels, previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
    }

    private fun updateMask(mask: Mask) {
        val current = currentMask ?: return
        if (current.alpha.contentEquals(mask.alpha)) return
        maskHistory.record(current.alpha)
        currentMask = Mask(mask.alpha.copyOf())
        persistAndRenderMask()
    }

    private fun selectConnectedRegion(
        mask: Mask,
        width: Int,
        height: Int,
        seed: Pair<Float, Float>,
    ): Mask? {
        if (mask.alpha.size != width * height) return null
        val scale = maxOf(1, kotlin.math.ceil(maxOf(width, height) / MAX_SELECTION_GRID_SIDE.toFloat()).toInt())
        val gridWidth = kotlin.math.ceil(width / scale.toFloat()).toInt()
        val gridHeight = kotlin.math.ceil(height / scale.toFloat()).toInt()
        val active = BooleanArray(gridWidth * gridHeight)
        for (gridY in 0 until gridHeight) {
            val top = gridY * scale
            val bottom = minOf(height, top + scale)
            for (gridX in 0 until gridWidth) {
                val left = gridX * scale
                val right = minOf(width, left + scale)
                var found = false
                loop@ for (y in top until bottom) {
                    for (x in left until right) {
                        if ((mask.alpha[y * width + x].toInt() and 0xff) > MASK_FOREGROUND_THRESHOLD) {
                            found = true
                            break@loop
                        }
                    }
                }
                active[gridY * gridWidth + gridX] = found
            }
        }

        val seedX = (seed.first * (gridWidth - 1)).toInt().coerceIn(0, gridWidth - 1)
        val seedY = (seed.second * (gridHeight - 1)).toInt().coerceIn(0, gridHeight - 1)
        val searchRadius = maxOf(3, maxOf(gridWidth, gridHeight) / 30)
        var start = -1
        var closestDistance = Int.MAX_VALUE
        for (y in (seedY - searchRadius).coerceAtLeast(0)..(seedY + searchRadius).coerceAtMost(gridHeight - 1)) {
            for (x in (seedX - searchRadius).coerceAtLeast(0)..(seedX + searchRadius).coerceAtMost(gridWidth - 1)) {
                val index = y * gridWidth + x
                if (!active[index]) continue
                val dx = x - seedX
                val dy = y - seedY
                val distance = dx * dx + dy * dy
                if (distance < closestDistance) {
                    closestDistance = distance
                    start = index
                }
            }
        }
        if (start < 0) return null

        val component = BooleanArray(active.size)
        val queue = IntArray(active.size)
        var head = 0
        var tail = 0
        queue[tail++] = start
        active[start] = false
        component[start] = true
        fun enqueue(neighbor: Int) {
            if (neighbor >= 0 && active[neighbor]) {
                active[neighbor] = false
                component[neighbor] = true
                queue[tail++] = neighbor
            }
        }
        while (head < tail) {
            val index = queue[head++]
            val x = index % gridWidth
            val y = index / gridWidth
            if (x > 0) enqueue(index - 1)
            if (x + 1 < gridWidth) enqueue(index + 1)
            if (y > 0) enqueue(index - gridWidth)
            if (y + 1 < gridHeight) enqueue(index + gridWidth)
        }
        val selected = mask.alpha.copyOf()
        for (y in 0 until height) {
            val gridY = y / scale
            for (x in 0 until width) {
                if (!component[gridY * gridWidth + x / scale]) selected[y * width + x] = 0
            }
        }
        return Mask(selected)
    }

    private fun makeMaskOverlay(mask: Mask, color: Int = OVERLAY_COLOR): Bitmap {
        val source = requireNotNull(sourcePixels)
        val scale = minOf(1f, PREVIEW_CANVAS_SIZE.toFloat() / maxOf(source.width, source.height))
        val overlayWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val overlayHeight = (source.height * scale).toInt().coerceAtLeast(1)
        val overlayPixels = IntArray(overlayWidth * overlayHeight) { index ->
            val x = index % overlayWidth
            val y = index / overlayWidth
            val sourceX = (x * source.width / overlayWidth).coerceIn(0, source.width - 1)
            val sourceY = (y * source.height / overlayHeight).coerceIn(0, source.height - 1)
            val maskAlpha = mask.alpha[sourceY * source.width + sourceX].toInt() and 0xff
            val alpha = maskAlpha * OVERLAY_OPACITY / 255
            (alpha shl 24) or color
        }
        return Bitmap.createBitmap(
            overlayPixels,
            overlayWidth,
            overlayHeight,
            Bitmap.Config.ARGB_8888,
        )
    }

    private fun saveMaskAtomically(itemId: String, alpha: ByteArray, revision: Int): File {
        val source = requireNotNull(sourcePixels)
        val directory = File(appContext.filesDir, "masks")
        if (!directory.exists() && !directory.mkdirs()) throw java.io.IOException("Could not create mask storage")
        val bitmapPixels = IntArray(alpha.size) { index ->
            ((alpha[index].toInt() and 0xff) shl 24) or 0x00ffffff
        }
        val bitmap = Bitmap.createBitmap(bitmapPixels, source.width, source.height, Bitmap.Config.ARGB_8888)
        val output = File(directory, "$itemId-$revision.png")
        val temporary = File(directory, "$itemId-$revision.png.part")
        try {
            FileOutputStream(temporary).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw java.io.IOException("Could not encode mask")
                }
                stream.fd.sync()
            }
            Files.move(
                temporary.toPath(),
                output.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (failure: Exception) {
            temporary.delete()
            throw failure
        } finally {
            bitmap.recycle()
        }
        directory.listFiles()
            ?.filter { it.name.startsWith("$itemId-") && it.name.endsWith(".png") }
            ?.sortedByDescending(File::lastModified)
            ?.drop(MASK_REVISION_LIMIT)
            ?.forEach(File::delete)
        return output
    }

    private fun revisionFilePath(itemId: String, revision: Int): String =
        "masks/$itemId-$revision.png"

    private fun loadSavedMask(path: String, source: PixelBuffer): Mask {
        val bitmap = BitmapFactory.decodeFile(File(appContext.filesDir, path).path)
            ?: throw java.io.IOException("Saved mask is not available")
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val alpha = ByteArray(pixels.size) { (pixels[it] ushr 24).toByte() }
        val mask = if (bitmap.width == source.width && bitmap.height == source.height) {
            Mask(alpha)
        } else {
            Mask(resizeMask(alpha, bitmap.width, bitmap.height, source.width, source.height))
        }
        bitmap.recycle()
        return mask
    }

    private fun paintLine(
        alpha: ByteArray,
        width: Int,
        height: Int,
        start: Pair<Int, Int>,
        end: Pair<Int, Int>,
        radius: Int,
        mode: MaskBrushMode,
    ) {
        val dx = end.first - start.first
        val dy = end.second - start.second
        val steps = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)).coerceAtLeast(1)
        val value = if (mode == MaskBrushMode.ADD) 0xff.toByte() else 0
        for (step in 0..steps) {
            val centerX = start.first + dx * step / steps
            val centerY = start.second + dy * step / steps
            for (y in (centerY - radius).coerceAtLeast(0)..(centerY + radius).coerceAtMost(height - 1)) {
                for (x in (centerX - radius).coerceAtLeast(0)..(centerX + radius).coerceAtMost(width - 1)) {
                    val offsetX = x - centerX
                    val offsetY = y - centerY
                    if (offsetX * offsetX + offsetY * offsetY <= radius * radius) {
                        alpha[y * width + x] = value
                    }
                }
            }
        }
    }

    companion object {
        private const val MAX_UPLOAD_LONGEST_SIDE = 2048
        private const val MAX_UPLOAD_BYTES = 4 * 1024 * 1024
        private const val PREVIEW_CANVAS_SIZE = 1200
        private const val MASK_REVISION_LIMIT = 5
        private const val DEFAULT_BRUSH_SIZE = 0.018f
        private const val MIN_BRUSH_SIZE = 0.005f
        private const val MAX_BRUSH_SIZE = 0.06f
        private const val MAX_SELECTION_GRID_SIDE = 512
        private const val MASK_FOREGROUND_THRESHOLD = 8
        private const val ERASE_DOWNLOAD_ATTEMPTS = 3
        private const val OVERLAY_OPACITY = 105
        private const val OVERLAY_COLOR = 0x0000bca8
        private const val ERASE_OVERLAY_COLOR = 0x00e5352b
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CutoutViewModel(applicationContext) as T
            }
        }
    }
}

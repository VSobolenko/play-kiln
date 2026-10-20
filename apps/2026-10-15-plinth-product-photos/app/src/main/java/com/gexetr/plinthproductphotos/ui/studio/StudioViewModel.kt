package com.gexetr.plinthproductphotos.ui.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.api.ApiClient
import com.gexetr.plinthproductphotos.data.api.ApiConfigResponse
import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiJobError
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.DeliveryReceipt
import com.gexetr.plinthproductphotos.data.api.JobAccepted
import com.gexetr.plinthproductphotos.data.api.JobRequest
import com.gexetr.plinthproductphotos.data.api.JobState
import com.gexetr.plinthproductphotos.BuildConfig
import com.gexetr.plinthproductphotos.data.api.OperationId
import com.gexetr.plinthproductphotos.data.session.DevIntegrityTokenProvider
import com.gexetr.plinthproductphotos.data.session.IntegrityTokenProvider
import com.gexetr.plinthproductphotos.data.session.PlayIntegrityTokenProvider
import com.gexetr.plinthproductphotos.data.session.SessionBootstrap
import com.gexetr.plinthproductphotos.data.session.SessionConnectionState
import com.gexetr.plinthproductphotos.data.db.OutputEntity
import com.gexetr.plinthproductphotos.data.db.PlinthDatabase
import com.gexetr.plinthproductphotos.data.photoimport.PhotoImporter
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels.toBitmap
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImageLoader
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.data.project.PROJECT_OUTPUT_DIRECTORY
import com.gexetr.plinthproductphotos.data.session.WalletSessionStore
import com.gexetr.plinthproductphotos.domain.export.WatermarkGate
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.studio.CanvasAspect
import com.gexetr.plinthproductphotos.domain.studio.MutablePixelRows
import com.gexetr.plinthproductphotos.domain.studio.ProductPixelRestoreException
import com.gexetr.plinthproductphotos.domain.studio.addStudioPngProvenance
import com.gexetr.plinthproductphotos.domain.studio.calculateCanvasExpansion
import com.gexetr.plinthproductphotos.domain.studio.restoreOriginalPixels
import com.gexetr.plinthproductphotos.domain.studio.uploadImageDimensions
import com.gexetr.plinthproductphotos.ui.wallet.ServerUnlock
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

internal enum class StudioPhase {
    READY, UPLOADING, QUEUED, RUNNING, DOWNLOADING, CONFIRMING, DONE, FAILED,
}

internal data class StudioState(
    val photos: List<ProjectPhoto> = emptyList(),
    val photoId: String? = null,
    val config: ApiConfigResponse? = null,
    val balance: Int? = null,
    val reserved: Int? = null,
    val busy: Boolean = false,
    val phase: StudioPhase = StudioPhase.READY,
    val result: Bitmap? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val retryAfterSec: Int? = null,
    val retryAt: Long = 0,
    val charged: Int? = null,
    val applied: Boolean = false,
    val documentSaved: Boolean = false,
    val deliveryConfirmed: Boolean = false,
    val hasPending: Boolean = false,
    val operation: OperationId? = null,
    val savedVariants: List<OutputEntity> = emptyList(),
    val savedVariantId: String? = null,
    val requestPrompt: String? = null,
    val lastServerState: JobState? = null,
    val terminalError: ApiJobError? = null,
) {
    internal val blocksNewOperation: Boolean get() = hasPending && !deliveryConfirmed
    internal val canChooseTool: Boolean get() = !busy && !blocksNewOperation

    // Single source for the pinned progress banner: derived from ViewModel state, never a local remember.
    internal val inFlight: Boolean get() = busy || (hasPending && result == null && errorCode == null)
}

internal data class StudioConfirmationPreview(
    val hasEnoughCredits: Boolean,
    val quoteMatches: Boolean,
    val remainingCredits: Int?,
    val canPressGenerate: Boolean,
    val canConfirm: Boolean,
)

internal fun studioConfirmationPreview(
    displayedCredits: Int,
    currentCredits: Int?,
    balance: Int?,
    operationEnabled: Boolean,
    killSwitchClear: Boolean,
    dailyCapClear: Boolean,
    rowBlocked: Boolean,
    providerReady: Boolean,
    hasPhoto: Boolean,
    promptValid: Boolean,
    busy: Boolean,
    hasPending: Boolean,
): StudioConfirmationPreview {
    val hasEnoughCredits = displayedCredits > 0 && balance != null && balance >= displayedCredits
    val quoteMatches = currentCredits == displayedCredits
    val remainingCredits = balance
        ?.takeIf { displayedCredits > 0 && it >= displayedCredits }
        ?.minus(displayedCredits)
    val canPressGenerate = displayedCredits > 0 && balance != null && quoteMatches && operationEnabled &&
        killSwitchClear && dailyCapClear && !rowBlocked && providerReady && hasPhoto && promptValid &&
        !busy && !hasPending
    return StudioConfirmationPreview(
        hasEnoughCredits = hasEnoughCredits,
        quoteMatches = quoteMatches,
        remainingCredits = remainingCredits,
        canPressGenerate = canPressGenerate,
        canConfirm = hasEnoughCredits && canPressGenerate,
    )
}

internal fun studioPromptIsValid(operation: OperationId, prompt: String): Boolean =
    prompt.length <= (if (operation == OperationId.GENERATED_SCENE) 2000 else STUDIO_INSTRUCTION_MAX_LENGTH) &&
        (operation != OperationId.GENERATED_SCENE || prompt.isNotBlank())

internal fun studioDocumentWatermarkRequired(serverConfirmedUnlock: Boolean): Boolean =
    WatermarkGate.required(serverConfirmedUnlock)

internal class StudioViewModel(
    context: Context,
    private val api: ApiClient = ApiClient(),
    private val integrity: IntegrityTokenProvider,
) : ViewModel() {
    private val app = context.applicationContext
    private val projects = ProjectRepository(app)
    private val sessionStore = WalletSessionStore(app)
    private val sessionBootstrap = SessionBootstrap(sessionStore, api::getIntegrityChallenge, api::createSession)
    private val store = StudioPendingStore(app)
    private val database = PlinthDatabase.get(app)
    private val mutable = MutableStateFlow(StudioState())
    val state = mutable.asStateFlow()

    // The wallet session is created by the app shell after launch, so the one-shot refresh() can run before it
    // exists. The cached server balance is the same source the Wallet chip reads; follow it live.
    init {
        viewModelScope.launch(Dispatchers.IO) {
            sessionStore.balance.collect { cached ->
                mutable.update { it.copy(balance = cached?.coerceAtLeast(0)) }
            }
        }
    }

    fun refresh() = task {
        enforceRetryHint()
        val pending = store.current()
        val photos = projects.recentPhotos()
        mutable.update {
            it.copy(
                photos = photos,
                // Keep an unfinished job or unsaved result with its source photo.
                photoId = pending?.photoId ?: it.photoId ?: photos.firstOrNull()?.id,
                hasPending = pending != null,
                operation = pending?.operation,
                requestPrompt = pending?.params?.get("prompt")?.jsonPrimitive?.contentOrNull,
            )
        }
        mutable.value.photoId?.let { loadVariants(it) }
        refreshQuote()
        if (pending != null) execute(pending)
    }

    // In-place answer to "an edit is unfinished": run it to its end and keep its result.
    fun finishPending() = task {
        val pending = store.current() ?: return@task
        if (!pending.canReplace) execute(pending)
        val settled = store.current()
        if (settled != null && settled.canReplace) {
            val result = mutable.value.result
            if (result != null && !settled.applied && !settled.terminal) saveVariant(settled, result)
            clearCompletedPending(settled)
        }
        resetAfterPending()
    }

    // In-place answer to "an edit is unfinished": let it end (so no paid job is orphaned), then drop its result.
    fun discardPending() = task {
        val pending = store.current() ?: return@task
        if (!pending.canReplace) execute(pending)
        store.current()?.takeIf { it.canReplace }?.let { clearCompletedPending(it) }
        resetAfterPending()
    }

    private fun resetAfterPending() {
        mutable.update {
            it.copy(result = null, hasPending = false, applied = false, documentSaved = false, charged = null,
                phase = StudioPhase.READY, operation = null, savedVariantId = null,
                deliveryConfirmed = false, lastServerState = null, terminalError = null, requestPrompt = null)
        }
    }

    fun choosePhoto(id: String) = task {
        clearCompletedPending(replaceablePending())
        mutable.update {
            it.copy(photoId = id, applied = false, result = null, savedVariantId = null,
                documentSaved = false, deliveryConfirmed = false, hasPending = false,
                operation = null, charged = null, phase = StudioPhase.READY,
                lastServerState = null, terminalError = null, requestPrompt = null)
        }
        loadVariants(id)
    }

    fun importPhoto(uri: Uri) = task {
        val previous = replaceablePending()
        val photo = PhotoImporter(app, projects).importPhoto(uri)
        clearCompletedPending(previous)
        mutable.update {
            it.copy(photos = projects.recentPhotos(), photoId = photo.id, savedVariants = emptyList(),
                result = null, savedVariantId = null, applied = false, documentSaved = false,
                deliveryConfirmed = false, hasPending = false, operation = null, charged = null,
                phase = StudioPhase.READY, lastServerState = null, terminalError = null,
                requestPrompt = null)
        }
    }

    // The single entry for Recolour, Relight, Scene and Expand. Resuming is only for an unresolved job of the
    // SAME operation; a finished leftover (e.g. an earlier Recolour) must never swallow a new request.
    fun generate(
        operation: OperationId,
        prompt: String,
        displayedCredits: Int,
        canvasAspect: CanvasAspect = CanvasAspect.SQUARE,
    ) {
        val pending = runCatching { store.current() }.getOrNull()
        if (pending != null && !pending.canReplace && pending.operation == operation) retry()
        else start(operation, prompt, displayedCredits, canvasAspect)
    }

    private fun start(
        operation: OperationId,
        prompt: String,
        displayedCredits: Int,
        canvasAspect: CanvasAspect,
    ) = task {
        if (!STUDIO_PROVIDER_RESULTS_READY) throw StudioProblem("PROVIDER_PROTOCOL_UNAVAILABLE")
        val previous = replaceablePending()
        require(operation in REQUEST_OPERATIONS)
        require(displayedCredits > 0)
        val photoId = requireNotNull(mutable.value.photoId)
        require(studioPromptIsValid(operation, prompt))
        val session = sessionStore.current() ?: throw StudioProblem("SESSION_REQUIRED")
        refreshQuote()
        val config = mutable.value.config ?: throw StudioProblem("CONFIG_REQUIRED")
        val quote = config.ops[operation.wireId()] ?: throw StudioProblem("OPERATION_DISABLED")
        if (!quote.enabled || config.killSwitch || config.dailyCapReached) {
            throw StudioProblem("OPERATION_DISABLED")
        }
        if (quote.credits != displayedCredits) throw StudioProblem("PRICE_CHANGED")
        val balance = mutable.value.balance
            ?: throw StudioProblem("BALANCE_UNAVAILABLE", app.getString(R.string.studio_live_balance_unknown))
        if (balance < displayedCredits) {
            throw StudioProblem("INSUFFICIENT_CREDITS")
        }
        val params = buildJsonObject {
            if (prompt.isNotBlank()) put("prompt", prompt.trim())
            if (operation == OperationId.EXPAND) {
                val source = ProjectImageLoader(projects).load(photoId).source
                val (width, height) = uploadImageDimensions(source.width, source.height)
                val expansion = calculateCanvasExpansion(width, height, canvasAspect)
                putJsonArray("canvas_size") {
                    add(expansion.canvasSize.first)
                    add(expansion.canvasSize.second)
                }
                putJsonArray("original_image_size") { add(width); add(height) }
                putJsonArray("original_image_location") {
                    add(expansion.originalImageLocation.first)
                    add(expansion.originalImageLocation.second)
                }
            }
        }
        val pending = StudioPending(
            requestId = UUID.randomUUID().toString(),
            photoId = photoId,
            walletId = session.walletId,
            operation = operation,
            credits = displayedCredits,
            params = params,
        )
        clearCompletedPending(previous)
        store.save(pending)
        mutable.update {
            it.copy(hasPending = true, operation = operation, applied = false, charged = null,
                deliveryConfirmed = false, lastServerState = null, terminalError = null,
            result = null, savedVariantId = null, documentSaved = false, requestPrompt = prompt)
        }
        execute(pending)
    }

    private fun replaceablePending(): StudioPending? = store.current()?.also {
        if (!it.canReplace) throw StudioProblem("JOB_UNRESOLVED")
    }

    private fun clearCompletedPending(pending: StudioPending?) {
        if (pending == null) return
        check(pending.canReplace)
        pending.resultPath?.let { path ->
            val file = File(path)
            if (file.exists() && !file.delete()) throw IOException("RESULT_DELETE_FAILED")
        }
        store.clear()
    }

    fun retry() = task {
        enforceRetryHint()
        val pending = store.current()
        if (pending == null) refreshQuote()
        else execute(pending)
    }

    private fun enforceRetryHint() {
        val remaining = mutable.value.retryAt - System.currentTimeMillis()
        if (remaining > 0) {
            throw StudioProblem("RETRY_LATER", retryAfterSec = ((remaining + 999) / 1000).toInt())
        }
    }

    private suspend fun refreshQuote() {
        mutable.update { it.copy(config = null) }
        val config = value(api.getConfig())
        mutable.update { it.copy(config = config) }
        val session = sessionStore.current() ?: throw StudioProblem("SESSION_REQUIRED")
        val wallet = value(api.getWallet(session.deviceToken))
        sessionStore.updateServerBalance(wallet.balance)
        mutable.update {
            it.copy(config = config, balance = wallet.balance.coerceAtLeast(0), reserved = wallet.reserved)
        }
    }

    private suspend fun execute(saved: StudioPending) {
        var pending = saved
        val session = sessionStore.current() ?: throw StudioProblem("SESSION_REQUIRED")
        if (session.walletId != pending.walletId) throw StudioProblem("WALLET_CHANGED")
        if (pending.terminal) {
            mutable.update { it.copy(phase = StudioPhase.FAILED, hasPending = false) }
            store.clear()
            refreshQuote()
            return
        }
        if (pending.resultPath != null) {
            val decoded = try {
                decodeResult(File(requireNotNull(pending.resultPath)).readBytes())
            } catch (failure: StudioProblem) {
                if (failure.code != "INVALID_RESULT_IMAGE") throw failure
                rejectInvalidResult(pending, pending.charged)
                if (pending.charged != 0) refreshQuote()
                return
            }
            val bitmap = if (pending.operation == OperationId.EXPAND) {
                try {
                    restoreExpandedProduct(decoded, pending)
                } catch (_: ProductPixelRestoreException) {
                    rejectExpandRestoreFailure(pending, pending.charged)
                    if (pending.charged != 0) refreshQuote()
                    return
                }
            } else {
                decoded
            }
            mutable.update {
                it.copy(
                    result = bitmap,
                    applied = pending.applied,
                    savedVariantId = pending.requestId.takeIf { pending.applied },
                    charged = pending.charged,
                )
            }
            settle(pending, session.deviceToken)
            return
        }
        if (pending.jobId == null) {
            if (pending.uploadId == null) {
                mutable.update { it.copy(phase = StudioPhase.UPLOADING) }
                val source = ProjectImageLoader(projects).load(pending.photoId).source
                val (uploadWidth, uploadHeight) = uploadImageDimensions(source.width, source.height)
                val scaled = source.scale(uploadWidth, uploadHeight).toBitmap()
                val bytes = try {
                    ByteArrayOutputStream().use { output ->
                        if (!scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)) {
                            throw IOException("PHOTO_ENCODE_FAILED")
                        }
                        output.toByteArray()
                    }
                } finally {
                    scaled.recycle()
                }
                val upload = value(api.uploadPhoto(bytes, "image/jpeg", session.deviceToken))
                pending = pending.copy(uploadId = upload.uploadId)
                store.save(pending)
            }
            val accepted = submitJobAfterReattestation(
                JobRequest(
                    clientRequestId = pending.requestId,
                    op = pending.operation,
                    uploadIds = listOf(requireNotNull(pending.uploadId)),
                    params = pending.params,
                    estimateCredits = pending.credits,
                ),
                session.deviceToken,
            )
            pending = pending.copy(jobId = accepted.jobId)
            store.save(pending)
            mutable.update { it.copy(phase = StudioPhase.QUEUED, lastServerState = accepted.state) }
        }
        val jobId = requireNotNull(pending.jobId)
        val pollingRecovery = StudioPollingRecovery()
        while (true) {
            val poll = api.pollJob(jobId, session.deviceToken)
            if (poll is ApiResult.Failure && poll.failure is ApiFailure.Network) {
                when (val decision = pollingRecovery.afterNetworkFailure(poll.failure)) {
                    is StudioPollDecision.Retry -> {
                        android.util.Log.w("PlinthStudio", "Poll retry after ${poll.failure.category ?: "network"}: ${poll.failure.message.orEmpty()}")
                        delay(decision.delayMs)
                        continue
                    }
                    is StudioPollDecision.ConnectionLost -> throw StudioProblem(
                        code = "CONNECTION_LOST_WHILE_RUNNING",
                        serverMessage = decision.message,
                    )
                    StudioPollDecision.Continue, StudioPollDecision.TerminalStatus -> error("Unexpected polling decision")
                }
            }
            val status = value(poll)
            pollingRecovery.afterStatus(status.state)
            mutable.update { it.copy(charged = status.charged, lastServerState = status.state) }
            when (status.state) {
                JobState.QUEUED, JobState.RUNNING -> {
                    mutable.update {
                        it.copy(phase = if (status.state == JobState.QUEUED) StudioPhase.QUEUED else StudioPhase.RUNNING)
                    }
                    delay(2000)
                }
                JobState.FAILED, JobState.CANCELLED -> {
                    pending = pending.copy(terminal = true)
                    store.save(pending)
                    mutable.update {
                        it.copy(
                            phase = StudioPhase.FAILED,
                            errorCode = status.error?.code ?: status.state.name,
                            errorMessage = status.error?.message,
                            terminalError = status.error ?: ApiJobError(status.state.name, ""),
                            hasPending = false,
                        )
                    }
                    store.clear()
                    // The status route omits released/reserved; do not invent a refund amount.
                    refreshQuote()
                    return
                }
                JobState.SUCCEEDED -> {
                    mutable.update { it.copy(phase = StudioPhase.DOWNLOADING) }
                    val result = status.result ?: throw StudioProblem("RESULT_NOT_AVAILABLE")
                    val delivered = value(api.downloadResult(jobId, result, session.deviceToken))
                    val decoded = try {
                        decodeResult(delivered.bytes)
                    } catch (failure: StudioProblem) {
                        if (failure.code != "INVALID_RESULT_IMAGE") throw failure
                        rejectInvalidResult(pending, status.charged)
                        if (status.charged != 0) refreshQuote()
                        return
                    }
                    val bitmap = if (pending.operation == OperationId.EXPAND) {
                        try {
                            restoreExpandedProduct(decoded, pending)
                        } catch (_: ProductPixelRestoreException) {
                            rejectExpandRestoreFailure(pending, status.charged)
                            if (status.charged != 0) refreshQuote()
                            return
                        }
                    } else {
                        decoded
                    }
                    val storedBytes = if (pending.operation == OperationId.EXPAND) {
                        ByteArrayOutputStream().use { output ->
                            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                                throw IOException("EXPAND_RESULT_ENCODE_FAILED")
                            }
                            output.toByteArray()
                        }
                    } else {
                        delivered.bytes
                    }
                    val file = File(resultDirectory(), "${pending.requestId}.received")
                    FileOutputStream(file).use { output ->
                        output.write(storedBytes)
                        output.fd.sync()
                    }
                    pending = pending.copy(
                        resultPath = file.path,
                        receipt = DeliveryReceipt(
                            delivered.deliveryAttemptId, delivered.bytes.size.toLong(), delivered.sha256,
                        ),
                    )
                    store.save(pending)
                    mutable.update { it.copy(result = bitmap) }
                    settle(pending, session.deviceToken)
                    return
                }
            }
        }
    }

    private fun restoreExpandedProduct(bitmap: Bitmap, pending: StudioPending): Bitmap {
        if (!bitmap.isMutable) {
            bitmap.recycle()
            throw ProductPixelRestoreException("Could not prepare the expanded canvas")
        }
        try {
            val canvasSize = requiredIntPair(pending.params, "canvas_size")
            val sourceSize = requiredIntPair(pending.params, "original_image_size")
            val location = requiredIntPair(pending.params, "original_image_location")
            if ((bitmap.width to bitmap.height) != canvasSize) {
                throw ProductPixelRestoreException("Expanded image dimensions do not match the requested canvas")
            }

            val source = ProjectImageLoader(projects).load(pending.photoId).source
            val actualSourceSize = uploadImageDimensions(source.width, source.height)
            if (actualSourceSize != sourceSize) {
                throw ProductPixelRestoreException("Original product dimensions no longer match the request")
            }
            val originalBitmap = source.scale(sourceSize.first, sourceSize.second).toBitmap()
            try {
                restoreOriginalPixels(BitmapPixelRows(bitmap), BitmapPixelRows(originalBitmap), location)
            } finally {
                originalBitmap.recycle()
            }
            return bitmap
        } catch (failure: ProductPixelRestoreException) {
            bitmap.recycle()
            throw failure
        } catch (failure: IOException) {
            bitmap.recycle()
            throw ProductPixelRestoreException("Original product pixels could not be restored")
        } catch (failure: IllegalArgumentException) {
            bitmap.recycle()
            throw ProductPixelRestoreException("Original product pixels could not be verified")
        } catch (failure: RuntimeException) {
            bitmap.recycle()
            throw ProductPixelRestoreException("Original product pixels could not be verified")
        }
    }

    private fun requiredIntPair(
        params: JsonObject,
        name: String,
    ): Pair<Int, Int> {
        val values = params[name] as? JsonArray
            ?: throw ProductPixelRestoreException("Missing expanded image geometry")
        if (values.size != 2) throw ProductPixelRestoreException("Invalid expanded image geometry")
        val first = (values[0] as? JsonPrimitive)?.intOrNull
            ?: throw ProductPixelRestoreException("Invalid expanded image geometry")
        val second = (values[1] as? JsonPrimitive)?.intOrNull
            ?: throw ProductPixelRestoreException("Invalid expanded image geometry")
        return first to second
    }

    private suspend fun rejectExpandRestoreFailure(pending: StudioPending, charged: Int?) {
        val terminal = pending.copy(charged = charged, terminal = true)
        store.save(terminal)
        clearCompletedPending(terminal)
        mutable.update {
            it.copy(
                result = null,
                hasPending = false,
                applied = false,
                savedVariantId = null,
                deliveryConfirmed = false,
                charged = charged,
                phase = StudioPhase.FAILED,
                errorCode = "EXPAND_PRODUCT_RESTORE_FAILED",
                errorMessage = app.getString(R.string.studio_live_error_generic),
                lastServerState = JobState.SUCCEEDED,
            )
        }
    }

    private suspend fun settle(pending: StudioPending, token: String) {
        if (pending.charged != null && pending.charged != pending.credits) {
            throw StudioProblem("CHARGE_MISMATCH")
        }
        if (pending.charged == null) {
            mutable.update { it.copy(phase = StudioPhase.CONFIRMING) }
            val receipt = requireNotNull(pending.receipt)
            val settled = value(api.submitDeliveryReceipt(requireNotNull(pending.jobId), receipt, token))
            if (settled.state != "SETTLED") throw StudioProblem("RECEIPT_NOT_SETTLED")
            store.save(pending.copy(charged = settled.charged))
            mutable.update { it.copy(charged = settled.charged) }
            if (settled.charged != pending.credits) throw StudioProblem("CHARGE_MISMATCH")
        }
        mutable.update {
            it.copy(phase = StudioPhase.DONE, charged = pending.charged ?: it.charged, deliveryConfirmed = true)
        }
        refreshQuote()
    }

    fun applyResult() = task {
        val pending = store.current() ?: throw StudioProblem("RESULT_NOT_AVAILABLE")
        if (pending.charged == null) throw StudioProblem("RECEIPT_PENDING")
        if (pending.charged != pending.credits) throw StudioProblem("CHARGE_MISMATCH")
        val input = mutable.value.result ?: throw StudioProblem("RESULT_NOT_AVAILABLE")
        saveVariant(pending, input)
        mutable.update { it.copy(applied = true, savedVariantId = pending.requestId) }
    }

    private suspend fun rejectInvalidResult(pending: StudioPending, charged: Int?) {
        val terminal = pending.copy(charged = charged, terminal = true)
        store.save(terminal)
        clearCompletedPending(terminal)
        val message = when {
            charged == 0 -> app.getString(R.string.studio_live_invalid_result_not_charged)
            charged != null && charged > 0 -> app.getString(
                R.string.studio_live_invalid_result_charged,
                app.resources.getQuantityString(
                    R.plurals.studio_live_credit_amount,
                    charged,
                    charged,
                ),
            )
            else -> app.getString(R.string.studio_live_invalid_result_charge_unknown)
        }
        mutable.update {
            it.copy(
                result = null,
                hasPending = false,
                applied = false,
                savedVariantId = null,
                deliveryConfirmed = false,
                charged = charged,
                phase = StudioPhase.FAILED,
                errorCode = "INVALID_RESULT_IMAGE",
                errorMessage = message,
                lastServerState = JobState.SUCCEEDED,
            )
        }
    }

    private suspend fun saveVariant(pending: StudioPending, input: Bitmap) {
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IOException("RESULT_COPY_FAILED")
        try {
            val png = ByteArrayOutputStream().use { stream ->
                if (!output.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IOException("RESULT_SAVE_FAILED")
                }
                addStudioPngProvenance(stream.toByteArray())
            }
            val file = File(resultDirectory(), "${pending.requestId}.png")
            FileOutputStream(file).use { stream ->
                stream.write(png)
                stream.fd.sync()
            }
            database.withTransaction {
                val item = database.itemDao().byId(pending.photoId)
                    ?: throw StudioProblem("ITEM_NOT_AVAILABLE")
                database.outputDao().insert(
                    OutputEntity(
                        id = pending.requestId,
                        itemId = item.id,
                        path = file.path,
                        width = output.width,
                        height = output.height,
                        bytes = file.length(),
                        format = "png",
                        provenance = Provenance.ENHANCED,
                        operationsJson = JSONObject()
                            .put("generated", true)
                            .put("operation", pending.operation.wireId())
                            .put("jobId", pending.jobId)
                            .put("sourceItemId", pending.photoId)
                            .toString(),
                        marketplaceId = item.marketplaceId,
                        role = SlotRole.EXTRA,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
            store.save(pending.copy(applied = true))
            loadVariants(pending.photoId)
        } finally {
            output.recycle()
        }
    }

    fun discard() = task {
        val current = mutable.value
        current.savedVariantId?.let { id ->
            val output = current.savedVariants.firstOrNull { it.id == id }
                ?: throw StudioProblem("RESULT_NOT_AVAILABLE")
            val file = File(output.path)
            if (file.canonicalFile.parentFile != resultDirectory().canonicalFile) {
                throw IOException("SAVED_RESULT_PATH_INVALID")
            }
            database.withTransaction {
                if (database.outputDao().deleteById(id) != 1) {
                    throw StudioProblem("RESULT_NOT_AVAILABLE")
                }
            }
            if (file.exists() && !file.delete()) throw IOException("RESULT_DELETE_FAILED")
        }
        val pending = store.current()
        if (pending != null) {
            if (pending.charged == null && !pending.terminal) throw StudioProblem("JOB_UNRESOLVED")
            pending.resultPath?.let { path ->
                val file = File(path)
                if (file.exists() && !file.delete()) throw IOException("RESULT_DELETE_FAILED")
            }
            store.clear()
        }
        mutable.update {
            it.copy(result = null, hasPending = false, applied = false, documentSaved = false, charged = null,
                phase = StudioPhase.READY, operation = null, savedVariantId = null,
                deliveryConfirmed = false, lastServerState = null, terminalError = null, requestPrompt = null)
        }
        current.photoId?.let { loadVariants(it) }
    }

    fun saveDocument(uri: Uri) = task {
        val pending = store.current()
        val file = if (pending != null) {
            if (!pending.applied) throw StudioProblem("APPLY_RESULT_FIRST")
            File(resultDirectory(), "${pending.requestId}.png")
        } else {
            val output = mutable.value.savedVariants.firstOrNull { it.id == mutable.value.savedVariantId }
                ?: throw StudioProblem("RESULT_NOT_AVAILABLE")
            File(output.path)
        }
        if (!file.isFile) throw StudioProblem("SAVED_RESULT_MISSING")
        val watermarkRequired = studioDocumentWatermarkRequired(ServerUnlock.fetch(app))
        val output = app.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("DOCUMENT_NOT_WRITABLE")
        output.use { stream ->
            if (watermarkRequired) writeWatermarkedDocument(file, stream)
            else file.inputStream().use { it.copyTo(stream) }
        }
        mutable.update { it.copy(documentSaved = true) }
    }

    private fun writeWatermarkedDocument(file: File, output: java.io.OutputStream) {
        output.write(WatermarkGate.applyToEncoded(file.readBytes()))
    }

    private suspend fun loadVariants(photoId: String) {
        val directory = resultDirectory().canonicalFile
        val outputs = database.outputDao().byItem(photoId).filter {
            File(it.path).canonicalFile.parentFile == directory &&
                it.operationsJson.contains("\"generated\":true")
        }
        mutable.update { it.copy(savedVariants = outputs) }
    }

    fun openVariant(id: String) = task {
        val previous = replaceablePending()
        val output = mutable.value.savedVariants.firstOrNull { it.id == id }
            ?: throw StudioProblem("RESULT_NOT_AVAILABLE")
        val bitmap = decodeResult(File(output.path).readBytes())
        val operationId = JSONObject(output.operationsJson).getString("operation")
        val operation = REQUEST_OPERATIONS.firstOrNull { it.wireId() == operationId }
            ?: throw StudioProblem("RESULT_NOT_AVAILABLE")
        clearCompletedPending(previous)
        mutable.update {
            it.copy(result = bitmap, savedVariantId = output.id, applied = true, charged = null,
                documentSaved = false, phase = StudioPhase.DONE, deliveryConfirmed = true,
                hasPending = false, operation = operation, requestPrompt = null)
        }
    }

    private fun resultDirectory(): File = File(app.filesDir, PROJECT_OUTPUT_DIRECTORY).also {
        if (!it.isDirectory && !it.mkdirs()) throw IOException("RESULT_DIRECTORY_FAILED")
    }

    private fun decodeResult(bytes: ByteArray): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth < 256 || bounds.outHeight < 256 ||
            bounds.outWidth.toLong() * bounds.outHeight > 25_000_000) {
            throw StudioProblem("INVALID_RESULT_IMAGE")
        }
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inMutable = true
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
            ?: throw StudioProblem("INVALID_RESULT_IMAGE")
    }

    private suspend fun <T> value(result: ApiResult<T>): T = when (result) {
        is ApiResult.Success -> result.value
        is ApiResult.Failure -> {
            val unknown = result.failure as? ApiFailure.Unknown
            val code = result.envelope?.code ?: unknown?.code
                ?: if (result.failure is ApiFailure.Network) "NETWORK_FAILURE"
                else result.failure.javaClass.simpleName
            throw StudioProblem(
                code, result.envelope?.message ?: unknown?.message,
                result.envelope?.retryAfterSec ?: unknown?.retryAfterSec,
            )
        }
    }

    /** Only the job-spend call may be retried: uploads and all read/settlement calls are never replayed. */
    private suspend fun submitJobAfterReattestation(request: JobRequest, token: String): JobAccepted {
        val first = api.submitJob(request, token)
        if (first !is ApiResult.Failure || first.failure != ApiFailure.VersionTooOld) return value(first)
        val renewed = sessionBootstrap.connect(
            appVersion = BuildConfig.VERSION_NAME,
            locale = app.resources.configuration.locales[0].toLanguageTag(),
            integrity = integrity,
            forceReattestation = true,
        )
        val session = (renewed as? SessionConnectionState.Ready)?.session
            ?: throw StudioProblem("UPDATE_REQUIRED")
        return value(api.submitJob(request, session.deviceToken))
    }

    private fun task(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update {
            it.copy(busy = true, errorCode = null, errorMessage = null, retryAfterSec = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: StudioProblem) {
                mutable.update {
                    it.copy(
                        phase = StudioPhase.FAILED,
                        errorCode = failure.code,
                        errorMessage = failure.serverMessage,
                        retryAfterSec = failure.retryAfterSec,
                        retryAt = System.currentTimeMillis() + (failure.retryAfterSec?.toLong() ?: 0L) * 1000,
                    )
                }
            } catch (failure: IOException) {
                localFailure("LOCAL_STORAGE_OR_NETWORK", failure)
            } catch (failure: SecurityException) {
                localFailure("PHOTO_ACCESS_DENIED", failure)
            } catch (failure: SerializationException) {
                localFailure("INVALID_PENDING_RECORD", failure)
            } catch (failure: IllegalArgumentException) {
                localFailure("INVALID_INPUT_OR_RECORD", failure)
            } catch (failure: org.json.JSONException) {
                localFailure("INVALID_PROJECT_RECORD", failure)
            } catch (failure: android.database.sqlite.SQLiteException) {
                localFailure("PROJECT_WRITE_FAILED", failure)
            } catch (failure: Exception) {
                localFailure("UNEXPECTED_FAILURE", failure)
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    private fun localFailure(code: String, failure: Exception) {
        android.util.Log.e("PlinthStudio", code, failure)
        mutable.update { it.copy(phase = StudioPhase.FAILED, errorCode = code) }
    }

    fun dismissError() = mutable.update { it.copy(errorCode = null, errorMessage = null, retryAfterSec = null) }

    private class StudioProblem(
        val code: String,
        val serverMessage: String? = null,
        val retryAfterSec: Int? = null,
    ) : Exception(code)

    companion object {
        // Upscale and Erase are not here: the server has no provider mapping for them (no mask, over the price cap).
        val REQUEST_OPERATIONS = setOf(
            OperationId.GENERATED_SCENE, OperationId.RELIGHT, OperationId.RECOLOUR, OperationId.EXPAND,
        )
    }
}

/**
 * The one place that decides which integrity provider a `StudioViewModel` attests with, so a
 * `426 UPDATE_REQUIRED` re-attestation and the first session cannot disagree about the build.
 */
internal fun studioViewModelFactory(context: Context): ViewModelProvider.Factory {
    val appContext = context.applicationContext
    val integrity: IntegrityTokenProvider = if (BuildConfig.DEBUG) {
        DevIntegrityTokenProvider()
    } else {
        PlayIntegrityTokenProvider(appContext, BuildConfig.INTEGRITY_CLOUD_PROJECT_NUMBER)
    }
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StudioViewModel(appContext, integrity = integrity) as T
    }
}

internal fun OperationId.wireId(): String = when (this) {
    OperationId.RECOGNISE -> "O-01"
    OperationId.CUTOUT -> "O-02"
    OperationId.GENERATED_SCENE -> "O-07"
    OperationId.RELIGHT -> "O-08"
    OperationId.EXPAND -> "O-09"
    OperationId.RECOLOUR -> "O-10"
    OperationId.UPSCALE -> "O-11"
    OperationId.ERASE -> "O-12"
    OperationId.GHOST_MANNEQUIN -> "O-13"
    OperationId.VIRTUAL_MODEL -> "O-14"
    OperationId.LISTING_TEXT -> "O-15"
}

private class BitmapPixelRows(private val bitmap: Bitmap) : MutablePixelRows {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height

    override fun readRow(y: Int): IntArray = IntArray(width).also {
        bitmap.getPixels(it, 0, width, 0, y, width, 1)
    }

    override fun writeRow(y: Int, pixels: IntArray) {
        bitmap.setPixels(pixels, 0, width, 0, y, width, 1)
    }
}

/**
 * Keeps a paid, server-owned job recoverable through transient poll failures. Two minutes is long
 * enough to cross a mobile network handoff without keeping a visibly disconnected screen forever.
 */
internal class StudioPollingRecovery {
    var consecutiveNetworkFailureMs: Long = 0
        private set

    fun afterNetworkFailure(failure: ApiFailure.Network): StudioPollDecision {
        val delayMs = (POLL_INTERVAL_MS shl minOf(consecutiveNetworkFailureMs / POLL_INTERVAL_MS, 2L).toInt())
            .coerceAtMost(MAX_NETWORK_RETRY_DELAY_MS)
        consecutiveNetworkFailureMs += delayMs
        return if (consecutiveNetworkFailureMs >= MAX_CONSECUTIVE_NETWORK_FAILURE_MS) {
            StudioPollDecision.ConnectionLost(
                "The connection was lost while the edit was still running. Your result may still be available when you return.",
            )
        } else {
            StudioPollDecision.Retry(delayMs)
        }
    }

    fun afterStatus(state: JobState): StudioPollDecision {
        consecutiveNetworkFailureMs = 0
        return when (state) {
            JobState.QUEUED, JobState.RUNNING, JobState.SUCCEEDED -> StudioPollDecision.Continue
            JobState.FAILED, JobState.CANCELLED -> StudioPollDecision.TerminalStatus
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
        const val MAX_NETWORK_RETRY_DELAY_MS = 8_000L
        const val MAX_CONSECUTIVE_NETWORK_FAILURE_MS = 120_000L
    }
}

internal sealed interface StudioPollDecision {
    data class Retry(val delayMs: Long) : StudioPollDecision
    data class ConnectionLost(val message: String) : StudioPollDecision
    data object Continue : StudioPollDecision
    data object TerminalStatus : StudioPollDecision
}

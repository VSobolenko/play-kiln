package com.gexetr.plinthproductphotos.ui.cutout

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.res.pluralStringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.wallet.JobMoneyOutcome
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.components.rememberPhotoSourceFlow
import com.gexetr.plinthproductphotos.domain.wallet.JobMoneyPresentation
import com.gexetr.plinthproductphotos.ui.editor.CutoutMaskEditorScreen
import com.gexetr.plinthproductphotos.ui.editor.CutoutActionGlyph
import com.gexetr.plinthproductphotos.ui.editor.CutoutActionGlyphIcon
import com.gexetr.plinthproductphotos.ui.editor.CutoutMaskPaintCanvas
import com.gexetr.plinthproductphotos.ui.editor.CutoutSelectionSurface
import com.gexetr.plinthproductphotos.ui.editor.EditorRoute

@Composable
fun CutoutRoute(
    onOpenWallet: () -> Unit,
    modifier: Modifier = Modifier,
    initialPhotoId: String? = null,
    viewModel: CutoutViewModel = viewModel(
        factory = CutoutViewModel.factory(androidx.compose.ui.platform.LocalContext.current),
    ),
) {
    remember(viewModel, initialPhotoId) { viewModel.requestPhoto(initialPhotoId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCardEditor by rememberSaveable { mutableStateOf(false) }
    val selectedPhotoId = state.selectedPhotoId
    val showPhotoSourceSheet = rememberPhotoSourceFlow(viewModel::select)
    if (showCardEditor && selectedPhotoId != null && state.maskAlpha != null) {
        EditorRoute(
            photoId = selectedPhotoId,
            onBack = { showCardEditor = false },
            initialMaskAlpha = state.maskAlpha,
            modifier = modifier,
        )
    } else {
        CutoutScreen(
            state = state,
            onChoosePhoto = showPhotoSourceSheet,
            onRefreshWallet = viewModel::refreshCostAndBalance,
            onStartCutout = viewModel::startCutout,
            onSetBrushMode = viewModel::setBrushMode,
            onBrushStroke = viewModel::applyBrushStroke,
            onUndo = viewModel::undoBrushStroke,
            onRedo = viewModel::redoBrushStroke,
            onDismissWallet = viewModel::dismissWalletShortfall,
            onSetSelectionSeed = viewModel::setSelectionSeed,
            onSelectAll = viewModel::selectAll,
            onResetSelection = viewModel::resetSelection,
            onSetBrushSize = viewModel::setBrushSize,
            onResetMask = viewModel::resetMask,
            onUseThis = { showCardEditor = true },
            onOpenWallet = onOpenWallet,
            onOpenErase = viewModel::openErase,
            onCloseErase = viewModel::closeErase,
            onEraseStroke = viewModel::applyEraseStroke,
            onEraseUndo = viewModel::undoErase,
            onEraseRedo = viewModel::redoErase,
            onEraseClear = viewModel::clearEraseRegion,
            onStartErase = viewModel::startErase,
            modifier = modifier,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CutoutScreen(
    state: CutoutUiState,
    onChoosePhoto: () -> Unit,
    onRefreshWallet: () -> Unit,
    onStartCutout: () -> Unit,
    onSetBrushMode: (MaskBrushMode) -> Unit,
    onBrushStroke: (List<Pair<Float, Float>>) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDismissWallet: () -> Unit,
    modifier: Modifier = Modifier,
    onSetSelectionSeed: (Float, Float) -> Unit = { _, _ -> },
    onSelectAll: () -> Unit = {},
    onResetSelection: () -> Unit = {},
    onSetBrushSize: (Float) -> Unit = {},
    onResetMask: () -> Unit = {},
    onUseThis: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenErase: () -> Unit = {},
    onCloseErase: () -> Unit = {},
    onEraseStroke: (List<Pair<Float, Float>>) -> Unit = {},
    onEraseUndo: () -> Unit = {},
    onEraseRedo: () -> Unit = {},
    onEraseClear: () -> Unit = {},
    onStartErase: () -> Unit = {},
) {
    var showMaskEditor by rememberSaveable(state.selectedPhotoId) { mutableStateOf(false) }
    val source = state.sourceBitmap
    if (state.eraseActive && source != null) {
        EraseEditor(
            state = state,
            source = source,
            onBack = onCloseErase,
            onSetBrushMode = onSetBrushMode,
            onSetBrushSize = onSetBrushSize,
            onStroke = onEraseStroke,
            onUndo = onEraseUndo,
            onRedo = onEraseRedo,
            onClear = onEraseClear,
            onErase = onStartErase,
            modifier = modifier,
        )
        return
    }
    if (showMaskEditor && source != null) {
        CutoutMaskEditorScreen(
            source = source,
            overlay = state.maskOverlay,
            brushMode = state.brushMode,
            brushSize = state.brushSize,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            isSaving = state.isSavingMask,
            onBack = { showMaskEditor = false },
            onBrushModeChange = onSetBrushMode,
            onBrushSizeChange = onSetBrushSize,
            onBrushStroke = onBrushStroke,
            onUndo = onUndo,
            onRedo = onRedo,
            onSelectAll = onSelectAll,
            onResetSelection = onResetMask,
        )
        return
    }
    val hasTerminalMoneyOutcome = state.terminalJobStatus != null ||
        state.terminalWalletRefresh != null &&
        (state.notice == CutoutNotice.GUARD_REJECTED || state.notice == CutoutNotice.JOB_FAILED)
    val terminalRefreshMessage = when {
        hasTerminalMoneyOutcome && state.terminalWalletRefresh == TerminalWalletRefresh.REFRESHING ->
            stringResource(R.string.cutout_request_refreshing)
        hasTerminalMoneyOutcome && state.terminalWalletRefresh == TerminalWalletRefresh.FAILED ->
            stringResource(R.string.cutout_request_refresh_failed)
        else -> null
    }
    val status = if (state.isSavingMask) {
        stringResource(R.string.cutout_mask_saving)
    } else terminalRefreshMessage ?: when (state.notice) {
        CutoutNotice.NONE -> when (state.phase) {
            CutoutPhase.EMPTY -> stringResource(R.string.cutout_pick_prompt)
            CutoutPhase.CHECKING_COST -> stringResource(R.string.cutout_quote_loading)
            CutoutPhase.READY -> stringResource(R.string.cutout_ready)
            CutoutPhase.UPLOADING -> stringResource(R.string.cutout_uploading)
            CutoutPhase.QUEUED -> stringResource(R.string.cutout_queued)
            CutoutPhase.RUNNING -> if (state.progress == null) {
                stringResource(R.string.cutout_running)
            } else {
                stringResource(R.string.cutout_progress, state.progress)
            }
            CutoutPhase.DOWNLOADING -> stringResource(R.string.cutout_downloading)
            CutoutPhase.SAVED -> stringResource(R.string.cutout_result_saved)
            CutoutPhase.FAILED -> stringResource(R.string.cutout_cutout_failed)
            CutoutPhase.RECEIPT_PENDING -> stringResource(R.string.cutout_receipt_pending)
        }
        CutoutNotice.SESSION_REQUIRED -> stringResource(R.string.cutout_session_required)
        CutoutNotice.DEVICE_BLOCKED -> stringResource(R.string.cutout_device_blocked)
        CutoutNotice.INSUFFICIENT_CREDITS -> {
            if (state.walletShortfall == null) stringResource(R.string.cutout_wallet_unavailable)
            else stringResource(R.string.cutout_wallet_top_up)
        }
        CutoutNotice.PRICE_CHANGED -> stringResource(R.string.cutout_price_changed)
        CutoutNotice.RATE_LIMITED -> stringResource(R.string.cutout_rate_limited)
        CutoutNotice.CAP_REACHED -> stringResource(R.string.cutout_cap_reached)
        CutoutNotice.KILL_SWITCH -> stringResource(R.string.cutout_kill_switch)
        CutoutNotice.SERVICE_CLOSED -> stringResource(R.string.cutout_service_closed)
        CutoutNotice.VERSION_TOO_OLD -> stringResource(R.string.cutout_version_too_old)
        CutoutNotice.TOO_LARGE -> stringResource(R.string.cutout_too_large)
        CutoutNotice.UNSUPPORTED_MEDIA -> stringResource(R.string.cutout_unsupported_media)
        CutoutNotice.NOT_CONFIGURED -> stringResource(R.string.cutout_not_configured)
        CutoutNotice.JOB_FAILED -> stringResource(R.string.cutout_job_failed)
        CutoutNotice.MODERATION_BLOCKED -> stringResource(R.string.cutout_moderation_blocked)
        CutoutNotice.GUARD_REJECTED -> stringResource(R.string.cutout_guard_rejected)
        CutoutNotice.RESULT_EXPIRED -> stringResource(R.string.cutout_result_expired)
        CutoutNotice.INTEGRITY_FAILURE -> stringResource(R.string.cutout_integrity_failure)
        CutoutNotice.NETWORK_FAILURE -> stringResource(R.string.cutout_network_failure)
        CutoutNotice.UNKNOWN_FAILURE -> stringResource(R.string.cutout_unknown_failure)
        CutoutNotice.MASK_NOT_FOUND -> stringResource(R.string.cutout_mask_not_found)
        CutoutNotice.SELECTION_NOT_FOUND -> stringResource(R.string.cutout_selection_not_found)
        CutoutNotice.RECEIPT_PENDING -> stringResource(R.string.cutout_receipt_pending)
        CutoutNotice.OPERATION_DISABLED -> stringResource(R.string.cutout_cutout_unavailable)
        CutoutNotice.PHOTO_IMPORT_FAILED -> stringResource(R.string.cutout_photo_import_failed)
        CutoutNotice.ERASE_MASK_EMPTY -> stringResource(R.string.cutout_erase_mask_empty)
        CutoutNotice.ERASE_VERIFY_FAILED -> stringResource(R.string.cutout_erase_verify_failed)
        CutoutNotice.ERASE_DONE -> stringResource(R.string.cutout_erase_done)
    }
    val canSubmit = state.phase == CutoutPhase.READY && state.selectionConfirmed && !state.isBusy &&
        state.cost != null && state.balance != null &&
        (state.notice == CutoutNotice.NONE || state.notice == CutoutNotice.ERASE_DONE)
    val imageProgressStatus = when (state.phase) {
        CutoutPhase.UPLOADING -> stringResource(R.string.cutout_uploading)
        CutoutPhase.QUEUED -> stringResource(R.string.cutout_queued)
        CutoutPhase.RUNNING -> if (state.progress == null) {
            stringResource(R.string.cutout_running)
        } else {
            stringResource(R.string.cutout_progress, state.progress.coerceIn(0, 100))
        }
        CutoutPhase.DOWNLOADING -> stringResource(R.string.cutout_downloading)
        else -> null
    }
    val imageProgress = if (state.phase == CutoutPhase.RUNNING) {
        state.progress?.coerceIn(0, 100)?.div(100f)
    } else {
        null
    }
    val showQuotedCost = state.resultJobId == null ||
        state.phase == CutoutPhase.READY ||
        imageProgressStatus != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, state.isBusy, state.phase) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                !state.isBusy && state.phase != CutoutPhase.SAVED
            ) {
                onRefreshWallet()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cutout_title)) },
                actions = {
                    state.balance?.let { balance ->
                        Text(
                            stringResource(R.string.cutout_need_credits_balance, balance),
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (source != null && state.resultBitmap == null) {
                Surface(modifier = Modifier.navigationBarsPadding(), shadowElevation = 6.dp) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                    OutlinedButton(
                        onClick = onOpenErase,
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.cutout_erase_open))
                    }
                    Button(
                        onClick = onStartCutout,
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        val cost = state.cost
                        Text(
                            if (cost != null && showQuotedCost) {
                                pluralStringResource(R.plurals.cutout_start_cost_label, cost, cost)
                            } else {
                                stringResource(R.string.cutout_start)
                            },
                        )
                    }
                    }
                }
            }
        },
    ) {
        when {
            source == null -> Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(status, style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = onChoosePhoto,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.cutout_choose_photo))
                }
            }
            state.resultBitmap != null -> Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CutoutReviewPreview(
                    bitmap = state.resultBitmap,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                    if (state.phase == CutoutPhase.SAVED) {
                        Text(
                            pluralStringResource(
                                R.plurals.cutout_saved_mask_plural,
                                state.maskRevision,
                                state.maskRevision,
                            ),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onOpenErase,
                        enabled = state.phase == CutoutPhase.SAVED && !state.isBusy,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.cutout_erase_open))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showMaskEditor = true },
                        enabled = state.phase == CutoutPhase.SAVED && !state.isBusy,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.cutout_edit_edges))
                    }
                    Button(
                        onClick = onUseThis,
                        enabled = state.phase == CutoutPhase.SAVED && !state.isBusy &&
                            state.selectedPhotoId != null && state.maskAlpha != null,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.cutout_use_this))
                    }
                }
            }
            else -> {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val availableHeight = maxHeight
                    val selectionPrompt = if (state.selectionConfirmed) {
                        if (state.selectAll) stringResource(R.string.cutout_selection_all_selected)
                        else stringResource(R.string.cutout_selection_point_selected)
                    } else {
                        stringResource(R.string.cutout_select_object_hint)
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CutoutSelectionSurface(
                            source = source,
                            overlay = null,
                            selectionSeed = state.selectionSeed,
                            selectAll = state.selectAll,
                            enabled = !state.isBusy,
                            onTap = onSetSelectionSeed,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                                .heightIn(min = availableHeight * 0.55f),
                        )
                        Text(
                            selectionPrompt,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CutoutSelectionActionChip(
                                label = R.string.cutout_need_action_all,
                                description = R.string.cutout_select_all,
                                glyph = CutoutActionGlyph.SELECT_ALL,
                                enabled = !state.isBusy,
                                onClick = onSelectAll,
                                modifier = Modifier.weight(1f),
                            )
                            CutoutSelectionActionChip(
                                label = R.string.cutout_need_action_reset,
                                description = R.string.cutout_reset_selection,
                                glyph = CutoutActionGlyph.RESET,
                                enabled = !state.isBusy && state.selectionConfirmed,
                                onClick = onResetSelection,
                                modifier = Modifier.weight(1f),
                            )
                            CutoutSelectionActionChip(
                                label = R.string.cutout_need_action_photo,
                                description = R.string.cutout_change_photo,
                                glyph = CutoutActionGlyph.PHOTO,
                                enabled = !state.isBusy,
                                onClick = onChoosePhoto,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (imageProgressStatus != null) {
                            if (imageProgress == null) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(
                                    progress = { imageProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        val terminalStatus = state.terminalJobStatus
                        if (terminalStatus != null &&
                            state.terminalWalletRefresh == TerminalWalletRefresh.REFRESHED
                        ) {
                            when (JobMoneyPresentation.forJob(terminalStatus, null)) {
                                is JobMoneyOutcome.Released ->
                                    Text(stringResource(R.string.cutout_cutout_not_completed))
                                is JobMoneyOutcome.ReleaseUnconfirmed ->
                                    Text(stringResource(R.string.cutout_delivery_unconfirmed))
                                is JobMoneyOutcome.Settled,
                                JobMoneyOutcome.AwaitingReceipt,
                                null -> Unit
                            }
                        }
                        if (!state.isBusy && (
                                state.notice == CutoutNotice.NETWORK_FAILURE ||
                                    state.notice == CutoutNotice.SESSION_REQUIRED
                                )
                        ) {
                            TextButton(
                                onClick = onRefreshWallet,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.cutout_retry))
                            }
                        }
                    }
                }
            }
        }
    }
    if (state.walletShortfall != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissWallet,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.cutout_wallet_title), style = MaterialTheme.typography.titleLarge)
                val balance = state.balance
                val shortfall = state.walletShortfall
                val cost = state.cost ?: if (balance != null && shortfall != null) {
                    balance + shortfall
                } else {
                    null
                }
                if (balance != null && cost != null) {
                    Text(
                        stringResource(R.string.cutout_need_summary, cost, balance),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = {
                        onDismissWallet()
                        onOpenWallet()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.cutout_need_action_add_credits))
                }
                TextButton(
                    onClick = onDismissWallet,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.cutout_need_action_later))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EraseEditor(
    state: CutoutUiState,
    source: Bitmap,
    onBack: () -> Unit,
    onSetBrushMode: (MaskBrushMode) -> Unit,
    onSetBrushSize: (Float) -> Unit,
    onStroke: (List<Pair<Float, Float>>) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (state.notice) {
        CutoutNotice.NONE -> null
        CutoutNotice.ERASE_MASK_EMPTY -> stringResource(R.string.cutout_erase_mask_empty)
        CutoutNotice.ERASE_VERIFY_FAILED -> stringResource(R.string.cutout_erase_verify_failed)
        CutoutNotice.OPERATION_DISABLED -> stringResource(R.string.cutout_cutout_unavailable)
        CutoutNotice.INSUFFICIENT_CREDITS -> stringResource(R.string.cutout_wallet_top_up)
        CutoutNotice.SESSION_REQUIRED -> stringResource(R.string.cutout_session_required)
        CutoutNotice.NETWORK_FAILURE -> stringResource(R.string.cutout_network_failure)
        CutoutNotice.RATE_LIMITED -> stringResource(R.string.cutout_rate_limited)
        CutoutNotice.MODERATION_BLOCKED -> stringResource(R.string.cutout_moderation_blocked)
        CutoutNotice.TOO_LARGE -> stringResource(R.string.cutout_too_large)
        else -> stringResource(R.string.cutout_unknown_failure)
    }
    val progress = when (state.phase) {
        CutoutPhase.UPLOADING -> stringResource(R.string.cutout_uploading)
        CutoutPhase.QUEUED -> stringResource(R.string.cutout_queued)
        CutoutPhase.RUNNING -> stringResource(R.string.cutout_running)
        CutoutPhase.DOWNLOADING -> stringResource(R.string.cutout_downloading)
        else -> null
    }.takeIf { state.isBusy }
    val cost = state.eraseCost
    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cutout_erase_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        enabled = !state.isBusy,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.plinth_back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.navigationBarsPadding(), shadowElevation = 6.dp) {
                Button(
                    onClick = onErase,
                    enabled = !state.isBusy && state.erasePainted && state.notice != CutoutNotice.OPERATION_DISABLED,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .heightIn(min = 52.dp),
                ) {
                    Text(
                        if (cost != null) {
                            pluralStringResource(R.plurals.cutout_erase_action_cost, cost, cost)
                        } else {
                            stringResource(R.string.cutout_erase_action)
                        },
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.cutout_erase_explain),
                style = MaterialTheme.typography.bodySmall,
            )
            CutoutMaskPaintCanvas(
                source = source,
                overlay = state.eraseOverlay,
                brushMode = state.brushMode,
                enabled = !state.isBusy,
                onBrushStroke = onStroke,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Text(
                if (state.erasePainted) {
                    stringResource(R.string.cutout_erase_region_marked)
                } else {
                    stringResource(R.string.cutout_erase_region_empty)
                },
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.brushMode == MaskBrushMode.ADD,
                    enabled = !state.isBusy,
                    onClick = { onSetBrushMode(MaskBrushMode.ADD) },
                    label = { Text(stringResource(R.string.cutout_erase_paint)) },
                    leadingIcon = { CutoutActionGlyphIcon(CutoutActionGlyph.ADD) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                FilterChip(
                    selected = state.brushMode == MaskBrushMode.REMOVE,
                    enabled = !state.isBusy,
                    onClick = { onSetBrushMode(MaskBrushMode.REMOVE) },
                    label = { Text(stringResource(R.string.cutout_erase_unpaint)) },
                    leadingIcon = { CutoutActionGlyphIcon(CutoutActionGlyph.REMOVE) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CutoutSelectionActionChip(
                    label = R.string.cutout_erase_undo,
                    description = R.string.cutout_undo,
                    glyph = CutoutActionGlyph.UNDO,
                    enabled = !state.isBusy && state.eraseCanUndo,
                    onClick = onUndo,
                )
                CutoutSelectionActionChip(
                    label = R.string.cutout_erase_redo,
                    description = R.string.cutout_redo,
                    glyph = CutoutActionGlyph.REDO,
                    enabled = !state.isBusy && state.eraseCanRedo,
                    onClick = onRedo,
                )
                CutoutSelectionActionChip(
                    label = R.string.cutout_erase_clear,
                    description = R.string.cutout_erase_clear,
                    glyph = CutoutActionGlyph.RESET,
                    enabled = !state.isBusy && state.erasePainted,
                    onClick = onClear,
                )
            }
            Text(
                stringResource(R.string.cutout_editor_brush_size, (state.brushSize * 200f).roundToInt()),
                style = MaterialTheme.typography.labelMedium,
            )
            Slider(
                value = state.brushSize,
                onValueChange = onSetBrushSize,
                valueRange = 0.005f..0.06f,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
            if (progress != null) {
                Text(progress, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (message != null && progress == null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CutoutSelectionActionChip(
    label: Int,
    description: Int,
    glyph: CutoutActionGlyph,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconDescription = stringResource(description)
    FilterChip(
        selected = false,
        enabled = enabled,
        onClick = onClick,
        label = {
            Text(
                stringResource(label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            CutoutActionGlyphIcon(
                glyph,
                modifier = Modifier.semantics {
                    contentDescription = iconDescription
                },
            )
        },
        modifier = modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun CutoutReviewPreview(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val tile = 20.dp.toPx()
            val columns = (size.width / tile).toInt() + 1
            val rows = (size.height / tile).toInt() + 1
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    drawRect(
                        color = if ((row + column) % 2 == 0) Color(0xfff4f3ef) else Color(0xffdcdad4),
                        topLeft = androidx.compose.ui.geometry.Offset(column * tile, row * tile),
                        size = androidx.compose.ui.geometry.Size(tile, tile),
                    )
                }
            }
        }
        Image(
            bitmap = image,
            contentDescription = stringResource(R.string.cutout_review_preview),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(8.dp),
        )
    }
}

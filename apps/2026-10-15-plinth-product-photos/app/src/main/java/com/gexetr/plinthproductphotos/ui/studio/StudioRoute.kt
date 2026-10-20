package com.gexetr.plinthproductphotos.ui.studio

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import android.graphics.Bitmap
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.BuildConfig
import com.gexetr.plinthproductphotos.data.api.OperationId
import com.gexetr.plinthproductphotos.data.api.ServiceState
import com.gexetr.plinthproductphotos.data.session.DevIntegrityTokenProvider
import com.gexetr.plinthproductphotos.data.session.IntegrityTokenProvider
import com.gexetr.plinthproductphotos.data.session.PlayIntegrityTokenProvider
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.domain.studio.CanvasAspect
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationId
import com.gexetr.plinthproductphotos.ui.autocard.FormingCardPreview
import com.gexetr.plinthproductphotos.ui.components.BalanceChip
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.components.PlinthStage
import com.gexetr.plinthproductphotos.ui.components.PlinthToolArtwork
import com.gexetr.plinthproductphotos.ui.components.ServiceClosedPanel
import com.gexetr.plinthproductphotos.ui.components.ServiceStateBanner
import com.gexetr.plinthproductphotos.ui.components.UpdateRequiredDialog
import com.gexetr.plinthproductphotos.ui.components.projectDisplayTitle
import java.io.File
import coil3.compose.SubcomposeAsyncImage

// Live createFalProvider probes retrieved O-01 JSON and O-02/O-07/O-08/O-10 images;
// evidence: .kiln/scratch/plinth-fal-live/{probe.mjs,probe2.mjs,recognition.json,output-*.png}.
// This proves transport/results, not product fidelity or the other provider operations.
// Only O-07/O-08/O-09/O-10 are represented; current availability still comes from /config.
// O-09 (Expand) is not covered by the live probe above.
internal const val STUDIO_INSTRUCTION_MAX_LENGTH = 240
internal const val STUDIO_PROVIDER_RESULTS_READY = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioRoute(
    onOpenWallet: () -> Unit,
    onOpenLibrary: (() -> Unit)? = null,
    initialOperation: OperationId? = null,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val factory = remember(context) { studioViewModelFactory(context) }
    val model: StudioViewModel = viewModel(factory = factory)
    val uriHandler = LocalUriHandler.current
    val state by model.state.collectAsState()
    var prompt by rememberSaveable { mutableStateOf("") }
    var editingPrompt by rememberSaveable { mutableStateOf(true) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var selectedOperation by rememberSaveable { mutableStateOf(initialOperation) }
    var canvasAspect by rememberSaveable { mutableStateOf(CanvasAspect.SQUARE) }
    var hideRestoredPendingTool by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedOperation = null
            hideRestoredPendingTool = false
            prompt = ""
            model.importPhoto(uri)
        }
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) model.saveDocument(uri)
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.refresh()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(model) { model.refresh() }

    val firstPhotoId = state.photos.firstOrNull()?.id
    LaunchedEffect(state.photoId, state.hasPending, firstPhotoId) {
        if (state.photoId == null && !state.hasPending && firstPhotoId != null) {
            model.choosePhoto(firstPhotoId)
        }
    }

    val selectedPhoto = state.photos.firstOrNull { it.id == state.photoId }
    val selectedPhotoFile = remember(context, selectedPhoto?.sourcePath) {
        selectedPhoto?.let { photo ->
            runCatching { ProjectRepository(context).sourceFile(photo.sourcePath) }
                .getOrNull()
                ?.takeIf(File::isFile)
        }
    }
    val availableRows = remember(state.config, state.photoId) {
        studioRows.mapNotNull { row ->
            val quote = state.config?.ops?.get(row.operation.wireId())
            if (
                quote != null && quote.enabled && quote.credits > 0 &&
                state.config?.killSwitch == false && state.config?.dailyCapReached == false &&
                STUDIO_PROVIDER_RESULTS_READY && state.photoId != null
            ) {
                row.copy(credits = quote.credits)
            } else {
                null
            }
        }
    }
    val activeOperation = selectedOperation ?: state.operation?.takeIf {
        (state.blocksNewOperation && !hideRestoredPendingTool) ||
            (state.phase == StudioPhase.DONE && state.result != null && !hideRestoredPendingTool) ||
        state.errorCode == "INVALID_RESULT_IMAGE" ||
        state.errorCode == "EXPAND_PRODUCT_RESTORE_FAILED"
    }
    val activeRow = activeOperation?.let { operation -> studioRows.firstOrNull { it.operation == operation } }
    val activeQuote = activeOperation?.wireId()?.let { wireId -> state.config?.ops?.get(wireId) }
    val photoName = selectedPhoto?.let { projectDisplayTitle(it.name, it.importedAt) }.orEmpty()
    val completedResult = state.phase == StudioPhase.DONE && state.result != null &&
        state.operation == activeOperation

    LaunchedEffect(state.phase, state.result) {
        if (state.phase == StudioPhase.DONE && state.result != null) editingPrompt = false
    }
    LaunchedEffect(state.operation, state.requestPrompt) {
        state.requestPrompt?.let { restored ->
            if (state.operation != null && prompt.isBlank()) prompt = restored
        }
    }

    fun launchPhotoPicker() {
        if (state.canChooseTool) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun returnToCatalogue() {
        if (initialOperation != null && onBack != null) {
            onBack()
            return
        }
        selectedOperation = null
        hideRestoredPendingTool = true
    }

    BackHandler(enabled = activeOperation != null, onBack = ::returnToCatalogue)

    PlinthScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (activeOperation != null || onBack != null) {
                        IconButton(
                            onClick = { if (activeOperation != null) returnToCatalogue() else onBack?.invoke() },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_arrow_left),
                                contentDescription = stringResource(R.string.studio_live_back),
                            )
                        }
                    }
                },
                title = {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            activeRow?.let { stringResource(it.name) }
                                ?: activeOperation?.let { stringResource(studioToolTitle(it)) }
                                ?: stringResource(R.string.studio_live_heading),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    BalanceChip(state.balance, onClick = onOpenWallet)
                },
            )
        },
    ) {
        val serviceState = state.config?.serviceState
        if (serviceState == ServiceState.MAINTENANCE || state.errorCode == "SERVICE_MAINTENANCE" || state.config?.dailyCapReached == true) {
            ServiceStateBanner(
                state = serviceState ?: ServiceState.MAINTENANCE,
                dismissible = state.config?.notice?.dismissible ?: true,
                onRetry = model::refresh,
                onDismiss = model::dismissError,
            )
        }
        val serviceClosed = serviceState == ServiceState.CLOSED || state.errorCode == "SERVICE_CLOSED"
        if (serviceClosed) {
            ServiceClosedPanel(onOpenResults = { onOpenLibrary?.invoke() }, onOpenWallet = onOpenWallet)
        }
        if (serviceState == ServiceState.UPDATE_REQUIRED || state.errorCode == "UPDATE_REQUIRED") {
            UpdateRequiredDialog(state.config?.minAppVersion.orEmpty()) {
                val packageId = BuildConfig.APPLICATION_ID
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageId")),
                    )
                }.onFailure {
                    uriHandler.openUri("https://play.google.com/store/apps/details?id=$packageId")
                }
            }
        }
        if (activeOperation == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.result != null) {
                    StudioBeforeAfterComparison(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                        before = {
                            StudioBeforePhoto(
                                sourceFile = selectedPhotoFile,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.78f)
                                    .clip(MaterialTheme.shapes.large),
                            )
                        },
                        after = {
                            StudioZoomableResultPreview(
                                bitmap = requireNotNull(state.result),
                            )
                        },
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(
                                if (state.applied) R.string.studio_live_result_done
                                else R.string.studio_live_result_ready,
                            ),
                        )
                        Text(
                            stringResource(R.string.studio_live_generated),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        StudioResultActions(
                            applied = state.applied,
                            documentSaved = state.documentSaved,
                            enabled = !state.busy,
                            onSave = model::applyResult,
                            onDiscard = { showDiscardConfirmation = true },
                            onTryAnother = state.operation?.let { operation ->
                                {
                                    selectedOperation = operation
                                    hideRestoredPendingTool = false
                                    editingPrompt = true
                                }
                            },
                            onSavePng = { documentPicker.launch("plinth-generated.png") },
                        )
                    }
                } else {
                    StudioPhotoStrip(
                        photoImage = selectedPhotoFile,
                        photoName = photoName,
                        onChangePhoto = ::launchPhotoPicker,
                        enabled = state.canChooseTool,
                    )
                    if (selectedPhoto == null) {
                        PlinthStage(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(R.string.studio_live_heading),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            Text(
                                stringResource(R.string.studio_live_no_photos),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = ::launchPhotoPicker,
                                enabled = state.canChooseTool,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) {
                                Text(stringResource(R.string.studio_live_import))
                            }
                            }
                        }
                    } else if (availableRows.isEmpty()) {
                        Text(
                            stringResource(R.string.studio_live_tools_unavailable),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        StudioToolGrid(
                            tools = availableRows,
                            enabled = state.canChooseTool,
                            onSelect = {
                                prompt = ""
                                hideRestoredPendingTool = false
                                selectedOperation = it
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (state.savedVariants.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(stringResource(R.string.studio_live_saved_title))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.savedVariants, key = { it.id }) { variant ->
                                    TextButton(
                                        onClick = { model.openVariant(variant.id) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        Column {
                                            Text(
                                                stringResource(
                                                    R.string.studio_live_open_saved,
                                                    variant.width,
                                                    variant.height,
                                                ),
                                            )
                                            Text(
                                                stringResource(R.string.w74_generated_disclosure),
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (state.savedVariants.isNotEmpty() && onOpenLibrary != null) {
                        TextButton(
                            onClick = onOpenLibrary,
                            modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.studio_live_open_library))
                        }
                    }
                    StudioFailureNotice(state.errorCode, state.errorMessage, state.retryAfterSec, onRetry = null)
                }
            }
        } else {
            val row = activeRow
            val quote = activeQuote
            if (row == null || quote == null) {
                if (state.result != null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StudioBeforeAfterComparison(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            before = {
                                StudioBeforePhoto(
                                    sourceFile = selectedPhotoFile,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.78f)
                                        .clip(MaterialTheme.shapes.large),
                                )
                            },
                            after = { StudioZoomableResultPreview(requireNotNull(state.result)) },
                        )
                        StudioResultActions(
                            applied = state.applied,
                            documentSaved = state.documentSaved,
                            enabled = !state.busy && (state.applied || state.deliveryConfirmed),
                            onSave = model::applyResult,
                            onDiscard = { showDiscardConfirmation = true },
                            onTryAnother = null,
                            onSavePng = { documentPicker.launch("plinth-generated.png") },
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.studio_live_tools_unavailable),
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val scenePrompt = activeOperation == OperationId.GENERATED_SCENE
                val promptValid = studioPromptIsValid(activeOperation, prompt)
                val eligibility = studioConfirmationPreview(
                    displayedCredits = quote.credits,
                    currentCredits = quote.credits,
                    balance = state.balance,
                    operationEnabled = quote.enabled,
                    killSwitchClear = state.config?.killSwitch == false,
                    dailyCapClear = state.config?.dailyCapReached == false,
                    rowBlocked = false,
                    providerReady = STUDIO_PROVIDER_RESULTS_READY,
                    hasPhoto = state.photoId != null,
                    promptValid = promptValid,
                    busy = state.busy,
                    hasPending = state.blocksNewOperation,
                )
                val canShowCreditDialog = state.balance?.let { it < quote.credits } == true &&
                    quote.credits > 0 &&
                    quote.enabled &&
                    state.config?.killSwitch == false &&
                    state.config?.dailyCapReached == false &&
                    STUDIO_PROVIDER_RESULTS_READY &&
                    state.photoId != null &&
                    promptValid &&
                    !state.busy &&
                    !state.blocksNewOperation
                val listState = rememberLazyListState()
                val resultIndex = if (state.errorCode != null) 2 else 1
                val hasResult = completedResult
                LaunchedEffect(state.result, hasResult) {
                    if (hasResult) listState.animateScrollToItem(resultIndex)
                }
                Column(modifier = Modifier.fillMaxSize()) {
                if (state.inFlight) StudioProgressBanner(activeOperation)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        StudioWorkflowPanel(
                            photoImage = selectedPhotoFile,
                            photoName = photoName,
                            operation = activeOperation,
                            operationName = stringResource(row.name),
                            costCredits = quote.credits,
                            balance = state.balance,
                            instruction = prompt,
                            onInstructionChange = { prompt = it.take(STUDIO_INSTRUCTION_MAX_LENGTH) },
                            promptRequired = scenePrompt,
                            enabled = eligibility.canPressGenerate || canShowCreditDialog,
                            waiting = state.inFlight,
                            showGenerate = !hasResult || editingPrompt,
                            showPrompt = !hasResult || editingPrompt,
                            hasUnsavedResult = hasResult && !state.applied,
                            canvasAspect = canvasAspect,
                            onCanvasAspectChange = { canvasAspect = it },
                            onTogglePrompt = { editingPrompt = !editingPrompt },
                            onGenerate = {
                                model.generate(activeOperation, prompt, quote.credits, canvasAspect)
                            },
                            onOpenWallet = onOpenWallet,
                            onChangePhoto = ::launchPhotoPicker,
                            allowChangePhoto = false,
                            photoChangeEnabled = state.canChooseTool,
                        )
                    }
                    if (state.errorCode != null) {
                        item {
                            StudioFailureNotice(
                                state.errorCode, state.errorMessage, state.retryAfterSec,
                                enabled = !state.busy,
                                onRetry = if (state.errorCode == "EXPAND_PRODUCT_RESTORE_FAILED") null else {
                                    {
                                        model.dismissError()
                                        model.generate(activeOperation, prompt, quote.credits, canvasAspect)
                                    }
                                },
                            )
                        }
                    }
                    if (state.result != null && state.operation == activeOperation) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)),
                            ) {
                                Column {
                                    StudioBeforeAfterComparison(
                                        modifier = Modifier.fillMaxWidth(),
                                        before = {
                                            StudioBeforePhoto(
                                                sourceFile = selectedPhotoFile,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(0.78f)
                                                    .clip(MaterialTheme.shapes.large),
                                            )
                                        },
                                        after = {
                                            StudioZoomableResultPreview(
                                                bitmap = requireNotNull(state.result),
                                            )
                                        },
                                    )
                                    Text(
                                        stringResource(
                                            if (state.applied) R.string.studio_live_result_done
                                            else R.string.studio_live_result_ready,
                                        ),
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                    Text(
                                        stringResource(R.string.studio_live_generated),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        item {
                            StudioResultActions(
                                applied = state.applied,
                                documentSaved = state.documentSaved,
                                enabled = !state.busy && (state.applied || state.deliveryConfirmed),
                                onSave = model::applyResult,
                                onDiscard = { showDiscardConfirmation = true },
                                onTryAnother = if (editingPrompt) null else {
                                    { editingPrompt = true }
                                },
                                onSavePng = { documentPicker.launch("plinth-generated.png") },
                            )
                        }
                    }
                }
                }
            }
        }
        if (showDiscardConfirmation) {
            AlertDialog(
                onDismissRequest = { showDiscardConfirmation = false },
                title = { Text(stringResource(R.string.studio_live_discard_title)) },
                text = { Text(stringResource(R.string.studio_live_discard_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardConfirmation = false
                            model.discard()
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.studio_live_discard_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDiscardConfirmation = false },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.studio_live_discard_cancel))
                    }
                },
            )
        }
    }
}

@Composable
internal fun studioCreditBalanceLabel(balance: Int?): String =
    balance?.let {
        stringResource(R.string.shell_balance_count, it.coerceAtLeast(0))
    } ?: stringResource(R.string.studio_live_balance_unknown)

@Composable
private fun StudioZoomableResultPreview(bitmap: Bitmap) {
    var showViewer by remember(bitmap) { mutableStateOf(false) }
    val zoomLabel = stringResource(R.string.studio_live_zoom)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = { showViewer = true })
            .semantics {
                contentDescription = zoomLabel
                role = Role.Button
            },
    ) {
        FormingCardPreview(
            bitmap = bitmap,
            aspectRatio = 0.78f,
            busy = false,
            message = "",
            failed = false,
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Text(
                zoomLabel,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
    if (showViewer) {
        StudioImageZoomDialog(bitmap = bitmap, onDismiss = { showViewer = false })
    }
}

@Composable
private fun StudioResultActions(
    applied: Boolean,
    documentSaved: Boolean,
    enabled: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onTryAnother: (() -> Unit)?,
    onSavePng: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSave,
            enabled = enabled && !applied,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(
                stringResource(
                    if (applied) R.string.studio_live_saved_result
                    else R.string.studio_live_apply,
                ),
            )
        }
        TextButton(
            onClick = onDiscard,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.studio_live_discard))
        }
        onTryAnother?.let { tryAnother ->
            TextButton(
                onClick = tryAnother,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.studio_live_try_another))
            }
        }
        if (applied) {
            Button(
                onClick = onSavePng,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.studio_live_save_png))
            }
            if (documentSaved) {
                Text(stringResource(R.string.studio_live_document_saved))
            }
        }
    }
}

@Composable
private fun StudioImageZoomDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var zoom by remember(bitmap) { mutableStateOf(1f) }
    var pan by remember(bitmap) { mutableStateOf(Offset.Zero) }
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(pan)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center,
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val viewport = Size(
                with(density) { maxWidth.toPx() },
                with(density) { maxHeight.toPx() },
            )
            val fitScale = minOf(
                viewport.width / bitmap.width.coerceAtLeast(1),
                viewport.height / bitmap.height.coerceAtLeast(1),
            )
            val imageSize = Size(bitmap.width * fitScale, bitmap.height * fitScale)
            val imageWidth = with(density) { imageSize.width.toDp() }
            val imageHeight = with(density) { imageSize.height.toDp() }
            val zoomCloseDescription = stringResource(R.string.studio_live_zoom_close)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(viewport, imageSize) {
                        detectTransformGestures { centroid, panChange, zoomChange, _ ->
                            val nextZoom = (currentZoom * zoomChange).coerceIn(1f, 5f)
                            val factor = nextZoom / currentZoom
                            val relativeCentroid = centroid - Offset(viewport.width / 2f, viewport.height / 2f)
                            val nextPan = Offset(
                                x = relativeCentroid.x - (relativeCentroid.x - currentPan.x) * factor + panChange.x,
                                y = relativeCentroid.y - (relativeCentroid.y - currentPan.y) * factor + panChange.y,
                            )
                            zoom = nextZoom
                            pan = clampStudioImagePan(nextPan, nextZoom, imageSize, viewport)
                        }
                    }
                    .pointerInput(viewport, imageSize) {
                        detectTapGestures(onDoubleTap = { tap ->
                            val nextZoom = if (currentZoom > 1f) 1f else 2.5f
                            if (nextZoom == 1f) {
                                zoom = 1f
                                pan = Offset.Zero
                            } else {
                                val factor = nextZoom / currentZoom
                                val relativeTap = tap - Offset(viewport.width / 2f, viewport.height / 2f)
                                val nextPan = Offset(
                                    x = relativeTap.x - (relativeTap.x - currentPan.x) * factor,
                                    y = relativeTap.y - (relativeTap.y - currentPan.y) * factor,
                                )
                                zoom = nextZoom
                                pan = clampStudioImagePan(nextPan, nextZoom, imageSize, viewport)
                            }
                        })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = image,
                    contentDescription = stringResource(R.string.studio_live_after),
                    modifier = Modifier
                        .requiredSize(imageWidth, imageHeight)
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = pan.x
                            translationY = pan.y
                        },
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_x),
                    contentDescription = zoomCloseDescription,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            Text(
                stringResource(R.string.viewer_zoom_hint),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun clampStudioImagePan(
    pan: Offset,
    zoom: Float,
    imageSize: Size,
    viewport: Size,
): Offset {
    val maxX = ((imageSize.width * zoom - viewport.width) / 2f).coerceAtLeast(0f)
    val maxY = ((imageSize.height * zoom - viewport.height) / 2f).coerceAtLeast(0f)
    return Offset(pan.x.coerceIn(-maxX, maxX), pan.y.coerceIn(-maxY, maxY))
}

@Composable
private fun StudioPhotoStrip(
    photoImage: Any?,
    photoName: String,
    onChangePhoto: () -> Unit,
    enabled: Boolean,
) {
    PlinthStage(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlinthToolArtwork(
                imageModel = photoImage,
                icon = R.drawable.ic_lucide_image,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.studio_live_your_photo), style = MaterialTheme.typography.labelLarge)
                Text(
                    photoName.ifBlank { stringResource(R.string.studio_live_photo_not_selected) },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onChangePhoto,
                enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(if (photoName.isBlank()) R.string.studio_live_import else R.string.studio_live_change_photo))
            }
        }
    }
}

@Composable
internal fun StudioToolGrid(
    tools: List<StudioToolRow>,
    onSelect: (OperationId) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tools, key = { it.operation }) { tool ->
            Card(
                onClick = { onSelect(tool.operation) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 164.dp)
                    .testTag("studio-tool-${tool.operation.name}"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlinthToolArtwork(
                        imageModel = null,
                        icon = tool.icon,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        stringResource(tool.name),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(tool.purpose),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            pluralStringResource(R.plurals.studio_live_credit_amount, tool.credits, tool.credits),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun StudioFailureNotice(
    code: String?,
    serverMessage: String?,
    retryAfterSec: Int?,
    onRetry: (() -> Unit)?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (code == null) return
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stringResource(R.string.studio_live_error_generic),
            color = MaterialTheme.colorScheme.error,
        )
        if (code != "EXPAND_PRODUCT_RESTORE_FAILED") {
            Text(
                stringResource(R.string.studio_live_error_detail, serverMessage?.takeIf { it.isNotBlank() } ?: code),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        retryAfterSec?.takeIf { it > 0 }?.let {
            Text(stringResource(R.string.studio_live_retry_wait, it))
        }
        if (onRetry != null) {
            OutlinedButton(
                onClick = onRetry,
                enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.studio_live_retry))
            }
        }
    }
}

@Composable
internal fun StudioBeforeAfterComparison(
    before: @Composable () -> Unit,
    after: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.studio_live_before),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                stringResource(R.string.studio_live_after),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { before() }
            Box(Modifier.weight(1f)) { after() }
        }
    }
}

@Composable
private fun StudioBeforePhoto(
    sourceFile: File?,
    modifier: Modifier = Modifier,
) {
    if (sourceFile == null) {
        StudioBeforeUnavailable(modifier)
        return
    }
    var showViewer by remember(sourceFile) { mutableStateOf(false) }
    val zoomLabel = stringResource(R.string.studio_live_zoom)
    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = sourceFile,
            contentDescription = stringResource(R.string.studio_live_before_photo),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable { showViewer = true }
                .semantics { role = Role.Button },
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
            error = { StudioBeforeUnavailable(Modifier.fillMaxSize()) },
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Text(
                zoomLabel,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
    if (showViewer) {
        StudioPhotoZoomDialog(
            sourceFile = sourceFile,
            contentDescription = stringResource(R.string.studio_live_before_photo),
            onDismiss = { showViewer = false },
        )
    }
}

@Composable
private fun StudioPhotoZoomDialog(
    sourceFile: File,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center,
        ) {
            SubcomposeAsyncImage(
                model = sourceFile,
                contentDescription = contentDescription,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                loading = { CircularProgressIndicator() },
                error = { StudioBeforeUnavailable(Modifier.fillMaxSize()) },
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_x),
                    contentDescription = stringResource(R.string.studio_live_zoom_close),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}

@Composable
private fun StudioBeforeUnavailable(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.studio_live_before_unavailable),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
internal fun StudioWorkflowPanel(
    photoImage: Any?,
    photoName: String,
    operation: OperationId = OperationId.RELIGHT,
    operationName: String,
    costCredits: Int,
    balance: Int?,
    instruction: String,
    onInstructionChange: (String) -> Unit,
    promptRequired: Boolean = false,
    enabled: Boolean,
    waiting: Boolean,
    showGenerate: Boolean = true,
    showPrompt: Boolean = true,
    hasUnsavedResult: Boolean = false,
    canvasAspect: CanvasAspect = CanvasAspect.SQUARE,
    onCanvasAspectChange: (CanvasAspect) -> Unit = {},
    onTogglePrompt: (() -> Unit)? = null,
    onGenerate: () -> Unit,
    onOpenWallet: () -> Unit,
    onChangeTool: (() -> Unit)? = null,
    onChangePhoto: () -> Unit = onChangeTool ?: {},
    photoChangeEnabled: Boolean = true,
    allowChangePhoto: Boolean = true,
) {
    var showCreditDialog by remember { mutableStateOf(false) }
    var showReplaceConfirmation by remember { mutableStateOf(false) }
    var replaceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun requestReplace(action: () -> Unit) {
        if (hasUnsavedResult) {
            replaceAction = action
            showReplaceConfirmation = true
        } else {
            action()
        }
    }
    val stagePulse by rememberInfiniteTransition(label = "studio-stage-progress").animateFloat(
        initialValue = 0.08f,
        targetValue = 0.26f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "studio-stage-progress-alpha",
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlinthToolArtwork(
                imageModel = photoImage,
                icon = R.drawable.ic_lucide_image,
                modifier = Modifier.size(42.dp).clip(MaterialTheme.shapes.medium),
            )
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        stringResource(R.string.studio_live_using_photo),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    photoName,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (allowChangePhoto) {
                TextButton(
                    onClick = { requestReplace(onChangePhoto) },
                    enabled = photoChangeEnabled && !waiting,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.studio_live_change_photo))
                }
            }
        }
        if (onChangeTool != null) {
            Text(operationName, style = MaterialTheme.typography.titleMedium)
        }
        if (operation == OperationId.EXPAND && showGenerate) {
            Text(operationName, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.w6_studio_purpose_expand),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CanvasAspect.values().toList(), key = { it.name }) { aspect ->
                    FilterChip(
                        selected = canvasAspect == aspect,
                        onClick = { onCanvasAspectChange(aspect) },
                        enabled = !waiting,
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(aspect.label) },
                    )
                }
            }
        }
        if (showGenerate) {
            PlinthStage(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth()) {
                    PlinthToolArtwork(
                        imageModel = photoImage,
                        icon = R.drawable.ic_lucide_image,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.78f)
                            .clip(MaterialTheme.shapes.large)
                            .then(
                                if (operation == OperationId.RECOLOUR) {
                                    Modifier.border(3.dp, PlinthTheme.accentInk, MaterialTheme.shapes.large)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                    if (waiting) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = stagePulse)),
                        )
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text(
                            stringResource(if (waiting) studioProgressLabel(operation) else R.string.studio_live_using_photo),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (operation == OperationId.RECOLOUR) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                stringResource(R.string.studio_live_recolour_target_chip),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
            if (operation == OperationId.RECOLOUR) {
                Text(
                    stringResource(R.string.studio_live_recolour_target_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.studio_live_recolour_target_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showPrompt) {
            if (onTogglePrompt != null) {
                TextButton(
                    onClick = onTogglePrompt,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.studio_live_hide_prompt))
                }
            }
            if (operation == OperationId.GENERATED_SCENE) {
                Text(
                    stringResource(R.string.studio_live_scene_prompt_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            OutlinedTextField(
                value = instruction,
                onValueChange = { onInstructionChange(it.take(STUDIO_INSTRUCTION_MAX_LENGTH)) },
                label = {
                    Text(
                        stringResource(
                            if (promptRequired) R.string.studio_live_scene_prompt
                            else R.string.studio_live_prompt,
                        ),
                    )
                },
                supportingText = {
                    Text(
                        stringResource(
                            if (promptRequired) R.string.studio_live_scene_prompt_hint
                            else R.string.studio_live_prompt_hint,
                        ),
                    )
                },
                enabled = !waiting,
                minLines = if (promptRequired) 5 else 2,
                maxLines = if (promptRequired) 8 else 4,
                modifier = Modifier.fillMaxWidth().testTag("studio-prompt-input"),
            )
            if (operation == OperationId.GENERATED_SCENE) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            R.string.studio_live_scene_suggestion_sea,
                            R.string.studio_live_scene_suggestion_forest,
                            R.string.studio_live_scene_suggestion_campsite,
                        ),
                        key = { it },
                    ) { suggestion ->
                        val suggestionText = stringResource(suggestion)
                        AssistChip(
                            onClick = { onInstructionChange(suggestionText) },
                            enabled = !waiting,
                            modifier = Modifier.heightIn(min = 48.dp),
                            label = { Text(suggestionText) },
                        )
                    }
                }
            }
            if (showGenerate) {
                Button(
                    onClick = {
                        val available = balance ?: return@Button
                        if (available < costCredits) {
                            showCreditDialog = true
                        } else if (enabled) {
                            requestReplace(onGenerate)
                        }
                    },
                    enabled = enabled && balance != null && !waiting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(
                        if (waiting) {
                            stringResource(studioProgressLabel(operation))
                        } else {
                            stringResource(
                                studioActionLabel(operation),
                                pluralStringResource(R.plurals.studio_live_credit_amount, costCredits, costCredits),
                            )
                        },
                    )
                }
                if (!waiting && (balance == null || !enabled)) {
                    Text(
                        text = when {
                            balance == null -> stringResource(R.string.studio_live_balance_unknown)
                            balance < costCredits -> stringResource(
                                R.string.studio_live_need_body,
                                pluralStringResource(R.plurals.studio_live_credit_amount, costCredits, costCredits),
                                studioCreditBalanceLabel(balance),
                            )
                            promptRequired -> stringResource(R.string.studio_live_scene_prompt_hint)
                            else -> stringResource(R.string.studio_live_tools_unavailable)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (onTogglePrompt != null) {
            TextButton(
                onClick = onTogglePrompt,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(
                        R.string.studio_live_edit_prompt_summary,
                        instruction.takeIf { it.isNotBlank() }?.take(72)
                            ?: stringResource(R.string.studio_live_prompt),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (showCreditDialog && balance != null && balance < costCredits) {
        AlertDialog(
            onDismissRequest = { showCreditDialog = false },
            title = { Text(stringResource(R.string.studio_live_need_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.studio_live_need_body,
                        pluralStringResource(
                            R.plurals.studio_live_credit_amount,
                            costCredits,
                            costCredits,
                        ),
                        studioCreditBalanceLabel(balance),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreditDialog = false
                        onOpenWallet()
                    },
                ) {
                    Text(stringResource(R.string.studio_live_need_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreditDialog = false }) {
                    Text(stringResource(R.string.studio_live_need_dismiss))
                }
            },
        )
    }
    if (showReplaceConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showReplaceConfirmation = false
                replaceAction = null
            },
            title = { Text(stringResource(R.string.studio_live_replace_unsaved_title)) },
            text = { Text(stringResource(R.string.studio_live_replace_unsaved_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val action = replaceAction
                        replaceAction = null
                        showReplaceConfirmation = false
                        action?.invoke()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.studio_live_replace_unsaved_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReplaceConfirmation = false
                        replaceAction = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.studio_live_replace_unsaved_cancel))
                }
            },
        )
    }
}

@Composable
internal fun StudioOperationActionButton(
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val actionLabel = stringResource(R.string.studio_live_choose_tool)
    if (primary) {
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(actionLabel)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(actionLabel)
        }
    }
}

internal fun studioProgressLabel(operation: OperationId): Int = when (operation) {
    OperationId.GENERATED_SCENE -> R.string.studio_live_progress_scene
    OperationId.RELIGHT -> R.string.studio_live_progress_relight
    OperationId.RECOLOUR -> R.string.studio_live_progress_recolour
    OperationId.EXPAND -> R.string.studio_live_progress_expand
    else -> R.string.studio_live_generating
}

// One pinned progress banner shared by every operation; it lives outside the scrolling list.
@Composable
internal fun StudioProgressBanner(operation: OperationId?, modifier: Modifier = Modifier) {
    val label = stringResource(operation?.let(::studioProgressLabel) ?: R.string.studio_live_generating)
    Surface(
        modifier = modifier.fillMaxWidth().testTag("studio-progress-banner"),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp).semantics { contentDescription = label },
                    strokeWidth = 3.dp,
                )
                Text(label, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                stringResource(R.string.studio_live_progress_keep_open),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            if (operation == OperationId.RELIGHT) {
                Text(
                    stringResource(R.string.studio_live_eta_long),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

internal fun studioActionLabel(operation: OperationId): Int = when (operation) {
    OperationId.GENERATED_SCENE -> R.string.studio_live_action_scene
    OperationId.RELIGHT -> R.string.studio_live_action_relight
    OperationId.RECOLOUR -> R.string.studio_live_action_recolour
    else -> R.string.studio_live_generate_cost
}

internal fun studioToolTitle(operation: OperationId): Int = when (operation) {
    OperationId.EXPAND -> R.string.studio_operation_expand_canvas
    OperationId.VIRTUAL_MODEL -> R.string.studio_operation_virtual_model
    OperationId.RELIGHT -> R.string.studio_operation_relight
    OperationId.RECOLOUR -> R.string.studio_operation_recolour
    else -> R.string.studio_live_heading
}

internal data class StudioToolRow(
    val operation: OperationId,
    val name: Int,
    val purpose: Int,
    val icon: Int,
    val credits: Int = 0,
    val homeOperation: StudioOperationId = StudioOperationId.GENERATED_SCENE,
)

// The one list of runnable image tools; Home renders it (plus Cutout) and Studio renders it again.
internal val studioRows = listOf(
    StudioToolRow(
        OperationId.GENERATED_SCENE,
        R.string.home_tool_scene_title,
        R.string.home_scene_description,
        R.drawable.ic_lucide_image,
        credits = 5,
        homeOperation = StudioOperationId.GENERATED_SCENE,
    ),
    StudioToolRow(
        OperationId.RELIGHT,
        R.string.studio_operation_relight,
        R.string.home_relight_description,
        R.drawable.ic_lucide_camera,
        credits = 5,
        homeOperation = StudioOperationId.RELIGHT,
    ),
    StudioToolRow(
        OperationId.EXPAND,
        R.string.studio_operation_expand_canvas,
        R.string.w6_studio_purpose_expand,
        R.drawable.ic_lucide_image,
        credits = 2,
        homeOperation = StudioOperationId.EXPAND_CANVAS,
    ),
    StudioToolRow(
        OperationId.RECOLOUR,
        R.string.studio_operation_recolour,
        R.string.home_recolour_description,
        R.drawable.ic_lucide_refresh_cw,
        credits = 2,
        homeOperation = StudioOperationId.RECOLOUR,
    ),
)

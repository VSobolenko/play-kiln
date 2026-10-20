package com.gexetr.plinthproductphotos.ui.card

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.LocalPhotoSharedTransitionScope
import com.gexetr.plinthproductphotos.ui.PhotoTransitionKey
import com.gexetr.plinthproductphotos.ui.PhotoJourney
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.components.PlinthStage
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
fun CardRoute(
    photoId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardViewModel = viewModel(factory = CardViewModel.factory(LocalContext.current)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backgroundArgb = MaterialTheme.colorScheme.surface.toArgb()
    val saveCard = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) {
        uri -> if (uri != null) viewModel.exportCard(uri)
    }
    LaunchedEffect(photoId, backgroundArgb) { viewModel.load(photoId, backgroundArgb) }
    CardScreen(
        photoId = photoId,
        state = state,
        onBack = onBack,
        onMakeCard = { viewModel.makeCard(backgroundArgb) },
        onEdit = onEdit,
        onExport = { saveCard.launch("product-card.png") },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CardScreen(
    state: CardUiState,
    onBack: () -> Unit,
    onMakeCard: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
    photoId: String? = null,
) {
    val photoTransition = LocalPhotoSharedTransitionScope.current?.takeIf { photoId != null }?.let { sharedScope ->
        with(sharedScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(PhotoTransitionKey(requireNotNull(photoId), journey = PhotoJourney.LIBRARY_CARD)),
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            )
        }
    } ?: Modifier
    var inspecting by rememberSaveable(state.photoName) { mutableStateOf(false) }
    val composition = state.composition
    val preview = composition ?: state.original
    val busy = state.isLoading || state.isComposing
    val saving = state.exportState == CardExportState.SAVING
    val previewLabel = stringResource(
        if (composition == null) R.string.plinth_before_description else R.string.plinth_card_preview_description,
    )
    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.card_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.plinth_back))
                    }
                },
            )
        },
        bottomBar = {
            if (composition != null || state.error && state.original != null) {
                Surface {
                    Button(
                        onClick = when {
                            composition == null -> onMakeCard
                            state.exportState == CardExportState.SAVED -> onBack
                            else -> onExport
                        },
                        enabled = state.original != null && !busy && !saving,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                            .heightIn(min = 52.dp),
                    ) {
                        Text(stringResource(
                            when {
                                composition == null -> R.string.card_try_again
                                state.exportState == CardExportState.SAVED -> R.string.card_done
                                state.exportState == CardExportState.SAVING -> R.string.card_saving_action
                                else -> R.string.card_save_png
                            },
                        ))
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(state.photoName, style = MaterialTheme.typography.titleMedium)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PlinthStage(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
                    Box(
                        modifier = photoTransition.fillMaxWidth().aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (preview != null) {
                            Image(
                                bitmap = preview.asImageBitmap(),
                                contentDescription = previewLabel,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (busy) {
                            Surface(shape = MaterialTheme.shapes.large) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    CircularProgressIndicator()
                                    Text(stringResource(
                                        if (state.isLoading) R.string.card_loading else R.string.card_composing,
                                    ))
                                }
                            }
                        } else if (state.error && state.original == null) {
                            Text(
                                stringResource(R.string.plinth_photo_error),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(when {
                    state.isLoading -> R.string.card_loading
                    state.isComposing -> R.string.card_composing
                    state.error -> R.string.card_failed
                    composition != null -> R.string.card_result_ready
                    else -> R.string.card_source_ready
                }),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.titleMedium,
                color = if (state.error) MaterialTheme.colorScheme.error else PlinthTheme.accentInk,
            )
            if (composition != null) {
                val exportStatus = when (state.exportState) {
                    CardExportState.IDLE -> stringResource(R.string.card_not_saved)
                    CardExportState.SAVING -> stringResource(R.string.card_saving)
                    CardExportState.SAVED -> state.savedDestination?.let {
                        stringResource(R.string.card_saved, it)
                    } ?: stringResource(R.string.card_saved_location_unknown)
                    CardExportState.ERROR -> stringResource(R.string.card_save_error)
                }
                Text(
                    exportStatus,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.exportState == CardExportState.ERROR) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.card_export_guidance),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                )
            }
            if (preview != null) {
                OutlinedButton(
                    onClick = { inspecting = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.card_inspect)) }
            }
            if (state.error && state.original != null) {
                Text(
                    stringResource(R.string.plinth_compose_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (composition != null) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !busy && !saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.plinth_open_editor))
                }
            }
        }
    }
    if (inspecting && preview != null) {
        Dialog(
            onDismissRequest = { inspecting = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 8f))
            LaunchedEffect(zoomableState, preview) {
                zoomableState.contentScale = ContentScale.Inside
                zoomableState.setContentLocation(
                    ZoomableContentLocation.scaledInsideAndCenterAligned(
                        Size(preview.width.toFloat(), preview.height.toFloat()),
                    ),
                )
            }
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(previewLabel, style = MaterialTheme.typography.titleLarge)
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = previewLabel,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .zoomable(zoomableState)
                            .testTag("card-inspection-zoomable-image"),
                    )
                    Text(
                        stringResource(R.string.viewer_zoom_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { inspecting = false },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.card_close_inspection)) }
                }
            }
        }
    }
}

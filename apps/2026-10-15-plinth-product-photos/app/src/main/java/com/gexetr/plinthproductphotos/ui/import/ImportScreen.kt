package com.gexetr.plinthproductphotos.ui.importscreen

import android.net.Uri
import android.widget.Toast
import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.wallet.WalletAccessPolicy
import com.gexetr.plinthproductphotos.ui.wallet.ServerUnlock
import com.gexetr.plinthproductphotos.ui.components.PlinthStage
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.components.PhotoCaptureLauncher
import com.gexetr.plinthproductphotos.ui.components.PhotoSource
import com.gexetr.plinthproductphotos.ui.components.PhotoSourceSheet
import com.gexetr.plinthproductphotos.ui.components.importSourceSelection
import java.io.File

@Composable
fun ImportRoute(
    onBack: () -> Unit,
    onOpenAutoCard: (String) -> Unit,
    onOpenLocalCard: (String) -> Unit,
    launchPickerOnEnter: Boolean = false,
    prefillUri: String? = null,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = viewModel(factory = ImportViewModel.factory(LocalContext.current)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var awaitingQuickImport by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf<Boolean?>(null) }
    var selectionTruncated by rememberSaveable { mutableStateOf(false) }
    var showSourceSheet by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { unlocked = ServerUnlock.fetch(context) }
    val serverConfirmedUnlock = unlocked ?: run {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val limit = WalletAccessPolicy.pickerSelectionLimit(serverConfirmedUnlock)
    // A photo picked on Home arrives here already chosen. Importing it on entry is what makes the
    // home sheet one tap instead of a detour into an empty import screen.
    var prefillHandled by remember(prefillUri) { mutableStateOf(false) }
    LaunchedEffect(prefillUri, serverConfirmedUnlock) {
        if (!prefillHandled && !prefillUri.isNullOrBlank()) {
            prefillHandled = true
            awaitingQuickImport = true
            viewModel.importUris(
                importSourceSelection(PhotoSource.GALLERY, listOf(Uri.parse(prefillUri)), serverConfirmedUnlock),
            )
        }
    }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = pickerMaxItems(limit)),
    ) { uris ->
        if (uris.isEmpty()) {
            if (launchPickerOnEnter) onBack()
        } else {
            val kept = importSourceSelection(PhotoSource.GALLERY, uris, serverConfirmedUnlock)
            selectionTruncated = kept.size < uris.size
            awaitingQuickImport = launchPickerOnEnter
            viewModel.importUris(kept)
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val kept = importSourceSelection(PhotoSource.FILES, uris, serverConfirmedUnlock)
            selectionTruncated = kept.size < uris.size
            awaitingQuickImport = launchPickerOnEnter
            viewModel.importUris(kept)
        }
    }
    val camera = remember {
        PhotoCaptureLauncher(
            context = context,
            onCaptured = { uri ->
                awaitingQuickImport = launchPickerOnEnter
                viewModel.importUris(importSourceSelection(PhotoSource.CAMERA, listOf(uri), serverConfirmedUnlock))
            },
            onCancelled = {},
            onDenied = {
                Toast.makeText(context, R.string.photo_source_camera_permission_denied, Toast.LENGTH_LONG).show()
                showSourceSheet = true
            },
        )
    }
    camera.Bind()
    LaunchedEffect(launchPickerOnEnter) {
        if (launchPickerOnEnter) showSourceSheet = true
    }
    if (showSourceSheet) {
        PhotoSourceSheet(
            onDismiss = { showSourceSheet = false },
            onSource = { source ->
                showSourceSheet = false
                when (source) {
                    PhotoSource.CAMERA -> camera.launch()
                    PhotoSource.GALLERY -> picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    PhotoSource.FILES -> filePicker.launch(arrayOf("image/*"))
                }
            },
        )
    }
    LaunchedEffect(awaitingQuickImport, state.isImporting, state.importedPhotos) {
        if (awaitingQuickImport && !state.isImporting) {
            state.importedPhotos.firstOrNull()?.let {
                awaitingQuickImport = false
                onOpenLocalCard(it.id)
            }
        }
    }

    ImportScreen(
        state = state,
        batchLimit = limit,
        unlocked = serverConfirmedUnlock,
        selectionTruncated = selectionTruncated,
        onBack = onBack,
        onPickPhotos = {
            showSourceSheet = true
        },
        onOpenAutoCard = onOpenAutoCard,
        onOpenLocalCard = onOpenLocalCard,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    state: ImportUiState,
    onBack: () -> Unit,
    onPickPhotos: () -> Unit,
    onOpenAutoCard: (String) -> Unit,
    onOpenLocalCard: (String) -> Unit,
    modifier: Modifier = Modifier,
    batchLimit: Int = WalletAccessPolicy.FREE_BATCH_LIMIT,
    unlocked: Boolean = false,
    selectionTruncated: Boolean = false,
) {
    val context = LocalContext.current
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPhoto = state.importedPhotos.firstOrNull { it.id == selectedId }
        ?: state.importedPhotos.firstOrNull()
    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_flow_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.plinth_back))
                    }
                },
            )
        },
        bottomBar = {
            if (!state.isImporting && selectedPhoto != null) {
                Surface(shadowElevation = 6.dp) {
                    Column(
                        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Button(
                            onClick = { onOpenLocalCard(selectedPhoto.id) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            Text(stringResource(R.string.create_flow_continue_to_studio))
                        }
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
            if (selectedPhoto != null) {
                SelectedMediaPreview(
                    model = File(context.filesDir, selectedPhoto.sourcePath),
                    name = selectedPhoto.name,
                    state = stringResource(R.string.create_flow_photo_ready),
                    modifier = Modifier.photoSwipeBack(onBack),
                )
                if (state.importedPhotos.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.importedPhotos, key = { it.id }) { photo ->
                            val photoDescription = stringResource(
                                R.string.plinth_recent_photo_content_description,
                                photo.name,
                            )
                            Surface(
                                onClick = { selectedId = photo.id },
                                shape = MaterialTheme.shapes.large,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (photo.id == selectedPhoto.id) 3.dp else 1.dp,
                                    if (photo.id == selectedPhoto.id) PlinthTheme.accentInk
                                    else MaterialTheme.colorScheme.outlineVariant,
                                ),
                                modifier = Modifier.widthIn(min = 156.dp, max = 220.dp).semantics(
                                    mergeDescendants = true,
                                ) {
                                    contentDescription = photoDescription
                                    selected = photo.id == selectedPhoto.id
                                },
                            ) {
                                SelectedMediaPreview(
                                    model = File(context.filesDir, photo.sourcePath),
                                    name = photo.name,
                                    state = stringResource(
                                        if (photo.id == selectedPhoto.id) R.string.create_flow_photo_selected
                                        else R.string.create_flow_photo_tap_select,
                                    ),
                                )
                            }
                        }
                    }
                }
            } else if (state.isImporting) {
                state.selectedUris.firstOrNull()?.let { uri ->
                    SelectedMediaPreview(
                        model = uri,
                        name = stringResource(R.string.w3_selected_photo),
                        state = stringResource(R.string.w3_import_copying),
                        modifier = Modifier.photoSwipeBack(onBack),
                    )
                }
            }
            if (selectionTruncated) {
                Text(
                    stringResource(R.string.import_batch_truncated, batchLimit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (selectedPhoto == null && !state.isImporting) {
                PlinthStage(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            stringResource(R.string.create_flow_heading),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(
                                if (unlocked) R.string.import_batch_limit_unlocked else R.string.import_batch_limit_free,
                                batchLimit,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onPickPhotos,
                            enabled = !state.isImporting,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            Text(stringResource(R.string.create_flow_choose_photo))
                        }
                    }
                }
            } else {
                TextButton(
                    onClick = onPickPhotos,
                    enabled = !state.isImporting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.w3_change_photo))
                }
            }

            if (state.isImporting) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    Text(stringResource(R.string.plinth_import_progress, state.completedCount, state.totalCount))
                }
            }

            state.failures.forEach { failure ->
                val reason = when (failure.kind) {
                    ImportFailureKind.NOT_IMAGE -> R.string.plinth_import_error_type
                    ImportFailureKind.STORAGE -> R.string.plinth_import_error_storage
                    ImportFailureKind.UNAVAILABLE -> R.string.plinth_import_error_access
                    ImportFailureKind.UNKNOWN -> R.string.plinth_import_error_unknown
                }
                Text(
                    stringResource(R.string.plinth_import_failure, failure.selectionNumber, stringResource(reason)),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@android.annotation.SuppressLint("NewApi")
private fun pickerMaxItems(limit: Int): Int =
    runCatching { android.provider.MediaStore.getPickImagesMaxLimit() }.getOrDefault(limit).coerceAtMost(limit).coerceAtLeast(2)

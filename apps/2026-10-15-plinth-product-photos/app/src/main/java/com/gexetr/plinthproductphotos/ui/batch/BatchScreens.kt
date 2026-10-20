package com.gexetr.plinthproductphotos.ui.batch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.look.LookRepository
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.domain.batch.BatchItemState
import com.gexetr.plinthproductphotos.domain.batch.BatchQueueItem
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.notification.BatchNotifier
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BatchScreen(
    batchId: String? = null,
    sourceIds: List<String>,
    look: Look,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val stableBatchId = remember(batchId) { batchId ?: UUID.randomUUID().toString() }
    val viewModel: BatchViewModel = viewModel(
        key = "plinth-batch-$stableBatchId",
        factory = BatchViewModel.factory(context, stableBatchId, sourceIds, look),
    )
    val state by viewModel.state.collectAsState()
    val observedItems by viewModel.items.collectAsState()
    val workStatus by viewModel.workStatus.collectAsState()
    val items = observedItems.filter { it.batchId == state.batchId }
    var photos by remember { mutableStateOf<Map<String, ProjectPhoto>>(emptyMap()) }
    var photoLoadError by remember { mutableStateOf<String?>(null) }
    var notificationEnabled by remember(context) { mutableStateOf(BatchNotifier.notificationsEnabled(context)) }
    var askPermission by remember { mutableStateOf(false) }
    LaunchedEffect(sourceIds, items.map { it.sourceId }) {
        try {
            photos = withContext(Dispatchers.IO) {
                val selected = sourceIds.toSet() + items.map { it.sourceId }
                ProjectRepository(context).recentPhotos().filter { it.id in selected }.associateBy { it.id }
            }
            photoLoadError = null
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            photoLoadError = failure.message ?: failure.javaClass.simpleName
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.start()
    }

    val completed = items.count { it.state == BatchItemState.COMPOSED }
    val failed = items.count { it.state == BatchItemState.FAILED }
    val cancelled = items.count { it.state == BatchItemState.CANCELLED }
    val active = items.any { it.state == BatchItemState.QUEUED || it.state == BatchItemState.PROCESSING }
    val batchWork = workStatus?.takeIf { it.batchId == state.batchId }
    val isPaused = active && !state.isStarting && batchWork != null && !batchWork.isScheduled
    val isRunning = batchWork?.isRunning == true
    val running = if (isRunning) items.count { it.state == BatchItemState.PROCESSING } else 0
    val queued = items.count { it.state == BatchItemState.QUEUED } +
        if (!isRunning) items.count { it.state == BatchItemState.PROCESSING } else 0
    val isFinished = items.isNotEmpty() && !active
    val notificationDenied = isFinished && notificationEnabled && Build.VERSION.SDK_INT >= 33 &&
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED

    PlinthScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.batch_back)) }
                    Text(
                        stringResource(R.string.batch_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Text(
                    stringResource(R.string.batch_look_label, look.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (items.isEmpty() && !state.hasStarted && !state.isStarting) {
                item {
                    Text(
                        stringResource(
                            R.string.batch_selection_summary,
                            state.selectedSourceIds.size,
                            sourceIds.distinct().size,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = viewModel::selectAll) {
                            Text(stringResource(R.string.batch_select_all))
                        }
                        TextButton(
                            onClick = viewModel::clearSelection,
                            enabled = state.selectedSourceIds.isNotEmpty(),
                        ) {
                            Text(stringResource(R.string.batch_clear_selection))
                        }
                    }
                }
            }
            item {
                if (items.isNotEmpty()) {
                    QueueSummary(
                        completed = completed,
                        failed = failed,
                        cancelled = cancelled,
                        running = running,
                        queued = queued,
                        total = items.size,
                        isPaused = isPaused,
                        isFinished = isFinished,
                        workStatus = batchWork,
                    )
                }
                if (state.isStarting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (notificationDenied) {
                        Text(stringResource(R.string.batch_notifications_denied))
                    }
                    val error = state.error ?: photoLoadError
                    if (error != null) {
                        Text(
                            stringResource(R.string.batch_action_error, error),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (items.isEmpty()) {
                            Button(
                                onClick = {
                                    if (notificationEnabled && BatchNotifier.shouldAskPermission(context)) {
                                        askPermission = true
                                    } else {
                                        viewModel.start()
                                    }
                                },
                                enabled = !state.isStarting && !state.hasStarted &&
                                    state.selectedSourceIds.isNotEmpty(),
                            ) {
                                Text(stringResource(R.string.batch_apply_look))
                            }
                        }
                        if (active && isPaused) {
                            Button(onClick = viewModel::resume, enabled = !state.isStarting) {
                                Text(stringResource(R.string.batch_resume))
                            }
                        }
                        if (failed > 0) {
                            OutlinedButton(onClick = viewModel::retryFailed, enabled = !state.isStarting) {
                                Text(stringResource(R.string.batch_retry_failed))
                            }
                        }
                        if (active && batchWork?.isScheduled == true) {
                            OutlinedButton(onClick = viewModel::pause, enabled = !state.isStarting) {
                                Text(stringResource(R.string.batch_pause))
                            }
                        }
                        if (isFinished) {
                            OutlinedButton(onClick = viewModel::reapplyLook, enabled = !state.isStarting) {
                                Text(stringResource(R.string.batch_reapply_look))
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.batch_notifications_toggle),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val notificationLabel = stringResource(R.string.batch_notifications_toggle)
                        Switch(
                            checked = notificationEnabled,
                            onCheckedChange = { enabled ->
                                notificationEnabled = enabled
                                BatchNotifier.setNotificationsEnabled(context, enabled)
                            },
                            modifier = Modifier.semantics { contentDescription = notificationLabel },
                        )
                    }
                }
            }
            if (items.isEmpty() && !state.hasStarted && !state.isStarting) {
                columnItems(sourceIds.distinct(), key = { "source-$it" }) { sourceId ->
                    val photo = photos[sourceId]
                    val name = photo?.name ?: stringResource(R.string.batch_photo_unknown)
                    val selectPhotoDescription = stringResource(
                        R.string.batch_select_photo,
                        name,
                    )
                    Card(modifier = Modifier.fillMaxWidth().animateItem()) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .toggleable(
                                    value = sourceId in state.selectedSourceIds,
                                    role = Role.Checkbox,
                                    onValueChange = { viewModel.toggleSelection(sourceId) },
                                )
                                .semantics { contentDescription = selectPhotoDescription }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            QueueThumbnail(photo?.let { File(context.filesDir, it.sourcePath) })
                            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Checkbox(checked = sourceId in state.selectedSourceIds, onCheckedChange = null)
                        }
                    }
                }
            }
            if (items.isEmpty() && sourceIds.isEmpty() && state.error == null) {
                item { Text(stringResource(R.string.batch_empty)) }
            } else if (items.isNotEmpty()) {
                columnItems(items, key = { it.id }) { item ->
                    val photo = photos[item.sourceId]
                    QueueCard(
                        item = item,
                        name = photo?.name ?: stringResource(R.string.batch_photo_unknown),
                        thumbnail = (item.outputPath?.takeIf {
                            item.state == BatchItemState.COMPOSED && it.isNotBlank()
                        } ?: photo?.sourcePath?.takeIf(String::isNotBlank))
                            ?.let { File(context.filesDir, it) },
                        isPaused = isPaused,
                        isRunning = isRunning,
                        actionEnabled = !state.isStarting,
                        onRetry = { viewModel.retry(item.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (askPermission) {
        AlertDialog(
            onDismissRequest = {
                askPermission = false
                BatchNotifier.markPermissionAsked(context)
                viewModel.start()
            },
            title = { Text(stringResource(R.string.batch_permission_title)) },
            text = { Text(stringResource(R.string.batch_permission_explanation)) },
            confirmButton = {
                TextButton(onClick = {
                    askPermission = false
                    BatchNotifier.markPermissionAsked(context)
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.start()
                    }
                }) { Text(stringResource(R.string.batch_permission_enable)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    askPermission = false
                    BatchNotifier.markPermissionAsked(context)
                    viewModel.start()
                }) { Text(stringResource(R.string.batch_permission_not_now)) }
            },
        )
    }
}

@Composable
private fun QueueSummary(
    completed: Int,
    failed: Int,
    cancelled: Int,
    running: Int,
    queued: Int,
    total: Int,
    isPaused: Boolean,
    isFinished: Boolean,
    workStatus: BatchWorkStatus?,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(
                    when {
                        isFinished && completed == total -> R.string.batch_finished
                        isFinished -> R.string.batch_queue_settled
                        isPaused -> R.string.batch_queue_paused
                        workStatus == null -> R.string.batch_queue_checking
                        workStatus.isRunning -> R.string.batch_queue_live
                        workStatus.isRetryScheduled -> R.string.batch_queue_auto_retry
                        else -> R.string.batch_queue_waiting
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QueueStateBadge(BatchItemState.QUEUED, queued)
                QueueStateBadge(BatchItemState.PROCESSING, running)
                QueueStateBadge(BatchItemState.COMPOSED, completed)
                QueueStateBadge(BatchItemState.FAILED, failed)
                if (cancelled > 0) QueueStateBadge(BatchItemState.CANCELLED, cancelled)
            }
            Text(
                stringResource(R.string.batch_queue_progress, completed + failed + cancelled, total),
                style = MaterialTheme.typography.bodySmall,
            )
            LinearProgressIndicator(
                progress = { (completed + failed + cancelled).toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun QueueCard(
    item: BatchQueueItem,
    name: String,
    thumbnail: File?,
    isPaused: Boolean,
    isRunning: Boolean,
    actionEnabled: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pending = item.state == BatchItemState.QUEUED || item.state == BatchItemState.PROCESSING
    val displayState = if (!isRunning && pending) BatchItemState.QUEUED else item.state
    // A claim increments attempt; a requeued row retains its previous attempt count.
    val repeatAttempt = when (displayState) {
        BatchItemState.QUEUED -> item.attempt > 0
        BatchItemState.PROCESSING -> item.attempt > 1
        else -> false
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(width = 4.dp, height = 64.dp)
                    .clip(MaterialTheme.shapes.small).background(displayState.queueColors().second),
            )
            QueueThumbnail(thumbnail)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                QueueStateBadge(
                    displayState,
                    paused = isPaused && pending,
                    repeatAttempt = repeatAttempt,
                )
                if (item.attempt > 0) {
                    Text(
                        stringResource(R.string.batch_attempt, item.attempt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (item.state == BatchItemState.FAILED) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                Text(
                    item.error?.takeIf { it.isNotBlank() } ?: stringResource(R.string.batch_queue_failure),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                val retryDescription = stringResource(R.string.batch_retry_photo, name)
                TextButton(
                    onClick = onRetry,
                    enabled = actionEnabled,
                    modifier = Modifier.semantics { contentDescription = retryDescription },
                ) {
                    Text(stringResource(R.string.batch_retry_item))
                }
            }
        }
    }
}

@Composable
private fun QueueThumbnail(model: File?) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        },
        error = {
            Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.batch_queue_no_preview),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun QueueStateBadge(
    state: BatchItemState,
    count: Int? = null,
    paused: Boolean = false,
    repeatAttempt: Boolean = false,
) {
    val (container, content) = state.queueColors()
    val label = stringResource(
        when {
            paused -> R.string.batch_queue_paused
            repeatAttempt && state == BatchItemState.QUEUED -> R.string.batch_state_retry_queued
            repeatAttempt && state == BatchItemState.PROCESSING -> R.string.batch_state_retrying
            else -> state.stringId()
        },
    )
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        border = if (repeatAttempt) BorderStroke(1.dp, content) else null,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state == BatchItemState.PROCESSING && count != 0) {
                CircularProgressIndicator(Modifier.size(12.dp), color = content, strokeWidth = 2.dp)
            }
            Text(
                if (count == null) label else stringResource(R.string.batch_queue_count, label, count),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun BatchItemState.queueColors(): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (this@queueColors) {
        BatchItemState.QUEUED -> secondaryContainer to onSecondaryContainer
        BatchItemState.PROCESSING -> primaryContainer to onPrimaryContainer
        BatchItemState.COMPOSED -> tertiaryContainer to onTertiaryContainer
        BatchItemState.FAILED -> errorContainer to onErrorContainer
        BatchItemState.CANCELLED -> surfaceContainerHigh to onSurfaceVariant
    }
}

@Composable
fun LooksScreen(
    currentLook: Look?,
    onApplyLook: (Look) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val copySuffix = stringResource(R.string.looks_copy_suffix)
    val repository = remember(context) { LookRepository(context) }
    val scope = rememberCoroutineScope()
    var looks by remember { mutableStateOf<List<Look>>(emptyList()) }
    var editTarget by remember { mutableStateOf<Look?>(null) }
    var name by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.all() } }
                .onSuccess { looks = it; error = null }
                .onFailure { error = it.message ?: "Could not read saved Looks" }
        }
    }
    LaunchedEffect(repository) { refresh() }

    PlinthScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.looks_back)) }
                Text(stringResource(R.string.looks_title), style = MaterialTheme.typography.headlineSmall)
            }
            if (currentLook != null) {
                Button(onClick = {
                    name = currentLook.name
                    showSaveDialog = true
                }) { Text(stringResource(R.string.looks_save_current)) }
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.looks_backup_included))
            if (looks.isEmpty()) {
                Text(stringResource(R.string.looks_empty))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    gridItems(looks, key = Look::id) { look ->
                        Card {
                            Column(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(look.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(
                                        R.string.looks_recipe_summary,
                                        look.marginPct * 100f,
                                        look.fillTarget * 100f,
                                    ),
                                )
                                TextButton(onClick = { onApplyLook(look) }) {
                                    Text(stringResource(R.string.looks_apply))
                                }
                                TextButton(onClick = {
                                    editTarget = look
                                    name = look.name
                                }) { Text(stringResource(R.string.looks_rename)) }
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                repository.duplicate(
                                                    look.id,
                                                    "${look.name} $copySuffix",
                                                )
                                            }
                                        }.onSuccess { refresh() }
                                            .onFailure { error = it.message ?: "Could not duplicate Look" }
                                    }
                                }) { Text(stringResource(R.string.looks_duplicate)) }
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) { repository.delete(look.id) }
                                        }.onSuccess { refresh() }
                                            .onFailure { error = it.message ?: "Could not delete Look" }
                                    }
                                }) { Text(stringResource(R.string.looks_delete)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog || editTarget != null) {
        val saving = showSaveDialog
        AlertDialog(
            onDismissRequest = { showSaveDialog = false; editTarget = null },
            title = {
                Text(stringResource(if (saving) R.string.looks_save_title else R.string.looks_rename_title))
            },
            text = {
                Column(Modifier.imePadding()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.looks_name_label)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val current = currentLook
                    val target = editTarget
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                if (saving) {
                                    requireNotNull(current).let {
                                        repository.save(it.copy(id = UUID.randomUUID().toString(), name = name.trim()))
                                    }
                                } else if (target != null) {
                                    repository.rename(target.id, name)
                                }
                            }
                        }.onSuccess {
                            showSaveDialog = false
                            editTarget = null
                            refresh()
                        }.onFailure { error = it.message ?: "Could not save Look" }
                    }
                }) { Text(stringResource(R.string.looks_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false; editTarget = null }) {
                    Text(stringResource(R.string.batch_cancel))
                }
            },
        )
    }
}

private fun BatchItemState.stringId(): Int = when (this) {
    BatchItemState.QUEUED -> R.string.batch_state_queued
    BatchItemState.PROCESSING -> R.string.batch_state_processing
    BatchItemState.COMPOSED -> R.string.batch_state_composed
    BatchItemState.FAILED -> R.string.batch_state_failed
    BatchItemState.CANCELLED -> R.string.batch_state_cancelled
}

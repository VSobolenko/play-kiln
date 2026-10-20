package com.gexetr.plinthproductphotos.ui.library

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.db.ProjectSummary
import com.gexetr.plinthproductphotos.ui.LocalPhotoSharedTransitionScope
import com.gexetr.plinthproductphotos.ui.PhotoJourney
import com.gexetr.plinthproductphotos.ui.PhotoTransitionKey
import com.gexetr.plinthproductphotos.ui.components.projectDisplayTitle
import com.gexetr.plinthproductphotos.ui.theme.PlinthMotion
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.Date

private const val SkeletonCount = 6

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenProject: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenPhoto: ((String) -> Unit)? = null,
    sharedPhotoTransitionsEnabled: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val paging = viewModel.projects.collectAsLazyPagingItems()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showMarketplace by rememberSaveable { mutableStateOf(false) }
    var compact by rememberSaveable { mutableStateOf(false) }
    val selectionMode = selecting || state.selectedProjectIds.isNotEmpty()
    val filtersActive = state.category.isNotBlank() || state.marketplaceId.isNotBlank() ||
        state.updatedAfter.isNotBlank() || state.updatedBefore.isNotBlank()
    val hasFilters = filtersActive || state.searchQuery.isNotBlank()
    val refresh = paging.loadState.refresh
    val append = paging.loadState.append
    val exitSelection = {
        viewModel.clearSelection()
        selecting = false
    }

    BackHandler(enabled = selectionMode, onBack = exitSelection)

    // The host already supplies PlinthScaffold, PlinthTopBar and project navigation.
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::search,
                    placeholder = { Text(stringResource(R.string.library_search), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_lucide_search), contentDescription = null)
                    },
                    trailingIcon = if (state.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.search("") }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    painterResource(R.drawable.ic_lucide_x),
                                    contentDescription = stringResource(R.string.library_search_clear),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                FilledTonalIconButton(
                    onClick = { showFilters = true },
                    modifier = Modifier.size(48.dp),
                    colors = if (filtersActive) {
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        IconButtonDefaults.filledTonalIconButtonColors()
                    },
                ) {
                    Icon(
                        painterResource(R.drawable.ic_lucide_list_filter),
                        contentDescription = stringResource(
                            if (filtersActive) R.string.library_browser_filters_active else R.string.library_browser_filters,
                        ),
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                val loadedCount = paging.itemCount
                val moreToLoad = !(append is LoadState.NotLoading && append.endOfPaginationReached)
                Text(
                    text = when {
                        loadedCount == 0 -> stringResource(R.string.library_browser_projects)
                        moreToLoad -> stringResource(
                            R.string.library_count_sorted,
                            stringResource(R.string.library_count_more, loadedCount),
                        )
                        else -> stringResource(
                            R.string.library_count_sorted,
                            pluralStringResource(R.plurals.library_count_projects, loadedCount, loadedCount),
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).widthIn(min = 120.dp).semantics { heading() },
                )
                if (hasFilters) {
                    TextButton(onClick = viewModel::clearFilters, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.library_clear_filters))
                    }
                }
                TextButton(onClick = { compact = !compact }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(if (compact) R.string.library_density_comfortable else R.string.library_density_compact))
                }
                if (!selectionMode) {
                    TextButton(onClick = { selecting = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.library_browser_select))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(3.dp)) {
                if (refresh is LoadState.Loading && paging.itemCount > 0) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
        OperationBanner(state = state, onDismiss = viewModel::dismissResults)
        Box(Modifier.weight(1f)) {
            LibraryGrid(
                paging = paging,
                state = state,
                compact = compact,
                selectionMode = selectionMode,
                hasFilters = hasFilters,
                onToggle = viewModel::toggleProject,
                onLongPress = { id ->
                    selecting = true
                    if (id !in state.selectedProjectIds) viewModel.toggleProject(id)
                },
                onClearFilters = viewModel::clearFilters,
                onOpenProject = onOpenProject,
                onOpenPhoto = onOpenPhoto,
                sharedPhotoTransitionsEnabled = sharedPhotoTransitionsEnabled,
            )
        }
        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInVertically(tween(180)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(150)) { it } + fadeOut(tween(150)),
        ) {
            SelectionBar(
                count = state.selectedProjectIds.size,
                canSelectLoaded = paging.itemCount > 0,
                onClose = exitSelection,
                onToggleLoaded = { viewModel.toggleProjects(paging.itemSnapshotList.items.map { it.project.id }) },
                onMarketplace = { showMarketplace = true },
                onReExport = viewModel::reExportSelected,
                onDelete = { confirmDelete = true },
            )
        }
    }
    if (showFilters) {
        LibraryFilters(state, viewModel, onDismiss = { showFilters = false })
    }
    if (showMarketplace && state.selectedProjectIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showMarketplace = false },
            title = { Text(stringResource(R.string.library_marketplace_apply)) },
            text = {
                OutlinedTextField(
                    value = state.marketplaceTarget,
                    onValueChange = viewModel::setMarketplaceTarget,
                    label = { Text(stringResource(R.string.library_marketplace_target)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMarketplace = false
                        viewModel.changeMarketplaceForSelected(state.marketplaceTarget)
                    },
                    enabled = state.marketplaceTarget.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.library_marketplace_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showMarketplace = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.library_browser_cancel))
                }
            },
        )
    }
    if (confirmDelete && state.selectedProjectIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.library_delete_selected)) },
            text = { Text(stringResource(R.string.library_browser_delete_confirm, state.selectedProjectIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteSelectedProjects()
                }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.library_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.library_browser_cancel))
                }
            },
        )
    }
}

@Composable
private fun LibraryGrid(
    paging: LazyPagingItems<ProjectSummary>,
    state: LibraryUiState,
    compact: Boolean,
    selectionMode: Boolean,
    hasFilters: Boolean,
    onToggle: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onClearFilters: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenPhoto: ((String) -> Unit)?,
    sharedPhotoTransitionsEnabled: Boolean,
) {
    val refresh = paging.loadState.refresh
    val append = paging.loadState.append
    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (compact) 104.dp else 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        when {
            refresh is LoadState.Loading && paging.itemCount == 0 -> {
                item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                    LoadingLabel(stringResource(R.string.library_browser_loading))
                }
                items(count = SkeletonCount, key = { "skeleton-$it" }) { SkeletonTile() }
            }
            refresh is LoadState.Error -> {
                item(key = "refresh-error", span = { GridItemSpan(maxLineSpan) }) {
                    LibraryStatus(
                        text = stringResource(R.string.library_load_error, refresh.error.localizedMessage.orEmpty()),
                        error = true,
                        actionLabel = stringResource(R.string.library_browser_retry),
                        onAction = { paging.retry() },
                    )
                }
            }
            paging.itemCount == 0 -> {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    LibraryStatus(
                        text = stringResource(if (hasFilters) R.string.library_no_results else R.string.library_empty),
                        detail = if (hasFilters) null else stringResource(R.string.library_browser_empty_hint),
                        actionLabel = if (hasFilters) stringResource(R.string.library_clear_filters) else null,
                        onAction = if (hasFilters) onClearFilters else null,
                    )
                }
            }
        }
        items(count = paging.itemCount, key = paging.itemKey { it.project.id }) { index ->
            paging[index]?.let { project ->
                val id = project.project.id
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(PlinthMotion.enterStagger(index)) +
                        scaleIn(initialScale = 0.94f, animationSpec = PlinthMotion.enterStagger(index)),
                ) {
                    ProjectTile(
                        project = project,
                        selected = id in state.selectedProjectIds,
                        selectionMode = selectionMode,
                        onToggle = { onToggle(id) },
                        onOpen = { onOpenProject(id) },
                        onOpenPhoto = onOpenPhoto?.let { openPhoto -> { openPhoto(id) } },
                        sharedPhotoTransitionsEnabled = sharedPhotoTransitionsEnabled,
                        onLongPress = { onLongPress(id) },
                        compact = compact,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        if (append is LoadState.Loading || append is LoadState.Error) {
            item(key = "append-state", span = { GridItemSpan(maxLineSpan) }) {
                if (append is LoadState.Error) {
                    LibraryStatus(
                        text = stringResource(R.string.library_load_error, append.error.localizedMessage.orEmpty()),
                        error = true,
                        actionLabel = stringResource(R.string.library_browser_retry),
                        onAction = { paging.retry() },
                    )
                } else {
                    LoadingLabel(stringResource(R.string.library_browser_loading))
                }
            }
        }
    }
}

@Composable
private fun OperationBanner(state: LibraryUiState, onDismiss: () -> Unit) {
    val message = state.message
    val results = state.operationResults
    if (message == null && results.isEmpty() && state.filterError == null) return
    val failed = results.filterNot { it.succeeded }
    Surface(
        color = if (message != null || failed.isNotEmpty() || state.filterError != null) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 4.dp)) {
            if (state.filterError != null) {
                Text(stringResource(R.string.library_date_invalid), style = MaterialTheme.typography.bodyMedium)
            }
            if (message != null) {
                Text(
                    text = if (message == "marketplace-required") {
                        stringResource(R.string.library_marketplace_required)
                    } else {
                        stringResource(R.string.library_operation_failed, message)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (results.isNotEmpty()) {
                Text(
                    stringResource(R.string.library_results_summary, results.size - failed.size, failed.size),
                    style = MaterialTheme.typography.titleSmall,
                )
                failed.take(3).forEach { result ->
                    Text(
                        stringResource(R.string.library_item_operation_failed, result.itemId, result.error.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (results.isNotEmpty() || message != null) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).align(Alignment.End)) {
                    Text(stringResource(R.string.library_browser_dismiss))
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    canSelectLoaded: Boolean,
    onClose: () -> Unit,
    onToggleLoaded: () -> Unit,
    onMarketplace: () -> Unit,
    onReExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_lucide_x),
                        contentDescription = stringResource(R.string.library_selection_exit),
                    )
                }
                Text(
                    stringResource(R.string.library_selected, count),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onToggleLoaded,
                    enabled = canSelectLoaded,
                    modifier = Modifier.heightIn(min = 48.dp).widthIn(max = 180.dp),
                ) {
                    Text(
                        stringResource(R.string.library_select_all),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                        textAlign = TextAlign.End,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BarAction(
                    R.drawable.ic_lucide_external_link, stringResource(R.string.library_marketplace_apply),
                    count > 0, onMarketplace, Modifier.weight(1f),
                )
                BarAction(
                    R.drawable.ic_lucide_refresh_cw, stringResource(R.string.library_reexport),
                    count > 0, onReExport, Modifier.weight(1f),
                )
                BarAction(
                    R.drawable.ic_lucide_trash_2, stringResource(R.string.library_delete),
                    count > 0, onDelete, Modifier.weight(1f), destructive = true,
                )
            }
        }
    }
}

@Composable
private fun BarAction(
    icon: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        colors = if (destructive) {
            ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.textButtonColors()
        },
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun ProjectTile(
    project: ProjectSummary,
    selected: Boolean,
    selectionMode: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPhoto: (() -> Unit)? = null,
    sharedPhotoTransitionsEnabled: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val preview = remember(context, project.project.previewPath) {
        project.project.previewPath?.let { safePreviewFile(context, it) }
    }
    val title = projectDisplayTitle(project.project.name, project.project.createdAt)
    val selectDescription = stringResource(R.string.library_project_selected_description, title)
    val updated = remember(project.project.updatedAt) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(project.project.updatedAt))
    }
    val marketplace = project.project.marketplaceId?.takeIf(String::isNotBlank)
    val photoTransition = LocalPhotoSharedTransitionScope.current?.takeIf { sharedPhotoTransitionsEnabled }?.let { sharedScope ->
        with(sharedScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(PhotoTransitionKey(project.project.id, journey = PhotoJourney.LIBRARY_CARD)),
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            )
        }
    } ?: Modifier
    val borderColor by animateColorAsState(
        targetValue = if (selected) PlinthTheme.accentInk else Color.Transparent,
        animationSpec = tween(120),
        label = "library-tile-border",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
        ),
        shadowElevation = if (selected) 8.dp else 3.dp,
    ) {
        Column(
            Modifier
                .combinedClickable(
                    onClick = if (selectionMode) onToggle else onOpen,
                    onLongClick = onLongPress,
                )
                .semantics { if (selectionMode) this.selected = selected },
        ) {
            // 4:5 portrait with Crop: product photos are taller than wide, so the image fills the tile.
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                ProjectPreview(
                    file = preview,
                    projectName = title,
                    modifier = Modifier.fillMaxSize().then(photoTransition).then(
                        if (!selectionMode && onOpenPhoto != null) {
                            Modifier.clickable(
                                onClickLabel = stringResource(R.string.card_inspect),
                                onClick = onOpenPhoto,
                            )
                        } else {
                            Modifier
                        },
                    ),
                )
                if (selected) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)))
                }
                if (marketplace != null && !compact) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    ) {
                        Text(
                            marketplace,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                if (selectionMode) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(onClickLabel = selectDescription, onClick = onToggle)
                                .semantics { contentDescription = selectDescription },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(if (selected) R.drawable.ic_lucide_check else R.drawable.ic_lucide_image),
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            Column(
                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.library_items, project.itemCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!compact) {
                        project.primarySku?.takeIf(String::isNotBlank)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (!compact) {
                    Text(
                        stringResource(R.string.library_browser_updated, updated),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectPreview(file: File?, projectName: String, modifier: Modifier = Modifier) {
    val unavailable = stringResource(R.string.library_browser_preview_unavailable)
    if (file == null) {
        PreviewPlaceholder(unavailable, modifier)
        return
    }
    var loaded by remember(file) { mutableStateOf(false) }
    var failed by remember(file) { mutableStateOf(false) }
    Box(modifier) {
        AsyncImage(
            model = file,
            contentDescription = stringResource(R.string.library_preview_description, projectName),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onState = { result ->
                loaded = result is AsyncImagePainter.State.Success
                failed = result is AsyncImagePainter.State.Error
            },
        )
        if (!loaded) {
            PreviewPlaceholder(
                if (failed) unavailable else stringResource(R.string.library_browser_preview_loading),
                Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PreviewPlaceholder(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_lucide_image),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SkeletonTile() {
    val pulse by rememberInfiniteTransition(label = "library-skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "library-skeleton-alpha",
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().alpha(pulse),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(MaterialTheme.colorScheme.surfaceContainerHighest))
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.8f).height(12.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp)))
                Box(Modifier.fillMaxWidth(0.5f).height(10.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(5.dp)))
            }
        }
    }
}

@Composable
private fun LoadingLabel(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LibraryStatus(
    text: String,
    detail: String? = null,
    error: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                painterResource(if (error) R.drawable.ic_lucide_triangle_alert else R.drawable.ic_lucide_image),
                contentDescription = null,
            )
            Text(text, style = MaterialTheme.typography.titleMedium)
            detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun LibraryFilters(state: LibraryUiState, viewModel: LibraryViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_browser_filters)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.category,
                    onValueChange = viewModel::setCategory,
                    label = { Text(stringResource(R.string.library_category_filter)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.marketplaceId,
                    onValueChange = viewModel::setMarketplace,
                    label = { Text(stringResource(R.string.library_marketplace_filter)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.updatedAfter,
                    onValueChange = viewModel::setUpdatedAfter,
                    label = { Text(stringResource(R.string.library_updated_after)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.filterError != null,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.updatedBefore,
                    onValueChange = viewModel::setUpdatedBefore,
                    label = { Text(stringResource(R.string.library_updated_before)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.filterError != null,
                    singleLine = true,
                )
                if (state.filterError != null) {
                    Text(stringResource(R.string.library_date_invalid), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.library_browser_done))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::clearFilters, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.library_clear_filters))
            }
        },
    )
}

private fun safePreviewFile(context: Context, path: String): File? = try {
    val filesRoot = context.filesDir.canonicalFile
    val candidate = File(path).let { if (it.isAbsolute) it else File(filesRoot, path) }.canonicalFile
    candidate.takeIf { it.toPath().startsWith(filesRoot.toPath()) && it.isFile && it.canRead() }
} catch (failure: IOException) {
    Log.w("LibraryPreview", "Could not resolve project preview", failure)
    null
}

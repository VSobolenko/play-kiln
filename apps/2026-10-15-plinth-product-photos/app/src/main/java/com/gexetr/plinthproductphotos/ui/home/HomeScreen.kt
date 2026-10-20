package com.gexetr.plinthproductphotos.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.domain.autocard.AutoCategory
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationCatalog
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationId
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationRequest
import com.gexetr.plinthproductphotos.ui.studio.studioRows
import com.gexetr.plinthproductphotos.ui.components.AuroraField
import com.gexetr.plinthproductphotos.ui.components.rememberPhotoSourceFlow
import com.gexetr.plinthproductphotos.ui.components.ShowcaseStrip
import com.gexetr.plinthproductphotos.ui.components.ToolExample
import com.gexetr.plinthproductphotos.ui.components.ToolExampleArt
import com.gexetr.plinthproductphotos.ui.components.projectDisplayTitle
import com.gexetr.plinthproductphotos.ui.theme.PlinthMotion
import java.io.File

@Composable
fun HomeRoute(
    onNavigateToImport: () -> Unit,
    onImportUri: (android.net.Uri) -> Unit,
    onNavigateToPhoto: (String) -> Unit,
    onNavigateToListingCard: (String) -> Unit,
    onNavigateToAutoCard: (String, AutoCategory?) -> Unit,
    onNavigateToSlideKit: (String) -> Unit,
    onNavigateToCutout: () -> Unit,
    onNavigateToCoach: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToListingText: () -> Unit,
    onNavigateToStudioTools: (StudioOperationId) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenWallet: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(LocalContext.current)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onImport = onNavigateToImport,
        onImportUri = onImportUri,
        onOpenPhoto = onNavigateToPhoto,
        onOpenListingCard = onNavigateToListingCard,
        onMakeAutoCard = onNavigateToAutoCard,
        onOpenSlideKit = onNavigateToSlideKit,
        onCutoutClick = onNavigateToCutout,
        onCoachClick = onNavigateToCoach,
        onLibraryClick = onNavigateToLibrary,
        onExportClick = onNavigateToExport,
        onListingTextClick = onNavigateToListingText,
        onStudioToolsClick = onNavigateToStudioTools,
        onRetry = viewModel::refresh,
        onAboutClick = onNavigateToAbout,
        onSettingsClick = onNavigateToSettings,
        onOpenWallet = onOpenWallet,
        hapticsEnabled = hapticsEnabled,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onImport: () -> Unit,
    onImportUri: (android.net.Uri) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onOpenListingCard: (String) -> Unit,
    onMakeAutoCard: (String, AutoCategory?) -> Unit,
    onOpenSlideKit: (String) -> Unit,
    onCutoutClick: () -> Unit,
    onCoachClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onExportClick: () -> Unit,
    onListingTextClick: () -> Unit,
    onStudioToolsClick: (StudioOperationId) -> Unit,
    onRetry: () -> Unit,
    onAboutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenWallet: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val latest = state.recentPhotos.firstOrNull()
    val photoSourceFlow = rememberPhotoSourceFlow { uri -> onImportUri(uri) }
    val importPhoto = {
        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        photoSourceFlow()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "hero") {
            HomeEntrance(0) { StudioHero(state = state, onImport = importPhoto, onOpenPhoto = onOpenPhoto) }
        }
        if (state.loadError) {
            item(key = "load-error") {
                HomeEntrance(1) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_triangle_alert),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.home_load_failed), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    stringResource(R.string.home_load_retry_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = .78f),
                                )
                            }
                            Button(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.plinth_retry))
                            }
                        }
                    }
                }
            }
        }
        if (latest == null) {
            item(key = "showcase") {
                HomeEntrance(1) { ShowcaseStrip(modifier = Modifier.fillMaxWidth()) }
            }
        }
        item(key = "tools-heading") {
            HomeEntrance(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            stringResource(R.string.home_toolbox_title).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.home_tool_count, homeTools.size, homeTools.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AssistChip(
                        onClick = onOpenWallet,
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(stringResource(R.string.home_toolbox_note)) },
                        leadingIcon = { ToolboxWalletIcon() },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
        }
        items(
            items = homeTools.chunked(2),
            key = { tools -> "tools-row-${tools.first().action}-${tools.first().operation.name}" },
        ) { tools ->
            HomeEntrance(2 + homeTools.indexOf(tools.first())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    tools.forEach { tool ->
                        HomeToolCard(
                            tool = tool,
                            onCutoutClick = onCutoutClick,
                            onListingCardClick = { latest?.let { onOpenListingCard(it.id) } ?: importPhoto() },
                            onStudioToolsClick = onStudioToolsClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (tools.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
        if (state.isLoading || state.recentPhotos.size > 1) {
            item(key = "continue") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(R.string.home_continue_title))
                    if (state.isLoading) {
                        RecentLoadingRail()
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.recentPhotos.drop(1), key = ProjectPhoto::id) { photo ->
                                RecentPhotoCard(
                                    photo = photo,
                                    onOpen = { onOpenPhoto(photo.id) },
                                    onMarketplaceCard = { onMakeAutoCard(photo.id, null) },
                                )
                            }
                        }
                    }
                }
            }
        }
        item(key = "workflows-heading") {
            SectionTitle(stringResource(R.string.home_workflow_more))
        }
        item(key = "workflows") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "coach") {
                    WorkflowAction(
                        label = stringResource(R.string.home_nav_coach),
                        icon = R.drawable.ic_lucide_camera,
                        onClick = onCoachClick,
                    )
                }
                item(key = "slide-kit") {
                    WorkflowAction(
                        label = stringResource(R.string.home_slide_kit_separate),
                        icon = R.drawable.ic_lucide_image,
                        onClick = {
                            if (latest == null) importPhoto() else onOpenSlideKit(latest.id)
                        },
                    )
                }
                item(key = "listing-text") {
                    WorkflowAction(
                        label = stringResource(R.string.home_listing_text_separate),
                        icon = R.drawable.ic_lucide_list_filter,
                        onClick = onListingTextClick,
                    )
                }
                item(key = "export") {
                    WorkflowAction(
                        label = stringResource(R.string.home_nav_export),
                        icon = R.drawable.ic_lucide_download,
                        onClick = onExportClick,
                    )
                }
                item(key = "library") {
                    WorkflowAction(
                        label = stringResource(R.string.home_nav_library),
                        icon = R.drawable.ic_lucide_image,
                        onClick = onLibraryClick,
                    )
                }
                item(key = "settings") {
                    WorkflowAction(
                        label = stringResource(R.string.settings_title),
                        icon = R.drawable.ic_lucide_list_filter,
                        onClick = onSettingsClick,
                    )
                }
                item(key = "about") {
                    WorkflowAction(
                        label = stringResource(R.string.home_about_action),
                        icon = R.drawable.ic_lucide_list_filter,
                        onClick = onAboutClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioHero(
    state: HomeUiState,
    onImport: () -> Unit,
    onOpenPhoto: (String) -> Unit,
) {
    val context = LocalContext.current
    val photo = state.recentPhotos.firstOrNull()
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1.45f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp)),
        )
        return
    }
    val ambient = rememberInfiniteTransition(label = "home-hero-ambient").animateFloat(
        initialValue = .78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 6000)),
        label = "home-hero-aurora-alpha",
    ).value
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.45f).clip(RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        AuroraField(Modifier.matchParentSize().alpha(ambient), intensity = .82f)
        Surface(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .88f),
            shadowElevation = 10.dp,
        ) {
            if (photo == null) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.home_hero_promise), style = MaterialTheme.typography.headlineSmall)
                    HeroImportButton(onClick = onImport)
                    Text(
                        stringResource(R.string.home_hero_quiet_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LatestPhotoStage(
                    photo = photo,
                    source = remember(context, photo.sourcePath) { File(context.filesDir, photo.sourcePath) },
                    onImport = onImport,
                    onOpen = { onOpenPhoto(photo.id) },
                )
            }
        }
    }
}

@Composable
private fun ToolboxWalletIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_lucide_plus),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun LatestPhotoStage(photo: ProjectPhoto, source: File, onImport: () -> Unit, onOpen: () -> Unit) {
    val changePhotoLabel = stringResource(R.string.home_change_photo)
    val openLabel = stringResource(R.string.home_continue_action)
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubcomposeAsyncImage(
            model = source,
            contentDescription = stringResource(R.string.home_preview_original),
            modifier = Modifier.fillMaxWidth().aspectRatio(1.7f)
                .shadow(8.dp, RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop,
            loading = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) },
            error = {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_triangle_alert),
                        contentDescription = stringResource(R.string.home_thumbnail_failed),
                    )
                }
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                projectDisplayTitle(photo.name, photo.importedAt),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FilledTonalIconButton(onClick = onImport, modifier = Modifier.size(48.dp).semantics { contentDescription = changePhotoLabel }) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_plus),
                    contentDescription = null,
                )
            }
            FilledIconButton(onClick = onOpen, modifier = Modifier.size(48.dp).semantics { contentDescription = openLabel }) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_image),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun HeroImportButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, PlinthMotion.pressScale, label = "hero-import-press")
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Icon(painterResource(R.drawable.ic_lucide_plus), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.home_import_photo))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun HomeToolCard(
    tool: HomeTool,
    onCutoutClick: () -> Unit,
    onListingCardClick: () -> Unit,
    onStudioToolsClick: (StudioOperationId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .97f else 1f,
        animationSpec = PlinthMotion.pressScale,
        label = "home-tool-press",
    )
    val cost = remember(tool.operation, tool.cost) {
        tool.cost ?: StudioOperationCatalog.costFor(StudioOperationRequest(tool.operation)).credits
    }
    val costLabel = pluralStringResource(R.plurals.home_cost_chip, cost, cost)
    val title = stringResource(tool.title)
    val description = stringResource(tool.description)
    val accessibleLabel = stringResource(
        R.string.home_tool_accessibility_description,
        title,
        description,
        costLabel,
    )
    val onToolClick: () -> Unit = when (tool.action) {
        HomeToolAction.CUT_OUT -> onCutoutClick
        HomeToolAction.LISTING_CARD -> onListingCardClick
        HomeToolAction.STUDIO -> { { onStudioToolsClick(tool.operation) } }
    }
    Surface(
        onClick = onToolClick,
        modifier = modifier
            .heightIn(min = 150.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleLabel
            },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolExampleArt(
                example = tool.example,
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .aspectRatio(1.52f)
                    .clip(RoundedCornerShape(18.dp)),
            )
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = costLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = PlinthTheme.accentInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RecentPhotoCard(
    photo: ProjectPhoto,
    onOpen: () -> Unit,
    onMarketplaceCard: () -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(min = 180.dp, max = 240.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeThumbnail(photo, Modifier.fillMaxWidth().aspectRatio(1.15f))
                    Text(
                        text = projectDisplayTitle(photo.name, photo.importedAt),
                        modifier = Modifier.padding(horizontal = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(
                onClick = onMarketplaceCard,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.home_marketplace_card_separate))
            }
        }
    }
}

@Composable
private fun HomeThumbnail(photo: ProjectPhoto, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = remember(photo.sourcePath, context) { File(context.filesDir, photo.sourcePath) }
    SubcomposeAsyncImage(
        model = model,
        contentDescription = stringResource(R.string.plinth_recent_photo_content_description, photo.name),
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        contentScale = ContentScale.Crop,
        loading = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_lucide_triangle_alert),
                    contentDescription = stringResource(R.string.home_thumbnail_failed),
                )
            }
        },
    )
}

@Composable
private fun RecentLoadingRail() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(2) {
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1.3f)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(20.dp),
                    ),
            )
        }
    }
}

@Composable
private fun WorkflowAction(
    label: String,
    icon: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 148.dp).heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = PlinthTheme.accentInk,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal data class HomeTool(
    val action: HomeToolAction,
    val operation: StudioOperationId,
    val title: Int,
    val description: Int,
    val icon: Int,
    val example: ToolExample,
    val cost: Int? = null,
)

internal enum class HomeToolAction { CUT_OUT, LISTING_CARD, STUDIO }

// Cutout runs on its own screen; every other tool comes from the Studio list so both screens agree.
internal val homeTools: List<HomeTool> = listOf(
    HomeTool(
        HomeToolAction.CUT_OUT,
        StudioOperationId.CUT_OUT,
        R.string.studio_operation_cut_out,
        R.string.home_cutout_description,
        R.drawable.ic_lucide_image,
        ToolExample.CUT_OUT,
    ),
    HomeTool(
        HomeToolAction.LISTING_CARD,
        StudioOperationId.COMPOSE_ORIGINAL,
        R.string.home_listing_card_title,
        R.string.home_listing_card_description,
        R.drawable.ic_lucide_image,
        ToolExample.LISTING_CARD,
        cost = 0,
    ),
) + studioRows.map {
    HomeTool(
        HomeToolAction.STUDIO, it.homeOperation, it.name, it.purpose, it.icon,
        it.homeOperation.toolExample(),
        it.credits,
    )
}

private fun StudioOperationId.toolExample(): ToolExample = when (this) {
    StudioOperationId.RECOGNISE,
    StudioOperationId.CUT_OUT,
    StudioOperationId.REFINE_MASK -> ToolExample.CUT_OUT
    StudioOperationId.COMPOSE_ORIGINAL -> ToolExample.LISTING_CARD
    StudioOperationId.SHADOW_MARGIN_FILL -> ToolExample.SHADOW_FILL
    StudioOperationId.LOCAL_BACKGROUND -> ToolExample.LOCAL_BACKGROUND
    StudioOperationId.GENERATED_SCENE -> ToolExample.GENERATED_SCENE
    StudioOperationId.RELIGHT -> ToolExample.RELIGHT
    StudioOperationId.EXPAND_CANVAS -> ToolExample.EXPAND_CANVAS
    StudioOperationId.RECOLOUR -> ToolExample.RECOLOUR
    StudioOperationId.UPSCALE -> ToolExample.UPSCALE
    StudioOperationId.ERASE_REGION -> ToolExample.ERASE_REGION
    StudioOperationId.GHOST_MANNEQUIN -> ToolExample.GHOST_MANNEQUIN
    StudioOperationId.VIRTUAL_MODEL -> ToolExample.VIRTUAL_MODEL
    StudioOperationId.LISTING_TEXT,
    StudioOperationId.SERIES,
    StudioOperationId.SLIDE_RENDER,
    StudioOperationId.EXPORT_PACK -> ToolExample.LISTING_CARD
}

@Composable
private fun HomeEntrance(index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = PlinthMotion.enterStagger(index))) {
        content()
    }
}

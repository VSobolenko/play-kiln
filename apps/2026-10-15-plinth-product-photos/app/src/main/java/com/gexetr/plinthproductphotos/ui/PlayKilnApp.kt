package com.gexetr.plinthproductphotos.ui

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.coach.CoachCaptureHandoff
import com.gexetr.plinthproductphotos.data.export.ExportDelivery
import com.gexetr.plinthproductphotos.data.look.LookRepository
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImage
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImageLoader
import com.gexetr.plinthproductphotos.data.project.ProjectOutputRepository
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.data.project.StoredProjectOutput
import com.gexetr.plinthproductphotos.data.project.selectSavedOutput
import com.gexetr.plinthproductphotos.data.repository.LibraryRepository
import com.gexetr.plinthproductphotos.domain.listing.ListingAttributes
import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePresetParser
import com.gexetr.plinthproductphotos.domain.presets.PresetParseResult
import com.gexetr.plinthproductphotos.domain.slide.SlideMarketplaceRules
import com.gexetr.plinthproductphotos.domain.slide.SlideSeriesBackground
import com.gexetr.plinthproductphotos.domain.slide.SlideStyle
import com.gexetr.plinthproductphotos.ui.about.AboutScreen
import com.gexetr.plinthproductphotos.domain.autocard.AutoCategory
import com.gexetr.plinthproductphotos.domain.export.WatermarkGate
import com.gexetr.plinthproductphotos.ui.autocard.GeneratedSceneRoute
import com.gexetr.plinthproductphotos.ui.autocard.AutoCardResultsViewModel
import com.gexetr.plinthproductphotos.ui.autocard.FormingCardPreview
import com.gexetr.plinthproductphotos.ui.autocard.autoCardPreviewBitmap
import com.gexetr.plinthproductphotos.ui.batch.BatchScreen
import com.gexetr.plinthproductphotos.ui.batch.LooksScreen
import com.gexetr.plinthproductphotos.ui.card.CardRoute
import com.gexetr.plinthproductphotos.ui.coach.CoachRoute
import com.gexetr.plinthproductphotos.ui.components.PlinthBottomNavBar
import com.gexetr.plinthproductphotos.ui.components.PlinthNavigationRail
import com.gexetr.plinthproductphotos.ui.components.PlinthSection
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.components.PlinthTopBar
import com.gexetr.plinthproductphotos.ui.cutout.CutoutRoute
import com.gexetr.plinthproductphotos.ui.editor.EditorRoute
import com.gexetr.plinthproductphotos.ui.editor.LocalEditorSavedAction
import com.gexetr.plinthproductphotos.ui.export.ExportPackEmptyReason
import com.gexetr.plinthproductphotos.ui.export.ExportPackSourceState
import com.gexetr.plinthproductphotos.ui.export.ExportScreen
import com.gexetr.plinthproductphotos.ui.export.rememberExportPackSource
import com.gexetr.plinthproductphotos.ui.group.GroupScreen
import com.gexetr.plinthproductphotos.ui.home.HomeViewModel
import com.gexetr.plinthproductphotos.ui.home.MoreScreen
import com.gexetr.plinthproductphotos.ui.home.HomeRoute
import com.gexetr.plinthproductphotos.ui.importscreen.ImportRoute
import com.gexetr.plinthproductphotos.ui.library.LibraryScreen
import com.gexetr.plinthproductphotos.ui.library.LibraryViewModel
import com.gexetr.plinthproductphotos.ui.listing.ListingTextScreen
import com.gexetr.plinthproductphotos.ui.profile.ProfileRoute
import com.gexetr.plinthproductphotos.ui.settings.SettingsRoute
import com.gexetr.plinthproductphotos.ui.slide.SlideKitScreen
import com.gexetr.plinthproductphotos.ui.studio.StudioRoute
import com.gexetr.plinthproductphotos.ui.studio.StudioViewModel
import com.gexetr.plinthproductphotos.ui.studio.studioViewModelFactory
import com.gexetr.plinthproductphotos.ui.wallet.WalletScreen
import com.gexetr.plinthproductphotos.ui.wallet.WalletViewModel
import com.gexetr.plinthproductphotos.ui.wallet.ServerUnlock
import com.gexetr.plinthproductphotos.ui.theme.PlinthMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
private data object HomeDestination : NavKey

@Serializable
private data object MoreDestination : NavKey

@Serializable
private data object AboutDestination : NavKey

@Serializable
private data object SettingsDestination : NavKey

@Serializable
private data class ImportDestination(
    val launchPickerOnEnter: Boolean = false,
    val prefillUri: String? = null,
) : NavKey

@Serializable
private data class CardDestination(val photoId: String) : NavKey

@Serializable
private data class EditorDestination(val photoId: String) : NavKey

@Serializable
private data class CutoutDestination(val photoId: String?) : NavKey

@Serializable
private data class AutoCardDestination(
    val photoId: String,
    val category: AutoCategory? = null,
) : NavKey

@Serializable
private data class AutoCardResultDestination(val photoId: String) : NavKey

@Serializable
private data class SavedProjectCardDestination(
    val itemId: String,
    val outputId: String? = null,
) : NavKey

@Serializable
private data object ExportDestination : NavKey

@Serializable
private data object LibraryDestination : NavKey

@Serializable
private data object ProfileDestination : NavKey

@Serializable
private data object BatchHubDestination : NavKey

@Serializable
private data class ProjectDestination(val projectId: String) : NavKey

@Serializable
private data class GroupDestination(val projectId: String) : NavKey

@Serializable
private data class LooksDestination(val projectId: String) : NavKey

@Serializable
private data class BatchDestination(val projectId: String, val lookId: String) : NavKey

@Serializable
private data class SlideKitDestination(val photoId: String) : NavKey

@Serializable
private data object ListingTextDestination : NavKey

@Serializable
private data class StudioToolsDestination(
    val operation: String? = null,
    val photoId: String? = null,
) : NavKey

private fun studioOperationFor(name: String?): com.gexetr.plinthproductphotos.data.api.OperationId? =
    when (name) {
        "GENERATED_SCENE" -> com.gexetr.plinthproductphotos.data.api.OperationId.GENERATED_SCENE
        "RELIGHT" -> com.gexetr.plinthproductphotos.data.api.OperationId.RELIGHT
        "RECOLOUR" -> com.gexetr.plinthproductphotos.data.api.OperationId.RECOLOUR
        "EXPAND_CANVAS" -> com.gexetr.plinthproductphotos.data.api.OperationId.EXPAND
        "ERASE_REGION" -> com.gexetr.plinthproductphotos.data.api.OperationId.ERASE
        "UPSCALE" -> com.gexetr.plinthproductphotos.data.api.OperationId.UPSCALE
        "VIRTUAL_MODEL" -> com.gexetr.plinthproductphotos.data.api.OperationId.VIRTUAL_MODEL
        else -> null
    }

@Serializable
private data object WalletDestination : NavKey

@Serializable
private data class CoachDestination(val projectId: String) : NavKey

private const val SECTION_ROOT_METADATA = "plinth.sectionRoot"

@OptIn(ExperimentalSharedTransitionApi::class)
internal val LocalPhotoSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

internal enum class PhotoJourney { AUTO_CARD, LIBRARY_CARD }

internal data class PhotoTransitionKey(
    val photoId: String,
    val output: Boolean = false,
    val journey: PhotoJourney = PhotoJourney.AUTO_CARD,
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlayKilnApp(hapticsEnabled: Boolean = false) {
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(HomeDestination)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val sceneBackgroundDecorator = remember {
        NavEntryDecorator<NavKey> { entry ->
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                entry.Content()
            }
        }
    }
    val autoCardResults: AutoCardResultsViewModel = viewModel()
    val context = LocalContext.current
    val walletFactory = remember(context) {
        WalletViewModel.factory(context) { context.findActivity() }
    }
    val walletViewModel: WalletViewModel = viewModel(factory = walletFactory)
    val walletState by walletViewModel.state.collectAsStateWithLifecycle()
    val chipBalance by walletViewModel.chipBalance.collectAsStateWithLifecycle(initialValue = null)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, walletViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) walletViewModel.refreshWallet()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val widthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val isWideWindow =
        widthSizeClass == WindowWidthSizeClass.MEDIUM ||
            widthSizeClass == WindowWidthSizeClass.EXPANDED
    val selectedSection = when (backStack.firstOrNull()) {
        LibraryDestination -> PlinthSection.LIBRARY
        ProfileDestination -> PlinthSection.PROFILE
        MoreDestination -> PlinthSection.MORE
        else -> PlinthSection.STUDIO
    }
    val isTopLevel = backStack.size == 1 && backStack.lastOrNull() in setOf(
        HomeDestination,
        LibraryDestination,
        ProfileDestination,
        MoreDestination,
    )
    val slideDistancePx = with(LocalDensity.current) { 24.dp.roundToPx() } *
        if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1

    fun selectSection(section: PlinthSection) {
        val destination = when (section) {
            PlinthSection.STUDIO -> HomeDestination
            PlinthSection.LIBRARY -> LibraryDestination
            PlinthSection.PROFILE -> ProfileDestination
            PlinthSection.MORE -> MoreDestination
        }
        backStack.resetToRoot(destination)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.pop()
        } else if (selectedSection != PlinthSection.STUDIO) {
            selectSection(PlinthSection.STUDIO)
        }
    }

    fun openWallet() {
        backStack.push(WalletDestination)
    }

    fun openCreateFlow() {
        selectSection(PlinthSection.STUDIO)
        backStack.push(ImportDestination(launchPickerOnEnter = true))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        PlinthScaffold(
            topBar = {
                if (isWideWindow && isTopLevel) {
                    PlinthTopBar(
                        section = selectedSection,
                        balance = chipBalance,
                        onOpenWallet = ::openWallet,
                    )
                }
            },
            bottomBar = {
                if (!isWideWindow) {
                    PlinthBottomNavBar(
                        selected = selectedSection,
                        onSelect = ::selectSection,
                        onCreate = ::openCreateFlow,
                    )
                }
            },
        ) {
            Row(Modifier.fillMaxSize()) {
                if (isWideWindow) {
                    PlinthNavigationRail(
                        selected = selectedSection,
                        onSelect = ::selectSection,
                        onCreate = ::openCreateFlow,
                    )
                }
                SharedTransitionLayout(modifier = Modifier.weight(1f)) {
                val sharedTransitionScope = this
                CompositionLocalProvider(LocalPhotoSharedTransitionScope provides sharedTransitionScope) {
                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = backStack,
                    onBack = ::navigateBack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        sceneBackgroundDecorator,
                    ),
                    sceneStrategies = listOf(listDetailStrategy),
                    sharedTransitionScope = sharedTransitionScope,
                    transitionSpec = {
                        snappyNavigationTransition(
                            isPop = false,
                            isSectionSwitch =
                                initialState.entries.lastOrNull()?.metadata
                                    ?.get(SECTION_ROOT_METADATA) == true &&
                                    targetState.entries.lastOrNull()?.metadata
                                        ?.get(SECTION_ROOT_METADATA) == true,
                            slideDistancePx = slideDistancePx,
                        )
                    },
                    popTransitionSpec = {
                        snappyNavigationTransition(isPop = true, slideDistancePx = slideDistancePx)
                    },
                    predictivePopTransitionSpec = {
                        snappyNavigationTransition(isPop = true, slideDistancePx = slideDistancePx)
                    },
                    entryProvider = entryProvider {
                entry<HomeDestination>(
                    metadata = ListDetailSceneStrategy.detailPane() +
                        (SECTION_ROOT_METADATA to true),
                ) {
                    val context = LocalContext.current
                    val homeFactory = remember(context) { HomeViewModel.factory(context) }
                    val homeViewModel: HomeViewModel = viewModel(factory = homeFactory)
                    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
                    SectionRoot(
                        showTopBar = !isWideWindow,
                        section = PlinthSection.STUDIO,
                        balance = chipBalance,
                        onOpenWallet = ::openWallet,
                    ) {
                        HomeRoute(
                            onNavigateToImport = { backStack.push(ImportDestination()) },
                            onImportUri = { uri -> backStack.push(ImportDestination(prefillUri = uri.toString())) },
                            onNavigateToPhoto = { backStack.push(AutoCardDestination(it)) },
                            onNavigateToListingCard = { backStack.push(CardDestination(it)) },
                            onNavigateToAutoCard = { photoId, category ->
                                backStack.push(AutoCardDestination(photoId, category))
                            },
                            onNavigateToSlideKit = { backStack.push(SlideKitDestination(it)) },
                            onNavigateToCutout = { backStack.push(CutoutDestination(homeState.recentPhotos.firstOrNull()?.id)) },
                            onNavigateToCoach = { backStack.push(CoachDestination(COACH_PROJECT_ID)) },
                            onNavigateToLibrary = { selectSection(PlinthSection.LIBRARY) },
                            onNavigateToExport = { backStack.push(ExportDestination) },
                            onNavigateToListingText = { backStack.push(ListingTextDestination) },
                            onNavigateToStudioTools = { operation ->
                                backStack.push(
                                    StudioToolsDestination(
                                        operation = operation.name,
                                        photoId = homeState.recentPhotos.firstOrNull()?.id,
                                    ),
                                )
                            },
                            onNavigateToAbout = { backStack.push(AboutDestination) },
                            onNavigateToSettings = { backStack.push(SettingsDestination) },
                            onOpenWallet = ::openWallet,
                            hapticsEnabled = hapticsEnabled,
                        )
                    }
                }
                entry<MoreDestination>(
                    metadata = ListDetailSceneStrategy.detailPane() +
                        (SECTION_ROOT_METADATA to true),
                ) {
                    SectionRoot(
                        showTopBar = !isWideWindow,
                        section = PlinthSection.MORE,
                        balance = chipBalance,
                        onOpenWallet = ::openWallet,
                    ) {
                        MoreScreen(
                            onOpenWallet = ::openWallet,
                            onOpenSettings = { backStack.push(SettingsDestination) },
                            onOpenAbout = { backStack.push(AboutDestination) },
                        )
                    }
                }
                entry<ProfileDestination>(
                    metadata = ListDetailSceneStrategy.detailPane() +
                        (SECTION_ROOT_METADATA to true),
                ) {
                    SectionRoot(
                        showTopBar = !isWideWindow,
                        section = PlinthSection.PROFILE,
                        balance = chipBalance,
                        onOpenWallet = ::openWallet,
                    ) {
                        ProfileRoute(
                            onOpenWallet = ::openWallet,
                            onOpenLibrary = { selectSection(PlinthSection.LIBRARY) },
                            onOpenSavedResult = { photoId, outputId ->
                                backStack.push(SavedProjectCardDestination(photoId, outputId))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                entry<AboutDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    AboutScreen(onBack = ::navigateBack)
                }
                entry<SettingsDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    SettingsRoute(
                        onBack = ::navigateBack,
                        onOpenWallet = ::openWallet,
                    )
                }
                entry<ImportDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    ImportRoute(
                        onBack = { backStack.pop() },
                        prefillUri = destination.prefillUri,
                        onOpenAutoCard = { photoId -> backStack.push(AutoCardDestination(photoId)) },
                        onOpenLocalCard = { backStack.pop() },
                        launchPickerOnEnter = destination.launchPickerOnEnter,
                    )
                }
                entry<CardDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    CardRoute(
                        photoId = destination.photoId,
                        onBack = { backStack.pop() },
                        onEdit = { backStack.push(EditorDestination(destination.photoId)) },
                    )
                }
                entry<EditorDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    EditorRoute(
                        photoId = destination.photoId,
                        onBack = { backStack.pop() },
                        onSaved = {
                            backStack.pop()
                            backStack.push(SavedProjectCardDestination(destination.photoId))
                        },
                    )
                }
                entry<CutoutDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    CompositionLocalProvider(
                        LocalEditorSavedAction provides { photoId ->
                            backStack.pop()
                            backStack.push(SavedProjectCardDestination(photoId))
                        },
                    ) {
                        WithShellBack(onBack = { backStack.pop() }) {
                            CutoutRoute(onOpenWallet = ::openWallet, initialPhotoId = destination.photoId)
                        }
                    }
                }
                entry<AutoCardDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    GeneratedSceneRoute(
                        photoId = destination.photoId,
                        onBack = { backStack.pop() },
                        onChangePhoto = {
                            backStack.pop()
                            if (backStack.lastOrNull() !is ImportDestination) {
                                backStack.push(ImportDestination())
                            }
                        },
                    )
                }
                entry<AutoCardResultDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    AutoCardResultScreen(
                        photoId = destination.photoId,
                        pixels = autoCardResults[destination.photoId],
                        onBack = { backStack.pop() },
                        onRegenerate = {
                            backStack.pop()
                            val current = backStack.lastOrNull()
                            if (current !is AutoCardDestination || current.photoId != destination.photoId) {
                                backStack.push(AutoCardDestination(destination.photoId))
                            }
                        },
                    )
                }
                entry<SavedProjectCardDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    AutoCardResultScreen(
                        photoId = destination.itemId,
                        outputId = destination.outputId,
                        pixels = null,
                        onBack = { backStack.pop() },
                        onRegenerate = {
                            backStack.pop()
                            val current = backStack.lastOrNull()
                            if (current !is AutoCardDestination || current.photoId != destination.itemId) {
                                backStack.push(AutoCardDestination(destination.itemId))
                            }
                        },
                    )
                }
                entry<ExportDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    val packSource by rememberExportPackSource()
                    when (val loaded = packSource) {
                        ExportPackSourceState.Loading ->
                            PhotoLoadMessage(R.string.home_nav_export_loading)
                        is ExportPackSourceState.Failed ->
                            PhotoLoadMessage(R.string.home_nav_export_load_failed)
                        is ExportPackSourceState.Empty -> PhotoLoadMessage(
                            when (loaded.reason) {
                                ExportPackEmptyReason.NO_SAVED_LOOKS ->
                                    R.string.home_nav_export_empty_look
                                ExportPackEmptyReason.NO_MARKETPLACE_PRESETS ->
                                    R.string.home_nav_no_marketplaces
                                ExportPackEmptyReason.NO_LIBRARY_ITEMS,
                                ExportPackEmptyReason.NO_EXPORTABLE_ITEMS,
                                ->
                                    R.string.home_nav_export_empty_library
                            },
                        )
                        is ExportPackSourceState.Ready ->
                            ExportScreen(packs = loaded.packs, onBack = { backStack.pop() })
                    }
                }
                entry<LibraryDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = { DetailPlaceholder() },
                    ) + (SECTION_ROOT_METADATA to true),
                ) {
                    val context = LocalContext.current
                    val libraryExportUnavailable = stringResource(
                        R.string.home_nav_library_export_unavailable,
                    )
                    val factory = remember(context, libraryExportUnavailable) {
                        val repository = LibraryRepository(context)
                        object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                LibraryViewModel(repository) {
                                    throw IllegalStateException(
                                        libraryExportUnavailable,
                                    )
                                } as T
                        }
                    }
                    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
                    SectionRoot(
                        showTopBar = !isWideWindow,
                        section = PlinthSection.LIBRARY,
                        balance = chipBalance,
                        onOpenWallet = ::openWallet,
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            TextButton(
                                onClick = { backStack.replaceDetail(BatchHubDestination) },
                                modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.shell_batch))
                            }
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                modifier = Modifier.weight(1f),
                                sharedPhotoTransitionsEnabled = !isWideWindow,
                                onOpenProject = { projectId ->
                                    backStack.replaceDetail(ProjectDestination(projectId))
                                },
                                onOpenPhoto = { projectId ->
                                    if (ProjectRepository(context).photo(projectId) != null) {
                                        backStack.replaceDetail(CardDestination(projectId))
                                    } else {
                                        backStack.replaceDetail(ProjectDestination(projectId))
                                    }
                                },
                            )
                        }
                    }
                }
                entry<BatchHubDestination>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    val context = LocalContext.current
                    val libraryExportUnavailable = stringResource(
                        R.string.home_nav_library_export_unavailable,
                    )
                    val factory = remember(context, libraryExportUnavailable) {
                        val repository = LibraryRepository(context)
                        object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                LibraryViewModel(repository) {
                                    throw IllegalStateException(libraryExportUnavailable)
                                } as T
                        }
                    }
                    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
                    WithShellBack(onBack = { backStack.pop() }) {
                        BatchHubScreen(
                            viewModel = libraryViewModel,
                            onOpenProject = { projectId ->
                                backStack.replaceDetail(LooksDestination(projectId))
                            },
                        )
                    }
                }
                entry<ProjectDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    PlinthScaffold {
                        ProjectWorkspaceScreen(
                            onBack = { backStack.pop() },
                            onOpenGroups = { backStack.push(GroupDestination(destination.projectId)) },
                            onOpenBatch = { backStack.push(LooksDestination(destination.projectId)) },
                            onOpenExport = { backStack.push(ExportDestination) },
                        )
                    }
                }
                entry<GroupDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    val context = LocalContext.current
                    val factory = remember(context, destination.projectId) {
                        object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                com.gexetr.plinthproductphotos.ui.group.GroupViewModel(
                                    destination.projectId,
                                    LibraryRepository(context),
                                ) as T
                        }
                    }
                    val groupViewModel: com.gexetr.plinthproductphotos.ui.group.GroupViewModel =
                        viewModel(key = "plinth-group-${destination.projectId}", factory = factory)
                    WithShellBack(onBack = { backStack.pop() }) {
                        PlinthScaffold {
                            GroupScreen(viewModel = groupViewModel)
                        }
                    }
                }
                entry<LooksDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    val context = LocalContext.current
                    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
                    val defaultLookName = stringResource(R.string.home_nav_default_look)
                    WithLoadedDestination(
                        context,
                        surfaceArgb,
                        defaultLookName,
                        load = {
                            val looks = LookRepository(context).all()
                            looks.firstOrNull() ?: defaultLook(
                                name = defaultLookName,
                                surfaceArgb = surfaceArgb,
                            )
                        },
                    ) { currentLook ->
                        LooksScreen(
                            currentLook = currentLook,
                            onApplyLook = { look ->
                                backStack.push(BatchDestination(destination.projectId, look.id))
                            },
                            onBack = { backStack.pop() },
                        )
                    }
                }
                entry<BatchDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    BatchDestinationRoute(
                        projectId = destination.projectId,
                        lookId = destination.lookId,
                        onBack = { backStack.pop() },
                    )
                }
                entry<SlideKitDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    val context = LocalContext.current
                    when (val loaded = rememberProjectImage(destination.photoId)) {
                        ProjectImageState.Loading -> PhotoLoadMessage(R.string.home_nav_photo_loading)
                        ProjectImageState.Failed -> PhotoLoadMessage(R.string.home_nav_photo_unavailable)
                        is ProjectImageState.Ready -> {
                            val surface = MaterialTheme.colorScheme.surface.toArgb()
                            val accent = PlinthTheme.accentInk.toArgb()
                            val slideReadyCountFormat = stringResource(R.string.home_nav_slide_ready_count)
                            val formatLocale = LocalConfiguration.current.locales[0]
                            WithLoadedDestination(
                                context,
                                load = { firstMarketplace(context) },
                            ) { preset ->
                                WithShellBack(onBack = { backStack.pop() }) {
                                    SlideKitScreen(
                                        items = listOf(loaded.image.source),
                                        background = SlideSeriesBackground.create(
                                            "plinth-paper",
                                            PixelBuffer.solid(
                                                900,
                                                1200,
                                                PixelBuffer.fromArgb(surface),
                                            ),
                                        ),
                                        style = SlideStyle(
                                            id = "plinth-studio",
                                            plateColorArgb = surface,
                                            accentColorArgb = accent,
                                        ),
                                        marketplaceRules = SlideMarketplaceRules(
                                            marketplaceId = preset.id,
                                            marketplaceName = preset.name,
                                            languageTag = preset.defaultLocale,
                                            prohibitedCategories = preset.text.prohibited,
                                            mainBackgroundPolicy = preset.main.backgroundPolicy,
                                        ),
                                        onRenderAndExport = { rendered ->
                                            Toast.makeText(
                                                context,
                                                String.format(formatLocale, slideReadyCountFormat, rendered.size),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                entry<ListingTextDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    val context = LocalContext.current
                    WithLoadedDestination(
                        context,
                        load = { readableMarketplaces(context) },
                    ) { marketplaces ->
                        var selectedMarketplaceId by rememberSaveable {
                            mutableStateOf(marketplaces.firstOrNull()?.id)
                        }
                        var selectedLocale by rememberSaveable {
                            mutableStateOf(marketplaces.firstOrNull()?.defaultLocale ?: "en-US")
                        }
                        var attributes by remember { mutableStateOf(ListingAttributes()) }
                        var attributesConfirmed by rememberSaveable { mutableStateOf(false) }
                        ListingTextScreen(
                            marketplaces = marketplaces,
                            selectedMarketplaceId = selectedMarketplaceId,
                            selectedLocale = selectedLocale,
                            attributes = attributes,
                            attributesConfirmed = attributesConfirmed,
                            isOffline = false,
                            onBack = { backStack.pop() },
                            onMarketplaceSelected = { id ->
                                selectedMarketplaceId = id
                                selectedLocale = marketplaces.firstOrNull { it.id == id }?.defaultLocale ?: "en-US"
                            },
                            onLocaleSelected = { selectedLocale = it },
                            onAttributesChanged = { attributes = it },
                            onAttributesConfirmedChange = { attributesConfirmed = it },
                        )
                    }
                }
                entry<StudioToolsDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    val context = LocalContext.current
                    val studioFactory = remember(context) { studioViewModelFactory(context) }
                    val studioViewModel: StudioViewModel = viewModel(factory = studioFactory)
                    val studioState by studioViewModel.state.collectAsStateWithLifecycle()
                    var photoSelectionReady by remember(destination.photoId, studioViewModel) {
                        mutableStateOf(
                            destination.photoId == null ||
                                studioViewModel.state.value.photoId == destination.photoId,
                        )
                    }
                    LaunchedEffect(destination.photoId, studioViewModel) {
                        destination.photoId?.let { photoId ->
                            if (studioViewModel.state.value.photoId != photoId) {
                                studioViewModel.choosePhoto(photoId)
                            }
                        }
                    }
                    LaunchedEffect(destination.photoId, studioState.photoId) {
                        if (destination.photoId == null || studioState.photoId == destination.photoId) {
                            photoSelectionReady = true
                        }
                    }
                    if (photoSelectionReady) {
                        Box(modifier = Modifier.fillMaxSize().imePadding()) {
                            StudioRoute(
                                onOpenWallet = ::openWallet,
                                initialOperation = studioOperationFor(destination.operation),
                                onBack = { backStack.pop() },
                            )
                        }
                    }
                }
                entry<WalletDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    WalletScreen(
                        state = walletState,
                        onBack = ::navigateBack,
                        onRefreshBalance = walletViewModel::refreshWallet,
                        onPurchase = walletViewModel::purchase,
                        onRestorePurchases = walletViewModel::restorePurchases,
                        onRecoverWallet = walletViewModel::recoverWallet,
                    )
                }
                entry<CoachDestination>(metadata = ListDetailSceneStrategy.detailPane()) { destination ->
                    val context = LocalContext.current
                    WithLoadedDestination(
                        context,
                        load = { firstMarketplace(context) },
                    ) { preset ->
                        CoachRoute(
                            projectId = destination.projectId,
                            marketplace = preset,
                            onBack = { backStack.pop() },
                            onUsePhoto = { handoff: CoachCaptureHandoff ->
                                backStack.push(AutoCardDestination(handoff.item.id))
                            },
                        )
                    }
                }
                    },
                )
                }
                }
            }
        }
    }
}

private const val COACH_PROJECT_ID = "plinth-studio"

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        val next = current.baseContext
        if (next === current) break
        current = next
    }
    return current as? Activity
}

private sealed interface DestinationDataState<out T> {
    data object Loading : DestinationDataState<Nothing>
    data class Ready<T>(val value: T) : DestinationDataState<T>
}

@Composable
private fun <T> WithLoadedDestination(
    vararg keys: Any?,
    load: () -> T,
    loadingContent: @Composable () -> Unit = { CircularProgressIndicator() },
    content: @Composable (T) -> Unit,
) {
    var state by remember(*keys) {
        mutableStateOf<DestinationDataState<T>>(DestinationDataState.Loading)
    }
    LaunchedEffect(*keys) {
        state = DestinationDataState.Ready(loadDestinationData(load))
    }
    when (val loaded = state) {
        DestinationDataState.Loading -> loadingContent()
        is DestinationDataState.Ready -> content(loaded.value)
    }
}

private sealed interface ProjectImageState {
    data object Loading : ProjectImageState
    data object Failed : ProjectImageState
    data class Ready(val image: ProjectImage) : ProjectImageState
}

@Composable
private fun rememberProjectImage(photoId: String): ProjectImageState {
    val context = LocalContext.current
    var state by remember(photoId) { mutableStateOf<ProjectImageState>(ProjectImageState.Loading) }
    LaunchedEffect(photoId, context) {
        state = ProjectImageState.Loading
        state = try {
            ProjectImageState.Ready(
                withContext(Dispatchers.IO) {
                    ProjectImageLoader(ProjectRepository(context)).load(photoId)
                },
            )
        } catch (_: IOException) {
            ProjectImageState.Failed
        } catch (_: IllegalArgumentException) {
            ProjectImageState.Failed
        }
    }
    return state
}

private fun readableMarketplaces(context: Context): List<MarketplacePreset> {
    val source = context.assets.open("marketplaces.json").bufferedReader().use { it.readText() }
    return when (val result = MarketplacePresetParser.parseDocument(source)) {
        is PresetParseResult.Success ->
            result.document.marketplaces.filter { it.status != com.gexetr.plinthproductphotos.domain.presets.PresetStatus.UNREAD }
        is PresetParseResult.Invalid ->
            throw IllegalStateException("Marketplace rules asset is invalid at ${result.error.path}: ${result.error.message}")
    }
}

private fun firstMarketplace(context: Context): MarketplacePreset =
    readableMarketplaces(context).firstOrNull()
        ?: throw IllegalStateException(context.getString(R.string.home_nav_no_marketplaces))

private fun defaultLook(name: String, surfaceArgb: Int) = Look(
    id = "plinth-default-look",
    name = name,
    background = BackgroundSpec(
        kind = BackgroundSpec.Kind.SOLID,
        colorArgb = surfaceArgb,
        color2Argb = null,
        angleDeg = null,
        sceneId = null,
        sceneAssetPath = null,
        prompt = null,
        seed = null,
    ),
    shadow = ShadowSpec(),
    createdAt = 0L,
)

@Composable
private fun PhotoLoadMessage(resourceId: Int) {
    PlinthScaffold {
        Text(
            text = stringResource(resourceId),
            modifier = Modifier.fillMaxSize().padding(24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
private fun AutoCardResultScreen(
    photoId: String,
    pixels: PixelBuffer?,
    onBack: () -> Unit,
    onRegenerate: () -> Unit,
    outputId: String? = null,
) {
    val photoTransition = LocalPhotoSharedTransitionScope.current?.let { sharedScope ->
        with(sharedScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(PhotoTransitionKey(photoId, output = true)),
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            )
        }
    } ?: Modifier
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var outputState by remember(photoId) {
        mutableStateOf<AutoCardOutputState>(
            if (pixels == null) AutoCardOutputState.Loading else AutoCardOutputState.Saving,
        )
    }
    var legacyOutputUnavailable by remember(photoId, outputId) { mutableStateOf(false) }
    var saveAttempt by remember(photoId) { mutableIntStateOf(0) }
    var actionMessage by remember(photoId) { mutableStateOf<Int?>(null) }
    val shareTitle = stringResource(R.string.home_nav_auto_card_share_title)
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val output = (outputState as? AutoCardOutputState.Ready)?.output
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null && output != null) {
            scope.launch {
                try {
                    val unlocked = withContext(Dispatchers.IO) { ServerUnlock.fetch(context) }
                    val bytes = withContext(Dispatchers.IO) {
                        if (WatermarkGate.required(unlocked)) WatermarkGate.applyToEncoded(output.bytes) else output.bytes
                    }
                    withContext(Dispatchers.IO) {
                        ExportDelivery.writeBytesTo(context, uri, bytes)
                    }
                    actionMessage = R.string.home_nav_auto_card_saved_to_folder
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    actionMessage = R.string.home_nav_auto_card_save_error
                }
            }
        }
    }
    LaunchedEffect(photoId, outputId, pixels, saveAttempt) {
        legacyOutputUnavailable = false
        outputState = if (pixels == null) AutoCardOutputState.Loading else AutoCardOutputState.Saving
        try {
            val output = withContext(Dispatchers.IO) {
                val repository = ProjectOutputRepository(context)
                if (pixels != null) repository.saveAutoCard(photoId, pixels)
                else selectSavedOutput(outputId, repository::outputById) { repository.latestAutoCard(photoId) }
            }
            outputState = output?.let(AutoCardOutputState::Ready) ?: AutoCardOutputState.Missing
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            legacyOutputUnavailable =
                failure is com.gexetr.plinthproductphotos.data.project.LegacyCardUnavailableException
            outputState = AutoCardOutputState.Failed
        }
    }
    val resultPixels = (outputState as? AutoCardOutputState.Ready)?.output?.pixels ?: pixels
    val sourceState = rememberProjectImage(photoId)
    val sourcePixels = (sourceState as? ProjectImageState.Ready)?.image?.source
    var resultBitmap by remember(resultPixels, saveAttempt) { mutableStateOf<Bitmap?>(null) }
    var resultPreviewFailed by remember(resultPixels, saveAttempt) { mutableStateOf(false) }
    var sourceBitmap by remember(sourcePixels, saveAttempt) { mutableStateOf<Bitmap?>(null) }
    var sourcePreviewFailed by remember(sourcePixels, saveAttempt) { mutableStateOf(false) }
    var showOriginal by remember(photoId) { mutableStateOf(false) }
    LaunchedEffect(resultPixels, saveAttempt) {
        try {
            resultBitmap = resultPixels?.autoCardPreviewBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            resultPreviewFailed = true
        }
    }
    LaunchedEffect(sourcePixels, saveAttempt) {
        try {
            sourceBitmap = sourcePixels?.autoCardPreviewBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            sourcePreviewFailed = true
        }
    }
    val previewFailed = resultPreviewFailed || sourcePreviewFailed
    val previewGeometry = resultPixels ?: sourcePixels
    val displayedBitmap = if (showOriginal) sourceBitmap else resultBitmap
    val displayedPreviewFailed = if (showOriginal) sourcePreviewFailed else resultPreviewFailed
    val failed = outputState == AutoCardOutputState.Failed ||
        outputState == AutoCardOutputState.Missing || displayedPreviewFailed
    val previewBusy = !failed && (
        outputState == AutoCardOutputState.Loading ||
            outputState == AutoCardOutputState.Saving ||
            (if (showOriginal) sourcePixels != null else resultPixels != null) && displayedBitmap == null
        )
    val previewMessage = when {
        outputState == AutoCardOutputState.Missing -> R.string.home_nav_auto_card_unavailable
        legacyOutputUnavailable -> R.string.home_nav_auto_card_legacy_unavailable
        outputState == AutoCardOutputState.Failed || displayedPreviewFailed -> R.string.home_nav_auto_card_save_error
        outputState == AutoCardOutputState.Saving -> R.string.home_nav_auto_card_saving
        previewBusy -> R.string.home_nav_photo_loading
        else -> R.string.home_nav_auto_card_ready
    }
    PlinthScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plinth_card_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.home_nav_back))
                    }
                },
            )
        },
        bottomBar = {
            if (outputState is AutoCardOutputState.Ready) {
                Surface {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                val ready = (outputState as AutoCardOutputState.Ready).output
                                scope.launch {
                                    try {
                                        val unlocked = withContext(Dispatchers.IO) { ServerUnlock.fetch(context) }
                                        val bytes = withContext(Dispatchers.IO) {
                                            if (WatermarkGate.required(unlocked)) {
                                                WatermarkGate.applyToEncoded(ready.bytes)
                                            } else {
                                                ready.bytes
                                            }
                                        }
                                        val uri = withContext(Dispatchers.IO) {
                                            ExportDelivery.prepareShareUri(
                                                context,
                                                "auto-card-${ready.record.id}.png",
                                                bytes,
                                            )
                                        }
                                        ExportDelivery.shareImage(
                                            context,
                                            "auto-card-${ready.record.id}.png",
                                            "image/png",
                                            uri,
                                            shareTitle,
                                        )
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        actionMessage = R.string.home_nav_auto_card_share_error
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) {
                            Text(stringResource(R.string.home_nav_auto_card_share))
                        }
                        OutlinedButton(
                            onClick = {
                                val ready = (outputState as AutoCardOutputState.Ready).output
                                createDocument.launch(
                                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_TITLE, "auto-card-${ready.record.id}.png")
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) {
                            Text(stringResource(R.string.home_nav_auto_card_save_to_folder))
                        }
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (previewGeometry != null) {
                    val aspectRatio = previewGeometry.width.toFloat() / previewGeometry.height
                    val previewWidth = minOf(maxWidth, maxHeight * aspectRatio)
                    Box(
                        modifier = Modifier.width(previewWidth).aspectRatio(aspectRatio),
                    ) {
                        FormingCardPreview(
                            modifier = photoTransition.fillMaxSize(),
                            bitmap = displayedBitmap.takeUnless { failed },
                            aspectRatio = aspectRatio,
                            busy = previewBusy,
                            message = "",
                            failed = failed,
                        )
                        if (sourceBitmap != null && resultBitmap != null && !failed) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                FilterChip(
                                    selected = showOriginal,
                                    onClick = { showOriginal = true },
                                    label = { Text(stringResource(R.string.autocard_result_before)) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                                FilterChip(
                                    selected = !showOriginal,
                                    onClick = { showOriginal = false },
                                    label = { Text(stringResource(R.string.autocard_result_after)) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (outputState == AutoCardOutputState.Loading ||
                            outputState == AutoCardOutputState.Saving
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            when (outputState) {
                AutoCardOutputState.Loading,
                AutoCardOutputState.Saving,
                -> Text(stringResource(previewMessage), style = MaterialTheme.typography.bodyLarge)
                AutoCardOutputState.Missing -> {
                    Text(stringResource(R.string.home_nav_auto_card_unavailable))
                    TextButton(onClick = onRegenerate, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.home_nav_auto_card_regenerate))
                    }
                }
                AutoCardOutputState.Failed -> {
                    Text(stringResource(R.string.home_nav_auto_card_save_error), color = MaterialTheme.colorScheme.error)
                }
                is AutoCardOutputState.Ready -> {
                    Text(
                        stringResource(if (previewBusy) R.string.home_nav_photo_loading else R.string.home_nav_auto_card_ready),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (previewFailed && outputState != AutoCardOutputState.Failed) {
                Text(stringResource(R.string.home_nav_auto_card_save_error), color = MaterialTheme.colorScheme.error)
            }
            if ((outputState == AutoCardOutputState.Failed && pixels != null) || previewFailed) {
                TextButton(
                    onClick = {
                        actionMessage = null
                        saveAttempt++
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.home_nav_auto_card_retry_save))
                }
            }
            actionMessage?.let { message ->
                Text(
                    stringResource(message),
                    color = if (message == R.string.home_nav_auto_card_saved_to_folder) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

private sealed interface AutoCardOutputState {
    data object Loading : AutoCardOutputState
    data object Saving : AutoCardOutputState
    data object Missing : AutoCardOutputState
    data object Failed : AutoCardOutputState
    data class Ready(val output: StoredProjectOutput) : AutoCardOutputState
}

@Composable
private fun ProjectWorkspaceScreen(
    onBack: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenBatch: () -> Unit,
    onOpenExport: () -> Unit,
) {
    PlinthScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.home_nav_back))
            }
            Text(stringResource(R.string.home_nav_project_title), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onOpenGroups, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.home_nav_project_group))
            }
            TextButton(onClick = onOpenBatch, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.home_nav_project_batch))
            }
            TextButton(onClick = onOpenExport, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.home_nav_project_export))
            }
        }
    }
}

@Composable
private fun BatchHubScreen(
    viewModel: LibraryViewModel,
    onOpenProject: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.shell_batch_choose_project),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        LibraryScreen(
            viewModel = viewModel,
            onOpenProject = onOpenProject,
            modifier = Modifier.weight(1f),
            sharedPhotoTransitionsEnabled = false,
        )
    }
}

private sealed interface BatchLoadState {
    data object Loading : BatchLoadState
    data object Failed : BatchLoadState
    data object MissingLook : BatchLoadState
    data class Ready(val sourceIds: List<String>, val look: Look) : BatchLoadState
}

@Composable
private fun BatchDestinationRoute(
    projectId: String,
    lookId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var state by remember(projectId, lookId) { mutableStateOf<BatchLoadState>(BatchLoadState.Loading) }
    LaunchedEffect(projectId, lookId, context) {
        state = BatchLoadState.Loading
        state = try {
            withContext(Dispatchers.IO) {
                val repository = LibraryRepository(context)
                val look = LookRepository(context).get(lookId)
                if (look == null) {
                    BatchLoadState.MissingLook
                } else {
                    BatchLoadState.Ready(
                        sourceIds = repository.itemsForProject(projectId).map { it.id },
                        look = look,
                    )
                }
            }
        } catch (_: IOException) {
            BatchLoadState.Failed
        } catch (_: IllegalStateException) {
            BatchLoadState.Failed
        }
    }
    when (val loaded = state) {
        BatchLoadState.Loading -> PhotoLoadMessage(R.string.home_nav_projects_loading)
        BatchLoadState.Failed -> PhotoLoadMessage(R.string.home_nav_project_load_failed)
        BatchLoadState.MissingLook -> PhotoLoadMessage(R.string.home_nav_look_missing)
        is BatchLoadState.Ready -> BatchScreen(
            sourceIds = loaded.sourceIds,
            look = loaded.look,
            onBack = onBack,
        )
    }
}

@Composable
private fun WithShellBack(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 8.dp).heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.home_nav_tools_back))
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun SectionRoot(
    showTopBar: Boolean,
    section: PlinthSection,
    balance: Int?,
    onOpenWallet: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (showTopBar) {
        // Keep the compact header inside the animated scene, not in NavDisplay's parent bounds.
        Column(Modifier.fillMaxSize()) {
            PlinthTopBar(section = section, balance = balance, onOpenWallet = onOpenWallet)
            Box(Modifier.weight(1f)) { content() }
        }
    } else {
        content()
    }
}

internal fun AnimatedContentTransitionScope<*>.snappyNavigationTransition(
    isPop: Boolean,
    slideDistancePx: Int,
    isSectionSwitch: Boolean = false,
): ContentTransform = when {
    isSectionSwitch ->
        fadeIn(animationSpec = PlinthMotion.tabFadeIn) togetherWith
            ExitTransition.KeepUntilTransitionsFinished
    isPop ->
        EnterTransition.None togetherWith (
            fadeOut(animationSpec = PlinthMotion.popFadeOut) +
                slideOutHorizontally(
                    animationSpec = PlinthMotion.popSlideOut,
                    targetOffsetX = { slideDistancePx },
                )
            )
    else ->
        (
            fadeIn(animationSpec = PlinthMotion.pushFadeIn) +
                slideInHorizontally(
                    animationSpec = PlinthMotion.pushSlideIn,
                    initialOffsetX = { slideDistancePx },
                )
            ) togetherWith ExitTransition.KeepUntilTransitionsFinished
}

/** The leading pane's own words for "nothing is open yet", shown only on a wide window. */
@Composable
private fun DetailPlaceholder() {
    Text(
        text = stringResource(R.string.home_pick_section),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** One pop, and only when there is something to pop. */
private fun NavBackStack<NavKey>.pop() = popKeepingRoot()

/** One push, and never a duplicate of the destination already on top. */
private fun NavBackStack<NavKey>.push(destination: NavKey) = pushUnlessTop(destination)

/**
 * The stack's root is its first entry, and it is never removed: [NavDisplay] cannot render an
 * empty stack. These are plain list operations so a JVM unit test can run them.
 */
internal fun <T> MutableList<T>.popKeepingRoot() {
    if (size > 1) removeLastOrNull()
}

internal fun <T> MutableList<T>.pushUnlessTop(destination: T) {
    if (lastOrNull() != destination) add(destination)
}

/** Drains to the retained root, then makes [root] the root; the stack is never empty in between. */
internal fun <T> MutableList<T>.resetToRoot(root: T) {
    while (size > 1) popKeepingRoot()
    if (isEmpty()) add(root) else if (first() != root) set(0, root)
}

/** Replaces whatever sits above the root with [detail], so repeated list-pane taps cannot grow the stack. */
internal fun <T> MutableList<T>.replaceDetail(detail: T) {
    while (size > 1) popKeepingRoot()
    pushUnlessTop(detail)
}

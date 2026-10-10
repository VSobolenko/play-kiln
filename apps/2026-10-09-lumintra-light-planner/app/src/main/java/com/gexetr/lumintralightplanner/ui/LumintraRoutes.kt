package com.gexetr.lumintralightplanner.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.lumintralightplanner.data.CatalogueRepository
import com.gexetr.lumintralightplanner.data.CloudRepository
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.data.SkyAssetSource
import com.gexetr.lumintralightplanner.data.sky.SkyEphemeris
import com.gexetr.lumintralightplanner.domain.SatellitePass
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.ui.brief.BriefScreen
import com.gexetr.lumintralightplanner.ui.brief.BriefViewModel
import com.gexetr.lumintralightplanner.ui.night.NightSceneScreen
import com.gexetr.lumintralightplanner.ui.night.NightViewModel
import com.gexetr.lumintralightplanner.ui.settings.CreditsScreen
import com.gexetr.lumintralightplanner.ui.share.ShareScreen
import com.gexetr.lumintralightplanner.ui.share.ShareViewModel
import com.gexetr.lumintralightplanner.ui.spots.SpotEditScreen
import com.gexetr.lumintralightplanner.ui.spots.SpotEditViewModel
import com.gexetr.lumintralightplanner.ui.spots.SpotsScreen
import com.gexetr.lumintralightplanner.ui.spots.SpotsViewModel
import com.gexetr.lumintralightplanner.ui.targets.TargetsScreen
import com.gexetr.lumintralightplanner.ui.targets.TargetsViewModel
import com.gexetr.lumintralightplanner.ui.welcome.WelcomeScreen
import com.gexetr.lumintralightplanner.ui.welcome.WelcomeViewModel
import java.time.Instant

/** Thin routes: ViewModel wiring and frozen screen signatures only. */
@Composable
fun WelcomeRoute(
    scoutStore: ScoutStore,
    onCreateSpot: () -> Unit,
    onSkip: () -> Unit,
    onManualEntry: () -> Unit,
) {
    val viewModel: WelcomeViewModel = viewModel(
        factory = WelcomeViewModel.factory(scoutStore),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    WelcomeScreen(
        state = state,
        onCreateSpot = onCreateSpot,
        onSkip = onSkip,
        onManualEntry = onManualEntry,
    )
}

@Composable
fun BriefRoute(
    appContext: android.content.Context,
    scoutStore: ScoutStore,
    catalogue: CatalogueRepository,
    cloudRepository: CloudRepository,
    ephemeris: SkyEphemeris,
    assetSource: SkyAssetSource,
    passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass>,
    onOpenScene: () -> Unit,
    onShare: () -> Unit,
    onOpenTargets: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: BriefViewModel = viewModel(
        factory = BriefViewModel.factory(
            appContext = appContext,
            scoutStore = scoutStore,
            catalogue = catalogue,
            cloudRepository = cloudRepository,
            ephemeris = ephemeris,
            assetSource = assetSource,
            passProvider = passProvider,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    BriefScreen(
        state = state,
        onScrub = viewModel::onScrub,
        onOpenTarget = { onOpenTargets() },
        onShare = onShare,
        onOpenScene = onOpenScene,
        onToggleZone = viewModel::onToggleZone,
        onRefreshCloud = viewModel::onRefreshCloud,
        onBack = onBack,
    )
}

@Composable
fun SpotsRoute(
    scoutStore: ScoutStore,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: SpotsViewModel = viewModel(
        factory = SpotsViewModel.factory(scoutStore),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    SpotsScreen(
        state = state,
        onAdd = onAdd,
        onOpen = onOpen,
        onSetActive = viewModel::setActive,
        onDelete = viewModel::delete,
        onBack = onBack,
    )
}

@Composable
fun SpotEditRoute(
    spotId: String?,
    scoutStore: ScoutStore,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SpotEditViewModel = viewModel(
        key = "spot-edit-${spotId ?: "new"}",
        factory = SpotEditViewModel.factory(context, scoutStore, spotId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var handledSaveId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.savedSpotId) {
        val id = state.savedSpotId ?: return@LaunchedEffect
        if (id != handledSaveId) {
            handledSaveId = id
            onSaved()
        }
    }
    SpotEditScreen(
        state = state,
        onName = viewModel::onName,
        onLatLon = viewModel::onLatLon,
        onElevation = viewModel::onElevation,
        onTz = viewModel::onTz,
        onPickOnMap = viewModel::onPickOnMap,
        onHorizonSample = viewModel::onHorizonSample,
        onHorizonReset = viewModel::onHorizonReset,
        onSave = viewModel::onSave,
        onCancel = onCancel,
        onPaste = viewModel::onPaste,
        onEntryFormat = viewModel::onEntryFormat,
        onLookupQuery = viewModel::onLookupQuery,
        onLookupSearch = viewModel::onLookupSearch,
        onReverseLookup = viewModel::onReverseLookup,
        onLookupClear = viewModel::onLookupClear,
        onZoneQuery = viewModel::onZoneQuery,
        onUseDeviceZone = viewModel::onUseDeviceZone,
        onHorizonField = viewModel::onHorizonField,
        onCopyCoordinate = { viewModel.onCopyCoordinate(context) },
    )
}

@Composable
fun NightRoute(
    appContext: android.content.Context,
    scoutStore: ScoutStore,
    catalogue: CatalogueRepository,
    cloudRepository: CloudRepository,
    ephemeris: SkyEphemeris,
    assetSource: SkyAssetSource,
    passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass>,
    onOpenBrief: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: NightViewModel = viewModel(
        factory = NightViewModel.factory(
            appContext = appContext,
            scoutStore = scoutStore,
            catalogue = catalogue,
            cloudRepository = cloudRepository,
            assetSource = assetSource,
            ephemeris = ephemeris,
            passProvider = passProvider,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    NightSceneScreen(
        state = state.copy(commands = viewModel),
        onScrub = viewModel::onScrub,
        onSelectTarget = viewModel::onSelectTarget,
        onBackToHorizon = viewModel::onBackToHorizon,
        onToggleRedMode = viewModel::onToggleRedMode,
        onOpenBrief = onOpenBrief,
        modifier = modifier,
    )
}

@Composable
fun TargetsRoute(
    appContext: android.content.Context,
    scoutStore: ScoutStore,
    ephemeris: SkyEphemeris,
    onOpenSettings: () -> Unit,
    onOpenSpots: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: TargetsViewModel = viewModel(
        factory = TargetsViewModel.factory(
            context = appContext,
            store = scoutStore,
            ephemeris = ephemeris,
            onOpenSettings = onOpenSettings,
            onOpenSpots = onOpenSpots,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TargetsScreen(
        state = state,
        onQuery = viewModel::onQuery,
        onToggleFavourites = viewModel::onToggleFavourites,
        onToggleVisibleTonight = viewModel::onToggleVisibleTonight,
        onOpenTarget = viewModel::onOpenTarget,
        onBack = onBack,
    )
}

@Composable
fun ShareRoute(
    appContext: android.content.Context,
    scoutStore: ScoutStore,
    catalogue: CatalogueRepository,
    cloudRepository: CloudRepository,
    ephemeris: SkyEphemeris,
    passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ShareViewModel = viewModel(
        factory = ShareViewModel.factory(
            appContext = appContext,
            scoutStore = scoutStore,
            catalogue = catalogue,
            cloudRepository = cloudRepository,
            ephemeris = ephemeris,
            passProvider = passProvider,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPostcardSaved(context, uri)
        }
    }
    ShareScreen(
        state = state,
        onModeText = viewModel::onModeText,
        onModePostcard = viewModel::onModePostcard,
        onSend = { viewModel.send(context) },
        onSavePostcard = {
            val name = state.postcardFileName
            if (name.isNotBlank() && state.postcard != null) {
                saveLauncher.launch(name)
            }
        },
        onBack = onBack,
    )
}

@Composable
fun CreditsRoute(onBack: () -> Unit) {
    CreditsScreen(onBack = onBack)
}
